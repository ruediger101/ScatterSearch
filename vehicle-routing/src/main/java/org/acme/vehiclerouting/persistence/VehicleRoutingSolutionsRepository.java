package org.acme.vehiclerouting.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.acme.vehiclerouting.domain.Customer;
import org.acme.vehiclerouting.domain.Vehicle;
import org.acme.vehiclerouting.domain.VehicleRoutingSolution;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class VehicleRoutingSolutionsRepository {

    private int time = 0;

    private int refSetSize = 20;
    private List<Solution> vehicleRoutingSolutions = new ArrayList<>();

    public void add(VehicleRoutingSolution vehicleRoutingSolution) {
        this.vehicleRoutingSolutions.add(new Solution(vehicleRoutingSolution, time));
    }

    public void addAll(List<VehicleRoutingSolution> vehicleRoutingSolutions) {
        vehicleRoutingSolutions.forEach(this::add);
    }

    public void incrementTime() {
        time++;
    }

    public int getTime() {
        return time;
    }

    public class DistanceSolutionTuple {
        private int distance = 0; // used to store distance to referenceSet during selection
        private Solution solution;

        DistanceSolutionTuple(Solution solution, Collection<Solution> referenceSet) {
            this.solution = solution;
            this.distance = referenceSet.stream().mapToInt(this.solution::noCommonArcs).min().orElse(0);
        }

        public void updateDistance(Solution otherSolution) {
            distance = Math.min(distance, solution.noCommonArcs(otherSolution));
        }
    }

    public void createRefSet() {
        sortVehicleRoutingSolutions();

        List<Solution> refSet = vehicleRoutingSolutions.subList(0, refSetSize / 2);

        List<DistanceSolutionTuple> diverseCandidates = vehicleRoutingSolutions.subList(refSetSize / 2, vehicleRoutingSolutions.size()).stream()
                .map(s -> new DistanceSolutionTuple(s, refSet)).sorted((i, j) -> Integer.compare(j.distance, i.distance)).collect(Collectors.toList());

        while (refSet.size() < refSetSize && !diverseCandidates.isEmpty()) {
            DistanceSolutionTuple removed = diverseCandidates.remove(0);
            refSet.add(removed.solution);
            diverseCandidates.forEach(c -> c.updateDistance(removed.solution));
        }

        vehicleRoutingSolutions = refSet;
    }

    public void updateRefSet() {
        sortVehicleRoutingSolutions();

        vehicleRoutingSolutions = vehicleRoutingSolutions.subList(0, refSetSize);
    }

    private void sortVehicleRoutingSolutions() {
        vehicleRoutingSolutions.sort((i, j) -> {
            long s1 = i.getVehicleRoutingSolution().getScore() == null ? Long.MIN_VALUE : i.getVehicleRoutingSolution().getScore().hardScore();
            long s2 = j.getVehicleRoutingSolution().getScore() == null ? Long.MIN_VALUE : j.getVehicleRoutingSolution().getScore().hardScore();
            int result = Long.compare(s2, s1);
            if (result == 0) {
                s1 = i.getVehicleRoutingSolution().getScore() == null ? Long.MIN_VALUE : i.getVehicleRoutingSolution().getScore().softScore();
                s2 = j.getVehicleRoutingSolution().getScore() == null ? Long.MIN_VALUE : j.getVehicleRoutingSolution().getScore().softScore();
                result = Long.compare(s2, s1);
                if (result == 0) {
                    result = Integer.compare(i.getLastUpdate(), j.getLastUpdate());
                }
            }
            return result;
        });
    }

    private Set<Set<Solution>> getSubSets() {
        sortVehicleRoutingSolutions();
        Set<Set<Solution>> twoSolutionSets = new HashSet<>();
        Set<Solution> solutionSet = new HashSet<>(vehicleRoutingSolutions);

        for (Iterator<Solution> it1 = solutionSet.iterator(); it1.hasNext(); it1 = solutionSet.iterator()) {
            Solution s1 = it1.next();
            it1.remove();

            for (Iterator<Solution> it2 = solutionSet.iterator(); it2.hasNext();) {
                Solution s2 = it2.next();
                if (s1.getLastUpdate() < time || s2.getLastUpdate() < time) {
                    Set<Solution> subSet = new HashSet<>();
                    subSet.add(s1);
                    subSet.add(s2);
                    twoSolutionSets.add(subSet);
                }
            }
        }

        Set<Set<Solution>> threeSolutionSets = new HashSet<>();
        twoSolutionSets.forEach(set -> {
            Set<Solution> tempSet = new HashSet<>(set);
            tempSet.add(vehicleRoutingSolutions.stream().filter(solution -> !set.contains(solution)).findFirst().orElse(null));
            threeSolutionSets.add(tempSet);
        });

        Set<Set<Solution>> fourSolutionSets = new HashSet<>();
        threeSolutionSets.forEach(set -> {
            Set<Solution> tempSet = new HashSet<>(set);
            tempSet.add(vehicleRoutingSolutions.stream().filter(solution -> !set.contains(solution)).findFirst().orElse(null));
            fourSolutionSets.add(tempSet);
        });

        Set<Set<Solution>> bestSolutions = new HashSet<>();
        for (int i = 5; i <= vehicleRoutingSolutions.size(); i++) {
            bestSolutions.add(vehicleRoutingSolutions.subList(0, i).stream().collect(Collectors.toSet()));
        }

        Set<Set<Solution>> finalSet = new HashSet<>(twoSolutionSets);
        finalSet.addAll(threeSolutionSets);
        finalSet.addAll(fourSolutionSets);

        return finalSet;
    }

    public Set<Solution> combineSolutions(Set<Set<Solution>> subSets) {
        Set<Solution> combinedSolutions = new HashSet<>();

        subSets.stream().forEach(set -> {
            String name = set.stream().map(s -> s.getVehicleRoutingSolution().getName()).collect(Collectors.joining(")+(", "(", ")"));
            Solution newSolution = new Solution(new VehicleRoutingSolution(name, set.iterator().next().getVehicleRoutingSolution()), time);
            newSolution.getVehicleRoutingSolution().getVehicleList().forEach(v -> v.getCustomerList().clear());
            combinedSolutions.add(newSolution);

            Map<Solution, Long> solutionDistanceMap = set.stream()
                    .collect(Collectors.toMap(Function.identity(), solution -> solution.getVehicleRoutingSolution().getDistanceMeters()));
            double summedDistance = solutionDistanceMap.values().stream().mapToLong(Long::longValue).sum();

            Map<Solution, Double> intermediateSolutionValues = solutionDistanceMap.entrySet().stream()
                    .collect(Collectors.toMap(Entry::getKey, entry -> summedDistance / entry.getValue()));
            double summedIntermediateSolutionValues = intermediateSolutionValues.values().stream().mapToDouble(Double::doubleValue).sum();

            Map<Solution, Double> solutionValues = intermediateSolutionValues.entrySet().stream()
                    .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue() / summedIntermediateSolutionValues));

            double threshold;
            switch (set.size()) {
            case 2:
                threshold = 1.0;
                break;
            case 3:
                threshold = 0.85;
                break;
            case 4:
                threshold = 0.85;
                break;
            default:
                threshold = 0.8;
            }

            List<Entry<Customer, Customer>> weightedFilteredSortedArcs = solutionValues.entrySet().stream()
                    .map(e -> e.getKey().getArcs().entrySet().stream().collect(Collectors.toMap(Function.identity(), v -> e.getValue())))
                    .flatMap(m -> m.entrySet().stream()).collect(Collectors.toMap(Entry::getKey, Entry::getValue, Double::sum)).entrySet().stream()
                    .filter(e -> e.getValue() >= threshold).sorted((i, j) -> Double.compare(j.getValue(), i.getValue())).map(Entry::getKey)
                    .collect(Collectors.toList());

            Set<List<Customer>> newRoutes = new HashSet<>();

            while (!weightedFilteredSortedArcs.isEmpty()) {
                List<Customer> newRoute = new ArrayList<>();
                // start by adding best arc
                Entry<Customer, Customer> addedArc = weightedFilteredSortedArcs.remove(0);
                newRoute.add(addedArc.getKey());
                newRoute.add(addedArc.getValue());

                boolean expandRoute = true;
                while (expandRoute) {
                    expandRoute = false;

                    // remove entries with identical heads or trails
                    for (Iterator<Entry<Customer, Customer>> it = weightedFilteredSortedArcs.iterator(); it.hasNext();) {
                        Entry<Customer, Customer> arc = it.next();
                        if (Objects.equals(arc.getKey(), addedArc.getKey()) || Objects.equals(arc.getValue(), addedArc.getValue())) {
                            it.remove();
                        }
                    }

                    Customer routeStart = newRoute.get(0);
                    Customer routeEnd = newRoute.get(newRoute.size() - 1);

                    for (Iterator<Entry<Customer, Customer>> it = weightedFilteredSortedArcs.iterator(); it.hasNext();) {
                        Entry<Customer, Customer> arc = it.next();
                        if (routeStart != null && routeStart.equals(arc.getValue()) && !newRoute.contains(arc.getKey())) {
                            newRoute.add(0, arc.getKey());
                        } else if (routeEnd != null && routeEnd.equals(arc.getKey()) && !newRoute.contains(arc.getValue())) {
                            newRoute.add(arc.getValue());
                        } else {
                            continue;
                        }

                        addedArc = arc;
                        it.remove();
                        expandRoute = true;
                        break;
                    }
                }
                // remove route from and to depot if added.
                newRoute.removeAll(null);
                newRoutes.add(newRoute);
            }

            // check and restore route feasibility
            Iterator<Vehicle> unusedVehicles = new ArrayList<>(newSolution.getVehicleRoutingSolution().getVehicleList()).iterator();
            List<Vehicle> usedVehicles = new ArrayList<>();
            List<Customer> unroutedCustomers = new ArrayList<>(newSolution.getVehicleRoutingSolution().getCustomerList());
            for (Iterator<List<Customer>> it = newRoutes.iterator(); it.hasNext(); it = newRoutes.iterator()) {
                Vehicle vehicle = unusedVehicles.next();
                unusedVehicles.remove();
                usedVehicles.add(vehicle);

                List<Customer> newRoute = it.next();
                it.remove();

                vehicle.getCustomerList().addAll(newRoute);
                int truncatePosition = newRoute.size();
                Customer firstDemandViolation = vehicle.getFirstDemandViolation();
                if (firstDemandViolation != null) {
                    truncatePosition = newRoute.indexOf(firstDemandViolation);
                }
                Customer firstServiceTimeViolation = vehicle.getFirstServiceTimeViolation();
                if (firstServiceTimeViolation != null) {
                    truncatePosition = Math.min(truncatePosition, newRoute.indexOf(firstServiceTimeViolation));
                }

                if (truncatePosition < newRoute.size()) {
                    vehicle.getCustomerList().clear();
                    List<Customer> newFeasibleRoute = newRoute.subList(0, truncatePosition);
                    unroutedCustomers.removeAll(newFeasibleRoute);
                    vehicle.getCustomerList().addAll(newFeasibleRoute);
                    newRoutes.add(newRoute.subList(truncatePosition, newRoute.size()));
                }

            }

            // add unrouted customers

            for (Iterator<Customer> it = unroutedCustomers.iterator(); it.hasNext();) {
                Customer customer = it.next();

                long bestDeltaDistance = Long.MAX_VALUE;
                Vehicle bestVehicle = null;
                List<Customer> bestSequence = null;

                for (Iterator<Vehicle> it2 = usedVehicles.stream().filter(v -> customer.getDemand() <= (v.getCapacity() - v.getTotalDemand())).iterator(); it2
                        .hasNext();) {
                    Vehicle vehicle = it2.next();
                    long currentDistance = vehicle.getTotalDistanceMeters();

                    vehicle.getCustomerList().add(0, customer);

                    long deltaDistance = vehicle.getTotalDistanceMeters() - currentDistance;

                    if (!vehicle.isServiceTimeViolated() && deltaDistance < bestDeltaDistance) {
                        bestDeltaDistance = deltaDistance;
                        bestVehicle = vehicle;
                        bestSequence = List.copyOf(vehicle.getCustomerList());
                    }

                    for (int i = 0; i < vehicle.getCustomerList().size() - 1; i++) {
                        Collections.swap(vehicle.getCustomerList(), i, i + 1);
                        deltaDistance = vehicle.getTotalDistanceMeters() - currentDistance;
                        if (!vehicle.isServiceTimeViolated() && deltaDistance < bestDeltaDistance) {
                            bestDeltaDistance = vehicle.getTotalDistanceMeters() - currentDistance;
                            bestVehicle = vehicle;
                            bestSequence = List.copyOf(vehicle.getCustomerList());
                        }
                    }
                    vehicle.getCustomerList().remove(customer);
                }

                if (bestVehicle != null) {
                    bestVehicle.getCustomerList().clear();
                    bestVehicle.getCustomerList().addAll(bestSequence);
                } else {
                    Vehicle newRoute = unusedVehicles.next();
                    newRoute.getCustomerList().add(customer);
                    usedVehicles.add(newRoute);
                }
            }

        });

        return combinedSolutions;
    }

    public Set<Solution> generateNewSolutions() {
        time++;
        return combineSolutions(getSubSets());
    }

    public List<Solution> getSolutions() {
        return vehicleRoutingSolutions;
    }

    public Optional<Solution> getBestSolution() {
        return vehicleRoutingSolutions.stream().sorted((i, j) -> {
            long s1 = i.getVehicleRoutingSolution().getScore() == null ? Long.MIN_VALUE : i.getVehicleRoutingSolution().getScore().hardScore();
            long s2 = j.getVehicleRoutingSolution().getScore() == null ? Long.MIN_VALUE : j.getVehicleRoutingSolution().getScore().hardScore();
            int result = Long.compare(s2, s1);
            if (result == 0) {
                s1 = i.getVehicleRoutingSolution().getScore() == null ? Long.MIN_VALUE : i.getVehicleRoutingSolution().getScore().softScore();
                s2 = j.getVehicleRoutingSolution().getScore() == null ? Long.MIN_VALUE : j.getVehicleRoutingSolution().getScore().softScore();
                result = Long.compare(s2, s1);
                if (result == 0) {
                    result = Integer.compare(i.getLastUpdate(), j.getLastUpdate());
                }
            }
            return result;
        }).findFirst();
    }

    public List<Solution> getNotOptimizedSolutions() {
        return vehicleRoutingSolutions.stream().filter(s -> s.getLastUpdate() == this.time).collect(Collectors.toList());
    }
}
