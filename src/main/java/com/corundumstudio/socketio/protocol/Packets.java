package com.corundumstudio.socketio.protocol;

import io.netty.buffer.ByteBuf;

public interface Packets {

	int setting_len(ByteBuf buffer);
	ByteBuf setting_frame(ByteBuf buffer,int len);
	
}
