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

import java.util.List;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.MapleClient;
import client.MapleCharacter;
import constants.GameConstants;
import client.inventory.ItemLoader;
import database.DatabaseConnection;
import handling.world.World;
import java.util.Map;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MerchItemPackage;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.packet.PlayerShopPacket;
import tools.data.LittleEndianAccessor;

public class HiredMerchantHandler {

    public static final boolean UseHiredMerchant(final MapleClient c, final boolean packet) {
        if (c.getPlayer().getMap() != null && c.getPlayer().getMap().allowPersonalShop()) {
            final byte state = checkExistance(c.getPlayer().getAccountID(), c.getPlayer().getId());

            switch (state) {
                case 1:
                    c.getPlayer().dropMessage(1, "프레드릭에게서 먼저 아이템을 찾아가 주세요.");
                    break;
                case 0:
                    boolean merch = World.hasMerchant(c.getPlayer().getAccountID(), c.getPlayer().getId());
                    if (!merch) {
                        if (c.getChannelServer().isShutdown()) {
                            c.getPlayer().dropMessage(1, "서버가 종료 됩니다.");
                            return false;
                        }
                        if (packet) {
                            c.getSession().write(PlayerShopPacket.sendTitleBox());
                        }
                        return true;
                    } else {
                        c.getPlayer().dropMessage(1, "해당 계정으로 열린 고용상점이 이미 존재합니다.");
                    }
                    break;
                default:
                    c.getPlayer().dropMessage(1, "An unknown error occured.");
                    break;
            }
        } else {
            c.getPlayer().dropMessage(1, "사용할 수 없습니다.");
            c.getSession().write(MaplePacketCreator.enableActions());
        }
        return false;
    }

    private static final byte checkExistance(final int accid, final int cid) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * from hiredmerch where accountid = ? OR characterid = ?");
            ps.setInt(1, accid);
            ps.setInt(2, cid);
            rs = ps.executeQuery();

            if (rs.next()) {
                ps.close();
                rs.close();
                return 1;
            }
            rs.close();
            ps.close();
            return 0;
        } catch (SQLException se) {
            return -1;
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
    
    public static final void displayMerch(MapleClient c) {
        final int conv = c.getPlayer().getConversation();
        Pair<Integer, Integer> merch = World.findMerchant(c.getPlayer().getAccountID(), c.getPlayer().getId());
        if (merch != null) {
            c.sendPacket(PlayerShopPacket.merchItemAlreadyOpen(merch.getLeft() - 1, merch.getRight()));
            c.getPlayer().setConversation(0);
        } else if (c.getChannelServer().isShutdown()) {
            c.getPlayer().dropMessage(1, "서버 종료 중입니다.");
            c.getPlayer().setConversation(0);
        } else if (conv == 3) { // Hired Merch
            final MerchItemPackage pack = loadItemFrom_Database(c.getPlayer().getAccountID());

            if (pack == null) {
                c.getPlayer().setConversation(0);
                c.getSession().write(PlayerShopPacket.merchItemStore2());
            } 
            else {
                c.getSession().write(PlayerShopPacket.merchItemStore_ItemData(pack));
            }
        }
    }

    public static final void MerchantItemStore(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer() == null) {
            return;
        }
        final byte operation = slea.readByte();

        if (operation == 0x19) {
            if (c.getPlayer().getConversation() != 3) {
                return;
            }
            boolean merch = World.hasMerchant(c.getPlayer().getAccountID(), c.getPlayer().getId());
            if (merch) {
                c.getPlayer().dropMessage(1, "해당 계정으로 열려있는 상점을 닫은 후 다시 시도하세요.");
                c.getPlayer().setConversation(0);
                return;
            }
            final MerchItemPackage pack = loadItemFrom_Database(c.getPlayer().getAccountID());

            if (pack == null) {
                c.getPlayer().dropMessage(1, "An unknown error occured.");
                return;
            } else if (c.getChannelServer().isShutdown()) {
                c.getPlayer().dropMessage(1, "The world is going to shut down.");
                c.getPlayer().setConversation(0);
                return;
            }
//            if (true) {
//                c.getSession().write(PlayerShopPacket.merchItem_Message((byte) 0x21)); //인벤토리에 자리가 없어 받지 못했어
//                return;
//            }
            if (!check(c.getPlayer(), pack, c)) {
                c.getPlayer().setConversation(0);
                return;
            }
            if (deletePackage(c.getPlayer().getAccountID(), pack.getPackageid(), c.getPlayer().getId())) {
                c.getPlayer().gainMeso(pack.getMesos(), false);
                for (Item item : pack.getItems()) {
                    MapleInventoryManipulator.addFromDrop(c, item, false);
                }
                c.getSession().write(PlayerShopPacket.merchItem_Message((byte) 29));
                //c.getPlayer().dropMessage(1, "기모링.");
                //c.getSession().write(PlayerShopPacket.merchItem_Message((byte) 0x1D)); //아이템과 메소를 모두 찾았어.
                c.getPlayer().setConversation(0);
            } else {
                c.getPlayer().dropMessage(1, "An unknown error occured.");
            }
        } else if (operation == 0x1B) {
            c.getPlayer().setConversation(0);
        }
    }

    private static final boolean check(final MapleCharacter chr, final MerchItemPackage pack, final MapleClient c) {//고상 체크 (받을 때)
        /*
         * 이것은 나의 영혼이 담겼다 공유하면 15대가 고자
         * 제작 큐티버크
         */
        if (chr.getMeso() + pack.getMesos() < 0 || chr.getMeso() + pack.getMesos() >= Integer.MAX_VALUE) { //메소 합산이 음수 또는 최대치를 넘겼을 때
            c.getSession().write(PlayerShopPacket.merchItem_Message((byte) 30));
            c.getPlayer().setConversation(0);
            return false;
        }
        byte eq = 0, use = 0, setup = 0, etc = 0, cash = 0;
        int imsi = 0, slotMax = 0, quantity = 0, qq = 0;
        List<Integer> itemList = new ArrayList<Integer>();
        for (Item item : pack.getItems()) {
            if (MapleItemInformationProvider.getInstance().isPickupRestricted(item.getItemId()) && chr.haveItem(item.getItemId(), 1)) {
                c.getSession().write(PlayerShopPacket.merchItem_Message((byte) 31)); // 하나밖에 못갖는아이템
                c.getPlayer().setConversation(0);
                return false;
            }
            if (item.getItemId() < 2000000) {
                eq++;
            } else {
                if (GameConstants.isRechargable(item.getItemId())) {
                    use++;
                } else {
                    if (!itemList.contains(item.getItemId())) {
                        itemList.add(item.getItemId());
                    }
                }
            }
        }
        for (int items : itemList) {
            qq = 0;
            imsi = 0;
            quantity = 0;
            slotMax = MapleItemInformationProvider.getInstance().getSlotMax(items);
            for (final Item invitem : chr.getInventory(GameConstants.getInventoryType(items))) {
                if (invitem.getItemId() == items) {
                    if (invitem.getQuantity() < slotMax) {
                        quantity += invitem.getQuantity();
                    } else {
                        quantity += slotMax;
                    }
                }
            }
            for (Item itemq : pack.getItems()) {
                if (itemq.getItemId() == items) {
                    qq += itemq.getQuantity();
                }
            }
            imsi = (int) Math.ceil((double) (qq + quantity - slotMax * (int) Math.ceil((double) quantity / slotMax)) / slotMax);
            switch (items / 1000000) {
                case 2:
                    use += imsi;
                    break;
                case 3:
                    setup += imsi;
                    break;
                case 4:
                    etc += imsi;
                    break;
                case 5:
                    cash += imsi;
                    break;
            }
        }
        if (chr.getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() < eq || chr.getInventory(MapleInventoryType.USE).getNumFreeSlot() < use || chr.getInventory(MapleInventoryType.SETUP).getNumFreeSlot() < setup || chr.getInventory(MapleInventoryType.ETC).getNumFreeSlot() < etc || chr.getInventory(MapleInventoryType.CASH).getNumFreeSlot() < cash) {
            c.getSession().write(PlayerShopPacket.merchItem_Message((byte) 33)); // 인벤부족
            c.getPlayer().setConversation(0);
            return false;
        }
        return true;
    }

    private static final boolean deletePackage(final int accid, final int packageid, final int chrId) {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("DELETE from hiredmerch where accountid = ? OR packageid = ? OR characterid = ?");
            ps.setInt(1, accid);
            ps.setInt(2, packageid);
            ps.setInt(3, chrId);
            ps.executeUpdate();
            ps.close();
            ItemLoader.HIRED_MERCHANT.saveItems(null, packageid);
            return true;
        } catch (SQLException e) {
            return false;
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

    private static final MerchItemPackage loadItemFrom_Database(final int accountid) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * from hiredmerch where accountid = ?");
            ps.setInt(1, accountid);

            rs = ps.executeQuery();

            if (!rs.next()) {
                ps.close();
                rs.close();
                return null;
            }
            final int packageid = rs.getInt("PackageId");

            final MerchItemPackage pack = new MerchItemPackage();
            pack.setPackageid(packageid);
            pack.setMesos(rs.getInt("Mesos"));
            pack.setSentTime(rs.getLong("time"));

            ps.close();
            rs.close();

            Map<Long, Pair<Item, MapleInventoryType>> items = ItemLoader.HIRED_MERCHANT.loadItems(false, packageid);
            if (items != null) {
                List<Item> iters = new ArrayList<Item>();
                for (Pair<Item, MapleInventoryType> z : items.values()) {
                    iters.add(z.left);
                }
                pack.setItems(iters);
            }

            return pack;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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
