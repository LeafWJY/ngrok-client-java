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
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.nio.charset.Charset;

public class ProxyHandler extends ChannelInboundHandlerAdapter {

    private static InternalLogger logger = InternalLoggerFactory.getInstance(ProxyHandler.class);
    private String REG_PROXY;
    private boolean init = false;
    private ChannelFuture f;
    private static NioEventLoopGroup group = new NioEventLoopGroup(1);

    ProxyHandler(String clientId) {
        this.REG_PROXY = "{\"Type\": \"RegProxy\", \"Payload\": {\"ClientId\": \"" + clientId + "\"}}";
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.channel().writeAndFlush(GenericUtil.getByteBuf(REG_PROXY));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws InterruptedException {
        ByteBuf byteBuf = (ByteBuf) msg;
        if (byteBuf.isReadable()) {
            int rb = byteBuf.readableBytes();
            if (rb > 8) {
                if (!init){
                    CharSequence charSequence = byteBuf.readCharSequence(rb, Charset.defaultCharset());
                    JSONObject jsonObject = JSON.parseObject(charSequence.toString());
                    if ("StartProxy".equals(jsonObject.get("Type"))) {
                        logger.info("=====StartProxy=====");
                        Bootstrap b = new Bootstrap();
                        b.group(group)
                                .channel(NioSocketChannel.class)
                                .option(ChannelOption.TCP_NODELAY, true)
                                .handler(new ChannelInitializer<SocketChannel>() {
                                    protected void initChannel(SocketChannel ch) {
                                        ChannelPipeline p = ch.pipeline();
                                        p.addLast(new FetchDataHandler(ctx.channel()));
                                    }
                                });
                        f = b.connect("127.0.0.1", NgrokClient.LOCAL_PORT).sync();
                        logger.info("connect local port："+f.channel().localAddress());
                        f.channel().closeFuture().addListener((ChannelFutureListener) t -> {
                            logger.info("disconnect local port："+f.channel().localAddress());
                            init = false;
                        });
                        init = true;
                    }
                }else {
                    logger.info("ProxyHandler write message to local port "+f.channel().localAddress()+":"+byteBuf.toString((CharsetUtil.UTF_8)));
                    f.channel().writeAndFlush(byteBuf.copy());

                }
            }
        }
    }
}
