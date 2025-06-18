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
import app.bootstrap.core.cqrs.IQueryBus;
import com.ibm.usecases.database.commands.DeleteCBOMCommand;
import com.ibm.usecases.database.commands.StoreCBOMCommand;
import com.ibm.usecases.database.errors.NoCBOMForProjectIdentifierFound;
import com.ibm.usecases.database.queries.GetCBOMByProjectIdentifierQuery;
import com.ibm.usecases.database.queries.ListStoredCBOMsQuery;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.concurrent.ExecutionException;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.jboss.resteasy.reactive.RestPath;

@Path("/api/v1/cbom")
@ApplicationScoped
public class CBOMResource {

    @Nonnull protected final ICommandBus commandBus;
    @Nonnull protected final IQueryBus queryBus;

    public CBOMResource(@Nonnull ICommandBus commandBus, @Nonnull IQueryBus queryBus) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
    }

    @GET
    @Path("/last/{limit}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Return recently generated CBOMs from the repository",
            description =
                    "Returns a list of the most recently generated CBOMs. "
                            + "The length of the list can by specified via the optional 'limit' "
                            + "parameter.")
    public Response getLastCBOMs(@RestPath @Nullable Integer limit)
            throws ExecutionException, InterruptedException {
        return this.queryBus
                .send(new ListStoredCBOMsQuery(limit))
                .thenApply(readModels -> Response.ok(readModels).build())
                .get();
    }

    @GET
    @Path("/{projectIdentifier}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCBOM(@RestPath @Nullable String projectIdentifier) {
        if (projectIdentifier == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        try {
            final var readModel =
                    this.queryBus
                            .send(new GetCBOMByProjectIdentifierQuery(projectIdentifier))
                            .get();
            return Response.ok(readModel).build();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof NoCBOMForProjectIdentifierFound) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("/{projectIdentifier}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response storeCBOM(
            @Nullable @RestPath String projectIdentifier, @Nullable String cbomJson) {
        if (projectIdentifier == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (cbomJson == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            commandBus.send(new StoreCBOMCommand(projectIdentifier, cbomJson));
            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DELETE
    @Path("/{projectIdentifier}")
    public Response deleteCBOM(@Nullable @RestPath String projectIdentifier) {
        if (projectIdentifier == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            boolean success = this.commandBus.send(new DeleteCBOMCommand(projectIdentifier)).get();
            if (success) {
                return Response.ok().build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            if (e.getCause() instanceof NoCBOMForProjectIdentifierFound) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
