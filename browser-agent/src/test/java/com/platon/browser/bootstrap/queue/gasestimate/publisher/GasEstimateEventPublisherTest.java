package com.platon.browser.bootstrap.queue.gasestimate.publisher;

import com.platon.browser.AgentTestBase;
import com.platon.browser.common.collection.dto.EpochMessage;
import com.platon.browser.common.queue.complement.handler.IComplementEventHandler;
import com.platon.browser.common.queue.gasestimate.handler.IGasEstimateEventHandler;
import com.platon.browser.common.queue.gasestimate.publisher.GasEstimateEventPublisher;
import com.platon.browser.elasticsearch.dto.Block;
import com.platon.browser.elasticsearch.dto.Transaction;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @description: MySQL/ES/Redis启动一致性自检服务测试
 * @author: chendongming@juzix.net
 * @create: 2019-11-13 11:41:00
 **/
@RunWith(MockitoJUnitRunner.Silent.class)
public class GasEstimateEventPublisherTest extends AgentTestBase {
    @Mock
    private IGasEstimateEventHandler handler;

    @Spy
    private GasEstimateEventPublisher target;

    @Before
    public void setup() {
        ReflectionTestUtils.setField(target, "handler", handler);
        ReflectionTestUtils.setField(target, "ringBufferSize", 1024);
    }

    @Test
    public void test(){
        ReflectionTestUtils.invokeMethod(target,"init");
        EpochMessage epochMessage = EpochMessage.newInstance();
        Block block = blockList.get(0);
        List<Transaction> transactions = new ArrayList<>(transactionList);

        target.publish(1l, Collections.emptyList());
        target.getRingBufferSize();
        target.info();
        target.getPublisherMap();
        target.register(target.getClass().getSimpleName(),target);
        target.unregister(target.getClass().getSimpleName());
        verify(target, times(1)).publish(any(),any());
    }
}