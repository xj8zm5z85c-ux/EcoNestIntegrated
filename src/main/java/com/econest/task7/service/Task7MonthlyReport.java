package com.econest.task7.service;

import com.econest.task7.model.Task7ProductStock;
import com.econest.task7.model.Task7Sale;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record Task7MonthlyReport(YearMonth month, BigDecimal revenue, int orderCount,
                            BigDecimal averageOrderValue, List<Task7ProductStock> lowStockProducts) {
    public Task7MonthlyReport {
        lowStockProducts = List.copyOf(lowStockProducts);
    }

    public static Task7MonthlyReport create(YearMonth month, List<Task7Sale> sales, List<Task7ProductStock> inventory) {
        List<Task7Sale> monthSales = sales.stream().filter(s -> YearMonth.from(s.date()).equals(month)).toList();
        BigDecimal revenue = monthSales.stream().map(Task7Sale::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = monthSales.isEmpty() ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(monthSales.size()), 2, java.math.RoundingMode.HALF_UP);
        List<Task7ProductStock> lowStock = inventory.stream().filter(Task7ProductStock::isLowStock)
                .sorted(java.util.Comparator.comparing(Task7ProductStock::sku)).toList();
        return new Task7MonthlyReport(month, revenue, monthSales.size(), average, lowStock);
    }
}
