package com.ldtteam.jam.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.asm.ParameterData;
import com.ldtteam.jam.spi.payload.IPayloadSupplier;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class MethodDataUtils {

    private MethodDataUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: MethodDataUtils. This is a utility class");
    }

    public static <TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> List<ParameterData<TClassPayload, TMethodPayload, TParameterPayload>> parametersAsList(final MethodData<TClassPayload, TMethodPayload> method, final IPayloadSupplier<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> payloads) {
        return parametersAsStream(method, payloads)
                .collect(ListsUtil.parameters());
    }

    public static <TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> Set<ParameterData <TClassPayload, TMethodPayload, TParameterPayload>> parametersAsSet(final MethodData<TClassPayload, TMethodPayload> method, final IPayloadSupplier<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> payloads) {
        return parametersAsStream(method, payloads)
                .collect(SetsUtil.parameters());
    }

    public static <TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> Stream<ParameterData<TClassPayload, TMethodPayload, TParameterPayload>> parametersAsStream(final MethodData <TClassPayload, TMethodPayload> method, final IPayloadSupplier<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> payloads) {
        final int parameterCount = parameterCount(method);
        return IntStream.range(0, parameterCount)
                .mapToObj(i -> new ParameterData<>(method.owner(), method, method.node().parameters.get(i), i, parameterDescriptor(method, i), payloads.forParameter(method.owner().node(), method.node(), method.node().parameters.get(i))));
    }

    private static <TClassPayload, TMethodPayload> String parameterDescriptor(final MethodData<TClassPayload, TMethodPayload> method, final int index) {
        final Type methodDescriptor = Type.getMethodType(method.node().desc);
        if (methodDescriptor.getArgumentTypes().length <= index)
            throw new IllegalArgumentException("The index given for the parameter is outside of the given range. Bytecode contains more arguments then descriptor!");

        return methodDescriptor.getArgumentTypes()[index].getDescriptor();
    }

    private static <TClassPayload, TMethodPayload> int parameterCount(final MethodData<TClassPayload, TMethodPayload> method) {
        if (method.node().parameters != null)
            return method.node().parameters.size();

        final Type methodDescriptor = Type.getMethodType(method.node().desc);

        return methodDescriptor.getArgumentTypes().length;
    }

    public static <TClassPayload, TMethodPayload> Multimap<MethodData<TClassPayload, TMethodPayload>, MethodData<TClassPayload, TMethodPayload>> buildOverrideTree(final Map<MethodData<TClassPayload, TMethodPayload>, MethodData<TClassPayload, TMethodPayload>> methodsByRoot) {
        final Multimap<MethodData<TClassPayload, TMethodPayload>, MethodData<TClassPayload, TMethodPayload>> overrideTree = HashMultimap.create();
        methodsByRoot.forEach(overrideTree::put);
        return overrideTree;
    }
}
