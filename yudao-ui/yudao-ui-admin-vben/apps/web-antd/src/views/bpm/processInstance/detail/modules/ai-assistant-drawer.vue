<script lang="ts" setup>
import type { BpmAiApprovalApi } from '#/api/bpm/aiApproval';

import { ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';
import { formatDateTime } from '@vben/utils';

import {
  Alert,
  Button,
  Drawer,
  Empty,
  Input,
  message,
  Tag,
  Tooltip,
} from 'ant-design-vue';

import {
  chatWithAiApproval,
  getAiApprovalDetail,
} from '#/api/bpm/aiApproval';
import { syncGuanlanAiApprovalTask } from '#/api/bpm/task';

defineOptions({ name: 'BpmAiAssistantDrawer' });

const props = defineProps<{
  open: boolean;
  processInstanceId: string;
}>();

const emit = defineEmits<{
  'update:open': [open: boolean];
  refreshed: [];
}>();

const detail = ref<BpmAiApprovalApi.Detail>();
const loading = ref(false);
const syncing = ref(false);
const chatting = ref(false);
const question = ref('');
const messages = ref<{ content: string; role: 'ai' | 'user' }[]>([]);

async function loadDetail() {
  if (!props.processInstanceId) {
    return;
  }
  loading.value = true;
  try {
    detail.value = await getAiApprovalDetail(props.processInstanceId);
    if (detail.value?.opinion && messages.value.length === 0) {
      messages.value = [{ role: 'ai', content: detail.value.opinion }];
    }
  } finally {
    loading.value = false;
  }
}

async function handleSync() {
  if (!detail.value?.taskId) {
    message.warning('当前 AI 审批任务还未创建');
    return;
  }
  syncing.value = true;
  try {
    const synced = await syncGuanlanAiApprovalTask(detail.value.taskId);
    await loadDetail();
    emit('refreshed');
    message.success(synced ? 'AI 结果已同步' : '暂无新的 AI 结果');
  } finally {
    syncing.value = false;
  }
}

async function handleAsk() {
  const text = question.value.trim();
  if (!text) {
    return;
  }
  messages.value.push({ role: 'user', content: text });
  question.value = '';
  chatting.value = true;
  try {
    const answer = await chatWithAiApproval(props.processInstanceId, text);
    messages.value.push({ role: 'ai', content: answer.answer });
  } finally {
    chatting.value = false;
  }
}

function handleClose() {
  emit('update:open', false);
}

watch(
  () => props.open,
  (open) => {
    if (open) {
      loadDetail();
    }
  },
);
</script>

<template>
  <Drawer
    :open="open"
    title="AI 审批助手"
    width="520"
    @close="handleClose"
  >
    <div class="flex h-full flex-col gap-4">
      <div
        class="flex items-center gap-3 rounded-md border border-solid border-blue-100 bg-blue-50 p-3 dark:border-blue-900 dark:bg-blue-950"
      >
        <div
          class="flex size-10 shrink-0 items-center justify-center rounded-full bg-blue-600 text-white"
        >
          <IconifyIcon icon="lucide:bot" class="size-6" />
        </div>
        <div class="min-w-0 flex-1">
          <div class="truncate font-medium">
            {{ detail?.agentName || '观澜智能审批助手' }}
          </div>
          <div class="mt-1 text-sm text-gray-500">
            {{ detail?.conclusion || 'AI结论：等待结果' }}
          </div>
        </div>
        <Tag v-if="detail?.verdict" color="blue">{{ detail.verdict }}</Tag>
      </div>

      <div class="flex flex-wrap items-center gap-2">
        <Button size="small" :loading="loading" @click="loadDetail">
          <template #icon>
            <IconifyIcon icon="lucide:refresh-cw" />
          </template>
          刷新详情
        </Button>
        <Tooltip title="定时查询会自动运行；该按钮用于人工兜底刷新观澜结果">
          <Button
            size="small"
            type="primary"
            ghost
            :loading="syncing"
            @click="handleSync"
          >
            <template #icon>
              <IconifyIcon icon="lucide:rotate-cw" />
            </template>
            同步AI结果
          </Button>
        </Tooltip>
      </div>

      <Alert
        v-if="detail?.callbackTime"
        type="info"
        show-icon
        :message="`最近同步：${formatDateTime(detail.callbackTime)}`"
      />

      <div class="min-h-0 flex-1 overflow-y-auto rounded-md bg-gray-50 p-3 dark:bg-gray-900">
        <Empty v-if="!detail && !loading" description="当前单据暂无 AI 审批记录" />
        <div v-else class="space-y-3">
          <div
            v-for="(item, index) in messages"
            :key="index"
            class="flex"
            :class="item.role === 'user' ? 'justify-end' : 'justify-start'"
          >
            <div
              class="max-w-[88%] whitespace-pre-wrap rounded-md px-3 py-2 text-sm leading-6"
              :class="
                item.role === 'user'
                  ? 'bg-blue-600 text-white'
                  : 'bg-white text-gray-700 shadow-sm dark:bg-gray-800 dark:text-gray-200'
              "
            >
              {{ item.content }}
            </div>
          </div>
        </div>
      </div>

      <div class="flex gap-2">
        <Input.TextArea
          v-model:value="question"
          :auto-size="{ minRows: 2, maxRows: 4 }"
          placeholder="询问当前单据的审核依据"
          @press-enter.exact.prevent="handleAsk"
        />
        <Button type="primary" :loading="chatting" @click="handleAsk">
          发送
        </Button>
      </div>
    </div>
  </Drawer>
</template>
