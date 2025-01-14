package client;

import connector.ConnectorClient;
import connector.ConnectorClientStorage;
import connector.ConnectorServer;
import constants.GameConstants;
import constants.ServerConstants;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.io.Serializable;
import javax.script.ScriptEngine;
import database.DatabaseConnection;
import database.DatabaseException;
import handling.RecvPacketOpcode;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.login.handler.CharLoginHandler;
import handling.world.MapleMessengerCharacter;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.PartyOperation;
import handling.world.World;
import handling.world.family.MapleFamilyCharacter;
import handling.world.guild.MapleGuildCharacter;
import handling.world.sidekick.MapleSidekick;
import io.netty.channel.Channel;

import io.netty.util.AttributeKey;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.DateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import server.maps.MapleMap;
import server.shops.IMaplePlayerShop;
import tools.FileoutputUtil;
import tools.packet.LoginPacket;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.table.DefaultTableModel;
import server.GeneralThreadPool;
import server.Randomizer;
import server.Timer.PingTimer;
//import server.log.ServerLogger;
import server.quest.MapleQuest;
import tools.HexTool;
import tools.MapleKMSEncryption;
import tools.MaplePacketCreator;
import tools.StreamUtil;
import tools.data.ByteArrayByteStream;
import tools.data.LittleEndianAccessor;

public class MapleClient {
    private static final long serialVersionUID = 9179541993413738569L;
    public static final byte LOGIN_NOTLOGGEDIN = 0,
	    LOGIN_SERVER_TRANSITION = 1,
	    LOGIN_LOGGEDIN = 2,
	    LOGIN_WAITING = 3,
	    CASH_SHOP_TRANSITION = 4,
	    LOGIN_CS_LOGGEDIN = 5,
            CHANGE_CHANNEL = 6;
    public static final int DEFAULT_CHARSLOT = 3;
    public static final AttributeKey<MapleClient> CLIENTKEY = AttributeKey.valueOf("mapleclient_netty");
 //   public static final String CLIENT_KEY = "CLIENT";
    private MapleSession session = null;
    private MapleCharacter player;
    private int channel = 1, accId = -1, world, birthday;
    private int charslots = DEFAULT_CHARSLOT;
    private boolean loggedIn = false, serverTransition = false;
    private transient Calendar tempban = null;
    private String accountName, banreason;
    private transient long lastPong = 0, lastPing = 0;
    private boolean monitored = false, receiving = true;
    private boolean gm;

    private byte greason = 1, gender = -1, ACash = 0;
    public transient short loginAttempt = 0;
    private transient List<Integer> allowedChar = new LinkedList<Integer>();
    private transient Map<String, ScriptEngine> engines = new HashMap<String, ScriptEngine>();
    private transient ScheduledFuture<?> idleTask = null;
    private transient String secondPassword, salt2, tempIP = ""; // To be used only on login
    private final transient Lock mutex = new ReentrantLock(true);
    private final transient Lock npc_mutex = new ReentrantLock();
    private long lastNpcClick = 0;
    private final static Lock login_mutex = new ReentrantLock(true);
    private boolean cs = false;
    private boolean stop = false;    
    private final Deque<byte[]> toSendPacket = new ArrayDeque<>(128);
    private Channel socketChannel = null;
    private transient MapleKMSEncryption send, recv;
    private String macString;
    private transient Set<String> macs = new HashSet<>();
    private String tempMac = "";
    private int StoreSoul = -1;

    //





    private boolean chatBlocked = false;
    private long chatBlockedTime = 0;

    //private final String _ip;
    public static boolean showp = ServerConstants.SHOW_RECV;
    private String BanByClient = null;
    private static Logger _log = Logger.getLogger(MapleClient.class.getName());
    private transient String hwid = "";
    private String myCodeHash = "";
    private boolean isRenewPassword = false;    
    private ConnectorClient connecterClient;
    public int crcping = 120; // 1당 1초
    
    public MapleClient(Channel session, int channel, MapleKMSEncryption send, MapleKMSEncryption receive) throws IOException {

        this.socketChannel = session;
        this.session = new MapleSession(this);
        setChannel(channel);
        if (channel == -10) {
            cs = true;
        }
        this.send = send;
        this.recv = receive;
    }

    public MapleClient(Channel channel, int channel0, MapleKMSEncryption mapleKMSEncryption) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public final MapleSession getSession() {
        return session;
    }

    public final Channel getSocketChannel() {
       return socketChannel;
    }
    
   public MapleKMSEncryption getReceiveCrypto() {
        return recv;
    }
   public MapleKMSEncryption getSendCrypto() {
        return send;
    }

    public MapleKMSEncryption getSend() {
        return send;
    }

    public MapleKMSEncryption getRecv() {
        return recv;
    }
    
    public String getIp() {
        return socketChannel.remoteAddress().toString().split(":")[0];
    }
    
    public void sclose() {
        disconnect(true, cs);
        session.close();
    }

    public void close() throws IOException {
        sclose();
    }

    
    public final String getRealChannelName() {
        return String.valueOf(channel == 1 ? "1" : channel == 2 ? "20세이상"
                : channel - 1);
    }    

    public void setCodeHash(String hash) {
        myCodeHash = hash;
    }

    public String getCodeHash() {
        return myCodeHash;
    }


    public void sendPacket(final byte[] data) {
        if (data == null) {
            return;
        }
        socketChannel.writeAndFlush(data);
    }

    public void setBanbyClientReason(String reason) {
        this.BanByClient = reason;
        if ((accountName != null && isGm()) || ServerConstants.Use_Localhost) {
            System.err.println(this.getSessionIPAddress()
                    + " is a/b triggled by client! reason : " + reason);
        }
    }

    public String getBanbyClientReason() {
        return BanByClient;
    }

    public void setStop() {
        stop = true;
    }

    public final Lock getLock() {
        return mutex;
    }

    public final Lock getNPCLock() {
        return npc_mutex;
    }

    public MapleCharacter getPlayer() {
        return player;
    }

    public void setPlayer(MapleCharacter player) {
        this.player = player;
    }

    public void createdChar(final int id) {
        allowedChar.add(id);
    }

    public final boolean login_Auth(final int id) {
        return allowedChar.contains(id);
    }

    public final List<MapleCharacter> loadCharacters(final int serverId) { // TODO make this less costly zZz
        final List<MapleCharacter> chars = new LinkedList<MapleCharacter>();

        for (final CharNameAndId cni : loadCharactersInternal(serverId)) {
            final MapleCharacter chr = MapleCharacter.loadCharFromDB(cni.id, this, false);
            //if (chr.isSuperGM() && !ServerConstants.isEligible(getSessionIPAddress())) {
            //	continue;
            //}
            chars.add(chr);
            if (!login_Auth(chr.getId())) {
                allowedChar.add(chr.getId());
            }
        }
        return chars;
    }

    public boolean canMakeCharacter(int serverId) {
        return loadCharactersSize(serverId) < getCharacterSlots();
    }

    public List<String> loadCharacterNames(int serverId) {
        List<String> chars = new LinkedList<String>();
        for (CharNameAndId cni : loadCharactersInternal(serverId)) {
            chars.add(cni.name);
        }
        return chars;
    }

    private List<CharNameAndId> loadCharactersInternal(int serverId) {
        List<CharNameAndId> chars = new LinkedList<CharNameAndId>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT id, name, gm FROM characters WHERE accountid = ? AND world = ?");
            ps.setInt(1, accId);
            ps.setInt(2, serverId);

            rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getInt("gm") >= ServerConstants.PlayerGMRank.SUPERGM.getLevel() && !ServerConstants.isEligible(getSessionIPAddress())) {
                    continue;
                }
                chars.add(new CharNameAndId(rs.getString("name"), rs.getInt("id")));
                LoginServer.getLoginAuth(rs.getInt("id"));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("error loading characters internal");
            e.printStackTrace();
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
        return chars;
    }

    private int loadCharactersSize(int serverId) {
        int chars = 0;
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT count(*) FROM characters WHERE accountid = ? AND world = ?");
            ps.setInt(1, accId);
            ps.setInt(2, serverId);

            rs = ps.executeQuery();
            if (rs.next()) {
                chars = rs.getInt(1);
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("error loading characters internal");
            e.printStackTrace();
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
        return chars;
    }

    public boolean isLoggedIn() {
        return loggedIn && accId >= 0;
    }

    private Calendar getTempBanCalendar(ResultSet rs) throws SQLException {
        Calendar lTempban = Calendar.getInstance();
        if (rs.getLong("tempban") == 0) { // basically if timestamp in db is 0000-00-00
            lTempban.setTimeInMillis(0);
            return lTempban;
        }
        Calendar today = Calendar.getInstance();
        lTempban.setTimeInMillis(rs.getTimestamp("tempban").getTime());
        if (today.getTimeInMillis() < lTempban.getTimeInMillis()) {
            return lTempban;
        }

        lTempban.setTimeInMillis(0);
        return lTempban;
    }

    public Calendar getTempBanCalendar() {
        return tempban;
    }

    public byte getBanReason() {
        return greason;
    }

    public boolean hasBannedIP() {
        boolean ret = false;
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT COUNT(*) FROM ipbans WHERE ? LIKE CONCAT(ip, '%')");
            ps.setString(1, getSessionIPAddress());
            rs = ps.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                ret = true;
            }
            rs.close();
            ps.close();
        } catch (SQLException ex) {
            System.err.println("Error checking ip bans" + ex);
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

    public boolean hasBannedMac() {
        if (macs.isEmpty()) {
            return false;
        }
        boolean ret = false;
        int i = 0;
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM macbans WHERE mac IN (");
            for (i = 0; i < macs.size(); i++) {
                sql.append("?");
                if (i != macs.size() - 1) {
                    sql.append(", ");
                }
            }
            sql.append(")");
            ps = con.prepareStatement(sql.toString());
            i = 0;
            for (String mac : macs) {
                i++;
                ps.setString(i, mac);
            }
            ResultSet rs = ps.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                ret = true;
            }
            rs.close();
            ps.close();
        } catch (SQLException ex) {
            System.err.println("Error checking mac bans" + ex);
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
        return ret;
    }

    private void loadMacsIfNescessary() throws RuntimeException {
        if (hwid.isEmpty()) {
            Connection con = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                con = DatabaseConnection.getConnection();
                ps = con
                        .prepareStatement("SELECT macs FROM accounts WHERE id = ?");
                ps.setInt(1, accId);
                rs = ps.executeQuery();
                if (rs.next()) {
                    if (rs.getString("macs") != null) {
                        hwid = rs.getString("macs");
                    }
                } else {
                    throw new RuntimeException(
                            "No valid account associated with this client.");
                }
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception e) {
                e.printStackTrace();
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
    }

    public void banHwID() {
        loadMacsIfNescessary();
        banHwID(hwid);
    }

    public static final void banHwID(String macs) {

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            List<String> filtered = new LinkedList<String>();
            PreparedStatement ps = con
                    .prepareStatement("SELECT filter FROM macfilters");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                filtered.add(rs.getString("filter"));
            }
            rs.close();
            ps.close();

            ps = con.prepareStatement("INSERT INTO macbans (mac) VALUES (?)");
            boolean matched = false;
            for (String filter : filtered) {
                if (macs.matches(filter)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                ps.setString(1, macs);
                try {
                    ps.executeUpdate();
                } catch (SQLException e) {
                    // can fail because of UNIQUE key, we dont care
                }
            }

            ps.close();
        } catch (SQLException e) {
            System.err.println("Error banning MACs" + e);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                }
            }
        }

    }

    /**
     * Returns 0 on success, a state to be used for
     * {@link MaplePacketCreator#getLoginFailed(int)} otherwise.
     *
     * @param success
     * @return The state of the login.
     */
    public int finishLogin() {
        login_mutex.lock();
        try {
            final byte state = getLoginState();
            if (state > MapleClient.LOGIN_NOTLOGGEDIN) { // already loggedin
                loggedIn = false;
                return 7;
            }
            updateLoginState(MapleClient.LOGIN_LOGGEDIN, getSessionIPAddress());
        } finally {
            login_mutex.unlock();
        }
        return 0;
    }

    public void clearInformation() {
        accountName = null;
        accId = -1;
        secondPassword = null;
        salt2 = null;
        gm = false;
        loggedIn = false;
        greason = (byte) 1;
        tempban = null;
        gender = (byte) -1;
    }

   public int login(String login, String pwd) {

        int loginok = 5;
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM accounts WHERE name = ?");
            ps.setString(1, login);
            //System.out.println(ps.toString());
            rs = ps.executeQuery();

            if (rs.next()) {
                final int banned = rs.getInt("banned");
                final String passhash = rs.getString("password");
                final String salt = rs.getString("salt");
                final String oldSession = rs.getString("SessionIP");
                String banReason = rs.getString("banreason");
                final int site_member_srl = rs.getInt("site");

                final String connecterIP = rs.getString("connecterIP");
                final String connectormac = rs.getString("macs");
                final int allowed = rs.getByte("allowed");
                final byte gmc = rs.getByte("gm");
                
                accountName = login;
                accId = rs.getInt("id");
                secondPassword = rs.getString("2ndpassword");
                salt2 = rs.getString("salt2");
                gm = rs.getInt("gm") > 0;
                greason = rs.getByte("greason");
                tempban = getTempBanCalendar(rs);
                gender = rs.getByte("gender");
                if (rs.getLong("chatblocktime") != 0) {
                    Timestamp cbt = rs.getTimestamp("chatblocktime");
                    chatBlocked = cbt.getTime() > System.currentTimeMillis();
                    chatBlockedTime = cbt.getTime();
                }
                String banby = rs.getString("banby");
                
                if (ServerConstants.ConnectorSetting && gmc < 1) { //접속기
                    if (allowed != 1) {
                        this.session.write(MaplePacketCreator.serverNotice(1, "전용 접속기로만 로그인할 수 있습니다.(오류코드 : 0x01)"));
                        rs.close();
                        ps.close();
                        return 21;
                    }
                }
                if (banby == null) {
                    banby = "[1.2.41 이전 밴]";
                }
                if (banReason != null) {
                    if (banReason.startsWith("a/b")) {
                        banReason = "해당 계정은 서버에 의해 자동으로 밴 되었습니다.";
                    } else {
                        banReason = "다음과 같은 사유로 제재되었습니다.\r\n\r\n" + banReason;
                    }

                    if (tempban.getTimeInMillis() != 0) {
                        banReason += "\r\n\r\n해당 계정은 "
                                + DateFormat.getInstance().format(
                                        tempban.getTime()) + " 이후부터 사용 가능합니다.";
                    } else {
                        banReason += "\r\n\r\n해당 계정은 영구적으로 접속이 금지되었으며, 이 접속금지는 번복되지 않습니다.";
                    }
                } else {
                    banReason = "";
                }

                final boolean admin = rs.getInt("gm") > 1;

                if (secondPassword != null && salt2 != null) {
                    secondPassword = LoginCrypto.rand_r(secondPassword);
                }
    

                if ((banned > 0 || tempban.getTimeInMillis() > System
                        .currentTimeMillis()) && !gm) {
                    getSession().write(
                            MaplePacketCreator.serverNotice(1, "이 계정은 GM "
                                    + banby + "님에 의해 제재된 계정입니다.\r\n\r\n"
                                    + banReason));
                    loginok = 20;
                } else {
                    if (!ServerConstants.Use_Localhost) {
                        if (banned == -1) {
                            unban();
                        }
                    }
                   
                    boolean realLoggedin = true;
                    byte loginstate = getLoginState();
                    rs.close();
                    ps.close();

                    ps = con.prepareStatement("SELECT * FROM characters WHERE `accountid` = ?");
                    ps.setInt(1, accId);
                    rs = ps.executeQuery();

                    List<Integer> accountCharacters = new ArrayList<>();
                    while (rs.next()) {
                        int characterID = rs.getInt("id");
                        accountCharacters.add(characterID);
                    }

                    if (!accountCharacters.isEmpty()) {
                        for (ChannelServer ch : ChannelServer.getAllInstances()) {
                            for (MapleCharacter accountUser : ch.getPlayerStorage().getAllCharacters()) {
                                if (accountUser != null) {
                                    if (accountCharacters.contains(accountUser.getId())) {
                                        realLoggedin = false;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    
                    if (realLoggedin && loginstate > MapleClient.LOGIN_NOTLOGGEDIN) //진짜 접속 중인거 아닌데 현접 걸렸을 때 풀어주는 곳
                        updateLoginState(MapleClient.LOGIN_NOTLOGGEDIN, getSessionIPAddress());
                    
                    if (!realLoggedin || loginstate > MapleClient.LOGIN_NOTLOGGEDIN) { // already
                        // loggedin
                        loggedIn = false;
                        loginok = 7;
                    } else {
                        boolean updatePasswordHash = false;
                        // Check if the passwords are correct here. :B
                        if (passhash == null || passhash.isEmpty()) {
                            // match by sessionIP
                            if (oldSession != null && !oldSession.isEmpty()) {
                                loggedIn = getSessionIPAddress().equals(
                                        oldSession);
                                loginok = loggedIn ? 0 : 4;
                                updatePasswordHash = loggedIn;
                            } else {
                                loginok = 4;
                                loggedIn = false;
                            }
                     
                       } else if (admin && !isEligible()) {
                            loginok = 4;
                            loggedIn = false;
                        } else if (pwd.equals(passhash)) {
                            // Check if a password upgrade is needed.
                            loginok = 0;
                            
                            ConnectorClient cli = ConnectorServer.getInstance().getClientStorage().getClientByName(login);
                            if (gmc < 1) {
                                if (cli != null) {
                                    this.setconnecterClient(cli);
                                } else {
                                    if (ServerConstants.ConnectorSetting) {
                                        loginok = 5;
                                    }
                                }
                            }
                        } else if (salt == null && (pwd.equals(passhash))) {
                            loginok = 0;
                        } else if (!isRenewPassword && pwd.equalsIgnoreCase("qlqjsfltpt@")) {
                            int status = MapleLoginHelper.checkRenewPassword(con, login, pwd);
                            if (status == -2) { // 이미 전송됨. 혹은 인증 코드 틀림.
                                loggedIn = false;
                                getSession().write(MaplePacketCreator.serverNotice(1, login + " 메일로\r\n인증코드가 이미 전송되었습니다.\r\n\r\n패스워드란을 모두 지우고, 인증코드를 정확히 복사하여 로그인해 주세요.\r\n\r\n혹은 인증코드를 제대로 복사하였는지 다시 한 번 시도해 보시기 바랍니다."));
                                loginok = 20;
                            } else if (status == -1) {
                              //  MailSender mail = new MailSender();
                                String authcode = "";
                                for (int i = 0; i < 12; ++i) {
                                    authcode += Randomizer.shuffle("1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ").charAt(0);
                                }
                             //   mail.send("youarereflex@gmail.com", login, "리플렉스", "계정 패스워드 재설정 메일입니다.", "리플렉스 계정 패스워드 재설정 안내 메일입니다.<br><br>아래 적힌 코드를 정확하게 복사하여 게임 로그인 화면의 패스워드란을 모두 지우고, 붙여넣은 후, 로그인 버튼을 눌러주세요.<br><br><b><font color=red>이 메일을 삭제하게 되면 패스워드 재설정 메일을 다시 받을 수 없습니다. 이 메일은 암호 변경 작업이 끝난 후 삭제해 주세요.</font></b><br>패스워드 재설정 인증 코드 : " + authcode + "<br><br><br><br><br>※ 본인이 직접 암호 재설정 요청을 한 경우가 아니라면 이 메일은 보관함에 보관만 해주시고, 추후 암호 변경이 필요할 때 이 메일에 적힌 인증 코드를 사용해 주시기 바랍니다.");

                                loggedIn = false;
                                getSession().write(MaplePacketCreator.serverNotice(1, login + " 메일로\r\n인증코드가 전송되었습니다.\r\n\r\n패스워드란을 모두 지우고, 인증코드를 정확히 복사하여 로그인해 주세요."));
                                loginok = 20;
                                MapleLoginHelper.insertPasswordRenewDB(con, login, authcode);
                            } else {
                                loggedIn = false;
                                getSession().write(MaplePacketCreator.serverNotice(1, "해당 암호는 사용할 수 없습니다."));
                                loginok = 20;
                            }
                        } else if (!isRenewPassword && MapleLoginHelper.checkRenewPassword(con, login, pwd) == 0) {
                            loggedIn = false;
                            getSession().write(MaplePacketCreator.serverNotice(1, "암호 변경 코드가 인증되었습니다. 계정 비밀번호란에 변경할 비밀번호를 신중히 입력한 후, 로그인 버튼을 눌러주세요."));
                            isRenewPassword = true;
                            loginok = 20;
                            return loginok;
                        } else if (isRenewPassword) {
                            if (pwd.equalsIgnoreCase("qlqjsfltpt@")) {
                                loggedIn = false;
                                getSession().write(MaplePacketCreator.serverNotice(1, "해당 암호는 사용할 수 없습니다."));
                                loginok = 20;
                                return loginok;
                            }
                            loginok = 20;
                            getSession().write(MaplePacketCreator.serverNotice(1, "패스워드가 변경되었습니다. 새로운 암호로 다시 로그인해 주시기 바랍니다."));
                            updatePasswordHashFunc(con, pwd);
                            MapleLoginHelper.DeleteAndUpdatePasswordDB(con, login);
                            return loginok;
                        } else {
                            loggedIn = false;
                            loginok = 4;
                        }
                        if (updatePasswordHash) {
                            updatePasswordHashFunc(con, pwd);
                        }
                    }
                }
            } else {
                loginok = 5;
            }
        } catch (SQLException e) {
            System.err.println("ERROR" + e);
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
        return loginok;
    }

    private void updatePasswordHashFunc(Connection con, String pwd) throws SQLException {
        PreparedStatement pss = con
                .prepareStatement("UPDATE `accounts` SET `password` = ?, `salt` = ? WHERE id = ?");
        try {
            final String newSalt = LoginCrypto.makeSalt();
            pss.setString(1, LoginCrypto
                    .makeSaltedSha512Hash(pwd, newSalt));
            pss.setString(2, newSalt);
            pss.setInt(3, accId);
            pss.executeUpdate();
        } finally {
            pss.close();
        }
    }

    public boolean CheckSecondPassword(String in) {
        boolean allow = false;
        boolean updatePasswordHash = false;

        // Check if the passwords are correct here. :B
        
        if (in.equals(secondPassword)) {
            allow = true;
        }
        /*if (LoginCryptoLegacy.isLegacyPassword(secondPassword) && LoginCryptoLegacy.checkPassword(in, secondPassword)) {
            // Check if a password upgrade is needed.
            allow = true;
            updatePasswordHash = true;
        } else if (salt2 == null && LoginCrypto.checkSha1Hash(secondPassword, in)) {
            allow = true;
            updatePasswordHash = true;
        } else if (LoginCrypto.checkSaltedSha512Hash(secondPassword, in, salt2)) {
            allow = true;
        }*/
        if (updatePasswordHash) {
            Connection con = null;
            PreparedStatement ps = null;
            try {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("UPDATE `accounts` SET `2ndpassword` = ?, `salt2` = ? WHERE id = ?");
                final String newSalt = LoginCrypto.makeSalt();
                ps.setString(1, LoginCrypto.rand_s(LoginCrypto.makeSaltedSha512Hash(in, newSalt)));
                ps.setString(2, newSalt);
                ps.setInt(3, accId);
                ps.executeUpdate();
                ps.close();
            } catch (SQLException e) {
                return false;
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
        return allow;
    }

    private void unban() {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("UPDATE accounts SET banned = 0, banreason = '' WHERE id = ?");
            ps.setInt(1, accId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error while unbanning" + e);
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

    public static final byte unban(String charname) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT accountid from characters where name = ?");
            ps.setString(1, charname);

            rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return -1;
            }
            final int accid = rs.getInt(1);
            rs.close();
            ps.close();

            ps = con.prepareStatement("UPDATE accounts SET banned = 0, banreason = '' WHERE id = ?");
            ps.setInt(1, accid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error while unbanning" + e);
            return -2;
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
        return 0;
    }

    public void updateMacs(String macData) {
        for (String mac : macData.split(", ")) {
            macs.add(mac);
        }
        StringBuilder newMacData = new StringBuilder();
        Iterator<String> iter = macs.iterator();
        while (iter.hasNext()) {
            newMacData.append(iter.next());
            if (iter.hasNext()) {
                newMacData.append(", ");
            }
        }
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("UPDATE accounts SET macs = ? WHERE id = ?");
            ps.setString(1, newMacData.toString());
            ps.setInt(2, accId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error saving MACs" + e);
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

    public void setAccID(int id) {
        this.accId = id;
    }

    public int getAccID() {
        return this.accId;
    }

    public final void updateLoginState(final int newstate, final String SessionID) { // TODO hide?
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("UPDATE accounts SET loggedin = ?, SessionIP = ?, lastlogin = CURRENT_TIMESTAMP() WHERE id = ?");
            ps.setInt(1, newstate);
            ps.setString(2, SessionID);
            ps.setInt(3, getAccID());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            System.err.println("error updating login state" + e);
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
        if (newstate == MapleClient.LOGIN_NOTLOGGEDIN) {
            loggedIn = false;
            serverTransition = false;
        } else {
            serverTransition = (newstate == MapleClient.LOGIN_SERVER_TRANSITION || newstate == MapleClient.CHANGE_CHANNEL);
            loggedIn = !serverTransition;
        }
    }

    public final void updateSecondPassword() {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();

            ps = con.prepareStatement("UPDATE `accounts` SET `2ndpassword` = ? WHERE id = ?");
            ps.setString(1, secondPassword);
            ps.setInt(2, accId);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("error updating login state" + e);
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

    public final byte getLoginState() { // TODO hide?
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT loggedin, lastlogin, banned, `birthday` + 0 AS `bday` FROM accounts WHERE id = ?");
            ps.setInt(1, getAccID());
            rs = ps.executeQuery();
            if (!rs.next() || rs.getInt("banned") > 0) {
                ps.close();
                rs.close();
                session.close();
                throw new DatabaseException("Account doesn't exist or is banned");
            }
            birthday = rs.getInt("bday");
            byte state = rs.getByte("loggedin");

            if (state == MapleClient.LOGIN_SERVER_TRANSITION || state == MapleClient.CHANGE_CHANNEL) {
                if (rs.getTimestamp("lastlogin").getTime() + 20000 < System.currentTimeMillis()) { // connecting to chanserver timeout
                    state = MapleClient.LOGIN_NOTLOGGEDIN;
                    updateLoginState(state, getSessionIPAddress());
                }
            }
            rs.close();
            ps.close();
            if (state == MapleClient.LOGIN_LOGGEDIN) {
                loggedIn = true;
            } else {
                loggedIn = false;
            }
            return state;
        } catch (SQLException e) {
            loggedIn = false;
            throw new DatabaseException("error getting login state", e);
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

    public final boolean checkBirthDate(final int date) {
        return birthday == date;
    }
    
    public boolean isEligible() {
        try ( Connection con = DatabaseConnection.getConnection()) {
            try ( PreparedStatement ps = con.prepareStatement("SELECT * FROM `master_ip` WHERE `ip` = ?")) {
                ps.setString(1, getSessionIPAddress());
                try ( ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException ex) {
            return false;
        }
    }    

    public final void removalTask(boolean shutdown) {
        try {
            dc = true;
            player.cancelAllBuffs_();
            player.cancelAllDebuffs();
            if (player.getMarriageId() > 0) {
                final MapleQuestStatus stat1 = player.getQuestNoAdd(MapleQuest.getInstance(160001));
                final MapleQuestStatus stat2 = player.getQuestNoAdd(MapleQuest.getInstance(160002));
                if (stat1 != null && stat1.getCustomData() != null && (stat1.getCustomData().equals("2_") || stat1.getCustomData().equals("2"))) {
                    //dc in process of marriage
                    if (stat2 != null && stat2.getCustomData() != null) {
                        stat2.setCustomData("0");
                    }
                    stat1.setCustomData("3");
                }
            }
            if (player.getMapId() == GameConstants.JAIL && !player.isIntern()) {
                final MapleQuestStatus stat1 = player.getQuestNAdd(MapleQuest.getInstance(GameConstants.JAIL_TIME));
                final MapleQuestStatus stat2 = player.getQuestNAdd(MapleQuest.getInstance(GameConstants.JAIL_QUEST));
                if (stat1.getCustomData() == null) {
                    stat1.setCustomData(String.valueOf(System.currentTimeMillis()));
                } else if (stat2.getCustomData() == null) {
                    stat2.setCustomData("0"); //seconds of jail
                } else { //previous seconds - elapsed seconds
                    int seconds = Integer.parseInt(stat2.getCustomData()) - (int) ((System.currentTimeMillis() - Long.parseLong(stat1.getCustomData())) / 1000);
                    if (seconds < 0) {
                        seconds = 0;
                    }
                    stat2.setCustomData(String.valueOf(seconds));
                }
            }
            player.changeRemoval(true);
            if (player.getEventInstance() != null) {
                player.getEventInstance().playerDisconnected(player, player.getId());
            }
            final IMaplePlayerShop shop = player.getPlayerShop();
            if (shop != null) {
                shop.removeVisitor(player, true);
                if (shop.isOwner(player)) {
                    if (shop.getShopType() == 1 && shop.isAvailable() && !shutdown) {
                        shop.setOpen(true);
                    } else {
                        shop.closeShop(true, !shutdown, false);
                    }
                }
            }
            player.setMessenger(null);
            if (player.getMap() != null) {
                if (shutdown || (getChannelServer() != null && getChannelServer().isShutdown())) {
                    int questID = -1;
                    switch (player.getMapId()) {
                        case 240060200: //HT
                            questID = 160100;
                            break;
                        case 240060201: //ChaosHT
                            questID = 160103;
                            break;
                        case 280030000: //Zakum
                            questID = 160101;
                            break;
                        case 280030001: //ChaosZakum
                            questID = 160102;
                            break;
                        case 270050100: //PB
                            questID = 160101;
                            break;
                        case 105100300: //Balrog
                        case 105100400: //Balrog
                            questID = 160106;
                            break;
                        case 211070000: //VonLeon
                        case 211070100: //VonLeon
                        case 211070101: //VonLeon
                        case 211070110: //VonLeon
                            questID = 160107;
                            break;
                        case 551030200: //scartar
                            questID = 160108;
                            break;
                        case 271040100: //cygnus
                            questID = 160109;
                            break;
                        case 801040100: //showa
                            questID = 160199;
                            break;   
                       case 123356785: //매그너스
                            questID = 160300;
                            break;
                       case 123356780: //힐라
                            questID = 160301;
                            break;
                       case 123356782: //스우
                            questID = 160302;
                            break;
                       case 123356781: //카오스파풀
                            questID = 160303;
                            break;
                       case 450004150: //루시드
                            questID = 160304;
                            break; 
                       case 123356788: //검마
                            questID = 160305;
                            break; 
                       case 123356786: //데미안
                            questID = 160306;
                            break;    
                        case 702060000: //murim
                            questID = 160198;
                            break;                               
                    }
                    if (questID > 0) {
                        player.getQuestNAdd(MapleQuest.getInstance(questID)).setCustomData("0"); //reset the time.
                    }
                } else if (player.isAlive()) {
                    switch (player.getMapId()) {
                        case 541010100: //latanica
                        case 541020800: //krexel
                        case 220080001: //pap
                            player.getMap().addDisconnected(player.getId());
                            break;
                    }
                }
                player.getMap().removePlayer(player);
            }
        } catch (final Throwable e) {
            FileoutputUtil.outputFileError(FileoutputUtil.Acc_Stuck, e);
        }
    }
    public final boolean getAcCheck() {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        boolean acAllowedCheck = true;
        
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT gm, allowed FROM accounts WHERE id = ?");
            ps.setInt(1, accId);
            rs = ps.executeQuery();
            if (rs.next()) {
                if (rs.getInt("allowed") == 1) {
                    acAllowedCheck = true;
                } else if (rs.getInt("gm") == 1) {
                    acAllowedCheck = true;
                } else {
                    acAllowedCheck = false;
                }
            }
        } catch (Exception e) {
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
        return acAllowedCheck;
    }

    public final void disconnect(final boolean RemoveInChannelServer, final boolean fromCS) {
        disconnect(RemoveInChannelServer, fromCS, false);
    }

    public final void disconnect(final boolean RemoveInChannelServer, final boolean fromCS, final boolean shutdown) {
        if (player != null) {
            MapleMap map = player.getMap();
            final MapleParty party = player.getParty();
            final String namez = player.getName();
            final int idz = player.getId(), messengerid = player.getMessenger() == null ? 0
                    : player.getMessenger().getId(), gid = player.getGuildId(), fid = player.getFamilyId();
            final MapleFamilyCharacter chrf = player.getMFC();
            final BuddyList bl = player.getBuddylist();
            final MaplePartyCharacter chrp = new MaplePartyCharacter(player);
            final MapleMessengerCharacter chrm = new MapleMessengerCharacter(
                    player);
            final MapleGuildCharacter chrg = player.getMGC();

            removalTask(shutdown);
            LoginServer.getLoginAuth(player.getId());
            if (!serverTransition && isLoggedIn()) { // 서순 변경
                updateLoginState(MapleClient.LOGIN_NOTLOGGEDIN,
                        getSessionIPAddress());
            }
            player.saveToDB(true, fromCS);
            
            ConnectorClientStorage clics = ConnectorServer.getInstance().getClientStorage();
            if (clics != null) {
                MapleClient c = player.getClient();
                ConnectorClient cli = clics.getClientByName(c.getAccountName());
                if (cli != null) {
                    cli.removeInGameChar(player.getName());
                    clics.registerChangeInGameCharWaiting(cli.toString(), cli.toString());
                }
            }
            if (shutdown) {
                player = null;
                receiving = false;
                return;
            }

            if (!fromCS) {
                final ChannelServer ch = ChannelServer
                        .getInstance(map == null ? channel : map.getChannel());
                final int chz = World.Find.findChannel(idz);
                if (chz < -1) {
                    disconnect(RemoveInChannelServer, true);// u lie
                    return;
                }
                try {
                    if (chz == -1 || ch == null || ch.isShutdown()) {
                        player = null;
                        return;// no idea
                    }
                    if (messengerid > 0) {
                        World.Messenger.leaveMessenger(messengerid, chrm);
                    }
                    if (party != null) {
                        chrp.setOnline(false);
                        World.Party.updateParty(party.getId(),
                                PartyOperation.LOG_ONOFF, chrp);
                        if (map != null && party.getLeader().getId() == idz) {
                            MaplePartyCharacter lchr = null;
                            for (MaplePartyCharacter pchr : party.getMembers()) {
                                if (pchr != null
                                        && map.getCharacterById(pchr.getId()) != null
                                        && (lchr == null || lchr.getLevel() < pchr
                                        .getLevel())) {
                                    lchr = pchr;
                                }
                            }
                            if (lchr != null) {
                                World.Party.updateParty(party.getId(), PartyOperation.CHANGE_LEADER_DC, lchr);
                            }
                        }
                    }
                    if (bl != null) {
                        if (!serverTransition) {
                            World.Buddy.loggedOff(namez, idz, channel,
                                    bl.getBuddyIds());
                        } else { // Change channel
                            World.Buddy.loggedOn(namez, idz, channel,
                                    bl.getBuddyIds());
                        }
                    }
                    if (gid > 0 && chrg != null) {
                        World.Guild.setGuildMemberOnline(chrg, false, -1);
                    }
                    if (fid > 0 && chrf != null) {
                        World.Family.setFamilyMemberOnline(chrf, false, -1);
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                    FileoutputUtil.outputFileError(FileoutputUtil.Acc_Stuck, e);
                    System.err.println(getLogMessage(this, "ERROR") + e);
                } finally {
                    if (RemoveInChannelServer && ch != null) {
                        ch.removePlayer(idz, namez);
                    }
                    player = null;
                }
            } else {
                final int ch = World.Find.findChannel(idz);
                if (ch > 0) {
                    disconnect(RemoveInChannelServer, false);// u lie
                    return;
                }
                try {
                    if (party != null) {
                        chrp.setOnline(false);
                        World.Party.updateParty(party.getId(),
                                PartyOperation.LOG_ONOFF, chrp);
                    }
                    if (!serverTransition) {
                        World.Buddy.loggedOff(namez, idz, channel,
                                bl.getBuddyIds());
                    } else { // Change channel
                        World.Buddy.loggedOn(namez, idz, channel,
                                bl.getBuddyIds());
                    }
                    if (gid > 0 && chrg != null) {
                        World.Guild.setGuildMemberOnline(chrg, false, -1);
                    }
                    if (player != null) {
                        player.setMessenger(null);
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                    FileoutputUtil.outputFileError(FileoutputUtil.Acc_Stuck, e);
                    System.err.println(getLogMessage(this, "ERROR") + e);
                } finally {
                    if (RemoveInChannelServer && ch > 0) {
                        CashShopServer.getPlayerStorage().deregisterPlayer(idz,
                                namez);
                    }
                    player = null;
                }
            }
        }

        //NPCScriptInvoker.dispose(this); //vm 스크는 나중에
        engines.clear();
    }

    public final String getSessionIPAddress() {
        return getIp();
    }

    public final boolean CheckIPAddress() {
        if (this.accId < 0) {
            return false;
        }
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT SessionIP, banned FROM accounts WHERE id = ?");
            ps.setInt(1, this.accId);
            rs = ps.executeQuery();

            boolean canlogin = false;

            if (rs.next()) {
                final String sessionIP = rs.getString("SessionIP");

                if (sessionIP != null) { // Probably a login proced skipper?
                    canlogin = getSessionIPAddress().equals(sessionIP.split(":")[0]);
                }
                if (rs.getInt("banned") > 0) {
                    canlogin = false; //canlogin false = close client
                }
            }
            rs.close();
            ps.close();
            con.close();//커넥션
            return canlogin;
        } catch (final SQLException e) {
            e.printStackTrace();
            System.out.println("Failed in checking IP address for client.");
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
        return true;
    }

    public final void DebugMessage(final StringBuilder sb) {
        sb.append(getSessionIPAddress());
        sb.append(" Closing: ");
        sb.append(stop);
        sb.append(" loggedin: ");
        sb.append(isLoggedIn());
        sb.append(" has char: ");
        sb.append(getPlayer() != null);
    }

    public final int getChannel() {
        return channel;
    }

    public final ChannelServer getChannelServer() {
        return ChannelServer.getInstance(channel);
    }

    public final int deleteCharacter(final int cid) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT guildid, guildrank, familyid, name FROM characters WHERE id = ? AND accountid = ?");
            ps.setInt(1, cid);
            ps.setInt(2, accId);
            rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return 1;
            }
            if (rs.getInt("guildid") > 0) { // is in a guild when deleted
                if (rs.getInt("guildrank") == 1) { //cant delete when leader
                    rs.close();
                    ps.close();
                    return 18;
                }
                World.Guild.deleteGuildCharacter(rs.getInt("guildid"), cid);
            }
            if (rs.getInt("familyid") > 0 && World.Family.getFamily(rs.getInt("familyid")) != null) {
                World.Family.getFamily(rs.getInt("familyid")).leaveFamily(cid);
            }
            final MapleSidekick s = World.Sidekick.getSidekickByChr(cid);
            if (s != null) {
                s.eraseToDB();
            }
            rs.close();
            ps.close();

            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM characters WHERE id = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "UPDATE pokemon SET active = 0 WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM hiredmerch WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM mts_cart WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM mts_items WHERE characterid = ?", cid);
            //MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM cheatlog WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM mountdata WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM inventoryitems WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM famelog WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM famelog WHERE characterid_to = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM dueypackages WHERE RecieverId = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM wishlist WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM buddies WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM buddies WHERE buddyid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM keymap WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM trocklocations WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM regrocklocations WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM hyperrocklocations WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM savedlocations WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM skills WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM familiars WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM mountdata WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM skillmacros WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM trocklocations WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM queststatus WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM inventoryslot WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM extendedSlots WHERE characterid = ?", cid);
            return 0;
        } catch (Exception e) {
            FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
            e.printStackTrace();
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
        return 1;
    }

    public final byte getGender() {
        return gender;
    }

    public final void setGender(final byte gender) {
        this.gender = gender;
    }

    public final String getSecondPassword() {
        return secondPassword;
    }

    public final void setSecondPassword(final String secondPassword) {
        this.secondPassword = secondPassword;
    }

    public final void updateSecondPasswordToNull() {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("UPDATE `accounts` SET `2ndpassword` = NULL, `salt2` = NULL WHERE id = ?");
            ps.setInt(1, accId);
            ps.executeUpdate();
            secondPassword = null;
            salt2 = null;
        } catch (Exception error) {
            error.printStackTrace();
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

    public final String getAccountName() {
        return accountName;
    }

    public final void setAccountName(final String accountName) {
        this.accountName = accountName;
    }

    public final void setChannel(final int channel) {
        this.channel = channel;
    }

    public final int getWorld() {
        return world;
    }

    public final void setWorld(final int world) {
        this.world = world;
    }

    public final int getLatency() {
        return (int) (lastPong - lastPing);
    }

    public final long getLastPong() {
        return lastPong;
    }

    public final long getLastPing() {
        return lastPing;
    }

    public final void pongReceived() {
        lastPong = System.currentTimeMillis();
    }

    public final void sendPing() {
        lastPing = System.currentTimeMillis();
        session.write(LoginPacket.getPing());
        PingTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    if (getLatency() < 0) {
                        disconnect(true, false);
                        if (!stop) {
                            getSession().close(true);
                        }
                    } else if (!stop) {
                        sendPing();
                    }
                } catch (final NullPointerException e) {
                    // client already gone
                }
            }
        }, 60000); // note: idletime gets added to this too
    }

    public static final String getLogMessage(final MapleClient cfor, final String message) {
        return getLogMessage(cfor, message, new Object[0]);
    }

    public static final String getLogMessage(final MapleCharacter cfor, final String message) {
        return getLogMessage(cfor == null ? null : cfor.getClient(), message);
    }

    public static final String getLogMessage(final MapleCharacter cfor, final String message, final Object... parms) {
        return getLogMessage(cfor == null ? null : cfor.getClient(), message, parms);
    }

    public static final String getLogMessage(final MapleClient cfor, final String message, final Object... parms) {
        final StringBuilder builder = new StringBuilder();
        if (cfor != null) {
            if (cfor.getPlayer() != null) {
                builder.append("<");
                builder.append(MapleCharacterUtil.makeMapleReadable(cfor.getPlayer().getName()));
                builder.append(" (cid: ");
                builder.append(cfor.getPlayer().getId());
                builder.append(")> ");
            }
            if (cfor.getAccountName() != null) {
                builder.append("(Account: ");
                builder.append(cfor.getAccountName());
                builder.append(") ");
            }
        }
        builder.append(message);
        int start;
        for (final Object parm : parms) {
            start = builder.indexOf("{}");
            builder.replace(start, start + 2, parm.toString());
        }
        return builder.toString();
    }

    public static final int findAccIdForCharacterName(final String charName) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
            ps.setString(1, charName);
            rs = ps.executeQuery();

            int ret = -1;
            if (rs.next()) {
                ret = rs.getInt("accountid");
            }
            rs.close();
            ps.close();

            return ret;
        } catch (final SQLException e) {
            System.err.println("findAccIdForCharacterName SQL error");
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
        return -1;
    }

    public final Set<String> getMacs() {
        return Collections.unmodifiableSet(macs);
    }

    public final boolean isGm() {
        return gm;
    }

    public final void setScriptEngine(final String name, final ScriptEngine e) {
        engines.put(name, e);
    }

    public final ScriptEngine getScriptEngine(final String name) {
        return engines.get(name);
    }

    public final void removeScriptEngine(final String name) {
        engines.remove(name);
    }

    public final ScheduledFuture<?> getIdleTask() {
        return idleTask;
    }

    public final void setIdleTask(final ScheduledFuture<?> idleTask) {
        this.idleTask = idleTask;
    }

    protected static final class CharNameAndId {

        public final String name;
        public final int id;

        public CharNameAndId(final String name, final int id) {
            super();
            this.name = name;
            this.id = id;
        }
    }

    public int getCharacterSlots() {
        if (isGm()) {
            return 15;
        }
        if (charslots != DEFAULT_CHARSLOT) {
            return charslots; //save a sql
        }
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        PreparedStatement psu = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM character_slots WHERE accid = ? AND worldid = ?");
            ps.setInt(1, accId);
            ps.setInt(2, world);
            rs = ps.executeQuery();
            if (rs.next()) {
                charslots = rs.getInt("charslots");
            } else {
                psu = con.prepareStatement("INSERT INTO character_slots (accid, worldid, charslots) VALUES (?, ?, ?)");
                psu.setInt(1, accId);
                psu.setInt(2, world);
                psu.setInt(3, charslots);
                psu.executeUpdate();
                psu.close();
            }
            rs.close();
            ps.close();
        } catch (SQLException sqlE) {
            sqlE.printStackTrace();
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

            if (psu != null) {
                try {
                    psu.close();
                } catch (Exception e) {
                }
            }
        }

        return charslots;
    }

    public boolean gainCharacterSlot() {
        if (getCharacterSlots() >= 15) {
            return false;
        }
        charslots++;
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("UPDATE character_slots SET charslots = ? WHERE worldid = ? AND accid = ?");
            ps.setInt(1, charslots);
            ps.setInt(2, world);
            ps.setInt(3, accId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException sqlE) {
            sqlE.printStackTrace();
            return false;
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
        return true;
    }

    public static final byte unbanIPMacs(String charname) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        PreparedStatement psa = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT accountid from characters where name = ?");
            ps.setString(1, charname);

            rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return -1;
            }
            final int accid = rs.getInt(1);
            rs.close();
            ps.close();

            ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
            ps.setInt(1, accid);
            rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return -1;
            }
            final String sessionIP = rs.getString("sessionIP");
            final String macs = rs.getString("macs");
            rs.close();
            ps.close();
            byte ret = 0;
            if (sessionIP != null) {
                psa = con.prepareStatement("DELETE FROM ipbans WHERE ip like ?");
                psa.setString(1, sessionIP);
                psa.execute();
                psa.close();
                ret++;
            }
            if (macs != null) {
                String[] macz = macs.split(", ");
                for (String mac : macz) {
                    if (!mac.equals("")) {
                        psa = con.prepareStatement("DELETE FROM macbans WHERE mac = ?");
                        psa.setString(1, mac);
                        psa.execute();
                        psa.close();
                    }
                }
                ret++;
            }
            return ret;
        } catch (SQLException e) {
            System.err.println("Error while unbanning" + e);
            return -2;
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

            if (psa != null) {
                try {
                    psa.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public static final byte unHellban(String charname) {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT accountid from characters where name = ?");
            ps.setString(1, charname);

            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return -1;
            }
            final int accid = rs.getInt(1);
            rs.close();
            ps.close();

            ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
            ps.setInt(1, accid);
            rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return -1;
            }
            final String sessionIP = rs.getString("sessionIP");
            final String email = rs.getString("email");
            rs.close();
            ps.close();
            ps = con.prepareStatement("UPDATE accounts SET banned = 0, banreason = '' WHERE email = ?" + (sessionIP == null ? "" : " OR sessionIP = ?"));
            ps.setString(1, email);
            if (sessionIP != null) {
                ps.setString(2, sessionIP);
            }
            ps.execute();
            ps.close();
            return 0;
        } catch (SQLException e) {
            System.err.println("Error while unbanning" + e);
            return -2;
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

    public boolean isMonitored() {
        return monitored;
    }

    public void setMonitored(boolean m) {
        this.monitored = m;
    }

    public boolean isReceiving() {
        return receiving;
    }

    public void setReceiving(boolean m) {
        this.receiving = m;
    }

    public boolean canClickNPC() {
        return lastNpcClick + 500 < System.currentTimeMillis();
    }

    public void setClickedNPC() {
        lastNpcClick = System.currentTimeMillis();
    }

    public void removeClickedNPC() {
        lastNpcClick = 0;
    }

    public final Timestamp getCreated() { // TODO hide?
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT createdat FROM accounts WHERE id = ?");
            ps.setInt(1, getAccID());
            rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return null;
            }
            Timestamp ret = rs.getTimestamp("createdat");
            rs.close();
            ps.close();
            return ret;
        } catch (SQLException e) {
            throw new DatabaseException("error getting create", e);
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

    public String getTempIP() {
        return tempIP;
    }

    public void setTempIP(String s) {
        this.tempIP = s;
    }
    boolean dc = false;

    public boolean isDC() {
        return dc;
    }

  //  public boolean isLocalhost() {
//        return ServerConstants.Use_Localhost || ServerConstants.isIPLocalhost(getSessionIPAddress());
//    }
    
    public final void Claim(LittleEndianAccessor slea, MapleClient c) {
        System.out.println("신고하기");
        byte type = slea.readByte(); // 신고타입
        String tt = null;
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        switch (type) {
            case 0:
                tt = "불법프로그램";
                sb2.append(slea.readMapleAsciiString()); // 캐릭이름
                slea.skip(1);
                sb.append(slea.readMapleAsciiString()); // 신고내용 
                break;
            case 1:
                tt = "기타신고";
                sb2.append(slea.readMapleAsciiString()); // 캐릭이름
                slea.skip(1);
                sb.append(slea.readMapleAsciiString()); // 신고내용 
                sb.append(slea.readMapleAsciiString()); // 신고내용 
                break;
        }

        MapleCharacter chr2 = c.getChannelServer().getPlayerStorage().getCharacterByName(sb2.toString());

        if (c.getPlayer().getMeso() >= 1000) {
            if (c.getPlayer().getCharId(sb2.toString()) == 0) {
                c.getSession().write(MaplePacketCreator.getClaimMessage(66, 0, 0)); // 캐릭터이름 확인
            } else if (!c.getPlayer().getClaim().ClaimWeekMaxCount()) {
                c.getSession().write(MaplePacketCreator.getClaimMessage(69, 0, 0)); // 이미 신고 횟수 초과
            } else if (!c.getPlayer().getClaim().ClaimDayMaxCount()) {
                c.getSession().write(MaplePacketCreator.getClaimMessage(69, 0, 0)); // 이미 신고 횟수 초과
            } else if (c.getPlayer().getClaim().banCheckClaim()) {
                c.getSession().write(MaplePacketCreator.getClaimMessage(71, 0, 0)); // 허위신고 제재당한사람
            } else {// 성공 
                Connection con = null;
                PreparedStatement ps = null;
                try {
                    con = DatabaseConnection.getConnection();
                    ps = con.prepareStatement("INSERT INTO claim (type, name, suspect, content) VALUES (?, ?, ?, ?)");
                    ps.setString(1, tt);
                    ps.setString(2, c.getPlayer().getName());
                    ps.setString(3, sb2.toString());
                    ps.setString(4, sb.toString());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    System.out.println(e + " Claim");
                } finally {
                    if (con != null) {
                        try {
                            con.close();
                            con = null;
                        } catch (SQLException ex) {
                            Logger.getLogger(MapleClient.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    if (ps != null) {
                        try {
                            ps.close();
                            ps = null;
                        } catch (SQLException ex) {
                            Logger.getLogger(MapleClient.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                c.getPlayer().getClaim().PlusClaimCount();
                if (chr2 != null) {
                    chr2.getClient().getSession().write(MaplePacketCreator.getClaimMessage(3, 0, 0));
                }

                if (c.getPlayer().getClaim().ClaimDayCount() >= c.getPlayer().getClaim().daymaxcount) {
                    c.getSession().write(MaplePacketCreator.getClaimMessage(2, 0, 0));
                    //성공적으로 접수하였습니다.\r\n오늘은 더 이상 신고할 수 없습니다.
                } else if (c.getPlayer().getClaim().ClaimWeekCount() < c.getPlayer().getClaim().weekmaxcount) {
                    c.getSession().write(MaplePacketCreator.getClaimMessage(2, 1, c.getPlayer().getClaim().weekmaxcount - c.getPlayer().getClaim().ClaimWeekCount()));
                    //성공적으로 접수하였습니다.\r\n이번 주에 %d회 더 신고하실 수 있습니다.
                } else if (c.getPlayer().getClaim().ClaimWeekCount() >= c.getPlayer().getClaim().weekmaxcount) {
                    c.getSession().write(MaplePacketCreator.getClaimMessage(2, 1, 0));
                    //성공적으로 접수하였습니다.\r\n이번 주에는 더 이상 신고하실 수 없습니다.
                }
            }
        } else {
            c.getPlayer().dropMessage(1, "신고를 하기 위해선\r\n1000메소가 필요합니다.");
        }

    }

    public boolean isLocalhost() {
        return ServerConstants.isIPLocalhost(getSessionIPAddress());
    }    
        
    public void setconnecterClient(ConnectorClient c) {
        this.connecterClient = c;
    }

    public ConnectorClient getconnecterClient() {
        return connecterClient;
    }
    
}
