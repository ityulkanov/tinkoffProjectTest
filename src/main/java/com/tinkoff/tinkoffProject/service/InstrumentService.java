package com.tinkoff.tinkoffProject.service;

import com.tinkoff.tinkoffProject.helper.ApiHelper;
import com.tinkoff.tinkoffProject.repo.InstrumentRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.tinkoff.invest.openapi.OpenApi;
import ru.tinkoff.invest.openapi.models.market.Instrument;
import ru.tinkoff.invest.openapi.okhttp.OkHttpOpenApiFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

@Service
public class InstrumentService {

    @Autowired
    private InstrumentRepo instrumentRepo;

    @Value("${app.tinkoff.key}")
    private String key;

    public boolean saveInstruments(final List<Instrument> instruments) {
        final List<com.tinkoff.tinkoffProject.model.Instrument> myInstruments = new ArrayList<>();
        for (final Instrument instrument : instruments) {
            myInstruments.add(com.tinkoff.tinkoffProject.model.Instrument.builder()
                    .currency(instrument.currency)
                    .figi(instrument.figi)
                    .isin(instrument.isin)
                    .lot(instrument.lot)
                    .minPriceIncrement(instrument.minPriceIncrement)
                    .name(instrument.name)
                    .ticker(instrument.ticker)
                    .type(instrument.type.name())
                    .dateCreate(LocalDateTime.now())
                    .build());
        }
        instrumentRepo.saveAll(myInstruments);
        return true;
    }

    public List<com.tinkoff.tinkoffProject.model.Instrument> findInstruments() {
        final List<com.tinkoff.tinkoffProject.model.Instrument> instruments = instrumentRepo.findAll();
        final ArrayList<com.tinkoff.tinkoffProject.model.Instrument> templist = new ArrayList<>();
        for (final com.tinkoff.tinkoffProject.model.Instrument instrument : instruments) {
            final long priceNow = instrument.getPriceNow().longValue();
            if ((priceNow > 98 && priceNow < 101) || (priceNow > 980 && priceNow < 1010) || (priceNow > 9800 && priceNow < 10100)) {
                templist.add(instrument);
            }
        }
        return templist;
    }

    public void updatePrices() {
        final List<com.tinkoff.tinkoffProject.model.Instrument> instrumentsList = instrumentRepo.findAll();
        Map<com.tinkoff.tinkoffProject.model.Instrument, BigDecimal> values = new HashMap<>();
        final OkHttpOpenApiFactory okHttpOpenApiFactory = new OkHttpOpenApiFactory(key, Logger.getLogger(ApiHelper.class.getName()));

        try (OpenApi api = okHttpOpenApiFactory.createOpenApiClient(Executors.newSingleThreadExecutor())) {
            for (final com.tinkoff.tinkoffProject.model.Instrument instrument : instrumentsList) {
                values.put(instrument, api.getMarketContext().getMarketOrderbook(instrument.getFigi(), 1).get().get().bids.get(0).price);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }

        instrumentRepo.saveAll(instrumentsList);
    }
}
