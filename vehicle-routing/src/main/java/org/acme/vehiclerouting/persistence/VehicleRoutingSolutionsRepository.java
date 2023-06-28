package org.acme.vehiclerouting.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        private int distance = 0; // used to store distance to refset during selection
        private Solution solution;

        DistanceSolutionTuple(Solution solution, Collection<Solution> refSet) {
            this.solution = solution;
            this.distance = refSet.stream().mapToInt(this.solution::noCommonArcs).min().orElse(0);
        }

        public void updateDistance(Solution otherSolution) {
            distance = Math.min(distance, solution.noCommonArcs(otherSolution));
        }
    }

    public void createRefSet() {
        sortVehicleRoutingSolutions();

        List<Solution> refSet = vehicleRoutingSolutions.subList(0, refSetSize / 2);

        List<DistanceSolutionTuple> diverseCandidates = vehicleRoutingSolutions
                .subList(refSetSize / 2, vehicleRoutingSolutions.size()).stream()
                .map(s -> new DistanceSolutionTuple(s, refSet))
                .sorted((i, j) -> Integer.compare(j.distance, i.distance)).collect(Collectors.toList());

        while (refSet.size() < refSetSize && !diverseCandidates.isEmpty()) {
            DistanceSolutionTuple removed = diverseCandidates.remove(0);
            refSet.add(removed.solution);
            diverseCandidates.forEach(c -> c.updateDistance(removed.solution));
        }

        vehicleRoutingSolutions = refSet;
    }

    public void updateRefSet() {
        sortVehicleRoutingSolutions();

        List<Solution> refSet = vehicleRoutingSolutions.subList(0, refSetSize);
    }

    private void sortVehicleRoutingSolutions() {
        vehicleRoutingSolutions.sort((i, j) -> {
            long s1 = i.getVehicleRoutingSolution().getScore() == null ? Long.MIN_VALUE
                    : i.getVehicleRoutingSolution().getScore().hardScore();
            long s2 = j.getVehicleRoutingSolution().getScore() == null ? Long.MIN_VALUE
                    : j.getVehicleRoutingSolution().getScore().hardScore();
            int result = Long.compare(s2, s1);
            if (result == 0) {
                s1 = i.getVehicleRoutingSolution().getScore() == null ? Long.MIN_VALUE
                        : i.getVehicleRoutingSolution().getScore().softScore();
                s2 = j.getVehicleRoutingSolution().getScore() == null ? Long.MIN_VALUE
                        : j.getVehicleRoutingSolution().getScore().softScore();
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
            tempSet.add(vehicleRoutingSolutions.stream().filter(solution -> !set.contains(solution)).findFirst()
                    .orElse(null));
            threeSolutionSets.add(tempSet);
        });

        Set<Set<Solution>> fourSolutionSets = new HashSet<>();
        threeSolutionSets.forEach(set -> {
            Set<Solution> tempSet = new HashSet<>(set);
            tempSet.add(vehicleRoutingSolutions.stream()
                    .filter(solution -> !set.contains(solution)).findFirst().orElse(null));
            fourSolutionSets.add(tempSet);
        });

        Set<Set<Solution>> bestIsolutions = new HashSet<>();
        for (int i = 5; i <= vehicleRoutingSolutions.size(); i++) {
            bestIsolutions.add(vehicleRoutingSolutions.subList(0, i).stream()
                    .collect(Collectors.toSet()));
        }

        Set<Set<Solution>> finalSet = new HashSet<>(twoSolutionSets);
        finalSet.addAll(threeSolutionSets);
        finalSet.addAll(fourSolutionSets);

        return finalSet;
    }

    public Set<Solution> combineSolutions(Set<Set<Solution>> subSets) {
        Set<Solution> combiniedSolutions = new HashSet<>();

        subSets.stream().forEach(set -> {
            Map<Solution, Long> solutionDistanceMap = set.stream()
                    .collect(Collectors.toMap(Function.identity(),
                            solution -> solution.getVehicleRoutingSolution().getDistanceMeters()));
            double summedDistance = solutionDistanceMap.values().stream().mapToLong(Long::longValue).sum();

            Map<Solution, Double> intermediateSolutionValues = solutionDistanceMap.entrySet().stream()
                    .collect(Collectors.toMap(Entry::getKey, entry -> summedDistance / entry.getValue()));
            double summedIntermediateSolutionValues = intermediateSolutionValues.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .sum();

            Map<Solution, Double> solutionValues = intermediateSolutionValues.entrySet().stream()
                    .collect(Collectors.toMap(Entry::getKey,
                            entry -> entry.getValue() / summedIntermediateSolutionValues));

            //TODO calculate ARC weight

        });

        return combiniedSolutions;
    }

    public void generateNewSolutions() {
        time++;
        Set<Set<Solution>> subSets = getSubSets();
        // TODO combine solutions
    }

    public List<Solution> getSolutions() {
        return vehicleRoutingSolutions;
    }

    public Optional<Solution> getBestSolution() {
        return vehicleRoutingSolutions.stream().sorted((i, j) -> {
            long s1 = i.getVehicleRoutingSolution().getScore() == null ? Long.MIN_VALUE
                    : i.getVehicleRoutingSolution().getScore().hardScore();
            long s2 = j.getVehicleRoutingSolution().getScore() == null ? Long.MIN_VALUE
                    : j.getVehicleRoutingSolution().getScore().hardScore();
            int result = Long.compare(s2, s1);
            if (result == 0) {
                s1 = i.getVehicleRoutingSolution().getScore() == null ? Long.MIN_VALUE
                        : i.getVehicleRoutingSolution().getScore().softScore();
                s2 = j.getVehicleRoutingSolution().getScore() == null ? Long.MIN_VALUE
                        : j.getVehicleRoutingSolution().getScore().softScore();
                result = Long.compare(s2, s1);
                if (result == 0) {
                    result = Integer.compare(i.getLastUpdate(), j.getLastUpdate());
                }
            }
            return result;
        }).findFirst();
    }

    public List<Solution> getNotOptimizedSolutions() {
        return vehicleRoutingSolutions.stream().filter(s -> s.getLastUpdate() == this.time)
                .collect(Collectors.toList());
    }
}
