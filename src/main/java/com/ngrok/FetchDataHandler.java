package com.ngrok;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class FetchDataHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static InternalLogger logger = InternalLoggerFactory.getInstance(FetchDataHandler.class);
    private Channel channel;

    FetchDataHandler(Channel channel) {
        this.channel=channel;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
        logger.info("FatchDataHandler write message to remote address " +channel.remoteAddress()+":"+ byteBuf.toString(CharsetUtil.UTF_8));
        channel.writeAndFlush(byteBuf.copy());
    }
}
