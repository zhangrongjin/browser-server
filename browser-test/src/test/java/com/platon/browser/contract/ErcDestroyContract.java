package com.platon.browser.contract;

import com.alaya.abi.solidity.TypeReference;
import com.alaya.abi.solidity.datatypes.Function;
import com.alaya.abi.solidity.datatypes.Type;
import com.alaya.crypto.Credentials;
import com.alaya.protocol.Web3j;
import com.alaya.protocol.core.RemoteCall;
import com.alaya.protocol.core.methods.response.TransactionReceipt;
import com.alaya.tx.Contract;
import com.alaya.tx.TransactionManager;
import com.alaya.tx.gas.GasProvider;
import java.util.Arrays;
import java.util.Collections;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://github.com/PlatONnetwork/client-sdk-java/releases">platon-web3j command line tools</a>,
 * or the com.alaya.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/PlatONnetwork/client-sdk-java/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 0.13.2.1.
 */
public class ErcDestroyContract extends Contract {
    private static final String BINARY = "6080604052348015600f57600080fd5b5060818061001e6000396000f3fe608060405260043610601c5760003560e01c8063cbf0b0c014601e575b005b348015602957600080fd5b50601c60048036036020811015603e57600080fd5b50356001600160a01b031680fffea265627a7a7231582008aeb6a2c7b583aea89b8b865b96d28aa40ce13451b18870db70d4692a6ab86364736f6c63430005110032";

    public static final String FUNC_KILL = "kill";

    protected ErcDestroyContract(String contractAddress, Web3j web3j, Credentials credentials, GasProvider contractGasProvider, Long chainId) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider, chainId);
    }

    protected ErcDestroyContract(String contractAddress, Web3j web3j, TransactionManager transactionManager, GasProvider contractGasProvider, Long chainId) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider, chainId);
    }

    public RemoteCall<TransactionReceipt> kill(String _addr) {
        final Function function = new Function(
                FUNC_KILL, 
                Arrays.<Type>asList(new com.alaya.abi.solidity.datatypes.Address(_addr)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public static RemoteCall<ErcDestroyContract> deploy(Web3j web3j, Credentials credentials, GasProvider contractGasProvider, Long chainId) {
        return deployRemoteCall(ErcDestroyContract.class, web3j, credentials, contractGasProvider, BINARY,  "", chainId);
    }

    public static RemoteCall<ErcDestroyContract> deploy(Web3j web3j, TransactionManager transactionManager, GasProvider contractGasProvider, Long chainId) {
        return deployRemoteCall(ErcDestroyContract.class, web3j, transactionManager, contractGasProvider, BINARY,  "", chainId);
    }

    public static ErcDestroyContract load(String contractAddress, Web3j web3j, Credentials credentials, GasProvider contractGasProvider, Long chainId) {
        return new ErcDestroyContract(contractAddress, web3j, credentials, contractGasProvider, chainId);
    }

    public static ErcDestroyContract load(String contractAddress, Web3j web3j, TransactionManager transactionManager, GasProvider contractGasProvider, Long chainId) {
        return new ErcDestroyContract(contractAddress, web3j, transactionManager, contractGasProvider, chainId);
    }
}
