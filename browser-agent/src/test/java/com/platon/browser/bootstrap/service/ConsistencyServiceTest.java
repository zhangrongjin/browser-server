package com.platon.browser.bootstrap.service;

import com.platon.browser.AgentTestBase;
import com.platon.browser.bean.CollectionNetworkStat;
import com.platon.browser.bootstrap.BootstrapEventPublisher;
import com.platon.browser.bootstrap.ShutdownCallback;
import com.platon.browser.dao.entity.NetworkStat;
import com.platon.browser.dao.mapper.NetworkStatMapper;
import com.platon.browser.dao.mapper.SyncTokenInfoMapper;
import com.platon.browser.service.block.BlockService;
import com.platon.browser.service.elasticsearch.EsBlockRepository;
import com.platon.browser.service.elasticsearch.bean.TokenTxSummary;
import com.platon.browser.service.receipt.ReceiptService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * @description: MySQL/ES/Redis启动一致性自检服务测试
 * @author: chendongming@matrixelements.com
 * @create: 2019-11-13 11:41:00
 **/
@RunWith(MockitoJUnitRunner.Silent.class)
public class ConsistencyServiceTest extends AgentTestBase {
    @Mock
    private NetworkStatMapper networkStatMapper;
    @Mock
    private EsBlockRepository ESBlockRepository;
    @Mock
    private BlockService blockService;
    @Mock
    private ReceiptService receiptService;
    @Mock
    private BootstrapEventPublisher bootstrapEventPublisher;
    @Mock
    private ShutdownCallback shutdownCallback;
    @Mock
    private SyncTokenInfoMapper syncTokenInfoMapper;

    @InjectMocks
    @Spy
    private ConsistencyService target;

    @Before
    public void setup() {
        when(shutdownCallback.isDone()).thenReturn(true);
    }

    @Test
    public void post() throws IOException {

        TokenTxSummary summary = new TokenTxSummary();
        //when(oldEsErc20TxRepository.groupContractTxCount()).thenReturn(summary);

        NetworkStat networkStat = null;
        when(networkStatMapper.selectByPrimaryKey(anyInt())).thenReturn(networkStat);
        target.post();

        networkStat = CollectionNetworkStat.newInstance();
        networkStat.setCurNumber(10L);
        when(networkStatMapper.selectByPrimaryKey(anyInt())).thenReturn(networkStat);
        when(ESBlockRepository.exists(anyString())).thenReturn(false);
        target.post();
        when(ESBlockRepository.exists(anyString())).thenReturn(true);
        target.post();

        when(blockService.getBlockAsync(anyLong())).thenReturn(null);
        when(receiptService.getReceiptAsync(anyLong())).thenReturn(null);
        target.post();

        verify(target, times(4)).post();
    }

}
