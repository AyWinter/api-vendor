package com.mash.api.service.impl;

import com.mash.api.entity.Execute;
import com.mash.api.entity.OperateRecord;
import com.mash.api.entity.ProductPeriod;
import com.mash.api.entity.Schedule;
import com.mash.api.repository.ExecuteRepository;
import com.mash.api.repository.OperateRecordRepository;
import com.mash.api.service.*;
import com.mash.api.util.Tools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

@Service
public class ExecuteServiceImpl implements ExecuteService {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private ExecuteRepository executeRepository;

    @Autowired
    private OperateRecordRepository operateRecordRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EnterpriseService enterpriseService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private ProductPeriodService productPeriodService;

    @Transactional
    @Override
    public Execute save(Integer scheduleId,
                        Date planInstallTime,
                        HttpServletRequest request) {

        // 获取排期信息
        Schedule schedule = scheduleService.findById(scheduleId);
        Execute execute = new Execute();
        execute.setPlanInstallTime(planInstallTime);
        execute.setSchedule(schedule);
        execute.setState(0);
        Integer vendorId = Tools.getVendorId(request, enterpriseService, accountService, employeeService, departmentService);
        execute.setVendorId(vendorId);
        String executeNumber = Tools.makeExecuteNumber();
        execute.setNumber(executeNumber);
        // 1 生成执行单
        execute = executeRepository.save(execute);
        // 2 更新排期所有广告位状态
        // 查询排期中所有广告位
        List<ProductPeriod> productPeriods = productPeriodService.findByScheduleId(scheduleId);
        for (ProductPeriod productPeriod : productPeriods)
        {
            Integer periodId = productPeriod.getId();
            // 更新单个广告位状态
            productPeriodService.updateState(periodId, 5);
        }
        // 3 更新排期单状态  5  已下单
        scheduleService.updateState(scheduleId, 5, request);
        // 4 保存操作记录
        OperateRecord operateRecord = new OperateRecord();
        // 登录用户ID
        Integer accountId = Tools.getVendorLoginUserId(request);
        String operator = Tools.getNameByLoginUserId(request, accountId, accountService, employeeService);
        // 操作人
        operateRecord.setOperateName(operator);
        // 操所时间
        operateRecord.setOperateTime(new Date());
        // 类型
        operateRecord.setType(1);
        // 操作说明
        String explainText = "【" + operator + "】生成执行单，执行单号：【"+executeNumber+"】，排期单号：【"+schedule.getNumber()+"】";
        operateRecord.setExplainText(explainText);
        // vendorId
        operateRecord.setVendorId(Tools.getVendorId(request, enterpriseService, accountService, employeeService, departmentService));

        // 保存
        operateRecordRepository.save(operateRecord);

        return execute;
    }

    @Override
    public Page<Execute> findByParams(Specification<Execute> specification, Pageable pageable) {
        return executeRepository.findAll(specification, pageable);
    }

    @Override
    public Execute findByScheduleId(Integer scheduleId) {
        return executeRepository.findByScheduleId(scheduleId);
    }

    @Override
    public Execute findById(Integer id) {
        return executeRepository.findById(id);
    }

    @Transactional
    @Override
    public void setWorker(Integer id, String number, String worker, HttpServletRequest request) {
        // 1 设置安装工人
        executeRepository.setWorker(id, worker);
        // 2 保存操作记录
        OperateRecord operateRecord = new OperateRecord();
        // 登录用户ID
        Integer accountId = Tools.getVendorLoginUserId(request);
        String operator = Tools.getNameByLoginUserId(request, accountId, accountService, employeeService);
        // 操作人
        operateRecord.setOperateName(operator);
        // 操所时间
        operateRecord.setOperateTime(new Date());
        // 类型
        operateRecord.setType(3);
        // 操作说明
        String explainText = "【" + operator + "】将执行单【"+number+"】分配给【"+worker+"】";
        operateRecord.setExplainText(explainText);
        // vendorId
        operateRecord.setVendorId(Tools.getVendorId(request, enterpriseService, accountService, employeeService, departmentService));

        // 保存
        operateRecordRepository.save(operateRecord);
    }
}
