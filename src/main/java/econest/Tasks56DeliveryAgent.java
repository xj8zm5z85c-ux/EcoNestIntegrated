package econest;

import java.io.Serializable;

public class Tasks56DeliveryAgent implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id;
    private final String name;
    private final String phone;
    private final String vehicle;

    public Tasks56DeliveryAgent(String id, String name, String phone, String vehicle) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.vehicle = vehicle;
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getVehicle() { return vehicle; }
    @Override public String toString() { return name + " — " + vehicle; }
}
