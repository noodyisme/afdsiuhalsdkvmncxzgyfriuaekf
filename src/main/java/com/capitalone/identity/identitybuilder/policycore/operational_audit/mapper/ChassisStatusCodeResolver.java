package com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper;

import com.capitalone.chassis.engine.dfs.emitter.core.support.AuditClientHelper;
import com.capitalone.chassis.engine.model.audit.ExceptionData;
import com.capitalone.chassis.engine.model.context.RequestContext;
import com.capitalone.chassis.engine.model.response.ResponseData;
import lombok.AllArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.lang.NonNull;

import java.util.Optional;

@AllArgsConstructor
public class ChassisStatusCodeResolver {

    private final AuditClientHelper chassisAuditClientHelper;
    private final ConversionService chassisConversionService;

    /**
     * Resolves the chassis status code using the exception if present and containing the status, otherwise
     * infers the status code from the supplied chassis request context
     * @param chassisRequestContext the chassis request context
     * @param exceptionOptional the optional exception
     * @return the status code
     */
    public Integer resolveHttpStatusCode(
            final @NonNull RequestContext chassisRequestContext,
            final @NonNull Optional<Throwable> exceptionOptional) {
        final ResponseData responseData = chassisRequestContext.getResponseData();
        try {
            if (exceptionOptional.isPresent()) {
                Optional<ExceptionData> exceptionDataOptional =
                        Optional.ofNullable(chassisConversionService.convert(exceptionOptional.get(), ExceptionData.class));
                return chassisAuditClientHelper.resolveHttpStatusCode(exceptionDataOptional, responseData);
            } else {
                return chassisAuditClientHelper.resolveHttpStatusCode(Optional.empty(), responseData);
            }
        } catch (ConverterNotFoundException e) {
            return 500;
        }
    }
}
