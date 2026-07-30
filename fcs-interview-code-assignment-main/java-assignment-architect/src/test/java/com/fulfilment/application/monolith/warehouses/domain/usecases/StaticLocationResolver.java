package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import java.util.HashMap;
import java.util.Map;

/** Simple in-memory test double for {@link LocationResolver}, used by the use case unit tests. */
class StaticLocationResolver implements LocationResolver {

  private final Map<String, Location> locations = new HashMap<>();

  void register(Location location) {
    locations.put(location.identification, location);
  }

  @Override
  public Location resolveByIdentifier(String identifier) {
    return locations.get(identifier);
  }
}
