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
package com.ibm.presentation.error;

import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public final class ErrorCodeGenerator {
    private static ErrorCodeGenerator INSTANCE;

    private static final String PREFIX = "ERR";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MMdd");

    private LocalDate currentDate = LocalDate.now();
    private final Map<String, AtomicInteger> errorCounters = new ConcurrentHashMap<>();

    private ErrorCodeGenerator() {}

    public static synchronized ErrorCodeGenerator getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ErrorCodeGenerator();
        }
        return INSTANCE;
    }

    @Nonnull
    public synchronized String generateErrorCode(@Nonnull String errorType) {
        final LocalDate today = LocalDate.now();

        if (!today.equals(currentDate)) {
            currentDate = today;
            errorCounters.clear();
        }

        AtomicInteger counter = errorCounters.computeIfAbsent(errorType, v -> new AtomicInteger(0));
        int sequenceNumber = counter.incrementAndGet();

        String formattedDate = today.format(DATE_FORMAT);
        String formattedSequence = String.format("%04d", sequenceNumber);

        return String.format("%s-%s-%s-%s", PREFIX, errorType, formattedDate, formattedSequence);
    }
}
