package com.hotel.continental.model.core.service;

import com.hotel.continental.api.core.service.IEmployeeService;
import com.hotel.continental.model.core.dao.EmployeeDao;
import com.hotel.continental.model.core.dao.HotelDao;
import com.hotel.continental.model.core.tools.ErrorMessages;
import com.ontimize.jee.common.dto.EntityResult;
import com.ontimize.jee.common.dto.EntityResultMapImpl;
import com.ontimize.jee.common.security.PermissionsProviderSecured;
import com.ontimize.jee.server.dao.DefaultOntimizeDaoHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Lazy
@Service("EmployeeService")
public class EmployeeService implements IEmployeeService {

    @Autowired
    EmployeeDao employeeDao;
    @Autowired
    HotelDao hotelDao;
    @Autowired
    DefaultOntimizeDaoHelper daoHelper;

    @Override
    public EntityResult employeeInsert(Map<?, ?> attrMap) {
        if (!attrMap.containsKey(EmployeeDao.EMPLOYMENT) || !attrMap.containsKey(EmployeeDao.IDHOTEL)
                || attrMap.containsKey(EmployeeDao.DOCUMENT) || !attrMap.containsKey(EmployeeDao.NAME)) {
            EntityResult er = new EntityResultMapImpl();
            er.setCode(1);
            er.setMessage(ErrorMessages.NECESSARY_DATA);
        }

        // Comprueba que el hotel existe

        Map<String, Object> filter = new HashMap<>();
        filter.put(HotelDao.ID, attrMap.get(EmployeeDao.IDHOTEL));
        EntityResult hotel = this.daoHelper.query(this.hotelDao, filter, Arrays.asList(HotelDao.ID));

        if (hotel.calculateRecordNumber() == 0) {
            EntityResult er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.HOTEL_NOT_EXIST);
            return er;
        }
        //Check que no existe el nif
        filter = new HashMap<>();
        filter.put(EmployeeDao.DOCUMENT, attrMap.get(EmployeeDao.DOCUMENT));
        EntityResult nif = this.daoHelper.query(this.employeeDao, filter, Arrays.asList(EmployeeDao.DOCUMENT));

        if (nif.calculateRecordNumber() > 0) {
            EntityResult er = new EntityResultMapImpl();
            er.setCode(1);
            er.setMessage(ErrorMessages.DOCUMENT_ALREADY_EXIST);
            return er;
        }
        return this.daoHelper.insert(this.employeeDao, attrMap);
    }

    @Override
    @Secured({PermissionsProviderSecured.SECURED})
    public EntityResult employeeDelete(Map<?, ?> keyMap) {
        EntityResult er;
        //Comprobamos que nos envia un id
        if (!keyMap.containsKey(EmployeeDao.EMPLOYEEID)) {
            er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.NECESSARY_KEY);
            return er;
        }
        //Comprobamos que el empleado existe
        //Si no existe, devolvemos un entityResult que representa un error
        Map<String, Object> filter = new HashMap<>();
        filter.put(EmployeeDao.EMPLOYEEID, keyMap.get(EmployeeDao.EMPLOYEEID));
        EntityResult employee = this.daoHelper.query(this.employeeDao, filter, Arrays.asList(EmployeeDao.EMPLOYEEID, EmployeeDao.EMPLOYEEDOWNDATE));
        if (employee.calculateRecordNumber() == 0) {
            er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.EMPLOYEE_NOT_EXIST);
            return er;
        }

        //Comprobamos que el empleado esta en activo
        if (employee.getRecordValues(0).get(EmployeeDao.EMPLOYEEDOWNDATE) != null) {
            er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.EMPLOYEE_ALREADY_INACTIVE);
            return er;
        }

        Map<Object, Object> attrMap = new HashMap<>();//Mapa de atributos
        attrMap.put(EmployeeDao.EMPLOYEEDOWNDATE, new Timestamp(System.currentTimeMillis()));//Añadimos la fecha de baja
        //Devolvemos un entityResult que representa el éxito de la operación
        er = this.daoHelper.update(this.employeeDao, attrMap, keyMap);//Actualizamos el empleado
        er.setCode(EntityResult.OPERATION_SUCCESSFUL);
        er.setMessage("Empleado dado de baja correctamente con fecha " + new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        return er;
    }

    public EntityResult employeeUpdate(Map<?, ?> attrMap, Map<?, ?> keyMap) {
        EntityResult er = new EntityResultMapImpl();
        Map<String, Object> filter = new HashMap<>();

        //Comprobamos que nos envia un EmployeeId
        if (!attrMap.containsKey(EmployeeDao.EMPLOYEEID)) {
            er.setCode(1);
            er.setMessage(ErrorMessages.NECESSARY_KEY);
        }

        // Comprueba que el hotel existe
        if (attrMap.containsKey(EmployeeDao.IDHOTEL)) {
            filter.put(HotelDao.ID, attrMap.get(EmployeeDao.IDHOTEL));
            EntityResult hotel = this.daoHelper.query(this.hotelDao, filter, Arrays.asList(HotelDao.ID));
            if (hotel.calculateRecordNumber() == 0) {
                er = new EntityResultMapImpl();
                er.setCode(EntityResult.OPERATION_WRONG);
                er.setMessage(ErrorMessages.HOTEL_NOT_EXIST);
                return er;
            }
        }

        //Comprobamos que el nif no existe
        filter.put(EmployeeDao.DOCUMENT, attrMap.get(EmployeeDao.DOCUMENT));
        EntityResult employee = this.daoHelper.query(this.employeeDao, filter, Arrays.asList(EmployeeDao.DOCUMENT, EmployeeDao.EMPLOYEEDOWNDATE));
        if (attrMap.containsKey(EmployeeDao.DOCUMENT)) {
            filter = new HashMap<>();

            if (employee.calculateRecordNumber() > 0) {
                er = new EntityResultMapImpl();
                er.setCode(1);
                er.setMessage(ErrorMessages.DOCUMENT_ALREADY_EXIST);
                return er;
            }
        }
        
        //Check de que no esta dado de baja
        if (employee.getRecordValues(0).get(EmployeeDao.EMPLOYEEDOWNDATE) != null) {
            er = new EntityResultMapImpl();
            er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.EMPLOYEE_ALREADY_INACTIVE);
            return er;
        }
        return this.daoHelper.insert(this.employeeDao, attrMap);
    }
}

