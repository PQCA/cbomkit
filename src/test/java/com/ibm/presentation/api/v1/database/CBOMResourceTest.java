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
package com.ibm.presentation.api.v1.database;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CBOMResourceTest {

    @Test
    @DisplayName(
            "Test that /api/v1/cbom/<projetcIdentifier> endpoint for an in valid pi returns 404")
    void testGetBOMInvalidPI() {
        given().when()
                .get("/api/v1/cbom/invalid")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Transactional
    @Test
    @DisplayName("Test that a CBOM can be stored, retrieved and deleted")
    void testCBOMStoreGetDelete() {
        String testIdentifier = "pkg:test/empty";

        String cbomString =
                "{"
                        + " \"bomFormat\": \"CycloneDX\","
                        + " \"specVersion\": \"1.6\","
                        + " \"serialNumber\": \"1\","
                        + " \"version\": 1 }";
        given().pathParam("projectIdentifier", testIdentifier)
                .when()
                .header("Content-type", "application/json")
                .body(cbomString)
                .when()
                .post("/api/v1/cbom/{projectIdentifier}")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());

        given().pathParam("projectIdentifier", testIdentifier)
                .when()
                .get("/api/v1/cbom/{projectIdentifier}")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body(
                        "projectIdentifier", equalTo(testIdentifier),
                        "bom.serialNumber", equalTo("1"));

        given().pathParam("projectIdentifier", testIdentifier)
                .when()
                .delete("/api/v1/cbom/{projectIdentifier}")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @DisplayName(
            "Test that /api/v1/cbom/<projetcIdentifier> endpoint for an in valid pi returns 404")
    void testDeleteBOMInvalidPI() {
        given().when()
                .delete("/api/v1/cbom/invalid")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @DisplayName("Test that /api/v1/cbom/lastn endpoint returns up to 5 CBOMS")
    void testGetLastCBOMs() {
        final int limit = 5;
        given().when()
                .get("/api/v1/cbom/last/" + limit)
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("size()", lessThanOrEqualTo(limit));
    }
}
