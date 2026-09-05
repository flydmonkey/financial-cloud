import router from './router'
import {ElMessage} from 'element-plus'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import {getToken} from '@/utils/Auth'
import {isHttp} from '@/utils/Validate'
import useUserStore from '@/store/modules/user'
import booksSetStore from '@/store/modules/bookStore'
import useSettingsStore from '@/store/modules/settings'
import usePermissionStore from '@/store/modules/permission'
import {loginPreGet} from "@/api/login.js";
import {resolveInstitutionLogo} from "@/constants/branding";
import appStore from "@/store/modules/app.js";

NProgress.configure({showSpinner: false});

const whiteList: any = ['/login', '/register', '/callback', '/forgot'];
const authWhiteList: any = ['/onboarding', '/no-access'];

function firstMenuPath(routes: any[]): string | null {
    for (const route of routes || []) {
        if (route.redirect && route.redirect !== 'noRedirect') {
            return route.redirect
        }
        const child = route.children?.[0]
        if (child?.path) {
            return child.path.startsWith('/') ? child.path : `/${child.path}`
        }
        if (route.path && route.path !== '/' && route.path !== '') {
            return route.path.startsWith('/') ? route.path : `/${route.path}`
        }
    }
    return null
}

router.beforeEach(async (to: any, from: any, next: any) => {
    NProgress.start();
    const token = getToken();

    if (token) {
        to.meta.title && useSettingsStore().setTitle(to.meta.title);

        if (to.path === '/login') {
            next({path: '/'});
            NProgress.done();
        } else if (whiteList.includes(to.path) || authWhiteList.includes(to.path)) {
            next();
        } else {
            const userStore = useUserStore();

            if (userStore.roles.length === 0) {
                try {
                    const res = await loginPreGet();
                    if (res.code === 0) {
                        const staticAppInfo: any = res.data.inst;
                        staticAppInfo.logo = resolveInstitutionLogo(staticAppInfo.logo);
                        appStore().setAppInfo(staticAppInfo);
                    }

                    // 👉 等待账套加载完成
                    await booksSetStore().refreshData();

                    await userStore.currentUser();

                    if (booksSetStore().setList.length === 0) {
                        if (to.path !== '/onboarding') {
                            next({path: '/onboarding', replace: true});
                            NProgress.done();
                            return;
                        }
                        next();
                        return;
                    }

                    if (to.path === '/onboarding') {
                        next({path: '/', replace: true});
                        return;
                    }

                    const accessRoutes = await usePermissionStore().generateRoutes();

                    accessRoutes.forEach((route: any) => {
                        if (!isHttp(route.path)) {
                            router.addRoute(route);
                        }
                    });

                    const homePath = firstMenuPath(accessRoutes)
                    if (!homePath) {
                        ElMessage.warning('当前账套未分配菜单权限，请联系账套管理员')
                        next({path: '/no-access', replace: true})
                        return
                    }

                    if (to.path === '/' || to.path === '/index') {
                        next({path: homePath, replace: true})
                        return
                    }

                    next({...to, replace: true});
                } catch (err: any) {
                    console.error(err);
                    await userStore.logOut();
                    ElMessage.error(err);
                    next({path: '/login'});
                }
            } else {
                const booksStore = booksSetStore();
                if (booksStore.setList.length === 0 && to.path !== '/onboarding') {
                    next({path: '/onboarding', replace: true});
                    NProgress.done();
                    return;
                }
                if (booksStore.setList.length > 0 && to.path === '/onboarding') {
                    next({path: '/', replace: true});
                    return;
                }
                next();
            }
        }
    } else {
        if (whiteList.includes(to.path)) {
            next();
        } else {
            next(`/login?redirect=${to.fullPath}`);
            NProgress.done();
        }
    }
});

router.afterEach(() => {
    NProgress.done()
})
