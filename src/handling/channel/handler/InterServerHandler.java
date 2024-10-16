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

import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;

import client.MapleQuestStatus;
import client.Skill;
import client.SkillFactory;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import connector.ConnectorClient;
import connector.ConnectorServer;
import constants.GameConstants;
import constants.ServerConstants;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.CharacterIdChannelPair;
import handling.world.CharacterTransfer;
import handling.world.MapleMessenger;
import handling.world.MapleMessengerCharacter;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.PartyOperation;
import handling.world.PlayerBuffStorage;
import handling.world.World;
import handling.world.exped.MapleExpedition;
import handling.world.guild.MapleGuild;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.table.DefaultTableModel;
import scripting.NPCScriptManager;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.MedalRanking;
import server.Start;
import server.maps.FieldLimitType;
import server.quest.MapleQuest;
import system.BuffHandler;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.data.LittleEndianAccessor;
import tools.packet.FamilyPacket;
import tools.packet.LoginPacket;
import tools.packet.MTSCSPacket;
import tools.packet.PetPacket;
import util.FileTime;

public class InterServerHandler {

    /*public static void EnterCS(MapleClient c) {
     c.sendPacket(MaplePacketCreator.enableActions());
     //if (NPCScriptInvoker.runNpc(c, 9900003, 0) != 0) {
     CashShopEnter(c, c.getPlayer());
     // }
     }*/
    public static void EnterCS(MapleClient c) {
        c.getSession().write(MaplePacketCreator.enableActions());
        c.removeClickedNPC();
        NPCScriptManager.getInstance().start(c, 9000030, "OpenCS");
    }

    public static final void CashShopEnter(final MapleClient c, final MapleCharacter chr) {

        if (chr.hasBlockedInventory() || chr.getMap() == null || chr.getEventInstance() != null || c.getChannelServer() == null) {
            c.getSession().write(MaplePacketCreator.serverBlocked(2));
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (World.getPendingCharacterSize() >= 10) {
            chr.dropMessage(1, "The server is busy at the moment. Please try again in a minute or less.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        //if (c.getChannel() == 1 && !c.getPlayer().isGM()) {
        //    c.getPlayer().dropMessage(5, "You may not enter on this channel. Please change channels and try again.");
        //    c.getSession().write(MaplePacketCreator.enableActions());
        //    return;
        //}
        final ChannelServer ch = ChannelServer.getInstance(c.getChannel());

        chr.changeRemoval();
        if (chr.checkPcTime()) {
            c.getSession().write(MaplePacketCreator.enableInternetCafe((byte) 0, chr.getCalcPcTime()));
        }
        //PC방

        if (chr.getMessenger() != null) {
            MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(chr);
            World.Messenger.leaveMessenger(chr.getMessenger().getId(), messengerplayer);
        }
        PlayerBuffStorage.addBuffsToStorage(chr.getId(), chr.getAllBuffs());
        PlayerBuffStorage.addCooldownsToStorage(chr.getId(), chr.getCooldowns());
        PlayerBuffStorage.addDiseaseToStorage(chr.getId(), chr.getAllDiseases());
        World.ChannelChange_Data(new CharacterTransfer(chr), chr.getId(), -10);
        ch.removePlayer(chr);
        c.updateLoginState(MapleClient.CHANGE_CHANNEL, c.getSessionIPAddress());
        chr.saveToDB(false, false);
        chr.getMap().removePlayer(chr);

        LoginServer.setCodeHash(chr.getId(), c.getCodeHash());
        c.getSession().write(MaplePacketCreator.getChannelChange(c, CashShopServer.getPort()));
        c.setPlayer(null);
        c.setReceiving(false);
    }

    public static final void Loggedin(final int playerid, final MapleClient c) {
        final ChannelServer channelServer = c.getChannelServer();
        MapleCharacter player;
        final CharacterTransfer transfer = channelServer.getPlayerStorage().getPendingCharacter(playerid);
        boolean newLogin = false;
        if (transfer == null) { // Player isn't in storage, probably isn't CC
       
            player = MapleCharacter.loadCharFromDB(playerid, c, true);
            Pair<String, String> ip = LoginServer.getLoginAuth(playerid);
            String s = c.getSessionIPAddress();
            if (ip == null || !s.substring(s.indexOf('/') + 1, s.length()).equals(ip.left)) {
                if (ip != null) {
                    LoginServer.putLoginAuth(playerid, ip.left, ip.right);
                }
                c.getSession().close();
                return;
            }
            c.setTempIP(ip.right);
            newLogin = true;
           
        } else {
            player = MapleCharacter.ReconstructChr(transfer, c, true);
        }
     
        c.setPlayer(player);
        c.setAccID(player.getAccountID());

        if (!c.CheckIPAddress()) { // Remote hack
            c.getSession().close();
            return;
        }
  
        CharacterTransfer transfer2 = CashShopServer.getPlayerStorage().getPendingCharacter(playerid);
        if (transfer2 != null) { // Remote hack
            c.getSession().close();
            return;
        }

        final int state = c.getLoginState();
        boolean allowLogin = false;

        if (state == MapleClient.LOGIN_SERVER_TRANSITION || state == MapleClient.CHANGE_CHANNEL || state == MapleClient.LOGIN_NOTLOGGEDIN) {
            allowLogin = !World.isCharacterListConnected(c.loadCharacterNames(c.getWorld()));
        }
        if (!allowLogin) {
            c.setPlayer(null);
            c.getSession().close();
            return;
        }
       
        c.updateLoginState(MapleClient.LOGIN_LOGGEDIN, c.getSessionIPAddress());
        channelServer.addPlayer(player);

        player.giveCoolDowns(PlayerBuffStorage.getCooldownsFromStorage(player.getId()));
        player.silentGiveBuffs(PlayerBuffStorage.getBuffsFromStorage(player.getId()));
        player.giveSilentDebuff(PlayerBuffStorage.getDiseaseFromStorage(player.getId()));
        c.getSession().write(MaplePacketCreator.getCharInfo(player));
        c.getSession().write(MTSCSPacket.enableCSUse());

        if (player.checkPcTime()) {
            player.clearPc(true);
            c.getSession().write(MaplePacketCreator.enableInternetCafe((byte) 2, player.getCalcPcTime()));
        } else {
            c.getSession().write(MaplePacketCreator.enableInternetCafe((byte) 0, player.getCalcPcTime()));
        }
        //PC방

        c.getSession().write(MaplePacketCreator.temporaryStats_Reset()); // .
        player.getMap().addPlayer(player);

        int connection = 0;
        for (ChannelServer chn : ChannelServer.getAllInstances()) {
            connection += chn.getPlayerStorage().getConnectedClients();
        }
        int incConnection = Start.increaseConnectionUsers;
        if (connection < 10) {
            incConnection = 0;
        }
        if (incConnection < 0) {
            if (Math.abs(incConnection) < connection)
                connection += incConnection;
        } else {
            connection += incConnection;
        }
        c.sendPacket(MaplePacketCreator.yellowChat("[알림] 현재 " + connection + "명이 서버에 접속중입니다."));
        //c.getSession().write(MaplePacketCreator.yellowChat("[안내] @도움말 입력시 가이드 엔피시와 대화합니다."));
        //c.getSession().write(MaplePacketCreator.yellowChat("[안내] ~ 할말 : 고성능 확성기 이용 가능 합니다."));
        c.getSession().write(MaplePacketCreator.yellowChat("[안내] 고용 상인 설치시 경험치 10% 효과를 얻게 됩니다."));
        
        if (ChannelServer.isElite(c.getChannel())) {
            player.dropMessage(-9, "[알림] 엘리트 채널에 입장하셨습니다. 엘리트 채널에서는 모든 몬스터가 강력해지며 드롭율과 경험치가 상승합니다.");
        }
        
        //c.getSession().write(MaplePacketCreator.yellowChat("[TIP] @저장 명령어를 항상 사용하시기바랍니다. 서버의 모든 데이터가 저장 됩니다."));
        //c.getSession().write(MaplePacketCreator.yellowChat("[알림] 현재 " + 동접 + "명이 플레이 중입니다."));
        //c.getSession().write(MaplePacketCreator.yellowChat("[알림] 모든 펫은 자석효과를 가지고있습니다."));
        c.getSession().write(MTSCSPacket.enableCSUse());

        try {
            // Start of buddylist
            final int buddyIds[] = player.getBuddylist().getBuddyIds();
            World.Buddy.loggedOn(player.getName(), player.getId(), c.getChannel(), buddyIds);
            if (player.getParty() != null) {
                final MapleParty party = player.getParty();
                World.Party.updateParty(party.getId(), PartyOperation.LOG_ONOFF, new MaplePartyCharacter(player));

                if (party != null && party.getExpeditionId() > 0) {
                    final MapleExpedition me = World.Party.getExped(party.getExpeditionId());
                    if (me != null) {
                        c.getSession().write(MaplePacketCreator.expeditionStatus(me, false));
                    }
                }
            }

            if (player.getSidekick() == null) {
                player.setSidekick(World.Sidekick.getSidekickByChr(player.getId()));
            }
            final CharacterIdChannelPair[] onlineBuddies = World.Find.multiBuddyFind(player.getId(), buddyIds);
            for (CharacterIdChannelPair onlineBuddy : onlineBuddies) {
                player.getBuddylist().get(onlineBuddy.getCharacterId()).setChannel(onlineBuddy.getChannel());
            }
            c.getSession().write(MaplePacketCreator.updateBuddylist(player.getBuddylist().getBuddies()));

            // Start of Messenger
            final MapleMessenger messenger = player.getMessenger();
            if (messenger != null) {
                World.Messenger.silentJoinMessenger(messenger.getId(), new MapleMessengerCharacter(c.getPlayer()));
                World.Messenger.updateMessenger(messenger.getId(), c.getPlayer().getName(), c.getChannel());
            }

            // Start of Guild and alliance
            if (player.getGuildId() > 0) {
                World.Guild.setGuildMemberOnline(player.getMGC(), true, c.getChannel());
                c.getSession().write(MaplePacketCreator.showGuildInfo(player));
                final MapleGuild gs = World.Guild.getGuild(player.getGuildId());
                if (gs != null) {
                    final List<byte[]> packetList = World.Alliance.getAllianceInfo(gs.getAllianceId(), true);
                    if (packetList != null) {
                        for (byte[] pack : packetList) {
                            if (pack != null) {
                                c.getSession().write(pack);
                            }
                        }
                    }
                } else { //guild not found, change guild id
                    player.setGuildId(0);
                    player.setGuildRank((byte) 5);
                    player.setAllianceRank((byte) 5);
                    player.saveGuildStatus();
                }
            }

            if (player.getFamilyId() > 0) {
                World.Family.setFamilyMemberOnline(player.getMFC(), true, c.getChannel());
            }
            c.getSession().write(FamilyPacket.getFamilyData());
            c.getSession().write(FamilyPacket.getFamilyInfo(player));
        } catch (Exception e) {
        }
        player.getClient().getSession().write(MaplePacketCreator.serverMessage(channelServer.getServerMessage()));
        player.sendMacros();
        player.showNote();
        player.updatePartyMemberHP();
        player.baseSkills(); //fix people who've lost skills.   
        if (newLogin) {
            player.setEquippedTimeAll();
        }
        
        Date nowtime = new Date();

        for (int ho : ServerConstants.hour) {
            if (nowtime.getMinutes() >= 50 && nowtime.getMinutes() <= 59 && nowtime.getHours() == (ho - 1)) {
                player.dropMessage(6, (60 - nowtime.getMinutes()) + "분후 월드 보스가 시작됩니다. 자유 시장 월드 보스 NPC 를 통해 어서 입장하세요.");
                player.sethottimeboss(true);
            }
        }

        if (player.haveItem(1142005, 1, true, true) && player.getQuestStatus(29400) >= 1) {
            if (MedalRanking.getReadOnlyRanking(MedalRanking.MedalRankingType.ExpertHunter).isEmpty() || MedalRanking.canMedalRank(MedalRanking.MedalRankingType.ExpertHunter, player.getName(), Integer.parseInt(player.getOneInfo(29400, "mon"))) != 0) {
                //노련한 사냥꾼의 자격이 없다면
                player.removeAllEquip(1142005, false);
                player.dropMessage(5, "전설적인 사냥꾼 자격이 박탈되어, 칭호가 회수되었습니다.");
            }
        }

        if (player.haveItem(1142006, 1, true, true) && player.getQuestStatus(29500) >= 1) {
            if (MedalRanking.getReadOnlyRanking(MedalRanking.MedalRankingType.Pop).isEmpty() || MedalRanking.canMedalRank(MedalRanking.MedalRankingType.Pop, player.getName(), player.getFame()) != 0) {
                player.removeAllEquip(1142006, false);
                player.dropMessage(5, "메이플 아이돌스타 자격이 박탈되어, 칭호가 회수되었습니다.");
            }
        }

        if (player.haveItem(1142014, 1, true, true) && player.getQuestStatus(29503) >= 1) {
            if (MedalRanking.getReadOnlyRanking(MedalRanking.MedalRankingType.HenesysDonor).isEmpty() || MedalRanking.canMedalRank(MedalRanking.MedalRankingType.HenesysDonor, player.getName(), Integer.parseInt(player.getOneInfo(29503, "money"))) != 0) {
                player.removeAllEquip(1142014, false);
                player.dropMessage(5, "헤네시스 기부왕 자격이 박탈되어, 칭호가 회수되었습니다.");
            }
        }
        if (player.haveItem(1142015, 1, true, true) && player.getQuestStatus(29503) >= 1) {
            if (MedalRanking.getReadOnlyRanking(MedalRanking.MedalRankingType.ElliniaDonor).isEmpty() || MedalRanking.canMedalRank(MedalRanking.MedalRankingType.ElliniaDonor, player.getName(), Integer.parseInt(player.getOneInfo(29503, "money"))) != 0) {
                player.removeAllEquip(1142015, false);
                player.dropMessage(5, "엘리니아 기부왕 자격이 박탈되어, 칭호가 회수되었습니다.");
            }
        }
        if (player.haveItem(1142016, 1, true, true) && player.getQuestStatus(29503) >= 1) {
            if (MedalRanking.getReadOnlyRanking(MedalRanking.MedalRankingType.PerionDonor).isEmpty() || MedalRanking.canMedalRank(MedalRanking.MedalRankingType.PerionDonor, player.getName(), Integer.parseInt(player.getOneInfo(29503, "money"))) != 0) {
                player.removeAllEquip(1142016, false);
                player.dropMessage(5, "페리온 기부왕 자격이 박탈되어, 칭호가 회수되었습니다.");
            }
        }
        if (player.haveItem(1142017, 1, true, true) && player.getQuestStatus(29503) >= 1) {
            if (MedalRanking.getReadOnlyRanking(MedalRanking.MedalRankingType.KerningDonor).isEmpty() || MedalRanking.canMedalRank(MedalRanking.MedalRankingType.KerningDonor, player.getName(), Integer.parseInt(player.getOneInfo(29503, "money"))) != 0) {
                player.removeAllEquip(1142017, false);
                player.dropMessage(5, "커닝시티 기부왕 자격이 박탈되어, 칭호가 회수되었습니다.");
            }
        }
        if (player.haveItem(1142018, 1, true, true) && player.getQuestStatus(29503) >= 1) {
            if (MedalRanking.getReadOnlyRanking(MedalRanking.MedalRankingType.SleepyWoodDonor).isEmpty() || MedalRanking.canMedalRank(MedalRanking.MedalRankingType.SleepyWoodDonor, player.getName(), Integer.parseInt(player.getOneInfo(29503, "money"))) != 0) {
                player.removeAllEquip(1142018, false);
                player.dropMessage(5, "슬리피우드 기부왕 자격이 박탈되어, 칭호가 회수되었습니다.");
            }
        }
        if (player.haveItem(1142019, 1, true, true) && player.getQuestStatus(29503) >= 1) {
            if (MedalRanking.getReadOnlyRanking(MedalRanking.MedalRankingType.NautilusDonor).isEmpty() || MedalRanking.canMedalRank(MedalRanking.MedalRankingType.NautilusDonor, player.getName(), Integer.parseInt(player.getOneInfo(29503, "money"))) != 0) {
                player.removeAllEquip(1142019, false);
                player.dropMessage(5, "노틸러스 기부왕 자격이 박탈되어, 칭호가 회수되었습니다.");
            }
        }
        if (player.haveItem(1142030, 1, true, true) && player.getQuestStatus(29503) >= 1) {
            if (MedalRanking.getReadOnlyRanking(MedalRanking.MedalRankingType.LithDonor).isEmpty() || MedalRanking.canMedalRank(MedalRanking.MedalRankingType.LithDonor, player.getName(), Integer.parseInt(player.getOneInfo(29503, "money"))) != 0) {
                player.removeAllEquip(1142030, false);
                player.dropMessage(5, "리스항구 기부왕 자격이 박탈되어, 칭호가 회수되었습니다.");
            }
        }

        if (player.haveItem(1142007, 1, true, true) && player.getQuestStatus(29501) >= 1) {
            String d = player.getQuestNAdd(MapleQuest.getInstance(136000)).getCustomData();
            int d2 = 0;
            if (d == null) {
                d2 = 0;
            }
            d2 = Integer.parseInt(d);
            if (d2 == 0 || MedalRanking.getReadOnlyRanking(MedalRanking.MedalRankingType.HorntailSlayer).isEmpty() || MedalRanking.canMedalRank(MedalRanking.MedalRankingType.HorntailSlayer, player.getName(), d2) != 0) {
                player.removeAllEquip(1142007, false);
                player.dropMessage(5, "혼테일 슬레이어 자격이 박탈되어, 칭호가 회수되었습니다.");
            }
        }

        if (player.haveItem(1142008, 1, true, true) && player.getQuestStatus(29502) >= 1) {
            String d = player.getQuestNAdd(MapleQuest.getInstance(136001)).getCustomData();
            int d2 = 0;
            if (d == null) {
                d2 = 0;
            }
            d2 = Integer.parseInt(d);
            if (d2 == 0 || MedalRanking.getReadOnlyRanking(MedalRanking.MedalRankingType.PinkbeanSlayer).isEmpty() || MedalRanking.canMedalRank(MedalRanking.MedalRankingType.PinkbeanSlayer, player.getName(), d2) != 0) {
                player.removeAllEquip(1142008, false);
                player.dropMessage(5, "핑크빈 슬레이어 자격이 박탈되어, 칭호가 회수되었습니다.");
            }
        }

        player.acceptUpdate();
        c.getSession().write(MaplePacketCreator.getKeymap(player.getKeyLayout()));
        player.updatePetAuto();
        player.expirationTask(true, transfer == null);
        if (player.getJob() == 132) { // DARKKNIGHT
            player.checkBerserk();
        }

        player.spawnSavedPets();

        DueyHandler.checkReceivePackage(player);
        if (player.isHidden()) {
            c.getSession().write(MaplePacketCreator.GmHide(player.isHidden()));
        }

        if (player.getUltimate() == 1) { // 궁극의 모험가
            player.gainAp((short) 258);
            player.teachSkill(1004, (byte) 1, (byte) 1);
            switch (player.getJob()) {
                case 110:
                case 120:
                case 130:
                    player.gainSP((short) 123);
                    player.teachSkill(1075, (byte) 1, (byte) 1);
                    player.setUltimate((int) 2);
                    break;
                case 210:
                case 220:
                case 230:
                    player.gainSP((short) 129);
                    player.teachSkill(1074, (byte) 1, (byte) 1);
                    player.setUltimate((int) 2);
                    break;
                case 310:
                case 320:
                    player.gainSP((short) 123);
                    player.teachSkill(1077, (byte) 1, (byte) 1);
                    player.setUltimate((int) 2);
                    break;
                case 410:
                case 420:
                    player.gainSP((short) 123);
                    player.teachSkill(1078, (byte) 1, (byte) 1);
                    player.setUltimate((int) 2);
                    break;
                case 432:
                    player.gainSP((short) 123);
                    player.teachSkill(1078, (byte) 1, (byte) 1);
                    player.teachSkill(4311003, (byte) 0, (byte) 5);
                    player.setSubcategory(1);
                    player.setUltimate((int) 2);
                    break;
                case 510:
                case 520:
                    player.gainSP((short) 123);
                    player.teachSkill(1079, (byte) 1, (byte) 1);
                    player.setUltimate((int) 2);
                    break;
            }
        }

//        MapleQuestStatus stat = player.getQuestNoAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
//        c.getSession().write(MaplePacketCreator.pendantSlot(stat != null && stat.getCustomData() != null && Long.parseLong(stat.getCustomData()) > System.currentTimeMillis()));
        c.sendPacket(MaplePacketCreator.pendantSlot(FileTime.compareFileTime(player.getEquipExtExpire(), FileTime.systemTimeToFileTime()) >= 0));
        MapleQuestStatus stat = player.getQuestNoAdd(MapleQuest.getInstance(GameConstants.QUICK_SLOT));
        c.getSession().write(MaplePacketCreator.quickSlot(stat != null && stat.getCustomData() != null && stat.getCustomData().length() == 8 ? stat.getCustomData() : null));
        //player.getStat().recalcLocalStats(false, player);
        if (player.getStat().equippedSummon > 0) {
            SkillFactory.getSkill(player.getStat().equippedSummon).getEffect(1).applyTo(player);
        }
        ConnectorClient cli = ConnectorServer.getInstance().getClientStorage().getClientByName(player.getClient().getAccountName());
        if (cli != null) {
            player.getClient().setconnecterClient(cli);
            ConnectorServer.getInstance().getClientStorage().registerChangeInGameCharWaiting(cli.toString(), cli.toString());
            if (player.getClient().getAccountName().equals(cli.getId())) {
                cli.setChar(player);
            } else if (player.getClient().getAccountName().equals(cli.getSecondId())) {
                cli.setSecondChar(player);
            }
            cli.addInGameChar(player.getName(), player.getName());
        }
        if (player.getGuild() != null) { // 아니구나 이건데 잠만여
            player.guild_buff = World.Guild.getGuildBuffLevel(0, player.getGuildId());
            player.hunter_buff = World.Guild.getGuildBuffLevel(1, player.getGuildId());
            player.guild_stat_watk = World.Guild.getGuildBuffStat(0, 0, player.getGuildId());
            player.guild_stat_matk = World.Guild.getGuildBuffStat(0, 1, player.getGuildId());
            player.guild_stat_boss = World.Guild.getGuildBuffStat(1, 2, player.getGuildId());
            String guildname = World.Guild.getName(player.getGuildId());
            if (player.guild_buff > 0) {
                player.dropMessage(5, "[길드] 당신이 속한 " + guildname + " 길드의 <길드의 가호> 효과로 (공격력 +" + player.guild_stat_watk + ", 마력 +" + player.guild_stat_matk + ") 만큼 상승합니다.");
            }
            if (player.hunter_buff > 0) {
                //player.dropMessage(5, "[길드] 당신이 속한 " + guildname + " 길드의 <사냥꾼의 가호> 효과로 (보스 공격력 +" + player.guild_stat_boss + "%) 만큼 상승합니다.");
            }
        }
      //  player.rosySymbol();
        player.guildUpdate();
        
        for (int boosterEquipItemID : GameConstants.boosterEquipItemID) {
            int boosterItemID = 0;
            if ((boosterItemID = GameConstants.getBoosterItemID(boosterEquipItemID)) != 0) {
                MapleStatEffect boosterEff = MapleItemInformationProvider.getInstance().getItemEffect(boosterItemID);
                if (boosterEff != null) {
                    if (player.hasEquipped(boosterEquipItemID)) {
                        if (!player.getBuffedValue1(boosterItemID))
                            boosterEff.applyTo(c.getPlayer());
                    } else {
                        player.cancelEffect(boosterEff, -2);
                    }
                }
            }
        }
        
        if (newLogin) {
            Map<Integer, Long> getBuffs = BuffHandler.get().getBuffs(player.getId());
            if (getBuffs != null) {
                long cur = System.currentTimeMillis();
                for (Entry<Integer, Long> entry : getBuffs.entrySet()) {
                    int skillID = entry.getKey();
                    long end = entry.getValue();
                    long remain = end - cur;
                    if (remain < 1000) { //1초 보다 작을 시 
                        continue;
                    }
                    switch (skillID) {
                        //특정 아이템 스킬만
                        case 2450018: {
                            MapleStatEffect itemEff = MapleItemInformationProvider.getInstance().getItemEffect(skillID);
                            if (itemEff != null) {
                                itemEff.applyTo(player, (int) remain);
                            }
                            break;
                        }
                        case 9001008: {
                            Skill skill = SkillFactory.getSkill(skillID);
                            if (skill != null) {
                                MapleStatEffect eff = skill.getEffect(1);
                                if (eff != null) {
                                    eff.applyTo(player, (int) remain);
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
        
//        player.CustomStatEffect(false);
        player.customizeStat(0);
        
        if (ServerConstants.cubeDayValue > 1.0) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("[이벤트] 미라클 큐브 데이 이벤트가 진행 중입니다. " + ServerConstants.cubeDayValue + "배 등급업의 기회를 노려보세요!"));
        }
    }

    public static final void ChangeChannel(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || chr.hasBlockedInventory() || chr.getEventInstance() != null || chr.getMap() == null || chr.isInBlockedMap() || FieldLimitType.ChannelSwitch.check(chr.getMap().getFieldLimit())) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (World.getPendingCharacterSize() >= 10) {
            chr.dropMessage(1, "The server is busy at the moment. Please try again in a less than a minute.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        final int chc = slea.readByte();
        final int nextChc = (chc + 1);
        
    
        if (!World.isChannelAvailable(chc + 1)) {
            chr.dropMessage(1, "The channel is full at the moment.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        chr.changeChannel(chc + 1);
    }

}
