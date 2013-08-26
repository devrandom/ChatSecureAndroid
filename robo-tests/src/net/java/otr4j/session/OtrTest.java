/*
 * Copyright 2011 Google Inc. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package net.java.otr4j.session;

import static org.junit.Assert.assertEquals;

import java.security.KeyPair;
import java.util.List;
import java.util.Random;

import net.java.otr4j.OtrEngineImpl;
import net.java.otr4j.OtrException;
import net.java.otr4j.OtrKeyManagerImpl;
import net.java.otr4j.OtrPolicy;
import net.java.otr4j.OtrPolicyImpl;
import net.java.otr4j.session.OtrSm.OtrSmEngineHost;

import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

public class OtrTest extends EasyMockSupport {
    static class TestEngineHost implements OtrSmEngineHost {
        Session other;
        String pendingMessage;
        
        private OtrKeyManagerImpl manager;
        
        public TestEngineHost(OtrKeyManagerImpl manager) {
            this.manager = manager;
        }

        public void setOther(Session other) {
            this.other = other;
        }
        
        @Override
        public void injectMessage(SessionID sessionID, String msg) {
            if (pendingMessage != null)
                throw new RuntimeException("already have a pending message from " + sessionID);
            pendingMessage = msg;
        }
        
        boolean sendPending() {
            try {
                if (pendingMessage == null)
                    return false;
                String msg = pendingMessage;
                pendingMessage = null;
                other.transformReceiving(msg);
                return true;
            } catch (OtrException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public OtrPolicy getSessionPolicy(SessionID sessionID) {
            return new OtrPolicyImpl(OtrPolicyImpl.ALLOW_V2);
        }

        @Override
        public KeyPair getKeyPair(SessionID sessionID) {
            KeyPair kp = manager.loadLocalKeyPair(sessionID);

            if (kp == null) {
                manager.generateLocalKeyPair(sessionID);
                kp = manager.loadLocalKeyPair(sessionID);
            }
            return kp;

        }

        @Override
        public void askForSecret(SessionID sessionID, String question) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void showWarning(SessionID sessionID, String warning) {
            System.out.println("warn " + sessionID + ": " + warning);
        }

        @Override
        public void showError(SessionID sessionID, String error) {
            System.out.println("error " + sessionID + ": " + error);
            //throw new UnsupportedOperationException();
        }        
    }

    private OtrKeyManagerImpl manager_a;
    private OtrKeyManagerImpl manager_b;
    private SessionID sessionId_a;
    private SessionID sessionId_b;
    private Session session_a;
    private Session session_b;
    private TestEngineHost host_a;
    private TestEngineHost host_b;
    private OtrEngineImpl engine_a;
    private OtrEngineImpl engine_b;

    @Before
    public void setUp() throws Exception {
        manager_a = new OtrKeyManagerImpl(new MemoryPropertiesStore());
        manager_b = new OtrKeyManagerImpl(new MemoryPropertiesStore());
        sessionId_a = new SessionID("a1", "ua", "xmpp");
        sessionId_b = new SessionID("a1", "ub", "xmpp");
        host_a = new TestEngineHost(manager_a);
        host_b = new TestEngineHost(manager_b);
        manager_a.generateLocalKeyPair(sessionId_a);
        manager_b.generateLocalKeyPair(sessionId_b);
        manager_a.savePublicKey(sessionId_a, manager_b.loadLocalKeyPair(sessionId_b).getPublic());
        manager_b.savePublicKey(sessionId_b, manager_a.loadLocalKeyPair(sessionId_a).getPublic());
        engine_a = new OtrEngineImpl(host_a);
        engine_b = new OtrEngineImpl(host_b);
        session_a = engine_a.getSession(sessionId_a);
        session_b = engine_b.getSession(sessionId_b);
        host_a.setOther(session_b);
        host_b.setOther(session_a);
    }

    void runEngines() {
        while (host_a.sendPending() || host_b.sendPending()) {
        }
    }
    
    @Test
    public void testNegotiate() throws OtrException {
        for (int i = 0 ; i < 100 ; i++) {
            // Clear
            engine_a.endSession(sessionId_a);
            runEngines();
            engine_b.endSession(sessionId_b);
            runEngines();
            assertEquals(SessionStatus.PLAINTEXT, engine_a.getSessionStatus(sessionId_a));
            assertEquals(SessionStatus.PLAINTEXT, engine_b.getSessionStatus(sessionId_b));
            
            // Negotiate
            engine_a.startSession(sessionId_a);
            runEngines();
            assertEquals(SessionStatus.ENCRYPTED, engine_a.getSessionStatus(sessionId_a));
            assertEquals(SessionStatus.ENCRYPTED, engine_b.getSessionStatus(sessionId_b));
        }
    }

    @Test
    public void testMessage() throws OtrException {
        engine_a.startSession(sessionId_a);
        runEngines();
        
        String enc = engine_a.transformSending(sessionId_a, "hello");
        String dec = engine_b.transformReceiving(sessionId_b, enc);
        assertEquals("hello", dec);
    }

    @Test
    public void testMessage_random1() throws OtrException {
        engine_a.startSession(sessionId_a);
        runEngines();
        
        Random random = new Random(1234L);
        
        for (int i = 0 ; i < 100 ; i++) {
            String msg = "hello " + random.nextInt(1000000);
            if (random.nextBoolean()) {
                String enc = engine_a.transformSending(sessionId_a, msg);
                String dec = engine_b.transformReceiving(sessionId_b, enc);
                assertEquals(msg, dec);
            } else {
                String enc = engine_b.transformSending(sessionId_b, msg);
                String dec = engine_a.transformReceiving(sessionId_a, enc);
                assertEquals(msg, dec);
            }
        }
    }

    @Test
    public void testMessage_random2() throws Exception {
        engine_a.startSession(sessionId_a);
        runEngines();
        
        long seed = new Random().nextLong();
        System.out.println("seed " + seed);
        Random random = new Random(seed);
        List<String> ma = Lists.newArrayList();
        List<String> mb = Lists.newArrayList();
        List<String> qa = Lists.newArrayList();
        List<String> qb = Lists.newArrayList();
        
        int max = 0;
        
        byte[] bytes = new byte[40000];
        for (int i = 0 ; i < 10000 ; i++) {
            random.nextBytes(bytes);
            for (int j = 0 ; j < bytes.length ; j++) bytes[j] = (byte)((bytes[j] & 0x3f) + 1);
            String msg = new String(bytes, "ASCII");
            if (random.nextBoolean()) {
                String enc = engine_a.transformSending(sessionId_a, msg);
                ma.add(msg);
                qa.add(enc);
            } else {
                String enc = engine_b.transformSending(sessionId_b, msg);
                mb.add(msg);
                qb.add(enc);
            }
            
            if (ma.size() > max) max = ma.size();
            if (mb.size() > max) max = mb.size();

            if (random.nextBoolean() && !ma.isEmpty()) {
                String plain = ma.remove(0);
                String enc = qa.remove(0); 
                String dec = engine_b.transformReceiving(sessionId_b, enc);
                assertEquals(plain, dec);
            }

            if (random.nextBoolean() && !mb.isEmpty()) {
                String plain = mb.remove(0);
                String enc = qb.remove(0); 
                String dec = engine_a.transformReceiving(sessionId_a, enc);
                assertEquals(plain, dec);
            }
        }
        
        System.out.println("max queue size " + max);
        System.out.println("current queue sizes " + ma.size() + " " + mb.size());
    }
}
