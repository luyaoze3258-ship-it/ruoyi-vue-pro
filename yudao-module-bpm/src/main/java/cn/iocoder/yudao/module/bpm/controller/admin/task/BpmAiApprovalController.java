package cn.iocoder.yudao.module.bpm.controller.admin.task;

import cn.hutool.core.util.BooleanUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.servlet.ServletUtils;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.bpm.framework.ai.config.BpmAiApprovalProperties;
import cn.iocoder.yudao.module.bpm.service.ai.BpmAiApprovalCallbackVerifier;
import cn.iocoder.yudao.module.bpm.service.ai.BpmAiApprovalService;
import cn.iocoder.yudao.module.bpm.service.ai.dto.BpmAiApprovalCallbackReqDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - BPM AI 审批")
@RestController
@RequestMapping("/bpm/ai-approval")
public class BpmAiApprovalController {

    @Resource
    private BpmAiApprovalService aiApprovalService;
    @Resource
    private BpmAiApprovalCallbackVerifier callbackVerifier;
    @Resource
    private BpmAiApprovalProperties properties;

    @PostMapping("/guanlan/callback")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "观澜审批结果回调")
    public CommonResult<Boolean> handleGuanlanCallback(HttpServletRequest request) {
        byte[] rawBody = ServletUtils.getBodyBytes(request);
        String body = rawBody != null ? new String(rawBody, StandardCharsets.UTF_8) : "";
        String signature = request.getHeader(properties.getGuanlan().getCallback().getSignatureHeader());
        boolean verified = callbackVerifier.verify(rawBody, properties.getGuanlan().getCallback().getHmacSecret(), signature);
        if (!verified) {
            return CommonResult.error(1_009_015_001, "AI 审批回调验签失败");
        }
        BpmAiApprovalCallbackReqDTO reqDTO = JsonUtils.parseObject(body, BpmAiApprovalCallbackReqDTO.class);
        String callbackId = request.getHeader(properties.getGuanlan().getCallback().getCallbackIdHeader());
        boolean testMode = BooleanUtil.toBoolean(request.getHeader(properties.getGuanlan().getCallback().getTestModeHeader()));
        return success(aiApprovalService.handleCallback(reqDTO, callbackId, body, testMode, true));
    }

    @PostMapping("/guanlan/sync")
    @Operation(summary = "主动查询观澜审批结果")
    @Parameter(name = "taskId", description = "BPM 任务编号", required = true)
    @PreAuthorize("@ss.hasPermission('bpm:task:update')")
    public CommonResult<Boolean> syncGuanlanTaskResult(@RequestParam("taskId") String taskId) {
        return success(aiApprovalService.syncTaskResultFromGuanlan(taskId));
    }

}
