package tools;

import constants.ServerConstants;
import database.DatabaseOption;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import server.GeneralThreadPool;
import static server.Start.SQL_PASSWORD;
import static server.Start.SQL_USER;
import server.Timer.WorldTimer;

/**
 *
 * @author
 */
public class DatabaseBackup {

    public static DatabaseBackup instance = null;

    public static DatabaseBackup getInstance() {
        if (instance == null) {
            instance = new DatabaseBackup();
        }
        return instance;
    }

    private static boolean isName(String name) {
        String osname = "windows";

        if ((osname == null) || (osname.length() <= 0)) {
            return false;
        }

        osname = osname.toLowerCase();
        name = name.toLowerCase();

        if (osname.indexOf(name) >= 0) {
            return true;
        }

        return false;
    }

    /*public void startTasking() {//1229
//        WorldTimer tMan = WorldTimer.getInstance();
        Runnable r = new Runnable() {
            public void run() {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHH");
                    String name = sdf.format(Calendar.getInstance().getTime());
                    Process p = null;
                    p = Runtime.getRuntime().exec("cmd /C mysqldump -u" + DatabaseOption.MySQLUSER + " -p" + DatabaseOption.MySQLPASS + " maplestory95 > dbbackup\\" + name + ".sql");
                    p.getInputStream().read();
                    System.out.println("[DBBackup] DB Backup Completed.");
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
//        System.out.println("[DBBackup] DB Backup Started.");
//        GeneralThreadPool.getInstance().execute(r);
        r.run();
        //tMan.register(r, 3600000);
    }*/
    
    public void startTasking() {
//        WorldTimer tMan = WorldTimer.getInstance();
        Runnable r = new Runnable() {
            public void run() {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHH");
                    String name = sdf.format(Calendar.getInstance().getTime());
                    Process p = null;
                    if (isName("linux")) {
                        p = Runtime.getRuntime().exec("cmd /C mysqldump -u" + SQL_USER + " -p" + SQL_PASSWORD + " maplestory109 > dbbackup\\" + name + ".sql");
                        System.out.print("1");
                    } else {
                        p = Runtime.getRuntime().exec("cmd /C mysqldump -u" + SQL_USER + " -p" + SQL_PASSWORD + " maplestory109 > dbbackup\\" + name + ".sql");
                    }
                    p.getInputStream().read();
                    try {
                        p.waitFor();
                    } finally {
                        p.destroy();
                    }
                    System.out.println("[DBBackup] DB Backup Completed.");
                    if (isName("windows")) {
                        System.out.println("[DBBackup] Compressing DB Backup SQL by GunZip.");
                        p = Runtime.getRuntime().exec("cmd /C Backzip -9 dbbackup\\" + name + ".sql");
                        p.getInputStream().read();
                        try {
                            p.waitFor();
                        } finally {
                            p.destroy();
                        }
                        System.out.println("[DBBackup] Successfully Compressed DB Backup SQL by GunZip.");
                        File toDel = new File("dbbackup\\" + name + ".sql");
                        toDel.delete();
                    }

                    String name2 = sdf.format(new Date(System.currentTimeMillis() - (86400000L * 14)));
                    File del = null;
                    if (isName("windows")) {
                        del = new File("dbbackup\\" + name2 + ".sql");
                    } else if (isName("linux")) {
                        del = new File("dbbackup/" + name2 + ".sql");
                    }
                    if ((del != null) && (del.exists())) {
                        del.delete();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        
        r.run();
    }
    
}
