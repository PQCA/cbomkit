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
package com.ibm.presentation.api.v1.scanning;

import app.bootstrap.core.cqrs.ICommandBus;
import app.bootstrap.core.ddd.IDomainEventBus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.domain.scanning.ScanId;
import com.ibm.domain.scanning.authentication.ICredentials;
import com.ibm.domain.scanning.authentication.PersonalAccessToken;
import com.ibm.domain.scanning.authentication.UsernameAndPasswordCredentials;
import com.ibm.infrastructure.progress.ProgressMessage;
import com.ibm.infrastructure.progress.ProgressMessageType;
import com.ibm.infrastructure.progress.WebSocketProgressDispatcher;
import com.ibm.infrastructure.scanning.IScanConfiguration;
import com.ibm.infrastructure.scanning.repositories.ScanRepository;
import com.ibm.usecases.scanning.commands.CloneGitRepositoryCommand;
import com.ibm.usecases.scanning.commands.IdentifyPackageFolderCommand;
import com.ibm.usecases.scanning.commands.IndexModulesCommand;
import com.ibm.usecases.scanning.commands.RequestScanCommand;
import com.ibm.usecases.scanning.commands.ResolvePurlCommand;
import com.ibm.usecases.scanning.commands.ScanCommand;
import com.ibm.usecases.scanning.processmanager.ScanProcessManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServerEndpoint("/v1/scan/{clientId}")
@ApplicationScoped
public class ScanningResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScanningResource.class);

    @Nonnull private final Map<String, Session> sessions;
    @Nonnull protected final ICommandBus commandBus;
    @Nonnull private final IDomainEventBus domainEventBus;
    @Nonnull private final IScanConfiguration configuration;

    public ScanningResource(
            @Nonnull ICommandBus commandBus,
            @Nonnull IDomainEventBus domainEventBus,
            @Nonnull IScanConfiguration configuration) {
        this.sessions = new ConcurrentHashMap<>();
        this.commandBus = commandBus;
        this.domainEventBus = domainEventBus;
        this.configuration = configuration;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("clientId") String clientId) {
        LOGGER.info("Session open for id {}", clientId);
        sessions.put(clientId, session);
    }

    @OnClose
    public void onClose(Session session, @PathParam("clientId") String clientId) {
        LOGGER.warn("asking to close: {}", clientId);
        sessions.remove(clientId);
    }

    @OnError
    public void onError(
            Session session, @PathParam("clientId") String clientId, Throwable throwable) {
        sessions.remove(clientId);
    }

    @OnMessage
    public void onMessage(
            @Nullable String requestJSONString, @Nullable @PathParam("clientId") String clientId) {
        try {
            LOGGER.info("Received from {}", clientId);
            final Session session = Optional.ofNullable(sessions.get(clientId)).orElseThrow();
            final WebSocketProgressDispatcher webSocketProgressDispatcher =
                    new WebSocketProgressDispatcher(session);
            final ScanRequest scanRequest =
                    new ObjectMapper().readValue(requestJSONString, ScanRequest.class);

            final ScanRepository scanRepository = new ScanRepository(this.domainEventBus);

            final ScanId scanId = new ScanId();
            final ScanProcessManager scanProcessManager =
                    new ScanProcessManager(
                            scanId,
                            this.commandBus,
                            scanRepository,
                            webSocketProgressDispatcher,
                            this.configuration);
            this.commandBus.register(
                    scanProcessManager,
                    List.of(
                            ResolvePurlCommand.class,
                            CloneGitRepositoryCommand.class,
                            IdentifyPackageFolderCommand.class,
                            IndexModulesCommand.class,
                            ScanCommand.class));

            final ICredentials authCredentials = getCredentials(scanRequest);

            webSocketProgressDispatcher.send(
                    new ProgressMessage(ProgressMessageType.LABEL, "Starting..."));
            commandBus.send(
                    new RequestScanCommand(
                            scanId,
                            scanRequest.getScanUrl(),
                            scanRequest.getBranch(),
                            scanRequest.getSubfolder(),
                            authCredentials));

        } catch (Exception e) {
            LOGGER.error("Error processing request", e);
        }
    }

    @Nullable private static ICredentials getCredentials(@Nonnull ScanRequest scanRequest) {
        @Nullable ICredentials authCredentials = null;
        final Credentials credentials = scanRequest.getCredentials();
        if (credentials != null) {
            if (credentials.username() != null && credentials.password() != null) {
                authCredentials =
                        new UsernameAndPasswordCredentials(
                                credentials.username(), credentials.password());
            } else if (credentials.pat() != null) {
                authCredentials = new PersonalAccessToken(credentials.pat());
            }
        }
        return authCredentials;
    }
}
