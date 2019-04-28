/*
 * Copyright 2015-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glowroot.agent.it.harness.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.glowroot.agent.it.harness.model.Trace;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

class TraceCollector {

    private final List<Trace> traces = Lists.newCopyOnWriteArrayList();

    private final ServerSocket serverSocket;

    TraceCollector(int port) throws Exception {
        serverSocket = new ServerSocket(port);
        Socket socket = serverSocket.accept();
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        while (true) {
            traces.add((Trace) in.readObject());
            out.writeObject("ok");
        }
    }

    void close() throws IOException {
        serverSocket.close();
    }

    Trace getCompletedTrace(@Nullable String transactionType, @Nullable String transactionName,
            int timeout, TimeUnit unit) throws InterruptedException {
        if (transactionName != null) {
            checkNotNull(transactionType);
        }
        Stopwatch stopwatch = Stopwatch.createStarted();
        while (stopwatch.elapsed(unit) < timeout) {
            for (Trace trace : traces) {
                if ((transactionType == null || trace.transactionType().equals(transactionType))
                        && (transactionName == null
                                || trace.transactionName().equals(transactionName))) {
                    return trace;
                }
            }
            MILLISECONDS.sleep(10);
        }
        if (transactionName != null) {
            throw new IllegalStateException("No trace was collected for transaction type \""
                    + transactionType + "\" and transaction name \"" + transactionName + "\"");
        } else if (transactionType != null) {
            throw new IllegalStateException(
                    "No trace was collected for transaction type: " + transactionType);
        } else {
            throw new IllegalStateException("No trace was collected");
        }
    }

    boolean hasTrace() {
        return !traces.isEmpty();
    }

    void clearTrace() {
        traces.clear();
    }
}
