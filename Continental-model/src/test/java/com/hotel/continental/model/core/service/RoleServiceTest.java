package com.hotel.continental.model.core.service;

import com.hotel.continental.model.core.dao.RoleDao;
import com.hotel.continental.model.core.tools.ErrorMessages;
import com.ontimize.jee.common.dto.EntityResult;
import com.ontimize.jee.common.dto.EntityResultMapImpl;
import com.ontimize.jee.server.dao.DefaultOntimizeDaoHelper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RoleServiceTest {
    //Generame los test para el metodo roleQuery
    @Mock
    DefaultOntimizeDaoHelper daoHelper;
    @InjectMocks
    RoleService roleService;

    @Mock
    RoleDao roleDao;

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class TestRoleQuery {
        @ParameterizedTest()
        @ArgumentsSource(queryGoodWithAndWithoutFilter.class)
        @DisplayName("Test role query good with and without filter")
        void testRoleQueryGoodWithAndWithoutFilter(HashMap<String, Object> keyMap) {
            EntityResult er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_SUCCESSFUL);
            er.put(RoleDao.ROLENAME, List.of("admin"));

            List<Object> attr = new ArrayList<>();
            attr.add(RoleDao.ID_ROLENAME);
            attr.add(RoleDao.ROLENAME);

            when(daoHelper.query(any(RoleDao.class), anyMap(), anyList())).thenReturn(er);

            EntityResult result = roleService.roleQuery(keyMap, attr);
            Assertions.assertEquals(EntityResult.OPERATION_SUCCESSFUL, result.getCode());
        }

        @Test
        @DisplayName("Test role query bad without columns ")
        void testRoleQueryBadWithoutColumns() {
            EntityResult er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_WRONG);

            Map<String, Object> keyMap = new HashMap<>();
            keyMap.put(RoleDao.ROLENAME,"admin");

            List<Object> attr = new ArrayList<>();

            when(daoHelper.query(any(RoleDao.class), anyMap(), anyList())).thenReturn(er);

            EntityResult result = roleService.roleQuery(keyMap, attr);
            Assertions.assertEquals(EntityResult.OPERATION_WRONG, result.getCode());
        }
    }
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class TestRoleInsert {
        @Test
        @DisplayName("Test role insert good")
        void testRoleInsertGood() {
            EntityResult er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_SUCCESSFUL);

            EntityResult erQuery = new EntityResultMapImpl();
            erQuery.setCode(EntityResult.OPERATION_SUCCESSFUL);

            Map<String, Object> attr = new HashMap<>();
            attr.put(RoleDao.ROLENAME,"admin");

            //el insert comprueba a traves de un query que no existe asique devolvemos un entity result okey+vacio
            when(daoHelper.query(any(RoleDao.class), anyMap(), anyList())).thenReturn(erQuery);
            //Ejecutamos el insert
            when(daoHelper.insert(any(RoleDao.class), anyMap())).thenReturn(er);

            EntityResult result = roleService.roleInsert(attr);
            Assertions.assertEquals(EntityResult.OPERATION_SUCCESSFUL, result.getCode());
        }

        @ParameterizedTest
        @ArgumentsSource(hashMapsNullAndEmptyInsert.class)
        @DisplayName("Test role insert null data & empty data")
        void testRoleInsertNullDataAndEmptyData(HashMap<String, Object> attr) {
            //Ejecutamos el insert, al no tener datos nos va petar sin llegar a tener que mockear nada
            EntityResult result = roleService.roleInsert(attr);

            Assertions.assertEquals(EntityResult.OPERATION_WRONG, result.getCode());
            Assertions.assertEquals(ErrorMessages.NECESSARY_DATA, result.getMessage());
        }

        @Test
        @DisplayName("Test role insert duplicated data")
        void testRoleInsertDuplicatedData() {
            EntityResult erQuery=new EntityResultMapImpl();
            erQuery.put(RoleDao.ROLENAME, List.of("admin"));
            erQuery.put(RoleDao.ID_ROLENAME, List.of(1));
            erQuery.setCode(EntityResult.OPERATION_SUCCESSFUL);

            Map<String, Object> attr = new HashMap<>();
            attr.put(RoleDao.ROLENAME,"admin");

            //el insert comprueba a traves de un query que no existe asique devolvemos un entity result okey+vacio
            when(daoHelper.query(any(RoleDao.class), anyMap(), anyList())).thenReturn(erQuery);
            //no hace falta mockear el insert porque no llega a el,dado qeu el error salta antes

            EntityResult result = roleService.roleInsert(attr);
            Assertions.assertEquals(EntityResult.OPERATION_WRONG, result.getCode());
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class TestRoleDelete {
        @Test
        @DisplayName("Test role delete good")
        void testRoleDeleteGood() {
            EntityResult er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_SUCCESSFUL);

            EntityResult erQuery = new EntityResultMapImpl();
            erQuery.setCode(EntityResult.OPERATION_SUCCESSFUL);
            erQuery.put(RoleDao.ID_ROLENAME, List.of(1));

            Map<String, Object> attr = new HashMap<>();
            attr.put(RoleDao.ID_ROLENAME, 1);

            //el delete comprueba a traves de un query que existe asique devolvemos un entity result okey + 1 resultado
            when(daoHelper.query(any(RoleDao.class), anyMap(), anyList())).thenReturn(erQuery);
            //Ejecutamos el delete
            when(daoHelper.delete(any(RoleDao.class), anyMap())).thenReturn(er);

            EntityResult result = roleService.roleDelete(attr);
            Assertions.assertEquals(EntityResult.OPERATION_SUCCESSFUL, result.getCode());
        }

        @ParameterizedTest
        @ArgumentsSource(testRoleDeleteNotExistKeyAndNullKey.class)
        @DisplayName("Test role delete not exist key and null key")
        void testRoleDeleteNotExistKeyAndNullKey(HashMap<String, Object> attr) {
            //Ejecutamos el delete, al no existir la clave nos va petar sin llegar a tener que mockear nada
            EntityResult result = roleService.roleDelete(attr);

            Assertions.assertEquals(EntityResult.OPERATION_WRONG, result.getCode());
            Assertions.assertEquals(ErrorMessages.NECESSARY_KEY, result.getMessage());
        }

        @Test
        @DisplayName("Test role delete admin")
        void testRoleDeleteAdmin() {
            Map<String, Object> attr = new HashMap<>();
            attr.put(RoleDao.ID_ROLENAME, 0);
            //Ejecutamos el delete, da error porque se esta intentando borrar el admin
            EntityResult result = roleService.roleDelete(attr);
            Assertions.assertEquals(EntityResult.OPERATION_WRONG, result.getCode());
        }
    }
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class TestRoleUpdate {
        @Test
        @DisplayName("Test role update good")
        void testRoleUpdateGood() {
            //Query resultado
            EntityResult er = new EntityResultMapImpl();
            er.setCode(EntityResult.OPERATION_SUCCESSFUL);
            //Query buscar id, correcta+1 resultado
            EntityResult erQueryID = new EntityResultMapImpl();
            erQueryID.setCode(EntityResult.OPERATION_SUCCESSFUL);
            erQueryID.put(RoleDao.ID_ROLENAME, List.of(1));
            //Query buscar rolename, correcta+ 0 resultado
            EntityResult erQueryRolename=new EntityResultMapImpl();
            erQueryRolename.setCode(EntityResult.OPERATION_SUCCESSFUL);
            Map<String, Object> attr = new HashMap<>();
            attr.put(RoleDao.ROLENAME,"admin");

            Map<String, Object> keyMap = new HashMap<>();
            keyMap.put(RoleDao.ID_ROLENAME,1);
            //el insert comprueba a traves de un query que existe asique devolvemos un entity result okey+1 resultado
            when(daoHelper.query(any(RoleDao.class), anyMap(), anyList())).thenReturn(erQueryID).thenReturn(erQueryRolename);
            //Ejecutamos el insert
            when(daoHelper.update(any(RoleDao.class), anyMap(),anyMap())).thenReturn(er);
            EntityResult result = roleService.roleUpdate(attr, keyMap);
            Assertions.assertEquals(EntityResult.OPERATION_SUCCESSFUL, result.getCode());
        }

        @ParameterizedTest
        @ArgumentsSource(testRoleUpdateNullDataAndNullKey.class)
        @DisplayName("Test role update null key and null data")
        void testRoleUpdateNullDataAndNullKey(HashMap<String, Object> keyMap, String errorMessage) {
            Map<String, Object> attr = new HashMap<>();
            //Ejecutamos el update, al no tener datos nos va petar sin llegar a tener que mockear nada
            EntityResult result = roleService.roleUpdate(attr, keyMap);

            Assertions.assertEquals(EntityResult.OPERATION_WRONG, result.getCode());
            Assertions.assertEquals(errorMessage, result.getMessage());
        }

        @Test
        @DisplayName("Test role update empty data")
        void testRoleUpdateEmptyData() {
            Map<String, Object> attr = new HashMap<>();
            attr.put(RoleDao.ROLENAME,"");
            Map<String, Object> keyMap = new HashMap<>();
            keyMap.put(RoleDao.ID_ROLENAME,1);
            //Ejecutamos el insert, al no tener datos nos va petar sin llegar a tener que mockear nada
            EntityResult result = roleService.roleInsert(attr);

            Assertions.assertEquals(EntityResult.OPERATION_WRONG, result.getCode());
            Assertions.assertEquals(ErrorMessages.NECESSARY_DATA, result.getMessage());
        }

        @Test
        @DisplayName("Test role update duplicated data")
        void testRoleUpdateDuplicatedData() {
            EntityResult erQuery=new EntityResultMapImpl();
            erQuery.put(RoleDao.ROLENAME, List.of("admin"));
            erQuery.put(RoleDao.ID_ROLENAME, List.of(1));
            erQuery.setCode(EntityResult.OPERATION_SUCCESSFUL);

            Map<String, Object> attr = new HashMap<>();
            attr.put(RoleDao.ROLENAME,"admin");
            //el insert comprueba a traves de un query que no existe asique devolvemos un entity result okey+vacio
            when(daoHelper.query(any(RoleDao.class), anyMap(), anyList())).thenReturn(erQuery);
            //no hace falta mockear el insert porque no llega a el,dado qeu el error salta antes
            EntityResult result = roleService.roleInsert(attr);
            Assertions.assertEquals(EntityResult.OPERATION_WRONG, result.getCode());
        }
    }

    public static class hashMapsNullAndEmptyInsert implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(new HashMap<String, Object>() {{
                put(null, null);
            }},new HashMap<String, Object>()).map(Arguments::of);
        }
    }

    public static class queryGoodWithAndWithoutFilter implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(new HashMap<String, Object>() {{
                put(RoleDao.ROLENAME, "admin");
            }},new HashMap<String, Object>()).map(Arguments::of);
        }
    }

    public static class testRoleDeleteNotExistKeyAndNullKey implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(new HashMap<String, Object>() {{
                put(RoleDao.ROLENAME, 1);
            }},new HashMap<String, Object>()).map(Arguments::of);
        }
    }

    public static class testRoleUpdateNullDataAndNullKey implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(Arguments.of(new HashMap<String, Object>() {{
                put(RoleDao.ID_ROLENAME, 1);
            }}, ErrorMessages.NECESSARY_DATA),
                    Arguments.of(new HashMap<String, Object>(),
                            ErrorMessages.NECESSARY_KEY));
        }
    }
}
