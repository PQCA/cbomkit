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
package com.ibm.infrastructure.scanning.repositories;

import app.bootstrap.core.ddd.IDomainEventBus;
import app.bootstrap.core.ddd.Repository;
import com.ibm.domain.scanning.ScanAggregate;
import com.ibm.domain.scanning.ScanId;
import com.ibm.infrastructure.errors.EntityNotFoundById;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Status;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class ScanRepository extends Repository<ScanId, ScanAggregate>
        implements PanacheRepository<Scan> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScanRepository.class);

    public ScanRepository(@Nonnull IDomainEventBus domainEventBus) {
        super(domainEventBus);
    }

    @Nonnull
    @Override
    public Optional<ScanAggregate> read(@Nonnull ScanId id) {
        final EntityManager entityManager = Scan.getEntityManager();
        final ArcContainer container = Arc.container();
        container.requestContext().activate();
        try {
            QuarkusTransaction.begin();
            final Optional<Scan> scan =
                    Optional.ofNullable(entityManager.find(Scan.class, id.getUuid()));
            QuarkusTransaction.commit();
            if (scan.isEmpty()) {
                throw new EntityNotFoundById(id);
            }
            final ScanAggregate scanAggregate = scan.get().asAggregate();
            return Optional.of(scanAggregate);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            if (QuarkusTransaction.getStatus() != Status.STATUS_NO_TRANSACTION) {
                QuarkusTransaction.rollback();
            }
        } finally {
            container.requestContext().terminate();
        }
        return Optional.empty();
    }

    @Override
    public void save(@Nonnull ScanAggregate entity) {
        final EntityManager entityManager = Scan.getEntityManager();
        final ArcContainer container = Arc.container();
        container.requestContext().activate();
        try {
            QuarkusTransaction.begin();
            final Optional<Scan> existing =
                    Optional.ofNullable(entityManager.find(Scan.class, entity.getId().getUuid()));
            final Scan scan = new Scan(entity);
            if (existing.isEmpty()) {
                entityManager.persist(scan);
            } else {
                entityManager.merge(scan);
            }
            QuarkusTransaction.commit();
            // emit domain events
            if (entity.hasUncommitedChanges()) {
                // commit uncommited events from the aggregate here
                entity.commit(domainEvents -> domainEvents.forEach(domainEventBus::publish));
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            if (QuarkusTransaction.getStatus() != Status.STATUS_NO_TRANSACTION) {
                QuarkusTransaction.rollback();
            }
        } finally {
            container.requestContext().terminate();
        }
    }

    @Override
    public void delete(@Nonnull ScanId id) {
        final EntityManager entityManager = Scan.getEntityManager();
        final ArcContainer container = Arc.container();
        container.requestContext().activate();
        try {
            QuarkusTransaction.begin();
            final Optional<Scan> scan =
                    Optional.ofNullable(entityManager.find(Scan.class, id.getUuid()));
            scan.ifPresent(entityManager::remove);
            QuarkusTransaction.commit();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            if (QuarkusTransaction.getStatus() != Status.STATUS_NO_TRANSACTION) {
                QuarkusTransaction.rollback();
            }
        } finally {
            container.requestContext().terminate();
        }
    }
}
