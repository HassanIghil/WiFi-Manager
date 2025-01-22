package com.example.wifimanager.miwifi;

import com.example.wifimanager.miwifi.DO.MiWifiBaseDO;
import com.example.wifimanager.miwifi.DO.MiWifiDevicelistDO;
import com.example.wifimanager.miwifi.DO.MiWifiLoginDO;
import com.example.wifimanager.miwifi.DO.MiWifiMacFilterInfoDO;
import com.example.wifimanager.miwifi.DO.MiWifiRouterNameDO;
import com.example.wifimanager.miwifi.DO.MiWifiStatusDO;
import com.example.wifimanager.miwifi.DO.MiWifiTimeDO;

import java.time.ZonedDateTime;


public interface MiWifiApi {

    /**
     * 路由器登录
     * @return
     */
    public MiWifiLoginDO login();

    /**
     * 在线设备
     * @return
     */
    public MiWifiDevicelistDO onlineDevice();

    /**
     * 路由器名称与位置。
     * @return
     */
    public MiWifiRouterNameDO routerName();

    /**
     * 设置路由器名称与位置
     * @param name 名称
     * @param locale 位置
     * @return
     */
    public MiWifiBaseDO setRouterName(String name, String locale);

    /**
     * 路由器系统时间
     * @return 系统时间
     */
    public MiWifiTimeDO sysTime();

    /**
     * 设置路由器系统时间
     * @param time 时间
     * @return
     */
    public MiWifiBaseDO setSysTime(ZonedDateTime time);

    /**
     * 设置mac过滤
     * @param mac mac地址
     * @param wan 是否允许访问互联网。
     * @return
     */
    public MiWifiBaseDO setMacFilter(String mac, boolean wan);

    /**
     * 设置wifi过滤模式
     * @param enable 是否启用
     * @param blackMode 是否为黑名单模式
     * @return
     */
    public MiWifiBaseDO setWifiFilter(boolean enable, boolean blackMode);

    /**
     * 路由器状态
     * @return
     */
    public MiWifiStatusDO status();

    /**
     * 设置设备状态。
     * @param mac 设备mac地址
     * @param isblack 是否黑名单模式
     * @param active 当isBlack 为true时 此项为true则表明该mac在黑名单中。
     *               当isBlack 为false时 此项为true则表明该mac在白名单中。
     * @return
     */
    public MiWifiBaseDO editDevice(String mac, boolean isblack, boolean active);
    public MiWifiMacFilterInfoDO macFilterInfo(boolean isBlackList);
}
