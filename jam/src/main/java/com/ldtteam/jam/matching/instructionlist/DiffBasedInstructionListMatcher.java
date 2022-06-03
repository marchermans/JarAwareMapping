package com.ldtteam.jam.matching.instructionlist;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;
import com.ldtteam.jam.spi.matching.IMatcher;
import com.ldtteam.jam.spi.matching.MatchingResult;
import com.ldtteam.jam.util.InstructionNodeUtils;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;
import org.objectweb.asm.util.TraceModuleVisitor;

import java.util.*;
import java.util.stream.Collectors;

public class DiffBasedInstructionListMatcher implements IMatcher<InsnList> {

    public static IMatcher<InsnList> create(final Map<Integer, Float> mappingThresholdPercentage) {
        return new DiffBasedInstructionListMatcher(mappingThresholdPercentage);
    }

    final Map<Integer, Float> mappingThresholdPercentage;

    private DiffBasedInstructionListMatcher(final Map<Integer, Float> mappingThresholdPercentage) {this.mappingThresholdPercentage = mappingThresholdPercentage;}


    @Override
    public MatchingResult match(InsnList left, InsnList right) {
        final List<InstructionNodeComparisonDelegate> leftInstructions = toComparisonArray(left);
        final List<InstructionNodeComparisonDelegate> rightInstructions = toComparisonArray(right);

        Patch<InstructionNodeComparisonDelegate> patch = DiffUtils.diff(leftInstructions, rightInstructions);
        final long deleteAndInsertCount = patch.getDeltas().stream().map(AbstractDelta::getType)
                                           .filter(type -> type == DeltaType.DELETE || type == DeltaType.INSERT)
                                           .count();

        final float mappingPercentage = 100 - (float) deleteAndInsertCount / patch.getDeltas().size();
        float matchChangePercentage = 100-0f;
        List<Map.Entry<Integer, Float>> toSort = new ArrayList<>(mappingThresholdPercentage.entrySet());
        toSort.sort(Map.Entry.comparingByKey());
        for (Map.Entry<Integer, Float> entry : toSort)
        {
            if (entry.getKey() < leftInstructions.size() || entry.getKey() < rightInstructions.size())
            {
                matchChangePercentage = entry.getValue();
            }
        }

        if (mappingPercentage >= matchChangePercentage)
        {
            return MatchingResult.MATCH;
        }

        return MatchingResult.UNKNOWN;
    }

    private static List<InstructionNodeComparisonDelegate> toComparisonArray(final InsnList instructions) {
        return Arrays.stream(instructions.toArray()).map(InstructionNodeComparisonDelegate::new).collect(Collectors.toList());
    }

    private static final class InstructionNodeComparisonDelegate {
        private final AbstractInsnNode instructionNode;

        private InstructionNodeComparisonDelegate(final AbstractInsnNode instructionNode) {this.instructionNode = instructionNode;}

        @Override
        public boolean equals(final Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof final InstructionNodeComparisonDelegate that))
            {
                return false;
            }

            return InstructionNodeUtils.isSameInstruction(instructionNode, that.instructionNode);
        }

        @Override
        public int hashCode()
        {
            return InstructionNodeUtils.instructionHashCode(instructionNode);
        }

        @Override
        public String toString()
        {
            final Textifier textifier = new Textifier();
            final TraceMethodVisitor traceMethodVisitor = new TraceMethodVisitor(textifier);
            instructionNode.accept(traceMethodVisitor);
            return textifier.text.get(0).toString();
        }
    }
}
