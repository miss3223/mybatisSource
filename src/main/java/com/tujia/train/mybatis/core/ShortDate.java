package com.tujia.train.mybatis.core;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 表示只有年月日的时间
 * 为了提高效率，使用26个位来存储 无时区 14[yyyy] + 6[mm] + 6[dd]
 * @author jianhong.li Date: 2017-07-16 Time: 6:24 PM
 * @version $Id$
 */


public class ShortDate implements Serializable, Comparable<ShortDate> {

    public static final ShortDate MIN_DATE = ShortDate.valueOf("0001-01-01");
    public static final ShortDate MAX_DATE = ShortDate.valueOf("9999-12-31");
    private static final long serialVersionUID = 8952116329368020820L;

    private int bits;

    /**
     * 用于json序列化初始化, 严禁调用.
     */
    @Deprecated
    public ShortDate() {
    }

    /***
     * 使用yyyy-MM-dd格式字符串初始化
     *
     * @param str
     */
    public ShortDate(String str) {
        if (str == null) throw new IllegalArgumentException("null string");
        if (str.length() != 10) throw new IllegalArgumentException("QunarDate format must be yyyy-MM-dd: " + str);
        int year = Integer.parseInt(str.substring(0, 4));
        int month = Integer.parseInt(str.substring(5, 7));
        int day = Integer.parseInt(str.substring(8, 10));
        init(year,month, day);
    }

    /***
     * 通过给定格式解析date初始化
     *
     * @param date 日期字符串
     * @param format 日期格式
     * @throws ParseException
     */
    @Deprecated
    public ShortDate(String date, String format) throws ParseException {
        SimpleDateFormat df = new SimpleDateFormat(format);
        df.setLenient(false);
        Date parsedDate = df.parse(date);
        int year = parsedDate.getYear() + 1900 ;
        int month = parsedDate.getMonth() +1 ;
        int day = parsedDate.getDate();
        init(year,month, day);
    }

    /***
     * 使用日期对象初始化 注意这里使用了默认时区，如果非默认时区，请调用年月日显示初始化
     *
     * @param date
     */
    public ShortDate(Date date) {
        int year = date.getYear() + 1900 ;
        int month = date.getMonth() +1 ;
        int day = date.getDate();
        init(year,month, day);
    }

    /***
     * 使用年月日初始化
     *
     * @param year
     * @param month
     * @param day
     */
    public ShortDate(int year, int month, int day) {
        init(year, month, day);
    }

    private void init(int year, int month, int day) {
        check(year, 0, 9999, "year is invalid");
        check(month, 1, 12, "month is invalid");
        check(day, 1, 31, "day is invalid");

        bits = year;
        bits <<= 6;
        bits |= month;
        bits <<= 6;
        bits |= day;
    }

    /**
     * @return 返回年份
     */
    public int getYear() {
        return (bits >> 12) & 0x3FFF;
    }

    /**
     * @return 返回月份
     */
    public int getMonth() {
        return (bits >> 6) & 0x3F;
    }

    /***
     * @return 返回天数
     */
    public int getDay() {
        return bits & 0x3F;
    }

    /**
     * 设置年份
     * @see com.tujia.train.mybatis.utils.ShortDateUtils#addYears(ShortDate, int)
     * @param year
     */
    @Deprecated
    public void setYear(int year) {
        check(year, 0, 9999, "year is invalid");
        bits &= ~(0x3FFF << 12); //clear bits..
        bits |= year << 12;
    }


    /**
     * 设置月份
     * @see com.tujia.train.mybatis.utils.ShortDateUtils#addMonths(ShortDate, int)
     * @param month
     */
    @Deprecated
    public void setMonth(int month) {
        check(month, 1, 12, "month is invalid");
        bits &= ~(0x3F << 6);
        bits |= month << 6 ;
    }

    /**
     * 设置天数
     * @param day
     * @see com.tujia.train.mybatis.utils.ShortDateUtils#addDays(ShortDate, int)
     * @return
     */
    @Deprecated
    public void setDay(int day) {
        check(day, 1, 31, "day is invalid");
        bits &= ~(0x3F);
        bits |= day;
    }

    /**
     * 使用默认时区转换为日期对象 时分秒部分默认为00:00:00.000
     *
     * @return
     */
    public Date toDate() {
        return new Date(getYear()-1900,getMonth()-1,getDay());
    }

    @Override
    public int hashCode() {
        return bits;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ShortDate other = (ShortDate) obj;
        if (bits != other.bits)
            return false;
        return true;
    }

    @Override
    public String toString() {
        String yearStr = "";
        int year =getYear();
        if(year <10){
            yearStr ="000"+year;
        }else if(year<100){
            yearStr = "00"+year;
        }else if(year<1000){
            yearStr = "0" +year;
        }else{
            yearStr = String.valueOf(year);
        }

        return yearStr+ "-" + fill(getMonth()) +"-" +fill(getDay());
    }
    private static String fill(int i) {
        if (i < 10) return "0" + i;
        else return Integer.toString(i, 10);
    }
    public boolean after(ShortDate o) {
        return this.toLiteral() > o.toLiteral();
    }

    public boolean before(ShortDate o) {
        return this.toLiteral() < o.toLiteral();
    }

    @Override
    public int compareTo(ShortDate o) {
        int thisVal = this.toLiteral();
        int anotherVal = o.toLiteral();
        return (thisVal < anotherVal ? -1 : (thisVal == anotherVal ? 0 : 1));
    }

    public static ShortDate valueOf(String value) {
        return new ShortDate(value);
    }

    public static ShortDate valueOf(Date date) {
        return new ShortDate(date);
    }

    private int toLiteral() {
        return getYear() * 10000 + getMonth() * 100 + getDay();
    }

    private static void check(int value, int min, int max, String msg) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(String.format("%s: value=%d min=%d max=%d", msg, value, min, max));
        }
    }

}
