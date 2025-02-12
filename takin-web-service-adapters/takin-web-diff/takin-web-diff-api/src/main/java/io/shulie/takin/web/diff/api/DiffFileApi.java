package io.shulie.takin.web.diff.api;

import io.shulie.takin.cloud.open.req.filemanager.FileContentParamReq;
import io.shulie.takin.cloud.open.req.filemanager.FileCopyParamReq;
import io.shulie.takin.cloud.open.req.filemanager.FileCreateByStringParamReq;
import io.shulie.takin.cloud.open.req.filemanager.FileDeleteParamReq;
import io.shulie.takin.cloud.open.req.filemanager.FileZipParamReq;

/**
 * @author shiyajian
 * create: 2020-10-19
 */
public interface DiffFileApi {

    /**
     * 获取文件上传、下载的路径
     * 私有化版本：获得takin-web的路径
     * 云版本：获得阿里云takin-cloud的路径
     */
    String getFileManageContextPath(FileContentParamReq req);

    /**
     * 删除文件
     *
     * @return
     */
    public Boolean deleteFile(FileDeleteParamReq req);

    /**
     * 复制文件到指定目录
     *
     * @return
     */
    public Boolean copyFile(FileCopyParamReq req);

    /**
     * 将指定文件打包到指定目录
     *
     * @return
     */
    public Boolean zipFile(FileZipParamReq fileZipParamReq);

    /**
     * 将字符串转为指定文件
     *
     * @return
     */
    Boolean createFileByPathAndString(FileCreateByStringParamReq req);
}
