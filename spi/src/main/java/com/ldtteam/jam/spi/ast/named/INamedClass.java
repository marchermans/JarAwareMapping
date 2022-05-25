package com.ldtteam.jam.spi.ast.named;

import java.util.Collection;

/**
 * Represents a named class in the AST.
 */
public interface INamedClass {

    /**
     * Gives access to the original name of the class in the AST.
     *
     * @return The original name of the class.
     */
    String originalName();

    /**
     * Gives access to the identification name of the class in the AST.
     * Depending on the generators scope, the identified name is unique across the entire scope of the
     * generator. Which means it might be unique permanently.
     *
     * @return The identifier name of the class.
     */
    String identifiedName();

    /**
     * Gives access to the unique identifier of the class.
     *
     * @return The identifier of the class.
     */
    int id();

    /**
     * Gives access to the fields which are part of this class in the AST.
     *
     * @return The fields of this class.
     */
    Collection<? extends INamedField> fields();

    /**
     * Gives access to the methods which are part of this class in the AST.
     *
     * @return The methods of this class.
     */
    Collection<? extends INamedMethod> methods();
}
