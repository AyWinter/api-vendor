package com.mash.api.controller;

import com.mash.api.entity.Employee;
import com.mash.api.entity.PictureLibrary;
import com.mash.api.entity.Result;
import com.mash.api.service.*;
import com.mash.api.util.ResultUtil;
import com.mash.api.util.Tools;
import org.apache.tomcat.jni.Library;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import sun.misc.BASE64Encoder;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PipedReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
public class PictureLibraryController {

    @Autowired
    private PictureLibraryService pictureLibraryService;

    @Autowired
    private EnterpriseService enterpriseService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private EmployeeService employeeService;

    /**
     * 画面库管理
     * @param page
     * @param pageSize
     * @param scheduleNumber
     * @param request
     * @return
     */
    @PostMapping(value="/vendor/picture/search")
    public Result<PictureLibrary> findByParams(@RequestParam("page")Integer page,
                                               @RequestParam("pageSize")Integer pageSize,
                                               @RequestParam("scheduleNumber")String scheduleNumber,
                                               HttpServletRequest request)
    {
        Pageable pageable = new PageRequest(page,pageSize, Sort.Direction.DESC,"id");

        final String finalScheduleNumber = scheduleNumber;

        final Integer finalVendorId = Tools.getVendorId(request,
                enterpriseService,
                accountService,
                employeeService,
                departmentService);
        final Integer finalPage = page;

        Specification<PictureLibrary> specification = new Specification<PictureLibrary>(){


            @Override
            public Predicate toPredicate(Root<PictureLibrary> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {

                List<Predicate> predicate = new ArrayList<>();

                predicate.add(criteriaBuilder.equal(root.get("vendorId").as(Integer.class), finalVendorId));
                if (!Tools.isEmpty(finalScheduleNumber))
                {
                    predicate.add(criteriaBuilder.equal(root.get("scheduleNumber").as(String.class), finalScheduleNumber));
                }

                // 只查询未删除的图片
                predicate.add(criteriaBuilder.notEqual(root.get("state").as(Integer.class), 3));
                Predicate[] pre = new Predicate[predicate.size()];
                return criteriaQuery.where(predicate.toArray(pre)).getRestriction();
            }
        };

        return ResultUtil.success(pictureLibraryService.findByParams(specification, pageable));
    }

    /**
     * 上传图片
     * @param request
     * @return
     * @throws Exception
     */
    @PostMapping(value="/vendor/picture/upload")
    public Result uploadImage(HttpServletRequest request)throws Exception
    {
        // 排期单
        String scheduleNumber = request.getParameter("scheduleNumber");
        // 客户名称
        String customerName = request.getParameter("customerName");
        // vendorId
        Integer vendorId = Integer.valueOf(request.getParameter("vendorId").toString());

        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        //文件对象
        Map<String, MultipartFile> files = multipartRequest.getFileMap();

        MultipartFile multipartFile = files.get("file");
        String oldFileName = multipartFile.getOriginalFilename();
        String fileType = oldFileName.substring(oldFileName.indexOf("."), oldFileName.length());

        if (!fileType.toLowerCase().equals(".png") && !fileType.toLowerCase().equals(".jpg"))
        {
            System.out.println("只能上传jpg/png文件");
            return ResultUtil.fail(-1, "只能上传jpg/png文件");
        }

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

        PictureLibrary pictureLibrary = new PictureLibrary();
        pictureLibrary.setScheduleNumber(scheduleNumber);
        pictureLibrary.setCustomerName(customerName);
        // 图画状态 0 未审核
        pictureLibrary.setState(0);
        pictureLibrary.setVendorId(vendorId);
        // 文件名称
        Date data = new Date();
        String fileName = scheduleNumber + "_" +  data.getTime() + fileType;

        String filePath = Tools.uploadImg("data:image/jpeg;base64," + strBase64, fileName);
        pictureLibrary.setFilePath(filePath);
        // 保存
        pictureLibraryService.save(pictureLibrary);

        return ResultUtil.success();
    }

    /**
     * 查询审核成功画面根据排期号
     * @param scheduleNumber
     * @return
     */
    @GetMapping(value="/vendor/picture/{scheduleNumber}")
    public Result findByScheduleNumber(@PathVariable("scheduleNumber")String scheduleNumber)
    {
        return ResultUtil.success(pictureLibraryService.findByScheduleNumberAndState(scheduleNumber, 1));
    }

    /**
     * 画面审核
     * @param id
     * @param state
     * @param explain
     * @return
     */
    @PostMapping(value="/vendor/picture/examine")
    public Result updateState(@RequestParam("id")Integer id,
                              @RequestParam("state")Integer state,
                              @RequestParam("explain")String explain,
                              HttpServletRequest request)
    {
        pictureLibraryService.updateState(id, state, explain, request);
        return ResultUtil.success();
    }
}
