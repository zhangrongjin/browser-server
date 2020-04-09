package com.platon.browser.complement.converter.slash;

import com.platon.browser.common.complement.cache.ReportMultiSignParamCache;
import com.platon.browser.common.queue.collection.event.CollectionEvent;
import com.platon.browser.complement.converter.BusinessParamConverter;
import com.platon.browser.complement.dao.mapper.SlashBusinessMapper;
import com.platon.browser.complement.dao.param.slash.Report;
import com.platon.browser.config.BlockChainConfig;
import com.platon.browser.elasticsearch.dto.NodeOpt;
import com.platon.browser.elasticsearch.dto.Transaction;
import com.platon.browser.param.ReportParam;
import com.platon.browser.service.misc.StakeMiscService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * @description: 举报验证人业务参数转换器
 * @author: chendongming@juzix.net
 * @create: 2019-11-04 17:58:27
 **/
@Slf4j
@Service
public class ReportConverter extends BusinessParamConverter<NodeOpt> {
	
    @Autowired
    private BlockChainConfig chainConfig;
    @Autowired
    private SlashBusinessMapper slashBusinessMapper;

    @Autowired
    private ReportMultiSignParamCache reportMultiSignParamCache;
    @Autowired
    private StakeMiscService stakeMiscService;

    @Override
    public NodeOpt convert(CollectionEvent event, Transaction tx) {
        // 举报信息
        ReportParam txParam = tx.getTxParam(ReportParam.class);
        if(null==txParam) return null;
        updateTxInfo(txParam,tx);
        // 失败的交易不分析业务数据
        if(Transaction.StatusEnum.FAILURE.getCode()==tx.getStatus()) return null;

        long startTime = System.currentTimeMillis();

        // 举报成功，先把节点设置为异常，后续处罚操作在共识周期切换时执行
        List<String> nodeIdList = new ArrayList<>();
        nodeIdList.add(txParam.getVerify());

        slashBusinessMapper.setException(txParam.getVerify(),txParam.getStakingBlockNum().longValue());

        // 更新解质押到账需要经过的结算周期数
        BigInteger  unStakeFreezeDuration = stakeMiscService.getUnStakeFreeDuration();
        // 理论上的退出区块号, 实际的退出块号还要跟状态为进行中的提案的投票截至区块进行对比，取最大者
        BigInteger unStakeEndBlock = stakeMiscService.getUnStakeEndBlock(txParam.getVerify(),event.getEpochMessage().getSettleEpochRound(),true);
        Report businessParam= Report.builder()
        		.slashData(txParam.getData())
                .nodeId(txParam.getVerify())
                .txHash(tx.getHash())
                .time(tx.getTime())
                .stakingBlockNum(txParam.getStakingBlockNum())
                .slashRate(chainConfig.getDuplicateSignSlashRate())
                .benefitAddr(tx.getFrom())
                .slash2ReportRate(chainConfig.getDuplicateSignRewardRate())
                .settingEpoch(event.getEpochMessage().getSettleEpochRound().intValue())
                .unStakeFreezeDuration(unStakeFreezeDuration.intValue())
                .unStakeEndBlock(unStakeEndBlock)
                .build();

        //更新节点提取质押需要经过的周期数
        slashBusinessMapper.updateUnStakeFreezeDuration(businessParam);

        // 把举报参数暂时缓存，待共识周期切换时处理
        reportMultiSignParamCache.addReport(businessParam);

        log.debug("处理耗时:{} ms",System.currentTimeMillis()-startTime);

        return null;
    }
}
