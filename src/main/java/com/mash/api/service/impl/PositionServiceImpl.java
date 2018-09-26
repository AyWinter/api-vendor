package com.mash.api.service.impl;

import com.mash.api.entity.Position;
import com.mash.api.repository.PositionRepository;
import com.mash.api.service.PositionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PositionServiceImpl implements PositionService {

    @Autowired
    private PositionRepository positionRepository;

    @Override
    public Position save(Position position) {
        return positionRepository.save(position);
    }

    @Override
    public Page<Position> findAll(Specification<Position> specification, Pageable pageable) {
        return positionRepository.findAll(specification, pageable);
    }

    @Override
    public Position findById(Integer id) {
        return positionRepository.findOne(id);
    }

    @Override
    public List<Integer> findPositionIdsByVendorIdAndArea(Integer vendorId, String area) {
        return positionRepository.findPositionIdsByVendorIdAndArea(vendorId, area);
    }

    @Override
    public void delete(Integer id) {
        positionRepository.delete(id);
    }

    @Override
    public List<Position> findByProvinceAndCityAndAreaAndRoadAndStation(String province, String city, String area, String road, String station) {
        return positionRepository.findByProvinceAndCityAndAreaAndRoadAndStation(province, city, area, road, station);
    }

    @Override
    public List<Position> findByVendorId(Integer vendorId) {
        return positionRepository.findByVendorId(vendorId);
    }
}
