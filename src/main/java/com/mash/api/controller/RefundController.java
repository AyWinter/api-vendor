package com.mash.api.controller;

import com.mash.api.entity.MainOrder;
import com.mash.api.entity.Refund;
import com.mash.api.entity.Result;
import com.mash.api.service.*;
import com.mash.api.util.*;
import net.sf.json.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.net.ssl.SSLContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
public class RefundController {

    private static final Logger log = LoggerFactory.getLogger(RefundController.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private RefundService refundService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private EnterpriseService enterpriseService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private EmployeeService employeeService;

    /**
     * 获取用户申请的退款记录
     * @param request
     * @return
     */
    @GetMapping(value="/vendor/refund/list")
    public Result<Refund> findByVendorId(HttpServletRequest request)
    {
        Integer vendorId = Tools.getVendorId(request,
                enterpriseService,
                accountService,
                employeeService,
                departmentService);

        return ResultUtil.success(refundService.findByVendorId(vendorId));
    }

    @PostMapping(value="/vendor/refund")
    public Result refund(@RequestParam("orderNo")String orderNo,
                         @RequestParam("refundId")Integer refundId,
                         @RequestParam("remark")String remark,
                         HttpServletRequest request)
    {
        try
        {
            SortedMap<String, Object> map = new TreeMap<String, Object>();

            map.put("appid", WechatConfig.appid);
            map.put("mch_id", WechatConfig.mchid);
            map.put("nonce_str", Tools.getRandomString(32));
            map.put("out_trade_no", orderNo);
            // 退款单号
            Date d = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String out_refund_no = Tools.FormatDateToString(sdf, d)
                    + Tools.getRandomString(4);

            // 退款单号
            map.put("out_refund_no", out_refund_no);
            // 退款金额 单位（分）
            MainOrder order = this.findOrderByOrderNo(orderNo);
            float orderTotal = order.getAmount();
            log.info("退款订单金额orderTotal = " + orderTotal);
            int orderTotalInt = (int)orderTotal * 100;
            log.info("退款金额：orderTotalInt（分） = " + orderTotalInt);
            orderTotalInt = 1;
            map.put("total_fee", orderTotalInt);
            map.put("refund_fee", orderTotalInt);

            String mySign = Tools.createSign(map);
            map.put("sign", mySign);

            // 退款签名
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            String path = WechatConfig.WECHAT_REFUND_SIGN_CERT;
            String shh = WechatConfig.mchid;
            FileInputStream instream = new FileInputStream(new File(path));
            try {
                keyStore.load(instream, shh.toCharArray());
            } finally {
                instream.close();
            }
            // Trust own CA and all self-signed certs
            SSLContext sslcontext = SSLContexts.custom()
                    .loadKeyMaterial(keyStore, shh.toCharArray()).build();
            // Allow TLSv1 protocol only
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                    sslcontext,
                    new String[] { "TLSv1" },
                    null,
                    SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
            CloseableHttpClient httpClient = HttpClients.custom()
                    .setSSLSocketFactory(sslsf).build();

            HttpPost httppost = new HttpPost(WechatConfig.REFUND_URL);
            StringEntity se = new StringEntity(WebUtil.mapToXml(map));
            httppost.setEntity(se);

            CloseableHttpResponse responseEntry = httpClient.execute(httppost);

            HttpEntity entity = responseEntry.getEntity();
            log.info("退款请求结果：entity = " + entity);
            Map<String, Object> resultMap = WebUtil.entityToMap(entity);
            log.info("退款请求结果：resultMap = " + resultMap);
            if (resultMap.get("return_code").equals("SUCCESS")) {
                if (resultMap.get("result_code").equals("SUCCESS")) {

                    // 操作人
                    Integer vendorId = Tools.getVendorLoginUserId(request);
                    String vendorMobileNo = accountService.findById(vendorId).getMobileNo();
                    orderService.updateRefundState(orderNo, out_refund_no, new Date(), orderTotal, refundId, vendorMobileNo, remark);

                    return ResultUtil.success();
                }
                else
                {
                    return ResultUtil.fail(-1, "退款申请失败");
                }
            }
            else
            {
                return ResultUtil.fail(-1, "退款申请失败");
            }
        }
        catch(Exception e){
            return ResultUtil.fail(-1, "退款申请失败");
        }
    }

    /**
     * 获取订单信息
     * @param orderNo
     * @return
     */
    public MainOrder findOrderByOrderNo(String orderNo)
    {
        MainOrder order = orderService.findByOrderNo(orderNo);

        return order;
    }

    /**
     * 获取申请退款的订单记录（购买广告位订单）
     * @param page
     * @param refundState
     * @param pageSize
     * @return
     */
    @PostMapping(value="/vendor/refund/order")
    public Result findRefundOrder(@RequestParam("page")Integer page,
                                  @RequestParam("refundState")Integer refundState,
                                  @RequestParam("pageSize")Integer pageSize,
                                  HttpServletRequest request)
    {
        Pageable pageable = new PageRequest(page,pageSize, Sort.Direction.DESC,"id");
        final Integer finalRefundState = refundState;

        final Integer vendorId = Tools.getVendorId(request,
                enterpriseService,
                accountService,
                employeeService,
                departmentService);

        Specification<MainOrder> specification = new Specification<MainOrder>()
        {

            @Override
            public Predicate toPredicate(Root<MainOrder> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {

                List<Predicate> predicate = new ArrayList<>();
                // 退款状态
                predicate.add(criteriaBuilder.equal(root.get("refundState").as(Integer.class), finalRefundState));
                // vendor
                predicate.add(criteriaBuilder.equal(root.get("vendorId").as(Integer.class), vendorId));
                Predicate[] pre = new Predicate[predicate.size()];
                return criteriaQuery.where(predicate.toArray(pre)).getRestriction();
            }
        };
        return ResultUtil.success(orderService.findAll(specification, pageable));
    }

    /**
     * 退款处理
     * @param refundState
     * @param orderNo
     * @param feedback
     * @param request
     * @return
     */
    @PostMapping(value="/vendor/refund/order/execute")
    public Result executeRefund(@RequestParam("refundState")Integer refundState,
                                @RequestParam("orderNo")String orderNo,
                                @RequestParam("feedback")String feedback,
                                HttpServletRequest request)
    {
        MainOrder order = orderService.findByOrderNo(orderNo);
        Integer payState = order.getState();
        // 排期状态
        Integer scheduleState = order.getSchedule().getState();
        // 排期单号
        String scheduleNumber = order.getSchedule().getNumber();
        // 支付状态为【已支付】排期状态为【已锁定】才可申请退款
        if (payState == 1 && scheduleState == 3)
        {
            if (refundState == 1)
            {
                // 执行退款操作
                try
                {
                    SortedMap<String, Object> map = new TreeMap<String, Object>();

                    map.put("appid", WechatConfig.appid);
                    map.put("mch_id", WechatConfig.mchid);
                    map.put("nonce_str", Tools.getRandomString(32));
                    map.put("out_trade_no", orderNo);
                    // 退款单号
                    Date d = new Date();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                    String out_refund_no = Tools.FormatDateToString(sdf, d)
                            + Tools.getRandomString(4);

                    // 退款单号
                    map.put("out_refund_no", out_refund_no);
                    // 退款金额 单位（分）
                    float orderTotal = order.getAmount();
                    log.info("退款订单金额orderTotal = " + orderTotal);
                    int orderTotalInt = (int)orderTotal * 100;
                    log.info("退款金额：orderTotalInt（分） = " + orderTotalInt);
                    orderTotalInt = 1;
                    map.put("total_fee", orderTotalInt);
                    map.put("refund_fee", orderTotalInt);

                    String mySign = Tools.createSign(map);
                    map.put("sign", mySign);

                    // 退款签名
                    KeyStore keyStore = KeyStore.getInstance("PKCS12");
                    String path = WechatConfig.WECHAT_REFUND_SIGN_CERT;
                    String shh = WechatConfig.mchid;
                    FileInputStream instream = new FileInputStream(new File(path));
                    try {
                        keyStore.load(instream, shh.toCharArray());
                    } finally {
                        instream.close();
                    }
                    // Trust own CA and all self-signed certs
                    SSLContext sslcontext = SSLContexts.custom()
                            .loadKeyMaterial(keyStore, shh.toCharArray()).build();
                    // Allow TLSv1 protocol only
                    SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                            sslcontext,
                            new String[] { "TLSv1" },
                            null,
                            SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
                    CloseableHttpClient httpClient = HttpClients.custom()
                            .setSSLSocketFactory(sslsf).build();

                    HttpPost httppost = new HttpPost(WechatConfig.REFUND_URL);
                    StringEntity se = new StringEntity(WebUtil.mapToXml(map));
                    httppost.setEntity(se);

                    CloseableHttpResponse responseEntry = httpClient.execute(httppost);

                    HttpEntity entity = responseEntry.getEntity();
                    log.info("退款请求结果：entity = " + entity);
                    Map<String, Object> resultMap = WebUtil.entityToMap(entity);
                    log.info("退款请求结果：resultMap = " + resultMap);
                    if (resultMap.get("return_code").equals("SUCCESS")) {
                        if (resultMap.get("result_code").equals("SUCCESS")) {

                            log.info("订单号：" + order.getOrderNo() + " 已执行微信退款，开始更新业务数据。");
                            orderService.executeRefund(order, orderNo, refundState, feedback, new Date(), request);

                            if (order.getVendorId() == Const.DM_ACCOUNT_ID)
                            {
                                log.info("订单是达美系统订单（排期单号："+scheduleNumber+"），发送请求到达美服务器取消排期单。");

                                JSONObject result = cancelSchedule(scheduleNumber);
                                Integer code = Integer.valueOf(result.get("code").toString());
                                if (code == 0)
                                {
                                    log.info("达美服务器排期单已取消。");
                                    return ResultUtil.success();
                                }
                                else
                                {
                                    String msg = result.getString("msg");
                                    log.info("达美服务器取消排期单失败，错误原因：" + msg);
                                    return ResultUtil.fail(-1, msg);
                                }
                            }
                            else
                            {
                                return ResultUtil.success();
                            }
                        }
                        else
                        {
                            return ResultUtil.fail(-1, "退款申请失败");
                        }
                    }
                    else
                    {
                        return ResultUtil.fail(-1, "退款申请失败");
                    }
                }
                catch(Exception e){
                    return ResultUtil.fail(-1, "退款申请失败");
                }
            }
            else
            {
                log.info("订单："+order.getOrderNo()+" 人工拒绝退款,更新订单状态并反馈信息。");
                orderService.executeRefund(order, orderNo, refundState, feedback, new Date(), request);
                return ResultUtil.success();
            }
        }
        else
        {
            log.info("订单："+order.getOrderNo()+" 不符合基本退款要求。");
            return ResultUtil.fail(-1, "该订单不符合退款要求。");
        }
    }

    /**
     * 取消排期单
     * @param scheduleNumber
     * @return
     */
    public JSONObject cancelSchedule(String scheduleNumber)
    {
        List<NameValuePair> paramsList = new ArrayList();

        paramsList.add(new BasicNameValuePair("key", Const.DM_KEY));
        paramsList.add(new BasicNameValuePair("usingNumber", scheduleNumber));

        JSONObject result = WebUtil.post2(Const.DM_API_URL_CANCEL_SCHEDULE, paramsList);

        return result;
    }
}
