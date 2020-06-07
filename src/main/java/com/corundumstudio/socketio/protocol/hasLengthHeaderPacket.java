package com.corundumstudio.socketio.protocol;

import io.netty.buffer.ByteBuf;

public class hasLengthHeaderPacket implements Packets{
	
	private final UTF8CharsScanner utf8scanner = new UTF8CharsScanner();
	
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
		int lengthEndIndex = buffer.bytesBefore((byte)':');
        int lenHeader = (int) readLong(buffer, lengthEndIndex);
		return utf8scanner.getActualLength(buffer, lenHeader);
	}

	@Override
	public ByteBuf setting_frame(ByteBuf buffer, int len) {
		return buffer.slice(buffer.readerIndex() + 1, len);
	}

}
