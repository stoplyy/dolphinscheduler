package org.apache.dolphinscheduler.api.platform.dto.solaris;

/**
 * sre定义的角色 1-developer; 2-owner; 3-leader；4-第一负责人
 */
public enum AppRole {

    DEVELOPER("1"), OWNER("2"),
    ;

    AppRole(String role) {
        this.role = role;
    }

    private String role;

    public static AppRole role(String role) {
        if ("3".equals(role)) {
            return OWNER;
        }
        if ("4".equals(role)) {
            return OWNER;
        }
        for (AppRole value : values()) {
            if (value.role.equals(role)) {
                return value;
            }
        }
        return DEVELOPER;
    }

    @Override
    public String toString() {
        return this.name();
    }
}
