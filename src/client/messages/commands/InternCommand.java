package client.messages.commands;

import client.MapleCharacter;
import server.Randomizer;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.MapleStat;
import client.Skill;
import client.SkillFactory;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import constants.ServerConstants.PlayerGMRank;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.world.World;
import java.awt.Point;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import provider.MapleDataTool;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import server.ItemInformation;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MaplePortal;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.quest.MapleQuest;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.StringUtil;

public class InternCommand {

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.INTERN;
    }
    
    public static class 명령어 extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(5, "암허스트 경찰 도우미 여러분께 감사 말씀 드립니다.");
            c.getPlayer().dropMessage(5, "핵유저 리패커 수정유저 발견시 디엠주세요!");
            c.getPlayer().dropMessage(5, "!이동 닉네임 : 해당 유저에게 이동");
            c.getPlayer().dropMessage(5, "!연결 : 전체채널 플레이유저 확인");
            c.getPlayer().dropMessage(5, "!온라인 : 현재채널 플레이유저 확인");
            c.getPlayer().dropMessage(5, "!캐릭터정보 닉네임 : 해당 캐릭터 정보확인가능 ");
            c.getPlayer().dropMessage(5, "!거탐 닉네임 : 핵 유저 발견시 거탐적용");
            c.getPlayer().dropMessage(5, "!검색 아이템 이름 : 검색만 가능합니다. ");
            
            return 1;
        }
    }    
    
    public static class 거탐 extends CommandExecute { //영자거탐 !거탐 유저닉네임 심심할때 쓰면 재밌음 ㄷㄷ 감사합니다ㅠ

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length != 2) {
                c.getPlayer().dropMessage(6, "사용법: !거탐 유저닉네임");
                return 0;
            }

            String targetUsername = splitted[1];
            MapleCharacter targetUser = ChannelServer.getInstance(c.getChannel()).getPlayerStorage().getCharacterByName(targetUsername);

            if (targetUser != null) {
                // 매크로 설정 로직
                String[] s1 = {"경찰", "경찰", "경찰"};
                String[] s2 = {"입니다", "입니다", "입니다", "입니다"};
                String[] s3 = {"걸렸어요", "걸렸어요", "걸렸어요", "걸렸어요", "걸렸어요"};
                int i1 = Randomizer.rand(0, s1.length - 1);
                int i2 = Randomizer.rand(0, s2.length - 1);
                int i3 = Randomizer.rand(0, s3.length - 1);
                targetUser.setMacroStr(s1[i1] + s2[i2] + s3[i3]);

                // 모든 채팅 유형에서 매크로 문자열을 인식하도록 설정
                targetUser.getMap().startMapEffect(targetUser.getName() + "님, 1분 내에 채팅으로 '" + targetUser.getMacroStr() + "'을 작성해주세요.", 5120008);
                targetUser.dropMessage(6, targetUser.getName() + "님, 1분 내에 채팅으로 '" + targetUser.getMacroStr() + "'을 작성해주세요.");
                targetUser.dropMessage(1, targetUser.getName() + "님, 1분 내에 채팅으로 '" + targetUser.getMacroStr() + "'을 작성해주세요.");
                targetUser.startMacro(90); // 매크로 지속 시간 설정

                c.getPlayer().dropMessage(6, targetUser.getName() + "에게 거탐 매크로를 설정했습니다.");
            } else {
                c.getPlayer().dropMessage(6, "해당 유저를 찾을 수 없습니다.");
            }

            return 1;
        }
    }

    public static class ID extends 검색 {
    }

    public static class LookUp extends 검색 {
    }

    public static class 찾기 extends 검색 {
    }

    public static class 검색 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length == 1) {
                c.getPlayer().dropMessage(6, splitted[0] + ": <엔피시> <몹> <아이템> <맵> <스킬> <퀘스트>");
            } else if (splitted.length == 2) {
                c.getPlayer().dropMessage(6, "제공된 코드으로만 입력이 가능합니다. 현재 가능한 코드는 <엔피시> <몹> <아이템> <맵> <스킬> <퀘스트> 입니다.");
            } else {
                String type = splitted[1];
                String search = StringUtil.joinStringFrom(splitted, 2);
                MapleData data = null;
                MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/" + "String.wz"));
                c.getPlayer().dropMessage(6, "<< 종류 : " + type + " | 검색어 : " + search + ">>");

                if (type.equalsIgnoreCase("엔피시")) {
                    List<String> retNpcs = new ArrayList<String>();
                    data = dataProvider.getData("Npc.img");
                    List<Pair<Integer, String>> npcPairList = new LinkedList<Pair<Integer, String>>();
                    for (MapleData npcIdData : data.getChildren()) {
                        npcPairList.add(new Pair<Integer, String>(Integer.parseInt(npcIdData.getName()), MapleDataTool.getString(npcIdData.getChildByPath("name"), "NO-NAME")));
                    }
                    for (Pair<Integer, String> npcPair : npcPairList) {
                        if (npcPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                            retNpcs.add(npcPair.getLeft() + " - " + npcPair.getRight());
                        }
                    }
                    if (retNpcs != null && retNpcs.size() > 0) {
                        for (String singleRetNpc : retNpcs) {
                            c.getPlayer().dropMessage(6, singleRetNpc);
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "검색된 엔피시가 없습니다.");
                    }

                } else if (type.equalsIgnoreCase("맵")) {
                    List<String> retMaps = new ArrayList<String>();
                    data = dataProvider.getData("Map.img");
                    List<Pair<Integer, String>> mapPairList = new LinkedList<Pair<Integer, String>>();
                    for (MapleData mapAreaData : data.getChildren()) {
                        for (MapleData mapIdData : mapAreaData.getChildren()) {
                            mapPairList.add(new Pair<Integer, String>(Integer.parseInt(mapIdData.getName()), MapleDataTool.getString(mapIdData.getChildByPath("streetName"), "NO-NAME") + " - " + MapleDataTool.getString(mapIdData.getChildByPath("mapName"), "NO-NAME")));
                        }
                    }
                    for (Pair<Integer, String> mapPair : mapPairList) {
                        if (mapPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                            retMaps.add(mapPair.getLeft() + " - " + mapPair.getRight());
                        }
                    }
                    if (retMaps != null && retMaps.size() > 0) {
                        for (String singleRetMap : retMaps) {
                            c.getPlayer().dropMessage(6, singleRetMap);
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "검색된 맵이 없습니다.");
                    }
                } else if (type.equalsIgnoreCase("몹")) {
                    List<String> retMobs = new ArrayList<String>();
                    data = dataProvider.getData("Mob.img");
                    List<Pair<Integer, String>> mobPairList = new LinkedList<Pair<Integer, String>>();
                    for (MapleData mobIdData : data.getChildren()) {
                        mobPairList.add(new Pair<Integer, String>(Integer.parseInt(mobIdData.getName()), MapleDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME")));
                    }
                    for (Pair<Integer, String> mobPair : mobPairList) {
                        if (mobPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                            retMobs.add(mobPair.getLeft() + " - " + mobPair.getRight());
                        }
                    }
                    if (retMobs != null && retMobs.size() > 0) {
                        for (String singleRetMob : retMobs) {
                            c.getPlayer().dropMessage(6, singleRetMob);
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "검색된 아이템이 없습니다.");
                    }
                } else if (type.equalsIgnoreCase("아이템")) {
                    List<String> retItems = new ArrayList<String>();
                    for (ItemInformation itemPair : MapleItemInformationProvider.getInstance().getAllItems()) {
                        if (itemPair != null && itemPair.name != null && itemPair.name.toLowerCase().contains(search.toLowerCase())) {
                            retItems.add(itemPair.itemId + " - " + itemPair.name);
                        }
                    }
                    if (retItems != null && retItems.size() > 0) {
                        for (String singleRetItem : retItems) {
                            c.getPlayer().dropMessage(6, singleRetItem);
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "검색된 아이템이 없습니다.");
                    }
                } else if (type.equalsIgnoreCase("퀘스트")) {
                    List<String> retItems = new ArrayList<String>();
                    for (MapleQuest itemPair : MapleQuest.getAllInstances()) {
                        if (itemPair.getName().length() > 0 && itemPair.getName().toLowerCase().contains(search.toLowerCase())) {
                            retItems.add(itemPair.getId() + " - " + itemPair.getName());
                        }
                    }
                    if (retItems != null && retItems.size() > 0) {
                        for (String singleRetItem : retItems) {
                            c.getPlayer().dropMessage(6, singleRetItem);
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "검색된 스킬이 없습니다.");
                    }
                } else if (type.equalsIgnoreCase("스킬")) {
                    List<String> retSkills = new ArrayList<String>();
                    for (Skill skil : SkillFactory.getAllSkills()) {
                        if (skil.getName() != null && skil.getName().toLowerCase().contains(search.toLowerCase())) {
                            retSkills.add(skil.getId() + " - " + skil.getName());
                        }
                    }
                    if (retSkills != null && retSkills.size() > 0) {
                        for (String singleRetSkill : retSkills) {
                            c.getPlayer().dropMessage(6, singleRetSkill);
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "검색된 스킬이 없습니다.");
                    }
                } else if (type.equalsIgnoreCase("헤어")) {
                    List<String> retHair = new ArrayList<String>();
                    List<Pair<Integer, String>> hairPairList = new LinkedList<Pair<Integer, String>>();
                    MapleDataProvider hairstring = MapleDataProviderFactory.getDataProvider(new File("wz/String.wz"));
                    MapleData hair = hairstring.getData("Eqp.img");
                    for (MapleData hairData : hair.getChildByPath("Eqp").getChildByPath("Hair")) {
                        hairPairList.add(new Pair<Integer, String>(Integer.parseInt(hairData.getName()), MapleDataTool.getString(hairData.getChildByPath("name"), "NO-NAME")));
                    }
                    for (Pair<Integer, String> hairPair : hairPairList) {
                        if (hairPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                            retHair.add(hairPair.getLeft() + " - " + hairPair.getRight());
                        }
                    }
                    if (retHair != null && retHair.size() > 0) {
                        for (String singleRetHair : retHair) {
                            c.getPlayer().dropMessage(6, singleRetHair);
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "검색된 헤어가 없습니다.");
                    }
                } else if (type.equalsIgnoreCase("얼굴") || type.equalsIgnoreCase("성형")) {
                    List<String> retface = new ArrayList<String>();
                    List<Pair<Integer, String>> facePairList = new LinkedList<Pair<Integer, String>>();
                    MapleDataProvider facestring = MapleDataProviderFactory.getDataProvider(new File("wz/String.wz"));
                    MapleData face = facestring.getData("Eqp.img");
                    for (MapleData faceData : face.getChildByPath("Eqp").getChildByPath("Face")) {
                        facePairList.add(new Pair<Integer, String>(Integer.parseInt(faceData.getName()), MapleDataTool.getString(faceData.getChildByPath("name"), "NO-NAME")));
                    }
                    for (Pair<Integer, String> facePair : facePairList) {
                        if (facePair.getRight().toLowerCase().contains(search.toLowerCase())) {
                            retface.add(facePair.getLeft() + " - " + facePair.getRight());
                        }
                    }
                    if (retface != null && retface.size() > 0) {
                        for (String singleRetface : retface) {
                            c.getPlayer().dropMessage(6, singleRetface);
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "검색된 성형이 없습니다.");
                    }
                } else {
                    c.getPlayer().dropMessage(6, "해당 검색은 처리할 수 없습니다.");
                }
            }
            return 0;
        }
    }

    public static class 이동 extends CommandExecute {
        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "사용법: !이동 <닉네임>");
                return 0;
            }

            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);

            if (victim != null) {
                if (splitted.length == 2) {
                    c.getPlayer().changeMap(victim.getMap(), victim.getMap().findClosestSpawnpoint(victim.getTruePosition()));
                } else {
                    MapleMap target = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(Integer.parseInt(splitted[2]));
                    if (target == null) {
                        c.getPlayer().dropMessage(6, "존재하지 않는 유저입니다.");
                        return 0;
                    }
                    MaplePortal targetPortal = null;
                    if (splitted.length > 3) {
                        try {
                            targetPortal = target.getPortal(Integer.parseInt(splitted[3]));
                        } catch (IndexOutOfBoundsException e) {
                            c.getPlayer().dropMessage(5, "포털 이동중입니다.");
                        } catch (NumberFormatException a) {
                        }
                    }
                    if (targetPortal == null) {
                        targetPortal = target.getPortal(0);
                    }
                    victim.changeMap(target, targetPortal);
                }
            } else {
                try {
                    int ch = World.Find.findChannel(splitted[1]);
                    if (ch < 0) {
                        c.getPlayer().dropMessage(6, "존재하지 않는 유저입니다.");
                        return 0;
                    } else {
                        victim = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(splitted[1]);
                        c.getPlayer().dropMessage(6, "채널 변경중 기다려주세요.");
                        
                        if (victim != null) {
                            if (victim.getMapId() != c.getPlayer().getMapId()) {
                                final MapleMap mapp = c.getChannelServer().getMapFactory().getMap(victim.getMapId());
                                c.getPlayer().changeMap(mapp, mapp.findClosestPortal(victim.getTruePosition()));
                            }
                            c.getPlayer().changeChannel(ch);
                        }
                    }
                } catch (Exception e) {
                    c.getPlayer().dropMessage(6, "오류가 발생했습니다. " + e.getMessage());
                    return 0;
                }
            }
            return 1;
        }
    }

    public static class 맵 extends CommandExecute {
        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "사용법: !맵 <맵코드>");
                return 0;
            }

        try {
            int mapId = Integer.parseInt(splitted[1]);

            if (!isMapAvailable(mapId)) {
                c.getPlayer().dropMessage(6, "해당 맵을 찾을 수 없습니다.");
                return 0;
            }

                MapleMap target = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(mapId);
                if (target != null) {
                    c.getPlayer().changeMap(target, target.getPortal(0));
                } else {
                    c.getPlayer().dropMessage(6, "해당 맵을 찾을 수 없습니다.");
                }
            } catch (NumberFormatException e) {
                c.getPlayer().dropMessage(6, "유효한 맵 코드를 입력해주세요.");
            }
            return 1;
        }
    }

    public static class 캐릭터정보 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            boolean isOnline = false;
            MapleCharacter player = null;

            for (int i = 1; i <= ChannelServer.getChannelCount(); i++) {
                for (MapleCharacter other : ChannelServer.getInstance(i).getPlayerStorage().getAllCharacters()) {
                    if (other != null && other.getName().equals(splitted[1])) {
                        other.saveToDB(false, false); //세이브
                        isOnline = true;
                        player = other;
                    }
                }
            }

            Connection con = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            StringBuilder text = new StringBuilder().append("#b" + splitted[1] + " #k님의 캐릭터 정보입니다.\r\n\r\n");

            try {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("SELECT * FROM characters c INNER JOIN accounts a ON c.accountid = a.id WHERE c.name = ?");
                ps.setString(1, splitted[1]);
                rs = ps.executeQuery();

                if (rs.next()) {
                    text.append("#b캐릭터 ID : #k" + rs.getInt("c.id") + " #b어카운트 ID : #k" + rs.getInt("a.id") + "\r\n\r\n");
                    text.append("#e캐릭터 스탯#n\r\n#b힘 : #k" + rs.getInt("c.str") + " #b덱 : #k" + rs.getInt("c.dex") + " #b인 : #k" + rs.getInt("c.int") + " #b럭 : #k" + rs.getInt("c.luk") + "\r\n");
                    text.append("#b최대 HP : #k" + rs.getInt("c.maxhp") + " #b최대 MP : #k" + rs.getInt("c.maxmp") + "\r\n");
                    text.append("#b현재 HP : #k" + rs.getInt("c.hp") + " #b현재 MP : #k" + rs.getInt("c.mp") + "\r\n\r\n");
                    if (isOnline) {
                        text.append("#b토탈 공격력 : #k" + player.getStat().getTotalWatk() + " #b토탈 마력 : #k" + player.getStat().getTotalMagic() + "\r\n");
                        text.append("#b토탈 힘 : #k" + player.getStat().getTotalStr() + " #b토탈 덱 : #k" + player.getStat().getTotalDex() + "\r\n");
                        text.append("#b토탈 인 : #k" + player.getStat().getTotalInt() + " #b토탈 럭 : #k" + player.getStat().getTotalLuk() + "\r\n\r\n");
                        //text.append("#b스공 : #k" + (int) player.getStat().getCurrentMinBaseDamage() + "~" + (int) player.getStat().getCurrentMaxBaseDamage() + "\r\n\r\n");
                    }
                    text.append("#b직업 : #k" + c.getPlayer().getJobName(rs.getInt("c.job")) + " #d #b직업코드 : #k" + rs.getInt("c.job") + "\r\n");
                    text.append("#b레벨 : #k" + rs.getInt("c.level") + " #b경험치 : #k" + rs.getInt("c.exp") + "\r\n");
                    text.append("#b헤어 : #k" + rs.getInt("c.hair") + " #b성형 : #k" + rs.getInt("c.face") + "\r\n\r\n");
                    text.append("#b소지 중인 후원포인트 : #k" + rs.getInt("DonateCash") + "\r\n");
                    text.append("#b소지 중인 캐시 : #k" + rs.getInt("ACash") + "\r\n");

                    text.append("#b소지 중인 메소 : #k" + c.getPlayer().getBanJum((long) rs.getInt("c.meso")) + "\r\n\r\n");

                    text.append("#b현재 맵 : #k" + c.getChannelServer().getMapFactory().getMap(rs.getInt("c.map")).getStreetName() + "-" + c.getChannelServer().getMapFactory().getMap(rs.getInt("c.map")).getMapName() + " (" + rs.getInt("c.map") + ")\r\n");

                    if (isOnline) {
                        text.append("#b캐릭터 좌표 : #k (#dX : " + player.getPosition().getX() + " Y : " + player.getPosition().getY() + "#k)\r\n");
                    }

                    if (rs.getInt("c.gm") > 0) {
                        text.append("#bGM : #k" + "권한 있음 (" + rs.getInt("c.gm") + " 레벨)\r\n");
                    } else {
                        text.append("#bGM : #k권한 없음\r\n");
                    }

                    String guild = "";
                    if (rs.getInt("c.guildid") == 0) {
                        guild = "없음";
                    } else {
                        guild = World.Guild.getGuild(rs.getInt("c.guildid")).getName();
                    }
                    text.append("#b소속된 길드 : #k" + guild + "\r\n\r\n");

                    String connect = "";
                    if (rs.getInt("a.loggedin") == 0) {
                        connect = "#r오프라인";
                    } else {
                        connect = "#g온라인";
                    }
                    text.append("#b접속 현황 : #k" + connect + "\r\n");
                    text.append("#b계정 아이디 : #k" + rs.getString("a.name") + "\r\n");
                    //text.append("#b계정 비밀번호 : #k" + rs.getString("a.password") + "\r\n");//비밀번호는 의미가 없으니.. 주석
                    text.append("#b아이피 : #k" + rs.getString("SessionIP") + "\r\n\r\n");
                    text.append("#b마지막 접속 : #k" + rs.getString("lastlogin") + "\r\n");
                    text.append("#b아이디 생성 날짜 : #k" + rs.getString("createdat") + "\r\n");

                } else {
                    c.getPlayer().dropMessage(5, "[시스템] " + splitted[1] + " 닉네임을 가진 유저가 존재하지 않습니다.");
                    return 1;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (rs != null) {
                        rs.close();
                    }
                    if (ps != null) {
                        ps.close();
                    }
                    if (con != null) {
                        con.close();
                    }
                } catch (Exception e) {

                }
            }
            c.getSession().write(MaplePacketCreator.getNPCTalk(9900000, (byte) 0, text.toString(), "00 00", (byte) 0));
            return 1;
        }
    } 
    
    public static class 캐릭터정보2 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final StringBuilder builder = new StringBuilder();
            final MapleCharacter other = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (other == null) {
                builder.append("존재하지 않는 캐릭터입니다.");
                c.getPlayer().dropMessage(6, builder.toString());
                return 0;
            }
            if (other.getClient().getLastPing() <= 0) {
                other.getClient().sendPing();
            }
            if (other.getGMLevel() > c.getPlayer().getGMLevel()) {
                c.getPlayer().dropMessage(6, "이 캐릭터의 정보를 볼 수 없습니다.");
                return 0;
            }
            builder.append(MapleClient.getLogMessage(other, ""));
            builder.append(" at ").append(other.getPosition().x);
            builder.append("/").append(other.getPosition().y);

            builder.append(" || HP : ");
            builder.append(other.getStat().getHp());
            builder.append(" /");
            builder.append(other.getStat().getCurrentMaxHp());

            builder.append(" || MP : ");
            builder.append(other.getStat().getMp());
            builder.append(" /");
            builder.append(other.getStat().getCurrentMaxMp());

            builder.append(" || 물리공격력 : ");
            builder.append(other.getStat().getTotalWatk());
            builder.append(" || 마법공격력 : ");
            builder.append(other.getStat().getTotalMagic());
//            builder.append(" || DAMAGE% : ");
//            builder.append(other.getStat().dam_r);
//            builder.append(" || BOSSDAMAGE% : ");
//            builder.append(other.getStat().bossdam_r);
            builder.append(" || STR : ");
            builder.append(other.getStat().getStr());
            builder.append(" || DEX : ");
            builder.append(other.getStat().getDex());
            builder.append(" || INT : ");
            builder.append(other.getStat().getInt());
            builder.append(" || LUK : ");
            builder.append(other.getStat().getLuk());

            builder.append(" || 총합 STR : ");
            builder.append(other.getStat().getTotalStr());
            builder.append(" || 총합 DEX : ");
            builder.append(other.getStat().getTotalDex());
            builder.append(" || 총합 INT : ");
            builder.append(other.getStat().getTotalInt());
            builder.append(" || 총합 LUK : ");
            builder.append(other.getStat().getTotalLuk());

            builder.append(" || EXP : ");
            builder.append(other.getExp());
            builder.append(" || 메소 : ");
            builder.append(other.getMeso());

            builder.append(" || party : ");
            builder.append(other.getParty() == null ? -1 : other.getParty().getId());

            builder.append(" || hasTrade : ");
            builder.append(other.getTrade() != null);
            builder.append(" || 딜레이 : ");
            builder.append(other.getClient().getLatency());
            builder.append(" || PING : ");
            builder.append(other.getClient().getLastPing());
            builder.append(" || PONG : ");
            builder.append(other.getClient().getLastPong());
            builder.append(" || 접속한 IP 주소 : ");

            other.getClient().DebugMessage(builder);

            c.getPlayer().dropMessage(6, builder.toString());
            return 1;
        }
    }
    
    public static class 소환 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                if ((!c.getPlayer().isGM() && (victim.isInBlockedMap() || victim.isGM()))) {
                    c.getPlayer().dropMessage(5, "Try again later.");
                    return 0;
                }
                victim.changeMap(c.getPlayer().getMap(), c.getPlayer().getMap().findClosestPortal(c.getPlayer().getTruePosition()));
            } else {
                int ch = World.Find.findChannel(splitted[1]);
                if (ch < 0) {
                    c.getPlayer().dropMessage(5, "Not found.");
                    return 0;
                }
                victim = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim == null || (!c.getPlayer().isGM() && (victim.isInBlockedMap() || victim.isGM()))) {
                    c.getPlayer().dropMessage(5, "Try again later.");
                    return 0;
                }
                c.getPlayer().dropMessage(5, "대상이 채널 이동 중입니다.");
                victim.dropMessage(5, "채널 이동 중입니다.");
                if (victim.getMapId() != c.getPlayer().getMapId()) {
                    final MapleMap mapp = victim.getClient().getChannelServer().getMapFactory().getMap(c.getPlayer().getMapId());
                    victim.changeMap(mapp, mapp.findClosestPortal(c.getPlayer().getTruePosition()));
                }
                victim.changeChannel(c.getChannel());
            }
            return 1;
        }
    }
    
    public static class 온라인 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(5, "CH." + c.getChannel() + "에 접속 중인 캐릭터:");
            c.getPlayer().dropMessage(5, c.getChannelServer().getPlayerStorage().getOnlinePlayers(true));
            return 1;
        }
    }

    public static class 연결 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            StringBuilder text = new StringBuilder();
            int count = 0, value = 0;
            for (int i = 1; i <= ChannelServer.getChannelCount(); i++) {
                if (i < 2) {
                    text.append(i + "채널");
                } else if (i == 2) {
                    text.append("20세이상");
                } else {
                    text.append((i - 1) + "채널");
                }
                for (MapleCharacter chr : ChannelServer.getInstance(i).getPlayerStorage().getAllCharacters()) {
                    if (!chr.isGM()) {
                        count++;
                        value++;
                        if (value == 1) {
                            text.append(" : " + chr.getName());
                        } else {
                            text.append(", " + chr.getName());
                        }
                    }
                }
                c.getPlayer().dropMessage(5, text.toString());
                text.setLength(0);
                value = 0;
            }
            c.getPlayer().dropMessage(5, "총 접속자 : " + count + "명");
            return 1;
        }
    }

    public static class 사냥동접 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            StringBuilder text = new StringBuilder();
            int count = 0, value = 0;
            for (int i = 1; i <= ChannelServer.getChannelCount(); i++) {
                if (i < 2) {
                    text.append(i + "채널");
                } else if (i == 2) {
                    text.append("20세이상");
                } else {
                    text.append((i - 1) + "채널");
                }
                for (MapleCharacter chr : ChannelServer.getInstance(i).getPlayerStorage().getAllCharacters()) {
                    if (!chr.isGM() && chr.getMap().getAllMonster().size() > 0) {
                        count++;
                        value++;
                        if (value == 1) {
                            text.append(" : " + chr.getName());
                        } else {
                            text.append(", " + chr.getName());
                        }
                    }
                }
                c.getPlayer().dropMessage(5, text.toString());
                text.setLength(0);
                value = 0;
            }
            c.getPlayer().dropMessage(5, "사냥 중인 유저 : " + count + "명");
            return 1;
        }
    }

    public static class 아이템소지 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3 || splitted[1] == null || splitted[1].equals("") || splitted[2] == null || splitted[2].equals("")) {
                c.getPlayer().dropMessage(6, "!아이템소지 닉네임 아이템코드");
                return 0;
            } else {
                int item = Integer.parseInt(splitted[2]);
                MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                int itemamount = chr.getItemQuantity(item, true);
                if (itemamount > 0) {
                    c.getPlayer().dropMessage(5, chr.getName() + "는 " + itemamount + " (" + item + ")를 소지중입니다.");
                } else {
                    c.getPlayer().dropMessage(5, chr.getName() + "는 (" + item + ")를 소지중이지 않습니다.");
                }
            }
            return 1;
        }
    }    
    
    public static class xy extends InternCommand.현재좌표 {
    }    

    public static class 현재좌표 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            Point pos = c.getPlayer().getPosition();
            final String format = "[포지션] Map : %09d  X : %d  Y : %d  RX0 : %d  RX1 : %d  FH : %d";
            c.getPlayer().dropMessage(5, String.format(format, c.getPlayer().getMap().getId(), pos.x, pos.y, (pos.x - 50), (pos.x + 50), c.getPlayer().getFH()));
            return 1;
        }
    }

    private static boolean isMapAvailable(int mapId) {
        return mapId != 188888888;
    }

    public static class 채팅금지 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "채팅금지 [닉네임] [기간]");
                return 0;
            }
            final int numDay = Integer.parseInt(splitted[2]);

            final Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, numDay);
            final DateFormat df = DateFormat.getInstance();
            Connection con = null;
            PreparedStatement ps = null;
            try {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("UPDATE accounts SET `chatblocktime` = ? WHERE id = ?");
                ps.setTimestamp(1, new java.sql.Timestamp(cal.getTimeInMillis()));
                ps.setInt(2, MapleCharacterUtil.getAccIdByName(splitted[1]));
                ps.executeUpdate();
            } catch (Exception e) {
                c.getPlayer().dropMessage(5, "오류입니다 : " + e);
                return 0;
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
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim != null) {
                    victim.dropMessage(1, "대화가 금지되었습니다.");
                    victim.canTalk(false);
                }
            }
            c.getPlayer().dropMessage(6, "해당 캐릭터는 " + splitted[1] + " 일정 기간동안 채팅금지가 성립되었습니다. " + df.format(cal.getTime()));
            return 1;
        }
    }

    public static class 채팅금지헤제 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "채팅금지 [닉네임] [기간]");
                return 0;
            }
            final int numDay = Integer.parseInt(splitted[2]);

            final Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, 0);
            final DateFormat df = DateFormat.getInstance();
            Connection con = null;
            PreparedStatement ps = null;
            try {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("UPDATE accounts SET `chatblocktime` = ? WHERE id = ?");
                ps.setTimestamp(1, new java.sql.Timestamp(cal.getTimeInMillis()));
                ps.setInt(2, MapleCharacterUtil.getAccIdByName(splitted[1]));
                ps.executeUpdate();
            } catch (Exception e) {
                c.getPlayer().dropMessage(5, "오류입니다 : " + e);
                return 0;
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
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim != null) {
                    victim.dropMessage(1, "대화 금지가 해제 되었습니다. 재접속을 해주세요^^");
                    victim.canTalk(false);
                }
            }
            c.getPlayer().dropMessage(6, "해당 캐릭터는 대화 금지가 헤제 되었습니다.");
            return 1;
        }
    }    
    
    
    
    public static class 펫 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            boolean allowed = c.getPlayer().getMap().togglePetPick();
            c.getPlayer().dropMessage(6, "Current Map's Pet Pickup allowed : " + allowed);
            if (!allowed) {
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.yellowChat("현재 맵에서 펫 줍기 기능이 비활성화 되었습니다."));
            } else {
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.yellowChat("현재 맵에서 펫 줍기 기능이 활성화 되었습니다."));
            }
            return 1;
        }

    }    
    
    public static class 맵온라인 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "현재 이 맵에 있는 유저:");
            StringBuilder builder = new StringBuilder();
            for (MapleCharacter chr : c.getPlayer().getMap().getCharactersThreadsafe()) {
                if (builder.length() > 150) { // wild guess :o
                    builder.setLength(builder.length() - 2);
                    c.getPlayer().dropMessage(6, builder.toString());
                    builder = new StringBuilder();
                }
                builder.append(MapleCharacterUtil.makeMapleReadable(chr.getName()));
                builder.append(", ");
            }
            builder.setLength(builder.length() - 2);
            c.getPlayer().dropMessage(6, builder.toString());
            return 1;
        }
    }
    public static class 정지 extends CommandExecute {

        protected boolean ipBan = false;
        private String[] types = {"핵 사용", "매크로 사용", "광고", "욕설 / 비난 / 비방", "도배", "GM 괴롭힘 / 욕", "공개 욕설/비난/비방", "현금거래", "임시 정지 처분", "사칭", "관리자 사칭", "불법 / 비인가 프로그램 사용 (감지)", "계정 도용"};

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 4) {
                c.getPlayer().dropMessage(5, "정지 [닉네임] [정지사유] [정지 일 수]");
                StringBuilder s = new StringBuilder("정지사유: ");
                for (int i = 0; i < types.length; i++) {
                    s.append(i).append(" - ").append(types[i]).append(", ");
                }
                c.getPlayer().dropMessage(6, s.toString());
                return 0;
            }
            final MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            final int reason = Integer.parseInt(splitted[2]);
            final int numDay = Integer.parseInt(splitted[3]);

            final Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, numDay);
            final DateFormat df = DateFormat.getInstance();

            if (reason < 0 || reason >= types.length) {
                c.getPlayer().dropMessage(5, "캐릭터 이름이 잘못됐거나 정지 사유가 짧습니다.");
                return 0;
            }
            if (victim == null) {
                boolean res = MapleCharacter.tempban(types[reason], cal, reason, c.getPlayer().getName(), splitted[1]);
                if (!res) {
                    c.getPlayer().dropMessage(5, "캐릭터 이름이 잘못됐거나 정지 사유가 짧습니다.");
                    return 0;
                }
                c.getPlayer().dropMessage(5, "" + splitted[1] + "캐릭터가 성공적으로 정지됐습니다. 정지기간:" + df.format(cal.getTime()));
                return 1;
            }
            victim.tempban(types[reason], cal, reason, ipBan, c.getPlayer().getName());
            victim.getClient().disconnect(true, false);
            victim.getClient().getSession().close();
            victim.getClient().getSocketChannel().close();
            c.getPlayer().dropMessage(5, "" + splitted[1] + "캐릭터가 성공적으로 정지됐습니다. 정지기간:" + df.format(cal.getTime()));
            return 1;
        }
    }    
    public static class 언밴 extends CommandExecute {

        protected boolean hellban = false;

        private String getCommand() {
            if (hellban) {
                return "언헬밴";
            } else {
                return "언밴";
            }
        }

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "[Syntax] !" + getCommand() + " <IGN>");
                return 0;
            }
            byte ret;
            if (hellban) {
                ret = MapleClient.unHellban(splitted[1]);
            } else {
                ret = MapleClient.unban(splitted[1]);
            }
            if (ret == -2) {
                c.getPlayer().dropMessage(6, "[" + getCommand() + "] SQL error.");
                return 0;
            } else if (ret == -1) {
                c.getPlayer().dropMessage(6, "[" + getCommand() + "] The character does not exist.");
                return 0;
            } else {
                c.getPlayer().dropMessage(6, "[" + getCommand() + "] Successfully unbanned!");

            }
            byte ret_ = MapleClient.unbanIPMacs(splitted[1]);
            if (ret_ == -2) {
                c.getPlayer().dropMessage(6, "[UnbanIP] SQL error.");
            } else if (ret_ == -1) {
                c.getPlayer().dropMessage(6, "[UnbanIP] The character does not exist.");
            } else if (ret_ == 0) {
                c.getPlayer().dropMessage(6, "[UnbanIP] No IP or Mac with that character exists!");
            } else if (ret_ == 1) {
                c.getPlayer().dropMessage(6, "[UnbanIP] IP/Mac -- one of them was found and unbanned.");
            } else if (ret_ == 2) {
                c.getPlayer().dropMessage(6, "[UnbanIP] Both IP and Macs were unbanned.");
            }
            return ret_ > 0 ? 1 : 0;
        }
    }

    public static class 언밴아이피 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "[Syntax] !unbanip <IGN>");
                return 0;
            }
            byte ret = MapleClient.unbanIPMacs(splitted[1]);
            if (ret == -2) {
                c.getPlayer().dropMessage(6, "[UnbanIP] SQL error.");
            } else if (ret == -1) {
                c.getPlayer().dropMessage(6, "[UnbanIP] The character does not exist.");
            } else if (ret == 0) {
                c.getPlayer().dropMessage(6, "[UnbanIP] No IP or Mac with that character exists!");
            } else if (ret == 1) {
                c.getPlayer().dropMessage(6, "[UnbanIP] IP/Mac -- one of them was found and unbanned.");
            } else if (ret == 2) {
                c.getPlayer().dropMessage(6, "[UnbanIP] Both IP and Macs were unbanned.");
            }
            if (ret > 0) {
                return 1;
            }
            return 0;
        }
    }

    public static class 밴 extends CommandExecute {

        protected boolean hellban = false, ipBan = false;

        private String getCommand() {
            if (hellban) {
                return "헬밴";
            } else {
                return "밴";
            }
        }

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(5, "[Syntax] !" + getCommand() + " <IGN> <Reason>");
                return 0;
            }
            if (StringUtil.joinStringFrom(splitted, 2).length() < 10) {
                c.getPlayer().dropMessage(5, "밴 사유가 너무 짧습니다. 상세하게 적어주세요.");
                return 0;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("밴 캐릭터 : " + splitted[1]).append("\r\n사유 : ").append(StringUtil.joinStringFrom(splitted, 2));
            MapleCharacter target = World.getCharacterByName(splitted[1]);
            if (target != null) {
                if (c.getPlayer().getGMLevel() > target.getGMLevel() || c.getPlayer().isAdmin()) {
                    sb.append(" (IP: ").append(target.getClient().getSessionIPAddress()).append(")");
                    if (target.ban(sb.toString(), hellban || ipBan, false, hellban, c.getPlayer().getName())) {
                        c.getPlayer().dropMessage(6, "[" + getCommand() + "] Successfully banned " + splitted[1] + ".");
                        return 1;
                    } else {
                        c.getPlayer().dropMessage(6, "[" + getCommand() + "] Failed to ban.");
                        return 0;
                    }
                } else {
                    c.getPlayer().dropMessage(6, "[" + getCommand() + "] May not ban GMs...");
                    return 1;
                }
            } else {
                if (MapleCharacter.ban(splitted[1], sb.toString(), false, c.getPlayer().isAdmin() ? 250 : c.getPlayer().getGMLevel(), hellban, c.getPlayer().getName())) {
                    c.getPlayer().dropMessage(6, "[" + getCommand() + "] Successfully offline banned " + splitted[1] + ".");
                    return 1;
                } else {
                    c.getPlayer().dropMessage(6, "[" + getCommand() + "] Failed to ban " + splitted[1]);
                    return 0;
                }
            }
        }
    }    

    public static class 맵디버그 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, c.getPlayer().getMap().spawnDebug());
            return 1;
        }
    }
}