package com.corundumstudio.socketio.protocol;

import io.netty.buffer.ByteBuf;

public class PacketFraming {
	private Packets packets;
	
	public PacketFraming(Packets packets) {
		this.packets = packets;
	}
	public void setPackets(Packets packets) {
    	this.packets = packets;
    }
	
    public ByteBuf frame_making(ByteBuf buffer) {
    	int len = packets.setting_len(buffer);
    	ByteBuf frame = packets.setting_frame(buffer, len);
    	buffer.readerIndex(buffer.readerIndex() + 1 + len);
    	return frame;
    }
}
