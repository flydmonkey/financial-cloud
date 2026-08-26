import * as echarts from 'echarts/core'
import {
    BarChart,
    LineChart,
    MapChart,
    PieChart,
    RadarChart,
} from 'echarts/charts'
import {
    GeoComponent,
    GridComponent,
    LegendComponent,
    RadarComponent,
    TitleComponent,
    TooltipComponent,
    VisualMapComponent,
} from 'echarts/components'
import {LabelLayout, UniversalTransition} from 'echarts/features'
import {CanvasRenderer} from 'echarts/renderers'

echarts.use([
    BarChart,
    LineChart,
    PieChart,
    RadarChart,
    MapChart,
    GridComponent,
    TooltipComponent,
    LegendComponent,
    TitleComponent,
    VisualMapComponent,
    RadarComponent,
    GeoComponent,
    LabelLayout,
    UniversalTransition,
    CanvasRenderer,
])

export type {ECharts, EChartsOption} from 'echarts/core'
export default echarts
