package pt.lsts.imc4j.net;

import pt.lsts.imc4j.msg.Announce;
import pt.lsts.imc4j.msg.Heartbeat;
import pt.lsts.imc4j.msg.Message;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImcPeer {

    private Announce lastAnnounce = null;
    private Heartbeat lastHeartbeat = null;
    private String services = "";
    private long lastHeartbeatMillis = 0;
    private static final long aliveThresholdMillis = 30_000;
    private int remoteId;
    private InetSocketAddress tcpAddress = null;
    private InetSocketAddress udpAddress = null;


    private boolean active = false;

    private Pattern pProto = Pattern
            .compile("imc\\+(.*)://(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+):(\\d+)/");

    public ImcPeer(int imcId) {
        this.remoteId = imcId;
    }

    public int getImcId() {
        return remoteId;
    }

    public InetSocketAddress getTcpAddress() {
        return tcpAddress;
    }

    public InetSocketAddress getUdpAddress() {
        return udpAddress;
    }

    public void setMessage(Message m, SocketAddress source) {
        switch (m.mgid()) {
            case Heartbeat.ID_STATIC:
                lastHeartbeatMillis = System.currentTimeMillis();
                lastHeartbeat = (Heartbeat) m;
                break;
            case Announce.ID_STATIC:
                lastAnnounce = (Announce) m;
                if (!services.equals(lastAnnounce.services)) {
                    parseServices(lastAnnounce.services);
                    services = lastAnnounce.services;
                }
                break;
            default:
                break;
        }
    }

    private void parseServices(String services) {
        String[] servs = lastAnnounce.services.split(";");
        LinkedHashSet<InetSocketAddress> udpAddresses = new LinkedHashSet<>();
        LinkedHashSet<InetSocketAddress> tcpAddresses = new LinkedHashSet<>();

        for (String serv : servs) {
            Matcher mProto = pProto.matcher(serv);
            if (mProto.matches()) {
                String proto = mProto.group(1);
                String hostname = mProto.group(2) + "." + mProto.group(3) + "."
                        + mProto.group(4) + "." + mProto.group(5);
                int port = Integer.parseInt(mProto.group(6));

                if (proto.equals("udp"))
                    udpAddresses.add(new InetSocketAddress(hostname, port));
                else if (proto.equals("tcp"))
                    tcpAddresses.add(new InetSocketAddress(hostname, port));
            }
        }
        if (udpAddresses.iterator().hasNext())
            udpAddress = udpAddresses.iterator().next();
        if (tcpAddresses.iterator().hasNext())
            tcpAddress = tcpAddresses.iterator().next();
    }

    boolean isAlive() {
        return (System.currentTimeMillis() - lastHeartbeatMillis) < aliveThresholdMillis;
    }

    void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    long getLastHeartBeatMillis() {
        return lastHeartbeatMillis;
    }
}