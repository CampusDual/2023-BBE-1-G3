package com.hotel.continental.ws.core.rest;

import com.hotel.continental.api.core.service.IProductsService;
import com.ontimize.jee.server.rest.ORestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductRestController extends ORestController<IProductsService> {
    @Autowired
    private IProductsService productsService;

    @Override
    public IProductsService getService() {
        return this.productsService;
    }
}