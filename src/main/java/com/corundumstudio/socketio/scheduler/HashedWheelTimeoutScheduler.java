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
/**
 * Modified version of HashedWheelScheduler specially for timeouts handling.
 * Difference:
 * - handling old timeout with same key after adding new one
 *   fixes multithreaded problem that appears in highly concurrent non-atomic sequence cancel() -> schedule()
 *
 * (c) Alim Akbashev, 2015-02-11
 */

package com.corundumstudio.socketio.scheduler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class HashedWheelTimeoutScheduler extends CancelableScheduler {

    private volatile ChannelHandlerContext ctx;

    public HashedWheelTimeoutScheduler() {
       super();
    }

    public HashedWheelTimeoutScheduler(ThreadFactory threadFactory) {
        super(threadFactory);
    }

    @Override
    protected void scheduledFuture(final SchedulerKey key, Timeout timeout) {
        replaceScheduledFuture(key, timeout);
    }

    private void replaceScheduledFuture(final SchedulerKey key, final Timeout newTimeout) {
        final Timeout oldTimeout;

        if (newTimeout.isExpired()) {
            // no need to put already expired timeout to scheduledFutures map.
            // simply remove old timeout
            oldTimeout = scheduledFutures.remove(key);
        } else {
            oldTimeout = scheduledFutures.put(key, newTimeout);
        }

        // if there was old timeout, cancel it
        if (oldTimeout != null) {
            oldTimeout.cancel();
        }
    }

}
