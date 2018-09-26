package com.mash.api.service.impl;

import com.mash.api.entity.OperateRecord;
import com.mash.api.entity.PictureLibrary;
import com.mash.api.entity.Schedule;
import com.mash.api.repository.OperateRecordRepository;
import com.mash.api.repository.PictureLibraryRepository;
import com.mash.api.service.*;
import com.mash.api.util.Tools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

@Service
public class PictureLibraryServiceImpl implements PictureLibraryService {

    @Autowired
    private PictureLibraryRepository pictureLibraryRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EnterpriseService enterpriseService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private OperateRecordRepository operateRecordRepository;

    @Override
    public Page<PictureLibrary> findByParams(Specification<PictureLibrary> specification, Pageable pageable) {
        return pictureLibraryRepository.findAll(specification, pageable);
    }

    @Override
    public List<PictureLibrary> findByScheduleNumber(String scheduleNumber) {
        return pictureLibraryRepository.findByScheduleNumber(scheduleNumber);
    }

    @Override
    public PictureLibrary save(PictureLibrary pictureLibrary) {
        return pictureLibraryRepository.save(pictureLibrary);
    }

    @Transactional
    @Override
    public void updateState(Integer id,
                            Integer state,
                            String explain,
                            HttpServletRequest request) {
        // 1 画面审核
        pictureLibraryRepository.updateState(id, state, explain);
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
        operateRecord.setType(2);
        // 操作说明
        String explainText = "";
        if(state == 1)
        {
            explainText = "用户【"+operator+"】审核画面【ID:"+id+"】,审核成功";
        }
        else
        {
            explainText = "用户【"+operator+"】审核画面【ID:"+id+"】,审核失败";
        }
        operateRecord.setExplainText(explainText);
        // vendorId
        operateRecord.setVendorId(Tools.getVendorId(request, enterpriseService, accountService, employeeService, departmentService));

        // 保存
        operateRecordRepository.save(operateRecord);
    }

    @Override
    public List<PictureLibrary> findByScheduleNumberAndState(String scheduleNumber, Integer state) {
        return pictureLibraryRepository.findByScheduleNumberAndState(scheduleNumber, state);
    }
}
