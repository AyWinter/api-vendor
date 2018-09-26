package com.mash.api.controller;

import com.mash.api.entity.Department;
import com.mash.api.entity.Employee;
import com.mash.api.entity.Menu;
import com.mash.api.entity.Result;
import com.mash.api.repository.MenuRepository;
import com.mash.api.service.DepartmentService;
import com.mash.api.service.EmployeeService;
import com.mash.api.util.ResultUtil;
import com.mash.api.util.RightsUtil;
import com.mash.api.util.Tools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.List;

@RestController
public class RightsController {

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private DepartmentService departmentService;

    @GetMapping(value="/vendor/rights")
    public Result index()
    {
        // 一级菜单
        List<Menu> parentMenus = menuRepository.findByParentIdOrderByOrderIndex(0);

        for (Menu menu : parentMenus)
        {
            Integer id = menu.getId();
            // 子菜单
            List<Menu> children = menuRepository.findByParentIdOrderByOrderIndex(id);

            menu.setChildren(children);
        }

        return ResultUtil.success(parentMenus);
    }

    /**
     * 权限设置
     * @param employeeId
     * @param menuIds
     * @return
     */
    @PostMapping("/vendor/employee/rights")
    public Result save(@RequestParam("employeeId")Integer employeeId,
                       @RequestParam("menuIds")String menuIds)
    {
        BigInteger rights = RightsUtil.sumRights(Tools.str2StrArray(menuIds));

        employeeService.updateRights(employeeId, rights.toString(), menuIds);

        return ResultUtil.success();
    }

    /**
     * 设置部门权限
     * @param departmentId
     * @param menuIds
     * @return
     */
    @PostMapping(value="/vendor/department/rights")
    public Result departmentRights(@RequestParam("departmentId")Integer departmentId,
                                   @RequestParam("menuIds")String menuIds)
    {
        BigInteger rights = RightsUtil.sumRights(Tools.str2StrArray(menuIds));

        departmentService.updateRights(departmentId, rights.toString(), menuIds);

        return ResultUtil.success();
    }

    /**
     * 获取部门当前权限
     * @param departmentId
     * @return
     */
    @GetMapping(value="/vendor/department/rights/{departmentId}")
    public Result departmentRights(@PathVariable("departmentId")Integer departmentId)
    {
        Department department = departmentService.findById(departmentId);

        String menuIds = department.getMenuIds();

        return ResultUtil.success(menuIds.split(","));
    }

    /**
     * 获取部门当前权限
     * @param employeeId
     * @return
     */
    @GetMapping(value="/vendor/employee/rights/{employeeId}")
    public Result employeeRights(@PathVariable("employeeId")Integer employeeId)
    {
        Employee employee = employeeService.findById(employeeId);

        String menuIds = employee.getMenuIds();

        return ResultUtil.success(menuIds.split(","));
    }
}
