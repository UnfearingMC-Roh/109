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

import client.inventory.Equip;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import constants.GameConstants;
import client.Skill;
import client.inventory.Item;
import client.MapleDisease;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.inventory.MapleInventoryType;
import client.MapleClient;
import handling.channel.ChannelServer;
import client.SkillFactory;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.ServerConstants;

import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.World;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import scripting.EventInstanceManager;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.Randomizer;
import server.RateManager;
import server.Start;
import server.Timer.EtcTimer;
import server.Timer.PoisonTimer;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import tools.ArrayMap;
import tools.ConcurrentEnumMap;
import tools.FileoutputUtil;
import tools.Pair;
import tools.MaplePacketCreator;
import tools.StringUtil;
import tools.packet.MobPacket;

public class MapleMonster extends AbstractLoadedMapleLife {

    private MapleMonsterStats stats;
    private ChangeableStats ostats = null;
    private long hp, nextKill = 0, lastDropTime = 0;
    private int mp;
    private byte carnivalTeam = -1;
    private MapleMap map;
    private boolean fake = false, dropsDisabled = false, controllerHasAggro = false, nextAttackPossible;
    private WeakReference<MapleMonster> sponge = new WeakReference<MapleMonster>(null);
    private int linkoid = 0, lastNode = -1, highestDamageChar = 0, linkCID = 0; // Just a reference for monster EXP distribution after dead
    private WeakReference<MapleCharacter> controller = new WeakReference<MapleCharacter>(null);
    private final List<AttackerEntry> attackers = new LinkedList<AttackerEntry>();
    //private final Collection<AttackerEntry> attackers = new LinkedList<AttackerEntry>(); //좀비몹,유령몹현상 픽스
    private EventInstanceManager eventInstance;
    private MonsterListener listener = null;
    private byte[] reflectpack = null, nodepack = null;
    private final ConcurrentEnumMap<MonsterStatus, MonsterStatusEffect> stati = new ConcurrentEnumMap<MonsterStatus, MonsterStatusEffect>(MonsterStatus.class);
    private final LinkedList<MonsterStatusEffect> poisons = new LinkedList<MonsterStatusEffect>();
    private final ReentrantReadWriteLock poisonsLock = new ReentrantReadWriteLock();
    private Map<Integer, Long> usedSkills;
    private int stolen = -1; //monster can only be stolen ONCE
    private boolean shouldDropItem = false, killed = false;
    private int eventDrop = 0;
    private long lastReceivedMovePacket = System.currentTimeMillis();
    private List<Integer> reflections = new ArrayList<Integer>();
    private boolean broadcastedEventGM = false;
    private boolean broadcastedEvent = false;
    
    public List<MapleCharacter> getAttackUsers() {
        List<MapleCharacter> users = new ArrayList<>();
        for (final AttackerEntry mattacker : getAttackers()) {
            if (mattacker != null) {
                for (final AttackingMapleCharacter cattacker : mattacker.getAttackers()) {
                    if (cattacker != null) {
                        MapleCharacter user = cattacker.getAttacker();
                        if (user != null)
                            users.add(user);
                    }
                }
            }
        }
        if (users.isEmpty())
            return null;
        return users;
    }

    public MapleMonster(final int id, final MapleMonsterStats stats) {
        super(id);
        initWithStats(stats);
    }

    public MapleMonster(final MapleMonster monster) {
        super(monster);
        initWithStats(monster.stats);
    }

    public long getLastReceivedMovePacket() {
        return lastReceivedMovePacket;
    }

    public void receiveMovePacket() {
        lastReceivedMovePacket = System.currentTimeMillis();
    }

    private final void initWithStats(final MapleMonsterStats stats) {
        setStance(5);
        this.stats = stats;
        hp = stats.getHp();
        mp = stats.getMp();

        if (stats.getNoSkills() > 0) {
            usedSkills = new HashMap<Integer, Long>();
        }
    }

    public final ArrayList<AttackerEntry> getAttackers() {
        if (attackers == null || attackers.size() <= 0) {
            return new ArrayList<AttackerEntry>();
        }
        ArrayList<AttackerEntry> ret = new ArrayList<AttackerEntry>();
        for (AttackerEntry e : attackers) {
            if (e != null) {
                ret.add(e);
            }
        }
        return ret;
    }

    public void setEventDropFlag(int flag) {
        this.eventDrop = flag;
    }

    public final int getEventDropFlag() {
        return eventDrop;
    }

    public final MapleMonsterStats getStats() {
        return stats;
    }

    public final void disableDrops() {
        this.dropsDisabled = true;
    }

    public final boolean dropsDisabled() {
        return dropsDisabled;
    }

    public final void setSponge(final MapleMonster mob) {
        sponge = new WeakReference<MapleMonster>(mob);
        if (linkoid <= 0) {
            linkoid = mob.getObjectId();
        }
    }

    public final void setMap(final MapleMap map) {
        this.map = map;
        startDropItemSchedule();
    }

    public final long getHp() {
        return hp;
    }

    public final void setHp(long hp) {
        this.hp = hp;
    }

    public final ChangeableStats getChangedStats() {
        return ostats;
    }

    public final long getMobMaxHp() {
        if (ostats != null) {
            return ostats.hp;
        }
        return stats.getHp();
    }

    public final int getMp() {
        return mp;
    }

    public final void setMp(int mp) {
        if (mp < 0) {
            mp = 0;
        }
        this.mp = mp;
    }

    public final int getMobMaxMp() {
        if (ostats != null) {
            return ostats.mp;
        }
        return stats.getMp();
    }

    public final int getMobExp() {
        if (ostats != null) {
            return ostats.exp;
        }
        return stats.getExp();
    }

    public final void setOverrideStats(final OverrideMonsterStats ostats) {
        this.ostats = new ChangeableStats(stats, ostats);
        this.hp = ostats.getHp();
        this.mp = ostats.getMp();
    }

    public final void changeLevel(final int newLevel) {
        changeLevel(newLevel, true);
    }

    public final void changeLevel(final int newLevel, boolean pqMob) {
        if (!stats.isChangeable()) {
            return;
        }
        this.ostats = new ChangeableStats(stats, newLevel, pqMob);
        this.hp = ostats.getHp();
        this.mp = ostats.getMp();
    }

    public final MapleMonster getSponge() {
        return sponge.get();
    }

    public final void damage(final MapleCharacter from, final long damage, final boolean updateAttackTime) {
        damage(from, damage, updateAttackTime, 0);
    }

    public final void damage(final MapleCharacter from, final long damage, final boolean updateAttackTime, final int lastSkill) {
        if (from == null || damage <= 0 || !isAlive()) {
            return;
        }
        
        AttackerEntry attacker = null;

        if (from.getParty() != null) {
            attacker = new PartyAttackerEntry(from.getParty().getId());
        } else {
            attacker = new SingleAttackerEntry(from);
        }
        boolean replaced = false;
        for (final AttackerEntry aentry : getAttackers()) {
            if (aentry != null && aentry.equals(attacker)) {
                attacker = aentry;
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            attackers.add(attacker);
        }
        final long rDamage = Math.max(0, Math.min(damage, hp));
        attacker.addDamage(from, rDamage, updateAttackTime);

        if (stats.getSelfD() != -1) {
            hp -= rDamage;
            if (hp > 0) {
                if (hp < stats.getSelfDHp()) { // HP is below the selfd level
                    map.killMonster(this, from, false, false, stats.getSelfD(), lastSkill);
                } else { // Show HP
                    for (final AttackerEntry mattacker : getAttackers()) {
                        for (final AttackingMapleCharacter cattacker : mattacker.getAttackers()) {
                            if (cattacker.getAttacker().getMap() == from.getMap()) { // current attacker is on the map of the monster
                                if (cattacker.getLastAttackTime() >= System.currentTimeMillis() - 4000) {
                                    cattacker.getAttacker().getClient().getSession().write(MobPacket.showMonsterHP(getObjectId(), getHPPercent()));
                                }
                            }
                        }
                    }
                }
            } else { // Character killed it without explosing :(
                map.killMonster(this, from, true, false, (byte) 1, lastSkill);
            }
        } else {
            if (sponge.get() != null) {
                if (sponge.get().hp > 0) { // If it's still alive, dont want double/triple rewards
                    // Sponge are always in the same map, so we can use this.map
                    // The only mob that uses sponge are PB/HT
                    sponge.get().hp -= rDamage;

                    if (sponge.get().hp <= 0) {
                        map.broadcastMessage(MobPacket.showBossHP(sponge.get().getId(), -1, sponge.get().getMobMaxHp(), sponge.get().getStats().getTagColor(), sponge.get().getStats().getTagBgColor()));
                        map.killMonster(sponge.get(), from, true, false, (byte) 1, lastSkill);
                    } else {
                        map.broadcastMessage(MobPacket.showBossHP(sponge.get()));
                    }
                }
            }
            if (hp > 0) {
                hp -= rDamage;
                if (eventInstance != null) {
                    eventInstance.monsterDamaged(from, this, (int) rDamage);
                } else {
                    final EventInstanceManager em = from.getEventInstance();
                    if (em != null) {
                        em.monsterDamaged(from, this, (int) rDamage);
                    }
                }
                if (sponge.get() == null && hp >= 0) {
                    switch (stats.getHPDisplayType()) {
                        case 0:
                            map.broadcastMessage(MobPacket.showBossHP(this), this.getTruePosition());
                            break;
                        case 1:
                            map.broadcastMessage(from, MobPacket.damageFriendlyMob(this, damage, true), true);
                            break;
                        case 2:
                            map.broadcastMessage(MobPacket.showMonsterHP(getObjectId(), getHPPercent()));
                            from.mulung_EnergyModify(true);
                            break;
                        case 3:
                            for (final AttackerEntry mattacker : getAttackers()) {
                                if (mattacker != null) {
                                    for (final AttackingMapleCharacter cattacker : mattacker.getAttackers()) {
                                        if (cattacker != null && cattacker.getAttacker().getMap() == from.getMap()) { // current attacker is on the map of the monster
                                            if (cattacker.getLastAttackTime() >= System.currentTimeMillis() - 4000) {
                                                cattacker.getAttacker().getClient().getSession().write(MobPacket.showMonsterHP(getObjectId(), getHPPercent()));
                                            }
                                        }
                                    }
                                }
                            }
                            break;
                    }
                }
                if (getId() == 100100 && getHp() > 99000 && getHp() < 10000000 && !broadcastedEventGM) {
                    broadcastedEventGM = true;
                    String members = "";
                    int membersC = 0;
                    for (MapleCharacter obj : map.getCharacters()) {
                        membersC++;
                        members += obj.getName() + ", ";
                    }
                    FileoutputUtil.log("log_event_snail.txt", "Found monster"
                            + " at " + StringUtil.getCurrentTime()
                            + " by " + from.getName()
                            + " in " + map.getStreetName() + " : " + map.getMapName()
                            + "\r\n\r\n");
                    World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("현재 달팽이의 위치는 [" + map.getStreetName() + " : " + map.getMapName() + "] 이며, 현재 [" + members + "] " + membersC + "명이 달팽이를 공격중입니다."));
                }
                if (getId() == 100100 && getHp() > 49000 && getHp() < 5000000 && !broadcastedEvent) {
                    broadcastedEvent = true;
                    String members = "";
                    int membersC = 0;
                    for (MapleCharacter obj : map.getCharacters()) {
                        membersC++;
                        members += obj.getName() + ", ";
                    }
                    World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("현재 달팽이의 위치는 [" + map.getStreetName() + " : " + map.getMapName() + "] 이며, 현재 [" + members + "] " + membersC + "명이 달팽이를 공격중입니다."));
                }

                if (hp <= 0) {
                    if (stats.getHPDisplayType() == 0) {
                        map.broadcastMessage(MobPacket.showBossHP(getId(), -1, getMobMaxHp(), getStats().getTagColor(), getStats().getTagBgColor()), this.getTruePosition());
                    }
                    map.killMonster(this, from, true, false, (byte) 1, lastSkill);
                }
            }
        }
        startDropItemSchedule();

    }

    public int getHPPercent() {
        return (int) Math.ceil((hp * 100.0) / getMobMaxHp());
    }

    public final void heal(int hp, int mp, final boolean broadcast) {
        final long TotalHP = getHp() + hp;
        final int TotalMP = getMp() + mp;

        if (TotalHP >= getMobMaxHp()) {
            setHp(getMobMaxHp());
        } else {
            setHp(TotalHP);
        }
        if (TotalMP >= getMp()) {
            setMp(getMp());
        } else {
            setMp(TotalMP);
        }
        if (broadcast) {
            map.broadcastMessage(MobPacket.healMonster(getObjectId(), hp));
        } else if (sponge.get() != null) { // else if, since only sponge doesn't broadcast
            sponge.get().hp += hp;
        }
    }

    public final void killed() {
        if (listener != null) {
            listener.monsterKilled();
        }
        listener = null;
    }

    public void setNextAttackPossible(boolean bln) {
        nextAttackPossible = bln;
    }

    public boolean getNextAttackPossible() {
        return nextAttackPossible;
    }

    private final void giveExpToCharacter(final MapleCharacter attacker, long exp, final boolean highestDamage, final byte pty/*, final int lastskillID*/) {
        /*if (attacker.getMapId() / 10000000 == 19) {
         return;
         }*/ //피시방 경험치
        long originalexp = exp;
        long bonusexp = exp;
        long bonusexp2 = exp;
        long bonusexp3 = exp;
        if (eventDrop > 0) {
            return;
        }
        if (highestDamage) {
            if (eventInstance != null) {
                eventInstance.monsterKilled(attacker, this);
            } else {
                final EventInstanceManager em = attacker.getEventInstance();
                if (em != null) {
                    em.monsterKilled(attacker, this);
                }
            }
            highestDamageChar = attacker.getId();
        }
        if (exp > 0) {
            final MonsterStatusEffect ms = stati.get(MonsterStatus.SHOWDOWN);
            if (ms != null) {
                exp = (long) Math.max(0, Math.min(exp + (exp * (ms.getX() / 100.0)), Long.MAX_VALUE));
            }
            final Integer holySymbol = attacker.getBuffedValue(MapleBuffStat.HOLY_SYMBOL);
            if (holySymbol != null) {
                boolean canFullHSEffect = false;
                if (attacker.getParty() != null) {
                    int ff = 0;
                    for (MaplePartyCharacter pchr : attacker.getParty().getMembers()) {
                        if (pchr.isOnline() && pchr.getChannel() == attacker.getClient().getChannel() && pchr.getMapid() == attacker.getMapId()) {
                            ff++;
                        }
                    }
                    if (ff >= 2) {
                        canFullHSEffect = true;
                    }
                }
                if (attacker.getBuffSource(MapleBuffStat.HOLY_SYMBOL) / 1000000 == 5 || attacker.getBuffSource(MapleBuffStat.HOLY_SYMBOL) == 2022033) { //GM
                    canFullHSEffect = true;
                }
                if (canFullHSEffect) { //파티원수 인원 체크여부
                    bonusexp = (long) Math.max(0, Math.min(bonusexp * (holySymbol.doubleValue() * 5 / 100.0), Long.MAX_VALUE));
                    exp = Math.max(0, Math.min(exp + bonusexp, Long.MAX_VALUE));

                } else {
                    bonusexp = (long) Math.max(0, Math.min(bonusexp * (holySymbol.doubleValue() * 5 / 150.0), Long.MAX_VALUE));
                    exp = Math.max(0, Math.min(exp + bonusexp, Long.MAX_VALUE));

                }

            }
            if (attacker.hasDisease(MapleDisease.CURSE)) {
                exp = Math.max(0, Math.min(exp / 2, Long.MAX_VALUE));
            }
            if (attacker.getStat().expBuff / 100.0 != (double) 100.0) {
                bonusexp2 = (long) Math.max(0, Math.min(bonusexp2 * (attacker.getStat().expBuff / 100.0) - 1, Long.MAX_VALUE));
                exp = Math.max(0, Math.min(exp + bonusexp2, Long.MAX_VALUE));/*2배 쿠폰같은것 합공식처리*/
            }
            if (attacker.getInventory(MapleInventoryType.EQUIPPED).findById(1137000) != null) { // 몽환의 벨트
                exp = (long) Math.max(0, Math.min(exp + ((exp / 100.0) * 10), Long.MAX_VALUE));
            }
            if (attacker.getInventory(MapleInventoryType.EQUIPPED).findById(1122207) != null) { // 스정펜
                exp = (long) Math.max(0, Math.min(exp + ((exp / 100.0) * 10), Long.MAX_VALUE));
            }
            if (attacker.getPlayerShop() != null) { // 고상
                exp = (long) Math.max(0, Math.min(exp + ((exp / 100.0) * 10), Long.MAX_VALUE));
            }
            //뿌리기버프
            if (attacker.getBuffedValue1(9001008)) {
                exp = (long) Math.max(0, Math.min(exp + ((exp / 100.0) * 50), Long.MAX_VALUE));
            }
            if (attacker.getBuffedValue1(2022091)) {// 길드의 축복 50%
                exp = (long) Math.max(0, Math.min(exp + ((exp / 100.0) * 30), Long.MAX_VALUE));//50은 경치 50%라는뜻
            }
            if (attacker.getGuild() != null) { // 길드..
                if (attacker.getGuild().getLevel2() >= 2) {
                    exp = (long) Math.max(0, Math.min(exp + ((exp / 100.0) * (attacker.getGuild().getLevel2() * 5)), Long.MAX_VALUE));
                }
            }//잠시마뇽고공
            //bonusexp3 은 어차피 0이 들어옴
            bonusexp3 *= ((int) Math.min(Integer.MAX_VALUE, (GameConstants.getExpRate(attacker.getJob(), ChannelServer.getInstance(map.getChannel()).getExpRate()))) - 1);
            //System.err.println(exp + " / " + bonusexp3);
            if ((attacker.getLevel() >= 1 && attacker.getLevel() <= 9) && ServerConstants.TUTORIAL_EXP_SYSTEM) {//1~10레벨 튜토리얼 경험치배율
                exp = Math.max(0, Math.min(exp * ServerConstants.TUTORIAL_EXP_SYSTEM_RATE + bonusexp, Long.MAX_VALUE));
            } else if ((attacker.getJob() >= 1100 && attacker.getJob() < 1600)) {// 시그너스 직업
                exp = Math.max(0, Math.min((exp * 20) + bonusexp3, Long.MAX_VALUE));//시그너스 직업[20 = 40배]
            } else if ((attacker.getLevel() >= 10 && attacker.getLevel() < 120) && ! (attacker.getJob() >= 1100 && attacker.getJob() < 1600)) {// 10 ~120 구간 
                exp = Math.max(0, Math.min((exp * 20) + bonusexp3, Long.MAX_VALUE)); //배율 [20 = 40배]
            } else if ((attacker.getLevel() >= 120 && attacker.getLevel() < 999) && ! (attacker.getJob() >= 1100 && attacker.getJob() < 1600)) {// 120 ~999 구간 
                exp = Math.max(0, Math.min((exp * 15) + bonusexp3, Long.MAX_VALUE)); //배율 [15 = 30배]
            } else {
                exp = Math.max(0, Math.min(exp + bonusexp3, Long.MAX_VALUE));
            }
            List<Integer> realExp = new ArrayList<>();
            int idx = (int) ((double) exp / Integer.MAX_VALUE != 1 ? Math.ceil((double) exp / Integer.MAX_VALUE) : 1);
            if (exp > Integer.MAX_VALUE) {
                for (int i = 0; i < idx; i++) {
                    if (i == idx - 1) {
                        int remainEXP = (int) Math.max(0, Math.min(exp - (i * Integer.MAX_VALUE), Integer.MAX_VALUE));
                        realExp.add(i, remainEXP);
                    } else {
                        realExp.add(i, Integer.MAX_VALUE);
                    }
                }
            } else {
                int giveEXP = (int) Math.max(0, Math.min(exp, Integer.MAX_VALUE));
                realExp.add(0, giveEXP);
            }
            
            for (int giveEXP : realExp) {
            //attacker.dropMessage(6, "bonusexp3: " + bonusexp3 + " || bonusexp: " + bonusexp + " || bonusexp2: " + bonusexp2 + " || expBuff" + attacker.getStat().expBuff / 100.0 + " || exp: " + exp + " || getExpRate: " + GameConstants.getExpRate(attacker.getJob(), ChannelServer.getInstance(map.getChannel()).getExpRate()));
                attacker.gainExpMonster(giveEXP, giveEXP, true, highestDamage, pty, stats.isPartyBonus(), stats.getPartyBonusRate());
            }
        }

        if ((getStats().getLevel() >= attacker.getLevel()) || (getStats().getLevel() >= 120 && attacker.getLevel() >= 120)) {
            if (getStats().getLevel() >= attacker.getLevel()) {
                // do 노련한 사냥꾼
                if (attacker.getQuestStatus(29400) == 1) {
                    int mob = Integer.parseInt(attacker.getOneInfo(29400, "mon"));
                    attacker.updateOneInfo(29400, "mon", (mob + 1) + "");
                }
            }
        }
        attacker.mobKilled(getId(), attacker.LastSkill);
    }

    public final int killBy(final MapleCharacter killer, final int lastSkill) {
        if (killed) {
            return 1;
        }
        killed = true;
        int totalBaseExp = getMobExp();
        AttackerEntry highest = null;
        long highdamage = 0;
        final List<AttackerEntry> list = getAttackers();
        for (final AttackerEntry attackEntry : list) {
            if (attackEntry != null && attackEntry.getDamage() > highdamage) {
                highest = attackEntry;
                highdamage = attackEntry.getDamage();
            }
        }
        long baseExp;
        for (final AttackerEntry attackEntry : list) {
            if (attackEntry != null) {
                baseExp = (long) Math.max(0, Math.min(Math.ceil(totalBaseExp * ((double) attackEntry.getDamage() / getMobMaxHp())), Long.MAX_VALUE));
                attackEntry.killedMob(getMap(), baseExp, attackEntry == highest, lastSkill);
            }
        }
        final MapleCharacter controll = controller.get();
        if (controll != null) { // this can/should only happen when a hidden gm attacks the monster
            controll.getClient().getSession().write(MobPacket.stopControllingMonster(getObjectId()));
            controll.stopControllingMonster(this);
        }
        int achievement = 0;

        switch (getId()) {
            case 9400121:
                achievement = 0;
                break;
            case 9600025://무림요승
                achievement = 0;
                break;
            case 8800002:
            case 8810018:
                achievement = 0;
                break;
            case 9420544:
                achievement = 0;
                break;
            case 9400409://대두꺼비 가엘
                achievement = 0;
                break;
            case 9420513:
            case 9400405:
                achievement = 0;
                break;
            case 9420522://크렉셀
                achievement = 0;
                break;
            case 9400300://대두목
                achievement = 0;
                break;
            default:
                break;
        }

        if (achievement != 0) {
            if (killer != null && killer.getParty() != null) {
                for (MaplePartyCharacter mp : killer.getParty().getMembers()) {
                    final MapleCharacter mpc = killer.getMap().getCharacterById(mp.getId());
                    if (mpc != null) {
                        //mpc.finishAchievement(achievement); 보스포인트
                    }
                }
            } else if (killer != null) {
                //killer.finishAchievement(achievement);
            }
        }
        if (killer != null && stats.isBoss()) {
            //killer.finishAchievement(18);
        }
        spawnRevives(getMap());
        if (eventInstance != null) {
            eventInstance.unregisterMonster(this);
            eventInstance = null;
        }
        if (killer != null && killer.getPyramidSubway() != null) {
            killer.getPyramidSubway().onKill(killer);
        }
        hp = 0;
        MapleMonster oldSponge = getSponge();
        sponge = new WeakReference<MapleMonster>(null);
        if (oldSponge != null && oldSponge.isAlive()) {
            boolean set = true;
            for (MapleMapObject mon : map.getAllMonstersThreadsafe()) {
                MapleMonster mons = (MapleMonster) mon;
                if (mons.isAlive() && mons.getObjectId() != oldSponge.getObjectId() && mons.getStats().getLevel() > 1 && mons.getObjectId() != this.getObjectId() && (mons.getSponge() == oldSponge || mons.getLinkOid() == oldSponge.getObjectId())) { //sponge was this, please update
                    set = false;
                    break;
                }
            }
            if (set) { //all sponge monsters are dead, please kill off the sponge
                map.killMonster(oldSponge, killer, true, false, (byte) 1);
            }
        }

        reflectpack = null;
        nodepack = null;
        if (stati.size() > 0) {
            List<MonsterStatus> statuses = new LinkedList<MonsterStatus>(stati.keySet());
            for (MonsterStatus ms : statuses) {
                cancelStatus(ms);
            }
            statuses.clear();
        }
        if (poisons.size() > 0) {
            List<MonsterStatusEffect> ps = new LinkedList<MonsterStatusEffect>();
            poisonsLock.readLock().lock();
            try {
                ps.addAll(poisons);
            } finally {
                poisonsLock.readLock().unlock();
            }
            for (MonsterStatusEffect p : ps) {
                cancelSingleStatus(p);
            }
            ps.clear();
        }
        //attackers.clear();
        cancelDropItem();
        int v1 = highestDamageChar;
        this.highestDamageChar = 0; //reset so we dont kill twice
        return v1;
    }

    public final void spawnRevives(final MapleMap map) {
        final List<Integer> toSpawn = stats.getRevives();

        if (toSpawn == null || this.getLinkCID() > 0) {
            return;
        }
        MapleMonster spongy = null;
        switch (getId()) {
            case 8820002:
            case 8820003:
            case 8820004:
            case 8820005:
            case 8820006:
            case 8840000:
            case 6160003:
            case 8850011:
                break;
            case 8810118:
            case 8810119:
            case 8810120:
            case 8810121: //must update sponges
                for (final int i : toSpawn) {
                    final MapleMonster mob = MapleLifeFactory.getMonster(i);

                    mob.setPosition(getTruePosition());
                    if (eventInstance != null) {
                        eventInstance.registerMonster(mob);
                    }
                    if (dropsDisabled()) {
                        mob.disableDrops();
                    }
                    switch (mob.getId()) {
                        case 8810119:
                        case 8810120:
                        case 8810121:
                        case 8810122:
                            spongy = mob;
                            break;
                    }
                }
                if (spongy != null && map.getMonsterById(spongy.getId()) == null) {
                    map.spawnMonster(spongy, -2);
                    for (MapleMapObject mon : map.getAllMonstersThreadsafe()) {
                        MapleMonster mons = (MapleMonster) mon;
                        if (mons.getObjectId() != spongy.getObjectId() && (mons.getSponge() == this || mons.getLinkOid() == this.getObjectId())) { //sponge was this, please update
                            mons.setSponge(spongy);
                        }
                    }
                }
                break;
            case 8810026:
            case 8810130:
            case 8820008:
            case 8820009:
            case 8820010:
            case 8820011:
            case 8820012:
            case 8820013: {
                final List<MapleMonster> mobs = new ArrayList<MapleMonster>();

                for (final int i : toSpawn) {
                    final MapleMonster mob = MapleLifeFactory.getMonster(i);

                    mob.setPosition(getTruePosition());
                    if (eventInstance != null) {
                        eventInstance.registerMonster(mob);
                    }
                    if (dropsDisabled()) {
                        mob.disableDrops();
                    }
                    switch (mob.getId()) {
                        case 8810018: // Horntail Sponge
                        case 8810122: // chaos Horntail Sponge    
                        case 8810118:
                        case 8820009: // PinkBeanSponge0
                        case 8820010: // PinkBeanSponge1
                        case 8820011: // PinkBeanSponge2
                        case 8820012: // PinkBeanSponge3
                        case 8820013: // PinkBeanSponge4
                        case 8820014: // PinkBeanSponge5
                            spongy = mob;
                            break;
                        default:
                            mobs.add(mob);
                            break;
                    }
                }
                if (spongy != null && map.getMonsterById(spongy.getId()) == null) {
                    map.spawnMonster(spongy, -2);

                    for (final MapleMonster i : mobs) {
                        map.spawnMonster(i, -2);
                        i.setSponge(spongy);
                    }
                }
                break;
            }
            case 8820014: {
                for (final int i : toSpawn) {
                    final MapleMonster mob = MapleLifeFactory.getMonster(i);

                    if (eventInstance != null) {
                        eventInstance.registerMonster(mob);
                    }
                    mob.setPosition(getTruePosition());
                    if (dropsDisabled()) {
                        mob.disableDrops();
                    }
                    map.spawnMonster(mob, -2);
                }
                break;
            }
            default: {
                for (final int i : toSpawn) {
                    final MapleMonster mob = MapleLifeFactory.getMonster(i);

                    if (eventInstance != null) {
                        eventInstance.registerMonster(mob);
                    }
                    mob.setPosition(getTruePosition());
                    if (dropsDisabled()) {
                        mob.disableDrops();
                    }
                    map.spawnRevives(mob, this.getObjectId());

                    if (mob.getId() == 9300216) {
                        map.broadcastMessage(MaplePacketCreator.environmentChange("Dojang/clear", 4));
                        map.broadcastMessage(MaplePacketCreator.environmentChange("dojang/end/clear", 3));
                    }
                }
                break;
            }
        }
    }

    public final boolean isAlive() {
        return hp > 0;
    }

    public final void setCarnivalTeam(final byte team) {
        carnivalTeam = team;
    }

    public final byte getCarnivalTeam() {
        return carnivalTeam;
    }

    public final MapleCharacter getController() {
        return controller.get();
    }

    public final void setController(final MapleCharacter controller) {
        this.controller = new WeakReference<MapleCharacter>(controller);
    }

    public final void switchController(final MapleCharacter newController, boolean immediateAggro) {
        final MapleCharacter controllers = getController();
        if (controllers == newController) {
            return;
        } else if (controllers != null) {
            controllers.stopControllingMonster(this);
            controllers.getClient().getSession().write(MobPacket.stopControllingMonster(getObjectId()));
            sendStatus(controllers.getClient());
        }
        if (getCarnivalTeam() != -1) {
            if (newController.getCarnivalParty() != null) {
                if (getCarnivalTeam() == newController.getCarnivalParty().getTeam()) {
                    immediateAggro = false;
                }
            }
        }
        newController.controlMonster(this, immediateAggro);
        setController(newController);
        if (immediateAggro) {
            setControllerHasAggro(true);
        }
    }

    public final void addListener(final MonsterListener listener) {
        this.listener = listener;
    }

    public final boolean isControllerHasAggro() {
        return controllerHasAggro;
    }

    public final void setControllerHasAggro(final boolean controllerHasAggro) {
        this.controllerHasAggro = controllerHasAggro;
    }

    public final void sendStatus(final MapleClient client) {
        if (reflectpack != null) {
            client.getSession().write(reflectpack);
        }
        if (poisons.size() > 0) {
            poisonsLock.readLock().lock();
            try {
                client.getSession().write(MobPacket.applyMonsterStatus3(this, poisons));
            } finally {
                poisonsLock.readLock().unlock();
            }
        }
    }

    @Override
    public final void sendSpawnData(final MapleClient client) {
        if (!isAlive()) {
            return;
        }
        client.getSession().write(MobPacket.spawnMonster(this, fake && linkCID <= 0 ? -4 : -1, 0));
        sendStatus(client);
        if (map != null && !stats.isEscort() && client.getPlayer() != null && client.getPlayer().getTruePosition().distanceSq(getTruePosition()) <= GameConstants.maxViewRangeSq_Half()) {
            map.updateMonsterController(this);
        }
    }

    @Override
    public final void sendDestroyData(final MapleClient client) {
        if (stats.isEscort() && getEventInstance() != null && lastNode >= 0) { //shammos
            map.resetShammos(client);
        } else {
            client.getSession().write(MobPacket.killMonster(getObjectId(), 0));
            if (getController() != null && client.getPlayer() != null && client.getPlayer().getId() == getController().getId()) {
                client.getPlayer().stopControllingMonster(this);
            }
        }
    }

    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append(stats.getName());
        sb.append("(");
        sb.append(getId());
        sb.append(") (Level ");
        sb.append(stats.getLevel());
        sb.append(") at (X");
        sb.append(getTruePosition().x);
        sb.append("/ Y");
        sb.append(getTruePosition().y);
        sb.append(") with ");
        sb.append(getHp());
        sb.append("/ ");
        sb.append(getMobMaxHp());
        sb.append("hp, ");
        sb.append(getMp());
        sb.append("/ ");
        sb.append(getMobMaxMp());
        sb.append(" mp, oid: ");
        sb.append(getObjectId());
        sb.append(" || Controller : ");
        final MapleCharacter chr = controller.get();
        sb.append(chr != null ? chr.getName() : "none");

        return sb.toString();
    }

    @Override
    public final MapleMapObjectType getType() {
        return MapleMapObjectType.MONSTER;
    }

    public final EventInstanceManager getEventInstance() {
        return eventInstance;
    }

    public final void setEventInstance(final EventInstanceManager eventInstance) {
        this.eventInstance = eventInstance;
    }

    public final int getStatusSourceID(final MonsterStatus status) {
        if (status == MonsterStatus.POISON || status == MonsterStatus.BURN) {
            poisonsLock.readLock().lock();
            try {
                for (MonsterStatusEffect ps : poisons) {
                    if (ps != null) {
                        return ps.getSkill();
                    }
                }
                return -1;
            } finally {
                poisonsLock.readLock().unlock();
            }
        }
        final MonsterStatusEffect effect = stati.get(status);
        if (effect != null) {
            return effect.getSkill();
        }
        return -1;
    }

    public final ElementalEffectiveness getEffectiveness(final Element e) {
        if (stati.size() > 0 && stati.containsKey(MonsterStatus.DOOM)) {
            return ElementalEffectiveness.NORMAL; // like blue snails
        }
        return stats.getEffectiveness(e);
    }

    public final void applyStatus(final MapleCharacter from, final MonsterStatusEffect status, final boolean poison, long duration, final boolean checkboss, final MapleStatEffect eff) {
        if (!isAlive() || getLinkCID() > 0 || (getStats().isBoss() && status != null && status.getStati() != null && (status.getStati() == MonsterStatus.POISON || status.getStati() == MonsterStatus.NINJA_AMBUSH || status.getStati() == MonsterStatus.BURN))) {
            return;
        }
        Skill skilz = SkillFactory.getSkill(status.getSkill());
        if (skilz != null) {
            switch (stats.getEffectiveness(skilz.getElement())) {
                case IMMUNE:
                case STRONG:
                    return;
                case NORMAL:
                case WEAK:
                    break;
                default:
                    return;
            }
        }
        // compos don't have an elemental (they have 2 - so we have to hack here...)
        final int statusSkill = status.getSkill();
        switch (statusSkill) {
            case 2111006: { // FP compo
                switch (stats.getEffectiveness(Element.POISON)) {
                    case IMMUNE:
                    case STRONG:
                        return;
                }
                break;
            }
            case 2211006: { // IL compo
                switch (stats.getEffectiveness(Element.ICE)) {
                    case IMMUNE:
                    case STRONG:
                        return;
                }
                break;
            }
            case 4120005:
            case 4220005:
            case 14110004: {
                switch (stats.getEffectiveness(Element.POISON)) {
                    case IMMUNE:
                    case STRONG:
                        return;
                }
                break;
            }
            case 5211004: {
                switch (stats.getEffectiveness(Element.FIRE)) {
                    case IMMUNE:
                    case STRONG:
                        return;
                }
                break;
            }
        }
        if (duration >= 2000000000) {
            duration = 5000; //teleport master
        }
        if (eff.getSourceId() == 5211005) {
            if (from.getTotalSkillLevel(5220001) > 0) {
                MapleStatEffect eff1 = SkillFactory.getSkill(5220001).getEffect(from.getTotalSkillLevel(5220001));
                duration += (eff1.getY() * 1000);
            }
        }
        final MonsterStatus stat = status.getStati();
        if (stats.isNoDoom() && stat == MonsterStatus.DOOM) {
            return;
        }

        if (stats.isBoss()) {
            if (stat == MonsterStatus.STUN || stat == MonsterStatus.SEAL || stat == MonsterStatus.FREEZE) {
                return;
            }
            if (checkboss && stat != (MonsterStatus.SPEED) && stat != (MonsterStatus.NINJA_AMBUSH) && stat != (MonsterStatus.WATK) && stat != (MonsterStatus.POISON) && stat != MonsterStatus.BURN && stat != (MonsterStatus.DARKNESS)) {
                return;
            }
            //hack: don't magic crash cygnus boss
        }
        if (stats.isFriendly() || isFake()) {
            if (stat == MonsterStatus.STUN || stat == MonsterStatus.SPEED || stat == MonsterStatus.POISON || stat == MonsterStatus.BURN) {
                return;
            }
        }
        if ((stat == MonsterStatus.BURN || stat == MonsterStatus.POISON) && eff == null) {
            return;
        }
        if (stati.containsKey(stat)
                && (stat == MonsterStatus.STUN || stat == MonsterStatus.SEAL || stat == MonsterStatus.FREEZE)) {
            MonsterStatusEffect mffect = stati.get(stat);
//            if (mffect.getFromID() > 0 && mffect.getFromID() == from.getId()) {
            mffect.setCancelTask(duration);
            return;
//            }
        }
        if (stati.containsKey(stat) && stat != MonsterStatus.BURN) {
            cancelStatus(stat);
        }
        MonsterStatusEffect venomeff = null;
        if (stat == MonsterStatus.POISON || stat == MonsterStatus.BURN) {
            poisonsLock.readLock().lock();
            try {
                for (MonsterStatusEffect mse : poisons) {
                    if ((mse != null && mse.getSkill() == eff.getSourceId() && stat != MonsterStatus.BURN) || (mse != null && stat == MonsterStatus.BURN && mse.getStati() == MonsterStatus.BURN && mse.getVenomCount() >= 3)) {
                        return;
                    }
                    if (mse != null && mse.getStati() == MonsterStatus.BURN && stat == MonsterStatus.BURN) {
                        venomeff = mse;
                        break;
                    }
                }
            } finally {
                poisonsLock.readLock().unlock();
            }
        }
        if (stat != MonsterStatus.BURN) {
            if (poison && getHp() > 1 && eff != null) {
                duration = Math.max(duration, eff.getDOTTime() * 1000);
            }
            duration += from.getStat().dotTime * 1000;
        }
        long aniTime = duration;
        if (skilz != null) {
            aniTime += skilz.getAnimationTime();
        }
        status.setCancelTask(aniTime);
//        System.out.println("Cancel Time : " + aniTime);
        if (poison && getHp() > 1) {
            float weak = 1.0F;
            switch (stats.getEffectiveness(skilz.getElement())) {
                case STRONG:
                case IMMUNE:
                    weak = 0.5F;
                    break;
                case WEAK:
                    weak = 1.5F;
                    break;
            }
            //y / 100
            float nAmp = 1.0F;
            if (from.getJob() / 10 == 21) {
                if (from.getSkillLevel(2110001) > 0) {
                    MapleStatEffect effz = SkillFactory.getSkill(2110001).getEffect(from.getSkillLevel(2110001));
                    nAmp = effz.getY() / 100.0F;
                }
            }
            if (from.getJob() / 10 == 22) {
                if (from.getSkillLevel(2210001) > 0) {
                    MapleStatEffect effz = SkillFactory.getSkill(2210001).getEffect(from.getSkillLevel(2210001));
                    nAmp = effz.getY() / 100.0F;
                }
            }
            int pDam = 1;
            if (stat == MonsterStatus.POISON) {
                pDam = (int) Math.min(Math.max(1.0D, (this.getStats().getHp() / (70 - from.getSkillLevel(eff.getSourceId())) + 0.999D) * weak * nAmp), 30000.0D);
                if ((from.getJob() == 521 || from.getJob() == 522) && eff.getSourceId() == 5211004) {
                    if (from.getTotalSkillLevel(5220001) > 0) {
                        MapleStatEffect eff1 = SkillFactory.getSkill(5220001).getEffect(from.getTotalSkillLevel(5220001));
                        pDam = (int) Math.min(30000, ((eff1.getX() / 100.0D) + 1.0D) * pDam);
                    }
                }
                //System.out.println("14번 : " + pDam);
            } else { //Venom
                int v55 = from.getStat().getTotalStr() + from.getStat().getTotalLuk();
                double v56 = v55 * 0.8;
                int v43 = (int) (((Randomizer.nextInt() & Integer.MAX_VALUE) % v55) + v56);
                int v44 = eff.getMatk() * (from.getStat().getTotalDex() + 5 * v43) / 49;
                pDam = (int) Math.min(Math.max(1.0D, v44), 30000.0D);
            }
            //status.setValue(status.getStati(), Integer.valueOf((int) ((eff.getDOT() + from.getStat().dot) * from.getStat().getCurrentMaxBaseDamage() / 100.0)));
            //int dam = Integer.valueOf((int) (aniTime / 1000 * status.getX() / 2));
//            System.out.println("AAAA");
//            if (venomeff != null) {
//                System.out.println(" Venom Count : " + venomeff.getVenomCount() + " / Damage : " + venomeff.getX());
//            }
            if (stat == MonsterStatus.POISON || (venomeff == null && stat == MonsterStatus.BURN)) {
                status.setValue(status.getStati(), pDam);
                int dam = Integer.valueOf((int) pDam);
                status.setPoisonSchedule(dam, from);
                if (dam > 0) {
                    if (dam >= hp) {
                        dam = (int) (hp - 1);
                    }
                    damage(from, dam, false);
                }
                if (stat == MonsterStatus.BURN) {
                    status.setVenomCount(status.getVenomCount() + 1);
                }
            } else if (stat == MonsterStatus.BURN && venomeff != null && venomeff.getVenomCount() > 0) {
                if (venomeff.getVenomCount() < 3) {
                    int fzzz = Math.min(Math.max(1, Integer.valueOf((int) pDam + venomeff.getPoisonSchedule())), 30000);
                    venomeff.setValue(venomeff.getStati(), fzzz);
                    int dam = Integer.valueOf(fzzz);
                    venomeff.setPoisonSchedule(dam, from);
                    if (dam > 0) {
                        if (dam >= hp) {
                            dam = (int) (hp - 1);
                        }
                        damage(from, dam, false);
                    }
                    venomeff.setVenomCount(venomeff.getVenomCount() + 1);
                }
                venomeff.setCancelTask(eff.getDuration());
            }
        } else if (statusSkill == 4111003) { // shadow web
            status.setValue(status.getStati(), Math.min(Math.max(1, (int) (getMobMaxHp() / 50.0 + 0.999)), 30000));
            status.setPoisonSchedule(Integer.valueOf(status.getX()), from);
        } else if (statusSkill == 4121004 || statusSkill == 4221004) {
            short pDam = (short) (int) Math.min(Math.max(1.0D, ((from.getSkillLevel(eff.getSourceId()) + 30) * eff.getDamage() * (from.getStat().getTotalStr() + from.getStat().getTotalLuk()) / 2000)), 30000.0D);
            status.setValue(status.getStati(), Math.min(Short.MAX_VALUE, Integer.valueOf((int) pDam)));
            int dam = Integer.valueOf((int) pDam);
            status.setPoisonSchedule(dam, from);
            if (dam > 0) {
                if (dam >= hp) {
                    dam = (int) (hp - 1);
                }
                damage(from, dam, false);
            }
        }
//        if (venomeff != null) {
//            System.out.println(" Venom Count : " + venomeff.getVenomCount() + " / Damage : " + venomeff.getX());
//        }
        final MapleCharacter con = getController();
        if (stat == MonsterStatus.POISON || (stat == MonsterStatus.BURN && venomeff == null)) {
            poisonsLock.writeLock().lock();
            try {
                poisons.add(status);
                if (con != null) {
                    map.broadcastMessage(con, MobPacket.applyMonsterStatus3(this, poisons), getTruePosition());
                    con.getClient().getSession().write(MobPacket.applyMonsterStatus3(this, poisons));
                } else {
                    map.broadcastMessage(MobPacket.applyMonsterStatus3(this, poisons), getTruePosition());
                }
            } finally {
                poisonsLock.writeLock().unlock();
            }
        } else if (stat == MonsterStatus.BURN && venomeff != null) {
            if (con != null) {
                map.broadcastMessage(con, MobPacket.applyMonsterStatus2(this, venomeff), getTruePosition());
                con.getClient().getSession().write(MobPacket.applyMonsterStatus2(this, venomeff));
            } else {
                map.broadcastMessage(MobPacket.applyMonsterStatus2(this, venomeff), getTruePosition());
            }
        } else {
            stati.put(stat, status);
            if (con != null) {
                map.broadcastMessage(con, MobPacket.applyMonsterStatus2(this, status), getTruePosition());
                con.getClient().getSession().write(MobPacket.applyMonsterStatus2(this, status));
            } else {
                map.broadcastMessage(MobPacket.applyMonsterStatus2(this, status), getTruePosition());
            }
        }
    }

    public void applyStatus(MonsterStatusEffect status) { //ONLY USED FOR POKEMONN, ONLY WAY POISON CAN FORCE ITSELF INTO STATI.
        if (stati.containsKey(status.getStati())) {
            cancelStatus(status.getStati());
        }
        stati.put(status.getStati(), status);
        map.broadcastMessage(MobPacket.applyMonsterStatus2(this, status), getTruePosition());
    }

    public final void dispelSkill(final MobSkill skillId) {
        List<MonsterStatus> toCancel = new ArrayList<MonsterStatus>();
        for (Entry<MonsterStatus, MonsterStatusEffect> effects : stati.entrySet()) {
            MonsterStatusEffect mse = effects.getValue();
            if (mse.getMobSkill() != null && mse.getMobSkill().getSkillId() == skillId.getSkillId()) { //not checking for level.
                toCancel.add(effects.getKey());
            }
        }
        for (MonsterStatus stat : toCancel) {
            cancelStatus(stat);
        }
    }

    public final void applyMonsterBuff(final Map<MonsterStatus, Integer> effect, final int skillId, final long duration, final MobSkill skill, final List<Integer> reflection, short tDelay) {
        for (Entry<MonsterStatus, Integer> z : effect.entrySet()) {
            if (stati.containsKey(z.getKey())) {
                cancelStatus(z.getKey());
            }
            final MonsterStatusEffect effectz = new MonsterStatusEffect(z.getKey(), z.getValue(), 0, skill, true, reflection.size() > 0);
            effectz.setCancelTask(duration);
            stati.put(z.getKey(), effectz);
        }
        final MapleCharacter con = getController();
        if (reflection.size() > 0) {
            reflections.addAll(reflection);
            this.reflectpack = MobPacket.applyMonsterStatus4(getObjectId(), effect, reflection, skill, tDelay);
            if (con != null) {
                map.broadcastMessage(con, reflectpack, getTruePosition());
                con.getClient().getSession().write(this.reflectpack);
            } else {
                map.broadcastMessage(reflectpack, getTruePosition());
            }
        } else {
            for (Entry<MonsterStatus, Integer> z : effect.entrySet()) {
                if (con != null) {
                    map.broadcastMessage(con, MobPacket.applyMonsterStatus1(getObjectId(), z.getKey(), z.getValue(), skill, tDelay), getTruePosition());
                    con.getClient().getSession().write(MobPacket.applyMonsterStatus1(getObjectId(), z.getKey(), z.getValue(), skill, tDelay));
                } else {
                    map.broadcastMessage(MobPacket.applyMonsterStatus1(getObjectId(), z.getKey(), z.getValue(), skill, tDelay), getTruePosition());
                }
            }
        }
    }

    public final void setTempEffectiveness(final Element e, final long milli) {
        stats.setEffectiveness(e, ElementalEffectiveness.WEAK);
        EtcTimer.getInstance().schedule(new Runnable() {

            public void run() {
                stats.removeEffectiveness(e);
            }
        }, milli);
    }

    public final boolean isBuffed(final MonsterStatus status) {
        if (status == MonsterStatus.POISON || status == MonsterStatus.BURN) {
            return poisons.size() > 0 || stati.containsKey(status);
        }
        return stati.containsKey(status);
    }

    public final MonsterStatusEffect getBuff(final MonsterStatus status) {
        return stati.get(status);
    }

    public final int getStatiSize() {
        return stati.size() + (poisons.size() > 0 ? 1 : 0);
    }

    public final ArrayList<MonsterStatusEffect> getAllBuffs() {
        ArrayList<MonsterStatusEffect> ret = new ArrayList<MonsterStatusEffect>();
        for (MonsterStatusEffect e : stati.values()) {
            ret.add(e);
        }
        poisonsLock.readLock().lock();
        try {
            for (MonsterStatusEffect e : poisons) {
                ret.add(e);
            }
        } finally {
            poisonsLock.readLock().unlock();
        }
        return ret;
    }

    public final void setFake(final boolean fake) {
        this.fake = fake;
    }

    public final boolean isFake() {
        return fake;
    }

    public final MapleMap getMap() {
        return map;
    }

    public final List<Pair<Integer, Integer>> getSkills() {
        return stats.getSkills();
    }

    public final boolean hasSkill(final int skillId, final int level) {
        return stats.hasSkill(skillId, level);
    }

    public final long getLastSkillUsed(final int skillId) {
        if (usedSkills.containsKey(skillId)) {
            return usedSkills.get(skillId);
        }
        return 0;
    }

    public final void setLastSkillUsed(final int skillId, final long now, final long cooltime) {
        switch (skillId) {
            case 140:
                usedSkills.put(skillId, now + (cooltime * 2));
                usedSkills.put(141, now);
                break;
            case 141:
                usedSkills.put(skillId, now + (cooltime * 2));
                usedSkills.put(140, now + cooltime);
                break;
            default:
                usedSkills.put(skillId, now + cooltime);
                break;
        }
    }

    public final byte getNoSkills() {
        return stats.getNoSkills();
    }

    public final boolean isFirstAttack() {
        return stats.isFirstAttack();
    }

    public final int getBuffToGive() {
        return stats.getBuffToGive();
    }

    private final class PoisonTask implements Runnable {

        private final int poisonDamage;
        private final MapleCharacter chr;
        private final MonsterStatusEffect status;
        private final Runnable cancelTask;
        private final boolean shadowWeb;
        private final MapleMap map;

        private PoisonTask(final int poisonDamage, final MapleCharacter chr, final MonsterStatusEffect status, final Runnable cancelTask, final boolean shadowWeb) {
            this.poisonDamage = poisonDamage;
            this.chr = chr;
            this.status = status;
            this.cancelTask = cancelTask;
            this.shadowWeb = shadowWeb;
            this.map = chr.getMap();
        }

        public void run() {
            long damage = poisonDamage;
            if (damage >= hp) {
                damage = hp - 1;
                if (!shadowWeb) {
                    cancelTask.run();
                    status.cancelTask();
                }
            }
            if (hp > 1 && damage > 0) {
                damage(chr, damage, false);
                if (shadowWeb) {
                    map.broadcastMessage(MobPacket.damageMonster(getObjectId(), damage), getPosition());
                }
            }
        }
    }

    private static class AttackingMapleCharacter {

        private MapleCharacter attacker;
        private long lastAttackTime;

        public AttackingMapleCharacter(final MapleCharacter attacker, final long lastAttackTime) {
            super();
            this.attacker = attacker;
            this.lastAttackTime = lastAttackTime;
        }

        public final long getLastAttackTime() {
            return lastAttackTime;
        }

        public final void setLastAttackTime(final long lastAttackTime) {
            this.lastAttackTime = lastAttackTime;
        }

        public final MapleCharacter getAttacker() {
            return attacker;
        }
    }

    private interface AttackerEntry {

        List<AttackingMapleCharacter> getAttackers();

        public void addDamage(MapleCharacter from, long damage, boolean updateAttackTime);

        public long getDamage();

        public boolean contains(MapleCharacter chr);

        public void killedMob(MapleMap map, long baseExp, boolean mostDamage, int lastSkill);
    }

    private final class SingleAttackerEntry implements AttackerEntry {

        private long damage = 0;
        private int chrid;
        private long lastAttackTime;

        public SingleAttackerEntry(final MapleCharacter from) {
            this.chrid = from.getId();
        }

        @Override
        public void addDamage(final MapleCharacter from, final long damage, final boolean updateAttackTime) {
            if (chrid == from.getId()) {
                this.damage += damage;
                if (updateAttackTime) {
                    lastAttackTime = System.currentTimeMillis();
                }
            }
        }

        @Override
        public final List<AttackingMapleCharacter> getAttackers() {
            final MapleCharacter chr = map.getCharacterById(chrid);
            if (chr != null) {
                return Collections.singletonList(new AttackingMapleCharacter(chr, lastAttackTime));
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public boolean contains(final MapleCharacter chr) {
            return chrid == chr.getId();
        }

        @Override
        public long getDamage() {
            return damage;
        }

        @Override
        public void killedMob(final MapleMap map, final long baseExp, final boolean mostDamage, final int lastSkill) {
            final MapleCharacter chr = map.getCharacterById(chrid);
            if (chr != null && chr.isAlive()) {
                giveExpToCharacter(chr, baseExp, mostDamage, (byte) 1);
            }
        }

        @Override
        public int hashCode() {
            return chrid;
        }

        @Override
        public final boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SingleAttackerEntry other = (SingleAttackerEntry) obj;
            return chrid == other.chrid;
        }
    }

    private static final class OnePartyAttacker {

        public MapleParty lastKnownParty;
        public long damage;
        public long lastAttackTime;

        public OnePartyAttacker(final MapleParty lastKnownParty, final long damage) {
            super();
            this.lastKnownParty = lastKnownParty;
            this.damage = damage;
            this.lastAttackTime = System.currentTimeMillis();
        }
    }

    private class PartyAttackerEntry implements AttackerEntry {

        private long totDamage = 0;
        private final Map<Integer, OnePartyAttacker> attackers = new HashMap<Integer, OnePartyAttacker>(6);
        private int partyid;

        public PartyAttackerEntry(final int partyid) {
            this.partyid = partyid;
        }

        public List<AttackingMapleCharacter> getAttackers() {
            final List<AttackingMapleCharacter> ret = new ArrayList<AttackingMapleCharacter>(attackers.size());
            for (final Entry<Integer, OnePartyAttacker> entry : attackers.entrySet()) {
                final MapleCharacter chr = map.getCharacterById(entry.getKey());
                if (chr != null) {
                    ret.add(new AttackingMapleCharacter(chr, entry.getValue().lastAttackTime));
                }
            }
            return ret;
        }

        private final Map<MapleCharacter, OnePartyAttacker> resolveAttackers() {
            final Map<MapleCharacter, OnePartyAttacker> ret = new HashMap<MapleCharacter, OnePartyAttacker>(attackers.size());
            for (final Entry<Integer, OnePartyAttacker> aentry : attackers.entrySet()) {
                final MapleCharacter chr = map.getCharacterById(aentry.getKey());
                if (chr != null) {
                    ret.put(chr, aentry.getValue());
                }
            }
            return ret;
        }

        @Override
        public final boolean contains(final MapleCharacter chr) {
            return attackers.containsKey(chr.getId());
        }

        @Override
        public final long getDamage() {
            return totDamage;
        }

        public void addDamage(final MapleCharacter from, final long damage, final boolean updateAttackTime) {
            final OnePartyAttacker oldPartyAttacker = attackers.get(from.getId());
            if (oldPartyAttacker != null) {
                oldPartyAttacker.damage += damage;
                oldPartyAttacker.lastKnownParty = from.getParty();
                if (updateAttackTime) {
                    oldPartyAttacker.lastAttackTime = System.currentTimeMillis();
                }
            } else {
                // TODO actually this causes wrong behaviour when the party changes between attacks
                // only the last setup will get exp - but otherwise we'd have to store the full party
                // constellation for every attack/everytime it changes, might be wanted/needed in the
                // future but not now
                final OnePartyAttacker onePartyAttacker = new OnePartyAttacker(from.getParty(), damage);
                attackers.put(from.getId(), onePartyAttacker);
                if (!updateAttackTime) {
                    onePartyAttacker.lastAttackTime = 0;
                }
            }
            totDamage += damage;
        }

        @Override
        public final void killedMob(final MapleMap map, final long baseExp, final boolean mostDamage, final int lastSkill) {
            Map<MapleCharacter, OnePartyAttacker> aaaattackers = resolveAttackers();

            MapleCharacter highest = null;
            long highestDamage = 0;

            Map<MapleCharacter, Long> expMap = new ArrayMap<>(6);
            for (Entry<MapleCharacter, OnePartyAttacker> attacker : aaaattackers.entrySet()) {
                MapleParty party = attacker.getValue().lastKnownParty;
                double averagePartyLevel = 0;

                List<MapleCharacter> expApplicable = new ArrayList<MapleCharacter>();
                for (MaplePartyCharacter partychar : party.getMembers()) {
                    if (attacker.getKey().getLevel() - partychar.getLevel() <= 5
                            || stats.getLevel() - partychar.getLevel() <= 5) {
                        MapleCharacter pchr = ChannelServer.getInstance(map.getChannel()).getPlayerStorage().getCharacterByName(partychar.getName());
                        if (pchr != null) {
                            if (pchr.isAlive() && pchr.getMap() == map) {
                                expApplicable.add(pchr);
                                averagePartyLevel += pchr.getLevel();
                            }
                        }
                    }
                }
                double expBonus = 1.0;
                if (expApplicable.size() > 1) {
                    expBonus = 1.10 + 0.05 * expApplicable.size();
                    averagePartyLevel /= expApplicable.size();
                }

                int iDamage = (int) Math.min(attacker.getValue().damage, Integer.MAX_VALUE);
                if (iDamage > highestDamage) {
                    highest = attacker.getKey();
                    highestDamage = iDamage;
                }
                double innerBaseExp = baseExp * ((double) iDamage / totDamage);
                double expFraction = (innerBaseExp * expBonus) / (expApplicable.size() + 1);
                for (MapleCharacter expReceiver : expApplicable) {
                    Long oexp = expMap.get(expReceiver);
                    long iexp;
                    if (oexp == null) {
                        iexp = 0;
                    } else {
                        iexp = oexp.intValue();
                    }
                    double expWeight = (expReceiver == attacker.getKey() ? 2.0 : 1.0);
                    double levelMod = expReceiver.getLevel() / averagePartyLevel;
                    if (levelMod > 1.0 || this.attackers.containsKey(expReceiver.getId())) {
                        levelMod = 1.0;
                    }
                    iexp = Math.max(0, Math.min(iexp + Math.round(expFraction * expWeight * levelMod), Long.MAX_VALUE));
                    expMap.put(expReceiver, Long.valueOf(iexp));
                }
            }
            // FUCK we are done -.-
            for (Entry<MapleCharacter, Long> expReceiver : expMap.entrySet()) {
                boolean white = mostDamage ? expReceiver.getKey() == highest : false;
                //giveExpToCharacter(expReceiver.getKey(), expmap.exp, mostDamage ? expReceiver.getKey() == highest : false, expMap.size(), expmap.ptysize, expmap.Class_Bonus_EXP, expmap.Premium_Bonus_EXP, lastSkill);
                giveExpToCharacter(expReceiver.getKey(), expReceiver.getValue(), white, (byte) expMap.size());
            }
        }

        @Override
        public final int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + partyid;
            return result;
        }

        @Override
        public final boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PartyAttackerEntry other = (PartyAttackerEntry) obj;
            if (partyid != other.partyid) {
                return false;
            }
            return true;
        }
    }

    public int getLinkOid() {
        return linkoid;
    }

    public void setLinkOid(int lo) {
        this.linkoid = lo;
    }

    public final ConcurrentEnumMap<MonsterStatus, MonsterStatusEffect> getStati() {
        return stati;
    }

    public void addEmpty() {
        for (MonsterStatus stat : MonsterStatus.values()) {
            if (stat.isEmpty()) {
                stati.put(stat, new MonsterStatusEffect(stat, 0, 0, null, false));
            }
        }
    }

    public final int getStolen() {
        return stolen;
    }

    public final void setStolen(final int s) {
        this.stolen = s;
    }

    public final void handleSteal(MapleCharacter chr) {
        double showdown = 100.0;
        final MonsterStatusEffect mse = getBuff(MonsterStatus.SHOWDOWN);
        if (mse != null) {
            showdown += mse.getX();
        }

        Skill steal = SkillFactory.getSkill(4201004);//스틸
        final int level = chr.getTotalSkillLevel(steal), chServerrate = ChannelServer.getInstance(chr.getClient().getChannel()).getDropRate();
        if (level > 0 && !getStats().isBoss() && stolen == -1 && steal.getEffect(level).makeChanceResult()) {
            final MapleMonsterInformationProvider mi = MapleMonsterInformationProvider.getInstance();
            final List<MonsterDropEntry> de = mi.retrieveDrop(getId());
            if (de == null) {
                stolen = 0;
                return;
            }
            final List<MonsterDropEntry> dropEntry = new ArrayList<MonsterDropEntry>(de);
            Collections.shuffle(dropEntry);
            Item idrop;
            for (MonsterDropEntry d : dropEntry) { //set to 4x rate atm, 40% chance + 10x
                if (d.itemId > 0 && d.questid == 0 && d.itemId / 10000 != 238 && Randomizer.nextInt(999999) < (int) (1.5 * d.chance * chServerrate * (chr.getStat().dropBuff / 100.0) * (showdown / 100.0))) { //kinda op
                    if (GameConstants.getInventoryType(d.itemId) == MapleInventoryType.EQUIP) {
                        Equip eq = (Equip) MapleItemInformationProvider.getInstance().getEquipById(d.itemId);
                        idrop = MapleItemInformationProvider.getInstance().randomizeStats(eq);
                    } else {
                        idrop = new Item(d.itemId, (byte) 0, (short) (d.Maximum != 1 ? Randomizer.nextInt(d.Maximum - d.Minimum) + d.Minimum : 1), (byte) 0);
                    }
                    stolen = d.itemId;
                    map.spawnMobDrop(idrop, map.calcDropPos(getPosition(), getTruePosition()), this, chr, (byte) 0, (short) 0);
                    break;
                }
            }
        } else {
            stolen = 0; //failed once, may not go again
        }
    }

    public final void setLastNode(final int lastNode) {
        this.lastNode = lastNode;
    }

    public final int getLastNode() {
        return lastNode;
    }

    public final void cancelStatus(final MonsterStatus stat) {
        if (stat == MonsterStatus.BURN) {
            return;
        }
        final MonsterStatusEffect mse = stati.get(stat);
        if (mse == null || !isAlive()) {
            return;
        }
        mse.cancelPoisonSchedule(this);
        final MapleCharacter con = getController();
        if (con != null) {
            map.broadcastMessage(con, MobPacket.cancelMonsterStatus(getObjectId(), stat), getTruePosition());
            con.getClient().getSession().write(MobPacket.cancelMonsterStatus(getObjectId(), stat));
        } else {
            map.broadcastMessage(MobPacket.cancelMonsterStatus(getObjectId(), stat), getTruePosition());
        }
        stati.remove(stat);
        if (stat == MonsterStatus.MAGIC_DAMAGE_REFLECT) {
            try {
                reflections.remove(0);
            } catch (Exception e) {
            }
        }
        if (stat == MonsterStatus.WEAPON_DAMAGE_REFLECT) {
            try {
                reflections.remove(0);
            } catch (Exception e) {
            }
        }
        if (reflections.isEmpty()) {
            reflectpack = null; //무한 공반
        }
    }

    public final void cancelSingleStatus(final MonsterStatusEffect stat) {
        if (stat == null || !isAlive()) {
            return;
        }
        if (stat.getStati() != MonsterStatus.POISON && stat.getStati() != MonsterStatus.BURN) {
            cancelStatus(stat.getStati());
            return;
        }
        poisonsLock.writeLock().lock();
        try {
            for (MonsterStatusEffect sf : poisons) {
                if (sf.getStati() == stat.getStati()) {
                    poisons.remove(sf);
                    break;
                }
            }

            stat.cancelPoisonSchedule(this);
            final MapleCharacter con = getController();
            if (con != null) {
                map.broadcastMessage(con, MobPacket.cancelPoison(this.getObjectId(), stat), getTruePosition());
                con.getClient().getSession().write(MobPacket.cancelPoison(this.getObjectId(), stat));
            } else {
                map.broadcastMessage(MobPacket.cancelPoison(this.getObjectId(), stat), getTruePosition());
            }
        } finally {
            poisonsLock.writeLock().unlock();
        }
    }

    public final void cancelDropItem() {
        lastDropTime = 0;
    }

    public List<Integer> getReflections() {
        return reflections;
    }

    public final void startDropItemSchedule() {
        cancelDropItem();
        if (stats.getDropItemPeriod() <= 0 || !isAlive()) {
            return;
        }
        shouldDropItem = false;
        lastDropTime = System.currentTimeMillis();
    }

    public boolean shouldDrop(long now) {
        return lastDropTime > 0 && lastDropTime + (stats.getDropItemPeriod() * 1000) < now;
    }

    int time = 1;
    long lastHittedTime = 0;

    public void setHittedTime() {
        this.lastHittedTime = System.currentTimeMillis();
    }

    public void doDropItem(long now) {
        final int itemId;

        switch (getId()) {
            case 9300061:
                shouldDropItem = lastHittedTime + 11000 < System.currentTimeMillis();
                itemId = 4001101;
                break;
            case 9300102: //호위 멧돼지
                shouldDropItem = lastHittedTime + 11000 < System.currentTimeMillis();
                itemId = 4031507;
                break;
            default: //until we find out ... what other mobs use this and how to get the ITEMID
                cancelDropItem();
                return;
        }
        if (isAlive() && map != null) {
            if (shouldDropItem) {
                if (itemId == 4001101) {
                    map.broadcastMessage(MaplePacketCreator.serverNotice(6, "월묘가 " + (time++) + "번째 떡을 만들었습니다."));
                }
                if (itemId == 4031507) {
                    map.broadcastMessage(MaplePacketCreator.serverNotice(6, "멧돼지가 " + (time++) + "번째 페로몬 샘플을 떨어뜨렸습니다."));
                }
                map.spawnAutoDrop(itemId, getTruePosition());
            } else if (getId() != 9300061 && getId() != 9300102) {
                shouldDropItem = true;
            }
        }
        lastDropTime = now;
    }

    public byte[] getNodePacket() {
        return nodepack;
    }

    public void setNodePacket(final byte[] np) {
        this.nodepack = np;
    }

    public void registerKill(final long next) {
        this.nextKill = System.currentTimeMillis() + next;
    }

    public boolean shouldKill(long now) {
        return nextKill > 0 && now > nextKill;
    }

    public int getLinkCID() {
        return linkCID;
    }

    long lastPoison = System.currentTimeMillis();

    public boolean canPoison() {
        return lastPoison + 1000 < System.currentTimeMillis();
    }

    public final void doPoison(final MonsterStatusEffect status, final WeakReference<MapleCharacter> weakChr) {
        //if (!canPoison()) {
        //    return;
        //}
        lastPoison = System.currentTimeMillis();
        if ((status.getStati() == MonsterStatus.BURN || status.getStati() == MonsterStatus.POISON) && poisons.size() <= 0) {
//            System.out.println("Returned 1 : " + status.getStati().name());
            return;
        }
        if (status.getStati() != MonsterStatus.BURN && status.getStati() != MonsterStatus.POISON && !stati.containsKey(status.getStati())) {
//            System.out.println("Returned 2 : " + status.getStati() + ", " + stati.containsKey(status.getStati()));
            return;
        }
        if (weakChr == null) {
//            System.out.println("Returned 3");
            return;
        }
        long damage = status.getPoisonSchedule();
        final boolean shadowWeb = status.getSkill() == 4111003;
        final MapleCharacter chr = weakChr.get();
        boolean cancel = damage <= 0 || chr == null || chr.getMapId() != map.getId();
        if (damage >= hp) {
            damage = hp - 1;
        }
        if (!cancel) {
            damage(chr, damage, false);
            if (shadowWeb) {
                map.broadcastMessage(MobPacket.damageMonster(getObjectId(), damage), getTruePosition());
            }
        } else if (chr == null || chr.getMapId() != map.getId()) {
            if (!shadowWeb) {
                setHp(hp - damage);
            }
        }
    }

}
