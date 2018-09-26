package com.mash.api.service;

import com.mash.api.entity.Account;
import com.mash.api.entity.PictureLibrary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface PictureLibraryService {

    Page<PictureLibrary> findByParams(Specification<PictureLibrary> specification, Pageable pageable);

    List<PictureLibrary> findByScheduleNumber(String scheduleNumber);

    List<PictureLibrary> findByScheduleNumberAndState(String scheduleNumber, Integer state);

    PictureLibrary save(PictureLibrary pictureLibrary);

    /**
     * 更新画面状态
     * @param id
     * @param state
     * @param explain
     */
    void updateState(Integer id,
                     Integer state,
                     String explain,
                     HttpServletRequest request);
}
