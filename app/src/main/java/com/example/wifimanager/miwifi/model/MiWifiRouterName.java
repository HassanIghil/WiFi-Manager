package com.example.wifimanager.miwifi.model;

import lombok.Data;

import java.io.Serializable;

/**
 * xiaomi-route-api
 * <br />
 *
 * @author: Jiashun Zhang
 * @since: 2021-08-14
 */
@Data
public class MiWifiRouterName  implements Serializable {
    /**
     * 路由器分配位置  好像默认是Home
     */
    String locale;
    /**
     * 路由器名字。
     */
    String name;
}
