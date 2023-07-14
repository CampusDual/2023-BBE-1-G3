package com.hotel.continental.model.core.tools;

import com.fasterxml.jackson.databind.*;
import com.hotel.continental.model.core.tools.DateUtils.DateCondition;
import com.hotel.continental.model.core.tools.DateUtils.DateConditionModule;
import com.hotel.continental.model.core.tools.DateUtils.DateField;
import com.hotel.continental.model.core.tools.DateUtils.DateOperator;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

class test {
    public static void main(String[] args) {
        List<DateCondition> lista = new ArrayList<>();
        //DateCondition que comprueba si es entre el 1 del 6 y el 31 del 8
        DateCondition dateConditionHighSeason = new DateCondition(new DateField(LocalDate.of(2020, 6, 1)), DateOperator.DATE_BETWEEN_SEASON, new DateField(LocalDate.of(2020, 8, 31)), null);
        lista.add(dateConditionHighSeason);
        //DateCondition que comprueba si es entre el 1 del 9 y el 30 del 11
        DateCondition dateConditionLowSeason = new DateCondition(new DateField(LocalDate.of(2020, 9, 1)), DateOperator.DATE_BETWEEN_SEASON, new DateField(LocalDate.of(2020, 11, 30)), null);
        lista.add(dateConditionLowSeason);
        //DateCondition que comprueba si la reserva se realizo con 10 dias de antelacion
        DateCondition dateCondition10DaysBefore = new DateCondition(null, DateOperator.DAYS_BEFORE, null, new DateField(10));
        lista.add(dateCondition10DaysBefore);
        System.out.println(dateCondition10DaysBefore.evaluate(LocalDate.of(2023, 7, 20), LocalDate.of(2024, 6, 30)));
        //DateCondition que comprueba si la reserva dura mas de 5 dias
        DateCondition dateCondition5DaysBetween = new DateCondition(null, DateOperator.DAYS_BETWEEN, null, new DateField(5));
        lista.add(dateCondition5DaysBetween);
        System.out.println(dateCondition5DaysBetween.evaluate(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 6, 30)));
        //DateCondition que comprueba si el dia es sabado o domingo
        DateCondition dateConditionSunday = new DateCondition(null, DateOperator.DAY_OF_WEEK, null, new DateField(List.of(6,7)));
        lista.add(dateConditionSunday);

        //Serializacion
        DateConditionModule dateConditionModule = new DateConditionModule();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(dateConditionModule.getModule());
        List<String> jsonList = new ArrayList<>();
        try {
            for (DateCondition dateCondition : lista) {
                String json = objectMapper.writeValueAsString(dateCondition);
                System.out.println(json);
                jsonList.add(json);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Deserializacion
        try {
            for (String json : jsonList) {
                DateCondition dateCondition = objectMapper.readValue(json, DateCondition.class);
                System.out.println(dateCondition);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


