/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import server.MapleAdminShopItem;
import server.MapleItemInformationProvider;
import tools.MaplePacketCreator;

/**
 *
 * @author 글귀
 */
public class MapleAdminShop {

    private int id, npcid;

    private List<MapleAdminShopItem> items = new LinkedList<>();

    public MapleAdminShop(final int id, final int npcId) {
        this.id = id;
        this.npcid = npcId;
    }

    public void addItem(MapleAdminShopItem item) {
        items.add(item);
    }

    public List<MapleAdminShopItem> getItems() {
        return items;
    }

    public void sendShop(MapleClient c) {
        c.getPlayer().setAdminShop(this);
        c.getSession().write(MaplePacketCreator.AdminShopDlg(this.getNpcId(), this));
    }

    public MapleAdminShopItem findById(int ID) {
        for (MapleAdminShopItem item : items) {
            if (item.getId() == ID) {
                return item;
            }
        }
        return null;
    }

    public int getId() {
        return id;
    }

    public int getNpcId() {
        return npcid;
    }

    public static MapleAdminShop createFromDB(int id, int npcid) {
        MapleAdminShop ret = null;
        int shopId;
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM adminshops WHERE shopid = ?");

            ps.setInt(1, id);
            rs = ps.executeQuery();
            int i = 0;
            while (rs.next()) {
                if (i == 0) {
                    shopId = rs.getInt("shopid");
                    ret = new MapleAdminShop(shopId, npcid);
                }
                if (!ii.itemExists(rs.getInt("itemid"))) {
                    continue;
                }
                if (ret != null) {
                    ret.addItem(new MapleAdminShopItem(rs.getInt("id"), rs.getInt("itemid"), rs.getInt("price"), rs.getInt("count"), rs.getInt("type"), rs.getInt("maxquantity")));
                }
                i++;
            }
            rs.close();
            ps.close();

            return ret;
        } catch (SQLException e) {
            System.err.println("Could not load shop");
            e.printStackTrace();
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
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
        }
        return ret;
    }
}
