/**
 * Copyright (c) 2012-2019 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.corundumstudio.socketio.messages;

import com.corundumstudio.socketio.Transport;
import com.corundumstudio.socketio.protocol.PacketEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.io.IOException;
import java.util.Map;

public class HttpErrorMessage extends HttpMessage {

    private final Map<String, Object> data;

    public HttpErrorMessage(Map<String, Object> data) {
        super(null, null);
        this.data = data;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void messageWrite(Object message, ChannelHandlerContext ctx, ChannelPromise promise, PacketEncoder encoder) throws IOException {
        HttpErrorMessage errorMsg = (HttpErrorMessage) message;
        final ByteBuf encBuf = encoder.allocateBuffer(ctx.alloc());
        ByteBufOutputStream out = new ByteBufOutputStream(encBuf);
        encoder.getJsonSupport().writeValue(out, errorMsg.getData());

        sendMessage(errorMsg, ctx.channel(), encBuf, "application/json", promise, HttpResponseStatus.BAD_REQUEST);
    }
}
