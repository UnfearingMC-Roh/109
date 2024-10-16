/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package connector;

import java.awt.Point;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import tools.data.MaplePacketLittleEndianWriter;

/**
 * @author SLFCG & 글귀
 */
public class PacketCreator {

    public static byte[] sShiftKey = new byte[]{
        (byte) 0xEC, (byte) 0x3F, (byte) 0x77, (byte) 0xA4, (byte) 0x45, (byte) 0xD0, (byte) 0x71, (byte) 0xBF,
        (byte) 0xB7, (byte) 0x98, (byte) 0x20, (byte) 0xFC, (byte) 0x4B, (byte) 0xE9, (byte) 0xB3, (byte) 0xE1,
        (byte) 0x5C, (byte) 0x22, (byte) 0xF7, (byte) 0x0C, (byte) 0x44, (byte) 0x1B, (byte) 0x81, (byte) 0xBD,
        (byte) 0x63, (byte) 0x8D, (byte) 0xD4, (byte) 0xC3, (byte) 0xF2, (byte) 0x10, (byte) 0x19, (byte) 0xE0
    };

    public static final byte[] sendHandShake(final byte[] sendiv, final byte[] recviv) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write(sendiv);
        mplew.write(recviv);
        byte[] qwer = mplew.getPacket();
        for (int a = 0; a < mplew.getPacket().length; a++) {
            qwer[a] ^= sShiftKey[a % 32];
        }
        return qwer;
    }

    public static final byte[] sendEnd() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write((byte) SendPacketOpcode.END.getValue());
        return mplew.getPacket();
    }

    public static final byte[] sendPing(final int a) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write((byte) SendPacketOpcode.PING.getValue());
        mplew.writeInt(a);
        return mplew.getPacket();
    }

    public static final byte[] sendMessageBox(String msg, String title) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write(SendPacketOpcode.MESSAGE_BOX.getValue());
        mplew.writeAsciiString(msg);
        mplew.writeAsciiString(title);
        return mplew.getPacket();
    }

    public static final byte[] sendProcEnd(String prn) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write(SendPacketOpcode.PROCESS_END.getValue());
        mplew.writeMapleAsciiString2(prn);
        return mplew.getPacket();
    }

    public static final byte[] sendHappy(String a) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write((byte) SendPacketOpcode.HAPPY_NARU.getValue());
        mplew.writeMapleAsciiString(a);
        return mplew.getPacket();
    }

    public static final byte[] sendShuvi(String a, boolean noDownload) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write((byte) SendPacketOpcode.SHUVI.getValue());
        mplew.write(noDownload ? 0 : 1);
        mplew.writeMapleAsciiString(a);
        return mplew.getPacket();
    }

    public static final byte[] sendCharInfo(String name) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write((byte) SendPacketOpcode.CHAR_INFO.getValue());
        mplew.writeMapleAsciiString2(name);//이름
        mplew.write(ConnectorServerHandler.getCharacterInfo(name, "level"));//레벨
        mplew.writeShort(ConnectorServerHandler.getCharacterInfo(name, "skincolor"));//몸색
        mplew.writeInt(ConnectorServerHandler.getCharacterInfo(name, "hair"));//헤어
        mplew.writeShort(ConnectorServerHandler.getCharacterInfo(name, "face"));//성형
        ConnectorServerHandler.getCharItems(ConnectorServerHandler.getCharacterId(name), mplew);
        return mplew.getPacket();
    }

    public static final byte[] sendInGameChat(String a, byte b) {
        /*
         1 팝업
         2 하늘색바탕파란색글씨
         3 고확인듯;?
         4 버프줄 노란색공지
         5 바탕x 빨간글씨
         6 바탕x 파란글씨
         7 옐로우
         */
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write((byte) SendPacketOpcode.INGAME_CHAT.getValue());
        mplew.write((byte) b);
        mplew.writeMapleAsciiString2(a);
        return mplew.getPacket();
    }

    public static final byte[] sendInGameChat(byte[] a, byte b, ConnectorClient c) {
        /*
         1 팝업
         2 하늘색바탕파란색글씨
         3 고확인듯;?
         4 버프줄 노란색공지
         5 바탕x 빨간글씨
         6 바탕x 파란글씨
         7 옐로우
         */
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write((byte) SendPacketOpcode.INGAME_CHAT.getValue());
        mplew.write((byte) b);
        mplew.writeMapleAsciiString2((c.getCharName() == null ? c.getId() : c.getCharName()) + " : ");
        mplew.write(a);
        return mplew.getPacket();
    }

    public static final byte[] sendUserList() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write((byte) SendPacketOpcode.USER_LIST.getValue());
        mplew.writeMapleAsciiString2(ConnectorServerHandler.userList());
        return mplew.getPacket();
    }

    public static final byte[] sendCharList(int code) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write((byte) SendPacketOpcode.CHAR_LIST.getValue());
        mplew.writeMapleAsciiString2(ConnectorServerHandler.getCharacterList(code));
        return mplew.getPacket();
    }

    public static final byte[] sendProcessKillList(ConnectorClient c) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write((byte) SendPacketOpcode.PROCESS_KILL.getValue());
        try {
            byte[] a = c.processKillList().getBytes("UTF-8");
            mplew.writeShort(a.length);
            mplew.write(a);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(PacketCreator.class.getName()).log(Level.SEVERE, null, ex);
        }

        return mplew.getPacket();
    }

    public static final byte[] sendEditMemory(ConnectorClient c) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write((byte) SendPacketOpcode.MEMORY_EDIT.getValue());
        mplew.writeMapleAsciiString2(c.FileLoad("Adress.txt"));
        return mplew.getPacket();
    }

    public static final byte[] sendResolution(ConnectorClient c) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write((byte) SendPacketOpcode.RESOLUTION.getValue());
        Point p = c.getMyRESOLUTION();
        mplew.writeInt(p.x);
        mplew.writeInt(p.y);
        return mplew.getPacket();
    }
    
    public static final byte[] sendGameStartKet(int key) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write((byte) SendPacketOpcode.GAME_START_KEY.getValue());
        mplew.writeInt(key);
        return mplew.getPacket();
    }
}
