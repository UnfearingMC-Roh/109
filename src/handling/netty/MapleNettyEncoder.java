/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.netty;

import client.MapleClient;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import java.util.concurrent.locks.Lock;
import tools.MapleKMSEncryption;

/**
 *
 * @author csproj
 */
public class MapleNettyEncoder extends MessageToByteEncoder<byte[]> {

    @Override
    protected void encode(ChannelHandlerContext ctx, byte[] msg, ByteBuf buffer) throws Exception {

        final MapleClient client = ctx.channel().attr(MapleClient.CLIENTKEY).get();

        if (client != null) {
            final Lock mutex = client.getLock();

            mutex.lock();
            try {
                MapleKMSEncryption send_crypto = client.getSendCrypto();

                buffer.writeBytes(send_crypto.getPacketHeader(msg.length));
                buffer.writeBytes(send_crypto.encrypt(msg));
              //  buffer.writeBytes(send_crypto.crypt(msg));
            } finally {
                mutex.unlock();
            }
        } else { // no client object created yet, send unencrypted (hello)
            buffer.writeBytes(msg);
        }
    }
}
