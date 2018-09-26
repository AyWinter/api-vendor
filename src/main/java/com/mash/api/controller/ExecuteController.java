package com.mash.api.controller;

import com.mash.api.entity.Execute;
import com.mash.api.entity.Product;
import com.mash.api.entity.ProductPeriod;
import com.mash.api.entity.Result;
import com.mash.api.service.*;
import com.mash.api.util.ResultUtil;
import com.mash.api.util.Tools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
public class ExecuteController {

    @Autowired
    private ExecuteService executeService;

    @Autowired
    private EnterpriseService enterpriseService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private ProductPeriodService productPeriodService;

    /**
     * 执行单做成
     * @param scheduleId  排期单id
     * @param executeTime 计划安装时间
     * @return
     */
    @PostMapping(value="/vendor/schedule/execute")
    public Result save(@RequestParam("scheduleId")Integer scheduleId,
                       @RequestParam("executeTime")String executeTime,
                       HttpServletRequest request)
    {
        try
        {
            // 验证是否设置广告位画面
            List<ProductPeriod> productPeriods = productPeriodService.findByScheduleId(scheduleId);
            for (ProductPeriod productPeriod : productPeriods)
            {
                if (Tools.isEmpty(productPeriod.getBackPicture()) || Tools.isEmpty(productPeriod.getPositivePicture()))
                {
                    return ResultUtil.fail(-1, "请设置广告位画面");
                }
            }

            // 生成执行单
            Execute execute = executeService.save(scheduleId, Tools.str2Date(executeTime), request);

            return ResultUtil.success();
        }
        catch(Exception e)
        {
            return ResultUtil.fail(-1, e.getMessage());
        }
    }

    /**
     * 查询执行单
     * @param page
     * @param pageSize
     * @param state
     * @return
     */
    @PostMapping(value="/vendor/schedule/execute/all")
    public Result findByParams(@RequestParam("page")Integer page,
                               @RequestParam("pageSize")Integer pageSize,
                               @RequestParam("state")Integer state,
                               HttpServletRequest request)
    {
        Pageable pageable = new PageRequest(page,pageSize, Sort.Direction.DESC,"id");
        final Integer vendorId = Tools.getVendorId(request, enterpriseService, accountService, employeeService, departmentService);
        final Integer executeState = state;
        Specification<Execute> specification = new Specification<Execute>(){

            @Override
            public Predicate toPredicate(Root<Execute> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {

                List<Predicate> predicate = new ArrayList<>();
                if (executeState != -1)
                {
                    predicate.add(criteriaBuilder.equal(root.get("state").as(Integer.class), executeState));
                }
                predicate.add(criteriaBuilder.equal(root.get("vendorId").as(Integer.class), vendorId));
                Predicate[] pre = new Predicate[predicate.size()];
                return criteriaQuery.where(predicate.toArray(pre)).getRestriction();
            }
        };

        return ResultUtil.success(executeService.findByParams(specification, pageable));
    }

    /**
     * 设置安装工人
     * @param id
     * @param worker
     * @param request
     * @return
     */
    @PostMapping(value="/vendor/execute/worker")
    public Result setWorker(@RequestParam("id")Integer id,
                            @RequestParam("number")String number,
                            @RequestParam("worker")String worker,
                            HttpServletRequest request)
    {
        executeService.setWorker(id, number, worker, request);

        return ResultUtil.success();
    }

    @GetMapping(value="/vendor/execute/detail/{executeId}")
    public Result executeDetail(@PathVariable("executeId")Integer executeId)
    {
        return ResultUtil.success(executeService.findById(executeId));
    }

    /**
     * 设置安装工人
     * @param ids
     * @param worker
     * @param workerAccountId
     * @param request
     * @return
     */
    @PostMapping(value="/vendor/execute/product/worker")
    public Result setWorker(@RequestParam("ids")String ids,
                            @RequestParam("worker")String worker,
                            @RequestParam("workerAccountId")Integer workerAccountId,
                            HttpServletRequest request)
    {
        String[] idArray = ids.split(",");
        List<Integer> idList = new ArrayList<Integer>();
        for (Integer i = 0; i < idArray.length; i ++)
        {
            idList.add(Integer.valueOf(idArray[i]));
        }
        // 设置安装功工人
        productPeriodService.multipleSetWorker(worker, workerAccountId, idList);

        return ResultUtil.success();
    }

    /**
     * 设置拆除工人
     * @param ids
     * @param workerId
     * @param workerName
     * @param request
     * @return
     */
    @PostMapping(value="/vendor/product/worker/dismantle")
    public Result setDismantleWorker(@RequestParam("ids")String ids,
                                     @RequestParam("workerId")Integer workerId,
                                     @RequestParam("workerName")String workerName,
                                     HttpServletRequest request)
    {
        String[] idArray = ids.split(",");
        List<Integer> idList = new ArrayList<Integer>();
        for (Integer i = 0; i < idArray.length; i ++)
        {
            idList.add(Integer.valueOf(idArray[i]));
        }
        // 设置拆除工人

        productPeriodService.multipleSetDismantleWorker(workerName, workerId, idList, request);

        return ResultUtil.success();
    }
}
