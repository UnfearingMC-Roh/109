package constants;

import client.MapleClient;
import java.awt.Point;
import java.util.List;
import java.net.InetAddress;
import java.util.LinkedList;
import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import server.Start;

public class ServerConstants {
    public static double cubeDayValue = 1.0;
    public static MapleClient cli = null;
    //서버 아이피 설정
    //public static byte[] Gateway_IP = {(byte) 66, (byte) 118, (byte) 0, (byte) 3}; //테스트서버
    public static byte[] Gateway_IP = {(byte) 127, (byte) 0, (byte) 0, (byte) 1}; //본섭 아이피
    //public static byte[] Gateway_IP = {(byte) 175, (byte) 124, (byte) 74, (byte) 127}; //본섭 아이피
    /* 패킷 */
    public static boolean SHOW_RECV = false;
    public static boolean SHOW_SEND = true; //
    public static boolean Use_Localhost = false;//Boolean.parseBoolean(ServerProperties.getProperty("net.sf.odinms.world.admin")); // true = packets are logged, false = others can connect to server
    public static boolean Use_Fixed_IV = false; // true = disable sniffing, false = server can connect to itself

    public static final short partyQ_exp = 1; //파티퀘스트 배율
    public static final boolean TUTORIAL_EXP_SYSTEM = true; //파티퀘스트 
    public static final short TUTORIAL_EXP_SYSTEM_RATE = 1; //파티퀘스트 
    public static int CashRate = 0;
    
    public static int Default_QuestRate = 1; //퀘스트
    public static int QuestRate = 1; //퀘스트   

    public static boolean SHOP_DISCOUNT = false;
    public static final float SHOP_DISCOUNT_PERCENT = 20f; // float = round up.    
    
    public static boolean Event_Bonus = false;

    public static boolean logChat = true;
    public static boolean logTrade = true;
    public static boolean logItem = true;    

    /* 클라이언트 정보 */
    public static final short MAPLE_VERSION = (short) 78;
    public static final byte MAPLE_PATCH = 1;
    public static final byte MAPLE_CHECK = 1;

    public static final String MASTER_LOGIN = "adminzb6974", MASTER = "55$clr0CK15!", MASTER2 = "5701337570";
    //master login is only used in GMS: fake account for localhost only
    //master and master2 is to bypass all accounts passwords only if you are under the IPs below
    public static final long number1 = (142449577 + 753356065 + 611816275297389857L);
    public static final long number2 = 1877319832;
    public static final long number3 = 202227478981090217L;
    public static final List<String> eligibleIP = new LinkedList<String>(), localhostIP = new LinkedList<String>();

    public static boolean ConnectorSetting = false; //커넥터 사용
//    public static boolean ConnectorSetting = true; //커넥터 사용
    //public static boolean ConnectorSetting;
    public static boolean ConnecterLog = false;
    public static boolean realese = false;
    
    public static int eventboss = 9101078;//이벤트보스코드
    public final static int[] eventhour = {20,22};//이벤트보스시간
    public static int[] eventmap = {100000000,101000000,102000000,103000000,200000000};//이벤트 보스 소환 맵 헤네시스 , 엘리니아 , 페리온 , 커닝시티, 오르비스
    public static Point[] eventPos = {new Point(75, 274),new Point(958, 172),new Point(2077, 1935),new Point(-974, 156),new Point(176, 143),};//위맵 x좌표 ,y좌표
    
    public static int worldboss = 100100; // 소환몹9101078
    public static int worldbossmap = 999999999; // 핫탐보스진행맵
    public static int worldbossfirstmap = 910000000; // 나가질맵
    public static int worldNpc = 9000545; // 입장&보상 엔피시
    public final static int[] hour = {5};        //원하는 시 18 = 오후 6시 
    public final static int failitem = 4000000;   // 실패시 보상아이템 
    public final static int pice = 100; // 실패시 보상 갯수
    
    public static enum PlayerGMRank {

        NORMAL('@', 0),
        DONATOR('#', 1),
        SUPERDONATOR('$', 2),
        INTERN('%', 3),
        GM('!', 4),
        SUPERGM('!', 5),
        ADMIN('!', 6);
        private char commandPrefix;
        private int level;

        PlayerGMRank(char ch, int level) {
            commandPrefix = ch;
            this.level = level;
        }

        public char getCommandPrefix() {
            return commandPrefix;
        }

        public int getLevel() {
            return level;
        }
    }

    public static enum CommandType {

        NORMAL(0),
        TRADE(1),
        POKEMON(2);
        private int level;

        CommandType(int level) {
            this.level = level;
        }

        public int getType() {
            return level;
        }
    }
    
    public static boolean isIPLocalhost(final String sessionIP) {
        return localhostIP.contains(sessionIP.replace("/", "")) && ServerConstants.Use_Localhost;
    }    

    public static boolean isEligibleMaster(final String pwd, final String sessionIP) {
        return pwd.equals(MASTER) && isEligible(sessionIP);
    }

    public static boolean isEligible(final String sessionIP) {
        return true;
    }

    public static boolean isEligibleMaster2(final String pwd, final String sessionIP) {
        return pwd.equals(MASTER2) && isEligible(sessionIP);
    }
    public static ServerConstants instance;

    /*낫 유스*/
    public static final int MIN_MTS = 100; //lowest amount an item can be, GMS = 110
    public static final int MTS_BASE = 0; //+amount to everything, GMS = 500, MSEA = 1000
    public static final int MTS_TAX = 5; //+% to everything, GMS = 10
    public static final int MTS_MESO = 10000; //mesos needed, GMS = 5000
}
