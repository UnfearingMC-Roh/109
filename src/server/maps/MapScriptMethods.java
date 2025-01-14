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

import client.MapleCharacter;
import java.awt.Point;

import client.MapleClient;
import client.MapleQuestStatus;
import client.SkillFactory;
import constants.GameConstants;
import scripting.AbstractPlayerInteraction;
import scripting.EventManager;
import scripting.NPCScriptManager;
import server.Randomizer;
import server.MapleItemInformationProvider;
import server.Timer.EventTimer;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.OverrideMonsterStats;
import server.quest.MapleQuest;
import server.quest.MapleQuest.MedalQuest;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.packet.UIPacket;
import server.Timer.MapTimer;
import server.maps.MapleNodes.DirectionInfo;

public class MapScriptMethods {

    private static final Point witchTowerPos = new Point(-60, 184);
    private static final String[] mulungEffects = {
        "무릉도장에 도전한 것을 후회하게 해주겠다! 어서 들어와봐!",
        "기다리고 있었다! 용기가 남았다면 들어와 보시지!",
        "배짱 하나는 두둑하군! 현명함과 무모함을 혼동하지말라고!",
        "무릉도장에 도전하다니 용기가 가상하군!",
        "패배의 길을 걷고싶다면 들어오라고!"};

    private static enum onFirstUserEnter {

        dojang_Eff,
        dojang_Msg,
        PinkBeen_before,
        onRewordMap,
        StageMsg_together,
        StageMsg_crack,
        StageMsg_davy,
        StageMsg_goddess,
        party6weatherMsg,
        StageMsg_juliet,
        StageMsg_romio,
        moonrabbit_mapEnter,
        astaroth_summon,
        boss_Ravana,
        boss_Ravana_mirror,
        killing_BonusSetting,
        killing_MapSetting,
        metro_firstSetting,
        balog_bonusSetting,
        balog_summon,
        easy_balog_summon,
        Sky_TrapFEnter,
        shammos_Fenter,
        PRaid_D_Fenter,
        PRaid_B_Fenter,
        Depart_Boss_F_Enter,
        summon_pepeking,
        Xerxes_summon,
        VanLeon_Before,
        cygnus_Summon,
        storymap_scenario,
        shammos_FStart,
        kenta_mapEnter,
        iceman_FEnter,
        Elin_forest,
        iceman_Boss,
        prisonBreak_mapEnter,
        Visitor_Cube_poison,
        Visitor_Cube_Hunting_Enter_First,
        VisitorCubePhase00_Start,
        visitorCube_addmobEnter,
        Visitor_Cube_PickAnswer_Enter_First_1,
        visitorCube_medicroom_Enter,
        visitorCube_iceyunna_Enter,
        Visitor_Cube_AreaCheck_Enter_First,
        visitorCube_boomboom_Enter,
        visitorCube_boomboom2_Enter,
        CubeBossbang_Enter,
        MalayBoss_Int,
        tristan_questMap,        
        mPark_summonBoss,
        mpark_mobRegen,
        NULL;

        private static onFirstUserEnter fromString(String Str) {
            try {
                return valueOf(Str);
            } catch (IllegalArgumentException ex) {
                return NULL;
            }
        }
    };

    private static enum onUserEnter {

        babyPigMap,
        crash_Dragon,
        evanleaveD,
        getDragonEgg,
        meetWithDragon,
        go1010100,
        go1010200,
        go1010300,
        go1010400,
        evanPromotion,
        PromiseDragon,
        evanTogether,
        incubation_dragon,
        TD_MC_Openning,
        TD_MC_gasi,
        TD_MC_title,
        cygnusJobTutorial,
        cygnusTest,
        startEreb,
        dojang_Msg,
        dojang_1st,
        reundodraco,
        undomorphdarco,
        explorationPoint,
        goAdventure,
        go10000,
        go20000,
        go30000,
        go40000,
        go50000,
        go1000000,
        go1010000,
        go1020000,
        go2000000,
        goArcher,
        goPirate,
        goRogue,
        goMagician,
        goSwordman,
        goLith,
        iceCave,
        mirrorCave,
        Depart_inSubway,        
        aranDirection,
        rienArrow,
        rien,
        check_count,
        Massacre_first,
        Massacre_result,
        aranTutorAlone,
        evanAlone,
        dojang_QcheckSet,
        Sky_StageEnter,
        outCase,
        balog_buff,
        balog_dateSet,
        Sky_BossEnter,
        Sky_GateMapEnter,
        shammos_Enter,
        shammos_Result,
        shammos_Base,
        dollCave00,
        dollCave01,
        dollCave02,
        Sky_Quest,
        enterBlackfrog,
        onSDI,
        blackSDI,
        summonIceWall,
        metro_firstSetting,
        start_itemTake,
        findvioleta,
        pepeking_effect,
        TD_MC_keycheck,
        TD_MC_gasi2,
        in_secretroom,
        sealGarden,
        TD_NC_title,
        TD_neo_BossEnter,
        PRaid_D_Enter,
        PRaid_B_Enter,
        PRaid_Revive,
        PRaid_W_Enter,
        PRaid_WinEnter,
        PRaid_FailEnter,
        Resi_tutor10,
        Resi_tutor20,
        Resi_tutor30,
        Resi_tutor40,
        Resi_tutor50,
        Resi_tutor60,
        Resi_tutor70,
        Resi_tutor80,
        Resi_tutor50_1,
        summonSchiller,
        q31102e,
        q31103s,
        jail,
        VanLeon_ExpeditionEnter,
        cygnus_ExpeditionEnter,
        knights_Summon,
        TCMobrevive,
        mPark_stageEff,
        moonrabbit_takeawayitem,
        StageMsg_crack,
        shammos_Start,
        iceman_Enter,
        prisonBreak_1stageEnter,
        VisitorleaveDirectionMode,
        visitorPT_Enter,
        VisitorCubePhase00_Enter,
        visitor_ReviveMap,
        cannon_tuto_01,
        cannon_tuto_direction,
        cannon_tuto_direction1,
        cannon_tuto_direction2,
        userInBattleSquare,
        merTutorDrecotion00,
        merTutorDrecotion10,
        merTutorDrecotion20,
        merStandAlone,
        merOutStandAlone,
        merTutorSleep00,
        merTutorSleep01,
        merTutorSleep02,
        EntereurelTW,
        ds_tuto_ill0,
        ds_tuto_0_0,
        ds_tuto_1_0,
        ds_tuto_3_0,
        ds_tuto_3_1,
        ds_tuto_4_0,
        ds_tuto_5_0,
        ds_tuto_2_prep,
        ds_tuto_1_before,
        ds_tuto_2_before,
        ds_tuto_home_before,
        ds_tuto_ani,
        dragon_rider,
        dangerInfo,
        Depart_topFloorEnter,
        Depart_BossEnter,        
        q3143_clear,
        sao_enter05,
        space_first,
        NULL;        

        private static onUserEnter fromString(String Str) {
            try {
                return valueOf(Str);
            } catch (IllegalArgumentException ex) {
                return NULL;
            }
        }
    };

    private static enum directionInfo {

        merTutorDrecotion01,
        merTutorDrecotion02,
        merTutorDrecotion03,
        merTutorDrecotion04,
        merTutorDrecotion05,
        merTutorDrecotion12,
        merTutorDrecotion21,
        ds_tuto_0_1,
        ds_tuto_0_2,
        ds_tuto_0_3,
        NULL;

        private static directionInfo fromString(String Str) {
            try {
                return valueOf(Str);
            } catch (IllegalArgumentException ex) {
                return NULL;
            }
        }
    };

    public static void startScript_FirstUser(MapleClient c, String scriptName) {
        if (c.getPlayer() == null) {
            return;
        } //o_O
        if (c.getPlayer().isGM()) {
            c.getPlayer().dropMessage(6, "startScript_FirstUser : " + onFirstUserEnter.fromString(scriptName));
        }
        switch (onFirstUserEnter.fromString(scriptName)) {
            case tristan_questMap: {
                break;
            }
            case dojang_Eff: {
                int temp = (c.getPlayer().getMapId() - 925000000) / 100;
                int stage = (int) (temp - ((temp / 100) * 100));
                c.getPlayer().getMap().setOutMapTime(System.currentTimeMillis() + getTiming(stage) * 60000L);
                sendDojoClock(c, getTiming(stage) * 60);
                sendDojoStart(c, stage - getDojoStageDec(stage));
                break;
            }
            case PinkBeen_before: {
                handlePinkBeanStart(c);
                break;
            }
            case onRewordMap: {
                reloadWitchTower(c);
                break;
            }
            //5120019 = orbis(start_itemTake - onUser)
            case moonrabbit_mapEnter: {
                c.getPlayer().getMap().startMapEffect("달맞이꽃 씨앗을 심고 보름달이 차오르면 월묘를 지켜내세요!", 5120016);
                break;
            }
            case StageMsg_goddess: {
                switch (c.getPlayer().getMapId()) {
                    case 920010000:
                        c.getPlayer().getMap().startMapEffect("구름 조각을 모아 저를 구해주세요!", 5120019);
                        break;
                    case 920010100:
                        c.getPlayer().getMap().startMapEffect("여신 미네르바님의 석상 조각을 모아주세요!", 5120019);
                        break;
                    case 920010200:
                        c.getPlayer().getMap().startMapEffect("몬스터를 없애고 석상 조각을 되찾아주세요!", 5120019);
                        break;
                    case 920010300:
                        c.getPlayer().getMap().startMapEffect("각 방에 있는 몬스터를 없애고 석상 조각을 되찾아 주세요!", 5120019);
                        break;
                    case 920010400:
                        c.getPlayer().getMap().startMapEffect("오늘 날짜에 맞는 LP 디스크를 구해주세요!", 5120019);
                        break;
                    case 920010500:
                        c.getPlayer().getMap().startMapEffect("올바른 발판을 찾으세요!", 5120019);
                        break;
                    case 920010600:
                        c.getPlayer().getMap().startMapEffect("몬스터를 없애고 석상 조각을 되찾아주세요!", 5120019);
                        break;
                    case 920010700:
                        c.getPlayer().getMap().startMapEffect("올바른 길을 찾아 꼭대기로 올라가세요!", 5120019);
                        break;
                    case 920010800:
                        c.getPlayer().getMap().startMapEffect("파파픽시를 소환하여 물리치세요!", 5120019);
                        break;
                }
                break;
            }            
            case StageMsg_crack: {
                switch (c.getPlayer().getMapId()) {
                    case 922010100:
                        c.getPlayer().getMap().startMapEffect("차원의 라츠와 차원의 블랙라츠를 모두 해치우고 차원의 통행증 25장을 모아라!", 5120018);
                        break;
                    case 922010200:
                        c.getPlayer().getMap().startSimpleMapEffect("상자를 부셔서 차원의 통행증 10장을 모아라!", 5120018);
                        c.getPlayer().getMap().setReactorDelay(-1);//리액터 못살아나게
                        break;
                    case 922010300:
                        c.getPlayer().getMap().startMapEffect("모든 몬스터를 없애고 통행증을 모으세요!", 5120018);
                        break;
                    case 922010400:
                        c.getPlayer().getMap().startMapEffect("다크아이와 쉐도우아이를 찾아서 퇴치하자!", 5120018);
                        break;
                    case 922010500:
                        c.getPlayer().getMap().startMapEffect("각 방의 통행증을 모으세요!", 5120018);
                        break;
                    case 922010600:
                        c.getPlayer().getMap().startMapEffect("숨겨진 상자의 암호를 풀고 꼭대기로 올라가라.", 5120018);
                        break;
                    case 922010700:
                        c.getPlayer().getMap().startMapEffect("이 곳에 있는 롬바드를 모두 물리치자!", 5120018);
                        break;
                    case 922010800:
                        final short answer = (short) Randomizer.rand(100, 999);
                        AbstractPlayerInteraction.setAnswer(answer);
                        c.getPlayer().getMap().startSimpleMapEffect("문제를 듣고 정답에 맞는 상자 위로 올라가라!", 5120018);
                        break;
                    case 922010900:
                        c.getPlayer().getMap().startMapEffect("몬스터를 퇴치하고 알리샤르를 소환하여 퇴치하라!", 5120018);
                        //MapleQuestStatus stat = c.getPlayer().getQuestNAdd(MapleQuest.getInstance(9662));//차원의 균열
                        //stat.setCustomData("1");
                        break;
                }
                break;
            }
            case StageMsg_together: {
                switch (c.getPlayer().getMapId()) {
                    case 910340100:
                        c.getPlayer().getMap().startMapEffect("클로토가 내는 미션만큼 쿠폰을 모아라! 쿠폰은 리게이터를 물리치면 받을 수 있다!", 5120017);
                        break;
                    case 910340200:
                        c.getPlayer().getMap().startMapEffect("다음 단계로 가는 문을 열 수 있는 줄 3개를 찾아서 매달려라!", 5120017);
                        break;
                    case 910340300:
                        c.getPlayer().getMap().startMapEffect("다음 단계로 가는 문을 열 수 있는 발판 3개를 찾아라!", 5120017);
                        break;
                    case 910340400:
                        c.getPlayer().getMap().startMapEffect("사악한 커즈아이를 모두 해치워라!!", 5120017);
                        //c.getPlayer().getMap().startMapEffect("통 위에 올라서서, 올바른 통을 찾으세요!", 5120017);
                        break;
                    case 910340500:
                        c.getPlayer().getMap().startMapEffect("킹슬라임을 해치우고 통행증 1장을 모아오세요!", 5120017);
                        //MapleQuestStatus stat = c.getPlayer().getQuestNAdd(MapleQuest.getInstance(9661));//첫번째 동행
                        //stat.setCustomData("1");
                        break;
                }
                break;
            }
            case StageMsg_romio: {
                switch (c.getPlayer().getMapId()) {
                    case 926100000:
                        c.getPlayer().getMap().startMapEffect("연구실로 가는 문을 여는 수상한 스위치를 찾아주세요.", 5120021);
                        break;
                    case 926100001:
                        c.getPlayer().getMap().startMapEffect("어둠 속의 몬스터를 모두 물리쳐 주세요.", 5120021);
                        break;
                    case 926100100:
                        c.getPlayer().getMap().startMapEffect("비커에 액체를 가득 채워주세요.", 5120021);
                        break;
                    case 926100200:
                        c.getPlayer().getMap().startMapEffect("각 문을 통해 연구 자료를 찾아와 주세요!", 5120021);
                        break;
                    case 926100203:
                        c.getPlayer().getMap().startMapEffect("모든 몬스터를 없애주세요!", 5120021);
                        break;
                    case 926100300:
                        c.getPlayer().getMap().startMapEffect("연구실 꼭대기로 가는 길을 찾으세요!", 5120021);
                        break;
                    case 926100401:
                        c.getPlayer().getMap().startMapEffect("제 사랑 줄리엣을 보호해주세요!", 5120021);

                        break;
                }
                break;
            }
            case StageMsg_juliet: {
                switch (c.getPlayer().getMapId()) {
                    case 926110000:
                        c.getPlayer().getMap().startMapEffect("연구실로 가는 문을 여는 수상한 스위치를 찾아주세요.", 5120022);
                        break;
                    case 926110001:
                        c.getPlayer().getMap().startMapEffect("어둠 속의 몬스터를 모두 물리쳐 주세요.", 5120022);
                        break;
                    case 926110100:
                        c.getPlayer().getMap().startMapEffect("비커에 액체를 가득 채워주세요.", 5120022);
                        break;
                    case 926110200:
                        c.getPlayer().getMap().startMapEffect("각 문을 통해 연구 자료를 찾아와 주세요!", 5120022);
                        break;
                    case 926110203:
                        c.getPlayer().getMap().startMapEffect("모든 몬스터를 없애주세요!", 5120022);
                        break;
                    case 926110300:
                        c.getPlayer().getMap().startMapEffect("연구실 꼭대기로 가는 길을 찾으세요!", 5120022);
                        break;
                    case 926110401:
                        c.getPlayer().getMap().startMapEffect("제 사랑 로미오를 보호해주세요!", 5120022);
                        break;
                }
                break;
            }
            case party6weatherMsg: {
                switch (c.getPlayer().getMapId()) {
                    case 930000000:
                        c.getPlayer().getMap().startMapEffect("중앙의 포탈을 타고 입장해. 지금 너에게 변신 마법을 걸게.", 5120023);
                        break;
                    case 930000010:
                        c.getPlayer().getMap().startMapEffect("본인이 누군지 헷갈리지 않도록 자신의 모습을 확인해!", 5120023);
                        break;
                    case 930000100:
                        c.getPlayer().getMap().startMapEffect("모든 몬스터를 없애!", 5120023);
                        break;
                    case 930000200:
                        c.getPlayer().getMap().startMapEffect("중앙의 웅덩이 위에서 몬스터를 없앤 후 웅덩이에서 나온 희석된 독으로 가시덤불을 없애!", 5120023);
                        break;
                    case 930000300:
                        c.getPlayer().getMap().startMapEffect("다들 어디 가버린거야? 포탈을 타고 내가 있는 곳까지 와!", 5120023);
                        break;
                    case 930000400:
                        c.getPlayer().getMap().startMapEffect("나에게 정화의 구슬을 받은 다음, 몬스터들을 캐치해서 몬스터 구슬 20개를 파티장이 가져와!", 5120023);
                        break;
                    case 930000500:
                        c.getPlayer().getMap().startMapEffect("괴인의 책상 앞에 있는 상자들을 열고 보라색 마력석을 가져와!", 5120023);
                        break;
                    case 930000600:
                        c.getPlayer().getMap().startMapEffect("괴인의 제단 위에 보라색 마력석을 올려놔봐!", 5120023);
                        break;
                }
                break;
            }
            case prisonBreak_mapEnter: {
                break;
            }   
            case Depart_Boss_F_Enter: {
                break;
            }         
            case shammos_Fenter: {
                if (c.getPlayer().getMapId() >= (921120005) && c.getPlayer().getMapId() < (921120500)) {
                    final MapleMonster shammos = MapleLifeFactory.getMonster(9300275);
                    if (c.getPlayer().getEventInstance() != null) {
                        int averageLevel = 0, size = 0;
                        for (MapleCharacter pl : c.getPlayer().getEventInstance().getPlayers()) {
                            averageLevel += pl.getLevel();
                            size++;
                        }
                        if (size <= 0) {
                            return;
                        }
                        averageLevel /= size;
                        //shammos.changeLevel(averageLevel);
                        c.getPlayer().getEventInstance().registerMonster(shammos);
                        if (c.getPlayer().getEventInstance().getProperty("HP") == null) {
                            c.getPlayer().getEventInstance().setProperty("HP", averageLevel + "000");
                        }
                        shammos.setHp(Long.parseLong(c.getPlayer().getEventInstance().getProperty("HP")));
                    }
                    c.getPlayer().getMap().spawnMonsterWithEffectBelow(shammos, new Point(c.getPlayer().getMap().getPortal(0).getPosition()), 12);
                    shammos.switchController(c.getPlayer(), false);
                    c.getSession().write(MaplePacketCreator.getNodeProperties(shammos, c.getPlayer().getMap()));
                }
                break;
            }
            case StageMsg_davy: {
                switch (c.getPlayer().getMapId()) {
                    case 925100000:
                        c.getPlayer().getMap().startMapEffect("몬스터를 모두 없애주세요!", 5120020);
                        break;
                    case 925100100:
                        c.getPlayer().getMap().startMapEffect("몬스터를 잡고 해적의 증표를 모아오세요!", 5120020);
                        break;
                    case 925100200:
                        c.getPlayer().getMap().startMapEffect("몬스터를 모두 없애주세요!", 5120020);
                        break;
                    case 925100300:
                        c.getPlayer().getMap().startMapEffect("몬스터를 모두 없애주세요!", 5120020);
                        break;
                    case 925100400:
                        c.getPlayer().getMap().startMapEffect("열쇠로 문을 잠궈주세요!", 5120020);
                        break;
                    case 925100500:
                        c.getPlayer().getMap().startMapEffect("해적왕을 물리쳐주세요!", 5120020);
                        break;
                }
                break;
            }
            case astaroth_summon: {
                c.getPlayer().getMap().resetFully();
                c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9400633), new Point(600, -26)); //rough estimate
                break;
            }
            case boss_Ravana_mirror:
            case boss_Ravana: { //event handles this so nothing for now until i find out something to do with it
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.serverNotice(5, "라바나가 나타났습니다!"));
                break;
            }
            case killing_BonusSetting: { //spawns monsters according to mapid
                //910320010-910320029 = Train 999 bubblings.
                //926010010-926010029 = 30 Yetis
                //926010030-926010049 = 35 Yetis
                //926010050-926010069 = 40 Yetis
                //926010070-926010089 - 50 Yetis (specialized? immortality)
                //TODO also find positions to spawn these at
                c.getPlayer().getMap().resetFully();
                c.getSession().write(MaplePacketCreator.showEffect("killing/bonus/bonus"));
                c.getSession().write(MaplePacketCreator.showEffect("killing/bonus/stage"));
                Point pos1 = null, pos2 = null, pos3 = null;
                int spawnPer = 0;
                int mobId = 0;
                //9700019, 9700029
                //9700021 = one thats invincible
                if (c.getPlayer().getMapId() >= 910320010 && c.getPlayer().getMapId() <= 910320029) {
                    pos1 = new Point(121, 218);
                    pos2 = new Point(396, 43);
                    pos3 = new Point(-63, 43);
                    mobId = 9700020;
                    spawnPer = 10;
                } else if (c.getPlayer().getMapId() >= 926010010 && c.getPlayer().getMapId() <= 926010029) {
                    pos1 = new Point(0, 88);
                    pos2 = new Point(-326, -115);
                    pos3 = new Point(361, -115);
                    mobId = 9700019;
                    spawnPer = 10;
                } else if (c.getPlayer().getMapId() >= 926010030 && c.getPlayer().getMapId() <= 926010049) {
                    pos1 = new Point(0, 88);
                    pos2 = new Point(-326, -115);
                    pos3 = new Point(361, -115);
                    mobId = 9700019;
                    spawnPer = 15;
                } else if (c.getPlayer().getMapId() >= 926010050 && c.getPlayer().getMapId() <= 926010069) {
                    pos1 = new Point(0, 88);
                    pos2 = new Point(-326, -115);
                    pos3 = new Point(361, -115);
                    mobId = 9700019;
                    spawnPer = 20;
                } else if (c.getPlayer().getMapId() >= 926010070 && c.getPlayer().getMapId() <= 926010089) {
                    pos1 = new Point(0, 88);
                    pos2 = new Point(-326, -115);
                    pos3 = new Point(361, -115);
                    mobId = 9700029;
                    spawnPer = 20;
                } else {
                    break;
                }
                for (int i = 0; i < spawnPer; i++) {
                    c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(mobId), new Point(pos1));
                    c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(mobId), new Point(pos2));
                    c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(mobId), new Point(pos3));
                }
                c.getPlayer().startMapTimeLimitTask(120, c.getPlayer().getMap().getForcedReturnMap());
                break;
            }

            case mPark_summonBoss: {
                if (c.getPlayer().getEventInstance() != null && c.getPlayer().getEventInstance().getProperty("boss") != null && c.getPlayer().getEventInstance().getProperty("boss").equals("0")) {
                    for (int i = 9800119; i < 9800125; i++) {
                        final MapleMonster boss = MapleLifeFactory.getMonster(i);
                        c.getPlayer().getEventInstance().registerMonster(boss);
                        c.getPlayer().getMap().spawnMonsterOnGroundBelow(boss, new Point(c.getPlayer().getMap().getPortal(2).getPosition()));
                    }
                }
                break;
            }
            case mpark_mobRegen: {
		for (MapleMapObject mob : c.getPlayer().getMap().getAllMonster()) {
                    MapleMonster monster = (MapleMonster) mob;
                    c.getPlayer().getEventInstance().registerMonster(monster);
                }
		break;
            }
            //5120038 =  dr bing. 5120039 = visitor lady. 5120041 = unknown dr bing.
            case iceman_FEnter: {
                if (c.getPlayer().getMapId() >= 932000100 && c.getPlayer().getMapId() < 932000300) {
                    final MapleMonster shammos = MapleLifeFactory.getMonster(9300438);
                    if (c.getPlayer().getEventInstance() != null) {
                        int averageLevel = 0, size = 0;
                        for (MapleCharacter pl : c.getPlayer().getEventInstance().getPlayers()) {
                            averageLevel += pl.getLevel();
                            size++;
                        }
                        if (size <= 0) {
                            return;
                        }
                        averageLevel /= size;
                        shammos.changeLevel(averageLevel);
                        c.getPlayer().getEventInstance().registerMonster(shammos);
                        if (c.getPlayer().getEventInstance().getProperty("HP") == null) {
                            c.getPlayer().getEventInstance().setProperty("HP", averageLevel + "000");
                        }
                        shammos.setHp(Long.parseLong(c.getPlayer().getEventInstance().getProperty("HP")));
                    }
                    c.getPlayer().getMap().spawnMonsterWithEffectBelow(shammos, new Point(c.getPlayer().getMap().getPortal(0).getPosition()), 12);
                    shammos.switchController(c.getPlayer(), false);
                    c.getSession().write(MaplePacketCreator.getNodeProperties(shammos, c.getPlayer().getMap()));

                }
                break;
            }
            case PRaid_D_Fenter: {
                switch (c.getPlayer().getMapId() % 10) {
                    case 0:
                        c.getPlayer().getMap().startMapEffect("몬스터를 모두 퇴치해라!", 5120033);
                        break;
                    case 1:
                        c.getPlayer().getMap().startMapEffect("상자를 부수고, 나오는 몬스터를 모두 퇴치해라!", 5120033);
                        break;
                    case 2:
                        c.getPlayer().getMap().startMapEffect("일등항해사를 퇴치해라!", 5120033);
                        break;
                    case 3:
                        c.getPlayer().getMap().startMapEffect("몬스터를 모두 퇴치해라!", 5120033);
                        break;
                    case 4:
                        c.getPlayer().getMap().startMapEffect("몬스터를 모두 퇴치하고, 점프대를 작동시켜서 건너편으로 건너가라!", 5120033);
                        break;
                    case 5:
                        c.getPlayer().getMap().startMapEffect("상대편보다 먼저 몬스터를 퇴치하라!", 5120033);
                        break;
                }
                break;
            }
            case PRaid_B_Fenter: {
                c.getPlayer().getMap().startMapEffect("상대편보다 먼저 몬스터를 퇴치하라!", 5120033);
                break;
            }
                    
            case summon_pepeking: {
                c.getPlayer().getMap().resetFully();
                final int rand = Randomizer.nextInt(10);
                int mob_ToSpawn = 100100;
                if (rand >= 4) { //60%
                    mob_ToSpawn = 3300007;
                    c.getSession().write(MaplePacketCreator.showEffect("pepeKing/pepe/pepeW"));
                    c.getSession().write(MaplePacketCreator.showEffect("pepeKing/chat/nugu"));
                    //c.getSession().write(MaplePacketCreator.showEffect("pepeKing/frame/W"));
                    //c.getSession().write(MaplePacketCreator.showEffect("pepeKing/frame/B"));
                } else if (rand >= 1) {
                    mob_ToSpawn = 3300006;
                    c.getSession().write(MaplePacketCreator.showEffect("pepeKing/pepe/pepeG"));
                    c.getSession().write(MaplePacketCreator.showEffect("pepeKing/chat/nugu"));
                    //c.getSession().write(MaplePacketCreator.showEffect("pepeKing/frame/W"));
                   // c.getSession().write(MaplePacketCreator.showEffect("pepeKing/frame/B"));
                } else {
                    mob_ToSpawn = 3300005;
                    c.getSession().write(MaplePacketCreator.showEffect("pepeKing/pepe/pepeB"));
                    c.getSession().write(MaplePacketCreator.showEffect("pepeKing/chat/nugu"));
                   // c.getSession().write(MaplePacketCreator.showEffect("pepeKing/frame/W"));
                   // c.getSession().write(MaplePacketCreator.showEffect("pepeKing/frame/B"));
                }
                c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(mob_ToSpawn), c.getPlayer().getPosition());
                break;
            }
            case Xerxes_summon: {
                c.getPlayer().getMap().resetFully();
                c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(6160003), c.getPlayer().getPosition());
                break;
            }
            case shammos_FStart:
                c.getPlayer().getMap().startMapEffect("Defeat the monsters!", 5120035);
                break;
            case kenta_mapEnter:
                switch (c.getPlayer().getMapId()) {
                    case 923040100:
                        if (c.getPlayer().getEventInstance().getProperty("kenta_nextStage") == null) {
                            c.getPlayer().getEventInstance().setProperty("kenta_nextStage", "1");
                        }
                        if (c.getPlayer().getEventInstance().getProperty("kentasave") == null) {
                            c.getPlayer().getEventInstance().setProperty("kentasave", "1");
                        }
                        c.getPlayer().getMap().spawnNpc(9020004, new Point(45, 300));
                        c.getPlayer().getMap().startMapEffectAOJ("모든 몬스터를 처치후 켄타와 대화하세요.", 5120036, false);
                        break;
                    case 923040200:
                        c.getPlayer().getMap().spawnNpc(9020004, new Point(52, -422));
                        c.getPlayer().getMap().startMapEffectAOJ("공기방울 30개를모아 켄타와 대화하세요.", 5120036, false);
                        break;
                    case 923040300:
//                        final MapleMonster shammos = MapleLifeFactory.getMonster(9300460);
//                        c.getPlayer().getEventInstance().registerMonster(shammos);
//                        shammos.setHp(300000);
//                        c.getPlayer().getMap().spawnMonsterWithEffectBelow(shammos, new Point(-121, 89), 12);
//                        c.getPlayer().getMap().spawnNpc(9020004, new Point(-66, 625));
                        c.getPlayer().getMap().startMapEffectAOJ("3분간 켄타를 지켜주세요!!", 5120036, false);
                        break;
                    case 923040400:
                        c.getPlayer().getMap().spawnNpc(9020004, new Point(-220, 168));
                        c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9300467), new Point(582,168));
                        c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9300468), new Point(-1190, 168));
                        c.getPlayer().getMap().startMapEffectAOJ("피아누스가 양쪽에 나타났습니다 모두 처리해주세요", 5120036, false);
                        break;
                } //TODOO find out which one it really is, lol
                break;
            case cygnus_Summon: {
                c.getPlayer().getMap().startMapEffect("오랜만에 손님이 왔군요. 여기서 아무도 살아간 자는 없습니다.", 5120026);
                break;
            }
            case iceman_Boss: {
                c.getPlayer().getMap().startMapEffect("You will perish!", 5120050);
                break;
            }
            case Visitor_Cube_poison: {
                c.getPlayer().getMap().startMapEffect("Eliminate all the monsters!", 5120039);
                break;
            }
            case Visitor_Cube_Hunting_Enter_First: {
                c.getPlayer().getMap().startMapEffect("Eliminate all the Visitors!", 5120039);
                break;
            }
            case VisitorCubePhase00_Start: {
                c.getPlayer().getMap().startMapEffect("Eliminate all the flying monsters!", 5120039);
                break;
            }
            case visitorCube_addmobEnter: {
                c.getPlayer().getMap().startMapEffect("Eliminate all the monsters by moving around the map!", 5120039);
                break;
            }
            case Visitor_Cube_PickAnswer_Enter_First_1: {
                c.getPlayer().getMap().startMapEffect("One of the aliens must have a clue to the way out.", 5120039);
                break;
            }
            case visitorCube_medicroom_Enter: {
                c.getPlayer().getMap().startMapEffect("Eliminate all of the Unjust Visitors!", 5120039);
                break;
            }
            case visitorCube_iceyunna_Enter: {
                c.getPlayer().getMap().startMapEffect("Eliminate all of the Speedy Visitors!", 5120039);
                break;
            }
            case Visitor_Cube_AreaCheck_Enter_First: {
                c.getPlayer().getMap().startMapEffect("The switch at the top of the room requires a heavy weight.", 5120039);
                break;
            }
            case visitorCube_boomboom_Enter: {
                c.getPlayer().getMap().startMapEffect("The enemy is powerful! Watch out!", 5120039);
                break;
            }
            case visitorCube_boomboom2_Enter: {
                c.getPlayer().getMap().startMapEffect("This Visitor is strong! Be careful!", 5120039);
                break;
            }
            case CubeBossbang_Enter: {
                c.getPlayer().getMap().startMapEffect("This is it! Give it your best shot!", 5120039);
                break;
            }
            case MalayBoss_Int:
            case storymap_scenario:
            case VanLeon_Before:
            case dojang_Msg:
            case balog_summon:
            case easy_balog_summon: { //we dont want to reset
                break;
            }
            case metro_firstSetting:
            case killing_MapSetting:
            case Sky_TrapFEnter:
            case balog_bonusSetting: { //not needed
                c.getPlayer().getMap().resetFully();
                break;
            }
            default: {
                System.out.println("Unhandled script : " + scriptName + ", type : onFirstUserEnter - MAPID " + c.getPlayer().getMapId());
                FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Unhandled script : " + scriptName + ", type : onFirstUserEnter - MAPID " + c.getPlayer().getMapId());
                break;
            }
        }
    }

    public static void startScript_User(final MapleClient c, String scriptName) {
        if (c.getPlayer() == null) {
            return;
        } //o_O
        String data = "";
        switch (onUserEnter.fromString(scriptName)) {
            case cannon_tuto_direction: {
                showIntro(c, "Effect/Direction4.img/cannonshooter/Scene00");
                showIntro(c, "Effect/Direction4.img/cannonshooter/out00");
                break;
            }
            case cannon_tuto_direction1: {
                c.getSession().write(UIPacket.IntroDisableUI(true));
                c.getSession().write(UIPacket.IntroLock(true));
//                c.getSession().write(UIPacket.getDirectionInfo("Effect/Direction4.img/effect/cannonshooter/balloon/0", 5000, 0, 0, 1));
                //              c.getSession().write(UIPacket.getDirectionInfo("Effect/Direction4.img/effect/cannonshooter/balloon/1", 5000, 0, 0, 1));
                //            c.getSession().write(UIPacket.getDirectionInfo("Effect/Direction4.img/effect/cannonshooter/balloon/2", 5000, 0, 0, 1));
                c.getSession().write(UIPacket.ShowWZEffect("Effect/Direction4.img/cannonshooter/face04"));
                c.getSession().write(UIPacket.ShowWZEffect("Effect/Direction4.img/cannonshooter/out01"));
                //          c.getSession().write(UIPacket.getDirectionInfo(1, 5000));
                break;
            }
            case cannon_tuto_direction2: {
                showIntro(c, "Effect/Direction4.img/cannonshooter/Scene01");
                showIntro(c, "Effect/Direction4.img/cannonshooter/out02");
                break;
            }
            case cygnusTest: {
                showIntro(c, "Effect/Direction.img/cygnus/Scene" + (c.getPlayer().getMapId() == 913040006 ? 9 : (c.getPlayer().getMapId() - 913040000)));
                break;
            }
            case cygnusJobTutorial: {
                showIntro(c, "Effect/Direction.img/cygnusJobTutorial/Scene" + (c.getPlayer().getMapId() - 913040100));
                break;
            }
            case shammos_Enter: { //nothing to go on inside the map
                if (c.getPlayer().getEventInstance() != null && c.getPlayer().getMapId() == (GameConstants.GMS ? 921120300 : 921120500)) {
                    NPCScriptManager.getInstance().dispose(c); //only boss map.
                    c.removeClickedNPC();
                    NPCScriptManager.getInstance().start(c, 2022006);
                }
                break;
            }
            case iceman_Enter: { //nothing to go on inside the map
                if (c.getPlayer().getEventInstance() != null && c.getPlayer().getMapId() == 932000300) {
                    NPCScriptManager.getInstance().dispose(c); //only boss map.
                    c.removeClickedNPC();
                    NPCScriptManager.getInstance().start(c, 2159020);
                }
                break;
            }
            case start_itemTake: { //nothing to go on inside the map
                break;
            }
            case PRaid_W_Enter: {
                c.getSession().write(MaplePacketCreator.sendPyramidEnergy("PRaid_expPenalty", "0"));
                c.getSession().write(MaplePacketCreator.sendPyramidEnergy("PRaid_ElapssedTimeAtField", "0"));
                c.getSession().write(MaplePacketCreator.sendPyramidEnergy("PRaid_Point", "-1"));
                c.getSession().write(MaplePacketCreator.sendPyramidEnergy("PRaid_Bonus", "-1"));
                c.getSession().write(MaplePacketCreator.sendPyramidEnergy("PRaid_Total", "-1"));
                c.getSession().write(MaplePacketCreator.sendPyramidEnergy("PRaid_Team", ""));
                c.getSession().write(MaplePacketCreator.sendPyramidEnergy("PRaid_IsRevive", "0"));
                c.getPlayer().writePoint("PRaid_Point", "-1");
                c.getPlayer().writeStatus("Red_Stage", "1");
                c.getPlayer().writeStatus("Blue_Stage", "1");
                c.getPlayer().writeStatus("redTeamDamage", "0");
                c.getPlayer().writeStatus("blueTeamDamage", "0");
                break;
            }
            case jail: {
                if (!c.getPlayer().isIntern()) {
                    c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.JAIL_TIME)).setCustomData(String.valueOf(System.currentTimeMillis()));
                    final MapleQuestStatus stat = c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.JAIL_QUEST));
                    if (stat.getCustomData() != null) {
                        final int seconds = Integer.parseInt(stat.getCustomData());
                        if (seconds > 0) {
                            c.getPlayer().startMapTimeLimitTask(seconds, c.getChannelServer().getMapFactory().getMap(100000000));
                        }
                    }
                }
                break;
            }
            case TD_neo_BossEnter:
            case findvioleta: {
                c.getPlayer().getMap().resetFully();
                break;
            }

            case StageMsg_crack:
                if (c.getPlayer().getMapId() == 922010400) { //2nd stage
                    MapleMapFactory mf = c.getChannelServer().getMapFactory();
                    int q = 0;
                    for (int i = 0; i < 5; i++) {
                        q += mf.getMap(922010401 + i).getAllMonstersThreadsafe().size();
                    }
                    if (q > 0) {
                        c.getPlayer().dropMessage(-1, "There are still " + q + " monsters remaining.");
                    }
                } else if (c.getPlayer().getMapId() >= 922010401 && c.getPlayer().getMapId() <= 922010405) {
                    if (c.getPlayer().getMap().getAllMonstersThreadsafe().size() > 0) {
                        c.getPlayer().dropMessage(-1, "There are still some monsters remaining in this map.");
                    } else {
                        c.getPlayer().dropMessage(-1, "There are no monsters remaining in this map.");
                    }
                }
                break;
            case q31102e:
                if (c.getPlayer().getQuestStatus(31102) == 1) {
                    MapleQuest.getInstance(31102).forceComplete(c.getPlayer(), 2140000);
                }
                break;
            case q31103s:
                if (c.getPlayer().getQuestStatus(31103) == 0) {
                    MapleQuest.getInstance(31103).forceComplete(c.getPlayer(), 2142003);
                }
                break;
            case Resi_tutor20:
                c.getSession().write(UIPacket.MapEff("resistance/tutorialGuide"));
                break;
            case Resi_tutor30:
                c.getSession().write(UIPacket.AranTutInstructionalBalloon("Effect/OnUserEff.img/guideEffect/resistanceTutorial/userTalk"));
                break;
            case Resi_tutor40:
                NPCScriptManager.getInstance().dispose(c);
                c.removeClickedNPC();
                NPCScriptManager.getInstance().start(c, 2159012);
                break;
            case Resi_tutor50:
                c.getSession().write(UIPacket.IntroDisableUI(false));
                c.getSession().write(UIPacket.IntroLock(false));
                c.getSession().write(MaplePacketCreator.enableActions());
                NPCScriptManager.getInstance().dispose(c);
                c.removeClickedNPC();
                NPCScriptManager.getInstance().start(c, 2159006);
                break;
            case Resi_tutor70:
                showIntro(c, "Effect/Direction4.img/Resistance/TalkJ");
                break;
            case prisonBreak_1stageEnter:
            case shammos_Start:
            case moonrabbit_takeawayitem:
            case TCMobrevive:
            case cygnus_ExpeditionEnter:
            case knights_Summon:
          //  case VanLeon_ExpeditionEnter:
            case Resi_tutor10:
            case Resi_tutor60:
            case Resi_tutor50_1:
            case sealGarden:
                
                if (c.getPlayer().getQuestStatus(21739) == 1 && c.getPlayer().getQuestNoAdd(MapleQuest.getInstance(21739)).getMobKills(9300348) == 0) {
                    c.getPlayer().getMap().resetFully();
                    c.getPlayer().getMap().spawnNpc(1204010, new Point(731, 83));
                }
                break;                
            case in_secretroom:
            case TD_MC_gasi2: {
                c.getSession().write(MaplePacketCreator.enableActions());
                break;
            }   
            case TD_MC_keycheck:
                MapleQuestStatus q = c.getPlayer().getQuestNoAdd(MapleQuest.getInstance(2330));
                if (q == null) {
                    break;
                }
                if (q.getStatus() == 1) {
                    if (q.getMobKills(3300007) == 1 && q.getMobKills(3300006) == 1 && q.getMobKills(3300005) == 1) {
                        if (!c.getPlayer().haveItem(4032388)) {
                            c.getPlayer().gainItem(4032388, (short) 1, true);
                            c.getPlayer().dropMessage(-1, "결혼식장 열쇠 획득 1 / 1");
                        }
                    }
                } else if (q.getStatus() == 2) {
                    if (!c.getPlayer().haveItem(4032388)) {
                        c.getPlayer().gainItem(4032388, (short) 1, true);
                        c.getPlayer().dropMessage(-1, "결혼식장 열쇠 획득 1 / 1");
                    }
                }
                break;                
            case pepeking_effect:
            case userInBattleSquare:
            case summonSchiller:
            case VisitorleaveDirectionMode:
            case visitorPT_Enter:
            case VisitorCubePhase00_Enter:
            case visitor_ReviveMap:
            case PRaid_D_Enter:
            case PRaid_B_Enter:
            case PRaid_WinEnter: //handled by event
            case PRaid_FailEnter: //also
            case PRaid_Revive: //likely to subtract points or remove a life, but idc rly
            case metro_firstSetting:
            case blackSDI:
                c.getPlayer().getMap().spawnNpc(1013204, new Point(-288, 53));
                c.getPlayer().getMap().spawnNpc(1205000, new Point(360, 145));
                break;         
            case summonIceWall:
                c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9300391), new Point(35, 145));
                break;             
            case onSDI:
                if (c.getPlayer().getQuestStatus(22579) == 2) {
                    if (c.getPlayer().getQuestStatus(22599) == 0) {
                        MapleQuest.getInstance(22599).forceStart(c.getPlayer(), 0, "1");
                    }
                }
                if (c.getPlayer().getQuestStatus(22588) == 2) {
                    if (c.getPlayer().getQuestStatus(22600) == 0) {
                        MapleQuest.getInstance(22600).forceStart(c.getPlayer(), 0, "1");
                    }
                }
                break;             
            case enterBlackfrog:
                if (c.getPlayer().getQuestStatus(22596) == 1) {
                    if (c.getPlayer().getQuestNoAdd(MapleQuest.getInstance(22596)).getMobKills(9300393) == 0) {
                        c.getPlayer().getMap().spawnNpc(1013206, new Point(165, 31));
                    }
                }
                break;
            case Sky_Quest: //forest that disappeared 240030102
            case dollCave00:
            case dollCave01:
            case dollCave02:
            case shammos_Base:
                if (c.getPlayer().getMapId() >= (921120005) && c.getPlayer().getMapId() < (921120500)) {
                    final MapleMonster shammos = MapleLifeFactory.getMonster(9300275);
                    if (c.getPlayer().getEventInstance() != null) {
                        int averageLevel = 0, size = 0;
                        for (MapleCharacter pl : c.getPlayer().getEventInstance().getPlayers()) {
                            averageLevel += pl.getLevel();
                            size++;
                        }
                        if (size <= 0) {
                            return;
                        }
                        averageLevel /= size;
                        //shammos.changeLevel(averageLevel);
                        c.getPlayer().getEventInstance().registerMonster(shammos);
                        if (c.getPlayer().getEventInstance().getProperty("HP") == null) {
                            c.getPlayer().getEventInstance().setProperty("HP", averageLevel + "000");
                        }
                        shammos.setHp(Long.parseLong(c.getPlayer().getEventInstance().getProperty("HP")));
                    }
                    c.getPlayer().getMap().spawnMonsterWithEffectBelow(shammos, new Point(c.getPlayer().getMap().getPortal(0).getPosition()), 12);
                    shammos.switchController(c.getPlayer(), false);
                    c.getSession().write(MaplePacketCreator.getNodeProperties(shammos, c.getPlayer().getMap()));
                }
                break;              
            case shammos_Result:
                break;              
            case Depart_inSubway: {
                switch (c.getPlayer().getMapId() % 10) {
                    case 0:
                        //c.getPlayer().dropMessage(6, "이번 정차역은 커닝 스퀘어 역 입니다. 내리실 문은 왼쪽입니다.");
                        c.getPlayer().dropMessage(-1, "이번 정차역은 커닝 스퀘어 역 입니다. 내리실 문은 왼쪽입니다.");
                        break;
                    case 1:
                        //c.getPlayer().dropMessage(6, "이번 정차역은 지하철매표소 입니다. 내리실 문은 왼쪽입니다.");
                        c.getPlayer().dropMessage(-1, "이번 정차역은 지하철매표소 입니다. 내리실 문은 왼쪽입니다.");
                        break;
                }
                break;
            }
            case Depart_topFloorEnter: { // 커닝스퀘어 일반맵
                break;
            }                
            case Sky_BossEnter:
                c.getPlayer().dropMessage(5, "용족의 기운으로 소비아이템 사용에 제한이 가해집니다. 쿨 타임 20초가 적용됩니다.");
                break;            
            case Sky_GateMapEnter:
            case balog_dateSet:
            case balog_buff:
            case outCase:
            case Sky_StageEnter:
            case dojang_QcheckSet:
            case evanTogether:
            case merStandAlone:
            case EntereurelTW:
            case aranTutorAlone:
            case evanAlone: { //no idea
                c.getSession().write(MaplePacketCreator.enableActions());
                break;
            }
            case merOutStandAlone: {
                if (c.getPlayer().getQuestStatus(24001) == 1) {
                    MapleQuest.getInstance(24001).forceComplete(c.getPlayer(), 0);
                    c.getPlayer().dropMessage(5, "Quest complete.");
                }
                break;
            }
            case merTutorSleep00: {
                showIntro(c, "Effect/Direction5.img/mersedesTutorial/Scene0");
                c.getPlayer().changeSkillLevel(SkillFactory.getSkill(20021181), -1, (byte) 0);
                c.getPlayer().changeSkillLevel(SkillFactory.getSkill(20021166), -1, (byte) 0);
                c.getPlayer().changeSkillLevel(SkillFactory.getSkill(20020109), 1, (byte) 1);
                c.getPlayer().changeSkillLevel(SkillFactory.getSkill(20021110), 1, (byte) 1);
                c.getPlayer().changeSkillLevel(SkillFactory.getSkill(20020111), 1, (byte) 1);
                c.getPlayer().changeSkillLevel(SkillFactory.getSkill(20020112), 1, (byte) 1);
                break;
            }
            case merTutorSleep01: {
                while (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().levelUp();
                }
                c.getPlayer().changeJob(2300);
                showIntro(c, "Effect/Direction5.img/mersedesTutorial/Scene1");
                break;
            }
            case merTutorSleep02: {
//                c.getSession().write(UIPacket.IntroEnableUI(0));
                break;
            }
            case merTutorDrecotion00: {
                //          c.getSession().write(UIPacket.playMovie("Mercedes.avi", true));
                c.getPlayer().changeSkillLevel(SkillFactory.getSkill(20021181), 1, (byte) 1);
                c.getPlayer().changeSkillLevel(SkillFactory.getSkill(20021166), 1, (byte) 1);
                break;
            }
            case merTutorDrecotion10: {
                //    c.getSession().write(UIPacket.getDirectionStatus(true));
                //  c.getSession().write(UIPacket.IntroEnableUI(1));
                //c.getSession().write(UIPacket.getDirectionInfo("Effect/Direction5.img/effect/mercedesInIce/merBalloon/6", 2000, 0, -100, 1));
                // c.getSession().write(UIPacket.getDirectionInfo(1, 2000));
                c.getPlayer().setDirection(0);
                break;
            }
            case merTutorDrecotion20: {
//                c.getSession().write(UIPacket.getDirectionStatus(true));
                //               c.getSession().write(UIPacket.IntroEnableUI(1));
                //              c.getSession().write(UIPacket.getDirectionInfo("Effect/Direction5.img/effect/mercedesInIce/merBalloon/9", 2000, 0, -100, 1));
                //             c.getSession().write(UIPacket.getDirectionInfo(1, 2000));
                c.getPlayer().setDirection(0);
                break;
            }
            case ds_tuto_ani: {
//                c.getSession().write(UIPacket.playMovie("DemonSlayer1.avi", true));
                break;
            }
            case Resi_tutor80:
            case startEreb:
            case mirrorCave:
            case babyPigMap:
            case evanleaveD: {
                c.getSession().write(UIPacket.IntroDisableUI(false));
                c.getSession().write(UIPacket.IntroLock(false));
                c.getSession().write(MaplePacketCreator.enableActions());
                break;
            }
            case dojang_Msg: {
                c.getPlayer().getMap().startMapEffect(mulungEffects[Randomizer.nextInt(mulungEffects.length)], 5120024);
                break;
            }
            case dojang_1st: {
                c.getPlayer().writeMulungEnergy();
                break;
            }
            case undomorphdarco:
            case reundodraco: {
                c.getPlayer().cancelEffect(MapleItemInformationProvider.getInstance().getItemEffect(2210016), -1);
                break;
            }
            case Depart_BossEnter:
                break;            
            case goAdventure: {
                // BUG in MSEA v.91, so let's skip this part.
                //if (GameConstants.GMS) {
                //	c.getPlayer().changeMap(c.getChannelServer().getMapFactory().getMap(10000));
                //} else {
                showIntro(c, "Effect/Direction3.img/goAdventure/Scene" + (c.getPlayer().getGender() == 0 ? "0" : "1"));
                //}
                break;
            }
            case crash_Dragon:
                showIntro(c, "Effect/Direction4.img/crash/Scene" + (c.getPlayer().getGender() == 0 ? "0" : "1"));
                break;
            case getDragonEgg:
                showIntro(c, "Effect/Direction4.img/getDragonEgg/Scene" + (c.getPlayer().getGender() == 0 ? "0" : "1"));
                break;
            case meetWithDragon:
                showIntro(c, "Effect/Direction4.img/meetWithDragon/Scene" + (c.getPlayer().getGender() == 0 ? "0" : "1"));
                break;
            case PromiseDragon:
                showIntro(c, "Effect/Direction4.img/PromiseDragon/Scene" + (c.getPlayer().getGender() == 0 ? "0" : "1"));
                break;
            case evanPromotion:
                switch (c.getPlayer().getMapId()) {
                    case 900090000:
                        data = "Effect/Direction4.img/promotion/Scene0" + (c.getPlayer().getGender() == 0 ? "0" : "1");
                        break;
                    case 900090001:
                        data = "Effect/Direction4.img/promotion/Scene1";
                        break;
                    case 900090002:
                        data = "Effect/Direction4.img/promotion/Scene2" + (c.getPlayer().getGender() == 0 ? "0" : "1");
                        break;
                    case 900090003:
                        data = "Effect/Direction4.img/promotion/Scene3";
                        break;
                    case 900090004:
                        c.getSession().write(UIPacket.IntroDisableUI(false));
                        c.getSession().write(UIPacket.IntroLock(false));
                        c.getSession().write(MaplePacketCreator.enableActions());
                        final MapleMap mapto = c.getChannelServer().getMapFactory().getMap(900010000);
                        c.getPlayer().changeMap(mapto, mapto.getPortal(0));
                        return;
                }
                showIntro(c, data);
                break;
           case mPark_stageEff:
                int stage =(c.getPlayer().getMap().getId() / 100) % 100;
                if (stage == 5) {
                    c.getSession().write(UIPacket.MapEff("monsterPark/stageEff/final"));
                } else {
                    c.getSession().write(UIPacket.MapEff("monsterPark/stageEff/stage"));
		    c.getSession().write(UIPacket.MapEff("monsterPark/stageEff/number/" + (stage + 1)));
                }
                c.getPlayer().dropMessage(5, "필드 내의 모든 몬스터를 제거해야 다음 스테이지로 이동하실 수 있습니다.");
                c.getPlayer().dropMessage(5, "파티원 한명당 추가경험치를 획득합니다.");
		break; 
            case TD_MC_title: {
                c.getSession().write(UIPacket.IntroDisableUI(false));
                c.getSession().write(UIPacket.IntroLock(false));
                c.getSession().write(MaplePacketCreator.enableActions());
                c.getSession().write(UIPacket.MapEff("temaD/enter/mushCatle"));
                break;
            }
            case TD_NC_title: {
                switch ((c.getPlayer().getMapId() / 100) % 10) {
                    case 0:
                        c.getSession().write(UIPacket.MapEff("temaD/enter/teraForest"));
                        break;
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                        c.getSession().write(UIPacket.MapEff("temaD/enter/neoCity" + ((c.getPlayer().getMapId() / 100) % 10)));
                        break;
                }
                break;
            }
            case explorationPoint: {
                if (c.getPlayer().getMapId() == 104000000) {
                    c.getSession().write(UIPacket.IntroDisableUI(false));
                    c.getSession().write(UIPacket.IntroLock(false));
                    c.getSession().write(MaplePacketCreator.enableActions());
                    c.getSession().write(UIPacket.MapNameDisplay(c.getPlayer().getMapId()));
                }
                MedalQuest m = null;
                for (MedalQuest mq : MedalQuest.values()) {
                    for (int i : mq.maps) {
                        if (c.getPlayer().getMapId() == i) {
                            m = mq;
                            break;
                        }
                    }
                }
                if (m != null && c.getPlayer().getLevel() >= m.level && c.getPlayer().getQuestStatus(m.questid) != 2) {
                    if (c.getPlayer().getQuestStatus(m.lquestid) != 1) {
                        MapleQuest.getInstance(m.lquestid).forceStart(c.getPlayer(), 0, "0");
                    }
                    if (c.getPlayer().getQuestStatus(m.questid) != 1) {
                        MapleQuest.getInstance(m.questid).forceStart(c.getPlayer(), 0, null);
                        final StringBuilder sb = new StringBuilder("enter=");
                        for (int i = 0; i < m.maps.length; i++) {
                            sb.append("0");
                        }
                        c.getPlayer().updateInfoQuest(m.questid - 2005, sb.toString());
                        MapleQuest.getInstance(m.questid - 1995).forceStart(c.getPlayer(), 0, "0");
                    }
                    String quest = c.getPlayer().getInfoQuest(m.questid - 2005);
                    if (quest.length() != m.maps.length + 6) { //enter= is 6
                        final StringBuilder sb = new StringBuilder("enter=");
                        for (int i = 0; i < m.maps.length; i++) {
                            sb.append("0");
                        }
                        quest = sb.toString();
                        c.getPlayer().updateInfoQuest(m.questid - 2005, quest);
                    }
                    final MapleQuestStatus stat = c.getPlayer().getQuestNAdd(MapleQuest.getInstance(m.questid - 1995));
                    if (stat.getCustomData() == null) { //just a check.
                        stat.setCustomData("0");
                    }
                    int number = Integer.parseInt(stat.getCustomData());
                    final StringBuilder sb = new StringBuilder("enter=");
                    boolean changedd = false;
                    for (int i = 0; i < m.maps.length; i++) {
                        boolean changed = false;
                        if (c.getPlayer().getMapId() == m.maps[i]) {
                            if (quest.substring(i + 6, i + 7).equals("0")) {
                                sb.append("1");
                                changed = true;
                                changedd = true;
                            }
                        }
                        if (!changed) {
                            sb.append(quest.substring(i + 6, i + 7));
                        }
                    }
                    if (changedd) {
                        number++;
                        c.getPlayer().updateInfoQuest(m.questid - 2005, sb.toString());
                        MapleQuest.getInstance(m.questid - 1995).forceStart(c.getPlayer(), 0, String.valueOf(number));
                        c.getSession().write(MaplePacketCreator.showQuestMsg("칭호 -  " + String.valueOf(m) + " 탐험가 도전 중. " + number + "/" + m.maps.length + " 완료"));
                    }
                }
                break;
            }
            case go10000:
            case go1020000:
                c.getSession().write(UIPacket.IntroDisableUI(false));
                c.getSession().write(UIPacket.IntroLock(false));
                c.getSession().write(MaplePacketCreator.enableActions());
            case go20000:
            case go30000:
            case go40000:
            case go50000:
            case go1000000:
            case go2000000:
            case go1010000:
            case go1010100:
            case go1010200:
            case go1010300:
            case go1010400: {
                c.getSession().write(UIPacket.MapNameDisplay(c.getPlayer().getMapId()));
                break;
            }
            case ds_tuto_ill0: {
//                c.getSession().write(UIPacket.getDirectionInfo(1, 6300));
                showIntro(c, "Effect/Direction6.img/DemonTutorial/SceneLogo");
                EventTimer.getInstance().schedule(new Runnable() {

                    public void run() {
                        c.getSession().write(UIPacket.IntroDisableUI(false));
                        c.getSession().write(UIPacket.IntroLock(false));
                        c.getSession().write(MaplePacketCreator.enableActions());
                        final MapleMap mapto = c.getChannelServer().getMapFactory().getMap(927000000);
                        c.getPlayer().changeMap(mapto, mapto.getPortal(0));
                    }
                }, 6300); //wtf
                break;
            }
            case ds_tuto_home_before: {
                //      c.getSession().write(UIPacket.getDirectionInfo(3, 1));
                //      c.getSession().write(UIPacket.getDirectionInfo(1, 30));
                //      c.getSession().write(UIPacket.getDirectionStatus(true));
                //      c.getSession().write(UIPacket.getDirectionInfo(3, 0));
                //      c.getSession().write(UIPacket.getDirectionInfo(1, 90));

                c.getSession().write(MaplePacketCreator.showEffect("demonSlayer/text11"));
                //      c.getSession().write(UIPacket.getDirectionInfo(1, 4000));
                EventTimer.getInstance().schedule(new Runnable() {

                    public void run() {
                        showIntro(c, "Effect/Direction6.img/DemonTutorial/Scene2");
                    }
                }, 1000);
                break;
            }
            case ds_tuto_1_0: {
//                c.getSession().write(UIPacket.getDirectionInfo(3, 1));
                //              c.getSession().write(UIPacket.getDirectionInfo(1, 30));
                //            c.getSession().write(UIPacket.getDirectionStatus(true));

                EventTimer.getInstance().schedule(new Runnable() {

                    public void run() {
                        //                  c.getSession().write(UIPacket.getDirectionInfo(3, 0));
                        //                c.getSession().write(UIPacket.getDirectionInfo(4, 2159310));
                        NPCScriptManager.getInstance().start(c, 2159310);
                    }
                }, 1000);
                break;
            }
            case ds_tuto_4_0: {
                c.getSession().write(UIPacket.IntroDisableUI(true));
                //        c.getSession().write(UIPacket.IntroEnableUI(1));
                //      c.getSession().write(UIPacket.getDirectionStatus(true));
                //    c.getSession().write(UIPacket.getDirectionInfo(3, 0));
                //  c.getSession().write(UIPacket.getDirectionInfo(4, 2159344));
                NPCScriptManager.getInstance().start(c, 2159344);
                break;
            }
            case cannon_tuto_01: {
                c.getSession().write(UIPacket.IntroDisableUI(true));
                //     c.getSession().write(UIPacket.IntroEnableUI(1));
                //   c.getSession().write(UIPacket.getDirectionStatus(true));
                c.getPlayer().changeSkillLevel(SkillFactory.getSkill(110), (byte) 1, (byte) 1);
                //         c.getSession().write(UIPacket.getDirectionInfo(3, 0));
                //       c.getSession().write(UIPacket.getDirectionInfo(4, 1096000));
                NPCScriptManager.getInstance().dispose(c);
                NPCScriptManager.getInstance().start(c, 1096000);
                break;
            }
            case ds_tuto_5_0: {
                c.getSession().write(UIPacket.IntroDisableUI(true));
                //           c.getSession().write(UIPacket.IntroEnableUI(1));
                //              c.getSession().write(UIPacket.getDirectionStatus(true));
                //            c.getSession().write(UIPacket.getDirectionInfo(3, 0));
                //             c.getSession().write(UIPacket.getDirectionInfo(4, 2159314));
                NPCScriptManager.getInstance().dispose(c);
                NPCScriptManager.getInstance().start(c, 2159314);
                break;
            }
            case ds_tuto_3_0: {
                //            c.getSession().write(UIPacket.getDirectionInfo(3, 1));
                //            c.getSession().write(UIPacket.getDirectionInfo(1, 30));
                //           c.getSession().write(UIPacket.getDirectionStatus(true));
                c.getSession().write(MaplePacketCreator.showEffect("demonSlayer/text12"));

                EventTimer.getInstance().schedule(new Runnable() {

                    public void run() {
                        //                   c.getSession().write(UIPacket.getDirectionInfo(3, 0));
                        //                   c.getSession().write(UIPacket.getDirectionInfo(4, 2159311));
                        NPCScriptManager.getInstance().dispose(c);
                        NPCScriptManager.getInstance().start(c, 2159311);
                    }
                }, 1000);
                break;
            }
            case ds_tuto_3_1: {
                c.getSession().write(UIPacket.IntroDisableUI(true));
                //          c.getSession().write(UIPacket.IntroEnableUI(1));
                //           c.getSession().write(UIPacket.getDirectionStatus(true));
                if (!c.getPlayer().getMap().containsNPC(2159340)) {
                    c.getPlayer().getMap().spawnNpc(2159340, new Point(175, 0));
                    c.getPlayer().getMap().spawnNpc(2159341, new Point(300, 0));
                    c.getPlayer().getMap().spawnNpc(2159342, new Point(600, 0));
                }
                //         c.getSession().write(UIPacket.getDirectionInfo("Effect/Direction5.img/effect/tuto/balloonMsg2/0", 2000, 0, -100, 1));
                //         c.getSession().write(UIPacket.getDirectionInfo("Effect/Direction5.img/effect/tuto/balloonMsg1/3", 2000, 0, -100, 1));
                EventTimer.getInstance().schedule(new Runnable() {

                    public void run() {
                        //                 c.getSession().write(UIPacket.getDirectionInfo(3, 0));
                        //                 c.getSession().write(UIPacket.getDirectionInfo(4, 2159340));
                        NPCScriptManager.getInstance().dispose(c);
                        NPCScriptManager.getInstance().start(c, 2159340);
                    }
                }, 1000);
                break;
            }
            case ds_tuto_2_before: {
                //       c.getSession().write(UIPacket.IntroEnableUI(1));
                //       c.getSession().write(UIPacket.getDirectionInfo(3, 1));
                //       c.getSession().write(UIPacket.getDirectionInfo(1, 30));
                //       c.getSession().write(UIPacket.getDirectionStatus(true));
                EventTimer.getInstance().schedule(new Runnable() {

                    public void run() {
                        //                 c.getSession().write(UIPacket.getDirectionInfo(3, 0));
                        c.getSession().write(MaplePacketCreator.showEffect("demonSlayer/text13"));
                        //               c.getSession().write(UIPacket.getDirectionInfo(1, 500));
                    }
                }, 1000);
                EventTimer.getInstance().schedule(new Runnable() {

                    public void run() {
                        c.getSession().write(MaplePacketCreator.showEffect("demonSlayer/text14"));
                        //                c.getSession().write(UIPacket.getDirectionInfo(1, 4000));
                    }
                }, 1500);
                EventTimer.getInstance().schedule(new Runnable() {

                    public void run() {
                        final MapleMap mapto = c.getChannelServer().getMapFactory().getMap(927000020);
                        c.getPlayer().changeMap(mapto, mapto.getPortal(0));
                        //                c.getSession().write(UIPacket.IntroEnableUI(0));
                        MapleQuest.getInstance(23204).forceStart(c.getPlayer(), 0, null);
                        MapleQuest.getInstance(23205).forceComplete(c.getPlayer(), 0);
                        c.getPlayer().changeSkillLevel(SkillFactory.getSkill(30011170), (byte) 1, (byte) 1);
                        c.getPlayer().changeSkillLevel(SkillFactory.getSkill(30011169), (byte) 1, (byte) 1);
                        c.getPlayer().changeSkillLevel(SkillFactory.getSkill(30011168), (byte) 1, (byte) 1);
                        c.getPlayer().changeSkillLevel(SkillFactory.getSkill(30011167), (byte) 1, (byte) 1);
                        c.getPlayer().changeSkillLevel(SkillFactory.getSkill(30010166), (byte) 1, (byte) 1);
                    }
                }, 5500);
                break;
            }
            case ds_tuto_1_before: {
                //        c.getSession().write(UIPacket.getDirectionInfo(3, 1));
                //        c.getSession().write(UIPacket.getDirectionInfo(1, 30));
                //        c.getSession().write(UIPacket.getDirectionStatus(true));
                EventTimer.getInstance().schedule(new Runnable() {

                    public void run() {
                        //               c.getSession().write(UIPacket.getDirectionInfo(3, 0));
                        c.getSession().write(MaplePacketCreator.showEffect("demonSlayer/text8"));
                        //               c.getSession().write(UIPacket.getDirectionInfo(1, 500));
                    }
                }, 1000);
                EventTimer.getInstance().schedule(new Runnable() {

                    public void run() {
                        c.getSession().write(MaplePacketCreator.showEffect("demonSlayer/text9"));
                        //             c.getSession().write(UIPacket.getDirectionInfo(1, 3000));
                    }
                }, 1500);
                EventTimer.getInstance().schedule(new Runnable() {

                    public void run() {
                        final MapleMap mapto = c.getChannelServer().getMapFactory().getMap(927000010);
                        c.getPlayer().changeMap(mapto, mapto.getPortal(0));
                    }
                }, 4500);
                break;
            }
            case ds_tuto_0_0: {
                //            c.getSession().write(UIPacket.getDirectionStatus(true));
                //            c.getSession().write(UIPacket.IntroEnableUI(1));
                c.getSession().write(UIPacket.IntroDisableUI(true));

                c.getPlayer().changeSkillLevel(SkillFactory.getSkill(30011109), (byte) 1, (byte) 1);
                c.getPlayer().changeSkillLevel(SkillFactory.getSkill(30010110), (byte) 1, (byte) 1);
                c.getPlayer().changeSkillLevel(SkillFactory.getSkill(30010111), (byte) 1, (byte) 1);
                c.getPlayer().changeSkillLevel(SkillFactory.getSkill(30010185), (byte) 1, (byte) 1);
                //           c.getSession().write(UIPacket.getDirectionInfo(3, 0));
                c.getSession().write(MaplePacketCreator.showEffect("demonSlayer/back"));
                c.getSession().write(MaplePacketCreator.showEffect("demonSlayer/text0"));
                //           c.getSession().write(UIPacket.getDirectionInfo(1, 500));
                c.getPlayer().setDirection(0);
                if (!c.getPlayer().getMap().containsNPC(2159307)) {
                    c.getPlayer().getMap().spawnNpc(2159307, new Point(1305, 50));
                }
                break;
            }
            case ds_tuto_2_prep: {
                if (!c.getPlayer().getMap().containsNPC(2159309)) {
                    c.getPlayer().getMap().spawnNpc(2159309, new Point(550, 50));
                }
                break;
            }
            case goArcher: {
                showIntro(c, "Effect/Direction3.img/archer/Scene" + (c.getPlayer().getGender() == 0 ? "0" : "1"));
                break;
            }
            case goPirate: {
                showIntro(c, "Effect/Direction3.img/pirate/Scene" + (c.getPlayer().getGender() == 0 ? "0" : "1"));
                break;
            }
            case goRogue: {
                showIntro(c, "Effect/Direction3.img/rogue/Scene" + (c.getPlayer().getGender() == 0 ? "0" : "1"));
                break;
            }
            case goMagician: {
                showIntro(c, "Effect/Direction3.img/magician/Scene" + (c.getPlayer().getGender() == 0 ? "0" : "1"));
                break;
            }
            case goSwordman: {
                showIntro(c, "Effect/Direction3.img/swordman/Scene" + (c.getPlayer().getGender() == 0 ? "0" : "1"));
                break;
            }
            case goLith: {
                showIntro(c, "Effect/Direction3.img/goLith/Scene" + (c.getPlayer().getGender() == 0 ? "0" : "1"));
                break;
            }
            case TD_MC_Openning: {
                showIntro(c, "Effect/Direction2.img/open/back0");
                showIntro(c, "Effect/Direction2.img/open/back1");
                showIntro(c, "Effect/Direction2.img/open/chat");
                showIntro(c, "Effect/Direction2.img/open/frame");
                showIntro(c, "Effect/Direction2.img/open/line");
                showIntro(c, "Effect/Direction2.img/open/out");
                showIntro(c, "Effect/Direction2.img/open/pepeKing");
                showIntro(c, "Effect/Direction2.img/open/violeta0");
                showIntro(c, "Effect/Direction2.img/open/violeta1");
                break;
            }
            case TD_MC_gasi: {
                showIntro(c, "Effect/Direction2.img/gasi/gasi1");
                showIntro(c, "Effect/Direction2.img/gasi/gasi2");
                showIntro(c, "Effect/Direction2.img/gasi/gasi22");
                showIntro(c, "Effect/Direction2.img/gasi/gasi3");
                showIntro(c, "Effect/Direction2.img/gasi/gasi4");
                showIntro(c, "Effect/Direction2.img/gasi/gasi5");
                showIntro(c, "Effect/Direction2.img/gasi/gasi6");
                showIntro(c, "Effect/Direction2.img/gasi/gasi7");
                showIntro(c, "Effect/Direction2.img/gasi/gasi8");
                break;
            }
            case aranDirection: {
                switch (c.getPlayer().getMapId()) {
                    case 914090010:
                        data = "Effect/Direction1.img/aranTutorial/Scene0";
                        break;
                    case 914090011:
                        data = "Effect/Direction1.img/aranTutorial/Scene1" + (c.getPlayer().getGender() == 0 ? "0" : "1");
                        break;
                    case 914090012:
                        data = "Effect/Direction1.img/aranTutorial/Scene2" + (c.getPlayer().getGender() == 0 ? "0" : "1");
                        break;
                    case 914090013:
                        data = "Effect/Direction1.img/aranTutorial/Scene3";
                        break;
                    case 914090100:
                        data = "Effect/Direction1.img/aranTutorial/HandedPoleArm" + (c.getPlayer().getGender() == 0 ? "0" : "1");
                        break;
                    case 914090200:
                        data = "Effect/Direction1.img/aranTutorial/Maha";
                        break;
                    case 914090201:
                        data = "Effect/Direction1.img/aranTutorial/PoleArm";
                        break;
                }
                showIntro(c, data);
                break;
            }
            case iceCave: {
                c.getPlayer().changeSkillLevel(SkillFactory.getSkill(20000014), (byte) -1, (byte) 0);
                c.getPlayer().changeSkillLevel(SkillFactory.getSkill(20000015), (byte) -1, (byte) 0);
                c.getPlayer().changeSkillLevel(SkillFactory.getSkill(20000016), (byte) -1, (byte) 0);
                c.getPlayer().changeSkillLevel(SkillFactory.getSkill(20000017), (byte) -1, (byte) 0);
                c.getPlayer().changeSkillLevel(SkillFactory.getSkill(20000018), (byte) -1, (byte) 0);
                c.getSession().write(UIPacket.ShowWZEffect("Effect/Direction1.img/aranTutorial/ClickLirin"));
                c.getSession().write(UIPacket.IntroDisableUI(false));
                c.getSession().write(UIPacket.IntroLock(false));
                c.getSession().write(MaplePacketCreator.enableActions());
                break;
            }        
            case q3143_clear:
                if (c.getPlayer().getQuestStatus(3143) == 1) {
                    if (c.getPlayer().getQuestNAdd(MapleQuest.getInstance(3143)).getCustomData() != "1") {
                        MapleQuest.getInstance(3143).forceStart(c.getPlayer(), 0, "1");
                    }
                }
                break;            
            case rienArrow: {
                if (c.getPlayer().getInfoQuest(21019).equals("miss=o;helper=clear")) {
                    c.getPlayer().updateInfoQuest(21019, "miss=o;arr=o;helper=clear");
                    c.getSession().write(UIPacket.AranTutInstructionalBalloon("Effect/OnUserEff.img/guideEffect/aranTutorial/tutorialArrow3"));
                }
                break;
            }
            case rien: {
                if (c.getPlayer().getQuestStatus(21101) == 2 && c.getPlayer().getInfoQuest(21019).equals("miss=o;arr=o;helper=clear")) {
                    c.getPlayer().updateInfoQuest(21019, "miss=o;arr=o;ck=1;helper=clear");
                }
                c.getSession().write(UIPacket.IntroDisableUI(false));
                c.getSession().write(UIPacket.IntroLock(false));
                break;
            }
            case check_count: {
                if (c.getPlayer().getMapId() == 950101010 && (!c.getPlayer().haveItem(4001433, 5) || c.getPlayer().getLevel() < 50)) { //ravana Map
                    final MapleMap mapp = c.getChannelServer().getMapFactory().getMap(950101100); //exit Map
                    c.getPlayer().changeMap(mapp, mapp.getPortal(0));
                }
                break;
            }
            case Massacre_first: { //sends a whole bunch of shit.
                if (c.getPlayer().getPyramidSubway() == null) {
                    c.getPlayer().setPyramidSubway(new Event_PyramidSubway(c.getPlayer()));
                }
                break;
            }
            case Massacre_result: { //clear, give exp, etc.
                if (c.getPlayer().getPyramidSubway() != null) {
                    c.getSession().write(MaplePacketCreator.showEffect("killing/clear"));
                } else {
                    c.getSession().write(MaplePacketCreator.showEffect("killing/fail"));
                }
                //left blank because pyramidsubway handles this.
                break;
            }
            default: {
                System.out.println("Unhandled script : " + scriptName + ", type : onUserEnter - MAPID " + c.getPlayer().getMapId());
                FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Unhandled script : " + scriptName + ", type : onUserEnter - MAPID " + c.getPlayer().getMapId());
                break;
            }
        }
    }

    private static final int getTiming(int ids) {
        if (ids <= 5) {
            return 5;
        } else if (ids >= 7 && ids <= 11) {
            return 6;
        } else if (ids >= 13 && ids <= 17) {
            return 7;
        } else if (ids >= 19 && ids <= 23) {
            return 8;
        } else if (ids >= 25 && ids <= 29) {
            return 9;
        } else if (ids >= 31 && ids <= 35) {
            return 10;
        } else if (ids >= 37 && ids <= 38) {
            return 15;
        }
        return 0;
    }

    private static final int getDojoStageDec(int ids) {
        if (ids <= 5) {
            return 0;
        } else if (ids >= 7 && ids <= 11) {
            return 1;
        } else if (ids >= 13 && ids <= 17) {
            return 2;
        } else if (ids >= 19 && ids <= 23) {
            return 3;
        } else if (ids >= 25 && ids <= 29) {
            return 4;
        } else if (ids >= 31 && ids <= 35) {
            return 5;
        } else if (ids >= 37 && ids <= 38) {
            return 6;
        }
        return 0;
    }

    private static void showIntro(final MapleClient c, final String data) {
        c.getSession().write(UIPacket.IntroDisableUI(true));
        c.getSession().write(UIPacket.IntroLock(true));
        c.getSession().write(UIPacket.ShowWZEffect(data));
    }

    private static void sendDojoClock(MapleClient c, int time) {
        c.getSession().write(MaplePacketCreator.getClock(time));
    }

    private static void sendDojoStart(MapleClient c, int stage) {
        c.getSession().write(MaplePacketCreator.environmentChange("Dojang/start", 4));
        c.getSession().write(MaplePacketCreator.environmentChange("dojang/start/stage", 3));
        c.getSession().write(MaplePacketCreator.environmentChange("dojang/start/number/" + stage, 3));
        c.getSession().write(MaplePacketCreator.trembleEffect(0, 1));
    }

    private static void handlePinkBeanStart(MapleClient c) {
        final MapleMap map = c.getPlayer().getMap();
       // map.resetFully();

//        if (!map.containsNPC(2141000)) {
//            map.spawnNpc(2141000, new Point(-190, -42));
//        }
    }

    private static void reloadWitchTower(MapleClient c) {
        final MapleMap map = c.getPlayer().getMap();
        map.killAllMonsters(false);

        final int level = c.getPlayer().getLevel();
        int mob;
        if (level <= 10) {
            mob = 9300367;
        } else if (level <= 20) {
            mob = 9300368;
        } else if (level <= 30) {
            mob = 9300369;
        } else if (level <= 40) {
            mob = 9300370;
        } else if (level <= 50) {
            mob = 9300371;
        } else if (level <= 60) {
            mob = 9300372;
        } else if (level <= 70) {
            mob = 9300373;
        } else if (level <= 80) {
            mob = 9300374;
        } else if (level <= 90) {
            mob = 9300375;
        } else if (level <= 100) {
            mob = 9300376;
        } else {
            mob = 9300377;
        }
        MapleMonster theMob = MapleLifeFactory.getMonster(mob);
        OverrideMonsterStats oms = new OverrideMonsterStats();
        oms.setOMp(theMob.getMobMaxMp());
        oms.setOExp(theMob.getMobExp());
        oms.setOHp((long) Math.ceil(theMob.getMobMaxHp() * (level / 5.0))); //10k to 4m
        theMob.setOverrideStats(oms);
        map.spawnMonsterOnGroundBelow(theMob, witchTowerPos);
    }

    public static void startDirectionInfo(MapleCharacter chr, boolean start) {
        final MapleClient c = chr.getClient();
        DirectionInfo di = chr.getMap().getDirectionInfo(start ? 0 : chr.getDirection());
        if (di != null && di.eventQ.size() > 0) {
            if (start) {
                c.getSession().write(UIPacket.IntroDisableUI(true));
//                c.getSession().write(UIPacket.getDirectionInfo(3, 4));
            } else {
                for (String s : di.eventQ) {
                    switch (directionInfo.fromString(s)) {
                        case merTutorDrecotion01: //direction info: 1 is probably the time
                            //                     c.getSession().write(UIPacket.getDirectionInfo("Effect/Direction5.img/effect/mercedesInIce/merBalloon/0", 2000, 0, -100, 1));
                            break;
                        case merTutorDrecotion02:
                            //                    c.getSession().write(UIPacket.getDirectionInfo("Effect/Direction5.img/effect/mercedesInIce/merBalloon/1", 2000, 0, -100, 1));
                            break;
                        case merTutorDrecotion03:
                            //                      c.getSession().write(UIPacket.getDirectionInfo(3, 2));
                            //                     c.getSession().write(UIPacket.getDirectionStatus(true));
                            //                     c.getSession().write(UIPacket.getDirectionInfo("Effect/Direction5.img/effect/mercedesInIce/merBalloon/2", 2000, 0, -100, 1));
                            break;
                        case merTutorDrecotion04:
                            //                   c.getSession().write(UIPacket.getDirectionInfo(3, 2));
                            //                   c.getSession().write(UIPacket.getDirectionStatus(true));
                            //                   c.getSession().write(UIPacket.getDirectionInfo("Effect/Direction5.img/effect/mercedesInIce/merBalloon/3", 2000, 0, -100, 1));
                            break;
                        case merTutorDrecotion05:
                            //                c.getSession().write(UIPacket.getDirectionInfo(3, 2));
                            //                c.getSession().write(UIPacket.getDirectionStatus(true));
                            //               c.getSession().write(UIPacket.getDirectionInfo("Effect/Direction5.img/effect/mercedesInIce/merBalloon/4", 2000, 0, -100, 1));
                            EventTimer.getInstance().schedule(new Runnable() {

                                public void run() {
                                    //                       c.getSession().write(UIPacket.getDirectionInfo(3, 2));
                                    //                       c.getSession().write(UIPacket.getDirectionStatus(true));
                                    //                       c.getSession().write(UIPacket.getDirectionInfo("Effect/Direction5.img/effect/mercedesInIce/merBalloon/5", 2000, 0, -100, 1));
                                }
                            }, 2000);
                            EventTimer.getInstance().schedule(new Runnable() {

                                public void run() {
                                    //                      c.getSession().write(UIPacket.IntroEnableUI(0));
                                    c.getSession().write(MaplePacketCreator.enableActions());
                                }
                            }, 4000);
                            break;
                        case merTutorDrecotion12:
                            //            c.getSession().write(UIPacket.getDirectionInfo(3, 2));
                            //            c.getSession().write(UIPacket.getDirectionStatus(true));
                            //            c.getSession().write(UIPacket.getDirectionInfo("Effect/Direction5.img/effect/mercedesInIce/merBalloon/8", 2000, 0, -100, 1));
                            //            c.getSession().write(UIPacket.IntroEnableUI(0));
                            break;
                        case merTutorDrecotion21:
                            //                       c.getSession().write(UIPacket.getDirectionInfo(3, 1));
                            //                       c.getSession().write(UIPacket.getDirectionStatus(true));
                            MapleMap mapto = c.getChannelServer().getMapFactory().getMap(910150005);
                            c.getPlayer().changeMap(mapto, mapto.getPortal(0));
                            break;
                        case ds_tuto_0_2:
                            c.getSession().write(MaplePacketCreator.showEffect("demonSlayer/text1"));
                            break;
                        case ds_tuto_0_1:
                            //                       c.getSession().write(UIPacket.getDirectionInfo(3, 2));
                            break;
                        case ds_tuto_0_3:
                            c.getSession().write(MaplePacketCreator.showEffect("demonSlayer/text2"));
                            EventTimer.getInstance().schedule(new Runnable() {

                                public void run() {
                                    //                                 c.getSession().write(UIPacket.getDirectionInfo(1, 4000));
                                    c.getSession().write(MaplePacketCreator.showEffect("demonSlayer/text3"));
                                }
                            }, 2000);
                            EventTimer.getInstance().schedule(new Runnable() {

                                public void run() {
                                    //                                c.getSession().write(UIPacket.getDirectionInfo(1, 500));
                                    c.getSession().write(MaplePacketCreator.showEffect("demonSlayer/text4"));
                                }
                            }, 6000);
                            EventTimer.getInstance().schedule(new Runnable() {

                                public void run() {
                                    //                                c.getSession().write(UIPacket.getDirectionInfo(1, 4000));
                                    c.getSession().write(MaplePacketCreator.showEffect("demonSlayer/text5"));
                                }
                            }, 6500);
                            EventTimer.getInstance().schedule(new Runnable() {

                                public void run() {
                                    //                                c.getSession().write(UIPacket.getDirectionInfo(1, 500));
                                    c.getSession().write(MaplePacketCreator.showEffect("demonSlayer/text6"));
                                }
                            }, 10500);
                            EventTimer.getInstance().schedule(new Runnable() {

                                public void run() {
                                    //                               c.getSession().write(UIPacket.getDirectionInfo(1, 4000));
                                    c.getSession().write(MaplePacketCreator.showEffect("demonSlayer/text7"));
                                }
                            }, 11000);
                            EventTimer.getInstance().schedule(new Runnable() {

                                public void run() {
                                    //                               c.getSession().write(UIPacket.getDirectionInfo(4, 2159307));
                                    NPCScriptManager.getInstance().dispose(c);
                                    NPCScriptManager.getInstance().start(c, 2159307);
                                }
                            }, 15000);
                            break;
                    }
                }
            }
//            c.getSession().write(UIPacket.getDirectionInfo(1, 2000));
            chr.setDirection(chr.getDirection() + 1);
            if (chr.getMap().getDirectionInfo(chr.getDirection()) == null) {
                chr.setDirection(-1);
            }
        } else if (start) {
            switch (chr.getMapId()) {
                //hack
                case 931050300:
                    while (chr.getLevel() < 10) {
                        chr.levelUp();
                    }
                    final MapleMap mapto = c.getChannelServer().getMapFactory().getMap(931050000);
                    chr.changeMap(mapto, mapto.getPortal(0));
                    break;
            }
        }
    }
}
