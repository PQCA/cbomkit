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
package com.ibm.domain.scanning.events;

import app.bootstrap.core.ddd.DomainEvent;
import com.ibm.domain.scanning.ScanAggregate;
import com.ibm.domain.scanning.ScanId;
import jakarta.annotation.Nonnull;

public final class RevisionIdentifiedEvent extends DomainEvent {

    public RevisionIdentifiedEvent(@Nonnull ScanId scanId) {
        super(scanId, ScanAggregate.class, null);
    }

    @Nonnull
    public ScanId getScanId() {
        return (ScanId) this.aggregateId;
    }

    @Nonnull
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[id=" + this.aggregateId + "]";
    }
}
