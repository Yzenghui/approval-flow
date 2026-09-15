package com.approval.backend.service.cache;

import com.approval.backend.common.TransactionSyncHelper;
import com.approval.backend.entity.ProcessDefinition;
import com.approval.backend.entity.ProcessNode;
import com.approval.backend.mapper.ProcessDefinitionMapper;
import com.approval.backend.mapper.ProcessNodeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 流程定义与节点的缓存助手（cache-aside 模式）
 *
 * 缓存键约定：
 * process:def:{id} -> ProcessDefinition JSON
 * process:nodes:by-def:{defId} -> List<ProcessNode> JSON（按 node_order 升序）
 *
 * 失效策略：
 * create 无需失效（新 ID 无历史缓存）；
 * update 走 evictDefinitionAfterCommit，延迟到事务提交后再删，避免「缓存已删、事务未提交、
 * 并发请求回源写回旧值」的窗口；
 * enable/disable 无事务，直接调用 evictDefinition 即可；
 * TTL 兜底：30 分钟，防止极端情况下缓存与 DB 长期不一致。
 */
@Component
public class ProcessCacheHelper {

    private static final Logger log = LoggerFactory.getLogger(ProcessCacheHelper.class);

    private static final String DEF_KEY_PREFIX = "process:def:";
    private static final String NODES_KEY_PREFIX = "process:nodes:by-def:";
    private static final long TTL_SECONDS = 1800; // 30分钟

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ProcessDefinitionMapper processDefinitionMapper;
    private final ProcessNodeMapper processNodeMapper;

    public ProcessCacheHelper(StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
            ProcessDefinitionMapper processDefinitionMapper, ProcessNodeMapper processNodeMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.processDefinitionMapper = processDefinitionMapper;
        this.processNodeMapper = processNodeMapper;
    }

    /**
     * 按 ID 获取流程定义（cache-aside）
     * 缓存未命中时回源 DB 并回写；DB 也未命中返回 null（不缓存空值）。
     */
    public ProcessDefinition getProcessDefinition(Long id) {
        String key = DEF_KEY_PREFIX + id;
        String json = redisTemplate.opsForValue().get(key);
        if (json != null) {
            try {
                return objectMapper.readValue(json, ProcessDefinition.class);
            } catch (Exception e) {
                // 反序列化失败视为缓存脏数据，回源 DB
            }
        }
        ProcessDefinition processDefinition = processDefinitionMapper.selectById(id);
        if (processDefinition != null) {
            put(key, processDefinition);
        }
        return processDefinition;
    }

    /**
     * 按 definitionId 获取节点列表（cache-aside，按 node_order 升序）
     */
    public List<ProcessNode> getNodesByDefinitionId(Long definitionId) {
        String key = NODES_KEY_PREFIX + definitionId;
        String json = redisTemplate.opsForValue().get(key);
        if (json != null) {
            try {
                return objectMapper.readValue(json, new TypeReference<List<ProcessNode>>() {
                });
            } catch (Exception e) {
                // 反序列化失败视为缓存脏数据，回源 DB
            }
        }
        List<ProcessNode> nodes = processNodeMapper.selectList(new LambdaQueryWrapper<ProcessNode>()
                .eq(ProcessNode::getDefinitionId, definitionId)
                .orderByAsc(ProcessNode::getNodeOrder));
        if (!nodes.isEmpty()) {
            put(key, nodes);
        }
        return nodes;
    }

    /**
     * 立即失效指定流程定义的缓存（定义本体 + 节点列表）
     *
     * 只负责「删」，不负责「何时删」。
     *
     * 异常不外抛：到达本方法时业务数据已落库，此时 Redis 删除失败若向上抛，
     * 调用方会当成「整个操作失败」，而实际是「数据已改成功、只是缓存没删」，
     * 语义正好相反，会被误导反复重试，故吞掉并记 warn，最坏情况交给 TTL 兜底自愈。
     */
    public void evictDefinition(Long id) {
        try {
            redisTemplate.delete(DEF_KEY_PREFIX + id);
            redisTemplate.delete(NODES_KEY_PREFIX + id);
        } catch (Exception e) {
            log.warn("流程定义缓存失效失败，id={}，将由 TTL 兜底", id, e);
        }
    }

    /**
     * 事务提交后失效缓存，回滚时不失效
     *
     * 事务内直接删除会留下窗口：缓存已删、事务尚未提交，并发请求回源 DB 读到旧值并写回，
     * 缓存从此长期是旧值直到 TTL 过期，延迟到提交后删除可消除该窗口。
     */
    public void evictDefinitionAfterCommit(Long id) {
        TransactionSyncHelper.afterCommit(() -> evictDefinition(id));
    }

    private void put(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value),
                    TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("流程定义缓存写入失败，key={}，下次读将回源 DB", key, e);
        }
    }
}
