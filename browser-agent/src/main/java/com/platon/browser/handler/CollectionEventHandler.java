package com.platon.browser.handler;

import com.lmax.disruptor.EventHandler;
import com.platon.browser.bean.CollectionEvent;
import com.platon.browser.bean.TxAnalyseResult;
import com.platon.browser.cache.NetworkStatCache;
import com.platon.browser.dao.entity.NOptBak;
import com.platon.browser.dao.entity.NOptBakExample;
import com.platon.browser.dao.entity.TxBak;
import com.platon.browser.dao.entity.TxBakExample;
import com.platon.browser.dao.mapper.CustomNOptBakMapper;
import com.platon.browser.dao.mapper.CustomTxBakMapper;
import com.platon.browser.dao.mapper.NOptBakMapper;
import com.platon.browser.dao.mapper.TxBakMapper;
import com.platon.browser.elasticsearch.dto.NodeOpt;
import com.platon.browser.elasticsearch.dto.Transaction;
import com.platon.browser.publisher.ComplementEventPublisher;
import com.platon.browser.service.block.BlockService;
import com.platon.browser.service.statistic.StatisticService;
import com.platon.browser.service.transaction.TransactionService;
import com.platon.browser.utils.BakDataDeleteUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 区块事件处理器
 */
@Slf4j
@Component
public class CollectionEventHandler implements EventHandler<CollectionEvent> {

    @Resource
    private TransactionService transactionService;
    @Resource
    private BlockService blockService;
    @Resource
    private StatisticService statisticService;
    @Resource
    private ComplementEventPublisher complementEventPublisher;
    @Resource
    private NetworkStatCache networkStatCache;
    @Resource
    private NOptBakMapper nOptBakMapper;
    @Resource
    private CustomNOptBakMapper customNOptBakMapper;
    @Resource
    private TxBakMapper txBakMapper;
    @Resource
    private CustomTxBakMapper customTxBakMapper;

    // 交易序号id
    private long transactionId = 0;

    private long txDeleteBatchCount = 0;
    private long optDeleteBatchCount = 0;

    @Transactional
    @Retryable(value = Exception.class, maxAttempts = Integer.MAX_VALUE)
    public void onEvent(CollectionEvent event, long sequence, boolean endOfBatch) throws Exception {
        long startTime = System.currentTimeMillis();

        log.debug("CollectionEvent处理:{}(event(block({}),transactions({})),sequence({}),endOfBatch({}))",
            Thread.currentThread().getStackTrace()[1].getMethodName(), event.getBlock().getNum(),
            event.getTransactions().size(), sequence, endOfBatch);

        // 使用已入库的交易数量初始化交易ID初始值
        if (this.transactionId == 0)
            this.transactionId = this.networkStatCache.getNetworkStat().getTxQty();

        try {
            List<Transaction> transactions = event.getTransactions();
            // 根据区块号解析出业务参数
            List<NodeOpt> nodeOpts1 = blockService.analyze(event);
            // 根据交易解析出业务参数
            TxAnalyseResult txAnalyseResult = this.transactionService.analyze(event);
            // 统计业务参数
            this.statisticService.analyze(event);

            if (!txAnalyseResult.getNodeOptList().isEmpty())
                nodeOpts1.addAll(txAnalyseResult.getNodeOptList());

            this.complementEventPublisher.publish(event.getBlock(), transactions, nodeOpts1,
                txAnalyseResult.getDelegationRewardList());

            this.txDeleteBatchCount++;
            this.optDeleteBatchCount++;

            if (this.txDeleteBatchCount >= 10) {
                // 删除小于最高ID的交易备份
                TxBakExample txBakExample = new TxBakExample();
                txBakExample.createCriteria().andIdLessThanOrEqualTo(BakDataDeleteUtil.getTxBakMaxId());
                int txCount = this.txBakMapper.deleteByExample(txBakExample);
                log.debug("清除交易备份记录({})条", txCount);
                this.txDeleteBatchCount = 0;
            }
            // 交易入库mysql
            if (!transactions.isEmpty()) {
                List<TxBak> baks = new ArrayList<>();
                transactions.forEach(tx -> {
                    TxBak bak = new TxBak();
                    BeanUtils.copyProperties(tx, bak);
                    baks.add(bak);
                });
                this.customTxBakMapper.batchInsertOrUpdateSelective(baks, TxBak.Column.values());
            }

            if (this.optDeleteBatchCount >= 10) {
                // 删除小于最高ID的操作记录备份
                NOptBakExample nOptBakExample = new NOptBakExample();
                nOptBakExample.createCriteria().andIdLessThanOrEqualTo(BakDataDeleteUtil.getNOptBakMaxId());
                int optCount = this.nOptBakMapper.deleteByExample(nOptBakExample);
                log.debug("清除操作备份记录({})条", optCount);
                this.optDeleteBatchCount = 0;
            }
            // 操作日志入库mysql
            if (!nodeOpts1.isEmpty()) {
                List<NOptBak> baks = new ArrayList<>();
                nodeOpts1.forEach(no -> {
                    NOptBak bak = new NOptBak();
                    BeanUtils.copyProperties(no, bak);
                    baks.add(bak);
                });
                this.customNOptBakMapper.batchInsertOrUpdateSelective(baks, NOptBak.Column.values());
            }
            // 释放对象引用
            event.releaseRef();
        } catch (Exception e) {
            log.error("", e);
            throw e;
        }

        log.debug("处理耗时:{} ms", System.currentTimeMillis() - startTime);
    }
}
