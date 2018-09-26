package com.mash.api.repository;

import com.mash.api.entity.MainOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

public interface OrderRepository extends JpaRepository<MainOrder, Integer>{

    MainOrder findByOrderNo(String orderNo);

    /**
     * 查询用户所有订单
     * @param accountId
     * @param type
     * @return
     */
    List<MainOrder> findByAccountIdAndTypeAndState(Integer accountId, Integer type, Integer payState);

    /**
     *
     * @param accountId
     * @param type
     * @return
     */
    List<MainOrder> findByAccountIdAndType(Integer accountId, Integer type);

    /**
     * 根据状态查询用户所有订单
     * @param accountId
     * @param state
     * @return
     */
    List<MainOrder> findByAccountIdAndState(Integer accountId, Integer state);

    /**
     * 支付成功后更新订单信息
     * @param state
     * @param payMethod
     * @param payTime
     * @param transactionId
     * @param orderNo
     */
    @Transactional
    @Query(value="update main_order set state=?1, pay_method=?2, pay_time=?3, transaction_id=?4 where order_no=?5", nativeQuery = true)
    @Modifying
    void updateOrderState(Integer state, Integer payMethod, Date payTime, String transactionId, String orderNo);

    @Transactional
    @Query(value="update main_order set refund_amount=?4, refund_no=?2, refund_state=1, refund_time=?3 where order_no=?1", nativeQuery = true)
    @Modifying
    void updateRefundState(String orderNo, String refundNo, Date refundTime, float refundAmount);

    @Transactional
    @Query(value="update main_order set refund_state=2 where order_no=?1", nativeQuery = true)
    @Modifying
    void updateRefundStateToExamine(String orderNo);

    /**
     * 获取用户带退款的保证金记录
     * @param accountId
     * @return
     */
    @Query(value="select * from main_order where type=2 and account_id=?1 and refund_state is null", nativeQuery = true)
    List<MainOrder> findRefundBondOrder(Integer accountId);


    /**
     * 后台系统查询所有订单
     * @param specification
     * @param pageable
     * @return
     */
    Page<MainOrder> findAll(Specification<MainOrder> specification, Pageable pageable);

    /**
     * 查询订单根据排期单
     * @param scheduleId
     * @return
     */
    MainOrder findByScheduleId(Integer scheduleId);

    @Transactional
    @Query(value="update main_order set amount=?2 where id=?1", nativeQuery = true)
    @Modifying
    void updateAmount(Integer orderId, float amount);

    /**
     * 退款处理
     * @param orderNo
     * @param refundState
     * @param feedback
     * @param feedbackTime
     */
    @Transactional
    @Query(value="update main_order set refund_state=?2, feedback=?3, feedback_time=?4 where order_no=?1", nativeQuery = true)
    @Modifying
    void executeRefund(String orderNo, Integer refundState, String feedback, Date feedbackTime);
}
