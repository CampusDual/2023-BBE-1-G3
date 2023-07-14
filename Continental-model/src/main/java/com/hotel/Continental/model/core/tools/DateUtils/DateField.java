package com.hotel.continental.model.core.tools.DateUtils;

import java.time.LocalDate;
import java.util.List;

public class DateField {
    private LocalDate date;
    private List<Integer> days;
    private Integer day;

    public DateField(LocalDate date) {
        this.date = date;
    }

    public DateField(List<Integer> days) {
        this.days = days;
    }
    public DateField(Integer day) {
        this.day = day;
    }

    public LocalDate getDate() {
        return date;
    }

    public List<Integer> getDays() {
        return days;
    }
    public Integer getDay() {
        return day;
    }

    public Object getValue() {
        if (date != null) {
            return date;
        } else if (days != null) {
            return days;
        } else if (day != null) {
            return day;
        }
        return null;
    }
}
