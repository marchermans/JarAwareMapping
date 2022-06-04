package com.ldtteam.jam.mcpconfig;

public class Main
{
    public static void main(String[] args)
    {
        final JammerRuntime runtime = new JammerRuntime(
          TSRGRemapper::createObfuscatedToOfficial,
          TSRGIdentitySupplier::create,
          TSRGNewIdentitySupplier::create,
          TSRGNamedASTBuilder::ast,
          TSRGMetadataASTBuilder::create,
          TSRGNamedASTWriter::create,
          TSRGMappingRuntimeConfiguration::create,
          TSRGStatisticsWriter::create);

        runtime.run(args);
    }
}
