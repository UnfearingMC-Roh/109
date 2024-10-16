package connector;

import client.MapleCharacter;
import client.MapleClient;
import static connector.ConnectorServerHandler.ConnecterLog;
import constants.ServerConstants;
import handling.channel.ChannelServer;
import static java.lang.Thread.sleep;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.table.DefaultTableModel;
import server.Randomizer;

/**
 * @author 글귀
 */
public class ConnectorThread extends Thread {

    public ConnectorThread() {
    }

    public void run() {
        //System.out.println("스레드 시작");
        while (true) {
            try {
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                        try {
                            if (chr != null) {
                                MapleClient c = chr.getClient();
                                if (c != null) {
                                    if (!chr.hasGmLevel(1)) {
                                        ConnectorClient cli = ConnectorServer.getInstance().getClientStorage().getClientByName(c.getAccountName());
                                        if (cli != null) {
                                            c.setconnecterClient(cli);
                                            if ((cli.getId() != null && cli.getId().equals(c.getAccountName()))
                                                    || (cli.getSecondId() != null && cli.getSecondId().equals(c.getAccountName()))) {
                                                continue;
                                            }
                                            if (ServerConstants.ConnectorSetting) {
                                                System.out.println("비정상 접속 2 : " + chr.getName());
                                                c.sclose();
                                            }
                                        } else {
                                            if (ServerConstants.ConnectorSetting) {
                                                System.out.println("비정상 접속 : " + chr.getName() + " | " + chr.getAccountID() + "");
                                                c.sclose();
                                            }
                                        }
                                    }
                                } else {
                                    if (ServerConstants.ConnectorSetting) {
                                        System.out.println("비정상 접속3 : " + chr.getName() + " | " + chr.getAccountID() + "");
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            System.out.println("캐릭터검사도중 오류 발생\r\n");
                            ex.printStackTrace();
                        }
                    }
                }

                ConnectorClientStorage cs = ConnectorServer.getInstance().getClientStorage();
                try {
                    DefaultTableModel model = (DefaultTableModel) ConnectorPanel.jTable1.getModel();
                    for (int i = model.getRowCount() - 1; i >= 0; i--) {
                        String[] names = model.getValueAt(i, 0).toString().split(",");
                        ConnectorClient cli = cs.getClientByName(names[0]);
                        if (cli == null && names.length > 1) {
                            try {
                                cli = cs.getClientByName(model.getValueAt(i, 0).toString().split(",")[1]);
                            } catch (Exception ex) {
                                System.out.println("두번쨰닉\r\n" + ex);
                                ex.printStackTrace();
                            }
                        }
                        if (model.getValueAt(i, 4) != null) {
                            /*
                             접속중인 캐릭터 변경
                             */
                            try {
                                if (cs.getChangeInGameCharWaiting(model.getValueAt(i, 4).toString()) != null) {
                                    if (cli != null) {
                                        model.setValueAt(cli.getIngameCharString(), i, 3);
                                    }
                                    cs.deregisterChangeInGameCharWaiting(model.getValueAt(i, 4).toString());
                                }
                            } catch (Exception ex) {
                                System.out.println("체인지 인게임\r\n" + ex);
                                ex.printStackTrace();
                            }
                            /*
                             접속종료
                             */
                            try {
                                if (cs.getRemoveWaiting(model.getValueAt(i, 4).toString()) != null) {
                                    cs.deregisterRemoveWaiting(model.getValueAt(i, 4).toString());
                                    model.removeRow(i);
                                }
                            } catch (Exception ex) {
                                System.out.println("리무브 웨이팅\r\n" + ex);
                                ex.printStackTrace();
                            }
                        } else {
                            cs.deregisterRemoveWaiting(model.getValueAt(i, 4).toString());
                        }

                    }
                } catch (Exception ex) {
                    System.out.println("모델을 제거하는 도중 오류 발생\r\n" + ex);
                }
            } catch (Exception e) {
                System.out.println("쓰레드 오류 발생" + e);
            } finally {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ConnectorThread.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public static void main(String[] args) {
        ConnectorThread CT = new ConnectorThread();
        CT.start();
    }

}
