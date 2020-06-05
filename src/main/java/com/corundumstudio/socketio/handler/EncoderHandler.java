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
package com.corundumstudio.socketio.handler;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.messages.*;
import com.corundumstudio.socketio.protocol.PacketEncoder;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

@Sharable
public class EncoderHandler extends ChannelOutboundHandlerAdapter {
    private final PacketEncoder encoder;

    private String version;
    private Configuration configuration;

    public EncoderHandler(Configuration configuration, PacketEncoder encoder) throws IOException {
        this.encoder = encoder;
        this.configuration = configuration;

        if (configuration.isAddVersionHeader()) {
            readVersion();
        }
    }

    private void readVersion() throws IOException {
        Enumeration<URL> resources = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
        while (resources.hasMoreElements()) {
            try {
                Manifest manifest = new Manifest(resources.nextElement().openStream());
                Attributes attrs = manifest.getMainAttributes();
                if (attrs == null) {
                    continue;
                }
                String name = attrs.getValue("Bundle-Name");
                if (name != null && name.equals("netty-socketio")) {
                    version = name + "/" + attrs.getValue("Bundle-Version");
                    break;
                }
            } catch (IOException E) {
                // skip it
            }
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!(msg instanceof HttpMessage)) {
            super.write(ctx, msg, promise);
            return;
        }

        try {
            HttpMessage httpMessage = getHttpMessage(msg);

            httpMessage.setConfiguration(configuration);
            httpMessage.setVersion(version);
            httpMessage.messageWrite(msg, ctx, promise, encoder);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static HttpMessage getHttpMessage(Object msg) throws Exception {
        if( msg instanceof OutPacketMessage) {
            return (OutPacketMessage) msg;
        } else if( msg instanceof HttpErrorMessage) {
            return (HttpErrorMessage) msg;
        } else if( msg instanceof XHROptionsMessage) {
            return (XHROptionsMessage) msg;
        } else if( msg instanceof XHRPostMessage) {
            return (XHRPostMessage) msg;
        } else {
            throw new Exception();
        }
    }
}
