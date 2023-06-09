package org.acme.vehiclerouting.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.acme.vehiclerouting.bootstrap.DemoDataBuilder;
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;

@PlanningSolution
public class VehicleRoutingSolution {

    private String name;

    @ProblemFactCollectionProperty
    private List<Location> locationList;

    @ProblemFactCollectionProperty
    private List<Depot> depotList;

    @PlanningEntityCollectionProperty
    private List<Vehicle> vehicleList;

    @ProblemFactCollectionProperty
    @ValueRangeProvider
    private List<Customer> customerList;

    @PlanningScore
    private HardSoftLongScore score;

    private Location southWestCorner;
    private Location northEastCorner;

    public boolean isIdentical(VehicleRoutingSolution other) {
        if (this == other)
            return true;

        List<Long> d1 = depotList == null ? Collections.emptyList() : depotList.stream().map(Depot::getId).sorted().collect(Collectors.toList());
        List<Long> d2 = other.getDepotList() == null ? Collections.emptyList()
                : other.getDepotList().stream().map(Depot::getId).sorted().collect(Collectors.toList());

        boolean result = d1.equals(d2);
        if (!result)
            return false;

        List<Vehicle> v1 = vehicleList == null ? Collections.emptyList()
                : vehicleList.stream().filter(v -> !v.getCustomerList().isEmpty())
                        .sorted((i, j) -> Long.compare(i.getCustomerIds().get(0), j.getCustomerIds().get(0))).collect(Collectors.toList());
        List<Vehicle> v2 = other.getVehicleList() == null ? Collections.emptyList()
                : other.getVehicleList().stream().filter(v -> !v.getCustomerList().isEmpty())
                        .sorted((i, j) -> Long.compare(i.getCustomerIds().get(0), j.getCustomerIds().get(0))).collect(Collectors.toList());

        if (v1.size() != v2.size())
            return false;

        for (int i = 0; i < v1.size(); i++) {
            if (!v1.get(i).isIdentical(v2.get(i)))
                return false;
        }

        return true;
    }

    public VehicleRoutingSolution() {
    }

    public VehicleRoutingSolution(String name, VehicleRoutingSolution old, boolean keepCustomers) {
        this(name, old.locationList, old.depotList, old.vehicleList.stream().map(v -> new Vehicle(v, keepCustomers)).collect(Collectors.toList()),
                old.customerList, old.southWestCorner, old.northEastCorner);
        this.score = old.score;
    }

    public VehicleRoutingSolution(String name, List<Location> locationList, List<Depot> depotList, List<Vehicle> vehicleList, List<Customer> customerList,
            Location southWestCorner, Location northEastCorner) {
        this.name = name;
        this.locationList = locationList;
        this.depotList = depotList;
        this.vehicleList = vehicleList;
        this.customerList = customerList;
        this.southWestCorner = southWestCorner;
        this.northEastCorner = northEastCorner;
    }

    public static VehicleRoutingSolution empty() {
        VehicleRoutingSolution problem = DemoDataBuilder.builder().setMinDemand(1).setMaxDemand(2).setVehicleCapacity(77).setCustomerCount(77)
                .setVehicleCount(7).setDepotCount(1).setMinServiceTime(1).setMaxServiceTime(1).setMinServiceWindow(10000).setMaxServiceWindow(1000000)
                .setSouthWestCorner(new Location(0L, 51.44, -0.16)).setNorthEastCorner(new Location(0L, 51.56, -0.01)).build();

        problem.setScore(HardSoftLongScore.ZERO);

        return problem;
    }

    public Customer getCustomerById(long id) {
        return customerList.stream().filter(c -> c.getId() == id).findFirst().orElse(null);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Location> getLocationList() {
        return locationList;
    }

    public void setLocationList(List<Location> locationList) {
        this.locationList = locationList;
    }

    public List<Depot> getDepotList() {
        return depotList;
    }

    public void setDepotList(List<Depot> depotList) {
        this.depotList = depotList;
    }

    public List<Vehicle> getVehicleList() {
        return vehicleList;
    }

    public List<Vehicle> getUsedVehicleList() {
        return vehicleList.stream().filter(v -> !v.getCustomerList().isEmpty()).collect(Collectors.toList());
    }

    public void setVehicleList(List<Vehicle> vehicleList) {
        this.vehicleList = vehicleList;
    }

    public List<Customer> getCustomerList() {
        return customerList;
    }

    public void setCustomerList(List<Customer> customerList) {
        this.customerList = customerList;
    }

    public HardSoftLongScore getScore() {
        return score;
    }

    public void setScore(HardSoftLongScore score) {
        this.score = score;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    public List<Location> getBounds() {
        return Arrays.asList(southWestCorner, northEastCorner);
    }

    public long getDistanceMeters() {
        return getVehicleList() == null ? 0 : getVehicleList().stream().mapToLong(Vehicle::getTotalDistanceMeters).sum();
    }

    public long getTotalTime() {
        return getVehicleList() == null ? 0 : getVehicleList().stream().mapToLong(Vehicle::getTotalTime).sum();
    }

    public long getFixCost() {
        return getVehicleList().stream().filter(v -> v.getTotalDemand() > 0).mapToLong(Vehicle::getFixCost).sum();
    }
}
