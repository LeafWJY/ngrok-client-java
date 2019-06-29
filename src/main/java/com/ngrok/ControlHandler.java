package com.ngrok;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ngrok.util.GenericUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import java.nio.charset.Charset;

public class ControlHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static InternalLogger logger = InternalLoggerFactory.getInstance(ControlHandler.class);
    private String clientId;
    private static NioEventLoopGroup group = new NioEventLoopGroup(1);
    private static final String AUTH = "{\"Type\": \"Auth\", \"Payload\": {\"ClientId\": \"\", \"OS\": \"darwin\", \"Arch\": \"amd64\", \"Version\": \"2\", \"MmVersion\": \"1.7\", \"User\": \"user\", \"Password\": \"\"}}";
    private static final String REQ_TUNNEL = "{\"Type\": \"ReqTunnel\", \"Payload\": {\"ReqId\": \"jhnl8GF3\", \"Protocol\": \"tcp\", \"Hostname\": \"\", \"Subdomain\": \"www\", \"HttpAuth\": \"\", \"RemotePort\": " + NgrokClient.REMORTE_PORT + "}}";
    private static final String PING = "{\"Type\": \"Ping\", \"Payload\": {}}";

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.channel().writeAndFlush(GenericUtil.getByteBuf(AUTH));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        if (byteBuf.isReadable()) {
            int rb = byteBuf.readableBytes();
            if (rb > 8) {
                CharSequence charSequence = byteBuf.readCharSequence(rb, Charset.defaultCharset());
                JSONObject jsonObject = JSON.parseObject(charSequence.toString());
                if ("AuthResp".equals(jsonObject.get("Type"))) {
                    clientId = jsonObject.getJSONObject("Payload").getString("ClientId");
                    ctx.channel().writeAndFlush(GenericUtil.getByteBuf(PING));
                    ctx.channel().writeAndFlush(GenericUtil.getByteBuf(REQ_TUNNEL));
                }else if ("ReqProxy".equals(jsonObject.get("Type"))) {
                    Bootstrap b = new Bootstrap();
                    try {
                        b.group(group)
                                .channel(NioSocketChannel.class)
                                .option(ChannelOption.TCP_NODELAY, true)
                                .handler(new ChannelInitializer<SocketChannel>() {
                                    protected void initChannel(SocketChannel ch) throws SSLException {
                                        SSLEngine engine = SslContextBuilder.forClient()
                                                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                                .build()
                                                .newEngine(ch.alloc());
                                        ChannelPipeline p = ch.pipeline();
                                        p.addFirst(new SslHandler(engine,false));
                                        p.addLast(new ProxyHandler(clientId));
                                    }
                                });
                        ChannelFuture f = b.connect(NgrokClient.HOST, NgrokClient.PORT).sync();
                        logger.info("connect to remote address "+f.channel().remoteAddress());
                        f.channel().closeFuture().addListener((ChannelFutureListener) channelFuture -> logger.info("disconnect to remote address "+f.channel().remoteAddress()));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }else if ("NewTunnel".equals(jsonObject.get("Type"))) {
                    logger.info(jsonObject.toJSONString());
                }
            }
        }

    }



}
