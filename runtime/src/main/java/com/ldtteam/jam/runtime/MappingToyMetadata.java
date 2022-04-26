package com.ldtteam.jam.runtime;

import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MappingToyMetadata
{
    private interface IAccessible {
        int getAccess();

        default boolean isInterface() {
            return ((getAccess() & Opcodes.ACC_INTERFACE) != 0);
        }

        default boolean isAbstract() {
            return ((getAccess() & Opcodes.ACC_ABSTRACT) != 0);
        }

        default boolean isSynthetic() {
            return ((getAccess() & Opcodes.ACC_SYNTHETIC) != 0);
        }

        default boolean isAnnotation() {
            return ((getAccess() & Opcodes.ACC_ANNOTATION) != 0);
        }

        default boolean isEnum() {
            return ((getAccess() & Opcodes.ACC_ENUM) != 0);
        }

        default boolean isPackagePrivate() {
            return (getAccess() & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) == 0;
        }

        default boolean isPublic() {
            return (getAccess() & Opcodes.ACC_PUBLIC) != 0;
        }

        default boolean isPrivate() {
            return (getAccess() & Opcodes.ACC_PRIVATE) != 0;
        }

        default boolean isProtected() {
            return (getAccess() & Opcodes.ACC_PROTECTED) != 0;
        }

        default boolean isStatic() {
            return (getAccess() & Opcodes.ACC_STATIC) != 0;
        }

        default boolean isFinal() {
            return (getAccess() & Opcodes.ACC_FINAL) != 0;
        }
    }

    @SuppressWarnings("unused")
    public static class ClassInfo implements IAccessible {
        private String       superName;
        private List<String> interfaces;
        private Integer      access;
        private String                  signature;
        private Map<String, FieldInfo>  fields;
        private Map<String, MethodInfo> methods;
        private List<RecordInfo> records;

        public String getSuperName()
        {
            return superName;
        }

        public void setSuperName(final String superName)
        {
            this.superName = superName;
        }

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

        public String getSignature()
        {
            return signature;
        }

        public void setSignature(final String signature)
        {
            this.signature = signature;
        }

        public Map<String, FieldInfo> getFields()
        {
            return fields;
        }

        public void setFields(final Map<String, FieldInfo> fields)
        {
            this.fields = fields;
        }

        public Map<String, MethodInfo> getMethods()
        {
            return methods;
        }

        public void setMethods(final Map<String, MethodInfo> methods)
        {
            this.methods = methods;
        }

        public List<RecordInfo> getRecords()
        {
            return records;
        }

        public void setRecords(final List<RecordInfo> records)
        {
            this.records = records;
        }

        public static class FieldInfo implements IAccessible {
            private String desc;
            private Integer access;
            private String signature;
            private String force;

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

            public String getSignature()
            {
                return signature;
            }

            public void setSignature(final String signature)
            {
                this.signature = signature;
            }

            public String getForce()
            {
                return force;
            }

            public void setForce(final String force)
            {
                this.force = force;
            }
        }

        public static class MethodInfo implements IAccessible {
            private Integer access;
            private String signature;
            private Bounce bouncer;
            private String      force;
            private Set<Method> overrides;
            private Method      parent;

            @Override
            public int getAccess()
            {
                return access;
            }

            public void setAccess(final Integer access)
            {
                this.access = access;
            }

            public String getSignature()
            {
                return signature;
            }

            public void setSignature(final String signature)
            {
                this.signature = signature;
            }

            public Bounce getBouncer()
            {
                return bouncer;
            }

            public void setBouncer(final Bounce bouncer)
            {
                this.bouncer = bouncer;
            }

            public String getForce()
            {
                return force;
            }

            public void setForce(final String force)
            {
                this.force = force;
            }

            public Set<Method> getOverrides()
            {
                return overrides;
            }

            public void setOverrides(final Set<Method> overrides)
            {
                this.overrides = overrides;
            }

            public Method getParent()
            {
                return parent;
            }

            public void setParent(final Method parent)
            {
                this.parent = parent;
            }
        }

        public static class RecordInfo {
            private String field;
            private String desc;
            private List<String> methods = new ArrayList<>();

            public String getField()
            {
                return field;
            }

            public void setField(final String field)
            {
                this.field = field;
            }

            public String getDesc()
            {
                return desc;
            }

            public void setDesc(final String desc)
            {
                this.desc = desc;
            }

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

    public static class Method {
        private String owner;
        private String name;
        private String desc;

        public String getOwner()
        {
            return owner;
        }

        public void setOwner(final String owner)
        {
            this.owner = owner;
        }

        public String getName()
        {
            return name;
        }

        public void setName(final String name)
        {
            this.name = name;
        }

        public String getDesc()
        {
            return desc;
        }

        public void setDesc(final String desc)
        {
            this.desc = desc;
        }
    }

    private static class Bounce {
        private Method target;
        private Method owner;

        public Method getTarget()
        {
            return target;
        }

        public void setTarget(final Method target)
        {
            this.target = target;
        }

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
