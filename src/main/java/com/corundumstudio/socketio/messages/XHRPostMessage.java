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

import com.corundumstudio.socketio.protocol.PacketEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;

import java.util.UUID;

public class XHRPostMessage extends HttpMessage {
    private static final byte[] OK = "ok".getBytes(CharsetUtil.UTF_8);

    public XHRPostMessage(String origin, UUID sessionId) {
        super(origin, sessionId);
    }

    public void messageWrite(Object message, ChannelHandlerContext ctx, ChannelPromise promise, PacketEncoder encoder) throws Exception {
        XHRPostMessage xhrPostMessage = (XHRPostMessage) message;
        ByteBuf out = encoder.allocateBuffer(ctx.alloc());
        out.writeBytes(OK);
        sendMessage(xhrPostMessage, ctx.channel(), out, "text/html", promise, HttpResponseStatus.OK);
    }
}
