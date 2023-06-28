package org.acme.vehiclerouting.persistence;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.acme.vehiclerouting.domain.Customer;
import org.acme.vehiclerouting.domain.Vehicle;
import org.acme.vehiclerouting.domain.VehicleRoutingSolution;

public class Solution {
    private long id;
    private int lastUpdate;
    private VehicleRoutingSolution vrs;
    private Map<Customer, Customer> arcs = new HashMap<>();
    private boolean arcUpdateRequired;

    private static final AtomicLong sequence = new AtomicLong();

    public Solution(VehicleRoutingSolution vrs, int time) {
        this.lastUpdate = time;
        this.vrs = vrs;
        this.arcUpdateRequired = true;
        this.id = sequence.incrementAndGet();
    }

    public void updateArcLists() {
        this.vrs.getVehicleList().stream().map(Vehicle::getCustomerList).filter(l -> !l.isEmpty()).forEach(l -> {
            arcs.put(null, l.get(0));
            for (int i = 0; i < l.size() - 1; i++) {
                arcs.put(l.get(i), l.get(i + 1));
            }
            arcs.put(l.get(l.size() - 1), null);
        });
        this.arcUpdateRequired = false;
    }

    public void setVehicleRoutingSolution(VehicleRoutingSolution vrs) {
        this.vrs = vrs;
        this.arcUpdateRequired = true;
    }

    public VehicleRoutingSolution getVehicleRoutingSolution() {
        return vrs;
    }

    public int noCommonArcs(Solution b) {
        if (arcUpdateRequired)
            updateArcLists();

        Set<Entry<Customer, Customer>> entries = arcs.entrySet();

        entries.retainAll(b.getArcs().entrySet());
        return entries.size();
    }

    public Map<Customer, Customer> getArcs() {
        if (arcUpdateRequired)
            updateArcLists();

        return arcs;
    }

    public int getLastUpdate() {
        return lastUpdate;
    }

    public long getId() {
        return id;
    }
}
