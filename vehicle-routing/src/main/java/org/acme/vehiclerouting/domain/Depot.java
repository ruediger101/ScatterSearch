package org.acme.vehiclerouting.domain;

public class Depot {

    private final long id;
    private final Location location;

    public Depot(long id, Location location) {
        this.id = id;
        this.location = location;
    }

    public long getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o instanceof Depot)
            return id == ((Depot) o).getId();
        else
            return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (int) id;
        return hash;
    }
}
