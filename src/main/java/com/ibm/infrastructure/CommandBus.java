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
package com.ibm.infrastructure;

import app.bootstrap.core.cqrs.ICommand;
import app.bootstrap.core.cqrs.ICommandBus;
import app.bootstrap.core.cqrs.ICommandHandler;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class CommandBus implements ICommandBus {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandBus.class);

    @SuppressWarnings("all")
    @Nonnull
    private final Map<Class<? extends ICommand>, List<ICommandHandler>> handlers;

    private final ExecutorService executorService;

    public CommandBus() {
        this.handlers = new ConcurrentHashMap<>();
        this.executorService = Executors.newCachedThreadPool();
    }

    @Override
    public void register(
            @Nonnull ICommandHandler commandHandler,
            @Nonnull Class<? extends ICommand> forCommand) {
        this.handlers
                .computeIfAbsent(forCommand, k -> new java.util.ArrayList<>())
                .add(commandHandler);
    }

    @Override
    public void register(
            @Nonnull ICommandHandler commandHandler,
            @Nonnull List<Class<? extends ICommand>> forCommands) {
        for (Class<? extends ICommand> forCommand : forCommands) {
            this.handlers
                    .computeIfAbsent(forCommand, k -> new java.util.ArrayList<>())
                    .add(commandHandler);
        }
    }

    @Override
    public void unregister(
            @Nonnull ICommandHandler commandHandler,
            @Nonnull Class<? extends ICommand> forCommand) {
        List<ICommandHandler> handlersForCommand = this.handlers.get(forCommand);
        if (handlersForCommand != null) {
            handlersForCommand.remove(commandHandler);
            if (handlersForCommand.isEmpty()) {
                this.handlers.remove(forCommand);
            }
        }
    }

    @Override
    public void unregister(
            @Nonnull ICommandHandler commandHandler,
            @Nonnull List<Class<? extends ICommand>> forCommands) {
        for (Class<? extends ICommand> forCommand : forCommands) {
            unregister(commandHandler, forCommand);
        }
    }

    @Nonnull
    @Override
    public CompletableFuture<Boolean> send(@Nonnull ICommand command) throws Exception {
        final CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        this.executorService.submit(() -> completableFuture.complete(sendSync(command)));
        return completableFuture;
    }

    @Nonnull
    private Boolean executeCommand(
            @Nonnull List<ICommandHandler> handlers, @Nonnull ICommand command) {
        boolean allSucceeded = true;
        for (ICommandHandler handler : handlers) {
            try {
                handler.handle(command);
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
                allSucceeded = false;
            }
        }
        return allSucceeded;
    }

    @Override
    public Boolean sendSync(ICommand command) throws Exception {
        LOGGER.info("sending command {}", command);
        final List<ICommandHandler> handlersForCommand = handlers.get(command.getClass());
        if (handlersForCommand == null || handlersForCommand.isEmpty()) {
            LOGGER.error("No handler for command {}", command);
            return false;
        }

        return executeCommand(handlersForCommand, command);
    }
}
