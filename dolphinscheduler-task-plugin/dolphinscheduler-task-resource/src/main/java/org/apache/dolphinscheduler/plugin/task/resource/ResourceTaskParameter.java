package org.apache.dolphinscheduler.plugin.task.resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResourceTaskParameter {

    /**
     * 文件模板 （支持参数替换）
     */
    private String fileContext;

    /**
     * 是否用 参数替换 文件内容
     */
    private ParseMethod parseMethod;

    /**
     * 关联的资源文件
     *
     */
    @Nullable
    private String resource;

    /*
     * 动态资源文件 reourceTask 处理此文件
     */
    @Nullable
    private String dynamicResource;

    /*
     * 文件参数名
     */
    @Nullable
    private String inputFileParam;

    /*
     * 资源文件 所属的角色
     */
    @Nullable
    private String tenant;

    /**
     * 文件名称 支持参数替换
     * 上传文件时，文件名称 包含相对路径 a/b/file.txt
     * 下载文件时，文件名称 包含相对路径 a/b/file.txt (与resourceInfo中的文件名称一致)
     * 删除文件时，文件名称 包含相对路径 a/b/file.txt (与resourceInfo中的文件名称一致)
     */
    private String fileName;

    /**
     * NONE: 无操作
     * DOWNLOAD: 下载
     * DELETE: 删除
     * UPLOAD: 上传
     */
    private OperMethod operMethod;

    private String sourceLocalAbsoluteFile;

    public boolean needParse() {
        return parseMethod != null && parseMethod != ParseMethod.NONE;
    }

    public boolean checkParameters() {
        if (operMethod == null) {
            return false;
        }
        if (StringUtils.isEmpty(resource) && StringUtils.isEmpty(fileContext) && StringUtils.isEmpty(dynamicResource)) {
            return false;
        }

        return true;
    }
}
