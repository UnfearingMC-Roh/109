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
package server.life;

import client.MapleCharacter;
import constants.GameConstants;
import handling.channel.ChannelServer;
import handling.world.World;
import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import provider.MapleData;
import provider.MapleDataDirectoryEntry;
import provider.MapleDataFileEntry;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import provider.MapleDataType;
import server.MedalRanking;
import server.Randomizer;
import server.Timer;
import server.marriage.MarriageManager;
import server.shops.MinervaOwlSearchTop;
import tools.Pair;
import tools.StringUtil;

public class MapleLifeFactory {

    private static final MapleDataProvider data = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/Mob.wz"));
    private static final MapleDataProvider npcData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/Npc.wz"));
    private static final MapleDataProvider stringDataWZ = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/String.wz"));
    private static final MapleDataProvider etcDataWZ = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/Etc.wz"));
    private static final MapleData mobStringData = stringDataWZ.getData("Mob.img");
    private static final MapleData npcStringData = stringDataWZ.getData("Npc.img");
    private static final MapleData npclocData = etcDataWZ.getData("NpcLocation.img");
    private static Map<Integer, String> npcNames = new HashMap<Integer, String>();
    private static Map<Integer, MapleMonsterStats> monsterStats = new HashMap<Integer, MapleMonsterStats>();
    private static Map<Integer, Integer> NPCLoc = new HashMap<Integer, Integer>();
    private static Map<Integer, List<Integer>> questCount = new HashMap<Integer, List<Integer>>();
    private static ScheduledFuture<?> autosave = null;
    private static int savetime = 0;
    
    public static void AutoSave() {//자동저장시스템
        if (autosave == null) {
            autosave = Timer.EtcTimer.getInstance().register(new Runnable() {
                @Override
                public void run() {
                    if (savetime == 0) {
                        savetime++;
                    } else {
                        try {
                            for (ChannelServer ch : ChannelServer.getAllInstances()) {
                                for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                                    chr.saveToDB(false, false);
                                }
                            }
                            World.Guild.save();
                            World.Alliance.save();
                         //   World.Family.save();
                            //MarriageManager.getInstance().saveAll();
                          //  MedalRanking.saveAll();
                            MinervaOwlSearchTop.getInstance().saveToFile();
                            for (int i = 1; i <= ChannelServer.getChannelCount(); i++) {
                                ChannelServer.getInstance(i).saveMerchant();
                           //  System.out.println("10 초 단위로 서버정보가 저장됩니다.");
                            }
                            //System.out.println("Saving merchant... 1 Channel");
                           System.out.println("6시간 단위로 서버정보가 저장됩니다.");
                        } catch (Exception e) {
                            System.err.println("자동저장 시스템 오류발생.");
                        }
                    }
                }
            }, 21600000); //6시간
        }
    }

    public static AbstractLoadedMapleLife getLife(int id, String type) {
        if (type.equalsIgnoreCase("n")) {
            return getNPC(id);
        } else if (type.equalsIgnoreCase("m")) {
            return getMonster(id);
        } else {
            System.err.println("Unknown Life type: " + type + "");
            return null;
        }
    }

    public static int getNPCLocation(int npcid) {
        if (NPCLoc.containsKey(npcid)) {
            return NPCLoc.get(npcid);
        }
        final int map = MapleDataTool.getIntConvert(Integer.toString(npcid) + "/0", npclocData, -1);
        NPCLoc.put(npcid, map);
        return map;
    }

    public static final void loadQuestCounts() {
        if (questCount.size() > 0) {
            return;
        }
        for (MapleDataDirectoryEntry mapz : data.getRoot().getSubdirectories()) {
            if (mapz.getName().equals("QuestCountGroup")) {
                for (MapleDataFileEntry entry : mapz.getFiles()) {
                    final int id = Integer.parseInt(entry.getName().substring(0, entry.getName().length() - 4));
                    MapleData dat = data.getData("QuestCountGroup/" + entry.getName());
                    if (dat != null && dat.getChildByPath("info") != null) {
                        List<Integer> z = new ArrayList<Integer>();
                        for (MapleData da : dat.getChildByPath("info")) {
                            z.add(MapleDataTool.getInt(da, 0));
                        }
                        questCount.put(id, z);
                    } else {
                        System.out.println("null questcountgroup");
                    }
                }
            }
        }
        for (MapleData c : npcStringData) {
            int nid = Integer.parseInt(c.getName());
            String n = StringUtil.getLeftPaddedStr(nid + ".img", '0', 11);
            try {
                if (npcData.getData(n) != null) {//only thing we really have to do is check if it exists. if we wanted to, we could get the script as well :3
                    String name = MapleDataTool.getString("name", c, "MISSINGNO");
                    if (name.contains("Maple TV") || name.contains("Baby Moon Bunny")) {
                        continue;
                    }
                    npcNames.put(nid, name);
                }
            } catch (NullPointerException e) {
            } catch (RuntimeException e) { //swallow, don't add if 
            }
        }
    }

    public static final List<Integer> getQuestCount(final int id) {
        return questCount.get(id);
    }

    public static MapleMonster getMonster(int mid) {
        MapleMonsterStats stats = getMonsterStats(mid);
        if (stats == null) {
            return null;
        }
        return new MapleMonster(mid, stats);
    }

    public static MapleMonsterStats getMonsterStats(int mid) {
        MapleMonsterStats stats = monsterStats.get(Integer.valueOf(mid));

        if (stats == null) {
            MapleData monsterData = null;
            try {
                monsterData = data.getData(StringUtil.getLeftPaddedStr(Integer.toString(mid) + ".img", '0', 11));
            } catch (RuntimeException e) {
                return null;
            }
            if (monsterData == null) {
                return null;
            }
            MapleData monsterInfoData = monsterData.getChildByPath("info");
            stats = new MapleMonsterStats(mid);
            
            switch (mid) { //////////////그저 구분...입니다ㅠㅠ
              case 8620012: //변형된 스텀피
                    stats.setHp(50000000000L); // 피통
                    stats.setExp(21000000); // 경험치
                    break;    
              case 9400266: //카무나
                    stats.setHp(1000000000000L); // 1조
                    stats.setExp(500000000); // 5억 경험치
                    break;     
              case 9400270: //칼리하
                    stats.setHp(2000000000000L); // 2조
                    stats.setExp(900000000); // 9억 경험치
                    break;
              case 9400293: //듀나스
                    stats.setHp(700000000000L); // 7000억
                    stats.setExp(1100000000); // 11억 경험치
                    break; 
              case 9400294: //듀나스
                    stats.setHp(900000000000L); // 7000억
                    stats.setExp(1100000000); // 11억 경험치
                    break; 
              case 9400295: //듀나스 겉이미지
                    stats.setHp(1000000000000L); // 1조
                    stats.setExp(1100000000); // 11억 경험치
                    break;       
              case 9400263: //베르가모트 겉이미지
                    stats.setHp(1000000000000L); // 1조
                    stats.setExp(1300000000); // 13억 경험치
                    break;
              case 9400264: //베르가모트
                    stats.setHp(1000000000000L); // 1조
                    stats.setExp(1300000000); // 13억 경험치
                    break;
              case 9400265: //베르가모트
                    stats.setHp(1400000000000L); // 1.4조
                    stats.setExp(1300000000); // 13억 경험치
                    break;
              case 9400271: //니벨룽겐
                    stats.setHp(1300000000000L); // 1.3조
                    stats.setExp(1500000000); // 15억 경험치
                    break;
              case 9400272: //니벨룽겐 겉이미지
                    stats.setHp(1400000000000L); // 1.4조
                    stats.setExp(1500000000); // 15억 경험치
                    break;
              case 9400273: //니벨룽겐
                    stats.setHp(2000000000000L); // 2조
                    stats.setExp(1500000000); // 15억 경험치
                    break;
              case 9400288: //로봇
                    stats.setHp(2000000000000L); // 2조
                    stats.setExp(1500000000); // 15억 경험치
                    break;
              case 9400296: //코어 블레이즈
                    stats.setHp(5000000000000L); // 5조
                    stats.setExp(1700000000); // 17억 경험치
                    break;
              case 9400376: //아우프헤븐
                    stats.setHp(4000000000000L); // 4조
                    stats.setExp(2100000000); // 21억 경험치
                    break;
              case 9400289: //아우프헤븐
                    stats.setHp(6000000000000L); // 6조
                    stats.setExp(2100000000); // 21억 경험치
                    break; 
              case 8850011: //여제
                    stats.setHp(30000000000000L); // 30조
                    stats.setExp(2100000000); // 21억 경험치
                    break; 
              case 8880000: //매그너스
                    stats.setHp(40000000000000L); // 40조
                    stats.setExp(2100000000); // 21억 경험치
                    break;   
              case 8880400: //진힐라
                    stats.setHp(55000000000000L); // 55조
                    stats.setExp(2100000000); // 21억 경험치
                    break;      
              case 9801028: //스우1
                    stats.setHp(20000000000000L); // 20조
                    stats.setExp(2100000000); // 21억 경험치
                    break;
              case 9801029: //스우2
                    stats.setHp(30000000000000L); // 30조
                    stats.setExp(2100000000); // 21억 경험치
                    break;  
              case 9801030: //스우3
                    stats.setHp(50000000000000L); // 50조
                    stats.setExp(2100000000); // 21억 경험치
                    break; 
              case 9300890: //데미안1
                    stats.setHp(60000000000000L); // 60조
                    stats.setExp(2100000000); // 21억 경험치
                    break; 
              case 9300891: //데미안1
                    stats.setHp(75000000000000L); // 75조
                    stats.setExp(2100000000); // 21억 경험치
                    break; 
              case 8880140: //루시드1
                    stats.setHp(80000000000000L); // 80조
                    stats.setExp(2100000000); // 21억 경험치
                    break; 
              case 8880150: //루시드2
                    stats.setHp(90000000000000L); // 90조
                    stats.setExp(2100000000); // 21억 경험치
                    break;  
              case 8880500: //검마2
                    stats.setHp(200000000000000L); // 200조
                    stats.setExp(2100000000); // 21억 경험치
                    break; 
              case 8880501: //검마1
                    stats.setHp(150000000000000L); // 150조
                    stats.setExp(2100000000); // 21억 경험치
                    break;      
                  //////////////보스///////////////
               //////////////잡몹///////////////   
              case 9420513: //라타니카
                    stats.setHp(1000000L); // 피통
                    stats.setExp(210000); // 경험치
                    break;
              case 9420514: //버서키
                    stats.setHp(32000); // 피통
                    stats.setExp(1450); // 경험치
                    break;
              case 9800078: //미래도시로봇
                    stats.setHp(32000); // 피통
                    stats.setExp(1450); // 경험치
                    break;   
              case 4250000: //이끼달팽이
                    stats.setHp(4000); // 피통
                    stats.setExp(200); // 경험치
                    break; 
              case 4250001: //트리로드
                    stats.setHp(6000); // 피통
                    stats.setExp(300); // 경험치
                    break;     
              case 5250000: //이끼버섯
                    stats.setHp(8000); // 피통
                    stats.setExp(400); // 경험치
                    break; 
              case 5250001: //스톤버그
                    stats.setHp(7000); // 피통
                    stats.setExp(350); // 경험치
                    break;
              case 5250002: //원시멧돼지
                    stats.setHp(13000); // 피통
                    stats.setExp(450); // 경험치
                    break;  
              case 5250003: //난폭한 원시멧돼지
                    stats.setHp(12000); // 피통
                    stats.setExp(403); // 경험치
                    break;    
               //////////////잡몹/////////////// 
               //////////////파헤///////////////
              case 8600000: //변형된 달팽이
                    stats.setHp(900000L); // 피통
                    stats.setExp(30000); // 경험치
                    break;
              case 8600001: //변형된 주황버섯
                    stats.setHp(1100000L); // 피통
                    stats.setExp(45000); // 경험치
                    break;
              case 8600002: //변형된 슬라임
                    stats.setHp(1300000L); // 피통
                    stats.setExp(63000); // 경험치
                    break;
              case 8600003: //변형된 리본돼지
                    stats.setHp(2000000L); // 피통
                    stats.setExp(90000); // 경험치
                    break;   
                  //////////////파헤///////////////
                  //////////////전당///////////////        
              case 8610005: //정식기사A
                    stats.setHp(140000L); // 피통
                    stats.setExp(70000);
                    break;
              case 8610006: //정식기사B
                    stats.setHp(140000L); // 피통
                    stats.setExp(70000); // 경험치
                    break;     
              case 8610007: //전투 기사단C
                    stats.setHp(150000L); // 피통
                    stats.setExp(110000); // 경험치 
                    break;
              case 8610008: //정식 기사D
                    stats.setHp(175000L); // 피통
                    stats.setExp(130000); // 경험치
                    break;
              case 8610009: //전투기사단D
                    stats.setHp(223000L); // 피통
                    stats.setExp(150000); // 경험치
                    break;
              case 8610010: //전투기사단D
                    stats.setHp(5000000L); // 피통
                    stats.setExp(550000); // 경험치
                    break;
              case 8610011: //전투기사단D
                    stats.setHp(5200000L); // 피통
                    stats.setExp(560000); // 경험치
                    break; 
              case 8610012: //전투기사단D
                    stats.setHp(5400000L); // 피통
                    stats.setExp(600000); // 경험치
                    break;  
              case 8610013: //전투기사단D
                    stats.setHp(5800000L); // 피통
                    stats.setExp(630000); // 경험치
                    break;
              case 8610014: //전투기사단D
                    stats.setHp(6000000L); // 피통
                    stats.setExp(730000); // 경험치
                    break;    
                  //////////////전당///////////////
                  //////////////헤이븐///////////////
              case 8250009: //미사일 안드로이드 레드
                    stats.setHp(800000000L); // 8억
                    stats.setExp(12000000); // 1200만
                    break; 
              case 8250007: //개조당한 안드로이드 수송차
                    stats.setHp(850000000L); // 8.5억
                    stats.setExp(14000000); // 1400만
                    break; 
              case 8250002: //개조당한 레이저 안드로이드
                    stats.setHp(800000000L); // 8억
                    stats.setExp(12000000); // 1200만
                    break; 
              case 8250001: //개조당한 실패작 안드로이드
                    stats.setHp(700000000L); // 7억
                    stats.setExp(10000000); // 1000만
                    break;
              case 8250000: //개조당한 겁쟁이 안드로이드
                    stats.setHp(700000000L); // 7억
                    stats.setExp(10000000); // 1000만
                    break; 
              case 8250006: //사냥개 로봇 블루
                    stats.setHp(800000000L); // 8억
                    stats.setExp(11000000); // 1000만
                    break; 
              case 8250004: //추적자 안드로이드 블루
                    stats.setHp(900000000L); // 9억
                    stats.setExp(15000000); // 1500만
                    break;   
              case 8250005: //사냥개 로봇 레드
                    stats.setHp(800000000L); // 8억
                    stats.setExp(11000000); // 1000만
                    break;    
              case 8250003: //추적자 안드로이드 레드
                    stats.setHp(900000000L); // 9억
                    stats.setExp(15000000); // 1500만
                    break; 
              case 8250013: //수리로봇
                    stats.setHp(1000000000L); // 10억
                    stats.setExp(17000000); // 1700만
                    break;       
              case 8250008: //개조당한 허세로이드
                    stats.setHp(1200000000L); // 12억
                    stats.setExp(20000000); // 2000만
                    break;        
                   //////////////헤이븐///////////////
                  //////////////뉴리프시티///////////////
               case 9400539: //도시버섯
                    stats.setHp(2000000000L); // 20억
                    stats.setExp(18000000); // 1800만
                    break; 
               case 9400538: //스트리트슬라임
                    stats.setHp(2100000000L); // 21억
                    stats.setExp(19000000); // 1900만
                    break;  
               case 9400547: //부머
                    stats.setHp(2100000000L); // 21억
                    stats.setExp(19000000); // 1900만
                    break;   
               case 9601253: //크리키아 정글 벌레
                    stats.setHp(2200000000L); // 22억
                    stats.setExp(19500000); // 1950만
                    break;   
               case 9400548: //마이티 메이플 이터
                    stats.setHp(2500000000L); // 25억
                    stats.setExp(20000000); // 2000만
                    break;     
               case 9601254: //사악한 정글 원숭이
                    stats.setHp(3000000000L); // 30억
                    stats.setExp(20500000); // 2050만
                    break;     
               case 9601255: //난폭한 정글 원숭이
                    stats.setHp(3500000000L); // 35억
                    stats.setExp(21000000); // 2100만
                    break;
               case 9601294: //사나운 그리폰
                    stats.setHp(3500000000L); // 35억
                    stats.setExp(21000000); // 2100만
                    break;  
               case 9400544: //그리폰
                    stats.setHp(3300000000L); // 33억
                    stats.setExp(20000000); // 2000만
                    break; 
               case 9601256: //정글베어워리어
                    stats.setHp(4500000000L); // 45억
                    stats.setExp(23000000); // 2300만
                    break;  
               case 9601257: //정글몽키워리어
                    stats.setHp(4800000000L); // 48억
                    stats.setExp(24000000); // 2400만
                    break;  
               case 9601258: //그리폰워리어
                    stats.setHp(5200000000L); // 52억
                    stats.setExp(25000000); // 2500만
                    break;      
                   //////////////뉴리프시티///////////////
                  //////////////소멸의여로///////////////
              case 8641000: //기쁨의 에르다스
                    stats.setHp(6000000000L); // 60억
                    stats.setExp(60000000); // 6000만
                    break; 
              case 8641001: //분노의 에르다스
                    stats.setHp(6500000000L); // 65억
                    stats.setExp(62000000); // 6200만
                    break; 
              case 8641002: //슬픔의 에르다스
                    stats.setHp(6700000000L); // 67억
                    stats.setExp(63000000); // 6300만
                    break;  
              case 8641003: //즐거움의 에르다스
                    stats.setHp(7000000000L); // 70억
                    stats.setExp(64000000); // 6400만
                    break;   
              case 8641004: //암석의 에르다스
                    stats.setHp(8000000000L); // 80억
                    stats.setExp(68000000); // 6800만
                    break;   
              case 8641005: //화염의 에르다스
                    stats.setHp(8400000000L); // 84억
                    stats.setExp(72000000); // 7200만
                    break; 
               case 8641006: //강인의 에르다스
                    stats.setHp(9000000000L); // 90억
                    stats.setExp(79000000); // 7900만
                    break;      
                   //////////////소멸의여로///////////////
                    //////////////츄츄///////////////
              case 8642016: //츄릅나무 츄츄아일랜드
                    stats.setHp(20000000000L); // 200억
                    stats.setExp(100000000); // 1억 경험치
                    break; 
              case 8642009: //츄츄아일랜드 블루 캣피쉬
                    stats.setHp(21000000000L); // 210억
                    stats.setExp(105000000); // 1.05억 경험치
                    break; 
              case 8642008: //츄츄아일랜드 그린 캣피쉬
                    stats.setHp(22000000000L); // 220억
                    stats.setExp(100000000); // 1.1억 경험치
                    break; 
              case 8642011: //츄츄아일랜드 대장 라이터틀
                    stats.setHp(25000000000L); // 250억
                    stats.setExp(125000000); // 1.25억 경험치
                    break; 
              case 8642010: //츄츄아일랜드 라이터틀
                    stats.setHp(23000000000L); // 230억
                    stats.setExp(115000000); // 1.15억 경험치
                    break; 
              case 8642000: //츄츄아일랜드 파인디어
                    stats.setHp(24000000000L); // 240억
                    stats.setExp(120000000); // 1.2억 경험치
                    break; 
              case 8642001: //츄츄아일랜드 큰뿔 파인디어
                    stats.setHp(25000000000L); // 250억
                    stats.setExp(123000000); // 1.23억 경험치
                    break; 
              case 8642002: //츄츄아일랜드 유나나
                    stats.setHp(26000000000L); // 260억
                    stats.setExp(128000000); // 1.28억 경험치
                    break;
               case 8642003: //츄츄아일랜드 램나나
                    stats.setHp(27000000000L); // 270억
                    stats.setExp(134000000); // 1.34억 경험치
                    break;
               case 8642006: //츄츄아일랜드 설익은 울프룻
                    stats.setHp(29000000000L); // 290억
                    stats.setExp(140000000); // 1.4억 경험치
                    break;
               case 8642007: //츄츄아일랜드 잘익은 울프룻
                    stats.setHp(32000000000L); // 320억
                    stats.setExp(144000000); // 1.44억 경험치
                    break;
               case 8642004: //츄츄아일랜드 플리온
                    stats.setHp(38000000000L); // 380억
                    stats.setExp(150000000); // 1.5억 경험치
                    break;
               case 8642005: //츄츄아일랜드 성난 플리온
                    stats.setHp(40000000000L); // 400억
                    stats.setExp(158000000); // 1.58억 경험치
                    break;
               case 8642012: //츄츄아일랜드 크릴라
                    stats.setHp(40000000000L); // 400억
                    stats.setExp(158000000); // 1.58억 경험치
                    break;
               case 8642013: //츄츄아일랜드 족장 크릴라
                    stats.setHp(45000000000L); // 450억
                    stats.setExp(170000000); // 1.7억 경험치
                    break;
               case 8642015: //츄츄아일랜드 족장 버샤크
                    stats.setHp(47000000000L); // 470억
                    stats.setExp(180000000); // 1.8억 경험치
                    break;
               case 8642014: //츄츄아일랜드 버샤크
                    stats.setHp(42000000000L); // 420억
                    stats.setExp(162000000); // 1.62억 경험치
                    break;
                   //////////////츄츄///////////////
                    //////////////레헬른///////////////
              case 8643003: //레헬른 갈리나
                    stats.setHp(120000000000L); // 1200억
                    stats.setExp(212000000); // 2.12억 경험치
                    break;   
              case 8643004: //레헬른 갈루스
                    stats.setHp(130000000000L); // 1300억
                    stats.setExp(222000000); // 2.22억 경험치
                    break;   
              case 8643005: //레헬른성난 우승접시
                    stats.setHp(140000000000L); // 1400억
                    stats.setExp(234000000); // 2.34억 경험치
                    break; 
              case 8643006: //레헬른 삐뚫어진 우승접시
                    stats.setHp(150000000000L); // 1500억
                    stats.setExp(237000000); // 2.37억 경험치
                    break; 
              case 8643001: //레헬른 종이봉투 뒷골목주인
                    stats.setHp(150000000000L); // 1500억
                    stats.setExp(237000000); // 2.37억 경험치
                    break; 
              case 8643002: //레헬른 나무판자 뒷골목주인
                    stats.setHp(160000000000L); // 1600억
                    stats.setExp(244000000); // 2.44억 경험치
                    break; 
              case 8643008: //레헬른 성난 무도회주민
                    stats.setHp(175000000000L); // 1750억
                    stats.setExp(252000000); // 2.52억 경험치
                    break; 
              case 8643009: //레헬른 광기의 무도회주민
                    stats.setHp(180000000000L); // 1800억
                    stats.setExp(256000000); // 2.56억 경험치
                    break; 
              case 8643012: //레헬른 약화된 클리너
                    stats.setHp(200000000000L); // 2000억
                    stats.setExp(268000000); // 2.68억 경험치
                    break; 
              case 8643007: //레헬른 춤추는 빨간구두
                    stats.setHp(187000000000L); // 1870억
                    stats.setExp(261000000); // 2.61억 경험치
                    break; 
              case 8643000: //레헬른 클리너
                    stats.setHp(250000000000L); // 2500억
                    stats.setExp(290000000); // 2.9억 경험치
                    break; 
              case 8643010: //레헬른 푸른눈의 가고일
                    stats.setHp(260000000000L); // 2600억
                    stats.setExp(295000000); // 2.95억 경험치
                    break; 
               case 8643011: //레헬른 붉은눈의 가고일
                    stats.setHp(270000000000L); // 2700억
                    stats.setExp(300000000); // 3억 경험치
                    break; 
                   //////////////레헬른///////////////
                    //////////////아르카나///////////////
              case 8644000: //아르카나 물의정령
                    stats.setHp(450000000000L); // 4500억
                    stats.setExp(340000000); // 3.4억 경험치
                    break;          
              case 8644001: //아르카나 햇살의 정령
                    stats.setHp(455000000000L); // 4550억
                    stats.setExp(347000000); // 3.47억 경험치
                    break;     
              case 8644002: //아르카나 흙의정령
                    stats.setHp(463000000000L); // 4630억
                    stats.setExp(352000000); // 3.52억 경험치
                    break;   
              case 8644007: //아르카나 혼돈의 정령
                    stats.setHp(490000000000L); // 4900억
                    stats.setExp(362000000); // 3.62억 경험치
                    break;  
              case 8644009: //아르카나 비탄의 정령
                    stats.setHp(500000000000L); // 5000억
                    stats.setExp(369000000); // 3.69억 경험치
                    break;
              case 8644008: //아르카나 절망의 정령
                    stats.setHp(550000000000L); // 5500억
                    stats.setExp(378000000); // 3.78억 경험치
                    break; 
                  //////////////아르카나///////////////
                 /* //////////////무릉도장///////////////
              case 9300184: //마노 1층
                    stats.setHp(450000000000L); // 4500억
                    stats.setExp(2000000000); // 20억
                    break; 
              case 9300185: //스텀피2층
                    stats.setHp(450000000000L); // 4500억
                    stats.setExp(2000000000); // 20억
                    break;   
              case 9300186: //데우 3층
                    stats.setHp(450000000000L); // 4500억
                    stats.setExp(2000000000); // 20억
                    break;    
              case 9300187: //킹슬라임 4층
                    stats.setHp(450000000000L); // 4500억
                    stats.setExp(2000000000); // 20억
                    break;   
              case 9300188: //대왕지네
                    stats.setHp(450000000000L); // 4500억
                    stats.setExp(2000000000); // 20억
                    break;    
              case 9300189: //파우스트
                    stats.setHp(450000000000L); // 4500억
                    stats.setExp(2000000000); // 20억
                    break;  
              case 9300190: //킹크랑
                    stats.setHp(450000000000L); // 4500억
                    stats.setExp(2000000000); // 20억
                    break;  */     


                default:
                    if (MapleDataTool.getLongConvert("finalmaxHP", monsterInfoData) > 0L) {
                        stats.setHp(MapleDataTool.getLongConvert("finalmaxHP", monsterInfoData));
                    } else {
                        stats.setHp(GameConstants.getPartyPlayHP(mid) > 0 ? GameConstants.getPartyPlayHP(mid) : MapleDataTool.getIntConvert("maxHP", monsterInfoData));
                    }
                    stats.setMp(MapleDataTool.getIntConvert("maxMP", monsterInfoData, 0));
                    stats.setExp(mid == 9300027 ? 0 : (GameConstants.getPartyPlayEXP(mid) > 0 ? GameConstants.getPartyPlayEXP(mid) : MapleDataTool.getIntConvert("exp", monsterInfoData, 0)));
                  //  stats.setPhysicalAttack(MapleDataTool.getIntConvert("PADamage", monsterInfoData, 0));
                   // stats.setMagicAttack(MapleDataTool.getIntConvert("MADamage", monsterInfoData, 0));
                    break;
            }

            //stats.setHp(GameConstants.getPartyPlayHP(mid) > 0 ? GameConstants.getPartyPlayHP(mid) : MapleDataTool.getIntConvert("maxHP", monsterInfoData));
            //stats.setMp(MapleDataTool.getIntConvert("maxMP", monsterInfoData, 0));
            //stats.setExp(mid == 9300027 ? 0 : (GameConstants.getPartyPlayEXP(mid) > 0 ? GameConstants.getPartyPlayEXP(mid) : MapleDataTool.getIntConvert("exp", monsterInfoData, 0)));
            stats.setLevel((short) MapleDataTool.getIntConvert("level", monsterInfoData, 1));
            stats.setCharismaEXP((short) MapleDataTool.getIntConvert("charismaEXP", monsterInfoData, 0));
            stats.setRemoveAfter(MapleDataTool.getIntConvert("removeAfter", monsterInfoData, 0));
            stats.setrareItemDropLevel((byte) MapleDataTool.getIntConvert("rareItemDropLevel", monsterInfoData, 0));
            stats.setFixedDamage(MapleDataTool.getIntConvert("fixedDamage", monsterInfoData, -1));
            stats.setLink(MapleDataTool.getIntConvert("link", monsterInfoData, 0));
            stats.setPDDamage(MapleDataTool.getIntConvert("PDDamage", monsterInfoData, 0));
            stats.setMDDamage(MapleDataTool.getIntConvert("MDDamage", monsterInfoData, 0));
            stats.setOnlyNormalAttack(MapleDataTool.getIntConvert("onlyNormalAttack", monsterInfoData, 0) > 0);
            stats.setBoss(GameConstants.getPartyPlayHP(mid) > 0 || MapleDataTool.getIntConvert("boss", monsterInfoData, 0) > 0 || mid == 8810018 || mid == 8810214 || mid == 9410066 || (mid >= 8810118 && mid <= 8810122));
            stats.setExplosiveReward(MapleDataTool.getIntConvert("explosiveReward", monsterInfoData, 0) > 0);
            stats.setUndead(MapleDataTool.getIntConvert("undead", monsterInfoData, 0) > 0);
            stats.setEscort(MapleDataTool.getIntConvert("escort", monsterInfoData, 0) > 0);
            stats.setPartyBonus(GameConstants.getPartyPlayHP(mid) > 0 || MapleDataTool.getIntConvert("partyBonusMob", monsterInfoData, 0) > 0);
            stats.setPartyBonusRate(MapleDataTool.getIntConvert("partyBonusR", monsterInfoData, 0));
            if (mobStringData.getChildByPath(String.valueOf(mid)) != null) {
                stats.setName(MapleDataTool.getString("name", mobStringData.getChildByPath(String.valueOf(mid)), "MISSINGNO"));
            }
            stats.setBuffToGive(MapleDataTool.getIntConvert("buff", monsterInfoData, -1));
            stats.setChange(MapleDataTool.getIntConvert("changeableMob", monsterInfoData, 0) > 0);
            stats.setFriendly(MapleDataTool.getIntConvert("damagedByMob", monsterInfoData, 0) > 0);
            stats.setNoDoom(MapleDataTool.getIntConvert("noDoom", monsterInfoData, 0) > 0);
            stats.setFfaLoot(MapleDataTool.getIntConvert("publicReward", monsterInfoData, 0) > 0);
            stats.setCP((byte) MapleDataTool.getIntConvert("getCP", monsterInfoData, 0));
            stats.setPoint(MapleDataTool.getIntConvert("point", monsterInfoData, 0));
            stats.setDropItemPeriod(MapleDataTool.getIntConvert("dropItemPeriod", monsterInfoData, 0));
            stats.setPhysicalAttack(MapleDataTool.getIntConvert("PADamage", monsterInfoData, 0));
            stats.setMagicAttack(MapleDataTool.getIntConvert("MADamage", monsterInfoData, 0));
            stats.setPDRate((byte) MapleDataTool.getIntConvert("PDRate", monsterInfoData, 0));
            stats.setMDRate((byte) MapleDataTool.getIntConvert("MDRate", monsterInfoData, 0));
            stats.setAcc(MapleDataTool.getIntConvert("acc", monsterInfoData, 0));
            stats.setEva(MapleDataTool.getIntConvert("eva", monsterInfoData, 0));
            stats.setSummonType((byte) MapleDataTool.getIntConvert("summonType", monsterInfoData, 0));
            stats.setCategory((byte) MapleDataTool.getIntConvert("category", monsterInfoData, 0));
            stats.setSpeed(MapleDataTool.getIntConvert("speed", monsterInfoData, 0));
            stats.setPushed(MapleDataTool.getIntConvert("pushed", monsterInfoData, 0));
            //final boolean hideHP = MapleDataTool.getIntConvert("HPgaugeHide", monsterInfoData, 0) > 0 || MapleDataTool.getIntConvert("hideHP", monsterInfoData, 0) > 0;
            final MapleData selfd = monsterInfoData.getChildByPath("selfDestruction");
            if (selfd != null) {
                stats.setSelfDHP(MapleDataTool.getIntConvert("hp", selfd, 0));
                stats.setRemoveAfter(MapleDataTool.getIntConvert("removeAfter", selfd, stats.getRemoveAfter()));
                stats.setSelfD((byte) MapleDataTool.getIntConvert("action", selfd, -1));
            } else {
                stats.setSelfD((byte) -1);
            }
            final MapleData firstAttackData = monsterInfoData.getChildByPath("firstAttack");
            if (firstAttackData != null) {
                if (firstAttackData.getType() == MapleDataType.FLOAT) {
                    stats.setFirstAttack(Math.round(MapleDataTool.getFloat(firstAttackData)) > 0);
                } else {
                    stats.setFirstAttack(MapleDataTool.getInt(firstAttackData) > 0);
                }
            }
            if (stats.isBoss() || isDmgSponge(mid)) {
                if (/*hideHP || */monsterInfoData.getChildByPath("hpTagColor") == null || monsterInfoData.getChildByPath("hpTagBgcolor") == null) {
                    stats.setTagColor(0);
                    stats.setTagBgColor(0);
                } else {
                    stats.setTagColor(MapleDataTool.getIntConvert("hpTagColor", monsterInfoData));
                    stats.setTagBgColor(MapleDataTool.getIntConvert("hpTagBgcolor", monsterInfoData));
                }
            }

            final MapleData banishData = monsterInfoData.getChildByPath("ban");
            if (banishData != null) {
                stats.setBanishInfo(new BanishInfo(
                        MapleDataTool.getString("banMsg", banishData),
                        MapleDataTool.getInt("banMap/0/field", banishData, -1),
                        MapleDataTool.getString("banMap/0/portal", banishData, "sp")));
            }

            final MapleData reviveInfo = monsterInfoData.getChildByPath("revive");
            if (reviveInfo != null) {
                List<Integer> revives = new LinkedList<Integer>();
                for (MapleData bdata : reviveInfo) {
                    revives.add(MapleDataTool.getInt(bdata));
                }
                stats.setRevives(revives);
            }

            final MapleData monsterSkillData = monsterInfoData.getChildByPath("skill");
            if (monsterSkillData != null) {
                int i = 0;
                List<Pair<Integer, Integer>> skills = new ArrayList<Pair<Integer, Integer>>();
                while (monsterSkillData.getChildByPath(Integer.toString(i)) != null) {
                    skills.add(new Pair<Integer, Integer>(Integer.valueOf(MapleDataTool.getInt(i + "/skill", monsterSkillData, 0)), Integer.valueOf(MapleDataTool.getInt(i + "/level", monsterSkillData, 0))));
                    i++;
                }
                stats.setSkills(skills);
            }

            decodeElementalString(stats, MapleDataTool.getString("elemAttr", monsterInfoData, ""));

            // Other data which isn;t in the mob, but might in the linked data
            final int link = MapleDataTool.getIntConvert("link", monsterInfoData, 0);
            if (link != 0) { // Store another copy, for faster processing.
                monsterData = data.getData(StringUtil.getLeftPaddedStr(link + ".img", '0', 11));
            }

            for (MapleData idata : monsterData) {
                if (idata.getName().equals("fly")) {
                    stats.setFly(true);
                    stats.setMobile(true);
                    break;
                } else if (idata.getName().equals("move")) {
                    stats.setMobile(true);
                }
            }

            for (int i = 1; true; i++) {
                final MapleData attackData = monsterData.getChildByPath("attack" + i + "/info");
                if (attackData == null) {
                    break;
                }
                final MobAttackInfo ret = new MobAttackInfo();
                ret.setDeadlyAttack(attackData.getChildByPath("deadlyAttack") != null);
                ret.setMpBurn(MapleDataTool.getInt("mpBurn", attackData, 0));
                ret.setDiseaseSkill(MapleDataTool.getInt("disease", attackData, 0));
                ret.setDiseaseLevel(MapleDataTool.getInt("level", attackData, 0));
                ret.setMpCon(MapleDataTool.getInt("conMP", attackData, 0));
                ret.attackAfter = MapleDataTool.getInt("attackAfter", attackData, 0);
                ret.PADamage = MapleDataTool.getInt("PADamage", attackData, 0);
                ret.MADamage = MapleDataTool.getInt("PADamage", attackData, 0);
                ret.magic = MapleDataTool.getInt("magic", attackData, 0) > 0;
                if (attackData.getChildByPath("range") != null) {
                    ret.range = MapleDataTool.getInt("range/r", attackData, 0);
                    if (attackData.getChildByPath("range/lt") != null && attackData.getChildByPath("range/rb") != null) {
                        ret.lt = (Point) attackData.getChildByPath("range/lt").getData();
                        ret.rb = (Point) attackData.getChildByPath("range/rb").getData();
                    }
                }
                stats.addMobAttack(ret);
            }

            byte hpdisplaytype = -1;
            if (stats.getTagColor() > 0 || mid == 8830000 || mid == 8830001 || mid == 8830002 || mid == 8830007) {
                hpdisplaytype = 0;
            } else if (stats.isFriendly()) {
                hpdisplaytype = 1;
            } else if (mid >= 9300184 && mid <= 9300215) { // Mulung TC mobs
                hpdisplaytype = 2;
            } else if (!stats.isBoss() || mid == 9410066 || stats.isPartyBonus()) { // Not boss and dong dong chiang
                hpdisplaytype = 3;
            }
            stats.setHPDisplayType(hpdisplaytype);

            monsterStats.put(Integer.valueOf(mid), stats);
        }
        return stats;
    }

    public static final void decodeElementalString(MapleMonsterStats stats, String elemAttr) {
        for (int i = 0; i < elemAttr.length(); i += 2) {
            stats.setEffectiveness(
                    Element.getFromChar(elemAttr.charAt(i)),
                    ElementalEffectiveness.getByNumber(Integer.valueOf(String.valueOf(elemAttr.charAt(i + 1)))));
        }
    }

    private static final boolean isDmgSponge(final int mid) {
        switch (mid) {
            case 8810018:
            case 8810118:
            case 8810119:
            case 8810120:
            case 8810121:
            case 8810122:
            case 8820009:
            case 8820010:
            case 8820011:
            case 8820012:
            case 8820013:
            case 8820014:
                return true;
        }
        return false;
    }

    public static MapleNPC getNPC(final int nid) {
        String name = npcNames.get(nid);
        if (name == null) {
            return null;
        }
        MapleNPC npc = new MapleNPC(nid, name);
        MapleData npcD = npcData.getData(StringUtil.getLeftPaddedStr(Integer.toString(nid) + ".img", '0', 11));
        if (npcD != null) {
            boolean isMove = npcD.getChildByPath("move") != null;
            npc.setMove(isMove);
        }
        return npc;
    }

    public static int getRandomNPC() {
        List<Integer> vals = new ArrayList<Integer>(npcNames.keySet());
        int ret = 0;
        while (ret <= 0) {
            ret = vals.get(Randomizer.nextInt(vals.size()));
            if (npcNames.get(ret).contains("MISSINGNO")) {
                ret = 0;
            }
        }
        return ret;
    }
}
