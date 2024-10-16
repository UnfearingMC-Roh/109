/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.customize;

import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleQuestStatus;
import client.MapleStat;
import client.PlayerStats;
import client.Skill;
import client.SkillFactory;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import constants.GameConstants;
import handling.world.World;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import tools.MaplePacketCreator;
import tools.packet.TemporaryStatsPacket;

/**
 *
 * @author jch50
 */
public class CustomizeStat {

    public MapleCharacter user;
    public PlayerStats stat;
    public long tUpdate;

    public int pad;
    public int mad;
    public int booster;
    public int maxHPMP;
    public int STR, DEX, INT, LUK;
    public int exp, meso, drop, damage, bossDamage;
    public long addDamage;

//    public Map<MapleBuffStat, Map<Integer, Integer>> lCTS;
    public CustomizeStat(MapleCharacter user) {
        this.user = user;
        this.stat = user.getStat();
        this.tUpdate = System.currentTimeMillis();
        //initialize Values
        this.pad = 0;
        this.mad = 0;
        this.booster = 0;
        this.maxHPMP = 0;
        this.STR = 0;
        this.DEX = 0;
        this.INT = 0;
        this.LUK = 0;
        this.exp = 0;
        this.meso = 0;
        this.drop = 0;
        this.damage = 0;
        this.bossDamage = 0;
        this.addDamage = 0;
    }

    public void setCTS(MapleBuffStat cts, int skillID, int val, boolean delete) {
        boolean update = false;
        Map<Integer, Integer> mapData;
        if (!user.lCTS.containsKey(cts)) {
            if (!delete) {
                //제일 처음으로 해당 버프 사용
                mapData = new HashMap<>();
                mapData.put(skillID, val);
                user.lCTS.put(cts, mapData);
                update = true;
            }
        } else {
            mapData = user.lCTS.get(cts);
            if (mapData.containsKey(skillID)) {
                if (delete) {
                    mapData.remove(skillID);
                    user.lCTS.put(cts, mapData);
                    update = true;
                } else {
                    int oldVal = mapData.get(skillID);
                    if (val != oldVal) {
                        //해당 버프를 포함한 해당 스킬 사용했을 때 이전 값과 현재 값이 다를 경우
                        mapData.remove(skillID);
                        mapData.put(skillID, val);
                        user.lCTS.put(cts, mapData);
                        update = true;
                    }
                }
            } else {
                //제일 처음으로 해당 버프를 포함한 해당 스킬 사용
                if (!delete) {
                    mapData.put(skillID, val);
                    user.lCTS.put(cts, mapData);
                    update = true;
                }
            }
        }

        if (update) {
            if (delete) { //이걸 통해서 부스터 값 ㅈㄴ 왔다갔다하는거 해결
                if (skillID == 1320009
                        || skillID == 32111005) {
                    setFrom(0, false);
                } else {
                    setFrom(System.currentTimeMillis(), false);
                }
            } else {
                setFrom(0, false);
            }
        }
    }

    public boolean isUpdate(long tCur) {
        int tLimitUpdate = 1 * 1000; //마지막 업데이트로부터 30초가 지났냐 안지났냐 따짐
        return tCur == 0 || tUpdate + tLimitUpdate <= tCur;
    }

    public void setFrom(long tCur, boolean inChat) {
        if (!user.isAlive()) {
            return;
        }

        /*
         boolean stanceSkill = false;
         for (int i = 0; i < 6; i++) {
         if (user.getInventory(MapleInventoryType.SETUP).findById(3960096 + i) != null) {
         stanceSkill = true;
         user.changeSkillLevel_Skip(SkillFactory.getSkill(user.getStat().getSkillByJob(1210, user.getJob())), (byte) 1, (byte) 0, true);
         }
         }
         if (!stanceSkill) {
         user.changeSkillLevel_Skip(SkillFactory.getSkill(user.getStat().getSkillByJob(1210, user.getJob())), (byte) -1, (byte) -1, true);
         }
         */
        if (!isUpdate(tCur)) {
//            if (user.isGM()) {
//                user.dropMessage(5, "STR = " + STR);
//                user.dropMessage(5, "DEX = " + DEX);
//                user.dropMessage(5, "INT = " + INT);
//                user.dropMessage(5, "LUK = " + LUK);
//                user.dropMessage(5, "pad = " + pad);
//                user.dropMessage(5, "mad = " + mad);
//                user.dropMessage(5, "booster = " + booster);
//                user.dropMessage(5, "lCTS = " + lCTS.toString());
//            }
//            user.getClient().sendPacket(MaplePacketCreator.enableActions());
            return;
        }

        if (tCur == 0) {
            this.tUpdate = System.currentTimeMillis();
        } else {
            this.tUpdate = tCur;
        }
        this.pad = 0;
        this.mad = 0;
        this.booster = 0;
        this.maxHPMP = 0;
        this.STR = 0;
        this.DEX = 0;
        this.INT = 0;
        this.LUK = 0;
        this.exp = 0;
        this.meso = 0;
        this.drop = 0;
        this.damage = 0;
        this.bossDamage = 0;

        //오브 계산 시작
        MapleInventory inv = user.getInventory(MapleInventoryType.CASH);
        if (inv != null) {
            int orbCount = 0;
            for (short i = 0; i < inv.getSlotLimit(); i++) {
                if (orbCount >= 5 + user.getAddOrbCount()) {
                    break;
                }

                Item item;
                if ((item = inv.getItem(i)) != null) {
                    int itemID = item.getItemId();
                    String orbStat = item.getOwner();
                    if (itemID == 5150054) {
                        if (user.getemblem() + user.getAddOrbCount() == orbCount) {
                            break;
                        }
                        if (orbStat.equals("")) {
                            continue;
                        }

                        String[] data = orbStat.split(" ");
                        String sGrade = data[0].replaceAll("등급", "");
                        String sStatName = data[1];
                        String sStatVal = data[2].replaceAll("%", "").replaceAll("증가", "");

                        int nGrade, nStatVal;
                        try {
                            nGrade = Integer.parseInt(sGrade);
                            nStatVal = Integer.parseInt(sStatVal);

                            if (sStatName.equals("힘")) {
                                if (nGrade >= 8 && nGrade <= 10) {
                                    this.STR += stat.getStr() * (nStatVal / 100.0);
                                } else {
                                    this.STR += nStatVal;
                                }
                            } else if (sStatName.equals("덱스")) {
                                if (nGrade >= 8 && nGrade <= 10) {
                                    this.DEX += stat.getDex() * (nStatVal / 100.0);
                                } else {
                                    this.DEX += nStatVal;
                                }
                            } else if (sStatName.equals("인트")) {
                                if (nGrade >= 8 && nGrade <= 10) {
                                    this.INT += stat.getInt() * (nStatVal / 100.0);
                                } else {
                                    this.INT += nStatVal;
                                }
                            } else if (sStatName.equals("럭")) {
                                if (nGrade >= 8 && nGrade <= 10) {
                                    this.LUK += stat.getLuk() * (nStatVal / 100.0);
                                } else {
                                    this.LUK += nStatVal;
                                }
                            }
                        } catch (NumberFormatException e) {
                            continue;
                        }
                        orbCount++;
                    }
                }
            }
        }
        //오브 계산 종료

        //길드 버프 계산 시작
        if (user.getGuild() != null) {
            if (user.guild_buff > 0) {
                this.pad += user.guild_stat_watk;
                this.mad += user.guild_stat_matk;
            }
        }
        //길드 버프 계산 종료

        //소울 계산 시작
        int soulItemID = 0;
        int soulBaseItemID = 5010809;
        if (soulItemID > soulBaseItemID) {
            int allStat = 200, pmad = 20;
            int soulItemLV = soulItemID - soulBaseItemID;
            this.STR += allStat * soulItemLV;
            this.DEX += allStat * soulItemLV;
            this.INT += allStat * soulItemLV;
            this.LUK += allStat * soulItemLV;
            this.pad += pmad * soulItemLV;
            this.mad += pmad * soulItemLV;
        }
        //소울 계산 종료

        //라이딩 장비 계산 시작
        short[] tamingMobEquip = {18, 19, 20};
        if (user.getBuffedValue(MapleBuffStat.MONSTER_RIDING) == null) { //라이딩 안탔을 때만
            inv = user.getInventory(MapleInventoryType.EQUIPPED);
            if (inv != null) {
                for (int i = 0; i < tamingMobEquip.length; i++) {
                    Equip item = (Equip) inv.getItem((short) -tamingMobEquip[i]);
                    if (item != null) {
                        this.pad += item.getWatk();
                        this.mad += item.getMatk();
                    }
                }
            }
        }
        //라이딩 장비 계산 종료

        //심볼 계산 시작
        int syPAD = 0, syMAD = 0;
        int sySTR = 0, syDEX = 0, syINT = 0, syLUK = 0, syEXP = 0, syMeso = 0;
        stat.setSimbolStr(sySTR);
        stat.setSimbolDex(syDEX);
        stat.setSimbolInt(syINT);
        stat.setSimbolLuk(syLUK);
        user.setSysbolExp(syEXP);
        user.setSysbolMeso(syMeso);
        user.setSysbolDrop(this.drop);
        int bossDAMr = 0;
        int ignoreTargetDEF = 0;
        int[][] symbol = MapleCharacter.symbolSkill;
        for (int i = 0; i < symbol.length; i++) {
            int itemID = symbol[i][0];
            int skillID = symbol[i][1];
            int allStat = symbol[i][2];
            int exp = symbol[i][3];
            int meso = symbol[i][4];
            if (user.haveItem(itemID)) {
                sySTR += allStat;
                syDEX += allStat;
                syINT += allStat;
                syLUK += allStat;
                syEXP += exp;
                syMeso += meso;

                this.STR += allStat;
                this.DEX += allStat;
                this.INT += allStat;
                this.LUK += allStat;

                int jobSkillID = PlayerStats.getSkillByJob(skillID, user.getJob());
                Skill incSkill = SkillFactory.getSkill(jobSkillID);
                if (user.getSkillLevel(jobSkillID) <= 0) {
                    user.changeSkillLevel(incSkill, (byte) 1, (byte) 1);
                }
                if (incSkill != null) { //심볼의 모든 스킬은 보공/방무
                    MapleStatEffect incEffect = incSkill.getEffect(1);
                    if (incEffect != null) {
                        //여긴 메시지 출력용
                        bossDAMr += incEffect.getDAMRate();
                        ignoreTargetDEF += incEffect.getIgnoreMob();
                        syPAD += incEffect.getAttackX();
                        syMAD += incEffect.getMagicX();
                    }
                }
            } else {
                int jobSkillID = PlayerStats.getSkillByJob(skillID, user.getJob());
                Skill incSkill = SkillFactory.getSkill(jobSkillID);
                if (incSkill != null) {
                    if (user.getSkillLevel(incSkill) > 0) {
                        user.changeSkillLevel(incSkill, (byte) 0, (byte) 0);
                    }
                }
            }
        }
        stat.setSimbolStr(sySTR);
        stat.setSimbolDex(syDEX);
        stat.setSimbolInt(syINT);
        stat.setSimbolLuk(syLUK);
        user.setSysbolExp(syEXP);
        user.setSysbolMeso(syMeso);
        user.setSysbolDrop(this.drop);
        //심볼 계산 종료

        //수집 스텟 계산시작
        int collectionKeySize = 3;
        String collectionBaseKey = "books";
        for (int i = 1; i <= collectionKeySize; i++) {
            String key = String.valueOf(collectionBaseKey + i);
            String val = user.getKeyValue(key);
            if (val != null) {
                String[] data = val.split(",");
                for (int j = 0; j < data.length; j++) {
                    int allStat;//올스텟
                    if ((allStat = Integer.parseInt(data[j])) > 0) {
                        this.STR += allStat;
                        this.DEX += allStat;
                        this.INT += allStat;
                        this.LUK += allStat;
                    }
                    int pad;//북 1,2,3 수집 공격력
                    if ((pad = Integer.parseInt(data[j])) > 0) {
                        this.pad += 2;
                    }
                    int mad;//북 1,2,3 수집 마력
                    if ((mad = Integer.parseInt(data[j])) > 0) {
                        this.mad += 2;
                    }
                }
            }
        }
        String collectionKey = "booss1";
        String collectionVal = user.getKeyValue(collectionKey);
        if (collectionVal != null) {
            String[] data = collectionVal.split(",");
            for (int i = 0; i < data.length; i++) {
                int allStat;//올스텟
                if ((allStat = Integer.parseInt(data[i])) > 0) {
                    this.STR += allStat;
                    this.DEX += allStat;
                    this.INT += allStat;
                    this.LUK += allStat;
                    this.pad += allStat;
                    this.mad += allStat;
                }
            }
        }
        String[] newCollectionKeys = {"CollectionAllStat", "CollectionPAD", "CollectionMAD"};
        for (int i = 0; i < newCollectionKeys.length; i++) {
            String newCollectionKey = newCollectionKeys[i];
            if (user.getKeyValue(newCollectionKey) != null) {
                try {
                    int val = Integer.parseInt(user.getKeyValue(newCollectionKey));
                    switch (i) {
                        case 0: //올스탯
                            this.STR += val;
                            this.DEX += val;
                            this.INT += val;
                            this.LUK += val;
                            break;
                        case 1: //공격력
                            this.pad += val;
                            break;
                        case 2: //마력
                            this.mad += val;
                            break;
                    }
                } catch (NumberFormatException e) {

                }
            }
        }

        //퀘스트 커스텀 데이터를 이용한 컬렉션
        int[][] collectionKeyAndVal = {
            {221018, 30}, {221019, 40}, {221020, 100}
        };
        for (int i = 0; i < collectionKeyAndVal.length; i++) {
            MapleQuestStatus status;
            if ((status = user.getQuest(collectionKeyAndVal[i][0])) != null) {
                int state = Integer.parseInt(status.getCustomData());
                if (state != 0) {
                    int allStat = collectionKeyAndVal[i][1];

                    this.STR += allStat;
                    this.DEX += allStat;
                    this.INT += allStat;
                    this.LUK += allStat;
                    this.pad += allStat;
                    this.mad += allStat;
                }
            }
        }
        MapleQuestStatus status;
        if ((status = user.getQuest(99898)) != null) {
            int val = Integer.parseInt(status.getCustomData());
            this.pad += val;
            this.mad += val * 2;
        }
        //컬렉션 스탯 계산 종료

        //아케인 심볼 계산 시작
        for (int[] arcane : World.ArcaneSimbol.getItems().values()) {
            if (user.haveItem(arcane[0])) {
                this.STR += arcane[1];
                this.DEX += arcane[2];
                this.INT += arcane[3];
                this.LUK += arcane[4];
                this.pad += arcane[5];
                this.mad += arcane[6];
            }
        }
        //아케인 심볼 계산 종료

        //소울에서 아획 = drop, 메획 = meso //소울소스
        int activeEffectItemID = user.getItemEffect();
        if (user.haveItem(activeEffectItemID)) {
            switch (activeEffectItemID) {
                case 5010801://카오스 자쿰의 소울
                    this.STR += 50;
                    this.DEX += 50;
                    this.INT += 50;
                    this.LUK += 50;
                    this.pad += 10;
                    this.mad += 10;
                    this.addDamage = 599999; // 혹시 이거 바로적용하는게 어떤버튼인가요?>?? 컨트롤+s저장누르고
                    break;
                case 5010813://궁극의 모험가 소울
                    this.STR += 65;
                    this.DEX += 65;
                    this.INT += 65;
                    this.LUK += 65;
                    this.pad += 10;
                    this.mad += 10;
                    break;
                case 5010802://카오스 혼테일의 소울
                    this.STR += 80;
                    this.DEX += 80;
                    this.INT += 80;
                    this.LUK += 80;
                    this.pad += 15;
                    this.mad += 15;
                    break;
                case 5010825://시그너스의 소울
                    this.STR += 200;
                    this.DEX += 200;
                    this.INT += 200;
                    this.LUK += 200;
                    this.pad += 50;
                    this.mad += 50;
                    break;
                case 5010826://매그너스의 소울
                    this.STR += 300;
                    this.DEX += 300;
                    this.INT += 300;
                    this.LUK += 300;
                    this.pad += 100;
                    this.mad += 100;
                    this.drop += 5;
                    this.meso += 5;
                    this.booster += -1;//소울 공속 예제, 이럼 1단계
                    break;
                case 5010831://스우의 소울
                    this.STR += 500;
                    this.DEX += 500;
                    this.INT += 500;
                    this.LUK += 500;
                    this.pad += 200;
                    this.mad += 200;
                    this.drop += 10;
                    this.meso += 10;
                    this.booster += -2;//소울 공속 예제, 이럼 1단계
                    break;
                case 5010834://검은마법사의 수하 소울
                    this.STR += 1000;
                    this.DEX += 1000;
                    this.INT += 1000;
                    this.LUK += 1000;
                    this.pad += 500;
                    this.mad += 500;
                    this.drop += 30;
                    this.meso += 30;
                    this.booster += -3;//소울 공속 예제, 이럼 2단계
                    break;
                case 5010828://강력한 피넛의 소울
                    this.STR += 400;
                    this.DEX += 400;
                    this.INT += 400;
                    this.LUK += 400;
                    this.pad += 200;
                    this.mad += 200;
                    this.drop += 25;
                    this.meso += 25;
                    this.booster += -1;//소울 공속
                    break;
                case 5010836://위대한 피넛의 소울
                    this.STR += 1000;
                    this.DEX += 1000;
                    this.INT += 1000;
                    this.LUK += 1000;
                    this.pad += 500;
                    this.mad += 500;
                    this.drop += 50;
                    this.meso += 50;
                    this.booster += -3;//소울 공속
                    break;
                case 5010809://강화된 피아누스의 소울
                    this.STR += 120;
                    this.DEX += 120;
                    this.INT += 120;
                    this.LUK += 120;
                    this.pad += 25;
                    this.mad += 25;
                    break;
                case 5010812://기타소울
                    this.STR += 200;
                    this.DEX += 200;
                    this.INT += 200;
                    this.LUK += 200;
                    this.pad += 100;
                    this.mad += 100;
                    break;
                case 5010815://보스소울
                    this.STR += 200;
                    this.DEX += 200;
                    this.INT += 200;
                    this.LUK += 200;
                    this.pad += 100;
                    this.mad += 100;
                    break;
                case 5010804://장비소울
                    this.STR += 200;
                    this.DEX += 200;
                    this.INT += 200;
                    this.LUK += 200;
                    this.pad += 100;
                    this.mad += 100;
                    break;
                case 5010824://전설의 수집소울
                    this.STR += 700;
                    this.DEX += 700;
                    this.INT += 700;
                    this.LUK += 700;
                    this.pad += 300;
                    this.mad += 300;
                    this.drop += 30;
                    this.meso += 30;
                    this.booster += -2;//소울 공속
                    break;
            }
        }

        Map<MapleStat, Integer> lBasicStat = new EnumMap<>(MapleStat.class);
        Map<MapleBuffStat, Integer> lNewPADCTS = new EnumMap<>(MapleBuffStat.class);
        Map<MapleBuffStat, Integer> lNewMADCTS = new EnumMap<>(MapleBuffStat.class);
        Map<MapleBuffStat, Integer> lNewBoosterCTS = new EnumMap<>(MapleBuffStat.class);

        //올스탯 합산 시작
        int[] customizeStat = {STR, DEX, INT, LUK};
        int[] nAllStat = {stat.getStr(), stat.getDex(), stat.getInt(), stat.getLuk()};
        MapleStat[] basicStat = {MapleStat.STR, MapleStat.DEX, MapleStat.INT, MapleStat.LUK};
        for (int i = 0; i < basicStat.length; i++) {
            nAllStat[i] += customizeStat[i];
            lBasicStat.put(basicStat[i], nAllStat[i]);
        }
        //올스탯 합산 종료

        //CTS 합산 시작
        int skillPADID = 0, skillMADID = 0;
        int[] nSecondaryStat = {pad, mad, booster};
        MapleBuffStat[] secondaryStat = {MapleBuffStat.WATK, MapleBuffStat.MATK, MapleBuffStat.BOOSTER};
        for (int i = 0; i < secondaryStat.length; i++) {
            if (user.lCTS.containsKey(secondaryStat[i])) {
                Map<Integer, Integer> mapData = user.lCTS.get(secondaryStat[i]);

                if (secondaryStat[i] != MapleBuffStat.BOOSTER) {
                    long beforeRemainTime = 0;
                    for (Entry<Integer, Integer> entry : mapData.entrySet()) {
                        int key = entry.getKey();
                        Skill skill = SkillFactory.getSkill(key);
                        if (skill != null) {
                            MapleStatEffect effect = skill.getEffect(user.getSkillLevel(key));
                            if (effect != null) {
                                if (effect.isMorph() || effect.isPirateMorph()) {
                                    continue;
                                }

                                long remainTime = System.currentTimeMillis() - (user.getStartTimeFromSkillID(entry.getKey()) + effect.getDuration());
                                if (remainTime > 0) {
                                    if (beforeRemainTime == 0 || (beforeRemainTime > 0 && remainTime <= beforeRemainTime)) {
                                        beforeRemainTime = remainTime;
                                        if (secondaryStat[i] == MapleBuffStat.WATK) {
                                            skillPADID = key;
                                        }
                                        if (secondaryStat[i] == MapleBuffStat.MATK) {
                                            skillMADID = key;
                                        }
                                    }
                                }
                            }
                        } else {
                            MapleStatEffect effect = MapleItemInformationProvider.getInstance().getItemEffect(key);
                            if (effect != null) {
                                if (effect.isMorph() || effect.isPirateMorph()) {
                                    continue;
                                }

                                long remainTime = System.currentTimeMillis() - (user.getStartTimeFromSkillID(entry.getKey()) + effect.getDuration());
                                if (remainTime > 0) {
                                    if (beforeRemainTime == 0 || (beforeRemainTime > 0 && remainTime <= beforeRemainTime)) {
                                        beforeRemainTime = remainTime;
                                        if (secondaryStat[i] == MapleBuffStat.WATK) {
                                            skillPADID = key;
                                        }
                                        if (secondaryStat[i] == MapleBuffStat.MATK) {
                                            skillMADID = key;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                for (Entry<Integer, Integer> entry : mapData.entrySet()) {
                    int key = entry.getKey();
                    int val = entry.getValue();
                    if (secondaryStat[i] == MapleBuffStat.BOOSTER) {
                        //val = -2; //지금 공속 강제로 10단계 해봤는데 개잘됌
                        this.booster += val;
                        nSecondaryStat[i] += val;

//                        //반지 아이템 아닐 때만
//                        if (!GameConstants.boosterBuffItemID.contains(key)) {
//                            skillID = key;
//                        }
                    } else {
                        nSecondaryStat[i] += val;
                    }
                }
            }
            if (secondaryStat[i] == MapleBuffStat.BOOSTER) {
                MapleStatEffect tempEff;
                if ((tempEff = user.getStatForBuff(MapleBuffStat.YELLOW_AURA)) != null) {
                    if (!user.getBuffedValue1(32111005)) {
                        if (user.lCTS.containsKey(secondaryStat[i])) {
                            Map<Integer, Integer> mapData = user.lCTS.get(secondaryStat[i]);
                            if (!mapData.containsKey(tempEff.getSourceId())) {
                                mapData.put(tempEff.getSourceId(), tempEff.getY());
                                user.lCTS.put(secondaryStat[i], mapData);
                                nSecondaryStat[2] += tempEff.getY();
                            }
                        }
                    } else {
                        if (user.lCTS.containsKey(secondaryStat[i])) {
                            Map<Integer, Integer> mapData = user.lCTS.get(secondaryStat[i]);
                            if (mapData.containsKey(tempEff.getSourceId())) {
                                mapData.remove(tempEff.getSourceId(), tempEff.getY());
                                user.lCTS.put(secondaryStat[i], mapData);
                                nSecondaryStat[2] -= tempEff.getY();
                            }
                        }
                    }
                }
                lNewBoosterCTS.put(secondaryStat[i], nSecondaryStat[i]);
            } else if (secondaryStat[i] == MapleBuffStat.WATK) {
                lNewPADCTS.put(secondaryStat[i], nSecondaryStat[i]);
            } else if (secondaryStat[i] == MapleBuffStat.MATK) {
                lNewMADCTS.put(secondaryStat[i], nSecondaryStat[i]);
            }
        }
        //CTS 합산 종료

        if (lNewPADCTS.isEmpty()) {
            if (this.pad > 0) {
                lNewPADCTS.put(MapleBuffStat.WATK, this.pad);
            }
        }
        if (lNewMADCTS.isEmpty()) {
            if (this.mad > 0) {
                lNewMADCTS.put(MapleBuffStat.MATK, this.mad);
            }
        }
        if (lNewBoosterCTS.isEmpty()) {
            if (this.booster < 0) {
                lNewBoosterCTS.put(MapleBuffStat.BOOSTER, this.booster);
            }
        }

        //펫 버프
        for (MaplePet pet : user.getSummonedPets()) {
            if (pet != null) {
                int petSkillID = pet.getBuffSkill();
                if (petSkillID != -1) {
                    Skill petSkill = SkillFactory.getSkill(petSkillID);
                    if (petSkill != null) {
                        int petSkillLV = user.getSkillLevel(petSkillID);
                        if (petSkillLV > 0) {
                            MapleStatEffect tempEff = petSkill.getEffect(petSkillLV);
                            if (tempEff != null) {
                                for (int i = 0; i < secondaryStat.length; i++) {
                                    if (user.lCTS.containsKey(secondaryStat[i])) {
                                        Map<Integer, Integer> mapData = user.lCTS.get(secondaryStat[i]);

                                        if (mapData.containsKey(petSkillID)) {
                                            if (secondaryStat[i] == MapleBuffStat.WATK) {
                                                if (petSkillID != skillPADID) {
                                                    skillPADID = petSkillID;
                                                }
                                            } else if (secondaryStat[i] == MapleBuffStat.MATK) {
                                                if (petSkillID != skillMADID) {
                                                    skillMADID = petSkillID;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        //펫 버프

        user.getClient().sendPacket(MaplePacketCreator.updatePlayerStats(lBasicStat, true, user.getJob()));
        if (lNewPADCTS.size() > 0) {
            user.getClient().sendPacket(TemporaryStatsPacket.giveBuff(skillPADID, 24 * 60 * 60 * 1000, lNewPADCTS, null));
        }
        if (lNewMADCTS.size() > 0) {
            user.getClient().sendPacket(TemporaryStatsPacket.giveBuff(skillMADID, 24 * 60 * 60 * 1000, lNewMADCTS, null));
        }
        if (lNewBoosterCTS.size() > 0) {
            user.getClient().sendPacket(TemporaryStatsPacket.giveBuff(0, 24 * 60 * 60 * 1000, lNewBoosterCTS, null));
        }

        MapleStatEffect tempEff;
        if ((tempEff = user.getStatForBuff(MapleBuffStat.SPEED_INFUSION)) != null) {
            nSecondaryStat[2] += tempEff.getX();
        }

        boolean stanceSkill = false;
        for (int i = 0; i < 6; i++) {
            if (user.getInventory(MapleInventoryType.SETUP).findById(3960096 + i) != null) {
                stanceSkill = true;
                user.changeSkillLevel_Skip(SkillFactory.getSkill(user.getStat().getSkillByJob(1210, user.getJob())), (byte) 1, (byte) 0, true);
            }
        }
        if (!stanceSkill) {
            user.changeSkillLevel_Skip(SkillFactory.getSkill(user.getStat().getSkillByJob(1210, user.getJob())), (byte) -1, (byte) -1, true);
        }

        int[][] dropSymbol = {
            {3960070, 2}, 
            {3960071, 4}, 
            {3960072, 6}, 
            {3960073, 8}, 
            {3960074, 10}, 
            {3960075, 15}, 
            {3960090, 10}, 
            {3960091, 3}, 
            {3960092, 3}, 
            {3960093, 3}, 
            {3960094, 3}, 
            {3960095, 3}, 
        };
        
        int symbolDrop = 0;
        for (int j = 0; j < dropSymbol.length; j++) {
            if (user.getInventory(MapleInventoryType.SETUP).findById(dropSymbol[j][0]) != null) {
                symbolDrop += dropSymbol[j][1];
            }
        }
        
        if (inChat) {
            bossDAMr += (stat.bossdam_r - 100.0);
            ignoreTargetDEF += stat.ignoreTargetDEF;

            String talk = new String();
            talk += String.format("현재 %s님의 추가 효과입니다.\r\n\r\n", user.getName());
            talk += String.format("STR : +%d\r\n", customizeStat[0]);
            talk += String.format("DEX : +%d\r\n", customizeStat[1]);
            talk += String.format("INT : +%d\r\n", customizeStat[2]);
            talk += String.format("LUK : +%d\r\n", customizeStat[3]);
            talk += String.format("공격력 : +%d\r\n", nSecondaryStat[0] + syPAD);
            talk += String.format("마력 : +%d\r\n", nSecondaryStat[1] + syMAD);
            talk += String.format("공격속도 : +%d단계\r\n", -nSecondaryStat[2]);
            talk += String.format("보스 공격시 데미지 : +%d%%\r\n", bossDAMr);
            talk += String.format("방어력 무시 : +%d%%\r\n", ignoreTargetDEF);

            talk += String.format("추가 데미지 : +%d\r\n", user.getRCDamage());

            talk += String.format("메소 획득량 : +%d%%\r\n", user.getSymbolMeso() + this.meso);
            talk += String.format("아이템 획득량 : +%d%%\r\n", symbolDrop + this.drop);

            user.getClient().sendPacket(MaplePacketCreator.serverNotice(7, 9010000, talk));

        }

        if (tCur == 0) {
            stat.recalcLocalStats(user);
        }
    }
}
