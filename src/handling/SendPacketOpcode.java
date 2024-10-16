package handling;

public enum SendPacketOpcode {
//핑

    PING(9),
    // LOGIN
    LOGIN_STATUS(0),
    LOGIN_SECOND(1),
    SERVERLIST(2),
    CHARLIST(3),
    SERVER_IP(4),
    CHAR_NAME_RESPONSE(5),
    ADD_NEW_CHAR_ENTRY(6),
    DELETE_CHAR_RESPONSE(7),
    CHANGE_CHANNEL(8),
    PRIMIUM(9),
    CS_USE(10),
    SECONDPW_ERROR(13),//OnEnableSPWResult    
    ENABLE_RECOMMENDED(16),
    SEND_RECOMMENDED(17),//월드 노란색 테두리
    CHECK_SPW(18),//OnCheckSPWResult
    // +2
    MODIFY_INVENTORY_ITEM(19),
    UPDATE_INVENTORY_SLOT(20),
    UPDATE_STATS(21),
    GIVE_BUFF(22),
    CANCEL_BUFF(23),
    TEMP_STATS(24),
    TEMP_STATS_RESET(25),
    UPDATE_SKILLS(26),
    FAME_RESPONSE(28),
    SHOW_STATUS_INFO(29),
    SHOW_NOTES(30),
    TROCK_LOCATIONS(31),
    ANTI_MACRO(34),
    UPDATE_MOUNT(37),
    SHOW_QUEST_COMPLETION(38),
    SEND_TITLE_BOX(39),
    USE_SKILL_BOOK(40),
    SP_RESET(41),
    FINISH_SORT(42), //Sort - 1 = Gather
    FINISH_GATHER(43), //OnCharacterInfo - 3 = Sort
    CHAR_INFO(46),
    PARTY_OPERATION(47),
    EXPEDITION_OPERATION(49),
    BUDDYLIST(50),
    GUILD_OPERATION(52),
    ALLIANCE_OPERATION(53),
    SPAWN_PORTAL(54),
    MECH_PORTAL(55),
    SERVERMESSAGE(56),
    PIGMI_REWARD(57),
    OWL_OF_MINERVA(58),
    XMAS_SURPRISE(0x1BD),    
    ENGAGE_REQUEST(60),
    ENGAGE_RESULT(61),
    // 여기까지 +2
    WEDDING_GIFT(62),
    YELLOW_CHAT(65),
    SHOP_DISCOUNT(66),
    CATCH_MOB(67),
    PLAYER_NPC(69),
    GET_CARD(71),
    BOOK_STATS(72),
    SESSION_VALUE(78),
    PARTY_VALUE(79),
    MAP_VALUE(80),
    FAIRY_PEND_MSG(81),
    SEND_PEDIGREE(82),
    OPEN_FAMILY(83),
    FAMILY_MESSAGE(84),
    FAMILY_INVITE(85),
    FAMILY_JUNIOR(86),
    SENIOR_MESSAGE(87),
    FAMILY(88),
    REP_INCREASE(89),
    FAMILY_LOGGEDIN(90),
    FAMILY_BUFF(91),
    FAMILY_USE_REQUEST(92),
    LEVEL_UPDATE(93),
    MARRIAGE_UPDATE(94),
    JOB_UPDATE(95),
    PENDANT_SLOT(96),
    FOLLOW_REQUEST(97),
    TOP_MSG(98), //맞음
    MID_MSG(99),// 다름
    CLEAR_MID_MSG(100), //얘도
    CLAIM_MESSAGE(34),
    CLAIM_TIME(35),
    CLAIM_SERVER(36),
    // 여기까지 변동 없음
    // 삭제된 부분
    UPDATE_JAGUAR(101),
    ULTIMATE_EXPLORER(-1),
    PAM_SONG(-1),
    GM_POLICE(0xFF),
    PROFESSION_INFO(-1),
    // -5
    SKILL_MACRO(102),
    WARP_TO_MAP(103),
    CS_OPEN(104),
    //2
    LOGIN_WELCOME(105),
    //2
    SERVER_BLOCKED(107),
    PVP_BLOCKED(-1),
    //2
    SHOW_EQUIP_EFFECT(108),
    MULTICHAT(111),
    WHISPER(112),
    UPDATE_ENV(113),
    BOSS_ENV(114),
    MAP_EFFECT(115),
    // 여기까지 -5
    // -6
    CASH_SONG(116),
    GM_EFFECT(117),
    OX_QUIZ(118),
    GMEVENT_INSTRUCTIONS(119),
    CLOCK(120),
    BOAT_EFF(121),
    BOAT_EFFECT(122),
    STOP_CLOCK(126),
    PYRAMID_UPDATE(129),//여기서부터
    PYRAMID_RESULT(130),
    MOVE_PLATFORM(132),
    QUICK_SLOT(131), //요까지 이상함

    SPAWN_PLAYER(135),
    REMOVE_PLAYER_FROM_MAP(136),
    CHATTEXT(137),
    CHALKBOARD(138),
    UPDATE_CHAR_BOX(139),
    SHOW_SCROLL_EFFECT(141),
    SHOW_HYPER_SCROLL_EFFECT(142),
    SHOW_POTENTIAL_RESET(144), //햇갈림 142번은 하이퍼 업그레이드 이펙트
    SHOW_POTENTIAL_EFFECT(145),
    PVP_ATTACK(-1),
    PVP_MIST(-1),
    PVP_COOL(-1),
    CAPTURE_FLAGS(0x19F),
    CAPTURE_POSITION(0x1A0),
    CAPTURE_RESET(0x1A1),
    CRAFT_EFFECT(-1),
    CRAFT_COMPLETE(-1),
    TESLA_TRIANGLE(147),
    FOLLOW_EFFECT(148), //148 pqreward
    SHOW_PQ_REWARD(149), //149 pqreward

    HARVESTED(-1),
    SPAWN_PET(150),
    MOVE_PET(151),
    PET_CHAT(152),
    PET_NAMECHANGE(153),
    PET_UPDATE(154),//1126 펫업데이트 패킷
    PET_COMMAND(155),
    PET_EXCEPTION_LIST(154),
    // -13

    DRAGON_SPAWN(156),
    DRAGON_MOVE(157),
    DRAGON_REMOVE(158),
    ANDROID_SPAWN(-1),
    ANDROID_MOVE(-1),
    ANDROID_EMOTION(-1),
    ANDROID_REMOVE(-1),
    ANDROID_DEACTIVATED(-1),
    // -18
    MOVE_PLAYER(160),
    CLOSE_RANGE_ATTACK(161),
    RANGED_ATTACK(162),
    MAGIC_ATTACK(163),
    ENERGY_ATTACK(164),
    SKILL_EFFECT(165),//166? 낫코디드
    CANCEL_SKILL_EFFECT(167),
    DAMAGE_PLAYER(168),
    FACIAL_EXPRESSION(169),
    SHOW_ITEM_EFFECT(170),
    SHOW_WHEEL(171),//1126 운수이펙트
    // 169 뭔진 모르겠지만 구현 해봄
    SHOW_CHAIR(172),
    UPDATE_CHAR_LOOK(173),//180 폭탄
    // +3
    SHOW_FOREIGN_EFFECT(174), //완료
    GIVE_FOREIGN_BUFF(175),
    CANCEL_FOREIGN_BUFF(176),
    UPDATE_PARTYMEMBER_HP(177),
    LOAD_GUILD_NAME(178),
    LOAD_GUILD_ICON(179),
    LOAD_TEAM(-1),
    SHOW_HARVEST(-1),
    SHOW_BOMB(178),//??
    CANCEL_CHAIR(181),
    SHOW_ITEM_GAIN_INCHAT(183),
    CURRENT_MAP_WARP(184),
    //2
    MESOBAG_SUCCESS(186),
    MESOBAG_FAILURE(187),
    UPDATE_QUEST_INFO(188),
    BUFF_BAR(189),//???
    PET_FLAG_CHANGE(190),
    PLAYER_HINT(191),
    //4
    REPAIR_WINDOW(198),
    CYGNUS_INTRO_LOCK(199),
    CYGNUS_INTRO_DISABLE_UI(200),
    SUMMON_HINT(201),
    SUMMON_HINT_MSG(202),
    ARAN_COMBO(203),//214까지 돈노
    ARAN_COMBO_RECHARGE(-1),
    GAME_POLL_REPLY(-1),
    FOLLOW_MOVE(211),
    FOLLOW_MSG(212),//낫슈어

    COOLDOWN(215),
    SPAWN_SUMMON(217),
    REMOVE_SUMMON(218),
    MOVE_SUMMON(219),
    SUMMON_ATTACK(220),//별도

    PVP_SUMMON(-1),
    SUMMON_SKILL(221),
    DAMAGE_SUMMON(222),
    GAME_POLL_QUESTION(-1),
    CREATE_ULTIMATE(-1),
    HARVEST_MESSAGE(-1),
    OPEN_BAG(-1),
    DRAGON_BLINK(-1),
    SPAWN_MONSTER(223),
    KILL_MONSTER(224),
    SPAWN_MONSTER_CONTROL(225),
    MOVE_MONSTER(226),
    MOVE_MONSTER_RESPONSE(227),
    APPLY_MONSTER_STATUS(229),
    CANCEL_MONSTER_STATUS(230),
    DAMAGE_MONSTER(233),
    MONSTER_BOMB(234),
    SHOW_MONSTER_HP(237),
    SHOW_MAGNET(238),
    CATCH_MONSTER(239),
    MOB_SKILL_DELAY(241),// 
    MONSTER_PROPERTIES(242),
    REMOVE_TALK_MONSTER(243),
    TALK_MONSTER(244),//낫슈어
    SPAWN_NPC(249),
    REMOVE_NPC(250),
    SPAWN_NPC_REQUEST_CONTROLLER(251),
    NPC_ACTION(252),
    NPC_SPECIAL_ACTION(254),
    NPC_SCRIPTABLE(255),
    SPAWN_HIRED_MERCHANT(257),
    DESTROY_HIRED_MERCHANT(258),
    UPDATE_HIRED_MERCHANT(259),
    DROP_ITEM_FROM_MAPOBJECT(260),
    REMOVE_ITEM_FROM_MAP(261),
    FAIL_SPAWN_MEESAGEBOX(262),
    SPAWN_MESSAGEBOX(263),
    DESTROY_MESSAGEBOX(264),
    SPAWN_MIST(265),
    REMOVE_MIST(266),
    SPAWN_DOOR(267),
    REMOVE_DOOR(268),
    MECH_DOOR_SPAWN(269),
    MECH_DOOR_REMOVE(270),
    
    REACTOR_HIT(271),
    REACTOR_SPAWN(273),
    REACTOR_DESTROY(274),
    ROLL_SNOWBALL(275),
    HIT_SNOWBALL(276),
    SNOWBALL_MESSAGE(277),
    LEFT_KNOCK_BACK(278),
    HIT_COCONUT(279),
    COCONUT_SCORE(280),
    MONSTER_CARNIVAL_START(283),
    MONSTER_CARNIVAL_OBTAINED_CP(284),
    MONSTER_CARNIVAL_PARTY_CP(285),
    MONSTER_CARNIVAL_SUMMON(286),
    MONSTER_CARNIVAL_DIED(287),
    CHAOS_HORNTAIL_SHRINE(272),//니ㅏㅅ슈어
    CHAOS_ZAKUM_SHRINE(272),
    NPC_TALK(299),
    OPEN_NPC_SHOP(300),
    CONFIRM_SHOP_TRANSACTION(301),
    OPEN_STORAGE(304),
    MERCH_ITEM_MSG(305),
    MERCH_ITEM_STORE(306),
    RPS_GAME(307),
    MESSENGER(308),
    PLAYER_INTERACTION(309),
    DUEY(317),
    CS_UPDATE(319),
    CS_OPERATION(320),
    KEYMAP(324),
    PET_AUTO_HP(325),
    PET_AUTO_MP(326),
    VICIOUS_HAMMER(335); //끝난듯?

    private int code = -2;

    private SendPacketOpcode(int code) {
        this.code = code;
    }
    
        public static String getOpcodeName(int value) {

        for (SendPacketOpcode opcode : values()) {
            if (opcode.getValue() == value) {
                return opcode.name();
            }
        }
        return "UNKNOWN";
    }

    public int getValue() {
        return code;
    }
}