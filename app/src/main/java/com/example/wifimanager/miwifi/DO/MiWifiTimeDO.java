package com.example.wifimanager.miwifi.DO;

import lombok.Data;
import lombok.EqualsAndHashCode;


@EqualsAndHashCode(callSuper = true)
@Data
public class MiWifiTimeDO extends MiWifiBaseDO {
    Time time;
    @Data
    public static class Time{
        int day;
        int hour;
        int index;
        int min;
        int month;
        int sec;
        String timezone;
        int year;
    }
}
