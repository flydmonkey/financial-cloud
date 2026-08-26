package com.financial.cloud.constants;

import java.util.ArrayList;
import java.util.List;

import com.financial.cloud.authn.core.Authority;
import com.financial.cloud.authn.core.SimpleAuthority;

/**
 * ROLES.
 */
public final class ConstsRoles {

    public static final SimpleAuthority ROLE_SUPERVISOR = new SimpleAuthority("ROLE_SUPERVISOR");

    public static final SimpleAuthority ROLE_ADMINISTRATOR = new SimpleAuthority("ROLE_ADMINISTRATOR");

    public static final SimpleAuthority ROLE_MANAGER = new SimpleAuthority("ROLE_MANAGER");

    public static final SimpleAuthority ROLE_USER = new SimpleAuthority("ROLE_USER");

    public static final SimpleAuthority ROLE_ALL_USER = new SimpleAuthority("ROLE_ALL_USER");

    /**
     * 普通组/角色
     */
    public static final SimpleAuthority ROLE_GENERAL_USER = new SimpleAuthority("ROLE_GENERAL_USER");

    public static final List<Authority> grantedAdminAuthoritys = new ArrayList<>();

    /**
     * 模型
     */
    public class Pattern {
        /**
         * 动态模型
         */
        public static final String DYNAMIC = "dynamic";
        /**
         * 静态模型
         */
        public static final String STATIC = "static";

    }

    /**
     * 类型
     */
    public class Category {
        /**
         * 超级管理员
         */
        public static final String SUPERVISOR = "supervisor";
        /**
         * 管理员
         */
        public static final String ADMINISTRATOR = "administrator";
        /**
         * 授权管理员-分级管理员
         */
        public static final String MANAGER = "manager";
        /**
         * 普通
         */
        public static final String GENERAL = "general";

    }

    /**
     * 管理员角色
     */
    static {
        //超级管理员
        grantedAdminAuthoritys.add(ConstsRoles.ROLE_SUPERVISOR);
        //管理员
        grantedAdminAuthoritys.add(ConstsRoles.ROLE_ADMINISTRATOR);
        //授权管理员
        grantedAdminAuthoritys.add(ConstsRoles.ROLE_MANAGER);
    }
}
