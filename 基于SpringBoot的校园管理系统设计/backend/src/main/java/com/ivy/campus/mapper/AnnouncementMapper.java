package com.ivy.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ivy.campus.dto.AnnouncementQuery;
import com.ivy.campus.entity.Announcement;
import com.ivy.campus.vo.CategoryStatVO;
import com.ivy.campus.vo.TrendVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {

    IPage<Announcement> selectAnnouncementPage(IPage<Announcement> page, @Param("q") AnnouncementQuery query);

    List<CategoryStatVO> selectCategoryStats(@Param("days") Integer days);

    List<TrendVO> selectPublishTrend(@Param("days") Integer days);

    List<Announcement> selectTopViewed(@Param("limit") Integer limit);

    // 统计指定状态的公告总数
    Long countByStatus(@Param("status") String status);

    // 待审核公告列表
    List<Announcement> selectPendingList();

    // 到点待发布公告列表
    List<Announcement> selectPublishableList();
}
