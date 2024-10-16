package server;

import constants.GameConstants;
import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import database.DatabaseConnection;
import java.sql.Connection;

/**
 *
 * @author Emilyx3
 */
public class ServerProperties {

    public static final String WZ_PATH;    
    private static final Properties props = new Properties();

    private ServerProperties() {
    }

    static {
        WZ_PATH = System.getProperty("net.sf.odinms.wzpath", "wz");
        System.setProperty("net.sf.odinms.wzpath", WZ_PATH);
        String toLoad = "config/channel.properties";
        
        loadProperties(toLoad);
        if (getProperty("GMS") != null) {
            GameConstants.GMS = Boolean.parseBoolean(getProperty("GMS"));
        }
        Connection con = null;//커넥션
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM auth_server_channel_ip");
            rs = ps.executeQuery();
            while (rs.next()) {
                //if (rs.getString("name").equalsIgnoreCase("gms")) {
                //    GameConstants.GMS = Boolean.parseBoolean(rs.getString("value"));
                //} else {
                props.put(rs.getString("name") + rs.getInt("channelid"), rs.getString("value"));
                //}
            }
            rs.close();
            ps.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
            System.exit(0); //Big ass error.
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
        toLoad = GameConstants.GMS ? "worldGMS.properties" : "config/world.properties";
        loadProperties(toLoad);

    }

    public static void loadProperties(String s) {
        FileReader fr;
        try {
            fr = new FileReader(s);
            props.load(fr);
            fr.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static String getProperty(String s) {
        return props.getProperty(s);
    }

    public static void setProperty(String prop, String newInf) {
        props.setProperty(prop, newInf);
    }

    public static String getProperty(String s, String def) {
        return props.getProperty(s, def);
    }
}
