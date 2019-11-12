package com.platon.browser.common.complement.cache;

import com.platon.browser.dao.entity.NetworkStat;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;

@Component
@Data
public class NetworkStatCache {
    private NetworkStat networkStat=new NetworkStat();
    
    @Autowired
    private TpsCalcCache tpsCalcCache;

    /**
     * 基于区块维度更新网络统计信息
     * @param txQty
     * @param proposalQty
     * @param time
     */
    public void updateByBlock(int txQty, int proposalQty, Date time, String bHash) {
    	tpsCalcCache.update(txQty, time.getTime());
    	int tps = tpsCalcCache.getTps();
    	networkStat.setTxQty(txQty+networkStat.getTxQty());
    	networkStat.setProposalQty(proposalQty+networkStat.getProposalQty());
    	networkStat.setCurTps(tps);
    	networkStat.setCurBlockHash(bHash);
    	if(tps > networkStat.getMaxTps()) {
    		networkStat.setMaxTps(tps);
    	}
    }
    
    /**
     * 获得操作日志需要
     * @return
     */
    public long getAndIncrementNodeOptSeq() {
    	long seq = networkStat.getNodeOptSeq() == null? 1: networkStat.getNodeOptSeq() + 1;
    	networkStat.setNodeOptSeq(seq);
    	return seq;
    }

    /**
     * 基于任务更新网络统计信息
     * @param issueValue
     * @param turnValue
     * @param totalValue
     * @param stakingValue
     * @param addressQty
     * @param doingProposalQty
     */
	public void updateByTask(BigDecimal issueValue, BigDecimal turnValue, BigDecimal totalValue, BigDecimal stakingValue, int addressQty, int doingProposalQty) {
		networkStat.setIssueValue(issueValue);
		networkStat.setTurnValue(turnValue);
		networkStat.setStakingDelegationValue(totalValue);
		networkStat.setStakingValue(stakingValue);
		networkStat.setAddressQty(addressQty);
		networkStat.setDoingProposalQty(doingProposalQty);		
	}

    public void init(NetworkStat networkStat) {
	    this.networkStat=networkStat;
    }
}
