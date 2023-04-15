package com.github.ruediste.digitalSmpsSim;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.module.SimpleModule;

import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ObjectMapperContextResolver implements ContextResolver<ObjectMapper> {

    private final ObjectMapper mapper;

    public ObjectMapperContextResolver() {
        mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(
                PropertyNamingStrategies.LOWER_CAMEL_CASE);
        SimpleModule module = new SimpleModule();

        mapper.registerModule(module);
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }
}