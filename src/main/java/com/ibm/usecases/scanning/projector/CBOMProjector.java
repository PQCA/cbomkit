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
package com.ibm.usecases.scanning.projector;

import app.bootstrap.core.cqrs.Projector;
import app.bootstrap.core.ddd.IDomainEvent;
import app.bootstrap.core.ddd.IDomainEventBus;
import app.bootstrap.core.ddd.IRepository;
import com.ibm.domain.scanning.Commit;
import com.ibm.domain.scanning.GitUrl;
import com.ibm.domain.scanning.LanguageScan;
import com.ibm.domain.scanning.ScanAggregate;
import com.ibm.domain.scanning.ScanId;
import com.ibm.domain.scanning.errors.NoValidProjectIdentifierForScan;
import com.ibm.domain.scanning.events.ScanFinishedEvent;
import com.ibm.infrastructure.database.readmodels.CBOMReadModel;
import com.ibm.infrastructure.database.readmodels.ICBOMReadRepository;
import com.ibm.infrastructure.errors.EntityNotFoundById;
import com.ibm.usecases.scanning.errors.NoCBOMForScan;
import com.ibm.usecases.scanning.errors.NoGitUrlSpecifiedForScan;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.pqca.errors.CBOMSerializationFailed;
import org.pqca.scanning.CBOM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class CBOMProjector extends Projector<UUID, CBOMReadModel> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CBOMProjector.class);
    @Nonnull private final IRepository<ScanId, ScanAggregate> sourceRepository;

    void onStart(@Observes StartupEvent event) {
        this.domainEventBus.subscribe(this);
    }

    public CBOMProjector(
            @Nonnull IDomainEventBus domainEventBus,
            @Nonnull ICBOMReadRepository repository,
            @Nonnull IRepository<ScanId, ScanAggregate> sourceRepository) {
        super(domainEventBus, repository);
        this.sourceRepository = sourceRepository;
    }

    @Override
    public void handleEvent(@Nonnull IDomainEvent event) throws Exception {
        if (event instanceof ScanFinishedEvent scanFinishedEvent) {
            this.handleScanFinishedEvent(scanFinishedEvent);
        }
    }

    private void handleScanFinishedEvent(@Nonnull ScanFinishedEvent scanFinishedEvent)
            throws CBOMSerializationFailed,
                    NoCBOMForScan,
                    EntityNotFoundById,
                    NoGitUrlSpecifiedForScan,
                    NoValidProjectIdentifierForScan {
        final ScanId scanId = scanFinishedEvent.getScanId();
        // fetch scan aggregate
        final Optional<ScanAggregate> possibleScanAggregate = this.sourceRepository.read(scanId);
        final ScanAggregate scanAggregate =
                possibleScanAggregate.orElseThrow(() -> new EntityNotFoundById(scanId));
        // delete existing read model
        String projectIdentifier = scanAggregate.getProjectIdentifier();
        if (this.repository instanceof ICBOMReadRepository cbomReadRepository) {
            cbomReadRepository
                    .findBy(projectIdentifier)
                    .ifPresent(crm -> this.repository.delete(crm.getId()));
        }
        // build merged CBOM
        final List<CBOM> cbomList =
                scanAggregate
                        .getLanguageScans()
                        .map(scans -> scans.stream().map(LanguageScan::cbom).toList())
                        .orElseThrow(NoCBOMForScan::new);
        // merge CBOMs for each language
        CBOM mergedCBOM = null;
        for (final CBOM cbom : cbomList) {
            if (mergedCBOM == null) {
                mergedCBOM = cbom;
            } else {
                mergedCBOM.merge(cbom);
            }
        }

        if (mergedCBOM == null) {
            throw new NoCBOMForScan();
        }
        // create read model
        final CBOMReadModel cbomReadModel =
                new CBOMReadModel(
                        scanAggregate.getId().getUuid(),
                        projectIdentifier,
                        scanAggregate
                                .getGitUrl()
                                .map(GitUrl::value)
                                .orElseThrow(() -> new NoGitUrlSpecifiedForScan(scanId)),
                        scanAggregate.getRevision().value(),
                        scanAggregate.getPackageFolder().map(Path::toString).orElse(null),
                        scanAggregate.getCommit().map(Commit::hash).orElse(null),
                        scanFinishedEvent.getTimestamp(),
                        mergedCBOM.toJSON());
        // save read model
        this.repository.save(cbomReadModel);
        LOGGER.info("Stored CBOM for {}", projectIdentifier);
    }
}
