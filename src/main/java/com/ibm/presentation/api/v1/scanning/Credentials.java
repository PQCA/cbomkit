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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.domain.scanning.authentication.ICredentials;
import com.ibm.domain.scanning.authentication.PersonalAccessToken;
import com.ibm.domain.scanning.authentication.UsernameAndPasswordCredentials;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public record Credentials(
        @Nullable @JsonProperty("username") String username,
        @Nullable @JsonProperty("password") String password,
        @Nullable @JsonProperty("pat") String pat) {

    @Nullable public static ICredentials extractFrom(@Nonnull ScanRequest scanRequest) {
        @Nullable ICredentials authCredentials = null;
        final Credentials credentials = scanRequest.credentials();
        if (credentials != null) {
            if (credentials.username() != null && credentials.password() != null) {
                authCredentials =
                        new UsernameAndPasswordCredentials(
                                credentials.username(), credentials.password());
            } else if (credentials.pat() != null) {
                authCredentials = new PersonalAccessToken(credentials.pat());
            }
        }
        return authCredentials;
    }
}
