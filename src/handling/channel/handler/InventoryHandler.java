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
package handling.channel.handler;

import java.util.Map;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.awt.Point;

import client.inventory.Equip;
import client.inventory.Equip.ScrollResult;
import client.inventory.Item;
import client.Skill;
import client.inventory.ItemFlag;
import client.inventory.MaplePet;
import client.inventory.MaplePet.PetFlag;
import client.inventory.MapleMount;
import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.MapleDisease;
import client.MapleQuestStatus;
import client.inventory.MapleInventoryType;
import client.inventory.MapleInventory;
import client.MapleStat;
import client.MapleTrait.MapleTraitType;
import client.PlayerStats;
import client.SkillEntry;
import constants.GameConstants;
import client.SkillFactory;
import client.anticheat.CheatingOffense;
import constants.ServerConstants;
import database.DatabaseConnection;
import handling.SendPacketOpcode;
import handling.channel.ChannelServer;
import handling.world.MaplePartyCharacter;
import handling.world.World;
import java.awt.Rectangle;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import scripting.EtcScriptInvoker;
import server.Randomizer;
import server.RandomRewards;
import server.MapleShopFactory;
import server.MapleStatEffect;
import server.MapleItemInformationProvider;
import server.MapleInventoryManipulator;
import server.StructRewardItem;
import server.quest.MapleQuest;
import server.maps.SavedLocationType;
import server.maps.FieldLimitType;
import server.maps.MapleMap;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.life.MapleMonster;
import server.life.MapleLifeFactory;
import scripting.NPCScriptManager;
import server.ItemInformation;
import server.StructPotentialItem;
import server.events.MapleEvent;
import server.events.MapleEventType;
import server.log.LogType;
import server.log.ServerLogger;
import server.maps.MapleMessageBox;
import server.maps.MapleMist;
import server.shops.AbstractPlayerStore;
import server.shops.HiredMerchant;
import server.shops.IMaplePlayerShop;
import server.shops.MaplePlayerShop;
import server.shops.MinervaOwlSearchTop;
import tools.FileoutputUtil;
import tools.Pair;
import tools.packet.MTSCSPacket;
import tools.packet.PetPacket;
import tools.data.LittleEndianAccessor;
import tools.MaplePacketCreator;
import tools.data.MaplePacketLittleEndianWriter;
import tools.packet.CSPacket;
import tools.packet.PlayerShopPacket;
import tools.packet.UIPacket;

public class InventoryHandler {

    private static boolean suc = false;

    public static void ItemMove(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer().hasBlockedInventory()) { //hack
            return;
        }
        //2D 96 4F 9E 00 01 F5 FF 01 00 FF FF
        c.getPlayer().setScrolledPosition((short) 0);
        c.getPlayer().updateTick(slea.readInt());
        final MapleInventoryType type = MapleInventoryType.getByType(slea.readByte()); //04
        final short src = slea.readShort();                                            //01 00
        final short dst = slea.readShort();                                            //00 00
        final short quantity = slea.readShort();                                       //53 01
        if (c.getPlayer().cubeitemid > 0) {
            c.getPlayer().cubeitemid = 0;
        }

        if (src < 0 && dst > 0) {
            MapleInventoryManipulator.unequip(c, src, dst);
        } else if (dst < 0) {
            MapleInventoryManipulator.equip(c, src, dst);
        } else if (dst == 0) {
            MapleInventoryManipulator.drop(c, type, src, quantity);
        } else {
            if (dst > c.getPlayer().getInventory(type).getSlotLimit()) {
                c.getPlayer().dropMessage(1, "아이템을 황천으로 보내는 것은 불가능합니다.");
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            MapleInventoryManipulator.move(c, type, src, dst);
        }
    }

    public static final void SwitchBag(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer().hasBlockedInventory()) { //hack
            return;
        }
        c.getPlayer().setScrolledPosition((short) 0);
        c.getPlayer().updateTick(slea.readInt());
        //slea.skip(4);1229
        final short src = (short) slea.readInt();                                       //01 00
        final short dst = (short) slea.readInt();                                            //00 00
        if (src < 100 || dst < 100) {
            return;
        }
        MapleInventoryManipulator.move(c, MapleInventoryType.ETC, src, dst);
    }

    public static final void MoveBag(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer().hasBlockedInventory()) { //hack
            return;
        }
        c.getPlayer().setScrolledPosition((short) 0);
        //slea.skip(4);1229
        c.getPlayer().updateTick(slea.readInt());
        final boolean srcFirst = slea.readInt() > 0;
        short dst = (short) slea.readInt();                                       //01 00
        if (slea.readByte() != 4) { //must be etc) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        short src = slea.readShort();                                            //00 00
        MapleInventoryManipulator.move(c, MapleInventoryType.ETC, srcFirst ? dst : src, srcFirst ? src : dst);
    }

    public static final void ItemSort(final LittleEndianAccessor slea, final MapleClient c) {
        //slea.skip(4);
        c.getPlayer().updateTick(slea.readInt());
        c.getPlayer().setScrolledPosition((short) 0);
        final MapleInventoryType pInvType = MapleInventoryType.getByType(slea.readByte());
        if (pInvType == MapleInventoryType.UNDEFINED || c.getPlayer().hasBlockedInventory()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        final MapleInventory pInv = c.getPlayer().getInventory(pInvType); //Mode should correspond with MapleInventoryType
        boolean sorted = false;

        if (pInvType == MapleInventoryType.SETUP) {
            sorted = true;
        }

        while (!sorted) {
            final byte freeSlot = (byte) pInv.getNextFreeSlot();
            if (freeSlot != -1) {
                byte itemSlot = -1;
                for (byte i = (byte) (freeSlot + 1); i <= pInv.getSlotLimit(); i++) {
                    if (pInv.getItem(i) != null) {
                        itemSlot = i;
                        break;
                    }
                }
                if (itemSlot > 0) {
                    MapleInventoryManipulator.move(c, pInvType, itemSlot, freeSlot);
                } else {
                    sorted = true;
                }
            } else {
                sorted = true;
            }
        }
        c.getSession().write(MaplePacketCreator.finishedSort(pInvType.getType()));
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static final boolean UseTeleRock(LittleEndianAccessor slea, MapleClient c, int itemId) {
        if (c.getPlayer().getMapId() / 100000 == 1100 || c.getPlayer().getMapId() >= 190000000 && c.getPlayer().getMapId() <= 198000000) {
            c.getSession().write(MaplePacketCreator.enableActions());
            c.getPlayer().dropMessage(6, "여기선 돌 금지야! 금지!!");
            return true;
        }
        boolean used = false;
        if (itemId == 5041001 || itemId == 5040004) {
            // slea.readByte(); //useless //프리미엄 고성
        }
        if (itemId == 5041001) {
            itemId = 5041000;
        }
        int mmap = c.getPlayer().getMapId();
        if (mmap >= 190000000 && mmap <= 198000000) {
            c.getSession().write(MaplePacketCreator.serverNotice(1, "피시방 도둑을 드디어 찾은것 같습니다."));
            return true;
        }
        if (slea.readByte() == 0) { // Rocktype
            final MapleMap target = c.getChannelServer().getMapFactory().getMap(slea.readInt());
            if (target.getId() >= 190000000 && target.getId() <= 198000000) {
                c.getSession().write(MaplePacketCreator.serverNotice(1, "피시방 도둑을 드디어 찾은것 같습니다. 내 돈 돌려주세요....."));
                return true;
            }
            if (((itemId == 5041000 && c.getPlayer().isRockMap(target.getId())) || (itemId != 5041000 && c.getPlayer().isRegRockMap(target.getId()) && (target.getId() / 100000000) == (c.getPlayer().getMapId() / 100000000)) || ((itemId == 5040004 || itemId == 5041001) && (c.getPlayer().isHyperRockMap(target.getId()) || GameConstants.isHyperTeleMap(target.getId())))) && target.getId() >= 100000000) {
                if (!FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit()) && !FieldLimitType.VipRock.check(target.getFieldLimit()) && !c.getPlayer().isInBlockedMap()) { //Makes sure this map doesn't have a forced return map
                    if ((target.getId() == 110000000 || c.getPlayer().getMapId() == 110000000) && itemId == 5040000) {
                        int tmap = target.getId();
                        int retmap = c.getPlayer().getSavedLocation(SavedLocationType.FLORINA);
                        if (tmap / 100000000 != retmap / 100000000) {
                            c.getPlayer().changeMap(target, target.getPortal(0));
                            used = true;
                        }
                    } else {
                        c.getPlayer().changeMap(target, target.getPortal(0));
                        used = true;
                    }
                }
            }
        } else {
            final MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(slea.readMapleAsciiString());
            if (victim != null && !victim.isIntern() && c.getPlayer().getEventInstance() == null && victim.getEventInstance() == null) {
                if (!FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit()) && !FieldLimitType.VipRock.check(c.getChannelServer().getMapFactory().getMap(victim.getMapId()).getFieldLimit()) && !victim.isInBlockedMap() && !c.getPlayer().isInBlockedMap()) {
                    if ((itemId == 5041000 || itemId == 5040004 || itemId == 5041001 || (victim.getMapId() / 100000000) == (c.getPlayer().getMapId() / 100000000)) && victim.getMapId() >= 100000000) { // Viprock or same continent
                        if (itemId == 5040000) {
                            if (victim.getMapId() == 110000000 || c.getPlayer().getMapId() == 110000000) {
                                int tmap = victim.getMapId();
                                int retmap = c.getPlayer().getSavedLocation(SavedLocationType.FLORINA);
                                if (tmap / 100000000 != retmap / 100000000) {
                                    c.getPlayer().changeMap(victim.getMap(), victim.getMap().findClosestPortal(victim.getTruePosition()));
                                    used = true;
                                }
                            } else {
                                int tmap = victim.getMapId();
                                if (mmap >= 190000000 && mmap <= 198000000) {
                                    c.getSession().write(MaplePacketCreator.serverNotice(1, "피시방 도둑을 드디어 찾은것 같습니다."));
                                    return true;
                                }
                                if (tmap >= 190000000 && tmap <= 198000000) {
                                    c.getSession().write(MaplePacketCreator.serverNotice(1, "피시방비 얼마나 한다고 이걸......ㅡㅡ"));
                                    return true;
                                }
                                switch (tmap / 10000000) {
                                    case 20:
                                    case 21:
                                    case 23:
                                    case 22:
                                        if (tmap / 100000000 == mmap / 100000000 && (tmap / 10000000 == 20 || tmap / 10000000 == 21 || tmap / 10000000 == 22 || tmap / 10000000 == 23)) {
                                            c.getPlayer().changeMap(victim.getMap(), victim.getMap().findClosestPortal(victim.getTruePosition()));
                                            used = true;
                                        }
                                        break;
                                    case 24:
                                    case 25:
                                    case 26:
                                        if (tmap / 10000000 == mmap / 10000000) {
                                            c.getPlayer().changeMap(victim.getMap(), victim.getMap().findClosestPortal(victim.getTruePosition()));
                                            used = true;
                                        }
                                        break;
                                    default:
                                        c.getPlayer().changeMap(victim.getMap(), victim.getMap().findClosestPortal(victim.getTruePosition()));
                                        used = true;
                                        break;
                                }
                            }
                        } else {
                            if (victim.getMapId() >= 190000000 && victim.getMapId() <= 198000000) {
                                c.getSession().write(MaplePacketCreator.serverNotice(1, "피시방비 얼마나 한다고 이걸......ㅡㅡ\r\n 안돼!! 돌아가"));
                                return true;
                            }
                            c.getPlayer().changeMap(victim.getMap(), victim.getMap().findClosestPortal(victim.getTruePosition()));
                            used = true;
                        }
                    }
                }
            }
        }
        boolean bln = (used && itemId != 5041001 && itemId != 5040004);
        if (!bln) {
            c.getSession().write(MaplePacketCreator.serverNotice(1, "이동할 수 없습니다."));
        }
        return bln;
    }

    public static void useRemoteHiredMerchant(LittleEndianAccessor slea, MapleClient c) {
        short slot = slea.readShort();
        Item item = c.getPlayer().getInventory(MapleInventoryType.CASH).getItem(slot);
        if (item == null) {
            c.getSession().close(); //hack
            return;
        }
        if (item.getItemId() != 5470000 || item.getQuantity() <= 0) {
            c.getSession().close(); //hack
            return;
        }
        boolean use = false;

        HiredMerchant merchant = c.getChannelServer().findAndGetMerchant(c.getPlayer().getAccountID(), c.getPlayer().getId());

        if (merchant == null) {
            c.getPlayer().dropMessage(1, "현재 채널에서 열려있는 고용상점이 없습니다.");
            return;
        }

        if (FieldLimitType.ChannelSwitch.check(c.getPlayer().getMap().getFieldLimit())) {
            c.getPlayer().dropMessage(1, "이곳에서는 사용할 수 없습니다.");
            return;
        }

        MapleCharacter chr = c.getPlayer();

        if (merchant.isOwner(chr) && merchant.isOpen() && merchant.isAvailable()) {
            merchant.setOpen(false);
            merchant.broadcastToVisitors(MaplePacketCreator.serverNotice(1, "고용상점이 점검중에 있습니다. 나중에 다시 이용해 주세요."));
            merchant.removeAllVisitors((byte) 16, (byte) -2);
            chr.setPlayerShop(merchant);
            c.getSession().write(PlayerShopPacket.getHiredMerch(chr, merchant, false));
            use = true;
        }
    }

    public static final void ItemGather(final LittleEndianAccessor slea, final MapleClient c) {
        // [41 00] [E5 1D 55 00] [01]
        // [32 00] [01] [01] // Sent after
        c.getPlayer().updateTick(slea.readInt());
        c.getPlayer().setScrolledPosition((short) 0);
        if (c.getPlayer().hasBlockedInventory()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        final byte mode = slea.readByte();
        final MapleInventoryType invType = MapleInventoryType.getByType(mode);
        MapleInventory Inv = c.getPlayer().getInventory(invType);
        MaplePet pet1 = c.getPlayer().getPet(0);
        MaplePet pet2 = c.getPlayer().getPet(1);
        MaplePet pet3 = c.getPlayer().getPet(2);
        if (mode == 5 && (pet1 != null || pet2 != null || pet3 != null)) { //임시방편
            c.getSession().write(MaplePacketCreator.serverNotice(1, "모든 펫을 장착 해제후 시도해 주세요"));
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        //c.getPlayer().dropMessage(6, "타입 1");
        final List<Item> itemMap = new LinkedList<Item>();
        for (Item item : Inv.list()) {
            itemMap.add(item.copy()); // clone all  items T___T.
        }
        for (Item itemStats : itemMap) {
            MapleInventoryManipulator.removeFromSlot(c, invType, itemStats.getPosition(), itemStats.getQuantity(), true, false);
        }

        final List<Item> sortedItems = sortItems(itemMap);
        for (Item item : sortedItems) {
            MapleInventoryManipulator.addFromDrop(c, item, false, 0);
        }
        c.getSession().write(MaplePacketCreator.finishedGather(mode));
        c.getSession().write(MaplePacketCreator.enableActions());
        itemMap.clear();
        sortedItems.clear();
    }

    private static final List<Item> sortItems(final List<Item> passedMap) {
        final List<Integer> itemIds = new ArrayList<Integer>(); // empty list.
        for (Item item : passedMap) {
            itemIds.add(item.getItemId()); // adds all item ids to the empty list to be sorted.
        }
        Collections.sort(itemIds); // sorts item ids

        final List<Item> sortedList = new LinkedList<Item>(); // ordered list pl0x <3.

        for (Integer val : itemIds) {
            List<Item> copy = new ArrayList<>(passedMap);
            for (Item item : copy) {
                if (val == item.getItemId()) { // Goes through every index and finds the first value that matches
                    sortedList.add(item);
                    passedMap.remove(item);
                    break;
                }
            }
        }
        return sortedList;
    }

    public static final void UseGoldHammer(final LittleEndianAccessor slea, final MapleClient c) {
        boolean used = false;

        final byte slot = (byte) slea.readShort();
        slea.skip(2);
        final int itemId = slea.readInt();
        slea.readInt(); // Inventory type, Hammered eq is always EQ.
        Item toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        final Equip item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) slea.readInt());
        if (toUse == null || toUse.getItemId() != itemId || toUse.getQuantity() < 1) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (item != null) {
            if (GameConstants.canHammer(item.getItemId()) && MapleItemInformationProvider.getInstance().getSlots(item.getItemId()) > 0 && item.getViciousHammer() < 2) {
                switch (itemId) {
                    case 2470000:
                        item.setViciousHammer((byte) (item.getViciousHammer() + 1));
                        item.setUpgradeSlots((byte) (item.getUpgradeSlots() + 1));
                        c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIP);
                        used = true;
                        suc = true;
                        break;
                    case 2470001:
                        if (Randomizer.nextInt(100) < 50) {
                            item.setViciousHammer((byte) (item.getViciousHammer() + 1));
                            item.setUpgradeSlots((byte) (item.getUpgradeSlots() + 1));
                            c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIP);
                            used = true;
                            suc = true;
                        } else {
                            item.setViciousHammer((byte) (item.getViciousHammer() + 1));
                            c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIP);
                            used = true;
                            c.getPlayer().dropMessage(5, "황금 망치 실패");
                            suc = false;
                        }
                        break;
                }
                c.getSession().write(MTSCSPacket.ViciousHammer(true, suc));
            } else {
                c.getPlayer().dropMessage(5, "황금망치로 제련할 수 없는 아이템 입니다.");
                suc = false;
                c.getSession().write(MTSCSPacket.ViciousHammer(false, suc));
            }
        }
        if (used) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false, true);
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static final void UsedGoldHammer(final LittleEndianAccessor slea, final MapleClient c) {
        slea.skip(8);
        c.getSession().write(MTSCSPacket.ViciousHammer(false, suc));
    }

    public static final boolean UseRewardItem(final byte slot, final int itemId, final MapleClient c, final MapleCharacter chr) {
        final Item toUse = c.getPlayer().getInventory(GameConstants.getInventoryType(itemId)).getItem(slot);
        c.getSession().write(MaplePacketCreator.enableActions());
        if (toUse != null && toUse.getQuantity() >= 1 && toUse.getItemId() == itemId && !chr.hasBlockedInventory()) {
            if (chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot() > -1 && chr.getInventory(MapleInventoryType.USE).getNextFreeSlot() > -1 && chr.getInventory(MapleInventoryType.SETUP).getNextFreeSlot() > -1 && chr.getInventory(MapleInventoryType.ETC).getNextFreeSlot() > -1) {
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                final Pair<Integer, List<StructRewardItem>> rewards = ii.getRewardItem(itemId);

                if (rewards != null && rewards.getLeft() > 0) {
                    while (true) {
                        for (StructRewardItem reward : rewards.getRight()) {
                            if (reward.prob > 0 && Randomizer.nextInt(rewards.getLeft()) < reward.prob) { // Total prob
                                if (GameConstants.getInventoryType(reward.itemid) == MapleInventoryType.EQUIP) {
                                    final Item item = ii.getEquipById(reward.itemid);
                                    if (reward.period > 0) {
                                        item.setExpiration(System.currentTimeMillis() + (reward.period * 60 * 60 * 10));
                                    }
                                    item.setGMLog("Reward item: " + itemId + " on " + FileoutputUtil.CurrentReadable_Date());
                                    MapleInventoryManipulator.addbyItem(c, item);
                                } else {
                                    MapleInventoryManipulator.addById(c, reward.itemid, reward.quantity, "Reward item: " + itemId + " on " + FileoutputUtil.CurrentReadable_Date());
                                }
                                MapleInventoryManipulator.removeById(c, GameConstants.getInventoryType(itemId), itemId, 1, false, false);

                                c.getSession().write(MaplePacketCreator.showRewardItemAnimation(reward.itemid, reward.effect));
                                chr.getMap().broadcastMessage(chr, MaplePacketCreator.showRewardItemAnimation(reward.itemid, reward.effect, chr.getId()), false);
                                chr.getClient().getSession().write(MaplePacketCreator.getShowItemGain(reward.itemid, reward.quantity, true));
                                return true;
                            }
                        }
                    }
                } else {
                    chr.dropMessage(6, "인벤토리가 부족합니다.");
                }
            } else {
                chr.dropMessage(6, "인벤토리가 부족합니다.");
            }
        }
        return false;
    }

    public static final void UseItem(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || !chr.isAlive() || chr.getMapId() == 749040100 || chr.getMap() == null || chr.hasBlockedInventory()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }

        c.getPlayer().updateTick(slea.readInt());
        final byte slot = (byte) slea.readShort();
        final int itemId = slea.readInt();
        final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);
        switch (itemId) {
            case 2430100:
            //case 2430101:  ///수정했음 오류나면 주석풀기
            case 2430102:
                break;
            /*    case 2430014:
             if (c.getPlayer().getMapId() != 106020300) {
             c.getPlayer().dropMessage(5, "이곳에서는 사용하실 수 없습니다.");
             break;
             }
             c.getPlayer().getQuestNAdd(MapleQuest.getInstance(2015421)).setCustomData("1");
             c.getPlayer().dropMessage(5, "킬라 버섯 포자를 이용하여 앞 길을 가로 막고 있는 결계를 제거하였다.");
             MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
             break;*/
            case 2430143: //정체불명의 러브레터
                int prop = Randomizer.rand(0, 100);
                int count = 0;
                if (prop <= 20) {
                    count = 1;
                } else if (prop <= 40) {
                    count = 2;
                } else if (prop <= 60) {
                    count = 3;
                } else if (prop <= 80) {
                    count = 4;
                } else {
                    count = 5;
                }

                c.getSession().write(MaplePacketCreator.getShowFameGain(count));
                c.getPlayer().addFame(count);
                c.getPlayer().updateSingleStat(MapleStat.FAME, c.getPlayer().getFame());
                c.getPlayer().dropMessage(5, "편지 속에 담긴 사랑의 힘으로 인기도가 올라갔습니다. 하지만 누가 보낸 편지인지 알 수 없군요.");
                break;

            case 2430007: // Blank Compass
            {
                final MapleInventory inventory = chr.getInventory(MapleInventoryType.SETUP);

                if (inventory.countById(3994102) >= 1 // Compass Letter "North"
                        && inventory.countById(3994103) >= 1 // Compass Letter "South"
                        && inventory.countById(3994104) >= 1 // Compass Letter "East"
                        && inventory.countById(3994105) >= 1) { // Compass Letter "West"
                    MapleInventoryManipulator.addById(c, 2430008, (short) 1, "Scripted item: " + itemId + " on " + FileoutputUtil.CurrentReadable_Date()); // Gold Compass
                    MapleInventoryManipulator.removeById(c, MapleInventoryType.SETUP, 3994102, 1, false, false);
                    MapleInventoryManipulator.removeById(c, MapleInventoryType.SETUP, 3994103, 1, false, false);
                    MapleInventoryManipulator.removeById(c, MapleInventoryType.SETUP, 3994104, 1, false, false);
                    MapleInventoryManipulator.removeById(c, MapleInventoryType.SETUP, 3994105, 1, false, false);
                    c.getPlayer().dropMessage(5, "황금 나침반을 제작하였습니다. 더블클릭하여 <골드리치 보물섬>으로 떠나보세요!");
                } else {
                    c.getPlayer().dropMessage(1, "나침반용 알파벳 N,W,E,S를 1개씩 모으면 황금 나침반을 제작할 수 있습니다.");
                    MapleInventoryManipulator.addById(c, 2430007, (short) 1, "Scripted item: " + itemId + " on " + FileoutputUtil.CurrentReadable_Date()); // Blank Compass
                }
                    //NPCScriptManager.getInstance().start(c, 2084001);
                //c.getPlayer().dropMessage(5, "나침반용 알파벳 N,W,E,S를 1개씩 모으면 황금 나침반을 제작할 수 있습니다.");                    
                break;
            }
            case 2433017: {
                List<Pair<Integer, Short>> items = new ArrayList<>();
                int[] id = {2000000, 2000001, 2000003}; // 아이템코드
                int[] q = {1, 2, 5}; // 갯수
                for (int i = 0; i < id.length; i++) {
                    items.add(new Pair<>(id[i], (short) q[i]));
                }
                byte num = (byte) Randomizer.rand(0, items.size() - 1);
                chr.gainItem(items.get(num).getLeft(), items.get(num).getRight());
                break;
            }
            case 2430031: {
                c.getSession().write(UIPacket.AranTutInstructionalBalloon("Effect/OnUserEff.img/itemEffect/polaloid/2430031"));
                break;
            }
            case 2430032: {
                if (chr.getMapId() != 922030011 || chr.getTruePosition().x < 120) {
                    chr.dropMessage(5, "이곳에서는 사용할 수 없습니다.");
                    break;
                }
                chr.getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9300388), new Point(chr.getTruePosition().x, 132));
                break;
            }
            case 2430071: {
                if (!c.getPlayer().canHold(4032616, 1)) {
                    c.getPlayer().dropMessage(1, "기타 인벤토리 공간이 부족합니다.");
                    break;
                }
                if (c.getPlayer().haveItem(4032616, 1)) {
                    c.getPlayer().dropMessage(1, "이미 혜안을 소지하고 있습니다.");
                    break;
                }
                c.getSession().write(UIPacket.AranTutInstructionalBalloon("Effect/OnUserEff.img/itemEffect/quest/2430071"));
                if ((int) (Math.random() * 5) == 0) {
                    chr.dropMessage(5, "탁한 유리구슬이 부서졌습니다. 혜안을 얻었습니다.");
                    c.getPlayer().gainItem(4032616, (short) 1, true);
                } else {
                    chr.dropMessage(5, "탁한 유리구슬이 부서졌습니다. 아무것도 나오지 않았습니다.");
                }
                break;
            }
            case 2430014: {
                c.getPlayer().openNpc(1300010);
                break;
            }
            /*case 2434568: {
             c.getPlayer().openNpc(9000546);
             break;
             }*/
            case 2434568: {
                if (c.getPlayer().getMapId() != 701210161) {
                    c.getPlayer().dropMessage(5, "이곳에서는 사용하실 수 없습니다.");
                    break;
                }
                if (c.getPlayer().getPosition().x < -75 || c.getPlayer().getPosition().x > 92) {
                    c.getPlayer().dropMessage(1, "악령 퇴치제를 사용 할 수 없습니다.");
                    break;
                }
                NPCScriptManager.getInstance().start(c, 2007, "DemonBGone");
                break;
            }
            case 2430015: {
                if (c.getPlayer().getMapId() != 106020500) {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    c.getSession().write(MaplePacketCreator.getNPCTalk(1300011, (byte) 0, "이곳에선 사용할 수 없습니다.", "00 00", (byte) 3));
                    break;
                } else if (c.getPlayer().getPosition().x < 180) {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    c.getSession().write(MaplePacketCreator.getNPCTalk(1300011, (byte) 0, "조금 더 가까이 가서 사용하자.", "00 00", (byte) 3));
                    break;
                }
                c.getPlayer().openNpc(1300011);
                break;
            }
            case 2430159: {
                if (c.getPlayer().getMapId() == 211060400) {
                    c.getPlayer().openNpc(2161004);
                } else {
                    c.getPlayer().dropMessage(5, "이곳에선 사용할 수 없다. 머트가 있는 곳으로 가자.");
                }
                break;
            }
            case 2430180: {
                if (c.getPlayer().getMapId() == 211041400) {
                    if (c.getPlayer().getQuestStatus(3192) == 1) {
                        MapleQuest.getInstance(3192).forceStart(c.getPlayer(), 0, "1");
                    }
                    c.getPlayer().dropMessage(5, "결계의 토템이 밝은 빛을 내며 화로가 타기 시작했고, 토템에 새겨진 알케스터의 결계 마법이 발동합니다.");
                    c.getSession().write(MaplePacketCreator.getShowItemGain(itemId, (byte) -1, true));
                } else {
                    c.getPlayer().dropMessage(5, "이곳에선 사용할 수 없다.");
                }
                break;
            }
            case 2431008: { //불가사의 레시피
                final int itemid = Randomizer.nextInt(143) + 2510000;//
                if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {//인벤 필요 요구 수치
                    MapleInventoryManipulator.addById(c, itemid, (short) 1, "Reward item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                } else {
                    c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                }
                break;
            }
            case 2431007: { //불가사의 주문서
                final int itemid = Randomizer.nextInt(14) + 2046200;//
                if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {//인벤 필요 요구 수치
                    MapleInventoryManipulator.addById(c, itemid, (short) 1, "Reward item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                } else {
                    c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                }
                break;
            }
            case 2430008: // Gold Compass
            {
                chr.saveLocation(SavedLocationType.RICHIE);
                MapleMap map;
                boolean warped = false;

                for (int i = 390001000; i <= 390001004; i++) {
                    map = c.getChannelServer().getMapFactory().getMap(i);

                    if (map.getCharactersSize() == 0) {
                        chr.changeMap(map, map.getPortal(0));
                        warped = true;
                        break;
                    }
                }
                if (warped) { // Removal of gold compass
                } else { // Or mabe some other message.
                    c.getPlayer().dropMessage(5, "모든 맵에 유저가 존재합니다. 잠시 후 다시 시도해주세요.");
                }
                break;
            }
            case 2439998: // 백간랑하 스킵티켓
            {
                MapleMap map;
                boolean warped = false;

                for (int i = 800040400; i <= 800040400; i++) {
                    map = c.getChannelServer().getMapFactory().getMap(i);

                    if (map.getCharactersSize() == 0) {
                        chr.changeMap(map, map.getPortal(0));
                        warped = true;
                        break;
                    }
                }
                if (warped) { // Removal of gold compass
                } else { // Or mabe some other message.
                    c.getPlayer().dropMessage(5, "천수각 1층에 다른 유저가 존재합니다. 잠시 후 다시 시도해주세요.");
                }
                break;
            }
            case 2431003: //마일리지50
                c.getPlayer().modifyCSPoints(3, 50, false);//캐시
                c.getPlayer().dropMessage(5, 50 + " 마일리지가 적립 되었습니다.");
                break;
            case 2431004: //마일리지100
                c.getPlayer().modifyCSPoints(3, 100, false);
                c.getPlayer().dropMessage(5, 100 + " 마일리지가 적립 되었습니다.");
                break;
            case 2431005: //마일리지250
                c.getPlayer().modifyCSPoints(3, 250, false);
                c.getPlayer().dropMessage(5, 250 + " 마일리지가 적립 되었습니다.");
                break;
            case 2431006: //마일리지500
                c.getPlayer().modifyCSPoints(3, 500, false);
                c.getPlayer().dropMessage(5, 500 + " 마일리지가 적립 되었습니다.");
                break;
            case 2431009: //마일리지1000
                c.getPlayer().modifyCSPoints(3, 1000, false);
                c.getPlayer().dropMessage(5, 1000 + " 마일리지가 적립 되었습니다.");
                break;
            case 2431010: //마일리지2000
                c.getPlayer().modifyCSPoints(3, 2000, false);
                c.getPlayer().dropMessage(5, 2000 + " 마일리지가 적립 되었습니다.");
                break;
            case 2431011: //마일리지3000
                c.getPlayer().modifyCSPoints(3, 3000, false);
                c.getPlayer().dropMessage(5, 3000 + " 마일리지가 적립 되었습니다.");
                break;
            case 2431012: //마일리지5000
                c.getPlayer().modifyCSPoints(3, 5000, false);
                c.getPlayer().dropMessage(5, 5000 + " 마일리지가 적립 되었습니다.");
                break;
            case 2432000: //캐시1000
                c.getPlayer().modifyCSPoints(1, 1000, false);
                c.getPlayer().dropMessage(5, 1000 + " 캐시가 적립 되었습니다.");
                break;
            case 2432001: //캐시5000
                c.getPlayer().modifyCSPoints(1, 5000, false);
                c.getPlayer().dropMessage(5, 5000 + " 캐시가 적립 되었습니다.");
                break;
            case 2432002: //캐시10000
                c.getPlayer().modifyCSPoints(1, 10000, false);
                c.getPlayer().dropMessage(5, 10000 + " 캐시가 적립 되었습니다.");
                break;
            case 2432003: //캐시50000
                c.getPlayer().modifyCSPoints(1, 50000, false);
                c.getPlayer().dropMessage(5, 50000 + " 캐시가 적립 되었습니다.");
                break;   //2022505  
            case 2432004: //캐시100000
                c.getPlayer().modifyCSPoints(1, 100000, false);
                c.getPlayer().dropMessage(5, 100000 + " 캐시가 적립 되었습니다.");
                break;
            case 2432005: //캐시200000
                c.getPlayer().modifyCSPoints(1, 200000, false);
                c.getPlayer().dropMessage(5, 200000 + " 캐시가 적립 되었습니다.");
                break;
            case 2432006: //캐시300000
                c.getPlayer().modifyCSPoints(1, 300000, false);
                c.getPlayer().dropMessage(5, 300000 + " 캐시가 적립 되었습니다.");
                break;
            case 2432007: //캐시500000
                c.getPlayer().modifyCSPoints(1, 500000, false);
                c.getPlayer().dropMessage(5, 500000 + " 캐시가 적립 되었습니다.");
                break;
            case 2432008: //캐시1000000
                c.getPlayer().modifyCSPoints(1, 1000000, false);
                c.getPlayer().dropMessage(5, 1000000 + " 캐시가 적립 되었습니다.");
                break;
            case 2431013: //메이플 포인트1000
                c.getPlayer().modifyCSPoints(2, 1000, false);
                c.getPlayer().dropMessage(5, 1000 + " 메이플포인트가 적립 되었습니다.");
                break;
            case 2431000: //메이플 포인트5000
                c.getPlayer().modifyCSPoints(2, 5000, false);//메포
                c.getPlayer().dropMessage(5, 5000 + " 메이플포인트가 적립 되었습니다.");
                break;
            case 2431001: //메이플 포인트10000
                c.getPlayer().modifyCSPoints(2, 10000, false);
                c.getPlayer().dropMessage(5, 10000 + " 메이플포인트가 적립 되었습니다.");
                break;
            case 2431002: //메이플 포인트30000
                c.getPlayer().modifyCSPoints(2, 30000, false);
                c.getPlayer().dropMessage(5, 30000 + " 메이플포인트가 적립 되었습니다.");
                break;
            case 2434004: //캐시500만
                c.getPlayer().modifyCSPoints(1, 5000000, false);
                c.getPlayer().dropMessage(5, 5000000 + " 캐시가 적립 되었습니다.");
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                c.getSession().write(MaplePacketCreator.enableActions());
                break;
            case 2434000: //메이플 포인트50만
                c.getPlayer().modifyCSPoints(2, 500000, false);
                c.getPlayer().dropMessage(5, 500000 + " 메이플포인트가 적립 되었습니다.");
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                c.getSession().write(MaplePacketCreator.enableActions());
                break;
            case 2434001: //메이플 포인트100만
                c.getPlayer().modifyCSPoints(2, 1000000, false);
                c.getPlayer().dropMessage(5, 1000000 + " 메이플포인트가 적립 되었습니다.");
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                c.getSession().write(MaplePacketCreator.enableActions());
                break;
            case 2434002: //메이플 포인트500만
                c.getPlayer().modifyCSPoints(2, 5000000, false);
                c.getPlayer().dropMessage(5, 5000000 + " 메이플포인트가 적립 되었습니다.");
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                c.getSession().write(MaplePacketCreator.enableActions());
                break;
            case 2430190: //금빛 각인의 인장
                NPCScriptManager.getInstance().start(c, 9031005);
                break;
            case 2430192: //AP초기화
                NPCScriptManager.getInstance().start(c, 9031003);
                break;
            case 2430193: //로얄스타일
                NPCScriptManager.getInstance().start(c, 9000172);
                break;
            case 2439988: //VIP 순간이동의 돌
                NPCScriptManager.getInstance().start(c, 1082100);
                break;
                //case 2430016: //아이스박스
            //NPCScriptManager.getInstance().start(c, 9000021);
            //break;
            case 2430191: //프로텍트 쉴드
                break;
            case 2430114: //플래티넘 미라클 큐브 조각
                if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {//인벤 필요 요구 수치
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430114) >= 30) {//필요한 아이템
                        if (MapleInventoryManipulator.checkSpace(c, 2049300, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 30, true, false)) {//얻을템/뺏을템
                            MapleInventoryManipulator.addById(c, 2049300, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());//얻을템
                            c.getPlayer().dropMessage(5, "플래티넘 미라클 큐브 조각을 사용 해 고급 장비강화 주문서 1개를 획득 했습니다.");
                        } else {
                            c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "플래티넘 미라클 큐브 조각 30개를 고급 장비강화 주문서 1개로 교환 가능합니다.");
                    }
                }
                break;
            case 2430187: //플래티넘 미라클 큐브 교환권
                //이게 범인이네 
                if (c.getPlayer().getInventory(MapleInventoryType.CASH).getNumFreeSlot() >= 1) {
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430187) >= 1) {
                        if (MapleInventoryManipulator.checkSpace(c, 5062003, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false)) {
                            MapleInventoryManipulator.addById(c, 5062003, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                            c.getPlayer().dropMessage(5, "플래티넘 미라클 큐브 교환권을 사용 해 플래티넘 미라클 큐브 1개를 획득 했습니다.");
                        } else {
                            c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "큐브 교환권은 사용 시 큐브 1개로 교환이 가능합니다.");
                    }
                }
                break;
            case 2430188: //플래티넘 미라클 큐브 10개 교환권
                if (c.getPlayer().getInventory(MapleInventoryType.CASH).getNumFreeSlot() >= 1) {
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430188) >= 1) {
                        if (MapleInventoryManipulator.checkSpace(c, 5062003, 10, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false)) {
                            MapleInventoryManipulator.addById(c, 5062003, (short) 10, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                            c.getPlayer().dropMessage(5, "플래티넘 미라클 큐브 10개 교환권을 사용 해 플래티넘 미라클 큐브 10개를 획득 했습니다.");
                        } else {
                            c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "플래티넘 미라클 큐브 10개 교환권은 사용 시 큐브 10개로 교환이 가능합니다.");
                    }
                }
                break;
            case 2430189: //플래티넘 미라클 큐브 20개 교환권
                if (c.getPlayer().getInventory(MapleInventoryType.CASH).getNumFreeSlot() >= 1) {
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430189) >= 1) {
                        if (MapleInventoryManipulator.checkSpace(c, 5062003, 20, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false)) {
                            MapleInventoryManipulator.addById(c, 5062003, (short) 20, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                            c.getPlayer().dropMessage(5, "플래티넘 미라클 큐브 20개 교환권을 사용 해 플래티넘 미라클 큐브 20개를 획득 했습니다.");
                        } else {
                            c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "플래티넘 미라클 큐브 20개 교환권은 사용 시 큐브 10개로 교환이 가능합니다.");
                    }
                }
                break;
            case 2430113: //마스터 미라클 큐브 조각
                if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {//인벤 필요 요구 수치
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430113) >= 30) {//필요한 아이템
                        if (MapleInventoryManipulator.checkSpace(c, 2049300, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 30, true, false)) {//얻을템/뺏을템
                            MapleInventoryManipulator.addById(c, 2049300, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());//얻을템
                            c.getPlayer().dropMessage(5, "마스터 미라클 큐브 조각을 사용 해 고급 장비강화 주문서 1개를 획득 했습니다.");
                        } else {
                            c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "마스터 미라클 큐브 조각 30개를 고급 장비강화 주문서 1개로 교환 가능합니다.");
                    }
                }
                break;
            case 2430183: //마스터 미라클 큐브 교환권
                if (c.getPlayer().getInventory(MapleInventoryType.CASH).getNumFreeSlot() >= 1) {
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430183) >= 1) {
                        if (MapleInventoryManipulator.checkSpace(c, 5062001, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false)) {
                            MapleInventoryManipulator.addById(c, 5062001, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                            c.getPlayer().dropMessage(5, "마스터 미라클 큐브 교환권을 사용 해 마스터 미라클 큐브 1개를 획득 했습니다.");
                        } else {
                            c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "큐브 교환권은 사용 시 큐브 1개로 교환이 가능합니다.");
                    }
                }
                break;
            case 2430186: //마스터 미라클 큐브 10개 교환권
                if (c.getPlayer().getInventory(MapleInventoryType.CASH).getNumFreeSlot() >= 1) {
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430186) >= 1) {
                        if (MapleInventoryManipulator.checkSpace(c, 5062001, 10, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false)) {
                            MapleInventoryManipulator.addById(c, 5062001, (short) 10, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                            c.getPlayer().dropMessage(5, "마스터 미라클 큐브 10개 교환권을 사용 해 마스터 미라클 큐브 10개를 획득 했습니다.");
                        } else {
                            c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "마스터 미라클 큐브 10개 교환권은 사용 시 큐브 10개로 교환이 가능합니다.");
                    }
                }
                break;
            case 2430112: //레드 큐브 조각
                //c.getPlayer().dropMessage(5, "미라클 큐브 조각 5개로 잠재능력 부여 주문서, 10개로 고급 잠재능력 부여 주문서 교환이 가능합니다.");
                NPCScriptManager.getInstance().start(c, 9000030);
                break;
            case 2430111: //미라클 큐브 조각
                if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 2 && c.getPlayer().getInventory(MapleInventoryType.CASH).getNumFreeSlot() >= 2) {
                    NPCScriptManager.getInstance().start(c, 9010019, "miracle_box");
                } else {
                    c.getPlayer().dropMessage(5, "아이템창의 빈공간 부족.");
                }
                break;
            case 2430182: //레드 큐브 교환권
                if (c.getPlayer().getInventory(MapleInventoryType.CASH).getNumFreeSlot() >= 1) {
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430182) >= 1) {
                        if (MapleInventoryManipulator.checkSpace(c, 5062000, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false)) {
                            MapleInventoryManipulator.addById(c, 5062000, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                            c.getPlayer().dropMessage(5, "레드 큐브 교환권을 사용 해 레드 큐브 1개를 획득 했습니다.");
                        } else {
                            c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "큐브 교환권은 사용 시 큐브 1개로 교환이 가능합니다.");
                    }
                }
                break;
            case 2430185: //레드 큐브 10개 교환권
                if (c.getPlayer().getInventory(MapleInventoryType.CASH).getNumFreeSlot() >= 1) {
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430185) >= 1) {
                        if (MapleInventoryManipulator.checkSpace(c, 5062000, 10, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false)) {
                            MapleInventoryManipulator.addById(c, 5062000, (short) 10, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                            c.getPlayer().dropMessage(5, "레드 큐브 10개 교환권을 사용 해 레드 큐브 10개를 획득 했습니다.");
                        } else {
                            c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "레드 큐브 10개 교환권은 사용 시 큐브 10개로 교환이 가능합니다.");
                    }
                }
                break;
            case 2430181: //미라클 큐브 교환권
                if (c.getPlayer().getInventory(MapleInventoryType.CASH).getNumFreeSlot() >= 1) {
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430181) >= 1) {
                        if (MapleInventoryManipulator.checkSpace(c, 5062002, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false)) {
                            MapleInventoryManipulator.addById(c, 5062002, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                            c.getPlayer().dropMessage(5, "미라클 큐브 교환권을 사용 해 미라클 큐브 1개를 획득 했습니다.");
                        } else {
                            c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "큐브 교환권은 사용 시 큐브 1개로 교환이 가능합니다.");
                    }
                }
                break;
            case 2430184: //미라클 큐브 10개 교환권
                if (c.getPlayer().getInventory(MapleInventoryType.CASH).getNumFreeSlot() >= 1) {
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430184) >= 1) {
                        if (MapleInventoryManipulator.checkSpace(c, 5062002, 10, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false)) {
                            MapleInventoryManipulator.addById(c, 5062002, (short) 10, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                            c.getPlayer().dropMessage(5, "미라클 큐브 10개 교환권을 사용 해 미라클 큐브 10개를 획득 했습니다.");
                        } else {
                            c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "미라클 큐브 10개 교환권은 사용 시 큐브 10개로 교환이 가능합니다.");
                    }
                }
                break;
        }
        boolean oneCube = false;
        switch (itemId) {
            case 2000024:
            case 2000025:
            case 2000026:
            case 2000027:
            case 2000028:
            case 2000029:
            case 2000030:
                oneCube = true;
                break;
        }
        final long time = System.currentTimeMillis();
        if (!oneCube && chr.getMap().getConsumeItemCoolTime() <= 0 && chr.getNextConsume() > time) {
            chr.dropMessage(5, "아직 아이템을 사용할 수 없습니다.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }

        if (slot < 0 || slot > c.getPlayer().getInventory(GameConstants.getInventoryType(itemId)).getSlotLimit() || itemId > 3000000) {
            c.getPlayer().dropMessage(1, "오류가 발생하였습니다. : - 7");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (itemId == 2022337) { // 마법제련술사의 약
            chr.addHP(-chr.getStat().getMaxHp());
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (itemId == 2000024
                || itemId == 2000025
                || itemId == 2000026
                || itemId == 2000027
                || itemId == 2000028
                || itemId == 2000029
                || itemId == 2000030) { //소비템 여기에도 추가하면된다
            if (chr.cubeitemid < 0) { // 최초 장비 셋팅

                c.getSession().write(MaplePacketCreator.enableActions());
                c.getPlayer().getClient().removeClickedNPC();
                NPCScriptManager.getInstance().start(c, 9010019, "miracle");

                return;
            }
            if (chr.cubeitemid == 0) { // 변경 장비 셋팅

                c.getSession().write(MaplePacketCreator.enableActions());
                c.getPlayer().getClient().removeClickedNPC();
                NPCScriptManager.getInstance().start(c, 9010019, "miracle");
                return;
            }
            boolean use = false;
            int cubeid = 0;

            if (itemId == 2000024) { //[원클릭]레드큐브  / 아이템 코드
                cubeid = 5062000; // 레드큐브
                if (MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, cubeid, 1, false, false)) {
                    use = true;
                }
            }
            if (itemId == 2000025) { //[원클릭]마스터 미라클 큐브 / 아이템 코드
                cubeid = 5062001; //마스터 미라클 큐브
                if (MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, cubeid, 1, false, false)) {
                    use = true;
                }
            }
            if (itemId == 2000026) { //[원클릭] 미라클 큐브 / 아이템 코드
                cubeid = 5062002; // 미라클 큐브
                if (MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, cubeid, 1, false, false)) {
                    use = true;
                }
            }
            if (itemId == 2000027) { //[원클릭] 플래티넘 미라클 큐브 / 아이템 코드
                cubeid = 5062003; // 플래티넘 미라클 큐브
                if (MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, cubeid, 1, false, false)) {
                    use = true;
                }
            }
            if (itemId == 2000028) { //첫 번째 우연의 큐브 /아이템 코드 
                cubeid = 5062004; // 첫 번째 우연의 큐브
                if (MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, cubeid, 1, false, false)) {
                    use = true;
                }
            }
            if (itemId == 2000029) { //두 번째 우연의 큐브 /아이템 코드 
                cubeid = 5062005; // 두 번째 우연의 큐브
                if (MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, cubeid, 1, false, false)) {
                    use = true;
                }
            }
            if (itemId == 2000030) { //세 번째 우연의 큐브 /아이템 코드 
                cubeid = 5062006; // 세 번째 우연의 큐브
                if (MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, cubeid, 1, false, false)) {
                    use = true;
                }
            }
            if (use && (cubeid >= 5062000 && cubeid <= 5062003)) {
                final Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) 1);
                if (item == null) {
                    c.getPlayer().dropMessage(5, "설정된 아이템이 없어 재설정할 수 없습니다.");
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }

                if (chr.cubeitemid > 1 && chr.cubeitemid != item.getItemId()) { //설정된 값이 안맞으면 
                    c.getPlayer().dropMessage(5, "설정된 아이템이 변경되어 재설정할 수 없습니다.");
                    chr.cubeitemid = 0; // 재설정으로 보냄
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
                if (item != null && c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                    final Equip eq = (Equip) item;
                    if (chr.cubeitemid == 1) { // 확인 값 1로 변경시
                        chr.cubeitemid = eq.getItemId();
                    }
                    int piece = 0;
                    if (eq.getState() >= 5) {
                        final List<List<StructPotentialItem>> pots = new LinkedList<>(MapleItemInformationProvider.getInstance().getAllPotentialInfo().values());

                        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                        final Map<String, Integer> eqstats = ii.getEquipStats(eq.getItemId());
                        if (!eqstats.containsKey("tuc") || eqstats.containsKey("tuc") && eqstats.get("tuc") == 0) {
                            c.getPlayer().dropMessage(5, "설정된 아이템은 업그레이드 횟수가 없어 재설정할 수 없습니다.");
                            chr.cubeitemid = 0; // 재설정으로 보냄
                            c.getSession().write(MaplePacketCreator.enableActions());
                            return;
                        }
                        /*
                                    
                         eqstats.containsKey("tuc")
                         */
                        final int reqLevel = (ii.getReqLevel(eq.getItemId()) < 10 ? 10 : ii.getReqLevel(eq.getItemId()) / 10);
                        final int reqMeso = (reqLevel >= 12 ? 39999 : reqLevel >= 7 ? 19999 : 9999);
                        if (c.getPlayer().getMeso() > reqMeso) {
                            int rank = 0;
                            int prop = 0;
                            if (cubeid == 5062002) { // 미라클 큐브 등급 업 확률
                                prop = 0; // 야 이거 2퍼가 아니라 0.2퍼네 일단 기본값 / 1당 0.1%
                                if (eq.getState() == 6) {
                                    prop = 2;  // 에픽 -> 유니크
                                } else if (eq.getState() == 5) {
                                    prop = 9;  // 레어 -> 에픽
                                }
                                if ((eq.getPotential1() > 30000 && eq.getPotential1() < 30041)
                                        || eq.getPotential1() == 30400 || eq.getPotential1() == 30401 || eq.getPotential1() == 30402 || eq.getPotential1() == 30403) {
                                    c.getPlayer().dropMessage(5, "이 아이템의 잠재 능력은 재설정할 수 없습니다.");
                                    c.getSession().write(MaplePacketCreator.enableActions());
                                    return;
                                }
                                piece = 2430111; // 미라클 큐브 조각
                            }
                            if (cubeid == 5062001) { // 마스터 미라클 큐브 등급 업 확률
                                prop = 15; // 2% 기본값
                                piece = 2430112; // 미라클 큐브 조각
                                if (eq.getState() == 7) { // 유니크 -> 레전드리
                                    prop = 6;
                                } else if (eq.getState() == 6) {
                                    prop = 20;  // 에픽 -> 유니크
                                } else if (eq.getState() == 5) {
                                    prop = 50;  // 레어 -> 에픽
                                }
                            }
                            if (cubeid == 5062000) { // 레드 큐브 등급 업 확률
                                prop = 25; // 3% 기본값
                                if (eq.getState() == 7) { // 유니크 -> 레전드리
                                    prop = 3;
                                } else if (eq.getState() == 6) {
                                    prop = 15;  // 에픽 -> 유니크
                                } else if (eq.getState() == 5) {
                                    prop = 30;  // 레어 -> 에픽
                                }
                                piece = 2430112; // 미라클 큐브 조각
                            }
                            if (cubeid == 5062003) { // 플래티넘 큐브 등급 업 확률
                                prop = 40; // 4% 기본값
                                if (eq.getState() == 7) { // 유니크 -> 레전드리
                                    prop = 10;
                                } else if (eq.getState() == 6) {
                                    prop = 30;  // 에픽 -> 유니크
                                } else if (eq.getState() == 5) {
                                    prop = 80;  // 레어 -> 에픽
                                }
                                piece = 2430112; // 미라클 큐브 조각
                            }
                            if (c.getPlayer().isGM() == true) { // GM 등급 업 확률
                                prop = 1000; // 100%
                            }

                            if (ServerConstants.cubeDayValue > 1.0) {
                                prop *= ServerConstants.cubeDayValue;
                            }

                            if (eq.getState() == 7) {
                                if ((eq.getPotential1() > 30000 && eq.getPotential1() < 30041)
                                        || eq.getPotential1() == 30400 || eq.getPotential1() == 30401 || eq.getPotential1() == 30402 || eq.getPotential1() == 30403) {
                                    rank = -8; // 레전드리
                                } else {
                                    rank = Randomizer.nextInt(1000) < prop ? -8 : -7; // 유니크 > 레전드리
                                }
                            } else if (eq.getState() == 6) {
                                rank = Randomizer.nextInt(1000) < prop ? -7 : -6; // 에픽 > 유니크
                            } else if (eq.getState() == 5) {
                                rank = Randomizer.nextInt(1000) < prop ? -6 : -5; // 레어 > 에픽
                            }
//                            c.getPlayer().dropMessage(5, "rank  : " + rank + " | " + eq.getItemId() + " | " + eq.getPosition());

                            int new_state = Math.abs(rank);

                            if (new_state > 12 || new_state < 5) { //보정
                                new_state = 5;
                            }

                            StructPotentialItem pot2 = pots.get(90).get(reqLevel);
//                            c.getPlayer().dropMessage(5, "통과 : " + pot2.potentialID);
                            final int lines = (eq.getPotential3() != 0 ? 3 : 2);
                            for (int i = 0; i < lines; i++) {
                                boolean rewarded = false;
                                while (!rewarded) { // Randomizer.nextInt(pots.size()) >> 이게 레어가 나오면 ㄱㄱ
                                    int a = Randomizer.nextInt(pots.size());
                                    StructPotentialItem pot = pots.get(a).get(reqLevel);

                                    if (pot != null && pot.reqLevel / 10 <= reqLevel && GameConstants.optionTypeFits(pot.optionType, eq.getItemId(), pot.potentialID) && GameConstants.potentialIDFits(pot.potentialID, new_state, i)) {
                                        if (pot.boss && pot.incDAMr > 0) { //보공일 때
                                            double per = 90d; //확률
                                            double secondRandom = Math.random() * 100;
                                            if (secondRandom > per) {
                                                continue;
                                            } //미당첨 새로 옵션 뽑음
                                        } else if (pot.ignoreTargetDEF > 0) { //방무일 때
                                            double per = 90d; //확률
                                            double secondRandom = Math.random() * 100;
                                            if (secondRandom > per) {
                                                continue;
                                            } //미당첨
                                        } else if (pot.incRewardProp > 0) { //아이템 드롭률 증가일 때
                                            double per = 20d; //확률
                                            double secondRandom = Math.random() * 100;
                                            if (secondRandom > per) {
                                                continue;
                                            } //미당첨 새로 옵션 뽑음
                                        } else if (pot.incMesoProp > 0) { //메소 획득량 증가일 때
                                            double per = 20d; //확률
                                            double secondRandom = Math.random() * 100;
                                            if (secondRandom > per) {
                                                continue;
                                            } //미당첨
                                        } else {
                                            switch (pot.potentialID) {
                                                case 30040: //쓸어블
                                                case 30400: //쓸컴뱃
                                                case 30401: //쓸윈부
                                                case 31003: //쓸샾
                                                {
                                                    double per = 10d; //확률
                                                    double secondRandom = Math.random() * 100;
                                                    if (secondRandom > per) {
                                                        continue;
                                                    } //미당첨
                                                    break;
                                                }
                                                case 20051: //공격력 %
                                                case 30051:
                                                case 30023:
                                                case 20052: //마력 %
                                                case 30052:
                                                case 30024: {
                                                    double per = 90d; //확률
                                                    double secondRandom = Math.random() * 100;
                                                    if (secondRandom > per) {
                                                        continue;
                                                    } //미당첨
                                                    break;
                                                }
                                                //여기서부터
//                                                case 30041:
//                                                case 30042:
//                                                case 30043:
//                                                case 30044: {
//                                                //여기까지 힘,덱,인,럭 유니크 옵션
//                                                    double per = 95d; //확률
//                                                    double secondRandom = Math.random() * 100;
//                                                    if (secondRandom > per) {
//                                                        continue;
//                                                    } //미당첨
//                                                    break;
//                                                }
//                                                //여기서부터
//                                                case 30015:
//                                                case 30016:
//                                                case 30017:
//                                                case 30018: {
//                                                //여기까지 힘,덱,인,럭 레전드리 옵션
//                                                    double per = 95d; //확률
//                                                    double secondRandom = Math.random() * 100;
//                                                    if (secondRandom > per) {
//                                                        continue;
//                                                    } //미당첨
//                                                    break;
//                                                }
                                            }
                                        }

                                        if (i == 0) {
                                            eq.setPotential1(pot.potentialID);
                                        } else if (i == 1) {
                                            eq.setPotential2(pot.potentialID);
                                        } else if (i == 2) {
                                            eq.setPotential3(pot.potentialID);
                                        }
                                        rewarded = true;
//                                        if (pot.incPADr > 0 || pot.incMADr > 0) {
//                                            System.err.println((i + 1) + "번째 옵션 코드 : " + pot.potentialID);
//                                        }
                                        if (i == lines) {
                                            break;
                                        }
//                                        System.out.print(a + " : 큐브 옵션\r\n");
//                                        c.getPlayer().dropMessage(5, "a : " + a);
//                                        c.getPlayer().dropMessage(5, "pot.potentialID : " + pot.potentialID);
                                    }
                                }
                            }
                            c.getPlayer().gainMeso(-(reqMeso), true, true);
                        } else {
                            c.getPlayer().dropMessage(1, "'" + ii.getName(eq.getItemId()) + "'(을)를 감정하기 위해서는 " + reqMeso + "메소가 필요합니다.");
                            c.getSession().write(MaplePacketCreator.enableActions());
                            return;
                        }
                        c.getSession().write(MaplePacketCreator.scrolledItem(toUse, item, false, true));
                        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.getPotentialEffect(c.getPlayer().getId(), 1), true);
                        c.getPlayer().forceReAddItem_NoUpdate(item, MapleInventoryType.EQUIP);
                        if (piece > 0) {
                            MapleInventoryManipulator.addById(c, piece, (short) 1, "Cube" + " on " + FileoutputUtil.CurrentReadable_Date());
                        }
                        chr.setNextConsume(time + (1 * 1000)); // 쿨타임
                    } else {
                        c.getPlayer().dropMessage(5, "이 아이템의 잠재 능력은 재설정할 수 없습니다.");
                    }
                } else {
                    c.getPlayer().dropMessage(5, "소비 아이템 여유 공간이 부족하여 잠재 능력 재설정을 실패하였습니다.");
                }
            } else if (use && (cubeid >= 5062004 && cubeid <= 5062006)) {
                final Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) 1);
                if (item == null) {
                    c.getPlayer().dropMessage(5, "설정된 아이템이 없어 재설정할 수 없습니다.");
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }

                if (chr.cubeitemid > 1 && chr.cubeitemid != item.getItemId()) { //설정된 값이 안맞으면 
                    c.getPlayer().dropMessage(5, "설정된 아이템이 변경되어 재설정할 수 없습니다.");
                    chr.cubeitemid = 0; // 재설정으로 보냄
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
                if (item != null && c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                    final Equip eq = (Equip) item;
                    if (chr.cubeitemid == 1) { // 확인 값 1로 변경시
                        chr.cubeitemid = eq.getItemId();
                    }
                    int piece = 0;
                    if (eq.getState() >= 5) {
                        final List<List<StructPotentialItem>> pots = new LinkedList<>(MapleItemInformationProvider.getInstance().getAllPotentialInfo().values());

                        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                        final Map<String, Integer> eqstats = ii.getEquipStats(eq.getItemId());
                        if (!eqstats.containsKey("tuc") || eqstats.containsKey("tuc") && eqstats.get("tuc") == 0) {
                            c.getPlayer().dropMessage(5, "설정된 아이템은 업그레이드 횟수가 없어 재설정할 수 없습니다.");
                            chr.cubeitemid = 0; // 재설정으로 보냄
                            c.getSession().write(MaplePacketCreator.enableActions());
                            return;
                        }

                        final int reqLevel = (ii.getReqLevel(eq.getItemId()) < 10 ? 10 : ii.getReqLevel(eq.getItemId()) / 10);
                        final int reqMeso = (reqLevel >= 12 ? 39999 : reqLevel >= 7 ? 19999 : 9999);
                        if (c.getPlayer().getMeso() > reqMeso) {
                            int rank = 0;
                            if (eq.getState() == 5 || eq.getState() == 6 || eq.getPotential3() == 0) { //레전더리 , 잠재3줄이상에만 사용 가능하게 검사
                                c.getPlayer().dropMessage(5, "이 아이템의 잠재 능력은 재설정할 수 없습니다. 레전더리 잠재3줄이상");
                            }

                            if (eq.getState() == 7) {
                                if ((eq.getPotential1() > 30000 && eq.getPotential1() < 30041)
                                        || eq.getPotential1() == 30400 || eq.getPotential1() == 30401 || eq.getPotential1() == 30402 || eq.getPotential1() == 30403) {
                                    rank = -8; // 레전드리
                                }
                            }

                            int new_state = Math.abs(rank);

                            if (new_state > 12 || new_state < 5) { //보정
                                new_state = 5;
                            }

                            int targetLine = cubeid == 5062004 ? 0 : (cubeid == 5062005 ? 1 : 2); // 줄큡1은 첫 번째, 줄큡2는 두 번째, 줄큡3은 세 번째
                            StructPotentialItem pot2 = pots.get(90).get(reqLevel);
                            boolean rewarded = false;
                            while (!rewarded) {
                                int a = Randomizer.nextInt(pots.size());
                                StructPotentialItem pot = pots.get(a).get(reqLevel);

                                if (pot != null && pot.reqLevel / 10 <= reqLevel && GameConstants.optionTypeFits(pot.optionType, eq.getItemId(), pot.potentialID) && GameConstants.potentialIDFits(pot.potentialID, new_state, targetLine)) {
                                    if (pot.boss && pot.incDAMr > 0) { //보공일 때
                                        double per = 30d; //확률
                                        double secondRandom = Math.random() * 100;
                                        if (secondRandom > per) {
                                            continue;
                                        } //미당첨 새로 옵션 뽑음
                                    } else if (pot.ignoreTargetDEF > 0) { //방무일 때
                                        double per = 50d; //확률
                                        double secondRandom = Math.random() * 100;
                                        if (secondRandom > per) {
                                            continue;
                                        } //미당첨
                                    } else if (pot.incRewardProp > 0) { //아이템 드롭률 증가일 때
                                        double per = 10d; //확률
                                        double secondRandom = Math.random() * 100;
                                        if (secondRandom > per) {
                                            continue;
                                        } //미당첨 새로 옵션 뽑음
                                    } else if (pot.incMesoProp > 0) { //메소 획득량 증가일 때
                                        double per = 10d; //확률
                                        double secondRandom = Math.random() * 100;
                                        if (secondRandom > per) {
                                            continue;
                                        } //미당첨
                                    } else {
                                        switch (pot.potentialID) {
                                            case 30040: //쓸어블
                                            case 30400: //쓸컴뱃
                                            case 30401: //쓸윈부
                                            case 31003: //쓸샾
                                            {
                                                double per = 15d; //확률
                                                double secondRandom = Math.random() * 100;
                                                if (secondRandom > per) {
                                                    continue;
                                                } //미당첨
                                                break;
                                            }
                                            case 20051: //공격력 %
                                            case 30051:
                                            case 30023:
                                            case 20052: //마력 %
                                            case 30052:
                                            case 30024: {
                                                double per = 35d; //확률
                                                double secondRandom = Math.random() * 100;
                                                if (secondRandom > per) {
                                                    continue;
                                                } //미당첨
                                                break;
                                            }
                                            //여기서부터
//                                                case 30041:
//                                                case 30042:
//                                                case 30043:
//                                                case 30044: {
//                                                //여기까지 힘,덱,인,럭 유니크 옵션
//                                                    double per = 20d; //확률
//                                                    double secondRandom = Math.random() * 100;
//                                                    if (secondRandom > per) {
//                                                        continue;
//                                                    } //미당첨
//                                                    break;
//                                                }
//                                                //여기서부터
//                                                case 30015:
//                                                case 30016:
//                                                case 30017:
//                                                case 30018: {
//                                                //여기까지 힘,덱,인,럭 레전드리 옵션
//                                                    double per = 20d; //확률
//                                                    double secondRandom = Math.random() * 100;
//                                                    if (secondRandom > per) {
//                                                        continue;
//                                                    } //미당첨
//                                                    break;
//                                                }
                                            }
                                    }
                                    if (targetLine == 0) {
                                        eq.setPotential1(pot.potentialID);
                                        break;
                                    }
                                    if (targetLine == 1) {
                                        eq.setPotential2(pot.potentialID);
                                        break;
                                    }
                                    if (targetLine == 2) {
                                        eq.setPotential3(pot.potentialID);
                                        break;
                                    }
                                    rewarded = true;

                                     //   System.out.print(a + " : 큐브 옵션\r\n");
                                    // c.getPlayer().dropMessage(5, "a : " + a);
                                    //c.getPlayer().dropMessage(5, "pot.potentialID : " + pot.potentialID);
                                }
                            }
                            c.getPlayer().gainMeso(-reqMeso, true, true);
                        } else {
                            c.getPlayer().dropMessage(1, "'" + ii.getName(eq.getItemId()) + "'(을)를 감정하기 위해서는 " + reqMeso + "메소가 필요합니다.");
                        }
                        c.getSession().write(MaplePacketCreator.scrolledItem(toUse, item, false, true));
                        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.getPotentialEffect(c.getPlayer().getId(), item.getItemId()), true);
                        c.getPlayer().forceReAddItem_NoUpdate(item, MapleInventoryType.EQUIP);
                        c.getPlayer().dropMessage(5, "특정 잠재 능력이 재설정되었습니다.");
                    } else {
                        c.getPlayer().dropMessage(5, "이 아이템의 잠재 능력은 재설정할 수 없습니다.");
                    }
                } else {
                    c.getPlayer().dropMessage(5, "소비 아이템 여유 공간이 부족하여 잠재 능력 재설정을 실패하였습니다.");
                }

            } else {
                c.getPlayer().dropMessage(5, "연결된 큐브가 부족하여 잠재 능력 재설정을 실패하였습니다.");
            }
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (!FieldLimitType.PotionUse.check(chr.getMap().getFieldLimit())) { //cwk quick hack
            if (MapleItemInformationProvider.getInstance().getItemEffect(toUse.getItemId()).applyTo(chr)) {
                MapleStatEffect specEx = MapleItemInformationProvider.getInstance().getItemEffectEX(toUse.getItemId());
                if (specEx != null) {
                    specEx.applyTo(chr);
                }
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                /*if (itemId > 2022423 && itemId <= 2022455) {
                 c.getSession().writeAndFlush(UIPacket.getStatusMsg(itemId));
                 }*/
                if (chr.getMap().getConsumeItemCoolTime() > 0) {
                    chr.setNextConsume(time + (chr.getMap().getConsumeItemCoolTime() * 1000));
                }
            }
            //
        } else {
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    public static final void UseCosmetic(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || !chr.isAlive() || chr.getMap() == null || chr.hasBlockedInventory()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        final byte slot = (byte) slea.readShort();
        final int itemId = slea.readInt();
        final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId || itemId / 10000 != 254 || (itemId / 1000) % 10 != chr.getGender()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (MapleItemInformationProvider.getInstance().getItemEffect(toUse.getItemId()).applyTo(chr)) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
        }
    }

    public static final void UseReturnScroll(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (!chr.isAlive() || chr.getMapId() == 749040100 || chr.hasBlockedInventory() || chr.isInBlockedMap()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        c.getPlayer().updateTick(slea.readInt());
        final byte slot = (byte) slea.readShort();
        final int itemId = slea.readInt();
        final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (!FieldLimitType.PotionUse.check(chr.getMap().getFieldLimit())) {
            if (MapleItemInformationProvider.getInstance().getItemEffect(toUse.getItemId()).applyReturnScroll(chr)) {
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
            } else {
                if (c.getPlayer().isGM()) {
                    c.getPlayer().dropMessage(6, "사용 데이터가 비어있음");
                }
                c.getSession().write(MaplePacketCreator.enableActions());
            }
        } else {
            if (c.getPlayer().isGM()) {
                c.getPlayer().dropMessage(6, "필드 타입이 맞지않음");
            }
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    public static final void UseMagnify(final LittleEndianAccessor slea, final MapleClient c) {
        slea.skip(4);
        c.getPlayer().setScrolledPosition((short) 0);
        final byte src = (byte) slea.readShort();
        final byte check = (byte) slea.readShort();
        final boolean insight = src == 127;
        Equip toReveal;
        final Item magnify = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(src);
        if (check < 0) {
            toReveal = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((byte) check);
        } else {
            toReveal = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) check);
        }
        if ((magnify == null && !insight) || toReveal == null || c.getPlayer().hasBlockedInventory()) {
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return;
        }
        final Equip eqq = (Equip) toReveal;
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        int reqLevel = ii.getReqLevel(eqq.getItemId()) / 10;
        if (eqq.getState() == 1 && (magnify.getItemId() == 2460003 || (magnify.getItemId() == 2460002 && reqLevel <= 12) || (magnify.getItemId() == 2460001 && reqLevel <= 7) || (magnify.getItemId() == 2460000 && reqLevel <= 3))) {
            final List<List<StructPotentialItem>> pots = new LinkedList<>(MapleItemInformationProvider.getInstance().getAllPotentialInfo().values());
            int new_state = Math.abs(eqq.getPotential1());
            int legendary_state = Math.abs(eqq.getPotential1());
            //new_state = (new_state > 12 || new_state < 5) ? 5 : (new_state == 8 || new_state == 11 || new_state == 12) ? 7 : (new_state == 10) ? 6 : (new_state == 9) ? 5 : new_state;
            if (new_state == 8) {
                new_state = 7;
            } else if (new_state == 9) {
                new_state = 5;
            } else if (new_state == 10) {
                new_state = 6;
            } else if (new_state == 11) {
                new_state = 7;
            } else if (new_state == 12) {
                new_state = 7;
            } else if (new_state > 12 || new_state < 5) {
                new_state = 5;
            }
            if (legendary_state > 12 || legendary_state < 5) {
                legendary_state = 5;
            }
            final int lines = (eqq.getPotential3() != 0 ? 3 : 2);
            while (eqq.getState() != new_state) {
                for (int i = 0; i < lines; i++) {
                    boolean rewarded = false;
                    while (!rewarded) {
                        List<StructPotentialItem> potList = pots.get(Randomizer.nextInt(pots.size()));
                        StructPotentialItem pot = potList.get(Math.min(reqLevel, potList.size() - 1));
                        //if (pot != null && pot.reqLevel / 10 <= reqLevel && GameConstants.optionTypeFits(pot.optionType, eqq.getItemId()) && GameConstants.potentialIDFits(pot.potentialID, legendary_state, i, 10)) {
                        if (pot != null && pot.reqLevel / 10 <= reqLevel && GameConstants.optionTypeFits(pot.optionType, eqq.getItemId(), pot.potentialID) && GameConstants.potentialIDFits(pot.potentialID, legendary_state, i)) {
                            if (i == 0) {
                                eqq.setPotential1(pot.potentialID);
                            } else if (i == 1) {
                                eqq.setPotential2(pot.potentialID);
                            } else if (i == 2) {
                                eqq.setPotential3(pot.potentialID);
                            }
                            rewarded = true;
                        }
                    }
                }
            }
            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getPotentialReset(c.getPlayer().getId(), eqq.getPosition()));
            c.getSession().write(MaplePacketCreator.scrolledItem(magnify, toReveal, false, true));
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, magnify.getPosition(), (short) 1, false);
        } else {
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            //return;
        }
    }

    public static final void addToScrollLog(int accountID, int charID, int scrollID, int itemID, byte oldSlots, byte newSlots, byte viciousHammer, String result, boolean ws, boolean ls, int vega) {
        Connection con = null;//커넥션
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("INSERT INTO scroll_log VALUES(DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setInt(1, accountID);
            ps.setInt(2, charID);
            ps.setInt(3, scrollID);
            ps.setInt(4, itemID);
            ps.setByte(5, oldSlots);
            ps.setByte(6, newSlots);
            ps.setByte(7, viciousHammer);
            ps.setString(8, result);
            ps.setByte(9, (byte) (ws ? 1 : 0));
            ps.setByte(10, (byte) (ls ? 1 : 0));
            ps.setInt(11, vega);
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
            FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
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

    public static final boolean UseUpgradeScroll(final short slot, final short dst, final short ws, final MapleClient c, final MapleCharacter chr) {
        return UseUpgradeScroll(slot, dst, ws, c, chr, 0);
    }

    public static final boolean UseUpgradeScroll(final short slot, final short dst, final short ws, final MapleClient c, final MapleCharacter chr, final int vegas) {
        boolean whiteScroll = false; // white scroll being used?
        boolean legendarySpirit = false; // legendary spirit skill
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        chr.setScrolledPosition((short) 0);
        if ((ws & 2) == 2) {
            whiteScroll = true;
        }
        Equip toScroll;
        if (dst < 0) {
            toScroll = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
        } else { // legendary spirit
            legendarySpirit = true;
            toScroll = (Equip) chr.getInventory(MapleInventoryType.EQUIP).getItem(dst);
        }
        if (toScroll == null || c.getPlayer().hasBlockedInventory()) {
            return false;
        }
        final byte oldLevel = toScroll.getLevel();
        final byte oldEnhance = toScroll.getEnhance();
        final byte oldState = toScroll.getState();
        final short oldFlag = toScroll.getFlag();
        final byte oldSlots = toScroll.getUpgradeSlots();
        final byte oldVH = (byte) toScroll.getViciousHammer();
        final int itemID = toScroll.getItemId();

        Item scroll = chr.getInventory(MapleInventoryType.USE).getItem(slot);
        if (scroll == null) {
            scroll = chr.getInventory(MapleInventoryType.CASH).getItem(slot);
            if (scroll == null) {
                c.getSession().write(MaplePacketCreator.getInventoryFull());
                return false;
            }
        }
        if (scroll.getItemId() == 2049123) {
            if (toScroll.getItemId() / 10000 == 114) {
                c.getSession().write(MaplePacketCreator.getInventoryFull());
                return false;
            }
        }

        if (scroll.getItemId() != 2049123 && !GameConstants.isSpecialScroll(scroll.getItemId()) && !GameConstants.isCleanSlate(scroll.getItemId()) && !GameConstants.isEquipScroll(scroll.getItemId()) && !GameConstants.isPotentialScroll(scroll.getItemId())) {
            if (toScroll.getUpgradeSlots() < 1) {
                c.getSession().write(MaplePacketCreator.getInventoryFull());
                return false;
            }
        } else if (GameConstants.isEquipScroll(scroll.getItemId())) {
            if (toScroll.getUpgradeSlots() >= 1 || toScroll.getEnhance() >= 100 || vegas > 0 || ii.isCash(toScroll.getItemId())) {
                c.getSession().write(MaplePacketCreator.getInventoryFull());
                return false;
            }
        } else if (GameConstants.isPotentialScroll(scroll.getItemId())) {
            if (toScroll.getState() >= 1 || (toScroll.getLevel() == 0 && toScroll.getUpgradeSlots() == 0 && toScroll.getItemId() / 10000 != 135) || vegas > 0 || ii.isCash(toScroll.getItemId())) {
                c.getSession().write(MaplePacketCreator.getInventoryFull());
                return false;
            }
        } else if (GameConstants.isSpecialScroll(scroll.getItemId())) {
            if (ii.isCash(toScroll.getItemId()) || toScroll.getEnhance() >= 8) {
                c.getSession().write(MaplePacketCreator.getInventoryFull());
                return false;
            }
        }
        if (!GameConstants.canScroll(toScroll.getItemId()) && !GameConstants.isChaosScroll(toScroll.getItemId())) {
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return false;
        }
        if ((GameConstants.isCleanSlate(scroll.getItemId()) || GameConstants.isTablet(scroll.getItemId()) || GameConstants.isGeneralScroll(scroll.getItemId()) || GameConstants.isChaosScroll(scroll.getItemId())) && (vegas > 0 || ii.isCash(toScroll.getItemId()))) {
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return false;
        }
        if (GameConstants.isTablet(scroll.getItemId()) && toScroll.getDurability() < 0) { //not a durability item
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return false;
        } else if ((!GameConstants.isTablet(scroll.getItemId()) && !GameConstants.isPotentialScroll(scroll.getItemId()) && !GameConstants.isEquipScroll(scroll.getItemId()) && !GameConstants.isCleanSlate(scroll.getItemId()) && !GameConstants.isSpecialScroll(scroll.getItemId()) && !GameConstants.isChaosScroll(scroll.getItemId())) && toScroll.getDurability() >= 0) {
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return false;
        }

        if (scroll.getItemId() == 2049009 || scroll.getItemId() == 2049010) { //환불
            if (GameConstants.isRing(toScroll.getItemId()) || toScroll.getItemId() / 1000 == 1092 || toScroll.getItemId() / 1000 == 1342 || toScroll.getItemId() / 1000 == 1713 || toScroll.getItemId() / 1000 == 1712 || toScroll.getItemId() / 1000 == 1152 || toScroll.getItemId() / 1000 == 1142 || toScroll.getItemId() / 1000 == 1143 || toScroll.getItemId() / 1000 == 1672 || toScroll.getItemId() / 1000 == 1190 || toScroll.getItemId() / 1000 == 1191 || toScroll.getItemId() / 1000 == 1182 || toScroll.getItemId() / 1000 == 1662 || toScroll.getItemId() / 1000 == 1802) {
                c.getSession().write(MaplePacketCreator.getInventoryFull());
                return false;
            }
        } else if (GameConstants.isCleanSlate(scroll.getItemId())) {
            Map<String, Integer> eqstats = ii.getEquipStats(toScroll.getItemId());
            if (eqstats != null) {
                if (eqstats.containsKey("tuc") && toScroll.getLevel() + toScroll.getUpgradeSlots() < (eqstats.get("tuc") + toScroll.getViciousHammer())) {
                    //가능
                } else {
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    return false;
                }
            }
        }

        Item wscroll = null;

        // Anti cheat and validation
        List<Integer> scrollReqs = ii.getScrollReqs(scroll.getItemId());
        if (scrollReqs != null && scrollReqs.size() > 0 && !scrollReqs.contains(toScroll.getItemId())) {
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return false;
        }

        if (whiteScroll) {
            wscroll = chr.getInventory(MapleInventoryType.USE).findById(2340000);
            if (wscroll == null) {
                whiteScroll = false;
            }
        }
        if (GameConstants.isTablet(scroll.getItemId()) || GameConstants.isGeneralScroll(scroll.getItemId())) {
            switch (scroll.getItemId() % 1000 / 100) {
                case 0: //1h
                    if (GameConstants.isTwoHanded(toScroll.getItemId()) || !GameConstants.isWeapon(toScroll.getItemId())) {
                        return false;
                    }
                    break;
                case 1: //2h
                    if (!GameConstants.isTwoHanded(toScroll.getItemId()) || !GameConstants.isWeapon(toScroll.getItemId())) {
                        return false;
                    }
                    break;
                case 2: //armor
                    if (GameConstants.isAccessory(toScroll.getItemId()) || GameConstants.isWeapon(toScroll.getItemId())) {
                        return false;
                    }
                    break;
                case 3: //accessory
                    if (!GameConstants.isAccessory(toScroll.getItemId()) || GameConstants.isWeapon(toScroll.getItemId())) {
                        System.err.println("Return" + toScroll.getItemId());
                        return false;
                    }
                    break;
            }
        } else if (scroll.getItemId() != 2049123 && !GameConstants.isAccessoryScroll(scroll.getItemId()) && !GameConstants.isChaosScroll(scroll.getItemId()) && !GameConstants.isCleanSlate(scroll.getItemId()) && !GameConstants.isEquipScroll(scroll.getItemId()) && !GameConstants.isPotentialScroll(scroll.getItemId()) && !GameConstants.isSpecialScroll(scroll.getItemId())) {
            if (!ii.canScroll(scroll.getItemId(), toScroll.getItemId())) {
                return false;
            }
        }
        if (GameConstants.isAccessoryScroll(scroll.getItemId()) && !GameConstants.isAccessory(toScroll.getItemId())) {
            return false;
        }
        if (scroll.getQuantity() <= 0) {
            return false;
        }

        if (legendarySpirit && vegas == 0) {
            if (chr.getSkillLevel(SkillFactory.getSkill(chr.getStat().getSkillByJob(1003, chr.getJob()))) <= 0) {
                return false;
            }
        }

        if (scroll.getItemId() == 2041200 && toScroll.getLevel() > 0 && toScroll.getItemId() == 1122000) { //혼목 & 드래곤의 돌
            c.getPlayer().dropMessage(5, "더 이상 강화할 수 없는 아이템 입니다.");
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return false;
        }
        if (scroll.getItemId() == 2041200 && toScroll.getLevel() > 0 && toScroll.getItemId() == 1122076) { //카혼목 & 드래곤의 돌
            c.getPlayer().dropMessage(5, "더 이상 강화할 수 없는 아이템 입니다.");
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return false;
        }

        // Scroll Success/ Failure/ Curse
        Equip scrolled = (Equip) ii.scrollEquipWithId(toScroll, scroll, whiteScroll, chr, vegas);
        ScrollResult scrollSuccess;
        if (scrolled == null) {
            if (ItemFlag.SHIELD_WARD.check(oldFlag)) {
                scrolled = toScroll;
                scrollSuccess = Equip.ScrollResult.FAIL;
                scrolled.setFlag((short) (oldFlag - ItemFlag.SHIELD_WARD.getValue()));
            } else {
                scrollSuccess = Equip.ScrollResult.CURSE;
            }
        } else if (scrolled.getLevel() > oldLevel || scrolled.getEnhance() > oldEnhance || scrolled.getState() > oldState || scrolled.getFlag() > oldFlag) {
            scrollSuccess = Equip.ScrollResult.SUCCESS;
        } else if ((GameConstants.isCleanSlate(scroll.getItemId()) && scrolled.getUpgradeSlots() > oldSlots) || scroll.getItemId() == 2049009 || scroll.getItemId() == 2049010 || scroll.getItemId() == 2049123) {
            scrollSuccess = Equip.ScrollResult.SUCCESS;
        } else {
            scrollSuccess = Equip.ScrollResult.FAIL;
        }
        // Update
        chr.getInventory(GameConstants.getInventoryType(scroll.getItemId())).removeItem(scroll.getPosition(), (short) 1, false);
        if (whiteScroll) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, wscroll.getPosition(), (short) 1, false, false);
        } else if (scrollSuccess == Equip.ScrollResult.FAIL && scrolled.getUpgradeSlots() < oldSlots && c.getPlayer().getInventory(MapleInventoryType.CASH).findById(5640000) != null) {
            chr.setScrolledPosition(scrolled.getPosition());
            if (vegas == 0) {
                c.getSession().write(MaplePacketCreator.pamSongUI());
            }
        }

        if (scrollSuccess == Equip.ScrollResult.CURSE) {
            c.getSession().write(MaplePacketCreator.scrolledItem(scroll, toScroll, true, false));
            if (dst < 0) {
//                chr.CustomStatEffect(true);
                chr.customizeStat(0);
                //chr.rosySymbol();//0106
                chr.getInventory(MapleInventoryType.EQUIPPED).removeItem(toScroll.getPosition());
            } else {
                chr.getInventory(MapleInventoryType.EQUIP).removeItem(toScroll.getPosition());
            }
        } else if (vegas == 0) {
            c.getSession().write(MaplePacketCreator.scrolledItem(scroll, scrolled, false, false));
        }

        chr.getMap().broadcastMessage(chr, MaplePacketCreator.getScrollEffect(c.getPlayer().getId(), scrollSuccess, legendarySpirit, whiteScroll), vegas == 0);
        addToScrollLog(chr.getAccountID(), chr.getId(), scroll.getItemId(), itemID, oldSlots, (byte) (scrolled == null ? -1 : scrolled.getUpgradeSlots()), oldVH, scrollSuccess.name(), whiteScroll, legendarySpirit, vegas);
        // equipped item was scrolled and changed
        if (dst < 0 && (scrollSuccess == Equip.ScrollResult.SUCCESS || scrollSuccess == Equip.ScrollResult.CURSE) && vegas == 0) {
            chr.equipChanged();
        }
        return true;
    }

    public static final boolean UseSkillBook(final byte slot, final int itemId, final MapleClient c, final MapleCharacter chr) {
        final Item toUse = chr.getInventory(GameConstants.getInventoryType(itemId)).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId || chr.hasBlockedInventory()) {
            return false;
        }
        final Map<String, Integer> skilldata = MapleItemInformationProvider.getInstance().getEquipStats(toUse.getItemId());
        if (skilldata == null) { // Hacking or used an unknown item
            return false;
        }
        boolean canuse = false, success = false;
        int skill = 0, maxlevel = 0;

        final Integer SuccessRate = skilldata.get("success");
        final Integer ReqSkillLevel = skilldata.get("reqSkillLevel");
        final Integer MasterLevel = skilldata.get("masterLevel");

        byte i = 0;
        Integer CurrentLoopedSkillId;
        while (true) {
            CurrentLoopedSkillId = skilldata.get("skillid" + i);
            i++;
            if (CurrentLoopedSkillId == null || MasterLevel == null) {
                break; // End of data
            }
            final Skill CurrSkillData = SkillFactory.getSkill(CurrentLoopedSkillId);
            if (CurrSkillData != null && CurrSkillData.canBeLearnedBy(chr.getJob()) && (ReqSkillLevel == null || chr.getSkillLevel(CurrSkillData) >= ReqSkillLevel) && chr.getMasterLevel(CurrSkillData) < MasterLevel) {
                canuse = true;
                if (SuccessRate == null || Randomizer.nextInt(100) <= SuccessRate) {
                    success = true;
                    chr.changeSkillLevel(CurrSkillData, chr.getSkillLevel(CurrSkillData), (byte) (int) MasterLevel);
                } else {
                    success = false;
                }
                MapleInventoryManipulator.removeFromSlot(c, GameConstants.getInventoryType(itemId), slot, (short) 1, false);
                break;
            }
        }
        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.useSkillBook(chr, skill, maxlevel, canuse, success));
        c.getSession().write(MaplePacketCreator.enableActions());
        return canuse;
    }

    public static final void UseCatchItem(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        c.getPlayer().updateTick(slea.readInt());
        c.getPlayer().setScrolledPosition((short) 0);
        final byte slot = (byte) slea.readShort();
        final int itemid = slea.readInt();
        final MapleMonster mob = chr.getMap().getMonsterByOid(slea.readInt());
        final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);
        final MapleMap map = chr.getMap();

        if (toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemid && mob != null && !chr.hasBlockedInventory() && itemid / 10000 == 227 && MapleItemInformationProvider.getInstance().getCardMobId(itemid) == mob.getId()) {
            final ItemInformation i = MapleItemInformationProvider.getInstance().getItemInformation(itemid);
            int mobhp = i.flag & 0x1000;
            if (mobhp != 0 && mob.getHp() <= mob.getMobMaxHp() * mobhp / 10000 || mobhp == 0 && mob.getHp() <= mob.getMobMaxHp() / 2) {
                //c.getSession().write(MaplePacketCreator.catchMonster(mob.getId(), itemid, (byte) 1));
                map.broadcastMessage(MaplePacketCreator.catchMonster(mob.getId(), itemid, (byte) 1));
                map.broadcastMessage(MaplePacketCreator.showMagnet(mob.getObjectId(), (byte) 1));
                map.killMonster(mob, chr, true, false, (byte) 1);
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false, false);
                if (MapleItemInformationProvider.getInstance().getCreateId(itemid) > 0) {
                    MapleInventoryManipulator.addById(c, MapleItemInformationProvider.getInstance().getCreateId(itemid), (short) 1, "Catch item " + itemid + " on " + FileoutputUtil.CurrentReadable_Date());
                }
            } else {
                //map.broadcastMessage(MaplePacketCreator.showMagnet(mob.getObjectId(), (byte) 0));
                map.broadcastMessage(MaplePacketCreator.catchMonster(mob.getId(), itemid, (byte) 0));
                c.getSession().write(MaplePacketCreator.catchMob(mob.getId(), itemid, (byte) 0));
            }
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    /*public static final void UseCatchItem(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
     //slea.skip(4);1229
     c.getPlayer().updateTick(slea.readInt());
     c.getPlayer().setScrolledPosition((short) 0);
     final byte slot = (byte) slea.readShort();
     final int itemid = slea.readInt();
     final MapleMonster mob = chr.getMap().getMonsterByOid(slea.readInt());
     final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);
     final MapleMap map = chr.getMap();

     if (toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemid && mob != null && !chr.hasBlockedInventory() && itemid / 10000 == 227 && MapleItemInformationProvider.getInstance().getCardMobId(itemid) == mob.getId()) {
     if (!MapleItemInformationProvider.getInstance().isMobHP(itemid) || mob.getHp() <= mob.getMobMaxHp() / 1) {
     map.broadcastMessage(MaplePacketCreator.catchMonster(mob.getObjectId(), itemid, (byte) 1));
     map.killMonster(mob, chr, true, false, (byte) 1);
     MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false, false);
     if (MapleItemInformationProvider.getInstance().getCreateId(itemid) > 0) {
     MapleInventoryManipulator.addById(c, MapleItemInformationProvider.getInstance().getCreateId(itemid), (short) 1, "Catch item " + itemid + " on " + FileoutputUtil.CurrentReadable_Date());
     }
     } else {
     //map.broadcastMessage(MaplePacketCreator.catchMonster(mob.getObjectId(), itemid, (byte) 0));
     //c.getSession().write(MaplePacketCreator.catchMob(mob.getId(), itemid, (byte) 0));
     }
     }
     c.getSession().write(MaplePacketCreator.enableActions());
     }*/
    public static final void UseMountFood(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        //slea.skip(4);
        c.getPlayer().updateTick(slea.readInt());
        final byte slot = (byte) slea.readShort();
        final int itemid = slea.readInt(); //2260000 usually
        final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);
        final MapleMount mount = chr.getMount();

        if (itemid / 10000 == 226 && toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemid && mount != null && !c.getPlayer().hasBlockedInventory()) {
            final int fatigue = mount.getFatigue();

            boolean levelup = false;
            mount.setFatigue((byte) -30);

            if (fatigue > 0) {
                mount.increaseExp();
                final int level = mount.getLevel();
                if (level < 30 && mount.getExp() >= GameConstants.getMountExpNeededForLevel(level + 1)) {
                    mount.setLevel((byte) (level + 1));
                    levelup = true;
                }
            }
            chr.getMap().broadcastMessage(MaplePacketCreator.updateMount(chr, levelup));
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static final void UseScriptedNPCItem(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        slea.skip(4);
        final byte slot = (byte) slea.readShort();
        final int itemId = slea.readInt();
        //  final Item toUse = chr.getInventory(GameConstants.getInventoryType(itemId)).getItem(slot);
        final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);
        long expiration_days = 0;
        int mountid = 0;

        Map<MapleStat, Integer> statup = new EnumMap<MapleStat, Integer>(MapleStat.class);

        if (slot < 0 || slot > c.getPlayer().getInventory(GameConstants.getInventoryType(itemId)).getSlotLimit() || itemId > 3000000) {
            c.getPlayer().dropMessage(1, "오류가 발생하였습니다. : - 7");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }

        if (toUse != null && toUse.getQuantity() >= 1 && toUse.getItemId() == itemId && !chr.hasBlockedInventory() && !chr.inPVP()) {
            switch (toUse.getItemId()) {
                /*   case 2430015:
                 if (c.getPlayer().getMapId() != 106020500) {
                 c.getPlayer().dropMessage(5, "이곳에서는 사용하실 수 없습니다.");
                 break;
                 }
                 MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);  
                 c.getSession().write(MaplePacketCreator.enableActions());   
                 NPCScriptManager.getInstance().start(c, 1300015);
                 break;*/
                case 2430100:
                //case 2430101:  ///수정했음 오류나면 주석풀기
                case 2430102:
                    int mpitemid = toUse.getItemId();
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                /*    case 2430014:
                 if (c.getPlayer().getMapId() != 106020300) {
                 c.getPlayer().dropMessage(5, "이곳에서는 사용하실 수 없습니다.");
                 break;
                 }
                 c.getPlayer().getQuestNAdd(MapleQuest.getInstance(2015421)).setCustomData("1");
                 c.getPlayer().dropMessage(5, "킬라 버섯 포자를 이용하여 앞 길을 가로 막고 있는 결계를 제거하였다.");
                 MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                 break;*/
                case 2430143: //정체불명의 러브레터
                    int prop = Randomizer.rand(0, 100);
                    int count = 0;
                    if (prop <= 20) {
                        count = 1;
                    } else if (prop <= 40) {
                        count = 2;
                    } else if (prop <= 60) {
                        count = 3;
                    } else if (prop <= 80) {
                        count = 4;
                    } else {
                        count = 5;
                    }

                    c.getSession().write(MaplePacketCreator.getShowFameGain(count));
                    c.getPlayer().addFame(count);
                    c.getPlayer().updateSingleStat(MapleStat.FAME, c.getPlayer().getFame());
                    c.getPlayer().dropMessage(5, "편지 속에 담긴 사랑의 힘으로 인기도가 올라갔습니다. 하지만 누가 보낸 편지인지 알 수 없군요.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;

                case 2430007: // Blank Compass
                {
                    final MapleInventory inventory = chr.getInventory(MapleInventoryType.SETUP);
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);

                    if (inventory.countById(3994102) >= 1 // Compass Letter "North"
                            && inventory.countById(3994103) >= 1 // Compass Letter "South"
                            && inventory.countById(3994104) >= 1 // Compass Letter "East"
                            && inventory.countById(3994105) >= 1) { // Compass Letter "West"
                        MapleInventoryManipulator.addById(c, 2430008, (short) 1, "Scripted item: " + itemId + " on " + FileoutputUtil.CurrentReadable_Date()); // Gold Compass
                        MapleInventoryManipulator.removeById(c, MapleInventoryType.SETUP, 3994102, 1, false, false);
                        MapleInventoryManipulator.removeById(c, MapleInventoryType.SETUP, 3994103, 1, false, false);
                        MapleInventoryManipulator.removeById(c, MapleInventoryType.SETUP, 3994104, 1, false, false);
                        MapleInventoryManipulator.removeById(c, MapleInventoryType.SETUP, 3994105, 1, false, false);
                        c.getPlayer().dropMessage(5, "황금 나침반을 제작하였습니다. 더블클릭하여 <골드리치 보물섬>으로 떠나보세요!");
                    } else {
                        c.getPlayer().dropMessage(1, "나침반용 알파벳 N,W,E,S를 1개씩 모으면 황금 나침반을 제작할 수 있습니다.");
                        MapleInventoryManipulator.addById(c, 2430007, (short) 1, "Scripted item: " + itemId + " on " + FileoutputUtil.CurrentReadable_Date()); // Blank Compass
                    }
                    //NPCScriptManager.getInstance().start(c, 2084001);
                    //c.getPlayer().dropMessage(5, "나침반용 알파벳 N,W,E,S를 1개씩 모으면 황금 나침반을 제작할 수 있습니다.");                    
                    break;
                }
                case 2433017: {
                    List<Pair<Integer, Short>> items = new ArrayList<>();
                    int[] id = {2000000, 2000001, 2000003}; // 아이템코드
                    int[] q = {1, 2, 5}; // 갯수
                    for (int i = 0; i < id.length; i++) {
                        items.add(new Pair<>(id[i], (short) q[i]));
                    }
                    byte num = (byte) Randomizer.rand(0, items.size() - 1);
                    chr.gainItem(items.get(num).getLeft(), items.get(num).getRight());
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                }
                case 2430031: {
                    c.getSession().write(UIPacket.AranTutInstructionalBalloon("Effect/OnUserEff.img/itemEffect/polaloid/2430031"));
                    break;
                }
                case 2430032: {
                    if (chr.getMapId() != 922030011 || chr.getTruePosition().x < 120) {
                        chr.dropMessage(5, "이곳에서는 사용할 수 없습니다.");
                        break;
                    }
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    chr.getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9300388), new Point(chr.getTruePosition().x, 132));
                    break;
                }
                case 2430071: {
                    if (!c.getPlayer().canHold(4032616, 1)) {
                        c.getPlayer().dropMessage(1, "기타 인벤토리 공간이 부족합니다.");
                        break;
                    }
                    if (c.getPlayer().haveItem(4032616, 1)) {
                        c.getPlayer().dropMessage(1, "이미 혜안을 소지하고 있습니다.");
                        break;
                    }
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    c.getSession().write(UIPacket.AranTutInstructionalBalloon("Effect/OnUserEff.img/itemEffect/quest/2430071"));
                    if ((int) (Math.random() * 5) == 0) {
                        chr.dropMessage(5, "탁한 유리구슬이 부서졌습니다. 혜안을 얻었습니다.");
                        c.getPlayer().gainItem(4032616, (short) 1, true);
                    } else {
                        chr.dropMessage(5, "탁한 유리구슬이 부서졌습니다. 아무것도 나오지 않았습니다.");
                    }
                    break;
                }
                case 2430014: {
                    c.getPlayer().openNpc(1300010);
                    break;
                }
                /*case 2434568: {
                 c.getPlayer().openNpc(9000546);
                 break;
                 }*/
                case 2434568: {
                    if (c.getPlayer().getMapId() != 701210161) {
                        c.getPlayer().dropMessage(5, "이곳에서는 사용하실 수 없습니다.");
                        break;
                    }
                    if (c.getPlayer().getPosition().x < -75 || c.getPlayer().getPosition().x > 92) {
                        c.getPlayer().dropMessage(1, "악령 퇴치제를 사용 할 수 없습니다.");
                        break;
                    }
                    NPCScriptManager.getInstance().start(c, 2007, "DemonBGone");
                    break;
                }
                case 2430015: {
                    if (c.getPlayer().getMapId() != 106020500) {
                        c.getSession().write(MaplePacketCreator.enableActions());
                        c.getSession().write(MaplePacketCreator.getNPCTalk(1300011, (byte) 0, "이곳에선 사용할 수 없습니다.", "00 00", (byte) 3));
                        break;
                    } else if (c.getPlayer().getPosition().x < 180) {
                        c.getSession().write(MaplePacketCreator.enableActions());
                        c.getSession().write(MaplePacketCreator.getNPCTalk(1300011, (byte) 0, "조금 더 가까이 가서 사용하자.", "00 00", (byte) 3));
                        break;
                    }
                    c.getPlayer().openNpc(1300011);
                    break;
                }
                case 2430159: {
                    if (c.getPlayer().getMapId() == 211060400) {
                        c.getPlayer().openNpc(2161004);
                    } else {
                        c.getPlayer().dropMessage(5, "이곳에선 사용할 수 없다. 머트가 있는 곳으로 가자.");
                    }
                    break;
                }
                case 2430180: {
                    if (c.getPlayer().getMapId() == 211041400) {
                        if (c.getPlayer().getQuestStatus(3192) == 1) {
                            MapleQuest.getInstance(3192).forceStart(c.getPlayer(), 0, "1");
                        }
                        c.getPlayer().dropMessage(5, "결계의 토템이 밝은 빛을 내며 화로가 타기 시작했고, 토템에 새겨진 알케스터의 결계 마법이 발동합니다.");
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                        c.getSession().write(MaplePacketCreator.getShowItemGain(itemId, (byte) -1, true));
                    } else {
                        c.getPlayer().dropMessage(5, "이곳에선 사용할 수 없다.");
                    }
                    break;
                }
                case 2431008: { //불가사의 레시피
                    final int itemid = Randomizer.nextInt(143) + 2510000;//
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {//인벤 필요 요구 수치
                        MapleInventoryManipulator.addById(c, itemid, (short) 1, "Reward item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    } else {
                        c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                    }
                    break;
                }
                case 2431007: { //불가사의 주문서
                    final int itemid = Randomizer.nextInt(14) + 2046200;//
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {//인벤 필요 요구 수치
                        MapleInventoryManipulator.addById(c, itemid, (short) 1, "Reward item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    } else {
                        c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                    }
                    break;
                }
                case 2430008: // Gold Compass
                {
                    chr.saveLocation(SavedLocationType.RICHIE);
                    MapleMap map;
                    boolean warped = false;

                    for (int i = 390001000; i <= 390001004; i++) {
                        map = c.getChannelServer().getMapFactory().getMap(i);

                        if (map.getCharactersSize() == 0) {
                            chr.changeMap(map, map.getPortal(0));
                            warped = true;
                            break;
                        }
                    }
                    if (warped) { // Removal of gold compass
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    } else { // Or mabe some other message.
                        c.getPlayer().dropMessage(5, "모든 맵에 유저가 존재합니다. 잠시 후 다시 시도해주세요.");
                    }
                    break;
                }
                case 2439998: // 백간랑하 스킵티켓
                {
                    MapleMap map;
                    boolean warped = false;

                    for (int i = 800040400; i <= 800040400; i++) {
                        map = c.getChannelServer().getMapFactory().getMap(i);

                        if (map.getCharactersSize() == 0) {
                            chr.changeMap(map, map.getPortal(0));
                            warped = true;
                            break;
                        }
                    }
                    if (warped) { // Removal of gold compass
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    } else { // Or mabe some other message.
                        c.getPlayer().dropMessage(5, "천수각 1층에 다른 유저가 존재합니다. 잠시 후 다시 시도해주세요.");
                    }
                    break;
                }
                case 2431003: //마일리지50
                    c.getPlayer().modifyCSPoints(3, 50, false);//캐시
                    c.getPlayer().dropMessage(5, 50 + " 마일리지가 적립 되었습니다.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                case 2431004: //마일리지100
                    c.getPlayer().modifyCSPoints(3, 100, false);
                    c.getPlayer().dropMessage(5, 100 + " 마일리지가 적립 되었습니다.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                case 2431005: //마일리지250
                    c.getPlayer().modifyCSPoints(3, 250, false);
                    c.getPlayer().dropMessage(5, 250 + " 마일리지가 적립 되었습니다.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                case 2431006: //마일리지500
                    c.getPlayer().modifyCSPoints(3, 500, false);
                    c.getPlayer().dropMessage(5, 500 + " 마일리지가 적립 되었습니다.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                case 2431009: //마일리지1000
                    c.getPlayer().modifyCSPoints(3, 1000, false);
                    c.getPlayer().dropMessage(5, 1000 + " 마일리지가 적립 되었습니다.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                case 2431010: //마일리지2000
                    c.getPlayer().modifyCSPoints(3, 2000, false);
                    c.getPlayer().dropMessage(5, 2000 + " 마일리지가 적립 되었습니다.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                case 2431011: //마일리지3000
                    c.getPlayer().modifyCSPoints(3, 3000, false);
                    c.getPlayer().dropMessage(5, 3000 + " 마일리지가 적립 되었습니다.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                case 2431012: //마일리지5000
                    c.getPlayer().modifyCSPoints(3, 5000, false);
                    c.getPlayer().dropMessage(5, 5000 + " 마일리지가 적립 되었습니다.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                case 2432000: //캐시1000
                    c.getPlayer().modifyCSPoints(1, 1000, false);
                    c.getPlayer().dropMessage(5, 1000 + " 캐시가 적립 되었습니다.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                case 2432001: //캐시5000
                    c.getPlayer().modifyCSPoints(1, 5000, false);
                    c.getPlayer().dropMessage(5, 5000 + " 캐시가 적립 되었습니다.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                case 2432002: //캐시10000
                    c.getPlayer().modifyCSPoints(1, 10000, false);
                    c.getPlayer().dropMessage(5, 10000 + " 캐시가 적립 되었습니다.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                case 2432003: //캐시50000
                    c.getPlayer().modifyCSPoints(1, 50000, false);
                    c.getPlayer().dropMessage(5, 50000 + " 캐시가 적립 되었습니다.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;   //2022505  
                case 2432004: //캐시100000
                    c.getPlayer().modifyCSPoints(1, 100000, false);
                    c.getPlayer().dropMessage(5, 100000 + " 캐시가 적립 되었습니다.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                case 2432005: //캐시200000
                    c.getPlayer().modifyCSPoints(1, 200000, false);
                    c.getPlayer().dropMessage(5, 200000 + " 캐시가 적립 되었습니다.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                case 2432006: //캐시300000
                    c.getPlayer().modifyCSPoints(1, 300000, false);
                    c.getPlayer().dropMessage(5, 300000 + " 캐시가 적립 되었습니다.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                case 2432007: //캐시500000
                    c.getPlayer().modifyCSPoints(1, 500000, false);
                    c.getPlayer().dropMessage(5, 500000 + " 캐시가 적립 되었습니다.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                case 2432008: //캐시1000000
                    c.getPlayer().modifyCSPoints(1, 1000000, false);
                    c.getPlayer().dropMessage(5, 1000000 + " 캐시가 적립 되었습니다.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                case 2431013: //메이플 포인트1000
                    c.getPlayer().modifyCSPoints(2, 1000, false);
                    c.getPlayer().dropMessage(5, 1000 + " 메이플포인트가 적립 되었습니다.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                case 2431000: //메이플 포인트5000
                    c.getPlayer().modifyCSPoints(2, 5000, false);//메포
                    c.getPlayer().dropMessage(5, 5000 + " 메이플포인트가 적립 되었습니다.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                case 2431001: //메이플 포인트10000
                    c.getPlayer().modifyCSPoints(2, 10000, false);
                    c.getPlayer().dropMessage(5, 10000 + " 메이플포인트가 적립 되었습니다.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                case 2431002: //메이플 포인트30000
                    c.getPlayer().modifyCSPoints(2, 30000, false);
                    c.getPlayer().dropMessage(5, 30000 + " 메이플포인트가 적립 되었습니다.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                case 2434004: //캐시500만
                    c.getPlayer().modifyCSPoints(1, 5000000, false);
                    c.getPlayer().dropMessage(5, 5000000 + " 캐시가 적립 되었습니다.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                case 2434000: //메이플 포인트50만
                    c.getPlayer().modifyCSPoints(2, 500000, false);
                    c.getPlayer().dropMessage(5, 500000 + " 메이플포인트가 적립 되었습니다.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                case 2434001: //메이플 포인트100만
                    c.getPlayer().modifyCSPoints(2, 1000000, false);
                    c.getPlayer().dropMessage(5, 1000000 + " 메이플포인트가 적립 되었습니다.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                case 2434002: //메이플 포인트500만
                    c.getPlayer().modifyCSPoints(2, 5000000, false);
                    c.getPlayer().dropMessage(5, 5000000 + " 메이플포인트가 적립 되었습니다.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                case 2430190: //금빛 각인의 인장
                    NPCScriptManager.getInstance().start(c, 9031005);
                    break;
                case 2430192: //AP초기화
                    NPCScriptManager.getInstance().start(c, 9031003);
                    break;
                case 2430193: //로얄스타일
                    NPCScriptManager.getInstance().start(c, 9000172);
                    break;
                case 2439988: //VIP 순간이동의 돌
                    NPCScriptManager.getInstance().start(c, 1082100);
                    break;
                //case 2430016: //아이스박스
                //NPCScriptManager.getInstance().start(c, 9000021);
                //break;
                case 2430191: //프로텍트 쉴드
                    break;
                case 2430114: //플래티넘 미라클 큐브 조각
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {//인벤 필요 요구 수치
                        if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430114) >= 30) {//필요한 아이템
                            if (MapleInventoryManipulator.checkSpace(c, 2049300, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 30, true, false)) {//얻을템/뺏을템
                                MapleInventoryManipulator.addById(c, 2049300, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());//얻을템
                                c.getPlayer().dropMessage(5, "플래티넘 미라클 큐브 조각을 사용 해 고급 장비강화 주문서 1개를 획득 했습니다.");
                            } else {
                                c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "플래티넘 미라클 큐브 조각 30개를 고급 장비강화 주문서 1개로 교환 가능합니다.");
                        }
                    }
                    break;
                case 2430187: //플래티넘 미라클 큐브 교환권
                    //이게 범인이네 
                    if (c.getPlayer().getInventory(MapleInventoryType.CASH).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430187) >= 1) {
                            if (MapleInventoryManipulator.checkSpace(c, 5062003, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false)) {
                                MapleInventoryManipulator.addById(c, 5062003, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                                c.getPlayer().dropMessage(5, "플래티넘 미라클 큐브 교환권을 사용 해 플래티넘 미라클 큐브 1개를 획득 했습니다.");
                            } else {
                                c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "큐브 교환권은 사용 시 큐브 1개로 교환이 가능합니다.");
                        }
                    }
                    break;
                case 2430188: //플래티넘 미라클 큐브 10개 교환권
                    if (c.getPlayer().getInventory(MapleInventoryType.CASH).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430188) >= 1) {
                            if (MapleInventoryManipulator.checkSpace(c, 5062003, 10, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false)) {
                                MapleInventoryManipulator.addById(c, 5062003, (short) 10, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                                c.getPlayer().dropMessage(5, "플래티넘 미라클 큐브 10개 교환권을 사용 해 플래티넘 미라클 큐브 10개를 획득 했습니다.");
                            } else {
                                c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "플래티넘 미라클 큐브 10개 교환권은 사용 시 큐브 10개로 교환이 가능합니다.");
                        }
                    }
                    break;
                case 2430189: //플래티넘 미라클 큐브 20개 교환권
                    if (c.getPlayer().getInventory(MapleInventoryType.CASH).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430189) >= 1) {
                            if (MapleInventoryManipulator.checkSpace(c, 5062003, 20, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false)) {
                                MapleInventoryManipulator.addById(c, 5062003, (short) 20, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                                c.getPlayer().dropMessage(5, "플래티넘 미라클 큐브 20개 교환권을 사용 해 플래티넘 미라클 큐브 20개를 획득 했습니다.");
                            } else {
                                c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "플래티넘 미라클 큐브 20개 교환권은 사용 시 큐브 10개로 교환이 가능합니다.");
                        }
                    }
                    break;
                case 2430113: //마스터 미라클 큐브 조각
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {//인벤 필요 요구 수치
                        if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430113) >= 30) {//필요한 아이템
                            if (MapleInventoryManipulator.checkSpace(c, 2049300, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 30, true, false)) {//얻을템/뺏을템
                                MapleInventoryManipulator.addById(c, 2049300, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());//얻을템
                                c.getPlayer().dropMessage(5, "마스터 미라클 큐브 조각을 사용 해 고급 장비강화 주문서 1개를 획득 했습니다.");
                            } else {
                                c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "마스터 미라클 큐브 조각 30개를 고급 장비강화 주문서 1개로 교환 가능합니다.");
                        }
                    }
                    break;
                case 2430183: //마스터 미라클 큐브 교환권
                    if (c.getPlayer().getInventory(MapleInventoryType.CASH).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430183) >= 1) {
                            if (MapleInventoryManipulator.checkSpace(c, 5062001, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false)) {
                                MapleInventoryManipulator.addById(c, 5062001, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                                c.getPlayer().dropMessage(5, "마스터 미라클 큐브 교환권을 사용 해 마스터 미라클 큐브 1개를 획득 했습니다.");
                            } else {
                                c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "큐브 교환권은 사용 시 큐브 1개로 교환이 가능합니다.");
                        }
                    }
                    break;
                case 2430186: //마스터 미라클 큐브 10개 교환권
                    if (c.getPlayer().getInventory(MapleInventoryType.CASH).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430186) >= 1) {
                            if (MapleInventoryManipulator.checkSpace(c, 5062001, 10, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false)) {
                                MapleInventoryManipulator.addById(c, 5062001, (short) 10, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                                c.getPlayer().dropMessage(5, "마스터 미라클 큐브 10개 교환권을 사용 해 마스터 미라클 큐브 10개를 획득 했습니다.");
                            } else {
                                c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "마스터 미라클 큐브 10개 교환권은 사용 시 큐브 10개로 교환이 가능합니다.");
                        }
                    }
                    break;
                case 2430112: //레드 큐브 조각
                    //c.getPlayer().dropMessage(5, "미라클 큐브 조각 5개로 잠재능력 부여 주문서, 10개로 고급 잠재능력 부여 주문서 교환이 가능합니다.");
                    NPCScriptManager.getInstance().start(c, 9000030);
                    break;
                case 2430111: //미라클 큐브 조각
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 2 && c.getPlayer().getInventory(MapleInventoryType.CASH).getNumFreeSlot() >= 2) {
                        NPCScriptManager.getInstance().start(c, 9010019, "miracle_box");
                    } else {
                        c.getPlayer().dropMessage(5, "아이템창의 빈공간 부족.");
                    }
                    break;
                case 2430182: //레드 큐브 교환권
                    if (c.getPlayer().getInventory(MapleInventoryType.CASH).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430182) >= 1) {
                            if (MapleInventoryManipulator.checkSpace(c, 5062000, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false)) {
                                MapleInventoryManipulator.addById(c, 5062000, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                                c.getPlayer().dropMessage(5, "레드 큐브 교환권을 사용 해 레드 큐브 1개를 획득 했습니다.");
                            } else {
                                c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "큐브 교환권은 사용 시 큐브 1개로 교환이 가능합니다.");
                        }
                    }
                    break;
                case 2430185: //레드 큐브 10개 교환권
                    if (c.getPlayer().getInventory(MapleInventoryType.CASH).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430185) >= 1) {
                            if (MapleInventoryManipulator.checkSpace(c, 5062000, 10, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false)) {
                                MapleInventoryManipulator.addById(c, 5062000, (short) 10, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                                c.getPlayer().dropMessage(5, "레드 큐브 10개 교환권을 사용 해 레드 큐브 10개를 획득 했습니다.");
                            } else {
                                c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "레드 큐브 10개 교환권은 사용 시 큐브 10개로 교환이 가능합니다.");
                        }
                    }
                    break;
                case 2430181: //미라클 큐브 교환권
                    if (c.getPlayer().getInventory(MapleInventoryType.CASH).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430181) >= 1) {
                            if (MapleInventoryManipulator.checkSpace(c, 5062002, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false)) {
                                MapleInventoryManipulator.addById(c, 5062002, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                                c.getPlayer().dropMessage(5, "미라클 큐브 교환권을 사용 해 미라클 큐브 1개를 획득 했습니다.");
                            } else {
                                c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "큐브 교환권은 사용 시 큐브 1개로 교환이 가능합니다.");
                        }
                    }
                    break;
                case 2430184: //미라클 큐브 10개 교환권
                    if (c.getPlayer().getInventory(MapleInventoryType.CASH).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430184) >= 1) {
                            if (MapleInventoryManipulator.checkSpace(c, 5062002, 10, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false)) {
                                MapleInventoryManipulator.addById(c, 5062002, (short) 10, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                                c.getPlayer().dropMessage(5, "미라클 큐브 10개 교환권을 사용 해 미라클 큐브 10개를 획득 했습니다.");
                            } else {
                                c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "미라클 큐브 10개 교환권은 사용 시 큐브 10개로 교환이 가능합니다.");
                        }
                    }
                    break;
                case 5620000: //슬래시스톰20
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.CASH).countById(5620000) >= 1) {
                            if (MapleInventoryManipulator.checkSpace(c, 2290153, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, toUse.getItemId(), 1, true, false)) {
                                MapleInventoryManipulator.addById(c, 2290153, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                                c.getPlayer().dropMessage(5, "마스터리를 사용해 마스터리북을 획득했습니다.");
                            } else {
                                c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "마스터리를 사용해 마스터리북을 획득이 가능합니다.");
                        }
                    }
                    break;
                case 5620001: //토네이도 스핀
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.CASH).countById(5620001) >= 1) {
                            if (MapleInventoryManipulator.checkSpace(c, 2290154, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, toUse.getItemId(), 1, true, false)) {
                                MapleInventoryManipulator.addById(c, 2290154, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                                c.getPlayer().dropMessage(5, "마스터리를 사용해 마스터리북을 획득했습니다.");
                            } else {
                                c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "마스터리를 사용해 마스터리북을 획득이 가능합니다.");
                        }
                    }
                    break;
                case 5620002: //미러 이미징
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.CASH).countById(5620002) >= 1) {
                            if (MapleInventoryManipulator.checkSpace(c, 2290155, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, toUse.getItemId(), 1, true, false)) {
                                MapleInventoryManipulator.addById(c, 2290155, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                                c.getPlayer().dropMessage(5, "마스터리를 사용해 마스터리북을 획득했습니다.");
                            } else {
                                c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "마스터리를 사용해 마스터리북을 획득이 가능합니다.");
                        }
                    }
                    break;
                case 5620003: //플라잉 어썰터
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.CASH).countById(5620003) >= 1) {
                            if (MapleInventoryManipulator.checkSpace(c, 2290156, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, toUse.getItemId(), 1, true, false)) {
                                MapleInventoryManipulator.addById(c, 2290156, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                                c.getPlayer().dropMessage(5, "마스터리를 사용해 마스터리북을 획득했습니다.");
                            } else {
                                c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "마스터리를 사용해 마스터리북을 획득이 가능합니다.");
                        }
                    }
                    break;
                case 5620004: //써든 레이드
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.CASH).countById(5620004) >= 1) {
                            if (MapleInventoryManipulator.checkSpace(c, 2290159, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, toUse.getItemId(), 1, true, false)) {
                                MapleInventoryManipulator.addById(c, 2290159, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                                c.getPlayer().dropMessage(5, "마스터리를 사용해 마스터리북을 획득했습니다.");
                            } else {
                                c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "마스터리를 사용해 마스터리북을 획득이 가능합니다.");
                        }
                    }
                    break;
                case 5620005: //쏜즈 이펙트
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.CASH).countById(5620005) >= 1) {
                            if (MapleInventoryManipulator.checkSpace(c, 2290161, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, toUse.getItemId(), 1, true, false)) {
                                MapleInventoryManipulator.addById(c, 2290161, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                                c.getPlayer().dropMessage(5, "마스터리를 사용해 마스터리북을 획득했습니다.");
                            } else {
                                c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "마스터리를 사용해 마스터리북을 획득이 가능합니다.");
                        }
                    }
                    break;
                case 5530024: //설지도
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.CASH).countById(5530024) >= 1) {
                            if (MapleInventoryManipulator.checkSpace(c, 1342023, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, toUse.getItemId(), 1, true, false)) {
                                MapleInventoryManipulator.addById(c, 1342023, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                                c.getPlayer().dropMessage(5, "교환권을 사용해 장비 아이템을 획득했습니다.");
                            } else {
                                c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "교환권을 사용해 장비 아이템 획득이 가능합니다.");
                        }
                    }
                    break;
                case 5530023: //설천도
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.CASH).countById(5530023) >= 1) {
                            if (MapleInventoryManipulator.checkSpace(c, 1332112, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, toUse.getItemId(), 1, true, false)) {
                                MapleInventoryManipulator.addById(c, 1332112, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                                c.getPlayer().dropMessage(5, "교환권을 사용해 장비 아이템을 획득했습니다.");
                            } else {
                                c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "교환권을 사용해 장비 아이템 획득이 가능합니다.");
                        }
                    }
                    break;
                case 5530026: //화설지도
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.CASH).countById(5530026) >= 1) {
                            if (MapleInventoryManipulator.checkSpace(c, 1342024, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, toUse.getItemId(), 1, true, false)) {
                                MapleInventoryManipulator.addById(c, 1342024, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                                c.getPlayer().dropMessage(5, "교환권을 사용해 장비 아이템을 획득했습니다.");
                            } else {
                                c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "교환권을 사용해 장비 아이템 획득이 가능합니다.");
                        }
                    }
                    break;
                case 5530025: //화설천도
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.CASH).countById(5530025) >= 1) {
                            if (MapleInventoryManipulator.checkSpace(c, 1332113, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, toUse.getItemId(), 1, true, false)) {
                                MapleInventoryManipulator.addById(c, 1332113, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                                c.getPlayer().dropMessage(5, "교환권을 사용해 장비 아이템을 획득했습니다.");
                            } else {
                                c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "교환권을 사용해 장비 아이템 획득이 가능합니다.");
                        }
                    }
                    break;
                case 5530017: //홍아의 지천도
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.CASH).countById(5530017) >= 1) {
                            if (MapleInventoryManipulator.checkSpace(c, 1342021, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, toUse.getItemId(), 1, true, false)) {
                                MapleInventoryManipulator.addById(c, 1342021, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                                c.getPlayer().dropMessage(5, "교환권을 사용해 장비 아이템을 획득했습니다.");
                            } else {
                                c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "교환권을 사용해 장비 아이템 획득이 가능합니다.");
                        }
                    }
                    break;
                case 5530018: //홍아의 패왕도
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.CASH).countById(5530018) >= 1) {
                            if (MapleInventoryManipulator.checkSpace(c, 1342022, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, toUse.getItemId(), 1, true, false)) {
                                MapleInventoryManipulator.addById(c, 1342022, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                                c.getPlayer().dropMessage(5, "교환권을 사용해 장비 아이템을 획득했습니다.");
                            } else {
                                c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "교환권을 사용해 장비 아이템 획득이 가능합니다.");
                        }
                    }
                    break;
                case 5530019: //홍아의 신기타
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.CASH).countById(5530019) >= 1) {
                            if (MapleInventoryManipulator.checkSpace(c, 1332111, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, toUse.getItemId(), 1, true, false)) {
                                MapleInventoryManipulator.addById(c, 1332111, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                                c.getPlayer().dropMessage(5, "교환권을 사용해 장비 아이템을 획득했습니다.");
                            } else {
                                c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "교환권을 사용해 장비 아이템 획득이 가능합니다.");
                        }
                    }
                    break;
                case 5680019: {//starling hair 
                    //if (c.getPlayer().getGender() == 1) {
                    int hair = 32150 + (c.getPlayer().getHair() % 10);
                    c.getPlayer().setHair(hair);
                    c.getPlayer().updateSingleStat(MapleStat.HAIR, hair);
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, slot, (byte) 1, false);
                    //}
                    break;
                }
                case 5680020: {//starling hair 
                    //if (c.getPlayer().getGender() == 0) {
                    int hair = 32160 + (c.getPlayer().getHair() % 10);
                    c.getPlayer().setHair(hair);
                    c.getPlayer().updateSingleStat(MapleStat.HAIR, hair);
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, slot, (byte) 1, false);
                    //}
                    break;
                }
                case 3994225:
                    c.getPlayer().dropMessage(5, "Please bring this item to the NPC.");
                    break;
                case 2430212: //energy drink
                    MapleQuestStatus marr = c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.ENERGY_DRINK));
                    if (marr.getCustomData() == null) {
                        marr.setCustomData("0");
                    }
                    long lastTime = Long.parseLong(marr.getCustomData());
                    if (lastTime + (600000) > System.currentTimeMillis()) {
                        c.getPlayer().dropMessage(5, "You can only use one energy drink per 10 minutes.");
                    } else if (c.getPlayer().getFatigue() > 0) {
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                        c.getPlayer().setFatigue(c.getPlayer().getFatigue() - 5);
                    }
                    break;
                case 2430213: //energy drink
                    marr = c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.ENERGY_DRINK));
                    if (marr.getCustomData() == null) {
                        marr.setCustomData("0");
                    }
                    lastTime = Long.parseLong(marr.getCustomData());
                    if (lastTime + (600000) > System.currentTimeMillis()) {
                        c.getPlayer().dropMessage(5, "You can only use one energy drink per 10 minutes.");
                    } else if (c.getPlayer().getFatigue() > 0) {
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                        c.getPlayer().setFatigue(c.getPlayer().getFatigue() - 10);
                    }
                    break;
                case 2430220: //energy drink
                case 2430214: //energy drink
                    if (c.getPlayer().getFatigue() > 0) {
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                        c.getPlayer().setFatigue(c.getPlayer().getFatigue() - 30);
                    }
                    break;
                case 2430227: //energy drink
                    if (c.getPlayer().getFatigue() > 0) {
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                        c.getPlayer().setFatigue(c.getPlayer().getFatigue() - 50);
                    }
                    break;
                case 2430231: //energy drink
                    marr = c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.ENERGY_DRINK));
                    if (marr.getCustomData() == null) {
                        marr.setCustomData("0");
                    }
                    lastTime = Long.parseLong(marr.getCustomData());
                    if (lastTime + (600000) > System.currentTimeMillis()) {
                        c.getPlayer().dropMessage(5, "You can only use one energy drink per 10 minutes.");
                    } else if (c.getPlayer().getFatigue() > 0) {
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                        c.getPlayer().setFatigue(c.getPlayer().getFatigue() - 40);
                    }
                    break;
                case 2003001: //피작
                    if (c.getPlayer().getStat().maxhp <= 500000) {
                        c.getPlayer().getStat().maxhp = c.getPlayer().getStat().maxhp + 10;
                        statup.put(MapleStat.MAXHP, Integer.valueOf(c.getPlayer().getStat().maxhp));
                        c.getPlayer().getStat().recalcLocalStats(c.getPlayer());
                        c.getSession().write(MaplePacketCreator.updatePlayerStats(statup, c.getPlayer().getJob()));
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                        c.getPlayer().dropMessage(5, "최대 체력이 10 늘었습니다.");
                    } else {
                        c.getPlayer().dropMessage(5, "맥스 체력 입니다.");
                    }
                    break;
                case 2003002: //엠작
                    if (c.getPlayer().getStat().maxmp <= 500000) {
                        c.getPlayer().getStat().maxmp = c.getPlayer().getStat().maxmp + 10;
                        statup.put(MapleStat.MAXMP, Integer.valueOf(c.getPlayer().getStat().maxmp));
                        c.getPlayer().getStat().recalcLocalStats(c.getPlayer());
                        c.getSession().write(MaplePacketCreator.updatePlayerStats(statup, c.getPlayer().getJob()));
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                        c.getPlayer().dropMessage(5, "최대 엠피가 10 늘었습니다.");
                    } else {
                        c.getPlayer().dropMessage(5, "맥스 엠피 입니다.");
                    }
                    break;
                case 2433928: //신비의 마스터리북
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                        NPCScriptManager.getInstance().start(c, 9000311, "consume_2433928");
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    } else {
                        c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                    }
                    break;
                case 2430098: //Lv.10 뉴비 상자
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1 && c.getPlayer().getJob() != 0 && c.getPlayer().getJob() != 1000 && c.getPlayer().getJob() != 3000) {
                        NPCScriptManager.getInstance().start(c, 9010019, "newbi_box");
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    } else {
                        c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없거나 초보자는 사용할수 없습니다. 전직을 해주세요");
                    }
                    break;
                case 2433927: //스윗박스
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 2 && c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() >= 2) {
                        NPCScriptManager.getInstance().start(c, 9010019, "rebol_box");
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    } else {
                        c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                    }
                    break;
                case 2000034: //원클릭피넛
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 2 && c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() >= 2) {
                        NPCScriptManager.getInstance().start(c, 9010019, "peanut");
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    } else {
                        c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                    }
                    break;
                case 2430028: //핫타임박스
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 2 && c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() >= 2) {
                        NPCScriptManager.getInstance().start(c, 9010019, "hotz_box");
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    } else {
                        c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                    }
                    break;
                case 2430016: //아이스박스(여름이벤트)
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1 && c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() >= 2) {
                        NPCScriptManager.getInstance().start(c, 9010019, "ice_box");
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    } else {
                        c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                    }
                    break;
                case 2430099: //Lv.30 반지 상자
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1 && c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() >= 2) {
                        NPCScriptManager.getInstance().start(c, 9010019, "3ring_box");
                        //MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    } else {
                        c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                    }
                    break;
                case 2430144: //비밀의 마스터리 북
                    final int itemid = Randomizer.nextInt(284) + 2290000;//마스터리북 코드 결정하는 곳
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {//인벤 필요 요구 수치
                        MapleInventoryManipulator.addById(c, itemid, (short) 1, "Reward item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    } else {
                        c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                        break;
                    }
                case 2434746: //월요일 몬파상자
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1 && c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() >= 2) {
                        NPCScriptManager.getInstance().start(c, 9010019, "mon_box");
                        //MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    } else {
                        c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                    }
                    break;
                case 2434747: //화요일 몬파상자
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1 && c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() >= 2) {
                        NPCScriptManager.getInstance().start(c, 9010019, "tue_box");
                        //MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    } else {
                        c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                    }
                    break;
                case 2434748: //수요일 몬파상자
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1 && c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() >= 2) {
                        NPCScriptManager.getInstance().start(c, 9010019, "wed_box");
                        //MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    } else {
                        c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                    }
                    break;
                case 2434749: //목요일 몬파상자
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1 && c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() >= 2) {
                        NPCScriptManager.getInstance().start(c, 9010019, "thu_box");
                        //MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    } else {
                        c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                    }
                    break;
                case 2434750: //금요일 몬파상자
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1 && c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() >= 2) {
                        NPCScriptManager.getInstance().start(c, 9010019, "fri_box");
                        //MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    } else {
                        c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                    }
                    break;
                case 2434751: //토요일 몬파상자
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1 && c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() >= 2) {
                        NPCScriptManager.getInstance().start(c, 9010019, "sat_box");
                        //MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    } else {
                        c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                    }
                    break;
                case 2434745: //일요일 몬파상자
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1 && c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() >= 2) {
                        NPCScriptManager.getInstance().start(c, 9010019, "sun_box");
                        //MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    } else {
                        c.getPlayer().dropMessage(5, "인벤토리에 빈 공간이 없습니다.");
                    }
                    break;
                case 2430370:
                    if (MapleInventoryManipulator.checkSpace(c, 2028062, (short) 1, "")) {
                        MapleInventoryManipulator.addById(c, 2028062, (short) 1, "Reward item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    }
                    break;
                case 2430158: //lion king
                    if (c.getPlayer().getInventory(MapleInventoryType.ETC).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.ETC).countById(4000630) >= 100) {
                            if (MapleInventoryManipulator.checkSpace(c, 4310010, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false)) {
                                MapleInventoryManipulator.addById(c, 4310010, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                            } else {
                                c.getPlayer().dropMessage(5, "Please make some space.");
                            }
                        } else if (c.getPlayer().getInventory(MapleInventoryType.ETC).countById(4000630) >= 50) {
                            if (MapleInventoryManipulator.checkSpace(c, 4310009, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false)) {
                                MapleInventoryManipulator.addById(c, 4310009, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                            } else {
                                c.getPlayer().dropMessage(5, "Please make some space.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "There needs to be 50 Purification Totems for a Noble Lion King Medal, 100 for Royal Lion King Medal.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "Please make some space.");
                    }
                    break;
                case 2430200: //thunder stone
                    if (c.getPlayer().getQuestStatus(31152) != 2) {
                        c.getPlayer().dropMessage(5, "아직 제작 방법을 획득하지 않았습니다.");
                    } else {
                        if (c.getPlayer().getInventory(MapleInventoryType.ETC).getNumFreeSlot() >= 1) {
                            if (c.getPlayer().getInventory(MapleInventoryType.ETC).countById(4000660) >= 1 && c.getPlayer().getInventory(MapleInventoryType.ETC).countById(4000661) >= 1 && c.getPlayer().getInventory(MapleInventoryType.ETC).countById(4000662) >= 1 && c.getPlayer().getInventory(MapleInventoryType.ETC).countById(4000663) >= 1) {
                                if (MapleInventoryManipulator.checkSpace(c, 4032923, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false) && MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, 4000660, 1, true, false) && MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, 4000661, 1, true, false) && MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, 4000662, 1, true, false) && MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, 4000663, 1, true, false)) {
                                    MapleInventoryManipulator.addById(c, 4032923, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                                } else {
                                    c.getPlayer().dropMessage(5, "이미 꿈의 열쇠를 소지하고 있거나 인벤토리칸이 부족합니다.");
                                }
                            } else {
                                c.getPlayer().dropMessage(5, "꿈의 열쇠 제작에 필요한 재료를 소지하고 있지않습니다.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "이미 꿈의 열쇠를 소지하고 있거나 인벤토리칸이 부족합니다.");
                        }
                    }
                    break;
                case 2430130:
                case 2430131: //energy charge
                    if (!GameConstants.isResist(c.getPlayer().getJob())) {
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                        c.getPlayer().gainExp(20000 + (c.getPlayer().getLevel() * 50 * c.getChannelServer().getExpRate()), true, true, false);
                    } else {
                        c.getPlayer().dropMessage(5, "You may not use this item.");
                    }
                    break;
                case 2430132:
                case 2430133:
                case 2430134: //resistance box
                case 2430142:
                    if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getJob() == 3200 || c.getPlayer().getJob() == 3210 || c.getPlayer().getJob() == 3211 || c.getPlayer().getJob() == 3212) {
                            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                            MapleInventoryManipulator.addById(c, 1382101, (short) 1, "Scripted item: " + itemId + " on " + FileoutputUtil.CurrentReadable_Date());
                        } else if (c.getPlayer().getJob() == 3300 || c.getPlayer().getJob() == 3310 || c.getPlayer().getJob() == 3311 || c.getPlayer().getJob() == 3312) {
                            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                            MapleInventoryManipulator.addById(c, 1462093, (short) 1, "Scripted item: " + itemId + " on " + FileoutputUtil.CurrentReadable_Date());
                        } else if (c.getPlayer().getJob() == 3500 || c.getPlayer().getJob() == 3510 || c.getPlayer().getJob() == 3511 || c.getPlayer().getJob() == 3512) {
                            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                            MapleInventoryManipulator.addById(c, 1492080, (short) 1, "Scripted item: " + itemId + " on " + FileoutputUtil.CurrentReadable_Date());
                        } else {
                            c.getPlayer().dropMessage(5, "You may not use this item.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "Make some space.");
                    }
                    break;
                case 2430036: //croco 1 day
                    mountid = 1027;
                    expiration_days = 1;
                    break;
                case 2430170: //croco 7 day
                    mountid = 1027;
                    expiration_days = 7;
                    break;
                case 2430037: //바이크7일권
                    mountid = 1028;
                    expiration_days = 7;
                    break;
                case 2430038: //스쿠터7일
                    mountid = 1029;
                    expiration_days = 7;
                    break;
                case 2430039: //근두운
                    mountid = 1030;
                    expiration_days = 7;
                    break;
                case 2430040: //balrog 1 day
                    mountid = 1031;
                    expiration_days = 1;
                    break;
                case 2430223: //balrog 1 day
                    mountid = 1031;
                    expiration_days = 15;
                    break;
                case 2430259: //balrog 1 day
                    mountid = 1031;
                    expiration_days = 3;
                    break;
                case 2430242: //motorcycle
                    mountid = 80001018;
                    expiration_days = 10;
                    break;
                case 2430243: //power suit
                    mountid = 80001019;
                    expiration_days = 10;
                    break;
                case 2430261: //power suit
                    mountid = 80001019;
                    expiration_days = 3;
                    break;
                case 2430249: //motorcycle
                    mountid = 80001027;
                    expiration_days = 3;
                    break;
                case 2430225: //balrog 1 day
                    mountid = 1031;
                    expiration_days = 10;
                    break;
                case 2430053: //croco 30 day
                    mountid = 1027;
                    expiration_days = 1;
                    break;
                case 2430054: //black scooter 30 day
                    mountid = 1028;
                    expiration_days = 30;
                    break;
                case 2430055: //pink scooter 30 day
                    mountid = 1029;
                    expiration_days = 30;
                    break;
                case 2430257: //스포츠카
                    mountid = 1029;
                    expiration_days = 7;
                    break;
                case 2430056: //주발
                    mountid = 1035;
                    expiration_days = 7;
                    break;
                case 2430057: //스포츠카
                    mountid = 1033;
                    expiration_days = 7;
                    break;
                case 2430072: //ZD tiger 7 day
                    mountid = 1034;
                    expiration_days = 7;
                    break;
                case 2430101: //핑크곰 열기구 7일권
                    mountid = 1052;
                    expiration_days = 7;
                    break;
                case 2430073: //lion 15 day
                    mountid = 1036;
                    expiration_days = 15;
                    break;
                case 2430074: //unicorn 15 day
                    mountid = 1037;
                    expiration_days = 15;
                    break;
                case 2430272: //low rider 15 day
                    mountid = 1038;
                    expiration_days = 3;
                    break;
                case 2430275: //spiegelmann
                    mountid = 80001033;
                    expiration_days = 7;
                    break;
                case 2430075: //low rider 15 day
                    mountid = 1038;
                    expiration_days = 15;
                    break;
                case 2430076: //red truck 15 day
                    mountid = 1039;
                    expiration_days = 15;
                    break;
                case 2430077: //gargoyle 15 day
                    mountid = 1040;
                    expiration_days = 15;
                    break;
                case 2430080: //신조 7일
                    mountid = 1042;
                    expiration_days = 7;
                    break;
                case 2430082: //orange mush 7 day
                    mountid = 1044;
                    expiration_days = 7;
                    break;
                case 2430260: //orange mush 7 day
                    mountid = 1044;
                    expiration_days = 3;
                    break;
                case 2430091: //나이트메어 7일
                    mountid = 1049;
                    expiration_days = 7;
                    break;
                case 2430092: //예티7
                    mountid = 1050;
                    expiration_days = 7;
                    break;
                case 2430263: //yeti 10 day
                    mountid = 1050;
                    expiration_days = 3;
                    break;
                case 2430093: //타조7일
                    mountid = 1051;
                    expiration_days = 7;
                    break;
                case 2430103: //chicken 30 day
                    mountid = 1054;
                    expiration_days = 30;
                    break;
                case 2430266: //chicken 30 day
                    mountid = 1054;
                    expiration_days = 3;
                    break;
                case 2430265: //chariot
                    mountid = 1151;
                    expiration_days = 3;
                    break;
                case 2430258: //law officer
                    mountid = 1115;
                    expiration_days = 365;
                    break;
                case 2430117: //lion 1 year
                    mountid = 1036;
                    expiration_days = 365;
                    break;
                case 2430118: //red truck 1 year
                    mountid = 1039;
                    expiration_days = 365;
                    break;
                case 2430119: //gargoyle 1 year
                    mountid = 1040;
                    expiration_days = 365;
                    break;
                case 2430120: //unicorn 1 year
                    mountid = 1037;
                    expiration_days = 365;
                    break;
                case 2430271: //owl 30 day
                    mountid = 1069;
                    expiration_days = 3;
                    break;
                case 2430136: //owl 30 day
                    mountid = 1069;
                    expiration_days = 30;
                    break;
                case 2430137: //owl 1 year
                    mountid = 1069;
                    expiration_days = 365;
                    break;
                case 2430145: //mothership
                    mountid = 1070;
                    expiration_days = 30;
                    break;
                case 2430146: //mothership
                    mountid = 1070;
                    expiration_days = 365;
                    break;
                case 2430147: //mothership
                    mountid = 1071;
                    expiration_days = 30;
                    break;
                case 2430148: //mothership
                    mountid = 1071;
                    expiration_days = 365;
                    break;
                case 2430135: //os4
                    mountid = 1065;
                    expiration_days = 15;
                    break;
                case 2430149: //레오나르도 7일
                    mountid = 1072;
                    expiration_days = 7;
                    break;
                case 2430262: //leonardo 30 day
                    mountid = 1072;
                    expiration_days = 3;
                    break;
                case 2430179: //witch 15 day
                    mountid = 1081;
                    expiration_days = 15;
                    break;
                case 2430264: //witch 15 day
                    mountid = 1081;
                    expiration_days = 3;
                    break;
                case 2430201: //giant bunny 60 day
                    mountid = 1096;
                    expiration_days = 60;
                    break;
                case 2430228: //tiny bunny 60 day
                    mountid = 1101;
                    expiration_days = 60;
                    break;
                case 2430276: //tiny bunny 60 day
                    mountid = 1101;
                    expiration_days = 15;
                    break;
                case 2430277: //tiny bunny 60 day
                    mountid = 1101;
                    expiration_days = 365;
                    break;
                case 2430283: //trojan
                    mountid = 1025;
                    expiration_days = 10;
                    break;
                case 2430291: //hot air
                    mountid = 1145;
                    expiration_days = -1;
                    break;
                case 2430293: //nadeshiko
                    mountid = 1146;
                    expiration_days = -1;
                    break;
                case 2430295: //pegasus
                    mountid = 1147;
                    expiration_days = -1;
                    break;
                case 2430297: //dragon
                    mountid = 1148;
                    expiration_days = -1;
                    break;
                case 2430299: //broom
                    mountid = 1149;
                    expiration_days = -1;
                    break;
                case 2430301: //cloud
                    mountid = 1150;
                    expiration_days = -1;
                    break;
                case 2430303: //chariot
                    mountid = 1151;
                    expiration_days = -1;
                    break;
                case 2430305: //nightmare
                    mountid = 1152;
                    expiration_days = -1;
                    break;
                case 2430307: //rog
                    mountid = 1153;
                    expiration_days = -1;
                    break;
                case 2430309: //mist rog
                    mountid = 1154;
                    expiration_days = -1;
                    break;
                case 2430311: //owl
                    mountid = 1156;
                    expiration_days = -1;
                    break;
                case 2430313: //helicopter
                    mountid = 1156;
                    expiration_days = -1;
                    break;
                case 2430315: //pentacle
                    mountid = 1118;
                    expiration_days = -1;
                    break;
                case 2430317: //frog
                    mountid = 1121;
                    expiration_days = -1;
                    break;
                case 2430319: //turtle
                    mountid = 1122;
                    expiration_days = -1;
                    break;
                case 2430321: //buffalo
                    mountid = 1123;
                    expiration_days = -1;
                    break;
                case 2430323: //tank
                    mountid = 1124;
                    expiration_days = -1;
                    break;
                case 2430325: //viking
                    mountid = 1129;
                    expiration_days = -1;
                    break;
                case 2430327: //pachinko
                    mountid = 1130;
                    expiration_days = -1;
                    break;
                case 2430329: //kurenai
                    mountid = 1063;
                    expiration_days = -1;
                    break;
                case 2430331: //horse
                    mountid = 1025;
                    expiration_days = -1;
                    break;
                case 2430333: //tiger
                    mountid = 1034;
                    expiration_days = -1;
                    break;
                case 2430335: //hyena
                    mountid = 1136;
                    expiration_days = -1;
                    break;
                case 2430337: //ostrich
                    mountid = 1051;
                    expiration_days = -1;
                    break;
                case 2430339: //low rider
                    mountid = 1138;
                    expiration_days = -1;
                    break;
                case 2430341: //napoleon
                    mountid = 1139;
                    expiration_days = -1;
                    break;
                case 2430343: //croking
                    mountid = 1027;
                    expiration_days = -1;
                    break;
                case 2430346: //lovely
                    mountid = 1029;
                    expiration_days = -1;
                    break;
                case 2430348: //retro
                    mountid = 1028;
                    expiration_days = -1;
                    break;
                case 2430350: //f1
                    mountid = 1033;
                    expiration_days = -1;
                    break;
                case 2430352: //power suit
                    mountid = 1064;
                    expiration_days = -1;
                    break;
                case 2430354: //giant rabbit
                    mountid = 1096;
                    expiration_days = -1;
                    break;
                case 2430356: //small rabit
                    mountid = 1101;
                    expiration_days = -1;
                    break;
                case 2430358: //rabbit rickshaw
                    mountid = 1102;
                    expiration_days = -1;
                    break;
                case 2430360: //chicken
                    mountid = 1054;
                    expiration_days = -1;
                    break;
                case 2430362: //transformer
                    mountid = 1053;
                    expiration_days = -1;
                    break;
                case 2430292: //hot air
                    mountid = 1145;
                    expiration_days = 90;
                    break;
                case 2430294: //nadeshiko
                    mountid = 1146;
                    expiration_days = 90;
                    break;
                case 2430296: //pegasus
                    mountid = 1147;
                    expiration_days = 90;
                    break;
                case 2430298: //dragon
                    mountid = 1148;
                    expiration_days = 90;
                    break;
                case 2430300: //broom
                    mountid = 1149;
                    expiration_days = 90;
                    break;
                case 2430302: //cloud
                    mountid = 1150;
                    expiration_days = 90;
                    break;
                case 2430304: //chariot
                    mountid = 1151;
                    expiration_days = 90;
                    break;
                case 2430306: //nightmare
                    mountid = 1152;
                    expiration_days = 90;
                    break;
                case 2430308: //rog
                    mountid = 1153;
                    expiration_days = 90;
                    break;
                case 2430310: //mist rog
                    mountid = 1154;
                    expiration_days = 90;
                    break;
                case 2430312: //owl
                    mountid = 1156;
                    expiration_days = 90;
                    break;
                case 2430314: //helicopter
                    mountid = 1156;
                    expiration_days = 90;
                    break;
                case 2430316: //pentacle
                    mountid = 1118;
                    expiration_days = 90;
                    break;
                case 2430318: //frog
                    mountid = 1121;
                    expiration_days = 90;
                    break;
                case 2430320: //turtle
                    mountid = 1122;
                    expiration_days = 90;
                    break;
                case 2430322: //buffalo
                    mountid = 1123;
                    expiration_days = 90;
                    break;
                case 2430326: //viking
                    mountid = 1129;
                    expiration_days = 90;
                    break;
                case 2430328: //pachinko
                    mountid = 1130;
                    expiration_days = 90;
                    break;
                case 2430330: //kurenai
                    mountid = 1063;
                    expiration_days = 90;
                    break;
                case 2430332: //horse
                    mountid = 1025;
                    expiration_days = 90;
                    break;
                case 2430334: //tiger
                    mountid = 1034;
                    expiration_days = 90;
                    break;
                case 2430336: //hyena
                    mountid = 1136;
                    expiration_days = 90;
                    break;
                case 2430338: //ostrich
                    mountid = 1051;
                    expiration_days = 90;
                    break;
                case 2430340: //low rider
                    mountid = 1138;
                    expiration_days = 90;
                    break;
                case 2430342: //napoleon
                    mountid = 1139;
                    expiration_days = 90;
                    break;
                case 2430344: //croking
                    mountid = 1027;
                    expiration_days = 90;
                    break;
                case 2430347: //lovely
                    mountid = 1029;
                    expiration_days = 90;
                    break;
                case 2430349: //retro
                    mountid = 1028;
                    expiration_days = 90;
                    break;
                case 2430351: //f1
                    mountid = 1033;
                    expiration_days = 90;
                    break;
                case 2430353: //power suit
                    mountid = 1064;
                    expiration_days = 90;
                    break;
                case 2430355: //giant rabbit
                    mountid = 1096;
                    expiration_days = 90;
                    break;
                case 2430357: //small rabit
                    mountid = 1101;
                    expiration_days = 90;
                    break;
                case 2430359: //rabbit rickshaw
                    mountid = 1102;
                    expiration_days = 90;
                    break;
                case 2430361: //chicken
                    mountid = 1054;
                    expiration_days = 90;
                    break;
                case 2430363: //transformer
                    mountid = 1053;
                    expiration_days = 90;
                    break;
                case 2430324: //high way
                    mountid = 1158;
                    expiration_days = -1;
                    break;
                case 2430345: //high way
                    mountid = 1158;
                    expiration_days = 90;
                    break;
                case 2430367: //law off
                    mountid = 1115;
                    expiration_days = 3;
                    break;
                case 2430365: //pony
                    mountid = 1025;
                    expiration_days = 365;
                    break;
                case 2430366: //pony
                    mountid = 1025;
                    expiration_days = 15;
                    break;
                case 2430369: //nightmare
                    mountid = 1049;
                    expiration_days = 10;
                    break;
                case 2430392: //speedy
                    mountid = 80001038;
                    expiration_days = 90;
                    break;
                case 2430476: //red truck? but name is pegasus?
                    mountid = 1039;
                    expiration_days = 15;
                    break;
                case 2430477: //red truck? but name is pegasus?
                    mountid = 1039;
                    expiration_days = 365;
                    break;
                case 2430232: //fortune
                    mountid = 1106;
                    expiration_days = 10;
                    break;
                case 2430511: //spiegel
                    mountid = 80001033;
                    expiration_days = 15;
                    break;
                case 2430512: //rspiegel
                    mountid = 80001033;
                    expiration_days = 365;
                    break;
                case 2430536: //buddy buggy
                    mountid = 80001114;
                    expiration_days = 365;
                    break;
                case 2430537: //buddy buggy
                    mountid = 80001114;
                    expiration_days = 15;
                    break;
                case 2430229: //bunny rickshaw 60 day
                    mountid = 1102;
                    expiration_days = 60;
                    break;
                case 2430199: //santa sled
                    mountid = 1102;
                    expiration_days = 60;
                    break;
                case 2430206: //race
                    mountid = 1089;
                    expiration_days = 7;
                    break;
                case 2430211: //race
                    mountid = 80001009;
                    expiration_days = 30;
                    break;
            }
        }
        if (mountid > 0) {
            mountid = c.getPlayer().getStat().getSkillByJob(mountid, c.getPlayer().getJob());
            final int fk = GameConstants.getMountItem(mountid, c.getPlayer());
            if (GameConstants.GMS && fk > 0 && mountid < 80001000) { //TODO JUMP
                for (int i = 80001001; i < 80001999; i++) {
                    final Skill skill = SkillFactory.getSkill(i);
                    if (skill != null && GameConstants.getMountItem(skill.getId(), c.getPlayer()) == fk) {
                        mountid = i;
                        break;
                    }
                }
            }
            if (c.getPlayer().getSkillLevel(mountid) > 0) {
                c.getPlayer().dropMessage(5, "이미 스킬을 가지고 있습니다.");
            } else if (SkillFactory.getSkill(mountid) == null || GameConstants.getMountItem(mountid, c.getPlayer()) == 0) {
                c.getPlayer().dropMessage(5, "스킬을 배울 수 없습니다.");
            } else if (expiration_days > 0) {
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                c.getPlayer().changeSkillLevel(SkillFactory.getSkill(mountid), (byte) 1, (byte) 1, System.currentTimeMillis() + (long) (expiration_days * 24 * 60 * 60 * 1000));
                c.getPlayer().dropMessage(5, "스킬을 배웠습니다.");
            }
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static final void UseSummonBag(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (!chr.isAlive() || chr.hasBlockedInventory() || chr.inPVP()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        //slea.skip(4);1229
        c.getPlayer().updateTick(slea.readInt());
        final byte slot = (byte) slea.readShort();
        final int itemId = slea.readInt();
        final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse != null && toUse.getQuantity() >= 1 && toUse.getItemId() == itemId && (c.getPlayer().getMapId() < 910000000 || c.getPlayer().getMapId() > 910000022)) {
            final Map<String, Integer> toSpawn = MapleItemInformationProvider.getInstance().getEquipStats(itemId);

            if (toSpawn == null) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            MapleMonster ht = null;
            int type = 0;
            for (Entry<String, Integer> i : toSpawn.entrySet()) {
                if (i.getKey().startsWith("mob") && Randomizer.nextInt(99) <= i.getValue()) {
                    ht = MapleLifeFactory.getMonster(Integer.parseInt(i.getKey().substring(3)));
                    chr.getMap().spawnMonster_sSack(ht, chr.getPosition(), type);
                }
            }
            if (ht == null) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }

            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static final void UseTreasureChest(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final short slot = slea.readShort();
        final int itemid = slea.readInt();

        final Item toUse = chr.getInventory(MapleInventoryType.ETC).getItem((byte) slot);
        if (toUse == null || toUse.getQuantity() <= 0 || toUse.getItemId() != itemid || chr.hasBlockedInventory()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        int reward;
        int keyIDforRemoval = 0;
        String box;

        switch (toUse.getItemId()) {
            case 4280000: // Gold box
                reward = RandomRewards.getGoldBoxReward();
                keyIDforRemoval = 5490000;
                box = "Gold";
                break;
            case 4280001: // Silver box
                reward = RandomRewards.getSilverBoxReward();
                keyIDforRemoval = 5490001;
                box = "Silver";
                break;
            default: // Up to no good
                return;
        }

        // Get the quantity
        int amount = 1;
        switch (reward) {
            case 2000004:
                amount = 200; // Elixir
                break;
            case 2000005:
                amount = 100; // Power Elixir
                break;
        }
        if (chr.getInventory(MapleInventoryType.CASH).countById(keyIDforRemoval) > 0) {
            final Item item = MapleInventoryManipulator.addbyId_Gachapon(c, reward, (short) amount);

            if (item == null) {
                chr.dropMessage(5, "Please check your item inventory and see if you have a Master Key, or if the inventory is full.");
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.ETC, (byte) slot, (short) 1, true);
            MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, keyIDforRemoval, 1, true, false);
            c.getSession().write(MaplePacketCreator.getShowItemGain(reward, (short) amount, true));

            if (GameConstants.gachaponRareItem(item.getItemId()) > 0) {
                World.Broadcast.broadcastSmega(MaplePacketCreator.getGachaponMega("[" + box + " Chest] " + c.getPlayer().getName(), " : Lucky winner of Gachapon!", item, (byte) 2));
            }
        } else {
            chr.dropMessage(5, "Please check your item inventory and see if you have a Master Key, or if the inventory is full.");
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    private Map<Skill, SkillEntry> skills;

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

    public static final void UseSPResetScroll(final LittleEndianAccessor slea, final MapleClient c) {
        final MapleCharacter chr = c.getPlayer();
        c.getPlayer().updateTick(slea.readInt());
        final short slot = slea.readShort();
        if (slot < 1 && slot > 96) {//hmm
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        final Item item = chr.getInventory(MapleInventoryType.USE).getItem((byte) slot);
        final int itemId = slea.readInt();
        if (item.getItemId() / 1000 != 2500 || item == null || item.getItemId() != itemId || GameConstants.isBeginnerJob(chr.getJob())) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        final int[] spToGive = chr.getRemainingSps();
        int skillshit = 0;
        int skillLevel;
        final List<Skill> toRemove = new ArrayList<Skill>();
        for (Skill skill : chr.getSkills().keySet()) {
            if (!skill.isBeginnerSkill() && skill.getId() / 10000000 != 9) {
                skillLevel = chr.getSkillLevel(skill);
                if (skillLevel > 0) {
                    skillshit = skillLevel;
                }
                spToGive[GameConstants.getSkillBookForSkill(skill.getId())] += skillLevel;
                toRemove.add(skill);
            }
        }
        for (Skill skill : toRemove) {
            chr.changeSingleSkillLevel(skill, 0, (byte) c.getPlayer().getMasterLevel(skill), -1);
        }
        c.getSession().write(MaplePacketCreator.useSPReset(chr.getId()));
        if (skillshit == 0 && spToGive[0] == 0 && chr.getLevel() > 10) {
            if (GameConstants.isExtendedSPJob(chr.getJob())) {
                chr.dropMessage(1, "현재 직업은 SP초기화가 불가능합니다.");
            } else {
                int sp = 1;
                sp += (chr.getLevel() - (chr.getJob() / 100 % 10 == 2 ? 8 : 10)) * 3;
                if (sp < 0) {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
                sp += (chr.getJob() % 100 != 0 && chr.getJob() % 100 != 1) ? ((chr.getJob() % 10) + 1) : 0;
                if (chr.getJob() % 10 >= 2) {
                    sp += 2;
                }
                spToGive[0] = sp;
            }
        }
        chr.baseSkills();
        for (int i = 0; i < spToGive.length; i++) {
            chr.setRemainingSp(spToGive[i], i);
        }
        chr.updateSingleStat(MapleStat.AVAILABLESP, 0);//lol
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, (byte) slot, (short) 1, true);
    }

    public static final void UseCashItem(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (c.getPlayer() == null || c.getPlayer().getMap() == null || c.getPlayer().inPVP()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        //slea.skip(4);1229
        c.getPlayer().updateTick(slea.readInt());
        c.getPlayer().setScrolledPosition((short) 0);
        final byte slot = (byte) slea.readShort();
        final int itemId = slea.readInt();

        final Item toUse = c.getPlayer().getInventory(MapleInventoryType.CASH).getItem(slot);
        if (toUse == null || toUse.getItemId() != itemId || toUse.getQuantity() < 1 || c.getPlayer().hasBlockedInventory()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }

        boolean used = false, cc = false;

        switch (itemId) {
            case 5330000: //퀵배송
            {
                if (!c.getPlayer().hasBlockedInventory()) {
                    c.getPlayer().dropMessage(1, "현재 사용 불가능한 시스템입니다.");
                    //c.getPlayer().setConversation(2);
                    //c.getSession().write(MaplePacketCreator.sendDuey((byte) 0x1B, null, null));
                }
                break;
            }
            case 5043001: // NPC Teleport Rock
            case 5043000: { // NPC Teleport Rock
                final short questid = slea.readShort();
                final int npcid = slea.readInt();
                final MapleQuest quest = MapleQuest.getInstance(questid);

                if (c.getPlayer().getQuest(quest).getStatus() == 1 && quest.canComplete(c.getPlayer(), npcid)) {
                    final int mapId = MapleLifeFactory.getNPCLocation(npcid);
                    if (mapId != -1) {
                        final MapleMap map = c.getChannelServer().getMapFactory().getMap(mapId);
                        if (map.containsNPC(npcid) && !FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit()) && !FieldLimitType.VipRock.check(map.getFieldLimit()) && !c.getPlayer().isInBlockedMap()) {
                            c.getPlayer().changeMap(map, map.getPortal(0));
                        }
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(1, "Unknown error has occurred.");
                    }
                }
                break;
            }
            case 5041001:
            case 5040004:
            case 5040003:
            case 5040002:
            case 2320000: // The Teleport Rock
            case 5041000: // VIP Teleport Rock
            case 5040000: // The Teleport Rock
            case 5040001: { // Teleport Coke
                used = UseTeleRock(slea, c, itemId);
                break;
            }
            case 5450005: {
                c.getPlayer().setConversation(4);
                c.getPlayer().getStorage().sendStorage(c, 1022005);
                break;
            }
            case 5050000: { // AP Reset
                Map<MapleStat, Integer> statupdate = new EnumMap<MapleStat, Integer>(MapleStat.class);
                final int apto = GameConstants.GMS ? (int) slea.readLong() : slea.readInt();
                final int apfrom = GameConstants.GMS ? (int) slea.readLong() : slea.readInt();

                if (apto == apfrom) {
                    break; // Hack
                }
                final int job = c.getPlayer().getJob();
                final PlayerStats playerst = c.getPlayer().getStat();
                used = true;

                switch (apto) { // AP to
                    case 64: // str
                        if (playerst.getStr() >= 999) {
                            used = false;
                        }
                        break;
                    case 128: // dex
                        if (playerst.getDex() >= 999) {
                            used = false;
                        }
                        break;
                    case 256: // int
                        if (playerst.getInt() >= 999) {
                            used = false;
                        }
                        break;
                    case 512: // luk
                        if (playerst.getLuk() >= 999) {
                            used = false;
                        }
                        break;
                    case 2048: // hp
                        if (playerst.getMaxHp() >= 500000) {
                            used = false;
                        }
                        break;
                    case 8192: // mp
                        if (playerst.getMaxMp() >= 500000) {
                            used = false;
                        }
                        break;
                }
                switch (apfrom) { // AP to
                    case 64: // str
                        if (playerst.getStr() <= 4 || (c.getPlayer().getJob() % 1000 / 100 == 1 && playerst.getStr() <= 35)) {
                            used = false;
                        }
                        break;
                    case 128: // dex
                        if (playerst.getDex() <= 4 || (c.getPlayer().getJob() % 1000 / 100 == 3 && playerst.getDex() <= 25) || (c.getPlayer().getJob() % 1000 / 100 == 4 && playerst.getDex() <= 25) || (c.getPlayer().getJob() % 1000 / 100 == 5 && playerst.getDex() <= 20)) {
                            used = false;
                        }
                        break;
                    case 256: // int
                        if (playerst.getInt() <= 4 || (c.getPlayer().getJob() % 1000 / 100 == 2 && playerst.getInt() <= 20)) {
                            used = false;
                        }
                        break;
                    case 512: // luk
                        if (playerst.getLuk() <= 4) {
                            used = false;
                        }
                        break;
                    case 2048: // hp
                        if (/*playerst.getMaxMp() < ((c.getPlayer().getLevel() * 14) + 134) || */c.getPlayer().getHpApUsed() <= 0 || c.getPlayer().getHpApUsed() >= 10000) {
                            used = false;
                            c.getPlayer().dropMessage(1, "You need points in HP or MP in order to take points out.");
                        }
                        break;
                    case 8192: // mp
                        if (/*playerst.getMaxMp() < ((c.getPlayer().getLevel() * 14) + 134) || */c.getPlayer().getHpApUsed() <= 0 || c.getPlayer().getHpApUsed() >= 10000) {
                            used = false;
                            c.getPlayer().dropMessage(1, "You need points in HP or MP in order to take points out.");
                        }
                        break;
                }
                if (used) {
                    switch (apto) { // AP to
                        case 64: { // str
                            final int toSet = playerst.getStr() + 1;
                            playerst.setStr((short) toSet, c.getPlayer());
                            statupdate.put(MapleStat.STR, toSet);
                            break;
                        }
                        case 128: { // dex
                            final int toSet = playerst.getDex() + 1;
                            playerst.setDex((short) toSet, c.getPlayer());
                            statupdate.put(MapleStat.DEX, toSet);
                            break;
                        }
                        case 256: { // int
                            final int toSet = playerst.getInt() + 1;
                            playerst.setInt((short) toSet, c.getPlayer());
                            statupdate.put(MapleStat.INT, toSet);
                            break;
                        }
                        case 512: { // luk
                            final int toSet = playerst.getLuk() + 1;
                            playerst.setLuk((short) toSet, c.getPlayer());
                            statupdate.put(MapleStat.LUK, toSet);
                            break;
                        }
                        case 2048: // hp
                            int maxhp = playerst.getMaxHp();
                            if (GameConstants.isBeginnerJob(job)) { // Beginner
                                maxhp += Randomizer.rand(4, 8);
                            } else if ((job >= 100 && job <= 132) || (job >= 3200 && job <= 3212) || (job >= 1100 && job <= 1112) || (job >= 3100 && job <= 3112)) { // Warrior
                                maxhp += Randomizer.rand(36, 42);
                            } else if ((job >= 200 && job <= 232) || (GameConstants.isEvan(job)) || (job >= 1200 && job <= 1212)) { // Magician
                                maxhp += Randomizer.rand(10, 12);
                            } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 1300 && job <= 1312) || (job >= 1400 && job <= 1412) || (job >= 3300 && job <= 3312) || (job >= 2300 && job <= 2312)) { // Bowman
                                maxhp += Randomizer.rand(14, 18);
                            } else if ((job >= 510 && job <= 512) || (job >= 1510 && job <= 1512)) {
                                maxhp += Randomizer.rand(24, 28);
                            } else if ((job >= 500 && job <= 532) || (job >= 3500 && job <= 3512) || job == 1500) { // Pirate
                                maxhp += Randomizer.rand(16, 20);
                            } else if (job >= 2000 && job <= 2112) { // Aran
                                maxhp += Randomizer.rand(34, 38);
                            } else { // GameMaster
                                maxhp += Randomizer.rand(50, 100);
                            }
                            maxhp = Math.min(99999, Math.abs(maxhp));
                            c.getPlayer().setHpApUsed((short) (c.getPlayer().getHpApUsed() + 1));
                            playerst.setMaxHp(maxhp, c.getPlayer());
                            statupdate.put(MapleStat.MAXHP, (int) maxhp);
                            break;

                        case 8192: // mp
                            int maxmp = playerst.getMaxMp();

                            if (GameConstants.isBeginnerJob(job)) { // Beginner
                                maxmp += Randomizer.rand(6, 8);
                            } else if (job >= 3100 && job <= 3112) {
                                break;
                            } else if ((job >= 100 && job <= 132) || (job >= 1100 && job <= 1112) || (job >= 2000 && job <= 2112)) { // Warrior
                                maxmp += Randomizer.rand(4, 9);
                            } else if ((job >= 200 && job <= 232) || (GameConstants.isEvan(job)) || (job >= 3200 && job <= 3212) || (job >= 1200 && job <= 1212)) { // Magician
                                maxmp += Randomizer.rand(32, 36);
                            } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 500 && job <= 532) || (job >= 3200 && job <= 3212) || (job >= 3500 && job <= 3512) || (job >= 1300 && job <= 1312) || (job >= 1400 && job <= 1412) || (job >= 1500 && job <= 1512) || (job >= 2300 && job <= 2312)) { // Bowman
                                maxmp += Randomizer.rand(8, 10);
                            } else { // GameMaster
                                maxmp += Randomizer.rand(50, 100);
                            }
                            maxmp = Math.min(99999, Math.abs(maxmp));
                            c.getPlayer().setHpApUsed((short) (c.getPlayer().getHpApUsed() + 1));
                            playerst.setMaxMp(maxmp, c.getPlayer());
                            statupdate.put(MapleStat.MAXMP, (int) maxmp);
                            break;
                    }
                    switch (apfrom) { // AP from
                        case 64: { // str
                            final int toSet = playerst.getStr() - 1;
                            playerst.setStr((short) toSet, c.getPlayer());
                            statupdate.put(MapleStat.STR, toSet);
                            break;
                        }
                        case 128: { // dex
                            final int toSet = playerst.getDex() - 1;
                            playerst.setDex((short) toSet, c.getPlayer());
                            statupdate.put(MapleStat.DEX, toSet);
                            break;
                        }
                        case 256: { // int
                            final int toSet = playerst.getInt() - 1;
                            playerst.setInt((short) toSet, c.getPlayer());
                            statupdate.put(MapleStat.INT, toSet);
                            break;
                        }
                        case 512: { // luk
                            final int toSet = playerst.getLuk() - 1;
                            playerst.setLuk((short) toSet, c.getPlayer());
                            statupdate.put(MapleStat.LUK, toSet);
                            break;
                        }
                        case 2048: // HP
                            int maxhp = playerst.getMaxHp();
                            if (GameConstants.isBeginnerJob(job)) { // Beginner
                                maxhp -= 12;
                            } else if ((job >= 200 && job <= 232) || (job >= 1200 && job <= 1212)) { // Magician
                                maxhp -= 10;
                            } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 1300 && job <= 1312) || (job >= 1400 && job <= 1412) || (job >= 3300 && job <= 3312) || (job >= 3500 && job <= 3512) || (job >= 2300 && job <= 2312)) { // Bowman, Thief
                                maxhp -= 15;
                            } else if ((job >= 500 && job <= 532) || (job >= 1500 && job <= 1512)) { // Pirate
                                maxhp -= 22;
                            } else if (((job >= 100 && job <= 132) || job >= 1100 && job <= 1112) || (job >= 3100 && job <= 3112)) { // Soul Master
                                maxhp -= 32;
                            } else if ((job >= 2000 && job <= 2112) || (job >= 3200 && job <= 3212)) { // Aran
                                maxhp -= 40;
                            } else { // GameMaster
                                maxhp -= 20;
                            }
                            c.getPlayer().setHpApUsed((short) (c.getPlayer().getHpApUsed() - 1));
                            playerst.setMaxHp(maxhp, c.getPlayer());
                            statupdate.put(MapleStat.MAXHP, (int) maxhp);
                            break;
                        case 8192: // MP
                            int maxmp = playerst.getMaxMp();
                            if (GameConstants.isBeginnerJob(job)) { // Beginner
                                maxmp -= 8;
                            } else if (job >= 3100 && job <= 3112) {
                                break;
                            } else if ((job >= 100 && job <= 132) || (job >= 1100 && job <= 1112)) { // Warrior
                                maxmp -= 4;
                            } else if ((job >= 200 && job <= 232) || (job >= 1200 && job <= 1212)) { // Magician
                                maxmp -= 30;
                            } else if ((job >= 500 && job <= 532) || (job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 1300 && job <= 1312) || (job >= 1400 && job <= 1412) || (job >= 1500 && job <= 1512) || (job >= 3300 && job <= 3312) || (job >= 3500 && job <= 3512) || (job >= 2300 && job <= 2312)) { // Pirate, Bowman. Thief
                                maxmp -= 10;
                            } else if (job >= 2000 && job <= 2112) { // Aran
                                maxmp -= 5;
                            } else { // GameMaster
                                maxmp -= 20;
                            }
                            c.getPlayer().setHpApUsed((short) (c.getPlayer().getHpApUsed() - 1));
                            playerst.setMaxMp(maxmp, c.getPlayer());
                            statupdate.put(MapleStat.MAXMP, (int) maxmp);
                            break;
                    }
                    c.getSession().write(MaplePacketCreator.updatePlayerStats(statupdate, true, c.getPlayer().getJob()));
                }
                break;
            }
            case 5050001: // SP Reset (1st job)
            case 5050002: // SP Reset (2nd job)
            case 5050003: // SP Reset (3rd job)
            case 5050004:  // SP Reset (4th job)
            case 5050005: //evan sp resets
            case 5050006:
            case 5050007:
            case 5050008:
            case 5050009: {
                if (itemId >= 5050005 && !GameConstants.isEvan(c.getPlayer().getJob())) {
                    c.getPlayer().dropMessage(1, "This reset is only for Evans.");
                    break;
                } //well i dont really care other than this o.o
                if (itemId < 5050005 && GameConstants.isEvan(c.getPlayer().getJob())) {
                    c.getPlayer().dropMessage(1, "This reset is only for non-Evans.");
                    break;
                } //well i dont really care other than this o.o
                int skill1 = slea.readInt();
                int skill2 = slea.readInt();
                for (int i : GameConstants.blockedSkills) {
                    if (skill1 == i) {
                        c.getPlayer().dropMessage(1, "You may not add this skill.");
                        return;
                    }
                }

                Skill skillSPTo = SkillFactory.getSkill(skill1);
                Skill skillSPFrom = SkillFactory.getSkill(skill2);

                if (skillSPTo.isBeginnerSkill() || skillSPFrom.isBeginnerSkill()) {
                    c.getPlayer().dropMessage(1, "You may not add beginner skills.");
                    break;
                }
                if (GameConstants.getSkillBookForSkill(skill1) != GameConstants.getSkillBookForSkill(skill2)) { //resistance evan
                    c.getPlayer().dropMessage(1, "You may not add different job skills.");
                    break;
                }
                //if (GameConstants.getJobNumber(skill1 / 10000) > GameConstants.getJobNumber(skill2 / 10000)) { //putting 3rd job skillpoints into 4th job for example
                //    c.getPlayer().dropMessage(1, "You may not add skillpoints to a higher job.");
                //    break;
                //}
                if ((c.getPlayer().getSkillLevel(skillSPTo) + 1 <= skillSPTo.getMaxLevel()) && c.getPlayer().getSkillLevel(skillSPFrom) > 0 && skillSPTo.canBeLearnedBy(c.getPlayer().getJob())) {
                    if (skillSPTo.isFourthJob() && (c.getPlayer().getSkillLevel(skillSPTo) + 1 > c.getPlayer().getMasterLevel(skillSPTo))) {
                        c.getPlayer().dropMessage(1, "You will exceed the master level.");
                        break;
                    }
                    if (itemId >= 5050005) {
                        if (GameConstants.getSkillBookForSkill(skill1) != (itemId - 5050005) * 2 && GameConstants.getSkillBookForSkill(skill1) != (itemId - 5050005) * 2 + 1) {
                            c.getPlayer().dropMessage(1, "You may not add this job SP using this reset.");
                            break;
                        }
                    } else {
                        int theJob = GameConstants.getJobNumber(skill2 / 10000);
                        switch (skill2 / 10000) {
                            case 430:
                                theJob = 1;
                                break;
                            case 432:
                            case 431:
                                theJob = 2;
                                break;
                            case 433:
                                theJob = 3;
                                break;
                            case 434:
                                theJob = 4;
                                break;
                        }
                        if (theJob != itemId - 5050000) { //you may only subtract from the skill if the ID matches Sp reset
                            c.getPlayer().dropMessage(1, "You may not subtract from this skill. Use the appropriate SP reset.");
                            break;
                        }
                    }
                    c.getPlayer().changeSkillLevel(skillSPFrom, (byte) (c.getPlayer().getSkillLevel(skillSPFrom) - 1), c.getPlayer().getMasterLevel(skillSPFrom));
                    c.getPlayer().changeSkillLevel(skillSPTo, (byte) (c.getPlayer().getSkillLevel(skillSPTo) + 1), c.getPlayer().getMasterLevel(skillSPTo));
                    used = true;
                }
                break;
            }
            case 5500000: { // Magic Hourglass 1 day
                final Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slea.readShort());
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                final int days = 1;
                if (item != null && item.getExpiration() > -1 && !ii.isCash(item.getItemId()) && System.currentTimeMillis() + (100 * 24 * 60 * 60 * 1000L) > item.getExpiration() + (days * 24 * 60 * 60 * 1000L)) {
                    boolean change = true;
                    for (String z : GameConstants.RESERVED) {
                        if (c.getPlayer().getName().indexOf(z) != -1 || item.getOwner().indexOf(z) != -1) {
                            change = false;
                        }
                    }
                    if (change) {
                        item.setExpiration(item.getExpiration() + (days * 24 * 60 * 60 * 1000));
                        c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIPPED);
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(1, "이 아이템에는 사용할 수 없습니다.");
                    }
                }
                break;
            }
            case 5500001: { // Magic Hourglass 7 day
                final Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slea.readShort());
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                final int days = 7;
                if (item != null && item.getExpiration() > -1 && !ii.isCash(item.getItemId()) && System.currentTimeMillis() + (100 * 24 * 60 * 60 * 1000L) > item.getExpiration() + (days * 24 * 60 * 60 * 1000L)) {
                    boolean change = true;
                    for (String z : GameConstants.RESERVED) {
                        if (c.getPlayer().getName().indexOf(z) != -1 || item.getOwner().indexOf(z) != -1) {
                            change = false;
                        }
                    }
                    if (change) {
                        item.setExpiration(item.getExpiration() + (days * 24 * 60 * 60 * 1000));
                        c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIPPED);
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(1, "이 아이템에는 사용할 수 없습니다.");
                    }
                }
                break;
            }
            case 5500002: { // Magic Hourglass 20 day
                final Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slea.readShort());
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                final int days = 20;
                if (item != null && item.getExpiration() > -1 && !ii.isCash(item.getItemId()) && System.currentTimeMillis() + (100 * 24 * 60 * 60 * 1000L) > item.getExpiration() + (days * 24 * 60 * 60 * 1000L)) {
                    boolean change = true;
                    for (String z : GameConstants.RESERVED) {
                        if (c.getPlayer().getName().indexOf(z) != -1 || item.getOwner().indexOf(z) != -1) {
                            change = false;
                        }
                    }
                    if (change) {
                        item.setExpiration(item.getExpiration() + (days * 24 * 60 * 60 * 1000));
                        c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIPPED);
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(1, "이 아이템에는 사용할 수 없습니다.");
                    }
                }
                break;
            }
            case 5500005: { // Magic Hourglass 50 day
                final Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slea.readShort());
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                final int days = 50;
                if (item != null && item.getExpiration() > -1 && !ii.isCash(item.getItemId()) && System.currentTimeMillis() + (100 * 24 * 60 * 60 * 1000L) > item.getExpiration() + (days * 24 * 60 * 60 * 1000L)) {
                    boolean change = true;
                    for (String z : GameConstants.RESERVED) {
                        if (c.getPlayer().getName().indexOf(z) != -1 || item.getOwner().indexOf(z) != -1) {
                            change = false;
                        }
                    }
                    if (change) {
                        item.setExpiration(item.getExpiration() + (20 * 24 * 60 * 60 * 1000));
                        c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIPPED);
                        item.setExpiration(item.getExpiration() + (20 * 24 * 60 * 60 * 1000));
                        c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIPPED);
                        item.setExpiration(item.getExpiration() + (10 * 24 * 60 * 60 * 1000));
                        c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIPPED);
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(1, "이 아이템에는 사용할 수 없습니다.");
                    }
                }
                break;
            }
            case 5500006: { // Magic Hourglass 99 day
                final Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slea.readShort());
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                final int days = 99;
                if (item != null && item.getExpiration() > -1 && !ii.isCash(item.getItemId()) && System.currentTimeMillis() + (100 * 24 * 60 * 60 * 1000L) > item.getExpiration() + (days * 24 * 60 * 60 * 1000L)) {
                    boolean change = true;
                    for (String z : GameConstants.RESERVED) {
                        if (c.getPlayer().getName().indexOf(z) != -1 || item.getOwner().indexOf(z) != -1) {
                            change = false;
                        }
                    }
                    if (change) {
                        item.setExpiration(item.getExpiration() + (20 * 24 * 60 * 60 * 1000));
                        c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIPPED);
                        item.setExpiration(item.getExpiration() + (20 * 24 * 60 * 60 * 1000));
                        c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIPPED);
                        item.setExpiration(item.getExpiration() + (20 * 24 * 60 * 60 * 1000));
                        c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIPPED);
                        item.setExpiration(item.getExpiration() + (20 * 24 * 60 * 60 * 1000));
                        c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIPPED);
                        item.setExpiration(item.getExpiration() + (19 * 24 * 60 * 60 * 1000));
                        c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIPPED);
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(1, "이 아이템에는 사용할 수 없습니다.");
                    }
                }
                break;
            }
            case 5060000: { // Item Tag
                final Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slea.readShort());

                if (item != null && item.getOwner().equals("")) {
                    boolean change = true;
                    for (String z : GameConstants.RESERVED) {
                        if (c.getPlayer().getName().indexOf(z) != -1) {
                            change = false;
                        }
                    }
                    if (change) {
                        item.setOwner(c.getPlayer().getName());
                        c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIPPED);
                        used = true;
                    }
                }
                break;
            }
            case 5062000:       // 레드 큐브 (1)
            case 5062001:       // 마스터 미라클 큐브 (2)
            case 5062002:       // 미라클 큐브 (0)
            case 5062003: {
                final Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) slea.readInt());
                if (item != null && c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                    final Equip eq = (Equip) item;
                    int piece = 0;
                    if (eq.getState() >= 5) {
                        final List<List<StructPotentialItem>> pots = new LinkedList<>(MapleItemInformationProvider.getInstance().getAllPotentialInfo().values());

                        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

                        final int reqLevel = (ii.getReqLevel(eq.getItemId()) < 10 ? 10 : ii.getReqLevel(eq.getItemId()) / 10);
                        final int reqMeso = (reqLevel >= 12 ? 39999 : reqLevel >= 7 ? 19999 : 9999);
                        if (c.getPlayer().getMeso() > reqMeso) {
                            int rank = 0;
                            int prop = 0;
                            //
                            // 
                            if (itemId == 5062002) { // 미라클 큐브 등급 업 확률
                                prop = 0; // 야 이거 2퍼가 아니라 0.2퍼네 일단 기본값 / 1당 0.1%
                                if (eq.getState() == 6) {
                                    prop = 2;  // 에픽 -> 유니크
                                } else if (eq.getState() == 5) {
                                    prop = 9;  // 레어 -> 에픽
                                }
                                if ((eq.getPotential1() > 30000 && eq.getPotential1() < 30041)
                                        || eq.getPotential1() == 30400 || eq.getPotential1() == 30401 || eq.getPotential1() == 30402 || eq.getPotential1() == 30403) {
                                    c.getPlayer().dropMessage(5, "이 아이템의 잠재 능력은 재설정할 수 없습니다.");
                                    break;
                                }
                                piece = 2430111; // 미라클 큐브 조각
                            }
                            if (itemId == 5062001) { // 마스터 미라클 큐브 등급 업 확률
                                prop = 15; // 2% 기본값
                                piece = 2430112; // 미라클 큐브 조각
                                if (eq.getState() == 7) { // 유니크 -> 레전드리
                                    prop = 8;
                                } else if (eq.getState() == 6) {
                                    prop = 20;  // 에픽 -> 유니크
                                } else if (eq.getState() == 5) {
                                    prop = 50;  // 레어 -> 에픽
                                }
                            }
                            if (itemId == 5062000) { // 레드 큐브 등급 업 확률
                                prop = 25; // 3% 기본값
                                if (eq.getState() == 7) { // 유니크 -> 레전드리
                                    prop = 3;
                                } else if (eq.getState() == 6) {
                                    prop = 15;  // 에픽 -> 유니크
                                } else if (eq.getState() == 5) {
                                    prop = 30;  // 레어 -> 에픽
                                }
                                piece = 2430112; // 미라클 큐브 조각
                            }
                            if (itemId == 5062003) { // 플래티넘 큐브 등급 업 확률
                                prop = 40; // 4% 기본값
                                if (eq.getState() == 7) { // 유니크 -> 레전드리
                                    prop = 15;
                                } else if (eq.getState() == 6) {
                                    prop = 30;  // 에픽 -> 유니크
                                } else if (eq.getState() == 5) {
                                    prop = 80;  // 레어 -> 에픽
                                }
                                piece = 2430112; // 미라클 큐브 조각
                            }
                            if (c.getPlayer().isGM() == true) { // GM 등급 업 확률
                                prop = 1000; // 100%
                            }

                            if (ServerConstants.cubeDayValue > 1.0) {
                                prop *= ServerConstants.cubeDayValue;
                            }

                            if (eq.getState() == 7) {
                                if ((eq.getPotential1() > 30000 && eq.getPotential1() < 30041)
                                        || eq.getPotential1() == 30400 || eq.getPotential1() == 30401 || eq.getPotential1() == 30402 || eq.getPotential1() == 30403) {
                                    rank = -8; // 레전드리
                                } else {
                                    rank = Randomizer.nextInt(1000) < prop ? -8 : -7; // 유니크 > 레전드리
                                }
                            } else if (eq.getState() == 6) {
                                rank = Randomizer.nextInt(1000) < prop ? -7 : -6; // 에픽 > 유니크
                            } else if (eq.getState() == 5) {
                                rank = Randomizer.nextInt(1000) < prop ? -6 : -5; // 레어 > 에픽
                            }
                            //c.getPlayer().dropMessage(5, "rank  : " + rank );

                            int new_state = Math.abs(rank);

                            if (new_state > 12 || new_state < 5) { //보정
                                new_state = 5;
                            }

                            StructPotentialItem pot2 = pots.get(90).get(reqLevel);
//                            c.getPlayer().dropMessage(5, "통과 : " + pot2.potentialID);
                            final int lines = (eq.getPotential3() != 0 ? 3 : 2);
                            for (int i = 0; i < lines; i++) {
                                boolean rewarded = false;
                                while (!rewarded) { // Randomizer.nextInt(pots.size()) >> 이게 레어가 나오면 ㄱㄱ
                                    int a = Randomizer.nextInt(pots.size());
                                    StructPotentialItem pot = pots.get(a).get(reqLevel);

                                    //System.out.print(a + " : 큐브 옵션\r\n");
                                    if (pot != null && pot.reqLevel / 10 <= reqLevel && GameConstants.optionTypeFits(pot.optionType, eq.getItemId(), pot.potentialID) && GameConstants.potentialIDFits(pot.potentialID, new_state, i)) {
                                        if (pot.boss && pot.incDAMr > 0) { //보공일 때
                                            double per = 30d; //확률
                                            double secondRandom = Math.random() * 100;
                                            if (secondRandom > per) {
                                                continue;
                                            } //미당첨 새로 옵션 뽑음
                                        } else if (pot.ignoreTargetDEF > 0) { //방무일 때
                                            double per = 50d; //확률
                                            double secondRandom = Math.random() * 100;
                                            if (secondRandom > per) {
                                                continue;
                                            } //미당첨
                                        } else if (pot.incRewardProp > 0) { //아이템 드롭률 증가일 때
                                            double per = 10d; //확률
                                            double secondRandom = Math.random() * 100;
                                            if (secondRandom > per) {
                                                continue;
                                            } //미당첨 새로 옵션 뽑음
                                        } else if (pot.incMesoProp > 0) { //메소 획득량 증가일 때
                                            double per = 10d; //확률
                                            double secondRandom = Math.random() * 100;
                                            if (secondRandom > per) {
                                                continue;
                                            } //미당첨
                                        } else {
                                            switch (pot.potentialID) {
                                                case 30040: //쓸어블
                                                case 30400: //쓸컴뱃
                                                case 30401: //쓸윈부
                                                case 31003: //쓸샾
                                                {
                                                    double per = 15d; //확률
                                                    double secondRandom = Math.random() * 100;
                                                    if (secondRandom > per) {
                                                        continue;
                                                    } //미당첨
                                                    break;
                                                }
                                                case 20051: //공격력 %
                                                case 30051:
                                                case 30023:
                                                case 20052: //마력 %
                                                case 30052:
                                                case 30024: {
                                                    double per = 35d; //확률
                                                    double secondRandom = Math.random() * 100;
                                                    if (secondRandom > per) {
                                                        continue;
                                                    } //미당첨
                                                    break;
                                                }
                                                //여기서부터
//                                                case 30041:
//                                                case 30042:
//                                                case 30043:
//                                                case 30044: {
//                                                //여기까지 힘,덱,인,럭 유니크 옵션
//                                                    double per = 20d; //확률
//                                                    double secondRandom = Math.random() * 100;
//                                                    if (secondRandom > per) {
//                                                        continue;
//                                                    } //미당첨
//                                                    break;
//                                                }
//                                                //여기서부터
//                                                case 30015:
//                                                case 30016:
//                                                case 30017:
//                                                case 30018: {
//                                                //여기까지 힘,덱,인,럭 레전드리 옵션
//                                                    double per = 20d; //확률
//                                                    double secondRandom = Math.random() * 100;
//                                                    if (secondRandom > per) {
//                                                        continue;
//                                                    } //미당첨
//                                                    break;
//                                                }
                                            }
                                        }

                                        if (i == 0) {
                                            eq.setPotential1(pot.potentialID);
                                        } else if (i == 1) {
                                            eq.setPotential2(pot.potentialID);
                                        } else if (i == 2) {
                                            eq.setPotential3(pot.potentialID);
                                        }
                                        rewarded = true;
                                        if (i == lines) {
                                            break;
                                        }
                                     //   System.out.print(a + " : 큐브 옵션\r\n");
                                        // c.getPlayer().dropMessage(5, "a : " + a);
                                        //c.getPlayer().dropMessage(5, "pot.potentialID : " + pot.potentialID);
                                    }
                                }
                            }
                            c.getPlayer().gainMeso(-(reqMeso), true, true);
                        } else {
                            c.getPlayer().dropMessage(1, "'" + ii.getName(eq.getItemId()) + "'(을)를 감정하기 위해서는 " + reqMeso + "메소가 필요합니다.");
                            break;
                        }
                        c.getSession().write(MaplePacketCreator.scrolledItem(toUse, item, false, true));
                        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.getPotentialEffect(c.getPlayer().getId(), 1), true);
                        c.getPlayer().forceReAddItem_NoUpdate(item, MapleInventoryType.EQUIP);
                        if (piece > 0) {
                            MapleInventoryManipulator.addById(c, piece, (short) 1, "Cube" + " on " + FileoutputUtil.CurrentReadable_Date());
                        }
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(5, "이 아이템의 잠재 능력은 재설정할 수 없습니다.");
                    }
                } else {
                    c.getPlayer().dropMessage(5, "소비 아이템 여유 공간이 부족하여 잠재 능력 재설정을 실패하였습니다.");
                }
                break;
            }
            /*case 5062000:       // 레드 큐브 (1)
             case 5062001:       // 마스터 미라클 큐브 (2)
             case 5062002:       // 미라클 큐브 (0)
             case 5062003: {     // 플래티넘 미라클 큐브 (3)
             final Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) slea.readInt());
             if (item != null && c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
             final Equip eq = (Equip) item;
             int piece = 0;
             if (eq.getState() >= 5) {
             final List<List<StructPotentialItem>> pots = new LinkedList<>(MapleItemInformationProvider.getInstance().getAllPotentialInfo().values());
                 
             final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                        
             final int reqLevel = (ii.getReqLevel(eq.getItemId()) < 10 ? 10 : ii.getReqLevel(eq.getItemId()) / 10);
             final int reqMeso = (reqLevel >= 12 ? 39999 : reqLevel >= 7 ? 19999 : 9999);
             if (c.getPlayer().getMeso() > reqMeso) {
             int rank = 0;
             int prop = 0;
             int pprop = 10; // 이탈 확률 기본 90%
             //
             // 
             if (itemId == 5062002) { // 미라클 큐브 등급 업 확률
             prop = 0; // 야 이거 2퍼가 아니라 0.2퍼네 일단 기본값 / 1당 0.1%
             if (eq.getState() == 6) {
             prop = 2;  // 에픽 -> 유니크
             } else if (eq.getState() == 5) {
             prop = 9;  // 레어 -> 에픽
             }
             if (eq.getPotential1() > 30000 && eq.getPotential1() < 30042) {
             c.getPlayer().dropMessage(5, "이 아이템의 잠재 능력은 재설정할 수 없습니다.");
             break;
             }
             pprop = 90; // 이탈옵 나올 확률
             //piece = 2430112; // 미라클 큐브 조각
             }
             if (itemId == 5062001) { // 마스터 미라클 큐브 등급 업 확률
             prop = 15; // 2% 기본값
             piece = 2430112; // 미라클 큐브 조각
             if (eq.getState() == 7) { // 유니크 -> 레전드리
             prop = 15; 
             } else if (eq.getState() == 6) {
             prop = 20;  // 에픽 -> 유니크
             } else if (eq.getState() == 5) {
             prop = 50;  // 레어 -> 에픽
             }
             pprop = 10; // 이탈옵 나올 확률
             }
             if (itemId == 5062000) { // 레드 큐브 등급 업 확률
             prop = 25; // 3% 기본값
             if (eq.getState() == 7) { // 유니크 -> 레전드리
             prop = 10; 
             } else if (eq.getState() == 6) {
             prop = 15;  // 에픽 -> 유니크
             } else if (eq.getState() == 5) {
             prop = 30;  // 레어 -> 에픽
             }
             piece = 2430112; // 미라클 큐브 조각
             pprop = 10; // 이탈옵 나올 확률
             }
             if (itemId == 5062003) { // 플래티넘 큐브 등급 업 확률
             prop = 40; // 4% 기본값
             if (eq.getState() == 7) { // 유니크 -> 레전드리
             prop = 20; 
             } else if (eq.getState() == 6) {
             prop = 30;  // 에픽 -> 유니크
             } else if (eq.getState() == 5) {
             prop = 80;  // 레어 -> 에픽
             }
             piece = 2430112; // 미라클 큐브 조각
             pprop = 10; // 이탈옵 나올 확률
             }
             if (c.getPlayer().isGM() == true) { // GM 등급 업 확률
             prop = 1000; // 100%
             }
                            

                            
             if (eq.getState() == 7) {
             if (eq.getPotential1() > 30000 && eq.getPotential1() < 30042) {
             rank = -8; // 레전드리
             } else {
             rank = Randomizer.nextInt(1000) < prop ? -8 : -7; // 유니크 > 레전드리
             }
             } else if (eq.getState() == 6) {
             rank = Randomizer.nextInt(1000) < prop ? -7 : -6; // 에픽 > 유니크
             } else if (eq.getState() == 5) {
             rank = Randomizer.nextInt(1000) < prop ? -6 : -5; // 레어 > 에픽
             }
             //c.getPlayer().dropMessage(5, "rank  : " + rank );
                            
             int new_state = Math.abs(rank);

             if (new_state > 12 || new_state < 5) { //보정
             new_state = 5;
             }
                            
             //StructPotentialItem pot2 = pots.get(90).get(reqLevel);
             //c.getPlayer().dropMessage(5, "통과 : " + pot2.potentialID);
             final int lines = (eq.getPotential3() != 0 ? 3 : 2);
             for (int i = 0; i < lines; i++) {
             boolean rewarded = false;
             while (!rewarded) { // Randomizer.nextInt(pots.size()) >> 이게 레어가 나오면 ㄱㄱ
             int a = Randomizer.nextInt(pots.size());
             StructPotentialItem pot = pots.get(a).get(reqLevel);
             //System.out.print(a + " : 큐브 옵션\r\n");
             if (pot != null && pot.reqLevel / 10 <= reqLevel && GameConstants.optionTypeFits(pot.optionType, eq.getItemId()) && GameConstants.potentialIDFits(pot.potentialID, new_state, i, pprop)) {
                                   
             if (i == 0) {
             eq.setPotential1(pot.potentialID);
             } else if (i == 1) {
             eq.setPotential2(pot.potentialID);
             } else if (i == 2) {
             eq.setPotential3(pot.potentialID);
             }
             rewarded = true;
                                        
             if (i == lines) {
             break;
             }
             //   System.out.print(a + " : 큐브 옵션\r\n");
             // c.getPlayer().dropMessage(5, "a : " + a);
             //c.getPlayer().dropMessage(5, "pot.potentialID : " + pot.potentialID);
             }
             }
             }
             c.getPlayer().gainMeso(-(reqMeso), true, true);
             } else {
             c.getPlayer().dropMessage(1, "'" + ii.getName(eq.getItemId()) + "'(을)를 감정하기 위해서는 " + reqMeso + "메소가 필요합니다.");
             break;
             }
             c.getSession().write(MaplePacketCreator.scrolledItem(toUse, item, false, true));
             c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.getPotentialEffect(c.getPlayer().getId(), 1), true);
             c.getPlayer().forceReAddItem_NoUpdate(item, MapleInventoryType.EQUIP);
             if (piece > 0) {
             MapleInventoryManipulator.addById(c, piece, (short) 1, "Cube" + " on " + FileoutputUtil.CurrentReadable_Date());
             }
             used = true;
             } else {
             c.getPlayer().dropMessage(5, "이 아이템의 잠재 능력은 재설정할 수 없습니다.");
             }
             } else {
             c.getPlayer().dropMessage(5, "소비 아이템 여유 공간이 부족하여 잠재 능력 재설정을 실패하였습니다.");
             }
             break;
             }*/
            case 5062004:       //줄큡 1 (4)
            case 5062005:       // 줄큡 2 (5)
            case 5062006: {   // 줄큡 3 (6)
                final Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) slea.readInt());
                if (item != null && c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                    final Equip eq = (Equip) item;
                    if (eq.getState() >= 5) {
                        final List<List<StructPotentialItem>> pots = new LinkedList<>(MapleItemInformationProvider.getInstance().getAllPotentialInfo().values());
                        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                        final int reqLevel = (ii.getReqLevel(eq.getItemId()) < 10 ? 10 : ii.getReqLevel(eq.getItemId()) / 10);
                        final int reqMeso = (reqLevel >= 12 ? 39999 : reqLevel >= 7 ? 19999 : 9999);
                        if (c.getPlayer().getMeso() > reqMeso) {
                            int rank = 0;
                            if (eq.getState() == 5 || eq.getState() == 6 || eq.getPotential3() == 0) { //레전더리 , 잠재3줄이상에만 사용 가능하게 검사
                                c.getPlayer().dropMessage(5, "이 아이템의 잠재 능력은 재설정할 수 없습니다. 레전더리 잠재3줄이상");
                                break;
                            }

                            if (eq.getState() == 7) {
                                if ((eq.getPotential1() > 30000 && eq.getPotential1() < 30041)
                                        || eq.getPotential1() == 30400 || eq.getPotential1() == 30401 || eq.getPotential1() == 30402 || eq.getPotential1() == 30403) {
                                    rank = -8; // 레전드리
                                }
                            }

                            int new_state = Math.abs(rank);

                            if (new_state > 12 || new_state < 5) { //보정
                                new_state = 5;
                            }

                            int targetLine = itemId == 5062004 ? 0 : (itemId == 5062005 ? 1 : 2); // 줄큡1은 첫 번째, 줄큡2는 두 번째, 줄큡3은 세 번째
                            StructPotentialItem pot2 = pots.get(90).get(reqLevel);
                            boolean rewarded = false;
                            while (!rewarded) {
                                int a = Randomizer.nextInt(pots.size());
                                StructPotentialItem pot = pots.get(a).get(reqLevel);

                                if (pot != null && pot.reqLevel / 10 <= reqLevel && GameConstants.optionTypeFits(pot.optionType, eq.getItemId(), pot.potentialID) && GameConstants.potentialIDFits(pot.potentialID, new_state, targetLine)) {
                                    if (pot.boss && pot.incDAMr > 0) { //보공일 때
                                        double per = 30d; //확률
                                        double secondRandom = Math.random() * 100;
                                        if (secondRandom > per) {
                                            continue;
                                        } //미당첨 새로 옵션 뽑음
                                    } else if (pot.ignoreTargetDEF > 0) { //방무일 때
                                        double per = 50d; //확률
                                        double secondRandom = Math.random() * 100;
                                        if (secondRandom > per) {
                                            continue;
                                        } //미당첨
                                    } else if (pot.incRewardProp > 0) { //아이템 드롭률 증가일 때
                                        double per = 10d; //확률
                                        double secondRandom = Math.random() * 100;
                                        if (secondRandom > per) {
                                            continue;
                                        } //미당첨 새로 옵션 뽑음
                                    } else if (pot.incMesoProp > 0) { //메소 획득량 증가일 때
                                        double per = 10d; //확률
                                        double secondRandom = Math.random() * 100;
                                        if (secondRandom > per) {
                                            continue;
                                        } //미당첨
                                    } else {
                                        switch (pot.potentialID) {
                                            case 30040: //쓸어블
                                            case 30400: //쓸컴뱃
                                            case 30401: //쓸윈부
                                            case 31003: //쓸샾
                                            {
                                                double per = 15d; //확률
                                                double secondRandom = Math.random() * 100;
                                                if (secondRandom > per) {
                                                    continue;
                                                } //미당첨
                                                break;
                                            }
                                            case 20051: //공격력 %
                                            case 30051:
                                            case 30023:
                                            case 20052: //마력 %
                                            case 30052:
                                            case 30024: {
                                                double per = 35d; //확률
                                                double secondRandom = Math.random() * 100;
                                                if (secondRandom > per) {
                                                    continue;
                                                } //미당첨
                                                break;
                                            }
                                            //여기서부터
//                                                case 30041:
//                                                case 30042:
//                                                case 30043:
//                                                case 30044: {
//                                                //여기까지 힘,덱,인,럭 유니크 옵션
//                                                    double per = 20d; //확률
//                                                    double secondRandom = Math.random() * 100;
//                                                    if (secondRandom > per) {
//                                                        continue;
//                                                    } //미당첨
//                                                    break;
//                                                }
//                                                //여기서부터
//                                                case 30015:
//                                                case 30016:
//                                                case 30017:
//                                                case 30018: {
//                                                //여기까지 힘,덱,인,럭 레전드리 옵션
//                                                    double per = 20d; //확률
//                                                    double secondRandom = Math.random() * 100;
//                                                    if (secondRandom > per) {
//                                                        continue;
//                                                    } //미당첨
//                                                    break;
//                                                }
                                            }
                                    }
                                    if (targetLine == 0) {
                                        eq.setPotential1(pot.potentialID);
                                        break;
                                    }
                                    if (targetLine == 1) {
                                        eq.setPotential2(pot.potentialID);
                                        break;
                                    }
                                    if (targetLine == 2) {
                                        eq.setPotential3(pot.potentialID);
                                        break;
                                    }
                                    rewarded = true;

                                     //   System.out.print(a + " : 큐브 옵션\r\n");
                                    // c.getPlayer().dropMessage(5, "a : " + a);
                                    //c.getPlayer().dropMessage(5, "pot.potentialID : " + pot.potentialID);
                                }
                            }
                            c.getPlayer().gainMeso(-reqMeso, true, true);
                        } else {
                            c.getPlayer().dropMessage(1, "'" + ii.getName(eq.getItemId()) + "'(을)를 감정하기 위해서는 " + reqMeso + "메소가 필요합니다.");
                            break;
                        }
                        c.getSession().write(MaplePacketCreator.scrolledItem(toUse, item, false, true));
                        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.getPotentialEffect(c.getPlayer().getId(), item.getItemId()), true);
                        c.getPlayer().forceReAddItem_NoUpdate(item, MapleInventoryType.EQUIP);
                        c.getPlayer().dropMessage(5, "특정 잠재 능력이 재설정되었습니다.");
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(5, "이 아이템의 잠재 능력은 재설정할 수 없습니다.");
                    }
                } else {
                    c.getPlayer().dropMessage(5, "소비 아이템 여유 공간이 부족하여 잠재 능력 재설정을 실패하였습니다.");
                }
                break;
            }
            case 5521000: { // Karma
                final MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                final Item item = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());

                if (item != null && !ItemFlag.KARMA_ACC.check(item.getFlag()) && !ItemFlag.KARMA_ACC_USE.check(item.getFlag())) {
                    if (MapleItemInformationProvider.getInstance().isShareTagEnabled(item.getItemId())) {
                        short flag = item.getFlag();
                        if (ItemFlag.UNTRADEABLE.check(flag)) {
                            flag -= ItemFlag.UNTRADEABLE.getValue();
                        } else if (type == MapleInventoryType.EQUIP) {
                            flag |= ItemFlag.KARMA_ACC.getValue();
                        } else {
                            flag |= ItemFlag.KARMA_ACC_USE.getValue();
                        }
                        item.setFlag(flag);
                        c.getPlayer().forceReAddItem_NoUpdate(item, type);
                        c.getSession().write(MaplePacketCreator.updateSpecialItemUse(item, type.getType(), item.getPosition(), true, c.getPlayer()));
                        used = true;
                    }
                }
                break;
            }
            case 5520001: //p.karma
            case 5520000: { // Karma
                final MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                final Item item = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());

                if (item != null && !ItemFlag.KARMA_EQ.check(item.getFlag()) && !ItemFlag.KARMA_USE.check(item.getFlag())) {
                    if ((itemId == 5520000 && MapleItemInformationProvider.getInstance().isKarmaEnabled(item.getItemId())) || (itemId == 5520001 && MapleItemInformationProvider.getInstance().isPKarmaEnabled(item.getItemId()))) {
                        short flag = item.getFlag();
                        if (ItemFlag.UNTRADEABLE.check(flag)) {
                            flag -= ItemFlag.UNTRADEABLE.getValue();
                        } else if (type == MapleInventoryType.EQUIP) {
                            flag |= ItemFlag.KARMA_EQ.getValue();
                        } else {
                            flag |= ItemFlag.KARMA_USE.getValue();
                        }
                        item.setFlag(flag);
                        c.getPlayer().forceReAddItem_NoUpdate(item, type);
                        c.getSession().write(MaplePacketCreator.updateSpecialItemUse(item, type.getType(), item.getPosition(), true, c.getPlayer()));
                        used = true;
                    }
                }
                break;
            }
            case 5610001:
            case 5610000: { // Vega 30
                slea.readInt(); // Inventory type, always eq
                final short dst = (short) slea.readInt();
                slea.readInt(); // Inventory type, always use
                final short src = (short) slea.readInt();
                used = UseUpgradeScroll(src, dst, (short) 2, c, c.getPlayer(), itemId); //cannot use ws with vega but we dont care
                cc = used;
                break;
            }
            case 5060001: { // Sealing Lock
                final MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                final Item item = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());
                // another int here, lock = 5A E5 F2 0A, 7 day = D2 30 F3 0A
                if (item != null && item.getExpiration() == -1) {
                    short flag = item.getFlag();
                    flag |= ItemFlag.LOCK.getValue();
                    item.setFlag(flag);

                    c.getPlayer().forceReAddItem_Flag(item, type);
                    used = true;
                }
                break;
            }
            case 5061000: { // Sealing Lock 7 days
                final MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                final Item item = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());
                // another int here, lock = 5A E5 F2 0A, 7 day = D2 30 F3 0A
                if (item != null && item.getExpiration() == -1) {
                    short flag = item.getFlag();
                    flag |= ItemFlag.LOCK.getValue();
                    item.setFlag(flag);
                    item.setExpiration(System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000));

                    c.getPlayer().forceReAddItem_Flag(item, type);
                    used = true;
                }
                break;
            }
            case 5061001: { // Sealing Lock 30 days
                final MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                final Item item = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());
                // another int here, lock = 5A E5 F2 0A, 7 day = D2 30 F3 0A
                if (item != null && item.getExpiration() == -1) {
                    short flag = item.getFlag();
                    flag |= ItemFlag.LOCK.getValue();
                    item.setFlag(flag);

                    item.setExpiration(System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000));

                    c.getPlayer().forceReAddItem_Flag(item, type);
                    used = true;
                }
                break;
            }
            case 5061002: { // Sealing Lock 90 days
                final MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                final Item item = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());
                // another int here, lock = 5A E5 F2 0A, 7 day = D2 30 F3 0A
                if (item != null && item.getExpiration() == -1) {
                    short flag = item.getFlag();
                    flag |= ItemFlag.LOCK.getValue();
                    item.setFlag(flag);

                    item.setExpiration(System.currentTimeMillis() + (90 * 24 * 60 * 60 * 1000));

                    c.getPlayer().forceReAddItem_Flag(item, type);
                    used = true;
                }
                break;
            }
            case 5061003: { // Sealing Lock 365 days
                final MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                final Item item = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());
                // another int here, lock = 5A E5 F2 0A, 7 day = D2 30 F3 0A
                if (item != null && item.getExpiration() == -1) {
                    short flag = item.getFlag();
                    flag |= ItemFlag.LOCK.getValue();
                    item.setFlag(flag);

                    item.setExpiration(System.currentTimeMillis() + (365 * 24 * 60 * 60 * 1000));

                    c.getPlayer().forceReAddItem_Flag(item, type);
                    used = true;
                }
                break;
            }
            case 5063000: {
                final MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                final Item item = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());
                // another int here, lock = 5A E5 F2 0A, 7 day = D2 30 F3 0A
                if (item != null && item.getType() == 1) { //equip
                    short flag = item.getFlag();
                    flag |= ItemFlag.LUCKS_KEY.getValue();
                    item.setFlag(flag);

                    c.getPlayer().forceReAddItem_Flag(item, type);
                    used = true;
                }
                break;
            }
            case 5064000: {
                final MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                final Item item = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());
                // another int here, lock = 5A E5 F2 0A, 7 day = D2 30 F3 0A
                if (item != null && item.getType() == 1) { //equip
                    if (((Equip) item).getEnhance() >= 8) {
                        break; //cannot be used
                    }
                    short flag = item.getFlag();
                    flag |= ItemFlag.SHIELD_WARD.getValue();
                    item.setFlag(flag);

                    c.getPlayer().forceReAddItem_Flag(item, type);
                    used = true;
                }
                break;
            }
            case 5060004:
            case 5060003: {//피넛머신
                if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() < 1 || c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() < 2 || c.getPlayer().getInventory(MapleInventoryType.ETC).getNumFreeSlot() < 3) {
                    c.getPlayer().dropMessage(5, "장비창 1개, 소비창 2개, 기타창 3개의 공간이 필요합니다.");
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
                slea.skip(4);
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                Item item = c.getPlayer().getInventory(MapleInventoryType.ETC).findById(itemId == 5060003 ? 4170023 : 4170024);
                if (getIncubatedItems(c, item.getItemId(), 1)) {
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.ETC, item.getPosition(), (short) 1, false);
                    used = true;
                }
//                int id1 = RandomRewards.getPeanutReward();
//                while (!ii.itemExists(id1)) {
//                    id1 = RandomRewards.getPeanutReward();
//                }
//                MapleInventoryManipulator.addById(c, id1, (isPotion(id1) ? (short) 100 : (short) 1), "피넛 머신");
//                c.getSession().write(MaplePacketCreator.getNPCTalk(9010000, (byte) 0, "단단한 땅콩에서 아이템이 나왔습니다.\r\n\r\n#fUI/UIWindow.img/QuestIcon/4/0#\r\n#i" + id1 + "# " + ii.getItemInformation(id1).name + (isPotion(id1) ? "100" : "1개 \r\n\r\n#b#i4310016# #z4310016#를 획득했습니다."), "00 00", (byte) 0));
//                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.ETC, item.getPosition(), (short) 1, false);
//                MapleInventoryManipulator.addById(c, 4310016, (short) 1, "Cube" + " on " + FileoutputUtil.CurrentReadable_Date());
//                //MapleInventoryManipulator.addById(c, 4310007, (short) 1, "Cube" + " on " + FileoutputUtil.CurrentReadable_Date());    //아이스피크            
//                World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, c.getPlayer().getName() + "님이 피넛 머신에서 " + "[{" + ii.getItemInformation(id1).name + "}]" + "(을)를 얻었습니다.", id1));
//                used = true;
                break;
            }
            case 5060002: {//부화기
                if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() < 1 || c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() < 2 || c.getPlayer().getInventory(MapleInventoryType.ETC).getNumFreeSlot() < 3) {
                    c.getPlayer().dropMessage(5, "장비창 1개, 소비창 2개, 기타창 3개의 공간이 필요합니다.");
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
                slea.skip(4);
                int egg = slea.readInt();
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                Item item = c.getPlayer().getInventory(MapleInventoryType.ETC).getItem((short) egg);
                if (getIncubatedItems(c, item.getItemId(), 0)) {
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.ETC, item.getPosition(), (short) 1, false);
                    used = true;
                }
//                int id1 = RandomRewards.getPigmiResult();
//                while (!ii.itemExists(id1)) {
//                    id1 = RandomRewards.getPigmiResult();
//                }
//                MapleInventoryManipulator.addById(c, id1, (isPotion(id1) ? (short) 100 : (short) 1), "피그미에그");
//                c.getSession().write(MaplePacketCreator.getNPCTalk(9050008, (byte) 0, "피그미 에그 안에서 아이템이 나왔습니다.\r\n\r\n#fUI/UIWindow.img/QuestIcon/4/0#\r\n#i" + id1 + "# " + ii.getItemInformation(id1).name + (isPotion(id1) ? "100" : " 1개를 획득했습니다."), "00 00", (byte) 0));
//                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.ETC, item.getPosition(), (short) 1, false);
                //MapleInventoryManipulator.addById(c, 4310001, (short) 1, "Cube" + " on " + FileoutputUtil.CurrentReadable_Date());        
                //MapleInventoryManipulator.addById(c, 4310014, (short) 1, "Cube" + " on " + FileoutputUtil.CurrentReadable_Date());                      
                //World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, c.getPlayer().getName() + "님이 피그미 에그에서 " + "[{" + ii.getItemInformation(id1).name + "}]" + "(을)를 얻었습니다.", id1));
                // World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, c.getPlayer().getName() + "님이 피그미 에그에서 " + "" + ii.getItemInformation(id1).name + "" + "(을)를 얻었습니다.", id1));
                used = true;
                break;
            }
            case 5071000: { // Megaphone
                if (c.getPlayer().getLevel() < 1) {
                    c.getPlayer().dropMessage(5, "레벨 1이상만 사용할 수 있습니다.");
                    break;
                }
                if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                    c.getPlayer().dropMessage(5, "이 곳에선 사용할 수 없습니다.");
                    break;
                }
                if (!c.getChannelServer().getMegaphoneMuteState()) {
                    final String message = slea.readMapleAsciiString();

                    if (message.length() > 65) {
                        break;
                    }
                    final StringBuilder sb = new StringBuilder();
                    addMedalString(c.getPlayer(), sb);
                    sb.append(c.getPlayer().getName());
                    sb.append(" : ");
                    sb.append(message);

                    c.getChannelServer().broadcastSmegaPacket(MaplePacketCreator.serverNotice(9, c.getChannel(), sb.toString())); //9=초록확성기
                    ServerLogger.getInstance().logChat(LogType.Chat.Megaphone, c.getPlayer().getId(), c.getPlayer().getName(), message, "채널 : " + c.getRealChannelName());
                    used = true;
                } else {
                    c.getPlayer().dropMessage(5, "현재 확성기 사용 금지 상태입니다.");
                }
                break;
            }
            case 5077000: { // 3 line Megaphone
                if (c.getPlayer().getLevel() < 1) {
                    c.getPlayer().dropMessage(5, "레벨 10 이상만 사용할 수 있습니다.");
                    break;
                }
                if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                    c.getPlayer().dropMessage(5, "Cannot be used here.");
                    break;
                }
                if (!c.getPlayer().getCheatTracker().canSmega()) {
                    c.getPlayer().dropMessage(5, "15초 마다 한번씩만 사용 가능합니다.");
                    break;
                }
                if (!c.getChannelServer().getMegaphoneMuteState()) {
                    final byte numLines = slea.readByte();
                    if (numLines > 3) {
                        return;
                    }
                    final List<String> messages = new LinkedList<String>();
                    String message;
                    for (int i = 0; i < numLines; i++) {
                        message = slea.readMapleAsciiString();
                        if (message.length() > 65) {
                            break;
                        }

                        final StringBuilder sb = new StringBuilder();
                        addMedalString(c.getPlayer(), sb);
                        sb.append(c.getPlayer().getName());
                        sb.append(" : ");
                        sb.append(message);
                        messages.add(sb.toString());
                    }
                    final boolean ear = slea.readByte() > 0;

                    World.Broadcast.broadcastSmega(MaplePacketCreator.tripleSmega(messages, ear, c.getChannel()));
                    used = true;
                } else {
                    c.getPlayer().dropMessage(5, "현재 확성기 사용 금지 상태입니다.");
                }
                break;
            }
            case 5072000: { // Super Megaphone
                if (c.getPlayer().getLevel() < 1) {
                    c.getPlayer().dropMessage(5, "레벨 10 이상만 사용할 수 있습니다.");
                    break;
                }
                if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                    c.getPlayer().dropMessage(5, "이곳에서 사용할 수 없습니다.");
                    break;
                }
                if (!c.getPlayer().getCheatTracker().canSmega()) {
                    c.getPlayer().dropMessage(5, "15초마다 한번씩만 사용할 수 있습니다.");
                    break;
                }
                if (!c.getChannelServer().getMegaphoneMuteState()) {
                    final String message = slea.readMapleAsciiString();

                    if (message.length() > 65) {
                        break;
                    }
                    final StringBuilder sb = new StringBuilder();
                    addMedalString(c.getPlayer(), sb);
                    sb.append(c.getPlayer().getName());
                    sb.append(" : ");
                    sb.append(message);

                    final boolean ear = slea.readByte() != 0;

                    World.Broadcast.broadcastSmega(MaplePacketCreator.serverNotice(3, c.getChannel(), sb.toString(), ear));
                    ServerLogger.getInstance().logChat(LogType.Chat.SuperMegaphone, c.getPlayer().getId(), c.getPlayer().getName(), message, "채널 : " + c.getRealChannelName() + " / 귀 : " + (ear ? "예" : "아니오"));
                    used = true;
                } else {
                    c.getPlayer().dropMessage(5, "현재 확성기 사용 금지 상태입니다.");
                }
                break;
            }
            case 5076000: { // Item Megaphone
                if (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().dropMessage(5, "레벨 10 이상만 사용할 수 있습니다.");
                    break;
                }
                if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                    c.getPlayer().dropMessage(5, "이곳에서 사용할 수 없습니다.");
                    break;
                }
                if (!c.getPlayer().getCheatTracker().canSmega()) {
                    c.getPlayer().dropMessage(5, "15초마다 한번씩만 사용할 수 있습니다.");
                    break;
                }
                if (!c.getChannelServer().getMegaphoneMuteState()) {
                    final String message = slea.readMapleAsciiString();

                    if (message.length() > 65) {
                        break;
                    }
                    final StringBuilder sb = new StringBuilder();
                    addMedalString(c.getPlayer(), sb);
                    sb.append(c.getPlayer().getName());
                    sb.append(" : ");
                    sb.append(message);

                    final boolean ear = slea.readByte() > 0;

                    Item item = null;
                    if (slea.readByte() == 1) { //item
                        byte invType = (byte) slea.readInt();
                        byte pos = (byte) slea.readInt();
                        item = c.getPlayer().getInventory(MapleInventoryType.getByType(invType)).getItem(pos);
                    }
                    ServerLogger.getInstance().logChat(LogType.Chat.ItemMegaphone, c.getPlayer().getId(), c.getPlayer().getName(), message, "채널 : " + c.getRealChannelName() + " / 귀 : " + (ear ? "예" : "아니오"));
                    World.Broadcast.broadcastSmega(MaplePacketCreator.itemMegaphone(sb.toString(), ear, c.getChannel(), item));
                    used = true;
                } else {
                    c.getPlayer().dropMessage(5, "현재 확성기 사용 금지 상태입니다.");
                }
                break;
            }
            case 5152100:
            case 5152101:
            case 5152102:
            case 5152103:
            case 5152104:
            case 5152105:
            case 5152106:
            case 5152107:
                used = true;
                int face = ((c.getPlayer().getFace() / 1000) * 1000) + (c.getPlayer().getFace() % 100);
                face += (itemId % 10) * 100;
                c.getPlayer().setFace(face);
                c.getPlayer().updateSingleStat(MapleStat.FACE, face);
                c.getPlayer().equipChanged();
                break;
            case 5075000: // MapleTV Messenger
            case 5075001: // MapleTV Star Messenger
            case 5075002: { // MapleTV Heart Messenger
                c.getPlayer().dropMessage(5, "There are no MapleTVs to broadcast the message to.");
                break;
            }
            case 5075003:
            case 5075004:
            case 5075005: {
                if (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().dropMessage(5, "Must be level 10 or higher.");
                    break;
                }
                if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                    c.getPlayer().dropMessage(5, "Cannot be used here.");
                    break;
                }
                if (!c.getPlayer().getCheatTracker().canSmega()) {
                    c.getPlayer().dropMessage(5, "You may only use this every 15 seconds.");
                    break;
                }
                int tvType = itemId % 10;
                if (tvType == 3) {
                    slea.readByte(); //who knows
                }
                boolean ear = tvType != 1 && tvType != 2 && slea.readByte() > 1; //for tvType 1/2, there is no byte. 
                MapleCharacter victim = tvType == 1 || tvType == 4 ? null : c.getChannelServer().getPlayerStorage().getCharacterByName(slea.readMapleAsciiString()); //for tvType 4, there is no string.
                if (tvType == 0 || tvType == 3) { //doesn't allow two
                    victim = null;
                } else if (victim == null) {
                    c.getPlayer().dropMessage(1, "That character is not in the channel.");
                    break;
                }
                String message = slea.readMapleAsciiString();
                World.Broadcast.broadcastSmega(MaplePacketCreator.serverNotice(3, c.getChannel(), c.getPlayer().getName() + " : " + message, ear));
                used = true;
                break;
            }
            case 5090100: // Wedding Invitation Card
            case 5090000: { // Note
                final String sendTo = slea.readMapleAsciiString();
                final String msg = slea.readMapleAsciiString();
                int VictimChannel = World.Find.findChannel(sendTo);
                if (VictimChannel > 0) {
                    c.getPlayer().dropMessage(1, "상대방이 이미 게임에 접속 중입니다. 귓속말을 이용해 주세요.");
                } else {
                    c.getPlayer().sendNote(sendTo, msg);
                    c.getSession().write(CSPacket.SendNoteResult((byte) 4));
                    ServerLogger.getInstance().logChat(LogType.Chat.Note, c.getPlayer().getId(), c.getPlayer().getName(), msg, "수신 : " + sendTo);
                    used = true;
                }
                break;
            }
            case 5100000: { // Congratulatory Song
                c.getPlayer().getMap().startMapEffect(c.getPlayer().getName(), itemId, true); //CashSong
//                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.musicChange("Jukebox/Congratulation"));
                used = true;
                break;
            }
            case 5190001:
            case 5190002:
            case 5190003:
            case 5190004:
            case 5190005:
            case 5190006:
            case 5190007:
            case 5190008:
            case 5190000: { // Pet Flags
                final int uniqueid = (int) slea.readLong();
                MaplePet pet = c.getPlayer().getPet(0);
                int slo = 0;

                if (pet == null) {
                    break;
                }
                PetFlag zz = PetFlag.getByAddId(itemId);
                if (zz != null && !zz.check(pet.getFlags())) {
                    pet.setFlags(pet.getFlags() | zz.getValue());
                    c.getSession().write(PetPacket.updatePet(pet, c.getPlayer().getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition()), true));
                    c.getSession().write(MaplePacketCreator.enableActions());
                    c.getSession().write(MTSCSPacket.changePetFlag(uniqueid, true, zz.getValue()));
                    used = true;
                }
                break;
            }
            case 5191001:
            case 5191002:
            case 5191003:
            case 5191004:
            case 5191000: { // Pet Flags
                final int uniqueid = (int) slea.readLong();
                MaplePet pet = c.getPlayer().getPet(0);
                int slo = 0;

                if (pet == null) {
                    break;
                }
                if (pet.getUniqueId() != uniqueid) {
                    pet = c.getPlayer().getPet(1);
                    slo = 1;
                    if (pet != null) {
                        if (pet.getUniqueId() != uniqueid) {
                            pet = c.getPlayer().getPet(2);
                            slo = 2;
                            if (pet != null) {
                                if (pet.getUniqueId() != uniqueid) {
                                    break;
                                }
                            } else {
                                break;
                            }
                        }
                    } else {
                        break;
                    }
                }
                PetFlag zz = PetFlag.getByDelId(itemId);
                if (zz != null && zz.check(pet.getFlags())) {
                    pet.setFlags(pet.getFlags() - zz.getValue());
                    c.getSession().write(PetPacket.updatePet(pet, c.getPlayer().getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition()), true));
                    c.getSession().write(MaplePacketCreator.enableActions());
                    c.getSession().write(MTSCSPacket.changePetFlag(uniqueid, false, zz.getValue()));
                    used = true;
                }
                break;
            }
            case 5501001:
            case 5501002: { //expiry mount
                final Skill skil = SkillFactory.getSkill(slea.readInt());
                if (skil == null || skil.getId() / 10000 != 8000 || c.getPlayer().getSkillLevel(skil) <= 0 || !skil.isTimeLimited() || GameConstants.getMountItem(skil.getId(), c.getPlayer()) <= 0) {
                    break;
                }
                final long toAdd = (itemId == 5501001 ? 30 : 60) * 24 * 60 * 60 * 1000L;
                final long expire = c.getPlayer().getSkillExpiry(skil);
                if (expire < System.currentTimeMillis() || (long) (expire + toAdd) >= System.currentTimeMillis() + (365 * 24 * 60 * 60 * 1000L)) {
                    break;
                }
                c.getPlayer().changeSkillLevel(skil, c.getPlayer().getSkillLevel(skil), c.getPlayer().getMasterLevel(skil), (long) (expire + toAdd));
                used = true;
                break;
            }
            case 5170000: { // Pet name change
                final int uniqueid = (int) slea.readLong();
                MaplePet pet = c.getPlayer().getPet(0);
                int slo = 0;

                if (pet == null) {
                    break;
                }
                if (pet.getUniqueId() != uniqueid) {
                    pet = c.getPlayer().getPet(1);
                    slo = 1;
                    if (pet != null) {
                        if (pet.getUniqueId() != uniqueid) {
                            pet = c.getPlayer().getPet(2);
                            slo = 2;
                            if (pet != null) {
                                if (pet.getUniqueId() != uniqueid) {
                                    break;
                                }
                            } else {
                                break;
                            }
                        }
                    } else {
                        break;
                    }
                }
                String nName = slea.readMapleAsciiString();
                for (String z : GameConstants.RESERVED) {
                    if (pet.getName().indexOf(z) != -1 || nName.indexOf(z) != -1) {
                        break;
                    }
                }
                if (MapleCharacterUtil.canChangePetName(nName)) {
                    pet.setName(nName);
                    c.getSession().write(PetPacket.updatePet(pet, c.getPlayer().getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition()), true));
                    c.getSession().write(MaplePacketCreator.enableActions());
                    c.getPlayer().getMap().broadcastMessage(MTSCSPacket.changePetName(c.getPlayer(), nName, slo));
                    used = true;
                } else {
                }
                break;
            }
            case 5700000: {
                slea.skip(8);
                if (c.getPlayer().getAndroid() == null) {
                    break;
                }
                String nName = slea.readMapleAsciiString();
                for (String z : GameConstants.RESERVED) {
                    if (c.getPlayer().getAndroid().getName().indexOf(z) != -1 || nName.indexOf(z) != -1) {
                        break;
                    }
                }
                if (MapleCharacterUtil.canChangePetName(nName)) {
                    c.getPlayer().getAndroid().setName(nName);
                    c.getPlayer().setAndroid(c.getPlayer().getAndroid()); //respawn it
                    used = true;
                }
                break;
            }
            case 5240000:
            case 5240001:
            case 5240002:
            case 5240003:
            case 5240004:
            case 5240005:
            case 5240006:
            case 5240007:
            case 5240008:
            case 5240009:
            case 5240010:
            case 5240011:
            case 5240012:
            case 5240013:
            case 5240014:
            case 5240015:
            case 5240016:
            case 5240017:
            case 5240018:
            case 5240019:
            case 5240020:
            case 5240021:
            case 5240022:
            case 5240023:
            case 5240024:
            case 5240025:
            case 5240026:
            case 5240027:
            case 5240029:
            case 5240030:
            case 5240031:
            case 5240032:
            case 5240033:
            case 5240034:
            case 5240035:
            case 5240036:
            case 5240037:
            case 5240038:
            case 5240039:
            case 5240040:
            case 5240028: { // Pet food
                MaplePet pet = c.getPlayer().getPet(0);

                if (pet == null) {
                    break;
                }
                if (!pet.canConsume(itemId)) {
                    pet = c.getPlayer().getPet(1);
                    if (pet != null) {
                        if (!pet.canConsume(itemId)) {
                            pet = c.getPlayer().getPet(2);
                            if (pet != null) {
                                if (!pet.canConsume(itemId)) {
                                    break;
                                }
                            } else {
                                break;
                            }
                        }
                    } else {
                        break;
                    }
                }
                final byte petindex = c.getPlayer().getPetIndex(pet);
                pet.setFullness(100);
                if (pet.getCloseness() < 30000) {
                    if (pet.getCloseness() + (100 * c.getChannelServer().getTraitRate()) > 30000) {
                        pet.setCloseness(30000);
                    } else {
                        pet.setCloseness(pet.getCloseness() + (100));
                    }
                    if (pet.getCloseness() >= GameConstants.getClosenessNeededForLevel(pet.getLevel() + 1)) {
                        pet.setLevel(pet.getLevel() + 1);
                        c.getSession().write(PetPacket.showOwnPetLevelUp(c.getPlayer().getPetIndex(pet)));
                        c.getPlayer().getMap().broadcastMessage(PetPacket.showPetLevelUp(c.getPlayer(), petindex));
                    }
                }
                c.getSession().write(PetPacket.updatePet(pet, c.getPlayer().getInventory(MapleInventoryType.CASH).getItem(pet.getInventoryPosition()), true));
                c.getPlayer().getMap().broadcastMessage(c.getPlayer(), PetPacket.commandResponse(c.getPlayer().getId(), (byte) 1, petindex, true, true), true);
                used = true;
                break;
            }
            case 5230001:
            case 5230000: {// owl of minerva
                final int itemSearch = slea.readInt();
                MinervaOwlSearchTop.getInstance().searchItem(itemSearch);
                final List<AbstractPlayerStore> hms = c.getChannelServer().searchShop(itemSearch);
                if (hms.size() > 0) {
                    c.getSession().write(MaplePacketCreator.getOwlSearched(itemSearch, hms));
                    used = true;
                } else {
                    c.getPlayer().dropMessage(1, "아이템을 발견하지 못하였습니다.");
                }
                break;
            }
            case 5080000: //티썬님소스.
            case 5080001:
            case 5080002:
            case 5080003: {
                final Point pos = c.getPlayer().getPosition();
                used = true;
                List<MapleMapObjectType> list = new LinkedList<MapleMapObjectType>();
                list.add(MapleMapObjectType.NPC);
                list.add(MapleMapObjectType.MESSAGEBOX);
                list.add(MapleMapObjectType.HIRED_MERCHANT);
                list.add(MapleMapObjectType.SHOP);
                if (!c.getPlayer().getMap().getMapObjectsInRange(pos, 30000, list).isEmpty()) {
                    used = false;
                    MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(2);
                    mplew.writeShort(SendPacketOpcode.FAIL_SPAWN_MEESAGEBOX.getValue());
                    c.getSession().write(mplew.getPacket());
                    break;
                }
                list.clear();
                if (used) {
                    String owner = c.getPlayer().getName();
                    String message = slea.readMapleAsciiString();
                    MapleMessageBox mmb = new MapleMessageBox(itemId, pos, owner, message);
                    c.getPlayer().getMap().spawnMessageBox(mmb);
                    ServerLogger.getInstance().logChat(LogType.Chat.MessageBox, c.getPlayer().getId(), c.getPlayer().getName(), message, "채널 : " + c.getRealChannelName() + " / 맵 : " + c.getPlayer().getMapId() + " / 아이템 : " + MapleItemInformationProvider.getInstance().getName(itemId));
                }
                break;
            }

            /*case 5281001: //idk, but probably
             case 5280001: // Gas Skill
             case 5281000: { // Passed gas
             Rectangle bounds = new Rectangle((int) c.getPlayer().getPosition().getX(), (int) c.getPlayer().getPosition().getY(), 1, 1);
             MapleMist mist = new MapleMist(bounds, c.getPlayer());
             c.getPlayer().getMap().spawnMist(mist, 10000, true);
             c.getSession().write(MaplePacketCreator.enableActions());
             used = true;
             break;
             }*/
            case 5370001:
            case 5370000: { // Chalkboard
                for (MapleEventType t : MapleEventType.values()) {
                    final MapleEvent e = ChannelServer.getInstance(c.getChannel()).getEvent(t);
                    if (e.isRunning()) {
                        for (int i : e.getType().mapids) {
                            if (c.getPlayer().getMapId() == i) {
                                c.getPlayer().dropMessage(5, "You may not use that here.");
                                c.getSession().write(MaplePacketCreator.enableActions());
                                return;
                            }
                        }
                    }
                }
                c.getPlayer().setChalkboard(slea.readMapleAsciiString());
                break;
            }
            case 5079000:
            case 5079001:
            case 5390007:
            case 5390008:
            case 5390009:
            case 5390000: // Diablo Messenger
            case 5390001: // Cloud 9 Messenger
            case 5390002: // Loveholic Messenger
            case 5390003: // New Year Megassenger 1
            case 5390004: // New Year Megassenger 2
            case 5390005: // Cute Tiger Messenger
            case 5390006: { // Tiger Roar's Messenger
                if (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().dropMessage(5, "Must be level 10 or higher.");
                    break;
                }
                if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                    c.getPlayer().dropMessage(5, "Cannot be used here.");
                    break;
                }
                if (!c.getPlayer().getCheatTracker().canAvatarSmega()) {
                    c.getPlayer().dropMessage(5, "You may only use this every 5 minutes.");
                    break;
                }
                if (!c.getChannelServer().getMegaphoneMuteState()) {
                    final String text = slea.readMapleAsciiString();
                    if (text.length() > 55) {
                        break;
                    }
                    final boolean ear = slea.readByte() != 0;
//                    World.Broadcast.broadcastSmega(MaplePacketCreator.getAvatarMega(c.getPlayer(), c.getChannel(), itemId, text, ear));
                    used = true;
                } else {
                    c.getPlayer().dropMessage(5, "The usage of Megaphone is currently disabled.");
                }
                break;
            }
            case 5452001:
            case 5450003:
            case 5450000: { // Mu Mu the Travelling Merchant
                for (int i : GameConstants.blockedMaps) {
                    if (c.getPlayer().getMapId() == i) {
                        c.getPlayer().dropMessage(5, "현재맵에서는 사용이 불가능 합니다..");
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    }
                }
                if (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().dropMessage(5, "레벨이 10보다 낮아 사용하실 수 없습니다.");
                } else if (c.getPlayer().hasBlockedInventory() || c.getPlayer().getMap().getSquadByMap() != null || c.getPlayer().getEventInstance() != null || c.getPlayer().getMap().getEMByMap() != null || c.getPlayer().getMapId() >= 990000000) {
                    c.getPlayer().dropMessage(5, "이 곳에서 사용하실 수 없습니다.");
                } else if ((c.getPlayer().getMapId() >= 680000210 && c.getPlayer().getMapId() <= 680000502) || (c.getPlayer().getMapId() / 1000 == 980000 && c.getPlayer().getMapId() != 980000000) || (c.getPlayer().getMapId() / 100 == 1030008) || (c.getPlayer().getMapId() / 100 == 922010) || (c.getPlayer().getMapId() / 10 == 13003000)) {
                    c.getPlayer().dropMessage(5, "이 곳에서 사용하실 수 없습니다.");
                } else {
                    MapleShopFactory.getInstance().getShop(9090000).sendShop(c);
                }
                used = true;
                break;
            }
            default:
                if (itemId / 10000 == 512) {
                    final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    String msg = ii.getMsg(itemId);
                    final String ourMsg = slea.readMapleAsciiString();
                    c.getPlayer().getMap().startMapEffect(/*msg*/ourMsg, itemId);
                    final int buff = ii.getStateChangeItem(itemId);
                    if (buff != 0) {
                        for (MapleCharacter mChar : c.getPlayer().getMap().getCharactersThreadsafe()) {
                            ii.getItemEffect(buff).applyTo(mChar);
                        }
                    }
                    ServerLogger.getInstance().logChat(LogType.Chat.Weather, c.getPlayer().getId(), c.getPlayer().getName(), ourMsg, "아이템 : " + MapleItemInformationProvider.getInstance().getName(itemId) + " / 맵 : " + c.getPlayer().getMapId() + " / 채널 : " + c.getRealChannelName());
                    used = true;
                } else if (itemId / 10000 == 510) {
                    c.getPlayer().getMap().startJukebox(c.getPlayer().getName(), itemId);
                    used = true;
                } else if (itemId / 10000 == 562) {
                    if (UseSkillBook(slot, itemId, c, c.getPlayer())) {
                        c.getPlayer().gainSP(1);
                    }
                } else if (itemId / 10000 == 553) {
                    UseRewardItem(slot, itemId, c, c.getPlayer());// this too
                } else if (itemId / 10000 != 519) {
                    System.out.println("Unhandled CS item : " + itemId);
                    System.out.println(slea.toString(true));
                }
                break;
        }

        if (used) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, slot, (short) 1, false, true);
        }
        c.getSession().write(MaplePacketCreator.enableActions());
        if (cc) {
            if (!c.getPlayer().isAlive() || c.getPlayer().getEventInstance() != null || FieldLimitType.ChannelSwitch.check(c.getPlayer().getMap().getFieldLimit())) {
                c.getPlayer().dropMessage(1, "Auto relog failed.");
                return;
            }
            c.getPlayer().dropMessage(5, "Auto relogging. Please wait.");
            c.getPlayer().fakeRelog();
            if (c.getPlayer().getScrolledPosition() != 0) {
                c.getSession().write(MaplePacketCreator.pamSongUI());
            }
        }
    }

    public static final void Pickup_Player(final LittleEndianAccessor slea, MapleClient c, final MapleCharacter chr) {
        if (c.getPlayer().hasBlockedInventory()) { //hack
            return;
        }
        //slea.skip(4); 1229
        chr.updateTick(slea.readInt());
        c.getPlayer().setScrolledPosition((short) 0);
        slea.skip(1); // or is this before tick?
        final Point Client_Reportedpos = slea.readPos();
        if (chr == null || chr.getMap() == null) {
            return;
        }
        final MapleMapObject ob = chr.getMap().getMapObject(slea.readInt(), MapleMapObjectType.ITEM);

        if (ob == null) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        final MapleMapItem mapitem = (MapleMapItem) ob;
        final Lock lock = mapitem.getLock();
        lock.lock();
        try {
            if (mapitem.isPickedUp()) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            if (mapitem.getQuest() > 0 && chr.getQuestStatus(mapitem.getQuest()) != 1) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            if (mapitem.getOwner() != chr.getId() && ((!mapitem.isPlayerDrop() && mapitem.getDropType() == 0) || (mapitem.isPlayerDrop() && chr.getMap().getEverlast()))) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            if (!mapitem.isPlayerDrop() && mapitem.getDropType() == 1 && mapitem.getOwner() != chr.getId() && (chr.getParty() == null || chr.getParty().getMemberById(mapitem.getOwner()) == null)) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            final double Distance = Client_Reportedpos.distanceSq(mapitem.getPosition());
            /*if (Distance > 5000 && (mapitem.getMeso() > 0 || mapitem.getItemId() != 4001025)) {
             chr.getCheatTracker().registerOffense(CheatingOffense.ITEMVAC_CLIENT, String.valueOf(Distance));
             } else if (chr.getPosition().distanceSq(mapitem.getPosition()) > 640000.0) {
             chr.getCheatTracker().registerOffense(CheatingOffense.ITEMVAC_SERVER);
             }*/ //자석버그방지
            if (mapitem.getMeso() > 0) {
                if (chr.getParty() != null && mapitem.getOwner() != chr.getId()) {
                    final List<MapleCharacter> toGive = new LinkedList<MapleCharacter>();
                    final int splitMeso = mapitem.getMeso() * 40 / 100;
                    for (MaplePartyCharacter z : chr.getParty().getMembers()) {
                        MapleCharacter m = chr.getMap().getCharacterById(z.getId());
                        if (m != null && m.getId() != chr.getId()) {
                            toGive.add(m);
                        }
                    }
                    for (final MapleCharacter m : toGive) {
                        m.gainMeso(splitMeso / toGive.size() + (m.getStat().hasPartyBonus ? (int) (mapitem.getMeso() / 20.0) : 0), true);
                        c.getSession().write(MaplePacketCreator.enableActions());
                    }
                    chr.gainMeso(mapitem.getMeso() - splitMeso, true);
                    c.getSession().write(MaplePacketCreator.enableActions());
                } else {
                    chr.gainMeso(mapitem.getMeso(), true);
                    c.getSession().write(MaplePacketCreator.enableActions());
                }
                removeItem(chr, mapitem, ob);
            } else {
                if (MapleItemInformationProvider.getInstance().isPickupBlocked(mapitem.getItemId())) {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    c.getPlayer().dropMessage(5, "이 아이템은 주울 수 없습니다.");
                } else if (c.getPlayer().inPVP() && Integer.parseInt(c.getPlayer().getEventInstance().getProperty("ice")) == c.getPlayer().getId()) {
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                    c.getSession().write(MaplePacketCreator.enableActions());
                } else if (useItem(c, mapitem.getItemId())) {
                    removeItem(c.getPlayer(), mapitem, ob);
                    //another hack
                    if (mapitem.getItemId() / 10000 == 291) {
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getCapturePosition(c.getPlayer().getMap()));
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.resetCapture());
                    }
                } else if (mapitem.getItemId() / 10000 != 291 && MapleInventoryManipulator.checkSpace(c, mapitem.getItemId(), mapitem.getItem().getQuantity(), mapitem.getItem().getOwner())) {
                    if (mapitem.getItem().getQuantity() >= 50 && mapitem.getItemId() == 2340000) {
                        c.setMonitored(true); //hack check
                    }
                    if (mapitem.isPlayerDrop()) {
                        ServerLogger.getInstance().logTrade(LogType.Trade.DropAndPick, c.getPlayer().getId(), c.getPlayer().getName(), mapitem.getDropperName(), MapleItemInformationProvider.getInstance().getName(mapitem.getItem().getItemId()) + " " + mapitem.getItem().getQuantity() + "개", "맵 : " + c.getPlayer().getMapId());
                    }

                    MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true, mapitem.getDropper() instanceof MapleMonster);
                    removeItem(chr, mapitem, ob);
                } else {
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                    c.getSession().write(MaplePacketCreator.enableActions());
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public static final void Pickup_Pet(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        if (c.getPlayer().hasBlockedInventory()) { //hack
            return;
        }

        c.getPlayer().setScrolledPosition((short) 0);
        final byte petz = (byte) (GameConstants.GMS ? (c.getPlayer().getPetIndex((int) slea.readLong())) : slea.readInt());
        final MaplePet pet = chr.getPet(petz);
        slea.skip(1); // [4] Zero, [4] Seems to be tickcount, [1] Always zero
        chr.updateTick(slea.readInt());
        final Point Client_Reportedpos = slea.readPos();
        final MapleMapObject ob = chr.getMap().getMapObject(slea.readInt(), MapleMapObjectType.ITEM);

        if (ob == null || pet == null) {
            return;
        }
        final MapleMapItem mapitem = (MapleMapItem) ob;
        final Lock lock = mapitem.getLock();
        lock.lock();
        try {
            if (mapitem.isPickedUp()) {
                c.getSession().write(MaplePacketCreator.getInventoryFull());
                return;
            }
            if (mapitem.getOwner() != chr.getId() && mapitem.isPlayerDrop()) {
                return;
            }
            if (mapitem.getOwner() != chr.getId() && ((!mapitem.isPlayerDrop() && mapitem.getDropType() == 0) || (mapitem.isPlayerDrop() && chr.getMap().getEverlast()))) {
                return;
            }
            if (!mapitem.isPlayerDrop() && mapitem.getDropType() == 1 && mapitem.getOwner() != chr.getId() && (chr.getParty() == null || chr.getParty().getMemberById(mapitem.getOwner()) == null)) {
                return;
            }
            if (mapitem.getMeso() > 0) {
                if (chr.getParty() != null && mapitem.getOwner() != chr.getId()) {
                    final List<MapleCharacter> toGive = new LinkedList<MapleCharacter>();
                    final int splitMeso = mapitem.getMeso() * 40 / 100;
                    for (MaplePartyCharacter z : chr.getParty().getMembers()) {
                        MapleCharacter m = chr.getMap().getCharacterById(z.getId());
                        if (m != null && m.getId() != chr.getId()) {
                            toGive.add(m);
                        }
                    }
                    for (final MapleCharacter m : toGive) {
                        m.gainMeso(splitMeso / toGive.size() + (m.getStat().hasPartyBonus ? (int) (mapitem.getMeso() / 20.0) : 0), true);
                    }
                    chr.gainMeso(mapitem.getMeso() - splitMeso, true);
                } else {
                    chr.gainMeso(mapitem.getMeso(), true);
                }
                removeItem_Pet(chr, mapitem, petz);
            } else {
                if (MapleItemInformationProvider.getInstance().isPickupBlocked(mapitem.getItemId()) || mapitem.getItemId() / 10000 == 291) {
                    c.sendPacket(MaplePacketCreator.enableActions()); //ExclRequestSent TEST
//                    c.getSession().write(MaplePacketCreator.enableActions(false));
                } else if (useItem(c, mapitem.getItemId())) {
                    removeItem_Pet(chr, mapitem, petz);
                } else if (MapleInventoryManipulator.checkSpace(c, mapitem.getItemId(), mapitem.getItem().getQuantity(), mapitem.getItem().getOwner())) {
                    if (mapitem.getItem().getQuantity() >= 50 && mapitem.getItemId() == 2340000) {
                        c.setMonitored(true); //hack check
                    }
                    MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true, mapitem.getDropper() instanceof MapleMonster, false);
                    if (mapitem.isPlayerDrop()) {
                        ServerLogger.getInstance().logTrade(LogType.Trade.DropAndPick, c.getPlayer().getId(), c.getPlayer().getName(), mapitem.getDropperName(), MapleItemInformationProvider.getInstance().getName(mapitem.getItem().getItemId()) + " " + mapitem.getItem().getQuantity() + "개", "맵 : " + c.getPlayer().getMapId());
                    }
                    removeItem_Pet(chr, mapitem, petz);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public static final boolean useItem(final MapleClient c, final int id) {
        if (GameConstants.isUse(id)) { // TO prevent caching of everything, waste of mem
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            final MapleStatEffect eff = ii.getItemEffect(id);
            if (eff == null) {
                return false;
            }
            //must hack here for ctf
            if (id / 10000 == 291) {
                boolean area = false;
                for (Rectangle rect : c.getPlayer().getMap().getAreas()) {
                    if (rect.contains(c.getPlayer().getTruePosition())) {
                        area = true;
                        break;
                    }
                }
                if (!c.getPlayer().inPVP() || (c.getPlayer().getTeam() == (id - 2910000) && area)) {
                    return false; //dont apply the consume
                }
            }
            final int consumeval = eff.getConsume();

            if (consumeval > 0) {
                consumeItem(c, eff);
                consumeItem(c, ii.getItemEffectEX(id));
                c.getSession().write(MaplePacketCreator.getShowItemGain(id, (byte) 1));
                return true;
            }
        }
        return false;
    }

    public static final void consumeItem(final MapleClient c, final MapleStatEffect eff) {
        if (eff == null) {
            return;
        }
        if (eff.getConsume() == 2) {
            if (c.getPlayer().getParty() != null && c.getPlayer().isAlive()) {
                for (final MaplePartyCharacter pc : c.getPlayer().getParty().getMembers()) {
                    final MapleCharacter chr = c.getPlayer().getMap().getCharacterById(pc.getId());
                    if (chr != null && chr.isAlive()) {
                        eff.applyTo(chr);
                    }
                }
            } else {
                eff.applyTo(c.getPlayer());
            }
        } else if (c.getPlayer().isAlive()) {
            eff.applyTo(c.getPlayer());
        }
    }

    public static final void removeItem_Pet(final MapleCharacter chr, final MapleMapItem mapitem, int pet) {
        mapitem.setPickedUp(true);
        chr.getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 5, chr.getId(), pet));
        chr.getMap().removeMapObject(mapitem);
        if (mapitem.isRandDrop()) {
            chr.getMap().spawnRandDrop();
        }
    }

    private static final void removeItem(final MapleCharacter chr, final MapleMapItem mapitem, final MapleMapObject ob) {
        mapitem.setPickedUp(true);
        chr.getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 2, chr.getId()), mapitem.getPosition());
        chr.getMap().removeMapObject(ob);
        if (mapitem.isRandDrop()) {
            chr.getMap().spawnRandDrop();
        }
    }

    private static final void addMedalString(final MapleCharacter c, final StringBuilder sb) {
        final Item medal = c.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -21);
        if (medal != null) { // Medal
            sb.append("<");
            sb.append(MapleItemInformationProvider.getInstance().getName(medal.getItemId()).replace("의 훈장", ""));
            sb.append("> ");
        }
    }

    private static final boolean getPeanutItems(MapleClient c, int itemId) {
        if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() < 2 || c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() < 2 || c.getPlayer().getInventory(MapleInventoryType.SETUP).getNumFreeSlot() < 2) {
            c.getPlayer().dropMessage(5, "장비창 1개, 소비창 2개, 기타창 1개의 공간이 필요합니다.");
            return false;
        }
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        int id1 = RandomRewards.getPeanutReward(), id2 = RandomRewards.getPeanutRidingReward();
        while (!ii.itemExists(id1)) {
            id1 = RandomRewards.getPeanutReward();
        }
        while (!ii.itemExists(id2)) {
            id2 = RandomRewards.getPeanutRidingReward();
        }
        c.getSession().write(MaplePacketCreator.getPeanutResult(id1, (short) 1, id2, (short) 1, itemId));
        MapleInventoryManipulator.addById(c, id1, (short) 1, ii.getName(itemId) + " on " + FileoutputUtil.CurrentReadable_Date());
        MapleInventoryManipulator.addById(c, id2, (short) 1, ii.getName(itemId) + " on " + FileoutputUtil.CurrentReadable_Date());
        return true;
    }

    public static final void OwlMinerva(final LittleEndianAccessor slea, final MapleClient c) {
        final byte slot = (byte) slea.readShort();
        final int itemid = slea.readInt();
        final Item toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        if (toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemid && itemid == 2310000 && !c.getPlayer().hasBlockedInventory()) {
            final int itemSearch = slea.readInt();
            final List<AbstractPlayerStore> hms = c.getChannelServer().searchShop(itemSearch);
            if (hms.size() > 0) {
                c.getSession().write(MaplePacketCreator.getOwlSearched(itemSearch, hms));
                MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, itemid, 1, true, false);
            } else {
                c.getPlayer().dropMessage(1, "아이템을 발견하지 못하였습니다.");
            }
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static final void Owl(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer().haveItem(5230000, 1, true, false) || c.getPlayer().haveItem(2310000, 1, true, false)) {
            if (c.getPlayer().getMapId() >= 910000000 && c.getPlayer().getMapId() <= 910000022) {
                c.getSession().write(MaplePacketCreator.getOwlOpen());
            } else {
                c.getPlayer().dropMessage(5, "자유시장 내에서만 사용할 수 있습니다.");
                c.getSession().write(MaplePacketCreator.enableActions());
            }
        }
    }
    public static final int OWL_ID = 2; //don't change. 0 = owner ID, 1 = store ID, 2 = object ID

    public static final void OwlWarp(final LittleEndianAccessor slea, final MapleClient c) {
        c.getSession().write(MaplePacketCreator.enableActions());
        if (c.getPlayer().getMapId() >= 910000000 && c.getPlayer().getMapId() <= 910000022 && !c.getPlayer().hasBlockedInventory()) {
            final int id = slea.readInt();
            final int map = slea.readInt();
            if (map >= 910000001 && map <= 910000022) {
                final MapleMap mapp = c.getChannelServer().getMapFactory().getMap(map);
                c.getPlayer().changeMap(mapp, mapp.getPortal(0));
                AbstractPlayerStore merchant = null;
                List<MapleMapObject> objects;
                switch (OWL_ID) {
                    case 0:
                        boolean bln = false;
                        objects = mapp.getAllHiredMerchantsThreadsafe();
                        for (MapleMapObject ob : objects) {
                            if (ob instanceof IMaplePlayerShop) {
                                final IMaplePlayerShop ips = (IMaplePlayerShop) ob;
                                if (ips instanceof HiredMerchant) {
                                    final HiredMerchant merch = (HiredMerchant) ips;
                                    if (merch.getOwnerId() == id) {
                                        merchant = merch;
                                        bln = true;
                                        break;
                                    }
                                }
                            }
                        }
                        if (!bln) {
                            List<MapleCharacter> objs = mapp.getCharactersThreadsafe();
                            for (MapleCharacter chr : objs) {
                                if (chr.getPlayerShop() != null && chr.getMapId() == map) {
                                    if (chr.getPlayerShop() instanceof MaplePlayerShop) {
                                        MaplePlayerShop shop = (MaplePlayerShop) chr.getPlayerShop();
                                        if (shop.isOpen()) {
                                            if (shop.getOwnerId() == id) {
                                                merchant = shop;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    case 1:
                        objects = mapp.getAllHiredMerchantsThreadsafe();
                        for (MapleMapObject ob : objects) {
                            if (ob instanceof IMaplePlayerShop) {
                                final IMaplePlayerShop ips = (IMaplePlayerShop) ob;
                                if (ips instanceof HiredMerchant) {
                                    final HiredMerchant merch = (HiredMerchant) ips;
                                    if (merch.getStoreId() == id) {
                                        merchant = merch;
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    default:
                        final MapleMapObject ob = mapp.getMapObject(id, MapleMapObjectType.HIRED_MERCHANT);
                        if (ob instanceof IMaplePlayerShop) {
                            final IMaplePlayerShop ips = (IMaplePlayerShop) ob;
                            if (ips instanceof HiredMerchant) {
                                merchant = (HiredMerchant) ips;
                            }
                        }
                        final MapleMapObject ob2 = mapp.getMapObject(id, MapleMapObjectType.SHOP);
                        if (ob2 instanceof IMaplePlayerShop) {
                            final IMaplePlayerShop ips = (IMaplePlayerShop) ob2;
                            if (ips instanceof MaplePlayerShop) {
                                merchant = (MaplePlayerShop) ips;
                            }
                        }
                        break;
                }
                if (merchant != null) {
                    if (merchant instanceof HiredMerchant) {
                        if (merchant.isOwner(c.getPlayer())) {
                            HiredMerchant merc = (HiredMerchant) merchant;
                            merc.setOpen(false);
                            merc.removeAllVisitors((byte) 16, (byte) 0);
                            c.getPlayer().setPlayerShop(merc);
                            c.getSession().write(PlayerShopPacket.getHiredMerch(c.getPlayer(), merc, false));
                        } else {
                            HiredMerchant merc = (HiredMerchant) merchant;
                            if (!merc.isOpen() || !merc.isAvailable()) {
                                c.getPlayer().dropMessage(1, "상점이 준비중에 있습니다. 잠시 후에 다시 시도해 주세요.");
                            } else {
                                if (merc.getFreeSlot() == -1) {
                                    c.getPlayer().dropMessage(1, "상점 최대 수용 인원을 초과하였습니다.");
                                } else if (merc.isInBlackList(c.getPlayer().getName())) {
                                    c.getPlayer().dropMessage(1, "당신은 이 상점에 입장이 금지되었습니다.");
                                } else {
                                    c.getPlayer().setPlayerShop(merc);
                                    merc.addVisitor(c.getPlayer());
                                    c.getSession().write(PlayerShopPacket.getHiredMerch(c.getPlayer(), merc, false));
                                }
                            }
                        }
                    } else if (merchant instanceof MaplePlayerShop) {
                        if (((MaplePlayerShop) merchant).isBanned(c.getPlayer().getName())) {
                            c.getPlayer().dropMessage(1, "당신은 이 상점에 입장이 금지되었습니다.");
                            return;
                        } else {
                            if (merchant.getFreeSlot() < 0 || merchant.getVisitorSlot(c.getPlayer()) > -1 || !merchant.isOpen() || !merchant.isAvailable()) {
                                c.getSession().write(PlayerShopPacket.getMiniGameFull());
                            } else {
                                c.getPlayer().setPlayerShop(merchant);
                                merchant.addVisitor(c.getPlayer());
                                c.getSession().write(PlayerShopPacket.getPlayerStore(c.getPlayer(), false));
                            }
                        }
                    }
                } else {
                    c.getPlayer().dropMessage(1, "상점을 발견하지 못하였습니다.");
                }
            }
        }
    }

    public static final void PamSong(LittleEndianAccessor slea, MapleClient c) {
        final Item pam = c.getPlayer().getInventory(MapleInventoryType.CASH).findById(5640000);
        if (slea.readByte() > 0 && c.getPlayer().getScrolledPosition() != 0 && pam != null && pam.getQuantity() > 0) {
            final MapleInventoryType inv = c.getPlayer().getScrolledPosition() < 0 ? MapleInventoryType.EQUIPPED : MapleInventoryType.EQUIP;
            final Item item = c.getPlayer().getInventory(inv).getItem(c.getPlayer().getScrolledPosition());
            c.getPlayer().setScrolledPosition((short) 0);
            if (item != null) {
                final Equip eq = (Equip) item;
                eq.setUpgradeSlots((byte) (eq.getUpgradeSlots() + 1));
                c.getPlayer().forceReAddItem_Flag(eq, inv);
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, pam.getPosition(), (short) 1, true, false);
//                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.pamsSongEffect(c.getPlayer().getId()));
            }
        } else {
            c.getPlayer().setScrolledPosition((short) 0);
        }
    }

    public static final void TeleRock(LittleEndianAccessor slea, MapleClient c) {
        final byte slot = (byte) slea.readShort();
        final int itemId = slea.readInt();
        final Item toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId || itemId / 10000 != 232 || c.getPlayer().hasBlockedInventory()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        boolean used = UseTeleRock(slea, c, itemId);
        if (used) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static void QuestPotOpen(LittleEndianAccessor slea, MapleClient c) {
        int qid = slea.readUShort();
        MapleQuest q = MapleQuest.getInstance(qid);
        if (q != null && c.getPlayer().getQuestNoAdd(q) != null) {
            MapleQuestStatus qs = c.getPlayer().getQuestNoAdd(q);
            if (qs.getCustomData() == null || qs.getCustomData().isEmpty()) {
                return;
            }
            if (qs.getStatus() != 1) { //진행 중이 아닐 때
                q.forceStart(c.getPlayer(), qid, "0");
                qs = c.getPlayer().getQuestNoAdd(q);
            }
            c.getSession().write(MaplePacketCreator.updateQuest(qs));
            if (c.getPlayer().isGM()) {
                c.getPlayer().dropMessage(6, "" + qs.getCustomData());
            }
        }
    }

    /*
     [R] C5 00 03 00 F8 85 3D 00 9B 1B 00 00 64 00 00 00
     �..�=.�..d...
     */
    public static void QuestPotFeed(LittleEndianAccessor slea, MapleClient c) {
        short slot = slea.readShort();
        int itemid = slea.readInt();
        int qid = slea.readUShort();
        slea.skip(2);
        int value = slea.readInt();
        Item item = c.getPlayer().getInventory(MapleInventoryType.ETC).getItem(slot);
        if (item == null || item.getItemId() != itemid || item.getQuantity() <= 0) {
            return;
        }
        if (qid == 7067) {
            if (qid != 7067) { // 작전 3단계 : 아기새
                c.sendPacket(MaplePacketCreator.enableActions());
                return;
            }

            MapleQuest q = MapleQuest.getInstance(qid);
            if (q != null && c.getPlayer().getQuestNoAdd(q) != null) {
                MapleQuestStatus qs = c.getPlayer().getQuestNoAdd(q);
                if (qs.getCustomData() == null || qs.getCustomData().isEmpty()) {
                    return;
                }
                int feeds = Integer.parseInt(qs.getCustomData());
                if (feeds < 3000) {
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.ETC, slot, (short) 1, false);
                    feeds = Math.min(feeds + 100, 3000);
                    qs.setCustomData(feeds + "");
                    if (feeds == 3000) {
                        c.sendPacket(MaplePacketCreator.getShowQuestCompletion(3250));
                    }
                }
                c.getSession().write(MaplePacketCreator.updateQuest(qs));
            }
        } else if (qid == 7691) {
            if (!c.getPlayer().haveItem(4220045)) {
                c.getPlayer().dropMessage(6, "상자를 구매한뒤 사용해 주세요.");
                c.sendPacket(MaplePacketCreator.enableActions());
                return;
            }
            if (qid != 7691) { //5주년 케이크
                c.sendPacket(MaplePacketCreator.enableActions());
                return;
            }
            MapleQuest q = MapleQuest.getInstance(qid);
            if (c.getPlayer().getQuestNoAdd(q) == null) {
                q.forceStart(c.getPlayer(), 9000021, "0");
            }
            if (q != null) {
                MapleQuestStatus qs = c.getPlayer().getQuestNoAdd(q);
                if (qs.getCustomData() == null || qs.getCustomData().isEmpty()) {
                    qs.setCustomData("0");
                }
                int feeds = Integer.parseInt(qs.getCustomData());
                if (feeds < 500) {
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.ETC, slot, (short) 1, false);
                    feeds = (feeds + value);
                    qs.setCustomData(feeds + "");
                } else if (feeds == 500) {
                    //c.sendPacket(MaplePacketCreator.getShowQuestCompletion(9983));
                }
                c.getSession().write(MaplePacketCreator.updateQuest(qs));
            }
        } else if (qid == 21763) {
            MapleQuest q = MapleQuest.getInstance(qid);
            if (q != null && c.getPlayer().getQuestNoAdd(q) != null) {
                MapleQuestStatus qs = c.getPlayer().getQuestNoAdd(q);
                if (qs.getCustomData() == null || qs.getCustomData().isEmpty()) {
                    return;
                }
                int feeds = Integer.parseInt(qs.getCustomData());
                if (feeds < 800) {
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.ETC, slot, (short) 1, false);
                    feeds = Math.min(feeds + value, 3000);
                    qs.setCustomData(feeds + "");
                    if (feeds == 800) {
                        //c.sendPacket(MaplePacketCreator.getShowQuestCompletion(21763));
                    }
                }
                c.getSession().write(MaplePacketCreator.updateQuest(qs));
            }
        } else if (qid == 10288) {
            MapleQuest q = MapleQuest.getInstance(qid);
            if (q != null && c.getPlayer().getQuestNoAdd(q) != null) {
                MapleQuestStatus qs = c.getPlayer().getQuestNoAdd(q);
                if (qs.getCustomData() == null || qs.getCustomData().isEmpty()) {
                    return;
                }
                int feeds = Integer.parseInt(qs.getCustomData());
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.ETC, slot, (short) 1, false);
                feeds = Math.min(feeds + value, 2000);
                qs.setCustomData(feeds + "");
                if (feeds == 800) {
                    //c.sendPacket(MaplePacketCreator.getShowQuestCompletion(21763));
                }
                c.getSession().write(MaplePacketCreator.updateQuest(qs));
            }
        } else {
            MapleQuest q = MapleQuest.getInstance(qid);
            if (q != null && c.getPlayer().getQuestNoAdd(q) != null) {
                MapleQuestStatus qs = c.getPlayer().getQuestNoAdd(q);
                if (qs.getCustomData() == null || qs.getCustomData().isEmpty()) {
                    return;
                }
                int feeds = Integer.parseInt(qs.getCustomData());
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.ETC, slot, (short) 1, false);
                feeds = Math.min(feeds + value, 50000);
                qs.setCustomData(feeds + "");
                c.getSession().write(MaplePacketCreator.updateQuest(qs));
            }
        }
        c.sendPacket(MaplePacketCreator.enableActions());
    }

    public static final void ChoosePqReward(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        byte type = slea.readByte();
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        int itemid = 0;
        int itemType = Randomizer.nextInt(100);
        if (itemType <= 70) {//포션 70퍼센트
            while (!ii.itemExists(itemid)) {
                itemid = RandomRewards.getPQResultE();
            }
            //chr.dropMessage(6, "potion: " + itemid);
        } else if (itemType > 70 && itemType <= 80) {//장비 10퍼센트
            while (!ii.itemExists(itemid)) {
                itemid = RandomRewards.getPQResultE();
            }
            //chr.dropMessage(6, "equip: " + itemid);
        } else if (itemType > 80 && itemType <= 90) {//줌서 10퍼센트
            while (!ii.itemExists(itemid)) {
                itemid = RandomRewards.getPQResultS();
            }
            //chr.dropMessage(6, "scroll: " + itemid);
        } else if (itemType > 91 && itemType <= 100) {//촉진제 10퍼센트
            while (!ii.itemExists(itemid)) {
                itemid = RandomRewards.getPQResultEtc();
            }
            //chr.dropMessage(6, "etc: " + itemid);
        }

        if (GameConstants.getInventoryType(itemid) == MapleInventoryType.EQUIP) {
            final Item nEquip = ii.getEquipById(itemid);
            if (chr.getMapId() == 910340500) {//커닝파퀘
                if (ii.getReqLevel(itemid) > 30) {//다시
                    while (ii.getReqLevel(itemid) > 30 || !ii.itemExists(itemid)) {
                        itemid = RandomRewards.getPQResultE();
                    }
                }
            } else if (chr.getMapId() == 922010900) {//루디파퀘
                if (ii.getReqLevel(itemid) > 40 || ii.getReqLevel(itemid) <= 30 || !ii.itemExists(itemid)) {//다시
                    while (ii.getReqLevel(itemid) > 40 || ii.getReqLevel(itemid) <= 30 || !ii.itemExists(itemid)) {
                        itemid = RandomRewards.getPQResultE();
                    }
                }
            } else if (chr.getMapId() == 930000600) {//독안개의숲
                if (ii.getReqLevel(itemid) > 50 || ii.getReqLevel(itemid) <= 40 || !ii.itemExists(itemid)) {//다시
                    while (ii.getReqLevel(itemid) > 50 || ii.getReqLevel(itemid) <= 40 || !ii.itemExists(itemid)) {
                        itemid = RandomRewards.getPQResultE();
                    }
                }
            } else if (chr.getMapId() == 240080800) {//천공의 둥지
                if (ii.getReqLevel(itemid) > 110 || ii.getReqLevel(itemid) <= 80 || !ii.itemExists(itemid)) {//다시
                    while (ii.getReqLevel(itemid) > 110 || ii.getReqLevel(itemid) <= 80 || !ii.itemExists(itemid)) {
                        itemid = RandomRewards.getPQResultE();
                    }
                }
            }
        }
        if (chr.getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() < 1
                || chr.getInventory(MapleInventoryType.USE).getNumFreeSlot() < 1
                || chr.getInventory(MapleInventoryType.SETUP).getNumFreeSlot() < 1
                || chr.getInventory(MapleInventoryType.ETC).getNumFreeSlot() < 1
                || chr.getInventory(MapleInventoryType.CASH).getNumFreeSlot() < 1) {
            c.getSession().write(MaplePacketCreator.recievePQrewardFail((byte) 2));
            return;
        } else if (c.getPlayer().getInventory(GameConstants.getInventoryType(itemid)).getNextFreeSlot() > -1) {
            if (Randomizer.nextInt(100) <= 50 && GameConstants.getInventoryType(itemid) == MapleInventoryType.EQUIP) {
                MapleInventoryManipulator.addByIdPotential(c, itemid, ((short) 1), null, null, "파티퀘스트", true);//잠재 on!
                c.getSession().write(MaplePacketCreator.recievePQrewardSuccess((byte) type, itemid, true));
            } else {
                MapleInventoryManipulator.addByIdPotential(c, itemid, (isPotion(itemid) ? (short) 10 : (short) 1), null, null, "파티퀘스트", false);
                c.getSession().write(MaplePacketCreator.recievePQrewardSuccess((byte) type, itemid, false));
            }
        }
    }

    private static final boolean getIncubatedItems(MapleClient c, int itemId, int type) {
        if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() < 1 || c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() < 3 || c.getPlayer().getInventory(MapleInventoryType.ETC).getNumFreeSlot() < 1) {
            c.getPlayer().dropMessage(5, "장비창 1개, 소비창 3개, 기타창 1개의 공간이 필요합니다.");
            return false;
        }

        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        String invoked = null;
        if (type == 0) { // 부화기
            try {
                invoked = (String) EtcScriptInvoker.getInvocable("etc/incubator.js").invokeFunction("run", (itemId % 10));

                String[] ids = invoked.split(",");
                if (ids.length < 4) {
                    c.getPlayer().dropMessage(1, "현재 부화기를 사용할 수 없는 기간입니다.");
                    return false;
                }
                int itemid = Integer.parseInt(ids[2]);
                //DBLogger.getInstance().logItem(LogType.Item.Incubator, c.getPlayer().getId(), c.getPlayer().getName(), Integer.parseInt(ids[2]), Integer.parseInt(ids[3]), ii.getName(Integer.parseInt(ids[2])), 0, "[피그미 알 : " + itemId + "]");

                c.getSession().write(MaplePacketCreator.getNPCTalk(9050008, (byte) 0, "피그미 에그 안에서 아이템이 나왔습니다.\r\n\r\n#fUI/UIWindow.img/QuestIcon/4/0#\r\n#i" + itemid + "# " + ii.getItemInformation(itemid).name + (isPotion(itemid) ? "100" : " 1개를 획득했습니다."), "00 00", (byte) 0));
                //World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, c.getPlayer().getName() + "님이 피그미 에그에서 " + "" + ii.getItemInformation(itemid).name + "" + "(을)를 얻었습니다.", itemid));
                MapleInventoryManipulator.addById(c, itemid, (short) Integer.parseInt(ids[3]), ii.getName(itemId) + " on " + FileoutputUtil.CurrentReadable_Date());
                return true;
            } catch (Exception e) {
                System.err.println("Error executing Etc script. Path: " + "etc/incubator.js" + "\nException " + e);
                FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Error executing Etc script. Path: " + "etc/incubator.js" + "\nException " + e);
                return false;
            }
        } else if (type == 1) { // 피넛머신
            try {
                invoked = (String) EtcScriptInvoker.getInvocable("etc/peanut.js").invokeFunction("run", (itemId % 10));

                String[] ids = invoked.split(",");
                if (ids.length < 4) {
                    c.getPlayer().dropMessage(1, "현재 피넛 머신을 사용할 수 없는 기간입니다.");
                    return false;
                }
                int itemid = Integer.parseInt(ids[2]);

                // 특정 아이템에 대해 알림을 보냄
                if (itemid == 1149018 || itemid == 1112663 || itemid == 5010836 || itemid == 1149016 || itemid == 1149017) {
                    World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, c.getPlayer().getName() + "님이 피넛 머신에서 " + "[" + ii.getItemInformation(itemid).name + "]" + "(을)를 얻었습니다.", itemid));
                }

                MapleInventoryManipulator.addById(c, 4310016, (short) 1, "Cube" + " on " + FileoutputUtil.CurrentReadable_Date()); // 증정품
                c.getSession().write(MaplePacketCreator.getNPCTalk(9010000, (byte) 0, "단단한 땅콩에서 아이템이 나왔습니다.\r\n\r\n#fUI/UIWindow.img/QuestIcon/4/0#\r\n#i" + itemid + "# " + ii.getItemInformation(itemid).name + (isPotion(itemid) ? "100" : "1개 \r\n\r\n#b#i4310016# #z4310016#를 획득했습니다."), "00 00", (byte) 0));
                MapleInventoryManipulator.addById(c, itemid, (short) Integer.parseInt(ids[3]), ii.getName(itemId) + " on " + FileoutputUtil.CurrentReadable_Date());
                return true;
            } catch (Exception e) {
                System.err.println("Error executing Etc script. Path: " + "etc/peanut.js" + "\nException " + e);
                FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Error executing Etc script. Path: " + "etc/incubator.js" + "\nException " + e);
                return false;
            }
        }
        return false;
    }

    private static final boolean isPotion(final int itemId) {
        if (itemId >= 2000000 && itemId <= 2020015) {
            return true;
        }
        return false;
    }

    public static final boolean autoLoot23(final MapleCharacter chr, MapleMapItem mapitem) {
        if (chr == null || chr.getMap() == null) {
            return false;
        }
        if (!chr.getAutoStatus()) {
            return false;
        }

        MapleClient c = chr.getClient();
        //  final MapleMapObject ob = chr.getMap().getMapObject(mapitem.getObjectId(), MapleMapObjectType.ITEM);
        if (mapitem.getDropper().getType() == MapleMapObjectType.MONSTER) {
            final Lock lock = mapitem.getLock();
            lock.lock();
            try {
                if (mapitem == null) {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return false;
                }
                if (mapitem.getOwner() != chr.getId()) {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return false;
                }
                if (mapitem.isPickedUp()) {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return false;
                }
                if (mapitem.getQuest() > 0 && chr.getQuestStatus(mapitem.getQuest()) != 1) {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return false;
                }
                if (mapitem.getOwner() != chr.getId() && ((!mapitem.isPlayerDrop() && mapitem.getDropType() == 0) || (mapitem.isPlayerDrop() && chr.getMap().getEverlast()))) {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return false;
                }
                if (!mapitem.isPlayerDrop() && mapitem.getDropType() == 1 && mapitem.getOwner() != chr.getId() && (chr.getParty() == null || chr.getParty().getMemberById(mapitem.getOwner()) == null)) {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return false;
                }
                if (mapitem.getMeso() <= 0) {
                    final boolean canShow;
                    Pair<Integer, Integer> questInfo = MapleItemInformationProvider.getInstance().getQuestItemInfo(mapitem.getItemId());
                    if (questInfo != null && questInfo.getLeft() == mapitem.getQuest()) {
                        canShow = !chr.haveItem(mapitem.getItemId(), questInfo.getRight(), true, true);
                    } else {
                        canShow = true;
                    }
                    //퀘스트 아이템은 필요 갯수를 초과하여 먹을 수 없음.
//                chr.dropMessage(6, MapleItemInformationProvider.getInstance().getName(mapitem.getItemId()) + " is can pickup ? " + canShow + " / questInfo : " + questInfo);
                    if (!canShow) {
                        c.getSession().write(MaplePacketCreator.getInventoryFull());
                        c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                        c.getSession().write(MaplePacketCreator.enableActions());
                        //c.getSession().writeAndFlush(MaplePacketCreator.enableActions(c.getPlayer()));
                        return false;
                    }
                }
                if (GameConstants.isPickupRestrictedMap(c.getPlayer().getMapId()) && mapitem.getOwner() != c.getPlayer().getId()) {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return false;
                }

                if (mapitem.getMeso() > 0) {
                    if (chr.getParty() != null) {
                        final List<MapleCharacter> toGive = new LinkedList<MapleCharacter>();
                        final int splitMeso = mapitem.getMeso() * 40 / 100;
                        int givenMeso = 0;
                        for (MaplePartyCharacter pChr : chr.getParty().getMembers()) {
                            MapleCharacter otherChar = chr.getMap().getCharacterById(pChr.getId());
                            if (otherChar != null && otherChar.getId() != chr.getId()) {
                                toGive.add(otherChar);
                            }
                        }
                        for (final MapleCharacter m : toGive) {
                            int meso = splitMeso / toGive.size();
                            m.gainMeso(meso, true);
                            givenMeso += meso;
                        }
                        if (chr.getArcaneMeso() > 0 || chr.getCollectionMeso() > 0) {
                            int simbolMeso = chr.getArcaneMeso();
                            int collection = chr.getCollectionMeso();
                            int totalMeso = mapitem.getMeso() - givenMeso;
                            chr.gainMeso(totalMeso + ((totalMeso * simbolMeso + collection) / 100), true);
                        } else {
                            chr.gainMeso(mapitem.getMeso() - givenMeso, true);
                        }
                        c.getSession().write(MaplePacketCreator.enableActions());
                    } else {
                        if (chr.getArcaneMeso() > 0 || chr.getCollectionMeso() > 0) {
                            int simbolMeso = chr.getArcaneMeso();
                            int collection = chr.getCollectionMeso();
                            int totalMeso = mapitem.getMeso();
                            if (chr.getParty() == null) {
                                if (chr.haveItem(9999999, 1)) {
                                    chr.gainMeso(totalMeso * (chr.getInventory(GameConstants.getInventoryType(9999999)).countById(9999999) > 100 ? 20 : (int) chr.getInventory(GameConstants.getInventoryType(9999999)).countById(9999999) / 5), false);
                                }
                            }
                            chr.gainMeso(totalMeso + ((totalMeso * simbolMeso + collection) / 100), true);
                        } else {
                            chr.gainMeso(mapitem.getMeso(), true);
                        }
                        c.getSession().write(MaplePacketCreator.enableActions());
                    }
                    removeItem_Pet(chr, mapitem, 0);
                } else {
                    if (MapleItemInformationProvider.getInstance().isPickupBlocked(mapitem.getItemId())) {
                        c.getSession().write(MaplePacketCreator.enableActions());
                        c.getPlayer().dropMessage(5, "이 아이템은 주울 수 없습니다.");
                    } else if (mapitem.getItemId() / 10000 == 291) {
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getCapturePosition(c.getPlayer().getMap()));
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.resetCapture());
                    } else if (mapitem.getItemId() / 10000 != 291 && MapleInventoryManipulator.checkSpace(c, mapitem.getItemId(), mapitem.getItem().getQuantity(), mapitem.getItem().getOwner())) {
                        if (mapitem.getItem().getQuantity() >= 50 && mapitem.getItemId() == 2340000) {
                            c.setMonitored(true); //hack check
                        }

                        if (mapitem.isPlayerDrop()) {
                            ServerLogger.getInstance().logTrade(LogType.Trade.DropAndPick, c.getPlayer().getId(), c.getPlayer().getName(), mapitem.getDropperName(), MapleItemInformationProvider.getInstance().getName(mapitem.getItem().getItemId()) + " " + mapitem.getItem().getQuantity() + "개", "맵 : " + c.getPlayer().getMapId());
                        }
                        //final MapleInventoryType type = GameConstants.getInventoryType(mapitem.getItem().getItemId());
                        //c.getSession().write(MaplePacketCreator.addInventorySlot(type, mapitem.getItem()));
                        MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true, mapitem.getDropper() instanceof MapleMonster);//
                        //removeItem(chr, mapitem, ob, false);
                    } else {
                        c.getSession().write(MaplePacketCreator.getInventoryFull());
                        c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                        c.getSession().write(MaplePacketCreator.enableActions());
                    }
                }
            } finally {
                lock.unlock();
            }
            return true;
        } else {
            return false;
        }
    }
}
