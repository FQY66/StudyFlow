package com.sf.exception;

/**
 * 员工已存在异常
 */
public class EmployeeAlreadyExistsException extends BaseException {

    public EmployeeAlreadyExistsException() {
    }

    public EmployeeAlreadyExistsException(String msg) {
        super(msg);
    }
}