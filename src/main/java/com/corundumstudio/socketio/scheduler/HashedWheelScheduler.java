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
package com.corundumstudio.socketio.scheduler;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;

public class HashedWheelScheduler extends CancelableScheduler {

    private volatile ChannelHandlerContext ctx;

    public HashedWheelScheduler() {
        super();
    }

    public HashedWheelScheduler(ThreadFactory threadFactory) {
        super(threadFactory);
    }

    @Override
    protected void scheduledFuture(final SchedulerKey key, Timeout timeout) {
        addScheduledFuture(key, timeout);
    }

    @Override
    public void shutdown() {
        executorService.stop();
    }

    private void addScheduledFuture(final SchedulerKey key, Timeout timeout){
    if (!timeout.isExpired()) {
        scheduledFutures.put(key, timeout);
    }
}

}
