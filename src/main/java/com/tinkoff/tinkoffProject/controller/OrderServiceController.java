package com.tinkoff.tinkoffProject.controller;


import com.tinkoff.tinkoffProject.service.OrderService;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.tinkoff.invest.openapi.OpenApi;
import ru.tinkoff.invest.openapi.models.market.Instrument;
import ru.tinkoff.invest.openapi.models.operations.Operation;
import ru.tinkoff.invest.openapi.okhttp.OkHttpOpenApiFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

@RestController
@Slf4j
@NoArgsConstructor
public class OrderServiceController {
    @Autowired
    OrderService orderService;
    @Value("${app.tinkoff.key}")
    private String key;
    @Value("${app.tinkoff.brokerId}")
    private String brokerId;
    private OkHttpOpenApiFactory openApiFactory;
    private OpenApi api;

    private List<Instrument> instruments;


    @PostConstruct
    public void init() throws ExecutionException, InterruptedException {
        this.openApiFactory = new OkHttpOpenApiFactory(key, Logger.getLogger(OrderServiceController.class.getName()));
        this.api = openApiFactory.createOpenApiClient(Executors.newSingleThreadExecutor());
        this.instruments = api.getMarketContext().getMarketStocks().get().instruments;
    }

    @PreDestroy
    public void close() throws Exception {
        this.api.close();
    }

    @SneakyThrows
    @GetMapping("/login")
    public String login() {
        return api.getUserContext().getAccounts().get().accounts.iterator().next().toString();

    }


    @GetMapping("/sendOrders")
    public StringBuffer createOrders() {

        try {
            return orderService.createOrders(openApiFactory);
        } catch (final IOException e) {
            e.printStackTrace();
        } catch (final InterruptedException e) {
            e.printStackTrace();
        } catch (final ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    @GetMapping("/createOrder")

    public String sendOrder(@RequestParam final String price, @RequestParam final String ticker, @RequestParam final String orderType, @RequestParam final String quantity) {
        return price + " " + ticker + " " + orderType + " " + quantity;
    }

    @GetMapping("/getOrders")
    public int getOrders(@RequestParam final String from, @RequestParam final String to, @RequestParam final String ticker) throws Exception {
        final String figi = orderService.findFigiByTicker(this.api, brokerId, ticker, this.instruments);
        final List<Operation> operations = api.getOperationsContext().getOperations(convertToOffset(from), convertToOffset(to), ticker, brokerId).get().operations;
        api.close();
        return operations.size();
    }

    private OffsetDateTime convertToOffset(@RequestParam final String from) {
        final ZoneOffset zoneOffSet = ZoneOffset.of("+03:00");
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy:HH:mm");
        final LocalDateTime fromLDT = LocalDateTime.parse(from, dateTimeFormatter);
        return OffsetDateTime.of(fromLDT, zoneOffSet);
    }
}
