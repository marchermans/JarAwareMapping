package com.ldtteam.jam.spi.matching;

/**
 * Describes object which are capable of matching objects of the given type.
 * @param <TType> The type which the matcher can match.
 */
public interface IMatcher<TType> {

    /**
     * Checks if the given objects match.
     *
     * @param left The left (original) object.
     * @param right THe right (new) object.
     * @return A matching result indicating if the objects are equal or not.
     */
    MatchingResult match(final TType left, final TType right);
}
