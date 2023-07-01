package org.acme.vehiclerouting.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

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

    public boolean isIdentical(Vehicle other) {
        if (this == other)
            return true;

        return depot.getId() == other.getDepot().getId() && capacity == other.getCapacity() && fixCost == other.getFixCost()
                && this.getCustomerIds().equals(other.getCustomerIds());
    }

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

    public List<Long> getCustomerIds() {
        return customerList.stream().map(Customer::getId).collect(Collectors.toList());
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

        List<Location> route = new ArrayList<>();

        route.add(depot.getLocation());
        route.addAll(customerList.stream().map(Customer::getLocation).collect(Collectors.toList()));
        route.add(depot.getLocation());

        return route;
    }

    public int getTotalDemand() {
        return customerList.stream().mapToInt(Customer::getDemand).sum();
    }

    public Customer getFirstDemandViolation() {
        int totalDemand = 0;
        for (Iterator<Customer> it = customerList.iterator(); it.hasNext();) {
            Customer customer = it.next();
            totalDemand += customer.getDemand();
            if (totalDemand > capacity)
                return customer;
        }
        return null;
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

        return getFirstServiceTimeViolation() != null;
    }

    public Customer getFirstServiceTimeViolation() {
        if (customerList.isEmpty()) {
            return null;
        }

        long currentTime = 0;
        Location previousLocation = depot.getLocation();

        for (Customer customer : customerList) {
            currentTime += previousLocation.getDistanceTo(customer.getLocation());
            if (currentTime > customer.getEndServiceWindow()) {
                return customer;
            }
            currentTime += Math.max(currentTime, customer.getBeginServiceWindow()) + customer.getServiceTime();
            previousLocation = customer.getLocation();
        }

        return null;
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
        return "Vehicle{" + "id=" + id + '}';
    }
}
