package com.hotel.continental.api.core.service;

import com.ontimize.jee.common.dto.EntityResult;
import com.ontimize.jee.common.exceptions.OntimizeJEERuntimeException;

import java.util.List;
import java.util.Map;

public interface IRoomService {
    public EntityResult roomQuery(Map<?, ?> keyMap, List<?> attrList);
    public EntityResult roomInsert(Map<?, ?> attrMap);
    public EntityResult roomDelete(Map<?, ?> keyMap);
    public EntityResult freeRoomsQuery(Map<String, Object> keyMap, List<String> attrList) throws OntimizeJEERuntimeException;
    public EntityResult roomUpdate(Map<?, ?> attrMap, Map<?, ?> keyMap);
}
