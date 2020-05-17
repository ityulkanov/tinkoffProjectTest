package com.tinkoff.tinkoffProject.repo;

import com.tinkoff.tinkoffProject.model.Instrument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InstrumentRepo extends JpaRepository<Instrument, Long> {

}
