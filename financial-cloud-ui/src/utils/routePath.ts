/**
 * Join parent/child paths for vue-router dynamic menus.
 * Absolute child paths (starting with /) are kept as-is.
 */
export function joinRoutePath(parentPath: string | undefined | null, childPath: string): string {
    if (!childPath) {
        return parentPath || '/'
    }
    if (childPath.startsWith('/')) {
        return childPath
    }
    const parent = (parentPath || '').replace(/\/+$/, '')
    const child = childPath.replace(/^\/+/, '')
    if (!parent) {
        return `/${child}`
    }
    return `${parent}/${child}`
}

export function resolveMenuPath(
    requestUrl: string | undefined | null,
    permission: string,
    hasVisibleChildren: boolean,
): string {
    if (requestUrl) {
        return requestUrl
    }
    return hasVisibleChildren ? '' : permission
}
