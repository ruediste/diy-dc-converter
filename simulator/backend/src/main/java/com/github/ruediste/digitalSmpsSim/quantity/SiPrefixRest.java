package com.github.ruediste.digitalSmpsSim.quantity;

import java.util.List;
import java.util.stream.Stream;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("api/siPrefix")
@Produces(MediaType.APPLICATION_JSON)
public class SiPrefixRest {

	public static class SiPrefixPMod {
		public String symbol;
		public double multiplier;
	}

	@GET
	public List<SiPrefixPMod> list() {
		return Stream.of(SiPrefix.values()).map(SiPrefixRest::toPMod).toList();
	}

	public static SiPrefixPMod toPMod(SiPrefix x) {
		var pMod = new SiPrefixPMod();
		pMod.symbol = x.symbol;
		pMod.multiplier = x.multiplier;
		return pMod;
	}
}
