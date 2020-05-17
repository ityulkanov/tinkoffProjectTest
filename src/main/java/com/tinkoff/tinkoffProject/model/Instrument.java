package com.tinkoff.tinkoffProject.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.tinkoff.invest.openapi.models.Currency;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.math.BigDecimal;

@Data
@Entity
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Instrument {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @NotNull
    private String figi;
    @NotNull
    private String ticker;
    @Nullable
    private String isin;
    @Nullable
    private BigDecimal minPriceIncrement;
    private int lot;
    @Nullable
    private Currency currency;
    @NotNull
    private String name;
    @NotNull
    private String type;

    private Long dailyLow;
    private Long dailyHigh;


}
