package com.ldtteam.jam.util;

import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class InstructionNodeUtils
{

    private InstructionNodeUtils()
    {
        throw new IllegalStateException("Can not instantiate an instance of: InstructionNodeUtils. This is a utility class");
    }

    public static boolean isSameInstructionList(final InsnList source, final InsnList candidate) {
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

    public static boolean isSameInstruction(final AbstractInsnNode source, final AbstractInsnNode candidate) {
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

    public static int instructionHashCode(final AbstractInsnNode instruction) {
        final List<Object> hashValues = new ArrayList<>();

        hashValues.add(instruction.getType());
        hashValues.add(instruction.getOpcode());

        if (instruction.getType() == AbstractInsnNode.METHOD_INSN && instruction instanceof final MethodInsnNode instructionMethod)
        {
            hashValues.add(instructionMethod.owner);
            hashValues.add(instructionMethod.name);
            hashValues.add(instructionMethod.desc);
        }

        if (instruction.getType() == AbstractInsnNode.FIELD_INSN && instruction instanceof final FieldInsnNode instructionField)
        {
            hashValues.add(instructionField.owner);
            hashValues.add(instructionField.name);
            hashValues.add(instructionField.desc);
        }

        if (instruction.getType() == AbstractInsnNode.MULTIANEWARRAY_INSN && instruction instanceof final MultiANewArrayInsnNode instructionSourceMultiANewArray) {
            hashValues.add(instructionSourceMultiANewArray.desc);
        }

        if (instruction.getType() == AbstractInsnNode.TYPE_INSN && instruction instanceof final TypeInsnNode instructionType) {
            hashValues.add(instructionType.desc);
        }

        return Objects.hash(hashValues.toArray());
    }
}
