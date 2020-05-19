package com.tinkoff.tinkoffProject.controller;

import com.tinkoff.tinkoffProject.helper.ApiHelper;
import com.tinkoff.tinkoffProject.model.Instrument;
import com.tinkoff.tinkoffProject.service.InstrumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.tinkoff.invest.openapi.OpenApi;
import ru.tinkoff.invest.openapi.okhttp.OkHttpOpenApiFactory;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

@RestController
public class InstrumentController {

    @Autowired
    private InstrumentService instrumentService;

    @Autowired
    private ApiHelper apiHelper;

    @PostMapping("/instruments")
    public boolean saveAllInstruments(@Value("${app.tinkoff.key}") final String key) throws ExecutionException, InterruptedException {
        try (final OpenApi api = apiHelper.getApi();){
            final boolean b = instrumentService.saveInstruments(api.getMarketContext().getMarketStocks().get().instruments);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    @GetMapping("/findrounds")
    public List<Instrument> findRoundValues() {
        // add latest price to every instrument
        return instrumentService.findInstruments();
    }

    @GetMapping("/updatePrices")
    public void updatePrices() {
        instrumentService.updatePrices();
    }

    @PostMapping("/dailystats")
    public boolean getDailyStats() {


        return true;
    }
}
