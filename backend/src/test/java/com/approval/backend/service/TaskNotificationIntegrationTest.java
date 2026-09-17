package com.approval.backend.service;

import com.approval.backend.dto.message.ApprovalEventMessage;
import com.approval.backend.entity.*;
import com.approval.backend.mapper.*;
import com.approval.backend.mq.ApprovalEventProducer;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 通知生产端集成测试
 * <p>
 * 验证的是"业务动作在正确的时机登记了正确的事件"，即本地消息表里落了对应的记录，
 * 落库为通知由 MQ 消费端异步完成，依赖 RocketMQ 真实投递，既慢又不稳定，因此不放进断言范围。
 * </p>
 * <p>
 * 业务事务一旦提交，mq_outbox 里就应有这条消息的快照（body 为事务内序列化好的 JSON），
 * 所以断言直接查表并反序列化还原，不经过任何异步环节。
 * </p>
 */
@SpringBootTest
@DisplayName("通知生产端")
class TaskNotificationIntegrationTest {

    /** 发起人：初始化的 admin 用户 */
    private static final Long INITIATOR_ID = 1L;

    /** 当前节点审批人：zhaoliu */
    private static final Long APPROVER_ID = 2L;

    /** 下一节点审批人：lisi。 */
    private static final Long NEXT_APPROVER_ID = 3L;

    /**
     * 替换真实生产者，避免扫表任务真的把消息投到 RocketMQ。
     * 消费端是异步落库，落库时机与 {@code tearDown} 的数据清理互相竞争，双方会因外键约束互相打断；
     * 而本用例要验证的只是"业务动作是否登记了本地消息"，止步于 mq_outbox 表即可。
     */
    @MockBean
    private ApprovalEventProducer approvalEventProducer;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ProcessDefinitionMapper processDefinitionMapper;
    @Autowired
    private ProcessNodeMapper processNodeMapper;
    @Autowired
    private ProcessInstanceMapper processInstanceMapper;
    @Autowired
    private TaskMapper taskMapper;
    @Autowired
    private ApprovalRecordMapper approvalRecordMapper;
    @Autowired
    private NotificationMapper notificationMapper;
    @Autowired
    private MqOutboxMapper mqOutboxMapper;

    /** 消息体是本模块自己序列化的简单 POJO，无需容器定制的 ObjectMapper */
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final List<Long> createdNodeIds = new ArrayList<>();

    private Long definitionId;
    private Long instanceId;
    private Long taskId;

    @BeforeEach
    void setUp() {
        ProcessDefinition definition = new ProcessDefinition();
        definition.setName("通知生产端测试-" + System.currentTimeMillis());
        definition.setVersion(1);
        definition.setStatus(1);
        definition.setFormSchema("{}");
        processDefinitionMapper.insert(definition);
        definitionId = definition.getId();

        Long firstNodeId = insertNode(1, "部门经理审批", APPROVER_ID);

        ProcessInstance instance = new ProcessInstance();
        instance.setDefinitionId(definitionId);
        instance.setDefinitionName("通知生产端测试");
        instance.setInitiatorId(INITIATOR_ID);
        instance.setFormData("{}");
        instance.setSubmitCount(1);
        instance.setStatus(1);
        processInstanceMapper.insert(instance);
        instanceId = instance.getId();

        Task task = new Task();
        task.setInstanceId(instanceId);
        task.setNodeId(firstNodeId);
        task.setNodeName("部门经理审批");
        task.setAssigneeId(APPROVER_ID);
        task.setSubmitRound(1);
        task.setStatus(0);
        taskMapper.insert(task);
        taskId = task.getId();
    }

    @AfterEach
    void tearDown() {
        if (instanceId != null) {
            // 清理本地消息表，残留的待发行会被扫表任务投递，消费端落库将撞 notification 的 RESTRICT 外键
            List<String> taskKeys = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                            .eq(Task::getInstanceId, instanceId))
                    .stream().map(task -> String.valueOf(task.getId())).toList();
            if (!taskKeys.isEmpty()) {
                mqOutboxMapper.delete(new LambdaQueryWrapper<MqOutbox>()
                        .in(MqOutbox::getMsgKey, taskKeys));
            }

            // 逆着外键依赖清理：notification 的 instance / task 外键都是 RESTRICT
            notificationMapper.delete(new LambdaQueryWrapper<Notification>()
                    .eq(Notification::getInstanceId, instanceId));
            taskMapper.delete(new LambdaQueryWrapper<Task>()
                    .eq(Task::getInstanceId, instanceId));
            approvalRecordMapper.delete(new LambdaQueryWrapper<ApprovalRecord>()
                    .eq(ApprovalRecord::getInstanceId, instanceId));
            processInstanceMapper.deleteById(instanceId);
        }
        createdNodeIds.forEach(processNodeMapper::deleteById);
        if (definitionId != null) {
            processDefinitionMapper.deleteById(definitionId);
        }
    }

    @Test
    @DisplayName("通过且存在下一节点：只登记一条待办通知给下一节点审批人")
    void approveWithNextNodeShouldPublishTodo() {
        insertNode(2, "总监审批", NEXT_APPROVER_ID);

        taskService.approveTask(taskId, "同意", APPROVER_ID);

        List<ApprovalEventMessage> messages = messagesOf();
        assertEquals(1, messages.size(), "有下一节点时只应发待办通知");

        ApprovalEventMessage todo = messages.get(0);
        assertEquals(ApprovalEventMessage.TYPE_TODO, todo.getType());
        assertEquals(instanceId, todo.getInstanceId());
        assertEquals(NEXT_APPROVER_ID, todo.getRecipientId(), "待办应发给下一节点的审批人，而非发起人或当前审批人");
        assertTrue(todo.getContent().contains("总监审批"), "内容应带上待办节点名");

        // taskId 必须指向新建的那个任务，否则消费端幂等键会落在已办任务上
        Task nextTask = taskMapper.selectOne(new LambdaQueryWrapper<Task>()
                .eq(Task::getInstanceId, instanceId)
                .eq(Task::getStatus, 0));
        assertNotNull(nextTask, "应生成下一节点任务");
        assertEquals(NEXT_APPROVER_ID, nextTask.getAssigneeId(), "下一节点任务应分配给下一节点审批人");
        assertEquals(nextTask.getId(), todo.getTaskId());
    }

    @Test
    @DisplayName("通过且无下一节点：只登记一条通过通知给发起人")
    void approveWithoutNextNodeShouldPublishApproved() {
        taskService.approveTask(taskId, "同意", APPROVER_ID);

        List<ApprovalEventMessage> messages = messagesOf();
        assertEquals(1, messages.size(), "流程结束时只应发通过通知");

        ApprovalEventMessage approved = messages.get(0);
        assertEquals(ApprovalEventMessage.TYPE_APPROVED, approved.getType());
        assertEquals(INITIATOR_ID, approved.getRecipientId(), "通过应发给流程发起人，而非审批人");
        assertEquals(taskId, approved.getTaskId(), "通过通知归属最后一个节点的任务");
        assertTrue(approved.getContent().contains("已审批通过"));
    }

    @Test
    @DisplayName("驳回：只登记一条驳回通知给发起人，内容含审批意见")
    void rejectShouldPublishReject() {
        taskService.rejectTask(taskId, "材料不全", APPROVER_ID);

        List<ApprovalEventMessage> messages = messagesOf();
        assertEquals(1, messages.size());

        ApprovalEventMessage reject = messages.get(0);
        assertEquals(ApprovalEventMessage.TYPE_REJECT, reject.getType());
        assertEquals(INITIATOR_ID, reject.getRecipientId(), "驳回应发给流程发起人，而非审批人");
        assertEquals(taskId, reject.getTaskId());
        assertEquals(instanceId, reject.getInstanceId());
        assertTrue(reject.getContent().contains("材料不全"));
    }

    private Long insertNode(int nodeOrder, String nodeName, Long assigneeId) {
        ProcessNode node = new ProcessNode();
        node.setDefinitionId(definitionId);
        node.setNodeOrder(nodeOrder);
        node.setNodeName(nodeName);
        node.setAssigneeType("USER");
        node.setAssigneeExpr(String.valueOf(assigneeId));
        processNodeMapper.insert(node);
        createdNodeIds.add(node.getId());
        return node.getId();
    }

    /**
     * 取出本流程实例登记到本地消息表的所有事件，
     * 按 msg_key（任务ID）过滤后把 body 反序列化还原成 {@link ApprovalEventMessage}。
     */
    private List<ApprovalEventMessage> messagesOf() {
        List<String> taskKeys = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                        .eq(Task::getInstanceId, instanceId))
                .stream().map(task -> String.valueOf(task.getId())).toList();
        if (taskKeys.isEmpty()) {
            return List.of();
        }
        return mqOutboxMapper.selectList(new LambdaQueryWrapper<MqOutbox>()
                        .in(MqOutbox::getMsgKey, taskKeys))
                .stream()
                .map(MqOutbox::getBody)
                .map(this::parseBody)
                .toList();
    }

    private ApprovalEventMessage parseBody(String body) {
        try {
            return objectMapper.readValue(body, ApprovalEventMessage.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("本地消息表 body 不是合法的审批事件 JSON: " + body, e);
        }
    }
}