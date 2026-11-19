package lk.econest.catalog;

/** Task1Product categories supported by the EcoNest catalogue. */
public enum Task1ProductType {
    PLANT("Plant"),
    PLANTER("Planter"),
    GARDENING_KIT("Gardening kit");

    private final String displayName;

    Task1ProductType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
