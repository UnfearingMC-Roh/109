package client;

import client.MapleTrait.MapleTraitType;
import constants.GameConstants;
import client.inventory.MapleInventoryType;
import client.inventory.MapleInventory;
import client.inventory.Item;
import client.inventory.ItemLoader;
import client.inventory.MapleMount;
import client.inventory.MaplePet;
import client.inventory.ItemFlag;
import client.inventory.MapleRing;
import java.awt.Point;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Deque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.Serializable;

import handling.login.LoginInformationProvider.JobType;
import client.anticheat.CheatTracker;
import client.anticheat.ReportType;
import client.customize.CustomizeStat;
import client.inventory.Equip;
import client.inventory.MapleAndroid;
import client.inventory.MapleImp;
import client.inventory.MapleImp.ImpFlag;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.MapConstants;
import constants.ServerConstants;
import constants.ServerConstants.PlayerGMRank;
import database.DatabaseConnection;
import database.DatabaseException;

import handling.channel.ChannelServer;
import handling.channel.handler.DueyHandler;
import handling.channel.handler.PlayerHandler;
import handling.login.LoginServer;
import handling.world.CharacterTransfer;
import handling.world.MapleMessenger;
import handling.world.MapleMessengerCharacter;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.PartyOperation;
import handling.world.PlayerBuffStorage;
import handling.world.PlayerBuffValueHolder;
import handling.world.World;
import handling.world.family.MapleFamily;
import handling.world.family.MapleFamilyBuff;
import handling.world.family.MapleFamilyCharacter;
import handling.world.guild.MapleGuild;
import handling.world.guild.MapleGuildCharacter;
import handling.world.sidekick.MapleSidekick;
import java.awt.Rectangle;
import java.io.File;
import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import scripting.EventInstanceManager;
import scripting.EventManager;
import scripting.NPCScriptManager;
import server.AutobanManager;
import server.MapleAchievements;
import server.MaplePortal;
import server.MapleShop;
import server.MapleStatEffect;
import server.MapleStorage;
import server.MapleTrade;
import server.Randomizer;
import server.RandomRewards;
import server.MapleCarnivalParty;
import server.MapleItemInformationProvider;
import server.life.MapleMonster;
import server.maps.AnimatedMapleMapObject;
import server.maps.MapleDoor;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleSummon;
import server.maps.FieldLimitType;
import server.maps.SavedLocationType;
import server.quest.MapleQuest;
import server.shops.IMaplePlayerShop;
import server.CashShop;
import server.ItemInformation;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.packet.MTSCSPacket;
import tools.packet.CSPacket;
import tools.packet.MobPacket;
import tools.packet.PetPacket;
import tools.packet.MonsterCarnivalPacket;
import tools.packet.UIPacket;
import server.MapleCarnivalChallenge;
import server.MapleInventoryManipulator;
import server.MapleStatEffect.CancelEffectAction;
import server.StructBonusExp;
import server.Timer;
import server.Timer.BuffTimer;
import server.Timer.MapTimer;
import server.life.MapleLifeFactory;
import server.life.MapleMonsterInformationProvider;
import server.life.MapleNPC;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.life.MonsterDropEntry;
import server.life.MonsterGlobalDropEntry;
import server.life.PlayerNPC;
import server.log.LogType;
import server.log.ServerLogger;
import server.maps.Event_PyramidSubway;
import server.maps.MapleDragon;
import server.maps.MapleExtractor;
import server.maps.MapleFoothold;
import server.maps.MechDoor;
import server.marriage.MarriageDataEntry;
import server.marriage.MarriageManager;
import server.movement.LifeMovementFragment;
import system.BuffHandler;
import tools.ConcurrentEnumMap;
import tools.FileoutputUtil;
import static tools.FileoutputUtil.CurrentReadable_Time;
import tools.StringUtil;
import tools.Triple;
import tools.packet.FamilyPacket;
import tools.packet.PlayerShopPacket;
import tools.packet.TemporaryStatsPacket;
import util.FileTime;

public class MapleCharacter extends AnimatedMapleMapObject implements Serializable {

    private static final long serialVersionUID = 845748950829L;
    private String name, chalktext, BlessOfFairy_Origin, BlessOfEmpress_Origin, teleportname;
    private long lastCombo, lastfametime, keydown_skill, nextConsume, pqStartTime, lastDragonBloodTime,
            lastBerserkTime, lastRecoveryTime, lastSummonTime, mapChangeTime, lastFishingTime, lastFairyTime,
            lastHPTime, lastMPTime, lastFamiliarEffectTime, lastDOTTime;
    private byte gmLevel, gender, initialSpawnPoint, skinColor, guildrank = 5, allianceRank = 5, spawnPoint, mobKilledNo, hoursFromLogin,
            world, fairyExp, subcategory;
    private short level, mulung_energy, combo, availableCP, fatigue, totalCP, hpApUsed, job, remainingAp, scrolledPosition, emblem;
    private int accountid, id, meso, exp, hair, face, mapid, fame, pvpExp, pvpPoints, totalWins, totalLosses,
            guildid = 0, fallcounter, maplepoints, acash, chair, itemEffect, points, vpoints,
            rank = 1, rankMove = 0, jobRank = 1, jobRankMove = 0, marriageId, marriageItemId, dotHP, primium_time, ultimate,
            currentrep, totalrep, coconutteam, followid, battleshipHP, gachexp, challenge, donatecash, guildContribution = 0;
    private Point old;
    private int[] wishlist, rocks, savedLocations, regrocks, hyperrocks, remainingSp = new int[10];
    private transient AtomicInteger inst, insd;
    private transient List<LifeMovementFragment> lastres;
    private List<Integer> lastmonthfameids, lastmonthbattleids, extendedSlots;
    private List<MapleDoor> doors;
    private List<MechDoor> mechDoors;
    private List<MaplePet> pets;
    public int killp = 0;
    private MaplePet[] petz = new MaplePet[3];
    private List<Item> rebuy;
    private MapleImp[] imps;
    private long extraexp;
    public int kentatime = 0;
    private transient Set<MapleMonster> controlled;
    private transient Set<MapleMapObject> visibleMapObjects;
    private transient ReentrantReadWriteLock visibleMapObjectsLock;
    private transient ReentrantReadWriteLock summonsLock;
    private transient ReentrantReadWriteLock controlledLock;
    private transient MapleAndroid android;
    public ScheduledFuture<?> macrotimer = null;
    private Map<MapleQuest, MapleQuestStatus> quests;
    private Map<Integer, String> questinfo;
    private Map<Skill, SkillEntry> skills;
    private transient Map<MapleBuffStat, MapleBuffStatValueHolder> effects;
    private transient List<MapleSummon> summons;
    private transient Map<Integer, MapleCoolDownValueHolder> coolDowns;
    private transient Map<MapleDisease, MapleDiseaseValueHolder> diseases;
    private Map<ReportType, Integer> reports;
    private CashShop cs;
    private transient Deque<MapleCarnivalChallenge> pendingCarnivalRequests;
    private transient MapleCarnivalParty carnivalParty;
    private BuddyList buddylist;
    private MonsterBook monsterbook;
    private transient CheatTracker anticheat;
    private MapleClient client;
    private transient MapleParty party;
    private PlayerStats stats;
    private transient MapleMap map;
    private transient MapleShop shop;
    private transient MapleAdminShop adminshop;
    private transient MapleDragon dragon;
    private transient MapleExtractor extractor;
    private boolean hide;
    private transient RockPaperScissors rps;
    private MapleSidekick sidekick;
    private MapleStorage storage;
    //private transient boolean usingStrongBuff = false;    
    private transient MapleTrade trade;
    private MapleMount mount;
    private List<Integer> finishedAchievements;
    private MapleMessenger messenger;
    private byte[] petStore;
    private String macrostr = "";
    private int canGainNoteFame = 0;
    private transient IMaplePlayerShop playerShop;
    private boolean invincible, canTalk, followinitiator, followon, smega, hasSummon;
    private MapleGuildCharacter mgc;
    private MapleFamilyCharacter mfc;
    private transient EventInstanceManager eventInstance;
    private MapleInventory[] inventory;
    private SkillMacro[] skillMacros = new SkillMacro[5];
    private EnumMap<MapleTraitType, MapleTrait> traits;
    private MapleKeyLayout keylayout;
    private transient ScheduledFuture<?> mapTimeLimitTask;
    private transient Event_PyramidSubway pyramidSubway = null;
    private int eventbuff, eventbufftime, eventbuffuse;
    public int DiceCooltime = 0, doorid = 0;
    private boolean goDonateCashShop = false;
    private transient List<Integer> pendingExpiration = null, pendingSkills = null;
    private transient Map<Integer, Integer> linkMobs;
    private boolean changed_wishlist, changed_trocklocations, changed_regrocklocations, changed_hyperrocklocations, changed_skillmacros, changed_achievements,
            changed_savedlocations, changed_pokemon, changed_questinfo, changed_skills, changed_reports, changed_extendedSlots;
    private int bonusExpR = 0;
    private boolean updateAccepted = false;
    private transient MapleClaim claim;
    private static String[] ariantroomleader = new String[3];
    private static int[] ariantroomslot = new int[3];
    public int LastSkill = 0;
    private Map<String, String> CustomValues = new HashMap<String, String>();
    private Map<String, Integer> CustomValues2 = new HashMap<String, Integer>();
    public Map<Integer, Integer> MATKValue = new HashMap<Integer, Integer>();
    public Map<Integer, Integer> WATKValue = new HashMap<Integer, Integer>();
    public boolean keyvalue_changed = false;//키밸류
    private long damagetotal, nowdamagetotal, damagerank;
    private boolean damagecount = false;
    private boolean autostatus = true;
    private boolean hottimeboss = false, hottimebosslastattack = false, hottimebossattackcheck = false, hottime = false;//핫탐
    private boolean eventboss = false, eventbosslastattack = false, eventbossattackcheck = false;//이벤트랜덤보스
    public int min;
    public int max;
    public long lastfishingtime;
    public int cubeitemid = -1;
    public int guild_buff;
    public int hunter_buff;
    public int guild_stat_watk;
    public int guild_stat_matk;
    public int guild_stat_boss;

    public List<PlayerBuffValueHolder> revieveReturnBuffs = new ArrayList<>();

    private FileTime equipExtExpire;

    public Map<Integer, Integer> boosterVal = new HashMap<>();
    public Map<MapleBuffStat, Map<Integer, Integer>> lCTS = new HashMap<>();
    public int mobKillCount;
    private Map<String, String> keyValues;

    public static String getAriantRoomLeaderName(int room) {
        return ariantroomleader[room];
    }

    public static int getAriantSlotsRoom(int room) {
        return ariantroomslot[room];
    }

    public static void removeAriantRoom(int room) {
        ariantroomleader[room] = "";
        ariantroomslot[room] = 0;
    }

    public static void setAriantRoomLeader(int room, String charname) {
        ariantroomleader[room] = charname;
    }

    public static void setAriantSlotRoom(int room, int slot) {
        ariantroomslot[room] = slot;
    }

    private MapleCharacter(final boolean ChannelServer) {
        setStance(0);
        setPosition(new Point(0, 0));

        inventory = new MapleInventory[MapleInventoryType.values().length];
        for (MapleInventoryType type : MapleInventoryType.values()) {
            inventory[type.ordinal()] = new MapleInventory(type);
        }
        quests = new LinkedHashMap<MapleQuest, MapleQuestStatus>(); // Stupid erev quest.
        this.keyValues = new LinkedHashMap<>();
        skills = new LinkedHashMap<Skill, SkillEntry>(); //Stupid UAs.
        stats = new PlayerStats();
        for (int i = 0; i < remainingSp.length; i++) {
            remainingSp[i] = 0;
        }
        traits = new EnumMap<MapleTraitType, MapleTrait>(MapleTraitType.class);
        for (MapleTraitType t : MapleTraitType.values()) {
            traits.put(t, new MapleTrait(t));
        }
        if (ChannelServer) {
            changed_reports = false;
            changed_skills = false;
            changed_achievements = false;
            changed_wishlist = false;
            changed_trocklocations = false;
            changed_regrocklocations = false;
            changed_hyperrocklocations = false;
            changed_skillmacros = false;
            changed_savedlocations = false;
            changed_pokemon = false;
            changed_extendedSlots = false;
            changed_questinfo = false;
            scrolledPosition = 0;
            lastCombo = 0;
            mulung_energy = 0;
            combo = 0;
            keydown_skill = 0;
            nextConsume = 0;
            pqStartTime = 0;
            fairyExp = 0;
            mapChangeTime = 0;
            lastRecoveryTime = 0;
            lastDragonBloodTime = 0;
            lastBerserkTime = 0;
            lastFishingTime = 0;
            lastFairyTime = 0;
            lastHPTime = 0;
            lastMPTime = 0;
            lastFamiliarEffectTime = 0;
            old = new Point(0, 0);
            coconutteam = 0;
            followid = 0;
            battleshipHP = 0;
            marriageItemId = 0;
            fallcounter = 0;
            challenge = 0;
            dotHP = 0;
            lastSummonTime = 0;
            hasSummon = false;
            invincible = false;
            canTalk = true;
            followinitiator = false;
            followon = false;
            rebuy = new ArrayList<Item>();
            linkMobs = new HashMap<Integer, Integer>();
            finishedAchievements = new ArrayList<Integer>();
            reports = new EnumMap<ReportType, Integer>(ReportType.class);
            teleportname = "";
            smega = true;
            petStore = new byte[3];
            for (int i = 0; i < petStore.length; i++) {
                petStore[i] = (byte) -1;
            }
            wishlist = new int[10];
            rocks = new int[10];
            regrocks = new int[5];
            hyperrocks = new int[13];
            imps = new MapleImp[3];
            extendedSlots = new ArrayList<Integer>();
            effects = new ConcurrentEnumMap<MapleBuffStat, MapleBuffStatValueHolder>(MapleBuffStat.class);
            coolDowns = new LinkedHashMap<Integer, MapleCoolDownValueHolder>();
            diseases = new ConcurrentEnumMap<MapleDisease, MapleDiseaseValueHolder>(MapleDisease.class);
            inst = new AtomicInteger(0);// 1 = NPC/ Quest, 2 = Duey, 3 = Hired Merch store, 4 = Storage
            insd = new AtomicInteger(-1);
            keylayout = new MapleKeyLayout();
            doors = new ArrayList<MapleDoor>();
            mechDoors = new ArrayList<MechDoor>();
            controlled = new LinkedHashSet<MapleMonster>();
            controlledLock = new ReentrantReadWriteLock();
            summons = new LinkedList<MapleSummon>();
            summonsLock = new ReentrantReadWriteLock();
            visibleMapObjects = new LinkedHashSet<MapleMapObject>();
            visibleMapObjectsLock = new ReentrantReadWriteLock();
            pendingCarnivalRequests = new LinkedList<MapleCarnivalChallenge>();

            savedLocations = new int[SavedLocationType.values().length];
            for (int i = 0; i < SavedLocationType.values().length; i++) {
                savedLocations[i] = -1;
            }
            questinfo = new LinkedHashMap<Integer, String>();
            pets = new ArrayList<MaplePet>();
        }
    }

    public static MapleCharacter getDefault(final MapleClient client, final JobType type) {
        MapleCharacter ret = new MapleCharacter(false);
        ret.client = client;
        ret.map = null;
        ret.exp = 0;
        ret.gmLevel = 0;
        ret.job = (short) type.id;
        ret.meso = 0;
        ret.level = 1;
        ret.remainingAp = 0;
        ret.fame = 0;
        ret.accountid = client.getAccID();
        ret.buddylist = new BuddyList((byte) 20);

        ret.stats.str = 12;
        ret.stats.dex = 5;
        ret.stats.int_ = 4;
        ret.stats.luk = 4;
        ret.stats.maxhp = 50;
        ret.stats.hp = 50;
        ret.stats.maxmp = 50;
        ret.stats.mp = 50;
        ret.gachexp = 0;
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
            ps.setInt(1, ret.accountid);
            rs = ps.executeQuery();

            if (rs.next()) {
                ret.client.setAccountName(rs.getString("name"));
                ret.acash = rs.getInt("ACash");
                ret.maplepoints = rs.getInt("mPoints");
                ret.points = rs.getInt("points");
                ret.vpoints = rs.getInt("vpoints");
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error getting character default" + e);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }
        }
        return ret;
    }

    public final static MapleCharacter ReconstructChr(final CharacterTransfer ct, final MapleClient client, final boolean isChannel) {
        final MapleCharacter ret = new MapleCharacter(true); // Always true, it's change channel
        ret.client = client;
        if (!isChannel) {
            ret.client.setChannel(ct.channel);
        }
        ret.id = ct.characterid;
        ret.name = ct.name;
        ret.level = ct.level;
        ret.fame = ct.fame;

        ret.stats.str = ct.str;
        ret.stats.dex = ct.dex;
        ret.stats.int_ = ct.int_;
        ret.stats.luk = ct.luk;
        ret.stats.maxhp = ct.maxhp;
        ret.stats.maxmp = ct.maxmp;
        ret.stats.hp = ct.hp;
        ret.stats.mp = ct.mp;

        ret.chalktext = ct.chalkboard;
        ret.gmLevel = ct.gmLevel;
        ret.hide = ret.isGM();
        ret.exp = (ret.level >= 999 || (GameConstants.isKOC(ret.job) && ret.level >= 120)) && !ret.isIntern() ? 0 : ct.exp;
        ret.hpApUsed = ct.hpApUsed;
        ret.remainingSp = ct.remainingSp;
        ret.remainingAp = ct.remainingAp;
        ret.meso = ct.meso;
        ret.skinColor = ct.skinColor;
        ret.gender = ct.gender;
        ret.job = ct.job;
        ret.hair = ct.hair;
        ret.face = ct.face;
        ret.accountid = ct.accountid;
        ret.totalWins = ct.totalWins;
        ret.totalLosses = ct.totalLosses;
        client.setAccID(ct.accountid);
        ret.mapid = ct.mapid;

        //PC방 처리
        ret.pctime = ct.pctime;
        ret.pcdate = ct.pcdate;

        ret.WATKValue = ct.WATKValue;
        ret.MATKValue = ct.MATKValue;
        ret.boosterVal = ct.boosterVal;
        ret.mobKillCount = ct.mobKillCount;
        ret.extraexp = ct.extraexp;
        ret.lCTS = ct.lCTS;
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        int charid = ret.id;

        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT spawnpoint FROM characters WHERE id = ?");
            ps.setInt(1, charid);
            rs = ps.executeQuery();
            while (rs.next()) {
                ret.spawnPoint = rs.getByte("spawnpoint");
            }
        } catch (Exception se) {
            System.err.println("unable to read spawnpoint from sql");
            se.printStackTrace();
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }
        }
        ret.world = ct.world;
        ret.guildid = ct.guildid;
        ret.guildrank = ct.guildrank;
        ret.guildContribution = ct.guildContribution;
        ret.allianceRank = ct.alliancerank;
        ret.points = ct.points;
        ret.vpoints = ct.vpoints;
        ret.fairyExp = ct.fairyExp;
        ret.marriageId = ct.marriageId;
        ret.currentrep = ct.currentrep;
        ret.totalrep = ct.totalrep;
        ret.itemEffect = ct.itemEffect;
        ret.equipExtExpire = ct.equipExtExpire;
        ret.damagetotal = ct.damagetotal;
        ret.gachexp = ct.gachexp;
        ret.pvpExp = ct.pvpExp;
        ret.pvpPoints = ct.pvpPoints;
        ret.makeMFC(ct.familyid, ct.seniorid, ct.junior1, ct.junior2);
        if (ret.guildid > 0) {
            ret.mgc = new MapleGuildCharacter(ret);
        }
        ret.fatigue = ct.fatigue;
        ret.buddylist = new BuddyList(ct.buddysize);
        ret.subcategory = ct.subcategory;

        if (ct.sidekick > 0) {
            ret.sidekick = World.Sidekick.getSidekick(ct.sidekick);
        }

        ret.ultimate = ct.ultimate;

        if (isChannel) {
            final MapleMapFactory mapFactory = ChannelServer.getInstance(client.getChannel()).getMapFactory();
            ret.map = mapFactory.getMap(ret.mapid);
            if (ret.map == null) { //char is on a map that doesn't exist warp it to henesys
                ret.map = mapFactory.getMap(100000000);
            } else {
                if (ret.map.getForcedReturnId() != 999999999 && ret.map.getForcedReturnMap() != null) {
                    ret.map = ret.map.getForcedReturnMap();
                } else if (ret.mapid >= 925010000 && ret.mapid <= 925010300) {
                    ret.map = mapFactory.getMap(120000104);
                }
            }
            MaplePortal portal = ret.map.getPortal(ret.initialSpawnPoint);
            if (portal == null) {
                portal = ret.map.getPortal(0); // char is on a spawnpoint that doesn't exist - select the first spawnpoint instead
                ret.initialSpawnPoint = 0;
            }
            ret.setPosition(portal.getPosition());

            final int messengerid = ct.messengerid;
            if (messengerid > 0) {
                ret.messenger = World.Messenger.getMessenger(messengerid);
            }
        } else {

            ret.messenger = null;
        }
        int partyid = ct.partyid;
        if (partyid >= 0) {
            MapleParty party = World.Party.getParty(partyid);
            if (party != null && party.getMemberById(ret.id) != null) {
                ret.party = party;
            }
        }

        MapleQuestStatus queststatus_from;
        for (final Map.Entry<Integer, Object> qs : ct.Quest.entrySet()) {
            queststatus_from = (MapleQuestStatus) qs.getValue();
            queststatus_from.setQuest(qs.getKey());
            ret.quests.put(queststatus_from.getQuest(), queststatus_from);
        }
//        for (Entry<String, String> entry : ct.keyValues.entrySet()) {
//            ret.keyValues.put(entry.getKey(), entry.getValue());
//        }
        for (final Map.Entry<Integer, SkillEntry> qs : ct.Skills.entrySet()) {
            ret.skills.put(SkillFactory.getSkill(qs.getKey()), qs.getValue());
        }
        for (final Integer zz : ct.finishedAchievements) {
            ret.finishedAchievements.add(zz);
        }
        //       for (Entry<MapleTraitType, Integer> t : ct.traits.entrySet()) {
        //          ret.traits.get(t.getKey()).setExp(t.getValue());
        //     }
        for (final Map.Entry<Byte, Integer> qs : ct.reports.entrySet()) {
            ret.reports.put(ReportType.getById(qs.getKey()), qs.getValue());
        }
        ret.monsterbook = new MonsterBook(ct.mbook, ret);
        ret.inventory = (MapleInventory[]) ct.inventorys;
        ret.BlessOfFairy_Origin = ct.BlessOfFairy;
        ret.BlessOfEmpress_Origin = ct.BlessOfEmpress;
        ret.skillMacros = (SkillMacro[]) ct.skillmacro;
        ret.petStore = ct.petStore;
        ret.keylayout = new MapleKeyLayout(ct.keymap);
        ret.questinfo = ct.InfoQuest;
        ret.savedLocations = ct.savedlocation;
        ret.wishlist = ct.wishlist;
        ret.rocks = ct.rocks;
        ret.regrocks = ct.regrocks;
        ret.hyperrocks = ct.hyperrocks;
        ret.buddylist.loadFromTransfer(ct.buddies);
        ret.emblem = ct.emblem;
        // ret.lastfametime
        // ret.lastmonthfameids
        ret.keydown_skill = 0; // Keydown skill can't be brought over
        ret.lastfametime = ct.lastfametime;
        ret.lastmonthfameids = ct.famedcharacters;
        ret.lastmonthbattleids = ct.battledaccs;
        ret.extendedSlots = ct.extendedSlots;
        ret.storage = (MapleStorage) ct.storage;
        ret.cs = (CashShop) ct.cs;
        client.setAccountName(ct.accountname);
        ret.acash = ct.ACash;
        ret.maplepoints = ct.MaplePoints;
        ret.imps = ct.imps;
        ret.anticheat = (CheatTracker) ct.anticheat;
        ret.anticheat.start(ret);
        ret.rebuy = ct.rebuy;
        ret.mount = new MapleMount(ret, ct.mount_itemid, ret.stats.getSkillByJob(1004, ret.job), ct.mount_Fatigue, ct.mount_level, ct.mount_exp);
        ret.expirationTask(false, false);
        ret.stats.recalcLocalStats(true, ret);
        client.setTempIP(ct.tempIP);

        return ret;
    }

    public static MapleCharacter loadCharFromDB(int charid, MapleClient client, boolean channelserver) {
        final MapleCharacter ret = new MapleCharacter(channelserver);
        ret.client = client;
        ret.id = charid;

        Connection con = null;
        PreparedStatement ps = null;
        PreparedStatement pse = null;
        ResultSet rs = null;

        try {
            con = DatabaseConnection.getConnection();

            ps = con.prepareStatement("SELECT * FROM characters WHERE id = ?");
            ps.setInt(1, charid);
            rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                throw new RuntimeException("Loading the Char Failed (char not found)");
            }
            ret.name = rs.getString("name");
            ret.level = rs.getShort("level");
            ret.fame = rs.getInt("fame");

            ret.stats.str = rs.getShort("str");
            ret.stats.dex = rs.getShort("dex");
            ret.stats.int_ = rs.getShort("int");
            ret.stats.luk = rs.getShort("luk");
            ret.stats.maxhp = rs.getInt("maxhp");
            ret.stats.maxmp = rs.getInt("maxmp");
            ret.stats.hp = rs.getInt("hp");
            ret.stats.mp = rs.getInt("mp");
            ret.job = rs.getShort("job");
            ret.gmLevel = rs.getByte("gm");
            ret.hide = ret.isGM();
            ret.exp = rs.getInt("exp");
            ret.hpApUsed = rs.getShort("hpApUsed");
            final String[] sp = rs.getString("sp").split(",");
            for (int i = 0; i < ret.remainingSp.length; i++) {
                ret.remainingSp[i] = Integer.parseInt(sp[i]);
            }
            ret.remainingAp = rs.getShort("ap");
            ret.meso = rs.getInt("meso");
            ret.skinColor = rs.getByte("skincolor");
            ret.gender = rs.getByte("gender");

            ret.hair = rs.getInt("hair");
            ret.face = rs.getInt("face");
            ret.accountid = rs.getInt("accountid");
            client.setAccID(ret.accountid);
            ret.mapid = rs.getInt("map");
            ret.spawnPoint = rs.getByte("spawnpoint");
            ret.world = rs.getByte("world");
            ret.guildid = rs.getInt("guildid");
            ret.guildrank = rs.getByte("guildrank");
            ret.allianceRank = rs.getByte("allianceRank");
            ret.guildContribution = rs.getInt("guildContribution");
            ret.totalWins = rs.getInt("totalWins");
            ret.totalLosses = rs.getInt("totalLosses");
            ret.currentrep = rs.getInt("currentrep");
            ret.totalrep = rs.getInt("totalrep");
            ret.extraexp = rs.getLong("extraexp");
            ret.makeMFC(rs.getInt("familyid"), rs.getInt("seniorid"), rs.getInt("junior1"), rs.getInt("junior2"));
            if (ret.guildid > 0) {
                ret.mgc = new MapleGuildCharacter(ret);
            }
            ret.gachexp = rs.getInt("gachexp");
            ret.buddylist = new BuddyList(rs.getByte("buddyCapacity"));
            ret.subcategory = rs.getByte("subcategory");
            ret.mount = new MapleMount(ret, 0, ret.stats.getSkillByJob(1004, ret.job), (byte) 0, (byte) 1, 0);
            ret.rank = rs.getInt("rank");
            ret.rankMove = rs.getInt("rankMove");
            ret.jobRank = rs.getInt("jobRank");
            ret.jobRankMove = rs.getInt("jobRankMove");
            ret.marriageId = rs.getInt("marriageId");
            ret.fatigue = rs.getShort("fatigue");
            ret.pvpExp = rs.getInt("pvpExp");
            ret.pvpPoints = rs.getInt("pvpPoints");
            ret.damagetotal = rs.getLong("damagetotal");
            ret.emblem = rs.getShort("emblem");

            ret.ultimate = rs.getInt("ultimate");
            ret.equipExtExpire = FileTime.longToFileTime(rs.getLong("equipExtExpire"));

            //           for (MapleTrait t : ret.traits.values()) {
            //               t.setExp(rs.getInt(t.getType().name()));
            //           }
            if (channelserver) {
                ret.anticheat = new CheatTracker(ret);
                MapleMapFactory mapFactory = ChannelServer.getInstance(client.getChannel()).getMapFactory();
                ret.map = mapFactory.getMap(ret.mapid);
                if (ret.map == null) { //char is on a map that doesn't exist warp it to henesys
                    ret.map = mapFactory.getMap(100000000);
                }
                MaplePortal portal = ret.map.getPortal(ret.initialSpawnPoint);
                if (portal == null) {
                    portal = ret.map.getPortal(0); // char is on a spawnpoint that doesn't exist - select the first spawnpoint instead
                    ret.initialSpawnPoint = 0;
                }
                ret.setPosition(portal.getPosition());

                int partyid = rs.getInt("party");
                if (partyid >= 0) {
                    MapleParty party = World.Party.getParty(partyid);
                    if (party != null && party.getMemberById(ret.id) != null) {
                        ret.party = party;
                    }
                }
                final String[] pets = rs.getString("pets").split(",");
                for (int i = 0; i < ret.petStore.length; i++) {
                    ret.petStore[i] = Byte.parseByte(pets[i]);
                }
                rs.close();
                ps.close();
                ps = con.prepareStatement("SELECT * FROM achievements WHERE accountid = ?");
                ps.setInt(1, ret.accountid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.finishedAchievements.add(rs.getInt("achievementid"));
                }
                ps.close();
                rs.close();

                ps = con.prepareStatement("SELECT * FROM reports WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    if (ReportType.getById(rs.getByte("type")) != null) {
                        ret.reports.put(ReportType.getById(rs.getByte("type")), rs.getInt("count"));
                    }
                }

            }
            rs.close();
            ps.close();

            ps = con.prepareStatement("SELECT * FROM c_pctime WHERE acc = ?");
            ps.setInt(1, ret.accountid);
            rs = ps.executeQuery();
            if (rs.next()) { // 캐릭터를 불러올 때 시간이 같으면 가져오고 아니면 리셋 //피시방
                if (rs.getInt(4) == GameConstants.getCurrentDate_NoTime()) {
                    ret.pctime = rs.getLong(3);
                    ret.pcdate = rs.getInt(4);
                }
            }
            ps.close();//피시방 불러오기
            rs.close();

            ps = con.prepareStatement("SELECT * FROM queststatus WHERE characterid = ?");
            ps.setInt(1, charid);
            rs = ps.executeQuery();
            pse = con.prepareStatement("SELECT * FROM queststatusmobs WHERE queststatusid = ?");

            while (rs.next()) {
                final int id = rs.getInt("quest");
                final MapleQuest q = MapleQuest.getInstance(id);
                final byte stat = rs.getByte("status");
                if ((stat == 1 || stat == 2) && channelserver && (q == null || q.isBlocked())) { //bigbang
                    continue;
                }
                if (stat == 1 && channelserver && !q.canStart(ret, null)) { //bigbang
                    continue;
                }
                final MapleQuestStatus status = new MapleQuestStatus(q, stat);
                final long cTime = rs.getLong("time");
                if (cTime > -1) {
                    status.setCompletionTime(cTime * 1000);
                }
                status.setForfeited(rs.getInt("forfeited"));
                status.setCustomData(rs.getString("customData"));
                ret.quests.put(q, status);
                pse.setInt(1, rs.getInt("queststatusid"));
                final ResultSet rsMobs = pse.executeQuery();

                while (rsMobs.next()) {
                    status.setMobKills(rsMobs.getInt("mob"), rsMobs.getInt("count"));
                }
                rsMobs.close();
            }
            rs.close();
            ps.close();
            pse.close();

//            ps = con.prepareStatement("SELECT * FROM customvalues WHERE `cid` = ?");
//            ps.setInt(1, ret.id);
//            rs = ps.executeQuery();
//            
//            while (rs.next()) {
//                ret.keyValues.put(rs.getString("key"), rs.getString("value"));
//            }
//            rs.close();
//            ps.close();
            if (channelserver) {
                ret.monsterbook = MonsterBook.loadCards(ret.accountid, ret);

                ps = con.prepareStatement("SELECT * FROM inventoryslot where characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();

                if (!rs.next()) {
                    rs.close();
                    ps.close();
                    throw new RuntimeException("No Inventory slot column found in SQL. [inventoryslot]");
                } else {
                    ret.getInventory(MapleInventoryType.EQUIP).setSlotLimit(rs.getByte("equip"));
                    ret.getInventory(MapleInventoryType.USE).setSlotLimit(rs.getByte("use"));
                    ret.getInventory(MapleInventoryType.SETUP).setSlotLimit(rs.getByte("setup"));
                    ret.getInventory(MapleInventoryType.ETC).setSlotLimit(rs.getByte("etc"));
                    ret.getInventory(MapleInventoryType.CASH).setSlotLimit(rs.getByte("cash"));
                }
                ps.close();
                rs.close();

                for (Pair<Item, MapleInventoryType> mit : ItemLoader.INVENTORY.loadItems(false, charid).values()) {
                    ret.getInventory(mit.getRight()).addFromDB(mit.getLeft());
//                    if (mit.getLeft().getPet() != null) {
//                        //ret.pets.add(mit.getLeft().getPet());
////                        mit.getLeft().getPet().setSummoned(1);
//                        ret.addPetz(mit.getLeft().getPet());
////                        mit.getLeft().getPet().setSummoned(ret.getPetIndex(mit.getLeft().getPet()) + 1);
//                    }
                }

                ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
                ps.setInt(1, ret.accountid);
                rs = ps.executeQuery();
                if (rs.next()) {
                    ret.getClient().setAccountName(rs.getString("name"));
                    ret.acash = rs.getInt("ACash");
                    ret.maplepoints = rs.getInt("mPoints");
                    ret.points = rs.getInt("points");
                    ret.vpoints = rs.getInt("vpoints");
                    ret.primium_time = rs.getInt("primium");

                    if (rs.getTimestamp("lastlogon") != null) {
                        final Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(rs.getTimestamp("lastlogon").getTime());
                        if (cal.get(Calendar.DAY_OF_WEEK) + 1 == Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
                            ret.acash += GameConstants.GMS ? 375 : 500;
                        }
                    }
                    if (rs.getInt("banned") > 0) {
                        rs.close();
                        ps.close();
                        ret.getClient().getSession().close();
                        throw new RuntimeException("Loading a banned character");
                    }
                    rs.close();
                    ps.close();

                    ps = con.prepareStatement("UPDATE accounts SET lastlogon = CURRENT_TIMESTAMP() WHERE id = ?");
                    ps.setInt(1, ret.accountid);
                    ps.executeUpdate();
                } else {
                    rs.close();
                }
                ps.close();

                ps = con.prepareStatement("SELECT * FROM questinfo WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();

                while (rs.next()) {
                    ret.questinfo.put(rs.getInt("quest"), rs.getString("customData"));
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT skillid, skilllevel, masterlevel, expiration FROM skills WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                Skill skil;
                while (rs.next()) {
                    final int skid = rs.getInt("skillid");
                    skil = SkillFactory.getSkill(skid);
                    int skl = rs.getInt("skilllevel");
                    byte msl = rs.getByte("masterlevel");
                    if (skil != null && GameConstants.isApplicableSkill(skid)) {
                        if (skl > skil.getMaxLevel() && skid < 92000000) {
                            if (!skil.isBeginnerSkill() && skil.canBeLearnedBy(ret.job) && !skil.isSpecialSkill()) {
                                ret.remainingSp[GameConstants.getSkillBookForSkill(skid)] += (skl - skil.getMaxLevel());
                            }
                            skl = (byte) skil.getMaxLevel();
                        }
                        if (msl > skil.getMaxLevel()) {
                            msl = (byte) skil.getMaxLevel();
                        }
                        ret.skills.put(skil, new SkillEntry(skl, msl, rs.getLong("expiration")));
                    } else if (skil == null) { //doesnt. exist. e.g. bb
                        if (!GameConstants.isBeginnerJob(skid / 10000) && skid / 10000 != 900 && skid / 10000 != 800 && skid / 10000 != 9000) {
                            ret.remainingSp[GameConstants.getSkillBookForSkill(skid)] += skl;
                        }
                    }
                }
                rs.close();
                ps.close();

                ret.expirationTask(false, true); //do it now

                // Bless of Fairy handling
                ps = con.prepareStatement("SELECT * FROM characters WHERE accountid = ? ORDER BY level DESC");
                ps.setInt(1, ret.accountid);
                rs = ps.executeQuery();
                int maxlevel_ = 0, maxlevel_2 = 0;
                while (rs.next()) {
                    if (rs.getInt("id") != charid) { // Not this character
                        if (GameConstants.isKOC(rs.getShort("job"))) {
                            int maxlevel = (rs.getShort("level") / 5);

                            if (maxlevel > 24) {
                                maxlevel = 24;
                            }
                            if (maxlevel > maxlevel_2 || maxlevel_2 == 0) {
                                maxlevel_2 = maxlevel;
                                ret.BlessOfEmpress_Origin = rs.getString("name");
                            }
                        }
                        int maxlevel = (rs.getShort("level") / 10);

                        if (maxlevel > 20) {
                            maxlevel = 20;
                        }
                        if (maxlevel > maxlevel_ || maxlevel_ == 0) {
                            maxlevel_ = maxlevel;
                            ret.BlessOfFairy_Origin = rs.getString("name");
                        }
                    }
                }
                /*if (!compensate_previousSP) {
                 for (Entry<Skill, SkillEntry> skill : ret.skills.entrySet()) {
                 if (!skill.getKey().isBeginnerSkill() && !skill.getKey().isSpecialSkill()) {
                 ret.remainingSp[GameConstants.getSkillBookForSkill(skill.getKey().getId())] += skill.getValue().skillevel;
                 skill.getValue().skillevel = 0;
                 }
                 }
                 ret.setQuestAdd(MapleQuest.getInstance(170000), (byte) 0, null); //set it so never again
                 }*/
                if (ret.BlessOfFairy_Origin == null) {
                    ret.BlessOfFairy_Origin = ret.name;
                }
                ret.skills.put(SkillFactory.getSkill(GameConstants.getBOF_ForJob(ret.job)), new SkillEntry(maxlevel_, (byte) 0, -1));
                if (SkillFactory.getSkill(GameConstants.getEmpress_ForJob(ret.job)) != null) {
                    if (ret.BlessOfEmpress_Origin == null) {
                        ret.BlessOfEmpress_Origin = ret.BlessOfFairy_Origin;
                    }
                    ret.skills.put(SkillFactory.getSkill(GameConstants.getEmpress_ForJob(ret.job)), new SkillEntry(maxlevel_2, (byte) 0, -1));
                }
                ps.close();
                rs.close();
                // END

                ps = con.prepareStatement("SELECT * FROM skillmacros WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                int position;
                while (rs.next()) {
                    position = rs.getInt("position");
                    SkillMacro macro = new SkillMacro(rs.getInt("skill1"), rs.getInt("skill2"), rs.getInt("skill3"), rs.getString("name"), rs.getInt("shout"), position);
                    ret.skillMacros[position] = macro;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT * FROM familiars WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT `key`,`type`,`action` FROM keymap WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();

                final Map<Integer, Pair<Byte, Integer>> keyb = ret.keylayout.Layout();
                while (rs.next()) {
                    keyb.put(Integer.valueOf(rs.getInt("key")), new Pair<Byte, Integer>(rs.getByte("type"), rs.getInt("action")));
                }
                rs.close();
                ps.close();
                ret.keylayout.unchanged();

                ps = con.prepareStatement("SELECT `locationtype`,`map` FROM savedlocations WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.savedLocations[rs.getInt("locationtype")] = rs.getInt("map");
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT `characterid_to`,`when` FROM famelog WHERE characterid = ? AND DATEDIFF(NOW(),`when`) < 30");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                ret.lastfametime = 0;
                ret.lastmonthfameids = new ArrayList<Integer>(31);
                while (rs.next()) {
                    ret.lastfametime = Math.max(ret.lastfametime, rs.getTimestamp("when").getTime());
                    ret.lastmonthfameids.add(Integer.valueOf(rs.getInt("characterid_to")));
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT `accid_to`,`when` FROM battlelog WHERE accid = ? AND DATEDIFF(NOW(),`when`) < 30");
                ps.setInt(1, ret.accountid);
                rs = ps.executeQuery();
                ret.lastmonthbattleids = new ArrayList<Integer>();
                while (rs.next()) {
                    ret.lastmonthbattleids.add(Integer.valueOf(rs.getInt("accid_to")));
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT `itemId` FROM extendedSlots WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.extendedSlots.add(Integer.valueOf(rs.getInt("itemId")));
                }
                rs.close();
                ps.close();

                ret.buddylist.loadFromDb(charid);
                ret.storage = MapleStorage.loadStorage(ret.accountid);
                ret.cs = new CashShop(ret.accountid, charid, ret.getJob());

                ps = con.prepareStatement("SELECT sn FROM wishlist WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                int i = 0;
                while (rs.next()) {
                    ret.wishlist[i] = rs.getInt("sn");
                    i++;
                }
                while (i < 10) {
                    ret.wishlist[i] = 0;
                    i++;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT mapid FROM trocklocations WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                int r = 0;
                while (rs.next()) {
                    ret.rocks[r] = rs.getInt("mapid");
                    r++;
                }
                while (r < 10) {
                    ret.rocks[r] = 999999999;
                    r++;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT mapid FROM regrocklocations WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                r = 0;
                while (rs.next()) {
                    ret.regrocks[r] = rs.getInt("mapid");
                    r++;
                }
                while (r < 5) {
                    ret.regrocks[r] = 999999999;
                    r++;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT mapid FROM hyperrocklocations WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                r = 0;
                while (rs.next()) {
                    ret.hyperrocks[r] = rs.getInt("mapid");
                    r++;
                }
                while (r < 13) {
                    ret.hyperrocks[r] = 999999999;
                    r++;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT * FROM imps WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                r = 0;
                while (rs.next()) {
                    ret.imps[r] = new MapleImp(rs.getInt("itemid"));
                    ret.imps[r].setLevel(rs.getByte("level"));
                    ret.imps[r].setState(rs.getByte("state"));
                    ret.imps[r].setCloseness(rs.getShort("closeness"));
                    ret.imps[r].setFullness(rs.getShort("fullness"));
                    r++;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT * FROM mountdata WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                if (!rs.next()) {
                    throw new RuntimeException("No mount data found on SQL column");
                }
                final Item mount = ret.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) (-23));
                ret.mount = new MapleMount(ret, mount != null ? mount.getItemId() : 0, GameConstants.GMS ? 80001000 : ret.stats.getSkillByJob(1004, ret.job), rs.getByte("Fatigue"), rs.getByte("Level"), rs.getInt("Exp"));
                ps.close();
                rs.close();

                ret.stats.recalcLocalStats(true, ret);
                ret.claim = new MapleClaim(ret);
            } else { // Not channel server
                for (Pair<Item, MapleInventoryType> mit : ItemLoader.INVENTORY.loadItems(true, charid).values()) {
                    ret.getInventory(mit.getRight()).addFromDB(mit.getLeft());
                }
                ret.stats.recalcPVPRank(ret);
            }
        } catch (Exception ess) {
            ess.printStackTrace();
            System.out.println("Failed to load character..");
            FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, ess);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }
            if (pse != null) {
                try {
                    pse.close();
                } catch (Exception e) {
                }
            }
        }
        return ret;
    }

    public static void saveNewCharToDB(final MapleCharacter chr, final JobType type, short db) {
        Connection con = null;

        PreparedStatement ps = null;
        PreparedStatement pse = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            con.setAutoCommit(false);

            ps = con.prepareStatement("INSERT INTO characters (level, str, dex, luk, `int`, hp, mp, maxhp, maxmp, sp, ap, skincolor, gender, job, hair, face, map, meso, party, buddyCapacity, pets, subcategory, accountid, name, world, ultimate, extraexp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", DatabaseConnection.RETURN_GENERATED_KEYS);
            ps.setInt(1, chr.level); // Level
            final PlayerStats stat = chr.stats;
            ps.setShort(2, stat.getStr()); // Str
            ps.setShort(3, stat.getDex()); // Dex
            ps.setShort(4, stat.getInt()); // Int
            ps.setShort(5, stat.getLuk()); // Luk
            ps.setInt(6, stat.getHp()); // HP
            ps.setInt(7, stat.getMp());
            ps.setInt(8, stat.getMaxHp()); // MP
            ps.setInt(9, stat.getMaxMp());
            final StringBuilder sps = new StringBuilder();
            for (int i = 0; i < chr.remainingSp.length; i++) {
                sps.append(chr.remainingSp[i]);
                sps.append(",");
            }
            final String sp = sps.toString();
            ps.setString(10, sp.substring(0, sp.length() - 1));
            ps.setShort(11, (short) chr.remainingAp); // Remaining AP
            ps.setByte(12, chr.skinColor);
            ps.setByte(13, chr.gender);
            ps.setShort(14, chr.job);
            ps.setInt(15, chr.hair);
            ps.setInt(16, chr.face);
            if (db < 0 || db > (GameConstants.GMS ? 2 : 1)) { //todo legend
                db = 0;
            }
            ps.setInt(17, type.map);
            ps.setInt(18, 0); // Meso
            ps.setInt(19, -1); // Party
            ps.setByte(20, chr.buddylist.getCapacity()); // Buddylist
            ps.setString(21, "-1,-1,-1");
            ps.setInt(22, db); //for now
            ps.setInt(23, chr.getAccountID());
            ps.setString(24, chr.name);
            ps.setByte(25, chr.world);
            ps.setInt(26, chr.ultimate);
            ps.setLong(27, chr.extraexp);
            ps.executeUpdate();

            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                chr.id = rs.getInt(1);
            } else {
                ps.close();
                rs.close();
                throw new DatabaseException("Inserting char failed.");
            }
            ps.close();
            rs.close();
            ps = con.prepareStatement("INSERT INTO queststatus (`queststatusid`, `characterid`, `quest`, `status`, `time`, `forfeited`, `customData`) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?)", DatabaseConnection.RETURN_GENERATED_KEYS);
            pse = con.prepareStatement("INSERT INTO queststatusmobs VALUES (DEFAULT, ?, ?, ?)");
            ps.setInt(1, chr.id);
            for (final MapleQuestStatus q : chr.quests.values()) {
                ps.setInt(2, q.getQuest().getId());
                ps.setInt(3, q.getStatus());
                ps.setInt(4, (int) (q.getCompletionTime() / 1000));
                ps.setInt(5, q.getForfeited());
                ps.setString(6, q.getCustomData());
                ps.execute();
                rs = ps.getGeneratedKeys();
                if (q.hasMobKills()) {
                    rs.next();
                    for (int mob : q.getMobKills().keySet()) {
                        pse.setInt(1, rs.getInt(1));
                        pse.setInt(2, mob);
                        pse.setInt(3, q.getMobKills(mob));
                        pse.execute();
                    }
                }
                rs.close();
            }
            ps.close();
            pse.close();

//            ps = con.prepareStatement("INSERT INTO customvalues (`cid`, `key`, `value`, `day`) VALUES (?, ?, ?, ?)");
//            ps.setInt(1, chr.id);
//            for (Entry<String, String> entry : chr.keyValues.entrySet()) {
//                ps.setString(2, entry.getKey());
//                ps.setString(3, entry.getValue());
//                ps.setInt(4, 1);
//                ps.addBatch();
//            }
//            ps.executeBatch();
//            ps.close();
            ps = con.prepareStatement("INSERT INTO skills (characterid, skillid, skilllevel, masterlevel, expiration) VALUES (?, ?, ?, ?, ?)");
            ps.setInt(1, chr.id);

            for (final Entry<Skill, SkillEntry> skill : chr.skills.entrySet()) {
                if (GameConstants.isApplicableSkill(skill.getKey().getId())) { //do not save additional skills
                    ps.setInt(2, skill.getKey().getId());
                    ps.setInt(3, skill.getValue().skillevel);
                    ps.setByte(4, skill.getValue().masterlevel);
                    ps.setLong(5, skill.getValue().expiration);
                    ps.execute();
                }
            }
            ps.close();

            ps = con.prepareStatement("INSERT INTO inventoryslot (characterid, `equip`, `use`, `setup`, `etc`, `cash`) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setInt(1, chr.id); //인벤칸 늘리기
            ps.setByte(2, (byte) 96); // Eq
            ps.setByte(3, (byte) 96); // Use
            ps.setByte(4, (byte) 96); // Setup
            ps.setByte(5, (byte) 96); // ETC
            ps.setByte(6, (byte) 96); // Cash //캐시창 인벤 설정값
            ps.execute();
            ps.close();

            ps = con.prepareStatement("INSERT INTO mountdata (characterid, `Level`, `Exp`, `Fatigue`) VALUES (?, ?, ?, ?)");
            ps.setInt(1, chr.id);
            ps.setByte(2, (byte) 1);
            ps.setInt(3, 0);
            ps.setByte(4, (byte) 0);
            ps.execute();
            ps.close();

            final int[] array1 = {2, 3, 64, 4, 65, 5, 6, 7, 17, 16, 19, 18, 20, 23, 25, 24, 27, 26, 29, 31, 34, 35, 33, 38, 39, 37, 43, 40, 41, 46, 44, 45, 50, 48, 59, 57, 56, 63, 62, 61, 60};
            final int[] array2 = {4, 4, 6, 4, 6, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 4, 4, 6, 5, 5, 6, 6, 6, 6};
            final int[] array3 = {10, 12, 105, 13, 106, 18, 23, 28, 5, 8, 4, 0, 27, 1, 19, 24, 15, 14, 52, 2, 17, 11, 25, 20, 26, 3, 9, 16, 22, 6, 50, 51, 7, 29, 100, 54, 53, 104, 103, 102, 101};

            ps = con.prepareStatement("INSERT INTO keymap (characterid, `key`, `type`, `action`) VALUES (?, ?, ?, ?)");
            ps.setInt(1, chr.id);
            for (int i = 0; i < array1.length; i++) {
                ps.setInt(2, array1[i]);
                ps.setInt(3, array2[i]);
                ps.setInt(4, array3[i]);
                ps.execute();
            }
            ps.close();

            List<Pair<Item, MapleInventoryType>> listing = new ArrayList<Pair<Item, MapleInventoryType>>();
            for (final MapleInventory iv : chr.inventory) {
                for (final Item item : iv.list()) {
                    listing.add(new Pair<Item, MapleInventoryType>(item, iv.getType()));
                }
            }
            ItemLoader.INVENTORY.saveItems(listing, con, chr.id);

            con.commit();
        } catch (Exception e) {
            FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
            e.printStackTrace();
            System.err.println("[charsave] Error saving character data");
            try {
                con.rollback();
            } catch (SQLException ex) {
                FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, ex);
                ex.printStackTrace();
                System.err.println("[charsave] Error Rolling Back");
            }
        } finally {
            try {
                con.setAutoCommit(true);
                con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            } catch (SQLException e) {
                FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
                e.printStackTrace();
                System.err.println("[charsave] Error going back to autocommit mode");
            }
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }
            if (pse != null) {
                try {
                    pse.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public void saveToDB(boolean dc, boolean fromcs) {
        Connection con = null;

        PreparedStatement ps = null;
        PreparedStatement pse = null;
        ResultSet rs = null;

        try {
            con = DatabaseConnection.getConnection();
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            con.setAutoCommit(false);

            ps = con.prepareStatement("UPDATE characters SET level = ?, fame = ?, str = ?, dex = ?, luk = ?, `int` = ?, exp = ?, hp = ?, mp = ?, maxhp = ?, maxmp = ?, sp = ?, ap = ?, gm = ?, skincolor = ?, gender = ?, job = ?, hair = ?, face = ?, map = ?, meso = ?, hpApUsed = ?, spawnpoint = ?, party = ?, buddyCapacity = ?, pets = ?, subcategory = ?, marriageId = ?, currentrep = ?, totalrep = ?, gachexp = ?, fatigue = ?, charm = ?, charisma = ?, craft = ?, insight = ?, sense = ?, will = ?, totalwins = ?, totallosses = ?, pvpExp = ?, pvpPoints = ?, name = ?, damagetotal = ?, ultimate = ?, extraexp = ?, emblem = ?, equipExtExpire = ? WHERE id = ?", DatabaseConnection.RETURN_GENERATED_KEYS);
            ps.setInt(1, level);
            ps.setInt(2, fame);
            ps.setShort(3, stats.getStr());
            ps.setShort(4, stats.getDex());
            ps.setShort(5, stats.getLuk());
            ps.setShort(6, stats.getInt());
            ps.setInt(7, exp);
            ps.setInt(8, stats.getHp() < 1 ? 50 : stats.getHp());
            ps.setInt(9, stats.getMp());
            ps.setInt(10, stats.getMaxHp());
            ps.setInt(11, stats.getMaxMp());
            final StringBuilder sps = new StringBuilder();
            for (int i = 0; i < remainingSp.length; i++) {
                sps.append(remainingSp[i]);
                sps.append(",");
            }
            final String sp = sps.toString();
            ps.setString(12, sp.substring(0, sp.length() - 1));
            ps.setShort(13, remainingAp);
            ps.setByte(14, gmLevel);
            ps.setByte(15, skinColor);
            ps.setByte(16, gender);
            ps.setShort(17, job);
            ps.setInt(18, hair);
            ps.setInt(19, face);
            if (!fromcs && map != null) { // getForcedReturnMap
                if (map.getForcedReturnMap() != null && map.getForcedReturnId() != 999999999) {
                    ps.setInt(20, map.getForcedReturnId());
                } else if (mapid >= 925010000 && mapid <= 925010300) {
                    ps.setInt(20, 120000104);
                } else {
                    ps.setInt(20, stats.getHp() < 1 ? map.getReturnMapId() : map.getId());
                }
            } else {
                ps.setInt(20, mapid);
            }
            ps.setInt(21, meso);
            ps.setShort(22, hpApUsed);
            if (map == null) {
                if (fromcs) {
                    return;
                } else {
                    ps.setByte(23, (byte) 0);
                }
            } else {
                final MaplePortal closest = map.findClosestSpawnpoint(getTruePosition());
                ps.setByte(23, (byte) (closest != null ? closest.getId() : 0));
            }
            ps.setInt(24, party == null ? -1 : party.getId());
            ps.setShort(25, buddylist.getCapacity());
            final StringBuilder petz = new StringBuilder();
            int petLength = 0;
            for (final MaplePet pet : pets) {
                if (pet.getSummoned()) {
                    pet.saveToDb();
                    petz.append(pet.getInventoryPosition());
                    petz.append(",");
                    petLength++;
                }
            }
            while (petLength < 3) {
                petz.append("-1,");
                petLength++;
            }
            final String petstring = petz.toString();
            ps.setString(26, petstring.substring(0, petstring.length() - 1));
            ps.setByte(27, subcategory);
            ps.setInt(28, marriageId);
            ps.setInt(29, currentrep);
            ps.setInt(30, totalrep);
            ps.setInt(31, gachexp);
            ps.setShort(32, fatigue);
            ps.setInt(33, traits.get(MapleTraitType.charm).getTotalExp());
            ps.setInt(34, traits.get(MapleTraitType.charisma).getTotalExp());
            ps.setInt(35, traits.get(MapleTraitType.craft).getTotalExp());
            ps.setInt(36, traits.get(MapleTraitType.insight).getTotalExp());
            ps.setInt(37, traits.get(MapleTraitType.sense).getTotalExp());
            ps.setInt(38, traits.get(MapleTraitType.will).getTotalExp());
            ps.setInt(39, totalWins);
            ps.setInt(40, totalLosses);
            ps.setInt(41, pvpExp);
            ps.setInt(42, pvpPoints);
            ps.setString(43, name);
            ps.setLong(44, damagetotal);
            ps.setInt(45, ultimate);
            ps.setLong(46, extraexp);
            ps.setShort(47, emblem < 4 ? 4 : emblem); // 수정
            ps.setLong(48, equipExtExpire.fileTimeToLong());
            ps.setInt(49, id);
            if (ps.executeUpdate() < 1) {
                ps.close();
                throw new DatabaseException("Character not in database (" + id + ")");
            }
            ps.close();
            if (changed_skillmacros) {
                deleteWhereCharacterId(con, "DELETE FROM skillmacros WHERE characterid = ?");
                for (int i = 0; i < 5; i++) {
                    final SkillMacro macro = skillMacros[i];
                    if (macro != null) {
                        ps = con.prepareStatement("INSERT INTO skillmacros (characterid, skill1, skill2, skill3, name, shout, position) VALUES (?, ?, ?, ?, ?, ?, ?)");
                        ps.setInt(1, id);
                        ps.setInt(2, macro.getSkill1());
                        ps.setInt(3, macro.getSkill2());
                        ps.setInt(4, macro.getSkill3());
                        ps.setString(5, macro.getName());
                        ps.setInt(6, macro.getShout());
                        ps.setInt(7, i);
                        ps.execute();
                        ps.close();
                    }
                }
            }

            deleteWhereCharacterId(con, "DELETE FROM inventoryslot WHERE characterid = ?");
            ps = con.prepareStatement("INSERT INTO inventoryslot (characterid, `equip`, `use`, `setup`, `etc`, `cash`) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setInt(1, id);
            ps.setByte(2, getInventory(MapleInventoryType.EQUIP).getSlotLimit());
            ps.setByte(3, getInventory(MapleInventoryType.USE).getSlotLimit());
            ps.setByte(4, getInventory(MapleInventoryType.SETUP).getSlotLimit());
            ps.setByte(5, getInventory(MapleInventoryType.ETC).getSlotLimit());
            ps.setByte(6, getInventory(MapleInventoryType.CASH).getSlotLimit());
            ps.execute();
            ps.close();

            saveInventory(con);

            if (changed_questinfo) {
                deleteWhereCharacterId(con, "DELETE FROM questinfo WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO questinfo (`characterid`, `quest`, `customData`) VALUES (?, ?, ?)");
                ps.setInt(1, id);
                for (final Entry<Integer, String> q : questinfo.entrySet()) {
                    ps.setInt(2, q.getKey());
                    ps.setString(3, q.getValue());
                    ps.execute();
                }
                ps.close();
            }

            deleteWhereCharacterId(con, "DELETE FROM queststatus WHERE characterid = ?");
            ps = con.prepareStatement("INSERT INTO queststatus (`queststatusid`, `characterid`, `quest`, `status`, `time`, `forfeited`, `customData`) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?)", DatabaseConnection.RETURN_GENERATED_KEYS);
            pse = con.prepareStatement("INSERT INTO queststatusmobs VALUES (DEFAULT, ?, ?, ?)");
            ps.setInt(1, id);
            for (final MapleQuestStatus q : quests.values()) {
                ps.setInt(2, q.getQuest().getId());
                ps.setInt(3, q.getStatus());
                ps.setInt(4, (int) (q.getCompletionTime() / 1000));
                ps.setInt(5, q.getForfeited());
                ps.setString(6, q.getCustomData());
                ps.execute();
                rs = ps.getGeneratedKeys();
                if (q.hasMobKills()) {
                    rs.next();
                    for (int mob : q.getMobKills().keySet()) {
                        pse.setInt(1, rs.getInt(1));
                        pse.setInt(2, mob);
                        pse.setInt(3, q.getMobKills(mob));
                        pse.execute();
                    }
                }
                rs.close();
            }
            ps.close();
            pse.close();

//            deleteWhereCharacterId(con, "DELETE FROM customvalues WHERE `cid` = ?");
//            ps = con.prepareStatement("INSERT INTO customvalues (`cid`, `key`, `value`, `day`) VALUES (?, ?, ?, ?)");
//            ps.setInt(1, id);
//            for (Entry<String, String> entry : this.keyValues.entrySet()) {
//                ps.setString(2, entry.getKey());
//                ps.setString(3, entry.getValue());
//                ps.setInt(4, 1);
//                ps.addBatch();
//            }
//            ps.executeBatch();
//            ps.close();
            if (changed_skills) {
                deleteWhereCharacterId(con, "DELETE FROM skills WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO skills (characterid, skillid, skilllevel, masterlevel, expiration) VALUES (?, ?, ?, ?, ?)");
                ps.setInt(1, id);

                for (final Entry<Skill, SkillEntry> skill : skills.entrySet()) {
                    if (GameConstants.isApplicableSkill(skill.getKey().getId())) { //do not save additional skills
                        ps.setInt(2, skill.getKey().getId());
                        ps.setInt(3, skill.getValue().skillevel);
                        ps.setByte(4, skill.getValue().masterlevel);
                        ps.setLong(5, skill.getValue().expiration);
                        ps.execute();
                    }
                }
                ps.close();
            }

            List<MapleCoolDownValueHolder> cd = getCooldowns();
            if (dc && cd.size() > 0) {
                ps = con.prepareStatement("INSERT INTO skills_cooldowns (charid, SkillID, StartTime, length) VALUES (?, ?, ?, ?)");
                ps.setInt(1, getId());
                for (final MapleCoolDownValueHolder cooling : cd) {
                    ps.setInt(2, cooling.skillId);
                    ps.setLong(3, cooling.startTime);
                    ps.setLong(4, cooling.length);
                    ps.execute();
                }
                ps.close();
            }

            if (changed_savedlocations) {
                deleteWhereCharacterId(con, "DELETE FROM savedlocations WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO savedlocations (characterid, `locationtype`, `map`) VALUES (?, ?, ?)");
                ps.setInt(1, id);
                for (final SavedLocationType savedLocationType : SavedLocationType.values()) {
                    if (savedLocations[savedLocationType.getValue()] != -1) {
                        ps.setInt(2, savedLocationType.getValue());
                        ps.setInt(3, savedLocations[savedLocationType.getValue()]);
                        ps.execute();
                    }
                }
                ps.close();
            }

            if (changed_achievements) {
                ps = con.prepareStatement("DELETE FROM achievements WHERE accountid = ?");
                ps.setInt(1, accountid);
                ps.executeUpdate();
                ps.close();
                ps = con.prepareStatement("INSERT INTO achievements(charid, achievementid, accountid) VALUES(?, ?, ?)");
                for (Integer achid : finishedAchievements) {
                    ps.setInt(1, id);
                    ps.setInt(2, achid);
                    ps.setInt(3, accountid);
                    ps.execute();
                }
                ps.close();
            }

            if (changed_reports) {
                deleteWhereCharacterId(con, "DELETE FROM reports WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO reports VALUES(DEFAULT, ?, ?, ?)");
                for (Entry<ReportType, Integer> achid : reports.entrySet()) {
                    ps.setInt(1, id);
                    ps.setByte(2, achid.getKey().i);
                    ps.setInt(3, achid.getValue());
                    ps.execute();
                }
                ps.close();
            }

            if (buddylist.changed()) {
                deleteWhereCharacterId(con, "DELETE FROM buddies WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO buddies (characterid, `buddyid`, `pending`, `groupname`) VALUES (?, ?, ?, ?)");
                ps.setInt(1, id);
                for (BuddylistEntry entry : buddylist.getBuddies()) {
                    ps.setInt(2, entry.getCharacterId());
                    ps.setInt(3, entry.isVisible() ? 0 : 1);
                    ps.setString(4, entry.getGroup());
                    ps.execute();
                }
                ps.close();
                buddylist.setChanged(false);
            }

            ps = con.prepareStatement("UPDATE accounts SET `ACash` = ?, `mPoints` = ?, `points` = ?, `vpoints` = ? WHERE id = ?");
            ps.setInt(1, acash);
            ps.setInt(2, maplepoints);
            ps.setInt(3, points);
            ps.setInt(4, vpoints);
            ps.setInt(5, client.getAccID());
            ps.executeUpdate();
            ps.close();

            if (storage != null) {
                storage.saveToDB();
            }
            if (cs != null) {
                cs.save(con);
            }
            // PlayerNPC.updateByCharId(this);
            keylayout.saveKeys(id, con);
            mount.saveMount(id, con);
            monsterbook.saveCards(accountid, con);

            deleteWhereCharacterId(con, "DELETE FROM imps WHERE characterid = ?");
            ps = con.prepareStatement("INSERT INTO imps (characterid, itemid, closeness, fullness, state, level) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setInt(1, id);
            for (int i = 0; i < imps.length; i++) {
                if (imps[i] != null) {
                    ps.setInt(2, imps[i].getItemId());
                    ps.setShort(3, imps[i].getCloseness());
                    ps.setShort(4, imps[i].getFullness());
                    ps.setByte(5, imps[i].getState());
                    ps.setByte(6, imps[i].getLevel());
                    ps.executeUpdate();
                }
            }
            ps.close();
            if (changed_wishlist) {
                deleteWhereCharacterId(con, "DELETE FROM wishlist WHERE characterid = ?");
                for (int i = 0; i < getWishlistSize(); i++) {
                    ps = con.prepareStatement("INSERT INTO wishlist(characterid, sn) VALUES(?, ?) ");
                    ps.setInt(1, getId());
                    ps.setInt(2, wishlist[i]);
                    ps.execute();
                    ps.close();
                }
            }
            if (changed_trocklocations) {
                deleteWhereCharacterId(con, "DELETE FROM trocklocations WHERE characterid = ?");
                for (int i = 0; i < rocks.length; i++) {
                    if (rocks[i] != 999999999) {
                        ps = con.prepareStatement("INSERT INTO trocklocations(characterid, mapid) VALUES(?, ?) ");
                        ps.setInt(1, getId());
                        ps.setInt(2, rocks[i]);
                        ps.execute();
                        ps.close();
                    }
                }
            }

            if (changed_regrocklocations) {
                deleteWhereCharacterId(con, "DELETE FROM regrocklocations WHERE characterid = ?");
                for (int i = 0; i < regrocks.length; i++) {
                    if (regrocks[i] != 999999999) {
                        ps = con.prepareStatement("INSERT INTO regrocklocations(characterid, mapid) VALUES(?, ?) ");
                        ps.setInt(1, getId());
                        ps.setInt(2, regrocks[i]);
                        ps.execute();
                        ps.close();
                    }
                }
            }
            if (changed_hyperrocklocations) {
                deleteWhereCharacterId(con, "DELETE FROM hyperrocklocations WHERE characterid = ?");
                for (int i = 0; i < hyperrocks.length; i++) {
                    if (hyperrocks[i] != 999999999) {
                        ps = con.prepareStatement("INSERT INTO hyperrocklocations(characterid, mapid) VALUES(?, ?) ");
                        ps.setInt(1, getId());
                        ps.setInt(2, hyperrocks[i]);
                        ps.execute();
                        ps.close();
                    }
                }
            }
            if (changed_extendedSlots) {
                deleteWhereCharacterId(con, "DELETE FROM extendedSlots WHERE characterid = ?");
                for (int i : extendedSlots) {
                    if (getInventory(MapleInventoryType.ETC).findById(i) != null) { //just in case
                        ps = con.prepareStatement("INSERT INTO extendedSlots(characterid, itemId) VALUES(?, ?) ");
                        ps.setInt(1, getId());
                        ps.setInt(2, i);
                        ps.execute();
                        ps.close();
                    }
                }
            }
            changed_wishlist = false;
            changed_trocklocations = false;
            changed_regrocklocations = false;
            changed_hyperrocklocations = false;
            changed_skillmacros = false;
            changed_savedlocations = false;
            changed_pokemon = false;
            changed_questinfo = false;
            changed_achievements = false;
            changed_extendedSlots = false;
            changed_skills = false;
            changed_reports = false;
            con.commit();
            saveToPC();//피시방 저장
        } catch (Exception e) {
            FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
            e.printStackTrace();
            System.err.println(MapleClient.getLogMessage(this, "[charsave] Error saving character data") + e);
            try {
                con.rollback();
            } catch (SQLException ex) {
                FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, ex);
                System.err.println(MapleClient.getLogMessage(this, "[charsave] Error Rolling Back") + e);
            }
        } finally {
            try {
                con.setAutoCommit(true);
                con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            } catch (SQLException e) {
                FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
                System.err.println(MapleClient.getLogMessage(this, "[charsave] Error going back to autocommit mode") + e);
            }
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }
            if (pse != null) {
                try {
                    pse.close();
                } catch (Exception e) {
                }
            }
        }
    }

    private void deleteWhereCharacterId(Connection con, String sql) throws SQLException {
        deleteWhereCharacterId(con, sql, id);
    }

    public static void deleteWhereCharacterId(Connection con, String sql, int id) throws SQLException {
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        ps.close();
    }

    public static void deleteWhereCharacterId_NoLock(Connection con, String sql, int id) throws SQLException {
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.execute();
        ps.close();
    }

    public void saveInventory(final Connection con) throws SQLException {
        List<Pair<Item, MapleInventoryType>> listing = new ArrayList<Pair<Item, MapleInventoryType>>();
        for (final MapleInventory iv : inventory) {
            for (final Item item : iv.list()) {
                listing.add(new Pair<Item, MapleInventoryType>(item, iv.getType()));
            }
        }
        if (con != null) {
            ItemLoader.INVENTORY.saveItems(listing, con, id);
        } else {
            ItemLoader.INVENTORY.saveItems(listing, id);
        }
    }

    public final PlayerStats getStat() {
        return stats;
    }

    public final void QuestInfoPacket(final tools.data.MaplePacketLittleEndianWriter mplew) {
        mplew.writeShort(questinfo.size());

        for (final Entry<Integer, String> q : questinfo.entrySet()) {
            mplew.writeShort(q.getKey());
            mplew.writeMapleAsciiString(q.getValue() == null ? "" : q.getValue());
        }
    }

    public final void updateInfoQuest(final int questid, final String data) {
        questinfo.put(questid, data);
        changed_questinfo = true;
        client.getSession().write(MaplePacketCreator.updateInfoQuest(questid, data));
    }

    public final String getInfoQuest(final int questid) {
        if (questinfo.containsKey(questid)) {
            return questinfo.get(questid);
        }
        return "";
    }

    public final int getNumQuest() {
        int i = 0;
        for (final MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == 2 && !(q.isCustom())) {
                i++;
            }
        }
        return i;
    }

    public final byte getQuestStatus(final int quest) {
        final MapleQuest qq = MapleQuest.getInstance(quest);
        if (getQuestNoAdd(qq) == null) {
            return 0;
        }
        return getQuestNoAdd(qq).getStatus();
    }

    public final MapleQuestStatus getQuest(final MapleQuest quest) {
        if (!quests.containsKey(quest)) {
            return new MapleQuestStatus(quest, (byte) 0);
        }
        return quests.get(quest);
    }

    public final void setQuestAdd(final MapleQuest quest, final byte status, final String customData) {
        if (!quests.containsKey(quest)) {
            final MapleQuestStatus stat = new MapleQuestStatus(quest, status);
            stat.setCustomData(customData);
            quests.put(quest, stat);
        }
    }

    public final MapleQuestStatus getQuestNAdd(final MapleQuest quest) {
        if (!quests.containsKey(quest)) {
            final MapleQuestStatus status = new MapleQuestStatus(quest, (byte) 0);
            quests.put(quest, status);
            return status;
        }
        return quests.get(quest);
    }

    public final MapleQuestStatus getQuestNoAdd(final MapleQuest quest) {
        return quests.get(quest);
    }

    public final MapleQuestStatus getQuestRemove(final MapleQuest quest) {
        return quests.remove(quest);
    }

    public final void updateQuest(final MapleQuestStatus quest) {
        updateQuest(quest, false);
    }

    public final void updateQuest(final MapleQuestStatus quest, final boolean update) {
        quests.put(quest.getQuest(), quest);
        if (!(quest.isCustom())) {
            client.getSession().write(MaplePacketCreator.updateQuest(quest));
            if (quest.getStatus() == 1 && !update) {
                client.getSession().write(MaplePacketCreator.updateQuestInfo(this, quest.getQuest().getId(), quest.getNpc(), (byte) 14)); //완료
            }
        }
    }

    public final Map<Integer, String> getInfoQuest_Map() {
        return questinfo;
    }

    public final Map<MapleQuest, MapleQuestStatus> getQuest_Map() {
        return quests;
    }

    public int getBuffLeaderID(MapleBuffStat buffStat) {
        final MapleBuffStatValueHolder mbsvh = effects.get(buffStat);
        return mbsvh == null ? -1 : mbsvh.cid;
    }

    public boolean getBuffedValue1(int skillid) {
        for (MapleBuffStatValueHolder mbsvh : effects.values()) {
            if (mbsvh.effect.getSourceId() == skillid) {
                return true;
            }
        }
        return false;
    }

    public Integer getBuffedValue(MapleBuffStat effect) {
        final MapleBuffStatValueHolder mbsvh = effects.get(effect);
        return mbsvh == null ? null : Integer.valueOf(mbsvh.value);
    }

    public final Integer getBuffedSkill_X(final MapleBuffStat effect) {
        final MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return null;
        }
        return mbsvh.effect.getX();
    }

    public final Integer getBuffedSkill_Y(final MapleBuffStat effect) {
        final MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return null;
        }
        return mbsvh.effect.getY();
    }

    public boolean isBuffFrom(MapleBuffStat stat, Skill skill) {
        final MapleBuffStatValueHolder mbsvh = effects.get(stat);
        if (mbsvh == null || mbsvh.effect == null) {
            return false;
        }
        return mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skill.getId();
    }

    public int getBuffSource(MapleBuffStat stat) {
        final MapleBuffStatValueHolder mbsvh = effects.get(stat);
        return mbsvh == null ? -1 : mbsvh.effect.getSourceId();
    }

    public int getTrueBuffSource(MapleBuffStat stat) {
        final MapleBuffStatValueHolder mbsvh = effects.get(stat);
        return mbsvh == null ? -1 : (mbsvh.effect.isSkill() ? mbsvh.effect.getSourceId() : -mbsvh.effect.getSourceId());
    }

    public int getItemQuantity(int itemid, boolean checkEquipped) {
        int possesed = inventory[GameConstants.getInventoryType(itemid).ordinal()].countById(itemid);
        if (checkEquipped) {
            possesed += inventory[MapleInventoryType.EQUIPPED.ordinal()].countById(itemid);
        }
        return possesed;
    }

    public void setBuffedValue(MapleBuffStat effect, int value) {
        final MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return;
        }
        mbsvh.value = value;
    }

    public void setSchedule(MapleBuffStat effect, ScheduledFuture<?> sched) {
        final MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return;
        }
        mbsvh.schedule.cancel(false);
        mbsvh.schedule = sched;
    }

    public Long getBuffedStarttime(MapleBuffStat effect) {
        final MapleBuffStatValueHolder mbsvh = effects.get(effect);
        return mbsvh == null ? null : Long.valueOf(mbsvh.startTime);
    }

    public MapleStatEffect getStatForBuff(MapleBuffStat effect) {
        final MapleBuffStatValueHolder mbsvh = effects.get(effect);
        return mbsvh == null ? null : mbsvh.effect;
    }

    public void doDragonBlood() {
        final MapleStatEffect bloodEffect = getStatForBuff(MapleBuffStat.DRAGONBLOOD);
        if (bloodEffect == null) {
            lastDragonBloodTime = 0;
            return;
        }
        prepareDragonBlood();
        if (stats.getHp() - bloodEffect.getX() <= 1) {
            cancelBuffStats(true, MapleBuffStat.DRAGONBLOOD);
            cancelBuffStats(true, MapleBuffStat.WATK);
        } else {
            addHP(-bloodEffect.getX());
            client.getSession().write(MaplePacketCreator.showOwnBuffEffect(bloodEffect.getSourceId(), 7, getLevel(), bloodEffect.getLevel()));
            map.broadcastMessage(MapleCharacter.this, MaplePacketCreator.showBuffeffect(getId(), bloodEffect.getSourceId(), 7, getLevel(), bloodEffect.getLevel()), false);
        }
    }

    public final boolean canBlood(long now) {
        return lastDragonBloodTime > 0 && lastDragonBloodTime + 4000 < now;
    }

    public void prepareDragonBlood() {
        lastDragonBloodTime = System.currentTimeMillis();
    }

    public void doRecovery() {
        MapleStatEffect bloodEffect = getStatForBuff(MapleBuffStat.RECOVERY);
        if (bloodEffect != null) {
            prepareRecovery();
            if (stats.getHp() >= stats.getCurrentMaxHp()) {
                cancelEffectFromBuffStat(MapleBuffStat.RECOVERY);
            } else {
                healHP(bloodEffect.getX());
            }
        }
    }

    public final boolean canRecover(long now) {
        return lastRecoveryTime > 0 && lastRecoveryTime + 5000 < now;
    }

    private void prepareRecovery() {
        lastRecoveryTime = System.currentTimeMillis();
    }

    public void startMapTimeLimitTask(int time, final MapleMap to) {
        if (time <= 0) { //jail
            time = 1;
        }
        client.getSession().write(MaplePacketCreator.getClock(time));
        final MapleMap ourMap = getMap();
        time *= 1000;
        mapTimeLimitTask = MapTimer.getInstance().register(new Runnable() {

            @Override
            public void run() {
                if (ourMap.getId() == GameConstants.JAIL) {
                    getQuestNAdd(MapleQuest.getInstance(GameConstants.JAIL_TIME)).setCustomData(String.valueOf(System.currentTimeMillis()));
                    getQuestNAdd(MapleQuest.getInstance(GameConstants.JAIL_QUEST)).setCustomData("0"); //release them!
                }
                changeMap(to, to.getPortal(0));
            }
        }, time, time);
    }

    public boolean canDOT(long now) {
        return lastDOTTime > 0 && lastDOTTime + 8000 < now;
    }

    public boolean hasDOT() {
        return dotHP > 0;
    }

    public void doDOT() {
        addHP(-(dotHP * 4));
        dotHP = 0;
        lastDOTTime = 0;
    }

    public void setDOT(int d, int source, int sourceLevel) {
        this.dotHP = d;
        addHP(-(dotHP * 4));
        map.broadcastMessage(MaplePacketCreator.getPVPMist(id, source, sourceLevel, d));
        lastDOTTime = System.currentTimeMillis();
    }

    public void startFishingTask() {
        cancelFishingTask();
        lastFishingTime = System.currentTimeMillis();
    }

    public boolean canFish(long now) {
        return lastFishingTime > 0 && lastFishingTime + GameConstants.getFishingTime(stats.canFishVIP, isGM()) < now;
    }

    public void doFish(long now) {
        lastFishingTime = now;
        //final boolean expMulti = haveItem(4031851, 1, false, true);
        if (client == null || client.getPlayer() == null || !client.isReceiving() || !GameConstants.isFishingMap(getMapId()) || !stats.canFish || chair <= 0) {
            cancelFishingTask();
            return;
        }
        //MapleInventoryManipulator.removeById(client, MapleInventoryType.USE, expMulti ? 4031851 : (GameConstants.GMS ? 2270008 : 2300000), 1, false, false);
        boolean passed = false;
        while (!passed) {
            int randval = RandomRewards.getFishingReward();
            switch (randval) {
                case 0: // Meso
                    //final int money = Randomizer.rand(expMulti ? 15 : 10, expMulti ? 75000 : 50000);
                    //gainMeso(money, true);
                    passed = true;
                    break;
                case 1: // EXP
                    final int experi = Math.min(Randomizer.nextInt(Math.abs(getNeededExp() / 200) + 1), 500000);
                    //gainExp(expMulti ? (experi * 3 / 2) : experi, true, false, true);
                    passed = true;
                    break;
                default:
                    if (MapleItemInformationProvider.getInstance().itemExists(randval)) {
                        MapleInventoryManipulator.addById(client, randval, (short) 1, "Fishing" + " on " + FileoutputUtil.CurrentReadable_Date());
                        passed = true;
                    }
                    break;
            }
        }

    }

    public void cancelMapTimeLimitTask() {
        if (mapTimeLimitTask != null) {
            mapTimeLimitTask.cancel(false);
            mapTimeLimitTask = null;
        }
    }

    public int getNeededExp() {
        return GameConstants.getExpNeededForLevel(level);
    }

    public void cancelFishingTask() {
        lastFishingTime = 0;
    }

    public void registerEffect(MapleStatEffect effect, long starttime, ScheduledFuture<?> schedule, int from) {
        registerEffect(effect, starttime, schedule, effect.getStatups(), false, effect.getDuration(), from);
    }

    public void registerEffect(MapleStatEffect effect, long starttime, ScheduledFuture<?> schedule, Map<MapleBuffStat, Integer> statups, boolean silent, final int localDuration, final int cid) {
        if (effect.isHide()) {
            map.broadcastMessage(this, MaplePacketCreator.removePlayerFromMap(getId()), false);
        } else if (effect.isDragonBlood()) {
            prepareDragonBlood();
        } else if (effect.isRecovery()) {
            prepareRecovery();
        } else if (effect.isBerserk()) {
            checkBerserk();
        } else if (effect.isMonsterRiding_()) {
            getMount().startSchedule();
        }
//        int modifiedDuration = localDuration;
//        if (effect.getSourceId() == 35111004 || effect.getSourceId() == 35121013) {
//            modifiedDuration = Integer.MAX_VALUE;
//        }
        for (Entry<MapleBuffStat, Integer> statup : statups.entrySet()) {
            int value = statup.getValue().intValue();
            if (statup.getKey() == MapleBuffStat.MONSTER_RIDING) {
                if (effect.getSourceId() == 5221006 && battleshipHP <= 0) {
                    battleshipHP = maxBattleshipHP(effect.getSourceId()); //copy this as well
                }
            }
            effects.put(statup.getKey(), new MapleBuffStatValueHolder(effect, starttime, schedule, value, localDuration, cid));
//            effects.put(statup.getKey(), new MapleBuffStatValueHolder(effect, starttime, schedule, value, modifiedDuration, cid));
        }
        switch (effect.getSourceId()) {
            //특정 아이템 스킬만
            case 2450018:
            case 9001008: {
                BuffHandler.get().addBuff(this.id, effect.getSourceId(), starttime + localDuration);
//                BuffHandler.get().addBuff(this.id, effect.getSourceId(), starttime + modifiedDuration);
                break;
            }
        }

        if (!silent) {
            stats.recalcLocalStats(this);
        }
    }

    public List<MapleBuffStat> getBuffStats(final MapleStatEffect effect, final long startTime) {
        final List<MapleBuffStat> bstats = new ArrayList<MapleBuffStat>();
        final Map<MapleBuffStat, MapleBuffStatValueHolder> allBuffs = new EnumMap<MapleBuffStat, MapleBuffStatValueHolder>(effects);
        for (Entry<MapleBuffStat, MapleBuffStatValueHolder> stateffect : allBuffs.entrySet()) {
            final MapleBuffStatValueHolder mbsvh = stateffect.getValue();
            if (mbsvh.effect.sameSource(effect) && (startTime == -1 || startTime == mbsvh.startTime)) {
                bstats.add(stateffect.getKey());
            }
        }
        return bstats;
    }

    private boolean deregisterBuffStats(List<MapleBuffStat> stats) { // 수정
        boolean clonez = false;

        List<MapleBuffStatValueHolder> effectsToCancel = new ArrayList<MapleBuffStatValueHolder>(stats.size());
        for (MapleBuffStat stat : stats) {
            final MapleBuffStatValueHolder mbsvh = effects.remove(stat);
            if (mbsvh != null) {
                boolean addMbsvh = true;
                for (MapleBuffStatValueHolder contained : effectsToCancel) {
                    if (mbsvh.startTime == contained.startTime && contained.effect == mbsvh.effect) {
                        addMbsvh = false;
                    }
                }
                if (addMbsvh) {
                    effectsToCancel.add(mbsvh);
                }
                if (stat == MapleBuffStat.SUMMON || stat == MapleBuffStat.PUPPET || stat == MapleBuffStat.donno4) { //donno4 = 새틀라이트
                    final int summonId = mbsvh.effect.getSourceId();
                    final List<MapleSummon> toRemove = new ArrayList<MapleSummon>();
                    visibleMapObjectsLock.writeLock().lock(); //We need to lock this later on anyway so do it now to prevent deadlocks.
                    summonsLock.writeLock().lock();
                    try {
                        for (MapleSummon summon : summons) {
                            if (summon.getSkill() == summonId) { //removes bots n tots
                                map.broadcastMessage(MaplePacketCreator.removeSummon(summon, true));
                                map.removeMapObject(summon);
                                visibleMapObjects.remove(summon);
                                toRemove.add(summon);
                            }
                        }
                        for (MapleSummon s : toRemove) {
                            summons.remove(s);
                        }
                    } finally {
                        summonsLock.writeLock().unlock();
                        visibleMapObjectsLock.writeLock().unlock(); //lolwut
                    }
                } else if (stat == MapleBuffStat.DRAGONBLOOD) {
                    lastDragonBloodTime = 0;
                } else if (stat == MapleBuffStat.RECOVERY) {
                    lastRecoveryTime = 0;
                }
            }
        }
        for (MapleBuffStatValueHolder cancelEffectCancelTasks : effectsToCancel) {
            if (getBuffStats(cancelEffectCancelTasks.effect, cancelEffectCancelTasks.startTime).size() == 0) {
                if (cancelEffectCancelTasks.schedule != null) {
                    cancelEffectCancelTasks.schedule.cancel(false);
                }
            }
        }
        return clonez;
    }

    /**
     * @param effect
     * @param overwrite when overwrite is set no data is sent and all the
     * Buffstats in the StatEffect are deregistered
     * @param startTime
     */
    public void cancelEffect(final MapleStatEffect effect, final long startTime) {
        if (effect == null) {
            return;
        }
        cancelEffect(effect, startTime, effect.getStatups(), true);
    }

    public void cancelEffect(final MapleStatEffect effect, final long startTime, Map<MapleBuffStat, Integer> statups, boolean recalcStat) {
//        List<MapleBuffStat> buffstats = new ArrayList<MapleBuffStat>(statups.keySet());
//
//        if (buffstats.size() <= 0) {
//            return;
//        }
        //       try { // 내기억엔 이렇게 과격한 소스는 없는데 
        //           //버프캔슬
//            for (MapleBuffStat stat : statups.keySet()) {
//                if (getBuffSource(stat) != effect.getSourceId()) {
        //                   if (stat == MapleBuffStat.PUPPET || stat == MapleBuffStat.SUMMON || stat == MapleBuffStat.DASH_SPEED
        //                           || stat == MapleBuffStat.DASH_JUMP || stat == MapleBuffStat.EXPRATE || stat == MapleBuffStat.DROP_RATE || stat == MapleBuffStat.GHOST_MORPH) {
        //                       continue;
        //                   }
        //                   //System.out.println("버프 캔슬 막음 : " + stat);
        //                   buffstats.remove(stat);
        //               }
        //           }
        //       } catch (Exception e) {
        //           System.err.println("buff err");
        //       }
        if (effect == null) {
            return;
        }

        int boosterBuff = 0;
        long realStartTime = startTime;
        for (int boosterItemID : GameConstants.boosterBuffItemID) {
            if (effect.getSourceId() == boosterItemID) {  //부스터 아이템 코드
                boosterBuff = boosterItemID;
                if (realStartTime == -1) {
                    return;
                } else {
                    realStartTime = -1;
                    break;
                }
            }
        }
        List<MapleBuffStat> buffstats;

//        if (effect.getSourceId() == 35121003) { //강제로 타이탄 캔슬 막아서 작동되게하는건데 우리가 지금 커스텀 스탯이 타이탄 그거라서 
//            return;
//        }
        switch (effect.getSourceId()) {
            case 2450018:
            case 2022694:
            case 2022332:
            case 2000000:
                getClient().getSession().write(FamilyPacket.cancelFamilyBuff());
                break;
        }

//        if (this.MATKValue.containsKey(effect.getSourceId())) {
//            this.MATKValue.remove(effect.getSourceId());
//            // this.MATKValue.clear();
//            //  this.dropMessage(6, effect.getSourceId() + " 해당 스킬의 마력 값 삭제");
//        }
//        if (this.WATKValue.containsKey(effect.getSourceId())) {
//            this.WATKValue.remove(effect.getSourceId());
//            // this.WATKValue.clear();
//            //  this.dropMessage(6, effect.getSourceId() + " 해당 스킬의 공격 값 삭제");
//        }
//
//        boolean boosterUpdated = false;
//        if (this.boosterVal.containsKey(effect.getSourceId())) {
//            this.boosterVal.remove(effect.getSourceId());
//            boosterUpdated = true;
//        }
        if (effect.getSourceId() == 35111004) {
            SkillFactory.getSkill(35001002).getEffect(getTotalSkillLevel(35001002)).applyTo(this);
        }
//        if (effect.getSourceId() == 32111005) {
//            cancelEffectFromBuffStat(MapleBuffStat.BLUE_AURA, getId());
//            cancelEffectFromBuffStat(MapleBuffStat.YELLOW_AURA, getId());
//            cancelEffectFromBuffStat(MapleBuffStat.DARK_AURA, getId());
//            cancelEffectFromBuffStat(MapleBuffStat.AURA, getId());
//        }
//        

        if (recalcStat) {
            buffstats = getBuffStats(effect, realStartTime);
        } else {
            buffstats = new ArrayList<>(statups.keySet());
        }

        if (effect.isMagicDoor()) {
            // remove for all on maps
            if (!getDoors().isEmpty()) {
                removeDoor();
                silentPartyUpdate();
            }
        } else if (effect.isMechDoor()) {
            if (!getMechDoors().isEmpty()) {
                removeMechDoor();
            }
        } else if (effect.isMonsterRiding_()) {
            getMount().cancelSchedule();
        } else if (effect.isMonsterRiding()) {
            cancelEffectFromBuffStat(MapleBuffStat.MECH_CHANGE);
            if (effect.getSourceId() == 35120000 || effect.getSourceId() == 35001002) {
                cancelBuffStats(true, MapleBuffStat.EMHP);
                cancelBuffStats(true, MapleBuffStat.EMMP);
                cancelBuffStats(true, MapleBuffStat.EPAD);
                cancelBuffStats(true, MapleBuffStat.EPDD);
                cancelBuffStats(true, MapleBuffStat.EMDD);
                cancelBuffStats(true, MapleBuffStat.MONSTER_RIDING);
            }
        } else if (effect.isAranCombo()) {
            combo = 0;
        }
        deregisterBuffStats(buffstats);
        // check if we are still logged in o.o
        if (effect.getSourceId() != 35121003) {
            cancelPlayerBuffs(buffstats, recalcStat);
        }

        if (effect.isHide() && client.getChannelServer().getPlayerStorage().getCharacterById(this.getId()) != null) { //Wow this is so fking hacky...
            map.broadcastMessage(this, MaplePacketCreator.spawnPlayerMapobject(this), false);

            sendTemporaryStats();
            for (final MaplePet pet : pets) {
                if (pet.getSummoned()) {
                    map.broadcastMessage(this, PetPacket.showPet(this, pet, false, false), false);
                }
            }
        }

        if (effect.getSourceId() == 35121013) { //when siege 2 deactivates, missile re-activates
            SkillFactory.getSkill(35121005).getEffect(getTotalSkillLevel(35121005)).applyTo(this);
        }

        if (boosterBuff != 0 && startTime >= 0) {
            MapleStatEffect itemEff = MapleItemInformationProvider.getInstance().getItemEffect(boosterBuff);
            if (itemEff != null && !this.getBuffedValue1(boosterBuff)) {
                itemEff.applyTo(this);
            }
        }

        boolean customizeUpdate = false;
        MapleBuffStat[] customizeCTS = {MapleBuffStat.WATK, MapleBuffStat.MATK, MapleBuffStat.BOOSTER};
        for (MapleBuffStat cts : customizeCTS) {
            if (buffstats.contains(cts)) {
                setCTS(cts, effect.getSourceId(), 0, true);

                if (!customizeUpdate) {
                    customizeUpdate = true;
                }
            }
        }

        if (!customizeUpdate) {
            if (boosterBuff != 0 && buffstats.contains(MapleBuffStat.PYRAMID_PQ)) {
                setCTS(MapleBuffStat.BOOSTER, effect.getSourceId(), 0, true);

                if (!customizeUpdate) {
                    customizeUpdate = true;
                }
            }
        }

        BuffHandler.get().removeBuff(this.id, effect.getSourceId());

//        if (customizeUpdate) {
//            customizeStat(System.currentTimeMillis());
//        }
//        if (boosterUpdated) {
//            if (!GameConstants.boosterBuffItemID.contains(effect.getSourceId()) && this.boosterVal.size() > 0) {
//                this.CustomStatEffect(false);
//            }
//        }
    }

    public void openNpc(int id) {
        NPCScriptManager.getInstance().start(getClient(), id);
    }

    public void cancelBuffStats(boolean recalcStat, MapleBuffStat... stat) {
        List<MapleBuffStat> buffStatList = Arrays.asList(stat);
        deregisterBuffStats(buffStatList);
        cancelPlayerBuffs(buffStatList, recalcStat);
    }

    public void cancelEffectFromBuffStat(MapleBuffStat stat) {
        if (effects.get(stat) != null) {
            cancelEffect(effects.get(stat).effect, -1);
        }
    }

    public void cancelEffectFromBuffStat(MapleBuffStat stat, int from) {
        if (effects.get(stat) != null && effects.get(stat).cid == from) {
            cancelEffect(effects.get(stat).effect, -1);
        }
    }

    public void cancelEffectFromSkillID(int skillID) {
        for (MapleBuffStatValueHolder mbsvh : effects.values()) {
            if (mbsvh.effect.getSourceId() == skillID) {
                cancelEffect(mbsvh.effect, -2);
                break;
            }
        }
    }

    public long getStartTimeFromSkillID(int skillID) {
        for (MapleBuffStatValueHolder mbsvh : effects.values()) {
            if (mbsvh.effect.getSourceId() == skillID) {
                return mbsvh.startTime;
            }
        }
        return 0;
    }

    private void cancelPlayerBuffs(List<MapleBuffStat> buffstats, boolean recalcStat) {

        boolean write = client != null && client.getChannelServer() != null && client.getChannelServer().getPlayerStorage().getCharacterById(getId()) != null;
        if (write && recalcStat) {
            stats.recalcLocalStats(this);
        }

        client.getSession().write(TemporaryStatsPacket.cancelBuff(buffstats));
        map.broadcastMessage(this, TemporaryStatsPacket.cancelForeignBuff(getId(), buffstats), false);
    }

    public int getArcaneExp() {
        int exp = 0;
        for (int[] a : World.ArcaneSimbol.getItems().values()) {
            if (haveItem(a[0], 1)) {
                exp += a[7];
            }
        }
        return exp;
    }

    public int getArcaneMeso() {
        int meso = 0;
        for (int[] a : World.ArcaneSimbol.getItems().values()) {
            if (haveItem(a[0], 1)) {
                meso += a[8];
            }
        }
        return meso;
    }

    public int getCollectionExp() {
        int exp = 0;
        if (getQuestNAdd(MapleQuest.getInstance(221018)).getCustomData() == null) { //보스컬 100%
            getQuestNAdd(MapleQuest.getInstance(221018)).setCustomData("0");
        }
        if (Integer.parseInt(getQuestNAdd(MapleQuest.getInstance(221018)).getCustomData()) != 0) {
            exp += 6;
        }
        return exp;
    }

    public int getCollectionMeso() {
        int meso = 0;
        if (getQuestNAdd(MapleQuest.getInstance(221019)).getCustomData() == null) { //재료2컬100%
            getQuestNAdd(MapleQuest.getInstance(221019)).setCustomData("0");
        }
        if (Integer.parseInt(getQuestNAdd(MapleQuest.getInstance(221019)).getCustomData()) != 0) {
            meso += 6;
        }
        return meso;
    }

    public void dispel() {
        if (!isHidden()) {
            final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());
            for (MapleBuffStatValueHolder mbsvh : allBuffs) { // && !mbsvh.effect.isMechChange() 
                if (mbsvh.effect.isSkill() && mbsvh.schedule != null && !mbsvh.effect.isMorph() && !mbsvh.effect.isGmBuff() && !mbsvh.effect.isMonsterRiding() && !mbsvh.effect.isEnergyCharge() && !mbsvh.effect.isAranCombo()) {
                    cancelEffect(mbsvh.effect, mbsvh.startTime);
                }
            }
        }
    }

    public void dispelSkill(int skillid) {
        final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());

        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skillid) {
                cancelEffect(mbsvh.effect, mbsvh.startTime);
                break;
            }
        }
    }

    public void dispelSummons() {
        final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());

        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.getSummonMovementType() != null) {
                cancelEffect(mbsvh.effect, mbsvh.startTime);
            }
        }
    }

    public void dispelBuff(int skillid) {
        final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());

        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.getSourceId() == skillid) {
                cancelEffect(mbsvh.effect, mbsvh.startTime);
                break;
            }
        }
    }

    public void cancelAllBuffs_() {
        effects.clear();
    }

    public void cancelAllBuffs() {
        final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());

        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.getSourceId() != 22181003) {
                cancelEffect(mbsvh.effect, mbsvh.startTime);
            }
        }
    }

    public void cancelMorphs() {
        final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());

        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            switch (mbsvh.effect.getSourceId()) {
                case 5111005:
                case 5121003:
                case 15111002:
                case 13111005:
                    return; // Since we can't have more than 1, save up on loops
                default:
                    if (mbsvh.effect.isMorph()) {
                        cancelEffect(mbsvh.effect, mbsvh.startTime);
                        continue;
                    }
            }
        }
    }

    public int getMorphState() {
        LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.isMorph()) {
                return mbsvh.effect.getSourceId();
            }
        }
        return -1;
    }

    public void silentGiveBuffs(List<PlayerBuffValueHolder> buffs) {
        if (buffs == null) {
            return;
        }
        for (PlayerBuffValueHolder mbsvh : buffs) {
            if (System.currentTimeMillis() - mbsvh.startTime > mbsvh.localDuration) {
                // 버프 시간이 지나면 강제로 0.3초로 조정
                mbsvh.effect.silentApplyBuff(this, System.currentTimeMillis(), 300, mbsvh.statup, mbsvh.cid);
                continue;
            }
            mbsvh.effect.silentApplyBuff(this, mbsvh.startTime, mbsvh.localDuration, mbsvh.statup, mbsvh.cid);
        }
    }

    public List<PlayerBuffValueHolder> getAllBuffs() {
        final List<PlayerBuffValueHolder> ret = new ArrayList<PlayerBuffValueHolder>();
        final Map<Pair<Integer, Byte>, Integer> alreadyDone = new HashMap<Pair<Integer, Byte>, Integer>();
        final LinkedList<Entry<MapleBuffStat, MapleBuffStatValueHolder>> allBuffs = new LinkedList<Entry<MapleBuffStat, MapleBuffStatValueHolder>>(effects.entrySet());
        for (Entry<MapleBuffStat, MapleBuffStatValueHolder> mbsvh : allBuffs) {
            final Pair<Integer, Byte> key = new Pair<Integer, Byte>(mbsvh.getValue().effect.getSourceId(), mbsvh.getValue().effect.getLevel());
            if (alreadyDone.containsKey(key)) {
                ret.get(alreadyDone.get(key)).statup.put(mbsvh.getKey(), mbsvh.getValue().value);
            } else {
                alreadyDone.put(key, ret.size());
                final EnumMap<MapleBuffStat, Integer> list = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                list.put(mbsvh.getKey(), mbsvh.getValue().value);
                ret.add(new PlayerBuffValueHolder(mbsvh.getValue().startTime, mbsvh.getValue().effect, list, mbsvh.getValue().localDuration, mbsvh.getValue().cid));
            }
        }
        return ret;
    }

    public void cancelMagicDoor() {
        final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());

        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.isMagicDoor()) {
                cancelEffect(mbsvh.effect, mbsvh.startTime);
                break;
            }
        }
    }

    public int getSkillLevel(int skillid) {
        return getSkillLevel(SkillFactory.getSkill(skillid));
    }

    public int getTotalSkillLevel(int skillid) {
        return getTotalSkillLevel(SkillFactory.getSkill(skillid));
    }

    public long getExtraExp() {
        return extraexp;
    }

    public void setExtraExp(long i) {
        this.extraexp = i;
    }

    public void addExtraExp(long i) {
        this.extraexp += i;
    }

    public final void handleEnergyCharge(final int skillid, final int targets) {
        final Skill echskill = SkillFactory.getSkill(skillid);
        final int skilllevel = getTotalSkillLevel(echskill);
        if (skilllevel > 0) {
            final MapleStatEffect echeff = echskill.getEffect(skilllevel);
            if (targets > 0) {
                if (getBuffedValue(MapleBuffStat.ENERGY_CHARGE) == null) {
                    echeff.applyEnergyBuff(this, true); // Infinity time
                } else {
                    Integer energyLevel = getBuffedValue(MapleBuffStat.ENERGY_CHARGE);
                    //TODO: bar going down
                    if (energyLevel < 10000) {
                        energyLevel += (echeff.getX() * targets);

                        client.getSession().write(MaplePacketCreator.showOwnBuffEffect(skillid, 2, getLevel(), skilllevel));
                        map.broadcastMessage(this, MaplePacketCreator.showBuffeffect(id, skillid, 2, getLevel(), skilllevel), false);

                        if (energyLevel >= 10000) {
                            energyLevel = 10000;
                        }
                        client.getSession().write(MaplePacketCreator.giveEnergyChargeTest(energyLevel, echeff.getDuration() / 1000));
                        setBuffedValue(MapleBuffStat.ENERGY_CHARGE, Integer.valueOf(energyLevel));
                    } else if (energyLevel == 10000) {
                        echeff.applyEnergyBuff(this, false); // One with time
                        setBuffedValue(MapleBuffStat.ENERGY_CHARGE, Integer.valueOf(10001));
                    }
                }
            }
        }
    }

    public final void handleBattleshipHP(int damage) {
        if (damage < 0) {
            final MapleStatEffect effect = getStatForBuff(MapleBuffStat.MONSTER_RIDING);
            if (effect != null && effect.getSourceId() == 5221006) {
                battleshipHP += damage;
                client.getSession().write(MaplePacketCreator.skillCooldown(5221999, battleshipHP / 10));
                if (battleshipHP <= 0) {
                    battleshipHP = 0;
                    client.getSession().write(MaplePacketCreator.skillCooldown(5221006, effect.getCooldown()));
                    addCooldown(5221006, System.currentTimeMillis(), effect.getCooldown() * 1000);
                    cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
                }
            }
        }
    }

    public final void handleOrbgain() {
        int orbcount = getBuffedValue(MapleBuffStat.COMBO);
        MapleStatEffect curEffect = this.getStatForBuff(MapleBuffStat.COMBO);

        if (curEffect == null) {
            return;
        }

        Skill combo;
        Skill advcombo;

        switch (getJob()) {
            case 1110:
            case 1111:
            case 1112:
                combo = SkillFactory.getSkill(11111001);
                advcombo = SkillFactory.getSkill(11110005);
                break;
            default:
                combo = SkillFactory.getSkill(1111002);
                advcombo = SkillFactory.getSkill(1120003);
                break;
        }

        MapleStatEffect ceffect = null;
        int advComboSkillLevel = getTotalSkillLevel(advcombo);
        if (advComboSkillLevel > 0) {
            ceffect = advcombo.getEffect(advComboSkillLevel);
        } else if (getSkillLevel(combo) > 0) {
            ceffect = combo.getEffect(getTotalSkillLevel(combo));
        } else {
            return;
        }

        if (orbcount < ceffect.getX() + 1) {
            int neworbcount = orbcount + 1;
            if (advComboSkillLevel > 0 && ceffect.makeChanceResult()) {
                if (neworbcount < ceffect.getX() + 1) {
                    neworbcount++;
                }
            }
            EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
            stat.put(MapleBuffStat.COMBO, neworbcount);
            setBuffedValue(MapleBuffStat.COMBO, neworbcount);
            int duration = curEffect.getDuration(); //콤보어택 펫 버프 이상했던 점, 어드밴스드 콤보의 delay 를 받아와서 음수 값이 나왔던 것
            duration += (int) ((getBuffedStarttime(MapleBuffStat.COMBO) - System.currentTimeMillis()));
            client.getSession().write(MaplePacketCreator.giveBuff(combo.getId(), duration, stat, ceffect));
            map.broadcastMessage(this, MaplePacketCreator.giveForeignBuff(this, stat, ceffect), false);
        }
    }

    public String getQuestInfo(int key) {
        MapleQuestStatus quest = getQuestNoAdd(MapleQuest.getInstance(key));
        if (quest == null) {
            return "";
        }
        String data = quest.getCustomData();
        if (data == null) {
            return "";
        }
        return data;
    }

    public void setQuestInfo(int key, String value) {
        getQuestNAdd(MapleQuest.getInstance(key)).setCustomData(value);
    }

    public long remainingCooltime(int key, long time) {
        long ctm = System.currentTimeMillis();
        String info = getQuestInfo(key);
        long infol;
        try {
            infol = Long.parseLong(info);
        } catch (Exception ex) {
            infol = ctm;
        }
        if (infol > ctm) {
            return infol - ctm;
        }
        if (time != 0) {
            setQuestInfo(key, Long.toString(ctm + time));
        }
        return 0;
    }

    public void handleOrbconsume(int howmany) {
        Skill combo;

        switch (getJob()) {
            case 1110:
            case 1111:
            case 1112:
                combo = SkillFactory.getSkill(11111001);
                break;
            default:
                combo = SkillFactory.getSkill(1111002);
                break;
        }
        if (getSkillLevel(combo) <= 0) {
            return;
        }
        MapleStatEffect ceffect = getStatForBuff(MapleBuffStat.COMBO);
        if (ceffect == null) {
            return;
        }
        EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
        stat.put(MapleBuffStat.COMBO, Math.max(1, getBuffedValue(MapleBuffStat.COMBO) - howmany));
        setBuffedValue(MapleBuffStat.COMBO, Math.max(1, getBuffedValue(MapleBuffStat.COMBO) - howmany));
        int duration = ceffect.getDuration();
        duration += (int) ((getBuffedStarttime(MapleBuffStat.COMBO) - System.currentTimeMillis()));

        client.getSession().write(MaplePacketCreator.giveBuff(combo.getId(), duration, stat, ceffect));
        map.broadcastMessage(this, MaplePacketCreator.giveForeignBuff(this, stat, ceffect), false);
    }

    public void silentEnforceMaxHpMp() {
        stats.setMp(stats.getMp(), this);
        stats.setHp(stats.getHp(), true, this);

    }

    public void enforceMaxHpMp() {
        Map<MapleStat, Integer> statups = new EnumMap<MapleStat, Integer>(MapleStat.class);
        if (stats.getMp() > stats.getCurrentMaxMp()) {
            stats.setMp(stats.getMp(), this);
            statups.put(MapleStat.MP, Integer.valueOf(stats.getMp()));
        }
        if (stats.getHp() > stats.getCurrentMaxHp()) {
            stats.setHp(stats.getHp(), this);
            statups.put(MapleStat.HP, Integer.valueOf(stats.getHp()));
        }
        if (statups.size() > 0) {
            client.getSession().write(MaplePacketCreator.updatePlayerStats(statups, getJob()));
        }

    }

    public MapleMap getMap() {
        return map;
    }

    public MonsterBook getMonsterBook() {
        return monsterbook;
    }

    public void setMap(MapleMap newmap) {
        this.map = newmap;
    }

    public void setMap(int PmapId) {
        this.mapid = PmapId;
    }

    public int getMapId() {
        if (map != null) {
            return map.getId();
        }
        return mapid;
    }

    public byte getInitialSpawnpoint() {
        return initialSpawnPoint;
    }

    public byte getSpawnpoint() {
        return spawnPoint;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public final String getBlessOfFairyOrigin() {
        return this.BlessOfFairy_Origin;
    }

    public final String getBlessOfEmpressOrigin() {
        return this.BlessOfEmpress_Origin;
    }

    public final short getLevel() {
        return level;
    }

    public final int getFame() {
        return fame;
    }

    public final int getFallCounter() {
        return fallcounter;
    }

    public final MapleClient getClient() {
        return client;
    }

    public final void setClient(final MapleClient client) {
        this.client = client;
    }

    public int getExp() {
        return exp;
    }

    public short getRemainingAp() {
        return remainingAp;
    }

    public int getRemainingSp() {
        return remainingSp[GameConstants.getSkillBook(job)]; //default
    }

    public int getRemainingSp(final int skillbook) {
        return remainingSp[skillbook];
    }

    public int[] getRemainingSps() {
        return remainingSp;
    }

    public int getRemainingSpSize() {
        int ret = 0;
        for (int i = 0; i < remainingSp.length; i++) {
            if (remainingSp[i] > 0) {
                ret++;
            }
        }
        return ret;
    }

    public short getHpApUsed() {
        return hpApUsed;
    }

    public void setHidden(boolean f) {
        hide = f;
        client.getSession().write(MaplePacketCreator.GmHide(hide));
        if (hide) {
            map.broadcastMessage(this, MaplePacketCreator.removePlayerFromMap(getId()), false);
        } else {
            map.broadcastMessage(this, MaplePacketCreator.spawnPlayerMapobject(this), false);
            sendTemporaryStats();

            for (final MaplePet pet : pets) {
                if (pet.getSummoned()) {
                    map.broadcastMessage(this, PetPacket.showPet(this, pet, false, false), false);
                }
            }
        }
    }

    //boolean hide = false;
    public boolean isHidden() {
        return hide;
//        return getBuffSource(MapleBuffStat.DARKSIGHT) / 1000000 == 9;
    }

    private void sendTemporaryStats() {
        final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
        MapleStatEffect eff = getStatForBuff(MapleBuffStat.MONSTER_RIDING);
        if (eff != null) {
            map.broadcastMessage(this, TemporaryStatsPacket.giveForeignMount(id, MapleStatEffect.parseMountInfo_Pure(this, eff.getSourceId()), eff.getSourceId(), stat), false);
        }
        Integer val = getBuffedValue(MapleBuffStat.ENERGY_CHARGE);
        if (val != null) {
            map.broadcastMessage(this, TemporaryStatsPacket.giveEnergyChargeTest(id, 10000, 50), false);
        }
        eff = getStatForBuff(MapleBuffStat.DASH_SPEED);
        if (eff != null) {
            map.broadcastMessage(this, TemporaryStatsPacket.giveForeignPirate(Collections.singletonMap(MapleBuffStat.DASH_SPEED, getBuffedValue(MapleBuffStat.DASH_SPEED)), eff.getDuration() / 1000, id, eff.getSourceId()), false);
        }
        /*eff = getStatForBuff(MapleBuffStat.AURA);
         if (eff != null) {
         map.broadcastMessage(this, TemporaryStatsPacket.giveForeignBuff(getId(), Collections.singletonMap(MapleBuffStat.DARK_AURA, getBuffedValue(MapleBuffStat.DARK_AURA)), eff), false);
         }*/
    }

    public void setHpApUsed(short hpApUsed) {
        this.hpApUsed = hpApUsed;
    }

    public byte getSkinColor() {
        return skinColor;
    }

    public void setSkinColor(byte skinColor) {
        this.skinColor = skinColor;
    }

    public short getJob() {
        return job;
    }

    public byte getGender() {
        return gender;
    }

    public int getHair() {
        return hair;
    }

    public int getFace() {
        return face;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setExp(int exp) {
        this.exp = exp;
    }

    public void setHair(int hair) {
        this.hair = hair;
        if (getQuestStatus(29020) == 1) {
            MapleQuestStatus quest = getQuestNoAdd(MapleQuest.getInstance(29020));
            if (quest != null) { // 버라이어티 훈장
                int value = 0;
                if (quest.getCustomData() == null) {
                    MapleQuest.getInstance(29020).forceStart(this, 0, String.valueOf(1 + value));
                    return;
                }
                try {
                    value = Integer.parseInt(quest.getCustomData());
                } catch (Exception e) {
                }
                MapleQuest.getInstance(29020).forceStart(this, 0, String.valueOf(1 + value));
            }
        }
    }

    public void setFace(int face) {
        this.face = face;
    }

    public void setFame(int fame) {
        this.fame = fame;
    }

    public void setFallCounter(int fallcounter) {
        this.fallcounter = fallcounter;
    }

    public Point getOldPosition() {
        return old;
    }

    public void setOldPosition(Point x) {
        this.old = x;
    }

    public void setRemainingAp(short remainingAp) {
        this.remainingAp = remainingAp;
    }

    public void setRemainingSp(int remainingSp) {
        this.remainingSp[GameConstants.getSkillBook(job)] = remainingSp; //default
    }

    public void setRemainingSp(int remainingSp, final int skillbook) {
        this.remainingSp[skillbook] = remainingSp;
    }

    public void setGender(byte gender) {
        this.gender = gender;
    }

    public void setInvincible(boolean invinc) {
        invincible = invinc;
    }

    public void startMacro(int s) {
        macrotimer = MapTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                MapleCharacter mc = client.getPlayer();
                //mc.saveLocation(SavedLocationType.MULUNG_TC);
                mc.saveLocation(SavedLocationType.MULUNG_TC, mc.getMap().getReturnMap().getId());
                mc.changeMap(910000000);
                mc.dropMessage(1, "현재 매크로 방지 문자를 입력하지 않으셨습니다. 매크로 방지 숫자 입력 실패 5회누적 시 3일 정지 처분이됩니다.");
                mc.setMacroStr("");
                cancelMacro();
            }
        }, s * 1000L);
    }

    public void cancelMacro() {
        macrotimer.cancel(false);
        macrotimer = null;
    }

    public String getMacroStr() {
        return this.macrostr;
    }

    public void setMacroStr(String a) {
        macrostr = a;
    }

    public boolean isInvincible() {
        return invincible;
    }

    public CheatTracker getCheatTracker() {
        return anticheat;
    }

    public BuddyList getBuddylist() {
        return buddylist;
    }

    public void addFame(int famechange) {
        this.fame += famechange;
        if (this.fame >= 10) {
            //finishAchievement(7);
        }
        if (this.fame >= 100) {
            //finishAchievement(8);
        }
    }

    public void updateFame() {
        updateSingleStat(MapleStat.FAME, this.fame);
    }

    public void changeMapBanish(final int mapid, final String portal, final String msg) {
        dropMessage(5, msg);
        final MapleMap map = client.getChannelServer().getMapFactory().getMap(mapid);
        changeMap(map, map.getPortal(portal));
    }

    public void changeMap(final MapleMap to, final Point pos) {
        changeMapInternal(to, pos, MaplePacketCreator.getWarpToMap(to, 0x80, this), null);
    }

    public void changeMap(final MapleMap to) {
        changeMapInternal(to, to.getPortal(0).getPosition(), MaplePacketCreator.getWarpToMap(to, 0, this), to.getPortal(0));
    }

    public void changeMap(final int mapid) {
        final MapleMap map = client.getChannelServer().getMapFactory().getMap(mapid);
        changeMap(map, map.getPortal(0));
    }

    public void changeMap(final MapleMap to, final MaplePortal pto) {
        changeMapInternal(to, pto.getPosition(), MaplePacketCreator.getWarpToMap(to, pto.getId(), this), null);
    }

    public void changeMapPortal(final MapleMap to, final MaplePortal pto) {
        changeMapInternal(to, pto.getPosition(), MaplePacketCreator.getWarpToMap(to, pto.getId(), this), pto);
    }

    public void changeMapInternal(final MapleMap to, final Point pos, byte[] warpPacket, final MaplePortal pto) {//맵 레벨제한
        if (to == null) {
            return;
        }
//        if (to.getId() == 600010150) {
//            if ((level <= 999)) {
//                MapleCharacter mc = client.getPlayer();
//                mc.changeMap(910000000);
//                dropMessage(5, "[안내] 다음 맵은 추후 업데이트 예정입니다.");
//                return;
//            }
//        }

        if (to.getId() >= 600000000 && to.getId() <= 600020600) {
            if ((level < 280)) {
                MapleCharacter mc = client.getPlayer();
                mc.changeMap(910000000);
                dropMessage(5, "[안내] 280미만은 입장 불가능.");
                return;
            }
        }

        if (to.getId() == 802000802) {
            if ((level > 100)) {
                MapleCharacter mc = client.getPlayer();
                mc.changeMap(802000803);
                dropMessage(5, "[안내] 알수없는 힘에 의해 코어블레이즈 에게 도달 합니다.");
                return;
            }
        }

//        if (to.getId() == 600010160) {
//            if ((level <= 999)) {
//                MapleCharacter mc = client.getPlayer();
//                mc.changeMap(910000000);
//                dropMessage(5, "[안내] 다음 맵은 추후 업데이트 예정입니다.");
//                return;
//            }
//        }
        if (to.getId() >= 600010200 && to.getId() <= 600010230) {
            if ((level < 310)) {
                MapleCharacter mc = client.getPlayer();
                mc.changeMap(910000000);
                dropMessage(5, "[안내] 310레벨 달성후 입장 가능합니다");
                return;
            }
        }

        if (to.getId() >= 600010160 && to.getId() <= 600010180) {
            if ((level < 300)) {
                MapleCharacter mc = client.getPlayer();
                mc.changeMap(910000000);
                dropMessage(5, "[안내] 300레벨 달성후 입장 가능합니다");
                return;
            }
        }

        if (to.getId() == 910320000) {//미개통구역
            if (!(level <= 1 && level >= 201)) {
                dropMessage(5, "[안내] 미개통 구역은 추후 업데이트 예정입니다.");
                return;
            }
        }
        /*  if (to.getId() >= 106020000 && to.getId() <= 106021800) { //버섯왕국
         if (!(level <= 1 && level >= 201)) {
         dropMessage(5, "[안내] 버섯왕국 테마던전은 추후 업데이트 예정입니다.");
         return;
         }
         }*//*
         if (to.getId() >= 103040000 && to.getId() <= 103040460) { //커닝스퀘어
         if (!(level <= 1 && level >= 200)) {
         dropMessage(5, "[안내] 커닝스퀘어 테마던전은 추후 업데이트 예정입니다.");
         return;
         }
         }*/

        if (getId() >= 271010000 && getId() <= 271020100) { //파괴된 헤네시스
            if (!(level >= 1 && level >= 160)) {
                dropMessage(5, "[안내] 파괴된 헤네시스 테마던전은 160레벨 이상 이용이 가능합니다.");
                return;
            }
        }
        if (to.getId() >= 211060100 && to.getId() <= 211060900) { //사자왕의 성
            if (!(level >= 1 && level >= 120)) {
                dropMessage(5, "[안내] 사자왕의 성 테마던전은 120레벨 이상 이용이 가능합니다.");
                return;
            }
        }
        /*    if (to.getId() >= 950100000 && to.getId() <= 950101100) {//황금사원
         if (!(level <= 1 && level >= 200)) {
         dropMessage(5, "[안내] 황금사원 테마던전은 추후 업데이트 예정입니다.");
         return;
         }
         }
         if (to.getId() >= 200100000 && to.getId() <= 200101600) {//크리세
         if (!(level <= 1 && level >= 200)) {
         dropMessage(5, "[안내] 천상의 크리세 테마던전은 추후 업데이트 예정입니다.");
         return;
         }
         }         
         if (to.getId () == 200090500) {//시간의신전입구
         if (!(level <= 1 && level >= 120)) {
         dropMessage(5, "[안내] 시간의 신전의 입장 권장 레벨은 120레벨 입니다.");
         return;
         }
         }   
         if (to.getId() >= 270000000 && to.getId() <= 270030100) {//시간의신전
         if (!(level <= 1 && level >= 100)) {
         dropMessage(5, "[안내] 시간의 신전은 추후 업데이트 예정입니다.");
         return;
         }
         }
         if (to.getId() >= 921120000 && to.getId() <= 921121200) {//호브제왕
         if (!(level <= 1 && level >= 201)) {
         dropMessage(5, "[안내] 호브제왕의 분노 파티퀘스트는 추후 업데이트 예정입니다.");
         return;
         }
         }        
         if (to.getId () == 261000011) {//로미오
         if (!(level <= 1 && level >= 201)) {
         dropMessage(5, "[안내] 현재 로미오와 줄리엣 파티퀘스트는 줄리엣에게만 입장 가능합니다.");
         return;
         }
         }        
         if (to.getId () == 261000021) {//줄리엣
         if (!(level <= 1 && level >= 200)) {
         dropMessage(5, "[안내] 로미오와 줄리엣 파티퀘스트는 추후 업데이트 예정입니다.");
         return;
         }
         }    
         if (to.getId () == 926010000) {//피라미드
         if (!(level <= 1 && level >= 201)) {
         dropMessage(5, "[안내] 네트의 피라미드 파티퀘스트는 잠시 수리 중 입니다.");
         return;
         }
         }            
         if (to.getId () == 920010000) {//여신의흔적
         if (!(level <= 1 && level >= 201)) {
         dropMessage(5, "[안내] 여신의흔적 파티퀘스트는 잠시 수리 중 입니다.");
         return;
         }
         }     */
        if (to.getId() == 105100100) {//마발
            if (!(level <= 1 && level >= 201)) {
                dropMessage(5, "[안내] 마왕 발록 처치는 추후 업데이트 예정입니다.");
                return;
            }
        }
        if (to.getId() == 271000000) {//미래의문
            if (!(level >= 1 && level >= 160)) {
                dropMessage(5, "[안내] 미래의 문 테마던전은 160레벨 이상 이용 가능합니다.");
                return;
            }
        }
        /*     if (to.getId () == 221023300) {//101층
         if (!(level <= 1 && level >= 200)) {
         dropMessage(5, "[안내] 루디브리엄 파티퀘스트 잠시 수리 중 입니다.");
         return;
         }
         }       */
        final int nowmapid = map.getId();
        if (eventInstance != null) {
            eventInstance.changedMap(this, to.getId());
        }
        removeMechDoor();
        final boolean pyramid = pyramidSubway != null;
        if (map.getId() == nowmapid) {
            client.getSession().write(warpPacket);
            if (isHidden()) {
                client.getSession().write(MaplePacketCreator.GmHide(isHidden()));
            }
            //강제 설정 시그너스 여제 부활
            final boolean shouldChange = this.getMapId() == 271040100 || client.getChannelServer().getPlayerStorage().getCharacterById(getId()) != null;
            final boolean shouldState = map.getId() == to.getId();
            if (shouldChange && shouldState) {
                to.setCheckStates(false);
            }
            MapleMap oldMap = map;
            if (shouldChange) {
                map = to;
                oldMap.removePlayer(this);
                setPosition(pos);
                if (getPet(0) != null) {
                    getPet(0).setPos(pos);
                    getPet(0).setStance(0);
                }
                setStance(0);
                to.addPlayer(this);
                stats.relocHeal(this);
                if (shouldState) {
                    to.setCheckStates(true);
                }
                initializeDeathCount();
            } else {
                map.removePlayer(this);
            }
        }
        if (pyramid && pyramidSubway != null) { //checks if they had pyramid before AND after changing
            pyramidSubway.onChangeMap(this, to.getId());
        }
        final MapleStatEffect darkAura = this.getStatForBuff(MapleBuffStat.DARK_AURA);
        final MapleStatEffect blueAura = this.getStatForBuff(MapleBuffStat.BLUE_AURA);
        final MapleStatEffect yellowAura = this.getStatForBuff(MapleBuffStat.YELLOW_AURA);

        MapleCharacter mc = client.getPlayer();
        int gmLevel = mc.getGMLevel();

        if (to.getId() == 188888888 && gmLevel > PlayerGMRank.NORMAL.getLevel()) {
            mc.changeMap(101000000);
            return;
        }
    }

    public void cancelChallenge() {
        if (challenge != 0 && client.getChannelServer() != null) {
            final MapleCharacter chr = client.getChannelServer().getPlayerStorage().getCharacterById(challenge);
            if (chr != null) {
                chr.dropMessage(6, getName() + " has denied your request.");
                chr.setChallenge(0);
            }
            dropMessage(6, "Denied the challenge.");
            challenge = 0;
        }
    }

    public void leaveMap(MapleMap map) {
        controlledLock.writeLock().lock();
        visibleMapObjectsLock.writeLock().lock();
        try {
            for (final MapleMapObject monstermo : map.getAllMonstersThreadsafe()) {
                final MapleMonster mons = (MapleMonster) monstermo;
                if (mons != null) {
                    mons.setController(null);
                    mons.setControllerHasAggro(false);
                    map.updateMonsterController(mons);
                }
            }
            controlled.clear();
            visibleMapObjects.clear();
        } finally {
            controlledLock.writeLock().unlock();
            visibleMapObjectsLock.writeLock().unlock();
        }
        if (chair != 0) {
            chair = 0;
        }
        clearLinkMid();
        cancelFishingTask();
        cancelChallenge();
        if (!getMechDoors().isEmpty()) {
            removeMechDoor();
        }
        cancelMapTimeLimitTask();
        if (getTrade() != null) {
            MapleTrade.cancelTrade(getTrade(), client, this);
        }
    }

    public void changeJob(int newJob) {
        try {
            cancelEffectFromBuffStat(MapleBuffStat.SHADOWPARTNER);
            this.job = (short) newJob;
            updateSingleStat(MapleStat.JOB, newJob);
            if (!GameConstants.isBeginnerJob(newJob)) {
                if (GameConstants.isEvan(newJob) || GameConstants.isResist(newJob) || GameConstants.isMercedes(newJob)) {
                    int changeSp = (newJob == 2200 || newJob == 2210 || newJob == 2211 || newJob == 2213 ? 3 : 5);
                    if (GameConstants.isResist(job) && newJob != 3100 && newJob != 3200 && newJob != 3300 && newJob != 3500) {
                        changeSp = 3;
                    }
                    remainingSp[GameConstants.getSkillBook(newJob)] += changeSp;
                    client.getSession().write(UIPacket.getSPMsg((byte) changeSp, (short) newJob));
                } else {
                    remainingSp[GameConstants.getSkillBook(newJob)]++;
                    if (newJob % 10 >= 2) {
                        remainingSp[GameConstants.getSkillBook(newJob)] += 2;
                    }
                }
                if (newJob % 10 >= 1 && level >= 70) {
                    remainingAp += 5;
                    updateSingleStat(MapleStat.AVAILABLEAP, remainingAp);
                }
                if (!isGM()) {
                    resetStatsByJob(true);
                    if (!GameConstants.isEvan(newJob)) {
                        if (getLevel() > (newJob == 200 ? 8 : 10) && newJob % 100 == 0 && (newJob % 1000) / 100 > 0) { //first job
                            remainingSp[GameConstants.getSkillBook(newJob)] += 3 * (getLevel() - (newJob == 200 ? 8 : 10));
                        }
                    } else if (newJob == 2200) {
                        MapleQuest.getInstance(22100).forceStart(this, 0, null);
                        MapleQuest.getInstance(22100).forceComplete(this, 0);
                        expandInventory((byte) 1, 4);
                        expandInventory((byte) 2, 4);
                        expandInventory((byte) 3, 4);
                        expandInventory((byte) 4, 4);
                        client.getSession().write(MaplePacketCreator.getEvanTutorial("UI/tutorial/evan/14/0"));
                        dropMessage(5, "아기 드래곤이 뭔가 하고 싶은 말이 있는 것 같다. 아기 드래곤을 클릭해 말을 걸어 보자.");
                    }
                }
                client.getSession().write(MaplePacketCreator.updateSp(this, false, false));
            }

            int maxhp = stats.getMaxHp(), maxmp = stats.getMaxMp();

            switch (job) {
                case 100: // Warrior
                case 1100: // Soul Master
                case 2100: // Aran
                case 3200:
                    maxhp += Randomizer.rand(200, 250);
                    break;
                case 3100:
                    maxhp += Randomizer.rand(200, 250);
                    maxmp = 30;
                    break;
                case 3110:
                    maxhp += Randomizer.rand(300, 350);
                    maxmp = 50;
                    break;
                case 3111:
                    maxmp = 100;
                    break;
                case 3112:
                    maxmp = 120;
                    break;
                case 200: // Magician
                case 2200: //evan
                case 2210: //evan
                    maxmp += Randomizer.rand(100, 150);
                    break;
                case 300: // Bowman
                case 400: // Thief
                case 500: // Pirate
                case 2300:
                case 3300:
                case 3500:
                    maxhp += Randomizer.rand(100, 150);
                    maxmp += Randomizer.rand(25, 50);
                    break;
                case 110: // Fighter
                case 120: // Page
                case 130: // Spearman
                case 1110: // Soul Master
                case 2110: // Aran
                case 3210:
                    maxhp += Randomizer.rand(300, 350);
                    break;
                case 210: // FP
                case 220: // IL
                case 230: // Cleric
                    maxmp += Randomizer.rand(400, 450);
                    break;
                case 310: // Bowman
                case 320: // Crossbowman
                case 410: // Assasin
                case 420: // Bandit
                case 430: // Semi Dualer
                case 510:
                case 520:
                case 530:
                case 2310:
                case 1310: // Wind Breaker
                case 1410: // Night Walker
                case 3310:
                case 3510:
                    maxhp += Randomizer.rand(200, 250);
                    maxhp += Randomizer.rand(150, 200);
                    break;
                case 900: // GM
                case 800: // Manager
                    break;
            }
            stats.setInfo(maxhp, maxmp, maxhp, maxmp);
            Map<MapleStat, Integer> statup = new EnumMap<MapleStat, Integer>(MapleStat.class);
            statup.put(MapleStat.MAXHP, Integer.valueOf(maxhp));
            statup.put(MapleStat.MAXMP, Integer.valueOf(maxmp));
            statup.put(MapleStat.HP, Integer.valueOf(maxhp));
            statup.put(MapleStat.MP, Integer.valueOf(maxmp));
            stats.recalcLocalStats(this);
            client.getSession().write(MaplePacketCreator.updatePlayerStats(statup, getJob()));
            map.broadcastMessage(this, MaplePacketCreator.showForeignEffect(getId(), 10), false);
            silentPartyUpdate();
            guildUpdate();
            familyUpdate();
            sidekickUpdate();
            if (dragon != null) {
                fakeRelog();
                //map.broadcastMessage(MaplePacketCreator.removeDragon(this.id));
                dragon = null;
            }
            baseSkills();
            if (newJob >= 2200 && newJob <= 2218) { //make new
                if (getBuffedValue(MapleBuffStat.MONSTER_RIDING) != null) {
                    cancelBuffStats(true, MapleBuffStat.MONSTER_RIDING);
                }
                makeDragon();
            }
        } catch (Exception e) {
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, e); //all jobs throw errors :(
        }
    }

    public void baseSkills() {
        if (GameConstants.getJobNumber(job) >= 3) { //third job.
            List<Integer> skills = SkillFactory.getSkillsByJob(job);
            if (skills != null) {
                for (int i : skills) {
                    final Skill skil = SkillFactory.getSkill(i);
                    if (skil != null && !skil.isInvisible() && skil.isFourthJob() && getSkillLevel(skil) <= 0 && getMasterLevel(skil) <= 0 && skil.getMasterLevel() > 0) {
                        changeSkillLevel(skil, (byte) 0, (byte) skil.getMasterLevel()); //usually 10 master
                    } else if (skil != null && skil.getName() != null && skil.getName().contains("Maple Warrior") && getSkillLevel(skil) <= 0 && getMasterLevel(skil) <= 0) {
                        changeSkillLevel(skil, (byte) 0, (byte) 10); //hackish
                    }
                }
            }
        }
    }

    public void makeDragon() {
        dragon = new MapleDragon(this);
        map.broadcastMessage(MaplePacketCreator.spawnDragon(dragon));
    }

    public MapleDragon getDragon() {
        return dragon;
    }

    public void gainAp(short ap) {
        this.remainingAp += ap;
        updateSingleStat(MapleStat.AVAILABLEAP, this.remainingAp);
    }

    public void gainSP(int sp) {
        this.remainingSp[GameConstants.getSkillBook(job)] += sp; //default
        client.getSession().write(MaplePacketCreator.updateSp(this, false));
        client.getSession().write(UIPacket.getSPMsg((byte) sp, (short) job));
    }

    public void gainSP(int sp, final int skillbook) {
        this.remainingSp[skillbook] += sp; //default
        client.getSession().write(MaplePacketCreator.updateSp(this, false));
        client.getSession().write(UIPacket.getSPMsg((byte) sp, (short) 0));
    }

    public void resetSP(int sp) {
        for (int i = 0; i < remainingSp.length; i++) {
            this.remainingSp[i] = sp;
        }
        client.getSession().write(MaplePacketCreator.updateSp(this, false));
    }

    public void resetJobSp(int sp) {
        resetSP(0);
        gainSP(sp);
        this.skills.clear();
        fakeRelog();
    }

    public void resetAPSP() {
        resetSP(0);
        gainAp((short) -this.remainingAp);
    }

    public List<Integer> getProfessions() {
        List<Integer> prof = new ArrayList<Integer>();
        for (int i = 9200; i <= 9204; i++) {
            if (getProfessionLevel(id * 10000) > 0) {
                prof.add(i);
            }
        }
        return prof;
    }

    public byte getProfessionLevel(int id) {
        int ret = getSkillLevel(id);
        if (ret <= 0) {
            return 0;
        }
        return (byte) ((ret >>> 24) & 0xFF); //the last byte
    }

    public short getProfessionExp(int id) {
        int ret = getSkillLevel(id);
        if (ret <= 0) {
            return 0;
        }
        return (short) (ret & 0xFFFF); //the first two byte
    }

    public boolean addProfessionExp(int id, int expGain) {
        int ret = getProfessionLevel(id);
        if (ret <= 0 || ret >= 10) {
            return false;
        }
        int newExp = getProfessionExp(id) + expGain;
        if (newExp >= GameConstants.getProfessionEXP(ret)) {
            //gain level
            changeProfessionLevelExp(id, ret + 1, newExp - GameConstants.getProfessionEXP(ret));
            int traitGain = (int) Math.pow(2, ret + 1);
            return true;
        } else {
            changeProfessionLevelExp(id, ret, newExp);
            return false;
        }
    }

    public void changeProfessionLevelExp(int id, int level, int exp) {
        changeSkillLevel(SkillFactory.getSkill(id), ((level & 0xFF) << 24) + (exp & 0xFFFF), (byte) 10);
    }

    public boolean changeSkillData(final Skill skill, int newLevel, byte newMasterlevel, long expiration) {
        if (skill == null || (!GameConstants.isApplicableSkill(skill.getId()) && !GameConstants.isApplicableSkill_(skill.getId()))) {
            return false;
        }
        if (newLevel == 0 && newMasterlevel == 0) {
            if (skills.containsKey(skill)) {
                skills.remove(skill);
            } else {
                return false; //nothing happen
            }
        } else {
            skills.put(skill, new SkillEntry(newLevel, newMasterlevel, expiration));
        }
        return true;
    }

    public void changeSingleSkillLevel(final Skill skill, int newLevel, byte newMasterlevel) { //1 month
        if (skill == null) {
            return;
        }
        changeSingleSkillLevel(skill, newLevel, newMasterlevel, SkillFactory.getDefaultSExpiry(skill));
    }

    public void changeSingleSkillLevel(final Skill skill, int newLevel, byte newMasterlevel, long expiration) {
        final Map<Skill, SkillEntry> list = new HashMap<Skill, SkillEntry>();
        boolean hasRecovery = false, recalculate = false;
        if (changeSkillData(skill, newLevel, newMasterlevel, expiration)) { // no loop, only 1
            list.put(skill, new SkillEntry(newLevel, newMasterlevel, expiration));
            if (GameConstants.isRecoveryIncSkill(skill.getId())) {
                hasRecovery = true;
            }
            if (skill.getId() < 80000000) {
                recalculate = true;
            }
        }
        if (list.isEmpty()) { // nothing is changed
            return;
        }
        client.getSession().write(MaplePacketCreator.updateSkills(list));
        reUpdateStat(hasRecovery, recalculate);
    }

    private void reUpdateStat(boolean hasRecovery, boolean recalculate) {
        changed_skills = true;
        if (hasRecovery) {
            stats.relocHeal(this);
        }
        if (recalculate) {
            stats.recalcLocalStats(this);
        }
    }

    public void changeSkillLevel(final Skill skill, int newLevel, byte newMasterlevel) { //1 month
        if (skill == null) {
            return;
        }
        changeSkillLevel(skill, newLevel, newMasterlevel, skill.isTimeLimited() ? (System.currentTimeMillis() + (long) (30L * 24L * 60L * 60L * 1000L)) : -1);
    }

    public final void teachSkill(final int id, final byte level, final byte masterlevel) {
        changeSkillLevel(SkillFactory.getSkill(id), level, masterlevel);
    }

    public void changeSkillLevel(final Skill skill, int newLevel, byte newMasterlevel, long expiration) {
        if (skill == null || (!GameConstants.isApplicableSkill(skill.getId()) && !GameConstants.isApplicableSkill_(skill.getId()))) {
            return;
        }
        client.getSession().write(MaplePacketCreator.updateSkill(skill.getId(), newLevel, newMasterlevel, expiration));
        if (newLevel == 0 && newMasterlevel == 0) {
            if (skills.containsKey(skill)) {
                skills.remove(skill);
            } else {
                return; //nothing happen
            }
        } else {
            skills.put(skill, new SkillEntry(newLevel, newMasterlevel, expiration));
        }
        changed_skills = true;
        if (GameConstants.isRecoveryIncSkill(skill.getId())) {
            stats.relocHeal(this);
        }
        if (skill.getId() < 80000000) {
            stats.recalcLocalStats(this);
        }
    }

    public void changeSkillLevel_Skip(final Skill skill, int newLevel, byte newMasterlevel) {
        changeSkillLevel_Skip(skill, newLevel, newMasterlevel, false);
    }

    public void changeSkillLevel_Skip(final Skill skill, int newLevel, byte newMasterlevel, boolean write) {
        if (skill == null) {
            return;
        }
        if (write) {
            client.getSession().write(MaplePacketCreator.updateSkill(skill.getId(), newLevel, newMasterlevel, -1L));
        }
        if (newLevel == 0 && newMasterlevel == 0) {
            if (skills.containsKey(skill)) {
                skills.remove(skill);
            } else {
                return; //nothing happen
            }
        } else {
            skills.put(skill, new SkillEntry(newLevel, newMasterlevel, -1L));
        }

    }

    public void revieveGainBuff() {

        if (this.revieveReturnBuffs != null) {
            if (!this.revieveReturnBuffs.isEmpty()) {
                for (PlayerBuffValueHolder pb : this.revieveReturnBuffs) {
                    if (pb != null && pb.effect != null) {
                        int buffcode = pb.effect.getSourceId();
                        //System.err.println("buff code : " + buffcode);

                        if (MapleItemInformationProvider.getInstance().getItemEffect(buffcode) != null) {
                            MapleStatEffect specEx = MapleItemInformationProvider.getInstance().getItemEffect(buffcode);
                            if (specEx != null) {
                                //System.err.println("is buff item : " + buffcode);
                                specEx.applyTo(this, this, false, null, pb.effect.getDuration());
                                continue;
                            }
                        }
                        //System.err.println("skill level: " + pb.effect.getLevel());
                        //pb.effect.applyTo(this);
                        pb.effect.applyTo(this, this, false, null, pb.effect.getDuration());
                    } else {
                        System.err.println("RevieveGainBuff SavedBuff NullPointer.");
                    }
                }
            }
        }
    }

    public final Map<MapleBuffStat, MapleBuffStatValueHolder> getEffects() { //경뿌
        return effects;
    }

    public void playerDead() {
        this.revieveReturnBuffs = this.getAllBuffs();

        //System.err.println("buff size : " + this.getAllBuffs().size());
        if (getEventInstance() != null) {
            getEventInstance().playerKilled(this);
        }
        cancelBuffStats(true, MapleBuffStat.INFINITY);
        /*        cancelEffectFromBuffStat(MapleBuffStat.SHADOWPARTNER);
         cancelEffectFromBuffStat(MapleBuffStat.MORPH);
         cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
         cancelEffectFromBuffStat(MapleBuffStat.MECH_CHANGE);
         cancelEffectFromBuffStat(MapleBuffStat.RECOVERY);
         cancelEffectFromBuffStat(MapleBuffStat.HP_BOOST);
         cancelEffectFromBuffStat(MapleBuffStat.MP_BOOST);
         cancelEffectFromBuffStat(MapleBuffStat.ENHANCED_MAXHP);
         cancelEffectFromBuffStat(MapleBuffStat.ENHANCED_MAXMP);
         cancelEffectFromBuffStat(MapleBuffStat.MAXHP);
         cancelEffectFromBuffStat(MapleBuffStat.MAXMP);     */
        //cancelAllBuffs_Dead();
        if (getBuffedValue(MapleBuffStat.YELLOW_AURA) != null) {
            cancelBuffStats(true, MapleBuffStat.YELLOW_AURA);
        }

        cancelAllDebuffs();
        dispelSummons();
        checkFollow();
        dotHP = 0;
        lastDOTTime = 0;

        boolean reduceDeathExp = true;
        if (getMapId() / 1000000 == 109) { // 이벤트맵
            reduceDeathExp = false;
        } else if (getMapId() / 10000000 == 98) { // 몬스터 카니발
            reduceDeathExp = false;
        } else if (getMapId() == 270020211) { // 마법제련술사의 방
            reduceDeathExp = false;
        } else if (getMapId() >= 925020000 && getMapId() <= 925033804) { // 무릉도장
            reduceDeathExp = false;
        }

        if (!GameConstants.isBeginnerJob(job) && reduceDeathExp) {
            int charms = getItemQuantity(5130000, false);
            if (charms > 0) {
                int days = getRemainingExpirationDay(5130000);
                MapleInventoryManipulator.removeById(client, MapleInventoryType.CASH, 5130000, 1, true, false);

                charms--;
                if (charms > 0xFF) {
                    charms = 0xFF;
                }
                client.getSession().write(MTSCSPacket.useCharm((byte) charms, (byte) 0));
            } else {
                float diepercentage = 0.0f;
                int expforlevel = getNeededExp();
                if (map.isTown() || FieldLimitType.RegularExpLoss.check(map.getFieldLimit())) {
                    diepercentage = 0.01f;
                } else {
                    float decRate;
                    if (getJob() / 100 == 3) {
                        decRate = 0.08F;
                    } else {
                        decRate = 0.2F;
                    }
                    diepercentage = decRate / getStat().luk + 0.05F;
                }

                float myExpF = getExp() - expforlevel * diepercentage;
                if (myExpF <= 0.0F) {
                    myExpF = 0.0F;
                }
                float ff = expforlevel - 1;
                if (myExpF < ff - 1.0F) {
                    ff = myExpF;
                }
                this.exp = (int) ff;
            }
            this.updateSingleStat(MapleStat.EXP, this.exp);
        }
        getStat().checkEquipDurabilitys(this, -1000, false, true);
    }

    public void addCanGainFame() {
        canGainNoteFame++;
    }

    public int getRemainingExpirationDay(int itemid) {
        int possesed = inventory[GameConstants.getInventoryType(itemid).ordinal()].expirationById(itemid);
        return possesed;
    }

    public void updatePartyMemberHP() {
        if (party != null && client.getChannelServer() != null) {
            final int channel = client.getChannel();
            for (MaplePartyCharacter partychar : party.getMembers()) {
                if (partychar != null && partychar.getMapid() == getMapId() && partychar.getChannel() == channel) {
                    final MapleCharacter other = client.getChannelServer().getPlayerStorage().getCharacterByName(partychar.getName());
                    if (other != null) {
                        other.getClient().getSession().write(MaplePacketCreator.updatePartyMemberHP(getId(), stats.getHp(), stats.getCurrentMaxHp()));
                    }
                }
            }
        }
    }

    public void receivePartyMemberHP() {
        if (party == null) {
            return;
        }
        int channel = client.getChannel();
        for (MaplePartyCharacter partychar : party.getMembers()) {
            if (partychar != null && partychar.getMapid() == getMapId() && partychar.getChannel() == channel) {
                MapleCharacter other = client.getChannelServer().getPlayerStorage().getCharacterByName(partychar.getName());
                if (other != null) {
                    client.getSession().write(MaplePacketCreator.updatePartyMemberHP(other.getId(), other.getStat().getHp(), other.getStat().getCurrentMaxHp()));
                }
            }
        }
    }

    public void healHP(int delta) {
        addHP(delta);
        client.getSession().write(MaplePacketCreator.showOwnHpHealed(delta));
        getMap().broadcastMessage(this, MaplePacketCreator.showHpHealed(getId(), delta), false);
    }

    public void healMP(int delta) {
        client.getSession().write(MaplePacketCreator.showOwnHpHealed(delta));
        getMap().broadcastMessage(this, MaplePacketCreator.showHpHealed(getId(), delta), false);
    }

    /**
     * Convenience function which adds the supplied parameter to the current hp
     * then directly does a updateSingleStat.
     *
     * @see MapleCharacter#setHp(int)
     * @param delta
     */
    public void addHP(int delta) {
        if (stats.setHp(stats.getHp() + delta, this)) {
            updateSingleStat(MapleStat.HP, stats.getHp());
        }
    }

    /**
     * Convenience function which adds the supplied parameter to the current mp
     * then directly does a updateSingleStat.
     *
     * @see MapleCharacter#setMp(int)
     * @param delta
     */
    public void addMP(int delta) {
        //dropMessage(5, "엠피 " + delta);
        if (stats.setMp(stats.getMp() + delta, this)) {
            updateSingleStat(MapleStat.MP, stats.getMp());
        }
    }

    public void addMPHP(int hpDiff, int mpDiff) {
        Map<MapleStat, Integer> statups = new EnumMap<MapleStat, Integer>(MapleStat.class);

        if (stats.setHp(stats.getHp() + hpDiff, this)) {
            statups.put(MapleStat.HP, Integer.valueOf(stats.getHp()));
        }
        if (stats.setMp(stats.getMp() + mpDiff, this)) {
            statups.put(MapleStat.MP, Integer.valueOf(stats.getMp()));
        }
        if (statups.size() > 0) {
            client.getSession().write(MaplePacketCreator.updatePlayerStats(statups, getJob()));
        }
        //dropMessage(6, "hp" + stats.getHp() + " || to reduce(대미지)" + hpDiff + " || Chp" + (stats.getHp() + hpDiff));
    }

    public void updateSingleStat(MapleStat stat, int newval) {
        updateSingleStat(stat, newval, false);
    }

    /**
     * Updates a single stat of this MapleCharacter for the client. This method
     * only creates and sends an update packet, it does not update the stat
     * stored in this MapleCharacter instance.
     *
     * @param stat
     * @param newval
     * @param itemReaction
     */
    public void updateSingleStat(MapleStat stat, int newval, boolean itemReaction) {
        if (stat == MapleStat.AVAILABLESP) {
            client.getSession().write(MaplePacketCreator.updateSp(this, itemReaction, false));
            return;
        }
        Map<MapleStat, Integer> statup = new EnumMap<MapleStat, Integer>(MapleStat.class);
        statup.put(stat, newval);
        client.getSession().write(MaplePacketCreator.updatePlayerStats(statup, itemReaction, getJob()));
    }

    public void gainExp(final int total, final boolean show, final boolean inChat, final boolean white) {
        try {
            int prevexp = getExp();
            int needed = getNeededExp();
            if (total > 0) {
                stats.checkEquipLevels(this, total); //gms like
            }
            if (level >= 999 || (GameConstants.isKOC(job) && level >= 120)) {
                setExp(0);
                //if (exp + total > needed) {
                //    setExp(needed);
                //} else {
                //    exp += total;
                //}
            } else if (level < 200) {
                boolean leveled = false;
                long tot = (long) exp + total;
                if (tot >= needed) {
                    exp = (int) Math.max(0, Math.min((long) exp + total, Integer.MAX_VALUE));
                    levelUp();
                    leveled = true;
                    if ((level == 999 || (GameConstants.isKOC(job) && level >= 120))) {
                        setExp(0);
                    } else {
                        needed = getNeededExp();
                        if (exp >= needed) {
                            setExp(needed - 1);
                        }
                    }
                } else {
                    exp = (int) Math.max(0, Math.min((long) exp + total, Integer.MAX_VALUE));
                }
                if (total > 0) {
//                    familyRep(prevexp, needed, leveled);
                }
            } else if (level >= 200 && level < 999) {
                addExtraExp(total);
                if (GameConstants.getNewExpTable(level) < getExtraExp()) {
                    setExtraExp(0);
                    levelUp();
                    setExp(0);
                } else {
                    double onePerc = (double) GameConstants.getNewExpTable(level) / (double) getExtraExp() * 100.0;
                    double sssPerc = (double) Math.max(0, Math.min(Integer.MAX_VALUE / (double) onePerc * 100.0, Integer.MAX_VALUE));
                    setExp((int) sssPerc);
                }
            }

            if (total != 0) {
                if (exp < 0) { // After adding, and negative
                    if (total > 0) {
                        setExp(needed);
                    } else if (total < 0) {
                        setExp(0);
                    }
                }
                updateSingleStat(MapleStat.EXP, getExp());
                if (show) { // still show the expgain even if it's not there
                    client.getSession().write(MaplePacketCreator.GainEXP_Others(total, inChat, white));
                }
            }
        } catch (Exception e) {
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, e); //all jobs throw errors :(
        }
    }

    public void familyRep(int gainRep) {
        if (mfc != null) {
            if (level >= 180) { //레벨이 180 이상일 때만 명성도 지급
                int sensen = World.Family.setRep(mfc.getFamilyId(), mfc.getSeniorId(), 1, level, name, true);
                if (sensen > 0) {
                    World.Family.setRep(mfc.getFamilyId(), sensen, 1, level, name, true); //and we stop here
                }
            }
        }
    }

    public void familyRep(int prevexp, int needed, boolean leveled) {
        if (mfc != null) {
            int onepercent = needed / 100;
            if (onepercent <= 0) {
                return;
            }
            int percentrep = (getExp() / onepercent - prevexp / onepercent);
            boolean exppercent = ((double) getExp()) >= ((double) getNeededExp() / 10) && (double) getExp() / ((double) getNeededExp() / 10) >= 1 && (double) getExp() / ((double) getNeededExp() / 10) <= 1.025;
            boolean exppercent2 = ((double) getExp()) >= ((double) getNeededExp() / 10) && (double) getExp() / ((double) getNeededExp() / 10) >= 2 && (double) getExp() / ((double) getNeededExp() / 10) <= 2.025;
            boolean exppercent3 = ((double) getExp()) >= ((double) getNeededExp() / 10) && (double) getExp() / ((double) getNeededExp() / 10) >= 3 && (double) getExp() / ((double) getNeededExp() / 10) <= 3.025;
            boolean exppercent4 = ((double) getExp()) >= ((double) getNeededExp() / 10) && (double) getExp() / ((double) getNeededExp() / 10) >= 4 && (double) getExp() / ((double) getNeededExp() / 10) <= 4.025;
            boolean exppercent5 = ((double) getExp()) >= ((double) getNeededExp() / 10) && (double) getExp() / ((double) getNeededExp() / 10) >= 5 && (double) getExp() / ((double) getNeededExp() / 10) <= 5.025;
            boolean exppercent6 = ((double) getExp()) >= ((double) getNeededExp() / 10) && (double) getExp() / ((double) getNeededExp() / 10) >= 6 && (double) getExp() / ((double) getNeededExp() / 10) <= 6.025;
            boolean exppercent7 = ((double) getExp()) >= ((double) getNeededExp() / 10) && (double) getExp() / ((double) getNeededExp() / 10) >= 7 && (double) getExp() / ((double) getNeededExp() / 10) <= 7.025;
            boolean exppercent8 = ((double) getExp()) >= ((double) getNeededExp() / 10) && (double) getExp() / ((double) getNeededExp() / 10) >= 8 && (double) getExp() / ((double) getNeededExp() / 10) <= 8.025;
            boolean exppercent9 = ((double) getExp()) >= ((double) getNeededExp() / 10) && (double) getExp() / ((double) getNeededExp() / 10) >= 9 && (double) getExp() / ((double) getNeededExp() / 10) <= 9.025;
            if (leveled && level >= 180) { //레벨업 후 레벨이 180 이상일 때만 명성도 지급
                percentrep = 100 - percentrep + (level / 2);
                int sensen = World.Family.setRep(mfc.getFamilyId(), mfc.getSeniorId(), percentrep, level, name, true);
                if (sensen > 0) {
                    World.Family.setRep(mfc.getFamilyId(), sensen, percentrep, level, name, true); //and we stop here
                }
            }
            if (percentrep > 0 && !leveled && (exppercent || exppercent2 || exppercent3 || exppercent4 || exppercent5 || exppercent6 || exppercent7 || exppercent8 || exppercent9)) {
                int sensen = World.Family.setRep(mfc.getFamilyId(), mfc.getSeniorId(), percentrep * 5, level, name, true);
                if (sensen > 0) {
                    World.Family.setRep(mfc.getFamilyId(), sensen, percentrep * 5, level, name, true); //and we stop here
                }
            }
        }
    }

    public void gainExpMonster(int originalgain, int gain, final boolean show, final boolean white, final byte pty, boolean partyBonusMob, final int partyBonusRate) {
        if (ChannelServer.isElite(client.getChannel())) {
            if (getParty() != null && getParty().getMembers().size() > 1) {
                dropMessage(5, "[알림] 엘리트 채널에서는 파티를 생성한 상태에서 사냥시 경험치를 획득할 수 없습니다.");
                return;
            }
        }
        int total = gain;
        int partyinc = 0;
        int partyBonusR = 0;
        int prevexp = getExp();
        int equip = 0;
        int weddinginc = 0;
        int eventinc = 0;
        int extrabonus = 0;
        int 피시보너스 = 0;
        int rainbowweek = 0;
        int simbolexp = Math.max(0, Math.min(((total * (getArcaneExp() + getCollectionExp())) / 100), Integer.MAX_VALUE));
        MapleParty party = getParty();
        if (pty > 1 && party != null) {
            final double rate = (partyBonusRate > 0 ? (partyBonusRate / 100.0) : (map == null || !partyBonusMob || map.getPartyBonusRate() <= 0 ? 0.05 : (map.getPartyBonusRate() / 100.0)));
            partyinc = (int) Math.max(0, Math.min((((float) (gain * rate)) * (pty + (rate > 0.05 ? -1 : 1))), Integer.MAX_VALUE));
            //공격한 파티원 수 = 3명 기준
            //만약 경험치가 300일 경우 (300 * 0.05) * (공격한 파티원 수 + -1) = 15 * (3 + -1) = 30
            extrabonus = Math.max(0, Math.min(gain * 5 * pty / 100, Integer.MAX_VALUE));
            //extrabonus 라는건 임의로 추가한 것 같은데 300 * 5 * 공격한 파티원 수 / 100 = 1500 * 3 / 100 = 45
            partyinc = Math.max(0, Math.min(partyinc + extrabonus, Integer.MAX_VALUE));
            //여기서는 이제 위에 두 계산식에서 나온 값을 합침 30 + 45 = 75
            partyBonusR = Math.max(0, Math.min(partyinc * 100 / gain, Integer.MAX_VALUE));
            //여기서 이제 파티원 수를 통해 계산 된 값으로 퍼센트를 계산함. partyBonusR 75 * 100 / 300 = 25%
            partyinc = Math.max(0, Math.min(gain * partyBonusR / 100, Integer.MAX_VALUE));
            //그리고 마지막으로 여기서 위에서 계산 된 25퍼를 적용 300 * (25 / 100) = 75, 즉 원래 경험치의 25%
            //gain += partyinc;
            total = Math.max(0, Math.min(total + partyinc, Integer.MAX_VALUE)); //마지막으로 계산 된 75의 경험치가 기존 경험치에 더해짐
            if (marriageId != 0) {
                MarriageDataEntry data = MarriageManager.getInstance().getMarriage(marriageId);
                if (data != null) {
                    int spouseId = id == data.getBrideId() ? data.getGroomId() : data.getBrideId();
                    MapleCharacter spouse = map.getCharacterById(spouseId);
                    /*if (spouse != null && party.equals(spouse.getParty())) {
                     weddinginc = gain * 1 / 100; //웨딩 보너스 30%
                     }*/
                }
            }
        }

        short percentage = 0;
        int time = 0;
        int eventBonus = 0;//이벤트 보너스
        if (mobKilledNo == 3 && ServerConstants.Event_Bonus) { //이벤트 보너스, 반올림 Math.round()
            //dropMessage(6, hoursFromLogin + "ㅇㅇ" + System.currentTimeMillis());
            if (hoursFromLogin >= 1 && hoursFromLogin < 2) {
                time = 1;
                percentage = 10;
            } else if (hoursFromLogin >= 2 && hoursFromLogin < 3) {
                time = 2;
                percentage = 20;
            } else if (hoursFromLogin >= 3 && hoursFromLogin < 4) {
                time = 3;
                percentage = 30;
            } else if (hoursFromLogin >= 4) {
                time = 4;
                percentage = 40;
            }
            eventBonus = (int) Math.max(0, Math.min(((float) ((gain * percentage / 100))), Integer.MAX_VALUE)); //이벤트 보너스의 경험치량 몬스터 exp
            mobKilledNo = 0;
            total = Math.max(0, Math.min(eventBonus, Integer.MAX_VALUE));
        }

        if (bonusExpR > 0) {
            long l = Math.max(0, Math.min((long) gain * bonusExpR / 100, Integer.MAX_VALUE));
            if (l > 0) {
                equip = (int) l;
                total = Math.max(0, Math.min(total + equip, Integer.MAX_VALUE));
            }
        }
        if (stats.eventExpRate != 0) {
            eventinc = (int) Math.max(0, Math.min((gain * stats.eventExpRate), Integer.MAX_VALUE));
            total = Math.max(0, Math.min(total + eventinc, Integer.MAX_VALUE));
        }
        if (this.getSymbolExp() > 0) {
            double a = ((double) this.getSymbolExp() / 100);
            int b = (int) Math.max(0, Math.min((gain * a), Integer.MAX_VALUE));
            this.gainExp(b, false, false, false);
            this.getClient().getSession().write(MaplePacketCreator.showGainBonusExp(b, "심볼"));
        }
        if (checkPcTime()) {//PC방
            if (getPcDate() == GameConstants.getCurrentDate_NoTime()) {
                피시보너스 = (int) Math.max(0, Math.min((gain * 0.2), Integer.MAX_VALUE));
                total = Math.max(0, Math.min(total + 피시보너스, Integer.MAX_VALUE));
            }
        } else {
            if (pcbang) {
                dropMessage(5, "PC방 정량제 잔여시간이 끝났습니다.");
                getClient().getSession().write(MaplePacketCreator.enableInternetCafe((byte) 0, getCalcPcTime()));
                pcbang = false;
                if (getMapId() >= 190000000 && getMapId() <= 198000000) {
                    MapleMap to = getClient().getChannelServer().getMapFactory().getMap(193000000);
                    changeMap(to);
                    //dropMessage(5, "나가 이썌끼야");
                }
            }
        }

        MapleQuestStatus summonEXPData = this.getQuestNAdd(MapleQuest.getInstance(202306120));
        if (summonEXPData != null) {
            String data = null;
            if ((data = summonEXPData.getCustomData()) != null) {
                try {
                    int summonEXP = Integer.parseInt(data);
                    int give = (int) Math.max(0, Math.min((gain * (summonEXP * 0.01)), Integer.MAX_VALUE));
                    this.gainExp(give, false, false, false);
                    this.getClient().getSession().write(MaplePacketCreator.showGainBonusExp(give, "소환수"));
                } catch (NumberFormatException e) {

                }
            }
        }

        if (gain > 0 && total < gain) { //just in case
            total = Integer.MAX_VALUE;
        }
        total = Math.max(0, Math.min(total, Integer.MAX_VALUE)); //최소 0, 최대 21억의 값을 가짐.
        int needed = getNeededExp();
        if (total > 0) {
            stats.checkEquipLevels(this, total); //gms like

            this.mobKillCount++;
            int checkKillCount = 1000;
            if (mobKillCount >= checkKillCount && mobKillCount % checkKillCount == 0) {
                this.mobKillCount = 0;
                int gainRep = 20;
                familyRep(gainRep);//명성도
            }
        }
        if (level >= 999 || (GameConstants.isKOC(job) && level >= 120)) {
            setExp(0);
            //dropMessage(5,"3");
            //if (exp + total > needed) {
            //    setExp(needed);
            //} else {
            //    exp += total;
            //}
        } else if (level < 200) {
            boolean leveled = false;
            if (Math.max(0, Math.min((long) exp + total, Integer.MAX_VALUE)) >= needed || exp >= needed) {
                exp = (int) Math.max(0, Math.min((long) exp + total, Integer.MAX_VALUE));
                levelUp();
                leveled = true;
                if ((level == 999 || (GameConstants.isKOC(job) && level >= 120))) {
                    setExp(0);
                } else {
                    needed = getNeededExp();
                    if (exp >= needed) {
                        setExp(needed);
                    }
                }
            } else {
                exp = (int) Math.max(0, Math.min((long) exp + total, Integer.MAX_VALUE));
            }
            //  dropMessage(6, "게인 : " + gain + " 토탈 : " + total + " 파티보너스R : " + partyBonusR + " 파티사이즈 : " + pty + " 파티inc : " + partyinc /*+ " 맵보너스: " + 맵보너스 */ + " 피시보너스 : " + 피시보너스 + " exp : " + exp);

            if (total > 0) {
                total = (int) Math.max(0, Math.min((long) total + simbolexp, Integer.MAX_VALUE));
//                familyRep(prevexp, needed, leveled);
            }

        }

        if (level >= 200 && level < 999) {  // 만렙포인트            
            addExtraExp(total);
            if (GameConstants.getNewExpTable(level) < getExtraExp()) {
                setExtraExp(0);
                levelUp();
                setExp(0);
            } else {
                double onePerc = (double) GameConstants.getNewExpTable(level) / (double) getExtraExp() * 100.0;
                double sssPerc = (double) Math.max(0, Math.min(Integer.MAX_VALUE / (double) onePerc * 100.0, Integer.MAX_VALUE));
                setExp((int) sssPerc);
            }
        }

        if (gain != 0) {
            if (exp < 0) { // After adding, and negative
                if (gain > 0) {
                    setExp(getNeededExp());
                } else if (gain < 0) {
                    setExp(0);
                }
            }
            //dropMessage(5, "bonusExpR: " + bonusExpR + " total: " + total + " equip: " + equip + " EXP: " + getExp());
            updateSingleStat(MapleStat.EXP, getExp());
            if (show) { // still show the expgain even if it's not there
                client.getSession().write(MaplePacketCreator.GainEXP_Monster(total, white, partyinc, rainbowweek, equip, 피시보너스, eventinc, percentage, weddinginc));
            }
        }
    }

    public void forceReAddItem_NoUpdate(Item item, MapleInventoryType type) {
        getInventory(type).removeSlot(item.getPosition());
        getInventory(type).addFromDB(item);
    }

    public void forceReAddItem(Item item, MapleInventoryType type) { //used for stuff like durability, item exp/level, probably owner?
        forceReAddItem_NoUpdate(item, type);
        if (type != MapleInventoryType.UNDEFINED) {
            client.getSession().write(MaplePacketCreator.updateSpecialItemUse(item, type == MapleInventoryType.EQUIPPED ? (byte) 1 : type.getType(), this));
        }
    }

    public void forceReAddItem_Flag(Item item, MapleInventoryType type) { //used for flags
        forceReAddItem_NoUpdate(item, type);
        if (type != MapleInventoryType.UNDEFINED) {
            client.getSession().write(MaplePacketCreator.updateSpecialItemUse_(item, type == MapleInventoryType.EQUIPPED ? (byte) 1 : type.getType(), this));
        }
    }

    public void forceReAddItem_Book(Item item, MapleInventoryType type) { //used for mbook
        forceReAddItem_NoUpdate(item, type);
        if (type != MapleInventoryType.UNDEFINED) {
            client.getSession().write(MaplePacketCreator.upgradeBook(item, this));
        }
    }

    public void silentPartyUpdate() {
        if (party != null) {
            World.Party.updateParty(party.getId(), PartyOperation.SILENT_UPDATE, new MaplePartyCharacter(this));
        }
    }

    public boolean isSuperGM() {
        return gmLevel >= PlayerGMRank.SUPERGM.getLevel();
    }

    public boolean isIntern() {
        return gmLevel >= PlayerGMRank.INTERN.getLevel();
    }

    public boolean isGM() {
        return gmLevel >= PlayerGMRank.GM.getLevel();
    }

    public boolean isAdmin() {
        return gmLevel >= PlayerGMRank.ADMIN.getLevel();
    }

    public int getGMLevel() {
        return gmLevel;
    }

    public boolean hasGmLevel(int level) {
        return gmLevel >= level;
    }

    public final MapleInventory getInventory(MapleInventoryType type) {
        return inventory[type.ordinal()];
    }

    public final MapleInventory[] getInventorys() {
        return inventory;
    }

    public final void expirationTask(boolean pending, boolean firstLoad) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (pending) {
            if (pendingExpiration != null) {
                for (Integer z : pendingExpiration) {
                    if (ii.isCash(z.intValue())) {
                        client.getSession().write(MTSCSPacket.cashItemExpiredI(z.intValue()));
                    } else {
                        client.getSession().write(MTSCSPacket.normalItemExpired(z.intValue()));
                    }
                    if (!firstLoad) {
                        final Pair<Integer, String> replace = ii.replaceItemInfo(z.intValue());
                        if (replace != null && replace.left > 0 && replace.right.length() > 0) {
                            dropMessage(5, replace.right);
                        }
                    }
                }
            }
            pendingExpiration = null;
            if (pendingSkills != null) {
                for (Integer z : pendingSkills) {
                    client.getSession().write(MaplePacketCreator.updateSkill(z, 0, 0, -1));
                    client.getSession().write(MaplePacketCreator.serverNotice(5, "[" + SkillFactory.getSkillName(z) + "] 스킬의 사용기한이 지나 삭제됩니다."));
                }
            } //not real msg
            pendingSkills = null;
            return;
        }
        final MapleQuestStatus stat = getQuestNoAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
        long expiration;
        final List<Integer> ret = new ArrayList<Integer>();
        final long currenttime = System.currentTimeMillis();
        final List<Triple<MapleInventoryType, Item, Boolean>> toberemove = new ArrayList<Triple<MapleInventoryType, Item, Boolean>>(); // This is here to prevent deadlock.
        final List<Item> tobeunlock = new ArrayList<Item>(); // This is here to prevent deadlock.

        for (final MapleInventoryType inv : MapleInventoryType.values()) {
            for (final Item item : getInventory(inv)) {
                expiration = item.getExpiration();

                if ((expiration != -1 && !GameConstants.isPet(item.getItemId()) && currenttime > expiration) || (firstLoad && ii.isLogoutExpire(item.getItemId()))) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        tobeunlock.add(item);
                    } else if (currenttime > expiration) {
                        toberemove.add(new Triple<MapleInventoryType, Item, Boolean>(inv, item, false));
                    }
                } else if (item.getItemId() == 5000054 && item.getPet() != null && item.getPet().getSecondsLeft() <= 0) {
                    toberemove.add(new Triple<MapleInventoryType, Item, Boolean>(inv, item, false));
                } else if (item.getPosition() == -59) {
                    if (FileTime.compareFileTime(getEquipExtExpire(), FileTime.systemTimeToFileTime()) < 0) {
//                    if (stat == null || stat.getCustomData() == null || Long.parseLong(stat.getCustomData()) < currenttime) {
                        toberemove.add(new Triple<MapleInventoryType, Item, Boolean>(inv, item, true));
                    }
                }
            }
        }
        Item item;
        for (final Triple<MapleInventoryType, Item, Boolean> itemz : toberemove) {
            item = itemz.getMid();
            getInventory(itemz.getLeft()).removeItem(item.getPosition(), item.getQuantity(), false);
            if (itemz.getRight() && getInventory(GameConstants.getInventoryType(item.getItemId())).getNextFreeSlot() > -1) {
                item.setPosition(getInventory(GameConstants.getInventoryType(item.getItemId())).getNextFreeSlot());
                getInventory(GameConstants.getInventoryType(item.getItemId())).addFromDB(item);
            } else {
                ret.add(item.getItemId());
            }
            if (!firstLoad) {
                final Pair<Integer, String> replace = ii.replaceItemInfo(item.getItemId());
                if (replace != null && replace.left > 0) {
                    Item theNewItem = null;
                    if (GameConstants.getInventoryType(replace.left) == MapleInventoryType.EQUIP) {
                        theNewItem = ii.getEquipById(replace.left);
                        theNewItem.setPosition(item.getPosition());
                    } else {
                        theNewItem = new Item(replace.left, item.getPosition(), (short) 1, (byte) 0);
                    }
                    getInventory(itemz.getLeft()).addFromDB(theNewItem);
                }
            }
        }
        for (final Item itemz : tobeunlock) {
            itemz.setExpiration(-1);
            itemz.setFlag((byte) (itemz.getFlag() - ItemFlag.LOCK.getValue()));
        }
        this.pendingExpiration = ret;

        final List<Integer> skilz = new ArrayList<Integer>();
        final List<Skill> toberem = new ArrayList<Skill>();
        for (Entry<Skill, SkillEntry> skil : skills.entrySet()) {
            if (skil.getValue().expiration != -1 && currenttime > skil.getValue().expiration) {
                toberem.add(skil.getKey());
            }
        }
        for (Skill skil : toberem) {
            skilz.add(skil.getId());
            this.skills.remove(skil);
            changed_skills = true;
        }
        this.pendingSkills = skilz;
        if (stat != null && stat.getCustomData() != null && Long.parseLong(stat.getCustomData()) < currenttime) { //expired bro
            quests.remove(MapleQuest.getInstance(7830));
            quests.remove(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
        }
    }

    public final void expirationTask2(boolean pending, boolean firstLoad) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final MapleQuestStatus stat = getQuestNoAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
        long expiration;
        final List<Integer> ret = new ArrayList<Integer>();
        final long currenttime = System.currentTimeMillis();
        final List<Triple<MapleInventoryType, Item, Boolean>> toberemove = new ArrayList<Triple<MapleInventoryType, Item, Boolean>>(); // This is here to prevent deadlock.
        final List<Item> tobeunlock = new ArrayList<Item>(); // This is here to prevent deadlock.

        for (final MapleInventoryType inv : MapleInventoryType.values()) {
            for (final Item item : getInventory(inv)) {
                expiration = item.getExpiration();

                if ((expiration != -1 && !GameConstants.isPet(item.getItemId()) && currenttime > expiration) || (firstLoad && ii.isLogoutExpire(item.getItemId()))) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        tobeunlock.add(item);
                    } else if (currenttime > expiration) {
                        toberemove.add(new Triple<MapleInventoryType, Item, Boolean>(inv, item, false));
                    }
                } else if (item.getItemId() == 5000054 && item.getPet() != null && item.getPet().getSecondsLeft() <= 0) {
                    toberemove.add(new Triple<MapleInventoryType, Item, Boolean>(inv, item, false));
                } else if (item.getPosition() == -30) {
                    if (stat == null || stat.getCustomData() == null || Long.parseLong(stat.getCustomData()) < currenttime) {
                        toberemove.add(new Triple<MapleInventoryType, Item, Boolean>(inv, item, true));
                    }
                } else if (expiration != -1 && item.getItemId() == 4001432 && getMapId() / 1000 == 950100) {
                    warp(950100000);
                    dropMessage(6, "프리미엄 티켓의 시간이 끝나 바깥으로 이동합니다");
                }

            }
        }
        Item item;
        for (final Triple<MapleInventoryType, Item, Boolean> itemz : toberemove) {
            item = itemz.getMid();
            getInventory(itemz.getLeft()).removeItem(item.getPosition(), item.getQuantity(), false);
            if (itemz.left == MapleInventoryType.EQUIPPED) {
                getClient().sendPacket(MaplePacketCreator.dropInventoryItem(MapleInventoryType.EQUIP, item.getPosition()));
            } else {
                client.getSession().write(MaplePacketCreator.clearInventoryItem(itemz.getLeft(), item.getPosition(), false));
            }
            if (itemz.getRight() && getInventory(GameConstants.getInventoryType(item.getItemId())).getNextFreeSlot() > -1) {
                item.setPosition(getInventory(GameConstants.getInventoryType(item.getItemId())).getNextFreeSlot());
                getInventory(GameConstants.getInventoryType(item.getItemId())).addFromDB(item);
            } else {
                ret.add(item.getItemId());
            }
            if (!firstLoad) {
                final Pair<Integer, String> replace = ii.replaceItemInfo(item.getItemId());
                if (replace != null && replace.left > 0) {
                    Item theNewItem = null;
                    if (GameConstants.getInventoryType(replace.left) == MapleInventoryType.EQUIP) {
                        theNewItem = ii.getEquipById(replace.left);
                        theNewItem.setPosition(item.getPosition());
                    } else {
                        theNewItem = new Item(replace.left, item.getPosition(), (short) 1, (byte) 0);
                    }
                    getInventory(itemz.getLeft()).addFromDB(theNewItem);
                }
            }
        }
        for (final Item itemz : tobeunlock) {
            itemz.setExpiration(-1);
            itemz.setFlag((byte) (itemz.getFlag() - ItemFlag.LOCK.getValue()));
        }
        this.pendingExpiration = ret;
        if (pending) {
            if (pendingExpiration != null) {
                for (Integer z : pendingExpiration) {
                    client.getSession().write(CSPacket.itemExpired(z.intValue(), ii.isCash(z.intValue())));
                    equipChanged();
                    if (!firstLoad) {
                        final Pair<Integer, String> replace = ii.replaceItemInfo(z.intValue());
                        if (replace != null && replace.left > 0 && replace.right.length() > 0) {
                            dropMessage(5, replace.right);
                        }
                    }
                }
            }
            pendingExpiration = null;
        }
    }

    public MapleShop getShop() {
        return shop;
    }

    public void setShop(MapleShop shop) {
        this.shop = shop;
    }

    public void setAdminShop(MapleAdminShop shop) {
        this.adminshop = shop;
    }

    public MapleAdminShop getAdminShop() {
        return adminshop;
    }

    public int getMeso() {
        return meso;
    }

    public final int[] getSavedLocations() {
        return savedLocations;
    }

    public int getSavedLocation(SavedLocationType type) {
        return savedLocations[type.getValue()];
    }

    public void saveLocation(SavedLocationType type) {
        savedLocations[type.getValue()] = getMapId();
        changed_savedlocations = true;
    }

    public void saveLocation(SavedLocationType type, int mapz) {
        savedLocations[type.getValue()] = mapz;
        changed_savedlocations = true;
    }

    public void clearSavedLocation(SavedLocationType type) {
        savedLocations[type.getValue()] = -1;
        changed_savedlocations = true;
    }

    public void gainMeso(int gain, boolean show) {
        gainMeso(gain, show, false);
    }

    public void gainMeso(int gain, boolean show, boolean inChat) {
        gainMeso(gain, show, inChat, false);
    }

    public void gainMeso(int gain, boolean show, boolean inChat, boolean hangDisable) { //21억 넘으면 자동 가가주화로 변경 메소변환
        if (meso + gain < 0) {
            client.getSession().write(MaplePacketCreator.enableActions());
            return;
        }

        meso += gain;

        if (meso >= 1000000) {
        }
        if (meso >= 10000000) {
        }
        if (meso >= 100000000) {
        }

        if (meso >= 2100000000) {
            meso = meso - 2000000000;
            gainItem(4001211, (short) 20);
            getClient().getSession().write(MaplePacketCreator.yellowChat("보유 메소가 최대를 넘어 자동 교환됩니다."));
        }
        updateSingleStat(MapleStat.MESO, meso, hangDisable);
        client.getSession().write(MaplePacketCreator.enableActions());
        if (show) {
            client.getSession().write(MaplePacketCreator.showMesoGain(gain, inChat));
        }
    }

    public void controlMonster(MapleMonster monster, boolean aggro) {
        if (monster == null) {
            return;
        }
        monster.setController(this);
        controlledLock.writeLock().lock();
        try {
            controlled.add(monster);
        } finally {
            controlledLock.writeLock().unlock();
        }
        client.getSession().write(MobPacket.controlMonster(monster, false, aggro));
        monster.sendStatus(client);
    }

    public void stopControllingMonster(MapleMonster monster) {
        if (monster == null) {
            return;
        }
        controlledLock.writeLock().lock();
        try {
            if (controlled.contains(monster)) {
                controlled.remove(monster);
            }
        } finally {
            controlledLock.writeLock().unlock();
        }
    }

    public void checkMonsterAggro(MapleMonster monster) {
        if (monster == null) {
            return;
        }
        if (monster.getController() == this) {
            monster.setControllerHasAggro(true);
        } else {
            monster.switchController(this, true);
        }
    }

    public int getControlledSize() {
        return controlled.size();
    }

    public int getAccountID() {
        return accountid;
    }

    public void mobKilled(final int id, final int skillID) {
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() != 1 || !q.hasMobKills()) {
                continue;
            }
            if (q.mobKilled(id, skillID)) {
                client.getSession().write(MaplePacketCreator.updateQuestMobKills(q));
                if (q.getQuest().canComplete(this, null)) {
                    client.getSession().write(MaplePacketCreator.getShowQuestCompletion(q.getQuest().getId()));
                }
            }
        }
    }

    public final List<MapleQuestStatus> getStartedQuests() {
        List<MapleQuestStatus> ret = new LinkedList<MapleQuestStatus>();
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == 1 && !q.isCustom() && !q.getQuest().isBlocked()) {
                ret.add(q);
            }
        }
        return ret;
    }

    public final List<MapleQuestStatus> getCompletedQuests() {
        List<MapleQuestStatus> ret = new LinkedList<MapleQuestStatus>();
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == 2 && !q.isCustom() && !q.getQuest().isBlocked()) {
                ret.add(q);
            }
        }
        return ret;
    }

    public final List<Pair<Integer, Long>> getCompletedMedals() {
        List<Pair<Integer, Long>> ret = new ArrayList<Pair<Integer, Long>>();
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == 2 && !q.isCustom() && !q.getQuest().isBlocked() && q.getQuest().getMedalItem() > 0 && GameConstants.getInventoryType(q.getQuest().getMedalItem()) == MapleInventoryType.EQUIP) {
                ret.add(new Pair<Integer, Long>(q.getQuest().getId(), q.getCompletionTime()));
            }
        }
        return ret;
    }

    public Map<Skill, SkillEntry> getSkills() {
        return Collections.unmodifiableMap(skills);
    }

    public int getTotalSkillLevel(final Skill skill) {
        if (skill == null) {
            return 0;
        }
        final SkillEntry ret = skills.get(skill);
        if (ret == null || ret.skillevel <= 0) {
            return 0;
        }
        return Math.min(skill.getTrueMax(), ret.skillevel + (skill.isBeginnerSkill() ? 0 : (stats.combatOrders + (skill.getMaxLevel() > 10 ? stats.incAllskill : 0) + stats.getSkillIncrement(skill.getId()))));
    }

    public int getAllSkillLevels() {
        int rett = 0;
        for (Entry<Skill, SkillEntry> ret : skills.entrySet()) {
            if (!ret.getKey().isBeginnerSkill() && !ret.getKey().isSpecialSkill() && ret.getValue().skillevel > 0) {
                rett += ret.getValue().skillevel;
            }
        }
        return rett;
    }

    public long getSkillExpiry(final Skill skill) {
        if (skill == null) {
            return 0;
        }
        final SkillEntry ret = skills.get(skill);
        if (ret == null || ret.skillevel <= 0) {
            return 0;
        }
        return ret.expiration;
    }

    public int getSkillLevel(final Skill skill) {
        if (skill == null) {
            return 0;
        }
        final SkillEntry ret = skills.get(skill);
        if (ret == null || ret.skillevel <= 0) {
            return 0;
        }
        return ret.skillevel;
    }

    public byte getMasterLevel(final int skill) {
        return getMasterLevel(SkillFactory.getSkill(skill));
    }

    public byte getMasterLevel(final Skill skill) {
        final SkillEntry ret = skills.get(skill);
        if (ret == null) {
            return 0;
        }
        return ret.masterlevel;
    }

    public void maxskill(int i) {
        if (i != 3210) {
            MapleData data = MapleDataProviderFactory.getDataProvider(MapleDataProviderFactory.fileInWZPath("Skill.wz")).getData(StringUtil.getLeftPaddedStr("" + i, '0', 3) + ".img");
            byte maxLevel = 0;
            for (MapleData skill : data) {
                if (skill != null) {
                    for (MapleData skillId : skill.getChildren()) {
                        if (!skillId.getName().equals("icon")) {
                            maxLevel = (byte) MapleDataTool.getIntConvert("maxLevel", skillId.getChildByPath("common"), 0);
                            if (MapleDataTool.getIntConvert("invisible", skillId, 0) == 0) { //스킬창에 안보이는 스킬은 올리지않음
                                if (SkillFactory.getSkillName(Integer.parseInt(skillId.getName())) != "" || SkillFactory.getSkillName(Integer.parseInt(skillId.getName())) != null) {
                                    if (getLevel() >= MapleDataTool.getIntConvert("reqLev", skillId, 0)) {
                                        try {
                                            changeSkillLevel(SkillFactory.getSkill(Integer.parseInt(skillId.getName())), maxLevel, maxLevel);
                                        } catch (NumberFormatException ex) {
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            changeSkillLevel(SkillFactory.getSkill(32101000), (byte) 20, (byte) 20);
            changeSkillLevel(SkillFactory.getSkill(32101001), (byte) 20, (byte) 20);
            changeSkillLevel(SkillFactory.getSkill(32101002), (byte) 20, (byte) 20);
            changeSkillLevel(SkillFactory.getSkill(32101003), (byte) 20, (byte) 20);
            changeSkillLevel(SkillFactory.getSkill(32101004), (byte) 20, (byte) 20);
            changeSkillLevel(SkillFactory.getSkill(32101005), (byte) 20, (byte) 20);
            changeSkillLevel(SkillFactory.getSkill(32100006), (byte) 20, (byte) 20);
        }

    }

    public void levelUp() {
        if (GameConstants.isKOC(job)) {
            if (level <= 70) {
                remainingAp += 6;
            } else {
                remainingAp += 5;
            }
        } else {
            remainingAp += 5;
        }
        int maxhp = stats.getMaxHp();
        int maxmp = stats.getMaxMp();

        if (GameConstants.isBeginnerJob(job)) { // Beginner
            maxhp += Randomizer.rand(14, 18);
            maxmp += Randomizer.rand(12, 14);
        } else if (job >= 3100 && job <= 3112) { // Warrior
            maxhp += Randomizer.rand(28, 32);
        } else if ((job >= 100 && job <= 132) || (job >= 1100 && job <= 1111)) { // Warrior
            maxhp += Randomizer.rand(28, 32);
            maxmp += Randomizer.rand(4, 6);
        } else if ((job >= 200 && job <= 232) || (job >= 1200 && job <= 1211)) { // Magician
            maxhp += Randomizer.rand(14, 18);
            maxmp += Randomizer.rand(48, 52);
        } else if (job >= 3200 && job <= 3212) { //battle mages get their own little neat thing
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(42, 44);
        } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 1300 && job <= 1311) || (job >= 1400 && job <= 1411) || (job >= 3300 && job <= 3312) || (job >= 2300 && job <= 2312)) { // Bowman, Thief, Wind Breaker and Night Walker
            maxhp += Randomizer.rand(24, 28);
            maxmp += Randomizer.rand(14, 16);
        } else if ((job >= 510 && job <= 512) || (job >= 1510 && job <= 1512)) { // Pirate
            maxhp += Randomizer.rand(26, 30);
            maxmp += Randomizer.rand(18, 22);
        } else if ((job >= 500 && job <= 532) || (job >= 3500 && job <= 3512) || job == 1500) { // Pirate
            maxhp += Randomizer.rand(24, 28);
            maxmp += Randomizer.rand(18, 22);
        } else if (job >= 2100 && job <= 2112) { // Aran
            maxhp += Randomizer.rand(32, 36);
            maxmp += Randomizer.rand(4, 6);
        } else if (job >= 2200 && job <= 2218) { // Evan
            maxhp += Randomizer.rand(14, 18);
            maxmp += Randomizer.rand(50, 52);
        } else { // GameMaster
            maxhp += Randomizer.rand(50, 100);
            maxmp += Randomizer.rand(50, 100);
        }
        maxmp += stats.getTotalInt() / 10;

        if ((getTotalSkillLevel(1000001) > 0)) {
            getStat().maxhp += SkillFactory.getSkill(1000001).getEffect(getTotalSkillLevel(1000001)).getX();
        }
        if ((getTotalSkillLevel(2000001) > 0)) {
            getStat().maxmp += SkillFactory.getSkill(2000001).getEffect(getTotalSkillLevel(2000001)).getX();
        }

        exp -= getNeededExp();
        level += 1;
        if (level == 8) {
            addGuildMember(44); // 길드 값 //초보자길드
            if (this.getGuildId() != 0) {
                dropMessage(5, "[안내] 초보자 길드에 가입되었습니다.");
            } else {
                dropMessage(5, "[안내] 초보자 길드가 가득찼습니다.");
            }
        }
        if (level == 10) {
            gainItem(2430098, (short) 1);
            getClient().removeClickedNPC();
            NPCScriptManager.getInstance().dispose(getClient());
            getClient().getSession().write(MaplePacketCreator.getNPCTalk(9010019, (byte) 0, ("#bLv.10#k 달성 #r축하#k 선물 입니다."), "00 00", (byte) 0));
        }
        if (level == 30) {
            gainItem(2430099, (short) 1);
            getClient().removeClickedNPC();
            NPCScriptManager.getInstance().dispose(getClient());
            getClient().getSession().write(MaplePacketCreator.getNPCTalk(9010019, (byte) 0, ("#bLv.30#k 달성 #r축하#k 선물 입니다."), "00 00", (byte) 0));
        }
        if ((level == 51 && getUltimate() == 1) || (level == 51 && getUltimate() == 2)) {
            gainItem(2430098, (short) 1);//10박스
            gainItem(2430099, (short) 1);//30박스
            gainItem(5010813, (short) 1);//궁극의 모험가 소울
            gainItem(2450018, (short) 5);//경두5
            gainItem(2022694, (short) 5);//드두5
            addGuildMember(44);
            getClient().removeClickedNPC();
            NPCScriptManager.getInstance().dispose(getClient());
            getClient().getSession().write(MaplePacketCreator.getNPCTalk(9010019, (byte) 0, ("#b궁극의 모험가#k 생성을 축하드립니다!."), "00 00", (byte) 0));
        }
        if (level >= 10 && this.getGuildId() > 0) {
            //World.Guild.gainGP(this.getGuildId(), level * 2, this.getId());
//World.Guild.guildPacket(getGuildId(), MaplePacketCreator.serverNotice(5,"<길드>" + getName() + "님이 " + (getLevel() - 0) + "레벨을 달성하셨습니다."));            
        }
        if (level == 120 && GameConstants.isKOC(job)) {
            if (!isGM()) {
                final StringBuilder sb = new StringBuilder("[축하] ");
                final Item medal = getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -21);
                if (medal != null) { // Medal
                    sb.append("<");
                    sb.append(MapleItemInformationProvider.getInstance().getName(medal.getItemId()));
                    sb.append("> ");
                }
                sb.append(getName());
                sb.append("님이 레벨 120을 달성했습니다. 모두 축하해 주세요.");
                World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, sb.toString()));
                int map = 130000101;
                int npc = 9901800;
                MapleMap nmap = getClient().getChannelServer().getMapFactory().getMap(map);
                int count = 0;
                for (PlayerNPC pnpc : getClient().getChannelServer().getAllPlayerNPC()) {
                    if (pnpc.getMapId() == map) {
                        ++count;
                    }
                }
                if (count < 50) {
                    npc += count;
                    MapleNPC npctemplate = nmap.getNPCById(npc);
                    if (null != npctemplate) {
                        PlayerNPC newpnpc = new PlayerNPC(this, npc, nmap, npctemplate.getTruePosition().x, npctemplate.getTruePosition().y, npctemplate.getF(), npctemplate.getFh());
                        newpnpc.addToServer();
                        PlayerNPC.sendBroadcastModifiedNPC(this, nmap, npc, false);
                        //dropMessage(1, "만렙 축하의 의미로 자신의 NPC가 기사의 전당에 생겨났습니다!");
                        //dropMessage(5, "만렙 축하의 의미로 자신의 NPC가 기사의 전당에 생겨났습니다!");
                    }
                }
            }
        }

        if (level == 200) {
            if (!isGM()) {
                final StringBuilder sb = new StringBuilder("[축하] ");
                final Item medal = getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -21);
                if (medal != null) { // Medal
                    sb.append("<");
                    sb.append(MapleItemInformationProvider.getInstance().getName(medal.getItemId()));
                    sb.append("> ");
                }
                sb.append(getName());
                sb.append("님이 레벨 200을 달성했습니다. 모두 축하해 주세요.");
                World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, sb.toString()));
                int map = 0;
                switch (getJob() / 100) {
                    case 1:
                        map = 102000004; //전사의 전당(1) 8명
                        break;
                    case 2:
                        map = 101000004; //마법사의 전당(1) 8명
                        break;
                    case 3:
                        map = 100000205; //궁수의 전당
                        break;
                    case 4:
                        map = 103000009; //도적의 전당
                        break;
                    case 5:
                        map = 120000105; //해적의 전당
                        break;
                    case 21:
                        map = 140010110; //기사의 전각
                        break;
                    case 22:
                        map = 100030301; //울창한 전당
                        break;
                    case 32:
                        map = 310010000; //비밀 광장
                        break;
                    case 33:
                        map = 310010000; //비밀 광장
                        break;
                    case 35:
                        map = 310010000; //비밀 광장
                        break;
                }
                int npc = 9901000;
                npc += ((getJob() / 100) - 1) * 100;
                if (getJob() / 100 == 1) {
                    npc += 1;
                } else if (getJob() / 100 == 3) {
                    npc += 520;
                } else if (getJob() / 100 == 4) {
                    npc += 430;
                } else if (getJob() / 100 == 5) {
                    npc += 340;
                } else if (getJob() / 100 == 21) {
                    npc -= 1400;
                } else if (getJob() / 100 == 22) {
                    npc -= 1190;
                } else if (getJob() / 100 == 32) {
                    npc -= 1190;
                } else if (getJob() / 100 == 33) {
                    npc -= 1190;
                } else if (getJob() / 100 == 35) {
                    npc -= 1190;
                }
                MapleMap nmap = getClient().getChannelServer().getMapFactory().getMap(map);
                int count = 0;
                for (PlayerNPC pnpc : getClient().getChannelServer().getAllPlayerNPC()) {
                    if (pnpc.getMapId() == map) {
                        ++count;
                    }
                }
                if (count < 10) {
                    npc += count;
                    MapleNPC npctemplate = nmap.getNPCById(npc);
                    if (null != npctemplate) {
                        PlayerNPC newpnpc = new PlayerNPC(this, npc, nmap, npctemplate.getTruePosition().x, npctemplate.getTruePosition().y, npctemplate.getF(), npctemplate.getFh());
                        newpnpc.addToServer();
                        PlayerNPC.sendBroadcastModifiedNPC(this, nmap, npc, false);
                        String mapString = "";
                        switch (getJob() / 100) {
                            case 1:
                                mapString = "전사의 전당";
                                break;
                            case 2:
                                mapString = "마법사의 전당";
                                break;
                            case 3:
                                mapString = "궁수의 전당";
                                break;
                            case 4:
                                mapString = "도적의 전당";
                                break;
                            case 5:
                                mapString = "해적의 전당";
                                break;
                            case 21:
                                mapString = "달인의 전각";
                                break;
                            case 22:
                                mapString = "울창한 전당";
                                break;
                            case 32:
                                mapString = "비밀 광장";
                                break;
                            case 33:
                                mapString = "비밀 광장";
                                break;
                            case 35:
                                mapString = "비밀 광장";
                                break;
                        }
                        dropMessage(1, "자신의 NPC가 " + mapString + "에 생겨났습니다!");
                        //dropMessage(5, "만렙 축하의 의미로 자신의 NPC가 " + mapString + "에 생겨났습니다!");
                    }
                }
            }
        }
        if (level == 300) {
            if (!isGM()) {
                final StringBuilder sb = new StringBuilder("[축하] ");
                final Item medal = getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -21);
                if (medal != null) { // Medal
                    sb.append("<");
                    sb.append(MapleItemInformationProvider.getInstance().getName(medal.getItemId()));
                    sb.append("> ");
                }
                sb.append(getName());
                sb.append("님이 레벨 300을 달성했습니다. 모두 축하해 주세요.");
                World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, sb.toString()));
            }
        }
        if (level == 400) {
            if (!isGM()) {
                final StringBuilder sb = new StringBuilder("[축하] ");
                final Item medal = getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -21);
                if (medal != null) { // Medal
                    sb.append("<");
                    sb.append(MapleItemInformationProvider.getInstance().getName(medal.getItemId()));
                    sb.append("> ");
                }
                sb.append(getName());
                sb.append("님이 레벨 400을 달성했습니다. 모두 축하해 주세요.");
                World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, sb.toString()));
            }
        }
        if (level == 500) {
            if (!isGM()) {
                final StringBuilder sb = new StringBuilder("[축하] ");
                final Item medal = getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -21);
                if (medal != null) { // Medal
                    sb.append("<");
                    sb.append(MapleItemInformationProvider.getInstance().getName(medal.getItemId()));
                    sb.append("> ");
                }
                sb.append(getName());
                sb.append("님이 레벨 500을 달성했습니다. 모두 축하해 주세요.");
                World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, sb.toString()));
            }
        }
        if (level == 600) {
            if (!isGM()) {
                final StringBuilder sb = new StringBuilder("[축하] ");
                final Item medal = getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -21);
                if (medal != null) { // Medal
                    sb.append("<");
                    sb.append(MapleItemInformationProvider.getInstance().getName(medal.getItemId()));
                    sb.append("> ");
                }
                sb.append(getName());
                sb.append("님이 레벨 600을 달성했습니다. 모두 축하해 주세요.");
                World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, sb.toString()));
            }
        }
        if (level == 700) {
            if (!isGM()) {
                final StringBuilder sb = new StringBuilder("[축하] ");
                final Item medal = getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -21);
                if (medal != null) { // Medal
                    sb.append("<");
                    sb.append(MapleItemInformationProvider.getInstance().getName(medal.getItemId()));
                    sb.append("> ");
                }
                sb.append(getName());
                sb.append("님이 레벨 700을 달성했습니다. 모두 축하해 주세요.");
                World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, sb.toString()));
            }
        }
        if (level == 800) {
            if (!isGM()) {
                final StringBuilder sb = new StringBuilder("[축하] ");
                final Item medal = getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -21);
                if (medal != null) { // Medal
                    sb.append("<");
                    sb.append(MapleItemInformationProvider.getInstance().getName(medal.getItemId()));
                    sb.append("> ");
                }
                sb.append(getName());
                sb.append("님이 레벨 800을 달성했습니다. 모두 축하해 주세요.");
                World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, sb.toString()));
            }
        }
        if (level == 900) {
            if (!isGM()) {
                final StringBuilder sb = new StringBuilder("[축하] ");
                final Item medal = getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -21);
                if (medal != null) { // Medal
                    sb.append("<");
                    sb.append(MapleItemInformationProvider.getInstance().getName(medal.getItemId()));
                    sb.append("> ");
                }
                sb.append(getName());
                sb.append("님이 레벨 900을 달성했습니다. 모두 축하해 주세요.");
                World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, sb.toString()));
            }
        }
        if (level == 999) {
            if (!isGM()) {
                final StringBuilder sb = new StringBuilder("[축하] ");
                final Item medal = getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -21);
                if (medal != null) { // Medal
                    sb.append("<");
                    sb.append(MapleItemInformationProvider.getInstance().getName(medal.getItemId()));
                    sb.append("> ");
                }
                sb.append(getName());
                sb.append("님이 레벨 999을 달성했습니다. 모두 축하해 주세요.");
                World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, sb.toString()));
            }
        }
        if (level == 30) {
            handling.channel.handler.DueyHandler.addNewItemToDb(2431010, 1, getId(), "30레벨", "레벨30 달성", true);    //마일1000 
            dropMessage(5, "레벨30 달성 아이템이 지급되었습니다. NPC 택배원 <듀이> 에게서 아이템을 수령하세요!");
        }
        if (level == 70) {
            handling.channel.handler.DueyHandler.addNewItemToDb(2431011, 1, getId(), "70레벨", "레벨70 달성", true);  //마일3000
            handling.channel.handler.DueyHandler.addNewItemToDb(1902000, 1, getId(), "70레벨", "레벨70 달성", true);  //멧돼지
            dropMessage(5, "레벨70 달성 아이템이 지급되었습니다. NPC 택배원 <듀이> 에게서 아이템을 수령하세요!");
        }
        if (level == 120) {
            handling.channel.handler.DueyHandler.addNewItemToDb(2431012, 1, getId(), "120레벨", "레벨120 달성", true);     //마일5000
            handling.channel.handler.DueyHandler.addNewItemToDb(1902001, 1, getId(), "120레벨", "레벨120 달성", true);    //은빛갈기
            dropMessage(5, "레벨120 달성 아이템이 지급되었습니다. NPC 택배원 <듀이> 에게서 아이템을 수령하세요!");
        }
        if (level == 200) {
            handling.channel.handler.DueyHandler.addNewItemToDb(2431012, 2, getId(), "200레벨", "레벨200 달성", true);  //마일10000
            handling.channel.handler.DueyHandler.addNewItemToDb(1902002, 1, getId(), "200레벨", "레벨200 달성", true);  //레드 드라코
            dropMessage(5, "레벨200 달성 아이템이 지급되었습니다. NPC 택배원 <듀이> 에게서 아이템을 수령하세요!");
        }
        if (level == 300) {
            handling.channel.handler.DueyHandler.addNewItemToDb(2431012, 4, getId(), "300레벨", "레벨300 달성", true);  //마일20000
            handling.channel.handler.DueyHandler.addNewItemToDb(1902048, 1, getId(), "300레벨", "레벨300 달성", true);    //하이에나
            handling.channel.handler.DueyHandler.addNewItemToDb(1912041, 1, getId(), "300레벨", "레벨300 달성", true);    //하이에나안장
            handling.channel.handler.DueyHandler.addNewItemToDb(1142092, 1, getId(), "300레벨", "레벨300 달성", true);    //노력의모험가
            dropMessage(5, "레벨300 달성 아이템이 지급되었습니다. NPC 택배원 <듀이> 에게서 아이템을 수령하세요!");
        }
        if (level == 400) {
            handling.channel.handler.DueyHandler.addNewItemToDb(1142093, 1, getId(), "400레벨", "레벨400 달성", true);    //긍지의모험가
            dropMessage(5, "레벨400 달성 아이템이 지급되었습니다. NPC 택배원 <듀이> 에게서 아이템을 수령하세요!");
        }
        if (level == 500) {
            handling.channel.handler.DueyHandler.addNewItemToDb(1142094, 1, getId(), "500레벨", "레벨500 달성", true);    //용기의모험가
            dropMessage(5, "레벨500 달성 아이템이 지급되었습니다. NPC 택배원 <듀이> 에게서 아이템을 수령하세요!");
        }
        if (level == 600) {
            handling.channel.handler.DueyHandler.addNewItemToDb(1142095, 1, getId(), "600레벨", "레벨600 달성", true);    //화랑의모험가
            dropMessage(5, "레벨600 달성 아이템이 지급되었습니다. NPC 택배원 <듀이> 에게서 아이템을 수령하세요!");
        }
        if (level == 700) {
            handling.channel.handler.DueyHandler.addNewItemToDb(1142096, 1, getId(), "700레벨", "레벨700 달성", true);    //태극의모험가
            dropMessage(5, "레벨700 달성 아이템이 지급되었습니다. NPC 택배원 <듀이> 에게서 아이템을 수령하세요!");
        }
        if (level == 800) {
            handling.channel.handler.DueyHandler.addNewItemToDb(1142097, 1, getId(), "800레벨", "레벨800 달성", true);    //천하의모험가
            dropMessage(5, "레벨800 달성 아이템이 지급되었습니다. NPC 택배원 <듀이> 에게서 아이템을 수령하세요!");
        }
        if (level == 900) {
            handling.channel.handler.DueyHandler.addNewItemToDb(1142098, 1, getId(), "900레벨", "레벨900 달성", true);    //전설의모험가
            dropMessage(5, "레벨900 달성 아이템이 지급되었습니다. NPC 택배원 <듀이> 에게서 아이템을 수령하세요!");
        }
        if (level == 999) {
            handling.channel.handler.DueyHandler.addNewItemToDb(1142099, 1, getId(), "999레벨", "레벨999 달성", true);    //빛의모험가
            dropMessage(5, "레벨999 달성 아이템이 지급되었습니다. NPC 택배원 <듀이> 에게서 아이템을 수령하세요!");
        }

        maxhp = Math.min(99999, Math.abs(maxhp));
        maxmp = Math.min(99999, Math.abs(maxmp));

        final Map<MapleStat, Integer> statup = new EnumMap<MapleStat, Integer>(MapleStat.class);

        statup.put(MapleStat.MAXHP, maxhp);
        statup.put(MapleStat.MAXMP, maxmp);
        statup.put(MapleStat.HP, maxhp);
        statup.put(MapleStat.MP, maxmp);
        statup.put(MapleStat.EXP, exp);
        statup.put(MapleStat.LEVEL, (int) level);

        if (isGM() || !GameConstants.isBeginnerJob(job)) { // Not Beginner, Nobless and Legend
            if (GameConstants.isResist(this.job) || GameConstants.isMercedes(this.job)) {//레지스탕스 스킬포인트
                remainingSp[GameConstants.getSkillBook(this.job, this.level)] += 3;
            } else {
                remainingSp[GameConstants.getSkillBook(this.job)] += 3;
            }
            client.getSession().write(MaplePacketCreator.updateSp(this, false));
        } else {
            if (level <= 10) { //초보자는 올힘
                stats.str += remainingAp;
                remainingAp = 0;
                statup.put(MapleStat.STR, (int) stats.getStr());
            } else if (level == 11) {
                resetStats(4, 4, 4, 4);
            }
        }
        statup.put(MapleStat.AVAILABLEAP, (int) remainingAp);
        stats.setInfo(maxhp, maxmp, maxhp, maxmp);
        client.getSession().write(MaplePacketCreator.updatePlayerStats(statup, getJob()));
        map.broadcastMessage(this, MaplePacketCreator.showForeignEffect(getId(), 0), false);
        stats.recalcLocalStats(this);
        healMaxHPMP();
        silentPartyUpdate();
        //  guildUpdate();
        familyUpdate();

        if (GameConstants.isKOC(getJob()) && getLevel() == 120) {
            if (getJob() == 1111) {
                changeJob(1112);
            }
            if (getJob() == 1211) {
                changeJob(1212);
            }
            if (getJob() == 1311) {
                changeJob(1312);
            }
            if (getJob() == 1411) {
                changeJob(1412);
            }
            if (getJob() == 1511) {
                changeJob(1512);
            }
        }

        if (getLevel() == 60 || getLevel() == 70 || getLevel() == 80 || getLevel() == 90) {
            if ((getUltimate() == 2) || (getUltimate() == 1)) {
                if (getJob() >= 100 && getJob() <= 132) {
                    final Skill skill = SkillFactory.getSkill(1075);
                    if (skill != null && getTotalSkillLevel(skill) != 0 && getTotalSkillLevel(skill) < 5) {
                        changeSkillLevel(skill, getTotalSkillLevel(skill) + 1, (byte) (getTotalSkillLevel(skill) + 1));
                        dropMessage(-1, "<미하일의 소울 드라이버> 스킬 레벨이 1 상승 하였습니다.");
                    }
                }
                if (getJob() >= 200 && getJob() <= 232) {
                    final Skill skill = SkillFactory.getSkill(1074);
                    if (skill != null && getTotalSkillLevel(skill) != 0 && getTotalSkillLevel(skill) < 5) {
                        changeSkillLevel(skill, getTotalSkillLevel(skill) + 1, (byte) (getTotalSkillLevel(skill) + 1));
                        dropMessage(-1, "<오즈의 키신> 스킬 레벨이 1 상승 하였습니다.");
                    }
                }
                if (getJob() >= 300 && getJob() <= 322) {
                    final Skill skill = SkillFactory.getSkill(1077);
                    if (skill != null && getTotalSkillLevel(skill) != 0 && getTotalSkillLevel(skill) < 5) {
                        changeSkillLevel(skill, getTotalSkillLevel(skill) + 1, (byte) (getTotalSkillLevel(skill) + 1));
                        dropMessage(-1, "<이리나의 윈드 피어싱> 스킬 레벨이 1 상승 하였습니다.");
                    }
                }
                if (getJob() >= 400 && getJob() <= 434) {
                    final Skill skill = SkillFactory.getSkill(1078);
                    if (skill != null && getTotalSkillLevel(skill) != 0 && getTotalSkillLevel(skill) < 5) {
                        changeSkillLevel(skill, getTotalSkillLevel(skill) + 1, (byte) (getTotalSkillLevel(skill) + 1));
                        dropMessage(-1, "<이카르트의 뱀파이어> 스킬 레벨이 1 상승 하였습니다.");
                    }
                }
                if (getJob() >= 500 && getJob() <= 522) {
                    final Skill skill = SkillFactory.getSkill(1079);
                    if (skill != null && getTotalSkillLevel(skill) != 0 && getTotalSkillLevel(skill) < 5) {
                        changeSkillLevel(skill, getTotalSkillLevel(skill) + 1, (byte) (getTotalSkillLevel(skill) + 1));
                        dropMessage(-1, "<호크아이의 샤크 웨이브> 스킬 레벨이 1 상승 하였습니다.");
                    }
                }
            }
        }

        //FileoutputUtil.log(FileoutputUtil.LevelUp_Log, "" + getName() + "님이 " + getMap().getMapName() + "맵에서 " + getLevel() + "레벨을 달성 // " + CurrentReadable_Time() + "");        
    }

    public void changeKeybinding(int key, byte type, int action) {
        if (type != 0) {
            keylayout.Layout().put(Integer.valueOf(key), new Pair<Byte, Integer>(type, action));
        } else {
            keylayout.Layout().remove(Integer.valueOf(key));
        }
    }

    public void addGuildMember(int gid) {
        if (getGuildId() > 0) {
            return; // 있으면 사라져
        } else {
            int guildId = gid; // 외부에서 받아온 길드 아이디 인자를 내부 변수 guildId에 넣음
            int cid = getId();

            if (cid != getId()) {
                return;
            } else {
                setGuildId(guildId); // gulidId에 지정된 길드아이디로 플레이어를 가입시킴
                setGuildRank((byte) 5);
                int s = World.Guild.addGuildMember(getMGC());
                if (s == 0) {
                    dropMessage(1, "가입하려는 길드는 이미 최대 인원으로 가득 찼습니다."); // 사라져..
                    setGuildId(0);
                    return; // ㅂㅂ
                }
                getClient().getSession().write(MaplePacketCreator.showGuildInfo(this));
                final MapleGuild gs = World.Guild.getGuild(guildId);
                for (byte[] pack : World.Alliance.getAllianceInfo(gs.getAllianceId(), true)) {
                    if (pack != null) {
                        getClient().getSession().write(pack);
                    }
                }
                saveGuildStatus();
                respawnPlayer(this);
            }
        }
    }

    private static final void respawnPlayer(final MapleCharacter mc) {
        if (mc.getMap() == null) {
            return;
        }
        mc.getMap().broadcastMessage(MaplePacketCreator.loadGuildName(mc));
        mc.getMap().broadcastMessage(MaplePacketCreator.loadGuildIcon(mc));
    }

    public void sendMacros() {
        for (int i = 0; i < 5; i++) {
            if (skillMacros[i] != null) {
                client.getSession().write(MaplePacketCreator.getMacros(skillMacros));
                break;
            }
        }
    }

    public void updateMacros(int position, SkillMacro updateMacro) {
        skillMacros[position] = updateMacro;
        changed_skillmacros = true;
    }

    public final SkillMacro[] getMacros() {
        return skillMacros;
    }

    public void tempban(String reason, Calendar duration, int greason, boolean IPMac, String banby) {
        if (IPMac) {
            client.banHwID();
        }
//        client.getSession().write(MaplePacketCreator.GMPoliceMessage());
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            if (IPMac) {
                ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                ps.setString(1, client.getIp());
                ps.execute();
                ps.close();
            }

            ps = con.prepareStatement("UPDATE accounts SET tempban = ?, banreason = ?, greason = ?, banby = ? WHERE id = ?");
            Timestamp TS = new Timestamp(duration.getTimeInMillis());
            ps.setTimestamp(1, TS);
            ps.setString(2, reason);
            ps.setInt(3, greason);
            ps.setString(4, banby);
            ps.setInt(5, accountid);
            ps.execute();
            ps.close();
        } catch (SQLException ex) {
            System.err.println("Error while tempbanning" + ex);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }
        }

    }

    public static boolean tempban(String reason, Calendar duration, int greason, String banby, String name) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            PreparedStatement ps;

            int accid = MapleCharacterUtil.getAccIdByName(name);
            if (accid == -1) {
                return false;
            }

            ps = con.prepareStatement("UPDATE accounts SET tempban = ?, banreason = ?, greason = ?, banby = ? WHERE id = ?");
            Timestamp TS = new Timestamp(duration.getTimeInMillis());
            ps.setTimestamp(1, TS);
            ps.setString(2, reason);
            ps.setInt(3, greason);
            ps.setString(4, banby);
            ps.setInt(5, accid);
            ps.execute();
            ps.close();
        } catch (SQLException ex) {
            System.err.println("Error while tempbanning" + ex);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }
        }
        return true;
    }

    public final boolean ban(String reason, boolean IPMac, boolean autoban, boolean hellban, String banby) {
        if (lastmonthfameids == null) {
            throw new RuntimeException("Trying to ban a non-loaded character (testhack)");
        }

        Connection con = null;

        client.getSession().write(MaplePacketCreator.GMPoliceMessage());
        try {
            con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET banned = ?, banreason = ?, banby = ? WHERE id = ?");
            ps.setInt(1, autoban ? 2 : 1);
            ps.setString(2, reason);
            ps.setString(3, banby);
            ps.setInt(4, accountid);
            ps.execute();
            ps.close();

            if (IPMac) {
                client.banHwID();
                ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                ps.setString(1, client.getSessionIPAddress());
                ps.execute();
                ps.close();

                if (hellban) {
                    PreparedStatement psa = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
                    psa.setInt(1, accountid);
                    ResultSet rsa = psa.executeQuery();
                    if (rsa.next()) {
                        PreparedStatement pss = con.prepareStatement("UPDATE accounts SET banned = ?, banreason = ?, banby = ? WHERE email = ? OR SessionIP = ?");
                        pss.setInt(1, autoban ? 2 : 1);
                        pss.setString(2, reason);
                        pss.setString(3, banby);
                        pss.setString(4, rsa.getString("email"));
                        pss.setString(5, client.getSessionIPAddress());
                        pss.execute();
                        pss.close();
                    }
                    rsa.close();
                    psa.close();

                }
            }
        } catch (SQLException ex) {
            System.err.println("Error while banning" + ex + " target : " + getName() + "(" + getId() + ")");
            return false;
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }
        }
        client.getSession().close(true);
        return true;
    }

    public static boolean ban(String id, String reason, boolean accountId, int gmlevel, boolean hellban, String banby) {
        int z = 0;
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            if (accountId) {
                ps = con.prepareStatement("SELECT id FROM accounts WHERE name = ?");
            } else {
                ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
            }
            boolean ret = false;
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                z = rs.getInt(1);
                PreparedStatement psb = con.prepareStatement("UPDATE accounts SET banned = 1, banreason = ?, banby = ? WHERE id = ? AND gm < ?");
                psb.setString(1, reason);
                psb.setString(2, banby);
                psb.setInt(3, z);
                psb.setInt(4, gmlevel);
                psb.execute();
                psb.close();

                if (gmlevel > 100) { //admin ban
                    PreparedStatement psa = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
                    psa.setInt(1, z);
                    ResultSet rsa = psa.executeQuery();
                    if (rsa.next()) {
                        String sessionIP = rsa.getString("sessionIP");
                        if (rsa.getString("macs") != null) {
                            MapleClient.banHwID(rsa.getString("macs"));
                        }
                        if (hellban) {
                            PreparedStatement pss = con.prepareStatement("UPDATE accounts SET banned = 1, banreason = ?, banby = ? WHERE email = ?" + (sessionIP == null ? "" : " OR SessionIP = ?"));
                            pss.setString(1, reason);
                            pss.setString(2, banby);
                            pss.setString(3, rsa.getString("email"));
                            if (sessionIP != null) {
                                pss.setString(4, sessionIP);
                            }
                            pss.execute();
                            pss.close();
                        }
                    }
                    rsa.close();
                    psa.close();
                }
                ret = true;
            }
            rs.close();
            ps.close();
            return ret;
        } catch (SQLException ex) {
            System.err.println("Error while banning" + ex + " target : " + id + "(" + z + ")");
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }
        }
        return false;
    }

    /**
     * Oid of players is always = the cid
     */
    @Override
    public int getObjectId() {
        return getId();
    }

    /**
     * Throws unsupported operation exception, oid of players is read only
     */
    @Override
    public void setObjectId(int id) {
        throw new UnsupportedOperationException();
    }

    public MapleStorage getStorage() {
        return storage;
    }

    public void addVisibleMapObject(MapleMapObject mo) {

        visibleMapObjectsLock.writeLock().lock();
        try {
            visibleMapObjects.add(mo);
        } finally {
            visibleMapObjectsLock.writeLock().unlock();
        }
    }

    public void removeVisibleMapObject(MapleMapObject mo) {

        visibleMapObjectsLock.writeLock().lock();
        try {
            visibleMapObjects.remove(mo);
        } finally {
            visibleMapObjectsLock.writeLock().unlock();
        }
    }

    public boolean isMapObjectVisible(MapleMapObject mo) {
        visibleMapObjectsLock.readLock().lock();
        try {
            return visibleMapObjects.contains(mo);
        } finally {
            visibleMapObjectsLock.readLock().unlock();
        }
    }

    public Collection<MapleMapObject> getAndWriteLockVisibleMapObjects() {
        visibleMapObjectsLock.writeLock().lock();
        return visibleMapObjects;
    }

    public void unlockWriteVisibleMapObjects() {
        visibleMapObjectsLock.writeLock().unlock();
    }

    public boolean isAlive() {
        return stats.getHp() > 0;
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.getSession().write(MaplePacketCreator.removePlayerFromMap(this.getObjectId()));
        //don't need this, client takes care of it
        /*if (dragon != null) {
         client.getSession().write(MaplePacketCreator.removeDragon(this.getId()));
         }
         if (android != null) {
         client.getSession().write(MaplePacketCreator.deactivateAndroid(this.getId()));
         }
         if (summonedFamiliar != null) {
         client.getSession().write(MaplePacketCreator.removeFamiliar(this.getId()));
         }*/
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        if (client.getPlayer().allowedToTarget(this)) {
            if (client.getPlayer().getParty() != null) {
                client.getPlayer().silentPartyUpdate();
                client.getPlayer().getClient().getSession().write(MaplePacketCreator.updateParty(client.getPlayer().getClient().getChannel(), client.getPlayer().getParty(), PartyOperation.SILENT_UPDATE, null));
                client.getPlayer().receivePartyMemberHP();
                client.getPlayer().updatePartyMemberHP();
            }
            client.getSession().write(MaplePacketCreator.spawnPlayerMapobject(this));
            sendTemporaryStats();//1126
            for (final MaplePet pet : pets) {
                if (pet.getSummoned()) {
                    pet.setPos(getPosition());
                    client.getSession().write(PetPacket.showPet(this, pet, false, false));
                    //dropMessage(5, "showpet");
                }
            }
            if (dragon != null) {
                client.getSession().write(MaplePacketCreator.spawnDragon(dragon));
            }

            if (summons != null && summons.size() > 0) {
                summonsLock.readLock().lock();
                try {
                    for (final MapleSummon summon : summons) {
                        if (client != getClient()) {
                            client.getSession().write(MaplePacketCreator.spawnSummon(summon, false));
                        }
                    }
                } finally {
                    summonsLock.readLock().unlock();
                }
            }
            if (followid > 0 && followon) {
                client.getSession().write(MaplePacketCreator.followEffect(followinitiator ? followid : id, followinitiator ? id : followid, null));
            }
        }
    }

    public final void equipChanged() {
        if (map == null) {
            return;
        }
        map.broadcastMessage(this, MaplePacketCreator.updateCharLook(this), false);
        stats.recalcLocalStats(this);
        if (getMessenger() != null) {
            World.Messenger.updateMessenger(getMessenger().getId(), getName(), client.getChannel());
        }
    }

    public final MaplePet getPet(final int index) {
        byte count = 0;
        for (final MaplePet pet : pets) {
            if (pet.getSummoned()) {
                if (count == index) {
                    return pet;
                }
                count++;
            }
        }
        return null;
    }

    public void removePetCS(MaplePet pet) {
        pets.remove(pet);
    }

//    public void addPet(final MaplePet pet) {
//        if (pets.contains(pet)) {
//            pets.remove(pet);
//        }
//        pets.add(pet);
//        // So that the pet will be at the last
//        // Pet index logic :(
//    }
    public void addPetz(final MaplePet pet) {
        if (pets.contains(pet)) {
            pets.remove(pet);
        }
        pets.add(pet);
        for (int i = 0; i < 3; ++i) {
            if (petz[i] == null) {
                petz[i] = pet;
                return;
            }
        }

        // So that the pet will be at the last
        // Pet index logic :(
    }

    public void removePet(MaplePet pet, boolean shiftLeft) {
        pet.setSummoned(0);
        int slot = -1;
        for (int i = 0; i < 3; i++) {
            if (petz[i] != null) {
                if (petz[i].getUniqueId() == pet.getUniqueId()) {
                    petz[i] = null;
                    slot = i;
                    break;
                }
            }
        }
        if (shiftLeft) {
            if (slot > -1) {
                for (int i = slot; i < 3; i++) {
                    if (i != 2) {
                        petz[i] = petz[i + 1];
                    } else {
                        petz[i] = null;
                    }
                }
            }
        }
    }

    public final byte getPetIndex(final MaplePet petz) {
        for (byte i = 0; i < 3; i++) {
            if (this.petz[i] != null) {
                if (this.petz[i].getUniqueId() == petz.getUniqueId()) {
                    return i;
                }
            }
        }
        return -1;
    }

    public final byte getPetIndex(final int petId) {
        byte count = 0;
        for (final MaplePet pet : pets) {
            if (pet.getSummoned()) {
                if (pet.getUniqueId() == petId) {
                    return count;
                }
                count++;
            }
        }
        return -1;
    }

    /*public final byte getPetIndex(final MaplePet petz) {
     byte count = 0;
     for (final MaplePet pet : pets) {
     if (pet.getSummoned()) {
     if (pet.getUniqueId() == petz.getUniqueId()) {
     return count;
     }
     count++;
     }
     }
     return -1;
     }

     public final byte getPetIndex(final int petId) {
     byte count = 0;
     for (final MaplePet pet : pets) {
     if (pet.getSummoned()) {
     if (pet.getUniqueId() == petId) {
     return count;
     }
     count++;
     }
     }
     return -1;
     }*/
    public final List<MaplePet> getSummonedPets() {
        List<MaplePet> ret = new ArrayList<MaplePet>();
        for (final MaplePet pet : pets) {
            if (pet.getSummoned()) {
                ret.add(pet);
            }
        }
        return ret;
    }

    public final byte getPetById(final int petId) {
        byte count = 0;
        for (final MaplePet pet : pets) {
            if (pet.getSummoned()) {
                if (pet.getPetItemId() == petId) {
                    return count;
                }
                count++;
            }
        }
        return -1;
    }

    public final List<MaplePet> getPets() {
        return pets;
    }

    public final void unequipAllPets() {
        for (final MaplePet pet : pets) {
            if (pet != null) {
                unequipPet(pet, true, false);
            }
        }
    }

    public void unequipPet(MaplePet pet, boolean shiftLeft, boolean hunger) {
        if (pet.getSummoned()) {
            pet.saveToDb();
            client.getSession().write(PetPacket.updatePet(pet, getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition()), false));
            if (map != null) {
                map.broadcastMessage(this, PetPacket.showPet(this, pet, true, hunger), true);
            }
            removePet(pet, shiftLeft);
            client.getSession().write(PetPacket.petStatUpdate(this));//1126
            //client.getSession().write(MaplePacketCreator.updatePlayerStats(Collections.singletonMap(MapleStat.PET, 0), 0));
            client.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    /*    public void shiftPetsRight() {
     if (pets[2] == null) {
     pets[2] = pets[1];
     pets[1] = pets[0];
     pets[0] = null;
     }
     }*/
    public final long getLastFameTime() {
        return lastfametime;
    }

    public final List<Integer> getFamedCharacters() {
        return lastmonthfameids;
    }

    public final List<Integer> getBattledCharacters() {
        return lastmonthbattleids;
    }

    public FameStatus canGiveFame(MapleCharacter from) {
        if (lastfametime >= System.currentTimeMillis() - 60 * 60 * 24 * 1000) {
            return FameStatus.NOT_TODAY;
        } else if (from == null || lastmonthfameids == null || lastmonthfameids.contains(Integer.valueOf(from.getId()))) {
            return FameStatus.NOT_THIS_MONTH;
        }
        return FameStatus.OK;
    }

    public void hasGivenFame(MapleCharacter to) {
        lastfametime = System.currentTimeMillis();
        lastmonthfameids.add(Integer.valueOf(to.getId()));
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("INSERT INTO famelog (characterid, characterid_to) VALUES (?, ?)");
            ps.setInt(1, getId());
            ps.setInt(2, to.getId());
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            System.err.println("ERROR writing famelog for char " + getName() + " to " + to.getName() + e);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public boolean canBattle(MapleCharacter to) {
        if (to == null || lastmonthbattleids == null || lastmonthbattleids.contains(Integer.valueOf(to.getAccountID()))) {
            return false;
        }
        return true;
    }

    public void hasBattled(MapleCharacter to) {
        lastmonthbattleids.add(Integer.valueOf(to.getAccountID()));
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("INSERT INTO battlelog (accid, accid_to) VALUES (?, ?)");
            ps.setInt(1, getAccountID());
            ps.setInt(2, to.getAccountID());
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            System.err.println("ERROR writing battlelog for char " + getName() + " to " + to.getName() + e);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public final MapleKeyLayout getKeyLayout() {
        return this.keylayout;
    }

    public MapleParty getParty() {
        if (party == null) {
            return null;
        } else if (party.isDisbanded()) {
            party = null;
        }
        return party;
    }

    public byte getWorld() {
        return world;
    }

    public void setWorld(byte world) {
        this.world = world;
    }

    public void setParty(MapleParty party) {
        this.party = party;
    }

    public MapleTrade getTrade() {
        return trade;
    }

    public void setTrade(MapleTrade trade) {
        this.trade = trade;
    }

    public EventInstanceManager getEventInstance() {
        return eventInstance;
    }

    public void setEventInstance(EventInstanceManager eventInstance) {
        this.eventInstance = eventInstance;
    }

    public void addDoor(MapleDoor door) {
        doors.add(door);
    }

    public void clearDoors() {
        doors.clear();
    }

    public List<MapleDoor> getDoors() {
        return new ArrayList<MapleDoor>(doors);
    }

    public void addMechDoor(MechDoor door) {

        mechDoors.add(door);
    }

    public void clearMechDoors() {
        mechDoors.clear();
    }

    public List<MechDoor> getMechDoors() {

        return new ArrayList<MechDoor>(mechDoors);
    }

    public void setSmega() {
        if (smega) {
            smega = false;
            dropMessage(5, "You have set megaphone to disadropMbled mode");
        } else {
            smega = true;
            dropMessage(5, "You have set megaphone to enabled mode");
        }
    }

    public boolean getSmega() {
        return smega;
    }

    public List<MapleSummon> getSummonsReadLock() {
        summonsLock.readLock().lock();
        return summons;
    }

    public int getSummonsSize() {
        return summons.size();
    }

    public void unlockSummonsReadLock() {
        summonsLock.readLock().unlock();
    }

    public void addSummon(MapleSummon s) {
        summonsLock.writeLock().lock();
        try {
            summons.add(s);
        } finally {
            summonsLock.writeLock().unlock();
        }
    }

    public void removeSummon(MapleSummon s) {
        summonsLock.writeLock().lock();
        try {
            summons.remove(s);
        } finally {
            summonsLock.writeLock().unlock();
        }
    }

    public int getChair() {
        return chair;
    }

    public int getItemEffect() {
        return itemEffect;
    }

    public void setChair(int chair) {
        this.chair = chair;
        stats.relocHeal(this);
    }

    public void setItemEffect(int itemEffect) {
        this.itemEffect = itemEffect;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.PLAYER;
    }

    public int getFamilyId() {
        if (mfc == null) {
            return 0;
        }
        return mfc.getFamilyId();
    }

    public int getSeniorId() {
        if (mfc == null) {
            return 0;
        }
        return mfc.getSeniorId();
    }

    public int getJunior1() {
        if (mfc == null) {
            return 0;
        }
        return mfc.getJunior1();
    }

    public int getJunior2() {
        if (mfc == null) {
            return 0;
        }
        return mfc.getJunior2();
    }

    public int getCurrentRep() {
        return currentrep;
    }

    public int getTotalRep() {
        return totalrep;
    }

    public void setCurrentRep(int _rank) {
        currentrep = _rank;
        if (mfc != null) {
            mfc.setCurrentRep(_rank);
        }
    }

    public void setTotalRep(int _rank) {
        totalrep = _rank;
        if (mfc != null) {
            mfc.setTotalRep(_rank);
        }
    }

    public int getTotalWins() {
        return totalWins;
    }

    public int getTotalLosses() {
        return totalLosses;
    }

    public void increaseTotalWins() {
        totalWins++;
    }

    public void increaseTotalLosses() {
        totalLosses++;
    }

    public int getGuildId() {
        return guildid;
    }

    public byte getGuildRank() {
        return guildrank;
    }

    public int getGuildContribution() {
        return guildContribution;
    }

    public void setGuildId(int _id) {
        guildid = _id;
        if (guildid > 0) {
            if (mgc == null) {
                mgc = new MapleGuildCharacter(this);
            } else {
                mgc.setGuildId(guildid);
            }
        } else {
            mgc = null;
            guildContribution = 0;
        }
    }

    public void setGuildRank(byte _rank) {
        guildrank = _rank;
        if (mgc != null) {
            mgc.setGuildRank(_rank);
        }
    }

    public void setGuildContribution(int _c) {
        this.guildContribution = _c;
        if (mgc != null) {
            mgc.setGuildContribution(_c);
        }
    }

    public MapleGuildCharacter getMGC() {
        return mgc;
    }

    public void setAllianceRank(byte rank) {
        allianceRank = rank;
        if (mgc != null) {
            mgc.setAllianceRank(rank);
        }
    }

    public byte getAllianceRank() {
        return allianceRank;
    }

    public MapleGuild getGuild() {
        if (getGuildId() <= 0) {
            return null;
        }
        return World.Guild.getGuild(getGuildId());
    }

    public void getGuildInfo(int id) {
        for (ChannelServer cserv : ChannelServer.getAllInstances()) {
            for (MapleCharacter mch : cserv.getPlayerStorage().getAllCharacters()) {
                if (mch != null && mch.getGuildId() == id) {
                    mch.getClient().getSession().write(MaplePacketCreator.showGuildInfo(mch));
                }
            }
        }
    }

    public void setJob(int j) {
        this.job = (short) j;
    }

    public void sidekickUpdate() {
        if (sidekick == null) {
            return;
        }
        sidekick.getCharacter(sidekick.getCharacter(0).getId() == getId() ? 0 : 1).update(this);

        if (!sidekick.checkLevels(getLevel(), sidekick.getCharacter(sidekick.getCharacter(0).getId() == getId() ? 1 : 0).getLevel())) {
            sidekick.eraseToDB();
        }
    }

    public void guildUpdate() {
        if (guildid <= 0) {
            return;
        }
        mgc.setLevel((short) level);
        mgc.setJobId(job);
        World.Guild.memberLevelJobUpdate(mgc);
    }

    public void saveGuildStatus() {
        MapleGuild.setOfflineGuildStatus(guildid, guildrank, guildContribution, allianceRank, id);
    }

    public void healMaxHPMP() {
        Map<MapleStat, Integer> statups = new EnumMap<>(MapleStat.class);
        stats.setHp(stats.getCurrentMaxHp(), this);
        statups.put(MapleStat.HP, stats.getCurrentMaxHp());
        stats.setMp(stats.getCurrentMaxMp(), this);
        statups.put(MapleStat.MP, stats.getCurrentMaxMp());
        client.getSession().write(MaplePacketCreator.updatePlayerStats(statups, getJob()));
    }

    public void familyUpdate() {
        if (mfc == null) {
            return;
        }
        World.Family.memberFamilyUpdate(mfc, this);
    }

    public void saveFamilyStatus() {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("UPDATE characters SET familyid = ?, seniorid = ?, junior1 = ?, junior2 = ? WHERE id = ?");
            if (mfc == null) {
                ps.setInt(1, 0);
                ps.setInt(2, 0);
                ps.setInt(3, 0);
                ps.setInt(4, 0);
            } else {
                ps.setInt(1, mfc.getFamilyId());
                ps.setInt(2, mfc.getSeniorId());
                ps.setInt(3, mfc.getJunior1());
                ps.setInt(4, mfc.getJunior2());
            }
            ps.setInt(5, id);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException se) {
            System.out.println("SQLException: " + se.getLocalizedMessage());
            se.printStackTrace();
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }

            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }
        }
        //MapleFamily.setOfflineFamilyStatus(familyid, seniorid, junior1, junior2, currentrep, totalrep, id);
    }

    public void modifyCSPoints(int type, int quantity) {
        modifyCSPoints(type, quantity, false);
    }

    public void modifyCSPoints(int type, int quantity, boolean show) {

        switch (type) {
            case 1:
            case 4:
                if (acash + quantity < 0) {
                    if (show) {
                        dropMessage(-1, "You have gained the max cash. No cash will be awarded.");
                    }
                    return;
                }
                acash += quantity;
                break;
            case 2:
                if (maplepoints + quantity < 0) {
                    return;
                }
                maplepoints += quantity;
                break;
            case 3:
                if (vpoints + quantity < 0) {
                    if (show) {
                        dropMessage(-1, "본섭 캐시를 더 받을 수 없습니다. 캐시가 지급되지 않습니다.");
                    }
                    return;
                }
                vpoints += quantity;
                break;
            default:
                break;
        }
        if (show && quantity != 0) {
            //client.getSession().write(MaplePacketCreator.yellowChat("[알림] " + quantity + (type == 1 ? " 캐시를 획득 했습니다." : " 메이플포인트를 획득 했습니다.")));
        }
    }

    public int getCSPoints(int type) {
        switch (type) {
            case 1:
            case 4:
                return acash;
            case 2:
                return maplepoints;
            case 3:
                return vpoints;
            default:
                return 0;
        }
    }

    public final boolean hasEquipped(int itemid) {
        return inventory[MapleInventoryType.EQUIPPED.ordinal()].countById(itemid) >= 1;
    }

    public final boolean haveItem(int itemid, int quantity, boolean checkEquipped, boolean greaterOrEquals) {
        final MapleInventoryType type = GameConstants.getInventoryType(itemid);
        int possesed = inventory[type.ordinal()].countById(itemid);
        if (checkEquipped && type == MapleInventoryType.EQUIP) {
            possesed += inventory[MapleInventoryType.EQUIPPED.ordinal()].countById(itemid);
        }
        if (greaterOrEquals) {
            return possesed >= quantity;
        } else {
            return possesed == quantity;
        }
    }

    public final boolean haveItem(int itemid, int quantity) {
        return haveItem(itemid, quantity, true, true);
    }

    public final boolean haveItem(int itemid) {
        return haveItem(itemid, 1, true, true);
    }

    public String getJobName(int job) {
        switch (job) {
            case 0:
                return "초보자";
            case 100:
                return "전사";
            case 110:
                return "파이터";
            case 111:
                return "크루세이더";
            case 112:
                return "히어로";
            case 120:
                return "페이지";
            case 121:
                return "나이트";
            case 122:
                return "팔라딘";
            case 130:
                return "스피어맨";
            case 131:
                return "용기사";
            case 132:
                return "다크나이트";
            case 200:
                return "마법사";
            case 210:
                return "위자드(불,독)";
            case 211:
                return "메이지(불,독)";
            case 212:
                return "아크메이지(불,독)";
            case 220:
                return "위자드(썬,콜)";
            case 221:
                return "메이지(썬,콜)";
            case 222:
                return "아크메이지(썬,콜)";
            case 230:
                return "클레릭";
            case 231:
                return "프리스트";
            case 232:
                return "비숍";
            case 300:
                return "궁수";
            case 310:
                return "헌터";
            case 311:
                return "레인저";
            case 312:
                return "보우마스터";
            case 320:
                return "사수";
            case 321:
                return "저격수";
            case 322:
                return "신궁";
            case 400:
                return "도적";
            case 410:
                return "어쌔신";
            case 411:
                return "허밋";
            case 412:
                return "나이트로드";
            case 420:
                return "시프";
            case 421:
                return "시프마스터";
            case 422:
                return "섀도어";
            case 430:
                return "세미듀어러";
            case 431:
                return "듀어러";
            case 432:
                return "듀얼마스터";
            case 433:
                return "슬래셔";
            case 434:
                return "듀얼블레이더";
            case 500:
                return "해적";
            case 510:
                return "인파이터";
            case 511:
                return "버커니어";
            case 512:
                return "바이퍼";
            case 520:
                return "건슬링거";
            case 521:
                return "발키리";
            case 522:
                return "캡틴";
            case 800:
                return "매니저";
            case 900:
                return "운영자";
            case 1000:
                return "시그너스";
            case 1100:
                return "소울마스터 1차";
            case 1110:
                return "소울마스터 2차";
            case 1111:
                return "소울마스터 3차";
            case 1112:
                return "소울마스터 4차";
            case 1200:
                return "플레임위자드 1차";
            case 1210:
                return "플레임위자드 2차";
            case 1211:
                return "플레임위자드 3차";
            case 1212:
                return "플레임위자드 4차";
            case 1300:
                return "윈드브레이커 1차";
            case 1310:
                return "윈드브레이커 2차";
            case 1311:
                return "윈드브레이커 3차";
            case 1312:
                return "윈드브레이커 4차";
            case 1400:
                return "나이트워커 1차";
            case 1410:
                return "나이트워커 2차";
            case 1411:
                return "나이트워커 3차";
            case 1412:
                return "나이트워커 4차";
            case 1500:
                return "스트라이커 1차";
            case 1510:
                return "스트라이커 2차";
            case 1511:
                return "스트라이커 3차";
            case 1512:
                return "스트라이커 4차";
            case 2000:
                return "레전드 (아란)";
            case 2100:
                return "아란 1차";
            case 2110:
                return "아란 2차";
            case 2111:
                return "아란 3차";
            case 2112:
                return "아란 4차";
            case 2001:
                return "레전드 (에반)";
            case 2200:
                return "에반 1차";
            case 2210:
                return "에반 2차";
            case 2211:
                return "에반 3차";
            case 2212:
                return "에반 4차";
            case 2213:
                return "에반 5차";
            case 2214:
                return "에반 6차";
            case 2215:
                return "에반 7차";
            case 2216:
                return "에반 8차";
            case 2217:
                return "에반 9차";
            case 2218:
                return "에반 10차";
            case 3000:
                return "시티즌";
            case 3200:
                return "배틀메이지";
            case 3210:
                return "배틀메이지";
            case 3211:
                return "배틀메이지";
            case 3212:
                return "배틀메이지";
            case 3300:
                return "와일드헌터";
            case 3310:
                return "와일드헌터";
            case 3311:
                return "와일드헌터";
            case 3312:
                return "와일드헌터";
            case 3500:
                return "메카닉";
            case 3510:
                return "메카닉";
            case 3511:
                return "메카닉";
            case 3512:
                return "메카닉";
            default:
                return "알 수 없음";
        }
    }

    public void setGMLevel(int gmLevel) {
        this.gmLevel = (byte) gmLevel;
    }

    public static enum FameStatus {

        OK, NOT_TODAY, NOT_THIS_MONTH
    }

    public byte getBuddyCapacity() {
        return buddylist.getCapacity();
    }

    public void setBuddyCapacity(byte capacity) {
        buddylist.setCapacity(capacity);
        client.getSession().write(MaplePacketCreator.updateBuddyCapacity(capacity));
    }

    public MapleMessenger getMessenger() {
        return messenger;
    }

    public void setMessenger(MapleMessenger messenger) {
        this.messenger = messenger;
    }

    public void addCooldown(int skillId, long startTime, long length) {
        coolDowns.put(Integer.valueOf(skillId), new MapleCoolDownValueHolder(skillId, startTime, length));
    }

    public void removeCooldown(int skillId) {
        if (coolDowns.containsKey(Integer.valueOf(skillId))) {
            coolDowns.remove(Integer.valueOf(skillId));
        }
    }

    public boolean skillisCooling(int skillId) {
        return coolDowns.containsKey(Integer.valueOf(skillId));
    }

    public void giveCoolDowns(final int skillid, long starttime, long length) {
        addCooldown(skillid, starttime, length);
    }

    public void giveCoolDowns(final List<MapleCoolDownValueHolder> cooldowns) {
        int time;
        if (cooldowns != null) {
            for (MapleCoolDownValueHolder cooldown : cooldowns) {
                coolDowns.put(cooldown.skillId, cooldown);
            }
        } else {
            Connection con = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("SELECT SkillID,StartTime,length FROM skills_cooldowns WHERE charid = ?");
                ps.setInt(1, getId());
                rs = ps.executeQuery();
                while (rs.next()) {
                    if (rs.getLong("length") + rs.getLong("StartTime") - System.currentTimeMillis() <= 0) {
                        continue;
                    }
                    giveCoolDowns(rs.getInt("SkillID"), rs.getLong("StartTime"), rs.getLong("length"));
                }
                ps.close();
                rs.close();
                deleteWhereCharacterId(con, "DELETE FROM skills_cooldowns WHERE charid = ?");

            } catch (SQLException e) {
                System.err.println("Error while retriving cooldown from SQL storage");
            } finally {
                if (con != null) {
                    try {
                        con.close();
                    } catch (Exception e) {
                    }
                }

                if (rs != null) {
                    try {
                        rs.close();
                    } catch (Exception e) {
                    }
                }

                if (ps != null) {
                    try {
                        ps.close();
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    public int getCooldownSize() {
        return coolDowns.size();
    }

    public int getDiseaseSize() {
        return diseases.size();
    }

    public List<MapleCoolDownValueHolder> getCooldowns() {
        List<MapleCoolDownValueHolder> ret = new ArrayList<MapleCoolDownValueHolder>();
        for (MapleCoolDownValueHolder mc : coolDowns.values()) {
            if (mc != null) {
                ret.add(mc);
            }
        }
        return ret;
    }

    public final List<MapleDiseaseValueHolder> getAllDiseases() {
        return new ArrayList<MapleDiseaseValueHolder>(diseases.values());
    }

    public final boolean hasDisease(final MapleDisease dis) {
        return diseases.containsKey(dis);
    }

    public void giveDebuff(final MapleDisease disease, MobSkill skill, short tDelay) {
        giveDebuff(disease, skill.getX(), skill.getDuration(), skill.getSkillId(), skill.getSkillLevel(), tDelay);
    }

    public void giveDebuff(final MapleDisease disease, int x, long duration, int skillid, int level, int tDelay) {
        if (map != null && !hasDisease(disease)) {
            if (disease != MapleDisease.STUN) {
                if (getBuffedValue(MapleBuffStat.HOLY_SHIELD) != null) {
                    return;
                }
            }
            diseases.put(disease, new MapleDiseaseValueHolder(disease, System.currentTimeMillis(), duration));
            client.getSession().write(TemporaryStatsPacket.giveDebuff(disease, x, skillid, level, (int) duration, tDelay));
            if (disease == MapleDisease.SLOW) {
                map.broadcastMessage(this, TemporaryStatsPacket.giveForeignDebuffSlow(id, disease, skillid, level, x, tDelay), false);
            } else {
                map.broadcastMessage(this, TemporaryStatsPacket.giveForeignDebuff(id, disease, skillid, level, x, tDelay), false);
            }

            if (x > 0 && disease == MapleDisease.POISON) { //poison, subtract all HP
                addHP((int) -x);
            }
        }
    }

    public final void giveSilentDebuff(final List<MapleDiseaseValueHolder> ld) {
        if (ld != null) {
            for (final MapleDiseaseValueHolder disease : ld) {
                diseases.put(disease.disease, disease);
            }
        }
    }

    public void dispelDebuff(MapleDisease debuff) {
        if (hasDisease(debuff)) {
            client.getSession().write(MaplePacketCreator.cancelDebuff(debuff));
            map.broadcastMessage(this, MaplePacketCreator.cancelForeignDebuff(id, debuff), false);

            diseases.remove(debuff);
        }
    }

    public void dispelDebuffs() {
        List<MapleDisease> diseasess = new ArrayList<MapleDisease>(diseases.keySet());
        for (MapleDisease d : diseasess) {
            dispelDebuff(d);
        }
    }

    public void cancelAllDebuffs() {
        diseases.clear();
    }

    public void setLevel(final short level) {
        this.level = (short) (level - 1);
    }

    public void sendNote(String to, String msg) {
        sendNote(to, msg, 0);
    }

    public void sendNote(String to, String msg, int fame) {
        MapleCharacterUtil.sendNote(to, getName(), msg, fame);
    }

    public void showNote() {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM notes WHERE `to`=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ps.setString(1, getName());
            rs = ps.executeQuery();
            rs.last();
            int count = rs.getRow();
            rs.first();
            client.getSession().write(MTSCSPacket.showNotes(rs, count));
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Unable to show note" + e);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }

            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }

            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }
        }
        deleteNote();
    }

    public void deleteNote() {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT gift FROM notes WHERE `to`=?");
            ps.setString(1, getName());
            rs = ps.executeQuery();
            if (rs.next()) {
                if (rs.getInt("gift") == fame && fame > 0) { //not exploited! hurray
                    addFame(fame);
                    updateSingleStat(MapleStat.FAME, getFame());
                    client.getSession().write(MaplePacketCreator.getShowFameGain(fame));
                }
            }
            rs.close();
            ps.close();
            ps = con.prepareStatement("DELETE FROM notes WHERE `to`=?");
            ps.setString(1, getName());
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Unable to delete note" + e);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }

            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }

            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public void deleteNote(int id, int fame) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT gift FROM notes WHERE `id`=?");
            ps.setInt(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                if (rs.getInt("gift") == fame && fame > 0) { //not exploited! hurray
                    addFame(fame);
                    updateSingleStat(MapleStat.FAME, getFame());
                    client.getSession().write(MaplePacketCreator.getShowFameGain(fame));
                }
            }
            rs.close();
            ps.close();
            ps = con.prepareStatement("DELETE FROM notes WHERE `id`=?");
            ps.setInt(1, id);
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Unable to delete note" + e);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }

            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }

            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public int getMulungEnergy() {
        return mulung_energy;
    }

    public void mulung_EnergyModify(boolean inc) {
        if (inc) {
            if (mulung_energy + 100 > 10000) {
                mulung_energy = 10000;
            } else {
                mulung_energy += 100;
            }
        } else {
            mulung_energy = 0;
        }
        client.getSession().write(MaplePacketCreator.MulungEnergy(mulung_energy));
    }

    public String searchCash(String search) {
        StringBuilder Text = new StringBuilder("아래의 아이템이 검색되었습니다.\r\n");
        boolean Count = false;
        if (search.length() < 2) {
            return "검색한 값의 글자 수가 너무 작습니다.";
        } else {
            for (ItemInformation itemPair : MapleItemInformationProvider.getInstance().getAllItems()) {
                if (itemPair.itemId < 2000000 && itemPair != null && itemPair.name != null && itemPair.name.toLowerCase().contains(search.toLowerCase()) && MapleItemInformationProvider.getInstance().isCash(itemPair.itemId)) {
                    Count = true;
                    Text.append("#L" + itemPair.itemId + "##i" + itemPair.itemId + ":# #b#z" + itemPair.itemId + "##k (코드 : " + itemPair.itemId + ")\r\n");
                }
            }
            if (!Count) {
                return "검색된 아이템이 없습니다.";
            }
        }
        return Text.toString();
    }

    public String searchPet(String search) {
        StringBuilder Text = new StringBuilder("아래의 아이템이 검색되었습니다.\r\n");
        boolean Count = false;
        if (search.length() < 2) {
            return "검색한 값의 글자 수가 너무 작습니다.";
        } else {
            for (ItemInformation itemPair : MapleItemInformationProvider.getInstance().getAllItems()) {
                if (((itemPair.itemId >= 1802000 && itemPair.itemId <= 1802200) || (itemPair.itemId >= 5000000 && itemPair.itemId <= 5003000))
                        && itemPair != null
                        && itemPair.name != null
                        && itemPair.name.toLowerCase().contains(search.toLowerCase())
                        && MapleItemInformationProvider.getInstance().isCash(itemPair.itemId)) {

                    Count = true;
                    Text.append("#L" + itemPair.itemId + "##i" + itemPair.itemId + ":# #b#z" + itemPair.itemId + "##k (코드 : " + itemPair.itemId + ")\r\n");
                }
            }
            if (!Count) {
                return "검색된 아이템이 없습니다.";
            }
        }
        return Text.toString();
    }

    public void writeMulungEnergy() {
        client.getSession().write(MaplePacketCreator.MulungEnergy(mulung_energy));
    }

    public void writeEnergy(String type, String inc) {
        client.getSession().write(MaplePacketCreator.sendPyramidEnergy(type, inc));
    }

    public void writeStatus(String type, String inc) {
        client.getSession().write(MaplePacketCreator.sendGhostStatus(type, inc));
    }

    public void writePoint(String type, String inc) {
        client.getSession().write(MaplePacketCreator.sendGhostPoint(type, inc));
    }

    public final short getCombo() {
        return combo;
    }

    public void setCombo(final short combo) {
        this.combo = combo;
    }

    public final long getLastCombo() {
        return lastCombo;
    }

    public void setLastCombo(final long combo) {
        this.lastCombo = combo;
    }

    public final long getKeyDownSkill_Time() {
        return keydown_skill;
    }

    public void setKeyDownSkill_Time(final long keydown_skill) {
        this.keydown_skill = keydown_skill;
    }

    public void checkBerserk() { //berserk is special in that it doesn't use worldtimer :)
        if (job != 132 || lastBerserkTime < 0 || lastBerserkTime + 10000 > System.currentTimeMillis()) {
            return;
        }
        final Skill BerserkX = SkillFactory.getSkill(1320006);
        final int skilllevel = getTotalSkillLevel(BerserkX);
        if (skilllevel >= 1 && map != null) {
            //lastBerserkTime = System.currentTimeMillis();
            //final MapleStatEffect ampStat = BerserkX.getEffect(skilllevel);
            //stats.Berserk = stats.getHp() * 100 / stats.getCurrentMaxHp() >= ampStat.getX();
            //client.getSession().write(MaplePacketCreator.showOwnBuffEffect(1320006, 1, getLevel(), skilllevel, (byte) (stats.Berserk ? 1 : 0)));
            //map.broadcastMessage(this, MaplePacketCreator.showBuffeffect(getId(), 1320006, 1, getLevel(), skilllevel, (byte) (stats.Berserk ? 1 : 0)), false);
        } else {
            //lastBerserkTime = -1;
        }
    }

    public void checkFury(boolean check) { //berserk is special in that it doesn't use worldtimer :)
        if (!GameConstants.isEvan(job)) {
            return;
        }
        final Skill FuryX = SkillFactory.getSkill(22160000);
        final int skilllevel = getTotalSkillLevel(FuryX);
        if (check) {
            client.getSession().write(MaplePacketCreator.showOwnBuffEffect(22160000, 1, getLevel(), skilllevel, (byte) 0));
            map.broadcastMessage(this, MaplePacketCreator.showBuffeffect(getId(), 22160000, 1, getLevel(), skilllevel, (byte) 0), false);
        }
        if (skilllevel >= 1 && map != null) {
            if (stats.getMPPercent() >= FuryX.getEffect(skilllevel).getX() && stats.getMPPercent() <= FuryX.getEffect(skilllevel).getY()) {
                stats.Fury = true;
                client.getSession().write(MaplePacketCreator.showOwnBuffEffect(22160000, 1, getLevel(), skilllevel, (byte) 1));
                map.broadcastMessage(this, MaplePacketCreator.showBuffeffect(getId(), 22160000, 1, getLevel(), skilllevel, (byte) 1), false);
                stats.FuryValue = FuryX.getEffect(skilllevel).getDamage();
                return;
            } else {
                client.getSession().write(MaplePacketCreator.showOwnBuffEffect(22160000, 1, getLevel(), skilllevel, (byte) 0));
                map.broadcastMessage(this, MaplePacketCreator.showBuffeffect(getId(), 22160000, 1, getLevel(), skilllevel, (byte) 0), false);
            }
        }
        stats.Fury = false;
        stats.FuryValue = 0;
        return;
    }

    /*   public void checkFury() { //berserk is special in that it doesn't use worldtimer :)
     if (!GameConstants.isEvan(job)) {
     return;
     }
     final Skill FuryX = SkillFactory.getSkill(22160000);
     final int skilllevel = getTotalSkillLevel(FuryX);
     if (skilllevel >= 1 && map != null) {
     client.getSession().write(MaplePacketCreator.showOwnBuffEffect(22160000, 1, getLevel(), skilllevel, (byte) (stats.Berserk ? 1 : 0)));
     map.broadcastMessage(this, MaplePacketCreator.showBuffeffect(getId(), 22160000, 1, getLevel(), skilllevel, (byte) (stats.Berserk ? 1 : 0)), false);
     }
     }*/
    public void setChalkboard(String text) {
        this.chalktext = text;
        if (map != null) {
            map.broadcastMessage(MTSCSPacket.useChalkboard(getId(), text));
        }
    }

    public String getChalkboard() {
        return chalktext;
    }

    public MapleMount getMount() {
        return mount;
    }

    public int[] getWishlist() {
        return wishlist;
    }

    public void clearWishlist() {
        for (int i = 0; i < 10; i++) {
            wishlist[i] = 0;
        }
        changed_wishlist = true;
    }

    public int getWishlistSize() {
        int ret = 0;
        for (int i = 0; i < 10; i++) {
            if (wishlist[i] > 0) {
                ret++;
            }
        }
        return ret;
    }

    public void setWishlist(int[] wl) {
        this.wishlist = wl;
        changed_wishlist = true;
    }

    public int[] getRocks() {
        return rocks;
    }

    public int getRockSize() {
        int ret = 0;
        for (int i = 0; i < 10; i++) {
            if (rocks[i] != 999999999) {
                ret++;
            }
        }
        return ret;
    }

    public void deleteFromRocks(int map) {
        for (int i = 0; i < 10; i++) {
            if (rocks[i] == map) {
                rocks[i] = 999999999;
                changed_trocklocations = true;
                break;
            }
        }
    }

    public void addRockMap() {
        if (getRockSize() >= 10) {
            return;
        }
        for (int i = 0; i < 10; ++i) {
            if (rocks[i] == 999999999) {
                rocks[i] = getMapId();
                changed_trocklocations = true;
                break;
            }
        }
    }

    public boolean isRockMap(int id) {
        for (int i = 0; i < 10; i++) {
            if (rocks[i] == id) {
                return true;
            }
        }
        return false;
    }

    public int[] getRegRocks() {
        return regrocks;
    }

    public int getRegRockSize() {
        int ret = 0;
        for (int i = 0; i < 5; i++) {
            if (regrocks[i] != 999999999) {
                ret++;
            }
        }
        return ret;
    }

    public void deleteFromRegRocks(int map) {
        for (int i = 0; i < 5; i++) {
            if (regrocks[i] == map) {
                regrocks[i] = 999999999;
                changed_regrocklocations = true;
                break;
            }
        }
    }

    public void addRegRockMap() {
        if (getRegRockSize() >= 5) {
            return;
        }
        for (int i = 0; i < 5; ++i) {
            if (regrocks[i] == 999999999) {
                regrocks[i] = getMapId();
                changed_regrocklocations = true;
                break;
            }
        }

    }

    public boolean isRegRockMap(int id) {
        for (int i = 0; i < 5; i++) {
            if (regrocks[i] == id) {
                return true;
            }
        }
        return false;
    }

    public int[] getHyperRocks() {
        return hyperrocks;
    }

    public int getHyperRockSize() {
        int ret = 0;
        for (int i = 0; i < 13; i++) {
            if (hyperrocks[i] != 999999999) {
                ret++;
            }
        }
        return ret;
    }

    public void deleteFromHyperRocks(int map) {
        for (int i = 0; i < 13; i++) {
            if (hyperrocks[i] == map) {
                hyperrocks[i] = 999999999;
                changed_hyperrocklocations = true;
                break;
            }
        }
    }

    public void addHyperRockMap() {
        if (getRegRockSize() >= 13) {
            return;
        }
        hyperrocks[getHyperRockSize()] = getMapId();
        changed_hyperrocklocations = true;
    }

    public boolean isHyperRockMap(int id) {
        for (int i = 0; i < 13; i++) {
            if (hyperrocks[i] == id) {
                return true;
            }
        }
        return false;
    }

    public List<LifeMovementFragment> getLastRes() {
        return lastres;
    }

    public void setLastRes(List<LifeMovementFragment> lastres) {
        this.lastres = lastres;
    }

    public void dropMessage(int type, String message) {
        if (type == -1) {
            client.getSession().write(UIPacket.getTopMsg(message));
        } else if (type == -2) {
            client.getSession().write(PlayerShopPacket.shopChat(message, 0)); //0 or what
        } else if (type == -3) {
            client.getSession().write(MaplePacketCreator.getChatText(getId(), message, isSuperGM(), 0)); //1 = hide
        } else if (type == -4) {
            client.getSession().write(MaplePacketCreator.getChatText(getId(), message, isSuperGM(), 1)); //1 = hide
        } else if (type == -7) {
            client.getSession().write(UIPacket.getMidMsg(message, false, 0));
        } else if (type == -8) {
            client.getSession().write(UIPacket.getMidMsg(message, true, 0));
        } else if (type == -9) {
            client.sendPacket(MaplePacketCreator.yellowChat(message));
        } else {
            client.getSession().write(MaplePacketCreator.serverNotice(type, message));
        }
    }

    public IMaplePlayerShop getPlayerShop() {
        return playerShop;
    }

    public void setPlayerShop(IMaplePlayerShop playerShop) {
        this.playerShop = playerShop;
    }

    public int getConversation() {
        return inst.get();
    }

    public void setConversation(int inst) {
        this.inst.set(inst);
    }

    public int getDirection() {
        return insd.get();
    }

    public void setDirection(int inst) {
        this.insd.set(inst);
    }

    public MapleCarnivalParty getCarnivalParty() {
        return carnivalParty;
    }

    public void setCarnivalParty(MapleCarnivalParty party) {
        carnivalParty = party;
    }

    public void addCP(int ammount) {
        totalCP += ammount;
        availableCP += ammount;
    }

    public void useCP(int ammount) {
        availableCP -= ammount;
    }

    public int getAvailableCP() {
        return availableCP;
    }

    public int getTotalCP() {
        return totalCP;
    }

    public void resetCP() {
        totalCP = 0;
        availableCP = 0;
    }

    public void addCarnivalRequest(MapleCarnivalChallenge request) {
        pendingCarnivalRequests.add(request);
    }

    public final MapleCarnivalChallenge getNextCarnivalRequest() {
        return pendingCarnivalRequests.pollLast();
    }

    public void clearCarnivalRequests() {
        pendingCarnivalRequests = new LinkedList<MapleCarnivalChallenge>();
    }

    public void startMonsterCarnival(final int enemyavailable, final int enemytotal) {
        client.getSession().write(MonsterCarnivalPacket.startMonsterCarnival(this, enemyavailable, enemytotal));
    }

    public void CPUpdate(final boolean party, final int available, final int total, final int team) {
        client.getSession().write(MonsterCarnivalPacket.CPUpdate(party, available, total, team));
    }

    public void playerDiedCPQ(final String name, final int lostCP, final int team) {
        client.getSession().write(MonsterCarnivalPacket.playerDiedMessage(name, lostCP, team));
    }

    public void setAchievementFinished(int id) {
        if (!finishedAchievements.contains(id)) {
            finishedAchievements.add(id);
            changed_achievements = true;
        }
    }

    public boolean achievementFinished(int achievementid) {
        return finishedAchievements.contains(achievementid);
    }

    public void finishAchievement(int id) {
        if (!achievementFinished(id)) {
            if (isAlive()) {
                MapleAchievements.getInstance().getById(id).finishAchievement(this);
            }
        }
    }

    public List<Integer> getFinishedAchievements() {
        return finishedAchievements;
    }

    public boolean getCanTalk() {
        return this.canTalk;
    }

    public void canTalk(boolean talk) {
        this.canTalk = talk;
    }

    public double getEXPMod() {
        return stats.expMod;
    }

    public int getDropMod() {
        return stats.dropMod;
    }

    public int getCashMod() {
        return stats.cashMod;
    }

    public void setPoints(int p) {
        this.points = p;
        if (this.points >= 1) {
            // finishAchievement(1);
        }
    }

    public int getPoints() {
        return points;
    }

    public void setVPoints(int p) {
        this.vpoints = p;
    }

    public int getVPoints() {
        return vpoints;
    }

    public CashShop getCashInventory() {
        return cs;
    }

    public void removeItem(int id, int quantity) {
        MapleInventoryManipulator.removeById(client, GameConstants.getInventoryType(id), id, -quantity, true, false);
        client.getSession().write(MaplePacketCreator.getShowItemGain(id, (short) quantity, true));
    }

    public void removeAll(int id) {
        removeAll(id, true);
    }

    public void removeAll(int id, boolean show) {
        MapleInventoryType type = GameConstants.getInventoryType(id);
        int possessed = getInventory(type).countById(id);

        if (possessed > 0) {
            MapleInventoryManipulator.removeById(getClient(), type, id, possessed, true, false);
            if (show) {
                getClient().getSession().write(MaplePacketCreator.getShowItemGain(id, (short) -possessed, true));
            }
        }

        boolean stanceSkill = false;
        for (int i = 0; i < 6; i++) {
            if (this.getInventory(MapleInventoryType.SETUP).findById(3960096 + i) != null) {
                stanceSkill = true;
                this.changeSkillLevel_Skip(SkillFactory.getSkill(this.getStat().getSkillByJob(1210, this.getJob())), (byte) 1, (byte) 0, true);
            }
        }
        if (!stanceSkill) {
            this.changeSkillLevel_Skip(SkillFactory.getSkill(this.getStat().getSkillByJob(1210, this.getJob())), (byte) -1, (byte) -1, true);
        }
    }

    public Triple<List<MapleRing>, List<MapleRing>, List<MapleRing>> getRings(boolean equip) {
        MapleInventory iv = getInventory(MapleInventoryType.EQUIPPED);
        List<Item> equipped = iv.newList();
        Collections.sort(equipped);
        List<MapleRing> crings = new ArrayList<MapleRing>(), frings = new ArrayList<MapleRing>(), mrings = new ArrayList<MapleRing>();
        MapleRing ring;
        for (Item ite : equipped) {
            Equip item = (Equip) ite;
            if (item.getRing() != null) {
                ring = item.getRing();
                ring.setEquipped(true);
                if (GameConstants.isEffectRing(item.getItemId())) {
                    if (equip) {
                        if (GameConstants.isCrushRing(item.getItemId())) {
                            crings.add(ring);
                        } else if (GameConstants.isFriendshipRing(item.getItemId())) {
                            frings.add(ring);
                        } else if (GameConstants.isMarriageRing(item.getItemId())) {
                            mrings.add(ring);
                        }
                    } else {
                        if (crings.size() == 0 && GameConstants.isCrushRing(item.getItemId())) {
                            crings.add(ring);
                        } else if (frings.size() == 0 && GameConstants.isFriendshipRing(item.getItemId())) {
                            frings.add(ring);
                        } else if (mrings.size() == 0 && GameConstants.isMarriageRing(item.getItemId())) {
                            mrings.add(ring);
                        } //for 3rd person the actual slot doesnt matter, so we'll use this to have both shirt/ring same?
                        //however there seems to be something else behind this, will have to sniff someone with shirt and ring, or more conveniently 3-4 of those
                    }
                }
            }
        }
        if (equip) {
            iv = getInventory(MapleInventoryType.EQUIP);
            for (Item ite : iv.list()) {
                Equip item = (Equip) ite;
                if (item.getRing() != null && GameConstants.isCrushRing(item.getItemId())) {
                    ring = item.getRing();
                    ring.setEquipped(false);
                    if (GameConstants.isFriendshipRing(item.getItemId())) {
                        frings.add(ring);
                    } else if (GameConstants.isCrushRing(item.getItemId())) {
                        crings.add(ring);
                    } else if (GameConstants.isMarriageRing(item.getItemId())) {
                        mrings.add(ring);
                    }
                }
            }
        }
        Collections.sort(frings, new MapleRing.RingComparator());
        Collections.sort(crings, new MapleRing.RingComparator());
        Collections.sort(mrings, new MapleRing.RingComparator());
        return new Triple<List<MapleRing>, List<MapleRing>, List<MapleRing>>(crings, frings, mrings);
    }

    public int getFH() {
        MapleFoothold fh = getMap().getFootholds().findBelow(getTruePosition());
        if (fh != null) {
            return fh.getId();
        }
        return 0;
    }

    public final boolean canFairy(long now) {
        return lastFairyTime > 0 && lastFairyTime + (60 * 60 * 1000) < now;
    }

    public final boolean canHP(long now) {
        if (lastHPTime + 5000 < now) {
            lastHPTime = now;
            return true;
        }
        return false;
    }

    public final boolean canMP(long now) {
        if (lastMPTime + 5000 < now) {
            lastMPTime = now;
            return true;
        }
        return false;
    }

    public final boolean canHPRecover(long now) {
        if (stats.hpRecoverTime > 0 && lastHPTime + stats.hpRecoverTime < now) {
            lastHPTime = now;
            return true;
        }
        return false;
    }

    public final boolean canMPRecover(long now) {
        if (stats.mpRecoverTime > 0 && lastMPTime + stats.mpRecoverTime < now) {
            lastMPTime = now;
            return true;
        }
        return false;
    }

    public byte getFairyExp() {
        return fairyExp;
    }

    public int getTeam() {
        return coconutteam;
    }

    public void setTeam(int v) {
        this.coconutteam = v;
    }

    public void spawnPet(byte slot) {
        spawnPet(slot, true);
    }

    public void spawnPet(byte slot, boolean lead) {
        spawnPet(slot, lead, true);
    }

    public void spawnPet(byte slot, boolean lead, boolean broadcast) {
        final Item item = getInventory(MapleInventoryType.CASH).getItem(slot);
        if (item == null || item.getItemId() >= 5010000 || item.getItemId() < 5000000) {
            return;
        }
        final MaplePet pet = item.getPet();
        Map<String, Integer> multipet = MapleItemInformationProvider.getInstance().getEquipStats(item.getItemId());
        if (pet != null && (item.getItemId() != 5000054 || pet.getSecondsLeft() > 0) && (item.getExpiration() == -1 || item.getExpiration() > System.currentTimeMillis())) {
            int petIndex = getPetIndex(pet);
            if (petIndex != -1) {
                unequipPet(pet, false, false);
            } else {
                final Point pos = getTruePosition();
                pet.setPos(pos);
                try {
                    pet.setFh(getMap().getFootholds().findBelow(pos).getId());
                } catch (NullPointerException e) {
                    pet.setFh(0); //lol, it can be fixed by movement
                }
                pet.setStance(0);
//                pet.setSummoned(1);
                addPetz(pet);
                pet.setSummoned(getPetIndex(pet) + 1); //then get the index
                if (getMap() != null) {
                    getMap().broadcastMessage(this, PetPacket.showPet(this, pet, false, false), true);
                    client.getSession().write(PetPacket.updatePet(pet, getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition()), true));
                    client.getSession().write(PetPacket.petStatUpdate(this));//1126
                    //client.getSession().write(MaplePacketCreator.updatePlayerStats(Collections.singletonMap(MapleStat.PET, pet.getUniqueId()), 0));
                }
            }
        }
        client.getSession().write(MaplePacketCreator.enableActions());
    }

    public void clearLinkMid() {
        linkMobs.clear();
        cancelEffectFromBuffStat(MapleBuffStat.HOMING_BEACON);
        cancelEffectFromBuffStat(MapleBuffStat.ARCANE_AIM);
    }

    public int getFirstLinkMid() {
        for (Integer lm : linkMobs.keySet()) {
            return lm.intValue();
        }
        return 0;
    }

    public Map<Integer, Integer> getAllLinkMid() {
        return linkMobs;
    }

    public void setLinkMid(int lm, int x) {
        linkMobs.put(lm, x);
    }

    public int getDamageIncrease(int lm) {
        if (linkMobs.containsKey(lm)) {
            return linkMobs.get(lm);
        }
        return 0;
    }

    public void setDragon(MapleDragon d) {
        this.dragon = d;
    }

    public MapleExtractor getExtractor() {
        return extractor;
    }

    public void setExtractor(MapleExtractor me) {
        removeExtractor();
        this.extractor = me;
    }

    public void removeExtractor() {
        if (extractor != null) {
//            map.broadcastMessage(MaplePacketCreator.removeExtractor(this.id));
            map.removeMapObject(extractor);
            extractor = null;
        }
    }

    public final void spawnSavedPets() {
        for (int i = 0; i < petStore.length; i++) {
            if (petStore[i] > -1) {
                spawnPet(petStore[i], false);
            }
        }
        petStore = new byte[]{-1, -1, -1};
    }

    public final byte[] getPetStores() {
        return petStore;
    }

    public void resetStats(final int str, final int dex, final int int_, final int luk) {
        Map<MapleStat, Integer> stat = new EnumMap<MapleStat, Integer>(MapleStat.class);
        int total = stats.getStr() + stats.getDex() + stats.getLuk() + stats.getInt() + getRemainingAp();

        total -= str;
        stats.str = (short) str;

        total -= dex;
        stats.dex = (short) dex;

        total -= int_;
        stats.int_ = (short) int_;

        total -= luk;
        stats.luk = (short) luk;

        setRemainingAp((short) total);
        stats.recalcLocalStats(this);
        stat.put(MapleStat.STR, str);
        stat.put(MapleStat.DEX, dex);
        stat.put(MapleStat.INT, int_);
        stat.put(MapleStat.LUK, luk);
        stat.put(MapleStat.AVAILABLEAP, total);
        client.getSession().write(MaplePacketCreator.updatePlayerStats(stat, false, getJob()));
    }

    public Event_PyramidSubway getPyramidSubway() {
        return pyramidSubway;
    }

    public void setPyramidSubway(Event_PyramidSubway ps) {
        this.pyramidSubway = ps;
    }

    public byte getSubcategory() {
        if (job >= 430 && job <= 434) {
            return 1; //dont set it
        }
        if (GameConstants.isCannon(job) || job == 1) {
            return 2;
        }
        if (job != 0 && job != 400) {
            return 0;
        }
        return subcategory;
    }

    public void setSubcategory(int z) {
        this.subcategory = (byte) z;
    }

    public int itemQuantity(final int itemid) {
        return getInventory(GameConstants.getInventoryType(itemid)).countById(itemid);
    }

    public void setRPS(RockPaperScissors rps) {
        this.rps = rps;
    }

    public RockPaperScissors getRPS() {
        return rps;
    }

    public long getNextConsume() {
        return nextConsume;
    }

    public void setNextConsume(long nc) {
        this.nextConsume = nc;
    }

    public int getRank() {
        return rank;
    }

    public int getRankMove() {
        return rankMove;
    }

    public int getJobRank() {
        return jobRank;
    }

    public int getJobRankMove() {
        return jobRankMove;
    }

    public void changeChannel(final int channel) {
        final ChannelServer toch = ChannelServer.getInstance(channel);

        if (channel == client.getChannel() || toch == null || toch.isShutdown()) {
            client.getSession().write(MaplePacketCreator.serverBlocked(1));
            return;
        }
        changeRemoval();

        final ChannelServer ch = ChannelServer.getInstance(client.getChannel());
        if (getMessenger() != null) {
            World.Messenger.silentLeaveMessenger(getMessenger().getId(), new MapleMessengerCharacter(this));
        }
        PlayerBuffStorage.addBuffsToStorage(getId(), getAllBuffs());
        PlayerBuffStorage.addCooldownsToStorage(getId(), getCooldowns());
        PlayerBuffStorage.addDiseaseToStorage(getId(), getAllDiseases());
        World.ChannelChange_Data(new CharacterTransfer(this), getId(), channel);
        if (client.getPlayer().getParty() != null) {
            client.getPlayer().silentPartyUpdate();
            client.getPlayer().getClient().getSession().write(MaplePacketCreator.updateParty(client.getPlayer().getClient().getChannel(), client.getPlayer().getParty(), PartyOperation.SILENT_UPDATE, null));
            client.getPlayer().receivePartyMemberHP();
            client.getPlayer().updatePartyMemberHP();
        }
        ch.removePlayer(this);
        client.updateLoginState(MapleClient.CHANGE_CHANNEL, client.getSessionIPAddress());
        final String s = client.getSessionIPAddress();
        LoginServer.addIPAuth(s.substring(s.indexOf('/') + 1, s.length()));
        LoginServer.setCodeHash(getId(), client.getCodeHash());
        client.getSession().write(MaplePacketCreator.getChannelChange(client, toch.getPort()));
        saveToDB(false, false);
        getMap().removePlayer(this);
        client.setPlayer(null);
        client.setReceiving(false);
    }

    public void expandInventory(byte type, int amount) {
        final MapleInventory inv = getInventory(MapleInventoryType.getByType(type));
        inv.addSlot((byte) amount);
        client.getSession().write(MaplePacketCreator.getSlotUpdate(type, (byte) inv.getSlotLimit()));
    }

    public boolean allowedToTarget(MapleCharacter other) {
        return other != null && (!other.isHidden() || getGMLevel() >= other.getGMLevel());
    }

    public int getFollowId() {
        return followid;
    }

    public void setFollowId(int fi) {
        this.followid = fi;
        if (fi == 0) {
            this.followinitiator = false;
            this.followon = false;
        }
    }

    public void setFollowInitiator(boolean fi) {
        this.followinitiator = fi;
    }

    public void setFollowOn(boolean fi) {
        this.followon = fi;
    }

    public boolean isFollowOn() {
        return followon;
    }

    public boolean isFollowInitiator() {
        return followinitiator;
    }

    public void checkFollow() {
        if (followid <= 0) {
            return;
        }
        if (followon) {
            map.broadcastMessage(MaplePacketCreator.followEffect(id, 0, null));
            map.broadcastMessage(MaplePacketCreator.followEffect(followid, 0, null));
        }
        MapleCharacter tt = map.getCharacterById(followid);
        //      client.getSession().write(MaplePacketCreator.getFollowMessage("Follow canceled."));
        if (tt != null) {
            tt.setFollowId(0);
            //       tt.getClient().getSession().write(MaplePacketCreator.getFollowMessage("Follow canceled."));
        }
        setFollowId(0);
    }

    public int getMarriageId() {
        return marriageId;
    }

    public void setMarriageId(final int mi) {
        this.marriageId = mi;
    }

    public int getMarriageItemId() {
        return marriageItemId;
    }

    public void setMarriageItemId(final int mi) {
        this.marriageItemId = mi;
    }

    public boolean isStaff() {
        return this.gmLevel >= ServerConstants.PlayerGMRank.INTERN.getLevel();
    }

    public boolean isDonator() {
        return this.gmLevel >= ServerConstants.PlayerGMRank.DONATOR.getLevel();
    }

    // TODO: gvup, vic, lose, draw, VR
    public boolean startPartyQuest(final int questid) {
        boolean ret = false;
        MapleQuest q = MapleQuest.getInstance(questid);
        if (q == null || !q.isPartyQuest()) {
            return false;
        }
        if (!quests.containsKey(q) || !questinfo.containsKey(questid)) {
            final MapleQuestStatus status = getQuestNAdd(q);
            status.setStatus((byte) 1);
            updateQuest(status);
            switch (questid) {
                case 1300:
                case 1301:
                case 1302: //carnival, ariants.
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have=0;rank=F;try=0;cmp=0;CR=0;VR=0;gvup=0;vic=0;lose=0;draw=0");
                    break;
                case 1303: //ghost pq
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have=0;have1=0;rank=F;try=0;cmp=0;CR=0;VR=0;vic=0;lose=0");
                    break;
                case 1204: //herb town pq
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have0=0;have1=0;have2=0;have3=0;rank=F;try=0;cmp=0;CR=0;VR=0");
                    break;
                case 1206: //ellin pq
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have0=0;have1=0;rank=F;try=0;cmp=0;CR=0;VR=0");
                    break;
                default:
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have=0;rank=F;try=0;cmp=0;CR=0;VR=0");
                    break;
            }
            ret = true;
        } //started the quest.
        return ret;
    }

    public String getOneInfo(final int questid, final String key) {
        if (!questinfo.containsKey(questid) || key == null || MapleQuest.getInstance(questid) == null/* || !MapleQuest.getInstance(questid).isPartyQuest()*/) {
            return null;
        }
        final String[] split = questinfo.get(questid).split(";");
        for (String x : split) {
            final String[] split2 = x.split("="); //should be only 2
            if (split2.length == 2 && split2[0].equals(key)) {
                return split2[1];
            }
        }
        return null;
    }

    public void updateOneInfo(final int questid, final String key, final String value) {
        if (!questinfo.containsKey(questid) || key == null || value == null || MapleQuest.getInstance(questid) == null/* || !MapleQuest.getInstance(questid).isPartyQuest()*/) {
            return;
        }
        final String[] split = questinfo.get(questid).split(";");
        boolean changed = false;
        final StringBuilder newQuest = new StringBuilder();
        for (String x : split) {
            final String[] split2 = x.split("="); //should be only 2
            if (split2.length != 2) {
                continue;
            }
            if (split2[0].equals(key)) {
                newQuest.append(key).append("=").append(value);
            } else {
                newQuest.append(x);
            }
            newQuest.append(";");
            changed = true;
        }

        updateInfoQuest(questid, changed ? newQuest.toString().substring(0, newQuest.toString().length() - 1) : newQuest.toString());
    }

    public void recalcPartyQuestRank(final int questid) {
        if (MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return;
        }
        if (!startPartyQuest(questid)) {
            final String oldRank = getOneInfo(questid, "rank");
            if (oldRank == null || oldRank.equals("S")) {
                return;
            }
            String newRank = null;
            if (oldRank.equals("A")) {
                newRank = "S";
            } else if (oldRank.equals("B")) {
                newRank = "A";
            } else if (oldRank.equals("C")) {
                newRank = "B";
            } else if (oldRank.equals("D")) {
                newRank = "C";
            } else if (oldRank.equals("F")) {
                newRank = "D";
            } else {
                return;
            }
            final List<Pair<String, Pair<String, Integer>>> questInfo = MapleQuest.getInstance(questid).getInfoByRank(newRank);
            if (questInfo == null) {
                return;
            }
            if (questid == 1200/*월묘*/ || questid == 1203/*여신의 흔적*/ || questid == 1204/*뎁존*/) {
                for (Pair<String, Pair<String, Integer>> q : questInfo) {
                    boolean found = false;
                    final String val = getOneInfo(questid, q.right.left);
                    //dropMessage(6, "val" + val);
                    if (val == null) {
                        return;
                    }
                    int vall = 0;
                    try {
                        vall = Integer.parseInt(val);
                    } catch (NumberFormatException e) {
                        return;
                    }//시간 아이템 갯수 횟수체크순으로 다시 봐보장
                    if (q.left.equals("less")) {//시간통과
                        found = vall < q.right.right;
                        //dropMessage(6, "1: " + found + " vall: " + vall + " q.right.right: " + q.right.right);
                    } else if (q.left.equals("more")) {
                        found = vall >= q.right.right;//"클리어횟수"
                    } else if (q.left.equals("equal")) {
                        found = vall == q.right.right;//"HAVE"
                    }
                    if (!found) {
                        return;
                    }
                }
            } else if (questid == 1201/*커파*/ || questid == 1202/*루파*/ || questid == 1205/*rnj*/ || questid == 1206) {
                for (Pair<String, Pair<String, Integer>> q : questInfo) {
                    boolean found = false;
                    final String val = getOneInfo(questid, q.right.left);
                    //dropMessage(6, "val" + val);
                    if (val == null) {
                        return;
                    }
                    int vall = 0;
                    try {
                        vall = Integer.parseInt(val);
                    } catch (NumberFormatException e) {
                        return;
                    }//시간 아이템 갯수 횟수체크순으로 다시 봐보장
                    if (q.left.equals("less")) {//시간통과
                        found = vall < q.right.right;
                        //dropMessage(6, "1: " + found + " vall: " + vall + " q.right.right: " + q.right.right);
                    } else if (q.left.equals("more")) {
                        found = vall == q.right.right;//"HAVE"
                        found = vall >= q.right.right;//"클리어횟수"
                    } else if (q.left.equals("equal")) {
                    }
                    if (!found) {
                        return;
                    }
                }
            } else if (questid == 1209/*렉스*/) {
                for (Pair<String, Pair<String, Integer>> q : questInfo) {
                    boolean found = false;
                    final String val = getOneInfo(questid, q.right.left);
                    //dropMessage(6, "val" + val);
                    if (val == null) {
                        return;
                    }
                    int vall = 0;
                    try {
                        vall = Integer.parseInt(val);
                    } catch (NumberFormatException e) {
                        return;
                    }//시간 아이템 갯수 횟수체크순으로 다시 봐보장
                    if (q.left.equals("less")) {//시간통과
                        found = vall < q.right.right;
                        //dropMessage(6, "1: " + found + " vall: " + vall + " q.right.right: " + q.right.right);
                    } else if (q.left.equals("more")) {
                        found = vall >= q.right.right;//"클리어횟수"
                    } else if (q.left.equals("equal")) {
                    }
                    if (!found) {
                        return;
                    }
                }
            } else if (questid == 1210/*드래곤라이더*/) {
                for (Pair<String, Pair<String, Integer>> q : questInfo) {
                    boolean found = false;
                    final String val = getOneInfo(questid, q.right.left);
                    //dropMessage(6, "val" + val);
                    if (val == null) {
                        return;
                    }
                    int vall = 0;
                    try {
                        vall = Integer.parseInt(val);
                    } catch (NumberFormatException e) {
                        return;
                    }//시간 아이템 갯수 횟수체크순으로 다시 봐보장
                    if (q.left.equals("less")) {//시간통과
                        //dropMessage(6, "1: " + found + " vall: " + vall + " q.right.right: " + q.right.right);
                    } else if (q.left.equals("more")) {
                        found = vall >= q.right.right;//"클리어횟수"
                    } else if (q.left.equals("equal")) {
                    }
                    if (!found) {
                        return;
                    }
                }
            } else if (questid == 1211/*피라미드*/ || questid == 1212/*임차장*/ || questid == 1213/*무릉도장*/) {
                for (Pair<String, Pair<String, Integer>> q : questInfo) {
                    boolean found = false;
                    final String val = getOneInfo(questid, q.right.left);
                    //dropMessage(6, "val" + val);
                    if (val == null) {
                        return;
                    }
                    int vall = 0;
                    try {
                        vall = Integer.parseInt(val);
                    } catch (NumberFormatException e) {
                        return;
                    }
                    if (q.left.equals("less")) {//시간통과
                    } else if (q.left.equals("more")) {
                        found = vall >= q.right.right;//"클리어횟수"
                    } else if (q.left.equals("equal")) {
                        found = vall == q.right.right;//"해브"
                    }
                    if (!found) {
                        return;
                    }
                }
            } else if (questid == 1303/*안개바다*/) {
                for (Pair<String, Pair<String, Integer>> q : questInfo) {
                    boolean found = false;
                    final String val = getOneInfo(questid, q.right.left);
                    //dropMessage(6, "val" + val);
                    if (val == null) {
                        return;
                    }
                    int vall = 0;
                    try {
                        vall = Integer.parseInt(val);
                    } catch (NumberFormatException e) {
                        return;
                    }
                    if (q.left.equals("less")) {//시간통과
                    } else if (q.left.equals("more")) {
                        found = vall >= q.right.right;//"클리어횟수"
                    } else if (q.left.equals("equal")) {
                        found = vall == q.right.right;//"해브"
                    }
                    if (!found) {
                        return;
                    }
                }
            }
            //perfectly safe
            updateOneInfo(questid, "rank", newRank);
        }
    }

    public void tryPartyQuest(final int questid) {
        if (MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return;
        }
        try {
            startPartyQuest(questid);
            pqStartTime = System.currentTimeMillis();
            updateOneInfo(questid, "try", String.valueOf(Integer.parseInt(getOneInfo(questid, "try")) + 1));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("tryPartyQuest error");
        }
    }

    public void endPartyQuest(final int questid) {
        if (MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return;
        }
        try {
            startPartyQuest(questid);
            if (pqStartTime > 0) {
                final long changeTime = System.currentTimeMillis() - pqStartTime;
                final int mins = (int) (changeTime / 1000 / 60);//신규
                final int secs = (int) (changeTime / 1000 % 60);
                final int mins2 = Integer.parseInt(getOneInfo(questid, "min"));//기존
                final int secs2 = Integer.parseInt(getOneInfo(questid, "sec"));
                if (mins2 + secs2 == 0/*첫 트라이*/ || mins * 60 + secs < mins2 * 60 + secs2) {
                    updateOneInfo(questid, "min", String.valueOf(mins));
                    updateOneInfo(questid, "sec", String.valueOf(secs));
                    updateOneInfo(questid, "date", FileoutputUtil.CurrentReadable_Date());
                }
                final int newCmp = Integer.parseInt(getOneInfo(questid, "cmp")) + 1;
                //final int newCmp = 120;
                updateOneInfo(questid, "cmp", String.valueOf(newCmp));
                updateOneInfo(questid, "CR", String.valueOf((int) Math.ceil((newCmp * 100.0) / Integer.parseInt(getOneInfo(questid, "try")))));
                recalcPartyQuestRank(questid);
                pqStartTime = 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("endPartyQuest error");
        }

    }

    public int getCmp(int questid) {
        final int newCmp = Integer.parseInt(getOneInfo(questid, "cmp"));
        return newCmp;
    }

    public void havePartyQuest(final int itemId) {
        int questid = 0, index = -1;
        switch (itemId) {
            case 1002798:
                questid = 1200; //henesys
                break;
            case 1072369:
                questid = 1201; //kerning
                break;
            case 1022073:
                questid = 1202; //ludi
                break;
            case 1082232:
                questid = 1203; //orbis
                break;
            case 1002571:
            case 1002572:
            case 1002573:
            case 1002574:
                questid = 1204; //herbtown
                index = itemId - 1002571;
                break;
            case 1102226:
                questid = 1303; //ghost
                break;
            case 1102227:
                questid = 1303; //ghost
                index = 0;
                break;
            case 1122010:
                questid = 1205; //magatia
                break;
            case 1032061:
            case 1032060:
                questid = 1206; //ellin
                index = itemId - 1032060;
                break;
            case 3010018:
                questid = 1300; //ariant
                break;
            case 1122007:
                questid = 1301; //carnival
                break;
            case 1122058:
                questid = 1302; //carnival2
                break;
            default:
                return;
        }
        if (MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return;
        }
        startPartyQuest(questid);
        updateOneInfo(questid, "have" + (index == -1 ? "" : index), "1");//퀘스트 장비 업뎃부분
        if (itemId / 10000 == 521) {
            stats.recalcLocalStats(this);
        }
        if (itemId / 10000 == 410) {
            stats.recalcLocalStats(this);
        }
    }

    public void resetStatsByJob(boolean beginnerJob) {
        int baseJob = (beginnerJob ? (job % 1000) : (((job % 1000) / 100) * 100)); //1112 -> 112 -> 1 -> 100
        boolean UA = getQuestNoAdd(MapleQuest.getInstance(GameConstants.ULT_EXPLORER)) != null;
        if (baseJob == 100 || baseJob == 2100) { //first job = warrior
            resetStats(UA ? 4 : 35, 4, 4, 4);
        } else if (baseJob == 200 || baseJob == 2200 || baseJob == 3200) {
            resetStats(4, 4, UA ? 4 : 20, 4);
        } else if (baseJob == 300 || baseJob == 400 || baseJob == 3300 || baseJob == 3500) {
            resetStats(4, UA ? 4 : 25, 4, 4);
        } else if (baseJob == 500) {
            resetStats(4, UA ? 4 : 20, 4, 4);
        } else if (baseJob == 0) {
            resetStats(4, 4, 4, 4);
        }
    }

    public boolean hasSummon() {
        return hasSummon;
    }

    public void setHasSummon(boolean summ) {
        this.hasSummon = summ;
    }

    public void removeDoor() {
        final MapleDoor door = getDoors().iterator().next();
        for (final MapleCharacter chr : door.getTarget().getCharactersThreadsafe()) {
            door.sendDestroyData(chr.getClient());
        }
        for (final MapleCharacter chr : door.getTown().getCharactersThreadsafe()) {
            door.sendDestroyData(chr.getClient());
        }
        for (final MapleDoor destroyDoor : getDoors()) {
            door.getTarget().removeMapObject(destroyDoor);
            door.getTown().removeMapObject(destroyDoor);
        }
        clearDoors();
    }

    public void removeMechDoor() {
        for (final MechDoor destroyDoor : getMechDoors()) {
            for (final MapleCharacter chr : getMap().getCharactersThreadsafe()) {
                destroyDoor.sendDestroyData(chr.getClient());
            }
            getMap().removeMapObject(destroyDoor);
        }
        if (getMap().mechaDoorTimer != null) {
            getMap().mechaDoorTimer.cancel(true);
            getMap().mechaDoorTimer = null;
        }
        clearMechDoors();
    }

    public void changeRemoval() {
        changeRemoval(false);
    }

    public void changeRemoval(boolean dc) {
        if (getCheatTracker() != null && dc) {
            getCheatTracker().dispose();
        }
        dispelSummons();
        if (!dc) {
//            cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
            int machineType = this.getBuffSource(MapleBuffStat.MECH_CHANGE);
            if (machineType != -1) {
                switch (machineType) {
                    case 35111004:
                    case 35121013: //헤비 머신건만 캔슬
                        cancelEffectFromBuffStat(MapleBuffStat.MECH_CHANGE);
                        break;
                }
            }
            cancelEffectFromBuffStat(MapleBuffStat.RECOVERY);
        }
        if (getPyramidSubway() != null) {
            getPyramidSubway().dispose(this);
        }
        if (playerShop != null && !dc) {
            playerShop.removeVisitor(this, true);
            if (playerShop.isOwner(this)) {
                playerShop.setOpen(true);
            }
        }
        if (!getDoors().isEmpty()) {
            removeDoor();
        }
        if (!getMechDoors().isEmpty()) {
            removeMechDoor();
        }
        NPCScriptManager.getInstance().dispose(client);
    }

    public void updateTick(int newTick) {
        anticheat.updateTick(newTick);
    }

    public boolean canUseFamilyBuff(MapleFamilyBuff buff) {
        final MapleQuestStatus stat = getQuestNoAdd(MapleQuest.getInstance(buff.questID));
        if (stat == null) {
            return true;
        }
        if (stat.getCustomData() == null) {
            stat.setCustomData("0");
        }
        return Long.parseLong(stat.getCustomData()) + (24 * 3600000) < System.currentTimeMillis();
    }

    public void useFamilyBuff(MapleFamilyBuff buff) {
        final MapleQuestStatus stat = getQuestNAdd(MapleQuest.getInstance(buff.questID));
        stat.setCustomData(String.valueOf(System.currentTimeMillis()));
    }

    public List<Integer> usedBuffs() {
        //assume count = 1
        List<Integer> used = new ArrayList<Integer>();
        MapleFamilyBuff[] z = MapleFamilyBuff.values();
        for (int i = 0; i < z.length; i++) {
            if (!canUseFamilyBuff(z[i])) {
                used.add(i);
            }
        }
        return used;
    }

    public String getTeleportName() {
        return teleportname;
    }

    public void setTeleportName(final String tname) {
        teleportname = tname;
    }

    public int getNoJuniors() {
        if (mfc == null) {
            return 0;
        }
        return mfc.getNoJuniors();
    }

    public MapleFamilyCharacter getMFC() {
        return mfc;
    }

    public void makeMFC(final int familyid, final int seniorid, final int junior1, final int junior2) {
        if (familyid > 0) {
            MapleFamily f = World.Family.getFamily(familyid);
            if (f == null) {
                mfc = null;
            } else {
                mfc = f.getMFC(id);
                if (mfc == null) {
                    mfc = f.addFamilyMemberInfo(this, seniorid, junior1, junior2);
                }
                if (mfc.getSeniorId() != seniorid) {
                    mfc.setSeniorId(seniorid);
                }
                if (mfc.getJunior1() != junior1) {
                    mfc.setJunior1(junior1);
                }
                if (mfc.getJunior2() != junior2) {
                    mfc.setJunior2(junior2);
                }
            }
        } else {
            mfc = null;
        }
    }

    public void setFamily(final int newf, final int news, final int newj1, final int newj2) {
        if (mfc == null || newf != mfc.getFamilyId() || news != mfc.getSeniorId() || newj1 != mfc.getJunior1() || newj2 != mfc.getJunior2()) {
            makeMFC(newf, news, newj1, newj2);
        }
    }

    public int getEventBuff() {
        return eventbuff;
    }

    public void setEventBuff(int event) {
        this.eventbuff = event;
    }

    public int getEventBuffTime() {
        return eventbufftime;
    }

    public void setEventBuffTime(int time) {
        this.eventbufftime = time;
    }

    public int getEventBuffUse() {
        return eventbuffuse;
    }

    public int maxBattleshipHP(int skillid) {
        return (getTotalSkillLevel(skillid) * 5000) + ((getLevel() + 10) * 3000);
    }

    public int currentBattleshipHP() {
        return battleshipHP;
    }

    public void setBattleshipHP(int v) {
        this.battleshipHP = v;
    }

    public void decreaseBattleshipHP() {
        this.battleshipHP--;
    }

    public int getGachExp() {
        return gachexp;
    }

    public void setGachExp(int ge) {
        this.gachexp = ge;
    }

    public static int[] Equipments_Bonus = {1114317}; //경험치 부스트 반지

    public static int Equipment_Bonus_EXP(final int itemid) {
        switch (itemid) {
            case 1114317:
                return 0;
        }
        return 0;
    }

    public boolean isInBlockedMap() {
        if (!isAlive() || getMap().getSquadByMap() != null || getEventInstance() != null || getMap().getEMByMap() != null) {
            return true;
        }
        if ((getMapId() >= 680000210 && getMapId() <= 680000502) || (getMapId() / 10000 == 92502 && getMapId() >= 925020100) || (getMapId() / 10000 == 92503) || getMapId() == GameConstants.JAIL) {
            return true;
        }
        /*if ((getMapId() >= 270010200 && getMapId() <= 270030630)) {
         return true;
         }*/
        /*if ((getMapId() >= 271030000 && getMapId() <= 271030600)) {
         return true;
         }*/
        /*if ((getMapId() >= 310070000 && getMapId() <= 320000100)) {
         return true;
         }*/
        for (int i : GameConstants.blockedMaps) {
            if (getMapId() == i) {
                return true;
            }
        }
        return false;
    }

    public boolean isInTownMap() {
        if (hasBlockedInventory() || !getMap().isTown() || FieldLimitType.VipRock.check(getMap().getFieldLimit()) || getEventInstance() != null) {
            return false;
        }
        for (int i : GameConstants.blockedMaps) {
            if (getMapId() == i) {
                return false;
            }
        }
        return true;
    }

    public boolean hasBlockedInventory() {
        return !isAlive() || /*getTrade() != null ||*/ getConversation() > 0 || getDirection() >= 0 || getPlayerShop() != null || map == null;
    }

    public void startPartySearch(final List<Integer> jobs, final int maxLevel, final int minLevel, final int membersNeeded) {
        for (MapleCharacter chr : map.getCharacters()) {
            if (chr.getId() != id && chr.getParty() == null && chr.getLevel() >= minLevel && chr.getLevel() <= maxLevel && (jobs.isEmpty() || jobs.contains(Integer.valueOf(chr.getJob()))) && (isGM() || !chr.isGM())) {
                if (party != null && party.getMembers().size() < 6 && party.getMembers().size() < membersNeeded) {
                    chr.setParty(party);
                    World.Party.updateParty(party.getId(), PartyOperation.JOIN, new MaplePartyCharacter(chr));
                    chr.receivePartyMemberHP();
                    chr.updatePartyMemberHP();
                } else {
                    break;
                }
            }
        }
    }

    public void changedBattler() {
        changed_pokemon = true;
    }

    public int getChallenge() {
        return challenge;
    }

    public void setChallenge(int c) {
        this.challenge = c;
    }

    public short getFatigue() {
        return fatigue;
    }

    public void setFatigue(int j) {
        this.fatigue = (short) Math.max(0, j);
        updateSingleStat(MapleStat.FATIGUE, this.fatigue);
    }

    public void fakeRelog() {
        client.getSession().write(MaplePacketCreator.getCharInfo(this));
        final MapleMap mapp = getMap();
        mapp.setCheckStates(false);
        mapp.removePlayer(this);
        mapp.addPlayer(this);
        mapp.setCheckStates(true);
    }

    public boolean canSummon() {
        return canSummon(5000);
    }

    public boolean canSummon(int g) {
        if (lastSummonTime + g < System.currentTimeMillis()) {
            lastSummonTime = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    public int getIntNoRecord(int questID) {
        final MapleQuestStatus stat = getQuestNoAdd(MapleQuest.getInstance(questID));
        if (stat == null || stat.getCustomData() == null) {
            return 0;
        }
        return Integer.parseInt(stat.getCustomData());
    }

    public int getIntRecord(int questID) {
        final MapleQuestStatus stat = getQuestNAdd(MapleQuest.getInstance(questID));
        if (stat.getCustomData() == null) {
            stat.setCustomData("0");
        }
        return Integer.parseInt(stat.getCustomData());
    }

    public void updatePetAuto() {
        if (getIntNoRecord(GameConstants.HP_ITEM) > 0) {
            client.getSession().write(MaplePacketCreator.petAutoHP(getIntRecord(GameConstants.HP_ITEM)));
        }
        if (getIntNoRecord(GameConstants.MP_ITEM) > 0) {
            client.getSession().write(MaplePacketCreator.petAutoMP(getIntRecord(GameConstants.MP_ITEM)));
        }
    }

    public void sendEnglishQuiz(String msg) {
        // client.getSession().write(MaplePacketCreator.englishQuizMsg(msg));
    }

    public void setChangeTime() {
        mapChangeTime = System.currentTimeMillis();
    }

    public long getChangeTime() {
        return mapChangeTime;
    }

    public Map<ReportType, Integer> getReports() {
        return reports;
    }

    public void addReport(ReportType type) {
        Integer value = reports.get(type);
        reports.put(type, value == null ? 1 : (value + 1));
        changed_reports = true;
    }

    public void clearReports(ReportType type) {
        reports.remove(type);
        changed_reports = true;
    }

    public void clearReports() {
        reports.clear();
        changed_reports = true;
    }

    public final int getReportPoints() {
        int ret = 0;
        for (Integer entry : reports.values()) {
            ret += entry;
        }
        return ret;
    }

    public final String getReportSummary() {
        final StringBuilder ret = new StringBuilder();
        final List<Pair<ReportType, Integer>> offenseList = new ArrayList<Pair<ReportType, Integer>>();
        for (final Entry<ReportType, Integer> entry : reports.entrySet()) {
            offenseList.add(new Pair<ReportType, Integer>(entry.getKey(), entry.getValue()));
        }
        Collections.sort(offenseList, new Comparator<Pair<ReportType, Integer>>() {

            @Override
            public final int compare(final Pair<ReportType, Integer> o1, final Pair<ReportType, Integer> o2) {
                final int thisVal = o1.getRight();
                final int anotherVal = o2.getRight();
                return (thisVal < anotherVal ? 1 : (thisVal == anotherVal ? 0 : -1));
            }
        });
        for (int x = 0; x < offenseList.size(); x++) {
            ret.append(StringUtil.makeEnumHumanReadable(offenseList.get(x).left.name()));
            ret.append(": ");
            ret.append(offenseList.get(x).right);
            ret.append(" ");
        }
        return ret.toString();
    }

    public short getScrolledPosition() {
        return scrolledPosition;
    }

    public void setScrolledPosition(short s) {
        this.scrolledPosition = s;
    }

    public MapleTrait getTrait(MapleTraitType t) {
        return traits.get(t);
    }

    public void forceCompleteQuest(int id) {
        MapleQuest.getInstance(id).forceComplete(this, 9270035); //troll
    }

    public void forceCompleteQuest2(int id) {
        MapleQuest.getInstance(id).forceComplete(this, 2450006); //troll
    }

    public List<Integer> getExtendedSlots() {
        return extendedSlots;
    }

    public int getExtendedSlot(int index) {
        if (extendedSlots.size() <= index || index < 0) {
            return -1;
        }
        return extendedSlots.get(index);
    }

    public void changedExtended() {
        changed_extendedSlots = true;
    }

    public MapleAndroid getAndroid() {
        return android;
    }

    public void removeAndroid() {
        if (map != null) {
            map.broadcastMessage(MaplePacketCreator.deactivateAndroid(this.id));
        }
        android = null;
    }

    public void setAndroid(MapleAndroid a) {
        this.android = a;
        if (map != null && a != null) {
            map.broadcastMessage(MaplePacketCreator.spawnAndroid(this, a));
            map.broadcastMessage(MaplePacketCreator.showAndroidEmotion(this.getId(), Randomizer.nextInt(17) + 1));
        }
    }

    public void setSidekick(MapleSidekick s) {
        this.sidekick = s;
    }

    public MapleSidekick getSidekick() {
        return sidekick;
    }

    public List<Item> getRebuy() {
        return rebuy;
    }

    public MapleImp[] getImps() {
        return imps;
    }

    public void sendImp() {
        for (int i = 0; i < imps.length; i++) {
            if (imps[i] != null) {
//                client.getSession().write(MaplePacketCreator.updateImp(imps[i], ImpFlag.SUMMONED.getValue(), i, true));
            }
        }
    }

    public int getBattlePoints() {
        return pvpPoints;
    }

    public int getTotalBattleExp() {
        return pvpExp;
    }

    public void setBattlePoints(int p) {
        if (p != pvpPoints) {
            //client.getSession().write(UIPacket.getBPMsg(p - pvpPoints));
            //updateSingleStat(MapleStat.BATTLE_POINTS, p);
        }
        this.pvpPoints = p;
    }

    public void setTotalBattleExp(int p) {
        final int previous = pvpExp;
        this.pvpExp = p;
        if (p != previous) {
            stats.recalcPVPRank(this);

            //updateSingleStat(MapleStat.BATTLE_EXP, stats.pvpExp);
            //updateSingleStat(MapleStat.BATTLE_RANK, stats.pvpRank);
        }
    }

    public void changeTeam(int newTeam) {
        this.coconutteam = newTeam;

        if (inPVP()) {
//            client.getSession().write(MaplePacketCreator.getPVPTransform(newTeam + 1));
            map.broadcastMessage(MaplePacketCreator.changeTeam(id, newTeam + 1));
        } else {
            client.getSession().write(MaplePacketCreator.showEquipEffect(newTeam));
        }
    }

    public void disease(int type, int level) {
        if (MapleDisease.getBySkill(type) == null) {
            return;
        }
        chair = 0;
        client.getSession().write(MaplePacketCreator.cancelChair(-1));
        map.broadcastMessage(this, MaplePacketCreator.showChair(id, 0), false);
        giveDebuff(MapleDisease.getBySkill(type), MobSkillFactory.getMobSkill(type, level), (short) 0);
    }

    public boolean inPVP() {
        return eventInstance != null && eventInstance.getName().startsWith("PVP");
    }

    public void clearAllCooldowns() {
        for (MapleCoolDownValueHolder m : getCooldowns()) {
            final int skil = m.skillId;
            removeCooldown(skil);
            client.getSession().write(MaplePacketCreator.skillCooldown(skil, 0));
        }
    }

    public Pair<Double, Boolean> modifyDamageTaken(double damage, MapleMapObject attacke) {
        Pair<Double, Boolean> ret = new Pair<Double, Boolean>(damage, false);
        if (damage <= 0) {
            return ret;
        }
        final Integer div = getBuffedValue(MapleBuffStat.DIVINE_SHIELD);
        final Integer div2 = getBuffedValue(MapleBuffStat.HOLY_MAGIC_SHELL);
        if (div2 != null) {
            if (div2 <= 0) {
                cancelEffectFromBuffStat(MapleBuffStat.HOLY_MAGIC_SHELL);
            } else {
                setBuffedValue(MapleBuffStat.HOLY_MAGIC_SHELL, div2 - 1);
                damage = 0;
            }
        } else if (div != null) {
            if (div <= 0) {
                cancelEffectFromBuffStat(MapleBuffStat.DIVINE_SHIELD);
            } else {
                setBuffedValue(MapleBuffStat.DIVINE_SHIELD, div - 1);
                damage = 0;
            }
        }
        /*final int[] achilles = {1220005, 1120004, 1320005};
         for (int sid : achilles) {
         int slv = getTotalSkillLevel(sid);
         if (slv != 0) {
         damage *= (int) (getEffect(achilles).getX() / 1000.0 * damage);
         dropMessage(6, "아킬레스로 받는 데미지" + damage + " 스킬레벨" + slv);
         }
         }*/
//        MapleStatEffect barrier = getStatForBuff(MapleBuffStat.COMBO_BARRIER);
//        if (barrier != null) {
//            damage =  ((100.0 -barrier.getT()) / 100.0) * damage;//공식 정상화(데미지 이상하게 들어옴)
//            //damage = damage; 데미지값 이상함
//           // System.err.println("take damage:" + damage);
//        }
        MapleStatEffect barrier = getStatForBuff(MapleBuffStat.COMBO_BARRIER);
        if (barrier != null) {
            //this.dropMessage(6, "계산 전 데미지 : " + damage);
            damage -= ((barrier.getT() / 100.0) * damage);
            //this.dropMessage(6, "발생 시점 HP : " + this.getStat().getHp());
            //this.dropMessage(6, "계산 후 데미지 : " + damage);
        }
        barrier = getStatForBuff(MapleBuffStat.MAGIC_SHIELD);
        if (barrier != null) {
            damage = (((100.00 - barrier.getX()) / 100.0) * damage);//공식 정상화
        }
        barrier = getStatForBuff(MapleBuffStat.WATER_SHIELD);
        if (barrier != null) {
            damage = ((barrier.getX() / 100.0) * damage);
        }
        List<Integer> attack = attacke instanceof MapleMonster || attacke == null ? null : (new ArrayList<Integer>());
        if (damage > 0) {
            if (getJob() == 122 && !skillisCooling(1220013)) {
                final Skill divine = SkillFactory.getSkill(1220013);
                if (getTotalSkillLevel(divine) > 0) {
                    final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (divineShield.makeChanceResult()) {
                        divineShield.applyTo(this);
                        client.getSession().write(MaplePacketCreator.skillCooldown(1220013, divineShield.getCooldown()));
                        addCooldown(1220013, System.currentTimeMillis(), divineShield.getCooldown() * 1000);
                    }
                }
            } else if (getBuffedValue(MapleBuffStat.SATELLITESAFE_PROC) != null && getBuffedValue(MapleBuffStat.SATELLITESAFE_ABSORB) != null && getBuffedValue(MapleBuffStat.PUPPET) != null) {
                double buff = getBuffedValue(MapleBuffStat.SATELLITESAFE_PROC).doubleValue();
                double buffz = getBuffedValue(MapleBuffStat.SATELLITESAFE_ABSORB).doubleValue();
                if ((int) ((buff / 100.0) * getStat().getMaxHp()) <= damage) {
                    damage -= ((buffz / 100.0) * damage);
                    cancelEffectFromBuffStat(MapleBuffStat.PUPPET);
                }
            } else if (getJob() == 433 || getJob() == 434) {
                final Skill divine = SkillFactory.getSkill(4330001);
                if (getTotalSkillLevel(divine) > 0 && getBuffedValue(MapleBuffStat.DARKSIGHT) == null && !skillisCooling(divine.getId())) {
                    final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (Randomizer.nextInt(100) < divineShield.getX()) {
                        divineShield.applyTo(this);
                    }
                }
            } else if ((getJob() == 512 || getJob() == 522) && getBuffedValue(MapleBuffStat.PIRATES_REVENGE) == null) {
                final Skill divine = SkillFactory.getSkill(getJob() == 512 ? 5120011 : 5220012);
                if (getTotalSkillLevel(divine) > 0 && !skillisCooling(divine.getId())) {
                    final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (divineShield.makeChanceResult()) {
                        divineShield.applyTo(this);
                        client.getSession().write(MaplePacketCreator.skillCooldown(divine.getId(), divineShield.getX()));
                        addCooldown(divine.getId(), System.currentTimeMillis(), divineShield.getX() * 1000);
                    }
                }
            } else if (getJob() == 312 && attacke != null) {
                final Skill divine = SkillFactory.getSkill(3120010);
                if (getTotalSkillLevel(divine) > 0) {
                    final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (divineShield.makeChanceResult()) {
                        if (attacke instanceof MapleMonster) {
                            final Rectangle bounds = divineShield.calculateBoundingBox(getTruePosition(), isFacingLeft());
                            final List<MapleMapObject> affected = getMap().getMapObjectsInRect(bounds, Arrays.asList(attacke.getType()));
                            int i = 0;

                            for (final MapleMapObject mo : affected) {
                                MapleMonster mons = (MapleMonster) mo;
                                if (mons.getStats().isFriendly() || mons.isFake()) {
                                    continue;
                                }
                                mons.applyStatus(this, new MonsterStatusEffect(MonsterStatus.STUN, 1, divineShield.getSourceId(), null, false), false, divineShield.getDuration(), true, divineShield);
                                final int theDmg = (int) (divineShield.getDamage() * getStat().getCurrentMaxBaseDamage() / 100.0);
                                mons.damage(this, theDmg, true);
                                getMap().broadcastMessage(MobPacket.damageMonster(mons.getObjectId(), theDmg));
                                client.getSession().write(MobPacket.damageMonster(mons.getObjectId(), theDmg));
                                i++;
                                if (i >= divineShield.getMobCount()) {
                                    break;
                                }
                            }
                        } else {
                            MapleCharacter chr = (MapleCharacter) attacke;
                            chr.addHP(-divineShield.getDamage());
                            attack.add((int) divineShield.getDamage());
                        }
                    }
                }
            } else if ((getJob() == 531 || getJob() == 532) && attacke != null) {
                final Skill divine = SkillFactory.getSkill(5310009); //slea.readInt() = 5310009, then slea.readInt() = damage. (175000)
                if (getTotalSkillLevel(divine) > 0) {
                    final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (divineShield.makeChanceResult()) {
                        if (attacke instanceof MapleMonster) {
                            final MapleMonster attacker = (MapleMonster) attacke;
                            final int theDmg = (int) (divineShield.getDamage() * getStat().getCurrentMaxBaseDamage() / 100.0);
                            attacker.damage(this, theDmg, true);
                            getMap().broadcastMessage(MobPacket.damageMonster(attacker.getObjectId(), theDmg));
                        } else {
                            final MapleCharacter attacker = (MapleCharacter) attacke;
                            attacker.addHP(-divineShield.getDamage());
                            attack.add((int) divineShield.getDamage());
                        }
                    }
                }
            } else if (getJob() == 132 && attacke != null) {
                final Skill divine = SkillFactory.getSkill(1320011);
                if (getTotalSkillLevel(divine) > 0 && !skillisCooling(divine.getId()) && getBuffSource(MapleBuffStat.BEHOLDER) == 1321007) {
                    final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (divineShield.makeChanceResult()) {
                        client.getSession().write(MaplePacketCreator.skillCooldown(divine.getId(), divineShield.getCooldown()));
                        addCooldown(divine.getId(), System.currentTimeMillis(), divineShield.getCooldown() * 1000);
                        if (attacke instanceof MapleMonster) {
                            final MapleMonster attacker = (MapleMonster) attacke;
                            final int theDmg = (int) (divineShield.getDamage() * getStat().getCurrentMaxBaseDamage() / 100.0);
                            attacker.damage(this, theDmg, true);
                            getMap().broadcastMessage(MobPacket.damageMonster(attacker.getObjectId(), theDmg));
                        } else {
                            final MapleCharacter attacker = (MapleCharacter) attacke;
                            // attacker.addHP(-divineShield.getDamage());
                            attack.add((int) divineShield.getDamage());
                        }
                    }
                }
            }
            if (attacke != null) {
                final int damr = (Randomizer.nextInt(100) < getStat().DAMreflect_rate ? getStat().DAMreflect : 0);
                final int bouncedam_ = damr + (getBuffedValue(MapleBuffStat.PERFECT_ARMOR) != null ? getBuffedValue(MapleBuffStat.PERFECT_ARMOR) : 0);
                if (bouncedam_ > 0) {
                    long bouncedamage = (long) (damage * bouncedam_ / 100);
                    long bouncer = (long) (damage * damr / 100);
                    damage -= bouncer;
                    if (attacke instanceof MapleMonster) {
                        final MapleMonster attacker = (MapleMonster) attacke;
                        bouncedamage = Math.min(bouncedamage, attacker.getMobMaxHp() / 10);
                        attacker.damage(this, bouncedamage, true);
                        getMap().broadcastMessage(this, MobPacket.damageMonster(attacker.getObjectId(), bouncedamage), getTruePosition());
                        if (getBuffSource(MapleBuffStat.PERFECT_ARMOR) == 31101003) {
                            MapleStatEffect eff = this.getStatForBuff(MapleBuffStat.PERFECT_ARMOR);
                            attacker.applyStatus(this, new MonsterStatusEffect(MonsterStatus.STUN, 1, eff.getSourceId(), null, false), false, eff.getDuration(), true, eff);
                        }
                    } else {
                        final MapleCharacter attacker = (MapleCharacter) attacke;
                        bouncedamage = Math.min(bouncedamage, attacker.getStat().getCurrentMaxHp() / 10);
                        attacker.addHP(-((int) bouncedamage));
                        attack.add((int) bouncedamage);
                        if (getBuffSource(MapleBuffStat.PERFECT_ARMOR) == 31101003) {
                            attacker.disease(MapleDisease.STUN.getDisease(), 1);
                        }
                    }
                    ret.right = true;
                }
                if ((getJob() == 411 || getJob() == 412 || getJob() == 421 || getJob() == 422) && getBuffedValue(MapleBuffStat.SUMMON) != null && attacke != null) {
                    final List<MapleSummon> ss = getSummonsReadLock();
                    try {
                        for (MapleSummon sum : ss) {
                            if (sum.getTruePosition().distanceSq(getTruePosition()) < 400000.0 && (sum.getSkill() == 4111007 || sum.getSkill() == 4211007)) {
                                final List<Pair<Integer, Integer>> allDamage = new ArrayList<Pair<Integer, Integer>>();
                                if (attacke instanceof MapleMonster) {
                                    final MapleMonster attacker = (MapleMonster) attacke;
                                    final int theDmg = (int) (SkillFactory.getSkill(sum.getSkill()).getEffect(sum.getSkillLevel()).getX() * damage / 100.0);
                                    allDamage.add(new Pair<Integer, Integer>(attacker.getObjectId(), theDmg));
                                    getMap().broadcastMessage(MaplePacketCreator.summonAttack(sum.getOwnerId(), sum.getObjectId(), (byte) 0x84, allDamage, getLevel(), true));
                                    attacker.damage(this, theDmg, true);
                                    checkMonsterAggro(attacker);
                                    if (!attacker.isAlive()) {
                                        getClient().getSession().write(MobPacket.killMonster(attacker.getObjectId(), 1));
                                    }
                                } else {
                                    final MapleCharacter chr = (MapleCharacter) attacke;
                                    final int dmg = SkillFactory.getSkill(sum.getSkill()).getEffect(sum.getSkillLevel()).getX();
                                    chr.addHP(-dmg);
                                    attack.add(dmg);
                                }
                            }
                        }
                    } finally {
                        unlockSummonsReadLock();
                    }
                }
            }
        }
        if (attack != null && attack.size() > 0 && attacke != null) {
            getMap().broadcastMessage(MaplePacketCreator.pvpCool(attacke.getObjectId(), attack));
        }
        ret.left = damage;
        return ret;
    }

    public void onAttack(long maxhp, int maxmp, int skillid, int oid, int totDamage) {
        if (stats.hpRecoverProp > 0) {
            if (Randomizer.nextInt(100) <= stats.hpRecoverProp) {//i think its out of 100, anyway
                if (stats.hpRecover > 0) {
                    healHP(stats.hpRecover);
                }
                if (stats.hpRecoverPercent > 0) {
                    addHP(((int) Math.min(maxhp, Math.min(((int) ((double) totDamage * (double) stats.hpRecoverPercent / 100.0)), stats.getMaxHp() / 2))));
                }
            }
        }
        if (stats.mpRecoverProp > 0 && (!GameConstants.isDemon(getJob()) || GameConstants.isForceIncrease(skillid))) {
            if (Randomizer.nextInt(100) <= stats.mpRecoverProp) {//i think its out of 100, anyway

                healMP(stats.mpRecover);

            }
        }
        if (getBuffedValue(MapleBuffStat.COMBO_DRAIN) != null) {
            addHP(((int) Math.min(maxhp, Math.min(((int) ((double) totDamage * (double) getStatForBuff(MapleBuffStat.COMBO_DRAIN).getX() / 100.0)), stats.getMaxHp() / 2))));
        }
        if (getBuffSource(MapleBuffStat.COMBO_DRAIN) == 23101003) {
            addMP(((int) Math.min(maxmp, Math.min(((int) ((double) totDamage * (double) getStatForBuff(MapleBuffStat.COMBO_DRAIN).getX() / 100.0)), stats.getMaxMp() / 2))));
        }
        if (getJob() == 212 || getJob() == 222 || getJob() == 232) {
            int[] skills = {2120010, 2220010, 2320011};
            for (int i : skills) {
                final Skill skill = SkillFactory.getSkill(i);
                if (getTotalSkillLevel(skill) > 0) {
                    final MapleStatEffect venomEffect = skill.getEffect(getTotalSkillLevel(skill));
                    if (venomEffect.makeChanceResult() && getAllLinkMid().size() < venomEffect.getY()) {
                        setLinkMid(oid, venomEffect.getX());
                        venomEffect.applyTo(this);
                    }
                    break;
                }
            }
        }
        // effects
        if (skillid > 0) {
            final Skill skil = SkillFactory.getSkill(skillid);
            final MapleStatEffect effect = skil.getEffect(getTotalSkillLevel(skil));
            switch (skillid) {
                case 15111001:
                case 3111008:
                case 1078:
                case 31111003:
                case 11078:
                case 14101006:
                case 33111006: //swipe
                case 4101005: //drain
                case 5111004: { // Energy Drain
                    addHP(((int) Math.min(maxhp, Math.min(((int) ((double) totDamage * (double) effect.getX() / 100.0)), stats.getMaxHp() / 2))));
                    break;
                }
                case 5211006:
                case 22151002: //killer wing
                case 5220011: {//homing
                    clearLinkMid();
                    setLinkMid(oid, effect.getX());
                    break;
                }
                case 33101007: { //jaguar
                    clearLinkMid();
                    break;
                }
            }
        }
    }

    public void afterAttack(int mobCount, int attackCount, int skillid) {
        switch (getJob()) {
            case 511:
            case 512: {
                handleEnergyCharge(5110001, mobCount * attackCount);
                break;
            }
            case 1510:
            case 1511:
            case 1512: {
                handleEnergyCharge(15100004, mobCount * attackCount);
                break;
            }
            case 400:
            case 410:
            case 411:
            case 412: {
                if (skillid != 4001003 & getBuffedValue(MapleBuffStat.DARKSIGHT) != null) {
                    cancelEffectFromBuffStat(MapleBuffStat.DARKSIGHT);
                }
                break;
            }
            case 1300:
            case 1310:
            case 1311:
            case 1312: {
                if (skillid != 13101006 & getBuffedValue(MapleBuffStat.WIND_WALK) != null) {
                    cancelEffectFromBuffStat(MapleBuffStat.WIND_WALK);
                }
                break;
            }
            case 1400:
            case 1410:
            case 1411:
            case 1412: {
                if (skillid != 14001003 & getBuffedValue(MapleBuffStat.DARKSIGHT) != null) {
                    cancelEffectFromBuffStat(MapleBuffStat.DARKSIGHT);
                }
                break;
            }
            case 111:
            case 112:
            case 1111:
            case 1112:
                if ((((skillid != 1111008) ? 1 : 0) & ((skillid != 1111003) ? 1 : 0) & ((skillid != 1111004) ? 1 : 0) & ((skillid != 1111005) ? 1 : 0) & ((skillid != 1111006) ? 1 : 0) & ((skillid != 11111002) ? 1 : 0) & ((skillid != 11111003) ? 1 : 0) & ((getBuffedValue(MapleBuffStat.COMBO) != null) ? 1 : 0)) != 0) {
                    handleOrbgain();
                }
                break;
        }
        if (getBuffedValue(MapleBuffStat.OWL_SPIRIT) != null) {
            if (currentBattleshipHP() > 0) {
                decreaseBattleshipHP();
            }
            if (currentBattleshipHP() <= 0) {
                cancelEffectFromBuffStat(MapleBuffStat.OWL_SPIRIT);
            }
        }
        if (!isIntern()) {
            cancelEffectFromBuffStat(MapleBuffStat.WIND_WALK);
            cancelEffectFromBuffStat(MapleBuffStat.INFILTRATE);
            final MapleStatEffect ds = getStatForBuff(MapleBuffStat.DARKSIGHT);
            if (ds != null) {
                if (ds.getSourceId() != 4330001 || !ds.makeChanceResult()) {
                    cancelEffectFromBuffStat(MapleBuffStat.DARKSIGHT);
                }
            }
        }
    }

    public void applyIceGage(int x) {
        updateSingleStat(MapleStat.ICE_GAGE, x);
    }

    public int[] blockItems() {
        /* [금지된 아이템] */ //금지
        int[] blocks = {1004642, 1122017, 1004532, 1003010, 1004711, 1012008, 1004727, 1702525, 1004725, 1004730, 1102873, 1102907, 1102888, 1102890, 1102894, 1102897, 1115100, 1115101, 1115102, 1115196, 1115012, 1115013, 1115014, 1004641, 1050388, 1051458, 1082666, 1082667, 1082668, 1004597, 1004598, 1004599, 1073090, 1073091, 1073092, 1102095, 1102096, 1102097, 1102899, 1102837, 1003142};
        return blocks;
    }

    public String getSearchCashItem(String itemName) {
        String Text = "";

        if (itemName.length() < 2) {
            return "두글자 이상 입력해주세요\r\n\r\n#b#L01#돌아가기#l";
        }

        if (itemName.equals("")) {
            return "무엇을 갖고싶은건데?";
        }

        //System.out.println(itemName);
        Connection con = null;
        PreparedStatement ps = null;
        PreparedStatement pse = null;
        ResultSet rs = null;
        ResultSet rse = null;
        int stat = 0;
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM wz_itemdata WHERE name like '%" + itemName + "%'");

            rs = ps.executeQuery();

            if (!rs.first()) {
                return "입력하신 아이템이 존재하지 않습니다\r\n\r\n#b#L01#돌아가기#l";
            }

            while (rs.next()) {
                final Map<String, Integer> stats = ii.getEquipStats(rs.getInt("itemid"));

                if (ii.isCash(rs.getInt("itemid")) && ii.itemExists(rs.getInt("itemid")) && rs.getInt("itemid") / 1000000 == 1) {
                    pse = con.prepareStatement("SELECT * FROM wz_itemequipdata WHERE (itemid = ?) && (`Key` = 'STR' || `Key` = 'DEX' || `Key` = 'LUK' || `Key` = 'INT' || `Key` = 'PAD' || `Key` = 'MAD')");
                    pse.setInt(1, rs.getInt("itemid"));
                    rse = pse.executeQuery();

                    while (rse.next()) {
                        stat += rse.getInt("value");
                    }

                    for (int i = 0; i < blockItems().length; i++) {
                        if (rs.getInt("itemid") == blockItems()[i]) {
                            stat++;
                        }
                    }

                    if (stat == 0) {
                        Text += "#L" + rs.getInt("itemid") + "##b#v" + rs.getInt("itemid") + "##l ";
                    }
                }
            }

            ps.close();
            rse.close();
            rs.close();
            pse.close();
            con.close();

        } catch (SQLException e) {
            System.err.println("Unable to show note" + e);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }
            if (rse != null) {
                try {
                    rse.close();
                } catch (Exception e) {
                }
            }
            if (pse != null) {
                try {
                    pse.close();
                } catch (Exception e) {
                }
            }
        }

        if (Text.equals("") || Text.length() < 2) {
            Text = "입력하신 아이템이 존재하지 않습니다\r\n\r\n#b#L01#돌아가기#l";
        } else {
            Text = "가지고 싶은 캐시템을 검색해주세요!\r\n셀렉션 조작시 자동벤 처리됩니다.\r\n\r\n" + Text;
        }

        return Text;
    }

    public final void gainItem(final int id, final short quantity, final boolean randomStats, final long period, String gm_log) {
        if (quantity < 0 || quantity > 9999) {
            AutobanManager.getInstance().autoban(getClient(), "복사버그 (gainItem)");
            getClient().getSession().close();
            return;
        }
        if (quantity >= 0) {
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            final MapleInventoryType type = GameConstants.getInventoryType(id);

            if (!MapleInventoryManipulator.checkSpace(client, id, quantity, "")) {
                return;
            }
            if (type.equals(MapleInventoryType.EQUIP) && !GameConstants.isThrowingStar(id) && !GameConstants.isBullet(id)) {
                final Item item = randomStats ? ii.randomizeStats2((Equip) ii.getEquipById(id), true) : ii.getEquipById(id);
                if (period > 0) {
                    item.setExpiration(System.currentTimeMillis() + period);
                }
                item.setGMLog(System.currentTimeMillis() + "에 " + gm_log);

                MapleInventoryManipulator.addbyItem(client, item);
            } else {
                MapleInventoryManipulator.addById(client, id, quantity, "", null, period, System.currentTimeMillis() + "에 " + getName() + "에서 호출된 gainItem 스크립트로 얻은 아이템.");
            }
        } else {
            MapleInventoryManipulator.removeById(client, GameConstants.getInventoryType(id), id, -quantity, true, false);
        }
        client.getSession().write(MaplePacketCreator.getShowItemGain(id, quantity, true));
    }

    public void goDonateShop(boolean bln) {
        goDonateCashShop = bln;
    }

    public boolean isDonateShop() {
        return goDonateCashShop;
    }

    public Rectangle getBounds() {
        return new Rectangle(getTruePosition().x - 25, getTruePosition().y - 75, 50, 75);
    }

    private transient ScheduledFuture<?> fishTimer;
    private int fishTasking = 0;
    private final int bait = 4001126;
    public final int[] fishingMap = {741000200, 741000201, 741000202, 741000203, 741000204, 741000205, 741000206, 741000207, 741000208, 910000000};
    public int[] fishingChair = {3010000, 3010024, 3010026, 3010023, 3010022, 3010019, 3010045, 3010036, 3010013, 3010009, 3010011, 3010001, 3010109, 3010110, 3010097, 3010060, 3010157, 3010154, 3010113, 3010114, 3010133, 3010132, 3010129, 3010131, 3010081, 3010082, 3010083, 3010084, 3010028, 3010040, 3010005, 3010004, 3010006, 3010062, 3010002, 3010003, 3010130, 3010131, 3010132, 3010133, 3010134};

    public final void fishingTimer(int time) {
        getClient().getSession().write(MaplePacketCreator.serverNotice(5, "[체어 마일리지 안내]"));
        getClient().getSession().write(MaplePacketCreator.serverNotice(5, "① 의자에 앉아 체어 마일리지 를 60초당 1점씩 획득합니다."));
        getClient().getSession().write(MaplePacketCreator.serverNotice(5, "② 도중에 의자에서 일어나게 되면 기존 점수는 누적되지 않습니다."));
        getClient().getSession().write(MaplePacketCreator.serverNotice(5, "③ 체어 마일리지 는 시간 경과 시 아이템으로 바로 지급됩니다."));
        getClient().getSession().write(MaplePacketCreator.serverNotice(5, "④ 기타슬롯 2칸이상을 비워놔야 원활한 체어 마일리지 획득이 가능합니다."));
        fishTimer = MapTimer.getInstance().register(new Runnable() {
            public void run() {
                if (fishTasking < 10) {
                    fishTasking++;
                    String gage = "";
                    for (int i = 0; i <= fishTasking; i++) {
                        gage += "■";
                    }
                    for (int i = 9; i > fishTasking; i--) {
                        gage += "□";
                    }
                    getClient().getSession().write(MaplePacketCreator.sendHint("체어 마일리지 획득중...\r\n" + gage, 250, 60));
                } else {
                    fishTasking = 0;
                    /*if (getItemQuantity(bait, false) == 0) {
                     cancelFishing();
                     return;
                     }*/
                    // int[] items = {4031635,4031636,4031639,4031640,4031642,4031643,4031644,4031645,4031646,4031647,4031648,4260000,4260001,4260002,4260003,4260004,4260005,4260006,4260007,4260008, 2000004, 2020015, 4001126};
                    int[] items = {4310200};
                    int randItem = 4310200; //items[Randomizer.nextInt(items.length - 1)];
                    int chance = (short) Randomizer.rand(1, 100);
                    if (chance < 0) {
                        //gainItem(bait, (short) -1, false, -1, null);
                        //dropMessage(5, "아무것도 낚지 못했습니다.");
                    } else {

                        if (getInventory(MapleInventoryType.ETC).getNumFreeSlot() < 2) {
                            dropMessage(5, "[안내] 인벤토리 슬롯이 부족하여 체어 마일리지 를 획득 하지 못했습니다.");
                        } else {
                            MapleInventoryManipulator.addById(getClient(), randItem, (short) 1, null);
                            //gainItem(bait, (short) -1, false, -1, null);
                            dropMessage(5, "[안내] 60초가 경과해 체어 마일리지 1개를 획득했습니다.");
                        }
                    }
                    //MapleInventoryManipulator.addById(getClient(), bait, (short) -1, null);
                }
            }
        }, time / 10, time / 10);
    }

    public final void cancelFishing() {
        if (fishTimer != null) {
            if (getMapId() == 910000000) {
                getClient().getSession().write(MaplePacketCreator.serverNotice(5, "의자에서 일어나 체어 마일리지 획득을 그만두었습니다."));
            }
            fishTimer.cancel(false);
            fishTimer = null;
        }
    }

    public int getPrimium() {
        return primium_time;
    }

    public void setSaveLocation(int mapid) {
        MapleQuestStatus q = getQuestNAdd(MapleQuest.getInstance(201544));

        if (q == null) {
            q.setCustomData("0");
        }

        q.setCustomData(String.valueOf(mapid));
    }

    public int getSavedLocation() {
        MapleQuestStatus q = getQuestNAdd(MapleQuest.getInstance(201544));

        if (q == null) {
            q.setCustomData("0");
        }

        return Integer.valueOf(q.getCustomData());
    }

    public final void changeMusic(final String songName) {
        getClient().getSession().write(MaplePacketCreator.musicChange(songName));
    }

    /*  public void toggleStrongBuff() {
     this.usingStrongBuff = !usingStrongBuff;
     }

     public boolean isStrongBuff() {
     return usingStrongBuff;
     }    */
    public String getRouterIP() {

        String _255 = "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
        String exIP = "(?:" + _255 + "\\.){3}" + _255;

        // Regexp to find the good line
        java.util.regex.Pattern pat = java.util.regex.Pattern.compile("^\\s*(?:0\\.0\\.0\\.0\\s*){1,2}(" + exIP + ").*");
        Process proc;
        try {

            // netstat
            proc = Runtime.getRuntime().exec("netstat -rn");

            java.io.InputStream inputstream
                    = proc.getInputStream();
            java.io.InputStreamReader inputstreamreader
                    = new java.io.InputStreamReader(inputstream);
            java.io.BufferedReader bufferedreader
                    = new java.io.BufferedReader(inputstreamreader);

            // Parsing the result
            String line;
            while ((line = bufferedreader.readLine()) != null) {
                java.util.regex.Matcher m = pat.matcher(line);

                // This is the good line
                if (m.matches()) {

                    // return the first group
                    return m.group(1);
                }
            }
            // can't find netstat
        } catch (java.io.IOException ex) {
            //java.util.logging.Logger.getLogger(NetworkManager.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println(ex.getMessage());
        }

        return "해당 없음";
    }

    public void changeAccGender(byte gender) {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("UPDATE accounts SET gender = " + gender + " WHERE id = " + client.getAccID());
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
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
    }

    public boolean getLeaf() {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM gaincoin WHERE name = '" + getName() + "'");
            rs = ps.executeQuery();

            if (rs.next()) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }
            if (rs != null) {
                try {
                    rs.close();
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
        return false;
    }

    public void gainDPoint(int point) {
        MapleQuestStatus q = getQuestNAdd(MapleQuest.getInstance(20150425));

        if (q.getCustomData() == null) {
            q.setCustomData("0");
        }

        q.setCustomData(String.valueOf(Integer.valueOf(q.getCustomData()) + point));
    }

    public int getDPoint() {
        MapleQuestStatus q = getQuestNAdd(MapleQuest.getInstance(20150425));

        if (q.getCustomData() == null) {
            q.setCustomData("0");
        }

        return Integer.valueOf(q.getCustomData());
    }

    public void gainVPoint(int point) {
        MapleQuestStatus q = getQuestNAdd(MapleQuest.getInstance(20150425));

        if (q.getCustomData() == null) {
            q.setCustomData("0");
        }

        q.setCustomData(String.valueOf(Integer.valueOf(q.getCustomData()) + point));
    }

    public int getVPoint() {
        MapleQuestStatus q = getQuestNAdd(MapleQuest.getInstance(20150425));

        if (q.getCustomData() == null) {
            q.setCustomData("0");
        }

        return Integer.valueOf(q.getCustomData());
    }

    public MapleQuestStatus getQuest(int quest) {
        MapleQuestStatus q = getQuestNAdd(MapleQuest.getInstance(quest));

        if (q.getCustomData() == null) {
            q.setCustomData("0");
        }

        return q;
    }

    public void removeAllEquip(int id, boolean show) {
        MapleInventoryType type = GameConstants.getInventoryType(id);
        int possessed = getInventory(type).countById(id);

        if (possessed > 0) {
            MapleInventoryManipulator.removeById(getClient(), type, id, possessed, true, false);
            if (show) {
                getClient().getSession().write(MaplePacketCreator.getShowItemGain(id, (short) -possessed, true));
            }
        }
        if (type == MapleInventoryType.EQUIP) { //check equipped
            type = MapleInventoryType.EQUIPPED;
            possessed = getInventory(type).countById(id);

            if (possessed > 0) {
                Item equip = getInventory(type).findById(id);
                if (equip != null) {
                    getInventory(type).removeSlot(equip.getPosition());
                    equipChanged();
                    getClient().getSession().write(MaplePacketCreator.dropInventoryItem(MapleInventoryType.EQUIP, equip.getPosition()));
                }
            }
        }
    }

    public final void gainItem(final int id, final short quantity) {
        gainItem(id, quantity, false, 0, -1, "");
    }

    public final void gainItem(final int id, final short quantity, final boolean randomStats) {
        gainItem(id, quantity, randomStats, 0, -1, "");
    }

    public final void gainItem(final int id, final short quantity, final boolean randomStats, final int slots) {
        gainItem(id, quantity, randomStats, 0, slots, "");
    }

    public final void gainItem(final int id, final short quantity, final long period) {
        gainItem(id, quantity, false, period, -1, "");
    }

    public final void gainItem(final int id, final short quantity, final boolean randomStats, final long period, final int slots) {
        gainItem(id, quantity, randomStats, period, slots, "");
    }

    public final void gainItem(final int id, final short quantity, final boolean randomStats, final long period, final int slots, final String owner) {
        gainItem(id, quantity, randomStats, period, slots, owner, client);
    }

    public final void gainItem(final int id, final short quantity, final boolean randomStats, final long period, final int slots, final String owner, final MapleClient cg) {
        if (quantity < 0 || quantity > 9999) {
            //AutobanManager.getInstance().autoban(getClient(), "복사버그 (gainItem2)");
            //getClient().getSession().close();
            //return;
        }
        if (quantity >= 0) {
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            final MapleInventoryType type = GameConstants.getInventoryType(id);
            ServerLogger.getInstance().logItem(LogType.Item.FromScript, getId(), getName(), id, quantity, ii.getName(id), 0, "");

            if (!MapleInventoryManipulator.checkSpace(cg, id, quantity, "")) {
                return;
            }
            if (type.equals(MapleInventoryType.EQUIP) && !GameConstants.isThrowingStar(id) && !GameConstants.isBullet(id)) {
                final Equip item = (Equip) (randomStats ? ii.randomizeStats((Equip) ii.getEquipById(id)) : ii.getEquipById(id));
                if (period > 0) {
                    item.setExpiration(System.currentTimeMillis() + (period * 24 * 60 * 60 * 1000));
                }
                if (slots > 0) {
                    item.setUpgradeSlots((byte) (item.getUpgradeSlots() + slots));
                }
                if (owner != null) {
                    item.setOwner(owner);
                }
                final String name = ii.getName(id);
                if (id / 10000 == 114 && name != null && name.length() > 0) { //medal
                    final String msg = "훈장을 얻었습니다 <" + name + ">";
                    cg.getPlayer().dropMessage(5, msg);
                }
                MapleInventoryManipulator.addbyItem(cg, item.copy());
            } else {
                MapleInventoryManipulator.addById(cg, id, quantity, owner == null ? "" : owner, null, period, "Received from interaction " + this.id + " () on " + FileoutputUtil.CurrentReadable_Date());
            }
        } else {
            MapleInventoryManipulator.removeById(cg, GameConstants.getInventoryType(id), id, -quantity, true, false);
        }
        cg.getSession().write(MaplePacketCreator.getShowItemGain(id, quantity, true));
    }

    public void showQuestCompletion(int qid) {
        client.getSession().write(MaplePacketCreator.getShowQuestCompletion(qid));
    }

    public void setGMLevel(byte i) {
        this.gmLevel = i;
    }

    public void setEquippedTimeAll() {
        long now = System.currentTimeMillis();
        for (Item item : getInventory(MapleInventoryType.EQUIPPED)) {
            item.setEquippedTime(now);
        }
    }

    public void acceptUpdate() {
        this.updateAccepted = true;
    }

    public void updateBonusExp(long now) {
        if (!this.updateAccepted) {
            return;
        }
        int slot = 0;
        String itemName = "";
        int hour = 0;
        int applyExpR = 0;
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        for (Item item : getInventory(MapleInventoryType.EQUIPPED)) {
            int itemId = item.getItemId();
            ItemInformation info = ii.getItemInformation(itemId);
            if (info != null) {
                if (info.bonusExps != null) {
                    long equippedTime = item.getEquippedTime();
                    for (StructBonusExp struct : info.bonusExps) {
                        if (struct.checkTerm(now, equippedTime) && applyExpR < struct.incExpR) {
                            slot = -item.getPosition();
                            itemName = ii.getName(itemId);
                            hour = struct.termStart;
                            applyExpR = struct.incExpR;
                        }
                    }
                }
            }
        }
        if (this.bonusExpR != applyExpR) {
            this.bonusExpR = applyExpR;
            if (applyExpR != 0) {
                if (hour == 0) {
                    client.getSession().write(MaplePacketCreator.EquipfairyPendant1S(applyExpR));
                    client.getSession().write(MaplePacketCreator.EquipfairyPendant2S(applyExpR));
                } else {
                    client.getSession().write(MaplePacketCreator.IncreasefairyPendant1S(hour, applyExpR));
                    client.getSession().write(MaplePacketCreator.IncreasefairyPendant2S(hour, applyExpR));
                }
            }
        }
    }

    public String checkDrop(int mobId) {
        ArrayList<Integer> ret = new ArrayList<Integer>();
        ArrayList<Integer> ret2 = new ArrayList<Integer>();

        final List<MonsterDropEntry> ranks = new ArrayList<>();
        final List<MonsterDropEntry> original = MapleMonsterInformationProvider.getInstance().retrieveDrop(mobId);

        if (original != null) {
            ranks.addAll(original);
        }

        MapleMonster mob = MapleLifeFactory.getMonster(mobId);
        boolean MonsterPark = (mob.getId() >= 9800000 && mob.getId() <= 9800124);
        /*if (MonsterPark) {
         ranks.add(new MonsterDropEntry(4310020, 50000000, 1, 1, 0));
         }*/
        if (getId() >= 240000000 && getId() <= 240040610) { //리프레 //소스드롭 드랍
            ranks.add(new MonsterDropEntry(3961001, 150, 1, 1, 0));
        }
        if (getId() >= 211060000 && getId() <= 211070200) { //사자성
            ranks.add(new MonsterDropEntry(3962001, 150, 1, 1, 0));
        }
        if (getId() >= 270010100 && getId() <= 270030503) { //타임로드
            ranks.add(new MonsterDropEntry(3963001, 150, 1, 1, 0));
        }
        if (getId() >= 271030000 && getId() <= 271030540) { //무기고 //(getId() == 271030320) { //무기고
            ranks.add(new MonsterDropEntry(3964001, 150, 1, 1, 0));
        }
        if (getId() >= 271010000 && getId() <= 271020100) { //파괴된 헤네시스
            ranks.add(new MonsterDropEntry(3965001, 150, 1, 1, 0));
        }
        if (getId() >= 273000000 && getId() <= 273060300) { //황혼의 페리온
            ranks.add(new MonsterDropEntry(3966001, 150, 1, 1, 0));
        }

        if ((mob.getStats().getLevel() >= 0 && mob.getStats().getLevel() <= 120) && !mob.getStats().isBoss()) {//10000 1퍼
            ranks.add(new MonsterDropEntry(0, 999999, mob.getStats().getLevel() * 4, mob.getStats().getLevel() * 6, 0));//메소
        }
        if ((mob.getStats().getLevel() >= 121 && mob.getStats().getLevel() <= 199) && !mob.getStats().isBoss()) {//10000 1퍼
            ranks.add(new MonsterDropEntry(0, 999999, mob.getStats().getLevel() * 8, mob.getStats().getLevel() * 10, 0));//메소
        }
        if ((mob.getStats().getLevel() >= 200 && mob.getStats().getLevel() <= 279) && mob.getStats().isBoss()) {//10000 1퍼 140 140000
            ranks.add(new MonsterDropEntry(0, 999999, mob.getStats().getLevel() * 14, mob.getStats().getLevel() * 17, 0));//메소
        }
        if ((mob.getStats().getLevel() > 280 && mob.getStats().getLevel() <= 399) && mob.getStats().isBoss()) {//10000 1퍼 140 140000
            ranks.add(new MonsterDropEntry(0, 999999, mob.getStats().getLevel() * 18, mob.getStats().getLevel() * 20, 0));//메소
        }
        if ((mob.getStats().getLevel() >= 400 && mob.getStats().getLevel() <= 599) && mob.getStats().isBoss()) {//10000 1퍼 140 140000
            ranks.add(new MonsterDropEntry(0, 999999, mob.getStats().getLevel() * 22, mob.getStats().getLevel() * 24, 0));//메소
        }
        if ((mob.getStats().getLevel() >= 600 && mob.getStats().getLevel() <= 799) && mob.getStats().isBoss()) {//10000 1퍼 140 140000
            ranks.add(new MonsterDropEntry(0, 999999, mob.getStats().getLevel() * 26, mob.getStats().getLevel() * 28, 0));//메소
        }
        if ((mob.getStats().getLevel() >= 800 && mob.getStats().getLevel() <= 999) && mob.getStats().isBoss()) {//
            ranks.add(new MonsterDropEntry(0, 999999, mob.getStats().getLevel() * 30, mob.getStats().getLevel() * 32, 0));//메소
        }
        Calendar cal = Calendar.getInstance();
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        switch (dayOfWeek) {
            case 2: //월
                if (mob.getStats().getLevel() >= 110 && MonsterPark) {//(코드,확률,최소갯수,최대갯수,퀘스트코드)
                    ranks.add(new MonsterDropEntry(2434746, 1500, 1, 1, 0));//월상자
                }
                if (mob.getStats().getLevel() >= 50 && mob.getStats().getLevel() <= 110) {
                    ranks.add(new MonsterDropEntry(5062002, 1500, 1, 1, 0));//미큐               
                }
                if (mob.getStats().getLevel() > 110 && mob.getStats().getLevel() <= 160) {
                    ranks.add(new MonsterDropEntry(5062002, 3000, 1, 1, 0));//미큐               
                }
                if (mob.getStats().getLevel() > 160 && mob.getStats().getLevel() <= 999) {
                    ranks.add(new MonsterDropEntry(5062002, 5000, 1, 1, 0));//미큐               
                }
                break;
            case 3: //화
                if (mob.getStats().getLevel() >= 110 && MonsterPark) {//(코드,확률,최소갯수,최대갯수,퀘스트코드)
                    ranks.add(new MonsterDropEntry(2434747, 1500, 1, 1, 0));//화상자
                }
                if (mob.getStats().getLevel() >= 50 && mob.getStats().getLevel() <= 110) {
                    ranks.add(new MonsterDropEntry(5062002, 1500, 1, 1, 0));//미큐               
                }
                if (mob.getStats().getLevel() > 110 && mob.getStats().getLevel() <= 160) {
                    ranks.add(new MonsterDropEntry(5062002, 3000, 1, 1, 0));//미큐               
                }
                if (mob.getStats().getLevel() > 160 && mob.getStats().getLevel() <= 999) {
                    ranks.add(new MonsterDropEntry(5062002, 5000, 1, 1, 0));//미큐               
                }
                break;
            case 4: //수
                if (mob.getStats().getLevel() >= 110 && MonsterPark) {//(코드,확률,최소갯수,최대갯수,퀘스트코드)
                    ranks.add(new MonsterDropEntry(2434748, 1500, 1, 1, 0));//수상자
                }
                if (mob.getStats().getLevel() >= 50 && mob.getStats().getLevel() <= 110) {
                    ranks.add(new MonsterDropEntry(5062002, 1500, 1, 1, 0));//미큐               
                }
                if (mob.getStats().getLevel() > 110 && mob.getStats().getLevel() <= 160) {
                    ranks.add(new MonsterDropEntry(5062002, 3000, 1, 1, 0));//미큐               
                }
                if (mob.getStats().getLevel() > 160 && mob.getStats().getLevel() <= 999) {
                    ranks.add(new MonsterDropEntry(5062002, 5000, 1, 1, 0));//미큐               
                }
                break;
            case 5: //목
                if (mob.getStats().getLevel() >= 110 && MonsterPark) {//(코드,확률,최소갯수,최대갯수,퀘스트코드)
                    ranks.add(new MonsterDropEntry(2434749, 1500, 1, 1, 0));//목상자
                }
                if (mob.getStats().getLevel() >= 50 && mob.getStats().getLevel() <= 110) {
                    ranks.add(new MonsterDropEntry(5062002, 1500, 1, 1, 0));//미큐               
                }
                if (mob.getStats().getLevel() > 110 && mob.getStats().getLevel() <= 160) {
                    ranks.add(new MonsterDropEntry(5062002, 3000, 1, 1, 0));//미큐               
                }
                if (mob.getStats().getLevel() > 160 && mob.getStats().getLevel() <= 999) {
                    ranks.add(new MonsterDropEntry(5062002, 5000, 1, 1, 0));//미큐               
                }
                break;
            case 6: //금
                if (mob.getStats().getLevel() >= 110 && MonsterPark) {//(코드,확률,최소갯수,최대갯수,퀘스트코드)
                    ranks.add(new MonsterDropEntry(2434750, 1500, 1, 1, 0));//금상자
                }
                if (mob.getStats().getLevel() >= 50 && mob.getStats().getLevel() <= 110) {
                    ranks.add(new MonsterDropEntry(5062002, 1500, 1, 1, 0));//미큐               
                }
                if (mob.getStats().getLevel() > 110 && mob.getStats().getLevel() <= 160) {
                    ranks.add(new MonsterDropEntry(5062002, 3000, 1, 1, 0));//미큐               
                }
                if (mob.getStats().getLevel() > 160 && mob.getStats().getLevel() <= 999) {
                    ranks.add(new MonsterDropEntry(5062002, 5000, 1, 1, 0));//미큐               
                }
                break;
            case 7: //토
                if (mob.getStats().getLevel() >= 110 && MonsterPark) {//(코드,확률,최소갯수,최대갯수,퀘스트코드)
                    ranks.add(new MonsterDropEntry(2434751, 1500, 1, 1, 0));//토상자
                }
                if (mob.getStats().getLevel() >= 50 && mob.getStats().getLevel() <= 110) {
                    ranks.add(new MonsterDropEntry(5062002, 1500, 1, 1, 0));//미큐               
                }
                if (mob.getStats().getLevel() > 110 && mob.getStats().getLevel() <= 160) {
                    ranks.add(new MonsterDropEntry(5062002, 3000, 1, 1, 0));//미큐               
                }
                if (mob.getStats().getLevel() > 160 && mob.getStats().getLevel() <= 999) {
                    ranks.add(new MonsterDropEntry(5062002, 5000, 1, 1, 0));//미큐               
                }
                break;
            case 1: //일
                if (mob.getStats().getLevel() >= 110 && MonsterPark) {//(코드,확률,최소갯수,최대갯수,퀘스트코드)
                    ranks.add(new MonsterDropEntry(2434745, 1500, 1, 1, 0));//일상자
                }
                if (mob.getStats().getLevel() >= 50 && mob.getStats().getLevel() <= 110) {
                    ranks.add(new MonsterDropEntry(5062002, 1500, 1, 1, 0));//미큐               
                }
                if (mob.getStats().getLevel() > 110 && mob.getStats().getLevel() <= 160) {
                    ranks.add(new MonsterDropEntry(5062002, 3000, 1, 1, 0));//미큐               
                }
                if (mob.getStats().getLevel() > 160 && mob.getStats().getLevel() <= 999) {
                    ranks.add(new MonsterDropEntry(5062002, 5000, 1, 1, 0));//미큐               
                }
                break;
        }
        if (mob.getStats().getLevel() >= 30 && mob.getStats().getLevel() <= 999) {//레벨 30이상 
            ranks.add(new MonsterDropEntry(5062002, 100, 1, 1, 0));//미큐
            ranks.add(new MonsterDropEntry(2330004, 3, 1, 1, 0));//샤이니 불릿
            ranks.add(new MonsterDropEntry(2070005, 3, 1, 1, 0));//뇌전
        }
        if (mob.getStats().getLevel() >= 160 && mob.getStats().getLevel() <= 999) {//레벨 160이상 파괴된 헤네시스 
            ranks.add(new MonsterDropEntry(5150054, 50, 1, 1, 0));//오브
            ranks.add(new MonsterDropEntry(2070006, 3, 1, 1, 0));//일비
            ranks.add(new MonsterDropEntry(2330005, 3, 1, 1, 0));//이터널 불릿
            ranks.add(new MonsterDropEntry(4020013, 100, 1, 1, 0));//꿈의조각
            ranks.add(new MonsterDropEntry(4020013, 100, 1, 1, 0));//시간의조각
        }
        if (mob.getStats().getLevel() >= 200 && mob.getStats().getLevel() <= 999) {//레벨 200이상 전당
            ranks.add(new MonsterDropEntry(4310210, 1000, 1, 1, 0));//추뎀코인
        }
        if (mob.getStats().getLevel() >= 250 && mob.getStats().getLevel() <= 999) {//레벨 250 헤이븐 이상
            ranks.add(new MonsterDropEntry(2431000, 120, 1, 1, 0));//메포 5000원
            ranks.add(new MonsterDropEntry(2432001, 50, 1, 1, 0));//캐포 5000원
            ranks.add(new MonsterDropEntry(2070024, 2, 1, 1, 0));//무한의 수리검
            ranks.add(new MonsterDropEntry(2330007, 2, 1, 1, 0));//아머피어싱 불릿
        }
        if (mob.getStats().getLevel() >= 330 && mob.getStats().getLevel() <= 999) {//레벨 330여로이상
            ranks.add(new MonsterDropEntry(2431000, 150, 1, 1, 0));//메포 5000원
            ranks.add(new MonsterDropEntry(2432001, 60, 1, 1, 0));//캐포 5000원
            ranks.add(new MonsterDropEntry(2070023, 2, 1, 1, 0));//플레임표창
            ranks.add(new MonsterDropEntry(2070019, 2, 1, 1, 0));//악마의 표창
        }
        if (mob.getStats().getLevel() >= 400 && mob.getStats().getLevel() <= 999) {//레벨 330여로이상
            ranks.add(new MonsterDropEntry(2431000, 170, 1, 1, 0));//메포 5000원
            ranks.add(new MonsterDropEntry(2432001, 70, 1, 1, 0));//캐포 5000원
            ranks.add(new MonsterDropEntry(2431001, 40, 1, 1, 0));//메포 10000원
            ranks.add(new MonsterDropEntry(2432002, 8, 1, 1, 0));//캐포 10000원
            ranks.add(new MonsterDropEntry(2070023, 2, 1, 1, 0));//플레임표창
            ranks.add(new MonsterDropEntry(2070019, 2, 1, 1, 0));//악마의 표창
        }
        if (mob.getStats().getLevel() >= 119 && mob.getStats().getLevel() <= 250) {//레벨 120~250성아
        }
        if (mob.getStats().getLevel() >= 250 && mob.getStats().getLevel() < 300) {//레벨 250~300헤이븐
        }
        if (mob.getStats().getLevel() >= 300 && mob.getStats().getLevel() < 330) {//레벨 300~330뉴리프
            ranks.add(new MonsterDropEntry(4310277, 50, 1, 1, 0));//뉴 리프 시티 코인[환수제작용]
        }
        if (mob.getStats().getLevel() >= 330 && mob.getStats().getLevel() <= 450) {//레벨 330 ~ 450 소멸의여로
            ranks.add(new MonsterDropEntry(4310277, 50, 1, 1, 0));//뉴 리프 시티 코인[환수제작용]
            ranks.add(new MonsterDropEntry(3970024, 2, 1, 1, 0));//정화의 가루 소멸의여로
            ranks.add(new MonsterDropEntry(3970020, 1500, 1, 1, 0));//빛바랜 심볼 소멸의여로
        }
        if (mob.getStats().getLevel() >= 450 && mob.getStats().getLevel() < 620) {//레벨 450~620츄츄
            ranks.add(new MonsterDropEntry(4310277, 50, 1, 1, 0));//뉴 리프 시티 코인[환수제작용]
            ranks.add(new MonsterDropEntry(3970024, 2, 1, 1, 0));//정화의 가루 뉴리프시티
            ranks.add(new MonsterDropEntry(3970020, 1500, 1, 1, 0));//빛바랜 심볼 뉴리프시티
            ranks.add(new MonsterDropEntry(4310241, 50, 1, 1, 0));//츄츄코인 [츄츄강화용]
            ranks.add(new MonsterDropEntry(3970025, 2, 1, 1, 0));//정화의 가루 츄츄아일랜드
            ranks.add(new MonsterDropEntry(3970021, 1500, 1, 1, 0));//빛바랜 심볼 츄츄아일랜드
        }
        if (mob.getStats().getLevel() >= 620 && mob.getStats().getLevel() <= 830) {//레벨 620~830 레헬른
            ranks.add(new MonsterDropEntry(4310277, 50, 1, 1, 0));//뉴 리프 시티 코인[환수제작용]
            ranks.add(new MonsterDropEntry(3970024, 2, 1, 1, 0));//정화의 가루 뉴리프시티
            ranks.add(new MonsterDropEntry(3970020, 1500, 1, 1, 0));//빛바랜 심볼 뉴리프시티
            ranks.add(new MonsterDropEntry(4310241, 50, 1, 1, 0));//츄츄코인 [츄츄강화용]
            ranks.add(new MonsterDropEntry(3970025, 2, 1, 1, 0));//정화의 가루 츄츄아일랜드
            ranks.add(new MonsterDropEntry(3970021, 1500, 1, 1, 0));//빛바랜 심볼 츄츄아일랜드
            ranks.add(new MonsterDropEntry(4310058, 20, 1, 1, 0));//레헬른 코인 [다크엔블 제작용]
            ranks.add(new MonsterDropEntry(3970026, 2, 1, 1, 0));//정화의 가루 레헬른
            ranks.add(new MonsterDropEntry(3970022, 1500, 1, 1, 0));//빛바랜 심볼 레헬른
        }
        if (mob.getStats().getLevel() >= 830 && mob.getStats().getLevel() <= 999) {//레벨 800이상 999이하 드롭 아르카나
            ranks.add(new MonsterDropEntry(4310277, 50, 1, 1, 0));//뉴 리프 시티 코인[환수제작용]
            ranks.add(new MonsterDropEntry(3970024, 2, 1, 1, 0));//정화의 가루 뉴리프시티
            ranks.add(new MonsterDropEntry(3970020, 1500, 1, 1, 0));//빛바랜 심볼 뉴리프시티
            ranks.add(new MonsterDropEntry(4310241, 50, 1, 1, 0));//츄츄코인 [츄츄강화용]
            ranks.add(new MonsterDropEntry(3970025, 2, 1, 1, 0));//정화의 가루 츄츄아일랜드
            ranks.add(new MonsterDropEntry(3970021, 1500, 1, 1, 0));//빛바랜 심볼 츄츄아일랜드
            ranks.add(new MonsterDropEntry(4310058, 20, 1, 1, 0));//레헬른 코인 [다크엔블 제작용]
            ranks.add(new MonsterDropEntry(3970026, 2, 1, 1, 0));//정화의 가루 레헬른
            ranks.add(new MonsterDropEntry(3970022, 1500, 1, 1, 0));//빛바랜 심볼 레헬른
            ranks.add(new MonsterDropEntry(4310260, 20, 1, 1, 0));//아르카나 정령의보옥 [화엔블제작용]
            ranks.add(new MonsterDropEntry(3970027, 2, 1, 1, 0));//정화의 가루 아르카나
            ranks.add(new MonsterDropEntry(3970023, 1500, 1, 1, 0));//빛바랜 심볼 아르카나
        }
        if (mob.getStats().getLevel() > 10) {//10렙몹 이상드롭
            ranks.add(new MonsterDropEntry(4032575, 3000, 1, 1, 10546));//일일퀘스트 온도계
            ranks.add(new MonsterDropEntry(2049100, 15, 1, 3, 0));//혼줌
            ranks.add(new MonsterDropEntry(2049401, 15, 1, 3, 0));//잠재일반
            ranks.add(new MonsterDropEntry(4001513, 500, 1, 1, 0));//얼룩무늬 티켓 조각
            ranks.add(new MonsterDropEntry(4001515, 300, 1, 1, 0));//표범무늬 티켓
            ranks.add(new MonsterDropEntry(4001521, 150, 1, 1, 0));//호랑무늬 티켓
            ranks.add(new MonsterDropEntry(4001431, 500, 1, 1, 0));//황금사원 티켓
            ranks.add(new MonsterDropEntry(3994102, 1000, 1, 1, 0));//나침반
            ranks.add(new MonsterDropEntry(3994103, 1000, 1, 1, 0));//나침반
            ranks.add(new MonsterDropEntry(3994104, 1000, 1, 1, 0));//나침반
            ranks.add(new MonsterDropEntry(3994105, 1000, 1, 1, 0));//나침반
            ranks.add(new MonsterDropEntry(4001126, 2000, 1, 1, 0));//단풍잎
            ranks.add(new MonsterDropEntry(2431013, 1000, 1, 1, 0));//1천메포
            ranks.add(new MonsterDropEntry(2432000, 700, 1, 1, 0));//1천캐포
        }
        if (getId() >= 925020000 && getId() <= 925033804) {
            if (GameConstants.isMulungBoss(mob.getId())) {
                for (int idd = 2022359; idd <= 2022421; ++idd) {
                    ranks.add(new MonsterDropEntry(idd, 3000, 1, 1, 0));
                }
            } else {
                for (int idd = 2022430; idd <= 2022433; ++idd) {
                    ranks.add(new MonsterDropEntry(idd, 150000, 1, 1, 0));
                }
            }
        } else if (getId() >= 190000000 && getId() <= 198000000) {
            ranks.add(new MonsterDropEntry(4000047, 1000, 1, 1, 0));
        }

        final List<MonsterGlobalDropEntry> ranks_global = new ArrayList<>();
        final List<MonsterGlobalDropEntry> original_global = MapleMonsterInformationProvider.getInstance().getGlobalDrop();

        if (original_global != null) {
            ranks_global.addAll(original_global);
        }

        if (ranks.size() > 0 || ranks_global.size() > 0) {
            int num = 0, itemId = 0, ch = 0;
            MonsterDropEntry de;
            MonsterGlobalDropEntry deg;
            StringBuilder name = new StringBuilder();
            StringBuilder name2 = new StringBuilder();
            for (int i = 0; i < ranks.size(); i++) {
                de = ranks.get(i);
                if (de.chance > 0 && (de.questid <= 0 || (de.questid > 0 && MapleQuest.getInstance(de.questid).getName().length() > 0))) {
                    itemId = de.itemId;
                    if (num == 0) {
                        if (mobId == 8510000 || mobId == 8520000 || mobId == 8800002 || mobId == 8810018 || mobId == 8810122 || mobId == 8800102) {
                            name.append("선택하신 몬스터의 드롭데이터 입니다.\r\n");
                        } else {
                            name.append("");
                            if (mob.getStats().getFly()) {
                                if (mob.getStats().getLink() > 0) {
                                    if (mob.getStats().getLink() < 1000000) {
                                        name.append("선택하신 몬스터의 드롭데이터 입니다.\r\n");
                                    } else {
                                        name.append("선택하신 몬스터의 드롭데이터 입니다.\r\n");
                                    }
                                } else if (mobId < 1000000) {
                                    name.append("선택하신 몬스터의 드롭데이터 입니다.\r\n");
                                } else {
                                    name.append("선택하신 몬스터의 드롭데이터 입니다.\r\n");
                                }
                            } else if (mob.getStats().getLink() > 0) {
                                if (mob.getStats().getLink() < 1000000) {
                                    name.append("선택하신 몬스터의 드롭데이터 입니다.\r\n");
                                } else {
                                    name.append("선택하신 몬스터의 드롭데이터 입니다.\r\n");
                                }
                            } else if (mobId < 1000000) {
                                name.append("선택하신 몬스터의 드롭데이터 입니다.\r\n");
                            } else {
                                name.append("선택하신 몬스터의 드롭데이터 입니다.\r\n");
                            }
                        }
                        name.append("#b아이템을 클릭하면 해당 아이템 드롭 몬스터를 알려줍니다.#k\r\n\r\n");
                        name.append("#b" + mob.getStats().getName() + "#k (Lv. " + mob.getStats().getLevel() + ")\r\n");
                    }
                    if (itemId == 0) { //meso
                        itemId = 4031041; //display sack of cash
                        name2.append((de.Minimum * getClient().getChannelServer().getMesoRate()) + " ~ " + (de.Maximum * getClient().getChannelServer().getMesoRate()) + " 메소");
                    } else {
                        name2.append("#z" + itemId + "#");
                    }
                    ch = de.chance * getClient().getChannelServer().getDropRate();
                    if (!ret.contains(itemId)) {
                        if (MapleItemInformationProvider.getInstance().itemExists(itemId)) {
                            name.append("#L" + itemId + "#" + name2.toString() + " -");
                        } else {
                            name.append("#L" + itemId + "#메소 - ");
                        }
                        ret.add(itemId);
                        if (ch < 10) {
                            name.append("0.000" + ch + "%#l");
                        } else {
                            name.append((Integer.valueOf(ch > 999999 ? 1000000 : ch).doubleValue() / 10000.0) + "%#l");
                        }
                        name.append((de.questid > 0 && MapleQuest.getInstance(de.questid).getName().length() > 0 ? ("\r\n\r\n     #b(퀘스트 : " + MapleQuest.getInstance(de.questid).getName() + ")") : "") + "#k\r\n");
                        num++;
                    } else {
                        if (!ret2.contains(itemId)) {
                            if (num < 10) {
                                if (MapleItemInformationProvider.getInstance().itemExists(itemId)) {
                                    name.append("\r\n		#i" + itemId + ":# " + name2.toString() + " - ");
                                } else {
                                    name.append("\r\n		#i4031039# " + name2.toString() + " - ");
                                }
                            } else {
                                if (MapleItemInformationProvider.getInstance().itemExists(itemId)) {
                                    name.append("\r\n		  #i" + itemId + ":# " + name2.toString() + " - ");
                                } else {
                                    name.append("\r\n		  #i4031039# " + name2.toString() + " - ");
                                }
                            }
                            ret2.add(itemId);
                        } else if (num < 10) {
                            if (MapleItemInformationProvider.getInstance().itemExists(itemId)) {
                                name.append("		#i" + itemId + ":# " + name2.toString() + " - ");
                            } else {
                                name.append("		#i4031039# " + name2.toString() + " - ");
                            }
                        } else {
                            if (MapleItemInformationProvider.getInstance().itemExists(itemId)) {
                                name.append("		  #i" + itemId + ":# " + name2.toString() + " - ");
                            } else {
                                name.append("		  #i4031039# " + name2.toString() + " - ");
                            }
                        }
                        if (ch < 10) {
                            name.append("0.000" + ch + "%#l");
                        } else {
                            name.append((Integer.valueOf(ch >= 999999 ? 1000000 : ch).doubleValue() / 10000.0) + "%#l");
                        }
                        name.append((de.questid > 0 && MapleQuest.getInstance(de.questid).getName().length() > 0 ? ("\r\n\r\n				(퀘스트 : " + MapleQuest.getInstance(de.questid).getName() + ")") : "") + "\r\n");
                    }
                }
                name2.setLength(0);
            }
            for (int i = 0; i < ranks_global.size(); i++) {
                deg = ranks_global.get(i);
                if (deg.chance > 0 && (deg.questid <= 0 || (deg.questid > 0 && MapleQuest.getInstance(deg.questid).getName().length() > 0))) {
                    itemId = deg.itemId;
                    if (num == 0) {
                        if (mobId == 8510000 || mobId == 8520000 || mobId == 8800002 || mobId == 8810018 || mobId == 8810122 || mobId == 8800102) {
                            name.append("선택하신 몬스터의 드롭데이터 입니다.\r\n");
                        } else {
                            name.append("");
                            if (mob.getStats().getFly()) {
                                if (mob.getStats().getLink() > 0) {
                                    if (mob.getStats().getLink() < 1000000) {
                                        name.append("선택하신 몬스터의 드롭데이터 입니다.\r\n");
                                    } else {
                                        name.append("선택하신 몬스터의 드롭데이터 입니다.\r\n");
                                    }
                                } else if (mobId < 1000000) {
                                    name.append("선택하신 몬스터의 드롭데이터 입니다.\r\n");
                                } else {
                                    name.append("선택하신 몬스터의 드롭데이터 입니다.\r\n");
                                }
                            } else if (mob.getStats().getLink() > 0) {
                                if (mob.getStats().getLink() < 1000000) {
                                    name.append("선택하신 몬스터의 드롭데이터 입니다.\r\n");
                                } else {
                                    name.append("선택하신 몬스터의 드롭데이터 입니다.\r\n");
                                }
                            } else if (mobId < 1000000) {
                                name.append("선택하신 몬스터의 드롭데이터 입니다.\r\n");
                            } else {
                                name.append("선택하신 몬스터의 드롭데이터 입니다.\r\n");
                            }
                        }
                        name.append("#b아이템을 클릭하면 해당 아이템 드롭 몬스터를 알려줍니다.#k\r\n\r\n");
                        name.append("#b" + mob.getStats().getName() + "#k (Lv. " + mob.getStats().getLevel() + ")\r\n");
                    }
                    if (itemId == 0) { //meso
                        itemId = 4031041; //display sack of cash
                        name2.append((deg.Minimum * getClient().getChannelServer().getMesoRate()) + " ~ " + (deg.Maximum * getClient().getChannelServer().getMesoRate()) + " 메소");
                    } else {
                        name2.append("#z" + itemId + "#");
                    }
                    ch = deg.chance * getClient().getChannelServer().getDropRate();
                    if (!ret.contains(itemId)) {
                        if (MapleItemInformationProvider.getInstance().itemExists(itemId)) {
                            name.append("#L" + itemId + "#" + name2.toString() + " -");
                        } else {
                            name.append("#L" + itemId + "#메소 - ");
                        }
                        ret.add(itemId);
                        if (ch < 10) {
                            name.append("0.000" + ch + "%#l");
                        } else {
                            name.append((Integer.valueOf(ch > 999999 ? 1000000 : ch).doubleValue() / 10000.0) + "%#l");
                        }
                        name.append((deg.questid > 0 && MapleQuest.getInstance(deg.questid).getName().length() > 0 ? ("\r\n\r\n     #b(퀘스트 : " + MapleQuest.getInstance(deg.questid).getName() + ")") : "") + "#k\r\n");
                        num++;
                    } else {
                        if (!ret2.contains(itemId)) {
                            if (num < 10) {
                                if (MapleItemInformationProvider.getInstance().itemExists(itemId)) {
                                    name.append("\r\n		#i" + itemId + ":# " + name2.toString() + " - ");
                                } else {
                                    name.append("\r\n		#i4031039# " + name2.toString() + " - ");
                                }
                            } else {
                                if (MapleItemInformationProvider.getInstance().itemExists(itemId)) {
                                    name.append("\r\n		  #i" + itemId + ":# " + name2.toString() + " - ");
                                } else {
                                    name.append("\r\n		  #i4031039# " + name2.toString() + " - ");
                                }
                            }
                            ret2.add(itemId);
                        } else if (num < 10) {
                            if (MapleItemInformationProvider.getInstance().itemExists(itemId)) {
                                name.append("		#i" + itemId + ":# " + name2.toString() + " - ");
                            } else {
                                name.append("		#i4031039# " + name2.toString() + " - ");
                            }
                        } else {
                            if (MapleItemInformationProvider.getInstance().itemExists(itemId)) {
                                name.append("		  #i" + itemId + ":# " + name2.toString() + " - ");
                            } else {
                                name.append("		  #i4031039# " + name2.toString() + " - ");
                            }
                        }
                        if (ch < 10) {
                            name.append("0.000" + ch + "%#l");
                        } else {
                            name.append((Integer.valueOf(ch >= 999999 ? 1000000 : ch).doubleValue() / 10000.0) + "%#l");
                        }
                        name.append((deg.questid > 0 && MapleQuest.getInstance(deg.questid).getName().length() > 0 ? ("\r\n\r\n				(퀘스트 : " + MapleQuest.getInstance(deg.questid).getName() + ")") : "") + "\r\n");
                    }
                }
                name2.setLength(0);
            }

            if (name.length() > 0) {
                return name.toString();
            }
        }
        return "아무것도 드랍하지 않거나 없는 몬스터 코드네요.";
    }

    public String SearchDropMonster(int itemid) {
        ArrayList<Integer> ret = new ArrayList<Integer>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        StringBuilder text = new StringBuilder(" #i" + itemid + ":# #b#t" + itemid + "##k\r\n\r\n");
        double chance = 0;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM drop_data WHERE itemid = ?");
            ps.setInt(1, itemid);
            rs = ps.executeQuery();
            MapleMonster mob = null;
            while (rs.next()) {
                if (!ret.contains(rs.getInt("dropperid"))) {
                    ret.add(rs.getInt("dropperid"));
                    mob = MapleLifeFactory.getMonster(rs.getInt("dropperid"));
                    text.append("   #b" + mob.getStats().getName() + "#k Lv. " + mob.getStats().getLevel() + "#k");
                    chance = (double) ((double) 100 * ((double) rs.getInt("chance") * (double) getClient().getChannelServer().getDropRate()) / (double) 1000000);
                    if (chance > 100) {
                        text.append("  드랍 확률 : 100%\r\n");
                    } else if (chance < 0.001) {
                        text.append("  드랍 확률 : 0.000" + rs.getInt("chance") + "%\r\n");
                    } else {
                        text.append("  드랍 확률 : " + chance + "%\r\n");
                    }
                    //text.append("#r#L" + mob.getId() + "#" + mob.getStats().getName() + "의 전체 드랍 목록 보기#k#l\r\n\r\n\r\n");
                }
            }
            rs.close();
            ps.close();
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
            } catch (Exception e) {
            }
        }
        if (text.length() < 47) {
            return "해당 아이템을 드랍하는 몬스터가 없습니다.";
        }
        return text.toString();
    }

    public String globalDrop() {
        final List<MonsterGlobalDropEntry> global = MapleMonsterInformationProvider.getInstance().getGlobalDrop();
        StringBuilder Text = new StringBuilder();
        StringBuilder Text2 = new StringBuilder();
        Text.append("모든 몬스터에게 나오는 드랍정보입니다.\r\n");
        double chance = 0;
        int itemid = 0;
        if (global.size() > 0) {
            for (int i = 0; i < global.size(); i++) {
                chance = (double) ((double) 100 * ((double) global.get(i).chance * (double) getClient().getChannelServer().getDropRate()) / (double) 1000000);
                if (chance > 100) {
                    Text2.append("- 100%");
                } else if (chance < 0.001) {
                    Text2.append("- 0.000" + global.get(i).chance + "%");
                } else {
                    Text2.append("- " + chance + "%");
                }
                itemid = global.get(i).itemId;
                if (itemid == 0) {
                    itemid = 4031041;
                }
                if (!MapleItemInformationProvider.getInstance().itemExists(itemid)) {
                    itemid = 4031039;
                }
                Text.append("\r\n" + (1 + i) + ") #i" + itemid + ":# #b#z" + itemid + "##k " + Text2.toString() + " (" + global.get(i).Minimum + "개 ~ " + global.get(i).Maximum + "개)");
                Text2.setLength(0);
            }
            return Text.toString();
        }
        return "글로벌 드랍이 존재하지 않습니다.";
    }

    public String SearchDropItems(String dropItemName) {
        StringBuilder Text = new StringBuilder("아래의 아이템들이 검색되었어요.\r\n");
        if (dropItemName.length() < 2) {
            return "두 글자는 입력해주셔야 해요.";
        }
        for (ItemInformation itemPair : MapleItemInformationProvider.getInstance().getAllItems()) {
            if (itemPair != null && itemPair.name != null && itemPair.name.toLowerCase().contains(dropItemName.toLowerCase())) {
                Text.append("#L" + itemPair.itemId + "##b#t" + itemPair.itemId + "##k\r\n");
            }
        }
        if (Text.length() < 20) {
            return "검색된 아이템이 없습니다.";
        }
        return Text.toString();
    }

    public String searchTingitem(String search) {
        StringBuilder Text = new StringBuilder().append("마우스 가져대서 팅기면 팅템입니다.\r\n\r\n");
        boolean Count = false;
        if (search.length() < 2) {
            return "검색한 값의 글자 수가 너무 작습니다.";
        } else {
            for (ItemInformation itemPair : MapleItemInformationProvider.getInstance().getAllItems()) {
                if (itemPair != null && itemPair.name != null && itemPair.name.toLowerCase().contains(search.toLowerCase())) {
                    Count = true;
                    Text.append("#b#z" + itemPair.itemId + "##k\r\n");
                }
            }
            if (!Count) {
                return "검색된 아이템이 없습니다.";
            }
        }
        return Text.toString();
    }

    public String getBanJum(Long S) {
        StringBuilder SS = new StringBuilder("" + S);
        if (S < 0) {
            return SS.toString();
        }
        SS.reverse();
        int SSS = 0;
        for (int i = 0; i < SS.length(); i++) {
            SSS++;
            if (SSS == 4) {
                SS.insert(i, ",");
                SSS = 0;
            }
        }
        SS.reverse();
        return SS.toString();
    }

    public String searchMob(String search) {
        MapleData data = null;
        MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/" + "String.wz"));
        data = dataProvider.getData("Mob.img");
        List<Pair<Integer, String>> mobPairList = new LinkedList<Pair<Integer, String>>();
        StringBuilder Text = new StringBuilder().append("아래의 몬스터가 검색되었습니다.\r\n");
        boolean Count = false;
        for (MapleData mobIdData : data.getChildren()) {
            mobPairList.add(new Pair<Integer, String>(Integer.parseInt(mobIdData.getName()), MapleDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME")));
        }
        if (search.length() < 1) {
            return "검색한 값의 글자 수가 너무 작습니다.";
        } else {
            for (Pair<Integer, String> mobPair : mobPairList) {
                if (mobPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                    Count = true;
                    Text.append("#L" + mobPair.getLeft() + "##b" + mobPair.getRight() + "#k\r\n");
                }
            }
            if (!Count) {
                return "검색된 몬스터가 없습니다.";
            }
        }
        return Text.toString();
    }
    //PC방
    private int pcdate;
    private long pctime;
    private boolean pccheck, pcbang;

    public long getPcTime() {
        return pctime;
    }

    public void setPcTime(Long i) {
        pctime = System.currentTimeMillis() + i;
        pccheck = true;
        pcbang = true;
    }

    public void addPcTime(Long i) {
        pctime += i;
        pccheck = true;
        pcbang = true;
    }

    public void clearPc(boolean bool) {
        pccheck = bool;
        pcbang = bool;
    }

    public int getPcDate() {
        return pcdate;
    }

    public void setPcDate(int i) {
        pcdate = i;
    }

    public int getCalcPcTime() {
        if (pctime == 0 || pctime < System.currentTimeMillis()) {
            return 0;
        }
        return (int) ((pctime - System.currentTimeMillis()) / 60000);
    }

    public boolean checkPcTime() {
        if (pctime > System.currentTimeMillis()) {
            return true;
        }
        return false;
    }

    public void getPcManager() {
        if (checkPcTime()) {
            if (pccheck) {
                if (getCalcPcTime() == 10) {
                    pccheck = false;
                    dropMessage(5, "PC방 정량제 잔여시간이 10분 남았습니다.");
                }
            }
        } else {
            if (pcbang) {
                dropMessage(5, "PC방 정량제 잔여시간이 끝났습니다.");
                getClient().getSession().write(MaplePacketCreator.enableInternetCafe((byte) 0, getCalcPcTime()));
                pcbang = false;
                if (getMapId() >= 190000000 && getMapId() <= 198000000) {
                    MapleMap to = getClient().getChannelServer().getMapFactory().getMap(193000000);
                    changeMap(to);
                    //dropMessage(5, "나가 이썌끼야");
                }
            }
        }
    }

    public void saveToPC() {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM c_pctime WHERE acc = ?");
            ps.setInt(1, getAccountID());
            rs = ps.executeQuery();
            if (rs.next()) {
                ps.close();
                rs.close();
                ps = con.prepareStatement("UPDATE c_pctime SET time = ?, date = ? WHERE acc = ?");
                ps.setLong(1, pctime);
                ps.setInt(2, pcdate);
                ps.setInt(3, getAccountID());
                ps.executeUpdate();
            } else {
                ps.close();
                rs.close();
                ps = con.prepareStatement("INSERT INTO c_pctime (acc, time, date) VALUES(?, ?, ?)");
                ps.setInt(1, getAccountID());
                ps.setLong(2, pctime);
                ps.setInt(3, pcdate);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("PC err...");
            e.printStackTrace();
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
            } catch (Exception e) {
            }
        }
    }

    public int partyMembersInMap() {
        int inMap = 0;
        for (MapleCharacter char2 : getMap().getCharacters()) {
            if (char2.getParty() == getParty()) {
                inMap++;
            }
        }
        return inMap;
    }

    private final MapleMap getWarpMap(int map) {
        if (getEventInstance() != null) {
            return getEventInstance().getMapFactory().getMap(map);
        }
        return ChannelServer.getInstance(this.client.getChannel()).getMapFactory().getMap(map);
    }

    public final void warp(int map) {
        MapleMap mapz = getWarpMap(map);
        changeMap(mapz, (MaplePortal) mapz.getPortalSP().get(Randomizer.nextInt(mapz.getPortalSP().size())));
    }

    private ScheduledFuture<?> timemove;

    public void timeMove() {
        if (timemove != null) {
            timemove.cancel(true);
        }
        timemove = null;
    }

    public final void timeMoveMap(final int destination, final int movemap, final int time) {
        warp(movemap);
        getClient().getSession().write(MaplePacketCreator.getClock(time));
        timemove = Timer.EtcTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                if (client.getPlayer() != null) {
                    if (getMapId() == movemap) {
                        warp(destination);
                    }
                    if (destination == 931000440) {
                        forceCompleteQuest2(23127);
                        client.getPlayer().gainExp((int) (65560), true, true, true);
                        dropMessage(5, "수아르 를 성공적으로 지켜 내셨습니다.");
                    }
                }
            }
        }, time * 1000);
    }

    public final boolean canHold(final int itemid, final int quantity) {
        return MapleInventoryManipulator.checkSpace(getClient(), itemid, quantity, "");
    }

    public final MapleClaim getClaim() {
        return claim;
    }

    public int getCharId(String name) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            int id = 0;
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM c_pctime WHERE acc = ?");
            ps.setString(1, name);
            rs = ps.executeQuery();
            if (rs.next()) {
                id = rs.getInt("id");
            }
            return id;
        } catch (SQLException ex) {
            ex.printStackTrace();
            dropMessage(6, "캐릭터 번호 찾기 오류!");
            return 0;
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
            } catch (Exception e) {
            }
        }
    }

    private long nextWarningTime = 0;

    public boolean isPartyMember(MapleCharacter chr) {
        return isPartyMember(chr.getId());
    }

    public boolean isPartyMember(int cid) {
        for (MapleCharacter mpcu : getPartyMembers()) {
            if (mpcu.getId() == cid) {
                return true;
            }
        }

        return false;
    }

    public List<MapleCharacter> getPartyMembers() {
        List<MapleCharacter> list = new LinkedList<>();
        if (party != null) {
            for (MaplePartyCharacter partyMembers : party.getMembers()) {
                MapleCharacter otherChar = getMap().getCharacterById(partyMembers.getId());
                list.add(otherChar);
            }
        }
        return list;
    }

    public void showMapOwnershipInfo(MapleCharacter mapOwner) {
        long curTime = System.currentTimeMillis();
        if (nextWarningTime < curTime) {
            nextWarningTime = curTime + (5 * 1000);   // show underlevel info again after 1 minute

            List<String> strLines = new LinkedList<>();
            strLines.add("");
            strLines.add("");
            strLines.add("");
            //strLines.add(this.getClient().getChannelServer().getServerMessage().isEmpty() ? 0 : 1, "Get off my lawn!!");

            dropMessage(5, "현재 이 맵의 주인은 " + mapOwner.getName() + "입니다. (마지막 활동 : " + getMap().OwnerActivity() + "초 전)");
        }
    }

    private Map<String, String> ACCCustomValues = new HashMap<String, String>();

    public void setKeyValue(String key, String value, boolean a) {//키밸류
        //this.keyValues.put(key, value);
        if (getKeyValue(key) == null) {

            Connection con = null;
            PreparedStatement ps = null;
            try {
                con = DatabaseConnection.getConnection();
                //ps = null;
                String query = "INSERT into `customvalues` (`cid`, `key`, `value`, `day`) VALUES ('";
                query = new StringBuilder().append(query).append(id).toString();
                query = new StringBuilder().append(query).append("', '").toString();
                query = new StringBuilder().append(query).append(key).toString();
                query = new StringBuilder().append(query).append("', '").toString();
                query = new StringBuilder().append(query).append(value).toString();
                query = new StringBuilder().append(query).append("', '").toString();
                query = new StringBuilder().append(query).append(a ? "1" : "0").toString();
                query = new StringBuilder().append(query).append("')").toString();
                ps = con.prepareStatement(query);
                ps.executeUpdate();
                ps.close();

            } catch (SQLException ex) {
            } finally {
                try {
                    if (ps != null) {
                        ps.close();
                    }
                    if (con != null) {
                        con.close();
                    }
                } catch (Exception e) {
                }
            }
        } else {

            Connection con = null;
            PreparedStatement ps = null;
            try {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("UPDATE `customvalues` SET `value` = ? WHERE `cid` = ? AND `key` = ?");
                ps.setString(1, value);
                ps.setInt(2, id);
                ps.setString(3, key);
                ps.executeUpdate();
                ps.close();
            } catch (SQLException ex) {
            } finally {
                try {
                    if (ps != null) {
                        ps.close();
                    }
                    if (con != null) {
                        con.close();
                    }
                } catch (Exception e) {
                }
            }
            if (ACCCustomValues.containsKey(key)) {
                ACCCustomValues.remove(key);
            }
            ACCCustomValues.put(key, value);

        }
    }

    public void setKeyValue3(String key, String value, boolean a) {
        if (getKeyValue3(key) == null) {
            Connection con = null;
            PreparedStatement ps = null;
            try {
                con = DatabaseConnection.getConnection();
                //ps = null;
                String query = "INSERT into `acheck` (`accid`, `key`, `value`, `day`) VALUES ('";
                query = new StringBuilder().append(query).append(getAccountID()).toString();
                query = new StringBuilder().append(query).append("', '").toString();
                query = new StringBuilder().append(query).append(key).toString();
                query = new StringBuilder().append(query).append("', '").toString();
                query = new StringBuilder().append(query).append(value).toString();
                query = new StringBuilder().append(query).append("', '").toString();
                query = new StringBuilder().append(query).append(a ? "1" : "0").toString();
                query = new StringBuilder().append(query).append("')").toString();
                ps = con.prepareStatement(query);
                ps.executeUpdate();
                ps.close();
            } catch (SQLException ex) {
            } finally {
                try {
                    if (ps != null) {
                        ps.close();
                    }
                    if (con != null) {
                        con.close();
                    }
                } catch (Exception e) {
                }
            }
        } else {
            Connection con = null;
            PreparedStatement ps = null;
            try {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("UPDATE `acheck` SET `value` = ? WHERE `accid` = ? AND `key` = ?");
                ps.setString(1, value);
                ps.setInt(2, accountid);
                ps.setString(3, key);
                ps.executeUpdate();
                ps.close();
            } catch (SQLException ex) {
            } finally {
                try {
                    if (ps != null) {
                        ps.close();
                    }
                    if (con != null) {
                        con.close();
                    }
                } catch (Exception e) {
                }
            }
        }
    }

    public void setKeyValue(String key, String value) {
        setKeyValue(key, value, false);

    }

    private ScheduledFuture<?> timeMoveMap;

    public void cancelTimeMove() {
        if (timeMoveMap != null) {
            timeMoveMap.cancel(true);
        }
        timeMoveMap = null;
    }

    public final void timeMoveMap(final int destination, final int movemap, final int time, final int portal) {
        warp(movemap);
        getClient().sendPacket(MaplePacketCreator.getClock(time));
        timeMoveMap = Timer.EtcTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                if (this != null && getMapId() == movemap) {
                    warp(destination);
                }
            }
        }, time * 1000);
    }

    public final void PartyTimeMoveMap(final int destination, final int movemap, final int time) {
        if (getParty() == null || getParty().getMembers().size() == 1) {
            timeMoveMap(destination, movemap, time, 0);
            return;
        }
        final int cMap = getMapId();
        for (final MaplePartyCharacter chr : getParty().getMembers()) {
            final MapleCharacter curChar = getClient().getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && curChar.isAlive() && (curChar.getMapId() == cMap || (curChar.getEventInstance() != null && curChar.getEventInstance() == getEventInstance()))) {
                curChar.timeMoveMap(destination, movemap, time, 0);
            }
        }
    }

    public String getKeyValue(String key) {
//        if (this.keyValues.containsKey(key)) 
//            return this.keyValues.get(key);
//        return null;

        if (ACCCustomValues.containsKey(key)) {
            //System.out.print("이미 입력된 값에서 불러옴");
            return ACCCustomValues.get(key);
        }
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM `customvalues` WHERE `cid` = ? and `key` = ?");
            ps.setInt(1, id);
            ps.setString(2, key);
            rs = ps.executeQuery();
            while (rs.next()) {
                String ret = rs.getString("value");
                ACCCustomValues.put(rs.getString("key"), rs.getString("value").equals("null") ? null : rs.getString("value"));
                //System.out.print("신규 선택 값에서 불러옴");
                rs.close();
                ps.close();
                con.close();
                return ret;
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            return null;
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (Exception e) {
            }
        }
        return null;
    }

    public String getKeyValue3(String key) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM `acheck` WHERE `accid` = ? and `key` = ?");
            ps.setLong(1, getAccountID());
            ps.setString(2, key);
            rs = ps.executeQuery();
            while (rs.next()) {
                return rs.getString("value");
            }
            rs.close();
        } catch (SQLException ex) {
            return null;
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (Exception e) {
            }
        }
        return null;
    }

    public int getKeyValue2(String key) {
        if (CustomValues2.containsKey(key)) {
            return CustomValues2.get(key).intValue();
        }
        return -1;
    }

    public void setKeyValue2(String key, int values) {
        if (CustomValues2.containsKey(key)) {
            CustomValues2.remove(key);
        }
        CustomValues2.put(key, values);
        keyvalue_changed = true;
    }

    public String getDate() {
        Calendar ocal = Calendar.getInstance();

        int year = ocal.get(ocal.YEAR);
        int month = ocal.get(ocal.MONTH) + 1;
        int day = ocal.get(ocal.DAY_OF_MONTH);

        return (year + "" + month + "" + day);
    }

    public String getDateKey(String key) {
        Calendar ocal = Calendar.getInstance();
        int year = ocal.get(ocal.YEAR);
        int month = ocal.get(ocal.MONTH) + 1;
        int day = ocal.get(ocal.DAY_OF_MONTH);
        return getKeyValue(year + "" + month + "" + day + "_" + key);
    }

    public void setDateKey(String key, String value) {
        Calendar ocal = Calendar.getInstance();
        int year = ocal.get(ocal.YEAR);
        int month = ocal.get(ocal.MONTH) + 1;
        int day = ocal.get(ocal.DAY_OF_MONTH);
        setKeyValue(year + "" + month + "" + day + "_" + key, value, true);
    }

    public String getDateKey2(String key) {
        Calendar ocal = Calendar.getInstance();
        int year = ocal.get(ocal.YEAR);
        int month = ocal.get(ocal.MONTH) + 1;
        int day = ocal.get(ocal.DAY_OF_MONTH);
        return getKeyValue3(year + "" + month + "" + day + "_" + key);
    }

    public int getDateKey22(String key) {
        Calendar ocal = Calendar.getInstance();
        int year = ocal.get(ocal.YEAR);
        int month = ocal.get(ocal.MONTH) + 1;
        int day = ocal.get(ocal.DAY_OF_MONTH);
        return Integer.valueOf(getKeyValue3(year + "" + month + "" + day + "_" + key));
    }

    public void setDateKey2(String key, String value) {
        Calendar ocal = Calendar.getInstance();
        int year = ocal.get(ocal.YEAR);
        int month = ocal.get(ocal.MONTH) + 1;
        int day = ocal.get(ocal.DAY_OF_MONTH);
        setKeyValue3(year + "" + month + "" + day + "_" + key, value, true);
    }

    public String getDateKeychr(String key) {
        Calendar ocal = Calendar.getInstance();
        int year = ocal.get(1);
        int month = ocal.get(2) + 1;
        int day = ocal.get(5);
        return getKeyValuechr(new StringBuilder().append(year).append("").append(month).append("").append(day).append("_").append(key).toString());
    }

    public void setDateKeychr(String key, String value) {
        Calendar ocal = Calendar.getInstance();
        int year = ocal.get(1);
        int month = ocal.get(2) + 1;
        int day = ocal.get(5);
        setKeyValuechr(new StringBuilder().append(year).append("").append(month).append("").append(day).append("_").append(key).toString(), value, true);
    }
    private Map<String, String> ACCCustomValuesChr = new HashMap<String, String>();

    /*      */ public String getKeyValuechr(String key) {

        if (ACCCustomValuesChr.containsKey(key)) {
            //System.out.print("이미 입력된 값에서 불러옴");
            return ACCCustomValuesChr.get(key);
        }
        /* 3270 */ Connection con = null;
        /* 3271 */ PreparedStatement ps = null;
        /* 3272 */ ResultSet rs = null;
        /*      */ try {
            /* 3274 */ con = DatabaseConnection.getConnection();
            /* 3275 */ ps = con.prepareStatement("SELECT * FROM `customvalueschr` WHERE `cid` = ? and `key` = ?");
            /* 3276 */ ps.setInt(1, getAccountID());
            /* 3277 */ ps.setString(2, key);
            /* 3278 */ rs = ps.executeQuery();
            while (rs.next()) {
                String ret = rs.getString("value");
                ACCCustomValuesChr.put(rs.getString("key"), rs.getString("value").equals("null") ? null : rs.getString("value"));
                //System.out.print("신규 선택 값에서 불러옴");
                rs.close();
                ps.close();
                con.close();
                return ret;
            }
            rs.close();
            ps.close();
            con.close();
            /*      */        } catch (SQLException ex) {
            /* 3284 */ return null;
            /*      */        } finally {
            /*      */ try {
                /* 3287 */ if (ps != null) {
                    /* 3288 */ ps.close();
                    /*      */                }
                /* 3290 */ if (rs != null) {
                    /* 3291 */ rs.close();
                    /*      */                }
                /* 3293 */ if (con != null) /* 3294 */ {
                    con.close();
                }
                /*      */            } /*      */ catch (Exception e) {
                /*      */            }
            /*      */        }
        /* 3299 */ return null;
        /*      */    }

    /*      */ public void setKeyValuechr(String key, String value, boolean a) {

        /* 3121 */ if (getKeyValuechr(key) == null) {
            /* 3122 */ Connection con = null;
            /* 3123 */ PreparedStatement ps = null;
            /*      */ try {
                /* 3125 */ con = DatabaseConnection.getConnection();
                /*      */
                /* 3127 */ String query = "INSERT into `customvalueschr` (`cid`, `key`, `value`, `day`) VALUES ('";
                /* 3128 */ query = new StringBuilder().append(query).append(getAccountID()).toString();
                /* 3129 */ query = new StringBuilder().append(query).append("', '").toString();
                /* 3130 */ query = new StringBuilder().append(query).append(key).toString();
                /* 3131 */ query = new StringBuilder().append(query).append("', '").toString();
                /* 3132 */ query = new StringBuilder().append(query).append(value).toString();
                /* 3133 */ query = new StringBuilder().append(query).append("', '").toString();
                /* 3134 */ query = new StringBuilder().append(query).append(a ? "1" : "0").toString();
                /* 3135 */ query = new StringBuilder().append(query).append("')").toString();
                /* 3136 */ ps = con.prepareStatement(query);
                /* 3137 */ ps.executeUpdate();
                /* 3138 */ ps.close();
                /*      */            } catch (SQLException ex) {
                /*      */            } finally {
                /*      */ try {
                    /* 3142 */ if (ps != null) {
                        /* 3143 */ ps.close();
                        /*      */                    }
                    /* 3145 */ if (con != null) /* 3146 */ {
                        con.close();
                    }
                    /*      */                } catch (Exception e) {
                    /*      */                }
                /*      */            }
            /*      */        } /*      */ else {
            /* 3152 */ Connection con = null;
            /* 3153 */ PreparedStatement ps = null;
            /*      */ try {
                /* 3155 */ con = DatabaseConnection.getConnection();
                /* 3156 */ ps = con.prepareStatement("UPDATE `customvalueschr` SET `value` = ? WHERE `cid` = ? AND `key` = ?");
                /* 3157 */ ps.setString(1, value);
                /* 3158 */ ps.setInt(2, getAccountID());
                /* 3159 */ ps.setString(3, key);
                /* 3160 */ ps.executeUpdate();
                /* 3161 */ ps.close();
                /*      */            } catch (SQLException ex) {
                /*      */            } finally {
                /*      */ try {
                    /* 3165 */ if (ps != null) {
                        /* 3166 */ ps.close();
                        /*      */                    }
                    /* 3168 */ if (con != null) /* 3169 */ {
                        con.close();
                    }
                    /*      */                } /*      */ catch (Exception e) {
                    /*      */                }
                /*      */            }
            if (ACCCustomValuesChr.containsKey(key)) {
                ACCCustomValuesChr.remove(key);
            }
            ACCCustomValuesChr.put(key, value);
            /*      */        }
        /*      */    }

    public void startMapTimeLimitTask2(int time, final MapleMap to) {
        if (time <= 0) {
            time = 1;
        }
        client.getSession().write(MaplePacketCreator.getClock(time));
        final MapleMap ourMap = getMap();
        time *= 1000;
        client.getPlayer().nowdamagetotal = 0;
        mapTimeLimitTask = MapTimer.getInstance().register(new Runnable() {
            @Override
            public void run() {
                if (ourMap.getId() == GameConstants.JAIL) {
                    getQuestNAdd(MapleQuest.getInstance(GameConstants.JAIL_TIME)).setCustomData(String.valueOf(System.currentTimeMillis()));
                    getQuestNAdd(MapleQuest.getInstance(GameConstants.JAIL_QUEST)).setCustomData("0"); //release them!
                }
                changeMap(to, to.getPortal(0));
                // client.getPlayer().gainExp((int) (nowdamagetotal / 3), true, true, true);
                if (nowdamagetotal > client.getPlayer().getdamagetotal()) {
                    client.getPlayer().setdamagetotal(nowdamagetotal);
                    client.getPlayer().dropMessage(6, "축하합니다 ! 이전보다 더 좋은 성적을 거두셨어요! 총 데미지 : " + nowdamagetotal);
                    //saveToDB(false, false);
                } else {
                    client.getPlayer().dropMessage(6, "총 데미지 : " + nowdamagetotal);
                    //saveToDB(false, false);
                }
                client.getPlayer().setdamagecount(false);

            }
        }, time, time);
    }

    public long getdamagetotal() {
        return damagetotal;
    }

    public void setdamagetotal(long bosskill) {
        this.damagetotal = bosskill;
    }

    public void gaindamagetotal(long gainbosskill) {
        this.damagetotal += gainbosskill;
    }

    public void setdamagecount(boolean bosskill) {
        this.damagecount = bosskill;
    }

    public boolean getdamagecount() {
        return damagecount;
    }

    public void gainnowdamagetotal(long a) {
        nowdamagetotal += a;
    }

    public long getNowDamage() {
        return nowdamagetotal;
    }

    public static void upgradeBan(int accountid) {
        Connection con = null;
        PreparedStatement ps = null;
        PreparedStatement ts = null;

        ResultSet rs = null;

        try {

            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("INSERT INTO connectorban (IP, connecterkey, comment) VALUES (?, ?, ?)");

            ts = con.prepareStatement("SELECT SessionIP, connecterKey FROM accounts WHERE id = ?");
            ts.setInt(1, accountid);
            rs = ts.executeQuery();

            rs.next();
            ps.setString(1, rs.getString("SessionIP"));
            ps.setString(2, rs.getString("connecterKey"));
            ps.setString(3, "서버내복사및악용정지");

            ps.execute();
            rs.close();
            ts.close();
            ps.close();

            ps = con.prepareStatement("UPDATE accounts SET banned = 1, banreason = ?, banby = '운영자' WHERE id = ?");
            ps.setString(1, "서버내복사및악용정지");
            ps.setInt(2, accountid);
//            ps.setString(3, c.getIP());
            ps.executeUpdate();

            ps.close();
            con.close();
        } catch (Exception e) {
            System.out.println("업그레이드 벤 에러 : " + e);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }

            if (ts != null) {
                try {
                    ts.close();
                } catch (Exception e) {
                }
            }

            if (rs != null) {
                try {
                    rs.close();
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
    }

    public boolean getAutoStatus() {
        return autostatus;
    }

    public void setAutoStatus(boolean a) {
        autostatus = a;
    }

    private int symbolExp = 0, symbolMeso = 0, symbolDrop = 0;

    public int getSymbolExp() {
        return symbolExp;
    }

    public void setSysbolExp(int a) {
        symbolExp = a;
    }

    public int getSymbolMeso() {
        return symbolMeso;
    }

    public void setSysbolMeso(int a) {
        symbolMeso = a;
    }

    public int getSymbolDrop() {
        return symbolDrop;
    }

    public void setSysbolDrop(int a) {
        symbolDrop = a;
    }

    public static final int[][] symbolSkill = {
        // 아이템 코드. 스킬 코드, 스탯 증가량, 경험치, 메소
        {3960050, 90, 60, 10, 10}, //무제한 후원 심볼
        {3960060, 91, 40, 5, 5}, //기간제 홍보 심볼

        {3995000, 92, 30, 0, 0}, //궁극의 심볼1
        {3995001, 93, 60, 0, 0}, //궁극의 심볼2
        {3995002, 94, 90, 0, 0}, //궁극의 심볼3
        {3995003, 95, 120, 0, 0}, //궁극의 심볼4
        {3995004, 96, 150, 0, 0}, //궁극의 심볼5
        {3995005, 97, 180, 0, 0}, //궁극의 심볼6
        {3995006, 98, 210, 0, 0}, //궁극의 심볼7
        {3995007, 99, 240, 0, 0}, //궁극의 심볼8
        {3995008, 100, 270, 0, 0}, //궁극의 심볼9
        {3995009, 101, 300, 0, 0}, //궁극의 심볼10

        {3961001, 102, 10, 0, 0}, //리프레 심볼1
        {3961002, 103, 20, 0, 0}, //리프레 심볼2
        {3961003, 104, 30, 0, 0}, //리프레 심볼3
        {3961004, 105, 40, 0, 0}, //리프레 심볼4
        {3961005, 106, 50, 0, 0}, //리프레 심볼5
        {3962001, 107, 10, 0, 0}, //사자왕 심볼1
        {3962002, 108, 20, 0, 0}, //사자왕 심볼2
        {3962003, 109, 30, 0, 0}, //사자왕 심볼3
        {3962004, 110, 40, 0, 0}, //사자왕 심볼4
        {3962005, 111, 50, 0, 0}, //사자왕 심볼5
        {3963001, 112, 10, 0, 0}, //타임로드 심볼1
        {3963002, 113, 20, 0, 0}, //타임로드 심볼2
        {3963003, 114, 30, 0, 0}, //타임로드 심볼3
        {3963004, 115, 40, 0, 0}, //타임로드 심볼4
        {3963005, 116, 50, 0, 0}, //타임로드 심볼5
        {3964001, 117, 10, 0, 0}, //무기고 심볼1
        {3964002, 118, 20, 0, 0}, //무기고 심볼2
        {3964003, 119, 30, 0, 0}, //무기고 심볼3
        {3964004, 120, 40, 0, 0}, //무기고 심볼4
        {3964005, 121, 50, 0, 0}, //무기고 심볼5
        {3965001, 122, 10, 0, 0}, //파괴된 헤네시스 심볼1
        {3965002, 123, 20, 0, 0}, //파괴된 헤네시스 심볼2
        {3965003, 124, 30, 0, 0}, //파괴된 헤네시스 심볼3
        {3965004, 125, 40, 0, 0}, //파괴된 헤네시스 심볼4
        {3965005, 126, 50, 0, 0}, //파괴된 헤네시스 심볼5
        {3966001, 127, 10, 0, 0}, //황혼의 페리온 심볼1
        {3966002, 128, 20, 0, 0}, //황혼의 페리온 심볼2
        {3966003, 129, 30, 0, 0}, //황혼의 페리온 심볼3
        {3966004, 130, 40, 0, 0}, //황혼의 페리온 심볼4
        {3966005, 131, 50, 0, 0}, //황혼의 페리온 심볼5
        {3960900, 132, 10, 0, 2}, //단풍잎 심볼1
        {3960901, 133, 20, 0, 4}, //단풍잎 심볼2
        {3960902, 134, 30, 0, 6}, //단풍잎 심볼3
        {3960903, 135, 40, 0, 8}, //단풍잎 심볼4
        {3960904, 136, 50, 0, 10}, //단풍잎 심볼5
        {3960051, 137, 100, 5, 5}, //50000원 무제한 탐욕의 후원심볼
        {3960052, 138, 100, 5, 5}, //50000원 무제한 탐욕의 홍보 심볼
        {3983000, 139, 50, 5, 5}, //[플레티넘] 후원 심볼
        {3983001, 140, 60, 10, 10}, //[다이아] 후원 심볼
        {3983002, 141, 70, 15, 15}, //[마스터] 후원 심볼
        {3983003, 142, 80, 20, 20}, //[그랜드 마스터] 후원 심볼
        {3983004, 143, 90, 30, 30}, //[챌린저] 후원 심볼
        {3983005, 144, 100, 40, 40}, //[슈퍼 챌린저] 후원 심볼
        {3983006, 145, 150, 20, 20}, //프리미엄 펫 심볼
        {3960070, 146, 100, 10, 10}, //날카로운 데미안 심볼1
        {3960071, 147, 150, 15, 15}, //날카로운 데미안 심볼2
        {3960072, 148, 200, 20, 20}, //날카로운 데미안 심볼3
        {3960073, 149, 250, 25, 25}, //날카로운 데미안 심볼4
        {3960074, 150, 300, 30, 30}, //날카로운 데미안 심볼5
        {3960075, 151, 400, 40, 40}, //날카로운 데미안 심볼6\
        {3960090, 152, 0, 10, 10}, //비비아이스크림
        {3960091, 153, 0, 3, 3}, //비비아이스크림
        {3960092, 154, 0, 3, 3}, //비비아이스크림
        {3960093, 155, 0, 3, 3}, //비비아이스크림
        {3960094, 156, 0, 3, 3}, //비비아이스크림
        {3960095, 157, 0, 3, 3}, //비비아이스크림
    };

    public void rosySymbol() {
        int stra = 0, dexa = 0, inta = 0, luka = 0, watka = 0, matka = 0, sexp = 0;

        this.getStat().setSymbolStr(0);
        this.getStat().setSymbolDex(0);
        this.getStat().setSymbolInt(0);
        this.getStat().setSymbolLuk(0);
        this.setSysbolExp(0);
        this.setSysbolMeso(0);
        for (int a = 0; a < symbolSkill.length; a++) {
            if (this.haveItem(symbolSkill[a][0]) == true) {
                this.setSysbolExp(symbolSkill[a][3]);
                this.setSysbolMeso(symbolSkill[a][4]);
                this.getStat().gainSymbolStr(symbolSkill[a][2]);
                this.getStat().gainSymbolDex(symbolSkill[a][2]);
                this.getStat().gainSymbolInt(symbolSkill[a][2]);
                this.getStat().gainSymbolLuk(symbolSkill[a][2]);
                this.changeSkillLevel_Skip(SkillFactory.getSkill(this.getStat().getSkillByJob(symbolSkill[a][1], this.getJob())), (byte) 1, (byte) 0, true);
            } else {
                this.changeSkillLevel_Skip(SkillFactory.getSkill(this.getStat().getSkillByJob(symbolSkill[a][1], this.getJob())), (byte) -1, (byte) -1, true);
            }
        }
        if (getKeyValue("books1") != null) {
            String[] boos = getKeyValue("books1").split(",");
            for (int i = 0; i < boos.length; i++) {
                if (i <= 9) {
                    if (Integer.parseInt(boos[i]) == 1) {
                        stra += 5;
                        dexa += 5;
                        inta += 5;
                        luka += 5;
                    }
                } else if (i >= 10 && i <= 19) {
                    if (Integer.parseInt(boos[i]) == 1) {
                        stra += 10;
                        dexa += 10;
                        inta += 10;
                        luka += 10;
                    }
                } else {
                    if (Integer.parseInt(boos[i]) == 1) {
                        stra += 15;
                        dexa += 15;
                        inta += 15;
                        luka += 15;
                    }
                }
            }
        }
        if (getKeyValue("books2") != null) {
            String[] boos2 = getKeyValue("books2").split(",");
            for (int i = 0; i < boos2.length; i++) {
                if (Integer.parseInt(boos2[i]) == 1) {
                    stra += 30;
                    dexa += 30;
                    inta += 30;
                    luka += 30;
                }
            }
        }
        if (getKeyValue("books3") != null) {
            String[] boos3 = getKeyValue("books3").split(",");
            for (int i = 0; i < boos3.length; i++) {
                if (Integer.parseInt(boos3[i]) == 1) {
                    stra += 6;
                    dexa += 6;
                    inta += 6;
                    luka += 6;
                }
            }
        }
        if (getKeyValue("booss1") != null) {
            String[] booss2 = getKeyValue("booss1").split(",");
            for (int i = 0; i < booss2.length; i++) {
                if (Integer.parseInt(booss2[i]) == 1) {
                    stra += 30;
                    dexa += 30;
                    inta += 30;
                    luka += 30;
                }
            }
        }
        if (getQuestNAdd(MapleQuest.getInstance(221018)).getCustomData() == null) { //보스컬 100% 달성시 보상1
            getQuestNAdd(MapleQuest.getInstance(221018)).setCustomData("0");
        }
        if (Integer.parseInt(getQuestNAdd(MapleQuest.getInstance(221018)).getCustomData()) != 0) {
            stra += 30;
            dexa += 30;
            inta += 30;
            luka += 30;
        }
        if (getQuestNAdd(MapleQuest.getInstance(221019)).getCustomData() == null) { //재료2컬100% 달성시 보상1
            getQuestNAdd(MapleQuest.getInstance(221019)).setCustomData("0");
        }
        if (Integer.parseInt(getQuestNAdd(MapleQuest.getInstance(221019)).getCustomData()) != 0) {
            stra += 40;
            dexa += 40;
            inta += 40;
            luka += 40;
        }
        if (getQuestNAdd(MapleQuest.getInstance(221020)).getCustomData() == null) { //재료3컬100% 달성시 보상1
            getQuestNAdd(MapleQuest.getInstance(221020)).setCustomData("0");
        }
        if (Integer.parseInt(getQuestNAdd(MapleQuest.getInstance(221020)).getCustomData()) != 0) {
            stra += 100;
            dexa += 100;
            inta += 100;
            luka += 100;
        }

        Map<MapleStat, Integer> statups = new EnumMap<>(MapleStat.class);
        MapleStat stat = null;
        short totalValues = 0;
        stat = MapleStat.STR;
        totalValues = (short) (getStat().getStr() + getStat().getSymbolStr() + stra);
        statups.put(stat, Integer.valueOf(totalValues));
        stat = MapleStat.DEX;
        totalValues = (short) (getStat().getDex() + getStat().getSymbolDex() + dexa);
        statups.put(stat, Integer.valueOf(totalValues));
        stat = MapleStat.INT;
        totalValues = (short) (getStat().getInt() + getStat().getSymbolInt() + inta);
        statups.put(stat, Integer.valueOf(totalValues));
        stat = MapleStat.LUK;
        totalValues = (short) (getStat().getLuk() + getStat().getSymbolLuk() + luka);
        statups.put(stat, Integer.valueOf(totalValues));
        client.getSession().write(MaplePacketCreator.updatePlayerStats(statups, true, this.getJob()));
        getStat().recalcLocalStats(this);
    }

    public void customizeStat(long tCur) {
        customizeStat(tCur, false);
    }

    public void customizeStat(long tCur, boolean inChat) {
        CustomizeStat customizeStat = stats.customizeStat;
        if (customizeStat != null) {
            customizeStat.setFrom(tCur, inChat);
        }
    }

    public void setCTS(MapleBuffStat cts, int skillID, int val) {
        setCTS(cts, skillID, val, false);
    }

    public void setCTS(MapleBuffStat cts, int skillID, int val, boolean delete) {
        CustomizeStat customizeStat = stats.customizeStat;
        if (customizeStat != null) {
            customizeStat.setCTS(cts, skillID, val, delete);
        }
    }

    public void CustomStatEffect(boolean eqcheck) {
        CustomStatEffect(eqcheck, false);
    }

    public void CustomStatEffect(boolean eqcheck, boolean inChat) {

        int watk = 0, matk = 0, gwatk = 0, gmatk = 0, hpmp = 0;;
        int exp = 0, meso = 0, drop = 0, ndamage = 0, bdamage = 0;
        int stra = 0, dexa = 0, inta = 0, luka = 0, watka = 0, matka = 0, sexp = 0;

        Item item = null;
        String owner = "";
        short count = 0;

        for (short i = 1; i < getInventory(MapleInventoryType.CASH).getSlotLimit(); i++) {
            if (getInventory(MapleInventoryType.CASH).getItem(i) != null) {
                item = getInventory(MapleInventoryType.CASH).getItem(i);
                if (item.getItemId() == 5150054 && count < 5) { //오브코드
                    if (!"".equals(item.getOwner())) {

                        owner = item.getOwner();
                        if (getemblem() == count) {
                            //System.out.println("StopEmblem");
                            break;
                        }
                        /*if (owner.split(" ")[1].contains("경험치획득")) {
                         exp += Short.parseShort(owner.split(" ")[2].split("%증가")[0]);
                         //c.getPlayer().dropMessage(5, "테스트 경험치획득 : " + exp);
                         } else if (owner.split(" ")[1].contains("메소획득")) {
                         meso += Short.parseShort(owner.split(" ")[2].split("%증가")[0]);
                         } else if (owner.split(" ")[1].contains("드랍퍼")) {
                         drop += Short.parseShort(owner.split(" ")[2].split("%증가")[0]);
                         } else if (owner.split(" ")[1].contains("일반데미지")) {
                         ndamage += Short.parseShort(owner.split(" ")[2].split("%증가")[0]);
                         } else if (owner.split(" ")[1].contains("보스데미지")) {
                         bdamage += Short.parseShort(owner.split(" ")[2].split("%증가")[0]);
                           
                         } else*/ if (owner.split(" ")[1].contains("힘") && (owner.contains("8등급") || owner.contains("9등급") || owner.contains("10등급"))) {
                            stra += getStat().getStr() * Short.parseShort(owner.split(" ")[2].split("%증가")[0]) / 100;
                        } else if (owner.split(" ")[1].contains("덱스") && (owner.contains("8등급") || owner.contains("9등급") || owner.contains("10등급"))) {
                            dexa += getStat().getDex() * Short.parseShort(owner.split(" ")[2].split("%증가")[0]) / 100;
                        } else if (owner.split(" ")[1].contains("인트") && (owner.contains("8등급") || owner.contains("9등급") || owner.contains("10등급"))) {
                            inta += getStat().getInt() * Short.parseShort(owner.split(" ")[2].split("%증가")[0]) / 100;
                        } else if (owner.split(" ")[1].contains("럭") && (owner.contains("8등급") || owner.contains("9등급") || owner.contains("10등급"))) {
                            luka += getStat().getLuk() * Short.parseShort(owner.split(" ")[2].split("%증가")[0]) / 100;
                        } else if (owner.split(" ")[1].contains("힘")) {
                            stra += (short) Short.parseShort(owner.split(" ")[2].split("증가")[0]);
                        } else if (owner.split(" ")[1].contains("덱스")) {
                            dexa += (short) Short.parseShort(owner.split(" ")[2].split("증가")[0]);
                        } else if (owner.split(" ")[1].contains("인트")) {
                            inta += (short) Short.parseShort(owner.split(" ")[2].split("증가")[0]);
                        } else if (owner.split(" ")[1].contains("럭")) {
                            luka += (short) Short.parseShort(owner.split(" ")[2].split("증가")[0]);
                        }
                        count++;
                    }
                }
            }
        }

        if (this.getGuild() != null) {
            int guild_buff = this.guild_buff;
            int buffwatk = this.guild_stat_watk;
            int buffmatk = this.guild_stat_matk;
            if (guild_buff > 0) {
                gwatk += buffwatk;
                gmatk += buffmatk;
            }
        }
        /* 소울 */
//        int soulid = id;
//        if (soulid > 5010809) {
//            int soullevel = (soulid - 5010809);
//            stra += 200 * soullevel;
//            dexa += 200 * soullevel;
//            inta += 200 * soullevel;
//            luka += 200 * soullevel;
//            watk += 20 * soullevel;
//            matk += 20 * soullevel;
//            //c.getPlayer().dropMessage(5, "현재 착용한 소울 레벨 : " + soullevel);
//        }

        if (getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18) != null) {
            Equip equip = (Equip) getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18);
            watk += equip.getWatk();
            matk += equip.getMatk();
        }
        if (getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -19) != null) {
            Equip equip = (Equip) getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -19);
            watk += equip.getWatk();
            matk += equip.getMatk();
        }
        if (getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -20) != null) {
            Equip equip = (Equip) getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -20);
            watk += equip.getWatk();
            matk += equip.getMatk();
        }

        this.getStat().setSymbolStr(0);
        this.getStat().setSymbolDex(0);
        this.getStat().setSymbolInt(0);
        this.getStat().setSymbolLuk(0);
        this.setSysbolExp(0);
        this.setSysbolMeso(0);

        int bossDAMr = 0;
        int ignoreDEF = 0;
        for (int a = 0; a < symbolSkill.length; a++) {
            if (this.haveItem(symbolSkill[a][0]) == true) {
                this.setSysbolExp(symbolSkill[a][3]);
                this.setSysbolMeso(symbolSkill[a][4]);
                this.getStat().gainSymbolStr(symbolSkill[a][2]);
                this.getStat().gainSymbolDex(symbolSkill[a][2]);
                this.getStat().gainSymbolInt(symbolSkill[a][2]);
                this.getStat().gainSymbolLuk(symbolSkill[a][2]);
                Skill skill = SkillFactory.getSkill(this.getStat().getSkillByJob(symbolSkill[a][1], this.getJob()));
                this.changeSkillLevel_Skip(skill, (byte) 1, (byte) 0, true);
                if (skill != null) {
                    MapleStatEffect skillEffect;
                    if ((skillEffect = skill.getEffect(1)) != null) {
                        bossDAMr += skillEffect.getDAMRate();
                        ignoreDEF += skillEffect.getIgnoreMob();
                    }
                }
            } else {
                this.changeSkillLevel_Skip(SkillFactory.getSkill(this.getStat().getSkillByJob(symbolSkill[a][1], this.getJob())), (byte) -1, (byte) -1, true);
            }
        }
        //this.dropMessage(6, "갱신");
        if (getKeyValue("books1") != null) {
            String[] boos = getKeyValue("books1").split(",");
            for (int i = 0; i < boos.length; i++) {
                if (i <= 9) {
                    if (Integer.parseInt(boos[i]) == 1) {
                        stra += 5;
                        dexa += 5;
                        inta += 5;
                        luka += 5;
                    }
                } else if (i >= 10 && i <= 19) {
                    if (Integer.parseInt(boos[i]) == 1) {
                        stra += 10;
                        dexa += 10;
                        inta += 10;
                        luka += 10;
                    }
                } else {
                    if (Integer.parseInt(boos[i]) == 1) {
                        stra += 15;
                        dexa += 15;
                        inta += 15;
                        luka += 15;
                    }
                }
            }
        }
        if (getKeyValue("books2") != null) {
            String[] boos2 = getKeyValue("books2").split(",");
            for (int i = 0; i < boos2.length; i++) {
                if (Integer.parseInt(boos2[i]) == 1) {
                    stra += 15;
                    dexa += 15;
                    inta += 15;
                    luka += 15;
                }
            }
        }
        if (getKeyValue("books3") != null) {
            String[] boos3 = getKeyValue("books3").split(",");
            for (int i = 0; i < boos3.length; i++) {
                if (Integer.parseInt(boos3[i]) == 1) {
                    stra += 30;
                    dexa += 30;
                    inta += 30;
                    luka += 30;
                }
            }
        }
        if (getKeyValue("booss1") != null) {
            String[] booss2 = getKeyValue("booss1").split(",");
            for (int i = 0; i < booss2.length; i++) {
                if (Integer.parseInt(booss2[i]) == 1) {
                    stra += 30;
                    dexa += 30;
                    inta += 30;
                    luka += 30;
                }
            }
        }
        if (getQuestNAdd(MapleQuest.getInstance(221018)).getCustomData() == null) { //보스컬 100% 달성시 보상1
            getQuestNAdd(MapleQuest.getInstance(221018)).setCustomData("0");
        }
        if (Integer.parseInt(getQuestNAdd(MapleQuest.getInstance(221018)).getCustomData()) != 0) {
            stra += 30;
            dexa += 30;
            inta += 30;
            luka += 30;
        }
        if (getQuestNAdd(MapleQuest.getInstance(221019)).getCustomData() == null) { //재료2컬100% 달성시 보상1
            getQuestNAdd(MapleQuest.getInstance(221019)).setCustomData("0");
        }
        if (Integer.parseInt(getQuestNAdd(MapleQuest.getInstance(221019)).getCustomData()) != 0) {
            stra += 40;
            dexa += 40;
            inta += 40;
            luka += 40;
        }
        if (getQuestNAdd(MapleQuest.getInstance(221020)).getCustomData() == null) { //재료3컬100% 달성시 보상1
            getQuestNAdd(MapleQuest.getInstance(221020)).setCustomData("0");
        }

        if (getQuestNAdd(MapleQuest.getInstance(99898)).getCustomData() == null) {
            getQuestNAdd(MapleQuest.getInstance(99898)).setCustomData("0");
        }
        if (getQuestNAdd(MapleQuest.getInstance(99898)).getCustomData() != null) { //캐릭강화공마
            watk += Integer.parseInt(getQuestNAdd(MapleQuest.getInstance(99898)).getCustomData());
            matk += Integer.parseInt(getQuestNAdd(MapleQuest.getInstance(99898)).getCustomData()) * 2;
        }
        if (Integer.parseInt(getQuestNAdd(MapleQuest.getInstance(221020)).getCustomData()) != 0) {
            stra += 100;
            dexa += 100;
            inta += 100;
            luka += 100;
        }
        for (int[] a : World.ArcaneSimbol.getItems().values()) {
            if (haveItem(a[0], 1)) {
                stra += a[1];
                dexa += a[2];
                inta += a[3];
                luka += a[4];
                watka += a[5];
                matka += a[6];
                //exp += a[7];
                //meso += a[8];
                //drop += a[9];
                //ndamage += a[10];
                //bdamage += a[11];
            }
        }

        Map<MapleStat, Integer> statups = new EnumMap<>(MapleStat.class);
        MapleStat stat = null;
        short totalValues = 0;
        stat = MapleStat.STR;
        totalValues = (short) (getStat().getStr() + getStat().getSymbolStr() + stra);
        statups.put(stat, Integer.valueOf(totalValues));
        stat = MapleStat.DEX;
        totalValues = (short) (getStat().getDex() + getStat().getSymbolDex() + dexa);
        statups.put(stat, Integer.valueOf(totalValues));
        stat = MapleStat.INT;
        totalValues = (short) (getStat().getInt() + getStat().getSymbolInt() + inta);
        statups.put(stat, Integer.valueOf(totalValues));
        stat = MapleStat.LUK;
        totalValues = (short) (getStat().getLuk() + getStat().getSymbolLuk() + luka);
        statups.put(stat, Integer.valueOf(totalValues));
//        stat = MapleStat.MAXHP;
//        totalValues = (short) (this.getStat().getMaxHp() + hpmp);
//        statups.put(stat, Integer.valueOf(totalValues));
//        stat = MapleStat.MAXMP;
//        totalValues = (short) (this.getStat().getMaxMp() + hpmp);
//        statups.put(stat, Integer.valueOf(totalValues));

        Map<MapleBuffStat, Integer> localstatups = new EnumMap<>(MapleBuffStat.class);
        Map<MapleBuffStat, Integer> boosterStatups = new EnumMap<>(MapleBuffStat.class);
        int a = 0;

        for (final Map.Entry<Integer, Integer> qs : this.MATKValue.entrySet()) { // 마력
            if (this.getBuffedValue1(qs.getKey())) {
                a = a + qs.getValue();
                //this.dropMessage(6, "스킬 ID : " + qs.getKey());
                //this.dropMessage(6, "저장된 벨류  : " + qs.getValue());
            } else {
                // this.MATKValue.remove(qs.getKey());
                //this.dropMessage(6, "삭제 처리 스킬 ID : " + qs.getKey());
            }

        }
        //this.dropMessage(6, "계산단 합산된 마력  : " + a);
        int b = 0;
        for (final Map.Entry<Integer, Integer> qs : this.WATKValue.entrySet()) { // 공격력
            if (this.getBuffedValue1(qs.getKey())) {
                b = b + qs.getValue();
                //this.dropMessage(6, "스킬 ID : " + qs.getKey());
                //this.dropMessage(6, "저장된 벨류  : " + qs.getValue());
            } else {
                //  this.WATKValue.remove(qs.getKey());
                //this.dropMessage(6, "삭제 처리 스킬 ID : " + qs.getKey());
            }
        }
        //this.dropMessage(6, "계산단 합산된 공격력  : " + b);

        int booster = 0;
        for (Entry<Integer, Integer> entry : this.boosterVal.entrySet()) {
            booster += -entry.getValue();
        }

        if (booster > 0) {
            long time = 0;
            int skillID = 0;
            MapleStatEffect eff = null;
            for (Entry<Integer, Integer> entry : this.boosterVal.entrySet()) {
                Skill skill = SkillFactory.getSkill(entry.getKey());
                if (skill != null) {
                    eff = skill.getEffect(this.getSkillLevel(entry.getKey()));
                    if (eff != null) {
                        skillID = eff.getSourceId();
                        long startTime = this.getStartTimeFromSkillID(eff.getSourceId());
                        time = (startTime + eff.getDuration()) - System.currentTimeMillis();
                        if (time <= 0) {
                            if (startTime != 0) {
                                booster += entry.getValue();
                                boosterVal.remove(eff.getSourceId());
                                cancelEffectFromSkillID(eff.getSourceId());
                                skillID = 0;
                                time = Integer.MAX_VALUE;

                                for (Entry<MapleBuffStat, Integer> pStatups : eff.getStatups().entrySet()) {
                                    if (pStatups.getKey() != MapleBuffStat.BOOSTER && pStatups.getKey() != MapleBuffStat.WATK && pStatups.getKey() != MapleBuffStat.MATK) {
                                        if (!boosterStatups.containsKey(pStatups.getKey())) {
                                            boosterStatups.put(pStatups.getKey(), pStatups.getValue());
                                        } else {
                                            int oldVal = boosterStatups.get(pStatups.getKey());
                                            boosterStatups.put(pStatups.getKey(), oldVal + pStatups.getValue());
                                        }
                                    }
                                }
                            } else {
                                time = eff.getDuration();
//                                time = Integer.MAX_VALUE;
                            }
                        }
                    }
                } else {
//                    if (skillID == 0) {
                    time = Integer.MAX_VALUE;
//                    }
                }
            }
//            if (superBodyEff != null && superBodyEff.getStatups() != null) {
//                boosterStatups = new EnumMap<>(superBodyEff.getStatups());
//                boosterStatups.remove(MapleBuffStat.BOOSTER);
//            } else if (eff != null && eff.getStatups() != null) {
//                boosterStatups = new EnumMap<>(eff.getStatups());
//                boosterStatups.remove(MapleBuffStat.BOOSTER);
//            } else {
//                boosterStatups = new EnumMap<>(MapleBuffStat.class);
//            }
//            System.err.println(booster);
            boosterStatups.put(MapleBuffStat.BOOSTER, -booster);
//            this.getClient().getSession().write(TemporaryStatsPacket.giveBuff(skillID, (int) time, boosterStatups, eff));
            this.getClient().getSession().write(TemporaryStatsPacket.giveBuff(0, (int) time, boosterStatups, null));
        }
        localstatups.put(MapleBuffStat.WATK, (int) watk + gwatk + watka + this.getStat().getBuffWatk() + b);
        localstatups.put(MapleBuffStat.MATK, (int) matk + gmatk + matka + this.getStat().getBuffMagic() + a);
        this.getClient().getSession().write(TemporaryStatsPacket.giveBuff(0, 10000, localstatups, null));

        this.getClient().getSession().write(MaplePacketCreator.updatePlayerStats(statups, true, this.getJob()));

        if (inChat) {
            String talk = new String();
            talk += String.format("현재 %s님의 추가 효과입니다.\r\n\r\n", name);
            talk += String.format("STR : +%d\r\n", getStat().getStr() + getStat().getSymbolStr() + stra);
            talk += String.format("DEX : +%d\r\n", getStat().getDex() + getStat().getSymbolDex() + dexa);
            talk += String.format("INT : +%d\r\n", getStat().getInt() + getStat().getSymbolInt() + inta);
            talk += String.format("LUK : +%d\r\n", getStat().getLuk() + getStat().getSymbolLuk() + luka);
            talk += String.format("공격력 : +%d\r\n", watk + gwatk + watka + this.getStat().getBuffWatk() + b);
            talk += String.format("마력 : +%d\r\n", matk + gmatk + matka + this.getStat().getBuffMagic() + a);
            talk += String.format("공격속도 : +%d단계\r\n", booster);
            talk += String.format("보스 공격시 데미지 : +%d%%\r\n", (int) (bossDAMr + (getStat().bossdam_r - 100.0)));
            talk += String.format("방어력 무시 : +%d%%\r\n", (int) (ignoreDEF + getStat().ignoreTargetDEF));
            client.sendPacket(MaplePacketCreator.serverNotice(7, 9010000, talk));
        }

    }

    public int getUltimate() {
        return ultimate;
    }

    public void setUltimate(int a) {
        this.ultimate = a;
    }

    public void storageIn(long inventoryItemId, byte invType) {
        if (cubeitemid > 0) {
            cubeitemid = 0;
        }
        int itemId = 0;
        short position = 0;
        short quantity = 0;
        String owner = "";
        String GM_Log = "";
        int uniqueId = 0;
        short flag = 0;
        long expiredate = 0;
        byte type = 0;
        String sender = "";
        int marriageId = 0;

        byte upgradeSlots = 0;
        byte level = 0;
        short str = 0;
        short dex = 0;
        short _int = 0;
        short luk = 0;
        short hp = 0;
        short mp = 0;
        short watk = 0;
        short matk = 0;
        short wdef = 0;
        short mdef = 0;
        short acc = 0;
        short avoid = 0;
        short hands = 0;
        short speed = 0;
        short jump = 0;
        byte ViciousHammer = 0;
        int ItemEXP = 0;
        int durability = 0;
        byte enhance = 0;
        int potential1 = 0;
        int potential2 = 0;
        int potential3 = 0;
        short hpR = 0;
        short mpR = 0;
        int incSkill = 0;
        short charmEXP = 0;
        short pvpDamage = 0;
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            MapleInventoryType invTypeR = null;

            switch (invType) {
                case 1:
                    invTypeR = MapleInventoryType.EQUIP;
                    break;
                case 2:
                    invTypeR = MapleInventoryType.USE;
                    break;
                case 3:
                    invTypeR = MapleInventoryType.SETUP;
                    break;
                case 4:
                    invTypeR = MapleInventoryType.ETC;
                    break;
                case 5:
                    invTypeR = MapleInventoryType.CASH;
            }

            int length = getInventory(invTypeR).getSlotLimit();

            for (short i = 0; i < length; i++) {
                Item dItem = getInventory(invTypeR).getItem(i);

                if (dItem == null) {
                    continue;
                }

                if (dItem.getInventoryId() == inventoryItemId) {
                    itemId = dItem.getItemId();
                    position = dItem.getPosition();
                    quantity = dItem.getQuantity();
                    owner = dItem.getOwner();
                    GM_Log = dItem.getGMLog();
                    uniqueId = dItem.getUniqueId();
                    flag = dItem.getFlag();
                    expiredate = dItem.getExpiration();
                    sender = dItem.getGiftFrom();
                    marriageId = dItem.getMarriageId();

                    if (invType == 1) {
                        Equip dItem2 = (Equip) dItem;

                        upgradeSlots = dItem2.getUpgradeSlots();
                        level = dItem2.getLevel();
                        str = dItem2.getStr();
                        dex = dItem2.getDex();
                        _int = dItem2.getInt();
                        luk = dItem2.getLuk();
                        hp = dItem2.getHp();
                        mp = dItem2.getMp();
                        watk = dItem2.getWatk();
                        matk = dItem2.getMatk();
                        wdef = dItem2.getWdef();
                        mdef = dItem2.getMdef();
                        acc = dItem2.getAcc();
                        avoid = dItem2.getAvoid();
                        hands = dItem2.getHands();
                        speed = dItem2.getSpeed();
                        jump = dItem2.getJump();
                        ViciousHammer = dItem2.getViciousHammer();
                        ItemEXP = dItem2.getItemEXP();
                        durability = dItem2.getDurability();
                        enhance = dItem2.getEnhance();
                        potential1 = dItem2.getPotential1();
                        potential2 = dItem2.getPotential2();
                        potential3 = dItem2.getPotential3();
                        hpR = dItem2.getHpR();
                        mpR = dItem2.getMpR();
                        incSkill = dItem2.getIncSkill();
                        charmEXP = dItem2.getCharmEXP();
                        pvpDamage = dItem2.getPVPDamage();
                    }

                    MapleInventoryManipulator.removeFromSlot(getClient(), invTypeR, i, (short) quantity, false);
                    break;
                }
            }

            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("INSERT INTO newstorage VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", DatabaseConnection.RETURN_GENERATED_KEYS);
            ps.setLong(1, accountid);
            ps.setInt(2, itemId);
            ps.setByte(3, invType);
            ps.setShort(4, position);
            ps.setShort(5, quantity);
            ps.setString(6, owner);
            ps.setString(7, GM_Log);
            ps.setLong(8, uniqueId);
            ps.setShort(9, flag);
            ps.setLong(10, expiredate);
            ps.setByte(11, type);
            ps.setString(12, sender);
            ps.setInt(13, marriageId);
            ps.executeUpdate();

            if (invType == 1) {
                rs = ps.getGeneratedKeys();

                long tempData = 0;

                if (rs.next()) {
                    tempData = rs.getLong(1);
                }

                ps = con.prepareStatement("INSERT INTO newstorageequipment VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                ps.setLong(1, tempData);
                ps.setByte(2, upgradeSlots);
                ps.setByte(3, level);
                ps.setShort(4, str);
                ps.setShort(5, dex);
                ps.setShort(6, _int);
                ps.setShort(7, luk);
                ps.setShort(8, hp);
                ps.setShort(9, mp);
                ps.setShort(10, watk);
                ps.setShort(11, matk);
                ps.setShort(12, wdef);
                ps.setShort(13, mdef);
                ps.setShort(14, acc);
                ps.setShort(15, avoid);
                ps.setShort(16, hands);
                ps.setShort(17, speed);
                ps.setShort(18, jump);
                ps.setByte(19, ViciousHammer);
                ps.setInt(20, ItemEXP);
                ps.setInt(21, durability);
                ps.setByte(22, enhance);
                ps.setInt(23, potential1);
                ps.setInt(24, potential2);
                ps.setInt(25, potential3);
                ps.setShort(26, hpR);
                ps.setShort(27, mpR);
                ps.setInt(28, incSkill);
                ps.setShort(29, charmEXP);
                ps.setShort(30, pvpDamage);
                ps.executeUpdate();
                rs.close();
            }

            ps.close();
            con.close();

            String talk = new String();
            talk += getName() + "(ACCID:" + client.getAccID() + ") | 아이템 코드 : " + itemId + "(" + invTypeR.name() + ") | 인벤토리 ID : " + inventoryItemId + " | 갯수 : " + quantity + "\r\n";
            FileoutputUtil.log(FileoutputUtil.뻐꾸기보관, talk);
        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
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
    }

    public String storageView(byte type) {
        String view = "";

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM newstorage WHERE accountid = ? AND inventorytype = ?");
            ps.setLong(1, accountid);
            ps.setByte(2, type);
            rs = ps.executeQuery();

            while (rs.next()) {
                long invId = rs.getLong("inventoryitemid");
                int itemId = rs.getInt("itemid");
                short quantity = rs.getShort("quantity");

                if (type != 1) {
                    view += "#L" + invId + "##i" + itemId + "# #b#z" + itemId + "##k " + quantity + "개\r\n";
                } else {
                    view += "#L" + invId + "##i" + itemId + "# #b#z" + itemId + "##k\r\n";
                }
            }

            rs.close();
            ps.close();
            con.close();
        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                }
            }
        }

        return view;
    }

    public void storageOut(long inventoryItemId) {
        if (cubeitemid > 0) {
            cubeitemid = 0;
        }
        int itemId = 0;
        byte inventoryType = 0;
        short position = 0;
        short quantity = 0;
        String owner = "";
        String GM_Log = "";
        int uniqueId = 0;
        short flag = 0;
        long expiredate = 0;
        byte type = 0;
        String sender = "";
        int marriageId = 0;

        byte upgradeSlots = 0;
        byte level = 0;
        short str = 0;
        short dex = 0;
        short _int = 0;
        short luk = 0;
        short hp = 0;
        short mp = 0;
        short watk = 0;
        short matk = 0;
        short wdef = 0;
        short mdef = 0;
        short acc = 0;
        short avoid = 0;
        short hands = 0;
        short speed = 0;
        short jump = 0;
        byte ViciousHammer = 0;
        int ItemEXP = 0;
        int durability = 0;
        byte enhance = 0;
        int potential1 = 0;
        int potential2 = 0;
        int potential3 = 0;
        short hpR = 0;
        short mpR = 0;
        int incSkill = 0;
        short charmEXP = 0;
        short pvpDamage = 0;

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM newstorage WHERE inventoryitemid = ?");
            ps.setLong(1, inventoryItemId);
            rs = ps.executeQuery();

            if (rs.next()) {
                itemId = rs.getInt("itemid");
                inventoryType = rs.getByte("inventorytype");
                position = rs.getByte("position");
                quantity = rs.getShort("quantity");
                owner = rs.getString("owner");
                GM_Log = rs.getString("GM_Log");
                uniqueId = rs.getInt("uniqueid");
                flag = rs.getShort("flag");
                expiredate = rs.getLong("expiredate");
                type = rs.getByte("type");
                sender = rs.getString("sender");
                marriageId = rs.getInt("marriageId");
            }

            if (inventoryType == 1) {
                ps = con.prepareStatement("SELECT * FROM newstorageequipment WHERE inventoryitemid = ?");
                ps.setLong(1, inventoryItemId);
                rs = ps.executeQuery();

                if (rs.next()) {
                    upgradeSlots = rs.getByte("upgradeslots");
                    level = rs.getByte("level");
                    str = rs.getShort("str");
                    dex = rs.getShort("dex");
                    _int = rs.getShort("int");
                    luk = rs.getShort("luk");
                    hp = rs.getShort("hp");
                    mp = rs.getShort("mp");
                    watk = rs.getShort("watk");
                    matk = rs.getShort("matk");
                    wdef = rs.getShort("wdef");
                    mdef = rs.getShort("mdef");
                    acc = rs.getShort("acc");
                    avoid = rs.getShort("avoid");
                    hands = rs.getShort("hands");
                    speed = rs.getShort("speed");
                    jump = rs.getShort("jump");
                    ViciousHammer = rs.getByte("ViciousHammer");
                    ItemEXP = rs.getInt("itemEXP");
                    durability = rs.getInt("durability");
                    enhance = rs.getByte("enhance");
                    potential1 = rs.getInt("potential1");
                    potential2 = rs.getInt("potential2");
                    potential3 = rs.getInt("potential3");
                    hpR = rs.getShort("hpR");
                    mpR = rs.getShort("mpR");
                    incSkill = rs.getInt("incSkill");
                    charmEXP = rs.getShort("charmEXP");
                    pvpDamage = rs.getShort("pvpDamage");
                }
            }

            rs.close();

            ps = con.prepareStatement("DELETE FROM newstorage WHERE inventoryitemid = ?");
            ps.setLong(1, inventoryItemId);
            ps.executeUpdate();

            if (inventoryType == 1) {
                ps = con.prepareStatement("DELETE FROM newstorageequipment WHERE inventoryitemid = ?");
                ps.setLong(1, inventoryItemId);
                ps.executeUpdate();
            }

            if (inventoryType != 1) {
                Equip gItem1 = new Equip(itemId, position, (byte) 0);
                //if (itemId == 3994025 || itemId == 5212000 || itemId == 5211000) {
                if (expiredate > 0) {
                    if (expiredate - System.currentTimeMillis() > 0) {
                        gItem1.setExpiration(expiredate);
                        MapleInventoryManipulator.addFromDrop(getClient(), gItem1, true);
                    } else {
                        dropMessage(1, "해당아이템은 사용기간이 만료되어 삭제되었습니다.");
                        dropMessage(5, "해당아이템은 사용기간이 만료되어 삭제되었습니다.");
                    }
                } else {
                    gainItem(itemId, quantity);
                }
            } else {
                Equip gItem = new Equip(itemId, position, (byte) 0);

                gItem.setOwner(owner);
                gItem.setGMLog(GM_Log);
                gItem.setUniqueId(uniqueId);
                gItem.setFlag(flag);
                gItem.setExpiration(expiredate);
                gItem.setGiftFrom(sender);
                gItem.setMarriageId(marriageId);

                gItem.setUpgradeSlots(upgradeSlots);
                gItem.setLevel(level);
                gItem.setStr(str);
                gItem.setDex(dex);
                gItem.setInt(_int);
                gItem.setLuk(luk);
                gItem.setHp(hp);
                gItem.setMp(mp);
                gItem.setWatk(watk);
                gItem.setMatk(matk);
                gItem.setWdef(wdef);
                gItem.setMdef(mdef);
                gItem.setAcc(acc);
                gItem.setAvoid(avoid);
                gItem.setHands(hands);
                gItem.setSpeed(speed);
                gItem.setJump(jump);
                gItem.setViciousHammer(ViciousHammer);
                gItem.setItemEXP(ItemEXP);
                gItem.setDurability(durability);
                gItem.setEnhance(enhance);
                gItem.setPotential1(potential1);
                gItem.setPotential2(potential2);
                gItem.setPotential3(potential3);
                gItem.setHpR(hpR);
                gItem.setMpR(mpR);
                gItem.setIncSkill(incSkill);
                gItem.setCharmEXP(charmEXP);
                gItem.setPVPDamage(pvpDamage);

                MapleInventoryManipulator.addFromDrop(getClient(), gItem, true);
            }
            ps.close();
            con.close();

            String talk = new String();
            talk += getName() + "(ACCID:" + client.getAccID() + ") | 아이템 코드 : " + itemId + "(" + GameConstants.getInventoryType(itemId).name() + ") | 인벤토리 ID : " + inventoryItemId + " | 갯수 : " + quantity + "\r\n";
            FileoutputUtil.log(FileoutputUtil.뻐꾸기회수, talk);

            this.saveToDB(false, false);

        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                }
            }
        }
    }

    public void sethottimeboss(boolean check) {
        this.hottimeboss = check;

    }

    public boolean gethottimeboss() {
        return hottimeboss;
    }

    public void sethottimebosslastattack(boolean check) {
        if (check) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat(this.getName() + "님께서 월드보스 막타보상을 획득하셨습니다. 축하합니다!!"));
        }
        this.hottimebosslastattack = check;
    }

    public boolean gethottimebosslastattack() {
        return hottimebosslastattack;
    }

    public void sethottimebossattackcheck(boolean check) {
        this.hottimebossattackcheck = check;
    }

    public boolean gethottimebossattackcheck() {
        return hottimebossattackcheck;
    }

    public String getNum(long dd) {
        String df = new DecimalFormat("###,###,###,###,###,###").format(dd);
        return df;
    }

    public int getemblem() {
        return emblem;
    }

    public final String getToday() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String time = sdf.format(Calendar.getInstance().getTime());
        return time;
    }

    public FileTime getEquipExtExpire() {
        if (equipExtExpire == null) {
            return FileTime.START;
        }
        return equipExtExpire;
    }

    public void setEquipExtExpire(FileTime ft) {
        this.equipExtExpire = ft;
    }

    public void seteventboss(boolean check) {
        this.eventboss = check;
    }

    public boolean geteventboss() {
        return eventboss;
    }

    public void seteventbosslastattack(boolean check) {
        if (check) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat(this.getName() + "님께서 랜덤 보스를 처치하셨습니다!! 축하합니다!!"));
        }
        this.eventbosslastattack = check;
    }

    public boolean geteventbosslastattack() {
        return eventbosslastattack;
    }

    public void seteventbossattackcheck(boolean check) {
        this.eventbossattackcheck = check;
    }

    public boolean geteventossattackcheck() {
        return eventbossattackcheck;
    }

    public void initializeDeathCount() {
        if (/*this.eventInstance != null && */this.map != null) {
            int count = MapConstants.getDeathCount(this.map.getId());
            if (count != 0) {
                if (getDeathCount() == 0) { //데카가 0일 때, 즉 처음 입장 때만 적용
                    MapleQuestStatus data = this.getQuestNAdd(MapleQuest.getInstance(20230424));
                    data.setCustomData(String.valueOf(count));
                    dropMessage(2, "[시스템] : 데스 카운트 " + count + "회가 설정 되었습니다.");
                    MapleQuestStatus emData = this.getQuestNAdd(MapleQuest.getInstance(202304240));
                    emData.setCustomData(eventInstance.getName());
                    saveToDB(false, false);
                } else if (eventInstance == null && getDeathCount() == -1) { //데카 다 썻는데 죽었을 때
                    removeDeathCount();
                    saveToDB(false, false);
                }
            } else {
                MapleQuestStatus emData = this.getQuestNAdd(MapleQuest.getInstance(202304240));
                String instanceName = emData.getCustomData();
                if (instanceName != null) {
                    EventManager em = client.getChannelServer().getEventSM().getEventManager(instanceName);
                    if (em != null) {
                        EventInstanceManager eim = em.getInstance(instanceName);
                        if (eim != null) {
                            if (eim.getPlayers().size() <= 0) {
                                removeDeathCount();
                            }
                        } else {
                            removeDeathCount();
                        }
                    } else {
                        removeDeathCount();
                    }
                } else { //저장된 원정이 없는데
                    if (getDeathCount() > 0) { //데카가 0보다 많을 때
                        removeDeathCount(); //삭제
                        saveToDB(false, false);
                    } else if (eventInstance == null && getDeathCount() == -1) { //데카 다 썻는데 초기화 안됬을 때
                        removeDeathCount();
                        saveToDB(false, false);
                    }
                }
            }
        }
    }

    public void removeDeathCount() {
        MapleQuestStatus data = this.getQuestNAdd(MapleQuest.getInstance(20230424));
        data.setCustomData("0");
        MapleQuestStatus emData = this.getQuestNAdd(MapleQuest.getInstance(202304240));
        emData.setCustomData(null);
//        saveToDB(false, false);
    }

    public int getDeathCount() {
        MapleQuestStatus data = this.getQuestNAdd(MapleQuest.getInstance(20230424));
        if (data.getCustomData() == null/* || eventInstance == null*/) {
            data.setCustomData("0");
        }
        return Integer.parseInt(data.getCustomData());
    }

    public void decreaseDeathCount() { //태어날 때만 저장댐
        int deathCount;
        if ((deathCount = getDeathCount()) > 0) {
            deathCount--;
            MapleQuestStatus data = this.getQuestNAdd(MapleQuest.getInstance(20230424));
            data.setCustomData(String.valueOf(deathCount));
            dropMessage(2, "[시스템] : 데스 카운트가 " + deathCount + "회 남았습니다.");
            if (deathCount <= 0) {
                data.setCustomData("-1");
                MapleQuestStatus emData = this.getQuestNAdd(MapleQuest.getInstance(202304240));
                emData.setCustomData(null);
            }
            saveToDB(false, false);
        }
    }

    public short EmblemPoketItem(int itemid) { //엠블렘 포켓류 바로장착1 소스 / 코드
        short val = 0;
        int[] 엠블렘 = {
            1133002, 1133003, 1133004, 1133005, 1133006, 1133007, 1133008, 1133009
        };
        int[] 저주마도서 = {
            1133100,
            1133101,
            1133102,
            1133103,
            1133104,
            1133105
        };
        int[] 창세뱃지 = {
            1133992,};
        int[] 미트라분노 = {
            1133993
        };

        //엠블렘 포켓 장착 포지션 설정 / 지정1
        for (int i = 0; i < 엠블렘.length; i++) {
            if (엠블렘[i] == itemid) {
                val = -20;
                break;
            }
        }
        for (int i = 0; i < 저주마도서.length; i++) {
            if (저주마도서[i] == itemid) {
                val = -120;
                break;
            }
        }
        for (int i = 0; i < 미트라분노.length; i++) {
            if (미트라분노[i] == itemid) {
                val = -14; //-114 캐시
                break;

            }
        }
        for (int i = 0; i < 창세뱃지.length; i++) {
            if (창세뱃지[i] == itemid) {
                val = -19;
                break;
            }
        }
        return val;
    }

    public final boolean isLeader() {
        if (getParty() == null) {
            return false;
        }
        return getParty().getLeader().getId() == getId();
    }

    public final void warpParty(final int mapId) {
        if (getClient().getPlayer().getParty() == null || getClient().getPlayer().getParty().getMembers().size() == 1) {
            warp(mapId);
            return;
        }
        final MapleMap target = ChannelServer.getInstance(getClient().getChannel()).getMapFactory().getMap(mapId);
        final int cMap = getClient().getPlayer().getMapId();
        for (final MaplePartyCharacter chr : getClient().getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getClient().getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getClient().getPlayer().getEventInstance())) {
                curChar.changeMap(target, target.getPortal(0));
            }
        }
    }

    public long getRCDamage() {
        if (getKeyValue("rc_damage") == null) {
            return 0;
        }
        CustomizeStat customizeStat = stats.customizeStat;
        return Long.parseLong(getKeyValue("rc_damage")) + customizeStat.addDamage;
    }

    public boolean getBossGage() {
        return bossGage;
    }

    public void setBossGage(boolean a) {
        bossGage = a;
    }
    private boolean bossGage = false;

    private boolean showDamageHint = false; // 기본값은 false로 설정

    public boolean isShowDamageHint() {
        return showDamageHint;
    }

    public void setShowDamageHint(boolean showDamageHint) {
        this.showDamageHint = showDamageHint;
    }

    public Map<String, String> getKeyValues() {
        return keyValues;
    }

    public int getAddOrbCount() {
        int addCount = 0;
        if (level >= 200) {
            addCount = (int) Math.floor((level - 200) / 50);
        }
        return addCount;
    }

    public void updateOneInfoQuest(int questid, String key, String value) {
        String allValues = getInfoQuest(questid);
        if (!allValues.equals("")) {
            Map<String, String> values = new HashMap<String, String>();
            String[] keyvalues = allValues.split(";");
            for (int i = 0; i < keyvalues.length; i++) {
                String[] keyandvalue = keyvalues[i].split("=");
                values.put(keyandvalue[0], keyandvalue[1]);
            }
            if (values.containsKey(key)) {
                values.remove(key);
            }
            values.put(key, value);
            allValues = "";
            int size = 1;
            for (Entry<String, String> e : values.entrySet()) {
                allValues += e.getKey() + "=" + e.getValue();
                if (size < values.size()) {
                    allValues += ";";
                }
                size++;
            }
        } else {
            allValues = key + "=" + value;
        }
        updateInfoQuest(questid, allValues);
    }

    public String getOneInfoQuest(int questid, String key) {
        String allValues = getInfoQuest(questid);
        if (!allValues.equals("")) {
            Map<String, String> values = new HashMap<String, String>();
            String[] keyvalues = allValues.split(";");
            for (int i = 0; i < keyvalues.length; i++) {
                String[] keyandvalue = keyvalues[i].split("=");
                values.put(keyandvalue[0], keyandvalue[1]);
            }
            if (values.containsKey(key)) {
                return values.get(key);
            }
        }
        return "";
    }
}
