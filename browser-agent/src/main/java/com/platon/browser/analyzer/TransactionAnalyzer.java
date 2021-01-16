package com.platon.browser.analyzer;

import com.alaya.protocol.core.methods.response.Transaction;
import com.alaya.protocol.core.methods.response.TransactionReceipt;
import com.platon.browser.bean.CollectionBlock;
import com.platon.browser.bean.CollectionTransaction;
import com.platon.browser.bean.ComplementInfo;
import com.platon.browser.bean.Receipt;
import com.platon.browser.cache.AddressCache;
import com.platon.browser.client.PlatOnClient;
import com.platon.browser.client.SpecialApi;
import com.platon.browser.elasticsearch.dto.OldErcTx;
import com.platon.browser.enums.ContractTypeEnum;
import com.platon.browser.enums.InnerContractAddrEnum;
import com.platon.browser.exception.BeanCreateOrUpdateException;
import com.platon.browser.exception.BlankResponseException;
import com.platon.browser.exception.ContractInvokeException;
import com.platon.browser.param.DelegateExitParam;
import com.platon.browser.param.DelegateRewardClaimParam;
import com.platon.browser.service.erc20.Erc20ResolveServiceImpl;
import com.platon.browser.utils.TransactionUtil;
import com.platon.browser.v0151.service.ErcContractService;
import com.platon.browser.v0151.service.ErcDetectService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * 交易分析器
 */
@Slf4j
@Component
public class TransactionAnalyzer {
    @Resource
    private PlatOnClient platOnClient;
    @Resource
    private AddressCache addressCache;
    @Resource
    private SpecialApi specialApi;
    @Resource
    private Erc20ResolveServiceImpl erc20ResolveServiceImpl;
    @Resource
    private ErcContractService ercContractService;
    
    // 交易解析阶段，维护自身的普通合约地址列表，其初始化数据来自地址缓存
    // <普通合约地址,合约类型枚举>
    private static final Map<String, ContractTypeEnum> GENERAL_CONTRACT_ADDRESS_2_TYPE_MAP = new HashMap<>();

    public static Map<String, ContractTypeEnum> getGeneralContractAddressCache() {
        return Collections.unmodifiableMap(GENERAL_CONTRACT_ADDRESS_2_TYPE_MAP);
    }

    public static void setGeneralContractAddressCache(String key, ContractTypeEnum contractTypeEnum) {
        GENERAL_CONTRACT_ADDRESS_2_TYPE_MAP.put(key, contractTypeEnum);
    }

    private void initGeneralContractCache(AddressCache addressCache) {
        if (GENERAL_CONTRACT_ADDRESS_2_TYPE_MAP.isEmpty()) {
            addressCache.getEvmContractAddressCache()
                    .forEach(address -> GENERAL_CONTRACT_ADDRESS_2_TYPE_MAP.put(address, ContractTypeEnum.EVM));
            addressCache.getWasmContractAddressCache()
                    .forEach(address -> GENERAL_CONTRACT_ADDRESS_2_TYPE_MAP.put(address, ContractTypeEnum.WASM));
            addressCache.getEvmErc20ContractAddressCache()
                    .forEach(address -> GENERAL_CONTRACT_ADDRESS_2_TYPE_MAP.put(address, ContractTypeEnum.ERC20_EVM));
        }
    }

    public CollectionTransaction analyze(CollectionBlock collectionBlock, Transaction rawTransaction, Receipt receipt) throws BeanCreateOrUpdateException, ContractInvokeException, BlankResponseException {
        CollectionTransaction result = CollectionTransaction.newInstance()
                .updateWithBlock(collectionBlock)
                .updateWithRawTransaction(rawTransaction);
            // 使用地址缓存初始化普通合约缓存信息
            initGeneralContractCache(addressCache);

            // ============需要通过解码补充的交易信息============
            ComplementInfo ci = new ComplementInfo();

            String inputWithoutPrefix = StringUtils.isNotBlank(result.getInput()) ? result.getInput().replace("0x", "") : "";
            if (InnerContractAddrEnum.getAddresses().contains(result.getTo()) && StringUtils.isNotBlank(inputWithoutPrefix)) {
                // 如果to地址是内置合约地址，则解码交易输入
                TransactionUtil.resolveInnerContractInvokeTxComplementInfo(result, receipt.getLogs(), ci);
            } else {

                // 检测是否包指定代币的创建事件
                TransactionReceipt transactionReceipt = new TransactionReceipt();
                transactionReceipt.setLogs(receipt.getLogs());
                List<String> contractAddress = erc20ResolveServiceImpl.getContractAddressFromEvents(transactionReceipt);
                if (null != contractAddress && !contractAddress.isEmpty()) {
                    for (String address : contractAddress) {
                        ComplementInfo complementInfo = new ComplementInfo();
                        // 如果to地址为空则是普通合约创建
                        TransactionUtil.resolveGeneralContractCreateTxComplementInfo(result, address, platOnClient, complementInfo, log);
                        TransactionUtil.resolveErcContract(result, complementInfo, address, erc20ResolveServiceImpl, addressCache);
                        addressCache.updateFirst(address, complementInfo);
                        // 把合约地址添加至缓存
                        GENERAL_CONTRACT_ADDRESS_2_TYPE_MAP.put(address, ContractTypeEnum.getEnum(complementInfo.getContractType()));
                    }
                }

                if (StringUtils.isBlank(result.getTo())) {
                    // 如果to地址为空则是普通合约创建
                    TransactionUtil.resolveGeneralContractCreateTxComplementInfo(result, receipt.getContractAddress(), platOnClient, ci, log);


                    // 把回执里的合约地址回填到交易的to字段
                    result.setTo(receipt.getContractAddress());
                    // 检测当前合约是否是erc相关合约
                    ercContractService.analyze(receipt.getContractAddress());
                    // 解析ERC合约信息
                    TransactionUtil.resolveErcContract(result, ci, receipt.getContractAddress(), erc20ResolveServiceImpl, addressCache);
                    addressCache.updateFirst(receipt.getContractAddress(), ci);
                    // 把合约地址添加至缓存
                    GENERAL_CONTRACT_ADDRESS_2_TYPE_MAP.put(result.getTo(), ContractTypeEnum.getEnum(ci.getContractType()));
                } else {
                    if (GENERAL_CONTRACT_ADDRESS_2_TYPE_MAP.containsKey(result.getTo()) && inputWithoutPrefix.length() >= 8) {
                        // 如果是普通合约调用（EVM||WASM）
                        ContractTypeEnum contractTypeEnum = GENERAL_CONTRACT_ADDRESS_2_TYPE_MAP.get(result.getTo());
                        TransactionUtil.resolveGeneralContractInvokeTxComplementInfo(result, platOnClient, ci,contractTypeEnum, log);
                        result.setStatus(receipt.getStatus()); // 普通合约调用的交易是否成功只看回执的status,不用看log中的状态
                        if (result.getStatus() == com.platon.browser.elasticsearch.dto.Transaction.StatusEnum.SUCCESS.getCode()) {
                            // 普通合约调用成功, 取成功的代理PPOS虚拟交易列表
                            List<com.platon.browser.elasticsearch.dto.Transaction> successVirtualTransactions =
                                    TransactionUtil.processVirtualTx(collectionBlock, specialApi, platOnClient, result, receipt, log);
                            // 把成功的虚拟交易挂到当前普通合约交易上
                            result.setVirtualTransactions(successVirtualTransactions);
                            // 如果是erc20地址则需要查询转账记录
                            // 跨合约调用：需要取回执中的地址判定是否有代币交易
                            List<String> uniAddressList = new ArrayList<>();
                            receipt.getLogs().forEach(log -> {
                                if (StringUtils.isEmpty(log.getAddress())) {
                                    return;
                                }
                                if (!uniAddressList.contains(log.getAddress())) {
                                    uniAddressList.add(log.getAddress());
                                }
                            });
                            // 提取产生Event的合约地址，并进行解析
                            uniAddressList.forEach(addr -> {
                                if (addressCache.isEvmErc20ContractAddress(addr)) {
                                    List<OldErcTx> erc20Tokens = TransactionUtil
                                            .resolveInnerToken(result, ci, receipt.getLogs(), erc20ResolveServiceImpl, addressCache, addr);
                                    if (!erc20Tokens.isEmpty()) {
                                        result.getOldErcTxes().addAll((erc20Tokens));
                                    }
                                }
                            });
                        }
                    } else {
                        BigInteger value =
                                StringUtils.isNotBlank(result.getValue()) ? new BigInteger(result.getValue()) : BigInteger.ZERO;
                        if (value.compareTo(BigInteger.ZERO) >= 0) {
                            // 如果输入为空且value大于0，则是普通转账
                            TransactionUtil.resolveGeneralTransferTxComplementInfo(result, ci, addressCache);
                        }
                    }
                }
            }

            if (ci.getType() == null) {
                throw new BeanCreateOrUpdateException(
                        "交易类型为空,遇到未知交易:[blockNumber=" + result.getNum() + ",txHash=" + result.getHash() + "]");
            }
            if (ci.getType() == null) {
                throw new BeanCreateOrUpdateException(
                        "To地址为空:[blockNumber=" + result.getNum() + ",txHash=" + result.getHash() + "]");
            }

            // 默认取状态字段作为交易成功与否的状态
            int status = receipt.getStatus();
            if (InnerContractAddrEnum.getAddresses().contains(result.getTo()) && ci.getType() != com.platon.browser.elasticsearch.dto.Transaction.TypeEnum.TRANSFER.getCode()) {
                // 如果接收者为内置合约且不为转账, 取日志中的状态作为交易成功与否的状态
                status = receipt.getLogStatus();
            }

            // 交易信息
            result.setGasUsed(receipt.getGasUsed().toString())
                .setCost(result.decimalGasUsed().multiply(result.decimalGasPrice()).toString())
                .setFailReason(receipt.getFailReason())
                .setStatus(status)
                .setSeq(result.getNum() * 100000 + result.getIndex())
                .setInfo(ci.getInfo())
                .setType(ci.getType())
                .setToType(ci.getToType())
                .setContractAddress(receipt.getContractAddress())
                .setContractType(ci.getContractType())
                .setBin(ci.getBinCode())
                .setMethod(ci.getMethod());

            // 累加总交易数
            collectionBlock.setTxQty(collectionBlock.getTxQty() + 1);
            // 累加具体业务交易数
            switch (result.getTypeEnum()) {
                case TRANSFER: // 转账交易，from地址转账交易数加一
                    collectionBlock.setTranQty(collectionBlock.getTranQty() + 1);
                    break;
                case STAKE_CREATE:// 创建验证人
                case STAKE_INCREASE:// 增加自有质押
                case STAKE_MODIFY:// 编辑验证人
                case STAKE_EXIT:// 退出验证人
                case REPORT:// 举报验证人
                    collectionBlock.setSQty(collectionBlock.getSQty() + 1);
                    break;
                case DELEGATE_CREATE:// 发起委托
                    collectionBlock.setDQty(collectionBlock.getDQty() + 1);
                    break;
                case DELEGATE_EXIT:// 撤销委托
                    if (status == Receipt.SUCCESS) {
                        // 成功的领取交易才解析info回填
                        // 设置委托奖励提取额
                        DelegateExitParam param = result.getTxParam(DelegateExitParam.class);
                        BigDecimal reward = new BigDecimal(TransactionUtil.getDelegateReward(receipt.getLogs()));
                        param.setReward(reward);
                        result.setInfo(param.toJSONString());
                    }
                    collectionBlock.setDQty(collectionBlock.getDQty() + 1);
                    break;
                case CLAIM_REWARDS: // 领取委托奖励
                    DelegateRewardClaimParam param =
                            DelegateRewardClaimParam.builder().rewardList(new ArrayList<>()).build();
                    if (status == Receipt.SUCCESS) {
                        // 成功的领取交易才解析info回填
                        param = result.getTxParam(DelegateRewardClaimParam.class);
                    }
                    result.setInfo(param.toJSONString());
                    collectionBlock.setDQty(collectionBlock.getDQty() + 1);
                    break;
                case PROPOSAL_TEXT:// 创建文本提案
                case PROPOSAL_UPGRADE:// 创建升级提案
                case PROPOSAL_PARAMETER:// 创建参数提案
                case PROPOSAL_VOTE:// 提案投票
                case PROPOSAL_CANCEL:// 取消提案
                case VERSION_DECLARE:// 版本声明
                    collectionBlock.setPQty(collectionBlock.getPQty() + 1);
                    break;
                default:
            }
            // 累加当前交易的手续费到当前区块的txFee
            collectionBlock.setTxFee(collectionBlock.decimalTxFee().add(result.decimalCost()).toString());
            // 累加当前交易的能量限制到当前区块的txGasLimit
            collectionBlock.setTxGasLimit(collectionBlock.decimalTxGasLimit().add(result.decimalGasLimit()).toString());
            return result;
    }
}