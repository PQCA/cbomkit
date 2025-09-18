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
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class PackageFinderServiceTest {

    @Test
    @DisplayName("Test maven package finder")
    void testMavenPackageFinder() throws MalformedPackageURLException {
        File root = new File("testdata/nested");
        PackageURL purl = new PackageURL("pkg:test/nested/module");
        PackageFinderService packageFinder = new MavenPackageFinderService(root);
        Optional<Path> packagePath = packageFinder.findPackage(purl);
        assertThat(packagePath)
                .hasValueSatisfying(path -> assertThat(path).isEqualTo(Paths.get("src/module")));
    }

    @Test
    @DisplayName("Test python package finder for pyproject.toml")
    void testTomlPackageFinder() throws MalformedPackageURLException {
        File root = new File("testdata/python");
        PackageURL purl = new PackageURL("pkg:test/python/module_a");
        PackageFinderService packageFinder = new PypiPackageFinderService(root);
        Optional<Path> packagePath = packageFinder.findPackage(purl);
        assertThat(packagePath)
                .hasValueSatisfying(path -> assertThat(path).isEqualTo(Paths.get("module_a")));
    }

    @Test
    @DisplayName("Test python package finder for setup.cfg")
    void testSetupCfgPackageFinder() throws MalformedPackageURLException {
        File root = new File("testdata/python");
        PackageURL purl = new PackageURL("pkg:test/python/module_b");
        PackageFinderService packageFinder = new PypiPackageFinderService(root);
        Optional<Path> packagePath = packageFinder.findPackage(purl);
        assertThat(packagePath)
                .hasValueSatisfying(path -> assertThat(path).isEqualTo(Paths.get("module_b")));
    }

    @Test
    @DisplayName("Test python package finder for setup.py]")
    void testSetupPyPackageFinder() throws MalformedPackageURLException {
        File root = new File("testdata/python");
        PackageURL purl = new PackageURL("pkg:test/python/module_c");
        PackageFinderService packageFinder = new PypiPackageFinderService(root);
        Optional<Path> packagePath = packageFinder.findPackage(purl);
        assertThat(packagePath)
                .hasValueSatisfying(path -> assertThat(path).isEqualTo(Paths.get("module_c")));
    }
}
