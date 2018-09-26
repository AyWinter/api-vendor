package com.mash.api.controller;

import com.mash.api.entity.Demand;
import com.mash.api.entity.Result;
import com.mash.api.service.DemandService;
import com.mash.api.util.ResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

@RestController
public class DemandController {

    @Autowired
    private DemandService demandService;

    /**
     * 查询所有需求
     * @param page
     * @param pageSize
     * @param state
     * @return
     */
    @PostMapping(value="/vendor/demand")
    public Result findAll(@RequestParam("page")Integer page,
                          @RequestParam("pageSize")Integer pageSize,
                          @RequestParam("state")Integer state)
    {
        Pageable pageable = new PageRequest(page,pageSize, Sort.Direction.DESC,"id");

        final Integer demandState = state;
        Specification<Demand> specification = new Specification<Demand>(){

            @Override
            public Predicate toPredicate(Root<Demand> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicate = new ArrayList<>();
                if (demandState != -1)
                {
                    predicate.add(criteriaBuilder.equal(root.get("state").as(Integer.class), demandState));
                }
                Predicate[] pre = new Predicate[predicate.size()];
                return criteriaQuery.where(predicate.toArray(pre)).getRestriction();
            }
        };

        return ResultUtil.success(demandService.findAll(specification, pageable));
    }

    /**
     * 更新状态
     * @param id
     * @param state
     * @return
     */
    @PostMapping(value="/vendor/demand/update")
    public Result updateState(@RequestParam("id")Integer id,
                              @RequestParam("state")Integer state)
    {
        Demand demand = demandService.findById(id);
        demand.setState(state);

        demandService.save(demand);
        return ResultUtil.success();
    }
}
