package com.company.oa.audit;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {
    String action();
    String entityType() default "";
    String description() default "";
    boolean captureDiff() default false;
}
