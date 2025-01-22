package com.example.wifimanager.miwifi.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * xiaomi-route-api
 * <br />
 *
 * @author: Jiashun Zhang
 * @since: 2021-08-14
 */
@Data
public class MiWifiRouterStatus implements Serializable {
    /**
     * 路由器温度 部分路由器不支持始终显示为0；
     */
    int temperature;
    /**
     * 在线终端数
     */
    int onlineCount;
    /**
     * 累计终端数
     */
    int allCount;
    /**
     * cpu核心数；
     */
    int cpuCore;
    /**
     * CPU核心频率
     */
    String  cpuHz;
    /**
     * CPU 负载
     */
    double cpuLoad;
    /**
     * 系统启动时间
     */
    LocalDateTime upTime;

    String channel;//: "release"
    String mac;//: "50:D2:F5:22:A3:D7"
    String platform;//: "R4A"
    String sn;//: "21894/20490938"
    String version;//: "2.28.38"


    String memHz;//: "1200MHz"
    String memSize;//: "128MB"
    String memType;//: "DDR3"
    double memUsage;//: 0.42

    String wanName;
    long downloadSize;
    int downloadSpeed;
    long uploadSize;
    int uploadSpeed;
//    List<Long> history
int maxDownloadSpeed;
    int maxUploadSpeed;

    List<ConnDevice> deviceList;

    @Data
    public static class  ConnDevice{
        String name;//: "SC-202005232300"
        long downloadSize;//: "4107003368"
        int downSpeed;//: "683483"
        String mac;//: "2C:F0:5D:2D:03:62"
        int maxDownloadSpeed;//: "5393048"
        int maxUploadSpeed;//: "4565332"
        int onlineSecond;//: "17165"
        long uploadSize;//: "2192519160"
        int uploadSpeed;//: "482305"
    }
}
