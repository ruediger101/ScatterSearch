package org.acme.vehiclerouting.persistence;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
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
    private Set<Customer> routeStart = new HashSet<>();
    private Set<Customer> routeEnd = new HashSet<>();
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
            routeStart.add(l.get(0));
            routeEnd.add(l.get(l.size() - 1));
            for (int i = 0; i < l.size() - 1; i++) {
                arcs.put(l.get(i), l.get(i + 1));
            }
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

        Set<Customer> start = new HashSet<>(routeStart);
        start.retainAll(b.routeStart);
        Set<Customer> end = new HashSet<>(routeEnd);
        end.retainAll(b.routeEnd);

        int noArcs = start.size() + end.size();

        for (Iterator<Entry<Customer, Customer>> it = arcs.entrySet().iterator(); it.hasNext();) {
            Entry<Customer, Customer> next = it.next();
            if (Objects.equals(next.getValue(), b.arcs.get(next.getKey())))
                noArcs++;
        }

        return noArcs;
    }

    public int getLastUpdate() {
        return lastUpdate;
    }

    public long getId() {
        return id;
    }
}
