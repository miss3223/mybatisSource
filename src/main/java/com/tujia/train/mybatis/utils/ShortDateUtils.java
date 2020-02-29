package com.tujia.train.mybatis.utils;

import com.tujia.train.mybatis.core.ShortDate;
import com.tujia.train.mybatis.core.WeekDayType;

import java.util.*;

/**
 * @author jianhong.li Date: 2017-07-16 Time: 6:26 PM
 * @version $Id$
 */
public class ShortDateUtils {
    public static ShortDate addDays(ShortDate shortDate, int delta) {
        Date date = shortDate.toDate();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_YEAR, delta);
        return new ShortDate(cal.getTime());
    }

    public static ShortDate addMonths(ShortDate shortDate, int delta) {
        Date date = shortDate.toDate();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.MONTH, delta);
        return new ShortDate(cal.getTime());
    }

    public static ShortDate addYears(ShortDate shortDate, int delta) {
        Date date = shortDate.toDate();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.YEAR, delta);
        return new ShortDate(cal.getTime());
    }

    public static int diffDays(ShortDate shortDate1, ShortDate shortDate2) {
        long time1 = shortDate1.toDate().getTime();
        long time2 = shortDate2.toDate().getTime();
        long span = time2 - time1;
        return (int) (span / (1000L * 3600L * 24L));
    }

    public static WeekDayType getWeekDay(ShortDate shortDate) {
        return WeekDayType.weekOf(shortDate.toDate());
    }

    public static Set<WeekDayType> map(ShortDate fromDate, ShortDate toDate) {
        Set<WeekDayType> set = new HashSet<WeekDayType>();
        for (ShortDate shortDate = fromDate; !shortDate.after(toDate); shortDate = addDays(shortDate, 1)) {
            set.add(getWeekDay(shortDate));
        }
        return set;
    }

    public static List<ShortDate> mapEffectiveList(ShortDate fromDate, ShortDate toDate) {
        List<ShortDate> effectiveList = new ArrayList<ShortDate>();
        if (fromDate.after(toDate)) {
            return effectiveList;
        }
        for (ShortDate date = fromDate; !date.after(toDate); date = ShortDateUtils.addDays(date, 1)) {
            effectiveList.add(date);
        }
        return effectiveList;
    }

    public static boolean isValidShortDate(String shortDate) {
        try {
            new ShortDate(shortDate);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static ShortDate findFirstDate(List<ShortDate> shortDateList){
        if(shortDateList == null || shortDateList.size() == 0){
            return null;
        }
        Collections.sort(shortDateList);
        return shortDateList.get(0);
    }
}
