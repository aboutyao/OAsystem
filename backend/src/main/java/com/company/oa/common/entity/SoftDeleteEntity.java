package com.company.oa.common.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SoftDeleteEntity extends BaseEntity {

    private Integer deleted;
}
