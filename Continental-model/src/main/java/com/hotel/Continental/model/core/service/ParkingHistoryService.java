package com.hotel.continental.model.core.service;

import com.hotel.continental.api.core.service.IParkingHistoryService;
import com.hotel.continental.model.core.dao.ParkingHistoryDao;
import com.ontimize.jee.common.dto.EntityResult;
import com.ontimize.jee.server.dao.DefaultOntimizeDaoHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Map;

@Lazy
@Service("ParkingHistoryService")
public class ParkingHistoryService implements IParkingHistoryService {
    @Autowired
    private DefaultOntimizeDaoHelper daoHelper;
    @Autowired
    private ParkingHistoryDao parkingHistoryDao;

    /**
     * Inserto de un registro en la tabla parking_history con datos id_parking id_booking,entry_date es el dia que entra
     * @param attrMap
     * @return
     */
    @Override
    public EntityResult parkingHistoryInsert(Map<String, Object> attrMap) {
        //Solo va ser usada desde el servicio de parking
        //Insertamos en la tabla parking_history con los datos id_parking id_booking,entry_date es el dia que entra
        attrMap.put(ParkingHistoryDao.ENTRY_DATE, new java.sql.Date(System.currentTimeMillis()));
        return this.daoHelper.insert(parkingHistoryDao, attrMap);
    }

}
