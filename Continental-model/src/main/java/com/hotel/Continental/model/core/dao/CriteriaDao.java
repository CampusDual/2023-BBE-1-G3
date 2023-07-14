package com.hotel.continental.model.core.dao;


import com.ontimize.jee.server.dao.common.ConfigurationFile;
import com.ontimize.jee.server.dao.jdbc.OntimizeJdbcDaoSupport;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

@Lazy
@Repository(value = "CriteriaDao")
@ConfigurationFile(
        configurationFile = "dao/CriteriaDao.xml",
        configurationFilePlaceholder = "dao/placeholders.properties")
public class CriteriaDao extends OntimizeJdbcDaoSupport {
    public static final String ID="id";

    public static final String NAME="name";
    public static final String DESCRIPTION="description";
    public static final String TYPE="type";
    public static final String DATE_CONDITION="date_condition";
    public static final String MULTIPLIER="multiplier";
}
