package com.ldtteam.jam.mapping;

import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.mapping.IMapper;
import com.ldtteam.jam.spi.matching.IMatcher;
import com.ldtteam.jam.spi.matching.MatchingResult;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class ConstantBooleanReturnValuesFlippedMethodMapper extends SingleEntryBasedMapper<MethodData>
{

    public static IMapper<MethodData> create(final IMatcher<InsnList> remainderMatcher) {
        return new ConstantBooleanReturnValuesFlippedMethodMapper(remainderMatcher);
    }

    private final IMatcher<InsnList> remainderMatcher;

    private ConstantBooleanReturnValuesFlippedMethodMapper(final IMatcher<InsnList> remainderMatcher)
    {
        this.remainderMatcher = remainderMatcher;
    }

    @Override
    protected Optional<MethodData> map(final MethodData source, final Set<MethodData> candidates)
    {
        //Return type of the source is a boolean
        if (!source.node().desc.endsWith(")Z")) {
            return Optional.empty();
        }

        record MatchingResultForCandidate(MethodData candidate, MatchingResult result) { }
        return candidates.stream().map(candidate -> new MatchingResultForCandidate(candidate, map(source, candidate)))
                 .filter(result -> result.result() != MatchingResult.UNKNOWN)
                 .findFirst()
                 .flatMap(result -> result.result() == MatchingResult.MATCH ? Optional.of(result.candidate()) : Optional.empty());
    }

    private MatchingResult map(final MethodData source, final MethodData candidate) {
        if (source.node().instructions.size() != candidate.node().instructions.size())
            return MatchingResult.FAIL;

        final Set<Integer> returnIndices = IntStream.range(0, source.node().instructions.size())
                                             .filter(i -> source.node().instructions.get(i).getOpcode() == Opcodes.IRETURN)
                                             .boxed()
                                             .collect(Collectors.toSet());

        final List<Integer> sourceReturnedValueOpIndices = returnIndices
                                                             .stream()
                                                             .map(source.node().instructions::get)
                                                             .map(AbstractInsnNode::getPrevious)
                                                             .filter(Objects::nonNull)
                                                             .mapToInt(AbstractInsnNode::getOpcode)
                                                             .boxed().toList();

        final List<Integer> candidateReturnedValueOpIndices = returnIndices
                                                                .stream()
                                                                .map(candidate.node().instructions::get)
                                                                .map(AbstractInsnNode::getPrevious)
                                                                .filter(Objects::nonNull)
                                                                .mapToInt(AbstractInsnNode::getOpcode)
                                                                .boxed().toList();

        if (sourceReturnedValueOpIndices.size() != candidateReturnedValueOpIndices.size())
            return MatchingResult.FAIL;

        for (int i = 0; i < sourceReturnedValueOpIndices.size(); i++)
        {
            final int sourceOpcode = sourceReturnedValueOpIndices.get(i);
            final int candidateOpcode = candidateReturnedValueOpIndices.get(i);
            if (sourceOpcode == candidateOpcode)
                return MatchingResult.FAIL;
        }

        final InsnList sourceCopy = copy(source.node().instructions);
        final InsnList candidateCopy = copy(candidate.node().instructions);

        final AbstractInsnNode[] sourceArray = sourceCopy.toArray();
        final AbstractInsnNode[] candidateArray = candidateCopy.toArray();

        final List<AbstractInsnNode> sourceToRemove = returnIndices.stream().map(index -> sourceArray[index].getPrevious()).toList();
        final List<AbstractInsnNode> candidateToRemove = returnIndices.stream().map(index -> candidateArray[index].getPrevious()).toList();

        sourceToRemove.forEach(sourceCopy::remove);
        candidateToRemove.forEach(candidateCopy::remove);

        return remainderMatcher.match(sourceCopy, candidateCopy);
    }

    private static InsnList copy(InsnList insnList) {
        MethodNode mv = new MethodNode();
        insnList.accept(mv);
        return mv.instructions;
    }
}
