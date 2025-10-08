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

import static com.ibm.output.IAggregator.LOGGER;

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
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
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

    @Nullable private static String extractVersion(String rev) {
        if (rev == null || rev.isEmpty()) {
            return null;
        }

        Matcher matcher = Pattern.compile("\\b(\\d+(?:\\.\\d+)+)\\b").matcher(rev);

        return matcher.find() ? matcher.group(1) : null;
    }

    @Nonnull
    public CloneResultDTO clone(
            @Nonnull GitUrl gitUrl, @Nonnull Revision revision, @Nullable Commit commit)
            throws GitCloneFailed, ClientDisconnected {
        final File scanCloneFile = createDirectory();
        try {
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
                // Try to find a tag matching the exact revision string first ...
                Ref ref = clonedRepo.getRepository().findRef(revision.value());
                if (ref == null) {
                    //  ... otherwise extract the version (only numbers and dots)
                    // and try to find a tag that ends with this version
                    // (separated by . or _)
                    final String version = extractVersion(revision.value());
                    final String alternative = version.replaceAll("\\.", "_");
                    ref =
                            refs.stream()
                                    .filter(
                                            r ->
                                                    r.getName().endsWith(version)
                                                            || r.getName().endsWith(alternative))
                                    .findFirst()
                                    .orElse(null);
                }
                if (ref == null) {
                    throw new GitCloneFailed("Revision not found: " + revision.value());
                }
                LOGGER.info("Found revision {}", ref.getName());

                ObjectId commitHash = ref.getPeeledObjectId(); // only works for tagged versions
                if (commitHash == null) {
                    commitHash = ref.getObjectId();
                }
                if (commitHash == null) {
                    throw new GitCloneFailed("Commit not found for revision " + revision.value());
                }
                try (ObjectReader reader = clonedRepo.getRepository().newObjectReader()) {
                    commit = new Commit(reader.abbreviate(commitHash, 7).name());
                }
            }

            return new CloneResultDTO(commit, scanCloneFile);
        } catch (GitAPIException | GitCloneFailed | IOException e) {
            Optional.ofNullable(scanCloneFile)
                    .ifPresent(
                            dir -> {
                                try {
                                    FileUtils.deleteDirectory(dir);
                                } catch (IOException e1) {
                                    // do nothing
                                }
                            });
            throw new GitCloneFailed("Git clone from " + gitUrl.value() + " failed", e);
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
