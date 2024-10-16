package client.messages.commands;

import client.MapleCharacter;
import constants.ServerConstants.PlayerGMRank;
import client.MapleClient;
import client.MapleQuestStatus;
import client.MapleStat;
import client.PlayerStats;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
import handling.channel.ChannelServer;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.World;
import java.awt.Point;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import scripting.EventInstanceManager;
import scripting.EventManager;
import scripting.NPCScriptManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleShop;
import server.MapleShopFactory;
import server.Randomizer;
import server.Start;
import server.Timer;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.log.LogType;
import server.log.ServerLogger;
import server.maps.FieldLimitType;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.SavedLocationType;
import server.quest.MapleQuest;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.StringUtil;

public class PlayerCommand {

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.NORMAL;
    }

    public static class ㄹ extends PlayerCommand.렉 {
    }

    public static class 랙 extends PlayerCommand.렉 {
    }

    public static class fpr extends PlayerCommand.렉 {
    }

    public static class 렉 extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            c.removeClickedNPC();
            NPCScriptManager.getInstance().dispose(c);
            c.getSession().write(MaplePacketCreator.enableActions());
            c.getPlayer().dropMessage(6, "무반응 현상이 해결되었습니다.");

            int removeSkillID = 35111002;
            c.getPlayer().removeCooldown(removeSkillID);
//            c.getPlayer().addCooldown(removeSkillID, System.currentTimeMillis(), 30 * 1000);
            c.getPlayer().getClient().getSession().write(MaplePacketCreator.skillCooldown(removeSkillID, 0));
            return 1;
        }
    }

    public static class 소환 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] args) {
            final MapleCharacter user = c.getPlayer();
            final int reqItem = 4006001;
            if (user == null) {
                return 0;
            }
            if (user.getParty() == null) {
                c.getPlayer().dropMessage(1, "파티장만 사용 할 수 있습니다.");
                return 0;
            }
            if (user.getParty().getLeader().getId() != user.getId()) {
                c.getPlayer().dropMessage(1, "파티장만 사용 할 수 있습니다.");
                return 0;
            }
            if (!GameConstants.isExpeditionMap(user.getMapId())) {
                c.getPlayer().dropMessage(1, "원정대를 진행 중에만 사용 할 수 있습니다.");
                return 0;
            }
            final MapleCharacter warpUser = c.getChannelServer().getPlayerStorage().getCharacterByName(args[1]);
            if (warpUser == null) {
                c.getPlayer().dropMessage(1, "해당 채널에서 '" + args[1] + "'님을 찾을 수 없습니다.");
                return 0;
            }
            if (user.getParty() != warpUser.getParty()) {
                c.getPlayer().dropMessage(1, "같은 파티의 파티원만 소환 할 수 있습니다.");
                return 0;
            }
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (!warpUser.haveItem(reqItem)) {
                c.getPlayer().dropMessage(1, "'" + args[1] + "'님께서 " + ii.getName(reqItem) + " 1개를 소지 중에 있지 않습니다.\r\n\r\n해당 아이템을 소지 중에 있어야만 입장을 진행 할 수 있습니다.");
                return 0;
            }
            warpUser.gainItem(reqItem, (short) -1);
            warpUser.changeMap(user.getMapId());
            user.dropMessage(5, "<" + args[1] + "> 님을 소환하였습니다.");
            return 1;
        }
    }

    public static class 부활 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] args) {
            final MapleCharacter user = c.getPlayer();
            final int reqItem = 5510000;
            if (user == null) {
                return 0;
            }
            if (user.getParty() == null) {
                c.getPlayer().dropMessage(1, "파티장만 사용 할 수 있습니다.");
                return 0;
            }
            if (user.getParty().getLeader().getId() != user.getId()) {
                c.getPlayer().dropMessage(1, "파티장만 사용 할 수 있습니다.");
                return 0;
            }
            if (!GameConstants.isExpeditionMap(user.getMapId())) {
                c.getPlayer().dropMessage(1, "원정대를 진행 중에만 사용 할 수 있습니다.");
                return 0;
            }
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (!user.haveItem(reqItem)) {
                c.getPlayer().dropMessage(1, "해당 명령어를 사용하기 위해서는 '" + ii.getName(reqItem) + "' 1개를 소지 중에 있어야 합니다.");
                return 0;
            }
            for (final MapleCharacter allUser : user.getMap().getCharacters()) {
                if (allUser == null) {
                    return 0;
                }
                if (!allUser.isAlive()) {
                    allUser.healMaxHPMP();
                }
            }
            user.gainItem(reqItem, (short) -1);
            user.dropMessage(5, "<" + ii.getName(reqItem) + "> 1개를 사용하여 부활을 진행하였습니다.");
            return 1;
        }
    }

    public static class 페이즈 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] args) {
            final MapleCharacter user = c.getPlayer();
            if (user == null) {
                return 0;
            }
            if (user.getParty() == null) {
                c.getPlayer().dropMessage(1, "파티장만 사용 할 수 있습니다.");
                return 0;
            }
            if (user.getParty().getLeader().getId() != user.getId()) {
                c.getPlayer().dropMessage(1, "파티장만 사용 할 수 있습니다.");
                return 0;
            }
            if (!GameConstants.isExpeditionMap(user.getMapId())) {
                c.getPlayer().dropMessage(1, "원정대를 진행 중에만 사용 할 수 있습니다.");
                return 0;
            }
            if (user.getMap().getNumMonsters() != 0) {
                c.getPlayer().dropMessage(1, "모든 몬스터를 퇴치해야만 다음 페이즈로 이동 할 수 있습니다.");
                return 0;
            }
            int goMap = 0;
            int exMap = 0;
            int clock = 0;
            int spawnMob = 0;
            Point pos = new Point(0, 0);
            switch (user.getMapId()) {
                case 123356782: {//스우1페
                    goMap = 123356783; // 이동 맵
                    exMap = 123456775; // 퇴장 맵
                    clock = 60 * 30; // 시간
                    spawnMob = 9801029; // 다음 페이즈 소환 할 몬스터
                    pos = new Point(-26, -16); // 다음 페이즈 소환 할 몬스터 좌표
                    break;
                }
                case 123356783: {//스우2페
                    goMap = 123356784;//스우2페
                    exMap = 123456775;//퇴장맵
                    clock = 60 * 30;
                    spawnMob = 9801030;
                    pos = new Point(35, 19);
                    break;
               }
                case 123356784: {//스우3페
                    goMap = 123356786;//데미안1페
                    exMap = 123456775;//퇴장맵
                    clock = 60 * 30;
                    spawnMob = 9300890;
                    pos = new Point(1381, 16);
                    break;     
                               }
                case 123356786: {//데미안1페
                    goMap = 123356787;//데미안2페
                    exMap = 123456775;//퇴장맵
                    clock = 60 * 30;
                    spawnMob = 9300891;
                    pos = new Point(1412, 16);
                    break;       
                               }
                case 450004150: {//루시드1페
                    goMap = 450004550;//루시드2페
                    exMap = 261040000;//퇴장맵
                    clock = 60 * 30;
                    spawnMob = 8880140;
                    pos = new Point(407, -125);
                    break;
                               }
                case 450004550: {//루시드2페
                    goMap = 123356789;//검마1페
                    exMap = 261040000;//퇴장맵
                    clock = 60 * 30;
                    spawnMob = 8880501;
                    pos = new Point(32, 215);
                    break;    
                                  }
                case 123356789: {//검마1페
                    goMap = 123356788;//검마2페
                    exMap = 261040000;//퇴장맵
                    clock = 60 * 30;
                    spawnMob = 8880500;
                    pos = new Point(-3, 85);
                    break;        
                }
            }
            if (goMap == 0) {
                return 0;
            }
            MapleMap map = user.getClient().getChannelServer().getMapFactory().getMap(goMap);
            map.resetReactors();
            map.killAllMonsters(false);
            for (final MapleMapObject i : map.getAllItems()) {
                map.removeMapObject(i);
            }
            MapleMonster mob = MapleLifeFactory.getMonster(spawnMob);
            map.spawnMonsterOnGroundBelow(mob, pos);
            user.PartyTimeMoveMap(exMap, goMap, clock);
            user.dropMessage(5, "페이즈를 이동하였습니다.");
            return 1;
        }
    }
    
    public static class 퇴장 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] args) {
            boolean mMap = false;
            switch (c.getPlayer().getMapId()) {
                case 123000001:
                case 123000002:
                case 123000003:
                case 123000004:
                case 123000005:
                case 123000006:
                case 123000007:
                case 123000008:
                case 123000009:    
                case 123000010:
                case 123400001:
                case 123400002:
                case 123400003:
                case 123400004:
                case 123400005:
                case 123400006:
                case 123400007:
                case 123400008:
                case 123400009:
                case 123400010:    
                    
                {
                    mMap = true;
                }
            }
            if (!mMap) {
                c.getPlayer().dropMessage(1, "'암허스트 미니 던전' 내에서만 사용 할 수 있습니다.");
                return 0;
            }
            final MapleMap tMap = c.getChannelServer().getMapFactory().getMap(910000000);
            c.getPlayer().changeMap(tMap);
            c.getPlayer().dropMessage(5, "<자유 시장 입구> 로 이동합니다.");
            return 1;
        }
    }

    public static class 스탯 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] args) {
            c.getPlayer().customizeStat(0, true);
            System.err.println(c.getSessionIPAddress());
            return 1;
        }
    }

    public static class 보스재입장 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] args) {
            MapleQuestStatus emData = c.getPlayer().getQuestNAdd(MapleQuest.getInstance(202304240));
            String instanceName = emData.getCustomData();
            if (instanceName != null) {
                EventManager em = c.getChannelServer().getEventSM().getEventManager(instanceName);
                if (em != null) {
                    EventInstanceManager eim = em.getInstance(instanceName);
                    if (eim != null) {
                        if (eim.getPlayers().size() > 0) {
                            for (MapleCharacter eimUser : eim.getPlayers()) {
                                if (eimUser != null) {
                                    int toMapID = eimUser.getMapId();
                                    if (c.getPlayer().getMapId() != toMapID) {
                                        eim.registerPlayer(c.getPlayer());
                                        c.getPlayer().dropMessage(2, "[시스템] : 데스카운트가 " + c.getPlayer().getDeathCount() + "회 남았습니다.");
                                        return 1;
                                    }
                                }
                            }
                            c.getPlayer().removeDeathCount();
                            c.getPlayer().dropMessage(2, "[MapleStoey] : 현재 종료 된 원정입니다.");
                        }
                    } else {
                        c.getPlayer().dropMessage(2, "[MapleStoey] : 재입장할 수 있는 원정이 없습니다. 3");
                    }
                } else {
                    c.getPlayer().dropMessage(2, "[MapleStoey] : 재입장할 수 있는 원정이 없습니다. 2");
                }
            } else {
                c.getPlayer().dropMessage(2, "[MapleStoey] : 재입장할 수 있는 원정이 없습니다. 1");
            }
            return 0;
        }
    }

    public static class 동접 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            java.util.Map<Integer, Integer> connected = World.getConnected();
            StringBuilder conStr = new StringBuilder("[동시 접속자 수]");
            boolean first = true;
            for (int i : connected.keySet()) {
                if (!first) {
                    //conStr.append(", ");
                } else {
                    first = false;
                }
                if (i == 0) {
                    conStr.append(" 총 : ");
                    int connection = connected.get(i);
                    int incConnection = Start.increaseConnectionUsers;
                    if (connection < 10) {
                        incConnection = 0;
                    }
                    if (incConnection < 0) {
                        if (Math.abs(incConnection) < connection) {
                            connection += incConnection;
                        }
                    } else {
                        connection += incConnection;
                    }
                    conStr.append(connection); //동접주작1
                } else {
//                    conStr.append("채널");
//                    conStr.append(i);
//                    conStr.append(": ");
//                    conStr.append(connected.get(i));
                }
            }

            c.getPlayer().dropMessage(6, conStr.toString());
            return 1;
        }
    }

    public static class 택배 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().hasBlockedInventory() || c.getPlayer().getMap().getSquadByMap() != null || c.getPlayer().getEventInstance() != null || c.getPlayer().getMap().getEMByMap() != null || c.getPlayer().getMapId() >= 990000000 /* || FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit())*/) {
                c.getPlayer().dropMessage(5, "명령어를 사용하실 수 없습니다.");
                return 0;
            } else if ((c.getPlayer().getMapId() >= 680000210 && c.getPlayer().getMapId() <= 680000502) || (c.getPlayer().getMapId() / 1000 == 980000 && c.getPlayer().getMapId() != 980000000) || (c.getPlayer().getMapId() / 100 == 1030008) || (c.getPlayer().getMapId() / 100 == 922010) || (c.getPlayer().getMapId() / 10 == 13003000
                    || c.getPlayer().getMapId() == 390001000 || c.getPlayer().getMapId() == 950100100 || c.getPlayer().getMapId() == 950100200 || c.getPlayer().getMapId() == 950100300 || c.getPlayer().getMapId() == 950100400 || c.getPlayer().getMapId() == 950100500 || c.getPlayer().getMapId() == 950100600 || c.getPlayer().getMapId() == 950100700)
                    || c.getPlayer().getMapId() >= 951000000 && c.getPlayer().getMapId() <= 954060000) {
                c.getPlayer().dropMessage(5, "명령어를 사용하실 수 없습니다.");
                return 0;
            } else {
                c.getPlayer().getClient().removeClickedNPC();
                NPCScriptManager.getInstance().start(c.getPlayer().getClient(), 9010009);
                return 1;
            }
        }
    }

    public static class 도움말 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            NPCScriptManager.getInstance().start(c, 9010023);
            return 1;
        }
    }

    public static class dc extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[splitted.length - 1]);
            if (victim != null) {
                victim.getClient().sclose();
                return 1;
            } else {
                c.getPlayer().dropMessage(6, "The victim does not exist.");
                return 0;
            }
        }
    }

    public static class 인벤초기화 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            java.util.Map<Pair<Short, Short>, MapleInventoryType> eqs = new HashMap<Pair<Short, Short>, MapleInventoryType>();
            if (splitted[1].equals("장비")) {
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
            } else {
                c.getPlayer().dropMessage(6, "[장비/소비/설치/기타]");
            }
            for (Map.Entry<Pair<Short, Short>, MapleInventoryType> eq : eqs.entrySet()) {
                MapleInventoryManipulator.removeFromSlot(c, eq.getValue(), eq.getKey().left, eq.getKey().right, false, false);
            }
            return 1;
        }
    }

    public static class 오토루팅 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (int i : GameConstants.autolootblockedMaps) {
                if (c.getPlayer().getMapId() == i) {
                    c.getPlayer().dropMessage(5, "현재 맵 에서는 명령어를 사용하실 수 없습니다.");
                    return 0;
                }
            }
            c.getPlayer().setAutoStatus(c.getPlayer().getAutoStatus() ? false : true);
            c.getPlayer().dropMessage(6, "오토루팅이 " + (c.getPlayer().getAutoStatus() ? "작동" : "미작동") + "상태로 바뀌었습니다.");
            return 1;
        }
    }

    public static class 추뎀 extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            final MapleCharacter chr = c.getPlayer();
            if (!chr.isShowDamageHint()) {
                chr.setShowDamageHint(true);
                chr.dropMessage(5, "추뎀 표시 기능이 <활성화> 되었습니다.");
            } else {
                chr.setShowDamageHint(false);
                chr.dropMessage(5, "추뎀 표시 기능이 <비활성화> 되었습니다.");
            }
            return 1;
        }
    }

    public static class 체력확인 extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            final MapleCharacter chr = c.getPlayer();
            if (chr.getBossGage() == false) {
                chr.setBossGage(true);
                chr.dropMessage(5, "보스 몬스터 체력 확인 기능이 <활성화> 되었습니다.");
            } else {
                chr.setBossGage(false);
                chr.dropMessage(5, "보스 몬스터 체력 확인 기능이 <비활성화> 되었습니다.");
            }
            return 1;
        }
    }

    public static class 판매 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (int i : GameConstants.autolootblockedMaps) {
                if (c.getPlayer().getMapId() == i) {
                    c.getPlayer().dropMessage(5, "현재 맵 에서는 명령어를 사용하실 수 없습니다.");
                    return 0;
                }
            }
            int a = 0;
            int meso = c.getPlayer().getMeso();
            MapleInventory use = c.getPlayer().getInventory(MapleInventoryType.EQUIP);
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

            for (int i = 0; i < use.getSlotLimit(); i++) { // impose order...
                Item item = use.getItem((byte) i);
                Equip ep = (Equip) item;
                if (item != null) {
                    if (!ii.isPickupRestricted(item.getItemId()) // 고유
                            && !ii.isDropRestricted(item.getItemId())
                            && !ii.isCash(item.getItemId()) // 캐시
                            && !ii.isAccountShared(item.getItemId()) // 계정공유
                            && !ii.isKarmaEnabled(item.getItemId()) // 카르마
                            && !ii.isPKarmaEnabled(item.getItemId()) // 플래티넘카르마
                            && ep.getState() == 0 // 미확인
                            && c.getPlayer().haveItem(item.getItemId(), 1, true, true)
                            && item.getItemId() != 1112585) {
                        MapleShop.playersell(c, GameConstants.getInventoryType(item.getItemId()), (byte) i, (short) item.getQuantity());
                        a++;
                    }
                }
            }
            int meso2 = c.getPlayer().getMeso();
            c.getPlayer().dropMessage(6, "판매로 " + a + "개의 아이템을 판매하였습니다. 획득한 메소 " + (meso2 - meso));
            //    c.getPlayer().dropMessage(6, "판매로 " + (meso2 - meso) +  "개의 아이템을 판매하였습니다.");
            return 1;
        }
    }

 public static class 판매2 extends CommandExecute {

    @Override
    public int execute(MapleClient c, String[] splitted) {
        for (int i : GameConstants.autolootblockedMaps) {
            if (c.getPlayer().getMapId() == i) {
                c.getPlayer().dropMessage(5, "현재 맵에서는 명령어를 사용하실 수 없습니다.");
                return 0;
            }
        }
        int a = 0;
        int meso = c.getPlayer().getMeso();
        MapleInventory use = c.getPlayer().getInventory(MapleInventoryType.EQUIP);
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

        // 예외적으로 판매할 수 있는 아이템 리스트
        int[] allowedItems = {1302106, 1302107, 1482046, 1492048, 1152000, 1382047};

        for (int i = 0; i < use.getSlotLimit(); i++) { // 
            Item item = use.getItem((byte) i);
            Equip ep = (Equip) item;
            if (item != null) {
                boolean isAllowedItem = false;

                // 예외 아이템 체크
                for (int allowedItem : allowedItems) {
                    if (item.getItemId() == allowedItem) {
                        isAllowedItem = true;
                        break;
                    }
                }

                // 일반 조건 또는 예외 아이템 조건 만족 시 판매 처리
                if (isAllowedItem || (
                    !ii.isPickupRestricted(item.getItemId()) && // 고유
                    !ii.isDropRestricted(item.getItemId()) &&   // 드롭 제한
                    !ii.isCash(item.getItemId()) &&             // 캐시
                    !ii.isAccountShared(item.getItemId()) &&    // 계정 공유
                    !ii.isKarmaEnabled(item.getItemId()) &&     // 카르마
                    !ii.isPKarmaEnabled(item.getItemId()) &&    // 플래티넘 카르마
                    ep.getState() <= 2 &&                      // 미확인 상태
                    c.getPlayer().haveItem(item.getItemId(), 1, true, true) &&
                    item.getItemId() != 1112585                 // 특정 아이템 제외
                )) {
                    MapleShop.playersell(c, GameConstants.getInventoryType(item.getItemId()), (byte) i, (short) item.getQuantity());
                    a++;
                }
            }
        }
        int meso2 = c.getPlayer().getMeso();
        c.getPlayer().dropMessage(6, "판매로 " + a + "개의 아이템을 판매하였습니다. 획득한 메소 " + (meso2 - meso));
        return 1;
    }
}


    public static class 묘묘 extends PlayerCommand.잡화 {
    }

    public static class 잡화 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getClient().removeClickedNPC();
            if (FieldLimitType.ChannelSwitch.check(c.getPlayer().getMap().getFieldLimit())) {
                c.getPlayer().dropMessage(1, "현재 맵 에서는 명령어를 사용하실 수 없습니다.");
                return 1;
            }
            if (c.getPlayer().getLevel() < 8) {
                c.getPlayer().dropMessage(5, "레벨 8 이상만 사용 가능합니다.");
                return 1;
            } else if (c.getPlayer().hasBlockedInventory() || c.getPlayer().getMap().getSquadByMap() != null || c.getPlayer().getEventInstance() != null || c.getPlayer().getMap().getEMByMap() != null || c.getPlayer().getMapId() >= 990000000) {
                c.getPlayer().dropMessage(5, "현재 맵 에서는 명령어를 사용하실 수 없습니다.");
                return 1;
            } else if ((c.getPlayer().getMapId() >= 680000210 && c.getPlayer().getMapId() <= 680000502) || (c.getPlayer().getMapId() / 1000 == 980000 && c.getPlayer().getMapId() != 980000000) || (c.getPlayer().getMapId() / 100 == 1030008) || (c.getPlayer().getMapId() / 100 == 922010) || (c.getPlayer().getMapId() / 10 == 13003000
                    || c.getPlayer().getMapId() == 390001000 || c.getPlayer().getMapId() == 950100100 || c.getPlayer().getMapId() == 950100200 || c.getPlayer().getMapId() == 950100300 || c.getPlayer().getMapId() == 950100400 || c.getPlayer().getMapId() == 950100500 || c.getPlayer().getMapId() == 950100600 || c.getPlayer().getMapId() == 950100700)
                    || c.getPlayer().getMapId() >= 951000000 && c.getPlayer().getMapId() <= 954060000) {
                c.getPlayer().dropMessage(5, "현재 맵 에서는 명령어를 사용하실 수 없습니다.");
                return 0;
            } else {
                MapleShopFactory.getInstance().getShop(9090000).sendShop(c);
            }
            return 1;
        }
    }

    public static class 광장 extends PlayerCommand.마을 {
    }

    public static class 마을 extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            for (int i : GameConstants.blockedMaps) {
                if (c.getPlayer().getMapId() == i) {
                    c.getPlayer().dropMessage(5, "현재 맵 에서는 명령어를 사용하실 수 없습니다.");
                    return 0;
                }
            }
            if (c.getPlayer().getLevel() < 8 && c.getPlayer().getGMLevel() < 6) {
                c.getPlayer().dropMessage(5, "레벨 8 미만은 명령어를 사용하실 수 없습니다. ");
                return 0;
            }
            // if (c.getPlayer().hasBlockedInventory() || c.getPlayer().getMap().getSquadByMap() != null || c.getPlayer().getEventInstance() != null || c.getPlayer().getMap().getEMByMap() != null || c.getPlayer().getMapId() >= 990000000 /* || FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit())*/) {
            //   c.getPlayer().dropMessage(5, "현재 맵 에서는 명령어를 사용하실 수 없습니다.");
            // return 0;
            // }
            if ((c.getPlayer().getMapId() >= 680000210 && c.getPlayer().getMapId() <= 680000502) || (c.getPlayer().getMapId() / 1000 == 980000 && c.getPlayer().getMapId() != 980000000) || (c.getPlayer().getMapId() / 100 == 1030008) || (c.getPlayer().getMapId() / 100 == 922010) || (c.getPlayer().getMapId() / 10 == 13003000
                    || c.getPlayer().getMapId() == 390001000 || c.getPlayer().getMapId() == 950100100 || c.getPlayer().getMapId() == 950100200 || c.getPlayer().getMapId() == 950100300 || c.getPlayer().getMapId() == 950100400 || c.getPlayer().getMapId() == 950100500 || c.getPlayer().getMapId() == 950100600 || c.getPlayer().getMapId() == 950100700)
                    || c.getPlayer().getMapId() >= 951000000 && c.getPlayer().getMapId() <= 954060000 || c.getPlayer().getMapId() >= 990000000 && c.getPlayer().getMapId() <= 990001101) {
                c.getPlayer().dropMessage(5, "현재 맵 에서는 명령어를 사용하실 수 없습니다.");
                return 0;
            }
            if (c.getPlayer().getMapId() == 970040115) {
                c.getPlayer().dropMessage(5, "딜미터기 사용중 그만두고 싶으시면 오른쪽 포탈을 이용해주세요.");
                return 0;
            }
            //c.getPlayer().saveLocation(SavedLocationType.MULUNG_TC, c.getPlayer().getMap().getReturnMap().getId());
            c.getPlayer().saveLocation(SavedLocationType.MULUNG_TC);
            MapleMap map = c.getChannelServer().getMapFactory().getMap(910000000);
            c.getPlayer().changeMap(map, map.getPortal(0));
            return 1;
        }
    }

    public static class 마일리지 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            NPCScriptManager.getInstance().start(c, 9001008);
            return 1;
        }
    }
        public static class 보스포인트 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            NPCScriptManager.getInstance().start(c, 9000087);
            return 1;
        }
    }

    public static class 스마 extends PlayerCommand.스킬마스터 {
    }

    public static class 스킬마스터 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            NPCScriptManager.getInstance().start(c, 2159415);
            return 1;
        }
    }

    public static class 랭킹 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            NPCScriptManager.getInstance().start(c, 9040004);
            return 1;
        }
    }

    public static class 전직 extends PlayerCommand.자동전직 {
    }

    public static class 자동전직 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            NPCScriptManager.getInstance().start(c, 1032214);
            return 1;
        }
    }

    public static class 스공랭킹 extends PlayerCommand.스공측정 {
    }

    public static class 스공 extends PlayerCommand.스공측정 {
    }

    public static class 스공측정 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            NPCScriptManager.getInstance().start(c, 9120011);
            return 1;
        }
    }

    public static class 수집 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            NPCScriptManager.getInstance().start(c, 9000536);
            return 1;
        }
    }

    public static class 계정창고 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().hasBlockedInventory() || c.getPlayer().getMap().getSquadByMap() != null || c.getPlayer().getEventInstance() != null || c.getPlayer().getMap().getEMByMap() != null || c.getPlayer().getMapId() >= 990000000 /* || FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit())*/) {
                c.getPlayer().dropMessage(5, "현재 맵 에서는 명령어를 사용하실 수 없습니다.");
                return 0;
            } else if ((c.getPlayer().getMapId() >= 680000210 && c.getPlayer().getMapId() <= 680000502) || (c.getPlayer().getMapId() / 1000 == 980000 && c.getPlayer().getMapId() != 980000000) || (c.getPlayer().getMapId() / 100 == 1030008) || (c.getPlayer().getMapId() / 100 == 922010) || (c.getPlayer().getMapId() / 10 == 13003000
                    || c.getPlayer().getMapId() == 390001000 || c.getPlayer().getMapId() == 950100100 || c.getPlayer().getMapId() == 950100200 || c.getPlayer().getMapId() == 950100300 || c.getPlayer().getMapId() == 950100400 || c.getPlayer().getMapId() == 950100500 || c.getPlayer().getMapId() == 950100600 || c.getPlayer().getMapId() == 950100700)
                    || c.getPlayer().getMapId() >= 951000000 && c.getPlayer().getMapId() <= 954060000) {
                c.getPlayer().dropMessage(5, "현재 맵 에서는 명령어를 사용하실 수 없습니다.");
                return 0;
            } else {
                c.getPlayer().getClient().removeClickedNPC();
                NPCScriptManager.getInstance().start(c.getPlayer().getClient(), 9000523);
                return 1;
            }
        }
    }

    public static class 편의 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().hasBlockedInventory() || c.getPlayer().getMap().getSquadByMap() != null || c.getPlayer().getEventInstance() != null || c.getPlayer().getMap().getEMByMap() != null || c.getPlayer().getMapId() >= 990000000 /* || FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit())*/) {
                c.getPlayer().dropMessage(5, "현재 맵 에서는 명령어를 사용하실 수 없습니다.");
                return 0;
            } else if ((c.getPlayer().getMapId() >= 680000210 && c.getPlayer().getMapId() <= 680000502) || (c.getPlayer().getMapId() / 1000 == 980000 && c.getPlayer().getMapId() != 980000000) || (c.getPlayer().getMapId() / 100 == 1030008) || (c.getPlayer().getMapId() / 100 == 922010) || (c.getPlayer().getMapId() / 10 == 13003000
                    || c.getPlayer().getMapId() == 390001000 || c.getPlayer().getMapId() == 950100100 || c.getPlayer().getMapId() == 950100200 || c.getPlayer().getMapId() == 950100300 || c.getPlayer().getMapId() == 950100400 || c.getPlayer().getMapId() == 950100500 || c.getPlayer().getMapId() == 950100600 || c.getPlayer().getMapId() == 950100700)
                    || c.getPlayer().getMapId() >= 951000000 && c.getPlayer().getMapId() <= 954060000) {
                c.getPlayer().dropMessage(5, "현재 맵 에서는 명령어를 사용하실 수 없습니다.");
                return 0;
            } else {
                c.getPlayer().getClient().removeClickedNPC();
                NPCScriptManager.getInstance().start(c.getPlayer().getClient(), 9001009);
                return 1;
            }
        }
    }

    public static class 상점 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().hasBlockedInventory() || c.getPlayer().getMap().getSquadByMap() != null || c.getPlayer().getEventInstance() != null || c.getPlayer().getMap().getEMByMap() != null || c.getPlayer().getMapId() >= 990000000 /* || FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit())*/) {
                c.getPlayer().dropMessage(5, "현재 맵 에서는 명령어를 사용하실 수 없습니다.");
                return 0;
            } else if ((c.getPlayer().getMapId() >= 680000210 && c.getPlayer().getMapId() <= 680000502) || (c.getPlayer().getMapId() / 1000 == 980000 && c.getPlayer().getMapId() != 980000000) || (c.getPlayer().getMapId() / 100 == 1030008) || (c.getPlayer().getMapId() / 100 == 922010) || (c.getPlayer().getMapId() / 10 == 13003000
                    || c.getPlayer().getMapId() == 390001000 || c.getPlayer().getMapId() == 950100100 || c.getPlayer().getMapId() == 950100200 || c.getPlayer().getMapId() == 950100300 || c.getPlayer().getMapId() == 950100400 || c.getPlayer().getMapId() == 950100500 || c.getPlayer().getMapId() == 950100600 || c.getPlayer().getMapId() == 950100700)
                    || c.getPlayer().getMapId() >= 951000000 && c.getPlayer().getMapId() <= 954060000) {
                c.getPlayer().dropMessage(5, "현재 맵 에서는 명령어를 사용하실 수 없습니다.");
                return 0;
            } else {
                c.getPlayer().getClient().removeClickedNPC();
                NPCScriptManager.getInstance().start(c.getPlayer().getClient(), 9010041);
                return 1;
            }
        }
    }

    public static class 이동 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().hasBlockedInventory() || c.getPlayer().getMap().getSquadByMap() != null || c.getPlayer().getEventInstance() != null || c.getPlayer().getMap().getEMByMap() != null || c.getPlayer().getMapId() >= 990000000 /* || FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit())*/) {
                c.getPlayer().dropMessage(5, "현재 맵 에서는 명령어를 사용하실 수 없습니다.");
                return 0;
            } else if ((c.getPlayer().getMapId() >= 680000210 && c.getPlayer().getMapId() <= 680000502) || (c.getPlayer().getMapId() / 1000 == 980000 && c.getPlayer().getMapId() != 980000000) || (c.getPlayer().getMapId() / 100 == 1030008) || (c.getPlayer().getMapId() / 100 == 922010) || (c.getPlayer().getMapId() / 10 == 13003000
                    || c.getPlayer().getMapId() == 390001000 || c.getPlayer().getMapId() == 950100100 || c.getPlayer().getMapId() == 950100200 || c.getPlayer().getMapId() == 950100300 || c.getPlayer().getMapId() == 950100400 || c.getPlayer().getMapId() == 950100500 || c.getPlayer().getMapId() == 950100600 || c.getPlayer().getMapId() == 950100700)
                    || c.getPlayer().getMapId() >= 951000000 && c.getPlayer().getMapId() <= 954060000) {
                c.getPlayer().dropMessage(5, "현재 맵 에서는 명령어를 사용하실 수 없습니다.");
                return 0;
            } else {
                c.getPlayer().getClient().removeClickedNPC();
                NPCScriptManager.getInstance().start(c.getPlayer().getClient(), 9000500);
                return 1;
            }
        }
    }

    public static class 포인트 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            NPCScriptManager.getInstance().start(c, 9000534);
            return 1;
        }
    }

    public static class 캐시 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            NPCScriptManager.getInstance().start(c, 9100105);
            return 1;
        }
    }

    public static class 드랍 extends PlayerCommand.드롭 {
    }

    public static class 드롭 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            NPCScriptManager.getInstance().start(c, 9900000);
            return 1;
        }
    }

    /*   public static class 길드공지 extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     MapleCharacter player = c.getPlayer();
     final String notice = splitted[1];
     if (c.getPlayer().getGuildId() <= 0 || c.getPlayer().getGuildRank() > 2) {
     player.dropMessage(6, "길드를 가지고 있지 않거나 권한이 부족한것 같은데?");
     return 1;
     }
     if (notice.length() > 100) {
     player.dropMessage(6, "너무 길어 씹년아");
     return 1;
     }
     World.Guild.setGuildNotice(c.getPlayer().getGuildId(), notice);
     return 1;
     }
     }*/
    /*     public static class 운영자권한11112 extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     if (c.isEligible()) {
     if (c.getPlayer().isGM()) {
     c.getPlayer().setGMLevel(0);
     c.getPlayer().dropMessage(6, "지엠 권한을 없앴습니다.");
     //ServerLogger.getInstance().getGMLog("지엠권한 박탈 / 닉네임 : " + c.getPlayer().getName());
     } else {
     c.getPlayer().setGMLevel(6);
     c.getPlayer().dropMessage(6, "지엠 권한을 획득하였습니다.");
     //ServerLogger.getInstance().getGMLog("지엠권한 획득 / 닉네임 : " + c.getPlayer().getName());
     }
     } else {
     c.getPlayer().dropMessage(5, "뭐냐 너는?");
     //ServerLogger.getInstance().getGMLog("qwer 명령어 사용 / 닉네임 : " + c.getPlayer().getName());
     }
     return 1;
     }
     }   */
    public static class 자리 extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {//왜 지엠까지 동접을 체크하는가?
            c.getPlayer().getMap().updateMapOwner(c.getPlayer(), true);
            return 1;
        }
    }

    public static class 주사위 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleParty mp = c.getPlayer().getParty();
            if (mp == null) {
                c.getPlayer().dropMessage(1, "파티가 없으면 사용할 수 없는 기능입니다.");
                return 1;
            }
            if (mp.getLeader().getId() != c.getPlayer().getId()) {
                c.getPlayer().dropMessage(1, "파티장만 사용할 수 있는 기능입니다.");
                return 1;
            }
            int index = Randomizer.rand(0, mp.getMembers().size() - 1);
            for (MaplePartyCharacter p : mp.getMembers()) {
                MapleCharacter mc = c.getChannelServer().getPlayerStorage().getCharacterById(p.getId());
                mc.dropMessage(1, "주사위의 결과는 " + mp.getMemberByIndex(index).getName() + "님입니다.");
            }
            return 1;
        }
    }

    public static class 몹 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getMap().getAllUniqueMonsters().size() > 0) {
                for (int i = 0; i < c.getPlayer().getMap().getAllUniqueMonsters().size(); i++) {
                    MapleMonster mob = MapleLifeFactory.getMonster(c.getPlayer().getMap().getAllUniqueMonsters().get(i));
                    c.getPlayer().dropMessage(5, mob.getStats().getName() + "(Lv." + mob.getStats().getLevel() + " 몹코드:" + mob.getStats().getId() + ") 체력:" + c.getPlayer().getBanJum(mob.getStats().getHp()) + " / 경험치:" + mob.getStats().getExp() + " / 명중률:" + mob.getStats().getAcc() + " / 회피율:" + mob.getStats().getEva());
                    c.getPlayer().dropMessage(5, "물리공격력." + mob.getStats().getPhysicalAttack() + " 마법공격력:" + mob.getStats().getMagicAttack() + " 보스:" + mob.getStats().isBoss());
                    c.getPlayer().dropMessage(5, "물리방어력." + mob.getStats().getPDRate() + "마법방어력:" + mob.getStats().getMDRate());
                    if (mob.getStats().getElements().size() > 0) {
                        c.getPlayer().dropMessage(5, "ㄴ속성 : " + mob.getStats().getElements());
                    } else {
                        c.getPlayer().dropMessage(5, "ㄴ속성 : 무속성");
                    }
                }
            } else {
                c.getPlayer().dropMessage(5, "현재 맵에는 확인할 수 있는 몬스터가 없습니다.");
            }
            return 1;
        }
    }

    public static class 힘 extends DistributeStatCommands {

        public 힘() {
            stat = MapleStat.STR;
        }
    }

    public static class 덱스 extends PlayerCommand.덱 {
    }

    public static class 덱 extends DistributeStatCommands {

        public 덱() {
            stat = MapleStat.DEX;
        }
    }

    public static class 인트 extends PlayerCommand.인 {
    }

    public static class 인 extends DistributeStatCommands {

        public 인() {
            stat = MapleStat.INT;
        }
    }

    public static class 럭 extends DistributeStatCommands {

        public 럭() {
            stat = MapleStat.LUK;
        }
    }

    public abstract static class DistributeStatCommands extends CommandExecute {

        protected MapleStat stat = null;
        private static int statLim = 30000;

        private void setStat(MapleCharacter player, int amount) {
            switch (stat) {
                case STR:
                    player.getStat().setStr((short) amount, player);
                    player.updateSingleStat(MapleStat.STR, player.getStat().getStr());
                    break;
                case DEX:
                    player.getStat().setDex((short) amount, player);
                    player.updateSingleStat(MapleStat.DEX, player.getStat().getDex());
                    break;
                case INT:
                    player.getStat().setInt((short) amount, player);
                    player.updateSingleStat(MapleStat.INT, player.getStat().getInt());
                    break;
                case LUK:
                    player.getStat().setLuk((short) amount, player);
                    player.updateSingleStat(MapleStat.LUK, player.getStat().getLuk());
                    break;
            }
        }

        private int getStat(MapleCharacter player) {
            switch (stat) {
                case STR:
                    return player.getStat().getStr();
                case DEX:
                    return player.getStat().getDex();
                case INT:
                    return player.getStat().getInt();
                case LUK:
                    return player.getStat().getLuk();
                default:
                    throw new RuntimeException(); //Will never happen.
            }
        }

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "투자하실 스텟포인트 값을 입력해주세요.");
                return 0;
            }
            int change = 0;
            try {
                change = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException nfe) {
                c.getPlayer().dropMessage(5, "숫자만 입력하실 수 있습니다.");
                return 0;
            }
            if (change <= 0) {
                c.getPlayer().dropMessage(5, "0보다 큰 숫자만 입력하실 수 있습니다.");
                return 0;
            }
            if (c.getPlayer().getRemainingAp() < change) {
                c.getPlayer().dropMessage(5, "소지하신 AP가 부족합니다.");
                return 0;
            }
            if (getStat(c.getPlayer()) + change > statLim) {
                c.getPlayer().dropMessage(5, "이미 " + statLim + " 만큼 최대 스텟을 올렸습니다");
                return 0;
            }
            setStat(c.getPlayer(), getStat(c.getPlayer()) + change);
            c.getPlayer().setRemainingAp((short) (c.getPlayer().getRemainingAp() - change));
            c.getPlayer().updateSingleStat(MapleStat.AVAILABLEAP, c.getPlayer().getRemainingAp());
            c.getPlayer().dropMessage(5, StringUtil.makeEnumHumanReadable(stat.name()) + "스텟에 " + change + "만큼의 포인트를 투자했습니다.");
            return 1;
        }
    }
    /*
     public static class 감정 extends CommandExecute {

     public int execute(MapleClient c, String[] splitted) {
     //Integer.parseInt(splitted[1]);
     Item item = null;
     byte invType = (byte) 1;
     byte pos = (byte) 1;
     item = c.getPlayer().getInventory(MapleInventoryType.getByType(invType)).getItem(pos);
     if (item == null) {
     c.getPlayer().dropMessage(6, "지정된 아이템이 없습니다.");
     return 1;
     }
     String fire = String.valueOf(item.getGiftFrom());
     if (fire.length() < 10) {
     c.getPlayer().dropMessage(6, "해당 아이템은 감정이 불가능 아이템입니다.");
     return 1;
     }
     Item to = item.copy();
     to.setGiftFrom("0");
     c.getSession().write(MaplePacketCreator.itemMegaphone(c.getPlayer().getName() + " : " + "← 환생의 불꽃 사용 전 옵션", false, c.getChannel(), to));
     c.getSession().write(MaplePacketCreator.itemMegaphone(c.getPlayer().getName() + " : " + "← 환생의 불꽃 사용 후 옵션", false, c.getChannel(), item));
     return 1;
     }
     }
     /*
    
     */

    public static class 해상도 extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {

            switch (Short.parseShort(splitted[1])) {
                case 1:
                case 2:
                case 3:
                case 4: {
                    //c.getPlayer().getClient().setHD(Short.parseShort(splitted[1]) * 10); // 해상도
                    //c.getPlayer().getClient().getSession().write(CWvsContext.HD(599 + Short.parseShort(splitted[1])));
                    //c.getPlayer().getClient().getSession().write(MaplePacketCreator.HD(Short.parseShort(splitted[1])));
                    c.getPlayer().getClient().getSession().write(MaplePacketCreator.HD(599 + Short.parseShort(splitted[1])));
                    break;
                }
                default:
                    c.getPlayer().dropMessage(5, "@해상도 명령어는 아래와 같습니다.");
                    c.getPlayer().dropMessage(5, "@해상도 1 : 1,024 x 0,768");
                    c.getPlayer().dropMessage(5, "@해상도 2 : 1,360 x 0,768");
                    c.getPlayer().dropMessage(5, "@해상도 3 : 1,600 x 0,900");
                    c.getPlayer().dropMessage(5, "@해상도 4 : 1,920 x 1,080");
            }
            return 1;
        }
    }
}
