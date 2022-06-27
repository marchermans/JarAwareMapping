package com.ldtteam.jam.rename;

import com.ldtteam.jam.spi.name.IRemapper;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.MethodRemapper;
import org.objectweb.asm.commons.Remapper;

///This class comes with thanks to the Forge FART team.
///It is adapted from their remapper, but has reduced functionality.
public final class EnhancedClassRemapper extends ClassRemapper
{
    private final RemapperAdapter           remapper;

    public EnhancedClassRemapper(ClassVisitor classVisitor, IRemapper remapper) {
        super(classVisitor, new RemapperAdapter(remapper));
        this.remapper = (RemapperAdapter) super.remapper;
    }

    private static final Handle META_FACTORY = new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false);
    private static final Handle ALT_META_FACTORY = new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "altMetafactory", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;", false);

    @Override
    public MethodVisitor visitMethod(final int access, final String mname, final String mdescriptor, final String msignature, final String[] exceptions) {
        String remappedDescriptor = remapper.mapMethodDesc(mdescriptor);
        MethodVisitor methodVisitor = cv.visitMethod(
          access,
          remapper.mapMethodName(className, mname, mdescriptor),
          remappedDescriptor,
          remapper.mapSignature(msignature, false),
          exceptions == null ? null : remapper.mapTypes(exceptions)
        );
        if (methodVisitor == null)
            return null;

        return new MethodRemapper(methodVisitor, remapper) {
            @Override
            public void visitLocalVariable(final String pname, final String pdescriptor, final String psignature, final Label start, final Label end, final int index) {
                super.visitLocalVariable(EnhancedClassRemapper.this.remapper.mapParameterName(className, mname, mdescriptor, index, pname), pdescriptor, psignature, start, end, index);
            }

            @Override
            public void visitInvokeDynamicInsn(final String name, final String descriptor, final Handle bootstrapMethodHandle, final Object... bootstrapMethodArguments) {
                if (META_FACTORY.equals(bootstrapMethodHandle) || ALT_META_FACTORY.equals(bootstrapMethodHandle)) {
                    String owner = Type.getReturnType(descriptor).getInternalName();
                    String odesc = ((Type)bootstrapMethodArguments[0]).getDescriptor();
                    // First constant argument is "samMethodType - Signature and return type of method to be implemented by the function object."
                    // index 2 is the signature, but with generic types. Should we use that instead?

                    // We can't call super, because that'd double map the name.
                    // So we do our own mappings.
                    Object[] remappedBootstrapMethodArguments = new Object[bootstrapMethodArguments.length];
                    for (int i = 0; i < bootstrapMethodArguments.length; ++i) {
                        remappedBootstrapMethodArguments[i] = remapper.mapValue(bootstrapMethodArguments[i]);
                    }
                    mv.visitInvokeDynamicInsn(
                      remapper.mapMethodName(owner, name, odesc), // We change this
                      remapper.mapMethodDesc(descriptor),
                      (Handle) remapper.mapValue(bootstrapMethodHandle),
                      remappedBootstrapMethodArguments);
                    return;
                }

                super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
            }
        };
    }

    private static final class RemapperAdapter extends Remapper
    {

        private final IRemapper remapper;

        private RemapperAdapter(final IRemapper remapper) {this.remapper = remapper;}

        @Override
        public String mapMethodName(final String owner, final String name, final String descriptor) {
            return remapper.remapMethod(owner, name, descriptor).orElse(name);
        }

        @Override
        public String mapFieldName(final String owner, final String name, final String descriptor) {
            return remapper.remapField(owner, name, descriptor).orElse(name);
        }

        @Override
        public String map(final String key) {
            return remapper.remapClass(key).orElse(key);
        }

        public String mapParameterName(final String className, final String methodName, final String descriptor, final int index, final String parameterName)
        {
            return remapper.remapParameter(className, methodName, descriptor, parameterName, index).orElse(parameterName);
        }

        @Override
        public String mapRecordComponentName(final String owner, final String name, final String descriptor) {
            return mapFieldName(owner, name, descriptor);
        }

        @Override
        public String mapPackageName(final String packageName) {
            return remapper.remapPackage(packageName).orElse(packageName);
        }

        @Override
        public Object mapValue(final Object value) {
            if (value instanceof final Handle handle) {
                // Backport of ASM!327 https://gitlab.ow2.org/asm/asm/-/merge_requests/327
                final boolean isFieldHandle = handle.getTag() <= Opcodes.H_PUTSTATIC;

                return new Handle(
                  handle.getTag(),
                  this.mapType(handle.getOwner()),
                  isFieldHandle
                    ? this.mapFieldName(handle.getOwner(), handle.getName(), handle.getDesc())
                    : this.mapMethodName(handle.getOwner(), handle.getName(), handle.getDesc()),
                  isFieldHandle ? this.mapDesc(handle.getDesc()) : this.mapMethodDesc(handle.getDesc()),
                  handle.isInterface());
            } else {
                return super.mapValue(value);
            }
        }
    }
}