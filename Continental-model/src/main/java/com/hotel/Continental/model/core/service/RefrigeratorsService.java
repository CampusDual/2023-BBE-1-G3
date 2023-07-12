package com.hotel.continental.model.core.service;

import com.hotel.continental.api.core.service.IRefrigeratorsService;
import com.hotel.continental.model.core.dao.RefrigeratorsDao;
import com.hotel.continental.model.core.dao.RoomDao;
import com.hotel.continental.model.core.tools.ErrorMessages;
import com.ontimize.jee.common.dto.EntityResult;
import com.ontimize.jee.common.dto.EntityResultMapImpl;
import com.ontimize.jee.server.dao.DefaultOntimizeDaoHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Lazy
@Service("RefrigeratorsService")
public class RefrigeratorsService implements IRefrigeratorsService {
    @Autowired
    RefrigeratorsDao refrigeratorsDao;
    @Autowired
    RoomDao roomDao;
    @Autowired
    DefaultOntimizeDaoHelper daoHelper;

    @Override
    public EntityResult refrigeratorsInsert(Map<?, ?> attrMap) {
        //Compruebo que me envia una capacidad y un room_id
        if (attrMap.get(RefrigeratorsDao.ROOM_ID) == null || attrMap.get(RefrigeratorsDao.CAPACITY) == null) {
            EntityResult erError = new EntityResultMapImpl();
            erError.setCode(EntityResult.OPERATION_WRONG);
            erError.setMessage(ErrorMessages.NECESSARY_DATA);
            return erError;
        }
        //Compruebo que la capacidad es un numero
        try {
            Integer.parseInt(attrMap.get(RefrigeratorsDao.CAPACITY).toString());
        } catch (NumberFormatException e) {
            EntityResult erError = new EntityResultMapImpl();
            erError.setCode(EntityResult.OPERATION_WRONG);
            erError.setMessage(ErrorMessages.CAPACITY_NOT_NUMBER);
            return erError;
        }
        //Compruebo que la capacidad es mayor que 0
        if (Integer.parseInt(attrMap.get(RefrigeratorsDao.CAPACITY).toString()) <= 0) {
            EntityResult erError = new EntityResultMapImpl();
            erError.setCode(EntityResult.OPERATION_WRONG);
            erError.setMessage(ErrorMessages.CAPACITY_NOT_POSITIVE);
            return erError;
        }
        Map<String, Object> filter = new HashMap<>();
        filter.put(RoomDao.IDHABITACION, attrMap.get(RefrigeratorsDao.ROOM_ID));
        EntityResult rooms = this.daoHelper.query(this.roomDao, filter, List.of(RoomDao.IDHABITACION));

        if (rooms.calculateRecordNumber()==0) {
            EntityResult er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.ROOM_NOT_EXIST);
            return er;
        }

        return this.daoHelper.insert(this.refrigeratorsDao, attrMap);
    }
}
