package com.hotel.continental.model.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.continental.api.core.service.IBookingService;
import com.hotel.continental.model.core.dao.*;
import com.hotel.continental.model.core.tools.DateUtils.DateCondition;
import com.hotel.continental.model.core.tools.DateUtils.DateConditionModule;
import com.hotel.continental.model.core.tools.ErrorMessages;
import com.ontimize.jee.common.db.SQLStatementBuilder;
import com.ontimize.jee.common.db.SQLStatementBuilder.BasicExpression;
import com.ontimize.jee.common.db.SQLStatementBuilder.BasicField;
import com.ontimize.jee.common.db.SQLStatementBuilder.BasicOperator;
import com.ontimize.jee.common.dto.EntityResult;
import com.ontimize.jee.common.dto.EntityResultMapImpl;
import com.ontimize.jee.common.security.PermissionsProviderSecured;
import com.ontimize.jee.server.dao.DefaultOntimizeDaoHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Lazy
@Service("BookingService")
public class BookingService implements IBookingService {

    @Autowired
    private BookingDao bookingDao;
    @Autowired
    private RoomService roomService;
    @Autowired
    private AccessCardAssignmentService accessCardAssignmentService;
    @Autowired
    private AccessCardAssignmentDao accessCardAssignmentDao;
    @Autowired
    private DefaultOntimizeDaoHelper daoHelper;
    @Autowired
    private RoomTypeDao roomTypeDao;
    @Autowired
    private RoomDao roomDao;
    @Autowired
    private CriteriaDao criteriaDao;
    @Autowired
    private ExtraExpensesDao extraExpensesDao;
    @Autowired
    private CriteriaService criteriaService;
    public static final String INITIALDATE = "initialdate";
    public static final String FINALDATE = "finaldate";

    /**
     * Metodo que devuelve las reservas
     *
     * @param keyMap   Mapa con los campos de la clave
     * @param attrList Lista de atributos que se quieren devolver
     * @return EntityResult con las reservas o un mensaje de error
     */
    @Override
    @Secured({PermissionsProviderSecured.SECURED})
    public EntityResult bookingQuery(Map<?, ?> keyMap, List<?> attrList) {
        EntityResult result = this.daoHelper.query(this.bookingDao, keyMap, attrList);
        if (result == null || result.calculateRecordNumber() == 0) {
            EntityResult er;
            er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.BOOKING_NOT_EXIST);
            return er;
        }
        return result;
    }

    /**
     * Metodo que inserta una reserva
     *
     * @param attrMap Mapa con los campos de la reserva
     * @return EntityResult con la reserva insertada o un mensaje de error
     */
    @Override
    @Secured({PermissionsProviderSecured.SECURED})
    public EntityResult bookingInsert(Map<String, Object> attrMap) {
        //Comprobar que se han introducido los datos necesarios
        //Primero comprobamos que nos envian startdate, enddate y client

        if (attrMap.get(BookingDao.STARTDATE) == null || attrMap.get(BookingDao.ENDDATE) == null ||
                attrMap.get(BookingDao.CLIENT) == null) {
            EntityResult er;
            er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.NECESSARY_DATA);
            return er;
        }
        //Comprobar si la habitacion esta libre usando la fecha de inicio y fin de la reserva el id de habitacion
        //Si esta libre se inserta
        //Si no esta libre se devuelve un error
        List<String> roomKeyMap = new ArrayList<>();
        roomKeyMap.add(RoomDao.IDHABITACION);
        Map<String, Object> roomAttrMap = new HashMap<>();
        roomAttrMap.put(INITIALDATE, attrMap.get(BookingDao.STARTDATE));
        roomAttrMap.put(FINALDATE, attrMap.get(BookingDao.ENDDATE));
        //Si se nos envia el id de la habitacion se busca esa habitacion

        if (attrMap.get(BookingDao.ROOMID) != null) {
            roomAttrMap.put(BookingDao.ROOMID, attrMap.get(RoomDao.IDHABITACION));
        }
        if (attrMap.get(RoomDao.ROOMTYPEID) != null) {
            roomAttrMap.put(RoomDao.ROOMTYPEID, attrMap.get(RoomDao.ROOMTYPEID));
        }
        EntityResult habitacionesLibres = roomService.freeRoomsQuery(roomAttrMap, roomKeyMap);//Todas las habitaciones libres entre esas dos fechas
        //Comprobar que no dio error
        if (habitacionesLibres.getCode() == EntityResult.OPERATION_WRONG) {
            return habitacionesLibres;
        }
        //Si hay habitaciones libres se busca si esa habitacion esta libre en esas fechas
        //Si esta libre se inserta
        //Si no esta libre se devuelve un error
        if (habitacionesLibres.calculateRecordNumber() > 0) {
            //Buscamos si la habitacion esta libre en esas fechas
            Map<String, Object> room = habitacionesLibres.getRecordValues(0);
            attrMap.put(BookingDao.ROOMID, room.get(RoomDao.IDHABITACION));
            //Calculamos el precio de la reserva
            EntityResult price = bookingPrice(attrMap);
            attrMap.put(BookingDao.PRICE, price.get(BookingDao.PRICE));
            EntityResult erResultlado = this.daoHelper.insert(this.bookingDao, attrMap);
            erResultlado.put(BookingDao.PRICE, price.get(BookingDao.PRICE));
            return erResultlado;
        }
        EntityResult er = new EntityResultMapImpl();
        er.setCode(EntityResult.OPERATION_WRONG);
        er.setMessage(ErrorMessages.ROOM_NOT_FREE);
        return er;

    }

    /**
     * Metodo que borra una reserva
     *
     * @param keyMap Mapa con los campos de la clave
     * @return EntityResult con la reserva borrada o un mensaje de error
     */
    @Override
    @Secured({PermissionsProviderSecured.SECURED})
    public EntityResult bookingDelete(Map<?, ?> keyMap) {
        //Comprobamos si se nos envia el id
        if (keyMap.get(BookingDao.BOOKINGID) == null) {
            EntityResult er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.NECESSARY_KEY);
            return er;
        }
        //Comprobamos si la reserva existe
        EntityResult book = this.daoHelper.query(this.bookingDao, keyMap, List.of(BookingDao.BOOKINGID));
        if (book == null || book.calculateRecordNumber() == 0) {
            EntityResult er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.BOOKING_NOT_EXIST);
            return er;
        }
        return this.daoHelper.delete(this.bookingDao, keyMap);
    }

    /**
     * Metodo que actualiza una reserva
     *
     * @param attrMap Mapa con los campos a actualizar
     * @param keyMap  Mapa con los campos de la clave
     * @return EntityResult con la reserva actualizada o un mensaje de error
     */
    @Override
    @Secured({PermissionsProviderSecured.SECURED})
    public EntityResult bookingUpdate(Map<String, Object> attrMap, Map<?, ?> keyMap) {
        //Comprobamos que se ha introducido el id de la reserva
        if (keyMap.get(BookingDao.BOOKINGID) == null) {
            EntityResult er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.NECESSARY_KEY);
            return er;
        }
        //Comprobamos si la reserva existe
        EntityResult book = this.daoHelper.query(this.bookingDao, keyMap, List.of(BookingDao.BOOKINGID));
        if (book == null || book.getCode() == EntityResult.OPERATION_WRONG) {
            EntityResult er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.BOOKING_NOT_EXIST);
            return er;
        }
        //Si se envia alguna fecha comprobamos que se envian ambas
        if (attrMap.get(BookingDao.STARTDATE) != null || attrMap.get(BookingDao.ENDDATE) != null) {
            if (attrMap.get(BookingDao.STARTDATE) == null || attrMap.get(BookingDao.ENDDATE) == null) {
                EntityResult er = new EntityResultMapImpl();
                er.setCode(EntityResult.OPERATION_WRONG);
                er.setMessage(ErrorMessages.NECESSARY_DATA);
                return er;
            }
            //Comprobamos si la habitacion esta libre usando la fecha de inicio y fin de la reserva el id de habitacion
            //Si esta libre se actualiza
            //Si no esta libre se devuelve un error
            List<String> roomKeyMap = new ArrayList<>();
            roomKeyMap.add(RoomDao.IDHABITACION);
            Map<String, Object> roomAttrMap = new HashMap<>();
            roomAttrMap.put(INITIALDATE, attrMap.get(BookingDao.STARTDATE));
            roomAttrMap.put(FINALDATE, attrMap.get(BookingDao.ENDDATE));
            //Si se nos envia el id de la habitacion se busca esa habitacion
            if (attrMap.get(BookingDao.ROOMID) != null) {
                roomAttrMap.put(BookingDao.ROOMID, attrMap.get(RoomDao.IDHABITACION));
            }
            if (attrMap.get(RoomDao.ROOMTYPEID) != null) {
                roomAttrMap.put(RoomDao.ROOMTYPEID, attrMap.get(RoomDao.ROOMTYPEID));
            }
            EntityResult habitacionesLibres = roomService.freeRoomsQuery(roomAttrMap, roomKeyMap);//Todas las habitaciones libres entre esas dos fechas
            //Comprobar que no dio error
            if (habitacionesLibres.getCode() == EntityResult.OPERATION_WRONG) {
                return habitacionesLibres;
            }
            //Si hay habitaciones libres se busca si esa habitacion esta libre en esas fechas
            //Si esta libre se actualiza
            //Si no esta libre se devuelve un error
            if (habitacionesLibres.calculateRecordNumber() == 0) {
                EntityResult er = new EntityResultMapImpl();
                er.setCode(EntityResult.OPERATION_WRONG);
                er.setMessage(ErrorMessages.ROOM_NOT_FREE);
                return er;
            }
            //Buscamos si la habitacion esta libre en esas fechas
            Map<String, Object> room = habitacionesLibres.getRecordValues(0);
            attrMap.put(BookingDao.ROOMID, room.get(RoomDao.IDHABITACION));
            attrMap.remove(BookingDao.STARTDATE);
            attrMap.remove(BookingDao.ENDDATE);
            attrMap.put(BookingDao.STARTDATE, roomAttrMap.get(INITIALDATE));
            attrMap.put(BookingDao.ENDDATE, roomAttrMap.get(FINALDATE));
            EntityResult price = bookingPrice(attrMap);
            attrMap.put(BookingDao.PRICE, price.get(BookingDao.PRICE));
        }
        return this.daoHelper.update(this.bookingDao, attrMap, keyMap);
    }

    /**
     * Metodo que realiza el checkin de una reserva
     *
     * @param attrMap
     * @return EntityResult
     */
    @Override
    @Secured({PermissionsProviderSecured.SECURED})
    public EntityResult bookingCheckin(Map<String, Object> attrMap) {
        //Comprobamos que se ha introducido el id de la reserva
        if (attrMap.get(BookingDao.BOOKINGID) == null) {
            //Comprobamos que se ha introducido el id de cliente
            EntityResult er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.NECESSARY_KEY);
            return er;
        }
        if (attrMap.get(AccessCardAssignmentDao.ACCESSCARDID) == null) {
            EntityResult er = new EntityResultMapImpl();
            er.setCode(1);
            er.setMessage(ErrorMessages.NECESSARY_DATA);
            return er;
        }
        //Comprobamos si la reserva existe
        Map<String, Object> filterId = new HashMap<>();
        filterId.put(BookingDao.BOOKINGID, attrMap.get(BookingDao.BOOKINGID));
        EntityResult book = this.daoHelper.query(this.bookingDao, filterId, List.of(BookingDao.BOOKINGID, BookingDao.CHECKIN_DATETIME));
        if (book.getRecordValues(0).isEmpty() || book.getCode() == EntityResult.OPERATION_WRONG) {
            EntityResult er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.BOOKING_NOT_EXIST);
            return er;
        }
        //Comprobamos que la reserva pertenece al cliente
        Map<String, Object> filter = new HashMap<>();
        filter.put(BookingDao.BOOKINGID, attrMap.get(BookingDao.BOOKINGID));
        filter.put(BookingDao.CLIENT, attrMap.get(BookingDao.CLIENT));
        EntityResult bookclient = this.daoHelper.query(this.bookingDao, filter, List.of(BookingDao.BOOKINGID, BookingDao.CLIENT));
        if (bookclient.calculateRecordNumber() == 0) {
            EntityResult er = new EntityResultMapImpl();
            er.setCode(1);
            er.setMessage(ErrorMessages.BOOKING_DOESNT_BELONG_CLIENT);
            return er;
        }
        //Comprobamos que no se ha hecho el checkin
        if (book.getRecordValues(0).get(BookingDao.CHECKIN_DATETIME) != null) {
            EntityResult er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.BOOKING_ALREADY_CHECKED_IN);
            return er;
        }
        EntityResult card = accessCardAssignmentService.accesscardassignmentInsert(attrMap);
        if (card.getCode() == 1) {
            return card;
        }

        //Update de la reserva
        Map<String, Object> keyMap = new HashMap<>();
        keyMap.put(BookingDao.BOOKINGID, attrMap.get(BookingDao.BOOKINGID));
        Map<String, Object> attrMapUpdate = new HashMap<>();
        attrMapUpdate.put(BookingDao.CHECKIN_DATETIME, LocalDateTime.now());
        EntityResult er = this.daoHelper.update(this.bookingDao, attrMapUpdate, keyMap);
        er.setMessage(ErrorMessages.BOOKING_CHECK_IN_SUCCESS);
        return er;
    }

    /**
     * Metodo que realiza el checkout de una reserva
     *
     * @param attrMap
     * @return EntityResult
     */
    @Override
    @Secured({PermissionsProviderSecured.SECURED})
    public EntityResult bookingCheckout(Map<String, Object> attrMap) {
        //Comprobamos que se ha introducido el id de la reserva
        //Si se introduce id de reserva se usa id de reserva
        if (attrMap.get(BookingDao.BOOKINGID) == null) {
            EntityResult er = new EntityResultMapImpl();
            er.setCode(1);
            er.setMessage(ErrorMessages.NECESSARY_KEY);
            return er;
        }
        //Comprobamos si la reserva existe
        Map<String, Object> filterId = new HashMap<>();
        filterId.put(BookingDao.BOOKINGID, attrMap.get(BookingDao.BOOKINGID));
        EntityResult book = this.daoHelper.query(this.bookingDao, filterId, List.of(BookingDao.BOOKINGID, BookingDao.CHECKIN_DATETIME, BookingDao.CHECKOUT_DATETIME));
        if (book.getRecordValues(0).isEmpty() || book.getCode() == EntityResult.OPERATION_WRONG) {
            EntityResult er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.BOOKING_NOT_EXIST);
            return er;
        }

        //Comprobamos que se haya hecho el checkin
        if (book.getRecordValues(0).get(BookingDao.CHECKIN_DATETIME) == null) {
            EntityResult er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.BOOKING_NOT_CHECKED_IN);
            return er;
        }
        //Comprobamos que no se ha hecho el checkout
        if (book.getRecordValues(0).get(BookingDao.CHECKOUT_DATETIME) != null) {
            EntityResult er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.BOOKING_ALREADY_CHECKED_OUT);
            return er;
        }

        //Comprobamos que la reserva pertenece al cliente
        Map<String, Object> filter = new HashMap<>();
        filter.put(BookingDao.BOOKINGID, attrMap.get(BookingDao.BOOKINGID));
        filter.put(BookingDao.CLIENT, attrMap.get(BookingDao.CLIENT));
        EntityResult bookclient = this.daoHelper.query(this.bookingDao, filter, List.of(BookingDao.BOOKINGID, BookingDao.CLIENT));
        if (bookclient.calculateRecordNumber() == 0) {
            EntityResult er = new EntityResultMapImpl();
            er.setCode(1);
            er.setMessage(ErrorMessages.BOOKING_DOESNT_BELONG_CLIENT);
            return er;
        }
        //Update de la tarjeta
        Map<String, Object> keyMapCard = new HashMap<>();
        keyMapCard.put(AccessCardDao.ACCESSCARDID, attrMap.get(AccessCardDao.ACCESSCARDID));
        keyMapCard.put(BookingDao.BOOKINGID, attrMap.get(BookingDao.BOOKINGID));
        EntityResult erTarjeta = accessCardAssignmentService.accesscardassignmentRecover(keyMapCard);
        if (erTarjeta.getCode() == EntityResult.OPERATION_WRONG) {
            return erTarjeta;
        }
        EntityResult finalPrice = obtainFinalPrice(attrMap);
        //Update de la reserva
        Map<String, Object> keyMap = new HashMap<>();
        keyMap.put(BookingDao.BOOKINGID, attrMap.get(BookingDao.BOOKINGID));
        Map<String, Object> attrMapUpdate = new HashMap<>();
        attrMapUpdate.put(BookingDao.CHECKOUT_DATETIME, LocalDateTime.now());
        this.daoHelper.update(this.bookingDao, attrMapUpdate, keyMap);
        finalPrice.setMessage(ErrorMessages.BOOKING_CHECK_OUT_SUCCESS);
        return finalPrice;
    }

    /**
     * Metodo que realiza el calculo del precio de una reserva
     *
     * @param attrMap fecha de inicio y fin de la reserva y el id de la habitacion
     * @return EntityResult con el precio de la reserva
     */
    @Override
    @Secured({PermissionsProviderSecured.SECURED})
    public EntityResult bookingPrice(Map<String, Object> attrMap) {
        double priceBooking = 0;
        //Comprobamos que se ha introducido el id de la habitacion
        if (attrMap.get(BookingDao.ROOMID) == null) {
            EntityResult er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.NECESSARY_KEY);
            return er;
        }
        //Comprobamos que se ha introducido la fecha de inicio
        if (attrMap.get(BookingDao.STARTDATE) == null && attrMap.get(BookingDao.ENDDATE) == null) {
            EntityResult er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.NECESSARY_DATA);
            return er;
        }
        //Obtener tipo de habitacion
        Map<String, Object> attrMapRoom = new HashMap<>();
        attrMapRoom.put(RoomDao.IDHABITACION, attrMap.get(BookingDao.ROOMID));
        EntityResult room = this.daoHelper.query(this.roomDao, attrMapRoom, List.of(RoomDao.IDHABITACION, RoomDao.ROOMTYPEID, RoomDao.ROOMDOWNDATE));
        //Comprobamos que la habitacion existe
        if (room.calculateRecordNumber() == 0) {
            EntityResult er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.ROOM_NOT_EXIST);
            return er;
        }
        Map<String, Object> attrMapRoomType = new HashMap<>();
        attrMapRoomType.put(RoomTypeDao.TYPEID, room.getRecordValues(0).get(RoomDao.ROOMTYPEID));
        EntityResult roomtype = this.daoHelper.query(this.roomTypeDao, attrMapRoomType, List.of(RoomTypeDao.TYPEID, RoomTypeDao.PRICE));

        //Precio de la habitacion por dia
        double roomprice = (double) roomtype.getRecordValues(0).get(RoomTypeDao.PRICE);
        //Obtener criterios de precio
        Map<String, Object> attrMapCriteria = new HashMap<>();
        EntityResult criteria = this.daoHelper.query(this.criteriaDao, attrMapCriteria, List.of(CriteriaDao.ID, CriteriaDao.NAME, CriteriaDao.MULTIPLIER));

        //Hacer que obtenga los multiplicadores por nombre
        BigDecimal multiplierWeekend = (BigDecimal) criteria.getRecordValues(0).get(CriteriaDao.MULTIPLIER);
        BigDecimal earlyBooking = (BigDecimal) criteria.getRecordValues(1).get(CriteriaDao.MULTIPLIER);
        Map<Integer, BigDecimal> multiplierSeason = new HashMap<>();
        multiplierSeason.put(0, BigDecimal.ONE);
        multiplierSeason.put((int) criteria.getRecordValues(2).get(CriteriaDao.ID), (BigDecimal) criteria.getRecordValues(2).get(CriteriaDao.MULTIPLIER));
        multiplierSeason.put((int) criteria.getRecordValues(3).get(CriteriaDao.ID), (BigDecimal) criteria.getRecordValues(3).get(CriteriaDao.MULTIPLIER));
        BigDecimal multiplierLongStay = (BigDecimal) criteria.getRecordValues(4).get(CriteriaDao.MULTIPLIER);

        //Obtener fechas de la reserva
        LocalDate start;
        LocalDate startIter;
        LocalDate end;
        try {
            start = LocalDate.parse((String) attrMap.get(BookingDao.STARTDATE), DateTimeFormatter.ISO_DATE);
            end = LocalDate.parse((String) attrMap.get(BookingDao.ENDDATE), DateTimeFormatter.ISO_DATE);
            startIter = start;
        } catch (Exception e) {
            EntityResult er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.DATE_FORMAT_ERROR);
            return er;
        }
        //Primero necesito cargar todos las DateCondition
        //Luego las separo por tipo de aplicacion(Diaria,Unica)
        //Luego creo un mapa que me guarda el id de la DateCondition y el numero de veces que se aplica
        //Aplique las que se aplican una vez y añada al mapa el id de la DateCondition y el numero de veces que se aplica
        //Itere por cada dia de la reserva y aplique las que se aplican diariamente añadiendo al mapa el id de la DateCondition y el numero de veces que se aplica
        //Por ultimo itere por el mapa y aplique los multiplicadores

        //Obtener las DateCondition
        Map<String, Object> attrMapDateCondition = new HashMap<>();
        List<String> fields = new ArrayList<>();
        fields.add("*");
        EntityResult erDateCondition = criteriaService.criteriaQuery(attrMapDateCondition, fields);
        //Multiplicadores
        Map<Integer, BigDecimal> multiplierDateCondition = new HashMap<>();//Guarda los multiplicadores de las DateCondition
        //Separar por tipo de aplicacion
        Map<Integer,DateCondition> dateConditionDaily = new HashMap<>();//Guarda las condiciones que se aplican diariamente
        Map<Integer,DateCondition> dateConditionUnique = new HashMap<>();//Guarda las condiciones que se aplican una vez

        //Creo los dateCondition y cargo los multiplicadores
        for (int i = 0; i < erDateCondition.calculateRecordNumber(); i++) {
            multiplierDateCondition.put((int) erDateCondition.getRecordValues(i).get(CriteriaDao.ID), (BigDecimal) erDateCondition.getRecordValues(i).get(CriteriaDao.MULTIPLIER));
            if (erDateCondition.getRecordValues(i).get(CriteriaDao.TYPE) != null && erDateCondition.getRecordValues(i).get(CriteriaDao.DATE_CONDITION) != null) {
                String json = erDateCondition.getRecordValues(i).get(CriteriaDao.DATE_CONDITION).toString();
                int id = (int) erDateCondition.getRecordValues(i).get(CriteriaDao.ID);
                DateCondition dc = jsonToDateCondition(json);
                if (erDateCondition.getRecordValues(i).get(CriteriaDao.TYPE).equals("unique")){
                    dateConditionUnique.put(id,dc);
                }else if(erDateCondition.getRecordValues(i).get(CriteriaDao.TYPE).equals("daily")){
                    dateConditionDaily.put(id,dc);
                }
            }
        }

        //Aplico las que se aplican una vez y le aplico el multiplicador a la habitacion
        for (Map.Entry<Integer, DateCondition> entry : dateConditionUnique.entrySet()) {
            if (entry.getValue().evaluate(start,end)) {
                roomprice = roomprice * multiplierDateCondition.get(entry.getKey()).doubleValue();
            }
        }

        //Crear mapa de id de DateCondition y el extra que se aplica
        Map<Integer, BigDecimal> dateConditionExtraByCondition = new HashMap<>();
        //Itero por cada dia de la reserva y aplico las que se aplican diariamente añadiendo al mapa el id de la DateCondition y el numero de veces que se aplica
        while (!startIter.isAfter(end)) {
            priceBooking = priceBooking + roomprice;
            for (Map.Entry<Integer, DateCondition> entry : dateConditionDaily.entrySet()) {
                if (entry.getValue().evaluate(startIter,end)) {
                    if (dateConditionExtraByCondition.containsKey(entry.getKey())) {
                        dateConditionExtraByCondition.put(entry.getKey(), dateConditionExtraByCondition.get(entry.getKey()).add(multiplierDateCondition.get(entry.getKey()).multiply(BigDecimal.valueOf(roomprice)).subtract(BigDecimal.valueOf(roomprice))));
                    } else {
                        dateConditionExtraByCondition.put(entry.getKey(), multiplierDateCondition.get(entry.getKey()).multiply(BigDecimal.valueOf(roomprice)).subtract(BigDecimal.valueOf(roomprice)));
                    }
                }
            }
            startIter = startIter.plusDays(1);
        }

        //por cada extra añado al precio de la reserva
        for (Map.Entry<Integer, BigDecimal> entry : dateConditionExtraByCondition.entrySet()) {
            priceBooking = priceBooking + entry.getValue().doubleValue();
        }
        EntityResult er = new EntityResultMapImpl();
        er.setCode(EntityResult.OPERATION_SUCCESSFUL);
        er.put(BookingDao.PRICE, BigDecimal.valueOf(priceBooking).setScale(2, RoundingMode.FLOOR));
        return er;
    }

    private EntityResult obtainFinalPrice(Map<String,Object> attrMap) {
        Map<String, Object> filter = new HashMap<>();
        filter.put(BookingDao.BOOKINGID, attrMap.get(BookingDao.BOOKINGID));
        List<String> columns = new ArrayList<>();
        columns.add(ExtraExpensesDao.CONCEPT);
        columns.add(ExtraExpensesDao.PRICE);
        EntityResult historic = this.daoHelper.query(this.extraExpensesDao, filter, columns);

        EntityResult bookingPrice = this.daoHelper.query(this.bookingDao, filter, List.of(BookingDao.PRICE));

        EntityResult extraexpenses = new EntityResultMapImpl();
        BigDecimal extradecimal = (BigDecimal)bookingPrice.getRecordValues(0).get(BookingDao.PRICE);
        extraexpenses.put("Booking price: ", extradecimal);

        //Iteramos todas las extraexpenses para hacer un ticket
        BigDecimal price = new BigDecimal("0.00");
        price = price.add(extradecimal);
        for(int i=0;i<historic.calculateRecordNumber();i++) {
            extradecimal = BigDecimal.valueOf((Double) historic.getRecordValues(i).get(ExtraExpensesDao.PRICE));
            String key = (String) historic.getRecordValues(i).get(ExtraExpensesDao.CONCEPT) + ": ";
            if (extraexpenses.containsKey(key)) {
                BigDecimal repeatedexpense = (BigDecimal) extraexpenses.get(key);
                extradecimal = extradecimal.add(repeatedexpense);
            }
            extraexpenses.put((String) historic.getRecordValues(i).get(ExtraExpensesDao.CONCEPT) + ": ", extradecimal);
            price = price.add(extradecimal);
        }

        extraexpenses.put("Total: ", price);
        return extraexpenses;
    }

    private DateCondition jsonToDateCondition(String json) {
        try {
            DateConditionModule dateConditionModule = new DateConditionModule();
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(dateConditionModule.getModule());
            DateCondition dateCondition = objectMapper.readValue(json, DateCondition.class);
            return dateCondition;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}

