package com.github.ruediste.digitalSmpsSim;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        // register(GraphRest.class);
        packages(this.getClass().getPackage().getName());
    }
}