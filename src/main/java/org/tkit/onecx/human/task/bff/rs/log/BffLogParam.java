package org.tkit.onecx.human.task.bff.rs.log;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import org.tkit.quarkus.log.cdi.LogParam;

import gen.org.tkit.onecx.human.task.bff.rs.internal.model.*;

@ApplicationScoped
public class BffLogParam implements LogParam {

    @Override
    public List<Item> getClasses() {
        return List.of(
                item(10, TaskSearchCriteriaDTO.class, x -> {
                    TaskSearchCriteriaDTO d = (TaskSearchCriteriaDTO) x;
                    return TaskSearchCriteriaDTO.class.getSimpleName() + "[" + d.getPageNumber() + ","
                            + d.getPageSize()
                            + "]";
                }));
    }
}