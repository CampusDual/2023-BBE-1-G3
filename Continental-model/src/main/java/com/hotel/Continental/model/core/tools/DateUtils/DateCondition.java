package com.hotel.continental.model.core.tools.DateUtils;

import org.apache.poi.ss.formula.functions.Days;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import static java.time.temporal.ChronoUnit.DAYS;

public class DateCondition {
    private DateField dateFieldLeft;
    private DateField dateFieldRight;
    private DateField daysField;
    private DateOperator operator;

    /**
     * Constructor para serializacion
     * Dependiendo del operador se requieren diferentes parametros
     * DATE_BETWEEN: dateFieldLeft, dateFieldRight
     * EQUALS: dateFieldLeft
     * BEFORE: dateFieldLeft
     * AFTER: dateFieldLeft
     * DAY_OF_WEEK: daysField
     * DAYS_BEFORE: daysField
     * DAYS_BETWEEN: dateFieldLeft, dateFieldRight, daysField
     *
     * @param dateFieldLeft  fecha izquierda
     * @param operator       operador
     * @param dateFieldRight fecha derecha
     * @param daysField      dias
     */
    public DateCondition(DateField dateFieldLeft, DateOperator operator, DateField dateFieldRight, DateField daysField) {
        validateArguments(operator, dateFieldLeft, dateFieldRight, daysField);
        this.dateFieldLeft = dateFieldLeft;
        this.dateFieldRight = dateFieldRight;
        this.daysField = daysField;
        this.operator = operator;
    }


    public DateField getDateFieldLeft() {
        return dateFieldLeft;
    }

    public DateField getDateFieldRight() {
        return dateFieldRight;
    }

    public DateOperator getOperator() {
        return operator;
    }

    public DateField getDaysField() {
        return daysField;
    }

    @Override
    public String toString() {
        return "DateCondition{" +
                "dateFieldLeft=" + dateFieldLeft +
                ", dateFieldRight=" + dateFieldRight +
                ", operator=" + operator +
                '}';
    }

    /**
     * Evalua la condicion
     *
     * @param initialDate fecha a evaluar,en algunos casos es ignorada
     * @return true si la condicion se cumple, false en caso contrario
     */
    public boolean evaluate(LocalDate initialDate,LocalDate endDate) {
        Object leftValue = dateFieldLeft != null ? dateFieldLeft.getValue() : null;
        Object rightValue = dateFieldRight != null ? dateFieldRight.getValue() : null;
        Object daysValue = daysField != null ? daysField.getValue() : null;

        switch (operator) {
            case DATE_BETWEEN:
                if (leftValue instanceof LocalDate && rightValue instanceof LocalDate) {
                    LocalDate leftDate = (LocalDate) leftValue;
                    LocalDate rightDate = (LocalDate) rightValue;
                    return initialDate.compareTo(leftDate) >= 0 && initialDate.compareTo(rightDate) <= 0;
                }
                break;
            case DATE_BETWEEN_SEASON:
                if (leftValue instanceof LocalDate && rightValue instanceof LocalDate) {
                    LocalDate leftDate = (LocalDate) leftValue;
                    LocalDate rightDate = (LocalDate) rightValue;
                    int leftMonth = leftDate.getMonthValue();
                    int leftDay = leftDate.getDayOfMonth();
                    int rightMonth = rightDate.getMonthValue();
                    int rightDay = rightDate.getDayOfMonth();
                    int currentMonth = initialDate.getMonthValue();
                    int currentDay = initialDate.getDayOfMonth();
                    return (currentMonth > leftMonth || (currentMonth == leftMonth && currentDay >= leftDay))
                            && (currentMonth < rightMonth || (currentMonth == rightMonth && currentDay <= rightDay));
                }
                break;
            case EQUALS:
                if (leftValue instanceof LocalDate) {
                    LocalDate leftDate = (LocalDate) leftValue;
                    return initialDate.equals(leftDate);
                }
                break;
            case BEFORE:
                if (leftValue instanceof LocalDate) {
                    LocalDate leftDate = (LocalDate) leftValue;
                    return initialDate.compareTo(leftDate) < 0;
                }
                break;
            case AFTER:
                if (leftValue instanceof LocalDate) {
                    LocalDate leftDate = (LocalDate) leftValue;
                    return initialDate.compareTo(leftDate) > 0;
                }
                break;
            case DAY_OF_WEEK:
                if (daysValue instanceof List) {
                    List<Integer> dayOfWeekList = (List<Integer>) daysValue;
                    int currentDayOfWeek = initialDate.getDayOfWeek().getValue();
                    return dayOfWeekList.contains(currentDayOfWeek);
                }
                break;
            case DAYS_BEFORE:
                if (daysValue instanceof Integer) {
                    int daysBefore = (int) daysValue;
                    boolean result = initialDate.compareTo(LocalDate.now().plusDays(daysBefore)) >= 0;
                    return result;
                }
                break;
            case DAYS_BETWEEN:
                if (daysValue instanceof Integer) {
                    //Calcular la diferencia entre leftValue y rightValue
                    int daysBetween = (int) daysValue;
                    Long dias = DAYS.between(initialDate, endDate.plusDays(1));
                    return dias>=daysBetween;
                }
                break;
        }

        return false;
    }

    protected void validateArguments(DateOperator operator, DateField dateFieldLeft, DateField dateFieldRight, DateField daysField) {
        if (operator == null) {
            throw new IllegalArgumentException("The operator is required.");
        }
        if (operator == DateOperator.DATE_BETWEEN && (dateFieldLeft == null || dateFieldRight == null)) {
            throw new IllegalArgumentException("The " + operator + " operator requires two date fields.");
        }
        if (operator == DateOperator.DAYS_BEFORE && daysField == null) {
            throw new IllegalArgumentException("The " + operator + " operator requires days field.");
        }
        if (operator == DateOperator.DAYS_BETWEEN && daysField == null) {
            throw new IllegalArgumentException("The " + operator + " operator requires  days field.");
        }
        if (operator == DateOperator.EQUALS && dateFieldLeft == null) {
            throw new IllegalArgumentException("The " + operator + " operator requires a dateFieldLeft field.");
        }
        if (operator == DateOperator.BEFORE && dateFieldLeft == null) {
            throw new IllegalArgumentException("The " + operator + " operator requires a dateFieldLeft field.");
        }
        if (operator == DateOperator.AFTER && dateFieldLeft == null) {
            throw new IllegalArgumentException("The " + operator + " operator requires a dateFieldLeft field.");
        }
        if (operator == DateOperator.DAY_OF_WEEK && daysField == null && daysField.getDays() == null) {
            throw new IllegalArgumentException("The " + operator + " operator requires a days field of type List<Integer>.");
        }
        if (operator == DateOperator.DATE_BETWEEN_SEASON && (dateFieldLeft == null || dateFieldRight == null)) {
            throw new IllegalArgumentException("The " + operator + " operator requires dateFieldLeft & dateFieldRight fields.");
        }
    }

}
