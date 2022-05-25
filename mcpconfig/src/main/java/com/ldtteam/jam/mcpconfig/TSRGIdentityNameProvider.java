package com.ldtteam.jam.mcpconfig;

import com.ldtteam.jam.spi.name.INameProvider;

public class TSRGIdentityNameProvider {

    public static INameProvider<Integer> classes() {
        return "net/minecraft/src/C_%d_"::formatted;
    }

    public static INameProvider<Integer> fields() {
        return "f_%d_"::formatted;
    }

    public static INameProvider<Integer> methods() {
        return "m_%d_"::formatted;
    }

    public static INameProvider<Integer> parameters() {
        return "p_%d_"::formatted;
    }

}
