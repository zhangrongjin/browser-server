package com.platon.browser.service;


import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import com.alaya.protocol.Web3j;
import com.alaya.protocol.core.RemoteCall;
import com.alaya.protocol.core.Request;
import com.alaya.protocol.core.methods.response.PlatonGetBalance;

import com.github.pagehelper.Page;
import com.platon.browser.ApiTestMockBase;
import com.platon.browser.client.PlatOnClient;
import com.platon.browser.bean.RestrictingBalance;
import com.platon.browser.client.SpecialApi;
import com.platon.browser.client.Web3jWrapper;
import com.platon.browser.dao.entity.Address;
import com.platon.browser.dao.entity.RpPlan;
import com.platon.browser.dao.mapper.CustomRpPlanMapper;
import com.platon.browser.dao.mapper.RpPlanMapper;
import com.platon.browser.request.address.QueryDetailRequest;
import com.platon.browser.request.address.QueryRPPlanDetailRequest;
import com.alaya.contracts.ppos.RestrictingPlanContract;
import com.alaya.contracts.ppos.RewardContract;
import com.alaya.contracts.ppos.dto.CallResponse;
import com.alaya.contracts.ppos.dto.resp.RestrictingItem;
import com.alaya.contracts.ppos.dto.resp.Reward;


@RunWith(MockitoJUnitRunner.Silent.class)
public class AddressServiceTest extends ApiTestMockBase {
	@Mock
    private RpPlanMapper rpPlanMapper;
	@Mock
    private CustomRpPlanMapper customRpPlanMapper;
	@Mock
    private PlatOnClient platonClient;
	@Mock
    private SpecialApi specialApi;
	@Spy
    private AddressService target;
	
	@SuppressWarnings("unchecked")
	@Before
    public void setup() throws Exception {
        ReflectionTestUtils.setField(target, "addressMapper", addressMapper);
        ReflectionTestUtils.setField(target, "rpPlanMapper", rpPlanMapper);
        ReflectionTestUtils.setField(target, "customRpPlanMapper", customRpPlanMapper);
        ReflectionTestUtils.setField(target, "platonClient", platonClient);
        ReflectionTestUtils.setField(target, "i18n", i18n);
        ReflectionTestUtils.setField(target, "blockChainConfig", blockChainConfig);
        ReflectionTestUtils.setField(target, "blockESRepository", ESBlockRepository);
        ReflectionTestUtils.setField(target, "specialApi", specialApi);
        ReflectionTestUtils.setField(target, "statisticCacheService", statisticCacheService);
        RestrictingPlanContract restrictingPlanContract = mock(RestrictingPlanContract.class);
        when(platonClient.getRestrictingPlanContract()).thenReturn(restrictingPlanContract);
        RestrictingItem restrictingItem = new RestrictingItem();
		restrictingItem.setBalance("0x0");
		restrictingItem.setDebt("0x0");
		restrictingItem.setPledge("0x0");
		CallResponse<RestrictingItem> baseResponse = new CallResponse<RestrictingItem>();
		baseResponse.setCode(0);
		baseResponse.setData(restrictingItem);
		
		RemoteCall<CallResponse<RestrictingItem>> remoteCall =mock(RemoteCall.class);
		when(restrictingPlanContract.getRestrictingInfo(anyString())).thenReturn(remoteCall);
		when(remoteCall.send()).thenReturn(baseResponse);
    }
	
	@SuppressWarnings("unchecked")
	@Test
	public void getDetails() throws Exception {
		QueryDetailRequest req = new QueryDetailRequest();
		req.setAddress("0x1000000000000000000000000000000000000001");
//		assertNotNull(addressService.getDetails(req));
		
		Address address = new Address();
		address.setAddress("0x1000000000000000000000000000000000000001");
		address.setBalance(BigDecimal.ZERO);
		address.setDelegateHes(BigDecimal.ZERO);
		address.setStakingValue(BigDecimal.ZERO);
		address.setDelegateValue(BigDecimal.ZERO);
		address.setContractDestroyHash("0x");
		
		when(addressMapper.selectByPrimaryKey(req.getAddress())).thenReturn(address);
		List<RestrictingBalance> restrictingBalances = new ArrayList<RestrictingBalance>();
		RestrictingBalance restrictingBalance = new RestrictingBalance();
		restrictingBalance.setAccount("0x1000000000000000000000000000000000000001");
		restrictingBalance.setFreeBalance("0x10");
		restrictingBalance.setLockBalance("0x10");
		restrictingBalance.setPledgeBalance("0x10");
		restrictingBalances.add(restrictingBalance);
		when(specialApi.getRestrictingBalance(any(),any())).thenReturn(restrictingBalances);
		Web3jWrapper web3jWrapper = mock(Web3jWrapper.class);
		when(platonClient.getWeb3jWrapper()).thenReturn(web3jWrapper);
		Web3j web3j = mock(Web3j.class);
		when(web3jWrapper.getWeb3j()).thenReturn(web3j);
		Request<?, PlatonGetBalance> rq = mock(Request.class);
		doReturn(rq).when(web3j).platonGetBalance(any(),any());
		PlatonGetBalance platonGetBalance = mock(PlatonGetBalance.class);
		when(rq.send()).thenReturn(platonGetBalance);
		
		when(platonClient.getWeb3jWrapper().getWeb3j().platonGetBalance(any(),any()).send().getBalance()).thenReturn(BigInteger.ONE);
		
		RewardContract rewardContract = mock(RewardContract.class);
		when(platonClient.getRewardContract()).thenReturn(rewardContract);
		
		RemoteCall<CallResponse<List<Reward>>> remoteCall = mock(RemoteCall.class);
		when(rewardContract.getDelegateReward(any(),any())).thenReturn(remoteCall);
		
		CallResponse<List<Reward>> callResponse = mock(CallResponse.class);
		when(remoteCall.send()).thenReturn(callResponse);
		when(platonClient.getRewardContract().getDelegateReward(any(),any()).send().getData()).thenReturn(null);
		target.getDetails(req);
		List<Reward> rewards = new ArrayList<Reward>();
		Reward reward = new Reward();
		reward.setReward("0x0");
		when(platonClient.getRewardContract().getDelegateReward(any(),any()).send().getData()).thenReturn(rewards);
		
		Page<RpPlan> rpPlans = new Page<RpPlan>();
		RpPlan rpPlan = new RpPlan();
		rpPlans.add(rpPlan);
		when(rpPlanMapper.selectByExample(any())).thenReturn(rpPlans);
		target.getDetails(req);
	}
	@Test
	public void rpplanDetail() throws Exception {

		List<RestrictingBalance> restrictingBalances = new ArrayList<RestrictingBalance>();
		RestrictingBalance restrictingBalance = new RestrictingBalance();
		restrictingBalance.setAccount("0x1000000000000000000000000000000000000001");
		restrictingBalance.setFreeBalance("0x10");
		restrictingBalance.setLockBalance("0x10");
		restrictingBalance.setPledgeBalance("0x10");
		restrictingBalances.add(restrictingBalance);
		when(specialApi.getRestrictingBalance(any(),any())).thenReturn(restrictingBalances);
		Web3jWrapper web3jWrapper = mock(Web3jWrapper.class);
		when(platonClient.getWeb3jWrapper()).thenReturn(web3jWrapper);

		QueryRPPlanDetailRequest req = new QueryRPPlanDetailRequest();
		req.setPageNo(0);
		req.setPageSize(10);
		req.setAddress("0x60ceca9c1290ee56b98d4e160ef0453f7c40d219");
		Page<RpPlan> rpPlansPage = new Page<RpPlan>();
		RpPlan rpPlan = new RpPlan();
		rpPlan.setAddress("0x60ceca9c1290ee56b98d4e160ef0453f7c40d219");
		rpPlan.setAmount(BigDecimal.TEN);
		rpPlan.setEpoch(BigInteger.TEN);
		rpPlan.setNumber(10l);
		rpPlansPage.add(rpPlan);
		when(rpPlanMapper.selectByExample(any())).thenReturn(rpPlansPage);
		when(customRpPlanMapper.selectSumByAddress(any())).thenReturn(BigDecimal.TEN);
		assertNotNull(target.rpplanDetail(req));
	}
	
}
