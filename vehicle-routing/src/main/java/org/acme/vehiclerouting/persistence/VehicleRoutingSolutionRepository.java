package org.acme.vehiclerouting.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.acme.vehiclerouting.domain.Customer;
import org.acme.vehiclerouting.domain.Vehicle;
import org.acme.vehiclerouting.domain.VehicleRoutingSolution;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class VehicleRoutingSolutionRepository {
    private class Solution {
        private int lastUpdate;
        private VehicleRoutingSolution vrs;
        private Map<Customer, Customer> arcs = new HashMap<>();
        private Set<Customer> routeStart = new HashSet<>();
        private Set<Customer> routeEnd = new HashSet<>();

        Solution(VehicleRoutingSolution s, int time) {
            this.vrs = s;
            this.lastUpdate = time;
            s.getVehicleList().stream().map(Vehicle::getCustomerList).filter(l -> !l.isEmpty()).forEach(l -> {
                routeStart.add(l.get(0));
                routeEnd.add(l.get(l.size() - 1));
                for (int i = 0; i < l.size() - 1; i++) {
                    arcs.put(l.get(i), l.get(i + 1));
                }
            });

        }

        public int noCommonArcs(Solution b) {
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
    }

    private class SolutionDistance {
        private int distance = 0; // used to store distance to refset during selection
        private Solution solution;

        SolutionDistance(Solution solution, Collection<Solution> refSet) {
            this.solution = solution;
            this.distance = refSet.stream().mapToInt(this.solution::noCommonArcs).min().orElse(Integer.MAX_VALUE);
        }

        private void updateDistance(Solution otherSolution) {
            distance = Math.min(distance, solution.noCommonArcs(otherSolution));
        }
    }

    private int time = 0;

    private int refSetSize = 20;
    private VehicleRoutingSolution currentSolution = null;
    private List<Solution> vehicleRoutingSolutions = new ArrayList<>();

    public Optional<VehicleRoutingSolution> getCurrentSolution() {
        return Optional.ofNullable(currentSolution);
    }

    public void add(VehicleRoutingSolution vehicleRoutingSolution) {
        // vehicleRoutingSolution.getVehicleList().sort((i, j) -> {
        //     int result = Boolean.compare(i.getCustomerList().isEmpty(), j.getCustomerList().isEmpty());
        //     if (result == 0) {
        //         result = Long.compare(i.getId(), j.getId());
        //     }
        //     return result;
        // });

        this.vehicleRoutingSolutions.add(new Solution(vehicleRoutingSolution, time));
        if (currentSolution == null) {
            currentSolution = vehicleRoutingSolution;
        }
    }

    public void addAll(List<VehicleRoutingSolution> vehicleRoutingSolutions) {
        vehicleRoutingSolutions.forEach(this::add);
    }

    public void setCurrentSolution(VehicleRoutingSolution vehicleRoutingSolution) {
        currentSolution = vehicleRoutingSolution;
    }

    public void updateRefSet() {
        vehicleRoutingSolutions.sort((i, j) -> {
            int result = Long.compare(j.vrs.getScore().hardScore(), i.vrs.getScore().hardScore());
            if (result == 0) {
                result = Long.compare(j.vrs.getScore().softScore(), i.vrs.getScore().softScore());
                if (result == 0) {
                    result = Integer.compare(i.lastUpdate, j.lastUpdate);
                }
            }
            return result;
        });

        List<Solution> refSet = vehicleRoutingSolutions.subList(0, refSetSize / 2);

        List<SolutionDistance> diverseCandidates = vehicleRoutingSolutions.subList(refSetSize / 2,
                vehicleRoutingSolutions.size()).stream().map(l -> new SolutionDistance(l, refSet))
                .sorted((i, j) -> Integer.compare(j.distance, i.distance))
                .collect(Collectors.toList());

        while (refSet.size() < refSetSize && !diverseCandidates.isEmpty()) {
            SolutionDistance removed = diverseCandidates.remove(0);
            refSet.add(removed.solution);
            diverseCandidates.forEach(c -> c.updateDistance(removed.solution));
        }

        vehicleRoutingSolutions = refSet;
    }

    public Set<Set<VehicleRoutingSolution>> getSubSets() {
        // increase time to identify all entries changed from now
        time++;

        Set<Set<VehicleRoutingSolution>> setOfSets = new HashSet<>();
        Set<Solution> solutionSet = new HashSet<>(vehicleRoutingSolutions);

        for (Iterator<Solution> it1 = solutionSet.iterator(); it1.hasNext(); it1 = solutionSet.iterator()) {
            Solution s1 = it1.next();
            it1.remove();

            for (Iterator<Solution> it2 = solutionSet.iterator(); it2.hasNext();) {
                Solution s2 = it2.next();
                if (s1.lastUpdate < time || s2.lastUpdate < time) {
                    Set<VehicleRoutingSolution> subSet = new HashSet<>();
                    subSet.add(s1.vrs);
                    subSet.add(s2.vrs);
                    setOfSets.add(subSet);
                }
            }
        }

        return setOfSets;
    }

}
