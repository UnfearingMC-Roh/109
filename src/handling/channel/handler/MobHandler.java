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
import java.util.List;

import client.MapleClient;
import client.MapleCharacter;
import client.Skill;
import client.SkillFactory;
import client.inventory.MapleInventoryType;
import client.status.MonsterStatus;
import static java.lang.ProcessBuilder.Redirect.to;
import java.util.Arrays;
import server.MapleInventoryManipulator;
import server.MapleStatEffect;
import server.Randomizer;
import server.Timer;
import server.maps.MapleMap;
import server.life.MapleMonster;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleNodes.MapleNodeInfo;
import server.movement.AbsoluteLifeMovement;
import server.movement.LifeMovement;
import server.movement.LifeMovementFragment;

import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.packet.MobPacket;
import tools.data.LittleEndianAccessor;

public class MobHandler {

    public static final void MoveMonster(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return; //?
        }
        final int oid = slea.readInt();
        final MapleMonster monster = chr.getMap().getMonsterByOid(oid);

        if (monster == null) { // movin something which is not a monster
            return;
        }
        if (monster.getLinkCID() > 0) {
            return;
        }
        final short moveid = slea.readShort();
        byte v9 = slea.readByte();
        final boolean bCheatResult = (v9 & 0xF) != 0;
        final boolean v56 = (v9 & 0xF0) != 0;

        int pCenterSplit = slea.readByte();
        int nAction = pCenterSplit;
        int skill1 = slea.readByte() & 0xFF; // unsigned?
        int skill2 = slea.readByte() & 0xFF;
        short option = slea.readShort();//skill_3,skill_4 
        if (skill1 > 0) {
            // chr.dropMessage(6, "nAction: " + nAction + " || 스킬 아이디: " + skill1 + " || 스킬 레벨: " + skill2 + " || 옵션(딜레이): " + skill4 + " || skill3(딜레이??): " + skill3);
        }
//        if (skill1 > 0) {
//            System.out.println(skill1 + " / " + skill2 + " / " + skill3 + " / " + skill4);
//        }
        if (nAction < 0) {
            nAction = -1;
        } else {
            nAction = nAction >> 1;
        }

        int realskill = 0;
        int level = 0;

        monster.setNextAttackPossible(bCheatResult);

        if ((nAction >= 21 && nAction <= 25) || bCheatResult) { // Monster Skill
            boolean madeSkill = !(nAction >= 21 && nAction <= 25);
            int skillid = skill1;
            int skilllevel = skill2;
            final int skillDelay = option;
            final byte size = monster.getNoSkills();

            if (size > 0) {
                if (madeSkill) {
                    for (final Pair<Integer, Integer> skillToUse : monster.getSkills()) {
                        final Pair<Integer, Integer> skillToUse2 = monster.getSkills().get((byte) Randomizer.nextInt(size));
                        //chr.dropMessage(6, "skillToUse2: " + skillToUse2);
                        skillid = skillToUse.getLeft();
                        skilllevel = skillToUse2.getRight();
                        if (monster.hasSkill(skillid, skilllevel)) {

                            final MobSkill mobSkill = MobSkillFactory.getMobSkill(skillid, skilllevel);

                            if (mobSkill != null && !mobSkill.checkCurrentBuff(chr, monster)) {
                                final long now = System.currentTimeMillis();
                                final long ls = monster.getLastSkillUsed(skillid);

                                if (ls == 0 || (((now - ls) > mobSkill.getCoolTime()) && !mobSkill.onlyOnce())) {

                                    final int reqHp = (int) (((float) monster.getHp() / monster.getMobMaxHp()) * 100); // In case this monster have 2.1b and above HP
                                    if (reqHp <= mobSkill.getHP()) {
                                        //chr.dropMessage(6, "mobSkill.getHP(): " + mobSkill.getHP());
                                     //  chr.dropMessage(6, "조건 등록 : " + skillid);
                                        
                                        //메시지를 지웠는디 ㅡ
                                        monster.setLastSkillUsed(skillid, now, mobSkill.getCoolTime());
                                        realskill = skillid;
                                        level = skilllevel;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } else if (monster.hasSkill(skillid, skilllevel)) {
                    final MobSkill mobSkill = MobSkillFactory.getMobSkill(skillid, skilllevel);
                    if (monster.isAlive()) {
                        c.getSession().write(MobPacket.MobSkillDelay(oid, skillid, skilllevel, 0, (short) option));
                        if (skillid == 200) {
                            //chr.dropMessage(6, "skillid: " + skillid + "skilllevel: " + skilllevel + "skill2: " + skill2);
                        }
                        //공반..? 아예 몹스킬을 안쓰는데?
                        //mobSkill.applyEffect(chr, monster, true, (short) skillDelay);
                    }
                }
            }
        }
        int gg = slea.readInt();
        int gg2 = slea.readInt();
        boolean skipped = gg2 != 0 && gg > 0;
        //chr.dropMessage(6, "스킵이니?" + skipped + "gg" + gg);
        slea.skip(skipped ? 33 : 25); //extra 8 bytes or what

        if (monster.getController() != null && monster.getController().getId() != c.getPlayer().getId()) {
            if (!v56/* || monster.getNextAttackPossible()*/) { // 동시에 컨트롤 방지.. 안그럼 문워크함 ㅠㅠ
//                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.serverNotice(5, c.getPlayer().getName() + "- !v56 : " + !v56/* + " / mb : " + monster.getNextAttackPossible()*/));
                c.sendPacket(MobPacket.stopControllingMonster(oid));
                //c.getSession().write(MobPacket.stopControllingMonster(oid));
                return;
            } else {
                monster.switchController(chr, true);
            }
        }
        final Point startPos = monster.getPosition();
        List<LifeMovementFragment> res = null;
        try {
            res = MovementParse.parseMovement(slea, 2);
        } catch (ArrayIndexOutOfBoundsException e) {
            return;
        }

        if (res != null && chr != null && res.size() > 0) {
            final MapleMap map = chr.getMap();
            for (final LifeMovementFragment move : res) {
                if (move instanceof AbsoluteLifeMovement) {
                    final Point endPos = ((LifeMovement) move).getPosition();
                    if (endPos.x < (map.getLeft() - 250) || endPos.y < (map.getTop() - 250) || endPos.x > (map.getRight() + 250) || endPos.y > (map.getBottom() + 250)) { //experimental
                        chr.getCheatTracker().checkMoveMonster(endPos);
                        return;
                    }
                }
            }
            monster.receiveMovePacket();
            //c.getPlayer().dropMessage(5, "bCheatResult : " + bCheatResult + " nAction : " + nAction + " / s1 : " + skill1 + " / s2 : " + skill2 + " / s3 : " + skill3 + " / s4 : " + skill4 + " / realskill : " + realskill + " / reallevel : " + level);
            c.getSession().write(MobPacket.moveMonsterResponse(monster.getObjectId(), moveid, Math.max(monster.getMp(), Math.min(monster.getStats().getMp(), 500)), bCheatResult, realskill, level));

            MovementParse.updatePosition(res, monster, -1);
            final Point endPos = monster.getTruePosition();
            map.moveMonster(monster, endPos);
            map.broadcastMessage(chr, MobPacket.moveMonster(bCheatResult, pCenterSplit, skill1, skill2, option, monster.getObjectId(), startPos, res), endPos);
            chr.getCheatTracker().checkMoveMonster(endPos);
          //  chr.dropMessage(6, "모션발생");
        }
    }

    public static final void FriendlyDamage(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final MapleMap map = chr.getMap();
        if (map == null) {
            return;
        }
        final MapleMonster mobfrom = map.getMonsterByOid(slea.readInt());
        slea.skip(4); // Player ID
        final MapleMonster mobto = map.getMonsterByOid(slea.readInt());

        if (mobfrom != null && mobto != null && mobto.getStats().isFriendly()) {
            final int damage = (mobto.getStats().getLevel() * Randomizer.nextInt(mobto.getStats().getLevel())) / 2; // Temp for now until I figure out something more effective
            mobto.damage(chr, damage, true);
            checkShammos(chr, mobto, map);
        }
    }

    public static final void MobSkillDelayEnd(LittleEndianAccessor slea, MapleCharacter chr) {
        MapleMonster monster = chr.getMap().getMonsterByOid(slea.readInt());
        if (monster != null) {
            int skillID = slea.readInt();
            int skillLv = slea.readInt();
            int option = slea.readInt();
            if (monster.hasSkill(skillID, skillLv)) {
                final MobSkill mobSkill = MobSkillFactory.getMobSkill(skillID, skillLv);
                if (mobSkill != null) {
                    final long now = System.currentTimeMillis();
                    final long ls = monster.getLastSkillUsed(skillID);
                    if (ls > 0) {
                        mobSkill.applyEffect(chr, monster, true, (short) option);
               //         chr.dropMessage(6, "조건을 모든 충족하였음.");
                    }
                }    
            }
        }
    }

    public static final void MobBomb(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final MapleMap map = chr.getMap();
        int randdmg = Randomizer.nextInt(5);
        final MapleMonster mobfrom = map.getMonsterByOid(slea.readInt());
        slea.readInt();//x
        slea.readInt();//y     
        final Skill skill = SkillFactory.getSkill(4341003);
        final MapleStatEffect eff = skill.getEffect(chr.getSkillLevel(skill));
        double damage = eff.getDamage() * chr.getStat().getCurrentMaxBaseDamage() / (95.0 + randdmg);
        map.broadcastMessage(MaplePacketCreator.showMonsterBomb(mobfrom.getObjectId(), chr.getId()));
        MapleMonster mob;
        if (mobfrom.getBuff(MonsterStatus.MONSTER_BOMB) != null) {
            for (MapleMapObject mo : map.getMapObjectsInRange(mobfrom.getPosition(), 28000, Arrays.asList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) mo;
                map.broadcastMessage(MobPacket.damageMonster(mob.getObjectId(), (int) damage));
                mob.damage(chr, (int) damage, false);
                chr.checkMonsterAggro(mob);
                if (!mob.isAlive()) {
                    map.broadcastMessage(MobPacket.killMonster(mob.getObjectId(), 1));
                }
            }
            if (!map.getMapObjectsInRange(mobfrom.getPosition(), 28000, Arrays.asList(MapleMapObjectType.PLAYER)).isEmpty()) {//플레이어가있따면  
                map.broadcastMessage(MaplePacketCreator.showrealbomb(mobfrom.getId()));
                map.broadcastMessage(chr, MaplePacketCreator.damagePlayer(0, mobfrom.getId(), chr.getId(), ((int) damage / 5), 0, (byte) 0, 0, 0, false, 0, 0, 0, 0), false);
                chr.addHP((int) -(damage / 5));
                chr.dropMessage(6, "몬스터봄의 폭발로 " + ((int) damage / 5) + "의 데미지를 입었습니다");
            }
        }
    }

    public static final void checkShammos(final MapleCharacter chr, final MapleMonster mobto, final MapleMap map) {
        if (!mobto.isAlive() && mobto.getStats().isEscort()) { //shammos
            for (MapleCharacter chrz : map.getCharactersThreadsafe()) { //check for 2022698
                if (chrz.getParty() != null && chrz.getParty().getLeader().getId() == chrz.getId()) {
                    //leader
                    if (chrz.haveItem(2022698)) {
                        MapleInventoryManipulator.removeById(chrz.getClient(), MapleInventoryType.USE, 2022698, 1, false, true);
                        mobto.heal((int) mobto.getMobMaxHp(), mobto.getMobMaxMp(), true);
                        return;
                    }
                    break;
                }
            }
            map.broadcastMessage(MaplePacketCreator.serverNotice(6, "Your party has failed to protect the monster."));
            final MapleMap mapp = chr.getMap().getForcedReturnMap();
            for (MapleCharacter chrz : map.getCharactersThreadsafe()) {
                chrz.changeMap(mapp, mapp.getPortal(0));
            }
        } else if (mobto.getStats().isEscort() && mobto.getEventInstance() != null) {
            mobto.getEventInstance().setProperty("HP", String.valueOf(mobto.getHp()));
        }
    }

    public static final void MonsterBomb(final int oid, final MapleCharacter chr) {
        final MapleMonster monster = chr.getMap().getMonsterByOid(oid);

        if (monster == null || !chr.isAlive() || chr.isHidden() || monster.getLinkCID() > 0) {
            return;
        }
        final byte selfd = monster.getStats().getSelfD();
        if (selfd != -1) {
            chr.getMap().killMonster(monster, chr, false, false, selfd);
            //monster.getMap().broadcastMessage(MobPacket.killMonster(monster.getObjectId(), 4));
            //chr.getMap().removeMapObject(monster);
        }
    }

    public static final void AutoAggro(final int monsteroid, final MapleCharacter chr) {
        if (chr == null || chr.getMap() == null || chr.isHidden()) { //no evidence :)
            return;
        }
        final MapleMonster monster = chr.getMap().getMonsterByOid(monsteroid);

        if (monster != null && chr.getTruePosition().distanceSq(monster.getTruePosition()) < 200000 && monster.getLinkCID() <= 0) {
            if (monster.getController() != null) {
                if (chr.getMap().getCharacterById(monster.getController().getId()) == null) {
                    monster.switchController(chr, true);
                } else {
                    monster.switchController(monster.getController(), true);
                }
            } else {
                monster.switchController(chr, true);
            }
        }
    }

    public static final void HypnotizeDmg(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final MapleMonster mob_from = chr.getMap().getMonsterByOid(slea.readInt()); // From
        slea.skip(4); // Player ID
        final int to = slea.readInt(); // mobto
        slea.skip(1); // Same as player damage, -1 = bump, integer = skill ID
        final int damage = slea.readInt();
	slea.skip(1); // Facing direction
	slea.skip(4); // Some type of pos, damage display, I think

        final MapleMonster mob_to = chr.getMap().getMonsterByOid(to);

        if (mob_from != null && mob_to != null) { //temp for now
            mob_to.damage(chr, damage, true);
	    chr.getMap().broadcastMessage(chr, MobPacket.damageMonster(to, damage), false);//이거안쏴주면 데미지안보임         
        }
    }
    
    public static final void DisplayNode(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final MapleMonster mob_from = chr.getMap().getMonsterByOid(slea.readInt()); // From
        if (mob_from != null) {
            chr.getClient().getSession().write(MaplePacketCreator.getNodeProperties(mob_from, chr.getMap()));
        }
    }

    public static final void MobNode(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final MapleMonster mob_from = chr.getMap().getMonsterByOid(slea.readInt()); // From
        final int newNode = slea.readInt();
        final int nodeSize = chr.getMap().getNodes().size();
        if (mob_from != null && nodeSize > 0) {
            final MapleNodeInfo mni = chr.getMap().getNode(newNode);
            if (mni == null) {
                return;
            }
            if (mni.attr == 2) { //talk
                switch (chr.getMapId() / 100) {
                    case 9211200:
                    case 9211201:
                    case 9211202:
                    case 9211203:
                    case 9211204:
                        chr.getMap().talkMonster("이런! 렉스가 봉인된 동굴로 가는 길에 몬스터가 너무 많아. 이것들로부터 모두 처리해줘.", 5120035, mob_from.getObjectId()); //temporary for now. itemID is located in WZ file
                        break;
                    case 9320001:
                    case 9320002:
                    case 9320003:
                        chr.getMap().talkMonster("이런! 렉스가 봉인된 동굴로 가는 길에 몬스터가 너무 많아. 이것들로부터 모두 처리해줘.", 5120051, mob_from.getObjectId()); //temporary for now. itemID is located in WZ file
                        break;
                }
            }
            mob_from.setLastNode(newNode);
            if (chr.getMap().isLastNode(newNode)) { //the last node on the map.
                switch (chr.getMapId() / 100) {
                    case 9211200:
                    case 9211201:
                    case 9211202:
                    case 9211203:
                    case 9211204:
                    case 9320001:
                    case 9320002:
                    case 9320003:
                        chr.getMap().broadcastMessage(MaplePacketCreator.serverNotice(5, "샤모스가 도착했습니다. 다음 스테이지로 이동하시기 바랍니다."));
                        chr.getMap().removeMonster(mob_from);
                        break;

                }
            }
        }
    }
}
