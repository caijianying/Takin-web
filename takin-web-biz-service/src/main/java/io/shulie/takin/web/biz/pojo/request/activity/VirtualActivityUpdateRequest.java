package io.shulie.takin.web.biz.pojo.request.activity;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.shulie.takin.web.amdb.bean.common.EntranceTypeEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author 无涯
 * @date 2021/6/2 11:52 上午
 */
@Data
@ApiModel("业务活动修改对象")
public class VirtualActivityUpdateRequest {

    @NotNull(message = "业务活动ID不能为空")
    @ApiModelProperty("业务活动ID")
    private Long activityId;

    @ApiModelProperty("业务活动名称")
    @NotEmpty(message = "业务活动名称不能为空")
    private String activityName;


    @ApiModelProperty(name = "link_level", value = "业务活动等级")
    @JsonProperty("link_level")
    @NotEmpty(message = "业务活动等级不能为空")
    private String activityLevel;

    @NotNull
    @ApiModelProperty("入口类型")
    private EntranceTypeEnum type;

    @ApiModelProperty(name = "isCore", value = "业务活动链路是否核心链路 0:不是;1:是")
    @NotNull(message = "业务活动链路不能为空")
    private Integer isCore;

    @ApiModelProperty(name = "businessDomain", value = "业务域")
    @NotEmpty(message = "业务域不能为空")
    private String businessDomain;

    @ApiModelProperty(name = "virtualEntrance", value = "虚拟入口")
    @NotEmpty(message = "虚拟入口不能为空")
    private String virtualEntrance;


    @ApiModelProperty(name = "bindBusinessId", value = "绑定业务活动id")
    private String bindBusinessId;

}
