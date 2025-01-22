package com.example.wifimanager.miwifi.model;



import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;


@Data
public class MiWifiDevice implements Serializable {
    /**
     * 设备备注名
     */
    private String remarkName;
    /**
     * 设备名
     */
    private String name;
    /**
     * 设备 mac 地址
     */
    private String mac;
    /**
     * 是否在线
     */
    boolean isOnline;
    /**
     * 是否ap
     */
    boolean isAP;
    /**
     * 设备 ip 地址
     */
    String ip;
    /**
     * 当前下载速度
     */
    int downloadSpeed;

    /**
     * 在线时长
     */
    int onlineSecond;
    /**
     * 上线时间
     */
    LocalDateTime upTime;
    /**
     * 上传速度
     */
    int uploadSpeed;

    ConnType connType;

    boolean allowAdmin;
    boolean allowLan;
    boolean allowPriDisk;
    boolean allowWan;
    /**
     * 设备连接类型。
     */
    public  enum ConnType {
        /**
         * 网线连接
         */
        ETH(0),
        /**
         * 2.4G wifi 连接
         */
        WIFI2_4G(1),
        /**
         * 5G wifi 连接
         */
        WIFI5G(2),
        /**
         * 未知连接
         */
        UNKNOWN(-1);
        int type;
        public static ConnType mapType(int type){
            switch (type){
                case 0:
                    return ConnType.ETH;
                case 1:
                    return ConnType.WIFI2_4G;
                case 2:
                    return ConnType.WIFI5G;
                default:
                    return ConnType.UNKNOWN;
            }
        }
        ConnType(int type){
            this.type = type;
        }

    }
}
