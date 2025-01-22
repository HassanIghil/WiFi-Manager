package com.example.wifimanager.miwifi;



import com.example.wifimanager.miwifi.model.MiWifiDevice;
import com.example.wifimanager.miwifi.model.MiWifiRouterName;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * xiaomi-route-api
 * <br />
 *
 *
 * @since: 2021-08-14
 */
public interface MiWifiService {
    /**
     * 所有在线设备
     * @return 在线设备。
     */
    public List<MiWifiDevice> onlineDevice();

    /**
     * 路由器名称与位置
     * @return 名称与位置
     */
    public MiWifiRouterName routerName();

    /**
     * 设置路由器名称与位置
     * @param name 名称
     * @param locale 位置
     * @return
     */
    public boolean setRouterName(String name, String locale);

    /**
     * 获取系统时间
     * @return 路由器系统时间
     */
    public ZonedDateTime sysTime();

    /**
     * 允许连接互联网。
     * @param mac mac地址。
     * @return 是否设置成功
     */
    public boolean allowWan(String mac);

    /**
     * 禁止连接互联网
     * @param mac mac地址
     * @return 是否设置成功
     */
    public boolean forbidWan(String mac);

    /**
     * 设置路由器系统时间
     * @param time 时间
     * @return 是否设置成功
     */
    public boolean setSysTime(ZonedDateTime time);

    /**
     * 允许连接wifi
     * @param mac mac地址
     * @return 是否设置成功
     */
    public boolean allowConnWifi(String mac);

    /**
     * 禁止连接wifi
     * @param mac mac地址
     * @return 是否设置成功。
     */
    public boolean forbidConnWifi(String mac);
}
