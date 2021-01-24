package com.platon.browser.v0152.bean;

import com.platon.browser.dao.entity.Token;
import com.platon.browser.v0152.enums.ErcTypeEnum;
import lombok.Data;

import java.util.Date;

@Data
public class ErcToken extends Token {
    private ErcTypeEnum typeEnum;
    private boolean dirty; // 是否脏了
    public ErcToken() {
        setTokenTxQty(0);
        setHolder(0);
        Date date = new Date();
        setCreateTime(date);
        setUpdateTime(date);
    }
}
