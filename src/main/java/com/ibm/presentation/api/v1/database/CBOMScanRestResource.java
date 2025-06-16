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

import app.bootstrap.core.cqrs.ICommandBus;
import app.bootstrap.core.ddd.IDomainEventBus;
import com.ibm.domain.scanning.Revision;
import com.ibm.domain.scanning.ScanId;
import com.ibm.domain.scanning.authentication.ICredentials;
import com.ibm.domain.scanning.authentication.PersonalAccessToken;
import com.ibm.domain.scanning.authentication.UsernameAndPasswordCredentials;
import com.ibm.infrastructure.progress.NoOpProgressDispatcher;
import com.ibm.infrastructure.scanning.IScanConfiguration;
import com.ibm.infrastructure.scanning.repositories.ScanRepository;
import com.ibm.presentation.api.v1.scanning.Credentials;
import com.ibm.presentation.api.v1.scanning.ScanRequest;
import com.ibm.usecases.scanning.commands.RequestScanCommand;
import com.ibm.usecases.scanning.processmanager.ScanProcessManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/cbom")
@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CBOMScanRestResource {

    private final ICommandBus commandBus;
    private final IDomainEventBus domainEventBus;
    private final IScanConfiguration configuration;

    public CBOMScanRestResource(
            ICommandBus commandBus,
            IDomainEventBus domainEventBus,
            IScanConfiguration configuration) {
        this.commandBus = commandBus;
        this.domainEventBus = domainEventBus;
        this.configuration = configuration;
    }

    @POST
    @Path("/scan")
    public Response scanRepo(ScanRequest scanRequest) {
        try {
            ScanId scanId = new ScanId();

            com.ibm.domain.scanning.ScanRequest domainRequest =
                    new com.ibm.domain.scanning.ScanRequest(
                            new com.ibm.domain.scanning.ScanUrl(scanRequest.getScanUrl()),
                            scanRequest.getBranch() != null
                                    ? new Revision(scanRequest.getBranch())
                                    : new Revision("main"),
                            scanRequest.getSubfolder());

            ScanRepository scanRepository = new ScanRepository(domainEventBus);
            ScanProcessManager scanProcessManager =
                    new ScanProcessManager(
                            scanId,
                            commandBus,
                            scanRepository,
                            new NoOpProgressDispatcher(),
                            configuration);
            commandBus.register(scanProcessManager);

            ICredentials credentials = extractCredentials(scanRequest);

            commandBus.send(
                    new RequestScanCommand(
                            scanId,
                            domainRequest.scanUrl().value(),
                            domainRequest.revision().value(),
                            domainRequest.subFolder(),
                            credentials));

            return Response.status(Response.Status.ACCEPTED)
                    .entity("{\"scanId\": \"" + scanId.toString() + "\", \"status\": \"started\"}")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    private ICredentials extractCredentials(ScanRequest request) {
        Credentials c = request.getCredentials();
        if (c != null) {
            if (c.username() != null && c.password() != null) {
                return new UsernameAndPasswordCredentials(c.username(), c.password());
            } else if (c.pat() != null) {
                return new PersonalAccessToken(c.pat());
            }
        }
        return null;
    }
}
