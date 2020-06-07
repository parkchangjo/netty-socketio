package com.corundumstudio.socketio.protocol;

import io.netty.buffer.ByteBuf;

public class isStringPacket implements Packets{
	
	private long readLong(ByteBuf chars, int length) {
        long result = 0;
        for (int i = chars.readerIndex(); i < chars.readerIndex() + length; i++) {
            int digit = ((int)chars.getByte(i) & 0xF);
            for (int j = 0; j < chars.readerIndex() + length-1-i; j++) {
                digit *= 10;
            }
            result += digit;
        }
        chars.readerIndex(chars.readerIndex() + length);
        return result;
    }
	
	@Override
	public int setting_len(ByteBuf buffer) {
		int maxLength = Math.min(buffer.readableBytes(), 10);
        int headEndIndex = buffer.bytesBefore(maxLength, (byte)-1);
        if (headEndIndex == -1) {
            headEndIndex = buffer.bytesBefore(maxLength, (byte)0x3f);
        }  
		return (int) readLong(buffer, headEndIndex);
	}
	
	@Override
	public ByteBuf setting_frame(ByteBuf buffer,int len) {
		
		return buffer.slice(buffer.readerIndex() + 1, len);
	}

	

}
