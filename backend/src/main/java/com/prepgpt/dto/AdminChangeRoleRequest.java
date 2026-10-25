package com.prepgpt.dto;

import jakarta.validation.constraints.NotBlank;

public class AdminChangeRoleRequest {

    @NotBlank(message = "newRole must not be blank")
    private String newRole;

    public AdminChangeRoleRequest() {}

    public AdminChangeRoleRequest(String newRole) {
        this.newRole = newRole;
    }

    public String getNewRole() { return newRole; }
    public void setNewRole(String newRole) { this.newRole = newRole; }
}
