package com.ldtteam.jam.matching.instructionlist;

import com.ldtteam.jam.spi.matching.IMatcher;
import com.ldtteam.jam.spi.matching.MatchingResult;
import org.objectweb.asm.tree.*;

public class DirectInstructionListMatcher implements IMatcher<InsnList> {

    public static IMatcher<InsnList> create() {
        return new DirectInstructionListMatcher();
    }

    private DirectInstructionListMatcher() {}

    @Override
    public MatchingResult match(InsnList left, InsnList right) {
        return isSameInstructionList(left, right) ? MatchingResult.MATCH : MatchingResult.FAIL;
    }

    private boolean isSameInstructionList(final InsnList source, final InsnList candidate) {
        if (source.size() != candidate.size())
        {
            return false;
        }

        for (int i = 0; i < source.size(); i++)
        {
            if (!isSameInstruction(source.get(i), candidate.get(i))) {
                return false;
            }
        }

        return true;
    }

    private boolean isSameInstruction(final AbstractInsnNode source, final AbstractInsnNode candidate) {
        if (source.getType() != candidate.getType())
        {
            return false;
        }

        if (source.getType() == AbstractInsnNode.METHOD_INSN)
        {
            final MethodInsnNode sourceMethodInsn = (MethodInsnNode) source;
            final MethodInsnNode candidateMethodInsn = (MethodInsnNode) candidate;

            if (!sourceMethodInsn.owner.equals(candidateMethodInsn.owner))
            {
                return false;
            }

            if (!sourceMethodInsn.name.equals(candidateMethodInsn.name))
            {
                return false;
            }

            if (!sourceMethodInsn.desc.equals(candidateMethodInsn.desc))
            {
                return false;
            }
        }

        if (source.getType() == AbstractInsnNode.FIELD_INSN) {
            final FieldInsnNode sourceFieldInsn = (FieldInsnNode) source;
            final FieldInsnNode candidateFieldInsn = (FieldInsnNode) candidate;

            if (!sourceFieldInsn.owner.equals(candidateFieldInsn.owner))
            {
                return false;
            }

            if (!sourceFieldInsn.name.equals(candidateFieldInsn.name))
            {
                return false;
            }

            if (!sourceFieldInsn.desc.equals(candidateFieldInsn.desc))
            {
                return false;
            }
        }

        if (source.getType() == AbstractInsnNode.MULTIANEWARRAY_INSN) {
            final MultiANewArrayInsnNode sourceMultiANewArrayInsn = (MultiANewArrayInsnNode) source;
            final MultiANewArrayInsnNode candidateMultiANewArrayInsn = (MultiANewArrayInsnNode) candidate;

            if (!sourceMultiANewArrayInsn.desc.equals(candidateMultiANewArrayInsn.desc)) {
                return false;
            }
        }

        if (source.getType() == AbstractInsnNode.TYPE_INSN) {
            final TypeInsnNode sourceTypeInsn = (TypeInsnNode) source;
            final TypeInsnNode candidateTypeInsn = (TypeInsnNode) candidate;

            if (!sourceTypeInsn.desc.equals(candidateTypeInsn.desc)) {
                return false;
            }
        }

        return source.getOpcode() == candidate.getOpcode();
    }
}
