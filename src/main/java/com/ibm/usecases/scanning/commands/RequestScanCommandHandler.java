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
package com.ibm.usecases.scanning.commands;

import app.bootstrap.core.cqrs.CommandHandler;
import app.bootstrap.core.cqrs.ICommand;
import app.bootstrap.core.cqrs.ICommandBus;
import app.bootstrap.core.ddd.IRepository;
import com.ibm.domain.scanning.Revision;
import com.ibm.domain.scanning.ScanAggregate;
import com.ibm.domain.scanning.ScanId;
import com.ibm.domain.scanning.ScanRequest;
import com.ibm.domain.scanning.ScanUrl;
import com.ibm.domain.scanning.authentication.ICredentials;
import com.ibm.domain.scanning.errors.InvalidScanUrl;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import java.util.Optional;

@Singleton
public final class RequestScanCommandHandler extends CommandHandler<ScanId, ScanAggregate> {

    void onStart(@Observes StartupEvent event) {
        this.commandBus.register(this, RequestScanCommand.class);
    }

    public RequestScanCommandHandler(
            @Nonnull ICommandBus commandBus,
            @Nonnull IRepository<ScanId, ScanAggregate> repository) {
        super(commandBus, repository);
    }

    @Override
    public void handle(@Nonnull ICommand command) throws InvalidScanUrl {
        if (command
                instanceof
                RequestScanCommand(
                        @Nonnull ScanId scanId,
                        @Nonnull String scanUrl,
                        @Nullable String branch,
                        @Nullable String subfolder,
                        @Nullable ICredentials credentials)) {
            final ScanRequest scanRequest =
                    new ScanRequest(
                            new ScanUrl(scanUrl),
                            Optional.ofNullable(branch)
                                    .map(Revision::new)
                                    .orElse(ScanAggregate.REVISION_MAIN),
                            subfolder);
            // create Aggregate and start scan
            // it will emit a domain event that the scan is requested
            final ScanAggregate scanAggregate =
                    ScanAggregate.requestScan(scanId, scanRequest, credentials);
            this.repository.save(scanAggregate);
        }
    }
}
