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

import app.bootstrap.core.cqrs.IReadModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.Date;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Cacheable
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "projectIdentifier",
    "gitUrl",
    "branch",
    "packageFolder",
    "commit",
    "createdAt",
    "bom"
})
public class CBOMReadModel extends PanacheEntityBase implements IReadModel<UUID> {
    @JsonIgnore @Id @Nonnull public UUID id;

    @Nonnull protected String projectIdentifier;

    @JsonProperty("gitUrl")
    @Nonnull
    protected String repository;

    @JsonProperty("branch")
    @Nullable protected String revision;

    @JsonProperty("packageFolder")
    @Nullable protected String packageFolder;

    @Nullable protected String commit;
    @Nonnull protected Date createdAt;

    @Nonnull
    @JdbcTypeCode(SqlTypes.JSON)
    protected JsonNode bom;

    @SuppressWarnings("java:S107")
    public CBOMReadModel(
            @Nonnull UUID id,
            @Nonnull String projectIdentifier,
            @Nonnull String repository,
            @Nullable String revision,
            @Nullable String packageFolder,
            @Nullable String commit,
            @Nonnull Date createdAt,
            @Nonnull JsonNode bom) {
        this.id = id;
        this.projectIdentifier = projectIdentifier;
        this.repository = repository;
        this.revision = revision;
        this.packageFolder = packageFolder;
        this.commit = commit;
        this.createdAt = createdAt;
        this.bom = bom;
    }

    protected CBOMReadModel() {}

    @Override
    public @Nonnull UUID getId() {
        return this.id;
    }

    @Nonnull
    public String getProjectIdentifier() {
        return projectIdentifier;
    }

    @Nonnull
    public String getRepository() {
        return repository;
    }

    @Nullable public String getRevision() {
        return revision;
    }

    @Nullable public String getPackageFolder() {
        return packageFolder;
    }

    @Nullable public String getCommit() {
        return commit;
    }

    @Nonnull
    public Date getCreatedAt() {
        return createdAt;
    }

    @Nonnull
    public JsonNode getBom() {
        return bom;
    }
}
