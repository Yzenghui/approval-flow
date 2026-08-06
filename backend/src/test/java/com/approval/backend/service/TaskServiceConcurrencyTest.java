package com.approval.backend.service;

import com.approval.backend.common.BusinessException;
import com.approval.backend.entity.*;
import com.approval.backend.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TaskServiceConcurrencyTest {

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

    private Long testDefinitionId;
    private Long testNodeId;
    private Long testInstanceId;
    private Long testTaskId;

    @BeforeEach
    void setUp() {
        ProcessDefinition definition = new ProcessDefinition();
        definition.setName("并发测试流程-" + System.currentTimeMillis());
        definition.setVersion(1);
        definition.setStatus(1);
        definition.setFormSchema("{}");
        processDefinitionMapper.insert(definition);
        testDefinitionId = definition.getId();

        ProcessNode node = new ProcessNode();
        node.setDefinitionId(testDefinitionId);
        node.setNodeName("审批节点");
        node.setNodeOrder(1);
        node.setAssigneeType("USER");
        node.setAssigneeExpr("1");
        processNodeMapper.insert(node);
        testNodeId = node.getId();

        ProcessInstance instance = new ProcessInstance();
        instance.setDefinitionId(testDefinitionId);
        instance.setDefinitionName("并发测试流程");
        instance.setInitiatorId(1L);
        instance.setFormData("{}");
        instance.setSubmitCount(1);
        instance.setStatus(1);
        processInstanceMapper.insert(instance);
        testInstanceId = instance.getId();

        Task task = new Task();
        task.setInstanceId(testInstanceId);
        task.setNodeId(testNodeId);
        task.setNodeName("审批节点");
        task.setAssigneeId(1L);
        task.setSubmitRound(1);
        task.setStatus(0);
        taskMapper.insert(task);
        testTaskId = task.getId();
    }

    @AfterEach
    void tearDown() {
        if (testTaskId != null) {
            taskMapper.deleteById(testTaskId);
        }
        if (testInstanceId != null) {
            approvalRecordMapper.delete(
                new LambdaQueryWrapper<ApprovalRecord>()
                    .eq(ApprovalRecord::getInstanceId, testInstanceId));
            processInstanceMapper.deleteById(testInstanceId);
        }
        if (testNodeId != null) {
            processNodeMapper.deleteById(testNodeId);
        }
        if (testDefinitionId != null) {
            processDefinitionMapper.deleteById(testDefinitionId);
        }
    }

    @Test
    void concurrentApproveTask_shouldOnlySucceedOnce() throws InterruptedException {
        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    taskService.approveTask(testTaskId, "同意", 1L);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getMessage().contains("请勿重复提交")) {
                        failCount.incrementAndGet();
                    } else {
                        exceptions.add(e);
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertTrue(exceptions.isEmpty(), "不应有意外异常: " + exceptions);
        assertEquals(1, successCount.get(), "只有一个线程应成功通过审批");
        assertEquals(threadCount - 1, failCount.get(), "其余线程应被锁拦截");

        Task updatedTask = taskMapper.selectById(testTaskId);
        assertEquals(1, updatedTask.getStatus(), "任务状态应为已处理");

        ProcessInstance updatedInstance = processInstanceMapper.selectById(testInstanceId);
        assertEquals(2, updatedInstance.getStatus(), "流程实例状态应为通过（无下一节点）");

        Long recordCount = approvalRecordMapper.selectCount(
                new LambdaQueryWrapper<ApprovalRecord>()
                        .eq(ApprovalRecord::getInstanceId, testInstanceId)
                        .eq(ApprovalRecord::getTaskId, testTaskId)
                        .eq(ApprovalRecord::getAction, 1)
        );
        assertEquals(1, recordCount, "只有一条审批记录");
    }

    @Test
    void concurrentRejectTask_shouldOnlySucceedOnce() throws InterruptedException {
        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    taskService.rejectTask(testTaskId, "驳回", 1L);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getMessage().contains("请勿重复提交")) {
                        failCount.incrementAndGet();
                    } else {
                        exceptions.add(e);
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertTrue(exceptions.isEmpty(), "不应有意外异常: " + exceptions);
        assertEquals(1, successCount.get(), "只有一个线程应成功驳回审批");
        assertEquals(threadCount - 1, failCount.get(), "其余线程应被锁拦截");

        Task updatedTask = taskMapper.selectById(testTaskId);
        assertEquals(1, updatedTask.getStatus(), "任务状态应为已处理");

        ProcessInstance updatedInstance = processInstanceMapper.selectById(testInstanceId);
        assertEquals(3, updatedInstance.getStatus(), "流程实例状态应为驳回");

        Long recordCount = approvalRecordMapper.selectCount(
                new LambdaQueryWrapper<ApprovalRecord>()
                        .eq(ApprovalRecord::getInstanceId, testInstanceId)
                        .eq(ApprovalRecord::getTaskId, testTaskId)
                        .eq(ApprovalRecord::getAction, 2)
        );
        assertEquals(1, recordCount, "只有一条驳回记录");
    }
}