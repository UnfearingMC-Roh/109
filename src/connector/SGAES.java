package connector;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import tools.HexTool;

/**
 * @author SLFCG
 */
public class SGAES {

    private final static byte[] sShiftKey = new byte[]{(byte) 0xEC, (byte) 0x3F, (byte) 0x77, (byte) 0xA4, (byte) 0x45, (byte) 0xD0, (byte) 0x71, (byte) 0xBF, (byte) 0xB7, (byte) 0x98, (byte) 0x20, (byte) 0xFC,
        (byte) 0x4B, (byte) 0xE9, (byte) 0xB3, (byte) 0xE1, (byte) 0x5C, (byte) 0x22, (byte) 0xF7, (byte) 0x0C, (byte) 0x44, (byte) 0x1B, (byte) 0x81, (byte) 0xBD, (byte) 0x63, (byte) 0x8D, (byte) 0xD4, (byte) 0xC3,
        (byte) 0xF2, (byte) 0x10, (byte) 0x19, (byte) 0xE0, (byte) 0xFB, (byte) 0xA1, (byte) 0x6E, (byte) 0x66, (byte) 0xEA, (byte) 0xAE, (byte) 0xD6, (byte) 0xCE, (byte) 0x06, (byte) 0x18, (byte) 0x4E, (byte) 0xEB,
        (byte) 0x78, (byte) 0x95, (byte) 0xDB, (byte) 0xBA, (byte) 0xB6, (byte) 0x42, (byte) 0x7A, (byte) 0x2A, (byte) 0x83, (byte) 0x0B, (byte) 0x54, (byte) 0x67, (byte) 0x6D, (byte) 0xE8, (byte) 0x65, (byte) 0xE7,
        (byte) 0x2F, (byte) 0x07, (byte) 0xF3, (byte) 0xAA, (byte) 0x27, (byte) 0x7B, (byte) 0x85, (byte) 0xB0, (byte) 0x26, (byte) 0xFD, (byte) 0x8B, (byte) 0xA9, (byte) 0xFA, (byte) 0xBE, (byte) 0xA8, (byte) 0xD7,
        (byte) 0xCB, (byte) 0xCC, (byte) 0x92, (byte) 0xDA, (byte) 0xF9, (byte) 0x93, (byte) 0x60, (byte) 0x2D, (byte) 0xDD, (byte) 0xD2, (byte) 0xA2, (byte) 0x9B, (byte) 0x39, (byte) 0x5F, (byte) 0x82, (byte) 0x21,
        (byte) 0x4C, (byte) 0x69, (byte) 0xF8, (byte) 0x31, (byte) 0x87, (byte) 0xEE, (byte) 0x8E, (byte) 0xAD, (byte) 0x8C, (byte) 0x6A, (byte) 0xBC, (byte) 0xB5, (byte) 0x6B, (byte) 0x59, (byte) 0x13, (byte) 0xF1,
        (byte) 0x04, (byte) 0x00, (byte) 0xF6, (byte) 0x5A, (byte) 0x35, (byte) 0x79, (byte) 0x48, (byte) 0x8F, (byte) 0x15, (byte) 0xCD, (byte) 0x97, (byte) 0x57, (byte) 0x12, (byte) 0x3E, (byte) 0x37, (byte) 0xFF,
        (byte) 0x9D, (byte) 0x4F, (byte) 0x51, (byte) 0xF5, (byte) 0xA3, (byte) 0x70, (byte) 0xBB, (byte) 0x14, (byte) 0x75, (byte) 0xC2, (byte) 0xB8, (byte) 0x72, (byte) 0xC0, (byte) 0xED, (byte) 0x7D, (byte) 0x68,
        (byte) 0xC9, (byte) 0x2E, (byte) 0x0D, (byte) 0x62, (byte) 0x46, (byte) 0x17, (byte) 0x11, (byte) 0x4D, (byte) 0x6C, (byte) 0xC4, (byte) 0x7E, (byte) 0x53, (byte) 0xC1, (byte) 0x25, (byte) 0xC7, (byte) 0x9A,
        (byte) 0x1C, (byte) 0x88, (byte) 0x58, (byte) 0x2C, (byte) 0x89, (byte) 0xDC, (byte) 0x02, (byte) 0x64, (byte) 0x40, (byte) 0x01, (byte) 0x5D, (byte) 0x38, (byte) 0xA5, (byte) 0xE2, (byte) 0xAF, (byte) 0x55,
        (byte) 0xD5, (byte) 0xEF, (byte) 0x1A, (byte) 0x7C, (byte) 0xA7, (byte) 0x5B, (byte) 0xA6, (byte) 0x6F, (byte) 0x86, (byte) 0x9F, (byte) 0x73, (byte) 0xE6, (byte) 0x0A, (byte) 0xDE, (byte) 0x2B, (byte) 0x99,
        (byte) 0x4A, (byte) 0x47, (byte) 0x9C, (byte) 0xDF, (byte) 0x09, (byte) 0x76, (byte) 0x9E, (byte) 0x30, (byte) 0x0E, (byte) 0xE4, (byte) 0xB2, (byte) 0x94, (byte) 0xA0, (byte) 0x3B, (byte) 0x34, (byte) 0x1D,
        (byte) 0x28, (byte) 0x0F, (byte) 0x36, (byte) 0xE3, (byte) 0x23, (byte) 0xB4, (byte) 0x03, (byte) 0xD8, (byte) 0x90, (byte) 0xC8, (byte) 0x3C, (byte) 0xFE, (byte) 0x5E, (byte) 0x32, (byte) 0x24, (byte) 0x50,
        (byte) 0x1F, (byte) 0x3A, (byte) 0x43, (byte) 0x8A, (byte) 0x96, (byte) 0x41, (byte) 0x74, (byte) 0xAC, (byte) 0x52, (byte) 0x33, (byte) 0xF0, (byte) 0xD9, (byte) 0x29, (byte) 0x80, (byte) 0xB1, (byte) 0x16,
        (byte) 0xD3, (byte) 0xAB, (byte) 0x91, (byte) 0xB9, (byte) 0x84, (byte) 0x7F, (byte) 0x61, (byte) 0x1E, (byte) 0xCF, (byte) 0xC5, (byte) 0xD1, (byte) 0x56, (byte) 0x3D, (byte) 0xCA, (byte) 0xF4, (byte) 0x05,
        (byte) 0xC6, (byte) 0xE5, (byte) 0x08, (byte) 0x49};

    private static byte[] key = {
        (byte) 0xda, (byte) 0xd6, (byte) 0x29, (byte) 0xa8, (byte) 0xc6, (byte) 0xc0, (byte) 0xaf, (byte) 0xe5,
        (byte) 0x02, (byte) 0x3c, (byte) 0x77, (byte) 0xc0, (byte) 0xb7, (byte) 0xe8, (byte) 0xef, (byte) 0x86,
        (byte) 0x2b, (byte) 0x80, (byte) 0xb7, (byte) 0xdd, (byte) 0xef, (byte) 0xd8, (byte) 0x23, (byte) 0xbd,
        (byte) 0x95, (byte) 0x6b, (byte) 0xa8, (byte) 0xd7, (byte) 0x27, (byte) 0xdd, (byte) 0xbd, (byte) 0xbd};

    public static byte[] key2 = new byte[]{
        (byte) 0xAA, (byte) 0x95, (byte) 0xE5, (byte) 0xB0, (byte) 0xF5, (byte) 0x79, (byte) 0x9A, (byte) 0x92,
        (byte) 0xA8, (byte) 0xE6, (byte) 0xB2, (byte) 0x38, (byte) 0xB4, (byte) 0xC5, (byte) 0x06, (byte) 0x91
    };

    private byte[] mIV;
    private final transient Lock mutex = new ReentrantLock(true);

    public SGAES(byte[] _iv) {
        this.mIV = _iv;
    }

    public byte[] getIv() {
        return this.mIV;
    }

    private void updateIv() {
        mutex.lock();
        this.mIV = getNewIv(this.mIV);
        //System.out.println(HexTool.toString(this.mIV));
        mutex.unlock();
    }

    public static byte[] getNewIv(byte oldIv[]) {
        byte[] newIv = {0x52, 0x09, 0x1F, 0x15};
        for (int i = 0; i < 4; i++) {
            Shuffle(oldIv[i], newIv);
        }
        return newIv;
    }

    private static byte[] Shuffle(byte inputValue, byte[] newIV) {
        byte elina = newIV[1];
        byte anna = inputValue;
        byte moritz = sShiftKey[(int) elina & 0xFF];
        moritz -= inputValue;
        newIV[0] += moritz;
        moritz = newIV[2];
        moritz ^= sShiftKey[(int) anna & 0xFF];
        elina -= (int) moritz & 0xFF;
        newIV[1] = elina;
        elina = newIV[3];
        moritz = elina;
        elina -= (int) newIV[0] & 0xFF;
        moritz = sShiftKey[(int) moritz & 0xFF];
        moritz += inputValue;
        moritz ^= newIV[2];
        newIV[2] = moritz;
        elina += (int) sShiftKey[(int) anna & 0xFF] & 0xFF;
        newIV[3] = elina;

        int merry = ((int) newIV[0]) & 0xFF;
        merry |= (newIV[1] << 8) & 0xFF00;
        merry |= (newIV[2] << 16) & 0xFF0000;
        merry |= (newIV[3] << 24) & 0xFF000000;
        int ret_value = merry >>> 0x1d;
        merry <<= 3;
        ret_value |= merry;

        newIV[0] = (byte) (ret_value & 0xFF);
        newIV[1] = (byte) ((ret_value >> 8) & 0xFF);
        newIV[2] = (byte) ((ret_value >> 16) & 0xFF);
        newIV[3] = (byte) ((ret_value >> 24) & 0xFF);

        return newIV;
    }

    public byte[] Encrypt(byte[] pData) throws Exception {
        byte[] datacpy = new byte[pData.length];
        System.arraycopy(pData, 0, datacpy, 0, pData.length);
        byte[] freshIVBlock = new byte[]{
            (byte) mIV[0], (byte) mIV[1], (byte) mIV[2], (byte) mIV[3],
            (byte) mIV[2], (byte) mIV[0], (byte) mIV[3], (byte) mIV[1],
            (byte) mIV[1], (byte) mIV[3], (byte) mIV[0], (byte) mIV[2],
            (byte) mIV[3], (byte) mIV[2], (byte) mIV[1], (byte) mIV[0]
        };
        byte[] RealKey = new byte[datacpy.length];
        for (int a = 0; a < datacpy.length; a++) {
            if (a % 2 == 0) {
                RealKey[a] = (byte) (freshIVBlock[a % freshIVBlock.length] ^ key[a % key.length]);
            } else {
                RealKey[a] = (byte) (freshIVBlock[a % freshIVBlock.length] ^ key2[a % key2.length]);
            }
        }
        for (int a = 0; a < RealKey.length; a++) {
            datacpy[a] ^= (byte) RealKey[a];
        }
        return datacpy;
    }

    public byte[] Decrypt(byte[] pData) throws Exception {
        byte[] freshIVBlock = new byte[]{
            (byte) mIV[0], (byte) mIV[1], (byte) mIV[2], (byte) mIV[3],
            (byte) mIV[2], (byte) mIV[0], (byte) mIV[3], (byte) mIV[1],
            (byte) mIV[1], (byte) mIV[3], (byte) mIV[0], (byte) mIV[2],
            (byte) mIV[3], (byte) mIV[2], (byte) mIV[1], (byte) mIV[0]
        };
        byte[] RealKey = new byte[pData.length];
        for (int a = 0; a < pData.length; a++) {
            if (a % 2 == 0) {
                RealKey[a] = (byte) (freshIVBlock[a % freshIVBlock.length] ^ key[a % key.length]);
            } else {

                RealKey[a] = (byte) (freshIVBlock[a % freshIVBlock.length] ^ key2[a % key2.length]);
            }
        }
        for (int a = 0; a < RealKey.length; a++) {
            pData[a] ^= (byte) RealKey[a];
        }
        //updateIv();
        return pData;
    }

}
