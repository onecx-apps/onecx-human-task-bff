package org.tkit.onecx.human.task.bff.rs.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import gen.org.tkit.onecx.human.task.bff.rs.internal.model.AcceptTaskRequestDTO;
import gen.org.tkit.onecx.human.task.bff.rs.internal.model.DeclineTaskRequestDTO;
import gen.org.tkit.onecx.human.task.bff.rs.internal.model.GetTaskResponseDTO;
import gen.org.tkit.onecx.human.task.bff.rs.internal.model.TaskDTO;
import gen.org.tkit.onecx.human.task.bff.rs.internal.model.TaskPageResultDTO;
import gen.org.tkit.onecx.human.task.bff.rs.internal.model.TaskSearchCriteriaDTO;
import gen.org.tkit.onecx.human.task.client.model.AcceptTaskRequest;
import gen.org.tkit.onecx.human.task.client.model.DeclineTaskRequest;
import gen.org.tkit.onecx.human.task.client.model.Task;
import gen.org.tkit.onecx.human.task.client.model.TaskPageResult;
import gen.org.tkit.onecx.human.task.client.model.TaskSearchCriteria;

@Mapper(uses = { OffsetDateTimeMapper.class, ProviderTypeMapper.class })
public interface TaskMapper {
    TaskSearchCriteria toTaskSearchCriteria(TaskSearchCriteriaDTO taskSearchCriteriaDTO);

    @Mapping(target = "removeStreamItem", ignore = true)
    TaskPageResultDTO toTaskPageResultDTO(TaskPageResult taskPageResult);

    @Mapping(target = "resource", source = ".")
    GetTaskResponseDTO toGetTaskResponseDTO(Task task);

    AcceptTaskRequest toAcceptTaskRequest(AcceptTaskRequestDTO acceptTaskRequestDTO);

    DeclineTaskRequest toDeclineTaskRequest(DeclineTaskRequestDTO declineTaskRequestDTO);

    @Mapping(target = "removeCustomInputItem", ignore = true)
    TaskDTO toTaskDTO(Task task);
}
