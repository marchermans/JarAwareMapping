package com.ldtteam.jam.spi.ast.named;

/**
 * Represents a named and identified field in an AST.
 */
public interface INamedField {
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
}
