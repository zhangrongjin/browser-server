package com.platon.browser.cache;

import com.platon.browser.bean.CustomProposal;
import com.platon.browser.dao.entity.Proposal;
import com.platon.browser.dao.entity.ProposalExample;
import com.platon.browser.dao.mapper.ProposalMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/**
 * 参数提案缓存
 * 缓存结构: Map<提案生效块号,List<参数提案交易Hash>>
 */
@Component
public class ProposalCache {
    @Resource
    private ProposalMapper proposalMapper;
    //<生效块号->提案实体列表>
    private static final Map<Long, Set<String>> cache = new HashMap<>();

    public void add(Long activeBlockNumber,String proposalId)
    {
        Set<String> proposalIdList = cache.computeIfAbsent(activeBlockNumber, k -> new HashSet<>());
        proposalIdList.add(proposalId);
    }

    public Set<String> get(Long activeBlockNumber){
        return cache.get(activeBlockNumber);
    }

    public void init() {
        // 初始化提案缓存：把所有状态为投票中的【参数提案】和【升级提案】缓存到内存中
        ProposalExample proposalExample = new ProposalExample();
        proposalExample.createCriteria().andStatusEqualTo(CustomProposal.StatusEnum.VOTING.getCode());
        List<Proposal> proposalList = proposalMapper.selectByExample(proposalExample);
        proposalList.forEach(p->add(p.getActiveBlock(),p.getHash()));
    }
}
