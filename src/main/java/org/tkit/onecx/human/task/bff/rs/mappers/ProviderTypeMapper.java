package org.tkit.onecx.human.task.bff.rs.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.ValueMapping;
import org.mapstruct.ValueMappings;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import gen.org.tkit.onecx.human.task.bff.rs.internal.model.ProviderTypeDTO;
import gen.org.tkit.onecx.human.task.client.model.ProviderType;

@Mapper(uses = { OffsetDateTimeMapper.class })
public interface ProviderTypeMapper {
    @ValueMappings({
            @ValueMapping(source = "N8_N", target = "N8N")
    })
    ProviderType toBackend(ProviderTypeDTO source);

    @ValueMappings({
            @ValueMapping(source = "N8N", target = "N8_N")
    })
    ProviderTypeDTO toFrontend(ProviderType source);
}
