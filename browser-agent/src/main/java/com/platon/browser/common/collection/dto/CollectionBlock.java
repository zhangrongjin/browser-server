package com.platon.browser.common.collection.dto;

import com.platon.browser.client.result.Receipt;
import com.platon.browser.client.result.ReceiptResult;
import com.platon.browser.elasticsearch.dto.Block;
import com.platon.browser.exception.BeanCreateOrUpdateException;
import com.platon.browser.exception.BusinessException;
import com.platon.browser.utils.HexTool;
import com.platon.browser.utils.NodeTool;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.web3j.protocol.core.methods.response.PlatonBlock;
import org.web3j.protocol.core.methods.response.Transaction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@Slf4j
@Accessors(chain = true)
public class CollectionBlock extends Block {
    private List<CollectionTransaction> transactions=new ArrayList<>();

    private CollectionBlock(){}
    public static CollectionBlock newInstance(){
        CollectionBlock block = new CollectionBlock();
        Date date = new Date();
        block.setCreTime(date)
            .setUpdTime(date)
            .setDQty(0)
            .setPQty(0)
            .setSQty(0)
            .setTranQty(0)
            .setTxQty(0)
            .setReward(BigDecimal.ZERO)
            .setGasLimit(BigDecimal.ZERO)
            .setGasUsed(BigDecimal.ZERO)
            .setTxFee(BigDecimal.ZERO)
            .setSize(0)
            .setTxGasLimit(BigDecimal.ZERO);
        return block;
    }

    public CollectionBlock updateWithRawBlockAndReceiptResult(PlatonBlock.Block block,ReceiptResult receiptResult) throws BeanCreateOrUpdateException {
        String nodeId = NodeTool.getPublicKey(block);
        nodeId = HexTool.prefix(nodeId);
        this.setNum(block.getNumber().longValue())
            .setHash(block.getHash())
            .setPHash(block.getParentHash())
            .setSize(block.getSize().intValue())
            .setTime(new Date(block.getTimestamp().longValue()))
            .setExtra(block.getExtraData())
            .setMiner(block.getMiner())
            .setNodeId(nodeId)
            .setGasLimit(new BigDecimal(block.getGasLimit()))
            .setGasUsed(new BigDecimal(block.getGasUsed()));

        if(block.getTransactions().isEmpty()) return this;

        if(receiptResult.getResult().isEmpty()) throw new BusinessException("区块["+this.getNum()+"]有["+block.getTransactions().size()+"]笔交易,但查询不到回执!");

        // 解析交易
        List<PlatonBlock.TransactionResult> transactionResults = block.getTransactions();
        if(receiptResult.getResult()!=null&&!receiptResult.getResult().isEmpty()){
            Map<String,Receipt> receiptMap=receiptResult.getMap();
            for (PlatonBlock.TransactionResult tr : transactionResults) {
                PlatonBlock.TransactionObject to = (PlatonBlock.TransactionObject) tr.get();
                Transaction rawTransaction = to.get();
                CollectionTransaction transaction = CollectionTransaction.newInstance()
                        .updateWithBlock(this)
                        .updateWithRawTransaction(rawTransaction)
                        .updateWithBlockAndReceipt(this,receiptMap.get(rawTransaction.getHash()));
                transactions.add(transaction);
            }
        }
        return this;
    }
}