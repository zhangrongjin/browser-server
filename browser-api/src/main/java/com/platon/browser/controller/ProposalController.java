package com.platon.browser.controller;

import com.platon.browser.request.PageReq;
import com.platon.browser.request.proposal.ProposalDetailRequest;
import com.platon.browser.request.proposal.VoteListRequest;
import com.platon.browser.response.BaseResp;
import com.platon.browser.response.RespPage;
import com.platon.browser.response.proposal.ProposalDetailsResp;
import com.platon.browser.response.proposal.ProposalListResp;
import com.platon.browser.response.proposal.VoteListResp;
import com.platon.browser.service.ProposalService;
import com.platon.browser.service.VoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 提案模块Contract。定义使用方法
 * 
 * @file AppDocProposalController.java
 * @description
 * @author zhangrj
 * @data 2019年8月31日
 */
@Slf4j
@RestController
public class ProposalController {
	@Resource
	private ProposalService proposalService;
	@Resource
	private VoteService voteService;

	@PostMapping("proposal/proposalList")
	public Mono<RespPage<ProposalListResp>> proposalList(@Valid @RequestBody(required = false) PageReq req) {
		return Mono.just(proposalService.list(req));
	}

	@PostMapping("proposal/proposalDetails")
	public Mono<BaseResp<ProposalDetailsResp>> proposalDetails(@Valid @RequestBody ProposalDetailRequest req) {
		return Mono.just(proposalService.get(req));
	}

	@PostMapping("proposal/voteList")
	public Mono<RespPage<VoteListResp>> voteList(@Valid @RequestBody VoteListRequest req) {
		return Mono.just(voteService.queryByProposal(req));
	}
}
