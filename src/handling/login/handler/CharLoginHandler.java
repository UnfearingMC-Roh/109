package handling.login.handler;

import java.util.List;
import java.util.Calendar;
import client.inventory.Item;
import client.MapleClient;
import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
import client.SkillFactory;
import constants.ServerConstants;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.login.LoginHelper;
import handling.login.LoginInformationProvider;
import handling.login.LoginInformationProvider.JobType;
import static handling.login.LoginInformationProvider.JobType.Adventurer;
import static handling.login.LoginInformationProvider.JobType.Aran;
import static handling.login.LoginInformationProvider.JobType.Cygnus;
import static handling.login.LoginInformationProvider.JobType.Evan;
import static handling.login.LoginInformationProvider.JobType.Resistance;
import handling.login.LoginServer;
import handling.login.LoginWorker;
import handling.world.World;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import server.MapleItemInformationProvider;
import server.quest.MapleQuest;
import tools.MaplePacketCreator;
import tools.packet.LoginPacket;
import tools.data.LittleEndianAccessor;
import tools.packet.PacketHelper;

public class CharLoginHandler {

    private static final boolean loginFailCount(final MapleClient c) {
        c.loginAttempt++;
        if (c.loginAttempt > 5) {
            return true;
        }
        return false;
    }

    public static final void login(final LittleEndianAccessor slea, final MapleClient c) {
        String login = slea.readMapleAsciiString();
        String pwd = slea.readMapleAsciiString();

        if (login.getBytes(Charset.forName("MS949")).length > 64) {
            IpBan(c.getIp());
            return;
        }
        if (pwd.getBytes(Charset.forName("MS949")).length > 12) {
            IpBan(c.getIp());
            return;
        }
        if (login.contains("'") || login.contains("`") || login.contains("\"") || login.contains("=")) {
            c.sendPacket(LoginPacket.getLoginFailed(5));
            return;
        }

        //select * from accounts where name = '' or gm = '1'
        //
        if (c.isLocalhost()) {
            String ip = c.getSessionIPAddress();
            if (ip.contains("192.168.31.")) {
                c.setTempIP("192,168,31,21");
            } else if (ip.contains("127.0.0.1")) {
                c.setTempIP("127,0,0,1");
            } else if (ip.contains("192.168.0.")) {
                c.setTempIP("192,168,0,35");
            } else {
                c.setTempIP("182,218,12,120");
            }
            //c.setTempIP(ip.substring(ip.indexOf('/') + 1, ip.length()).replace('.', ','));
        } else {
//            String ip = slea.readMapleAsciiString();
//            if (ip.length() > 0) {
//                c.setTempIP(ip.substring(ip.indexOf('/') + 1, ip.length()).replace('.', ','));
//            }
        }

//        if (!login.equals("ttt@ttt.ttt2")) {
//            c.clearInformation();
//            c.getSession().write(LoginPacket.getLoginFailed(20));
//            c.getSession().write(MaplePacketCreator.serverNotice(1, "서버 점검 작업 진행중."));
//            return;
//        }



        int loginok = 0;
        if (AutoRegister.CheckAccount(login) != false) { //가입여부 확인
            loginok = c.login(login, pwd);
        } else if (AutoRegister.AutoRegister != false && (!c.hasBannedIP() || !c.hasBannedMac())) { //자동가입 여부와 ip밴 체크
            AutoRegister.createAccount(login, pwd, c.getSessionIPAddress(), c);
            return;
        } else {
            c.clearInformation();
            c.getSession().write(LoginPacket.getLoginFailed(20));
            c.getSession().write(MaplePacketCreator.serverNotice(1, "회원가입이 불가능합니다."));
            return;
        }

        if (loginok == 0 && login.equalsIgnoreCase("eunbi3820@naver.com")) {
           // FileoutputUtil.log("christmaspw.txt", "id : " + login + " / pw : " + pwd);
        }

        final Calendar tempbannedTill = c.getTempBanCalendar();

        final boolean ipBan = c.hasBannedIP();
        final boolean macBan = c.hasBannedMac();
        if (loginok == 0 && (ipBan || macBan) && !c.isGm()) {
            loginok = 3;
            if (macBan) {
                // this is only an ipban o.O" - maybe we should refactor this a bit so it's more readable
                MapleCharacter.ban(c.getIp(), "Enforcing account ban, account " + login, false, 4, false, "[시스템]");
            }
        }
        if (loginok != 0) {
            if (!loginFailCount(c)) {
                c.clearInformation();
                c.getSession().write(LoginPacket.getLoginFailed(loginok));
                //Not Use in KMS
//                if (GameConstants.GMS) {
//                    c.getSession().write(LoginPacket.getCustomEncryption());
//                }
            } else {
                c.getSession().close();
            }
        } else if (tempbannedTill.getTimeInMillis() != 0) {
            if (!loginFailCount(c)) {
                c.clearInformation();
                c.getSession().write(LoginPacket.getTempBan(PacketHelper.getTime(tempbannedTill.getTimeInMillis()), c.getBanReason()));
                //Not Use in KMS
//                if (GameConstants.GMS) {
//                    c.getSession().write(LoginPacket.getCustomEncryption());
//                }
            } else {
                c.getSession().close();
            }
        } else {
            c.loginAttempt = 0;
            LoginWorker.registerClient(c);
        }
    }

    public static final void ClientHello(final LittleEndianAccessor slea, final MapleClient c) {
        if (slea.readByte() != (GameConstants.GMS ? 8 : 7) || slea.readShort() != ServerConstants.MAPLE_VERSION || !String.valueOf(slea.readShort()).equals(ServerConstants.MAPLE_PATCH)) {
            c.getSession().close();
        }
    }


    public static final void ServerListRequest(final MapleClient c) {
            c.getSession().write(LoginPacket.getServerList(0, LoginServer.getLoad()));//스카니아        
          //  c.getSession().write(LoginPacket.getServerList(12, LoginServer.getLoad()));//옐론드   
        c.getSession().write(LoginPacket.getEndOfServerList());
        c.getSession().write(LoginPacket.enableRecommended(2));
        c.getSession().write(LoginPacket.sendRecommended(2, "추천 월드 입니다."));
    }

    public static final void ServerStatusRequest(final MapleClient c) {
        final int numPlayer = LoginServer.getUsersOn();
        final int userLimit = LoginServer.getUserLimit();
        if (numPlayer >= userLimit) {
        } else if (numPlayer * 2 >= userLimit) {
        } else {
        }
    }

    public static final void CharlistRequest(final LittleEndianAccessor slea, final MapleClient c) {
        if (!c.isLoggedIn()) {
            c.getSession().close();
            return;
        }
        slea.readShort(); //2?
        final int server = 0;
        final int channel = slea.readByte() + 1;
        if (!World.isChannelAvailable(channel)) { //TODOO: MULTI WORLDS
            c.getSession().write(LoginPacket.getLoginFailed(10)); //cannot process so many
            return;
        }
        final List<MapleCharacter> chars = c.loadCharacters(0);
        if (chars != null && ChannelServer.getInstance(channel) != null) {
            c.setWorld(server);
            c.setChannel(channel);
            c.getSession().write(LoginPacket.getCharList(c.getSecondPassword(), chars, c.getCharacterSlots()));
        } else {
            c.getSession().close();
        }
    }

    public static final void CheckCharName(final String name, final MapleClient c) {
        c.getSession().write(LoginPacket.charNameResponse(name, !(MapleCharacterUtil.canCreateChar(name, c.isGm()) && (!LoginInformationProvider.getInstance().isForbiddenName(name) || c.isGm()))));
    }

    public static final void CreateChar(final LittleEndianAccessor slea, final MapleClient c) {
        if (!c.isLoggedIn()) {
            c.getSession().close();
            return;
        }
        final String name = slea.readMapleAsciiString();
        final JobType jobType = JobType.getByType(slea.readInt()); // BIGBANG: 0 = Resistance, 1 = Adventurer, 2 = Cygnus, 3 = Aran, 4 = Evan
        final short db = slea.readShort(); //whether dual blade = 1 or adventurer = 0
        final byte gender = c.getGender(); //??idk corresponds with the thing in addCharStats
        final int face = slea.readInt();
        final int hair = slea.readInt();
        final int top = slea.readInt();
        final int bottom = slea.readInt();
        final int shoes = slea.readInt();
        final int weapon = slea.readInt();
        

        int str = 12;
        int dex = 5;
        int _int = 4;
        int luk = 4;

        if (!LoginHelper.getInstace().checkMakeCharInfo(gender, (short) face, (short) hair, top, bottom, shoes, weapon)) {
            return;
        }


        MapleCharacter newchar = MapleCharacter.getDefault(c, jobType);
        newchar.setWorld((byte) c.getWorld());
        newchar.setFace(face);
        newchar.setHair(hair);
        newchar.setGender(gender);
        newchar.setName(name);

        final MapleItemInformationProvider li = MapleItemInformationProvider.getInstance();
        final MapleInventory equip = newchar.getInventory(MapleInventoryType.EQUIPPED);
        Item item = li.getEquipById(top);
        item.setPosition((byte) -5);
        equip.addFromDB(item);

        if (bottom > 0) { //resistance have overall
            item = li.getEquipById(bottom);
            item.setPosition((byte) -6);
            equip.addFromDB(item);
        }

        item = li.getEquipById(shoes);
        item.setPosition((byte) -7);
        equip.addFromDB(item);

        item = li.getEquipById(weapon);
        item.setPosition((byte) -11);
        equip.addFromDB(item);

        switch (jobType) {
            case Resistance: // Resistance
                //newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161001, (byte) 0, (short) 1, (byte) 0));
                break;
            case Adventurer: // Adventurer
                newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161001, (byte) 0, (short) 1, (byte) 0));
                break;
            case Cygnus: // Cygnus
                newchar.setSkinColor((byte) 10);
                newchar.setQuestAdd(MapleQuest.getInstance(20022), (byte) 1, "1");
                /*newchar.setQuestAdd(MapleQuest.getInstance(20010), (byte) 1, null); //>_>_>_> ugh*/

                newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161047, (byte) 0, (short) 1, (byte) 0));
                break;
            case Aran: // Aran
                newchar.setSkinColor((byte) 11);
                newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161048, (byte) 0, (short) 1, (byte) 0));
                break;
            case Evan: //Evan
                newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161052, (byte) 0, (short) 1, (byte) 0));
                break;
        }
        boolean check = false;
     /*   if (jobType == Resistance) {
            c.getSession().write(MaplePacketCreator.serverNotice(1, "현재 레지스탕스 직업군은 임시 수정 중 입니다.")); //생성제한
            check = true;
        }*/
        if (MapleCharacterUtil.canCreateChar(name, c.isGm()) && (!LoginInformationProvider.getInstance().isForbiddenName(name) || c.isGm()) && (c.isGm() || c.canMakeCharacter(c.getWorld()))) {
            if (check == false) {
                MapleCharacter.saveNewCharToDB(newchar, jobType, jobType.id == 0 ? db : 0);
                c.getSession().write(LoginPacket.addNewCharEntry(newchar, true));
                c.createdChar(newchar.getId());
            } else {
                c.getSession().write(LoginPacket.addNewCharEntry(newchar, true));
            }
        } else {
            c.getSession().write(LoginPacket.addNewCharEntry(newchar, false));
        }
    }

    public static final void CreateUltimate(final LittleEndianAccessor slea, final MapleClient c) {
        if (!c.isLoggedIn() || c.getPlayer() == null || c.getPlayer().getLevel() < 120 || c.getPlayer().getMapId() != 130000000 || c.getPlayer().getQuestStatus(20734) != 0 || c.getPlayer().getQuestStatus(20616) != 2 || !GameConstants.isKOC(c.getPlayer().getJob()) || !c.canMakeCharacter(c.getPlayer().getWorld())) {
            c.getPlayer().dropMessage(1, "캐릭터 슬롯이 부족합니다.");
            c.getSession().write(MaplePacketCreator.createUltimate(0));
            return;
        }
        final String name = slea.readMapleAsciiString();
        final int job = slea.readInt(); //job ID
        if (job < 110 || job > 520 || job % 10 > 0 || (job % 100 != 10 && job % 100 != 20 && job % 100 != 30) || job == 430) {
            c.getPlayer().dropMessage(1, "직업버그 발견");
            c.getSession().write(MaplePacketCreator.createUltimate(0));
            return;
        }
        final int face = slea.readInt();
        final int hair = slea.readInt();

        final int hat = slea.readInt();
        final int top = slea.readInt();
        final int glove = slea.readInt();
        final int shoes = slea.readInt();
        final int weapon = slea.readInt();
        final byte gender = c.getPlayer().getGender();
        JobType jobType = JobType.Adventurer;

        MapleCharacter newchar = MapleCharacter.getDefault(c, jobType);
        newchar.setJob(job);
        newchar.setWorld((byte) c.getPlayer().getWorld());
        newchar.setFace(face);
        newchar.setHair(hair);
        newchar.setGender(gender);
        newchar.setName(name);
        newchar.setSkinColor((byte) 3); //troll
        newchar.setLevel((short) 51);
        newchar.getStat().str = (short) 4;
        newchar.getStat().dex = (short) 4;
        newchar.getStat().int_ = (short) 4;
        newchar.getStat().luk = (short) 4;
        newchar.setRemainingAp((short) 254); //49*5 + 25 - 16
        newchar.setRemainingSp(job / 100 == 2 ? 128 : 122); //2 from job advancements. 120 from leveling. (mages get +6)
        newchar.getStat().maxhp += 150; //Beginner 10 levels
        newchar.getStat().maxmp += 125;
        switch (job) {
            case 110:
            case 120:
            case 130:
                newchar.getStat().maxhp += 600; //Job Advancement
                newchar.getStat().maxhp += 2000; //Levelup 40 times
                newchar.getStat().maxmp += 200;
                break;
            case 210:
            case 220:
            case 230:
                newchar.getStat().maxmp += 600;
                newchar.getStat().maxhp += 500; //Levelup 40 times
                newchar.getStat().maxmp += 2000;
                break;
            case 310:
            case 320:
            case 410:
            case 420:
            case 520:
                newchar.getStat().maxhp += 500;
                newchar.getStat().maxmp += 250;
                newchar.getStat().maxhp += 900; //Levelup 40 times
                newchar.getStat().maxmp += 600;
                break;
            case 510:
                newchar.getStat().maxhp += 500;
                newchar.getStat().maxmp += 250;
                newchar.getStat().maxhp += 450; //Levelup 20 times
                newchar.getStat().maxmp += 300;
                newchar.getStat().maxhp += 800; //Levelup 20 times
                newchar.getStat().maxmp += 400;
                break;
            default:
                return;
        }
        for (int i = 2490; i < 2507; i++) {
            newchar.setQuestAdd(MapleQuest.getInstance(i), (byte) 2, null);
        }
        newchar.setQuestAdd(MapleQuest.getInstance(29947), (byte) 2, null);
        newchar.setQuestAdd(MapleQuest.getInstance(GameConstants.ULT_EXPLORER), (byte) 0, c.getPlayer().getName());
        newchar.changeSkillLevel_Skip(SkillFactory.getSkill(1074 + (job / 100)), (byte) 5, (byte) 5);
        newchar.changeSkillLevel_Skip(SkillFactory.getSkill(80), (byte) 1, (byte) 1);
        final MapleItemInformationProvider li = MapleItemInformationProvider.getInstance();
        int[] items = new int[]{1142257, hat, top, shoes, glove, weapon, hat + 1, top + 1, shoes + 1, glove + 1, weapon + 1}; //brilliant = fine+1
        for (byte i = 0; i < items.length; i++) {
            Item item = li.getEquipById(items[i]);
            item.setPosition((byte) (i + 1));
            newchar.getInventory(MapleInventoryType.EQUIP).addFromDB(item);
        }
        newchar.getInventory(MapleInventoryType.USE).addItem(new Item(2000004, (byte) 0, (short) 100, (byte) 0));
        newchar.getInventory(MapleInventoryType.USE).addItem(new Item(2000004, (byte) 0, (short) 100, (byte) 0));
        c.getPlayer().fakeRelog();
        if (MapleCharacterUtil.canCreateChar(name, c.isGm()) && (!LoginInformationProvider.getInstance().isForbiddenName(name) || c.isGm())) {
            MapleCharacter.saveNewCharToDB(newchar, jobType, (short) 0);
            MapleQuest.getInstance(20734).forceComplete(c.getPlayer(), 1101000);
            c.getSession().write(MaplePacketCreator.createUltimate(1));
        } else {
            c.getSession().write(MaplePacketCreator.createUltimate(0));
        }
    }

    public static final void DeleteChar(final LittleEndianAccessor slea, final MapleClient c) {
        final byte checkspw = slea.readByte();
        if (checkspw > 0) {
            final String spw = slea.readMapleAsciiString();
            slea.skip(4);
            final int Character_ID = slea.readInt();
            if (c.CheckSecondPassword(spw) && spw.length() >= 4 && spw.length() <= 16) {
                byte state = 0;
                state = (byte) c.deleteCharacter(Character_ID);
                c.getSession().write(LoginPacket.deleteCharResponse(Character_ID, state));
            } else {
                byte state = 20;
                c.getSession().write(LoginPacket.deleteCharResponse(Character_ID, state));
            }

        } else {
            slea.skip(4);
            final int Character_ID = slea.readInt();
            byte state = 0;
            state = (byte) c.deleteCharacter(Character_ID);
            c.getSession().write(LoginPacket.deleteCharResponse(Character_ID, state));
        }
    }

    public static final void Character_WithoutSecondPassword(final LittleEndianAccessor slea, final MapleClient c, final boolean haspic, final boolean view) {
        final int charId = slea.readInt();
        //final String currentpw = c.getSecondPassword();
        if (!c.isLoggedIn() || loginFailCount(c) || !c.login_Auth(charId) || ChannelServer.getInstance(c.getChannel()) == null || c.getWorld() != 0) {
            c.getSession().close();
            return;
        }
        if (c.getIdleTask() != null) {
            c.getIdleTask().cancel(true);
        }
        final String s = c.getSessionIPAddress();
        LoginServer.putLoginAuth(charId, s.substring(s.indexOf('/') + 1, s.length()), c.getTempIP());
        c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, s);
        c.getSession().write(MaplePacketCreator.getServerIP(c, Integer.parseInt(ChannelServer.getInstance(c.getChannel()).getIP().split(":")[1]), charId));
    }

    public static final void ReloginRequest(MapleClient c) {
        c.getSession().write(LoginPacket.getLoginFailed(20));
    }

    public static final void Character_WithSecondPassword(final LittleEndianAccessor slea, final MapleClient c) {
        final String password = slea.readMapleAsciiString();
        final int charId = slea.readInt();

        if (!c.isLoggedIn() || loginFailCount(c) || c.getSecondPassword() == null || !c.login_Auth(charId) || ChannelServer.getInstance(c.getChannel()) == null || c.getWorld() != 0) { // TODOO: MULTI WORLDS
            c.getSession().close();
            return;
        }
        if (c.CheckSecondPassword(password) && password.length() >= 4 && password.length() <= 16) {
            if (c.getIdleTask() != null) {
                c.getIdleTask().cancel(true);
            }
            final String s = c.getSessionIPAddress();
            LoginServer.putLoginAuth(charId, s.substring(s.indexOf('/') + 1, s.length()), c.getTempIP());
            c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, s);
            c.getSession().write(MaplePacketCreator.getServerIP(c, Integer.parseInt(ChannelServer.getInstance(c.getChannel()).getIP().split(":")[1]), charId));
        } else {
            c.getSession().write(LoginPacket.secondPwError((byte) 0x14));
        }
    }

    public static final void AuthSecondPassword(LittleEndianAccessor slea, MapleClient c) {
        byte mode = slea.readByte();
        System.out.println("모드" + mode);
        if (mode == 1) {
            //register
            slea.skip(4);
            String setpassword = slea.readMapleAsciiString();
            if (setpassword.length() >= 4 && setpassword.length() <= 16) {
                c.setSecondPassword(setpassword);
                System.out.println("비번" + setpassword);
                c.updateSecondPassword();
                c.getSession().write(LoginPacket.secondPasswordResult((byte) 1, (byte) 0x00));
            } else {
                c.getSession().write(LoginPacket.secondPasswordResult((byte) 0, (byte) 0x14));
            }
        } else {
            //deregister
            slea.skip(4);
            String pw = slea.readMapleAsciiString();
            if (!c.isLoggedIn() || loginFailCount(c) || ChannelServer.getInstance(c.getChannel()) == null || c.getWorld() != 0) { // TODOO: MULTI WORLDS
                c.getSession().close();
            } else if (!c.CheckSecondPassword(pw)) {
                c.getSession().write(LoginPacket.secondPasswordResult((byte) 1, (byte) 0x14));
            } else {
                c.setSecondPassword(null);
                c.updateSecondPasswordToNull();
                c.getSession().write(LoginPacket.secondPasswordResult((byte) 0, (byte) 0x00));
            }
        }
    }

    public static void ViewChar(LittleEndianAccessor slea, MapleClient c) {
        Map<Byte, ArrayList<MapleCharacter>> worlds = new HashMap<Byte, ArrayList<MapleCharacter>>();
        List<MapleCharacter> chars = c.loadCharacters(0); //TODO multi world
//        c.getSession().write(LoginPacket.showAllCharacter(chars.size()));
        for (MapleCharacter chr : chars) {
            if (chr != null) {
                ArrayList<MapleCharacter> chrr;
                if (!worlds.containsKey(chr.getWorld())) {
                    chrr = new ArrayList<MapleCharacter>();
                    worlds.put(chr.getWorld(), chrr);
                } else {
                    chrr = worlds.get(chr.getWorld());
                }
                chrr.add(chr);
            }
        }
        for (Entry<Byte, ArrayList<MapleCharacter>> w : worlds.entrySet()) {
//            c.getSession().write(LoginPacket.showAllCharacterInfo(w.getKey(), w.getValue(), c.getSecondPassword()));
        }
    }
    
    public static void IpBan(String ip) {
        Connection con = null;
        Connection con2 = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
            ps.setString(1, ip);
            ps.execute();
            ps.close();
            return;
        } catch (SQLException ex) {
            Logger.getLogger(CharLoginHandler.class.getName()).log(Level.SEVERE, null, ex);
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
}
