package com.platon.browser.complement.dao.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.springframework.transaction.annotation.Transactional;

import com.platon.browser.complement.dao.entity.AddressStatistics;
import com.platon.browser.complement.dao.entity.NetworkStatistics;
import com.platon.browser.complement.dao.param.BusinessParam;
import com.platon.browser.dao.entity.Address;
import com.platon.browser.dao.entity.NetworkStat;

/*
 * @Auther: dongqile
 * @Date:  2019/10/31
 * @Description:
 */
public interface StatisticBusinessMapper {
    /**
     * 地址数据变更
     * @param param
     */
    @Transactional
    void addressChange ( BusinessParam param );

    /**
     * 统计数据变更
     * @param param
     */
    @Transactional
    void networkChange ( NetworkStat param );
    
    /**
     * 获得网络
     * @return
     */
    NetworkStatistics getNetworkStatisticsFromNode ();
    
    /**
     * 获得地址数
     * @return
     */
	Integer getNetworkStatisticsFromAddress();
	
    /**
     * 获得投票中的提案
     * @return
     */
	Integer getNetworkStatisticsFromProposal();
		
	List<AddressStatistics> getAddressStatisticsFromStaking(@Param("list") List<String> list);

	List<AddressStatistics> getAddressStatisticsFromDelegation(@Param("list") List<String> list);
	
	@Transactional
	int batchUpdateFromTask(@Param("list") List<Address> list);


}