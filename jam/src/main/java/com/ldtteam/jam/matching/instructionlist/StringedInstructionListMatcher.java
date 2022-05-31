package com.ldtteam.jam.matching.instructionlist;

import com.ldtteam.jam.spi.matching.IMatcher;
import com.ldtteam.jam.spi.matching.MatchingResult;
import org.objectweb.asm.tree.InsnList;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class StringedInstructionListMatcher implements IMatcher<InsnList> {

    private final IMatcher<String> innerStringMatcher;

    public StringedInstructionListMatcher(IMatcher<String> innerStringMatcher) {
        this.innerStringMatcher = innerStringMatcher;
    }

    @Override
    public MatchingResult match(InsnList left, InsnList right) {
        final String leftInstructionString = Arrays.stream(left.toArray()).map(Objects::toString).collect(Collectors.joining("\n"));
        final String rightInstructionString = Arrays.stream(right.toArray()).map(Objects::toString).collect(Collectors.joining("\n"));

        return innerStringMatcher.match(leftInstructionString, rightInstructionString);
    }
}
