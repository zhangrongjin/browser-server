package com.platon.browser.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.platon.browser.client.PlatOnClient;
import com.platon.browser.bean.RestrictingBalance;
import com.platon.browser.client.SpecialApi;
import com.platon.browser.config.BlockChainConfig;
import com.platon.browser.dao.entity.Address;
import com.platon.browser.dao.entity.NetworkStat;
import com.platon.browser.dao.entity.RpPlan;
import com.platon.browser.dao.entity.RpPlanExample;
import com.platon.browser.dao.mapper.AddressMapper;
import com.platon.browser.dao.mapper.CustomRpPlanMapper;
import com.platon.browser.dao.mapper.RpPlanMapper;
import com.platon.browser.service.elasticsearch.EsBlockRepository;
import com.platon.browser.elasticsearch.dto.Block;
import com.platon.browser.enums.I18nEnum;
import com.platon.browser.exception.BusinessException;
import com.platon.browser.request.address.QueryDetailRequest;
import com.platon.browser.request.address.QueryRPPlanDetailRequest;
import com.platon.browser.response.address.DetailsRPPlanResp;
import com.platon.browser.response.address.QueryDetailResp;
import com.platon.browser.response.address.QueryRPPlanDetailResp;
import com.platon.browser.utils.ConvertUtil;
import com.platon.browser.utils.I18nUtil;
import com.alaya.contracts.ppos.RestrictingPlanContract;
import com.alaya.contracts.ppos.dto.CallResponse;
import com.alaya.contracts.ppos.dto.resp.RestrictingItem;
import com.alaya.contracts.ppos.dto.resp.Reward;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import com.alaya.protocol.core.DefaultBlockParameterName;

import javax.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * 地址具体逻辑实现方法
 *  @file AddressServiceImpl.java
 *  @description
 *	@author zhangrj
 *  @data 2019年8月31日
 */
@Service
public class AddressService {

	private final Logger logger = LoggerFactory.getLogger(AddressService.class);

    @Resource
    private AddressMapper addressMapper;
    @Resource
    private RpPlanMapper rpPlanMapper;
    @Resource
    private CustomRpPlanMapper customRpPlanMapper;
    @Resource
    private PlatOnClient platonClient;
    @Resource
    private I18nUtil i18n;
    @Resource
    private BlockChainConfig blockChainConfig;
    @Resource
	private EsBlockRepository ESBlockRepository;
    @Resource
    private SpecialApi specialApi;
    
    @Resource
    private StatisticCacheService statisticCacheService;

    public QueryDetailResp getDetails(QueryDetailRequest req) {
    	/** 根据主键查询地址信息 */
        Address item = addressMapper.selectByPrimaryKey(req.getAddress());
        QueryDetailResp resp = new QueryDetailResp();
        if (item != null) {
        	BeanUtils.copyProperties(item, resp);
        	resp.setDelegateUnlock(item.getDelegateHes());
        	/** 预先设置是否展示锁仓 */
        	resp.setIsRestricting(0);
        	resp.setStakingValue(item.getStakingValue().add(item.getDelegateValue()));
        	resp.setIsDestroy(StringUtils.isBlank(item.getContractDestroyHash())?0:1);
        	resp.setContractCreateHash(item.getContractCreatehash());
        	resp.setDestroyHash(item.getContractDestroyHash());
        	resp.setContractName(ConvertUtil.captureName(item.getContractName()));
        }
        /** 特殊账户余额直接查询链  */
	  	try {
	  		this.getAddressInfo(req, resp);
	  	} catch (Exception e) {
	  		logger.error("getBalance error",e);
	  		platonClient.updateCurrentWeb3jWrapper();
	  		try {
	  			this.getAddressInfo(req, resp);
			} catch (Exception e1) {
				logger.error("getBalance error again",e);
			} 
		}
        RpPlanExample rpPlanExample = new RpPlanExample();
		RpPlanExample.Criteria criteria = rpPlanExample.createCriteria();
		criteria.andAddressEqualTo(req.getAddress());
        List<RpPlan> rpPlans = rpPlanMapper.selectByExample(rpPlanExample);
        /** 有锁仓数据之后就可以返回1 */
        if(rpPlans != null && !rpPlans.isEmpty()) {
        	resp.setIsRestricting(1);
        }
       return resp;
    }

	public QueryRPPlanDetailResp rpplanDetail(QueryRPPlanDetailRequest req) {
		QueryRPPlanDetailResp queryRPPlanDetailResp = new QueryRPPlanDetailResp();
		try {
			// 锁仓可用余额查询特殊节点接口
			List<RestrictingBalance> restrictingBalances = specialApi.getRestrictingBalance(platonClient.getWeb3jWrapper().getWeb3j(), req.getAddress());
			if(restrictingBalances != null && !restrictingBalances.isEmpty()) {
				/**
				 * 可用余额为balance减去质押金额
				 */
				queryRPPlanDetailResp.setRestrictingBalance(new BigDecimal(restrictingBalances.get(0).getLockBalance().subtract(restrictingBalances.get(0).getPledgeBalance())));
			}
			// 质押金额和待释放金额查询锁仓合约
			RestrictingPlanContract restrictingPlanContract = platonClient.getRestrictingPlanContract();
			CallResponse<RestrictingItem> baseResponse = restrictingPlanContract.getRestrictingInfo(req.getAddress()).send();
			if(baseResponse.isStatusOk()) {
				queryRPPlanDetailResp.setStakingValue(new BigDecimal(baseResponse.getData().getPledge()));
				queryRPPlanDetailResp.setUnderReleaseValue(new BigDecimal(baseResponse.getData().getDebt()));
			}
		} catch (Exception e) {
			logger.error("rpplanDetail error", e);
			throw new BusinessException(i18n.i(I18nEnum.SYSTEM_EXCEPTION));
		}
		/**
		 * 分页查询对应的锁仓计划
		 */
		RpPlanExample rpPlanExample = new RpPlanExample();
		RpPlanExample.Criteria criteria = rpPlanExample.createCriteria();
		criteria.andAddressEqualTo(req.getAddress());
		List<DetailsRPPlanResp> detailsRPPlanResps = new ArrayList<>();
		PageHelper.startPage(req.getPageNo(),req.getPageSize());
		Page<RpPlan> rpPlans = rpPlanMapper.selectByExample(rpPlanExample);
		for(RpPlan rPlan : rpPlans) {
			DetailsRPPlanResp detailsRPPlanResp = new DetailsRPPlanResp();
			BeanUtils.copyProperties(rPlan, detailsRPPlanResp);
			/**
			 * 锁仓周期对应快高  结算周期数 * epoch  + number,如果不是整数倍则为：结算周期 * （epoch-1）  + 多余的数目
			 */
			BigInteger number;
			long remainder = rPlan.getNumber() % blockChainConfig.getSettlePeriodBlockCount().longValue();
			if(remainder == 0l) {
				number = blockChainConfig.getSettlePeriodBlockCount()
						.multiply(rPlan.getEpoch()).add(BigInteger.valueOf(rPlan.getNumber()));
			} else {
				number = blockChainConfig.getSettlePeriodBlockCount()
						.multiply(rPlan.getEpoch().subtract(BigInteger.ONE)).add(BigInteger.valueOf(rPlan.getNumber()))
						.add(blockChainConfig.getSettlePeriodBlockCount().subtract(BigInteger.valueOf(remainder)));
			}

			detailsRPPlanResp.setBlockNumber(number.toString());
			/** 预计时间：预计块高减去当前块高乘以出块时间再加上区块时间 */
			Block block = null;
			try {
				block = ESBlockRepository.get(String.valueOf(rPlan.getNumber()), Block.class);
			} catch (IOException e) {
				logger.error("获取区块错误。", e);
			}
			BigDecimal diff = new BigDecimal(number.subtract(BigInteger.valueOf(rPlan.getNumber())));
			if(block!=null) {
				if(diff.compareTo(BigDecimal.ZERO) > 0) {
					NetworkStat networkStat = statisticCacheService.getNetworkStatCache();
					detailsRPPlanResp.setEstimateTime(new BigDecimal(networkStat.getAvgPackTime()).multiply(diff)
					.add(BigDecimal.valueOf(block.getTime().getTime())).longValue());
				} else {
					detailsRPPlanResp.setEstimateTime(block.getTime().getTime());
				}
			}
			detailsRPPlanResps.add(detailsRPPlanResp);
		}
		queryRPPlanDetailResp.setRpPlans(detailsRPPlanResps);
		/**
		 * 获取计算总数
		 */
		BigDecimal bigDecimal = customRpPlanMapper.selectSumByAddress(req.getAddress());
		if(bigDecimal != null) {
			queryRPPlanDetailResp.setTotalValue(bigDecimal);
		}
		/**
		 * 获取列表总数
		 */
		queryRPPlanDetailResp.setTotal(rpPlans.getTotal());
		return queryRPPlanDetailResp;
	}
	
	private QueryDetailResp getAddressInfo(QueryDetailRequest req, QueryDetailResp resp) throws Exception {
		List<RestrictingBalance> restrictingBalances = specialApi.getRestrictingBalance(platonClient.getWeb3jWrapper().getWeb3j(), req.getAddress());
		if(restrictingBalances != null && !restrictingBalances.isEmpty()) {
			resp.setBalance(new BigDecimal(restrictingBalances.get(0).getFreeBalance()));
			resp.setRestrictingBalance(new BigDecimal(restrictingBalances.get(0).getLockBalance().subtract(restrictingBalances.get(0).getPledgeBalance())));
		}
		/** 特殊账户余额直接查询链  */
		if(resp.getBalance().compareTo(BigDecimal.valueOf(10000000000l)) > 0) {
			BigInteger balance = platonClient.getWeb3jWrapper().getWeb3j().platonGetBalance(req.getAddress(),DefaultBlockParameterName.LATEST).send().getBalance();
			resp.setBalance(new BigDecimal(balance));
		}
		/**
		 * 查询所有的交易金额进行汇总
		 */
		List<String> nodes = new ArrayList<>();
		List<Reward> rewards = platonClient.getRewardContract().getDelegateReward(req.getAddress(), nodes).send().getData();
		/**
		 * 当奖励为空时直接return
		 */
		if (rewards == null) {
			return resp;
		}
		BigDecimal allRewards = BigDecimal.ZERO;
		for(Reward reward : rewards) {
			allRewards = allRewards.add(new BigDecimal(reward.getReward()));
		}
		resp.setDelegateClaim(allRewards);
		return resp;
	}
}
