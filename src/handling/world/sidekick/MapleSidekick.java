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
package handling.world.sidekick;

import client.MapleBuffStat;
import client.MapleCharacter;
import java.sql.Connection;
import database.DatabaseConnection;
import handling.world.MapleSidekickCharacter;
import handling.world.World;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import tools.MaplePacketCreator;
import tools.Pair;

public class MapleSidekick implements Serializable {

    private static final long serialVersionUID = 954199343336738569L;
    private MapleSidekickCharacter[] sidekicks = new MapleSidekickCharacter[2];
    private int id;

    public MapleSidekick(int sid) {
        this.id = sid;
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        PreparedStatement ps2 = null;
        ResultSet rs2 = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM sidekicks WHERE id = ?");
            ps.setInt(1, sid);
            rs = ps.executeQuery();

            if (!rs.first()) {
                rs.close();
                ps.close();
                id = -1;
                return;
            }
            ps2 = con.prepareStatement("SELECT id, name, level, job, mapid FROM characters WHERE id = ? OR id = ?");
            ps.setInt(1, rs.getInt("firstid"));
            ps.setInt(2, rs.getInt("secondid"));
            rs2 = ps.executeQuery();
            while (rs2.next()) {
                sidekicks[rs2.getInt("id") == rs.getInt("firstid") ? 0 : 1] = new MapleSidekickCharacter(rs2.getInt("id"), rs2.getString("name"), rs2.getInt("level"), rs2.getInt("job"), rs2.getInt("mapid"));
            }
            rs2.close();
            ps2.close();

            if (sidekicks[0] == null || sidekicks[1] == null || !checkLevels(sidekicks[0].getLevel(), sidekicks[1].getLevel())) {
                id = -1;
                eraseToDB();
                return;
            }
        } catch (SQLException se) {
            System.err.println("unable to read sidekick information from sql");
            se.printStackTrace();
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }

            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }

            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }

            if (ps2 != null) {
                try {
                    ps2.close();
                } catch (Exception e) {

                }
            }

            if (rs2 != null) {
                try {
                    rs2.close();
                } catch (Exception e) {

                }
            }
        }
    }

    public void broadcast(byte[] packet) {
        World.Broadcast.sendPacket(sidekicks[0].getId(), packet);
        World.Broadcast.sendPacket(sidekicks[1].getId(), packet);
    }

    public void eraseToDB() {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("DELETE FROM sidekicks WHERE id = ?");
            ps.setInt(1, id);
            ps.executeUpdate();
            ps.close();

//            broadcast(MaplePacketCreator.disbandSidekick(this));
            World.Sidekick.eraseSidekick(id);
        } catch (SQLException se) {
            System.err.println("Error deleting sidekick");
            se.printStackTrace();
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
    }

    public void applyBuff(MapleCharacter chr) { //79797980? magic number
        final Map<MapleBuffStat, Integer> effects = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
        final int levelD = Math.abs(getCharacter(0).getLevel() - getCharacter(1).getLevel());
        effects.put(MapleBuffStat.STR, levelD * 5);
        effects.put(MapleBuffStat.DEX, levelD * 5);
        effects.put(MapleBuffStat.INT, levelD * 5);
        effects.put(MapleBuffStat.LUK, levelD * 5);
        effects.put(MapleBuffStat.SIDEKICK_PASSIVE, 1);

        chr.getClient().getSession().write(MaplePacketCreator.giveBuff(79797980, 2100000000, effects, null));
        final MapleStatEffect eff = MapleItemInformationProvider.getInstance().getItemEffect(2022891); //sidekick power
        chr.cancelEffectFromBuffStat(MapleBuffStat.SIDEKICK_PASSIVE);
        chr.registerEffect(eff, System.currentTimeMillis(), null, effects, false, 2100000000, chr.getId());
    }

    public static final int create(final int leaderId, final int leaderId2) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT id FROM sidekicks WHERE firstid = ? OR secondid = ? OR firstid = ? OR secondid = ?");
            ps.setInt(1, leaderId);
            ps.setInt(2, leaderId2);
            ps.setInt(3, leaderId);
            ps.setInt(4, leaderId2);
            rs = ps.executeQuery();

            if (rs.first()) {// taken
                rs.close();
                ps.close();
                return 0;
            }
            ps.close();
            rs.close();

            ps = con.prepareStatement("INSERT INTO sidekicks (firstid, secondid) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, leaderId);
            ps.setInt(2, leaderId2);
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            int ret = 0;
            if (rs.next()) {
                ret = rs.getInt(1);
            }
            rs.close();
            ps.close();
            return ret;
        } catch (SQLException se) {
            System.err.println("SQL THROW");
            se.printStackTrace();
            return 0;
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }

            if (rs != null) {
                try {
                    rs.close();
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
    }

    public static List<MapleSidekick> loadAll() {
        List<MapleSidekick> ret = new ArrayList<MapleSidekick>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT id FROM sidekicks");
            rs = ps.executeQuery();
            while (rs.next()) {
                ret.add(new MapleSidekick(rs.getInt("id")));
            }
            rs.close();
            ps.close();
        } catch (SQLException se) {
            System.err.println("unable to read sidekick information from sql");
            se.printStackTrace();
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }

            if (rs != null) {
                try {
                    rs.close();
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
        return ret;
    }

    public List<String> getSidekickMsg(boolean online) {
        final List<String> ret = new ArrayList<String>();
        if (!online) {
            ret.add("You may only get benefits from the sidekick if they are in the same map.");
        }
        if (getCharacter(0).getLevel() > 140 || getCharacter(1).getLevel() > 140) {
            ret.add("The sidekick relationship will end if one player gets above level 150.");
        }
        if (Math.abs(getCharacter(0).getLevel() - getCharacter(1).getLevel()) < 5 || Math.abs(getCharacter(0).getLevel() - getCharacter(1).getLevel()) > 30) {
            ret.add("The sidekick relationship will end if the level difference is less than 5 or greater than 30.");
        }
        return ret;
    }

    public static boolean checkLevels(int level1, int level2) {
        if (Math.abs(level1 - level2) < 5 || Math.abs(level1 - level2) > 30 || level1 > 150 || level2 > 150 || level1 < 10 || level2 < 10) {
            return false;
        }
        return true;
    }

    public int getId() {
        return id;
    }

    public MapleSidekickCharacter getCharacter(int index) {
        return sidekicks[index];
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MapleSidekick other = (MapleSidekick) obj;
        if (id != other.id) {
            return false;
        }
        return true;
    }
}
