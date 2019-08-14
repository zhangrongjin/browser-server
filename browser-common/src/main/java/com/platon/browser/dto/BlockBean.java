package com.platon.browser.dto;

import com.platon.browser.dao.entity.Block;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import org.web3j.protocol.core.methods.response.PlatonBlock;

import java.math.BigDecimal;
import java.util.*;

/**
 * @Auther: Chendongming
 * @Date: 2019/8/10 14:20
 * @Description:
 */
@Data
public class BlockBean extends Block {

    /**
     * 使用原生交易信息初始化交易信息
     * @param initData
     */
    public BlockBean(PlatonBlock.Block initData){
        BeanUtils.copyProperties(initData,this);
        // 属性类型转换
        this.setNumber(initData.getNumber().longValue());
        this.setTimestamp(new Date(initData.getTimestamp().longValue()));
        this.setSize(initData.getSize().intValue());
        this.setGasLimit(initData.getGasLimit().toString());
        this.setGasUsed(initData.getGasUsed().toString());
        Date date = new Date();
        this.setCreateTime(date);
        this.setUpdateTime(date);
        // 交易总数
        this.setStatTxQty(initData.getTransactions().size());
        this.setGasLimit(initData.getGasLimit().toString());
        // TODO:区块奖励
        this.setBlockReward("0");
        // TODO:节点名称
        this.setNodeName("DD");
        // TODO:节点id
        this.setNodeId("0xflsjgsflsdf");

        try{
            // 抽取交易信息
            initData.getTransactions().forEach(transactionResult -> {
                TransactionBean ti = new TransactionBean(transactionResult);
                ti.setTimestamp(this.getTimestamp());
                ti.setCreateTime(date);
                ti.setUpdateTime(date);
                transactionList.add(ti);
            });
        }catch (Exception ex){
            ex.printStackTrace();
            throw ex;
        }

        class Stat {
            int transferQty=0,stakingQty=0,proposalQty=0,delegateQty=0,txGasLimit=0;
            BigDecimal txFee = BigDecimal.ZERO;
        }
        Stat stat = new Stat();
        this.setStatDelegateQty(stat.delegateQty);
        this.setStatProposalQty(stat.proposalQty);
        this.setStatStakingQty(stat.stakingQty);
        this.setStatTransferQty(stat.transferQty);
        this.setStatTxGasLimit(String.valueOf(stat.txGasLimit));
        this.setStatTxFee(stat.txFee.toString());

        // 确保区块内的交易顺序
        Collections.sort(transactionList,(c1,c2)->{
            if(c1.getTransactionIndex()>c2.getTransactionIndex())return 1;
            if(c1.getTransactionIndex()<c2.getTransactionIndex())return -1;
            return 0;
        });
    }

    private List<TransactionBean> transactionList = new ArrayList<>();


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockBean blockInfo = (BlockBean) o;
        return Objects.equals(getNumber(), blockInfo.getNumber());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNumber());
    }
}