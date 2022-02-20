package com.progressoft.tools;

import java.math.BigDecimal;

public interface ScoringSummary {
    public BigDecimal mean();

    public BigDecimal standardDeviation();

    public BigDecimal variance();

    public BigDecimal median();

    public BigDecimal min();

    public BigDecimal max();
}
