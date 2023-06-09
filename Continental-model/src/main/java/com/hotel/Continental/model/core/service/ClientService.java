package com.hotel.continental.model.core.service;

import com.hotel.continental.api.core.service.IClientService;
import com.hotel.continental.model.core.dao.ClientDao;
import com.hotel.continental.model.core.tools.CheckDocument;
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
import java.util.*;

@Lazy
@Service("ClientService")
public class ClientService implements IClientService {
    @Autowired
    private ClientDao clientDao;
    @Autowired
    private DefaultOntimizeDaoHelper daoHelper;

    /**
     * Metodo que actualiza un cliente de la base de datos
     * Updatear por id? o updatear por filtro? ejemplo dni, o id o country code, o todos...
     *
     * @param attrMap Mapa con los campos de la clave
     * @param keyMap  Mapa con los campos de la clave
     * @return EntityResult con el id de los clientes o un mensaje de error
     */
    @Override
    @Secured({ PermissionsProviderSecured.SECURED })
    public EntityResult clientUpdate(Map<String, Object> attrMap, Map<?, ?> keyMap) {
        //Primero compruebo que el clientid existe dado que es necesario para la actualizacion
        if (keyMap.get(ClientDao.CLIENTID) == null) {
            EntityResult er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.NECESSARY_KEY);
            return er;
        }
        //Si el id del cliente no existe en la base de datos esta mal
        if (!existsKeymap(Collections.singletonMap(ClientDao.CLIENTID, keyMap.get(ClientDao.CLIENTID)))) {
            EntityResult er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.CLIENT_NOT_EXIST);
            return er;
        }
        //El check update hace las comprobaciones de los datos a insertar
        EntityResult checkDatos = checkUpdate(attrMap);
        if (checkDatos.getCode() == EntityResult.OPERATION_WRONG) {
            return checkDatos;
        }
        EntityResult er = this.daoHelper.update(this.clientDao, attrMap, keyMap);
        er.setCode(EntityResult.OPERATION_SUCCESSFUL);
        return er;
    }

    @Override
    @Secured({ PermissionsProviderSecured.SECURED })
    public EntityResult clientDelete(Map<?, ?> keyMap) {
        EntityResult er = new EntityResultMapImpl();
        //Comprobar que se envia el id del cliente
        if (keyMap.get(ClientDao.CLIENTID) == null) {
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.NECESSARY_KEY);
        }
        if (!existsKeymap(Collections.singletonMap(ClientDao.CLIENTID, keyMap.get(ClientDao.CLIENTID)))) {
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.CLIENT_NOT_EXIST);
        } else if (isCanceled(keyMap)) {
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.CLIENT_ALREADY_DELETED);
        } else {
            Map<String, Object> attrMap = new HashMap<>();
            attrMap.put(ClientDao.CLIENTDOWNDATE, new Timestamp(System.currentTimeMillis()));
            er = this.daoHelper.update(clientDao, attrMap, keyMap);
            er.setCode(EntityResult.OPERATION_SUCCESSFUL);
            er.setMessage("Este cliente se ha dado de baja con fecha " + new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        }
        return er;
    }


    /**
     * Metodo que devuelve todos los clientes
     *
     * @param attrMap Mapa con los campos de la clave
     * @return EntityResult con los clientes o un mensaje de error
     */
    @Override
    @Secured({ PermissionsProviderSecured.SECURED })
    public EntityResult clientInsert(Map<String, Object> attrMap) {
        //Si alguno de los campos necesarios esta nulo esta mal
        if (attrMap.get(ClientDao.COUNTRYCODE) == null || attrMap.get(ClientDao.NAME) == null || attrMap.get(ClientDao.DOCUMENT) == null) {
            EntityResult er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.NECESSARY_DATA);
            return er;
        }
        //Si alguno de los campos necesarios esta vacio esta mal
        if (((String) attrMap.get(ClientDao.COUNTRYCODE)).isEmpty() || ((String) attrMap.get(ClientDao.NAME)).isEmpty() || ((String) attrMap.get(ClientDao.DOCUMENT)).isEmpty()) {
            EntityResult er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.NECESSARY_DATA);
            return er;
        }
        //El check comprueba que los datos a insertar/Actualizar son correctos
        EntityResult check = checkUpdate(attrMap);
        if (check.getCode() == EntityResult.OPERATION_WRONG) {
            return check;
        }
        EntityResult er = this.daoHelper.insert(this.clientDao, attrMap);
        er.setCode(EntityResult.OPERATION_SUCCESSFUL);
        er.setMessage("Cliente insertado correctamente");
        return er;
    }


    /**
     * Metodo que hace las comprobacioes previas a un insert/update
     *
     * @param attrMap Mapa con los campos de la clave
     * @return EntityResult con OPERATION_SUCCESSFUL o un mensaje de error
     */
    private EntityResult checkUpdate(Map<String, Object> attrMap) {
        //Hago esto para asegurarme de que el codigo de pais esta en mayusculas y que no sea nulo
        if (attrMap.get(ClientDao.COUNTRYCODE) != null) {
            attrMap.put(ClientDao.COUNTRYCODE, ((String) attrMap.remove(ClientDao.COUNTRYCODE)).toUpperCase());
            //Si el country code no mide 2 Caracteres esta mal
            if (((String) attrMap.get(ClientDao.COUNTRYCODE)).length() != 2) {
                EntityResult er = new EntityResultMapImpl();
                er.setCode(EntityResult.OPERATION_WRONG);
                er.setMessage(ErrorMessages.COUNTRY_CODE_FORMAT_ERROR);
                return er;
            }
            //Si el country code no es un codigo de pais valido esta mal
            if (!checkCountryCode(((String) attrMap.get(ClientDao.COUNTRYCODE)))) {
                EntityResult er = new EntityResultMapImpl();
                er.setCode(EntityResult.OPERATION_WRONG);
                er.setMessage(ErrorMessages.COUNTRY_CODE_NOT_VALID);
                return er;
            }
        }
        if (attrMap.get(ClientDao.DOCUMENT) != null) {
            //Si el documento no es valido esta mal
            if (!CheckDocument.checkDocument((String) attrMap.get(ClientDao.DOCUMENT), (String) attrMap.get(ClientDao.COUNTRYCODE))) {
                EntityResult er = new EntityResultMapImpl();
                er.setCode(EntityResult.OPERATION_WRONG);
                er.setMessage(ErrorMessages.DOCUMENT_NOT_VALID);
                return er;
            }
            //Si el documento ya exite en la base de datos esta mal
            if (existsKeymap(Collections.singletonMap(ClientDao.DOCUMENT, attrMap.get(ClientDao.DOCUMENT)))) {
                EntityResult er = new EntityResultMapImpl();
                er.setCode(EntityResult.OPERATION_WRONG);
                er.setMessage(ErrorMessages.CLIENT_ALREADY_EXIST);
                return er;
            }
        }
        EntityResult er = new EntityResultMapImpl();
        er.setCode(EntityResult.OPERATION_SUCCESSFUL);
        return er;
    }

    /**
     * Metodo que comprueba si el codigo de pais es valido
     *
     * @param countryCode Codigo de pais
     * @return true si es valido, false si no lo es
     */
    private boolean checkCountryCode(String countryCode) {
        String[] isoCountryCodes = Locale.getISOCountries();
        return Arrays.stream(isoCountryCodes).anyMatch(countryCode::equals);
    }

    /**
     * Metodo que comprueba si el keyMap ya existe en la base de datos
     *
     * @param keyMap Mapa con los campos de la clave
     * @return true si existe, false si no existe
     */

    private boolean existsKeymap(Map<String, Object> keyMap) {
        List<Object> attrList = new ArrayList<>();
        attrList.add(ClientDao.CLIENTID);
        EntityResult er = this.daoHelper.query(this.clientDao, keyMap, attrList);
        return er.getCode() == EntityResult.OPERATION_SUCCESSFUL && er.calculateRecordNumber() > 0;
    }

    private boolean isCanceled(Map<?, ?> keyMap) {
        List<Object> attrList = new ArrayList<>();
        attrList.add(ClientDao.CLIENTDOWNDATE);
        EntityResult er = this.daoHelper.query(this.clientDao, keyMap, attrList);
        return er.getCode() == EntityResult.OPERATION_SUCCESSFUL && er.calculateRecordNumber() > 0 && er.getRecordValues(0).get(ClientDao.CLIENTDOWNDATE) != null;
    }
    @Override
    @Secured({ PermissionsProviderSecured.SECURED })
    public EntityResult clientQuery(Map<String, Object> keyMap, List<?> attrList) {
        //comprobamos que envio en el filtro un id,si lo envio y no existe el cliente devolvemos error
            EntityResult client = this.daoHelper.query(this.clientDao, keyMap, attrList);
            if(client == null || client.calculateRecordNumber() == 0) {
                EntityResult er = new EntityResultMapImpl();
                er.setCode(EntityResult.OPERATION_WRONG);
                er.setMessage(ErrorMessages.CLIENT_NOT_EXIST);
                return er;
            }
            return client;
    }
}
