package io.shulie.takin.web.config.sync.api;

import java.util.List;

import io.shulie.takin.web.config.entity.ShadowConsumer;

/**
 * 影子Job服务
 *
 * @author shiyajian
 * create: 2020-09-01
 */
public interface ShadowConsumerSyncService {

    void syncShadowConsumer(String namespace, String applicationName, List<ShadowConsumer> newConsumers);

}
