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
package com.ibm.usecases.scanning.services.git;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ibm.domain.scanning.Commit;
import com.ibm.domain.scanning.GitUrl;
import com.ibm.domain.scanning.ScanAggregate;
import com.ibm.infrastructure.Configuration;
import com.ibm.usecases.scanning.errors.GitCloneFailed;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.TransportException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class GitServiceTest {
    @Inject Configuration config;

    @Test
    @DisplayName("Test of git clone")
    void testGitClone() {
        GitUrl gitUrl = new GitUrl("https://github.com/mastercard/client-encryption-java");
        Commit commit = new Commit("1b27c1d");

        assertThatCode(
                        () -> {
                            GitService gitService =
                                    new GitService(config.getBaseCloneDirPath(), null);
                            CloneResultDTO git =
                                    gitService.clone(gitUrl, ScanAggregate.REVISION_MAIN, commit);
                            assertThat(git).isNotNull();
                            assertThat(git.directory().getParent())
                                    .isEqualTo(config.getBaseCloneDirPath());
                            assertThat(git.commit()).isEqualTo(commit);

                            // cleanup
                            FileUtils.deleteDirectory(git.directory());
                        })
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Test of git clone fail")
    void testGitCloneFail() {
        GitUrl gitUrl = new GitUrl("http://svn.apache.org/viewvc/commons/proper/codec/trunk");

        assertThatThrownBy(
                        () -> {
                            GitService gitService =
                                    new GitService(config.getBaseCloneDirPath(), null);
                            gitService.clone(gitUrl, ScanAggregate.REVISION_MAIN, null);
                        })
                .isInstanceOf(GitCloneFailed.class)
                .hasMessage("Git clone from " + gitUrl.value() + " failed")
                .hasCauseInstanceOf(TransportException.class)
                .cause()
                .hasMessage(
                        gitUrl.value()
                                + ": Authentication is required but no CredentialsProvider has been registered");

        // Test that no clone dir was left
        assertThat(new File(config.getBaseCloneDirPath()).list()).isEmpty();
    }
}
