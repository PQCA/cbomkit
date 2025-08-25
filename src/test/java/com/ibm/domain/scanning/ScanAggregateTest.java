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
package com.ibm.domain.scanning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ScanAggregateTest {

    @Test
    @DisplayName("Test getProjectIdentifier from purl")
    public void testGetIdentifierPurl() {
        ScanUrl scanUrl = new ScanUrl("pkg:github/mastercard/client-encryption-java@1b27c1d");
        ScanRequest req = new ScanRequest(scanUrl, ScanAggregate.REVISION_MAIN, null);
        assertThatCode(
                        () -> {
                            ScanAggregate scanAggregate =
                                    ScanAggregate.requestScan(new ScanId(), req, null);
                            assertThat(scanAggregate).isNotNull();
                            assertThat(scanAggregate.getProjectIdentifier())
                                    .isEqualTo(scanUrl.value());
                        })
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Test getProjectIdentifier from git url")
    public void testGetIdentifierGitUrl() {
        ScanUrl scanUrl = new ScanUrl("https://github.com/mastercard/client-encryption-java");
        ScanRequest req = new ScanRequest(scanUrl, ScanAggregate.REVISION_MAIN, null);
        assertThatCode(
                        () -> {
                            ScanAggregate scanAggregate =
                                    ScanAggregate.requestScan(new ScanId(), req, null);
                            assertThat(scanAggregate).isNotNull();
                            assertThat(scanAggregate.getProjectIdentifier())
                                    .isEqualTo("pkg:github/mastercard/client-encryption-java");
                        })
                .doesNotThrowAnyException();
    }
}
