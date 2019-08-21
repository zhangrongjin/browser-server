package com.platon.browser.param;

import jnr.ffi.annotations.In;
import lombok.Data;

/**
 * User: dongqile
 * Date: 2019/8/6
 * Time: 15:05
 * txType=2001提交升级提案(创建提案)
 */
@Data
public class CreateProposalUpgradeParam {

    /**
     * 提交提案的验证人
     */
    private String verifier;

    /**
     * 提案在pIDID
     */
    private String pIDID;

    /**
     * 提案投票截止块高（EpochSize*N-20，不超过2周的块高）
     */
    private Integer endVotingBlock;

    /**
     * 升级版本
     */
    private Integer newVersion;



    public void init( String verifier, String pIDID, Integer endVotingBlock,Integer newVersion){
        this.setVerifier(verifier);
        this.setPIDID(pIDID);
        this.setEndVotingBlock(endVotingBlock);
        this.setNewVersion(newVersion);
    }
}
