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

    private int iteration = 0;
    private boolean initialPopulation = true;

    private int refSetSize = 20;
    private List<Solution> vehicleRoutingSolutions = new ArrayList<>();

    private Set<Integer> checkedRoutes = new HashSet<>();

    private double initialRefSetDivisionFactor = 2.0;
    private double refSetDivisionFactor = 1.0;

    public boolean isIntialPopulation() {
        return initialPopulation && iteration == 0;
    }

    public void add(VehicleRoutingSolution vehicleRoutingSolution) {
        int hashCode = vehicleRoutingSolution.getVehicleList().stream().map(Vehicle::getCustomerList).filter(l -> !l.isEmpty()).collect(Collectors.toSet())
                .hashCode();
        if (!checkedRoutes.contains(hashCode)) {
            this.vehicleRoutingSolutions.add(new Solution(vehicleRoutingSolution, iteration));
            checkedRoutes.add(hashCode);
        }
    }

    public void addAll(List<VehicleRoutingSolution> vehicleRoutingSolutions) {
        vehicleRoutingSolutions.forEach(this::add);
    }

    public void incrementIteration() {
        iteration++;
    }

    public int getIteration() {
        return iteration;
    }

    public void setInitialRefSetDivisionFactor(double factor) {
        this.initialRefSetDivisionFactor = factor;
    }

    public double getInitialRefSetDivisionFactor() {
        return this.initialRefSetDivisionFactor;
    }

    public void setRefSetDivisionFactor(double factor) {
        this.refSetDivisionFactor = factor;
    }

    public double getRefSetDivisionFactor() {
        return this.refSetDivisionFactor;
    }

    public void setRefSetSize(int refSetSize) {
        this.refSetSize = refSetSize;
    }

    public int getRefSetSize() {
        return this.refSetSize;
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

    private void removeDuplicateSolutions() {
        Iterator<Solution> iterator = vehicleRoutingSolutions.iterator();

        Solution previousSolution = iterator.next();

        while (iterator.hasNext()) {
            Solution next = iterator.next();
            if (next.getVehicleRoutingSolution().getScore().equals(previousSolution.getVehicleRoutingSolution().getScore())) {
                iterator.remove();
            }
        }

    }

    public void updateRefSet() {
        sortVehicleRoutingSolutions();
        removeDuplicateSolutions();
        double divisionFactor = initialPopulation ? initialRefSetDivisionFactor : refSetDivisionFactor;

        if (divisionFactor == 1.0) {
            vehicleRoutingSolutions = vehicleRoutingSolutions.subList(0, refSetSize);
        } else {

            List<Solution> refSet = vehicleRoutingSolutions.subList(0, (int) (refSetSize / divisionFactor));

            List<DistanceSolutionTuple> diverseCandidates = vehicleRoutingSolutions.subList((int) (refSetSize / divisionFactor), vehicleRoutingSolutions.size())
                    .stream().map(s -> new DistanceSolutionTuple(s, refSet)).sorted((i, j) -> Integer.compare(j.distance, i.distance))
                    .collect(Collectors.toList());

            while (refSet.size() < refSetSize && !diverseCandidates.isEmpty()) {
                DistanceSolutionTuple removed = diverseCandidates.remove(0);
                refSet.add(removed.solution);
                diverseCandidates.forEach(c -> c.updateDistance(removed.solution));
            }

            vehicleRoutingSolutions = refSet;
        }
        initialPopulation = false;
    }

    private void sortVehicleRoutingSolutions() {
        vehicleRoutingSolutions.sort((i, j) -> j.getVehicleRoutingSolution().getScore().compareTo(i.getVehicleRoutingSolution().getScore()));
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
                if (s1.getLastUpdate() < iteration || s2.getLastUpdate() < iteration) {
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
        Set<Set<List<Customer>>> allNewRoutes = new HashSet<>();

        subSets.stream().forEach(set -> {
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

            Set<List<Customer>> newRoutes = combineArcs(weightedFilteredSortedArcs);

            if (!newRoutes.isEmpty() && !allNewRoutes.contains(newRoutes)) {
                allNewRoutes.add(newRoutes);
                String name = set.stream().map(s -> s.getVehicleRoutingSolution().getName()).collect(Collectors.joining(")+(", "(", ")"));
                Solution newSolution = new Solution(new VehicleRoutingSolution(name, set.iterator().next().getVehicleRoutingSolution(), false), iteration);
                checkAndRestoreFeasibility(newSolution, newRoutes);

                int hashCode = newSolution.getVehicleRoutingSolution().getVehicleList().stream().map(Vehicle::getCustomerList).filter(l -> !l.isEmpty())
                        .collect(Collectors.toSet()).hashCode();

                if (!checkedRoutes.contains(hashCode))
                    combinedSolutions.add(newSolution);
            }

        });

        return combinedSolutions;
    }

    private Set<List<Customer>> combineArcs(List<Entry<Customer, Customer>> weightedFilteredSortedArcs) {
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
            newRoute.removeIf(Objects::isNull);
            newRoutes.add(newRoute);
        }
        return newRoutes;
    }

    private void checkAndRestoreFeasibility(Solution newSolution, Set<List<Customer>> newRoutes) {
        Iterator<Vehicle> unusedVehicles = new ArrayList<>(newSolution.getVehicleRoutingSolution().getVehicleList()).iterator();
        List<Vehicle> usedVehicles = new ArrayList<>();
        List<Customer> unroutedCustomers = new ArrayList<>(newSolution.getVehicleRoutingSolution().getCustomerList());
        Set<List<Customer>> tempRoutes = new HashSet<>(newRoutes);
        for (Iterator<List<Customer>> it = tempRoutes.iterator(); it.hasNext(); it = tempRoutes.iterator()) {
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
                tempRoutes.add(newRoute.subList(truncatePosition, newRoute.size()));
            } else {
                unroutedCustomers.removeAll(newRoute);
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
    }

    public Set<Solution> generateNewSolutions() {
        Set<Solution> newSolutions = combineSolutions(getSubSets());
        vehicleRoutingSolutions.addAll(newSolutions);
        return newSolutions;
    }

    public List<Solution> getSolutions() {
        return vehicleRoutingSolutions;
    }

    public Optional<Solution> getBestSolution() {
        boolean useScore = vehicleRoutingSolutions.stream().allMatch(s -> s.getVehicleRoutingSolution().getScore() != null);
        return vehicleRoutingSolutions.stream().sorted((i, j) -> {
            if (useScore)
                return j.getVehicleRoutingSolution().getScore().compareTo(i.getVehicleRoutingSolution().getScore());
            else {
                int result = Long.compare(
                        i.getVehicleRoutingSolution().getDistanceMeters()
                                + i.getVehicleRoutingSolution().getUsedVehicleList().stream().mapToInt(Vehicle::getFixCost).sum(),
                        j.getVehicleRoutingSolution().getDistanceMeters()
                                + j.getVehicleRoutingSolution().getUsedVehicleList().stream().mapToInt(Vehicle::getFixCost).sum());
                if (result == 0)
                    result = Long.compare(i.getId(), j.getId());
                return result;
            }
        }).findFirst();
    }

    public List<Solution> getNotOptimizedSolutions() {
        return vehicleRoutingSolutions.stream().filter(s -> s.getLastUpdate() == this.iteration).collect(Collectors.toList());
    }
}
