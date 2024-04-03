package com.ldtteam.jam.neoform;

import java.util.Locale;

public class Main
{
    public static void main(String[] args)
    {
        Locale.setDefault(Locale.Category.FORMAT, Locale.ROOT);

        final JammerRuntime runtime = new JammerRuntime(
          TSRGRemapper::createObfuscatedToOfficial,
          TSRGIdentitySupplier::create,
          TSRGExistingNameSupplier::create,
          TSRGNewIdentitySupplier::create,
          TSRGNamedASTBuilder::ast,
          TSRGMetadataASTBuilder::create,
          TSRGNamedASTWriter::create,
          TSRGMappingRuntimeConfiguration::create,
          TSRGStatisticsWriter::create);

        if (!runtime.run(args)) {
            System.exit(1);
        }
    }
}
