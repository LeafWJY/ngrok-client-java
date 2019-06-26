package com.ngrok.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class GenericUtil {

    public static ByteBuf getByteBuf(String pk) {
        ByteBuf message = Unpooled.buffer();
        return message.writeByte(pk.length()).writeInt(0).writeByte(0).writeByte(0).writeByte(0).writeBytes(pk.getBytes());
    }
}
