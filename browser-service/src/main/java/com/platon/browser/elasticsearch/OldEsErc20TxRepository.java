package com.platon.browser.elasticsearch;

import com.alibaba.fastjson.JSON;
import com.platon.browser.elasticsearch.bean.TokenTxCount;
import com.platon.browser.elasticsearch.bean.TokenTxSummary;
import com.platon.browser.enums.Arc20TxGroupTypeEnum;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.ScriptQueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.ParsedValueCount;
import org.elasticsearch.search.aggregations.metrics.ValueCountAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 针对处理合约内部转账记录的ES处理器
 */
@Repository
@Slf4j
public class OldEsErc20TxRepository extends AbstractEsRepository {
    @Getter
    public String defaultIndexTemplateName = "browser_inner_tx_template";

    @PostConstruct
    public void init() {
        try {
            // Self-test index template.
            this.putIndexTemplate(this.defaultIndexTemplateName, this.defaultIndexTemplate());
        } catch (IOException e) {
            log.error("Automatic detection of internal transaction index template failed.", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getIndexName() {
        return config.getInnerTxIndexName();
    }

    public XContentBuilder defaultIndexTemplate() throws IOException {
        XContentBuilder indexPatterns = XContentFactory.jsonBuilder()
                .startObject()
                    .array("index_patterns", "browser_inner_tx_*")
                    .startObject("settings")
                        .field("number_of_shards", 5)
                        .field("number_of_replicas", 1)
                        .field("max_result_window", 2000000000)
                    .endObject()
                    .startObject("mappings")
                        .startObject("properties")
                            .startObject("seq").field("type", "long").endObject()
                            .startObject("hash").field("type", "keyword").endObject()
                            .startObject("bn").field("type", "long").endObject()
                            .startObject("from").field("type", "keyword").endObject()
                            .startObject("contract").field("type", "keyword").endObject()
                            .startObject("tto").field("type", "keyword").endObject()
                            .startObject("tValue").field("type", "keyword").endObject()
                            .startObject("decimal").field("type", "integer").endObject()
                            .startObject("name").field("type", "text").endObject()
                            .startObject("symbol").field("type", "keyword").endObject()
                            .startObject("sign").field("type", "keyword").endObject()
                            .startObject("result").field("type", "integer").endObject()
                            .startObject("fromType").field("type", "integer").endObject()
                            .startObject("toType").field("type", "integer").endObject()
                            .startObject("txFee").field("type", "keyword").endObject()
                            .startObject("bTime").field("type", "date").field("format", "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis").endObject()
                            .startObject("value").field("type", "keyword").endObject()
                            .startObject("info").field("type", "text").endObject()
                            .startObject("ctime").field("type", "date").field("format", "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis").endObject()
                        .endObject()
                    .endObject()
                .endObject();
        return indexPatterns;
    }


    public TokenTxSummary groupContractTxCount(){
        String equalCondition = "doc['from'].value == doc['tto'].value";
        TokenTxSummary equalSummary = groupContractTxCount(Arc20TxGroupTypeEnum.FROM,equalCondition);
        String notEqualCondition = "doc['from'].value != doc['tto'].value";
        TokenTxSummary fromSummary = groupContractTxCount(Arc20TxGroupTypeEnum.FROM,notEqualCondition);
        TokenTxSummary ttoSummary = groupContractTxCount(Arc20TxGroupTypeEnum.TTO,notEqualCondition);

        log.debug("from==tto的交易\n{}", JSON.toJSONString(equalSummary,true));
        log.debug("from角度from!=tto的交易\n{}", JSON.toJSONString(fromSummary,true));
        log.debug("tto角度tto!=from的交易\n{}", JSON.toJSONString(ttoSummary,true));

        // 汇总addressTxCount（【from==tto的交易】与任意一个【from!=tto的交易】的汇总），用于更新network_stat表的token_qty
        TokenTxSummary summary = new TokenTxSummary();
        summary.setAddressTxCount(
                equalSummary.getAddressTxCount()
                        +fromSummary.getAddressTxCount()
        );

        // 汇总addressTxCountMap（汇总地址from==tto、from!=tto的总数），用于更新address表token_qty
        Map<String,Long> summaryTxCountMap = summary.getAddressTxCountMap();
        Arrays.asList(equalSummary,fromSummary,ttoSummary).forEach(subSummary->{
            subSummary.getAddressTxCountMap().forEach((address,count)->{
                Long sum = summaryTxCountMap.get(address);
                if(sum==null) sum=0L;
                sum +=count;
                summaryTxCountMap.put(address,sum);
            });
        });

        // 汇总contractTxCountMap的tokenTxCountMap, 用于更新erc20_token_address_rel表的tx_count
        Map<String, TokenTxCount> contractTxCountMap = summary.getContractTxCountMap();
        Arrays.asList(equalSummary,fromSummary,ttoSummary).forEach(subSummary->{
            subSummary.getContractTxCountMap().forEach((contract,subTtc)->{
                TokenTxCount sumTtc = contractTxCountMap.get(contract);
                if(sumTtc==null) sumTtc = new TokenTxCount();
                // 累加地址分组交易数
                Map<String,Long> sumTokenTxCountMap = sumTtc.getTokenTxCountMap();
                subTtc.getTokenTxCountMap().forEach((address,subCount)->{
                    Long sumCount = sumTokenTxCountMap.get(address);
                    if(sumCount==null) sumCount = 0L;
                    sumCount +=subCount;
                    sumTokenTxCountMap.put(address,sumCount);
                });
                contractTxCountMap.put(contract,sumTtc);
            });
        });

        // 汇总contractTxCountMap的tokenTxCount（【from==tto的交易】与任意一个【from!=tto的交易】的汇总）, 用于更新erc20_token表的tx_count
        Arrays.asList(equalSummary,fromSummary).forEach(subSummary->{
            subSummary.getContractTxCountMap().forEach((contract,subTtc)->{
                TokenTxCount sumTtc = contractTxCountMap.get(contract);
                if(sumTtc==null) sumTtc = new TokenTxCount();
                // 累加合约内部交易数
                sumTtc.setTokenTxCount(sumTtc.getTokenTxCount()+subTtc.getTokenTxCount());
                contractTxCountMap.put(contract,sumTtc);
            });
        });

        log.debug("summary\n{}", JSON.toJSONString(summary,true));

        return summary;
    }

    /**
     * 根据groupType分组统计token合约交易数
     * @return
     */
    public TokenTxSummary groupContractTxCount(Arc20TxGroupTypeEnum groupType,String scriptCondition){
        TokenTxSummary tts = new TokenTxSummary();
        try {
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

            if(StringUtils.isNotBlank(scriptCondition)){
                // 脚本条件筛选
                searchSourceBuilder.from(0).size(0);
                Map<String, Object> params = new HashMap<>();//存放参数的map
                Script script =new Script(
                        ScriptType.INLINE,
                        "painless",
                        scriptCondition,
                        params
                );
                ScriptQueryBuilder scriptQueryBuilder = QueryBuilders.scriptQuery(script);
                BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
                boolQueryBuilder = boolQueryBuilder.must(scriptQueryBuilder);
                searchSourceBuilder.query(boolQueryBuilder);
            }

            //根据groupType.getField()进行分组统计个数

            TermsAggregationBuilder inAggs = AggregationBuilders
                    .terms(groupType.getTerms())
                    .field(groupType.getField());
            ValueCountAggregationBuilder inCountField = AggregationBuilders.count("tx_count").field(groupType.getField());
            inAggs.subAggregation(inCountField);

            TermsAggregationBuilder outAggs = AggregationBuilders
                    .terms(Arc20TxGroupTypeEnum.CONTRACT.getTerms())
                    .field(Arc20TxGroupTypeEnum.CONTRACT.getField());
            ValueCountAggregationBuilder outCountField = AggregationBuilders.count("tx_count").field(Arc20TxGroupTypeEnum.CONTRACT.getField());
            outAggs.subAggregation(outCountField);

            outAggs.subAggregation(inAggs);

            searchSourceBuilder.aggregation(outAggs);
            SearchRequest searchRequest = new SearchRequest(config.getInnerTxIndexName()).source(searchSourceBuilder);
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
            //分组在es中是分桶
            ParsedStringTerms contractTerms = response.getAggregations().get(Arc20TxGroupTypeEnum.CONTRACT.getTerms());
            List<? extends Terms.Bucket> contractBuckets = contractTerms.getBuckets();
            contractBuckets.forEach(tokenGroup -> {
                TokenTxCount ttc = new TokenTxCount();
                // 设置token地址和交易总数
                String tokenAddress = (String) tokenGroup.getKey();
                ParsedValueCount tokenValueCount = tokenGroup.getAggregations().get("tx_count");
                double tokenTxCount = tokenValueCount.value();
                ttc.setTokenTxCount((long)tokenTxCount);
                tts.getContractTxCountMap().put(tokenAddress,ttc);

                ParsedStringTerms innerTerms = tokenGroup.getAggregations().get(groupType.getTerms());
                innerTerms.getBuckets().forEach(innerGroup->{
                    String address = (String) innerGroup.getKey();
                    ParsedValueCount valueCount = innerGroup.getAggregations().get("tx_count");
                    double txCount = valueCount.value();
                    ttc.getTokenTxCountMap().put(address,(long)txCount);
                    log.info("name , count {} {}", address, txCount);
                });
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        tts.calculate();
        return tts;
    }
}