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
package com.ibm.infrastructure.database.readmodels;

import app.bootstrap.core.ddd.IDomainEventBus;
import app.bootstrap.core.ddd.ReadRepository;
import com.github.packageurl.PackageURL;
import com.ibm.domain.scanning.Commit;
import com.ibm.domain.scanning.GitUrl;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class CBOMReadRepository extends ReadRepository<UUID, CBOMReadModel>
        implements ICBOMReadRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(CBOMReadRepository.class);

    public CBOMReadRepository(@Nonnull IDomainEventBus domainEventBus) {
        super(domainEventBus);
    }

    @Override
    public @Nonnull Optional<CBOMReadModel> findBy(
            @Nonnull GitUrl gitUrl, @Nullable Commit commit, @Nullable Path packageFolder) {
        return findByRepository(gitUrl.value(), commit, packageFolder);
    }

    @Override
    public @Nonnull Optional<CBOMReadModel> findBy(
            @Nonnull PackageURL purl, @Nullable Commit commit) {
        return findByProjectIdentifier(purl.canonicalize(), commit);
    }

    @Override
    public @Nonnull Optional<CBOMReadModel> findBy(@Nonnull String projectIdentifier) {
        return findByProjectIdentifier(projectIdentifier, null);
    }

    @Override
    public @Nonnull Collection<CBOMReadModel> getRecent(int limit) {
        final EntityManager entityManager = CBOMReadModel.getEntityManager();
        final ArcContainer container = Arc.container();
        container.requestContext().activate();
        try {
            QuarkusTransaction.begin();
            final List<CBOMReadModel> match =
                    entityManager
                            .createQuery(
                                    "SELECT DISTINCT read FROM CBOMReadModel read"
                                            + " WHERE read.createdAt = ( SELECT MAX(r.createdAt) FROM CBOMReadModel r WHERE r.repository = read.repository )"
                                            + " ORDER BY read.createdAt DESC"
                                            + " LIMIT :limit",
                                    CBOMReadModel.class)
                            .setParameter("limit", limit)
                            .getResultList();
            QuarkusTransaction.commit();
            return match;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            if (QuarkusTransaction.getStatus()
                    != 6) { // https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.22#quarkustransactionisactive-deprecated
                QuarkusTransaction.rollback();
            }
        } finally {
            container.requestContext().terminate();
        }
        return List.of();
    }

    @Override
    public @Nonnull Optional<CBOMReadModel> read(@Nonnull UUID uuid) {
        final EntityManager entityManager = CBOMReadModel.getEntityManager();
        final ArcContainer container = Arc.container();
        container.requestContext().activate();
        try {
            QuarkusTransaction.begin();
            final Optional<CBOMReadModel> cbomReadModel =
                    Optional.ofNullable(entityManager.find(CBOMReadModel.class, uuid));
            QuarkusTransaction.commit();
            return cbomReadModel;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            if (QuarkusTransaction.getStatus()
                    != 6) { // https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.22#quarkustransactionisactive-deprecated
                QuarkusTransaction.rollback();
            }
        } finally {
            container.requestContext().terminate();
        }
        return Optional.empty();
    }

    @Override
    public void save(@Nonnull CBOMReadModel cbomReadModel) {
        final EntityManager entityManager = CBOMReadModel.getEntityManager();
        final ArcContainer container = Arc.container();
        container.requestContext().activate();
        try {
            QuarkusTransaction.begin();
            final CBOMReadModel existing =
                    entityManager.find(CBOMReadModel.class, cbomReadModel.getId());
            if (existing == null) {
                entityManager.persist(cbomReadModel);
            } else {
                entityManager.merge(cbomReadModel);
            }
            QuarkusTransaction.commit();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            if (QuarkusTransaction.getStatus()
                    != 6) { // https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.22#quarkustransactionisactive-deprecated
                QuarkusTransaction.rollback();
            }
        } finally {
            container.requestContext().terminate();
        }
    }

    @Override
    public void delete(@Nonnull UUID uuid) {
        final EntityManager entityManager = CBOMReadModel.getEntityManager();
        final ArcContainer container = Arc.container();
        container.requestContext().activate();
        try {
            QuarkusTransaction.begin();
            final Optional<CBOMReadModel> cbomReadModel =
                    Optional.ofNullable(entityManager.find(CBOMReadModel.class, uuid));
            cbomReadModel.ifPresent(entityManager::remove);
            QuarkusTransaction.commit();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            if (QuarkusTransaction.getStatus()
                    != 6) { // https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.22#quarkustransactionisactive-deprecated
                QuarkusTransaction.rollback();
            }
        } finally {
            container.requestContext().terminate();
        }
    }

    private @Nonnull Optional<CBOMReadModel> findByRepository(
            @Nonnull String repository, @Nullable Commit commit, @Nullable Path packageFolder) {
        final EntityManager entityManager = CBOMReadModel.getEntityManager();
        final ArcContainer container = Arc.container();
        container.requestContext().activate();
        try {
            QuarkusTransaction.begin();

            final StringBuilder queryBuilder = new StringBuilder();
            final String baseQuery =
                    "SELECT read FROM CBOMReadModel read WHERE read.repository = :repository";
            queryBuilder.append(baseQuery);

            if (commit != null) {
                queryBuilder.append(" AND read.commit = :commit");
            }
            if (packageFolder != null) {
                queryBuilder.append(" AND read.packageFolder = :packageFolder");
            }
            // set order
            queryBuilder.append(" ORDER BY createdAt desc");

            final TypedQuery<CBOMReadModel> query =
                    entityManager
                            .createQuery(queryBuilder.toString(), CBOMReadModel.class)
                            .setParameter("repository", repository);

            if (commit != null) {
                query.setParameter("commit", commit.hash());
            }
            if (packageFolder != null) {
                query.setParameter("packageFolder", packageFolder.toString());
            }
            final Optional<CBOMReadModel> match = query.getResultStream().findFirst();
            QuarkusTransaction.commit();
            return match;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            if (QuarkusTransaction.getStatus()
                    != 6) { // https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.22#quarkustransactionisactive-deprecated
                QuarkusTransaction.rollback();
            }
        } finally {
            container.requestContext().terminate();
        }
        return Optional.empty();
    }

    private @Nonnull Optional<CBOMReadModel> findByProjectIdentifier(
            @Nonnull String projectIdentifier, @Nullable Commit commit) {
        final EntityManager entityManager = CBOMReadModel.getEntityManager();
        final ArcContainer container = Arc.container();
        container.requestContext().activate();
        try {
            QuarkusTransaction.begin();
            String qString =
                    commit != null
                            ? "SELECT read FROM CBOMReadModel read WHERE read.commit = :commit AND read.projectIdentifier = :projectIdentifier"
                            : "SELECT read FROM CBOMReadModel read WHERE read.projectIdentifier = :projectIdentifier";
            qString += " ORDER BY createdAt desc";

            TypedQuery<CBOMReadModel> query =
                    entityManager
                            .createQuery(qString, CBOMReadModel.class)
                            .setParameter("projectIdentifier", projectIdentifier);

            if (commit != null) {
                query.setParameter("commit", commit.hash());
            }
            Optional<CBOMReadModel> match = query.getResultStream().findFirst();
            QuarkusTransaction.commit();
            return match;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            if (QuarkusTransaction.getStatus()
                    != 6) { // https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.22#quarkustransactionisactive-deprecated
                QuarkusTransaction.rollback();
            }
        } finally {
            container.requestContext().terminate();
        }
        return Optional.empty();
    }
}
