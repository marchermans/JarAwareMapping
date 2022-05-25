package com.ldtteam.jam.spi.ast.named;

import java.util.Collection;

/**
 * Represents a named and identified method in an AST.
 */
public interface INamedMethod {

    /**
     * Gives access to the original name of the field in the AST.
     *
     * @return The original name of the field.
     */
    String originalName();

    /**
     * Gives access to the identification name of the field in the AST.
     * Depending on the generators scope, the identified name is unique across the entire scope of the
     * generator. Which means it might be unique permanently.
     *
     * @return The identifier name of the field.
     */
    String identifiedName();

    /**
     * Gives access to the unique identifier of the field.
     *
     * @return The identifier of the field.
     */
    int id();

    /**
     * Gives access to the original named descriptor of the method.
     *
     * @return The named descriptor.
     */
    String originalDescriptor();

    /**
     * Indicates if the method is static within the class.
     *
     * @return {@code true} when the method is static, {@code false} when not.
     */
    boolean isStatic();

    /**
     * Indicates if the method is a lambda within the class.
     * Does the best case approximation.
     * Will never mark a none lambda as a lambda, but will potentially not mark a lambda as being such.
     *
     * @return {@code true} when guaranteed to be a lambda, {@code false} when not, or when unsure.
     */
    boolean isLambda();

    /**
     * Gives access to the methods named parameters.
     *
     * @return The parameters of the method.
     */
    Collection<? extends INamedParameter> parameters();
}
