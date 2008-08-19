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
package org.apache.directory.server.operations.search;

 
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.ReferralException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.server.core.integ.annotations.ApplyLdifs;
import org.apache.directory.server.integ.SiRunner;

import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContextThrowOnRefferal;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;

import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * Tests to make sure the server is operating correctly when handling referrals.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 545029 $
 */
@RunWith ( SiRunner.class )
@ApplyLdifs( {
    // Add new ref for ou=RemoteUsers
    "dn: ou=RemoteUsers,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: referral\n" +
    "objectClass: extensibleObject\n" +
    "ou: RemoteUsers\n" +
    "ref: ldap://fermi:10389/ou=users,ou=system\n" +
    "ref: ldap://hertz:10389/ou=users,dc=example,dc=com\n" +
    "ref: ldap://maxwell:10389/ou=users,ou=system\n\n" +
    
    "dn: c=France,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: country\n" +
    "c: France\n\n" +
    
    "dn: c=USA,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: country\n" +
    "c: USA\n\n" +
    
    "dn: l=Paris,c=france,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: locality\n" +
    "l: Paris\n\n" +
    
    "dn: l=Jacksonville,c=usa,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: locality\n" +
    "l: Jacksonville\n\n" +
    
    "dn: cn=emmanuel lecharny,l=paris,c=france,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: person\n" +
    "objectClass: residentialPerson\n" +
    "cn: emmanuel lecharny\n" +
    "sn: elecharny\n" +
    "l: Paris\n\n" +
    
    "dn: cn=alex karasulu,l=jacksonville,c=usa,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: person\n" +
    "objectClass: residentialPerson\n" +
    "cn: alex karasulu\n" +
    "sn: karasulu\n" +
    "l: Jacksonville\n\n" +
    
    "dn: ou=Countries,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: organizationalUnit\n" +
    "ou: Countries\n\n"
    }
)
public class ReferralSearchIT
{
    public static LdapServer ldapServer;

    
    @Before
    public void setupReferrals() throws Exception
    {
        String ldif =
        "dn: c=europ,ou=Countries,ou=system\n" +
        "objectClass: top\n" +
        "objectClass: referral\n" +
        "objectClass: extensibleObject\n" +
        "c: europ\n" +
        "ref: ldap://localhost:" + ldapServer.getIpPort() + "/c=france,ou=system\n\n" +

        "dn: c=america,ou=Countries,ou=system\n" +
        "objectClass: top\n" +
        "objectClass: referral\n" +
        "objectClass: extensibleObject\n" +
        "c: america\n" +
        "ref: ldap://localhost:" + ldapServer.getIpPort() + "/c=usa,ou=system\n\n";

        LdifReader reader = new LdifReader( new StringReader( ldif ) );
        while ( reader.hasNext() )
        {
            LdifEntry entry = reader.next();
            ldapServer.getDirectoryService().getAdminSession().add( 
                new DefaultServerEntry( ldapServer.getDirectoryService().getRegistries(), entry.getEntry() ) ); 
        }
    }
    
    
    @Test
    public void testSearchBaseIsReferral() throws Exception
    {
        DirContext ctx = getWiredContextThrowOnRefferal( ldapServer );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        
        try
        {
            ctx.search( "ou=RemoteUsers,ou=system", "(objectClass=*)", controls );
            fail( "should never get here" );
        }
        catch ( ReferralException e )
        {
            assertEquals( "ldap://fermi:10389/ou=users,ou=system??sub", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://hertz:10389/ou=users,dc=example,dc=com??sub", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://maxwell:10389/ou=users,ou=system??sub", e.getReferralInfo() );
            assertFalse( e.skipReferral() );
        }
    }


    @Test
    public void testSearchBaseParentIsReferral() throws Exception
    {
        DirContext ctx = getWiredContextThrowOnRefferal( ldapServer );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );

        try
        {
            ctx.search( "cn=alex karasulu,ou=RemoteUsers,ou=system", "(objectClass=*)", controls );
        }
        catch ( ReferralException e )
        {
            assertEquals( "ldap://fermi:10389/cn=alex%20karasulu,ou=users,ou=system??base", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://hertz:10389/cn=alex%20karasulu,ou=users,dc=example,dc=com??base", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://maxwell:10389/cn=alex%20karasulu,ou=users,ou=system??base", e.getReferralInfo() );
            assertFalse( e.skipReferral() );
        }
    }


    @Test
    public void testSearchBaseAncestorIsReferral() throws Exception
    {
        DirContext ctx = getWiredContextThrowOnRefferal( ldapServer );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );

        try
        {
            ctx.search( "cn=alex karasulu,ou=apache,ou=RemoteUsers,ou=system", "(objectClass=*)", controls );
        }
        catch ( ReferralException e )
        {
            assertEquals( "ldap://fermi:10389/cn=alex%20karasulu,ou=apache,ou=users,ou=system??base", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://hertz:10389/cn=alex%20karasulu,ou=apache,ou=users,dc=example,dc=com??base", e
                .getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://maxwell:10389/cn=alex%20karasulu,ou=apache,ou=users,ou=system??base", e
                .getReferralInfo() );
            assertFalse( e.skipReferral() );
        }
    }


    @Test
    public void testSearchContinuations() throws Exception
    {
        DirContext ctx = getWiredContext( ldapServer );

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        NamingEnumeration<SearchResult> list = ctx.search( "ou=system", "(objectClass=*)", controls );
        Map<String,SearchResult> results = new HashMap<String,SearchResult>();
        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            results.put( result.getName(), result );
        }

        assertNotNull( results.get( "ou=users" ) );

        // -------------------------------------------------------------------
        // Now we will throw exceptions when searching for referrals 
        // -------------------------------------------------------------------

        ctx.addToEnvironment( Context.REFERRAL, "throw" );
        list = ctx.search( "ou=system", "(objectClass=*)", controls );
        results = new HashMap<String,SearchResult>();

        try
        {
            while ( list.hasMore() )
            {
                SearchResult result = list.next();
                results.put( result.getName(), result );
            }
        }
        catch ( ReferralException e )
        {
            assertEquals( "ldap://fermi:10389/ou=users,ou=system??sub", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://hertz:10389/ou=users,dc=example,dc=com??sub", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://maxwell:10389/ou=users,ou=system??sub", e.getReferralInfo() );
        }

        assertNull( results.get( "ou=remoteusers" ) );

        // try again but this time with single level scope

        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        list = ctx.search( "ou=system", "(objectClass=*)", controls );
        results = new HashMap<String,SearchResult>();

        try
        {
            while ( list.hasMore() )
            {
                SearchResult result = list.next();
                results.put( result.getName(), result );
            }
        }
        catch ( ReferralException e )
        {
            assertEquals( "ldap://fermi:10389/ou=users,ou=system??base", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://hertz:10389/ou=users,dc=example,dc=com??base", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://maxwell:10389/ou=users,ou=system??base", e.getReferralInfo() );
        }

        assertNull( results.get( "ou=remoteusers" ) );
    }


    /**
     * Test of an search operation with a referral
     *
     * search for "cn=alex karasulu" on "c=america, ou=system"
     * we should get a referral URL thrown, which point to
     * "c=usa, ou=system", and ask for a subtree search
     */
    @Test
    public void testSearchWithReferralThrow() throws Exception
    {
        DirContext ctx = getWiredContextThrowOnRefferal( ldapServer );

        try
        {
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            ctx.search( "c=america,ou=Countries,ou=system", "(cn=alex karasulu)", controls );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch ( ReferralException re )
        {
            String referral = (String)re.getReferralInfo();
            assertEquals( "ldap://localhost:" + ldapServer.getIpPort() + "/c=usa,ou=system??sub", referral );
        }
    }


    /**
     * Test of an search operation with a referral
     *
     * search for "cn=alex karasulu" on "c=america, ou=system"
     * we should get a referral URL thrown, which point to
     * "c=usa, ou=system", and ask for a subtree search
     */
    @Test
    public void testSearchBaseWithReferralThrow() throws Exception
    {
        DirContext ctx = getWiredContextThrowOnRefferal( ldapServer );

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );

        try
        {
            ctx.search( "c=america,ou=Countries,ou=system", "(cn=alex karasulu)", controls );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch ( ReferralException re )
        {
            String referral = (String)re.getReferralInfo();
            assertEquals( "ldap://localhost:" + ldapServer.getIpPort() + "/c=usa,ou=system??base", referral );
        }
    }

    
    /**
     * Test of an search operation with a referral
     *
     * search for "cn=alex karasulu" on "c=america, ou=system"
     * we should get a referral URL thrown, which point to
     * "c=usa, ou=system", and ask for a subtree search
     */
    @Test
    public void testSearchOneLevelWithReferralThrow() throws Exception
    {
        DirContext ctx = getWiredContextThrowOnRefferal( ldapServer );

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );

        try
        {
            ctx.search( "c=america,ou=Countries,ou=system", "(cn=alex karasulu)", controls );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch ( ReferralException re )
        {
            String referral = (String)re.getReferralInfo();
            assertEquals( "ldap://localhost:" + ldapServer.getIpPort() + "/c=usa,ou=system??one", referral );
        }
    }
}