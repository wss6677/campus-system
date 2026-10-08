package com.ivy.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ivy.campus.entity.Receipt;
import com.ivy.campus.vo.DeptUnreadVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReceiptMapper extends BaseMapper<Receipt> {

    List<DeptUnreadVO> selectUnreadRank(@Param("announcementId") Long announcementId);

    Long countUnreadByAnnouncement(@Param("announcementId") Long announcementId);

    // C 组统计口径补充：部门维度未读排行（announcementId 为空时统计全校近30天）
    @Select("<script>"
            + "select r.dept_id as deptId, max(d.name) as deptName, count(1) as total, "
            + "sum(case when r.read_flag = 0 then 1 else 0 end) as unread, "
            + "round(sum(case when r.read_flag = 0 then 1 else 0 end) * 100.0 / count(1), 2) as rate "
            + "from ann_receipt r left join sys_dept d on d.id = r.dept_id "
            + "where r.dept_id is not null "
            + "<if test='announcementId != null'> and r.announcement_id = #{announcementId} </if>"
            + "group by r.dept_id order by unread desc, total desc"
            + "</script>")
    List<DeptUnreadVO> selectDeptUnreadRank(@Param("announcementId") Long announcementId);
}
