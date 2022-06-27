package com.ldtteam.jam.util;

import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.asm.ParameterData;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class MethodDataUtils {

    private MethodDataUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: MethodDataUtils. This is a utility class");
    }

    public static List<ParameterData> parametersAsList(final MethodData method) {
        return parametersAsStream(method)
                .collect(ListsUtil.parameters());
    }

    public static Set<ParameterData> parametersAsSet(final MethodData method) {
        return parametersAsStream(method)
                .collect(SetsUtil.parameters());
    }

    public static Stream<ParameterData> parametersAsStream(final MethodData method) {
        final int parameterCount = parameterCount(method);
        return IntStream.range(0, parameterCount)
                .mapToObj(i -> new ParameterData(method.owner(), method, method.node().parameters.get(i), i, parameterDescriptor(method, i)));
    }

    private static String parameterDescriptor(final MethodData method, final int index) {
        final Type methodDescriptor = Type.getMethodType(method.node().desc);
        if (methodDescriptor.getArgumentTypes().length <= index)
            throw new IllegalArgumentException("The index given for the parameter is outside of the given range. Bytecode contains more arguments then descriptor!");

        return methodDescriptor.getArgumentTypes()[index].getDescriptor();
    }

    private static int parameterCount(final MethodData method) {
        if (method.node().parameters != null)
            return method.node().parameters.size();

        final Type methodDescriptor = Type.getMethodType(method.node().desc);

        return methodDescriptor.getArgumentTypes().length;
    }
}
