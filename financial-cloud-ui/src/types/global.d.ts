declare global {
  /**
   * 弹窗属性
   */
  interface DialogOption {
    /**
     * 弹窗标题
     */
    title?: string
    /**
     * 是否显示
     */
    visible: boolean
    type: number
  }
}
export {}
