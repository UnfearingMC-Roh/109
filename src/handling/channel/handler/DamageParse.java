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
package handling.channel.handler;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import client.Skill;
import constants.GameConstants;
import client.inventory.Item;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleQuestStatus;
import client.inventory.MapleInventoryType;
import client.PlayerStats;
import client.SkillFactory;
import client.anticheat.CheatTracker;
import client.anticheat.CheatingOffense;
import client.customize.CustomizeStat;
import client.inventory.Equip;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import handling.RecvPacketOpcode;
import handling.world.World;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import server.AutobanManager;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.Randomizer;
import server.life.Element;
import server.life.ElementalEffectiveness;
import server.life.MapleMonster;
import server.life.MapleMonsterStats;
import server.maps.MapleMap;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleMist;
import server.quest.MapleQuest;
import tools.MaplePacketCreator;
import tools.AttackPair;
import tools.FileoutputUtil;
import static tools.FileoutputUtil.CurrentReadable_Time;
import tools.Pair;
import tools.data.LittleEndianAccessor;
import tools.packet.TemporaryStatsPacket;

public class DamageParse {

    public static void applyAttack(final AttackInfo attack, final Skill theSkill, final MapleCharacter player, int attackCount, final double maxDamagePerMonster, final MapleStatEffect effect, final AttackType attack_type) {

        final Iterator<Item> itera = player.getInventory(MapleInventoryType.EQUIPPED).newList().iterator();
        final Equip equip = (Equip) itera.next();
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (player == null) {
            return;
        }
        if (!player.isAlive()) {
            player.getCheatTracker().registerOffense(CheatingOffense.ATTACKING_WHILE_DEAD);
            return;
        }
        int[] 켄타맵 = {923040100, 923040200, 923040300, 923040400};
        if (attack.skill == 1221011) {
            for (int mapID : 켄타맵) {
                if (player.getMapId() == mapID) {
                    player.dropMessage(5, "현재 맵에서는 생츄어리의 데미지가 적용되지 않습니다.");
                    player.getClient().sendPacket(MaplePacketCreator.enableActions());
                    return;
                }
            }
        }

        if (player.getBuffedValue(MapleBuffStat.DARKSIGHT) != null) {
            Skill darksight = SkillFactory.getSkill(4330001);
            int darklevel = player.getSkillLevel(darksight);
            if (darklevel > 0) {
                if (Randomizer.nextInt(100) < darksight.getEffect(darklevel).getProb()) {
                    //player.cancelBuffStats(false, MapleBuffStat.DARKSIGHT);
                    player.cancelEffectFromBuffStat(MapleBuffStat.DARKSIGHT);
                }
            } else {
                player.cancelEffectFromBuffStat(MapleBuffStat.DARKSIGHT);
            }
        }
        if (GameConstants.getAttackDelay(attack.skill, theSkill) >= 100) {
            player.getCheatTracker().checkAttack(attack.skill, attack.lastAttackTickCount);
        }
        //player.dropMessage(6, "attack.skill : " + attack.skill); //메카닉 미사일탱크 고칠때 사용함
        if (attack.skill != 0) {
            if (effect == null) {
                player.getClient().getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            if (GameConstants.isMulungSkill(attack.skill)) {
                if (player.getMapId() / 10000 != 92502) {
                    //AutobanManager.getInstance().autoban(player.getClient(), "Using Mu Lung dojo skill out of dojo maps.");
                    return;
                } else {
                    if (player.getMulungEnergy() < 10000) {
                        return;
                    }
                    player.mulung_EnergyModify(false);
                }
            } else if (GameConstants.isPyramidSkill(attack.skill)) {
                if (player.getMapId() / 1000000 != 926) {
                    //AutobanManager.getInstance().autoban(player.getClient(), "Using Pyramid skill outside of pyramid maps.");
                    return;
                } else {
                    if (player.getPyramidSubway() == null || !player.getPyramidSubway().onSkillUse(player)) {
                        return;
                    }
                }
            } else if (GameConstants.isInflationSkill(attack.skill)) {
                if (player.getBuffedValue(MapleBuffStat.GIANT_POTION) == null) {
                    return;
                }
            }
        }
        if (player.getClient().getChannelServer().isAdminOnly()) {
            //player.dropMessage(-1, "Animation: " + Integer.toHexString(((attack.display & 0x8000) != 0 ? (attack.display - 0x8000) : attack.display)));
        }
        final boolean useAttackCount = attack.skill != 4211006 && attack.skill != 3221007 && attack.skill != 23121003 && (attack.skill != 1311001 || player.getJob() != 132) && attack.skill != 3211006;

        if (attack.hits > 0 && attack.targets > 0) {
            player.getStat().checkEquipDurabilitys(player, -1, false, false);
        }
        //final boolean useAttackCount = attack.skill != 4211006 && attack.skill != 3221007 && attack.skill != 23121003 && (attack.skill != 1311001 || player.getJob() != 132) && attack.skill != 3211006;
        //if (attack.hits > attackCount) {
        //    if (useAttackCount) { //buster
        //        player.getCheatTracker().registerOffense(CheatingOffense.MISMATCHING_BULLETCOUNT);
        //        return;
        //    }
        //}
        int totDamage = 0;
        final MapleMap map = player.getMap();
        // sql 에서 추뎀가져오기
        long additionalDamage = player.getRCDamage();

        if (attack.skill == 4211006) { // meso explosion
            for (AttackPair oned : attack.allDamage) {
                if (oned.attack != null) {
                    continue;
                }
                final MapleMapObject mapobject = map.getMapObject(oned.objectid, MapleMapObjectType.ITEM);

                if (mapobject != null) {
                    final MapleMapItem mapitem = (MapleMapItem) mapobject;
                    mapitem.getLock().lock();
                    try {
                        if (mapitem.getMeso() > 0) {
                            if (mapitem.isPickedUp()) {
                                return;
                            }
                            map.removeMapObject(mapitem);
                            map.broadcastMessage(MaplePacketCreator.explodeDrop(mapitem.getObjectId()));
                            mapitem.setPickedUp(true);
                        }
                    } finally {
                        mapitem.getLock().unlock();
                    }
                }
            }
        }
        int fixeddmg;
        long totDamageToOneMonster = 0;
        long hpMob = 0;
        final PlayerStats stats = player.getStat();

        int CriticalDamage = stats.passive_sharpeye_percent();
        int ShdowPartnerAttackPercentage = 0;
        if (attack_type == AttackType.RANGED_WITH_SHADOWPARTNER || attack_type == AttackType.NON_RANGED_WITH_MIRROR) {
            final MapleStatEffect shadowPartnerEffect = (player.getJob() == 433 || player.getJob() == 434) ? player.getStatForBuff(MapleBuffStat.미러이미징) : player.getStatForBuff(MapleBuffStat.SHADOWPARTNER);
            if (shadowPartnerEffect != null) {
                ShdowPartnerAttackPercentage += shadowPartnerEffect.getX();
            }
            attackCount /= 2; // hack xD
        }
        ShdowPartnerAttackPercentage *= (CriticalDamage + 100) / 100;
        if (attack.skill == 4221001) { //amplifyDamage
            ShdowPartnerAttackPercentage *= 10;
        }
        byte overallAttackCount; // Tracking of Shadow Partner additional damage.
        double maxDamagePerHit = 0;
        MapleMonster monster;
        MapleMonsterStats monsterstats;
        boolean Tempest;

        for (final AttackPair oned : attack.allDamage) {
            monster = map.getMonsterByOid(oned.objectid);

            if (monster != null && monster.getLinkCID() <= 0) {
                totDamageToOneMonster = 0;
                hpMob = monster.getMobMaxHp();
                monsterstats = monster.getStats();
                fixeddmg = monsterstats.getFixedDamage();
                Tempest = monster.getStatusSourceID(MonsterStatus.FREEZE) == 21120006 || attack.skill == 21120006/* || attack.skill == 1221011*/;

                if (!Tempest && !player.isGM()) {
                    if ((player.getJob() >= 3200 && player.getJob() <= 3212 && !monster.isBuffed(MonsterStatus.DAMAGE_IMMUNITY) && !monster.isBuffed(MonsterStatus.MAGIC_IMMUNITY) && !monster.isBuffed(MonsterStatus.MAGIC_DAMAGE_REFLECT)) || attack.skill == 3221007 || attack.skill == 23121003 || ((player.getJob() < 3200 || player.getJob() > 3212) && !monster.isBuffed(MonsterStatus.DAMAGE_IMMUNITY) && !monster.isBuffed(MonsterStatus.WEAPON_IMMUNITY) && !monster.isBuffed(MonsterStatus.WEAPON_DAMAGE_REFLECT))) {
                        maxDamagePerHit = CalculateMaxWeaponDamagePerHit(player, monster, attack, theSkill, effect, maxDamagePerMonster, CriticalDamage);
                    } else {
                        maxDamagePerHit = 1;
                    }
                }
                overallAttackCount = 0; // Tracking of Shadow Partner additional damage.
                Integer eachd;
                for (Pair<Integer, Boolean> eachde : oned.attack) {
                    eachd = eachde.left;
                    overallAttackCount++;

                    if (useAttackCount && overallAttackCount - 1 == attackCount) { // Is a Shadow partner hit so let's divide it once
                        maxDamagePerHit = (maxDamagePerHit / 100) * (ShdowPartnerAttackPercentage * (monsterstats.isBoss() ? stats.bossdam_r : stats.dam_r) / 100);
                    }
//                     System.out.println("Client damage : " + eachd + " Server : " + maxDamagePerHit);
                    if (fixeddmg != -1) {
                        if (monsterstats.getOnlyNoramlAttack()) {
                            eachd = attack.skill != 0 ? 0 : fixeddmg;
                        } else {
                            eachd = fixeddmg;
                        }
                    } else {
                        if (monsterstats.getOnlyNoramlAttack()) {
                            eachd = attack.skill != 0 ? 0 : Math.min(eachd, (int) maxDamagePerHit);  // Convert to server calculated damage
                        } else if (!player.isGM()) {
                            if (Tempest) { // Monster buffed with Tempest
                                if (eachd > monster.getMobMaxHp()) {
                                    eachd = (int) Math.min(monster.getMobMaxHp(), Integer.MAX_VALUE);

                                }
                            } else if ((player.getJob() >= 3200 && player.getJob() <= 3212 && !monster.isBuffed(MonsterStatus.DAMAGE_IMMUNITY) && !monster.isBuffed(MonsterStatus.MAGIC_IMMUNITY) && !monster.isBuffed(MonsterStatus.MAGIC_DAMAGE_REFLECT)) || attack.skill == 23121003 || ((player.getJob() < 3200 || player.getJob() > 3212) && !monster.isBuffed(MonsterStatus.DAMAGE_IMMUNITY) && !monster.isBuffed(MonsterStatus.WEAPON_IMMUNITY) && !monster.isBuffed(MonsterStatus.WEAPON_DAMAGE_REFLECT))) {
                                if (eachd > maxDamagePerHit) {
                                    //player.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE, "[Damage: " + eachd + ", Expected: " + maxDamagePerHit + ", Mob: " + monster.getId() + "] [Job: " + player.getJob() + ", Level: " + player.getLevel() + ", Skill: " + attack.skill + "]");
                                    if (attack.real) {
                                        //player.getCheatTracker().checkSameDamage(eachd, maxDamagePerHit);
                                    }
                                    if (eachd > maxDamagePerHit * 2) {
                                        //player.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE_2, "[Damage: " + eachd + ", Expected: " + maxDamagePerHit + ", Mob: " + monster.getId() + "] [Job: " + player.getJob() + ", Level: " + player.getLevel() + ", Skill: " + attack.skill + "]");
                                        //eachd = (int) (maxDamagePerHit * 2); // Convert to server calculated damage
                                    }
                                }
                            } else {
                                if (eachd > maxDamagePerHit) {
                                    eachd = (int) (maxDamagePerHit);
                                }
                            }
                        }
                    }
                    if (player == null) { // o_O
                        return;
                    }
                    totDamageToOneMonster = Math.max(0, Math.min(Math.abs(totDamageToOneMonster + eachd), Long.MAX_VALUE));
                    //force the miss even if they dont miss. popular wz edit
                    if ((eachd == 0 || monster.getId() == 9700021) && player.getPyramidSubway() != null) { //miss
                        player.getPyramidSubway().onMiss(player);
                    }
                    /*if (player.getLevel() < 230 && player.getJob() != 322) { //레벨별 리신 시작 - 200이하 
                     if (eachd > 1000000000) {//스나이핑 맥뎀 199999, 6억이상 박을 시 리신 

                     //MapleQuestStatus qs = player.getQuestNAdd(MapleQuest.getInstance(170983));
                     World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(2, "[핵감지 시스템] : " + player.getName() + " (이)가 (데미지핵 사용) 정지 완료."));
                     AutobanManager.getInstance().autoban(player.getClient(), "데미지핵");
                     player.getClient().getSession().close();
                     return;
                     }
                     }*/
                }
                if (player.getdamagecount()) {
                    if (/*attack.skill != 1221011 && */attack.skill != 3221007 && attack.skill != 4341002) {
                        player.gainnowdamagetotal(totDamageToOneMonster + additionalDamage);
                        player.getMap().broadcastMessage(MaplePacketCreator.removeMapEffect());
                        player.getMap().startSimpleMapEffect("누적 데미지 : " + player.getNowDamage(), 5120026);
                        totDamageToOneMonster = 0;
                        totDamage = 0;
                    } else {
                        player.dropMessage(5, "생츄어리,스나이핑,파이널컷 은 적립되지 않습니다.");
                    }
                }
                player.checkMonsterAggro(monster);

                if (!player.gethottimebossattackcheck()) {
                    player.sethottimebossattackcheck(true);
                }

                if (GameConstants.getAttackDelay(attack.skill, theSkill) >= 100 && !GameConstants.isNoDelaySkill(attack.skill) && attack.skill != 3101005 && !monster.getStats().isBoss() && player.getTruePosition().distanceSq(monster.getTruePosition()) > GameConstants.getAttackRange(effect, player.getStat().defRange)) {
                    player.getCheatTracker().registerOffense(CheatingOffense.ATTACK_FARAWAY_MONSTER, "[Distance: " + player.getTruePosition().distanceSq(monster.getTruePosition()) + ", Expected Distance: " + GameConstants.getAttackRange(effect, player.getStat().defRange) + " Job: " + player.getJob() + "]"); // , Double.toString(Math.sqrt(distance))

                }
                // pickpocket
                if (player.getBuffedValue(MapleBuffStat.PICKPOCKET) != null) {
                    switch (attack.skill) {
                        case 0:
                        case 4001334:
                        case 4201005:
                        case 4211002:
                        case 4211004:
                        case 4221003:
                        case 4221007:
                        case 4221005:
                            handlePickPocket(player, monster, oned);
                            break;
                    }
                }
                if (player.getBuffedValue(MapleBuffStat.WIND_WALK) != null) {
                    player.cancelEffectFromBuffStat(MapleBuffStat.WIND_WALK);
                }
                if (totDamageToOneMonster > 0 || /*attack.skill == 1221011 || */ attack.skill == 21120006) {
                    if (attack.skill != 122101100) { // 1221011
                        // 기본 데미지 적용
                        applyChunkedDamage(player, monster, totDamageToOneMonster, attack.skill);

                        // 추가 데미지 적용
                        applyChunkedDamage(player, monster, additionalDamage, attack.skill);

                        // 데미지 적용 후 결과를 출력
                        if (player.isShowDamageHint()) {
                            long afterDamage = totDamageToOneMonster + additionalDamage;
                            player.getClient().getSession().write(MaplePacketCreator.sendHint("#e< #r데미지 정보#k >#n\r\n현재 추가 데미지: " + player.getNum(additionalDamage) + "\r\n적용 전: #d" + player.getNum(totDamageToOneMonster) + " 데미지#k\r\n적용 후: #b" + player.getNum(afterDamage) + " 데미지#k\r\n", 240, 5));
                        }

                        long summonAddDAM = 0;
                        MapleQuestStatus summonDAMData = player.getQuestNAdd(MapleQuest.getInstance(202306121));
                        if (summonDAMData != null) {
                            String data = null;
                            if ((data = summonDAMData.getCustomData()) != null) {
                                try {
                                    int summonDAM = Integer.parseInt(data);
                                    summonAddDAM += (int) Math.max(0, Math.min(totDamageToOneMonster * (summonDAM * 0.01), Integer.MAX_VALUE));
                                } catch (NumberFormatException e) {
                                    // 에러 처리
                                }
                            }
                        }
                        if (summonAddDAM > 0) {
                            monster.damage(player, summonAddDAM, true, attack.skill);
                        }
                    } else {
                        monster.damage(player, (monster.getStats().isBoss() ? 500000 : (monster.getHp() + 0)), true, attack.skill);
                    }

                    if (monster.isBuffed(MonsterStatus.WEAPON_DAMAGE_REFLECT)) {
                        player.addHP(-(7000 + Randomizer.nextInt(8000)));
                    }
                    player.onAttack(monster.getMobMaxHp(), monster.getMobMaxMp(), attack.skill, monster.getObjectId(), totDamage);

                    switch (attack.skill) {
                        case 14001004:
                        case 14111002:
                        case 14111005:
                        case 4301001:
                        case 4311002:
                        case 4311003:
                        case 4331000:
                        case 4331004:
                        case 4331005:
                        case 4341002:
                        case 4341004:
                        case 4341005:
                        case 4331006:
                        case 4341009:
                        case 4221005:
                        case 4221007: // Boomerang Stab
                        case 4221001: // Assasinate
                        case 4211002: // Assulter
                        case 4201005: // Savage Blow
                        case 4001002: // Disorder
                        case 4001334: // Double Stab
                        case 4121007: // Triple Throw
                        case 4111005: // Avenger
                        case 4001344: { // Lucky Seven
                            // Venom
                            int[] skills = {4120005, 4220005, 4340001, 14110004};
                            for (int i : skills) {
                                final Skill skill = SkillFactory.getSkill(i);
                                if (player.getTotalSkillLevel(skill) > 0) {
                                    final MapleStatEffect venomEffect = skill.getEffect(player.getTotalSkillLevel(skill));
                                    if (venomEffect.makeChanceResult()) {
                                        //player.dropMessage(5, "applied venom!");
                                        monster.applyStatus(player, new MonsterStatusEffect(MonsterStatus.POISON, 1, i, null, false), true, venomEffect.getDuration(), true, venomEffect);
                                    }
                                    break;
                                }
                            }

                            break;
                        }
                        case 4201004: { //steal
                            monster.handleSteal(player);
                            break;
                        }
                        //case 21101003: // body pressure
                        case 21000002: // Double attack
                        case 21100001: // Triple Attack
                        case 21101002:
//                        case 21100002: // Pole Arm Push
                        case 21101004:
//                        case 21100004: // Pole Arm Smash
                        case 21110002: // Full Swing
                        case 21110003: // Pole Arm Toss
                        case 21111004:
//                        case 21110004: // Fenrir Phantom
                        //   case 21110006: // Whirlwind
                        case 21110007: // (hidden) Full Swing - Double Attack
                        case 21110008: // (hidden) Full Swing - Triple Attack
                        case 21120002: // Overswing
                        case 21121005:
//                        case 21120005: // Pole Arm finale
                        case 21120006: // Tempest
                        case 21120009: // (hidden) Overswing - Double Attack
                        case 21120010: { // (hidden) Overswing - Triple Attack
                            if (player.getBuffedValue(MapleBuffStat.WK_CHARGE) != null && !monster.getStats().isBoss()) {
                                final MapleStatEffect eff = player.getStatForBuff(MapleBuffStat.WK_CHARGE);
                                if (eff != null) {
                                    monster.applyStatus(player, new MonsterStatusEffect(MonsterStatus.SPEED, eff.getX(), eff.getSourceId(), null, false), false, eff.getY() * 1000, true, eff);
                                }
                            }
                            if (player.getBuffedValue(MapleBuffStat.BODY_PRESSURE) != null && !monster.getStats().isBoss()) {
                                final MapleStatEffect eff = player.getStatForBuff(MapleBuffStat.BODY_PRESSURE);

                                if (eff != null && eff.makeChanceResult() && !monster.isBuffed(MonsterStatus.NEUTRALISE)) {
                                    monster.applyStatus(player, new MonsterStatusEffect(MonsterStatus.NEUTRALISE, 1, eff.getSourceId(), null, false), false, eff.getX() * 1000, true, eff);
                                }
                            }
                            break;
                        }
                        default: //passives attack bonuses
                            break;
                    }
                    if (totDamageToOneMonster > 0) {
                        Item weapon_ = player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
                        if (weapon_ != null) {
                            MonsterStatus stat = GameConstants.getStatFromWeapon(weapon_.getItemId()); //10001 = acc/darkness. 10005 = speed/slow.
                            if (stat != null && Randomizer.nextInt(100) < GameConstants.getStatChance()) {
                                final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(stat, GameConstants.getXForStat(stat), GameConstants.getSkillForStat(stat), null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, 10000, false, null);
                            }
                        }
                        /*임시처리*/
                        int poten1 = equip.getPotential1();
                        int poten2 = equip.getPotential2();
                        int poten3 = equip.getPotential3();
                        final int itemLevel = ii.getReqLevel(equip.getItemId());
                        if (poten1 == 10241 || poten2 == 10241 || poten3 == 10241) {//빙결옵션
                            final Skill skill = SkillFactory.getSkill(1211006);
                            final MapleStatEffect eff = skill.getEffect(1);
                            if (Randomizer.nextInt(100) <= 5) { //5% 확률                            
                                final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.FREEZE, 1, skill.getId(), null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, itemLevel < 71 ? 1000 : 2000, true, eff); //2레벨 빙결효과
                            }
                        }
                        if (poten1 == 10221 || poten2 == 10221 || poten3 == 10221) { //포이즌
                            final Skill skill = SkillFactory.getSkill(2101005);
                            final MapleStatEffect eff = skill.getEffect(1);
                            int randomdamage = Randomizer.nextInt(3);
                            if (Randomizer.nextInt(100) <= 10) { //10% 확률                            
                                final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.POISON, (3 * (equip.getMatk() == 0 ? equip.getWatk() : equip.getWatk() != 0 ? (int) (equip.getWatk() + equip.getMatk() / 1.4) : equip.getMatk()) * randomdamage), skill.getId(), null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, itemLevel < 21 ? 1000 : itemLevel < 41 ? 2000 : itemLevel < 61 ? 3000 : itemLevel < 81 ? 4000 : itemLevel < 101 ? 5000 : 6000, true, eff); //2레벨 포이즌효과?
                            }
                        }
                        if (poten1 == 10226 || poten2 == 10226 || poten3 == 10226) {//스턴
                            final Skill skill = SkillFactory.getSkill(1121001);
                            final MapleStatEffect eff = skill.getEffect(1);
                            if (Randomizer.nextInt(100) <= 5) { //5% 확률                            
                                final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.STUN, 1, skill.getId(), null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, itemLevel < 71 ? 1000 : 2000, true, eff); //스턴효과
                            }
                        }
                        if (poten1 == 10231 || poten2 == 10231 || poten3 == 10231) {//슬로우
                            final Skill skill = SkillFactory.getSkill(2101003);
                            final MapleStatEffect eff = skill.getEffect(1);
                            final int slowX = itemLevel < 71 ? 10 : 20;
                            if (Randomizer.nextInt(100) <= 10) { //10% 확률                            
                                final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.SPEED, -4 * slowX, skill.getId(), null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, itemLevel < 71 ? 2000 : 4000, true, eff); //슬로우
                            }
                        }
                        if (poten1 == 10236 || poten2 == 10236 || poten3 == 10236) {//암흑
                            final Skill skill = SkillFactory.getSkill(3221006);
                            final MapleStatEffect eff = skill.getEffect(1);
                            final int blindX = itemLevel < 71 ? 10 : itemLevel < 101 ? 20 : 30;
                            if (Randomizer.nextInt(100) <= 10) { //10% 확률                            
                                final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.ACC, blindX, skill.getId(), null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, itemLevel < 71 ? 2000 : itemLevel < 101 ? 4000 : 9000, true, eff); //암흑
                            }
                        }
                        if (poten1 == 10246 || poten2 == 10246 || poten3 == 10246) {//봉인
                            final Skill skill = SkillFactory.getSkill(2111004);
                            final MapleStatEffect eff = skill.getEffect(1);
                            if (Randomizer.nextInt(100) <= 5) { //5% 확률                            
                                final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.SEAL, 1, skill.getId(), null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, itemLevel < 71 ? 1000 : 2000, true, eff); //봉인
                            }
                        }
                        if (player.getBuffedValue(MapleBuffStat.BLIND) != null) {
                            final MapleStatEffect eff = player.getStatForBuff(MapleBuffStat.BLIND);

                            if (eff != null && eff.makeChanceResult()) {
                                final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.ACC, eff.getX(), eff.getSourceId(), null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, eff.getY() * 1000, true, eff);
                            }

                        }
                        if (player.getBuffedValue(MapleBuffStat.HAMSTRING) != null) {
                            final MapleStatEffect eff = player.getStatForBuff(MapleBuffStat.HAMSTRING);

                            if (eff != null && eff.makeChanceResult()) {
                                final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.SPEED, eff.getX(), 3121007, null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, eff.getY() * 1000, true, eff);
                            }
                        }
                        if (player.getJob() == 121 || player.getJob() == 122) { // WHITEKNIGHT
                            final Skill skill = SkillFactory.getSkill(1211006);
                            if (player.isBuffFrom(MapleBuffStat.WK_CHARGE, skill)) {
                                final MapleStatEffect eff = skill.getEffect(player.getTotalSkillLevel(skill));
                                final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.FREEZE, 1, skill.getId(), null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, eff.getY() * 2000, true, eff);
                            }
                        }
                    }
                    if (effect != null && effect.getMonsterStati().size() > 0) {
                        if (effect.makeChanceResult()) {
                            for (Map.Entry<MonsterStatus, Integer> z : effect.getMonsterStati().entrySet()) {
                                monster.applyStatus(player, new MonsterStatusEffect(z.getKey(), z.getValue(), theSkill.getId(), null, false), effect.isPoison(), effect.getDuration(), true, effect);
                            }
                        }
                    }

                    /*if (attack.skill == 1211002) { //Charge Blow
                     //Handle when player uses charge blow
                     //charge must dispel. but if player has learned advanced charge, don't dispel.
                     Skill advancedCharge = SkillFactory.getSkill(1220010);
                     boolean mustDispel = true;
                     if (player.getSkillLevel(advancedCharge) > 0) {
                     int x = advancedCharge.getEffect(player.getSkillLevel(advancedCharge)).getX();
                     mustDispel = Randomizer.nextInt(100) > x;
                     }
                     if (mustDispel) {
                     player.cancelEffectFromBuffStat(MapleBuffStat.WK_CHARGE);
                     player.getMap().broadcastMessage(MaplePacketCreator.cancelForeignBuff(player.getId(), Collections.singletonList(MapleBuffStat.WK_CHARGE)));
                     }
                     }*/
                }
            }
        }
        if (attack.skill == 4331003 && (hpMob <= 0 || totDamageToOneMonster < hpMob)) {
            return;
        }
        if (hpMob > 0 && totDamageToOneMonster > 0) {
            player.afterAttack(attack.targets, attack.hits, attack.skill);
        }//3512101335121013
        if (attack.skill != 3221007
                && attack.skill != 32121003
                && attack.skill != 15111006
                && attack.skill != 35121003
                && attack.skill != 35111004
                && attack.skill != 35121005
                && attack.skill != 35121013
                && attack.skill != 32001003 //dark aura
                && attack.skill != 32120000
                && attack.skill != 32101002 //blue aura
                && attack.skill != 32110000
                && attack.skill != 32101003 //yellow aura
                && attack.skill != 32120001) {
            effect.applyTo(player, attack.position);
        }
        if (totDamage > 1 && GameConstants.getAttackDelay(attack.skill, theSkill) >= 100) {
            final CheatTracker tracker = player.getCheatTracker();

            tracker.setAttacksWithoutHit(true);
        }
    }

    public static final void applyAttackMagic(final AttackInfo attack, final Skill theSkill, final MapleCharacter player, final MapleStatEffect effect, double maxDamagePerHit) {
        if (!player.isAlive()) {
            player.getCheatTracker().registerOffense(CheatingOffense.ATTACKING_WHILE_DEAD);
            return;
        }
//	if (attack.skill != 2301002) { // heal is both an attack and a special move (healing) so we'll let the whole applying magic live in the special move part
//	    effect.applyTo(player);
//	}
        if (attack.hits > effect.getAttackCount() || attack.targets > effect.getMobCount()) {
            player.getCheatTracker().registerOffense(CheatingOffense.MISMATCHING_BULLETCOUNT);
            return;
        }
        if (attack.hits > 0 && attack.targets > 0) {
            player.getStat().checkEquipDurabilitys(player, -1, false, false);
        }
        if (GameConstants.getAttackDelay(attack.skill, theSkill) >= 100) {
            player.getCheatTracker().checkAttack(attack.skill, attack.lastAttackTickCount);
        }
        if (GameConstants.isMulungSkill(attack.skill)) {
            if (player.getMapId() / 10000 != 92502) {
                //AutobanManager.getInstance().autoban(player.getClient(), "Using Mu Lung dojo skill out of dojo maps.");
                return;
            } else {
                if (player.getMulungEnergy() < 10000) {
                    return;
                }
                player.mulung_EnergyModify(false);
            }
        } else if (GameConstants.isPyramidSkill(attack.skill)) {
            if (player.getMapId() / 1000000 != 926) {
                //AutobanManager.getInstance().autoban(player.getClient(), "Using Pyramid skill outside of pyramid maps.");
                return;
            } else {
                if (player.getPyramidSubway() == null || !player.getPyramidSubway().onSkillUse(player)) {
                    return;
                }
            }
            if (attack.targets > effect.getMobCount() && attack.skill != 1211002 && attack.skill != 1220010) { // 위젯 몹카운드 체크인데 흠....
                player.getCheatTracker().registerOffense(CheatingOffense.MISMATCHING_BULLETCOUNT);
                return;
            }
        } else if (GameConstants.isInflationSkill(attack.skill)) {
            if (player.getBuffedValue(MapleBuffStat.GIANT_POTION) == null) {
                return;
            }
        }
        if (player.getClient().getChannelServer().isAdminOnly()) {
            //player.dropMessage(-1, "Animation: " + Integer.toHexString(((attack.display & 0x8000) != 0 ? (attack.display - 0x8000) : attack.display)));
        }
        final PlayerStats stats = player.getStat();
        final Element element = player.getBuffedValue(MapleBuffStat.ELEMENT_RESET) != null ? Element.NEUTRAL : theSkill.getElement();
        double MaxDamagePerHit = 0;
        long totDamageToOneMonster;
        int totDamage = 0, fixeddmg;
        byte overallAttackCount;
        boolean Tempest;
        MapleMonsterStats monsterstats;
        int CriticalDamage = stats.passive_sharpeye_percent();
        final Skill eaterSkill = SkillFactory.getSkill(GameConstants.getMPEaterForJob(player.getJob()));
        final int eaterLevel = player.getTotalSkillLevel(eaterSkill);

        if (attack.hits > 0 && attack.targets > 0) {
            player.getStat().checkEquipDurabilitys(player, -1, false, false);
        }
        final MapleMap map = player.getMap();
        // sql 에서 추뎀가져오기
        long additionalDamage = player.getRCDamage();
        for (final AttackPair oned : attack.allDamage) {
            final MapleMonster monster = map.getMonsterByOid(oned.objectid);

            if (monster != null && monster.getLinkCID() <= 0) {
                Tempest = monster.getStatusSourceID(MonsterStatus.FREEZE) == 21120006 && !monster.getStats().isBoss();
                totDamageToOneMonster = 0;
                monsterstats = monster.getStats();
                fixeddmg = monsterstats.getFixedDamage();
                if (!Tempest && !player.isGM()) {
                    if (!monster.isBuffed(MonsterStatus.MAGIC_IMMUNITY) && !monster.isBuffed(MonsterStatus.MAGIC_DAMAGE_REFLECT)) {
                        MaxDamagePerHit = CalculateMaxMagicDamagePerHit(player, theSkill, monster, monsterstats, stats, element, CriticalDamage, maxDamagePerHit, effect);
                    } else {
                        MaxDamagePerHit = 1;
                    }
                }
                overallAttackCount = 0;
                Integer eachd;
                for (Pair<Integer, Boolean> eachde : oned.attack) {
                    eachd = eachde.left;
                    overallAttackCount++;
                    if (fixeddmg != -1) {
                        eachd = monsterstats.getOnlyNoramlAttack() ? 0 : fixeddmg; // Magic is always not a normal attack
                    } else {
                        if (monsterstats.getOnlyNoramlAttack()) {
                            eachd = 0; // Magic is always not a normal attack
                        } else if (!player.isGM()) {
//			    System.out.println("Client damage : " + eachd + " Server : " + MaxDamagePerHit);

                            if (Tempest) { // Buffed with Tempest
                                // In special case such as Chain lightning, the damage will be reduced from the maxMP.
                                if (eachd > monster.getMobMaxHp()) {
                                    eachd = (int) Math.min(monster.getMobMaxHp(), Integer.MAX_VALUE);
                                    player.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE_MAGIC);
                                }
                            } else if (!monster.isBuffed(MonsterStatus.MAGIC_IMMUNITY) && !monster.isBuffed(MonsterStatus.MAGIC_DAMAGE_REFLECT)) {
                                if (eachd > MaxDamagePerHit) {
                                    player.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE_MAGIC, "[Damage: " + eachd + ", Expected: " + MaxDamagePerHit + ", Mob: " + monster.getId() + "] [Job: " + player.getJob() + ", Level: " + player.getLevel() + ", Skill: " + attack.skill + "]");
                                    if (attack.real) {
                                        //player.getCheatTracker().checkSameDamage(eachd, MaxDamagePerHit);
                                    }
                                    if (eachd > MaxDamagePerHit * 2) {
//				    System.out.println("EXCEED!!! Client damage : " + eachd + " Server : " + MaxDamagePerHit);
                                        player.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE_MAGIC_2, "[Damage: " + eachd + ", Expected: " + MaxDamagePerHit + ", Mob: " + monster.getId() + "] [Job: " + player.getJob() + ", Level: " + player.getLevel() + ", Skill: " + attack.skill + "]");
                                        //eachd = (int) (MaxDamagePerHit * 2); // Convert to server calculated damage
                                    }
                                }
                            } else {
                                if (eachd > MaxDamagePerHit) {
                                    eachd = (int) (MaxDamagePerHit);
                                }
                            }
                        }
                    }
                    totDamageToOneMonster = Math.max(0, Math.min(Math.abs(totDamageToOneMonster + eachd), Long.MAX_VALUE));
                }
                totDamage = (int) Math.max(0, Math.min(Math.abs(totDamage + totDamageToOneMonster), Integer.MAX_VALUE));
                if (player.getdamagecount()) {
                    if (/*attack.skill != 1221011 && */attack.skill != 3221007 && attack.skill != 4341002) {
                        player.gainnowdamagetotal(totDamageToOneMonster);
                        player.getMap().broadcastMessage(MaplePacketCreator.removeMapEffect()); // 5120024
                        player.getMap().startSimpleMapEffect("누적 데미지 : " + player.getNowDamage(), 5120026);
                        totDamageToOneMonster = 0;
                        totDamage = 0;
                    } else {
                        player.dropMessage(5, "생츄어리,스나이핑,파이널컷 은 적립되지 않습니다.");
                    }
                }
                player.checkMonsterAggro(monster);

                if (!player.gethottimebossattackcheck()) {
                    player.sethottimebossattackcheck(true);
                }

                if (GameConstants.getAttackDelay(attack.skill, theSkill) >= 100 && !GameConstants.isNoDelaySkill(attack.skill) && !monster.getStats().isBoss() && player.getTruePosition().distanceSq(monster.getTruePosition()) > GameConstants.getAttackRange(effect, player.getStat().defRange)) {
                    player.getCheatTracker().registerOffense(CheatingOffense.ATTACK_FARAWAY_MONSTER, "[Distance: " + player.getTruePosition().distanceSq(monster.getTruePosition()) + ", Expected Distance: " + GameConstants.getAttackRange(effect, player.getStat().defRange) + " Job: " + player.getJob() + "]"); // , Double.toString(Math.sqrt(distance))
                }
                if (attack.skill == 2301002 && !monsterstats.getUndead()) {
                    player.getCheatTracker().registerOffense(CheatingOffense.HEAL_ATTACKING_UNDEAD);
                    return;
                }

//                int fixDAM = 0;
//                int userMapID = player.getMapId();
                //특정 맵코드로 하는 곳
//                switch (userMapID) {
//                    case 0: //원하는 특정 맵 코드 추가
//                        fixDAM = 40000000; //이러면 0 맵에서 데미지 1로 박힘
//                        break;
//                }
                //맵코드 구간
//                if (userMapID >= 600000000 && userMapID <= 600010130) {
//                    //이러면 0 맵부터 100000000 미만까지 데미지 1로 박힘
//                    fixDAM = 40000000;
//                }
//                
//                if (fixDAM > 0) { //고정 뎀 맵인 경우
//                    switch (attack.skill) {
//                        case 2121007:
//                        case 2221007: //블리자드
//                        case 2321008:
//                            break;
//                        default: {
//                            fixDAM = 0; //위에 스킬들이 아닌 경우 고정 뎀 X
//                            break;
//                        }
//                    }
//                }
//                if (fixDAM > 0) {
//                    totDamage = (int) Math.max(0, Math.min(totDamage - totDamageToOneMonster, Integer.MAX_VALUE));
//                    totDamageToOneMonster = fixDAM;
//                }
                if (totDamageToOneMonster > 0) {
                    // 기본 데미지 적용
                    applyChunkedDamage(player, monster, totDamageToOneMonster, attack.skill);

// 추가 데미지 적용
                    applyChunkedDamage(player, monster, additionalDamage, attack.skill);

                    // 데미지 적용 후 결과를 출력
                    if (player.isShowDamageHint()) {
                        long afterDamage = totDamageToOneMonster + additionalDamage;
                        player.getClient().getSession().write(MaplePacketCreator.sendHint("#e< #r데미지 정보#k >#n\r\n현재 추가 데미지: " + additionalDamage + "\r\n적용 전: #d" + totDamageToOneMonster + " 데미지#k\r\n적용 후: #b" + afterDamage + " 데미지#k\r\n", 240, 5));
                    }
                    long summonAddDAM = 0;
                    MapleQuestStatus summonDAMData = player.getQuestNAdd(MapleQuest.getInstance(202306121));
                    if (summonDAMData != null) {
                        String data = null;
                        if ((data = summonDAMData.getCustomData()) != null) {
                            try {
                                int summonDAM = Integer.parseInt(data);
                                summonAddDAM += (int) Math.max(0, Math.min(totDamageToOneMonster * (summonDAM * 0.01), Integer.MAX_VALUE));
                            } catch (NumberFormatException e) {

                            }
                        }
                    }
                    if (summonAddDAM > 0) {
                        monster.damage(player, summonAddDAM, true, attack.skill);
                    }

                    if (monster.isBuffed(MonsterStatus.MAGIC_DAMAGE_REFLECT)) { //test
                        player.addHP(-(7000 + Randomizer.nextInt(8000))); //this is what it seems to be?
                    }
                    if (player.getBuffedValue(MapleBuffStat.SLOW) != null) {
                        final MapleStatEffect eff = player.getStatForBuff(MapleBuffStat.SLOW);

                        if (eff != null && eff.makeChanceResult() && !monster.isBuffed(MonsterStatus.SPEED)) {
                            monster.applyStatus(player, new MonsterStatusEffect(MonsterStatus.SPEED, eff.getX(), eff.getSourceId(), null, false), false, eff.getY() * 1000, true, eff);
                        }
                    }
                    //if (monster.isBuffed(MonsterStatus.WEAPON_DAMAGE_REFLECT)) { //test
                    //    player.addHP(-(7000 + Randomizer.nextInt(8000))); //this is what it seems to be?
                    //}
                    player.onAttack(monster.getMobMaxHp(), monster.getMobMaxMp(), attack.skill, monster.getObjectId(), totDamage);
                    // effects, reversed after bigbang
                    switch (attack.skill) {
                        case 2221003:
                            monster.setTempEffectiveness(Element.ICE, effect.getDuration());
                            break;
                        case 2121003:
                            monster.setTempEffectiveness(Element.FIRE, effect.getDuration());
                            break;
                    }
                    if (effect != null && effect.getMonsterStati().size() > 0) {
                        if (effect.makeChanceResult()) {
                            for (Map.Entry<MonsterStatus, Integer> z : effect.getMonsterStati().entrySet()) {
                                monster.applyStatus(player, new MonsterStatusEffect(z.getKey(), z.getValue(), theSkill.getId(), null, false), effect.isPoison(), effect.getDuration(), true, effect);
                            }
                        }
                    }
                    if (eaterLevel > 0) {
                        eaterSkill.getEffect(eaterLevel).applyPassive(player, monster);
                    }
                }
            }
        }
        if (attack.skill != 2301002) {
            effect.applyTo(player);
        }

        if (totDamage > 1 && GameConstants.getAttackDelay(attack.skill, theSkill) >= 100) {
            final CheatTracker tracker = player.getCheatTracker();
            tracker.setAttacksWithoutHit(true);
        }
    }

    private static void applyChunkedDamage(final MapleCharacter player, final MapleMonster monster, long damage, int skillId) {
        // 청크당 21억
        final long maxDamage = 2100000000L;
        while (damage > 0) {
            long damageToApply = Math.min(maxDamage, damage);
            monster.damage(player, (int) Math.min(damageToApply, Integer.MAX_VALUE), true, skillId);
            damage -= damageToApply;
        }
    }

    private static final double CalculateMaxMagicDamagePerHit(final MapleCharacter chr, final Skill skill, final MapleMonster monster, final MapleMonsterStats mobstats, final PlayerStats stats, final Element elem, final Integer sharpEye, final double maxDamagePerMonster, final MapleStatEffect attackEffect) {
        final int dLevel = Math.max(mobstats.getLevel() - chr.getLevel(), 0) * 2;
        int HitRate = Math.min((int) Math.floor(Math.sqrt(stats.getAccuracy())) - (int) Math.floor(Math.sqrt(mobstats.getEva())) + 100, 100);
        if (dLevel > HitRate) {
            HitRate = dLevel;
        }
        HitRate -= dLevel;
        if (HitRate <= 0 && !(GameConstants.isBeginnerJob(skill.getId() / 10000) && skill.getId() % 10000 == 1000)) { // miss :P or HACK :O
            return 0;
        }
        double elemMaxDamagePerMob;
        int CritPercent = sharpEye;
        final ElementalEffectiveness ee = monster.getEffectiveness(elem);
        switch (ee) {
            case IMMUNE:
                elemMaxDamagePerMob = 1;
                break;
            default:
                elemMaxDamagePerMob = ElementalStaffAttackBonus(elem, maxDamagePerMonster * ee.getValue(), stats);
                break;
        }
        // Calculate monster magic def
        // Min damage = (MIN before defense) - MDEF*.6
        // Max damage = (MAX before defense) - MDEF*.5
        int MDRate = monster.getStats().getMDRate();
        MonsterStatusEffect pdr = monster.getBuff(MonsterStatus.MDEF);
        if (pdr != null) {
            MDRate += pdr.getX();
        }
        elemMaxDamagePerMob -= elemMaxDamagePerMob * (Math.max(MDRate - stats.ignoreTargetDEF - attackEffect.getIgnoreMob(), 0) / 100.0);
        // Calculate Sharp eye bonus
        elemMaxDamagePerMob += ((double) elemMaxDamagePerMob / 100.0) * CritPercent;
//	if (skill.isChargeSkill()) {
//	    elemMaxDamagePerMob = (float) ((90 * ((System.currentTimeMillis() - chr.getKeyDownSkill_Time()) / 1000) + 10) * elemMaxDamagePerMob * 0.01);
//	}
//      if (skill.isChargeSkill() && chr.getKeyDownSkill_Time() == 0) {
//          return 1;
//      }
        elemMaxDamagePerMob *= (monster.getStats().isBoss() ? chr.getStat().bossdam_r : chr.getStat().dam_r) / 100.0;
        final MonsterStatusEffect imprint = monster.getBuff(MonsterStatus.IMPRINT);
        if (imprint != null) {
            elemMaxDamagePerMob += (elemMaxDamagePerMob * imprint.getX() / 100.0);
        }
        elemMaxDamagePerMob += (elemMaxDamagePerMob * chr.getDamageIncrease(monster.getObjectId()) / 100.0);
        if (GameConstants.isBeginnerJob(skill.getId() / 10000)) {
            switch (skill.getId() % 10000) {
                case 1000:
                    elemMaxDamagePerMob = 40;
                    break;
                case 1020:
                    elemMaxDamagePerMob = 1;
                    break;
                case 1009:
                    elemMaxDamagePerMob = (monster.getStats().isBoss() ? monster.getMobMaxHp() / 30 * 100 : monster.getMobMaxHp());
                    break;
            }
        }
        switch (skill.getId()) {
            case 32001000:
            case 32101000:
            case 32111002:
            case 32121002:
            case 32121010:
                elemMaxDamagePerMob *= 1.5;
                break;
        }
        if (elemMaxDamagePerMob > 9100000000L) {//맥뎀
            elemMaxDamagePerMob = 9100000000L;
        } else if (elemMaxDamagePerMob <= 0) {
            elemMaxDamagePerMob = 1;
        }

        return elemMaxDamagePerMob;
    }

    private static final double ElementalStaffAttackBonus(final Element elem, double elemMaxDamagePerMob, final PlayerStats stats) {
        switch (elem) {
            case FIRE:
                return (elemMaxDamagePerMob / 100) * (stats.element_fire + stats.getElementBoost(elem));
            case ICE:
                return (elemMaxDamagePerMob / 100) * (stats.element_ice + stats.getElementBoost(elem));
            case LIGHTING:
                return (elemMaxDamagePerMob / 100) * (stats.element_light + stats.getElementBoost(elem));
            case POISON:
                return (elemMaxDamagePerMob / 100) * (stats.element_psn + stats.getElementBoost(elem));
            default:
                return (elemMaxDamagePerMob / 100) * (stats.def + stats.getElementBoost(elem));
        }
    }

    private static void handlePickPocket(final MapleCharacter player, final MapleMonster mob, AttackPair oned) {
        final int maxmeso = player.getBuffedValue(MapleBuffStat.PICKPOCKET).intValue();

        for (final Pair<Integer, Boolean> eachde : oned.attack) {
            final Integer eachd = eachde.left;
            if (player.getStat().pickRate >= 100 || Randomizer.nextInt(99) < player.getStat().pickRate) {
                player.getMap().spawnMesoDrop(Math.min((int) Math.max(((double) eachd / (double) 20000) * (double) maxmeso, (double) 1), maxmeso), new Point((int) (mob.getTruePosition().getX() + Randomizer.nextInt(100) - 50), (int) (mob.getTruePosition().getY())), mob, player, true, (byte) 0);
            }
        }
    }

    private static double CalculateMaxWeaponDamagePerHit(final MapleCharacter player, final MapleMonster monster, final AttackInfo attack, final Skill theSkill, final MapleStatEffect attackEffect, double maximumDamageToMonster, final Integer CriticalDamagePercent) {
        final int dLevel = Math.max(monster.getStats().getLevel() - player.getLevel(), 0) * 2;
        int HitRate = Math.min((int) Math.floor(Math.sqrt(player.getStat().getAccuracy())) - (int) Math.floor(Math.sqrt(monster.getStats().getEva())) + 100, 100);
        if (dLevel > HitRate) {
            HitRate = dLevel;
        }
        HitRate -= dLevel;
        if (HitRate <= 0 && !(GameConstants.isBeginnerJob(attack.skill / 10000) && attack.skill % 10000 == 1000) && !GameConstants.isPyramidSkill(attack.skill) && !GameConstants.isMulungSkill(attack.skill) && !GameConstants.isInflationSkill(attack.skill)) { // miss :P or HACK :O
            return 0;
        }
        if (player.getMapId() / 1000000 == 914 || player.getMapId() / 1000000 == 927) { //aran
            return 2100000000;
        }

        List<Element> elements = new ArrayList<Element>();
        boolean defined = false;
        int CritPercent = CriticalDamagePercent;
        int PDRate = monster.getStats().getPDRate();
        MonsterStatusEffect pdr = monster.getBuff(MonsterStatus.WDEF);
        if (pdr != null) {
            PDRate += pdr.getX(); //x will be negative usually
        }
        if (theSkill != null) {
            elements.add(theSkill.getElement());
            if (GameConstants.isBeginnerJob(theSkill.getId() / 10000)) {
                switch (theSkill.getId() % 10000) {
                    case 1000:
                        maximumDamageToMonster = 40;
                        defined = true;
                        break;
                    case 1020:
                        maximumDamageToMonster = 1;
                        defined = true;
                        break;
                    case 1009:
                        maximumDamageToMonster = (monster.getStats().isBoss() ? monster.getMobMaxHp() / 30 * 100 : monster.getMobMaxHp());
                        defined = true;
                        break;
                }
            }
            switch (theSkill.getId()) {
                case 1311005:
                    PDRate = (monster.getStats().isBoss() ? PDRate : 0);
                    break;
                case 3221001:
                case 33101001:
                    maximumDamageToMonster *= attackEffect.getMobCount();
                    defined = true;
                    break;
                case 3101005:
                    defined = true; //can go past 500000
                    break;
                case 32001000:
                case 32101000:
                case 32111002:
                    maximumDamageToMonster *= 1.5;
                    break;
                case 3221007: //snipe
                case 23121003:
                case 1221009: //BLAST FK
                case 4331003: //Owl Spirit
                    if (!monster.getStats().isBoss()) {
                        maximumDamageToMonster = (monster.getMobMaxHp());
                        defined = true;
                    }
                    break;
                //case 1221011://Heavens Hammer
                case 21120006: //Combo Tempest
                    maximumDamageToMonster = (monster.getStats().isBoss() ? 500000 : (monster.getHp() - 1));
                    defined = true;
                    break;
                case 3211006: //Sniper Strafe
                    if (monster.getStatusSourceID(MonsterStatus.FREEZE) == 3211003) { //blizzard in effect
                        defined = true;
                        maximumDamageToMonster = 999999; // 스나이핑 실제 데미지1
                    }
                    break;
            }
        }
        if (attack.skill == 1211002) {
            maximumDamageToMonster *= 1.25; //test
        }
        double elementalMaxDamagePerMonster = maximumDamageToMonster;
        if (player.getJob() == 311 || player.getJob() == 312 || player.getJob() == 321 || player.getJob() == 322) {
            //FK mortal blow
            Skill mortal = SkillFactory.getSkill(player.getJob() == 311 || player.getJob() == 312 ? 3110001 : 3210001);
            if (player.getTotalSkillLevel(mortal) > 0) {
                final MapleStatEffect mort = mortal.getEffect(player.getTotalSkillLevel(mortal));
                if (mort != null && monster.getHPPercent() < mort.getX()) {
                    elementalMaxDamagePerMonster = 999999;//스나이핑 실제 데미지2
                    defined = true;
                    if (mort.getZ() > 0) {
                        player.addHP((player.getStat().getMaxHp() * mort.getZ()) / 100);
                    }
                }
            }
        } else if (player.getJob() == 221 || player.getJob() == 222) {
            //FK storm magic
            Skill mortal = SkillFactory.getSkill(2210000);
            if (player.getTotalSkillLevel(mortal) > 0) {
                final MapleStatEffect mort = mortal.getEffect(player.getTotalSkillLevel(mortal));
                if (mort != null && monster.getHPPercent() < mort.getX()) {
                    elementalMaxDamagePerMonster = 999999;//스나이핑 실제 데미지3
                    defined = true;
                }
            }
        }
        if (!defined || (theSkill != null && (theSkill.getId() == 33101001 || theSkill.getId() == 3221001))) {
            if (player.getBuffedValue(MapleBuffStat.WK_CHARGE) != null) {
                int chargeSkillId = player.getBuffSource(MapleBuffStat.WK_CHARGE);

                switch (chargeSkillId) {
                    case 1211003:
                    case 1211004:
                        elements.add(Element.FIRE);
                        break;
                    case 1211005:
                    case 1211006:
                    case 21111005:
                        elements.add(Element.ICE);
                        break;
                    case 1211007:
                    case 1211008:
                    case 15101006:
                        elements.add(Element.LIGHTING);
                        break;
                    case 1221003:
                    case 1221004:
                    case 11111007:
                        elements.add(Element.HOLY);
                        break;
                    case 12101005:
                        //elements.clear(); //neutral
                        break;
                }
            }
            if (player.getBuffedValue(MapleBuffStat.LIGHTNING_CHARGE) != null) {
                elements.add(Element.LIGHTING);
            }
            if (player.getBuffedValue(MapleBuffStat.ELEMENT_RESET) != null) {
                elements.clear();
            }
            if (elements.size() > 0) {
                double elementalEffect;

                switch (attack.skill) {
                    case 3211003:
                    case 3111003: // inferno and blizzard
                        elementalEffect = attackEffect.getX() / 100.0;
                        break;
                    default:
                        elementalEffect = (0.5 / elements.size());
                        break;
                }
                for (Element element : elements) {
                    switch (monster.getEffectiveness(element)) {
                        case IMMUNE:
                            elementalMaxDamagePerMonster = 1;
                            break;
                        case WEAK:
                            elementalMaxDamagePerMonster *= (1.0 + elementalEffect + player.getStat().getElementBoost(element));
                            break;
                        case STRONG:
                            elementalMaxDamagePerMonster *= (1.0 - elementalEffect - player.getStat().getElementBoost(element));
                            break;
                    }
                }
            }
            // Calculate mob def
            elementalMaxDamagePerMonster -= elementalMaxDamagePerMonster * (Math.max(PDRate - Math.max(player.getStat().ignoreTargetDEF, 0) - Math.max(attackEffect == null ? 0 : attackEffect.getIgnoreMob(), 0), 0) / 100.0);

            // Calculate passive bonuses + Sharp Eye
            elementalMaxDamagePerMonster += ((double) elementalMaxDamagePerMonster / 100.0) * CritPercent;

//	    if (theSkill.isChargeSkill()) {
//	        elementalMaxDamagePerMonster = (double) (90 * (System.currentTimeMillis() - player.getKeyDownSkill_Time()) / 2000 + 10) * elementalMaxDamagePerMonster * 0.01;
//	    }
//          if (theSkill != null && theSkill.isChargeSkill() && player.getKeyDownSkill_Time() == 0) {
//              return 0;
//          }
            final MonsterStatusEffect imprint = monster.getBuff(MonsterStatus.IMPRINT);
            if (imprint != null) {
                elementalMaxDamagePerMonster += (elementalMaxDamagePerMonster * imprint.getX() / 100.0);
            }

            elementalMaxDamagePerMonster += (elementalMaxDamagePerMonster * player.getDamageIncrease(monster.getObjectId()) / 100.0);
            elementalMaxDamagePerMonster *= (monster.getStats().isBoss() && attackEffect != null ? (player.getStat().bossdam_r + attackEffect.getBossDamage()) : player.getStat().dam_r) / 100.0;
        }
        if (elementalMaxDamagePerMonster > 9100000000L) {//맥뎀
            if (!defined) {
                elementalMaxDamagePerMonster = 9100000000L;//맥뎀
            }
        } else if (elementalMaxDamagePerMonster <= 0) {
            elementalMaxDamagePerMonster = 1;
        }
        return elementalMaxDamagePerMonster;
    }

    public static final AttackInfo DivideAttack(final AttackInfo attack, final int rate) {
        attack.real = false;
        if (rate <= 1) {
            return attack; //lol
        }
        for (AttackPair p : attack.allDamage) {
            if (p.attack != null) {
                for (Pair<Integer, Boolean> eachd : p.attack) {
                    eachd.left /= rate; //too ex.
                }
            }
        }
        return attack;
    }

    public static final AttackInfo Modify_AttackCrit(final AttackInfo attack, final MapleCharacter chr, final int type, final MapleStatEffect effect) {
        if (attack.skill != 4211006 && attack.skill != 3211003 && attack.skill != 4111004) { //blizz + shadow meso + m.e no crits
            final int CriticalRate = chr.getStat().passive_sharpeye_rate() + (effect == null ? 0 : effect.getCr());
            final boolean shadow = chr.getBuffedValue(MapleBuffStat.SHADOWPARTNER) != null && (type == 1 || type == 2);
            final List<Integer> damages = new ArrayList<Integer>(), damage = new ArrayList<Integer>();
            int hit, toCrit, mid_att;
            for (AttackPair p : attack.allDamage) {
                if (p.attack != null) {
                    hit = 0;
                    mid_att = shadow ? (p.attack.size() / 2) : p.attack.size();
                    //grab the highest hits
                    toCrit = attack.skill == 4221001 || attack.skill == 3221007 || attack.skill == 23121003 || attack.skill == 4341005 || attack.skill == 4331006 || attack.skill == 21121005 ? mid_att : 0;
                    if (toCrit == 0) {
                        for (Pair<Integer, Boolean> eachd : p.attack) {
                            if (!eachd.right && hit < mid_att) {
                                if (eachd.left > 2100000000 || Randomizer.nextInt(100) < CriticalRate) {
                                    toCrit++;
                                }
                                damage.add(eachd.left);
                            }
                            hit++;
                        }
                        if (toCrit == 0) {
                            damage.clear();
                            continue; //no crits here
                        }
                        Collections.sort(damage); //least to greatest
                        for (int i = damage.size(); i > damage.size() - toCrit; i--) {
                            damages.add(damage.get(i - 1));
                        }
                        damage.clear();
                    }
                    hit = 0;
                    for (Pair<Integer, Boolean> eachd : p.attack) {
                        if (!eachd.right) {
                            if (attack.skill == 4221001) { //assassinate never crit first 3, always crit last
                                eachd.right = hit == 3;
                            } else if (attack.skill == 3221007 || attack.skill == 23121003 || attack.skill == 21121005 || attack.skill == 4341005 || attack.skill == 4331006 || eachd.left > 2100000000) { //snipe always crit
                                eachd.right = true;
                            } else if (hit >= mid_att) { //shadowpartner copies second half to first half
                                eachd.right = p.attack.get(hit - mid_att).right;
                            } else {
                                //rough calculation
                                eachd.right = damages.contains(eachd.left);
                            }
                        }
                        hit++;
                    }
                    damages.clear();
                }
            }
        }
        return attack;
    }

    public static final AttackInfo parseDmgMa(final LittleEndianAccessor lea, final MapleCharacter chr) {
        final AttackInfo ret = new AttackInfo();
        lea.skip(1);
        ret.tbyte = lea.readByte();
        ret.targets = (byte) ((ret.tbyte >>> 4) & 0xF);
        ret.hits = (byte) (ret.tbyte & 0xF);
        ret.skill = lea.readInt();
        lea.skip(5);
        if (GameConstants.isMagicChargeSkill(ret.skill)) {
            ret.charge = lea.readInt();
        } else {
            ret.charge = -1;
        }
        ret.unk = lea.readByte();
        ret.display = lea.readUShort();
        lea.skip(4); //big bang
        lea.skip(1); // Weapon class
        ret.speed = lea.readByte(); // Confirmed
        ret.lastAttackTickCount = lea.readInt(); // Ticks
        lea.skip(4); //0

        int damage, oid;
        List<Pair<Integer, Boolean>> allDamageNumbers;
        ret.allDamage = new ArrayList<AttackPair>();
        /*if ((ret.display < 0 || ret.display > 10) && recv == RecvPacketOpcode.CLOSE_RANGE_ATTACK) {
         AutobanManager.getInstance().autoban(chr.getClient(), "Invalid Display packet sent");
         FileoutputUtil.log(FileoutputUtil.감지로그, "[" + CurrentReadable_Time() + "] " + chr.getName() + "님이 패킷을 변조해서 영구정지 처리가 되었습니다.");
         System.out.println("[Ting Packet Perception] : " + chr.getName());
         ret.display = 1;
         }*/

        for (int i = 0; i < ret.targets; i++) {
            oid = lea.readInt();
            //if (chr.getMap().isTown()) {
            //    final MapleMonster od = chr.getMap().getMonsterByOid(oid);
            //    if (od != null && od.getLinkCID() > 0) {
            //	    return null;
            //    }
            //}
            lea.skip(14); // [1] Always 6?, [3] unk, [4] Pos1, [4] Pos2, [2] seems to change randomly for some attack

            allDamageNumbers = new ArrayList<Pair<Integer, Boolean>>();

            for (int j = 0; j < ret.hits; j++) {
                damage = lea.readInt();
                allDamageNumbers.add(new Pair<Integer, Boolean>(Integer.valueOf(damage), false));
            }
            lea.skip(4); // CRC of monster [Wz Editing]
            ret.allDamage.add(new AttackPair(Integer.valueOf(oid), allDamageNumbers));
        }
        ret.position = lea.readPos();

        return ret;
    }

    public static final AttackInfo parseDmgM(final LittleEndianAccessor lea, final MapleCharacter chr) {
        final AttackInfo ret = new AttackInfo();
        lea.skip(1);
        ret.tbyte = lea.readByte();
        ret.targets = (byte) ((ret.tbyte >>> 4) & 0xF);
        ret.hits = (byte) (ret.tbyte & 0xF);
        ret.skill = lea.readInt();
        lea.skip(5);
        switch (ret.skill) {
            case 5101004: // Corkscrew
            case 15101003: // Cygnus corkscrew
            case 5201002: // Gernard
            case 14111006: // Poison bomb
            case 4341002:
            case 4341003:
            case 5301001:
            case 5300007:
                ret.charge = lea.readInt();
                break;
            default:
                ret.charge = 0;
                break;
        }
        ret.unk = lea.readByte();
        ret.display = lea.readUShort();
        lea.skip(4); //big bang
        lea.skip(1); // Weapon class
        ret.speed = lea.readByte(); // Confirmed
        ret.lastAttackTickCount = lea.readInt(); // Ticks
        lea.skip(4); //0

        ret.allDamage = new ArrayList<AttackPair>();
        /*if ((ret.speed < 0 || ret.speed > 10) && recv == RecvPacketOpcode.CLOSE_RANGE_ATTACK) {
         AutobanManager.getInstance().autoban(chr.getClient(), "팅패킷 사용");
         System.out.println("팅패킷 사용 : " + chr.getName());
         return null;
         }*/
        if (ret.skill == 4211006) { // Meso Explosion
            return parseMesoExplosion(lea, ret, chr);
        }
        int damage, oid;
        List<Pair<Integer, Boolean>> allDamageNumbers;

        for (int i = 0; i < ret.targets; i++) {
            oid = lea.readInt();
            lea.skip(14); // [1] Always 6?, [3] unk, [4] Pos1, [4] Pos2, [2] seems to change randomly for some attack

            allDamageNumbers = new ArrayList<Pair<Integer, Boolean>>();

            for (int j = 0; j < ret.hits; j++) {
                damage = lea.readInt();
                allDamageNumbers.add(new Pair<Integer, Boolean>(Integer.valueOf(damage), false));
            }
            lea.skip(4); // CRC of monster [Wz Editing]
            ret.allDamage.add(new AttackPair(Integer.valueOf(oid), allDamageNumbers));
        }
        ret.position = lea.readPos();
        if (ret.skill == 14111006) {
            final Skill poisonBomb = SkillFactory.getSkill(14111006);
            final MapleStatEffect poison = poisonBomb.getEffect(chr.getTotalSkillLevel(poisonBomb));
            final Rectangle bounds = poison.calculateBoundingBox(ret.position = lea.readPos(), chr.isFacingLeft());
            final MapleMist mist = new MapleMist(bounds, chr, poison);
            chr.getMap().spawnMist(mist, poison.getDuration(), true, false);
        }
        return ret;
    }

    public static boolean isShootSkillNotUsingShootingWeapon(int skillID) {
        if (skillID > 15111007) {
            switch (skillID) {
                case 21101004:
                case 21111004:
                case 21120006:
                case 33101007:
                    return true;
            }
        } else {
            if (skillID >= 15111006) {
                return true;
            }
            switch (skillID) {
                case 1078:
                case 1079:
                case 4121003:
                case 4221003:
                case 5121002:
                case 11101004:
                    return true;
            }
        }
        return false;
    }

    public static boolean isShootSkillNotUsingBullet(int skillID) {
        if (isShootSkillNotUsingShootingWeapon(skillID)) {
            return true;
        }

        if (skillID <= 35001001) {
            switch (skillID) {
                case 35001001:
                case 3101003:
                case 3201003:
                case 4111004:
                case 13101005:
                case 14101006:
                case 33101002:
                    return true;
            }
        } else if (skillID > 35111015) {
            if (skillID != 35121005 && (skillID <= 35121011 || skillID > 35121013)) {
                return false;
            }
            return true;
        } else if (skillID != 35111015 && skillID != 35001004) {
            if (skillID <= 35101008) {
                return false;
            }
            if (skillID > 35101010) {
                if (skillID == 35111004) {
                    return true;
                }
            }
        }
        return false;
    }

    public static final AttackInfo parseDmgR(final LittleEndianAccessor lea, final MapleCharacter chr) {
        final AttackInfo ret = new AttackInfo();
        lea.skip(1); //fieldKey
        ret.tbyte = lea.readByte(); //attackInfo
        ret.targets = (byte) ((ret.tbyte >>> 4) & 0xF);
        ret.hits = (byte) (ret.tbyte & 0xF);
        ret.skill = lea.readInt(); //skillID
        if (ret.skill >= 91000000) { //guild/recipe? no
            return null;
        }
        byte idk = lea.readByte(); //bAddAttackProc
        lea.skip(4); //skillCrc
        switch (ret.skill) {
            case 3121004: // Hurricane
            case 3221001: // Pierce
            case 5221004: // Rapidfire
            case 13111002: // Cygnus Hurricane
            case 33121009:
            case 35001001:
            case 35101009:
            case 23121000:
            case 5311002:
                lea.skip(4); //nKeyDown
                break;
        }
        ret.charge = -1;
        ret.unk = lea.readByte(); //skillOption
        lea.readByte(); //bNextShootExJablin
        ret.display = lea.readUShort();
        lea.skip(4); //뭐야 이건, 커스텀으로 추가한건가
        lea.skip(1); // Weapon class
        ret.speed = lea.readByte(); // Confirmed
        ret.lastAttackTickCount = lea.readInt(); // Ticks
        lea.skip(4); //0
        ret.slot = (byte) lea.readShort();
        ret.csstar = (byte) lea.readShort();
        ret.AOE = lea.readByte(); // is AOE or not, TT/ Avenger = 41, Showdown = 0

        byte option = ret.unk;
        byte o1 = (byte) (option >>> 1); //bSoulArrow
        byte o2 = (byte) (o1 >>> 1); //bAcitonRollback
        byte o3 = (byte) (o2 >>> 1); //bShadowPartner
        byte o4 = (byte) (o3 >>> 3); //bSpiritJavelin
        byte o5 = (byte) (o4 >>> 1); //bSpark

        if ((o4 & 1) != 0 && !isShootSkillNotUsingBullet(ret.skill)) {
            int javelinItemID = lea.readInt();
        }

        int damage, oid;
        List<Pair<Integer, Boolean>> allDamageNumbers;
        ret.allDamage = new ArrayList<AttackPair>();

        for (int i = 0; i < ret.targets; i++) {
            oid = lea.readInt();
            lea.skip(14); // [1] Always 6?, [3] unk, [4] Pos1, [4] Pos2, [2] seems to change randomly for some attack
            allDamageNumbers = new ArrayList<Pair<Integer, Boolean>>();
            for (int j = 0; j < ret.hits; j++) {
                damage = lea.readInt();
                allDamageNumbers.add(new Pair<Integer, Boolean>(Integer.valueOf(damage), false));
            }
            lea.skip(4);
            ret.allDamage.add(new AttackPair(Integer.valueOf(oid), allDamageNumbers));
        }
        lea.skip(4);
        ret.position = lea.readPos();
        return ret;
    }

    public static final AttackInfo parseMesoExplosion(final LittleEndianAccessor lea, final AttackInfo ret, final MapleCharacter chr) {
        //System.out.println(lea.toString(true));
        byte bullets;
        if (ret.hits == 0) {
            lea.skip(4);
            bullets = lea.readByte();
            for (int j = 0; j < bullets; j++) {
                ret.allDamage.add(new AttackPair(Integer.valueOf(lea.readInt()), null));
                lea.skip(1);
            }
            lea.skip(2); // 8F 02
            return ret;
        }
        int oid;
        List<Pair<Integer, Boolean>> allDamageNumbers;

        for (int i = 0; i < ret.targets; i++) {
            oid = lea.readInt();
            //if (chr.getMap().isTown()) {
            //    final MapleMonster od = chr.getMap().getMonsterByOid(oid);
            //    if (od != null && od.getLinkCID() > 0) {
            //	    return null;
            //    }
            //}
            lea.skip(12);
            bullets = lea.readByte();
            allDamageNumbers = new ArrayList<Pair<Integer, Boolean>>();
            for (int j = 0; j < bullets; j++) {
                allDamageNumbers.add(new Pair<Integer, Boolean>(Integer.valueOf(lea.readInt()), false)); //m.e. never crits
            }
            ret.allDamage.add(new AttackPair(Integer.valueOf(oid), allDamageNumbers));
            lea.skip(4); // C3 8F 41 94, 51 04 5B 01
        }
        lea.skip(4);
        bullets = lea.readByte();

        for (int j = 0; j < bullets; j++) {
            ret.allDamage.add(new AttackPair(Integer.valueOf(lea.readInt()), null));
            lea.skip(1);
        }
        // 8F 02/ 63 02

        return ret;
    }
}
