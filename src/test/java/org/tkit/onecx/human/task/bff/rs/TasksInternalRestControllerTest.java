package org.tkit.onecx.human.task.bff.rs;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.JsonBody;
import org.mockserver.model.MediaType;
import org.tkit.onecx.human.task.bff.rs.controller.TasksInternalRestController;
import org.tkit.onecx.human.task.bff.rs.mappers.ExceptionMapper;

import gen.org.tkit.onecx.human.task.bff.rs.internal.model.*;
import gen.org.tkit.onecx.human.task.client.model.*;
import io.quarkiverse.mockserver.test.InjectMockServerClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(TasksInternalRestController.class)
class TasksInternalRestControllerTest extends AbstractTest {

    private static final String taskId = "82689h23-9624-2234-c50b-8749d073c287";
    private static final String HUMAN_TASK_SVC_INTERNAL_API_BASE_PATH = "/internal/tasks";
    private static final String SEARCH_ENDPOINT = "/search";
    private static final String ACCEPT_ENDPOINT = "/accept";
    private static final String DECLINE_ENDPOINT = "/decline";

    @InjectMockServerClient
    MockServerClient mockServerClient;

    @AfterEach
    void resetMocks() {
        mockServerClient.clear(MOCK_ID);
    }

    @Test
    void searchTasksByCriteria() {
        TaskPageResult taskPageResult = new TaskPageResult();
        taskPageResult.setNumber(2);
        taskPageResult.setTotalPages(1L);
        taskPageResult.setSize(1);
        var offsetDateTime = OffsetDateTime.parse("2026-07-29T10:54:06.213702137+01:00");
        List<Task> tasks = new ArrayList<>();
        tasks.add(createTask(taskId, "Title1", "description1", TaskStatus.CREATED, ProviderType.N8N, "1234",
                "http://localhost:8080/n8n", offsetDateTime, "testUser1"));
        tasks.add(createTask(taskId + "2", "Title2", "description2", TaskStatus.ACCEPTED, ProviderType.CAMUNDA, "4321",
                "http://localhost:8080/camunda", offsetDateTime, "testUser2"));
        taskPageResult.setStream(tasks);

        // mock the svc
        mockServerClient
                .when(HttpRequest.request().withPath(HUMAN_TASK_SVC_INTERNAL_API_BASE_PATH + SEARCH_ENDPOINT)
                        .withMethod(HttpMethod.POST))
                .withPriority(100).withId(MOCK_ID)
                .respond(_ -> HttpResponse.response().withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON).withBody(JsonBody.json(taskPageResult)));

        TaskSearchCriteriaDTO taskSearchCriteriaDTO = new TaskSearchCriteriaDTO();
        taskSearchCriteriaDTO.setTitle("Title");
        taskSearchCriteriaDTO.addStatusesItem(TaskStatusDTO.fromValue("CREATED"));
        taskSearchCriteriaDTO.addStatusesItem(TaskStatusDTO.fromValue("ACCEPTED"));

        // bff call
        var response = given().when().auth().oauth2(keycloakClient.getAccessToken(ADMIN)).header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON).body(taskSearchCriteriaDTO).post(SEARCH_ENDPOINT).then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(TaskPageResultDTO.class);

        // Assertions
        Assertions.assertNotNull(response);
        Assertions.assertEquals(2, response.getNumber());
        Assertions.assertEquals(2, response.getStream().size());
    }

    @Test
    void searchTasksByCriteria_shouldReturn400_whenBodyDoesNotExist() {
        ProblemDetailResponse problemDetailResponse = new ProblemDetailResponse().errorCode("400");

        // mock the svc
        mockServerClient
                .when(HttpRequest.request().withPath(HUMAN_TASK_SVC_INTERNAL_API_BASE_PATH + SEARCH_ENDPOINT)
                        .withMethod(HttpMethod.POST))
                .withPriority(100).withId(MOCK_ID)
                .respond(_ -> HttpResponse.response().withStatusCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON).withBody(JsonBody.json(problemDetailResponse)));

        // bff call
        var response = given().when().auth().oauth2(keycloakClient.getAccessToken(ADMIN)).header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON).post(SEARCH_ENDPOINT).then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponse.class);

        // Assertions
        Assertions.assertNotNull(response);
        Assertions.assertEquals(ExceptionMapper.ErrorKeys.CONSTRAINT_VIOLATIONS.name(), response.getErrorCode());
    }

    @Test
    void getTaskById() {
        var offsetDateTime = OffsetDateTime.parse("2026-07-29T10:54:06.213702137+01:00");
        Task task = createTask(taskId, "Title", "description", TaskStatus.CREATED, ProviderType.N8N, "1234",
                "http://localhost:8080/n8n", offsetDateTime, "testUser");

        // mock the svc
        mockServerClient
                .when(HttpRequest.request().withPath(HUMAN_TASK_SVC_INTERNAL_API_BASE_PATH + "/" + taskId)
                        .withMethod(HttpMethod.GET))
                .withPriority(100).withId(MOCK_ID)
                .respond(_ -> HttpResponse.response().withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON).withBody(JsonBody.json(task)));

        // bff call
        var response = given().when().auth().oauth2(keycloakClient.getAccessToken(ADMIN)).header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON).get(taskId).then().statusCode(Response.Status.OK.getStatusCode())
                .extract().as(GetTaskResponseDTO.class);

        // Assertions
        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getResource());
        Assertions.assertEquals(taskId, response.getResource().getId());
        Assertions.assertEquals("Title", response.getResource().getTitle());
        Assertions.assertEquals(ProviderTypeDTO.N8_N, response.getResource().getProviderType());
        Assertions.assertEquals("http://localhost:8080/n8n", response.getResource().getProviderURL());
    }

    @Test
    void getTaskById_shouldReturn404_WhenTaskDoesNotExist() {
        // mock the svc
        mockServerClient
                .when(HttpRequest.request().withPath(HUMAN_TASK_SVC_INTERNAL_API_BASE_PATH + "/" + taskId)
                        .withMethod(HttpMethod.GET))
                .withPriority(100).withId(MOCK_ID)
                .respond(_ -> HttpResponse.response().withStatusCode(Response.Status.NOT_FOUND.getStatusCode()));

        // bff call
        var response = given().when().auth().oauth2(keycloakClient.getAccessToken(ADMIN)).header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON).get(taskId).then().statusCode(Response.Status.NOT_FOUND.getStatusCode());

        // Assertions
        Assertions.assertNotNull(response);
    }

    @Test
    void deleteTaskById() {
        // mock the svc
        mockServerClient
                .when(HttpRequest.request().withPath(HUMAN_TASK_SVC_INTERNAL_API_BASE_PATH + "/" + taskId)
                        .withMethod(HttpMethod.DELETE))
                .withPriority(100).withId(MOCK_ID)
                .respond(_ -> HttpResponse.response().withStatusCode(Response.Status.NO_CONTENT.getStatusCode()));

        // bff call
        var response = given().when().auth().oauth2(keycloakClient.getAccessToken(ADMIN)).header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON).delete(taskId).then().statusCode(Response.Status.NO_CONTENT.getStatusCode())
                .extract().response();

        // Assertions
        Assertions.assertNotNull(response);
    }

    @Test
    void deleteTaskById_shouldReturn404_WhenTaskDoesNotExist() {
        // mock the svc
        mockServerClient
                .when(HttpRequest.request().withPath(HUMAN_TASK_SVC_INTERNAL_API_BASE_PATH + "/" + taskId)
                        .withMethod(HttpMethod.DELETE))
                .withPriority(100).withId(MOCK_ID)
                .respond(_ -> HttpResponse.response().withStatusCode(Response.Status.NOT_FOUND.getStatusCode()));

        // bff call
        var response = given().when().auth().oauth2(keycloakClient.getAccessToken(ADMIN)).header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON).delete(taskId).then().statusCode(Response.Status.NOT_FOUND.getStatusCode());

        // Assertions
        Assertions.assertNotNull(response);
    }

    @Test
    void acceptTask() {
        // mock the svc
        mockServerClient
                .when(HttpRequest.request().withPath(HUMAN_TASK_SVC_INTERNAL_API_BASE_PATH + "/" + taskId + ACCEPT_ENDPOINT)
                        .withMethod(HttpMethod.POST))
                .withPriority(100).withId(MOCK_ID)
                .respond(_ -> HttpResponse.response().withStatusCode(Response.Status.NO_CONTENT.getStatusCode()));

        AcceptTaskRequestDTO acceptTaskRequestDTO = new AcceptTaskRequestDTO().modificationCount(0);

        // bff call
        var response = given().when().auth().oauth2(keycloakClient.getAccessToken(ADMIN)).header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON).body(acceptTaskRequestDTO).post(taskId + ACCEPT_ENDPOINT).then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());

        // Assertions
        Assertions.assertNotNull(response);
    }

    @Test
    void acceptTask_shouldReturn400_whenBodyDoesNotExist() {
        ProblemDetailResponse problemDetailResponse = new ProblemDetailResponse().errorCode("400");

        // mock the svc
        mockServerClient
                .when(HttpRequest.request().withPath(HUMAN_TASK_SVC_INTERNAL_API_BASE_PATH + "/" + taskId + ACCEPT_ENDPOINT)
                        .withMethod(HttpMethod.POST))
                .withPriority(100).withId(MOCK_ID)
                .respond(_ -> HttpResponse.response().withStatusCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON).withBody(JsonBody.json(problemDetailResponse)));

        // bff call
        var response = given().when().auth().oauth2(keycloakClient.getAccessToken(ADMIN)).header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON).post(taskId + ACCEPT_ENDPOINT).then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponse.class);

        // Assertions
        Assertions.assertNotNull(response);
        Assertions.assertEquals(ExceptionMapper.ErrorKeys.CONSTRAINT_VIOLATIONS.name(), response.getErrorCode());
    }

    @Test
    void acceptTask_shouldReturn404_whenTaskDoesNotExistOrAlreadyProcessed() {
        // mock the svc
        mockServerClient
                .when(HttpRequest.request().withPath(HUMAN_TASK_SVC_INTERNAL_API_BASE_PATH + "/" + taskId + ACCEPT_ENDPOINT)
                        .withMethod(HttpMethod.POST))
                .withPriority(100).withId(MOCK_ID)
                .respond(_ -> HttpResponse.response().withStatusCode(Response.Status.NOT_FOUND.getStatusCode()));

        AcceptTaskRequestDTO acceptTaskRequestDTO = new AcceptTaskRequestDTO().modificationCount(0);

        // bff call
        var response = given().when().auth().oauth2(keycloakClient.getAccessToken(ADMIN)).header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON).body(acceptTaskRequestDTO).post(taskId + ACCEPT_ENDPOINT).then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());

        // Assertions
        Assertions.assertNotNull(response);
    }

    @Test
    void declineTask() {
        // mock the svc
        mockServerClient
                .when(HttpRequest.request().withPath(HUMAN_TASK_SVC_INTERNAL_API_BASE_PATH + "/" + taskId + DECLINE_ENDPOINT)
                        .withMethod(HttpMethod.POST))
                .withPriority(100).withId(MOCK_ID)
                .respond(_ -> HttpResponse.response().withStatusCode(Response.Status.NO_CONTENT.getStatusCode()));

        DeclineTaskRequestDTO declineTaskRequestDTO = new DeclineTaskRequestDTO().modificationCount(0);

        // bff call
        var response = given().when().auth().oauth2(keycloakClient.getAccessToken(ADMIN)).header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON).body(declineTaskRequestDTO).post(taskId + DECLINE_ENDPOINT).then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());

        // Assertions
        Assertions.assertNotNull(response);
    }

    @Test
    void declineTask_shouldReturn400_whenBodyDoesNotExist() {
        ProblemDetailResponse problemDetailResponse = new ProblemDetailResponse().errorCode("400");

        // mock the svc
        mockServerClient
                .when(HttpRequest.request().withPath(HUMAN_TASK_SVC_INTERNAL_API_BASE_PATH + "/" + taskId + DECLINE_ENDPOINT)
                        .withMethod(HttpMethod.POST))
                .withPriority(100).withId(MOCK_ID)
                .respond(_ -> HttpResponse.response().withStatusCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON).withBody(JsonBody.json(problemDetailResponse)));

        // bff call
        var response = given().when().auth().oauth2(keycloakClient.getAccessToken(ADMIN)).header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON).post(taskId + DECLINE_ENDPOINT).then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponse.class);

        // Assertions
        Assertions.assertNotNull(response);
        Assertions.assertEquals(ExceptionMapper.ErrorKeys.CONSTRAINT_VIOLATIONS.name(), response.getErrorCode());
    }

    @Test
    void declineTask_shouldReturn404_whenTaskDoesNotExistOrAlreadyProcessed() {
        // mock the svc
        mockServerClient
                .when(HttpRequest.request().withPath(HUMAN_TASK_SVC_INTERNAL_API_BASE_PATH + "/" + taskId + DECLINE_ENDPOINT)
                        .withMethod(HttpMethod.POST))
                .withPriority(100).withId(MOCK_ID)
                .respond(_ -> HttpResponse.response().withStatusCode(Response.Status.NOT_FOUND.getStatusCode()));

        DeclineTaskRequestDTO declineTaskRequestDTO = new DeclineTaskRequestDTO().modificationCount(0);

        // bff call
        var response = given().when().auth().oauth2(keycloakClient.getAccessToken(ADMIN)).header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON).body(declineTaskRequestDTO).post(taskId + DECLINE_ENDPOINT).then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());

        // Assertions
        Assertions.assertNotNull(response);
    }

    /*
     * Helpers ahead!!
     */

    private Task createTask(String taskId, String title, String description, TaskStatus status, ProviderType providerType,
            String providerTaskId, String providerUrl, OffsetDateTime creationDate, String creationUser) {
        Task task = new Task();
        task.setId(taskId);
        task.setTitle(title);
        task.setDescription(description);
        task.setStatus(status);
        task.setProviderType(providerType);
        task.setProviderTaskId(providerTaskId);
        task.setProviderURL(providerUrl);
        task.setCreationDate(creationDate);
        task.setCreationUser(creationUser);
        return task;
    }

}
