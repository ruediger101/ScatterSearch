package org.acme.vehiclerouting.bootstrap;

import org.acme.vehiclerouting.domain.Location;
import org.acme.vehiclerouting.domain.VehicleRoutingSolution;
import org.acme.vehiclerouting.persistence.VehicleRoutingSolutionsRepository;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class DemoDataGenerator {

    private final VehicleRoutingSolutionsRepository repository;

    public DemoDataGenerator(VehicleRoutingSolutionsRepository repository) {
        this.repository = repository;
    }

    public void generateDemoData(@Observes StartupEvent startupEvent) {
        VehicleRoutingSolution problem = DemoDataBuilder.builder()
                .setMinDemand(1)
                .setMaxDemand(5)
                .setMinServiceTime(50)
                .setMaxServiceTime(500)
                .setMinServiceWindow(5000)
                .setMaxServiceWindow(40000)
                .setVehicleCapacity(25)
                .setCustomerCount(77)
                .setVehicleCount(50)
                .setDepotCount(1)
                .setVehicleFixCost(10000)
                .setSouthWestCorner(new Location(0L, 49.43069, 11.03332))
                .setNorthEastCorner(new Location(0L, 49.49069, 11.13332))
                .build();
        repository.addAll(SequentialInsertion.solve(problem, 1, false));
        repository.addAll(SequentialInsertion.solve(problem, 99, true));
    }
}
