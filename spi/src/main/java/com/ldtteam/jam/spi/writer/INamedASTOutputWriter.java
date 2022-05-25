package com.ldtteam.jam.spi.writer;

import com.ldtteam.jam.spi.ast.named.INamedAST;

import java.nio.file.Path;

/**
 * An output writer which writes named AST output
 * instead of raw ids to disk.
 */
public interface INamedASTOutputWriter {

    /**
     * Writes the given named AST to the given output directory, in the writers supported format.
     *
     * @param outputDirectory The output directory.
     * @param ast             The ast to write.
     */
    void write(
            Path outputDirectory,
            INamedAST ast);
}
