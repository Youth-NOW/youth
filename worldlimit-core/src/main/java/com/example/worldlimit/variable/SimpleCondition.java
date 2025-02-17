package com.example.worldlimit.variable;

import com.example.worldlimit.api.variable.Condition;

import java.util.regex.Pattern;

public class SimpleCondition implements Condition {
    private final Operator operator;
    private final String expected;
    private final Double min;
    private final Double max;

    public SimpleCondition(Operator operator, String expected) {
        this.operator = operator;
        this.expected = expected;
        this.min = null;
        this.max = null;
    }

    public SimpleCondition(Double min, Double max) {
        this.operator = Operator.IN_RANGE;
        this.expected = null;
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean test(String value) {
        if (value == null) return false;
        
        switch (operator) {
            case EQUALS:
                return value.equals(expected);
            case NOT_EQUALS:
                return !value.equals(expected);
            case CONTAINS:
                return value.contains(expected);
            case STARTS_WITH:
                return value.startsWith(expected);
            case ENDS_WITH:
                return value.endsWith(expected);
            case MATCHES:
                return Pattern.matches(expected, value);
            default:
                try {
                    double numValue = Double.parseDouble(value);
                    return testNumeric(numValue);
                } catch (NumberFormatException e) {
                    return false;
                }
        }
    }

    @Override
    public boolean testNumeric(Double value) {
        if (value == null) return false;

        switch (operator) {
            case EQUALS:
                try {
                    return value.equals(Double.parseDouble(expected));
                } catch (NumberFormatException e) {
                    return false;
                }
            case NOT_EQUALS:
                try {
                    return !value.equals(Double.parseDouble(expected));
                } catch (NumberFormatException e) {
                    return false;
                }
            case GREATER_THAN:
                try {
                    return value > Double.parseDouble(expected);
                } catch (NumberFormatException e) {
                    return false;
                }
            case LESS_THAN:
                try {
                    return value < Double.parseDouble(expected);
                } catch (NumberFormatException e) {
                    return false;
                }
            case GREATER_EQUALS:
                try {
                    return value >= Double.parseDouble(expected);
                } catch (NumberFormatException e) {
                    return false;
                }
            case LESS_EQUALS:
                try {
                    return value <= Double.parseDouble(expected);
                } catch (NumberFormatException e) {
                    return false;
                }
            case IN_RANGE:
                return value >= min && value <= max;
            default:
                return false;
        }
    }

    @Override
    public String getDescription() {
        if (operator == Operator.IN_RANGE) {
            return "在范围 " + min + " 到 " + max + " 之间";
        }
        return operator.getSymbol() + " " + expected;
    }
} 