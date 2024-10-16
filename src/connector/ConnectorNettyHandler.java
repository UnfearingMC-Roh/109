package connector;

import static connector.ConnectorServerHandler.ConnecterLog;
import constants.ServerConstants;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import java.util.List;
import javax.swing.table.DefaultTableModel;
import server.Randomizer;
import tools.Pair;
import tools.data.LittleEndianAccessor;

/**
 *
 * @author SLFCG
 */
public class ConnectorNettyHandler extends SimpleChannelInboundHandler<LittleEndianAccessor> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        final String IP = ctx.channel().remoteAddress().toString().split(":")[0];
        long time = System.currentTimeMillis();

        ConnectorClientStorage cs = ConnectorServer.getInstance().getClientStorage();
        if (cs.getBlockedIP().contains(IP) && cs.getBlockedTime().containsKey(IP)) {
            if (cs.getBlockedTime().get(IP) + 60000L > time) {
                System.out.println(IP + "Session Opening Failed");
                return;
            } else {
                cs.getBlockedIP().remove(IP);
                cs.getBlockedTime().remove(IP);
            }
        }
        byte count;
        if (cs.getTracker().get(IP) == null) {
            count = 1;
        } else {
            count = cs.getTracker().get(IP).right;
            final long difference = time - cs.getTracker().get(IP).left;
            if (difference < 2000) { // Less than 2 sec
                count++;
            } else if (difference > 20000) { // Over 20 sec
                count = 1;
            }
            if (count >= 5) {
                cs.addBlockedIp(IP);
                System.out.println(IP + " 가 접속 차단 당했습니다.");
                return;
            }
        }
        cs.getTracker().put(IP, new Pair<>(time, count));

        DefaultTableModel model = (DefaultTableModel) ConnectorPanel.jTable2.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            if (model.getValueAt(i, 0).toString().equals(IP.split("/")[1])) {
                System.out.println(IP + " 가 접속 차단 당했습니다.");
                ctx.channel().close();
                return;
            }
        }
        //System.out.println(IP + " 가 접속했습니다.");
        final byte serverRecv[] = new byte[]{(byte) Randomizer.nextInt(255),
            (byte) Randomizer.nextInt(255), (byte) Randomizer.nextInt(255),
            (byte) Randomizer.nextInt(255)};
        final byte serverSend[] = new byte[]{(byte) Randomizer.nextInt(255),
            (byte) Randomizer.nextInt(255), (byte) Randomizer.nextInt(255),
            (byte) Randomizer.nextInt(255)};

        final byte ivRecv[] = serverRecv;
        final byte ivSend[] = serverSend;

        byte realIvRecv[] = new byte[4];
        byte realIvSend[] = new byte[4];
        System.arraycopy(ivRecv, 0, realIvRecv, 0, 4);
        System.arraycopy(ivSend, 0, realIvSend, 0, 4);

        SGAES send = new SGAES(realIvSend);
        SGAES recv = new SGAES(realIvRecv);

        final ConnectorClient cli = new ConnectorClient(ctx.channel(), send, recv);
        //ServerConstants.ccl.add(new Pair(cli, ctx.channel().remoteAddress().toString()));
        //model = (DefaultTableModel) ConnectorPanel.jTable1.getModel();
        //model.addRow(new Object[]{cli.getId(), "", cli.getSession().remoteAddress(), "", cli});
        ConnectorServer.getInstance().getClientStorage().registerMainClient(cli, cli.getAddressIP());
        ctx.write(PacketCreator.sendHandShake(ivSend, ivRecv));
        ctx.flush();
        ctx.channel().attr(ConnectorClient.CLIENTKEY).set(cli);
        cli.setPingTime(System.currentTimeMillis());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        DefaultTableModel model = (DefaultTableModel) ConnectorPanel.jTable1.getModel();
        ConnectorClient client = ctx.channel().attr(ConnectorClient.CLIENTKEY).get();
        if (client != null) {
            /*if (client.getId() != null) {
                if (ServerConstants.authlist2.get(client.getId()) != null) {
                    ServerConstants.authlist2.remove(client.getId());
                }
            }
            if (client.getAuth() != null) {
                if (ServerConstants.authlist.get(client.getAuth()) != null) {
                    ServerConstants.authlist.remove(client.getAuth());
                }
            }*/
            ConnectorServer.getInstance().getClientStorage().registerRemoveWaiting(client.toString(), client.toString());
            ConnectorServer.getInstance().getClientStorage().deregisterClient(client);
            ConnectorServer.getInstance().getClientStorage().broadcastPacket(PacketCreator.sendUserList());
            client.DisableAccount(client.getId());
            if (client.getChar() != null) {
                client.getChar().getClient().setconnecterClient(null);
            }
        }
        ctx.channel().attr(ConnectorClient.CLIENTKEY).set(null);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable ex) {
        ex.printStackTrace();
    }

    public byte[] intToByteArray(int value) {
        byte[] byteArray = new byte[4];
        byteArray[0] = (byte) (value >> 24);
        byteArray[1] = (byte) (value >> 16);
        byteArray[2] = (byte) (value >> 8);
        byteArray[3] = (byte) (value);
        return byteArray;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
        if (event instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) event;
            final ConnectorClient client = ctx.channel().attr(ConnectorClient.CLIENTKEY).get();
            if (e.state() == IdleState.READER_IDLE) {
                ctx.close();
                if (client != null) {
                    ConnecterLog("핑타임 초과 ID : " + client.getId() + " IP : " + client.getAddressIP() + " ", client);
                }
            } else if (e.state() == IdleState.WRITER_IDLE) {
                if (client != null) {
                    //int seed = Randomizer.rand(32512, 2147450880);
                    //client.send(PacketCreator.sendPing(seed));
                    //client.setPing(seed);
                    client.setSendSkill((int) (client.getSendSkill() + 1));
                    if (client.getSendSkill() >= 6) {
                        client.sendPacket(PacketCreator.sendProcessKillList(client));
                        //client.send(PacketCreator.sendSkillCheck()); 
                        client.setSendSkill(0);
                        //client.setSendSkillLimit(true); 
                        //client.timer(); 
                    }
                }
            }
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LittleEndianAccessor slea) throws Exception {
        final ConnectorClient client = (ConnectorClient) ctx.channel().attr(ConnectorClient.CLIENTKEY).get();
        ConnecterLog(client.getSession2().toString() + " \r\n" + slea.toString(), client);
        final byte header = slea.readByte();
        try {
            for (final RecvPacketOpcode recv : RecvPacketOpcode.values()) {
                if (recv.getValue() == header) {
                    ConnectorServerHandler.HandlePacket(recv, slea, client);
                }
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        return;
    }
}
