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
package handling;

public enum RecvPacketOpcode {
    // GENERIC

    CONNECTION_CLIENT(9734),
    PONG(11),
    CLAIM_SUCCESS(99),
    LOGIN_PASSWORD(1),
    CHARLIST_REQUEST(4),
    CHAR_SELECT(5),
    PLAYER_LOGGEDIN(6),
    CHECK_CHAR_NAME(7),
    CREATE_CHAR(8),
    CREATE_ULTIMATE(-1),
    DELETE_CHAR(10),
    AUTH_SECOND_PASSWORD(14),
    CHAR_SELECT_WITSECONDPW(15),
    CLIENT_ERROR(12),
    CHANGE_MAP(21),
    CHANGE_CHANNEL(22),
    ENTER_CASH_SHOP(23),
    MOVE_PLAYER(24),
    CANCEL_CHAIR(25),
    USE_CHAIR(26),
    CLOSE_RANGE_ATTACK(27),
    RANGED_ATTACK(28),
    MAGIC_ATTACK(29),
    PASSIVE_ENERGY(30),
    TAKE_DAMAGE(32),
    PVP_ATTACK(-1),
    GENERAL_CHAT(34),
    CLOSE_CHALKBOARD(35),
    FACE_EXPRESSION(36),
    USE_ITEMEFFECT(37),
    WHEEL_OF_FORTUNE(0xFFFF),//1126 운수 멀티 이펙트
    NPC_TALK(43),
    HIRED_REMOTE(44),
    NPC_TALK_MORE(45),
    NPC_SHOP(46),
    STORAGE(47),
    USE_HIRED_MERCHANT(48),
    MERCH_ITEM_STORE(49),
    DUEY_ACTION(50),
    MECH_CANCEL(51),
    OWL(52),
    OWL_WARP(53),
    AdminShop(54),
    ITEM_SORT(0xFF), //56 확실
    ITEM_GATHER(55),
    ITEM_MOVE(57),
    MOVE_BAG(-1),
    SWITCH_BAG(-1),
    USE_ITEM(58),
    CANCEL_ITEM_EFFECT(59),
    USE_SUMMON_BAG(61),
    PET_FOOD(62),
    USE_MOUNT_FOOD(63),
    USE_SCRIPTED_NPC_ITEM(64),
    USE_RECIPE(-1),
    USE_CASH_ITEM(65),
    USE_CATCH_ITEM(67),
    USE_SKILL_BOOK(68),
    USE_SP_RESET_SCROLL(69),
    USE_OWL_MINERVA(70),
    USE_TELE_ROCK(71),
    USE_RETURN_SCROLL(72),
    USE_UPGRADE_SCROLL(73),
    USE_EQUIP_SCROLL(74),
    USE_POTENTIAL_SCROLL(75),
    USE_BAG(-1),
    USE_MAGNIFY_GLASS(77),
    DISTRIBUTE_AP(78),
    AUTO_ASSIGN_AP(79),
    HEAL_OVER_TIME(80),
    DISTRIBUTE_SP(82),
    SPECIAL_MOVE(83),
    CANCEL_BUFF(84),
    SKILL_EFFECT(85),
    MESO_DROP(86),
    GIVE_FAME(87),
    CHAR_INFO_REQUEST(89),
    SPAWN_PET(90),
    PET_AUTOBUFF(91),
    CANCEL_DEBUFF(92),
    CHANGE_MAP_SPECIAL(93),
    USE_INNER_PORTAL(94),
    TROCK_ADD_MAP(95),
    QUEST_ACTION(100),
    POISON_BOMB(102),
    SKILL_MACRO(103),
    REWARD_ITEM(105),
    ITEM_MAKER(106),
    REPAIR_ALL(107),
    REPAIR(108),
    FOLLOW_REQUEST(111),
    CHOOSE_PQREWARD(113),
    AUTO_FOLLOW_REPLY(114),
    FOLLOW_REPLY(115),
    PARTYCHAT(117),
    WHISPER(118),
    MESSENGER(119),
    PLAYER_INTERACTION(120),
    PARTY_OPERATION(121),
    DENY_PARTY_REQUEST(122),
    EXPEDITION_OPERATION(123),
    EXPEDITION_LISTING(124),
    GUILD_OPERATION(125),
    DENY_GUILD_REQUEST(126),
    BUDDYLIST_MODIFY(129),
    NOTE_ACTION(131),
    USE_DOOR(132),
    USE_MECH_DOOR(133),
    CHANGE_KEYMAP(135),//여기까진 확실한듯
    RPS_GAME(136),
    RING_ACTION(137),
    ALLIANCE_OPERATION(142),
    DENY_ALLIANCE_REQUEST(143),
    REQUEST_FAMILY(144),//여기까지도
    OPEN_FAMILY(145),
    FAMILY_OPERATION(146),
    DELETE_JUNIOR(147),
    DELETE_SENIOR(148),
    ACCEPT_FAMILY(149),
    USE_FAMILY(150),
    FAMILY_PRECEPT(151),
    FAMILY_SUMMON(152),
    CYGNUS_SUMMON(153),
    ARAN_COMBO(154),
    BBS_OPERATION(144),//모름
    TRANSFORM_PLAYER(145),//모름
    MOVE_PET(158),
    PET_CHAT(159),
    PET_COMMAND(160),
    PET_LOOT(161),
    PET_AUTO_POT(162),
    PET_EXCEPTION_LIST(163),
    MOVE_SUMMON(166),
    SUMMON_ATTACK(167),
    DAMAGE_SUMMON(168),
    SUB_SUMMON(169),
    REMOVE_SUMMON(170),
    MOVE_DRAGON(173),
    QUICK_SLOT(175),
    MECH_CANCEL2(176),
    PAM_SONG(999),//모름
    MOVE_LIFE(182),
    AUTO_AGGRO(183),
    FRIENDLY_DAMAGE(186),//월묘,호브등 맞는거
    MONSTER_BOMB(187),//다크스타
    HYPNOTIZE_DMG(188), // 모름
    MOB_SKILL_DELAY_END(189),
    MOB_BOMB(190), // 듀블
    MOB_NODE(191), // 모름
    DISPLAY_NODE(192), // 모름
    NPC_ACTION(196),
    ITEM_PICKUP(201),
    DAMAGE_REACTOR(204),
    TOUCH_REACTOR(205),
    MAKE_EXTRACTOR(206),
    SNOWBALL(208),
    LEFT_KNOCK_BACK(209),
    COCONUT(210),
    MONSTER_CARNIVAL(215),
    SHIP_OBJECT(217),
    PARTY_SEARCH_START(219),
    PARTY_SEARCH_STOP(220),
    CS_UPDATE(227),
    BUY_CS_ITEM(228),
    COUPON_CODE(229),
    GIFT_CS_ITEM(231),
    UPDATE_QUEST(224),
    QUEST_ITEM(225),
    USE_ITEM_QUEST(226),
    // 나중에 맞출예정
    USE_GOLD_HAMMER(0xF8),//완료
    USED_GOLD_HAMMER(0xF9),//완료
    QUEST_POT_OPEN(237),
    QUEST_POT(238),
    QUEST_POT_FEED(239),    
    TOUCHING_MTS(0x159),
    CHANGE_SET(0x7FFE),// #npc_talk -4
    GET_BOOK_INFO(0x7FFE),// #char_info_request +2
    REISSUE_MEDAL(0x7FFEE),
    CLICK_REACTOR(0x7FFE),
    RSA_KEY(9734);// ??이 거봐야할듯
    private int code = -2;

    public final int getValue() {
        return code;
    }

    private RecvPacketOpcode(int code) {
        this.code = code;
    }

    public static String getOpcodeName(int value) {

        for (RecvPacketOpcode opcode : values()) {
            if (opcode.getValue() == value) {
                return opcode.name();
            }
        }
        return "UNKNOWN";
    }
}
