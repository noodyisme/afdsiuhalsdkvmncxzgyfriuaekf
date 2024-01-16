package com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Value;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;

@Value
public class UpdateTransactionResult implements Serializable {

    public static UpdateTransactionResult success() {
        return new UpdateTransactionResult(Status.SUCCESS, null);
    }

    public static UpdateTransactionResult error(@NotNull Throwable e) {
        return new UpdateTransactionResult(Status.ERROR, e);
    }

    Status status;
    @Nullable
    @JsonIgnore
    Throwable error;

    private UpdateTransactionResult(@NotNull Status status, @Nullable Throwable error) {
        this.status = Objects.requireNonNull(status);
        if (status.equals(Status.SUCCESS) && error != null) throw new IllegalArgumentException("success hasError");

        this.error = status.equals(Status.SUCCESS) ? null : Objects.requireNonNull(error);
    }

    public boolean isSuccess() {
        return Status.SUCCESS.equals(status);
    }

    public boolean isError() {
        return Status.ERROR.equals(status);
    }

    public enum Status {
        SUCCESS,
        ERROR
    }

}
