package org.tkit.onecx.human.task.bff.rs.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.onecx.human.task.bff.rs.mappers.ExceptionMapper;
import org.tkit.onecx.human.task.bff.rs.mappers.TaskMapper;
import org.tkit.quarkus.log.cdi.LogService;

import gen.org.tkit.onecx.human.task.bff.rs.internal.TasksInternalApiService;
import gen.org.tkit.onecx.human.task.bff.rs.internal.model.*;
import gen.org.tkit.onecx.human.task.client.api.TasksInternalApi;
import gen.org.tkit.onecx.human.task.client.model.*;

@ApplicationScoped
@Transactional(Transactional.TxType.NOT_SUPPORTED)
@LogService
public class TasksInternalRestController implements TasksInternalApiService {

    @Inject
    @RestClient
    TasksInternalApi client;

    @Inject
    TaskMapper mapper;

    @Inject
    ExceptionMapper exceptionMapper;

    @Override
    public Response searchTasksByCriteria(TaskSearchCriteriaDTO taskSearchCriteriaDto) {
        try (Response response = client.searchTasksByCriteria(mapper.toTaskSearchCriteria(taskSearchCriteriaDto))) {
            TaskPageResult result = response.readEntity(TaskPageResult.class);
            return Response.status(response.getStatus()).entity(mapper.toTaskPageResultDTO(result)).build();
        }
    }

    @Override
    public Response getTaskById(String id) {
        try (Response response = client.getTaskById(id)) {
            Task result = response.readEntity(Task.class);
            return Response.status(response.getStatus()).entity(mapper.toGetTaskResponseDTO(result)).build();
        }
    }

    @Override
    public Response deleteTaskById(String id) {
        try (Response response = client.deleteTaskById(id)) {
            return Response.status(response.getStatus()).build();
        }
    }

    @Override
    public Response acceptTask(String id, AcceptTaskRequestDTO acceptTaskRequestDto) {
        try (Response response = client.acceptTask(id, mapper.toAcceptTaskRequest(acceptTaskRequestDto))) {
            return Response.status(response.getStatus()).build();
        }
    }

    @Override
    public Response declineTask(String id, DeclineTaskRequestDTO declineTaskRequestDto) {
        try (Response response = client.declineTask(id, mapper.toDeclineTaskRequest(declineTaskRequestDto))) {
            return Response.status(response.getStatus()).build();
        }
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetailResponseDTO> constraint(ConstraintViolationException ex) {
        return exceptionMapper.constraint(ex);
    }

    @ServerExceptionMapper
    public Response restException(ClientWebApplicationException ex) {
        return exceptionMapper.clientException(ex);
    }
}
