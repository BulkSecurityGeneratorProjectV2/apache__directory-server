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
package org.apache.directory.server.ldap.support;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.naming.ldap.LdapContext;

import org.apache.directory.server.ldap.ExtendedOperationHandler;
import org.apache.directory.server.ldap.SessionRegistry;
import org.apache.directory.shared.ldap.message.ExtendedRequest;
import org.apache.directory.shared.ldap.message.ExtendedResponse;
import org.apache.directory.shared.ldap.message.LdapResult;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.util.ExceptionUtils;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.demux.MessageHandler;


/**
 * A single reply handler for {@link org.apache.directory.shared.ldap.message.ExtendedRequest}s.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ExtendedHandler extends LdapHandler implements MessageHandler
{
    private Map<String, ExtendedOperationHandler> handlers = new HashMap<String, ExtendedOperationHandler>();


    public ExtendedOperationHandler addHandler( ExtendedOperationHandler eoh )
    {
        synchronized ( handlers )
        {
            return handlers.put( eoh.getOid(), eoh );
        }
    }


    public ExtendedOperationHandler removeHandler( String oid )
    {
        synchronized ( handlers )
        {
            return handlers.remove( oid );
        }
    }


    public ExtendedOperationHandler getHandler( String oid )
    {
        return handlers.get( oid );
    }


    public Map getHandlerMap()
    {
        return Collections.unmodifiableMap( handlers );
    }


    public void messageReceived( IoSession session, Object request ) throws Exception
    {
        ExtendedRequest req = ( ExtendedRequest ) request;
        ExtendedOperationHandler handler = handlers.get( req.getOid() );

        if ( handler == null )
        {
            // As long as no extended operations are implemented, send appropriate
            // error back to the client.
            String msg = "Unrecognized extended operation EXTENSION_OID: " + req.getOid();
            LdapResult result = req.getResultResponse().getLdapResult();
            result.setResultCode( ResultCodeEnum.PROTOCOL_ERROR );
            result.setErrorMessage( msg );
            session.write( req.getResultResponse() );
        }
        else
        {
            try
            {
            	SessionRegistry registry = SessionRegistry.getSingleton();
                //LdapContext ldapContext = registry.getLdapContext( session, null, false);
            	
                handler.handleExtendedOperation( session, SessionRegistry.getSingleton(), req );
            }
            catch ( Exception e )
            {
                LdapResult result = req.getResultResponse().getLdapResult();
                result.setResultCode( ResultCodeEnum.OTHER );
                result.setErrorMessage( "Extended operation handler for the specified EXTENSION_OID (" + req.getOid()
                    + ") has failed to process your request:\n" + ExceptionUtils.getStackTrace( e ) );
                ExtendedResponse resp = ( ExtendedResponse ) req.getResultResponse();
                resp.setResponse( new byte[0] );
                session.write( req.getResultResponse() );
            } 
        }
    }
}
