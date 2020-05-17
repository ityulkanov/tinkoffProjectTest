package com.tinkoff.tinkoffProject.repo;

import com.tinkoff.tinkoffProject.model.Instrument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepo extends JpaRepository<Instrument, Long> {

}
