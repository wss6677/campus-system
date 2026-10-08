package com.ivy.campus.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ivy.campus.common.Constants;
import com.ivy.campus.entity.Announcement;
import com.ivy.campus.entity.Category;
import com.ivy.campus.entity.Comment;
import com.ivy.campus.entity.Dept;
import com.ivy.campus.entity.Receipt;
import com.ivy.campus.entity.User;
import com.ivy.campus.mapper.AnnouncementMapper;
import com.ivy.campus.mapper.CategoryMapper;
import com.ivy.campus.mapper.CommentMapper;
import com.ivy.campus.mapper.DeptMapper;
import com.ivy.campus.mapper.ReceiptMapper;
import com.ivy.campus.mapper.UserMapper;
import com.ivy.campus.service.StatsService;
import com.ivy.campus.vo.AnnouncementVO;
import com.ivy.campus.vo.CategoryStatVO;
import com.ivy.campus.vo.DeptUnreadVO;
import com.ivy.campus.vo.StatsOverviewVO;
import com.ivy.campus.vo.TrendVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsServiceImpl implements StatsService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_ARCHIVED = "ARCHIVED";

    private static final String CACHE_KEY_OVERVIEW = Constants.REDIS_STATS_PREFIX + "overview";

    private static final long CACHE_SECONDS = 60L;

    private static final int MAX_DAYS = 90;

    private final AnnouncementMapper announcementMapper;
    private final UserMapper userMapper;
    private final ReceiptMapper receiptMapper;
    private final CommentMapper commentMapper;
    private final CategoryMapper categoryMapper;
    private final DeptMapper deptMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public StatsOverviewVO overview() {
        StatsOverviewVO cached = readCache(CACHE_KEY_OVERVIEW, StatsOverviewVO.class);
        if (cached != null) {
            return cached;
        }
        StatsOverviewVO vo = new StatsOverviewVO();
        vo.setTotalAnnouncement(nullSafe(announcementMapper.selectCount(Wrappers.<Announcement>lambdaQuery())));
        vo.setTodayPublish(nullSafe(announcementMapper.selectCount(Wrappers.<Announcement>lambdaQuery()
                .eq(Announcement::getStatus, STATUS_PUBLISHED)
                .ge(Announcement::getPublishTime, LocalDate.now().atStartOfDay())
                .lt(Announcement::getPublishTime, LocalDate.now().plusDays(1).atStartOfDay()))));
        vo.setPendingAudit(nullSafe(announcementMapper.selectCount(Wrappers.<Announcement>lambdaQuery()
                .eq(Announcement::getStatus, STATUS_PENDING))));
        vo.setPublished(nullSafe(announcementMapper.selectCount(Wrappers.<Announcement>lambdaQuery()
                .eq(Announcement::getStatus, STATUS_PUBLISHED))));
        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
        vo.setPublishedThisMonth(nullSafe(announcementMapper.selectCount(Wrappers.<Announcement>lambdaQuery()
                .eq(Announcement::getStatus, STATUS_PUBLISHED)
                .ge(Announcement::getPublishTime, firstDayOfMonth.atStartOfDay()))));
        vo.setTotalUser(nullSafe(userMapper.selectCount(Wrappers.<User>lambdaQuery())));
        vo.setTodayActive(nullSafe(userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .ge(User::getLastLoginTime, LocalDate.now().atStartOfDay()))));
        vo.setTotalRead(nullSafe(receiptMapper.selectCount(Wrappers.<Receipt>lambdaQuery()
                .eq(Receipt::getReadFlag, 1))));
        vo.setUnreadUrge(nullSafe(receiptMapper.selectCount(Wrappers.<Receipt>lambdaQuery()
                .eq(Receipt::getReadFlag, 0)
                .gt(Receipt::getUrgeCount, 0))));
        vo.setCommentPending(nullSafe(commentMapper.selectCount(Wrappers.<Comment>lambdaQuery()
                .eq(Comment::getStatus, STATUS_PENDING))));
        vo.setTopCategory(resolveTopCategory());
        writeCache(CACHE_KEY_OVERVIEW, vo);
        return vo;
    }

    @Override
    public List<TrendVO> trend(Integer days) {
        int size = days == null || days <= 0 ? 15 : Math.min(days, MAX_DAYS);
        String key = Constants.REDIS_STATS_PREFIX + "trend:" + size;
        List<TrendVO> cached = readCacheList(key, TrendVO.class);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        List<TrendVO> list = announcementMapper.selectPublishTrend(size);
        List<TrendVO> result = list == null ? new ArrayList<>() : list;
        writeCache(key, result);
        return result;
    }

    @Override
    public List<CategoryStatVO> categoryStats(Integer days) {
        int size = days == null || days <= 0 ? 30 : Math.min(days, MAX_DAYS);
        String key = Constants.REDIS_STATS_PREFIX + "category:" + size;
        List<CategoryStatVO> cached = readCacheList(key, CategoryStatVO.class);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        List<CategoryStatVO> list = announcementMapper.selectCategoryStats(size);
        List<CategoryStatVO> result = list == null ? new ArrayList<>() : list;
        writeCache(key, result);
        return result;
    }

    @Override
    public List<DeptUnreadVO> deptUnread() {
        String key = Constants.REDIS_STATS_PREFIX + "deptUnread";
        List<DeptUnreadVO> cached = readCacheList(key, DeptUnreadVO.class);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        List<DeptUnreadVO> result = receiptMapper.selectDeptUnreadRank(null);
        if (result == null) {
            result = new ArrayList<>();
        }
        writeCache(key, result);
        return result;
    }

    @Override
    public List<AnnouncementVO> topViewed(Integer limit) {
        int size = limit == null || limit <= 0 ? 10 : Math.min(limit, 50);
        List<Announcement> list = announcementMapper.selectTopViewed(size);
        List<AnnouncementVO> result = new ArrayList<>();
        if (list == null || list.isEmpty()) {
            return result;
        }
        for (Announcement item : list) {
            AnnouncementVO vo = new AnnouncementVO();
            BeanUtils.copyProperties(item, vo);
            vo.setStatusText(statusText(item.getStatus()));
            if (item.getCategoryId() != null) {
                Category category = categoryMapper.selectById(item.getCategoryId());
                if (category != null) {
                    vo.setCategoryName(category.getName());
                    vo.setCategoryColor(category.getColor());
                }
            }
            if (item.getDeptId() != null) {
                Dept dept = deptMapper.selectById(item.getDeptId());
                if (dept != null) {
                    vo.setDeptName(dept.getName());
                }
            }
            if (item.getAuthorId() != null) {
                User author = userMapper.selectById(item.getAuthorId());
                if (author != null) {
                    vo.setAuthorName(author.getRealName() == null || author.getRealName().isBlank()
                            ? author.getUsername() : author.getRealName());
                }
            }
            vo.setCommentCount(0L);
            if (vo.getViewCount() == null) {
                vo.setViewCount(0);
            }
            if (vo.getReceiptCount() == null) {
                vo.setReceiptCount(0);
            }
            if (vo.getTargetCount() == null) {
                vo.setTargetCount(0);
            }
            result.add(vo);
        }
        return result;
    }

    private String resolveTopCategory() {
        List<CategoryStatVO> stats = announcementMapper.selectCategoryStats(MAX_DAYS);
        if (stats == null || stats.isEmpty()) {
            return null;
        }
        CategoryStatVO top = null;
        for (CategoryStatVO item : stats) {
            if (item == null || item.getValue() == null) {
                continue;
            }
            if (top == null || top.getValue() == null || item.getValue() > top.getValue()) {
                top = item;
            }
        }
        return top == null ? null : top.getName();
    }

    private Long nullSafe(Long value) {
        return value == null ? 0L : value;
    }

    private String statusText(String status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case "DRAFT":
                return "草稿";
            case STATUS_PENDING:
                return "待审核";
            case STATUS_PUBLISHED:
                return "已发布";
            case "SCHEDULED":
                return "定时发布";
            case "REJECTED":
                return "已驳回";
            case "WITHDRAWN":
                return "已撤回";
            case STATUS_ARCHIVED:
                return "已归档";
            default:
                return status;
        }
    }

    private <T> T readCache(String key, Class<T> clazz) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.warn("读取统计缓存失败，key={}，原因：{}", key, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> readCacheList(String key, Class<T> clazz) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            log.warn("读取统计列表缓存失败，key={}，原因：{}", key, e.getMessage());
            return null;
        }
    }

    private void writeCache(String key, Object value) {
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value),
                    CACHE_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("写入统计缓存失败，key={}，原因：{}", key, e.getMessage());
        }
    }
}
