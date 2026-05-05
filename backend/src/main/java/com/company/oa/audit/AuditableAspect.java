package com.company.oa.audit;

import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class AuditableAspect {
    private static final Logger log = LoggerFactory.getLogger(AuditableAspect.class);

    private final AuditService auditService;
    private final AuthService authService;

    public AuditableAspect(AuditService auditService, AuthService authService) {
        this.auditService = auditService;
        this.authService = authService;
    }

    @Around("@annotation(com.company.oa.auditable.Auditable)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        Method method = sig.getMethod();
        Auditable annotation = method.getAnnotation(Auditable.class);

        Object result = joinPoint.proceed();

        try {
            AuthUser user = authService.currentUser();
            auditService.safeRecordOperation(
                    user.id(),
                    annotation.action(),
                    annotation.entityType(),
                    null,
                    null,
                    null
            );
        } catch (Exception e) {
            log.warn("Audit aspect failed for {}: {}", method.getName(), e.getMessage());
        }

        return result;
    }
}
