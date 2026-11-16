package com.pikchh.institutes.institute;

public class InstituteConflictException extends RuntimeException {

    public InstituteConflictException(String code) {
        super("An institute with code " + code + " already exists");
    }
}
