package com.tinkoff.tinkoffProject.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinkoff.tinkoffProject.model.Instrument;
import com.tinkoff.tinkoffProject.service.OrderService;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@Slf4j
@NoArgsConstructor
public class OrderController {

    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    OrderService orderService;
    @Value("${app.tinkoff.key}")
    private String key;
    @Value("${app.tinkoff.brokerId}")
    private String brokerId;


    @SneakyThrows
    @GetMapping("/accinfo")
    public String getAccountInfo() {
        return orderService.getAccountInfo();

    }

    @GetMapping("/createOrders")
    public StringBuffer createOrders() {
        try {
            return orderService.createOrders();
        } catch (final IOException e) {
            e.printStackTrace();
        } catch (final InterruptedException e) {
            e.printStackTrace();
        } catch (final ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }


    @PostMapping("/createOrder")
    public String sendOrder(@RequestParam final String price, @RequestParam final String ticker, @RequestParam final String orderType, @RequestParam final String quantity) {

        return price + " " + ticker + " " + orderType + " " + quantity;
    }

    @GetMapping("/getProfit")
    public BigDecimal getProfit(@RequestParam final String from, @RequestParam final String to, @RequestParam final String ticker) throws Exception {
        return orderService.getProfit(from, to, ticker);
    }

    @GetMapping("/getUsd")
    public void usdData() throws ExecutionException, InterruptedException {
        orderService.getUsdInfo();
    }

    @GetMapping("candles")
    public String dailyCandles() throws JsonProcessingException {

        return objectMapper.writeValueAsString(orderService.saveDailyCandles());
    }
}
