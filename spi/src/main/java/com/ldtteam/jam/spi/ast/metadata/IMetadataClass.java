package com.ldtteam.jam.spi.ast.metadata;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Represents a class in the ast build from metadata.
 */
public interface IMetadataClass extends IMetadataWithAccessInformation {

    /**
     * Gives access to the name of the super class of this class.
     * <p>
     * This class is potentially not part of the current AST, if it is part of a library,
     * or part of the jdk.
     *
     * @return The name of the super class, potentially null if no super class is known or available.
     */
    @Nullable
    String getSuperName();

    /**
     * Gives access to a list of names of interfaces which this class implements.
     * <p>
     * The listed interfaces are potentially not all part of the current AST, if they are part of a library,
     * or part of the jdk.
     *
     * @return A collection of names of the interfaces that the class implements, potentially null if no interfaces are implemented.
     */
    @Nullable
    Collection<String> getInterfaces();

    /**
     * Gives access to the classes generic signature.
     *
     * @return The classes generic signature, potentially null or empty if not used.
     */
    @Nullable
    String getSignature();

    /**
     * Gives access to fields, identified by their name, inside the class.
     *
     * @return The fields of the class, can be null or empty if no fields exist.
     */
    @Nullable
    Map<String, ? extends IMetadataField> getFieldsByName();

    /**
     * Gives access to the methods, identified by a combination of name and descriptor, inside the class.
     *
     * @return The methods of the class, can be null or empty if no methods exist.
     */
    @Nullable
    Map<String, ? extends IMetadataMethod> getMethodsByName();

    /**
     * Gives access to the record components of this class.
     *
     * @return The record components of the class, can be null or empty if no record components exist.
     */
    @Nullable
    List<? extends IMetadataRecordComponent> getRecords();
}
