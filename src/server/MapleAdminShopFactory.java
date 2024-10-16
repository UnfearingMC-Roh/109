/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import client.MapleAdminShop;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author ssay4
 */
public class MapleAdminShopFactory {

    private Map<Integer, MapleAdminShop> shops = new HashMap<Integer, MapleAdminShop>();
    private static MapleAdminShopFactory instance = new MapleAdminShopFactory();

    public static MapleAdminShopFactory getInstance() {
        return instance;
    }

    public void clear() {
        shops.clear();
    }

    public MapleAdminShop getShop(int shopId, int npcid) {
        if (shops.containsKey(shopId)) {
            return shops.get(shopId);
        }
        return loadShop(shopId, npcid);
    }

    private MapleAdminShop loadShop(int id, int npcid) {
        MapleAdminShop ret = MapleAdminShop.createFromDB(id, npcid);
        if (ret != null) {
            shops.put(ret.getId(), ret);
        }
        return ret;
    }

}
