package com.mash.api.service;

import com.mash.api.entity.Demand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface DemandService {

    Demand save(Demand demand);

    Demand findById(Integer id);

    List<Demand> findAll();

    List<Demand> findByAccountId(Integer accountId);

    Page<Demand> findAll(Specification<Demand> specification, Pageable pageable);
}
