/*
 * Copyright (C) 2013 Nemesis Maple Story Online Server Program

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package scripting.vm;

import client.*;
import client.inventory.Equip;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
import handling.channel.ChannelServer;
import handling.channel.handler.DueyHandler;
import handling.channel.handler.InterServerHandler;
import handling.world.MapleParty;
import handling.world.World;
import scripting.EventInstanceManager;
import scripting.EventManager;
import scripting.NPCScriptManager;
import server.*;
import server.log.LogType;
import server.log.ServerLogger;
import server.maps.MapleMap;
import server.maps.SavedLocationType;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.StringUtil;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author Eternal
 */
public class NPCScriptTargetFunction {

    private NPCScriptVirtualMachine vm;

    public NPCScriptTargetFunction(NPCScriptVirtualMachine vms) {
        this.vm = vms;
    }

    public void message(String msg) {
        message(5, msg);
    }

    public void message(int type, String msg) {
        if (vm.isStop()) {
            return;
        }
        vm.getClient().getPlayer().dropMessage(type, msg);
    }

    public void music(String path) {
        if (vm.isStop()) {
            return;
        }
        vm.getClient().sendPacket(MaplePacketCreator.musicChange(path));
    }

    public void fmusic(String path) {
        vm.getClient().sendPacket(MaplePacketCreator.musicChange(path));
    }

    public void changeMusic(String path) {
        if (vm.isStop()) {
            return;
        }
        getPlayer().getMap().changeMusic(path);
        getPlayer().getMap().broadcastMessage(MaplePacketCreator.musicChange(path));
    }

    public void playMusic(String path) {
        if (vm.isStop()) {
            return;
        }
        getPlayer().getMap().broadcastMessage(MaplePacketCreator.musicChange(path));
    }

    public int getitemQuantity(int itemid) {
        return getPlayer().itemQuantity(itemid);
    }

    public final boolean haveItem(final int itemid) {
        return haveItem(itemid, 1);
    }

    public final boolean haveItem(final int itemid, final int quantity) {
        return haveItem(itemid, quantity, false, true);
    }

    public final boolean haveItem(final int itemid, final int quantity, final boolean checkEquipped, final boolean greaterOrEquals) {
        return vm.getClient().getPlayer().haveItem(itemid, quantity, checkEquipped, greaterOrEquals);
    }

    public final boolean canHold() {
        for (int i = 1; i <= 5; i++) {
            if (vm.getClient().getPlayer().getInventory(MapleInventoryType.getByType((byte) i)).getNextFreeSlot() <= -1) {
                return false;
            }
        }
        return true;
    }

    public final boolean canHoldSlots(final int slot) {
        for (int i = 1; i <= 5; i++) {
            if (vm.getClient().getPlayer().getInventory(MapleInventoryType.getByType((byte) i)).isFull(slot)) {
                return false;
            }
        }
        return true;
    }

    public final int getInvSlots(final int i) {
        return (vm.getClient().getPlayer().getInventory(MapleInventoryType.getByType((byte) i)).getNumFreeSlot());
    }

    public final boolean canHold(final int itemid) {
        return vm.getClient().getPlayer().getInventory(GameConstants.getInventoryType(itemid)).getNextFreeSlot() > -1;
    }

    public final boolean canHoldInv(final int type) {
        return vm.getClient().getPlayer().getInventory(MapleInventoryType.getByType((byte) type)).getNextFreeSlot() > -1;
    }

    public final boolean canHold(final int itemid, final int quantity) {
        return MapleInventoryManipulator.checkSpace(vm.getClient(), itemid, quantity, "");
    }

    public int random(int x) {
        return Randomizer.nextInt(x);
    }

    public final void gainItemPeriod(final int id, final short quantity, final int period) { //period is in days
        gainItem(id, quantity, false, period, -1, "");
    }

    public final void gainItemPeriod(final int id, final short quantity, final boolean randomStats, final int period) { //period is in days
        gainItem(id, quantity, randomStats, period, -1, "");
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
        gainItem(id, quantity, randomStats, period, slots, owner, vm.getClient());
    }

    public final void gainItem(final int id, final short quantity, final boolean randomStats, final long period, final int slots, final String owner, final MapleClient cg) {
        if (vm.isStop()) {
            return;
        }
        vm.flushSay();
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
                final String name = ii.getName(id);
                if (id / 10000 == 114 && name != null && name.length() > 0) { //medal
                    final String msg = "<" + name + "> 칭호를 얻었습니다.";
//                    cg.getPlayer().dropMessage(-1, msg);
                    cg.getPlayer().dropMessage(5, msg);
                }
                MapleInventoryManipulator.addbyItem(cg, item.copy());
            } else {
                MapleInventoryManipulator.addById(cg, id, quantity, owner == null ? "" : owner, null, period, "Received from interaction " + FileoutputUtil.CurrentReadable_Date());
            }
        } else {
            MapleInventoryManipulator.removeById(cg, GameConstants.getInventoryType(id), id, -quantity, true, false);
            ServerLogger.getInstance().logItem(LogType.Item.FromScript, cg.getPlayer().getId(), cg.getPlayer().getName(), id, quantity, MapleItemInformationProvider.getInstance().getName(id), 0, "Script");
        }
        cg.getSession().write(MaplePacketCreator.getShowItemGain(id, quantity, true));
    }

    public boolean isExistItem(int itemID) {
        return MapleItemInformationProvider.getInstance().getItemInformation(itemID) != null;
    }

    public MapleClient getClient() {
        return vm.getClient();
    }

    public MapleCharacter getPlayer() {
        return vm.getClient().getPlayer();
    }

    public MapleMap getMap() {
        return vm.getClient().getPlayer().getMap();
    }

    public int getMapId() {
        return vm.getClient().getPlayer().getMapId();
    }

    private final MapleMap getWarpMap(final int map) {
        return ChannelServer.getInstance(getClient().getChannel()).getMapFactory().getMap(map);
    }

    public int mobCount() {
        return getMap().getNumMonsters();
    }

    public List<MapleCharacter> getCharacters() {
        return getMap().getCharactersThreadsafe();
    }

    public int getPlayerCount(int map) {
        MapleMap m = getWarpMap(map);
        if (m == null) {
            return Integer.MAX_VALUE;
        }
        return m.getCharactersSize();
    }

    public final void warp(final int map) {
        if (vm.isStop()) {
            return;
        }
        vm.flushSay();
        final MapleMap mapz = getWarpMap(map);
        try {
            int overflow = 0;
            while (true) {
                MaplePortal p = mapz.getPortal(Randomizer.nextInt(mapz.getPortals().size()));
                if (p.getName().equals("sp")) {
                    getPlayer().changeMap(mapz, p);
                    break;
                }
                if (overflow >= 30) {
                    getPlayer().changeMap(mapz, mapz.getPortal(0));
                    break;
                }
                overflow++;
            }
        } catch (Exception e) {
            getPlayer().changeMap(mapz, mapz.getPortal(0));
        }
    }

    public final void warp(final int map, final int portal) {
        if (vm.isStop()) {
            return;
        }
        vm.flushSay();
        final MapleMap mapz = getWarpMap(map);
        if ((portal != 0 && map == getPlayer().getMapId()) || map == -1) { //test
            final Point portalPos = new Point(getPlayer().getMap().getPortal(portal).getPosition());
            if (portalPos.distanceSq(getPlayer().getTruePosition()) < 90000.0 || map == -1) { //estimation
                getClient().getSession().write(MaplePacketCreator.instantMapWarp((byte) portal)); //until we get packet for far movement, this will do
                getPlayer().getMap().movePlayer(getPlayer(), portalPos);
            } else {
                getPlayer().changeMap(mapz, mapz.getPortal(portal));
            }
        } else {
            getPlayer().changeMap(mapz, mapz.getPortal(portal));
        }
    }

    public void showEffect(String effect) {
        getMap().broadcastMessage(MaplePacketCreator.showEffect(effect));
    }

    public void playSound(String sound) {
        getMap().broadcastMessage(MaplePacketCreator.playSound(sound));
    }

    public boolean isPartyLeader() {
        MapleParty party = getPlayer().getParty();
        return party != null && party.getLeader().getId() == getPlayer().getId();
    }

    public boolean checkAllMapEmpty(Integer... maps) {
        return Stream.of(maps).map(this::getWarpMap).mapToInt(MapleMap::getCharactersSize).sum() == 0;
    }

    public void resetAllMap(Integer... maps) {
        Stream.of(maps).map(this::getWarpMap).forEach(MapleMap::resetFully);
    }

    public List<MapleCharacter> getPartyMembers() {
        List<MapleCharacter> list = new LinkedList<>();
        MapleCharacter p = getPlayer();
        list.add(p);
        MapleParty party = p.getParty();
        if (party != null) {
            int pmap = p.getMapId();
            EventInstanceManager eim = p.getEventInstance();
            party.getMembers().stream()
                    .filter(pchr -> pchr.getId() != p.getId())
                    .map(pchr -> getClient().getChannelServer().getPlayerStorage().getCharacterById(pchr.getId()))
                    .filter(chr -> chr != null && (chr.getMapId() == pmap || eim != null && chr.getEventInstance() == eim))
                    .forEach(list::add);
        }
        return list;
    }

    public boolean checkLevelList(List<MapleCharacter> list, int min, int max) {
        return list.stream().map(MapleCharacter::getLevel).allMatch(lv -> min <= lv && lv <= max);
    }

    public void warpList(List<MapleCharacter> list, int map) {
        MapleMap target = getWarpMap(map);
        list.forEach(mc -> mc.changeMap(target, target.getPortal(0)));
    }

    public void warpList(List<MapleCharacter> list, int map, String portal) {
        MapleMap target = getWarpMap(map);
        list.forEach(mc -> mc.changeMap(target, target.getPortal(portal)));
    }

    public int getMeso() {
        return getPlayer().getMeso();
    }

    public int getCash(int type) {
        return getPlayer().getCSPoints(type);
    }

    public void gainMeso(int gain) {
        if (vm.isStop()) {
            return;
        }
        vm.flushSay();
        ServerLogger.getInstance().logItem(LogType.Item.FromScript, getPlayer().getId(), getPlayer().getName(), 0, 0, "메소", gain, "Script : VM - ");
        getPlayer().gainMeso(gain, true, true);
    }

    public void gainCash(int type, int amount) {
        getPlayer().modifyCSPoints(type, amount, true);
    }

    public void gainExp(int gain) {
        if (vm.isStop()) {
            return;
        }
        vm.flushSay();
        getPlayer().gainExp(gain, true, true, true);
    }

    public void setAvatar(int args) {
        if (args < 0) {
            return;
        }
        final MapleCharacter player = getPlayer();
        if (args < 100) {
            player.setSkinColor((byte) args);
            player.updateSingleStat(MapleStat.SKIN, args);
        } else if ((args >= 20000 && args < 30000) || (args >= 50000 && args < 60000)) {
            player.setFace(args);
            player.updateSingleStat(MapleStat.FACE, args);
        } else {
            player.setHair(args);
            player.updateSingleStat(MapleStat.HAIR, args);
        }
        player.equipChanged();
    }

    public final void openLegacyNpc(final int id) {
        getClient().removeClickedNPC();
        NPCScriptInvoker.dispose(getClient());
        vm.openLegacyNpc = true;
        NPCScriptManager.getInstance().start(getClient(), id);
    }

    public final void openNpc(final int id) {
        getClient().removeClickedNPC();
        NPCScriptInvoker.dispose(getClient());
        vm.openLegacyNpc = true;
        NPCScriptInvoker.runNpc(getClient(), id, 0);
    }
      
    public final void enterCS(boolean donate) {
        getClient().removeClickedNPC();
        NPCScriptInvoker.dispose(getClient());
        vm.openLegacyNpc = true;
        getPlayer().goDonateShop(donate);
        InterServerHandler.CashShopEnter(getClient(), getPlayer());
    }

    public final void timeMoveMap(final int destination, final int movemap, final int time) {
        warp(movemap, 0);
        getClient().getSession().write(MaplePacketCreator.getClock(time));
        Timer.CloneTimer tMan = Timer.CloneTimer.getInstance();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (getPlayer() != null) {
                    if (getPlayer().getMapId() == movemap) {
                        warpNoCheck(destination);
                    }
                }
            }
        };
        tMan.schedule(r, time * 1000);
    }

    public final void warpNoCheck(final int map) {
        final MapleMap mapz = getWarpMap(map);
        try {
            int overflow = 0;
            while (true) {
                MaplePortal p = mapz.getPortal(Randomizer.nextInt(mapz.getPortals().size()));
                if (p.getName().equals("sp")) {
                    getPlayer().changeMap(mapz, p);
                    break;
                }
                if (overflow >= 30) {
                    getPlayer().changeMap(mapz, mapz.getPortal(0));
                    break;
                }
                overflow++;
            }
        } catch (Exception e) {
            getPlayer().changeMap(mapz, mapz.getPortal(0));
        }
    }

    public final void warpNoCheck(final int map, final int portal) {
        final MapleMap mapz = getWarpMap(map);
        if ((portal != 0 && map == getPlayer().getMapId()) || map == -1) { //test
            final Point portalPos = new Point(getPlayer().getMap().getPortal(portal).getPosition());
            if (portalPos.distanceSq(getPlayer().getTruePosition()) < 90000.0 || map == -1) { //estimation
                getClient().getSession().write(MaplePacketCreator.instantMapWarp((byte) portal)); //until we get packet for far movement, this will do
                getPlayer().getMap().movePlayer(getPlayer(), portalPos);
            } else {
                getPlayer().changeMap(mapz, mapz.getPortal(portal));
            }
        } else {
            getPlayer().changeMap(mapz, mapz.getPortal(portal));
        }
    }

    public void sendPackage(String receiver, int itemId, int quantity, String sender, String msg) {
        int cid = MapleCharacterUtil.getIdByName(receiver);
        int channel = World.Find.findChannel(cid);
        if (channel >= 0) {
            World.Broadcast.sendPacket(cid, MaplePacketCreator.sendDuey((byte) 28, null, null));
            World.Broadcast.sendPacket(cid, MaplePacketCreator.serverNotice(5, "아이템이 지급되었습니다. NPC 택배원 <듀이> 에게서 아이템을 수령하세요!"));
        }
        DueyHandler.addNewItemToDb(itemId, quantity, cid, sender, msg, channel >= 0);
    }

    public void putKey(int key, int type, int action) {
        getPlayer().changeKeybinding(key, (byte) type, action);
        getClient().getSession().write(MaplePacketCreator.getKeymap(getPlayer().getKeyLayout()));
    }

    public void teachSkill(final int id, int level) {
        final Skill skil = SkillFactory.getSkill(id);
        getPlayer().changeSkillLevel(skil, level, (byte) skil.getMaxLevel());
    }

    public final EventManager getEventManager(final String event) {
        return getClient().getChannelServer().getEventSM().getEventManager(event);
    }

    public final EventInstanceManager getEventInstance() {
        return getPlayer().getEventInstance();
    }

    public boolean checkCooltime(int key) {
        long t = getPlayer().remainingCooltime(key, 0);
        return t > 0;
    }

    public String remainingCooltime(int key) {
        long t = getPlayer().remainingCooltime(key, 0);
        if (t == 0) {
            return "0";
        } else {
            return StringUtil.getReadableMillis(0, t);
        }
    }

    public boolean cooltime(int key, long time) {
        return getPlayer().remainingCooltime(key, time) == 0;
    }

    public void saveLocation(String loc) {
        getPlayer().saveLocation(SavedLocationType.fromString(loc));
    }

    public void saveReturnLocation(String loc) {
        getPlayer().saveLocation(SavedLocationType.fromString(loc), getMap().getReturnMap().getId());
    }

    public void clearSavedLocation(String loc) {
        getPlayer().clearSavedLocation(SavedLocationType.fromString(loc));
    }
}
