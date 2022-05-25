package com.ldtteam.jam.spi.ast.metadata;

/**
 * Represents an AST entry which has access information attached to it.
 */
public interface IMetadataWithAccessInformation {

    /**
     * Indicates if this entry is an interface.
     *
     * @return {@code true} when this entry is an interface, {@code false} when not.
     */
    boolean isInterface();

    /**
     * Indicates if this entry is an abstract class.
     *
     * @return {@code true} when this entry is an abstract class, {@code false} when not.
     */
    boolean isAbstract();


    /**
     * Indicates if this entry is a synthetic.
     *
     * @return {@code true} when this entry is a synthetic, {@code false} when not.
     */
    boolean isSynthetic();

    /**
     * Indicates if this entry is an annotation.
     *
     * @return {@code true} when this entry is an annotation, {@code false} when not.
     */
    boolean isAnnotation();

    /**
     * Indicates if this entry is an enum.
     *
     * @return {@code true} when this entry is an enum, {@code false} when not.
     */
    boolean isEnum();

    /**
     * Indicates if this entry is a package private visibility.
     *
     * @return {@code true} when this entry is a package private visibility, {@code false} when not.
     */
    boolean isPackagePrivate();

    /**
     * Indicates if this entry is a public visibility.
     *
     * @return {@code true} when this entry is a public visibility, {@code false} when not.
     */
    boolean isPublic();

    /**
     * Indicates if this entry is a private visibility.
     *
     * @return {@code true} when this entry is a private visibility, {@code false} when not.
     */
    boolean isPrivate();

    /**
     * Indicates if this entry is a protected visibility.
     *
     * @return {@code true} when this entry is a protected visibility, {@code false} when not.
     */
    boolean isProtected();

    /**
     * Indicates if this entry is static.
     *
     * @return {@code true} when this entry is static, {@code false} when not.
     */
    boolean isStatic();

    /**
     * Indicates if this entry is final.
     *
     * @return {@code true} when this entry is final, {@code false} when not.
     */
    boolean isFinal();
}
