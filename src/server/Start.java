package server;

import client.MapleCharacter;
import client.SkillFactory;
import client.inventory.MapleInventoryIdentifier;
import connector.ConnectorPanel;
import connector.ConnectorServer;
import constants.GameConstants;
import constants.ServerConstants;
import static constants.ServerConstants.ConnectorSetting;
import database.DatabaseConnection;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.channel.MapleGuildRanking;
import handling.channel.handler.DueyHandler;
import handling.login.LoginInformationProvider;
import handling.login.LoginServer;
import handling.world.MaplePartyCharacter;
import handling.world.World;
import handling.world.family.MapleFamily;
import handling.world.guild.MapleGuild;
import java.awt.Point;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import server.Timer.*;
import server.events.MapleOxQuizFactory;
import server.life.MapleLifeFactory;
import server.life.MapleMonsterInformationProvider;
import server.life.MobSkillFactory;
import server.life.PlayerNPC;
import server.quest.MapleQuest;
import server.shops.MinervaOwlSearchTop;
import tools.DeadLockDetector;
import tools.MemoryUsageWatcher;
import tools.SystemUtils;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.fusesource.jansi.Ansi.ansi;
import org.fusesource.jansi.AnsiConsole;
import server.life.MapleMonster;
import server.maps.MapleMap;
import tools.DatabaseBackup;
import tools.MaplePacketCreator;

public class Start {

    public static String host;
    public static String SQL_URL;
    public static String SQL_USER;
    public static String SQL_PASSWORD;
    public static boolean TUTORIAL_EXP_SYSTEM = false;
    public static boolean PC_BONUS_EXP_SYSTEM = false;
    public static boolean RAINBOW_WEEK_EXP_SYSTEM = false;
    public static boolean BOOM_UP_BONUS_EXP_SYSTEM = false;
    public static int TUTORIAL_EXP_SYSTEM_RATE;
    public static int PC_BONUS_EXP_SYSTEM_RATE;
    public static int RAINBOW_WEEK_EXP_SYSTEM_RATE;
    public static int BOOM_UP_BONUS_EXP_SYSTEM_RATE;
    public static long startTime = System.currentTimeMillis();
    public static transient ScheduledFuture<?> boss;
    public static transient ScheduledFuture<?> eventboss;
    
    public static int increaseConnectionUsers = 0; //기본 값 40
    
    protected static String toUni(String kor)
            throws UnsupportedEncodingException {
        return new String(kor.getBytes("KSC5601"), "8859_1");
    }

    static {
        try {
            FileInputStream db = new FileInputStream("config/dbnhost.properties");
            Properties dbprobs = new Properties();
            dbprobs.load(db);
            db.close();
            host = new String(dbprobs.getProperty("serverIp").getBytes("ISO-8859-1"), "euc-kr");
            SQL_URL = new String(dbprobs.getProperty("url").getBytes("ISO-8859-1"), "euc-kr");
            SQL_USER = new String(dbprobs.getProperty("dbuser").getBytes("ISO-8859-1"), "euc-kr");
            SQL_PASSWORD = new String(dbprobs.getProperty("pass").getBytes("ISO-8859-1"), "euc-kr");
            //ConnectorSetting = Boolean.parseBoolean(dbprobs.getProperty("커넥터사용설정"));
            //ConnectorSetting = Boolean.parseBoolean(dbprobs.getProperty(toUni("커넥터사용설정")));

            FileInputStream exp = new FileInputStream("config/expsystem.properties");
            Properties expprobs = new Properties();
            expprobs.load(exp);
            exp.close();
            TUTORIAL_EXP_SYSTEM = expprobs.getProperty("TUTORIAL_EXP_SYSTEM").equals("true") ? true : false;
            PC_BONUS_EXP_SYSTEM = expprobs.getProperty("PC_BONUS_EXP_SYSTEM").equals("true") ? true : false;
            RAINBOW_WEEK_EXP_SYSTEM = expprobs.getProperty("RAINBOW_WEEK_EXP_SYSTEM").equals("true") ? true : false;
            BOOM_UP_BONUS_EXP_SYSTEM = expprobs.getProperty("BOOM_UP_BONUS_EXP_SYSTEM").equals("true") ? true : false;
            TUTORIAL_EXP_SYSTEM_RATE = Integer.parseInt(expprobs.getProperty("TUTORIAL_EXP_SYSTEM_RATE"));
            PC_BONUS_EXP_SYSTEM_RATE = Integer.parseInt(expprobs.getProperty("PC_BONUS_EXP_SYSTEM_RATE"));
            RAINBOW_WEEK_EXP_SYSTEM_RATE = Integer.parseInt(expprobs.getProperty("RAINBOW_WEEK_EXP_SYSTEM_RATE"));
            BOOM_UP_BONUS_EXP_SYSTEM_RATE = Integer.parseInt(expprobs.getProperty("BOOM_UP_BONUS_EXP_SYSTEM_RATE"));
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Start.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Start.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static final Start instance = new Start();
    public static AtomicInteger CompletedLoadingThreads = new AtomicInteger(0);
    public static int TotalLoadingThreads = 0;

    public static void startGC(long start) {
        System.gc();
        float end = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.println("[GarbageCollection] This Thread Memory : " + ((start - end) / (1024 * 1024)) + "Bytes Clean");
    }

    public void run() throws InterruptedException {
        AnsiConsole.systemInstall();
        String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        System.setProperty("net.sf.odinms.wzpath", "wz");
        
        if (osName.startsWith("windows")) {
            try {
                PrintStream out = new PrintStream(System.out, true, "EUC-KR");
                PrintStream err = new PrintStream(System.out, true, "EUC-KR");
                System.setOut(out);
                System.setErr(err);
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(Start.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        System.out.println(ansi().a("\033[01;37m"));

        DatabaseConnection.init();

        if (Boolean.parseBoolean(ServerProperties.getProperty("adminOnly")) || ServerConstants.Use_Localhost) {
            ServerConstants.Use_Fixed_IV = false;
            System.out.println("[!!! Admin Only Mode Active !!!]");
        }

        String ip = ServerProperties.getProperty("channel.ipconfig");
        if (ip != null) {
            try {
                InetAddress address = InetAddress.getByName(ip);
                String raw_address = address.getHostAddress();

                ServerConstants.Gateway_IP = address.getAddress();

                System.out.println("아이피 정보 : " + ip);
                //System.out.println("[Gateway IP] Resolved Host Address of Server Machine : " + raw_address);
            } catch (Exception e) {
                System.err.println("아이피 설정 오류입니다. ");
                //System.err.println("Set default gateway ip - 127.0.0.1 (loopback)");
                e.printStackTrace();

                ServerConstants.Gateway_IP = new byte[]{(byte) 26, (byte) 41, (byte) 76, (byte) 201}; //서버아이피
            }
        } else {
            System.out.println("Gateway IP was not specified. default : " + ServerConstants.Gateway_IP);
        }

        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("UPDATE accounts SET loggedin = 0, allowed = 0, connecterClient = null");
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ex) {
            throw new RuntimeException("[오류] 마이쿼리를 체크 해주세요.");
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }
        }
        //System.out.println("[Start] Korean MapleStory Ver. 1.2." + ServerConstants.MAPLE_VERSION + "");
        //System.out.println("서버 오픈을 환영합니다.");

        Start ld = new Start();
        try {
            LoadingThread();
        } catch (Exception ex) {
            Logger.getLogger(Start.class.getName()).log(Level.SEVERE, null, ex);
        }

        //System.out.println("[서버 불러오기]");
        try {
            LoginServer.run_startup_configurations();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
       // System.out.println("[Login Initialized]");

        System.out.println("[채널포트]");
        try {
            ChannelServer.startChannel_Main();
        } catch (Exception e) {
            throw new RuntimeException();
        }
        //System.out.println("[Channel Initialized]");

        System.out.println("[채널정보]");
        try {
            CashShopServer.run_startup_configurations();
            MapleLifeFactory.AutoSave();
        } catch (Exception e) {
            throw new RuntimeException();
        }

        //System.out.println("[CS Initialized]");
        PlayerNPC.loadAll();// touch - so we see database problems early...
        //threads.
        Timer.CheatTimer.getInstance().register(AutobanManager.getInstance(), 60000L);
        Runtime.getRuntime().addShutdownHook(new Thread(new Shutdown()));
        World.registerRespawn();
        World.AdminShopItemRequest.loadDB();
        ShutdownServer.registerMBean();
        // 서버 최적화 CPU 샘플러 << 지랄 개구라 메모리누수 원인
        // 에녹 : 헬지와 셈플러는 거르고봅니다.

        //CPUSampler.getInstance().start();
        // 메모리 최적화 가비지 콜렉션
        //Start.startGC(System.currentTimeMillis());//1229
        //sqlBackup();
        //timeBossHottime();//HOTTIME CLEAR

        LoginServer.setOn(); //now or later
        ConnectorPanel cpa = new ConnectorPanel();
        cpa.setVisible(true);
        try {
            new ConnectorServer().run_startup_configurations();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        System.out.println("[총 소요시간 " + ((System.currentTimeMillis() - startTime) / 1000) + " 초]");
        
        try {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                            for (int i = 0; i < cserv.getPlayerStorage().getAllCharacters().size(); i++) {
                                MapleCharacter player = cserv.getPlayerStorage().getAllCharacters().get(i);
                                if (player != null) {
                                    if (player.getMapId() == 923040300) { //켄타
                                        if (player.isLeader()) {
                                            if (player.getMapId() == 923040300) {
                                                for (MaplePartyCharacter Pplayer : player.getParty().getMembers()) {
                                                    MapleCharacter victim = player.getClient().getChannelServer().getPlayerStorage().getCharacterByName(Pplayer.getName());
                                                    if (victim.getMapId() != 923040300) {
//                                                            victim.warp(923040300);
                                                    }
                                                }
                                            }
                                            if (player.kentatime == 180) {
//                                                    player.getEventInstance().getMapInstance(923040300).setSpawns(true);
//                                                    player.getEventInstance().getMapInstance(923040300).respawn(true);
                                                player.getEventInstance().getMapInstance(923040300).spawnMonsterOnGroundBelow(player.getClient().getChannelServer().getEventSM().getEventManager("kentaPQ").getMonster(9300460), new java.awt.Point(-121, 89));
                                                player.getMap().startMapEffectAOJ("3분동안 켄타를 지켜주세요!!", 5120036, false);
                                                player.getMap().broadcastMessage(MaplePacketCreator.getClock(180));
                                            }
                                            player.kentatime--;
                                            if (player.kentatime == 120) {
                                                player.getMap().startMapEffect("2분 남았습니다.", 5120036);
                                            } else if (player.kentatime == 60) {
                                                player.getMap().startMapEffect("1분 남았습니다.", 5120036);
                                            } else if (player.kentatime == 30) {
                                                player.getMap().startMapEffect("30초 남았습니다.", 5120036);
                                            } else if (player.kentatime == 0) {
                                                player.getMap().broadcastMessage(MaplePacketCreator.showEffect("quest/party/clear"));
                                                player.getMap().broadcastMessage(MaplePacketCreator.playSound("Party1/Clear"));
                                                player.getEventInstance().setInstanceMap(923040300).setSpawns(false);
                                                player.getMap().killAllMonsters(true);
                                                player.gainExp(230000, true, true, true);
                                                player.getEventInstance().setProperty("kenta_nextStage", "1");
                                                player.getMap().startMapEffectAOJ("잠시후 피아누스가 있는맵으로 이동됩니다.", 5120036, false);
                                            } else if (player.kentatime == -3) {
                                                player.warpParty(923040400);
                                            }
                                        }
                                    } 
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("타이머에 문제있음 : ");
                        e.printStackTrace();
                    }
                }
            };
            Timer.WorldTimer.getInstance().register(r, 1000, 1000);
        } catch (Exception e) {
            System.err.println("월드 타이머 힐링 : " + e);
        }

        /* Handle Event */
        if (SystemUtils.getTimeMillisByDay(2018, 5, 31) < System.currentTimeMillis() && System.currentTimeMillis() < SystemUtils.getTimeMillisByDay(2018, 6, 8)) {
            Runnable eventStart = new Runnable() {
                @Override
                public void run() {
                    LoginServer.setFlag((byte) 1);
                    LoginServer.setEventMessage("KMS 1.2.109\r\n메이플 월드\r\n#b바다의 날 이벤트중#k~#r\r\nEXP : x1\r\nDROP : x2#k\r\n#b(아쿠아로드 한정)#k\r\n#rMESO : x1#k");
                }
            };
            Runnable eventEnd = new Runnable() {
                @Override
                public void run() {
                    LoginServer.setFlag((byte) Integer.parseInt(ServerProperties.getProperty("flag", "0")));
                    LoginServer.setEventMessage(ServerProperties.getProperty("eventMessage"));
                }
            };
            SystemUtils.setScheduleAtTime(2018, 5, 31, 0, 0, 0, eventStart);
            SystemUtils.setScheduleAtTime(2018, 6, 8, 0, 0, 0, eventEnd);
        }

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long time = cal.getTimeInMillis();
        long schedulewait = 0;
        if (time > System.currentTimeMillis()) {
            schedulewait = time - System.currentTimeMillis();
        } else {
            schedulewait = time + 86400000L - System.currentTimeMillis();
        }
        if (schedulewait < 3600000) {
            schedulewait += 86400000L;
        }
        // Timer.WorldTimer.getInstance().register(MapleCharacter::initDailyQuestBonus, schedulewait);
        Timer.WorldTimer.getInstance().register(RankingWorker::run, 30 * 60 * 1000L);
        World.ArcaneSimbol.loadDB();
        eventboss();
        GameConstants.loadBoosterItemID();
        
        new MemoryUsageWatcher(88).start();
        //new Debugger().setVisible(false);
        new DeadLockDetector(60, DeadLockDetector.RESTART).start();
    }

    public static class Shutdown implements Runnable {

        public void run() {
            ShutdownServer.getInstance().run();
            ShutdownServer.getInstance().run();
        }
    }

    public static void main(final String args[]) throws InterruptedException {
        instance.run();
    }

    public static final void LoadingThread() throws Exception {
        Start start = new Start();
        start.ThreadLoader();
    }

    public final void ThreadLoader() throws InterruptedException {
        World.init();
        WorldTimer.getInstance().start();
        EtcTimer.getInstance().start();
        MapTimer.getInstance().start();
        CloneTimer.getInstance().start();
        EventTimer.getInstance().start();
        BuffTimer.getInstance().start();
        PingTimer.getInstance().start();

        LoadingThread WorldLoader = new LoadingThread(new Runnable() {
            public void run() {
                MapleGuildRanking.getInstance().load();
                MapleGuild.loadAll();
            }
        }, "월드", this);

        /*LoadingThread MarriageLoader = new LoadingThread(new Runnable() {
         public void run() {
         MarriageManager.getInstance();
         }
         }, "MarriageLoader", this);*/
        LoadingThread MedalRankingLoader = new LoadingThread(new Runnable() {
            public void run() {
                MedalRanking.loadAll();
            }
        }, "랭크", this);

        LoadingThread FamilyLoader = new LoadingThread(new Runnable() {
            public void run() {
                MapleFamily.loadAll();
            }
        }, "패밀리", this);

        LoadingThread QuestLoader = new LoadingThread(new Runnable() {
            public void run() {
                MapleLifeFactory.loadQuestCounts();
                MapleQuest.initQuests();
            }
        }, "퀘스트", this);

        LoadingThread ProviderLoader = new LoadingThread(new Runnable() {
            public void run() {
                MapleItemInformationProvider.getInstance().runEtc();
            }
        }, "유저", this);

        LoadingThread MonsterLoader = new LoadingThread(new Runnable() {
            public void run() {
                MapleMonsterInformationProvider.getInstance().load();
            }
        }, "몬스터", this);

        LoadingThread ItemLoader = new LoadingThread(new Runnable() {
            public void run() {
                MapleItemInformationProvider.getInstance().runItems();
            }
        }, "아이템", this);

        LoadingThread SkillFactoryLoader = new LoadingThread(new Runnable() {
            public void run() {
                SkillFactory.load();
            }
        }, "스킬", this);

        LoadingThread BasicLoader = new LoadingThread(new Runnable() {
            public void run() {
                LoginInformationProvider.getInstance();
                RandomRewards.load();
                //RandomRewards.loadGachaponRewardFromINI("ini/gachapon.ini");
                MapleOxQuizFactory.getInstance();
                MapleCarnivalFactory.getInstance();  //1.2.6은 카니발 X
                MobSkillFactory.getInstance();
                SpeedRunner.loadSpeedRuns();
                MinervaOwlSearchTop.getInstance().loadFromFile();
                //CashItemSaleRank.setUp();
            }
        }, "이벤트", this);

        LoadingThread MIILoader = new LoadingThread(new Runnable() {
            public void run() {
                MapleInventoryIdentifier.getInstance();
            }
        }, "마일리지", this);

        LoadingThread CashItemLoader = new LoadingThread(new Runnable() {
            public void run() {
                CashItemFactory.getInstance().initialize();
            }
        }, "캐쉬", this);

        /*LoadingThread BgmLoader = new LoadingThread(new Runnable() {
         public void run() {
         MapleBgmProvider.load();
         }
         }, "BgmLoader", this);*/
        LoadingThread[] LoadingThreads = {WorldLoader, FamilyLoader, QuestLoader, ProviderLoader, SkillFactoryLoader, BasicLoader, CashItemLoader, MIILoader, MonsterLoader, ItemLoader, MedalRankingLoader};
        TotalLoadingThreads = LoadingThreads.length;

        for (Thread t : LoadingThreads) {
            t.start();
        }
        synchronized (this) {
            wait();
        }
        while (CompletedLoadingThreads.get() != TotalLoadingThreads) {
            synchronized (this) {
                wait();
            }
        }
        //System.out.println("[Loading] Caching Quest Item Information...");
        MapleItemInformationProvider.getInstance().runQuest();
        //System.out.println("[LoadingComplete] Cached Quest Item Information...");
    }

    private static class LoadingThread extends Thread {

        protected String LoadingThreadName;

        private LoadingThread(Runnable r, String t, Object o) {
            super(new NotifyingRunnable(r, o, t));
            LoadingThreadName = t;
        }

        @Override
        public synchronized void start() {
            //System.out.println("[불러오기] " + LoadingThreadName + " 정보");
            super.start();
        }
    }

    private static class NotifyingRunnable implements Runnable {

        private String LoadingThreadName;
        private long StartTime;
        private Runnable WrappedRunnable;
        private final Object ToNotify;

        private NotifyingRunnable(Runnable r, Object o, String name) {
            WrappedRunnable = r;
            ToNotify = o;
            LoadingThreadName = name;
        }

        public void run() {
            StartTime = System.currentTimeMillis();
            WrappedRunnable.run();
            System.out.println("[불러오기 완료] " + LoadingThreadName + "정보");
            synchronized (ToNotify) {
                CompletedLoadingThreads.incrementAndGet();
                ToNotify.notify();
            }
        }
    }
    
    public static void sqlBackup() {//쿼리백업
        Timer.WorldTimer.getInstance().register(new Runnable() {
            public void run() {
                DatabaseBackup.getInstance().startTasking();
                System.out.println("쿼리백업.");
            }
        }, 10000);
    }
    
    public static void timeBossHottime() {
        final int[] hour = ServerConstants.hour;
        final int failitem = ServerConstants.failitem;
        final int pice = ServerConstants.pice;

        if (boss == null) {//null
            Calendar cal = Calendar.getInstance();
            long time = cal.getTimeInMillis();
            long schedulewait = 0;
            if (time > System.currentTimeMillis()) {
                schedulewait = time - System.currentTimeMillis();
            } else {
                while (true) {
                    cal.add(Calendar.SECOND, 1);
                    for (int ho : hour) {
                        if (cal.getTimeInMillis() > System.currentTimeMillis() && cal.getTime().getHours() == (ho - 1) && cal.getTime().getMinutes() >= 50 && cal.getTime().getSeconds() == 0) {
                            schedulewait = cal.getTimeInMillis() - System.currentTimeMillis();
                            break;
                        }
                    }
                    if (schedulewait > 0) {
                        break;
                    }
                }
            }

            boss = Timer.WorldTimer.getInstance().register(new Runnable() {
                public void run() {
                    Date nowtime = new Date();
                    for (int ho : hour) {
                        if (ho <= 0) {
                            ho = 24;
                        }
                        if (nowtime.getMinutes() >= 50 && nowtime.getMinutes() <= 59 && nowtime.getHours() == (ho - 1)) {
                            //10분전 알림 & 핫타임시작
                            for (ChannelServer ch : ChannelServer.getAllInstances()) {
                                for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                                    chr.sethottimeboss(true);
                                }
                            }

                            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, (60 - nowtime.getMinutes()) + "분후 월드 보스가 시작됩니다.  자유시장 에 핫타임 보스 NPC 를 통해 어서 입장하세요."));
                        } else if (nowtime.getMinutes() == 0 && nowtime.getHours() == (ho == 24 ? 0 : ho)) {
                            //정각 핫타임 시작.
                            for (ChannelServer ch : ChannelServer.getAllInstances()) {
                                //World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, ch.getChannel() + "채널"));
                                if (ch.getChannel() == 1){
                                   if (ch.getMapFactory().getMap(ServerConstants.worldbossmap).getMonsterById(ServerConstants.worldboss) == null) { // 필드에 월드보스 몹이 없을 경우
                                       MapleMonster mob = MapleLifeFactory.getMonster(ServerConstants.worldboss);
                                       ch.getMapFactory().getMap(ServerConstants.worldbossmap).spawnMonsterOnGroundBelow(mob, new Point(-48, 215));//월드보스 좌표수정
                                   }
                                   //여기가 소환 부분이거든 
                                }
                                for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                                    chr.sethottimeboss(false);
                                    chr.sethottimebossattackcheck(false);
                                    chr.sethottimebosslastattack(false);
                                }
                            }
                            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "[MapleStory]월드 보스가 시작되었습니다. 데미지를 더 많이 누적 시켜 막타 보너스를 챙기세요!"));
                        } else if (nowtime.getMinutes() == 30 && nowtime.getHours() == (ho == 24 ? 0 : ho)) {
                            MapleMap map = ChannelServer.getInstance(1).getMapFactory().getMap(ServerConstants.worldbossmap);
                            if (map != null) {
                                for (MapleCharacter chr : map.getCharactersThreadsafe()) {
                                    MapleMap map2 = ChannelServer.getInstance(1).getMapFactory().getMap(ServerConstants.worldbossfirstmap);
                                    chr.changeMap(map2);
                                    chr.dropMessage(6, "월드 보스가 종료 됐습니다.");

                                    if (ServerConstants.worldbossmap == chr.getMapId()) {
                                        chr.sethottimeboss(false);
                                        chr.getClient().getSession().write(MaplePacketCreator.sendDuey((byte) 28, null, null));
                                        handling.world.World.Broadcast.sendPacket(chr.getId(), tools.MaplePacketCreator.serverNotice(5, "월드보스 참여 보상이 지급 됐습니다."));
                                        DueyHandler.addNewItemToDb(failitem, pice, chr.getId(), "[실패]", "월드보스 보상이 지급 됐습니다.", true);
                                    }
                                }
                            }
                            ChannelServer.getInstance(1).getMapFactory().getMap(ServerConstants.worldbossmap).resetNPCs();
                        }
                    }
                }
            }, 1000 * 60, schedulewait);//2400000L
        }
    }
    
    public static void eventboss() {
        //final int hour[] = {20,22};//이벤트보스 시간
        final int[] hour = ServerConstants.eventhour;
        ArrayList<Long> asd = new ArrayList<Long>();
        //final int failitem = ServerConstants.failitem;
        //final int pice = ServerConstants.pice;

        if (eventboss == null) {
            int schedulewait = 0;
            for (int ho : hour) {
                Calendar cal = Calendar.getInstance();
                long time = cal.getTimeInMillis();
                if (time > System.currentTimeMillis()) {
                    asd.add(time - System.currentTimeMillis());
                } else {
                    while (true) {
                        cal.add(Calendar.SECOND, 1);
                            if (cal.getTimeInMillis() > System.currentTimeMillis() && cal.getTime().getHours() == (ho - 1) && cal.getTime().getMinutes() >= 50 && cal.getTime().getSeconds() == 0) {
                                asd.add(time - System.currentTimeMillis());
                                break;
                            }
                        //if (asd.get(schedulewait) > 0) {
                            //break;
                        //}
                    }
                }
                schedulewait++;
            }
            int i = 0;
            for (final int ho : hour) {
            eventboss = Timer.WorldTimer.getInstance().register(new Runnable() {
                public void run() {
                    Date nowtime = new Date();
                        if (nowtime.getMinutes() >= 50 && nowtime.getMinutes() <= 59 && nowtime.getHours() == (ho - 1)) {
                            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, (60 - nowtime.getMinutes()) + "분후 랜덤 보스가 소환됩니다. 랜덤 → (헤네시스,엘리니아,페리온,커닝시티,오르비스)"));
                        } else if (nowtime.getMinutes() == 0 && nowtime.getHours() == (ho == 24 ? 0 : ho)) {
                            //정각 핫타임 시작.
                            for (ChannelServer ch : ChannelServer.getAllInstances()) {
                                if (ch.getChannel() == 1) {
                                    MapleMonster mob = MapleLifeFactory.getMonster(ServerConstants.eventboss);
                                    //for (int map = 0; map < ServerConstants.eventmap.length; map++)
                                    
                                    int randomizer = (int)Math.floor(Math.random() * ServerConstants.eventPos.length);
                                    MapleMap map = ch.getMapFactory().getMap(ServerConstants.eventmap[randomizer]);
                                    if (map != null) {
                                        List<MaplePortal> pts = new ArrayList<>(map.getPortals());
                                        Collections.shuffle(pts); //랜덤으로 섞기
                                        
                                        Point ptSpawn = null;
                                        for (MaplePortal pt : pts) {
                                            if (pt != null) {
                                                if (ptSpawn == null) {
                                                    ptSpawn = map.calcPointBelow(pt.getPosition());
                                                }
                                            }
                                        }
                                        map.spawnMonsterOnGroundBelow(mob, ptSpawn);// 월드보스 몬스터 좌표 설정
                                    }
                                }
                                for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                                    chr.seteventboss(false);
                                    chr.seteventbossattackcheck(false);
                                    chr.seteventbosslastattack(false);
                                }
                            }
                            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "랜덤 보스가 메이플 월드 어느 곳 에 소환되었습니다!"));
                        }
                }
                }, 1000 * 60, asd.get(i));//2400000L
            i++;
            }
        }
    }
}
