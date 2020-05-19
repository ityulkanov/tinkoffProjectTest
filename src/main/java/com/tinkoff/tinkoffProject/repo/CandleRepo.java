package com.tinkoff.tinkoffProject.repo;

import com.tinkoff.tinkoffProject.model.Candle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CandleRepo extends JpaRepository<Candle, Long> {

}
