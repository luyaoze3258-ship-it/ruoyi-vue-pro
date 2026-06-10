package cn.iocoder.yudao.module.bpm.dal.dataobject.task;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * BPM AI 审批回调日志 DO。
 */
@TableName(value = "bpm_ai_approval_callback_log", autoResultMap = true)
@KeySequence("bpm_ai_approval_callback_log_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BpmAiApprovalCallbackLogDO extends BaseDO {

    @TableId
    private Long id;

    private String callbackId;

    private String guanlanTaskId;

    private String externalId;

    private String verdict;

    private Boolean testMode;

    private Boolean verified;

    private Boolean processed;

    private String rawBody;

    private String errorMessage;

    private LocalDateTime receivedTime;

}
