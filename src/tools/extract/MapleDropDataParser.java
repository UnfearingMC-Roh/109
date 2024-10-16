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
import server.life.MapleMonsterStats;
import server.maps.MapleReactorFactory;
import server.quest.MapleQuest;
import server.quest.MapleQuestAction;
import server.quest.MapleQuestAction.QuestItem;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author 티썬
 */
public class MapleDropDataParser {

    private static void addFrankenroid(List<int[]> datas) {
        // 프랑켄로이드, 화난 프랑켄로이드
        for (int i = 9300139; i <= 9300140; ++i) {
            for (int z = 0; z < 3; ++z) {
                datas.add(new int[]{i, 2000002, 1, 1, 0, 600000});
                datas.add(new int[]{i, 2000002, 1, 1, 0, 600000});
                datas.add(new int[]{i, 2000002, 1, 1, 0, 600000});
                datas.add(new int[]{i, 2000002, 1, 1, 0, 600000});
                datas.add(new int[]{i, 2000004, 1, 1, 0, 100000});
                datas.add(new int[]{i, 2000005, 1, 1, 0, 100000});
                datas.add(new int[]{i, 2000006, 1, 1, 0, 300000});
                datas.add(new int[]{i, 2000006, 1, 1, 0, 300000});
                datas.add(new int[]{i, 2000006, 1, 1, 0, 300000});
                datas.add(new int[]{i, 2000006, 1, 1, 0, 300000});

                datas.add(new int[]{i, 2001001, 1, 1, 0, 200000});
                datas.add(new int[]{i, 2020013, 1, 1, 0, 200000});
                datas.add(new int[]{i, 2020013, 1, 1, 0, 200000});
                datas.add(new int[]{i, 2001001, 1, 1, 0, 200000});
                datas.add(new int[]{i, 2020014, 1, 1, 0, 200000});
                datas.add(new int[]{i, 2020014, 1, 1, 0, 200000});
                datas.add(new int[]{i, 2001002, 1, 1, 0, 200000});
                datas.add(new int[]{i, 2001002, 1, 1, 0, 200000});
                datas.add(new int[]{i, 2020015, 1, 1, 0, 200000});
                datas.add(new int[]{i, 2020015, 1, 1, 0, 200000});
                datas.add(new int[]{i, 2022003, 1, 1, 0, 200000});
                datas.add(new int[]{i, 2022003, 1, 1, 0, 200000});
            }
            for (int iiz = 0; iiz < 8; ++iiz) {
                datas.add(new int[]{i, 0, 30, 49, 0, 999999});
            }
            //

            datas.add(new int[]{i, 2044601, 1, 1, 0, 10000});
            datas.add(new int[]{i, 2040707, 1, 1, 0, 10000});
            datas.add(new int[]{i, 2044401, 1, 1, 0, 10000});
            datas.add(new int[]{i, 2040504, 1, 1, 0, 10000});
            datas.add(new int[]{i, 2044501, 1, 1, 0, 10000});
            datas.add(new int[]{i, 2044001, 1, 1, 0, 10000});
            datas.add(new int[]{i, 2043701, 1, 1, 0, 10000});
            datas.add(new int[]{i, 2043001, 1, 1, 0, 10000});
            datas.add(new int[]{i, 2040004, 1, 1, 0, 10000});
            datas.add(new int[]{i, 2044701, 1, 1, 0, 10000});
            datas.add(new int[]{i, 2043801, 1, 1, 0, 10000});
            datas.add(new int[]{i, 2043301, 1, 1, 0, 10000});

            datas.add(new int[]{i, 1092030, 1, 1, 0, 10000});//35제 메이플
            datas.add(new int[]{i, 1302020, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1382009, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1452016, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1462014, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1472030, 1, 1, 0, 10000});

            datas.add(new int[]{i, 1302030, 1, 1, 0, 10000});//43제
            datas.add(new int[]{i, 1332025, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1382012, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1412011, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1422014, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1432012, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1442024, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1452022, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1462019, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1472032, 1, 1, 0, 10000});

            datas.add(new int[]{i, 1092045, 1, 1, 0, 10000});//64제
            datas.add(new int[]{i, 1092046, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1092047, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1302064, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1312032, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1322054, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1332055, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1332056, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1372034, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1382039, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1402039, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1412027, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1422029, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1432040, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1442051, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1452045, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1462040, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1472055, 1, 1, 0, 10000});

            datas.add(new int[]{i, 5060002, 1, 1, 0, 50000});
            datas.add(new int[]{i, 5060002, 1, 1, 0, 50000});
            datas.add(new int[]{i, 5060002, 1, 1, 0, 50000});

        }
    }

    private static void add(Integer itemid, List<MobDropEntry> newMobDrops, Entry<Integer, List<Integer>> mBookChild, boolean isBoss, boolean isRaidBoss) {
        if (itemid / 10000 == 206) {
            newMobDrops.add(new MobDropEntry(mBookChild.getKey(), itemid, 8000, 20, 30, 0)); //화살
        } else if (itemid / 10000 == 200) {
            for (int i = 0; i < (isBoss ? 5 : 1); ++i) {
                newMobDrops.add(new MobDropEntry(mBookChild.getKey(), itemid, isRaidBoss ? 700000 : isBoss ? 350000 : 1000, 1, 1, 0)); //물약
            }
        } else if (itemid / 1000 == 4004) {
            newMobDrops.add(new MobDropEntry(mBookChild.getKey(), itemid, 80, 1, 1, 0)); // 크리스탈 원석
        } else if (itemid / 1000 == 4000) {
            if (itemid == 4000451 || itemid == 4000456 || itemid == 4000446 || itemid == 4000448 || itemid == 4000458 || itemid == 4000453) {
                newMobDrops.add(new MobDropEntry(mBookChild.getKey(), itemid, 80000, 1, 1, 0)); // 전리품
            } else {
                newMobDrops.add(new MobDropEntry(mBookChild.getKey(), itemid, 400000, 1, 1, 0)); // 전리품
            }
        } else if (itemid / 10000 == 401) {
            newMobDrops.add(new MobDropEntry(mBookChild.getKey(), itemid, 1000, 1, 1, 0)); //광석 원석
        } else if (itemid / 1000 == 4020) {
            if (itemid == 4020009) {
                int chance = 8;
                switch (mBookChild.getKey()) {
                    case 8200001:
                    case 8200002:
                        chance = 8;
                        break;
                    case 8200003:
                        chance = 30;
                        break;
                    case 8200004:
                        chance = 50;
                        break;
                    case 8200005:
                    case 8200006:
                        chance = 90;
                        break;
                    case 8200007:
                    case 8200008:
                        chance = 130;
                        break;
                    case 8200009:
                    case 8200010:
                        chance = 190;
                        break;
                    case 8200011:
                        chance = 250;
                        break;
                    case 8200012:
                        chance = 400;
                        break;

                }
                newMobDrops.add(new MobDropEntry(mBookChild.getKey(), itemid, chance, 1, 1, 0)); //시간 조각
            } else {
                newMobDrops.add(new MobDropEntry(mBookChild.getKey(), itemid, 150, 1, 1, 0)); //보석 원석..
            }
        } else if (itemid / 10000 == 204) {
            newMobDrops.add(new MobDropEntry(mBookChild.getKey(), itemid, isRaidBoss ? 30000 : isBoss ? 9000 : 70, 1, 1, 0)); //주문서..
        } else if (itemid / 10000 == 202) {
            for (int i = 0; i < (isBoss ? 5 : 1); ++i) {
                newMobDrops.add(new MobDropEntry(mBookChild.getKey(), itemid, isRaidBoss ? 700000 : isBoss ? 350000 : 5000, 1, 1, 0)); //특수물약
            }
        } else if (itemid / 1000000 == 1) {
            int f = 20;
            MapleMonsterStats stats = MapleLifeFactory.getMonsterStats(mBookChild.getKey());
            int itemlevel = MapleItemInformationProvider.getInstance().getReqLevel(itemid);
            if (Math.abs(stats.getLevel() - itemlevel) <= 5) {
                f = 15 + (stats.getLevel() - itemlevel);
            } else if (stats.getLevel() - itemlevel < -5) {
                f = 5;
            } else if (stats.getLevel() - itemlevel < -10) {
                f = 3;
            } else if (stats.getLevel() - itemlevel > 5) {
                f = 40;
            }
            newMobDrops.add(new MobDropEntry(mBookChild.getKey(), itemid, isRaidBoss ? 100000 : isBoss ? 900 : f, 1, 1, 0)); //장비
        } else if (itemid / 1000 == 4007) {
            newMobDrops.add(new MobDropEntry(mBookChild.getKey(), itemid, 300, 1, 1, 0)); //가루
        } else if (itemid / 10000 == 400) {
            newMobDrops.add(new MobDropEntry(mBookChild.getKey(), itemid, 600, 1, 1, 0)); //기타
        } else if (itemid / 10000 == 229) {
            newMobDrops.add(new MobDropEntry(mBookChild.getKey(), itemid, isRaidBoss ? 110000 : isBoss ? 50000 : 6, 1, 1, 0)); //마북
        } else if (itemid / 10000 == 233 || itemid / 10000 == 207) {
            newMobDrops.add(new MobDropEntry(mBookChild.getKey(), itemid, isRaidBoss ? 110000 : isBoss ? 10000 : 30, 1, 1, 0)); //표창 불릿
        } else if (itemid / 10000 == 413) {
            newMobDrops.add(new MobDropEntry(mBookChild.getKey(), itemid, 100, 1, 1, 0)); //촉진제
        } else if (itemid / 10000 == 221) {
            newMobDrops.add(new MobDropEntry(mBookChild.getKey(), itemid, 999999, 1, 1, 0)); //변신물약
        } else if (itemid / 10000 == 301) {
            newMobDrops.add(new MobDropEntry(mBookChild.getKey(), itemid, isRaidBoss ? 110000 : isBoss ? 6000 : 6, 1, 1, 0)); //의자?;
        } else {
            System.err.println("Undefined code : " + itemid);
        }
    }

    public enum DropType {

        Mob, Reactor;
    }

    public static void main(String[] args) throws Exception {
        DatabaseConnection.init();
        File from = new File("imgs");
        System.setProperty("net.sf.odinms.wzpath", "wz");
        MapleDataProvider pro = MapleDataProviderFactory.getDataProvider(from);
        MapleData root = pro.getData("Reward.img");
        Connection con = DatabaseConnection.getConnection();
        Map<Integer, Integer> quests = new HashMap<Integer, Integer>();

        List<Integer> customMoneyMobAdd = new ArrayList<Integer>();
        List<MobDropEntry> mobdrops = new ArrayList<MobDropEntry>();
        List<ReactorDropEntry> reactordrops = new ArrayList<>();
        Map<Integer, List<Integer>> mBookRewards = new HashMap<Integer, List<Integer>>();

        MapleQuest.initQuests();
        MapleItemInformationProvider.getInstance().runItems();
        for (MapleQuest quest : MapleQuest.getAllInstances()) {
            for (MapleQuestAction act : quest.getCompleteActs()) {
                if (act.getItems() == null) {
                    continue;
                }
                for (QuestItem qitem : act.getItems()) {
                    if (qitem.count < 0 && MapleItemInformationProvider.getInstance().isQuestItem(qitem.itemid)) {
                        if (quests.containsKey(qitem.itemid)) {
                            System.err.println("Warning : Duplicate Quest Required Item. - ItemID : " + qitem.itemid + " Quest1 : " + quests.get(qitem.itemid) + ", Quest2 : " + quest.getId());
                        }
                        quests.put(qitem.itemid, quest.getId());
                    }
                }
            }
        }
        //K:QuestItems   V:QuestID
//        PreparedStatement select = con.prepareStatement("SELECT id,itemid,count FROM wz_questactitemdata");
//        ResultSet rs = select.executeQuery();
//        while (rs.next()) {
//            int itemid = rs.getInt("itemid");
//            int count = rs.getInt("count");
//            int qid = rs.getInt("id");
//            if (itemid / 10000 == 403 && count < 0) {
//                if (quests.containsKey(itemid)) {
//                    System.err.println("Warning : Duplicate Quest Required Item. - ItemID : " + itemid + " Quest1 : " + quests.get(itemid) + ", Quest2 : " + qid);
//                }
//                quests.put(itemid, qid);
//            }
//        }
//        select.close();
//        rs.close();
        System.out.println("Cached Quest Items : " + quests.size());

        long start = System.currentTimeMillis();
        System.out.println("Job Start");

        List<Integer> questdrops = new ArrayList<Integer>();

        PreparedStatement ps = con.prepareStatement("INSERT INTO `drop_data` (`dropperid`, `itemid`, `minimum_quantity`, maximum_quantity, questid, chance) VALUES (?, ?, ?, ?, ?, ?)");
        PreparedStatement ps2 = con.prepareStatement("INSERT INTO `reactordrops` (`reactorid`, `itemid`, `chance`, `questid`, `min`, `max`) VALUES (?, ?, ?, ?, ?, ?)");
        PreparedStatement del1 = con.prepareStatement("TRUNCATE `drop_data`");
        PreparedStatement del2 = con.prepareStatement("TRUNCATE `reactordrops`");
        del1.executeUpdate();
        del2.executeUpdate();
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        ii.runItems();
        ii.runEtc();
        for (MapleData reroot : root.getChildren()) {
            DropType type;
            int id = Integer.parseInt(reroot.getName().substring(1));
            if (reroot.getName().startsWith("m")) {
                if (MapleLifeFactory.getMonster(id) == null) {
                    System.out.println("Monster Id " + id + " is not exists.... Continue...");
                    continue;
                }
                /*if (MapleLifeFactory.getMonster(id).getStats().isBoss() == false) {
                 System.out.println("Monster Id " + id + " is not boss.... Continue...");
                 continue;
                 }*/
                type = DropType.Mob;
            } else {
                try {
                    MapleReactorFactory.getReactor(id);
                } catch (RuntimeException r) {
                    System.out.println("Reactor Id " + id + " is not exists.... Continue...");
                    continue;
                }
                type = DropType.Reactor;
            }

            for (MapleData content : reroot.getChildren()) {
                int itemid = MapleDataTool.getIntConvert("item", content, 0);

                if (!ii.itemExists(itemid) && itemid != 0) {
                    System.err.println("Item " + itemid + " does not exists.. Continue.");
                    continue;
                }

                if (itemid == 4000047) {
                    continue;
                }

                int money = MapleDataTool.getIntConvert("money", content, 0);
                int prob = (int) Math.round(Double.parseDouble(MapleDataTool.getString("prob", content).substring(4)) * 1000000);
                int min = MapleDataTool.getIntConvert("min", content, 1);
                int max = MapleDataTool.getIntConvert("max", content, 1);
                int quest = 0;
                if (quests.containsKey(itemid)) {
                    quest = quests.get(itemid);
                }
                if (type == DropType.Mob) {
                    if (!questdrops.contains(Integer.valueOf(itemid))) {
                        questdrops.add(Integer.valueOf(itemid));
                    }
                    if (itemid == 0) {
                        min = ((int) (money * 0.75));
                        max = (money);
                    }
                    mobdrops.add(new MobDropEntry(id, itemid, prob, min, max, quest));
                } else {
                    if (!questdrops.contains(Integer.valueOf(itemid))) {
                        questdrops.add(Integer.valueOf(itemid));
                    }
                    if (itemid == 0) {
                        min = ((int) (money * 0.75));
                        max = (money);
                    }
                    reactordrops.add(new ReactorDropEntry(id, itemid, prob, min, max, quest));
                }
            }
        }

        //Hardcode Drop Data
        List<int[]> datas = new ArrayList<int[]>();
        //mobid, itemid, min, max, quest, prob

        //리게이터
        datas.add(new int[]{3110100, 4031405, 1, 1, 4207, 10000});

        //WorldTrip
        //들개
        datas.add(new int[]{9410000, 0, 50, 75, 0, 700000});
        datas.add(new int[]{9410000, 1002096, 1, 1, 0, 350});
        datas.add(new int[]{9410000, 1050011, 1, 1, 0, 350});
        datas.add(new int[]{9410000, 1072011, 1, 1, 0, 350});
        datas.add(new int[]{9410000, 1072034, 1, 1, 0, 350});
        datas.add(new int[]{9410000, 1082020, 1, 1, 0, 350});
        datas.add(new int[]{9410000, 1092001, 1, 1, 0, 350});
        datas.add(new int[]{9410000, 1452005, 1, 1, 0, 350});
        datas.add(new int[]{9410000, 2000001, 1, 1, 0, 1000});
        datas.add(new int[]{9410000, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9410000, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9410000, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9410000, 4000198, 1, 1, 0, 400000});
        datas.add(new int[]{9410000, 4003004, 1, 1, 0, 600});
        datas.add(new int[]{9410000, 4020002, 1, 1, 0, 150});
        datas.add(new int[]{9410000, 4020005, 1, 1, 0, 150});

        //멋쟁이 들개
        datas.add(new int[]{9410001, 0, 50, 75, 0, 700000});
        datas.add(new int[]{9410001, 1002049, 1, 1, 0, 350});
        datas.add(new int[]{9410001, 1050003, 1, 1, 0, 350});
        datas.add(new int[]{9410001, 1072035, 1, 1, 0, 350});
        datas.add(new int[]{9410001, 1082017, 1, 1, 0, 350});
        datas.add(new int[]{9410001, 1472011, 1, 1, 0, 350});
        datas.add(new int[]{9410001, 2000001, 1, 1, 0, 1000});
        datas.add(new int[]{9410001, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9410001, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9410001, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9410001, 4000199, 1, 1, 0, 400000});
        datas.add(new int[]{9410001, 4003004, 1, 1, 0, 600});
        datas.add(new int[]{9410001, 4010000, 1, 1, 0, 1000});
        datas.add(new int[]{9410001, 4010005, 1, 1, 0, 1000});

        //험악한 들개
        datas.add(new int[]{9410002, 0, 100, 150, 0, 700000});
        datas.add(new int[]{9410002, 1002153, 1, 1, 0, 350});
        datas.add(new int[]{9410002, 1051039, 1, 1, 0, 350});
        datas.add(new int[]{9410002, 1072000, 1, 1, 0, 350});
        datas.add(new int[]{9410002, 1082066, 1, 1, 0, 350});
        datas.add(new int[]{9410002, 1432005, 1, 1, 0, 350});
        datas.add(new int[]{9410002, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9410002, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9410002, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9410002, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9410002, 4000200, 1, 1, 0, 400000});
        datas.add(new int[]{9410002, 4003004, 1, 1, 0, 600});
        datas.add(new int[]{9410002, 4020000, 1, 1, 0, 150});
        datas.add(new int[]{9410002, 4020006, 1, 1, 0, 150});

        //광대 원숭이
        datas.add(new int[]{9410003, 0, 100, 150, 0, 700000});
        datas.add(new int[]{9410003, 1002170, 1, 1, 0, 350});
        datas.add(new int[]{9410003, 1040083, 1, 1, 0, 350});
        datas.add(new int[]{9410003, 1041075, 1, 1, 0, 350});
        datas.add(new int[]{9410003, 1060072, 1, 1, 0, 350});
        datas.add(new int[]{9410003, 1061070, 1, 1, 0, 350});
        datas.add(new int[]{9410003, 1072143, 1, 1, 0, 350});
        datas.add(new int[]{9410003, 1082024, 1, 1, 0, 350});
        datas.add(new int[]{9410003, 1332020, 1, 1, 0, 350});
        datas.add(new int[]{9410003, 1442014, 1, 1, 0, 350});
        datas.add(new int[]{9410003, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9410003, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9410003, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9410003, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9410003, 4000201, 1, 1, 0, 400000});
        datas.add(new int[]{9410003, 4003004, 1, 1, 0, 600});
        datas.add(new int[]{9410003, 4010006, 1, 1, 0, 1000});
        datas.add(new int[]{9410003, 4020007, 1, 1, 0, 150});
        datas.add(new int[]{9410003, 4031296, 1, 1, 4010, 50000});

        //폭주족 원숭이
        datas.add(new int[]{9410004, 0, 125, 185, 0, 700000});
        datas.add(new int[]{9410004, 1002208, 1, 1, 0, 350});
        datas.add(new int[]{9410004, 1041088, 1, 1, 0, 350});
        datas.add(new int[]{9410004, 1061087, 1, 1, 0, 350});
        datas.add(new int[]{9410004, 1072124, 1, 1, 0, 350});
        datas.add(new int[]{9410004, 1082080, 1, 1, 0, 350});
        datas.add(new int[]{9410004, 1382006, 1, 1, 0, 350});
        datas.add(new int[]{9410004, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9410004, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9410004, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9410004, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9410004, 4000202, 1, 1, 0, 400000});
        datas.add(new int[]{9410004, 4003004, 1, 1, 0, 600});
        datas.add(new int[]{9410004, 4010001, 1, 1, 0, 1000});
        datas.add(new int[]{9410004, 4010002, 1, 1, 0, 1000});
        datas.add(new int[]{9410004, 4031296, 1, 1, 4010, 50000});

        //레드 버블티
        datas.add(new int[]{9410005, 0, 75, 100, 0, 700000});
        datas.add(new int[]{9410005, 1002151, 1, 1, 0, 350});
        datas.add(new int[]{9410005, 1032012, 1, 1, 0, 350});
        datas.add(new int[]{9410005, 1041076, 1, 1, 0, 350});
        datas.add(new int[]{9410005, 1061071, 1, 1, 0, 350});
        datas.add(new int[]{9410005, 1302004, 1, 1, 0, 350});
        datas.add(new int[]{9410005, 1302010, 1, 1, 0, 350});
        datas.add(new int[]{9410005, 1302016, 1, 1, 0, 350});
        datas.add(new int[]{9410005, 1312005, 1, 1, 0, 350});
        datas.add(new int[]{9410005, 2040004, 1, 1, 0, 70});
        datas.add(new int[]{9410005, 2040501, 1, 1, 0, 70});
        datas.add(new int[]{9410005, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9410005, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9410005, 4000254, 1, 1, 0, 400000});
        datas.add(new int[]{9410005, 4010000, 1, 1, 0, 1000});
        datas.add(new int[]{9410005, 4010006, 1, 1, 0, 1000});
        datas.add(new int[]{9410005, 4020000, 1, 1, 0, 150});

        //옐로우 버블티
        datas.add(new int[]{9410006, 0, 75, 100, 0, 700000});
        datas.add(new int[]{9410006, 1050011, 1, 1, 0, 350});
        datas.add(new int[]{9410006, 1072127, 1, 1, 0, 350});
        datas.add(new int[]{9410006, 1302017, 1, 1, 0, 350});
        datas.add(new int[]{9410006, 1332020, 1, 1, 0, 350});
        datas.add(new int[]{9410006, 1402010, 1, 1, 0, 350});
        datas.add(new int[]{9410006, 2040401, 1, 1, 0, 70});
        datas.add(new int[]{9410006, 2044701, 1, 1, 0, 70});
        datas.add(new int[]{9410006, 2048004, 1, 1, 0, 70});
        datas.add(new int[]{9410006, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9410006, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9410006, 2070002, 1, 1, 0, 40});
        datas.add(new int[]{9410006, 4000255, 1, 1, 0, 400000});
        datas.add(new int[]{9410006, 4006001, 1, 1, 0, 600});
        datas.add(new int[]{9410006, 4010003, 1, 1, 0, 1000});
        datas.add(new int[]{9410006, 4010005, 1, 1, 0, 1000});
        datas.add(new int[]{9410006, 4020004, 1, 1, 0, 150});

        //그린 버블티
        datas.add(new int[]{9410007, 0, 75, 100, 0, 700000});
        datas.add(new int[]{9410007, 1002038, 1, 1, 0, 350});
        datas.add(new int[]{9410007, 1002098, 1, 1, 0, 350});
        datas.add(new int[]{9410007, 1002136, 1, 1, 0, 350});
        datas.add(new int[]{9410007, 1002172, 1, 1, 0, 350});
        datas.add(new int[]{9410007, 1002182, 1, 1, 0, 350});
        datas.add(new int[]{9410007, 1040083, 1, 1, 0, 350});
        datas.add(new int[]{9410007, 1041050, 1, 1, 0, 350});
        datas.add(new int[]{9410007, 1060072, 1, 1, 0, 350});
        datas.add(new int[]{9410007, 1061046, 1, 1, 0, 350});
        datas.add(new int[]{9410007, 1072103, 1, 1, 0, 350});
        datas.add(new int[]{9410007, 1472015, 1, 1, 0, 350});
        datas.add(new int[]{9410007, 2040704, 1, 1, 0, 70});
        datas.add(new int[]{9410007, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9410007, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9410007, 2070008, 1, 1, 0, 40});
        datas.add(new int[]{9410007, 4000256, 1, 1, 0, 400000});
        datas.add(new int[]{9410007, 4010002, 1, 1, 0, 1000});

        //예티 인형자판기
        datas.add(new int[]{9410008, 0, 135, 200, 0, 700000});
        datas.add(new int[]{9410008, 1002135, 1, 1, 0, 350});
        datas.add(new int[]{9410008, 1002141, 1, 1, 0, 350});
        datas.add(new int[]{9410008, 1002169, 1, 1, 0, 350});
        datas.add(new int[]{9410008, 1032018, 1, 1, 0, 350});
        datas.add(new int[]{9410008, 1041086, 1, 1, 0, 350});
        datas.add(new int[]{9410008, 1061085, 1, 1, 0, 350});
        datas.add(new int[]{9410008, 1082084, 1, 1, 0, 350});
        datas.add(new int[]{9410008, 1382001, 1, 1, 0, 350});
        datas.add(new int[]{9410008, 1402002, 1, 1, 0, 350});
        datas.add(new int[]{9410008, 1402003, 1, 1, 0, 350});
        datas.add(new int[]{9410008, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9410008, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9410008, 2040804, 1, 1, 0, 70});
        datas.add(new int[]{9410008, 2043201, 1, 1, 0, 70});
        datas.add(new int[]{9410008, 4020007, 1, 1, 0, 150});
        datas.add(new int[]{9410008, 4031352, 1, 1, 0, 80000});

        //예티 인형
        datas.add(new int[]{9410009, 0, 100, 150, 0, 700000});
        datas.add(new int[]{9410009, 4000257, 1, 1, 0, 400000});

        //주니어페페 인형자판기
        datas.add(new int[]{9410010, 0, 135, 200, 0, 700000});
        datas.add(new int[]{9410010, 1032008, 1, 1, 0, 350});
        datas.add(new int[]{9410010, 1032018, 1, 1, 0, 350});
        datas.add(new int[]{9410010, 1040085, 1, 1, 0, 350});
        datas.add(new int[]{9410010, 1072090, 1, 1, 0, 350});
        datas.add(new int[]{9410010, 1332016, 1, 1, 0, 350});
        datas.add(new int[]{9410010, 1372007, 1, 1, 0, 350});
        datas.add(new int[]{9410010, 1412004, 1, 1, 0, 350});
        datas.add(new int[]{9410010, 1432004, 1, 1, 0, 350});
        datas.add(new int[]{9410010, 1462004, 1, 1, 0, 350});
        datas.add(new int[]{9410010, 1462006, 1, 1, 0, 350});
        datas.add(new int[]{9410010, 1472013, 1, 1, 0, 350});
        datas.add(new int[]{9410010, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9410010, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9410010, 2040516, 1, 1, 0, 70});

        //주니어페페 인형
        datas.add(new int[]{9410011, 0, 100, 150, 0, 700000});
        datas.add(new int[]{9410011, 4000258, 1, 1, 0, 400000});

        //인형뽑기 기계
        datas.add(new int[]{9410013, 0, 175, 250, 0, 700000});
        datas.add(new int[]{9410013, 1032021, 1, 1, 0, 350});
        datas.add(new int[]{9410013, 1051044, 1, 1, 0, 350});
        datas.add(new int[]{9410013, 1051045, 1, 1, 0, 350});
        datas.add(new int[]{9410013, 1092002, 1, 1, 0, 350});
        datas.add(new int[]{9410013, 1102016, 1, 1, 0, 350});
        datas.add(new int[]{9410013, 1322016, 1, 1, 0, 350});
        datas.add(new int[]{9410013, 1332003, 1, 1, 0, 350});
        datas.add(new int[]{9410013, 1332019, 1, 1, 0, 350});
        datas.add(new int[]{9410013, 1412007, 1, 1, 0, 350});
        datas.add(new int[]{9410013, 1472022, 1, 1, 0, 350});
        datas.add(new int[]{9410013, 2000004, 1, 1, 0, 1000});
        datas.add(new int[]{9410013, 2002003, 1, 1, 0, 1000});
        datas.add(new int[]{9410013, 2043801, 1, 1, 0, 70});
        datas.add(new int[]{9410013, 2044601, 1, 1, 0, 70});
        datas.add(new int[]{9410013, 2070002, 1, 1, 0, 40});
        datas.add(new int[]{9410013, 4000259, 1, 1, 0, 400000});
        datas.add(new int[]{9410013, 4006001, 1, 1, 0, 600});

        //포장마차
        datas.add(new int[]{9410015, 0, 3500, 5000, 0, 700000});
        datas.add(new int[]{9410015, 2000004, 3, 6, 0, 350000});
        datas.add(new int[]{9410015, 2000005, 1, 5, 0, 350000});
        datas.add(new int[]{9410015, 4031354, 1, 1, 4013, 1000000});

        //두꺼비
        datas.add(new int[]{9420000, 0, 50, 75, 0, 700000});
        datas.add(new int[]{9420000, 1002164, 1, 1, 0, 350});
        datas.add(new int[]{9420000, 1032010, 1, 1, 0, 350});
        datas.add(new int[]{9420000, 1040062, 1, 1, 0, 350});
        datas.add(new int[]{9420000, 1082042, 1, 1, 0, 350});
        datas.add(new int[]{9420000, 1092018, 1, 1, 0, 350});
        datas.add(new int[]{9420000, 1302004, 1, 1, 0, 350});
        datas.add(new int[]{9420000, 1432002, 1, 1, 0, 350});
        datas.add(new int[]{9420000, 1462000, 1, 1, 0, 350});
        datas.add(new int[]{9420000, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9420000, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9420000, 2040601, 1, 1, 0, 70});
        datas.add(new int[]{9420000, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9420000, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9420000, 2070002, 1, 1, 0, 40});
        datas.add(new int[]{9420000, 4000246, 1, 1, 0, 400000});
        datas.add(new int[]{9420000, 4010001, 1, 1, 0, 1000});
        datas.add(new int[]{9420000, 4010002, 1, 1, 0, 1000});
        datas.add(new int[]{9420000, 4131010, 1, 1, 0, 100});

        //개구리
        datas.add(new int[]{9420001, 0, 30, 50, 0, 700000});
        datas.add(new int[]{9420001, 1002019, 1, 1, 0, 350});
        datas.add(new int[]{9420001, 1060002, 1, 1, 0, 350});
        datas.add(new int[]{9420001, 1061014, 1, 1, 0, 350});
        datas.add(new int[]{9420001, 1072023, 1, 1, 0, 350});
        datas.add(new int[]{9420001, 1332000, 1, 1, 0, 350});
        datas.add(new int[]{9420001, 1402001, 1, 1, 0, 350});
        datas.add(new int[]{9420001, 1412001, 1, 1, 0, 350});
        datas.add(new int[]{9420001, 1432001, 1, 1, 0, 350});
        datas.add(new int[]{9420001, 1442012, 1, 1, 0, 350});
        datas.add(new int[]{9420001, 2000000, 1, 1, 0, 1000});
        datas.add(new int[]{9420001, 2040601, 1, 1, 0, 70});
        datas.add(new int[]{9420001, 2041010, 1, 1, 0, 70});
        datas.add(new int[]{9420001, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9420001, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9420001, 4000247, 1, 1, 0, 400000});
        datas.add(new int[]{9420001, 4010005, 1, 1, 0, 1000});
        datas.add(new int[]{9420001, 4020005, 1, 1, 0, 150});

        //구렁이
        datas.add(new int[]{9420002, 0, 150, 200, 0, 700000});
        datas.add(new int[]{9420002, 1002100, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1002218, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1002246, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1041080, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1041094, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1050056, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1051052, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1061079, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1061093, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1072155, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1072161, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1072164, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1082067, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1082087, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1082088, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1092011, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1102018, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1332017, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1472020, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1472023, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1472025, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 2000004, 1, 1, 0, 1000});
        datas.add(new int[]{9420002, 2000006, 1, 1, 0, 1000});
        datas.add(new int[]{9420002, 4000248, 1, 1, 0, 400000});
        datas.add(new int[]{9420002, 4000249, 1, 1, 0, 400000});
        datas.add(new int[]{9420002, 4010004, 1, 1, 0, 1000});
        datas.add(new int[]{9420002, 4010005, 1, 1, 0, 1000});
        datas.add(new int[]{9420002, 4020000, 1, 1, 0, 150});

        //빨간 도마뱀
        datas.add(new int[]{9420003, 0, 100, 150, 0, 700000});
        datas.add(new int[]{9420003, 1002025, 1, 1, 0, 350});
        datas.add(new int[]{9420003, 1002093, 1, 1, 0, 350});
        datas.add(new int[]{9420003, 1002185, 1, 1, 0, 350});
        datas.add(new int[]{9420003, 1050038, 1, 1, 0, 350});
        datas.add(new int[]{9420003, 1072108, 1, 1, 0, 350});
        datas.add(new int[]{9420003, 1072116, 1, 1, 0, 350});
        datas.add(new int[]{9420003, 1072120, 1, 1, 0, 350});
        datas.add(new int[]{9420003, 1072126, 1, 1, 0, 350});
        datas.add(new int[]{9420003, 1082072, 1, 1, 0, 350});
        datas.add(new int[]{9420003, 1092014, 1, 1, 0, 350});
        datas.add(new int[]{9420003, 1302009, 1, 1, 0, 350});
        datas.add(new int[]{9420003, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9420003, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9420003, 2040001, 1, 1, 0, 70});
        datas.add(new int[]{9420003, 2040704, 1, 1, 0, 70});
        datas.add(new int[]{9420003, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9420003, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9420003, 4000251, 1, 1, 0, 400000});
        datas.add(new int[]{9420003, 4010003, 1, 1, 0, 1000});

        //노란 도마뱀
        datas.add(new int[]{9420004, 0, 75, 100, 0, 700000});
        datas.add(new int[]{9420004, 1002034, 1, 1, 0, 350});
        datas.add(new int[]{9420004, 1002151, 1, 1, 0, 350});
        datas.add(new int[]{9420004, 1032000, 1, 1, 0, 350});
        datas.add(new int[]{9420004, 1040059, 1, 1, 0, 350});
        datas.add(new int[]{9420004, 1040079, 1, 1, 0, 350});
        datas.add(new int[]{9420004, 1060045, 1, 1, 0, 350});
        datas.add(new int[]{9420004, 1060069, 1, 1, 0, 350});
        datas.add(new int[]{9420004, 1061054, 1, 1, 0, 350});
        datas.add(new int[]{9420004, 1382017, 1, 1, 0, 350});
        datas.add(new int[]{9420004, 2000001, 1, 1, 0, 1000});
        datas.add(new int[]{9420004, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9420004, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9420004, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9420004, 4000021, 1, 1, 0, 400000});
        datas.add(new int[]{9420004, 4000250, 1, 1, 0, 400000});
        datas.add(new int[]{9420004, 4010002, 1, 1, 0, 1000});
        datas.add(new int[]{9420004, 4010003, 1, 1, 0, 1000});
        datas.add(new int[]{9420004, 4031388, 1, 1, 0, 500000});

        //흰 닭
        datas.add(new int[]{9420005, 0, 40, 65, 0, 700000});
        datas.add(new int[]{9420005, 1040022, 1, 1, 0, 350});
        datas.add(new int[]{9420005, 1040074, 1, 1, 0, 350});
        datas.add(new int[]{9420005, 1041032, 1, 1, 0, 350});
        datas.add(new int[]{9420005, 1060031, 1, 1, 0, 350});
        datas.add(new int[]{9420005, 1060063, 1, 1, 0, 350});
        datas.add(new int[]{9420005, 1092007, 1, 1, 0, 350});
        datas.add(new int[]{9420005, 1322000, 1, 1, 0, 350});
        datas.add(new int[]{9420005, 1372001, 1, 1, 0, 350});
        datas.add(new int[]{9420005, 1402009, 1, 1, 0, 350});
        datas.add(new int[]{9420005, 1432000, 1, 1, 0, 350});
        datas.add(new int[]{9420005, 2000000, 1, 1, 0, 1000});
        datas.add(new int[]{9420005, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9420005, 2043101, 1, 1, 0, 70});
        datas.add(new int[]{9420005, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9420005, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9420005, 4000252, 1, 1, 0, 400000});
        datas.add(new int[]{9420005, 4000253, 1, 1, 0, 400000});
        datas.add(new int[]{9420005, 4020003, 1, 1, 0, 150});
        datas.add(new int[]{9420005, 4020004, 1, 1, 0, 150});

        //닭
        datas.add(new int[]{9600001, 0, 35, 50, 0, 700000});
        datas.add(new int[]{9600001, 1002051, 1, 1, 0, 350});
        datas.add(new int[]{9600001, 1041030, 1, 1, 0, 350});
        datas.add(new int[]{9600001, 1061027, 1, 1, 0, 350});
        datas.add(new int[]{9600001, 1072062, 1, 1, 0, 350});
        datas.add(new int[]{9600001, 1082016, 1, 1, 0, 350});
        datas.add(new int[]{9600001, 1312003, 1, 1, 0, 350});
        datas.add(new int[]{9600001, 2000001, 1, 1, 0, 1000});
        datas.add(new int[]{9600001, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9600001, 2002002, 1, 1, 0, 1000});
        datas.add(new int[]{9600001, 2040401, 1, 1, 0, 70});
        datas.add(new int[]{9600001, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9600001, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9600001, 4000187, 1, 1, 0, 400000});
        datas.add(new int[]{9600001, 4003004, 1, 1, 0, 600});
        datas.add(new int[]{9600001, 4020000, 1, 1, 0, 150});
        datas.add(new int[]{9600001, 4020001, 1, 1, 0, 150});

        //오리
        datas.add(new int[]{9600002, 0, 40, 55, 0, 700000});
        datas.add(new int[]{9600002, 1002104, 1, 1, 0, 350});
        datas.add(new int[]{9600002, 1041028, 1, 1, 0, 350});
        datas.add(new int[]{9600002, 1061026, 1, 1, 0, 350});
        datas.add(new int[]{9600002, 1072051, 1, 1, 0, 350});
        datas.add(new int[]{9600002, 1082037, 1, 1, 0, 350});
        datas.add(new int[]{9600002, 1302002, 1, 1, 0, 350});
        datas.add(new int[]{9600002, 1442001, 1, 1, 0, 350});
        datas.add(new int[]{9600002, 2000001, 1, 1, 0, 1000});
        datas.add(new int[]{9600002, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9600002, 2002003, 1, 1, 0, 1000});
        datas.add(new int[]{9600002, 2040601, 1, 1, 0, 70});
        datas.add(new int[]{9600002, 2040801, 1, 1, 0, 70});
        datas.add(new int[]{9600002, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9600002, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9600002, 4000188, 1, 1, 0, 400000});
        datas.add(new int[]{9600002, 4003004, 1, 1, 0, 600});
        datas.add(new int[]{9600002, 4010000, 1, 1, 0, 1000});
        datas.add(new int[]{9600002, 4020002, 1, 1, 0, 150});

        //양
        datas.add(new int[]{9600003, 0, 50, 75, 0, 700000});
        datas.add(new int[]{9600003, 1002119, 1, 1, 0, 350});
        datas.add(new int[]{9600003, 1040050, 1, 1, 0, 350});
        datas.add(new int[]{9600003, 1060039, 1, 1, 0, 350});
        datas.add(new int[]{9600003, 1072073, 1, 1, 0, 350});
        datas.add(new int[]{9600003, 1082006, 1, 1, 0, 350});
        datas.add(new int[]{9600003, 1322003, 1, 1, 0, 350});
        datas.add(new int[]{9600003, 1432002, 1, 1, 0, 350});
        datas.add(new int[]{9600003, 2000001, 1, 1, 0, 1000});
        datas.add(new int[]{9600003, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9600003, 2041007, 1, 1, 0, 70});
        datas.add(new int[]{9600003, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9600003, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9600003, 2070002, 1, 1, 0, 40});
        datas.add(new int[]{9600003, 4000021, 1, 1, 0, 400000});
        datas.add(new int[]{9600003, 4000189, 1, 1, 0, 400000});
        datas.add(new int[]{9600003, 4003004, 1, 1, 0, 600});
        datas.add(new int[]{9600003, 4020005, 1, 1, 0, 150});
        datas.add(new int[]{9600003, 4020006, 1, 1, 0, 150});

        //염소
        datas.add(new int[]{9600004, 0, 65, 85, 0, 700000});
        datas.add(new int[]{9600004, 1002150, 1, 1, 0, 350});
        datas.add(new int[]{9600004, 1051012, 1, 1, 0, 350});
        datas.add(new int[]{9600004, 1072102, 1, 1, 0, 350});
        datas.add(new int[]{9600004, 1082051, 1, 1, 0, 350});
        datas.add(new int[]{9600004, 1372001, 1, 1, 0, 350});
        datas.add(new int[]{9600004, 1422008, 1, 1, 0, 350});
        datas.add(new int[]{9600004, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9600004, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9600004, 2002004, 1, 1, 0, 1000});
        datas.add(new int[]{9600004, 2041022, 1, 1, 0, 70});
        datas.add(new int[]{9600004, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9600004, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9600004, 4000021, 1, 1, 0, 400000});
        datas.add(new int[]{9600004, 4000190, 1, 1, 0, 400000});
        datas.add(new int[]{9600004, 4003004, 1, 1, 0, 600});
        datas.add(new int[]{9600004, 4010001, 1, 1, 0, 1000});
        datas.add(new int[]{9600004, 4010002, 1, 1, 0, 1000});

        //흑염소
        datas.add(new int[]{9600005, 0, 85, 110, 0, 700000});
        datas.add(new int[]{9600005, 1002023, 1, 1, 0, 350});
        datas.add(new int[]{9600005, 1032004, 1, 1, 0, 350});
        datas.add(new int[]{9600005, 1051025, 1, 1, 0, 350});
        datas.add(new int[]{9600005, 1072108, 1, 1, 0, 350});
        datas.add(new int[]{9600005, 1082071, 1, 1, 0, 350});
        datas.add(new int[]{9600005, 1412005, 1, 1, 0, 350});
        datas.add(new int[]{9600005, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9600005, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9600005, 2041001, 1, 1, 0, 70});
        datas.add(new int[]{9600005, 2043201, 1, 1, 0, 70});
        datas.add(new int[]{9600005, 2044501, 1, 1, 0, 70});
        datas.add(new int[]{9600005, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9600005, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9600005, 4000021, 1, 1, 0, 400000});
        datas.add(new int[]{9600005, 4000191, 1, 1, 0, 400000});
        datas.add(new int[]{9600005, 4003004, 1, 1, 0, 600});
        datas.add(new int[]{9600005, 4010003, 1, 1, 0, 1000});
        datas.add(new int[]{9600005, 4020003, 1, 1, 0, 150});

        //소
        datas.add(new int[]{9600006, 0, 70, 95, 0, 700000});
        datas.add(new int[]{9600006, 1002037, 1, 1, 0, 350});
        datas.add(new int[]{9600006, 1041068, 1, 1, 0, 350});
        datas.add(new int[]{9600006, 1061063, 1, 1, 0, 350});
        datas.add(new int[]{9600006, 1072064, 1, 1, 0, 350});
        datas.add(new int[]{9600006, 1082075, 1, 1, 0, 350});
        datas.add(new int[]{9600006, 1382018, 1, 1, 0, 350});
        datas.add(new int[]{9600006, 1402010, 1, 1, 0, 350});
        datas.add(new int[]{9600006, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9600006, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9600006, 2002005, 1, 1, 0, 1000});
        datas.add(new int[]{9600006, 2040901, 1, 1, 0, 70});
        datas.add(new int[]{9600006, 2044301, 1, 1, 0, 70});
        datas.add(new int[]{9600006, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9600006, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9600006, 4000021, 1, 1, 0, 400000});
        datas.add(new int[]{9600006, 4000192, 1, 1, 0, 400000});
        datas.add(new int[]{9600006, 4003004, 1, 1, 0, 600});
        datas.add(new int[]{9600006, 4020003, 1, 1, 0, 150});
        datas.add(new int[]{9600006, 4020004, 1, 1, 0, 150});

        //쟁기소
        datas.add(new int[]{9600007, 0, 85, 110, 0, 700000});
        datas.add(new int[]{9600007, 1002163, 1, 1, 0, 350});
        datas.add(new int[]{9600007, 1032008, 1, 1, 0, 350});
        datas.add(new int[]{9600007, 1051009, 1, 1, 0, 350});
        datas.add(new int[]{9600007, 1072116, 1, 1, 0, 350});
        datas.add(new int[]{9600007, 1082023, 1, 1, 0, 350});
        datas.add(new int[]{9600007, 1332014, 1, 1, 0, 350});
        datas.add(new int[]{9600007, 1442015, 1, 1, 0, 350});
        datas.add(new int[]{9600007, 1452007, 1, 1, 0, 350});
        datas.add(new int[]{9600007, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9600007, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9600007, 2040704, 1, 1, 0, 70});
        datas.add(new int[]{9600007, 2043801, 1, 1, 0, 70});
        datas.add(new int[]{9600007, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9600007, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9600007, 4000021, 1, 1, 0, 400000});
        datas.add(new int[]{9600007, 4000193, 1, 1, 0, 400000});
        datas.add(new int[]{9600007, 4003004, 1, 1, 0, 600});
        datas.add(new int[]{9600007, 4010006, 1, 1, 0, 1000});
        datas.add(new int[]{9600007, 4020007, 1, 1, 0, 150});

        //검은 양
        datas.add(new int[]{9600008, 0, 80, 105, 0, 700000});
        datas.add(new int[]{9600008, 1002175, 1, 1, 0, 350});
        datas.add(new int[]{9600008, 1041086, 1, 1, 0, 350});
        datas.add(new int[]{9600008, 1061085, 1, 1, 0, 350});
        datas.add(new int[]{9600008, 1072120, 1, 1, 0, 350});
        datas.add(new int[]{9600008, 1082062, 1, 1, 0, 350});
        datas.add(new int[]{9600008, 1312006, 1, 1, 0, 350});
        datas.add(new int[]{9600008, 1472016, 1, 1, 0, 350});
        datas.add(new int[]{9600008, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9600008, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9600008, 2040707, 1, 1, 0, 70});
        datas.add(new int[]{9600008, 2043301, 1, 1, 0, 70});
        datas.add(new int[]{9600008, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9600008, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9600008, 2070002, 1, 1, 0, 40});
        datas.add(new int[]{9600008, 4000021, 1, 1, 0, 400000});
        datas.add(new int[]{9600008, 4000194, 1, 1, 0, 400000});
        datas.add(new int[]{9600008, 4003004, 1, 1, 0, 600});
        datas.add(new int[]{9600008, 4010004, 1, 1, 0, 1000});
        datas.add(new int[]{9600008, 4020000, 1, 1, 0, 150});

        //대왕지네
        datas.add(new int[]{9600009, 0, 1000, 1500, 0, 700000});
        datas.add(new int[]{9600009, 4031227, 1, 1, 4103, 1000000});
        datas.add(new int[]{9600010, 0, 1000, 1500, 0, 700000});
        datas.add(new int[]{9600010, 4031227, 1, 1, 4103, 1000000});

        //CokeTown
        //코-크 돼지
        datas.add(new int[]{9500143, 0, 75, 100, 0, 700000});
        datas.add(new int[]{9500143, 1002152, 1, 1, 0, 350});
        datas.add(new int[]{9500143, 1002164, 1, 1, 0, 350});
        datas.add(new int[]{9500143, 1002183, 1, 1, 0, 350});
        datas.add(new int[]{9500143, 1040029, 1, 1, 0, 350});
        datas.add(new int[]{9500143, 1040072, 1, 1, 0, 350});
        datas.add(new int[]{9500143, 1051011, 1, 1, 0, 350});
        datas.add(new int[]{9500143, 1060020, 1, 1, 0, 350});
        datas.add(new int[]{9500143, 1060061, 1, 1, 0, 350});
        datas.add(new int[]{9500143, 1092008, 1, 1, 0, 350});
        datas.add(new int[]{9500143, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9500143, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9500143, 2022075, 1, 1, 0, 5000});
        datas.add(new int[]{9500143, 2043200, 1, 1, 0, 70});
        datas.add(new int[]{9500143, 2043300, 1, 1, 0, 70});
        datas.add(new int[]{9500143, 2043700, 1, 1, 0, 70});
        datas.add(new int[]{9500143, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9500143, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9500143, 4000210, 1, 1, 0, 400000});
        datas.add(new int[]{9500143, 4006001, 1, 1, 0, 600});
        datas.add(new int[]{9500143, 4010000, 1, 1, 0, 1000});
        datas.add(new int[]{9500143, 4020006, 1, 1, 0, 150});
        datas.add(new int[]{9500143, 4030012, 1, 1, 0, 15000});

        //코-크 달팽이
        datas.add(new int[]{9500144, 0, 20, 30, 0, 700000});
        datas.add(new int[]{9500144, 1002001, 1, 1, 0, 350});
        datas.add(new int[]{9500144, 1002043, 1, 1, 0, 350});
        datas.add(new int[]{9500144, 1002132, 1, 1, 0, 350});
        datas.add(new int[]{9500144, 1041018, 1, 1, 0, 350});
        datas.add(new int[]{9500144, 1061013, 1, 1, 0, 350});
        datas.add(new int[]{9500144, 1092003, 1, 1, 0, 350});
        datas.add(new int[]{9500144, 2000000, 1, 1, 0, 1000});
        datas.add(new int[]{9500144, 2022075, 1, 1, 0, 5000});
        datas.add(new int[]{9500144, 2040501, 1, 1, 0, 70});
        datas.add(new int[]{9500144, 2040705, 1, 1, 0, 70});
        datas.add(new int[]{9500144, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9500144, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9500144, 4000214, 1, 1, 0, 400000});
        datas.add(new int[]{9500144, 4010001, 1, 1, 0, 1000});
        datas.add(new int[]{9500144, 4010003, 1, 1, 0, 1000});
        datas.add(new int[]{9500144, 4030009, 1, 1, 0, 15000});
        datas.add(new int[]{9500144, 4030012, 1, 1, 0, 15000});

        //코-크 씰
        datas.add(new int[]{9500145, 0, 115, 165, 0, 700000});
        datas.add(new int[]{9500145, 1002048, 1, 1, 0, 350});
        datas.add(new int[]{9500145, 1002176, 1, 1, 0, 350});
        datas.add(new int[]{9500145, 1040082, 1, 1, 0, 350});
        datas.add(new int[]{9500145, 1041078, 1, 1, 0, 350});
        datas.add(new int[]{9500145, 1060071, 1, 1, 0, 350});
        datas.add(new int[]{9500145, 1061077, 1, 1, 0, 350});
        datas.add(new int[]{9500145, 1372012, 1, 1, 0, 350});
        datas.add(new int[]{9500145, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9500145, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9500145, 2002005, 1, 1, 0, 1000});
        datas.add(new int[]{9500145, 2022075, 1, 1, 0, 5000});
        datas.add(new int[]{9500145, 2041023, 1, 1, 0, 70});
        datas.add(new int[]{9500145, 2043002, 1, 1, 0, 70});
        datas.add(new int[]{9500145, 2043102, 1, 1, 0, 70});
        datas.add(new int[]{9500145, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9500145, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9500145, 4000211, 1, 1, 0, 400000});
        datas.add(new int[]{9500145, 4010001, 1, 1, 0, 1000});
        datas.add(new int[]{9500145, 4010005, 1, 1, 0, 1000});
        datas.add(new int[]{9500145, 4020002, 1, 1, 0, 150});
        datas.add(new int[]{9500145, 4030012, 1, 1, 0, 15000});
        datas.add(new int[]{9500145, 4131005, 1, 1, 0, 100});

        //플레이 씰
        datas.add(new int[]{9500146, 0, 100, 150, 0, 700000});
        datas.add(new int[]{9500146, 1041068, 1, 1, 0, 350});
        datas.add(new int[]{9500146, 1061063, 1, 1, 0, 350});
        datas.add(new int[]{9500146, 1072109, 1, 1, 0, 350});
        datas.add(new int[]{9500146, 1072116, 1, 1, 0, 350});
        datas.add(new int[]{9500146, 1082066, 1, 1, 0, 350});
        datas.add(new int[]{9500146, 1082082, 1, 1, 0, 350});
        datas.add(new int[]{9500146, 1372000, 1, 1, 0, 350});
        datas.add(new int[]{9500146, 1402007, 1, 1, 0, 350});
        datas.add(new int[]{9500146, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9500146, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9500146, 2022075, 1, 1, 0, 5000});
        datas.add(new int[]{9500146, 2041008, 1, 1, 0, 70});
        datas.add(new int[]{9500146, 2041017, 1, 1, 0, 70});
        datas.add(new int[]{9500146, 2041020, 1, 1, 0, 70});
        datas.add(new int[]{9500146, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9500146, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9500146, 4000212, 1, 1, 0, 400000});
        datas.add(new int[]{9500146, 4010005, 1, 1, 0, 1000});
        datas.add(new int[]{9500146, 4020007, 1, 1, 0, 150});
        datas.add(new int[]{9500146, 4030012, 1, 1, 0, 15000});
        datas.add(new int[]{9500146, 4130010, 1, 1, 0, 100});

        //예티와 코-크텀프
        datas.add(new int[]{9500147, 0, 200, 300, 0, 700000});
        datas.add(new int[]{9500147, 1002271, 1, 1, 0, 350});
        datas.add(new int[]{9500147, 1002275, 1, 1, 0, 350});
        datas.add(new int[]{9500147, 1072137, 1, 1, 0, 350});
        datas.add(new int[]{9500147, 1072145, 1, 1, 0, 350});
        datas.add(new int[]{9500147, 1072148, 1, 1, 0, 350});
        datas.add(new int[]{9500147, 1072151, 1, 1, 0, 350});
        datas.add(new int[]{9500147, 1082095, 1, 1, 0, 350});
        datas.add(new int[]{9500147, 1082099, 1, 1, 0, 350});
        datas.add(new int[]{9500147, 1082104, 1, 1, 0, 350});
        datas.add(new int[]{9500147, 1082107, 1, 1, 0, 350});
        datas.add(new int[]{9500147, 1372014, 1, 1, 0, 350});
        datas.add(new int[]{9500147, 1382006, 1, 1, 0, 350});
        datas.add(new int[]{9500147, 1402012, 1, 1, 0, 350});
        datas.add(new int[]{9500147, 2000004, 1, 1, 0, 1000});
        datas.add(new int[]{9500147, 2000006, 1, 1, 0, 1000});
        datas.add(new int[]{9500147, 2022075, 1, 1, 0, 5000});
        datas.add(new int[]{9500147, 2040302, 1, 1, 0, 70});
        datas.add(new int[]{9500147, 2040502, 1, 1, 0, 70});
        datas.add(new int[]{9500147, 2040505, 1, 1, 0, 70});
        datas.add(new int[]{9500147, 2050000, 1, 1, 0, 35000});
        datas.add(new int[]{9500147, 4000213, 1, 1, 0, 400000});
        datas.add(new int[]{9500147, 4010006, 1, 1, 0, 1000});
        datas.add(new int[]{9500147, 4020007, 1, 1, 0, 150});
        datas.add(new int[]{9500147, 4020008, 1, 1, 0, 150});
        datas.add(new int[]{9500147, 4030012, 1, 1, 0, 15000});

        //이글루 터틀
        datas.add(new int[]{9500148, 0, 130, 185, 0, 700000});
        datas.add(new int[]{9500148, 1002004, 1, 1, 0, 350});
        datas.add(new int[]{9500148, 1002155, 1, 1, 0, 350});
        datas.add(new int[]{9500148, 1002161, 1, 1, 0, 350});
        datas.add(new int[]{9500148, 1002183, 1, 1, 0, 350});
        datas.add(new int[]{9500148, 1040095, 1, 1, 0, 350});
        datas.add(new int[]{9500148, 1041089, 1, 1, 0, 350});
        datas.add(new int[]{9500148, 1060084, 1, 1, 0, 350});
        datas.add(new int[]{9500148, 1061088, 1, 1, 0, 350});
        datas.add(new int[]{9500148, 1082087, 1, 1, 0, 350});
        datas.add(new int[]{9500148, 1082089, 1, 1, 0, 350});
        datas.add(new int[]{9500148, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9500148, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9500148, 2022075, 1, 1, 0, 5000});
        datas.add(new int[]{9500148, 2040802, 1, 1, 0, 70});
        datas.add(new int[]{9500148, 2040805, 1, 1, 0, 70});
        datas.add(new int[]{9500148, 2040900, 1, 1, 0, 70});
        datas.add(new int[]{9500148, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9500148, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9500148, 4000218, 1, 1, 0, 400000});
        datas.add(new int[]{9500148, 4006000, 1, 1, 0, 600});
        datas.add(new int[]{9500148, 4010001, 1, 1, 0, 1000});
        datas.add(new int[]{9500148, 4020001, 1, 1, 0, 150});
        datas.add(new int[]{9500148, 4020003, 1, 1, 0, 150});
        datas.add(new int[]{9500148, 4030012, 1, 1, 0, 15000});

        //코-크 골렘
        datas.add(new int[]{9500149, 0, 150, 200, 0, 700000});
        datas.add(new int[]{9500149, 1041084, 1, 1, 0, 350});
        datas.add(new int[]{9500149, 1051025, 1, 1, 0, 350});
        datas.add(new int[]{9500149, 1061083, 1, 1, 0, 350});
        datas.add(new int[]{9500149, 1072123, 1, 1, 0, 350});
        datas.add(new int[]{9500149, 1072136, 1, 1, 0, 350});
        datas.add(new int[]{9500149, 1082010, 1, 1, 0, 350});
        datas.add(new int[]{9500149, 1082066, 1, 1, 0, 350});
        datas.add(new int[]{9500149, 1452004, 1, 1, 0, 350});
        datas.add(new int[]{9500149, 1462008, 1, 1, 0, 350});
        datas.add(new int[]{9500149, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9500149, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9500149, 2000004, 1, 1, 0, 1000});
        datas.add(new int[]{9500149, 2002003, 1, 1, 0, 1000});
        datas.add(new int[]{9500149, 2022075, 1, 1, 0, 5000});
        datas.add(new int[]{9500149, 2040706, 1, 1, 0, 70});
        datas.add(new int[]{9500149, 2040709, 1, 1, 0, 70});
        datas.add(new int[]{9500149, 2040710, 1, 1, 0, 70});
        datas.add(new int[]{9500149, 4000219, 1, 1, 0, 400000});
        datas.add(new int[]{9500149, 4010006, 1, 1, 0, 1000});
        datas.add(new int[]{9500149, 4020004, 1, 1, 0, 150});
        datas.add(new int[]{9500149, 4030012, 1, 1, 0, 15000});

        //아이스 골렘
        datas.add(new int[]{9500150, 0, 185, 250, 0, 700000});
        datas.add(new int[]{9500150, 1002242, 1, 1, 0, 350});
        datas.add(new int[]{9500150, 1002247, 1, 1, 0, 350});
        datas.add(new int[]{9500150, 1002267, 1, 1, 0, 350});
        datas.add(new int[]{9500150, 1040090, 1, 1, 0, 350});
        datas.add(new int[]{9500150, 1041094, 1, 1, 0, 350});
        datas.add(new int[]{9500150, 1060079, 1, 1, 0, 350});
        datas.add(new int[]{9500150, 1061093, 1, 1, 0, 350});
        datas.add(new int[]{9500150, 1082095, 1, 1, 0, 350});
        datas.add(new int[]{9500150, 1082098, 1, 1, 0, 350});
        datas.add(new int[]{9500150, 1082106, 1, 1, 0, 350});
        datas.add(new int[]{9500150, 1302010, 1, 1, 0, 350});
        datas.add(new int[]{9500150, 1312018, 1, 1, 0, 350});
        datas.add(new int[]{9500150, 1332016, 1, 1, 0, 350});
        datas.add(new int[]{9500150, 2000004, 1, 1, 0, 1000});
        datas.add(new int[]{9500150, 2000006, 1, 1, 0, 1000});
        datas.add(new int[]{9500150, 2022075, 1, 1, 0, 5000});
        datas.add(new int[]{9500150, 2040514, 1, 1, 0, 70});
        datas.add(new int[]{9500150, 2040515, 1, 1, 0, 70});
        datas.add(new int[]{9500150, 2040602, 1, 1, 0, 70});
        datas.add(new int[]{9500150, 4000220, 1, 1, 0, 400000});
        datas.add(new int[]{9500150, 4020002, 1, 1, 0, 150});
        datas.add(new int[]{9500150, 4020008, 1, 1, 0, 150});
        datas.add(new int[]{9500150, 4030012, 1, 1, 0, 15000});

        //코-크 슬라임
        datas.add(new int[]{9500151, 0, 30, 50, 0, 700000});
        datas.add(new int[]{9500151, 1040012, 1, 1, 0, 350});
        datas.add(new int[]{9500151, 1041063, 1, 1, 0, 350});
        datas.add(new int[]{9500151, 1051000, 1, 1, 0, 350});
        datas.add(new int[]{9500151, 1060010, 1, 1, 0, 350});
        datas.add(new int[]{9500151, 1082000, 1, 1, 0, 350});
        datas.add(new int[]{9500151, 1082029, 1, 1, 0, 350});
        datas.add(new int[]{9500151, 1402018, 1, 1, 0, 350});
        datas.add(new int[]{9500151, 1432008, 1, 1, 0, 350});
        datas.add(new int[]{9500151, 1501000, 1, 1, 0, 350}); //해적템
        datas.add(new int[]{9500151, 2000000, 1, 1, 0, 1000});
        datas.add(new int[]{9500151, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9500151, 2022075, 1, 1, 0, 5000});
        datas.add(new int[]{9500151, 2040703, 1, 1, 0, 70});
        datas.add(new int[]{9500151, 2040800, 1, 1, 0, 70});
        datas.add(new int[]{9500151, 2041008, 1, 1, 0, 70});
        datas.add(new int[]{9500151, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9500151, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9500151, 4000209, 1, 1, 0, 400000});
        datas.add(new int[]{9500151, 4006000, 1, 1, 0, 600});
        datas.add(new int[]{9500151, 4020002, 1, 1, 0, 150});
        datas.add(new int[]{9500151, 4020005, 1, 1, 0, 150});
        datas.add(new int[]{9500151, 4030012, 1, 1, 0, 15000});

        //코-크 버섯
        datas.add(new int[]{9500152, 0, 40, 65, 0, 700000});
        datas.add(new int[]{9500152, 1002146, 1, 1, 0, 350});
        datas.add(new int[]{9500152, 1040020, 1, 1, 0, 350});
        datas.add(new int[]{9500152, 1060015, 1, 1, 0, 350});
        datas.add(new int[]{9500152, 1072019, 1, 1, 0, 350});
        datas.add(new int[]{9500152, 1072025, 1, 1, 0, 350});
        datas.add(new int[]{9500152, 1302003, 1, 1, 0, 350});
        datas.add(new int[]{9500152, 1312005, 1, 1, 0, 350});
        datas.add(new int[]{9500152, 2000001, 1, 1, 0, 1000});
        datas.add(new int[]{9500152, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9500152, 2002002, 1, 1, 0, 1000});
        datas.add(new int[]{9500152, 2022075, 1, 1, 0, 5000});
        datas.add(new int[]{9500152, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9500152, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9500152, 4000221, 1, 1, 0, 400000});
        datas.add(new int[]{9500152, 4020006, 1, 1, 0, 150});
        datas.add(new int[]{9500152, 4020007, 1, 1, 0, 150});
        datas.add(new int[]{9500152, 4030012, 1, 1, 0, 15000});

        //코-크텀프
        datas.add(new int[]{9500153, 0, 50, 75, 0, 700000});
        datas.add(new int[]{9500153, 1040022, 1, 1, 0, 350});
        datas.add(new int[]{9500153, 1040026, 1, 1, 0, 350});
        datas.add(new int[]{9500153, 1041032, 1, 1, 0, 350});
        datas.add(new int[]{9500153, 1050001, 1, 1, 0, 350});
        datas.add(new int[]{9500153, 1050005, 1, 1, 0, 350});
        datas.add(new int[]{9500153, 1051012, 1, 1, 0, 350});
        datas.add(new int[]{9500153, 1060019, 1, 1, 0, 350});
        datas.add(new int[]{9500153, 1062006, 1, 1, 0, 350});
        datas.add(new int[]{9500153, 1082026, 1, 1, 0, 350});
        datas.add(new int[]{9500153, 2000001, 1, 1, 0, 1000});
        datas.add(new int[]{9500153, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9500153, 2002001, 1, 1, 0, 1000});
        datas.add(new int[]{9500153, 2002005, 1, 1, 0, 1000});
        datas.add(new int[]{9500153, 2022075, 1, 1, 0, 5000});
        datas.add(new int[]{9500153, 2044202, 1, 1, 0, 70});
        datas.add(new int[]{9500153, 2044302, 1, 1, 0, 70});
        datas.add(new int[]{9500153, 2044602, 1, 1, 0, 70});
        datas.add(new int[]{9500153, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9500153, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9500153, 4000216, 1, 1, 0, 400000});
        datas.add(new int[]{9500153, 4010006, 1, 1, 0, 1000});
        datas.add(new int[]{9500153, 4020001, 1, 1, 0, 150});
        datas.add(new int[]{9500153, 4020002, 1, 1, 0, 150});
        datas.add(new int[]{9500153, 4030012, 1, 1, 0, 15000});

        //코-크텀프 라이트
        datas.add(new int[]{9500154, 0, 60, 90, 0, 700000});
        datas.add(new int[]{9500154, 1072003, 1, 1, 0, 350});
        datas.add(new int[]{9500154, 1072034, 1, 1, 0, 350});
        datas.add(new int[]{9500154, 1072072, 1, 1, 0, 350});
        datas.add(new int[]{9500154, 1082045, 1, 1, 0, 350});
        datas.add(new int[]{9500154, 1082068, 1, 1, 0, 350});
        datas.add(new int[]{9500154, 1322014, 1, 1, 0, 350});
        datas.add(new int[]{9500154, 1332009, 1, 1, 0, 350});
        datas.add(new int[]{9500154, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9500154, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9500154, 2000016, 1, 1, 0, 35000}); //41에 없는템
        datas.add(new int[]{9500154, 2022075, 1, 1, 0, 5000});
        datas.add(new int[]{9500154, 2043800, 1, 1, 0, 70});
        datas.add(new int[]{9500154, 2044001, 1, 1, 0, 70});
        datas.add(new int[]{9500154, 2044100, 1, 1, 0, 70});
        datas.add(new int[]{9500154, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9500154, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9500154, 4000217, 1, 1, 0, 350000});
        datas.add(new int[]{9500154, 4010004, 1, 1, 0, 1000});
        datas.add(new int[]{9500154, 4020003, 1, 1, 0, 150});
        datas.add(new int[]{9500154, 4030012, 1, 1, 0, 15000});
        datas.add(new int[]{9500154, 4131003, 1, 1, 0, 100});

        //PQ
        //킹슬라임(35제 메이플 장비)
        datas.add(new int[]{9300003, 1092030, 1, 1, 0, 10000});
        datas.add(new int[]{9300003, 1302020, 1, 1, 0, 10000});
        datas.add(new int[]{9300003, 1382009, 1, 1, 0, 10000});
        datas.add(new int[]{9300003, 1452016, 1, 1, 0, 10000});
        datas.add(new int[]{9300003, 1462014, 1, 1, 0, 10000});
        datas.add(new int[]{9300003, 1472030, 1, 1, 0, 10000});
        datas.add(new int[]{9300003, 1482020, 1, 1, 0, 10000});
        datas.add(new int[]{9300003, 1492020, 1, 1, 0, 10000});
        datas.add(new int[]{9300003, 1002416, 1, 1, 0, 5000});//슬라임 모자
        datas.add(new int[]{9300003, 1002296, 1, 1, 0, 5000});//슬라임모자

        //알리샤르(43제 메이플 장비)
        datas.add(new int[]{9300012, 1302030, 1, 1, 0, 10000});
        datas.add(new int[]{9300012, 1332025, 1, 1, 0, 10000});
        datas.add(new int[]{9300012, 1382012, 1, 1, 0, 10000});
        datas.add(new int[]{9300012, 1412011, 1, 1, 0, 10000});
        datas.add(new int[]{9300012, 1422014, 1, 1, 0, 10000});
        datas.add(new int[]{9300012, 1432012, 1, 1, 0, 10000});
        datas.add(new int[]{9300012, 1442024, 1, 1, 0, 10000});
        datas.add(new int[]{9300012, 1452022, 1, 1, 0, 10000});
        datas.add(new int[]{9300012, 1462019, 1, 1, 0, 10000});
        datas.add(new int[]{9300012, 1472032, 1, 1, 0, 10000});
        datas.add(new int[]{9300012, 1482021, 1, 1, 0, 10000});
        datas.add(new int[]{9300012, 1492021, 1, 1, 0, 10000});

        //파파픽시(64제 메이플 장비)
        datas.add(new int[]{9300039, 1092045, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1092046, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1092047, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1302064, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1312032, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1322054, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1332055, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1332056, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1372034, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1382039, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1402039, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1412027, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1422029, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1432040, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1442051, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1452045, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1462040, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1472055, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1482022, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1492022, 1, 1, 0, 20000});

        /*에레브*/
        //티노
        datas.add(new int[]{100120, 0, 4, 5, 0, 600000});//메소
        //datas.add(new int[]{100120, 4020000, 1, 1, 0, 3000});//기타템
        datas.add(new int[]{100120, 4020000, 1, 1, 0, 3000});//가넷의 원석
        datas.add(new int[]{100120, 4003004, 1, 1, 0, 40000});//뻣뻣한깃털
        datas.add(new int[]{100120, 2010000, 1, 1, 0, 10000});//사과
        datas.add(new int[]{100120, 2060000, 20, 30, 0, 8000});//활전용
        datas.add(new int[]{100120, 2061000, 20, 30, 0, 8000});//석궁전용

        //티브
        datas.add(new int[]{100121, 0, 9, 13, 0, 600000});//메소
        //datas.add(new int[]{1001211, 4020000, 1, 1, 0, 3000});//기타템
        datas.add(new int[]{100121, 4020000, 1, 1, 0, 3000});//가넷의 원석
        datas.add(new int[]{100121, 4003004, 1, 1, 0, 40000});//뻣뻣한깃털
        datas.add(new int[]{100121, 2000000, 1, 1, 0, 10000});//빨간포션
        datas.add(new int[]{100121, 2060000, 20, 30, 0, 8000});//활전용
        datas.add(new int[]{100121, 2061000, 20, 30, 0, 8000});//석궁전용        

        //티무
        datas.add(new int[]{100122, 0, 9, 18, 0, 600000});//메소
        //datas.add(new int[]{100122, 4020000, 1, 1, 0, 3000});//기타템
        datas.add(new int[]{100122, 4010002, 1, 1, 0, 3000});//아쿠아마린의 원석
        datas.add(new int[]{100122, 4020002, 1, 1, 0, 3000});//아쿠아마린의 원석
        datas.add(new int[]{100122, 4003004, 1, 1, 0, 40000});//뻣뻣한깃털
        datas.add(new int[]{100122, 2000000, 1, 1, 0, 10000});//빨간포션
        datas.add(new int[]{100122, 1452002, 1, 1, 0, 350});//워보우
        datas.add(new int[]{100122, 2060000, 20, 30, 0, 8000});//활전용
        datas.add(new int[]{100122, 2061000, 20, 30, 0, 8000});//석궁전용

        //티루
        datas.add(new int[]{100123, 0, 15, 21, 0, 600000});//메소
        //datas.add(new int[]{100123, 4020000, 1, 1, 0, 3000});//기타템
        datas.add(new int[]{100123, 4010003, 1, 1, 0, 3000});//아다만티움의 원석
        datas.add(new int[]{100123, 4003004, 1, 1, 0, 40000});//뻣뻣한깃털
        datas.add(new int[]{100123, 2000000, 1, 1, 0, 10000});//빨간포션
        datas.add(new int[]{100123, 1452002, 1, 1, 0, 350});//워보우
        datas.add(new int[]{100123, 2060000, 20, 30, 0, 8000});//활전용
        datas.add(new int[]{100123, 2061000, 20, 30, 0, 8000});//석궁전용

        //티구루
        datas.add(new int[]{100124, 0, 16, 24, 0, 600000});//메소
        //datas.add(new int[]{100124, 4020000, 1, 1, 0, 3000});//기타템
        datas.add(new int[]{100124, 4010004, 1, 1, 0, 3000});//은의 원석
        datas.add(new int[]{100124, 4020004, 1, 1, 0, 3000});//아다만티움의 원석
        datas.add(new int[]{100124, 4003004, 1, 1, 0, 40000});//뻣뻣한깃털
        datas.add(new int[]{100124, 2000000, 1, 1, 0, 10000});//빨간포션
        datas.add(new int[]{100124, 1452003, 1, 1, 0, 350});//워보우
        datas.add(new int[]{100124, 2060000, 20, 30, 0, 8000});//활전용
        datas.add(new int[]{100124, 2061000, 20, 30, 0, 8000});//석궁전용

        //시험의티구루
        datas.add(new int[]{9001011, 4032096, 1, 1, 20201, 300000});//시험의 증표
        datas.add(new int[]{9001011, 4032097, 1, 1, 20202, 300000});//시험의 증표
        datas.add(new int[]{9001011, 4032098, 1, 1, 20203, 300000});//시험의 증표
        datas.add(new int[]{9001011, 4032099, 1, 1, 20204, 300000});//시험의 증표
        datas.add(new int[]{9001011, 4032100, 1, 1, 20205, 300000});//시험의 증표

        //변신술사
        datas.add(new int[]{9001009, 4032101, 1, 1, 20301, 1000000});//시험의 증표
        datas.add(new int[]{9001009, 4032102, 1, 1, 20302, 1000000});//시험의 증표
        datas.add(new int[]{9001009, 4032103, 1, 1, 20303, 1000000});//시험의 증표
        datas.add(new int[]{9001009, 4032104, 1, 1, 20304, 1000000});//시험의 증표
        datas.add(new int[]{9001009, 4032105, 1, 1, 20305, 1000000});//시험의 증표

        //돼지의 반란
        datas.add(new int[]{1210100, 4032130, 1, 1, 20707, 50000});

        //가짜인형
        datas.add(new int[]{1210103, 4032137, 1, 1, 20711, 300000});
        //몽땅 따버리겠어!
        datas.add(new int[]{1210103, 4032139, 1, 1, 20713, 150000});

        //우드 마스크의 이상
        datas.add(new int[]{2230110, 4032146, 1, 1, 20722, 50000});

        //스톤 마스크의 이상
        datas.add(new int[]{2230111, 4032147, 1, 1, 20723, 50000});

        //포이즌 골렘
        datas.add(new int[]{9300182, 4001164, 1, 1, 0, 1000000});

        /*데비존 부화기*/
        datas.add(new int[]{9300119, 5060002, 1, 1, 0, 50000});
        datas.add(new int[]{9300119, 5060002, 1, 1, 0, 50000});
        datas.add(new int[]{9300119, 5060002, 1, 1, 0, 50000});

        datas.add(new int[]{9300105, 5060002, 1, 1, 0, 50000});
        datas.add(new int[]{9300105, 5060002, 1, 1, 0, 50000});
        datas.add(new int[]{9300105, 5060002, 1, 1, 0, 50000});

        datas.add(new int[]{9300106, 5060002, 1, 1, 0, 50000});
        datas.add(new int[]{9300106, 5060002, 1, 1, 0, 50000});
        datas.add(new int[]{9300106, 5060002, 1, 1, 0, 50000});

        datas.add(new int[]{9300107, 5060002, 1, 1, 0, 50000});
        datas.add(new int[]{9300107, 5060002, 1, 1, 0, 50000});
        datas.add(new int[]{9300107, 5060002, 1, 1, 0, 50000});

        datas.add(new int[]{9300018, 4000142, 1, 1, 1018, 1000000});

        MapleDataProvider mBookPro = MapleDataProviderFactory.getDataProvider(new File("imgs"));
        MapleData mBook = mBookPro.getData("MonsterBook.img");

        /*datas.add(new int[]{2100100, 0, 32, 48, 0, 500000}); //흰 모래토끼
         datas.add(new int[]{2100101, 0, 33, 49, 0, 500000}); //갈색 모래토끼
         datas.add(new int[]{2100102, 0, 40, 60, 0, 500000}); //주니어 카투스
         datas.add(new int[]{2100103, 0, 49, 66, 0, 500000}); //카투스
         datas.add(new int[]{2100104, 0, 56, 78, 0, 500000}); //로얄 카투스
         datas.add(new int[]{2100105, 0, 44, 63, 0, 500000}); //벨라모아
         datas.add(new int[]{2100106, 0, 45, 67, 0, 500000}); //귀마개 프릴드
         datas.add(new int[]{2100107, 0, 48, 72, 0, 500000}); //목도리 프릴드
         datas.add(new int[]{2100108, 0, 52, 78, 0, 500000}); //미요캐츠
         datas.add(new int[]{2110300, 0, 44, 66, 0, 500000}); //모래 두더지
         datas.add(new int[]{2110301, 0, 52, 78, 0, 500000}); //스콜피언
         datas.add(new int[]{3110300, 0, 75, 100, 0, 500000}); //큐브슬라임 lv32
         datas.add(new int[]{3110301, 0, 75, 100, 0, 500000}); //모래난쟁이 lv32
         datas.add(new int[]{3110302, 0, 85, 110, 0, 500000}); //루모 lv35
         datas.add(new int[]{3110303, 0, 95, 120, 0, 500000}); //트리플 루모 lv38
         datas.add(new int[]{4110300, 0, 120, 180, 0, 500000}); //뮤테 레벨 lv42
         datas.add(new int[]{4110301, 0, 160, 240, 0, 500000}); //강화된 아이언 뮤테 lv45
         datas.add(new int[]{4110302, 0, 172, 258, 0, 500000}); //미스릴 뮤테 lv47
         datas.add(new int[]{4230600, 0, 95, 135, 0, 500000}); //모래거인 lv 40
         datas.add(new int[]{5110300, 0, 200, 300, 0, 500000}); //강화된 미스릴 뮤테 lv50
         datas.add(new int[]{5110301, 0, 240, 360, 0, 500000}); //로이드
         datas.add(new int[]{5110302, 0, 288, 432, 0, 500000}); //네오휴로이드
         datas.add(new int[]{6090000, 0, 328 * 4, 492 * 4, 0, 500000}); //리치 lv65
         datas.add(new int[]{6110300, 0, 328, 492, 0, 500000}); //호문 lv65
         datas.add(new int[]{6110301, 0, 332, 498, 0, 500000}); //사이티 lv68
         datas.add(new int[]{7110300, 0, 352, 528, 0, 500000}); //D.로이 lv75
         datas.add(new int[]{7110301, 0, 332, 508, 0, 500000}); //호문쿨루 lv73
         datas.add(new int[]{8110300, 0, 417, 548, 0, 500000}); //호문스큘러 lv80

         for (MapleData fle : mBook) {
         int mid = Integer.parseInt(fle.getName());
         if (!isDonNeedDropDataMob(mid) && (mid >= 2100100 && mid <= 2110301 || isNeedDropDataMob(mid))) { //Ariant Monster, magatia
         MapleMonsterStats mobStat = MapleLifeFactory.getMonsterStats(mid);
         if (mobStat == null) {
         continue;
         }
         for (MapleData reward : fle.getChildByPath("reward")) {
         int itemid = MapleDataTool.getInt(reward);
         if (!ii.itemExists(itemid)) {
         continue;
         }
         if (itemid / 10000 == 206) {
         datas.add(new int[]{mid, itemid, 20, 30, 0, 8000}); //화살
         } else if (itemid / 10000 == 200) {
         datas.add(new int[]{mid, itemid, 1, 1, 0, 10000}); //물약
         } else if (itemid / 1000 == 4004) {
         datas.add(new int[]{mid, itemid, 1, 1, 0, 80}); // 크리스탈 원석
         } else if (itemid / 1000 == 4000) {
         datas.add(new int[]{mid, itemid, 1, 1, 0, 400000}); // 전리품
         } else if (itemid / 10000 == 401) {
         datas.add(new int[]{mid, itemid, 1, 1, 0, 1000}); //광석 원석
         } else if (itemid / 10000 == 402) {
         datas.add(new int[]{mid, itemid, 1, 1, 0, 150}); //보석 원석..
         } else if (itemid / 10000 == 204) {
         datas.add(new int[]{mid, itemid, 1, 1, 0, 70}); //주문서..
         } else if (itemid / 10000 == 202) {
         datas.add(new int[]{mid, itemid, 1, 1, 0, 6000}); //특수물약
         } else if (itemid / 1000000 == 1) {
         datas.add(new int[]{mid, itemid, 1, 1, 0, 40}); //장비
         } else if (itemid / 10000 == 400) {
         datas.add(new int[]{mid, itemid, 1, 1, 0, 600}); //특수물약
         }
         }
         }
         }*/
        
            
        datas.add(new int[]{130100, 4030009, 1, 1, 0, 3000});
        datas.add(new int[]{1110101, 4030009, 1, 1, 0, 3000});
        datas.add(new int[]{1130100, 4030009, 1, 1, 0, 3000});
        datas.add(new int[]{2130100, 4030009, 1, 1, 0, 3000});
        datas.add(new int[]{1210102, 4030001, 1, 1, 0, 300});
        datas.add(new int[]{210100, 4030000, 1, 1, 0, 200});
        datas.add(new int[]{1210100, 4030011, 1, 1, 0, 600});
        datas.add(new int[]{1120100, 4030010, 1, 1, 0, 600});

        datas.add(new int[]{2100108, 4031568, 1, 1, 0, 11000});

        datas.add(new int[]{9300147, 4001132, 1, 1, 0, 350000});
        datas.add(new int[]{9300148, 4001133, 1, 1, 0, 100000});

        addFrankenroid(datas); //1.2.6에는 마가티아 X
        //중독된 스톤버그 - 독안개의 숲
        datas.add(new int[]{9300173, 4001161, 1, 1, 0, 999999});

        //해적의시험장 - 강력한 결정
        datas.add(new int[]{9001005, 4031857, 1, 1, 0, 400000});
        datas.add(new int[]{9001006, 4031856, 1, 1, 0, 400000});
        questdrops.add(4031857);
        questdrops.add(4031856);

        //주황버섯인형
        datas.add(new int[]{9300274, 4032190, 1, 1, 0, 100000});
        questdrops.add(4032190);

        //카이린의 분신 - 검은 부적
        datas.add(new int[]{9001004, 4031059, 1, 1, 0, 999999});

        //영웅의 별, 영웅의 펜타곤 - 해적
        datas.add(new int[]{8180001, 4031861, 1, 1, 6944, 399999});
        datas.add(new int[]{8180000, 4031860, 1, 1, 6944, 399999});
        questdrops.add(4031861);
        questdrops.add(4031860);

        //루모의 잎사귀
        //3110302, 3110303 -> 4031694 (qid 3312)
        datas.add(new int[]{3110302, 4031694, 1, 1, 3312, 100000});
        datas.add(new int[]{3110303, 4031694, 1, 1, 3312, 300000});
        questdrops.add(4031694);

        //돌의 심장
        //8140701 -> 4031872
        datas.add(new int[]{8140701, 4031872, 1, 1, 6340, 200000});
        questdrops.add(4031872);

        //단단한 가죽
        //8140700 -> 4031871
        datas.add(new int[]{8140700, 4031871, 1, 1, 6350, 200000});
        questdrops.add(4031871);

        //바이킹의 깃발
        //8141000 -> 4031873
        datas.add(new int[]{8141000, 4031873, 1, 1, 6380, 200000});
        questdrops.add(4031873);

        //바이킹의 증표
        //8141100 -> 4031874
        datas.add(new int[]{8141100, 4031874, 1, 1, 6390, 200000});
        questdrops.add(4031874);

        //4031869 파풀라투스의 열쇠
        datas.add(new int[]{8500002, 4031869, 1, 1, 6360, 999999});
        questdrops.add(4031869);

        //4031773 바짝 마른 나뭇가지
        datas.add(new int[]{130100, 4031773, 1, 1, 2145, 199999});
        datas.add(new int[]{1110101, 4031773, 1, 1, 2145, 199999});
        datas.add(new int[]{1130100, 4031773, 1, 1, 2145, 199999});
        datas.add(new int[]{1140100, 4031773, 1, 1, 2145, 199999});
        datas.add(new int[]{2130100, 4031773, 1, 1, 2145, 199999});
        questdrops.add(4031773);

        //요괴선사 퇴치
        datas.add(new int[]{7220002, 4031789, 1, 1, 3844, 999999});
        questdrops.add(4031789);

        datas.add(new int[]{5300100, 4031925, 1, 1, 2223, 60000});
        questdrops.add(4031925);

        //카슨의 시험 퀘스트
        datas.add(new int[]{9300141, 4031698, 1, 1, 3310, 199999});
        questdrops.add(4031698);

        //파웬의 출입증 3358 6110301 4031745
        datas.add(new int[]{6110301, 4031745, 1, 1, 3358, 50000});
        questdrops.add(4031745);

        //감춰진 진실 8110300 4031737 3343
        datas.add(new int[]{8110300, 4031737, 1, 1, 3343, 1000000});
        questdrops.add(4031737);

        //검은 마법사의 마법진 3345
        datas.add(new int[]{8110300, 4031740, 1, 1, 3345, 1000000});
        questdrops.add(4031740);
        datas.add(new int[]{7110300, 4031741, 1, 1, 3345, 1000000});
        questdrops.add(4031741);

        //시약 만들기 3366 9300154 4031780 ~ 4031784
        datas.add(new int[]{9300154, 4031780, 1, 1, 3366, 200000});
        datas.add(new int[]{9300154, 4031781, 1, 1, 3366, 200000});
        datas.add(new int[]{9300154, 4031782, 1, 1, 3366, 200000});
        datas.add(new int[]{9300154, 4031783, 1, 1, 3366, 200000});
        datas.add(new int[]{9300154, 4031784, 1, 1, 3366, 200000});

        //3454, 4031926 모든 그레이
        datas.add(new int[]{4230116, 4031926, 1, 1, 3454, 100000});
        datas.add(new int[]{4230117, 4031926, 1, 1, 3454, 120000});
        datas.add(new int[]{4230118, 4031926, 1, 1, 3454, 130000});
        datas.add(new int[]{4240000, 4031926, 1, 1, 3454, 190000});
        questdrops.add(4031926);

        datas.add(new int[]{4230113, 2022354, 1, 1, 3248, 200000});
        datas.add(new int[]{3230306, 2022355, 1, 1, 3248, 200000});
        questdrops.add(4031991);
        questdrops.add(2022354);
        questdrops.add(2022355);
        questdrops.add(4031992);

        //220040000 ~ 220080000 : 4031992 드롭
        datas.add(new int[]{4230114, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{3230306, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{3210207, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{4230113, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{4230115, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{6130200, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{6230300, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{6300100, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{6400100, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{7140000, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{7160000, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{8141000, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{8141100, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{8160000, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{6230400, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{6230500, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{8140200, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{8140300, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{7130010, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{7130300, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{8142000, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{8143000, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{8170000, 4031992, 1, 1, 0, 30000});

        //5100004 4031790 3642
        questdrops.add(4031790);
        datas.add(new int[]{5100004, 4031790, 1, 1, 3642, 660000});

        //7220001 4031793 3647
        questdrops.add(4031790);
        datas.add(new int[]{7220001, 4031793, 1, 1, 3647, 1000000});
        datas.add(new int[]{7220001, 4031793, 1, 1, 3647, 1000000});
        datas.add(new int[]{7220001, 4031793, 1, 1, 3647, 1000000});

        //4031846 130101 1210100 2173
        questdrops.add(4031846);
        datas.add(new int[]{130101, 4031846, 1, 1, 2173, 100000});
        datas.add(new int[]{1210100, 4031846, 1, 1, 2173, 100000});

        datas.add(new int[]{8220004, 4020009, 1, 1, 0, 300000});
        datas.add(new int[]{8220005, 4020009, 1, 1, 0, 500000});
        datas.add(new int[]{8220005, 4020009, 1, 1, 0, 500000});
        datas.add(new int[]{8220006, 4020009, 1, 1, 0, 1000000});
        datas.add(new int[]{8220006, 4020009, 1, 1, 0, 500000});
        datas.add(new int[]{8220006, 4020009, 1, 1, 0, 200000});

        datas.add(new int[]{9300169, 4001022, 1, 1, 0, 1000000});
        datas.add(new int[]{9300170, 4001022, 1, 1, 0, 1000000});
        datas.add(new int[]{9300171, 4001022, 1, 1, 0, 1000000});

        PreparedStatement psdd = con.prepareStatement("SELECT * FROM `drop_data_p`");
        ResultSet rsdd = psdd.executeQuery();
        while (rsdd.next()) {
            int itemid = rsdd.getInt("itemid");
            if (ii.itemExists(itemid)) {
                datas.add(new int[]{rsdd.getInt("dropperid"), itemid, 1, 1, rsdd.getInt("questid"), rsdd.getInt("chance")});
            } else {
                System.err.println("Pinkbeen item not exists : " + itemid + "(" + ii.getName(itemid) + ")");
            }
        }
        rsdd.close();
        psdd.close();
        customMoneyMobAdd.add(8820001); // 핑크빈


        /* 해적 마북들
         //속성강화 20
         datas.add(new int[]{8510000, 2290112, 1, 1, 0, 50000});
         datas.add(new int[]{8140702, 2290112, 1, 1, 0, 6});

         //서포트 옥토퍼스 20
         datas.add(new int[]{8520000, 2290114, 1, 1, 0, 30000});
         datas.add(new int[]{8142100, 2290114, 1, 1, 0, 4});

         //어드밴스드 호밍
         datas.add(new int[]{8190005, 2290124, 1, 1, 0, 5});
         datas.add(new int[]{8510000, 2290124, 1, 1, 0, 20000});

         //래피드 파이어 20
         datas.add(new int[]{8810023, 2290117, 1, 1, 0, 7});
         datas.add(new int[]{8180000, 2290117, 1, 1, 0, 9000});

         //래피드 파이어 30
         datas.add(new int[]{8150100, 2290118, 1, 1, 0, 5});

         //에어스트라이크 20
         datas.add(new int[]{8800002, 2290115, 1, 1, 0, 90000});

         //에어스트라이크 30
         datas.add(new int[]{8810018, 2290116, 1, 1, 0, 230000});

         //마인드 컨트롤 20
         datas.add(new int[]{8500002, 2290123, 1, 1, 0, 11200});

         //배틀쉽 캐논 20
         datas.add(new int[]{8180001, 2290119, 1, 1, 0, 9000});
         datas.add(new int[]{8150302, 2290119, 1, 1, 0, 5});

         //배틀쉽 캐논 30
         datas.add(new int[]{8150300, 2290120, 1, 1, 0, 5});

         //배틀쉽 토르페도 20  2290121
         datas.add(new int[]{8500002, 2290121, 1, 1, 0, 7800});
         datas.add(new int[]{8190004, 2290121, 1, 1, 0, 6});

         //배틀쉽 토르페도 30  2290122
         datas.add(new int[]{8520000, 2290122, 1, 1, 0, 132000});
         datas.add(new int[]{8140701, 2290122, 1, 1, 0, 4});
         */
        for (int[] i : datas) {
            mobdrops.add(new MobDropEntry(i[0], i[1], i[5], i[2], i[3], i[4]));
        }

        List<int[]> datas_r = new ArrayList<int[]>();

        datas_r.add(new int[]{2612004, 4031703, 999999, 3302, 1, 1});

        int[] normal_scrolls = new int[]{2040001, 2040002, 2040004, 2040005, 2040025, 2040026, 2040029, 2040031, 2040301, 2040302,
            2040317, 2040318, 2040321, 2040323, 2040326, 2040328, 2040401, 2040402, 2040418, 2040419,
            2040421, 2040422, 2040425, 2040427, 2040501, 2040502, 2040504, 2040505, 2040513, 2040514,
            2040516, 2040517, 2040532, 2040534, 2040601, 2040602, 2040618, 2040619, 2040621, 2040622,
            2040625, 2040627, 2040701, 2040702, 2040704, 2040705, 2040707, 2040708, 2040801, 2040802,
            2040804, 2040805, 2040824, 2040825, 2040901, 2040902, 2040924, 2040925, 2040927, 2040928,
            2040931, 2040933, 2041001, 2041002, 2041004, 2041005, 2041007, 2041008, 2041010, 2041011,
            2041013, 2041014, 2041016, 2041017, 2041019, 2041020, 2041022, 2041023, 2043001, 2043002,
            2043017, 2043019, 2043101, 2043102, 2043112, 2043114, 2043201, 2043202, 2043212, 2043214,
            2043301, 2043302, 2043701, 2043702, 2043801, 2043802, 2044001, 2044002, 2044012, 2044014,
            2044101, 2044102, 2044112, 2044114, 2044201, 2044202, 2044212, 2044214, 2044301, 2044302,
            2044312, 2044314, 2044401, 2044402, 2044412, 2044414, 2044501, 2044502, 2044601, 2044602,
            2044701, 2044702, 2044801, 2044802, 2044807, 2044809, 2044901, 2044902, 2048001, 2048002,
            2048004, 2048005, 2049100};

        datas_r.add(new int[]{6802000, 0, 999999, 0, 10, 49});
        datas_r.add(new int[]{6802000, 0, 999999, 0, 10, 49});
        datas_r.add(new int[]{6802000, 0, 999999, 0, 10, 49});
        datas_r.add(new int[]{6802000, 0, 999999, 0, 10, 49});
        datas_r.add(new int[]{6802001, 0, 999999, 0, 10, 49});
        datas_r.add(new int[]{6802001, 0, 999999, 0, 10, 49});
        datas_r.add(new int[]{6802001, 0, 999999, 0, 10, 49});
        datas_r.add(new int[]{6802001, 0, 999999, 0, 10, 49});

        for (int zii : normal_scrolls) {
            datas_r.add(new int[]{6802001, zii, 1300, 0, 1, 1});
            datas_r.add(new int[]{6802000, zii, 1600, 0, 1, 1});
        }
        //크리스탈 원석
        for (int iii = 4004000; iii <= 4004003; ++iii) {
            datas_r.add(new int[]{6802000, iii, 20000, 0, 1, 1});
            datas_r.add(new int[]{6802001, iii, 20000, 0, 1, 1});
        }
        //광물 원석
        for (int iii = 4010000; iii <= 4010007; ++iii) {
            datas_r.add(new int[]{6802000, iii, 100000, 0, 1, 1});
            datas_r.add(new int[]{6802001, iii, 100000, 0, 1, 1});
        }
        //보석 원석
        for (int iii = 4020000; iii <= 4020008; ++iii) {
            datas_r.add(new int[]{6802000, iii, 50000, 0, 1, 1});
            datas_r.add(new int[]{6802001, iii, 50000, 0, 1, 1});
        }
        //만병 통치약
        datas_r.add(new int[]{6802000, 2050004, 250000, 0, 1, 1});
        datas_r.add(new int[]{6802001, 2050004, 250000, 0, 1, 1});
        //안약 보약 성수
        for (int iii = 2050001; iii <= 2050003; ++iii) {
            datas_r.add(new int[]{6802000, iii, 90000, 0, 1, 1});
            datas_r.add(new int[]{6802001, iii, 90000, 0, 1, 1});
        }

        //독안개의 숲 파퀘
        datas_r.add(new int[]{3002000, 4001162, 999999, 0, 1, 1});
        datas_r.add(new int[]{3002001, 4001163, 999999, 0, 1, 1});

        //속성강화 30
        datas_r.add(new int[]{9202012, 2290113, 3600, 0, 1, 1});

        //래피드 파이어 30
        datas_r.add(new int[]{9202012, 2290118, 4400, 0, 1, 1});

        //배틀쉽 캐논 30
        datas_r.add(new int[]{9202012, 2290120, 5400, 0, 1, 1});

        //커다란 진주 퀘스트
        datas_r.add(new int[]{1202002, 4031843, 999999, 2169, 1, 1});
        questdrops.add(4031843);

        //
        datas_r.add(new int[]{2612005, 4031798, 999999, 3366, 1, 1});
        questdrops.add(4031798);

        datas_r.add(new int[]{2502002, 2022252, 999999, 3839, 1, 1});
        questdrops.add(4031798);

        datas_r.add(new int[]{1012000, 4032143, 999999, 20717, 1, 1});
        questdrops.add(4032143);

        for (int[] i : datas_r) {
            //`reactorid`, `itemid`, `chance`, `questid`, `min`, `max`
            reactordrops.add(new ReactorDropEntry(i[0], i[1], i[2], i[4], i[5], i[3]));
        }
        //(qid)
        reactordrops.add(new ReactorDropEntry(2222001, 1002309, 5000, 1, 1, 0));//수박 모자
        reactordrops.add(new ReactorDropEntry(2222001, 1002312, 5000, 1, 1, 0));//귀신수박 모자
        reactordrops.add(new ReactorDropEntry(2222001, 5060002, 5000, 1, 1, 0));//부화기
        reactordrops.add(new ReactorDropEntry(2222001, 5060002, 5000, 1, 1, 0));//부화기
        reactordrops.add(new ReactorDropEntry(2222001, 5060002, 5000, 1, 1, 0));//부화기
        reactordrops.add(new ReactorDropEntry(2222001, 5060002, 5000, 1, 1, 0));//부화기
        reactordrops.add(new ReactorDropEntry(2222001, 5060002, 10000, 1, 1, 0));//부화기
        reactordrops.add(new ReactorDropEntry(2222001, 2001000, 1000000, 1, 5, 0));//소비템 수박
        reactordrops.add(new ReactorDropEntry(2222001, 2001000, 1000000, 1, 5, 0));//소비템 수박
        reactordrops.add(new ReactorDropEntry(2222001, 2001000, 1000000, 1, 5, 0));//소비템 수박
        reactordrops.add(new ReactorDropEntry(2222001, 2001000, 1000000, 1, 5, 0));//소비템 수박
        reactordrops.add(new ReactorDropEntry(2222001, 2001000, 1000000, 1, 5, 0));//소비템 수박
        reactordrops.add(new ReactorDropEntry(2222001, 2001000, 1000000, 1, 5, 0));//소비템 수박
        reactordrops.add(new ReactorDropEntry(2222001, 2001000, 1000000, 1, 5, 0));//소비템 수박
        reactordrops.add(new ReactorDropEntry(2222001, 2001000, 1000000, 1, 5, 0));//소비템 수박

        Map<Integer, List<MobDropEntry>> mdrop_final = new HashMap<Integer, List<MobDropEntry>>();
        Map<Integer, List<MobDropEntry>> mdrop_final2 = new HashMap<Integer, List<MobDropEntry>>();
        Map<Integer, List<ReactorDropEntry>> rdrop_final = new HashMap<Integer, List<ReactorDropEntry>>();

        for (MobDropEntry mde : mobdrops) {
            if (!mdrop_final.containsKey(mde.mobid)) {
                mdrop_final.put(mde.mobid, new ArrayList<MobDropEntry>());
            }
            List<MobDropEntry> dd = mdrop_final.get(mde.mobid);
            dd.add(mde);
        }
        for (ReactorDropEntry rde : reactordrops) {
            if (!rdrop_final.containsKey(rde.reactorid)) {
                rdrop_final.put(rde.reactorid, new ArrayList<ReactorDropEntry>());
            }
            List<ReactorDropEntry> dd = rdrop_final.get(rde.reactorid);
            dd.add(rde);
        }

        for (MapleData mD : mBook) {
            int mobid = Integer.parseInt(mD.getName());
            List<Integer> d = mBookRewards.get(mobid);
            if (d == null) {
                d = new LinkedList<Integer>();
                mBookRewards.put(mobid, d);
            }
            for (MapleData mDR : mD.getChildByPath("reward")) {
                int itemid = MapleDataTool.getInt(mDR);
                d.add(itemid);
            }
        }

        for (Entry<Integer, List<Integer>> mBookChild : mBookRewards.entrySet()) { //1.2.6은 몬스터북 X
            if (mdrop_final.containsKey(mBookChild.getKey())) {
                //                List<MobDropEntry> missingMBookDrops = new ArrayList<MobDropEntry>();
                List<MobDropEntry> mdes = mdrop_final.get(mBookChild.getKey());
                List<MobDropEntry> newMobDrops = new ArrayList<MobDropEntry>(mdes);
                List<Integer> mBookRewardIds = mBookChild.getValue();
                boolean isBoss = MapleLifeFactory.getMonsterStats(mBookChild.getKey()).isBoss();
                boolean isRaidBoss = false;

                for (Integer itemid : mBookRewardIds) {
                    boolean found = false;
                    for (MobDropEntry mde : mdes) {
                        if (mde.itemid == itemid) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        switch (mBookChild.getKey()) {
                            case 8810018:
                            case 8800002:
                            case 8520000:
                            case 8510000:
                            case 8500002:
                            case 8820001:
                                isRaidBoss = true;
                                break;
                        }
                        add(itemid, newMobDrops, mBookChild, isBoss, isRaidBoss);
                        System.out.println("드롭 테이블에 없는 아이템 - 몬스터 : " + mBookChild.getKey() + " / 아이템 : " + itemid + " (" + ii.getName(itemid) + ")");
                    }
                }

                boolean hasMoney = false;
                for (MobDropEntry mde : mdes) {
                    if (mde.itemid == 0) {
                        hasMoney = true;
                        break;
                    }
                }

                for (int i = 0; i < (isBoss ? 5 : 1) && !hasMoney; ++i) {
                    Random r = new Random();
                    MapleMonsterStats mobstat = MapleLifeFactory.getMonsterStats(mBookChild.getKey());
                    double mesoDecrease = Math.pow(0.93, mobstat.getExp() / (isBoss ? 2000.0 : 300.0));
                    if (mesoDecrease > 1.0) {
                        mesoDecrease = 1.0;
                    } else if (mesoDecrease < 0.001) {
                        mesoDecrease = 0.005;
                    }
                    int tempmeso = Math.min(30000, (int) (mesoDecrease * (mobstat.getExp() * 5.7) / 10.0));

                    final int meso = tempmeso;
                    newMobDrops.add(new MobDropEntry(mBookChild.getKey(), 0, 700000, (int) (meso * 0.75), meso, 0)); //화살
                }

                mdrop_final.put(mBookChild.getKey(), newMobDrops);
            } else {
                if (!mBookChild.getValue().isEmpty()) {
                    System.out.println("드롭에 없는 몹 : " + mBookChild.getKey() + " (" + getMobName(mBookChild.getKey()) + ")");

                    boolean isBoss = MapleLifeFactory.getMonsterStats(mBookChild.getKey()).isBoss();
                    boolean isRaidBoss = false;
                    switch (mBookChild.getKey()) {
                        case 8810018:
                        case 8800002:
                        case 8520000:
                        case 8510000:
                        case 8500002:
                        case 8820001:
                            isRaidBoss = true;
                            break;
                    }

                    List<MobDropEntry> newMobDrops = new ArrayList<MobDropEntry>();
                    if (isRaidBoss) {
                        // ..
                    } else {
                        for (int i = 0; i < (isBoss ? 5 : 1); ++i) {
                            Random r = new Random();
                            MapleMonsterStats mobstat = MapleLifeFactory.getMonsterStats(mBookChild.getKey());
                            double mesoDecrease = Math.pow(0.93, mobstat.getExp() / (isBoss ? 2000.0 : 300.0));
                            if (mesoDecrease > 1.0) {
                                mesoDecrease = 1.0;
                            } else if (mesoDecrease < 0.001) {
                                mesoDecrease = 0.005;
                            }
                            int tempmeso = Math.min(30000, (int) (mesoDecrease * (mobstat.getExp() * 5.7) / 10.0));

                            final int meso = tempmeso;
                            newMobDrops.add(new MobDropEntry(mBookChild.getKey(), 0, 700000, (int) (meso * 0.75), meso, 0)); //화살
                        }
                    }
                    for (Integer itemid : mBookChild.getValue()) {
                        add(itemid, newMobDrops, mBookChild, isBoss, isRaidBoss);
                    }
                    mdrop_final.put(mBookChild.getKey(), newMobDrops);
                }
            }
        }

        for (int cs : customMoneyMobAdd) {

            List<MobDropEntry> mdes = mdrop_final.get(cs);
            if (mdes == null) {
                mdes = new ArrayList<MobDropEntry>();
            }
            List<MobDropEntry> newMobDrops = new ArrayList<MobDropEntry>(mdes);
            boolean hasMoney = false;
            for (MobDropEntry mde : mdes) {
                if (mde.itemid == 0) {
                    hasMoney = true;
                    break;
                }
            }

            boolean isBoss = MapleLifeFactory.getMonsterStats(cs).isBoss();
            boolean isRaidBoss = false;
            switch (cs) {
                case 8810018:
                case 8800002:
                case 8520000:
                case 8510000:
                case 8500002:
                case 8820001:
                    isRaidBoss = true;
                    break;
            }
            for (int i = 0; i < (isRaidBoss ? 15 : isBoss ? 5 : 1) && !hasMoney; ++i) {
                Random r = new Random();
                MapleMonsterStats mobstat = MapleLifeFactory.getMonsterStats(cs);
                double mesoDecrease = Math.pow(0.93, mobstat.getExp() / (isBoss ? 2000.0 : 300.0));
                if (mesoDecrease > 1.0) {
                    mesoDecrease = 1.0;
                } else if (mesoDecrease < 0.001) {
                    mesoDecrease = 0.005;
                }
                int tempmeso = Math.min(30000, (int) (mesoDecrease * (mobstat.getExp() * 5.7) / 10.0));

                final int meso = tempmeso;
                newMobDrops.add(new MobDropEntry(cs, 0, 700000, (int) (meso * 0.75), meso, 0)); //화살
            }
            mdrop_final.put(cs, newMobDrops);
        }
        getMobName(100100);

        for (int i = 2380000; i <= 2388046; ++i) {
            if (ii.itemExists(i)) {
                String name = ii.getName(i).replaceAll(" 카드", "");
                for (Entry<Integer, String> es : mobnames.entrySet()) {
                    if (es.getValue().equalsIgnoreCase(name)) {
                        List<MobDropEntry> d = mdrop_final.get(es.getKey());
                        if (d == null) {
                            System.out.println("Empty mob : " + es.getKey() + " (" + getMobName(es.getKey()) + ")");
                            continue;
                            //mdrop_final.put(es.getKey(), d);
                            //d = mdrop_final.get(es.getKey());
                        }
                        MapleMonsterStats mobstat = MapleLifeFactory.getMonsterStats(es.getKey());
                        d.add(new MobDropEntry(es.getKey(), i, 1000 * (mobstat.isBoss() ? 25 : 1), 1, 1, 0));
                        break;
                    }
                }
            }
        }

        for (List<MobDropEntry> mdes : mdrop_final.values()) {
            for (MobDropEntry mde : mdes) {
                ps.setInt(1, mde.mobid);
                ps.setInt(2, mde.itemid);
                ps.setInt(3, mde.min);
                ps.setInt(4, mde.max);
                ps.setInt(5, mde.questid);
                ps.setInt(6, mde.chance);
                ps.addBatch();
            }
        }
        List<MobDropEntry> temp = mdrop_final.get(6110300);//6110300
        //temp.addAll(mdrop_final.get(6110300)); //호문, 폐쇄된 연구실의 호문 Clone
        for (MobDropEntry mde : temp) {
            mde.mobid = 9300141;
        }
        mdrop_final2.put(9300141, temp);

        temp = mdrop_final.get(1210102);
        for (MobDropEntry mde : temp) {
            mde.mobid = 9300274;
        }
        mdrop_final2.put(9300274, temp);
        for (List<MobDropEntry> mdes : mdrop_final2.values()) {
            for (MobDropEntry mde : mdes) {
                ps.setInt(1, mde.mobid);
                ps.setInt(2, mde.itemid);
                ps.setInt(3, mde.min);
                ps.setInt(4, mde.max);
                ps.setInt(5, mde.questid);
                ps.setInt(6, mde.chance);
                ps.addBatch();
            }
        }

        for (List<ReactorDropEntry> mdes : rdrop_final.values()) {
            for (ReactorDropEntry mde : mdes) {
                ps2.setInt(1, mde.reactorid);
                ps2.setInt(2, mde.itemid);
                ps2.setInt(3, mde.chance);
                ps2.setInt(4, mde.questid);
                ps2.setInt(5, mde.min);
                ps2.setInt(6, mde.max);
                ps2.addBatch();
            }
        }

        for (MapleQuest quest : MapleQuest.getAllInstances()) {
            for (MapleQuestAction act : quest.getCompleteActs()) {
                if (act.getItems() == null) {
                    continue;
                }
                for (QuestItem qitem : act.getItems()) {
                    if (qitem.count < 0 && MapleItemInformationProvider.getInstance().isQuestItem(qitem.itemid)) {
                        if (!questdrops.contains(Integer.valueOf(qitem.itemid))) {
                            System.out.println(qitem.itemid + " : " + (ii.getName(qitem.itemid)) + " (" + quest.getId() + " - " + quest.getName() + ")");
                        }
                    }
                }
            }
        }

        ps.executeBatch();
        ps2.executeBatch();
        ps.close();
        ps2.close();
        System.out.println("Job Done. Elapsed Time : " + (System.currentTimeMillis() - start) + "ms");
    }

    public static class MobDropEntry {

        public MobDropEntry(int mobid, int itemid, int chance, int min, int max, int questid) {
            this.mobid = mobid;
            this.itemid = itemid;
            this.chance = chance;
            this.min = min;
            this.max = max;
            this.questid = questid;
        }

        public int mobid;
        public int itemid;
        public int chance;
        public int min;
        public int max;
        public int questid;
    }

    public static class ReactorDropEntry {

        public ReactorDropEntry(int mobid, int itemid, int chance, int min, int max, int questid) {
            this.reactorid = mobid;
            this.itemid = itemid;
            this.chance = chance;
            this.min = min;
            this.max = max;
            this.questid = questid;
        }

        public int reactorid;
        public int itemid;
        public int chance;
        public int min;
        public int max;
        public int questid;
    }

    static final Map<Integer, String> mobnames = new HashMap<>();

    public static String getMobName(int mid) {
        if (mobnames.isEmpty()) {
            MapleDataProvider p = MapleDataProviderFactory.getDataProvider(new File("wz/String.wz"));
            MapleData d = p.getData("Mob.img");
            for (MapleData dd : d) {
                mobnames.put(Integer.parseInt(dd.getName()), MapleDataTool.getString("name", dd, "null"));
            }
        }
        return mobnames.get(mid);
    }

    public static boolean isNeedDropDataMob(final int mid) {
        switch (mid) {
            case 4110300://아이언 뮤테
            case 4110301://강화된 아이언 뮤테
            case 4110302://미스릴 뮤테
            case 4230600://모래거인
            case 5110301://로이드
            case 5110300://강화된 미스릴 뮤테
            case 5110302://네오 휴로이드
            case 6090000://리치
            case 6110300://호문
            case 6110301://사이티
            case 7110300://D.로이
            case 7110301://호문쿨루
            case 8110300://호문스큘러
                return true;
        }
        if (mid >= 3100101 && mid <= 3110303) { //모래 난쟁이부터
            return true;
        }
        return false;
    }

    public static boolean isDonNeedDropDataMob(final int mid) {
        switch (mid) {
            case 2110200://뿔버섯
            case 3110100://리게이터
            case 6090000://리치
                return true;
        }
        return false;
    }
}
