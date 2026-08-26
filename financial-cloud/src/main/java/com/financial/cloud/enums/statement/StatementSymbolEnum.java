package com.financial.cloud.enums.statement;

import lombok.Getter;

@Getter
public enum StatementSymbolEnum {
    PLUS("+"),           // 加
    MINUS("-");          // 减

    private final String value;

    StatementSymbolEnum(String value) {
        this.value = value;
    }

}
