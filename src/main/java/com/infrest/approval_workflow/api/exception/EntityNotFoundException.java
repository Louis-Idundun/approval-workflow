package com.infrest.approval_workflow.api.exception;

public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String entityName, Long id) {
        super("%s with id %d not found".formatted(entityName, id));
    }

    public EntityNotFoundException(String message) {
        super(message);
    }
}
