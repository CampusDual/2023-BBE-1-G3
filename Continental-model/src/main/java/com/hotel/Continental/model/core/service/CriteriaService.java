package com.hotel.continental.model.core.service;


import com.hotel.continental.api.core.service.ICriteriaService;
import com.hotel.continental.model.core.dao.CriteriaDao;
import com.hotel.continental.model.core.tools.ErrorMessages;
import com.ontimize.jee.common.dto.EntityResult;
import com.ontimize.jee.common.dto.EntityResultMapImpl;
import com.ontimize.jee.common.security.PermissionsProviderSecured;
import com.ontimize.jee.server.dao.DefaultOntimizeDaoHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

@Lazy
@Service("CriteriaService")
public class CriteriaService implements ICriteriaService {
    @Autowired
    private DefaultOntimizeDaoHelper daoHelper;
    @Autowired
    private CriteriaDao criteriaDao;
    /**
     * Metodo que devuelve un EntityResult con los datos del criterio
     *
     * @param keyMap   Mapa de claves que identifican el criterio
     * @param attrList Lista de atributos que se quieren obtener
     * @return EntityResult con los datos del criterio o un mensaje de error
     */
    @Override
    @Secured({PermissionsProviderSecured.SECURED})
    public EntityResult criteriaQuery(Map<?, ?> keyMap, List<String> attrList) {
        //si el attr tiene 1 asterisco devolver todos los atributos
        if (attrList.size() == 1 && attrList.get(0).equals("*")) {
            Field[] fields = CriteriaDao.class.getDeclaredFields();
            attrList.clear();
            for (Field field : fields) {
                attrList.add(field.getName().toLowerCase());
            }
        }
        EntityResult er = this.daoHelper.query(this.criteriaDao, keyMap, attrList);
        //Si hay filtro y no hay datos devolver error
        if (!keyMap.isEmpty() && er.calculateRecordNumber() == 0) {
            er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.CRITERIA_NOT_EXIST);
        }
        return er;
    }

    @Override
    public EntityResult criteriaUpdate(Map<String, Object> attrMap, Map<?, ?> keyMap) {
        EntityResult er = new EntityResultMapImpl();
        er.setCode(EntityResult.OPERATION_WRONG);

        //Compruebo que tenga la clave
        if(keyMap.isEmpty() || keyMap.get(CriteriaDao.ID) == null) {
            er.setMessage(ErrorMessages.NECESSARY_KEY);
            return er;
        }
        //Compruebo que el attrMap no este vacio
        if(attrMap.isEmpty()) {
            er.setMessage(ErrorMessages.NECESSARY_DATA);
            return er;
        }
        //Compruebo que el criterio exista
        EntityResult criteriaQuery = this.daoHelper.query(this.criteriaDao, keyMap, List.of(CriteriaDao.ID));
        if(criteriaQuery.calculateRecordNumber() == 0) {
            er.setMessage(ErrorMessages.CRITERIA_NOT_EXIST);
            return er;
        }

        //Comprobar formato correcto en multiplicador
        if(attrMap.get(CriteriaDao.MULTIPLIER) != null) {
            try {
                double multiplier = Double.parseDouble(attrMap.get(CriteriaDao.MULTIPLIER).toString());
                if(multiplier <= 0) {
                    er.setMessage(ErrorMessages.MULTIPLIER_NOT_POSITIVE);
                    return er;
                }
            } catch (NumberFormatException e) {
                er.setMessage(ErrorMessages.MULTIPLIER_NOT_NUMBER);
                return er;
            }
        }

        return this.daoHelper.update(this.criteriaDao, attrMap, keyMap);
    }
}
