package com.tujia.train.mybatis.core;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

/**
 * @author jianhong.li Date: 2017-07-16 Time: 6:27 PM
 * @version $Id$
 */
public enum WeekDayType implements Serializable {
    SUNDAY(0, "周日"),
    MONDAY(1, "周一"),
    TUESDAY(2, "周二"),
    WEDNESDAY(3, "周三"),
    THURSDAY(4, "周四"),
    FRIDAY(5, "周五"),
    SATURDAY( 6, "周六");

    private int code;
    private String name;

    public final int getCode() {
        return code;
    }

    public final String getName() {
        return name;
    }

    private WeekDayType(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static WeekDayType codeOf(int code) {
        for (WeekDayType weekDayType : values()) {
            if (weekDayType.code == code) {
                return weekDayType;
            }
        }
        throw new IllegalArgumentException("Invalid weekDayType code: " + code);
    }

    public static WeekDayType weekOf(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return codeOf((cal.get((Calendar.DAY_OF_WEEK)) - 1) % 7 ) ;
    }

    public static WeekDayType weekOf(ShortDate date) {
        return weekOf(date.toDate());
    }
}