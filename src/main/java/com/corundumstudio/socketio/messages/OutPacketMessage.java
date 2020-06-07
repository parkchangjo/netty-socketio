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
import com.corundumstudio.socketio.handler.ClientHead;
import com.corundumstudio.socketio.handler.EncoderHandler;
import com.corundumstudio.socketio.protocol.Packet;
import com.corundumstudio.socketio.protocol.PacketEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class OutPacketMessage extends HttpMessage {
    public static final AttributeKey<Boolean> WRITE_ONCE = AttributeKey.valueOf("writeOnce");
    public static final AttributeKey<Integer> JSONP_INDEX = AttributeKey.valueOf("jsonpIndex");
    public static final AttributeKey<Boolean> B64 = AttributeKey.valueOf("b64");

    private final ClientHead clientHead;
    private final Transport transport;

    public OutPacketMessage(ClientHead clientHead, Transport transport) {
        super(clientHead.getOrigin(), clientHead.getSessionId());

        this.clientHead = clientHead;
        this.transport = transport;
    }

    public Transport getTransport() {
        return transport;
    }

    public ClientHead getClientHead() {
        return clientHead;
    }

    public void messageWrite(Object message, ChannelHandlerContext ctx, ChannelPromise promise, PacketEncoder encoder) throws Exception {
        OutPacketMessage outPacketMessage = (OutPacketMessage) message;
        if (outPacketMessage.getTransport() == Transport.WEBSOCKET) {
            handleWebsocket((OutPacketMessage) message, ctx, promise, encoder);
        }
        if (outPacketMessage.getTransport() == Transport.POLLING) {
            handleHTTP((OutPacketMessage) message, ctx, promise, encoder);
        }
    }

    private void handleWebsocket(final OutPacketMessage msg, ChannelHandlerContext ctx, ChannelPromise promise, PacketEncoder encoder) throws IOException {
        ChannelFutureList writeFutureList = new ChannelFutureList();

        while (true) {
            Queue<Packet> queue = msg.getClientHead().getPacketsQueue(msg.getTransport());
            Packet packet = queue.poll();
            if (packet == null) {
                writeFutureList.setChannelPromise(promise);
                break;
            }

            final ByteBuf out = encoder.allocateBuffer(ctx.alloc());
            encoder.encodePacket(packet, out, ctx.alloc(), true);

            WebSocketFrame res = new TextWebSocketFrame(out);
            if (log.isTraceEnabled()) {
                log.trace("Out message: {} sessionId: {}", out.toString(CharsetUtil.UTF_8), msg.getSessionId());
            }

            if (out.isReadable()) {
                writeFutureList.add(ctx.channel().writeAndFlush(res));
            } else {
                out.release();
            }

            for (ByteBuf buf : packet.getAttachments()) {
                ByteBuf outBuf = encoder.allocateBuffer(ctx.alloc());
                outBuf.writeByte(4);
                outBuf.writeBytes(buf);
                if (log.isTraceEnabled()) {
                    log.trace("Out attachment: {} sessionId: {}", ByteBufUtil.hexDump(outBuf), msg.getSessionId());
                }
                writeFutureList.add(ctx.channel().writeAndFlush(new BinaryWebSocketFrame(outBuf)));
            }
        }
    }

    private void handleHTTP(OutPacketMessage msg, ChannelHandlerContext ctx, ChannelPromise promise, PacketEncoder encoder) throws IOException {
        Channel channel = ctx.channel();
        Attribute<Boolean> attr = channel.attr(WRITE_ONCE);

        Queue<Packet> queue = msg.getClientHead().getPacketsQueue(msg.getTransport());

        if (!channel.isActive() || queue.isEmpty() || !attr.compareAndSet(null, true)) {
            promise.trySuccess();
            return;
        }

        ByteBuf out = encoder.allocateBuffer(ctx.alloc());
        Boolean b64 = ctx.channel().attr(B64).get();
        if (b64 != null && b64) {
            Integer jsonpIndex = ctx.channel().attr(JSONP_INDEX).get();
            encoder.encodeJsonP(jsonpIndex, queue, out, ctx.alloc(), 50);
            String type = "application/javascript";
            if (jsonpIndex == null) {
                type = "text/plain";
            }
            sendMessage(msg, channel, out, type, promise, HttpResponseStatus.OK);
        } else {
            encoder.encodePackets(queue, out, ctx.alloc(), 50);
            sendMessage(msg, channel, out, "application/octet-stream", promise, HttpResponseStatus.OK);
        }
    }

    /**
     * Helper class for the handleWebsocket method, handles a list of ChannelFutures and
     * sets the status of a promise when
     * - any of the operations fail
     * - all of the operations succeed
     * The setChannelPromise method should be called after all the futures are added
     */

    private class ChannelFutureList implements GenericFutureListener<Future<Void>> {

        private List<ChannelFuture> futureList = new ArrayList<ChannelFuture>();
        private ChannelPromise promise = null;

        private void cleanup() {
            promise = null;
            for (ChannelFuture f : futureList) f.removeListener(this);
        }

        private void validate() {
            boolean allSuccess = true;
            for (ChannelFuture f : futureList) {
                if (f.isDone()) {
                    if (!f.isSuccess()) {
                        promise.tryFailure(f.cause());
                        cleanup();
                        return;
                    }
                }
                else {
                    allSuccess = false;
                }
            }
            if (allSuccess) {
                promise.trySuccess();
                cleanup();
            }
        }

        public void add(ChannelFuture f) {
            futureList.add(f);
            f.addListener(this);
        }

        public void setChannelPromise(ChannelPromise p) {
            promise = p;
            validate();
        }

        @Override
        public void operationComplete(Future<Void> voidFuture) throws Exception {
            if (promise != null) validate();
        }
    }
}
