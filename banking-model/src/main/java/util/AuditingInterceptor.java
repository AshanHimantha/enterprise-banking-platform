package util;

import annotation.Audit;
import entity.AuditLog;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext; // Keep this one
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.security.enterprise.SecurityContext;
import java.io.Serializable;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Arrays;

@Interceptor
@Audit
public class AuditingInterceptor implements Serializable {
    @Inject
    private Provider<SecurityContext> securityContextProvider;

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @AroundInvoke
    public Object createAuditLog(InvocationContext ctx) throws Exception {
        Object result;
        try {
            result = ctx.proceed();
            logAction(ctx, "SUCCESS");
        } catch (Exception e) {

            logAction(ctx, "FAILED: " + e.getMessage());
            throw e;
        }
        return result;
    }
    private void logAction(InvocationContext ctx, String status) {
        try {
            SecurityContext securityContext = securityContextProvider.get();
            Principal principal = securityContext.getCallerPrincipal();
            String principalName = (principal != null) ? principal.getName() : "SYSTEM";

            AuditLog log = new AuditLog();
            log.setPrincipalName(principalName);
            log.setAction(ctx.getTarget().getClass().getSimpleName() + "." + ctx.getMethod().getName());
            log.setTimestamp(LocalDateTime.now());

            String details = "Status: " + status + ", Params: " + Arrays.toString(ctx.getParameters());
            log.setDetails(details.substring(0, Math.min(details.length(), 255)));

            em.persist(log);
        } catch (Exception e) {
            System.err.println("!!! AUDIT LOGGING FAILED !!!");
            e.printStackTrace();
        }
    }
}