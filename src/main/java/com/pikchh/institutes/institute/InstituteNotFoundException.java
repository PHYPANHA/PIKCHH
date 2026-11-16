package com.pikchh.institutes.institute;

import java.util.UUID;

public class InstituteNotFoundException extends RuntimeException {

    public InstituteNotFoundException(UUID id) {
        super("Institute not found: " + id);
    }
}
