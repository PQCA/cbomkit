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
import org.apache.commons.configuration.HierarchicalINIConfiguration;

public class PypiPackageFinderService extends PackageFinderService {

    public static final String PYPROJECT_TOML = "pyproject.toml";
    public static final String SETUP_CFG = "setup.cfg";
    public static final String SETUP_PY = "setup.py";

    public static final String PYPROJECT_TOML_KEY = "project.name";
    public static final String SETUP_CFG_KEY = "metadata.name";

    public static final Pattern SETUP_PY_PATTERN =
            Pattern.compile("name\\s*=\\s*['\"]([^'\"]*)['\"]");

    public PypiPackageFinderService(@Nonnull File rootFile) throws IllegalArgumentException {
        super(rootFile);
    }

    @Override
    public boolean isBuildFile(@Nonnull Path file) {
        return file.endsWith(SETUP_CFG) || file.endsWith(SETUP_PY) || file.endsWith(PYPROJECT_TOML);
    }

    @Override
    public Optional<String> getPackageName(@Nonnull Path buildFile) throws Exception {
        if (buildFile.endsWith(SETUP_PY)) {
            return findPackageNameUsingRegex(buildFile);
        }

        final HierarchicalINIConfiguration cfg =
                new HierarchicalINIConfiguration(buildFile.toFile());
        if (buildFile.endsWith(SETUP_CFG)) {
            return Optional.ofNullable(cfg.getString(SETUP_CFG_KEY));
        } else if (buildFile.endsWith(PYPROJECT_TOML)) {
            return Optional.ofNullable(cfg.getString(PYPROJECT_TOML_KEY));
        }

        return Optional.empty();
    }

    @Nonnull
    private Optional<String> findPackageNameUsingRegex(@Nonnull Path buildFile) throws Exception {
        try (BufferedReader reader = new BufferedReader(new FileReader(buildFile.toFile()))) {
            return reader.lines()
                    .map(line -> SETUP_PY_PATTERN.matcher(line))
                    .filter(Matcher::find)
                    .map(matcher -> matcher.group(1))
                    .findFirst();
        }
    }
}
