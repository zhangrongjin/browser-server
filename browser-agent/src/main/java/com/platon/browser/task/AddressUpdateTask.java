package com.platon.browser.task;

import com.platon.browser.client.PlatonClient;
import com.platon.browser.client.RestrictingBalance;
import com.platon.browser.client.SpecialContractApi;
import com.platon.browser.dto.CustomAddress;
import com.platon.browser.engine.BlockChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.web3j.platon.BaseResponse;

import java.util.*;

import static com.platon.browser.engine.BlockChain.STAGE_DATA;

/**
 * @Auther: dongqile
 * @Date: 2019/8/17 20:09
 * @Description: 地址更新任务
 */
@Component
public class AddressUpdateTask {
    private static Logger logger = LoggerFactory.getLogger(AddressUpdateTask.class);

    @Autowired
    private PlatonClient client;

    @Scheduled(cron = "0/10 * * * * ?")
    protected void start () {
        StringBuilder sb = new StringBuilder();
        Collection<CustomAddress> addresses = BlockChain.ADDRESS_CACHE.getAllAddress();
        if(addresses.size()==0) return;
        addresses.forEach(address -> sb.append(address.getAddress()).append(";"));
        String params = sb.toString().substring(0,sb.lastIndexOf(";"));
        try {
            BaseResponse <List<RestrictingBalance>> response = SpecialContractApi.getRestrictingBalance(client.getWeb3j(),params);
            if(response.isStatusOk()&&response.data!=null&&response.data.size()>0){
                List <RestrictingBalance> data = response.data;
                Map<String,RestrictingBalance> map = new HashMap<>();
                data.forEach(rb->map.put(rb.getAccount(),rb));
                addresses.forEach(address->{
                    RestrictingBalance rb = map.get(address.getAddress());
                    if(rb!=null){
                        address.setRestrictingBalance(rb.getLockBalance()!=null?rb.getLockBalance().toString():"0");
                        address.setBalance(rb.getFreeBalance()!=null?rb.getFreeBalance().toString():"0");
                        // 把改动后的内容暂存至待更新列表
                        STAGE_DATA.getAddressStage().updateAddress(address);
                    }
                });
            }
        } catch (Exception e) {
            logger.error("锁仓合约查询余额出错:{}",e.getMessage());
        }
    }
}
