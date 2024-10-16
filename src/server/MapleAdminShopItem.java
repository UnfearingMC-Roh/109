/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

/**
 *
 * @author ssay4
 */
public class MapleAdminShopItem {

    private int id, itemid, price, count, maxquantity, type;

    public MapleAdminShopItem(int Id, int itemId, int p, int c, int t, int max) {
        id = Id;
        itemid = itemId;
        price = p;
        count = c;
        type = t;
        maxquantity = max;
    }

    public void setCount(int c) {
        count = c;
    }

    public void gainCount(int c) {
        count += c;
    }

    public int getCount() {
        return count;
    }

    public int getItemId() {
        return itemid;
    }

    public int getPrice() {
        return price;
    }

    public int getId() {
        return id;
    }

    public int getType() {
        return type;
    }

    public int getMaxQuantity() {
        return maxquantity;
    }

}
