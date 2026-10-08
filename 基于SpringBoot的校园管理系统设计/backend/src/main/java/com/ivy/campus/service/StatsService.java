package com.ivy.campus.service;

import com.ivy.campus.vo.AnnouncementVO;
import com.ivy.campus.vo.CategoryStatVO;
import com.ivy.campus.vo.DeptUnreadVO;
import com.ivy.campus.vo.StatsOverviewVO;
import com.ivy.campus.vo.TrendVO;

import java.util.List;

public interface StatsService {

    StatsOverviewVO overview();

    List<TrendVO> trend(Integer days);

    List<CategoryStatVO> categoryStats(Integer days);

    List<DeptUnreadVO> deptUnread();

    List<AnnouncementVO> topViewed(Integer limit);
}
