package cn.iocoder.yudao.module.bpm.dal.dataobject.task;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * BPM AI 审批任务映射 DO。
 */
@TableName(value = "bpm_ai_approval_task", autoResultMap = true)
@KeySequence("bpm_ai_approval_task_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BpmAiApprovalTaskDO extends BaseDO {

    @TableId
    private Long id;

    private String processInstanceId;

    private String processDefinitionId;

    private String taskId;

    private String taskDefinitionKey;

    private String taskName;

    private Long assigneeUserId;

    private String externalId;

    private String guanlanTaskId;

    private Boolean enabled;

    private Boolean adoptEnabled;

    private Integer status;

    private String verdict;

    private String opinion;

    private String callbackId;

    private LocalDateTime callbackTime;

    private String businessFinalVerdict;

    private String businessFinalOpinion;

    private LocalDateTime businessResultSyncTime;

    private String submitError;

}
