package com.github.ruediste.digitalSmpsSim;

import java.util.List;

import org.springframework.stereotype.Service;

import com.github.ruediste.digitalSmpsSim.simulation.Plot;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Service
@Path("/api/plot")
@Produces(MediaType.APPLICATION_JSON)
public class PlotRest {

    @Inject
    Simulations simulations;

    @GET
    public List<Plot> getPlots() {
        return simulations.plots;
    }
}