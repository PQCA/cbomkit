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

import jakarta.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.ini4j.Ini;

public class SetupPackageFinderService extends PackageFinderService {

    public SetupPackageFinderService(@Nonnull File rootFile) throws IllegalArgumentException {
        super(rootFile);
    }

    @Override
    public boolean isBuildFile(@Nonnull Path file) {
        return file.endsWith("setup.cfg") || file.endsWith("setup.py");
    }

    @Override
    public Optional<String> getPackageName(@Nonnull Path buildFile) {
        try {
            if (buildFile.endsWith("setup.cfg")) {
                final Ini cfg = new Ini(buildFile.toFile());
                return Optional.ofNullable(cfg.get("metadata", "name"));
            }
            return findPackageNameUsingRegex(buildFile);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Nonnull
    private Optional<String> findPackageNameUsingRegex(@Nonnull Path buildFile) throws Exception {
        try (BufferedReader reader = new BufferedReader(new FileReader(buildFile.toFile()))) {
            final Pattern pattern = Pattern.compile("name\\s*=\\s*['\"]([^'\"]*)['\"]");
            String line;

            while ((line = reader.readLine()) != null) {
                final Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    return Optional.ofNullable(matcher.group(1));
                }
            }
        }
        return Optional.empty();
    }
}
