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
package com.ibm.usecases.database.commands;

import app.bootstrap.core.cqrs.ICommand;
import app.bootstrap.core.cqrs.ICommandBus;
import app.bootstrap.core.cqrs.ICommandHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.infrastructure.database.readmodels.CBOMReadModel;
import com.ibm.infrastructure.database.readmodels.CBOMReadRepository;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Property;
import org.cyclonedx.parsers.BomParserFactory;
import org.cyclonedx.parsers.Parser;

@Singleton
public class StoreCBOMCommandHandler implements ICommandHandler {

    private final CBOMReadRepository readRepository;
    private final ICommandBus commandBus;

    @Inject
    public StoreCBOMCommandHandler(CBOMReadRepository readRepository, ICommandBus commandBus) {
        this.readRepository = readRepository;
        this.commandBus = commandBus;
    }

    void onStart(@Observes StartupEvent event) {
        commandBus.register(this, StoreCBOMCommand.class);
    }

    @Override
    public void handle(@Nonnull ICommand command) throws Exception {
        if (command
                instanceof
                StoreCBOMCommand(@Nonnull String projectIdentifier, @Nonnull String cbomJson)) {
            // validate that data is valid CBOM
            byte[] cbomBytes = cbomJson.getBytes(StandardCharsets.UTF_8);
            final Parser parser = BomParserFactory.createParser(cbomBytes);
            final Bom bom = parser.parse(cbomBytes);

            // delete existing CBOM
            this.readRepository
                    .findBy(projectIdentifier)
                    .ifPresent(existing -> readRepository.delete(existing.getId()));

            @Nonnull UUID cbomUUID = UUID.randomUUID();
            @Nonnull String repository = "manual-upload-" + cbomUUID;

            @Nullable String revision = null;
            @Nullable String packageFolder = null;
            @Nullable String commit = null;

            // extract amd override default properties with CBOM metadata
            if (bom.getMetadata() != null && bom.getMetadata().getProperties() != null) {
                for (Property property : bom.getMetadata().getProperties()) {
                    switch (property.getName()) {
                        case "gitUrl" -> repository = property.getValue();
                        case "revision" -> revision = property.getValue();
                        case "commit" -> commit = property.getValue();
                        case "subfolder" -> packageFolder = property.getValue();
                        default -> {}
                    }
                }
            }
            final CBOMReadModel model =
                    new CBOMReadModel(
                            cbomUUID,
                            projectIdentifier,
                            repository,
                            revision,
                            packageFolder,
                            commit,
                            Timestamp.from(Instant.now()),
                            (new ObjectMapper()).readTree(cbomJson));
            readRepository.save(model);
        }
    }
}
