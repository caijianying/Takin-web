package io.shulie.takin.web.biz.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.alibaba.fastjson.JSON;

import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pamirs.pradar.MiddlewareType;
import com.pamirs.takin.common.util.MD5Util;
import io.shulie.amdb.common.dto.link.topology.LinkEdgeDTO;
import io.shulie.amdb.common.dto.link.topology.LinkNodeDTO;
import io.shulie.amdb.common.dto.link.topology.LinkTopologyDTO;
import io.shulie.amdb.common.dto.link.topology.NodeExtendInfoBaseDTO;
import io.shulie.amdb.common.dto.link.topology.NodeExtendInfoForCacheDTO;
import io.shulie.amdb.common.dto.link.topology.NodeExtendInfoForDBDTO;
import io.shulie.amdb.common.dto.link.topology.NodeExtendInfoForMQDTO;
import io.shulie.amdb.common.dto.link.topology.NodeExtendInfoForOSSDTO;
import io.shulie.amdb.common.dto.link.topology.NodeExtendInfoForSearchDTO;
import io.shulie.amdb.common.enums.NodeTypeEnum;
import io.shulie.amdb.common.enums.NodeTypeGroupEnum;
import io.shulie.takin.common.beans.page.PagingList;
import io.shulie.takin.web.amdb.api.ApplicationClient;
import io.shulie.takin.web.amdb.api.ApplicationEntranceClient;
import io.shulie.takin.web.amdb.bean.query.application.ApplicationNodeQueryDTO;
import io.shulie.takin.web.amdb.bean.result.application.ApplicationNodeDTO;
import io.shulie.takin.web.biz.common.CommonService;
import io.shulie.takin.web.biz.pojo.request.application.ApplicationEntranceTopologyQueryRequest;
import io.shulie.takin.web.biz.pojo.response.application.ApplicationEntranceTopologyResponse;
import io.shulie.takin.web.biz.pojo.response.application.ApplicationEntranceTopologyResponse.AppCallDatasourceInfo;
import io.shulie.takin.web.biz.pojo.response.application.ApplicationEntranceTopologyResponse.AppCallInfo;
import io.shulie.takin.web.biz.pojo.response.application.ApplicationEntranceTopologyResponse.AppProviderDatasourceInfo;
import io.shulie.takin.web.biz.pojo.response.application.ApplicationEntranceTopologyResponse.AppProviderInfo;
import io.shulie.takin.web.biz.pojo.response.application.ApplicationEntranceTopologyResponse.ApplicationEntranceTopologyEdgeResponse;
import io.shulie.takin.web.biz.pojo.response.application.ApplicationEntranceTopologyResponse.DbInfo;
import io.shulie.takin.web.biz.pojo.response.application.ApplicationEntranceTopologyResponse.ExceptionListResponse;
import io.shulie.takin.web.biz.pojo.response.application.ApplicationEntranceTopologyResponse.MqInfo;
import io.shulie.takin.web.biz.pojo.response.application.ApplicationEntranceTopologyResponse.NodeDetailDatasourceInfo;
import io.shulie.takin.web.biz.pojo.response.application.ApplicationEntranceTopologyResponse.NodeTypeResponseEnum;
import io.shulie.takin.web.biz.pojo.response.application.ApplicationEntranceTopologyResponse.OssInfo;
import io.shulie.takin.web.biz.pojo.response.application.ApplicationEntranceTopologyResponse.TopologyAppNodeResponse;
import io.shulie.takin.web.biz.pojo.response.application.ApplicationEntranceTopologyResponse.TopologyCacheNodeResponse;
import io.shulie.takin.web.biz.pojo.response.application.ApplicationEntranceTopologyResponse.TopologyDbNodeResponse;
import io.shulie.takin.web.biz.pojo.response.application.ApplicationEntranceTopologyResponse.TopologyMqNodeResponse;
import io.shulie.takin.web.biz.pojo.response.application.ApplicationEntranceTopologyResponse.AbstractTopologyNodeResponse;
import io.shulie.takin.web.biz.pojo.response.application.ApplicationEntranceTopologyResponse.TopologyOssNodeResponse;
import io.shulie.takin.web.biz.pojo.response.application.ApplicationEntranceTopologyResponse.TopologyOtherNodeResponse;
import io.shulie.takin.web.biz.pojo.response.application.ApplicationEntranceTopologyResponse.TopologyUnknownNodeResponse;
import io.shulie.takin.web.biz.pojo.response.application.ApplicationEntranceTopologyResponse.TopologyVirtualNodeResponse;
import io.shulie.takin.web.biz.service.application.ApplicationMiddlewareService;
import io.shulie.takin.web.common.enums.application.ApplicationMiddlewareStatusEnum;
import io.shulie.takin.web.data.dao.application.ApplicationDAO;
import io.shulie.takin.web.data.result.application.ApplicationResult;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 链路拓扑图 接口--拓扑图信息从AMDB获取
 *
 * @author
 */
@Service
public class LinkTopologyService extends CommonService {

    @Autowired
    private ApplicationMiddlewareService applicationMiddlewareService;

    @Autowired
    private ApplicationClient applicationClient;

    @Autowired
    private ApplicationDAO applicationDAO;

    @Autowired
    private ApplicationEntranceClient applicationEntranceClient;

    /**
     * 拓扑图查询, 处理
     *
     * @param request 请求入参
     *
     * @return 拓扑图相关
     */
    public ApplicationEntranceTopologyResponse getApplicationEntrancesTopology(
            ApplicationEntranceTopologyQueryRequest request) {
        // 大数据查询拓扑图
        LinkTopologyDTO applicationEntrancesTopology = applicationEntranceClient.getApplicationEntrancesTopology(
                request.getApplicationName(), request.getLinkId(), request.getServiceName(), request.getMethod(),
                request.getRpcType(), request.getExtend());

        ApplicationEntranceTopologyResponse applicationEntranceTopologyResponse
                = new ApplicationEntranceTopologyResponse();
        if (applicationEntrancesTopology == null) {
            applicationEntranceTopologyResponse.setNodes(Lists.newArrayList());
            applicationEntranceTopologyResponse.setEdges(Lists.newArrayList());
            applicationEntranceTopologyResponse.setExceptions(Lists.newArrayList());

            return applicationEntranceTopologyResponse;

        }

        /* key:nodeId ,value: node */
        Map<String, LinkNodeDTO> nodeMap = Maps.newHashMap();
        /* key:nodeId ,value: 从node开始的边 */
        Map<String, List<LinkEdgeDTO>> providerEdgeMap = Maps.newHashMap();
        /* key:nodeId ,value: 访问到node的边 */
        Map<String, List<LinkEdgeDTO>> callEdgeMap = Maps.newHashMap();
        /* key:applicationName ,value: managerName */
        Map<String, String> managerMap = Maps.newHashMap();
        /* key:applicationName ,value: 应用实例节点 */
        Map<String, List<ApplicationNodeDTO>> appNodeMap = Maps.newHashMap();

        List<LinkNodeDTO> nodes = applicationEntrancesTopology.getNodes();
        if (CollectionUtils.isNotEmpty(nodes)) {
            nodeMap = nodes.stream().collect(Collectors.toMap(LinkNodeDTO::getNodeId, self -> self));
            managerMap = this.getManagers(nodes);
            appNodeMap = this.getAppNodes(nodes);
        }

        List<LinkEdgeDTO> edges = applicationEntrancesTopology.getEdges();
        if (CollectionUtils.isNotEmpty(edges)) {
            providerEdgeMap = edges.stream().collect(Collectors.groupingBy(LinkEdgeDTO::getTargetId));
            callEdgeMap = edges.stream().collect(Collectors.groupingBy(LinkEdgeDTO::getSourceId));
        }

        // 节点转换
        applicationEntranceTopologyResponse.setNodes(this.convertNodes(applicationEntrancesTopology, nodeMap,
                providerEdgeMap, callEdgeMap, managerMap, appNodeMap));
        // 边 转换
        applicationEntranceTopologyResponse.setEdges(this.convertEdges(applicationEntrancesTopology, nodeMap,
                providerEdgeMap, callEdgeMap, request));

        // 异常转换
        applicationEntranceTopologyResponse.setExceptions(
                this.getExceptionsFromNodes(applicationEntranceTopologyResponse.getNodes()));
        return applicationEntranceTopologyResponse;
    }

    private Map<String, List<ApplicationNodeDTO>> getAppNodes(List<LinkNodeDTO> nodes) {
        if (CollectionUtils.isEmpty(nodes)) {
            return Maps.newHashMap();
        }
        List<String> appNames = nodes.stream()
                //.filter(node -> node.getNodeTypeGroup().equalsIgnoreCase(NodeTypeGroupEnum.APP.name()))
                .map(LinkNodeDTO::getNodeName)
                .collect(Collectors.toList());
        ApplicationNodeQueryDTO applicationNodeQueryDTO = new ApplicationNodeQueryDTO();
        applicationNodeQueryDTO.setPageSize(9999);
        applicationNodeQueryDTO.setAppNames(StringUtils.join(appNames, ","));
        PagingList<ApplicationNodeDTO> applicationNodeDtoPagingList = applicationClient.pageApplicationNodes(
                applicationNodeQueryDTO);
        if (applicationNodeDtoPagingList.getTotal() == 0) {
            return Maps.newHashMap();
        }
        return applicationNodeDtoPagingList.getList().stream().collect(
                Collectors.groupingBy(ApplicationNodeDTO::getAppName));
    }

    private Map<String, String> getManagers(List<LinkNodeDTO> nodes) {
        if (CollectionUtils.isEmpty(nodes)) {
            return Maps.newHashMap();
        }
        List<String> applicationNames = nodes.stream().filter(node -> nodeIsApp(node) && !isVirtualNode(node)).map(
                LinkNodeDTO::getNodeName).collect(Collectors.toList());

        List<ApplicationResult> applicationsByName = applicationDAO.getApplicationByName(applicationNames);
        if (CollectionUtils.isEmpty(applicationsByName)) {
            return Maps.newHashMap();
        }
        Map<String, String> appNameManagerNameMap = applicationsByName.stream()
                .filter(app -> StringUtils.isNotBlank(app.getAppManagerName()))
                .collect(Collectors.toMap(ApplicationResult::getAppName, ApplicationResult::getAppManagerName));
        Map<String, String> map = Maps.newHashMap();
        for (LinkNodeDTO node : nodes) {
            if (nodeIsApp(node) && !isVirtualNode(node)) {
                map.put(node.getNodeName(), appNameManagerNameMap.get(node.getNodeName()));
            }
        }
        return map;
    }

    private List<AbstractTopologyNodeResponse> convertNodes(
            LinkTopologyDTO applicationEntrancesTopology,
            Map<String, LinkNodeDTO> nodeMap, Map<String, List<LinkEdgeDTO>> providerEdgeMap,
            Map<String, List<LinkEdgeDTO>> callEdgeMap, Map<String, String> managerMap,
            Map<String, List<ApplicationNodeDTO>> appNodeMap) {
        if (CollectionUtils.isEmpty(applicationEntrancesTopology.getNodes())) {
            return Lists.newArrayList();
        }

        return applicationEntrancesTopology.getNodes().stream().map(node -> {
                    if (NodeTypeGroupEnum.OTHER.getType().equals(node.getNodeTypeGroup()) && isVirtualNode(node)) {
                        TopologyVirtualNodeResponse nodeResponse = new TopologyVirtualNodeResponse();
                        setNodeDefaultResponse(nodeResponse, node, nodeMap, providerEdgeMap, callEdgeMap);
                        setVirtualResponse(nodeResponse, node, nodeMap, providerEdgeMap, callEdgeMap, managerMap, appNodeMap);
                        return nodeResponse;
                    }
                    if (NodeTypeGroupEnum.OTHER.getType().equals(node.getNodeTypeGroup()) && isOuterService(node)) {
                        TopologyOtherNodeResponse nodeResponse = new TopologyOtherNodeResponse();
                        setNodeDefaultResponse(nodeResponse, node, nodeMap, providerEdgeMap, callEdgeMap);
                        setOtherResponse(nodeResponse, node, nodeMap, providerEdgeMap, callEdgeMap, managerMap, appNodeMap);
                        return nodeResponse;
                    }
                    if (NodeTypeGroupEnum.OTHER.getType().equals(node.getNodeTypeGroup()) && isUnknownNode(node)) {
                        TopologyUnknownNodeResponse nodeResponse = new TopologyUnknownNodeResponse();
                        setNodeDefaultResponse(nodeResponse, node, nodeMap, providerEdgeMap, callEdgeMap);
                        setUnknownResponse(nodeResponse, node, nodeMap, providerEdgeMap, callEdgeMap, managerMap, appNodeMap);
                        return nodeResponse;
                    }
                    if (NodeTypeGroupEnum.APP.getType().equals(node.getNodeTypeGroup())) {
                        TopologyAppNodeResponse nodeResponse = new TopologyAppNodeResponse();
                        setNodeDefaultResponse(nodeResponse, node, nodeMap, providerEdgeMap, callEdgeMap);
                        setAppNodeResponse(nodeResponse, node, nodeMap, providerEdgeMap, callEdgeMap, managerMap, appNodeMap);
                        return nodeResponse;
                    }
                    if (NodeTypeGroupEnum.OSS.getType().equals(node.getNodeTypeGroup())) {
                        TopologyOssNodeResponse nodeResponse = new TopologyOssNodeResponse();
                        setNodeDefaultResponse(nodeResponse, node, nodeMap, providerEdgeMap, callEdgeMap);
                        setOssNodeResponse(nodeResponse, node, nodeMap, providerEdgeMap, callEdgeMap, appNodeMap);
                        return nodeResponse;
                    }
                    if (NodeTypeGroupEnum.CACHE.getType().equals(node.getNodeTypeGroup())) {
                        TopologyCacheNodeResponse nodeResponse = new TopologyCacheNodeResponse();
                        setNodeDefaultResponse(nodeResponse, node, nodeMap, providerEdgeMap, callEdgeMap);
                        setCacheNodeResponse(nodeResponse, node, nodeMap, providerEdgeMap, callEdgeMap, appNodeMap);
                        return nodeResponse;
                    }
                    if (NodeTypeGroupEnum.DB.getType().equals(node.getNodeTypeGroup())) {
                        TopologyDbNodeResponse nodeResponse = new TopologyDbNodeResponse();
                        setNodeDefaultResponse(nodeResponse, node, nodeMap, providerEdgeMap, callEdgeMap);
                        setDbNodeResponse(nodeResponse, node, nodeMap, providerEdgeMap, callEdgeMap, appNodeMap);
                        return nodeResponse;
                    }
                    if (NodeTypeGroupEnum.MQ.getType().equals(node.getNodeTypeGroup())) {
                        TopologyMqNodeResponse nodeResponse = new TopologyMqNodeResponse();
                        setNodeDefaultResponse(nodeResponse, node, nodeMap, providerEdgeMap, callEdgeMap);
                        setMqNodeResponse(nodeResponse, node, nodeMap, providerEdgeMap, callEdgeMap, appNodeMap);
                        return nodeResponse;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void setUnknownResponse(TopologyUnknownNodeResponse nodeResponse, LinkNodeDTO node,
                                    Map<String, LinkNodeDTO> nodeMap, Map<String, List<LinkEdgeDTO>> providerEdgeMap,
                                    Map<String, List<LinkEdgeDTO>> callEdgeMap, Map<String, String> managerMap,
                                    Map<String, List<ApplicationNodeDTO>> appNodeMap) {
        nodeResponse.setNodes(getNodeDetails(node, nodeMap, providerEdgeMap, callEdgeMap, appNodeMap));
        nodeResponse.setProviderService(convertAppNodeProviderService(node, nodeMap, providerEdgeMap, callEdgeMap));
    }

    /**
     * 获取上游节点名称列表
     *
     * @return
     */
    private List<String> getUpAppNames(LinkNodeDTO node, Map<String, LinkNodeDTO> nodeMap,
                                       Map<String, List<LinkEdgeDTO>> providerEdgeMap) {
        if (MapUtils.isEmpty(providerEdgeMap) || CollectionUtils.isEmpty(providerEdgeMap.get(node.getNodeId()))) {
            return new ArrayList<>();
        }
        List<String> updateNodeIdList = providerEdgeMap.get(node.getNodeId()).stream().map(LinkEdgeDTO::getSourceId)
                .collect(
                        Collectors.toList());
        return updateNodeIdList.stream().map(upNodeId -> {
            LinkNodeDTO linkNodeDTO = nodeMap.get(upNodeId);
            if (linkNodeDTO != null) {
                return nodeMap.get(upNodeId).getNodeName();
            }
            return null;
        }).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
    }

    private void setOtherResponse(TopologyOtherNodeResponse nodeResponse, LinkNodeDTO node,
                                  Map<String, LinkNodeDTO> nodeMap, Map<String, List<LinkEdgeDTO>> providerEdgeMap,
                                  Map<String, List<LinkEdgeDTO>> callEdgeMap, Map<String, String> managerMap,
                                  Map<String, List<ApplicationNodeDTO>> appNodeMap) {
        nodeResponse.setNodes(getNodeDetails(node, nodeMap, providerEdgeMap, callEdgeMap, appNodeMap));
        nodeResponse.setProviderService(convertAppNodeProviderService(node, nodeMap, providerEdgeMap, callEdgeMap));
    }

    private void setVirtualResponse(TopologyVirtualNodeResponse nodeResponse, LinkNodeDTO node,
                                    Map<String, LinkNodeDTO> nodeMap, Map<String, List<LinkEdgeDTO>> providerEdgeMap,
                                    Map<String, List<LinkEdgeDTO>> callEdgeMap, Map<String, String> managerMap,
                                    Map<String, List<ApplicationNodeDTO>> appNodeMap) {
        nodeResponse.setNodes(getNodeDetails(node, nodeMap, providerEdgeMap, callEdgeMap, appNodeMap));
    }

    private void setMqNodeResponse(TopologyMqNodeResponse nodeResponse,
                                   LinkNodeDTO node,
                                   Map<String, LinkNodeDTO> nodeMap, Map<String, List<LinkEdgeDTO>> providerEdgeMap,
                                   Map<String, List<LinkEdgeDTO>> callEdgeMap,
                                   Map<String, List<ApplicationNodeDTO>> appNodeMap) {

        List<LinkEdgeDTO> linkEdgeDtoList = providerEdgeMap.get(node.getNodeId());
        List<MqInfo> mqs = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(linkEdgeDtoList)) {
            mqs = linkEdgeDtoList.stream().map(edge -> {
                MqInfo mqInfo = new MqInfo();
                mqInfo.setTopic(edge.getService());
                return mqInfo;
            }).collect(Collectors.toList());
        }
        nodeResponse.setMq(mqs);
        nodeResponse.setNodes(getNodeDetails(node, nodeMap, providerEdgeMap, callEdgeMap, appNodeMap));
        if (isUnknownNode(node) || isOuterService(node)) {
            nodeResponse.setProviderService(convertAppNodeProviderService(node, nodeMap, providerEdgeMap, callEdgeMap));
        }
    }

    private void setDbNodeResponse(TopologyDbNodeResponse nodeResponse,
                                   LinkNodeDTO node,
                                   Map<String, LinkNodeDTO> nodeMap, Map<String, List<LinkEdgeDTO>> providerEdgeMap,
                                   Map<String, List<LinkEdgeDTO>> callEdgeMap,
                                   Map<String, List<ApplicationNodeDTO>> appNodeMap) {

        List<LinkEdgeDTO> linkEdgeDtoListS = providerEdgeMap.get(node.getNodeId());
        List<DbInfo> dbs = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(linkEdgeDtoListS)) {
            dbs = linkEdgeDtoListS.stream().map(edge -> {
                DbInfo db = new DbInfo();
                db.setTableName(edge.getMethod());
                return db;
            }).collect(Collectors.toList());
        }
        nodeResponse.setDb(dbs);
        nodeResponse.setNodes(getNodeDetails(node, nodeMap, providerEdgeMap, callEdgeMap, appNodeMap));
    }

    private void setCacheNodeResponse(TopologyCacheNodeResponse nodeResponse,
                                      LinkNodeDTO node,
                                      Map<String, LinkNodeDTO> nodeMap, Map<String, List<LinkEdgeDTO>> providerEdgeMap,
                                      Map<String, List<LinkEdgeDTO>> callEdgeMap,
                                      Map<String, List<ApplicationNodeDTO>> appNodeMap) {
        nodeResponse.setNodes(getNodeDetails(node, nodeMap, providerEdgeMap, callEdgeMap, appNodeMap));
    }

    private void setOssNodeResponse(TopologyOssNodeResponse nodeResponse,
                                    LinkNodeDTO node,
                                    Map<String, LinkNodeDTO> nodeMap, Map<String, List<LinkEdgeDTO>> providerEdgeMap,
                                    Map<String, List<LinkEdgeDTO>> callEdgeMap,
                                    Map<String, List<ApplicationNodeDTO>> appNodeMap) {

        List<LinkEdgeDTO> linkEdgeDtoList = providerEdgeMap.get(node.getNodeId());
        List<OssInfo> ossInfoLists = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(linkEdgeDtoList)) {
            ossInfoLists = linkEdgeDtoList.stream().map(edge -> {
                OssInfo ossInfo = new OssInfo();
                ossInfo.setFilePath(edge.getService());
                return ossInfo;
            }).collect(Collectors.toList());
        }
        nodeResponse.setNodes(getNodeDetails(node, nodeMap, providerEdgeMap, callEdgeMap, appNodeMap));
        nodeResponse.setOss(ossInfoLists);
    }

    private void setAppNodeResponse(TopologyAppNodeResponse nodeResponse,
                                    LinkNodeDTO node,
                                    Map<String, LinkNodeDTO> nodeMap, Map<String, List<LinkEdgeDTO>> providerEdgeMap,
                                    Map<String, List<LinkEdgeDTO>> callEdgeMap, Map<String, String> managerMap,
                                    Map<String, List<ApplicationNodeDTO>> appNodeMap) {
        nodeResponse.setManager(managerMap.get(node.getNodeName()));
        nodeResponse.setNodes(getNodeDetails(node, nodeMap, providerEdgeMap, callEdgeMap, appNodeMap));
        nodeResponse.setCallService(
                convertAppNodeCallService(node, nodeMap, providerEdgeMap, callEdgeMap, appNodeMap));
        nodeResponse.setProviderService(convertAppNodeProviderService(node, nodeMap, providerEdgeMap, callEdgeMap));
    }

    private List<AppProviderInfo> convertAppNodeProviderService(LinkNodeDTO node,
                                                                Map<String, LinkNodeDTO> nodeMap,
                                                                Map<String, List<LinkEdgeDTO>> providerEdgeMap, Map<String, List<LinkEdgeDTO>> callEdgeMap) {
        // 本服务提供的边
        List<LinkEdgeDTO> providerEdge = providerEdgeMap.get(node.getNodeId());
        if (CollectionUtils.isEmpty(providerEdge)) {
            return Lists.newArrayList();
        }
        Map<String, List<LinkEdgeDTO>> providerEdgeByType = providerEdge.stream().collect(
                Collectors.groupingBy(LinkEdgeDTO::getMiddlewareName));

        return providerEdgeByType.entrySet().stream().map(entry -> {
            AppProviderInfo appProviderInfo
                    = new AppProviderInfo();
            appProviderInfo.setLabel(entry.getKey());
            List<AppProviderDatasourceInfo> datasource = Lists.newArrayList();
            if (CollectionUtils.isNotEmpty(entry.getValue())) {
                // 根据service + name 2次分组，汇总所有的节点名称；
                Map<String, List<LinkEdgeDTO>> serviceMethodGroup = entry.getValue().stream().collect(
                        Collectors.groupingBy(e -> e.getService() + "#" + e.getMethod()));
                datasource = serviceMethodGroup.entrySet().stream().map(item -> {
                    AppProviderDatasourceInfo appProviderDatasourceInfo
                            = new AppProviderDatasourceInfo();
                    appProviderDatasourceInfo.setServiceName(item.getKey());
                    if (CollectionUtils.isNotEmpty(item.getValue())) {
                        if (nodeMap != null) {
                            appProviderDatasourceInfo.setBeforeApps(item.getValue().stream()
                                    .filter(Objects::nonNull)
                                    .map(LinkEdgeDTO::getSourceId)
                                    .filter(StrUtil::isNotBlank)
                                    .map(nodeMap::get)
                                    .filter(Objects::nonNull)
                                    .map(LinkNodeDTO::getNodeName)
                                    .filter(StrUtil::isNotBlank)
                                    .collect(Collectors.joining(","))
                            );
                        }
                    }
                    return appProviderDatasourceInfo;
                }).collect(Collectors.toList());
            }
            appProviderInfo.setDataSource(datasource);
            return appProviderInfo;
        }).collect(Collectors.toList());
    }

    private List<AppCallInfo> convertAppNodeCallService(LinkNodeDTO node,
                                                        Map<String, LinkNodeDTO> nodeMap,
                                                        Map<String, List<LinkEdgeDTO>> providerEdgeMap, Map<String, List<LinkEdgeDTO>> callEdgeMap,
                                                        Map<String, List<ApplicationNodeDTO>> appNodeMap) {
        // 本服务调用的边
        List<LinkEdgeDTO> callEdges = callEdgeMap.get(node.getNodeId());
        if (CollectionUtils.isEmpty(callEdges)) {
            return Lists.newArrayList();
        }
        Map<String, List<LinkEdgeDTO>> excludeMqNodeWithEdge = callEdges.stream().collect(
                Collectors.groupingBy(LinkEdgeDTO::getTargetId));
        return getCallInfo(nodeMap, excludeMqNodeWithEdge, appNodeMap);
    }

    private List<AppCallInfo> getCallInfo(Map<String, LinkNodeDTO> nodeMap,
                                          Map<String, List<LinkEdgeDTO>> excludeMqNodeWithEdge,
                                          Map<String, List<ApplicationNodeDTO>> appNodeMap) {
        return excludeMqNodeWithEdge.entrySet().stream().map(entry -> {
            LinkNodeDTO linkNodeDTO = nodeMap.get(entry.getKey());
            AppCallInfo appCallInfo
                    = new AppCallInfo();
            appCallInfo.setNodeType(NodeTypeResponseEnum
                    .getTypeByAmdbType(linkNodeDTO.getNodeTypeGroup()));
            appCallInfo.setLabel(linkNodeDTO.getNodeType().toUpperCase());
            appCallInfo.setDataSource(convertCallTypeInfo(entry.getValue(), linkNodeDTO, nodeMap, appNodeMap));
            return appCallInfo;
        }).collect(Collectors.toList());
    }

    private List<AppCallDatasourceInfo> convertCallTypeInfo(List<LinkEdgeDTO> edges,
                                                            LinkNodeDTO node,
                                                            Map<String, LinkNodeDTO> nodeMap,
                                                            Map<String, List<ApplicationNodeDTO>> appNodeMap) {
        if (CollectionUtils.isEmpty(edges)) {
            return Lists.newArrayList();
        }

        if (NodeTypeGroupEnum.APP.name().equals(node.getNodeTypeGroup())) {
            List<AppCallDatasourceInfo> infos = new ArrayList<>();

            AppCallDatasourceInfo info1
                    = new AppCallDatasourceInfo();
            info1.setLabel("节点名称");
            info1.setDataSource(Lists.newArrayList(node.getNodeName()));
            infos.add(info1);

            AppCallDatasourceInfo info2
                    = new AppCallDatasourceInfo();
            info2.setLabel("节点");
            List<ApplicationNodeDTO> applicationNodeDtoList = appNodeMap.get(node.getNodeName());
            if (CollectionUtils.isNotEmpty(applicationNodeDtoList)) {
                info2.setDataSource(applicationNodeDtoList.stream().map(ApplicationNodeDTO::getIpAddress).collect(
                        Collectors.toList()));
            } else {
                info2.setDataSource(Lists.newArrayList());
            }
            infos.add(info2);

            AppCallDatasourceInfo info3
                    = new AppCallDatasourceInfo();
            info3.setLabel("服务");
            info3.setDataSource(
                    edges.stream().map(edge -> edge.getService() + "#" + edge.getMethod()).collect(Collectors.toList()));
            infos.add(info3);

            return infos;
        }

        if (NodeTypeGroupEnum.CACHE.name().equals(node.getNodeTypeGroup())) {
            List<AppCallDatasourceInfo> infos = new ArrayList<>();

            AppCallDatasourceInfo info1
                    = new AppCallDatasourceInfo();
            info1.setLabel("节点");
            NodeExtendInfoForCacheDTO extend = convertNodeExtendInfo(node.getExtendInfo(),
                    NodeExtendInfoForCacheDTO.class);
            info1.setDataSource(Lists.newArrayList(extend.getIp() + ":" + extend.getPort()));
            infos.add(info1);

            return infos;
        }
        if (NodeTypeGroupEnum.OSS.name().equals(node.getNodeTypeGroup())) {
            List<AppCallDatasourceInfo> infos = new ArrayList<>();

            AppCallDatasourceInfo info2
                    = new AppCallDatasourceInfo();
            info2.setLabel("文件路径");
            info2.setDataSource(edges.stream().map(LinkEdgeDTO::getService).collect(Collectors.toList()));
            infos.add(info2);

            return infos;
        }
        if (NodeTypeGroupEnum.MQ.name().equals(node.getNodeTypeGroup())) {
            List<AppCallDatasourceInfo> infos = new ArrayList<>();

            AppCallDatasourceInfo info1
                    = new AppCallDatasourceInfo();
            info1.setLabel("节点名称");
            info1.setDataSource(Lists.newArrayList(node.getNodeName()));
            infos.add(info1);

            AppCallDatasourceInfo info2
                    = new AppCallDatasourceInfo();
            info2.setLabel("Topic");
            info2.setDataSource(edges.stream().map(LinkEdgeDTO::getService).collect(Collectors.toList()));
            infos.add(info2);

            return infos;
        }
        if (NodeTypeGroupEnum.DB.name().equals(node.getNodeTypeGroup())) {
            List<AppCallDatasourceInfo> infos = new ArrayList<>();

            AppCallDatasourceInfo info1
                    = new AppCallDatasourceInfo();
            info1.setLabel("节点名称");
            info1.setDataSource(Lists.newArrayList(node.getNodeName()));
            infos.add(info1);

            AppCallDatasourceInfo info2
                    = new AppCallDatasourceInfo();
            info2.setLabel("库名称");
            info2.setDataSource(Lists.newArrayList(edges.stream().map(LinkEdgeDTO::getService).distinct()
                    .collect(Collectors.joining(","))));
            infos.add(info2);

            AppCallDatasourceInfo info3
                    = new AppCallDatasourceInfo();
            info3.setLabel("表名称");
            info3.setDataSource(Lists.newArrayList(edges.stream().map(LinkEdgeDTO::getMethod).distinct()
                    .collect(Collectors.joining(","))));
            infos.add(info3);

            return infos;
        }
        if (NodeTypeGroupEnum.SEARCH.name().equals(node.getNodeTypeGroup())) {
            List<AppCallDatasourceInfo> infos = new ArrayList<>();
            AppCallDatasourceInfo info1
                    = new AppCallDatasourceInfo();
            info1.setLabel("节点名称");
            info1.setDataSource(Lists.newArrayList(node.getNodeName()));
            infos.add(info1);
            return infos;
        }
        if (NodeTypeGroupEnum.OTHER.name().equals(node.getNodeTypeGroup())) {
            List<AppCallDatasourceInfo> infos = new ArrayList<>();
            AppCallDatasourceInfo info1
                    = new AppCallDatasourceInfo();
            info1.setLabel("节点名称");
            info1.setDataSource(Lists.newArrayList(node.getNodeName()));
            infos.add(info1);
            return infos;
        }
        throw new RuntimeException("不支持的节点类型:" + node.getNodeTypeGroup());
    }

    private void setNodeDefaultResponse(AbstractTopologyNodeResponse nodeResponse,
                                        LinkNodeDTO node,
                                        Map<String, LinkNodeDTO> nodeMap,
                                        Map<String, List<LinkEdgeDTO>> providerEdgeMap,
                                        Map<String, List<LinkEdgeDTO>> callEdgeMap) {
        // 不是未知且不是三方服务
        if (!isUnknownNode(node) && !isOuterService(node)) {
            nodeResponse.setNodeType(
                    NodeTypeResponseEnum.getTypeByAmdbType(node.getNodeTypeGroup()));
        }
        // 如果是未知服务
        if (isUnknownNode(node)) {
            nodeResponse.setNodeType(NodeTypeResponseEnum.UNKNOWN);
        }
        // 第三方服务
        if (isOuterService(node)) {
            nodeResponse.setNodeType(NodeTypeResponseEnum.OUTER);
        }
        // 虚拟节点
        if (isVirtualNode(node)) {
            nodeResponse.setNodeType(NodeTypeResponseEnum.VIRTUAL);
        }
        nodeResponse.setLabel(getNodeInfoLabel(node));
        nodeResponse.setRoot(isRoot(node));
        nodeResponse.setId(node.getNodeId());
        nodeResponse.setUpAppNames(getUpAppNames(node, nodeMap, providerEdgeMap));
    }

    /**
     * 代码暂时看着是一模一样，未来不同类型应该是不一样的，这样写为了拓展。
     */
    private List<NodeDetailDatasourceInfo> getNodeDetails(LinkNodeDTO node,
                                                          Map<String, LinkNodeDTO> nodeMap,
                                                          Map<String, List<LinkEdgeDTO>> providerEdgeMap, Map<String, List<LinkEdgeDTO>> callEdgeMap,
                                                          Map<String, List<ApplicationNodeDTO>> appNodeMap) {
        if (NodeTypeGroupEnum.MQ.name().equals(node.getNodeTypeGroup())) {
            NodeExtendInfoForMQDTO extendInfo = convertNodeExtendInfo(node.getExtendInfo(),
                    NodeExtendInfoForMQDTO.class);
            NodeDetailDatasourceInfo nodeDetailDatasourceInfo
                    = new NodeDetailDatasourceInfo();
            nodeDetailDatasourceInfo.setNode(extendInfo.getIp() + ":" + extendInfo.getPort());
            return Lists.newArrayList(nodeDetailDatasourceInfo);
        }
        if (NodeTypeGroupEnum.CACHE.name().equals(node.getNodeTypeGroup())) {
            NodeExtendInfoForCacheDTO extendInfo = convertNodeExtendInfo(node.getExtendInfo(),
                    NodeExtendInfoForCacheDTO.class);
            NodeDetailDatasourceInfo nodeDetailDatasourceInfo
                    = new NodeDetailDatasourceInfo();
            nodeDetailDatasourceInfo.setNode(extendInfo.getIp() + ":" + extendInfo.getPort());
            return Lists.newArrayList(nodeDetailDatasourceInfo);
        }
        if (NodeTypeGroupEnum.DB.name().equals(node.getNodeTypeGroup())) {
            NodeExtendInfoForDBDTO extendInfo = convertNodeExtendInfo(node.getExtendInfo(),
                    NodeExtendInfoForDBDTO.class);
            NodeDetailDatasourceInfo nodeDetailDatasourceInfo
                    = new NodeDetailDatasourceInfo();
            nodeDetailDatasourceInfo.setNode(extendInfo.getIp() + ":" + extendInfo.getPort());
            return Lists.newArrayList(nodeDetailDatasourceInfo);
        }
        if (NodeTypeGroupEnum.OSS.name().equals(node.getNodeTypeGroup())) {
            NodeExtendInfoForOSSDTO extendInfo = convertNodeExtendInfo(node.getExtendInfo(),
                    NodeExtendInfoForOSSDTO.class);
            NodeDetailDatasourceInfo nodeDetailDatasourceInfo
                    = new NodeDetailDatasourceInfo();
            nodeDetailDatasourceInfo.setNode(extendInfo.getIp() + ":" + extendInfo.getPort());
            return Lists.newArrayList(nodeDetailDatasourceInfo);
        }
        if (NodeTypeGroupEnum.SEARCH.name().equals(node.getNodeTypeGroup())) {
            NodeExtendInfoForSearchDTO extendInfo = convertNodeExtendInfo(node.getExtendInfo(),
                    NodeExtendInfoForSearchDTO.class);
            NodeDetailDatasourceInfo nodeDetailDatasourceInfo
                    = new NodeDetailDatasourceInfo();
            nodeDetailDatasourceInfo.setNode(extendInfo.getIp() + ":" + extendInfo.getPort());
            return Lists.newArrayList(nodeDetailDatasourceInfo);
        }
        if (NodeTypeGroupEnum.APP.name().equals(node.getNodeTypeGroup())) {
            if (CollectionUtils.isNotEmpty(appNodeMap.get(node.getNodeName()))) {
                return appNodeMap.get(node.getNodeName()).stream().map(nodeDTO -> {
                    NodeDetailDatasourceInfo nodeDetailDatasourceInfo = new NodeDetailDatasourceInfo();
                    nodeDetailDatasourceInfo.setNode(nodeDTO.getIpAddress());
                    return nodeDetailDatasourceInfo;
                }).collect(Collectors.toList());
            }
            return Lists.newArrayList();
        }
        if (isUnknownNode(node) || isOuterService(node)) {
            NodeExtendInfoBaseDTO extendInfo = convertNodeExtendInfo(node.getExtendInfo(),
                    NodeExtendInfoBaseDTO.class);
            NodeDetailDatasourceInfo nodeDetailDatasourceInfo
                    = new NodeDetailDatasourceInfo();
            nodeDetailDatasourceInfo.setNode(extendInfo.getIp() + ":" + extendInfo.getPort());
            return Lists.newArrayList(nodeDetailDatasourceInfo);
        }
        return null;
    }

    /**
     * 拓扑图异常的转换
     *
     * @param nodes 节点列表
     *
     * @return 拓扑图异常
     */
    private List<ExceptionListResponse> getExceptionsFromNodes(List<AbstractTopologyNodeResponse> nodes) {
        if (CollectionUtils.isEmpty(nodes)) {
            return Collections.emptyList();
        }

        // 未知节点异常
        List<ExceptionListResponse> unknownExceptions = nodes.stream().filter(this::isUnknownResponseNode)
                .map(node -> {
                    ExceptionListResponse exception = new ExceptionListResponse();
                    StringBuilder exceptionTitle = new StringBuilder();
                    exceptionTitle.append("节点\"")
                            .append(StringUtils.join(node.getUpAppNames(), ","))
                            .append("\"下游存在应用未接入探针");
                    if (CollectionUtils.isNotEmpty(node.getNodes())) {
                        exceptionTitle.append("，节点IP:")
                                .append(StringUtils.join(node.getNodes().stream()
                                        .map(NodeDetailDatasourceInfo::getNode)
                                        .collect(Collectors.toList()), ","));
                    }

                    exception.setTitle(exceptionTitle.toString());
                    exception.setType("应用未接入探针");
                    exception.setSuggest("确认该服务为外部服务或内部服务提供，若为外部服务请将其标记为外部服务；若为内部应用请将其接入探针");
                    return exception;
                }).collect(Collectors.toList());

        // 应用的中间件异常
        List<ExceptionListResponse> middlewareExceptions = this.getMiddlewareExceptionList(nodes);
        unknownExceptions.addAll(middlewareExceptions);
        return unknownExceptions;
    }

    /**
     * 拓扑图下的应用中间件异常判断
     * <p>
     * 遍历节点, 根据应用名查询 应用id
     * 根据应用id 查询其下所有中间件状态
     * 然后判断, 无, 未知, 未支持
     *
     * @param nodes 节点
     *
     * @return 中间件异常
     */
    private List<ExceptionListResponse> getMiddlewareExceptionList(List<AbstractTopologyNodeResponse> nodes) {
        // 收集所有的 应用名称
        List<String> applicationNameList = nodes.stream().flatMap(node -> node.getUpAppNames().stream())
                .distinct().collect(Collectors.toList());
        if (applicationNameList.isEmpty()) {
            return Collections.emptyList();
        }

        // 查询出所有的 名称对应的中间件状态
        Map<String, Map<Integer, Integer>> applicationNameAboutStatusCountMap = applicationMiddlewareService
                .getApplicationNameAboutStatusCountMap(applicationNameList);
        if (applicationNameAboutStatusCountMap.isEmpty()) {
            return Collections.emptyList();
        }

        // 分隔处理
        int total = applicationNameList.size();
        int size = 500;
        int page = total / size + 1;

        // 从 0 开始, 但是是小于 page, 没有等于
        // 所以 页数的个数 是一样的
        // skip 从 0 开始, 0, 1; 2, 3; 4, 5; xxx
        return Stream.iterate(0, f -> f + 1).limit(page).parallel().flatMap(currentPage ->
                        applicationNameList.stream().skip(currentPage * size).limit(size)
                                .map(applicationName -> {
                                    Map<Integer, Integer> statusAboutCount = applicationNameAboutStatusCountMap.get(applicationName);
                                    if (statusAboutCount == null) {
                                        return null;
                                    }

                                    ExceptionListResponse exception = new ExceptionListResponse();
                                    exception.setTitle(String.format("应用: %s 存在不支持的中间件", applicationName));
                                    exception.setType("应用中间件不支持");

                                    StringBuilder sb = new StringBuilder();
                                    sb.append("该应用存在 ");
                                    if (statusAboutCount.get(ApplicationMiddlewareStatusEnum.NONE.getCode()) != null
                                            && statusAboutCount.get(ApplicationMiddlewareStatusEnum.NONE.getCode()) > 0) {
                                        sb.append("无状态 ");
                                    }

                                    if (statusAboutCount.get(ApplicationMiddlewareStatusEnum.UNKNOWN.getCode()) != null
                                            && statusAboutCount.get(ApplicationMiddlewareStatusEnum.UNKNOWN.getCode()) > 0) {
                                        sb.append("未知 ");
                                    }

                                    if (statusAboutCount.get(ApplicationMiddlewareStatusEnum.NOT_SUPPORTED.getCode()) != null
                                            && statusAboutCount.get(ApplicationMiddlewareStatusEnum.NOT_SUPPORTED.getCode()) > 0) {
                                        sb.append("未支持 ");
                                    }

                                    sb.append("的中间件, 请前往查看");
                                    exception.setSuggest(sb.toString());
                                    return exception;
                                }).filter(Objects::nonNull).collect(Collectors.toList()).stream())
                .collect(Collectors.toList());
    }

    private List<ApplicationEntranceTopologyEdgeResponse> convertEdges(
            LinkTopologyDTO applicationEntrancesTopology,
            Map<String, LinkNodeDTO> nodeMap, Map<String, List<LinkEdgeDTO>> providerEdgeMap,
            Map<String, List<LinkEdgeDTO>> callEdgeMap, ApplicationEntranceTopologyQueryRequest request) {
        if (CollectionUtils.isEmpty(applicationEntrancesTopology.getEdges())) {
            return Lists.newArrayList();
        }
        List<ApplicationEntranceTopologyEdgeResponse> edgeResponseList = applicationEntrancesTopology.getEdges()
                .stream()
                //.filter(linkEdgeDTO -> EdgeTypeEnum.getEdgeTypeEnum(linkEdgeDTO.getMiddlewareName()) != EdgeTypeEnum
                // .UNKNOWN)
                .map(edge -> {
                    ApplicationEntranceTopologyEdgeResponse
                            applicationEntranceTopologyEdgeResponse
                            = new ApplicationEntranceTopologyEdgeResponse();
                    // 虚拟入口边
                    if ("VIRTUAL".equals(edge.getEagleType())) {
                        //edge.setEagleType(request.getType().getType());
                        //edge.setEagleTypeGroup(EdgeTypeGroupEnum.getEdgeTypeEnum(request.getType().getType()).getType());
                        edge.setMethod(request.getMethod());
                        edge.setService(request.getServiceName());
                        edge.setMiddlewareName(request.getType().getType());
                    }
                    applicationEntranceTopologyEdgeResponse.setSource(edge.getSourceId());
                    applicationEntranceTopologyEdgeResponse.setTarget(edge.getTargetId());
                    //EdgeTypeEnum edgeTypeEnum = EdgeTypeEnum.getEdgeTypeEnum(edge.getMiddlewareName());
                    //applicationEntranceTopologyEdgeResponse.setLabel(edgeTypeEnum.name());
                    applicationEntranceTopologyEdgeResponse.setLabel(edge.getMiddlewareName());
                    //applicationEntranceTopologyEdgeResponse.setType(edgeTypeEnum.name());
                    applicationEntranceTopologyEdgeResponse.setInfo(convertEdgeInfo(edge, nodeMap));
                    applicationEntranceTopologyEdgeResponse.setId(edge.getEagleId() != null ? edge.getEagleId()
                            : MD5Util.getMD5(edge.getService() + "|" + edge.getMethod()));
                    return applicationEntranceTopologyEdgeResponse;
                }).collect(Collectors.toList());
        Map<String, Map<String, Set<String>>> collect = edgeResponseList.stream()
                .collect(
                        Collectors.groupingBy(ApplicationEntranceTopologyEdgeResponse::getSource,
                                Collectors.groupingBy(ApplicationEntranceTopologyEdgeResponse::getTarget,
                                        Collectors.mapping(ApplicationEntranceTopologyEdgeResponse::getInfo, Collectors.toSet()))));
        edgeResponseList
                .forEach(edge -> {
                    Map<String, Set<String>> stringListMap = collect.get(edge.getSource());
                    Set<String> strings = stringListMap.get(edge.getTarget());
                    edge.setInfo(null);
                    edge.setInfos(strings);
                });
        return edgeResponseList.stream().collect(Collectors.collectingAndThen(
                Collectors.toCollection(() -> new TreeSet<>(
                        Comparator.comparing(this::fetchingGroupKey))), ArrayList::new));
    }

    private <T> T convertNodeExtendInfo(Object extendInfo, Class<T> clazz) {
        String json = JSON.toJSONString(extendInfo);
        return JSON.parseObject(json, clazz);
    }

    private String convertEdgeInfo(LinkEdgeDTO edge, Map<String, LinkNodeDTO> nodeMap) {
        //if (EdgeTypeGroupEnum.HTTP.name().equals(edge.getEagleTypeGroup())) {
        if ((MiddlewareType.TYPE_WEB_SERVER + "").equals(edge.getRpcType())) {
            return "请求方式：" + edge.getMethod() + "，请求路径：" + edge.getService();
        }
        //if (EdgeTypeGroupEnum.DUBBO.name().equals(edge.getEagleTypeGroup())) {
        if ((MiddlewareType.TYPE_RPC + "").equals(edge.getRpcType())) {
            return "类：" + edge.getService() + "，方法(参数）：" + edge.getMethod();
        }
        //if (EdgeTypeGroupEnum.DB.name().equals(edge.getEagleTypeGroup())) {
        if ((MiddlewareType.TYPE_DB + "").equals(edge.getRpcType())) {
            return "数据库：" + edge.getService() + ", 表名: " + edge.getMethod();
        }
        //if (EdgeTypeGroupEnum.MQ.name().equals(edge.getEagleTypeGroup())) {
        if ((MiddlewareType.TYPE_MQ + "").equals(edge.getRpcType())) {
            // 消费者
            if (nodeMap.get(edge.getSourceId()).getNodeTypeGroup().equals(NodeTypeGroupEnum.MQ.getType())) {
                return "Topic：" + edge.getService() + "，Group：" + edge.getMethod();
            } else {
                return "Topic：" + edge.getService();
            }
        }
        //if (EdgeTypeGroupEnum.OSS.name().equals(edge.getEagleTypeGroup())) {
        if ((MiddlewareType.TYPE_FS + "").equals(edge.getRpcType())) {
            return "URL：" + edge.getService();
        }
        //if (EdgeTypeGroupEnum.CACHE.name().equals(edge.getEagleTypeGroup())) {
        if ((MiddlewareType.TYPE_CACHE + "").equals(edge.getRpcType())) {
            String db;
            String op;
            if (edge.getService().contains(":")) {
                String[] split = edge.getService().split(":");
                db = split[0];
                op = split[1];
            } else {
                db = edge.getService();
                op = edge.getMethod();
            }
            List<String> msg = new ArrayList<>();
            if (StringUtils.isNotBlank(db)) {
                msg.add("DB：" + db);
            }
            if (StringUtils.isNotBlank(op)) {
                msg.add("操作方式：" + op);
            }
            return StringUtils.join(msg, "，");
        }
        //if (EdgeTypeGroupEnum.JOB.name().equals(edge.getEagleTypeGroup())) {
        if ((MiddlewareType.TYPE_JOB + "").equals(edge.getRpcType())) {
            return "类：" + edge.getService() + "，方法：" + edge.getMethod();
        }
        return "";
    }

    private boolean isRoot(LinkNodeDTO node) {
        return node.isRoot() && node.getNodeName().endsWith("-Virtual");
    }

    private String getNodeInfoLabel(LinkNodeDTO node) {
        String label = node.getNodeName();
        if (isRoot(node)) {
            label = "入口";
        } else if (node.getNodeTypeGroup().equalsIgnoreCase(NodeTypeGroupEnum.APP.getType())
                || node.getNodeTypeGroup().equalsIgnoreCase(NodeTypeGroupEnum.CACHE.getType())) {
            label = node.getNodeType() + ":" + node.getNodeName();
        }
        // 未知应用
        if (isUnknownNode(node)) {
            label = "未知应用";
        }
        // 第三方服务
        else if (isOuterService(node)) {
            label = "外部应用";
        }
        return label;
    }

    private boolean nodeIsApp(LinkNodeDTO node) {
        return node.getNodeType().equalsIgnoreCase(NodeTypeEnum.APP.name());
    }

    private boolean isUnknownNode(LinkNodeDTO node) {
        return node.getNodeName().equalsIgnoreCase("UNKNOWN");// AMDB定义在拓扑图里面的
    }

    private boolean isUnknownResponseNode(AbstractTopologyNodeResponse node) {
        return node.getNodeType().getType().equals(NodeTypeResponseEnum.UNKNOWN.getType());
    }

    private boolean isOuterService(LinkNodeDTO node) {
        return node.getNodeName().equalsIgnoreCase("OUTSERVICE");
    }

    private boolean isVirtualNode(LinkNodeDTO node) {
        return node.getNodeName().endsWith("-Virtual");
    }

    private String fetchingGroupKey(ApplicationEntranceTopologyEdgeResponse response) {
        return response.getSource() + "@" + response.getTarget();
    }
}
