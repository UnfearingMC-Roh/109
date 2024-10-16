package handling.login.handler;

import client.MapleClient;
import tools.data.LittleEndianAccessor;

public class LoginSuccessHandler {
   public static final void CheckLogin(final LittleEndianAccessor slea, MapleClient c) {
     byte contype = slea.readByte();
        if (contype == 0x01) {
           // c.setConnector(true);
        } 
   }
}
