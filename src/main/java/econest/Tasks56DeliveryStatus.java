package econest;

public enum Tasks56DeliveryStatus {
    NOT_ASSIGNED("Not assigned"), ASSIGNED("Assigned"), DISPATCHED("Dispatched"), DELIVERED("Delivered");
    private final String label;
    Tasks56DeliveryStatus(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
