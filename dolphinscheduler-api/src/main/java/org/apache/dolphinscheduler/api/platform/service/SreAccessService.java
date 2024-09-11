package org.apache.dolphinscheduler.api.platform.service;

/**
 * @Date: 2023-04-19 14:50:22
 * @LastEditors: Liuyangyang
 * @LastEditTime: 2023-04-19 14:50:22
 **/
public interface SreAccessService {

    public String pastePubRsa(String ip);

    public String syncSREByAppid(String appId);
}
