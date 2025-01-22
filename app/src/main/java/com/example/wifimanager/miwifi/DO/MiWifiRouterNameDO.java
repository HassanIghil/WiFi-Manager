package com.example.wifimanager.miwifi.DO;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class MiWifiRouterNameDO extends MiWifiBaseDO {
    String locale;
    String name;
}
