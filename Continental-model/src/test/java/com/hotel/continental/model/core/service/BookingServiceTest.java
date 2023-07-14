package com.hotel.continental.model.core.service;


import com.hotel.continental.model.core.dao.*;
import com.hotel.continental.model.core.tools.ErrorMessages;
import com.ontimize.jee.common.dto.EntityResult;
import com.ontimize.jee.common.dto.EntityResultMapImpl;
import com.ontimize.jee.server.dao.DefaultOntimizeDaoHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    @Mock
    static DefaultOntimizeDaoHelper daoHelper;

    @InjectMocks
    BookingService bookingService;

    @Mock
    static RoomService roomService;
    @Mock
    static AccessCardDao accessCardDao;
    @Mock
    static AccessCardAssignmentService accessCardAssignmentService;
    @Mock
    BookingDao bookingDao;
    @Mock
    AccessCardAssignmentDao accessCardAssignmentDao;
    @Mock
    RoomDao roomDao;
    @Mock
    CriteriaDao criteriaDao;
    @Mock
    RoomTypeDao roomTypeDao;
    @Mock
    ExtraExpensesDao extraExpensesDao;

    @ParameterizedTest(name = "Test case {index} : {0}")
    @MethodSource("bookingInsertTestData")
    @DisplayName("TestParametrized booking Insert")
    void testBookingInsert(String name, Map<String, Object> attr, EntityResult expectedResult, List<Supplier> mocks) {
        //For each mock, execute the get method, to make sure the mock is called
        mocks.forEach(mockMap -> {
            mockMap.get();
        });
        EntityResult result = bookingService.bookingInsert(attr);
        // Assert
        assertEquals(expectedResult.getMessage(), result.getMessage());
        assertEquals(expectedResult.getCode(), result.getCode());
    }

    private static Stream<Arguments> bookingInsertTestData() {
        return Stream.of(
                // Test case 1: Successful insert
                Arguments.of(
                        "Successful Insert",//Nombre del test
                        new HashMap<String, Object>() {{
                            put(BookingDao.STARTDATE, "2023-06-09T10:31:10.000+0000");
                            put(BookingDao.ENDDATE, "2023-10-09T10:31:10.000+0000");
                            put(BookingDao.CLIENT, 0);
                        }},//Atributos-Columnas-Keymap
                        createEntityResult(EntityResult.OPERATION_SUCCESSFUL, ""),//Resultado esperado
                        List.of(
                                (Supplier) () -> {
                                    EntityResult erRoom = new EntityResultMapImpl();
                                    erRoom.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    erRoom.put(RoomDao.IDHOTEL, List.of(0));
                                    erRoom.put(RoomDao.IDHABITACION, List.of(2));
                                    erRoom.put(RoomDao.ROOMNUMBER, List.of(10));
                                    return Mockito.when(roomService.freeRoomsQuery(Mockito.anyMap(), Mockito.anyList())).thenReturn(erRoom);
                                },
                                (Supplier) () -> {
                                    EntityResult erReserva = new EntityResultMapImpl();
                                    erReserva.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    return Mockito.when(daoHelper.insert(Mockito.any(BookingDao.class), Mockito.anyMap())).thenReturn(erReserva);
                                },
                                (Supplier) () -> {
                                    EntityResult erRoom = new EntityResultMapImpl();
                                    erRoom.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    erRoom.put(RoomDao.ROOMTYPEID, List.of(1));
                                    return Mockito.when(daoHelper.query(Mockito.any(RoomDao.class), Mockito.anyMap(), Mockito.anyList())).thenReturn(erRoom);
                                },
                                (Supplier) () -> {
                                    EntityResult erRoomType = new EntityResultMapImpl();
                                    erRoomType.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    erRoomType.put(RoomDao.ROOMTYPEID, List.of(1));
                                    erRoomType.put(RoomTypeDao.PRICE, List.of(100.0));
                                    return Mockito.when(daoHelper.query(Mockito.any(RoomTypeDao.class), Mockito.anyMap(), Mockito.anyList())).thenReturn(erRoomType);
                                },
                                (Supplier) () -> {
                                    EntityResult erCriteria = new EntityResultMapImpl();
                                    erCriteria.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    erCriteria.put(CriteriaDao.MULTIPLIER, List.of(new BigDecimal(1.0), new BigDecimal(1.0), new BigDecimal(1.0), new BigDecimal(1.0), new BigDecimal(1.0)));
                                    erCriteria.put(CriteriaDao.ID, List.of(1, 2, 3, 4, 5));
                                    return Mockito.when(daoHelper.query(Mockito.any(CriteriaDao.class), Mockito.anyMap(), Mockito.anyList())).thenReturn(erCriteria);
                                }

                        )//Mocks
                ),
                // Test case 2: Insert no free rooms
                Arguments.of(
                        "Insert no free rooms",
                        new HashMap<String, Object>() {{
                            put(BookingDao.STARTDATE, "2023-06-09T10:31:10.000+0000");
                            put(BookingDao.ENDDATE, "2023-10-09T10:31:10.000+0000");
                            put(BookingDao.CLIENT, 0);
                        }},
                        createEntityResult(EntityResult.OPERATION_WRONG, ErrorMessages.ROOM_NOT_FREE),
                        List.of(
                                (Supplier) () -> {
                                    EntityResult erRoom = new EntityResultMapImpl();
                                    erRoom.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    return Mockito.when(roomService.freeRoomsQuery(Mockito.anyMap(), Mockito.anyList())).thenReturn(erRoom);
                                }
                        )
                ),
                // Test case 3: Insert empty data
                Arguments.of(
                        "Insert empty data",
                        Map.of(),
                        createEntityResult(EntityResult.OPERATION_WRONG, ErrorMessages.NECESSARY_DATA),
                        List.of()
                )

        );
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("bookingQueryTestData")
    @DisplayName("TestParametrized booking query")
    void testBookingQuery(String name, Map<String, Object> keyMap, List<String> attr, EntityResult expectedResult, List<Supplier> mocks) {
        //For each mock, execute the get method, to make sure the mock is called
        mocks.forEach(mockMap -> {
            mockMap.get();
        });
        EntityResult result = bookingService.bookingQuery(keyMap, attr);
        // Assert
        assertEquals(expectedResult.getMessage(), result.getMessage());
        assertEquals(expectedResult.getCode(), result.getCode());
    }

    private static Stream<Arguments> bookingQueryTestData() {
        return Stream.of(
                // Test case 1: Successful delete
                Arguments.of(
                        "Test case 1: Successful query",
                        Map.of(BookingDao.BOOKINGID, 0),
                        List.of(BookingDao.BOOKINGID),
                        createEntityResult(EntityResult.OPERATION_SUCCESSFUL, ""),
                        List.of(
                                (Supplier) () -> {
                                    EntityResult erReserva = new EntityResultMapImpl();
                                    erReserva.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    erReserva.put(BookingDao.BOOKINGID, List.of(0));
                                    return Mockito.when(daoHelper.query(Mockito.any(BookingDao.class), Mockito.anyMap(), Mockito.anyList())).thenReturn(erReserva);
                                }
                        )
                ),
                // Test case 2: Booking empty data
                Arguments.of(
                        "Test case 2: Booking empty data",
                        Map.of(),
                        List.of(BookingDao.BOOKINGID),
                        createEntityResult(EntityResult.OPERATION_WRONG, ErrorMessages.BOOKING_NOT_EXIST),
                        List.of()
                ),
                // Test case 3: Booking not found
                Arguments.of(
                        "Test case 3: Booking not found",
                        Map.of(BookingDao.BOOKINGID, 0),
                        List.of(BookingDao.BOOKINGID),
                        createEntityResult(EntityResult.OPERATION_WRONG, ErrorMessages.BOOKING_NOT_EXIST),
                        List.of(
                                (Supplier) () -> {
                                    EntityResult erReserva = new EntityResultMapImpl();
                                    erReserva.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    return Mockito.when(daoHelper.query(Mockito.any(BookingDao.class), Mockito.anyMap(), Mockito.anyList())).thenReturn(erReserva);
                                }
                        )
                )

        );
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("bookingUpdateTestData")
    @DisplayName("TestParametrized booking update")
    void testBookingDelete(String name, Map<String, Object> keyMap, Map<String, Object> attrMap, EntityResult expectedResult, List<Supplier> mocks) {
        //For each mock, execute the get method, to make sure the mock is called
        mocks.forEach(mockMap -> {
            mockMap.get();
        });
        EntityResult result = bookingService.bookingUpdate(attrMap, keyMap);
        // Assert
        assertEquals(expectedResult.getMessage(), result.getMessage());
        assertEquals(expectedResult.getCode(), result.getCode());
    }

    private static Stream<Arguments> bookingUpdateTestData() {
        return Stream.of(
                // Test case 1: Successful update
                Arguments.of(
                        "Test case 1: Successful update",
                        Map.of(BookingDao.BOOKINGID, 0),
                        Map.of(BookingDao.CLIENT, 0),
                        createEntityResult(EntityResult.OPERATION_SUCCESSFUL, ""),
                        List.of(
                                (Supplier) () -> {
                                    EntityResult erReserva = new EntityResultMapImpl();
                                    erReserva.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    erReserva.put(BookingDao.BOOKINGID, List.of(0));
                                    return Mockito.when(daoHelper.query(Mockito.any(BookingDao.class), Mockito.anyMap(), Mockito.anyList())).thenReturn(erReserva);
                                },
                                (Supplier) () -> {
                                    EntityResult er = new EntityResultMapImpl();
                                    er.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    return Mockito.when(daoHelper.update(Mockito.any(BookingDao.class), Mockito.anyMap(), Mockito.anyMap())).thenReturn(er);
                                }
                        )
                ),
                // Test case 2: Booking not found
                Arguments.of(
                        "Test case 2: Booking not found",
                        Map.of(BookingDao.BOOKINGID, 0),
                        Map.of(),
                        createEntityResult(EntityResult.OPERATION_WRONG, ErrorMessages.BOOKING_NOT_EXIST),
                        List.of(
                                (Supplier) () -> {
                                    EntityResult erReserva = new EntityResultMapImpl();
                                    erReserva.setCode(EntityResult.OPERATION_WRONG);
                                    return Mockito.when(daoHelper.query(Mockito.any(BookingDao.class), Mockito.anyMap(), Mockito.anyList())).thenReturn(erReserva);
                                }
                        )
                ),
                // Test case 3: Booking empty data
                Arguments.of(
                        "Test case 3: Booking empty data",
                        Map.of(),
                        Map.of(),
                        createEntityResult(EntityResult.OPERATION_WRONG, ErrorMessages.NECESSARY_KEY),
                        List.of()
                )

        );
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("bookingDeleteTestData")
    @DisplayName("TestParametrized booking delete")
    void testBookingDelete(String name, Map<String, Object> keyMap, EntityResult expectedResult, List<Supplier> mocks) {
        //For each mock, execute the get method, to make sure the mock is called
        mocks.forEach(mockMap -> {
            mockMap.get();
        });
        EntityResult result = bookingService.bookingDelete(keyMap);
        // Assert
        assertEquals(expectedResult.getMessage(), result.getMessage());
        assertEquals(expectedResult.getCode(), result.getCode());
    }

    private static Stream<Arguments> bookingDeleteTestData() {
        return Stream.of(
                // Test case 1: Successful delete
                Arguments.of(
                        "Test case 1: Successful delete",
                        Map.of(BookingDao.BOOKINGID, 0),
                        createEntityResult(EntityResult.OPERATION_SUCCESSFUL, ""),
                        List.of(
                                (Supplier) () -> {
                                    EntityResult erReserva = new EntityResultMapImpl();
                                    erReserva.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    erReserva.put(BookingDao.BOOKINGID, List.of(0));
                                    return Mockito.when(daoHelper.query(Mockito.any(BookingDao.class), Mockito.anyMap(), Mockito.anyList())).thenReturn(erReserva);
                                },
                                (Supplier) () -> {
                                    EntityResult er = new EntityResultMapImpl();
                                    er.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    return Mockito.when(daoHelper.delete(Mockito.any(BookingDao.class), Mockito.anyMap())).thenReturn(er);
                                }
                        )
                ),
                // Test case 2: Booking not found
                Arguments.of(
                        "Test case 2: Booking not found",
                        Map.of(BookingDao.BOOKINGID, 0),
                        createEntityResult(EntityResult.OPERATION_WRONG, ErrorMessages.BOOKING_NOT_EXIST),
                        List.of(
                                (Supplier) () -> {
                                    EntityResult erReserva = new EntityResultMapImpl();
                                    erReserva.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    return Mockito.when(daoHelper.query(Mockito.any(BookingDao.class), Mockito.anyMap(), Mockito.anyList())).thenReturn(erReserva);
                                }
                        )
                ),
                // Test case 3: Booking empty data
                Arguments.of(
                        "Test case 3: Booking empty data",
                        Map.of(),
                        createEntityResult(EntityResult.OPERATION_WRONG, ErrorMessages.NECESSARY_KEY),
                        List.of()
                )

        );
    }


    @ParameterizedTest(name = "Test case {index} : {0}")
    @MethodSource("bookingCheckinTestData")
    @DisplayName("TestParametrized booking check-in")
    void testBookingCheckin(String name, Map<String, Object> attrMap, EntityResult expectedResult, List<Supplier> mocks) {
        //For each mock, execute the get method, to make sure the mock is called
        mocks.forEach(mockMap -> {
            mockMap.get();
        });
        EntityResult result = bookingService.bookingCheckin(attrMap);
        // Assert
        assertEquals(expectedResult.getMessage(), result.getMessage());
        assertEquals(expectedResult.getCode(), result.getCode());
    }

    private static Stream<Arguments> bookingCheckinTestData() {
        return Stream.of(
                // Test case 1: Successful check-in
                Arguments.of(
                        "Successful check-in",
                        Map.of(BookingDao.BOOKINGID, 1, BookingDao.CLIENT, 2, AccessCardDao.ACCESSCARDID, 1),
                        createEntityResult(EntityResult.OPERATION_SUCCESSFUL, ErrorMessages.BOOKING_CHECK_IN_SUCCESS),
                        List.of(
                                (Supplier) () -> {
                                    EntityResult erReserva = new EntityResultMapImpl();
                                    erReserva.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    erReserva.put(BookingDao.BOOKINGID, List.of(0));
                                    EntityResult erReservaCliente = new EntityResultMapImpl();
                                    erReservaCliente.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    erReservaCliente.put(BookingDao.CLIENT, List.of(0));
                                    return Mockito.when(daoHelper.query(Mockito.any(BookingDao.class), Mockito.anyMap(), Mockito.anyList())).thenReturn(erReserva, erReservaCliente);
                                },
                                (Supplier) () -> {
                                    EntityResult erInsertarTarjeta = new EntityResultMapImpl();
                                    erInsertarTarjeta.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    return Mockito.when(accessCardAssignmentService.accesscardassignmentInsert(Mockito.anyMap())).thenReturn(erInsertarTarjeta);
                                },
                                (Supplier) () -> {
                                    EntityResult er = new EntityResultMapImpl();
                                    er.setCode(0);
                                    er.put(BookingDao.CLIENT, List.of(0));
                                    return Mockito.when(daoHelper.update(Mockito.any(BookingDao.class), Mockito.anyMap(), Mockito.anyMap())).thenReturn(er);
                                }
                        )
                ),
                // Test case 2: Check-in with invalid booking id
                Arguments.of(
                        "Check-in with invalid booking id",
                        Map.of(BookingDao.BOOKINGID, 1, BookingDao.CLIENT, 2, AccessCardDao.ACCESSCARDID, 1),
                        createEntityResult(EntityResult.OPERATION_WRONG, ErrorMessages.BOOKING_NOT_EXIST),
                        List.of(
                                (Supplier) () -> {
                                    EntityResult erReserva = new EntityResultMapImpl();
                                    erReserva.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    return Mockito.when(daoHelper.query(Mockito.any(BookingDao.class), Mockito.anyMap(), Mockito.anyList())).thenReturn(erReserva);
                                }
                        )
                ),
                // Test case 3: Check-in with invalid bookingid-clientid combination
                Arguments.of(
                        "Check-in with invalid bookingid-clientid combination",
                        Map.of(BookingDao.BOOKINGID, 1, BookingDao.CLIENT, 2, AccessCardDao.ACCESSCARDID, 1),
                        createEntityResult(EntityResult.OPERATION_WRONG, ErrorMessages.BOOKING_DOESNT_BELONG_CLIENT),
                        List.of(
                                (Supplier) () -> {
                                    EntityResult erReserva = new EntityResultMapImpl();
                                    erReserva.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    erReserva.put(BookingDao.BOOKINGID, List.of(0));
                                    EntityResult erReservaCliente = new EntityResultMapImpl();
                                    erReservaCliente.setCode(EntityResult.OPERATION_WRONG);
                                    return Mockito.when(daoHelper.query(Mockito.any(BookingDao.class), Mockito.anyMap(), Mockito.anyList())).thenReturn(erReserva, erReservaCliente);
                                }
                        )
                ),
                // Test case 4: Already checked-in
                Arguments.of(
                        "Already checked-in",
                        Map.of(BookingDao.BOOKINGID, 1, BookingDao.CLIENT, 2, AccessCardDao.ACCESSCARDID, 1),
                        createEntityResult(EntityResult.OPERATION_WRONG, ErrorMessages.BOOKING_ALREADY_CHECKED_IN),
                        List.of(
                                (Supplier) () -> {
                                    EntityResult erReserva = new EntityResultMapImpl();
                                    erReserva.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    erReserva.put(BookingDao.BOOKINGID, List.of(0));
                                    erReserva.put(BookingDao.CHECKIN_DATETIME, List.of(LocalDateTime.now()));
                                    EntityResult erReservaCliente = new EntityResultMapImpl();
                                    erReservaCliente.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    erReservaCliente.put(BookingDao.CLIENT, List.of(0));
                                    return Mockito.when(daoHelper.query(Mockito.any(BookingDao.class), Mockito.anyMap(), Mockito.anyList())).thenReturn(erReserva, erReservaCliente);
                                }
                        )
                ),
                // Test case 5: Missing booking ID
                Arguments.of(
                        "Missing booking ID",
                        Map.of(),
                        createEntityResult(EntityResult.OPERATION_WRONG, ErrorMessages.NECESSARY_KEY),
                        List.of()
                )

        );
    }

    @ParameterizedTest(name = "Test case {index} : {0}")
    @MethodSource("bookingCheckOutTestData")
    @DisplayName("TestParametrized booking check-out")
    void testBookingCheckout(String name, Map<String, Object> attrMap, EntityResult expectedResult, List<Supplier> mocks) {
        //For each mock, execute the get method, to make sure the mock is called
        mocks.forEach(mockMap -> {
            mockMap.get();
        });
        EntityResult result = bookingService.bookingCheckout(attrMap);
        // Assert
        assertEquals(expectedResult.getMessage(), result.getMessage());
        assertEquals(expectedResult.getCode(), result.getCode());
    }

    private static Stream<Arguments> bookingCheckOutTestData() {
        return Stream.of(
                // Test case 1: Successful check-out
                Arguments.of(
                        "Successful check-out",
                        Map.of(BookingDao.BOOKINGID, 1, BookingDao.CLIENT, 2),
                        createEntityResult(EntityResult.OPERATION_SUCCESSFUL, ErrorMessages.BOOKING_CHECK_OUT_SUCCESS),
                        List.of(
                                (Supplier) () -> {
                                    EntityResult erReserva = new EntityResultMapImpl();
                                    erReserva.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    erReserva.put(BookingDao.BOOKINGID, List.of(0));
                                    erReserva.put(BookingDao.CHECKIN_DATETIME, List.of(LocalDateTime.now()));
                                    EntityResult erReservaCliente = new EntityResultMapImpl();
                                    erReservaCliente.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    erReservaCliente.put(BookingDao.CLIENT, List.of(0));
                                    EntityResult erBookingCost = new EntityResultMapImpl();
                                    erBookingCost.put(BookingDao.PRICE, List.of(new BigDecimal(100)));
                                    return Mockito.when(daoHelper.query(Mockito.any(BookingDao.class), Mockito.anyMap(), Mockito.anyList())).thenReturn(erReserva, erReservaCliente, erBookingCost);
                                },
                                (Supplier) () -> {
                                    EntityResult erInsertarTarjeta = new EntityResultMapImpl();
                                    erInsertarTarjeta.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    return Mockito.when(accessCardAssignmentService.accesscardassignmentRecover(Mockito.anyMap())).thenReturn(erInsertarTarjeta);
                                },
                                (Supplier) () -> {
                                    EntityResult erExtraEspenses = new EntityResultMapImpl();
                                    erExtraEspenses.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    return Mockito.when(daoHelper.query(Mockito.any(ExtraExpensesDao.class), Mockito.anyMap(), Mockito.anyList())).thenReturn(erExtraEspenses);
                                },
                                (Supplier) () -> {
                                    EntityResult er = new EntityResultMapImpl();
                                    er.setCode(0);
                                    er.put(BookingDao.CLIENT, List.of(0));
                                    return Mockito.when(daoHelper.update(Mockito.any(BookingDao.class), Mockito.anyMap(), Mockito.anyMap())).thenReturn(er);
                                }
                        )
                ),
                // Test case 2: Check-out with invalid booking id
                Arguments.of(
                        "Check-out with invalid booking id",
                        Map.of(BookingDao.BOOKINGID, 1, BookingDao.CLIENT, 2),
                        createEntityResult(EntityResult.OPERATION_WRONG, ErrorMessages.BOOKING_NOT_EXIST),
                        List.of(
                                (Supplier) () -> {
                                    EntityResult erReserva = new EntityResultMapImpl();
                                    erReserva.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    return Mockito.when(daoHelper.query(Mockito.any(BookingDao.class), Mockito.anyMap(), Mockito.anyList())).thenReturn(erReserva);
                                }
                        )
                ),
                // Test case 3: Check-out with invalid bookingid-clientid combination
                Arguments.of(
                        "Check-out with invalid bookingid-clientid combination",
                        Map.of(BookingDao.BOOKINGID, 1, BookingDao.CLIENT, 2),
                        createEntityResult(EntityResult.OPERATION_WRONG, ErrorMessages.BOOKING_DOESNT_BELONG_CLIENT),
                        List.of(
                                (Supplier) () -> {
                                    EntityResult erReserva = new EntityResultMapImpl();
                                    erReserva.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    erReserva.put(BookingDao.BOOKINGID, List.of(0));
                                    erReserva.put(BookingDao.CHECKIN_DATETIME, List.of(LocalDateTime.now()));
                                    EntityResult erReservaCliente = new EntityResultMapImpl();
                                    erReservaCliente.setCode(EntityResult.OPERATION_WRONG);
                                    return Mockito.when(daoHelper.query(Mockito.any(BookingDao.class), Mockito.anyMap(), Mockito.anyList())).thenReturn(erReserva, erReservaCliente);
                                }
                        )
                ),
                // Test case 4: Check-out with invalid card-booking combination
                Arguments.of(
                        "Check-out with invalid card-booking combination",
                        Map.of(BookingDao.BOOKINGID, 1, BookingDao.CLIENT, 2),
                        createEntityResult(EntityResult.OPERATION_WRONG, ErrorMessages.CARD_DOESNT_BELONG_BOOKING),
                        List.of(
                                (Supplier) () -> {
                                    EntityResult erReserva = new EntityResultMapImpl();
                                    erReserva.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    erReserva.put(BookingDao.BOOKINGID, List.of(0));
                                    erReserva.put(BookingDao.CHECKIN_DATETIME, List.of(LocalDateTime.now()));
                                    EntityResult erReservaCliente = new EntityResultMapImpl();
                                    erReservaCliente.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    erReservaCliente.put(BookingDao.CLIENT, List.of(0));
                                    EntityResult erBookingCost = new EntityResultMapImpl();
                                    erBookingCost.put(BookingDao.PRICE, List.of(new BigDecimal(100)));
                                    return Mockito.when(daoHelper.query(Mockito.any(BookingDao.class), Mockito.anyMap(), Mockito.anyList())).thenReturn(erReserva, erReservaCliente, erBookingCost);
                                },
                                (Supplier) () -> {
                                    EntityResult erInsertarTarjeta = new EntityResultMapImpl();
                                    erInsertarTarjeta.setCode(EntityResult.OPERATION_WRONG);
                                    erInsertarTarjeta.setMessage(ErrorMessages.CARD_DOESNT_BELONG_BOOKING);
                                    return Mockito.when(accessCardAssignmentService.accesscardassignmentRecover(Mockito.anyMap())).thenReturn(erInsertarTarjeta);
                                }
                        )
                ),
                // Test case 5: Already checked-out
                Arguments.of(
                        "Already checked-out",
                        Map.of(BookingDao.BOOKINGID, 1, BookingDao.CLIENT, 2),
                        createEntityResult(EntityResult.OPERATION_WRONG, ErrorMessages.BOOKING_ALREADY_CHECKED_OUT),
                        List.of(
                                (Supplier) () -> {
                                    EntityResult erReserva = new EntityResultMapImpl();
                                    erReserva.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    erReserva.put(BookingDao.BOOKINGID, List.of(0));
                                    erReserva.put(BookingDao.CHECKIN_DATETIME, List.of(LocalDateTime.now()));
                                    erReserva.put(BookingDao.CHECKOUT_DATETIME, List.of(LocalDateTime.now()));
                                    EntityResult erReservaCliente = new EntityResultMapImpl();
                                    erReservaCliente.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    erReservaCliente.put(BookingDao.CLIENT, List.of(0));
                                    return Mockito.when(daoHelper.query(Mockito.any(BookingDao.class), Mockito.anyMap(), Mockito.anyList())).thenReturn(erReserva, erReservaCliente);
                                }
                        )
                ),
                // Test case 6: Already checked-out
                Arguments.of(
                        "Already checked-out",
                        Map.of(BookingDao.BOOKINGID, 1, BookingDao.CLIENT, 2),
                        createEntityResult(EntityResult.OPERATION_WRONG, ErrorMessages.BOOKING_ALREADY_CHECKED_OUT),
                        List.of(
                                (Supplier) () -> {
                                    EntityResult erReserva = new EntityResultMapImpl();
                                    erReserva.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    erReserva.put(BookingDao.BOOKINGID, List.of(0));
                                    erReserva.put(BookingDao.CHECKIN_DATETIME, List.of(LocalDateTime.now()));
                                    erReserva.put(BookingDao.CHECKOUT_DATETIME, List.of(LocalDateTime.now()));
                                    EntityResult erReservaCliente = new EntityResultMapImpl();
                                    erReservaCliente.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    erReservaCliente.put(BookingDao.CLIENT, List.of(0));
                                    return Mockito.when(daoHelper.query(Mockito.any(BookingDao.class), Mockito.anyMap(), Mockito.anyList())).thenReturn(erReserva, erReservaCliente);
                                }
                        )
                ),
                // Test case 5: Missing booking ID
                Arguments.of(
                        "Missing booking ID",
                        Map.of(),
                        createEntityResult(EntityResult.OPERATION_WRONG, ErrorMessages.NECESSARY_KEY),
                        List.of()
                )
        );
    }

    @ParameterizedTest(name = "Test case {index} : {0}")
    @MethodSource("bookingPriceTestData")
    @DisplayName("TestParametrized booking price")
    void testBookingPrice(String name, Map<String, Object> attrMap, EntityResult expectedResult, List<Supplier> mocks) {
        //For each mock, execute the get method, to make sure the mock is called
        mocks.forEach(mockMap -> {
            mockMap.get();
        });
        EntityResult result = bookingService.bookingPrice(attrMap);
        // Assert
        assertEquals(expectedResult.getMessage(), result.getMessage());
        assertEquals(expectedResult.getCode(), result.getCode());
    }

    private static Stream<Arguments> bookingPriceTestData() {
        final List<Integer> criteriaID = List.of(5, 6, 7, 8, 9);
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return Stream.of(
                //region Test case 1: Successful price
                Arguments.of(
                        "Successful price",
                        Map.of(RoomDao.IDHABITACION, 1, BookingDao.STARTDATE, LocalDate.now().plusDays(20).format(formatter), BookingDao.ENDDATE, LocalDate.now().plusDays(30).format(formatter)),
                        createEntityResult(EntityResult.OPERATION_SUCCESSFUL, ""),
                        List.of(
                                (Supplier) () -> {
                                    EntityResult erRoom = new EntityResultMapImpl();
                                    erRoom.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    erRoom.put(RoomDao.IDHABITACION, List.of(0));
                                    erRoom.put(RoomDao.ROOMTYPEID, List.of(1));
                                    erRoom.put(RoomDao.ROOMDOWNDATE, List.of(LocalDate.now().plusDays(10)));
                                    return Mockito.when(daoHelper.query(Mockito.any(RoomDao.class), Mockito.anyMap(), Mockito.anyList())).thenReturn(erRoom);
                                },
                                (Supplier) () -> {
                                    EntityResult erRoomType = new EntityResultMapImpl();
                                    erRoomType.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    erRoomType.put(RoomTypeDao.PRICE, List.of(100.0));
                                    return Mockito.when(daoHelper.query(Mockito.any(RoomTypeDao.class), Mockito.anyMap(), Mockito.anyList())).thenReturn(erRoomType);
                                },
                                (Supplier) () -> {
                                    EntityResult erCriteria = new EntityResultMapImpl();
                                    erCriteria.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    erCriteria.put(CriteriaDao.ID, criteriaID);
                                    erCriteria.put(CriteriaDao.NAME, List.of("Test", "Test", "Test", "Test", "Test"));
                                    erCriteria.put(CriteriaDao.MULTIPLIER, List.of(new BigDecimal(1), new BigDecimal(1), new BigDecimal(1), new BigDecimal(1), new BigDecimal(1)));
                                    return Mockito.when(daoHelper.query(Mockito.any(CriteriaDao.class), Mockito.anyMap(), Mockito.anyList())).thenReturn(erCriteria);
                                }
                        )
                ),
                //endregion
                //region Test case 2: Missing room ID
                Arguments.of(
                        "Missing room ID",
                        Map.of(BookingDao.STARTDATE, LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), BookingDao.ENDDATE, LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))),
                        createEntityResult(EntityResult.OPERATION_WRONG, ErrorMessages.NECESSARY_KEY),
                        List.of()
                ),
                //endregion
                //region Test case 3: Missing start date/ end date
                Arguments.of(
                        "Missing start date/ end date",
                        Map.of(RoomDao.IDHABITACION, 1),
                        createEntityResult(EntityResult.OPERATION_WRONG, ErrorMessages.NECESSARY_DATA),
                        List.of()
                ),
                //endregion
                //region Test case 4: Wrong room ID
                Arguments.of(
                        " Wrong room ID",
                        Map.of(BookingDao.ROOMID, 1, BookingDao.STARTDATE, LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), BookingDao.ENDDATE, LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))),
                        createEntityResult(EntityResult.OPERATION_WRONG, ErrorMessages.ROOM_NOT_EXIST),
                        List.of(
                                (Supplier) () -> {
                                    EntityResult erRoom = new EntityResultMapImpl();
                                    erRoom.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    return Mockito.when(daoHelper.query(Mockito.any(RoomDao.class), Mockito.anyMap(), Mockito.anyList())).thenReturn(erRoom);
                                }
                        )
                ),
                //endregion
                //region Test case 5: Wrong start date/ end date
                Arguments.of(
                        "Successful price",
                        Map.of(RoomDao.IDHABITACION, 1, BookingDao.STARTDATE, LocalDate.now(), BookingDao.ENDDATE, LocalDate.now()),
                        createEntityResult(EntityResult.OPERATION_WRONG, ErrorMessages.DATE_FORMAT_ERROR),
                        List.of(
                                (Supplier) () -> {
                                    EntityResult erRoom = new EntityResultMapImpl();
                                    erRoom.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    erRoom.put(RoomDao.IDHABITACION, List.of(0));
                                    erRoom.put(RoomDao.ROOMTYPEID, List.of(1));
                                    erRoom.put(RoomDao.ROOMDOWNDATE, List.of(LocalDate.now().plusDays(10)));
                                    return Mockito.when(daoHelper.query(Mockito.any(RoomDao.class), Mockito.anyMap(), Mockito.anyList())).thenReturn(erRoom);
                                },
                                (Supplier) () -> {
                                    EntityResult erRoomType = new EntityResultMapImpl();
                                    erRoomType.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    erRoomType.put(RoomTypeDao.PRICE, List.of(100.0));
                                    return Mockito.when(daoHelper.query(Mockito.any(RoomTypeDao.class), Mockito.anyMap(), Mockito.anyList())).thenReturn(erRoomType);
                                },
                                (Supplier) () -> {
                                    EntityResult erCriteria = new EntityResultMapImpl();
                                    erCriteria.setCode(EntityResult.OPERATION_SUCCESSFUL);
                                    erCriteria.put(CriteriaDao.ID, criteriaID);
                                    erCriteria.put(CriteriaDao.NAME, List.of("Test", "Test", "Test", "Test", "Test"));
                                    erCriteria.put(CriteriaDao.MULTIPLIER, List.of(new BigDecimal(1), new BigDecimal(1), new BigDecimal(1), new BigDecimal(1), new BigDecimal(1)));
                                    return Mockito.when(daoHelper.query(Mockito.any(CriteriaDao.class), Mockito.anyMap(), Mockito.anyList())).thenReturn(erCriteria);
                                }
                        )
                )
                //endregion

        );
    }

    /**
     * Creates an EntityResult with the given code and message
     *
     * @param code    EntityResult code
     * @param message EntityResult message
     * @return EntityResult
     */
    private static EntityResult createEntityResult(int code, String message) {
        EntityResult er = new EntityResultMapImpl();
        er.setCode(code);
        er.setMessage(message);
        return er;
    }
}
