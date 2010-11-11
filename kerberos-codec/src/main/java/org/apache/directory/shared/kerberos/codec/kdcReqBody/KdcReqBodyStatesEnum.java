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
package org.apache.directory.shared.kerberos.codec.kdcReqBody;


import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.States;
import org.apache.directory.shared.kerberos.codec.KerberosMessageGrammar;


/**
 * This class store the KDC-REQ-BODY grammar's constants. It is also used for debugging
 * purpose
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum KdcReqBodyStatesEnum implements States
{
    // Start
    START_STATE,                            // 0
    
    // ----- KDC-REQ-BODY message --------------------------------------
    KRB_REQ_BODY_SEQ_STATE,                     // 1
    
    KRB_REQ_BODY_KDC_OPTIONS_TAG_STATE,         // 2
    KRB_REQ_BODY_KDC_OPTIONS_STATE,             // 3
    
    KRB_REQ_BODY_CNAME_TAG_STATE,               // 4
    KRB_REQ_BODY_CNAME_STATE,                   // 5

    KRB_REQ_BODY_REALM_TAG_STATE,               // 6
    KRB_REQ_BODY_REALM_STATE,                   // 7

    KRB_REQ_BODY_SNAME_TAG_STATE,               // 8
    KRB_REQ_BODY_SNAME_STATE,                   // 9

    KRB_REQ_BODY_FROM_TAG_STATE,                // 10
    KRB_REQ_BODY_FROM_STATE,                    // 11

    KRB_REQ_BODY_TILL_TAG_STATE,                // 12
    KRB_REQ_BODY_TILL_STATE,                    // 13
    
    KRB_REQ_BODY_RTIME_TAG_STATE,               // 14
    KRB_REQ_BODY_RTIME_STATE,                   // 15

    KRB_REQ_BODY_NONCE_TAG_STATE,               // 16
    KRB_REQ_BODY_NONCE_STATE,                   // 17

    KRB_REQ_BODY_ETYPE_TAG_STATE,               // 18
    KRB_REQ_BODY_ETYPE_SEQ_STATE,               // 19
    KRB_REQ_BODY_ETYPE_STATE,                   // 20

    KRB_REQ_BODY_ADDRESSES_TAG_STATE,           // 21
    KRB_REQ_BODY_ADDRESSES_STATE,               // 22

    KRB_REQ_BODY_ENC_AUTH_DATA_TAG_STATE,       // 2
    KRB_REQ_BODY_ENC_AUTH_DATA_STATE,           // 2

    KRB_REQ_BODY_ADDITIONAL_TICKETS_TAG_STATE,  // 2
    KRB_REQ_BODY_ADDITIONAL_TICKETS_STATE,      // 2

    // End
    LAST_KRB_REQ_BODY_STATE;              // 8

    
    /**
     * Get the grammar name
     * 
     * @param grammar The grammar code
     * @return The grammar name
     */
    public String getGrammarName( int grammar )
    {
        return "KRB_REQ_BODY_GRAMMAR";
    }


    /**
     * Get the grammar name
     * 
     * @param grammar The grammar class
     * @return The grammar name
     */
    public String getGrammarName( Grammar grammar )
    {
        if ( grammar instanceof KerberosMessageGrammar )
        {
            return "KRB_REQ_BODY_GRAMMAR";
        }
        else
        {
            return "UNKNOWN GRAMMAR";
        }
    }


    /**
     * Get the string representing the state
     * 
     * @param state The state number
     * @return The String representing the state
     */
    public String getState( int state )
    {
        return ( ( state == LAST_KRB_REQ_BODY_STATE.ordinal() ) ? "KRB_REQ_BODY_END_STATE" : name() );
    }

    
    /**
     * {@inheritDoc}
     */
    public boolean isEndState()
    {
        return this == LAST_KRB_REQ_BODY_STATE;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public KdcReqBodyStatesEnum getStartState()
    {
        return START_STATE;
    }
}