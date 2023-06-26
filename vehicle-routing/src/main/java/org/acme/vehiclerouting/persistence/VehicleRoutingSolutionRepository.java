package org.acme.vehiclerouting.persistence;

import org.acme.vehiclerouting.domain.VehicleRoutingSolution;

public class VehicleRoutingSolutionRepository {

    private VehicleRoutingSolution vehicleRoutingSolution;

    public VehicleRoutingSolutionRepository(VehicleRoutingSolution s) {
        vehicleRoutingSolution = s;
    }

    public VehicleRoutingSolution solution() {
        return vehicleRoutingSolution;
    }

    public void update(VehicleRoutingSolution vehicleRoutingSolution) {
        this.vehicleRoutingSolution = vehicleRoutingSolution;
    }
}
