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
package com.ibm.usecases.scanning.services.resolve;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.github.packageurl.PackageURL;
import com.ibm.domain.scanning.GitUrl;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class PurlResolverTest {

    @Test
    @DisplayName("Test of Maven Purl resolution via deps.dev")
    void testDepsDevResolve() {
        assertThatCode(
                        () -> {
                            PurlResolver resolver = new DepsDevService();
                            GitUrl gitUrl =
                                    resolver.resolve(
                                            new PackageURL(
                                                    "pkg:maven/io.quarkus/quarkus-fs-util@0.0.10"));
                            gitUrl.validate();
                            assertThat(gitUrl.value())
                                    .isEqualTo("https://github.com/quarkusio/quarkus-fs-util");
                        })
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Test of Github Purl resolution")
    void testGithubResolve() {
        assertThatCode(
                        () -> {
                            PurlResolver resolver = new GithubPurlResolver();
                            GitUrl gitUrl =
                                    resolver.resolve(
                                            new PackageURL(
                                                    "pkg:github/mastercard/client-encryption-java@1b27c1d"));
                            gitUrl.validate();
                            assertThat(gitUrl.value())
                                    .isEqualTo(
                                            "https://github.com/mastercard/client-encryption-java");
                        })
                .doesNotThrowAnyException();
    }
}
