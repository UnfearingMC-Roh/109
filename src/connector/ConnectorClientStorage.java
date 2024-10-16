/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package connector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import tools.Pair;

/**
 *
 * @author cccv
 */
public class ConnectorClientStorage {

    private final Map<String, ConnectorClient> MainClients = new ConcurrentHashMap<>();//모두포함
    private final Map<String, ConnectorClient> LoginClients = new ConcurrentHashMap<>();//로그인한유저들
    private final Map<String, ConnectorClient> SClients = new ConcurrentHashMap<>();//세컨드계정

    private static final List<String> BlockedIP = new ArrayList<>();
    private static final Map<String, Long> BlockedTime = new ConcurrentHashMap<>();
    private static final Map<String, Pair<Long, Byte>> tracker = new ConcurrentHashMap<>();

    private static final Map<String, String> removeWaiting = new ConcurrentHashMap<>();
    private static final Map<String, String> addInGameCharWaiting = new ConcurrentHashMap<>();

    public ConnectorClientStorage() {

    }

    public final void addBlockedIp(String address) {
        BlockedIP.add(address);
        BlockedTime.put(address, System.currentTimeMillis());
        tracker.remove(address); // Cleanup
    }

    public final Map<String, Pair<Long, Byte>> getTracker() {
        return tracker;
    }

    public final List<String> getBlockedIP() {
        return BlockedIP;
    }

    public final Map<String, Long> getBlockedTime() {
        return BlockedTime;
    }

    public final void addTracker(String address, byte count) {
        tracker.put(address, new Pair<>(System.currentTimeMillis(), count));
    }

    public final void registerRemoveWaiting(final String c, final String s) {
        removeWaiting.put(s, c);
    }

    public final void deregisterRemoveWaiting(final String c) {
        if (c != null) {
            removeWaiting.remove(c);
        }
    }

    public final String getRemoveWaiting(final String c) {
        if (removeWaiting.get(c) != null) {
            return removeWaiting.get(c);
        }
        return null;
    }

    public final void registerChangeInGameCharWaiting(final String c, final String s) {
        if (getChangeInGameCharWaiting(c) == null) {
            addInGameCharWaiting.put(s, c);
        }
    }

    public final void deregisterChangeInGameCharWaiting(final String c) {
        if (c != null) {
            addInGameCharWaiting.remove(c);
        }
    }

    public final String getChangeInGameCharWaiting(final String c) {

        if (addInGameCharWaiting.get(c) != null) {
            return addInGameCharWaiting.get(c);
        }
        return null;
    }

    public final void registerMainClient(final ConnectorClient c, final String s) {
        MainClients.put(s.toLowerCase(), c);
    }

    public final void registerClient(final ConnectorClient c, final String s) {
        LoginClients.put(s.toLowerCase(), c);
    }

    public final void registerSClient(final ConnectorClient c, final String s) {
        SClients.put(s.toLowerCase(), c);
    }

    public final void deregisterClient(final ConnectorClient c) {
        if (c != null) {
            if (c.getAddressIP() != null) {
                MainClients.remove(c.getAddressIP().toLowerCase());
            }
            if (c.getId() != null) {
                LoginClients.remove(c.getId().toLowerCase());
            }
            if (c.getSecondId() != null) {
                SClients.remove(c.getSecondId().toLowerCase());
            }
        }
    }

    public final ConnectorClient getClientByName(final String c) {
        if (LoginClients.get(c.toLowerCase()) != null) {
            return LoginClients.get(c.toLowerCase());
        } else {
            if (SClients.get(c.toLowerCase()) != null) {
                return SClients.get(c.toLowerCase());
            } else if (MainClients.get(c.toLowerCase()) != null) {
                return MainClients.get(c.toLowerCase());
            }
        }
        return null;
    }

    public final List<ConnectorClient> getAllClient() {
        final Iterator<ConnectorClient> itr = MainClients.values().iterator();
        List<ConnectorClient> asd = new ArrayList<>();
        while (itr.hasNext()) {
            asd.add(itr.next());
        }
        return asd;
    }

    public final List<ConnectorClient> getMainClients() {
        final Iterator<ConnectorClient> itr = MainClients.values().iterator();
        List<ConnectorClient> asd = new ArrayList<>();
        while (itr.hasNext()) {
            asd.add(itr.next());
        }
        return asd;
    }

    public final List<ConnectorClient> getLoginClients() {
        final Iterator<ConnectorClient> itr = LoginClients.values().iterator();
        List<ConnectorClient> asd = new ArrayList<>();
        while (itr.hasNext()) {
            asd.add(itr.next());
        }
        return asd;
    }

    public final List<ConnectorClient> getSClients() {
        final Iterator<ConnectorClient> itr = SClients.values().iterator();
        List<ConnectorClient> asd = new ArrayList<>();
        while (itr.hasNext()) {
            asd.add(itr.next());
        }
        return asd;
    }

    public final void broadcastPacket(final byte[] data) {
        final Iterator<ConnectorClient> itr = LoginClients.values().iterator();
        while (itr.hasNext()) {
            itr.next().getSession().write(data);
        }
    }
}
