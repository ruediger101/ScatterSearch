package org.acme.vehiclerouting.bootstrap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.PrimitiveIterator.OfInt;
import java.util.stream.Collectors;

import org.acme.vehiclerouting.domain.Customer;
import org.acme.vehiclerouting.domain.Vehicle;
import org.acme.vehiclerouting.domain.VehicleRoutingSolution;

public class SequentialInsertion {
    private SequentialInsertion() {
    }

    public static List<VehicleRoutingSolution> solve(VehicleRoutingSolution solution, int numberSolutions) {
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

        List<Customer> sortedTwc = twcMap.entrySet().stream().filter(e -> e.getValue() > 0)
                .sorted((j, i) -> Integer.compare(i.getValue(), j.getValue())).map(Entry::getKey)
                .collect(Collectors.toList());
        List<Customer> sortedCompatibility = compatibilityMap.entrySet().stream()
                .filter(e -> !sortedTwc.contains(e.getKey())).sorted((i, j) -> Long.compare(i.getValue(), j.getValue()))
                .map(Entry::getKey).collect(Collectors.toList());

        // Start of new Solution
        VehicleRoutingSolution newSolution = new VehicleRoutingSolution(solution.getName() + "_1", solution);

        // list sorted by TWC and compatibility criterion
        List<Customer> unroutedCustomers = new ArrayList<>(sortedTwc);
        unroutedCustomers.addAll(sortedCompatibility);

        Random rand = new Random(System.currentTimeMillis());
        OfInt swap = rand.ints(0, 100).iterator();

        for (int i = 0; i < unroutedCustomers.size() - 1; i++) {
            for (int j = i + 1; j < unroutedCustomers.size(); j++) {
                if(swap.nextInt() > 90){
                    Collections.swap(unroutedCustomers, i, j);
                }
            }
        }

        Iterator<Vehicle> it = newSolution.getVehicleList().iterator();
        while (!unroutedCustomers.isEmpty() && it.hasNext()) {
            Vehicle vehicle = it.next();
            createNewRoute(unroutedCustomers, vehicle);
        }

        return List.of(newSolution);
    }

    private static long calcTwc(Customer c1, Customer c2) {
        long deltaTime = c1.getServiceTime() + c1.getLocation().getDistanceTo(c2.getLocation());
        long earliestArrival = c1.getBeginServiceWindow() + deltaTime;
        long latestArrival = c1.getEndServiceWindow() + deltaTime;

        if (earliestArrival < c2.getEndServiceWindow()) {
            return Math.min(latestArrival, c2.getEndServiceWindow())
                    - Math.max(earliestArrival, c2.getBeginServiceWindow());
        } else {
            return Long.MIN_VALUE;
        }
    }

    private static void createNewRoute(List<Customer> unroutedCustomers, Vehicle vehicle) {
        vehicle.getCustomerList().add(unroutedCustomers.remove(0));

        List<Customer> feasibleCustomers = unroutedCustomers.stream()
                .filter(c -> c.getDemand() <= (vehicle.getCapacity() - vehicle.getTotalDemand()))
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

                if (!vehicle.isServiceTimeViolated() && deltaDistance < bestDeltaDistance) {
                    bestDeltaDistance = deltaDistance;
                    bestCustomer = customer;
                    bestSequence = List.copyOf(vehicle.getCustomerList());
                }

                for (int i = 0; i < vehicle.getCustomerList().size() - 1; i++) {
                    Collections.swap(vehicle.getCustomerList(), i, i + 1);
                    deltaDistance = vehicle.getTotalDistanceMeters() - currentDistance;
                    if (!vehicle.isServiceTimeViolated() && deltaDistance < bestDeltaDistance) {
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

            unroutedCustomers.remove(bestCustomer);
            vehicle.getCustomerList().clear();
            vehicle.getCustomerList().addAll(bestSequence);
            feasibleCustomers = unroutedCustomers.stream()
                    .filter(c -> c.getDemand() <= (vehicle.getCapacity() - vehicle.getTotalDemand()))
                    .collect(Collectors.toList());
        }
    }

}
