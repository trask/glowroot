/*
 * Copyright 2018 the original author or authors.
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
package org.glowroot.central;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.google.common.base.Strings;
import com.google.common.io.Files;
import com.google.protobuf.AbstractMessageLite;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.glowroot.common.util.Clock;
import org.glowroot.wire.api.model.CollectorServiceGrpc.CollectorServiceImplBase;
import org.glowroot.wire.api.model.CollectorServiceOuterClass.AggregateResponseMessage;
import org.glowroot.wire.api.model.CollectorServiceOuterClass.AggregateStreamMessage;
import org.glowroot.wire.api.model.CollectorServiceOuterClass.EmptyMessage;
import org.glowroot.wire.api.model.CollectorServiceOuterClass.GaugeValueMessage;
import org.glowroot.wire.api.model.CollectorServiceOuterClass.GaugeValueResponseMessage;
import org.glowroot.wire.api.model.CollectorServiceOuterClass.InitMessage;
import org.glowroot.wire.api.model.CollectorServiceOuterClass.InitResponse;
import org.glowroot.wire.api.model.CollectorServiceOuterClass.LogMessage;
import org.glowroot.wire.api.model.CollectorServiceOuterClass.OldAggregateMessage;
import org.glowroot.wire.api.model.CollectorServiceOuterClass.OldTraceMessage;
import org.glowroot.wire.api.model.CollectorServiceOuterClass.TraceStreamMessage;

public class ReplayCollectorService extends CollectorServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(ReplayCollectorService.class);

    private final CollectorServiceImplBase delegate;
    private final String agentId;
    private final File directory;
    private final Clock clock;
    private final AtomicInteger uniqueness = new AtomicInteger();

    public ReplayCollectorService(CollectorServiceImplBase delegate, String agentId, File directory,
            Clock clock) {
        this.delegate = delegate;
        this.agentId = agentId;
        this.directory = directory;
        this.clock = clock;
    }

    @Override
    public StreamObserver<AggregateStreamMessage> collectAggregateStream(
            StreamObserver<AggregateResponseMessage> responseObserver) {
        return new ReplayStreamObserver<AggregateStreamMessage>("aggregate",
                delegate.collectAggregateStream(responseObserver), new AggregateStreamPredicate());
    }

    @Override
    public void collectGaugeValues(GaugeValueMessage request,
            StreamObserver<GaugeValueResponseMessage> responseObserver) {
        if (request.getAgentId().equals(agentId)) {
            collect(request, getUniqueFile("gauge"));
        }
        delegate.collectGaugeValues(request, responseObserver);
    }

    @Override
    public StreamObserver<TraceStreamMessage> collectTraceStream(
            StreamObserver<EmptyMessage> responseObserver) {
        return new ReplayStreamObserver<TraceStreamMessage>("trace",
                delegate.collectTraceStream(responseObserver), new TraceStreamPredicate());
    }

    @Override
    public void collectInit(InitMessage request, StreamObserver<InitResponse> responseObserver) {
        delegate.collectInit(request, responseObserver);
    }

    @Override
    public void collectAggregates(OldAggregateMessage request,
            StreamObserver<AggregateResponseMessage> responseObserver) {
        delegate.collectAggregates(request, responseObserver);
    }

    @Override
    public void collectTrace(OldTraceMessage request,
            StreamObserver<EmptyMessage> responseObserver) {
        delegate.collectTrace(request, responseObserver);
    }

    @Override
    public void log(LogMessage request, StreamObserver<EmptyMessage> responseObserver) {
        delegate.log(request, responseObserver);
    }

    private void collect(AbstractMessageLite<?, ?> request, File file) {
        try {
            Files.write(request.toByteArray(), file);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private File getUniqueFile(String ext) {
        int uniq = uniqueness.getAndUpdate(val -> {
            if (val == 999) {
                return 0;
            } else {
                return val + 1;
            }
        });
        return new File(directory, clock.currentTimeMillis() + "-"
                + Strings.padStart(Integer.toString(uniq), 3, '0') + "." + ext);
    }

    private class AggregateStreamPredicate implements Predicate<AggregateStreamMessage> {
        @Override
        public boolean test(AggregateStreamMessage message) {
            if (message.getMessageCase() == AggregateStreamMessage.MessageCase.STREAM_HEADER) {
                return message.getStreamHeader().getAgentId().equals(agentId);
            } else {
                logger.warn("unexpected first aggregate stream message: {}",
                        message.getMessageCase());
                return false;
            }
        }
    }

    private class TraceStreamPredicate implements Predicate<TraceStreamMessage> {
        @Override
        public boolean test(TraceStreamMessage message) {
            if (message.getMessageCase() == TraceStreamMessage.MessageCase.STREAM_HEADER) {
                return message.getStreamHeader().getAgentId().equals(agentId);
            } else {
                logger.warn("unexpected first trace stream message: {}", message.getMessageCase());
                return false;
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private class ReplayStreamObserver<T extends AbstractMessageLite> implements StreamObserver<T> {

        private final String type;
        private final StreamObserver<T> delegate;
        private final Predicate<T> capture;
        private @Nullable File file;
        private @Nullable FileOutputStream fileOut;
        private boolean first = true;

        private ReplayStreamObserver(String type, StreamObserver<T> delegate,
                Predicate<T> capture) {
            this.type = type;
            this.delegate = delegate;
            this.capture = capture;
        }

        @Override
        public void onNext(T value) {
            delegate.onNext(value);
            if (first) {
                if (capture.test(value)) {
                    file = getUniqueFile(type);
                    try {
                        fileOut = new FileOutputStream(file);
                    } catch (FileNotFoundException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
                first = false;
            }
            if (fileOut != null) {
                try {
                    value.writeDelimitedTo(fileOut);
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            delegate.onError(t);
            if (fileOut != null) {
                try {
                    fileOut.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            if (file != null && !file.delete()) {
                logger.error("could not delete file: {}", file);
            }
        }

        @Override
        public void onCompleted() {
            delegate.onCompleted();
            if (fileOut != null) {
                try {
                    fileOut.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }
}
