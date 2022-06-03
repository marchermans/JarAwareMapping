package com.ldtteam.jam.matching.instructionlist;

import com.ldtteam.jam.spi.matching.IMatcher;
import com.ldtteam.jam.spi.matching.MatchingResult;
import com.ldtteam.jam.util.InstructionNodeUtils;
import org.objectweb.asm.tree.*;

public class DirectInstructionListMatcher implements IMatcher<InsnList> {

    public static IMatcher<InsnList> create() {
        return new DirectInstructionListMatcher();
    }

    private DirectInstructionListMatcher() {}

    @Override
    public MatchingResult match(InsnList left, InsnList right) {
        return InstructionNodeUtils.isSameInstructionList(left, right) ? MatchingResult.MATCH : MatchingResult.UNKNOWN;
    }
}
