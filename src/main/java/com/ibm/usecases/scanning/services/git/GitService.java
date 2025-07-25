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

import com.ibm.domain.scanning.Commit;
import com.ibm.domain.scanning.GitUrl;
import com.ibm.domain.scanning.Revision;
import com.ibm.domain.scanning.authentication.ICredentials;
import com.ibm.domain.scanning.authentication.PersonalAccessToken;
import com.ibm.domain.scanning.authentication.UsernameAndPasswordCredentials;
import com.ibm.usecases.scanning.errors.GitCloneFailed;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.pqca.errors.ClientDisconnected;
import org.pqca.progress.IProgressDispatcher;
import org.pqca.progress.ProgressMessage;
import org.pqca.progress.ProgressMessageType;

public final class GitService {
    @Nullable private final IProgressDispatcher progressDispatcher;
    @Nonnull private final String baseCloneDirPath;
    @Nullable private final ICredentials credentials;

    public GitService(@Nonnull String baseCloneDirPath, @Nullable ICredentials credentials) {
        this(null, baseCloneDirPath, credentials);
    }

    public GitService(
            @Nonnull IProgressDispatcher progressDispatcher,
            @Nonnull String baseCloneDirPath,
            @Nullable ICredentials credentials) {
        this.progressDispatcher = progressDispatcher;
        this.baseCloneDirPath = baseCloneDirPath;
        this.credentials = credentials;
    }

    @Nonnull
    public CloneResultDTO clone(
            @Nonnull GitUrl gitUrl, @Nonnull Revision revision, @Nullable Commit commit)
            throws GitCloneFailed, ClientDisconnected {
        try {
            final File scanCloneFile = createDirectory();
            final Git clonedRepo =
                    Git.cloneRepository()
                            .setProgressMonitor(getProgressMonitor())
                            .setURI(gitUrl.value())
                            // .setBranch(revision.value())
                            .setDirectory(scanCloneFile)
                            .setCredentialsProvider(getCredentialsProvider(credentials))
                            .call();

            if (commit != null) {
                final Ref ref =
                        clonedRepo
                                .checkout()
                                .setName(revision.value())
                                .setCreateBranch(false)
                                .setStartPoint(commit.hash())
                                .call();
                if (ref == null) {
                    throw new GitCloneFailed(
                            "Commit "
                                    + commit.hash()
                                    + " not found for revision "
                                    + revision.value());
                }
            } else {
                final List<Ref> refs = clonedRepo.tagList().call();
                Ref ref =
                        refs.stream()
                                .filter(r -> r.getName().endsWith(revision.value()))
                                .findFirst()
                                .orElse(null);
                if (ref == null) {
                    ref = clonedRepo.getRepository().findRef(revision.value());
                }
                if (ref == null) {
                    throw new GitCloneFailed("Revision not found: " + revision.value());
                }

                ObjectId commitHash = ref.getPeeledObjectId(); // only works for tagged versions
                if (commitHash == null) {
                    commitHash = ref.getObjectId();
                }
                if (commitHash == null) {
                    throw new GitCloneFailed("Commit not found for revision " + revision.value());
                }
                commit = new Commit(commitHash.abbreviate(7).name());
            }

            return new CloneResultDTO(commit, scanCloneFile);
        } catch (GitAPIException | GitCloneFailed | IOException e) {
            throw new GitCloneFailed("Git clone failed: " + e.getMessage());
        }
    }

    @Nonnull
    private File createDirectory() throws GitCloneFailed {
        // create directory
        final String folderId = UUID.randomUUID().toString().replace("-", "");
        final String scanClonePath = this.baseCloneDirPath + File.separator + folderId;
        final File scanCloneFile = new File(scanClonePath);
        if (scanCloneFile.exists()) {
            throw new GitCloneFailed("Clone dir already exists " + scanCloneFile.getPath());
        }
        if (!scanCloneFile.mkdirs()) {
            throw new GitCloneFailed("Could not create " + scanCloneFile.getPath());
        }
        return scanCloneFile;
    }

    @Nonnull
    private GitProgressMonitor getProgressMonitor() {
        if (this.progressDispatcher == null) {
            return null;
        }

        return new GitProgressMonitor(
                progressMessage -> {
                    try {
                        this.progressDispatcher.send(
                                new ProgressMessage(ProgressMessageType.LABEL, progressMessage));
                    } catch (ClientDisconnected e) {
                        // nothing
                    }
                });
    }

    @Nullable private CredentialsProvider getCredentialsProvider(@Nullable ICredentials credentials) {
        if (credentials
                instanceof
                UsernameAndPasswordCredentials(
                        @Nonnull String username,
                        @Nonnull String password)) {
            return new UsernamePasswordCredentialsProvider(username, password);
        } else if (credentials instanceof PersonalAccessToken(@Nonnull String token)) {
            return new UsernamePasswordCredentialsProvider(token, "");
        }
        return null;
    }
}
