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
public class MiWifiMacFilter implements Serializable {
    private String name;
    private String mac;
}
