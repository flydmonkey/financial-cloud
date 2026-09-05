import useUserStore from '@/store/modules/user'

function authPermission(permission: any): any {
    const all_permission: any = "*:*:*";
    const permissions: any = useUserStore().permissions
    if (permission && permission.length > 0) {
        return permissions.some((v: any) => {
            return all_permission === v || v === permission
        })
    } else {
        return false
    }
}

function authRole(role: any): any {
    // Only book-scoped product admin bypasses role checks (not ROLE_SUPERVISOR / 1000).
    const productAdmins: any = ["ROLE_ADMINISTRATORS"];
    const roles: any = useUserStore().roles
    if (role && role.length > 0) {
        return roles.some((v: any) => {
            return productAdmins.includes(v) || v === role
        })
    } else {
        return false
    }
}

export default {
    // 验证用户是否具备某权限
    hasPermi(permission: any) {
        return authPermission(permission);
    },
    // 验证用户是否含有指定权限，只需包含其中一个
    hasPermiOr(permissions: any) {
        return permissions.some((item: any) => {
            return authPermission(item)
        })
    },
    // 验证用户是否含有指定权限，必须全部拥有
    hasPermiAnd(permissions: any) {
        return permissions.every((item: any) => {
            return authPermission(item)
        })
    },
    // 验证用户是否具备某角色
    hasRole(role: any) {
        return authRole(role);
    },
    // 验证用户是否含有指定角色，只需包含其中一个
    hasRoleOr(roles: any) {
        return roles.some((item: any) => {
            return authRole(item)
        })
    },
    // 验证用户是否含有指定角色，必须全部拥有
    hasRoleAnd(roles: any) {
        return roles.every((item: any) => {
            return authRole(item)
        })
    }
}
