package constants;

import client.MapleCharacter;
import client.MapleClient;
import client.PlayerStats;
import client.Skill;
import client.SkillFactory;
import client.inventory.MapleInventoryType;
import client.inventory.MapleWeaponType;
import client.status.MonsterStatus;
import handling.channel.ChannelServer;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Map;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.Randomizer;
import server.RateManager;
import server.life.MapleMonster;
import server.maps.MapleMapObjectType;
import tools.FileoutputUtil;

public class GameConstants {

    public static boolean GMS = false; //true = GMS
    public static String[] blockchat = {"메인", "한국", ".com", ".kr", "온라인", "올드", "플래닛", "화이트", "스타", "익스트림", "서버", "스토리", "CT", "ct", "엔진"};
    public static final List<MapleMapObjectType> rangedMapobjectTypes = Collections.unmodifiableList(Arrays.asList(
            MapleMapObjectType.ITEM,
            MapleMapObjectType.MONSTER,
            MapleMapObjectType.DOOR,
            MapleMapObjectType.REACTOR,
            MapleMapObjectType.SUMMON,
            MapleMapObjectType.NPC,
            MapleMapObjectType.MIST,
            MapleMapObjectType.FAMILIAR,
            MapleMapObjectType.EXTRACTOR));
    private static final int[] exp = {
        0, 15, 34, 57, 92, 135, 372, 560, 840, 1242, 1490,//0~10ㅇ
        1788, 2146, 2575, 3090, 3708, 4450, 5340, 6408, 7690, 9228,//11~20ㅇ
        11074, 13289, 15947, 19136, 22963, 27556, 33067, 39680, 47616, 57139,//21~30ㅇ
        68567, 82280, 98736, 118483, 130331, 143364, 157700, 173470, 190817, 209899,//31~40ㅇ
        230889, 253978, 279376, 307314, 338040, 371850, 409035, 449939, 494933, 544426,//41~50ㅇ
        598869, 658756, 724632, 797095, 876805, 964486, 1060935, 1167029, 1283732, 1353054,//51~60ㅇ
        1426119, 1503129, 1584298, 1669850, 1760022, 1855063, 1955236, 2060819, 2172103, 2289397,//61~70ㅇ
        2413024, 2543327, 2680667, 2825423, 2977996, 3138808, 3308304, 3486952, 3675247, 3873710,//71~80ㅇ
        4082890, 4303366, 4535748, 4780678, 5038835, 5310932, 5597722, 5899999, 6218599, 6554403,//81~90ㅇ
        6908341, 7281391, 7674586, 8089014, 8525821, 8986215, 9471471, 9982930, 10522008, 11090196,//91~100ㅇ
        11689067, 12320277, 12985572, 13686793, 14425880, 15204878, 16025941, 16891342, 17803474, 18764862,//101~110ㅇ
        19778165, 20846186, 21971880, 23158362, 24408914, 25726995, 27116253, 28580531, 30123880, 31768644,//111~120ㅇ
        33503212, 35332487, 37261641, 39296127, 41441696, 43704413, 46090674, 48607225, 51261179, 54060039,//121~130ㅇ
        57011717, 60124557, 63407358, 66869400, 70520469, 74370887, 78431537, 82713899, 87230078, 91992840,//131~140ㅇ
        97015649, 102312703, 107898977, 113790261, 120003209, 126555384, 133465308, 140752514, 148437601, 156542294,//141~150ㅇ
        165089503, 174103390, 183609435, 193634510, 204206954, 215356654, 227115127, 239515613, 252593165, 266384752,//151~160ㅇ
        280929359, 296268102, 312444340, 329503801, 347494709, 366467920, 386477068, 407578716, 429832514, 453301369,//161~170ㅇ
        478051624, 504153243, 531680010, 560709739, 591324491, 623610808, 657659958, 693568192, 731437015, 771373476,//171~180ㅇ
        813490468, 857907048, 904748773, 954148056, 1006244540, 1061185492, 1119126220, 1180230512, 1244671098, 1312630140,//181~190ㅇ
        1384299746, 1459882512, 1539592097, 1623653825, 1712305324, 1805797195, 1904393722, 2008373619, 2118030819, 2147483647};//191~200ㅇ
    private static final int[] closeness = {0, 1, 3, 6, 14, 31, 60, 108, 181, 287, 434, 632, 891, 1224, 1642, 2161, 2793,
        3557, 4467, 5542, 6801, 8263, 9950, 11882, 14084, 16578, 19391, 22547, 26074,
        30000};
    private static final int[] setScore = {0, 10, 100, 300, 600, 1000, 2000, 4000, 7000, 10000};
    private static final int[] cumulativeTraitExp = {0, 20, 46, 80, 124, 181, 255, 351, 476, 639, 851, 1084,
        1340, 1622, 1932, 2273, 2648, 3061, 3515, 4014, 4563, 5128,
        5710, 6309, 6926, 7562, 8217, 8892, 9587, 10303, 11040, 11788,
        12547, 13307, 14089, 14883, 15689, 16507, 17337, 18179, 19034, 19902,
        20783, 21677, 22584, 23505, 24440, 25399, 26362, 27339, 28331, 29338,
        30360, 31397, 32450, 33519, 34604, 35705, 36823, 37958, 39110, 40279,
        41466, 32671, 43894, 45135, 46395, 47674, 48972, 50289, 51626, 52967,
        54312, 55661, 57014, 58371, 59732, 61097, 62466, 63839, 65216, 66597,
        67982, 69371, 70764, 72161, 73562, 74967, 76376, 77789, 79206, 80627,
        82052, 83481, 84914, 86351, 87792, 89237, 90686, 92139, 93596, 96000};
    private static final int[] mobHpVal = {0, 15, 20, 25, 35, 50, 65, 80, 95, 110, 125, 150, 175, 200, 225, 250, 275, 300, 325, 350,
        375, 405, 435, 465, 495, 525, 580, 650, 720, 790, 900, 990, 1100, 1200, 1300, 1400, 1500, 1600, 1700, 1800,
        1900, 2000, 2100, 2200, 2300, 2400, 2520, 2640, 2760, 2880, 3000, 3200, 3400, 3600, 3800, 4000, 4300, 4600, 4900, 5200,
        5500, 5900, 6300, 6700, 7100, 7500, 8000, 8500, 9000, 9500, 10000, 11000, 12000, 13000, 14000, 15000, 17000, 19000, 21000, 23000,
        25000, 27000, 29000, 31000, 33000, 35000, 37000, 39000, 41000, 43000, 45000, 47000, 49000, 51000, 53000, 55000, 57000, 59000, 61000, 63000,
        65000, 67000, 69000, 71000, 73000, 75000, 77000, 79000, 81000, 83000, 85000, 89000, 91000, 93000, 95000, 97000, 99000, 101000, 103000,
        105000, 107000, 109000, 111000, 113000, 115000, 118000, 120000, 125000, 130000, 135000, 140000, 145000, 150000, 155000, 160000, 165000, 170000, 175000, 180000,
        185000, 190000, 195000, 200000, 205000, 210000, 215000, 220000, 225000, 230000, 235000, 240000, 250000, 260000, 270000, 280000, 290000, 300000, 310000, 320000,
        330000, 340000, 350000, 360000, 370000, 380000, 390000, 400000, 410000, 420000, 430000, 440000, 450000, 460000, 470000, 480000, 490000, 500000, 510000, 520000,
        530000, 550000, 570000, 590000, 610000, 630000, 650000, 670000, 690000, 710000, 730000, 750000, 770000, 790000, 810000, 830000, 850000, 870000, 890000, 910000};
    private static final int[] pvpExp = {0, 3000, 6000, 12000, 24000, 48000, 960000, 192000, 384000, 768000};
    private static final int[] guildexp = {0, 20000, 160000, 540000, 1280000, 2500000, 4320000, 6860000, 10240000, 14580000};
    private static final int[] mountexp = {0, 6, 25, 50, 105, 134, 196, 254, 263, 315, 367, 430, 543, 587, 679, 725, 897, 1146, 1394, 1701, 2247,
        2543, 2898, 3156, 3313, 3584, 3923, 4150, 4305, 4550};
    public static final int[] itemBlock = {4001168, 5220013, 3993003, 2340000, 2049100, 4001129, 2040037, 2040006, 2040007, 2040303, 2040403, 2040506, 2040507, 2040603, 2040709, 2040710, 2040711, 2040806, 2040903, 2041024, 2041025, 2043003, 2043103, 2043203, 2043303, 2043703, 2043803, 2044003, 2044103, 2044203, 2044303, 2044403, 2044503, 2044603, 2044908, 2044815, 2044019, 2044703, 2040211, 2040212};
    public static final int[] cashBlock = {4000000}; //miracle cube and stuff
    public static final int JAIL = 180000002, MAX_BUFFSTAT = 4;
    public static final int[] blockedSkills = {};
    public static final String[] RESERVED = {"Rental", "Donor"};
    public static final String[] stats = {"tuc", "reqLevel", "reqJob", "reqSTR", "reqDEX", "reqINT", "reqLUK", "reqPOP", "cash", "cursed", "success", "setItemID", "equipTradeBlock", "durability", "randOption", "randStat", "masterLevel", "reqSkillLevel", "elemDefault", "incRMAS", "incRMAF", "incRMAI", "incRMAL", "canLevel", "skill", "pickupAll", "pickupItem", "sweepForDrop", "longRange", "consumeHP", "consumeMP", "noRevive"};
    public static final int[] hyperTele = {310000000, 220000000, 100000000, 250000000, 240000000, 104000000, 103000000, 102000000, 101000000, 120000000, 260000000, 200000000, 230000000};
    private static final int[] prmaps = {990000500, 209000001, 209000002, 209000003, 209000004, 209000005, 209000006, 209000007, 209000008, 209000009, 209000010, 209000011, 209000012, 209000013, 209000014, 209000015};

    public static boolean isPickupRestrictedMap(int map) {
        for (int i : prmaps) {
            if (i == map) {
                return true;
            }
        }
        return false;
    }

    public static int getExpNeededForLevel(final int level) {
        if (level < 0 || level >= exp.length) {
            return Integer.MAX_VALUE;
        }
        return exp[level];
    }

    public static int getGuildExpNeededForLevel(final int level) {
        if (level < 0 || level >= guildexp.length) {
            return Integer.MAX_VALUE;
        }
        return guildexp[level];
    }

    public static int getPVPExpNeededForLevel(final int level) {
        if (level < 0 || level >= pvpExp.length) {
            return Integer.MAX_VALUE;
        }
        return pvpExp[level];
    }

    public static int getClosenessNeededForLevel(final int level) {
        return closeness[level - 1];
    }

    public static int getMountExpNeededForLevel(final int level) {
        return mountexp[level - 1];
    }

    public static int getTraitExpNeededForLevel(final int level) {
        if (level < 0 || level >= cumulativeTraitExp.length) {
            return Integer.MAX_VALUE;
        }
        return cumulativeTraitExp[level];
    }

    public static int getSetExpNeededForLevel(final int level) {
        if (level < 0 || level >= setScore.length) {
            return Integer.MAX_VALUE;
        }
        return setScore[level];
    }

    public static int getMonsterHP(final int level) {
        if (level < 0 || level >= mobHpVal.length) {
            return Integer.MAX_VALUE;
        }
        return mobHpVal[level];
    }

    public static int getBookLevel(final int level) {
        return (int) ((5 * level) * (level + 1));
    }

    public static int getTimelessRequiredEXP(final int level) {
        return 70 + (level * 10);
    }

    public static int getReverseRequiredEXP(final int level) {
        return 60 + (level * 5);
    }

    public static int getProfessionEXP(final int level) {
        return ((100 * level * level) + (level * 400)) / (GameConstants.GMS ? 2 : 1);
    }

    public static boolean isHarvesting(final int itemId) {
        return itemId >= 1500000 && itemId < 1520000;
    }

    public static int maxViewRangeSq() {
        return 2200000; // 1024 * 768
    }

    public static int maxViewRangeSq_Half() {
        return 500000; // 800 * 800
    }

    public static boolean isJobFamily(final int baseJob, final int currentJob) {
        return currentJob >= baseJob && currentJob / 100 == baseJob / 100;
    }

    public static boolean isKOC(final int job) {
        return job >= 1000 && job < 2000;
    }

    public static boolean isEvan(final int job) {
        return job == 2001 || (job >= 2200 && job <= 2218);
    }

    public static boolean isMercedes(final int job) {
        return job == 2002 || (job >= 2300 && job <= 2312);
    }

    public static boolean isDemon(final int job) {
        return job == 3001 || (job >= 3100 && job <= 3112);
    }

    public static boolean isAran(final int job) {
        return job >= 2000 && job <= 2112 && job != 2001 && job != 2002;
    }

    public static boolean isDualBlade(final int job) {
        switch (job) {
            case 430:
            case 431:
            case 432:
            case 433:
            case 434:
                return true;
        }
        return false;
    }

    public static boolean isResist(final int job) {
        return job >= 3000 && job <= 3512;
    }

    public static boolean isAdventurer(final int job) {
        return job >= 0 && job < 1000;
    }

    public static boolean isCannon(final int job) {
        return job == 1 || job == 501 || (job >= 530 && job <= 532);
    }

    public static boolean isComboSkill(final int id) {
        switch (id) {
            case 11111002:
            case 11111003:
            case 1111003:
            case 1111004:
            case 1111005:
            case 1111006:
            case 1111008://
                return false;
        }
        return true;
    }
    
     public static boolean isGMSetCustomMob(int mobid) { //보스체력바
        switch (mobid) {
            case 8820119:
                return true;
            case 9300890:
                return true;
            case 9300891:
                return true;
            case 9440025:
                return true;
            case 8840010:
                return true;
            case 8800003: // 자쿰 팔
                return true;
            case 8800000: // 자쿰 본체 1페
                return true;
            case 8800001: // 자쿰 본체 2페
                return true;
            case 8800002: // 자쿰 본체 3페
                return true;
            case 8800004:
                return true;
            case 8800005:
                return true;
            case 8800006:
                return true;
            case 8800007:
                return true;
            case 8800008: //자쿰팔8
                return true;
            case 8810024://혼테일 오른쪽대가리
                return true;
            case 8810025:
                return true; //혼테일 왼쪽대가리
            case 8810026:
                return true; // 본체인데?
                
            case 9420520: //나무새끼
                return true;
            case 9420521://나무새끼
                return true;
            case 9420522://나무새끼
                return true;
            case 6500001:
                return true;
            case 9801028:
                return true;
            case 9801029:
                return true;
            case 8880000: //매그쉑
                return true;
            case 8820021: //석상
                return true;
            case 8820022: //석상
                return true;
            case 8820023: //석상
                return true;
            case 8820024: //석상
                return true;
            case 8820020: //석상
                return true;
            case 8820019:
                return true; //돌댕이녀
            case 8820001:
                return true; //분홍뚱땡이
            case 8840000:
                return true; //반레온   
            case 8820004: //오른쪽 남자석상
                return true;
            case 8820005: //왼쪽 새
                return true;
            case 8820006: //오른쪽 새 
                return true;
            case 8820003: //왼쪽 남자석상 
                return true;
            case 8810000://왼쪽대가리
                return true;
            case 8810001:
                return true;
            case 8810002:
                return true;
            case 8810003:
                return true;
            case 8810004:
                return true;
            case 8810005:
                return true;
            case 8810006:
                return true;
            case 8810007:
                return true;
            case 8810008:
                return true;
            case 8810009: //테일이 꼬리
                return true;
            default:
                return false;
        }
    }

    public static boolean isRecoveryIncSkill(final int id) {
        switch (id) {
            case 1110000:
            case 2000000:
            case 1210000:
            case 11110000:
            case 4100002:
            case 4200001:
                return true;
        }
        return false;
    }

    public static boolean isLinkedAranSkill(final int id) {
        return getLinkedAranSkill(id) != id;
    }

    public static int getLinkedAranSkill(final int id) {
        switch (id) {
            case 35101009:
                return 35001001;
            case 21110007:
            case 21110008:
                return 21110002;
            case 21120009:
            case 21120010:
                return 21120002;
            case 4321001:
                return 4321000;
            case 33101006:
            case 33101007:
                return 33101005;
            case 33101008:
                return 33101004;
            // case 35101009:
            case 35101010:
                return 35100008;
            case 35111009:
            case 35111010:
                return 35111001;
            case 35121013:
                return 35111004;
            case 35121011:
                return 35121009;
            case 32001007:
            case 32001008:
            case 32001009:
            case 32001010:
            case 32001011:
                return 32001001;
            case 5300007:
                return 5301001;
            case 5320011:
                return 5321004;
            case 23101007:
                return 23101001;
            case 23111010:
            case 23111009:
                return 23111008;
            case 31001006:
            case 31001007:
            case 31001008:
                return 31000004;
        }
        return id;
    }

    public final static boolean isForceIncrease(int skillid) {
        switch (skillid) {
            case 31000004:
            case 31001006:
            case 31001007:
            case 31001008:

            case 30010166:
            case 30011167:
            case 30011168:
            case 30011169:
            case 30011170:
                return true;
        }
        return false;
    }

    public static boolean isBossMap(final int id) {
        switch (id) {
            case 280030000:
            case 280030001:
            case 280030010:
            case 240060200:
            case 240060210:
            case 240060201:
            case 270050100:
            case 270050110:
            case 211070100:
            case 211070102:
            case 401060100:
            case 271040100:
            case 105200110:
            case 105200111:
            case 105200210:
            case 105200610:
            case 105200410:
            case 105200810:
            case 450004150:
            case 272020200:
            case 350060160:
            case 350060180:
            case 350060200:
            case 350060161:
            case 350060181:
            case 350060201:
            case 450008400:
            case 450008550:
            case 450008650:
            case 450013700:
            case 450010500:
            case 450012200:
            case 123356788:
            case 350160100:
            case 541020800:
            case 105100300:
            case 105100400:
            case 200101500:
            case 802000611:
            case 802000211:
            case 802000821:
            case 802000824:
            case 123356785:
            case 123456788:
            case 123356783:
            case 970032600:
            case 123356781:
            case 551030200:
            case 450002250:
                return true;
        }
        return false;

    }

    public static int getBOF_ForJob(final int job) {
        return PlayerStats.getSkillByJob(12, job);
    }

    public static int getEmpress_ForJob(final int job) {
        return PlayerStats.getSkillByJob(73, job);
    }

    public static boolean isElementAmp_Skill(final int skill) {
        switch (skill) {
            case 2110001:
            case 2210001:
            case 12110001:
            case 22150000:
                return true;
        }
        return false;
    }

    public static int getMPEaterForJob(final int job) {
        switch (job) {
            case 210:
            case 211:
            case 212:
                return 2100000;
            case 220:
            case 221:
            case 222:
                return 2200000;
            case 230:
            case 231:
            case 232:
                return 2300000;
        }
        return 2100000; // Default, in case GM
    }

    public static int getJobShortValue(int job) {
        if (job >= 1000) {
            job -= (job / 1000) * 1000;
        }
        job /= 100;
        if (job == 4) { // For some reason dagger/ claw is 8.. IDK
            job *= 2;
        } else if (job == 3) {
            job += 1;
        } else if (job == 5) {
            job += 11; // 16
        }
        return job;
    }

    public static boolean isPyramidSkill(final int skill) {
        return isBeginnerJob(skill / 10000) && skill % 10000 == 1020;
    }

    public static boolean isInflationSkill(final int skill) {
        return isBeginnerJob(skill / 10000) && skill % 10000 == 1092;
    }

    public static boolean isMulungSkill(final int skill) {
        return isBeginnerJob(skill / 10000) && (skill % 10000 == 1009 || skill % 10000 == 1010 || skill % 10000 == 1011);
    }

    public static boolean isIceKnightSkill(final int skill) {
        return isBeginnerJob(skill / 10000) && (skill % 10000 == 1098 || skill % 10000 == 99 || skill % 10000 == 100 || skill % 10000 == 103 || skill % 10000 == 104 || skill % 10000 == 1105);
    }

    public static boolean isThrowingStar(final int itemId) {
        return itemId / 10000 == 207;
    }

    public static boolean isBullet(final int itemId) {
        return itemId / 10000 == 233;
    }

    public static boolean isRechargable(final int itemId) {
        return isThrowingStar(itemId) || isBullet(itemId);
    }

    public static boolean isOverall(final int itemId) {
        return itemId / 10000 == 105;
    }

    public static boolean isPet(final int itemId) {
        return itemId / 10000 == 500;
    }

    public static boolean isArrowForCrossBow(final int itemId) {
        return itemId >= 2061000 && itemId < 2062000;
    }

    public static boolean isArrowForBow(final int itemId) {
        return itemId >= 2060000 && itemId < 2061000;
    }

    public static boolean isMagicWeapon(final int itemId) {
        final int s = itemId / 10000;
        return s == 137 || s == 138;
    }

    public static boolean isWeapon(final int itemId) {
        return itemId >= 1300000 && itemId < 1500000;
    }

    public static MapleInventoryType getInventoryType(final int itemId) {
        final byte type = (byte) (itemId / 1000000);
        if (type < 1 || type > 5) {
            return MapleInventoryType.UNDEFINED;
        }
        return MapleInventoryType.getByType(type);
    }

    public static MapleWeaponType getWeaponType(final int itemId) {
        int cat = itemId / 10000;
        cat = cat % 100;
        switch (cat) {
            case 30:
                return MapleWeaponType.SWORD1H;
            case 31:
                return MapleWeaponType.AXE1H;
            case 32:
                return MapleWeaponType.BLUNT1H;
            case 33:
                return MapleWeaponType.DAGGER;
            case 34:
                return MapleWeaponType.KATARA;
            case 35:
                return MapleWeaponType.MAGIC_ARROW;
            case 37:
                return MapleWeaponType.WAND;
            case 38:
                return MapleWeaponType.STAFF;
            case 40:
                return MapleWeaponType.SWORD2H;
            case 41:
                return MapleWeaponType.AXE2H;
            case 42:
                return MapleWeaponType.BLUNT2H;
            case 43:
                return MapleWeaponType.SPEAR;
            case 44:
                return MapleWeaponType.POLE_ARM;
            case 45:
                return MapleWeaponType.BOW;
            case 46:
                return MapleWeaponType.CROSSBOW;
            case 47:
                return MapleWeaponType.CLAW;
            case 48:
                return MapleWeaponType.KNUCKLE;
            case 49:
                return MapleWeaponType.GUN;
            case 52:
                return MapleWeaponType.DUAL_BOW;
            case 53:
                return MapleWeaponType.CANNON;
        }
        return MapleWeaponType.NOT_A_WEAPON;
    }

    public static boolean isShield(final int itemId) {
        int cat = itemId / 10000;
        cat = cat % 100;
        return cat == 9;
    }

    public static boolean isEquip(final int itemId) {
        return itemId / 1000000 == 1;
    }

    public static boolean isCleanSlate(int itemId) {
        return itemId / 100 == 20490;
    }

    public static boolean isAccessoryScroll(int itemId) {
        return itemId / 100 == 20492;
    }

    public static boolean isChaosScroll(int itemId) {
        if ((itemId >= 2049105 && itemId <= 2049110)/* || itemId == 2049123*/) {
            return false;
        }
        return itemId / 100 == 20491 || itemId == 2040126;
    }

    public static int getChaosNumber(int itemId) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (ii.getName(itemId).contains("놀라운")) {
            return 6;
        } else {
            return 5;
        }
    }

    public static boolean isEquipScroll(int scrollId) {
        return scrollId / 100 == 20493;
    }

    public static boolean isPotentialScroll(int scrollId) {
        return scrollId / 100 == 20494 || scrollId == 5534000;
    }

    public static boolean isSpecialScroll(final int scrollId) {
        switch (scrollId) {
            case 2040727: // Spikes on show
            case 2041058: // Cape for Cold protection
            case 2530000:
            case 2530001:
            case 2531000:
            case 5063000:
            case 5064000:
                return true;
        }
        return false;
    }

    public static boolean isTwoHanded(final int itemId) {
        switch (getWeaponType(itemId)) {
            case AXE2H:
            case GUN:
            case KNUCKLE:
            case BLUNT2H:
            case BOW:
            case CLAW:
            case CROSSBOW:
            case POLE_ARM:
            case SPEAR:
            case SWORD2H:
            case CANNON:
                //case DUAL_BOW: //magic arrow
                return true;
            default:
                return false;
        }
    }

    public static boolean isTownScroll(final int id) {
        return id >= 2030000 && id < 2040000;
    }

    public static boolean isUpgradeScroll(final int id) {
        return id >= 2040000 && id < 2050000;
    }

    public static boolean isGun(final int id) {
        return id >= 1492000 && id < 1500000;
    }

    public static boolean isUse(final int id) {
        return id >= 2000000 && id < 3000000;
    }

    public static boolean isSummonSack(final int id) {
        return id / 10000 == 210;
    }

    public static boolean isMonsterCard(final int id) {
        return id / 10000 == 238;
    }

    public static boolean isSpecialCard(final int id) {
        return id / 1000 >= 2388;
    }

    public static int getCardShortId(final int id) {
        return id % 10000;
    }

    public static boolean isGem(final int id) {
        return id >= 4250000 && id <= 4251402;
    }

    public static boolean isOtherGem(final int id) {
        switch (id) {
            case 4001174:
            case 4001175:
            case 4001176:
            case 4001177:
            case 4001178:
            case 4001179:
            case 4001180:
            case 4001181:
            case 4001182:
            case 4001183:
            case 4001184:
            case 4001185:
            case 4001186:
            case 4031980:
            case 2041058:
            case 2040727:
            case 1032062:
            case 4032334:
            case 4032312:
            case 1142156:
            case 1142157:
                return true; //mostly quest items
        }
        return false;
    }

    public static boolean isCustomQuest(final int id) {
        return id > 99999;
    }

    public static int getTaxAmount(final int meso) {
        if (meso >= 100000000) {
            return (int) Math.round(0.06 * meso);
        } else if (meso >= 25000000) {
            return (int) Math.round(0.05 * meso);
        } else if (meso >= 10000000) {
            return (int) Math.round(0.04 * meso);
        } else if (meso >= 5000000) {
            return (int) Math.round(0.03 * meso);
        } else if (meso >= 1000000) {
            return (int) Math.round(0.018 * meso);
        } else if (meso >= 100000) {
            return (int) Math.round(0.008 * meso);
        }
        return 0;
    }

    public static int EntrustedStoreTax(final int meso) {
        if (meso >= 100000000) {
            return (int) Math.round(0.03 * meso);
        } else if (meso >= 25000000) {
            return (int) Math.round(0.025 * meso);
        } else if (meso >= 10000000) {
            return (int) Math.round(0.02 * meso);
        } else if (meso >= 5000000) {
            return (int) Math.round(0.015 * meso);
        } else if (meso >= 1000000) {
            return (int) Math.round(0.009 * meso);
        } else if (meso >= 100000) {
            return (int) Math.round(0.004 * meso);
        }
        return 0;
    }

    public static int getAttackDelay(final int id, final Skill skill) { //공속핵 몹몰핵 감지딜레이
        switch (id) { // Assume it's faster(2)
           // case 5221007: // 스킬코드
           //     return 99; // 딜레이
                
        //    case 5221007: // 스킬코드 
        //        return 99; // 딜레이
                
            case 3121004: // Storm of Arrow
            case 23121000:
            case 33121009:
            case 13111002: // Storm of Arrow
            case 5221004: // Rapidfire
            case 5201006: // Recoil shot/ Back stab shot
            case 35121005:
            case 35111004:
            case 35121013:
                return 40; //reason being you can spam with final assaulter
            case 14111005:
            case 4121007:
            case 5221007:
                return 99; //skip duh chek
            case 0: // Normal Attack, TODO delay for each weapon type
                return 500; // 0.5초 
        }
        if (skill != null && skill.getSkillType() == 3) {
            return 0; //final attack
        }
        if (skill != null && skill.getDelay() > 0 && !isNoDelaySkill(id)) {
            return skill.getDelay();
        }
        // TODO delay for final attack, weapon type, swing,stab etc
        return 330; // Default usually
    }

    public static byte gachaponRareItem(final int id) {
        switch (id) {
            case 2340000: // White Scroll
            case 2049100: // Chaos Scroll
            case 2049000: // Reverse Scroll
            case 2049001: // Reverse Scroll
            case 2049002: // Reverse Scroll
            case 2040006: // Miracle
            case 2040007: // Miracle
            case 2040303: // Miracle
            case 2040403: // Miracle
            case 2040506: // Miracle
            case 2040507: // Miracle
            case 2040603: // Miracle
            case 2040709: // Miracle
            case 2040710: // Miracle
            case 2040711: // Miracle
            case 2040806: // Miracle
            case 2040903: // Miracle
            case 2041024: // Miracle
            case 2041025: // Miracle
            case 2043003: // Miracle
            case 2043103: // Miracle
            case 2043203: // Miracle
            case 2043303: // Miracle
            case 2043703: // Miracle
            case 2043803: // Miracle
            case 2044003: // Miracle
            case 2044103: // Miracle
            case 2044203: // Miracle
            case 2044303: // Miracle
            case 2044403: // Miracle
            case 2044503: // Miracle
            case 2044603: // Miracle
            case 2044908: // Miracle
            case 2044815: // Miracle
            case 2044019: // Miracle
            case 2044703: // Miracle
                return 2;
            //1 = wedding msg o.o
        }
        return 0;
    }
    public final static int[] goldrewards = {
        2049400, 1,
        2049401, 2,
        2049301, 2,
        2340000, 1, // white scroll
        2070007, 2,
        2070016, 1,
        2330007, 1,
        2070018, 1, // balance fury
        1402037, 1, // Rigbol Sword
        2290096, 1, // Maple Warrior 20
        2290049, 1, // Genesis 30
        2290041, 1, // Meteo 30
        2290047, 1, // Blizzard 30
        2290095, 1, // Smoke 30
        2290017, 1, // Enrage 30
        2290075, 1, // Snipe 30
        2290085, 1, // Triple Throw 30
        2290116, 1, // Areal Strike
        1302059, 3, // Dragon Carabella
        2049100, 1, // Chaos Scroll
        1092049, 1, // Dragon Kanjar
        1102041, 1, // Pink Cape
        1432018, 3, // Sky Ski
        1022047, 3, // Owl Mask
        3010051, 1, // Chair
        3010020, 1, // Portable meal table
        2040914, 1, // Shield for Weapon Atk

        1432011, 3, // Fair Frozen
        1442020, 3, // HellSlayer
        1382035, 3, // Blue Marine
        1372010, 3, // Dimon Wand
        1332027, 3, // Varkit
        1302056, 3, // Sparta
        1402005, 3, // Bezerker
        1472053, 3, // Red Craven
        1462018, 3, // Casa Crow
        1452017, 3, // Metus
        1422013, 3, // Lemonite
        1322029, 3, // Ruin Hammer
        1412010, 3, // Colonian Axe

        1472051, 1, // Green Dragon Sleeve
        1482013, 1, // Emperor's Claw
        1492013, 1, // Dragon fire Revlover

        1382049, 1,
        1382050, 1, // Blue Dragon Staff
        1382051, 1,
        1382052, 1,
        1382045, 1, // Fire Staff, Level 105
        1382047, 1, // Ice Staff, Level 105
        1382048, 1, // Thunder Staff
        1382046, 1, // Poison Staff

        1372035, 1,
        1372036, 1,
        1372037, 1,
        1372038, 1,
        1372039, 1,
        1372040, 1,
        1372041, 1,
        1372042, 1,
        1332032, 8, // Christmas Tree
        1482025, 7, // Flowery Tube

        4001011, 8, // Lupin Eraser
        4001010, 8, // Mushmom Eraser
        4001009, 8, // Stump Eraser

        2047000, 1,
        2047001, 1,
        2047002, 1,
        2047100, 1,
        2047101, 1,
        2047102, 1,
        2047200, 1,
        2047201, 1,
        2047202, 1,
        2047203, 1,
        2047204, 1,
        2047205, 1,
        2047206, 1,
        2047207, 1,
        2047208, 1,
        2047300, 1,
        2047301, 1,
        2047302, 1,
        2047303, 1,
        2047304, 1,
        2047305, 1,
        2047306, 1,
        2047307, 1,
        2047308, 1,
        2047309, 1,
        2046004, 1,
        2046005, 1,
        2046104, 1,
        2046105, 1,
        2046208, 1,
        2046209, 1,
        2046210, 1,
        2046211, 1,
        2046212, 1,
        //list
        1132014, 3,
        1132015, 2,
        1132016, 1,
        1002801, 2,
        1102205, 2,
        1332079, 2,
        1332080, 2,
        1402048, 2,
        1402049, 2,
        1402050, 2,
        1402051, 2,
        1462052, 2,
        1462054, 2,
        1462055, 2,
        1472074, 2,
        1472075, 2,
        //pro raven
        1332077, 1,
        1382082, 1,
        1432063, 1,
        1452087, 1,
        1462053, 1,
        1472072, 1,
        1482048, 1,
        1492047, 1,
        2030008, 5, // Bottle, return scroll
        1442018, 3, // Frozen Tuna
        2040900, 4, // Shield for DEF
        2049100, 10,
        2000005, 10, // Power Elixir
        2000004, 10, // Elixir
        4280000, 8,
        //2430144, 10,
        2290285, 10,
        2028061, 10,
        2028062, 10,
        2530000, 5,
        2531000, 5}; // Gold Box
    public final static int[] silverrewards = {
        2049401, 2,
        2049301, 2,
        3010041, 1, // skull throne
        1002452, 6, // Starry Bandana
        1002455, 6, // Starry Bandana
        2290084, 1, // Triple Throw 20
        2290048, 1, // Genesis 20
        2290040, 1, // Meteo 20
        2290046, 1, // Blizzard 20
        2290074, 1, // Sniping 20
        2290064, 1, // Concentration 20
        2290094, 1, // Smoke 20
        2290022, 1, // Berserk 20
        2290056, 1, // Bow Expert 30
        2290066, 1, // xBow Expert 30
        2290020, 1, // Sanc 20
        1102082, 1, // Black Raggdey Cape
        1302049, 1, // Glowing Whip
        2340000, 1, // White Scroll
        1102041, 1, // Pink Cape
        1452019, 2, // White Nisrock
        4001116, 3, // Hexagon Pend
        4001012, 3, // Wraith Eraser
        1022060, 2, // Foxy Racoon Eye
        //2430144, 5,
        2290285, 5,
        2028062, 5,
        2028061, 5,
        2530000, 1,
        2531000, 1,
        2041100, 1,
        2041101, 1,
        2041102, 1,
        2041103, 1,
        2041104, 1,
        2041105, 1,
        2041106, 1,
        2041107, 1,
        2041108, 1,
        2041109, 1,
        2041110, 1,
        2041111, 1,
        2041112, 1,
        2041113, 1,
        2041114, 1,
        2041115, 1,
        2041116, 1,
        2041117, 1,
        2041118, 1,
        2041119, 1,
        2041300, 1,
        2041301, 1,
        2041302, 1,
        2041303, 1,
        2041304, 1,
        2041305, 1,
        2041306, 1,
        2041307, 1,
        2041308, 1,
        2041309, 1,
        2041310, 1,
        2041311, 1,
        2041312, 1,
        2041313, 1,
        2041314, 1,
        2041315, 1,
        2041316, 1,
        2041317, 1,
        2041318, 1,
        2041319, 1,
        2049200, 1,
        2049201, 1,
        2049202, 1,
        2049203, 1,
        2049204, 1,
        2049205, 1,
        2049206, 1,
        2049207, 1,
        2049208, 1,
        2049209, 1,
        2049210, 1,
        2049211, 1,
        1432011, 3, // Fair Frozen
        1442020, 3, // HellSlayer
        1382035, 3, // Blue Marine
        1372010, 3, // Dimon Wand
        1332027, 3, // Varkit
        1302056, 3, // Sparta
        1402005, 3, // Bezerker
        1472053, 3, // Red Craven
        1462018, 3, // Casa Crow
        1452017, 3, // Metus
        1422013, 3, // Lemonite
        1322029, 3, // Ruin Hammer
        1412010, 3, // Colonian Axe

        1002587, 3, // Black Wisconsin
        1402044, 1, // Pumpkin lantern
        2101013, 4, // Summoning Showa boss
        1442046, 1, // Super Snowboard
        1422031, 1, // Blue Seal Cushion
        1332054, 3, // Lonzege Dagger
        1012056, 3, // Dog Nose
        1022047, 3, // Owl Mask
        3012002, 1, // Bathtub
        1442012, 3, // Sky snowboard
        1442018, 3, // Frozen Tuna
        1432010, 3, // Omega Spear
        1432036, 1, // Fishing Pole
        2000005, 10, // Power Elixir
        2049100, 10,
        2000004, 10, // Elixir
        4280001, 8}; // Silver Box
    public final static int[] peanutsRiding = {//피넛머신 템변경하는곳
    };//mounts 
    public final static int[] peanuts = {//코드,확률 (숫자 높을수록 잘나옴)
        1152000, 3, 1152001, 1, 1152009, 1, //호랑이 발톱
        1012058, 1, 1012059, 1, 1012060, 1, 1012061, 1,
        2049400, 3, 2049401, 5, 2049301, 2, 2049300, 5,
        1442106, 3, 1000040, 3, 1082276, 3, 1050169, 3, 1001060, 4, 1051210, 3, 1102246, 3, 1072447, 3,//설빙세트
        //1112432, 2, 1112434, 2, //탕아,선도부반지,
        //4001208, 2, 4001209, 1, 4001210, 1, 4001211, 1, 4001212, 1, //주화
        //1112586, 1, 1112585, 1,//닼엔블,엔젤릭
        //1142249, 1,//럭키가이        
        1442057, 1,//보핑
        1382016, 1,//표고버섯     
        1302106, 1,//크리스탈 카타나
        1302107, 1,//블랙 크리스탈 카타나
        1402037, 1,//스톤투스
        1332030, 1,//부채
        1372008, 1,//히노마루 부채
        1402256, 1,//카스미기리노 카타나
        2049100, 3,//혼줌
        2430191, 2,//프로텍트 쉴드        
        2049116, 3,//놀혼줌
        //2430144, 3,//신마북
        1942002, 2,
        1952002, 2,
        1962002, 2,
        1972002, 2, //에반 장비
        1382045, 1,
        1382046, 1,
        1382047, 1,
        1382048, 1, //엘리멘탈스태프
        1612004, 2,
        1622004, 2,
        1632004, 2,
        1642004, 2,
        1652004, 2, //메카닉 장비
        //백줌
        2049008, 5,//저백줌
        2049007, 5,
        2049006, 5,
        2049005, 5, //백줌20%
        2070007, 3,//화비
        2330005, 3,//이터널불릿   
        2070016, 1,
        2020013, 10,
        2020015, 10,
        2020014, 10,
        5530004, 10,
        5530006, 10,
        2470000, 1,//황금망치
        2470001, 1,//황금망치
        2433928, 10,
        5530068, 5,};

    public static int[] eventCommonReward = {
        0, 10,
        1, 10,
        4, 5,
        5060004, 25,
        4170024, 25,
        4280000, 5,
        4280001, 6,
        5490000, 5,
        5490001, 6
    };
    public static int[] eventUncommonReward = {
        1, 4,
        2, 8,
        3, 8,
        2022179, 5,
        5062000, 20,
        2430082, 20,
        2430092, 20,
        2022459, 2,
        2022460, 1,
        2022462, 1,
        2430103, 2,
        2430117, 2,
        2430118, 2,
        2430201, 4,
        2430228, 4,
        2430229, 4,
        2430283, 4,
        2430136, 4,
        2430476, 4,
        2430511, 4,
        2430206, 4,
        2430199, 1,
        1032062, 5,
        5220000, 28,
        2022459, 5,
        2022460, 5,
        2022461, 5,
        2022462, 5,
        2022463, 5,
        5050000, 2,
        4080100, 10,
        4080000, 10,
        2049100, 10,
        //2430144, 10,
        2290285, 10,
        2028062, 10,
        2028061, 10,
        2530000, 5,
        2531000, 5,
        2041100, 1,
        2041101, 1,
        2041102, 1,
        2041103, 1,
        2041104, 1,
        2041105, 1,
        2041106, 1,
        2041107, 1,
        2041108, 1,
        2041109, 1,
        2041110, 1,
        2041111, 1,
        2041112, 1,
        2041113, 1,
        2041114, 1,
        2041115, 1,
        2041116, 1,
        2041117, 1,
        2041118, 1,
        2041119, 1,
        2041300, 1,
        2041301, 1,
        2041302, 1,
        2041303, 1,
        2041304, 1,
        2041305, 1,
        2041306, 1,
        2041307, 1,
        2041308, 1,
        2041309, 1,
        2041310, 1,
        2041311, 1,
        2041312, 1,
        2041313, 1,
        2041314, 1,
        2041315, 1,
        2041316, 1,
        2041317, 1,
        2041318, 1,
        2041319, 1,
        2049200, 1,
        2049201, 1,
        2049202, 1,
        2049203, 1,
        2049204, 1,
        2049205, 1,
        2049206, 1,
        2049207, 1,
        2049208, 1,
        2049209, 1,
        2049210, 1,
        2049211, 1
    };
    public static int[] eventRareReward = {
        2049100, 5,
        //2430144, 5,
        2290285, 5,
        2028062, 5,
        2028061, 5,
        2530000, 2,
        2531000, 2,
        2049116, 1,
        2049401, 10,
        2049301, 20,
        2049400, 3,
        2340000, 1,
        3010130, 5,
        3010131, 5,
        3010132, 5,
        3010133, 5,
        3010136, 5,
        3010116, 5,
        3010117, 5,
        3010118, 5,
        1112405, 1,
        1112445, 1,
        1022097, 1,
        //2040211, 1, 드래곤안경관련줌서
        //2040212, 1,
        2049000, 2,
        2049001, 2,
        2049002, 2,
        2049003, 2,
        1012058, 2,
        1012059, 2,
        1012060, 2,
        1012061, 2,
        2022460, 4,
        2022461, 3,
        2022462, 4,
        2022463, 3,
        2040041, 1,
        2040042, 1,
        2040334, 1,
        2040430, 1,
        2040538, 1,
        2040539, 1,
        2040630, 1,
        2040740, 1,
        2040741, 1,
        2040742, 1,
        2040829, 1,
        2040830, 1,
        2040936, 1,
        2041066, 1,
        2041067, 1,
        2043023, 1,
        2043117, 1,
        2043217, 1,
        2043312, 1,
        2043712, 1,
        2043812, 1,
        2044025, 1,
        2044117, 1,
        2044217, 1,
        2044317, 1,
        2044417, 1,
        2044512, 1,
        2044612, 1,
        2044712, 1,
        2046000, 1,
        2046001, 1,
        2046004, 1,
        2046005, 1,
        2046100, 1,
        2046101, 1,
        2046104, 1,
        2046105, 1,
        2046200, 1,
        2046201, 1,
        2046202, 1,
        2046203, 1,
        2046208, 1,
        2046209, 1,
        2046210, 1,
        2046211, 1,
        2046212, 1,
        2046300, 1,
        2046301, 1,
        2046302, 1,
        2046303, 1,
        2047000, 1,
        2047001, 1,
        2047002, 1,
        2047100, 1,
        2047101, 1,
        2047102, 1,
        2047200, 1,
        2047201, 1,
        2047202, 1,
        2047203, 1,
        2047204, 1,
        2047205, 1,
        2047206, 1,
        2047207, 1,
        2047208, 1,
        2047300, 1,
        2047301, 1,
        2047302, 1,
        2047303, 1,
        2047304, 1,
        2047305, 1,
        2047306, 1,
        2047307, 1,
        2047308, 1,
        2047309, 1,
        1112427, 5,
        1112428, 5,
        1112429, 5,
        1012240, 10,
        1022117, 10,
        1032095, 10,
        1112659, 10,
        2070007, 10,
        2330007, 5,
        2070016, 5,
        2070018, 5,
        1152038, 1,
        1152039, 1,
        1152040, 1,
        1152041, 1,
        1122090, 1,
        1122094, 1,
        1122098, 1,
        1122102, 1,
        1012213, 1,
        1012219, 1,
        1012225, 1,
        1012231, 1,
        1012237, 1,
        2070023, 5,
        2070024, 5,
        2330008, 5,
        2003516, 5,
        2003517, 1,
        1132052, 1,
        1132062, 1,
        1132072, 1,
        1132082, 1,
        1112585, 1,
        //walker
        1072502, 1,
        1072503, 1,
        1072504, 1,
        1072505, 1,
        1072506, 1,
        1052333, 1,
        1052334, 1,
        1052335, 1,
        1052336, 1,
        1052337, 1,
        1082305, 1,
        1082306, 1,
        1082307, 1,
        1082308, 1,
        1082309, 1,
        1003197, 1,
        1003198, 1,
        1003199, 1,
        1003200, 1,
        1003201, 1,
        1662000, 1,
        1662001, 1,
        1672000, 1,
        1672001, 1,
        1672002, 1,
        //crescent moon
        1112583, 1,
        1032092, 1,
        1132084, 1,
        //mounts, 90 day
        2430290, 1,
        2430292, 1,
        2430294, 1,
        2430296, 1,
        2430298, 1,
        2430300, 1,
        2430302, 1,
        2430304, 1,
        2430306, 1,
        2430308, 1,
        2430310, 1,
        2430312, 1,
        2430314, 1,
        2430316, 1,
        2430318, 1,
        2430320, 1,
        2430322, 1,
        2430324, 1,
        2430326, 1,
        2430328, 1,
        2430330, 1,
        2430332, 1,
        2430334, 1,
        2430336, 1,
        2430338, 1,
        2430340, 1,
        2430342, 1,
        2430344, 1,
        2430347, 1,
        2430349, 1,
        2430351, 1,
        2430353, 1,
        2430355, 1,
        2430357, 1,
        2430359, 1,
        2430361, 1,
        2430392, 1,
        2430512, 1,
        2430536, 1,
        2430477, 1,
        2430146, 1,
        2430148, 1,
        2430137, 1,};
    public static int[] eventSuperReward = {
        2022121, 10,
        4031307, 50,
        3010127, 10,
        3010128, 10,
        3010137, 10,
        3010157, 10,
        2049300, 10,
        2040758, 10,
        1442057, 10,
        2049402, 10,
        2049304, 1,
        2049305, 1,
        2040759, 7,
        2040760, 5,
        2040125, 10,
        2040126, 10,
        1012191, 5,
        1112514, 1, //untradable/tradable
        1112531, 1,
        1112629, 1,
        1112646, 1,
        1112515, 1, //untradable/tradable
        1112532, 1,
        1112630, 1,
        1112647, 1,
        1112516, 1, //untradable/tradable
        1112533, 1,
        1112631, 1,
        1112648, 1,
        2040045, 10,
        2040046, 10,
        2040333, 10,
        2040429, 10,
        2040542, 10,
        2040543, 10,
        2040629, 10,
        2040755, 10,
        2040756, 10,
        2040757, 10,
        2040833, 10,
        2040834, 10,
        2041068, 10,
        2041069, 10,
        2043022, 12,
        2043120, 12,
        2043220, 12,
        2043313, 12,
        2043713, 12,
        2043813, 12,
        2044028, 12,
        2044120, 12,
        2044220, 12,
        2044320, 12,
        2044520, 12,
        2044513, 12,
        2044613, 12,
        2044713, 12,
        2044817, 12,
        2044910, 12,
        2046002, 5,
        2046003, 5,
        2046102, 5,
        2046103, 5,
        2046204, 10,
        2046205, 10,
        2046206, 10,
        2046207, 10,
        2046304, 10,
        2046305, 10,
        2046306, 10,
        2046307, 10,
        2040006, 2,
        2040007, 2,
        2040303, 2,
        2040403, 2,
        2040506, 2,
        2040507, 2,
        2040603, 2,
        2040709, 2,
        2040710, 2,
        2040711, 2,
        2040806, 2,
        2040903, 2,
        2040913, 2,
        2041024, 2,
        2041025, 2,
        2044815, 2,
        2044908, 2,
        1152046, 1,
        1152047, 1,
        1152048, 1,
        1152049, 1,
        1122091, 1,
        1122095, 1,
        1122099, 1,
        1122103, 1,
        1012214, 1,
        1012220, 1,
        1012226, 1,
        1012232, 1,
        1012238, 1,
        1032088, 1,
        1032089, 1,
        1032090, 1,
        1032091, 1,
        1132053, 1,
        1132063, 1,
        1132073, 1,
        1132083, 1,
        1112586, 1,
        1112593, 1,
        1112597, 1,
        1662002, 1,
        1662003, 1,
        1672003, 1,
        1672004, 1,
        1672005, 1,
        //130, 140 weapons
        1092088, 1,
        1092089, 1,
        1092087, 1,
        1102275, 1,
        1102276, 1,
        1102277, 1,
        1102278, 1,
        1102279, 1,
        1102280, 1,
        1102281, 1,
        1102282, 1,
        1102283, 1,
        1102284, 1,
        1082295, 1,
        1082296, 1,
        1082297, 1,
        1082298, 1,
        1082299, 1,
        1082300, 1,
        1082301, 1,
        1082302, 1,
        1082303, 1,
        1082304, 1,
        1072485, 1,
        1072486, 1,
        1072487, 1,
        1072488, 1,
        1072489, 1,
        1072490, 1,
        1072491, 1,
        1072492, 1,
        1072493, 1,
        1072494, 1,
        1052314, 1,
        1052315, 1,
        1052316, 1,
        1052317, 1,
        1052318, 1,
        1052319, 1,
        1052329, 1,
        1052321, 1,
        1052322, 1,
        1052323, 1,
        1003172, 1,
        1003173, 1,
        1003174, 1,
        1003175, 1,
        1003176, 1,
        1003177, 1,
        1003178, 1,
        1003179, 1,
        1003180, 1,
        1003181, 1,
        1302152, 1,
        1302153, 1,
        1312065, 1,
        1312066, 1,
        1322096, 1,
        1322097, 1,
        1332130, 1,
        1332131, 1,
        1342035, 1,
        1342036, 1,
        1372084, 1,
        1372085, 1,
        1382104, 1,
        1382105, 1,
        1402095, 1,
        1402096, 1,
        1412065, 1,
        1412066, 1,
        1422066, 1,
        1422067, 1,
        1432086, 1,
        1432087, 1,
        1442116, 1,
        1442117, 1,
        1452111, 1,
        1452112, 1,
        1462099, 1,
        1462100, 1,
        1472122, 1,
        1472123, 1,
        1482084, 1,
        1482085, 1,
        1492085, 1,
        1492086, 1,
        1532017, 1,
        1532018, 1,
        //mounts
        2430291, 1,
        2430293, 1,
        2430295, 1,
        2430297, 1,
        2430299, 1,
        2430301, 1,
        2430303, 1,
        2430305, 1,
        2430307, 1,
        2430309, 1,
        2430311, 1,
        2430313, 1,
        2430315, 1,
        2430317, 1,
        2430319, 1,
        2430321, 1,
        2430323, 1,
        2430325, 1,
        2430327, 1,
        2430329, 1,
        2430331, 1,
        2430333, 1,
        2430335, 1,
        2430337, 1,
        2430339, 1,
        2430341, 1,
        2430343, 1,
        2430345, 1,
        2430348, 1,
        2430350, 1,
        2430352, 1,
        2430354, 1,
        2430356, 1,
        2430358, 1,
        2430360, 1,
        2430362, 1,
        //rising sun
        1012239, 1,
        1122104, 1,
        1112584, 1,
        1032093, 1,
        1132085, 1
    };
    public static int[] tenPercent = {
        //10% scrolls
        2040002,
        2040005,
        2040026,
        2040031,
        2040100,
        2040105,
        2040200,
        2040205,
        2040302,
        2040310,
        2040318,
        2040323,
        2040328,
        2040329,
        2040330,
        2040331,
        2040402,
        2040412,
        2040419,
        2040422,
        2040427,
        2040502,
        2040505,
        2040514,
        2040517,
        2040534,
        2040602,
        2040612,
        2040619,
        2040622,
        2040627,
        2040702,
        2040705,
        2040708,
        2040727,
        2040802,
        2040805,
        2040816,
        2040825,
        2040902,
        2040915,
        2040920,
        2040925,
        2040928,
        2040933,
        2041002,
        2041005,
        2041008,
        2041011,
        2041014,
        2041017,
        2041020,
        2041023,
        2041058,
        2041102,
        2041105,
        2041108,
        2041111,
        2041302,
        2041305,
        2041308,
        2041311,
        2043002,
        2043008,
        2043019,
        2043102,
        2043114,
        2043202,
        2043214,
        2043302,
        2043402,
        2043702,
        2043802,
        2044002,
        2044014,
        2044015,
        2044102,
        2044114,
        2044202,
        2044214,
        2044302,
        2044314,
        2044402,
        2044414,
        2044502,
        2044602,
        2044702,
        2044802,
        2044809,
        2044902,
        2045302,
        2048002,
        2048005
    };
    public static int[] fishingReward = {//낚시보상 숫자 높을수록 잘나옴
        0, 100, // Meso
        1, 100, // EXP
        4031209, 20, //sos편지
        4031302, 20, //바다쓰레기
        4031627, 20, // White Bait (3cm)
        4031628, 1, // Sailfish (120cm)
        4031630, 1, // Carp (30cm)
        4031631, 1, // Salmon(150cm)
        4031632, 1, // Shovel
        4031633, 20, // Whitebait (3.6cm)
        4031634, 10, // Whitebait (5cm)
        4031635, 5, // Whitebait (6.5cm)
        4031636, 2, // Whitebait (10cm)
        4031637, 2, // Carp (53cm)
        4031638, 2, // Carp (60cm)
        4031639, 1, // Carp (100cm)
        4031640, 1, // Carp (113cm)
        4031641, 2, // Sailfish (128cm)
        4031642, 2, // Sailfish (131cm)
        4031643, 1, // Sailfish (140cm)
        4031644, 1, // Sailfish (148cm)
        4031645, 2, // Salmon (166cm)
        4031646, 2, // Salmon (183cm)
        4031647, 1, // Salmon (227cm)
        4031648, 1, // Salmon (288cm)
        4001187, 5,
        4001188, 2,
        4001189, 2,
        4031629, 1 // Pot
    };

    public static boolean isReverseItem(int itemId) {
        switch (itemId) {
            case 1002790:
            case 1002791:
            case 1002792:
            case 1002793:
            case 1002794:
            case 1082239:
            case 1082240:
            case 1082241:
            case 1082242:
            case 1082243:
            case 1052160:
            case 1052161:
            case 1052162:
            case 1052163:
            case 1052164:
            case 1072361:
            case 1072362:
            case 1072363:
            case 1072364:
            case 1072365:

            case 1302086:
            case 1312038:
            case 1322061:
            case 1332075:
            case 1332076:
            case 1372045:
            case 1382059:
            case 1402047:
            case 1412034:
            case 1422038:
            case 1432049:
            case 1442067:
            case 1452059:
            case 1462051:
            case 1472071:
            case 1482024:
            case 1492025:

            case 1342012:
            case 1942002:
            case 1952002:
            case 1962002:
            case 1972002:
            case 1532016:
            case 1522017:
                return true;
            default:
                return false;
        }
    }

    public static boolean isTimelessItem(int itemId) {
        switch (itemId) {
            case 1032031: //shield earring, but technically
            case 1102172:
            case 1002776:
            case 1002777:
            case 1002778:
            case 1002779:
            case 1002780:
            case 1082234:
            case 1082235:
            case 1082236:
            case 1082237:
            case 1082238:
            case 1052155:
            case 1052156:
            case 1052157:
            case 1052158:
            case 1052159:
            case 1072355:
            case 1072356:
            case 1072357:
            case 1072358:
            case 1072359:
            case 1092057:
            case 1092058:
            case 1092059:

            case 1122011:
            case 1122012:

            case 1302081:
            case 1312037:
            case 1322060:
            case 1332073:
            case 1332074:
            case 1372044:
            case 1382057:
            case 1402046:
            case 1412033:
            case 1422037:
            case 1432047:
            case 1442063:
            case 1452057:
            case 1462050:
            case 1472068:
            case 1482023:
            case 1492023:
            case 1342011:
            case 1532015:
            case 1522016:
                //raven.
                return true;
            default:
                return false;
        }
    }

    public static boolean isRing(int itemId) {
        return itemId >= 1112000 && itemId < 1113000;
    }// 112xxxx - pendants, 113xxxx - belts

    //if only there was a way to find in wz files -.-
    public static boolean isEffectRing(int itemid) {
        return isFriendshipRing(itemid) || isCrushRing(itemid) || isMarriageRing(itemid);
    }

    public static boolean isMarriageRing(int itemId) {
        switch (itemId) {
            case 1112803:
            case 1112806:
            case 1112807:
            case 1112809:
                return true;
        }
        return false;
    }

    public static boolean isFriendshipRing(int itemId) {
        switch (itemId) {
            case 1112800:
            case 1112801:
            case 1112802:
            case 1112810: //new
            case 1112811: //new, doesnt work in friendship?
            case 1112812: //new, im ASSUMING it's friendship cuz of itemID, not sure.
            case 1112816: //new, i'm also assuming
            case 1112817:

            case 1049000:
                return true;
        }
        return false;
    }

    public static boolean isCrushRing(int itemId) {
        switch (itemId) {
            case 1112001:
            case 1112002:
            case 1112003:
            case 1112005: //new
            case 1112006: //new
            case 1112007:
            case 1112012:
            case 1112015: //new

            case 1048000:
            case 1048001:
            case 1048002:
                return true;
        }
        return false;
    }
    public static int[] Equipments_Bonus = {1122017, 1122156, 1114317}; //정령의 펜던트

    public static int Equipment_Bonus_EXP(final int itemid) {
        switch (itemid) {
            case 1122017:
            case 1122156:
                return 30;
            case 1114317:
                return 0;
        }
        return 0;
    }//블락 맵 오토루팅블락
    public static int[] blockedMaps = {701220310, 180000001, 180000002, 109050000, 280030000, 240060200, 280090000, 280030001, 240060201, 950101100, 950101010};
    public static int[] autolootblockedMaps = {105100300, 802000821, 802000801, 802000611, 802000803, 802000611, 802000111, 802000211, 802000711, 802000411, 280030000, 280030100, 240060200, 240060201, 270050100, 211070100, 211070102, 401060100, 271040100, 105200110, 105200111, 105200210, 105200610, 
                    105200410, 105200810, 450004150, 272020200, 350060160, 350060180, 350060200, 350060161, 350060181, 350060201, 280030001, 240060201,
    980000101,980000201,980000301,980000401,980000501,980000601,980000701,980000801,980000901};
    //오토루팅 금지맵
    //If you can think of more maps that could be exploitable via npc,block nao pliz!

    public static int getExpForLevel(int i, int itemId) {
        if (isReverseItem(itemId)) {
            return getReverseRequiredEXP(i);
        } else if (getMaxLevel(itemId) > 0) {
            return getTimelessRequiredEXP(i);
        }
        return 0;
    }

    public static int getMaxLevel(final int itemId) {
        Map<Integer, Map<String, Integer>> inc = MapleItemInformationProvider.getInstance().getEquipIncrements(itemId);
        return inc != null ? (inc.size()) : 0;
    }

    public static int getStatChance() {
        return 25;
    }

    public static MonsterStatus getStatFromWeapon(final int itemid) {
        switch (itemid) {
            case 1302109:
            case 1312041:
            case 1322067:
            case 1332083:
            case 1372048:
            case 1382064:
            case 1402055:
            case 1412037:
            case 1422041:
            case 1432052:
            case 1442073:
            case 1452064:
            case 1462058:
            case 1472079:
            case 1482035:
                return MonsterStatus.DARKNESS;
            case 1302108:
            case 1312040:
            case 1322066:
            case 1332082:
            case 1372047:
            case 1382063:
            case 1402054:
            case 1412036:
            case 1422040:
            case 1432051:
            case 1442072:
            case 1452063:
            case 1462057:
            case 1472078:
            case 1482036:
                return MonsterStatus.SPEED;
        }
        return null;
    }

    public static int getXForStat(MonsterStatus stat) {
        switch (stat) {
            case DARKNESS:
                return -70;
            case SPEED:
                return -50;
        }
        return 0;
    }

    public static int getSkillForStat(MonsterStatus stat) {
        switch (stat) {
            case DARKNESS:
                return 1111003;
            case SPEED:
                return 3121007;
        }
        return 0;
    }
    public final static int[] normalDrops = {
        4001009, //real
        4001010,
        4001011,
        4001012,
        4001013,
        4001014, //real
        4001021,
        4001038, //fake
        4001039,
        4001040,
        4001041,
        4001042,
        4001043, //fake
        4001038, //fake
        4001039,
        4001040,
        4001041,
        4001042,
        4001043, //fake
        4001038, //fake
        4001039,
        4001040,
        4001041,
        4001042,
        4001043, //fake
        4000164, //start
        2000000,
        2000003,
        2000004,
        2000005,
        4000019,
        4000000,
        4000016,
        4000006,
        2100121,
        4000029,
        4000064,
        5110000,
        4000306,
        4032181,
        4006001,
        4006000,
        2050004,
        3994102,
        3994103,
        3994104,
        3994105,
        2430007, //end
        4000164, //start
        2000000,
        2000003,
        2000004,
        2000005,
        4000019,
        4000000,
        4000016,
        4000006,
        2100121,
        4000029,
        4000064,
        5110000,
        4000306,
        4032181,
        4006001,
        4006000,
        2050004,
        3994102,
        3994103,
        3994104,
        3994105,
        2430007, //end
        4000164, //start
        2000000,
        2000003,
        2000004,
        2000005,
        4000019,
        4000000,
        4000016,
        4000006,
        2100121,
        4000029,
        4000064,
        5110000,
        4000306,
        4032181,
        4006001,
        4006000,
        2050004,
        3994102,
        3994103,
        3994104,
        3994105,
        2430007}; //end
    public final static int[] rareDrops = {
        2022179,
        2049100,
        2049100,
        //2430144,
        2028062,
        2028061,
        2290285,
        2049301,
        2049401,
        2022326,
        2022193,
        2049000,
        2049001,
        2049002};
    public final static int[] superDrops = {
        2040804,
        2049400,
        2028062,
        2028061,
        //2430144,
        //2430144,
        //2430144,
        //2430144,
        2290285,
        2049100,
        2049100,
        2049100,
        2049100};

    public static int getSkillBook(final int job) {
        if (job >= 2210 && job <= 2218) {
            return job - 2209;
        }
        switch (job) {
            case 2310:
            case 3110:
            case 3210:
            case 3310:
            case 3510:
                return 1;
            case 2311:
            case 3111:
            case 3211:
            case 3311:
            case 3511:
                return 2;
            case 2312:
            case 3112:
            case 3212:
            case 3312:
            case 3512:
                return 3;
        }
        return 0;
    }

    public static int getSkillBook(final int job, final int level) {
        if (job >= 2210 && job <= 2218) {
            return job - 2209;
        }
        switch (job) {
            case 2300:
            case 2310:
            case 2311:
            case 2312:
            case 3100:
            case 3200:
            case 3300:
            case 3500:
            case 3110:
            case 3210:
            case 3310:
            case 3510:
            case 3111:
            case 3211:
            case 3311:
            case 3511:
            case 3112:
            case 3212:
            case 3312:
            case 3512:
                return (level <= 30 ? 0 : (level >= 31 && level <= 70 ? 1 : (level >= 71 && level <= 120 ? 2 : (level >= 120 ? 3 : 0))));
        }
        return 0;
    }

    public static int getSkillBookForSkill(final int skillid) {
        return getSkillBook(skillid / 10000);
    }

    public static int getLinkedMountItem(final int sourceid) {
        switch (sourceid % 1000) {
            case 1:
            case 24:
            case 25:
                return 1018;
            case 2:
            case 26:
                return 1019;
            case 3:
                return 1025;
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
                return (sourceid % 1000) + 1023;
            case 9:
            case 10:
            case 11:
                return (sourceid % 1000) + 1024;
            case 12:
                return 1042;
            case 13:
                return 1044;
            case 14:
                return 1049;
            case 15:
            case 16:
            case 17:
                return (sourceid % 1000) + 1036;
            case 18:
            case 19:
                return (sourceid % 1000) + 1045;
            case 20:
                return 1072;
            case 21:
                return 1084;
            case 22:
                return 1089;
            case 23:
                return 1106;
            case 29:
                return 1151;
            case 30:
            case 50:
                return 1054;
            case 31:
            case 51:
                return 1069;
            case 32:
                return 1138;
            case 45:
            case 46:
            case 47:
            case 48:
            case 49:
                return (sourceid % 1000) + 1009;
            case 52:
                return 1070;
            case 53:
                return 1071;
            case 54:
                return 1096;
            case 55:
                return 1101;
            case 56:
                return 1102;
            case 58:
                return 1118;
            case 59:
                return 1121;
            case 60:
                return 1122;
            case 61:
                return 1129;
            case 62:
                return 1139;
            case 63:
            case 64:
            case 65:
            case 66:
            case 67:
            case 68:
            case 69:
            case 70:
            case 71:
            case 72:
            case 73:
            case 74:
            case 75:
            case 76:
            case 77:
            case 78:
                return (sourceid % 1000) + 1080;
            case 85:
            case 86:
            case 87:
                return (sourceid % 1000) + 928;
            case 88:
                return 1065;

            case 27:
                return 1932049; //airplane
            case 28:
                return 1932050; //airplane
            case 114:
                return 1932099; //bunny buddy
            //33 = hot air
            //37 = bjorn
            //38 = speedy chariot
            //57 = law officer
            //they all have in wz so its ok
        }
        return 0;
    }

    public static int getMountItem(final int sourceid, final MapleCharacter chr) {
        switch (sourceid) {
            case 5221006:
                return 1932000;
            case 33001001: //temp.
                if (chr == null) {
                    return 1932015;
                }
                switch (chr.getIntNoRecord(JAGUAR)) {
                    case 20:
                        return 1932030;
                    case 30:
                        return 1932031;
                    case 40:
                        return 1932032;
                    case 50:
                        return 1932033;
                    case 60:
                        return 1932036;
                }
                return 1932015;
            case 35001002:
            case 35120000:
                return 1932016;
        }
        if (!isBeginnerJob(sourceid / 10000)) {
            if (sourceid / 10000 == 8000 && sourceid != 80001000) { //todoo clean up
                final Skill skil = SkillFactory.getSkill(sourceid);
                if (skil != null && skil.getTamingMob() > 0) {
                    return skil.getTamingMob();
                } else {
                    final int link = getLinkedMountItem(sourceid);
                    if (link > 0) {
                        if (link < 10000) {
                            return getMountItem(link, chr);
                        } else {
                            return link;
                        }
                    }
                }
            }
            return 0;
        }
        switch (sourceid % 10000) {
            case 1013:
            case 1046:
                return 1932001;
            case 1015:
            case 1048:
                return 1932002;
            case 1016:
            case 1017:
            case 1027:
                return 1932007;
            case 1018:
                return 1932003;
            case 1019:
                return 1932005;
            case 1025:
                return 1932006;
            case 1028:
                return 1932008;
            case 1029:
                return 1932009;
            case 1030:
                return 1932011;
            case 1031:
                return 1932010;
            case 1033:
                return 1932013;
            case 1034:
                return 1932014;
            case 1035:
                return 1932012;
            case 1036:
                return 1932017;
            case 1037:
                return 1932018;
            case 1038:
                return 1932019;
            case 1039:
                return 1932020;
            case 1040:
                return 1932021;
            case 1042:
                return 1932022;
            case 1044:
                return 1932023;
            //case 1045:
            //return 1932030; //wth? helicopter? i didnt see one, so we use hog
            case 1049:
                return 1932025;
            case 1050:
                return 1932004;
            case 1051:
                return 1932026;
            case 1052:
                return 1932027;
            case 1053:
                return 1932028;
            case 1054:
                return 1932029;
            case 1063:
                return 1932034;
            case 1064:
                return 1932035;
            case 1065:
                return 1932037;
            case 1069:
                return 1932038;
            case 1070:
                return 1932039;
            case 1071:
                return 1932040;
            case 1072:
                return 1932041;
            case 1084:
                return 1932043;
            case 1089:
                return 1932044;
            case 1096:
                return 1932045;
            case 1101:
                return 1932046;
            case 1102:
                return GMS ? 1932061 : 1932047;
            case 1106:
                return 1932048;
            case 1118:
                return 1932060;
            case 1115:
                return 1932052;
            case 1121:
                return 1932063;
            case 1122:
                return 1932064;
            case 1123:
                return 1932065;
            case 1128:
                return 1932066;
            case 1130:
                return 1932072;
            case 1136:
                return 1932078;
            case 1138:
                return 1932080;
            case 1139:
                return 1932081;
            //FLYING
            case 1143:
            case 1144:
            case 1145:
            case 1146:
            case 1147:
            case 1148:
            case 1149:
            case 1150:
            case 1151:
            case 1152:
            case 1153:
            case 1154:
            case 1155:
            case 1156:
            case 1157:
                return 1992000 + (sourceid % 10000) - 1143;
            default:
                return 0;
        }
    }

    public static boolean isKatara(int itemId) {
        return itemId / 10000 == 134;
    }

    public static boolean isDagger(int itemId) {
        return itemId / 10000 == 133;
    }

    public static boolean isApplicableSkill(int skil) {
        return (skil < 40000000 && (skil % 10000 < 8000 || skil % 10000 > 8007) && !isAngel(skil)) || skil >= 92000000 || (skil >= 80000000 && skil < 80010000); //no additional/decent skills
    }

    public static boolean isApplicableSkill_(int skil) { //not applicable to saving but is more of temporary
        /*
        for (int i : PlayerStats.pvpSkills) {
            if (skil == i) {
                return true;
            }
        }
         */
        boolean custom_novice_skill = false;
        switch (skil % 10000) {
            case 1200:
            case 1201:
            case 1202:
            case 1203:
            case 1204:
            case 1300:
                custom_novice_skill = true;
                break;
        }
        return custom_novice_skill || (skil >= 90000000 && skil < 92000000) || (skil % 10000 >= 8000 && skil % 10000 <= 8006) || isAngel(skil);
    }

    public static boolean isTablet(int itemId) {
        return itemId / 1000 == 2047;
    }

    public static boolean isGeneralScroll(int itemId) {
        return itemId / 1000 == 2046;
    }

    public static int getSuccessTablet(final int scrollId, final int level) {
        if (scrollId % 1000 / 100 == 2) { //2047_2_00 = armor, 2047_3_00 = accessory
            switch (level) {
                case 0:
                    return 70;
                case 1:
                    return 55;
                case 2:
                    return 43;
                case 3:
                    return 33;
                case 4:
                    return 26;
                case 5:
                    return 20;
                case 6:
                    return 16;
                case 7:
                    return 12;
                case 8:
                    return 10;
                default:
                    return 7;
            }
        } else if (scrollId % 1000 / 100 == 3) {
            switch (level) {
                case 0:
                    return 70;
                case 1:
                    return 35;
                case 2:
                    return 18;
                case 3:
                    return 12;
                default:
                    return 7;
            }
        } else {
            switch (level) {
                case 0:
                    return 70;
                case 1:
                    return 50; //-20
                case 2:
                    return 36; //-14
                case 3:
                    return 26; //-10
                case 4:
                    return 19; //-7
                case 5:
                    return 14; //-5
                case 6:
                    return 10; //-4
                default:
                    return 7;  //-3
            }
        }
    }

    public static int getCurseTablet(final int scrollId, final int level) {
        if (scrollId % 1000 / 100 == 2) { //2047_2_00 = armor, 2047_3_00 = accessory
            switch (level) {
                case 0:
                    return 10;
                case 1:
                    return 12;
                case 2:
                    return 16;
                case 3:
                    return 20;
                case 4:
                    return 26;
                case 5:
                    return 33;
                case 6:
                    return 43;
                case 7:
                    return 55;
                case 8:
                    return 70;
                default:
                    return 100;
            }
        } else if (scrollId % 1000 / 100 == 3) {
            switch (level) {
                case 0:
                    return 12;
                case 1:
                    return 18;
                case 2:
                    return 35;
                case 3:
                    return 70;
                default:
                    return 100;
            }
        } else {
            switch (level) {
                case 0:
                    return 10;
                case 1:
                    return 14; //+4
                case 2:
                    return 19; //+5
                case 3:
                    return 26; //+7
                case 4:
                    return 36; //+10
                case 5:
                    return 50; //+14
                case 6:
                    return 70; //+20
                default:
                    return 100;  //+30
            }
        }
    }

    public static boolean isAccessory(final int itemId) {
        return (itemId >= 1010000 && itemId < 1040000) || (itemId >= 1122000 && itemId < 1153000) || itemId / 10000 == 111;//(itemId >= 1112000 && itemId < 1113000);
    }

    public static boolean potentialIDFits(final int potentialID, final int newstate, final int i) {
        if (newstate == 12) { // 프리미엄 미라클 큐브 : 레전드리
            return (potentialID >= 30001 && potentialID < 30041) 
                    || potentialID == 30400 || potentialID == 30401 || potentialID == 30402 || potentialID == 30403;
        } else if (newstate == 11) { // 프리미엄 미라클 큐브 : 유니크
            return (potentialID >= 30041
                    && potentialID != 30400 && potentialID != 30401 && potentialID != 30402 && potentialID != 30403);
        } else if (newstate == 10) { // 프리미엄 미라클 큐브 : 에픽
            return (potentialID >= 20000 && potentialID < 30000);
        } else if (newstate == 9) { // 프리미엄 미라클 큐브 : 레어
            return (potentialID >= 10000 && potentialID < 20000);
        } else if (newstate == 8) { // 레전드리
            return (i == 0 || Randomizer.nextInt(10) == 0 ? (potentialID >= 30001 && potentialID < 30041)
                    || potentialID == 30400 || potentialID == 30401 || potentialID == 30402 || potentialID == 30403
                    : potentialID >= 30041 && potentialID != 30400 && potentialID != 30401 && potentialID != 30402 && potentialID != 30403);
        } else if (newstate == 7) { // 유니크
            return (i == 0 || Randomizer.nextInt(10) == 0 ? potentialID >= 30041
                    && potentialID != 30400 && potentialID != 30401 && potentialID != 30402 && potentialID != 30403 : potentialID >= 20000 && potentialID < 30000);
        } else if (newstate == 6) { // 에픽
            return (i == 0 || Randomizer.nextInt(10) == 0 ? potentialID >= 20000 && potentialID < 30000 : potentialID >= 10000 && potentialID < 20000);
        } else if (newstate == 5) { // 레어
            return (i == 0 || Randomizer.nextInt(10) == 0 ? potentialID >= 10000 && potentialID < 20000 : (potentialID != 7 && !(potentialID >= 20 && potentialID <= 30) && potentialID < 10000));
        } else {
            return false;
        }
    }

    /*public static boolean potentialIDFits(final int potentialID, final int newstate, final int i, int a) {
        //이쪽으로 새로 이관댐 
        if (newstate == 12) { // 프리미엄 미라클 큐브 : 레전드리
            return (potentialID >= 30001 && potentialID < 30044);
        } else if (newstate == 11) { // 프리미엄 미라클 큐브 : 유니크
            return (potentialID >= 30044);
        } else if (newstate == 10) { // 프리미엄 미라클 큐브 : 에픽
            return (potentialID >= 20000 && potentialID < 30000);
        } else if (newstate == 9) { // 프리미엄 미라클 큐브 : 레어
            return (potentialID >= 10000 && potentialID < 20000);
        } else if (newstate == 8) { // 레전드리
            return (i == 0 || Randomizer.nextInt(1000) < a ? potentialID >= 30001 && potentialID < 30044 : potentialID >= 30044);
        } else if (newstate == 7) { // 유니크
            return (i == 0 || Randomizer.nextInt(1000) < a ? potentialID >= 30044 : potentialID >= 20000 && potentialID < 30000);
        } else if (newstate == 6) { // 에픽
            return (i == 0 || Randomizer.nextInt(1000) < a ? potentialID >= 20000 && potentialID < 30000 : potentialID >= 10000 && potentialID < 20000);
        } else if (newstate == 5) { // 레어
            return (i == 0 || Randomizer.nextInt(1000) < a ? potentialID >= 10000 && potentialID < 20000 : (potentialID != 7 && !(potentialID >= 20 && potentialID <= 30) && potentialID < 10000));
        } else {
            return false;
        }
    }*/
    public static boolean optionTypeFits(int optionType, int itemID) {
        return optionTypeFits(optionType, itemID, 0);
    }
    
    public static boolean optionTypeFits(final int optionType, final int itemId, int potentialID) {
        if (potentialID > 0) {
            int itemOptionType = itemId / 10000;
            switch (potentialID) {
                case 30402:
                case 30403: // 메/드획
                    //귀고리, 펜던트, 벨트만
                    return itemOptionType == 103 || itemOptionType == 112 || itemOptionType == 113;
                case 30040: //쓸어블
                case 30400: //쓸컴뱃
                case 30401: //쓸윈부
                    //장갑만
                    return itemOptionType == 108;
            }
        }
        
        switch (optionType) {
            case 10: //weapon
                return isWeapon(itemId);
            case 11: //any armor
                return !isWeapon(itemId);
            case 20: //shield??????????
                return itemId / 10000 == 109; //just a gues
            case 21: //pet equip?????????
                return itemId / 10000 == 180; //???LOL
            case 40: //face accessory
                return isAccessory(itemId);
            case 51: //hat
                return itemId / 10000 == 100;
            case 52: //cape
                return itemId / 10000 == 110;
            case 53: //top/bottom/overall
                return itemId / 10000 == 104 || itemId / 10000 == 105 || itemId / 10000 == 106;
            case 54: //glove
                return itemId / 10000 == 108;
            case 55: //shoe
                return itemId / 10000 == 107;
            case 90:
                return false; //half this stuff doesnt even work
            default:
                return true;
        }
    }

    public static boolean 제외옵션(final int itemId) { // 큐브 제외옵션
        switch (itemId) {
            case 31001:
            case 31002:
            case 31003:
            case 31004:
                return false;
            case 41005:
            case 41006:
            case 41007:
                return false;
        }
        return true;
    }

    public static final boolean isMountItemAvailable(final int mountid, final int jobid) {
        if (jobid != 900 && mountid / 10000 == 190) {
            switch (mountid) {
                case 1902000:
                case 1902001:
                case 1902002:
                    return isAdventurer(jobid);
                case 1902005:
                case 1902006:
                case 1902007:
                    return isKOC(jobid);
                case 1902015:
                case 1902016:
                case 1902017:
                case 1902018:
                    return isAran(jobid);
                case 1902040:
                case 1902041:
                case 1902042:
                    return isEvan(jobid);
            }

            if (isResist(jobid)) {
                return false; //none lolol
            }
        }
        if (mountid / 10000 != 190) {
            return false;
        }
        return true;
    }

    public static boolean isMechanicItem(final int itemId) {
        return itemId >= 1610000 && itemId < 1660000;
    }

    public static boolean isEvanDragonItem(final int itemId) {
        return itemId >= 1940000 && itemId < 1980000; //194 = mask, 195 = pendant, 196 = wings, 197 = tail
    }

    public static boolean canScroll(final int itemId) {
        return itemId / 100000 != 19 && itemId / 100000 != 16; //no mech/taming/dragon
    }

    public static boolean canHammer(final int itemId) {
        switch (itemId) {
            case 1122000:
            case 1122076: //ht, chaos ht
                return false;
        }
        if (!canScroll(itemId)) {
            return false;
        }
        return true;
    }
    public static int[] owlItems = new int[]{
        1082002, // work gloves
        2070005,
        2070006,
        1022047,
        1102041,
        2044705,
        2340000, // white scroll
        2040017,
        1092030,
        2040804};

    public static int getMasterySkill(final int job) {
        if (job >= 1410 && job <= 1412) {
            return 14100000;
        } else if (job >= 410 && job <= 412) {
            return 4100000;
        } else if (job >= 520 && job <= 522) {
            return 5200000;
        }
        return 0;
    }

    public static int getExpRate_Below10(final int job) {
        if (GameConstants.isEvan(job)) {
            return 1;
        } else if (GameConstants.isAran(job) || GameConstants.isKOC(job) || GameConstants.isResist(job)) {
            return 5;
        }
        return 10;
    }

    public static int getExpRate_Quest(final int level) {
        return ChannelServer.getquestRate();
    }

    public static int getCustomReactItem(final int rid, final int original) {
        if (rid == 2008006) { //orbis pq LOL
            return (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 4001055);
            //4001056 = sunday. 4001062 = saturday
        } else {
            return original;
        }
    }

    public static int getJobNumber(int jobz) {
        int job = (jobz % 1000);
        if (job / 100 == 0 || isBeginnerJob(jobz)) {
            return 0; //beginner
        } else if ((job / 10) % 10 == 0 || job == 501) {
            return 1;
        } else {
            return 2 + (job % 10);
        }
    }

    public static boolean isBeginnerJob(final int job) {
        return job == 0 || job == 1 || job == 1000 || job == 2000 || job == 2001 || job == 3000 || job == 3001 || job == 2002;
    }

    public static int isSoul(final int itemid) {
        if (itemid / 100 == 50108) {
            return itemid;
        } else if (itemid == 0) {
            return 0;
        } else {
            return -1;
        }
    }

    public static boolean isForceRespawn(int mapid) {//보스젠
        switch (mapid) {
            /* case 100000005://머쉬맘
            case 931000500://제이라                                
            case 270020500://릴리노흐       
            case 270030500://라이카       
            case 800020140://천구       
            case 230040420://피아누스        
            case 801040003://여두목        
            case 240020401://마뇽                      
            case 240020402://마뇽        
            case 240020101://그리프                      
            case 240020102://그리프             
            case 800040410://두꺼비 영주                                              
            case 240040401://레비아탄       */

            case 103000800: //kerning PQ crocs
            case 925100100: //crocs and stuff
                return true;
            default:
                return mapid / 100000 == 9800 && (mapid % 10 == 1 || mapid % 1000 == 100);
        }
    }

    public static int getFishingTime(boolean vip, boolean gm) {
        return gm ? 1000 : (vip ? 30000 : 60000);
    }

    public static int getCustomSpawnID(int summoner, int def) {
        switch (summoner) {
            case 9400589:
            case 9400748: //MV
                return 9400706; //jr
            default:
                return def;
        }
    }

    public static boolean canForfeit(int questid) {
        switch (questid) {
            default:
                return true;
        }
    }

    public static double getAttackRange(MapleStatEffect def, int rangeInc) {
        double defRange = ((400.0 + rangeInc) * (400.0 + rangeInc));
        if (def != null) {
            defRange += def.getMaxDistanceSq() + (def.getRange() * def.getRange());
        }
        //rangeInc adds to X
        //400 is approximate, screen is 600.. may be too much
        //200 for y is also too much
        //default 200000
        return defRange + 120000.0;
    }

    public static double getAttackRange(Point lt, Point rb) {
        double defRange = (400.0 * 400.0);
        final int maxX = Math.max(Math.abs(lt == null ? 0 : lt.x), Math.abs(rb == null ? 0 : rb.x));
        final int maxY = Math.max(Math.abs(lt == null ? 0 : lt.y), Math.abs(rb == null ? 0 : rb.y));
        defRange += (maxX * maxX) + (maxY * maxY);
        //rangeInc adds to X
        //400 is approximate, screen is 600.. may be too much
        //200 for y is also too much
        //default 200000
        return defRange + 120000.0;
    }

    public static int getLowestPrice(int itemId) {
        switch (itemId) {
            case 2340000: //ws
            case 2531000:
            case 2530000:
                return 50000000;
        }
        return -1;
    }

    public static boolean isNoDelaySkill(int skillId) {
        return skillId == 5110001 || skillId == 21101003 || skillId == 15100004 || skillId == 33101004 || skillId == 32111010 || skillId == 2111007 || skillId == 2211007 || skillId == 2311007 || skillId == 32121003 || skillId == 35121005 || skillId == 35111004 || skillId == 35121013 || skillId == 35121003 || skillId == 22150004 || skillId == 22181004 || skillId == 11101002 || skillId == 13101002;
    }

    public static boolean isNoSpawn(int mapID) {
        return mapID == 809040100 || mapID == 925020010 || mapID == 925020011 || mapID == 925020012 || mapID == 925020013 || mapID == 925020014 || mapID == 980010000 || mapID == 980010100 || mapID == 980010200 || mapID == 980010300 || mapID == 980010020;
    }

    public static int getExpRate(int job, int def) {
        return def;
    }

    public static int getModifier(int itemId, int up) {
        if (up <= 0) {
            return 0;
        }
        switch (itemId) {
            case 2022459:
            case 2860179:
            case 2860193:
            case 2860207:
                return 130;
            case 2022460:
            case 2022462:
            case 2022730:
                return 150;
            case 2860181:
            case 2860195:
            case 2860209:
                return 200;
        }
        if (itemId / 10000 == 286) { //familiars
            return 150;
        }
        return 200;
    }

    public static short getSlotMax(int itemId) {
        switch (itemId) {
            case 4030003:
            case 4030004:
            case 4030005:
            case 5530068:
                return 1;
            case 4001168:
            case 4031306:
            case 4031307:
            case 3993000:
            case 3993002:
            case 3993003:
                return 100;
            case 5220010:
            case 5220013:
                return 1000;
            case 5220020:
                return 2000;
        }
        return 0;
    }

    public static boolean isDropRestricted(int itemId) {
        return itemId == 3012000 || itemId == 4030004 || itemId == 1052098 || itemId == 1052202;
    }

    public static boolean isPickupRestricted(int itemId) {
        return itemId == 4030003 || itemId == 4030004;
    }

    public static short getStat(int itemId, int def) {
        switch (itemId) {
            case 1002419://베타상징두건
                return 1;
            case 1003068://라바나투구
                return 10;
            case 1142002://퀘스리
                return 5;
            case 1102047://실리스
                return 4;
            case 1102046://실리스
                return 4;
            case 1142355://어망홍리 낚시 훈장
                return 10;
            case 1142249://럭키가이 훈장
                return 10;
            case 1003139://시간여행자 로렐
                return 10;
            case 1142914://본투비 다이아
                return 10;
        }
        return (short) def;
    }

    public static short getHpMp(int itemId, int def) {
        switch (itemId) {
            case 1122121:
                return 500;
            case 1142002:
            case 1002959:
                return 1000;
            case 1142249:
                return 1500;
            case 1022137: //세인트세이버고글
                return 500;
            case 1032105: //세인트세이버이어링
                return 500;
        }
        return (short) def;
    }

    public static short getATK(int itemId, int def) {
        switch (itemId) {
            case 1122121:
                return 3;
            case 1002959:
                return 4;
            case 1142002:
                return 5;
            case 1142249:
                return 10;
            case 1022137: //세인트세이버고글
                return 1;
            case 1032105: //세인트세이버이어링
                return 1;
            case 1112663: //화엔블
                return 1;
        }
        return (short) def;
    }

    public static short getDEF(int itemId, int def) {
        switch (itemId) {
            case 1122121:
                return 250;
            case 1002959:
                return 500;
        }
        return (short) def;
    }

    public static boolean isDojo(int mapId) {
        return mapId >= 925020100 && mapId <= 925023814;
    }

    public static boolean isMonsterpark(int mapId) {
        return mapId >= 952000000 && mapId <= 954050600;
    }

    public static int getPartyPlayHP(int mobID) { // VIP존 몬스터와 일부 파티사냥터 몬스터 피통 수치 적용
        switch (mobID) {
            case 4250000:
                return 45000;
            case 4250001:
                return 50000;
            case 5250000:
                return 60000;
            case 5250001:
                return 55000;
            case 5250002:
                return 65000;

            case 9400661:
                return 15000000;
            case 9400660:
                return 30000000;
            case 9400659:
                return 45000000;
            case 9400658:
                return 20000000;
        }
        return 0;
    }

    public static int getPartyPlayEXP(int mobID) { // VIP존 몬스터와 일부 파티사냥터 몬스터 경험치 수치 적용
        switch (mobID) {
            case 4300014:
                return 206;
            case 4300015:
                return 212;
            case 4300016:
                return 218;

            case 4250000:
                return 1321;
            case 4250001:
                return 1421;
            case 5250000:
                return 1942;
            case 5250001:
                return 1642;
            case 5250002:
                return 2311;

            case 9400661:
                return 40000;
            case 9400660:
                return 70000;
            case 9400659:
                return 90000;
            case 9400658:
                return 50000;
            // case 8610005://정식기사A
            //     return 200000;                

            case 9600019: //황금무술가
                return 7950;
            case 9600020: //적금무술가
                return 8950;
            case 9600022: //백은거인
                return 7450;
            case 9600023: //적금봉술가
                return 8950;
            case 9600024://백은창술가
                return 11500;
        }
        return 0;
    }

    public static int getPartyPlay(int mapId) {//파티플레이존
        switch (mapId) {
            case 300010000:
            case 300010100:
            case 300010200:
            case 300010300:
            case 300010400:
            case 300020000:
            case 300020100:
            case 300020200:
            case 300030000:

            case 683070400:
            case 683070401:
            case 683070402:
                return 25;

            case 211060100:   //성벽   
            case 211060200:
            case 211060300:
            case 211060410:
            case 211060500:
            case 211060610:
            case 211060620:
            case 211060700:
            case 211060810:
            case 211060820:
            case 211060830:
            case 211060900:
                return 10;

            case 271030101:
            case 271030102:
            case 271030310:
            case 271030320:
               case 273000000:
               case 273010000:
               case 273020000:
               case 273020100:
               case 273020200:
               case 273020300:
               case 273020400:
               case 273030000:
               case 273030100:
               case 273030200:
               case 273030300:
               case 273040000:
               case 273040100:
               case 273040200:
               case 273040300:
               case 273050000:
               case 273060000:
               case 273060100:
               case 273060200:
               case 273060300:
                return 20;

            case 952000000:  //몬스터파크
            case 952000100:
            case 952000200:
            case 952000300:
            case 952000400:
            case 952000500:
            case 952010000:
            case 952010100:
            case 952010200:
            case 952010300:
            case 952010400:
            case 952010500:
            case 952020000:
            case 952020100:
            case 952020200:
            case 952020300:
            case 952020400:
            case 952020500:
            case 952030000:
            case 952030100:
            case 952030200:
            case 952030300:
            case 952030400:
            case 952030500:
            case 952040000:
            case 952040100:
            case 952040200:
            case 952040300:
            case 952040400:
            case 952040500:
            case 953000000:
            case 953000100:
            case 953000200:
            case 953000300:
            case 953000400:
            case 953000500:
            case 953010000:
            case 953010100:
            case 953010200:
            case 953010300:
            case 953010400:
            case 953010500:
            case 953020000:
            case 953020100:
            case 953020200:
            case 953020300:
            case 953020400:
            case 953020500:
            case 953030000:
            case 953030100:
            case 953030200:
            case 953030300:
            case 953030400:
            case 953030500:
                return 30;
        }
        return 0;
    }

    public static int getPartyPlay(int mapId, int def) {
        int dd = getPartyPlay(mapId);
        if (dd > 0) {
            return dd;
        }
        return def / 2;
    }

    public static boolean isHyperTeleMap(int mapId) {
        for (int i : hyperTele) {
            if (i == mapId) {
                return true;
            }
        }
        return false;
    }

    public static int getCurrentDate() {
        final String time = FileoutputUtil.CurrentReadable_Time();
        return Integer.parseInt(new StringBuilder(time.substring(0, 4)).append(time.substring(5, 7)).append(time.substring(8, 10)).append(time.substring(11, 13)).toString());
    }

    public static int getCurrentDate_NoTime() {
        final String time = FileoutputUtil.CurrentReadable_Time();
        return Integer.parseInt(new StringBuilder(time.substring(0, 4)).append(time.substring(5, 7)).append(time.substring(8, 10)).toString());
    }

    public static void achievementRatio(MapleClient c) {
        //PQs not affected: Amoria, MV, CWK, English, Zakum, Horntail(?), Carnival, Ghost, Guild, LudiMaze, Elnath(?) 
        switch (c.getPlayer().getMapId()) {

        }
    }

    public static boolean isAngel(int sourceid) {
        return isBeginnerJob(sourceid / 10000) && (sourceid % 10000 == 1085 || sourceid % 10000 == 1087 || sourceid % 10000 == 1090 || sourceid % 10000 == 1179);
    }

    public static boolean isMulungBoss(int mobid) {
        switch (mobid) {
            case 9300184: // Mano
            case 9300185: // Stumpy
            case 9300186: // Dewu
            case 9300187: // King Slime
            case 9300188: // Giant Centipede
            case 9300189: // Faust
            case 9300190: // King Clang
            case 9300191: // Mushmom
            case 9300192: // Alishar
            case 9300193: // Timer
            case 9300194: // Dale
            case 9300195: // Papa Pixie
            case 9300196: // Zombie Mushmom
            case 9300197: // Jeno
            case 9300198: // Lord Pirate
            case 9300199: // Old Fox
            case 9300200: // Tae Roon
            case 9300201: // Poison Golem
            case 9300202: // Ghost Priest
            case 9300203: // Jr. Balrog
            case 9300204: // Eliza
            case 9300205: // Frankenroid
            case 9300206: // Chimera
            case 9300207: // Snack Bar
            case 9300208: // Snowman
            case 9300209: // Blue Mushmom
            case 9300210: // Crimson Balrog
            case 9300211: // Manon
            case 9300212: // Griffey
            case 9300213: // Leviathan
            case 9300214: // Papulatus
            case 9300215: // Mu gong
                return true;
        }
        return false;
    }

    public static boolean isFishingMap(int mapid) {
        return mapid == 741000200 || mapid == 741000201 || mapid == 741000202 || mapid == 741000203 || mapid == 741000204 || mapid == 741000205 || mapid == 741000206 || mapid == 741000207 || mapid == 741000208;
    }
    public static int getRewardPot(int itemid, int closeness) {
        switch (itemid) {
            case 2440000:
                switch (closeness / 10) {
                    case 0:
                    case 1:
                    case 2:
                        return 2028041 + (closeness / 10);
                    case 3:
                    case 4:
                    case 5:
                        return 2028046 + (closeness / 10);
                    case 6:
                    case 7:
                    case 8:
                        return 2028049 + (closeness / 10);
                }
                return 2028057;
            case 2440001:
                switch (closeness / 10) {
                    case 0:
                    case 1:
                    case 2:
                        return 2028044 + (closeness / 10);
                    case 3:
                    case 4:
                    case 5:
                        return 2028049 + (closeness / 10);
                    case 6:
                    case 7:
                    case 8:
                        return 2028052 + (closeness / 10);
                }
                return 2028060;
            case 2440002:
                return 2028069;
            case 2440003:
                return 2430278;
            case 2440004:
                return 2430381;
            case 2440005:
                return 2430393;
        }
        return 0;
    }

    public static boolean isEventMap(final int mapid) {
        return (mapid >= 109010000 && mapid < 109050000) || (mapid > 109050001 && mapid < 109090000) || (mapid >= 809040000 && mapid <= 809040100);
    }

    public static boolean isMagicChargeSkill(final int skillid) {
        switch (skillid) {
            case 2121001: // Big Bang
            case 2221001:
            case 2321001:
            case 22121000: //breath
            case 22151001:
                return true;
        }
        return false;
    }

    public static boolean getSpecialItem(int itemid) {
        switch (itemid) {
            case 4007000:
            case 4007001:
            case 4007002:
            case 4007003:
            case 4007004:
            case 4007005:
            case 4007006:
            case 4007007:
            case 4010000:
            case 4010001:
            case 4010002:
            case 4010003:
            case 4010004:
            case 4010005:
            case 4010006:
            case 4010007:
            case 4006001:
            case 4006000:
            case 4004000:
            case 4004001:
            case 4004002:
            case 4004003:
            case 4004004:
                return true;
        }
        return false;
    }

    public static int getExpModByLevel(int level) { //레벨별 경험치배율 아니네
        if (level < 10) {
            return 1;
        } else if (level >= 10 && level < 120) {
            return 1;
        } else if (level >= 121 && level < 200) {
            return 1;
        } else if (level >= 201 && level < 400) {
            return 1;
        } else if (level >= 401 && level < 999) {
            return 1;
        } else {
            return RateManager.EXP;
        }
    }

    public static boolean isTeamMap(final int mapid) {
        return mapid == 109080000 || mapid == 109080001 || mapid == 109080002 || mapid == 109080003 || mapid == 109080010 || mapid == 109080011 || mapid == 109080012 || mapid == 109090300 || mapid == 109090301 || mapid == 109090302 || mapid == 109090303 || mapid == 109090304 || mapid == 910040100 || mapid == 960020100 || mapid == 960020101 || mapid == 960020102 || mapid == 960020103 || mapid == 960030100 || mapid == 689000000 || mapid == 689000010;
    }

    public static int getStatDice(int stat) {
        switch (stat) {
            case 2:
                return 30;
            case 3:
                return 20;
            case 4:
                return 15;
            case 5:
                return 20;
            case 6:
                return 30;
        }
        return 0;
    }

    public static int getDiceStat(int buffid, int stat) {
        if (buffid == stat || buffid % 10 == stat || buffid / 10 == stat) {
            return getStatDice(stat);
        } else if (buffid == (stat * 100)) {
            return getStatDice(stat) + 10;
        }
        return 0;
    }

    public static boolean moblimit1(final int mapid) { //젠률 맵추가 3배
        int[] chackMapid = {
            211060100, //성벽 아래 1
            211060300, //성벽 아래 2
            211060500, //성벽 아래 3
            211060700, //성벽 아래 4
            211060410, //낮은 성벽1
            211060610, //낮은 성벽2
            211060810, //낮은 성벽3
            211060620, //높은 성벽1
            211060820, //높은 성벽2
            271030310, //무기고1
            271030320, //무기고2
            271010100, //파괴된 헤네시스 시장
            271010200, //파괴된 헤네시스 공원
            271010300, //파괴된 니은숲 북쪽언덕
            271010301, //파괴된 니은숲 수상한 언덕
            271010400, //파괴된 니은숲 꿈꾸는 오솔길
            271020000, //파괴된 버섯노래숲 어두운포자언덕
            271020100, //파괴된 버섯노래숲 음산한 콧노래 오솔길
            271030500, //기사의전당1
            271030510,
            271030520,
            271030530,
            271030540, //기사의전당5
            701101000,//예원시작
            701101010,
            701101020,
            701101030,
            701103000,
            701103010,
            701103020,
            701103030,
            701102000,
            701102010,
            701102020,
            701102030,//예원종료
            123000001,//미던시작
            123000002,
            123000003,
            123000004,
            123000005,
            123000006,
            123000007,
            123000008,
            123000009,
            123000010,
            123400001,
            123400002,
            123400003,
            123400004,
            123400005,
            123400006,
            123400007,
            123400008,
            123400009,
            123400010,//미던종료
            
        };
        for (int i = 0; i < chackMapid.length; i++) {
            if (chackMapid[i] == mapid) {
                return true;
            }
        }
        return false;
    }
    
        public static boolean moblimit2(final int mapid) { //젠률 맵추가 3배
        int[] chackMapid = {
            273010000,//황폐시작
            273020000,
            273020100,
            273020200,
            273020300,
            273020400,
            273030000,
            273030100,
            273030200,
            273030300,
            273040000,
            273040100,
            273040200,
            273040300,
            273050000,
            273060000,
            273060100,
            273060200,
            273060300,//황폐종료
            310070200,//헤이븐시작
            310070210,
            310070220,
            310070230,
            310070160,
            310070150,
            310070140,
            310070130,
            310070120,
            310070110,
            310070100,
            310070000,//헤이븐종료
            600010010,//뉴리프 시작
            600010020,
            600010100,
            600010100,
            600010110,
            600010120,
            600010130,
            600010150,
            600010160,
            600010170,
            600010180,
            600010200,
            600010210,
            600010220,
            600010230,//뉴리프 종료
            450001114,//여로 시작
            450001112,
            450001113,
            450001110,
            450001111,
            450001010,
            450001011,
            450001012,
            450001013,
            450001014,
            450001015,
            450001016,//여로 종료
            450001000,//아르카나시작
            450001003,
            450001005,
            450001007,
            450001010,
            450001011,
            450001012,
            450001013,
            450001014,
            450001015,
            450001016,
            450001100,
            450001105,
            450001107,
            450001111,
            450001112,
            450001113,
            450001114,
            450001200,
            450001211,
            450001212,
            450001213,
            450001214,
            450001215,
            450001216,
            450001217,
            450001218,
            450001219,
            450001230,
            450001240,
            450001250,
            450001300,
            450001310,
            450001320,
            450001330,
            450001340,
            450001350,
            450001360,
            450001370,
            450001380,
            450001390,
            450002000,
            450002001,
            450002002,
            450002003,
            450002004,
            450002005,
            450002006,
            450002007,
            450002008,
            450002009,
            450002010,
            450002011,
            450002012,
            450002013,
            450002014,
            450002015,
            450002016,
            450002017,
            450002018,
            450002019,
            450002020,
            450002021,
            450002023,
            450002200,
            450002201,
            450002202,
            450002203,
            450002204,
            450002205,
            450002250,
            450003000,
            450003010,
            450003100,
            450003200,
            450003220,
            450003300,
            450003310,
            450003320,
            450003330,
            450003340,
            450003350,
            450003360,
            450003400,
            450003410,
            450003420,
            450003430,
            450003440,
            450003450,
            450003460,
            450003500,
            450003510,
            450003520,
            450003530,
            450003540,
            450003560,
            450003600,
            450003700,
            450003710,
            450003711,
            450003720,
            450003730,
            450003740,
            450003750,
            450003760,
            450003770,
            450004000,
            450004100,
            450004150,
            450004200,
            450004250,
            450004300,
            450004400,
            450004450,
            450004500,
            450004550,
            450004600,
            450004700,
            450004750,
            450004800,
            450004850,
            450004900,
            450005000,
            450005010,
            450005100,
            450005110,
            450005120,
            450005121,
            450005130,
            450005131,
            450005200,
            450005210,
            450005220,
            450005221,
            450005222,
            450005230,
            450005240,
            450005241,
            450005242,
            450005300,
            450005400,
            450005410,
            450005411,
            450005412,
            450005420,
            450005430,
            450005431,
            450005432,
            450005440,
            450005500,
            450005510,
            450005520,
            450005530,
            450005550,//아르카나 끝
        };
        for (int i = 0; i < chackMapid.length; i++) {
            if (chackMapid[i] == mapid) {
                return true;
            }
        }
        return false;
    }

    public static boolean moblimit3(final int mapid) { //젠률 맵추가 4배
        int[] chackMapid = { 
           450002002,//츄츄시작
           450002002,
           450002003,
           450002004,
           450002005,
           450002006,
           450002007,
           450002008,
           450002009,
           450002010,
           450002011,
           450002012,
           450002013,
           450002014,
           450002015,
           450002016,
           450002017,
           450002018,
           450002019,
           450002020,
           450002201,
           450002202,
           450002203,
           450002204,
           450002205,
           450002250,//츄츄끝
           450003000,//레헬른 시작
           450003010,
           450003100,
           450003200,
           450003210,
           450003220,
           450003300,
           450003310,
           450003320,
           450003330,
           450003340,
           450003350,
           450003360,
           450003400,
           450003410,
           450003420,
           450003430,
           450003440,
           450003450,
           450003460,
           450003500,
           450003510,
           450003520,
           450003530,
           450003540,
           450003550,
           450003560,
           450003600,
           450003700,
           450003710,
           450003711,
           450003720,
           450003730,
           450003740,
           450003750,
           450003760,
           450003770,
           450004000,
           450004100,
           450004150,
           450004200,
           450004250,
           450004300,
           450004400,
           450004500,
           450004550,
           450004600,
           450004700,
           450004750,
           450004800,
           450004850,
           450004900,//레헬른 끝
           
        };
        for (int i = 0; i < chackMapid.length; i++) {
            if (chackMapid[i] == mapid) {
                return true;
            }
        }
        return false;
    }

    public static boolean isMainBossEqp(int itemid) {
        switch (itemid) {
            //case 1002357:
            case 1372049:
            case 1003112:
            case 1372073:
            case 1122000:
            case 1122076: {
                return true;
            }
        }
        return false;
    }

    public static double isDropData(int itemid, MapleMonster monster) { // 드랍 셋팅
        int checkId = itemid / 10000;
        int monsterLevel = monster.getStats().getLevel();
        boolean monsterCheck = monster.getStats().isBoss();
        if (isWeapon(itemid) == true) { // 장비 아이템 (무기)
            return (monsterCheck == true ? (isMainBossEqp(itemid) == true ? 50.0 : 0.0) : 0.01);
        }
        if (isShield(itemid) == true) { // 장비 아이템 (방패)
            return (monsterCheck == true ? (isMainBossEqp(itemid) == true ? 50.0 : 0.0) : 0.01);
        }
        if (isEquip(itemid) == true && isWeapon(itemid) == false && isAccessory(itemid) == false) { // 장비 아이템 (방어구)
            return (monsterCheck == true ? (isMainBossEqp(itemid) == true ? 50.0 : 0.0) : 0.01);
        }
        if (isAccessory(itemid) == true) { // 장비 아이템 (악세서리)
            return (monsterCheck == true ? (isMainBossEqp(itemid) == true ? 50.0 : 0.0) : 0.01);
        }

        switch (checkId) {
            case 400: { // 기타 아이템 (일반)
                int checkId_Etc = itemid / 1000;
                if (checkId_Etc == 4000) { // 전리품
                    return (monsterCheck == true ? 0.0 : 35.0);
                } else if (checkId_Etc == 4003) { // 재료 (나사, 가공된 나무 등.)
                    return 0.5;
                } else if (checkId_Etc == 4004) { // 크리스탈 원석
                    return 0.1;
                } else if (checkId_Etc == 4006) { // 소환의 돌, 마법의 돌
                    return 1;
                } else if (checkId_Etc == 4007) { // 마법의 가루
                    return 0.1;
                } else {
                    return 0.0;
                }
            }
            case 401:
            case 402: { // 기타 아이템 (원석 & 광석)
                return (monsterCheck == true ? 0.0 : checkId == 4020009 ? 0.03 : 0.5);
            }
            case 413: { // 기타 아이템 (촉진제 & 제조법)
                return 0.01;
            }
            case 200:
            case 201:
            case 202: { // 소비 아이템 (포션)
                int checkId_Cun = itemid / 1000;
                return ((monsterCheck == true && monster.getStats().getCP() == 0) ? 0.0 : (checkId_Cun == 2022) ? 0.0 : 1.0);
            }
            case 204: { // 소비 아이템 (주문서)
                boolean checkId_SpecialCun = (itemid == 2049000 || itemid == 2049001 || itemid == 2049002 || itemid == 2049004 || itemid == 2049005 || itemid == 2049103 || itemid == 2049116 || itemid == 2049117 || itemid == 2049118 || itemid == 2049120 || itemid == 2049300 || itemid == 2049301 || itemid == 2049400 || itemid == 2049401 || itemid == 2049403 || itemid == 2430049 || itemid == 2430067);
                return (checkId_SpecialCun == true ? 0.0 : monsterLevel > 100 ? 0.05 : monsterLevel > 50 ? 0.04 : 0.03);
            }
            case 228:
            case 229: { // 소비 아이템 (스킬 북 & 마스터리 북)
                return (monsterCheck == true ? 0.0 : monsterLevel > 100 ? 0.02 : 0.01);
            }
            case 233:
            case 207: { // 소비 아이템 (불릿 & 표창)
                return (monsterCheck == true ? 0.0 : monsterLevel > 100 ? 0.02 : 0.01);
            }
            default: {
                return 0.0;
            }
        }
    }

    public static String getCashBlockedMsg(final int id) {
        return "현재 캐시샵에서 구매 불가능한 아이템입니다.111";
    }

    public static boolean isNoEnableSkill(int skillid) {
        switch (skillid) {
            case 3121005:
            case 3121004: // 폭풍의 시
            case 3100001: // 파이널 어택
            case 5221004: // 래피드 파이어
            case 13111002: // 폭풍의 시
            case 33121009: // 와일드 발칸
            case 33100009: // 파이널 어택
            case 32121021: // 배틀킹 바
                return true;
        }
        return false;
    }
    
    public static List<Integer> boosterBuffItemID = new ArrayList<>();
    public static List<Integer> boosterEquipItemID = new ArrayList<>();
    
    public static void loadBoosterItemID() {
        int[] item = {
            1112585, 1112586, 1112663
        };
        
        for (int itemID : item) {
            if (!boosterEquipItemID.contains(itemID))
                boosterEquipItemID.add(itemID);
        }
    }
    
    public static int getBoosterItemID(int itemID) {//공속
        int buffID = 0;
        switch (itemID) {
            case 1112585://엔젤릭
                buffID = 2022589;
                break;
            case 1112586://다크 엔젤릭
                buffID = 2022590;
                break;
            case 1112663://화이트 엔젤릭
                buffID = 2022591;
                break;    
        }
        
        if (buffID != 0) {
            if (!boosterBuffItemID.contains(buffID))
                boosterBuffItemID.add(buffID);
        }
        return buffID;
    }

    public static boolean isStatItem(int id) { //심볼코드
        switch (id) { //내가 고민을 많이했는데. 이러는게 젤 낫다 코드 다쑤셔라 

            case 3960050:
            case 3960060:
            case 3960040:
            case 3960041:
            case 3960042:
            case 3960043:
            case 3960044:
            case 3960045:
            case 3960046:
            case 3960047:
            case 3960048:
            case 3960049:
            case 3961001:
            case 3961002:
            case 3961003:
            case 3961004:
            case 3961005:
            case 3962001:
            case 3962002:
            case 3962003:
            case 3962004:
            case 3962005:
            case 3963001:
            case 3963002:
            case 3963003:
            case 3963004:
            case 3963005:
            case 3964001:
            case 3964002:
            case 3964003:
            case 3964004:
            case 3964005:
            case 3965001:
            case 3965002:
            case 3965003:
            case 3965004:
            case 3965005:
            case 3966001:
            case 3966002:
            case 3966003:
            case 3966004:
            case 3966005:
            case 3960900:
            case 3960901:
            case 3960902:
            case 3960903:
            case 3960904:
            case 3960051:
            case 3960052:
            case 3983000:
            case 3983001:
            case 3983002:
            case 3983003:
            case 3983004:
            case 3983005:
            case 3983006:
            case 3984000:
            case 3984001:
            case 5150054:
            case 3995000:
            case 3995001:
            case 3995002:
            case 3995003:
            case 3995004:
            case 3995005:
            case 3995006:
            case 3995007:
            case 3995008:
            case 3995009:
            case 3960000:   
            case 3960001:
            case 3960002:
            case 3960010:    
            case 3960011:
            case 3960012:
            case 3960013:
            case 3960014:
            case 3960020:    
            case 3960021:
            case 3960022:
            case 3960023:
            case 3960024:
            case 3960030:
            case 3960031:
            case 3960032:
            case 3960033:
            case 3960034:
            case 3960035:
            case 3960096:    
                return true;
        }
        return false;
    }

    public static boolean isExtendedSPJob(int jobId) {
        return jobId / 1000 == 3 || (jobId / 100 == 22 || jobId == 2001) || (jobId / 100 == 23 || jobId == 2002) || (jobId / 100 == 24 || jobId == 2003);
    }

    public static long getNewExpTable(final int level) {
        int exp = 100000000;
        return exp * getNewExpRate(level);
    }

     public static long getNewExpRate(final int level) { // 경험치통 
        if (level >= 200 && level < 205) {
            return 10;
        } else if (level >= 205 && level < 210) {
            return 30;
        } else if (level >= 210 && level < 215) {
            return 50;
        } else if (level >= 215 && level < 220) {
            return 60;
        } else if (level >= 220 && level < 225) {
            return 70;
        } else if (level >= 225 && level < 230) {
            return 80;
        } else if (level >= 230 && level < 235) {
            return 90;
        } else if (level >= 235 && level < 240) {
            return 100;
        } else if (level >= 240 && level < 245) {
            return 140;
        } else if (level >= 245 && level < 250) {
            return 180;
        } else if (level >= 250 && level < 255) {
            return 220;
        } else if (level >= 255 && level < 260) {
            return 300;
        } else if (level >= 260 && level < 265) {
            return 400;
        } else if (level >= 265 && level < 270) {
            return 500;
        } else if (level >= 270 && level < 275) {
            return 600;
        } else if (level >= 275 && level < 280) {
            return 700;
        } else if (level >= 280 && level < 285) {
            return 800;
        } else if (level >= 285 && level < 290) {
            return 900;
        } else if (level >= 290 && level < 295) {
            return 100;
        } else if (level >= 295 && level < 300) {
            return 1100;
        } else if (level >= 300 && level < 305) {
            return 2300;
        } else if (level >= 305 && level < 310) {
            return 3200;
        } else if (level >= 310 && level < 315) {
            return 4400;
        } else if (level >= 315 && level < 320) {
            return 4600;
        } else if (level >= 320 && level < 325) {
            return 6000;
        } else if (level >= 325 && level < 330) {
            return 7000;
        } else if (level >= 330 && level < 335) {
            return 8000;
        } else if (level >= 335 && level < 340) {
            return 9000;
        } else if (level >= 340 && level < 345) {
            return 10000;
        } else if (level >= 345 && level < 350) {
            return 12000;
        } else if (level >= 350 && level < 355) {
            return 15000;
        } else if (level >= 355 && level < 360) {
            return 20000;
        } else if (level >= 360 && level < 365) {
            return 25000;
        } else if (level >= 365 && level < 370) {
            return 30000;
        } else if (level >= 370 && level < 375) {
            return 35000;
        } else if (level >= 375 && level < 380) {
            return 37000;
        } else if (level >= 380 && level < 385) {
            return 39000;
        } else if (level >= 385 && level < 400) {
            return 42000;
        } else if (level >= 400 && level < 410) {
            return 43000;
        } else if (level >= 410 && level < 420) {
            return 44000;
        } else if (level >= 420 && level < 430) {
            return 45000;
        } else if (level >= 430 && level < 440) {
            return 49000;
        } else if (level >= 440 && level < 450) {
            return 58000;
        } else if (level >= 450 && level < 460) {
            return 65000;
        } else if (level >= 460 && level < 470) {
            return 75000;
        } else if (level >= 470 && level < 480) {
            return 79000;
        } else if (level >= 480 && level < 490) {
            return 85000;
        } else if (level >= 490 && level < 500) {
            return 91000;
        } else if (level >= 500 && level < 510) {
            return 95000;
        } else if (level >= 510 && level < 520) {
            return 100000;
        } else if (level >= 520 && level < 530) {
            return 110000;
        } else if (level >= 530 && level < 540) {
            return 120000;
        } else if (level >= 540 && level < 550) {
            return 130000;
        } else if (level >= 550 && level < 560) {
            return 140000;
        } else if (level >= 560 && level < 570) {
            return 150000;
        } else if (level >= 570 && level < 580) {
            return 160000;
        } else if (level >= 580 && level < 590) {
            return 170000;
        } else if (level >= 590 && level < 600) {
            return 200000;
        } else if (level >= 600 && level < 610) {
            return 220000;
        } else if (level >= 610 && level < 620) {
            return 230000;
        } else if (level >= 620 && level < 630) {
            return 250000;
        } else if (level >= 630 && level < 640) {
            return 270000;
        } else if (level >= 640 && level < 650) {
            return 290000;
        } else if (level >= 650 && level < 660) {
            return 310000;
        } else if (level >= 660 && level < 670) {
            return 320000;
        } else if (level >= 670 && level < 680) {
            return 340000;
        } else if (level >= 680 && level < 690) {
            return 350000;
        } else if (level >= 690 && level < 700) {
            return 370000;
        } else if (level >= 700 && level < 710) {
            return 380000;
        } else if (level >= 710 && level < 720) {
            return 390000;
        } else if (level >= 720 && level < 730) {
            return 400000;
        } else if (level >= 730 && level < 740) {
            return 410000;
        } else if (level >= 740 && level < 750) {
            return 420000;
        } else if (level >= 750 && level < 760) {
            return 430000;
        } else if (level >= 760 && level < 770) {
            return 450000;
        } else if (level >= 770 && level < 780) {
            return 470000;
        } else if (level >= 780 && level < 790) {
            return 490000;
        } else if (level >= 790 && level < 800) {
            return 550000;
        } else if (level >= 800 && level < 810) {
            return 580000;
        } else if (level >= 810 && level < 820) {
            return 600000;
        } else if (level >= 820 && level < 830) {
            return 620000;
        } else if (level >= 830 && level < 840) {
            return 640000;
        } else if (level >= 840 && level < 850) {
            return 660000;
        } else if (level >= 850 && level < 860) {
            return 700000;
        } else if (level >= 860 && level < 870) {
            return 730000;
        } else if (level >= 870 && level < 880) {
            return 750000;
        } else if (level >= 880 && level < 890) {
            return 770000;
        } else if (level >= 890 && level < 900) {
            return 790000;
        } else if (level >= 900 && level < 910) {
            return 810000;
        } else if (level >= 910 && level < 920) {
            return 840000;
        } else if (level >= 920 && level < 930) {
            return 870000;
        } else if (level >= 930 && level < 940) {
            return 890000;
        } else if (level >= 940 && level < 950) {
            return 912000;
        } else if (level >= 950 && level < 960) {
            return 930000;
        } else if (level >= 960 && level < 970) {
            return 940000;
        } else if (level >= 970 && level < 980) {
            return 960000;
        } else if (level >= 980 && level < 990) {
            return 980000;
        } else if (level >= 990 && level < 999) {
            return 1000000;
        } else if (level >= 999 && level < 2000) {
            return 999999999;
            
        }
        
        return 30000;
    }
     
    public static int isExpeditionMonster(int mobID) {//보스포인트 설정
        switch (mobID) {
            case 8800102: { // 카오스자쿰
                return 100; // 획득 보스 포인트
            }
            case 8810122: { // 카오스 혼테일
                return 120; // 획득 보스 포인트
            }
            case 8820001: { // 핑크빈
                return 250; // 획득 보스 포인트 
            }
            case 9400266: { // 카무나
                return 350; // 획득 보스 포인트 
            }
            case 9400265: { // 베르가모트
                return 400; // 획득 보스 포인트    
            }
            case 9400270: { // 칼리하
                return 450; // 획득 보스 포인트     
            }
            case 9400273: { // 니벨룽겐
                return 500; // 획득 보스 포인트 
            }
            case 9400294: { // 듀나스
                return 550; // 획득 보스 포인트 
            }
            case 9400296: { //코어블레이즈
                return 650; // 획득 보스 포인트 
            }
            case 9400289: { //아우프헤븐
                return 800; // 획득 보스 포인트
            }
            case 8850011: { //여제
                return 1000; // 획득 보스 포인트  
            }
            case 8880000: { //매그너스
                return 1000; // 획득 보스 포인트   
            }
            case 8880400: { //진 힐라
                return 1500; // 획득 보스 포인트  
            }
            case 9801030: { //스우 3
                return 2000; // 획득 보스 포인트 
            }
            case 9300891: { //데미안
                return 2500; // 획득 보스 포인트  
            }
            case 8880150: { //루시드
                return 3000; // 획득 보스 포인트   
            }
            case 8880500: { //검마
                return 4000; // 획득 보스 포인트       
                
            }
        }
        return 0;
    }
     
    public static boolean isExpeditionMap(int mapID) { //원정대 가능맵
        switch (mapID) {
            case 280030000://자쿰노말
            case 280030001://카쿰
            case 123356782://스우
            case 123356783://스우
            case 123356784://스우
            case 123356786://데미
            case 123356787://데미
            case 240060001://카혼
            case 240060101://카혼
            case 240060201://카혼
            case 240060000://혼테일
            case 240060100://혼테일
            case 240060200://혼테일
            case 270050100://핑크빈
            case 211070100://반레온
            case 211070101://반레온
            case 211070110://반레온
            case 271040100://여제
            case 802000111://카무나
            case 802000411://칼리하
            case 802000711://듀나스
            case 802000211://베르가모트
            case 802000611://니벨룽겐
            case 802000801://코어블레이즈1
            case 802000803://코어블레이즈2
            case 802000821://헤븐
            case 123356785://매그너스 힐라
            case 450004150://루시드1
            case 450004550://루시드2
            case 123356789://검마1
            case 123356788://검마2        
            {
                return true;
            }
        }
        return false;
    }

    public static final int[] publicNpcIds = {9270035, 9070004, 9010022, 9071003, 9000087, 9000088, 9010000, 9000085, 9000018, 9000000};
    public static final String[] publicNpcs = {"#cUniversal NPC#", "Move to the #cBattle Square# to fight other players", "Move to a variety of #cparty quests#.", "Move to #cMonster Park# to team up to defeat monsters.", "Move to #cFree Market# to trade items with players.", "Move to #cArdentmill#, the crafting town.",
        "Check #cdrops# of any monster in the map.", "Review #cPokedex#.", "Review #cPokemon#.", "Join an #cevent# in progress."};
    //questID; FAMILY USES 19000x, MARRIAGE USES 16000x, EXPED USES 16010x
    //dojo = 150000, bpq = 150001, master monster portals: 122600
    //compensate evan = 170000, compensate sp = 170001
    public static final int OMOK_SCORE = 122200;
    public static final int MATCH_SCORE = 122210;
    public static final int HP_ITEM = 122221;
    public static final int MP_ITEM = 122223;
    public static final int JAIL_TIME = 123455;
    public static final int JAIL_QUEST = 123456;
    public static final int REPORT_QUEST = 123457;
    public static final int ULT_EXPLORER = 111111;
    //codex = -55 slot
    //crafting/gathering are designated as skills(short exp then byte 0 then byte level), same with recipes(integer.max_value skill level)
    public static final int POKEMON_WINS = 122400;
    public static final int ENERGY_DRINK = 122500;
    public static final int HARVEST_TIME = 122501;
    public static final int PENDANT_SLOT = 122700;
    public static final int CURRENT_SET = 122800;
    public static final int BOSS_PQ = 150001;
    public static final int JAGUAR = 111112;
    public static final int DOJO = 150100;
    public static final int DOJO_RECORD = 150101;
    public static final int PARTY_REQUEST = 122900;
    public static final int PARTY_INVITE = 122901;
    public static final int QUICK_SLOT = 123000;
}
