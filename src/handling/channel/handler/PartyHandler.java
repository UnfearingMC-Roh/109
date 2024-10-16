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
import constants.GameConstants;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.PartyOperation;
import handling.world.World;
import handling.world.exped.ExpeditionType;
import handling.world.exped.MapleExpedition;
import handling.world.exped.PartySearch;
import handling.world.exped.PartySearchType;
import handling.world.sidekick.MapleSidekick;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import server.maps.Event_DojoAgent;
import server.maps.FieldLimitType;
import server.maps.MapleDoor;
import server.quest.MapleQuest;
import tools.MaplePacketCreator;
import tools.data.LittleEndianAccessor;

public class PartyHandler {

    public static final void DenyPartyRequest(final LittleEndianAccessor slea, final MapleClient c) {
        final int action = slea.readByte();
        final int partyid = slea.readInt();
        if (c.getPlayer().getParty() == null && c.getPlayer().getQuestNoAdd(MapleQuest.getInstance(GameConstants.PARTY_INVITE)) == null) {
            MapleParty party = World.Party.getParty(partyid);
            if (party != null) {
//                if (party.getExpeditionId() > 0) {
//                    c.getPlayer().dropMessage(5, "You may not do party operations while in a raid.");
//                    return;
//                }
                if (action == 0x1B) { //accept
                    if (party.getMembers().size() < 6) {
                        c.getPlayer().setParty(party);
                        World.Party.updateParty(partyid, PartyOperation.JOIN, new MaplePartyCharacter(c.getPlayer()));
                        c.getPlayer().receivePartyMemberHP();
                        c.getPlayer().updatePartyMemberHP();
                    } else {
                        c.getSession().write(MaplePacketCreator.partyStatusMessage(17));
                    }
                } else if (action != 0x16) {
                    final MapleCharacter cfrom = c.getChannelServer().getPlayerStorage().getCharacterById(party.getLeader().getId());
                    if (cfrom != null) {
                        cfrom.getClient().getSession().write(MaplePacketCreator.partyStatusMessage(23, c.getPlayer().getName()));
                    }
                }
            } else {
                c.getPlayer().dropMessage(5, "가입하려는 파티가 존재하지 않습니다.");
            }
        } else {
            c.getPlayer().dropMessage(5, "파티에 이미 가입된 상태로는 가입할 수 없습니다.");
        }
    }

    public static final void PartyOperation(final LittleEndianAccessor slea, final MapleClient c) {
        final int operation = slea.readByte();
        MapleParty party = c.getPlayer().getParty();
        MaplePartyCharacter partyplayer = new MaplePartyCharacter(c.getPlayer());
        switch (operation) {
            case 1: // create
                if (party == null) {
                    party = World.Party.createParty(partyplayer);
                    c.getPlayer().setParty(party);
                    c.getSession().write(MaplePacketCreator.partyCreated(party.getId()));

                    if (c.getPlayer().getDoors().size() == 2) {
                        try {
                            MapleDoor door1 = c.getPlayer().getDoors().get(0);
                            MapleDoor door2 = c.getPlayer().getDoors().get(1);
                            //door1.joinPartyElseDoorOwner(c);
                            //door2.joinPartyElseDoorOwner(c);
                        } catch (ArrayIndexOutOfBoundsException e) {
                        }
                    }

                } else if (partyplayer.equals(party.getLeader()) && party.getMembers().size() == 1) { //only one, reupdate
                    c.getSession().write(MaplePacketCreator.partyCreated(party.getId()));
                } else {
                    c.getPlayer().dropMessage(5, "파티에 이미 가입된 상태로는 가입할 수 없습니다.");
                }
                break;
            case 2: // leave
                if (party != null) { //are we in a party? o.O"
                    if (party.getExpeditionId() > 0) {
                        final MapleExpedition exped = World.Party.getExped(party.getExpeditionId());
                        if (exped != null) {
                            if (exped.getLeader() == c.getPlayer().getId()) { // disband
                                World.Party.expedPacket(exped.getId(), MaplePacketCreator.serverNotice(5, "원정대가 해체되었습니다."), null);
                                World.Party.expedPacket(exped.getId(), MaplePacketCreator.expeditionRemove(c.getPlayer().getName()), null);
                                World.Party.disbandExped(exped.getId()); //should take care of the rest
                            } else if (party.getLeader().getId() == c.getPlayer().getId()) {
                                World.Party.expedPacket(exped.getId(), MaplePacketCreator.expeditionUpdate(c, exped.getIndex(c.getPlayer().getParty().getId()), c.getPlayer().getParty()), null);
                                World.Party.updateParty(party.getId(), PartyOperation.LEAVE, new MaplePartyCharacter(c.getPlayer()));
                                World.Party.expedPacket(exped.getId(), MaplePacketCreator.expeditionLeft(c.getPlayer().getName()), null);
                                c.getSession().write(MaplePacketCreator.expeditionLeftOwn());
                            } else {
                                World.Party.expedPacket(exped.getId(), MaplePacketCreator.expeditionUpdate(c, exped.getIndex(c.getPlayer().getParty().getId()), c.getPlayer().getParty()), null);
                                World.Party.updateParty(party.getId(), PartyOperation.LEAVE, new MaplePartyCharacter(c.getPlayer()));
                                World.Party.expedPacket(exped.getId(), MaplePacketCreator.expeditionLeft(c.getPlayer().getName()), null);
                                c.getSession().write(MaplePacketCreator.expeditionLeftOwn());
                            }
                        }
                    }
                    if (partyplayer.equals(party.getLeader())) { // disband
                        if (c.getPlayer().getPyramidSubway() != null) {
                            c.getPlayer().getPyramidSubway().fail(c.getPlayer());
                        }
                        World.Party.updateParty(party.getId(), PartyOperation.DISBAND, partyplayer);
                        if (c.getPlayer().getEventInstance() != null) {
                            c.getPlayer().getEventInstance().disbandParty();
                        }
                    } else {
                        if (c.getPlayer().getPyramidSubway() != null) {
                            c.getPlayer().getPyramidSubway().fail(c.getPlayer());
                        }
                        World.Party.updateParty(party.getId(), PartyOperation.LEAVE, partyplayer);
                        if (c.getPlayer().getEventInstance() != null) {
                            c.getPlayer().getEventInstance().leftParty(c.getPlayer());
                        }
                    }
                    if (c.getPlayer().getDoors().size() == 2) {
                        try {
                            //c.getPlayer().getDoors().get(0).sendSinglePortal();
                            //c.getPlayer().getDoors().get(1).sendSinglePortal();
                        } catch (ArrayIndexOutOfBoundsException e) {
                        }
                    }
                    c.getPlayer().setParty(null);
                    
                    MapleBuffStat[] auras = {MapleBuffStat.DARK_AURA, MapleBuffStat.BLUE_AURA, MapleBuffStat.YELLOW_AURA};
                    for (MapleBuffStat auraStat : auras) {
                        //내 캐릭터가 오라 버프 적용 중이고, 오라 버프 준 캐릭터가 존재할 때
                        int buffLeaderID = c.getPlayer().getBuffLeaderID(auraStat);
                        if (c.getPlayer().getStatForBuff(auraStat) != null && buffLeaderID != -1) {
                            if (c.getPlayer().getId() != buffLeaderID) //내 캐릭터가 버프 준 캐릭터가 아닐 때만
                                c.getPlayer().cancelBuffStats(true, auraStat); //버프 캔슬
                        }
                    }
                }
                break;
            case 3: // accept invitation
                final int partyid = slea.readInt();
                if (party == null) {
                    party = World.Party.getParty(partyid);
                    if (party != null) {
                        if (party.getMembers().size() < 6 && c.getPlayer().getQuestNoAdd(MapleQuest.getInstance(GameConstants.PARTY_INVITE)) == null) {
                            boolean sameIP = false;
                            for (MaplePartyCharacter pchr : party.getMembers()) {
                                if (pchr != null) {
                                    final int theCh = World.Find.findChannel(pchr.getName());
                                    if (theCh > 0) {
                                        MapleCharacter chr = ChannelServer.getInstance(theCh).getPlayerStorage().getCharacterByName(pchr.getName());
                                        if (chr != null) {
                                            if (chr.getClient().getSessionIPAddress().equalsIgnoreCase(c.getSessionIPAddress())) {
                                                sameIP = true;
                                            }
                                        }
                                    }
                                    
                                    Connection con = null;
                                    PreparedStatement ps = null;
                                    ResultSet rs = null;
                                    try {
                                        con = DatabaseConnection.getConnection();
                                        ps = con.prepareStatement("SELECT accountid FROM characters WHERE `id` = ?");
                                        ps.setInt(1, pchr.getId());
                                        rs = ps.executeQuery();
                                        
                                        int accID = -1;
                                        if (rs.next()) {
                                            accID = rs.getInt("accountid");
                                        }
                                        rs.close();
                                        ps.close();
                                        if (accID != -1) {
                                            ps = con.prepareStatement("SELECT `SessionIP` FROM accounts WHERE `id` = ?");
                                            ps.setInt(1, accID);
                                            rs = ps.executeQuery();
                                            
                                            if (rs.next()) {
                                                if (rs.getString("SessionIP").equalsIgnoreCase(c.getSessionIPAddress())) {
                                                    sameIP = true;
                                                }
                                            }
                                            rs.close();
                                            ps.close();
                                        }
                                        con.close();
                                    } catch (SQLException e) {
                                        e.printStackTrace(System.err);
                                    } finally {
                                        if (rs != null) {
                                            try { rs.close(); } catch (SQLException e) { }
                                        }
                                        if (ps != null) {
                                            try { ps.close(); } catch (SQLException e) { }
                                        }
                                        if (con != null) {
                                            try { con.close(); } catch (SQLException e) { }
                                        }
                                    }
                                }
                                
                                if (sameIP) break;
                            }
                            if (sameIP) {
                                c.getPlayer().dropMessage(5, "부계정 캐릭터가 있는 파티에는 참여할 수 없습니다.");
                            } else {
                                c.getPlayer().setParty(party);
                                World.Party.updateParty(party.getId(), PartyOperation.JOIN, partyplayer);
                                c.getPlayer().receivePartyMemberHP();
                                c.getPlayer().updatePartyMemberHP();
                            }
                        } else {
                            c.getSession().write(MaplePacketCreator.partyStatusMessage(17));
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "해당 파티는 존재하지 않습니다.");
                    }
                } else {
                    c.getPlayer().dropMessage(5, "이미 파티에 가입되어 있습니다.");
                }
                break;
            case 4: // invite
                if (party == null) {
                    party = World.Party.createParty(partyplayer);
                    c.getPlayer().setParty(party);
                    c.getSession().write(MaplePacketCreator.partyCreated(party.getId()));
                    return;
                }
                
                if (GameConstants.isExpeditionMap(c.getPlayer().getMapId())) {
                    c.getPlayer().dropMessage(1, "원정대 맵에서는 파티 초대 기능을 사용 할 수 없습니다.");
                    return;
                }
                
                if (party.getExpeditionId() > 0) {
                    final MapleExpedition exped = World.Party.getExped(party.getExpeditionId());
                    if (exped != null) {
                        c.getPlayer().dropMessage(5, "원정대에 속해 있을 때는 초대할 수 없습니다.");
                        return;
                    }
                }
                // TODO store pending invitations and check against them
                final String theName = slea.readMapleAsciiString();
                final int theCh = World.Find.findChannel(theName);
                if (theCh > 0) {
                    final MapleCharacter invited = ChannelServer.getInstance(theCh).getPlayerStorage().getCharacterByName(theName);
                    if (invited != null && invited.getParty() == null && invited.getQuestNoAdd(MapleQuest.getInstance(GameConstants.PARTY_INVITE)) == null) {
//                        if (party.getExpeditionId() > 0) {
//                            c.getPlayer().dropMessage(5, "You may not do party operations while in a raid.");
//                            return;
//                        }
                        if (party.getMembers().size() < 6) {
//                            if (!c.getPlayer().isGM() && invited.getClient().getSessionIPAddress().equalsIgnoreCase(c.getSessionIPAddress())) {
//                                c.getPlayer().dropMessage(5, "동일한 IP를 가진 캐릭터는 파티에 초대할 수 없습니다.");
//                            } else {
                                c.getSession().write(MaplePacketCreator.partyStatusMessage(22, invited.getName()));
                                invited.getClient().getSession().write(MaplePacketCreator.partyInvite(c.getPlayer(), false));
//                            }
                        } else {
                            c.getSession().write(MaplePacketCreator.partyStatusMessage(16));
                        }
                    } else {
                        c.getSession().write(MaplePacketCreator.partyStatusMessage(17));
                    }
                } else {
                    c.getSession().write(MaplePacketCreator.partyStatusMessage(19));
                }
                break;
            case 5: // expel
                if (party != null && partyplayer != null && partyplayer.equals(party.getLeader())) {
                    final MaplePartyCharacter expelled = party.getMemberById(slea.readInt());

                    if (expelled != null) {
                        if (expelled.getId() == c.getPlayer().getId()) {
                            c.getPlayer().dropMessage(5, "자기 자신을 강퇴할 수 없습니다.");
                            break;
                        }
                        if (c.getPlayer().getPyramidSubway() != null && expelled.isOnline()) {
                            c.getPlayer().getPyramidSubway().fail(c.getPlayer());
                        }
                        World.Party.updateParty(party.getId(), PartyOperation.EXPEL, expelled);
                        if (c.getPlayer().getEventInstance() != null) {
                            /*if leader wants to boot someone, then the whole party gets expelled
                             TODO: Find an easier way to get the character behind a MaplePartyCharacter
                             possibly remove just the expellee.*/
                            if (expelled.isOnline()) {
                                c.getPlayer().getEventInstance().disbandParty();
                            }
                        }
                    }
                }
                MapleBuffStat[] auras = {MapleBuffStat.DARK_AURA, MapleBuffStat.BLUE_AURA, MapleBuffStat.YELLOW_AURA};
                    for (MapleBuffStat auraStat : auras) {
                        //내 캐릭터가 오라 버프 적용 중이고, 오라 버프 준 캐릭터가 존재할 때
                        int buffLeaderID = c.getPlayer().getBuffLeaderID(auraStat);
                        if (c.getPlayer().getStatForBuff(auraStat) != null && buffLeaderID != -1) {
                            if (c.getPlayer().getId() != buffLeaderID) //내 캐릭터가 버프 준 캐릭터가 아닐 때만
                                c.getPlayer().cancelBuffStats(true, auraStat); //버프 캔슬
                        }
                    }
                break;
            case 6: // change leader
                if (party != null) {
                    final MaplePartyCharacter newleader = party.getMemberById(slea.readInt());
                    if (newleader != null && partyplayer.equals(party.getLeader())) {
                        if (newleader.isOnline()) {
                            if (newleader.getChannel() == partyplayer.getChannel()) {
                                if (newleader.getMapid() == partyplayer.getMapid()) {
                                    World.Party.updateParty(party.getId(), PartyOperation.CHANGE_LEADER, newleader);
                                } else {
                                    c.getSession().write(MaplePacketCreator.partyStatusMessage(27)); //같은 장소에 있는 파티원에게만 양도할 수 있습니다.
                                }
                            } else {
                                c.getSession().write(MaplePacketCreator.partyStatusMessage(29)); //같은 장소에 있는 파티원에게만 양도할 수 있습니다.

                            }
                        } else {
                            c.getSession().write(MaplePacketCreator.partyStatusMessage(28)); //같은 장소에 있는 파티원에게만 양도할 수 있습니다.
                        }
                    }
                }
                break;

            //<editor-fold defaultstate="collapsed" desc="After BB Functions">
//            case 7: //request to  join a party
//                if (party != null) {
//                    if (c.getPlayer().getEventInstance() != null || c.getPlayer().getPyramidSubway() != null || party.getExpeditionId() > 0 || GameConstants.isDojo(c.getPlayer().getMapId())) {
//                        c.getPlayer().dropMessage(5, "You may not do party operations while in a raid.");
//                        return;
//                    }
//                    if (partyplayer.equals(party.getLeader())) { // disband
//                        World.Party.updateParty(party.getId(), PartyOperation.DISBAND, partyplayer);
//                    } else {
//                        World.Party.updateParty(party.getId(), PartyOperation.LEAVE, partyplayer);
//                    }
//                    c.getPlayer().setParty(null);
//                }
//                final int partyid_ = slea.readInt();
//                if (GameConstants.GMS) {
//                    //TODO JUMP
//                    party = World.Party.getParty(partyid_);
//                    if (party != null && party.getMembers().size() < 6) {
//                        if (party.getExpeditionId() > 0) {
//                            c.getPlayer().dropMessage(5, "You may not do party operations while in a raid.");
//                            return;
//                        }
//                        final MapleCharacter cfrom = c.getPlayer().getMap().getCharacterById(party.getLeader().getId());
//                        if (cfrom != null && cfrom.getQuestNoAdd(MapleQuest.getInstance(GameConstants.PARTY_REQUEST)) == null) {
//                            c.getSession().writeAndFlush(MaplePacketCreator.partyStatusMessage(50, c.getPlayer().getName()));
//                            cfrom.getClient().getSession().writeAndFlush(MaplePacketCreator.partyRequestInvite(c.getPlayer()));
//                        } else {
//                            c.getPlayer().dropMessage(5, "Player was not found or player is not accepting party requests.");
//                        }
//                    }
//                }
//                break;
//            case 8: //allow party requests
//                if (slea.readByte() > 0) {
//                    c.getPlayer().getQuestRemove(MapleQuest.getInstance(GameConstants.PARTY_REQUEST));
//                } else {
//                    c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.PARTY_REQUEST));
//                }
//                break;
            //</editor-fold>
            default:
                System.out.println("Unhandled Party function." + operation);
                break;
        }
    }

    public static final void AllowPartyInvite(final LittleEndianAccessor slea, final MapleClient c) {
        if (slea.readByte() > 0) {
            c.getPlayer().getQuestRemove(MapleQuest.getInstance(GameConstants.PARTY_INVITE));
        } else {
            c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.PARTY_INVITE));
        }
    }

    public static final void DenySidekickRequest(final LittleEndianAccessor slea, final MapleClient c) {
        final int action = slea.readByte();
        final int cid = slea.readInt();
        if (c.getPlayer().getSidekick() == null && action == 0x5A) { //accept
            MapleCharacter party = c.getPlayer().getMap().getCharacterById(cid);
            if (party != null) {
                if (party.getSidekick() != null || !MapleSidekick.checkLevels(c.getPlayer().getLevel(), party.getLevel())) {
                    return;
                }
                int sid = World.Sidekick.createSidekick(c.getPlayer().getId(), party.getId());
                if (sid <= 0) {
                    c.getPlayer().dropMessage(5, "Please try again.");
                } else {
                    MapleSidekick s = World.Sidekick.getSidekick(sid);
                    c.getPlayer().setSidekick(s);
//                    c.getSession().write(MaplePacketCreator.updateSidekick(c.getPlayer(), s, true));
                    party.setSidekick(s);
                    //          party.getClient().getSession().write(MaplePacketCreator.updateSidekick(party, s, true));
                }
            } else {
                c.getPlayer().dropMessage(5, "The sidekick you are trying to join does not exist");
            }
        }

    }

    public static final void SidekickOperation(final LittleEndianAccessor slea, final MapleClient c) {
        final int operation = slea.readByte();

        switch (operation) {
            case 0x41: // create
                if (c.getPlayer().getSidekick() == null) {
                    final MapleCharacter other = c.getPlayer().getMap().getCharacterByName(slea.readMapleAsciiString());
                    if (other.getSidekick() == null && MapleSidekick.checkLevels(c.getPlayer().getLevel(), other.getLevel())) {
//                        other.getClient().getSession().write(MaplePacketCreator.sidekickInvite(c.getPlayer()));
                        c.getPlayer().dropMessage(1, "You have sent the sidekick invite to " + other.getName() + ".");
                    }
                }
                break;
            case 0x3F: // leave
                if (c.getPlayer().getSidekick() != null) {
                    c.getPlayer().getSidekick().eraseToDB();
                }
                break;
        }
    }

    public static final void PartySearchStart(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer().isInBlockedMap() || FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit())) {
            c.getPlayer().dropMessage(5, "이 곳에서 파티 찾기를 하실 수 없습니다.");
            return;
        } else if (GameConstants.GMS) {
            //replaced with member search
            //  c.getSession().write(MaplePacketCreator.showMemberSearch(c.getPlayer().getMap().getCharactersThreadsafe()));
            return;
        } else if (c.getPlayer().getParty() == null) {
            MapleParty party = World.Party.createParty(new MaplePartyCharacter(c.getPlayer()));
            c.getPlayer().setParty(party);
            c.getSession().write(MaplePacketCreator.partyCreated(party.getId()));
        } else {
            if (c.getPlayer().getParty().getExpeditionId() > 0) {
                c.getPlayer().dropMessage(5, "원정대를 탈퇴하기 전 파티를 탈퇴 할 수 없습니다.");
                return;
            }
        }
        final int min = slea.readInt();
        final int max = slea.readInt();
        final int members = slea.readInt();
        final int jobs = slea.readInt();
        final List<Integer> jobsList = new ArrayList<Integer>();
        if (max <= min || max - min > 30 || members > 6 || min > c.getPlayer().getLevel() || max < c.getPlayer().getLevel() || jobs == 0) {
            c.getPlayer().dropMessage(1, "An error occurred.");
            return;
        }
        //all jobs = FF FF F7 0F
        //GMS - FF FF DF 7F!, no pirates = FE 7F D0 7F
        if ((jobs & 0x1) != 0) {
            //all jobs? skip check or what
            c.getPlayer().startPartySearch(jobsList, max, min, members);
            return;
        }
        if ((jobs & 0x2) != 0) { //beginner
            jobsList.add(0);
            jobsList.add(1);
            jobsList.add(1000);
            jobsList.add(2000);
            jobsList.add(2001);
            jobsList.add(3000);
        }
        if ((jobs & 0x4) != 0) { //aran
            jobsList.add(2100);
            jobsList.add(2110);
            jobsList.add(2111);
            jobsList.add(2112);
        }
        if ((jobs & 0x8) != 0) { //evan
            jobsList.add(2200);
            jobsList.add(2210);
            jobsList.add(2211);
            jobsList.add(2212);
            jobsList.add(2213);
            jobsList.add(2214);
            jobsList.add(2215);
            jobsList.add(2216);
            jobsList.add(2217);
            jobsList.add(2218);
        }
        if ((jobs & 0x10) != 0) { //swordman
            jobsList.add(100);
        }
        if ((jobs & 0x20) != 0) { //crusader
            jobsList.add(110);
            jobsList.add(111);
            jobsList.add(112);
        }
        if ((jobs & 0x40) != 0) { //knight
            jobsList.add(120);
            jobsList.add(121);
            jobsList.add(122);
        }
        if ((jobs & 0x80) != 0) { //dk
            jobsList.add(130);
            jobsList.add(131);
            jobsList.add(132);
        }
        if ((jobs & 0x100) != 0) { //soul
            jobsList.add(1100);
            jobsList.add(1110);
            jobsList.add(1111);
            jobsList.add(1112);
        }
        if ((jobs & 0x200) != 0) { //mage
            jobsList.add(200);
        }
        if ((jobs & 0x400) != 0) { //fp
            jobsList.add(210);
            jobsList.add(211);
            jobsList.add(212);
        }
        if ((jobs & 0x800) != 0) { //il
            jobsList.add(220);
            jobsList.add(221);
            jobsList.add(222);
        }
        if ((jobs & 0x1000) != 0) { //priest
            jobsList.add(230);
            jobsList.add(231);
            jobsList.add(232);
        }
        if ((jobs & 0x2000) != 0) { //flame
            jobsList.add(1200);
            jobsList.add(1210);
            jobsList.add(1211);
            jobsList.add(1212);
        }
        if ((jobs & 0x4000) != 0) { //battle mage <-- new
            jobsList.add(3200);
            jobsList.add(3210);
            jobsList.add(3211);
            jobsList.add(3212);
        }
        if ((jobs & 0x8000) != 0) { //pirate
            jobsList.add(500);
            jobsList.add(501);
        }
        if ((jobs & 0x10000) != 0) { //viper
            jobsList.add(510);
            jobsList.add(511);
            jobsList.add(512);
        }
        if ((jobs & 0x20000) != 0) { //gs
            jobsList.add(520);
            jobsList.add(521);
            jobsList.add(522);
        }
        if ((jobs & 0x40000) != 0) { //strikr
            jobsList.add(1500);
            jobsList.add(1510);
            jobsList.add(1511);
            jobsList.add(1512);
        }
        if ((jobs & 0x80000) != 0) { //mechanic <-- new
            jobsList.add(3500);
            jobsList.add(3510);
            jobsList.add(3511);
            jobsList.add(3512);
        }
        if ((jobs & 0x100000) != 0) { //teef
            jobsList.add(400);
        }
        //0x200000 doesn't exist in gms
        if ((jobs & 0x400000) != 0) { //hermit
            jobsList.add(410);
            jobsList.add(411);
            jobsList.add(412);
        }
        if ((jobs & 0x800000) != 0) { //cb
            jobsList.add(420);
            jobsList.add(421);
            jobsList.add(422);
        }
        if ((jobs & 0x1000000) != 0) { //nw
            jobsList.add(1400);
            jobsList.add(1410);
            jobsList.add(1411);
            jobsList.add(1412);
        }
        if ((jobs & 0x2000000) != 0) { //db
            jobsList.add(430);
            jobsList.add(431);
            jobsList.add(432);
            jobsList.add(433);
            jobsList.add(434);
        }
        if ((jobs & 0x4000000) != 0) { //archer
            jobsList.add(300);
        }
        if ((jobs & 0x8000000) != 0) { //ranger
            jobsList.add(310);
            jobsList.add(311);
            jobsList.add(312);
        }
        if ((jobs & 0x10000000) != 0) { //sniper
            jobsList.add(320);
            jobsList.add(321);
            jobsList.add(322);
        }
        if ((jobs & 0x20000000) != 0) { //wind breaker
            jobsList.add(1300);
            jobsList.add(1310);
            jobsList.add(1311);
            jobsList.add(1312);
        }
        if ((jobs & 0x40000000) != 0) { //wild hunter <-- new
            jobsList.add(3300);
            jobsList.add(3310);
            jobsList.add(3311);
            jobsList.add(3312);
        }
        if (jobsList.size() > 0) {
            c.getPlayer().startPartySearch(jobsList, max, min, members);
        } else {
            c.getPlayer().dropMessage(1, "An error occurred.");
        }
    }

    public static final void PartySearchStop(final LittleEndianAccessor slea, final MapleClient c) {
        if (GameConstants.GMS) {
            List<MapleParty> parties = new ArrayList<MapleParty>();
            for (MapleCharacter chr : c.getPlayer().getMap().getCharactersThreadsafe()) {
                if (chr.getParty() != null && chr.getParty().getId() != c.getPlayer().getParty().getId() && !parties.contains(chr.getParty())) {
                    parties.add(chr.getParty());
                }
            }
            //     c.getSession().write(MaplePacketCreator.showPartySearch(parties));
        }
    }

    public static final void PartyListing(final LittleEndianAccessor slea, final MapleClient c) {
        final int mode = slea.readByte();
        final int action = slea.readInt();
        if (c.getPlayer().isGM()) {
                        c.getPlayer().dropMessage(5, "현재 이 기능을 이용하실 수 없습니다.");
        }
        //PartySearchType pst;
        MapleParty party;
        switch (mode) {
            case 80: //make
                switch (action) {
                    default:
                        c.getPlayer().dropMessage(5, "현재 이 기능을 이용하실 수 없습니다.");
                        break;
                }
                break;
            }
/*            case 82: //display
            case 0xA1:
            case -95:
            case -103:
                pst = PartySearchType.getById(slea.readInt());
                if (pst == null || c.getPlayer().getLevel() > pst.maxLevel || c.getPlayer().getLevel() < pst.minLevel) {
                    return;
                }
                c.getSession().write(MaplePacketCreator.getPartyListing(pst));
                break;
            case 83: //close
            case 0xA2:
            case -94:
            case -102:
                break;
            case 84: //join
            case 0xA3:
            case -93:
            case -101:
                party = c.getPlayer().getParty();
                final MaplePartyCharacter partyplayer = new MaplePartyCharacter(c.getPlayer());
                if (party == null) { //are we in a party? o.O"
                    final int theId = slea.readInt();
                    party = World.Party.getParty(theId);
                    if (party != null) {
                        PartySearch ps = World.Party.getSearchByParty(party.getId());
                        if (ps != null && c.getPlayer().getLevel() <= ps.getType().maxLevel && c.getPlayer().getLevel() >= ps.getType().minLevel && party.getMembers().size() < 6) {
                            c.getPlayer().setParty(party);
                            World.Party.updateParty(party.getId(), PartyOperation.JOIN, partyplayer);
                            c.getPlayer().receivePartyMemberHP();
                            c.getPlayer().updatePartyMemberHP();
                        } else {
                            c.getSession().write(MaplePacketCreator.partyStatusMessage(17));
                        }
                    } else {
                        MapleExpedition exped = World.Party.getExped(theId);
                        if (exped != null) {
                            PartySearch ps = World.Party.getSearchByExped(exped.getId());
                            if (ps != null && c.getPlayer().getLevel() <= ps.getType().maxLevel && c.getPlayer().getLevel() >= ps.getType().minLevel && exped.getAllMembers() < exped.getType().maxMembers) {
                                int partyId = exped.getFreeParty();
                                if (partyId < 0) {
                                    c.getSession().write(MaplePacketCreator.partyStatusMessage(17));
                                } else if (partyId == 0) { //signal to make a new party
                                    party = World.Party.createPartyAndAdd(partyplayer, exped.getId());
                                    c.getPlayer().setParty(party);
                                    c.getSession().write(MaplePacketCreator.partyCreated(party.getId()));
                                    c.getSession().write(MaplePacketCreator.expeditionStatus(exped, true));
                                    World.Party.expedPacket(exped.getId(), MaplePacketCreator.expeditionJoined(c.getPlayer().getName()), null);
                                    World.Party.expedPacket(exped.getId(), MaplePacketCreator.expeditionUpdate(exped.getIndex(party.getId()), party), null);
                                } else {
                                    c.getPlayer().setParty(World.Party.getParty(partyId));
                                    World.Party.updateParty(partyId, PartyOperation.JOIN, partyplayer);
                                    c.getPlayer().receivePartyMemberHP();
                                    c.getPlayer().updatePartyMemberHP();
                                    c.getSession().write(MaplePacketCreator.expeditionStatus(exped, true));
                                    World.Party.expedPacket(exped.getId(), MaplePacketCreator.expeditionJoined(c.getPlayer().getName()), null);
                                }
                            } else {
                                c.getSession().write(MaplePacketCreator.expeditionError(0, c.getPlayer().getName()));
                            }
                        }
                    }
                }
                break;
            default:
                if (c.getPlayer().isGM()) {
                    System.out.println("Unknown PartyListing : " + mode + "\n" + slea);
                }
                break;
        }*/
    }

    public static final void Expedition(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer() == null || c.getPlayer().getMap() == null) {
            return;
        }
        final int mode = slea.readByte();
        MapleParty part, party;
        String name;
        switch (mode) {
            case 0x30: //create [PartySearchID]
            case 119:
                final ExpeditionType et = ExpeditionType.getById(slea.readInt());
                if (et != null && c.getPlayer().getParty() == null && c.getPlayer().getLevel() <= et.maxLevel && c.getPlayer().getLevel() >= et.minLevel) {
                    party = World.Party.createParty(new MaplePartyCharacter(c.getPlayer()), et.exped);
                    c.getPlayer().setParty(party);
                    c.getSession().write(MaplePacketCreator.partyCreated(party.getId()));
                    c.getSession().write(MaplePacketCreator.expeditionStatus(World.Party.getExped(party.getExpeditionId()), true));
                } else {
                    c.getSession().write(MaplePacketCreator.expeditionError(0, ""));
                }
                break;
            case 0x31: //invite [name]
            case 120:
                name = slea.readMapleAsciiString();
                final int theCh = World.Find.findChannel(name);
                if (theCh > 0) {
                    final MapleCharacter invited = ChannelServer.getInstance(theCh).getPlayerStorage().getCharacterByName(name);
                    party = c.getPlayer().getParty();
                    if (invited != null && invited.getParty() == null && party != null && party.getExpeditionId() > 0) {
                        MapleExpedition me = World.Party.getExped(party.getExpeditionId());
                        if (me != null && me.getAllMembers() < me.getType().maxMembers && invited.getLevel() <= me.getType().maxLevel && invited.getLevel() >= me.getType().minLevel) {
                            c.getSession().write(MaplePacketCreator.expeditionError(7, invited.getName()));
                            invited.getClient().getSession().write(MaplePacketCreator.expeditionInvite(c.getPlayer(), me.getType().exped));
                        } else {
                            c.getSession().write(MaplePacketCreator.expeditionError(3, invited.getName()));
                        }
                    } else {
                        c.getSession().write(MaplePacketCreator.expeditionError(2, name));
                    }
                } else {
                    c.getSession().write(MaplePacketCreator.expeditionError(0, name));
                }
                break;
            case 0x32: //accept invite [name] [int - 7, then int 8? lol.]
            case 121:
                name = slea.readMapleAsciiString();
                final int action = slea.readInt();
                final int theChh = World.Find.findChannel(name);
                if (theChh > 0) {
                    final MapleCharacter cfrom = ChannelServer.getInstance(theChh).getPlayerStorage().getCharacterByName(name);
                    if (cfrom != null && cfrom.getParty() != null && cfrom.getParty().getExpeditionId() > 0) {
                        party = cfrom.getParty();
                        MapleExpedition exped = World.Party.getExped(party.getExpeditionId());
                        if (exped != null && action == 8) {
                            if (c.getPlayer().getLevel() <= exped.getType().maxLevel && c.getPlayer().getLevel() >= exped.getType().minLevel && exped.getAllMembers() < exped.getType().maxMembers) {
                                int partyId = exped.getFreeParty();
                                if (partyId < 0) {
                                    c.getSession().write(MaplePacketCreator.partyStatusMessage(17));
                                } else if (partyId == 0) { //signal to make a new party
                                    party = World.Party.createPartyAndAdd(new MaplePartyCharacter(c.getPlayer()), exped.getId());
                                    c.getPlayer().setParty(party);
                                    c.getSession().write(MaplePacketCreator.partyCreated(party.getId()));
                                    c.getSession().write(MaplePacketCreator.expeditionStatus(exped, true));
                                    World.Party.expedPacket(exped.getId(), MaplePacketCreator.expeditionJoined(c.getPlayer().getName()), null);
                                    World.Party.expedPacket(exped.getId(), MaplePacketCreator.expeditionUpdate(exped.getIndex(party.getId()), party), null);
                                } else {
                                    c.getPlayer().setParty(World.Party.getParty(partyId));
                                    World.Party.updateParty(partyId, PartyOperation.JOIN, new MaplePartyCharacter(c.getPlayer()));
                                    c.getPlayer().receivePartyMemberHP();
                                    c.getPlayer().updatePartyMemberHP();
                                    c.getSession().write(MaplePacketCreator.expeditionStatus(exped, true));
                                    World.Party.expedPacket(exped.getId(), MaplePacketCreator.expeditionJoined(c.getPlayer().getName()), null);
                                }
                            } else {
                                c.getSession().write(MaplePacketCreator.expeditionError(3, cfrom.getName()));
                            }
                        } else if (action == 9) {
                            cfrom.getClient().getSession().write(MaplePacketCreator.partyStatusMessage(23, c.getPlayer().getName()));
                        }
                    }
                }
                break;
            case 0x33: //leaving
            case 122:
                part = c.getPlayer().getParty();
                if (part != null && part.getExpeditionId() > 0) {
                    final MapleExpedition exped = World.Party.getExped(part.getExpeditionId());
                    if (exped != null) {
                        if (GameConstants.isDojo(c.getPlayer().getMapId())) {
                            Event_DojoAgent.failed(c.getPlayer());
                        }
                        if (exped.getLeader() == c.getPlayer().getId()) { // disband
                            World.Party.disbandExped(exped.getId()); //should take care of the rest
                            if (c.getPlayer().getEventInstance() != null) {
                                c.getPlayer().getEventInstance().disbandParty();
                            }
                        } else if (part.getLeader().getId() == c.getPlayer().getId()) {
                            World.Party.updateParty(part.getId(), PartyOperation.DISBAND, new MaplePartyCharacter(c.getPlayer()));
                            if (c.getPlayer().getEventInstance() != null) {
                                c.getPlayer().getEventInstance().disbandParty();
                            }
                            //World.Party.expedPacket(exped.getId(), MaplePacketCreator.expeditionLeft(c.getPlayer().getName()), null);
                        } else {
                            World.Party.updateParty(part.getId(), PartyOperation.LEAVE, new MaplePartyCharacter(c.getPlayer()));
                            if (c.getPlayer().getEventInstance() != null) {
                                c.getPlayer().getEventInstance().leftParty(c.getPlayer());
                            }
                            World.Party.expedPacket(exped.getId(), MaplePacketCreator.expeditionLeft(c.getPlayer().getName()), null);
                        }
                        if (c.getPlayer().getPyramidSubway() != null) {
                            c.getPlayer().getPyramidSubway().fail(c.getPlayer());
                        }
                        c.getPlayer().setParty(null);
                    }
                }
                break;
            case 0x34: //kick [cid]
            case 123:
                part = c.getPlayer().getParty();
                if (part != null && part.getExpeditionId() > 0) {
                    final MapleExpedition exped = World.Party.getExped(part.getExpeditionId());
                    if (exped != null && exped.getLeader() == c.getPlayer().getId()) {
                        final int cid = slea.readInt();
                        for (int i : exped.getParties()) {
                            final MapleParty par = World.Party.getParty(i);
                            if (par != null) {
                                final MaplePartyCharacter expelled = par.getMemberById(cid);
                                if (expelled != null) {
                                    if (expelled.isOnline() && GameConstants.isDojo(c.getPlayer().getMapId())) {
                                        Event_DojoAgent.failed(c.getPlayer());
                                    }
                                    World.Party.updateParty(i, PartyOperation.EXPEL, expelled);
                                    if (c.getPlayer().getEventInstance() != null) {
                                        if (expelled.isOnline()) {
                                            c.getPlayer().getEventInstance().disbandParty();
                                        }
                                    }
                                    if (c.getPlayer().getPyramidSubway() != null && expelled.isOnline()) {
                                        c.getPlayer().getPyramidSubway().fail(c.getPlayer());
                                    }
                                    //World.Party.expedPacket(exped.getId(), MaplePacketCreator.expeditionLeft(expelled.getName()), null);
                                    World.Party.expedPacket(exped.getId(), MaplePacketCreator.expeditionKicked(expelled.getName()), null);// need to be tested
                                    break;
                                }
                            }
                        }
                    }
                }
                break;
            case 0x35: //give exped leader [cid]
            case 124:
                part = c.getPlayer().getParty();
                if (part != null && part.getExpeditionId() > 0) {
                    final MapleExpedition exped = World.Party.getExped(part.getExpeditionId());
                    if (exped != null && exped.getLeader() == c.getPlayer().getId()) {
                        final MaplePartyCharacter newleader = part.getMemberById(slea.readInt());
                        if (newleader != null) {
                            World.Party.updateParty(part.getId(), PartyOperation.CHANGE_LEADER, newleader);
                            exped.setLeader(newleader.getId());
                            World.Party.expedPacket(exped.getId(), MaplePacketCreator.expeditionLeaderChanged(0), null);
                        }
                    }
                }
                break;
            case 0x36: //give party leader [cid]
            case 125:
                part = c.getPlayer().getParty();
                if (part != null && part.getExpeditionId() > 0) {
                    final MapleExpedition exped = World.Party.getExped(part.getExpeditionId());
                    if (exped != null && exped.getLeader() == c.getPlayer().getId()) {
                        final int cid = slea.readInt();
                        for (int i : exped.getParties()) {
                            final MapleParty par = World.Party.getParty(i);
                            if (par != null) {
                                final MaplePartyCharacter newleader = par.getMemberById(cid);
                                if (newleader != null && par.getId() != part.getId()) {
                                    World.Party.updateParty(par.getId(), PartyOperation.CHANGE_LEADER, newleader);
                                }
                            }
                        }
                    }
                }
                break;
            case 0x37: //change party of diff player [partyIndexTo] [cid]
            case 126:
                part = c.getPlayer().getParty();
                if (part != null && part.getExpeditionId() > 0) {
                    final MapleExpedition exped = World.Party.getExped(part.getExpeditionId());
                    if (exped != null && exped.getLeader() == c.getPlayer().getId()) {
                        final int partyIndexTo = slea.readInt();
                        if (partyIndexTo < exped.getType().maxParty && partyIndexTo <= exped.getParties().size()) {
                            final int cid = slea.readInt();
                            for (int i : exped.getParties()) {
                                final MapleParty par = World.Party.getParty(i);
                                if (par != null) {
                                    final MaplePartyCharacter expelled = par.getMemberById(cid);
                                    if (expelled != null && expelled.isOnline()) {
                                        final MapleCharacter chr = World.getStorage(expelled.getChannel()).getCharacterById(expelled.getId());
                                        if (chr == null) {
                                            break;
                                        }
                                        if (partyIndexTo < exped.getParties().size()) { //already exists
                                            party = World.Party.getParty(exped.getParties().get(partyIndexTo));
                                            if (party == null || party.getMembers().size() >= 6) {
                                                c.getPlayer().dropMessage(5, "Invalid party.");
                                                break;
                                            }
                                        }
                                        if (GameConstants.isDojo(c.getPlayer().getMapId())) {
                                            Event_DojoAgent.failed(c.getPlayer());
                                        }
                                        World.Party.updateParty(i, PartyOperation.EXPEL, expelled);
                                        if (partyIndexTo < exped.getParties().size()) { //already exists
                                            party = World.Party.getParty(exped.getParties().get(partyIndexTo));
                                            if (party != null && party.getMembers().size() < 6) {
                                                World.Party.updateParty(party.getId(), PartyOperation.JOIN, expelled);
                                                chr.receivePartyMemberHP();
                                                chr.updatePartyMemberHP();
                                                chr.getClient().getSession().write(MaplePacketCreator.expeditionStatus(exped, true));
                                            }
                                        } else {
                                            party = World.Party.createPartyAndAdd(expelled, exped.getId());
                                            chr.setParty(party);
                                            chr.getClient().getSession().write(MaplePacketCreator.partyCreated(party.getId()));
                                            chr.getClient().getSession().write(MaplePacketCreator.expeditionStatus(exped, true));
                                            World.Party.expedPacket(exped.getId(), MaplePacketCreator.expeditionUpdate(exped.getIndex(party.getId()), party), null);
                                        }
                                        if (c.getPlayer().getEventInstance() != null) {
                                            if (expelled.isOnline()) {
                                                c.getPlayer().getEventInstance().disbandParty();
                                            }
                                        }
                                        if (c.getPlayer().getPyramidSubway() != null) {
                                            c.getPlayer().getPyramidSubway().fail(c.getPlayer());
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                break;
            default:
                if (c.getPlayer().isGM()) {
                    System.out.println("Unknown Expedition : " + mode + "\n" + slea);
                }
                break;
        }
    }
}
