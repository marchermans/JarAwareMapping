package com.ldtteam.jam.spi.ast.named;

import java.util.Collection;

/**
 * Represents a named syntax tree with mapping information.
 */
public interface INamedAST {

    /**
     * The classes in the ast that where named and identified.
     *
     * @return A collection of classes which where named.
     */
    Collection<? extends INamedClass> classes();
}
