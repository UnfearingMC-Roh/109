package client.messages.commands;

import client.MapleCharacter;
import constants.ServerConstants.PlayerGMRank;
import client.MapleClient;
import client.MapleStat;
import database.DatabaseConnection;
import handling.SendPacketOpcode;
import handling.channel.ChannelServer;
import handling.world.World;
import java.awt.Point;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import server.MaplePortal;
import server.MedalRanking;
import server.ShutdownServer;
import server.Timer;
import server.life.MapleLifeFactory;
import server.maps.MapleMap;
import server.marriage.MarriageManager;
import server.shops.MinervaOwlSearchTop;
import tools.CPUSampler;
import tools.MaplePacketCreator;
import tools.data.MaplePacketLittleEndianWriter;

public class AdminCommand {

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.ADMIN;
    }

    public static class 경험치배율 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                final int rate = Integer.parseInt(splitted[1]);
                if (splitted.length > 2 && splitted[2].equalsIgnoreCase("all")) {
                    for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                        cserv.setExpRate(rate);
                    }
                } else {
                    c.getChannelServer().setExpRate(rate);
                }
                c.getPlayer().dropMessage(6, "경험치 배율이 " + rate + " 배로 변경되었습니다.");
            } else {
                c.getPlayer().dropMessage(6, "Syntax: !exprate <number> [all]");
            }
            return 1;
        }
    }
    
    public static class 테스트 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.sendPacket(MaplePacketCreator.itemEffect(c.getPlayer().getId(), 0));
            c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.itemEffect(c.getPlayer().getId(), 0), false);
//            c.sendPacket(MaplePacketCreator.showOwnBuffEffect(0, 0, 0, 0));
//            MaplePacketLittleEndianWriter oPacket = new MaplePacketLittleEndianWriter();
//            oPacket.writeOpcode(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
//            oPacket.write(7);
//            oPacket.writeInt(4221001);
////            oPacket.write(100);
////            oPacket.write(30);
////            oPacket.write(Byte.parseByte(splitted[1]));
//            c.sendPacket(oPacket.getPacket());
            
//            c.sendPacket(MaplePacketCreator.serverNotice(6, 1302000, "한가지 색상이 질렸다면 [{}]로 한껏 뽐내보는건 어떨까요? CASHSHOP> 추천상품과 장비에 있습니다."));
//            c.sendPacket(MaplePacketCreator.temporaryStats_Aran());
            
//            c.getPlayer().gainSP(1000);
//            double damage = 15000; //15000 데미지
//            int pdrate = 100; //몬스터
//            int def = 0; //장비 방무
//            int a = def * pdrate / -100 + pdrate;
//            double d = (100.0 - a) * damage * 0.01; 
//            //여기까지 방무 계산
//            //여러가지의 계산 이후 . . . 
//            //보공 계산
//            int bossDAMr = 300; //보공 300
//            double dd = bossDAMr * d * 0.01 + d;
//            System.err.println("After Damage = " + dd + " / Original Damage = " + d);
            
            
//            c.getPlayer().getClient().getSession().write(MaplePacketCreator.HD(599 + Short.parseShort(splitted[1])));
            return 1;
        }
    }

    public static class 메소배율 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                final int rate = Integer.parseInt(splitted[1]);
                if (splitted.length > 2 && splitted[2].equalsIgnoreCase("all")) {
                    for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                        cserv.setMesoRate(rate);
                    }
                } else {
                    c.getChannelServer().setMesoRate(rate);
                }
                c.getPlayer().dropMessage(6, "메소 배율이 " + rate + " 배로 변경 되었습니다.");
            } else {
                c.getPlayer().dropMessage(6, "Syntax: !mesorate <number> [all]");
            }
            return 1;
        }
    }

    public static class 드롭배율 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                final int rate = Integer.parseInt(splitted[1]);
                if (splitted.length > 2 && splitted[2].equalsIgnoreCase("all")) {
                    for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                        cserv.setDropRate(rate);
                    }
                } else {
                    c.getChannelServer().setDropRate(rate);
                }
                c.getPlayer().dropMessage(6, "드롭 배율이 " + rate + " 배로 변경 되었습니다.");
            } else {
                c.getPlayer().dropMessage(6, "Syntax: !mesorate <number> [all]");
            }
            return 1;
        }
    }
    
    public static class 아이템삭제 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "사용법 : !아이템제거 <캐릭터명> <아이템코드>");
                return 0;
            }
            MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chr == null) {
                c.getPlayer().dropMessage(6, "존재하지 않는 캐릭터입니다.");
                return 0;
            }
            chr.removeAll(Integer.parseInt(splitted[2]), false);
            c.getPlayer().dropMessage(6, splitted[1] + "가 가진 모든 " + splitted[2] + "번 아이템이 제거되었습니다.");
            return 1;

        }
    }   
    
    public static class 인기도 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "사용법: !인기도 닉네임 숫자");
                return 0;
            }
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            int fame = 0;
            try {
                fame = Integer.parseInt(splitted[2]);
            } catch (NumberFormatException nfe) {
                c.getPlayer().dropMessage(5, "숫자를 기재해주세요.");
                return 0;
            }
            if (victim != null && player.allowedToTarget(victim)) {
                victim.addFame(fame);
                victim.updateSingleStat(MapleStat.FAME, victim.getFame());
            }
            return 1;
        }
    }    
    
    public static class 저장 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            // User Data Save Start
            for (ChannelServer ch : ChannelServer.getAllInstances()) {
                for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                    chr.saveToDB(false, false);
                }
            }
            // User Data Save End
            // Server Data Save Start
            World.Guild.save();
            World.Alliance.save();
            World.Family.save();
            MarriageManager.getInstance().saveAll();
            MinervaOwlSearchTop.getInstance().saveToFile();
            MedalRanking.saveAll();
            //       RankingWorker.getInstance().run();
            // Server Data Save End
            c.getPlayer().dropMessage(6, "저장이 완료되었습니다.");
            return 1;
        }
    }    
    
    public static class 펫 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            boolean allowed = c.getPlayer().getMap().togglePetPick();
            c.getPlayer().dropMessage(6, "Current Map's Pet Pickup allowed : " + allowed);
            if (!allowed) {
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.yellowChat("현재 맵에서 펫 줍기 기능이 비활성화 되었습니다."));
            } else {
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.yellowChat("현재 맵에서 펫 줍기 기능이 활성화 되었습니다."));
            }
            return 1;
        }

    }

    public static class 서버종료 extends CommandExecute {

        protected static Thread t = null;

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(5, "서버 종료 중...");
            if (t == null || !t.isAlive()) {
                t = new Thread(ShutdownServer.getInstance());
                ShutdownServer.getInstance().shutdown();
                t.start();
            } else {
                c.getPlayer().dropMessage(5, "서버 종료를 위해 프로그램들을 종료 중 입니다. 잠시만 기다려주십시오.");
            }
            return 1;
        }
    }

    public static class 서버종료시간 extends 서버종료 {

        private static ScheduledFuture<?> ts = null;
        private int minutesLeft = 0;

        @Override
        public int execute(MapleClient c, String[] splitted) {
            minutesLeft = Integer.parseInt(splitted[1]);
            c.getPlayer().dropMessage(5, minutesLeft + "분 후 서버가 종료됩니다.");
            if (ts == null && (t == null || !t.isAlive())) {
                t = new Thread(ShutdownServer.getInstance());
                ts = Timer.EventTimer.getInstance().register(new Runnable() {

                    public void run() {
                        if (minutesLeft == 0) {
                            ShutdownServer.getInstance().shutdown();
                            t.start();
                            ts.cancel(false);
                            return;
                        }
                        World.Broadcast.broadcastMessage(MaplePacketCreator.serverMessage("서버가 " + minutesLeft + "분 후 종료됩니다. 안전하게 로그아웃해 주세요."));
                        minutesLeft--;
                    }
                }, 60000);
            } else {
                c.getPlayer().dropMessage(5, "서버종료를 위해 프로그래밍을 종료하는 중 입니다.");
            }
            return 1;
        }
    }   
    

    public static class 아이피대조 extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "사용법: !아이피대조 <캐릭터 닉네임>");
                return 0;
            }
            Connection con = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            PreparedStatement ps2 = null;
            ResultSet rs2 = null;

            int Accid = 0, Count = 0;
            String IP = "";
            try {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("SELECT * FROM characters WHERE name = ?");
                ps.setString(1, splitted[1]);
                rs = ps.executeQuery();
                if (rs.next()) {
                    Accid = rs.getInt("accountid");
                    ps.close();
                    rs.close();
                    ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
                    ps.setInt(1, Accid);
                    rs = ps.executeQuery();
                    if (rs.next()) {
                        IP = rs.getString("SessionIP");
                        c.getPlayer().dropMessage(2, "검색한 캐릭터의 접속 아이디 파악 (검색 값 : " + splitted[1] + ") [아이피 : " + IP + "]");
                        ps.close();
                        rs.close();
                        ps = con.prepareStatement("SELECT * FROM accounts WHERE SessionIP = ?");
                        ps.setString(1, IP);
                        rs = ps.executeQuery();
                        String Text = "";
                        while (rs.next()) {
                            if (rs.getInt("banned") > 0) {
                                Text = " / 밴 당한 아이디";
                            }
                            c.getPlayer().dropMessage(5, "아이디 : " + rs.getString("name") + " / " + Text);
                            Accid = rs.getInt("id");
                            ps2 = con.prepareStatement("SELECT * FROM characters WHERE accountid = ?");
                            ps2.setInt(1, Accid);
                            rs2 = ps2.executeQuery();
                            while (rs2.next()) {
                                Count++;
                                c.getPlayer().dropMessage(6, Count + "번 캐릭터 : " + rs2.getString("name"));
                            }
                            if (Count == 0) {
                                c.getPlayer().dropMessage(6, rs.getString("name") + " 아이디는 캐릭터가 없습니다.");
                            }
                            Count = 0;
                            ps2.close();
                            rs2.close();
                        }
                        ps.close();
                        rs.close();
                    } else {
                        c.getPlayer().dropMessage(5, "버그가 발생했습니다.");
                        return 0;
                    }
                } else {
                    c.getPlayer().dropMessage(5, "존재하지 않는 닉네임입니다.");
                    return 0;
                }
                con.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (rs != null) {
                        rs.close();
                    }
                    if (ps != null) {
                        ps.close();
                    }
                    if (con != null) {
                        con.close();
                    }
                    if (rs2 != null) {
                        rs2.close();
                    }
                    if (ps2 != null) {
                        ps2.close();
                    }
                } catch (Exception e) {
                }
            }
            return 1;
        }
    }    

    public static class 고용상인닫기 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                cserv.closeAllMerchant();
            }
            return 1;
        }
    }
    
    
    public static class StartProfiling extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            CPUSampler sampler = CPUSampler.getInstance();
            sampler.addIncluded("client");
            sampler.addIncluded("constants"); //or should we do Packages.constants etc.?
            sampler.addIncluded("database");
            sampler.addIncluded("handling");
            sampler.addIncluded("provider");
            sampler.addIncluded("scripting");
            sampler.addIncluded("server");
            sampler.addIncluded("tools");
            sampler.start();
            return 1;
        }
    }
    
        public static class StopProfiling extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            CPUSampler sampler = CPUSampler.getInstance();
            try {
                String filename = "odinprofile.txt";
                if (splitted.length > 1) {
                    filename = splitted[1];
                }
                File file = new File(filename);
                if (file.exists()) {
                    c.getPlayer().dropMessage(6, "The entered filename already exists, choose a different one");
                    return 0;
                }
                sampler.stop();
                FileWriter fw = new FileWriter(file);
                sampler.save(fw, 1, 10);
                fw.close();
            } catch (IOException e) {
                System.err.println("Error saving profile" + e);
            }
            sampler.reset();
            return 1;
        }
    }
    
}
