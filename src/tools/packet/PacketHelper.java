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
package tools.packet;

import client.MapleBuffStat;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import client.inventory.Equip;
import client.Skill;
import constants.GameConstants;
import client.inventory.MapleRing;
import client.inventory.MaplePet;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleCoolDownValueHolder;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.MapleQuestStatus;
import client.MapleTrait.MapleTraitType;
import client.inventory.Item;
import client.SkillEntry;
import client.inventory.ItemFlag;
import handling.Buffstat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.SimpleTimeZone;
import server.MapleItemInformationProvider;
import server.MapleShop;
import server.MapleShopItem;
import tools.Pair;
import server.movement.LifeMovementFragment;
import server.quest.MapleQuest;
import server.shops.AbstractPlayerStore;
import server.shops.IMaplePlayerShop;
import tools.BitTools;
import tools.StringUtil;
import tools.Triple;
import tools.data.MaplePacketLittleEndianWriter;
import util.FileTime;

public class PacketHelper {

    public final static long FT_UT_OFFSET = 116445060000000000L; // KST
    public final static long MAX_TIME = 150842304000000000L; //00 80 05 BB 46 E6 17 02
    public final static long ZERO_TIME = 94354848000000000L; //00 40 E0 FD 3B 37 4F 01
    public final static long PERMANENT = 150841440000000000L; // 00 C0 9B 90 7D E5 17 02

    public static final long getKoreanTimestamp(final long realTimestamp) {
        return getTime(realTimestamp);
    }

    public static final long getTime(long realTimestamp) {
        if (realTimestamp == -1) {
            return MAX_TIME;
        } else if (realTimestamp == -2) {
            return ZERO_TIME;
        } else if (realTimestamp == -3) {
            return PERMANENT;
        }
        return ((realTimestamp * 10000) + FT_UT_OFFSET);
    }

    public static long getFileTimestamp(long timeStampinMillis, boolean roundToMinutes) {
        if (SimpleTimeZone.getDefault().inDaylightTime(new Date())) {
            timeStampinMillis -= 3600000L;
        }
        long time;
        if (roundToMinutes) {
            time = (timeStampinMillis / 1000 / 60) * 600000000;
        } else {
            time = timeStampinMillis * 10000;
        }
        return time + FT_UT_OFFSET;
    }

    public static void addQuestInfo(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        final List<MapleQuestStatus> started = chr.getStartedQuests();
        mplew.writeShort(started.size());
        for (final MapleQuestStatus q : started) {
            mplew.writeShort(q.getQuest().getId());
            if (q.hasMobKills()) {
                final StringBuilder sb = new StringBuilder();
                for (final int kills : q.getMobKills().values()) {
                    sb.append(StringUtil.getLeftPaddedStr(String.valueOf(kills), '0', 3));
                }
                mplew.writeMapleAsciiString(sb.toString());
            } else {
                mplew.writeMapleAsciiString(q.getCustomData() == null ? "" : q.getCustomData());
            }
        }
        final List<MapleQuestStatus> completed = chr.getCompletedQuests();
        mplew.writeShort(completed.size());
        for (final MapleQuestStatus q : completed) {
            mplew.writeShort(q.getQuest().getId());
            mplew.writeLong(getTime(q.getCompletionTime()));
        }
    }

    public static final void addSkillInfo(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {//
        final Map<Skill, SkillEntry> skills = chr.getSkills();
        mplew.writeShort(skills.size());
        for (final Entry<Skill, SkillEntry> skill : skills.entrySet()) {
            mplew.writeInt(skill.getKey().getId());
            mplew.writeInt(skill.getValue().skillevel);
            addExpirationTime(mplew, skill.getValue().expiration);
            if (skill.getKey().isFourthJob()) {
                mplew.writeInt(skill.getValue().masterlevel);
            }
        }
    }

    public static final void addCoolDownInfo(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        final List<MapleCoolDownValueHolder> cd = chr.getCooldowns();
        mplew.writeShort(cd.size());
        for (final MapleCoolDownValueHolder cooling : cd) {
            mplew.writeInt(cooling.skillId);
            mplew.writeShort((int) (cooling.length + cooling.startTime - System.currentTimeMillis()) / 1000);
        }
    }

    public static final void addRocksInfo(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        final int[] map = chr.getRegRocks();
        for (int i = 0; i < 5; i++) { // VIP teleport map
            mplew.writeInt(map[i]);
        }
        final int[] mapz = chr.getRocks();
        for (int i = 0; i < 10; i++) { // VIP teleport map
            mplew.writeInt(mapz[i]);
        }
        /* final int[] maps = chr.getHyperRocks();
         for (int i = 0; i < 13; i++) { // VIP teleport map
         mplew.writeInt(maps[i]);
         } */
    }

    public static final void addRingInfo(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        mplew.writeShort(0);
        //01 00 = size
        //01 00 00 00 = gametype?
        //03 00 00 00 = win
        //00 00 00 00 = tie/loss
        //01 00 00 00 = tie/loss
        //16 08 00 00 = points
        Triple<List<MapleRing>, List<MapleRing>, List<MapleRing>> aRing = chr.getRings(true);
        List<MapleRing> cRing = aRing.getLeft();
        mplew.writeShort(cRing.size());
        for (MapleRing ring : cRing) {
            mplew.writeInt(ring.getPartnerChrId());
            mplew.writeAsciiString(ring.getPartnerName(), 13);
            mplew.writeLong(ring.getRingId());
            mplew.writeLong(ring.getPartnerRingId());
        }
        List<MapleRing> fRing = aRing.getMid();
        mplew.writeShort(fRing.size());
        for (MapleRing ring : fRing) {
            mplew.writeInt(ring.getPartnerChrId());
            mplew.writeAsciiString(ring.getPartnerName(), 13);
            mplew.writeLong(ring.getRingId());
            mplew.writeLong(ring.getPartnerRingId());
            mplew.writeInt(ring.getItemId());
        }
        List<MapleRing> mRing = aRing.getRight();
        mplew.writeShort(mRing.size());
        int marriageId = 30000;
        for (MapleRing ring : mRing) {
            mplew.writeInt(marriageId);
            mplew.writeInt(chr.getId());
            mplew.writeInt(ring.getPartnerChrId());
            mplew.writeShort(3); //1 = engaged 3 = married
            mplew.writeInt(ring.getItemId());
            mplew.writeInt(ring.getItemId());
            mplew.writeAsciiString(chr.getName(), 13);
            mplew.writeAsciiString(ring.getPartnerName(), 13);
        }
    }

    public static void addInventoryInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.writeInt(chr.getMeso()); // mesos
        mplew.write(chr.getInventory(MapleInventoryType.EQUIP).getSlotLimit()); // equip slots
        mplew.write(chr.getInventory(MapleInventoryType.USE).getSlotLimit()); // use slots
        mplew.write(chr.getInventory(MapleInventoryType.SETUP).getSlotLimit()); // set-up slots
        mplew.write(chr.getInventory(MapleInventoryType.ETC).getSlotLimit()); // etc slots
        mplew.write(chr.getInventory(MapleInventoryType.CASH).getSlotLimit()); // cash slots

        if (FileTime.compareFileTime(chr.getEquipExtExpire(), FileTime.systemTimeToFileTime()) >= 0) {
            mplew.writeLong(chr.getEquipExtExpire().getFileTime());
        } else {
            mplew.writeLong(FileTime.START.getFileTime());
        }
//        final MapleQuestStatus stat = chr.getQuestNoAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
//        if (stat != null && stat.getCustomData() != null && Long.parseLong(stat.getCustomData()) > System.currentTimeMillis()) {
//            mplew.writeLong(getTime(Long.parseLong(stat.getCustomData())));
//        } else {
//            mplew.writeLong(getTime(-2));
//        }
        MapleInventory iv = chr.getInventory(MapleInventoryType.EQUIPPED);
        List<Item> equipped = iv.newList();
        Collections.sort(equipped);
        for (Item item : equipped) {
            if (item.getPosition() < 0 && item.getPosition() > -100) {
                addItemInfo(mplew, item, false, false, false, false, chr);
            }
        }
        mplew.writeShort(0); // start of equipped nx
        for (Item item : equipped) {
            if (item.getPosition() <= -100 && item.getPosition() > -1000) {
                addItemInfo(mplew, item, false, false, false, false, chr);
            }
        }

        mplew.writeShort(0); // start of equip inventory
        iv = chr.getInventory(MapleInventoryType.EQUIP);
        for (Item item : iv.list()) {
            addItemInfo(mplew, item, false, false, false, false, chr);
        }
        mplew.writeShort(0); //start of evan equips

        for (Item item : equipped) {
            if (item.getPosition() <= -1000 && item.getPosition() > -1100) {
                addItemInfo(mplew, item, false, false, false, false, chr);
            }
        }
        mplew.writeShort(0); //start of mechanic equips, ty KDMS
        for (Item item : equipped) {
            if (item.getPosition() <= -1100 && item.getPosition() > -1200) {
                addItemInfo(mplew, item, false, false, false, false, chr);
            }
        }
        mplew.writeShort(0); // start of use inventory
        iv = chr.getInventory(MapleInventoryType.USE);
        for (Item item : iv.list()) {
            addItemInfo(mplew, item, false, false, false, false, chr);
        }
        mplew.write(0); // start of set-up inventory
        iv = chr.getInventory(MapleInventoryType.SETUP);
        for (Item item : iv.list()) {
            addItemInfo(mplew, item, false, false, false, false, chr);
        }
        mplew.write(0); // start of etc inventory
        iv = chr.getInventory(MapleInventoryType.ETC);
        for (Item item : iv.list()) {
            if (item.getPosition() < 100) {
                addItemInfo(mplew, item, false, false, false, false, chr);
            }
        }
        mplew.write(0); // start of cash inventory
        iv = chr.getInventory(MapleInventoryType.CASH);
        for (Item item : iv.list()) {
            addItemInfo(mplew, item, false, false, false, false, chr);
        }
        mplew.write(0);

        /*for (int i = 0; i < chr.getExtendedSlots().size(); i++) {
         mplew.writeInt(i);
         mplew.writeInt(chr.getExtendedSlot(i));
         for (Item item : chr.getInventory(MapleInventoryType.ETC).list()) {
         if (item.getPosition() > (i * 100 + 100) && item.getPosition() < (i * 100 + 200)) {
         addItemInfo(mplew, item, false, false, false, true, chr);
         }
         }
         mplew.writeInt(-1);
         }*/
        // mplew.writeInt(-1);
    }

    public static final void addCharStats(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        mplew.writeInt(chr.getId()); // character id
        mplew.writeAsciiString(chr.getName(), 13);
        mplew.write(chr.getGender()); // gender (0 = male, 1 = female)
        mplew.write(chr.getSkinColor()); // skin color
        mplew.writeInt(chr.getFace()); // face
        mplew.writeInt(chr.getHair()); // hair
        mplew.writeShort(chr.getLevel()); //레벨 제한 패치
        mplew.writeShort(chr.getJob()); // job
        chr.getStat().connectData(mplew);
        mplew.writeShort(chr.getRemainingAp()); // remaining ap
        if (GameConstants.isEvan(chr.getJob()) || GameConstants.isResist(chr.getJob()) || GameConstants.isMercedes(chr.getJob())) {
            final int size = chr.getRemainingSpSize();
            mplew.write(size);
            for (int i = 0; i < chr.getRemainingSps().length; i++) {
                if (chr.getRemainingSp(i) > 0) {
                    mplew.write(i + 1);
                    mplew.write(chr.getRemainingSp(i));
                }
            }
        } else {
            mplew.writeShort(chr.getRemainingSp()); // remaining sp
        }
        mplew.writeInt(chr.getExp()); // exp
        mplew.writeShort(chr.getFame()); // fame
        mplew.writeInt(chr.getMapId()); // current map id
        mplew.write(chr.getSpawnpoint()); // spawnpoint
        mplew.writeShort(chr.getSubcategory()); //1 here = db
    }

    public static final void addCharLook(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr, final boolean mega) {
        mplew.write(chr.getGender());
        mplew.write(chr.getSkinColor());
        mplew.writeInt(chr.getFace());
        //mplew.writeInt(chr.getJob());
        mplew.write(mega ? 0 : 1);
        mplew.writeInt(chr.getHair());

        final Map<Byte, Integer> myEquip = new LinkedHashMap<Byte, Integer>();
        final Map<Byte, Integer> maskedEquip = new LinkedHashMap<Byte, Integer>();
        MapleInventory equip = chr.getInventory(MapleInventoryType.EQUIPPED);

        for (final Item item : equip.newList()) {
            if (item.getPosition() < -127) { //not visible
                continue;
            }
            byte pos = (byte) (item.getPosition() * -1);

            if (pos < 100 && myEquip.get(pos) == null) {
                myEquip.put(pos, item.getItemId());
            } else if (pos > 100 && pos != 111) {
                pos = (byte) (pos - 100);
                if (myEquip.get(pos) != null) {
                    maskedEquip.put(pos, myEquip.get(pos));
                }
                myEquip.put(pos, item.getItemId());
            } else if (myEquip.get(pos) != null) {
                maskedEquip.put(pos, item.getItemId());
            }
        }
        for (final Entry<Byte, Integer> entry : myEquip.entrySet()) {
            mplew.write(entry.getKey());
            mplew.writeInt(entry.getValue());
        }
        mplew.write(0xFF); // end of visible itens
        // masked itens
        for (final Entry<Byte, Integer> entry : maskedEquip.entrySet()) {
            mplew.write(entry.getKey());
            mplew.writeInt(entry.getValue());
        }
        mplew.write(0xFF); // ending markers

        final Item cWeapon = equip.getItem((byte) -111);
        mplew.writeInt(cWeapon != null ? cWeapon.getItemId() : 0);
        mplew.writeInt(0);
        mplew.writeLong(0);
    }

    public static final void addExpirationTime(final MaplePacketLittleEndianWriter mplew, final long time) {
        mplew.writeLong(getTime(time));
    }

    public static final void addItemInfo(final MaplePacketLittleEndianWriter mplew, final Item item, final boolean zeroPosition, final boolean leaveOut) {
        addItemInfo(mplew, item, zeroPosition, leaveOut, false, false, null);
    }

    public static final void addItemInfo(final MaplePacketLittleEndianWriter mplew, final Item item, final boolean zeroPosition, final boolean leaveOut, final boolean trade) {
        addItemInfo(mplew, item, zeroPosition, leaveOut, trade, false, null);
    }

    public static final void addItemInfo(final MaplePacketLittleEndianWriter mplew, final Item item, final boolean zeroPosition, final boolean leaveOut, final boolean trade, final boolean bagSlot, final MapleCharacter chr) {
        short pos = item.getPosition();
        if (zeroPosition) {
            if (!leaveOut) {
                mplew.write(0);
            }
        } else {
            if (pos <= -1) {
                pos *= -1;
                if (pos > 100 && pos < 1000) {
                    pos -= 100;
                }
            }
            if (bagSlot) {
                mplew.writeInt((pos % 100) - 1);
            } else if (!trade && item.getType() == 1) {
                mplew.writeShort(pos);
            } else {
                mplew.write(pos);
            }
        }
        mplew.write(item.getPet() != null ? 3 : item.getType());
        mplew.writeInt(item.getItemId());
        boolean hasUniqueId = item.getUniqueId() > 0 && !GameConstants.isMarriageRing(item.getItemId()) && item.getItemId() / 10000 != 166;
        //marriage rings arent cash items so dont have uniqueids, but we assign them anyway for the sake of rings
        mplew.write(hasUniqueId ? 1 : 0);
        if (hasUniqueId) {
            mplew.writeLong(item.getUniqueId());
        }
        int str = 0, dex = 0, in = 0, luk = 0, watk = 0, matk = 0;
        if (item.getGiftFrom() != null) {
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            final int[] rebirth = new int[4];
            int reqLevel = ii.getReqLevel(item.getItemId());
            String fire = String.valueOf(item.getGiftFrom());
            if (fire.length() == 12) {
                rebirth[0] = Integer.parseInt(fire.substring(0, 3));
                rebirth[1] = Integer.parseInt(fire.substring(3, 6));
                rebirth[2] = Integer.parseInt(fire.substring(6, 9));
                rebirth[3] = Integer.parseInt(fire.substring(9));
            } else if (fire.length() == 11) {
                rebirth[0] = Integer.parseInt(fire.substring(0, 2));
                rebirth[1] = Integer.parseInt(fire.substring(2, 5));
                rebirth[2] = Integer.parseInt(fire.substring(5, 8));
                rebirth[3] = Integer.parseInt(fire.substring(8));
            } else if (fire.length() == 10) {
                rebirth[0] = Integer.parseInt(fire.substring(0, 1));
                rebirth[1] = Integer.parseInt(fire.substring(1, 4));
                rebirth[2] = Integer.parseInt(fire.substring(4, 7));
                rebirth[3] = Integer.parseInt(fire.substring(7));
            }
            if (fire != null && fire.length() >= 10) {
                for (int i = 0; i < 4; ++i) {
                    int randomOption = rebirth[i] / 10;
                    int randomValue = rebirth[i] - rebirth[i] / 10 * 10;
                    switch (randomOption) {
                        case 0: {
                            str += (short) ((reqLevel / 80 + 1) * randomValue);
                            break;
                        }
                        case 1: {
                            dex += (short) ((reqLevel / 80 + 1) * randomValue);
                            break;
                        }
                        case 2: {
                            in += ((short) ((reqLevel / 80 + 1) * randomValue));
                            break;
                        }
                        case 3: {
                            luk += ((short) ((reqLevel / 80 + 1) * randomValue));
                            break;
                        }
                        case 4: {
                            watk += ((short) ((reqLevel / 80 + 1) * randomValue));
                            break;
                        }
                        case 5: {
                            matk += ((short) ((reqLevel / 80 + 1) * randomValue));
                            break;
                        }
                        case 6: {
                            str += ((short) ((reqLevel / 80 + 1) * randomValue));
                            watk += ((short) ((reqLevel / 80 + 1) * randomValue));
                            break;
                        }
                        case 7: {
                            dex += ((short) ((reqLevel / 80 + 1) * randomValue));
                            watk += ((short) ((reqLevel / 80 + 1) * randomValue));
                            break;
                        }
                        case 8: {
                            in += ((short) ((reqLevel / 80 + 1) * randomValue));
                            matk += ((short) ((reqLevel / 80 + 1) * randomValue));
                            break;
                        }
                        case 9: {
                            luk += ((short) ((reqLevel / 80 + 1) * randomValue));
                            watk += ((short) ((reqLevel / 80 + 1) * randomValue));
                            break;
                        }
                        case 10: {
                            in += ((short) ((reqLevel / 80 + 1) * randomValue));
                            matk += ((short) ((reqLevel / 80 + 1) * randomValue));
                            break;
                        }
                        case 11: { //좀 부족하다 싶을 때 11번 사용 인,마 따로 또는 둘다
                            //이건 마력이 좀 덜 뜬다 싶으면 밑에 주석 푸시면 되용 넵
//                            in += ((short) ((reqLevel / 80 + 1) * randomValue));
//                            matk += ((short) ((reqLevel / 80 + 1) * randomValue));
                            break;
                        }
                    }
                }
            }
        }
        if (item.getPet() != null) { // Pet
            addPetItemInfo(mplew, item, item.getPet(), true);
        } else {
            addExpirationTime(mplew, item.getExpiration());
            //mplew.writeInt(chr == null ? -1 : chr.getExtendedSlots().indexOf(item.getItemId()));
            if (item.getType() == 1) {
                final Equip equip = (Equip) item;
                mplew.write(equip.getUpgradeSlots());
                mplew.write(equip.getLevel());
                mplew.writeShort(equip.getStr());
                mplew.writeShort(equip.getDex());
                mplew.writeShort(equip.getInt());
                mplew.writeShort(equip.getLuk());
//                mplew.writeShort(equip.getStr() + str);
//                mplew.writeShort(equip.getDex() + dex);
//                mplew.writeShort(equip.getInt() + in);
//                mplew.writeShort(equip.getLuk() + luk);
                mplew.writeShort(equip.getHp());
                mplew.writeShort(equip.getMp());
                mplew.writeShort(equip.getWatk());
                mplew.writeShort(equip.getMatk());
//                mplew.writeShort(equip.getWatk() + watk);
//                mplew.writeShort(equip.getMatk() + matk);
                mplew.writeShort(equip.getWdef());
                mplew.writeShort(equip.getMdef());
                mplew.writeShort(equip.getAcc());
                mplew.writeShort(equip.getAvoid());
                mplew.writeShort(equip.getHands());
                mplew.writeShort(equip.getSpeed());
                mplew.writeShort(equip.getJump());
                mplew.writeMapleAsciiString(equip.getOwner());
                short flag = equip.getFlag();
                if (Math.floor(item.getItemId() / 10000) == 107) { //신발에 미끄럼 방지 상시 적용
                    if ((flag & ItemFlag.SPIKES.getValue()) == 0) {
                        flag |= ItemFlag.SPIKES.getValue();
                    }
                }
                mplew.writeShort(flag);
                mplew.write(equip.getIncSkill() > 0 ? 1 : 0);
                mplew.write(Math.max(equip.getBaseLevel(), equip.getEquipLevel())); // Item level
                mplew.writeInt(equip.getExpPercentage() * 100000); // Item Exp... 10000000 = 100%
                mplew.writeInt(equip.getDurability());
                mplew.writeInt(equip.getViciousHammer());
//                if (!hasUniqueId) {
//                    if (equip.getPotential1() >= 30041 && equip.getPotential1() <= 30044) {
//                        //님들 나중에 레전 등급 입맛대로 더 구현해주려고 44까지 뚫어놔준거긴한데
//                        //조건문 보면 알겠지만 42~44일 경우만 치환 작동되게 해놨거든 니도 보면 알지? 네 근데 지금 뜨는 올스텟은 사실상 다른스텟인거고 원래 지금 레전더리로 가있는 스텟이네요 
//            //근데 길게말하면 귀찮으니까 42번부터 44번까지만 해달라해 유니크로 근데 그러면 제가 지금 궁금한게 이게 사실상 코드를 더만들어서 추가를 해줘야 하는데 안해준거네요
//            //내가해봤어야알지 일단 치환식해둔다 
//                        mplew.write(7);
//                        mplew.write(equip.getEnhance());
//                        mplew.writeShort(30086); // 86네
//                        //mplew.writeShort(equip.getPotential1());
//                        mplew.writeShort(equip.getPotential2());
//                        mplew.writeShort(equip.getPotential3());
//                    } else {
                        mplew.write(equip.getState()); //7 = unique for the lulz equip.getState()
                        mplew.write(equip.getEnhance());
                        mplew.writeShort(equip.getPotential1());
                        mplew.writeShort(equip.getPotential2());
                        mplew.writeShort(equip.getPotential3());
//                    }
//                }

                mplew.writeShort(equip.getHpR());
                mplew.writeShort(equip.getMpR());
                
                mplew.writeShort(str);
                mplew.writeShort(dex);
                mplew.writeShort(in);
                mplew.writeShort(luk);
                mplew.writeShort(watk);
                mplew.writeShort(matk);
                
                if (!hasUniqueId) {
                    mplew.writeLong(equip.getInventoryId() <= 0 ? -1 : equip.getInventoryId()); //some tracking ID
                }
                mplew.writeLong(getTime(-2));
                mplew.writeInt(-1); //?
            } else {
                mplew.writeShort(item.getQuantity());
                mplew.writeMapleAsciiString(item.getOwner());
                mplew.writeShort(item.getFlag());
                if (GameConstants.isThrowingStar(item.getItemId()) || GameConstants.isBullet(item.getItemId()) || item.getItemId() / 10000 == 287) {
                    mplew.writeLong(item.getInventoryId() <= 0 ? -1 : item.getInventoryId());
                }
            }
        }
    }

    public static final void serializeMovementList(final MaplePacketLittleEndianWriter lew, final List<LifeMovementFragment> moves) {
        lew.write(moves.size());
        for (LifeMovementFragment move : moves) {
            move.serialize(lew);
        }
    }

    public static final void addAnnounceBox(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        if (chr.getPlayerShop() != null && chr.getPlayerShop().isOwner(chr) && chr.getPlayerShop().getShopType() != 1 && chr.getPlayerShop().isAvailable()) {
            addInteraction(mplew, chr.getPlayerShop());
        } else {
            mplew.write(0);
        }
    }

    public static final void addInteraction(final MaplePacketLittleEndianWriter mplew, IMaplePlayerShop shop) {
        mplew.write(shop.getGameType());
        mplew.writeInt(((AbstractPlayerStore) shop).getObjectId());
        mplew.writeMapleAsciiString(shop.getDescription());
        if (shop.getShopType() != 1) {
            mplew.write(shop.getPassword().length() > 0 ? 1 : 0); //password = false
        }
        mplew.write(shop.getItemId() % 10);
        mplew.write(shop.getSize()); //current size
        mplew.write(shop.getMaxSize()); //full slots... 4 = 4-1=3 = has slots, 1-1=0 = no slots
        if (shop.getShopType() != 1) {
            mplew.write(shop.isOpen() ? 0 : 1);
        }
    }

    public static final void addCharacterInfo(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        mplew.writeLong(-1);
        mplew.write(0);
        mplew.write(0);
        addCharStats(mplew, chr);
        mplew.write(chr.getBuddylist().getCapacity());
        // Bless
        if (chr.getBlessOfFairyOrigin() != null) {
            mplew.write(1);
            mplew.writeMapleAsciiString(chr.getBlessOfFairyOrigin());
        } else {
            mplew.write(0);
        }
        /*if (chr.getBlessOfEmpressOrigin() != null) {
         mplew.write(1);
         mplew.writeMapleAsciiString(chr.getBlessOfEmpressOrigin());
         } else {
         mplew.write(0);
         }*/
        /*final MapleQuestStatus ultExplorer = chr.getQuestNoAdd(MapleQuest.getInstance(GameConstants.ULT_EXPLORER));
         if (ultExplorer != null && ultExplorer.getCustomData() != null) {
         mplew.write(1);
         mplew.writeMapleAsciiString(ultExplorer.getCustomData());
         } else {
         mplew.write(0);
         }*/
        //AFTERSHOCK: EMPRESS ORIGIN (same structure)
        //AFTERSHOCK: UA LINK TO CYGNUS (same structure)
        // End
        addInventoryInfo(mplew, chr);
        addSkillInfo(mplew, chr); //ㅇ
        addCoolDownInfo(mplew, chr);//ㅇ
        addQuestInfo(mplew, chr); //ㅇ
        addRingInfo(mplew, chr); //??
        addRocksInfo(mplew, chr); //ㅇ
        chr.QuestInfoPacket(mplew); // for every questinfo: int16_t questid, string questdata
        if (chr.getJob() >= 3300 && chr.getJob() <= 3312) { //wh
            addJaguarInfo(mplew, chr);
        }
        mplew.writeShort(0); // 이 값이 1이면 두와일 돌리는데 뭔지모르겠음
    }

    public static final void addMonsterBookInfo(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        mplew.writeInt(0); //something
        if (chr.getMonsterBook().getSetScore() > 0) {
            chr.getMonsterBook().writeFinished(mplew);
        } else {
            chr.getMonsterBook().writeUnfinished(mplew);
        }
        mplew.writeInt(chr.getMonsterBook().getSet());
        mplew.writeZeroBytes(9); //tespia lol

        /*int totalVitality = 0;
         mplew.writeInt(chr.getFamiliars().size()); //size
         for (MonsterFamiliar mf : chr.getFamiliars().values()) {
         mf.writeRegisterPacket(mplew, true);
         totalVitality += mf.getVitality();
         }

         mplew.writeInt(totalVitality); //size of ALL not just stacked
         for (MonsterFamiliar mf : chr.getFamiliars().values()) {
         for (int i = 0; i < mf.getVitality(); i++) {
         mplew.writeInt(chr.getId());
         mplew.writeInt(mf.getFamiliar());
         mplew.writeLong(mf.getId() + (100000 * i)); //fake it like a pro
         mplew.write(1);
         }
         }*/
    }

    public static final void addPetItemInfo(final MaplePacketLittleEndianWriter mplew, final Item item, final MaplePet pet, final boolean active) {
        //PacketHelper.addExpirationTime(mplew, -1); //always
        if (item == null) {
            mplew.writeLong(PacketHelper.getKoreanTimestamp((long) (System.currentTimeMillis() * 1.5)));
        } else {
            PacketHelper.addExpirationTime(mplew, item.getExpiration() <= System.currentTimeMillis() ? -1 : item.getExpiration());
        }
        //mplew.writeInt(-1);
        mplew.writeAsciiString(pet.getName(), 13);
        mplew.write(pet.getLevel());
        mplew.writeShort(pet.getCloseness());
        mplew.write(pet.getFullness());
        if (item == null) {
            mplew.writeLong(PacketHelper.getKoreanTimestamp((long) (System.currentTimeMillis() * 1.5)));
        } else {
            PacketHelper.addExpirationTime(mplew, item.getExpiration() <= System.currentTimeMillis() ? -1 : item.getExpiration());
        }
        mplew.writeShort(pet.getSpeed());
        mplew.writeShort(pet.getFlags());
        mplew.writeInt(pet.getPetItemId() == 5000054 && pet.getSecondsLeft() > 0 ? pet.getSecondsLeft() : 0); //in seconds, 3600 = 1 hr.
        mplew.writeShort(0);
        mplew.write(active ? (pet.getSummoned() ? pet.getSummonedValue() : 0) : 0); // 1C 5C 98 C6 01
        mplew.writeInt(active ? pet.getBuffSkill() : 0);
    }

    public static final void addShopInfo(final MaplePacketLittleEndianWriter mplew, final MapleShop shop, final MapleClient c) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        mplew.writeShort(shop.getItems().size()); // item count
        for (MapleShopItem item : shop.getItems()) {
            addShopItemInfo(mplew, item, shop, ii, null);
        }
    }

    public static final void addShopItemInfo(final MaplePacketLittleEndianWriter mplew, final MapleShopItem item, final MapleShop shop, final MapleItemInformationProvider ii, final Item i) {
        mplew.writeInt(item.getItemId());
        mplew.writeInt(item.getPrice());
        mplew.writeInt(item.getReqItem());
        mplew.writeInt(item.getReqItemQ());
        mplew.writeLong(item.getExpire());//유통기한 //상점팅의심
        if (!GameConstants.isThrowingStar(item.getItemId()) && !GameConstants.isBullet(item.getItemId())) {
            mplew.writeShort(1); // 화살 스택갯수
            mplew.writeShort(item.getBuyable()); //살수있는 갯수
        } else {
            mplew.writeShort(0);
            mplew.writeInt(0);
            mplew.writeShort(BitTools.doubleToShortBits(ii.getPrice(item.getItemId())));
            mplew.writeShort(ii.getSlotMax(item.getItemId()));
        }
    }

    public static final void addJaguarInfo(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        mplew.write(chr.getIntNoRecord(GameConstants.JAGUAR));
        mplew.writeInt(100100); //probably mobID of the 5 mobs that can be captured.
        mplew.writeInt(100001);
        mplew.writeInt(1210104);
        mplew.writeInt(2220100);
        mplew.writeInt(7130401);

    }

    public static <E extends Buffstat> void writeSingleMask(MaplePacketLittleEndianWriter mplew, E statup) {
        for (int i = GameConstants.MAX_BUFFSTAT; i >= 1; i--) {
            mplew.writeInt(i == statup.getPosition() ? statup.getValue() : 0);
        }
    }

    public static <E extends Buffstat> void writeMask(MaplePacketLittleEndianWriter mplew, Collection<E> statups) {
        int[] mask = new int[GameConstants.MAX_BUFFSTAT];
        for (E statup : statups) {
            mask[statup.getPosition() - 1] |= statup.getValue();
        }
        for (int i = mask.length; i >= 1; i--) {
            mplew.writeInt(mask[i - 1]);
        }
    }

    public static <E extends Buffstat> void writeBuffMask(MaplePacketLittleEndianWriter mplew, Collection<Pair<E, Integer>> statups) {
        int[] mask = new int[GameConstants.MAX_BUFFSTAT];
        for (Pair<E, Integer> statup : statups) {
            mask[statup.left.getPosition() - 1] |= statup.left.getValue();
        }
        for (int i = mask.length; i >= 1; i--) {
            mplew.writeInt(mask[i - 1]);
        }
    }

    public static <E extends Buffstat> void writeBuffMask(MaplePacketLittleEndianWriter mplew, Map<E, Integer> statups) {
        int[] mask = new int[GameConstants.MAX_BUFFSTAT];
        for (E statup : statups.keySet()) {
            mask[statup.getPosition() - 1] |= statup.getValue();
        }
        for (int i = mask.length; i >= 1; i--) {
            mplew.writeInt(mask[i - 1]);
            /*if ((mask[i - 1]) > 0) {
             mplew.writeInt(mask[i - 1]);
             //mplew.writeInt(0x4000000);
             } else {
             mplew.writeInt(mask[i - 1]);
             }*/
        }
    }

}
