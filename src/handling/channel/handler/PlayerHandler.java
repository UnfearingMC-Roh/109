package handling.channel.handler;

import java.awt.Point;
import java.util.List;
import client.inventory.Item;
import client.Skill;
import client.SkillFactory;
import client.SkillMacro;
import constants.GameConstants;
import client.inventory.MapleInventoryType;
import client.MapleBuffStat;
import client.MapleClient;
import client.MapleCharacter;
import client.MapleQuestStatus;
import client.MapleStat;
import client.PlayerStats;
import client.anticheat.CheatingOffense;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.ServerConstants;
import handling.SendPacketOpcode;
import handling.channel.ChannelServer;
import handling.world.MaplePartyCharacter;
import handling.world.World;
import java.awt.Rectangle;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import server.events.MapleEvent;
import server.events.MapleEventType;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.MaplePortal;
import server.Randomizer;
import server.events.MapleSnowball.MapleSnowballs;
import server.life.MapleMonster;
import server.life.MobAttackInfo;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import server.life.MapleLifeFactory;
import server.maps.MapleMap;
import server.maps.FieldLimitType;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleSummon;
import server.maps.SavedLocationType;
import server.movement.LifeMovementFragment;
import server.quest.MapleQuest;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.packet.MobPacket;
import tools.packet.MTSCSPacket;
import tools.data.LittleEndianAccessor;
import tools.data.MaplePacketLittleEndianWriter;
import tools.packet.CSPacket;
import tools.packet.TemporaryStatsPacket;
import tools.packet.UIPacket;

public class PlayerHandler {

    public static long acCheckLong;

    public static long getAcCheckLong() {
        acCheckLong++;
        return acCheckLong;
    }

    public static long resetAcCheckLong() {
        acCheckLong = 0;
        return acCheckLong;
    }

    public static int isFinisher(final int skillid) {
        switch (skillid) {
            case 1111003:
                return GameConstants.GMS ? 1 : 10;
            case 1111005:
                return GameConstants.GMS ? 2 : 10;
            case 11111002:
                return GameConstants.GMS ? 1 : 10;
            case 11111003:
                return GameConstants.GMS ? 2 : 10;
        }
        return 0;
    }

    public static void WheelOfFortuneEffect(LittleEndianAccessor lea, MapleCharacter chr) {
        byte[] proxy = lea.read(0xC);
        chr.getMap().broadcastGMMessage(chr, MaplePacketCreator.showWheelEffect(chr.getId(), proxy), false);
    }

    public static void ChangeSkillMacro(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final int num = slea.readByte();
        String name;
        int shout, skill1, skill2, skill3;
        SkillMacro macro;

        for (int i = 0; i < num; i++) {
            name = slea.readMapleAsciiString();
            shout = slea.readByte();
            skill1 = slea.readInt();
            skill2 = slea.readInt();
            skill3 = slea.readInt();

            macro = new SkillMacro(skill1, skill2, skill3, name, shout, i);
            chr.updateMacros(i, macro);
        }
    }

    public static final void ChangeKeymap(final LittleEndianAccessor slea, final MapleCharacter chr) {
        if (slea.available() > 8 && chr != null) { // else = pet auto pot
            slea.skip(4); //0
            final int numChanges = slea.readInt();

            for (int i = 0; i < numChanges; i++) {
                final int key = slea.readInt();
                final byte type = slea.readByte();
                final int action = slea.readInt();
                if (type == 1 && action >= 1000) { //0 = normal key, 1 = skill, 2 = item
                    final Skill skil = SkillFactory.getSkill(action);
                    if (skil != null) { //not sure about aran tutorial skills..lol
                        if ((!skil.isFourthJob() && !skil.isBeginnerSkill() && skil.isInvisible() && chr.getSkillLevel(skil) <= 0) || GameConstants.isLinkedAranSkill(action) || action % 10000 < 1000 || action >= 91000000) { //cannot put on a key
                            continue;
                        }
                    }
                }
                chr.changeKeybinding(key, type, action);
            }
        } else if (chr != null) {
            final int type = slea.readInt(), data = slea.readInt();
            if (chr.isAlive()) {
                switch (type) {
                    case 1:
                        if (data <= 0) {
                            //    chr.getQuestRemove(MapleQuest.getInstance(GameConstants.HP_ITEM));
                        } else {
                            chr.getQuestNAdd(MapleQuest.getInstance(GameConstants.HP_ITEM)).setCustomData(String.valueOf(data));
                        }
                        break;
                    case 2:
                        if (data <= 0) {
                            //    chr.getQuestRemove(MapleQuest.getInstance(GameConstants.MP_ITEM));
                        } else {
                            chr.getQuestNAdd(MapleQuest.getInstance(GameConstants.MP_ITEM)).setCustomData(String.valueOf(data));
                        }
                        break;
                }
                chr.updatePetAuto();
            }
        }
    }

    public static final void UseChair(final int itemId, final MapleClient c, final MapleCharacter chr) {
        int a = 0;
        if (chr == null || chr.getMap() == null) {
            return;
        }
        for (int i = 0; i < chr.fishingChair.length; i++) {
            if (itemId == chr.fishingChair[i]) {
                a = 1;
                break;
            }
        }
        final Item toUse = chr.getInventory(MapleInventoryType.SETUP).findById(itemId);
        if (toUse == null) {
            chr.getCheatTracker().registerOffense(CheatingOffense.USING_UNAVAILABLE_ITEM, Integer.toString(itemId));
            return;
        }
        if (a == 1 && chr.getMapId() >= 910000000 && chr.getMapId() <= 910000001) { //낚시의자, 낚시맵
            chr.fishingTimer(60000); //60초에 낚시한번
        }
        if (a == 1 && chr.getMapId() == 229000000) { //낚시의자, 낚시맵
            chr.fishingTimer(60000); //60초에 낚시한번
        }
        chr.setChair(itemId);
        chr.getMap().broadcastMessage(chr, MaplePacketCreator.showChair(chr.getId(), itemId), false);
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static final void CancelChair(final short id, final MapleClient c, final MapleCharacter chr) {//낚시취소
        if (chr.getMapId() >= 910000000 && chr.getMapId() <= 910000001) {
            chr.cancelFishing();
        }
        if (id == -1) { // Cancel Chair
            chr.cancelFishingTask();
            chr.setChair(0);
            c.getSession().write(MaplePacketCreator.cancelChair(-1));
            if (chr.getMap() != null) {
                chr.getMap().broadcastMessage(chr, MaplePacketCreator.showChair(chr.getId(), 0), false);
            }
        } else { // Use In-Map Chair
            chr.setChair(id);
            c.getSession().write(MaplePacketCreator.cancelChair(id));
        }
    }

    public static final void PoisonBomb(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final int x = slea.readShort();
        slea.readShort();//00 00 도되었다가 FF FF도 되었다가
        final int y = slea.readShort();
        slea.readShort();//위랑같음
        slea.skip(4);//캐릭터의 Y좌표
        final int charge = slea.readInt(); //게이지를 모은정도
        final int skillId = slea.readInt(); //스킬아이디
        final int skillLevel = slea.readInt(); //스킬레벨
        if (skillId != 14111006 && skillLevel < 1) {
            System.out.println("핵의심유저: " + chr.getName());
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        } else {
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    /*public static final void PoisonBomb(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {    
     final int x = slea.readShort();
     slea.readShort();//00 00 도되었다가 FF FF도 되었다가
     final int y = slea.readShort();
     slea.readShort();//위랑같음
     slea.skip(4);//캐릭터의 Y좌표
     final int charge = slea.readInt(); //게이지를 모은정도
     final int skillId = slea.readInt(); //스킬아이디
     final int skillLevel = slea.readInt(); //스킬레벨
        
     if (skillId != 4321002) {
     chr.getMap().broadcastMessage(MaplePacketCreator.showbomb(chr, x, y, charge, skillId, skillLevel));
     } else {
     chr.getMap().broadcastMessage(MaplePacketCreator.showbomb(chr, x, y, charge, skillId, skillLevel));
     }
        
     if (skillId != 14111006 && skillLevel < 1) {
     System.out.println("핵의심유저: " + chr.getName());
     c.getSession().write(MaplePacketCreator.enableActions());                    
     return;
     } else {
     c.getSession().write(MaplePacketCreator.enableActions());        
     }
     }*/
    public static final void TrockAddMap(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final byte addrem = slea.readByte();
        final byte vip = slea.readByte();

        if (vip == 1) {
            if (addrem == 0) {
                chr.deleteFromRocks(slea.readInt());
            } else if (addrem == 1) {
                if (!FieldLimitType.VipRock.check(chr.getMap().getFieldLimit())) {
                    chr.addRockMap();
                } else {
                    chr.dropMessage(1, "순간이동이 불가능한 지역입니다.");
                }
            }
        } else if (vip >= 2) {
            if (addrem == 0) {
                chr.deleteFromHyperRocks(slea.readInt());
            } else if (addrem == 1) {
                if (!FieldLimitType.VipRock.check(chr.getMap().getFieldLimit())) {
                    chr.addHyperRockMap();
                } else {
                    chr.dropMessage(1, "순간이동이 불가능한 지역입니다.");
                }
            }
        } else {
            if (addrem == 0) {
                chr.deleteFromRegRocks(slea.readInt());
            } else if (addrem == 1) {
                if (!FieldLimitType.VipRock.check(chr.getMap().getFieldLimit())) {
                    chr.addRegRockMap();
                } else {
                    chr.dropMessage(1, "순간이동이 불가능한 지역입니다.");
                }
            }
        }
        c.getSession().write(MTSCSPacket.getTrockRefresh(chr, vip, addrem == 3));
    }

    public static final void CharInfoRequest(final int objectid, final MapleClient c, final MapleCharacter chr) {
        if (c.getPlayer() == null || c.getPlayer().getMap() == null) {
            return;
        }
        final MapleCharacter player = c.getPlayer().getMap().getCharacterById(objectid);
        c.getSession().write(MaplePacketCreator.enableActions());
        if (player != null) {
            if (!player.isGM() || c.getPlayer().isGM()) {
                c.getSession().write(MaplePacketCreator.charInfo(player, c.getPlayer().getId() == objectid));
            }
        }
    }

    public static final void TakeDamage(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        chr.updateTick(slea.readInt());//ida 기준 setdamaged1229
        //slea.readInt();
        final byte type = slea.readByte(); //-4 is mist, -3 and -2 are map damage.
        //chr.dropMessage(5, "타입" + type);
        final byte element = slea.readByte(); // Element - 0x00 = elementless, 0x01 = ice, 0x02 = fire, 0x03 = lightning
        int damage = slea.readInt();
        //chr.dropMessage(6, "클라이언트 데미지 표기 : " + damage);
        int rawdamage = damage;
        // slea.skip(1);
        int oid = 0;
        int monsteridfrom = 0;
        int reflectP = 0;
        double reduceR = 0;
        byte direction = 0;
        int pos_x = 0;
        int pos_y = 0;
        int fake = 0;
        int mpattack = 0;
        boolean is_pg = false;
        boolean isDeadlyAttack = false;
        MapleMonster attacker = null;
        if (chr.isHidden() || chr.getMap() == null) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (chr.getStat().getHp() <= 0) {
            chr.updateSingleStat(MapleStat.HP, 0);
            return;
        }
        if (chr.isGM() && chr.isInvincible()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (type == -2 && damage > 0) {
            chr.getMap().broadcastMessage(MaplePacketCreator.damagePlayer(0, 100100, chr.getId(), rawdamage, 0, (byte) 0, 0, is_pg, 0, 0, 0, 0));
        }
        final PlayerStats stats = chr.getStat();
        if (type != -2 && type != -3 && type != -4) { // Not map damage
            monsteridfrom = slea.readInt();
            oid = slea.readInt();
            attacker = chr.getMap().getMonsterByOid(oid);
            direction = slea.readByte(); //COutPacket::Encode1(v201);
            reflectP = slea.readByte() & 0xFF; //COutPacket::Encode1((char)v225); 여기서 1개썼으니까 나중에 스킬로 들어오는거 확인해야함
            /* 
             v211 = (IUnknown *)(대미지 * ?? / 100);
             */
            if (reflectP >= 1 || reflectP < 0) {
                slea.skip(1); //COutPacket::Encode1(v215); 109 새로 추가
                slea.skip(1); //COutPacket::Encode1(v224 == 0 ? 0 : (v220 != 0) + 1);
                byte pg = slea.readByte(); //COutPacket::Encode1(v225 == 0 ? 0 : a10 != 0);
                if (pg == 1) {
                    is_pg = true;
                } else {
                    is_pg = false;
                }
                oid = slea.readInt();
                slea.skip(10);
            }
            long bouncedamage = (long) (damage * reflectP / 100);
            final MapleMonster attacker2 = (MapleMonster) attacker;
            bouncedamage = Math.min(bouncedamage, attacker.getMobMaxHp() / 10);//몹 반감같은것도 생각해야함 그리프의 경우 절반만 들어감
                /*                 
             if ( CMob::IsBossMob(a7) )
             v221 /= 2; 보스면 뎀지 반감
             */
            attacker2.damage(chr, bouncedamage, true);
            //chr.dropMessage(5, "damage: " + damage + " || direction: " + direction + " || is_pg: " + is_pg + " || reflectP: " + reflectP + " || bouncedamage: " + bouncedamage);
            final MapleMonster mons = MapleLifeFactory.getMonster(monsteridfrom);
            if (mons == null) {
                return;
            }
            if (attacker == null || attacker.getLinkCID() > 0 || attacker.isFake() || attacker.getStats().isFriendly()) {
                if (mons.getStats().getSelfDHp() > 0) {//1229
                    chr.addHP(-damage);
                    chr.getMap().broadcastMessage(MaplePacketCreator.damagePlayer(0, 100100, chr.getId(), rawdamage, 0, (byte) 0, 0, is_pg, 0, 0, 0, 0));
                }
                return;
            }
            if (type != -1 && damage > 0) { // Bump damage
                final MobAttackInfo attackInfo = attacker.getStats().getMobAttack(type);
                if (attackInfo != null) {
                    if (attackInfo.isDeadlyAttack()) {
                        isDeadlyAttack = true;
                        mpattack = stats.getMp() - 1;
                    } else {
                        mpattack += attackInfo.getMpBurn();
                    }
                    final MobSkill skill = MobSkillFactory.getMobSkill(attackInfo.getDiseaseSkill(), attackInfo.getDiseaseLevel());
                    if (skill != null && (damage == -1 || damage > 0) && attacker.getStati() == null) {
                        skill.applyEffect(chr, attacker, false, (short) 0);
                        /*  try {
                         Thread.sleep(1000);
                         } catch (InterruptedException ex) {
                         Logger.getLogger(PlayerHandler.class.getName()).log(Level.SEVERE, null, ex);
                         }*/
                        // 와 시발 뭐 이딴게 여기있냐 좆댈려고 
                    }
                    attacker.setMp(attacker.getMp() - attackInfo.getMpCon());
                }
            }
        }
        if (type == -4) {
            final byte skillLevel = slea.readByte();
            final byte skillId = slea.readByte();
            final byte donno = slea.readByte();
            /*if (skillLevel > 0) {
             final MobSkill skill = MobSkillFactory.getMobSkill(-skillId, skillLevel);
             if (skill != null) {
             skill.applyEffect(chr, attacker, false, 0);
             }
             chr.dropMessage(6, "스킬 레벨: " + skillLevel + " || 스킬 아이디: " + skillId + " || 모름: " + donno + " || 스킬: " + skill);
             }*/
        }
        if (damage == -1) {
            fake = 4020002 + ((chr.getJob() / 10 - 40) * 100000);
            if (fake != 4120002 && fake != 4220002) {
                fake = 4120002;
            }
            if (type == -1 && chr.getJob() == 122 && attacker != null && chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -10) != null) {
                if (chr.getTotalSkillLevel(1220006) > 0) {
                    final MapleStatEffect eff = SkillFactory.getSkill(1220006).getEffect(chr.getTotalSkillLevel(1220006));
                    attacker.applyStatus(chr, new MonsterStatusEffect(MonsterStatus.STUN, 1, 1220006, null, false), false, eff.getDuration(), true, eff);
                    fake = 1220006;
                }
            }
            if (chr.getTotalSkillLevel(fake) <= 0) {
                return;
            }
        } else if (damage < -1 || damage > 200000) {
            //AutobanManager.getInstance().addPoints(c, 1000, 60000, "Taking abnormal amounts of damge from " + monsteridfrom + ": " + damage);
            return;
        }

        Pair<Double, Boolean> modify = chr.modifyDamageTaken((double) damage, attacker);
        damage = modify.left.intValue();
        if (damage > 0) {
            chr.getCheatTracker().setAttacksWithoutHit(false);

            if (chr.getBuffedValue(MapleBuffStat.MORPH) != null) {
                chr.cancelMorphs();
            }
            if (slea.available() == 3 || slea.available() == 4) {
                //chr.dropMessage(6, "요기좀 봐주세요1");
                byte level = slea.readByte();
                if (level > 0) {
                    final MobSkill skill = MobSkillFactory.getMobSkill(slea.readShort(), level);
                    if (skill != null) {
                        skill.applyEffect(chr, attacker, false, (short) 0);
                    }
                }
            }
            final int[] achilles = {1220005, 1120004, 1320005};
            for (int sid : achilles) {
                int slv = chr.getTotalSkillLevel(sid);
                if (slv != 0) {
                    reduceR = 0.005 * slv;//데미지이상하게들어옴
                    //chr.dropMessage(6, "아킬퍼센트" + reduceR + " 스킬레벨" + slv);
                }
            }

            if (reflectP != 0 || reduceR != 0) {
                damage -= ((reflectP * 0.01) + reduceR) * damage;
            } else if (reflectP != 0 || reduceR == 0) {
                damage -= reflectP * 0.01 * damage;
            } else if (reflectP == 0 || reduceR != 0) {
                damage -= reduceR * 0.01 * damage;
            }

            boolean mpAttack = chr.getBuffedValue(MapleBuffStat.MECH_CHANGE) != null && chr.getBuffSource(MapleBuffStat.MECH_CHANGE) != 35121005;
            if (chr.getBuffedValue(MapleBuffStat.MAGIC_GUARD) != null) {
                int hploss = 0, mploss = 0;
                if (isDeadlyAttack) {
                    if (stats.getHp() > 1) {
                        hploss = stats.getHp() - 1;
                    }
                    if (stats.getMp() > 1) {
                        mploss = stats.getMp() - 1;
                    }
                    if (chr.getBuffedValue(MapleBuffStat.INFINITY) != null) {
                        mploss = 0;
                    }
                    chr.addMPHP(-hploss, -mploss);
                    //} else if (mpattack > 0) {
                    //    chr.addMPHP(-damage, -mpattack);
                } else {
                    mploss = (int) (damage * (chr.getBuffedValue(MapleBuffStat.MAGIC_GUARD).doubleValue() / 100.0)) + mpattack;
                    hploss = damage - mploss;
                    if (chr.getBuffedValue(MapleBuffStat.INFINITY) != null) {
                        mploss = 0;
                    } else if (mploss > stats.getMp()) {
                        mploss = stats.getMp();
                        hploss = damage - mploss + mpattack;
                    }
                    chr.addMPHP(-hploss, -mploss);
                }

            } else if (chr.getStat().mesoGuardMeso > 0) {
                //handled in client
                int mesoloss = 0;
                mesoloss = (int) (damage * (chr.getStat().mesoGuardMeso / 100.0));
                if (chr.getMeso() < mesoloss) {
                    chr.gainMeso(-chr.getMeso(), false);
                    mesoloss = chr.getMeso();
                    chr.cancelBuffStats(true, MapleBuffStat.MESOGUARD);
                } else {
                    chr.gainMeso(-mesoloss, false);
                }
                if (isDeadlyAttack && stats.getMp() > 1) {
                    mpattack = stats.getMp() - 1;
                }
                chr.addMPHP(-(damage - mesoloss), -mpattack);
            } else {
                if (isDeadlyAttack) {
                    chr.addMPHP(stats.getHp() > 1 ? -(stats.getHp() - 1) : 0, stats.getMp() > 1 && !mpAttack ? -(stats.getMp() - 1) : 0);
                } else {
                    chr.addMPHP(-damage, mpAttack ? 0 : -mpattack);
                    //chr.dropMessage(6, "대미지" + damage);
                }
            }
            //chr.handleBattleshipHP(-damage); 鍮낅콉??諛고??쎈궡援щ룄 ?щ씪吏?.
            if (chr.inPVP() && chr.getStat().getHPPercent() <= 20) {
                SkillFactory.getSkill(chr.getStat().getSkillByJob(93, chr.getJob())).getEffect(1).applyTo(chr);
            }
        }
        byte offset = 0;
        /* if (slea.available() == 1) {
         offset = slea.readByte();
         if (offset < 0 || offset > 2) {
         offset = 0;
         }
         }*/
        //c.getSession().write(MaplePacketCreator.enableActions());
        chr.getMap().broadcastMessage(chr, MaplePacketCreator.damagePlayer(type, monsteridfrom, chr.getId(), rawdamage, fake, direction, reflectP, reduceR, is_pg, oid, pos_x, pos_y, offset), false);
    }

    public static final void AranCombo(final MapleClient c, final MapleCharacter chr, int toAdd) {
        if (chr != null && chr.getJob() >= 2000 && chr.getJob() <= 2112) {
            short combo = chr.getCombo();
            final long curr = System.currentTimeMillis();

            if (combo > 0 && (curr - chr.getLastCombo()) > 7000) {
                // Official MS timing is 3.5 seconds, so 7 seconds should be safe.
                //chr.getCheatTracker().registerOffense(CheatingOffense.ARAN_COMBO_HACK);
                combo = 0;
            }
            combo = (short) Math.min(30000, combo + toAdd);
            chr.setLastCombo(curr);
            chr.setCombo(combo);

            c.getSession().write(MaplePacketCreator.testCombo(combo));

            switch (combo) { // Hackish method xD
                case 10:
                case 20:
                case 30:
                case 40:
                case 50:
                case 60:
                case 70:
                case 80:
                case 90:
                case 100:
                    if (chr.getSkillLevel(21000000) >= (combo / 10)) {
                        SkillFactory.getSkill(21000000).getEffect(combo / 10).applyComboBuff(chr, combo);
                    }
                    break;
            }
        }
    }

    public static final void UseItemEffect(final int itemId, final MapleClient c, final MapleCharacter chr) {
        final Item toUse = chr.getInventory(MapleInventoryType.CASH).findById(itemId);
        if (itemId != 0 && (toUse == null || toUse.getItemId() != itemId || toUse.getQuantity() < 1)) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        
        if (itemId != 5010006) {
            chr.setItemEffect(itemId);
//            System.out.println(itemId);
            chr.customizeStat(0);
        }
        chr.getMap().broadcastMessage(chr, MaplePacketCreator.itemEffect(chr.getId(), itemId), false);
    }

    public static final void CancelItemEffect(final int id, final MapleCharacter chr) {
        if (-id == 2022536) {//지하신전의 봉인
            chr.dropMessage(5, "이 버프는 해제 하실 수 없습니다.");
            return;
        }
        chr.cancelEffect(MapleItemInformationProvider.getInstance().getItemEffect(-id), -1);
    }

    public static final void CancelBuffHandler(int sourceid, final MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return;
        }
        //51 99 17 02
        /*
         if (sourceid == 32001003) {
         if (chr.getSkillLevel(32120000) >= 1) {
         sourceid = 32120000;
         }
         }
         if (sourceid == 32101003) {
         if (chr.getSkillLevel(32120001) >= 1) {
         sourceid = 32120001;
         }
         }
         if (sourceid == 32101002) {
         if (chr.getSkillLevel(32110000) >= 1) {
         sourceid = 32110000;
         }
         }
         */
        if (sourceid == 35001002) {
            if (chr.getSkillLevel(35120000) >= 1) {
                sourceid = 35120000;
            }
        }
        /*
         if (sourceid == 35101009) {
         if (chr.getSkillLevel(35001001) >= 1) {
         sourceid = 35001001;
         }
         }
         */
        final Skill skill = SkillFactory.getSkill(sourceid);
        //chr.dropMessage(6, "캔슬 시작");
        if (skill.isChargeSkill()) {
            chr.setKeyDownSkill_Time(0);
            if (sourceid != 35101009 && sourceid != 35001001) {
                chr.getMap().broadcastMessage(chr, MaplePacketCreator.skillCancel(chr, sourceid), false);
            }
            if (skill.getId() == 4341002) { // 파이널 컷 버프 해제
                chr.cancelEffect(skill.getEffect(1), -1);
            }
            if (sourceid == 35101009 || sourceid == 27101202) {
                chr.cancelEffect(skill.getEffect(1), -1);
                //   chr.dropMessage(6, "캔슬 시작");
            }
           // chr.dropMessage(6, "종료");
        } else {
            if (skill.getId() == 4331003) { // 아울 데드 버프 해제
                chr.setBattleshipHP(0);
            }
            chr.cancelEffect(skill.getEffect(chr.getTotalSkillLevel(skill)), -1);
           // chr.dropMessage(6, "종료2");
        }
    }

    public static final void CancelMech(final LittleEndianAccessor slea, final MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        int sourceid = slea.readInt();
        if (sourceid % 10000 < 1000 && SkillFactory.getSkill(sourceid) == null) {
            sourceid += 1000;
        }
        final Skill skill = SkillFactory.getSkill(sourceid);
        int skillLevel = slea.readByte();
        if (skill == null) { //not sure
            return;
        }
        if (skill.isChargeSkill()) {
            chr.setKeyDownSkill_Time(0);

            if (sourceid != 35001001 && sourceid != 35101009) {
                chr.getMap().broadcastMessage(chr, MaplePacketCreator.skillCancel(chr, sourceid), false);
                chr.cancelEffect(skill.getEffect(slea.readByte()), -1); //메카닉 한줄추가함
            }
        } else {
            //if (sourceid != 35001001 && sourceid != 35101009) {
            chr.cancelEffect(skill.getEffect(slea.readByte()), -1);
            //}
        }
        if (sourceid == 35111004 || sourceid == 35121005 || sourceid == 35121013 || sourceid == 35001001) {
            chr.cancelEffect(skill.getEffect(skillLevel), -1);
            sourceid -= 1000;
            chr.getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(sourceid, 1, skillLevel, skillLevel, (byte) 1));
            chr.getMap().broadcastMessage(MaplePacketCreator.showBuffeffect(chr.getId(), sourceid, 1, skillLevel, skillLevel, (byte) 1));
        }
    }

    
    public static final void QuickSlot(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final StringBuilder ret = new StringBuilder();
        for (int i = 0; i < 8; i++) { //really hacky way of doing it
            ret.append(slea.readAsciiString(1));
            slea.skip(3);
        }
        chr.getQuestNAdd(MapleQuest.getInstance(GameConstants.QUICK_SLOT)).setCustomData(ret.toString());
    }

    public static final void SkillEffect(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final int skillId = slea.readInt();
        if (skillId >= 91000000) { //guild/recipe? no
            chr.getClient().getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        final byte level = slea.readByte();
        final byte flags = slea.readByte();
        final byte speed = slea.readByte();
        final byte unk = slea.readByte(); // Added on v.82

        final Skill skill = SkillFactory.getSkill(GameConstants.getLinkedAranSkill(skillId));
        if (chr == null || skill == null || chr.getMap() == null) {
            return;
        }
        final int skilllevel_serv = chr.getTotalSkillLevel(skill);
        MapleStatEffect effect = skill.getEffect(level);
        if (skillId == 33101005 || skill.isChargeSkill() || skillId == 35001001 || skillId == 35101009 || skillId == 27101202 || skillId == 27111100 || skillId == 35100008) {
            chr.setKeyDownSkill_Time(System.currentTimeMillis());
            if (skillId == 33101005 || skillId == 35001001 || skillId == 35101009 || skillId == 35100008 || skillId == 27101202 || skillId == 27111100) {
                if (skillId == 33101005) {
                    chr.setLinkMid(slea.readInt(), 0);
                }
                effect.applyTo(chr);
                if (skillId != 35101009) {
                    chr.getMap().broadcastMessage(chr, MaplePacketCreator.skillEffect(chr, skillId, level, flags, speed, unk), false);
                }
            } else {
                chr.getMap().broadcastMessage(chr, MaplePacketCreator.skillEffect(chr, skillId, level, flags, speed, unk), false);
            }
        }
    }

    //public static final void SpecialMove(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr, int count) {
    public static final void SpecialMove(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || chr.hasBlockedInventory() || chr.getMap() == null /*|| slea.available() < 9*/) { //1.2.6 
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        //[UNKNOWN] 1F 00 C9 14 F9 01 20 00 03 뭐지
        Point oldpos = null;
//        oldpos = slea.readPos(); //no oldPos //Real update_time
        int updateTime = slea.readInt();
        //slea.skip(4); // Old X and Y
        int skillid = slea.readInt();
        int skillLevel = slea.readByte();
        //System.out.println("skillid: " + skillid);

        final Skill skill = SkillFactory.getSkill(skillid);

        if (skill == null) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        MapleStatEffect linkEffect = SkillFactory.getSkill(skillid).getEffect(skillLevel);
        if (skillid == 35111002) {
            List<Integer> count = new ArrayList<Integer>();
            final List<MapleSummon> ss = chr.getSummonsReadLock();
            try {
                for (MapleSummon s : ss) {
                    if (s.getSkill() == skillid) {
                        count.add(s.getObjectId());
                    }
                }
            } finally {
                chr.unlockSummonsReadLock();
            }
            if (count.size() == 2) {
                c.getSession().write(MaplePacketCreator.skillCooldown(skillid, linkEffect.getCooldown()));
                chr.addCooldown(skillid, System.currentTimeMillis(), linkEffect.getCooldown());
            }
        }
        if (chr.getTotalSkillLevel(GameConstants.getLinkedAranSkill(skillid)) <= 0 || chr.getTotalSkillLevel(GameConstants.getLinkedAranSkill(skillid)) != skillLevel) {
            if (!GameConstants.isMulungSkill(skillid) && !GameConstants.isPyramidSkill(skillid) && chr.getTotalSkillLevel(GameConstants.getLinkedAranSkill(skillid)) <= 0) {
                c.getSession().close();
                return;
            }
            if (GameConstants.isMulungSkill(skillid)) {
                if (chr.getMapId() / 10000 != 92502) {
                    //AutobanManager.getInstance().autoban(c, "Using Mu Lung dojo skill out of dojo maps.");
                    return;
                } else {
                    if (chr.getMulungEnergy() < 300) {
                        return;
                    }
                    chr.mulung_EnergyModify(false);
                }
            } else if (GameConstants.isPyramidSkill(skillid)) {
                if (chr.getMapId() / 10000 != 92602 && chr.getMapId() / 10000 != 92601) {
                    //AutobanManager.getInstance().autoban(c, "Using Pyramid skill out of pyramid maps.");
                    return;
                }
            }
        }
        if (GameConstants.isEventMap(chr.getMapId())) {
            for (MapleEventType t : MapleEventType.values()) {
                final MapleEvent e = ChannelServer.getInstance(chr.getClient().getChannel()).getEvent(t);
                if (e.isRunning() && !chr.isGM()) {
                    for (int i : e.getType().mapids) {
                        if (chr.getMapId() == i) {
                            c.getSession().write(MaplePacketCreator.enableActions());
                            chr.dropMessage(5, "이곳에서 스킬을 사용할 수 없습니다.");
                            return; //non-skill cannot use
                        }
                    }
                }
            }
        }
        skillLevel = chr.getTotalSkillLevel(GameConstants.getLinkedAranSkill(skillid));
        final MapleStatEffect effect = skill.getEffect(skillLevel);
        if (effect.isMPRecovery() && chr.getStat().getHp() < (chr.getStat().getMaxHp() / 100) * 10) { //less than 10% hp
            c.getPlayer().dropMessage(5, "스킬을 사용하는데 필요한 HP가 부족합니다.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (effect.getCooldown() > 0 && !chr.isGM() && skillid != 35111002) {
            if (chr.skillisCooling(skillid)) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            if (skillid != 35111004 && skillid != 35121013 && skillid != 5221006 && skillid != 35111002 || chr.getBuffSource(MapleBuffStat.MECH_CHANGE) != skillid) {
                c.getSession().write(MaplePacketCreator.skillCooldown(skillid, effect.getCooldown()));
                chr.addCooldown(skillid, System.currentTimeMillis(), effect.getCooldown() * 1000);
            }
        }

        //chr.checkFollow(); //not msea-like but ALEX'S WISHES
        switch (skillid) {
            case 1121001:
            case 1221001:
            case 1321001:
                final byte number_of_mobs = slea.readByte();
                slea.skip(3); //number가 4바이트라는건 생각을 못했나? 아무튼 부호 있는 1바이트도 127까지 가능하니 상관 없음.
                for (int i = 0; i < number_of_mobs; i++) {
                    int mobId = slea.readInt();
                    byte success = slea.readByte();
                    final MapleMonster mob = chr.getMap().getMonsterByOid(mobId);
                    if (mob != null) {
                        chr.getMap().broadcastMessage(chr, MaplePacketCreator.showMagnet(mobId, success), oldpos);
                        mob.switchController(chr, mob.isControllerHasAggro());
                        //mob.applyStatus(chr, new MonsterStatusEffect(MonsterStatus.STUN, 1, skillid, null, false), false, effect.getDuration(), true, effect);
                    }
                }
                byte direction = slea.readByte();
                chr.getMap().broadcastMessage(chr, MaplePacketCreator.showBuffeffect(chr.getId(), skillid, 1, chr.getLevel(), skillLevel, direction), oldpos);
                c.getSession().write(MaplePacketCreator.enableActions());
                break;
            case 30001061: //capture
                int mobID = slea.readInt();
                MapleMonster mob = chr.getMap().getMonsterByOid(mobID);
                if (mob != null) {
                    boolean success = mob.getHp() <= mob.getMobMaxHp() / 2;
                    chr.getMap().broadcastMessage(chr, MaplePacketCreator.showBuffeffect(chr.getId(), skillid, 1, chr.getLevel(), skillLevel, (byte) (success ? 1 : 0)), oldpos);
                    if (success) {//이부분 첨에 null체크 안해줘서 팅기는것같은데
                        chr.getQuestNAdd(MapleQuest.getInstance(GameConstants.JAGUAR)).setCustomData(String.valueOf((mob.getId() - 9303999) * 10));
                        chr.getMap().killMonster(mob, chr, true, false, (byte) 1);
                        chr.cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
                        c.getSession().write(MaplePacketCreator.updateJaguar(chr));
                        chr.getMap().broadcastMessage(MaplePacketCreator.catchMonster(mob.getId(), skillid, (byte) 1));
                        chr.getMap().broadcastMessage(MaplePacketCreator.showMagnet(mob.getObjectId(), (byte) 1));
                    } else {
                        chr.dropMessage(5, "몬스터의 체력이 너무많습니다.");
                    }
                }
                c.getSession().write(MaplePacketCreator.enableActions());
                break;
            case 33101005: //jaguar oshi
                mobID = chr.getFirstLinkMid();
                mob = chr.getMap().getMonsterByOid(mobID);
                chr.setKeyDownSkill_Time(0);
                chr.getMap().broadcastMessage(chr, MaplePacketCreator.skillCancel(chr, skillid), false);
                if (mob != null) {
                    boolean success = mob.getStats().getLevel() < chr.getLevel() && mob.getId() < 9000000 && !mob.getStats().isBoss();
                    if (success) {
                        chr.getMap().broadcastMessage(MobPacket.suckMonster(mob.getObjectId(), chr.getId()));
                        chr.getMap().killMonster(mob, chr, false, false, (byte) -1);
                        chr.clearLinkMid();
                    } else {
                        chr.dropMessage(5, "The monster has too much physical strength, so you cannot catch it.");
                    }
                } else {
                    chr.dropMessage(5, "No monster was sucked. The skill failed.");
                }
                c.getSession().write(MaplePacketCreator.enableActions());
                break;
            case 1111007: {
                Point pos = null;
                pos = slea.readPos();
                int oid = 0;
                int count = slea.readByte();
                List<MonsterStatus> toCancel = new ArrayList<MonsterStatus>();
                toCancel.add(MonsterStatus.WEAPON_DEFENSE_UP);
                for (int i = 0; i < count; i++) {
                    oid = slea.readInt();
                    MapleMonster mob32 = chr.getMap().getMonsterByOid(oid);
                    if (Randomizer.nextInt(100) < skill.getEffect(chr.getSkillLevel(skillid)).getProb()) {
                        for (MonsterStatus stat : toCancel) {
                            mob32.cancelStatus(stat);
                        }
                    }
                    mob32.applyStatus(chr, new MonsterStatusEffect(MonsterStatus.SEAL, 1, skill.getId(), null, false), effect.isPoison(), effect.getDuration(), true, effect);
                }
                slea.skip(3);
                effect.applyTo(c.getPlayer(), pos);
                c.getSession().write(MaplePacketCreator.enableActions());
                break;
            }
            case 1211009: {
                Point pos = null;
                // pos = slea.readPos();
                //  slea.skip(3);
                int mobid = 0;
                int count = slea.readByte();
                List<MonsterStatus> toCancel = new ArrayList<MonsterStatus>();
                toCancel.add(MonsterStatus.MAGIC_DEFENSE_UP);

                MapleStatEffect Dispel = skill.getEffect(chr.getSkillLevel(skillid));

                for (int i = 0; i < count; i++) {
                    mobid = slea.readInt();
                    MapleMonster mob32 = chr.getMap().getMonsterByOid(mobid);
                    if (Randomizer.nextInt(100) < skill.getEffect(chr.getSkillLevel(skillid)).getProb()) {
                        for (MonsterStatus stat : toCancel) {
                            mob32.cancelStatus(stat);
                        }
                    }
                    mob32.applyStatus(chr, new MonsterStatusEffect(MonsterStatus.SEAL, 1, skill.getId(), null, false), effect.isPoison(), effect.getDuration(), true, effect);
                }
                effect.applyTo(c.getPlayer(), pos);
                c.getSession().write(MaplePacketCreator.enableActions());
                break;
            }
            case 1311007: {
                Point pos = null;
                // pos = slea.readPos();
                //  slea.skip(3);
                int mobid = 0;
                int count = slea.readByte();
                List<MonsterStatus> toCancel = new ArrayList<MonsterStatus>();
                toCancel.add(MonsterStatus.WEAPON_ATTACK_UP);

                MapleStatEffect Dispel = skill.getEffect(chr.getSkillLevel(skillid));

                for (int i = 0; i < count; i++) {
                    mobid = slea.readInt();
                    MapleMonster mob32 = chr.getMap().getMonsterByOid(mobid);
                    if (Randomizer.nextInt(100) < skill.getEffect(chr.getSkillLevel(skillid)).getProb()) {
                        for (MonsterStatus stat : toCancel) {
                            mob32.cancelStatus(stat);
                        }
                    }
                    mob32.applyStatus(chr, new MonsterStatusEffect(MonsterStatus.SEAL, 1, skill.getId(), null, false), effect.isPoison(), effect.getDuration(), true, effect);
                }
                effect.applyTo(c.getPlayer(), pos);
                c.getSession().write(MaplePacketCreator.enableActions());
                break;
            }
            case 2311001: {
                Point pos = null;
                //    pos = slea.readPos();
                Point antiRepeatSkillPoint = slea.readPos();
                byte affectedMemberBitmap = slea.readByte();
                short idk = slea.readShort(); //flag ?
                byte idk2 = slea.readByte();
                
                List<MonsterStatus> toCancel = new ArrayList<MonsterStatus>();
                toCancel.add(MonsterStatus.WEAPON_ATTACK_UP);
                toCancel.add(MonsterStatus.MAGIC_ATTACK_UP);
                toCancel.add(MonsterStatus.WEAPON_DEFENSE_UP);
                toCancel.add(MonsterStatus.MAGIC_DEFENSE_UP);
                toCancel.add(MonsterStatus.SPEED);
                
                int mobCount = slea.readByte();
                for (int i = 0; i < mobCount; i++) {
                    int monsterOID = slea.readInt();
                    MapleMonster mob32 = chr.getMap().getMonsterByOid(monsterOID);
                    if (Randomizer.nextInt(100) < skill.getEffect(chr.getSkillLevel(skillid)).getProb()) {
                        for (MonsterStatus stat : toCancel) {
                            mob32.cancelStatus(stat);
                        }
                    }
                    mob32.applyStatus(chr, new MonsterStatusEffect(MonsterStatus.SEAL, 1, skill.getId(), null, false), effect.isPoison(), effect.getDuration(), true, effect);
                }
                slea.readShort(); //same idk2
                slea.readByte(); //bByPet (Only Zero)
                
                effect.applyTo(c.getPlayer(), pos);
                
//                slea.skip(4); //antiRepeatSkill
//                int mobid = 0;
//                int count = slea.readByte(); //Only Zero
//                List<MonsterStatus> toCancel = new ArrayList<MonsterStatus>();
//                toCancel.add(MonsterStatus.WEAPON_ATTACK_UP);
//                toCancel.add(MonsterStatus.MAGIC_ATTACK_UP);
//                toCancel.add(MonsterStatus.WEAPON_DEFENSE_UP);
//                toCancel.add(MonsterStatus.MAGIC_DEFENSE_UP);
//                //  toCancel.add(MonsterStatus.DAMAGE_IMMUNITY);
//                //  toCancel.add(MonsterStatus.WEAPON_IMMUNITY);
//                //  toCancel.add(MonsterStatus.MAGIC_IMMUNITY);
//                toCancel.add(MonsterStatus.SPEED);
//
//                MapleStatEffect Dispel = skill.getEffect(chr.getSkillLevel(skillid));
//
//                for (int i = 0; i < count; i++) {
//                    mobid = slea.readInt();
//                    MapleMonster mob32 = chr.getMap().getMonsterByOid(mobid);
//                    if (Randomizer.nextInt(100) < skill.getEffect(chr.getSkillLevel(skillid)).getProb()) {
//                        for (MonsterStatus stat : toCancel) {
//                            mob32.cancelStatus(stat);
//                        }
//                    }
//                    mob32.applyStatus(chr, new MonsterStatusEffect(MonsterStatus.SEAL, 1, skill.getId(), null, false), effect.isPoison(), effect.getDuration(), true, effect);
//                }
//                effect.applyTo(c.getPlayer(), pos);
                c.getSession().write(MaplePacketCreator.enableActions());
                break;
            }
            /*
            case 35111004: {
                if (chr.getBuffSource(MapleBuffStat.MECH_CHANGE) == 35121005) {
                    if (!chr.isHidden()) {
                        effect.setSourceId(35121013);
                        EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                        stat.put(MapleBuffStat.MECH_CHANGE, 1);
                        chr.getMap().broadcastMessage(chr, MaplePacketCreator.giveForeignBuff(chr, stat, effect), false);
                    }
                    chr.getClient().getSession().write(MaplePacketCreator.giveMisileToHeavy(35121013));
                    c.getSession().write(MaplePacketCreator.enableActions());
                } else {
                    if (!chr.isHidden()) {
                        effect.setSourceId(35111004);
                        EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                        stat.put(MapleBuffStat.MECH_CHANGE, 1);
                        chr.getMap().broadcastMessage(chr, MaplePacketCreator.giveForeignBuff(chr, stat, effect), false);
                    }
                    chr.getClient().getSession().write(MaplePacketCreator.giveMisileToHeavy(35111004));
                    c.getSession().write(MaplePacketCreator.enableActions());
                    effect.setSourceId(35111004);
                    effect.applyTo(c.getPlayer(), null);
                }
                break;
            }
            */
            case 9001000: {
                Point pos = null;
                pos = slea.readPos();
                slea.skip(3);
                int mobid = 0;
                int count = slea.readByte();
                List<MonsterStatus> toCancel = new ArrayList<MonsterStatus>();
                toCancel.add(MonsterStatus.WEAPON_ATTACK_UP);
                toCancel.add(MonsterStatus.MAGIC_ATTACK_UP);
                toCancel.add(MonsterStatus.WEAPON_DEFENSE_UP);
                toCancel.add(MonsterStatus.MAGIC_DEFENSE_UP);
                toCancel.add(MonsterStatus.DAMAGE_IMMUNITY);
                toCancel.add(MonsterStatus.WEAPON_IMMUNITY);
                toCancel.add(MonsterStatus.MAGIC_IMMUNITY);
                toCancel.add(MonsterStatus.SPEED);

                MapleStatEffect Dispel = skill.getEffect(chr.getSkillLevel(skillid));

                for (int i = 0; i < count; i++) {
                    mobid = slea.readInt();
                    MapleMonster mob32 = chr.getMap().getMonsterByOid(mobid);
                    if (Randomizer.nextInt(100) < skill.getEffect(chr.getSkillLevel(skillid)).getProb()) {
                        for (MonsterStatus stat : toCancel) {
                            mob32.cancelStatus(stat);
                        }
                    }
                    mob32.applyStatus(chr, new MonsterStatusEffect(MonsterStatus.SEAL, 1, skill.getId(), null, false), effect.isPoison(), effect.getDuration(), true, effect);
                }
                effect.applyTo(c.getPlayer(), pos);
                c.getSession().write(MaplePacketCreator.enableActions());
                break;
            }
            default:
                if (skillid == 9001004) {
                    chr.setHidden(!chr.isHidden());
                    if (!chr.isHidden()) {
                        if (GameConstants.isEvan(chr.getJob()) && chr.getJob() >= 2200) {
                            chr.makeDragon();
                        }
                    }
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }

                Point pos = null;
                if (skillid != 4121006 && (slea.available() == 4 || slea.available() == 5 || slea.available() == 7)) {
                    pos = slea.readPos();
                }
                if (effect.isMagicDoor()) { // Mystic Door
                    if (!FieldLimitType.MysticDoor.check(chr.getMap().getFieldLimit())) {
                        effect.applyTo(c.getPlayer(), pos);
                    } else {
                        c.getSession().write(MaplePacketCreator.enableActions());
                    }
                } else {
                    final int mountid = MapleStatEffect.parseMountInfo(c.getPlayer(), skill.getId());
                    effect.applyTo(c.getPlayer(), pos);
                    //chr.dropMessage(6, "타입1");
                    //chr.dropMessage(6, "slea.available()" + slea.available());
                    //chr.dropMessage(6, "oldpos" + pos);
                }
                break;
        }
    }

    public static final void closeRangeAttack(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr, final boolean energy) {
        if (chr == null || (energy && chr.getBuffedValue(MapleBuffStat.ENERGY_CHARGE) == null && chr.getBuffedValue(MapleBuffStat.BODY_PRESSURE) == null && chr.getBuffedValue(MapleBuffStat.DARK_AURA) == null && chr.getBuffedValue(MapleBuffStat.TORNADO) == null && chr.getBuffedValue(MapleBuffStat.SUMMON) == null && chr.getBuffedValue(MapleBuffStat.RAINING_MINES) == null && chr.getBuffedValue(MapleBuffStat.TELEPORT_MASTERY) == null && chr.getBuffedValue(MapleBuffStat.ONYX_WILL) == null)) {
            return;
        }
        if (chr.hasBlockedInventory() || chr.getMap() == null) {
            return;
        }

        AttackInfo attack = DamageParse.parseDmgM(slea, chr);
        if (attack == null) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        final boolean mirror = (chr.getBuffedValue(MapleBuffStat.SHADOWPARTNER) != null || chr.getBuffedValue(MapleBuffStat.미러이미징) != null);
        double maxdamage = chr.getStat().getCurrentMaxBaseDamage();
        final Item shield = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10);
        int attackCount = (shield != null && shield.getItemId() / 10000 == 134 ? 2 : 1);
        int skillLevel = 0;
        MapleStatEffect effect = null;
        Skill skill = null;

        if (attack.skill != 0) {
            skill = SkillFactory.getSkill(GameConstants.getLinkedAranSkill(attack.skill));
            if (skill == null || (GameConstants.isAngel(attack.skill) && (chr.getStat().equippedSummon % 10000) != (attack.skill % 10000))) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            skillLevel = chr.getTotalSkillLevel(skill);
            effect = attack.getAttackEffect(chr, skillLevel, skill);
            if (effect == null) {
                return;
            }
            
            //브랜디쉬 스킬 이펙트 
//            if (attack.skill == 1121008) {
//                int effectID = 8001100;
//                if (chr.getBuffedValue(MapleBuffStat.ENRAGE) != null) {
//                    effectID = 8001101;
//                }
//                c.sendPacket(MaplePacketCreator.showOwnBuffEffect(effectID, 1, chr.getLevel(), 1));
//                chr.getMap().broadcastMessage(chr, MaplePacketCreator.showBuffeffect(chr.getId(), effectID, 1, chr.getLevel(), 1), false);
//            }
            
            if (GameConstants.isEventMap(chr.getMapId())) {
                for (MapleEventType t : MapleEventType.values()) {
                    final MapleEvent e = ChannelServer.getInstance(chr.getClient().getChannel()).getEvent(t);
                    if (e.isRunning() && !chr.isGM()) {
                        for (int i : e.getType().mapids) {
                            if (chr.getMapId() == i) {
                                chr.dropMessage(5, "You may not use that here.");
                                return; //non-skill cannot use
                            }
                        }
                    }
                }
            }
            maxdamage *= (effect.getDamage() + chr.getStat().getDamageIncrease(attack.skill)) / 100.0;
            attackCount = effect.getAttackCount();

            if (effect.getCooldown() > 0 && !chr.isGM() && !energy) {
                if (chr.skillisCooling(attack.skill)) {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
                c.getSession().write(MaplePacketCreator.skillCooldown(attack.skill, effect.getCooldown()));
                chr.addCooldown(attack.skill, System.currentTimeMillis(), effect.getCooldown() * 1000);
            }
        }
        if (!GameConstants.isComboSkill(attack.skill)) { //패닉 코마 멀티
            chr.getMap().broadcastMessage(chr, MaplePacketCreator.showBuffeffect(chr.getId(), attack.skill, 1, chr.getLevel(), skillLevel, (byte) 1), chr.getTruePosition());
        }
        attack = DamageParse.Modify_AttackCrit(attack, chr, 1, effect);
        attackCount *= (mirror ? 2 : 1);
        if (!energy) {
            if ((chr.getMapId() == 109060000 || chr.getMapId() == 109060002 || chr.getMapId() == 109060004) && attack.skill == 0) {
                MapleSnowballs.hitSnowball(chr);
            }
            // handle combo orbconsume
            int numFinisherOrbs = 0;
            final Integer comboBuff = chr.getBuffedValue(MapleBuffStat.COMBO);

            if (isFinisher(attack.skill) > 0) { // finisher
                if (comboBuff != null) {
                    numFinisherOrbs = comboBuff.intValue() - 1;
                }
                if (numFinisherOrbs <= 0) {
                    return;
                }
                chr.handleOrbconsume(isFinisher(attack.skill));
                if (!GameConstants.GMS) {
                    maxdamage *= numFinisherOrbs;
                }
            }
        }
//        if (attack.skill == 4341002) {
//            SkillFactory.getSkill(attack.skill).getEffect(chr.getSkillLevel(attack.skill)).applyTo(chr);
//        }
        chr.checkFollow();
        // chr.dropMessage(6, "타입 : C");
        if (/*attack.skill != 35001001 && */attack.skill != 35101009) {
            if (!chr.isHidden()) {
                chr.getMap().broadcastMessage(chr, MaplePacketCreator.closeRangeAttack(chr.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, attack.allDamage, energy, chr.getLevel(), chr.getStat().passive_mastery(), attack.unk, attack.charge), chr.getTruePosition());
            } else {
                chr.getMap().broadcastGMMessage(chr, MaplePacketCreator.closeRangeAttack(chr.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, attack.allDamage, energy, chr.getLevel(), chr.getStat().passive_mastery(), attack.unk, attack.charge), false);
            }
        }
        DamageParse.applyAttack(attack, skill, c.getPlayer(), attackCount, maxdamage, effect, mirror ? AttackType.NON_RANGED_WITH_MIRROR : AttackType.NON_RANGED);
    }

    public static final void rangedAttack(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        if (chr.hasBlockedInventory() || chr.getMap() == null) {
            return;
        }
        AttackInfo attack = DamageParse.parseDmgR(slea, chr);
        if (attack == null) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        int bulletCount = 1, skillLevel = 0;
        MapleStatEffect effect = null;
        Skill skill = null;
        boolean AOE = attack.skill == 4111004;
        boolean noBullet = (chr.getJob() >= 3500 && chr.getJob() <= 3512) || GameConstants.isCannon(chr.getJob()) || GameConstants.isMercedes(chr.getJob());
        if (attack.skill != 0) {
            skill = SkillFactory.getSkill(GameConstants.getLinkedAranSkill(attack.skill));
            if (skill == null || (GameConstants.isAngel(attack.skill) && (chr.getStat().equippedSummon % 10000) != (attack.skill % 10000))) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            skillLevel = chr.getTotalSkillLevel(skill);
            effect = attack.getAttackEffect(chr, skillLevel, skill);
            if (effect == null) {
                return;
            }
            if (GameConstants.isEventMap(chr.getMapId())) {
                for (MapleEventType t : MapleEventType.values()) {
                    final MapleEvent e = ChannelServer.getInstance(chr.getClient().getChannel()).getEvent(t);
                    if (e.isRunning() && !chr.isGM()) {
                        for (int i : e.getType().mapids) {
                            if (chr.getMapId() == i) {
                                chr.dropMessage(5, "You may not use that here.");
                                return; //non-skill cannot use
                            }
                        }
                    }
                }
            }
            switch (attack.skill) {
                case 13101005:
                case 21111004:
//                case 21110004: // Ranged but uses attackcount instead
                case 14101006: // Vampure
                case 21120006:
                case 11101004:
                case 1077:
                case 1079:
                case 11077:
                case 11078:
                case 11079:
                case 15111006:
                case 15111007:
                case 13111007: //Wind Shot
                case 33101007:
                case 33101002:
                case 33121002:
                case 33121001:
                case 21101004:
//                case 21100004:
                case 21110011:
                case 21100007:
                case 21000004:
                case 5121002:
                case 4121003:
                case 4221003:
                case 3201003:
                case 3101003:
                    AOE = true;
                    bulletCount = effect.getAttackCount();
                    break;
                case 1078:
                    AOE = true;
                    noBullet = true;
                    break;
                case 35121005:
                case 35111004:
                case 35121013:
                    AOE = true;
                    bulletCount = 6;
                    break;
                default:
                    bulletCount = effect.getBulletCount();
                    break;
            }
            if (noBullet && effect.getBulletCount() < effect.getAttackCount()) {
                bulletCount = effect.getAttackCount();
            }
            if (effect.getCooldown() > 0 && !chr.isGM() && ((attack.skill != 35111004 && attack.skill != 35121013) || chr.getBuffSource(MapleBuffStat.MECH_CHANGE) != attack.skill)) {
                // System.out.println("cooldown > 0");
                if (chr.skillisCooling(attack.skill)) {
                    //   System.out.println("cooldown > 0 return");
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
                c.getSession().write(MaplePacketCreator.skillCooldown(attack.skill, effect.getCooldown()));
                chr.addCooldown(attack.skill, System.currentTimeMillis(), effect.getCooldown() * 1000);
            }
        }
        attack = DamageParse.Modify_AttackCrit(attack, chr, 2, effect);
        final Integer ShadowPartner = chr.getBuffedValue(MapleBuffStat.SHADOWPARTNER);
        if (ShadowPartner != null) {
            bulletCount *= 2;
        }
        int projectile = 0, visProjectile = 0;
        if (!AOE && chr.getBuffedValue(MapleBuffStat.SOULARROW) == null && !noBullet) {
            Item ipp = chr.getInventory(MapleInventoryType.USE).getItem(attack.slot);
            /*if (ipp == null) {
             System.out.println("ipp null??");                    
             return;
             }*/
            projectile = ipp.getItemId();

            if (attack.csstar > 0) {
                if (chr.getInventory(MapleInventoryType.CASH).getItem(attack.csstar) == null) {
                    // System.out.println("cash??");
                    return;
                }
                visProjectile = chr.getInventory(MapleInventoryType.CASH).getItem(attack.csstar).getItemId();
            } else {
                visProjectile = projectile;
            }
            // Handle bulletcount
            if (chr.getBuffedValue(MapleBuffStat.SPIRIT_CLAW) == null) {
                int bulletConsume = bulletCount;
                if (effect != null && effect.getBulletConsume() != 0) {
                    bulletConsume = effect.getBulletConsume() * (ShadowPartner != null ? 2 : 1);
                }
                if (chr.getJob() == 412 && bulletConsume > 0 && ipp.getQuantity() < MapleItemInformationProvider.getInstance().getSlotMax(projectile)) {
                    final Skill expert = SkillFactory.getSkill(4120010);
                    if (chr.getTotalSkillLevel(expert) > 0) {
                        final MapleStatEffect eff = expert.getEffect(chr.getTotalSkillLevel(expert));
                        if (eff.makeChanceResult()) {
                            ipp.setQuantity((short) (ipp.getQuantity() + 1));
                            c.getSession().write(MaplePacketCreator.updateInventorySlot(MapleInventoryType.USE, ipp, false));
                            bulletConsume = 0; //regain a star after using
                            c.getSession().write(MaplePacketCreator.getInventoryStatus());
                        }
                    }
                }
                if (bulletConsume > 0) {
                    if (!MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, projectile, bulletConsume, false, true)) {
                        // System.out.println("use??");
                        chr.dropMessage(5, "표창/화살/불릿이 부족합니다.");
                        return;
                    }
                }
            }
        } else if (chr.getJob() >= 3500 && chr.getJob() <= 3512) {
            visProjectile = 2333000;
        } else if (GameConstants.isCannon(chr.getJob())) {
            visProjectile = 2333001;
        }
        double basedamage;
        int projectileWatk = 0;
        if (projectile != 0) {
            projectileWatk = MapleItemInformationProvider.getInstance().getWatkForProjectile(projectile);
        }
        final PlayerStats statst = chr.getStat();
        switch (attack.skill) {
            case 4001344: // Lucky Seven
            case 4121007: // Triple Throw
            case 14001004: // Lucky seven
            case 14111005: // Triple Throw
                basedamage = Math.max(statst.getCurrentMaxBaseDamage(), (float) ((float) ((statst.getTotalLuk() * 5.0f) * (statst.getTotalWatk() + projectileWatk)) / 100));
                break;
            case 4111004: // Shadow Meso
//		basedamage = ((effect.getMoneyCon() * 10) / 100) * effect.getProb(); // Not sure
                basedamage = 53000;
                break;
            default:
                basedamage = statst.getCurrentMaxBaseDamage();
                switch (attack.skill) {
                    case 3101005: // arrowbomb is hardcore like that
                        basedamage *= effect.getX() / 100.0;
                        break;
                }
                break;
        }
        if (effect != null) {
            basedamage *= (effect.getDamage() + statst.getDamageIncrease(attack.skill)) / 100.0;

            int money = effect.getMoneyCon();
            if (money != 0) {
                if (money > chr.getMeso()) {
                    money = chr.getMeso();
                }
                chr.gainMeso(-money, false);
            }
        }
        chr.checkFollow();
        //chr.dropMessage(6, "타입 : R : " + attack.skill);
        if (attack.skill != 33101005 && attack.skill != 33101006 && attack.skill != 33101007) {
            if (!chr.isHidden()) {
                if (attack.skill == 3211006) {
                    chr.getMap().broadcastMessage(chr, MaplePacketCreator.strafeAttack(chr.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, visProjectile, attack.allDamage, attack.position, chr.getLevel(), chr.getStat().passive_mastery(), attack.unk, chr.getTotalSkillLevel(3220010)), chr.getTruePosition());
                } else {
                    chr.getMap().broadcastMessage(chr, MaplePacketCreator.rangedAttack(chr.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, visProjectile, attack.allDamage, attack.position, chr.getLevel(), chr.getStat().passive_mastery(), attack.unk), chr.getTruePosition());
                }
            } else {
                if (attack.skill == 3211006) {
                    chr.getMap().broadcastGMMessage(chr, MaplePacketCreator.strafeAttack(chr.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, visProjectile, attack.allDamage, attack.position, chr.getLevel(), chr.getStat().passive_mastery(), attack.unk, chr.getTotalSkillLevel(3220010)), false);
                } else {
                    chr.getMap().broadcastGMMessage(chr, MaplePacketCreator.rangedAttack(chr.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, visProjectile, attack.allDamage, attack.position, chr.getLevel(), chr.getStat().passive_mastery(), attack.unk), false);
                }
            }
        }
        DamageParse.applyAttack(attack, skill, chr, bulletCount, basedamage, effect, ShadowPartner != null ? AttackType.RANGED_WITH_SHADOWPARTNER : AttackType.RANGED);
    }

    public static final void MagicDamage(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || chr.hasBlockedInventory() || chr.getMap() == null) {
            return;
        }
        AttackInfo attack = DamageParse.parseDmgMa(slea, chr);
        if (attack == null) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        final Skill skill = SkillFactory.getSkill(GameConstants.getLinkedAranSkill(attack.skill));
        if (skill == null || (GameConstants.isAngel(attack.skill) && (chr.getStat().equippedSummon % 10000) != (attack.skill % 10000))) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        
        final int skillLevel = chr.getTotalSkillLevel(skill);
        final MapleStatEffect effect = attack.getAttackEffect(chr, skillLevel, skill);
        if (effect == null) {
            return;
        }
        attack = DamageParse.Modify_AttackCrit(attack, chr, 3, effect);
        if (GameConstants.isEventMap(chr.getMapId())) {
            for (MapleEventType t : MapleEventType.values()) {
                final MapleEvent e = ChannelServer.getInstance(chr.getClient().getChannel()).getEvent(t);
                if (e.isRunning() && !chr.isGM()) {
                    for (int i : e.getType().mapids) {
                        if (chr.getMapId() == i) {
                            chr.dropMessage(5, "You may not use that here.");
                            return; //non-skill cannot use
                        }
                    }
                }
            }
        }
        double maxdamage = chr.getStat().getCurrentMaxBaseDamage() * (effect.getDamage() + chr.getStat().getDamageIncrease(attack.skill)) / 100.0;
        if (GameConstants.isPyramidSkill(attack.skill)) {
            maxdamage = 1;
        } else if (GameConstants.isBeginnerJob(skill.getId() / 10000) && skill.getId() % 10000 == 1000) {
            maxdamage = 40;
        }
        if (effect.getCooldown() > 0 && !chr.isGM()) {
            if (chr.skillisCooling(attack.skill)) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            c.getSession().write(MaplePacketCreator.skillCooldown(attack.skill, effect.getCooldown()));
            chr.addCooldown(attack.skill, System.currentTimeMillis(), effect.getCooldown() * 1000);
        }
        chr.checkFollow();
        if (!chr.isHidden()) {
            chr.getMap().broadcastMessage(chr, MaplePacketCreator.magicAttack(chr.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, attack.allDamage, attack.charge, chr.getLevel(), attack.unk), chr.getTruePosition());
        } else {
            chr.getMap().broadcastGMMessage(chr, MaplePacketCreator.magicAttack(chr.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, attack.allDamage, attack.charge, chr.getLevel(), attack.unk), false);
        }
        DamageParse.applyAttackMagic(attack, skill, c.getPlayer(), effect, maxdamage);
    }

    public static final void DropMeso(final int meso, final MapleCharacter chr) {
        if (!chr.isAlive() || (meso < 10 || meso > 50000) || (meso > chr.getMeso())) {
            chr.getClient().getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        chr.gainMeso(-meso, false, true);
        chr.getMap().spawnMesoDrop(meso, chr.getTruePosition(), chr, chr, true, (byte) 0);
        chr.getClient().getSession().write(MaplePacketCreator.enableActions());
        chr.getCheatTracker().checkDrop(true);
    }

    public static final void ChangeAndroidEmotion(final int emote, final MapleCharacter chr) {
        if (emote > 0 && chr != null && chr.getMap() != null && !chr.isHidden() && emote <= 17 && chr.getAndroid() != null) { //O_o
            chr.getMap().broadcastMessage(MaplePacketCreator.showAndroidEmotion(chr.getId(), emote));
        }
    }

    public static final void MoveAndroid(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        slea.skip(8);
        final List<LifeMovementFragment> res = MovementParse.parseMovement(slea, 3);

        if (res != null && chr != null && res.size() != 0 && chr.getMap() != null && chr.getAndroid() != null) { // map crash hack
            final Point pos = new Point(chr.getAndroid().getPos());
            chr.getAndroid().updatePosition(res);
            chr.getMap().broadcastMessage(chr, MaplePacketCreator.moveAndroid(chr.getId(), pos, res), false);
        }
    }

    public static final void ChangeEmotion(final int emote, final MapleCharacter chr) {
        if (emote > 7) {
            final int emoteid = 5159992 + emote;
            final MapleInventoryType type = GameConstants.getInventoryType(emoteid);
            if (chr.getInventory(type).findById(emoteid) == null) {
                chr.getCheatTracker().registerOffense(CheatingOffense.USING_UNAVAILABLE_ITEM, Integer.toString(emoteid));
                return;
            }
        }
        if (emote > 0 && chr != null && chr.getMap() != null && !chr.isHidden()) { //O_o
            chr.getMap().broadcastMessage(chr, MaplePacketCreator.facialExpression(chr, emote), false);
        }
    }

    public static final void Heal(final LittleEndianAccessor slea, final MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        //slea.skip(4);1229
        chr.updateTick(slea.readInt());
        if (slea.available() >= 8) {
            slea.skip(slea.available() >= 12 && GameConstants.GMS ? 8 : 4);
        }
        int healHP = slea.readShort();
        int healMP = slea.readShort();

        final PlayerStats stats = chr.getStat();

        if (stats.getHp() <= 0) {
            return;
        }
        final long now = System.currentTimeMillis();
        if (healHP != 0 && chr.canHP(now + 1000)) {
            if (healHP > stats.getHealHP()) {
                //chr.getCheatTracker().registerOffense(CheatingOffense.REGEN_HIGH_HP, String.valueOf(healHP));
                healHP = (int) stats.getHealHP();
            }
            chr.addHP(healHP);
        }
        if (healMP != 0 && !GameConstants.isDemon(chr.getJob()) && chr.canMP(now + 1000)) { //just for lag
            if (healMP > stats.getHealMP()) {
                //chr.getCheatTracker().registerOffense(CheatingOffense.REGEN_HIGH_MP, String.valueOf(healMP));
                healMP = (int) stats.getHealMP();
            }
            chr.addMP(healMP);
        }
    }

    public static final void MovePlayer(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        
        final Point Original_Pos = chr.getPosition(); // 4 bytes Added on v.80 MSEA
        byte fieldKey = slea.readByte(); //1
        int fieldVal = slea.readInt(); //4
        Point pt_01 = slea.readPos(), pt_02 = slea.readPos(); //8

        // log.trace("Movement command received: unk1 {} unk2 {}", new Object[] { unk1, unk2 });
        List<LifeMovementFragment> res;
        try {
            res = MovementParse.parseMovement(slea, 1);
        } catch (ArrayIndexOutOfBoundsException e) {
            //System.out.println("AIOBE Type1:\n" + slea.toString(true));
            return;
        }

        if (res != null && c.getPlayer().getMap() != null) { // TODO more validation of input data
            if (slea.available() < 11 || slea.available() > 26) {
                //System.out.println("slea.available != 11-26 (movement parsing error)\n" + slea.toString(true));
                return;
            }
            final MapleMap map = c.getPlayer().getMap();

            if (chr.isHidden()) {
                chr.setLastRes(res);
                c.getPlayer().getMap().broadcastGMMessage(chr, MaplePacketCreator.movePlayer(chr.getId(), res, Original_Pos), false);
            } else {
                c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.movePlayer(chr.getId(), res, Original_Pos), false);
            }

            MovementParse.updatePosition(res, chr, 0);
            final Point pos = chr.getTruePosition();
            map.movePlayer(chr, pos);
            if (chr.getFollowId() > 0 && chr.isFollowOn() && chr.isFollowInitiator()) {
                final MapleCharacter fol = map.getCharacterById(chr.getFollowId());
                if (fol != null) {
                    final Point original_pos = fol.getPosition();
                    fol.getClient().getSession().write(MaplePacketCreator.moveFollow(Original_Pos, original_pos, pos, res));
                    MovementParse.updatePosition(res, fol, 0);
                    map.movePlayer(fol, pos);
                    map.broadcastMessage(fol, MaplePacketCreator.movePlayer(fol.getId(), res, original_pos), false);
                } else {
                    chr.checkFollow();
                }
            }
            int count = c.getPlayer().getFallCounter();
            final boolean samepos = pos.y > c.getPlayer().getOldPosition().y && Math.abs(pos.x - c.getPlayer().getOldPosition().x) < 5;
            if (samepos && (pos.y > (map.getBottom() + 250) || map.getFootholds().findBelow(pos) == null)) {
                if (count > 5) {
                    c.getPlayer().changeMap(map, map.getPortal(0));
                    c.getPlayer().setFallCounter(0);
                } else {
                    c.getPlayer().setFallCounter(++count);
                }
            } else if (count > 0) {
                c.getPlayer().setFallCounter(0);
            }
            c.getPlayer().setOldPosition(pos);
            
            //근데 이건 기존에 있던건데 뭐하는거지
            if (!samepos && c.getPlayer().getBuffSource(MapleBuffStat.DARK_AURA) == 32120000) { //dark aura
                c.getPlayer().getStatForBuff(MapleBuffStat.DARK_AURA).applyMonsterBuff(c.getPlayer());
            } else if (!samepos && c.getPlayer().getBuffSource(MapleBuffStat.YELLOW_AURA) == 32120001) { //yellow aura
                c.getPlayer().getStatForBuff(MapleBuffStat.YELLOW_AURA).applyMonsterBuff(c.getPlayer());
            }
            
            if (chr.getParty() != null) {
                if (chr.getParty().getMembers().size() > 1) {
                    for (MaplePartyCharacter pchr : chr.getParty().getMembers()) {
                        MapleCharacter partyUser = chr.getMap().getCharacterById(pchr.getId());
                        if (partyUser != null) { //같은 파티이고, 같은 맵일 때만
                            MapleBuffStat[] auras = {MapleBuffStat.DARK_AURA, MapleBuffStat.BLUE_AURA, MapleBuffStat.YELLOW_AURA};
                            for (MapleBuffStat auraStat : auras) {
                                MapleStatEffect aura = partyUser.getStatForBuff(auraStat);
                                int buffLeaderID = partyUser.getBuffLeaderID(auraStat);
                                MapleCharacter buffLeader = partyUser.getMap().getCharacterById(buffLeaderID);
                                if (aura != null && buffLeaderID != -1 && buffLeader != null) { //같은 파티원 중 오라 버프 받고 있는 사람 있을 때
                                    boolean found = false;
                                    Rectangle bounds = aura.calculateBoundingBox(buffLeader.getTruePosition(), buffLeader.isFacingLeft());
                                    List<MapleMapObject> affecteds = buffLeader.getMap().getMapObjectsInRect(bounds, Arrays.asList(MapleMapObjectType.PLAYER));
                                    for (final MapleMapObject affectedmo : affecteds) {
                                        MapleCharacter affected = (MapleCharacter) affectedmo;
                                        if (affected != null) {
                                            if (affected.getId() == chr.getId()) { //범위 안에 내 캐릭턱가 있을 때
                                                found = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (chr.getId() != buffLeader.getId()) { //내 캐릭터가 버프 준 캐릭터가 아닐 때만
                                        if (!found) { //범위 내에 없으면
                                            if (chr.getStatForBuff(auraStat) != null) { //버프 적용 중일 때만 버프 캔슬
                                                String auraName = "다크 오라";
                                                if (auraStat == MapleBuffStat.BLUE_AURA)
                                                    auraName = "블루 오라";
                                                else if (auraStat == MapleBuffStat.YELLOW_AURA)
                                                    auraName = "옐로우 오라";
                                                int buffLeaderID2 = chr.getBuffLeaderID(auraStat);
                                                if (buffLeaderID2 != -1) {
                                                    if (chr.getId() != buffLeaderID2) {
                                                        chr.cancelBuffStats(true, auraStat);
                                                    }
                                                } else {
                                                    chr.cancelBuffStats(true, auraStat);
                                                }
                                                //chr.dropMessage(5, "<" + aura.getBuffLeader().getName() + "> 님과의 일정 거리가 멀어져 <" + auraName + "> 가 해제되었습니다.");
                                            }
                                        } else { //범위 내에 있으면
                                            if (chr.getStatForBuff(auraStat) == null) { //버프 적용 중일 때만
                                                aura.applyTo(buffLeader, chr, false, buffLeader.getTruePosition(), 2100000000);
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            MapleBuffStat[] auras = {MapleBuffStat.DARK_AURA, MapleBuffStat.BLUE_AURA, MapleBuffStat.YELLOW_AURA};
                            for (MapleBuffStat auraStat : auras) {
                                //내 캐릭터가 오라 버프 적용 중이고, 오라 버프 준 캐릭터가 존재할 때
                                int buffLeaderID = chr.getBuffLeaderID(auraStat);
                                if (chr.getStatForBuff(auraStat) != null && buffLeaderID != -1) {
                                    if (chr.getId() != buffLeaderID && pchr.getId() == buffLeaderID) //내 캐릭터가 버프 준 캐릭터가 아닐 때만
                                        chr.cancelBuffStats(true, auraStat); //버프 캔슬
                                }
                            }
                        }
                    }
                } else {
                    MapleBuffStat[] auras = {MapleBuffStat.DARK_AURA, MapleBuffStat.BLUE_AURA, MapleBuffStat.YELLOW_AURA};
                    for (MapleBuffStat auraStat : auras) {
                        //내 캐릭터가 오라 버프 적용 중이고, 오라 버프 준 캐릭터가 존재할 때
                        int buffLeaderID = chr.getBuffLeaderID(auraStat);
                        if (chr.getStatForBuff(auraStat) != null && buffLeaderID != -1) {
                            if (chr.getId() != buffLeaderID) //내 캐릭터가 버프 준 캐릭터가 아닐 때만
                                chr.cancelBuffStats(true, auraStat); //버프 캔슬
                        }
                    }
                }
            } else {
                MapleBuffStat[] auras = {MapleBuffStat.DARK_AURA, MapleBuffStat.BLUE_AURA, MapleBuffStat.YELLOW_AURA};
                for (MapleBuffStat auraStat : auras) {
                    //내 캐릭터가 오라 버프 적용 중이고, 오라 버프 준 캐릭터가 존재할 때
                    int buffLeaderID = chr.getBuffLeaderID(auraStat);
                    if (chr.getStatForBuff(auraStat) != null && buffLeaderID != -1) {
                        if (chr.getId() != buffLeaderID) //내 캐릭터가 버프 준 캐릭터가 아닐 때만
                            chr.cancelBuffStats(true, auraStat); //버프 캔슬
                    }
                }
            }
        }
    }

    public static final void ChangeMapSpecial(final String portal_name, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return;
        }
        final MaplePortal portal = chr.getMap().getPortal(portal_name);
//	slea.skip(2);

        if (portal != null && !chr.hasBlockedInventory()) {
            if (chr.isGM()) {
                chr.dropMessage(5, "포탈 : " + portal.getName() + " / 스크립트 : " + portal.getScriptName());
            }
            portal.enterPortal(c);
        } else {
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    public static final void ChangeMap(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return;
        }
        /*if (chr.getStat().getHp() <= 0 && chr.getPrimium() != 0) {
         chr.getStat().setHp(50, chr);
         chr.changeMap(chr.getMap(), chr.getMap().getPortal(0));
         return;
         }*/
        //나중에 한번에 코딩

        if (slea.available() != 0) {
            short seq = slea.readShort();
            Point ptSeq = slea.readPos();
            byte fadeInOut_idk = slea.readByte(); // 1 = from dying 2 = regular portals
            int targetid = slea.readInt(); //targetFieldID
            String sPortal = slea.readMapleAsciiString();
            MaplePortal portal = chr.getMap().getPortal(sPortal); //sPortal
            
            Point ptPortal = null;
            if (!sPortal.equals("")) {
                ptPortal = slea.readPos();
            }
            
            
            final MapleStatEffect statss = chr.getStatForBuff(MapleBuffStat.SOUL_STONE);
            byte onlyZeroInTransferField = slea.readByte();
            boolean revieve = slea.readByte() != 0;
//            short yes = slea.readShort();

            if (statss != null && chr.getStat().getHp() <= 0) {
                chr.setStance(0);
                if (revieve) {
//                    chr.dropMessage(6, "pc or 바퀴? or 수레?" + revieve);
                    chr.dropMessage(5, "소울스톤의 힘으로 부활합니다.");
                    chr.getStat().setHp(((chr.getStat().getMaxHp() * statss.getX()) / 100), chr);
                    chr.changeMap(chr.getMap(), chr.getMap().getPortal(0));
                    return;
                }
                chr.cancelEffectFromBuffStat(MapleBuffStat.SOUL_STONE);
            }

            final boolean wheel = revieve && !GameConstants.isEventMap(chr.getMapId()) && chr.itemQuantity(5510000) > 0 && chr.getMapId() / 1000000 != 925;
            
            boolean forced = false; //강제로 부활 맵 설정
            if (wheel && chr.getMapId() == 271040100) {
                forced = true;
                targetid = chr.getMapId();
            }
            
            if (forced || (targetid != -1 && !chr.isAlive())) {
                chr.setStance(0);
                if (chr.getEventInstance() != null && chr.getEventInstance().revivePlayer(chr) && chr.isAlive()) {
                    return;
                }
                /*if (chr.getPyramidSubway() != null) {
                 chr.getStat().setHp((short) 50, chr);
                 chr.getPyramidSubway().fail(chr);
                 return;
                 }*/
                if (!wheel) {
                    final MapleMap to = chr.getMap().getReturnMap();
                    if (to.getId() / 10000000 == 98) {
                        //carnival
                        chr.getStat().setHp((short) chr.getStat().getMaxHp() / 2, chr);
                        chr.getStat().setMp((short) chr.getStat().getMaxMp() / 2, chr);
                        chr.updateSingleStat(MapleStat.MP, chr.getStat().getMp());
                    } else {
                        chr.getStat().setHp((short) 50, chr);
                    }
                    
                    if (ServerConstants.worldbossmap == chr.getMapId()) {
                        chr.sethottimeboss(false);
                    }
                    chr.changeMap(to, to.getPortal(0));
                    
                    if (chr.haveItem(9999999, 1)) {
                        chr.revieveGainBuff();
                        chr.gainItem(2049704, (short) -1);
                    }
                } else {
//                    c.getSession().write(CSPacket.useWheel((byte) (chr.getInventory(MapleInventoryType.CASH).countById(5510000) - 1)));
                    chr.getStat().setHp(((chr.getStat().getMaxHp() / 100) * 40), chr);
                    MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, 5510000, 1, true, false);
                    c.sendPacket(CSPacket.useWheel((byte) chr.itemQuantity(5510000)));
                    // chr.dropMessage(6, "운명의 수레바퀴를 1개 소비하여 현재 맵에서 부활하였습니다. (" + chr.getInventory(MapleInventoryType.CASH).countById(5510000) + "개 남음)");
                    
                    //애초에 여기서 -1로 들어오는데. 왜 0으로 인식된거지 ?
                    //운수로 부활 시 데이터 삭제 굳이 여기서 안해도 되는데 추가해놓았었는데
                    //삭제 해야할 듯
                    /*MapleQuestStatus data = chr.getQuestNAdd(MapleQuest.getInstance(20230424));
                    MapleQuestStatus emData = chr.getQuestNAdd(MapleQuest.getInstance(202304240));
                    boolean deathCountForce = chr.getDeathCount() == 0 && emData.getCustomData() == null;
                    if (deathCountForce) {
                        data.setCustomData("-1");
                        emData.setCustomData(null);
                    }*/
                    final MapleMap to = chr.getMap();
                    chr.changeMap(to, to.getPortal(0));
//                    if (chr.haveItem(9999999, 1)) {
//                        chr.revieveGainBuff();
//                        chr.gainItem(2049704, (short) -1);
//                    }
                }
            } else if (targetid != -1) {
                final int divi = chr.getMapId() / 100;
                boolean unlock = false, warp = false;
                if (divi == 9130401) { // Only allow warp if player is already in Intro map, or else = hack
                    warp = targetid / 100 == 9130400 || targetid / 100 == 9130401; // Cygnus introduction
                    if (targetid / 10000 != 91304) {
                        warp = true;
                        unlock = true;
                        targetid = 130030000;
                    }
                } else if (divi == 9130400) { // Only allow warp if player is already in Intro map, or else = hack
                    warp = targetid / 100 == 9130400 || targetid / 100 == 9130401; // Cygnus introduction
                    if (targetid / 10000 != 91304) {
                        warp = true;
                        unlock = true;
                        targetid = 130030000;
                    }
                } else if (divi == 9140900) { // Aran Introductio
                    warp = targetid == 914090011 || targetid == 914090012 || targetid == 914090013 || targetid == 140090000;
                } else if (divi == 9120601 || divi == 9140602 || divi == 9140603 || divi == 9140604 || divi == 9140605) {
                    warp = targetid == 912060100 || targetid == 912060200 || targetid == 912060300 || targetid == 912060400 || targetid == 912060500 || targetid == 3000100;
                    unlock = true;
                } else if (divi == 9101500) {
                    warp = targetid == 910150006 || targetid == 101050010;
                    unlock = true;
                } else if (divi == 9140901 && targetid == 140000000) {
                    unlock = true;
                    warp = true;
                } else if (divi == 9240200 && targetid == 924020000) {
                    unlock = true;
                    warp = true;
                } else if (targetid == 980040000 && divi >= 9800410 && divi <= 9800450) {
                    warp = true;
                } else if (divi == 9140902 && (targetid == 140030000 || targetid == 140000000)) { //thing is. dont really know which one!
                    unlock = true;
                    warp = true;
                } else if (divi == 9000900 && targetid / 100 == 9000900 && targetid > chr.getMapId()) {
                    warp = true;
                } else if (divi / 1000 == 9000 && targetid / 100000 == 9000) {
                    unlock = targetid < 900090000 || targetid > 900090004; //1 movie
                    warp = true;
                } else if (divi / 10 == 1020 && targetid == 1020000) { // Adventurer movie clip Intro
                    unlock = true;
                    warp = true;
                } else if (chr.getMapId() == 900090101 && targetid == 100030100) {
                    unlock = true;
                    warp = true;
                } else if (chr.getMapId() == 2010000 && targetid == 104000000) {
                    unlock = true;
                    warp = true;
                } else if (chr.getMapId() == 106020001 || chr.getMapId() == 106020502) {
                    if (targetid == (chr.getMapId() - 1)) {
                        unlock = true;
                        warp = true;
                    }
                } else if (chr.getMapId() == 0 && targetid == 10000) {
                    unlock = true;
                    warp = true;
                } else if (chr.getMapId() == 931000011 && targetid == 931000012) {
                    unlock = true;
                    warp = true;
                } else if (chr.getMapId() == 931000021 && targetid == 931000030) {
                    unlock = true;
                    warp = true;
                }
                if (unlock) {
                    c.getSession().write(UIPacket.IntroDisableUI(false));
                    c.getSession().write(UIPacket.IntroLock(false));
                    c.getSession().write(MaplePacketCreator.enableActions());
                }
                if (warp) {
                    final MapleMap to = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(targetid);
                    chr.changeMap(to, to.getPortal(0));
                }
            } else {
                //int returnMap = c.getPlayer().getSavedLocation(SavedLocationType.MULUNG_TC);
                if (chr.getMapId() == 221023200 && portal.getId() == 1) {
                    final MapleMap mapp = chr.getClient().getChannelServer().getMapFactory().getMap(221023300);//101층
                    c.getPlayer().saveLocation(SavedLocationType.MULUNG_TC);
                    c.getPlayer().changeMap(mapp, mapp.getPortal(2));
                } else if (chr.getMapId() == 300030000 && portal.getId() == 2) {
                    final MapleMap mapp = chr.getClient().getChannelServer().getMapFactory().getMap(300030100);//엘린숲
                    c.getPlayer().saveLocation(SavedLocationType.MULUNG_TC);
                    c.getPlayer().changeMap(mapp, mapp.getPortal(1));
                } else if (chr.getMapId() == 200080100 && portal.getId() == 3) { //오르비스탑 입구
                    final MapleMap mapp = chr.getClient().getChannelServer().getMapFactory().getMap(200080101);
                    c.getPlayer().saveLocation(SavedLocationType.MULUNG_TC);
                    c.getPlayer().changeMap(mapp, mapp.getPortal(1));
                } else if (chr.getMapId() == 211000001 && portal.getId() == 3) { //장로의 관저
                    final MapleMap mapp = chr.getClient().getChannelServer().getMapFactory().getMap(211000002);
                    c.getPlayer().saveLocation(SavedLocationType.MULUNG_TC);
                    c.getPlayer().changeMap(mapp, mapp.getPortal(1));
                } else if (chr.getMapId() == 251010401 && portal.getId() == 5) { ////데비존
                    final MapleMap mapp = chr.getClient().getChannelServer().getMapFactory().getMap(251010404);
                    c.getPlayer().saveLocation(SavedLocationType.MULUNG_TC);
                    c.getPlayer().changeMap(mapp, mapp.getPortal(0));
                } else if (chr.getMapId() == 240030102 && portal.getId() == 5) { ////드래곤라이더
                    final MapleMap mapp = chr.getClient().getChannelServer().getMapFactory().getMap(240080000);
                    c.getPlayer().saveLocation(SavedLocationType.MULUNG_TC);
                    c.getPlayer().changeMap(mapp, mapp.getPortal(0));
                } else if (chr.getMapId() == 261000010 && portal.getId() == 0) { ////로미오 미완성
                    final MapleMap mapp = chr.getClient().getChannelServer().getMapFactory().getMap(261000011);
                    c.getPlayer().saveLocation(SavedLocationType.MULUNG_TC);
                    c.getPlayer().changeMap(mapp, mapp.getPortal(0));
                } else if (chr.getMapId() == 261000020 && portal.getId() == 0) { ////줄리엣 미완성
                    final MapleMap mapp = chr.getClient().getChannelServer().getMapFactory().getMap(261000021);
                    c.getPlayer().saveLocation(SavedLocationType.MULUNG_TC);
                    c.getPlayer().changeMap(mapp, mapp.getPortal(0));
                } else if (portal != null && !chr.hasBlockedInventory()) {
                    if (chr.isGM()) {
                        chr.dropMessage(5, "포탈 : " + portal.getName() + " / 스크립트 : " + portal.getScriptName());
                    }
                    portal.enterPortal(c);
                } else if (chr.isGM()) {
                    MapleMap map = c.getChannelServer().getMapFactory().getMap(targetid);
                    if (map != null) {
                        chr.changeMap(map);
                    }
                } else {
                    c.getSession().write(MaplePacketCreator.enableActions());
                }
            }
        }
    }

    public static final void InnerPortal(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return;
        }
        final MaplePortal portal = chr.getMap().getPortal(slea.readMapleAsciiString());
        final int toX = slea.readShort();
        final int toY = slea.readShort();
//	slea.readShort(); // Original X pos
//	slea.readShort(); // Original Y pos
        if (chr.isGM()) {
            chr.dropMessage(5, "포탈 : " + portal.getName() + " / 스크립트 : " + portal.getScriptName());
        }
        if (portal == null) {
            return;
        } else if (portal.getPosition().distanceSq(chr.getTruePosition()) > 22500 && !chr.isGM()) {
            chr.getCheatTracker().registerOffense(CheatingOffense.USING_FARAWAY_PORTAL);
            return;
        }
        chr.getMap().movePlayer(chr, new Point(toX, toY));
    }

    public static final void snowBall(LittleEndianAccessor slea, MapleClient c) {
        //B2 00
        //01 [team]
        //00 00 [unknown]
        //89 [position]
        //01 [stage]
        c.getSession().write(MaplePacketCreator.enableActions());
        //empty, we do this in closerange
    }

    public static final void leftKnockBack(LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer().getMapId() / 10000 == 10906) { //must be in snowball map or else its like infinite FJ
            c.getSession().write(MaplePacketCreator.leftKnockBack());
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    public static final void ReIssueMedal(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        final MapleQuest q = MapleQuest.getInstance(slea.readShort());
        if (q != null && q.getMedalItem() > 0 && chr.getQuestStatus(q.getId()) == 2 && !chr.haveItem(q.getMedalItem(), 1, true, true) && q.getMedalItem() == slea.readInt() && MapleInventoryManipulator.checkSpace(c, q.getMedalItem(), (short) 1, "")) {
            MapleInventoryManipulator.addById(c, q.getMedalItem(), (short) 1, "Redeemed item through medal quest " + q.getId() + " on " + FileoutputUtil.CurrentReadable_Date());
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }
}
