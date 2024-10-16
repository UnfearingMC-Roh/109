/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package connector;

import constants.ServerConstants;
import client.MapleCharacter;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.channel.handler.DueyHandler;
import handling.world.World;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import server.MedalRanking;
import server.Start;
import server.marriage.MarriageManager;
import server.shops.MinervaOwlSearchTop;
import tools.CPUSampler;
import tools.MaplePacketCreator;
import tools.data.MaplePacketLittleEndianWriter;

/**
 * @author 글귀
 */
public class ConnectorPanel extends javax.swing.JFrame implements ActionListener {

    static String txtMsg, txtOpt;
    static int intOpt;
    
    private MapleCharacter getPlayer(String name) {
        for (ChannelServer cserv : ChannelServer.getAllInstances()) {
            if (cserv.getPlayerStorage().getCharacterByName(name) != null)
                return cserv.getPlayerStorage().getCharacterByName(name);
        }
        return null;
        
    }
    public ConnectorPanel() {
        initComponents();
        ConnectorPanel.jTable2.getTableHeader().setReorderingAllowed(false);
        DefaultTableModel model = (DefaultTableModel) jTable2.getModel();
        /*
         String filePath = "KickList.txt";
         DefaultTableModel model = (DefaultTableModel) jTable2.getModel();
         try (
         InputStream inStream = new FileInputStream(filePath);
         InputStreamReader reader = new InputStreamReader(inStream, "UTF-8");
         BufferedReader bufReader = new BufferedReader(reader);) {
         StringBuilder sb = new StringBuilder();
         String line = null;
         while ((line = bufReader.readLine()) != null) {
         sb.append(line).append('\n');
         model.addRow(new Object[]{line});
         }
         if (sb.length() > 0) {
         sb.setLength(sb.length() - 1);
         }
         } catch (FileNotFoundException e) {
         e.printStackTrace();
         } catch (IOException e) {
         e.printStackTrace();
         };
         */
        Connection con = null;

        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM connectorban");
            rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getString("ip").length() > 3) {
                    model.addRow(new Object[]{rs.getString("ip")});
                }
            }
        } catch (SQLException e) {

        } finally {
            if (con != null) {
                try {
                    con.close();
                    con = null;
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                    ps = null;
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                    rs = null;
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }
        if (ServerConstants.ConnecterLog) {
            jCheckBox1.setSelected(true);
        } else {
            jCheckBox1.setSelected(false);
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jFrame2 = new javax.swing.JFrame();
        jButton5 = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jTextField1 = new javax.swing.JTextField();
        jButton6 = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        jTextField4 = new javax.swing.JTextField();
        jButton14 = new javax.swing.JButton();
        jButton13 = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTextArea2 = new javax.swing.JTextArea();
        jButton4 = new javax.swing.JButton();
        jCheckBox1 = new javax.swing.JCheckBox();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jButton10 = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        jTextField2 = new javax.swing.JTextField();
        jTextField3 = new javax.swing.JTextField();
        jButton8 = new javax.swing.JButton();
        jTextField5 = new javax.swing.JTextField();
        jTextField6 = new javax.swing.JTextField();
        jButton9 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jTextField7 = new javax.swing.JTextField();
        jTextField8 = new javax.swing.JTextField();
        jButton11 = new javax.swing.JButton();
        jRadioButton1 = new javax.swing.JRadioButton();
        jRadioButton2 = new javax.swing.JRadioButton();
        jTextField9 = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jTextField10 = new javax.swing.JTextField();
        jTextField11 = new javax.swing.JTextField();
        jTextField12 = new javax.swing.JTextField();
        jButton12 = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        jTextField13 = new javax.swing.JTextField();
        jButton15 = new javax.swing.JButton();
        jButton16 = new javax.swing.JButton();
        jButton18 = new javax.swing.JButton();
        jScrollPane6 = new javax.swing.JScrollPane();
        jTextArea3 = new javax.swing.JTextArea();
        cubeDayValue = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        cubeDayStart = new javax.swing.JButton();
        cubeDayEnd = new javax.swing.JButton();

        jFrame2.setName("jFrame2"); // NOI18N
        jFrame2.setResizable(false);

        jButton5.setText("강제종료");
        jButton5.setName("jButton5"); // NOI18N
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        jScrollPane3.setName("jScrollPane3"); // NOI18N

        jTable3.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Process Name", "Window Title"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jTable3.setName("jTable3"); // NOI18N
        jTable3.getTableHeader().setReorderingAllowed(false);
        jTable3.getTableHeader().setResizingAllowed(false);
        jScrollPane3.setViewportView(jTable3);

        org.jdesktop.layout.GroupLayout jFrame2Layout = new org.jdesktop.layout.GroupLayout(jFrame2.getContentPane());
        jFrame2.getContentPane().setLayout(jFrame2Layout);
        jFrame2Layout.setHorizontalGroup(
            jFrame2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jFrame2Layout.createSequentialGroup()
                .add(jScrollPane3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 475, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(0, 0, Short.MAX_VALUE))
            .add(jFrame2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jButton5)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jFrame2Layout.setVerticalGroup(
            jFrame2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jFrame2Layout.createSequentialGroup()
                .add(jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 449, Short.MAX_VALUE)
                .add(18, 18, 18)
                .add(jButton5)
                .addContainerGap())
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setName("Form"); // NOI18N
        setResizable(false);

        jTabbedPane1.setName("jTabbedPane1"); // NOI18N

        jPanel1.setAlignmentX(0.0F);
        jPanel1.setAlignmentY(0.0F);
        jPanel1.setName("jPanel1"); // NOI18N

        jScrollPane1.setBorder(null);
        jScrollPane1.setName("jScrollPane1"); // NOI18N

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "아이디", "비밀번호", "아이피", "접속중인캐릭터", "클라이언트", "대표캐릭터"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class, java.lang.Object.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jTable1.setName("jTable1"); // NOI18N
        jTable1.getTableHeader().setReorderingAllowed(false);
        jTable1.getTableHeader().setResizingAllowed(false);
        jTable1.setShowHorizontalLines(false);
        jTable1.setShowVerticalLines(false);
        jScrollPane1.setViewportView(jTable1);
        if (jTable1.getColumnModel().getColumnCount() > 0) {
            jTable1.getColumnModel().getColumn(1).setMinWidth(0);
            jTable1.getColumnModel().getColumn(1).setMaxWidth(0);
            jTable1.getColumnModel().getColumn(4).setMinWidth(0);
            jTable1.getColumnModel().getColumn(4).setMaxWidth(0);
        }

        jButton1.setText("영구퇴장");
        jButton1.setToolTipText("");
        jButton1.setName("jButton1"); // NOI18N
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText("강제퇴장");
        jButton2.setName("jButton2"); // NOI18N
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setText("리스트");
        jButton3.setName("jButton3"); // NOI18N
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jTextField1.setText("jTextField1");
        jTextField1.setName("jTextField1"); // NOI18N

        jButton6.setText("전체 공지");
        jButton6.setName("jButton6"); // NOI18N
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });

        jButton7.setText("선택 공지");
        jButton7.setName("jButton7"); // NOI18N
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 440, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(0, 0, Short.MAX_VALUE))
            .add(jPanel1Layout.createSequentialGroup()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jButton2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 100, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jButton1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 100, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jButton3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 100, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(0, 0, Short.MAX_VALUE))
                    .add(jTextField1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jButton6)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jButton7))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 388, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jButton3)
                    .add(jButton1)
                    .add(jButton2)
                    .add(jButton7))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jButton6)
                        .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jTextField1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(0, 0, Short.MAX_VALUE))))
        );

        jTabbedPane1.addTab("접속자", jPanel1);

        jPanel2.setName("jPanel2"); // NOI18N

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "IP"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jTable2.setName("jTable2"); // NOI18N
        jScrollPane2.setViewportView(jTable2);

        jTextField4.setText("127.0.0.1");
        jTextField4.setName("jTextField4"); // NOI18N

        jButton14.setText("추가");
        jButton14.setName("jButton14"); // NOI18N
        jButton14.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton14ActionPerformed(evt);
            }
        });

        jButton13.setText("삭제");
        jButton13.setToolTipText("");
        jButton13.setName("jButton13"); // NOI18N
        jButton13.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton13ActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .add(jScrollPane2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 273, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(0, 167, Short.MAX_VALUE))
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(jButton14, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 111, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(jButton13, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 107, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(jTextField4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 244, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .add(jScrollPane2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 392, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jTextField4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jButton14)
                    .add(jButton13))
                .add(0, 0, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("아이피 차단", jPanel2);

        jPanel3.setName("jPanel3"); // NOI18N

        jScrollPane4.setName("jScrollPane4"); // NOI18N

        jTextArea2.setColumns(20);
        jTextArea2.setRows(5);
        jTextArea2.setName("jTextArea2"); // NOI18N
        jScrollPane4.setViewportView(jTextArea2);

        jButton4.setText("청소");
        jButton4.setName("jButton4"); // NOI18N
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jCheckBox1.setSelected(true);
        jCheckBox1.setText("로그기록");
        jCheckBox1.setName("jCheckBox1"); // NOI18N
        jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox1ActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 440, Short.MAX_VALUE)
            .add(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .add(jButton4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 100, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(18, 18, 18)
                .add(jCheckBox1)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .add(jScrollPane4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 403, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jButton4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 52, Short.MAX_VALUE)
                    .add(jPanel3Layout.createSequentialGroup()
                        .add(jCheckBox1)
                        .add(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        jTabbedPane1.addTab("로그", jPanel3);

        jPanel4.setName("jPanel4"); // NOI18N

        jScrollPane5.setName("jScrollPane5"); // NOI18N

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jTextArea1.setName("jTextArea1"); // NOI18N
        jScrollPane5.setViewportView(jTextArea1);

        jButton10.setText("청소");
        jButton10.setName("jButton10"); // NOI18N
        jButton10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton10ActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel4Layout = new org.jdesktop.layout.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane5, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 440, Short.MAX_VALUE)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jButton10, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel4Layout.createSequentialGroup()
                .add(jScrollPane5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 406, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jButton10, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 59, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("채팅로그", jPanel4);

        jPanel5.setName("jPanel5"); // NOI18N

        jTextField2.setText("닉네임");
        jTextField2.setName("jTextField2"); // NOI18N

        jTextField3.setText("전송갯수");
        jTextField3.setName("jTextField3"); // NOI18N

        jButton8.setText("캐시지급");
        jButton8.setName("jButton8"); // NOI18N
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });

        jTextField5.setText("닉네임");
        jTextField5.setName("jTextField5"); // NOI18N

        jTextField6.setText("전송갯수");
        jTextField6.setName("jTextField6"); // NOI18N

        jButton9.setText("메포지급");
        jButton9.setName("jButton9"); // NOI18N
        jButton9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton9ActionPerformed(evt);
            }
        });

        jLabel1.setText("< 아이템 지급 >");
        jLabel1.setName("jLabel1"); // NOI18N

        jTextField7.setText("아이템코드");
        jTextField7.setName("jTextField7"); // NOI18N

        jTextField8.setText("개수");
        jTextField8.setName("jTextField8"); // NOI18N

        jButton11.setText("지급");
        jButton11.setName("jButton11"); // NOI18N
        jButton11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton11ActionPerformed(evt);
            }
        });

        jRadioButton1.setText("모두에게 지급");
        jRadioButton1.setName("jRadioButton1"); // NOI18N

        jRadioButton2.setText("개인에게 지급");
        jRadioButton2.setName("jRadioButton2"); // NOI18N

        jTextField9.setText("닉네임");
        jTextField9.setName("jTextField9"); // NOI18N

        jLabel3.setText("< 경험치 배율 >");
        jLabel3.setName("jLabel3"); // NOI18N

        jLabel4.setText("< 드랍 배율 >");
        jLabel4.setName("jLabel4"); // NOI18N

        jLabel5.setText("< 메소 배율 >");
        jLabel5.setName("jLabel5"); // NOI18N

        jTextField10.setText("3");
        jTextField10.setName("jTextField10"); // NOI18N

        jTextField11.setText("1");
        jTextField11.setName("jTextField11"); // NOI18N

        jTextField12.setText("1");
        jTextField12.setName("jTextField12"); // NOI18N
        jTextField12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField12ActionPerformed(evt);
            }
        });

        jButton12.setText("적용");
        jButton12.setName("jButton12"); // NOI18N
        jButton12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton12ActionPerformed(evt);
            }
        });

        jLabel6.setText("< 최적화&지엠권한 >");
        jLabel6.setName("jLabel6"); // NOI18N

        jTextField13.setText("닉네임");
        jTextField13.setName("jTextField13"); // NOI18N
        jTextField13.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField13ActionPerformed(evt);
            }
        });

        jButton15.setText("GM주기");
        jButton15.setName("jButton15"); // NOI18N
        jButton15.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton15ActionPerformed(evt);
            }
        });

        jButton16.setText("GM박탈");
        jButton16.setName("jButton16"); // NOI18N
        jButton16.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton16ActionPerformed(evt);
            }
        });

        jButton18.setText("버닝시작");
        jButton18.setName("jButton18"); // NOI18N
        jButton18.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton18ActionPerformed(evt);
            }
        });

        jScrollPane6.setName("jScrollPane6"); // NOI18N

        jTextArea3.setColumns(20);
        jTextArea3.setRows(5);
        jTextArea3.setText("1개 = 1000원");
        jTextArea3.setName("jTextArea3"); // NOI18N
        jScrollPane6.setViewportView(jTextArea3);

        cubeDayValue.setText("1.0");
        cubeDayValue.setName("cubeDayValue"); // NOI18N
        cubeDayValue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cubeDayValueActionPerformed(evt);
            }
        });

        jLabel2.setText("< 미라클 큐브 데이 >");
        jLabel2.setName("jLabel2"); // NOI18N

        cubeDayStart.setText("시작");
        cubeDayStart.setName("cubeDayStart"); // NOI18N
        cubeDayStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cubeDayStartActionPerformed(evt);
            }
        });

        cubeDayEnd.setText("종료");
        cubeDayEnd.setName("cubeDayEnd"); // NOI18N
        cubeDayEnd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cubeDayEndActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel5Layout = new org.jdesktop.layout.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel5Layout.createSequentialGroup()
                        .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel6)
                            .add(jPanel5Layout.createSequentialGroup()
                                .add(jTextField13, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 109, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jButton15)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jButton16))
                            .add(jLabel3)
                            .add(jPanel5Layout.createSequentialGroup()
                                .add(10, 10, 10)
                                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jPanel5Layout.createSequentialGroup()
                                        .add(jTextField10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 69, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(37, 37, 37)
                                        .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                            .add(jLabel4)
                                            .add(jPanel5Layout.createSequentialGroup()
                                                .add(10, 10, 10)
                                                .add(jTextField11, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 53, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                                        .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                                            .add(jPanel5Layout.createSequentialGroup()
                                                .add(18, 18, 18)
                                                .add(jLabel5))
                                            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel5Layout.createSequentialGroup()
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .add(jTextField12, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 53, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                .add(13, 13, 13))))
                                    .add(jButton12, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 263, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 49, Short.MAX_VALUE)
                        .add(jButton18)
                        .addContainerGap())
                    .add(jPanel5Layout.createSequentialGroup()
                        .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jPanel5Layout.createSequentialGroup()
                                .add(cubeDayValue, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 109, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(cubeDayStart, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 77, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(cubeDayEnd, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 77, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(jPanel5Layout.createSequentialGroup()
                                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jPanel5Layout.createSequentialGroup()
                                        .add(jRadioButton2)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(jTextField9, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 57, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                    .add(jRadioButton1)
                                    .add(jPanel5Layout.createSequentialGroup()
                                        .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                            .add(jTextField7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 101, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                            .add(jLabel1))
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(jTextField8, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 45, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                                .add(19, 19, 19)
                                .add(jButton11, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 70, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(jPanel5Layout.createSequentialGroup()
                                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jPanel5Layout.createSequentialGroup()
                                        .add(jTextField2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 72, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                        .add(jTextField3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 82, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(jButton8))
                                    .add(jPanel5Layout.createSequentialGroup()
                                        .add(jTextField5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 72, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(12, 12, 12)
                                        .add(jTextField6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 82, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(jButton9)))
                                .add(12, 12, 12)
                                .add(jScrollPane6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(jLabel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 131, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(0, 0, Short.MAX_VALUE))))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(jPanel5Layout.createSequentialGroup()
                        .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jTextField2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jTextField3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jButton8))
                        .add(71, 71, 71)
                        .add(jLabel1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jTextField7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jTextField8, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jRadioButton1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jRadioButton2)
                            .add(jTextField9, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel5Layout.createSequentialGroup()
                        .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                .add(jButton9)
                                .add(jTextField6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(jTextField5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(jScrollPane6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                        .add(18, 18, 18)
                        .add(jButton11, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 91, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .add(18, 18, 18)
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(jPanel5Layout.createSequentialGroup()
                        .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel3)
                            .add(jLabel4)
                            .add(jLabel5))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jTextField10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jTextField11, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jTextField12, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jButton12))
                    .add(jButton18, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .add(34, 34, 34)
                .add(jLabel6)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jTextField13, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jButton15)
                    .add(jButton16))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel2)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(cubeDayValue, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(cubeDayStart)
                    .add(cubeDayEnd))
                .addContainerGap(59, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("관리메뉴", jPanel5);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jTabbedPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jTabbedPane1)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton13ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton13ActionPerformed
        if (jTable2.getSelectedRow() <= -1) {
            txtMsg = "선택하지 않았습니다.";
            txtOpt = "오류";
            intOpt = JOptionPane.ERROR_MESSAGE;
            ConnectorPanel.Alert al = new ConnectorPanel.Alert();
            al.start();
            return;
        }
        int sel = jTable2.getSelectedRow();
        DefaultTableModel model = (DefaultTableModel) jTable2.getModel();

        model.removeRow(sel);
        BufferedWriter bw;
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < model.getRowCount(); i++) {
                sb.append(model.getValueAt(i, 0)).append("\r\n");
            }
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("KickList.txt")));
            bw.write(sb.toString());
            bw.flush();
            bw.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ConnectorPanel.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            //Logger.getLogger(ConnectorPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButton13ActionPerformed

    public void logChat(String a) {
        jTextArea2.append(a);
        jTextArea2.append("\r\n");
    }

    private void jButton14ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton14ActionPerformed
        DefaultTableModel model = (DefaultTableModel) jTable2.getModel();
        model.addRow(new Object[]{this.jTextField4.getText()});

        FileOutputStream out = null;
        String txt = null;
        try {
            out = new FileOutputStream("KickList.txt", true);
            txt = model.getValueAt(model.getRowCount() - 1, 0) + "\r\n";
            out.write(txt.getBytes(Charset.forName("UTF-8")));
        } catch (IOException ess) {
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ignore) {
            }
        }
    }//GEN-LAST:event_jButton14ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        ConnectorClient cc = (ConnectorClient) jTable1.getValueAt(jTable1.getSelectedRow(), 4);
        cc.getSession().close();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        if (jTable1.getSelectedRow() <= -1) {
            txtMsg = "선택하지 않았습니다.";
            txtOpt = "오류";
            intOpt = JOptionPane.ERROR_MESSAGE;
            ConnectorPanel.Alert al = new ConnectorPanel.Alert();
            al.start();
            return;
        }
        DefaultTableModel model = (DefaultTableModel) jTable2.getModel();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        int accountid = 0;
        boolean secondAcc = false;
        String text = "SELECT * FROM accounts WHERE name = ?";
        try {
            con = DatabaseConnection.getConnection();
            if (jTable1.getValueAt(jTable1.getSelectedRow(), 0).toString().contains(",")) {
                for (int i = 1; i < jTable1.getValueAt(jTable1.getSelectedRow(), 0).toString().split(",").length; i++) {
                    text += " or name = ?";
                }
                secondAcc = true;
            }
            ConnectorClient cc = (ConnectorClient) jTable1.getValueAt(jTable1.getSelectedRow(), 4);

            FileOutputStream out = null;
            String txt = null;
            try {
                out = new FileOutputStream("KickList.txt", true);
                txt = jTable1.getValueAt(jTable1.getSelectedRow(), 2).toString().split("/")[1].split(":")[0] + "\r\n";
                out.write(txt.getBytes(Charset.forName("UTF-8")));
            } catch (IOException ess) {
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException ignore) {
                }
                model.addRow(new Object[]{jTable1.getValueAt(jTable1.getSelectedRow(), 2).toString().split("/")[1].split(":")[0]});
            }
            ps = con.prepareStatement(text);
            if (secondAcc) {
                System.out.println(jTable1.getValueAt(jTable1.getSelectedRow(), 0).toString().split(",").length);
                for (int i = 1; i <= jTable1.getValueAt(jTable1.getSelectedRow(), 0).toString().split(",").length; i++) {
                    ps.setString(i, jTable1.getValueAt(jTable1.getSelectedRow(), 0).toString().split(",")[i - 1]);
                }
                rs = ps.executeQuery();

                while (rs.next()) {
                    String conkey = rs.getString("connecterKey");
                    ps = con.prepareStatement("INSERT INTO connectorban (`connecterkey`, `ip`) VALUES (?, ?)");
                    ps.setString(1, conkey);
                    ps.setString(2, rs.getString("SessionIP"));
                    ps.execute();

                    ps = con.prepareStatement("UPDATE accounts SET banned = 1, banreason = ?, banby = '운영자' WHERE name = ?");
                    ps.setString(1, "영구 퇴장 당하셨습니다.");
                    ps.setString(2, rs.getString("name"));
                    ps.executeUpdate();
                }
                cc.getSession().close();
            } else {
                ps = con.prepareStatement("SELECT * FROM accounts WHERE name = ?");
                ps.setString(1, jTable1.getValueAt(jTable1.getSelectedRow(), 0).toString());
                rs = ps.executeQuery();
                if (rs.next()) {
                    ps = con.prepareStatement("UPDATE accounts SET banned = 1, banreason = ?, banby = '운영자' WHERE name = ?");
                    ps.setString(1, "영구 퇴장 당하셨습니다.");
                    ps.setString(2, rs.getString("name"));
                    ps.executeUpdate();
                }
                cc.getSession().close();
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
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        jFrame2.setVisible(true);
        jFrame2.setSize(480, 440);
        if (jTable1.getSelectedRow() < 0) {
            txtMsg = "선택하지 않았습니다.";
            txtOpt = "오류";
            intOpt = JOptionPane.ERROR_MESSAGE;
            ConnectorPanel.Alert al = new ConnectorPanel.Alert();
            al.start();
            return;
        }
        ConnectorClient cc = (ConnectorClient) jTable1.getValueAt(jTable1.getSelectedRow(), 4);
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write(6);
        cc.sendPacket(mplew.getPacket());
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        ConnectorPanel.jTextArea2.setText("");
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        String procn = jTable3.getValueAt(jTable3.getSelectedRow(), 0).toString();
        ConnectorClient cc = (ConnectorClient) jTable1.getValueAt(jTable1.getSelectedRow(), 4);
        cc.send(PacketCreator.sendProcEnd(procn));
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox1ActionPerformed
        ServerConstants.ConnecterLog = jCheckBox1.isSelected();
    }//GEN-LAST:event_jCheckBox1ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        try {
            World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("GM전체메세지 : " + jTextField1.getText()));
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(0x05);
            mplew.write(0x07);
            mplew.writeMapleAsciiString2(this.jTextField1.getText());
            ConnectorServerHandler.AllMessage(mplew.getPacket());
        } catch (Exception e) {
            System.out.println(e);
        }
    }//GEN-LAST:event_jButton6ActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        if (jTable1.getSelectedRow() < 0) {
            txtMsg = "선택하지 않았습니다.";
            txtOpt = "오류";
            intOpt = JOptionPane.ERROR_MESSAGE;
            ConnectorPanel.Alert al = new ConnectorPanel.Alert();
            al.start();
            return;
        }
        ConnectorClient cc = (ConnectorClient) jTable1.getValueAt(jTable1.getSelectedRow(), 4);
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write(0x05);
        mplew.write(0x07);
        mplew.writeMapleAsciiString2(this.jTextField1.getText());
        cc.sendPacket(mplew.getPacket());
        for (String sp : jTable1.getValueAt(jTable1.getSelectedRow(), 3).toString().split(",")) {
            if (World.Find.findChannel(sp) >= 0) {
                MapleCharacter chr = ChannelServer.getInstance(World.Find.findChannel(sp)).getPlayerStorage().getCharacterByName(sp);
                if (chr != null) {
                    chr.getMap().broadcastMessage(MaplePacketCreator.yellowChat("GM개인메세지 : " + jTextField1.getText()));
                }
            }
        }
    }//GEN-LAST:event_jButton7ActionPerformed

    private void jButton10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton10ActionPerformed
        ConnectorPanel.jTextArea1.setText("");
        //c.getSession().write();
    }//GEN-LAST:event_jButton10ActionPerformed

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
            String txtname = jTextField2.getText();
            int txtitemcost = Integer.parseInt(jTextField3.getText());
            MapleCharacter hp = getPlayer(txtname);
            int cid,channel;
        
            cid = client.MapleCharacterUtil.getIdByName(txtname);
            channel = handling.world.World.Find.findChannel(txtname);

            if (channel >= 0) {
                hp.getClient().getSession().write(MaplePacketCreator.sendDuey((byte) 28, null, null));
                handling.world.World.Broadcast.sendPacket(cid, tools.MaplePacketCreator.serverNotice(5, "아이템이 지급되었습니다. NPC 택배원 <듀이> 에게서 아이템을 수령하세요!"));
            }
            handling.channel.handler.DueyHandler.addNewItemToDb(2432000, txtitemcost, cid, txtname, "캐시 보상이 지급되었습니다. 받을 개수가 틀릴경우 운영진에게 연락바랍니다.", channel >= 0);
            JOptionPane.showMessageDialog(null, txtname + "님께 아이템이 지급되었습니다.", "성공", JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_jButton8ActionPerformed

    private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton9ActionPerformed
            String txtname = jTextField5.getText();
            int txtitemcost = Integer.parseInt(jTextField6.getText());
            MapleCharacter hp = getPlayer(txtname);
            int cid,channel;
        
            cid = client.MapleCharacterUtil.getIdByName(txtname);
            channel = handling.world.World.Find.findChannel(txtname);

            if (channel >= 0) {
                hp.getClient().getSession().write(MaplePacketCreator.sendDuey((byte) 28, null, null));
                handling.world.World.Broadcast.sendPacket(cid, tools.MaplePacketCreator.serverNotice(5, "아이템이 지급되었습니다. NPC 택배원 <듀이> 에게서 아이템을 수령하세요!"));
            }
            handling.channel.handler.DueyHandler.addNewItemToDb(2431013, txtitemcost, cid, txtname, "홍보 보상이 지급되었습니다. 받을 개수가 틀릴경우 운영진에게 연락바랍니다.", channel >= 0);
            JOptionPane.showMessageDialog(null, txtname + "님께 메포아이템이 지급되었습니다.", "성공", JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_jButton9ActionPerformed

    private void jButton11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton11ActionPerformed
          // TODO add your handling code here:
        MapleCharacter victim = null;
        int vicid = -1;
        if (jRadioButton2.isSelected()) {
            /*
            for (ChannelServer csv : ChannelServer.getAllInstances()) {
                if (victim == null) {
                    victim = csv.getPlayerStorage().getCharacterByName(jTextField5.getText());
                    continue;
                }
            }*/
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM characters WHERE name = ?");
            ps.setString(1, jTextField9.getText());
            rs = ps.executeQuery();
            if (rs.next()) {
                vicid = rs.getInt("id");
            }
        } catch (SQLException e) {
            System.err.println("Error getting character default" + e);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (Exception e) {
            }
        }
            if (vicid != -1) {
                World.Broadcast.sendPacket(vicid, MaplePacketCreator.receiveParcel("서버보상", true));
                World.Broadcast.sendPacket(vicid, MaplePacketCreator.serverNotice(5, "운영자에 의해 아이템이 지급되었습니다. NPC 택배원 <듀이> 에게서 아이템을 수령하세요!"));
                DueyHandler.addNewItemToDb(Integer.parseInt(jTextField7.getText()), Integer.parseInt(jTextField8.getText()), vicid, "[보상]", "운영자에 의해 아이템이 지급되었습니다.", true);
            } else {
                txtMsg = "해당 유저는 없습니다.";
                txtOpt = "오류";
                intOpt = JOptionPane.ERROR_MESSAGE;
                ConnectorPanel.Alert al = new ConnectorPanel.Alert();
                al.start();
                return;
            }
        } else {
            for (ChannelServer csv : ChannelServer.getAllInstances()) {
                for (MapleCharacter temp : csv.getPlayerStorage().getAllCharacters())
                if (temp != null) {
                    
                            World.Broadcast.sendPacket(temp.getId(), MaplePacketCreator.receiveParcel("서버보상", true));
                            World.Broadcast.sendPacket(temp.getId(), MaplePacketCreator.serverNotice(5, "운영자에 의해 아이템이 지급되었습니다. NPC 택배원 <듀이> 에게서 아이템을 수령하세요!"));
                            DueyHandler.addNewItemToDb(Integer.parseInt(jTextField7.getText()), Integer.parseInt(jTextField8.getText()), temp.getId(), "[보상]", "운영자에 의해 아이템이 지급되었습니다.", true);
                }
                    //MapleInventoryManipulator.addById(temp.getClient(), Integer.parseInt(jTextField2.getText()), Short.parseShort(jTextField3.getText()), "");
            }
        }
        
    }//GEN-LAST:event_jButton11ActionPerformed

    private void jButton12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton12ActionPerformed
           // TODO add your handling code here:
        int exprate = Integer.parseInt(jTextField10.getText());
        int droprate = Integer.parseInt(jTextField11.getText());
        int mesorate = Integer.parseInt(jTextField12.getText());
            for (ChannelServer csv : ChannelServer.getAllInstances()) {
                csv.setExpRate(exprate);
                csv.setDropRate(droprate);
                csv.setMesoRate(mesorate);
                for (MapleCharacter temp : csv.getPlayerStorage().getAllCharacters())
                    temp.dropMessage(5, "핫타임 이벤트! 배율상승..");
            }
    }//GEN-LAST:event_jButton12ActionPerformed

    private void jTextField12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField12ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField12ActionPerformed

    private void jButton15ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton15ActionPerformed
        // TODO add your handling code here:
        MapleCharacter victim = null;
            for (ChannelServer csv : ChannelServer.getAllInstances()) {
                if (victim == null) {
                    victim = csv.getPlayerStorage().getCharacterByName(jTextField13.getText());
                    continue;
                }
            }
            if (victim != null) {
                victim.setGMLevel((byte) 10);
                victim.dropMessage(1, "GM권한을 부여받았습니다.");
            } else {
                
                txtMsg = "해당 유저가 접속 중이 아닙니다.";
                txtOpt = "오류";
                intOpt = JOptionPane.ERROR_MESSAGE;
                ConnectorPanel.Alert al = new ConnectorPanel.Alert();
                al.start();
                return;
            }
    }//GEN-LAST:event_jButton15ActionPerformed

    private void jButton16ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton16ActionPerformed
        // TODO add your handling code here:
        MapleCharacter victim = null;
            for (ChannelServer csv : ChannelServer.getAllInstances()) {
                if (victim == null) {
                    victim = csv.getPlayerStorage().getCharacterByName(jTextField13.getText());
                    continue;
                }
            }
            if (victim != null) {
                victim.setGMLevel((byte) 0);
                victim.dropMessage(1, "GM권한을 박탈 당하였습니다.");
            } else {
                
                txtMsg = "해당 유저가 접속 중이 아닙니다.";
                txtOpt = "오류";
                intOpt = JOptionPane.ERROR_MESSAGE;
                ConnectorPanel.Alert al = new ConnectorPanel.Alert();
                al.start();
                return;
            }
    }//GEN-LAST:event_jButton16ActionPerformed

    private void jButton18ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton18ActionPerformed
           // TODO add your handling code here:
            for (ChannelServer csv : ChannelServer.getAllInstances()) {
                csv.setExpRate(40);
                csv.setDropRate(2);
                csv.setMesoRate(3);
                for (MapleCharacter temp : csv.getPlayerStorage().getAllCharacters())
                 World.Broadcast.sendPacket(temp.getId(), MaplePacketCreator.serverNotice(5, "경험치 2배 주말 버닝이 시작 되었습니다!"));
                 //JOptionPane.showMessageDialog(null, "버닝시작", "성공", JOptionPane.INFORMATION_MESSAGE);
            }
    }//GEN-LAST:event_jButton18ActionPerformed

    private void jTextField13ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField13ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField13ActionPerformed

    private void cubeDayValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cubeDayValueActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_cubeDayValueActionPerformed

    private void cubeDayStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cubeDayStartActionPerformed
        try {
            double val = Double.parseDouble(cubeDayValue.getText());
            if (val < 1.0) {
                txtMsg = "1.0 이상의 숫자를 입력해주세요.";
                txtOpt = "오류";
                intOpt = JOptionPane.ERROR_MESSAGE;
                ConnectorPanel.Alert al = new ConnectorPanel.Alert();
                al.start();
                return;
            }
            ServerConstants.cubeDayValue = val;
            World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("[이벤트] 미라클 큐브 데이 이벤트가 시작 되었습니다. " + ServerConstants.cubeDayValue + "배 등급업의 기회를 노려보세요!"));
        } catch (NumberFormatException e) {
            txtMsg = "제대로 된 숫자를 입력해주세요.";
            txtOpt = "오류";
            intOpt = JOptionPane.ERROR_MESSAGE;
            ConnectorPanel.Alert al = new ConnectorPanel.Alert();
            al.start();
        }
    }//GEN-LAST:event_cubeDayStartActionPerformed

    private void cubeDayEndActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cubeDayEndActionPerformed
        ServerConstants.cubeDayValue = 1.0;
        World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("[이벤트] 미라클 큐브 데이 이벤트가 종료 되었습니다."));
    }//GEN-LAST:event_cubeDayEndActionPerformed

    @Override
    public void actionPerformed(ActionEvent evt) {
        // JMenuItem menu = (JMenuItem) evt.getSource();

        jFrame2.setVisible(true);
        jFrame2.setSize(480, 440);
    }

    class Alert extends Thread {

        @Override
        public void run() {
            try {
                Alert(txtMsg, txtOpt, intOpt);
            } catch (Exception e) {

            }
        }
    }

    private void Alert(String msg, String opt, int opt2) {
        JOptionPane.showMessageDialog(null, msg, opt, opt2);
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ConnectorPanel().setVisible(true);
            }
        });
    }

    public static int getModelId(String ip) {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        for (int i = model.getRowCount() - 1; i >= 0; i--) {
            if (model.getValueAt(i, 2).toString().equals(ip)) {
                return i;
            }
        }
        return 1;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cubeDayEnd;
    private javax.swing.JButton cubeDayStart;
    private javax.swing.JTextField cubeDayValue;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton12;
    private javax.swing.JButton jButton13;
    private javax.swing.JButton jButton14;
    private javax.swing.JButton jButton15;
    private javax.swing.JButton jButton16;
    private javax.swing.JButton jButton18;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JCheckBox jCheckBox1;
    public static javax.swing.JFrame jFrame2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JRadioButton jRadioButton1;
    private javax.swing.JRadioButton jRadioButton2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JTabbedPane jTabbedPane1;
    public static javax.swing.JTable jTable1;
    public static javax.swing.JTable jTable2;
    public static final javax.swing.JTable jTable3 = new javax.swing.JTable();
    public static javax.swing.JTextArea jTextArea1;
    public static javax.swing.JTextArea jTextArea2;
    private javax.swing.JTextArea jTextArea3;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField10;
    private javax.swing.JTextField jTextField11;
    private javax.swing.JTextField jTextField12;
    private javax.swing.JTextField jTextField13;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField jTextField7;
    private javax.swing.JTextField jTextField8;
    private javax.swing.JTextField jTextField9;
    // End of variables declaration//GEN-END:variables
}
