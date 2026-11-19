package econest;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Tasks56ClientOrder implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id;
    private final Tasks56Client client;
    private final LocalDate createdDate;
    private final List<Tasks56OrderItem> items;
    private final double total;
    private Tasks56DeliveryAgent deliveryAgent;
    private LocalDate deliveryDate;
    private Tasks56DeliveryStatus deliveryStatus;

    public Tasks56ClientOrder(String id, Tasks56Client client, List<Tasks56OrderItem> items) {
        this.id = id;
        this.client = client;
        this.createdDate = LocalDate.now();
        this.items = new ArrayList<Tasks56OrderItem>(items);
        double amount = 0;
        for (Tasks56OrderItem item : items) amount += item.getLineTotal();
        this.total = amount;
        this.deliveryStatus = Tasks56DeliveryStatus.NOT_ASSIGNED;
    }
    public String getId() { return id; }
    public Tasks56Client getClient() { return client; }
    public LocalDate getCreatedDate() { return createdDate; }
    public List<Tasks56OrderItem> getItems() { return new ArrayList<Tasks56OrderItem>(items); }
    public double getTotal() { return total; }
    public Tasks56DeliveryAgent getDeliveryAgent() { return deliveryAgent; }
    public LocalDate getDeliveryDate() { return deliveryDate; }
    public Tasks56DeliveryStatus getDeliveryStatus() { return deliveryStatus; }
    public void assignDelivery(Tasks56DeliveryAgent agent, LocalDate date) {
        if (agent == null || date == null) throw new IllegalArgumentException("Choose an agent and delivery date.");
        if (date.isBefore(createdDate)) throw new IllegalArgumentException("Delivery date cannot be before the order date.");
        this.deliveryAgent = agent;
        this.deliveryDate = date;
        this.deliveryStatus = Tasks56DeliveryStatus.ASSIGNED;
    }
    public void setDeliveryStatus(Tasks56DeliveryStatus status) {
        if (deliveryAgent == null) throw new IllegalStateException("Assign a delivery agent first.");
        if (status == Tasks56DeliveryStatus.NOT_ASSIGNED) throw new IllegalArgumentException("Use an active delivery status.");
        this.deliveryStatus = status;
    }
}
