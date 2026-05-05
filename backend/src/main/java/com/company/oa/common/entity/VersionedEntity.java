package com.company.oa.common.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class VersionedEntity extends SoftDeleteEntity {

    private Integer version;
}
