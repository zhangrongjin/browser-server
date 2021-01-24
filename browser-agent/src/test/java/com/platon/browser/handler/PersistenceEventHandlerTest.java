package com.platon.browser.handler;

import com.platon.browser.AgentTestBase;
import com.platon.browser.bean.PersistenceEvent;
import com.platon.browser.cache.NetworkStatCache;
import com.platon.browser.service.elasticsearch.EsImportService;
import com.platon.browser.service.redis.RedisImportService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @description: MySQL/ES/Redis启动一致性自检服务测试
 * @author: chendongming@matrixelements.com
 * @create: 2019-11-13 11:41:00
 **/
@RunWith(MockitoJUnitRunner.Silent.class)
public class PersistenceEventHandlerTest extends AgentTestBase {
    @Mock
    private EsImportService esImportService;
    @Mock
    private RedisImportService redisImportService;
    @Mock
    private NetworkStatCache networkStatCache;
    @InjectMocks
    @Spy
    private PersistenceEventHandler target;

    @Before
    public void setup() throws Exception {

    }

}
