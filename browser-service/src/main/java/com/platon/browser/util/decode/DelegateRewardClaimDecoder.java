package com.platon.browser.util.decode;

import com.platon.browser.param.DelegateRewardClaimParam;
import com.platon.browser.param.TxParam;
import com.platon.browser.param.claim.Reward;
import com.platon.browser.utils.HexTool;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.rlp.RlpDecoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * @description: 领取委托奖励交易输入参数解码器
 * @author: chendongming@juzix.net
 * @create: 2020-01-02 15:28:04
 **/
class DelegateRewardClaimDecoder {
    private DelegateRewardClaimDecoder(){}

    static TxParam decode(RlpList rootList,List<Log> logs) {

        String logData = logs.get(0).getData();
        RlpList rlp = RlpDecoder.decode(Numeric.hexStringToByteArray(logData));
        List<RlpType> rlpList = ((RlpList)(rlp.getValues().get(0))).getValues();
        String decodedStatus = new String(((RlpString)rlpList.get(0)).getBytes());
        int statusCode = Integer.parseInt(decodedStatus);

        DelegateRewardClaimParam param = DelegateRewardClaimParam.builder()
                 .rewardList(new ArrayList<>())
                .build();
        ((RlpList)RlpDecoder.decode(((RlpString)rlpList.get(1)).getBytes())
                .getValues()
                .get(0))
                .getValues()
                .forEach(rl -> {
                    RlpList rlpL = (RlpList)rl;

                    String nodeId = ((RlpString)rlpL.getValues().get(0)).asString();
                    BigInteger stakingNum = ((RlpString)rlpL.getValues().get(1)).asPositiveBigInteger();
                    BigInteger amount = ((RlpString)rlpL.getValues().get(2)).asPositiveBigInteger();

                    Reward reward = Reward.builder()
                            .nodeId(HexTool.prefix(nodeId))
                            .stakingNum(stakingNum)
                            .reward(new BigDecimal(amount))
                            .build();
                    param.getRewardList().add(reward);
                });
        return param;
    }
}
