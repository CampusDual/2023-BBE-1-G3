package com.hotel.continental.ws.core.rest;

import com.hotel.continental.api.core.service.IParkingHistoryService;
import com.ontimize.jee.server.rest.ORestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/parkingHistory")
public class ParkingHistoryRestController extends ORestController<IParkingHistoryService> {
    @Autowired
    private IParkingHistoryService parkingHistorySrv;

    @Override
    public IParkingHistoryService getService() {
        return this.parkingHistorySrv;
    }

}
