/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package connector;

import client.LoginCryptoLegacy;
import client.MapleCharacter;
import constants.ServerConstants;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.world.World;
import java.awt.Point;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.table.DefaultTableModel;
import server.Randomizer;
import tools.FileoutputUtil;
import static tools.FileoutputUtil.CurrentReadable_Time;
import tools.MaplePacketCreator;
import tools.Triple;
import tools.data.LittleEndianAccessor;
import tools.data.MaplePacketLittleEndianWriter;

/**
 * @author SLFCG & 글귀
 */
public class ConnectorServerHandler {

    public static final void HandlePacket(final RecvPacketOpcode header, final LittleEndianAccessor slea, final ConnectorClient c) throws InterruptedException {
        DefaultTableModel model = null;
        switch (header) {
            case LOGIN_PASSWORD: {
                MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
                String id = slea.readMapleAsciiString2();
                String pw = slea.readMapleAsciiString2();
                List<String> macs = new ArrayList<>();
                StringBuilder sb = new StringBuilder();
                int count = slea.readInt();
                for (int i = 0; i < count; i++) {
                    String mac = slea.readMapleAsciiString2();
                    sb.append(mac).append("\r\n");
                    macs.add(mac);
                }
                String key = slea.readMapleAsciiString2();
                int res = login(id, pw, sb.toString(), key, c, false);
                mplew.write(SendPacketOpcode.LOGIN.getValue());
                mplew.write((byte) res);
                if (res == 2) {
                    c.setId(id);
                    c.setPasswrod(pw);
                    model = (DefaultTableModel) ConnectorPanel.jTable1.getModel();
                    model.addRow(new Object[]{c.getId() + ",", c.getPassword(), c.getSession2().remoteAddress(), "", c, c.getCharName()});
                    //model.setValueAt(id + ",", ConnectorPanel.getModelId(c.getSession().remoteAddress().toString()), 0);
                    ConnectorServer.getInstance().getClientStorage().registerClient(c, id);
                }
                mplew.writeMapleAsciiString2(c.getIP());
                Point p = c.getMyRESOLUTION();
                mplew.writeInt(p.x);
                mplew.writeInt(p.y);
                c.send(mplew.getPacket());
                ConnectorServer.getInstance().getClientStorage().broadcastPacket(PacketCreator.sendUserList());
                break;
            }
            case CRC_DETECT_BAN: {
                //sb.append(slea.readMapleAsciiString()).append("\r\n");
                //sb.append(slea.readMapleAsciiString("UTF-8")); 
                String Date = slea.readMapleAsciiString2();
                String Key = slea.readMapleAsciiString2();
                //System.out.println(Date);
                if (c.getId().equals("cebreiro")) {
                    return;
                }
                Ban(c, Date, Key);
                c.getSession().close();
                FileoutputUtil.log(FileoutputUtil.감지로그, "아이디 : " + c.getId() + "\r\n" + Date + "");
                //System.out.println("[" + Date + "] 아이디 : " + c.getId() + "\r\n" + sb.toString());
                break;
            }
            case AUTO_REGISTER: {
                String id = slea.readMapleAsciiString2();
                String pw = slea.readMapleAsciiString2();
                String email = slea.readMapleAsciiString2();
                List<String> macs = new ArrayList<>();
                StringBuilder sb = new StringBuilder();
                int count = slea.readInt();
                for (int i = 0; i < count; i++) {
                    String mac = slea.readMapleAsciiString2();
                    sb.append(mac).append("\r\n");
                    macs.add(mac);
                }
                byte gender = slea.readByte();
                String key = slea.readMapleAsciiString2();

                MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
                mplew.write(SendPacketOpcode.REGISTER.getValue());
                mplew.write(AccountRegister(c, id, pw, email, sb.toString(), gender, key));
                c.send(mplew.getPacket());
                break;
            }
            case PONG: {
                /*int q = slea.readInt();
                 int w = c.getPing();
                 if (System.currentTimeMillis() < (c.getPingTime() + 30 * 1000)) {
                 c.setPingTime(System.currentTimeMillis());
                 w ^= 0xA4773FEC;
                 if (q != w) {
                 //c.getSession().close();
                 ConnecterLog("핑 값 오류 ID : " + c.getId() + " 받은 핑 값 : " + q + " / " + w + " ", c);
                 }
                 } else {
                 c.getSession().close();
                 ConnecterLog("핑 타임 초과 ID : " + c.getId() + "", c);
                 }*/
                c.setPingTime(System.currentTimeMillis());
                break;
            }
            case CLOSE: {
                c.getSession().close();
                break;
            }
//            case CONNECTOR_CHAT: {
//                slea.seek(1L);
//                if (c.getChatTime() + 2 * 1000 < System.currentTimeMillis()) {
//                    c.setChatTime(System.currentTimeMillis());
//                    String text = slea.readMapleAsciiString2();
//                    //byte[] b = text.getBytes("UTF-8");
//                    StringBuilder sb = new StringBuilder();
//                    sb.append(c.getCharName() == null ? c.getId() : c.getCharName()).append(" : ").append(text);
//                    AllMessage(PacketCreator.sendInGameChat(sb.toString(), (byte) 0));
//                    sb.setLength(0);
//                    sb.append("<접속기> ").append(c.getCharName() == null ? c.getId() : c.getCharName()).append("(")
//                            .append((c.getChar() == null ? "미접속" : c.getChar().getName()));
//                    if (c.getSecondId() != null) {
//                        if (c.getSecondChar() != null) {
//                            sb.append(",").append(c.getSecondChar().getName());
//                        }
//                    }
//                    sb.append(")").append(" : ").append(text.trim());
//                    ConnectorServerHandler.logchat(sb.toString());
//                    World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, sb.toString()));
//                }
//                break;
//            }
            case LOAD_PROCESS: {
                model = (DefaultTableModel) ConnectorPanel.jTable3.getModel();
                model.getDataVector().removeAllElements();
                String a, b;
                int as = slea.readInt();
                for (int i = 0; i < as; i++) {
                    a = slea.readMapleAsciiString2();
                    b = slea.readMapleAsciiString2();
                    model.addRow(new Object[]{a, b});
                }
                break;
            }
            case LOAD_CHARINFO: {
                c.sendPacket(PacketCreator.sendCharInfo(slea.readMapleAsciiString2()));
                break;
            }
            case LOAD_CHARLIST: {
                c.sendPacket(PacketCreator.sendCharList(c.getAccountId()));
                break;
            }
            case SETTING_MAINCHAR: {
                model = (DefaultTableModel) ConnectorPanel.jTable1.getModel();
                String charname = slea.readMapleAsciiString2();
                ConnectorServerHandler.setMainCharacter(c.getAccountId(), charname);
                c.setCharName(charname);
                model.setValueAt(charname, ConnectorPanel.getModelId(c.getSession2().remoteAddress().toString()), 5);
                ConnectorServer.getInstance().getClientStorage().broadcastPacket(PacketCreator.sendUserList());
                break;
            }
            case SECOND_LOGIN_PASSWORD: {
                MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
                String id = slea.readMapleAsciiString2();
                String pw = slea.readMapleAsciiString2();
                List<String> macs = new ArrayList<>();
                StringBuilder sb = new StringBuilder();
                int count = slea.readInt();
                for (int i = 0; i < count; i++) {
                    String mac = slea.readMapleAsciiString2();
                    sb.append(mac).append("\r\n");
                    macs.add(mac);
                }
                String key = slea.readMapleAsciiString2();
                int res = Secondlogin(id, pw, sb.toString(), key, c, false);
                mplew.write((byte) SendPacketOpcode.SECOND_LOGIN.getValue());
                mplew.write((byte) res);
                c.send(mplew.getPacket());
                break;
            }
            case CONNECTOR_HELP: {
                StringBuilder sb = new StringBuilder();
                sb.append("[접속기]").append(" : ").append(c.getId()).append("(").append(c.getCharName()).append(")").append("님이 접속기에서 도움을 요청합니다.");
                //World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, sb.toString()));
                break;
            }
            case DISCONNECT_LOGIN: {
                String id = slea.readMapleAsciiString2();
                String sid = slea.readMapleAsciiString2();
                //boolean su = disLoggedin(id, sid);
                //MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
                //mplew.write((byte) 14);
                // mplew.write(su ? 1 : 0);
                // c.send(mplew.getPacket());
                break;
            }
            case ADD_AUTH_LIST: {
                /*String auth = getRandomString(20);
                String id = slea.readMapleAsciiString2();
                String pw = slea.readMapleAsciiString2();
                if (ServerConstants.authlist2.get(id) != null) {
                    ServerConstants.authlist.remove(c.getAuth());
                    ServerConstants.authlist2.remove(id);
                }
                ServerConstants.authlist.put(auth, new Triple<>(id, pw, auth));
                ServerConstants.authlist2.put(id, new Triple<>(id, pw, auth));

                c.setAuth(auth);
                MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
                mplew.write(SendPacketOpcode.LOGIN_TOKEN.getValue());
                mplew.writeMapleAsciiString2(auth);
                mplew.writeMapleAsciiString2(c.FileLoad("Adress.txt"));
                c.send(mplew.getPacket());*/
                break;
            }
            case PROCESS_DETECT_BAN: {
                String text = slea.readMapleAsciiString2();
                String key = slea.readMapleAsciiString2();
                if (c.getId().equals("cebreiro")) {
                    return;
                }
                Ban(c, text, key);
                ConnecterLog(text, c);
                c.getSession().close();
                break;
            }
            case RESOLUTION: {
                c.setMyRESOLUTION(slea.readInt(), slea.readInt());
                break;
            }
            case GAME_START_CREATE_KEY: {
                int key = Randomizer.rand(1, Integer.MAX_VALUE);
                c.setGameKey(key);
                c.send(PacketCreator.sendGameStartKet(key));
                break;
            }
            default:
                System.out.println("[UNHANDLED] Recv [" + header + "] found");
                break;
        }
    }

    public static void ConnecterLog(String text, ConnectorClient c) {
        try {
            if (ServerConstants.ConnecterLog) {
                ConnectorPanel.jTextArea2.append(text + "\r\n");
                ConnectorPanel.jTextArea2.setCaretPosition(ConnectorPanel.jTextArea2.getDocument().getLength());
                //System.out.println(text);
            }
        } catch (NullPointerException e) {
            System.out.println("logchat error : " + e);
        }
    }

    public static void AllMessage(byte[] data) {
        /*DefaultTableModel model = (DefaultTableModel) ConnectorPanel.jTable1.getModel();
         ConnectorClient cc;
         for (int i = model.getRowCount() - 1; i >= 0; i--) {
         byte[] temp = new byte[data.length];
         System.arraycopy(data, 0, temp, 0, data.length);
         cc = (ConnectorClient) model.getValueAt(i, 4);
         cc.sendPacket(temp);
         }*/
        for (ConnectorClient c : ConnectorServer.getInstance().getClientStorage().getLoginClients()) {
            //byte[] temp = new byte[data.length];
            //System.arraycopy(data, 0, temp, 0, data.length);
            c.getSession().write(data);
        }
    }

    public static boolean bancheck(String key, String ip, Connection con) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement("SELECT * FROM connectorban WHERE `connecterkey` = ? OR `ip` = ?");
            ps.setString(1, key);
            ps.setString(2, ip);
            rs = ps.executeQuery();
            if (rs.next()) {
                return false;
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return true;
    }

    public static boolean disLoggedin(String id, String sid) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        boolean check1 = false;
        boolean check2 = false;
        try {
            for (MapleCharacter ch : World.getStorage(-10).getAllCharacters()) {
                if (ch.getClient().getAccountName().equals(id) || ch.getClient().getAccountName().equals(sid)) {
                    //ch.getClient().close();
                }
            }
            for (ChannelServer ch : ChannelServer.getAllInstances()) {
                for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                    if (chr.getClient().getAccountName().equals(id) || chr.getClient().getAccountName().equals(sid)) {
                        //  chr.getClient().close();
                    }
                }
            }
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT loggedin FROM accounts WHERE name = ?");
            ps.setString(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                if (rs.getByte("loggedin") > 0) {
                    check1 = true;
                }
            }
            rs.close();
            ps.close();
            ps = con.prepareStatement("SELECT loggedin FROM accounts WHERE name = ?");
            ps.setString(1, sid);
            rs = ps.executeQuery();
            if (rs.next()) {
                if (rs.getByte("loggedin") > 0) {
                    check2 = true;
                }
            }
            rs.close();
            ps.close();
            if (check1) {
                ps = con.prepareStatement("UPDATE accounts SET loggedin = 0 WHERE name = ?");
                ps.setString(1, id);
                ps.executeUpdate();
                ps.close();
                //FileoutputUtil.log(FileoutputUtil.현접풀기, "[" + CurrentReadable_Time() + "] ID : " + id + " 아이디가 현접풀기를 시도 했습니다.");
                World.Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, " ID : " + id + " 아이디가 현접풀기를 시도 했습니다."));
            }
            if (check2) {
                ps = con.prepareStatement("UPDATE accounts SET loggedin = 0 WHERE name = ?");
                ps.setString(1, sid);
                ps.executeUpdate();
                ps.close();
                //FileoutputUtil.log(FileoutputUtil.현접풀기, "[" + CurrentReadable_Time() + "] ID : " + sid + " 아이디가 현접풀기를 시도 했습니다.");
                World.Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, " ID : " + sid + " 아이디가 현접풀기를 시도 했습니다."));
            }
            if (!check2 && !check1) {
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return true;
    }

    /*
     0 비번실패
     1 없는아이디
     2 성공
     3 벤
     4 이미 접속중인 아이디
     5 알수없음
     */
    public static int login(String login, String pwd, String mac, String key, ConnectorClient c, boolean checkpw) {

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM accounts WHERE name = ?");
            ps.setString(1, login);
            rs = ps.executeQuery();
            if (rs.next()) {
                if (rs.getInt("allowed") == 1) {
                    rs.close();
                    return 4;
                }
                final String pass = rs.getString("password");
                final int banned = rs.getInt("banned");
                if (!checkpw) {
                    if (pass.equals(pwd)) {
                        if (banned > 0 || !bancheck(key, c.getIP(), con)) {
                            rs.close();
                            return 3;
                        }
                        ps = con.prepareStatement("UPDATE accounts SET allowed = ?, macs = ?, connecterKey = ?, connecterClient = ?, connecterIP = ? WHERE name = ?");
                        ps.setByte(1, (byte) 1);
                        ps.setString(2, mac);
                        ps.setString(3, key);
                        ps.setString(4, c.toString());
                        ps.setString(5, c.getIP());
                        ps.setString(6, login);
                        ps.executeUpdate();
                        ps.close();
                        c.setAccountId(rs.getInt("id"));
                        c.setConnecterKey(rs.getString("connecterKey"));
                        //String charname = ConnectorServerHandler.getFirstCharacter(c.getAccountId(), con, c.getId());
                        String charname = ConnectorServerHandler.getMainCharacter(c.getAccountId(), c.getId(), con);

                        //DefaultTableModel model = null;
                        //model = (DefaultTableModel) ConnectorPanel.jTable1.getModel();
                        //model.setValueAt(login + ",", ConnectorPanel.getModelId(c.getSession().remoteAddress().toString()), 0);
                        //model.setValueAt(charname, ConnectorPanel.getModelId(c.getSession().remoteAddress().toString()), 5);
                        c.setCharName(charname);
                        return 2;
                    } else {
                        return 0;
                    }
                } else {
                    if (LoginCryptoLegacy.isLegacyPassword(pass) && LoginCryptoLegacy.checkPassword(pwd, pass)) {
                        if (banned > 0) {
                            return 3;
                        }
                        ps = con.prepareStatement("UPDATE accounts SET allowed = ?, macs = ?, connecterKey = ?, connecterClient = ?, connecterIP = ? WHERE name = ?");
                        ps.setByte(1, (byte) 1);
                        ps.setString(2, mac);
                        ps.setString(3, key);
                        ps.setString(4, c.toString());
                        ps.setString(5, c.getIP());
                        ps.setString(6, login);
                        ps.executeUpdate();
                        ps.close();
                        c.setAccountId(rs.getInt("id"));
                        String charname = ConnectorServerHandler.getFirstCharacter(c.getAccountId(), con, c.getId());
                        //DefaultTableModel model = null;
                        //model = (DefaultTableModel) ConnectorPanel.jTable1.getModel();
                        //model.setValueAt(login + ",", ConnectorPanel.getModelId(c.getSession().remoteAddress().toString()), 0);
                        //model.setValueAt(charname, ConnectorPanel.getModelId(c.getSession().remoteAddress().toString()), 5);
                        c.setCharName(charname);
                        return 2;
                    } else {
                        return 0;
                    }
                }
            } else {
                return 1;
            }
        } catch (SQLException e) {
            System.out.println(e);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                    System.out.println(ex);
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return 5;
    }

    public static int Secondlogin(String login, String pwd, String mac, String key, ConnectorClient c, boolean checkpw) {

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            if (c.getSecondId() == null) {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("SELECT * FROM accounts WHERE name = ?");
                ps.setString(1, login);
                rs = ps.executeQuery();
                if (rs.next()) {
                    if (rs.getInt("allowed") == 1) {
                        return 4;
                    }
                    if (rs.getInt("banned") > 0) {
                        return 3;
                    }
                    if (!rs.getString("password").equals(pwd)) {
                        return 0;
                    } else {
                        ps = con.prepareStatement("UPDATE accounts SET allowed = ?, macs = ?, connecterKey = ?, connecterClient = ?, connecterIP = ?  WHERE name = ?"); //첫번째 세컨드계정 시도
                        ps.setByte(1, (byte) 1);
                        ps.setString(2, mac);
                        ps.setString(3, key);
                        ps.setString(4, c.toString());
                        ps.setString(5, c.getIP());
                        ps.setString(6, login);
                        ps.executeUpdate();
                        c.setSecondAccountId(rs.getInt("id"));
                        //String charname = ConnectorServerHandler.getFirstCharacter(c.getAccountId(), con, c.getId());
                        DefaultTableModel model = null;
                        model = (DefaultTableModel) ConnectorPanel.jTable1.getModel();
                        model.setValueAt(model.getValueAt(ConnectorPanel.getModelId(c.getSession2().remoteAddress().toString()), 0) + login + ",", ConnectorPanel.getModelId(c.getSession2().remoteAddress().toString()), 0);
                        //c.setCharName(charname);
                        c.setSecondId(login);
                        c.setSecondPasswrod(pwd);
                        ConnectorServer.getInstance().getClientStorage().registerSClient(c, login);
                        return 2;
                    }
                } else {
                    return 1;
                }
            } else if (c.getSecondId() != null && !login.equals(c.getSecondId())) { // 세컨드 아이디 변경
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("SELECT * FROM accounts WHERE name = ?");
                ps.setString(1, c.getSecondId());
                rs = ps.executeQuery();
                if (rs.next()) {
                    ps = con.prepareStatement("UPDATE accounts SET allowed = ?, macs = ?, connecterKey = ?, connecterClient = ?, connecterIP = ? WHERE name = ?"); // 기존계정 로그아웃
                    ps.setByte(1, (byte) 0);
                    ps.setString(2, mac);
                    ps.setString(3, key);
                    ps.setString(4, null);
                    ps.setString(5, null);
                    ps.setString(6, c.getSecondId());
                    ps.executeUpdate();
                    ps.close();
                    DefaultTableModel model = null;
                    model = (DefaultTableModel) ConnectorPanel.jTable1.getModel();
                    model.setValueAt(model.getValueAt(ConnectorPanel.getModelId(c.getSession2().remoteAddress().toString()), 0).toString().replaceAll("," + c.getSecondId() + ",", "") + ",", ConnectorPanel.getModelId(c.getSession2().remoteAddress().toString()), 0);
                    ps = con.prepareStatement("SELECT * FROM accounts WHERE name = ?");
                    ps.setString(1, login);
                    rs = ps.executeQuery();
                    if (rs.next()) {
                        if (rs.getInt("allowed") == 1) {
                            ps.close();
                            rs.close();
                            return 4;
                        }
                        if (rs.getInt("banned") > 0) {
                            ps.close();
                            rs.close();
                            return 3;
                        }
                        if (!rs.getString("password").equals(pwd)) {
                            ps.close();
                            rs.close();
                            return 0;
                        } else {
                            ps = con.prepareStatement("UPDATE accounts SET allowed = ?, macs = ?, connecterKey = ?, connecterClient = ?, connecterIP = ? WHERE name = ?");
                            ps.setByte(1, (byte) 1);
                            ps.setString(2, mac);
                            ps.setString(3, key);
                            ps.setString(4, c.toString());
                            ps.setString(5, c.getIP());
                            ps.setString(6, login);
                            ps.executeUpdate();
                            ps.close();
                            c.setSecondAccountId(rs.getInt("id"));
                            c.setSecondId(login);
                            c.setSecondPasswrod(pwd);
                            model = (DefaultTableModel) ConnectorPanel.jTable1.getModel();
                            model.setValueAt(model.getValueAt(ConnectorPanel.getModelId(c.getSession2().remoteAddress().toString()), 0) + login + ",", ConnectorPanel.getModelId(c.getSession2().remoteAddress().toString()), 0);
                        }
                        return 2;
                    } else {
                        return 1;
                    }
                }
            } else if (login.equals(c.getSecondId())) {
                return 4;
            }
            return 5;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                    System.out.println(ex);
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return 5;
    }

    public static String userList() {
        //DefaultTableModel model = null;
        //model = (DefaultTableModel) ConnectorPanel.jTable1.getModel();
        StringBuilder sb = new StringBuilder();
        /*for (int i = model.getRowCount() - 1; i >= 0; i--) {
         if (model.getValueAt(i, 5) != null) {
         sb.append(model.getValueAt(i, 5)).append(",");
         } else if (model.getValueAt(i, 0) != null) {
         sb.append(model.getValueAt(i, 0)).append("");
         }
         }*/
        for (ConnectorClient c : ConnectorServer.getInstance().getClientStorage().getLoginClients()) {
            sb.append((c.getCharName() != null ? c.getCharName() : c.getId())).append(",");
        }

        return sb.toString();
    }

    public static MaplePacketLittleEndianWriter getCharItems(int charid, MaplePacketLittleEndianWriter mplew) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        byte[] position = {-1, -2, -3, -4, -5, -6, -7, -8, -9, -11};
        byte[] positionCash = {-101, -102, -103, -104, -105, -106, -107, -108, -109, -111};
        List<Integer> itemArray = new ArrayList<>();

        try {
            con = DatabaseConnection.getConnection();
            for (int i = 0; i < position.length; i++) {
                ps = con.prepareStatement("SELECT itemid, position FROM inventoryitems WHERE characterid = ? and position = ?");
                ps.setInt(1, charid);
                ps.setByte(2, positionCash[i]);
                rs = ps.executeQuery();
                if (rs.next()) {
                    itemArray.add(rs.getInt("itemid"));
                } else {
                    ps.close();
                    rs.close();
                    ps = con.prepareStatement("SELECT itemid, position FROM inventoryitems WHERE characterid = ? and position = ?");
                    ps.setInt(1, charid);
                    ps.setByte(2, position[i]);
                    rs = ps.executeQuery();
                    if (rs.next()) {
                        itemArray.add(rs.getInt("itemid"));
                    }
                }
            }

            mplew.write(itemArray.size());
            for (int i = 0; i < itemArray.size(); i++) {
                mplew.writeInt(itemArray.get(i));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return mplew;
    }

    public static StringBuilder getCharItems(int charid, StringBuilder sb) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        byte[] position = {-1, -2, -3, -4, -5, -6, -7, -8, -9, -11};
        byte[] positionCash = {-101, -102, -103, -104, -105, -106, -107, -108, -109, -111};
        List<Integer> itemArray = new ArrayList<>();

        try {
            con = DatabaseConnection.getConnection();
            for (int i = 0; i < position.length; i++) {
                ps = con.prepareStatement("SELECT itemid, position FROM inventoryitems WHERE characterid = ? and position = ?");
                ps.setInt(1, charid);
                ps.setByte(2, positionCash[i]);
                rs = ps.executeQuery();
                if (rs.next()) {
                    itemArray.add(rs.getInt("itemid"));
                } else {
                    ps.close();
                    rs.close();
                    ps = con.prepareStatement("SELECT itemid, position FROM inventoryitems WHERE characterid = ? and position = ?");
                    ps.setInt(1, charid);
                    ps.setByte(2, position[i]);
                    rs = ps.executeQuery();
                    if (rs.next()) {
                        itemArray.add(rs.getInt("itemid"));
                    }
                }
            }

            for (int i = 0; i < itemArray.size(); i++) {
                sb.append(itemArray.get(i));
                sb.append(",");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return sb;
    }

    public static void logchat(String text) {
        try {
            ConnectorPanel.jTextArea1.append(text);
            ConnectorPanel.jTextArea1.append("\r\n");
            ConnectorPanel.jTextArea1.setCaretPosition(ConnectorPanel.jTextArea1.getDocument().getLength());
        } catch (NullPointerException e) {
            System.out.println("logchat error : " + e);
        }
    }

    public static int getCharacterInfo(String name, String who) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM characters WHERE name = ?");
            ps.setString(1, name);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("" + who + "");
            }
            ps.close();
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return 0;
    }

    public static int getCharacterId(String name) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT id FROM characters WHERE name = ?");
            ps.setString(1, name);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            ps.close();
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return 0;
    }

    public static String getMainCharacter(int code, String name, Connection con) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement("SELECT maincharacter FROM accounts WHERE id = ?");
            ps.setInt(1, code);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("maincharacter");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return name;
    }

    public static void setMainCharacter(int code, String name) {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("UPDATE accounts SET maincharacter = ? WHERE id = ?");
            ps.setString(1, name);
            ps.setInt(2, code);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String getFirstCharacter(int code, Connection con, String name) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement("SELECT name FROM characters WHERE accountid = ?");
            ps.setInt(1, code);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return name;
    }

    public static String getCharacterList(int code) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        StringBuilder sb = new StringBuilder();
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT name FROM characters WHERE accountid = ? ORDER BY level desc");
            ps.setInt(1, code);
            rs = ps.executeQuery();
            while (rs.next()) {
                sb.append(rs.getString("name")).append("|");
            }
            ps.close();
            rs.close();
            return sb.toString();
        } catch (SQLException e) {

            e.printStackTrace();
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return "";
    }
    /*
     0 존재하는아디
     1 성공적회원가입
     2 횟수 초과
     3 이용정지
     */

    public static int AccountRegister(ConnectorClient c, String id, String pw, String Email, String mac, byte Gender, String key) {
        Connection con = null;
        PreparedStatement ps = null;
        PreparedStatement ps2 = null;
        PreparedStatement ps3 = null;
        ResultSet rs = null;
        ResultSet rs3 = null;
        Date time = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT connecterKey FROM accounts WHERE name = ?");
            ps.setString(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                rs.close();
                ps.close();
                return 0;
            } else {
                if (!bancheck(key, c.getIP(), con)) {
                    return 3;
                }
                ps3 = con.prepareStatement("SELECT connecterKey FROM accounts WHERE connecterKey = ?");
                ps3.setString(1, key);
                rs3 = ps3.executeQuery();
                ps2 = con.prepareStatement("INSERT INTO accounts (name, password, email, birthday, macs, SessionIP, gender, connecterKey, mPoints) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"); //helios_wep
                if (rs3.next()) {
                    if (rs3.first() == false || rs3.last() == true && rs3.getRow() < 2) {
                        ps2.setString(1, id);
                        ps2.setString(2, pw);
                        ps2.setString(3, Email);
                        ps2.setString(4, sdf.format(time));
                        ps2.setString(5, mac);
                        ps2.setString(6, c.getIP());
                        ps2.setByte(7, Gender);
                        ps2.setString(8, key);
                        ps2.setInt(9, 0);
                        ps2.executeUpdate();
                        ps2.close();
                        rs3.close();
                        return 1;
                    } else {
                        return 2;
                    }
                } else {
                    ps2.setString(1, id);
                    ps2.setString(2, pw);
                    ps2.setString(3, Email);
                    ps2.setString(4, sdf.format(time));
                    ps2.setString(5, mac);
                    ps2.setString(6, c.getIP());
                    ps2.setByte(7, Gender);
                    ps2.setString(8, key);
                    ps2.setInt(9, 0);
                    ps2.executeUpdate();
                    ps2.close();
                    return 1;
                }
            }
        } catch (SQLException e) {
            System.out.println(e);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (ps2 != null) {
                try {
                    ps2.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (ps3 != null) {
                try {
                    ps3.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (rs3 != null) {
                try {
                    rs3.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return 4;
    }

    private static String getRandomString(int length) {
        StringBuffer buffer = new StringBuffer();
        Random random = new Random();

        String chars[] = "a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z,0,1,2,3,4,5,6,7,8,9".split(",");

        for (int i = 0; i < length; i++) {
            buffer.append(chars[random.nextInt(chars.length)]);
        }
        return buffer.toString();
    }

    public static void Ban(ConnectorClient c, String d, String key) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM connectorban WHERE ip = ? or connecterkey = ?");
            ps.setString(1, c.getIP());
            ps.setString(2, key);
            rs = ps.executeQuery();

            if (rs.next()) {
                rs.close();
                ps.close();
                return;
            }
            ps.close();
            rs.close();

            ps = con.prepareStatement("INSERT INTO connectorban (`connecterkey`, `ip`, `comment`) VALUES (?, ?, ?)");
            ps.setString(1, key);
            ps.setString(2, c.getIP());
            ps.setString(3, d);
            ps.execute();
            ps.close();

            ps = con.prepareStatement("UPDATE accounts SET banned = 1, banreason = ?, banby = '운영자' WHERE connecterKey = ? or SessionIP = ?");
            ps.setString(1, "영구 정지 당하셨습니다.");
            ps.setString(2, key);
            ps.setString(3, c.getIP());
            ps.executeUpdate();
            ps.close();
            if (c.getSecondId() != null) {
                ps = con.prepareStatement("UPDATE accounts SET banned = 1, banreason = ?, banby = '운영자' WHERE connecterKey = ? or SessionIP = ?");
                ps.setString(1, "영구 정지 당하셨습니다.");
                ps.setString(2, key);
                ps.setString(3, c.getIP());
                ps.executeUpdate();
                ps.close();
            }
        } catch (SQLException e) {
            System.out.println("Ban Error : ");
            e.printStackTrace();
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {

                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {

                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {

                }
            }
        }
    }
}
