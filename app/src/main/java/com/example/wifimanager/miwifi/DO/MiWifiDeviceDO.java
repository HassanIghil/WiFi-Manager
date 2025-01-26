package com.example.wifimanager.miwifi.DO;

import lombok.Data;

import java.util.List;

@Data
public class MiWifiDeviceDO {
    String name;
    String mac;
    int online;
    Statistics statistics;
    Authority authority;
    String icon;
    List<IP> ip;
    int isap;
    String oname;
    String parent;
    int push;
    int times;
    int type;

    @Data
    public static class IP {
        int active;
        String downspeed;
        String ip;
        String online;
        String upspeed;
    }

    @Data
    public static class Authority {
        int admin;
        int lan;
        int pridisk;
        int wan;
    }

    @Data
    public static class Statistics {
        String downspeed;
        String online;
        String upspeed;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MiWifiDeviceDO) {
            MiWifiDeviceDO device = (MiWifiDeviceDO) o;
            return this.mac.equals(device.mac);
        }
        return super.equals(o);
    }
}
