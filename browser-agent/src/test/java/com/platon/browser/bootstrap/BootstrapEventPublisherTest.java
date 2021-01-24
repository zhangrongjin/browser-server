package com.platon.browser.bootstrap;

import com.lmax.disruptor.dsl.Disruptor;
import com.platon.browser.AgentTestBase;
import com.platon.browser.bean.Receipt;
import com.platon.browser.bean.ReceiptResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import com.alaya.protocol.core.methods.response.PlatonBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @description: MySQL/ES/Redis启动一致性自检服务测试
 * @author: chendongming@matrixelements.com
 * @create: 2019-11-13 11:41:00
 **/
@RunWith(MockitoJUnitRunner.Silent.class)
public class BootstrapEventPublisherTest extends AgentTestBase {
    @Spy
    private BootstrapEventPublisher target;

    @Before
    public void setup() {
        ReflectionTestUtils.setField(target, "ringBufferSize", 1024);
    }

    @Test
    public void test(){
        target.init();
        ShutdownCallback shutdownCallback = new ShutdownCallback();
        shutdownCallback.setEndBlockNum(7000L);
        target.publish(getBlockAsync(7000L),getReceiptAsync(7000L),shutdownCallback);

        target.shutdown();
        Disruptor<BootstrapEvent> disruptor = target.getDisruptor();
        assertNotNull(disruptor);
        verify(target, times(1)).getDisruptor();
    }

    /**
     * 异步获取区块
     */
    public CompletableFuture<PlatonBlock> getBlockAsync(Long blockNumber) {
        return CompletableFuture.supplyAsync(()->{
            PlatonBlock pb = new PlatonBlock();
            PlatonBlock.Block block = rawBlockList.get(0);
            pb.setResult(block);
            return pb;
        });
    }

    /**
     * 异步获取区块
     */
    public CompletableFuture<ReceiptResult> getReceiptAsync(Long blockNumber) {
        return CompletableFuture.supplyAsync(()->{
            ReceiptResult receiptResult = new ReceiptResult();
            List<Receipt> receipts = new ArrayList<>();
            Receipt receipt = new Receipt();
            receipts.add(receipt);
            receiptResult.setResult(receipts);
            return receiptResult;
        });
    }
}
