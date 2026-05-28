package com.company.oa.contract.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.contract.ContractInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ContractInfoMapper extends BaseMapper<ContractInfo> {

    @Update("update contract_info set status = #{status}, updated_at = #{updatedAt}, version = version + 1 where id = #{id} and deleted = 0")
    int updateStatusById(@Param("id") long id, @Param("status") String status, @Param("updatedAt") LocalDateTime updatedAt);

    @Update("update contract_info set status = #{status}, process_instance_id = null, wf_instance_id = null, updated_at = #{updatedAt}, version = version + 1 where id = #{id} and deleted = 0")
    int updateStatusClearFlowKeysById(@Param("id") long id, @Param("status") String status, @Param("updatedAt") LocalDateTime updatedAt);

    @Select("""
            select id, contract_no as contractNo, contract_name as contractName, end_date as endDate
            from contract_info
            where deleted = 0 and status in ('SIGNED', 'APPROVED')
              and created_by = #{userId}
              and end_date between #{from} and #{to}
            order by end_date asc
            """)
    List<Map<String, Object>> selectExpiringContracts(@Param("userId") long userId,
                                                       @Param("from") LocalDate from,
                                                       @Param("to") LocalDate to);

    @Select("""
            select count(*) from contract_info
            where deleted = 0 and status in ('SIGNED', 'APPROVED')
              and end_date between #{from} and #{to}
            """)
    Long countExpiringContracts(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
