package com.hotel.continental.api.core.service;

import com.ontimize.jee.common.dto.EntityResult;

import java.util.Map;


public interface IRoomTypeService {
    public EntityResult roomtypeUpdate(Map<?, ?> attrMap, Map<?, ?> keyMap);
}