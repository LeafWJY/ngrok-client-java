package com.ngrok;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleStateHandler;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import java.util.concurrent.TimeUnit;

/**
 * HOST: ngrok服务端域名
 * PORT: ngrok服务端控制端口
 * REMORTE_PORT: ngrok服务端代理端口
 * LOCAL_PORT: 本地需要被暴露出来的端口
 */

public class NgrokClient  {

    static final String HOST = "codewjy.top";
    static final int PORT = 4454;
    static final int REMORTE_PORT = 55499;
    static final int LOCAL_PORT = 3306;

    public static void main(String[] args) {
        new NgrokClient().start();
    }

    private void start() {
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        Bootstrap b = new Bootstrap();
        try {
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel ch) throws SSLException {
                            SSLEngine engine = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build().newEngine(ch.alloc());
                            ChannelPipeline p = ch.pipeline();
                            p.addFirst(new SslHandler(engine,false));
                            p.addLast(new IdleStateHandler(5, 20, 0, TimeUnit.SECONDS));
                            p.addLast(new HeartBeatHandler());
                            p.addLast(new ControlHandler());
                        }
                    });
            ChannelFuture f = b.connect(NgrokClient.HOST, NgrokClient.PORT).sync();
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
