package com.campus.xianyu.auth;

import com.campus.xianyu.user.UserResponse;

public record LoginResponse(String token, UserResponse user) {
}
