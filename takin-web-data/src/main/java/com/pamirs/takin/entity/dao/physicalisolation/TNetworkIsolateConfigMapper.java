package com.pamirs.takin.entity.dao.physicalisolation;

import java.util.List;

import com.pamirs.takin.entity.domain.entity.NetworkIsolateConfig;
import org.apache.ibatis.annotations.Param;

public interface TNetworkIsolateConfigMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_network_isolate_config
     *
     * @mbggenerated
     */
    int deleteByPrimaryKey(Long id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_network_isolate_config
     *
     * @mbggenerated
     */
    int insert(NetworkIsolateConfig record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_network_isolate_config
     *
     * @mbggenerated
     */
    int insertSelective(NetworkIsolateConfig record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_network_isolate_config
     *
     * @mbggenerated
     */
    NetworkIsolateConfig selectByPrimaryKey(Long id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_network_isolate_config
     *
     * @mbggenerated
     */
    List<NetworkIsolateConfig> selectByAppName(String applicationName);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_network_isolate_config
     *
     * @mbggenerated
     */
    List<NetworkIsolateConfig> selectByAppNameList(@Param("names") List<String> names);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_network_isolate_config
     *
     * @mbggenerated
     */
    List<NetworkIsolateConfig> selectIsoldateSuceessByAppNameList(@Param("names") List<String> names);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_network_isolate_config
     *
     * @mbggenerated
     */
    int updateByPrimaryKeySelective(NetworkIsolateConfig record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_network_isolate_config
     *
     * @mbggenerated
     */
    int updateByPrimaryKeyWithBLOBs(NetworkIsolateConfig record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_network_isolate_config
     *
     * @mbggenerated
     */
    int updateByPrimaryKey(NetworkIsolateConfig record);

    int queryConfigExsit(NetworkIsolateConfig record);

    List<NetworkIsolateConfig> queryNetworkIsolateConfigs(NetworkIsolateConfig record);

    int deleteNetworkIsolateConfig(@Param("ids") List<Long> ids);

    List<NetworkIsolateConfig> queryNetworkIsolateConfig(@Param("ids") List<Long> ids);
}
