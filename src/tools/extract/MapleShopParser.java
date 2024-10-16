/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.extract;

import database.DatabaseConnection;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.MapleItemInformationProvider;
import server.life.MapleLifeFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 티썬
 */
public class MapleShopParser {

    public static void main(String[] args) throws Exception {
        DatabaseConnection.init();
        File fr = new File("imgs");
        MapleDataProvider pro = MapleDataProviderFactory.getDataProvider(fr);
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement del1 = con.prepareStatement("TRUNCATE `shops`");
        PreparedStatement del2 = con.prepareStatement("TRUNCATE `shopitems`");
        PreparedStatement del3 = con.prepareStatement("ALTER TABLE  `shops` AUTO_INCREMENT =1");
        //
        del1.executeUpdate();
        del2.executeUpdate();
        del3.executeUpdate();
        del1.close();
        del2.close();
        del3.close();
        int shopid = 1;
        PreparedStatement ps1 = con.prepareStatement("INSERT INTO shops (`npcid`) VALUES (?)");
        PreparedStatement ps2 = con.prepareStatement("INSERT INTO shopitems (shopid, itemid, price, position, max, expiretime, level) VALUES (?, ?, ?, ?, ?, ?, ?)");
        //PreparedStatement ps3 = con.prepareStatement("INSERT INTO shopitems (shopid, itemid, price, position, max) VALUES (?, ?, ?, ?, ?)");

        System.setProperty("net.sf.odinms.wzpath", "wz");
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        ii.runItems();
        ii.runEtc();
        MapleLifeFactory.loadQuestCounts();

        for (MapleData dd : pro.getData("NpcShop.img")) {
            int npcid = Integer.parseInt(dd.getName());
            try {
                if (MapleLifeFactory.getNPC(npcid) == null) {
                    System.out.println(npcid + " does not exists NPC.. continue.");
                    continue;
                }
            } catch (Exception e) {
                System.out.println(npcid + " does not exists NPC.. continue.");
                continue;
            }
            try {
                ps1.setInt(1, npcid);
                for (MapleData sp : dd.getChildren()) {
                    int i = Integer.parseInt(sp.getName()) * 10;
                    int item = MapleDataTool.getInt("item", sp);
                    if (item / 10000 == 207 && item != 2070000) {
                        continue;
                    }
                    if (!ii.itemExists(item)) {
                        System.err.println(item + " Item does not exists.. continue.");
                        continue;
                    }
                    int price = MapleDataTool.getInt("price", sp, -1);
                    if (price == -1) {
                        continue;
                    }
                    ps2.setInt(1, shopid);
                    ps2.setInt(2, item);
                    ps2.setInt(3, price);
                    ps2.setInt(4, i);
                    ps2.setInt(5, 0);
                    ps2.setInt(6, 0);
                    ps2.setInt(7, 0);
                    ps2.addBatch();
                    if (item == 2060000) { //화살 2천개
                        ps2.setInt(1, shopid);
                        ps2.setInt(2, item);
                        ps2.setInt(3, 1600);
                        ps2.setInt(4, i + 11);
                        ps2.setInt(5, 2000);
                        ps2.setInt(6, 0);
                        ps2.setInt(7, 0);
                        ps2.addBatch();
                    }
                    if (item == 2061000) { //석궁 화살 2천개
                        ps2.setInt(1, shopid);
                        ps2.setInt(2, item);
                        ps2.setInt(3, 1600);
                        ps2.setInt(4, i + 2);
                        ps2.setInt(5, 2000);
                        ps2.setInt(6, 0);
                        ps2.setInt(7, 0);
                        ps2.addBatch();
                    }
                    if (item == 2070000) { //불릿
                        ps2.setInt(1, shopid);
                        ps2.setInt(2, 2330000);
                        ps2.setInt(3, 600);
                        ps2.setInt(4, i + 1);
                        ps2.setInt(5, 0);
                        ps2.setInt(6, 0);
                        ps2.setInt(7, 0);
                        ps2.addBatch();
                    }
                }
                //Shop HardCoding
                if (isFancyGoodsShop(shopid)) {
                    ps2.setInt(1, shopid);
                    ps2.setInt(2, 2190000);//거짓말 탐지기
                    ps2.setInt(3, 10000);
                    ps2.setInt(4, 100 * 10);
                    ps2.setInt(5, 0);
                    ps2.setInt(6, 0);
                    ps2.setInt(7, 0);
                    ps2.addBatch();
                }
                if (shopid == 9090000) {
                    ps2.setInt(1, shopid);
                    ps2.setInt(2, 1002140);//거짓말 탐지기
                    ps2.setInt(3, 10000);
                    ps2.setInt(4, 100 * 10);
                    ps2.setInt(5, 0);
                    ps2.setInt(6, 0);
                    ps2.setInt(7, 0);
                    ps2.addBatch();
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Shopid : " + npcid);
                return;
            }
            ps1.addBatch();
            shopid++;
        }
        ps1.executeBatch();
        ps2.executeBatch();

        //상점추가(1100001);
        //상점추가(1100002);
        상점복사(1, 2150001);//에레브 무기 상점(방어구 추가 해야함)
        상점복사(9, 2150002);//커닝스퀘어잡화상점
        상점복사(9, 1052116);//커닝스퀘어잡화상점
        상점복사(1, 1100001);//에레브 무기 상점(방어구 추가 해야함)
        상점복사(9, 1100002);//에레브 잡화 상점
        상점복사(1, 1200001);//리엔 무기 상점(방어구 추가 해야함)
        상점복사(9, 1200002);//리엔 잡화 상점
        상점복사(9, 1301000);//버섯의성
        상점복사(9, 9000081);//황금사원 
        상점복사(9, 9090000);//묘묘

        //Shop HardCoding
        아이템추가(9090000, 9090000, 2022003, 1150, 141, 1);
        아이템추가(9090000, 9090000, 2022000, 1600, 142, 1);
        아이템추가(9090000, 9090000, 2001000, 1600, 143, 1);
        아이템추가(9090000, 9090000, 2001001, 2200, 144, 1);
        아이템추가(9090000, 9090000, 2001002, 4000, 145, 1);
        아이템추가(9090000, 9090000, 2020012, 4400, 146, 1);
        아이템추가(9090000, 9090000, 2020013, 5600, 147, 1);
        아이템추가(9090000, 9090000, 2020014, 8100, 148, 1);
        아이템추가(9090000, 9090000, 2020015, 10200, 149, 1);
        
        //모건 1091000
        상점추가(1091000);
        아이템추가(1091000, 1091000, 1492000, 3000, 100, 1);
        아이템추가(1091000, 1091000, 1492001, 6000, 101, 1);
        아이템추가(1091000, 1091000, 1492002, 10000, 102, 1);
        아이템추가(1091000, 1091000, 1492003, 22000, 103, 1);
        아이템추가(1091000, 1091000, 1492004, 50000, 104, 1);
        아이템추가(1091000, 1091000, 1482000, 3000, 105, 1);
        아이템추가(1091000, 1091000, 1482001, 6000, 106, 1);
        아이템추가(1091000, 1091000, 1482002, 10000, 107, 1);
        아이템추가(1091000, 1091000, 1482003, 20000, 108, 1);
        아이템추가(1091000, 1091000, 1482004, 52000, 109, 1);
        아이템추가(1091000, 1091000, 1442004, 24000, 110, 1);
        아이템추가(1091000, 1091000, 1302007, 3000, 111, 1);
        아이템추가(1091000, 1091000, 1322007, 6000, 112, 1);
        /*try {
            ps1.setInt(1, 9090000);
            ps2.setInt(1, 9090000);
            ps2.setInt(2, 1002140);//거짓말 탐지기
            ps2.setInt(3, 10000);
            ps2.setInt(4, 100 * 10);
            ps2.setInt(5, 0);
            ps2.setInt(6, 0);
            ps2.setInt(7, 0);
            ps2.addBatch();
        } catch (Exception e) {
            e.printStackTrace();
            //System.out.println("Shopid : " + npcid);
            return;
        }*/
        ps1.executeBatch();
        ps2.executeBatch();
        ps1.close();
        ps2.close();
    }

    public static void 아이템추가(int npcid, int shopid, int itemid, int price, int position, int count) throws Exception {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps1 = con.prepareStatement("INSERT INTO shops (`npcid`) VALUES (?)");
        PreparedStatement ps2 = con.prepareStatement("INSERT INTO shopitems (shopid, itemid, price, position, max, expiretime, level) VALUES (?, ?, ?, ?, ?, ?, ?)");
        try {
            ps1.setInt(1, npcid);
            ps2.setInt(1, shopid);
            ps2.setInt(2, itemid);//거짓말 탐지기
            ps2.setInt(3, price);
            ps2.setInt(4, position);
            ps2.setInt(5, count);
            ps2.setInt(6, 0);
            ps2.setInt(7, 0);
            ps2.addBatch();
        } catch (Exception e) {
            e.printStackTrace();
            //System.out.println("Shopid : " + npcid);
            return;
        }
        ps1.executeBatch();
        ps2.executeBatch();
    }

    public static boolean isFancyGoodsShop(final int sid) {
        switch (sid) {
            case 2:
            case 6:
            case 9://헤네시스
            case 13://페리온
            case 16://엘리니아
            case 20://커닝시티
            case 23://갬굴광장
            case 24://사우나
            case 30://오르비스
            case 33://엘나스
            case 35://엘나스골짜기
            case 38://루디
            case 41://뭐지?
            case 42://시계탑깊은곳
            case 45://지구방위본부
            case 47://아쿠아리움
            case 49://아랫마을
            case 52://리프레
            case 58://무릉
            case 62://백초마을
            case 65://아리안트
            case 66://마가티아
            case 69://코크
            case 77://태국
            case 81://중국
            case 82://어디지 야시장인가봄
            case 85://세계여행어딘가
            case 9090000://묘묘
                return true;
        }
        return false;
    }

    public static boolean isShopNpc(final int sid) {
        switch (sid) {
            case 1301000://버섯의성
                return true;
        }
        return false;
    }

    public static int 상점추가(int npcid)throws Exception {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO shops (shopid ,npcid) VALUES (?, ?)");
            ps.setInt(1, npcid);
            ps.setInt(2, npcid);
            ps.executeUpdate();
            ps.close();
            System.err.println("상점을 추가 했습니다. npcid : " + npcid);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Shop Dupe Err..");
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception e) {
            }
        }
        return 1;
    }

    public static int 상점복사(int shop1, int shop2) {

        Connection con = null;
        PreparedStatement ps = null;
        PreparedStatement ps2 = null;
        ResultSet rs = null;
        ResultSet rs2 = null;

        ArrayList<Integer> shopitems3 = new ArrayList<Integer>();
        ArrayList<Integer> shopitems4 = new ArrayList<Integer>();
        ArrayList<Integer> shopitems5 = new ArrayList<Integer>();
        ArrayList<Integer> shopitems6 = new ArrayList<Integer>();
        ArrayList<Integer> shopitems7 = new ArrayList<Integer>();
        ArrayList<Integer> shopitems8 = new ArrayList<Integer>();
        ArrayList<Integer> shopitems9 = new ArrayList<Integer>();
        ArrayList<Integer> shopitems10 = new ArrayList<Integer>();
        ArrayList<Integer> shopitems11 = new ArrayList<Integer>();

        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM shops WHERE shopid = ?");
            ps.setInt(1, shop1);
            rs = ps.executeQuery();
            if (!rs.next()) {
                System.out.println("존재하는 않는 상점 엔피시로 추정됩니다. [상점 1]");
                return 1;
            }
            ps.close();
            rs.close();

            ps = con.prepareStatement("SELECT * FROM shops WHERE npcid = ?");
            ps.setInt(1, shop2);
            rs = ps.executeQuery();
            if (rs.next()) {
                System.out.println("이미 존재하는 상점 엔피시로 추정됩니다. [상점 2]");
                return 1;
            }
            ps.close();
            rs.close();

            ps = con.prepareStatement("SELECT * FROM shopitems WHERE shopid = ?");
            ps.setInt(1, shop1);
            rs = ps.executeQuery();
            while (rs.next()) {
                shopitems3.add(rs.getInt(3));
                shopitems4.add(rs.getInt(4));
                shopitems5.add(rs.getInt(5));
                shopitems6.add(rs.getInt(6));
                shopitems7.add(rs.getInt(7));
                shopitems8.add(rs.getInt(8));
                shopitems9.add(rs.getInt(9));
                shopitems10.add(rs.getInt(10));
                shopitems11.add(rs.getInt(11));
            }
            ps.close();
            rs.close();

            ps = con.prepareStatement("INSERT INTO shops (shopid ,npcid) VALUES (?, ?)");
            ps.setInt(1, shop2);
            ps.setInt(2, shop2);
            //int shopid2 = rs.getInt("shopid");
            ps.executeUpdate();
            ps.close();

            ps = con.prepareStatement("DELETE FROM shopitems WHERE shopid = ?");
            ps.setInt(1, shop2);
            ps.executeUpdate();
            ps.close();
            for (int i = 0; i < shopitems3.size(); i++) {
                ps = con.prepareStatement("INSERT INTO shopitems (shopid, itemid, price, position, reqitem, reqitemq, rank, max, expiretime, level) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                ps.setInt(1, shop2);
                ps.setInt(2, shopitems3.get(i));
                ps.setInt(3, shopitems4.get(i));
                ps.setInt(4, shopitems5.get(i));
                ps.setInt(5, shopitems6.get(i));
                ps.setInt(6, shopitems7.get(i));
                ps.setInt(7, shopitems8.get(i));
                ps.setInt(8, shopitems9.get(i));
                ps.setInt(9, shopitems10.get(i));
                ps.setInt(10, shopitems11.get(i));
                ps.executeUpdate();
                ps.close();
            }
            System.out.println("모든 작업을 완료했습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Shop Dupe Err..");
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
            } catch (Exception e) {
            }
        }
        return 1;
    }
}
