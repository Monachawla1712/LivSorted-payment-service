package com.sorted.rest.services.payment.beans;

import lombok.Data;

@Data
public class UserDetail {
    private String id;
    private String name;
    private String phone;
    private String email;

    public static UserDetail newInstance() {
        return new UserDetail();
    }
}
