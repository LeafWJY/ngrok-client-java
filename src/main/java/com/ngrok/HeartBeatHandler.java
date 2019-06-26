package com.ngrok;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import static com.ngrok.util.GenericUtil.getByteBuf;

public class HeartBeatHandler extends ChannelInboundHandlerAdapter {
    private static final String HEARTBEAT_SEQUENCE = "{\"Type\": \"Ping\", \"Payload\": {}}";

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object obj) throws Exception {
        if (obj instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) obj;
            if (IdleState.WRITER_IDLE.equals(event.state())) {
                ctx.channel().writeAndFlush(getByteBuf(HEARTBEAT_SEQUENCE));
            }
        }
    }
}
