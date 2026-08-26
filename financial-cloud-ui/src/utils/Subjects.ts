import booksSetStore from "@/store/modules/bookStore";
import {h, VNode} from "vue";
import {formatAmount} from "@/utils/index";
import {TableColumnCtx} from "element-plus";
import Decimal from 'decimal.js'

export interface SummaryMethodProps<T = any> {
    columns: TableColumnCtx<T>[]
    data: T[]
}

export function hasBookAuxiliary(subjectMap: Record<string, any>) {
    return Object.values(subjectMap).some((item: any) => {
        const auxiliary = item?.auxiliary
        return Array.isArray(auxiliary) && auxiliary.length > 0
    })
}

export function subjectMatchesKeyword(data: any, keyword: string): boolean {
    if (!keyword) {
        return true
    }
    const kw = keyword.toLowerCase().trim()
    if (!kw) {
        return true
    }
    const fields = [
        data?.name,
        data?.code,
        data?.displayName,
        data?.pinyinCode,
        data?.pinyinDisplayCode,
        data?.pinyinFull,
        data?.pinyinDisplayFull,
    ]
    return fields.some((field) => field != null && String(field).toLowerCase().includes(kw))
}

export const cascaderSubjectProps = {
    expandTrigger: 'hover',
    label: 'name',
    value: 'code',
    children: 'children',
    checkStrictly: false,
    emitPath: false,
    defaultExpandAll: true,
    showAllLevels: false,
    clearable: false,
    filterable: true,
    filterMethod: (item: any, keyword: any) => {
        if (!keyword) return true
        const kw = String(keyword).toLowerCase()
        const data = item?.data ?? item
        if (subjectMatchesKeyword(data, kw)) {
            return true
        }
        return String(item?.text ?? '').toLowerCase().includes(kw)
            || String(item?.value ?? '').toLowerCase().includes(kw)
    }
}

export function getSubjectDisplayName(subject?: { displayName?: string; name?: string }) {
    if (!subject) {
        return ''
    }
    if (subject.displayName) {
        return subject.displayName
    }
    if (!subject.name) {
        return ''
    }
    const dashIndex = subject.name.indexOf('-')
    if (dashIndex > -1) {
        return subject.name.slice(dashIndex + 1)
    }
    return subject.name
}

export function indexSubjectTree(nodes: any[], subjectMap: Record<string, any>) {
    if (!nodes?.length) {
        return
    }
    for (const item of nodes) {
        const code = item.code != null ? String(item.code) : ''
        if (code) {
            subjectMap[code] = item
        }
        if (item.children?.length) {
            indexSubjectTree(item.children, subjectMap)
        }
    }
}

export function formatSubjectLabel(
    subjectCode: string | undefined | null,
    subjectMap: Record<string, { displayName?: string; name?: string }>
) {
    if (!subjectCode) {
        return ''
    }
    const code = String(subjectCode)
    const displayName = getSubjectDisplayName(subjectMap[code])
    return displayName ? `${code} ${displayName}` : code
}


/**
 * 获取科目缩进
 * @param subjectCode
 */
export function getSubjectIndent(subjectCode: string) {
    const booksSet = booksSetStore()
    let num = 0
    let start = 0
    for (let i = 0; i < booksSet.subjectCodeLen.length; i++) {
        const len = booksSet.subjectCodeLen[i]
        if (subjectCode.length > start + len) {
            start = start + len
            num += 1
        } else {
            break
        }
    }

    return num.toFixed(0)
}


/**
 * 获取科目余额的所有节点id
 * @param data
 */
export function getSubjectAllNodeIds(data: any) {
    const ids: any = [];

    function recurse(nodes: any) {
        nodes.forEach((node: any) => {
            ids.push(node.sourceId);
            if (node.children) {
                recurse(node.children);
            }
        });
    }

    recurse(data);
    return ids;
}

/**
 * 合计行
 * @param param 列表参数
 * @param recordsAll 自定义数据列
 * @param summaryTextIdx 显示文本的列索引
 * @param summaryValueIdx 显示数值的列索引
 */
export function handleSummaryMethod(param: SummaryMethodProps, recordsAll?: any, summaryTextIdx: number = 0, summaryValueIdx: Array<number> = []) {
    const {columns, data} = param
    const sums: (string | VNode)[] = []
    columns.forEach((column, index) => {
        if (index === summaryTextIdx) {
            sums[index] = h('div', {style: {}}, ['合计'])
            return
        }
        if (summaryValueIdx.length > 0 && !summaryValueIdx.includes(index)) {
            return;
        }

        const values = (recordsAll || data).filter((item: any) => {
            return true
        }).map((item: any) => Number(item[column.property]))
        if (!values.every((value: any) => Number.isNaN(value))) {
            sums[index] = formatAmount(values.reduce((total: Decimal, curr: any) => {
                if (!total) {
                    return new Decimal(curr || 0)
                }
                return total.plus(new Decimal(curr || 0));
            }, 0), '￥ 0')
        } else {
            sums[index] = ''
        }
    })

    return sums
}

