package me.ray.samplecamera.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by Ray on 2017/6/15.
 */

public class TimeUtils {

    public final static ThreadLocal<SimpleDateFormat> dateFormater_full = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            format.setTimeZone(TimeZone.getTimeZone("GMT+0"));
            return format;
        }
    };

    public final static ThreadLocal<SimpleDateFormat> dateFormater_full_packed = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
            format.setTimeZone(TimeZone.getTimeZone("GMT+0"));
            return format;
        }
    };

    public final static ThreadLocal<SimpleDateFormat> dateFormater_day = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            format.setTimeZone(TimeZone.getTimeZone("GMT+0"));
            return format;
        }
    };

    public final static ThreadLocal<Calendar> safe_calendar = new ThreadLocal<Calendar>(){
        @Override
        protected Calendar initialValue() {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeZone(TimeZone.getTimeZone("GMT+0"));
            return calendar;
        }
    };

    /**
     * 以友好的方式显示时间
     *
     * @param sdate
     * @return
     */
    public static String friendlyTime(String sdate) {
        Date time;

        if (isInEasternEightZones()) {
            time = toDate(sdate, dateFormater_full.get());
        } else {
            time = transformTime(toDate(sdate, dateFormater_full.get()), TimeZone.getTimeZone("GMT+08"),
                    TimeZone.getDefault());
        }

        if (time == null) {
            return "Unknown";
        }
        String ftime;
        Calendar cal = safe_calendar.get();

        // 判断是否是同一天
        String curDate = dateFormater_day.get().format(cal.getTime());
        String paramDate = dateFormater_day.get().format(time);
        if (curDate.equals(paramDate)) {
            int hour = (int) ((cal.getTimeInMillis() - time.getTime()) / 3600000);
            if (hour == 0)
                ftime = Math.max(
                        (cal.getTimeInMillis() - time.getTime()) / 60000, 1)
                        + "分钟前";
            else
                ftime = hour + "小时前";
            return ftime;
        }

        long lt = time.getTime() / 86400000;
        long ct = cal.getTimeInMillis() / 86400000;
        int days = (int) (ct - lt);
        if (days == 0) {
            int hour = (int) ((cal.getTimeInMillis() - time.getTime()) / 3600000);
            if (hour == 0)
                ftime = Math.max(
                        (cal.getTimeInMillis() - time.getTime()) / 60000, 1)
                        + "分钟前";
            else
                ftime = hour + "小时前";
        } else if (days == 1) {
            ftime = "昨天";
        } else if (days == 2) {
            ftime = "前天 ";
        } else if (days > 2 && days < 31) {
            ftime = days + "天前";
        } else if (days >= 31 && days <= 2 * 31) {
            ftime = "一个月前";
        } else if (days > 2 * 31 && days <= 3 * 31) {
            ftime = "2个月前";
        } else if (days > 3 * 31 && days <= 4 * 31) {
            ftime = "3个月前";
        } else {
            ftime = dateFormater_day.get().format(time);
        }
        return ftime;
    }

    public static String dateToDayString(Date date){
        return dateFormater_day.get().format(date);
    }

    public static String dateToDayString(Date date, TimeZone timeZone){
        SimpleDateFormat format = dateFormater_day.get();
        if(timeZone != null){
            format.setTimeZone(timeZone);
        }
        return format.format(date);
    }

    public static String dateToTimeString(Date date){
        return dateFormater_full.get().format(date);
    }

    /**
     * 将字符串转位日期类型
     */
    public static Date toDay(String sdate, TimeZone timeZone) {
        SimpleDateFormat format = dateFormater_day.get();
        if(timeZone != null){
            format.setTimeZone(timeZone);
        }
        return toDate(sdate, dateFormater_day.get());
    }

    public static Date toDate(String sdate, SimpleDateFormat dateFormater) {
        try {
            return dateFormater.parse(sdate);
        } catch (ParseException e) {
            return null;
        }
    }

    public static Date getNextDays(Date day, int daysAfter){
        return new Date(day.getTime() + (long) daysAfter * 24 * 3600 * 1000);
    }

    /**
     * 判断用户的设备时区是否为东八区（中国） 2014年7月31日
     *
     * @return
     */
    public static boolean isInEasternEightZones() {
        boolean defaultVaule;
        defaultVaule = TimeZone.getDefault() == TimeZone.getTimeZone("GMT+08");
        return defaultVaule;
    }

    /**
     * 根据不同时区，转换时间 2014年7月31日
     *
     * @param date
     * @return
     */
    public static Date transformTime(Date date, TimeZone oldZone,
                                     TimeZone newZone) {
        Date finalDate = null;
        if (date != null) {
            int timeOffset = oldZone.getOffset(date.getTime())
                    - newZone.getOffset(date.getTime());
            finalDate = new Date(date.getTime() - timeOffset);
        }
        return finalDate;
    }

    public static List<Date> rangedToListedDate(Date after, Date before){
        List<Date> list = new ArrayList<>();
        for(Date day = after; !isSameDay(day, before); day = getNextDays(day, 1)){
            list.add(day);
        }
        return list;
    }

    /**
     * 判断是否在同一天
     */
    public static boolean isSameDay(Date date1, Date date2){
        Calendar calendar = safe_calendar.get();
        calendar.setTime(date1);
        int year1 = calendar.get(Calendar.YEAR);
        int day1 = calendar.get(Calendar.DAY_OF_YEAR);
        calendar.setTime(date2);
        int year2 = calendar.get(Calendar.YEAR);
        int day2 = calendar.get(Calendar.DAY_OF_YEAR);
        return year1 == year2 && day1 == day2;
    }


    public static final long ONE_DAY_MILLISECONDS = 3600 * 24 * 1000;
    /**
     * 获取两个长整形日期间的间隔，单位为日
     * @return 间隔日
     */
    public static int daysBetween(Date date1, Date date2){
        int plusNonFullDay = 0;
        if((date1.getTime() - date2.getTime()) % ONE_DAY_MILLISECONDS != 0){
            plusNonFullDay = 1;
        }
        return Math.abs((int)((date1.getTime() - date2.getTime()) / ONE_DAY_MILLISECONDS)) + plusNonFullDay;
    }


    public static int getMonth(Date date, TimeZone timeZone) {
        Calendar calendar = safe_calendar.get();
        calendar.setTimeZone(timeZone);
        calendar.setTime(date);
        return calendar.get(Calendar.MONTH) + 1;
    }

    public static int getDay(Date date, TimeZone timeZone){
        Calendar calendar = safe_calendar.get();
        calendar.setTimeZone(timeZone);
        calendar.setTime(date);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    public static Date beginningOfDay(Date dayBefore, TimeZone timeZone) {
        Calendar calendar = safe_calendar.get();
        calendar.setTimeZone(timeZone);
        calendar.setTime(dayBefore);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
}
