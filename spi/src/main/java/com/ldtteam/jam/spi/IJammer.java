package com.ldtteam.jam.spi;

import com.ldtteam.jam.spi.configuration.Configuration;

public interface IJammer<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload>
{
    void run(Configuration<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> configuration);
}
