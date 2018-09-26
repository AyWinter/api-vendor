package com.mash.api.service.impl;

import com.mash.api.entity.*;
import com.mash.api.repository.*;
import com.mash.api.service.*;
import com.mash.api.util.Tools;
import com.mysql.jdbc.exceptions.DeadlockTimeoutRollbackMarker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ProductPeriodService productPeriodService;

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

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderProductRepository orderProductRepository;

    @Autowired
    private VipMemberService vipMemberService;

    @Autowired
    private DemandService demandService;

    @Autowired
    private PictureLibraryService pictureLibraryService;

    @Autowired
    private ActivityService activityService;

    /**
     * 更新排期单状态
     * @param id
     * @param state
     * @param request
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateState(Integer id, Integer state, HttpServletRequest request) {

        // 查询排期中所有广告位
        List<ProductPeriod> productPeriods = productPeriodService.findByScheduleId(id);
        for (ProductPeriod productPeriod : productPeriods) {
            Integer periodId = productPeriod.getId();
            // 更新单个广告位状态
            productPeriodService.updateState(periodId, state);
        }
        // 更新排期单状态
        Schedule schedule = scheduleRepository.findOne(id);
        schedule.setState(state);
        // 如果审核成功，则保存审核时间和锁定天数
        if (state == 2)
        {
            schedule.setExamineTime(new Date());
            // 锁定5天
            schedule.setLockDays(5);
        }
        schedule = scheduleRepository.save(schedule);

        // 如果删除排期单，则同时删除排期画面
        if (state == 7)
        {
            String scheduleNumber = schedule.getNumber();
            List<PictureLibrary> pictureLibraries = pictureLibraryService.findByScheduleNumber(scheduleNumber);
            for (PictureLibrary pictureLibrary : pictureLibraries)
            {
                pictureLibraryService.updateState(pictureLibrary.getId(), 3, "排期单已删除", request);
            }
        }

        // 更新项目状态
        Project project = schedule.getProject();
        project.setScheduleState(2);
        projectService.save(project);

        // 保存操作记录
        OperateRecord operateRecord = new OperateRecord();
        // 登录用户ID
        Integer accountId = Tools.getVendorLoginUserId(request);
        String operator = Tools.getNameByLoginUserId(request, accountId, accountService, employeeService);
        // 操作人
        operateRecord.setOperateName(operator);
        // 操所时间
        operateRecord.setOperateTime(new Date());
        // 类型
        operateRecord.setType(0);
        // 操作说明
        String explainText = "【" + operator + "】审核排期单【" + schedule.getNumber() + "】，状态【" + Tools.scheduleStateText(schedule.getState()) + "】=>【" + Tools.scheduleStateText(state) + "】";
        operateRecord.setExplainText(explainText);
        // vendorId
        operateRecord.setVendorId(Tools.getVendorId(request, enterpriseService, accountService, employeeService, departmentService));

        // 保存
        operateRecordRepository.save(operateRecord);
    }

    @Override
    public Schedule save(Schedule schedule) {
        return scheduleRepository.save(schedule);
    }

    @Override
    public List<Schedule> findAll() {
        return scheduleRepository.findAll();
    }

    @Override
    public Page<Schedule> findByParams(Specification<Schedule> specification, Pageable pageable) {
        return scheduleRepository.findAll(specification, pageable);
    }

    @Override
    public Schedule findByNumber(String number) {
        return scheduleRepository.findByNumber(number);
    }

    @Override
    public Schedule findById(Integer id) {
        return scheduleRepository.findOne(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Schedule makeSchedule(Integer projectId,
                                 String productIds,
                                 String startTime,
                                 String endTime,
                                 String totalAmount,
                                 HttpServletRequest request) throws Exception {
        Project project = projectService.findById(projectId);
        Schedule schedule = new Schedule();
        String scheduleNumber = Tools.makeScheduleNumber();
        schedule.setNumber(scheduleNumber);
        // 排期状态：0 已作成
        schedule.setState(0);
        schedule.setProject(project);
        // 广告投放开始时间
        schedule.setStartTime(Tools.str2Date(startTime + " 00:00"));
        // 广告投放结束时间
        schedule.setEndTime(Tools.str2Date(endTime + " 00:00"));
        // 创建人ID
        Integer accountId = Tools.getVendorLoginUserId(request);
        schedule.setCreatedUserId(accountId);
        // 创建时间
        schedule.setCreatedTime(new Date());
        schedule.setVendorId(Tools.getVendorId(request, enterpriseService, accountService, employeeService, departmentService));
        // 创建人姓名
        String operator = Tools.getNameByLoginUserId(request, accountId, accountService, employeeService);
        schedule.setCreatedName(operator);
        // 保存排期单
        schedule = scheduleRepository.save(schedule);

        // 2 创建订单
        MainOrder order = new MainOrder();
        order.setAccountId(0);
        order.setOrderNo(Tools.createOrderNo());
        order.setAmount(Float.valueOf(totalAmount));
        order.setType(1);
        order.setDiscountAmount(getDiscountAmount(Float.valueOf(totalAmount), schedule.getVendorId()));
        order.setCreatedTime(new Date());
        order.setState(0);
        order.setSchedule(schedule);
        order.setVendorId(schedule.getVendorId());
        order = orderRepository.save(order);

        // 3 保存产品周期和订单产品
        String[] ids = productIds.split(",");
        for (Integer i = 0; i < ids.length; i++) {
            ProductPeriod productPeriod = new ProductPeriod();
            Product product = productService.findById(Integer.valueOf(ids[i]));
            productPeriod.setProduct(product);
            productPeriod.setSchedule(schedule);
            productPeriod.setState(0);
            productPeriod.setStartTime(Tools.str2Date(startTime + " 00:00"));
            productPeriod.setEndTime(Tools.str2Date(endTime + " 00:00"));

            // 保存产品周期
            productPeriodService.save(productPeriod);

            // 保存订单产品
            OrderProduct orderProduct = new OrderProduct();
            Date startDate = Tools.str2Date(startTime + " 00:00");
            Date endDate = Tools.str2Date(endTime + " 00:00");
            // 天数
            long days = Tools.differDate(startDate, endDate);
            orderProduct.setQuantity((int) days);
            Set<ProductPrice> productPrices = product.getPrices();
            Iterator<ProductPrice> it = productPrices.iterator();
            String cycle = "";
            float unitPrice = 0;
            while (it.hasNext()) {
                ProductPrice productPrice = it.next();
                cycle = productPrice.getCycle();
                unitPrice = productPrice.getPrice();
            }
            orderProduct.setCycle(cycle);
            orderProduct.setAmount(unitPrice * days);
            orderProduct.setStartTime(startDate);
            orderProduct.setEndTime(endDate);
            orderProduct.setOrder(order);
            orderProduct.setProduct(product);

            orderProductRepository.save(orderProduct);
        }

        // 4 保存操作记录
        OperateRecord operateRecord = new OperateRecord();
        // 操作人
        operateRecord.setOperateName(operator);
        // 操所时间
        operateRecord.setOperateTime(new Date());
        // 类型
        operateRecord.setType(4);
        // 操作说明
        String explainText = "【" + operator + "】做成排期单，项目编号：【" + project.getNumber() + "】";
        operateRecord.setExplainText(explainText);
        // vendorId
        operateRecord.setVendorId(Tools.getVendorId(request, enterpriseService, accountService, employeeService, departmentService));

        // 5 更新项目状态   已作成排期单
        project.setScheduleState(1);
        projectService.save(project);
        return schedule;
    }

    /**
     * 计算折扣金额
     * @param amount 原金额
     * @param vendorId
     * @return 折扣后金额
     */
    public float getDiscountAmount(float amount, Integer vendorId)
    {
        float discountAmount = 0;

        List<Activity> activities = activityService.findByAccountId2(vendorId);
        if (activities.size() == 0)
        {
            discountAmount = amount;
            return discountAmount;
        }
        for (Activity activity : activities)
        {
            if (amount >= activity.getAmount() )
            {
                discountAmount = amount - amount * activity.getDiscount();
                break;
            }
        }

        return discountAmount;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void resetSchedule(Integer scheduleId, String startTime, String endTime, String productIds) throws Exception {

        // 向排期单中添加产品
        Schedule schedule = scheduleRepository.findOne(scheduleId);
        MainOrder order = orderRepository.findByScheduleId(scheduleId);
        String[] ids=  productIds.split(",");
        for (Integer i = 0; i < ids.length; i ++)
        {
            ProductPeriod productPeriod = new ProductPeriod();
            Product product = productService.findById(Integer.valueOf(ids[i]));
            productPeriod.setProduct(product);
            productPeriod.setSchedule(schedule);
            productPeriod.setState(0);
            productPeriod.setStartTime(Tools.str2Date(startTime + " 00:00"));
            productPeriod.setEndTime(Tools.str2Date(endTime + " 00:00"));

            // 保存产品周期
            productPeriodService.save(productPeriod);

            // 保存订单产品
            OrderProduct orderProduct = new OrderProduct();
            Date startDate = Tools.str2Date(startTime + " 00:00");
            Date endDate = Tools.str2Date(endTime + " 00:00");
            // 天数
            long days = Tools.differDate(startDate, endDate);
            orderProduct.setQuantity((int) days);
            Set<ProductPrice> productPrices = product.getPrices();
            Iterator<ProductPrice> it = productPrices.iterator();
            String cycle = "";
            float unitPrice = 0;
            while (it.hasNext()) {
                ProductPrice productPrice = it.next();
                cycle = productPrice.getCycle();
                unitPrice = productPrice.getPrice();
            }
            orderProduct.setCycle(cycle);
            orderProduct.setAmount(unitPrice * days);
            orderProduct.setStartTime(startDate);
            orderProduct.setEndTime(endDate);
            orderProduct.setOrder(order);
            orderProduct.setProduct(product);

            orderProductRepository.save(orderProduct);
        }
        // 更新订单金额
        List<OrderProduct> orderProducts = orderProductRepository.findByOrderId(order.getId());
        float totalAmount = 0;
        for (OrderProduct orderProduct : orderProducts)
        {
            float amount = orderProduct.getAmount();
            totalAmount += amount;
        }
        orderRepository.updateAmount(order.getId(), totalAmount);
    }

    /**
     * 删除拍其中的某个产品
     * @param periodId
     * @param scheduleId
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteProductByScheduleI(Integer periodId, Integer productId, Integer scheduleId) {

        // 产出排期中某个产品
        productPeriodService.deleteById(periodId);
        // 删除订单中的该产品
        Schedule schedule = scheduleRepository.findOne(scheduleId);
        Integer orderId = schedule.getMainOrder().getId();
        orderProductRepository.deleteByOrderIdAndProductId(orderId, productId);
        // 更新订单金额
        List<OrderProduct> orderProducts = orderProductRepository.findByOrderId(orderId);
        float totalAmount = 0;
        for (OrderProduct orderProduct : orderProducts)
        {
            float amount = orderProduct.getAmount();
            totalAmount += amount;
        }
        orderRepository.updateAmount(orderId, totalAmount);
    }

    @Override
    public List<Schedule> findByState(Integer state) {
        return scheduleRepository.findByState(state);
    }
}
