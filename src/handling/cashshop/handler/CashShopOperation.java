package handling.cashshop.handler;

import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;

import constants.GameConstants;
import client.MapleClient;
import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleQuestStatus;
import client.inventory.MapleInventoryType;
import client.inventory.MapleRing;
import client.inventory.MapleInventoryIdentifier;
import client.inventory.Item;
import client.inventory.ItemFlag;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.CharacterTransfer;
import handling.world.World;
import java.util.List;
import server.CashItemFactory;
import server.CashItemInfo;
import server.CashShop;
import server.MTSCart;
import server.MTSStorage;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.log.LogType;
import server.log.ServerLogger;
import server.quest.MapleQuest;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.packet.MTSCSPacket;
import tools.Triple;
import tools.data.LittleEndianAccessor;
import tools.packet.CSPacket;
import util.FileTime;

public class CashShopOperation {

    public static void LeaveCS(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        CashShopServer.getPlayerStorage().deregisterPlayer(chr);
        c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, c.getSessionIPAddress());

        try {

            World.ChannelChange_Data(new CharacterTransfer(chr), chr.getId(), c.getChannel());
            LoginServer.setCodeHash(chr.getId(), c.getCodeHash());
            c.getSession().write(MaplePacketCreator.getChannelChange(c, c.getChannelServer().getPort()));
        } finally {
            final String s = c.getSessionIPAddress();
            LoginServer.addIPAuth(s.substring(s.indexOf('/') + 1, s.length()));
            chr.saveToDB(false, true);
            c.setPlayer(null);
            c.setReceiving(false);
        }
    }

    public static void EnterCS(final int playerid, final MapleClient c) {
        CharacterTransfer transfer = CashShopServer.getPlayerStorage().getPendingCharacter(playerid);
        boolean mts = false;
        if (transfer == null) {
            transfer = CashShopServer.getPlayerStorageMTS().getPendingCharacter(playerid);
            mts = true;
        }
        MapleCharacter chr = MapleCharacter.ReconstructChr(transfer, c, false);

        c.setPlayer(chr);
        c.setAccID(chr.getAccountID());

        if (!c.CheckIPAddress()) { // Remote hack
            c.getSession().close();
            return;
        }

        final int state = c.getLoginState();
        boolean allowLogin = false;
        if (state == MapleClient.LOGIN_SERVER_TRANSITION || state == MapleClient.CHANGE_CHANNEL) {
            if (!World.isCharacterListConnected(c.loadCharacterNames(c.getWorld()))) {
                allowLogin = true;
            }
        }
        if (!allowLogin) {
            c.setPlayer(null);
            c.getSession().close();
            return;
        }
        c.updateLoginState(MapleClient.LOGIN_LOGGEDIN, c.getSessionIPAddress());
        if (mts) {
            CashShopServer.getPlayerStorageMTS().registerPlayer(chr);
            final MTSCart cart = MTSStorage.getInstance().getCart(c.getPlayer().getId());
            cart.refreshCurrentView();
            MTSOperation.MTSUpdate(cart, c);
        } else {
            CashShopServer.getPlayerStorage().registerPlayer(chr);
            c.getSession().write(MTSCSPacket.warpCS(c));
            CSUpdate(c);
        }

    }

    public static void CSUpdate(final MapleClient c) {
        c.getSession().write(MTSCSPacket.getCSGifts(c));
        doCSPackets(c);
        c.getSession().write(MTSCSPacket.sendWishList(c.getPlayer(), false));
    }

    public static void CouponCode(final String code, final MapleClient c) {
        if (code.length() <= 0) {
            return;
        }
        Triple<Boolean, Integer, Integer> info = null;
        try {
            info = MapleCharacterUtil.getNXCodeInfo(code);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (info != null && info.left) {
            int type = info.mid, item = info.right;
            try {
                MapleCharacterUtil.setNXCodeUsed(c.getPlayer().getName(), code);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            /*
             * Explanation of type!
             * Basically, this makes coupon codes do
             * different things!
             *
             * Type 1: A-Cash,
             * Type 2: Maple Points
             * Type 3: Item.. use SN
             * Type 4: Mesos
             */
            Map<Integer, Item> itemz = new HashMap<Integer, Item>();
            int maplePoints = 0, mesos = 0;
            switch (type) {
                case 1:
                case 2:
                    c.getPlayer().modifyCSPoints(type, item, false);
                    maplePoints = item;
                    break;
                case 3:
                    CashItemInfo itez = CashItemFactory.getInstance().getItem(item);
                    if (itez == null) {
                        c.getSession().write(MTSCSPacket.sendCSFail(0));
                        return;
                    }
                    byte slot = MapleInventoryManipulator.addId(c, itez.getId(), (short) 1, "", "Cash shop: coupon code" + " on " + FileoutputUtil.CurrentReadable_Date());
                    if (slot <= -1) {
                        c.getSession().write(MTSCSPacket.sendCSFail(0));
                        return;
                    } else {
                        itemz.put(item, c.getPlayer().getInventory(GameConstants.getInventoryType(item)).getItem(slot));
                    }
                    break;
                case 4:
                    c.getPlayer().gainMeso(item, false);
                    mesos = item;
                    break;
            }
            c.getSession().write(MTSCSPacket.showCouponRedeemedItem(itemz, mesos, maplePoints, c));
            System.out.println(c.getPlayer().getName() + "님이 " + code + "를 사용했습니다.");
        } else {
            c.getSession().write(MTSCSPacket.sendCSFail(info == null ? 149 : 151)); //A1, 9F
        }
    }

    public static final void BuyCashItem(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final int action = slea.readByte();
        if (action == 0) {
            slea.skip(2); //00 00
            CouponCode(slea.readMapleAsciiString(), c);
        } else if (action == 3) {
            final int toCharge = slea.readByte() + 1;
            final CashItemInfo item = CashItemFactory.getInstance().getItem(slea.readInt());
            
            if (item != null && chr.getCSPoints(toCharge) >= item.getPrice()) {
                if (!item.genderEquals(c.getPlayer().getGender())) {
                    c.getSession().write(MTSCSPacket.sendCSFail(130));
                    doCSPackets(c);
                    return;
                } else if (c.getPlayer().getCashInventory().getItemsSize() >= 100) {
                    c.getSession().write(MTSCSPacket.sendCSFail(129));
                    doCSPackets(c);
                    return;
                }
                
                for (int i : GameConstants.cashBlock) {
                    if (item.getId() == i) {
                        c.getPlayer().dropMessage(1, GameConstants.getCashBlockedMsg(item.getId()));
                        doCSPackets(c);
                        return;
                    }
                }
                chr.modifyCSPoints(toCharge, -item.getPrice(), false);
                Item itemz = chr.getCashInventory().toItem(item);
                if (itemz != null && itemz.getUniqueId() > 0 && itemz.getItemId() == item.getId() && itemz.getQuantity() == item.getCount()) {
                    if (toCharge == 1 && itemz.getType() == 1) {
                        itemz.setFlag((short) (ItemFlag.KARMA_EQ.getValue()));
                    } else if (toCharge == 1 && itemz.getType() != 1) {
                        itemz.setFlag((short) (ItemFlag.KARMA_USE.getValue()));
                    }
                    chr.getCashInventory().addToInventory(itemz);
                    c.getSession().write(MTSCSPacket.showBoughtCSItem(itemz, item.getSN(), c.getAccID()));
                } else {
                    c.getSession().write(MTSCSPacket.sendCSFail(0));
                    chr.dropMessage(1, "현재 구매할 수 없는 물품입니다.1");
                }
            } else {
                c.getSession().write(MTSCSPacket.sendCSFail(0));//1229 원래1
                chr.dropMessage(1, "현재 구매할 수 없는 물품입니다.2");
            }
        } else if (action == 4 || action == 30) { //gift, package
            slea.skip(4);
            final CashItemInfo item = CashItemFactory.getInstance().getItem(slea.readInt());
            String partnerName = slea.readMapleAsciiString();
            String msg = slea.readMapleAsciiString();
            if (item == null || c.getPlayer().getCSPoints(1) < item.getPrice() || msg.length() > 73 || msg.length() < 1) { //dont want packet editors gifting random stuff =P
                c.getSession().write(MTSCSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            }
            Triple<Integer, Integer, Integer> info = MapleCharacterUtil.getInfoByName(partnerName, c.getPlayer().getWorld());
            if (info == null || info.getLeft().intValue() <= 0 || info.getLeft().intValue() == c.getPlayer().getId() || info.getMid().intValue() == c.getAccID()) {
                //c.getSession().write(MTSCSPacket.sendCSFail(143)); //1229 원래 130
                c.getPlayer().dropMessage(1, "존재하지 않는 캐릭터 입니다.");
                doCSPackets(c);
                return;
            } else if (!item.genderEquals(info.getRight().intValue())) {
                c.getPlayer().dropMessage(1, "존재하지 않는 캐릭터 입니다.");
                //c.getSession().write(MTSCSPacket.sendCSFail(143));//1229 원래 130
                doCSPackets(c);
                return;
            } else if (CashShop.giftCount(info.getLeft().intValue()) >= 100) {
                //c.getSession().write(CSPacket.sendCSFail(129));
                c.getPlayer().dropMessage(1, "상대방의 선물함이 가득 찼습니다.");
                doCSPackets(c);
                return;
            } else {
                c.getPlayer().getCashInventory().gift(info.getLeft().intValue(), c.getPlayer().getName(), msg, item.getSN(), MapleInventoryIdentifier.getInstance());
                ServerLogger.getInstance().logTrade(LogType.Trade.CashShopGift, c.getPlayer().getId(), c.getPlayer().getName(), partnerName, "시리얼 넘버 : " + item.getSN() + " - " + item.getCount() + " 개 / 캐시 : " + item.getPrice(), (c.getPlayer().isDonateShop() ? "본섭캐시샵" : "일반캐시샵") + " / 메시지 : " + msg);

                c.getPlayer().modifyCSPoints(1, -item.getPrice(), false);
                c.getSession().write(MTSCSPacket.sendGift(item.getPrice(), item.getId(), item.getCount(), partnerName));
                MapleCharacterUtil.sendNote(partnerName, c.getPlayer().getName(), "캐시샵에 선물이 도착했습니다. 확인해 주세요.", 0);
            }
        } else if (action == 0x1B) { //1.2.41 OK 패키지 선물
            if (c.getPlayer().isGM() && !c.getPlayer().isSuperGM()) {
                c.getPlayer().dropMessage(1, "GM은 캐시를 선물할 수 없습니다.");
                doCSPackets(c);
                return;
            }
            slea.skip(4); //idcode 2
            final CashItemInfo item = CashItemFactory.getInstance().getItem(slea.readInt());

            String partnerName = slea.readMapleAsciiString();
            String msg = slea.readMapleAsciiString();
            if (item == null || c.getPlayer().getCSPoints(1) < item.getPrice() || msg.length() > 73 || msg.length() < 1) { //dont want packet editors gifting random stuff =P
                c.getSession().write(CSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            }
            Triple<Integer, Integer, Integer> info = MapleCharacterUtil.getInfoByName(partnerName, c.getPlayer().getWorld());
            if (info == null || info.getLeft().intValue() <= 0 || info.getLeft().intValue() == c.getPlayer().getId() || info.getMid().intValue() == c.getAccID()) {
                //c.getSession().write(CSPacket.sendCSFail(130)); //9E v75
                c.getPlayer().dropMessage(1, "존재하지 않는 캐릭터 입니다.");
                doCSPackets(c);
                return;
            } else if (!item.genderEquals(info.getRight().intValue())) {
                //c.getSession().write(CSPacket.sendCSFail(130));
                c.getPlayer().dropMessage(1, "존재하지 않는 캐릭터 입니다.");
                doCSPackets(c);
                return;
            } else if (CashShop.giftCount(info.getLeft().intValue()) >= 100) {
                c.getPlayer().dropMessage(1, "상대방의 선물함이 가득 찼습니다.");
                doCSPackets(c);
                return;
            } else {
                c.getPlayer().getCashInventory().gift(info.getLeft().intValue(), c.getPlayer().getName(), msg, item.getSN(), MapleInventoryIdentifier.getInstance());
                ServerLogger.getInstance().logTrade(LogType.Trade.CashShopGift, c.getPlayer().getId(), c.getPlayer().getName(), partnerName, "시리얼 넘버 : " + item.getSN() + " - " + item.getCount() + " 개 / 캐시 : " + item.getPrice(), (c.getPlayer().isDonateShop() ? "본섭캐시샵" : "일반캐시샵") + " / 메시지 : " + msg);

                c.getPlayer().modifyCSPoints(1, -item.getPrice(), false);
                c.getSession().write(CSPacket.sendPackageGift(item.getPrice(), item.getId(), item.getCount(), partnerName));
                MapleCharacterUtil.sendNote(partnerName, c.getPlayer().getName(), "캐시샵에 선물이 도착했습니다. 확인해 주세요.", 0);
            }
        } else if (action == 5) { // Wishlist
            chr.clearWishlist();
            if (slea.available() < 40) {
                c.getSession().write(MTSCSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            }
            int[] wishlist = new int[10];
            for (int i = 0; i < 10; i++) {
                wishlist[i] = slea.readInt();
            }
            chr.setWishlist(wishlist);
            c.getSession().write(MTSCSPacket.sendWishList(chr, true));

        } else if (action == 6) { // Increase inv
            final int toCharge = slea.readByte() + 1;
            final boolean coupon = slea.readByte() > 0;
            if (coupon) {
                final MapleInventoryType type = getInventoryType(slea.readInt());
                if (chr.getCSPoints(toCharge) >= 700 && chr.getInventory(type).getSlotLimit() < 89) {
                    chr.modifyCSPoints(toCharge, -700, false);
                    chr.getInventory(type).addSlot((byte) 80);
                    c.getSession().write(MTSCSPacket.increasedInvSlots(type.getType(), chr.getInventory(type).getSlotLimit()));
                } else {
                    c.getSession().write(MTSCSPacket.sendCSFail(141));
                }
            } else {
                final MapleInventoryType type = MapleInventoryType.getByType(slea.readByte());
                if (chr.getCSPoints(toCharge) >= 700 && chr.getInventory(type).getSlotLimit() <= 92) {
                    chr.modifyCSPoints(toCharge, -700, false);
                    chr.getInventory(type).addSlot((byte) 80);
                    c.getSession().write(MTSCSPacket.increasedInvSlots(type.getType(), chr.getInventory(type).getSlotLimit()));
                } else {
                    c.getSession().write(MTSCSPacket.sendCSFail(141));
                }
            }
        } else if (action == 7) { // Increase slot space
            final int toCharge = slea.readByte() + 1;
            final int coupon = slea.readByte() > 0 ? 2 : 1;
            if (chr.getCSPoints(toCharge) >= 700 * coupon && chr.getStorage().getSlots() <= (48 - (4 * coupon))) {
                chr.modifyCSPoints(toCharge, -700 * coupon, false);
                chr.getStorage().increaseSlots((byte) (4 * coupon));
                chr.getStorage().saveToDB();
                c.getSession().write(MTSCSPacket.increasedStorageSlots(chr.getStorage().getSlots()));
                //chr.dropMessage(1, "slot :" + chr.getStorage().getSlots());
            } else {
                c.getSession().write(MTSCSPacket.sendCSFail(141));
            }
        } else if (action == 8) { //...9 = pendant slot expansion
//            slea.skip(1);
            final int toCharge = slea.readByte() + 1;
            CashItemInfo item = CashItemFactory.getInstance().getItem(slea.readInt());
            int slots = c.getCharacterSlots();
            if (item == null || c.getPlayer().getCSPoints(toCharge) < item.getPrice() || slots > 15 || item.getId() != 5430000) {
                c.getSession().write(MTSCSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            }
            if (c.gainCharacterSlot()) {
                c.getPlayer().modifyCSPoints(toCharge, -item.getPrice(), false);
                c.getSession().write(MTSCSPacket.increasedCharacterSlots(slots + 1));
            } else {
                c.getSession().write(MTSCSPacket.sendCSFail(0));
            }
        } else if (action == 10) { // 펜던트 슬롯 늘리기
            final int toCharge = slea.readByte() + 1;
            final int sn = slea.readInt();
            CashItemInfo item = CashItemFactory.getInstance().getItem(sn);
            if (item == null || c.getPlayer().getCSPoints(toCharge) < item.getPrice() || item.getId() / 10000 != 555) {
                c.getSession().write(MTSCSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            }
//            MapleQuestStatus marr = c.getPlayer().getQuestNoAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
//            if (marr != null && marr.getCustomData() != null && Long.parseLong(marr.getCustomData()) >= System.currentTimeMillis()) {
//                chr.dropMessage(1, "이미 펜던트 늘리기가 적용중입니다.");
//                doCSPackets(c);
//                c.getSession().write(MaplePacketCreator.enableActions());
//                return;
//            } else {
//                long days = 0;
//                if (item.getId() == 5550000) { // 펜던트 슬롯늘리기 : 30일
//                    days = 30;
//                } else if (item.getId() == 5550001) { // 펜던트 슬롯늘리기 : 7일
//                    days = 7;
//                }
//                c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT)).setCustomData(String.valueOf(System.currentTimeMillis() + days * 24 * 60 * 60000));
//                chr.modifyCSPoints(toCharge, -item.getPrice(), false);
//                chr.dropMessage(1, "펜던트 슬롯 개수를 늘렸습니다.");
//                doCSPackets(c);
//            }
            FileTime cur = FileTime.systemTimeToFileTime();
            if (FileTime.compareFileTime(chr.getEquipExtExpire(), cur) >= 0) {
                chr.dropMessage(1, "이미 펜던트 늘리기가 적용중입니다.");
                doCSPackets(c);
                c.sendPacket(MaplePacketCreator.enableActions());
                return;
            } else {
                int day = 0;
                if (item.getId() == 5550000)
                    day = 30;
                if (item.getId() == 5550001)
                    day = 7;
                FileTime extExpire = FileTime.systemTimeToFileTime();
                extExpire.add(FileTime.FILETIME_DAY, day);
                chr.setEquipExtExpire(extExpire);
                chr.modifyCSPoints(toCharge, -item.getPrice(), false);
                chr.dropMessage(1, "펜던트 슬롯 개수를 늘렸습니다.");
                doCSPackets(c);
            }
        } else if (action == 14) { //get item from csinventory
            //uniqueid, 00 01 01 00, type->position(short)
            Item item = c.getPlayer().getCashInventory().findByCashId((int) slea.readLong());
            if (item != null && item.getQuantity() > 0 && MapleInventoryManipulator.checkSpace(c, item.getItemId(), item.getQuantity(), item.getOwner())) {
                Item item_ = item.copy();
                short pos = MapleInventoryManipulator.addbyItem(c, item_, true);
                if (pos >= 0) {
                    if (item_.getPet() != null) {
                        item_.getPet().setInventoryPosition(pos);
                        c.getPlayer().addPetz(item_.getPet());
                    }
                    c.getPlayer().getCashInventory().removeFromInventory(item);
                    c.getSession().write(MTSCSPacket.confirmFromCSInventory(item_, pos));
                } else {
                    c.getSession().write(MTSCSPacket.sendCSFail(0));
                }
            } else {
                c.getSession().write(MTSCSPacket.sendCSFail(0));
            }
        } else if (action == 15) { //put item in cash inventory
            int uniqueid = (int) slea.readLong();
            MapleInventoryType type = MapleInventoryType.getByType(slea.readByte());
            Item item = c.getPlayer().getInventory(type).findByUniqueId(uniqueid);
            if (item != null && item.getQuantity() > 0 && item.getUniqueId() > 0 && c.getPlayer().getCashInventory().getItemsSize() < 100) {
                Item item_ = item.copy();
                MapleInventoryManipulator.removeFromSlot(c, type, item.getPosition(), item.getQuantity(), false, false, false);
                if (item_.getPet() != null) {
                    c.getPlayer().removePetCS(item_.getPet());
                }
                item_.setPosition((byte) 0);
                c.getPlayer().getCashInventory().addToInventory(item_);
                c.getSession().write(MTSCSPacket.confirmToCSInventory(item, c.getAccID(), -1));
            } else {
                c.getSession().write(MTSCSPacket.sendCSFail(0));
            }
        } else if (action == (GameConstants.GMS ? 32 : 31) || action == (GameConstants.GMS ? 38 : 37)) { //36 = friendship, 30 = crush
            slea.skip(4);
            final int toCharge = 1;
            final CashItemInfo item = CashItemFactory.getInstance().getItem(slea.readInt());
            final String partnerName = slea.readMapleAsciiString();
            final String msg = slea.readMapleAsciiString();
            if (item == null || !GameConstants.isEffectRing(item.getId()) || c.getPlayer().getCSPoints(toCharge) < item.getPrice() || msg.length() > 73 || msg.length() < 1) {
                c.getSession().write(MTSCSPacket.sendCSFail(10));
                doCSPackets(c);
                return;
            } else if (!item.genderEquals(c.getPlayer().getGender())) {
                c.getSession().write(MTSCSPacket.sendCSFail(143));
                doCSPackets(c);
                return;
            } else if (c.getPlayer().getCashInventory().getItemsSize() >= 100) {
                c.getSession().write(MTSCSPacket.sendCSFail(129));
                doCSPackets(c);
                return;
            }
            Triple<Integer, Integer, Integer> info = MapleCharacterUtil.getInfoByName(partnerName, c.getPlayer().getWorld());
            if (info == null || info.getLeft().intValue() <= 0 || info.getLeft().intValue() == c.getPlayer().getId()) {
                c.getSession().write(MTSCSPacket.sendCSFail(144)); //9E v75
                doCSPackets(c);
                return;
            } else if (info.getMid().intValue() == c.getAccID()) {
                c.getSession().write(MTSCSPacket.sendCSFail(130)); //9D v75
                doCSPackets(c);
                return;
            } else {
                if (info.getRight().intValue() == c.getPlayer().getGender() && action == 30) {
                    c.getSession().write(MTSCSPacket.sendCSFail(143)); //9B v75
                    doCSPackets(c);
                    return;
                }
                int err = MapleRing.createRing(item.getId(), c.getPlayer(), partnerName, msg, info.getLeft().intValue(), item.getSN());
                if (err != 1) {
                    c.getSession().write(MTSCSPacket.sendCSFail(0)); //9E v75
                    doCSPackets(c);
                    return;
                }
                c.getPlayer().modifyCSPoints(toCharge, -item.getPrice(), false);
                //c.getSession().write(MTSCSPacket.showBoughtCSItem(itemz, item.getSN(), c.getAccID()));
                c.getSession().write(MTSCSPacket.sendGift(item.getPrice(), item.getId(), item.getCount(), partnerName));
                MapleCharacterUtil.sendNote(partnerName, c.getPlayer().getName(), "캐시샵에 선물이 도착했습니다. 확인해 주세요.", 0);
            }

        } else if (action == 32) {
            final int toCharge = slea.readByte() + 1;
            final CashItemInfo item = CashItemFactory.getInstance().getItem(slea.readInt());
            List<Integer> ccc = null;
            if (item != null) {
                ccc = CashItemFactory.getInstance().getPackageItems(item.getId());
            }
            if (item == null || ccc == null || c.getPlayer().getCSPoints(toCharge) < item.getPrice()) {
                c.getSession().write(MTSCSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            } else if (!item.genderEquals(c.getPlayer().getGender())) {
                c.getSession().write(MTSCSPacket.sendCSFail(130));
                doCSPackets(c);
                return;
            } else if (c.getPlayer().getCashInventory().getItemsSize() >= (100 - ccc.size())) {
                c.getSession().write(MTSCSPacket.sendCSFail(129));
                doCSPackets(c);
                return;
            }
            Map<Integer, Item> ccz = new HashMap<Integer, Item>();
            for (int i : ccc) {
                final CashItemInfo cii = CashItemFactory.getInstance().getSimpleItem(i);
                if (cii == null) {
                    continue;
                }
                Item itemz = c.getPlayer().getCashInventory().toItem(cii);
                if (itemz == null || itemz.getUniqueId() <= 0) {
                    continue;
                }
                for (int iz : GameConstants.cashBlock) {
                    if (itemz.getItemId() == iz) {
                        continue;
                    }
                }
                ccz.put(i, itemz);
                c.getPlayer().getCashInventory().addToInventory(itemz);
            }
            chr.modifyCSPoints(toCharge, -item.getPrice(), false);
            c.getSession().write(MTSCSPacket.showBoughtCSPackage(ccz, c.getAccID()));

        } else if (action == 34) {
            final CashItemInfo item = CashItemFactory.getInstance().getItem(slea.readInt());
            if (item == null || !MapleItemInformationProvider.getInstance().isQuestItem(item.getId())) {
                c.getSession().write(MTSCSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            } else if (c.getPlayer().getMeso() < item.getPrice()) {
                c.getSession().write(MTSCSPacket.sendCSFail(149));
                doCSPackets(c);
                return;
            } else if (c.getPlayer().getInventory(GameConstants.getInventoryType(item.getId())).getNextFreeSlot() < 0) {
                c.getSession().write(MTSCSPacket.sendCSFail(129));
                doCSPackets(c);
                return;
            }
            byte pos = MapleInventoryManipulator.addId(c, item.getId(), (short) item.getCount(), null, "Cash shop: quest item" + " on " + FileoutputUtil.CurrentReadable_Date());
            if (pos < 0) {
                c.getSession().write(MTSCSPacket.sendCSFail(129));
                doCSPackets(c);
                return;
            }
            chr.gainMeso(-item.getPrice(), false);
            c.getSession().write(MTSCSPacket.showBoughtCSQuestItem(item.getPrice(), (short) item.getCount(), pos, item.getId()));
        } else if (action == 0x1C) { //아이템없애기
            slea.skip(4); //주민등록번호
            int accountId = (int) slea.readInt();
            Item item = c.getPlayer().getCashInventory().findByCashId(accountId);
            if (item == null || item.getExpiration() != -1) {
                c.getSession().write(MTSCSPacket.sendCSFail(129));
                doCSPackets(c);
                return;
            }
            c.getPlayer().getCashInventory().removeFromInventory(item);
            c.getSession().write(MTSCSPacket.payBackResult(accountId, 0));
        } else if (action == 45) { //idk
            c.getSession().write(MTSCSPacket.redeemResponse());
        }
        doCSPackets(c);
    }

    public static final void GiftCashItem(final LittleEndianAccessor slea, final MapleClient c) {
        slea.skip(4); // 주민등록번호
        final CashItemInfo item = CashItemFactory.getInstance().getItem(slea.readInt());
        String partnerName = slea.readMapleAsciiString();
        String msg = slea.readMapleAsciiString();
        if (item == null || c.getPlayer().getCSPoints(1) < item.getPrice() || msg.length() > 73 || msg.length() < 1) { //dont want packet editors gifting random stuff =P
            c.getSession().write(MTSCSPacket.sendCSFail(0));
            doCSPackets(c);
            return;
        }
        Triple<Integer, Integer, Integer> info = MapleCharacterUtil.getInfoByName(partnerName, c.getPlayer().getWorld());
        if (info == null || info.getLeft().intValue() <= 0 || info.getLeft().intValue() == c.getPlayer().getId() || info.getMid().intValue() == c.getAccID()) {
            c.getSession().write(MTSCSPacket.sendCSFail(0xA2));
            doCSPackets(c);
        } else if (!item.genderEquals(info.getRight().intValue())) {
            c.getSession().write(MTSCSPacket.sendCSFail(0xA3));
            doCSPackets(c);
        } else {
            if (!c.getPlayer().getCashInventory().giftMax(info.getLeft().intValue())) {
                c.getPlayer().dropMessage(1, "선물 받는 캐릭터의 선물함이 가득찼습니다.");
                doCSPackets(c);
                return;
            }
            c.getPlayer().getCashInventory().gift(info.getLeft().intValue(), c.getPlayer().getName(), msg, item.getSN(), MapleInventoryIdentifier.getInstance());
            ServerLogger.getInstance().logTrade(LogType.Trade.CashShopGift, c.getPlayer().getId(), c.getPlayer().getName(), partnerName, "시리얼 넘버 : " + item.getSN() + " - " + item.getCount() + " 개 / 캐시 : " + item.getPrice(), (c.getPlayer().isDonateShop() ? "본섭캐시샵" : "일반캐시샵") + " / 메시지 : " + msg);

            c.getPlayer().modifyCSPoints(1, -item.getPrice(), false);
            c.getSession().write(MTSCSPacket.sendGift(item.getPrice(), item.getId(), item.getCount(), partnerName));
            MapleCharacterUtil.sendNote(partnerName, c.getPlayer().getName(), "캐시샵에 선물이 도착했습니다. 확인해 주세요.", 0);
        }
    }

    private static final MapleInventoryType getInventoryType(final int id) {
        switch (id) {
            case 50200016:
                return MapleInventoryType.EQUIP;
            case 50200017:
                return MapleInventoryType.USE;
            case 50200018:
                return MapleInventoryType.SETUP;
            case 50200019:
                return MapleInventoryType.ETC;
            default:
                return MapleInventoryType.UNDEFINED;
        }
    }

    public static final void doCSPackets(MapleClient c) {
        c.getSession().write(MTSCSPacket.getCSInventory(c));
        c.getSession().write(MTSCSPacket.showNXMapleTokens(c.getPlayer()));
        c.getSession().write(MTSCSPacket.enableCSUse());
        c.getPlayer().getCashInventory().checkExpire(c);
    }
}
