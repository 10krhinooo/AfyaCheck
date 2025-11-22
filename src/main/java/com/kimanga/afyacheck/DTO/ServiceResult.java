package com.kimanga.afyacheck.DTO;

import lombok.Data;

@Data
public class ServiceResult<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> ServiceResult<T> success(String message, T data) {
        ServiceResult<T> result = new ServiceResult<>();
        result.setSuccess(true);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    public static <T> ServiceResult<T> failure(String message) {
        ServiceResult<T> result = new ServiceResult<>();
        result.setSuccess(false);
        result.setMessage(message);
        result.setData(null);
        return result;
    }

    public boolean isSuccess() {
        return success;
    }
}