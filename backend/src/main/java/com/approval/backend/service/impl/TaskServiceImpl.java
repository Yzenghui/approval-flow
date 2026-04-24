package com.approval.backend.service.impl;

import com.approval.backend.common.PageResult;
import com.approval.backend.dto.response.PendingTaskResponse;
import com.approval.backend.entity.ProcessInstance;
import com.approval.backend.entity.Task;
import com.approval.backend.entity.User;
import com.approval.backend.mapper.ApprovalRecordMapper;
import com.approval.backend.mapper.ProcessInstanceMapper;
import com.approval.backend.mapper.TaskMapper;
import com.approval.backend.mapper.UserMapper;
import com.approval.backend.service.TaskService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务服务实现类
 */
@Service
public class TaskServiceImpl implements TaskService {
    private final UserMapper userMapper;
    private final ProcessInstanceMapper processInstanceMapper;
    private final TaskMapper taskMapper;
    private final ApprovalRecordMapper approvalRecordMapper;

    public TaskServiceImpl(UserMapper userMapper, ProcessInstanceMapper processInstanceMapper, TaskMapper taskMapper, ApprovalRecordMapper approvalRecordMapper) {
        this.userMapper = userMapper;
        this.processInstanceMapper = processInstanceMapper;
        this.taskMapper = taskMapper;
        this.approvalRecordMapper = approvalRecordMapper;
    }

    /**
     * 获取待办任务
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param userId   用户ID
     * @return 待办任务
     */
    @Override
    public PageResult<PendingTaskResponse> getPendingTasks(Integer pageNum, Integer pageSize, Long userId) {
        Page<Task> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Task> queryWrapper = new LambdaQueryWrapper<Task>()
                .eq(Task::getAssigneeId, userId)
                .eq(Task::getStatus, 0)
                .orderByDesc(Task::getCreateTime);
        Page<Task> taskPage = taskMapper.selectPage(page, queryWrapper);

        List<Long> taskIds = taskPage.getRecords().stream()
                .map(Task::getInstanceId)
                .toList();
        Map<Long, ProcessInstance> processInstanceMap = new HashMap<>();
        if (!taskIds.isEmpty()) {
            List<ProcessInstance> processInstances = processInstanceMapper.selectBatchIds(taskIds);
            processInstances.forEach(processInstance -> processInstanceMap.put(processInstance.getId(), processInstance));
        }

        List<Long> initiatorIds = processInstanceMap.values().stream()
                .map(ProcessInstance::getInitiatorId)
                .distinct()
                .toList();
        Map<Long, String> initiatorNameMap = new HashMap<>();
        if (!initiatorIds.isEmpty()) {
            List<User> initiators = userMapper.selectBatchIds(initiatorIds);
            initiators.forEach(initiator -> initiatorNameMap.put(initiator.getId(), initiator.getName()));
        }

        PageResult<PendingTaskResponse> pageResult = new PageResult<>();
        pageResult.setTotal(taskPage.getTotal());
        pageResult.setRecords(taskPage.getRecords().stream()
                .map(task -> {
                    PendingTaskResponse pendingTaskResponse = new PendingTaskResponse();
                    pendingTaskResponse.setTaskId(task.getId());
                    pendingTaskResponse.setInstanceId(task.getInstanceId());

                    ProcessInstance instance = processInstanceMap.get(task.getInstanceId());
                    pendingTaskResponse.setDefinitionName(instance.getDefinitionName());
                    pendingTaskResponse.setInitiatorName(initiatorNameMap.get(instance.getInitiatorId()));
                    pendingTaskResponse.setFormData(instance.getFormData());

                    pendingTaskResponse.setNodeName(task.getNodeName());
                    pendingTaskResponse.setCreateTime(task.getCreateTime());

                    return pendingTaskResponse;
                })
                .toList()
        );

        return pageResult;

    }
}
