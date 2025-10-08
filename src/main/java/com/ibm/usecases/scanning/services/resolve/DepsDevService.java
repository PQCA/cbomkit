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
package com.ibm.usecases.scanning.services.resolve;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.packageurl.PackageURL;
import com.ibm.domain.scanning.GitUrl;
import com.ibm.usecases.scanning.errors.PurlResolutionFailed;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DepsDevService implements PurlResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(DepsDevService.class);
    private static final String DEPS_DEV_URI = "https://api.deps.dev/v3alpha/purl/";
    private static final String SOURCE_REPO = "SOURCE_REPO";

    private static final class DepsDevResponseHandler implements HttpClientResponseHandler<String> {
        @Nullable @Override
        public String handleResponse(@Nonnull ClassicHttpResponse httpResponse) throws IOException {
            if (httpResponse.getCode() != HttpStatus.SC_OK) {
                throw new IOException(
                        "Received " + httpResponse.getCode() + " response from " + DEPS_DEV_URI);
            }
            return extractSourceRepo(httpResponse.getEntity().getContent());
        }

        @Nonnull
        public String extractSourceRepo(InputStream in) throws IOException {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(in);
            JsonNode version = rootNode.get("version");
            if (version != null) {
                ArrayNode links = (ArrayNode) version.get("links");
                if (links != null) {
                    for (JsonNode link : links) {
                        if (SOURCE_REPO.equals(link.get("label").textValue())) {
                            return link.get("url").textValue();
                        }
                    }
                }
            }
            throw new IOException("Invalid deps.dev response");
        }
    }

    @Nonnull
    public GitUrl resolve(@Nonnull PackageURL purl) throws PurlResolutionFailed {
        String purlStr = purl.getCoordinates();
        LOGGER.info("Sending DepsDev request for {}", purlStr);

        final HttpGet request =
                new HttpGet(DEPS_DEV_URI + URLEncoder.encode(purlStr, StandardCharsets.UTF_8));

        try (final CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            final String srcRepo =
                    httpClient
                            .execute(request, new DepsDevResponseHandler())
                            .replaceAll("scm:git:git://", "https://")
                            .replaceAll(".git$", "");
            LOGGER.info("Identified git repository {} for purl {}", srcRepo, purlStr);
            return new GitUrl(srcRepo);
        } catch (IOException ioe) {
            throw new PurlResolutionFailed(purlStr, ioe.getMessage());
        }
    }
}
