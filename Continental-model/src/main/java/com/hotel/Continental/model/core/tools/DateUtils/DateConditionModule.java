package com.hotel.continental.model.core.tools.DateUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class DateConditionModule {
    public static SimpleModule getModule() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(DateCondition.class, new DateConditionSerializer());
        module.addDeserializer(DateCondition.class, new DateConditionDeserializer());
        module.addSerializer(DateField.class, new DateFieldSerializer());
        module.addDeserializer(DateField.class, new DateFieldDeserializer());
        return module;
    }

    public static class DateConditionSerializer extends JsonSerializer<DateCondition> {
        @Override
        public void serialize(DateCondition dateCondition, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectField("dateFieldLeft", dateCondition.getDateFieldLeft());
            jsonGenerator.writeObjectField("dateFieldRight", dateCondition.getDateFieldRight());
            jsonGenerator.writeObjectField("operator", dateCondition.getOperator());
            jsonGenerator.writeObjectField("daysField", dateCondition.getDaysField());
            jsonGenerator.writeEndObject();
        }
    }

    public static class DateConditionDeserializer extends JsonDeserializer<DateCondition> {
        @Override
        public DateCondition deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
            DateField dateFieldLeft = null;
            DateField dateFieldRight = null;
            DateOperator operator = null;
            DateField daysField = null;
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = jsonParser.getCurrentName();

                if ("dateFieldLeft".equals(fieldName)) {
                    jsonParser.nextToken();
                    dateFieldLeft = jsonParser.readValueAs(DateField.class);
                } else if ("dateFieldRight".equals(fieldName)) {
                    jsonParser.nextToken();
                    dateFieldRight = jsonParser.readValueAs(DateField.class);
                } else if ("operator".equals(fieldName)) {
                    jsonParser.nextToken();
                    operator = DateOperator.valueOf(jsonParser.getValueAsString());
                } else if ("daysField".equals(fieldName)) {
                    jsonParser.nextToken();
                    daysField = jsonParser.readValueAs(DateField.class);
                }
            }

            return new DateCondition(dateFieldLeft, operator, dateFieldRight, daysField);
        }
    }

    public static class DateFieldDeserializer extends JsonDeserializer<DateField> {
        @Override
        public DateField deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
            JsonToken currentToken = jsonParser.getCurrentToken();
            if (currentToken == JsonToken.VALUE_NUMBER_INT) {
                return new DateField(jsonParser.getIntValue());
            } else if (currentToken == JsonToken.START_ARRAY) {
                List<Integer> days = new ArrayList<>();
                while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
                    if (jsonParser.getCurrentToken() == JsonToken.VALUE_NUMBER_INT) {
                        days.add(jsonParser.getIntValue());
                    }
                }
                return new DateField(days);
            } else if (currentToken == JsonToken.VALUE_STRING) {
                String dateString = jsonParser.readValueAs(String.class);
                try {
                    LocalDate date = LocalDate.parse(dateString);
                    return new DateField(date);
                } catch (DateTimeParseException e) {
                    // Handle parsing error
                    throw new IOException("Invalid date format: " + dateString, e);
                }
            }
            return null;
        }
    }



    public static class DateFieldSerializer extends JsonSerializer<DateField> {
        @Override
        public void serialize(DateField dateField, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            Object value = dateField.getValue();
            if (value instanceof LocalDate) {
                LocalDate date = (LocalDate) value;
                jsonGenerator.writeString(date.toString());
            } else if (value instanceof List) {
                List<Integer> days = (List<Integer>) value;
                jsonGenerator.writeStartArray();
                for (Integer day : days) {
                    jsonGenerator.writeNumber(day);
                }
                jsonGenerator.writeEndArray();
            }else if (value instanceof Integer) {
                Integer day = (Integer) value;
                jsonGenerator.writeNumber(day);
            }
        }
    }

}
