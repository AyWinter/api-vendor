package com.mash.api.service.impl;

import com.mash.api.entity.*;
import com.mash.api.repository.BondRepository;
import com.mash.api.repository.OrderProductRepository;
import com.mash.api.repository.OrderRepository;
import com.mash.api.repository.ReceiverRepository;
import com.mash.api.service.*;
import com.mash.api.util.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sun.applet.Main;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService{

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderProductRepository orderProductRepository;

    @Autowired
    private ProductPeriodService productPeriodService;

    @Autowired
    private ReceiverRepository receiverRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private ShopcartService shopcartService;

    @Autowired
    private BondRepository bondRepository;

    @Autowired
    private BidResultService bidResultService;

    @Autowired
    private RefundService refundService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ScheduleService scheduleService;

    /**
     * 保存订单
     * 1 创建客户
     * 2 创建项目
     * 3 创建排期
     * 4 创建订单
     * 5 订单产品
     * 6 排期详细
     * @param mainOrder
     * @param receiver
     * @param shopcarts
     * @return
     */
    @Transactional
    @Override
    public MainOrder save(MainOrder mainOrder, Receiver receiver, List<Shopcart> shopcarts) {

        // 资源方Id
        Integer vendorId = shopcarts.get(0).getProduct().getAccountId();

        // 保存客户信息
        Customer customer = customerService.findByPhone(receiver.getMobileNo());
        if (customer == null)
        {
            customer = new Customer();
            customer.setPhone(receiver.getMobileNo());
            customer.setName(receiver.getName());
            String number = Tools.makeCustomerNumber();
            customer.setNumber(number);
            customer.setOperator("system");
            customer.setOperateTime(new Date());
            customer.setVendorId(vendorId);

            customer = customerService.save(customer);
        }
        // 创建项目
        Project project = new Project();
        // 项目名称  默认客户名称
        project.setVendorId(vendorId);
        project.setName(receiver.getName());
        project.setCustomer(customer);
        project.setDepartment("system");
        project.setEmployee("system");
        project.setState(0);
        // 项目类型：0 媒体发布
        project.setType(0);
        project.setCreatedTime(new Date());
        // 流转状态  1 已完毕
        project.setExamineState(1);
        project.setPeopleCount(1);
        String number = Tools.makeProjectNumber();
        project.setNumber(number);
        project.setCustomer(customer);
        // 保存项目
        project = projectService.save(project);

        // 创建排期
        Schedule schedule = new Schedule();
        String scheduleNumber = Tools.makeScheduleNumber();
        schedule.setNumber(scheduleNumber);
        // 排期状态：0 已锁定
        schedule.setState(0);
        schedule.setMainOrder(mainOrder);
        schedule.setProject(project);
        schedule = scheduleService.save(schedule);

        // 保存订单信息
        mainOrder.setSchedule(schedule);
        mainOrder = orderRepository.save(mainOrder);
        //
        List<ProductPeriod> productPeriods = new ArrayList<ProductPeriod>();
        // 保存订单产品信息
        for (Shopcart shopcart : shopcarts)
        {
            vendorId = shopcart.getProduct().getAccountId();

            OrderProduct orderProduct = new OrderProduct();

            float amount = 0;
            orderProduct.setOrder(mainOrder);
            orderProduct.setCycle(shopcart.getCycle());
            orderProduct.setQuantity(shopcart.getQuantity());
            orderProduct.setStartTime(shopcart.getDeliverTime());
            Date endTime = Tools.calculateProductEndTime(shopcart.getDeliverTime(), shopcart.getQuantity(), shopcart.getCycle());
            orderProduct.setEndTime(endTime);

            orderProduct.setProduct(shopcart.getProduct());
            // 折扣
            float discount = 0;
            // 初始金额
            float initialAmount = 0;
            // 折扣后金额
            float discountAmount = 0;
            // 金额
            if (shopcart.getProduct().getPrices().size() > 0)
            {
                for (ProductPrice productPrice : shopcart.getProduct().getPrices())
                {
                    if (shopcart.getCycle().equals(productPrice.getCycle()))
                    {
                        discount = shopcart.getDiscount();
                        initialAmount = shopcart.getQuantity() * productPrice.getPrice();
                        if (discount > 0)
                        {
                            discountAmount = initialAmount  - (initialAmount * discount);
                        }
                        break;
                    }
                }
            }
            else
            {
                BidResult bidResult = bidResultService.findByProductId(shopcart.getProduct().getId());
                if (bidResult.getAmount() < bidResult.getProduct().getBidParameter().getReservePrice())
                {
                    initialAmount = bidResult.getProduct().getBidParameter().getReservePrice();
                }
                else
                {
                    initialAmount = bidResult.getAmount();
                }

                // 更新竞价状态
                Integer productId = shopcart.getProduct().getId();
                bidResultService.updateState2(productId, 1);
            }
            orderProduct.setAmount(initialAmount);
            // 折扣
            orderProduct.setDiscountAmount(discountAmount);

            // 保存
            orderProduct = orderProductRepository.save(orderProduct);

            // 创建排期
            ProductPeriod productPeriod = new ProductPeriod();
            productPeriod.setProduct(orderProduct.getProduct());
            productPeriod.setStartTime(orderProduct.getStartTime());
            productPeriod.setEndTime(orderProduct.getEndTime());
            productPeriod.setSchedule(schedule);
            // 已做成
            productPeriod.setState(0);
            // 保存排期记录
            productPeriod = productPeriodService.save(productPeriod);

            productPeriods.add(productPeriod);

        }

        // 保存收货人信息
        receiver.setOrder(mainOrder);
        receiverRepository.save(receiver);


        // 将下单后的产品从购物车中移除
        for (Shopcart shopcart : shopcarts)
        {
            shopcartService.delete(shopcart.getId());
        }

        return mainOrder;
    }

    /**
     * 竞价保证金订单做成
     * @param mainOrder
     * @param bond
     * @return
     */
    @Transactional
    @Override
    public MainOrder bondSave(MainOrder mainOrder, Bond bond) {
        // 保存订单信息
        mainOrder = orderRepository.save(mainOrder);
        // 保存保证金信息
        bond.setOrder(mainOrder);
        bondRepository.save(bond);
        return mainOrder;
    }

    @Override
    public MainOrder findById(Integer id) {
        return orderRepository.findOne(id);
    }

    @Override
    public MainOrder findByOrderNo(String orderNo) {
        return orderRepository.findByOrderNo(orderNo);
    }

    @Override
    public List<MainOrder> findByAccountIdAndTypeAndState(Integer accountId, Integer type, Integer payState) {
        return orderRepository.findByAccountIdAndTypeAndState(accountId, type, payState);
    }

    @Override
    public List<MainOrder> findByAccountIdAndType(Integer accountId, Integer type) {
        return orderRepository.findByAccountIdAndType(accountId, type);
    }

    @Override
    public List<MainOrder> findByAccountIdAndState(Integer accountId, Integer state) {
        return orderRepository.findByAccountIdAndState(accountId, state);
    }

    @Override
    public List<MainOrder> findAll() {
        return orderRepository.findAll();
    }

    @Override
    @Transactional
    public void updateOrderState(HttpServletRequest request,
                                 Integer state,
                                 Integer payMethod,
                                 Date payTime,
                                 String transactionId,
                                 String orderNo,
                                 Integer type,
                                 Integer id) {

        log.info("step1 update order start");
        orderRepository.updateOrderState(state, payMethod, payTime, transactionId, orderNo);
        log.info("update order end");
        if (type == 1)
        {
            log.info("step2 update product period start");

            MainOrder order = orderRepository.findByOrderNo(orderNo);

            // 排期ID
            Integer scheduleId = order.getSchedule().getId();

            List<ProductPeriod> productPeriods = productPeriodService.findByScheduleId(scheduleId);
            for (ProductPeriod productPeriod : productPeriods)
            {
                Integer productPeriodId = productPeriod.getId();
                // 更新产品排期状态 3： 已锁定
                productPeriodService.updateState(productPeriodId, 3);
            }
            // 更新排期单状态 3 锁定
            scheduleService.updateState(scheduleId, 3, request);

//            List<OrderProduct> orderProducts = orderProductRepository.findByOrderId(id);
//            for(OrderProduct orderProduct : orderProducts)
//            {
//                ProductPeriod productPeriod = new ProductPeriod();
//                productPeriod.setStartTime(orderProduct.getStartTime());
//                productPeriod.setEndTime(orderProduct.getEndTime());
//                productPeriod.setProduct(orderProduct.getProduct());
//
//                productPeriodRepository.save(productPeriod);
//
//                log.info("step3 update bid state start");
//                Integer productId = productPeriod.getProduct().getId();
//
//                bidResultService.updateState2(productId, 2);
//
//            }

        }
        else if (type == 2)
        {
            log.info("step2 update bond");
        }
    }

    @Override
    public List<MainOrder> findRefundBondOrder(Integer accountId) {
        return orderRepository.findRefundBondOrder(accountId);
    }

    /**
     * 退款竞价保证金相关数据更新
     * @param orderNo
     * @param refundNo
     * @param refundTime
     * @param refundAmount
     * @param refundId  退款记录ID
     * @param operator  审核人
     * @param explainText 退款说明
     */
    @Transactional
    @Override
    public void updateRefundState(String orderNo,
                                  String refundNo,
                                  Date refundTime,
                                  float refundAmount,
                                  Integer refundId,
                                  String operator,
                                  String explainText) {
        // 更新订单状态
        orderRepository.updateRefundState(orderNo, refundNo, refundTime, refundAmount);
        // 更新退款申请状态
        refundService.updateRefundState(refundId, operator, explainText, new Date(),2);
    }

    @Override
    public void updateRefundStateToExamine(String orderNo) {
        orderRepository.updateRefundStateToExamine(orderNo);
    }

    @Override
    public MainOrder findByOrderProductId(Integer orderProductId) {
//        OrderProduct orderProduct = orderProductRepository.findOne(orderProductId);
//        Integer orderId = orderProduct.getOrder().getId();
        MainOrder order = orderRepository.findOne(1);
        return order;
    }

    @Override
    public Page<MainOrder> findAll(Specification<MainOrder> specification, Pageable pageable) {
        return orderRepository.findAll(specification, pageable);
    }

    @Transactional
    @Override
    public void executeRefund(MainOrder order,
                              String orderNo,
                              Integer refundState,
                              String feedback,
                              Date feedbackTime,
                              HttpServletRequest request) {
        // 更新订单状态
        orderRepository.executeRefund(orderNo, refundState, feedback, feedbackTime);
        if (refundState == 1)
        {
            log.info("订单：" + orderNo + " 同意退款。");
            log.info("同意退款，更新排期相关数据。");
            // 排期ID
            Integer scheduleId = order.getSchedule().getId();
            // 更新排期单状态 7 退款删除
            scheduleService.updateState(scheduleId, 7, request);
        }
        else
        {
            log.info("订单：" + orderNo + " 人工拒绝退款。");
        }
    }
}
