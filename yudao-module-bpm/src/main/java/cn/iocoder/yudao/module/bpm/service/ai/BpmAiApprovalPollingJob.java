package cn.iocoder.yudao.module.bpm.service.ai;

import cn.hutool.core.util.BooleanUtil;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.bpm.framework.ai.config.BpmAiApprovalProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 定时查询观澜 AI 审批结果。
 */
@Slf4j
@Component
public class BpmAiApprovalPollingJob implements JobHandler {

    @Resource
    private BpmAiApprovalService aiApprovalService;
    @Resource
    private BpmAiApprovalProperties properties;

    @Override
    @TenantIgnore
    public String execute(String param) {
        return poll();
    }

    @Scheduled(initialDelayString = "${yudao.bpm.ai-approval.polling.initial-delay:15000}",
            fixedDelayString = "${yudao.bpm.ai-approval.polling.fixed-delay:60000}")
    public void pollGuanlanResults() {
        TenantUtils.executeIgnore(this::poll);
    }

    private String poll() {
        if (!BooleanUtil.isTrue(properties.getEnabled()) || !BooleanUtil.isTrue(properties.getPolling().getEnabled())) {
            return "AI 审批观澜结果定时查询未启用";
        }
        int batchSize = properties.getPolling().getBatchSize();
        int syncedCount = aiApprovalService.syncPendingTaskResultsFromGuanlan(batchSize);
        log.info("[execute][定时查询观澜 AI 审批结果，batchSize({}) syncedCount({})]", batchSize, syncedCount);
        return String.format("定时查询观澜 AI 审批结果，同步完成 %s 个", syncedCount);
    }

}
