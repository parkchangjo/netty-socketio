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

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.handler.EncoderHandler;
import com.corundumstudio.socketio.protocol.PacketEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public abstract class HttpMessage {
    public static final AttributeKey<String> ORIGIN = AttributeKey.valueOf("origin");
    public static final AttributeKey<String> USER_AGENT = AttributeKey.valueOf("userAgent");

    protected static final Logger log = LoggerFactory.getLogger(EncoderHandler.class);

    private final String origin;
    private final UUID sessionId;

    private String version;
    private Configuration configuration;

    HttpMessage(String origin, UUID sessionId) {
        this.origin = origin;
        this.sessionId = sessionId;
    }

    public void setConfiguration(Configuration _configuration) { configuration = _configuration; }

    public void setVersion(String _version) { version = _version; }

    public String getOrigin() {
        return origin;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    void sendMessage(HttpMessage msg, Channel channel, ByteBuf out, String type, ChannelPromise promise, HttpResponseStatus status) {
        HttpResponse res = new DefaultHttpResponse(HTTP_1_1, status);

        res.headers().add(HttpHeaderNames.CONTENT_TYPE, type)
                .add(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        if (msg.getSessionId() != null) {
            res.headers().add(HttpHeaderNames.SET_COOKIE, "io=" + msg.getSessionId());
        }

        String originMsg = channel.attr(ORIGIN).get();
        addOriginHeaders(originMsg, res);

        HttpUtil.setContentLength(res, out.readableBytes());

        // prevent XSS warnings on IE
        // https://github.com/LearnBoost/socket.io/pull/1333
        String userAgent = channel.attr(USER_AGENT).get();
        if (userAgent != null && (userAgent.contains(";MSIE") || userAgent.contains("Trident/"))) {
            res.headers().add("X-XSS-Protection", "0");
        }

        sendMessage(msg, channel, out, res, promise);
    }

    void sendMessage(HttpMessage msg, Channel channel, ByteBuf out, HttpResponse res, ChannelPromise promise) {
        channel.write(res);

        if (log.isTraceEnabled()) {
            if (msg.getSessionId() != null) {
                log.trace("Out message: {} - sessionId: {}", out.toString(CharsetUtil.UTF_8), msg.getSessionId());
            } else {
                log.trace("Out message: {}", out.toString(CharsetUtil.UTF_8));
            }
        }

        if (out.isReadable()) {
            channel.write(new DefaultHttpContent(out));
        } else {
            out.release();
        }

        channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT, promise).addListener(ChannelFutureListener.CLOSE);
    }

    void addOriginHeaders(String origin, HttpResponse res) {
        if (version != null) {
            res.headers().add(HttpHeaderNames.SERVER, version);
        }

        if (configuration.getOrigin() != null) {
            res.headers().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, configuration.getOrigin());
            res.headers().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.TRUE);
        } else {
            if (origin != null) {
                res.headers().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                res.headers().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.TRUE);
            } else {
                res.headers().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            }
        }
    }

    public abstract void messageWrite(Object msg, ChannelHandlerContext ctx, ChannelPromise promise, PacketEncoder encoder) throws Exception;
}
