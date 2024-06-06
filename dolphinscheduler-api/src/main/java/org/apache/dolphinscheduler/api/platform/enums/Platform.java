/*
 * Copyright 2023 tuhu.cn All right reserved. This software is the
 * confidential and proprietary information of tuhu.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Tuhu.cn
 */
package org.apache.dolphinscheduler.api.platform.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author bailei
 * @since 2023/11/2 16:42
 */
public enum Platform implements IEnum<String> {
    simple("simple"),
    hubble("hubble"),
    canal("canal"),
    hbase("hbase"),
    ;

    Platform(String platform) {
        this.platform = platform;
    }

    @EnumValue
    private String platform;

    @JsonValue
    @Override
    public String getValue() {
        return platform;
    }
}
