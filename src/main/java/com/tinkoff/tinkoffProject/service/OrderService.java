package com.tinkoff.tinkoffProject.service;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import ru.tinkoff.invest.openapi.OpenApi;
import ru.tinkoff.invest.openapi.models.market.Instrument;
import ru.tinkoff.invest.openapi.models.market.InstrumentsList;
import ru.tinkoff.invest.openapi.models.orders.LimitOrder;
import ru.tinkoff.invest.openapi.models.orders.Operation;
import ru.tinkoff.invest.openapi.models.portfolio.InstrumentType;
import ru.tinkoff.invest.openapi.models.portfolio.Portfolio;
import ru.tinkoff.invest.openapi.okhttp.OkHttpOpenApiFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@NoArgsConstructor
@Slf4j
public class OrderService {
    @NotNull
    public StringBuffer createOrders(final OkHttpOpenApiFactory openApiFactory) throws IOException, InterruptedException, ExecutionException {
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

    public String findFigiByTicker(final OpenApi api, final String brokerId, final String ticker, final List<Instrument> instruments) {
        return instruments.stream().filter(x -> x.type.equals(ru.tinkoff.invest.openapi.models.market.InstrumentType.Stock)).filter(x -> x.ticker.equals(ticker)).findFirst().get().figi;
    }
}
