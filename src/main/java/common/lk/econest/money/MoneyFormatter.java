package lk.econest.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Objects;

/** Formats application monetary values in Sri Lankan rupees independent of host locale. */
public final class MoneyFormatter {
    private MoneyFormatter() {
    }

    public static String format(BigDecimal amount) {
        Objects.requireNonNull(amount, "Amount is required.");
        return "LKR " + String.format(Locale.ROOT, "%,.2f",
                amount.setScale(2, RoundingMode.HALF_UP));
    }
}
