package com.fulfilment.application.monolith.location;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * EN: In-memory catalogue of valid warehouse locations (assignment fixture).
 *     Implements the domain port LocationResolver used by warehouse use cases.
 * PT: Catalogo em memoria de localizacoes validas de warehouse (fixture do assignment).
 *     Implementa a port de dominio LocationResolver usada pelos use cases de warehouse.
 */
@ApplicationScoped
public class LocationGateway implements LocationResolver {

  private static final List<Location> locations = new ArrayList<>();

  static {
    // EN: Seed data mirrors the business rules expected by the case study tests.
    // PT: Dados iniciais alinhados com as regras de negocio dos testes do case study.
    locations.add(new Location("ZWOLLE-001", 1, 40));
    locations.add(new Location("ZWOLLE-002", 2, 50));
    locations.add(new Location("AMSTERDAM-001", 5, 100));
    locations.add(new Location("AMSTERDAM-002", 3, 75));
    locations.add(new Location("TILBURG-001", 1, 40));
    locations.add(new Location("HELMOND-001", 1, 45));
    locations.add(new Location("EINDHOVEN-001", 2, 70));
    locations.add(new Location("VETSBY-001", 1, 90));
  }

  /**
   * EN: Resolves a location by its business identifier, or null if unknown.
   * PT: Resolve uma localizacao pelo identificador de negocio, ou null se desconhecida.
   *
   * @param identifier location id (e.g. ZWOLLE-001) / id da localizacao
   * @return matching location or null / localizacao correspondente ou null
   */
  @Override
  public Location resolveByIdentifier(String identifier) {
    return locations.stream()
        .filter(location -> Objects.equals(location.identification, identifier))
        .findFirst()
        .orElse(null);
  }
}
