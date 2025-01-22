package com.example.wifimanager.miwifi.DO;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;


@EqualsAndHashCode(callSuper = true)
@Data
public class MiWifiMacFilterInfoDO extends MiWifiBaseDO {
    int enable;
    List<MiWifiDeviceDO> flist;
    List<MiWifiDeviceDO> list;
    List<MacFilter> macfilter;

    List<String> weblist;
    int model;

    @Data
    public static class MacFilter{
        String mac;
        String name;
    }

    public boolean currentModeisBlackMode(){
        return model == 0;
    }
}
