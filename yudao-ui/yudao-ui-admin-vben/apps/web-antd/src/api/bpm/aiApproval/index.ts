import { requestClient } from '#/api/request';

export namespace BpmAiApprovalApi {
  export interface Detail {
    adoptEnabled?: boolean;
    agentName?: string;
    callbackTime?: string;
    conclusion?: string;
    guanlanTaskId?: string;
    opinion?: string;
    processInstanceId: string;
    status?: number;
    taskDefinitionKey?: string;
    taskId?: string;
    taskName?: string;
    verdict?: string;
  }

  export interface ChatResp {
    answer: string;
    source: string;
  }
}

export async function getAiApprovalDetail(processInstanceId: string) {
  return requestClient.get<BpmAiApprovalApi.Detail>('/bpm/ai-approval/detail', {
    params: { processInstanceId },
  });
}

export async function chatWithAiApproval(
  processInstanceId: string,
  question: string,
) {
  return requestClient.post<BpmAiApprovalApi.ChatResp>('/bpm/ai-approval/chat', {
    processInstanceId,
    question,
  });
}
