package com.platon.browser.now.service.impl;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.platon.browser.dao.entity.Erc20TokenTransferRecord;
import com.platon.browser.dao.mapper.Erc20TokenTransferRecordMapper;
import com.platon.browser.dto.elasticsearch.ESResult;
import com.platon.browser.elasticsearch.TokenTransferRecordESRepository;
import com.platon.browser.elasticsearch.dto.ESTokenTransferRecord;
import com.platon.browser.elasticsearch.service.impl.ESQueryBuilderConstructor;
import com.platon.browser.elasticsearch.service.impl.ESQueryBuilders;
import com.platon.browser.now.service.Erc20TokenTransferRecordService;
import com.platon.browser.req.token.QueryTokenTransferRecordListReq;
import com.platon.browser.res.RespPage;
import com.platon.browser.res.token.QueryTokenTransferRecordListResp;
import com.platon.browser.util.ConvertUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 合约内部转账交易记录
 *
 * @author AgentRJ
 * @create 2020-09-23 16:08
 */
@Service
@Slf4j
public class Erc20TokenTransferRecordServiceImpl implements Erc20TokenTransferRecordService {

    @Autowired
    private Erc20TokenTransferRecordMapper erc20TokenTransferRecordMapper;

    @Autowired
    private TokenTransferRecordESRepository esTokenTransferRecordRepository;

    @Override
    public RespPage<QueryTokenTransferRecordListResp> queryTokenRecordList(QueryTokenTransferRecordListReq req) {
        if (log.isDebugEnabled()) {
            log.debug("~ queryTokenRecordList, params: " + JSON.toJSONString(req));
        }
        // logic:
        // 1、合约内部交易列表中，数据存储于ES，列表的获取走ES获取
        // 2、所有查询直接走ES，不进行DB检索
        RespPage<QueryTokenTransferRecordListResp> result = new RespPage<>();

        // construct of params
        ESQueryBuilderConstructor constructor = new ESQueryBuilderConstructor();

        ESResult<ESTokenTransferRecord> queryResultFromES = new ESResult<>();

        // condition: txHash/contract/txFrom/transferTo
        if (StringUtils.isNotEmpty(req.getContract())) {
            constructor.must(new ESQueryBuilders().terms("contract", Collections.singletonList(req.getContract())));
        }
        if (StringUtils.isNotEmpty(req.getAddress())) {
            constructor.buildMust(new BoolQueryBuilder()
                    .should(QueryBuilders.termQuery("from", req.getAddress()))
                    .should(QueryBuilders.termQuery("tto", req.getAddress())));
        }
        if (StringUtils.isNotEmpty(req.getTxHash())) {
            constructor.must(new ESQueryBuilders().term("hash", req.getTxHash()));
        }
        // Set sort field
        constructor.setDesc("seq");
        // response filed to show.
        constructor.setResult(new String[] { "seq", "hash", "bn", "from", "contract",
            "tto", "tValue", "decimal", "name", "symbol", "result", "bTime", "fromType", "toType"});
        try {
            queryResultFromES = this.esTokenTransferRecordRepository.search(constructor, ESTokenTransferRecord.class,
                req.getPageNo(), req.getPageSize());
        } catch (Exception e) {
            log.error("检索代币交易列表失败", e);
            return result;
        }

        List<ESTokenTransferRecord> records = queryResultFromES.getRsData();
        if (null == records || records.size() == 0) {
            log.debug("未检索到有效数据，参数：" + JSON.toJSONString(req));
            return result;
        }

        List<QueryTokenTransferRecordListResp> recordListResp = records.parallelStream()
                .filter(p -> p != null && p.getDecimal() != null)
                .map(p -> {
                return this.toQueryTokenTransferRecordListResp(req.getAddress(), p);
                }).collect(Collectors.toList());

        Page<?> page = new Page<>(req.getPageNo(),req.getPageSize());
        result.init(page, recordListResp);
        result.setTotalCount(queryResultFromES.getTotal());
        return result;
    }

    public QueryTokenTransferRecordListResp toQueryTokenTransferRecordListResp(String address, ESTokenTransferRecord record) {
        QueryTokenTransferRecordListResp resp =  QueryTokenTransferRecordListResp.builder()
                .txHash(record.getHash()).blockNumber(record.getBn())
                .txFrom(record.getFrom()).contract(record.getContract())
                .transferTo(record.getTto()).name(record.getName())
                .decimal(record.getDecimal()).symbol(record.getSymbol())
                .methodSign(record.getSign()).result(record.getResult())
                .blockTimestamp(record.getBTime()).systemTimestamp(new Date().getTime())
                .value(null == record.getValue() ? BigDecimal.ZERO : new BigDecimal(record.getValue()))
            .fromType(record.getFromType()).toType(record.getToType())
                .build();
        // Processing accuracy calculation.
        if (null != record.getTValue()) {
            BigDecimal transferValue = new BigDecimal(record.getTValue());
            BigDecimal actualTransferValue = ConvertUtil.convertByFactor(transferValue, record.getDecimal());
            resp.setTransferValue(actualTransferValue);
        } else {
            resp.setTransferValue(BigDecimal.ZERO);
        }
        // input or out
        if (null != address && address.equals(record.getFrom())) {
            resp.setType(QueryTokenTransferRecordListResp.TransferType.OUT.val());
        } else {
            resp.setType(QueryTokenTransferRecordListResp.TransferType.INPUT.val());
        }
        if(null == address){
            resp.setType(QueryTokenTransferRecordListResp.TransferType.NONE.val());
        }
        return resp;
    }

    @Override
    public int save(Erc20TokenTransferRecord record) {
        return this.erc20TokenTransferRecordMapper.insert(record);
    }

    @Override
    public int batchSave(List<Erc20TokenTransferRecord> list) {
        return this.erc20TokenTransferRecordMapper.batchInsert(list);
    }
}
