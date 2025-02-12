package io.shulie.takin.web.biz.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.google.common.collect.Lists;
import com.pamirs.takin.entity.dao.confcenter.TApplicationMntDao;
import com.pamirs.takin.entity.domain.entity.TApplicationMnt;
import io.shulie.takin.common.beans.page.PagingList;
import io.shulie.takin.utils.string.StringUtil;
import io.shulie.takin.web.biz.service.ApplicationPluginsConfigService;
import io.shulie.takin.web.biz.utils.CopyUtils;
import io.shulie.takin.web.common.exception.ExceptionCode;
import io.shulie.takin.web.common.exception.TakinWebException;
import io.shulie.takin.web.data.dao.application.ApplicationPluginsConfigDAO;
import io.shulie.takin.web.data.mapper.mysql.ApplicationPluginsConfigMapper;
import io.shulie.takin.web.data.model.mysql.ApplicationPluginsConfigEntity;
import io.shulie.takin.web.data.param.application.ApplicationPluginsConfigParam;
import io.shulie.takin.web.data.result.application.ApplicationPluginsConfigVO;
import io.shulie.takin.web.ext.entity.UserExt;
import io.shulie.takin.web.ext.util.WebPluginUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * (ApplicationPluginsConfig)表服务实现类
 *
 * @author caijy
 * @since 2021-05-18 17:22:42
 */
@Slf4j
@Service
public class ApplicationPluginsConfigServiceImpl implements ApplicationPluginsConfigService {

    @Autowired
    ApplicationPluginsConfigDAO applicationPluginsConfigDAO;

    @Resource
    ApplicationPluginsConfigMapper applicationPluginsConfigMapper;

    @Autowired
    TApplicationMntDao tApplicationMntDao;

    @Override
    public ApplicationPluginsConfigVO getById(Long id) {
        if (id != null) {
            ApplicationPluginsConfigEntity configEntity = applicationPluginsConfigDAO.getById(id);
            return CopyUtils.copyFields(configEntity, ApplicationPluginsConfigVO.class);
        }
        return null;
    }

    @Override
    public PagingList<ApplicationPluginsConfigVO> getPageByParam(ApplicationPluginsConfigParam param) {
        if (Objects.isNull(param) || Objects.isNull(param.getApplicationId())) {
            throw new TakinWebException(ExceptionCode.POD_NUM_EMPTY, "缺少参数");
        }
        Long customerId = WebPluginUtils.getCustomerId();
        param.setCustomerId(customerId);
        IPage<ApplicationPluginsConfigEntity> listPage = applicationPluginsConfigDAO.findListPage(param);
        List<ApplicationPluginsConfigEntity> records = listPage.getRecords();
        List<ApplicationPluginsConfigVO> configVos = Lists.newArrayList();
        for (ApplicationPluginsConfigEntity record : records) {
            ApplicationPluginsConfigVO configVO = new ApplicationPluginsConfigVO();
            BeanUtils.copyProperties(record, configVO);
            if ("-1".equals(configVO.getConfigValue())) {
                configVO.setConfigValueName("与业务key一致");
            } else {
                configVO.setConfigValueName(record.getConfigValue() + "小时");
            }
            //精度丢失问题
            configVO.setApplicationId(record.getApplicationId() + "");
            configVO.setId(record.getId() + "");
            configVos.add(configVO);
        }
        return PagingList.of(configVos, listPage.getTotal());
    }

    @Override
    public Boolean add(ApplicationPluginsConfigParam param) {
        ApplicationPluginsConfigEntity entity = this.validateAndSet(param);
        applicationPluginsConfigMapper.insert(entity);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean addBatch(List<ApplicationPluginsConfigParam> params) {
        List<ApplicationPluginsConfigEntity> entityList = Lists.newArrayList();
        params.forEach(t -> {
            entityList.add(this.validateAndSet(t));
        });
        applicationPluginsConfigDAO.saveBatch(entityList);
        return true;
    }

    private ApplicationPluginsConfigEntity validateAndSet(ApplicationPluginsConfigParam param) {
        if (Objects.isNull(param)) {
            throw new TakinWebException(ExceptionCode.POD_NUM_EMPTY, "缺少参数");
        }
        ApplicationPluginsConfigEntity entity = CopyUtils.copyFields(param, ApplicationPluginsConfigEntity.class);
        if (Objects.isNull(entity.getApplicationId())) {
            throw new TakinWebException(ExceptionCode.POD_NUM_EMPTY, "应用ID不能为空！");
        }
        if (StringUtil.isBlank(entity.getConfigItem())) {
            throw new TakinWebException(ExceptionCode.POD_NUM_EMPTY, "配置项不能为空！");
        }
        if (StringUtil.isBlank(entity.getConfigValue())) {
            throw new TakinWebException(ExceptionCode.POD_NUM_EMPTY, "配置值不能为空！");
        }
        //优先取参数内的 否则从restcontext取
        if (param.getUserId() != null && param.getCustomerId() != null) {
            entity.setCreatorId(param.getUserId());
            entity.setModifierId(param.getUserId());
            entity.setCustomerId(param.getCustomerId());
        } else {
            UserExt user = WebPluginUtils.getUser();
            if (user != null) {
                entity.setCreatorId(user.getId());
                entity.setModifierId(user.getId());
                entity.setCustomerId(user.getCustomerId());
            }
        }

        Date now = new Date();
        entity.setCreateTime(now);
        entity.setModifieTime(now);
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateBatch(List<ApplicationPluginsConfigParam> params) {
        params.forEach(param -> {
            if (Objects.isNull(param)) {
                throw new TakinWebException(ExceptionCode.POD_NUM_EMPTY, "缺少参数");
            }
            if (Objects.isNull(param.getId())) {
                throw new TakinWebException(ExceptionCode.POD_NUM_EMPTY, "ID不能为空！");
            }
        });

        List<ApplicationPluginsConfigEntity> entitys = CopyUtils.copyFieldsList(params,
            ApplicationPluginsConfigEntity.class);
        UserExt user = WebPluginUtils.getUser();
        Date now = new Date();
        entitys.forEach(entity -> {
            entity.setCreateTime(now);
            entity.setModifieTime(now);
            entity.setCreatorId(user.getId());
            entity.setModifierId(user.getId());
            entity.setCustomerId(user.getCustomerId());
        });

        return applicationPluginsConfigDAO.updateBatchById(entitys);
    }

    @Override
    public Boolean update(ApplicationPluginsConfigParam param) {
        if (Objects.isNull(param)) {
            throw new TakinWebException(ExceptionCode.POD_NUM_EMPTY, "缺少参数");
        }
        if (Objects.isNull(param.getId())) {
            throw new TakinWebException(ExceptionCode.POD_NUM_EMPTY, "ID不能为空！");
        }
        if (StringUtil.isEmpty(param.getConfigValue())) {
            throw new TakinWebException(ExceptionCode.POD_NUM_EMPTY, "配置值不能为空！");
        }

        ApplicationPluginsConfigEntity entity = CopyUtils.copyFields(param, ApplicationPluginsConfigEntity.class);
        UserExt user = WebPluginUtils.getUser();
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setModifieTime(now);
        if (Objects.nonNull(user)) {
            entity.setCreatorId(user.getId());
            entity.setModifierId(user.getId());
            entity.setCustomerId(user.getCustomerId());
        }
        applicationPluginsConfigMapper.updateById(entity);
        return true;
    }

    @Override
    public List<ApplicationPluginsConfigVO> getListByParam(ApplicationPluginsConfigParam param) {
        if (Objects.isNull(param) || Objects.isNull(param.getApplicationName())) {
            throw new TakinWebException(ExceptionCode.POD_NUM_EMPTY, "缺少参数");
        }
        if (Objects.isNull(param.getConfigKey())) {
            throw new TakinWebException(ExceptionCode.POD_NUM_EMPTY, "configKey为空");
        }
        List<ApplicationPluginsConfigEntity> list = applicationPluginsConfigDAO.findList(param);
        if (list != null && !list.isEmpty()) {
            return CopyUtils.copyFieldsList(list, ApplicationPluginsConfigVO.class);
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void init() {
        log.info("开始补充每个应用的插件配置");
        List<TApplicationMnt> applications = tApplicationMntDao.getAllApplications();
        ApplicationPluginsConfigParam param = new ApplicationPluginsConfigParam();
        List<ApplicationPluginsConfigEntity> list = applicationPluginsConfigDAO.findList(param);
        List<Long> applicationIds = Lists.newArrayList();
        if (list.size() > 0) {
            applicationIds = list.stream().map(ApplicationPluginsConfigEntity::getApplicationId).collect(
                Collectors.toList());
        }
        List<Long> finalApplicationIds = applicationIds;
        List<TApplicationMnt> needInitList = applications.stream().filter(
            t -> !finalApplicationIds.contains(t.getApplicationId())).collect(
            Collectors.toList());
        needInitList.forEach(applicationMnt -> {
            ApplicationPluginsConfigParam configParam = new ApplicationPluginsConfigParam();
            configParam.setConfigItem("redis影子key有效期");
            configParam.setConfigKey("redis_expire");
            configParam.setConfigDesc("可自定义设置redis影子key有效期，默认与业务key有效期一致。若设置时间比业务key有效期长，不生效，仍以业务key有效期为准。");
            configParam.setConfigValue("-1");
            configParam.setApplicationName(applicationMnt.getApplicationName());
            configParam.setApplicationId(applicationMnt.getApplicationId());
            configParam.setUserId(applicationMnt.getUserId());
            configParam.setCustomerId(applicationMnt.getCustomerId());
            this.add(configParam);
        });
    }
}
