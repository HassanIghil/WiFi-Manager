package com.example.wifimanager.miwifi.DO;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class MiWifiLoginDO extends MiWifiBaseDO {
    String url;
    String token;
}
