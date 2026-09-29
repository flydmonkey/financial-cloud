import useDictStore from '@/store/modules/dict'
import distData from '@/utils/DistData'
import {ref, toRefs} from "vue";

/**
 * 获取字典数据
 */
/** 返回 any：调用方常从 proxy?.useDict 解构，具体键无法用固定接口表达 */
export function useDict(...args: string[]): any {
    const res: any = ref({});
    return (() => {
        args.forEach((dictType: string) => {
            res.value[dictType] = [];
            const dicts: any = useDictStore().getDict(dictType);
            if (dicts) {
                res.value[dictType] = dicts;
            } else {
                res.value[dictType] = distData[dictType];
                useDictStore().setDict(dictType, res.value[dictType]);
            }
        });
        return toRefs(res.value);
    })();
}

