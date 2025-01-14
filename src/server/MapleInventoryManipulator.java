package server;

import java.awt.Point;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import client.inventory.MapleInventoryIdentifier;
import constants.GameConstants;
import client.inventory.Equip;
import client.inventory.InventoryException;
import client.inventory.Item;
import client.inventory.ItemFlag;
import client.PlayerStats;
import client.MapleBuffStat;
import client.inventory.MaplePet;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleQuestStatus;
import client.SkillFactory;
import client.inventory.MapleInventoryType;
import server.maps.AramiaFireWorks;
import tools.packet.MTSCSPacket;
import tools.MaplePacketCreator;
import tools.StringUtil;
import client.inventory.EquipAdditions.RingSet;
import client.inventory.MapleAndroid;
import handling.channel.handler.PlayerHandler;
import server.log.LogType;
import server.log.ServerLogger;
import server.quest.MapleQuest;
import tools.Pair;
import util.FileTime;

public class MapleInventoryManipulator {

    public static void addRing(MapleCharacter chr, int itemId, int ringId, int sn) {
        CashItemInfo csi = CashItemFactory.getInstance().getItem(sn);
        if (csi == null) {
            return;
        }
        Item ring = chr.getCashInventory().toItem(csi, ringId);
        if (ring == null || ring.getUniqueId() != ringId || ring.getUniqueId() <= 0 || ring.getItemId() != itemId) {
            return;
        }
        chr.getCashInventory().addToInventory(ring);
        //chr.getClient().getSession().write(MTSCSPacket.confirmToCSInventory(ring, chr.getClient().getAccID(), csi.getSN()));
        chr.getClient().getSession().write(MTSCSPacket.showBoughtCSItem(ring, sn, chr.getClient().getAccID()));
    }

    public static boolean addbyItem(final MapleClient c, final Item item) {
        return addbyItem(c, item, false) >= 0;
    }

    public static short addbyItem(final MapleClient c, final Item item, final boolean fromcs) {
        final MapleInventoryType type = GameConstants.getInventoryType(item.getItemId());
        final short newSlot = c.getPlayer().getInventory(type).addItem(item);
        if (newSlot == -1) {
            if (!fromcs) {
                c.getSession().write(MaplePacketCreator.getInventoryFull());
                c.getSession().write(MaplePacketCreator.getShowInventoryFull());
            }
            return newSlot;
        }
        if (GameConstants.isHarvesting(item.getItemId())) {
            c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
        }
        c.getSession().write(MaplePacketCreator.addInventorySlot(type, item));
        c.getPlayer().havePartyQuest(item.getItemId());
        if (GameConstants.isStatItem(item.getItemId())) {
//            c.getPlayer().CustomStatEffect(false);
            c.getPlayer().customizeStat(0);
        }
        return newSlot;
    }
    
    public static int getUniqueId(int itemId, MaplePet pet) {
        return getUniqueId(itemId, pet, false);
    }

    public static int getUniqueId(int itemId, MaplePet pet, boolean forceItem) {
        int uniqueid = -1;
        if (GameConstants.isPet(itemId)) {
            if (pet != null) {
                uniqueid = pet.getUniqueId();
            } else {
                uniqueid = MapleInventoryIdentifier.getInstance();
            }
        } else if (!MapleItemInformationProvider.getInstance().isQuestItem(itemId) && (GameConstants.getInventoryType(itemId) == MapleInventoryType.CASH || MapleItemInformationProvider.getInstance().isCash(itemId))) { //less work to do
            uniqueid = MapleInventoryIdentifier.getInstance(); //shouldnt be generated yet, so put it here
        }
        return uniqueid;
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, String gmLog, Long expired) {
        return addById(c, itemId, quantity, null, null, expired, gmLog);
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, String gmLog) {
        return addById(c, itemId, quantity, null, null, 0, gmLog);
    }

    public static byte addId(MapleClient c, int itemId, short quantity, String owner, String gmLog) {
        return addId(c, itemId, quantity, owner, null, 0, gmLog, false);
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, String owner, MaplePet pet, String gmLog) {
        return addById(c, itemId, quantity, owner, pet, 0, gmLog);
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, String owner, MaplePet pet, long period, String gmLog) {
        return addId(c, itemId, quantity, owner, pet, period, gmLog, false) >= 0;
    }

    public static boolean addByIdPotential(MapleClient c, int itemId, short quantity, String owner, MaplePet pet, String gmLog, boolean potential) {
        return addByIdPotential(c, itemId, quantity, owner, pet, 0, gmLog, potential);
    }

    public static boolean addByIdPotential(MapleClient c, int itemId, short quantity, String owner, MaplePet pet, long period, String gmLog, boolean potential) {
        return addId(c, itemId, quantity, owner, pet, period, gmLog, potential) >= 0;
    }

    public static byte addId(MapleClient c, int itemId, short quantity, String owner, MaplePet pet, long period, String gmLog, boolean potential) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if ((ii.isPickupRestricted(itemId) && c.getPlayer().haveItem(itemId, 1, true, false)) || (!ii.itemExists(itemId))) {
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            c.getSession().write(MaplePacketCreator.showItemUnavailable());
            return -1;
        }
        final MapleInventoryType type = GameConstants.getInventoryType(itemId);
        int uniqueid = getUniqueId(itemId, pet);
        short newSlot = -1;
        if (!type.equals(MapleInventoryType.EQUIP)) {
            final boolean canShow;
            Pair<Integer, Integer> questInfo = MapleItemInformationProvider.getInstance().getQuestItemInfo(itemId);
            if (questInfo != null && itemId / 10000 == 403) {
                canShow = !c.getPlayer().haveItem(itemId, questInfo.getRight(), true, true);
            } else {
                canShow = true;
            }
            if (!canShow) {
                c.getSession().write(MaplePacketCreator.getInventoryFull());
                return -1;
            }

            final short slotMax = ii.getSlotMax(itemId);
            final List<Item> existing = c.getPlayer().getInventory(type).listById(itemId);
            if (!GameConstants.isRechargable(itemId)) {
                if (existing.size() > 0) { // first update all existing slots to slotMax
                    Iterator<Item> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            Item eItem = (Item) i.next();
                            short oldQ = eItem.getQuantity();
                            if (oldQ < slotMax && (eItem.getOwner().equals(owner) || owner == null) && eItem.getExpiration() == -1) {
                                short newQ = (short) Math.min(oldQ + quantity, slotMax);
                                quantity -= (newQ - oldQ);
                                eItem.setQuantity(newQ);
                                c.getSession().write(MaplePacketCreator.updateInventorySlot(type, eItem, false));
                            }
                        } else {
                            break;
                        }
                    }
                }

                Item nItem;
                // add new slots if there is still something left
                while (quantity > 0) {
                    short newQ = (short) Math.min(quantity, slotMax);
                    if (newQ != 0) {
                        quantity -= newQ;
                        nItem = new Item(itemId, (byte) 0, newQ, (byte) 0, uniqueid);
                        newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                        if (newSlot == -1) {
                            c.getSession().write(MaplePacketCreator.getInventoryFull());
                            c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                            return -1;
                        }
                        if (gmLog != null) {
                            nItem.setGMLog(gmLog);
                        }
                        if (owner != null) {
                            nItem.setOwner(owner);
                        }
                        if (period > 0) {
                            nItem.setExpiration(System.currentTimeMillis() + (period * 24 * 60 * 60 * 1000));
                        }
                        if (pet != null) {
                            nItem.setPet(pet);
                            pet.setInventoryPosition(newSlot);
//                            c.getPlayer().addPetz(pet);
                        }
                        c.getSession().write(MaplePacketCreator.addInventorySlot(type, nItem));
                        if (GameConstants.isRechargable(itemId) && quantity == 0) {
                            break;
                        }
                    } else {
                        c.getPlayer().havePartyQuest(itemId);
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return (byte) newSlot;
                    }
                }
            } else {
                // Throwing Stars and Bullets - Add all into one slot regardless of quantity.
                final Item nItem = new Item(itemId, (byte) 0, quantity, (byte) 0, uniqueid); //1.2.6 유니크아이디 삭제
                newSlot = c.getPlayer().getInventory(type).addItem(nItem);

                if (newSlot == -1) {
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                    return -1;
                }
                if (period > 0) {
                    nItem.setExpiration(System.currentTimeMillis() + (period * 24 * 60 * 60 * 1000));
                }
                if (gmLog != null) {
                    nItem.setGMLog(gmLog);
                }
                c.getSession().write(MaplePacketCreator.addInventorySlot(type, nItem));
                c.getSession().write(MaplePacketCreator.enableActions());
            }
        } else {
            if (quantity == 1) {
                final Item nEquip = ii.getEquipById(itemId, uniqueid); //1.2.6 유니크아이디 삭제
                if (owner != null) {
                    nEquip.setOwner(owner);
                }
                if (gmLog != null) {
                    nEquip.setGMLog(gmLog);
                }
                if (period > 0) {
                    nEquip.setExpiration(System.currentTimeMillis() + (period * 24 * 60 * 60 * 1000));
                }
                Item item = c.getPlayer().getInventory(type).findById(itemId);
                if (potential) {
                    item = checkEnhanced2(nEquip, c.getPlayer());//확률없이
                }
                newSlot = c.getPlayer().getInventory(type).addItem(nEquip);
                if (newSlot == -1) {
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                    return -1;
                }
                c.getSession().write(MaplePacketCreator.addInventorySlot(type, nEquip));
            } else {
                throw new InventoryException("Trying to create equip with non-one quantity");
            }
        }
        c.getPlayer().havePartyQuest(itemId);
        return (byte) newSlot;
    }
    
    private static final Item checkEnhanced(final Item before, final MapleCharacter chr) {
        if (before instanceof Equip) {
            final Equip eq = (Equip) before;
            if (eq.getState() == 0 && (eq.getUpgradeSlots() >= 1 || eq.getLevel() >= 1) && GameConstants.canScroll(eq.getItemId()) && Randomizer.nextInt(100) >= 90) { //10% 확률로 잠재부여
                eq.resetPotential();
            }
        }
        return before;
    }

    private static final Item checkEnhanced2(final Item before, final MapleCharacter chr) {
        if (before instanceof Equip) {
            final Equip eq = (Equip) before;
            if (eq.getState() == 0 && (eq.getUpgradeSlots() >= 1 || eq.getLevel() >= 1) && GameConstants.canScroll(eq.getItemId())) { //100% 확률로 잠재부여
                eq.resetPotential();
            }
        }
        return before;
    }    

    public static Item addbyId_Gachapon(final MapleClient c, final int itemId, short quantity) {
        if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNextFreeSlot() == -1 || c.getPlayer().getInventory(MapleInventoryType.USE).getNextFreeSlot() == -1 || c.getPlayer().getInventory(MapleInventoryType.ETC).getNextFreeSlot() == -1 || c.getPlayer().getInventory(MapleInventoryType.SETUP).getNextFreeSlot() == -1) {
            return null;
        }
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if ((ii.isPickupRestricted(itemId) && c.getPlayer().haveItem(itemId, 1, true, false)) || (!ii.itemExists(itemId))) {
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            c.getSession().write(MaplePacketCreator.showItemUnavailable());
            return null;
        }
        final MapleInventoryType type = GameConstants.getInventoryType(itemId);

        if (!type.equals(MapleInventoryType.EQUIP)) {
            short slotMax = ii.getSlotMax(itemId);
            final List<Item> existing = c.getPlayer().getInventory(type).listById(itemId);

            if (!GameConstants.isRechargable(itemId)) {
                Item nItem = null;
                boolean recieved = false;

                if (existing.size() > 0) { // first update all existing slots to slotMax
                    Iterator<Item> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            nItem = (Item) i.next();
                            short oldQ = nItem.getQuantity();

                            if (oldQ < slotMax) {
                                recieved = true;

                                short newQ = (short) Math.min(oldQ + quantity, slotMax);
                                quantity -= (newQ - oldQ);
                                nItem.setQuantity(newQ);
                                c.getSession().write(MaplePacketCreator.updateInventorySlot(type, nItem, false));
                            }
                        } else {
                            break;
                        }
                    }
                }
                // add new slots if there is still something left
                while (quantity > 0) {
                    short newQ = (short) Math.min(quantity, slotMax);
                    if (newQ != 0) {
                        quantity -= newQ;
                        nItem = new Item(itemId, (byte) 0, newQ, (byte) 0);
                        final short newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                        if (newSlot == -1 && recieved) {
                            return nItem;
                        } else if (newSlot == -1) {
                            return null;
                        }
                        recieved = true;
                        c.getSession().write(MaplePacketCreator.addInventorySlot(type, nItem));
                        if (GameConstants.isRechargable(itemId) && quantity == 0) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
                if (recieved) {
                    c.getPlayer().havePartyQuest(nItem.getItemId());
                    return nItem;
                }
            } else {
                // Throwing Stars and Bullets - Add all into one slot regardless of quantity.
                final Item nItem = new Item(itemId, (byte) 0, quantity, (byte) 0);
                final short newSlot = c.getPlayer().getInventory(type).addItem(nItem);

                if (newSlot == -1) {
                    return null;
                }
                c.getSession().write(MaplePacketCreator.addInventorySlot(type, nItem));
                c.getPlayer().havePartyQuest(nItem.getItemId());
                return nItem;
            }
        } else {
            if (quantity == 1) {
                final Item item = ii.randomizeStats((Equip) ii.getEquipById(itemId));
                final short newSlot = c.getPlayer().getInventory(type).addItem(item);

                if (newSlot == -1) {
                    return null;
                }
                c.getSession().write(MaplePacketCreator.addInventorySlot(type, item, true));
                c.getPlayer().havePartyQuest(item.getItemId());
                return item;
            } else {
                throw new InventoryException("Trying to create equip with non-one quantity");
            }
        }
        return null;
    }

    public static boolean addFromDrop(final MapleClient c, final Item item, final boolean show) {
        return addFromDrop(c, item, show, false, true);
    }
    
    public static boolean addFromDrop(final MapleClient c, final Item item, final boolean show, int a) {
        return addFromDrop(c, item, show, false, a != 0);
    }
    
    public static boolean addFromDrop(final MapleClient c, final Item item, final boolean show, final boolean enhance) {
        return addFromDrop(c, item, show, enhance, true);
    }

    public static boolean addFromDrop(final MapleClient c, Item item, final boolean show, final boolean enhance, boolean human) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

        if (c.getPlayer() == null || (ii.isPickupRestricted(item.getItemId()) && c.getPlayer().haveItem(item.getItemId(), 1, true, false)) || (!ii.itemExists(item.getItemId()))) {
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            c.getSession().write(MaplePacketCreator.showItemUnavailable());
            return false;
        }
        final int before = c.getPlayer().itemQuantity(item.getItemId());
        short quantity = item.getQuantity();
        final MapleInventoryType type = GameConstants.getInventoryType(item.getItemId());

        if (!type.equals(MapleInventoryType.EQUIP)) {
            if (item.getQuantity() > 9999 && !c.getPlayer().isGM()) {
                c.getPlayer().upgradeBan(c.getPlayer().getAccountID());
                String Text = "아이템 : " + MapleItemInformationProvider.getInstance().getName(item.getItemId()) + "(" + item.getItemId() + "), 개수 : " + item.getQuantity();
                ServerLogger.getInstance().copyLog("[복사 addfromDrop] " + c.getPlayer().getName() + " (Lv. " + c.getPlayer().getLevel() + ") / 직업 : " + c.getPlayer().getJobName(c.getPlayer().getJob()) + " / " + Text);
                c.getSession().close();
            }
            ServerLogger.getInstance().logItem(LogType.Item.FromScript, c.getPlayer().getId(),c.getPlayer().getName(), item.getItemId(), quantity, MapleItemInformationProvider.getInstance().getName(item.getItemId()), 0, "Script");

            final short slotMax = ii.getSlotMax(item.getItemId());
            final List<Item> existing = c.getPlayer().getInventory(type).listById(item.getItemId());
            if (!GameConstants.isRechargable(item.getItemId())) {
                if (quantity <= 0) { //wth
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    c.getSession().write(MaplePacketCreator.showItemUnavailable());
                    return false;
                }
                if (existing.size() > 0) { // first update all existing slots to slotMax
                    Iterator<Item> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            final Item eItem = (Item) i.next();
                            final short oldQ = eItem.getQuantity();
                            if (oldQ < slotMax && item.getOwner().equals(eItem.getOwner()) && item.getExpiration() == eItem.getExpiration()) {
                                final short newQ = (short) Math.min(oldQ + quantity, slotMax);
                                quantity -= (newQ - oldQ);
                                eItem.setQuantity(newQ);
                                c.getSession().write(MaplePacketCreator.updateInventorySlot(type, eItem, human));
                            }
                        } else {
                            break;
                        }
                    }
                }
                // add new slots if there is still something left
                while (quantity > 0) {
                    final short newQ = (short) Math.min(quantity, slotMax);
                    quantity -= newQ;
                    final Item nItem = new Item(item.getItemId(), (byte) 0, newQ, item.getFlag());
                    nItem.setExpiration(item.getExpiration());
                    nItem.setOwner(item.getOwner());
                    nItem.setPet(item.getPet());
                    nItem.setGMLog(item.getGMLog());
                    short newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                  //  c.getPlayer().rosySymbol();
                    if (newSlot == -1) {
                        c.getSession().write(MaplePacketCreator.getInventoryFull());
                        c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                        item.setQuantity((short) (quantity + newQ));
                        return false;
                    }
                    c.getSession().write(MaplePacketCreator.addInventorySlot(type, nItem, human));
                }
            } else {
                // Throwing Stars and Bullets - Add all into one slot regardless of quantity.
                final Item nItem = new Item(item.getItemId(), (byte) 0, quantity, item.getFlag());
                nItem.setExpiration(item.getExpiration());
                nItem.setOwner(item.getOwner());
                nItem.setPet(item.getPet());
                nItem.setGMLog(item.getGMLog());
                final short newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                if (newSlot == -1) {
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                    return false;
                }
                c.getSession().write(MaplePacketCreator.addInventorySlot(type, nItem));
                //c.getSession().write(MaplePacketCreator.enableActions());
            }
        } else {
            if (quantity == 1) {
                if (enhance) {
                    item = checkEnhanced(item, c.getPlayer());
                }
                final short newSlot = c.getPlayer().getInventory(type).addItem(item);

                if (newSlot == -1) {
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                    return false;
                }
                c.getSession().write(MaplePacketCreator.addInventorySlot(type, item, human));
                if (GameConstants.isHarvesting(item.getItemId())) {
                    c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
                }
            } else {
                throw new RuntimeException("Trying to create equip with non-one quantity");
            }
        }
        if (item.getQuantity() >= 50 && item.getItemId() == 2340000) {
            c.setMonitored(true);
        }
        /*    if (before == 0) {
         switch (item.getItemId()) {
         case AramiaFireWorks.KEG_ID:
         c.getPlayer().dropMessage(5, "You have gained a Powder Keg, you can give this in to Aramia of Henesys.");
         break;
         case AramiaFireWorks.SUN_ID:
         c.getPlayer().dropMessage(5, "You have gained a Warm Sun, you can give this in to Maple Tree Hill through @joyce.");
         break;
         case AramiaFireWorks.DEC_ID:
         c.getPlayer().dropMessage(5, "You have gained a Tree Decoration, you can give this in to White Christmas Hill through @joyce.");
         break;
         }
         }*/
        c.getPlayer().havePartyQuest(item.getItemId());
        if (GameConstants.isStatItem(item.getItemId())) {
        //    c.getPlayer().rosySymbol();
//            c.getPlayer().CustomStatEffect(false);
            c.getPlayer().customizeStat(0);
        }
        if (show) {
            c.getSession().write(MaplePacketCreator.getShowItemGain(item.getItemId(), item.getQuantity()));
        }
        return true;
    }

    public static boolean checkSpace(final MapleClient c, final int itemid, int quantity, final String owner) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (c.getPlayer() == null || (ii.isPickupRestricted(itemid) && c.getPlayer().haveItem(itemid, 1, true, false)) || (!ii.itemExists(itemid))) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return false;
        }
        if (quantity <= 0 && !GameConstants.isRechargable(itemid)) {
            return false;
        }
        final MapleInventoryType type = GameConstants.getInventoryType(itemid);
        if (c == null || c.getPlayer() == null || c.getPlayer().getInventory(type) == null) { //wtf is causing this?
            return false;
        }
        if (!type.equals(MapleInventoryType.EQUIP)) {
            final short slotMax = ii.getSlotMax(itemid);
            final List<Item> existing = c.getPlayer().getInventory(type).listById(itemid);
            if (!GameConstants.isRechargable(itemid)) {
                if (existing.size() > 0) { // first update all existing slots to slotMax
                    for (Item eItem : existing) {
                        final short oldQ = eItem.getQuantity();
                        if (oldQ < slotMax && owner != null && owner.equals(eItem.getOwner())) {
                            final short newQ = (short) Math.min(oldQ + quantity, slotMax);
                            quantity -= (newQ - oldQ);
                        }
                        if (quantity <= 0) {
                            break;
                        }
                    }
                }
            }
            // add new slots if there is still something left
            final int numSlotsNeeded;
            if (slotMax > 0 && !GameConstants.isRechargable(itemid)) {
                numSlotsNeeded = (int) (Math.ceil(((double) quantity) / slotMax));
            } else {
                numSlotsNeeded = 1;
            }
            return !c.getPlayer().getInventory(type).isFull(numSlotsNeeded - 1);
        } else {
            return !c.getPlayer().getInventory(type).isFull();
        }
    }

    public static boolean removeFromSlot(final MapleClient c, final MapleInventoryType type, final short slot, final short quantity, final boolean fromDrop) {
        return removeFromSlot(c, type, slot, quantity, fromDrop, false);
    }

    public static boolean removeFromSlot(final MapleClient c, final MapleInventoryType type, final short slot, short quantity, final boolean fromDrop, final boolean consume, boolean packet) {
        if (c.getPlayer() == null || c.getPlayer().getInventory(type) == null) {
            return false;
        }
        final Item item = c.getPlayer().getInventory(type).getItem(slot);
        if (item != null) {
            final boolean allowZero = consume && GameConstants.isRechargable(item.getItemId());
            c.getPlayer().getInventory(type).removeItem(slot, quantity, allowZero);

            if (item.getQuantity() == 0 && !allowZero) {
                if (packet) {
                    c.getSession().write(MaplePacketCreator.clearInventoryItem(type, item.getPosition(), fromDrop));
                }
            } else {
                if (packet) {
                    c.getSession().write(MaplePacketCreator.updateInventorySlot(type, (Item) item, fromDrop));
                }
            }
            return true;
        } else {
            c.sclose();
        }
        return false;
    }


    public static boolean removeFromSlot(final MapleClient c, final MapleInventoryType type, final short slot, short quantity, final boolean fromDrop, final boolean consume) {
        if (c.getPlayer() == null || c.getPlayer().getInventory(type) == null) {
            return false;
        }
        final Item item = c.getPlayer().getInventory(type).getItem(slot);
        if (item != null) {
            final boolean allowZero = consume && GameConstants.isRechargable(item.getItemId());
            c.getPlayer().getInventory(type).removeItem(slot, quantity, allowZero);
            if (GameConstants.isHarvesting(item.getItemId())) {
                c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
            }

            if (item.getQuantity() == 0 && !allowZero) {
                c.getSession().write(MaplePacketCreator.clearInventoryItem(type, item.getPosition(), fromDrop));
            } else {
                c.getSession().write(MaplePacketCreator.updateInventorySlot(type, (Item) item, fromDrop));
            }
            return true;
        } else {
            c.sclose();
        }
        return false;
    }

    public static boolean removeById(final MapleClient c, final MapleInventoryType type, final int itemId, final int quantity, final boolean fromDrop, final boolean consume) {
        int remremove = quantity;
        if (c.getPlayer() == null || c.getPlayer().getInventory(type) == null) {
            return false;
        }
        for (Item item : c.getPlayer().getInventory(type).listById(itemId)) {
            int theQ = item.getQuantity();
            if (remremove <= theQ && removeFromSlot(c, type, item.getPosition(), (short) remremove, fromDrop, consume)) {
                remremove = 0;
                break;
            } else if (remremove > theQ && removeFromSlot(c, type, item.getPosition(), item.getQuantity(), fromDrop, consume)) {
                remremove -= theQ;
            }
        }
        return remremove <= 0;
    }

    public static boolean removeFromSlot_Lock(final MapleClient c, final MapleInventoryType type, final short slot, short quantity, final boolean fromDrop, final boolean consume) {
        if (c.getPlayer() == null || c.getPlayer().getInventory(type) == null) {
            return false;
        }
        final Item item = c.getPlayer().getInventory(type).getItem(slot);
        if (item != null) {
            if (ItemFlag.LOCK.check(item.getFlag()) || ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                return false;
            }
            return removeFromSlot(c, type, slot, quantity, fromDrop, consume);
        }
        return false;
    }

    public static boolean removeById_Lock(final MapleClient c, final MapleInventoryType type, final int itemId) {
        for (Item item : c.getPlayer().getInventory(type).listById(itemId)) {
            if (removeFromSlot_Lock(c, type, item.getPosition(), (short) 1, false, false)) {
                return true;
            }
        }
        return false;
    }

    public static void move(final MapleClient c, final MapleInventoryType type, final short src, final short dst) {
        if (src < 0 || dst < 0 || src == dst || type == MapleInventoryType.EQUIPPED) {
            return;
        }
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final Item source = c.getPlayer().getInventory(type).getItem(src);
        final Item initialTarget = c.getPlayer().getInventory(type).getItem(dst);
        if (source == null) {
            return;
        }
        boolean bag = false, switchSrcDst = false, bothBag = false;
        short eqIndicator = -1;
        if (dst > c.getPlayer().getInventory(type).getSlotLimit()) {
            if (type == MapleInventoryType.ETC && dst > 100 && dst % 100 != 0) {
                final int eSlot = c.getPlayer().getExtendedSlot((dst / 100) - 1);
                if (eSlot > 0) {
                    final MapleStatEffect ee = ii.getItemEffect(eSlot);
                    if (dst % 100 > ee.getSlotCount() || ee.getType() != ii.getBagType(source.getItemId()) || ee.getType() <= 0) {
                        c.getPlayer().dropMessage(1, "해당 아이템은 인벤토리에 넣을 수 없습니다.");
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    } else {
                        eqIndicator = 0;
                        bag = true;
                    }
                } else {
                    c.getPlayer().dropMessage(1, "해당 아이템은 인벤토리에 넣을 수 없습니다..");
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
            } else {
                c.getPlayer().dropMessage(1, "이 곳에는 아이템을 옮길 수 없습니다.");
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
        }
        if (src > c.getPlayer().getInventory(type).getSlotLimit() && type == MapleInventoryType.ETC && src > 100 && src % 100 != 0) {
            //source should be not null so not much checks are needed
            if (!bag) {
                switchSrcDst = true;
                eqIndicator = 0;
                bag = true;
            } else {
                bothBag = true;
            }
        }
        short olddstQ = -1;
        if (initialTarget != null) {
            olddstQ = initialTarget.getQuantity();
        }
        final short oldsrcQ = source.getQuantity();
        final short slotMax = ii.getSlotMax(source.getItemId());
        c.getPlayer().getInventory(type).move(src, dst, slotMax);
        if (GameConstants.isHarvesting(source.getItemId())) {
            c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
        }
        if (!type.equals(MapleInventoryType.EQUIP) && initialTarget != null
                && initialTarget.getItemId() == source.getItemId()
                && initialTarget.getOwner().equals(source.getOwner())
                && initialTarget.getExpiration() == source.getExpiration()
                && !GameConstants.isRechargable(source.getItemId())
                && !type.equals(MapleInventoryType.CASH)) {
            if (GameConstants.isHarvesting(initialTarget.getItemId())) {
                c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
            }
            if ((olddstQ + oldsrcQ) > slotMax) {
                c.getSession().write(MaplePacketCreator.moveAndMergeWithRestInventoryItem(type, src, dst, (short) ((olddstQ + oldsrcQ) - slotMax), slotMax, bag, switchSrcDst, bothBag));
            } else {
                c.getSession().write(MaplePacketCreator.moveAndMergeInventoryItem(type, src, dst, ((Item) c.getPlayer().getInventory(type).getItem(dst)).getQuantity(), bag, switchSrcDst, bothBag));
            }
        } else {
            c.getSession().write(MaplePacketCreator.moveInventoryItem(type, switchSrcDst ? dst : src, switchSrcDst ? src : dst, eqIndicator, bag, bothBag));
        }
    }

    public static void equip(final MapleClient c, final short src, short dst) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final MapleCharacter chr = c.getPlayer();
        if (chr == null || (GameConstants.GMS && dst == -55)) {
            return;
        }
        final PlayerStats statst = c.getPlayer().getStat();
        Equip source = (Equip) chr.getInventory(MapleInventoryType.EQUIP).getItem(src);
        Equip target = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem(dst);

        if (source == null || source.getDurability() == 0 || GameConstants.isHarvesting(source.getItemId())) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
//        System.err.println(source.getPotential1() + " | " + source.getPotential2() + " | " + source.getPotential3());
        
        //엠블렘 포켓류 바로장착2 소스 / 지정2
        if (c.getPlayer().EmblemPoketItem(source.getItemId()) == -20) {
            dst = -20;
        } else if (c.getPlayer().EmblemPoketItem(source.getItemId()) == -120) {
            dst = -120;
        } else if (c.getPlayer().EmblemPoketItem(source.getItemId()) == -14) {
            dst = -14;
        } else if (c.getPlayer().EmblemPoketItem(source.getItemId()) == -19) {
            dst = -19;
        } else if (c.getPlayer().EmblemPoketItem(source.getItemId()) == -119) {
            dst = -119;
        }

        final Map<String, Integer> stats = ii.getEquipStats(source.getItemId());

        if (stats == null) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (dst > -1200 && dst < -999 && !GameConstants.isEvanDragonItem(source.getItemId()) && !GameConstants.isMechanicItem(source.getItemId())) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        } else if ((dst <= -1200 || (dst >= -999 && dst < -99)) && !stats.containsKey("cash")) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        } else if (dst <= -1300 && c.getPlayer().getAndroid() == null) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (!ii.canEquip(stats, source.getItemId(), chr.getLevel(), chr.getJob(), chr.getFame(), statst.getTotalStr(), statst.getTotalDex(), statst.getTotalLuk(), statst.getTotalInt(), c.getPlayer().getStat().levelBonus)) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (GameConstants.isWeapon(source.getItemId()) && dst != -10 && dst != -11 && dst != -110 && dst != -111) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (dst == (-18) && !GameConstants.isMountItemAvailable(source.getItemId(), c.getPlayer().getJob())) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (dst == (-118) && source.getItemId() / 10000 != 190) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (dst == -59) { //pendant
            if (FileTime.compareFileTime(c.getPlayer().getEquipExtExpire(), FileTime.systemTimeToFileTime()) < 0) {
                c.sendPacket(MaplePacketCreator.enableActions());
                return;
            }
//            MapleQuestStatus stat = c.getPlayer().getQuestNoAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
//            if (stat == null || stat.getCustomData() == null || Long.parseLong(stat.getCustomData()) < System.currentTimeMillis()) {
//                c.getSession().write(MaplePacketCreator.enableActions());
//                return;
//            }
        }
        if ((GameConstants.isKatara(source.getItemId()) || source.getItemId() / 10000 == 135) && !ii.isCash(source.getItemId())) {
            dst = (byte) -10; //shield slot
        }
        if (GameConstants.isEvanDragonItem(source.getItemId()) && (chr.getJob() < 2200 || chr.getJob() > 2218)) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }

        if (GameConstants.isMechanicItem(source.getItemId()) && (chr.getJob() < 3500 || chr.getJob() > 3512)) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }

        if (source.getItemId() / 1000 == 1112) { //ring
            for (RingSet s : RingSet.values()) {
                if (s.id.contains(Integer.valueOf(source.getItemId()))) {
                    List<Integer> theList = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).listIds();
                    for (Integer i : s.id) {
                        if (theList.contains(i)) {
                            if ("Job Ring".equals(StringUtil.makeEnumHumanReadable(s.name()))) {
                                c.getPlayer().dropMessage(1, "모험가의 반지와 리린의 반지와\r\n시그너스의 코히누르는\r\n서로 한개만 장비할 수 있습니다.");
                            } else {
                                c.getPlayer().dropMessage(1, "이미 " + (StringUtil.makeEnumHumanReadable(s.name())) + " 아이템을 장비하고 있어 장비할 수 없습니다.");
                            }
                            c.getSession().write(MaplePacketCreator.enableActions());
                            return;
                        }
                    }
                }
            }
        }
        if (target != null) {
            if (GameConstants.isStatItem(target.getItemId()) || GameConstants.isStatItem(source.getItemId())) {
//                c.getPlayer().CustomStatEffect(true);
                c.getPlayer().customizeStat(0);
            }
           // c.getPlayer().rosySymbol();
        }
        switch (dst) {
            case -6: { // Top
                final Item top = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -5);
                if (top != null && GameConstants.isOverall(top.getItemId())) {
                    if (chr.getInventory(MapleInventoryType.EQUIP).isFull()) {
                        c.getSession().write(MaplePacketCreator.getInventoryFull());
                        c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -5, chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
                }
                break;
            }
            case -5: {
                final Item top = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -5);
                final Item bottom = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -6);
                if (top != null && GameConstants.isOverall(source.getItemId())) {
                    if (chr.getInventory(MapleInventoryType.EQUIP).isFull(bottom != null && GameConstants.isOverall(source.getItemId()) ? 1 : 0)) {
                        c.getSession().write(MaplePacketCreator.getInventoryFull());
                        c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -5, chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
                }
                if (bottom != null && GameConstants.isOverall(source.getItemId())) {
                    if (chr.getInventory(MapleInventoryType.EQUIP).isFull()) {
                        c.getSession().write(MaplePacketCreator.getInventoryFull());
                        c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -6, chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
                }
                break;
            }
            case -10: { // Shield
                Item weapon = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
                if (GameConstants.isKatara(source.getItemId())) {
                    if ((chr.getJob() != 900 && (chr.getJob() < 430 || chr.getJob() > 434)) || weapon == null || !GameConstants.isDagger(weapon.getItemId())) {
                        c.getSession().write(MaplePacketCreator.getInventoryFull());
                        c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                        return;
                    }
                } else if (weapon != null && GameConstants.isTwoHanded(weapon.getItemId())) {
                    if (chr.getInventory(MapleInventoryType.EQUIP).isFull()) {
                        c.getSession().write(MaplePacketCreator.getInventoryFull());
                        c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -11, chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
                }
                break;
            }
            case -11: { // Weapon
                Item shield = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -10);
                if (shield != null && GameConstants.isTwoHanded(source.getItemId())) {
                    if (chr.getInventory(MapleInventoryType.EQUIP).isFull()) {
                        c.getSession().write(MaplePacketCreator.getInventoryFull());
                        c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -10, chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
                }
                break;
            }
        }
        source = (Equip) chr.getInventory(MapleInventoryType.EQUIP).getItem(src); // Equip
        target = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem(dst); // Currently equipping
        if (source == null) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        short flag = source.getFlag();
        if (stats.get("equipTradeBlock") != null || source.getItemId() / 10000 == 167) { // Block trade when equipped.
            if (!ItemFlag.UNTRADEABLE.check(flag)) {
                flag |= ItemFlag.UNTRADEABLE.getValue();
                source.setFlag(flag);
                c.getSession().write(MaplePacketCreator.updateSpecialItemUse_(source, MapleInventoryType.EQUIP.getType(), c.getPlayer()));
            }
        }
        if (ItemFlag.KARMA_EQ.check(flag)) {
            flag |= ItemFlag.KARMA_EQ.getValue();
            source.setFlag((byte) (flag - ItemFlag.KARMA_EQ.getValue()));
            c.getSession().write(MaplePacketCreator.updateSpecialItemUse_(source, MapleInventoryType.EQUIP.getType(), c.getPlayer()));
            c.getSession().write(MaplePacketCreator.serverNotice(1, ii.getName(source.getItemId()) + "의 교환가능 횟수가 차감됐습니다."));
        }
        if (source.getItemId() / 10000 == 166) {
            if (source.getAndroid() == null) {
                final int uid = MapleInventoryIdentifier.getInstance();
                source.setUniqueId(uid);
                source.setAndroid(MapleAndroid.create(source.getItemId(), uid));
                flag |= ItemFlag.LOCK.getValue();
                flag |= ItemFlag.UNTRADEABLE.getValue();
                flag |= ItemFlag.ANDROID_ACTIVATED.getValue();
                source.setFlag(flag);
                c.getSession().write(MaplePacketCreator.updateSpecialItemUse_(source, MapleInventoryType.EQUIP.getType(), c.getPlayer()));
            }
            chr.removeAndroid();
            chr.setAndroid(source.getAndroid());
        } else if (dst <= -1300 && chr.getAndroid() != null) {
            chr.setAndroid(chr.getAndroid()); //respawn it
        }
        chr.getInventory(MapleInventoryType.EQUIP).removeSlot(src);
        if (target != null) {
            chr.getInventory(MapleInventoryType.EQUIPPED).removeSlot(dst);
        }
        source.setPosition(dst);
        source.setEquippedTime(System.currentTimeMillis());
        chr.getInventory(MapleInventoryType.EQUIPPED).addFromDB(source);
        if (target != null) {
            target.setPosition(src);
            chr.getInventory(MapleInventoryType.EQUIP).addFromDB(target);
        }
        if (GameConstants.isWeapon(source.getItemId())) {
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.BOOSTER);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.SPIRIT_CLAW);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.SOULARROW);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.WK_CHARGE);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.LIGHTNING_CHARGE);
        }
        if (source.getItemId() / 10000 == 190 || source.getItemId() / 10000 == 191) {
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.MECH_CHANGE);
        } else if (GameConstants.isReverseItem(source.getItemId())) {
            //chr.finishAchievement(9);
        } else if (GameConstants.isTimelessItem(source.getItemId())) {
            //chr.finishAchievement(10);
        } else if (stats.containsKey("reqLevel") && stats.get("reqLevel") >= 140) {
            //chr.finishAchievement(41);
        } else if (stats.containsKey("reqLevel") && stats.get("reqLevel") >= 130) {
            //chr.finishAchievement(40);
        }
        if (source.getState() > 1) {
            int[] potentials = {source.getPotential1(), source.getPotential2(), source.getPotential3()};
            for (int i : potentials) {
                if (i > 0) {
                    StructPotentialItem pot = ii.getPotentialInfo(i).get(ii.getReqLevel(source.getItemId()) / 50);
                    if (pot != null && pot.potentialID == 30040) {
                        c.getPlayer().changeSkillLevel_Skip(SkillFactory.getSkill(c.getPlayer().getStat().getSkillByJob(8006, c.getPlayer().getJob())), (byte) 1, (byte) 0, true);
                    }
                    if (pot != null && pot.potentialID == 30400) {
                        c.getPlayer().changeSkillLevel_Skip(SkillFactory.getSkill(c.getPlayer().getStat().getSkillByJob(8004, c.getPlayer().getJob())), (byte) 1, (byte) 0, true);
                    }
                    if (pot != null && pot.potentialID == 30401) {
                        c.getPlayer().changeSkillLevel_Skip(SkillFactory.getSkill(c.getPlayer().getStat().getSkillByJob(8005, c.getPlayer().getJob())), (byte) 1, (byte) 0, true);
                    }
                    if (pot != null && pot.skillID > 0) {
                        c.getPlayer().changeSkillLevel_Skip(SkillFactory.getSkill(c.getPlayer().getStat().getSkillByJob(pot.skillID, c.getPlayer().getJob())), (byte) 1, (byte) 0, true);
                    }
                }
            }
        }
        if (target != null) {
            if (target.getState() > 1) {
                int[] potentials = {target.getPotential1(), target.getPotential2(), target.getPotential3()};
                for (int i : potentials) {
                    if (i > 0) {
                        StructPotentialItem pot = ii.getPotentialInfo(i).get(ii.getReqLevel(target.getItemId()) / 10);
                        if (pot != null && pot.potentialID == 30040) {
                            c.getPlayer().changeSkillLevel_Skip(SkillFactory.getSkill(c.getPlayer().getStat().getSkillByJob(8006, c.getPlayer().getJob())), (byte) 0, (byte) 0, true);
                        }
                        if (pot != null && pot.potentialID == 30400) {
                            c.getPlayer().changeSkillLevel_Skip(SkillFactory.getSkill(c.getPlayer().getStat().getSkillByJob(8004, c.getPlayer().getJob())), (byte) 0, (byte) 0, true);
                        }
                        if (pot != null && pot.potentialID == 30401) {
                            c.getPlayer().changeSkillLevel_Skip(SkillFactory.getSkill(c.getPlayer().getStat().getSkillByJob(8005, c.getPlayer().getJob())), (byte) 0, (byte) 0, true);
                        }
                        if (pot != null && pot.skillID > 0) {
                            c.getPlayer().changeSkillLevel_Skip(SkillFactory.getSkill(c.getPlayer().getStat().getSkillByJob(pot.skillID, c.getPlayer().getJob())), (byte) 0, (byte) 0, true);
                        }
                    }
                }
            }
        }
        c.getSession().write(MaplePacketCreator.moveInventoryItem(MapleInventoryType.EQUIP, src, dst, (byte) 2, false, false));
        chr.equipChanged();
        
        if (target != null) {
            int boosterItemID = 0;
            if ((boosterItemID = GameConstants.getBoosterItemID(target.getItemId())) != 0) {
                MapleStatEffect boosterEff = ii.getItemEffect(boosterItemID);
                if (boosterEff != null) {
                    c.getPlayer().cancelEffect(boosterEff, -2);
//                    c.getPlayer().CustomStatEffect(false);
                    c.getPlayer().customizeStat(0);
                }
            } else if ((boosterItemID = GameConstants.getBoosterItemID(source.getItemId())) != 0) {
                MapleStatEffect boosterEff = ii.getItemEffect(boosterItemID);
                if (boosterEff != null) {
                    boosterEff.applyTo(c.getPlayer());
                    c.getPlayer().customizeStat(0);
                }
            } else if (GameConstants.isStatItem(target.getItemId()) || GameConstants.isStatItem(source.getItemId())) {
//                c.getPlayer().CustomStatEffect(true);
                c.getPlayer().customizeStat(0);
            }
           // c.getPlayer().rosySymbol();
        } else {
            if (source != null) {
                int boosterItemID = 0;
                if ((boosterItemID = GameConstants.getBoosterItemID(source.getItemId())) != 0) {
                    MapleStatEffect boosterEff = ii.getItemEffect(boosterItemID);
                    if (boosterEff != null) {
                        boosterEff.applyTo(c.getPlayer());
                        c.getPlayer().customizeStat(0);
//                        c.getPlayer().CustomStatEffect(false);
                    }
                } else if ((target != null && GameConstants.isStatItem(target.getItemId())) || GameConstants.isStatItem(source.getItemId())) {
                    c.getPlayer().customizeStat(0);
//                    c.getPlayer().CustomStatEffect(true);
                } else if (dst == -18 || dst == -19 || dst == -20) {
                    c.getPlayer().customizeStat(0);
                }
            }
           // c.getPlayer().rosySymbol();
        }
    }

    public static void unequip(final MapleClient c, final short src, final short dst) {
        Equip source = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(src);
        Equip target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(dst);

        if (dst < 0 || source == null || (GameConstants.GMS && src == -55)) {
            return;
        }
        if (target != null && src <= 0) { // do not allow switching with equip
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return;
        }
        c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).removeSlot(src);
        if (target != null) {
            c.getPlayer().getInventory(MapleInventoryType.EQUIP).removeSlot(dst);
        }
        source.setPosition(dst);
        c.getPlayer().getInventory(MapleInventoryType.EQUIP).addFromDB(source);
        if (target != null) {
            target.setPosition(src);
            c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).addFromDB(target);
        }

        if (GameConstants.isWeapon(source.getItemId())) {
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.BOOSTER);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.SPIRIT_CLAW);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.SOULARROW);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.WK_CHARGE);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.LIGHTNING_CHARGE);
        } else if (source.getItemId() / 10000 == 190 || source.getItemId() / 10000 == 191) {
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.MECH_CHANGE);
        } else if (source.getItemId() / 10000 == 166) {
            c.getPlayer().removeAndroid();
        } else if (src <= -1300 && c.getPlayer().getAndroid() != null) {
            c.getPlayer().setAndroid(c.getPlayer().getAndroid());
        }
        if (source.getState() > 1) {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            int[] potentials = {source.getPotential1(), source.getPotential2(), source.getPotential3()};
            for (int i : potentials) {
                if (i > 0) {
                    StructPotentialItem pot = ii.getPotentialInfo(i).get(ii.getReqLevel(source.getItemId()) / 10);
                    if (pot != null && pot.potentialID == 30040) {
                        c.getPlayer().changeSkillLevel_Skip(SkillFactory.getSkill(c.getPlayer().getStat().getSkillByJob(8006, c.getPlayer().getJob())), (byte) 0, (byte) 0, true);
                    }
                    if (pot != null && pot.potentialID == 30400) {
                        c.getPlayer().changeSkillLevel_Skip(SkillFactory.getSkill(c.getPlayer().getStat().getSkillByJob(8004, c.getPlayer().getJob())), (byte) 0, (byte) 0, true);
                    }
                    if (pot != null && pot.potentialID == 30401) {
                        c.getPlayer().changeSkillLevel_Skip(SkillFactory.getSkill(c.getPlayer().getStat().getSkillByJob(8005, c.getPlayer().getJob())), (byte) 0, (byte) 0, true);
                    }
                    if (pot != null && pot.skillID > 0) {
                        c.getPlayer().changeSkillLevel_Skip(SkillFactory.getSkill(c.getPlayer().getStat().getSkillByJob(pot.skillID, c.getPlayer().getJob())), (byte) 0, (byte) 0, true);
                    }
                }
            }
        }
        
        c.getSession().write(MaplePacketCreator.moveInventoryItem(MapleInventoryType.EQUIP, src, dst, (byte) 1, false, false));
        c.getPlayer().equipChanged();
        if (target != null) {
            int boosterItemID = 0;
            if ((boosterItemID = GameConstants.getBoosterItemID(target.getItemId())) != 0) {
                MapleStatEffect boosterEff = MapleItemInformationProvider.getInstance().getItemEffect(boosterItemID);
                if (boosterEff != null) {
                    c.getPlayer().cancelEffect(boosterEff, -2);
                    c.getPlayer().customizeStat(0);
//                    c.getPlayer().CustomStatEffect(false);
                }
            } else if (GameConstants.isStatItem(target.getItemId()) || GameConstants.isStatItem(source.getItemId())) {
                c.getPlayer().customizeStat(0);
//                c.getPlayer().CustomStatEffect(true);
            } else if (dst == -18 || dst == -19 || dst == -20) {
                c.getPlayer().customizeStat(0);
            }
           // c.getPlayer().rosySymbol();
        } else {
            if (source != null) {
                int boosterItemID = 0;
                if ((boosterItemID = GameConstants.getBoosterItemID(source.getItemId())) != 0) {
                    MapleStatEffect boosterEff = MapleItemInformationProvider.getInstance().getItemEffect(boosterItemID);
                    if (boosterEff != null) {
                        c.getPlayer().cancelEffect(boosterEff, -2);
                        c.getPlayer().customizeStat(0);
//                        c.getPlayer().CustomStatEffect(false);
                    }
                } else if ((target != null && GameConstants.isStatItem(target.getItemId())) || GameConstants.isStatItem(source.getItemId())) {
                    c.getPlayer().customizeStat(0);
//                    c.getPlayer().CustomStatEffect(true);
                } else if (src == -18 || src == -19 || src == -20) {
                    c.getPlayer().customizeStat(0);
                }
            }
            //c.getPlayer().rosySymbol();
        }
    }

    public static boolean drop(final MapleClient c, MapleInventoryType type, final short src, final short quantity) {
        return drop(c, type, src, quantity, false);
    }

    public static boolean drop(final MapleClient c, MapleInventoryType type, final short src, short quantity, final boolean npcInduced) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (src < 0) {
            type = MapleInventoryType.EQUIPPED;
        }
        if (c.getPlayer() == null || c.getPlayer().getMap() == null) {
            return false;
        }
        final Item source = c.getPlayer().getInventory(type).getItem(src);
        if (quantity < 0 || source == null || (GameConstants.GMS && src == -55) || (!npcInduced && GameConstants.isPet(source.getItemId())) || (quantity == 0 && !GameConstants.isRechargable(source.getItemId())) || c.getPlayer().inPVP()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return false;
        }

        final short flag = source.getFlag();
        if (quantity > source.getQuantity() && !GameConstants.isRechargable(source.getItemId())) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return false;
        }
        if (ItemFlag.LOCK.check(flag) || (quantity != 1 && type == MapleInventoryType.EQUIP)) { // hack
            c.getSession().write(MaplePacketCreator.enableActions());
            return false;
        }
        if (quantity <= 0 && !GameConstants.isThrowingStar(source.getItemId()) && !GameConstants.isBullet(source.getItemId()) || 
            quantity > 30000 ||
            quantity > (short) ii.getSlotMax(source.getItemId())
            ) { 
           // AutobanManager.getInstance().autoban(c, "교환핵");//교환복사 복사핵 복사
            return false;
        }
        if (quantity >= 9999) {
          ServerLogger.getInstance().logTrade(LogType.Trade.DropAndPick, c.getPlayer().getId(), c.getPlayer().getName(), "뿌리고있음", MapleItemInformationProvider.getInstance().getName(source.getItemId()) + " " + source.getQuantity() + "개", "맵 : " + c.getPlayer().getMapId());
        }
       final Point dropPos = new Point(c.getPlayer().getPosition());
        c.getPlayer().getCheatTracker().checkDrop();//0203
        if (quantity < source.getQuantity() && !GameConstants.isRechargable(source.getItemId())) {
            final Item target = source.copy();
            target.setQuantity(quantity);
            source.setQuantity((short) (source.getQuantity() - quantity));
            c.getSession().write(MaplePacketCreator.dropInventoryItemUpdate(type, source));

            if (ii.isDropRestricted(target.getItemId()) || ii.isAccountShared(target.getItemId())) {
                if (ItemFlag.KARMA_EQ.check(flag)) {
                    target.setFlag((byte) (flag - ItemFlag.KARMA_EQ.getValue()));
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos, true, true);
                } else if (ItemFlag.KARMA_USE.check(flag)) {
                    target.setFlag((byte) (flag - ItemFlag.KARMA_USE.getValue()));
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos, true, true);
                } else {
                    c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos);
                }
            } else {
                if (GameConstants.isPet(source.getItemId()) || ItemFlag.UNTRADEABLE.check(flag)) {
                    c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos);
                } else {
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos, true, true);
                }
            }
        } else {
            c.getPlayer().getInventory(type).removeSlot(src);
            if (GameConstants.isHarvesting(source.getItemId())) {
                c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
            }
            c.getSession().write(MaplePacketCreator.dropInventoryItem((src < 0 ? MapleInventoryType.EQUIP : type), src));
            if (src < 0) {
                c.getPlayer().equipChanged();
            }
            if (ii.isDropRestricted(source.getItemId()) || ii.isAccountShared(source.getItemId())) {
                if (ItemFlag.KARMA_EQ.check(flag)) {
                    source.setFlag((byte) (flag - ItemFlag.KARMA_EQ.getValue()));
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos, true, true);
                } else if (ItemFlag.KARMA_USE.check(flag)) {
                    source.setFlag((byte) (flag - ItemFlag.KARMA_USE.getValue()));
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos, true, true);
                } else {
                    c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos);
                }
            } else {
                if (GameConstants.isPet(source.getItemId()) || ItemFlag.UNTRADEABLE.check(flag)) {
                    c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos);
                } else {
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos, true, true);
                }
            }
        }
       // c.getPlayer().rosySymbol();
        if (GameConstants.isStatItem(source.getItemId())) {
            c.getPlayer().customizeStat(0);
//            c.getPlayer().CustomStatEffect(true);
        } else if (source.getItemId() == c.getPlayer().getItemEffect()) {
            c.getPlayer().customizeStat(0);
        }
        return true;
    }
}
