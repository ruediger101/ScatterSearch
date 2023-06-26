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
public class VehicleRoutingSolutionsRepository {
    public class Solution {
        private long id;
        private int lastUpdate;
        private VehicleRoutingSolution vrs;
        private Map<Customer, Customer> arcs = new HashMap<>();
        private Set<Customer> routeStart = new HashSet<>();
        private Set<Customer> routeEnd = new HashSet<>();

        Solution(VehicleRoutingSolution s, int time) {
            this.lastUpdate = time;
            this.updateSolution(s);

        }

        public void updateSolution(VehicleRoutingSolution s) {
            this.vrs = s;
            s.getVehicleList().stream().map(Vehicle::getCustomerList).filter(l -> !l.isEmpty()).forEach(l -> {
                routeStart.add(l.get(0));
                routeEnd.add(l.get(l.size() - 1));
                for (int i = 0; i < l.size() - 1; i++) {
                    arcs.put(l.get(i), l.get(i + 1));
                }
            });
        }

        public VehicleRoutingSolution getVehicleRoutingSolution() {
            return vrs;
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
    private List<Solution> vehicleRoutingSolutions = new ArrayList<>();

    public void add(VehicleRoutingSolution vehicleRoutingSolution) {
        this.vehicleRoutingSolutions.add(new Solution(vehicleRoutingSolution, time));
    }

    public void addAll(List<VehicleRoutingSolution> vehicleRoutingSolutions) {
        vehicleRoutingSolutions.forEach(this::add);
    }

    public void updateRefSet() {
        vehicleRoutingSolutions.sort((i, j) -> {
            long s1 = i.vrs.getScore() == null ? Long.MIN_VALUE : i.vrs.getScore().hardScore();
            long s2 = j.vrs.getScore() == null ? Long.MIN_VALUE : j.vrs.getScore().hardScore();
            int result = Long.compare(s2, s1);
            if (result == 0) {
                s1 = i.vrs.getScore() == null ? Long.MIN_VALUE : i.vrs.getScore().softScore();
                s2 = j.vrs.getScore() == null ? Long.MIN_VALUE : j.vrs.getScore().softScore();
                result = Long.compare(s2, s1);
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

    public List<Solution> getSolutions() {
        return vehicleRoutingSolutions;
    }

    public Optional<VehicleRoutingSolution> getBestSolution() {
        return vehicleRoutingSolutions.stream().sorted((i, j) -> {
            long s1 = i.vrs.getScore() == null ? Long.MIN_VALUE : i.vrs.getScore().hardScore();
            long s2 = j.vrs.getScore() == null ? Long.MIN_VALUE : j.vrs.getScore().hardScore();
            int result = Long.compare(s2, s1);
            if (result == 0) {
                s1 = i.vrs.getScore() == null ? Long.MIN_VALUE : i.vrs.getScore().softScore();
                s2 = j.vrs.getScore() == null ? Long.MIN_VALUE : j.vrs.getScore().softScore();
                result = Long.compare(s2, s1);
                if (result == 0) {
                    result = Integer.compare(i.lastUpdate, j.lastUpdate);
                }
            }
            return result;
        }).findFirst().map(s -> s.vrs);
    }
}
