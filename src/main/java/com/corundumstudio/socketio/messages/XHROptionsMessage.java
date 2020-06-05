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
import io.netty.handler.codec.http.*;

import java.util.UUID;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class XHROptionsMessage extends XHRPostMessage {

    public XHROptionsMessage(String origin, UUID sessionId) {
        super(origin, sessionId);
    }

    public void messageWrite(Object message, ChannelHandlerContext ctx, ChannelPromise promise, PacketEncoder encoder) throws Exception {
        XHROptionsMessage xhrOptionsMessage = (XHROptionsMessage) message;
        HttpResponse res = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.OK);

        res.headers().add(HttpHeaderNames.SET_COOKIE, "io=" + xhrOptionsMessage.getSessionId())
                .add(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
                .add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, HttpHeaderNames.CONTENT_TYPE);

        String origin = ctx.channel().attr(ORIGIN).get();
        addOriginHeaders(origin, res);

        ByteBuf out = encoder.allocateBuffer(ctx.alloc());
        sendMessage(xhrOptionsMessage, ctx.channel(), out, res, promise);
    }
}
