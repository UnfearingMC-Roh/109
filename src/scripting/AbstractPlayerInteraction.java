/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package scripting;

import java.awt.Point;
import java.util.List;

import client.inventory.Equip;
import client.SkillFactory;
import constants.GameConstants;
import client.Skill;
import client.MapleCharacter;
import client.MapleClient;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.MapleQuestStatus;
import client.MapleTrait.MapleTraitType;
import client.inventory.MapleInventory;
import handling.channel.ChannelServer;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.guild.MapleGuild;
import server.Randomizer;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.maps.MapleMap;
import server.maps.MapleReactor;
import server.maps.MapleMapObject;
import server.maps.SavedLocationType;
import server.maps.Event_DojoAgent;
import server.life.MapleMonster;
import server.life.MapleLifeFactory;
import server.quest.MapleQuest;
import tools.MaplePacketCreator;
import tools.packet.PetPacket;
import tools.packet.UIPacket;
import client.inventory.MapleInventoryIdentifier;
import constants.ServerConstants;
import database.DatabaseConnection;
import handling.channel.handler.DueyHandler;
import handling.world.World;
import handling.world.guild.MapleGuildCharacter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ScheduledFuture;
import server.ItemInformation;
import server.MaplePortal;
import server.MedalRanking;
import server.MedalRanking.MedalRankingType;
import server.Timer;
import server.events.MapleEvent;
import server.events.MapleEventType;
import server.life.OverrideMonsterStats;
import server.log.LogType;
import server.log.ServerLogger;
import tools.FileoutputUtil;
import tools.Pair;
import tools.StringUtil;

public abstract class AbstractPlayerInteraction {

    protected MapleClient c;
    protected int id, id2;
    private transient ScheduledFuture<?> Timer2;
    private int fishTasking2 = 0;

    public AbstractPlayerInteraction(final MapleClient c, final int id, final int id2) {
        this.c = c;
        this.id = id;
        this.id2 = id2;
    }

    public final MapleClient getClient() {
        return c;
    }

    public final MapleClient getC() {
        return c;
    }

    public MapleCharacter getChar() {
        return c.getPlayer();
    }

    public final ChannelServer getChannelServer() {
        return c.getChannelServer();
    }

    public final MapleCharacter getPlayer() {
        return c.getPlayer();
    }

    public final EventManager getEventManager(final String event) {
        return c.getChannelServer().getEventSM().getEventManager(event);
    }

    public final EventInstanceManager getEventInstance() {
        return c.getPlayer().getEventInstance();
    }

    public final void warp(final int map) {
        final MapleMap mapz = getWarpMap(map);
        try {
            c.getPlayer().changeMap(mapz, mapz.getPortal(Randomizer.nextInt(mapz.getPortals().size())));
        } catch (Exception e) {
            c.getPlayer().changeMap(mapz, mapz.getPortal(0));
        }
    }

    public final void warp_Instanced(final int map) {
        final MapleMap mapz = getMap_Instanced(map);
        try {
            c.getPlayer().changeMap(mapz, mapz.getPortal(Randomizer.nextInt(mapz.getPortals().size())));
        } catch (Exception e) {
            c.getPlayer().changeMap(mapz, mapz.getPortal(0));
        }
    }

    public final void warp_Instanced(final int map, int pid) {
        final MapleMap mapz = getMap_Instanced(map);
        try {
            c.getPlayer().changeMap(mapz, mapz.getPortal(pid));
        } catch (Exception e) {
            c.getPlayer().changeMap(mapz, mapz.getPortal(0));
        }
    }

    public final void warp(final int map, final int portal) {
        final MapleMap mapz = getWarpMap(map);
        if (portal != 0 && map == c.getPlayer().getMapId()) { //test
            final Point portalPos = new Point(c.getPlayer().getMap().getPortal(portal).getPosition());
            if (portalPos.distanceSq(getPlayer().getTruePosition()) < 90000.0) { //estimation
                c.getSession().write(MaplePacketCreator.instantMapWarp((byte) portal)); //until we get packet for far movement, this will do
                c.getPlayer().checkFollow();
                c.getPlayer().getMap().movePlayer(c.getPlayer(), portalPos);
            } else {
                c.getPlayer().changeMap(mapz, mapz.getPortal(portal));
            }
        } else {
            c.getPlayer().changeMap(mapz, mapz.getPortal(portal));
        }
    }

    public final void warpS(final int map, final int portal) {
        final MapleMap mapz = getWarpMap(map);
        c.getPlayer().changeMap(mapz, mapz.getPortal(portal));
    }

    public final void warp(final int map, String portal) {
        final MapleMap mapz = getWarpMap(map);
        if (map == 109060000 || map == 109060002 || map == 109060004) {
            portal = mapz.getSnowballPortal();
        }
        if (map == c.getPlayer().getMapId()) { //test
            final Point portalPos = new Point(c.getPlayer().getMap().getPortal(portal).getPosition());
            if (portalPos.distanceSq(getPlayer().getTruePosition()) < 90000.0) { //estimation
                c.getPlayer().checkFollow();
                c.getSession().write(MaplePacketCreator.instantMapWarp((byte) c.getPlayer().getMap().getPortal(portal).getId()));
                c.getPlayer().getMap().movePlayer(c.getPlayer(), new Point(c.getPlayer().getMap().getPortal(portal).getPosition()));
            } else {
                c.getPlayer().changeMap(mapz, mapz.getPortal(portal));
            }
        } else {
            c.getPlayer().changeMap(mapz, mapz.getPortal(portal));
        }
    }

    public final void warpS(final int map, String portal) {
        final MapleMap mapz = getWarpMap(map);
        if (map == 109060000 || map == 109060002 || map == 109060004) {
            portal = mapz.getSnowballPortal();
        }
        c.getPlayer().changeMap(mapz, mapz.getPortal(portal));
    }

    public final void warpMap(final int mapid, final int portal) {
        final MapleMap map = getMap(mapid);
        for (MapleCharacter chr : c.getPlayer().getMap().getCharactersThreadsafe()) {
            chr.changeMap(map, map.getPortal(portal));
        }
    }

    public final void playPortalSE() {
        c.getSession().write(MaplePacketCreator.showOwnBuffEffect(0, 9, 1, 1));
    }

    private final MapleMap getWarpMap(final int map) {
        return ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(map);
    }

    public final MapleMap getMap() {
        return c.getPlayer().getMap();
    }

    public final MapleMap getMap(final int map) {
        return getWarpMap(map);
    }

    public final MapleMap getMap_Instanced(final int map) {
        return c.getPlayer().getEventInstance() == null ? getMap(map) : c.getPlayer().getEventInstance().getMapInstance(map);
    }

    public void spawnMonster(final int id, final int qty) {
        spawnMob(id, qty, c.getPlayer().getTruePosition());
    }

    public final void spawnMobOnMap(final int id, final int qty, final int x, final int y, final int map) {
        for (int i = 0; i < qty; i++) {
            getMap(map).spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(id), new Point(x, y));
        }
    }

    public final void spawnMob(final int id, final int qty, final int x, final int y) {
        spawnMob(id, qty, new Point(x, y));
    }

    public final void spawnMob(final int id, final int x, final int y) {
        spawnMob(id, 1, new Point(x, y));
    }

    private final void spawnMob(final int id, final int qty, final Point pos) {
        for (int i = 0; i < qty; i++) {
            c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(id), pos);
        }
    }

    public final void killMob(int ids) {
        c.getPlayer().getMap().killMonster(ids);
    }

    public final void killAllMob() {
        c.getPlayer().getMap().killAllMonsters(true);
    }

    public final void addHP(final int delta) {
        c.getPlayer().addHP(delta);
    }

    public final int getPlayerStat(final String type) {
        if (type.equals("LVL")) {
            return c.getPlayer().getLevel();
        } else if (type.equals("STR")) {
            return c.getPlayer().getStat().getStr();
        } else if (type.equals("DEX")) {
            return c.getPlayer().getStat().getDex();
        } else if (type.equals("INT")) {
            return c.getPlayer().getStat().getInt();
        } else if (type.equals("LUK")) {
            return c.getPlayer().getStat().getLuk();
        } else if (type.equals("HP")) {
            return c.getPlayer().getStat().getHp();
        } else if (type.equals("MP")) {
            return c.getPlayer().getStat().getMp();
        } else if (type.equals("MAXHP")) {
            return c.getPlayer().getStat().getMaxHp();
        } else if (type.equals("MAXMP")) {
            return c.getPlayer().getStat().getMaxMp();
        } else if (type.equals("RAP")) {
            return c.getPlayer().getRemainingAp();
        } else if (type.equals("RSP")) {
            return c.getPlayer().getRemainingSp();
        } else if (type.equals("GID")) {
            return c.getPlayer().getGuildId();
        } else if (type.equals("GRANK")) {
            return c.getPlayer().getGuildRank();
        } else if (type.equals("ARANK")) {
            return c.getPlayer().getAllianceRank();
        } else if (type.equals("GM")) {
            return c.getPlayer().isGM() ? 1 : 0;
        } else if (type.equals("ADMIN")) {
            return c.getPlayer().isAdmin() ? 1 : 0;
        } else if (type.equals("GENDER")) {
            return c.getPlayer().getGender();
        } else if (type.equals("FACE")) {
            return c.getPlayer().getFace();
        } else if (type.equals("HAIR")) {
            return c.getPlayer().getHair();
        } else if (type.equals("DB")) {
            return c.getPlayer().getSubcategory();
        }
        return -1;
    }

    public final String getName() {
        return c.getPlayer().getName();
    }

    public final boolean haveItem(final int itemid) {
        return haveItem(itemid, 1);
    }

    public final boolean haveItem(final int itemid, final int quantity) {
        return haveItem(itemid, quantity, false, true);
    }

    public final boolean haveItem(final int itemid, final int quantity, final boolean checkEquipped, final boolean greaterOrEquals) {
        return c.getPlayer().haveItem(itemid, quantity, checkEquipped, greaterOrEquals);
    }

    public final boolean canHold() {
        for (int i = 1; i <= 5; i++) {
            if (c.getPlayer().getInventory(MapleInventoryType.getByType((byte) i)).getNextFreeSlot() <= -1) {
                return false;
            }
        }
        return true;
    }

    public final boolean canHoldSlots(final int slot) {
        for (int i = 1; i <= 5; i++) {
            if (c.getPlayer().getInventory(MapleInventoryType.getByType((byte) i)).isFull(slot)) {
                return false;
            }
        }
        return true;
    }

    public final boolean canHold(final int itemid) {
        return c.getPlayer().getInventory(GameConstants.getInventoryType(itemid)).getNextFreeSlot() > -1;
    }

    public final boolean canHold(final int itemid, final int quantity) {
        return MapleInventoryManipulator.checkSpace(c, itemid, quantity, "");
    }

    public final MapleQuestStatus getQuestRecord(final int id) {
        return c.getPlayer().getQuestNAdd(MapleQuest.getInstance(id));
    }

    public final MapleQuestStatus getQuestNoRecord(final int id) {
        return c.getPlayer().getQuestNoAdd(MapleQuest.getInstance(id));
    }

    public final byte getQuestStatus(final int id) {
        return c.getPlayer().getQuestStatus(id);
    }

    public final boolean isQuestActive(final int id) {
        return getQuestStatus(id) == 1;
    }

    public final boolean isQuestFinished(final int id) {
        return getQuestStatus(id) == 2;
    }

    public final void showQuestMsg(final String msg) {
        c.getSession().write(MaplePacketCreator.showQuestMsg(msg));
    }

    public final void forceStartQuest(final int id, final String data) {
        MapleQuest.getInstance(id).forceStart(c.getPlayer(), 0, data);
    }
    
    public final void forceStartQuest(final int id, final int data) {
        MapleQuest.getInstance(id).forceStart(c.getPlayer(), 0, "" + data);
    }
    
    public final void forceStartQuest(final int id, final int data, final boolean filler) {
        MapleQuest.getInstance(id).forceStart(c.getPlayer(), 0, filler ? String.valueOf(data) : null);
    }

    public void forceStartQuest(final int id) {
        MapleQuest.getInstance(id).forceStart(c.getPlayer(), 0, null);
    }

    public void forceCompleteQuest(final int id) {
        MapleQuest.getInstance(id).forceComplete(getPlayer(), 0);
    }

    public void spawnNpc(final int npcId) {
        c.getPlayer().getMap().spawnNpc(npcId, c.getPlayer().getPosition());
    }

    public final void spawnNpc(final int npcId, final int x, final int y) {
        c.getPlayer().getMap().spawnNpc(npcId, new Point(x, y));
    }

    public final void spawnNpc(final int npcId, final Point pos) {
        c.getPlayer().getMap().spawnNpc(npcId, pos);
    }

    public final void removeNpc(final int mapid, final int npcId) {
        c.getChannelServer().getMapFactory().getMap(mapid).removeNpc(npcId);
    }

    public final void removeNpc(final int npcId) {
        c.getPlayer().getMap().removeNpc(npcId);
    }

    public final void forceStartReactor(final int mapid, final int id) {
        MapleMap map = c.getChannelServer().getMapFactory().getMap(mapid);
        MapleReactor react;

        for (final MapleMapObject remo : map.getAllReactorsThreadsafe()) {
            react = (MapleReactor) remo;
            if (react.getReactorId() == id) {
                react.forceStartReactor(c);
                break;
            }
        }
    }
    
    public final void handlePinkbeanSummon(int millisecs) {
        Timer.MapTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                c.getPlayer().getMap().removeNpc(2141000);
                forceStartReactor(270050100, 2709000);
            }
        }, millisecs);
        for (MapleCharacter chr : getPlayer().getMap().getCharacters()) {
            if (chr.getQuestStatus(3522) == 1) {
                MapleQuestStatus stat = chr.getQuestNAdd(MapleQuest.getInstance(7402));
                stat.setCustomData("1");
                chr.updateQuest(stat, true);
                chr.showQuestCompletion(3522);
            }
            if (chr.getQuestStatus(3538) == 1) {
                MapleQuestStatus stat = chr.getQuestNAdd(MapleQuest.getInstance(7402));
                stat.setCustomData("1");
                chr.updateQuest(stat, true);
                chr.showQuestCompletion(3538);
            }
        }
    }    

    public final void destroyReactor(final int mapid, final int id) {
        MapleMap map = c.getChannelServer().getMapFactory().getMap(mapid);
        MapleReactor react;

        for (final MapleMapObject remo : map.getAllReactorsThreadsafe()) {
            react = (MapleReactor) remo;
            if (react.getReactorId() == id) {
                react.hitReactor(c);
                break;
            }
        }
    }

    public final void hitReactor(final int mapid, final int id) {
        MapleMap map = c.getChannelServer().getMapFactory().getMap(mapid);
        MapleReactor react;

        for (final MapleMapObject remo : map.getAllReactorsThreadsafe()) {
            react = (MapleReactor) remo;
            if (react.getReactorId() == id) {
                react.hitReactor(c);
                break;
            }
        }
    }

    public final int getJob() {
        return c.getPlayer().getJob();
    }

    public final void gainNX(final int amount) {
        c.getPlayer().modifyCSPoints(3, amount, true);
    }

    public final void gainItemPeriod(final int id, final short quantity, final int period) { //period is in days
        gainItem(id, quantity, false, period, -1, "");
    }

    public final void gainItemPeriod(final int id, final short quantity, final long period, final String owner) { //period is in days
        gainItem(id, quantity, false, period, -1, owner);
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
        gainItem(id, quantity, randomStats, period, slots, owner, c);
    }

    public final void gainItem(final int id, final short quantity, final boolean randomStats, final long period, final int slots, final String owner, final MapleClient cg) {
        if (!c.getPlayer().canHold(id, quantity) && quantity > 0) {
            DueyHandler.addNewItemToDb(id, quantity, c.getPlayer().getId(), "인벤토리", "지급", true);
            c.getPlayer().dropMessage(6, "인벤토리의 공간이 부족하여 택배로 지급되었습니다.");
            ServerLogger.getInstance().logItem(LogType.Item.FromScript, cg.getPlayer().getId(), cg.getPlayer().getName(), id, quantity, "택배", 0, "Script : " + this.id + " (" + id2 + ")");
            return;
        }
        if (quantity >= 0) {
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            final MapleInventoryType type = GameConstants.getInventoryType(id);

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
                //item.setPotential1(30041);
                //item.setPotential2(30040);
                //item.setPotential3(30035);
                
                item.setGMLog("Received from interaction " + this.id + " (" + id2 + ") on " + FileoutputUtil.CurrentReadable_Time());
                final String name = ii.getName(id);
                if (id / 10000 == 114 && name != null && name.length() > 0) { //medal
                    final String msg = "<" + name + "> 칭호를 얻었습니다.";
//                    cg.getPlayer().dropMessage(-1, msg);
                    cg.getPlayer().dropMessage(5, msg);
                }
                
                /*
                if (id == 1002140) {
                item.setPotential1(코드);
                item.setPotential2(코드);
                item.setPotential3(코드);
                */
                
                MapleInventoryManipulator.addbyItem(cg, item.copy());
            } else {
                MapleInventoryManipulator.addById(cg, id, quantity, owner == null ? "" : owner, null, period, "Received from interaction " + this.id + " (" + id2 + ") on " + FileoutputUtil.CurrentReadable_Date());
            }
            ServerLogger.getInstance().logItem(LogType.Item.FromScript, cg.getPlayer().getId(), cg.getPlayer().getName(), id, quantity, ii.getName(id), 0, "Script : " + this.id + " (" + id2 + ")");

        } else {
            MapleInventoryManipulator.removeById(cg, GameConstants.getInventoryType(id), id, -quantity, true, false);
            ServerLogger.getInstance().logItem(LogType.Item.FromScript, cg.getPlayer().getId(), cg.getPlayer().getName(), id, quantity, MapleItemInformationProvider.getInstance().getName(id), 0, "Script : " + this.id + " (" + id2 + ")");
        }
        cg.getSession().write(MaplePacketCreator.getShowItemGain(id, quantity, true));
    }

    public final boolean removeItem(final int id) { //quantity 1
        if (MapleInventoryManipulator.removeById_Lock(c, GameConstants.getInventoryType(id), id)) {
ServerLogger.getInstance().logItem(LogType.Item.FromScript, c.getPlayer().getId(), c.getPlayer().getName(), id, -1, MapleItemInformationProvider.getInstance().getName(id), 0, "Script : " + this.id + " (" + id2 + ")");            
            c.getSession().write(MaplePacketCreator.getShowItemGain(id, (short) -1, true));
            return true;
        }
        return false;
    }

    public final void changeMusic(final String songName) {
        getPlayer().getMap().broadcastMessage(MaplePacketCreator.musicChange(songName));
    }

    public final void worldMessage(final int type, final String message) {
        World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(type, message));
    }

    // default playerMessage and mapMessage to use type 5
    public final void playerMessage(final String message) {
        playerMessage(5, message);
    }

    public final void mapMessage(final String message) {
        mapMessage(5, message);
    }

    public final void guildMessage(final String message) {
        guildMessage(5, message);
    }

    public final void timeGiveItem(final int afteritem, final int time) {
        getClient().getSession().write(MaplePacketCreator.getClock(time));
        Timer.CloneTimer tMan = Timer.CloneTimer.getInstance();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (getPlayer() != null) {
                    if (getPlayer().getMapId() == 910000020) {
                        gainItem(afteritem, (byte) 1);
                        getPlayer().dropMessage(1, "알이 부화하였습니다 인벤토리를 확인해주세요.");
                    } else {
                        getPlayer().dropMessage(1, "알부화가 실패하였습니다.");
                    }
                }
            }
        };
        tMan.schedule(r, time * 1000);
    }

    public final void timeMoveMap(final int destination, final int movemap, final int time) {
        c.getPlayer().timeMoveMap(destination, movemap, time);
    }

    public final void TimeMoveMap(final int movemap, final int destination, final int time) {
        c.getPlayer().timeMoveMap(destination, movemap, time);
    }

    public final void playerMessage(final int type, final String message) {
        c.getPlayer().dropMessage(type, message);
    }

    public final void mapMessage(final int type, final String message) {
        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.serverNotice(type, message));
    }

    public final void guildMessage(final int type, final String message) {
        if (getPlayer().getGuildId() > 0) {
            World.Guild.guildPacket(getPlayer().getGuildId(), MaplePacketCreator.serverNotice(type, message));
        }
    }
    
    public final void partyMessage(final int type, final String message) {
        if (c.getPlayer().getParty() != null) {
            World.Party.partyPacket(c.getPlayer().getParty().getId(), MaplePacketCreator.serverNotice(type, message), null);
        }
    }

    public final MapleGuild getGuild() {
        return getGuild(getPlayer().getGuildId());
    }

    public final MapleGuild getGuild(int guildid) {
        return World.Guild.getGuild(guildid);
    }
    
    public final String getGuildName() {
        return World.Guild.getGuild(getPlayer().getGuildId()).getName();
    }

    public final MapleParty getParty() {
        return c.getPlayer().getParty();
    }

    public final int getCurrentPartyId(int mapid) {
        return getMap(mapid).getCurrentPartyId();
    }

    public final boolean isLeader() {
        if (getPlayer().getParty() == null) {
            return false;
        }
        return getParty().getLeader().getId() == c.getPlayer().getId();
    }

    public final boolean isAllPartyMembersAllowedJob(final int job) {
        if (c.getPlayer().getParty() == null) {
            return false;
        }
        for (final MaplePartyCharacter mem : c.getPlayer().getParty().getMembers()) {
            if (mem.getJobId() / 100 != job) {
                return false;
            }
        }
        return true;
    }

    public final boolean allMembersHere() {
        if (c.getPlayer().getParty() == null) {
            return false;
        }
        for (final MaplePartyCharacter mem : c.getPlayer().getParty().getMembers()) {
            final MapleCharacter chr = c.getPlayer().getMap().getCharacterById(mem.getId());
            if (chr == null) {
                return false;
            }
        }
        return true;
    }

    public final void warpParty(final int mapId) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            warp(mapId, 0);
            return;
        }
        final MapleMap target = getMap(mapId);
        final int cMap = getPlayer().getMapId();
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                curChar.changeMap(target, target.getPortal(0));
            }
        }
    }

    public final void warpParty(final int mapId, final int portal) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            if (portal < 0) {
                warp(mapId);
            } else {
                warp(mapId, portal);
            }
            return;
        }
        final boolean rand = portal < 0;
        final MapleMap target = getMap(mapId);
        final int cMap = getPlayer().getMapId();
        if (GameConstants.isMonsterpark(mapId)) {
            if (getPlayer().getEventInstance() != null 
                && getPlayer().getEventInstance().getName() != null
                && getPlayer().getEventInstance().getName().startsWith("MonsterPark")) { // 몬스터 파크 이벤트 스크립트 실행 체크
                for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
                    final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
                    if (curChar.getEventInstance() != null 
                        && curChar.getEventInstance().getName() != null
                        && curChar.getEventInstance().getName().startsWith("MonsterPark")) {  // 이동하는 플레이어의 몬스터 파크 실행 체크 
                        if (curChar != null && curChar.getMapId() == cMap) { // 같은맵에 있는지 체크 정상적인 경우 true
                            if (curChar.getEventInstance() == getPlayer().getEventInstance()) { //같은 스크립트인지 체크
                                curChar.changeMap(target, target.getPortal(portal)); // 이동
                            } else {
                                curChar.dropMessage(6, "현재 상태에서 이동이 불가능 합니다."); //같은 스크립트가 아니라서 이동 불가
                            }
                        } else {
                            curChar.dropMessage(6, "현재 상태에서 이동이 불가능 합니다."); //같은맵이 아니라서 이동 불가
                        }
                    } else {
                        curChar.dropMessage(6, "현재 상태에서 이동이 불가능 합니다."); // 파티원이 실행중이 아님 이동 불가
                    }
                }
            } else {
                getPlayer().dropMessage(6, "현재 상태에서 이동이 불가능 합니다."); // 포탈탄 사람이 실행중이 아님 이동 불가
            }
        } else {
            for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
                final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
                if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                    if (rand) {
                        try {
                            curChar.changeMap(target, target.getPortal(Randomizer.nextInt(target.getPortals().size())));
                        } catch (Exception e) {
                            curChar.changeMap(target, target.getPortal(0));
                        }
                    } else {
                        curChar.changeMap(target, target.getPortal(portal));
                    }
                } 
            }
        }
    }

    public final void warpParty_Instanced(final int mapId) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            warp_Instanced(mapId);
            return;
        }
        final MapleMap target = getMap_Instanced(mapId);

        final int cMap = getPlayer().getMapId();
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                curChar.changeMap(target, target.getPortal(0));
            }
        }
    }

    public final void warpParty_Instanced(final int mapId, int pid) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            warp_Instanced(mapId, pid);
            return;
        }
        final MapleMap target = getMap_Instanced(mapId);

        final int cMap = getPlayer().getMapId();
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                curChar.changeMap(target, target.getPortal(pid));
            }
        }
    }
    
    public List<MapleCharacter> getPartyMembers() {
        if (getPlayer().getParty() == null) {
            return null;
        }
        List<MapleCharacter> chars = new LinkedList<MapleCharacter>();
        for (ChannelServer channel : ChannelServer.getAllInstances()) {
            for (MapleCharacter chr : channel.getPartyMembers(getPlayer().getParty())) {
                if (chr != null) {
                    chars.add(chr);
                }
            }
        }
        return chars;
    }
    
    public final boolean havePartyItems(int id, short quantity) {
        for (MapleCharacter chr : getPartyMembers()) {
            if(!chr.haveItem(id, quantity)) {
               return false;
            }
        }
      return true;
    }
    
    public final String NotPartyitem(int id, short quantity) {
        String name = ""; int namecost = 0;
        for (MapleCharacter chr : getPartyMembers()) {
            if(!chr.haveItem(id, quantity)) {
                namecost++;
                name += chr.getName() + ", ";
            }
        }
        if (namecost == 1) {
            return name.replaceAll(", ", "");
        } else {
            return name;
        }
    }
   
    public final void removePartyItems(int id) {
        for (MapleCharacter chr : getPartyMembers()) {
             MapleInventoryManipulator.removeById(chr.getClient(), MapleInventoryType.ETC, id, 1, false, false);
        }
    }
   
    public final boolean getPartyLevel(int ulevel, int dlevel) {
        for (MapleCharacter chr : getPartyMembers()) {
             if(!(chr.getLevel() >= ulevel && chr.getLevel() < dlevel)) {
                 return false;
             }
        }
      return true;
    }

    public void gainMeso(int gain) {
        ServerLogger.getInstance().logItem(LogType.Item.FromScript, c.getPlayer().getId(), c.getPlayer().getName(), 0, 0, "메소", gain, "Script : " + this.id + " (" + this.id2 + ")");        
        c.getPlayer().gainMeso(gain, true, true);
    }

    public void gainExp(int gain) {
        c.getPlayer().gainExp(gain, true, true, true);
    }

    public void gainExpR(int gain) {
        c.getPlayer().gainExp(gain * c.getChannelServer().getExpRate(), true, true, true);
    }

    public final void givePartyItems(final int id, final short quantity, final List<MapleCharacter> party) {
        for (MapleCharacter chr : party) {
            if (quantity >= 0) {
                MapleInventoryManipulator.addById(chr.getClient(), id, quantity, "Received from party interaction " + id + " (" + id2 + ")");
            } else {
                MapleInventoryManipulator.removeById(chr.getClient(), GameConstants.getInventoryType(id), id, -quantity, true, false);
            }
            chr.getClient().getSession().write(MaplePacketCreator.getShowItemGain(id, quantity, true));
        }
    }

    public void addPartyTrait(String t, int e, final List<MapleCharacter> party) {
        for (final MapleCharacter chr : party) {
            //chr.getTrait(MapleTraitType.valueOf(t)).addExp(e, chr);
        }
    }

    public void addPartyTrait(String t, int e) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            addTrait(t, e);
            return;
        }
        final int cMap = getPlayer().getMapId();
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                //curChar.getTrait(MapleTraitType.valueOf(t)).addExp(e, curChar);
            }
        }
    }

    public void addTrait(String t, int e) {
        //getPlayer().getTrait(MapleTraitType.valueOf(t)).addExp(e, getPlayer());
    }

    public final void givePartyItems(final int id, final short quantity) {
        givePartyItems(id, quantity, false);
    }

    public final void givePartyItems(final int id, final short quantity, final boolean removeAll) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            gainItem(id, (short) (removeAll ? -getPlayer().itemQuantity(id) : quantity));
            return;
        }

        final int cMap = getPlayer().getMapId();
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                gainItem(id, (short) (removeAll ? -curChar.itemQuantity(id) : quantity), false, 0, 0, "", curChar.getClient());
            }
        }
    }

    public final void givePartyExp_PQ(final int maxLevel, final double mod, final List<MapleCharacter> party) {
        for (final MapleCharacter chr : party) {
            final int amount = (int) Math.round(GameConstants.getExpNeededForLevel(chr.getLevel() > maxLevel ? (maxLevel + ((maxLevel - chr.getLevel()) / 10)) : chr.getLevel()) / (Math.min(chr.getLevel(), maxLevel) / 5.0) / (mod * 2.0));
            chr.gainExp(amount * ServerConstants.partyQ_exp, true, true, true);
        }
    }

    public final void gainExp_PQ(final int maxLevel, final double mod) {
        final int amount = (int) Math.round(GameConstants.getExpNeededForLevel(getPlayer().getLevel() > maxLevel ? (maxLevel + (getPlayer().getLevel() / 10)) : getPlayer().getLevel()) / (Math.min(getPlayer().getLevel(), maxLevel) / 10.0) / mod);
        gainExp(amount * c.getChannelServer().getExpRate());
    }

    public final void givePartyExp_PQ(final int maxLevel, final double mod) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            final int amount = (int) Math.round(GameConstants.getExpNeededForLevel(getPlayer().getLevel() > maxLevel ? (maxLevel + (getPlayer().getLevel() / 10)) : getPlayer().getLevel()) / (Math.min(getPlayer().getLevel(), maxLevel) / 10.0) / mod);
            gainExp(amount * ServerConstants.partyQ_exp);
            return;
        }
        final int cMap = getPlayer().getMapId();
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                final int amount = (int) Math.round(GameConstants.getExpNeededForLevel(curChar.getLevel() > maxLevel ? (maxLevel + (curChar.getLevel() / 10)) : curChar.getLevel()) / (Math.min(curChar.getLevel(), maxLevel) / 10.0) / mod);
                curChar.gainExp(amount * c.getChannelServer().getExpRate(), true, true, true);
            }
        }
    }

    public final void givePartyExp(final int amount, final List<MapleCharacter> party) {
        for (final MapleCharacter chr : party) {
            if (chr.getEventInstance() != null) {
                chr.gainExp(amount * c.getChannelServer().getExpRate(), true, true, true);
            } else {
                chr.gainExp(amount * ServerConstants.partyQ_exp, true, true, true);
            }
        }
    }

    public final void givePartyExp(final int amount) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            if (getPlayer().getEventInstance() != null) {
                gainExp(amount * c.getChannelServer().getExpRate());
            } else {
                gainExp(amount * ServerConstants.partyQ_exp);
            }
            return;
        }
        final int cMap = getPlayer().getMapId();
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                if (getPlayer().getEventInstance() != null) {
                    curChar.gainExp(amount * c.getChannelServer().getExpRate(), true, true, true);
                } else {
                    curChar.gainExp(amount * ServerConstants.partyQ_exp, true, true, true);
                }
            }
        }
    }

    public final void givePartyNX(final int amount, final List<MapleCharacter> party) {
        for (final MapleCharacter chr : party) {
            chr.modifyCSPoints(3, amount, true);
        }
    }

    public final void givePartyNX(final int amount) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            gainNX(amount);
            return;
        }
        final int cMap = getPlayer().getMapId();
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                curChar.modifyCSPoints(3, amount, true);
            }
        }
    }

    public final void endPartyQuest(final int amount, final List<MapleCharacter> party) {
        for (final MapleCharacter chr : party) {
            chr.endPartyQuest(amount);
        }
    }

    public final void endPartyQuest(final int amount) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            getPlayer().endPartyQuest(amount);
            return;
        }
        final int cMap = getPlayer().getMapId();
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                curChar.endPartyQuest(amount);
            }
        }
    }
    
    public void gainExpQ(int gain) {
        c.getPlayer().gainExp((int) (gain), true, true, true);
    }

    public final void removeFromParty(final int id, final List<MapleCharacter> party) {
        for (final MapleCharacter chr : party) {
            final int possesed = chr.getInventory(GameConstants.getInventoryType(id)).countById(id);
            if (possesed > 0) {
                MapleInventoryManipulator.removeById(c, GameConstants.getInventoryType(id), id, possesed, true, false);
                chr.getClient().getSession().write(MaplePacketCreator.getShowItemGain(id, (short) -possesed, true));
            }
        }
    }

    public final void removeFromParty(final int id) {
        givePartyItems(id, (short) 0, true);
    }

    public final void useSkill(final int skill, final int level) {
        if (level <= 0) {
            return;
        }
        SkillFactory.getSkill(skill).getEffect(level).applyTo(c.getPlayer());
    }

    public final void useItem(final int id) {
        MapleItemInformationProvider.getInstance().getItemEffect(id).applyTo(c.getPlayer());
    }

    public final void cancelItem(final int id) {
        c.getPlayer().cancelEffect(MapleItemInformationProvider.getInstance().getItemEffect(id), -1);
    }

    public final int getMorphState() {
        return c.getPlayer().getMorphState();
    }
   
    public final int getDayOfWeek() {
        return Calendar.getInstance(TimeZone.getTimeZone("KST")).get(Calendar.DAY_OF_WEEK);
    }        
    
    public final void removeAll(final int id) {
        c.getPlayer().removeAll(id);
    }

    public final void gainCloseness(final int closeness, final int index) {
        final MaplePet pet = getPlayer().getPet(index);
        if (pet != null) {
            pet.setCloseness(pet.getCloseness() + (closeness * getChannelServer().getTraitRate()));
            getClient().getSession().write(PetPacket.updatePet(pet, getPlayer().getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition()), true));
        }
    }

    public final void gainClosenessAll(final int closeness) {
        for (final MaplePet pet : getPlayer().getPets()) {
            if (pet != null && pet.getSummoned()) {
                pet.setCloseness(pet.getCloseness() + closeness);
                getClient().getSession().write(PetPacket.updatePet(pet, getPlayer().getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition()), true));
            }
        }
    }

    public final void resetMap(final int mapid) {
        getMap(mapid).resetFully();
    }

    public final void openNpc(final int id) {
        getClient().removeClickedNPC();
        NPCScriptManager.getInstance().start(getClient(), id);
    }
    
    public final void openNpc(final int id, String s) {
        getClient().removeClickedNPC();
        NPCScriptManager.getInstance().start(getClient(), id, s);
    }

    public final void openNpc(final MapleClient cg, final int id) {
        cg.removeClickedNPC();
        NPCScriptManager.getInstance().start(cg, id);
    }
    

    public final int getMapId() {
        return c.getPlayer().getMap().getId();
    }

    public final boolean haveMonster(final int mobid) {
        for (MapleMapObject obj : c.getPlayer().getMap().getAllMonstersThreadsafe()) {
            final MapleMonster mob = (MapleMonster) obj;
            if (mob.getId() == mobid) {
                return true;
            }
        }
        return false;
    }

    public final int getChannelNumber() {
        return c.getChannel();
    }

    public final int getMonsterCount(final int mapid) {
        return c.getChannelServer().getMapFactory().getMap(mapid).getNumMonsters();
    }

    public final void teachSkill(final int id, final int level, final byte masterlevel) {
        getPlayer().changeSkillLevel(SkillFactory.getSkill(id), level, masterlevel);
    }

    public final void teachSkill(final int id, int level) {
        final Skill skil = SkillFactory.getSkill(id);
        if (getPlayer().getSkillLevel(skil) > level) {
            level = getPlayer().getSkillLevel(skil);
        }
        getPlayer().changeSkillLevel(skil, level, (byte) skil.getMaxLevel());
    }

    public final int getPlayerCount(final int mapid) {
        return c.getChannelServer().getMapFactory().getMap(mapid).getCharactersSize();
    }

    public final void dojo_getUp() {
        c.getSession().write(MaplePacketCreator.updateInfoQuest(1207, "pt=1;min=4;belt=1;tuto=1")); //todo
        c.getSession().write(MaplePacketCreator.Mulung_DojoUp2());
        c.getSession().write(MaplePacketCreator.instantMapWarp((byte) 6));
    }

    public final boolean dojoAgent_NextMap(final boolean dojo, final boolean fromresting) {
        if (dojo) {
            return Event_DojoAgent.warpNextMap(c.getPlayer(), fromresting, c.getPlayer().getMap());
        }
        return Event_DojoAgent.warpNextMap_Agent(c.getPlayer(), fromresting);
    }

    public final boolean dojoAgent_NextMap(final boolean dojo, final boolean fromresting, final int mapid) {
        if (dojo) {
            return Event_DojoAgent.warpNextMap(c.getPlayer(), fromresting, getMap(mapid));
        }
        return Event_DojoAgent.warpNextMap_Agent(c.getPlayer(), fromresting);
    }

    public final int dojo_getPts() {
        return c.getPlayer().getIntNoRecord(GameConstants.DOJO);
    }

    public final MapleEvent getEvent(final String loc) {
        return c.getChannelServer().getEvent(MapleEventType.valueOf(loc));
    }

    public final int getSavedLocation(final String loc) {
        final Integer ret = c.getPlayer().getSavedLocation(SavedLocationType.fromString(loc));
        if (ret == null || ret == -1) {
            return 100000000;
        }
        return ret;
    }

    public void setQuestByInfo(int qid, String info, boolean complete) {
        MapleQuestStatus stat = getQuestRecord(qid);
        stat.setCustomData(info);
        getPlayer().updateQuest(stat, true);
        if (complete) {
            showQuestClear(qid);
        }
    }    
    
    public final void settingMob(final int mid, final int num, final long hp, final int x, final int y, MapleCharacter chr) {
        MapleMonster onemob = MapleLifeFactory.getMonster(mid);

        long newhp = hp;
        int newexp = onemob.getMobExp();

        final OverrideMonsterStats overrideStats = new OverrideMonsterStats(newhp, onemob.getMobMaxMp(), newexp, false);
        for (int i = 0; i < num; i++) {
            MapleMonster mob = MapleLifeFactory.getMonster(mid);
            mob.setEventDropFlag(0);
            mob.setHp(newhp);
            mob.setOverrideStats(overrideStats);

            chr.getMap().spawnMonsterOnGroundBelow(mob, new Point(x, y));
        }
    }
    
    public final void cancelF() {
        if (Timer2 != null) {
            Timer2.cancel(false);
            Timer2 = null;
        }
    }
    
    public final void fishingTimer2(int time2, final int mob, final int x, final int y) {
        time2 *= 1000;

        Timer2 = Timer.MapTimer.getInstance().register(new Runnable() {

            public void run() {
                if (fishTasking2 <= 4) {
                    fishTasking2++;
                    getClient().getSession().write(MaplePacketCreator.sendHint("잠시후 5초뒤 몬스터가 리젠됩니다.", 250, 5));
                } else {
                    if (fishTasking2 >= 5) {
                        getClient().getSession().write(MaplePacketCreator.sendHint("몬스터가 리젠되었습니다. 처치해주세요!", 250, 5));
                        final MapleMap mapto = c.getChannelServer().getMapFactory().getMap(910000000);
                        c.getPlayer().startMapTimeLimitTask2(60, mapto);//1=1초
                        settingMob(mob, 1, 2100000000 * 5, x, y, c.getPlayer());
                        cancelF();
                        c.getPlayer().setdamagecount(true);
                    }
                }

            }
        }, time2, time2);
    }
    
    public final void warpspawn(final int map1, final int mobid, final int x, final int y) {
        final MapleMap mapz = getWarpMap(map1);
        try {
            int overflow = 0;
            while (true) {
                MaplePortal p = mapz.getPortal(Randomizer.nextInt(mapz.getPortals().size()));
                if (p.getName().equals("sp")) {
                    c.getPlayer().changeMap(mapz, p);
                    fishingTimer2(1, mobid, x, y);
                    break;
                }
                if (overflow >= 30) {
                    c.getPlayer().changeMap(mapz, mapz.getPortal(0));
                    break;
                }
                overflow++;
            }
        } catch (Exception e) {
            c.getPlayer().changeMap(mapz, mapz.getPortal(0));
        }
    }
    
    public String getDateKey(String key) {  
       Calendar ocal = Calendar.getInstance();
       int year = ocal.get(ocal.YEAR);
       int month = ocal.get(ocal.MONTH) +1;
       int day = ocal.get(ocal.DAY_OF_MONTH);
       return getPlayer().getKeyValue(year + "" + month + "" + day + "_" + key);
   }
   
   public void setDateKey(String key, String value) {
       Calendar ocal = Calendar.getInstance();
       int year = ocal.get(ocal.YEAR);
       int month = ocal.get(ocal.MONTH) +1;
       int day = ocal.get(ocal.DAY_OF_MONTH);
       getPlayer().setKeyValue(year + "" + month + "" + day + "_" + key, value, true);
   }
    
    public String getDateKeychr(String key) {  
       Calendar ocal = Calendar.getInstance();
       int year = ocal.get(ocal.YEAR);
       int month = ocal.get(ocal.MONTH) +1;
       int day = ocal.get(ocal.DAY_OF_MONTH);
       return getPlayer().getKeyValuechr(year + "" + month + "" + day + "_" + key);
   }
   
   public void setDateKeychr(String key, String value) {
       Calendar ocal = Calendar.getInstance();
       int year = ocal.get(ocal.YEAR);
       int month = ocal.get(ocal.MONTH) +1;
       int day = ocal.get(ocal.DAY_OF_MONTH);
       getPlayer().setKeyValuechr(year + "" + month + "" + day + "_" + key, value, true);
   }
   
   public String getKeyValue(String key) {  
       return getPlayer().getKeyValue(key);
   }
   
   public void setKeyValue(String key, String value) {
       getPlayer().setKeyValue(key, value, false);
   }
    
    public int getGuildBuffStat(int type, int stat, int gid) {
        if (getPlayer().getGuildId() <= 0) {
            return 0;
        }
        return World.Guild.getGuildBuffStat(type, stat, gid);
    }
    
    public int getGuildBuffLevel(int type, int gid) {
        if (getPlayer().getGuildId() <= 0) {
            return 0;
        }
        return World.Guild.getGuildBuffLevel(type, gid);
    }
    
    public void LevelUpGuildBuff(int type, int gid, int watk, int matk, int bosswatk, int level) {
        if (getPlayer().getGuildId() <= 0) {
            return;
        }
        World.Guild.LevelUpGuildBuff(type, gid, watk, matk, bosswatk, level);
    }
    
    public String getGuildBuffList() {
        String List = "";
        int working = 0;
        
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM guildinfo WHERE bufflevel > 0");
            rs = ps.executeQuery();
            while (rs.next()) {
                working++;
                List += "[#b" + getGuild(rs.getInt("gid")).getName() + "#k] #d" + rs.getString("buffname") + "#k (" + rs.getInt("bufflevel") + "레벨)\r\n";
            }
            if (working == 0) {
                List += "현재 서버내의 길드 중 버프를 가진 길드가 없습니다.";
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        return List;
    }
    
    public void 치킨버프(int value, int time) {
        if (getPlayer().getGuildId() <= 0) {
            return;
        }
        if (getGuild() != null) {
            for (MapleGuildCharacter gc : getGuild().getMembers()) {
                if (gc.isOnline()) {
                    final MapleCharacter chr = ChannelServer.getInstance(gc.getChannel()).getPlayerStorage().getCharacterById(gc.getId());
                    if (chr != null) {
                      //  chr.StartEventBuff(value, time * 60); // 1.5배, 30분
                        MapleItemInformationProvider.getInstance().getItemEffect(2022091).applyTo(chr);
                    }
                }
            }
            guildMessage(2, "[길드] : 길드의 축복의 효과로 경험치와 드롭율이 1.5배로 향상됩니다. (30분)");
        }
    }
    
    public final void gainPartyExpPQ(int exp, String pq, int mod) {
        int qid = -1;
        if (pq.equalsIgnoreCase("ludipq")) {
            qid = 199600;
        }
        if (pq.equalsIgnoreCase("kerningpq")) {
            qid = 199601;
        }
        if (pq.equalsIgnoreCase("orbispq")) {
            qid = 199602;
        }
        if (pq.equalsIgnoreCase("rnj")) {
            qid = 199603;
        }
        if (pq.equalsIgnoreCase("ellin")) {
            qid = 199604;
        }

        final int cMap = getPlayer().getMapId();
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                int modifiedExp = exp;
                if (qid > -1) {
                    double modd = mod / 100.0D;
                    MapleQuestStatus qr = curChar.getQuestNAdd(MapleQuest.getInstance(qid));
                    if (qr.getCustomData() == null) {
                        qr.setCustomData("0");
                    }
                    int count = Integer.parseInt(qr.getCustomData());
                    if (count > 0) {
                        modifiedExp *= modd;
                    }
                }
                curChar.gainExp(modifiedExp * c.getChannelServer().getExpRate(), true, true, true);
            }
        }
    }

    public void clearEffect() {
        showEffect(true, "quest/party/clear");
        playSound(true, "Party1/Clear");
        environmentChange(true, "gate");
    }

    public void showEffect(boolean broadcast, String effect) {
        if (broadcast) {
            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.showEffect(effect));
        } else {
            c.getSession().write(MaplePacketCreator.showEffect(effect));
        }
    }

    public void playSound(boolean broadcast, String sound) {
        if (broadcast) {
            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.playSound(sound));
        } else {
            c.getSession().write(MaplePacketCreator.playSound(sound));
        }
    }

    public void environmentChange(boolean broadcast, String env) {
        if (broadcast) {
            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.environmentChange(env, 2));
        } else {
            c.getSession().write(MaplePacketCreator.environmentChange(env, 2));
        }
    }

    public final void saveLocation(final String loc) {
        c.getPlayer().saveLocation(SavedLocationType.fromString(loc));
    }

    public final void saveReturnLocation(final String loc) {
        c.getPlayer().saveLocation(SavedLocationType.fromString(loc));
    }

    public final void clearSavedLocation(final String loc) {
        c.getPlayer().clearSavedLocation(SavedLocationType.fromString(loc));
    }

    public final void summonMsg(final String msg) {
        if (!c.getPlayer().hasSummon()) {
            playerSummonHint(true);
        }
        c.getSession().write(UIPacket.summonMessage(msg));
    }

    public final void summonMsg(final int type) {
        if (!c.getPlayer().hasSummon()) {
            playerSummonHint(true);
        }
        c.getSession().write(UIPacket.summonMessage(type));
    }

    public final void showInstruction(final String msg, final int width, final int height) {
        c.getSession().write(MaplePacketCreator.sendHint(msg, width, height));
    }

    public final void playerSummonHint(final boolean summon) {
        c.getPlayer().setHasSummon(summon);
        c.getSession().write(UIPacket.summonHelper(summon));
    }

    public final String getInfoQuest(final int id) {
        return c.getPlayer().getInfoQuest(id);
    }

    public final void updateInfoQuest(final int id, final String data) {
        c.getPlayer().updateInfoQuest(id, data);
    }

    public final boolean getEvanIntroState(final String data) {
        return getInfoQuest(22013).equals(data);
    }

    public final void updateEvanIntroState(final String data) {
        updateInfoQuest(22013, data);
    }

    public final void Aran_Start() {
        c.getSession().write(UIPacket.Aran_Start());
    }

    public final void evanTutorial(final String data, final int v1) {
        c.getSession().write(MaplePacketCreator.getEvanTutorial(data));
    }

    public final void AranTutInstructionalBubble(final String data) {
        c.getSession().write(UIPacket.AranTutInstructionalBalloon(data));
    }

    public final void ShowWZEffect(final String data) {
        c.getSession().write(UIPacket.AranTutInstructionalBalloon(data));
    }

    public final void showWZEffect(final String data) {
        c.getSession().write(UIPacket.ShowWZEffect(data));
    }

    public final void EarnTitleMsg(final String data) {
//        c.getSession().write(UIPacket.EarnTitleMsg(data));
    }

    public final void EnableUI(final short i) {
//        c.getSession().write(UIPacket.IntroEnableUI(i));
    }

    public final void DisableUI(final boolean enabled) {
        c.getSession().write(UIPacket.IntroDisableUI(enabled));
    }

    public final void MovieClipIntroUI(final boolean enabled) {
        c.getSession().write(UIPacket.IntroDisableUI(enabled));
        c.getSession().write(UIPacket.IntroLock(enabled));
    }

    public MapleInventoryType getInvType(int i) {
        return MapleInventoryType.getByType((byte) i);
    }

    public String getItemName(final int id) {
        return MapleItemInformationProvider.getInstance().getName(id);
    }

    public void gainPet(int id, String name, int level, int closeness, int fullness, long period, short flags) { //펫범위
        if (id > 5009000 || id < 5000000) {
            id = 5000000;
        }
        if (level > 30) {
            level = 30;
        }
        if (closeness > 30000) {
            closeness = 30000;
        }
        if (fullness > 100) {
            fullness = 100;
        }
        try {
            MapleInventoryManipulator.addById(c, id, (short) 1, "", MaplePet.createPet(id, name, level, closeness, fullness, MapleInventoryIdentifier.getInstance(), id == 5000054 ? (int) period : 0, flags, 0), id == 5000054 ? 0 : 45, "Pet from interaction " + id + " (" + id2 + ")" + " on " + FileoutputUtil.CurrentReadable_Date());
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }

    public void removeSlot(int invType, byte slot, short quantity) {
        MapleInventoryManipulator.removeFromSlot(c, getInvType(invType), slot, quantity, true);
    }

    public void gainGP(final int gp) {
        if (getPlayer().getGuildId() <= 0) {
            return;
        }
        World.Guild.gainGP(getPlayer().getGuildId(), gp); //1 for
    }

    public int getGP() {
        if (getPlayer().getGuildId() <= 0) {
            return 0;
        }
        return World.Guild.getGP(getPlayer().getGuildId()); //1 for
    }

    public void showMapEffect(String path) {
        getClient().getSession().write(UIPacket.MapEff(path));
    }

    public int itemQuantity(int itemid) {
        return getPlayer().itemQuantity(itemid);
    }

    public EventInstanceManager getDisconnected(String event) {
        EventManager em = getEventManager(event);
        if (em == null) {
            return null;
        }
        for (EventInstanceManager eim : em.getInstances()) {
            if (eim.isDisconnected(c.getPlayer()) && eim.getPlayerCount() > 0) {
                return eim;
            }
        }
        return null;
    }

    public boolean isAllReactorState(final int reactorId, final int state) {
        boolean ret = false;
        for (MapleReactor r : getMap().getAllReactorsThreadsafe()) {
            if (r.getReactorId() == reactorId) {
                ret = r.getState() == state;
            }
        }
        return ret;
    }

    public long getCurrentTime() {
        return System.currentTimeMillis();
    }

    public void spawnMonster(int id) {
        spawnMonster(id, 1, getPlayer().getTruePosition());
    }

    // summon one monster, remote location
    public void spawnMonster(int id, int x, int y) {
        spawnMonster(id, 1, new Point(x, y));
    }

    // multiple monsters, remote location
    public void spawnMonster(int id, int qty, int x, int y) {
        spawnMonster(id, qty, new Point(x, y));
    }

    // handler for all spawnMonster
    public void spawnMonster(int id, int qty, Point pos) {
        for (int i = 0; i < qty; i++) {
            getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(id), pos);
        }
    }

    public void sendNPCText(final String text, final int npc) {
        getMap().broadcastMessage(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "00 00", (byte) 0));
    }

    public boolean getTempFlag(final int flag) {
        return (c.getChannelServer().getTempFlag() & flag) == flag;
    }

    public void logPQ(String text) {
//	FileoutputUtil.log(FileoutputUtil.PQ_Log, text);
    }

    public void outputFileError(Throwable t) {
        FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, t);
    }

    public void trembleEffect(int type, int delay) {
        c.getSession().write(MaplePacketCreator.trembleEffect(type, delay));
    }

    public int nextInt(int arg0) {
        return Randomizer.nextInt(arg0);
    }

    public MapleQuest getQuest(int arg0) {
        return MapleQuest.getInstance(arg0);
    }

    public void achievement(int a) {
//        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.achievementRatio(a));
    }

    public final MapleInventory getInventory(int type) {
        return c.getPlayer().getInventory(MapleInventoryType.getByType((byte) type));
    }

    public boolean isGMS() {
        return GameConstants.GMS;
    }

    public int randInt(int arg0) {
        return Randomizer.nextInt(arg0);
    }

    public void sendDirectionStatus(int key, int value) {
//        c.getSession().write(UIPacket.getDirectionInfo(key, value));
//        c.getSession().write(UIPacket.getDirectionStatus(true));
    }

    public void sendDirectionInfo(String data) {
        //      c.getSession().write(UIPacket.getDirectionInfo(data, 2000, 0, -100, 0));
        //  c.getSession().write(UIPacket.getDirectionInfo(1, 2000));
    }

    public final void removeAllParty(final int id) {
        if (c.getPlayer().getParty() != null) {
            for (MaplePartyCharacter pchr : c.getPlayer().getParty().getMembers()) {
                MapleCharacter chr = c.getPlayer().getMap().getCharacterById(pchr.getId());
                if (chr != null) {
                    chr.removeAll(id, true);
                }
            }
        } else {
            c.getPlayer().removeAll(id, true);
        }
    }
    
    public final void ItemAllParty(final int id, final int q) {
        if (c.getPlayer().getParty() != null) {
            for (MaplePartyCharacter pchr : c.getPlayer().getParty().getMembers()) {
                MapleCharacter chr = c.getPlayer().getMap().getCharacterById(pchr.getId());
                if (chr != null) {
                    chr.gainItem(id, (short) q);
                }
            }
        } else {
            c.getPlayer().gainItem(id, (short) q);
        }
    }

    public String getCurrentDate() {
        long cur = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance(Locale.KOREAN);
        cal.setTimeInMillis(cur);
        return cal.get(Calendar.YEAR)
                + StringUtil.getLeftPaddedStr(String.valueOf(cal.get(Calendar.MONTH) + 1), '0', 2)
                + StringUtil.getLeftPaddedStr(String.valueOf(cal.get(Calendar.DAY_OF_MONTH)), '0', 2)
                + StringUtil.getLeftPaddedStr(String.valueOf(cal.get(Calendar.HOUR_OF_DAY)), '0', 2);
    }

    public final int getInvSlots(final int i) {
        return (c.getPlayer().getInventory(MapleInventoryType.getByType((byte) i)).getNumFreeSlot());
    }

    public void gndnjs(int item, short allstat, short watk, byte upgrade) {
        if (GameConstants.isEquip(item)) {
            Equip Item = (Equip) MapleItemInformationProvider.getInstance().getEquipById(item);
            Item.setStr(allstat);
            Item.setDex(allstat);
            Item.setInt(allstat);
            Item.setLuk(allstat);
            Item.setWatk(watk);
            Item.setMatk(watk);
            Item.setUpgradeSlots(upgrade);
            MapleInventoryManipulator.addFromDrop(c, Item, false);
        } else {
            gainItem(item, allstat, watk);
        }
    }

    public long getBoatsTime(String type) {
        int arrivetime = 0;
        if (type.equalsIgnoreCase("eliorbis")) {
            arrivetime = 10;
        } else if (type.equalsIgnoreCase("ludileafreariantorbis")) {
            arrivetime = 5;
        }
        Calendar cal = Calendar.getInstance(Locale.KOREAN);
        int min = cal.get(Calendar.MINUTE);
        int secs = cal.get(Calendar.SECOND) + 60 * min;
        int wait = ((arrivetime + 5) * 60);
        int left = (wait - (secs % wait)) + (arrivetime * 60);
        return left * 1000L;
    }

    public final void setBossRepeatTime(String type, int min) {
        int qid = -1;
        if (type.equalsIgnoreCase("papulatus")) {
            qid = 199700;
        }
        if (type.equalsIgnoreCase("manon")) {
            qid = 199701;
        }
        if (type.equalsIgnoreCase("pianus")) {
            qid = 199702;
        }
        if (type.equalsIgnoreCase("griffey")) {
            qid = 199703;
        }
        if (type.equalsIgnoreCase("theboss")) {
            qid = 199704;
        }
        if (qid == -1) {
            return;
        }
        for (MapleCharacter chr : getPlayer().getMap().getCharacters()) {
            MapleQuestStatus qr = chr.getQuestNAdd(MapleQuest.getInstance(qid));
            qr.setCustomData((System.currentTimeMillis() + min * 60000L) + "");
        }
    }

    public final boolean canBossEnterTime(String type) {
        int qid = -1;
        if (type.equalsIgnoreCase("papulatus")) {
            qid = 199700;
        }
        if (type.equalsIgnoreCase("manon")) {
            qid = 199701;
        }
        if (type.equalsIgnoreCase("pianus")) {
            qid = 199702;
        }
        if (type.equalsIgnoreCase("griffey")) {
            qid = 199703;
        }
        if (type.equalsIgnoreCase("theboss")) {
            qid = 199704;
        }
        if (qid == -1) {
            return false;
        }
        MapleQuestStatus qr = getPlayer().getQuestNAdd(MapleQuest.getInstance(qid));
        if (qr.getCustomData() == null) {
            return true; //null pointer exception;
        }
        long time = Long.parseLong(qr.getCustomData());
        return time < System.currentTimeMillis();
    }

    public String getMedalRanking(String type) {
        String ret = "현재 순위 ";
        ret += "\r\n\r\n";
        List<Pair<String, Integer>> l = MedalRanking.getReadOnlyRanking(MedalRanking.MedalRankingType.valueOf(type));
        if (l.isEmpty()) {
            ret += "현재 랭킹이 없습니다.";
        } else {
            int rank = 1;
            for (Pair<String, Integer> p : l) {
                String str;
                if (MedalRanking.MedalRankingType.valueOf(type).isDonor()) {
                    if (rank == 1) {
                        str = new DecimalFormat("#,###").format(p.getRight()).replace("0", "?").replace("1", "?").replace("2", "?").replace("3", "?").replace("4", "?").replace("5", "?").replace("6", "?").replace("7", "?").replace("8", "?").replace("9", "?") + "#k 메소";
                    } else {
                        str = new DecimalFormat("#,###").format(p.getRight()) + "#k 메소";
                    }
                } else if (MedalRankingType.valueOf(type) == MedalRankingType.ExpertHunter) {
                    str = new DecimalFormat("#,###").format(p.getRight()) + "#k 마리";
                } else {
                    str = new DecimalFormat("#,###").format(p.getRight()) + "#k";
                }
                ret += (rank++) + ". #b" + p.getLeft() + "#k : #r" + str + "\r\n";
            }
        }
        return ret;
    }

    public int checkMedalScore(String type, int score) {
        int z = MedalRanking.canMedalRank(MedalRanking.MedalRankingType.valueOf(type), c.getPlayer().getName(), score);
        if (z >= 0) {
            MedalRanking.addNewMedalRank(MedalRanking.MedalRankingType.valueOf(type), c.getPlayer().getName(), score);
        }
        return z;
    }

    public void removeItemFromWorld(int itemid, String msg, boolean involveSelf) {
        for (ChannelServer cserv : ChannelServer.getAllInstances()) {
            for (MapleCharacter chr : cserv.getPlayerStorage().getAllCharacters()) {
                if (chr.getId() != c.getPlayer().getId() || involveSelf) {
                    if (chr.haveItem(itemid, 1, true, true)) {
                        if (itemid / 1000000 == 1) {
                            chr.removeAllEquip(itemid, false);
                        } else {
                            chr.removeAll(itemid, true);
                        }
                        if (msg != null && !msg.isEmpty()) {
                            chr.dropMessage(5, msg);
                        }
                    }
                }
            }
        }
    }
  
    public void showQuestCompleteEffect() {
        c.getSession().write(MaplePacketCreator.showSpecialEffect(9)); // Quest completion
        getPlayer().getMap().broadcastMessage(getPlayer(), MaplePacketCreator.showSpecialEffect(getPlayer().getId(), 9), false);
    }

    public void showQuestClear(int qid) {
        getPlayer().showQuestCompletion(qid);
    }

    public final String shuffle(String origin) {
        return Randomizer.shuffle(origin);
    }
    
    static public short Answer;

    static public void setAnswer(short answer) {
        Answer = answer;
    }

    static public short getAnswer() {
        return Answer;
    }    
}
