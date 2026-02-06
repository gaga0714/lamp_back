package com.lamp.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordDTO {
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
