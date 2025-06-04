package org.apache.dolphinscheduler.api.platform.service;

import java.util.List;

import org.apache.dolphinscheduler.api.platform.dto.solaris.AppIdDTO;

import com.github.pagehelper.PageInfo;

public interface AppIdService {

    List<AppIdDTO> query(List<String> appIds);

    AppIdDTO queryByAppIdAndUser(String appId, String userName);

    List<String> queryByUserAccount(String userAccount, boolean isOwner);

    List<AppIdDTO> queryAll();

    PageInfo<String> pageQueryAllAppIds(Integer pageNo, Integer pageSize);

    /**
     * 查询appId的平铺用户，空格分开，第一个为leader
     * 
     * @param appIds
     * @return
     */
    List<AppIdDTO> queryWithNameFlat(List<String> appIds);

    String queryAppOwnerNames(String appId);

    void syncAppId(String appId);

    List<String> getNotAdminAppId(String user, boolean onlyOwner);
}
