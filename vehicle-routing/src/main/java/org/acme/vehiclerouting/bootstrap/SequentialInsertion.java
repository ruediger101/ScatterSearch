package org.acme.vehiclerouting.bootstrap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PrimitiveIterator.OfInt;
import java.util.Random;
import java.util.stream.Collectors;

import org.acme.vehiclerouting.domain.Customer;
import org.acme.vehiclerouting.domain.Vehicle;
import org.acme.vehiclerouting.domain.VehicleRoutingSolution;

public class SequentialInsertion {
    private SequentialInsertion() {
    }

    private static Random rand = new Random(42);
    private static OfInt randInt = rand.ints(1, 100).iterator();
    private static final int ACCEPTANCE_THRESHOLD = 10;

    public static VehicleRoutingSolution solve(VehicleRoutingSolution solution) {
        List<VehicleRoutingSolution> solutions = solve(solution, 1);
        return solutions.isEmpty() ? null : solutions.get(0);
    }

    public static List<VehicleRoutingSolution> solve(VehicleRoutingSolution solution, int numberSolutions) {
        return solve(solution, numberSolutions, false);
    }

    public static List<VehicleRoutingSolution> solve(VehicleRoutingSolution solution, int numberSolutions, boolean randomize) {
        if (solution == null)
            return Collections.emptyList();

        List<Customer> customerList = solution.getCustomerList();

        Map<Customer, Integer> twcMap = new HashMap<>();
        Map<Customer, Long> compatibilityMap = new HashMap<>();

        for (Iterator<Customer> it1 = customerList.iterator(); it1.hasNext();) {
            Customer a = it1.next();
            long compatibility = 0;
            int twc = 0; // violated constraints
            long aa = calcTwc(a, a);

            if (aa == Long.MIN_VALUE) {
                twc++;
            } else {
                compatibility += aa;
            }

            for (Iterator<Customer> it2 = customerList.iterator(); it2.hasNext();) {
                Customer b = it2.next();
                if (a != b) {
                    long ab = calcTwc(a, b);
                    if (ab == Long.MIN_VALUE) {
                        twc++;
                    } else {
                        compatibility += ab;
                    }
                    long ba = calcTwc(b, a);
                    if (ba == Long.MIN_VALUE) {
                        twc++;
                    } else {
                        compatibility += ba;
                    }
                }
            }
            twcMap.put(a, twc);
            compatibilityMap.put(a, compatibility);
        }

        // solution.getCustomerList()
        // .forEach(c -> System.err.println("Kunde: " + c.getId() + ", TWC: " +
        // twcMap.get(c) + ", Compatibility: " + compatibilityMap.get(c)));

        List<Customer> sortedTwc = twcMap.entrySet().stream().filter(e -> e.getValue() > 0).sorted((j, i) -> Integer.compare(i.getValue(), j.getValue()))
                .map(Entry::getKey).collect(Collectors.toList());
        List<Customer> sortedCompatibility = compatibilityMap.entrySet().stream().filter(e -> !sortedTwc.contains(e.getKey()))
                .sorted((i, j) -> Long.compare(i.getValue(), j.getValue())).map(Entry::getKey).collect(Collectors.toList());

        List<VehicleRoutingSolution> initalPopulation = new ArrayList<>();

        for (int i = 1; i <= numberSolutions; i++) {
            // Start of new Solution
            VehicleRoutingSolution newSolution = new VehicleRoutingSolution(solution.getName() + "_" + i, solution, false);

            // list sorted by TWC and compatibility criterion
            List<Customer> unroutedCustomers = new ArrayList<>(sortedTwc);
            unroutedCustomers.addAll(sortedCompatibility);

            Iterator<Vehicle> it = newSolution.getVehicleList().iterator();
            while (!unroutedCustomers.isEmpty() && it.hasNext()) {
                Vehicle vehicle = it.next();
                int index = randomize ? (int) (Math.exp(rand.nextDouble() * Math.log(unroutedCustomers.size())) - 1.0) : 0;
                // System.err.println("New Route with Customer: " + unroutedCustomers.get(index).getId());
                vehicle.getCustomerList().add(unroutedCustomers.remove(index));
                populateRoute(unroutedCustomers, vehicle, randomize);
                // System.err.println("Finished route: " + vehicle.getCustomerList().stream().map(Customer::getId).collect(Collectors.toList()));
            }

            initalPopulation.add(newSolution);
        }

        return initalPopulation;
    }

    private static long calcTwc(Customer c1, Customer c2) {
        long deltaTime = c1.getServiceTime() + c1.getLocation().getDistanceTo(c2.getLocation());
        long earliestArrival = c1.getBeginServiceWindow() + deltaTime;
        long latestArrival = c1.getEndServiceWindow() + deltaTime;

        if (earliestArrival < c2.getEndServiceWindow()) {
            return Math.min(latestArrival, c2.getEndServiceWindow()) - Math.max(earliestArrival, c2.getBeginServiceWindow());
        } else {
            return Long.MIN_VALUE;
        }
    }

    public static void populateRoute(List<Customer> unroutedCustomers, Vehicle vehicle, boolean randomize) {
        List<Customer> feasibleCustomers = unroutedCustomers.stream().filter(c -> c.getDemand() <= (vehicle.getCapacity() - vehicle.getTotalDemand()))
                .collect(Collectors.toList());

        while (!feasibleCustomers.isEmpty()) {
            long currentDistance = vehicle.getTotalDistanceMeters();
            List<Customer> currentSequence = List.copyOf(vehicle.getCustomerList());

            long bestDeltaDistance = Long.MAX_VALUE;
            Customer bestCustomer = null;
            List<Customer> bestSequence = null;

            for (Iterator<Customer> it = feasibleCustomers.iterator(); it.hasNext();) {
                Customer customer = it.next();

                vehicle.getCustomerList().clear();
                vehicle.getCustomerList().add(customer);
                vehicle.getCustomerList().addAll(currentSequence);

                long deltaDistance = vehicle.getTotalDistanceMeters() - currentDistance;

                if (!vehicle.isServiceTimeViolated() && deltaDistance < bestDeltaDistance && (!randomize || randInt.nextInt() > ACCEPTANCE_THRESHOLD)) {
                    bestDeltaDistance = deltaDistance;
                    bestCustomer = customer;
                    bestSequence = List.copyOf(vehicle.getCustomerList());
                }

                for (int i = 0; i < vehicle.getCustomerList().size() - 1; i++) {
                    Collections.swap(vehicle.getCustomerList(), i, i + 1);
                    deltaDistance = vehicle.getTotalDistanceMeters() - currentDistance;
                    if (!vehicle.isServiceTimeViolated() && deltaDistance < bestDeltaDistance && (!randomize || randInt.nextInt() > ACCEPTANCE_THRESHOLD)) {
                        bestDeltaDistance = vehicle.getTotalDistanceMeters() - currentDistance;
                        bestCustomer = customer;
                        bestSequence = List.copyOf(vehicle.getCustomerList());
                    }
                }
            }

            if (bestCustomer == null) {
                vehicle.getCustomerList().clear();
                vehicle.getCustomerList().addAll(currentSequence);
                // route could not be extended
                return;
            }
            // System.err.println("Enlarge Route with Customer: " + bestCustomer.getId());

            unroutedCustomers.remove(bestCustomer);
            vehicle.getCustomerList().clear();
            vehicle.getCustomerList().addAll(bestSequence);
            feasibleCustomers = unroutedCustomers.stream().filter(c -> c.getDemand() <= (vehicle.getCapacity() - vehicle.getTotalDemand()))
                    .collect(Collectors.toList());
        }
    }

}
