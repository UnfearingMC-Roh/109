package handling.login.handler;
import client.LoginCryptoLegacy;
import client.MapleClient;
import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import tools.MaplePacketCreator;
import tools.packet.LoginPacket;

public class AutoRegister {
    
    public static final int ACCOUNTS_IP_COUNT = 1; //한 IP당 회원가입 제한수
    public static final boolean AutoRegister = true; //자동가입 사용 여부
    public static boolean CheckAccount(String id) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT name FROM accounts WHERE name = ?");
            ps.setString(1, id);
            rs = ps.executeQuery();
            if (rs.first()) {
                return true;
            }
            rs.close();
            ps.close();
            con.close();//커넥션
        } catch (Exception ex) {
            System.out.println(ex);
            return false;
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
                // ang sex
            } catch (Exception e) {

            }
        }
        return false;
    }
    public static void createAccount(String id, String pwd, String ip, final MapleClient c) {
        Connection con = null;
        PreparedStatement ps = null;
        PreparedStatement ipc = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ipc = con.prepareStatement("SELECT SessionIP FROM accounts WHERE SessionIP = ?");
            ipc.setString(1, ip);
            rs = ipc.executeQuery();
            if (rs.first() == false || rs.last() == true && rs.getRow() < ACCOUNTS_IP_COUNT) {
                try {
                    ps = con.prepareStatement("INSERT INTO accounts (name, password, email, birthday, macs, SessionIP) VALUES (?, ?, ?, ?, ?, ?)");
                    ps.setString(1, id);
                    ps.setString(2, pwd);
                    ps.setString(3, "no@email.com");
                    ps.setString(4, "2013-12-25");
                    ps.setString(5, "00-00-00-00-00-00");
                    ps.setString(6, ip);
                    ps.executeUpdate();
                    rs.close();
                    c.clearInformation();
                    c.getSession().write(LoginPacket.getLoginFailed(20));
                    c.getSession().write(MaplePacketCreator.serverNotice(1, "회원가입을 축하드립니다.\r\n즐거운 메이플되세요."));
                } catch (SQLException ex) {
                    System.out.println(ex);
                }
            } else {
                c.clearInformation();
                c.getSession().write(LoginPacket.getLoginFailed(20));
                c.getSession().write(MaplePacketCreator.serverNotice(1, "아이피당 회원가입 제한 횟수를 초과하였습니다."));
            }
            rs.close();
            ipc.close();
            ps.close();
            con.close();//커넥션
        } catch (SQLException ex) {
            System.out.println(ex);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (ipc != null) {
                    ipc.close();
                }
                if (con != null) {
                    con.close();
                }

            } catch (Exception e) {

            }
        }
    }
}