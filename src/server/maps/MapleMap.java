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
package server.maps;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.Calendar;

import client.inventory.Equip;
import client.inventory.Item;
import constants.GameConstants;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleCoolDownValueHolder;
import client.MapleQuestStatus;
import client.Skill;
import client.SkillFactory;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.ServerConstants;
import database.DatabaseConnection;

import handling.channel.ChannelServer;
import handling.channel.handler.InventoryHandler;
import handling.world.MaplePartyCharacter;
import handling.world.PartyOperation;
import handling.world.World;
import handling.world.exped.ExpeditionType;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import server.MapleItemInformationProvider;
import server.MaplePortal;
import server.MapleStatEffect;
import server.Randomizer;
import server.MapleInventoryManipulator;
import server.life.MapleMonster;
import server.life.MapleNPC;
import server.life.MapleLifeFactory;
import server.life.Spawns;
import server.life.SpawnPoint;
import server.life.SpawnPointAreaBoss;
import server.life.MonsterDropEntry;
import server.life.MonsterGlobalDropEntry;
import server.life.MapleMonsterInformationProvider;
import tools.FileoutputUtil;
import tools.StringUtil;
import tools.MaplePacketCreator;
import tools.packet.PetPacket;
import tools.packet.MobPacket;
import scripting.EventManager;
import server.MapleCarnivalFactory;
import server.MapleCarnivalFactory.MCSkill;
import server.MapleSquad;
import server.MapleSquad.MapleSquadType;
import server.SpeedRunner;
import server.Timer;
import server.Timer.MapTimer;
import server.Timer.EtcTimer;
import server.events.MapleEvent;
import server.maps.MapleNodes.DirectionInfo;
import server.maps.MapleNodes.MapleNodeInfo;
import server.maps.MapleNodes.MaplePlatform;
import server.maps.MapleNodes.MonsterPoint;
import server.quest.MapleQuest;
import tools.Pair;
import tools.packet.UIPacket;

public final class MapleMap {

    /*
     * Holds mappings of OID -> MapleMapObject separated by MapleMapObjectType.
     * Please acquire the appropriate lock when reading and writing to the LinkedHashMaps.
     * The MapObjectType Maps themselves do not need to synchronized in any way since they should never be modified.
     */
    private final Map<MapleMapObjectType, LinkedHashMap<Integer, MapleMapObject>> mapobjects;
    private final Map<MapleMapObjectType, ReentrantReadWriteLock> mapobjectlocks;
    private final List<MapleCharacter> characters = new ArrayList<MapleCharacter>();
    private final ReentrantReadWriteLock charactersLock = new ReentrantReadWriteLock();
    private int runningOid = 500000;
    private final Lock runningOidLock = new ReentrantLock();
    private final List<Spawns> monsterSpawn = new ArrayList<Spawns>();
    private final AtomicInteger spawnedMonstersOnMap = new AtomicInteger(0);
    private final Map<Integer, MaplePortal> portals = new HashMap<Integer, MaplePortal>();
    private MapleFootholdTree footholds = null;
    private float monsterRate, recoveryRate;
    private MapleMapEffect mapEffect;
    private byte channel;
    private short decHP = 0, createMobInterval = 9000, top = 0, bottom = 0, left = 0, right = 0;
    private int consumeItemCoolTime = 0, protectItem = 0, decHPInterval = 10000, mapid, returnMapId, timeLimit,
            fieldLimit, maxRegularSpawn = 0, fixedMob, forcedReturnMap = 999999999, instanceid = -1,
            lvForceMove = 0, lvLimit = 0, permanentWeather = 0, partyBonusRate = 0;
    private boolean town, clock, personalShop, everlast = false, dropsDisabled = false, gDropsDisabled = false,
            soaring = false, squadTimer = false, isSpawns = true, checkStates = true;
    private String mapName, streetName, onUserEnter, onFirstUserEnter, speedRunLeader, changedMusic = "";
    private List<Integer> dced = new ArrayList<Integer>();
    private ScheduledFuture<?> squadSchedule;
    private long speedRunStart = 0, lastSpawnTime = 0, lastHurtTime = 0, outMapTime = 0, lastSkillTime = System.currentTimeMillis();

    private boolean canPetPickup = true;
    private List<Integer> blockedMobGen = new LinkedList<Integer>();
    private MapleNodes nodes;
    private MapleSquadType squad;
    private int fixSpawns;
    private double plusMob = 0;
    private double plusMobSize = 0;
    private double mobRate = 0;
    private Map<String, Integer> environment = new LinkedHashMap<String, Integer>();
    private AtomicInteger plusMobLastOsize = new AtomicInteger(0);

    private MapleCharacter mapOwner = null;
    public long mapOwnerLastActivityTime = Long.MAX_VALUE;
    private long tLastOzSummoned = 0;

    public ScheduledFuture<?> mechaDoorTimer = null;

    public MapleMap(final int mapid, final int channel, final int returnMapId, final float monsterRate) {
        this.mapid = mapid;
        this.channel = (byte) channel;
        this.returnMapId = returnMapId;
        if (this.returnMapId == 999999999) {
            this.returnMapId = mapid;
        }
        if (GameConstants.getPartyPlay(mapid) > 0) {
            this.monsterRate = (monsterRate - 1.0f) * 2.5f + 1.0f;
        } else {
            this.monsterRate = monsterRate;
        }
        EnumMap<MapleMapObjectType, LinkedHashMap<Integer, MapleMapObject>> objsMap = new EnumMap<MapleMapObjectType, LinkedHashMap<Integer, MapleMapObject>>(MapleMapObjectType.class);
        EnumMap<MapleMapObjectType, ReentrantReadWriteLock> objlockmap = new EnumMap<MapleMapObjectType, ReentrantReadWriteLock>(MapleMapObjectType.class);
        for (MapleMapObjectType type : MapleMapObjectType.values()) {
            objsMap.put(type, new LinkedHashMap<Integer, MapleMapObject>());
            objlockmap.put(type, new ReentrantReadWriteLock());
        }
        mapobjects = Collections.unmodifiableMap(objsMap);
        mapobjectlocks = Collections.unmodifiableMap(objlockmap);
    }

    public void setLastOzSummoned(long time) {
        this.tLastOzSummoned = time;
    }

    public long getOzSummonedRemainTime(long curTime) {
        int coolTime = 21 * 10 * 1000; //5분
        if (tLastOzSummoned != 0) {
            return Math.max(0, (tLastOzSummoned + coolTime) - curTime);
        }
        return 0;
    }

    public void setOzSkillCooldown() {
        long curTime = System.currentTimeMillis();
        long remainTime;
        if ((remainTime = getOzSummonedRemainTime(curTime)) > 0) { //소환 불가능할 때
            for (MapleCharacter chr : this.getCharacters()) {
                if (chr != null) {
                    if (chr.getTotalSkillLevel(1074) > 0 && !chr.skillisCooling(1074)) {
                        chr.addCooldown(1074, curTime, remainTime);
                        chr.getClient().sendPacket(MaplePacketCreator.skillCooldown(1074, (int) Math.max(0, (remainTime / 1000))));
                    }
                }
            }
        } else {
            for (MapleCharacter chr : this.getCharacters()) {
                if (chr != null) {
                    if (chr.getTotalSkillLevel(1074) > 0 && chr.skillisCooling(1074)) {
                        chr.removeCooldown(1074);
                        chr.getClient().sendPacket(MaplePacketCreator.skillCooldown(1074, 0));
                    }
                }
            }
        }
    }

    public void monsterKilled() {
        plusMobLastOsize.incrementAndGet();
    }
    private int xy;

    public void setXY(int x) {
        this.xy = x;
    }

    public final void setSpawns(final boolean fm) {
        this.isSpawns = fm;
    }

    public final boolean getSpawns() {
        return isSpawns;
    }

    public final void setFixedMob(int fm) {
        this.fixedMob = fm;
    }

    public final void setForceMove(int fm) {
        this.lvForceMove = fm;
    }

    public final int getForceMove() {
        return lvForceMove;
    }

    public final void setLevelLimit(int fm) {
        this.lvLimit = fm;
    }

    public final int getLevelLimit() {
        return lvLimit;
    }

    public final void setReturnMapId(int rmi) {
        this.returnMapId = rmi;
    }

    public final void setSoaring(boolean b) {
        this.soaring = b;
    }

    public final boolean canSoar() {
        return soaring;
    }

    public final void toggleDrops() {
        this.dropsDisabled = !dropsDisabled;
    }

    public final void setDrops(final boolean b) {
        this.dropsDisabled = b;
    }

    public final void toggleGDrops() {
        this.gDropsDisabled = !gDropsDisabled;
    }

    public final boolean togglePetPick() {
        canPetPickup = !canPetPickup;
        return canPetPickup;
    }

    public final boolean canPetPick() {
        return canPetPickup;
    }

    public final int getId() {
        return mapid;
    }

    public final MapleMap getReturnMap() {
        return ChannelServer.getInstance(channel).getMapFactory().getMap(returnMapId);
    }

    public final int getReturnMapId() {
        return returnMapId;
    }

    public final int getForcedReturnId() {
        return forcedReturnMap;
    }

    public final MapleMap getForcedReturnMap() {
        ChannelServer ch = ChannelServer.getInstance(channel);
        if (ch != null) {
            MapleMapFactory mapFactory = ch.getMapFactory();
            if (mapFactory != null) {
                return mapFactory.getMap(forcedReturnMap);
            }
        }
        return null;
    }

    public final void setForcedReturnMap(final int map) {
        this.forcedReturnMap = map;
    }

    public final float getRecoveryRate() {
        return recoveryRate;
    }

    public final void setRecoveryRate(final float recoveryRate) {
        this.recoveryRate = recoveryRate;
    }

    public final int getFieldLimit() {
        return fieldLimit;
    }

    public final void setFieldLimit(final int fieldLimit) {
        this.fieldLimit = fieldLimit;
    }

    public final void setCreateMobInterval(final short createMobInterval) {
        this.createMobInterval = createMobInterval;
    }

    public final void setTimeLimit(final int timeLimit) {
        this.timeLimit = timeLimit;
    }

    public final void setMapName(final String mapName) {
        this.mapName = mapName;
    }

    public final String getMapName() {
        return mapName;
    }

    public final String getStreetName() {
        return streetName;
    }

    public final void setFirstUserEnter(final String onFirstUserEnter) {
        this.onFirstUserEnter = onFirstUserEnter;
    }

    public final void setUserEnter(final String onUserEnter) {
        this.onUserEnter = onUserEnter;
    }

    public final String getFirstUserEnter() {
        return onFirstUserEnter;
    }

    public final String getUserEnter() {
        return onUserEnter;
    }

    public final boolean hasClock() {
        return clock;
    }

    public final void setClock(final boolean hasClock) {
        this.clock = hasClock;
    }

    public final boolean isTown() {
        return town;
    }

    public final void setTown(final boolean town) {
        this.town = town;
    }

    public final boolean allowPersonalShop() {
        return personalShop;
    }

    public final void setPersonalShop(final boolean personalShop) {
        this.personalShop = personalShop;
    }

    public final void setStreetName(final String streetName) {
        this.streetName = streetName;
    }

    public final void setEverlast(final boolean everlast) {
        this.everlast = everlast;
    }

    public final boolean getEverlast() {
        return everlast;
    }

    public final int getHPDec() {
        return decHP;
    }

    public final void setHPDec(final int delta) {
        if (delta > 0 || mapid == 749040100) { //pmd
            lastHurtTime = System.currentTimeMillis(); //start it up
        }
        decHP = (short) delta;
    }

    public final int getHPDecInterval() {
        return decHPInterval;
    }

    public final void setHPDecInterval(final int delta) {
        decHPInterval = delta;
    }

    public final int getHPDecProtect() {
        return protectItem;
    }

    public final void setHPDecProtect(final int delta) {
        this.protectItem = delta;
    }

    public final int getCurrentPartyId() {
        for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.PLAYER).values()) {
            MapleCharacter chr = (MapleCharacter) obj;
            if (chr.getParty() != null) {
                return chr.getParty().getId();
            }
        }
        return -1;
    }

    public final void addMapObject(final MapleMapObject mapobject) {
        runningOidLock.lock();
        int newOid;
        try {
            newOid = ++runningOid;
        } finally {
            runningOidLock.unlock();
        }

        mapobject.setObjectId(newOid);
        mapobjects.get(mapobject.getType()).put(newOid, mapobject);
    }

    private void spawnAndAddRangedMapObject(final MapleMapObject mapobject, final DelayedPacketCreation packetbakery) {
        addMapObject(mapobject);

        charactersLock.readLock().lock();
        try {
            final Iterator<MapleCharacter> itr = characters.iterator();
            MapleCharacter chr;
            while (itr.hasNext()) {
                chr = itr.next();
                if ((mapobject.getType() == MapleMapObjectType.MIST || chr.getTruePosition().distanceSq(mapobject.getTruePosition()) <= GameConstants.maxViewRangeSq())) {
                    packetbakery.sendPackets(chr.getClient());
                    chr.addVisibleMapObject(mapobject);
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
    }

    public final void removeMapObject(final MapleMapObject obj) {
        mapobjectlocks.get(obj.getType()).writeLock().lock();
        try {
            mapobjects.get(obj.getType()).remove(Integer.valueOf(obj.getObjectId()));
        } finally {
            mapobjectlocks.get(obj.getType()).writeLock().unlock();
        }
    }

    public final Point calcPointBelow(final Point initial) {
        final MapleFoothold fh = footholds.findBelow(initial);
        if (fh == null) {
            return null;
        }
        int dropY = fh.getY1();
        if (!fh.isWall() && fh.getY1() != fh.getY2()) {
            final double s1 = Math.abs(fh.getY2() - fh.getY1());
            final double s2 = Math.abs(fh.getX2() - fh.getX1());
            if (fh.getY2() < fh.getY1()) {
                dropY = fh.getY1() - (int) (Math.cos(Math.atan(s2 / s1)) * (Math.abs(initial.x - fh.getX1()) / Math.cos(Math.atan(s1 / s2))));
            } else {
                dropY = fh.getY1() + (int) (Math.cos(Math.atan(s2 / s1)) * (Math.abs(initial.x - fh.getX1()) / Math.cos(Math.atan(s1 / s2))));
            }
        }
        return new Point(initial.x, dropY);
    }

    public final Point calcDropPos(final Point initial, final Point fallback) {
        final Point ret = calcPointBelow(new Point(initial.x, initial.y - 99));
        if (ret == null) {
            return fallback;
        }
        return ret;
    }

    private void dropFromMonster(final MapleCharacter chr, final MapleMonster mob, final boolean instanced, int killId) {
        dropFromMonster(chr, mob, instanced, false);
    }

    private void dropFromMonster(final MapleCharacter chr, final MapleMonster mob, final boolean instanced, final boolean isGainDrop) {
        if (mob == null || chr == null || ChannelServer.getInstance(channel) == null || dropsDisabled || mob.dropsDisabled()) { //no drops in pyramid ok? no cash either
            return;
        }

        //We choose not to readLock for this.
        //This will not affect the internal state, and we don't want to
        //introduce unneccessary locking, especially since this function
        //is probably used quite often.
        if (!instanced && mapobjects.get(MapleMapObjectType.ITEM).size() >= 1000) {
            removeDrops();
        }
        //드롭 동시에 삭제 부분 
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final byte droptype = (byte) (mob.getStats().isExplosiveReward() ? 3 : mob.getStats().isFfaLoot() ? 2 : chr.getParty() != null ? 1 : 0);

        int bossDropEquipManipulate = 0;
        int bossDropSkillBookManipulate = 0;
        int bossIgnoreEquip = 0;

        if (mob.getId() == 8800002) {
            //자쿰
            bossIgnoreEquip = 1002357; //자쿰의 투구
            bossDropEquipManipulate = Randomizer.rand(3, 5);
            bossDropSkillBookManipulate = Randomizer.rand(3, 6);
        }
        if (mob.getId() == 8800102) {
            //카오스 자쿰
            bossIgnoreEquip = 1003112; //카오스 자쿰의 투구
            bossDropEquipManipulate = Randomizer.rand(3, 5);
            bossDropSkillBookManipulate = Randomizer.rand(3, 6);
        }
        /*if (mob.getId() == 8810018) {
         bossIgnoreEquip = 1122000; //혼테일의 목걸이
         bossDropEquipManipulate = Randomizer.rand(6, 9);
         bossDropSkillBookManipulate = Randomizer.rand(6, 10);
         }*/
        if (mob.getId() == 8810214) {
            bossIgnoreEquip = 1122076; //카오스혼테일의 목걸이
            bossDropEquipManipulate = Randomizer.rand(6, 9);
            bossDropSkillBookManipulate = Randomizer.rand(6, 10);
        }
        if (mob.getId() == 8500002) {
            //파풀라투스
            bossDropEquipManipulate = Randomizer.rand(2, 3);
            bossDropSkillBookManipulate = Randomizer.rand(2, 3);
        }
        if (mob.getId() == 8820001) {
            //핑크빈
            bossDropEquipManipulate = Randomizer.rand(9, 20);
            bossDropSkillBookManipulate = 0;
        }
        if (mob.getId() == 8510000 || mob.getId() == 8520000) {
            //피아누스
            bossDropEquipManipulate = Randomizer.rand(2, 3);
            bossDropSkillBookManipulate = Randomizer.rand(2, 3);
        }
        if (mob.getId() == 7220005 || mob.getId() == 9400265) {
            //베르가모트
            bossDropEquipManipulate = Randomizer.rand(2, 3);
            bossDropSkillBookManipulate = Randomizer.rand(2, 3);
        }

//        chr.getMap().broadcastMessage(MaplePacketCreator.serverNotice(6, "Boss Manipulate : IgnoreEquip : " + bossIgnoreEquip + " , BossEquipMax : " + bossDropEquipManipulate + " , BossSkillBookMax : " + bossDropSkillBookManipulate));
//        System.out.println("Boss Manipulate : IgnoreEquip : " + bossIgnoreEquip + " , BossEquipMax : " + bossDropEquipManipulate + " , BossSkillBookMax : " + bossDropSkillBookManipulate);
        final int mobpos = mob.getPosition().x, smesorate = ChannelServer.getInstance(channel).getMesoRate(), sdroprate = ChannelServer.getInstance(channel).getDropRate(), scashrate = ChannelServer.getInstance(channel).getCashRate();
        Item idrop;
        byte d = 1;
        Point pos = new Point(0, mob.getTruePosition().y);
        double showdown = 100.0;
        final MonsterStatusEffect mse = mob.getBuff(MonsterStatus.SHOWDOWN);
        if (mse != null) {
            showdown += mse.getX();
        }

        final MapleMonsterInformationProvider mi = MapleMonsterInformationProvider.getInstance();

        final List<MonsterDropEntry> derp = mi.retrieveDrop(mob.getId());

        final List<MonsterDropEntry> dropEntry = new ArrayList<MonsterDropEntry>();
        boolean MonsterPark = (mob.getId() >= 9800000 && mob.getId() <= 9800124);
        /*if (MonsterPark) {
         dropEntry.add(new MonsterDropEntry(4310020, 50000000, 1, 1, 0));
         }*/

        if (getId() >= 240000000 && getId() <= 240040610) { //리프레 //소스드롭 //
            dropEntry.add(new MonsterDropEntry(3961001, 150, 1, 1, 0));
        }
        if (getId() >= 211060000 && getId() <= 211070200) { //사자성
            dropEntry.add(new MonsterDropEntry(3962001, 150, 1, 1, 0));
        }
        if (getId() >= 270010100 && getId() <= 270030503) { //타임로드
            dropEntry.add(new MonsterDropEntry(3963001, 150, 1, 1, 0));
        }
        if (getId() >= 271030000 && getId() <= 271030540) { //무기고 //(getId() == 271030320) { //무기고
            dropEntry.add(new MonsterDropEntry(3964001, 150, 1, 1, 0));
        }
        if (getId() >= 271010000 && getId() <= 271020100) { //파괴된 헤네시스
            dropEntry.add(new MonsterDropEntry(3965001, 150, 1, 1, 0));
        }
        if (getId() >= 273000000 && getId() <= 273060300) { //황혼의 페리온
            dropEntry.add(new MonsterDropEntry(3966001, 150, 1, 1, 0));
        }

        if ((mob.getStats().getLevel() >= 0 && mob.getStats().getLevel() <= 120) && !mob.getStats().isBoss()) {//10000 1퍼
            dropEntry.add(new MonsterDropEntry(0, 999999, mob.getStats().getLevel() * 4, mob.getStats().getLevel() * 6, 0));//메소
        }
        if ((mob.getStats().getLevel() >= 121 && mob.getStats().getLevel() <= 199) && !mob.getStats().isBoss()) {//10000 1퍼
            dropEntry.add(new MonsterDropEntry(0, 999999, mob.getStats().getLevel() * 6, mob.getStats().getLevel() * 8, 0));//메소
        }
        if ((mob.getStats().getLevel() >= 200 && mob.getStats().getLevel() <= 279) && mob.getStats().isBoss()) {//10000 1퍼 140 140000
            dropEntry.add(new MonsterDropEntry(0, 999999, mob.getStats().getLevel() * 10, mob.getStats().getLevel() * 14, 0));//메소
        }
        if ((mob.getStats().getLevel() > 280 && mob.getStats().getLevel() <= 399) && mob.getStats().isBoss()) {//10000 1퍼 140 140000
            dropEntry.add(new MonsterDropEntry(0, 999999, mob.getStats().getLevel() * 14, mob.getStats().getLevel() * 18, 0));//메소
        }
        if ((mob.getStats().getLevel() >= 400 && mob.getStats().getLevel() <= 599) && mob.getStats().isBoss()) {//10000 1퍼 140 140000
            dropEntry.add(new MonsterDropEntry(0, 999999, mob.getStats().getLevel() * 22, mob.getStats().getLevel() * 26, 0));//메소
        }
        if ((mob.getStats().getLevel() >= 600 && mob.getStats().getLevel() <= 799) && mob.getStats().isBoss()) {//10000 1퍼 140 140000
            dropEntry.add(new MonsterDropEntry(0, 999999, mob.getStats().getLevel() * 24, mob.getStats().getLevel() * 28, 0));//메소
        }
        if ((mob.getStats().getLevel() >= 800 && mob.getStats().getLevel() <= 999) && mob.getStats().isBoss()) {//
            dropEntry.add(new MonsterDropEntry(0, 999999, mob.getStats().getLevel() * 26, mob.getStats().getLevel() * 34, 0));//메소
        }
        Calendar cal = Calendar.getInstance();
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        switch (dayOfWeek) {
            case 2: //월
                if (mob.getStats().getLevel() >= 110 && MonsterPark) {//(코드,확률,최소갯수,최대갯수,퀘스트코드)
                    dropEntry.add(new MonsterDropEntry(2434746, 1500, 1, 1, 0));//월상자
                }
                if (mob.getStats().getLevel() >= 50 && mob.getStats().getLevel() <= 110) {
                    dropEntry.add(new MonsterDropEntry(5062002, 1500, 1, 1, 0));//미큐               
                }
                if (mob.getStats().getLevel() > 110 && mob.getStats().getLevel() <= 160) {
                    dropEntry.add(new MonsterDropEntry(5062002, 3000, 1, 1, 0));//미큐               
                }
                if (mob.getStats().getLevel() > 160 && mob.getStats().getLevel() <= 999) {
                    dropEntry.add(new MonsterDropEntry(5062002, 5000, 1, 1, 0));//미큐               
                }
                break;
            case 3: //화
                if (mob.getStats().getLevel() >= 110 && MonsterPark) {//(코드,확률,최소갯수,최대갯수,퀘스트코드)
                    dropEntry.add(new MonsterDropEntry(2434747, 1500, 1, 1, 0));//화상자
                }
                if (mob.getStats().getLevel() >= 50 && mob.getStats().getLevel() <= 110) {
                    dropEntry.add(new MonsterDropEntry(5062002, 1500, 1, 1, 0));//미큐               
                }
                if (mob.getStats().getLevel() > 110 && mob.getStats().getLevel() <= 160) {
                    dropEntry.add(new MonsterDropEntry(5062002, 3000, 1, 1, 0));//미큐               
                }
                if (mob.getStats().getLevel() > 160 && mob.getStats().getLevel() <= 999) {
                    dropEntry.add(new MonsterDropEntry(5062002, 5000, 1, 1, 0));//미큐               
                }
                break;
            case 4: //수
                if (mob.getStats().getLevel() >= 110 && MonsterPark) {//(코드,확률,최소갯수,최대갯수,퀘스트코드)
                    dropEntry.add(new MonsterDropEntry(2434748, 1500, 1, 1, 0));//수상자
                }
                if (mob.getStats().getLevel() >= 50 && mob.getStats().getLevel() <= 110) {
                    dropEntry.add(new MonsterDropEntry(5062002, 1500, 1, 1, 0));//미큐               
                }
                if (mob.getStats().getLevel() > 110 && mob.getStats().getLevel() <= 160) {
                    dropEntry.add(new MonsterDropEntry(5062002, 3000, 1, 1, 0));//미큐               
                }
                if (mob.getStats().getLevel() > 160 && mob.getStats().getLevel() <= 999) {
                    dropEntry.add(new MonsterDropEntry(5062002, 5000, 1, 1, 0));//미큐               
                }
                break;
            case 5: //목
                if (mob.getStats().getLevel() >= 110 && MonsterPark) {//(코드,확률,최소갯수,최대갯수,퀘스트코드)
                    dropEntry.add(new MonsterDropEntry(2434749, 1500, 1, 1, 0));//목상자
                }
                if (mob.getStats().getLevel() >= 50 && mob.getStats().getLevel() <= 110) {
                    dropEntry.add(new MonsterDropEntry(5062002, 1500, 1, 1, 0));//미큐               
                }
                if (mob.getStats().getLevel() > 110 && mob.getStats().getLevel() <= 160) {
                    dropEntry.add(new MonsterDropEntry(5062002, 3000, 1, 1, 0));//미큐               
                }
                if (mob.getStats().getLevel() > 160 && mob.getStats().getLevel() <= 999) {
                    dropEntry.add(new MonsterDropEntry(5062002, 5000, 1, 1, 0));//미큐               
                }
                break;
            case 6: //금
                if (mob.getStats().getLevel() >= 110 && MonsterPark) {//(코드,확률,최소갯수,최대갯수,퀘스트코드)
                    dropEntry.add(new MonsterDropEntry(2434750, 1500, 1, 1, 0));//금상자
                }
                if (mob.getStats().getLevel() >= 50 && mob.getStats().getLevel() <= 110) {
                    dropEntry.add(new MonsterDropEntry(5062002, 1500, 1, 1, 0));//미큐               
                }
                if (mob.getStats().getLevel() > 110 && mob.getStats().getLevel() <= 160) {
                    dropEntry.add(new MonsterDropEntry(5062002, 3000, 1, 1, 0));//미큐               
                }
                if (mob.getStats().getLevel() > 160 && mob.getStats().getLevel() <= 999) {
                    dropEntry.add(new MonsterDropEntry(5062002, 5000, 1, 1, 0));//미큐               
                }
                break;
            case 7: //토
                if (mob.getStats().getLevel() >= 110 && MonsterPark) {//(코드,확률,최소갯수,최대갯수,퀘스트코드)
                    dropEntry.add(new MonsterDropEntry(2434751, 1500, 1, 1, 0));//토상자
                }
                if (mob.getStats().getLevel() >= 50 && mob.getStats().getLevel() <= 110) {
                    dropEntry.add(new MonsterDropEntry(5062002, 1500, 1, 1, 0));//미큐               
                }
                if (mob.getStats().getLevel() > 110 && mob.getStats().getLevel() <= 160) {
                    dropEntry.add(new MonsterDropEntry(5062002, 3000, 1, 1, 0));//미큐               
                }
                if (mob.getStats().getLevel() > 160 && mob.getStats().getLevel() <= 999) {
                    dropEntry.add(new MonsterDropEntry(5062002, 5000, 1, 1, 0));//미큐               
                }
                break;
            case 1: //일
                if (mob.getStats().getLevel() >= 110 && MonsterPark) {//(코드,확률,최소갯수,최대갯수,퀘스트코드)
                    dropEntry.add(new MonsterDropEntry(2434745, 1500, 1, 1, 0));//일상자
                }
                if (mob.getStats().getLevel() >= 50 && mob.getStats().getLevel() <= 110) {
                    dropEntry.add(new MonsterDropEntry(5062002, 1500, 1, 1, 0));//미큐               
                }
                if (mob.getStats().getLevel() > 110 && mob.getStats().getLevel() <= 160) {
                    dropEntry.add(new MonsterDropEntry(5062002, 3000, 1, 1, 0));//미큐               
                }
                if (mob.getStats().getLevel() > 160 && mob.getStats().getLevel() <= 999) {
                    dropEntry.add(new MonsterDropEntry(5062002, 5000, 1, 1, 0));//미큐               
                }
                break;
        }
        if (mob.getStats().getLevel() >= 10 && mob.getStats().getLevel() <= 999) {//레벨 10이상 
            dropEntry.add(new MonsterDropEntry(5062002, 100, 1, 1, 0));//미큐
            dropEntry.add(new MonsterDropEntry(2330004, 3, 1, 1, 0));//샤이니 불릿
            dropEntry.add(new MonsterDropEntry(2070005, 3, 1, 1, 0));//뇌전
        }
        if (mob.getStats().getLevel() >= 160 && mob.getStats().getLevel() <= 999) {//레벨 160이상 파괴된 헤네시스 
            dropEntry.add(new MonsterDropEntry(5150054, 50, 1, 1, 0));//오브
            dropEntry.add(new MonsterDropEntry(2070006, 3, 1, 1, 0));//일비
            dropEntry.add(new MonsterDropEntry(2330005, 3, 1, 1, 0));//이터널 불릿
            dropEntry.add(new MonsterDropEntry(4020013, 100, 1, 1, 0));//꿈의조각
            dropEntry.add(new MonsterDropEntry(4020013, 100, 1, 1, 0));//시간의조각
        }
        if (mob.getStats().getLevel() >= 200 && mob.getStats().getLevel() <= 999) {//레벨 200이상 전당
            dropEntry.add(new MonsterDropEntry(4310210, 1000, 1, 1, 0));//추뎀코인
        }
        if (mob.getStats().getLevel() >= 250 && mob.getStats().getLevel() <= 999) {//레벨 250 헤이븐 이상
            dropEntry.add(new MonsterDropEntry(2431000, 120, 1, 1, 0));//메포 5000원
            dropEntry.add(new MonsterDropEntry(2432001, 50, 1, 1, 0));//캐포 5000원
            dropEntry.add(new MonsterDropEntry(2070024, 2, 1, 1, 0));//무한의 수리검
            dropEntry.add(new MonsterDropEntry(2330007, 2, 1, 1, 0));//아머피어싱 불릿
        }
        if (mob.getStats().getLevel() >= 330 && mob.getStats().getLevel() <= 999) {//레벨 330여로이상
            dropEntry.add(new MonsterDropEntry(2431000, 150, 1, 1, 0));//메포 5000원
            dropEntry.add(new MonsterDropEntry(2432001, 60, 1, 1, 0));//캐포 5000원
            dropEntry.add(new MonsterDropEntry(2070023, 2, 1, 1, 0));//플레임표창
            dropEntry.add(new MonsterDropEntry(2070019, 2, 1, 1, 0));//악마의 표창
        }
        if (mob.getStats().getLevel() >= 400 && mob.getStats().getLevel() <= 999) {//레벨 330여로이상
            dropEntry.add(new MonsterDropEntry(2431000, 170, 1, 1, 0));//메포 5000원
            dropEntry.add(new MonsterDropEntry(2432001, 70, 1, 1, 0));//캐포 5000원
            dropEntry.add(new MonsterDropEntry(2431001, 40, 1, 1, 0));//메포 10000원
            dropEntry.add(new MonsterDropEntry(2432002, 8, 1, 1, 0));//캐포 10000원
            dropEntry.add(new MonsterDropEntry(2070023, 2, 1, 1, 0));//플레임표창
            dropEntry.add(new MonsterDropEntry(2070019, 2, 1, 1, 0));//악마의 표창
        }
        if (mob.getStats().getLevel() >= 119 && mob.getStats().getLevel() <= 250) {//레벨 120~250 성아
        }
        if (mob.getStats().getLevel() >= 250 && mob.getStats().getLevel() < 300) {//레벨 250~300헤이븐
        }
        if (mob.getStats().getLevel() >= 300 && mob.getStats().getLevel() < 330) {//레벨 300~330뉴리프
            dropEntry.add(new MonsterDropEntry(4310277, 50, 1, 1, 0));//뉴 리프 시티 코인[환수제작용]
        }
        if (mob.getStats().getLevel() >= 330 && mob.getStats().getLevel() <= 450) {//레벨 330 ~ 450 소멸의여로
            dropEntry.add(new MonsterDropEntry(4310277, 50, 1, 1, 0));//뉴 리프 시티 코인[환수제작용]
            dropEntry.add(new MonsterDropEntry(3970024, 2, 1, 1, 0));//정화의 가루 소멸의여로
            dropEntry.add(new MonsterDropEntry(3970020, 1500, 1, 1, 0));//빛바랜 심볼 소멸의여로
        }
        if (mob.getStats().getLevel() >= 450 && mob.getStats().getLevel() < 620) {//레벨 450~620츄츄
            dropEntry.add(new MonsterDropEntry(4310277, 50, 1, 1, 0));//뉴 리프 시티 코인[환수제작용]
            dropEntry.add(new MonsterDropEntry(3970024, 2, 1, 1, 0));//정화의 가루 뉴리프시티
            dropEntry.add(new MonsterDropEntry(3970020, 1500, 1, 1, 0));//빛바랜 심볼 뉴리프시티
            dropEntry.add(new MonsterDropEntry(4310241, 50, 1, 1, 0));//츄츄코인 [츄츄강화용]
            dropEntry.add(new MonsterDropEntry(3970025, 2, 1, 1, 0));//정화의 가루 츄츄아일랜드
            dropEntry.add(new MonsterDropEntry(3970021, 1500, 1, 1, 0));//빛바랜 심볼 츄츄아일랜드
        }
        if (mob.getStats().getLevel() >= 620 && mob.getStats().getLevel() <= 830) {//레벨 620~830 레헬른
            dropEntry.add(new MonsterDropEntry(4310277, 50, 1, 1, 0));//뉴 리프 시티 코인[환수제작용]
            dropEntry.add(new MonsterDropEntry(3970024, 2, 1, 1, 0));//정화의 가루 뉴리프시티
            dropEntry.add(new MonsterDropEntry(3970020, 1500, 1, 1, 0));//빛바랜 심볼 뉴리프시티
            dropEntry.add(new MonsterDropEntry(4310241, 50, 1, 1, 0));//츄츄코인 [츄츄강화용]
            dropEntry.add(new MonsterDropEntry(3970025, 2, 1, 1, 0));//정화의 가루 츄츄아일랜드
            dropEntry.add(new MonsterDropEntry(3970021, 1500, 1, 1, 0));//빛바랜 심볼 츄츄아일랜드
            dropEntry.add(new MonsterDropEntry(4310058, 20, 1, 1, 0));//레헬른 코인 [다크엔블 제작용]
            dropEntry.add(new MonsterDropEntry(3970026, 2, 1, 1, 0));//정화의 가루 레헬른
            dropEntry.add(new MonsterDropEntry(3970022, 1500, 1, 1, 0));//빛바랜 심볼 레헬른
        }
        if (mob.getStats().getLevel() >= 830 && mob.getStats().getLevel() <= 999) {//레벨 800이상 999이하 드롭 아르카나
            dropEntry.add(new MonsterDropEntry(4310277, 50, 1, 1, 0));//뉴 리프 시티 코인[환수제작용]
            dropEntry.add(new MonsterDropEntry(3970024, 2, 1, 1, 0));//정화의 가루 뉴리프시티
            dropEntry.add(new MonsterDropEntry(3970020, 1500, 1, 1, 0));//빛바랜 심볼 뉴리프시티
            dropEntry.add(new MonsterDropEntry(4310241, 50, 1, 1, 0));//츄츄코인 [츄츄강화용]
            dropEntry.add(new MonsterDropEntry(3970025, 2, 1, 1, 0));//정화의 가루 츄츄아일랜드
            dropEntry.add(new MonsterDropEntry(3970021, 1500, 1, 1, 0));//빛바랜 심볼 츄츄아일랜드
            dropEntry.add(new MonsterDropEntry(4310058, 20, 1, 1, 0));//레헬른 코인 [다크엔블 제작용]
            dropEntry.add(new MonsterDropEntry(3970026, 2, 1, 1, 0));//정화의 가루 레헬른
            dropEntry.add(new MonsterDropEntry(3970022, 1500, 1, 1, 0));//빛바랜 심볼 레헬른
            dropEntry.add(new MonsterDropEntry(4310260, 20, 1, 1, 0));//아르카나 정령의보옥 [화엔블제작용]
            dropEntry.add(new MonsterDropEntry(3970027, 2, 1, 1, 0));//정화의 가루 아르카나
            dropEntry.add(new MonsterDropEntry(3970023, 1500, 1, 1, 0));//빛바랜 심볼 아르카나
        }
        if (mob.getStats().getLevel() > 10) {//10렙몹 이상드롭
            dropEntry.add(new MonsterDropEntry(4032575, 3000, 1, 1, 10546));//일일퀘스트 온도계
            dropEntry.add(new MonsterDropEntry(2049100, 15, 1, 3, 0));//혼줌
            dropEntry.add(new MonsterDropEntry(2049401, 15, 1, 3, 0));//잠재일반
            dropEntry.add(new MonsterDropEntry(4001513, 500, 1, 1, 0));//얼룩무늬 티켓 조각
            dropEntry.add(new MonsterDropEntry(4001515, 300, 1, 1, 0));//표범무늬 티켓
            dropEntry.add(new MonsterDropEntry(4001521, 150, 1, 1, 0));//호랑무늬 티켓
            dropEntry.add(new MonsterDropEntry(4001431, 500, 1, 1, 0));//황금사원 티켓
            dropEntry.add(new MonsterDropEntry(3994102, 1000, 1, 1, 0));//나침반
            dropEntry.add(new MonsterDropEntry(3994103, 1000, 1, 1, 0));//나침반
            dropEntry.add(new MonsterDropEntry(3994104, 1000, 1, 1, 0));//나침반
            dropEntry.add(new MonsterDropEntry(3994105, 1000, 1, 1, 0));//나침반
            dropEntry.add(new MonsterDropEntry(4001126, 2000, 1, 1, 0));//단풍잎
            dropEntry.add(new MonsterDropEntry(2431013, 1000, 1, 1, 0));//1천메포
            dropEntry.add(new MonsterDropEntry(2432000, 700, 1, 1, 0));//1천캐포
        }
        /*
         if (chr.getLevel() - mob.getStats().getLevel() >= -15 && chr.getLevel() - mob.getStats().getLevel() <=15 && mob.getStats().getLevel() >= 1) {
         //70레벨이상 캐릭에게서만 자기와 15렙 이상 차이안나는 몹에게 글로벌드롭(코드,확률,최소갯수,최대갯수,퀘스트코드)
         dropEntry.add(new MonsterDropEntry(4001246, 50000, 1 , 1, (short) 0)); //따듯한햇살
         }     
         */
        if (getId() >= 925020000 && getId() <= 925033804) {
            if (GameConstants.isMulungBoss(mob.getId())) {
                for (int idd = 2022359; idd <= 2022421; ++idd) {
                    dropEntry.add(new MonsterDropEntry(idd, 3000, 1, 1, 0));
                }
            } else {
                for (int idd = 2022430; idd <= 2022433; ++idd) {
                    dropEntry.add(new MonsterDropEntry(idd, 150000, 1, 1, 0));
                }
            }
        } else if (getId() >= 190000000 && getId() <= 198000000) {
            dropEntry.add(new MonsterDropEntry(4000047, 1000, 1, 1, 0));
            // 순서대로 아이템, 확률, 최소, 최대, 퀘스트 pc방 드롭
            dropEntry.addAll(derp); //마우스 드롭
        } else if (derp == null) { //if no drops, no global drops either <3
            return;
        } else {
            dropEntry.addAll(derp);
        }

        // mulung dojo  custom drop
        Collections.shuffle(dropEntry);
        boolean firstIterated = true;
        boolean checkBossDrop = false;

        if (bossDropEquipManipulate > 0 || bossDropSkillBookManipulate > 0) {
            checkBossDrop = true;
        }

        int slax = 0;

        /*
         *
         * Custom Boss Drop Manipuator
         *
         * 강제로 스킬북 또는 장비가 몇개는 뜨도록 설정한다.
         * 일단 기본적으로 이터레이트 한번씩은 돌리도록 하고,
         * 자쿰이나 혼테일의 목걸이를 제외한 장비가 몇개가 뜨는지를 체크한다.
         * 뜰때마다 1씩 값을 줄여서
         * 0이 될때까지 무한으로 루프를 돌린다.
         */
        // 과부하의 시작
        // 에녹 : 읍읍 확률적으로 과부하를 드립니다!
        boolean mesoDropped = false;
        boolean isFucked = false;
        boolean drop_rate = false;
        int drop_rate_value = 0;
        if (chr.getBuffedValue(MapleBuffStat.DROP_RATE) != null) {
            drop_rate = true;
            drop_rate_value = chr.getBuffedValue(MapleBuffStat.DROP_RATE);
        }
        do {
            if (isFucked) {
                checkBossDrop = false;
                break;
            }

            for (final MonsterDropEntry de : dropEntry) {
                if (de.itemId == mob.getStolen()) {
                    continue;
                }

                slax++;
                if (de.questid > 0) {
                    if (chr.getQuestStatus(de.questid) != 1) {
                        continue;
                    }
                }
                Pair<Integer, Integer> questInfo = MapleItemInformationProvider.getInstance().getQuestItemInfo(de.itemId);
                if (questInfo != null) {
                    if (chr.haveItem(de.itemId, questInfo.getRight(), true, true)) {
                        continue;
                    }
                }

//                double MDrate = 0.0;
//                if (chr.getBuffedValue(MapleBuffStat.MESO_RATE) != null) {//meso up by item
//                    MDrate = chr.getBuffedValue(MapleBuffStat.MESO_RATE) / 100.0D;
//                } else {
//                    MDrate = 1.0;
//                }
                double MDrate = 1.0;

                int chance = (int) (de.chance * (showdown / 100.0) * sdroprate);

                if (drop_rate) {
                    chance = (int) (chance * drop_rate_value / 100);
                }

                if (de.itemId == 0) {
                    chance *= MDrate;
                }

                /*if (isGainDrop && de.itemId != 0) {//개별드롭X 이상함 만지지마셈
                 int per = 50;
                 int minus = (int) (chance * (per * 0.01));
                 chance = Math.max(0, chance - minus);
                 }*/
                /*double fixChance = (GameConstants.isDropData(de.itemId, mob) * 10000);
                 if (fixChance > 0) {
                 chance = (int) (fixChance * (showdown / 100.0)) * sdroprate;
                 } else {
                 chance = (int) (de.chance * (showdown / 100.0) * sdroprate);
                 }*/
                //chr.dropMessage(6, "MDrate:" + MDrate);
                if (chr.getBuffedValue1(2022091)) {// 길드의 축복 50%
                    chance += (chance * ((float) 50 / 100));
                }

                //엘리트 채널 드롭률 증가
                if (ChannelServer.isElite(this.getChannel()) && mob.getStats().getLevel() >= 170) {
                    chance += (chance * 0.3);
                }

                if (chr.hasEquipped(1137000)) { //드롭률 상승
                    chance += (chance * 0.2);
                }

                if (chr.getStat().customizeStat != null) {
                    int dropChance = chr.getStat().customizeStat.drop;
                    if (dropChance > 0) {
                        chance += (chance * (dropChance * 0.01));
                    }
                }

                if (Randomizer.rand(0, 999999) < chance) {
                    if (mesoDropped && !(droptype == 3 || mob.getStats().isBoss()) && de.itemId == 0) { //not more than 1 sack of meso
                        continue;
                    }
                    if (droptype == 3) {
                        pos.x = (mobpos + (d % 2 == 0 ? (40 * (d + 1) / 2) : -(40 * (d / 2))));
                    } else {
                        pos.x = (mobpos + ((d % 2 == 0) ? (25 * (d + 1) / 2) : -(25 * (d / 2))));
                    }
                    int plusX = 30;
                    if (d % 2 != 0) {
                        plusX *= -1;
                    }
                    if (calcPointBelow(new Point(pos.x + plusX, pos.y)) == null) {
                        pos.x -= plusX / 2;
                    }
                    if (de.itemId == 0) { // meso
                        if (!firstIterated) { //메소는 처음 한번만 이터레이트를 돌린다.
                            continue;
                        }
                        //int mesos = Randomizer.rand(mob.getStats().getLevel() * 8, mob.getStats().getLevel() * 12);
                        int mesos = Randomizer.rand(de.Minimum, de.Maximum);

                        if (mesos > 0) {
                            // chr.dropMessage(5, "chance: " + chance);
//                            if (chr.getBuffedValue(MapleBuffStat.MESOUP) != null) {
//                                //Meso Up
//                                double rate = chr.getBuffedValue(MapleBuffStat.MESOUP) / 100.0D;
//                                mesos *= rate;
//                            }
                            double incMesoProp = chr.getStat().mesoBuff / 100.0;
                            if (incMesoProp > 1.0) {
                                mesos *= incMesoProp;
                            }

                            int endMeso = (mesos * smesorate);
                            double a = ((double) chr.getSymbolMeso() / 100);
                            int b = (int) (endMeso * a);

                            int summonAddMoney = 0;
                            MapleQuestStatus summonMoneyData = chr.getQuestNAdd(MapleQuest.getInstance(202306122));
                            if (summonMoneyData != null) {
                                String data = null;
                                if ((data = summonMoneyData.getCustomData()) != null) {
                                    try {
                                        int summonMoney = Integer.parseInt(data);
                                        summonAddMoney += (int) Math.max(0, Math.min((endMeso * (summonMoney * 0.01)), Integer.MAX_VALUE));
                                    } catch (NumberFormatException e) {

                                    }
                                }
                            }

                            int c = 0;
                            if (chr.getStat().customizeStat != null) {
                                int mesoChance = chr.getStat().customizeStat.meso;
                                if (mesoChance > 0) {
                                    c += (endMeso * (mesoChance * 0.01));
                                }
                            }

                            spawnMobMesoDrop(endMeso + b + summonAddMoney + c, calcDropPos(pos, mob.getTruePosition()), mob, chr, false, droptype, isGainDrop);
                            //chr.dropMessage(5, "endmeso: " + endMeso);
                            mesoDropped = true;
                        }
                    } else {
                        if (checkBossDrop && !firstIterated) { //보스의 스킬북 또는 장비 최대 드롭 갯수를 체크함과 동시에 두번째 이상 이터레이팅이면
                            if (bossDropSkillBookManipulate <= 0 && (de.itemId / 10000 == 229 || de.itemId / 10000 == 228)) { //최대 갯수를 채웠으면 브레이크.
                                continue;
                            }
                            if (bossDropEquipManipulate <= 0 && de.itemId / 1000000 == 1) { //최대 갯수를 채웠으면 브레이크.
                                continue;
                            }
                            if (de.itemId / 10000 != 229 && de.itemId / 10000 != 228 && de.itemId / 1000000 != 1) {
                                continue;
                            }
                        }
                        if (checkBossDrop) { //보스의 최대 아이템 갯수를 체크하는가?
                            if (de.itemId != bossIgnoreEquip) { //체크한다면 자투와 혼목같은 아이템을 제외했는가?
                                if (de.itemId / 1000000 == 1) {
                                    bossDropEquipManipulate--;
                                } else if ((de.itemId / 10000 == 229 || de.itemId / 10000 == 228)) {
                                    bossDropSkillBookManipulate--;
                                }
                            } else if (!firstIterated) {
                                continue;
                            }
                            if (!firstIterated) { //두번째 이상 이터레이팅일때,
                                if (bossDropEquipManipulate <= 0 && bossDropSkillBookManipulate <= 0) {
                                    //둘다 최대갯수만큼 채웠으면 브레이크.
                                    //System.out.println("보스드롭에큅메뮬 드롭스킬북메뮬이 0이하이면 브레이크");
                                    isFucked = true;
                                    checkBossDrop = false;
                                    break;
                                }
                            }
                        }
                        if (GameConstants.getInventoryType(de.itemId) == MapleInventoryType.EQUIP) {
                            idrop = ii.randomizeStats((Equip) ii.getEquipById(de.itemId));
                        } else {
                            final int range = Math.abs(de.Maximum - de.Minimum);
                            idrop = new Item(de.itemId, (byte) 0, (short) (de.Maximum != 1 ? Randomizer.nextInt(range <= 0 ? 1 : range) + de.Minimum : 1), (byte) 0);
                        }
                        idrop.setGMLog("Dropped from monster " + mob.getId() + " on " + mapid);
                        if (de.itemId == 4310030) { // 쿼리에서 삭제하기 귀찮으니 일일이... 이렇게 안뜨게 방지
                            continue;
                        }
                        spawnMobDrop(idrop, calcDropPos(pos, mob.getTruePosition()), mob, chr, droptype, de.questid, isGainDrop);
                    }
                    d++;
                }
            }
            if (firstIterated) {
                firstIterated = false;
            }

            if (slax > dropEntry.size() + 10) {
                checkBossDrop = true;
                isFucked = true;
                // 비상탈출!!
                break;
            }
        } while (checkBossDrop);
        final List<MonsterGlobalDropEntry> globalEntry = new ArrayList<MonsterGlobalDropEntry>(mi.getGlobalDrop());
        Collections.shuffle(globalEntry);
        final int cashz = (int) ((mob.getStats().isBoss() && mob.getStats().getHPDisplayType() == 0 ? 20 : 1) * scashrate);
        final int cashModifier = (int) ((mob.getStats().isBoss() ? (mob.getStats().isPartyBonus() ? (mob.getMobExp() / 1000) : 0) : (mob.getMobExp() / 1000 + mob.getMobMaxHp() / 20000))); //no rate
        // Global Drops
        for (final MonsterGlobalDropEntry de : globalEntry) {
            Pair<Integer, Integer> questInfo = MapleItemInformationProvider.getInstance().getQuestItemInfo(de.itemId);
            if (questInfo != null) {
                if (chr.haveItem(de.itemId, questInfo.getRight(), true, true)) {
                    continue;
                }
            }

            int chance = de.chance * sdroprate;
            //엘리트 채널 글로벌 드롭률 증가
            if (ChannelServer.isElite(this.getChannel()) && mob.getStats().getLevel() >= 170) {
                chance += (chance * 0.3);
            }

//            if (chr.hasEquipped(1137000)) { //드롭률 상승
//                    chance += (chance * 0.2);
//            }
//            if (chr.getPlayerShop() != null) { // 고상
//                chance += (chance * 0.1);
//            }
            if (Randomizer.nextInt(999999) < chance && (de.continent < 0 || (de.continent < 10 && mapid / 100000000 == de.continent) || (de.continent < 100 && mapid / 10000000 == de.continent) || (de.continent < 1000 && mapid / 1000000 == de.continent))) {
                if (de.itemId == 0) {
                    //chr.modifyCSPoints(1, (int) ((Randomizer.nextInt(cashz) + cashz + cashModifier) * (chr.getStat().cashBuff / 100.0) * chr.getCashMod()), true);
                } else if (!gDropsDisabled) {
                    if (droptype == 3) {
                        pos.x = (mobpos + (d % 2 == 0 ? (40 * (d + 1) / 2) : -(40 * (d / 2))));
                    } else {
                        pos.x = (mobpos + ((d % 2 == 0) ? (25 * (d + 1) / 2) : -(25 * (d / 2))));
                    }
                    int plusX = 30;
                    if (d % 2 != 0) {
                        plusX *= -1;
                    }
                    if (calcPointBelow(new Point(pos.x + plusX, pos.y)) == null) {
                        pos.x -= plusX / 2;
                    }
                    if (GameConstants.getInventoryType(de.itemId) == MapleInventoryType.EQUIP) {
                        idrop = ii.randomizeStats((Equip) ii.getEquipById(de.itemId));
                    } else {
                        idrop = new Item(de.itemId, (byte) 0, (short) (de.Maximum != 1 ? Randomizer.nextInt(de.Maximum - de.Minimum) + de.Minimum : 1), (byte) 0);
                    }
                    idrop.setGMLog("Dropped from monster " + mob.getId() + " on " + mapid + " (Global)");
                    spawnMobDrop(idrop, calcDropPos(pos, mob.getTruePosition()), mob, chr, de.onlySelf ? 0 : droptype, de.questid, isGainDrop);
                    d++;
                }
            }
        }

        int flag = mob.getEventDropFlag();
        if (flag > 0) {
            List<Integer> eventDrops = new ArrayList<>();
            if ((flag & 1) > 0) {
                eventDrops.add(3010007);
            }
            if ((flag & 2) > 0) {
                eventDrops.add(3010008);
            }
            if ((flag & 4) > 0) {
                eventDrops.add(3010009);
            }
            if ((flag & 8) > 0) {
                eventDrops.add(3010000);
            }
            if ((flag & 0x10) > 0) {
                eventDrops.add(2210000);
            }
            if ((flag & 0x20) > 0) {
                eventDrops.add(2210001);
            }
            if ((flag & 0x40) > 0) {
                eventDrops.add(2210002);
            }
            if ((flag & 0x80) > 0) {
                eventDrops.add(5370000);
            }
            for (int itemId : eventDrops) {
                if (GameConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                    idrop = ii.randomizeStats((Equip) ii.getEquipById(itemId));
                } else {
                    idrop = new Item(itemId, (byte) 0, (short) 1, (byte) 0);
                }
                pos.x = (mobpos + ((d % 2 == 0) ? (25 * (d + 1) / 2) : -(25 * (d / 2))));
                int plusX = 30;
                if (d % 2 != 0) {
                    plusX *= -1;
                }
                if (calcPointBelow(new Point(pos.x + plusX, pos.y)) == null) {
                    pos.x -= plusX / 2;
                }
                idrop.setGMLog("Dropped from monster " + mob.getId() + " on " + mapid + " (Event)");
                spawnMobDrop(idrop, calcDropPos(pos, mob.getTruePosition()), mob, chr, droptype, 0, isGainDrop);
                d++;
            }
        }

    }

    public void removeMonster(final MapleMonster monster) {
        if (monster == null) {
            return;
        }
        spawnedMonstersOnMap.decrementAndGet();
        broadcastMessage(MobPacket.killMonster(monster.getObjectId(), 0));
        removeMapObject(monster);
        monster.killed();
    }

    public void killMonster(final MapleMonster monster) { // For mobs with removeAfter
        if (monster == null) {
            return;
        }
        spawnedMonstersOnMap.decrementAndGet();
        monster.setHp(0);
        if (monster.getLinkCID() <= 0) {
            monster.spawnRevives(this);
        }

        broadcastMessage(MobPacket.killMonster(monster.getObjectId(), monster.getStats().getSelfD() < 0 ? 1 : monster.getStats().getSelfD()));
        removeMapObject(monster);
        monster.killed();
    }

    public final void killMonster(final MapleMonster monster, final MapleCharacter chr, final boolean withDrops, final boolean second, byte animation) {
        killMonster(monster, chr, withDrops, second, animation, 0);
    }

    public final void killMonster(final MapleMonster monster, final MapleCharacter chr, final boolean withDrops, final boolean second, byte animation, final int lastSkill) {
        if (getAllMonstersThreadsafe().size() <= 1) {
            switch (this.mapid) {
                case 240080100:
                case 240080200:
                case 240080300:
                case 240080400:
                case 240080500:
                case 240080600:
                case 240080700:
                case 240080800:
                    clearEffect();
                    break;
            }
        }

        final int mobid = monster.getId();

        /*int lierand = Randomizer.rand(0, 40000);//거탐
         //chr.gainKillMob(1);
         if (lierand < 3 && chr.getMacroStr().equals("")) {
         String[] s1 = {"123", "456", "789"};
         String[] s2 = {"마", "무", "미", "모"};
         String[] s3 = {"가", "나", "다", "라", "마"};
         int i1 = Randomizer.rand(0, s1.length - 1);
         int i2 = Randomizer.rand(0, s2.length - 1);
         int i3 = Randomizer.rand(0, s3.length - 1);
         chr.setMacroStr(s1[i1] + s2[i2] + s3[i3]);
         chr.getMap().startMapEffect(chr.getName() + "님, 1분 내에 채팅으로 '" + chr.getMacroStr() + "'을 작성해주세요.", 5120008);
         chr.dropMessage(6, chr.getName() + "님, 1분 내에 채팅으로 '" + chr.getMacroStr() + "'을 작성해주세요.");
         chr.dropMessage(1, chr.getName() + "님, 1분 내에 채팅으로 '" + chr.getMacroStr() + "'을 작성해주세요.");
         chr.startMacro(60);//매크로 시간
         }*/
        if (monster.getStats().getLevel() >= 100) { //100레벨 이상 몬스터 잡을 시
            chr.killp += 1; //킬포인트 1 상승
            if (chr.killp >= 3000) { //킬포인트 5000점 이상 시
                chr.killp -= 3000;
                World.Guild.gainGP(chr.getGuildId(), 1);
                //World.Guild.setGuildNotice(chr.getGuildId(),"길드원이 사냥을 하여 1GP 를 획득하였습니다." + World.Guild.getGP(chr.getGuildId()) + "점");
            }
        }

        if ((monster.getId() == 8810122 || monster.getId() == 8810018) && !second) {
            MapTimer.getInstance().schedule(new Runnable() {

                @Override
                public void run() {
                    killMonster(monster, chr, true, true, (byte) 1);
                    killAllMonsters(true);
                }
            }, 3000);
            return;
        }
        if (monster.getId() == 8820014) { //pb sponge, kills pb(w) first before dying
            killMonster(8820000);
        } else if (monster.getId() == 9300166) { //ariant pq bomb
            animation = 4; //or is it 3?
        }
        if (mobid == ServerConstants.worldboss && chr.getMapId() == ServerConstants.worldbossmap) {
            chr.sethottimebosslastattack(true);
            MapleNPC npc = MapleLifeFactory.getNPC(ServerConstants.worldNpc);
            npc.setPosition(monster.getPosition());
            npc.setCy(monster.getPosition().y);
            npc.setRx0(monster.getPosition().x + 50);
            npc.setRx1(monster.getPosition().x - 50);
            npc.setFh(monster.getMap().getFootholds().findBelow(chr.getPosition()).getId());
            npc.setCustom(true);
            chr.getMap().addMapObject(npc);
            chr.getMap().broadcastMessage(MaplePacketCreator.spawnNPC(npc, true));
            chr.getMap().broadcastMessage(MaplePacketCreator.yellowChat("5분내로 엔피시 를 눌러 보상을 받지않으면 보상을 받지 못합니다."));
            final MapleMap mapto = chr.getClient().getChannelServer().getMapFactory().getMap(ServerConstants.worldbossfirstmap);
            for (MapleCharacter cc : chr.getMap().getCharacters()) {
                cc.startMapTimeLimitTask(300, mapto);
            }
        }
        spawnedMonstersOnMap.decrementAndGet();
        removeMapObject(monster);
        monster.killed();
        final MapleSquad sqd = getSquadByMap();
        final boolean instanced = sqd != null || monster.getEventInstance() != null || getEMByMap() != null;
        int dropOwner = monster.killBy(chr, lastSkill);
        switch (monster.getId()) {
            case 8145102:
            case 8145202:
                if (getId() == 223030210) {
                    chr.getMap().spawnNpc(2192000, new Point(-336, 4));
                }
                break;
            case 8810122:
            case 8810018:
                if (!second) {
                    MapTimer.getInstance().schedule(new Runnable() {
                        @Override
                        public void run() {
                            killMonster(monster, chr, true, true, (byte) 1);
                            killAllMonsters(true);
                        }
                    }, 3000);
                    return;
                }
                break;
            case 8820014:
                killMonster(8820000);
                break;
            case 8840002: // 레드 크로키
                if (getId() == 211060201) {
                    if (chr.getQuestStatus(3139) == 1) {
                        if (getAllMonster().size() <= 1) {
                            chr.dropMessage(-1, "사자왕의 성 첫번째 봉인이 풀렸습니다.");
                            chr.getQuestNAdd(MapleQuest.getInstance(3139)).setCustomData("1");
                        }
                    }
                }
                break;
            case 8210006: // 교도관 보어
                if (getId() == 211060401) {
                    if (chr.getQuestStatus(3140) == 1) {
                        if (getAllMonster().size() <= 1) {
                            chr.dropMessage(-1, "사자왕의 성 두번째 봉인이 풀렸습니다.");
                            chr.getQuestNAdd(MapleQuest.getInstance(3140)).setCustomData("1");
                        }
                    }
                }
            case 8210007: // 교도관 라이노
                if (getId() == 211060601) {
                    if (chr.getQuestStatus(3141) == 1) {
                        if (getAllMonster().size() <= 1) {
                            chr.dropMessage(-1, "사자왕의 성 세번째 봉인이 풀렸습니다.");
                            chr.getQuestNAdd(MapleQuest.getInstance(3141)).setCustomData("1");
                        }
                    }
                }
                break;
            case 9300166:
                animation = 4; //or is it 3?
                break;
            case 9300326:
                if (getId() >= 910520000 && getId() <= 910520004) { // 트리스탄의 무덤
                    chr.getMap().spawnNpc(1061015, new Point(-30, 190));
                }
                break;
        }
        if (animation >= 0) {
            broadcastMessage(MobPacket.killMonster(monster.getObjectId(), animation));
        }
        if (mobid == ServerConstants.eventboss) {
            chr.seteventbosslastattack(true);
        }
        if (monster.getBuffToGive() > -1) {
            final int buffid = monster.getBuffToGive();
            final MapleStatEffect buff = MapleItemInformationProvider.getInstance().getItemEffect(buffid);

            charactersLock.readLock().lock();
            try {
                for (final MapleCharacter mc : characters) {
                    if (mc.isAlive()) {
                        buff.applyTo(mc);

                        switch (monster.getId()) {
                            case 8810018:
                            case 8810122:
                            case 8820001:
                                mc.getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(buffid, 13, mc.getLevel(), 1)); // HT nine spirit
                                broadcastMessage(mc, MaplePacketCreator.showBuffeffect(mc.getId(), buffid, 13, mc.getLevel(), 1), false); // HT nine spirit
                                break;
                        }
                    }
                }
            } finally {
                charactersLock.readLock().unlock();
            }
        }
        final Skill revieveSkill = SkillFactory.getSkill(32111006);
        if (revieveSkill != null) {
            final MapleStatEffect reveiveBuff = chr.getStatForBuff(MapleBuffStat.REAPER);
            if (reveiveBuff != null) {
                final int revieveSkillLevel = chr.getSkillLevel(revieveSkill);
                if (revieveSkillLevel > 0) {
                    final MapleStatEffect reveiveEffect = revieveSkill.getEffect(revieveSkillLevel);
                    if (lastSkill != 0) {
                        if (Randomizer.rand(0, 100) < reveiveEffect.getProb()) {
                            final MapleSummon reviveSummon = new MapleSummon(chr, 32111006, reveiveEffect.getLevel(), monster.getTruePosition(), SummonMovementType.WALK_STATIONARY);
                            if (chr.getSummonsSize() < 3) {
                                spawnSummon(reviveSummon);
                                chr.addSummon(reviveSummon);
                            }
                        }
                    }
                }
            }
        }
        int randoms = (int) (Math.floor(Math.random() * 50000)); //이확률을 높일시 스페셜 몹뜰확률이 낮아짐 10000 정도가 적당
        int randoms2 = (int) (Math.floor(Math.random() * 100)); //각 몬스터별 출현 확률
        if (randoms >= 0 && randoms <= 3) {
            if (chr.getLevel() >= 10) {
                if (randoms2 >= 0 && randoms2 <= 33) {
                    MapleMonster mons = MapleLifeFactory.getMonster(9500397); //경인년 호돌이
                    spawnMonsterOnGroundBelow(mons, chr.getTruePosition());
                } else if (randoms2 >= 34 && randoms2 <= 66) {
                    MapleMonster mons = MapleLifeFactory.getMonster(9500397); //경인년 호걸
                    spawnMonsterOnGroundBelow(mons, chr.getTruePosition());
                } else if (randoms2 >= 67 && randoms2 <= 100) {
                    MapleMonster mons = MapleLifeFactory.getMonster(9500397); //경인년 호걸
                    spawnMonsterOnGroundBelow(mons, chr.getTruePosition());
                }
            }
        }
        if (!monster.getStats().isBoss()) {
            if (this.getMobsSize() > 3) {
                int i = 0;
                for (final MapleMapObject mmo : getAllMonstersThreadsafe()) {
                    MapleMonster mons = (MapleMonster) mmo;
                    if (chr.getPosition().x - 150 < mons.getPosition().x && chr.getPosition().x + 150 > mons.getPosition().x) {
                        if (chr.getPosition().y - 100 < mons.getPosition().y && chr.getPosition().y + 100 > mons.getPosition().y) {
                            ++i;
                        }
                    }
                }
                int mobsize = chr.getClient().getChannelServer().getMapFactory().getMap(mapid).getNumMonsters();
                if (mobsize > 8 && i > 4) {
                    if (i > (mobsize / 2)) {
                        //World.Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM] 현재 감지 위치는 [닉네임 : "+chr.getName()+" / " + chr.getClient().getChannelServer().getMapFactory().getMap(mapid).getStreetName() + " : " + chr.getClient().getChannelServer().getMapFactory().getMap(mapid).getMapName() + "] 이며, 현재 [" + mapid + "] 에서 공격중입니다."));
                    }
                }
            }
        }
        //final int mobid = monster.getId();
        ExpeditionType type = null;
        if (monster.getStats().getId() == 8180001) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(5, "[4차전직] " + chr.getName() + "님이 " + chr.getMap().getChannel() + "채널에서 그리프를 학살했습니다."));
        }
        if (monster.getStats().getId() == 8180000) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(5, "[4차전직] " + chr.getName() + "님이 " + chr.getMap().getChannel() + "채널에서 마뇽을 학살했습니다."));
        }
        if (monster.getStats().getId() == 8840000) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "[보스] " + chr.getName() + "님이 " + chr.getMap().getChannel() + "채널에서 반레온 을 격파했습니다."));
        }
        if (monster.getStats().getId() == 8850011) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("- " + chr.getName() + "님의 시그너스 원정대가 " + chr.getMap().getChannel() + "채널에서 타락한 여제 : 시그너스를 처치하였습니다."));
        }
        if (monster.getStats().getId() == 9300467) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("- " + chr.getName() + "님의 켄타 원정대가 " + chr.getMap().getChannel() + "채널에서 강화된 피아누스 를 처치하였습니다."));
        }
        if (monster.getStats().getId() == 8810122) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("- " + chr.getName() + "님의 카오스 혼테일 원정대가 " + chr.getMap().getChannel() + "채널에서 카오스 혼테일을 처치하였습니다."));
        }
        if (monster.getStats().getId() == 8800102) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("- " + chr.getName() + "님의 카오스 자쿰 원정대가 " + chr.getMap().getChannel() + "채널에서 카오스 자쿰을 처치하였습니다."));
        }
        if (monster.getStats().getId() == 8300007) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("- " + chr.getName() + "님의 드래곤 라이더 원정대가 " + chr.getMap().getChannel() + "채널에서 드래곤 라이더를 처치하였습니다."));
        }
        if (monster.getStats().getId() == 9300028) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("- " + chr.getName() + "님의 길드 대항전 원정대가 " + chr.getMap().getChannel() + "채널에서 에레고스를 처치하였습니다."));
        }
        if (mobid == 8810018 && mapid == 240060200) { // Horntail
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "수많은 도전 끝에 혼테일을 격파한 원정대여! 그대들이 진정한 리프레의 영웅이다!"));
            charactersLock.readLock().lock();
            try {
            } finally {
                charactersLock.readLock().unlock();
            }
            //FileoutputUtil.log(FileoutputUtil.Horntail_Log, MapDebug_Log());
            if (speedRunStart > 0) {
                type = ExpeditionType.Horntail;
            }
            doShrine(true);
        } else if (mobid == 8810122 && mapid == 240060201) { // Horntail
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "수 많은 도전 끝에 혼테일을 물리친 원정대여! 그대들이 진정한 리프레의 영웅이다!"));
            doShrine(true);
        } else if (mobid == 4300013 && mapid == 103040430) {
            chr.getClient().getPlayer().dropMessage(-1, "락 스피릿을 퇴치후 들리는 음악을 기억해주세요!");
            chr.getClient().getSession().write(MaplePacketCreator.playSound("quest2288/5"));
            chr.getClient().getSession().write(MaplePacketCreator.musicChange("Bgm14/0"));
        } else if (mobid == 6160003 && mapid == 200101500) {
            chr.getClient().getPlayer().dropMessage(-1, "TIP. 크세르크세스를 퇴치시 일정확률로 미카엘의 안경이 드롭됩니다.");
        } else if (mobid == 9400266 && mapid == 802000111) {//네오도쿄카무나
            World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("- " + chr.getName() + "님의 카무나 원정대가 " + chr.getMap().getChannel() + "채널에서 카무나를 처치하였습니다."));
            doShrine(true);
        } else if (mobid == 9400265 && mapid == 802000211) {//네오도쿄오다이바
            World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("- " + chr.getName() + "님의 베르가모트 원정대가 " + chr.getMap().getChannel() + "채널에서 베르가모트를 처치하였습니다."));
            doShrine(true);
        } else if (mobid == 9400270 && mapid == 802000411) {//네오도쿄아키하바라
            World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("- " + chr.getName() + "님의 칼리하 원정대가 " + chr.getMap().getChannel() + "채널에서 칼리하를 처치하였습니다."));
            doShrine(true);
        } else if (mobid == 9400273 && mapid == 802000611) {//네오도쿄 상공
            World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("- " + chr.getName() + "님의 니벨룽겐 원정대가 " + chr.getMap().getChannel() + "채널에서 니벨룽겐을 처치하였습니다."));
            doShrine(true);
        } else if (mobid == 9400294 && mapid == 802000711) {//네오도쿄시부야
            World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("- " + chr.getName() + "님의 듀나스 원정대가 " + chr.getMap().getChannel() + "채널에서 듀나스를 처치하였습니다."));
            doShrine(true);
        } else if (mobid == 9400296 && mapid == 802000803) {//네오도쿄관출
            World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("- " + chr.getName() + "님의 코어블레이즈 원정대가 " + chr.getMap().getChannel() + "채널에서 코어블레이즈를 처치하였습니다."));
            doShrine(true);
        } else if (mobid == 9400289 && mapid == 802000821) { //네오도쿄 백화점
            World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("- " + chr.getName() + "님의 아우프헤븐 원정대가 " + chr.getMap().getChannel() + "채널에서 아우프헤븐를 처치하였습니다."));
            doShrine(true);
            //INSERT HERE: 2095_tokyo
        } else if (mobid == 8830000 && mapid == 105100300) { //발록
            if (speedRunStart > 0) {
                type = ExpeditionType.Normal_Balrog;
            }
        } else if ((mobid == 9420544 || mobid == 9420549) && mapid == 551030200 && monster.getEventInstance() != null && monster.getEventInstance().getName().contains(getEMByMap().getName())) {
            doShrine(getAllReactor().isEmpty());
        } else if (mobid == 8820001 && mapid == 270050100) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "지치지 않는 열정으로 핑크빈을 물리친 원정대여! 그대들이 진정한 시간의 승리자다!"));

            if (speedRunStart > 0) {
                type = ExpeditionType.Pink_Bean;
            }
            doShrine(true);
        } else if (mobid == 8850011 && mapid == 274040100) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "어둠으로 물든 에레브를 구한 영웅들이여! 여제와 신수가 그대들을 위한 거대한 축복을 선사하리!"));

            if (speedRunStart > 0) {
                type = ExpeditionType.Cygnus;
            }
            doShrine(true);
        } else if (mobid == 8880400) {
            if (chr.getParty() != null) {
                World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("- " + chr.getName() + "님의 원정대가 " + chr.getMap().getChannel() + "채널에서 [진힐라]를 처치하였습니다."));
            }
        } else if (mobid == 8880000) {
            if (chr.getParty() != null) {
                World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("- " + chr.getName() + "님의 원정대가 " + chr.getMap().getChannel() + "채널에서 [매그너스]를 처치하였습니다."));
            }
        } else if (mobid == 8820119) {
            if (chr.getParty() != null) {
                World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("- " + chr.getName() + "님의 카오스 파풀라투스 원정대가 " + chr.getMap().getChannel() + "채널에서 카오스 파풀라투스를 처치하였습니다."));
            }
        } else if (mobid == 9801028 && mapid == 123356782) {
            if (chr.getParty() != null) {
                World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("- " + chr.getName() + "님의 스우 데미안 원정대가 " + chr.getMap().getChannel() + "채널에서 [스우 페이즈1] 을 처치하였습니다."));
            }
        } else if (mobid == 9801029 && mapid == 123356783) {
            if (chr.getParty() != null) {
                World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("- " + chr.getName() + "님의 스우 데미안 원정대가 " + chr.getMap().getChannel() + "채널에서 [스우 페이즈2] 를 처치하였습니다."));
            }
        } else if (mobid == 9801030 && mapid == 123356784) {
            if (chr.getParty() != null) {
                World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("- " + chr.getName() + "님의 스우 데미안 원정대가 " + chr.getMap().getChannel() + "채널에서 [스우] 를 처치하였습니다."));
            }
        } else if (mobid == 9300890 && mapid == 123356786) {
            if (chr.getParty() != null) {
                World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("- " + chr.getName() + "님의 원정대가 " + chr.getMap().getChannel() + "채널에서 [데미안 페이즈1] 을 처치하였습니다."));
            }
        } else if (mobid == 9300891) {
            if (chr.getParty() != null) {
                World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("- " + chr.getName() + "님의 원정대가 " + chr.getMap().getChannel() + "채널에서 [데미안] 을 처치하였습니다."));
            }
        } else if (mobid == 450004150 && mapid == 8880140) {
            if (chr.getParty() != null) {
                World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("- " + chr.getName() + "님의 원정대가 " + chr.getMap().getChannel() + "채널에서 [루시드 페이즈1] 을 처치하였습니다."));
            }
        } else if (mobid == 450004550 && mapid == 8880150) {
            if (chr.getParty() != null) {
                World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("- " + chr.getName() + "님의 원정대가 " + chr.getMap().getChannel() + "채널에서 [각성의 루시드] 를 처치하였습니다."));
            }
        } else if (mobid == 123356789 && mapid == 8880501) {
            if (chr.getParty() != null) {
                World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("- " + chr.getName() + "님의 원정대가 " + chr.getMap().getChannel() + "채널에서 [하얀마법사] 를 처치하였습니다."));
            }
        } else if (mobid == 8880500) {
            if (chr.getParty() != null) {
                World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("- " + chr.getName() + "님의 원정대가 " + chr.getMap().getChannel() + "채널에서 검은마법사 를 처치하였습니다."));
            }
            /*
             작업 하실 때 이 부분들 다 지우고 
             goMap에는 123356786
             exMap에는 123456775
             clock에는 216 * 100
             spawnMob에는 9300890
             pos에는 new Point(1387, 16)
             를 입력하시면 됩니다.    
             */
        } else if (mobid == 8840000 && mapid == 211070100) {

            if (speedRunStart > 0) {
                type = ExpeditionType.Von_Leon;
            }
            doShrine(true);
        } else if (mobid == 8800002 && mapid == 280030000) {

//            FileoutputUtil.log(FileoutputUtil.Zakum_Log, MapDebug_Log());
            if (speedRunStart > 0) {
                type = ExpeditionType.Zakum;
            }
            doShrine(true);
        } else if (mobid == 8800102 && mapid == 280030001) {

            //FileoutputUtil.log(FileoutputUtil.Zakum_Log, MapDebug_Log());
            if (speedRunStart > 0) {
                type = ExpeditionType.Chaos_Zakum;
            }

            doShrine(true);
        } else if (mobid >= 8800003 && mobid <= 8800010) {
            boolean makeZakReal = true;
            final Collection<MapleMonster> monsters = getAllMonstersThreadsafe();

            for (final MapleMonster mons : monsters) {
                if (mons.getId() >= 8800003 && mons.getId() <= 8800010) {
                    makeZakReal = false;
                    break;
                }
            }
            if (makeZakReal) {
                for (final MapleMapObject object : monsters) {
                    final MapleMonster mons = ((MapleMonster) object);
                    if (mons.getId() == 8800000) {
                        final Point pos = mons.getTruePosition();
                        this.killAllMonsters(true);
                        MapleMonster mob = MapleLifeFactory.getMonster(8800000);
                        mob.setHp((long) (mob.getHp() * 1.0));
                        spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(8800000), pos);
                        break;
                    }
                }
            }
        } else if (mobid >= 8800103 && mobid <= 8800110) {
            boolean makeZakReal = true;
            final Collection<MapleMonster> monsters = getAllMonstersThreadsafe();

            for (final MapleMonster mons : monsters) {
                if (mons.getId() >= 8800103 && mons.getId() <= 8800110) {
                    makeZakReal = false;
                    break;
                }
            }
            if (makeZakReal) {
                for (final MapleMonster mons : monsters) {
                    if (mons.getId() == 8800100) {
                        final Point pos = mons.getTruePosition();
                        this.killAllMonsters(true);
                        MapleMonster mob = MapleLifeFactory.getMonster(8800100);
                        mob.setHp((long) (mob.getHp() * 1.0));
                        spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(8800100), pos);
                        break;
                    }
                }
            }

        } else if (mobid == 8820008) { //wipe out statues and respawn
            for (final MapleMapObject mmo : getAllMonstersThreadsafe()) {
                MapleMonster mons = (MapleMonster) mmo;
                if (mons.getLinkOid() != monster.getObjectId()) {
                    killMonster(mons, chr, false, false, animation);
                }
            }
        } else if (mobid >= 8820010 && mobid <= 8820014) {
            for (final MapleMapObject mmo : getAllMonstersThreadsafe()) {
                MapleMonster mons = (MapleMonster) mmo;
                if (mons.getId() != 8820000 && mons.getId() != 8820001 && mons.getObjectId() != monster.getObjectId() && mons.isAlive() && mons.getLinkOid() == monster.getObjectId()) {
                    killMonster(mons, chr, false, false, animation);
                }
            }
        } else if (mobid == 8800002 || mobid == 8800102) {//자쿰
            broadcastMessage(MaplePacketCreator.showPQreward(chr.getId()));
        } else if (mobid == 8810018) {//혼테일
            broadcastMessage(MaplePacketCreator.showPQreward(chr.getId()));
        } else if (mobid == 9300003) {//킹슬라임 첫번째 동행
            broadcastMessage(MaplePacketCreator.showPQreward(chr.getId()));
        } else if (mobid == 9300012) {//알리샤르 차원의 균열
            broadcastMessage(MaplePacketCreator.showPQreward(chr.getId()));
        } else if (mobid == 9300182) {//초강화형 포이즌 골렘 독안개의 숲
            broadcastMessage(MaplePacketCreator.showPQreward(chr.getId()));
        } else if (mobid == 9700037) {//유령선 함장 안개바다 유령선
            broadcastMessage(MaplePacketCreator.showPQreward(chr.getId()));
        } else if (mobid == 8300007) {//드래곤 라이더
            broadcastMessage(MaplePacketCreator.showPQreward(chr.getId()));
        } else if (mobid == 9300119 || mobid == 9300105 || mobid == 9300106 || mobid == 9300107) {//데비존
            broadcastMessage(MaplePacketCreator.showPQreward(chr.getId()));
        }

        if (GameConstants.isExpeditionMonster(mobid) > 0) {
            for (final MapleCharacter user : chr.getMap().getCharacters()) {
                user.setBattlePoints(user.getBattlePoints() + GameConstants.isExpeditionMonster(mobid));
                String chat = "#e#b몬스터 : (Lv." + MapleLifeFactory.getMonster(mobid).getStats().getLevel() + ") " + MapleLifeFactory.getMonster(mobid).getStats().getName();
                chat += "\r\n\r\n#r획득 보스 포인트 : " + GameConstants.isExpeditionMonster(mobid) + " 포인트";
                user.getClient().getSession().write(MaplePacketCreator.sendHint(chat, 250, 60));
            }
        }

        if (type != null) {
            if (speedRunStart > 0 && speedRunLeader.length() > 0) {
                long endTime = System.currentTimeMillis();
                String time = StringUtil.getReadableMillis(speedRunStart, endTime);
                //broadcastMessage(MaplePacketCreator.serverNotice(5, speedRunLeader + "'s squad has taken " + time + " to defeat " + type.name() + "!"));
                getRankAndAdd(speedRunLeader, time, type, (endTime - speedRunStart), (sqd == null ? null : sqd.getMembers()));
                endSpeedRun();
            }

        }

        if (withDrops && dropOwner != 1) {
            MapleCharacter drop = null;
            if (dropOwner <= 0) {
                drop = chr;
            } else {
                drop = getCharacterById(dropOwner);
                if (drop == null) {
                    drop = chr;
                }
            }
            boolean gainDropMonster = false;
            //개별 드롭 몹 코드 지정하는 곳
            switch (monster.getId()) {
                case 8850011:
                case 9800105:
                case 9800091:
                case 9800113:
                case 9800084:
                case 9800099:
                case 9300467:
                case 9300468:
                case 9300028:
                case 9800003:
                case 9800009:
                case 9800016:
                case 9800025:
                case 9800031:

                case 9800037:
                case 9800044:
                case 9800050:
                case 9800056:
                case 9800066:
                case 9800072:

                case 8810122:
                case 8800102:
                case 8840000:

                case 9400270:
                case 9400294:
                case 9400296:
                case 9400265:
                case 9400266:
                case 9400273:
                case 9400297:
                case 9400289:
                case 8800002:
                case 8810018:
                    gainDropMonster = true;
                    break;
            }
            if (gainDropMonster) {
                if (chr.getParty() != null) {
                    for (MapleCharacter user : chr.getPartyMembers()) {
                        if (user != null) {
                            if (user.getMapId() == chr.getMapId() && user.getClient().getChannel() == chr.getClient().getChannel()) {
                                dropFromMonster(user, monster, instanced, gainDropMonster);
                            }
                        }
                    }
                } else { //막타 친 사람이 파티 없을 때, 다 보이고 자기 혼자 드롭 개인보상
                    dropFromMonster(drop, monster, instanced, lastSkill);
                }
//                for (MapleCharacter user : this.getCharactersThreadsafe()) {
//                    if (user != null) {
//                        if (user.getMapId() == chr.getMapId() && user.getClient().getChannel() == chr.getClient().getChannel()) {
//                            dropFromMonster(user, monster, instanced, gainDropMonster);
//                        }
//                    }
//                }

            } else {
                dropFromMonster(drop, monster, instanced, lastSkill);
            }
        }
        if (Math.floor(Math.random() * 500) <= 1 && monster.getStats().getLevel() >= 50) {//배율-높을수록 안줌 캐시
            final int rand = ((short) (Math.random() * 10));//마일리지
            chr.modifyCSPoints(3, rand, true);
            //chr.dropMessage(5, "[안내] " + rand + " 사냥포인트(마일리지) 가 적립되었습니다.");
        }

        if (Collection(monster.getStats().getId())) {
            if (chr.getQuestNAdd(MapleQuest.getInstance(monster.getStats().getId())).getCustomData() == null) { //보스컬렉션 처치조건
                chr.getQuestNAdd(MapleQuest.getInstance(monster.getStats().getId())).setCustomData("0");
            }
            if (Integer.parseInt(chr.getQuestNAdd(MapleQuest.getInstance(monster.getStats().getId())).getCustomData()) < 10) {
                chr.getQuestNAdd(MapleQuest.getInstance(monster.getStats().getId())).setCustomData("" + (Integer.parseInt(chr.getQuestNAdd(MapleQuest.getInstance(monster.getStats().getId())).getCustomData()) + 1) + "");
                chr.dropMessage(6, "[보스 컬렉션] " + monster.getStats().getName() + "을 처치하였습니다. " + chr.getQuestNAdd(MapleQuest.getInstance(monster.getStats().getId())).getCustomData() + " / 10");
            } else {
                chr.getClient().getSession().write(MaplePacketCreator.sendHint("#b[보스컬렉션]\r\n이미 보스컬렉션 처치횟수를 달성한 몬스터입니다.#k", 300, 5));
            }
        }

        /*드라 하드코딩*/
        if (chr.getMapId() == 240080100 || chr.getMapId() == 240080200 || chr.getMapId() == 240080300 || chr.getMapId() == 240080400 || chr.getMapId() == 240080500) {
            if (getMobsSize() == 0) {
                //broadcastMessage(MaplePacketCreator.showEffect("quest/party/clear"));
            } else {
                broadcastMessage(UIPacket.getTopMsg("몬스터가 " + getMobsSize() + "마리 남았습니다."));
            }
        }
        if (chr.getMapId() == 240080600) {
            if (getMobsSize() == 0) {
                chr.getMap().startMapEffect("드래고니카를 물리쳤습니다. 포탈을 통해 이동해 주세요", 5120026);
                //broadcastMessage(MaplePacketCreator.showEffect("quest/party/clear"));
            }
        }
        if (chr.getMapId() == 240080800) {
            if (getMobsSize() == 0) {
                chr.getMap().startMapEffect("드래곤 라이더를 물리쳤습니다. 왼쪽 포탈을 통해 이동해 주세요", 5120026);
                //broadcastMessage(MaplePacketCreator.showEffect("quest/party/clear"));
            }
        }
        if (spawnedMonstersOnMap.get() == 0) {
            if (mapid / 1000 == 240080) {
                broadcastMessage(MaplePacketCreator.showEffect("quest/party/clear"));
                broadcastMessage(MaplePacketCreator.playSound("Party1/Clear"));
            }
        }
    }

    public List<MapleReactor> getAllReactor() {
        return getAllReactorsThreadsafe();
    }

    public List<MapleReactor> getAllReactorsThreadsafe() {
        ArrayList<MapleReactor> ret = new ArrayList<MapleReactor>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
            ret.add((MapleReactor) mmo);
        }
        return ret;
    }

    public List<MapleSummon> getAllSummonsThreadsafe() {
        ArrayList<MapleSummon> ret = new ArrayList<MapleSummon>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.SUMMON).values()) {
            if (mmo instanceof MapleSummon) {
                ret.add((MapleSummon) mmo);
            }
        }
        return ret;
    }

    public List<MapleMapObject> getAllDoor() {
        return getAllDoorsThreadsafe();
    }

    public List<MapleMapObject> getAllDoorsThreadsafe() {
        ArrayList<MapleMapObject> ret = new ArrayList<MapleMapObject>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.DOOR).values()) {
            if (mmo instanceof MapleDoor) {
                ret.add(mmo);
            }
        }
        return ret;
    }

    public List<MapleMapObject> getAllMechDoorsThreadsafe() {
        ArrayList<MapleMapObject> ret = new ArrayList<MapleMapObject>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.DOOR).values()) {
            if (mmo instanceof MechDoor) {
                ret.add(mmo);
            }
        }
        return ret;
    }

    public List<MapleMapObject> getAllMerchant() {
        return getAllHiredMerchantsThreadsafe();
    }

    public List<MapleMapObject> getAllHiredMerchantsThreadsafe() {
        ArrayList<MapleMapObject> ret = new ArrayList<MapleMapObject>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.HIRED_MERCHANT).values()) {
            ret.add(mmo);
        }
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.SHOP).values()) {
            ret.add(mmo);
        }
        return ret;
    }

    public List<MapleMonster> getAllMonster() {
        return getAllMonstersThreadsafe();
    }

    public List<MapleMonster> getAllMonstersThreadsafe() {
        ArrayList<MapleMonster> ret = new ArrayList<MapleMonster>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.MONSTER).values()) {
            ret.add((MapleMonster) mmo);
        }
        return ret;
    }

    public List<Integer> getAllUniqueMonsters() {
        ArrayList<Integer> ret = new ArrayList<Integer>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.MONSTER).values()) {
            final int theId = ((MapleMonster) mmo).getId();
            if (!ret.contains(theId)) {
                ret.add(theId);
            }
        }
        return ret;
    }

    public final void killAllMonsters(final boolean animate) {
        for (final MapleMapObject monstermo : getAllMonstersThreadsafe()) {
            final MapleMonster monster = (MapleMonster) monstermo;
            spawnedMonstersOnMap.decrementAndGet();
            monster.setHp(0);
            broadcastMessage(MobPacket.killMonster(monster.getObjectId(), animate ? 1 : 0));
            removeMapObject(monster);
            monster.killed();
        }
    }

    public final void killMonster(final int monsId) {
        for (final MapleMapObject mmo : getAllMonstersThreadsafe()) {
            if (((MapleMonster) mmo).getId() == monsId) {
                spawnedMonstersOnMap.decrementAndGet();
                removeMapObject(mmo);
                broadcastMessage(MobPacket.killMonster(mmo.getObjectId(), 1));
                ((MapleMonster) mmo).killed();
                break;
            }
        }
    }

    private String MapDebug_Log() {
        final StringBuilder sb = new StringBuilder("Defeat time : ");
        sb.append(FileoutputUtil.CurrentReadable_Time());

        sb.append(" | Mapid : ").append(this.mapid);

        charactersLock.readLock().lock();
        try {
            sb.append(" Users [").append(characters.size()).append("] | ");
            for (MapleCharacter mc : characters) {
                sb.append(mc.getName()).append(", ");
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        return sb.toString();
    }

    public final void limitReactor(final int rid, final int num) {
        List<MapleReactor> toDestroy = new ArrayList<MapleReactor>();
        Map<Integer, Integer> contained = new LinkedHashMap<Integer, Integer>();
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                MapleReactor mr = (MapleReactor) obj;
                if (contained.containsKey(mr.getReactorId())) {
                    if (contained.get(mr.getReactorId()) >= num) {
                        toDestroy.add(mr);
                    } else {
                        contained.put(mr.getReactorId(), contained.get(mr.getReactorId()) + 1);
                    }
                } else {
                    contained.put(mr.getReactorId(), 1);
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
        for (MapleReactor mr : toDestroy) {
            destroyReactor(mr.getObjectId());
        }
    }

    public final void destroyReactors(final int first, final int last) {
        List<MapleReactor> toDestroy = new ArrayList<MapleReactor>();
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                MapleReactor mr = (MapleReactor) obj;
                if (mr.getReactorId() >= first && mr.getReactorId() <= last) {
                    toDestroy.add(mr);
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
        for (MapleReactor mr : toDestroy) {
            destroyReactor(mr.getObjectId());
        }
    }

    public final void destroyReactor(final int oid) {
        final MapleReactor reactor = getReactorByOid(oid);
        if (reactor == null) {
            return;
        }
        broadcastMessage(MaplePacketCreator.destroyReactor(reactor));
        reactor.setAlive(false);
        removeMapObject(reactor);
        reactor.setTimerActive(false);

        if (reactor.getDelay() > 0) {
            MapTimer.getInstance().schedule(new Runnable() {

                @Override
                public final void run() {
                    respawnReactor(reactor);
                }
            }, reactor.getDelay());
        }
    }

    public final void reloadReactors() {
        List<MapleReactor> toSpawn = new ArrayList<MapleReactor>();
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                final MapleReactor reactor = (MapleReactor) obj;
                broadcastMessage(MaplePacketCreator.destroyReactor(reactor));
                reactor.setAlive(false);
                reactor.setTimerActive(false);
                toSpawn.add(reactor);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
        for (MapleReactor r : toSpawn) {
            removeMapObject(r);
            if (!r.isCustom()) { //guardians cpq
                respawnReactor(r);
            }
        }
    }

    /*
     * command to reset all item-reactors in a map to state 0 for GM/NPC use - not tested (broken reactors get removed
     * from mapobjects when destroyed) Should create instances for multiple copies of non-respawning reactors...
     */
    public final void resetReactors() {
        setReactorState((byte) 0);
    }

    public final void setReactorState() {
        setReactorState((byte) 1);
    }

    public final void setReactorState(final byte state) {
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                ((MapleReactor) obj).forceHitReactor((byte) state);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
    }

    public final void setReactorDelay(final int state) {
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                ((MapleReactor) obj).setDelay(state);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
    }

    /*
     * command to shuffle the positions of all reactors in a map for PQ purposes (such as ZPQ/LMPQ)
     */
    public final void shuffleReactors() {
        shuffleReactors(0, 9999999); //all
    }

    public final void shuffleReactors(int first, int last) {
        List<Point> points = new ArrayList<Point>();
        for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
            MapleReactor mr = (MapleReactor) obj;
            if (mr.getReactorId() >= first && mr.getReactorId() <= last && mr.getReactorId() != 2001016) { //hardcode - tower of goddess
                points.add(mr.getPosition());
            }
        }
        Collections.shuffle(points);
        for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
            MapleReactor mr = (MapleReactor) obj;
            if (mr.getReactorId() >= first && mr.getReactorId() <= last && mr.getReactorId() != 2001016) {
                mr.setPosition(points.remove(points.size() - 1));
            }
        }
    }

    /**
     * Automagically finds a new controller for the given monster from the chars
     * on the map...
     *
     * @param monster
     */
    public final void updateMonsterController(final MapleMonster monster) {
        if (!monster.isAlive() || monster.getLinkCID() > 0 || monster.getStats().isEscort()) {
            return;
        }
        if (monster.getController() != null) {
            if (monster.getController().getMap() != this || monster.getController().getTruePosition().distanceSq(monster.getTruePosition()) > monster.getRange()) {
                monster.getController().stopControllingMonster(monster);
            } else { // Everything is fine :)
                return;
            }
        }
        int mincontrolled = -1;
        MapleCharacter newController = null;

        charactersLock.readLock().lock();
        try {
            final Iterator<MapleCharacter> ltr = characters.iterator();
            MapleCharacter chr;
            while (ltr.hasNext()) {
                chr = ltr.next();
                if (!chr.isHidden() && (chr.getControlledSize() < mincontrolled || mincontrolled == -1) && chr.getTruePosition().distanceSq(monster.getTruePosition()) <= monster.getRange()) {
                    mincontrolled = chr.getControlledSize();
                    newController = chr;
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        if (newController != null) {
            if (monster.isFirstAttack()) {
                newController.controlMonster(monster, true);
                monster.setControllerHasAggro(true);
            } else {
                newController.controlMonster(monster, false);
            }
        }
    }

    public final MapleMapObject getMapObject(int oid, MapleMapObjectType type) {
        mapobjectlocks.get(type).readLock().lock();
        try {
            return mapobjects.get(type).get(oid);
        } finally {
            mapobjectlocks.get(type).readLock().unlock();
        }
    }

    public final boolean containsNPC(int npcid) {
        mapobjectlocks.get(MapleMapObjectType.NPC).readLock().lock();
        try {
            Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.NPC).values().iterator();
            while (itr.hasNext()) {
                MapleNPC n = (MapleNPC) itr.next();
                if (n.getId() == npcid) {
                    return true;
                }
            }
            return false;
        } finally {
            mapobjectlocks.get(MapleMapObjectType.NPC).readLock().unlock();
        }
    }

    public MapleNPC getNPCById(int id) {
        mapobjectlocks.get(MapleMapObjectType.NPC).readLock().lock();
        try {
            Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.NPC).values().iterator();
            while (itr.hasNext()) {
                MapleNPC n = (MapleNPC) itr.next();
                if (n.getId() == id) {
                    return n;
                }
            }
            return null;
        } finally {
            mapobjectlocks.get(MapleMapObjectType.NPC).readLock().unlock();
        }
    }

    public MapleMonster getMonsterById(int id) {
        mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().lock();
        try {
            MapleMonster ret = null;
            Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.MONSTER).values().iterator();
            while (itr.hasNext()) {
                MapleMonster n = (MapleMonster) itr.next();
                if (n.getId() == id) {
                    ret = n;
                    break;
                }
            }
            return ret;
        } finally {
            mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().unlock();
        }
    }

    public int countMonsterById(int id) {
        mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().lock();
        try {
            int ret = 0;
            Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.MONSTER).values().iterator();
            while (itr.hasNext()) {
                MapleMonster n = (MapleMonster) itr.next();
                if (n.getId() == id) {
                    ret++;
                }
            }
            return ret;
        } finally {
            mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().unlock();
        }
    }

    public MapleReactor getReactorById(int id) {
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            MapleReactor ret = null;
            Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.REACTOR).values().iterator();
            while (itr.hasNext()) {
                MapleReactor n = (MapleReactor) itr.next();
                if (n.getReactorId() == id) {
                    ret = n;
                    break;
                }
            }
            return ret;
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
    }

    /**
     * returns a monster with the given oid, if no such monster exists returns
     * null
     *
     * @param oid
     * @return
     */
    public final MapleMonster getMonsterByOid(final int oid) {
        MapleMapObject mmo = getMapObject(oid, MapleMapObjectType.MONSTER);
        if (mmo == null) {
            return null;
        }
        return (MapleMonster) mmo;
    }

    public final MapleNPC getNPCByOid(final int oid) {
        MapleMapObject mmo = getMapObject(oid, MapleMapObjectType.NPC);
        if (mmo == null) {
            return null;
        }
        return (MapleNPC) mmo;
    }

    public final MapleReactor getReactorByOid(final int oid) {
        MapleMapObject mmo = getMapObject(oid, MapleMapObjectType.REACTOR);
        if (mmo == null) {
            return null;
        }
        return (MapleReactor) mmo;
    }

    public final MapleReactor getReactorByName(final String name) {
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                MapleReactor mr = ((MapleReactor) obj);
                if (mr.getName().equalsIgnoreCase(name)) {
                    return mr;
                }
            }
            return null;
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
    }

    public final void spawnNpc(final int id, final Point pos) {
        final MapleNPC npc = MapleLifeFactory.getNPC(id);
        npc.setPosition(pos);
        npc.setCy(pos.y);
        npc.setRx0(pos.x + 50);
        npc.setRx1(pos.x - 50);
        npc.setFh(getFootholds().findBelow(pos).getId());
        npc.setCustom(true);
        addMapObject(npc);
        broadcastMessage(MaplePacketCreator.spawnNPC(npc, true));
    }

    public final void removeNpc(final int npcid) {
        mapobjectlocks.get(MapleMapObjectType.NPC).writeLock().lock();
        try {
            Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.NPC).values().iterator();
            while (itr.hasNext()) {
                MapleNPC npc = (MapleNPC) itr.next();
                if (npc.isCustom() && (npcid == -1 || npc.getId() == npcid)) {
                    broadcastMessage(MaplePacketCreator.removeNPCController(npc.getObjectId()));
                    broadcastMessage(MaplePacketCreator.removeNPC(npc.getObjectId()));
                    itr.remove();
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.NPC).writeLock().unlock();
        }
    }

    public final void hideNpc(final int npcid) {
        mapobjectlocks.get(MapleMapObjectType.NPC).readLock().lock();
        try {
            Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.NPC).values().iterator();
            while (itr.hasNext()) {
                MapleNPC npc = (MapleNPC) itr.next();
                if (npcid == -1 || npc.getId() == npcid) {
                    broadcastMessage(MaplePacketCreator.removeNPCController(npc.getObjectId()));
                    broadcastMessage(MaplePacketCreator.removeNPC(npc.getObjectId()));
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.NPC).readLock().unlock();
        }
    }

    public final void spawnReactorOnGroundBelow(final MapleReactor mob, final Point pos) {
        mob.setPosition(pos); //reactors dont need FH lol
        mob.setCustom(true);
        spawnReactor(mob);
    }

    public final void spawnMonster_sSack(final MapleMonster mob, final Point pos, final int spawnType) {
        mob.setPosition(calcPointBelow(new Point(pos.x, pos.y - 1)));
        spawnMonster(mob, spawnType);
    }

    public final void spawnMonster_Pokemon(final MapleMonster mob, final Point pos, final int spawnType) {
        final Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
        mob.setPosition(spos);
        spawnMonster(mob, spawnType, true);
    }

    public final void spawnMonsterOnGroundBelow(final MapleMonster mob, final Point pos) {
        spawnMonster_sSack(mob, pos, -2);
    }

    public final int spawnMonsterWithEffectBelow(final MapleMonster mob, final Point pos, final int effect) {
        final Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
        return spawnMonsterWithEffect(mob, effect, spos, (short) 0);
    }

    public final void spawnZakum(final int x, final int y) {
        final Point pos = new Point(x, y);
        final MapleMonster mainb = MapleLifeFactory.getMonster(8800000);
        final Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
        mainb.setPosition(spos);
        mainb.setFake(true);

        // Might be possible to use the map object for reference in future.
        spawnFakeMonster(mainb);

        final int[] zakpart = {8800003, 8800004, 8800005, 8800006, 8800007,
            8800008, 8800009, 8800010};

        for (final int i : zakpart) {
            final MapleMonster part = MapleLifeFactory.getMonster(i);
            part.setPosition(spos);

            spawnMonster(part, -2);
        }
        if (squadSchedule != null) {
            cancelSquadSchedule(false);
        }
    }

    public final void spawnChaosZakum(final int x, final int y) {
        final Point pos = new Point(x, y);
        final MapleMonster mainb = MapleLifeFactory.getMonster(8800100);
        final Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
        mainb.setPosition(spos);
        mainb.setFake(true);

        // Might be possible to use the map object for reference in future.
        spawnFakeMonster(mainb);

        final int[] zakpart = {8800103, 8800104, 8800105, 8800106, 8800107,
            8800108, 8800109, 8800110};

        for (final int i : zakpart) {
            final MapleMonster part = MapleLifeFactory.getMonster(i);
            part.setPosition(spos);

            spawnMonster(part, -2);
        }
        if (squadSchedule != null) {
            cancelSquadSchedule(false);
        }
    }

    public final void spawnFakeMonsterOnGroundBelow(final MapleMonster mob, final Point pos) {
        Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
        spos.y -= 1;
        mob.setPosition(spos);
        spawnFakeMonster(mob);
    }

    private void checkRemoveAfter(final MapleMonster monster) {
        final int ra = monster.getStats().getRemoveAfter();

        if (ra > 0 && monster.getLinkCID() <= 0) {
            monster.registerKill(ra * 1000);
        }
    }

    public final void spawnRevives(final MapleMonster monster, final int oid) {
        monster.setMap(this);
        checkRemoveAfter(monster);
        monster.setLinkOid(oid);
        spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {

            @Override
            public final void sendPackets(MapleClient c) {
                c.getSession().write(MobPacket.spawnMonster(monster, monster.getStats().getSummonType() <= 1 ? -3 : monster.getStats().getSummonType(), oid)); // TODO effect
            }
        });
        updateMonsterController(monster);

        spawnedMonstersOnMap.incrementAndGet();
    }

    public final void spawnMonster(final MapleMonster monster, final int spawnType) {
        spawnMonster(monster, spawnType, false);
    }

    public final void spawnMonster(final MapleMonster monster, final int spawnType, final boolean overwrite) {
        monster.setMap(this);
        checkRemoveAfter(monster);

        spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {

            public final void sendPackets(MapleClient c) {
                c.getSession().write(MobPacket.spawnMonster(monster, monster.getStats().getSummonType() <= 1 || monster.getStats().getSummonType() == 27 || overwrite ? spawnType : monster.getStats().getSummonType(), 0));
            }
        });
        updateMonsterController(monster);

        spawnedMonstersOnMap.incrementAndGet();
    }

    public final int spawnMonsterWithEffect(final MapleMonster monster, final int effect, Point pos, short tDelay) {
        try {
            monster.setMap(this);
            monster.setPosition(pos);

            spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {

                @Override
                public final void sendPackets(MapleClient c) {
                    c.getSession().write(MobPacket.spawnMonster(monster, effect, tDelay));
                }
            });
            updateMonsterController(monster);

            spawnedMonstersOnMap.incrementAndGet();
            return monster.getObjectId();
        } catch (Exception e) {
            return -1;
        }
    }

    public final void spawnFakeMonster(final MapleMonster monster) {
        monster.setMap(this);
        monster.setFake(true);

        spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {

            @Override
            public final void sendPackets(MapleClient c) {
                c.getSession().write(MobPacket.spawnMonster(monster, -4, 0));
            }
        });
        updateMonsterController(monster);

        spawnedMonstersOnMap.incrementAndGet();
    }

    public final void spawnReactor(final MapleReactor reactor) {
        reactor.setMap(this);

        spawnAndAddRangedMapObject(reactor, new DelayedPacketCreation() {

            @Override
            public final void sendPackets(MapleClient c) {
                c.getSession().write(MaplePacketCreator.spawnReactor(reactor));
            }
        });
    }

    private void respawnReactor(final MapleReactor reactor) {
        reactor.setState((byte) 0);
        reactor.setAlive(true);
        spawnReactor(reactor);
    }

    public void spawnMessageBox(final MapleMessageBox msgbox) {
        spawnAndAddRangedMapObject(msgbox, new DelayedPacketCreation() {
            @Override
            public void sendPackets(MapleClient c) {
                msgbox.sendSpawnData(c);
            }
        });
    }

    public final void spawnDoor(final MapleDoor door) {
        spawnAndAddRangedMapObject(door, new DelayedPacketCreation() {

            public final void sendPackets(MapleClient c) {
                door.sendSpawnData(c);
                c.getSession().write(MaplePacketCreator.enableActions());
            }
        });
    }

    public final void spawnMechDoor(final MechDoor door) {
        spawnAndAddRangedMapObject(door, new DelayedPacketCreation() {

            public final void sendPackets(MapleClient c) {
                c.getSession().write(MaplePacketCreator.spawnMechDoor(door, true));
                c.getSession().write(MaplePacketCreator.enableActions());
            }
        });

        if (mechaDoorTimer != null) {
            mechaDoorTimer.cancel(true);
            mechaDoorTimer = null;
        }

        mechaDoorTimer = MapTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                if (door != null) {
                    broadcastMessage(MaplePacketCreator.removeMechDoor(door, true));
                    removeMapObject(door);
                }
            }
        }, door.getDuration());
    }

    public final void spawnSummon(final MapleSummon summon) {
        summon.updateMap(this);
        spawnAndAddRangedMapObject(summon, new DelayedPacketCreation() {
            @Override
            public void sendPackets(MapleClient c) {
                if (summon != null && c.getPlayer() != null && (!summon.isChangedMap() || summon.getOwnerId() == c.getPlayer().getId())) {
                    c.getSession().write(MaplePacketCreator.spawnSummon(summon, true));
                    MapTimer.getInstance().schedule(new Runnable() {
                        @Override
                        public void run() {
                            if (summon.isReaper()) {
                                broadcastMessage(MaplePacketCreator.removeSummon(summon, true));
                                removeMapObject(summon);
                                c.getPlayer().removeSummon(summon);
                            }
                        }
                    }, (1000 * 10) + ((int) (summon.getSkillLevel()) / 2));

                }
            }
        });
    }

    public final void spawnExtractor(final MapleExtractor ex) {
        spawnAndAddRangedMapObject(ex, new DelayedPacketCreation() {

            @Override
            public void sendPackets(MapleClient c) {
                ex.sendSpawnData(c);
            }
        });
    }

    public final void spawnMist(final MapleMist mist, final int duration, boolean poison, boolean fake) {
        spawnAndAddRangedMapObject(mist, new DelayedPacketCreation() {

            @Override
            public void sendPackets(MapleClient c) {
                mist.sendSpawnData(c);
            }
        });

        final MapTimer tMan = MapTimer.getInstance();
        final ScheduledFuture<?> poisonSchedule;
        final ScheduledFuture<?> statusSchedule;
        if (poison) {
            final MapleCharacter owner = getCharacterById(mist.getOwnerId());
            poisonSchedule = tMan.register(new Runnable() {
                @Override
                public void run() {
                    for (final MapleMapObject mo : getMapObjectsInRect(mist.getBox(), Collections.singletonList(MapleMapObjectType.MONSTER))) {
                        if (mist.makeChanceResult() && !((MapleMonster) mo).isBuffed(MonsterStatus.POISON)) { //  && !((MapleMonster) mo).isBuffed(MonsterStatus.POISON)
                            ((MapleMonster) mo).applyStatus(owner, new MonsterStatusEffect(MonsterStatus.POISON, 1, mist.getSourceSkill().getId(), null, false), true, duration, true, mist.getSource());
                        }
                    }
                }
            }, 2000, 2500);
        } else {
            poisonSchedule = null;
        }
        if (mist.getSourceSkill().getId() == 22161003) {
            statusSchedule = tMan.register(new Runnable() {
                @Override
                public void run() {
                    for (final MapleMapObject mo : getMapObjectsInRect(mist.getBox(), Collections.singletonList(MapleMapObjectType.PLAYER))) {
                        if (mist.makeChanceResult()) {
                            final MapleCharacter chr = ((MapleCharacter) mo);
                            chr.addMP((int) (mist.getSource().getX() * (chr.getStat().getMaxMp() / 100.0)));
                        }
                    }
                }
            }, 2000, 2500);
        } else {
            statusSchedule = null;
        }
        mist.setPoisonSchedule(poisonSchedule);
        mist.setSchedule(tMan.schedule(new Runnable() {
            @Override
            public void run() {
                broadcastMessage(MaplePacketCreator.removeMist(mist.getObjectId(), false));
                removeMapObject(mist);
                if (poisonSchedule != null) {
                    poisonSchedule.cancel(false);
                }
                if (statusSchedule != null) {
                    statusSchedule.cancel(true);
                }
            }
        }, duration));
    }

    public final void disappearingItemDrop(final MapleMapObject dropper, final MapleCharacter owner, final Item item, final Point pos) {
        final Point droppos = calcDropPos(pos, pos);
        final MapleMapItem drop = new MapleMapItem(item, droppos, dropper, owner, (byte) 1, false);
        broadcastMessage(MaplePacketCreator.dropItemFromMapObject(drop, dropper.getPosition(), droppos, (byte) 3), drop.getPosition());
    }

    public final void spawnMesoDrop(final int meso, final Point position, final MapleMapObject dropper, final MapleCharacter owner, final boolean playerDrop, final byte droptype) {
        final Point droppos = calcDropPos(position, position);
        final MapleMapItem mdrop = new MapleMapItem(meso, droppos, dropper, owner, droptype, playerDrop);

        spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {

            @Override
            public void sendPackets(MapleClient c) {
                c.getSession().write(MaplePacketCreator.dropItemFromMapObject(mdrop, dropper.getPosition(), droppos, (byte) 1));
            }
        });
        if (!everlast) {
            mdrop.registerExpire(120000);
            if (droptype == 0 || droptype == 1) {
                mdrop.registerFFA(30000);
            }
        }
    }

    public final void spawnMobMesoDrop(final int meso, final Point position, final MapleMapObject dropper, final MapleCharacter owner, final boolean playerDrop, final byte droptype) {
        spawnMobMesoDrop(meso, position, dropper, owner, playerDrop, droptype, false);
    }

    public final void spawnMobMesoDrop(final int meso, final Point position, final MapleMapObject dropper, final MapleCharacter owner, final boolean playerDrop, final byte droptype, final boolean isGainDrop) {
        if (getId() / 10000000 == 19) {
            return;
        }
        final MapleMapItem mdrop = new MapleMapItem(meso, position, dropper, owner, droptype, playerDrop);

        boolean check = false;
//        if (owner.getPet(0) != null) {
        if (owner.getLevel() >= 10 && !isGainDrop) { //레벨 10이상 개별드롭 몹이 아닐 때만 오토루팅 가능
            check = InventoryHandler.autoLoot23(owner, mdrop);
        }

        if (!check) {

            spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {
                @Override
                public void sendPackets(MapleClient c) {
                    if (!isGainDrop || (isGainDrop && c.getPlayer().getId() == owner.getId())) {
                        c.getSession().write(MaplePacketCreator.dropItemFromMapObject(mdrop, dropper.getTruePosition(), position, (byte) 1));
                    }
                }
            });
            mdrop.registerExpire(120000);
            if (droptype == 0 || droptype == 1) {
                mdrop.registerFFA(30000);
            }
        }

    }

    public final void spawnMobDrop(final Item idrop, final Point dropPos, final MapleMonster mob, final MapleCharacter chr, final byte droptype, final int questid) {
        spawnMobDrop(idrop, dropPos, mob, chr, droptype, questid, false);
    }

    public final void spawnMobDrop(final Item idrop, final Point dropPos, final MapleMonster mob, final MapleCharacter chr, final byte droptype, final int questid, final boolean isGainDrop) {
        if (getId() / 10000000 == 19) {
            return;
        }
        final MapleMapItem mdrop = new MapleMapItem(idrop, dropPos, mob, chr, droptype, false, questid);

        boolean check = false;
//        if (chr.getPet(0) != null) {
        if (chr.getLevel() >= 10 && !isGainDrop) { //레벨 10이상 개별드롭 몹이 아닐 때만 오토루팅 가능
            check = InventoryHandler.autoLoot23(chr, mdrop);
        }

        if (idrop.getItemId() == 4001161) {
            chr.dropMessage(5, "희석된 독을 획득 하셨습니다. 가시 덤불을 제거 해주세요.");
            MapleInventoryManipulator.addById(chr.getClient(), 4001162, (short) 1, null);
            return;
        }
        if (!check) {
            spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {

                @Override
                public void sendPackets(MapleClient c) {
                    if (c != null && c.getPlayer() != null && (questid <= 0 || c.getPlayer().getQuestStatus(questid) == 1) && mob != null && dropPos != null) {
                        if (!isGainDrop || (isGainDrop && c.getPlayer().getId() == chr.getId())) {
                            c.getSession().write(MaplePacketCreator.dropItemFromMapObject(mdrop, mob.getTruePosition(), dropPos, (byte) 1));
                        }
                    }
                }
            });
//	broadcastMessage(MaplePacketCreator.dropItemFromMapObject(mdrop, mob.getTruePosition(), dropPos, (byte) 0));
        }

        mdrop.registerExpire(120000);
        if (droptype == 0 || droptype == 1) {
            mdrop.registerFFA(30000);
        }
        activateItemReactors(mdrop, chr.getClient());
    }

    public final void spawnRandDrop() {
        if (mapid != 910000000 || channel != 1) {
            return; //fm, ch1
        }

        for (MapleMapObject o : mapobjects.get(MapleMapObjectType.ITEM).values()) {
            if (((MapleMapItem) o).isRandDrop()) {
                return;
            }
        }
        MapTimer.getInstance().schedule(new Runnable() {
            public void run() {
                final Point pos = new Point(Randomizer.nextInt(800) + 531, -806);
                final int theItem = Randomizer.nextInt(1000);
                int itemid = 0;
                if (theItem < 950) { //0-949 = normal, 950-989 = rare, 990-999 = super
                    itemid = GameConstants.normalDrops[Randomizer.nextInt(GameConstants.normalDrops.length)];
                } else if (theItem < 990) {
                    itemid = GameConstants.rareDrops[Randomizer.nextInt(GameConstants.rareDrops.length)];
                } else {
                    itemid = GameConstants.superDrops[Randomizer.nextInt(GameConstants.superDrops.length)];
                }
                spawnAutoDrop(itemid, pos);
            }
        }, 20000);
    }

    public final void spawnAutoDrop(final int itemid, final Point pos) {
        Item idrop = null;
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (GameConstants.getInventoryType(itemid) == MapleInventoryType.EQUIP) {
            idrop = ii.randomizeStats((Equip) ii.getEquipById(itemid));
        } else {
            idrop = new Item(itemid, (byte) 0, (short) 1, (byte) 0);
        }
        idrop.setGMLog("Dropped from auto " + " on " + mapid);
        final MapleMapItem mdrop = new MapleMapItem(pos, idrop);
        spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {
            @Override
            public void sendPackets(MapleClient c) {
                c.getSession().write(MaplePacketCreator.dropItemFromMapObject(mdrop, pos, pos, (byte) 1));
            }
        });
        broadcastMessage(MaplePacketCreator.dropItemFromMapObject(mdrop, pos, pos, (byte) 0));
        if (itemid == 4001101) {
            mdrop.registerExpire(6000);
        } else if (itemid / 10000 != 291) {
            mdrop.registerExpire(120000);
        }
    }

    public final void spawnAutoDrop(final int itemid, final Point pos, final Point pos2) {
        Item idrop = null;
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (GameConstants.getInventoryType(itemid) == MapleInventoryType.EQUIP) {
            idrop = ii.randomizeStats((Equip) ii.getEquipById(itemid));
        } else {
            idrop = new Item(itemid, (byte) 0, (short) 1, (byte) 0);
        }
        idrop.setGMLog("Dropped from auto " + " on " + mapid);
        final MapleMapItem mdrop = new MapleMapItem(pos, idrop);
        spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {
            @Override
            public void sendPackets(MapleClient c) {
                c.getSession().write(MaplePacketCreator.dropItemFromMapObject(mdrop, pos2, pos, (byte) 1));
            }
        });
        broadcastMessage(MaplePacketCreator.dropItemFromMapObject(mdrop, pos2, pos, (byte) 0));
        if (itemid == 4001101) {
            mdrop.registerExpire(6000);
        } else if (itemid / 10000 != 291) {
            mdrop.registerExpire(120000);
        }
    }

    public final void spawnItemDrop(final MapleMapObject dropper, final MapleCharacter owner, final Item item, Point pos, final boolean ffaDrop, final boolean playerDrop) {
        final Point droppos = calcDropPos(pos, pos);
        final MapleMapItem drop = new MapleMapItem(item, droppos, dropper, owner, (byte) 2, playerDrop);

        spawnAndAddRangedMapObject(drop, new DelayedPacketCreation() {

            @Override
            public void sendPackets(MapleClient c) {
                c.getSession().write(MaplePacketCreator.dropItemFromMapObject(drop, dropper.getPosition(), droppos, (byte) 1));
            }
        });
        broadcastMessage(MaplePacketCreator.dropItemFromMapObject(drop, dropper.getPosition(), droppos, (byte) 0));

        if (!everlast) {
            drop.registerExpire(120000);
            activateItemReactors(drop, owner.getClient());
        }
    }

    private void activateItemReactors(final MapleMapItem drop, final MapleClient c) {
        final Item item = drop.getItem();

        for (final MapleMapObject o : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
            final MapleReactor react = (MapleReactor) o;
            /*if (react.getReactorId() == 2006000) {
             if (react.getArea().contains(drop.getTruePosition())) {
             if (item.getItemId() == 4001063 && item.getQuantity() == 20) {
             MapTimer.getInstance().schedule(new Runnable() {
             @Override
             public void run() {
             List<MapleMapItem> items = getAllItemsThreadsafe();
             for (MapleMapItem i : items) {
             if (i.getItemId() == 4001063) {
             if (i.getItem().getQuantity() == 20) { // 오르비스 리엑터
             i.expire(c.getPlayer().getMap());
             react.forceStartReactor(c);
             }
             }
             }
             }
             }, 3500);
             }
             }
             }*/
            if (react.getReactorType() == 100) {
                boolean canActivate = false;
                //hardcode cause too lazy (...)
                if (react.getReactorId() == 2008006) { //OrbisPQ
                    canActivate = (item.getItemId() >= 4001056 && item.getItemId() <= 4001062) && react.getReactItem().getRight() == item.getQuantity();
                } else if (react.getReactorId() == 2408002) { //HorntailPQ
                    canActivate = (item.getItemId() >= 4001088 && item.getItemId() <= 4001091) && react.getReactItem().getRight() == item.getQuantity();
                } else { //Default
                    canActivate = item.getItemId() == react.getReactItem().getLeft() && react.getReactItem().getRight() == item.getQuantity();
                }
                if (canActivate) {
                    if (react.getArea().contains(drop.getTruePosition())) {
                        if (!react.isTimerActive()) {
                            MapTimer.getInstance().schedule(new ActivateItemReactor(drop, react, c), 5000);
                            react.setTimerActive(true);
                            break;
                        }
                    }
                }
            }
        }
    }

    public int getItemsSize() {
        return mapobjects.get(MapleMapObjectType.ITEM).size();
    }

    public int getMessageBoxSize() {
        return mapobjects.get(MapleMapObjectType.MESSAGEBOX).size();
    }

    public int getExtractorSize() {
        return mapobjects.get(MapleMapObjectType.EXTRACTOR).size();
    }

    public int getMobsSize() {
        return mapobjects.get(MapleMapObjectType.MONSTER).size();
    }

    public List<MapleMapItem> getAllItems() {
        return getAllItemsThreadsafe();
    }

    public List<MapleMapItem> getAllItemsThreadsafe() {
        ArrayList<MapleMapItem> ret = new ArrayList<MapleMapItem>();
        mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.ITEM).values()) {
                ret.add((MapleMapItem) mmo);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().unlock();
        }
        return ret;
    }

    public List<MapleMessageBox> getAllMsgBoxesThreadsafe() {
        ArrayList<MapleMessageBox> ret = new ArrayList<MapleMessageBox>();
        mapobjectlocks.get(MapleMapObjectType.MESSAGEBOX).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.MESSAGEBOX).values()) {
                ret.add((MapleMessageBox) mmo);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.MESSAGEBOX).readLock().unlock();
        }
        return ret;
    }

    public Point getPointOfItem(int itemid) {
        mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.ITEM).values()) {
                MapleMapItem mm = ((MapleMapItem) mmo);
                if (mm.getItem() != null && mm.getItem().getItemId() == itemid) {
                    return mm.getPosition();
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().unlock();
        }
        return null;
    }

    public List<MapleMist> getAllMistsThreadsafe() {
        ArrayList<MapleMist> ret = new ArrayList<MapleMist>();
        mapobjectlocks.get(MapleMapObjectType.MIST).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.MIST).values()) {
                ret.add((MapleMist) mmo);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.MIST).readLock().unlock();
        }
        return ret;
    }

    public final void returnEverLastItem(final MapleCharacter chr) {
        for (final MapleMapObject o : getAllItemsThreadsafe()) {
            final MapleMapItem item = ((MapleMapItem) o);
            if (item.getOwner() == chr.getId()) {
                item.setPickedUp(true);
                broadcastMessage(MaplePacketCreator.removeItemFromMap(item.getObjectId(), 2, chr.getId()), item.getTruePosition());
                if (item.getMeso() > 0) {
                    chr.gainMeso(item.getMeso(), false);
                } else {
                    MapleInventoryManipulator.addFromDrop(chr.getClient(), item.getItem(), false);
                }
                removeMapObject(item);
            }
        }
        spawnRandDrop();
    }

    public final void talkMonster(final String msg, final int itemId, final int objectid) {
        if (itemId > 0) {
            startMapEffect(msg, itemId, false);
        }
        broadcastMessage(MobPacket.talkMonster(objectid, itemId, msg)); //5120035
        broadcastMessage(MobPacket.removeTalkMonster(objectid));
    }

    public final void startMapEffect(final String msg, final int itemId) {
        startMapEffect(msg, itemId, false);
    }

    public final void startMapEffect(final String msg, final int itemId, final boolean jukebox) {
        if (mapEffect != null) {
            return;
        }
        mapEffect = new MapleMapEffect(msg, itemId);
        mapEffect.setJukebox(jukebox);
        broadcastMessage(mapEffect.makeStartData());
        MapTimer.getInstance().schedule(new Runnable() {

            @Override
            public void run() {
                if (mapEffect != null) {
                    broadcastMessage(mapEffect.makeDestroyData());
                    mapEffect = null;
                }
            }
        }, jukebox ? 300000 : 30000);
        try {
            if (itemId == 5120010) { //경험치 뿌리기 
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                        if (chr.getMapId() == this.mapid) {
                            SkillFactory.getSkill(9001008).getEffect(1).applyTo(chr, 30000 * 60);
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    public final void startExtendedMapEffect(final String msg, final int itemId) {
        broadcastMessage(MaplePacketCreator.startMapEffect(msg, itemId, true));
        MapTimer.getInstance().schedule(new Runnable() {

            @Override
            public void run() {
                broadcastMessage(MaplePacketCreator.removeMapEffect());
                broadcastMessage(MaplePacketCreator.startMapEffect(msg, itemId, false));
                //dont remove mapeffect.
            }
        }, 60000);
    }

    public final void startSimpleMapEffect(final String msg, final int itemId) {
        broadcastMessage(MaplePacketCreator.startMapEffect(msg, itemId, true));
    }

    public final void startJukebox(final String msg, final int itemId) {
        startMapEffect(msg, itemId, true);
    }

    private boolean onFirstUserEnterScriptRunned = false;

    public final void addPlayer(final MapleCharacter chr) {
        mapobjectlocks.get(MapleMapObjectType.PLAYER).writeLock().lock();
        try {
            mapobjects.get(MapleMapObjectType.PLAYER).put(chr.getObjectId(), chr);
        } finally {
            mapobjectlocks.get(MapleMapObjectType.PLAYER).writeLock().unlock();
        }

        charactersLock.writeLock().lock();
        try {
            characters.add(chr);
        } finally {
            charactersLock.writeLock().unlock();
        }
        chr.setChangeTime();
        if (GameConstants.isTeamMap(mapid) && !chr.inPVP()) {
            chr.setTeam(getAndSwitchTeam() ? 0 : 1);
        }
        final byte[] packet = MaplePacketCreator.spawnPlayerMapobject(chr);
        if (!chr.isHidden()) {
            broadcastMessage(packet);
            if (chr.isIntern() && speedRunStart > 0) {
                endSpeedRun();
                //broadcastMessage(MaplePacketCreator.serverNotice(5, "The speed run has ended."));
            }
        } else {
            broadcastGMMessage(chr, packet, false);
        }
        switch (chr.getMapId()) {
            case 240080100:
                chr.getMap().startMapEffect("플라잉 호크를 퇴치하라!", 5120026);
                break;
            case 240080200:
                chr.getMap().startMapEffect("플라잉 이글을 퇴치하라!", 5120026);
                break;
            case 240080300:
                chr.getMap().startMapEffect("플라잉 레드 와이번과 플라잉 블루 와이번을 퇴치하라!", 5120026);
                break;
            case 240080400:
                chr.getMap().startMapEffect("모든 몬스터를 퇴치하라!", 5120026);
                break;
            case 240080500:
                chr.getMap().startMapEffect("모든 몬스터를 퇴치하라!", 5120026);
                break;
            case 240080600:
                chr.getMap().startMapEffect("드래고니카를 무찌르고, 천공의 둥지로 진입하라!", 5120026);
                break;
            case 240080700:
                chr.getMap().startMapEffect("제한시간 내에 장애물을 돌파하고, 천공의 둥지로 진입하라!", 5120026);
                break;
            case 240080800:
                chr.getMap().startMapEffect("미나르 마을을 괴롭히는 드래곤 라이더를 무찔러라!", 5120026);
                break;
        }
        if (!onFirstUserEnter.equals("")) {
            if (getCharactersSize() == 1) {
                MapScriptMethods.startScript_FirstUser(chr.getClient(), onFirstUserEnter);
            }
        }
        sendObjectPlacement(chr);

        chr.getClient().getSession().write(packet);

        if (!onUserEnter.equals("")) {
            MapScriptMethods.startScript_User(chr.getClient(), onUserEnter);
        } else if (mapid >= 952000000 && mapid <= 954050515) {
            onFirstUserEnterScriptRunned = true;
            MapScriptMethods.startScript_FirstUser(chr.getClient(), "mpark_mobRegen");
        }
        GameConstants.achievementRatio(chr.getClient());
        //  chr.getClient().getSession().write(MaplePacketCreator.spawnFlags(nodes.getFlags()));
        if (GameConstants.isTeamMap(mapid) && !chr.inPVP()) {
            chr.getClient().getSession().write(MaplePacketCreator.showEquipEffect(chr.getTeam()));
        }
        switch (mapid) {
            case 809000101:
            case 809000201:
                chr.getClient().getSession().write(MaplePacketCreator.showEquipEffect());
                break;
        }
        if (chr.cubeitemid > 0) {
            chr.cubeitemid = -1;
        }
        for (final MaplePet pet : chr.getPets()) {
            if (pet.getSummoned()) {
                broadcastMessage(chr, PetPacket.showPet(chr, pet, false, false), false);
                // chr.dropMessage(6, "스폰펫 작동함 : ");
                //chr.getClient().sendPacket(PetPacket.loadPetPickupExceptionList(chr.getId(), pet.getUniqueId(), pet.getPickupExceptionList()));//펫제외아이템
                chr.getClient().sendPacket(PetPacket.loadPetPickupExceptionList(chr.getId(), pet.getUniqueId(), pet.getPickupExceptionList(), (byte) (pet.getSummonedValue() - 1)));//1126 펫제외 파끝팅해결
            }
        }
        if (chr.getAndroid() != null) {
            chr.getAndroid().setPos(chr.getPosition());
            broadcastMessage(MaplePacketCreator.spawnAndroid(chr, chr.getAndroid()));
        }
        if (chr.getParty() != null) {
            chr.silentPartyUpdate();
            chr.getClient().getSession().write(MaplePacketCreator.updateParty(chr.getClient().getChannel(), chr.getParty(), PartyOperation.SILENT_UPDATE, null));
            chr.updatePartyMemberHP();
            chr.receivePartyMemberHP();
        }
        final List<MapleSummon> ss = chr.getSummonsReadLock();
        try {
            for (MapleSummon summon : ss) {
                summon.setPosition(chr.getTruePosition());
                chr.addVisibleMapObject(summon);
                this.spawnSummon(summon);
            }
        } finally {
            chr.unlockSummonsReadLock();
        }
        if (mapEffect != null) {
            mapEffect.sendStartData(chr.getClient());
        }
        if (timeLimit > 0 && getForcedReturnMap() != null) {
            chr.startMapTimeLimitTask(timeLimit, getForcedReturnMap());
        }
        if (chr.getBuffedValue(MapleBuffStat.MONSTER_RIDING) != null && !GameConstants.isResist(chr.getJob())) {
            if (FieldLimitType.Mount.check(fieldLimit)) {
                chr.cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
            }
        }
        if (chr.getSidekick() != null) {
            MapleCharacter side = getCharacterById(chr.getSidekick().getCharacter(chr.getSidekick().getCharacter(0).getId() == chr.getId() ? 1 : 0).getId());
            if (side != null) {
                chr.getSidekick().applyBuff(side);
                chr.getSidekick().applyBuff(chr);
            }
        }
        if (chr.getEventInstance() != null && chr.getEventInstance().isTimerStarted()) {
            if (chr.inPVP()) {
                chr.getClient().getSession().write(MaplePacketCreator.getPVPClock(Integer.parseInt(chr.getEventInstance().getProperty("type")), (int) (chr.getEventInstance().getTimeLeft() / 1000)));
            } else {
                chr.getClient().getSession().write(MaplePacketCreator.getClock((int) (chr.getEventInstance().getTimeLeft() / 1000)));
            }
        }
        if (hasClock()) {
            final Calendar cal = Calendar.getInstance();
            chr.getClient().getSession().write((MaplePacketCreator.getClockTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND))));
        }
        if (chr.getCarnivalParty() != null && chr.getEventInstance() != null) {
            chr.getEventInstance().onMapLoad(chr);
        }
        MapleEvent.mapLoad(chr, channel);
        if (getSquadBegin() != null && getSquadBegin().getTimeLeft() > 0 && getSquadBegin().getStatus() == 1) {
            chr.getClient().getSession().write(MaplePacketCreator.getClock((int) (getSquadBegin().getTimeLeft() / 1000)));
        }
        if (mapid / 1000 != 105100 && mapid / 100 != 8020003 && mapid / 100 != 8020008 && mapid != 271040100) { //no boss_balrog/2095/coreblaze/auf/cygnus. but coreblaze/auf/cygnus does AFTER
            final MapleSquad sqd = getSquadByMap(); //for all squads
            final EventManager em = getEMByMap();
            if (!squadTimer && sqd != null && chr.getName().equals(sqd.getLeaderName()) && em != null && em.getProperty("leader") != null && em.getProperty("leader").equals("true") && checkStates) {
                //leader? display
                doShrine(false);
                squadTimer = true;
            }
        }
        if (chr.getSkillLevel(1074) > 0) { // 
            long now = System.currentTimeMillis();
            if (chr.getMap().canSkill(now)) {
                if (!chr.skillisCooling(1074)) {
                    chr.getClient().sendPacket(MaplePacketCreator.skillCooldown(1074, (int) 0));
                }
            }

            long coolTime = (chr.getMap().getSkill() - System.currentTimeMillis()) / 1000;
            if (coolTime > 0) {
                chr.dropMessage(6, "아직 귀신의 기운이 맵에 떠돌아 다닙니다. 남은 시간 : " + coolTime + "초");
            }
        }
        chr.cancelFishing(); //낚시꺼짐
        if (getNumMonsters() > 0 && (mapid == 280030001 || mapid == 240060201 || mapid == 280030000 || mapid == 240060200 || mapid == 220080001 || mapid == 541020800 || mapid == 541010100)) {
            String music = "Bgm09/TimeAttack";
            switch (mapid) {
                case 240060200:
                case 240060201:
                    music = "Bgm14/HonTale";
                    break;
                case 280030000:
                case 280030001:
                    music = "musicChange";
                    break;
            }
            chr.getClient().getSession().write(MaplePacketCreator.musicChange(music));
            //maybe timer too for zak/ht
        }
        if (mapid == 914000000 || mapid == 927000000) {
            chr.getClient().getSession().write(MaplePacketCreator.temporaryStats_Aran());
        } else if (mapid == 105100300 && chr.getLevel() >= 91) {
            chr.getClient().getSession().write(MaplePacketCreator.temporaryStats_Balrog(chr));
        } else if (mapid == 140090000 || mapid == 105100301 || mapid == 105100401 || mapid == 105100100) {
            chr.getClient().getSession().write(MaplePacketCreator.temporaryStats_Reset());
        }
        if (GameConstants.isEvan(chr.getJob()) && chr.getJob() >= 2200) {
            if (chr.getDragon() == null) {
                chr.makeDragon();
            } else {
                chr.getDragon().setPosition(chr.getPosition());
            }
            if (chr.getDragon() != null) {
                broadcastMessage(MaplePacketCreator.spawnDragon(chr.getDragon()));
            }
        }
        if (permanentWeather > 0) {
            chr.getClient().getSession().write(MaplePacketCreator.startMapEffect("", permanentWeather, false)); //snow, no msg
        }
        if (getPlatforms().size() > 0) {
            chr.getClient().getSession().write(MaplePacketCreator.getMovingPlatforms(this));
        }
        if (environment.size() > 0) {
            chr.getClient().getSession().write(MaplePacketCreator.getUpdateEnvironment(this));
        }
        if (isTown()) {
            chr.cancelEffectFromBuffStat(MapleBuffStat.RAINING_MINES);
        }
        if (!canSoar()) {
            chr.cancelEffectFromBuffStat(MapleBuffStat.SOARING);
        }
        if (chr.getJob() < 3200 || chr.getJob() > 3212) {
            chr.cancelEffectFromBuffStat(MapleBuffStat.AURA);
        }
        recalcCanSpawnMobs();
    }

    public int getNumItems() {
        mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().lock();
        try {
            return mapobjects.get(MapleMapObjectType.ITEM).size();
        } finally {
            mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().unlock();
        }
    }

    public int getNumMonsters() {
        mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().lock();
        try {
            return mapobjects.get(MapleMapObjectType.MONSTER).size();
        } finally {
            mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().unlock();
        }
    }

    public void doShrine(final boolean spawned) { //false = entering map, true = defeated
        if (squadSchedule != null) {
            cancelSquadSchedule(true);
        }
        final MapleSquad sqd = getSquadByMap();
        if (sqd == null) {
            return;
        }
        final int mode = (mapid == 280030000 ? 1 : (mapid == 280030001 ? 2 : (mapid == 240060200 || mapid == 240060201 ? 3 : 0)));
        //chaos_horntail message for horntail too because it looks nicer
        final EventManager em = getEMByMap();
        if (sqd != null && em != null && getCharactersSize() > 0) {
            final String leaderName = sqd.getLeaderName();
            final String state = em.getProperty("state");
            final Runnable run;
            MapleMap returnMapa = getForcedReturnMap();
            if (returnMapa == null || returnMapa.getId() == mapid) {
                returnMapa = getReturnMap();
            }
            if (spawned) { //both of these together dont go well
                broadcastMessage(MaplePacketCreator.getClock(300)); //5 min
            }
            final MapleMap returnMapz = returnMapa;
            if (!spawned) { //no monsters yet; inforce timer to spawn it quickly
                final List<MapleMonster> monsterz = getAllMonstersThreadsafe();
                final List<Integer> monsteridz = new ArrayList<Integer>();
                for (MapleMapObject m : monsterz) {
                    monsteridz.add(m.getObjectId());
                }
                run = new Runnable() {

                    public void run() {
                        final MapleSquad sqnow = MapleMap.this.getSquadByMap();
                        if (MapleMap.this.getCharactersSize() > 0 && MapleMap.this.getNumMonsters() == monsterz.size() && sqnow != null && sqnow.getStatus() == 2 && sqnow.getLeaderName().equals(leaderName) && MapleMap.this.getEMByMap().getProperty("state").equals(state)) {
                            boolean passed = monsterz.isEmpty();
                            for (MapleMapObject m : MapleMap.this.getAllMonstersThreadsafe()) {
                                for (int i : monsteridz) {
                                    if (m.getObjectId() == i) {
                                        passed = true;
                                        break;
                                    }
                                }
                                if (passed) {
                                    break;
                                } //even one of the monsters is the same
                            }
                            if (passed) {
                                //are we still the same squad? are monsters still == 0?

                                for (MapleCharacter chr : MapleMap.this.getCharactersThreadsafe()) { //warp all in map
                                    //chr.getClient().getSession().write(packet);
                                    chr.changeMap(returnMapz, returnMapz.getPortal(0)); //hopefully event will still take care of everything once warp out
                                }
                                checkStates("");
                                resetFully();
                            }
                        }

                    }
                };
            } else { //inforce timer to gtfo
                run = new Runnable() {

                    public void run() {
                        MapleSquad sqnow = MapleMap.this.getSquadByMap();
                        //we dont need to stop clock here because they're getting warped out anyway
                        if (MapleMap.this.getCharactersSize() > 0 && sqnow != null && sqnow.getStatus() == 2 && sqnow.getLeaderName().equals(leaderName) && MapleMap.this.getEMByMap().getProperty("state").equals(state)) {
                            //are we still the same squad? monsters however don't count
                            for (MapleCharacter chr : MapleMap.this.getCharactersThreadsafe()) { //warp all in map
                                //chr.getClient().getSession().write(packet);
                                chr.changeMap(returnMapz, returnMapz.getPortal(0)); //hopefully event will still take care of everything once warp out
                            }
                            checkStates("");
                            resetFully();
                        }
                    }
                };
            }
            squadSchedule = MapTimer.getInstance().schedule(run, 300000); //5 mins
        }
    }

    public final MapleSquad getSquadByMap() {
        MapleSquadType zz = null;
        switch (mapid) {
            case 105100400:
            case 105100300:
                zz = MapleSquadType.bossbalrog;
                break;
            case 280030000:
                zz = MapleSquadType.zak;
                break;
            case 280030001:
                zz = MapleSquadType.chaoszak;
                break;
            case 240060000:
            case 240060100:
            case 240060200:
                zz = MapleSquadType.horntail;
                break;
            case 240060001:
            case 240060101:
            case 240060201:
                zz = MapleSquadType.chaosht;
                break;
            case 270050100:
                zz = MapleSquadType.pinkbean;
                break;
            case 802000111:
                zz = MapleSquadType.nmm_squad;
                break;
            case 802000211:
                zz = MapleSquadType.vergamot;
                break;
            case 802000311:
                zz = MapleSquadType.tokyo;
                break;
            case 802000411:
                zz = MapleSquadType.dunas;
                break;
            case 802000611:
                zz = MapleSquadType.nibergen_squad;
                break;
            case 802000711:
                zz = MapleSquadType.dunas2;
                break;
            case 802000801:
            case 802000802:
            case 802000803:
                zz = MapleSquadType.core_blaze;
                break;
            case 802000821:
            case 802000823:
                zz = MapleSquadType.aufheben;
                break;
            case 211070100:
            case 211070101:
            case 211070110:
                zz = MapleSquadType.vonleon;
                break;
            case 551030200:
                zz = MapleSquadType.scartar;
                break;
            case 271040100:
                zz = MapleSquadType.cygnus;
                break;
            case 801040100:
                zz = MapleSquadType.showa;
                break;
            case 123356785: //매그너스
                zz = MapleSquadType.magnas;
                break;
            case 123356780: //힐라
                zz = MapleSquadType.hilla;
                break;
            case 123356782: //스우
                zz = MapleSquadType.soo;
                break;
            case 123356781: //카오스파풀
                zz = MapleSquadType.kapapul;
                break;
            case 450004150: //루시드
                zz = MapleSquadType.lusid;
                break;
            case 123356788: //검은마법사
                zz = MapleSquadType.gumma;
                break;
            case 123356786: //데미안
                zz = MapleSquadType.dami;
                break;
            case 702060000:
                zz = MapleSquadType.murim;
                break;
            default:
                return null;
        }
        return ChannelServer.getInstance(channel).getMapleSquad(zz);
    }

    public final MapleSquad getSquadBegin() {
        if (squad != null) {
            return ChannelServer.getInstance(channel).getMapleSquad(squad);
        }
        return null;
    }

    public final EventManager getEMByMap() {
        String em = null;
        switch (mapid) {
            case 105100400:
                em = "BossBalrog_EASY";
                break;
            case 105100300:
                em = "BossBalrog_NORMAL";
                break;
            case 280030000:
                em = "ZakumBattle";
                break;
            case 240060000:
            case 240060100:
            case 240060200:
                em = "HorntailBattle";
                break;
            case 280030001:
                em = "ChaosZakum";
                break;
            case 240060001:
            case 240060101:
            case 240060201:
                em = "ChaosHorntail";
                break;
            case 270050100:
                em = "PinkBeanBattle";
                break;
            case 802000111:
                em = "NamelessMagicMonster";
                break;
            case 802000211:
                em = "Vergamot";
                break;
            case 802000311:
                em = "tokyopq";
                break;
            case 802000411:
                em = "Dunas";
                break;
            case 802000611:
                em = "Nibergen";
                break;
            case 802000711:
                em = "Dunas2";
                break;
            case 802000801:
            case 802000802:
            case 802000803:
                em = "CoreBlaze";
                break;
            case 802000821:
            case 802000823:
                em = "Aufhaven";
                break;
            case 211070100:
            case 211070101:
            case 211070110:
                em = "VonLeonBattle";
                break;
            case 551030200:
                em = "ScarTarBattle";
                break;
            case 271040100:
                em = "CygnusBattle";
                break;
            case 801040100:
                em = "showaboss";
                break;
            case 123356785:
                em = "magnas"; //매그너스
                break;
            case 123356780:
                em = "hila"; //힐라
                break;
            case 123356782:
                em = "soo"; //스우
                break;
            case 123356781:
                em = "kapapul"; //카오스파풀라투스
                break;
            case 450004150:
                em = "lusid"; //루시드
                break;
            case 123356788:
                em = "gumma"; //검은마법사
                break;
            case 123356786:
                em = "dami"; //데미안
                break;
            case 702060001:
                em = "MurieBattle";
                break;
            case 702060000:
                em = "MurimBattle";
                break;
            default:
                return null;
        }
        return ChannelServer.getInstance(channel).getEventSM().getEventManager(em);
    }

    public final void removePlayer(final MapleCharacter chr) {
        //log.warn("[dc] [level2] Player {} leaves map {}", new Object[] { chr.getName(), mapid });

        if (everlast) {
            returnEverLastItem(chr);
        }

        charactersLock.writeLock().lock();
        try {
            characters.remove(chr);
        } finally {
            charactersLock.writeLock().unlock();
        }
        removeMapObject(chr);
        chr.checkFollow();
        chr.removeExtractor();
        chr.cancelEffectFromBuffStat(MapleBuffStat.SIDEKICK_PASSIVE);
        if (chr.getSidekick() != null) {
            MapleCharacter side = getCharacterById(chr.getSidekick().getCharacter(chr.getSidekick().getCharacter(0).getId() == chr.getId() ? 1 : 0).getId());
            if (side != null) {
                side.cancelEffectFromBuffStat(MapleBuffStat.SIDEKICK_PASSIVE);
            }
        }
        broadcastMessage(MaplePacketCreator.removePlayerFromMap(chr.getId()));

        List<MapleSummon> toCancel = new ArrayList<MapleSummon>();
        final List<MapleSummon> ss = chr.getSummonsReadLock();
        try {
            for (final MapleSummon summon : ss) {
                broadcastMessage(MaplePacketCreator.removeSummon(summon, true));
                removeMapObject(summon);
                if (summon.getMovementType() == SummonMovementType.STATIONARY /*|| summon.getMovementType() == SummonMovementType.CIRCLE_STATIONARY*/ || summon.getMovementType() == SummonMovementType.WALK_STATIONARY) {

                    toCancel.add(summon);
                } else {
                    summon.setChangedMap(true);
                }
            }
        } finally {
            chr.unlockSummonsReadLock();
        }
        for (MapleSummon summon : toCancel) {
            chr.removeSummon(summon);
            chr.dispelSkill(summon.getSkill()); //remove the buff
        }
        checkStates(chr.getName());
        if (mapid == 109020001) {
            chr.canTalk(true);
        }
        chr.leaveMap(this);
        recalcCanSpawnMobs();
    }
    double maxspawns = 1;

    public void recalcCanSpawnMobs() {
        double min = (xy * monsterRate * 0.0000078125);
        if (min <= 1) {
            min = 1;
        }
        if (min >= 40) {
            min = 40;
        }
        double max = min * 1.6;
        maxspawns = max;
//        double fix = min + (getCharactersSize() * 1.3 * min);
//        maxspawns = Math.min(min * 1.3, fix);
//        System.out.println(maxspawns);
    }

    public final void broadcastMessage(final byte[] packet) {
        broadcastMessage(null, packet, Double.POSITIVE_INFINITY, null);
    }

    public final void broadcastMessage(final MapleCharacter source, final byte[] packet, final boolean repeatToSource) {
        broadcastMessage(repeatToSource ? null : source, packet, Double.POSITIVE_INFINITY, source.getTruePosition());
    }

    /*	public void broadcastMessage(MapleCharacter source, byte[] packet, boolean repeatToSource, boolean ranged) {
     broadcastMessage(repeatToSource ? null : source, packet, ranged ? MapleCharacter.MAX_VIEW_RANGE_SQ : Double.POSITIVE_INFINITY, source.getPosition());
     }*/
    public final void broadcastMessage(final byte[] packet, final Point rangedFrom) {
        broadcastMessage(null, packet, GameConstants.maxViewRangeSq(), rangedFrom);
    }

    public final void broadcastMessage(final MapleCharacter source, final byte[] packet, final Point rangedFrom) {
        broadcastMessage(source, packet, GameConstants.maxViewRangeSq(), rangedFrom);
    }

    public void broadcastMessage(final MapleCharacter source, final byte[] packet, final double rangeSq, final Point rangedFrom) {
        charactersLock.readLock().lock();
        try {
            for (MapleCharacter chr : characters) {
                if (chr != source) {
                    if (rangeSq < Double.POSITIVE_INFINITY) {
                        if (rangedFrom.distanceSq(chr.getTruePosition()) <= rangeSq) {
                            chr.getClient().getSession().write(packet);
                        }
                    } else {
                        chr.getClient().getSession().write(packet);
                    }
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
    }

    private void sendObjectPlacement(final MapleCharacter c) {
        if (c == null) {
            return;
        }
        for (final MapleMapObject o : getMapObjectsInRange(c.getTruePosition(), c.getRange(), GameConstants.rangedMapobjectTypes)) {
            if (o.getType() == MapleMapObjectType.REACTOR) {
                if (!((MapleReactor) o).isAlive()) {
                    continue;
                }
            }
            o.sendSpawnData(c.getClient());
            c.addVisibleMapObject(o);
        }
    }

    public final List<MaplePortal> getPortalsInRange(final Point from, final double rangeSq) {
        final List<MaplePortal> ret = new ArrayList<MaplePortal>();
        for (MaplePortal type : portals.values()) {
            if (from.distanceSq(type.getPosition()) <= rangeSq && type.getTargetMapId() != mapid && type.getTargetMapId() != 999999999) {
                ret.add(type);
            }
        }
        return ret;
    }

    public final List<MapleMapObject> getMapObjectsInRange(final Point from, final double rangeSq) {
        final List<MapleMapObject> ret = new ArrayList<MapleMapObject>();
        for (MapleMapObjectType type : MapleMapObjectType.values()) {
            mapobjectlocks.get(type).readLock().lock();
            try {
                Iterator<MapleMapObject> itr = mapobjects.get(type).values().iterator();
                while (itr.hasNext()) {
                    MapleMapObject mmo = itr.next();
                    if (from.distanceSq(mmo.getTruePosition()) <= rangeSq) {
                        ret.add(mmo);
                    }
                }
            } finally {
                mapobjectlocks.get(type).readLock().unlock();
            }
        }
        return ret;
    }

    public List<MapleMapObject> getItemsInRange(Point from, double rangeSq) {
        return getMapObjectsInRange(from, rangeSq, Arrays.asList(MapleMapObjectType.ITEM));
    }

    public final List<MapleMapObject> getMapObjectsInRange(final Point from, final double rangeSq, final List<MapleMapObjectType> MapObject_types) {
        final List<MapleMapObject> ret = new ArrayList<MapleMapObject>();
        for (MapleMapObjectType type : MapObject_types) {
            mapobjectlocks.get(type).readLock().lock();
            try {
                Iterator<MapleMapObject> itr = mapobjects.get(type).values().iterator();
                while (itr.hasNext()) {
                    MapleMapObject mmo = itr.next();
                    if (from.distanceSq(mmo.getTruePosition()) <= rangeSq) {
                        ret.add(mmo);
                    }
                }
            } finally {
                mapobjectlocks.get(type).readLock().unlock();
            }
        }
        return ret;
    }

    public final List<MapleMapObject> getMapObjectsInRect(final Rectangle box, final List<MapleMapObjectType> MapObject_types) {
        final List<MapleMapObject> ret = new ArrayList<MapleMapObject>();
        for (MapleMapObjectType type : MapObject_types) {
            mapobjectlocks.get(type).readLock().lock();
            try {
                Iterator<MapleMapObject> itr = mapobjects.get(type).values().iterator();
                while (itr.hasNext()) {
                    MapleMapObject mmo = itr.next();
                    if (box.contains(mmo.getTruePosition())) {
                        ret.add(mmo);
                    }
                }
            } finally {
                mapobjectlocks.get(type).readLock().unlock();
            }
        }
        return ret;
    }

    public final List<MapleCharacter> getCharactersIntersect(final Rectangle box) {
        final List<MapleCharacter> ret = new ArrayList<MapleCharacter>();
        charactersLock.readLock().lock();
        try {
            for (MapleCharacter chr : characters) {
                if (chr.getBounds().intersects(box)) {
                    ret.add(chr);
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        return ret;
    }

    public final List<MapleCharacter> getPlayersInRectAndInList(final Rectangle box, final List<MapleCharacter> chrList) {
        final List<MapleCharacter> character = new LinkedList<MapleCharacter>();

        charactersLock.readLock().lock();
        try {
            final Iterator<MapleCharacter> ltr = characters.iterator();
            MapleCharacter a;
            while (ltr.hasNext()) {
                a = ltr.next();
                if (chrList.contains(a) && box.contains(a.getTruePosition())) {
                    character.add(a);
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        return character;
    }

    public final void addPortal(final MaplePortal myPortal) {
        portals.put(myPortal.getId(), myPortal);
    }

    public final MaplePortal getPortal(final String portalname) {
        for (final MaplePortal port : portals.values()) {
            if (port.getName().equals(portalname)) {
                return port;
            }
        }
        return null;
    }

    public final MaplePortal getPortal(final int portalid) {
        return portals.get(portalid);
    }

    public final void resetPortals() {
        for (final MaplePortal port : portals.values()) {
            port.setPortalState(true);
        }
    }

    public final void setFootholds(final MapleFootholdTree footholds) {
        this.footholds = footholds;
    }

    public final MapleFootholdTree getFootholds() {
        return footholds;
    }

    public final int getNumSpawnPoints() {
        return monsterSpawn.size();
    }

    public final void loadMonsterRate(final boolean first) {
        final int spawnSize = monsterSpawn.size();
        if (spawnSize >= 20 || partyBonusRate > 0) {
            maxRegularSpawn = Math.round(spawnSize / monsterRate);
        } else {
            maxRegularSpawn = (int) Math.ceil(spawnSize * monsterRate);
        }
        if (fixedMob > 0) {
            maxRegularSpawn = fixedMob;
        } else if (maxRegularSpawn <= 2) {
            maxRegularSpawn = 2;
        } else if (maxRegularSpawn > spawnSize) {
            maxRegularSpawn = Math.max(10, spawnSize);
        }

        Collection<Spawns> newSpawn = new LinkedList<Spawns>();
        Collection<Spawns> newBossSpawn = new LinkedList<Spawns>();
        for (final Spawns s : monsterSpawn) {
            if (s.getCarnivalTeam() >= 2) {
                continue; // Remove carnival spawned mobs
            }
            if (s.getMonster().isBoss()) {
                newBossSpawn.add(s);
            } else {
                newSpawn.add(s);
            }
        }
        monsterSpawn.clear();
        monsterSpawn.addAll(newBossSpawn);
        monsterSpawn.addAll(newSpawn);

        if (first && spawnSize > 0) {
            lastSpawnTime = System.currentTimeMillis();
            if (GameConstants.isForceRespawn(mapid)) {
                createMobInterval = 15000;
            }
            respawn(false); // this should do the trick, we don't need to wait upon entering map                
        }
    }

    public final SpawnPoint addMonsterSpawn(final MapleMonster monster, final int mobTime, final byte carnivalTeam, final String msg) {
        final Point newpos = calcPointBelow(monster.getPosition());
        newpos.y -= 1;
        final SpawnPoint sp = new SpawnPoint(monster, newpos, mobTime, carnivalTeam, msg);
        if (carnivalTeam > -1) {
            monsterSpawn.add(0, sp); //at the beginning
        } else {
            monsterSpawn.add(sp);
            if (sp.getMonster().isBoss()) {
                sp.setRespawnTime();
            } else if (maxRegularSpawn > spawnedMonstersOnMap.get()) {
                sp.spawnMonster(this);
            }
        }
        return sp;
    }

    public final void addAreaMonsterSpawn(final MapleMonster monster, Point pos1, Point pos2, Point pos3, final int mobTime, final String msg, final boolean shouldSpawn) {
        pos1 = calcPointBelow(pos1);
        pos2 = calcPointBelow(pos2);
        pos3 = calcPointBelow(pos3);
        if (pos1 != null) {
            pos1.y -= 1;
        }
        if (pos2 != null) {
            pos2.y -= 1;
        }
        if (pos3 != null) {
            pos3.y -= 1;
        }
        if (pos1 == null && pos2 == null && pos3 == null) {
            System.out.println("WARNING: mapid " + mapid + ", monster " + monster.getId() + " could not be spawned.");

            return;
        } else if (pos1 != null) {
            if (pos2 == null) {
                pos2 = new Point(pos1);
            }
            if (pos3 == null) {
                pos3 = new Point(pos1);
            }
        } else if (pos2 != null) {
            if (pos1 == null) {
                pos1 = new Point(pos2);
            }
            if (pos3 == null) {
                pos3 = new Point(pos2);
            }
        } else if (pos3 != null) {
            if (pos1 == null) {
                pos1 = new Point(pos3);
            }
            if (pos2 == null) {
                pos2 = new Point(pos3);
            }
        }
        monsterSpawn.add(new SpawnPointAreaBoss(monster, pos1, pos2, pos3, mobTime, msg, shouldSpawn));
    }

    public final List<MapleCharacter> getCharacters() {
        return getCharactersThreadsafe();
    }

    public final List<MapleCharacter> getCharactersThreadsafe() {
        final List<MapleCharacter> chars = new ArrayList<MapleCharacter>();

        charactersLock.readLock().lock();
        try {
            for (MapleCharacter mc : characters) {
                chars.add(mc);
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        return chars;
    }

    public final MapleCharacter getCharacterByName(final String id) {
        charactersLock.readLock().lock();
        try {
            for (MapleCharacter mc : characters) {
                if (mc.getName().equalsIgnoreCase(id)) {
                    return mc;
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        return null;
    }

    public final MapleCharacter getCharacterById_InMap(final int id) {
        return getCharacterById(id);
    }

    public final MapleCharacter getCharacterById(final int id) {
        charactersLock.readLock().lock();
        try {
            for (MapleCharacter mc : characters) {
                if (mc.getId() == id) {
                    return mc;
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        return null;
    }

    public final void updateMapObjectVisibility(final MapleCharacter chr, final MapleMapObject mo) {
        if (chr == null) {
            return;
        }
        if (!chr.isMapObjectVisible(mo)) { // monster entered view range
            if (mo.getType() == MapleMapObjectType.MIST || mo.getType() == MapleMapObjectType.EXTRACTOR || mo.getType() == MapleMapObjectType.SUMMON || mo.getType() == MapleMapObjectType.FAMILIAR || mo instanceof MechDoor || mo.getTruePosition().distanceSq(chr.getTruePosition()) <= mo.getRange()) {
                chr.addVisibleMapObject(mo);
                mo.sendSpawnData(chr.getClient());
            }
        } else { // monster left view range
            if (!(mo instanceof MechDoor) && mo.getType() != MapleMapObjectType.MIST && mo.getType() != MapleMapObjectType.EXTRACTOR && mo.getType() != MapleMapObjectType.SUMMON && mo.getType() != MapleMapObjectType.PLAYER && mo.getType() != MapleMapObjectType.FAMILIAR && mo.getTruePosition().distanceSq(chr.getTruePosition()) > mo.getRange()) {
                chr.removeVisibleMapObject(mo);
                mo.sendDestroyData(chr.getClient());
            } else if (mo.getType() == MapleMapObjectType.MONSTER) { //monster didn't leave view range, and is visible
                if (chr.getTruePosition().distanceSq(mo.getTruePosition()) <= GameConstants.maxViewRangeSq()) {
                    updateMonsterController((MapleMonster) mo);
                }
            }
        }
    }

    public void moveMonster(MapleMonster monster, Point reportedPos) {
        monster.setPosition(reportedPos);
        for (MapleMapObject _obj : mapobjects.get(MapleMapObjectType.PLAYER).values()) {
            MapleCharacter mc = (MapleCharacter) _obj;
            updateMapObjectVisibility(mc, monster);
        }
    }

    public void movePlayer(final MapleCharacter player, final Point newPosition) {
        player.setPosition(newPosition);
        try {
            Collection<MapleMapObject> visibleObjects = player.getAndWriteLockVisibleMapObjects();
            ArrayList<MapleMapObject> copy = new ArrayList<MapleMapObject>(visibleObjects);
            Iterator<MapleMapObject> itr = copy.iterator();
            while (itr.hasNext()) {
                MapleMapObject mo = itr.next();
                if (mo != null && getMapObject(mo.getObjectId(), mo.getType()) == mo) {
                    updateMapObjectVisibility(player, mo);
                } else if (mo != null) {
                    visibleObjects.remove(mo);
                }
            }
            for (MapleMapObject mo : getMapObjectsInRange(player.getTruePosition(), player.getRange())) {
                if (mo != null && !visibleObjects.contains(mo)) {
                    mo.sendSpawnData(player.getClient());
                    visibleObjects.add(mo);
                }
            }
        } finally {
            player.unlockWriteVisibleMapObjects();
        }
    }

    public MaplePortal findClosestSpawnpoint(Point from) {
        MaplePortal closest = getPortal(0);
        double distance, shortestDistance = Double.POSITIVE_INFINITY;
        for (MaplePortal portal : portals.values()) {
            distance = portal.getPosition().distanceSq(from);
            if (portal.getType() >= 0 && portal.getType() <= 2 && distance < shortestDistance && portal.getTargetMapId() == 999999999) {
                closest = portal;
                shortestDistance = distance;
            }
        }
        return closest;
    }

    public MaplePortal findClosestPortal(Point from) {
        MaplePortal closest = getPortal(0);
        double distance, shortestDistance = Double.POSITIVE_INFINITY;
        for (MaplePortal portal : portals.values()) {
            distance = portal.getPosition().distanceSq(from);
            if (distance < shortestDistance) {
                closest = portal;
                shortestDistance = distance;
            }
        }
        return closest;
    }

    public String spawnDebug() {
        StringBuilder sb = new StringBuilder("Mobs in map : ");
        sb.append(this.getMobsSize());
        sb.append(" spawnedMonstersOnMap: ");
        sb.append(spawnedMonstersOnMap);
        sb.append(" spawnpoints: ");
        sb.append(monsterSpawn.size());
        sb.append(" maxRegularSpawn: ");
        sb.append(maxRegularSpawn);
        sb.append(" actual monsters: ");
        sb.append(getNumMonsters());
        sb.append(" monster rate: ");
        sb.append(monsterRate);
//        sb.append(" fixed: ");
//        sb.append(fixedMob);

        double fix2 = (monsterRate * 1.3) / 1.5;
        double realMax = (maxspawns / fix2) * 2 > maxRegularSpawn ? Math.min(maxRegularSpawn * 3, maxspawns) : (maxspawns / fix2) * 1.8;
        int min = (int) (realMax / 2.5);

        sb.append(" min: ").append(min);
        sb.append(" max: ").append(realMax);
        sb.append(" fixedSpawns: ");
        sb.append(fixSpawns);
        sb.append(" plusMob: ").append(plusMob);
        sb.append(" curPlusMobSize: ").append(plusMobSize);
        sb.append(" plusMobSizePerSec(Plusing): ").append(plusMobLastOsize.get());

        return sb.toString();
    }

    public int characterSize() {
        return characters.size();
    }

    public final int getMapObjectSize() {
        return mapobjects.size() + getCharactersSize() - characters.size();
    }

    public final int getCharactersSize() {
        int ret = 0;
        charactersLock.readLock().lock();
        try {
            final Iterator<MapleCharacter> ltr = characters.iterator();
            MapleCharacter chr;
            while (ltr.hasNext()) {
                chr = ltr.next();
                ret++;
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        return ret;
    }

    public Collection<MaplePortal> getPortals() {
        return Collections.unmodifiableCollection(portals.values());
    }

    public int getSpawnedMonstersOnMap() {
        return spawnedMonstersOnMap.get();
    }

    public final void startMapEffectAOJ(final String msg, final int itemId, final boolean jukebox) {
        broadcastMessage(MaplePacketCreator.startMapEffect(msg, itemId, true));
        MapTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                broadcastMessage(MaplePacketCreator.removeMapEffect());
                broadcastMessage(MaplePacketCreator.startMapEffect(msg, itemId, false));
                //dont remove mapeffect.
            }
        }, 5000);
    }

    public void spawnMist(MapleMist mist, int duration, boolean b) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void warp(int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private class ActivateItemReactor implements Runnable {

        private MapleMapItem mapitem;
        private MapleReactor reactor;
        private MapleClient c;

        public ActivateItemReactor(MapleMapItem mapitem, MapleReactor reactor, MapleClient c) {
            this.mapitem = mapitem;
            this.reactor = reactor;
            this.c = c;
        }

        @Override
        public void run() {
            if (mapitem != null && mapitem == getMapObject(mapitem.getObjectId(), mapitem.getType()) && !mapitem.isPickedUp()) {
                mapitem.expire(MapleMap.this);
                reactor.hitReactor(c);
                reactor.setTimerActive(false);

                if (reactor.getDelay() > 0) {
                    MapTimer.getInstance().schedule(new Runnable() {

                        @Override
                        public void run() {
                            reactor.forceHitReactor((byte) 0);
                        }
                    }, reactor.getDelay());
                }
            } else {
                reactor.setTimerActive(false);
            }
        }
    }

    public void respawn(final boolean force) {
        respawn(force, System.currentTimeMillis());
    }

    public void respawn(final boolean force, final long now) {
        lastSpawnTime = now;
        if (force) {

            final int numShouldSpawn = (int) monsterSpawn.size() - spawnedMonstersOnMap.get();

            if (numShouldSpawn > 0) {
                int spawned = 0;
                for (Spawns spawnPoint : monsterSpawn) {
                    if (!blockedMobGen.isEmpty() && blockedMobGen.contains(Integer.valueOf(spawnPoint.getMonster().getId()))) {
                        continue;
                    }
                    spawnPoint.spawnMonster(this);
                    spawned++;
                    if (spawned >= numShouldSpawn) {
                        break;
                    }
                }
            }
        } else if (getId() / 10000000 == 98) { // 몬스터 카니발
            final List<Spawns> randomSpawn = new ArrayList<Spawns>(monsterSpawn);
            Collections.shuffle(randomSpawn);
            int blue = 0, red = 0, maxblue = 0, maxred = 0;
            for (MapleMonster mob : getAllMonster()) {
                if (mob.getCarnivalTeam() == 0) {
                    blue++;
                } else {
                    red++;
                }
            }
            for (Spawns spawnMax : randomSpawn) {
                if (spawnMax.getCarnivalTeam() == 0) {
                    maxblue++;
                } else {
                    maxred++;
                }
            }
            for (Spawns spawnPoint : randomSpawn) {
                if (spawnPoint.getCarnivalTeam() == 0) {
                    if (blue >= maxblue * 1.2) {
                        continue;
                    }
                    if (!isSpawns && spawnPoint.getMobTime() > 0) {
                        continue;
                    }
                    if (!blockedMobGen.isEmpty() && blockedMobGen.contains(Integer.valueOf(spawnPoint.getMonster().getId()))) {
                        continue;
                    }
                    blue++;
                    spawnPoint.spawnMonster(this);
                } else {
                    if (red >= maxred * 1.2) {
                        continue;
                    }
                    if (!isSpawns && spawnPoint.getMobTime() > 0) {
                        continue;
                    }
                    if (!blockedMobGen.isEmpty() && blockedMobGen.contains(Integer.valueOf(spawnPoint.getMonster().getId()))) {
                        continue;
                    }
                    red++;
                    spawnPoint.spawnMonster(this);
                }
            }
        } else {
            int numShouldSpawn;//맵별 젠률

            int kannaSpawnCount = 0;
            double kannaSpawnCheck = 0.0;
            for (MapleSummon summon : getAllSummonsThreadsafe()) {
                final MapleStatEffect kysin = SkillFactory.getSkill(summon.getSkill()).getEffect(summon.getSkillLevel());
                if (summon.getSkill() == 1074) {
                    kannaSpawnCount = kysin.getZ();
                    kannaSpawnCheck = (kannaSpawnCount / 100.0) + 1.0;
                }
            }

            double addCount = 2;

            if (getId() >= 273010000 && getId() <= 273060300) {//황페
                addCount = 3;
            }
            if (getId() >= 701101000 && getId() <= 701103030) {//예원
                addCount = 3;
            }
            if (getId() >= 270010100 && getId() <= 270030500) {//타임로드
                addCount = 3;
            }
            if (getId() >= 240000000 && getId() <= 240040610) {//리프레
                addCount = 3;
            }
            if (getId() >= 541020000 && getId() <= 541020500) {//울루
                addCount = 1.5;
            }
            if (getId() >= 211060100 && getId() <= 211060900) {//성벽
                addCount = 1.5;
            }
            if (getId() >= 271030010 && getId() <= 271030600) {//기사단
                addCount = 3;
            }
            if (getId() >= 600000000 && getId() <= 600010230) {//마스테리아
                addCount = 3;
            }
//            if (getId() >= 600000000 && getId() <= 600010230) {//크리키아 추후 오픈때
//                addCount = 3;
//            }

            numShouldSpawn = (GameConstants.isForceRespawn(mapid) ? monsterSpawn.size() : (int) (maxRegularSpawn * (addCount + kannaSpawnCheck))) - spawnedMonstersOnMap.get();

            if (numShouldSpawn > 0) {
                int spawned = 0;
                int tries = 0;
                final List<Spawns> randomSpawn = new ArrayList<>(monsterSpawn);
                while (spawned < numShouldSpawn) {
                    Collections.shuffle(randomSpawn);
                    for (Spawns spawnPoint : randomSpawn) {
                        if (!isSpawns && spawnPoint.getMobTime() > 0) {
                            continue;
                        }
                        if (!blockedMobGen.isEmpty() && blockedMobGen.contains(Integer.valueOf(spawnPoint.getMonster().getId()))) {
                            continue;
                        }
                        if (kannaSpawnCount > 0) {
                            if (spawnPoint.getMonster().isBoss()) {
                                continue;
                            }
                            spawnPoint.spawnMonster(this);
                            spawned++;
                        }
                        if (spawnPoint.shouldSpawn(lastSpawnTime) || GameConstants.isForceRespawn(mapid) || (!GameConstants.isMonsterpark(mapid) && monsterSpawn.size() < 70 && maxRegularSpawn * 6 > monsterSpawn.size() && partyBonusRate > 0)) {
                            if (spawnPoint.isFieldBoss()) {
                                if (!spawnPoint.shouldSpawn(now)) {
                                    continue;
                                }
                            }
                            spawnPoint.spawnMonster(this);
                            spawned++;
                        }
                        if (spawned >= numShouldSpawn) {
                            break;
                        }
                    }
                    tries++;
                    if (tries > 10) {
                        break;
                    }
                }
            }
        }
    }

    private static interface DelayedPacketCreation {

        void sendPackets(MapleClient c);
    }

    public String getSnowballPortal() {
        int[] teamss = new int[2];
        charactersLock.readLock().lock();
        try {
            for (MapleCharacter chr : characters) {
                if (chr.getTruePosition().y > -80) {
                    teamss[0]++;
                } else {
                    teamss[1]++;
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        if (teamss[0] > teamss[1]) {
            return "st01";
        } else {
            return "st00";
        }
    }

    public boolean isDisconnected(int id) {
        return dced.contains(Integer.valueOf(id));
    }

    public void addDisconnected(int id) {
        dced.add(Integer.valueOf(id));
    }

    public void resetDisconnected() {
        dced.clear();
    }

    public void startSpeedRun() {
        final MapleSquad squad = getSquadByMap();
        if (squad != null) {
            charactersLock.readLock().lock();
            try {
                for (MapleCharacter chr : characters) {
                    if (chr.getName().equals(squad.getLeaderName()) && !chr.isIntern()) {
                        startSpeedRun(chr.getName());
                        return;
                    }
                }
            } finally {
                charactersLock.readLock().unlock();
            }
        }
    }

    public void startSpeedRun(String leader) {
        speedRunStart = System.currentTimeMillis();
        speedRunLeader = leader;
    }

    public void endSpeedRun() {
        speedRunStart = 0;
        speedRunLeader = "";
    }

    public void getRankAndAdd(String leader, String time, ExpeditionType type, long timz, Collection<String> squad) {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            long lastTime = SpeedRunner.getSpeedRunData(type) == null ? 0 : SpeedRunner.getSpeedRunData(type).right;
            //if(timz > lastTime && lastTime > 0) {
            //return;
            //}
            //Pair<String, Map<Integer, String>>
            StringBuilder rett = new StringBuilder();
            if (squad != null) {
                for (String chr : squad) {
                    rett.append(chr);
                    rett.append(",");
                }
            }
            String z = rett.toString();
            if (squad != null) {
                z = z.substring(0, z.length() - 1);
            }
            con = DatabaseConnection.getConnection();//커넥션
            ps = con.prepareStatement("INSERT INTO speedruns(`type`, `leader`, `timestring`, `time`, `members`) VALUES (?,?,?,?,?)");
            ps.setString(1, type.name());
            ps.setString(2, leader);
            ps.setString(3, time);
            ps.setLong(4, timz);
            ps.setString(5, z);
            ps.executeUpdate();
            ps.close();

            if (lastTime == 0) { //great, we just add it
                SpeedRunner.addSpeedRunData(type, SpeedRunner.addSpeedRunData(new StringBuilder(SpeedRunner.getPreamble(type)), new HashMap<Integer, String>(), z, leader, 1, time), timz);
            } else {
                //i wish we had a way to get the rank
                //TODO revamp
                SpeedRunner.removeSpeedRunData(type);
                SpeedRunner.loadSpeedRunData(type);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public long getSpeedRunStart() {
        return speedRunStart;
    }

    public final void disconnectAll() {
        for (MapleCharacter chr : getCharactersThreadsafe()) {
            if (!chr.isGM()) {
                chr.getClient().disconnect(true, false);
                chr.getClient().getSession().close();
            }
        }
    }

    public List<MapleNPC> getAllNPCs() {
        return getAllNPCsThreadsafe();
    }

    public List<MapleNPC> getAllNPCsThreadsafe() {
        ArrayList<MapleNPC> ret = new ArrayList<MapleNPC>();
        mapobjectlocks.get(MapleMapObjectType.NPC).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.NPC).values()) {
                ret.add((MapleNPC) mmo);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.NPC).readLock().unlock();
        }
        return ret;
    }

    public final void resetNPCs() {
        removeNpc(-1);
    }

    public final void resetPQ(int level) {
        resetFully();
        for (MapleMonster mons : getAllMonstersThreadsafe()) {
            mons.changeLevel(level, true);
        }
        resetSpawnLevel(level);
    }

    public final void resetSpawnLevel(int level) {
        for (Spawns spawn : monsterSpawn) {
            if (spawn instanceof SpawnPoint) {
                ((SpawnPoint) spawn).setLevel(level);
            }
        }
    }

    public final void resetFully() {
        resetFully(true);
    }

    public final void resetFully(final boolean respawn) {
        killAllMonsters(false);
        reloadReactors();
        removeDrops();
        resetNPCs();
        resetSpawns();
        resetDisconnected();
        endSpeedRun();
        cancelSquadSchedule(true);
        resetPortals();
        environment.clear();
        if (respawn) {
            respawn(true);
        }
    }

    public final void cancelSquadSchedule(boolean interrupt) {
        squadTimer = false;
        checkStates = true;
        if (squadSchedule != null) {
            squadSchedule.cancel(interrupt);
            squadSchedule = null;
        }
    }

    public final void removeDrops() {
        List<MapleMapItem> items = this.getAllItemsThreadsafe();
        for (MapleMapItem i : items) {
            i.expire(this);
        }
    }

    public final void resetAllSpawnPoint(int mobid, int mobTime) {
        Collection<Spawns> sss = new LinkedList<Spawns>(monsterSpawn);
        resetFully();
        monsterSpawn.clear();
        for (Spawns s : sss) {
            MapleMonster newMons = MapleLifeFactory.getMonster(mobid);
            newMons.setF(s.getF());
            newMons.setFh(s.getFh());
            newMons.setPosition(s.getPosition());
            addMonsterSpawn(newMons, mobTime, (byte) -1, null);
        }
        loadMonsterRate(true);
    }

    public final void resetSpawns() {
        boolean changed = false;
        Iterator<Spawns> sss = monsterSpawn.iterator();
        while (sss.hasNext()) {
            if (sss.next().getCarnivalId() > -1) {
                sss.remove();
                changed = true;
            }
        }
        setSpawns(true);
        if (changed) {
            loadMonsterRate(true);
        }
    }

    public final boolean makeCarnivalSpawn(final int team, final MapleMonster newMons, final int num) {
        MonsterPoint ret = null;
        for (MonsterPoint mp : nodes.getMonsterPoints()) {
            if (mp.team == team || mp.team == -1) {
                final Point newpos = calcPointBelow(new Point(mp.x, mp.y));
                newpos.y -= 1;
                boolean found = false;
                for (Spawns s : monsterSpawn) {
                    if (s.getCarnivalId() > -1 && (mp.team == -1 || s.getCarnivalTeam() == mp.team) && s.getPosition().x == newpos.x && s.getPosition().y == newpos.y) {
                        found = true;
                        break; //this point has already been used.
                    }
                }
                if (!found) {
                    ret = mp; //this point is safe for use.
                    break;
                }
            }
        }
        if (ret != null) {
            newMons.setCy(ret.cy);
            newMons.setF(0); //always.
            newMons.setFh(ret.fh);
            newMons.setRx0(ret.x + 50);
            newMons.setRx1(ret.x - 50); //does this matter
            newMons.setPosition(new Point(ret.x, ret.y));
            newMons.setHide(false);
            final SpawnPoint sp = addMonsterSpawn(newMons, 1, (byte) team, null);
            sp.setCarnival(num);
        }
        return ret != null;
    }

    public final boolean makeCarnivalReactor(final int team, final int num) {
        final MapleReactor old = getReactorByName(team + "" + num);
        if (old != null && old.getState() < 5) { //already exists
            return false;
        }
        Point guardz = null;
        final List<MapleReactor> react = getAllReactorsThreadsafe();
        for (Pair<Point, Integer> guard : nodes.getGuardians()) {
            if (guard.right == team || guard.right == -1) {
                boolean found = false;
                for (MapleReactor r : react) {
                    if (r.getTruePosition().x == guard.left.x && r.getTruePosition().y == guard.left.y && r.getState() < 5) {
                        found = true;
                        break; //already used
                    }
                }
                if (!found) {
                    guardz = guard.left; //this point is safe for use.
                    break;
                }
            }
        }
        if (guardz != null) {
            final MapleReactor my = new MapleReactor(MapleReactorFactory.getReactor(9980000 + team), 9980000 + team);
            my.setState((byte) 1);
            my.setName(team + "" + num); //lol
            //with num. -> guardians in factory
            spawnReactorOnGroundBelow(my, guardz);
            final MCSkill skil = MapleCarnivalFactory.getInstance().getGuardian(num);
            for (MapleMonster mons : getAllMonstersThreadsafe()) {
                if (mons.getCarnivalTeam() == team) {
                    skil.getSkill().applyEffect(null, mons, false, (short) 0);
                }
            }
        }
        return guardz != null;
    }

    public final void blockAllPortal() {
        for (MaplePortal p : portals.values()) {
            p.setPortalState(false);
        }
    }

    public boolean getAndSwitchTeam() {
        return getCharactersSize() % 2 != 0;
    }

    public void setSquad(MapleSquadType s) {
        this.squad = s;

    }

    public int getChannel() {
        return channel;
    }

    public int getConsumeItemCoolTime() {
        return consumeItemCoolTime;
    }

    public void setConsumeItemCoolTime(int ciit) {
        this.consumeItemCoolTime = ciit;
    }

    public void setPermanentWeather(int pw) {
        this.permanentWeather = pw;
    }

    public int getPermanentWeather() {
        return permanentWeather;
    }

    public void checkStates(final String chr) {
        if (!checkStates) {
            return;
        }
        final MapleSquad sqd = getSquadByMap();
        final EventManager em = getEMByMap();
        final int size = getCharactersSize();
        if (sqd != null && sqd.getStatus() == 2) {
            sqd.removeMember(chr);
            if (em != null) {
                if (sqd.getLeaderName().equalsIgnoreCase(chr)) {
                    em.setProperty("leader", "false");
                }
                if (chr.equals("") || size == 0) {
                    em.setProperty("state", "0");
                    em.setProperty("leader", "true");
                    cancelSquadSchedule(!chr.equals(""));
                    sqd.clear();
                    sqd.copy();
                }
            }
        }
        if (em != null && em.getProperty("state") != null && (sqd == null || sqd.getStatus() == 2) && size == 0) {
            em.setProperty("state", "0");
            if (em.getProperty("leader") != null) {
                em.setProperty("leader", "true");
            }
        }
        if (speedRunStart > 0 && size == 0) {
            endSpeedRun();
        }
        //if (squad != null) {
        //    final MapleSquad sqdd = ChannelServer.getInstance(channel).getMapleSquad(squad);
        //    if (sqdd != null && chr != null && chr.length() > 0 && sqdd.getAllNextPlayer().contains(chr)) {
        //	sqdd.getAllNextPlayer().remove(chr);
        //	broadcastMessage(MaplePacketCreator.serverNotice(5, "The queued player " + chr + " has left the map."));
        //    }
        //}
    }

    public void setCheckStates(boolean b) {
        this.checkStates = b;
    }

    public void setNodes(final MapleNodes mn) {
        this.nodes = mn;
    }

    public final List<MaplePlatform> getPlatforms() {
        return nodes.getPlatforms();
    }

    public Collection<MapleNodeInfo> getNodes() {
        return nodes.getNodes();
    }

    public MapleNodeInfo getNode(final int index) {
        return nodes.getNode(index);
    }

    public boolean isLastNode(final int index) {
        return nodes.isLastNode(index);
    }

    public final List<Rectangle> getAreas() {
        return nodes.getAreas();
    }

    public final Rectangle getArea(final int index) {
        return nodes.getArea(index);
    }

    public final void changeEnvironment(final String ms, final int type) {
        broadcastMessage(MaplePacketCreator.environmentChange(ms, type));
    }

    public final void toggleEnvironment(final String ms) {
        if (environment.containsKey(ms)) {
            moveEnvironment(ms, environment.get(ms) == 1 ? 2 : 1);
        } else {
            moveEnvironment(ms, 1);
        }
    }

    public final void moveEnvironment(final String ms, final int type) {
//        broadcastMessage(MaplePacketCreator.environmentMove(ms, type));
        environment.put(ms, type);
    }

    public final Map<String, Integer> getEnvironment() {
        return environment;
    }

    public final int getNumPlayersInArea(final int index) {
        return getNumPlayersInRect(getArea(index));
    }

    public final int getNumPlayersInRect(final Rectangle rect) {
        int ret = 0;

        charactersLock.readLock().lock();
        try {
            final Iterator<MapleCharacter> ltr = characters.iterator();
            MapleCharacter a;
            while (ltr.hasNext()) {
                if (rect.contains(ltr.next().getTruePosition())) {
                    ret++;
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        return ret;
    }

    public final int getNumPlayersItemsInArea(final int index) {
        return getNumPlayersItemsInRect(getArea(index));
    }

    public final int getNumPlayersItemsInRect(final Rectangle rect) {
        int ret = getNumPlayersInRect(rect);

        mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.ITEM).values()) {
                if (rect.contains(mmo.getTruePosition())) {
                    ret++;
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().unlock();
        }
        return ret;
    }

    public void broadcastGMMessage(MapleCharacter source, byte[] packet, boolean repeatToSource) {
        broadcastGMMessage(repeatToSource ? null : source, packet);
    }

    private void broadcastGMMessage(MapleCharacter source, byte[] packet) {
        charactersLock.readLock().lock();
        try {
            if (source == null) {
                for (MapleCharacter chr : characters) {
                    if (chr.isStaff()) {
                        chr.getClient().getSession().write(packet);
                    }
                }
            } else {
                for (MapleCharacter chr : characters) {
                    if (chr != source && (chr.getGMLevel() >= source.getGMLevel())) {
                        chr.getClient().getSession().write(packet);
                    }
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
    }

    public final List<Pair<Integer, Integer>> getMobsToSpawn() {
        return nodes.getMobsToSpawn();
    }

    public final List<Integer> getSkillIds() {
        return nodes.getSkillIds();
    }

    public final boolean canSpawn(long now) {
        return lastSpawnTime > 0 && lastSpawnTime + createMobInterval < now;
    }

    public final boolean canSkill(long now) {
        return lastSkillTime > 0 && lastSkillTime < now;
    }

    public void setSkill(long time) {
        this.lastSkillTime = time;
    }

    public long getSkill() {
        return lastSkillTime;
    }

    public final boolean canHurt(long now) {
        if (lastHurtTime > 0 && lastHurtTime + decHPInterval < now) {
            lastHurtTime = now;
            return true;
        }
        return false;
    }

    public final void resetShammos(final MapleClient c) {
        killAllMonsters(true);
        broadcastMessage(MaplePacketCreator.serverNotice(5, "샤모스와 거리가 멀어져 시작지점으로 되돌아갑니다."));
        EtcTimer.getInstance().schedule(new Runnable() {

            public void run() {
                if (c.getPlayer() != null) {
                    c.getPlayer().changeMap(MapleMap.this, getPortal(0));
                    if (getCharactersThreadsafe().size() > 1) {
                        MapScriptMethods.startScript_FirstUser(c, "shammos_Fenter");
                    }
                }
            }
        }, 500); //avoid dl
    }

    public int getInstanceId() {
        return instanceid;
    }

    public void setInstanceId(int ii) {
        this.instanceid = ii;
    }

    public int getPartyBonusRate() {
        return partyBonusRate;
    }

    public void setPartyBonusRate(int ii) {
        this.partyBonusRate = ii;
    }

    public short getTop() {
        return top;
    }

    public short getBottom() {
        return bottom;
    }

    public short getLeft() {
        return left;
    }

    public short getRight() {
        return right;
    }

    public void setTop(int ii) {
        this.top = (short) ii;
    }

    public void setBottom(int ii) {
        this.bottom = (short) ii;
    }

    public void setLeft(int ii) {
        this.left = (short) ii;
    }

    public void setRight(int ii) {
        this.right = (short) ii;
    }

    public void changeMusic(String newa) {
        changedMusic = newa;
    }

    public List<Pair<Point, Integer>> getGuardians() {
        return nodes.getGuardians();
    }

    public DirectionInfo getDirectionInfo(int i) {
        return nodes.getDirection(i);
    }

    //soory for poor hard coding
    public final void shuffleReactors_RomeoJuliet() {
        List<Point> points = new ArrayList<Point>();
        for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
            MapleReactor mr = (MapleReactor) obj;
            if (!mr.getName().contains("out")) {
                points.add(mr.getPosition());
            }
        }
        Collections.shuffle(points);
        for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
            MapleReactor mr = (MapleReactor) obj;
            if (!mr.getName().contains("out")) {
                mr.setPosition(points.remove(points.size() - 1));
            }
        }
    }

    public void setOutMapTime(long l) {
        outMapTime = l;
    }

    public long getOutMapTime() {
        return outMapTime;
    }

    public void clearEffect() {
        broadcastMessage(MaplePacketCreator.showEffect("quest/party/clear"));
        broadcastMessage(MaplePacketCreator.playSound("Party1/Clear"));
        broadcastMessage(MaplePacketCreator.environmentChange("gate", 2));
    }

    public void failEffect() {
        broadcastMessage(MaplePacketCreator.showEffect("quest/party/wrong_kor"));
        broadcastMessage(MaplePacketCreator.playSound("Party1/Failed"));
    }

    public void setMobGen(int mobid, boolean spawn) {
        Integer value = Integer.valueOf(mobid);
        if (spawn) {
            blockedMobGen.remove(value);
        } else {
            if (blockedMobGen.contains(value)) {
                return;
            }
            blockedMobGen.add(value);
        }
    }

    public List<MapleMapObject> getAllShopsThreadsafe() {
        ArrayList<MapleMapObject> ret = new ArrayList<MapleMapObject>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.HIRED_MERCHANT).values()) {
            ret.add(mmo);
        }
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.SHOP).values()) {
            ret.add(mmo);
        }
        return ret;
    }

    public final List<MaplePortal> getPortalSP() {
        List res = new LinkedList();
        for (MaplePortal port : this.portals.values()) {
            if (port.getName().equals("sp")) {
                res.add(port);
            }
        }
        return res;
    }

    private void refreshOwnership() {
        mapOwnerLastActivityTime = System.currentTimeMillis();
    }

    public void updateMapOwner(MapleCharacter chr, boolean message) {
        if (monsterSpawn.size() != 0) {
            MapleCharacter owner = mapOwner;
            if (owner != null && owner.getClient().getLoginState() != 2) {
                mapOwner = chr;
                refreshOwnership();
                chr.dropMessage(6, "맵주인이 게임을 종료하여 당신의 차지!");
                return;
            }
            if (owner != null && (owner.getMapId() != chr.getMapId() || (owner.getClient().getChannel() != chr.getClient().getChannel()))) { //맵주인의 현재위치가 그 맵이 아닐경우
                mapOwner = chr;
                refreshOwnership();
                chr.dropMessage(6, "맵주인이 자리를 이동하여 당신의 차지!");
            } else {
                if ((System.currentTimeMillis() - mapOwnerLastActivityTime) / 600000 >= 60) {
                    mapOwner = chr;
                    refreshOwnership();
                    chr.dropMessage(6, "주인이 600분 이상 활동이 없어 이 맵의 주인은 당신입니다.");
                } else if (owner != null) {//주인이 있다면
                    if (owner != chr && !owner.isPartyMember(chr)) { //주인이랑 공격한사람이 다르고 파티멤버가 아니라면
                        chr.showMapOwnershipInfo(owner); //이땅은 누구꺼야! 라고 말하고
                    } else {
                        refreshOwnership(); //주인이거나 파티 멤버면 가장 최근에 한 활동을 업데이트 한다
                        if (message) {
                            chr.dropMessage(6, "현재 이 맵의 주인은 당신입니다.");
                        }
                    }
                } else if (owner == null) {
                    mapOwner = chr;
                    refreshOwnership();
                    chr.dropMessage(6, "빈 땅이므로 당신의 것이 됩니다.");
                } else {
                    System.out.println("dasdasdas");
                }
            }
        }
    }

    public long OwnerActivity() {
        long timeNow = System.currentTimeMillis();
        return (timeNow - mapOwnerLastActivityTime) / 1000;
    }

    public boolean Collection(final int mobid) { //보스컬렉션 보스수집
        int[] Collectionmob = {
            2220000, //달팽이
            3220000, //나무
            3220001, //선인장
            4220000, //조개
            5220002, //루팡
            5220000, //소라
            5220003, //부엉이
            6220000, //악어
            6220001, //지구방위본부
            7220001, //구미호
            7220000, //곰
            7220002, //고양이
            8500002, //파풀라투스
            8840000, //반레온
            8800102, //카쿰
            8810122, //혼테일
            8300007, //드래곤라이더
            8820001, //핑크빈   
            9400266, //카무나
            9400265, //베르가모트
            9400270, //칼리하
            9400273, //니벨룽겐
            9400294, //듀나스
            9400296, //코어블레이즈
            9400289, //아우프헤븐
            8850011, //여제
            8880000, //매그너스
            8880400, //진힐라
            9801030, //스우
            8820119, //카오스파풀
            8880150, //루시드
            8880501, //검은마법사
            9300891, //데미안
        };
        for (int i = 0; i < Collectionmob.length; i++) {
            if (Collectionmob[i] == mobid) {
                return true;
            }
        }
        return false;
    }
}
