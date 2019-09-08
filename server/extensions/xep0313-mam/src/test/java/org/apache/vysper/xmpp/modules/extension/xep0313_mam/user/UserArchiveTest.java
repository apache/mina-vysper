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
package org.apache.vysper.xmpp.modules.extension.xep0313_mam.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.vysper.xmpp.modules.extension.xep0313_mam.IntegrationTest;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.forward.packet.Forwarded;
import org.jivesoftware.smackx.mam.MamManager;
import org.jivesoftware.smackx.mam.element.MamPrefsIQ;
import org.jivesoftware.smackx.sid.element.StableAndUniqueIdElement;
import org.jivesoftware.smackx.sid.element.StanzaIdElement;
import org.junit.Test;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

/**
 * @author Réda Housni Alaoui
 */
public class UserArchiveTest extends IntegrationTest {

    @Test
    public void queryEmptyArchive()
            throws XMPPException.XMPPErrorException, InterruptedException, SmackException.NotConnectedException,
            SmackException.NotLoggedInException, SmackException.NoResponseException {
        MamManager.MamQueryArgs mamQueryArgs = MamManager.MamQueryArgs.builder().build();
        MamManager.MamQuery mamQuery = MamManager.getInstanceFor(alice()).queryArchive(mamQueryArgs);

        assertEquals(0, mamQuery.getMessageCount());
    }

    @Test
    public void receivedArchivedMessageShouldHaveAStanzaId() throws SmackException.NotConnectedException, InterruptedException {
        AtomicReference<Message> carolReceivedMessage = new AtomicReference<>();
        ChatManager.getInstanceFor(carol())
                .addIncomingListener((from, message, chat) -> carolReceivedMessage.set(message));

        Chat chatFromAliceToCarol = ChatManager.getInstanceFor(alice()).chatWith(carol().getUser().asEntityBareJid());
        chatFromAliceToCarol.send("Hello carol");

        Thread.sleep(200);

        assertNotNull(carolReceivedMessage.get());

        StanzaIdElement stanzaIdElement = extractStanzaId(carolReceivedMessage.get());
        assertNotNull(stanzaIdElement);
        assertNotNull(stanzaIdElement.getId());
        assertEquals(carol().getUser().asEntityBareJid(), stanzaIdElement.getBy());
    }

    @Test
    public void sendMessageAndQuerySenderAndReceiverArchive()
            throws SmackException.NotConnectedException, InterruptedException, SmackException.NotLoggedInException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {
        AtomicReference<Message> carolReceivedMessage = new AtomicReference<>();
        ChatManager.getInstanceFor(carol()).addIncomingListener((from, message, chat) -> {
            carolReceivedMessage.set(message);
        });
        Chat chat = ChatManager.getInstanceFor(alice()).chatWith(carol().getUser().asEntityBareJid());
        chat.send("Hello carol");

        MamManager.MamQueryArgs fullQuery = MamManager.MamQueryArgs.builder().build();

        MamManager.MamQuery aliceArchive = MamManager.getInstanceFor(alice()).queryArchive(fullQuery);

        assertEquals(1, aliceArchive.getMessageCount());
        Message toCarolArchivedMessage = aliceArchive.getMessages().get(0);
        assertEquals("Hello carol", toCarolArchivedMessage.getBody());
        assertEquals(alice().getUser(), toCarolArchivedMessage.getFrom());
        assertEquals(carol().getUser().asEntityBareJidOrThrow(),
                toCarolArchivedMessage.getTo().asEntityBareJidOrThrow());
        String toCarolArchivedMessageId = extractStanzaId(toCarolArchivedMessage).getId();

        MamManager.MamQuery carolArchive = MamManager.getInstanceFor(carol()).queryArchive(fullQuery);

        assertEquals(1, carolArchive.getMessageCount());

        Message fromAliceArchivedMessage = carolArchive.getMessages().get(0);
        assertEquals("Hello carol", fromAliceArchivedMessage.getBody());
        assertEquals(alice().getUser(), fromAliceArchivedMessage.getFrom());
        assertEquals(carol().getUser().asEntityBareJidOrThrow(),
                fromAliceArchivedMessage.getTo().asEntityBareJidOrThrow());
        String fromAliceArchivedMessageId = extractStanzaId(fromAliceArchivedMessage).getId();

        assertFalse(toCarolArchivedMessageId.equals(fromAliceArchivedMessageId));
        assertEquals(extractStanzaId(carolReceivedMessage.get()).getId(), fromAliceArchivedMessageId);
    }

    @Test
    public void checkSorting() throws SmackException.NotConnectedException, InterruptedException,
            SmackException.NotLoggedInException, XMPPException.XMPPErrorException, SmackException.NoResponseException {
        Chat chat = ChatManager.getInstanceFor(alice()).chatWith(carol().getUser().asEntityBareJid());
        for (int index = 1; index <= 10; index++) {
            chat.send("Hello " + index);
        }

        MamManager.MamQueryArgs fullQuery = MamManager.MamQueryArgs.builder().build();
        MamManager.MamQuery aliceArchive = MamManager.getInstanceFor(alice()).queryArchive(fullQuery);

        assertEquals(10, aliceArchive.getMessageCount());
        assertTrue(aliceArchive.isComplete());

        for (int index = 1; index <= 10; index++) {
            assertEquals("Hello " + index, aliceArchive.getMessages().get(index - 1).getBody());
        }
    }

    @Test
    public void paginate() throws SmackException.NotConnectedException, InterruptedException,
            SmackException.NotLoggedInException, XMPPException.XMPPErrorException, SmackException.NoResponseException {
        Chat chat = ChatManager.getInstanceFor(alice()).chatWith(carol().getUser().asEntityBareJid());
        for (int index = 1; index <= 10; index++) {
            chat.send("Hello " + index);
        }

        MamManager mamManager = MamManager.getInstanceFor(alice());

        MamManager.MamQueryArgs firstHalfPageable = MamManager.MamQueryArgs.builder().setResultPageSizeTo(5).build();

        MamManager.MamQuery page = mamManager.queryArchive(firstHalfPageable);
        assertFalse(page.isComplete());
        assertEquals(5, page.getMessageCount());
        for (int index = 1; index <= 5; index++) {
            assertEquals("Hello " + index, page.getMessages().get(index - 1).getBody());
        }

        page.pageNext(5);
        assertFalse(page.isComplete());
        assertEquals(5, page.getMessageCount());
        for (int index = 6; index <= 10; index++) {
            assertEquals("Hello " + index, page.getMessages().get(index - 6).getBody());
        }

    }

    @Test
    public void lastPage() throws SmackException.NotConnectedException, InterruptedException,
            SmackException.NotLoggedInException, XMPPException.XMPPErrorException, SmackException.NoResponseException {
        Chat chat = ChatManager.getInstanceFor(alice()).chatWith(carol().getUser().asEntityBareJid());
        for (int index = 1; index <= 10; index++) {
            chat.send("Hello " + index);
        }

        MamManager mamManager = MamManager.getInstanceFor(alice());
        MamManager.MamQuery fullQuery = mamManager.queryArchive(MamManager.MamQueryArgs.builder().build());

        List<String> allMessageIds = fullQuery.getPage().getForwarded().stream().map(Forwarded::getForwardedStanza)
                .map(stanza -> stanza.getExtension("stanza-id", NamespaceURIs.XEP0359_STANZA_IDS))
                .map(StanzaIdElement.class::cast).map(StableAndUniqueIdElement::getId).collect(Collectors.toList());

        String lastMessageUid = mamManager.getMessageUidOfLatestMessage();
        assertEquals(allMessageIds.get(allMessageIds.size() - 1), lastMessageUid);
    }

    @Test
    public void givenOfflineStorageSendMessageToOfflineReceiver()
            throws SmackException, InterruptedException, XMPPException, IOException {
        carol().instantShutdown();

        Chat chatFromAliceToCarol = ChatManager.getInstanceFor(alice()).chatWith(carol().getUser().asEntityBareJid());
        chatFromAliceToCarol.send("Hello carol");

        AtomicReference<Message> carolReceivedMessage = new AtomicReference<>();
        ChatManager.getInstanceFor(carol())
                .addIncomingListener((from, message, chat) -> carolReceivedMessage.set(message));

        carol().connect();
        carol().login();

        Thread.sleep(200);

        assertNotNull(carolReceivedMessage.get());
        assertEquals("Hello carol", carolReceivedMessage.get().getBody());

        Message archivedMessage = fetchUniqueArchivedMessage(carol());
        String storedStanzaId = extractStanzaId(archivedMessage).getId();
        assertNotNull(storedStanzaId);

        String receivedStanzaId = extractStanzaId(carolReceivedMessage.get()).getId();

        assertEquals(storedStanzaId, receivedStanzaId);
    }

    @Test
    public void givenDisabledOfflineStorageSendMessageToOfflineReceiver()
            throws SmackException, InterruptedException, XMPPException, IOException {
        offlineStorageProvider().disable();
        carol().instantShutdown();

        Chat chatFromAliceToCarol = ChatManager.getInstanceFor(alice()).chatWith(carol().getUser().asEntityBareJid());
        chatFromAliceToCarol.send("Hello carol");

        AtomicReference<Message> carolReceivedMessage = new AtomicReference<>();
        ChatManager.getInstanceFor(carol())
                .addIncomingListener((from, message, chat) -> carolReceivedMessage.set(message));

        carol().connect();
        carol().login();

        Thread.sleep(200);

        Message archivedMessage = fetchUniqueArchivedMessage(carol());
        String storedStanzaId = extractStanzaId(archivedMessage).getId();
        assertNotNull(storedStanzaId);
    }

    @Test
    public void getPreferences() throws SmackException.NotLoggedInException, SmackException.NotConnectedException,
            InterruptedException, SmackException.NoResponseException, XMPPException.XMPPErrorException {
        MamManager mamManager = MamManager.getInstanceFor(alice());
        MamManager.MamPrefsResult prefsResult = mamManager.retrieveArchivingPreferences();
        MamManager.MamPrefs mamPrefs = prefsResult.asMamPrefs();

        assertEquals(MamPrefsIQ.DefaultBehavior.always, mamPrefs.getDefaultBehavior());
        assertTrue(mamPrefs.getAlwaysJids().isEmpty());
        assertTrue(mamPrefs.getNeverJids().isEmpty());
    }

    @Test
    public void setPreferences() throws XMPPException.XMPPErrorException, SmackException.NotLoggedInException,
            SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException,
            XmppStringprepException {
        MamManager mamManager = MamManager.getInstanceFor(alice());
        MamManager.MamPrefsResult prefsResult = mamManager.retrieveArchivingPreferences();
        MamManager.MamPrefs prefs = prefsResult.asMamPrefs();

        prefs.setDefaultBehavior(MamPrefsIQ.DefaultBehavior.never);
        prefs.getAlwaysJids().add(JidCreate.bareFrom("always@foo.com"));
        prefs.getNeverJids().add(JidCreate.bareFrom("never@foo.com"));

        MamManager.MamPrefsResult updatedPrefsResult = mamManager.updateArchivingPreferences(prefs);
        MamManager.MamPrefs updatedPrefs = updatedPrefsResult.asMamPrefs();
        assertEquals(MamPrefsIQ.DefaultBehavior.never, updatedPrefs.getDefaultBehavior());

        List<Jid> alwaysJids = updatedPrefs.getAlwaysJids();
        assertEquals(1, alwaysJids.size());
        assertEquals(JidCreate.bareFrom("always@foo.com"), alwaysJids.get(0));

        List<Jid> neverJids = updatedPrefs.getNeverJids();
        assertEquals(1, neverJids.size());
        assertEquals(JidCreate.bareFrom("never@foo.com"), neverJids.get(0));
    }

    @Test
    public void discoverInfo() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException,
            InterruptedException, SmackException.NoResponseException {
        ServiceDiscoveryManager discoveryManager = ServiceDiscoveryManager.getInstanceFor(alice());
        DiscoverInfo discoverInfo = discoveryManager.discoverInfo(alice().getUser().asBareJid());
        List<DiscoverInfo.Feature> features = discoverInfo.getFeatures();

        boolean mamV1Enabled = features.stream().map(DiscoverInfo.Feature::getVar).anyMatch("urn:xmpp:mam:1"::equals);
        assertTrue(mamV1Enabled);

        boolean mamV2Enabled = features.stream().map(DiscoverInfo.Feature::getVar).anyMatch("urn:xmpp:mam:2"::equals);
        assertTrue(mamV2Enabled);
    }

    private Message fetchUniqueArchivedMessage(AbstractXMPPConnection connection)
            throws XMPPException.XMPPErrorException, InterruptedException, SmackException.NotConnectedException,
            SmackException.NotLoggedInException, SmackException.NoResponseException {
        MamManager.MamQueryArgs archiveFullQuery = MamManager.MamQueryArgs.builder().build();
        MamManager.MamQuery archive = MamManager.getInstanceFor(connection).queryArchive(archiveFullQuery);
        assertEquals(1, archive.getMessageCount());
        return archive.getMessages().get(0);
    }

    private StanzaIdElement extractStanzaId(Stanza stanza) {
        assertNotNull(stanza);
        ExtensionElement extensionElement = stanza.getExtension(NamespaceURIs.XEP0359_STANZA_IDS);
        if (!(extensionElement instanceof StanzaIdElement)) {
            fail("No stanza id in " + stanza);
        }

        return (StanzaIdElement) extensionElement;
    }

}
