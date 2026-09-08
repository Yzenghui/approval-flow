package com.approval.backend.service.cache;

import com.approval.backend.entity.ProcessDefinition;
import com.approval.backend.entity.ProcessNode;
import com.approval.backend.mapper.ProcessDefinitionMapper;
import com.approval.backend.mapper.ProcessNodeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 失效策略：在流程定义 update/enable/disable 后主动删除对应缓存键；
 * create 无需失效（新 ID 无历史缓存）。
 * TTL 兜底：30 分钟，防止极端情况下缓存与 DB 长期不一致。
 */
@Component
public class ProcessCacheHelper {

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
     * 失效指定流程定义相关的所有缓存（定义本身 + 节点列表）
     * 在 update/enable/disable 后调用。
     */
    public void evictDefinition(Long id) {
        redisTemplate.delete(DEF_KEY_PREFIX + id);
        redisTemplate.delete(NODES_KEY_PREFIX + id);
    }

    private void put(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value),
                    TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // Redis 写入失败不影响业务流程，下次读会再次回源
        }
    }
}
