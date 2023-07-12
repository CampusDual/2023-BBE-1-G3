package com.hotel.continental.model.core.service;

import com.hotel.continental.api.core.service.IParkingService;
import com.hotel.continental.model.core.dao.BookingDao;
import com.hotel.continental.model.core.dao.ParkingDao;
import com.hotel.continental.model.core.dao.ParkingHistoryDao;
import com.hotel.continental.model.core.dao.RoomDao;
import com.hotel.continental.model.core.tools.ErrorMessages;
import com.ontimize.jee.common.dto.EntityResult;
import com.ontimize.jee.common.dto.EntityResultMapImpl;
import com.ontimize.jee.server.dao.DefaultOntimizeDaoHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Lazy
@Service("ParkingService")
public class ParkingService implements IParkingService {

    @Autowired
    private DefaultOntimizeDaoHelper daoHelper;
    @Autowired
    private ParkingDao parkingDao;
    @Autowired
    private ParkingHistoryDao parkingHistoryDao;
    @Autowired
    private BookingDao bookingDao;
    @Autowired
    private ParkingHistoryService parkingHistoryService;
    @Autowired
    private RoomDao roomDao;


    @Override
    public EntityResult parkingEnter(Map<?, ?> attrMap) {
        EntityResult er = new EntityResultMapImpl();
        //Comprobar que me llega los datos necesarios para hacer la entrada id_booking id_parking
        if(attrMap.get(ParkingHistoryDao.ID_BOOKING) == null || attrMap.get(ParkingDao.ID_PARKING) == null){
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.NECESSARY_DATA);
            return er;
        }
        //Comprobar que hay sitio en el parking indicado
        Map<String, Object> attrMapParking = Map.of(ParkingDao.ID_PARKING, attrMap.get(ParkingDao.ID_PARKING));
        EntityResult erParking = this.daoHelper.query(parkingDao, attrMapParking, List.of(ParkingDao.OCCUPIED_CAPACITY,ParkingDao.TOTAL_CAPACITY,ParkingDao.ID_HOTEL));
        if(erParking.calculateRecordNumber() == 0){
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.PARKING_NOT_FOUND);
            return er;
        }

        int occupiedCapacity = (int)erParking.getRecordValues(0).get(ParkingDao.OCCUPIED_CAPACITY);
        if(occupiedCapacity == (int)erParking.getRecordValues(0).get(ParkingDao.TOTAL_CAPACITY)){
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.PARKING_FULL);
            return er;
        }
        //Comprobar que la reserva existe
        Map<String, Object> attrMapBooking = Map.of(BookingDao.BOOKINGID, attrMap.get(ParkingHistoryDao.ID_BOOKING));
        EntityResult erBooking = this.daoHelper.query(bookingDao, attrMapBooking, List.of(BookingDao.BOOKINGID, BookingDao.STARTDATE, BookingDao.ENDDATE, BookingDao.CHECKIN_DATETIME, BookingDao.CHECKOUT_DATETIME,BookingDao.ROOMID));
        if(erBooking.calculateRecordNumber() == 0){
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.BOOKING_NOT_EXIST);
            return er;
        }
        //Pillo el id de la habitacion y el id del hotel del parking, para comprobar que la habitacion de la reserva esta en el mismo hotel del parking
        Map<String, Object> attrMapRoom = Map.of(RoomDao.IDHABITACION, erBooking.getRecordValues(0).get(BookingDao.ROOMID),RoomDao.IDHOTEL,erParking.getRecordValues(0).get(ParkingDao.ID_HOTEL));
        EntityResult erRoom = this.daoHelper.query(roomDao, attrMapRoom, List.of(BookingDao.ROOMID,RoomDao.IDHOTEL));
        if(erRoom.calculateRecordNumber() == 0){
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.BOOKING_NOT_SAME_HOTEL_AS_PARKING);
            return er;
        }
        //Comprobar que la fecha actual es igual o superior a la fecha de inicio de la reserva
        Date startDate = (Date) erBooking.getRecordValues(0).get(BookingDao.STARTDATE);
        Date currentDate = new Date();
        if(startDate.compareTo(currentDate) > 0){
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.BOOKING_NOT_STARTED);
            return er;
        }
        //Comprobar que la reserva esta activa,que hizo checkin y que no ha hecho checkout
        if(erBooking.getRecordValues(0).get(BookingDao.CHECKIN_DATETIME) == null){
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.BOOKING_NOT_CHECKED_IN);
            return er;
        }
        if(erBooking.getRecordValues(0).get(BookingDao.CHECKOUT_DATETIME) != null){
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.BOOKING_ALREADY_CHECKED_OUT);
            return er;
        }

        //Comprobar que la reserva no ha entrado ya en el parking (es decir tiene fecha de entrada pero no de salida)
        //Obtener todos los parking_history de la reserva
        Map<String, Object> attrMapParkingHistory = Map.of(ParkingHistoryDao.ID_BOOKING, attrMap.get(ParkingHistoryDao.ID_BOOKING));
        EntityResult erParkingHistory = this.daoHelper.query(parkingHistoryDao, attrMapParkingHistory, List.of(ParkingHistoryDao.ID_BOOKING,ParkingHistoryDao.ENTRY_DATE,ParkingHistoryDao.EXIT_DATE));
        //Comprobar que no hay ningun parking_history que tenga fecha de entrada pero no de salida
        boolean notExitDate=((List<Date>)erParkingHistory.get(ParkingHistoryDao.EXIT_DATE)).stream().anyMatch(date -> date == null);
        if(notExitDate){
            er.setCode(EntityResult.OPERATION_WRONG);
            er.setMessage(ErrorMessages.BOOKING_ALREADY_IN_PARKING);
            return er;
        }
        //Insertar en la tabla parking_history
        Map<String,Object> attrMapParkingHistoryInsert = Map.of(ParkingHistoryDao.ID_PARKING,attrMap.get(ParkingHistoryDao.ID_PARKING),ParkingHistoryDao.ID_BOOKING,attrMap.get(ParkingHistoryDao.ID_BOOKING),ParkingHistoryDao.ENTRY_DATE,currentDate);
        parkingHistoryService.parkingHistoryInsert(attrMapParkingHistoryInsert);
        //Actualizar tabla parking para sumar 1 a los coches que hay en el parking
        occupiedCapacity++;
        Map<String,Object> keyMapParkingUpdate = Map.of(ParkingDao.ID_PARKING,attrMap.get(ParkingDao.ID_PARKING));
        Map<String,Object> attrMapParkingUpdate = Map.of(ParkingDao.OCCUPIED_CAPACITY,occupiedCapacity);
        return this.daoHelper.update(parkingDao,attrMapParkingUpdate,keyMapParkingUpdate);
    }
}
