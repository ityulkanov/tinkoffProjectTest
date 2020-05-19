package com.tinkoff.tinkoffProject.service;

import com.tinkoff.tinkoffProject.controller.OrderController;
import com.tinkoff.tinkoffProject.helper.ApiHelper;
import com.tinkoff.tinkoffProject.repo.CandleRepo;
import com.tinkoff.tinkoffProject.repo.InstrumentRepo;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import ru.tinkoff.invest.openapi.OpenApi;
import ru.tinkoff.invest.openapi.models.market.Candle;
import ru.tinkoff.invest.openapi.models.market.CandleInterval;
import ru.tinkoff.invest.openapi.models.market.Instrument;
import ru.tinkoff.invest.openapi.models.market.InstrumentsList;
import ru.tinkoff.invest.openapi.models.operations.OperationStatus;
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
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

@Service
@NoArgsConstructor
@Slf4j
public class OrderService {
    @Autowired
    private InstrumentRepo instrumentsRepo;
    @Value("${app.tinkoff.key}")
    private String key ;

    @Value("${app.tinkoff.brokerId")
    private String brokerId;
    @Autowired
    private CandleRepo candleRepo;
    @Autowired
    ApiHelper apiHelper;




    @NotNull
    public StringBuffer createOrders() throws IOException, InterruptedException, ExecutionException {
        StringBuffer stringBuffer = null;
        try (final OpenApi api = apiHelper.getApi()) {
            final List<String> tickers = Files.readAllLines(Paths.get("src/main/resources/tickers.txt"), StandardCharsets.UTF_8);

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
            stringBuffer = new StringBuffer();
            for (final String instrumentName : tickers) {
                final Instrument instrument = instrumentsList.instruments.stream().filter(x -> x.ticker.equals(instrumentName)).findFirst().get();
                final BigDecimal currentPrice = api.getMarketContext().getMarketOrderbook(instrument.figi, 1).get().get().bids.get(0).price;
                final LimitOrder limitOrder = new LimitOrder(1, Operation.Buy, currentPrice.multiply(new BigDecimal(0.9)).round(new MathContext(4)));
                api.getOrdersContext().placeLimitOrder(instrument.figi, limitOrder, brokerAccountId).get();
                stringBuffer.append("created order for " + instrument.toString() + " at current price of " + currentOrders.toString() + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stringBuffer;
    }

    public String findFigiByTicker(final OpenApi api, final String brokerId, final String ticker) {
        final List<com.tinkoff.tinkoffProject.model.Instrument> instruments = instrumentsRepo.findAll();
        return instruments.stream().filter(x -> x.getType().equals("Stock")).filter(x -> x.getTicker().equals(ticker)).findFirst().get().getFigi();
    }

    @SneakyThrows
    public BigDecimal getProfit(final String from, final String to, final String ticker) {
        try (final OpenApi api = apiHelper.getApi()) {
            final String figi = findFigiByTicker(api, brokerId, ticker);
            final List<ru.tinkoff.invest.openapi.models.operations.Operation> operations = api.getOperationsContext().getOperations(convertStringToOffset(from), convertStringToOffset(to), figi, brokerId).get().operations;
            final Optional<Portfolio.PortfolioPosition> position = api.getPortfolioContext().getPortfolio(brokerId).get().positions.stream().filter(x -> x.ticker.equals(ticker)).findAny();
            if (position.isPresent()) {
                final int lots = position.get().lots;
                final BigDecimal balance = getDailyCandle(figi).closePrice.multiply(BigDecimal.valueOf(lots));
                return getProfit(operations).add(balance);
            } else {
                return getProfit(operations);
            }
        }

    }

    private OffsetDateTime convertStringToOffset(final String date) {
        final ZoneOffset zoneOffSet = ZoneOffset.of("+03:00");
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy:HH:mm");
        final LocalDateTime fromLDT = LocalDateTime.parse(date, dateTimeFormatter);
        return OffsetDateTime.of(fromLDT, zoneOffSet);
    }

    private OffsetDateTime convertLDTToOffset(final LocalDateTime localDateTime) {
        final ZoneOffset zoneOffSet = ZoneOffset.of("+03:00");
        return OffsetDateTime.of(localDateTime, zoneOffSet);
    }

    private Candle getDailyCandle(final String figi) throws InterruptedException, ExecutionException {
        final OpenApi api = apiHelper.getApi();
        final LocalDateTime currDate = getLatestTradeDay();
        final LocalDateTime prevDate = currDate.minusDays(1);
        return api.getMarketContext().getMarketCandles(figi, convertLDTToOffset(prevDate), convertLDTToOffset(currDate), CandleInterval.DAY).get().get().candles.get(0);
    }

    private LocalDateTime getLatestTradeDay() {
        final LocalDateTime now = LocalDateTime.now();
        if (now.getDayOfWeek().equals(DayOfWeek.SATURDAY)) {
            return now.minusDays(1);
        } else if (now.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
            return now.minusDays(2);
        } else {
            return now;
        }
    }


    private BigDecimal getProfit(final List<ru.tinkoff.invest.openapi.models.operations.Operation> operations) {
        final BigDecimal totalComissions = operations.stream()
                .filter(x -> x.status.equals(OperationStatus.Done))
                .filter(x -> x.id.equals("-1"))
                .map(x -> x.payment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        final BigDecimal profit = operations.stream()
                .filter(x -> x.status.equals(OperationStatus.Done))
                .filter(not(x -> x.id.equals("-1")))
                .map(x -> x.payment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return profit.add(totalComissions);
    }

    public String getAccountInfo() throws ExecutionException, InterruptedException {
        return apiHelper.getApi().getUserContext().getAccounts().get().accounts.iterator().next().toString();
    }

    @SneakyThrows
    public List<com.tinkoff.tinkoffProject.model.Candle> saveDailyCandles() {
        List<String> tickers = new ArrayList<>();
        List<com.tinkoff.tinkoffProject.model.Candle> candles = new ArrayList<>();
        try {
            tickers = Files.readAllLines(Paths.get("src/main/resources/tickers.txt"), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            e.printStackTrace();
        }
        for (final String ticker : tickers) {
            final Candle dailyCandle = getDailyCandle(findFigiByTicker(apiHelper.getApi(), brokerId, ticker));
            candles.add(com.tinkoff.tinkoffProject.model.Candle.builder()
                    .closePrice(dailyCandle.closePrice)
                    .figi(dailyCandle.figi)
                    .highestPrice(dailyCandle.highestPrice)
                    .interval(dailyCandle.interval.toString())
                    .lowestPrice(dailyCandle.lowestPrice)
                    .openPrice(dailyCandle.openPrice)
                    .time(dailyCandle.time)
                    .tradesValue(dailyCandle.tradesValue)
                    .build());
        }
        return candleRepo.saveAll(candles);

    }

    public void getUsdInfo() throws ExecutionException, InterruptedException {
        final List<Portfolio.PortfolioPosition> positions = apiHelper.getApi().getPortfolioContext().getPortfolio(brokerId).get().positions;

    }
}
