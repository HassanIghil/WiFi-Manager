package com.example.wifimanager.miwifi;



import com.example.wifimanager.miwifi.DO.MiWifiBaseDO;
import com.example.wifimanager.miwifi.DO.MiWifiLoginDO;
import com.example.wifimanager.miwifi.exceptions.MiWifiLoginException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import cn.hutool.core.util.StrUtil;


public class MiWifiInvocationHandler implements InvocationHandler {
    private final MiWifiApi api;
    public MiWifiInvocationHandler(MiWifiApi api){
        this.api = api;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MiWifiBaseDO vo = (MiWifiBaseDO)method.invoke(api, args);
        if (vo.getCode()==401) {
            MiWifiLoginDO login = api.login();
            if(StrUtil.isBlank(login.getToken())){
                throw new MiWifiLoginException("用户名密码错误");
            }
            return method.invoke(api, args);
        }
        return method.invoke(api,args);
    }
}
