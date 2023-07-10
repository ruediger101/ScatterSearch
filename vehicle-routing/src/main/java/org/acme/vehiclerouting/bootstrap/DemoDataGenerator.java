package org.acme.vehiclerouting.bootstrap;

import java.util.List;

import org.acme.vehiclerouting.domain.Customer;
import org.acme.vehiclerouting.domain.Depot;
import org.acme.vehiclerouting.domain.Location;
import org.acme.vehiclerouting.domain.VehicleRoutingSolution;
import org.acme.vehiclerouting.persistence.VehicleRoutingSolutionsRepository;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class DemoDataGenerator {

    private final VehicleRoutingSolutionsRepository repository;

    public DemoDataGenerator(VehicleRoutingSolutionsRepository repository) {
        this.repository = repository;
    }

    public void generateDemoData(@Observes StartupEvent startupEvent) {
        VehicleRoutingSolution problem = DemoDataBuilder.builder()
                .setMinDemand(1)
                .setMaxDemand(5)
                .setMinServiceTime(50)
                .setMaxServiceTime(500)
                .setMinServiceWindow(5000)
                .setMaxServiceWindow(40000)
                .setVehicleCapacity(25)
                .setCustomerCount(77)
                .setVehicleCount(50)
                .setDepotCount(1)
                .setVehicleFixCost(10000)
                .setSouthWestCorner(new Location(0L, 49.43069, 11.03332))
                .setNorthEastCorner(new Location(0L, 49.49069, 11.13332))
                .build();
        
        // Depot d = problem.getDepotList().get(0);
        // List<Customer> customers = problem.getCustomerList();
        // customers.forEach(c-> System.err.println("D -> "+ c.getId()+": " + d.getLocation().getDistanceTo(c.getLocation())));
        // for(int i = 0; i < customers.size() -1 ; i++){
        //     for(int j = i+1; j < customers.size(); j++){
        //         System.err.println( customers.get(i).getId() +" -> "+ customers.get(j).getId()+": " + customers.get(i).getLocation().getDistanceTo(customers.get(j).getLocation()));
        //     }
        // }
        // customers.forEach(c-> System.err.println("ID: " + c.getId() + ", Demand: "+ c.getDemand()+", Sevice Time: "+c.getServiceTime()+ ", Service Window: " + c.getBeginServiceWindow()+"/"+c.getEndServiceWindow()));

        repository.addAll(SequentialInsertion.solve(problem, 1, false));
        repository.addAll(SequentialInsertion.solve(problem, 99, true));
    }
}
