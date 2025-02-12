package io.shulie.takin.web.data.param.scriptmanage;

import lombok.Data;

/**
 * @author shulie
 */
@Data
public class ScriptManageDeployCreateParam {


    private Long scriptId;

    /**
     * 名称
     */
    private String name;

    /**
     * 关联类型(业务活动)
     */
    private String refType;

    /**
     * 关联值(活动id)
     */
    private String refValue;

    /**
     * 脚本类型;0为jmeter脚本
     */
    private Integer type;

    /**
     * 0代表新建，1代表调试通过
     */
    private Integer status;

    /**
     * 操作人id
     */
    private Long createUserId;

    /**
     * 操作人名称
     */
    private String createUserName;


    private Integer scriptVersion;

    /**
     * 拓展字段
     */
    private String feature;

    private Long customerId;

    /**
     * 描述
     */
    private String description;


}
