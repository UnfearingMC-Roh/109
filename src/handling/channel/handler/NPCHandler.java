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

import client.MapleAdminShop;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.MapleClient;
import client.MapleCharacter;
import constants.GameConstants;
import client.MapleQuestStatus;
import client.PlayerStats;
import client.RockPaperScissors;
import client.SkillFactory;
import client.inventory.ItemFlag;
import handling.SendPacketOpcode;
import handling.world.World;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import server.AutobanManager;
import server.MapleShop;
import server.MapleInventoryManipulator;
import server.MapleStorage;
import server.life.MapleNPC;
import server.quest.MapleQuest;
import scripting.NPCScriptManager;
import scripting.NPCConversationManager;
import scripting.vm.NPCScriptInvoker;
import scripting.vm.NPCScriptVirtualMachine;
import server.MapleAdminShopItem;
import server.MapleItemInformationProvider;
import server.maps.MapScriptMethods;
import server.maps.MapleMap;
import server.movement.LifeMovementFragment;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.data.LittleEndianAccessor;
import tools.data.MaplePacketLittleEndianWriter;
import tools.packet.PacketHelper;

public class NPCHandler {
    
    private static PlayerStats stats;    

    public static final void NPCAnimation(final LittleEndianAccessor slea, final MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_ACTION.getValue());
        
        int objectID = slea.readInt();
        byte oneTimeAction = slea.readByte();
        byte chatIndex = slea.readByte();
        
        MapleCharacter user = null;
//        MapleMap map = null;
//        MapleNPC npc = null;
        
        if (c == null
                || (user = c.getPlayer()) == null) {
//                || (map = user.getMap()) == null) {
//                || (npc = map.getNPCByOid(objectID)) == null) {
//            System.err.println("[ERROR] Npc Action Reading Client(" + c + ")");
//            System.err.println("[ERROR] Npc Action Reading User(" + user + ") | Map(" + map + ") | Npc(" + npc + ") Null Pointer");
            return;
        }
        mplew.writeInt(objectID);
        mplew.write(oneTimeAction);
        mplew.write(chatIndex);
        
        long len = slea.available(); //남은 패킷
//        if (npc.isMove()) {
        if (len > 0) {
            mplew.writePos(slea.readPos()); //Decode2 Decode2
            slea.readInt();
            mplew.writeInt(0); //Decode2 Decode2
            List<LifeMovementFragment> res = MovementParse.parseMovement(slea, 6);
            PacketHelper.serializeMovementList(mplew, res);
        }
        c.sendPacket(mplew.getPacket());
    }

    public static final void NPCShop(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final byte bmode = slea.readByte();
        if (chr == null) {
            return;
        }

        switch (bmode) {
            case 0: {
                final MapleShop shop = chr.getShop();
                if (shop == null) {
                    return;
                }
                final short select = slea.readShort();
                final int itemId = slea.readInt();
                final short quantity = slea.readShort();
                shop.buy(c, itemId, quantity, select);
                break;
            }
            case 1: {
                final MapleShop shop = chr.getShop();
                if (shop == null) {
                    return;
                }
                final byte slot = (byte) slea.readShort();
                final int itemId = slea.readInt();
                final short quantity = slea.readShort();
                shop.sell(c, GameConstants.getInventoryType(itemId), slot, quantity);
                
                break;
            }
            case 2: {
                final MapleShop shop = chr.getShop();
                if (shop == null) {
                    return;
                }
                final byte slot = (byte) slea.readShort();
                shop.recharge(c, slot);
                break;
            }
            default:
                chr.setConversation(0);
                break;
        }
    }

    public static final void AdminShop(final LittleEndianAccessor slea, final MapleClient c) {
        if (slea.available() == 0 || c.getPlayer() == null) {
            return;
        }
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

        switch (slea.readByte()) {
            case 1: {//판매/매입
                if (c.getPlayer().getAdminShop() == null) {
                    return;
                }
                int id = slea.readInt();
                int quantity = slea.readShort();
                short type = slea.readShort();

                MapleAdminShop mas = c.getPlayer().getAdminShop();
                MapleAdminShopItem masi = mas.findById(id);

                if (masi != null) {
                    MapleInventoryType invtype = GameConstants.getInventoryType(masi.getItemId());
                    if (type == 0) {
                        if (c.getPlayer().getMeso() < masi.getPrice()) {
                            c.getSession().write(MaplePacketCreator.AdminShopMsg((byte) 4));
                            return;
                        } else if (masi.getCount() <= 0) {
                            c.getSession().write(MaplePacketCreator.AdminShopMsg((byte) 7));
                            return;
                        } else if (quantity > masi.getMaxQuantity() || masi.getCount() - quantity < 0 || c.getPlayer().getInventory(invtype).getNumFreeSlot() <= 0) {
                            c.getSession().write(MaplePacketCreator.AdminShopMsg((byte) 9));
                            return;
                        }

                        if (invtype == MapleInventoryType.EQUIP) {
                            final Equip item = (Equip) ii.getEquipById(masi.getItemId());
                            MapleInventoryManipulator.addbyItem(c, item);
                        } else {
                            MapleInventoryManipulator.addById(c, masi.getItemId(), (short) 1, "AdminShopitem: " + masi.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                        }
                        masi.gainCount(-quantity);
                        c.getPlayer().gainMeso(-masi.getPrice() * quantity, true);
                    } else if (type > 0) {
                        if (!c.getPlayer().haveItem(masi.getItemId(), quantity)) {
                            c.getSession().write(MaplePacketCreator.AdminShopMsg((byte) 9));
                            return;
                        } else if (masi.getCount() <= 0) {
                            c.getSession().write(MaplePacketCreator.AdminShopMsg((byte) 7));
                            return;
                        } else if (masi.getCount() - quantity < 0) {
                            c.getSession().write(MaplePacketCreator.AdminShopMsg((byte) 9));
                            return;
                        } else if ((long) (c.getPlayer().getMeso() + (long) (masi.getPrice() * quantity)) >= Integer.MAX_VALUE) {
                            c.getSession().write(MaplePacketCreator.AdminShopMsg((byte) 5));
                            return;
                        }

                        MapleInventoryManipulator.removeById(c, invtype, masi.getItemId(), (short) quantity, true, false);
                        masi.gainCount(-quantity);
                        c.getPlayer().gainMeso(masi.getPrice() * quantity, true);
                    }
                    c.getSession().write(MaplePacketCreator.AdminShopDlg(9000510, mas));
                } else {
                    c.getSession().write(MaplePacketCreator.AdminShopMsg((byte) 1));
                }
            }
            break;
            case 2: {//종료
                final NPCConversationManager cm = NPCScriptManager.getInstance().getCM(c);
                if (c.getPlayer().getAdminShop() == null) {
                    return;
                }
                c.getPlayer().setAdminShop(null);
                cm.dispose();
            }
            break;
            case 3: {//물품등록
                int itemid = slea.readInt();
                if (ii.itemExists(itemid)) {
                    World.AdminShopItemRequest.gainItem(itemid, c);
                }
            }
            break;
        }
    }

    public static final void NPCTalk(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return;
        }
        final MapleNPC npc = chr.getMap().getNPCByOid(slea.readInt());

        if (npc == null) {
            return;
        }
        if (c.getPlayer().isGM()) {
            chr.dropMessage(5, npc.getName() +  ": " + npc.getId() + "");
        }
        if (chr.hasBlockedInventory()) {
//            chr.dropMessage(-1, "You already are talking to an NPC. Use @ea if this is not intended.");
            return;
        }
        //c.getPlayer().updateTick(slea.readInt());
        if (npc.hasShop()) {
            chr.setConversation(1);
            npc.sendShop(c);
        } else {
            if (NPCScriptInvoker.runNpc(c, npc.getId(), npc.getObjectId()) != 0) {
                NPCScriptManager.getInstance().start(c, npc.getId(), null, npc.getObjectId());
            }
        }
    }

    public static final void QuestAction(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final byte action = slea.readByte();
        int quest = slea.readUShort();
        if (chr == null) {
            return;
        }
        final MapleQuest q = MapleQuest.getInstance(quest);
        if (chr.isGM() && q.getId() / 100 != 299) {
            chr.dropMessage(5, "퀘스트 : " + q.getName() + " / 코드 : " + q.getId() + " / action : " + action);
        }
        switch (action) {
            case 0: { // Restore lost item
                //chr.updateTick(slea.readInt());
                slea.readInt();
                final int itemid = slea.readInt();
                q.RestoreLostItem(chr, itemid);
                break;
            }
            case 1: { // Start Quest
                final int npc = slea.readInt();
                if (!q.hasStartScript()) {
                    q.start(chr, npc);
                }
                break;
            }
            case 2: { // Complete Quest
                /*if (chr.getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() <= 2
                 || chr.getInventory(MapleInventoryType.USE).getNumFreeSlot() <= 2
                 || chr.getInventory(MapleInventoryType.SETUP).getNumFreeSlot() <= 2
                 || chr.getInventory(MapleInventoryType.ETC).getNumFreeSlot() <= 2
                 || chr.getInventory(MapleInventoryType.CASH).getNumFreeSlot() <= 2) {
                 chr.dropMessage(1, "버그 악용 방지를 위해 모든 인벤토리칸을 각각 3칸이상 비워 주세요.");
                 return;
                 }*/
                final int npc = slea.readInt();
                slea.readInt();
                if (q.hasEndScript()) {
                    return;
                }
                if (slea.available() >= 4) {
                    q.complete(chr, npc, slea.readInt());
                } else {
                    q.complete(chr, npc);
                }
                // c.getSession().write(MaplePacketCreator.completeQuest(c.getPlayer(), quest));
                //c.getSession().write(MaplePacketCreator.updateQuestInfo(c.getPlayer(), quest, npc, (byte)14));
                // 6 = start quest
                // 7 = unknown error
                // 8 = equip is full
                // 9 = not enough mesos
                // 11 = due to the equipment currently being worn wtf o.o
                // 12 = you may not posess more than one of this item
                break;
            }
            case 3: { // Forefit Quest
                if (GameConstants.canForfeit(q.getId())) {
                    q.forfeit(chr);
                } else {
                    chr.dropMessage(1, "포기하실 수 없는 퀘스트 입니다.");
                }
                break;
            }

            case 4: { // Scripted Start Quest
                final int npc = slea.readInt();
                if (chr.hasBlockedInventory()) {
//                    chr.dropMessage(-1, "You already are talking to an NPC. Use @ea if this is not intended.");
                    return;
                }
//                c.getPlayer().updateTick(slea.readInt());
                NPCScriptManager.getInstance().startQuest(c, npc, quest);
                break;
            }
            case 5: { // Scripted End Quest
                final int npc = slea.readInt();
                if (chr.hasBlockedInventory()) {
//                    chr.dropMessage(-1, "You already are talking to an NPC. Use @ea if this is not intended.");
                    return;
                }
//                c.getPlayer().updateTick(slea.readInt());
                NPCScriptManager.getInstance().endQuest(c, npc, quest, false);
                break;
            }
        }
    }

    public static final void Storage(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final byte mode = slea.readByte();
        if (chr == null) {
            return;
        }
        final MapleStorage storage = chr.getStorage();
        if (c.getPlayer().cubeitemid > 0) {
            c.getPlayer().cubeitemid = 0;
        }
        switch (mode) {
            case 4: { // Take Out
                final byte type = slea.readByte();
                final byte slot = storage.getSlot(MapleInventoryType.getByType(type), slea.readByte());
                final Item item = storage.takeOut(slot);

                if (item != null) {
                    if (!MapleInventoryManipulator.checkSpace(c, item.getItemId(), item.getQuantity(), item.getOwner())) {
                        storage.store(item);
                        chr.dropMessage(1, "인벤토리가 가득 찼습니다.");
                    } else {
                        MapleInventoryManipulator.addFromDrop(c, item, false);
                    }
                    storage.sendTakenOut(c, GameConstants.getInventoryType(item.getItemId()));
                } else {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
                break;
            }
            case 5: { // Store
                final byte slot = (byte) slea.readShort();
                final int itemId = slea.readInt();
                MapleInventoryType type = GameConstants.getInventoryType(itemId);
                short quantity = slea.readShort();
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                if (quantity < 1) {
                    //AutobanManager.getInstance().autoban(c, "Trying to store " + quantity + " of " + itemId);
                    return;
                }
                if (storage.isFull()) {
                    c.getSession().write(MaplePacketCreator.getStorageFull());
                    return;
                }
                if (chr.getInventory(type).getItem(slot) == null) {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }

                if (chr.getMeso() < 100) {
                    chr.dropMessage(1, "메소가 부족합니다.");
                } else {
                    Item item = chr.getInventory(type).getItem(slot).copy();

                    if (GameConstants.isPet(item.getItemId())) {
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    }
                    final short flag = item.getFlag();
                    if (ii.isPickupRestricted(item.getItemId()) && storage.findById(item.getItemId()) != null) {
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    }
                    if (item.getItemId() == itemId && (item.getQuantity() >= quantity || GameConstants.isThrowingStar(itemId) || GameConstants.isBullet(itemId))) {
                        if (ii.isDropRestricted(item.getItemId())) {
                            if (ItemFlag.KARMA_EQ.check(flag)) {
                                item.setFlag((short) (flag - ItemFlag.KARMA_EQ.getValue()));
                            } else if (ItemFlag.KARMA_USE.check(flag)) {
                                item.setFlag((short) (flag - ItemFlag.KARMA_USE.getValue()));
                            } else if (ItemFlag.KARMA_ACC.check(flag)) {
                                item.setFlag((short) (flag - ItemFlag.KARMA_ACC.getValue()));
                            } else if (ItemFlag.KARMA_ACC_USE.check(flag)) {
                                item.setFlag((short) (flag - ItemFlag.KARMA_ACC_USE.getValue()));
                            } else {
                                c.getSession().write(MaplePacketCreator.enableActions());
                                return;
                            }
                        }
                        if (GameConstants.isThrowingStar(itemId) || GameConstants.isBullet(itemId)) {
                            quantity = item.getQuantity();
                        }
                        chr.gainMeso(-100, false, false);
                        MapleInventoryManipulator.removeFromSlot(c, type, slot, quantity, false);
                        item.setQuantity(quantity);
                        storage.store(item);
                    } else {
                        AutobanManager.getInstance().addPoints(c, 1000, 0, "Trying to store non-matching itemid (" + itemId + "/" + item.getItemId() + ") or quantity not in posession (" + quantity + "/" + item.getQuantity() + ")");
                        return;
                    }
                }
                storage.sendStored(c, GameConstants.getInventoryType(itemId));
                break;
            }
            case 6: { //arrange
                storage.arrange();
                storage.update(c);
                break;
            }
            case 7: {
                int meso = slea.readInt();
                final int storageMesos = storage.getMeso();
                final int playerMesos = chr.getMeso();

                if ((meso > 0 && storageMesos >= meso) || (meso < 0 && playerMesos >= -meso)) {
                    if (meso < 0 && (storageMesos - meso) < 0) { // storing with overflow
                        meso = -(Integer.MAX_VALUE - storageMesos);
                        if ((-meso) > playerMesos) { // should never happen just a failsafe
                            return;
                        }
                    } else if (meso > 0 && (playerMesos + meso) < 0) { // taking out with overflow
                        meso = (Integer.MAX_VALUE - playerMesos);
                        if ((meso) > storageMesos) { // should never happen just a failsafe
                            return;
                        }
                    }
                    storage.setMeso(storageMesos - meso);
                    chr.gainMeso(meso, false, false);
                } else {
                    AutobanManager.getInstance().addPoints(c, 1000, 0, "Trying to store or take out unavailable amount of mesos (" + meso + "/" + storage.getMeso() + "/" + c.getPlayer().getMeso() + ")");
                    return;
                }
                storage.sendMeso(c);
                break;
            }
            case 8: {
                storage.close();
                chr.setConversation(0);
                break;
            }
            default:
                System.out.println("Unhandled Storage mode : " + mode);
                break;
        }
    }

    public static final void NPCMoreTalk(final LittleEndianAccessor slea, final MapleClient c) {
        byte lastMsg = slea.readByte(); // 00 (last msg type I think)
        final byte action = slea.readByte(); // 00 = end chat, 01 == follow

        if (lastMsg == 2) {
            lastMsg = 1;
        }
        
        /*if (lastMsg == 2 || lastMsg == 12) { // 화스 솟 때문에 강제 조정 : YesNo AcceptDecline //1231
            lastMsg -= 1;
        }*/
        
        //todo legend
        if (((lastMsg == 0x12 && c.getPlayer().getDirection() >= 0) || (lastMsg == 0x13 && c.getPlayer().getDirection() == -1)) && action == 1 && GameConstants.GMS) {
            MapScriptMethods.startDirectionInfo(c.getPlayer(), lastMsg == 0x13);
            return;
        }
        
        
        
        if (NPCScriptInvoker.isVmConversation(c)) {//1231
            NPCScriptVirtualMachine vm = NPCScriptInvoker.getVM(c);
            if (vm != null) {
                String str = "";
                int type = vm.getLastMsg();
                int selection = -1;
                if (type == 2) {
                    if (action != 0) {
                        str = slea.readMapleAsciiString();
                    }
                } else if (slea.available() >= 4) {
                    selection = slea.readInt();
                } else if (slea.available() > 0) {
                    selection = slea.readByte() & 0xFF;
                }

                NPCScriptInvoker.actionNpc(c, action, type, selection, str);
                return;
            }
        }

        final NPCConversationManager cm = NPCScriptManager.getInstance().getCM(c);

        if (cm == null || c.getPlayer().getConversation() == 0 || cm.getLastMsg() != lastMsg) {
            if (cm != null && cm.getLastMsg() != lastMsg) {
                if (c.getPlayer().isGM()) {
                    c.getPlayer().dropMessage(5, "기본 : " + cm.getLastMsg() + " / 패킷 :  " + lastMsg + " -> 수정바람");
                }
            }
            return;
        }
        cm.setLastMsg((byte) -1);
        if (lastMsg == 3) {
            if (action != 0) {
                int number = 0;
                String text = slea.readMapleAsciiString();
                boolean a = true;
                try { 
                  number = Integer.parseInt(text);
                } catch(NumberFormatException nfe){ 
                  a = false;
                } 
                if (a) {
                    if (number < 0 || number > 30000) { // || c.getPlayer().min > number || c.getPlayer().max < number
                        cm.dispose();
                        c.getPlayer().dropMessage(1, lastMsg + " 타입의 오류가 발생하였습니다. 1");
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    }
                }
                cm.setGetText(text);
                if (cm.getType() == 0) {
                    NPCScriptManager.getInstance().startQuest(c, action, lastMsg, -1);
                } else if (cm.getType() == 1) {
                    NPCScriptManager.getInstance().endQuest(c, action, lastMsg, -1);
                } else {
                    NPCScriptManager.getInstance().action(c, action, lastMsg, -1);
                }
            } else {
                cm.dispose();
            }
        } else {
            int selection = 0;
            if (lastMsg != 4) {
                selection = -1;
            }
            if (slea.available() >= 4) {
                selection = slea.readInt();
                if (c.getPlayer().min > 0 && c.getPlayer().max > 0) {
                    if (c.getPlayer().min > selection + 1 || c.getPlayer().max < selection) {
                        cm.dispose();
                        c.getPlayer().dropMessage(1, lastMsg + " 타입의 오류가 발생하였습니다. 2");
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    }
                }
                if (selection < 0 || selection > 2147483645) {
                    cm.dispose();
                    c.getPlayer().dropMessage(1, lastMsg + " 타입의 오류가 발생하였습니다. 3");
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
                c.getPlayer().min = 0;
                c.getPlayer().max = 0;
            } else if (slea.available() > 0) {
                selection = slea.readByte();
                if (selection < 0 || selection > 2147483645) {
                    cm.dispose();
                    c.getPlayer().dropMessage(1, "오류가 발생하였습니다. : -3");
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
            }
            if (lastMsg == 4 && selection == -1) {
                cm.dispose();
                return;//h4x
            }
            if (selection >= -1 && action != -1) {
                if (cm.getType() == 0) {
                    NPCScriptManager.getInstance().startQuest(c, action, lastMsg, selection);
                } else if (cm.getType() == 1) {
                    NPCScriptManager.getInstance().endQuest(c, action, lastMsg, selection);
                } else {
                    NPCScriptManager.getInstance().action(c, action, lastMsg, selection);
                }
            } else {
                cm.dispose();
            }
        }
    }

    public static final void repairAll(final MapleClient c) {
        Equip eq;
        double price = 0;
        Map<String, Integer> eqStats;
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final Map<Equip, Integer> eqs = new HashMap<Equip, Integer>();
        final MapleInventoryType[] types = {MapleInventoryType.EQUIP, MapleInventoryType.EQUIPPED};
        for (MapleInventoryType type : types) {
            for (Item item : c.getPlayer().getInventory(type).newList()) {
                if (item instanceof Equip) { //redundant
                    eq = (Equip) item;
                    if (eq.getDurability() >= 0) {
                        eqStats = ii.getEquipStats(eq.getItemId());
                        final double rPercentage = Math.ceil((100.0 - (eq.getDurability() * 100.0) / (eqStats.get("durability"))));
                        eqs.put(eq, eqStats.get("durability"));
                        price += ii.getWholePrice(eq.getItemId()) * 0.02 * (ii.getReqLevel(eq.getItemId()) * ii.getReqLevel(eq.getItemId())) / (eqStats.get("durability") * 0.01) * rPercentage;//* (ii.getWholePrice(eq.getItemId()) * 0.25 + 1)
                    }
                }
            }
        }
        if (c.getPlayer().getMeso() < price) {
            c.getPlayer().dropMessage(1, "수리비가 부족합니다.");
            return;
        }
        if (eqs.size() <= 0) {
            c.getPlayer().dropMessage(1, "수리 할 아이템이 없습니다.");
            return;
        }
        c.getPlayer().gainMeso(-(int) price, true, true, false);
        Equip ez;
        for (Entry<Equip, Integer> eqqz : eqs.entrySet()) {
            ez = eqqz.getKey();
            ez.setDurability(eqqz.getValue());
            c.getPlayer().forceReAddItem(ez.copy(), ez.getPosition() < 0 ? MapleInventoryType.EQUIPPED : MapleInventoryType.EQUIP);
            stats.durabilityHandling.add((Equip) ez.copy());
        }
        c.getPlayer().dropMessage(1, "모든 아이템의 수리가 완료되었습니다.");
    }

    public static final void repair(final LittleEndianAccessor slea, final MapleClient c) {
        /*if (c.getPlayer().getMapId() != 240000000 || slea.available() < 4) { //leafre for now
         return;
         }*/
        final int position = slea.readInt(); //who knows why this is a int
        final MapleInventoryType type = position < 0 ? MapleInventoryType.EQUIPPED : MapleInventoryType.EQUIP;
        final Item item = c.getPlayer().getInventory(type).getItem((byte) position);
        if (item == null) {
            return;
        }
        final Equip eq = (Equip) item; //이 부분 사용해보자
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final Map<String, Integer> eqStats = ii.getEquipStats(item.getItemId());
        if (eq.getDurability() < 0 || !eqStats.containsKey("durability") || eqStats.get("durability") <= 0 || eq.getDurability() >= eqStats.get("durability")) {
            return;
        }
        final double rPercentage = Math.ceil((100.0 - (eq.getDurability() * 100.0) / (eqStats.get("durability"))));
        double price = 0.00;
        price = ii.getWholePrice(eq.getItemId()) * 0.02 * (ii.getReqLevel(eq.getItemId()) * ii.getReqLevel(eq.getItemId())) / (eqStats.get("durability") * 0.01) * rPercentage;//* (ii.getWholePrice(eq.getItemId()) * 0.25 + 1)
        //c.getPlayer().dropMessage(5, "rPercentage: " + rPercentage + " price: " + (long) price);
        if (c.getPlayer().getMeso() < price) {
            c.getPlayer().dropMessage(1, "수리비가 부족합니다.");
            return;
        }
        c.getPlayer().gainMeso(-(int) price, true, true, false);
        eq.setDurability(eqStats.get("durability"));
        c.getPlayer().forceReAddItem(eq.copy(), type);
        stats.durabilityHandling.add((Equip) eq.copy());
        c.getPlayer().dropMessage(1, "수리가 완료되었습니다.");
    }

    public static final void UpdateQuest(final LittleEndianAccessor slea, final MapleClient c) {
        int qid = slea.readUShort();
        MapleQuest q = MapleQuest.getInstance(qid);
        if (q != null && c.getPlayer().getQuestNoAdd(q) != null) {
            MapleQuestStatus qs = c.getPlayer().getQuestNoAdd(q);
            if (qs.getCustomData() == null || qs.getCustomData().isEmpty()) {
                return;
            }
            c.getSession().write(MaplePacketCreator.updateQuest(qs));
        }
    }

    public static final void UseItemQuest(final LittleEndianAccessor slea, final MapleClient c) {
        short slot = slea.readShort();
        int itemid = slea.readInt();

        Item item = c.getPlayer().getInventory(MapleInventoryType.ETC).getItem(slot);
        if (item == null || item.getItemId() != itemid || item.getQuantity() <= 0) {
            return;
        }

        int qid = slea.readUShort();

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
                    c.getSession().write(MaplePacketCreator.getShowQuestCompletion(3250));
                }
            }
            c.getSession().write(MaplePacketCreator.updateQuest(qs));
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static final void RPSGame(final LittleEndianAccessor slea, final MapleClient c) {
        if (slea.available() == 0 || c.getPlayer() == null || c.getPlayer().getMap() == null || !c.getPlayer().getMap().containsNPC(9000510)) {
            if (c.getPlayer() != null && c.getPlayer().getRPS() != null) {
                c.getPlayer().getRPS().dispose(c);
            }
            return;
        }
        final byte mode = slea.readByte();
        switch (mode) {
            case 0: //start game
            case 5: //retry
                if (c.getPlayer().getRPS() != null) {
                    c.getPlayer().getRPS().reward(c);
                }
                if (c.getPlayer().getMeso() >= 1000) {
                    c.getPlayer().setRPS(new RockPaperScissors(c, mode));
                } else {
                    c.getSession().write(MaplePacketCreator.getRPSMode((byte) 0x08, -1, -1, -1));
                }
                break;
            case 1: //answer
                if (c.getPlayer().getRPS() == null || !c.getPlayer().getRPS().answer(c, slea.readByte())) {
                    c.getSession().write(MaplePacketCreator.getRPSMode((byte) 0x0D, -1, -1, -1));
                }
                break;
            case 2: //time over
                if (c.getPlayer().getRPS() == null || !c.getPlayer().getRPS().timeOut(c)) {
                    c.getSession().write(MaplePacketCreator.getRPSMode((byte) 0x0D, -1, -1, -1));
                }
                break;
            case 3: //continue
                if (c.getPlayer().getRPS() == null || !c.getPlayer().getRPS().nextRound(c)) {
                    c.getSession().write(MaplePacketCreator.getRPSMode((byte) 0x0D, -1, -1, -1));
                }
                break;
            case 4: //leave
                if (c.getPlayer().getRPS() != null) {
                    c.getPlayer().getRPS().dispose(c);
                } else {
                    c.getSession().write(MaplePacketCreator.getRPSMode((byte) 0x0D, -1, -1, -1));
                }
                break;
        }

    }

    public static final void OpenPublicNpc(final LittleEndianAccessor slea, final MapleClient c) {
        final int npcid = slea.readInt();
        for (int i = 0; i < GameConstants.publicNpcIds.length; i++) {
            if (GameConstants.publicNpcIds[i] == npcid) { //for now
                NPCScriptManager.getInstance().start(c, npcid);
                return;
            }
        }
    }
}
