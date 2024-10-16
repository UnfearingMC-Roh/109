package connector;

import handling.channel.PlayerStorage;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * @author SLFCG
 */
public class ConnectorServer {

    private static ConnectorServer instance = new ConnectorServer();
    private ServerBootstrap bootstrap;
    private ConnectorClientStorage clients;

    public static ConnectorServer getInstance() {
        return instance;
    }

    public final ConnectorClientStorage getClientStorage() {
        if (clients == null) { //wth
            clients = new ConnectorClientStorage(); //wthhhh
        }
        return clients;
    }

    public final void addPlayer(final ConnectorClient c) {
        getClientStorage().registerClient(c, c.getId());
    }

    public final void removePlayer(final ConnectorClient c) {
        getClientStorage().deregisterClient(c);

    }

    public void run_startup_configurations() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast("decodeer", new ConnectorNettyDecoder());
                    ch.pipeline().addLast("encodeer", new ConnectorNettyEncoder());
                    ch.pipeline().addLast("idleStateHandler", new IdleStateHandler(30, 5, 0));
                    ch.pipeline().addLast("handler", new ConnectorNettyHandler());
                }
            }).option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture cf = bootstrap.bind(7000).sync();//커넥터포트 
            ConnectorThread ct = new ConnectorThread();
            ct.start();
            clients = new ConnectorClientStorage();
            System.out.println("접속기 서버 개방 성공");
        } catch (InterruptedException ex) {
            System.err.println("접속기 서버 개방 실패\r\n" + ex);
        }
    }
}
