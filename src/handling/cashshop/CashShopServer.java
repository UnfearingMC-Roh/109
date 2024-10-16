/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package handling.cashshop;

import constants.ServerConstants;
import handling.ServerType;
import java.net.InetSocketAddress;

import handling.channel.PlayerStorage;
import java.net.ServerSocket;

import server.MTSStorage;
import server.ServerProperties;
import handling.login.LoginServer;
import handling.netty.MapleNettyDecoder;
import handling.netty.MapleNettyEncoder;
import handling.netty.MapleNettyHandler;
import handling.world.CheaterData;
import handling.world.World;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class CashShopServer {

    private static String ip;
    private static InetSocketAddress InetSocketadd;
    public final static int PORT = 8600;
    private static PlayerStorage players, playersMTS;
    private static boolean finishedShutdown = false;
    private static ServerBootstrap bootstrap;
//    private static CashShopServerThread thread;

    public static final void run_startup_configurations() throws Exception {
        ip = ServerConstants.Gateway_IP + ":" + PORT;

        players = new PlayerStorage(-10);
//        thread = new CashShopServerThread();
//        thread._serverSocket = new ServerSocket(PORT);
        //System.out.println("[info] CaShshop Open Done");
       // System.out.println("[info] PORT : " + PORT + " Open");
            EventLoopGroup bossGroup = new NioEventLoopGroup();
            EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {

            bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast("decoder", new MapleNettyDecoder());
                            ch.pipeline().addLast("encoder", new MapleNettyEncoder());
                            ch.pipeline().addLast("handler", new MapleNettyHandler(ServerType.CASHSHOP, -10));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture f = bootstrap.bind(PORT).sync();

          //  Start.out.println("Port " + PORT + " Opened.");

        } catch (InterruptedException e) {
          //  e.printStackTrace(Start.out);
        }   
    }

    public static final String getIP() {
        return ip;
    }

    public static int getPort() {
        return PORT;
    }

    public static final PlayerStorage getPlayerStorage() {
        return players;
    }

    public static final PlayerStorage getPlayerStorageMTS() {
        return playersMTS;
    }

    public static final void shutdown() {
        if (finishedShutdown) {
            return;
        }
        System.out.println("서버정보를 저장했습니다.");
        players.disconnectAll();
        //System.out.println("Shutting down CS...");
        finishedShutdown = true;
    }

    public static boolean isShutdown() {
        return finishedShutdown;
    }
}
