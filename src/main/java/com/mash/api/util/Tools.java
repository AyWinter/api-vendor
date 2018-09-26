package com.mash.api.util;

import com.mash.api.entity.Account;
import com.mash.api.entity.Department;
import com.mash.api.entity.Employee;
import com.mash.api.entity.Enterprise;
import com.mash.api.service.AccountService;
import com.mash.api.service.DepartmentService;
import com.mash.api.service.EmployeeService;
import com.mash.api.service.EnterpriseService;
import org.apache.commons.codec.binary.Base64;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.MessageDigest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Tools {

    /**
     * md5 加密
     * @param str
     * @return
     */
    public static String encryptStrByMD5 (String str)
    {
        String md5Str = "";

        char hexDigits[]={'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};

        try {
            byte[] btInput = str.getBytes();
            // 获得MD5摘要算法的 MessageDigest 对象
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            // 使用指定的字节更新摘要
            mdInst.update(btInput);
            // 获得密文
            byte[] md = mdInst.digest();
            // 把密文转换成十六进制的字符串形式
            int j = md.length;
            char ch[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                ch[k++] = hexDigits[byte0 >>> 4 & 0xf];
                ch[k++] = hexDigits[byte0 & 0xf];
            }

            md5Str = new String(ch);

            return md5Str;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 字符串为空校验
     * @param str
     * @return
     */
    public static boolean isEmpty(String str)
    {
        if (str == null || str.trim().equals(""))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * 图形验证码校验
     * @param request
     * @param code
     * @return
     */
    public static boolean validateKaptcha(HttpServletRequest request, String code)
    {
        HttpSession session = request.getSession();
        System.out.println("sessionId = " + session.getId());
        Object kaptcha = session.getAttribute("verifyCode");
        if (kaptcha == null)
        {
            return false;
        }
        else
        {
            if (code.equals(kaptcha))
            {
                return true;
            }
            else
            {
                return false;
            }
        }
    }

    /**
     * base64 => byte[]
     * @param base64Str
     * @return
     */
    public static byte[] base64StringToByteFun(String base64Str){
        return Base64.decodeBase64(base64Str);
    }

    /**
     * 图片上传
     * @param base64Str
     * @param fileName
     * @return
     */
    public static String uploadImg(String base64Str, String fileName)
    {
        byte[] idcardFrontContent = Tools.base64StringToByteFun(base64Str.split(",")[1]);
        String filePath = OssUtil.putImage(fileName, idcardFrontContent);

        return filePath;
    }

    /**
     * 创建订单
     * @return
     */
    public static String createOrderNo()
    {
        return UUID.randomUUID().toString()
                .replaceAll("-", "");
    }

    /**
     * 判断两个时间段是否有重合
     *
     * @param
     * @param
     * @param
     * @param
     * @return true 有重叠 false 无重叠
     */
    public static boolean dateCheckOverlap(Date startTime1, Date endTime1,
                                    Date startTime2, Date endTime2) {

        boolean b = !(endTime1.compareTo(startTime2) < 0 || startTime1
                .compareTo(endTime2) > 0);

        return b;
    }

    /**
     * 计算广告位到期时间
     * @param startTime
     * @param quantity
     * @param cycle
     * @return
     */
    public static Date calculateProductEndTime(Date startTime, Integer quantity, String cycle)
    {
        Calendar c = Calendar.getInstance();
        c.setTime(startTime);

        if ("日".equals(cycle)) {
            c.add(Calendar.DATE, (int) quantity);
        } else if ("周".equals(cycle)) {
            c.add(Calendar.DATE, (int) quantity * 7);
        } else if ("月".equals(cycle)) {
            c.add(Calendar.MONTH, (int) quantity);
        } else if ("季".equals(cycle)) {
            c.add(Calendar.MONTH, (int) quantity * 3);
        } else if ("年".equals(cycle)) {
            c.add(Calendar.YEAR, (int) quantity);
        }

        return c.getTime();
    }

    /**
     * 随机四位数
     * @return
     */
    public static int randomFourDigit ()
    {
        return (int)(Math.random()*9000+1000);
    }

    /**
     * 随机字符串生成
     *
     * @param length
     *            字符串长度
     * @return random
     */
    public static String getRandomString(int length) {
        String base = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }

    /**
     * string 转 date
     * @param date
     * @return
     */
    public static Date str2Date(String date) throws ParseException {
        if(!isEmpty(date)){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            return sdf.parse(date);
        }else{
            return null;
        }
    }

    /**
     * 格式化日期 date =》 string
     *
     * @return str date
     */
    public static String FormatDateToString(SimpleDateFormat format,
                                            Date datetime) {
        return format.format(datetime);
    }

    private static String Key = "86302a6r6g6h0u73o1ec02edchncg668";

    /**
     * 微信支付签名
     * @param map
     * @return
     */
    public static String createSign(SortedMap<String, Object> map) {
        StringBuffer sb = new StringBuffer();
        Set es = map.entrySet();
        Iterator it = es.iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String k = (String) entry.getKey();
            Object v = entry.getValue();
            if (null != v && !"".equals(v) && !"sign".equals(k)
                    && !"key".equals(k)) {
                sb.append(k + "=" + v + "&");
            }
        }
        sb.append("key=" + Key);
        String sign = MD5Util.sign(sb.toString(), "UTF-8").toUpperCase();
        return sign;
    }

    /**
     * 字符串转换为字符串数组
     * @param str 字符串
     * @param splitRegex 分隔符
     * @return
     */
    public static String[] str2StrArray(String str,String splitRegex){
        if(isEmpty(str)){
            return null;
        }
        return str.split(splitRegex);
    }

    /**
     * 做成客户编号
     * @return
     */
    public static String makeCustomerNumber()
    {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");

        String number = "KH" + Tools.FormatDateToString(sdf, date);

        return number;
    }

    /**
     * 做成执行单
     * @return
     */
    public static String makeExecuteNumber()
    {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");

        String number = "ZXD" + Tools.FormatDateToString(sdf, date);

        return number;
    }

    /**
     * 做成项目编号
     * @return
     */
    public static String makeProjectNumber()
    {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");

        String number = "P" + Tools.FormatDateToString(sdf, date);

        return number;
    }

    /**
     * 做成排期单号
     * @return
     */
    public static String makeScheduleNumber()
    {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");

        String number = "PQ" + Tools.FormatDateToString(sdf, date);

        return number;
    }

    /**
     * 用默认的分隔符(,)将字符串转换为字符串数组
     * @param str	字符串
     * @return
     */
    public static String[] str2StrArray(String str){
        return str2StrArray(str,",\\s*");
    }

    /**
     * 获取资源方ID
     * @param request
     * @param enterpriseService
     * @param accountService
     * @param employeeService
     * @param departmentService
     * @return
     */
    public static Integer getVendorId(HttpServletRequest request,
                                      EnterpriseService enterpriseService,
                                      AccountService accountService,
                                      EmployeeService employeeService,
                                      DepartmentService departmentService)
    {
        Integer vendorLoginUserId = getVendorLoginUserId(request);
        Integer vendorId = 0;

        // 判断是否是企业直接添加信息
        Enterprise enterprise = enterpriseService.getByAccountId(vendorLoginUserId);
        if (enterprise == null)
        {
            // 判断是否是员工
            String mobileNo = accountService.findById(vendorLoginUserId).getMobileNo();
            Employee employee = employeeService.findByMobileNo(mobileNo);
            if (employee != null)
            {
                // 企业ID
                Integer departmentId = employee.getDepartment().getId();
                // 获取部门信息
                Department department = departmentService.findById(departmentId);
                // 企业ID
                Integer enterpriseId = department.getEnterprise().getId();
                // 获取企业信息
                Enterprise enterprise1 = enterpriseService.getById(enterpriseId);

                vendorId = enterprise1.getAccountId();
            }
        }
        else
        {
            Integer state = enterprise.getState();
            if (state == 1)
            {
                vendorId = enterprise.getAccountId();
            }
        }

        return vendorId;
    }

    /**
     * 查询员工姓名 根据loginUserId
     * @param request
     * @param loginUserId
     * @return
     */
    public static String getNameByLoginUserId(HttpServletRequest request,
                                              Integer loginUserId,
                                              AccountService accountService,
                                              EmployeeService employeeService)
    {
        String name = "";

        Account account = accountService.findById(loginUserId);
        String mobileNo = account.getMobileNo();
        // 查询员工姓名
        Employee employee = employeeService.findByMobileNo(mobileNo);
        if (employee == null)
        {
            name = "system";
        }
        else
        {
            name = employee.getName();
        }

        return name;
    }

    /**
     * 获取管理系统登录用户id
     * @param request
     * @return
     */
    public static Integer getVendorLoginUserId(HttpServletRequest request)
    {
        Cookie[] cookies = request.getCookies();
        Cookie loginCookie = null;
        for (Cookie cookie : cookies)
        {
            if ("vendorLoginId".equals(cookie.getName()))
            {
                loginCookie = cookie;
                break;
            }
        }

        Object token = loginCookie.getValue();

        Object userId = RedisUtil.getStr(token.toString());
        return Integer.valueOf(userId.toString());
//        return 1;
    }

    /**
     * 获取用户id
     * @param request
     * @return
     */
    public static Integer getUserId(HttpServletRequest request)
    {
        Cookie[] cookies = request.getCookies();
        Cookie loginCookie = null;
        for (Cookie cookie : cookies)
        {
            if ("loginid".equals(cookie.getName()))
            {
                loginCookie = cookie;
                break;
            }
        }

        Object token = loginCookie.getValue();

        Object userId = RedisUtil.getStr(token.toString());
        return Integer.valueOf(userId.toString());
    }

    /**
     * 获取openId
     * @param request
     * @return
     */
    public static String getOpenId(HttpServletRequest request)
    {
        // openId
        Cookie[] cookies = request.getCookies();
        Cookie openIdCookie = null;
        for (Cookie cookie : cookies) {
            if ("openId".equals(cookie.getName())) {
                openIdCookie = cookie;
                break;
            }
        }

        if (openIdCookie == null)
        {
            return "";
        }

        String value = openIdCookie.getValue();

        String openId = RedisUtil.getStr(value).toString();
//        String openId = "oQh3_wNcOGq0K9ilCCOoBh_fxre0";
        return openId;
    }

    /**
     * 排期单状态
     * @param state
     * @return
     */
    public static String scheduleStateText(Integer state)
    {
        String text = "";
        switch (state)
        {
            case 0:
                text = "已做成";
            case 1:
                text = "已提交";
            case 2:
                text = "已审核";
            case 3:
                text = "已锁定";
            case 4:
                text = "已签";
            case 5:
                text = "下单";
            case 6:
                text = "终止";
            case 7:
                text = "删除";
        }

        return text;
    }

    /**
     * 计算两个日期相差天数
     * @param startDate
     * @param endDate
     * @return
     */
    public static long differDate(Date startDate, Date endDate)
    {
        long days = (endDate.getTime()-startDate.getTime())/(1000*3600*24) + 1;
        return days;
    }

    public static void main(String[] rags)
    {
        String fileType = "aaaa.JPG";
        System.out.println(fileType.substring(fileType.indexOf(".")+1, fileType.length()));
    }
}
