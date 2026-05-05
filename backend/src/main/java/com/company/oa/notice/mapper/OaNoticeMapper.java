package com.company.oa.notice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.notice.OaNotice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface OaNoticeMapper extends BaseMapper<OaNotice> {

    @Select("""
            select n.id, n.title, n.category, n.publish_at as publishAt, n.top_flag as topFlag,
                   u.real_name as createdByName
            from oa_notice n
            left join org_user u on u.id = n.created_by
            where n.deleted = 0 and n.status = 'PUBLISHED'
            order by n.publish_at desc, n.id desc
            limit #{limit}
            """)
    List<Map<String, Object>> selectRecentNotices(@Param("limit") int limit);
}