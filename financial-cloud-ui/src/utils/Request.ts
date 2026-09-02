import axios from 'axios'
import {ElLoading, ElMessage, ElMessageBox} from 'element-plus'
import {
    getRefreshToken,
    getToken,
    setToken,
    setRefreshToken,
    setTokenInfo,
} from '@/utils/Auth'
import errorCode from '@/utils/ErrorCode'
import {blobValidate, tansParams} from '@/utils/financialCloud'
import cache from '@/plugins/cache'
import {saveAs} from "file-saver"
import useUserStore from '@/store/modules/user'
import {getLang} from "@/languages";


let downloadLoadingInstance: any;
export const isRelogin: any = {show: false};
let isRefreshing: any = false;
let requests: any = [];

axios.defaults.headers['Content-Type'] = 'application/json;charset=utf-8'

const service: any = axios.create({
    baseURL: import.meta.env.VITE_APP_BASE_API,
    timeout: 60000
})

const serviceRefresh: any = axios.create({
    baseURL: import.meta.env.VITE_APP_BASE_API,
    timeout: 15000
})

function showReLoginToast(): any {
    if (!isRelogin.show) {
        isRefreshing = false;
        isRelogin.show = true;
        ElMessageBox.confirm(
            'Login expired. Please sign in again.',
            'System Notice',
            {confirmButtonText: 'Sign in', cancelButtonText: 'Cancel', type: 'warning'}
        )
            .then(() => {
                isRelogin.show = false;
                useUserStore().logOut().finally(() => {
                    window.location.reload()
                })
            })
            .catch(() => {
                isRelogin.show = false;
            });
    }
}

function refreshToken(token: any): any {
    return new Promise((resolve: any) => {
        serviceRefresh({
            url: '/auth/token/refresh?refresh_token=' + token,
            headers: {
                isToken: false
            },
            method: 'post'
        }).then((res: any) => {
            if (res.data.code === 0) {
                resolve(res.data)
            } else {
                showReLoginToast()
            }
        }).catch(() => {
            showReLoginToast()
        })
    })
}

service.interceptors.request.use((config: any) => {
    config.headers['Accept-Language'] = getLang();

    const isToken: any = (config.headers || {}).isToken === false
    const isRepeatSubmit: any = (config.headers || {}).repeatSubmit === false
    if (getToken() && !isToken) {
        config.headers['Authorization'] = useUserStore().headerType + ' ' + getToken()
    }
    if (config.method === 'get' && config.params) {
        let url: any = config.url + '?' + tansParams(config.params);
        url = url.slice(0, -1);
        config.params = {};
        config.url = url;
    }
    if (!isRepeatSubmit && (config.method === 'post' || config.method === 'put')) {
        const requestObj: any = {
            url: config.url,
            data: typeof config.data === 'object' ? JSON.stringify(config.data) : config.data,
            time: new Date().getTime()
        }
        const requestSize: any = Object.keys(JSON.stringify(requestObj)).length;
        const limitSize: any = 5 * 1024 * 1024;
        if (requestSize >= limitSize) {
            console.warn(`[${config.url}]: request payload exceeds 5M repeat-submit guard`)
            return config;
        }
        const sessionObj: any = cache.session.getJSON('sessionObj')
        if (sessionObj === undefined || sessionObj === null || sessionObj === '') {
            cache.session.setJSON('sessionObj', requestObj)
        } else {
            const s_url: any = sessionObj.url;
            const s_data: any = sessionObj.data;
            const s_time: any = sessionObj.time;
            const interval: any = 1000;
            if (s_data === requestObj.data && requestObj.time - s_time < interval && s_url === requestObj.url) {
                const message: any = 'Request is being processed, please do not resubmit';
                console.warn(`[${s_url}]: ` + message)
                return Promise.reject(new Error(message))
            } else {
                cache.session.setJSON('sessionObj', requestObj)
            }
        }
    }
    return config
}, (error: any) => {
    console.error(error)
    Promise.reject(error)
})

service.interceptors.response.use((res: any) => {
        const code: any = res.data.code || 0;
        const msg: any = errorCode[code] || res.data.message || errorCode['default']
        if (res.request.responseType === 'blob' || res.request.responseType === 'arraybuffer') {
            return res.data
        }
        if (code === 401) {
            return err401(res)
        } else if (code === 500) {
            ElMessage({message: msg, type: 'error'})
            return Promise.reject(res.data)
        } else if (code === 601) {
            ElMessage({message: msg, type: 'warning'})
            return Promise.reject(res.data)
        } else if (code === 2) {
            ElMessage({message: msg, type: 'error'})
            return Promise.reject(res.data)
        } else if (code === 0) {
            return Promise.resolve(res.data)
        } else {
            ElMessage({
                type: 'error',
                message: msg,
                dangerouslyUseHTMLString: true
            });
            return Promise.reject(res.data)
        }
    },
    (error: any) => {
        let {message, response} = error;
        console.error(error)
        if (response?.status === 401) {
            return err401(response)
        }

        if (message === "Network Error") {
            message = "Backend connection failed";
        } else if (message.includes("timeout")) {
            message = "Request timed out";
        } else if (message.includes("Request failed with status code")) {
            message = "Request failed with status code " + message.substring(message.length - 3);
        }

        ElMessage({message: message, type: 'error', duration: 5 * 1000})
        return Promise.reject(error)
    }
)

function err401(res: any): any {
    if (!isRefreshing) {
        isRefreshing = true;
        try {
            const refreshTokenStr: any = getRefreshToken();
            if (!refreshTokenStr) {
                showReLoginToast()
                return
            }
            refreshToken(refreshTokenStr).then((refreshRes: any) => {
                setToken(refreshRes.data.token)
                setRefreshToken(refreshRes.data.refresh_token)
                setTokenInfo(refreshRes.data)
                useUserStore().token = refreshRes.data.token
                isRefreshing = false;
                requests.forEach((cb: any) => cb(refreshRes.data.token));
                requests = [];
            })
        } catch (err) {
            console.error(err)
            if (!isRelogin.show) {
                showReLoginToast()
            } else {
                return Promise.reject('Login expired')
            }
        }
    }
    return new Promise((resolve: any) => {
        requests.push((token: any) => {
            res.config.headers['Authorization'] = 'Bearer ' + token;
            resolve(service(res.config));
        });
    });
}

export function download(url: any, params: any, filename: any, config: any): any {
    downloadLoadingInstance = ElLoading.service({text: "Downloading...", background: "rgba(0, 0, 0, 0.7)",})
    return service.post(url, params, {
        transformRequest: [(params: any) => {
            return tansParams(params)
        }],
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        responseType: 'blob',
        ...config
    }).then(async (data: any) => {
        const isBlob: any = blobValidate(data);
        if (isBlob) {
            const blob: any = new Blob([data])
            saveAs(blob, filename)
        } else {
            const resText: any = await data.text();
            const rspObj: any = JSON.parse(resText);
            const errMsg: any = errorCode[rspObj.code] || rspObj.msg || errorCode['default']
            ElMessage.error(errMsg);
        }
        downloadLoadingInstance.close();
    }).catch((r: any) => {
        console.error(r)
        ElMessage.error('Download failed')
        downloadLoadingInstance.close();
    })
}

export default service
