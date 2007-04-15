/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.core.partition;


import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.ServiceUnavailableException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchResult;
import javax.naming.event.EventContext;
import javax.naming.event.NamingListener;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.configuration.PartitionConfiguration;
import org.apache.directory.server.core.enumeration.SearchResultFilter;
import org.apache.directory.server.core.enumeration.SearchResultFilteringEnumeration;
import org.apache.directory.server.core.event.EventService;
import org.apache.directory.server.core.interceptor.InterceptorChain;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.shared.ldap.exception.LdapSizeLimitExceededException;
import org.apache.directory.shared.ldap.exception.LdapTimeLimitExceededException;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * A decorator that wraps other {@link PartitionNexus} to enable
 * {@link InterceptorChain} and {@link InvocationStack} support.
 * All {@link Invocation}s made to this nexus is automatically pushed to
 * {@link InvocationStack} of the current thread, and popped when
 * the operation ends.  All invocations are filtered by {@link InterceptorChain}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class PartitionNexusProxy extends PartitionNexus
{
    /** safe to use set of bypass instructions to lookup raw entries */
    public static final Collection LOOKUP_BYPASS;
    
    /** safe to use set of bypass instructions to getMatchedDn */
    public static final Collection GETMATCHEDDN_BYPASS;
    
    /** safe to use set of bypass instructions to lookup raw entries excluding operational attributes */
    public static final Collection LOOKUP_EXCLUDING_OPR_ATTRS_BYPASS;
    
    /** Bypass String to use when ALL interceptors should be skipped */
    public static final String BYPASS_ALL = "*";
    
    /** Bypass String to use when ALL interceptors should be skipped */
    public static final Collection BYPASS_ALL_COLLECTION = Collections.singleton( BYPASS_ALL );
    
    /** Integer const for DirContext.ADD_ATTRIBUTE */
    private static final Integer ADD_MODOP = new Integer( DirContext.ADD_ATTRIBUTE );
    
    /** Integer const for DirContext.REMOVE_ATTRIBUTE */
    private static final Integer REMOVE_MODOP = new Integer( DirContext.REMOVE_ATTRIBUTE );
    
    /** Integer const for DirContext.REPLACE_ATTRIBUTE */
    private static final Integer REPLACE_MODOP = new Integer( DirContext.REPLACE_ATTRIBUTE );

    private final Context caller;
    private final DirectoryService service;
    private final DirectoryServiceConfiguration configuration;

    static
    {
        Collection<String> c = new HashSet<String>();
        c.add( "normalizationService" );
        c.add( "authenticationService" );
        c.add( "authorizationService" );
        c.add( "defaultAuthorizationService" );
        c.add( "schemaService" );
        c.add( "subentryService" );
        c.add( "operationalAttributeService" );
        c.add( "referralService" );
        c.add( "eventService" );
        LOOKUP_BYPASS = Collections.unmodifiableCollection( c );

        c = new HashSet<String>();
        c.add( "authenticationService" );
        c.add( "authorizationService" );
        c.add( "defaultAuthorizationService" );
        c.add( "schemaService" );
        c.add( "subentryService" );
        c.add( "operationalAttributeService" );
        c.add( "referralService" );
        c.add( "eventService" );
        GETMATCHEDDN_BYPASS = Collections.unmodifiableCollection( c );

    	c = new HashSet<String>();
    	c.add( "normalizationService" );
    	c.add( "authenticationService" );
    	c.add( "authorizationService" );
    	c.add( "defaultAuthorizationService" );
    	c.add( "schemaService" );
    	c.add( "subentryService" );
    	c.add( "referralService" );
    	c.add( "eventService" );
    	c.add( "triggerService" );
    	LOOKUP_EXCLUDING_OPR_ATTRS_BYPASS = Collections.unmodifiableCollection( c );
    }


    /**
     * Creates a new instance.
     * 
     * @param caller a JNDI {@link Context} object that will call this proxy
     * @param service a JNDI service
     */
    public PartitionNexusProxy(Context caller, DirectoryService service)
    {
        this.caller = caller;
        this.service = service;
        this.configuration = service.getConfiguration();
    }


    public LdapContext getLdapContext()
    {
        return this.configuration.getPartitionNexus().getLdapContext();
    }


    public void init( DirectoryServiceConfiguration factoryCfg, PartitionConfiguration cfg )
    {
    }


    public void destroy()
    {
    }


    public Partition getSystemPartition()
    {
        return this.configuration.getPartitionNexus().getSystemPartition();
    }


    public Partition getPartition( LdapDN dn ) throws NamingException
    {
        return this.configuration.getPartitionNexus().getPartition( dn );
    }


    public LdapDN getSuffix() throws NamingException
    {
        return this.configuration.getPartitionNexus().getSuffix();
    }

    public LdapDN getUpSuffix() throws NamingException
    {
        return this.configuration.getPartitionNexus().getUpSuffix();
    }


    public void sync() throws NamingException
    {
        this.service.sync();
    }


    public void close() throws NamingException
    {
        this.service.shutdown();
    }


    public boolean isInitialized()
    {
        return this.service.isStarted();
    }


    public LdapDN getMatchedName ( OperationContext opContext ) throws NamingException
    {
        return getMatchedName( opContext, null );
    }


    public LdapDN getMatchedName( OperationContext opContext, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        Object[] args = new Object[] { opContext };
        stack.push( new Invocation( this, caller, "getMatchedName", args, bypass ) );
        
        try
        {
            return this.configuration.getInterceptorChain().getMatchedName( opContext );
        }
        finally
        {
            stack.pop();
        }
    }


    public LdapDN getSuffix ( OperationContext opContext ) throws NamingException
    {
        return getSuffix( opContext, null );
    }


    public LdapDN getSuffix( OperationContext opContext, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        Object[] args = new Object[] { opContext };
        stack.push( new Invocation( this, caller, "getSuffix", args, bypass ) );
        try
        {
            return this.configuration.getInterceptorChain().getSuffix( opContext );
        }
        finally
        {
            stack.pop();
        }
    }


    public Iterator listSuffixes( OperationContext opContext ) throws NamingException
    {
        return listSuffixes( opContext );
    }


    public Iterator listSuffixes( OperationContext opContext, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        Object[] args = new Object[] { };
        stack.push( new Invocation( this, caller, "listSuffixes", args, bypass ) );
        try
        {
            return this.configuration.getInterceptorChain().listSuffixes( opContext );
        }
        finally
        {
            stack.pop();
        }
    }


    public boolean compare( OperationContext opContext ) throws NamingException
    {
        return compare( opContext, null );
    }


    public boolean compare( OperationContext opContext, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation( this, caller, "compare", new Object[]
            { opContext }, bypass ) );
        try
        {
            return this.configuration.getInterceptorChain().compare( opContext );
        }
        finally
        {
            stack.pop();
        }
    }


    public void delete( OperationContext opContext ) throws NamingException
    {
        delete( opContext, null );
    }


    public void delete( OperationContext opContext, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation( this, caller, "delete", new Object[]
            { opContext }, bypass ) );
        try
        {
            this.configuration.getInterceptorChain().delete( opContext );
        }
        finally
        {
            stack.pop();
        }
    }


    public void add( OperationContext opContext ) throws NamingException
    {
        add( opContext, null );
    }


    public void add( OperationContext opContext, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation( this, caller, "add", new Object[]
            { opContext }, bypass ) );
        try
        {
            this.configuration.getInterceptorChain().add( opContext );
        }
        finally
        {
            stack.pop();
        }
    }


    public void modify( OperationContext opContext ) throws NamingException
    {
        modify( opContext, null );
    }


    public void modify( OperationContext opContext, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        Integer modOpObj;
        
        int modOp = ((ModifyOperationContext)opContext).getModOp();

        switch ( modOp )
        {
            case ( DirContext.ADD_ATTRIBUTE  ):
                modOpObj = ADD_MODOP;
                break;

            case ( DirContext.REMOVE_ATTRIBUTE  ):
                modOpObj = REMOVE_MODOP;
                break;
            
            case ( DirContext.REPLACE_ATTRIBUTE  ):
                modOpObj = REPLACE_MODOP;
                break;
            
            default:
                throw new IllegalArgumentException( "bad modification operation value: " + modOp );
        }

        stack.push( new Invocation( this, caller, "modify", new Object[]
            { opContext }, bypass ) );
        try
        {
            this.configuration.getInterceptorChain().modify( opContext );
        }
        finally
        {
            stack.pop();
        }
    }


    public void modify( LdapDN name, ModificationItemImpl[] mods ) throws NamingException
    {
        modify( name, mods, null );
    }


    public void modify( LdapDN name, ModificationItemImpl[] mods, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation( this, caller, "modify", new Object[]
            { name, mods }, bypass ) );
        try
        {
            this.configuration.getInterceptorChain().modify( name, mods );
        }
        finally
        {
            stack.pop();
        }
    }


    public NamingEnumeration list( OperationContext opContext ) throws NamingException
    {
        return list( opContext, null );
    }


    public NamingEnumeration list( OperationContext opContext, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation( this, caller, "list", new Object[]
            { opContext }, bypass ) );
        try
        {
            return this.configuration.getInterceptorChain().list( opContext );
        }
        finally
        {
            stack.pop();
        }
    }


    public NamingEnumeration search( LdapDN base, Map env, ExprNode filter, SearchControls searchCtls )
        throws NamingException
    {
        NamingEnumeration ne = search( base, env, filter, searchCtls, null );

        if ( ne instanceof SearchResultFilteringEnumeration )
        {
            SearchResultFilteringEnumeration results = ( SearchResultFilteringEnumeration ) ne;
            if ( searchCtls.getTimeLimit() + searchCtls.getCountLimit() > 0 )
            {
                // this will be he last filter added so other filters before it must 
                // have passed/approved of the entry to be returned back to the client
                // so the candidate we have is going to be returned for sure
                results.addResultFilter( new SearchResultFilter()
                {
                    final long startTime = System.currentTimeMillis();
                    int count = 1; // with prefetch we've missed one which is ok since 1 is the minimum


                    public boolean accept( Invocation invocation, SearchResult result, SearchControls controls )
                        throws NamingException
                    {
                        if ( controls.getTimeLimit() > 0 )
                        {
                            long runtime = System.currentTimeMillis() - startTime;
                            if ( runtime > controls.getTimeLimit() )
                            {
                                throw new LdapTimeLimitExceededException();
                            }
                        }

                        if ( controls.getCountLimit() > 0 )
                        {
                            if ( count > controls.getCountLimit() )
                            {
                                throw new LdapSizeLimitExceededException();
                            }
                        }

                        count++;
                        return true;
                    }
                } );
            }
        }
        return ne;
    }


    public NamingEnumeration search( LdapDN base, Map env, ExprNode filter, SearchControls searchCtls, Collection bypass )
        throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation( this, caller, "search", new Object[]
            { base, env, filter, searchCtls }, bypass ) );
        try
        {
            return this.configuration.getInterceptorChain().search( base, env, filter, searchCtls );
        }
        finally
        {
            stack.pop();
        }
    }


    public Attributes lookup( OperationContext opContext ) throws NamingException
    {
        return lookup( opContext, ( Collection ) null );
    }


    public Attributes lookup( OperationContext opContext, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation( this, caller, "lookup", new Object[]
            { opContext }, bypass ) );
        try
        {
            return this.configuration.getInterceptorChain().lookup( opContext );
        }
        finally
        {
            stack.pop();
        }
    }

    public boolean hasEntry( OperationContext opContext ) throws NamingException
    {
        return hasEntry( opContext, null );
    }


    public boolean hasEntry( OperationContext opContext, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation( this, caller, "hasEntry", new Object[]
            { opContext }, bypass ) );
        try
        {
            return this.configuration.getInterceptorChain().hasEntry( opContext );
        }
        finally
        {
            stack.pop();
        }
    }


    public void rename( OperationContext opContext ) throws NamingException
    {
        rename( opContext, null );
    }


    public void rename( OperationContext opContext, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        Object[] args = new Object[]
            { opContext };
        stack.push( new Invocation( this, caller, "rename", args, bypass ) );
        try
        {
            this.configuration.getInterceptorChain().rename( opContext );
        }
        finally
        {
            stack.pop();
        }
    }


    public void move( OperationContext opContext ) throws NamingException
    {
        move( opContext, null );
    }


    public void move( OperationContext opContext, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation( this, caller, "move", new Object[]
            { opContext }, bypass ) );
        try
        {
            this.configuration.getInterceptorChain().move( opContext );
        }
        finally
        {
            stack.pop();
        }
    }


    public void moveAndRename( OperationContext opContext ) throws NamingException
    {
        moveAndRename( opContext, null );
    }


    public void moveAndRename( OperationContext opContext, Collection bypass )
        throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        Object[] args = new Object[]
            { opContext };
        stack.push( new Invocation( this, caller, "moveAndRename", args, bypass ) );
        try
        {
            this.configuration.getInterceptorChain().moveAndRename( opContext );
        }
        finally
        {
            stack.pop();
        }
    }

    /**
     * TODO : check if we can find another way to procect ourselves from recursion.
     * 
     * @param opContext The operation context
     * @param bypass
     * @throws NamingException
     */
    public void bind( OperationContext opContext, Collection bypass )
        throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        Object[] args = new Object[]
            { opContext };
        
        stack.push( new Invocation( this, caller, "bind", args, bypass ) );
        
        try
        {
            configuration.getInterceptorChain().bind( opContext );
        }
        finally
        {
            stack.pop();
        }
    }

    public void unbind( OperationContext opContext, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        Object[] args = new Object[]
            { opContext };
        stack.push( new Invocation( this, caller, "unbind", args, bypass ) );
        try
        {
            this.configuration.getInterceptorChain().unbind( opContext );
        }
        finally
        {
            stack.pop();
        }
    }


    public void bind( OperationContext opContext ) throws NamingException
    {
        bind( opContext, null );
    }


    public void unbind( OperationContext opContext ) throws NamingException
    {
        unbind( opContext, null );
    }


    public Attributes getRootDSE( OperationContext opContext ) throws NamingException
    {
        return getRootDSE( null, null );
    }


    public Attributes getRootDSE( OperationContext opContext, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation( this, caller, "getRootDSE", null, bypass ) );
        try
        {
            return this.configuration.getInterceptorChain().getRootDSE( opContext );
        }
        finally
        {
            stack.pop();
        }
    }


    public void addContextPartition( OperationContext opContext ) throws NamingException
    {
        addContextPartition( opContext, null );
    }


    public void addContextPartition( OperationContext opContext, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation( this, caller, "addContextPartition", new Object[]
            { opContext }, bypass ) );
        try
        {
            this.configuration.getInterceptorChain().addContextPartition( opContext );
        }
        finally
        {
            stack.pop();
        }
    }


    public void removeContextPartition( OperationContext opContext ) throws NamingException
    {
        removeContextPartition( opContext, null );
    }


    public void removeContextPartition( OperationContext opContext, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation( this, caller, "removeContextPartition", new Object[]
            { opContext }, bypass ) );
        try
        {
            this.configuration.getInterceptorChain().removeContextPartition( opContext );
        }
        finally
        {
            stack.pop();
        }
    }


    private void ensureStarted() throws ServiceUnavailableException
    {
        if ( !service.isStarted() )
        {
            throw new ServiceUnavailableException( "Directory service is not started." );
        }
    }


    public void registerSupportedExtensions( Set extensionOids )
    {
        configuration.getPartitionNexus().registerSupportedExtensions( extensionOids );
    }


    // -----------------------------------------------------------------------
    // EventContext and EventDirContext notification methods
    // -----------------------------------------------------------------------

    /*
     * All listener registration/deregistration methods can be reduced down to
     * the following methods.  Rather then make these actual intercepted methods
     * we use them as out of band methods to interface with the notification
     * interceptor.
     */

    public void addNamingListener( EventContext ctx, Name name, ExprNode filter, SearchControls searchControls,
                                   NamingListener namingListener ) throws NamingException
    {
        InterceptorChain chain = this.configuration.getInterceptorChain();
        EventService interceptor = ( EventService ) chain.get( "eventService" );
        interceptor.addNamingListener( ctx, name, filter, searchControls, namingListener );
    }


    public void removeNamingListener( EventContext ctx, NamingListener namingListener ) throws NamingException
    {
        InterceptorChain chain = this.configuration.getInterceptorChain();
        if ( chain == null )
        {
            return;
        }
        EventService interceptor = ( EventService ) chain.get( "eventService" );
        interceptor.removeNamingListener( ctx, namingListener );
    }
}
