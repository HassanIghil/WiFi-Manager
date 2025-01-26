    package com.example.wifimanager.miwifi.impl;
    import android.os.Build;

    import cn.hutool.core.net.url.UrlBuilder;
    import cn.hutool.core.net.url.UrlPath;
    import cn.hutool.http.HttpRequest;
    import cn.hutool.http.HttpUtil;
    import cn.hutool.http.Method;
    import cn.hutool.json.JSONUtil;
    import com.android.volley.toolbox.HttpResponse;

    import com.android.volley.toolbox.HttpResponse;


    // ... other imports ...

    import com.example.wifimanager.miwifi.DO.MiWifiBaseDO;
    import com.example.wifimanager.miwifi.DO.MiWifiDevicelistDO;
    import com.example.wifimanager.miwifi.DO.MiWifiLoginDO;
    import com.example.wifimanager.miwifi.DO.MiWifiMacFilterInfoDO;
    import com.example.wifimanager.miwifi.DO.MiWifiRouterNameDO;
    import com.example.wifimanager.miwifi.DO.MiWifiStatusDO;
    import com.example.wifimanager.miwifi.DO.MiWifiTimeDO;
    import com.example.wifimanager.miwifi.MiWifiApi;
    import com.example.wifimanager.miwifi.exceptions.MiWifiStatusException;


    import java.nio.charset.Charset;
    import java.time.ZonedDateTime;
    import java.time.format.DateTimeFormatter;
    import java.util.HashMap;
    import java.util.Map;

    public class MiWifiApiDefaultImpl implements MiWifiApi {
        private String adminUrl;
        private String token;
        private String username;
        private String passwd;


        public MiWifiApiDefaultImpl(String url, String username, String passwd) {
            this.adminUrl = url;
            this.username = username;
            this.passwd = passwd;
        }

        @Override
        public MiWifiLoginDO login() {
            String reqUrl = UrlBuilder.ofHttp(adminUrl)
                    .setPath(UrlPath.of("/cgi-bin/luci/api/xqsystem/login", Charset.defaultCharset()))
                    .addQuery("username", username)
                    .addQuery("password", passwd).build();
            String s = this.getResponse(reqUrl);
            MiWifiLoginDO miWifiLoginModel = JSONUtil.toBean(s, MiWifiLoginDO.class);
            token = miWifiLoginModel.getToken();
            return miWifiLoginModel;
        }

        @Override
        public MiWifiDevicelistDO onlineDevice() {
            String url = UrlBuilder.ofHttp(adminUrl)
                    .setPath(UrlPath.of("/cgi-bin/luci/;stok=" + token + "/api/misystem/devicelist", Charset.defaultCharset()))
                    .build();
            String s = this.getResponse(url);
            return JSONUtil.toBean(s, MiWifiDevicelistDO.class);
        }

        @Override
        public MiWifiRouterNameDO routerName() {
            String url = UrlBuilder.ofHttp(adminUrl)
                    .setPath(UrlPath.of("/cgi-bin/luci/;stok=" + token + "/api/misystem/router_name", Charset.defaultCharset()))
                    .build();
            String s = this.getResponse(url);
            return JSONUtil.toBean(s, MiWifiRouterNameDO.class);
        }

        @Override
        public MiWifiBaseDO setRouterName(String name, String locale) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", name);
            map.put("locale", locale);
            String url = UrlBuilder.ofHttp(adminUrl)
                    .setPath(UrlPath.of("/cgi-bin/luci/;stok=" + token + "/api/misystem/set_router_name", Charset.defaultCharset()))
                    .build();
            String s = this.getResponse(url);
            return JSONUtil.toBean(s, MiWifiBaseDO.class);
        }

        @Override
        public MiWifiTimeDO sysTime() {
            String url = UrlBuilder.ofHttp(adminUrl)
                    .setPath(UrlPath.of("/cgi-bin/luci/;stok=" + token + "/api/misystem/sys_time", Charset.defaultCharset()))
                    .build();
            String s = this.getResponse(url);
            return JSONUtil.toBean(s, MiWifiTimeDO.class);
        }

        @Override
        public MiWifiBaseDO setSysTime(ZonedDateTime time) {
            String url = UrlBuilder.ofHttp(adminUrl)
                    .setPath(UrlPath.of("/cgi-bin/luci/;stok=" + token + "/api/misystem/set_sys_time", Charset.defaultCharset()))
                    .build();
            Map<String, Object> data = new HashMap<>();
            int offset = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                offset = time.getOffset().getTotalSeconds() / 60 / 60;
            }
            String prefix = offset <= 0 ? "+" : "-";
            data.put("timezone", "CST" + prefix + offset);
    //        data.put("index","0");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                data.put("time", time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            String s = this.getResponse(url,data);
            return JSONUtil.toBean(s, MiWifiBaseDO.class);
        }

        @Override
        public MiWifiBaseDO setMacFilter(String mac, boolean wan){

            String url = UrlBuilder.ofHttp(adminUrl)
                    .setPath(UrlPath.of("/cgi-bin/luci/;stok=" + token + "/api/xqsystem/set_mac_filter", Charset.defaultCharset()))
                    .addQuery("mac", mac)
                    .addQuery("wan", wan ? "1" : "0")

                    .build();
            String s = this.getResponse(url);
            return JSONUtil.toBean(s, MiWifiBaseDO.class);
        }



        public MiWifiBaseDO setWifiFilter(boolean enable, boolean blackMode){

            String url = UrlBuilder.ofHttp(adminUrl)
                    .setPath(UrlPath.of("/cgi-bin/luci/;stok=" + token + "/api/xqnetwork/set_wifi_macfilter", Charset.defaultCharset()))
                    .addQuery("model",blackMode?"0":"1")
                    .addQuery("enable",enable?"1":"0")
                    .build();
            String s = this.getResponse(url);
            return JSONUtil.toBean(s, MiWifiBaseDO.class);
        }


        @Override
        public MiWifiStatusDO status() {
            String url = UrlBuilder.ofHttp(adminUrl)
                    .setPath(UrlPath.of("/cgi-bin/luci/;stok=" + token + "/api/misystem/status", Charset.defaultCharset()))
                    .build();
            String s = this.getResponse(url);
            return JSONUtil.toBean(s, MiWifiStatusDO.class);
        }

        @Override
        public MiWifiBaseDO editDevice(String mac, boolean isblack, boolean active) {

            String model = isblack ? "0" : "1";
            String option = active ? "0" : "1";

            String url = UrlBuilder.ofHttp(adminUrl)
                    .setPath(UrlPath.of("/cgi-bin/luci/;stok=" + token + "/api/xqnetwork/edit_device", Charset.defaultCharset()))
                    .addQuery("mac", mac)
                    .addQuery("model", model)
                    .addQuery("option", option)
                    .build();
            String s = this.getResponse(url);
            return JSONUtil.toBean(s, MiWifiBaseDO.class);
        }

        @Override
        public MiWifiMacFilterInfoDO macFilterInfo(boolean isBlackList) {
            String model = isBlackList ? "0" : "1";

            String url = UrlBuilder.ofHttp(adminUrl)
                    .setPath(UrlPath.of("/cgi-bin/luci/;stok=" + token + "/api/xqnetwork/wifi_macfilter_info", Charset.defaultCharset()))
                    .addQuery("model", model)
                    .build();
            String s = this.getResponse(url);
            return JSONUtil.toBean(s, MiWifiMacFilterInfoDO.class);

        }

        public String getResponse(String url){
           return this.getResponse(url,null);
        }
        public String getResponse(String url,Map<String,Object> data){
            Method method = null;
            if(data == null){
                method = Method.GET;
            }else{
                method = Method.POST;
            }

            HttpRequest request = HttpUtil.createRequest(method, url);
            request.form(data);
            cn.hutool.http.HttpResponse execute = request.execute();
            if (execute.getStatus() != 200) {

                throw new MiWifiStatusException("Response status code error"+execute.getStatus()+" Please make sure you can access the admin address correctly:"+adminUrl);
            }
            return execute.body();
        }
    }
