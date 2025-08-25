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
package com.ibm.presentation.api.v1.scanning;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ScanningResourceTest {

    @Transactional
    @Test
    @DisplayName("Test that scan can be started")
    void testScanAggregate() throws InterruptedException {
        String projectIdentifier = "pkg:github/mastercard/client-encryption-java@1b27c1d";
        ScanRequest scanRequest = new ScanRequest(projectIdentifier, null, null, null);
        given().when()
                .header("Content-type", "application/json")
                .body(scanRequest)
                .when()
                .post("/api/v1/scan")
                .then()
                .statusCode(Response.Status.ACCEPTED.getStatusCode());

        // The scan endpoint just submits a scan request. The scan
        // may not have completed when the test terminates.
        // Avoid some ugly warnings.
        Thread.sleep(10000);
    }
}
