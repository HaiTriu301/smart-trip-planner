package com.trieu.tripplanner.exception;

import com.trieu.tripplanner.common.constant.ErrorCode;

public class ResourceNotFoundException extends AppException {

    public  ResourceNotFoundException(String resourceName, Object id){
        super(ErrorCode.RESOURCE_NOT_FOUND, "%s not found with id %s".formatted(resourceName, id));
    }
}
