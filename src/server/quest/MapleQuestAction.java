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
package server.quest;

import client.*;
import client.inventory.Equip;
import client.inventory.InventoryException;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
import constants.ServerConstants;
import server.ItemInfo;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.Randomizer;
import server.log.LogType;
import server.log.ServerLogger;
import server.maps.MapleMap;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Triple;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapleQuestAction implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    private MapleQuestActionType type;
    private MapleQuest quest;
    private int intStore = 0;
    private String stringStore = null;
    private List<Integer> applicableJobs = new ArrayList<Integer>();
    private List<QuestItem> items = null;
    private List<Triple<Integer, Integer, Integer>> skill = null;
    private List<Pair<Integer, Integer>> state = null;

    /**
     * Creates a new instance of MapleQuestAction
     */
    public MapleQuestAction(MapleQuestActionType type, ResultSet rse, MapleQuest quest, PreparedStatement pss, PreparedStatement psq, PreparedStatement psi) throws SQLException {
        this.type = type;
        this.quest = quest;

        this.intStore = rse.getInt("intStore");
        this.stringStore = rse.getString("stringStore");
        String[] jobs = rse.getString("applicableJobs").split(", ");
        if (jobs.length <= 0 && rse.getString("applicableJobs").length() > 0) {
            applicableJobs.add(Integer.parseInt(rse.getString("applicableJobs")));
        }
        for (String j : jobs) {
            if (j.length() > 0) {
                applicableJobs.add(Integer.parseInt(j));
            }
        }
        ResultSet rs;
        switch (type) {
            case item:
                items = new ArrayList<QuestItem>();
                psi.setInt(1, rse.getInt("uniqueid"));
                rs = psi.executeQuery();
                while (rs.next()) {
                    items.add(new QuestItem(rs.getInt("itemid"), rs.getInt("count"), rs.getInt("period"), rs.getInt("gender"), rs.getInt("job"), rs.getInt("jobEx"), rs.getInt("prop")));
                }
                rs.close();
                break;
            case quest:
                state = new ArrayList<Pair<Integer, Integer>>();
                psq.setInt(1, rse.getInt("uniqueid"));
                rs = psq.executeQuery();
                while (rs.next()) {
                    state.add(new Pair<Integer, Integer>(rs.getInt("quest"), rs.getInt("state")));
                }
                rs.close();
                break;
            case skill:
                skill = new ArrayList<Triple<Integer, Integer, Integer>>();
                pss.setInt(1, rse.getInt("uniqueid"));
                rs = pss.executeQuery();
                while (rs.next()) {
                    skill.add(new Triple<Integer, Integer, Integer>(rs.getInt("skillid"), rs.getInt("skillLevel"), rs.getInt("masterLevel")));
                }
                rs.close();
                break;
        }
    }

    private static boolean canGetItem(QuestItem item, MapleCharacter c) {
        if (item.gender != 2 && item.gender >= 0 && item.gender != c.getGender()) {
            return false;
        }
        if (item.job > 0) {
            final List<Integer> code = getJobBy5ByteEncoding(item.job);
            boolean jobFound = false;
            for (int codec : code) {
                if (codec / 100 == c.getJob() / 100) {
                    jobFound = true;
                    break;
                }
            }
            if (!jobFound && item.jobEx > 0) {
                final List<Integer> codeEx = getJobBySimpleEncoding(item.jobEx);
                for (int codec : codeEx) {
                    if ((codec / 100 % 10) == (c.getJob() / 100 % 10)) {
                        jobFound = true;
                        break;
                    }
                }
            }
            return jobFound;
        } else if (item.jobEx > 0) {
            boolean jobFound = false;
            if (!jobFound && item.jobEx > 0) {
                final List<Integer> codeEx = getJobBySimpleEncoding(item.jobEx);
                for (int codec : codeEx) {
                    if ((codec / 100 % 10) == (c.getJob() / 100 % 10)) {
                        jobFound = true;
                        break;
                    }
                }
            }
            return jobFound;
        }
        return true;
    }

    public final boolean RestoreLostItem(final MapleCharacter c, final int itemid) {
        if (type == MapleQuestActionType.item) {

            for (QuestItem item : items) {
                if (item.itemid == itemid) {
                    if (!c.haveItem(item.itemid, item.count, true, false)) {
                        MapleInventoryManipulator.addById(c.getClient(), item.itemid, (short) item.count, "Obtained from quest (Restored) " + quest.getId() + " on " + FileoutputUtil.CurrentReadable_Date());
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public String getInfoData() {
        if (type == MapleQuestActionType.info) {
            return stringStore;
        }
        return null;
    }

    public String getNpcAct() {
        if (type == MapleQuestActionType.npcAct) {
            return stringStore;
        }
        return null;
    }

    public void runStart(MapleCharacter c, Integer extSelection) {
        MapleQuestStatus status;
        switch (type) {
            case exp:
                status = c.getQuest(quest);
                if (status.getForfeited() > 0) {
                    break;
                }
                c.gainExp(intStore * (c.getStat().questBonus) * 1, true, true, true);
                break;
            case item:
                // first check for randomness in item selection
                Map<Integer, Integer> props = new HashMap<Integer, Integer>();
                for (QuestItem item : items) {
                    if (item.prop > 0 && canGetItem(item, c)) {
                        for (int i = 0; i < item.prop; i++) {
                            props.put(props.size(), item.itemid);
                        }
                    }
                }
                int selection = 0;
                int extNum = 0;
                if (props.size() > 0) {
                    selection = props.get(Randomizer.nextInt(props.size()));
                }
                for (QuestItem item : items) {
                    if (!canGetItem(item, c)) {
                        continue;
                    }
                    final int id = item.itemid;
                    if (item.prop != -2) {
                        if (item.prop == -1) {
                            if (extSelection != null && extSelection != extNum++) {
                                continue;
                            }
                        } else if (id != selection) {
                            continue;
                        }
                    }
                    short count = (short) item.count;
                    if (count <= 0) { // remove items
                        try {
                            if (count == 0) {
                                count = (short) c.getItemQuantity(id, false);
                            }
                            ServerLogger.getInstance().logItem(LogType.Item.Quest, c.getId(), c.getName(), id, count, MapleItemInformationProvider.getInstance().getName(id), 0, "QuestStart - (" + quest.getId() + " / " + quest.getName() + ")");
                            MapleInventoryManipulator.removeById(c.getClient(), GameConstants.getInventoryType(id), id, (count * -1), true, false);
                        } catch (InventoryException ie) {
                            // it's better to catch this here so we'll atleast try to remove the other items
                            System.err.println("[h4x] Completing a quest without meeting the requirements" + ie);
                        }
                        c.getClient().getSession().write(MaplePacketCreator.getShowItemGain(id, count, true));
                    } else { // add items
                        final int period = item.period / 1440; //im guessing.
                        ServerLogger.getInstance().logItem(LogType.Item.Quest, c.getId(), c.getName(), id, count, MapleItemInformationProvider.getInstance().getName(id), 0, "QuestStart - (" + quest.getId() + " / " + quest.getName() + ")");
                        MapleInventoryManipulator.addById(c.getClient(), id, count, "", null, period, "Obtained from quest " + quest.getId() + " on " + FileoutputUtil.CurrentReadable_Date());
                        c.getClient().getSession().write(MaplePacketCreator.getShowItemGain(id, count, true));
                    }
                }
                break;
            case nextQuest:
                status = c.getQuest(quest);
                if (status.getForfeited() > 0) {
                    break;
                }
                c.getClient().getSession().write(MaplePacketCreator.updateQuestFinish(quest.getId(), status.getNpc(), intStore));
                break;
            case money:
                status = c.getQuest(quest);
                if (status.getForfeited() > 0) {
                    break;
                }
                ServerLogger.getInstance().logItem(LogType.Item.Quest, c.getId(), c.getName(), 0, 0, "메소", intStore, "QuestStart - (" + quest.getId() + " / " + quest.getName() + ")");
                c.gainMeso(intStore, true, true);
                break;
            case quest:
                for (Pair<Integer, Integer> q : state) {
                    c.updateQuest(new MapleQuestStatus(MapleQuest.getInstance(q.left), q.right));
                }
                break;
            case skill:
                for (Triple<Integer, Integer, Integer> skills : skill) {
                    final int skillid = skills.left;
                    int skillLevel = skills.mid;
                    int masterLevel = skills.right;
                    final Skill skillObject = SkillFactory.getSkill(skillid);
                    boolean found = false;
                    for (int applicableJob : applicableJobs) {
                        if (c.getJob() == applicableJob) {
                            found = true;
                            break;
                        }
                    }
                    if (skillObject.isBeginnerSkill() || found) {
                        c.changeSkillLevel(skillObject, (byte) Math.max(skillLevel, c.getSkillLevel(skillObject)), (byte) Math.max(masterLevel, c.getMasterLevel(skillObject)));
                    }
                }
                break;
            case pop:
                status = c.getQuest(quest);
                if (status.getForfeited() > 0) {
                    break;
                }
                final int fameGain = intStore;
                c.addFame(fameGain);
                c.updateSingleStat(MapleStat.FAME, c.getFame());
                c.getClient().getSession().write(MaplePacketCreator.getShowFameGain(fameGain));
                break;
            case buffItemID:
                status = c.getQuest(quest);
                /*if (status.getForfeited() > 0) {
                 break;
                 }*/
                //중복 방지 같은데
                final int tobuff = intStore;
                if (tobuff <= 0) {
                    break;
                }
                MapleItemInformationProvider.getInstance().getItemEffect(tobuff).applyTo(c);
                break;
            case infoNumber: {
//		System.out.println("quest : "+intStore+"");
//		MapleQuest.getInstance(intStore).forceComplete(c, 0);
                break;
            }
            case sp: {
                status = c.getQuest(quest);
                if (status.getForfeited() > 0) {
                    break;
                }
                final int sp_val = intStore;
                if (applicableJobs.size() > 0) {
                    int finalJob = 0;
                    for (int job_val : applicableJobs) {
                        if (c.getJob() >= job_val && job_val > finalJob) {
                            finalJob = job_val;
                        }
                    }
                    if (finalJob == 0) {
                        c.gainSP(sp_val);
                    } else {
                        c.gainSP(sp_val, GameConstants.getSkillBook(finalJob));
                    }
                } else {
                    c.gainSP(sp_val);
                }
                break;
            }
            default:
                break;
        }
    }

    public boolean checkEnd(MapleCharacter c, Integer extSelection) {
        switch (type) {
            case item: {
                // first check for randomness in item selection
                final Map<Integer, Integer> props = new HashMap<Integer, Integer>();

                for (QuestItem item : items) {
                    if (item.prop > 0 && canGetItem(item, c)) {
                        for (int i = 0; i < item.prop; i++) {
                            props.put(props.size(), item.itemid);
                        }
                    }
                }
                int selection = 0;
                int extNum = 0;
                if (props.size() > 0) {
                    selection = props.get(Randomizer.nextInt(props.size()));
                }
                byte eq = 0, use = 0, setup = 0, etc = 0, cash = 0;

                for (QuestItem item : items) {
                    if (!canGetItem(item, c)) {
                        continue;
                    }
                    final int id = item.itemid;
                    if (item.prop != -2) {
                        if (item.prop == -1) {
                            if (extSelection != null && extSelection != extNum++) {
                                continue;
                            }
                        } else if (id != selection) {
                            continue;
                        }
                    }
                    final short count = (short) item.count;
                    if (count < 0) { // remove items
                        if (!c.haveItem(id, count, false, true)) {
                            if (!c.haveItem(id, count, true, true)) {
                                c.dropMessage(1, "아이템이 부족합니다.");
                            } else {
                                c.dropMessage(1, ItemInfo.getName(id) + "을(를) 장착중이어서 퀘스트를 진행할 수 없습니다.");
                            }
                            return false;
                        }
                    } else { // add items
                        if (MapleItemInformationProvider.getInstance().isPickupRestricted(id) && c.haveItem(id, 1, true, false)) {
                            c.dropMessage(1, "이 아이템은 이미 보유하고 있습니다.");
                            return false;
                        }
                        switch (GameConstants.getInventoryType(id)) {
                            case EQUIP:
                                eq++;
                                break;
                            case USE:
                                use++;
                                break;
                            case SETUP:
                                setup++;
                                break;
                            case ETC:
                                etc++;
                                break;
                            case CASH:
                                cash++;
                                break;
                        }
                    }
                }
                if (c.getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() < eq) {
                    c.dropMessage(1, "장비 인벤토리 공간이 부족합니다.");
                    return false;
                } else if (c.getInventory(MapleInventoryType.USE).getNumFreeSlot() < use) {
                    c.dropMessage(1, "소비 인벤토리 공간이 부족합니다.");
                    return false;
                } else if (c.getInventory(MapleInventoryType.SETUP).getNumFreeSlot() < setup) {
                    c.dropMessage(1, "설치 인벤토리 공간이 부족합니다.");
                    return false;
                } else if (c.getInventory(MapleInventoryType.ETC).getNumFreeSlot() < etc) {
                    c.dropMessage(1, "기타 인벤토리 공간이 부족합니다.");
                    return false;
                } else if (c.getInventory(MapleInventoryType.CASH).getNumFreeSlot() < cash) {
                    c.dropMessage(1, "캐시 인벤토리 공간이 부족합니다.");
                    return false;
                }
                return true;
            }
            case money: {
                final int meso = intStore;
                if (meso < 0 && c.getMeso() < Math.abs(meso)) { //remove meso
                    c.dropMessage(1, "메소가 부족합니다.");
                    return false;
                } else if (c.getMeso() + meso < 0) { // Giving, overflow
                    c.dropMessage(1, "최대 메소 소지량 2147483647 을 초과하였습니다.");
                    return false;
                }
                return true;
            }
        }
        return true;
    }

    public void runEnd(MapleCharacter c, Integer extSelection) {
        switch (type) {
            case exp: {
                if (c.getLevel() < 11) {
                    c.gainExp(intStore * (c.getStat().questBonus) * ServerConstants.Default_QuestRate, true, true, true);
                    break;
                } else {
                    c.gainExp(intStore * (c.getStat().questBonus) * ServerConstants.QuestRate, true, true, true);
                    if (c.getLevel() > 50) {
                    c.gainItem(2430181, (short) 1, false, -1, "퀘스트 완료"); 
                    c.dropMessage(5, "[보상] 퀘스트를 완료해 수상한 미라클 큐브 교환권을 지급 받았습니다.");   
                    }
                    break;
                }
            }
            case item: {
                // first check for randomness in item selection
                Map<Integer, Integer> props = new HashMap<Integer, Integer>();
                for (QuestItem item : items) {
                    if (item.prop > 0 && canGetItem(item, c)) {
                        for (int i = 0; i < item.prop; i++) {
                            props.put(props.size(), item.itemid);
                        }
                    }
                }
                int selection = 0;
                int extNum = 0;
                if (props.size() > 0) {
                    selection = props.get(Randomizer.nextInt(props.size()));
                }
                for (QuestItem item : items) {
                    if (!canGetItem(item, c)) {
                        continue;
                    }
                    final int id = item.itemid;
                    if (item.prop != -2) {
                        if (item.prop == -1) {
                            if (extSelection != null && extSelection != extNum++) {
                                continue;
                            }
                        } else if (id != selection) {
                            continue;
                        }
                    }
                    final short count = (short) item.count;
                    if (count <= 0) { // remove items
                        short realcount = count;
                        if (count == 0) {
                            realcount = (short) -c.getItemQuantity(id, false);
                        }
                        MapleInventoryManipulator.removeById(c.getClient(), GameConstants.getInventoryType(id), id, (realcount * -1), true, false);
                        ServerLogger.getInstance().logItem(LogType.Item.Quest, c.getId(), c.getName(), id, realcount, MapleItemInformationProvider.getInstance().getName(id), 0, "QuestComplete - (" + quest.getId() + " / " + quest.getName() + ")");
                        c.getClient().getSession().write(MaplePacketCreator.getShowItemGain(id, realcount, true));
                    } else { // add items
                        final int period = item.period / 1440; //im guessing.
                        final String name = MapleItemInformationProvider.getInstance().getName(id);
                        if (id / 10000 == 114 && name != null && name.length() > 0) { //medal
                            final String msg = "<" + name + "> 칭호를 얻었습니다.";
                            c.dropMessage(-1, msg);
                        }
                        ServerLogger.getInstance().logItem(LogType.Item.Quest, c.getId(), c.getName(), id, count, MapleItemInformationProvider.getInstance().getName(id), 0, "QuestComplete - (" + quest.getId() + " / " + quest.getName() + ")");
                        if (GameConstants.getInventoryType(item.itemid) == MapleInventoryType.EQUIP && item.period > 0) {
                            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                            final Item item2 = ii.getEquipById(item.itemid);
                            item2.setExpiration(System.currentTimeMillis() + (item.period * 60 * 60 * 10));
                            //MapleInventoryManipulator.addById(c.getClient(), id, count, "", null, period + " on " + FileoutputUtil.CurrentReadable_Date());
                            MapleInventoryManipulator.addbyItem(c.getClient(), item2);
                            c.getClient().getSession().write(MaplePacketCreator.getShowItemGain(id, count, true));
                        } else {
                            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                            if (id == 1082254 || id == 1032075 || id == 1003139 || id == 1112683) {//머쉬룸 장갑 페페킹 장갑, 자유로운 영혼의 피어싱,시간여행자의로렐,에피네아의 반지
                                MapleInventoryManipulator.addByIdPotential(c.getClient(), id, count, "", null, period + " on " + FileoutputUtil.CurrentReadable_Date(), true);
                                c.getClient().getSession().write(MaplePacketCreator.getShowItemGain(id, count, true));
                            } else {
                                MapleInventoryManipulator.addById(c.getClient(), id, count, "", null, period + " on " + FileoutputUtil.CurrentReadable_Date());
                                c.getClient().getSession().write(MaplePacketCreator.getShowItemGain(id, count, true));
                            }
                        }
                    }
                }
                break;
            }
            case nextQuest: {
                c.getClient().getSession().write(MaplePacketCreator.updateQuestFinish(quest.getId(), c.getQuest(quest).getNpc(), intStore));
                break;
            }
            case money: {
                ServerLogger.getInstance().logItem(LogType.Item.Quest, c.getId(), c.getName(), 0, 0, "메소", intStore, "QuestComplete - (" + quest.getId() + " / " + quest.getName() + ")");
                c.gainMeso(intStore, true, true);
                break;
            }
            case quest: {
                for (Pair<Integer, Integer> q : state) {
                    c.updateQuest(new MapleQuestStatus(MapleQuest.getInstance(q.left), q.right));
                }
                break;
            }
            case skill:
                for (Triple<Integer, Integer, Integer> skills : skill) {
                    final int skillid = skills.left;
                    int skillLevel = skills.mid;
                    int masterLevel = skills.right;
                    final Skill skillObject = SkillFactory.getSkill(skillid);
                    boolean found = false;
                    for (int applicableJob : applicableJobs) {
                        if (c.getJob() == applicableJob) {
                            found = true;
                            break;
                        }
                    }
                    if (skillObject.isBeginnerSkill() || found) {
                        c.changeSkillLevel(skillObject, (byte) Math.max(skillLevel, c.getSkillLevel(skillObject)), (byte) Math.max(masterLevel, c.getMasterLevel(skillObject)));
                    }
                }
                break;
            case pop: {
                final int fameGain = intStore;
                c.addFame(fameGain);
                c.updateSingleStat(MapleStat.FAME, c.getFame());
                c.getClient().getSession().write(MaplePacketCreator.getShowFameGain(fameGain));
                break;
            }
            case buffItemID: {
                final int tobuff = intStore;
                if (tobuff <= 0) {
                    break;
                }
                if (tobuff == 2022109) {
                    //ninespirit - hardcode
                    MapleMap map = c.getClient().getChannelServer().getMapFactory().getMap(240000000);
                    MapleMap map2 = c.getClient().getChannelServer().getMapFactory().getMap(240040611);
                    map.broadcastMessage(MaplePacketCreator.serverNotice(5, "나인스피릿 아기용의 힘찬 울음소리를 듣자 신비로운 힘이 솟아오른다."));
                    map2.broadcastMessage(MaplePacketCreator.serverNotice(5, "나인스피릿 아기용의 힘찬 울음소리를 듣자 신비로운 힘이 솟아오른다."));
                    for (MapleCharacter chr : map.getCharacters()) {
                        MapleItemInformationProvider.getInstance().getItemEffect(tobuff).applyTo(chr);
                    }
                    for (MapleCharacter chr : map2.getCharacters()) {
                        MapleItemInformationProvider.getInstance().getItemEffect(tobuff).applyTo(chr);
                    }
                } else {
                    MapleItemInformationProvider.getInstance().getItemEffect(tobuff).applyTo(c);
                }
                break;
            }
            case infoNumber: {
//		System.out.println("quest : "+intStore+"");
//		MapleQuest.getInstance(intStore).forceComplete(c, 0);
                break;
            }
            case sp: {
                final int sp_val = intStore;
                if (applicableJobs.size() > 0) {
                    int finalJob = 0;
                    for (int job_val : applicableJobs) {
                        if (c.getJob() >= job_val && job_val > finalJob) {
                            finalJob = job_val;
                        }
                    }
                    if (finalJob == 0) {
                        c.gainSP(sp_val);
                    } else {
                        c.gainSP(sp_val, GameConstants.getSkillBook(finalJob));
                    }
                } else {
                    c.gainSP(sp_val);
                }
                break;
            }
            default:
                break;
        }
    }

    private static List<Integer> getJobBy5ByteEncoding(int encoded) {
        List<Integer> ret = new ArrayList<Integer>();
        if ((encoded & 0x1) != 0) {
            ret.add(0);
        }
        if ((encoded & 0x2) != 0) {
            ret.add(100);
        }
        if ((encoded & 0x4) != 0) {
            ret.add(200);
        }
        if ((encoded & 0x8) != 0) {
            ret.add(300);
        }
        if ((encoded & 0x10) != 0) {
            ret.add(400);
        }
        if ((encoded & 0x20) != 0) {
            ret.add(500);
        }
        if ((encoded & 0x400) != 0) {
            ret.add(1000);
        }
        if ((encoded & 0x800) != 0) {
            ret.add(1100);
        }
        if ((encoded & 0x1000) != 0) {
            ret.add(1200);
        }
        if ((encoded & 0x2000) != 0) {
            ret.add(1300);
        }
        if ((encoded & 0x4000) != 0) {
            ret.add(1400);
        }
        if ((encoded & 0x8000) != 0) {
            ret.add(1500);
        }
        if ((encoded & 0x20000) != 0) {
            ret.add(2001); //im not sure of this one
            ret.add(2200);
        }
        if ((encoded & 0x100000) != 0) {
            ret.add(2000);
            ret.add(2001); //?
        }
        if ((encoded & 0x200000) != 0) {
            ret.add(2100);
        }
        if ((encoded & 0x400000) != 0) {
            ret.add(2001); //?
            ret.add(2200);
        }

        if ((encoded & 0x40000000) != 0) { //i haven't seen any higher than this o.o
            ret.add(3000);
            ret.add(3200);
            ret.add(3300);
            ret.add(3500);
        }
        return ret;
    }

    private static List<Integer> getJobBySimpleEncoding(int encoded) {
        List<Integer> ret = new ArrayList<Integer>();
        if ((encoded & 0x1) != 0) {
            ret.add(200);
        }
        if ((encoded & 0x2) != 0) {
            ret.add(300);
        }
        if ((encoded & 0x4) != 0) {
            ret.add(400);
        }
        if ((encoded & 0x8) != 0) {
            ret.add(500);
        }
        return ret;
    }

    public MapleQuestActionType getType() {
        return type;
    }

    @Override
    public String toString() {
        return type.toString();
    }

    public List<Triple<Integer, Integer, Integer>> getSkills() {
        return skill;
    }

    public List<QuestItem> getItems() {
        return items;
    }

    public static class QuestItem {

        public int itemid, count, period, gender, job, jobEx, prop;

        public QuestItem(int itemid, int count, int period, int gender, int job, int jobEx, int prop) {
            this.itemid = itemid;
            this.count = count;
            this.period = period;
            this.gender = gender;
            this.job = job;
            this.jobEx = jobEx;
            this.prop = prop;
        }
    }
}
