package com.example.wifimanager.miwifi.DO;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * MiWifiStatus
 * <br />
 *
 * @author Jiashun Zhang
 * @since 2021-05-06
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MiWifiStatusDO extends MiWifiBaseDO {
    DeviceCount count;
    CPU cpu;
    int temperature;
    String upTime;
    WAN wan;
    MemoryUsage mem;
    Hardware hardware;
    List<Device> dev;


    @Data
    public static class Device{
        String devname;
        String download;
        String downspeed;
        String mac;
        String maxdownloadspeed;
        String maxuploadspeed;
        String online;
        String upload;
        String upspeed;
    }

    @Data
    public static class Hardware{
        String channel;
        String mac;
        String platform;
        String sn;
        String version;
    }
    @Data
    public static class MemoryUsage{
        String hz;
        String total;
        String type;
        double usage;
    }
    @Data
    public static class WAN{
        String devname;
        String download;
        String downspeed;
        String history;
        String maxdownloadspeed;
        String maxuploadspeed;
        String upload;
        String upspeed;
    }
    @Data
    public static class DeviceCount{
        int all;
        int online;
    }
    @Data
    public static class CPU{
        int core;
        String  hz;
        double load;
    }
}
