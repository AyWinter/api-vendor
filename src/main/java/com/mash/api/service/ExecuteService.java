package com.mash.api.service;

import com.mash.api.entity.Execute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

/**
 * 执行单service
 */
public interface ExecuteService {

    Execute save(Integer scheduleId, Date planInstallTime, HttpServletRequest request);

    Page<Execute> findByParams(Specification<Execute> specification, Pageable pageable);

    Execute findByScheduleId(Integer scheduleId);

    Execute findById(Integer id);

    /**
     * 设置安装工人
     * @param id
     * @param number
     * @param worker
     * @param request
     */
    void setWorker(Integer id, String number, String worker, HttpServletRequest request);
}
