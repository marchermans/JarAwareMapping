package com.ldtteam.jam.neoform;

import com.ldtteam.jam.spi.ast.metadata.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MappingToyMetadata implements IMetadataAST
{

    private final Map<String, ClassInfo> classInfoMap;

    public MappingToyMetadata(Map<String, ClassInfo> classInfoMap) {
        this.classInfoMap = classInfoMap;
    }

    @Override
    public Map<String, ? extends IMetadataClass> getClassesByName() {
        return classInfoMap;
    }

    private interface IAccessible extends IMetadataWithAccessInformation {
        int getAccess();
        @Override
        default boolean isInterface() {
            return ((getAccess() & Opcodes.ACC_INTERFACE) != 0);
        }

        @Override
        default boolean isAbstract() {
            return ((getAccess() & Opcodes.ACC_ABSTRACT) != 0);
        }

        @Override
        default boolean isSynthetic() {
            return ((getAccess() & Opcodes.ACC_SYNTHETIC) != 0);
        }

        @Override
        default boolean isAnnotation() {
            return ((getAccess() & Opcodes.ACC_ANNOTATION) != 0);
        }

        @Override
        default boolean isEnum() {
            return ((getAccess() & Opcodes.ACC_ENUM) != 0);
        }

        @Override
        default boolean isPackagePrivate() {
            return (getAccess() & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) == 0;
        }

        @Override
        default boolean isPublic() {
            return (getAccess() & Opcodes.ACC_PUBLIC) != 0;
        }

        @Override
        default boolean isPrivate() {
            return (getAccess() & Opcodes.ACC_PRIVATE) != 0;
        }

        @Override
        default boolean isProtected() {
            return (getAccess() & Opcodes.ACC_PROTECTED) != 0;
        }

        @Override
        default boolean isStatic() {
            return (getAccess() & Opcodes.ACC_STATIC) != 0;
        }

        @Override
        default boolean isFinal() {
            return (getAccess() & Opcodes.ACC_FINAL) != 0;
        }
    }

    @SuppressWarnings("unused")
    public static class ClassInfo implements IAccessible, IMetadataClass {
        private String       superName;
        private List<String> interfaces;
        private Integer      access;
        private String                  signature;
        private Map<String, FieldInfo>  fields;
        private Map<String, MethodInfo> methods;
        private List<RecordInfo> records;

        @Override
        public String getSuperName()
        {
            return superName;
        }

        public void setSuperName(final String superName)
        {
            this.superName = superName;
        }

        @Override
        public List<String> getInterfaces()
        {
            return interfaces;
        }

        public void setInterfaces(final List<String> interfaces)
        {
            this.interfaces = interfaces;
        }

        @Override
        public int getAccess()
        {
            return access;
        }

        public void setAccess(final Integer access)
        {
            this.access = access;
        }

        @Override
        public String getSignature()
        {
            return signature;
        }

        public void setSignature(final String signature)
        {
            this.signature = signature;
        }

        @Override
        public Map<String, MethodInfo> getMethodsByName()
        {
            return methods;
        }

        public Map<String, MethodInfo> getMethods()
        {
            return methods;
        }

        @Override
        public Map<String, FieldInfo> getFieldsByName()
        {
            return fields;
        }

        public Map<String, FieldInfo> getFields()
        {
            return fields;
        }

        public void setFields(final Map<String, FieldInfo> fields)
        {
            this.fields = fields;
        }

        public void setMethods(final Map<String, MethodInfo> methods)
        {
            this.methods = methods;
        }

        @Override
        public List<RecordInfo> getRecords()
        {
            return records;
        }

        public void setRecords(final List<RecordInfo> records)
        {
            this.records = records;
        }

        public static class FieldInfo implements IAccessible, IMetadataField {
            private String desc;
            private Integer access;
            private String signature;
            private String force;

            @Override
            public String getDesc()
            {
                return desc;
            }

            public void setDesc(final String desc)
            {
                this.desc = desc;
            }

            @Override
            public int getAccess()
            {
                return access;
            }

            public void setAccess(final Integer access)
            {
                this.access = access;
            }

            @Override
            public String getSignature()
            {
                return signature;
            }

            public void setSignature(final String signature)
            {
                this.signature = signature;
            }

            @Override
            public String getForce()
            {
                return force;
            }

            public void setForce(final String force)
            {
                this.force = force;
            }
        }

        public static class MethodInfo implements IAccessible, IMetadataMethod {
            private Integer access;
            private String signature;
            private Bounce bouncer;
            private String      force;
            private Set<Method> overrides;
            private Method      parent;

            @Override
            public int getAccess()
            {
                return access == null ? 0 : access;
            }

            public void setAccess(final Integer access)
            {
                this.access = access;
            }

            @Override
            public String getSignature()
            {
                return signature;
            }

            public void setSignature(final String signature)
            {
                this.signature = signature;
            }

            @Override
            public Bounce getBouncer()
            {
                return bouncer;
            }

            public void setBouncer(final Bounce bouncer)
            {
                this.bouncer = bouncer;
            }

            @Override
            public String getForce()
            {
                return force;
            }

            public void setForce(final String force)
            {
                this.force = force;
            }

            @Override
            public Set<Method> getOverrides()
            {
                return overrides;
            }

            public void setOverrides(final Set<Method> overrides)
            {
                this.overrides = overrides;
            }

            @Override
            public Method getParent()
            {
                return parent;
            }

            public void setParent(final Method parent)
            {
                this.parent = parent;
            }
        }

        public static class RecordInfo implements IMetadataRecordComponent {
            private String field;
            private String desc;
            private List<String> methods = new ArrayList<>();

            @Override
            @NonNull
            public String getField()
            {
                return field;
            }

            public void setField(final String field)
            {
                this.field = field;
            }

            @Override
            @NonNull
            public String getDesc()
            {
                return desc;
            }

            public void setDesc(final String desc)
            {
                this.desc = desc;
            }

            @Override
            public List<String> getMethods()
            {
                return methods;
            }

            public void setMethods(final List<String> methods)
            {
                this.methods = methods;
            }
        }
    }

    @SuppressWarnings("unused")
    public static class Method implements IMetadataMethodReference {
        private String owner;
        private String name;
        private String desc;

        @Override
        @NonNull
        public String getOwner()
        {
            return owner;
        }

        public void setOwner(final String owner)
        {
            this.owner = owner;
        }

        @Override
        @NonNull
        public String getName()
        {
            return name;
        }

        public void setName(final String name)
        {
            this.name = name;
        }

        @Override
        @NonNull
        public String getDesc()
        {
            return desc;
        }

        public void setDesc(final String desc)
        {
            this.desc = desc;
        }
    }

    @SuppressWarnings("unused")
    private static class Bounce implements IMetadataBounce {
        private Method target;
        private Method owner;

        @Override
        public Method getTarget()
        {
            return target;
        }

        public void setTarget(final Method target)
        {
            this.target = target;
        }

        @Override
        public Method getOwner()
        {
            return owner;
        }

        public void setOwner(final Method owner)
        {
            this.owner = owner;
        }
    }
}
