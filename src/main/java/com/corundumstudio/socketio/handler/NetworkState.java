package com.corundumstudio.socketio.handler;

import com.corundumstudio.socketio.protocol.Packet;
import io.netty.channel.Channel;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class NetworkState {
    protected Queue<Packet> packetsQueue = new ConcurrentLinkedQueue<Packet>();
    protected Channel channel;

    public void setPacketsQueue(Queue<Packet> packetsQueue) {
        this.packetsQueue = packetsQueue;
    }

    public Queue<Packet> getPacketsQueue() {
        return packetsQueue;
    }

    public Channel getChannel() {
        return channel;
    }

    public abstract Channel update(Channel channel);
}
