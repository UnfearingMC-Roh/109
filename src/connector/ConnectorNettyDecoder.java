package connector;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;
import tools.data.ByteArrayByteStream;
import tools.data.LittleEndianAccessor;

/**
 * @author SLFCG
 */
public class ConnectorNettyDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> list) throws Exception {
        final ConnectorClient client = ctx.channel().attr(ConnectorClient.CLIENTKEY).get();
        if (client == null) {
            return;
        }
        if (buffer.readableBytes() < 4) {
            return;
        }
        while (true) {
            int packetlength = buffer.readIntLE();
            if (buffer.readableBytes() < packetlength) {
                buffer.resetReaderIndex();
                return;
            }
            try {
                buffer.markReaderIndex();
                byte[] decoded = new byte[packetlength];
                buffer.readBytes(decoded);
                buffer.markReaderIndex();
                list.add(new LittleEndianAccessor(new ByteArrayByteStream(client.getSendCrypto().Decrypt(decoded))));
                if (buffer.readableBytes() < 4) {
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
