package util;


import annotation.Logging;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import java.io.Serializable;

@Logging
@Interceptor
public class LoggingInterceptor implements Serializable {
    @AroundInvoke
    public Object logMethodCall(InvocationContext ctx) throws Exception {
        String className = ctx.getTarget().getClass().getSimpleName();
        String methodName = ctx.getMethod().getName();

        System.out.println("INTERCEPTOR-LOG ::: ENTER ::: " + className + " >>> " + methodName);

        long startTime = System.currentTimeMillis();

        try {
            Object result = ctx.proceed();
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("INTERCEPTOR-LOG ::: EXIT ::: " + className + " >>> " + methodName + " (Duration: " + duration + "ms)");
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            System.err.println("INTERCEPTOR-LOG ::: EXCEPTION ::: " + className + " >>> " + methodName + " | Error: " + e.getMessage());
            throw e;
        }
    }
}