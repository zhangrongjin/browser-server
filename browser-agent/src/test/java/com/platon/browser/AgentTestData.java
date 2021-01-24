package com.platon.browser;

import com.alaya.contracts.ppos.dto.resp.Node;
import com.alaya.protocol.core.methods.response.PlatonBlock;
import com.alibaba.fastjson.JSON;
import com.platon.browser.v0150.bean.AdjustParam;
import com.platon.browser.bean.CollectionBlock;
import com.platon.browser.bean.CollectionTransaction;
import com.platon.browser.bean.ComplementNodeOpt;
import com.platon.browser.bean.ReceiptResult;
import com.platon.browser.config.BlockChainConfig;
import com.platon.browser.dao.entity.NetworkStat;
import com.platon.browser.bean.*;
import com.platon.browser.elasticsearch.dto.NodeOpt;
import com.platon.browser.elasticsearch.dto.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.springframework.beans.BeanUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * @description: 测试数据
 * @author: chendongming@matrixelements.com
 * @create: 2019-11-13 15:11:37
 **/
@Slf4j
public class AgentTestData {
    public static final String testDataDir = AgentTestData.class.getClassLoader().getResource("./").getPath()+"../../../../../testdata/";
    private static String suffix=".json",encode="UTF8";
    private static String[] dataFile = {
            "node",
            "block",
            "raw-block",
            "transaction",
            "receipts",
            "staking",
            "delegate.estpl.yml",
            "verifier",
            "validator",
            "candidate",
            "address",
            "proposal",
            "blockChainConfig",
            "networkstat",
            "adjust-data"
    };

    protected List<CustomNode> nodeList= Collections.emptyList();
    protected List<CollectionBlock> blockList= Collections.emptyList();
    protected List<PlatonBlock.Block> rawBlockList= new ArrayList<>();
    protected List<CollectionTransaction> transactionList= Collections.emptyList();
    protected List<ReceiptResult> receiptResultList= Collections.emptyList();
    protected List<CustomStaking> stakingList= Collections.emptyList();
    protected List<CustomDelegation> delegationList= Collections.emptyList();
    protected List<CustomProposal> proposalList = Collections.emptyList();
    protected List<Node> verifierList = new ArrayList<>();
    protected List<Node> validatorList = new ArrayList<>();
    protected List<Node> candidateList = new ArrayList<>();
    protected List<CustomAddress> addressList= Collections.emptyList();
    protected List<NodeOpt> nodeOptList= new ArrayList<>();
    protected List<NetworkStat> networkStatList= new ArrayList<>();
    protected BlockChainConfig blockChainConfig = new BlockChainConfig();

    protected Map<Long,PlatonBlock.Block> rawBlockMap = new HashMap<>();
    protected Map<Long, ReceiptResult> receiptResultMap = new HashMap<>();

    protected List<AdjustParam> adjustParamList = new ArrayList<>();

    @Before
    public void init(){
        Arrays.asList(dataFile).forEach(fileName->{
            try {
                File data = new File(testDataDir + fileName + suffix);
                String content = FileUtils.readFileToString(data, encode);
                switch (fileName){
                    case "node":
                        nodeList = JSON.parseArray(content,CustomNode.class);
                        break;
                    case "block":
                        blockList = JSON.parseArray(content,CollectionBlock.class);
                        blockList.forEach(b->{
                            NodeOpt no = ComplementNodeOpt.newInstance().setId(b.getNum())
                                    .setCreTime(new Date())
                                    .setBNum(b.getNum())
                                    .setDesc("sfsf")
                                    .setNodeId(b.getNodeId())
                                    .setTime(new Date())
                                    .setTxHash("0x3435424242423")
                                    .setType(Transaction.TypeEnum.TRANSFER.getCode())
                                    .setUpdTime(new Date());
                            nodeOptList.add(no);
                        });
                        break;
                    case "raw-block":
                        List<BlockBean> blockBeans = JSON.parseArray(content, BlockBean.class);
                        blockBeans.forEach(b->{
                            PlatonBlock.Block block = new PlatonBlock.Block();
                            block.setTransactions(new ArrayList<>());
                            BeanUtils.copyProperties(b,block);
                            rawBlockList.add(block);
                            rawBlockMap.put(block.getNumber().longValue(),block);
                        });
                        break;
                    case "networkstat":
                        networkStatList = JSON.parseArray(content,NetworkStat.class);
                        break;
                    case "transaction":
                        transactionList = JSON.parseArray(content,CollectionTransaction.class);
                        break;
                    case "receipts":
                        receiptResultList = JSON.parseArray(content,ReceiptResult.class);
                        receiptResultList.forEach(rr->{
                            try {
                                rr.resolve(rr.getResult().get(0).getBlockNumber(), Executors.newFixedThreadPool(10));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            receiptResultMap.put(rr.getResult().get(0).getBlockNumber(),rr);
                        });
                        break;
                    case "staking":
                        stakingList = JSON.parseArray(content, CustomStaking.class);
                        break;
                    case "delegate.estpl.yml":
                        delegationList = JSON.parseArray(content, CustomDelegation.class);
                        break;
                    case "verifier":
                        List<NodeBean> verList = JSON.parseArray(content,NodeBean.class);
                        verifierList.addAll(verList);
                        break;
                    case "validator":
                        List<NodeBean> valList = JSON.parseArray(content,NodeBean.class);
                        validatorList.addAll(valList);
                        break;
                    case "candidate":
                        List<NodeBean> canList = JSON.parseArray(content,NodeBean.class);
                        candidateList.addAll(canList);
                        break;
                    case "address":
                        addressList = JSON.parseArray(content, CustomAddress.class);
                        break;
                    case "proposal":
                        proposalList = JSON.parseArray(content,CustomProposal.class);
                        break;
                    case "blockChainConfig":
                        blockChainConfig = JSON.parseObject(content,BlockChainConfig.class);
                        break;
                    case "adjust-data":
                        adjustParamList = JSON.parseArray(content, AdjustParam.class);
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


}
