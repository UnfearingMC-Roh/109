package connector;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import tools.HexTool;

/**
 * @author SLFCG
 */
public class ConnectorNettyEncoder extends MessageToByteEncoder<byte[]> {

    @Override
    protected void encode(ChannelHandlerContext ctx, byte[] pData, ByteBuf buffer) throws Exception {
        final ConnectorClient client = ctx.channel().attr(ConnectorClient.CLIENTKEY).get();

        if (client != null) {
            final Lock mutex = client.getLock();

            mutex.lock();
            try {
                final SGAES send_crpyto = client.getRecvCrypto();

                int i = pData.length;
                byte[] a = {(byte) (i & 0xFF), (byte) ((i >>> 8) & 0xFF), (byte) ((i >>> 16) & 0xFF), (byte) ((i >>> 24) & 0xFF)};
                buffer.writeBytes(a);
                buffer.writeBytes(send_crpyto.Encrypt(pData));
            } finally {
                mutex.unlock();
            }
        } else {
            buffer.writeByte((byte) 0xFF);
            buffer.writeBytes(pData);
        }
        // ctx.flush();
    }
}
