package com.hotel.continental.ws.core.rest;

import com.hotel.continental.api.core.service.IParkingService;
import com.ontimize.jee.common.dto.EntityResult;
import com.ontimize.jee.server.rest.ORestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/parking")
public class ParkingRestController extends ORestController<IParkingService> {
    @Autowired
    private IParkingService parkingSrv;

    @Override
    public IParkingService getService() {
        return this.parkingSrv;
    }
    @PostMapping(value = "/parkingEnter", produces = MediaType.APPLICATION_JSON_VALUE)
    public EntityResult parkingEnter(@RequestBody Map<String, Object> req) {
        Map<String, Object> attr = ( Map<String, Object>) req.get("data");
        return this.getService().parkingEnter(attr);
    }
}
