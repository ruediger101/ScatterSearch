package org.acme.vehiclerouting.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.PlanningListVariable;

@PlanningEntity
public class Vehicle {

    private long id;
    private int capacity;
    private Depot depot;
    private int fixCost;

    @PlanningListVariable
    private List<Customer> customerList;

    public Vehicle() {
    }

    public Vehicle(Vehicle old) {
        this(old.id, old.capacity, old.depot, old.fixCost);
        this.customerList.addAll(old.customerList);
    }

    public Vehicle(long id, int capacity, Depot depot, int fixCost) {
        this.id = id;
        this.capacity = capacity;
        this.depot = depot;
        this.customerList = new ArrayList<>();
        this.fixCost = fixCost;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public Depot getDepot() {
        return depot;
    }

    public void setDepot(Depot depot) {
        this.depot = depot;
    }

    public int getNoCustomers() {
        return customerList.size();
    }

    public List<Customer> getCustomerList() {
        return customerList;
    }

    public void setCustomerList(List<Customer> customerList) {
        this.customerList = customerList;
    }

    public int getFixCost() {
        return fixCost;
    }

    public void setFixCost(int fixCost) {
        this.fixCost = fixCost;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    /**
     * @return route of the vehicle
     */
    public List<Location> getRoute() {
        if (customerList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Location> route = new ArrayList<Location>();

        route.add(depot.getLocation());
        for (Customer customer : customerList) {
            route.add(customer.getLocation());
        }
        route.add(depot.getLocation());

        return route;
    }

    public int getTotalDemand() {
        int totalDemand = 0;
        for (Customer customer : customerList) {
            totalDemand += customer.getDemand();
        }
        return totalDemand;
    }

    public long getTotalDistanceMeters() {
        if (customerList.isEmpty()) {
            return 0;
        }

        long totalDistance = 0;
        Location previousLocation = depot.getLocation();

        for (Customer customer : customerList) {
            totalDistance += previousLocation.getDistanceTo(customer.getLocation());
            previousLocation = customer.getLocation();
        }
        totalDistance += previousLocation.getDistanceTo(depot.getLocation());

        return totalDistance;
    }

    public int getNoServiceTimeViolations() {
        if (customerList.isEmpty()) {
            return 0;
        }

        int noViolations = 0;
        long currentTime = 0;
        Location previousLocation = depot.getLocation();

        for (Customer customer : customerList) {
            currentTime += previousLocation.getDistanceTo(customer.getLocation());
            if (currentTime > customer.getEndServiceWindow()) {
                noViolations++;
            }
            currentTime += Math.max(currentTime, customer.getBeginServiceWindow()) + customer.getServiceTime();
            previousLocation = customer.getLocation();
        }

        return noViolations;
    }

    public boolean isServiceTimeViolated() {
        if (customerList.isEmpty()) {
            return false;
        }

        long currentTime = 0;
        Location previousLocation = depot.getLocation();

        for (Customer customer : customerList) {
            currentTime += previousLocation.getDistanceTo(customer.getLocation());
            if (currentTime > customer.getEndServiceWindow()) {
                return true;
            }
            currentTime += Math.max(currentTime, customer.getBeginServiceWindow()) + customer.getServiceTime();
            previousLocation = customer.getLocation();
        }

        return false;
    }

    public long getTotalTime() {
        if (customerList.isEmpty()) {
            return 0;
        }

        long currentTime = 0;
        Location previousLocation = depot.getLocation();

        for (Customer customer : customerList) {
            currentTime += previousLocation.getDistanceTo(customer.getLocation());
            currentTime += Math.max(currentTime, customer.getBeginServiceWindow()) + customer.getServiceTime();
            previousLocation = customer.getLocation();
        }
        currentTime += previousLocation.getDistanceTo(depot.getLocation());
        return currentTime;
    }

    @Override
    public String toString() {
        return "Vehicle{" +
                "id=" + id +
                '}';
    }
}
