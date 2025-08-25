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
package com.ibm.usecases.scanning.services.pkg;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import io.quarkus.test.junit.QuarkusTest;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class PackageFinderServiceTest {

    @Test
    @DisplayName("Test package finders")
    void testPackageFinder() throws MalformedPackageURLException {
        File root = new File("testdata/nested");
        PackageURL purl = new PackageURL("pkg:test/nested/module");
        List.of(
                        new MavenPackageFinderService(root),
                        new TomlPackageFinderService(root),
                        new SetupPackageFinderService(root))
                .forEach(
                        packageFinder -> {
                            Optional<Path> packagePath = packageFinder.findPackage(purl);
                            assertThat(packagePath)
                                    .hasValueSatisfying(
                                            path ->
                                                    assertThat(path)
                                                            .isEqualTo(Paths.get("src/module")));
                        });
    }
}
