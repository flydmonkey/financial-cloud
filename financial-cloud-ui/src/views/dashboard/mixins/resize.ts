import { debounce } from '@/utils'

/**
 * Chart resize mixin (Options API).
 * Host chart components provide `this.chart`.
 */
const resizeMixin = {
  data() {
    return {
      $_sidebarElm: null as HTMLElement | null,
      $_resizeHandler: null as ((...args: any[]) => void) | null
    }
  },
  mounted(this: any) {
    this.initListener()
  },
  activated(this: any) {
    if (!this.$_resizeHandler) {
      this.initListener()
    }
    this.resize()
  },
  beforeDestroy(this: any) {
    this.destroyListener()
  },
  deactivated(this: any) {
    this.destroyListener()
  },
  methods: {
    $_sidebarResizeHandler(this: any, e: TransitionEvent) {
      if (e.propertyName === 'width') {
        this.$_resizeHandler?.()
      }
    },
    initListener(this: any) {
      this.$_resizeHandler = debounce(() => {
        this.resize()
      }, 100, false)
      window.addEventListener('resize', this.$_resizeHandler)

      this.$_sidebarElm = (document.getElementsByClassName('sidebar-container')[0] as HTMLElement) || null
      this.$_sidebarElm?.addEventListener('transitionend', this.$_sidebarResizeHandler)
    },
    destroyListener(this: any) {
      if (this.$_resizeHandler) {
        window.removeEventListener('resize', this.$_resizeHandler)
      }
      this.$_resizeHandler = null

      this.$_sidebarElm?.removeEventListener('transitionend', this.$_sidebarResizeHandler)
    },
    resize(this: any) {
      const { chart } = this
      chart && chart.resize()
    }
  }
}

export default resizeMixin as any
