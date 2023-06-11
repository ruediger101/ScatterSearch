package org.acme.vehiclerouting.domain;

public class Customer {

    private long id;
    private Location location;
    private int demand;
    private int serviceTime;
    private int beginServiceWindow;
    private int endServiceWindow;

    public Customer() {
    }

    public Customer(long id, Location location, int demand, int serviceTime, int beginServiceWindow,
            int endServiceWindow) {
        this.id = id;
        this.location = location;
        this.demand = demand;
        this.serviceTime = serviceTime;
        if (endServiceWindow >= beginServiceWindow) {
            this.beginServiceWindow = beginServiceWindow;
            this.endServiceWindow = endServiceWindow;
        } else {
            this.beginServiceWindow = endServiceWindow;
            this.endServiceWindow = beginServiceWindow;
        }
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public int getDemand() {
        return demand;
    }

    public void setDemand(int demand) {
        this.demand = demand;
    }

    public int getServiceTime() {
        return serviceTime;
    }

    public void setServiceTime(int serviceTime) {
        this.serviceTime = serviceTime;
    }

    public int getBeginServiceWindow() {
        return beginServiceWindow;
    }

    public void setBeginServiceWindow(int beginServiceWindow) {
        this.beginServiceWindow = beginServiceWindow;
    }

    public int getEndServiceWindow() {
        return endServiceWindow;
    }

    public void setEndServiceWindow(int endServiceWindow) {
        this.endServiceWindow = endServiceWindow;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                '}';
    }
}
