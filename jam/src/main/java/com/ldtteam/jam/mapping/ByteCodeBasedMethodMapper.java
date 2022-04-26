package com.ldtteam.jam.mapping;

import com.ldtteam.jam.spi.mapping.IMapper;
import org.objectweb.asm.tree.*;

import java.util.Optional;
import java.util.Set;

public final class ByteCodeBasedMethodMapper extends SingleEntryBasedMapper<MethodNode>
{

    public static IMapper<MethodNode> create() {
        return new ByteCodeBasedMethodMapper();
    }

    private ByteCodeBasedMethodMapper()
    {
    }

    @Override
    public Optional<MethodNode> map(final MethodNode source, final Set<MethodNode> candidates)
    {
        return candidates.stream().filter(candidate -> isSameInstructionList(source.instructions, candidate.instructions)).findFirst();
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
