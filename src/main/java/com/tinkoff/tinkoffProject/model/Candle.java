package com.tinkoff.tinkoffProject.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;
import ru.tinkoff.invest.openapi.models.market.CandleInterval;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Builder
public class Candle {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    public Long id;
    public String figi;
    public String interval;
    public BigDecimal openPrice;
    public BigDecimal closePrice;
    public BigDecimal highestPrice;
    public BigDecimal lowestPrice;
    public BigDecimal tradesValue;
    public OffsetDateTime time;
}
