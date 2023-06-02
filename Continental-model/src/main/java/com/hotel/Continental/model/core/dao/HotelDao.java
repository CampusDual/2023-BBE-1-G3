package com.hotel.Continental.model.core.dao;

import com.ontimize.jee.server.dao.common.ConfigurationFile;
import com.ontimize.jee.server.dao.jdbc.OntimizeJdbcDaoSupport;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

@Lazy
@Repository(value = "HotelDao")
@ConfigurationFile(
        configurationFile = "dao/HotelDao.xml",
        configurationFilePlaceholder = "dao/placeholders.properties")
public class HotelDao extends OntimizeJdbcDaoSupport {
    public static final String ID = "id";
    public static final String ADDRESS = "direccion";
    public static final String NAME = "nombre";
    public static final String HOTELDOWNDATE = "hoteldowndate";
}