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
package client.inventory;

import constants.GameConstants;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import server.MapleItemInformationProvider;
import server.Randomizer;
import server.StructPotentialItem;

public class Equip extends Item implements Serializable {

    public static enum ScrollResult {

        SUCCESS, FAIL, CURSE
    }
    public static final int ARMOR_RATIO = 350000;
    public static final int WEAPON_RATIO = 700000;
    //charm: -1 = has not been initialized yet, 0 = already been worn, >0 = has teh charm exp
    private byte upgradeSlots = 0, level = 0, vicioushammer = 0, enhance = 0;
    private short str = 0, dex = 0, _int = 0, luk = 0, hp = 0, mp = 0, watk = 0, matk = 0, wdef = 0, mdef = 0, acc = 0, avoid = 0, hands = 0, speed = 0, jump = 0, hpR = 0, mpR = 0, charmExp = 0, pvpDamage = 0;
    private int itemEXP = 0, durability = -1, incSkill = -1, potential1 = 0, potential2 = 0, potential3 = 0;
    private MapleRing ring = null;
    private MapleAndroid android = null;
    private long equippedTime = 0;

    public Equip(int id, short position, byte flag) {
        super(id, position, (short) 1, flag);
    }

    public Equip(int id, short position, int uniqueid, short flag) {
        super(id, position, (short) 1, flag, uniqueid);
    }

    @Override
    public Item copy() {
        Equip ret = new Equip(getItemId(), getPosition(), getUniqueId(), getFlag());
        ret.str = str;
        ret.dex = dex;
        ret._int = _int;
        ret.luk = luk;
        ret.hp = hp;
        ret.mp = mp;
        ret.matk = matk;
        ret.mdef = mdef;
        ret.watk = watk;
        ret.wdef = wdef;
        ret.acc = acc;
        ret.avoid = avoid;
        ret.hands = hands;
        ret.speed = speed;
        ret.jump = jump;
        ret.enhance = enhance;
        ret.upgradeSlots = upgradeSlots;
        ret.level = level;
        ret.itemEXP = itemEXP;
        ret.durability = durability;
        ret.vicioushammer = vicioushammer;
        ret.potential1 = potential1;
        ret.potential2 = potential2;
        ret.potential3 = potential3;
        ret.charmExp = charmExp;
        ret.pvpDamage = pvpDamage;
        ret.hpR = hpR;
        ret.mpR = mpR;
        ret.incSkill = incSkill;
        ret.setGiftFrom(getGiftFrom());
        ret.setOwner(getOwner());
        ret.setQuantity(getQuantity());
        ret.setExpiration(getExpiration());
        return ret;
    }

    @Override
    public byte getType() {
        return 1;
    }

    @Override
    public long getEquippedTime() {
        return equippedTime;
    }

    @Override
    public void setEquippedTime(long equippedTime) {
        this.equippedTime = equippedTime;
    }

    public byte getUpgradeSlots() {
        return upgradeSlots;
    }

    public short getStr() {
        return str;
    }

    public short getDex() {
        return dex;
    }

    public short getInt() {
        return _int;
    }

    public short getLuk() {
        return luk;
    }

    public short getHp() {
        return hp;
    }

    public short getMp() {
        return mp;
    }

    public short getWatk() {
        return watk;
    }

    public short getMatk() {
        return matk;
    }

    public short getWdef() {
        return wdef;
    }

    public short getMdef() {
        return mdef;
    }

    public short getAcc() {
        return acc;
    }

    public short getAvoid() {
        return avoid;
    }

    public short getHands() {
        return hands;
    }

    public short getSpeed() {
        return speed;
    }

    public short getJump() {
        return jump;
    }

    public void setStr(short str) {
        if (str < 0) {
            str = 0;
        }
        this.str = str;
    }

    public void setDex(short dex) {
        if (dex < 0) {
            dex = 0;
        }
        this.dex = dex;
    }

    public void setInt(short _int) {
        if (_int < 0) {
            _int = 0;
        }
        this._int = _int;
    }

    public void setLuk(short luk) {
        if (luk < 0) {
            luk = 0;
        }
        this.luk = luk;
    }

    public void setHp(short hp) {
        if (hp < 0) {
            hp = 0;
        }
        this.hp = hp;
    }

    public void setMp(short mp) {
        if (mp < 0) {
            mp = 0;
        }
        this.mp = mp;
    }

    public void setWatk(short watk) {
        if (watk < 0) {
            watk = 0;
        }
        this.watk = watk;
    }

    public void setMatk(short matk) {
        if (matk < 0) {
            matk = 0;
        }
        this.matk = matk;
    }

    public void setWdef(short wdef) {
        if (wdef < 0) {
            wdef = 0;
        }
        this.wdef = wdef;
    }

    public void setMdef(short mdef) {
        if (mdef < 0) {
            mdef = 0;
        }
        this.mdef = mdef;
    }

    public void setAcc(short acc) {
        if (acc < 0) {
            acc = 0;
        }
        this.acc = acc;
    }

    public void setAvoid(short avoid) {
        if (avoid < 0) {
            avoid = 0;
        }
        this.avoid = avoid;
    }

    public void setHands(short hands) {
        if (hands < 0) {
            hands = 0;
        }
        this.hands = hands;
    }

    public void setSpeed(short speed) {
        if (speed < 0) {
            speed = 0;
        }
        this.speed = speed;
    }

    public void setJump(short jump) {
        if (jump < 0) {
            jump = 0;
        }
        this.jump = jump;
    }

    public void setUpgradeSlots(byte upgradeSlots) {
        this.upgradeSlots = upgradeSlots;
    }

    public byte getLevel() {
        return level;
    }

    public void setLevel(byte level) {
        this.level = level;
    }

    public byte getViciousHammer() {
        return vicioushammer;
    }

    public void setViciousHammer(byte ham) {
        vicioushammer = ham;
    }

    public int getItemEXP() {
        return itemEXP;
    }

    public void setItemEXP(int itemEXP) {
        if (itemEXP < 0) {
            itemEXP = 0;
        }
        this.itemEXP = itemEXP;
    }

    public int getEquipExp() {
        if (itemEXP <= 0) {
            return 0;
        }
        //aproximate value
        if (GameConstants.isWeapon(getItemId())) {
            return itemEXP / WEAPON_RATIO;
        } else {
            return itemEXP / ARMOR_RATIO;
        }
    }

    public int getEquipExpForLevel() {
        if (getEquipExp() <= 0) {
            return 0;
        }
        int expz = getEquipExp();
        for (int i = getBaseLevel(); i <= GameConstants.getMaxLevel(getItemId()); i++) {
            if (expz >= GameConstants.getExpForLevel(i, getItemId())) {
                expz -= GameConstants.getExpForLevel(i, getItemId());
            } else { //for 0, dont continue;
                break;
            }
        }
        return expz;
    }

    public int getExpPercentage() {
        if (getEquipLevel() < getBaseLevel() || getEquipLevel() > GameConstants.getMaxLevel(getItemId()) || GameConstants.getExpForLevel(getEquipLevel(), getItemId()) <= 0) {
            return 0;
        }
        return getEquipExpForLevel() * 100 / GameConstants.getExpForLevel(getEquipLevel(), getItemId());
    }

    public int getEquipLevel() {
        if (GameConstants.getMaxLevel(getItemId()) <= 0) {
            return 0;
        } else if (getEquipExp() <= 0) {
            return getBaseLevel();
        }
        int levelz = getBaseLevel();
        int expz = getEquipExp();
        for (int i = levelz; (GameConstants.getStatFromWeapon(getItemId()) == null ? (i <= GameConstants.getMaxLevel(getItemId())) : (i < GameConstants.getMaxLevel(getItemId()))); i++) {
            if (expz >= GameConstants.getExpForLevel(i, getItemId())) {
                levelz++;
                expz -= GameConstants.getExpForLevel(i, getItemId());
            } else { //for 0, dont continue;
                break;
            }
        }
        return levelz;
    }

    public int getBaseLevel() {
        return (GameConstants.getStatFromWeapon(getItemId()) == null ? 1 : 0);
    }

    @Override
    public void setQuantity(short quantity) {
        if (quantity < 0 || quantity > 1) {
            throw new RuntimeException("Setting the quantity to " + quantity + " on an equip (itemid: " + getItemId() + ")");
        }
        super.setQuantity(quantity);
    }

    public int getDurability() {
        return durability;
    }

    public void setDurability(final int dur) {
        this.durability = dur;
    }

    public byte getEnhance() {
        return enhance;
    }

    public void setEnhance(final byte en) {
        this.enhance = en;
    }

    public int getPotential1() {
        return potential1;
    }

    public void setPotential1(final int en) {
        this.potential1 = en;
    }

    public int getPotential2() {
        return potential2;
    }

    public void setPotential2(final int en) {
        this.potential2 = en;
    }

    public int getPotential3() {
        return potential3;
    }

    public void setPotential3(final int en) {
        this.potential3 = en;
    }

    public byte getState() {
        final int pots = potential1 + potential2 + potential3;
        if (potential1 >= 30000 || potential2 >= 30000 || potential3 >= 30000) {
            return 7;
        } else if (potential1 >= 20000 || potential2 >= 20000 || potential3 >= 20000) {
            return 6;
        } else if (pots >= 1) {
            return 5;
        } else if (pots < 0) {
            return 1;
        }
        return 0;
    }

    public void resetPotential_Fuse(boolean half, int potentialState) { //메이커로 만들때
        //0.16% chance unique, 4% chance epic, else rare
        potentialState = -potentialState;
        if (Randomizer.nextInt(100) < 4) {
            potentialState -= Randomizer.nextInt(100) < 4 ? (GameConstants.GMS && Randomizer.nextInt(100) < 4 ? 3 : 2) : 1;
        }
        setPotential1((short) potentialState);
        setPotential2((short) (Randomizer.nextInt(half ? 5 : 10) == 0 ? potentialState : 0)); //1/10 chance of 3 line
        setPotential3((short) 0); //just set it theoretically
    }

    public void resetPotential() { //줌서 바를떄 //아마 잠재능력주문서인듯
        //0.16% chance unique, 4% chance epic, else rare
        int rank = Randomizer.nextInt(100) < 4 ? (Randomizer.nextInt(100) < 4 ? -7 : -6) : -5;
        setPotential1((short) rank);
        setPotential2((short) (Randomizer.nextInt(9) == 0 ? rank : 0)); //1/10 chance of 3 line
        setPotential3((short) 0); //just set it theoretically
    }

    //안쓰이는 함수
    public void renewPotential(boolean prem, int type) { //큐브 등급
        /* @author : vestia_adm@naver.com
        * type
        * 0 : 미라클 큐브
        * 1 : 레드 큐브
        * 2 : 마스터 미라클 큐브 
        * 3 : 플래티넘 미라클 큐브
        * */
        final boolean suspicious_rare = Randomizer.nextInt(1000) < 0; // 수상한 미라클 큐브 레어 하락 확률 7%~8%  //1=0.1%
        final boolean suspicious_epic = Randomizer.nextInt(1000) < 9; // 수상한 미라클  큐브 에픽 확률 0.9%
        final boolean suspicious_unique = Randomizer.nextInt(1000) < 2; //미라클 큐브 유니크 확률 0.2%
        final boolean miracle_epic = Randomizer.nextInt(1000) < 30; // 레드 큐브 에픽 확률 3%
        final boolean miracle_unique = Randomizer.nextInt(1000) < 9; // 레드 큐브 유니크 확률 0.9% 
        final boolean premium_epic = Randomizer.nextInt(1000) < 50; // 마스터 미라클 큐브 미라클 에픽 확률 5%
        final boolean premium_unique = Randomizer.nextInt(1000) < 15; // 마스터 미라클 큐브 유니크 확률 1.5%
        final boolean platinum_epic = Randomizer.nextInt(1000) < 1000; // 플래티넘 미라클 큐브 미라클 에픽 확률 
        final boolean platinum_unique = Randomizer.nextInt(1000) < 1000; // 플래티넘 미라클 큐브 유니크 확률       

        if (getState() == 6) { // 에픽
            if (type == 0) {
                final int rank = suspicious_unique && getState() != 8 ? -(getState() + 1) : -(getState());
                setPotential1((short) rank);
                setPotential2((short) (getPotential3() > 0 || (prem && Randomizer.nextInt(10) == 0) ? rank : 0));
                setPotential3((short) 0);
            } else if (type == 1) {
                final int rank = miracle_unique && getState() != 8 ? -(getState() + 1) : -(getState());
                setPotential1((short) rank);
                setPotential2((short) (getPotential3() > 0 || (prem && Randomizer.nextInt(10) == 0) ? rank : 0));
                setPotential3((short) 0);
            } else if (type == 2) {
                final int rank = premium_unique && getState() != 8 ? -(getState() + 1) : -(getState());
                setPotential1((short) rank);
                setPotential2((short) (getPotential3() > 0 || (prem && Randomizer.nextInt(10) == 0) ? rank : 0));
                setPotential3((short) 0);
            } else if (type == 3) {
                final int rank = platinum_unique && getState() != 8 ? -(getState() + 1) : -(getState());
                setPotential1((short) rank);
                setPotential2((short) (getPotential3() > 0 || (prem && Randomizer.nextInt(10) == 0) ? rank : 0));
                setPotential3((short) 0);
            }
        } else if (getState() == 5) { // 레어
            if (type == 0) {
                final int rank = suspicious_epic && getState() != 8 ? -(getState() + 1) : -(getState());
                setPotential1((short) rank);
                setPotential2((short) (getPotential3() > 0 || (prem && Randomizer.nextInt(10) == 0) ? rank : 0));
                setPotential3((short) 0);
            } else if (type == 1) {
                final int rank = miracle_epic && getState() != 8 ? -(getState() + 1) : -(getState());
                setPotential1((short) rank);
                setPotential2((short) (getPotential3() > 0 || (prem && Randomizer.nextInt(10) == 0) ? rank : 0));
                setPotential3((short) 0);
            } else if (type == 2) {
                final int rank = premium_epic && getState() != 8 ? -(getState() + 1) : -(getState());
                setPotential1((short) rank);
                setPotential2((short) (getPotential3() > 0 || (prem && Randomizer.nextInt(10) == 0) ? rank : 0));
                setPotential3((short) 0);
            } else if (type == 3) {
                final int rank = platinum_epic && getState() != 8 ? -(getState() + 1) : -(getState());
                setPotential1((short) rank);
                setPotential2((short) (getPotential3() > 0 || (prem && Randomizer.nextInt(10) == 0) ? rank : 0));
                setPotential3((short) 0);
            }
        } else if (getState() == 7) { // 유니크
            if (type == 0) {
                final int rank = suspicious_epic && getState() != 8 ? -(getState() + 1) : -(getState());
                setPotential1((short) rank);
                setPotential2((short) (getPotential3() > 0 || (prem && Randomizer.nextInt(10) == 0) ? rank : 0));
                setPotential3((short) 0);
            } else if (type == 1) {
                final int rank = miracle_epic && getState() != 8 ? -(getState() + 1) : -(getState());
                setPotential1((short) rank);
                setPotential2((short) (getPotential3() > 0 || (prem && Randomizer.nextInt(10) == 0) ? rank : 0));
                setPotential3((short) 0);
            } else if (type == 2) {
                final int rank = premium_epic && getState() != 8 ? -(getState() + 1) : -(getState());
                setPotential1((short) rank);
                setPotential2((short) (getPotential3() > 0 || (prem && Randomizer.nextInt(10) == 0) ? rank : 0));
                setPotential3((short) 0);
            } else if (type == 3) {
                final int rank = platinum_epic && getState() != 8 ? -(getState() + 1) : -(getState());
                setPotential1((short) rank);
                setPotential2((short) (getPotential3() > 0 || (prem && Randomizer.nextInt(10) == 0) ? rank : 0));
                setPotential3((short) 0);
            }
        } else { // 레전드리
            final int rank = Randomizer.nextInt(100) < 4 && getState() != 8 ? -(getState() + 1) : -(getState());
            setPotential1((short) rank);
            setPotential2((short) (getPotential3() > 0 || (prem && Randomizer.nextInt(10) == 0) ? rank : 0));
            setPotential3((short) 0);
        }
        //  final Item magnify = this;
        Equip toReveal;
        toReveal = this;
        final Equip eqq = (Equip) toReveal;
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final int reqLevel = ii.getReqLevel(eqq.getItemId()) / 10;
        final List<List<StructPotentialItem>> pots = new LinkedList<List<StructPotentialItem>>(ii.getAllPotentialInfo().values());
        int new_state = Math.abs(eqq.getPotential1());
        int legendary_state = Math.abs(eqq.getPotential1());
        if (new_state == 8) {
            new_state = 7;
        } else if (new_state == 9) {
            new_state = 5;
        } else if (new_state == 10) {
            new_state = 6;
        } else if (new_state == 11) {
            new_state = 7;
        } else if (new_state == 12) {
            new_state = 7;
        } else if (new_state > 12 || new_state < 5) {
            new_state = 5;
        }
        if (legendary_state > 12 || legendary_state < 5) {
            legendary_state = 5;
        }
        final int lines = (eqq.getPotential2() != 0 ? 3 : 2);
        while (eqq.getState() != new_state) {
            //31001 = haste, 31002 = door, 31003 = se, 31004 = hb
            for (int i = 0; i < lines; i++) { //2 or 3 line
                boolean rewarded = false;
                while (!rewarded) {
                    // System.out.println("pots :" + pots + " new_state :" + new_state + " lines :" + lines + " reqLevel :" + reqLevel);
                    StructPotentialItem pot = pots.get(Randomizer.nextInt(pots.size())).get(reqLevel);
                    //System.out.println("pot :" + pot);
                    //if (pot != null && pot.reqLevel / 10 <= reqLevel && GameConstants.optionTypeFits(pot.optionType, eqq.getItemId()) && GameConstants.potentialIDFits(pot.potentialID, legendary_state, i, 10)) { //optionType
                    if (pot != null && pot.reqLevel / 10 <= reqLevel && GameConstants.optionTypeFits(pot.optionType, eqq.getItemId()) && GameConstants.potentialIDFits(pot.potentialID, legendary_state, i)) {
                        //have to research optionType before making this truely sea-like
                        if (i == 0) {
                            eqq.setPotential1(pot.potentialID);
                        } else if (i == 1) {
                            eqq.setPotential2(pot.potentialID);
                        } else if (i == 2) {
                            eqq.setPotential3(pot.potentialID);
                        }
                        rewarded = true;
                    }
                }
            }
        }
    }

    public short getHpR() {
        return hpR;
    }

    public void setHpR(final short hp) {
        this.hpR = hp;
    }

    public short getMpR() {
        return mpR;
    }

    public void setMpR(final short mp) {
        this.mpR = mp;
    }

    public int getIncSkill() {
        return incSkill;
    }

    public void setIncSkill(int inc) {
        this.incSkill = inc;
    }

    public short getCharmEXP() {
        return charmExp;
    }

    public short getPVPDamage() {
        return pvpDamage;
    }

    public void setCharmEXP(short s) {
        this.charmExp = s;
    }

    public void setPVPDamage(short p) {
        this.pvpDamage = p;
    }

    public MapleRing getRing() {
        if (!GameConstants.isEffectRing(getItemId()) || getUniqueId() <= 0) {
            return null;
        }
        if (ring == null) {
            ring = MapleRing.loadFromDb(getUniqueId(), getPosition() < 0);
        }
        return ring;
    }

    public void setRing(MapleRing ring) {
        this.ring = ring;
    }

    public MapleAndroid getAndroid() {
        if (getItemId() / 10000 != 166 || getUniqueId() <= 0) {
            return null;
        }
        if (android == null) {
            android = MapleAndroid.loadFromDb(getItemId(), getUniqueId());
        }
        return android;
    }

    public void setAndroid(MapleAndroid ring) {
        this.android = ring;
    }
    
    public long newRebirth(int scrollId, boolean update) {
        if (GameConstants.isRing(this.getItemId()) || this.getItemId() / 1000 == 1092 || this.getItemId() / 1000 == 1342 || this.getItemId() / 1000 == 1713 || this.getItemId() / 1000 == 1712 || this.getItemId() / 1000 == 1152 || this.getItemId() / 1000 == 1142 || this.getItemId() / 1000 == 1143 || this.getItemId() / 1000 == 1672 || this.getItemId() / 1000 == 1190 || this.getItemId() / 1000 == 1191 || this.getItemId() / 1000 == 1182 || this.getItemId() / 1000 == 1662 || this.getItemId() / 1000 == 1802) {
            return 0;
        }
        int maxValue = 5;
        if (MapleItemInformationProvider.getInstance().getName(scrollId) != null) {
            if (MapleItemInformationProvider.getInstance().getName(scrollId).contains("강력한 환생의")) {
                maxValue = 6;
            }
            if (MapleItemInformationProvider.getInstance().getName(scrollId).contains("영원한 환생의") || MapleItemInformationProvider.getInstance().getName(scrollId).contains("검은 환생의")) {
                maxValue = 7; //여기고 
            }
        }
        if (scrollId == 2049010) { // 최소 2
            maxValue = 4; // 여기란다 
        }
        Equip ordinary = (Equip) MapleItemInformationProvider.getInstance().getEquipById(this.getItemId());
        short ordinaryPad = ordinary.watk > 0 ? ordinary.watk : ordinary.matk;
        short ordinaryMad = ordinary.matk > 0 ? ordinary.matk : ordinary.watk;
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        long[] rebirth = new long[]{-1L, -1L, -1L, -1L};
        int[] rebirthOptions = new int[]{-1, -1, -1, -1};
        for (int i = 0; i < 4; ++i) {
            int randomOption = Randomizer.nextInt(12);
            while (rebirthOptions[0] == randomOption || rebirthOptions[1] == randomOption || rebirthOptions[2] == randomOption || rebirthOptions[3] == randomOption || randomOption == 12 || randomOption == 14 || randomOption == 15 || randomOption == 16 || !GameConstants.isWeapon(this.getItemId()) && (randomOption == 21 || randomOption == 23)) {
                randomOption = Randomizer.nextInt(12);
            }
            rebirthOptions[i] = randomOption;
            int randomValue = 0;
            randomValue = (randomOption == 17 || randomOption == 18) && !GameConstants.isWeapon(this.getItemId()) /*|| randomOption == 22*/ ? Randomizer.rand(1, maxValue) : Randomizer.rand(2, maxValue);
            rebirth[i] = randomOption * 10 + randomValue;
            for (int j = 0; j < i; ++j) {
                int n = i;
                rebirth[n] = rebirth[n] * 1000L;
            }
            if (!update) {
                continue;
            }
        }
        return rebirth[0] + rebirth[1] + rebirth[2] + rebirth[3];
    }
}
