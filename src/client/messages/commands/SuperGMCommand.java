package client.messages.commands;
import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.MapleQuestStatus;
import client.MapleStat;
import client.Skill;
import client.SkillEntry;
import client.SkillFactory;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.ItemFlag;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.messages.CommandProcessorUtil;
import constants.GameConstants;
import constants.ServerConstants;
import constants.ServerConstants.PlayerGMRank;
import server.events.OnTimeGiver;
import database.DatabaseConnection;
import handling.RecvPacketOpcode;
import handling.SendPacketOpcode;
import handling.channel.ChannelServer;
import handling.channel.handler.DueyHandler;
import handling.world.World;
import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import scripting.EventManager;
import scripting.PortalScriptManager;
import scripting.ReactorScriptManager;
import server.CashItemInfo;
import server.MapleAdminShopFactory;
import server.MapleCarnivalChallenge;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MaplePortal;
import server.MapleShopFactory;
import server.Start;
import server.events.MapleEvent;
import server.events.MapleEventType;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleMonsterInformationProvider;
import server.life.MapleNPC;
import server.life.OverrideMonsterStats;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleReactor;
import server.quest.MapleQuest;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.StringUtil;
import tools.data.MaplePacketLittleEndianWriter;
import tools.packet.MobPacket;

/**
 *
 * @author Emilyx3
 */
public class SuperGMCommand {

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.SUPERGM;
    }
    
    public static class 동접주작 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            String arg = splitted[1];
            if (arg == null) {
                c.getPlayer().dropMessage(5, "설정 할 동접 수를 입력하세요.");
                return 0;
            }
            try {
                int increaseConnectionUsers = Integer.parseInt(arg);
                int before = Start.increaseConnectionUsers;
                Start.increaseConnectionUsers = increaseConnectionUsers;
                c.getPlayer().dropMessage(5, "동접 주작 값이 " + before + " 에서 " + Start.increaseConnectionUsers + " 으로 변경 되었습니다.");
            } catch (NumberFormatException e) {
                c.getPlayer().dropMessage(5, "숫자만 입력하세요. 에러 : " + e);
                return 0;
            }
            return 1;
        }
    }

    public static class 소환 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                if ((!c.getPlayer().isGM() && (victim.isInBlockedMap() || victim.isGM()))) {
                    c.getPlayer().dropMessage(5, "Try again later.");
                    return 0;
                }
                victim.changeMap(c.getPlayer().getMap(), c.getPlayer().getMap().findClosestPortal(c.getPlayer().getTruePosition()));
            } else {
                int ch = World.Find.findChannel(splitted[1]);
                if (ch < 0) {
                    c.getPlayer().dropMessage(5, "Not found.");
                    return 0;
                }
                victim = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim == null || (!c.getPlayer().isGM() && (victim.isInBlockedMap() || victim.isGM()))) {
                    c.getPlayer().dropMessage(5, "Try again later.");
                    return 0;
                }
                c.getPlayer().dropMessage(5, "대상이 채널 이동 중입니다.");
                victim.dropMessage(5, "채널 이동 중입니다.");
                if (victim.getMapId() != c.getPlayer().getMapId()) {
                    final MapleMap mapp = victim.getClient().getChannelServer().getMapFactory().getMap(c.getPlayer().getMapId());
                    victim.changeMap(mapp, mapp.findClosestPortal(c.getPlayer().getTruePosition()));
                }
                victim.changeChannel(c.getChannel());
            }
            return 1;
        }
    }

    public static class 드롭 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final int itemId = Integer.parseInt(splitted[1]);
            final short quantity = (short) CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1);
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (GameConstants.isPet(itemId)) {
                c.getPlayer().dropMessage(5, "현재 코드는 캐시샵에서 구매하시기 바랍니다.");
            } else if (!ii.itemExists(itemId)) {
                c.getPlayer().dropMessage(5, itemId + " 존재하지 않는 아이템 코드입니다.");
            } else {
                Item toDrop;
                if (GameConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {

                    toDrop = ii.randomizeStats((Equip) ii.getEquipById(itemId));
                } else {
                    toDrop = new client.inventory.Item(itemId, (byte) 0, (short) quantity, (byte) 0);
                }
                if (!c.getPlayer().isAdmin()) {
                    toDrop.setGMLog(c.getPlayer().getName() + " used !드롭");
                    toDrop.setOwner(c.getPlayer().getName());
                }
                c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), toDrop, c.getPlayer().getPosition(), true, true);
            }
            return 1;
        }
    }
    
 public static class 오프라인캐시 extends CommandExecute {
        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "유저명과 포인트를 입력해주세요.");
                return 0;
            }
            if (World.Find.findChannel(splitted[1]) >= 0 && World.Find.findChannel(splitted[1]) <= 20) {
                MapleCharacter chrs = ChannelServer.getInstance(World.Find.findChannel(splitted[1])).getPlayerStorage().getCharacterByName(splitted[1]);
                c.getPlayer().modifyCSPoints(1, Integer.parseInt(splitted[2]), false);
                c.getPlayer().dropMessage(6, "캐시 지급완료");
            } else {    
                PreparedStatement ps1 = null;
                PreparedStatement ps2 = null;
                PreparedStatement ps3 = null;
                Connection con1 = null;
                Connection con2 = null;
                Connection con3 = null;
                ResultSet rs1 = null;
                ResultSet rs2 = null;
                try {
                    con1 = DatabaseConnection.getConnection();
                    con2 = DatabaseConnection.getConnection();
                    con3 = DatabaseConnection.getConnection();
                    ps1 = con1.prepareStatement("SELECT `accountid` FROM `characters` WHERE `name` = ?");
                    ps1.setString(1, splitted[1]);
                    rs1 = ps1.executeQuery();
                    rs1.next();
                    ps2 = con2.prepareStatement("SELECT `vpoints` FROM `accounts` WHERE `id` = ?");
                    ps2.setInt(1, rs1.getInt(1));
                    rs2 = ps2.executeQuery();
                    rs2.next();          
                    ps3 = con3.prepareStatement("UPDATE `accounts` set `Acash` = ? WHERE `id` = ?");
                    ps3.setInt(1, Integer.parseInt(splitted[2]) + rs2.getInt(1));
                    ps3.setInt(2, rs1.getInt(1));
                    ps3.executeUpdate();
                    c.getPlayer().dropMessage(6, "캐시 오프라인 지급 완료.");
                } catch (Exception e) {
                    c.getPlayer().dropMessage(6, "지급오류가 발생하였습니다.");
                } finally {
                    if (ps3 != null) {
                        try {
                            ps3.close();
                        } catch (Exception e) {
                        }
                    }
                    if (con3 != null) {
                        try {
                            con3.close();
                        } catch (Exception e) {
                        }
                    }
                    if (rs2 != null) {
                        try {
                            rs2.close();
                        } catch (Exception e) {
                        }
                    }
                    if (ps2 != null) {
                        try {
                            ps2.close();
                        } catch (Exception e) {
                        }
                    }
                    if (con2 != null) {
                        try {
                            con2.close();
                        } catch (Exception e) {
                        }
                    }
                    if (rs1 != null) {
                        try {
                            rs1.close();
                        } catch (Exception e) {
                        }
                    }
                    if (ps1 != null) {
                        try {
                            ps1.close();
                        } catch (Exception e) {
                        }
                    }
                    if (con1 != null) {
                        try {
                            con1.close();
                        } catch (Exception e) {
                        }
                    }
                }
            }
            return 1;
        }
    } 
 
    public static class 모두팅 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int range = -1;
            if (splitted[1].equals("m")) {
                range = 0;
            } else if (splitted[1].equals("c")) {
                range = 1;
            } else if (splitted[1].equals("w")) {
                range = 2;
            }
            if (range == -1) {
                range = 1;
            }
            if (range == 0) {
                c.getPlayer().getMap().disconnectAll();
            } else if (range == 1) {
                c.getChannelServer().getPlayerStorage().disconnectAll(true);
            } else if (range == 2) {
                for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                    cserv.getPlayerStorage().disconnectAll(true);
                }
            }
            return 1;
        }
    }   

    public static class dcAll extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (ChannelServer cs : ChannelServer.getAllInstances()) {
                for (MapleCharacter player : cs.getPlayerStorage().getAllCharacters()) {
                    if (player != null && player.getClient() != null) {
                        player.getClient().sclose();
                    }
                }
            }
            c.getPlayer().dropMessage(6, "All players have been disconnected.");
            return 1;
        }
    }

    public static class 유저죽이기 extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "Syntax: !kill <list player names>");
                return 0;
            }
            MapleCharacter victim = null;
            for (int i = 1; i < splitted.length; i++) {
                try {
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[i]);
                } catch (Exception e) {
                    c.getPlayer().dropMessage(6, "Player " + splitted[i] + " not found.");
                }
                if (player.allowedToTarget(victim) && player.getGMLevel() >= victim.getGMLevel()) {
                    victim.getStat().setHp((int) 0, victim);
                    victim.getStat().setMp((int) 0, victim);
                    victim.updateSingleStat(MapleStat.HP, 0);
                    victim.updateSingleStat(MapleStat.MP, 0);
                }
            }
            return 1;
        }
    } 
    
    public static class 킬올 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            double range = Double.POSITIVE_INFINITY;

            if (splitted.length > 1) {
                int irange = Integer.parseInt(splitted[1]);
                if (splitted.length <= 2) {
                    range = irange * irange;
                } else {
                    map = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[2]));
                }
            }
            if (map == null) {
                c.getPlayer().dropMessage(6, "존재하지 않는 맵입니다.");
                return 0;
            }
            MapleMonster mob;
            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                if (!mob.getStats().isBoss() || mob.getStats().isPartyBonus() || c.getPlayer().isGM()) {
                    map.killMonster(mob, c.getPlayer(), false, false, (byte) 1);
                }
            }
            return 1;
        }
    }    
    
    public static class 소지한아이템삭제 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            java.util.Map<Pair<Short, Short>, MapleInventoryType> eqs = new HashMap<Pair<Short, Short>, MapleInventoryType>();
            if (splitted[1].equals("모두")) {
                for (MapleInventoryType type : MapleInventoryType.values()) {
                    for (Item item : c.getPlayer().getInventory(type)) {
                        eqs.put(new Pair<Short, Short>(item.getPosition(), item.getQuantity()), type);
                    }
                }
            } else if (splitted[1].equals("장착")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.EQUIPPED)) {
                    eqs.put(new Pair<Short, Short>(item.getPosition(), item.getQuantity()), MapleInventoryType.EQUIPPED);
                }
            } else if (splitted[1].equals("장비")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.EQUIP)) {
                    eqs.put(new Pair<Short, Short>(item.getPosition(), item.getQuantity()), MapleInventoryType.EQUIP);
                }
            } else if (splitted[1].equals("소비")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.USE)) {
                    eqs.put(new Pair<Short, Short>(item.getPosition(), item.getQuantity()), MapleInventoryType.USE);
                }
            } else if (splitted[1].equals("설치")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.SETUP)) {
                    eqs.put(new Pair<Short, Short>(item.getPosition(), item.getQuantity()), MapleInventoryType.SETUP);
                }
            } else if (splitted[1].equals("기타")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.ETC)) {
                    eqs.put(new Pair<Short, Short>(item.getPosition(), item.getQuantity()), MapleInventoryType.ETC);
                }
            } else if (splitted[1].equals("캐시")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.CASH)) {
                    eqs.put(new Pair<Short, Short>(item.getPosition(), item.getQuantity()), MapleInventoryType.CASH);
                }
            } else {
                c.getPlayer().dropMessage(6, "[all/eqp/eq/u/s/e/c]");
            }
            for (Map.Entry<Pair<Short, Short>, MapleInventoryType> eq : eqs.entrySet()) {
                MapleInventoryManipulator.removeFromSlot(c, eq.getValue(), eq.getKey().left, eq.getKey().right, false, false);
            }
            return 1;
        }
    }        

    public static class 좌표 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(5, "맵 : " + c.getPlayer().getMap().getId());
            return 1;
        }
    }

    public static class 후원2222 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "존재하지 않는 유저입니다.");
                return 0;
            }
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chrs == null) {
                c.getPlayer().dropMessage(6, "유저가 채널에 접속해 있는지 확인해주세요.");
            } else {
                chrs.setVPoints(chrs.getVPoints() + Integer.parseInt(splitted[2]));
                c.getPlayer().dropMessage(6, splitted[1] + " 님께 후원 " + chrs.getVPoints() + " 를 지급하였습니다.");
            }
            return 1;
        }
    }

    public static class 킬올경험치 extends SuperGMCommand.KillAllExp {
    }

    public static class KillAllExp extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            double range = Double.POSITIVE_INFINITY;

            if (splitted.length > 1) {
                //&& !splitted[0].equals("!killmonster") && !splitted[0].equals("!hitmonster") && !splitted[0].equals("!hitmonsterbyoid") && !splitted[0].equals("!killmonsterbyoid")) {
                int irange = Integer.parseInt(splitted[1]);
                if (splitted.length <= 2) {
                    range = irange * irange;
                } else {
                    map = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[2]));
                }
            }
            if (map == null) {
                c.getPlayer().dropMessage(6, "존재하지 않는 맵입니다.");
                return 0;
            }
            MapleMonster mob;
            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                mob.damage(c.getPlayer(), mob.getHp(), false);
            }
            return 1;
        }
    }    
    
    public static class 킬올드롭 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            double range = Double.POSITIVE_INFINITY;

            if (splitted.length > 1) {
                //&& !splitted[0].equals("!killmonster") && !splitted[0].equals("!hitmonster") && !splitted[0].equals("!hitmonsterbyoid") && !splitted[0].equals("!killmonsterbyoid")) {
                int irange = Integer.parseInt(splitted[1]);
                if (splitted.length <= 2) {
                    range = irange * irange;
                } else {
                    map = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[2]));
                }
            }
            if (map == null) {
                c.getPlayer().dropMessage(6, "존재하지 않는 맵입니다.");
                return 0;
            }
            MapleMonster mob;
            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                map.killMonster(mob, c.getPlayer(), true, false, (byte) 1);
            }
            return 1;
        }
    }

    public static class 엔피시 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int npcId = Integer.parseInt(splitted[1]);
            MapleNPC npc = MapleLifeFactory.getNPC(npcId);
            if (npc != null && !npc.getName().equals("MISSINGNO")) {
                npc.setPosition(c.getPlayer().getPosition());
                npc.setCy(c.getPlayer().getPosition().y);
                npc.setRx0(c.getPlayer().getPosition().x + 50);
                npc.setRx1(c.getPlayer().getPosition().x - 50);
                npc.setFh(c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId());
                npc.setCustom(true);
                c.getPlayer().getMap().addMapObject(npc);
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.spawnNPC(npc, true));
            } else {
                c.getPlayer().dropMessage(6, "존재하지않는 엔피시 코드 입니다.");
                return 0;
            }
            return 1;
        }
    }

     public static class 고정엔피시 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int npcId = Integer.parseInt(splitted[1]);
            MapleNPC npc = MapleLifeFactory.getNPC(npcId);
            if (npc != null && !npc.getName().equals("MISSINGNO")) {
                npc.setPosition(c.getPlayer().getPosition());
                npc.setCy(c.getPlayer().getPosition().y);
                npc.setRx0(c.getPlayer().getPosition().x + 50);
                npc.setRx1(c.getPlayer().getPosition().x - 50);
                npc.setFh(c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId());
                c.getPlayer().getMap().addMapObject(npc);
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.spawnNPC(npc, true));
            } else {
                c.getPlayer().dropMessage(6, "WZ에 존재하지 않는 NPC를 입력했습니다.");
            }
            String sql = "INSERT INTO `spawn`(`lifeid`, `rx0`, `rx1`, `cy`, `fh`, `type`, `dir`, `mapid`, `mobTime`) VALUES (? ,? ,? ,? ,? ,? ,? ,? ,?)";
            PreparedStatement ps = null;
            Connection con = null;
            try {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement(sql);
                ps.setInt(1, npcId);
                ps.setInt(2, c.getPlayer().getPosition().x - 50);
                ps.setInt(3, c.getPlayer().getPosition().x + 50);
                ps.setInt(4, c.getPlayer().getPosition().y);
                ps.setInt(5, c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId());
                ps.setString(6, "n");
                ps.setInt(7, c.getPlayer().getFacingDirection() == 1 ? 0 : 1);
                ps.setInt(8, c.getPlayer().getMapId());
                ps.setInt(9, 0);
                ps.executeUpdate();
                ps.close();
            } catch (Exception e) {
                System.err.println("[오류] 엔피시를 고정 등록하는데 실패했습니다.");
                if (!ServerConstants.realese) {
                    e.printStackTrace();
                }
            } finally {
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException ex) {
                        Logger.getLogger(PlayerCommand.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                if (ps != null) {
                    try {
                        ps.close();
                    } catch (SQLException ex) {
                        Logger.getLogger(PlayerCommand.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            return 0;
        }
    }

    public static class 엔피시위치 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                if (splitted.length < 2) {
                    c.getPlayer().dropMessage(5, "사용법: !엔피시위치 <엔피시코드>");
                    return 0;
                }

                String npcCode = splitted[1];
                String wzPath = getWzPath();
//                System.out.println("WZ Path: " + wzPath);

                if (wzPath == null) {
                    c.getPlayer().dropMessage(5, "WZ 경로를 찾을 수 없습니다.");
                    return 0;
                }

                List<String> mapsWithNpc = findMapsWithNpc(wzPath, npcCode);

                if (mapsWithNpc.isEmpty()) {
                    c.getPlayer().dropMessage(5, "해당 NPC가 존재하지 않습니다.");
                } else {
                    for (String mapInfo : mapsWithNpc) {
                        c.getPlayer().dropMessage(5, mapInfo);
                    }
                }
            } catch (Exception e) {
                c.getPlayer().dropMessage(5, "명령어 처리 중 오류가 발생했습니다: " + e.getMessage());
                e.printStackTrace();
            }
            return 1;
        }
    }

    private static String getWzPath() {
        try {
            String jarPath = new File(SuperGMCommand.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent();
            String decodedPath = URLDecoder.decode(jarPath, StandardCharsets.UTF_8.name());
            String wzPath = new File(decodedPath, "../wz/Map.wz/Map").getCanonicalPath();
//            System.out.println("Decoded WZ Path: " + wzPath);
            return wzPath;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static List<String> findMapsWithNpc(String wzPath, String npcCode) {
        List<String> mapsWithNpc = new ArrayList<>();
        String[] mapFolders = {"Map0", "Map1", "Map2", "Map3", "Map4", "Map5", "Map6", "Map7", "Map8", "Map9"};

        for (String mapFolder : mapFolders) {
            File folder = new File(wzPath + "/" + mapFolder);

            if (!folder.exists() || !folder.isDirectory()) {
                System.out.println("유효하지 않은 WZ 경로입니다: " + folder.getPath());
                continue;
            }

            for (File file : folder.listFiles((dir, name) -> name.endsWith(".img.xml"))) {
//                System.out.println("Checking file: " + file.getName());
                String mapInfo = parseXmlFileForNpc(file, npcCode);
                if (mapInfo != null) {
                    mapsWithNpc.add(mapInfo);
                }
            }
        }
        return mapsWithNpc;
    }

    private static String parseXmlFileForNpc(File xmlFile, String npcCode) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(xmlFile);
            document.getDocumentElement().normalize();

            NodeList imgdirNodes = document.getElementsByTagName("imgdir");

            for (int i = 0; i < imgdirNodes.getLength(); i++) {
                Node imgdirNode = imgdirNodes.item(i);
                if (imgdirNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element imgdirElement = (Element) imgdirNode;
                    if ("life".equals(imgdirElement.getAttribute("name"))) {
                        NodeList stringNodes = imgdirElement.getElementsByTagName("string");

                        for (int j = 0; j < stringNodes.getLength(); j++) {
                            Element stringElement = (Element) stringNodes.item(j);
                            if ("id".equals(stringElement.getAttribute("name"))) {
                                String idValue = stringElement.getAttribute("value");
                                if (npcCode.equals(idValue.trim())) {
//                                    System.out.println("NPC Found in file: " + xmlFile.getName());
                                    String mapCode = xmlFile.getName().replace(".img.xml", "");
                                    String mapInfo = getMapInfo(mapCode);
                                    return mapInfo;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getMapInfo(String mapCode) {
        MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/String.wz"));
        MapleData mapData = dataProvider.getData("Map.img");

        for (MapleData mapAreaData : mapData.getChildren()) {
            for (MapleData mapIdData : mapAreaData.getChildren()) {
                if (mapIdData.getName().equals(mapCode)) {
                    String streetName = MapleDataTool.getString(mapIdData.getChildByPath("streetName"), "NO-NAME");
                    String mapName = MapleDataTool.getString(mapIdData.getChildByPath("mapName"), "NO-NAME");
                    return mapCode + " - " + streetName + " - " + mapName;
                }
            }
        }
        return mapCode + " - 알 수 없는 지역";
    }

    public static class 서버메세지 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            String outputMessage = StringUtil.joinStringFrom(splitted, 1);
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                cserv.setServerMessage(outputMessage);
            }
            return 1;
        }
    }

    public static class 스폰 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final int mid = Integer.parseInt(splitted[1]);
            final int num = Math.min(CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1), 500);
            Integer level = CommandProcessorUtil.getNamedIntArg(splitted, 1, "lvl");
            Long hp = CommandProcessorUtil.getNamedLongArg(splitted, 1, "hp");
            Integer exp = CommandProcessorUtil.getNamedIntArg(splitted, 1, "exp");
            Double php = CommandProcessorUtil.getNamedDoubleArg(splitted, 1, "php");
            Double pexp = CommandProcessorUtil.getNamedDoubleArg(splitted, 1, "pexp");

            MapleMonster onemob;
            try {
                onemob = MapleLifeFactory.getMonster(mid);
            } catch (RuntimeException e) {
                c.getPlayer().dropMessage(5, "에러 : " + e.getMessage());
                return 0;
            }
            if (onemob == null) {
                c.getPlayer().dropMessage(5, "존재하지 않는 몬스터 코드 입니다.");
                return 0;
            }
            long newhp = 0;
            int newexp = 0;
            if (hp != null) {
                newhp = hp.longValue();
            } else if (php != null) {
                newhp = (long) (onemob.getMobMaxHp() * (php.doubleValue() / 100));
            } else {
                newhp = onemob.getMobMaxHp();
            }
            if (exp != null) {
                newexp = exp.intValue();
            } else if (pexp != null) {
                newexp = (int) (onemob.getMobExp() * (pexp.doubleValue() / 100));
            } else {
                newexp = onemob.getMobExp();
            }
            if (newhp < 1) {
                newhp = 1;
            }

            final OverrideMonsterStats overrideStats = new OverrideMonsterStats(newhp, onemob.getMobMaxMp(), newexp, false);
            for (int i = 0; i < num; i++) {
                MapleMonster mob = MapleLifeFactory.getMonster(mid);
                mob.setHp(newhp);
                if (level != null) {
                    mob.changeLevel(level.intValue(), false);
                } else {
                    mob.setOverrideStats(overrideStats);
                }
                c.getPlayer().getMap().spawnMonsterOnGroundBelow(mob, c.getPlayer().getPosition());
            }
            return 1;
        }
    }

    public static class 메소 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().gainMeso(Integer.parseInt(splitted[1]), true);
            return 1;
        }
    }
    
    public abstract static class HideMobInternal extends CommandExecute {

        protected MapleCharacter chr = null;
        
        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            for (MapleMonster mob : map.getAllMonstersThreadsafe()) {
                c.sendPacket(MobPacket.killMonster(mob.getObjectId(), 1));
            }
            return 1;
        }
    }
    
    public static class 리신 extends HideMobInternal {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            chr = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chr == null) {
                c.getPlayer().dropMessage(5, "Cannot found player in this channel.");
                return 1;
            }
            MapleQuestStatus qs = chr.getQuestNAdd(MapleQuest.getInstance(170983));
            if (qs.getCustomData() == null) {
                qs.setCustomData("0");
            }

            if (qs.getCustomData().equals("0")) {
                c.getPlayer().dropMessage(5, "대상을 리신으로 적용.");
                qs.setCustomData("1");
            } else {
                c.getPlayer().dropMessage(5, "대상을 리신에서 적용해제.");
                qs.setCustomData("0");
            }
            return super.execute(c, splitted);
        }
    }

    public static class 캐시 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) { //비리가 가능한 캐시지급!!!!!!!!!!!!! 지우면 3대가 고자
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "기본 문법: !캐시 <숫자>");
                return 0;
            }
            if (splitted.length == 3) {
                int va = 0;
                try {
                    va = Integer.parseInt(splitted[2]);
                } catch (Exception e) {
                    c.getPlayer().dropMessage(5, "기본 문법: !캐시 <캐릭터> <숫자>");
                    return 0;
                }
                MapleCharacter player = null;
                for (int i = 1; i <= ChannelServer.getChannelCount(); i++) {
                    for (MapleCharacter other : ChannelServer.getInstance(i).getPlayerStorage().getAllCharacters()) {
                        if (other != null && other.getName().equals(splitted[1])) {
                            player = other;
                        }
                    }
                }
                if (player != null) {
                    player.modifyCSPoints(1, va, false);
                    if (player.getId() != c.getPlayer().getId()) {
                        player.dropMessage(5, "" + c.getPlayer().getBanJum((long) va) + " 캐시를 지급받았습니다.");
                    }
                    c.getPlayer().dropMessage(5, splitted[1] + " 님에게 " + c.getPlayer().getBanJum((long) va) + " 캐시를 지급하였습니다.");
                    c.getPlayer().dropMessage(5, splitted[1] + " 님의 캐시가 " + c.getPlayer().getBanJum((long) player.getCSPoints(1)) + " 캐시가 되었습니다.");
                    return 0;
                } else {
                    c.getPlayer().dropMessage(5, splitted[1] + " 님은 접속 중이지 않습니다.");
                    return 0;
                }
            } else {
                int va = 0;
                try {
                    va = Integer.parseInt(splitted[1]);
                } catch (Exception e) {
                    c.getPlayer().dropMessage(5, "기본 문법: !캐시 <숫자>");
                    return 0;
                }
                c.getPlayer().modifyCSPoints(1, Integer.parseInt(splitted[1]), false);
                c.getPlayer().dropMessage(5, c.getPlayer().getBanJum((long) va) + " 캐시를 획득하였습니다. (잔량 : " + c.getPlayer().getCSPoints(1) + ")");
            }
            return 1;
        }
    }

    public static class 메이플포인트 extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "획득량이 정의되지 않았습니다.");
                return 0;
            }
            c.getPlayer().modifyCSPoints(2, Integer.parseInt(splitted[1]), true);
            return 1;
        }
    }

    public static class 본섭캐시 extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "획득량이 정의되지 않았습니다.");
                return 0;
            }
            c.getPlayer().modifyCSPoints(3, Integer.parseInt(splitted[1]), true);
            return 1;
        }
    }   
     

    public static class 드롭리셋 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMonsterInformationProvider.getInstance().clearDrops();
            ReactorScriptManager.getInstance().clearDrops();
            return 1;
        }
    }

    public static class 포탈리셋 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            PortalScriptManager.getInstance().clearScripts();
            return 1;
        }
    }

    public static class 상점리셋 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleShopFactory.getInstance().clear();
            return 1;
        }
    }

    public static class 어드민상점리셋 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleAdminShopFactory.getInstance().clear();
            return 1;
        }
    }
    
    public static class 리액터리셋 extends SuperGMCommand.리엑터리셋 {
    }

    public static class 리엑터리셋 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().resetReactors();
            c.getPlayer().dropMessage(6, "[알림] 리엑터리셋이 완료되었습니다.");
            return 1;
        }
    }
    
    public static class 퀘스트리셋 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleLifeFactory.loadQuestCounts();
            MapleQuest.initQuests();
            c.getPlayer().dropMessage(6, "완료되었습니다.");
            return 1;
        }
    }    

    public static class 쪽지 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {

            if (splitted.length >= 1) {
                String text = StringUtil.joinStringFrom(splitted, 1);
                for (MapleCharacter mch : c.getChannelServer().getPlayerStorage().getAllCharacters()) {
                    c.getPlayer().sendNote(mch.getName(), text);
                }
            } else {
                c.getPlayer().dropMessage(6, "Use it like this, !sendallnote <text>");
                return 0;
            }
            return 1;
        }
    }
    
    public static class 퀘스트시작 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).start(c.getPlayer(), Integer.parseInt(splitted[2]));
            return 1;
        }
    }

    public static class 퀘스트완료 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).complete(c.getPlayer(), Integer.parseInt(splitted[2]), Integer.parseInt(splitted[3]));
            return 1;
        }
    }

    public static class F퀘스트시작 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).forceStart(c.getPlayer(), Integer.parseInt(splitted[2]), splitted.length >= 4 ? splitted[3] : null);
            return 1;
        }
    }

    public static class F퀘스트완료 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).forceComplete(c.getPlayer(), Integer.parseInt(splitted[2]));
            return 1;
        }
    }

    public static class killQuestMob extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuestStatus q = c.getPlayer().getQuestNoAdd(MapleQuest.getInstance(Integer.parseInt(splitted[1])));
            if (q == null) {
                c.getPlayer().dropMessage(6, "NULL QUEST");
                return 1;
            }
            for (int i = 0; i < Integer.parseInt(splitted[3]); ++i) {
                c.getPlayer().mobKilled(i, i);
                q.mobKilled(Integer.parseInt(splitted[2]), splitted.length >= 5 ? Integer.parseInt(splitted[4]) : 0);
            }
            c.getSession().write(MaplePacketCreator.updateQuestMobKills(q));
            if (q.getQuest().canComplete(c.getPlayer(), null)) {
                c.getSession().write(MaplePacketCreator.getShowQuestCompletion(q.getQuest().getId()));
            }

            return 1;
        }
    }

    public static class killMobCount extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (int i = 0; i < Integer.parseInt(splitted[2]); ++i) {
                c.getPlayer().mobKilled(Integer.parseInt(splitted[1]), splitted.length >= 4 ? Integer.parseInt(splitted[3]) : 0);
            }
            return 1;
        }
    }    
    

    public static class 팅 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[splitted.length - 1]);
            if (victim != null && c.getPlayer().getGMLevel() >= victim.getGMLevel()) {
                victim.getClient().sclose();
                return 1;
            } else {
                c.getPlayer().dropMessage(6, "The victim does not exist.");
                return 0;
            }
        }
    }    

    public static class 몹디버그 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            double range = Double.POSITIVE_INFINITY;

            if (splitted.length > 1) {
                //&& !splitted[0].equals("!killmonster") && !splitted[0].equals("!hitmonster") && !splitted[0].equals("!hitmonsterbyoid") && !splitted[0].equals("!killmonsterbyoid")) {
                int irange = Integer.parseInt(splitted[1]);
                if (splitted.length <= 2) {
                    range = irange * irange;
                } else {
                    map = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[2]));
                }
            }
            if (map == null) {
                c.getPlayer().dropMessage(6, "Map does not exist");
                return 0;
            }
            MapleMonster mob;
            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                c.getPlayer().dropMessage(6, "Monster " + mob.toString());
            }
            return 1;
        }
    }

    public static class 근처엔피시 extends SuperGMCommand.LookNPC {
    }
    
    public static class 엔피시보기 extends SuperGMCommand.LookNPC {
    }

    public static class LookNPC extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (MapleMapObject reactor1l : c.getPlayer().getMap().getAllNPCsThreadsafe()) {
                MapleNPC reactor2l = (MapleNPC) reactor1l;
                c.getPlayer().dropMessage(5, "NPC: oID: " + reactor2l.getObjectId() + " npcID: " + reactor2l.getId() + " Position: " + reactor2l.getPosition().toString() + " Name: " + reactor2l.getName());
            }
            return 0;
        }
    }
  
    public static class 근처리엑터 extends SuperGMCommand.LookReactor {
    }      
    public static class 근처리액터 extends SuperGMCommand.LookReactor {
    }    
    public static class 리엑터보기 extends SuperGMCommand.LookReactor {
    }
    public static class 리액터보기 extends SuperGMCommand.LookReactor {
    }

    public static class LookReactor extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (MapleMapObject reactor1l : c.getPlayer().getMap().getAllReactorsThreadsafe()) {
                MapleReactor reactor2l = (MapleReactor) reactor1l;
                c.getPlayer().dropMessage(5, "Reactor: oID: " + reactor2l.getObjectId() + " reactorID: " + reactor2l.getReactorId() + " Position: " + reactor2l.getPosition().toString() + " State: " + reactor2l.getState() + " Name: " + reactor2l.getName());
            }
            return 0;
        }
    }

    public static class 근처포탈 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MaplePortal portal = c.getPlayer().getMap().findClosestPortal(c.getPlayer().getTruePosition());
            c.getPlayer().dropMessage(6, portal.getName() + " id: " + portal.getId() + " script: " + portal.getScriptName());

            return 1;
        }
    }

    public static class 모든포탈 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (MaplePortal portal : c.getPlayer().getMap().getPortals()) {
                c.getPlayer().dropMessage(5, "Portal: ID: " + portal.getId() + " script: " + portal.getScriptName() + " name: " + portal.getName() + " pos: " + portal.getPosition().x + "," + portal.getPosition().y + " target: " + portal.getTargetMapId() + " / " + portal.getTarget());
            }
            return 0;
        }
    }

    public static class 스텟포인트 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().setRemainingAp((short) CommandProcessorUtil.getOptionalIntArg(splitted, 1, 1));
            c.getPlayer().updateSingleStat(MapleStat.AVAILABLEAP, c.getPlayer().getRemainingAp());
            return 1;
        }
    }
    
    public static class 이벤트스폰 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final int mid = Integer.parseInt(splitted[1]);
            final int num = CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1);
            Integer level = CommandProcessorUtil.getNamedIntArg(splitted, 1, "lvl");
            Long hp = CommandProcessorUtil.getNamedLongArg(splitted, 1, "hp");
            Integer exp = CommandProcessorUtil.getNamedIntArg(splitted, 1, "exp");
            Double php = CommandProcessorUtil.getNamedDoubleArg(splitted, 1, "php");
            Double pexp = CommandProcessorUtil.getNamedDoubleArg(splitted, 1, "pexp");

            int flag = 0;
            for (String str : splitted) {
                if (str.equalsIgnoreCase("핑쿠")) {
                    flag |= 0x1;
                }
                if (str.equalsIgnoreCase("블쿠")) {
                    flag |= 0x2;
                }
                if (str.equalsIgnoreCase("사랑의의자")) {
                    flag |= 0x4;
                }
                if (str.equalsIgnoreCase("릴렉스체어")) {
                    flag |= 0x8;
                }
                if (str.equalsIgnoreCase("주황버섯")) {
                    flag |= 0x10;
                }
                if (str.equalsIgnoreCase("리본돼지")) {
                    flag |= 0x20;
                }
                if (str.equalsIgnoreCase("그레이")) {
                    flag |= 0x40;
                }
                if (str.equalsIgnoreCase("칠판")) {
                    flag |= 0x80;
                }
            }

            MapleMonster onemob;
            try {
                onemob = MapleLifeFactory.getMonster(mid);
            } catch (RuntimeException e) {
                c.getPlayer().dropMessage(5, "Error: " + e.getMessage());
                return 0;
            }
            if (onemob == null) {
                c.getPlayer().dropMessage(5, "Mob does not exist");
                return 0;
            }
            long newhp = 0;
            int newexp = 0;
            if (hp != null) {
                newhp = hp.longValue();
            } else if (php != null) {
                newhp = (long) (onemob.getMobMaxHp() * (php.doubleValue() / 100));
            } else {
                newhp = onemob.getMobMaxHp();
            }
            if (exp != null) {
                newexp = exp.intValue();
            } else if (pexp != null) {
                newexp = (int) (onemob.getMobExp() * (pexp.doubleValue() / 100));
            } else {
                newexp = onemob.getMobExp();
            }
            if (newhp < 1) {
                newhp = 1;
            }

            final OverrideMonsterStats overrideStats = new OverrideMonsterStats(newhp, onemob.getMobMaxMp(), newexp, false);
            MapleMonster mob = MapleLifeFactory.getMonster(mid);
            mob.setEventDropFlag(flag);
            mob.setOverrideStats(overrideStats);
            mob.setHp(num);
            c.getPlayer().getMap().spawnMonsterOnGroundBelow(mob, c.getPlayer().getPosition());
            return 1;
        }
    }    
    
    public static class 지급 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 4) {
                c.getPlayer().dropMessage(5, "사용법: !아이템지급 <캐릭터이름, 아이템코드, 수량, 메시지>");
                return 1;
            }
            int cid = MapleCharacterUtil.getIdByName(splitted[1]);
            final int item = Integer.parseInt(splitted[2]);
            final int q = Integer.parseInt(splitted[3]);
            int channel = World.Find.findChannel(cid);
            if (channel >= 0) {
                World.Broadcast.sendPacket(cid, MaplePacketCreator.sendDuey((byte) 28, null, null));
                World.Broadcast.sendPacket(cid, MaplePacketCreator.serverNotice(5, "아이템이 지급되었습니다. NPC 택배원 <듀이> 에게서 아이템을 수령하세요!"));
            }
            DueyHandler.addNewItemToDb(item, q, cid, c.getPlayer().getName(), splitted[4], channel >= 0);
            return 1;
        }
    }

    public static class 핫타임 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final int item = Integer.parseInt(splitted[1]);
            final int q = Integer.parseInt(splitted[2]);
            OnTimeGiver.Hottimes((int) item, (short) q);
            return 1;
        }
    }    

    public static class 드롭정렬 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {

            Connection con = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            ArrayList<Integer> Aid = new ArrayList<Integer>();
            ArrayList<Integer> Adropperid = new ArrayList<Integer>();
            ArrayList<Integer> Aitemid = new ArrayList<Integer>();
            ArrayList<Integer> Aminimum_quantity = new ArrayList<Integer>();
            ArrayList<Integer> Amaximum_quantity = new ArrayList<Integer>();
            ArrayList<Integer> Aquestid = new ArrayList<Integer>();
            ArrayList<Integer> Achance = new ArrayList<Integer>();
            try {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("SELECT * FROM drop_data WHERE dropperid > 0 ORDER BY dropperid");
                rs = ps.executeQuery();
                c.getPlayer().dropMessage(5, "1단계 작업을 시작합니다.");
                while (rs.next()) {
                    Aid.add(rs.getInt("id"));
                    Adropperid.add(rs.getInt("dropperid"));
                    Aitemid.add(rs.getInt("itemid"));
                    Aminimum_quantity.add(rs.getInt("minimum_quantity"));
                    Amaximum_quantity.add(rs.getInt("maximum_quantity"));
                    Aquestid.add(rs.getInt("questid"));
                    Achance.add(rs.getInt("chance"));
                }
                ps.close();
                rs.close();
                c.getPlayer().dropMessage(5, "1단계 작업 완료 총 " + Aid.size() + "개 / 2단계 작업을 시작합니다.");
                ps = con.prepareStatement("DELETE FROM drop_data");
                ps.executeUpdate();
                ps.close();
                ps = con.prepareStatement("ALTER TABLE drop_data AUTO_INCREMENT = 1");
                ps.executeUpdate();
                ps.close();
                for (int i = 0; i < Aid.size(); i++) {
                    ps = con.prepareStatement("INSERT INTO drop_data (dropperid, itemid, minimum_quantity, maximum_quantity, questid, chance) VALUES (?, ?, ?, ?, ?, ?)");
                    ps.setInt(1, Adropperid.get(i));
                    ps.setInt(2, Aitemid.get(i));
                    ps.setInt(3, Aminimum_quantity.get(i));
                    ps.setInt(4, Amaximum_quantity.get(i));
                    ps.setInt(5, Aquestid.get(i));
                    ps.setInt(6, Achance.get(i));
                    ps.executeUpdate();
                    ps.close();
                }
                c.getPlayer().dropMessage(6, "모든 작업을 완료했습니다.");
            } catch (Exception e) {
                System.err.println("Drop List Err..");
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
            return 1;
        }
    }

    public static class 퀘스트드롭정렬 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {

            Connection con = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            ArrayList<Integer> Aid = new ArrayList<Integer>();
            ArrayList<Integer> Adropperid = new ArrayList<Integer>();
            ArrayList<Integer> Aitemid = new ArrayList<Integer>();
            ArrayList<Integer> Aminimum_quantity = new ArrayList<Integer>();
            ArrayList<Integer> Amaximum_quantity = new ArrayList<Integer>();
            ArrayList<Integer> Aquestid = new ArrayList<Integer>();
            ArrayList<Integer> Achance = new ArrayList<Integer>();
            try {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("SELECT * FROM drop_data WHERE questid > 0 ORDER BY dropperid");
                rs = ps.executeQuery();
                c.getPlayer().dropMessage(5, "1단계 작업을 시작합니다.");
                while (rs.next()) {
                    Aid.add(rs.getInt("id"));
                    Adropperid.add(rs.getInt("dropperid"));
                    Aitemid.add(rs.getInt("itemid"));
                    Aminimum_quantity.add(rs.getInt("minimum_quantity"));
                    Amaximum_quantity.add(rs.getInt("maximum_quantity"));
                    Aquestid.add(rs.getInt("questid"));
                    Achance.add(rs.getInt("chance"));
                }
                ps.close();
                rs.close();
                c.getPlayer().dropMessage(5, "1단계 작업 완료 총 " + Aid.size() + "개 / 2단계 작업을 시작합니다.");
                ps = con.prepareStatement("DELETE FROM drop_data WHERE questid > 0");
                ps.executeUpdate();
                ps.close();
                for (int i = 0; i < Aid.size(); i++) {
                    ps = con.prepareStatement("INSERT INTO drop_data (dropperid, itemid, minimum_quantity, maximum_quantity, questid, chance) VALUES (?, ?, ?, ?, ?, ?)");
                    ps.setInt(1, Adropperid.get(i));
                    ps.setInt(2, Aitemid.get(i));
                    ps.setInt(3, Aminimum_quantity.get(i));
                    ps.setInt(4, Amaximum_quantity.get(i));
                    ps.setInt(5, Aquestid.get(i));
                    ps.setInt(6, Achance.get(i));
                    ps.executeUpdate();
                    ps.close();
                }
                c.getPlayer().dropMessage(6, "모든 작업을 완료했습니다.");
            } catch (Exception e) {
                System.err.println("Drop List Err..");
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
            return 1;
        }
    }

    public static class 상점정렬 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {

            Connection con = null;
            PreparedStatement ps = null;
            PreparedStatement ps2 = null;
            ResultSet rs = null;

            ArrayList<Integer> shops_shopid = new ArrayList<Integer>();
            ArrayList<Integer> shops_npcid = new ArrayList<Integer>();

            ArrayList<Integer> shopitems2 = new ArrayList<Integer>();
            ArrayList<Integer> shopitems3 = new ArrayList<Integer>();
            ArrayList<Integer> shopitems4 = new ArrayList<Integer>();
            ArrayList<Integer> shopitems5 = new ArrayList<Integer>();
            ArrayList<Integer> shopitems6 = new ArrayList<Integer>();
            ArrayList<Integer> shopitems7 = new ArrayList<Integer>();
            ArrayList<Integer> shopitems8 = new ArrayList<Integer>();

            try {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("SELECT * FROM shops WHERE npcid > 0 ORDER BY npcid");
                rs = ps.executeQuery();
                c.getPlayer().dropMessage(5, "1단계 작업을 시작합니다.");
                while (rs.next()) {
                    shops_shopid.add(rs.getInt(1));
                    shops_npcid.add(rs.getInt(2));
                    ps2 = con.prepareStatement("UPDATE shopitems SET shopid = ? WHERE shopid = ?");
                    ps2.setInt(1, rs.getInt(2));
                    ps2.setInt(2, rs.getInt(1));
                    ps2.executeUpdate();
                    ps2.close();
                }
                ps.close();
                rs.close();
                ps = con.prepareStatement("DELETE FROM shops");
                ps.executeUpdate();
                ps.close();
                ps = con.prepareStatement("ALTER TABLE shops AUTO_INCREMENT = 1");
                ps.executeUpdate();
                ps.close();
                c.getPlayer().dropMessage(5, "1단계 작업 완료 총 " + shops_npcid.size() + "개 / 2단계 작업을 시작합니다.");
                for (int i = 0; i < shops_npcid.size(); i++) {
                    ps = con.prepareStatement("INSERT INTO shops (shopid, npcid) VALUES (?, ?)");
                    ps.setInt(1, shops_npcid.get(i));
                    ps.setInt(2, shops_npcid.get(i));
                    ps.executeUpdate();
                    ps.close();
                    ps = con.prepareStatement("SELECT * FROM shopitems WHERE shopid = ? ORDER BY position");
                    ps.setInt(1, shops_npcid.get(i));
                    rs = ps.executeQuery();
                    while (rs.next()) {
                        shopitems2.add(rs.getInt(2));
                        shopitems3.add(rs.getInt(3));
                        shopitems4.add(rs.getInt(4));
                        shopitems5.add(rs.getInt(5));
                        shopitems6.add(rs.getInt(6));
                        shopitems7.add(rs.getInt(7));
                        shopitems8.add(rs.getInt(8));
                    }
                    ps.close();
                    rs.close();
                }
                ps = con.prepareStatement("DELETE FROM shopitems");
                ps.executeUpdate();
                ps.close();
                ps = con.prepareStatement("ALTER TABLE shopitems AUTO_INCREMENT = 1");
                ps.executeUpdate();
                ps.close();
                c.getPlayer().dropMessage(5, "2단계 작업 완료 총 " + shopitems2.size() + "개 / 3단계 작업을 시작합니다.");
                for (int i = 0; i < shopitems2.size(); i++) {
                    ps = con.prepareStatement("INSERT INTO shopitems (shopid, itemid, price, position, reqitem, reqitemq, rank) VALUES (?, ?, ?, ?, ?, ?, ?)");
                    ps.setInt(1, shopitems2.get(i));
                    ps.setInt(2, shopitems3.get(i));
                    ps.setInt(3, shopitems4.get(i));
                    ps.setInt(4, shopitems5.get(i));
                    ps.setInt(5, shopitems6.get(i));
                    ps.setInt(6, shopitems7.get(i));
                    ps.setInt(7, shopitems8.get(i));
                    ps.executeUpdate();
                    ps.close();
                }
                c.getPlayer().dropMessage(6, "모든 작업을 완료했습니다.");
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Shop List Err..");
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
                    if (ps2 != null) {
                        ps2.close();
                    }
                } catch (Exception e) {
                }
            }
            return 1;
        }
    }

    public static class 패킷출력r extends SuperGMCommand.패킷보기r {
    }

    public static class 패킷보기r extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (MapleClient.showp) {
                c.getPlayer().dropMessage(5, "리시브 패킷 출력을 중지합니다.");
            } else {
                c.getPlayer().dropMessage(6, "리시브 패킷 출력을 시작합니다.");
            }
            MapleClient.showp = !MapleClient.showp;
            return 0;
        }
    }

    public static class 패킷출력s extends SuperGMCommand.패킷보기s {
    }

    public static class 패킷보기s extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (MaplePacketLittleEndianWriter.showp) {
                c.getPlayer().dropMessage(5, "센드 패킷 출력을 중지합니다.");
            } else {
                c.getPlayer().dropMessage(6, "센드 패킷 출력을 시작합니다.");
            }
            MaplePacketLittleEndianWriter.showp = !MaplePacketLittleEndianWriter.showp;
            return 0;
        }
    } 

    public static class 피시방 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int time = 0;
            try {
                time = Integer.parseInt(splitted[1]);
            } catch (Exception e) {
                c.getPlayer().dropMessage(6, "피씨방 시간을 초기화하였습니다.");
                c.getSession().write(MaplePacketCreator.enableInternetCafe((byte) 0, c.getPlayer().getCalcPcTime()));
                c.getPlayer().setPcTime((long) 0);
                return 0;
            }
            c.getPlayer().setPcTime((long) time);
            c.getPlayer().setPcDate(GameConstants.getCurrentDate_NoTime());
            c.getPlayer().dropMessage(6, time / 1000 + "초 피씨방을 충전하였습니다.");
            c.getSession().write(MaplePacketCreator.enableInternetCafe((byte) 2, c.getPlayer().getCalcPcTime()));
            return 0;
        }
    }
    
    public static class 서버시간 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(5, "서버가 리붓 된지 " + StringUtil.getReadableMillis(ChannelServer.serverStartTime, System.currentTimeMillis()) + "경과되었습니다.");
            return 1;
        }
    }    

    public static class 시간 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getClock(CommandProcessorUtil.getOptionalIntArg(splitted, 1, 60)));
            return 1;
        }
    }
    
    public static class 모두저장 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                for (MapleCharacter chr : cserv.getPlayerStorage().getAllCharacters()) {
                    chr.saveToDB(false, false);
                }
            }
            c.getPlayer().dropMessage(5, "모두 저장 되었습니다.");
            return 1;
        }
    }

    public static class 스킬마스터 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (Skill skill : SkillFactory.getAllSkills()) {
                c.getPlayer().changeSkillLevel(skill, skill.getMaxLevel(), (byte) skill.getMaxLevel());
            }
            return 1;
        }
    }

    public static class 부분스킬마스터 extends GMCommand.스킬부분마스터 {
    }    
    public static class 스킬부분마스터 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int Job = c.getPlayer().getJob();
            int Job1 = Job / 100 * 100;
            int Job2 = Job1 + Job % 100 - Job % 10;
            int Job3 = Job2 + 1;
            int Job4 = Job3 + 1;
            for (Skill skill : SkillFactory.getAllSkills()) {
                if (skill.getId() / 10000 == Job1) {
                    c.getPlayer().changeSkillLevel(skill, skill.getMaxLevel(), (byte) skill.getMaxLevel());
                }
                if (skill.getId() / 10000 >= Job2 && skill.getId() / 10000 <= Job) {
                    c.getPlayer().changeSkillLevel(skill, skill.getMaxLevel(), (byte) skill.getMaxLevel());
                }
            }
            return 1;
        }
    }  
    
    public static class 스킬초기화 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            Map<Skill, SkillEntry> skills = new LinkedHashMap<Skill, SkillEntry>(c.getPlayer().getSkills());
            List<Skill> skillss = new ArrayList<Skill>();
            for (Skill skill : skills.keySet()) {
                skillss.add(skill);
            }
            for (Skill i : skillss) {
                c.getPlayer().changeSkillLevel(i, (byte) 0, (byte) 0);
            }
            return 1;
        }
    }    

    public static class 스킬 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            Skill skill = SkillFactory.getSkill(Integer.parseInt(splitted[1]));
            byte level = (byte) CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1);
            byte masterlevel = (byte) CommandProcessorUtil.getOptionalIntArg(splitted, 3, 1);

            if (level > skill.getMaxLevel()) {
                level = (byte) skill.getMaxLevel();
            }
            if (masterlevel > skill.getMaxLevel()) {
                masterlevel = (byte) skill.getMaxLevel();
            }
            c.getPlayer().changeSkillLevel(skill, level, masterlevel);
            return 1;
        }
    }

    public static class 스킬포인트 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().setRemainingSp(CommandProcessorUtil.getOptionalIntArg(splitted, 1, 1));
            c.getSession().write(MaplePacketCreator.updateSp(c.getPlayer(), false));
            return 1;
        }
    }

    public static class 직업 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (MapleCarnivalChallenge.getJobNameById(Integer.parseInt(splitted[1])).length() == 0) {
                c.getPlayer().dropMessage(5, "존재하지않는 직업입니다");
                return 0;
            }
            c.getPlayer().changeJob(Integer.parseInt(splitted[1]));
            return 1;
        }
    }

    public static class 상점 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleShopFactory shop = MapleShopFactory.getInstance();
            int shopId = Integer.parseInt(splitted[1]);
            if (shop.getShop(shopId) != null) {
                shop.getShop(shopId).sendShop(c);
            }
            return 1;
        }
    }

    public static class 레벨업 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getLevel() < 999) {
                c.getPlayer().gainExp(500000000, true, false, true);
            }
            return 1;
        }
    }
    
   
    public static class 아이템 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final int itemId = Integer.parseInt(splitted[1]);
            final short quantity = (short) CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1);

            if (!c.getPlayer().isAdmin()) {
                for (int i : GameConstants.itemBlock) {
                    if (itemId == i) {
                        c.getPlayer().dropMessage(5, "이 아이템은 얻을 수 없습니다");
                        return 0;
                    }
                }
            }
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (GameConstants.isPet(itemId)) {
                c.getPlayer().dropMessage(5, "펫아이템은 캐시샵에서 구매해주세요");
            } else if (!ii.itemExists(itemId)) {
                c.getPlayer().dropMessage(5, itemId + " 은 존재하지 않는 아이템 코드입니다");
            } else {
                Item item;
                short flag = (short) ItemFlag.LOCK.getValue();

                if (GameConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                    item = ii.oriStats((Equip) ii.getEquipById(itemId));
                } else {
                    item = new client.inventory.Item(itemId, (byte) 0, quantity, (byte) 0);

                }

                MapleInventoryManipulator.addbyItem(c, item);
            }
            return 1;
        }
    }

    public static class 레벨 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().setLevel(Short.parseShort(splitted[1]));
            c.getPlayer().levelUp();
            if (c.getPlayer().getExp() < 0) {
                c.getPlayer().gainExp(-c.getPlayer().getExp(), false, false, true);
            }
            return 1;
        }
    }

    public static class 자동이벤트 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final EventManager em = c.getChannelServer().getEventSM().getEventManager("AutomatedEvent");
            if (em != null) {
                em.scheduleRandomEvent();
            }
            return 1;
        }
    }

    public static class 이벤트리셋 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (ChannelServer instance : ChannelServer.getAllInstances()) {
                instance.reloadEvents();
            }
            c.getPlayer().dropMessage(6, "완료되었습니다.");
            return 1;
        }
    }

    public static class 맵리셋 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().resetFully();
            c.getPlayer().dropMessage(6, "완료되었습니다.");
            return 1;
        }
    }

    public static class 이벤트시작 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getChannelServer().getEvent() == c.getPlayer().getMapId()) {
                MapleEvent.setEvent(c.getChannelServer(), false);
                //c.getPlayer().dropMessage(5, "이벤트가 시작되었습니다");
                return 1;
            } else {
                c.getPlayer().dropMessage(5, "!스케쥴이벤트 을 이벤트맵에서 먼저 실행해주세요");
                return 0;
            }
        }
    }

    public static class 이벤트예약 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final MapleEventType type = MapleEventType.getByString(splitted[1]);
            if (type == null) {
                final StringBuilder sb = new StringBuilder("입력코드: ");
                for (MapleEventType t : MapleEventType.values()) {
                    sb.append(t.name()).append(",");
                }
                c.getPlayer().dropMessage(5, sb.toString().substring(0, sb.toString().length() - 1));
                return 0;
            }
            final String msg = MapleEvent.scheduleEvent(type, c.getChannelServer());
            if (msg.length() > 0) {
                c.getPlayer().dropMessage(5, msg);
                return 0;
            }
            return 1;
        }
    }

    public static class 아이템삭제 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "필요 <name> <itemid>");
                return 0;
            }
            MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chr == null) {
                c.getPlayer().dropMessage(6, "존재하지 않는 유저 입니다");
                return 0;
            }
            chr.removeAll(Integer.parseInt(splitted[2]), false);
            //c.getPlayer().dropMessage(6, splitted[2] + " 모든아이템이 삭제됨 " + splitted[1]);
            return 1;

        }
    }

    public static class 킬맵 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (MapleCharacter map : c.getPlayer().getMap().getCharactersThreadsafe()) {
                if (map != null && !map.isGM()) {
                    map.getStat().setHp((short) 0, map);
                    map.getStat().setMp((short) 0, map);
                    map.updateSingleStat(MapleStat.HP, 0);
                    map.updateSingleStat(MapleStat.MP, 0);
                }
            }
            return 1;
        }
    }

    public static class Disease extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "!disease <type> [charname] <level> where type = SEAL/DARKNESS/WEAKEN/STUN/CURSE/POISON/SLOW/SEDUCE/REVERSE/ZOMBIFY/POTION/SHADOW/BLIND/FREEZE/POTENTIAL");
                return 0;
            }
            int type = 0;
            if (splitted[1].equalsIgnoreCase("SEAL")) {
                type = 120;
            } else if (splitted[1].equalsIgnoreCase("DARKNESS")) {
                type = 121;
            } else if (splitted[1].equalsIgnoreCase("WEAKEN")) {
                type = 122;
            } else if (splitted[1].equalsIgnoreCase("STUN")) {
                type = 123;
            } else if (splitted[1].equalsIgnoreCase("CURSE")) {
                type = 124;
            } else if (splitted[1].equalsIgnoreCase("POISON")) {
                type = 125;
            } else if (splitted[1].equalsIgnoreCase("SLOW")) {
                type = 126;
            } else if (splitted[1].equalsIgnoreCase("SEDUCE")) {
                type = 128;
            } else if (splitted[1].equalsIgnoreCase("REVERSE")) {
                type = 132;
            } else if (splitted[1].equalsIgnoreCase("ZOMBIFY")) {
                type = 133;
            } else if (splitted[1].equalsIgnoreCase("POTION")) {
                type = 134;
            } else if (splitted[1].equalsIgnoreCase("SHADOW")) {
                type = 135;
            } else if (splitted[1].equalsIgnoreCase("BLIND")) {
                type = 136;
            } else if (splitted[1].equalsIgnoreCase("FREEZE")) {
                type = 137;
            } else if (splitted[1].equalsIgnoreCase("POTENTIAL")) {
                type = 138;
            } else {
                c.getPlayer().dropMessage(6, "!disease <type> [charname] <level> where type = SEAL/DARKNESS/WEAKEN/STUN/CURSE/POISON/SLOW/SEDUCE/REVERSE/ZOMBIFY/POTION/SHADOW/BLIND/FREEZE/POTENTIAL");
                return 0;
            }
            if (splitted.length == 4) {
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[2]);
                if (victim == null) {
                    c.getPlayer().dropMessage(5, "Not found.");
                    return 0;
                }
                victim.disease(type, CommandProcessorUtil.getOptionalIntArg(splitted, 3, 1));
            } else {
                for (MapleCharacter victim : c.getPlayer().getMap().getCharactersThreadsafe()) {
                    victim.disease(type, CommandProcessorUtil.getOptionalIntArg(splitted, 3, 1));
                }
            }
            return 1;
        }
    }

    public static class 힐 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getStat().heal(c.getPlayer());
            c.getPlayer().dispelDebuffs();
            return 0;
        }
    }

    public static class 확성기 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            World.Broadcast.broadcastSmega(MaplePacketCreator.serverNotice(3, victim == null ? c.getChannel() : victim.getClient().getChannel(), victim == null ? splitted[1] : victim.getName() + " : " + StringUtil.joinStringFrom(splitted, 2), true));
            return 1;
        }
    }

    public static class 채팅 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim == null) {
                c.getPlayer().dropMessage(5, "유저를 찾을 수 없습니다 : '" + splitted[1]);
                return 0;
            } else {
                victim.getMap().broadcastMessage(MaplePacketCreator.getChatText(victim.getId(), StringUtil.joinStringFrom(splitted, 2), victim.isGM(), 0));
            }
            return 1;
        }
    }

    public static class 위치 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            StringBuilder builder = new StringBuilder("위치: ").append(c.getPlayer().getMap().getCharactersThreadsafe().size()).append(", ");
            for (MapleCharacter chr : c.getPlayer().getMap().getCharactersThreadsafe()) {
                if (builder.length() > 150) { // wild guess :o
                    builder.setLength(builder.length() - 2);
                    c.getPlayer().dropMessage(6, builder.toString());
                    builder = new StringBuilder();
                }
                builder.append(MapleCharacterUtil.makeMapleReadable(chr.getName()));
                builder.append(", ");
            }
            builder.setLength(builder.length() - 2);
            c.getPlayer().dropMessage(6, builder.toString());
            return 1;
        }
    }

    public static class 엔피시삭제 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().resetNPCs();
            return 1;
        }
    }

    public static class 공지 extends CommandExecute {

        protected static int getNoticeType(String typestring) {
            if (typestring.equals("공")) {
                return 0;
            } else if (typestring.equals("팝")) {
                return 1;
            } else if (typestring.equals("갈공")) {
                return 5;
            } else if (typestring.equals("갈")) {
                return 5;
            } else if (typestring.equals("파")) {
                return 6;
            }
            return -1;
        }

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int joinmod = 1;
            int range = -1;
            if (splitted[1].equals("맵")) {
                range = 0;
            } else if (splitted[1].equals("채널")) {
                range = 1;
            } else if (splitted[1].equals("월드")) {
                range = 2;
            }

            int tfrom = 2;
            if (range == -1) {
                range = 2;
                tfrom = 1;
            }
            int type = getNoticeType(splitted[tfrom]);
            if (type == -1) {
                type = 0;
                joinmod = 0;
            }
            StringBuilder sb = new StringBuilder();
            if (splitted[tfrom].equals("갈공")) {
                sb.append("[공지사항]");
            } else {
                sb.append("");
            }
            joinmod += tfrom;
            sb.append(StringUtil.joinStringFrom(splitted, joinmod));

            byte[] packet = MaplePacketCreator.serverNotice(type, sb.toString());
            if (range == 0) {
                c.getPlayer().getMap().broadcastMessage(packet);
            } else if (range == 1) {
                ChannelServer.getInstance(c.getChannel()).broadcastPacket(packet);
            } else if (range == 2) {
                World.Broadcast.broadcastMessage(packet);
            }
            return 1;
        }
    }

    public static class 노란색공지 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int range = -1;
            if (splitted[1].equals("맵")) {
                range = 0;
            } else if (splitted[1].equals("채널")) {
                range = 1;
            } else if (splitted[1].equals("월드")) {
                range = 2;
            }
            if (range == -1) {
                range = 2;
            }
            byte[] packet = MaplePacketCreator.yellowChat((splitted[0].equals("!y") ? ("[" + c.getPlayer().getName() + "] ") : "") + StringUtil.joinStringFrom(splitted, 2));
            if (range == 0) {
                c.getPlayer().getMap().broadcastMessage(packet);
            } else if (range == 1) {
                ChannelServer.getInstance(c.getChannel()).broadcastPacket(packet);
            } else if (range == 2) {
                World.Broadcast.broadcastMessage(packet);
            }
            return 1;
        }
    }
    
    public static class 풀메소 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().gainMeso(Integer.MAX_VALUE - c.getPlayer().getMeso(), true);
            return 1;
        }
    }

    public static class Y extends 노란색공지 {
    }

}