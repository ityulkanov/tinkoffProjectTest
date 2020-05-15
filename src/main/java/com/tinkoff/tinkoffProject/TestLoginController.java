package com.tinkoff.tinkoffProject;


import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.tinkoff.invest.openapi.OpenApi;
import ru.tinkoff.invest.openapi.models.market.Instrument;
import ru.tinkoff.invest.openapi.models.market.InstrumentsList;
import ru.tinkoff.invest.openapi.models.orders.LimitOrder;
import ru.tinkoff.invest.openapi.models.orders.Operation;
import ru.tinkoff.invest.openapi.models.portfolio.InstrumentType;
import ru.tinkoff.invest.openapi.models.portfolio.Portfolio;
import ru.tinkoff.invest.openapi.okhttp.OkHttpOpenApiFactory;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RestController
@Slf4j
@NoArgsConstructor
public class TestLoginController {
    @Value("${app.tinkoff.key}")
    private String key;


    private OkHttpOpenApiFactory openApiFactory;

    private String brokerId;

    @PostConstruct
    public void init() throws ExecutionException, InterruptedException {
        this.openApiFactory = new OkHttpOpenApiFactory(key, Logger.getLogger(TestLoginController.class.getName()));
    }

    @SneakyThrows
    @GetMapping("/login")
    public String login() {
        final OpenApi api = openApiFactory.createOpenApiClient(Executors.newSingleThreadExecutor());
        return api.getUserContext().getAccounts().get().accounts.iterator().next().toString();

    }

    @SneakyThrows
    @GetMapping("/sendOrders")
    public StringBuffer sendOrders() {
        final List<String> tickers = Files.readAllLines(Paths.get("src/main/resources/tickers.txt"), StandardCharsets.UTF_8);
        final OpenApi api = openApiFactory.createOpenApiClient(Executors.newSingleThreadExecutor());
        final InstrumentsList instrumentsList = api.getMarketContext().getMarketStocks().get();
        final String brokerAccountId = api.getUserContext().getAccounts().get().accounts.iterator().next().brokerAccountId;
        final Portfolio portfolio = api.getPortfolioContext().getPortfolio(brokerAccountId).get();
        final var currentOrders = api.getOrdersContext().getOrders(brokerAccountId).join();
        final List<Portfolio.PortfolioPosition> positions = portfolio.positions;
        // finding out what positions are currently active
        final List<String> positionTickers = positions.stream()
                .filter(x -> x.instrumentType.equals(InstrumentType.Stock))
                .map(x -> x.ticker).collect(Collectors.toList());
        // finding currently arctive orders
        final List<String> ordersFigi = currentOrders.stream().map(x -> x.figi).collect(Collectors.toList());
        final List<String> ordersTickers = instrumentsList.instruments.stream()
                .filter(x -> ordersFigi.contains(x.figi))
                .map(x -> x.ticker)
                .collect(Collectors.toList());
        // removing all active orders and positions
        tickers.removeAll(positionTickers);
        tickers.removeAll(ordersTickers);
        final StringBuffer stringBuffer = new StringBuffer();
        for (final String instrumentName : tickers) {
            final Instrument instrument = instrumentsList.instruments.stream().filter(x -> x.ticker.equals(instrumentName)).findFirst().get();
            final BigDecimal currentPrice = api.getMarketContext().getMarketOrderbook(instrument.figi, 1).get().get().bids.get(0).price;
            final LimitOrder limitOrder = new LimitOrder(1, Operation.Buy, currentPrice.multiply(new BigDecimal(0.9)).round(new MathContext(4)));
            api.getOrdersContext().placeLimitOrder(instrument.figi, limitOrder, brokerAccountId).get();
            stringBuffer.append("created order for " + instrument.toString() + " at current price of " + currentOrders.toString() + "\n");
        }
        return stringBuffer;
    }
}
