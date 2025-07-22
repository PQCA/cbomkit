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
package com.ibm.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.ibm.domain.compliance.CryptographicAsset;
import com.ibm.domain.scanning.ScanAggregate;
import com.ibm.domain.scanning.ScanUrl;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;
import org.pqca.scanning.CBOM;

@AnalyzeClasses(packages = "com.ibm.domain", importOptions = ImportOption.DoNotIncludeTests.class)
class DomainTest {

    private static final List<String> allowedDomainClasses =
            List.of(
                    "..domain..",
                    "java..",
                    "javax..",
                    "app.bootstrap.core.ddd..",
                    "jakarta.annotation..");

    private static final List<String> cbomAndCryptographicAssetDomainClasses =
            List.of(
                    "..domain..",
                    "java..",
                    "javax..",
                    "app.bootstrap.core.ddd..",
                    "jakarta.annotation..",
                    "org.cyclonedx..", // to not replicate bom object
                    "com.fasterxml.jackson.databind.." // dependency need for cyclonedx lib
                    );

    private static final List<String> scanAggregateAndScanUrlDomainClasses =
            List.of(
                    "..domain..",
                    "java..",
                    "javax..",
                    "app.bootstrap.core.ddd..",
                    "jakarta.annotation..",
                    "com.github.packageurl.." // to not replicate packageUrl object
                    );

    @ArchTest
    static final ArchRule defaultDomainIsolation =
            classes()
                    .should()
                    .onlyDependOnClassesThat()
                    .resideInAnyPackage(allowedDomainClasses.toArray(String[]::new))
                    .orShould()
                    .beAssignableTo(CBOM.class)
                    .orShould()
                    .beAssignableTo(CryptographicAsset.class)
                    .orShould()
                    .beAssignableTo(ScanAggregate.class)
                    .orShould()
                    .beAssignableTo(ScanUrl.class);

    @ArchTest
    static final ArchRule cbomAndCryptographicAssetDomainIsolation =
            classes()
                    .that()
                    .areAssignableTo(CBOM.class)
                    .or()
                    .areAssignableTo(CryptographicAsset.class)
                    .should()
                    .onlyDependOnClassesThat()
                    .resideInAnyPackage(
                            cbomAndCryptographicAssetDomainClasses.toArray(String[]::new));

    @ArchTest
    static final ArchRule scanAggregateAndScanUrlDomainIsolation =
            classes()
                    .that()
                    .areAssignableTo(ScanAggregate.class)
                    .or()
                    .areAssignableTo(ScanUrl.class)
                    .should()
                    .onlyDependOnClassesThat()
                    .resideInAnyPackage(
                            scanAggregateAndScanUrlDomainClasses.toArray(String[]::new));
}
