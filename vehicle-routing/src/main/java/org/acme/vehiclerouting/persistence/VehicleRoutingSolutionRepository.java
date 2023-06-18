package org.acme.vehiclerouting.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.acme.vehiclerouting.domain.VehicleRoutingSolution;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class VehicleRoutingSolutionRepository {

    private List<VehicleRoutingSolution> vehicleRoutingSolutions = new ArrayList<>();

    public Optional<VehicleRoutingSolution> solution() {
        return Optional.ofNullable(vehicleRoutingSolutions.get(0));
    }

    public void add(VehicleRoutingSolution vehicleRoutingSolution) {
        this.vehicleRoutingSolutions.add(vehicleRoutingSolution);
    }

    public void update(VehicleRoutingSolution vehicleRoutingSolution) {
        this.vehicleRoutingSolutions.set(0, vehicleRoutingSolution);
    }

    public List<VehicleRoutingSolution> getRefSet() {
        return vehicleRoutingSolutions;
    }
}
