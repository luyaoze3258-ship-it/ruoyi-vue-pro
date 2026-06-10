package cn.iocoder.yudao.module.bpm.dal.mysql.task;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.bpm.dal.dataobject.task.BpmAiApprovalTaskDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BpmAiApprovalTaskMapper extends BaseMapperX<BpmAiApprovalTaskDO> {

    default BpmAiApprovalTaskDO selectByTaskId(String taskId) {
        return selectOne(BpmAiApprovalTaskDO::getTaskId, taskId);
    }

    default BpmAiApprovalTaskDO selectByGuanlanTaskId(String guanlanTaskId) {
        return selectOne(BpmAiApprovalTaskDO::getGuanlanTaskId, guanlanTaskId);
    }

    default BpmAiApprovalTaskDO selectByExternalId(String externalId) {
        return selectOne(BpmAiApprovalTaskDO::getExternalId, externalId);
    }

    default BpmAiApprovalTaskDO selectByTaskIdForUpdate(String taskId) {
        return selectOneForUpdate(BpmAiApprovalTaskDO::getTaskId, taskId);
    }

    default BpmAiApprovalTaskDO selectByGuanlanTaskIdForUpdate(String guanlanTaskId) {
        return selectOneForUpdate(BpmAiApprovalTaskDO::getGuanlanTaskId, guanlanTaskId);
    }

    default BpmAiApprovalTaskDO selectLatestByProcessInstanceId(String processInstanceId) {
        return selectLastOne(new LambdaQueryWrapperX<BpmAiApprovalTaskDO>()
                .eq(BpmAiApprovalTaskDO::getProcessInstanceId, processInstanceId)
                .orderByDesc(BpmAiApprovalTaskDO::getId));
    }

    default List<BpmAiApprovalTaskDO> selectPendingPollingTasks(int limit) {
        return selectList(new LambdaQueryWrapperX<BpmAiApprovalTaskDO>()
                .isNotNull(BpmAiApprovalTaskDO::getGuanlanTaskId)
                .isNull(BpmAiApprovalTaskDO::getVerdict)
                .orderByAsc(BpmAiApprovalTaskDO::getId)
                .last("LIMIT " + limit));
    }

}
