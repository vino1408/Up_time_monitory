
package com.vinoth.uptimemonitor.validation;

import com.vinoth.uptimemonitor.dto.MonitorRequest;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MonitorConfigurationValidator
        implements ConstraintValidator<
                ValidMonitorConfiguration,
                MonitorRequest> {

    @Override
    public boolean isValid(
            MonitorRequest request,
            ConstraintValidatorContext context) {

        if (request == null) {
            return true;
        }

        Integer interval = request.getIntervalSeconds();
        Integer timeout = request.getTimeoutSeconds();

        // Let @NotNull validate missing values.
        if (interval == null || timeout == null) {
            return true;
        }

        if (timeout >= interval) {
            context.disableDefaultConstraintViolation();

            context.buildConstraintViolationWithTemplate(
                    "Timeout must be less than interval"
            )
            .addPropertyNode("timeoutSeconds")
            .addConstraintViolation();

            return false;
        }

        return true;
    }
}
