package io.shulie.takin.web.data.result.filemanage;

import java.util.Date;

import lombok.Data;


/**
 * @author zhaoyong
 */
@Data
public class FileManageResult {

    private Long id;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件大小：2MB
     */
    private String fileSize;

    /**
     * 文件后缀
     */
    private String fileExt;

    /**
     * 文件类型：0-脚本文件 1-数据文件 2-脚本jar文件 3-jmeter ext jar
     */
    private Integer fileType;

    /**
     * {
     * “dataCount”:”数据量”,
     * “isSplit”:”是否拆分”,
     * “topic”:”MQ主题”,
     * “nameServer”:”mq nameServer”,
     * }
     */
    private String fileExtend;

    /**
     * 客户id（当前登录用户对应的admin的id，数据隔离使用）
     */
    private Long customerId;

    /**
     * 上传时间
     */
    private Date uploadTime;

    /**
     * 上传路径：相对路径
     */
    private String uploadPath;

    private Integer isDeleted;

    private Date gmtCreate;

    private Date gmtUpdate;

}
