/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.netty;

import client.MapleClient;
import client.PacketHandler;
import constants.GameConstants;
import constants.ServerConstants;
import database.DatabaseConnection;
import handling.RecvPacketOpcode;
import handling.SendPacketOpcode;
import handling.ServerType;
import static handling.ServerType.*;
import handling.SessionOpen;
import handling.channel.handler.InventoryHandler;

import handling.login.handler.CharLoginHandler;
import handling.world.World;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import server.AutobanManager;
import server.GeneralThreadPool;
import server.Randomizer;
import server.Start;
import server.Timer;
import server.Timer.PingTimer;
import tools.FileoutputUtil;
import tools.HexTool;
import tools.MapleKMSEncryption;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.data.ByteArrayByteStream;
import tools.data.LittleEndianAccessor;
import tools.packet.LoginPacket;

/**
 *
 * @author csproj
 */
public class MapleNettyHandler extends SimpleChannelInboundHandler<LittleEndianAccessor> {

    private final int serverType;
    private final int channel;
    private List<String> BlockedIP;
    private Map<String, Pair<Long, Byte>> tracker;
    // private boolean norxoriv = !ServerConstants.Use_Fixed_IV;

    public MapleNettyHandler(int serverType, int channel) {
        this.BlockedIP = new ArrayList<>();
        this.tracker = new ConcurrentHashMap<String, Pair<Long, Byte>>();
        this.serverType = serverType;
        this.channel = channel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // Start of IP checking
        final String address = ctx.channel().remoteAddress().toString().split(":")[0];
        if (this.BlockedIP.contains(address)) {
           //  System.out.println("[알림] " + address + " 차단됨.");
            ctx.channel().close();
            return;
        }
        Pair<Long, Byte> track = this.tracker.get(address);
        byte count;
        if (track == null) {
            count = 1;
        } else {
            count = track.right;
            long difference = System.currentTimeMillis() - track.left;
            if (difference < 10000L) {
                ++count;
            } else if (difference > 20000L) {
                count = 1;
            }
            if (count >= 4) {
                this.BlockedIP.add(address);
                this.tracker.remove(address);
           //     System.out.println("[알림] " + address + " 차단 등록 됨.");
                ctx.channel().close();
                return;
            }
        }
        this.tracker.put(address, new Pair<>(System.currentTimeMillis(), count));
        // IV used to decrypt packets from client.
        switch (serverType) {
            case LOGIN:
              //  System.out.println("[알림] " + address + " 에서 로그인 서버로 연결을 시도했습니다.");
                break;
            case CHANNEL:
              //  System.out.println("[알림] " + address + " 에서 채널 서버로 연결을 시도했습니다.");
                break;
            case CASHSHOP:
               // System.out.println("[알림] " + address + " 에서 캐시샵 서버로 연결을 시도했습니다.");
                break;

            default:
        }
            final byte serverRecv[] = new byte[]{101, 86, 18, -3};
            final byte serverSend[] = new byte[]{47, -93, 101, 67};
        final byte ivRecv[] = serverRecv;
        final byte ivSend[] = serverSend;
        
        short sendVER = -83;
        short recvVER = 82;
        
//        //접속기 세팅 true 일 때 CRC 사용 (호스팅 전용)
//        if (ServerConstants.ConnectorSetting) {
//            sendVER = (short) 0xFFFF - 1052;
//            recvVER = 22304;
//        }

        sendVER = (short) 0xFFFF - 1052;
        recvVER = 22304;

        final MapleClient client = new MapleClient(
                ctx.channel(),
                channel,
                new MapleKMSEncryption(ivSend, (short) sendVER), // Sent Cypher
                new MapleKMSEncryption(ivRecv,  (short) recvVER));
//                new MapleKMSEncryption(ivSend, (short)( (short)0xFFFF - (short)1052 )), // Sent Cypher
//                new MapleKMSEncryption( ivRecv,  (short)22304 ));
        client.setChannel(channel);

        ctx.writeAndFlush(LoginPacket.getHello(ivSend, ivRecv));
        ctx.channel().attr(MapleClient.CLIENTKEY).set(client);
        //   System.out.println("[알림] " + ServerConstants.MAPLE_VERSION + " 연결을 시도했습니다.");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        MapleClient client = ctx.channel().attr(MapleClient.CLIENTKEY).get();

        if (client != null) {
            try {
                //System.out.println(client.getIp() + " 활동 감지.");
                client.disconnect(true, false);
            } catch (Throwable t) {
                //    Logger.log(LogType.ERROR, LogFile.ACCOUNT_STUCK, t);
            } finally {
                ctx.close();
                ctx.channel().attr(MapleClient.CLIENTKEY).set(null);

            }
        }
        super.channelUnregistered(ctx);
    
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      //  MapleClient client = ctx.channel().attr(MapleClient.CLIENTKEY).get();
        if (cause instanceof IOException) { // 이게 강제로 팅겼을때 부분인데 여기에도 처리한번해보삼 
            System.out.print("게임을 종료한 아이피입니다."); // 맞징??
            ///if (ctx.channel().eventLoop() != null) {
            //   ctx.channel().eventLoop().shutdown(); // 아들 스레드 초기화
            // }
        } else {
            cause.printStackTrace();
        }

        /*client.disconnect(true, false);
        ctx.close();
        ctx.channel().attr(MapleClient.CLIENTKEY).set(null);
        if (ctx.channel().eventLoop() != null) {
            ctx.channel().eventLoop().shutdownNow(); // 아들 스레드 초기화
        }
        super.exceptionCaught(ctx, cause);// ?*/
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
        }
        final String address = ctx.channel().remoteAddress().toString().split(":")[0];
        if (this.BlockedIP.contains(address)) {
           //  System.out.println("[알림] " + address + " 차단됨.");
            ctx.channel().close();
            return;
        }
        Pair<Long, Byte> track = this.tracker.get(address);
        byte count;
        if (track == null) {
            count = 1;
        } else {
            count = track.right;
            long difference = System.currentTimeMillis() - track.left;
            if (difference < 10000L) {
                ++count;
            } else if (difference > 20000L) {
                count = 1;
            }
            if (count >= 4) {
                this.BlockedIP.add(address);
                this.tracker.remove(address);
           //     System.out.println("[알림] " + address + " 차단 등록 됨.");
                ctx.channel().close();
                return;
            }
        }
        this.tracker.put(address, new Pair<>(System.currentTimeMillis(), count));
        
        MapleClient client = ctx.channel().attr(MapleClient.CLIENTKEY).get();
        if (client != null) {
            try {
                client.disconnect(true, false);
            } catch (Throwable t) {
                //    Logger.log(LogType.ERROR, LogFile.ACCOUNT_STUCK, t);
            } finally {
              //  ctx.close(); //여기가 팅김원인
              //  ctx.channel().attr(MapleClient.CLIENTKEY).set(null); //여기가 팅김원인
            }
        } else {
         //   ctx.channel().close(); //여기가 팅김원인
          //  ctx.close(); //여기가 팅김원인
        }
    }

    @Override
    /*   protected void channelRead0(ChannelHandlerContext ctx, LittleEndianAccessor slea) throws Exception {
        final MapleClient c = (MapleClient) ctx.channel().attr(MapleClient.CLIENTKEY).get();



        final short header_num = slea.readShort();
        try {
          PacketHandler.handlePacket(c, channel == -10, header_num, slea);
          System.out.print("반응4444");
        } catch (Throwable ex) {
            FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, ex);
            FileoutputUtil.log(FileoutputUtil.PacketEx_Log, "Packet: " + header_num + "\n" + slea.toString(true));
        }
    }*/

    protected void channelRead0(ChannelHandlerContext ctx, LittleEndianAccessor slea) throws Exception {
        final MapleClient c = (MapleClient) ctx.channel().attr(MapleClient.CLIENTKEY).get();
        
        final short header_num = slea.readShort();
                if (header_num != RecvPacketOpcode.MOVE_PLAYER.getValue()
                && header_num != RecvPacketOpcode.MOVE_LIFE.getValue()
                && header_num != RecvPacketOpcode.AUTO_AGGRO.getValue()
                && header_num != RecvPacketOpcode.QUEST_ACTION.getValue()
                && header_num != RecvPacketOpcode.NPC_ACTION.getValue()
                && header_num != RecvPacketOpcode.CHANGE_MAP_SPECIAL.getValue()
                && header_num != RecvPacketOpcode.MOVE_PET.getValue()
                && header_num != RecvPacketOpcode.HEAL_OVER_TIME.getValue()
                ) {
                  //System.out.println("Recv " + header_num + " : " + RecvPacketOpcode.getOpcodeName(header_num) + " : " + slea.toString()); //리시브
                }
                if (header_num != SendPacketOpcode.DRAGON_REMOVE.getValue()
                && header_num != SendPacketOpcode.MAP_VALUE.getValue()        
                && header_num != SendPacketOpcode.SECONDPW_ERROR.getValue()            
                ) {
                    //System.out.println("Send " + header_num + " : " + SendPacketOpcode.getOpcodeName(header_num) + " : " + slea.toString()); //센드
                }  
                if (header_num == 600) { // 여기다가 감지될때 아이피로그 하든 뭐하든 작업여기서
                    String encodeString = new String(HexTool.getByteArrayFromHexString(slea.toString().replaceAll("Data: ", "")), Charset.forName("EUC-KR"));
                    String talk = new String();
                    if (c.getPlayer() != null) {
                        talk += "[관리 시스템] 계정 : " + c.getAccountName() + " | 캐릭터 : " + c.getPlayer().getName() + " | 감지 내용 : " + encodeString;
                    } else {
                        if (c.getAccountName() != null) {
                            talk += "[관리 시스템] 계정 : " + c.getAccountName() + " | 감지 내용 : " + encodeString;
                        }
                    }
                    //c.getPlayer().ban("사유 : 자동감지", true, true, true, talk);
                    //c.getSession().close();
                    World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat(talk));
                    
                    //slea.toString() 바이드 euckr로 돌려보면 뭐감지됫는지 확인됨 ex> c7 c7 b1 bb b1 e2 c7 d9 b0 a8 c1 f6 => 피굳기핵감지
                }
                if(slea.toString().equals("Data: 43 73 75 63 63 65 73 73")){ // 여기는 crc 작동 잘되는지 확인하는곳 
                    //System.out.println("핑잘옴");
                    c.crcping = 100; // 100 = 100초 / crc는 20~30초 주기돌고 돌았는지 "CRCsuccess" 라는 패킷이 발송됨
                }     
                
        if (header_num == 56) {
            InventoryHandler.ItemSort(slea, c);
        }
        for (final RecvPacketOpcode recv : RecvPacketOpcode.values()) {
            if (recv.getValue() == header_num) {
                try {
                    //     MapleServerHandler.handlePacket(recv, slea, c, serverType.equals(ServerType.CASHSHOP));
                    PacketHandler.handlePacket(c, channel == -10, header_num, slea);
                    //     System.out.print("반응4444");
                } catch (Exception ex) {

                }
                return;
            }
        }
    }
}
