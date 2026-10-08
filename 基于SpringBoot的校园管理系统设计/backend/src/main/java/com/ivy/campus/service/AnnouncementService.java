package com.ivy.campus.service;

import com.ivy.campus.common.PageResult;
import com.ivy.campus.dto.AnnouncementDTO;
import com.ivy.campus.dto.AnnouncementQuery;
import com.ivy.campus.dto.AuditDTO;
import com.ivy.campus.vo.AnnouncementDetailVO;
import com.ivy.campus.vo.AnnouncementVO;

import java.util.List;

public interface AnnouncementService {

    PageResult<AnnouncementVO> page(AnnouncementQuery query);

    AnnouncementDetailVO detail(Long id, boolean countView);

    Long saveDraft(AnnouncementDTO dto);

    void update(Long id, AnnouncementDTO dto);

    void submit(Long id);

    void audit(Long id, AuditDTO dto);

    void publish(Long id);

    void withdraw(Long id);

    void archive(Long id);

    void toggleTop(Long id);

    void toggleBanner(Long id);

    void remove(Long id);

    List<AnnouncementVO> latest(Integer limit);

    List<AnnouncementVO> banner();
}
