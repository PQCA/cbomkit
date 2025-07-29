/*
 * CBOMkit
 * Copyright (C) 2024 PQCA
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.infrastructure.progress;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.annotation.Nonnull;
import jakarta.websocket.Session;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.pqca.errors.ClientDisconnected;
import org.pqca.progress.IProgressDispatcher;
import org.pqca.progress.ProgressMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WebSocketProgressDispatcher implements IProgressDispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketProgressDispatcher.class);

    @Nonnull private final Session session;

    public WebSocketProgressDispatcher(@Nonnull Session session) {
        this.session = session;
    }

    @Override
    public void send(@Nonnull ProgressMessage progressMessage) throws ClientDisconnected {
        try {
            // send
            final CountDownLatch errorLatch = new CountDownLatch(1);
            final AtomicBoolean errOccurred = new AtomicBoolean(false);
            session.getAsyncRemote()
                    .sendObject(
                            progressMessage.asJSONString(),
                            sendResult -> {
                                if (sendResult.getException() != null) {
                                    errOccurred.set(true);
                                    errorLatch.countDown();
                                    try {
                                        session.close();
                                    } catch (IOException e) {
                                        LOGGER.error(e.getLocalizedMessage());
                                    }
                                } else {
                                    errorLatch.countDown();
                                }
                            });
            errorLatch.await();
            if (errOccurred.get()) {
                throw new ClientDisconnected("Client disconnected " + session.getId());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ClientDisconnected(e.getLocalizedMessage());
        } catch (JsonProcessingException e) {
            LOGGER.error(e.getLocalizedMessage());
        }
    }
}
