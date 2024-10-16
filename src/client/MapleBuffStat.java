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
package client;

import constants.GameConstants;
import handling.Buffstat;
import java.io.Serializable;

public enum MapleBuffStat implements Serializable, Buffstat {

    WATK(0x1, 1), //PAD, 공격력
    WDEF(0x2, 1), //PDD, 물방
    MATK(0x4, 1), //MAD, 마력
    MDEF(0x8, 1), //MDD, 마방
    ACC(0x10, 1), //ACC, 명중
    AVOID(0x20, 1), //EVA, 회피
    HANDS(0x40, 1), //Craft, 손재주
    SPEED(0x80, 1), //Speed, 이동속도
    JUMP(0x100, 1), //Jump, 점프력
    MAGIC_GUARD(0x200, 1), //MagicGuard, 매직가드
    DARKSIGHT(0x400, 1), //DarkSight, 다크사이트
    BOOSTER(0x800, 1), //Booster, 웨폰류 부스터
    POWERGUARD(0x1000, 1), //PowerGuard, 파워가드
    MAXHP(0x2000, 1), //MaxHP, 최대체력
    MAXMP(0x4000, 1), //MaxMP, 최대마나
    INVINCIBLE(0x8000, 1), //Invincible, 아마 법사 인빈시블 버프
    SOULARROW(0x10000, 1), //SoulArrow, 소울에로우
    //Stun(0x20000, 1),
    //Poison(0x40000, 1),
    //Seal(0x80000, 1),
    //Darkness(0x100000, 1),
    COMBO(0x200000, 1), //ComboCounter, 콤보어택
    SUMMON(0x200000, 1), //원래는 없는 것, ComboCounter 버프 스탯
    WK_CHARGE(0x400000, 1), //WeaponCharge, 차지 블로우 종류
    DRAGONBLOOD(0x800000, 1), //DragonBlood, 드래곤 블러드
    HOLY_SYMBOL(0x1000000, 1), //HolySymbol, 홀리 심볼
    HOLY_SYMBOL2(0x1000000, 1), //원래는 없는 것, HolySymbol 버프 스탯
    MESOUP(0x2000000, 1), //MesoUp, 메소 업
    SHADOWPARTNER(0x4000000, 1), //ShadowPartner, 쉐도우 파트너
    PICKPOCKET(0x8000000, 1), //PickPocket, 픽 파켓
    PUPPET(0x8000000, 1), //원래는 없는 것, PickPocket 버프 스탯
    MESOGUARD(0x10000000, 1), //MesoGuard, 메소 가드
    HP_LOSS_GUARD(0x20000000, 1), //Thaw, 쉽게 말하면 호빵 추위 면역
    //Weakness(0x40000000, 1),
    //Curse(0x80000000, 1),
    //Slow(0x1, 2),
    MORPH(0x2, 2), //Morph, 변신
    RECOVERY(0x4, 2), //Regen, 회복
    MAPLE_WARRIOR(0x8, 2), //BasicStatUp, 메이플 용사
    STANCE(0x10, 2), //Stance, 스탠스
    SHARP_EYES(0x20, 2), //SharpEyes, 샤프 아이
    MANA_REFLECTION(0x40, 2), //ManaReflection, 마나 리플렉션
    //Attract(0x80, 2), //아마 유혹
    SPIRIT_CLAW(0x100, 2), //SpiritJavelin, 스피릿 자벨린
    INFINITY(0x200, 2), //Infinity, 인피니티
    HOLY_SHIELD(0x400, 2), //HolyShield, 홀리 실드
    HAMSTRING(0x800, 2), //HamString, 햄 스트링
    BLIND(0x1000, 2), //Blind, 블라인드
    CONCENTRATE(0x2000, 2), //Concentration, 집중
    //BanMap(0x4000, 2),
    ECHO_OF_HERO(0x8000, 2), //MaxLevelBuff, 영웅의 메아리
    donno(0x10000, 2), //MesoUpByItem, 메소 업 (아이템 버프)
    donno2(0x20000, 2), //Ghost, 유령 변신
    GHOST_MORPH(0x20000, 2), //Ghost, 유령 변신 위와 같음
    ARIANT_COSS_IMU(0x40000, 2), //Barrier, 아리안트 투기대회 방어막
    donno3(0x80000, 2), //ReverseInput
    donno4(0x100000, 2), //ItemUpByItem, 드롭 업 (아이템 버프)
    //RespectPImmune(0x200000, 2), //공무 때릴 때 일정 확률로 데미지 들어가게 하는 버프
    //RespectMImmune(0x400000, 2), //마무 때릴 때 일정 확률로 데미지 들어가게 하는 버프
    ACASH_RATE(0x800000, 2), //DefenseAtt, 이게 아마 몬스터 카드 버프 중 속성 뎀 관련 버프
    //DefenseState(0x1000000, 2), //이게 아마 몬스터 카드 버프 중 속성 뎀 관련 버프
    ILLUSION(0x1000000, 2), //원래는 없는 것, DefenseState 버프 스탯
    //1 = unknown, and for gms, 2 and 4 are unknown
    BERSERK_FURY(0x2000000, 2), //DojangBerserk, 무릉도장 버서크
    DIVINE_BODY(0x4000000, 2), //DojangInvincible, 무릉도장 무적
    SPARK(0x8000000, 2), //Spark, 스파크
    ARIANT_COSS_IMU2(0x10000000, 2), //DojangShield, 무릉도장 실드
    FINALATTACK(0x20000000, 2), //SoulMasterFinal, 소울마스터 파이널어택
    //WindBreakerFinal(0x40000000, 2), //윈드브레이커 파이널어택
    ELEMENT_RESET(0x80000000, 2), //ElementalReset, 엘리멘탈 리셋
    WIND_WALK(0x1, 3), //WindWalk, 윈드 워크
    MESO_DROP_RATE(0x2, 3), //EventRate, 이벤트 배율 관련 버프 (아마 여러 이벤트 버프 중첩되게 하기 위한 것인 듯)
    ARAN_COMBO(0x4, 3), //ComboAblityBuff, 아란 콤보
    COMBO_DRAIN(0x8, 3), //ComboDrain, 콤보 드레인
    COMBO_BARRIER(0x10, 3), //ComboBarrier, 콤보 베리어 (아마 아킬레스 같은 효과였던 걸로 기억)
    BODY_PRESSURE(0x20, 3), //BodyPressure, 바디 프레셔
    SMART_KNOCKBACK(0x40, 3), //SmartKnockback, 뭔진 정확힌 모르지만 와헌이 추가 되면서 추가 된 버프
    PYRAMID_PQ(0x80, 3), //RepeatEffect, 네트의 피라미드 버프
    //ExpBuffRate(0x100, 3), //경험치 배율
    //StopPortion(0x200, 3), //물약 못 먹게 하는거
    //StopMotion(0x400, 3), //아마 못 움직이게 하는거
    //Fear(0x800, 3), //공포
    SLOW(0x1000, 3), //EvanSlow, 에반 슬로우
    MAGIC_SHIELD(0x2000, 3), //MagicShield, 매직 실드
    MAGIC_RESISTANCE(0x4000, 3), //MagicResistance, 뭐하는건지 정확히 모름
    SOUL_STONE(0x8000, 3), //SoulStone, 소울 스톤
    SOARING(0x10000, 3), //Flying, 플라잉
    //Frozen(0x20000, 3), //캐릭터 얼어붙는 것
    LIGHTNING_CHARGE(0x40000, 3), //AssistCharge, 썬더차지 다른 차지랑 중첩 가능하게 하는거
    ENRAGE(0x80000, 3), //Enrage, 이 값이 아마 95에선 미러이미징이었던 것으로 기억
    OWL_SPIRIT(0x100000, 3), //SuddenDeath, 서든 데스 효과
    GODMODE(0x200000, 3), //NotDamaged, 데미지 안받는 거
    FINAL_CUT(0x400000, 3), //FinalCut, 파이널 컷
    DAMAGE_BUFF(0x800000, 3), //원래는 없는 것, ThornsEffect 버프 스탯
    ThornsEffect(0x800000, 3), //쏜즈 이펙트
    ATTACK_BUFF(0x1000000, 3), //SwallowAttackDamage 버프 스탯
    SwallowAttackDamage(0x1000000, 3), //서버에서 실제 사용 X
    MorewildDamageUp(0x2000000, 3), //서버에서 실제 사용 X
    RAINING_MINES(0x4000000, 3), //마인 확실
    EMHP(0x8000000, 3), //IndieMHP
    EMMP(0x10000000, 3), //IndieMMP
    EPAD(0x20000000, 3), //IndiePAD
    EPDD(0x40000000, 3), //IndiePDD
    EMDD(0x80000000, 3), //IndieMDD
    PERFECT_ARMOR(0x1, 4), //Guard, 어디 쓰는지 모름
    SATELLITESAFE_PROC(0x2, 4), //SafetyDamage, 어디 쓰는지 모름
    SATELLITESAFE_ABSORB(0x4, 4), //SafetyAbsorb, 어디 쓰는지 모름
    TORNADO(0x8, 4), //Cyclone, 싸이클론
    CRITICAL_RATE_BUFF(0x10, 4), //SwallowCritical
    MP_BUFF(0x20, 4), //SwallowMaxMP
    DAMAGE_TAKEN_BUFF(0x40, 4), //SwallowDefense
    DODGE_CHANGE_BUFF(0x80, 4), //SwallowEvasion
    CONVERSION(0x100, 4), //Conversion, 어디 쓰는지 모름
    REAPER(0x200, 4), //Revive, 어디 쓰는지 모름
    INFILTRATE(0x400, 4), //Sneak, 어디 쓰는지 모름
    MECH_CHANGE(0x800, 4), //Mechanic
    AURA(0x1000, 4), //Aura
    DARK_AURA(0x2000, 4), //DrakAura
    BLUE_AURA(0x4000, 4), //BlueAura
    YELLOW_AURA(0x8000, 4), //YellowAura
    BODY_BOOST(0x10000, 4), //SuperBody
    //이 아래부터는 주석 값이 확실한 값
    //MorewildMaxHP(0x20000, 4),
    //Dice(0x40000, 4), 
    //BlessingArmor(0x80000, 4),
    //DamR(0x100000, 4),
    //TeleportMasteryOn(0x200000, 4),
    //InfightingMastery(0x400000, 4),
    //CombatOrders(0x800000, 4),
    //Beholder(0x1000000, 4),
    
    //Start CTS TSIndex
    //EnergyCharged(0x2000000, 4),
    //DashSpeed(0x4000000, 4),
    //DashJump(0x8000000, 4),
    //RideVehicle(0x10000000, 4),
    //PartyBooster(0x20000000, 4),
    //GuidedBullet(0x40000000, 4),
    //Undead(0x80000000, 4),
    
    FELINE_BERSERK(0x2000000, 3), //(0x20000, 4), //d비스트폼 
    DICE_ROLL(0x40000, 4),
    DIVINE_SHIELD(0x100000, 4),
    PIRATES_REVENGE(0x100000, 4),
    TELEPORT_MASTERY(0x200000, 4),
    COMBAT_ORDERS(0x400000, 4),//확
    BEHOLDER(0x800000, 4),
    //BEHOLDER(GameConstants.GMS ? 0x2000000 : 0x800000, 4),
    ENERGY_CHARGE(0x1000000, 4),
    DASH_SPEED(0x2000000, 4),
    DASH_JUMP(0x4000000, 4),
    MONSTER_RIDING(0x8000000, 4),
    SPEED_INFUSION(0x10000000, 4),
    HOMING_BEACON(0x20000000, 4),
    DEFAULT_BUFFSTAT(0x40000000, 4), //end speshulness,     
    
    EXPRATE(0x100, 3),
    MESO_RATE(0x100000, 2), // MesoUpByItem 패밀리용 
    DROP_RATE(0x200000, 2), // ItemUpByItem 패밀리용
    //1 = debuff
    GIANT_POTION(GameConstants.GMS ? 0x8000000 : 0x2000000, 4),
    ONYX_SHROUD(GameConstants.GMS ? 0x10000000 : 0x4000000, 4),
    ONYX_WILL(GameConstants.GMS ? 0x20000000 : 0x8000000, 4),
    //1 = debuff
    BLESS(GameConstants.GMS ? 0x80000000 : 0x20000000, 4),
    //4 8 unknown

    THREATEN_PVP(GameConstants.GMS ? 0x4 : 0x1, 5),
    ICE_KNIGHT(GameConstants.GMS ? 0x8 : 0x2, 5),
    //4 unknown
    STR(GameConstants.GMS ? 0x20 : 0x8, 5),
    DEX(GameConstants.GMS ? 0x40 : 0x10, 5),
    INT(GameConstants.GMS ? 0x80 : 0x20, 5),
    LUK(GameConstants.GMS ? 0x100 : 0x40, 5),
    //8 unknown

    //1 2 unknown
    ANGEL_ATK(GameConstants.GMS ? 0x1000 : 0x400, 5, true),
    ANGEL_MATK(GameConstants.GMS ? 0x2000 : 0x800, 5, true),
    HP_BOOST(GameConstants.GMS ? 0x4000 : 0x1000, 5, true), //indie hp
    MP_BOOST(GameConstants.GMS ? 0x8000 : 0x2000, 5, true),
    ANGEL_ACC(GameConstants.GMS ? 0x10000 : 0x4000, 5, true),
    ANGEL_AVOID(GameConstants.GMS ? 0x20000 : 0x8000, 5, true),
    ANGEL_JUMP(GameConstants.GMS ? 0x40000 : 0x10000, 5, true),
    ANGEL_SPEED(GameConstants.GMS ? 0x80000 : 0x20000, 5, true),
    ANGEL_STAT(GameConstants.GMS ? 0x100000 : 0x40000, 5, true),
    PVP_DAMAGE(GameConstants.GMS ? 0x200000 : 0x4000, 5),
    PVP_ATTACK(GameConstants.GMS ? 0x400000 : 0x8000, 5), //skills
    INVINCIBILITY(GameConstants.GMS ? 0x800000 : 0x10000, 5),
    HIDDEN_POTENTIAL(GameConstants.GMS ? 0x1000000 : 0x20000, 5),
    ELEMENT_WEAKEN(GameConstants.GMS ? 0x2000000 : 0x40000, 5),
    SNATCH(GameConstants.GMS ? 0x4000000 : 0x80000, 5), //however skillid is 90002000, 1500 duration
    FROZEN(GameConstants.GMS ? 0x8000000 : 0x100000, 5),
    //4, unknown
    ICE_SKILL(GameConstants.GMS ? 0x20000000 : 0x400000, 5),
    //1, 2, 4 unknown
    //8 = debuff

    //1, 2 unknown
    HOLY_MAGIC_SHELL(0x4, 6), //max amount of attacks absorbed
    //8 unknown

    ARCANE_AIM(0x10, 6, true),
    BUFF_MASTERY(0x20, 6), //buff duration increase
    //4, 8 unknown

    WATER_SHIELD(GameConstants.GMS ? 0x400 : 0x100, 6),
    //2, 4, unknown
    SPIRIT_SURGE(GameConstants.GMS ? 0x2000 : 0x800, 6),
    SPIRIT_LINK(GameConstants.GMS ? 0x4000 : 0x1000, 6),
    //2 unknown
    VIRTUE_EFFECT(GameConstants.GMS ? 0x10000 : 0x4000, 6),
    //8, 1, 2 unknown

    NO_SLIP(GameConstants.GMS ? 0x100000 : 0x40000, 6),
    FAMILIAR_SHADOW(GameConstants.GMS ? 0x200000 : 0x80000, 6),
    SIDEKICK_PASSIVE(GameConstants.GMS ? 0x400000 : 0x100000, 6), //skillid 79797980

    //speshul0x1000000
    쏜즈이펙트(0x800000, 3),
    미러이미징(0x80000, 3);
    ;
    private static final long serialVersionUID = 0L;
    private final int buffstat;
    private final int first;
    private boolean stacked = false;

    private MapleBuffStat(int buffstat, int first) {
        this.buffstat = buffstat;
        this.first = first;
    }

    private MapleBuffStat(int buffstat, int first, boolean stacked) {
        this.buffstat = buffstat;
        this.first = first;
    }

    public final int getPosition() {
        return first;
    }

    public final int getValue() {
        return buffstat;
    }

    public final boolean canStack() {
        return stacked;
    }
}
