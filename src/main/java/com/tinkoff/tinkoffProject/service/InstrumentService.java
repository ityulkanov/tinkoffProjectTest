package com.tinkoff.tinkoffProject.service;

import com.tinkoff.tinkoffProject.repo.InstrumentRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.tinkoff.invest.openapi.models.market.Instrument;

import java.util.ArrayList;
import java.util.List;

@Service
public class InstrumentService {

    @Autowired
    private InstrumentRepo instrumentRepo;


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
                    .build());
        }
        instrumentRepo.saveAll(myInstruments);
        return true;
    }
}
