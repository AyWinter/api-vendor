package com.mash.api.controller;

import com.mash.api.entity.*;
import com.mash.api.repository.ProductPeriodRepository;
import com.mash.api.service.*;
import com.mash.api.util.ResultUtil;
import com.mash.api.util.Tools;
import net.sf.json.JSONObject;
import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import sun.misc.BASE64Encoder;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.*;

@RestController
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private EnterpriseService enterpriseService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductPeriodRepository productPeriodRepository;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private PositionService positionService;

    /**
     * 广告位保存
     * @param product
     * @param bidParameter
     * @param bindingResult
     * @param img1Base64
     * @param img2Base64
     * @param cycles
     * @param cyclePrice
     * @param request
     * @return
     */
    @PostMapping(value="/vendor/product")
    public Result<Product> save(@Valid Product product,
                                ProductAttribute productAttribute,
                                BindingResult bindingResult,
                                BidParameter bidParameter,
                                @RequestParam("img1Base64") String img1Base64,
                                @RequestParam("img2Base64") String img2Base64,
                                @RequestParam("cycles") String cycles,
                                @RequestParam("cyclePrice") String cyclePrice,
                                @RequestParam("startTimeParam") String startTimeParam,
                                @RequestParam("endTimeParam") String endTimeParam,
                                @RequestParam("deliverTimeParam") String deliverTimeParam,
                                @RequestParam("positionId")Integer positionId,
                                HttpServletRequest request)
    {
        try
        {
// 获取vendorId
            Integer vendorId = Tools.getVendorId(request,
                    enterpriseService,
                    accountService,
                    employeeService,
                    departmentService);
            if (vendorId == 0)
            {
                return ResultUtil.fail(-1, "暂时无权限发布广告位，请通过资源方审核后在进行发布");
            }

            if (bindingResult.hasErrors())
            {
                return ResultUtil.fail(-1, bindingResult.getFieldError().getDefaultMessage());
            }

            if (Tools.isEmpty(img1Base64) && Tools.isEmpty(img2Base64))
            {
                return ResultUtil.fail(-1, "请上传广告位图片");
            }

            if (product.getPriceType() == 0)
            {
                if (Tools.isEmpty(cycles) || Tools.isEmpty(cyclePrice))
                {
                    return ResultUtil.fail(-1, "请输入周期价格");
                }
            }
            else
            {
                bidParameter.setStartTime(Tools.str2Date(startTimeParam));
                bidParameter.setEndTime(Tools.str2Date(endTimeParam));
                bidParameter.setDeliverTime(Tools.str2Date(deliverTimeParam));
                // 竞价时间校验
                // 开始时间
                Date startTime = bidParameter.getStartTime();
                // 结束时间
                Date endTime = bidParameter.getEndTime();
                // 广告位交付 时间
                Date deliverTime = bidParameter.getDeliverTime();

                if (startTime.after(endTime))
                {
                    return ResultUtil.fail(-1, "请设置竞价正确的起止时间");
                }

                if (deliverTime.before(endTime))
                {
                    return ResultUtil.fail(-1, "请设置正确的广告位交付时间");
                }
            }

            // 保存图片
            Date date = new Date();
            String img1Path = "";
            String img2Path = "";
            if (!Tools.isEmpty(img1Base64))
            {
                String img1Name = "product_" + date.getTime() + Tools.randomFourDigit()+ ".jpg";
                img1Path = Tools.uploadImg(img1Base64, img1Name);
                if (img1Path.equals("error"))
                {
                    return ResultUtil.fail(-1, "广告位图片上传失败");
                }
            }

            if (!Tools.isEmpty(img2Base64))
            {
                String img2Name = "product_" + date.getTime() + Tools.randomFourDigit()+ ".jpg";
                img2Path = Tools.uploadImg(img2Base64, img2Name);
                if (img2Path.equals("error"))
                {
                    return ResultUtil.fail(-1, "广告位图片上传失败");
                }
            }

            // 保存
            product = productService.save(product, bidParameter, productAttribute, cycles,cyclePrice, img1Path, img2Path, vendorId, positionId);

            return ResultUtil.success(product);
        }catch(Exception e)
        {

            return ResultUtil.fail(-1, e.getMessage());
        }
    }

    /**
     * 条件查询
     * @param page
     * @param pageSize
     * @param area
     * @param level
     * @param startTime
     * @param endTime
     * @param request
     * @return
     */
    @PostMapping(value="/vendor/product/search")
    public Result<Product> search(@RequestParam("page")Integer page,
                                  @RequestParam("pageSize")Integer pageSize,
                                  @RequestParam("area")String area,
                                  @RequestParam("level")String level,
                                  @RequestParam("startTime")String startTime,
                                  @RequestParam("endTime")String endTime,
                                  HttpServletRequest request) throws ParseException {
        Pageable pageable = new PageRequest(page,pageSize, Sort.Direction.DESC,"id");
        final String searchLevel = level;
        final Date searchStartTime = startTime != "" ? Tools.str2Date(startTime + " 00:00:00") : null;
        final Date searchEndTime = endTime != "" ? Tools.str2Date(endTime + " 23:59:59") : null;
        final String searchArea = area;
        if (searchStartTime != null && searchEndTime != null)
        {
            if (searchStartTime.compareTo(searchEndTime) > 0)
            {
                return ResultUtil.fail(-1, "请选择正确的日期");
            }
        }

        // 获取vendorId
        final Integer vendorId = Tools.getVendorId(request,
                enterpriseService,
                accountService,
                employeeService,
                departmentService);

        // 获取所有排期
        List<Integer> ids = new ArrayList<Integer>();
        // 查询已被锁定的广告位
        List<ProductPeriod> productPeriods = productPeriodRepository.findByState();
        for (ProductPeriod productPeriod : productPeriods)
        {
            if (searchStartTime != null && searchEndTime != null)
            {
                boolean check = Tools.dateCheckOverlap(searchStartTime, searchEndTime, productPeriod.getStartTime(), productPeriod.getEndTime());
                if (check)
                {
                    // 所选时段被占用
                    Integer productId = productPeriod.getProduct().getId();
                    log.info("productId = {}", productId);
                    if (!ids.contains(productId))
                    {
                        ids.add(productId);
                    }
                }
            }
        }
        // 当前时段被占用的产品ID
        final List<Integer> productIds = ids;

        // 查询所有产品ID
        final List<Integer> allIds = productService.findProductId();
        // 将占用的产品id清除掉
        allIds.removeAll(productIds);

        // 区域查询
        final List<Integer> positionIds = positionService.findPositionIdsByVendorIdAndArea(vendorId, area);
        if(positionIds.size() == 0)
        {
            positionIds.add(-1);
        }

        Specification<Product> specification = new Specification<Product>(){

            @Override
            public Predicate toPredicate(Root<Product> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {

                List<Predicate> predicate = new ArrayList<>();

                predicate.add(criteriaBuilder.equal(root.get("accountId").as(Integer.class), vendorId));

                if(!Tools.isEmpty(searchLevel) && !"全部".equals(searchLevel))
                {
                    predicate.add(criteriaBuilder.equal(root.get("level").as(String.class), searchLevel));
                }
                // 价格类型
                predicate.add(criteriaBuilder.equal(root.get("priceType").as(Integer.class), 0));

//                if (allIds.size() > 0)
//                {
//                    predicate.add(root.<Integer>get("id").in(allIds));
//                }

                // 区域
                if(!Tools.isEmpty(searchArea) && !"全城".equals(searchArea))
                {
                    predicate.add(root.get("position").<Integer>get("id").in(positionIds));
                }

                Predicate[] pre = new Predicate[predicate.size()];
                return criteriaQuery.where(predicate.toArray(pre)).getRestriction();
            }
        };
        Page<Product> productPage = productService.findAll(specification, pageable);
        if (searchStartTime != null && searchEndTime != null)
        {
            productPage = fill(productPage, searchStartTime, searchEndTime);
        }

        return ResultUtil.success(productPage);

    }

    public Page<Product> fill(Page<Product> productPage, Date startTime, Date endTime)
    {
        long diffTime = Tools.differDate(startTime, endTime);
        Iterator<Product> productIterator = productPage.iterator();
        // 查询天数
        while(productIterator.hasNext())
        {

            List<Integer> state = new ArrayList<Integer>();
            for (Integer i = 0; i < diffTime; i ++)
            {
                state.add(0);
            }

            Product product = productIterator.next();
            Integer productId = product.getId();
            List<ProductPeriod> productPeriods = productPeriodRepository.findByStateAndProductId(productId);
            for (ProductPeriod productPeriod : productPeriods)
            {
                long startIndex = 0;
                long endIndex = 0;

                if (startTime.compareTo(productPeriod.getStartTime()) >= 0 && endTime.compareTo(productPeriod.getEndTime()) >= 0)
                {
                    startIndex = 0;
                    endIndex = Tools.differDate(startTime, productPeriod.getEndTime());
                }
                else if (productPeriod.getStartTime().compareTo(startTime) >= 0 && endTime.compareTo(productPeriod.getEndTime()) >= 0)
                {
                    startIndex = Tools.differDate(startTime, productPeriod.getStartTime());
                    endIndex = Tools.differDate(startTime, productPeriod.getEndTime());
                }
                else if (productPeriod.getStartTime().compareTo(startTime) >= 0 && productPeriod.getEndTime().compareTo(endTime) >= 0)
                {
                    startIndex = Tools.differDate(startTime, productPeriod.getStartTime());
                    endIndex = diffTime;
                }
                for (Integer i = 0; i < diffTime; i ++)
                {
                    if (i >= startIndex - 1 && i <= endIndex - 1)
                    {
                        state.set(i,1);
                    }
                    else
                    {
                        if (state.get(i) == 1)
                        {
                            continue;
                        }
                        state.set(i,0);
                    }
                }
            }
            product.setCurrentState(state);
        }
        return productPage;
    }

    /**
     * 资源列表
     * @param pageNo
     * @param request
     * @return
     */
    @GetMapping(value="/vendor/product/page/{pageNo}")
    public Result<Product> page(@PathVariable("pageNo")Integer pageNo,
                                HttpServletRequest request)
    {
        // 获取vendorId
        Integer vendorId = Tools.getVendorId(request,
                enterpriseService,
                accountService,
                employeeService,
                departmentService);

        Pageable pageable = new PageRequest(pageNo,10, Sort.Direction.DESC,"id");
        return ResultUtil.success(productService.findByAccountIdAndPriceType(pageable, vendorId, 0));
    }

    @GetMapping(value="/vendor/productId")
    public Result test()
    {
      return ResultUtil.success(productService.findProductId());
    }


    @GetMapping(value="/vendor/product/page/{priceType}/{pageNo}")
    public Result<Product> findPageByAccountIdAndPriceType(@PathVariable("priceType") Integer priceType,
                                                           @PathVariable("pageNo")Integer pageNo,
                                                           HttpServletRequest request)
    {
        // 获取vendorId
        Integer vendorId = Tools.getVendorId(request,
                enterpriseService,
                accountService,
                employeeService,
                departmentService);
        Pageable pageable = new PageRequest(pageNo,10, Sort.Direction.DESC,"id");
        return ResultUtil.success(productService.findByAccountIdAndPriceType(pageable, vendorId, priceType));
    }

    @PostMapping(value="/vendor/product/image")
    public Result uploadImage(HttpServletRequest request)throws Exception
    {
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        //文件对象
        Map<String, MultipartFile> files = multipartRequest.getFileMap();

        MultipartFile multipartFile = files.get("file");
        InputStream in = multipartFile.getInputStream();

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = -1;
        while((len = in.read(buffer)) != -1){
            outStream.write(buffer, 0, len);
        }
        outStream.close();
        in.close();

        byte[] bytes = outStream.toByteArray();
        String strBase64 = new BASE64Encoder().encode(bytes);

        return ResultUtil.success("data:image/jpeg;base64," + strBase64);
    }

    @GetMapping(value="/vendor/product/{id}")
    public Result<Product> findById(@PathVariable("id")Integer id)
    {
        return ResultUtil.success(productService.findById(id));
    }

    /**
     * 根据排期单查询广告位
     * @param scheduleId
     * @param page
     * @return
     */
    @GetMapping(value="/vendor/product/period/{scheduleId}/{page}")
    public Result<ProductPeriod> findProductPeriodByScheduleId(@PathVariable("scheduleId")Integer scheduleId,
                                                               @PathVariable("page")Integer page)
    {

        Pageable pageable = new PageRequest(page,10, Sort.Direction.DESC,"id");
        final Integer finalScheduleId = scheduleId;

        Specification<ProductPeriod> specification = new Specification<ProductPeriod>(){

            @Override
            public Predicate toPredicate(Root<ProductPeriod> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicate = new ArrayList<>();

                predicate.add(criteriaBuilder.equal(root.get("schedule").get("id").as(Integer.class), finalScheduleId));

                Predicate[] pre = new Predicate[predicate.size()];
                return criteriaQuery.where(predicate.toArray(pre)).getRestriction();
            }
        };

        Page<ProductPeriod> productPeriods = productPeriodRepository.findAll(specification, pageable);

        return ResultUtil.success(productPeriods);
    }

    /**
     * 查询将要到期的广告位
     * @param page
     * @param pageSize
     * @param dismantleState
     * @return
     */
    @GetMapping(value="/vendor/product/dismantle/{page}/{pageSize}/{dismantleState}")
    public Result dismantle(@PathVariable("page")Integer page,
                            @PathVariable("pageSize")Integer pageSize,
                            @PathVariable("dismantleState")Integer dismantleState,
                            HttpServletRequest request)
    {
        Pageable pageable = new PageRequest(page,pageSize, Sort.Direction.DESC,"id");
        final Integer state = dismantleState;
        final Integer vendorId = Tools.getVendorId(request,
                enterpriseService,
                accountService,
                employeeService,
                departmentService);

        Specification<ProductPeriod> specification = new Specification<ProductPeriod>(){

            @Override
            public Predicate toPredicate(Root<ProductPeriod> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicate = new ArrayList<>();

                // 已安装的广告位
                predicate.add(criteriaBuilder.equal(root.get("installState").as(Integer.class), 1));
                // 查询已经到期的广告位
                // 当前时间
                Date currentDate = new Date();
                predicate.add(criteriaBuilder.lessThanOrEqualTo(root.get("endTime").as(Date.class), currentDate));
                if (state != -1)
                {
                    predicate.add(criteriaBuilder.equal(root.get("dismantleState").as(Integer.class), state));
                }
                predicate.add(criteriaBuilder.equal(root.get("product").get("accountId").as(Integer.class), vendorId));
                Predicate[] pre = new Predicate[predicate.size()];
                return criteriaQuery.where(predicate.toArray(pre)).getRestriction();
            }
        };

        Page<ProductPeriod> productPeriods = productPeriodRepository.findAll(specification, pageable);

        return ResultUtil.success(productPeriods);
    }

    /**
     * 查询产品的所有排期
     * @param productId
     * @return
     */
    @GetMapping(value="/vendor/product/period/{productId}")
    public Result productPeriod(@PathVariable("productId")Integer productId)
    {
        List<ProductPeriod> productPeriods = productPeriodRepository.findByProductId(productId);

        return ResultUtil.success(productPeriods);
    }

    /**
     * 查询站点下所有广告位
     * @param positionId
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping(value="/vendor/position/product/{positionId}/{page}/{pageSize}")
    public Result getPositionProduct(@PathVariable("positionId")Integer positionId,
                                     @PathVariable("page")Integer page,
                                     @PathVariable("pageSize")Integer pageSize)
    {
        Pageable pageable = new PageRequest(page,pageSize, Sort.Direction.DESC,"id");
        final Integer searchPositionId = positionId;

        Specification<Product> specification = new Specification<Product>(){

            @Override
            public Predicate toPredicate(Root<Product> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {

                List<Predicate> predicate = new ArrayList<>();

                predicate.add(criteriaBuilder.equal(root.get("position").get("id").as(Integer.class), searchPositionId));

                Predicate[] pre = new Predicate[predicate.size()];
                return criteriaQuery.where(predicate.toArray(pre)).getRestriction();
            }
        };

        Page<Product> productPage = productService.findAll(specification, pageable);

        return ResultUtil.success(productPage);
    }

    /**
     * 删除广告位
     * @param id
     * @return
     */
    @DeleteMapping(value="/vendor/product/{id}")
    public Result deleteProduct(@PathVariable("id")Integer id)
    {
        try
        {
            productService.delete(id);

            return ResultUtil.success();
        }
        catch(Exception e)
        {
            return ResultUtil.fail(-1, "广告位已被使用，无法删除");
        }
    }

    @DeleteMapping(value="/vendor/position/{positionId}")
    public Result deletePosition(@PathVariable("positionId")Integer positionId)
    {
        List<Product> products = productService.findByPositionId(positionId);
        if (products.size() > 0)
        {
            return ResultUtil.fail(-1, "该站点下有广告位，不能删除");
        }

        positionService.delete(positionId);

        return ResultUtil.success();
    }

    @GetMapping(value="/product/coordinate")
    public Result convertCoordinate()
    {
        List<Position> positions = positionService.findByVendorId(-99);

        for (Position position : positions)
        {
            String lon = position.getLon();
            String lat = position.getLat();

            String result = map_tx2bd(Double.valueOf(lat), Double.valueOf(lon));
            String newLon = result.split(",")[0];
            String newLat = result.split(",")[1];

            position.setLon(newLon);
            position.setLat(newLat);

            positionService.save(position);
        }

        return ResultUtil.success();
    }

    public static String map_tx2bd(double lat, double lon){
        double bd_lat;
        double bd_lon;
        double x_pi=3.14159265358979324;
        double x = lon, y = lat;
        double z = Math.sqrt(x * x + y * y) + 0.00002 * Math.sin(y * x_pi);
        double theta = Math.atan2(y, x) + 0.000003 * Math.cos(x * x_pi);
        bd_lon = z * Math.cos(theta) + 0.0065;
        bd_lat = z * Math.sin(theta) + 0.006;

        System.out.println("bd_lat:"+bd_lat);
        System.out.println("bd_lon:"+bd_lon);
        return bd_lon+","+bd_lat;
    }

    @GetMapping(value="/product/damei/{pageNo}")
    public Result test(@PathVariable("pageNo") Integer pageNo)
    {
        JSONObject resultJson = null;
        String url = "http://dm.msplat.cn/api/point/list.htm";
        String pageNoStr = pageNo + "";
        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            // 封装请求参数
            List<NameValuePair> paramsList = new ArrayList();
            paramsList.add(new BasicNameValuePair("key", "112B41FA2DF34815983FC5DD82831EFC"));
            paramsList.add(new BasicNameValuePair("pageParam.curPage", pageNoStr));
            paramsList.add(new BasicNameValuePair("pageParam.pageSize", "100"));
            // 转换为键值对
            String params = EntityUtils.toString(new UrlEncodedFormEntity(paramsList, Consts.UTF_8));
            // 请求地址
            url += "?" + params;
            // 创建httpPost.
            HttpPost httpPost = new HttpPost(url);
            // 通过请求对象获取响应对象
            response = httpclient.execute(httpPost);
            // 判断网络连接状态码是否正常(0--200都数正常)
            if (response.getStatusLine().getStatusCode() == 200) {
                String result= EntityUtils.toString(response.getEntity(), "utf-8");
                resultJson = JSONObject.fromObject(result);

                JSONObject data = JSONObject.fromObject(resultJson.get("data"));

                List<JSONObject> jsonObjects = (List<JSONObject>) data.get("pageData");

                for (int i = 0; i < jsonObjects.size(); i ++)
                {
                    JSONObject jsonObject = jsonObjects.get(i);

                    // 省
                    String provinceName = jsonObject.get("provinceName").toString();
                    // 市
                    String cityName = jsonObject.get("cityName").toString();
                    // 区域
                    String districtName = jsonObject.get("districtName").toString();
                    // 路段
                    String resPlaceSite = jsonObject.get("resPlaceSite").toString();
                    // 名称
                    String adPointName = jsonObject.get("adPointName").toString();
                    // 编号
                    String customNumber = jsonObject.get("customNumber").toString();
                    // 级别
                    String levelValue = jsonObject.get("levelValue").toString();
                    // 经度
                    String lot = jsonObject.get("lot").toString();
                    // 纬度
                    String lat = jsonObject.get("lat").toString();
                    // 类型
                    String typeValue = jsonObject.get("typeValue").toString();
                    // 亮化
                    Integer lighting = Integer.valueOf(jsonObject.get("lighting").toString());
                    // 点位ID
                    Integer pointId = Integer.valueOf(jsonObject.get("id").toString());

                    List<Product> products = productService.findByAccountIdAndNumber(-99, customNumber);
                    if (products.size() > 0)
                    {
                        Product product = products.get(0);
                        product.setPointId(pointId);

                        productService.save2(product);
                    }

//                    List<Position> positions = positionService.findByProvinceAndCityAndAreaAndRoadAndStation(provinceName,
//                            cityName, districtName, resPlaceSite.split(",")[0], resPlaceSite.split(",")[1]);
//
//                    Product product = new Product();
//
//                    product.setNumber(customNumber);
//                    product.setName(adPointName);
//                    product.setPriceType(0);
//                    product.setCreatedTime(new Date());
//                    product.setProductType("候车亭");
//                    product.setUseType("商业类");
//                    product.setLevel(levelValue);
//                    product.setUpdateTime(new Date());
//
//                    ProductAttribute productAttribute = new ProductAttribute();
//                    productAttribute.setLength(Float.valueOf("3.5"));
//                    productAttribute.setWidth(Float.valueOf("1.5"));
//                    if (lighting == 0)
//                    {
//                        productAttribute.setLighting("未亮");
//                    }
//                    else if (lighting == 1)
//                    {
//                        productAttribute.setLighting("已亮");
//                    }
//                    productAttribute.setMaterial("灯箱片");
//                    productAttribute.setVehicleFlowrate("1千台以内");
//                    productAttribute.setVisitorsFlowrate("1万人以内");
//
//                    String img1Path = "http://oss.hunchg.com/product_15354440249465043.jpg";
//                    String img2Path = "";
//                    Integer positionId = positions.get(0).getId();
//                    String cycles = "日";
//                    String cyclesPrice = "";
//
//                    if ("AAA".equals(levelValue))
//                    {
//                        cyclesPrice = "258";
//                    }
//                    else if ("AA".equals(levelValue))
//                    {
//                        cyclesPrice = "215";
//                    }
//                    else if ("A".equals(levelValue))
//                    {
//                        cyclesPrice = "180";
//                    }
//                    else if ("B".equals(levelValue))
//                    {
//                        cyclesPrice = "128";
//                    }
//
//                    BidParameter bidParameter = new BidParameter();
//                    productService.save(product, bidParameter, productAttribute, cycles, cyclesPrice, img1Path, img2Path, -99, positionId);

//                    if (positions.size() > 0)
//                    {
//                        continue;
//                    }
//
//                    // 保存站点信息
//                    Position position = new Position();
//
//                    position.setVendorId(-99);
//                    position.setProvince(provinceName);
//                    position.setCity(cityName);
//                    position.setArea(districtName);
//                    position.setRoad(resPlaceSite.split(",")[0]);
//                    position.setStation(resPlaceSite.split(",")[1]);
//                    position.setLat(lat);
//                    position.setLon(lot);
//
//                    positionService.save(position);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
                httpclient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return ResultUtil.success();
    }
}
