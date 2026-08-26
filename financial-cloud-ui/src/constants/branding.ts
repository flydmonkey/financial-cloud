const base = import.meta.env.BASE_URL || '/'

export const DEFAULT_LOGO = `${base}logo.svg`

/** 机构 logo：仅接受远程地址或文件服务路径，忽略数据库里残留的旧静态资源路径 */
export function resolveInstitutionLogo(logo?: string | null): string {
  if (!logo) {
    return DEFAULT_LOGO
  }

  const value = logo.trim()
  if (!value) {
    return DEFAULT_LOGO
  }

  if (value.startsWith('http://') || value.startsWith('https://') || value.startsWith('data:')) {
    return value
  }

  if (value.startsWith('/file')) {
    return `${import.meta.env.VITE_APP_BASE_API}${value}`
  }

  return DEFAULT_LOGO
}
