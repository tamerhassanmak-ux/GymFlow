package com.mycompany.lazar_app.ui.paging;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** A bounded result set; totals always cover every matching database record. */
public record PageResult<T>(List<T> items, long total, int pageIndex,
                            Map<String, Long> statistics, Map<String, BigDecimal> amounts) {
    public PageResult(List<T> items, long total, int pageIndex, Map<String, Long> statistics) {
        this(items, total, pageIndex, statistics, Map.of());
    }
    public PageResult {
        items = List.copyOf(items);
        statistics = Map.copyOf(statistics);
        amounts = Map.copyOf(amounts);
    }
}
