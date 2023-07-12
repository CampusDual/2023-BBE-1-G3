package com.hotel.continental.ws.core.rest;

import com.hotel.continental.api.core.service.IRoomTypeService;
import com.ontimize.jee.server.rest.ORestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/roomtype")
public class RoomTypeRestController extends ORestController<IRoomTypeService> {
    @Autowired
    private IRoomTypeService roomtypeSrv;

    @Override
    public IRoomTypeService getService() {
        return this.roomtypeSrv;
    }

}
