package com.example.wifimanager.miwifi;

import com.example.wifimanager.miwifi.DO.*;
import com.example.wifimanager.miwifi.impl.*;
import com.example.wifimanager.miwifi.model.*;
import android.os.Build;
import androidx.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * Authenticates with the router and retrieves a session token.
     * @return Session token if successful, null otherwise.
     */
    public String login() {
        MiWifiLoginDO loginResponse = api.login();
        return loginResponse != null && loginResponse.getCode() == 0 ? loginResponse.getToken() : null;
    }

    /**
     * Obtains all online devices
     *
     * @return Collection of online devices
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
            DateTime upTime = DateUtil.offsetSecond(Date.from(sysTime.toInstant()), -device.getOnlineSecond());
            device.setUpTime(LocalDateTime.ofInstant(upTime.toInstant(), sysTime.getZone()));
            device.setAllowAdmin(deviceVO.getAuthority().getAdmin() == 1);
            device.setAllowLan(deviceVO.getAuthority().getLan() == 1);
            device.setAllowPriDisk(deviceVO.getAuthority().getPridisk() == 1);
            device.setAllowWan(deviceVO.getAuthority().getWan() == 1);
            devices.add(device);
        }
        return devices;
    }

    /**
     * Router name and location
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
     * Set the router name and location
     *
     * @param name   Name
     * @param locale Location
     * @return Success or failure of the configuration
     */
    @Override
    public boolean setRouterName(String name, String locale) {
        MiWifiBaseDO miWifiBaseDO = api.setRouterName(name, locale);
        return miWifiBaseDO.getCode() == 0;
    }

    /**
     * Obtains the system time from the router
     *
     * @return System time
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public ZonedDateTime sysTime() {
        MiWifiTimeDO timeVO = api.sysTime();
        MiWifiTimeDO.Time time = timeVO.getTime();

        return ZonedDateTime.of(time.getYear(), time.getMonth(), time.getDay(),
                time.getHour(), time.getMin(), time.getSec(), 0,
                ZoneId.of(time.getTimezone().replace("CST-", "UTC+").replace("CST+", "UTC-")));
    }

    /**
     * Set the system time on the router
     *
     * @param time Time
     * @return Success or failure of the configuration
     */
    public boolean setSysTime(ZonedDateTime time) {
        MiWifiBaseDO miWifiBaseDO = api.setSysTime(time);
        return miWifiBaseDO.getCode() == 0;
    }

    /**
     * Get the router's system status
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

    // Additional functions related to controlling devices and filtering
    public boolean allowWan(String mac) {
        MiWifiBaseDO vo = api.setMacFilter(mac, true);
        return vo.getCode() == 0;
    }

    public boolean forbidWan(String mac) {
        MiWifiBaseDO vo = api.setMacFilter(mac, false);
        return vo.getCode() == 0;
    }

    public boolean allowConnWifi(String mac) {
        MiWifiBaseDO vo = api.editDevice(mac, true, false);
        return vo.getCode() == 0;
    }

    public boolean forbidConnWifi(String mac) {
        MiWifiBaseDO vo = api.editDevice(mac, true, true);
        return vo.getCode() == 0;
    }

    public List<MiWifiMacFilter> blackList() {
        MiWifiMacFilterInfoDO info = api.macFilterInfo(true);
        return getMiWifiMacFilters(info);
    }

    public List<MiWifiMacFilter> whiteList() {
        MiWifiMacFilterInfoDO info = api.macFilterInfo(false);
        return getMiWifiMacFilters(info);
    }

    public boolean isBlackControlMode() {
        MiWifiMacFilterInfoDO info = api.macFilterInfo(false);
        return info.getModel() == 0;
    }

    public boolean enableWifiControl(boolean blackmode) {
        MiWifiBaseDO vo = api.setWifiFilter(true, blackmode);
        return vo.getCode() == 0;
    }

    public boolean disableWifiControl() {
        MiWifiBaseDO vo = api.setWifiFilter(false, false);
        return vo.getCode() == 0;
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


        private static final String ROUTER_URL = "http://192.168.31.1"; // Replace with your router's URL

        // Login method to first get the token and then perform the login
        public String login(String username, String password) {
            String token = getSessionToken();

            if (token != null) {
                String loginUrl = ROUTER_URL + "/cgi-bin/luci/;stok=" + token + "/api/misystem/login";

                // Prepare the parameters for login (username and password)
                Map<String, String> params = new HashMap<>();
                params.put("username", username);
                params.put("password", password);

                // Make the login request
                String response = makeHttpRequest(loginUrl, params);

                // Check if login was successful (you can customize this response check)
                if (response != null && response.contains("success")) {
                    return token;  // Return the token on success
                }
            }

            return null;  // Return null if login failed
        }

        // Get session token (stok) by making an unauthenticated GET request
        private String getSessionToken() {
            try {
                URL url = new URL(ROUTER_URL + "/cgi-bin/luci");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                // Read response to extract token from the headers or body
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                String token = null;
                while ((line = reader.readLine()) != null) {
                    // Look for the token in the response (you can adjust this depending on how the token is returned)
                    if (line.contains("stok=")) {
                        token = line.split("stok=")[1].split("\"")[0];
                        break;
                    }
                }
                reader.close();
                return token;  // Return the session token

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;  // Return null if token retrieval fails
        }

        // Helper method to send a POST request for login
        private String makeHttpRequest(String urlStr, Map<String, String> params) {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                // Set POST parameters (username and password)
                StringBuilder postData = new StringBuilder();
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    if (postData.length() > 0) postData.append('&');
                    postData.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                    postData.append('=');
                    postData.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                }

                // Send the POST data
                connection.getOutputStream().write(postData.toString().getBytes("UTF-8"));

                // Get the response
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                return response.toString();  // Return the response body

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;  // Return null if request fails
        }
    }

