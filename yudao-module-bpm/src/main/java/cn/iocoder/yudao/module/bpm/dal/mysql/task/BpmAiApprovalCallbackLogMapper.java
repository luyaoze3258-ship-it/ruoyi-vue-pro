package cn.iocoder.yudao.module.bpm.dal.mysql.task;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.bpm.dal.dataobject.task.BpmAiApprovalCallbackLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BpmAiApprovalCallbackLogMapper extends BaseMapperX<BpmAiApprovalCallbackLogDO> {

    default BpmAiApprovalCallbackLogDO selectByCallbackId(String callbackId) {
        return selectOne(BpmAiApprovalCallbackLogDO::getCallbackId, callbackId);
    }

}
