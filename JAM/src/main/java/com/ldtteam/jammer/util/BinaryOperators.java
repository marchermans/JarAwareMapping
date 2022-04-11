package com.ldtteam.jammer.util;

import java.util.function.BinaryOperator;

public class BinaryOperators
{

    private BinaryOperators()
    {
        throw new IllegalStateException("Can not instantiate an instance of: BinaryOperators. This is a utility class");
    }

    public static <T> BinaryOperator<T> left() {
        return (a, b) -> a;
    }

    public static <T> BinaryOperator<T> right() {
        return (a, b) -> b;
    }
}
