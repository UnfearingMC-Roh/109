/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package util;

import java.nio.charset.Charset;
import java.util.Arrays;
import tools.HexTool;
import tools.data.MaplePacketLittleEndianWriter;

/**
 *
 * @author jch50
 */
public class TEST {
    public static void main(String[] args) {
        
//        String test = new String(arr, Charset.forName("EUC-KR"));
        int i = 0;
        String test = " 63 68 65 61 74 65 6E 67 69 6E 65";
//        test = test.replaceAll(" ", ", 0x").replaceFirst(", ", "");
//        byte[] arr = new byte[test.split(", ").length];
//        for (String hexString : test.split(", ")) {
//            byte hex = Byte.parseByte(hexString);
//            arr[i++] = hex;
//        }
        byte[] arr = HexTool.getByteArrayFromHexString(test);
        System.err.println(new String(arr, Charset.forName("EUC-KR")));
        
        int a = -1;
        a -= -2;
        System.err.println(a);
    }
}
