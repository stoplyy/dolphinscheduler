package org.apache.dolphinscheduler.api.platform.service;

import java.util.List;

import org.apache.dolphinscheduler.api.platform.dto.halley.AssetsInfo;
import org.apache.dolphinscheduler.api.platform.dto.halley.DepartmentByAppIdDto;
import org.apache.dolphinscheduler.api.platform.dto.halley.HalleyServerInfo;
import org.apache.dolphinscheduler.api.platform.dto.halley.SimpleDepartmentDto;
import org.apache.dolphinscheduler.api.platform.dto.halley.UserInfoDto;

/**
 * @Date: 2021-03-25 14:50:22
 * @LastEditors: Liuyangyang
 * @LastEditTime: 2022-04-02 02:04:16
 **/
public interface HalleyAccessService {

    // **获取 所有部门的 树形列表 */
    List<SimpleDepartmentDto> getDepartmentTreeList();

    // **获取下级部门 */
    List<SimpleDepartmentDto> getChildDepartmentById(Integer departmentId);

    // **用户信息 */
    UserInfoDto getUserByHalley(String userName);

    List<String> getDepartList(String userName);

    // **获取机器列表 */
    public List<AssetsInfo> getAssetsInfoByAppId(String appid);

    // **获取appId详情 */
    public List<DepartmentByAppIdDto> getdepartmentInfoByAppId(List<String> appids);

    // **获取机器配置列表 */
    public List<HalleyServerInfo> getAssetsInfoByIps(List<String> ips);
}
