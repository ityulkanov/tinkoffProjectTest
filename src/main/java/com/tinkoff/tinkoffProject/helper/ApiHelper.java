package com.tinkoff.tinkoffProject.helper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.tinkoff.invest.openapi.OpenApi;
import ru.tinkoff.invest.openapi.okhttp.OkHttpOpenApiFactory;

import java.util.concurrent.Executors;
import java.util.logging.Logger;

@Component
public final class ApiHelper {

    private OpenApi api;

    @Value("${app.tinkoff.key}")
    private String key;

    public OpenApi getApi() {
        final OkHttpOpenApiFactory okHttpOpenApiFactory = new OkHttpOpenApiFactory(this.key, Logger.getLogger(ApiHelper.class.getName()));
        this.api = okHttpOpenApiFactory.createOpenApiClient(Executors.newSingleThreadExecutor());
        return this.api;
    }

}
