/*
 * CBOMkit
 * Copyright (C) 2025 PQCA
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
package com.ibm.usecases.scanning.services.pkg;

import com.github.packageurl.PackageURL;
import jakarta.annotation.Nonnull;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PackageFinderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PackageFinderService.class);

    protected Path root;

    protected PackageFinderService(@Nonnull File rootFile) throws IllegalArgumentException {
        if (!rootFile.isDirectory()) {
            throw new IllegalArgumentException("Path must be a directory!");
        }
        this.root = rootFile.toPath();
    }

    @Nonnull
    public Optional<Path> findPackage(@Nonnull PackageURL purl) {
        LOGGER.info("Searching package folder for purl {}", purl);
        try (Stream<Path> walk = Files.walk(this.root)) {
            final List<Path> poms =
                    walk.filter(p -> !Files.isDirectory(p)).filter(this::isBuildFile).toList();
            for (final Path pomPath : poms) {
                final Optional<String> possiblePackageName = getPackageName(pomPath);
                if (!possiblePackageName.isEmpty()) {
                    final String packageName = possiblePackageName.get();
                    if (purl.getName().equals(packageName)) {
                        final Path pkgPath = this.root.relativize(pomPath.getParent());
                        LOGGER.info("Identified package folder: {}", pkgPath);
                        return Optional.of(pkgPath);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to find package folder: {}", e.getLocalizedMessage());
        }
        return Optional.empty();
    }

    public abstract boolean isBuildFile(@Nonnull Path file);

    public abstract Optional<String> getPackageName(@Nonnull Path buildFile) throws Exception;
}
