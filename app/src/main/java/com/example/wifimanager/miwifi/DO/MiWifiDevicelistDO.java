package com.example.wifimanager.miwifi.DO;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class MiWifiDevicelistDO extends MiWifiBaseDO {
    String mac;
    List<MiWifiDeviceDO> list;
}
