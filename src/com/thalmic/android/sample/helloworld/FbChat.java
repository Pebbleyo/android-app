package com.thalmic.android.sample.helloworld;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.packet.Message;

import javax.net.ssl.SSLContext;
import java.security.GeneralSecurityException;
import java.util.Collection;

public class FbChat {
    private static final String TAG = "FbChat";

    static XMPPConnection xmpp;

    public static void init(final Activity activity, final FbMessageHandler handler) throws XMPPException {

        ConnectionConfiguration config = new ConnectionConfiguration("chat.facebook.com", 5222);
        config.setSASLAuthenticationEnabled(true);
        config.setSecurityMode(ConnectionConfiguration.SecurityMode.required);
        config.setRosterLoadedAtLogin(true);
        config.setTruststoreType("AndroidCAStore");
        config.setTruststorePath(null);
        config.setTruststorePassword(null);
        config.setSendPresence(false);
//        try {
//            SSLContext sc = SSLContext.getInstance("TLS");
//            sc.init(null, MemorizingTrustManager.getInstanceList(this), new java.security.SecureRandom());
//            config.setCustomSSLContext(sc);
//        } catch (GeneralSecurityException e) {
//            Log.w("TAG", "Unable to use MemorizingTrustManager", e);
//        }
        xmpp = new XMPPConnection(config);
        try {
            xmpp.connect();
            xmpp.login("100005010494816", "password1"); // Here you have to used only facebookusername from facebookusername@chat.facebook.com
            Roster roster = xmpp.getRoster();
            Collection<RosterEntry> entries = roster.getEntries();
            Log.i(TAG, "Connected!");
            Log.i(TAG, "\n\n" + entries.size() + " buddy(ies):");
            // shows first time onliners---->
            String temp[] = new String[50];
            int i = 0;
            for (RosterEntry entry : entries) {
                String user = entry.getUser();
                Log.i("TAG", user);
            }

            PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
            xmpp.addPacketListener(new PacketListener() {
                public void processPacket(Packet packet) {
                    Message message = (Message) packet;
                    final String body = message.getBody();
                    final String from = message.getFrom();

                    Log.i(TAG, body);

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            handler.onMessage(new com.thalmic.android.sample.helloworld.Message(from, body));
                        }
                    });
                }
            }, filter);
        } catch (XMPPException e) {
            xmpp.disconnect();
            e.printStackTrace();
        }
    }

    public static boolean send(String to, String body) {
        Message msg = new Message(to);
        msg.setBody(body);
        xmpp.sendPacket(msg);
        return true;
    }

    public interface FbMessageHandler {
        public void onMessage(com.thalmic.android.sample.helloworld.Message message);
    }
}