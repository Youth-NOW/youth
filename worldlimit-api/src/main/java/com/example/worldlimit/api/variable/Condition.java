package com.example.worldlimit.api.variable;

/**
 * 条件比较接口
 */
public interface Condition {
    /**
     * 比较操作符枚举
     */
    enum Operator {
        EQUALS("=="),           // 等于
        NOT_EQUALS("!="),       // 不等于
        GREATER_THAN(">"),      // 大于
        LESS_THAN("<"),         // 小于
        GREATER_EQUALS(">="),   // 大于等于
        LESS_EQUALS("<="),      // 小于等于
        CONTAINS("contains"),   // 包含
        STARTS_WITH("starts"),  // 开头是
        ENDS_WITH("ends"),      // 结尾是
        MATCHES("matches"),     // 正则匹配
        IN_RANGE("range");      // 范围内

        private final String symbol;

        Operator(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }

        public static Operator fromString(String text) {
            for (Operator op : values()) {
                if (op.symbol.equalsIgnoreCase(text) || op.name().equalsIgnoreCase(text)) {
                    return op;
                }
            }
            return null;
        }
    }

    /**
     * 检查条件是否满足
     * @param value 要检查的值
     * @return 是否满足条件
     */
    boolean test(String value);

    /**
     * 检查数值条件是否满足
     * @param value 要检查的数值
     * @return 是否满足条件
     */
    boolean testNumeric(Double value);

    /**
     * 获取条件描述
     * @return 条件描述
     */
    String getDescription();
} 