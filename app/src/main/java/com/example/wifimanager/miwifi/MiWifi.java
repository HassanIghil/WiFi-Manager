package com.example.wifimanager.miwifi;

import com.example.wifimanager.miwifi.DO.*;
import com.example.wifimanager.miwifi.impl.*;
import com.example.wifimanager.miwifi.model.*;
import android.os.Build;
import androidx.annotation.RequiresApi;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import cn.hutool.aop.ProxyUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;


public class MiWifi implements MiWifiService {

    private final MiWifiApi api;

    public MiWifi(String url, String username, String passwd) {
        this(new MiWifiApiDefaultImpl(url, username, passwd));
    }

    public MiWifi(String url, String passwd) {
        this(url, "admin", passwd);
    }

    public MiWifi(String passwd) {
        this("http://192.168.31.1", passwd);
    }

    public MiWifi(MiWifiApi api) {
        this.api = ProxyUtil.newProxyInstance(new MiWifiInvocationHandler(api), MiWifiApi.class);
    }


    /**
     * Obtient tous les appareils en ligne
     *
     * @return Collection des appareils en ligne
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public List<MiWifiDevice> onlineDevice() {
        MiWifiDevicelistDO miWifiDevicelistVO = api.onlineDevice();

        ZonedDateTime sysTime = this.sysTime();

        List<MiWifiDevice> devices = new ArrayList<>();
        for (MiWifiDeviceDO deviceVO : miWifiDevicelistVO.getList()) {
            MiWifiDevice device = new MiWifiDevice();
            MiWifiDeviceDO.IP ip = deviceVO.getIp().get(0);
            device.setIp(ip.getIp());
            device.setMac(deviceVO.getMac());
            device.setRemarkName(deviceVO.getName());
            device.setName(deviceVO.getOname());
            device.setOnline(deviceVO.getOnline() == 1);
            device.setAP(deviceVO.getIsap() != 0);
            device.setConnType(MiWifiDevice.ConnType.mapType(deviceVO.getType()));
            device.setDownloadSpeed(Integer.parseInt(ip.getDownspeed()));
            device.setOnlineSecond(Integer.parseInt(ip.getOnline()));
            device.setUploadSpeed(Integer.parseInt(ip.getUpspeed()));
            DateTime upTime = null;
            upTime = DateUtil.offsetSecond(Date.from(sysTime.toInstant()), -device.getOnlineSecond());
            device.setUpTime(LocalDateTime.ofInstant(upTime.toInstant(), sysTime.getZone()));
            device.setAllowAdmin(deviceVO.getAuthority().getAdmin()==1);
            device.setAllowLan(deviceVO.getAuthority().getLan()==1);
            device.setAllowPriDisk(deviceVO.getAuthority().getPridisk()==1);
            device.setAllowWan(deviceVO.getAuthority().getWan()==1);
            devices.add(device);
        }
        return devices;
    }

    /**
     * Nom du routeur et emplacement
     */
    @Override
    public MiWifiRouterName routerName() {
        MiWifiRouterNameDO miWifiRouterNameVO = api.routerName();
        MiWifiRouterName rn = new MiWifiRouterName();
        rn.setLocale(miWifiRouterNameVO.getLocale());
        rn.setName(miWifiRouterNameVO.getName());
        return rn;
    }

    /**
     * Définit le nom et l'emplacement du routeur
     *
     * @param name   Nom
     * @param locale Emplacement
     * @return Succès ou échec de la configuration
     */
    @Override
    public boolean setRouterName(String name, String locale) {
        // {"locale":"Home","name":"Xiaomi_A3D7","code":0}
        MiWifiBaseDO miWifiBaseDO = api.setRouterName(name, locale);
        return miWifiBaseDO.getCode() == 0;
    }

    /**
     * Obtient l'heure système du routeur
     *
     * @return Heure système
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public ZonedDateTime sysTime() {
        MiWifiTimeDO timeVO = api.sysTime();
        MiWifiTimeDO.Time time = timeVO.getTime();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Use ZonedDateTime with the system time and timezone
            return ZonedDateTime.of(time.getYear(), time.getMonth(), time.getDay(), time.getHour(), time.getMin(), time.getSec(), 0, ZoneId.of(time.getTimezone().replace("CST-", "UTC+").replace("CST+", "UTC-")));
        } else {
            // Use ThreeTenABP or fallback logic for older versions
            return ZonedDateTime.now();  // Example fallback
        }

    }

    /**
     * Définit l'heure système du routeur
     * @param time Heure
     * @return Succès ou échec de la configuration
     */
    public boolean setSysTime(ZonedDateTime time) {
        MiWifiBaseDO miWifiBaseDO = api.setSysTime(time);
        return miWifiBaseDO.getCode() == 0;
    }

    /**
     * Statut du système du routeur
     *
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public MiWifiRouterStatus status() {
        MiWifiStatusDO statusVO = api.status();
        MiWifiRouterStatus status = new MiWifiRouterStatus();

        status.setTemperature(statusVO.getTemperature());
        status.setOnlineCount(statusVO.getCount().getOnline());
        status.setAllCount(statusVO.getCount().getAll());

        status.setCpuCore(statusVO.getCpu().getCore());
        status.setCpuHz(statusVO.getCpu().getHz());
        status.setCpuLoad(statusVO.getCpu().getLoad());

        ZonedDateTime sysTime = sysTime();
        DateTime upTime = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            upTime = DateUtil.offsetSecond(Date.from(sysTime.toInstant()), -Double.valueOf(statusVO.getUpTime()).intValue());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            status.setUpTime(LocalDateTime.ofInstant(upTime.toInstant(), sysTime.getZone()));
        }

        status.setChannel(statusVO.getHardware().getChannel());
        status.setPlatform(statusVO.getHardware().getPlatform());
        status.setMac(statusVO.getHardware().getMac());
        status.setSn(statusVO.getHardware().getSn());
        status.setVersion(statusVO.getHardware().getVersion());

        status.setMemHz(statusVO.getMem().getHz());
        status.setMemSize(statusVO.getMem().getTotal());
        status.setMemType(statusVO.getMem().getType());
        status.setMemUsage(statusVO.getMem().getUsage());

        status.setWanName(statusVO.getWan().getDevname());
        status.setDownloadSize(Long.parseLong(statusVO.getWan().getDownload()));
        status.setUploadSize(Long.parseLong(statusVO.getWan().getUpload()));
        status.setDownloadSpeed(Integer.parseInt(statusVO.getWan().getDownspeed()));
        status.setUploadSpeed(Integer.parseInt(statusVO.getWan().getUpspeed()));

        status.setMaxDownloadSpeed(Integer.parseInt(statusVO.getWan().getMaxdownloadspeed()));
        status.setMaxUploadSpeed(Integer.parseInt(statusVO.getWan().getMaxuploadspeed()));

        List<MiWifiRouterStatus.ConnDevice> devices = new ArrayList<>();
        for (MiWifiStatusDO.Device deviceVO : statusVO.getDev()) {
            MiWifiRouterStatus.ConnDevice device = new MiWifiRouterStatus.ConnDevice();
            device.setName(deviceVO.getDevname());
            device.setDownloadSize(Long.parseLong(deviceVO.getDownload()));
            device.setUploadSize(Long.parseLong(deviceVO.getUpload()));
            device.setDownSpeed(Integer.parseInt(deviceVO.getDownspeed()));
            device.setUploadSpeed(Integer.parseInt(deviceVO.getUpspeed()));
            device.setMac(deviceVO.getMac());
            device.setMaxDownloadSpeed(Integer.parseInt(deviceVO.getMaxdownloadspeed()));
            device.setMaxUploadSpeed(Integer.parseInt(deviceVO.getMaxuploadspeed()));
            device.setOnlineSecond(Integer.parseInt(deviceVO.getOnline()));
            devices.add(device);
        }
        status.setDeviceList(devices);
        return status;
    }

    /**
     * Permet l'accès à Internet.
     * @param mac Adresse MAC
     * @return Succès ou échec
     */
    public boolean allowWan(String mac){
        MiWifiBaseDO vo = api.setMacFilter(mac, true);
        return vo.getCode()==0;
    }

    /**
     * Interdit l'accès à Internet. L'appareil peut se connecter au wifi et accéder au réseau local.
     * @param mac Adresse MAC
     * @return Succès ou échec
     */
    public boolean forbidWan(String mac){
        MiWifiBaseDO vo = api.setMacFilter(mac, false);
        return vo.getCode()==0;
    }

    /**
     * Permet la connexion au wifi
     * @param mac Adresse MAC
     * @return Succès ou échec
     */
    public boolean allowConnWifi(String mac){
        MiWifiBaseDO vo = api.editDevice(mac, true, false);
        return vo.getCode()==0;
    }

    /**
     * Interdit la connexion au wifi
     * @param mac Adresse MAC
     * @return Succès ou échec
     */
    public boolean forbidConnWifi(String mac){
        MiWifiBaseDO vo = api.editDevice(mac, true, true);
        return vo.getCode()==0;
    }

    /**
     * Liste des appareils dans la liste noire wifi.
     * @return Liste des appareils dans la liste noire
     */
    public List<MiWifiMacFilter> blackList(){
        MiWifiMacFilterInfoDO info = api.macFilterInfo(true);
        return getMiWifiMacFilters(info);
    }

    /**
     * Liste des appareils dans la liste blanche wifi.
     * @return Liste des appareils dans la liste blanche
     */
    public List<MiWifiMacFilter> whiteList(){
        MiWifiMacFilterInfoDO info = api.macFilterInfo(false);
        return getMiWifiMacFilters(info);
    }

    /**
     * Vérifie si le mode de contrôle wifi actuel est en mode liste noire.
     * @return Si le mode est liste noire.
     */
    public boolean isBlackControlMode(){
        MiWifiMacFilterInfoDO info = api.macFilterInfo(false);
        return info.getModel()==0;
    }

    /**
     * Définit le mode de contrôle wifi.
     * @param blackmode Si c'est le mode liste noire.
     * @return Succès ou échec
     */
    public boolean enableWifiControl(boolean blackmode){
        MiWifiBaseDO vo = api.setWifiFilter(true, blackmode);
        return vo.getCode()==0;
    }

    /**
     * Désactive le mode de contrôle wifi.
     * @return Succès ou échec
     */
    public boolean disableWifiControl(){
        MiWifiBaseDO vo = api.setWifiFilter(false, false);
        return vo.getCode()==0;
    }

    private List<MiWifiMacFilter> getMiWifiMacFilters(MiWifiMacFilterInfoDO info) {
        List<MiWifiMacFilterInfoDO.MacFilter> macfilter = info.getMacfilter();
        List<MiWifiMacFilter> list = new ArrayList<>();
        for (MiWifiMacFilterInfoDO.MacFilter filter : macfilter) {
            MiWifiMacFilter i = new MiWifiMacFilter();
            i.setMac(filter.getMac());
            i.setName(filter.getName());
            list.add(i);
        }
        return list;
    }

}