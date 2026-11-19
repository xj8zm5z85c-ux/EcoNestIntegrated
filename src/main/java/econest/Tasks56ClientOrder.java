package econest;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Tasks56ClientOrder implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id;
    private final Tasks56Client client;
    private final LocalDate createdDate;
    private final List<Tasks56OrderItem> items;
    private BigDecimal total;
    private Tasks56DeliveryAgent deliveryAgent;
    private LocalDate deliveryDate;
    private Tasks56DeliveryStatus deliveryStatus;

    public Tasks56ClientOrder(String id, Tasks56Client client, List<Tasks56OrderItem> items) {
        this(id, client, LocalDate.now(), items);
    }

    public Tasks56ClientOrder(String id, Tasks56Client client, LocalDate createdDate,
            List<Tasks56OrderItem> items) {
        this.id = id;
        this.client = client;
        this.createdDate = createdDate;
        this.items = new ArrayList<Tasks56OrderItem>(items);
        BigDecimal amount = BigDecimal.ZERO;
        for (Tasks56OrderItem item : items) amount = amount.add(item.getLineTotalAmount());
        this.total = amount;
        this.deliveryStatus = Tasks56DeliveryStatus.NOT_ASSIGNED;
    }
    public String getId() { return id; }
    public Tasks56Client getClient() { return client; }
    public LocalDate getCreatedDate() { return createdDate; }
    public List<Tasks56OrderItem> getItems() { return new ArrayList<Tasks56OrderItem>(items); }
    void addRestoredItem(Tasks56OrderItem item) {
        items.add(item);
        total = total.add(item.getLineTotalAmount());
    }
    public double getTotal() { return total.doubleValue(); }
    public BigDecimal getTotalAmount() { return total; }
    public Tasks56DeliveryAgent getDeliveryAgent() { return deliveryAgent; }
    public LocalDate getDeliveryDate() { return deliveryDate; }
    public Tasks56DeliveryStatus getDeliveryStatus() { return deliveryStatus; }
    public void assignDelivery(Tasks56DeliveryAgent agent, LocalDate date) {
        if (agent == null || date == null) throw new IllegalArgumentException("Choose an agent and delivery date.");
        if (date.isBefore(createdDate)) throw new IllegalArgumentException("Delivery date cannot be before the order date.");
        if (deliveryStatus == Tasks56DeliveryStatus.DISPATCHED
                || deliveryStatus == Tasks56DeliveryStatus.DELIVERED) {
            throw new IllegalStateException("A delivery cannot be reassigned after dispatch.");
        }
        this.deliveryAgent = agent;
        this.deliveryDate = date;
        this.deliveryStatus = Tasks56DeliveryStatus.ASSIGNED;
    }
    public void setDeliveryStatus(Tasks56DeliveryStatus status) {
        if (deliveryAgent == null) throw new IllegalStateException("Assign a delivery agent first.");
        if (status == Tasks56DeliveryStatus.NOT_ASSIGNED) throw new IllegalArgumentException("Use an active delivery status.");
        if (status == null) throw new IllegalArgumentException("Choose a delivery status.");
        if (status.ordinal() < deliveryStatus.ordinal()) {
            throw new IllegalArgumentException("Delivery status cannot move backwards.");
        }
        this.deliveryStatus = status;
    }

    public void restoreDelivery(Tasks56DeliveryAgent agent, LocalDate date,
            Tasks56DeliveryStatus status) {
        this.deliveryAgent = agent;
        this.deliveryDate = date;
        this.deliveryStatus = status;
    }
}
