package com.company.oa.meeting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.meeting.MeetingBooking;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface MeetingBookingMapper extends BaseMapper<MeetingBooking> {

    @Select("""
            select b.id, b.room_id as roomId, r.room_name as roomName, b.title, b.start_at as startAt, b.end_at as endAt,
                   b.organizer_id as organizerId, b.participant_count as participantCount, b.status,
                   b.cancel_reason as cancelReason, b.created_at as createdAt, b.updated_at as updatedAt
            from meeting_booking b join meeting_room r on r.id = b.room_id and r.deleted = 0
            where b.id = #{id}
            """)
    Map<String, Object> selectBookingWithRoom(@Param("id") long id);

    @Select("""
            <script>
            select b.id, b.room_id as roomId, r.room_name as roomName, b.title, b.start_at as startAt, b.end_at as endAt,
                   b.organizer_id as organizerId, b.participant_count as participantCount, b.status,
                   b.cancel_reason as cancelReason, b.created_at as createdAt
            from meeting_booking b
            join meeting_room r on r.id = b.room_id and r.deleted = 0
            where 1 = 1
            <if test="roomId != null">
              and b.room_id = #{roomId}
            </if>
            <if test="organizerId != null">
              and b.organizer_id = #{organizerId}
            </if>
            order by b.start_at desc, b.id desc
            limit #{limit} offset #{offset}
            </script>
            """)
    List<Map<String, Object>> selectBookingsWithRoom(@Param("roomId") Long roomId,
                                                      @Param("organizerId") Long organizerId,
                                                      @Param("limit") long limit,
                                                      @Param("offset") long offset);

    @Select("""
            <script>
            select count(*) from meeting_booking b where 1 = 1
            <if test="roomId != null">
              and b.room_id = #{roomId}
            </if>
            <if test="organizerId != null">
              and b.organizer_id = #{organizerId}
            </if>
            </script>
            """)
    long countBookings(@Param("roomId") Long roomId, @Param("organizerId") Long organizerId);

    @Select("""
            <script>
            select count(*) from meeting_booking b
            where b.room_id = #{roomId} and b.status = #{status}
            and not (b.end_at &lt;= #{start} or b.start_at &gt;= #{end})
            <if test="excludeId != null">
              and b.id != #{excludeId}
            </if>
            </script>
            """)
    long countOverlapping(@Param("roomId") long roomId,
                          @Param("status") String status,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end,
                          @Param("excludeId") Long excludeId);
}
