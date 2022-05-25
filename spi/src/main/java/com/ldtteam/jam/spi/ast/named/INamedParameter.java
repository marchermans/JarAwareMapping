package com.ldtteam.jam.spi.ast.named;

/**
 * Represents a named and identified parameter in an AST.
 */
public interface INamedParameter {
    /**
     * Gives access to the original name of the parameter in the AST.
     *
     * @return The original name of the parameter.
     */
    String originalName();

    /**
     * Gives access to the identification name of the parameter in the AST.
     * Depending on the generators scope, the identified name is unique across the entire scope of the
     * generator. Which means it might be unique permanently.
     *
     * @return The identifier name of the parameter.
     */
    String identifiedName();

    /**
     * Gives access to the unique identifier of the parameter.
     *
     * @return The identifier of the parameter.
     */
    int id();

    /**
     * The index of the parameter within the generators' method scope.
     * Might or might not be JVM indexes depending on the environment.
     *
     * @return The parameter index within the method descriptor.
     */
    int index();
}
