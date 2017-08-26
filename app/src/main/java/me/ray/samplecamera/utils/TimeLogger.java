package me.ray.samplecamera.utils;

import android.util.Log;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

/**
 * Created by Ray on 2017/5/10.
 */

public class TimeLogger {

    private static ThreadLocal<HashMap<String, Long>> map = new ThreadLocal<HashMap<String, Long>>(){
        @Override
        protected HashMap<String, Long> initialValue() {
            return new HashMap<>();
        }
    };
    private static ThreadLocal<Calendar> calendarThreadLocal = new ThreadLocal<Calendar>(){
        @Override
        protected Calendar initialValue() {
            return Calendar.getInstance();
        }
    };

    public static void start(String name){
        long current = System.currentTimeMillis();
        map.get().put(name, current);
        Log.d("TimeLogger", name + " start: " + formatTime(current, false));
    }

    public static void start(String name, String extra){
        long current = System.currentTimeMillis();
        map.get().put(name, current);
        Log.d("TimeLogger", name + " start: " + formatTime(current, false) + ", " + extra);
    }

    public static void stop(String name){
        Long start = map.get().get(name);
        if(start != null){
            long end = System.currentTimeMillis();
            Log.d("TimeLogger", name + " last for "  + formatTime(end - start, true) + ", start = " + formatTime(start, false) + ", end = " + formatTime(end, false));
        } else {
            Log.e("TimeLogger", "timer name not exists: " + name);
        }
    }

    public static void stop(String name, String extra){
        Long start = map.get().get(name);
        if(start != null){
            long end = System.currentTimeMillis();
            Log.d("TimeLogger", name + " last for "  + formatTime(end - start, true) + ", start = " + formatTime(start, false) + ", end = " + formatTime(end, false) + ", " + extra);
        } else {
            Log.e("TimeLogger", "timer name not exists: " + name);
        }
    }

    private static String formatTime(long time, boolean ignoreLocal) {
        Calendar calendar = calendarThreadLocal.get();
        calendar.setTimeZone(ignoreLocal ? TimeZone.getTimeZone("UTC+0") : TimeZone.getDefault());
        calendar.setTime(new Date(time));
        return String.valueOf(calendar.get(Calendar.HOUR)) +
                ":" +
                calendar.get(Calendar.MINUTE) +
                ":" +
                calendar.get(Calendar.SECOND) +
                ":" +
                calendar.get(Calendar.MILLISECOND);
    }
}
