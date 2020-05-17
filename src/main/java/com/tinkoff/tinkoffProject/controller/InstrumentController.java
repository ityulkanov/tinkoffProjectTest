package com.tinkoff.tinkoffProject.controller;

import com.tinkoff.tinkoffProject.service.InstrumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.tinkoff.invest.openapi.OpenApi;
import ru.tinkoff.invest.openapi.okhttp.OkHttpOpenApiFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

@RestController
public class InstrumentController {

    @Autowired
    private InstrumentService instrumentService;

    @PostMapping("/instruments")
    public boolean saveAllInstruments(@Value("${app.tinkoff.key}") final String key) throws ExecutionException, InterruptedException {
        final OkHttpOpenApiFactory okHttpOpenApiFactory = new OkHttpOpenApiFactory(key, Logger.getLogger(OrderServiceController.class.getName()));
        final OpenApi openApiClient = okHttpOpenApiFactory.createOpenApiClient(Executors.newSingleThreadExecutor());
        return instrumentService.saveInstruments(openApiClient.getMarketContext().getMarketStocks().get().instruments);
    }

    @PostMapping("/dailystats")
    public boolean getDailyStats() {


        return true;
    }
}
