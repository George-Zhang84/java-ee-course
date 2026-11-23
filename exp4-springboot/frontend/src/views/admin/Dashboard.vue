<template>
  <div class="page-container">
    <div class="section-title">数据看板</div>

    <div class="summary-grid">
      <div v-for="card in cards" :key="card.label" class="summary-card">
        <div class="summary-label">{{ card.label }}</div>
        <div class="summary-value">{{ card.value }}</div>
        <div class="summary-extra">{{ card.extra }}</div>
      </div>
    </div>

    <div class="chart-grid">
      <el-card shadow="never">
        <div class="chart-title">近 7 日订单与成交额</div>
        <div ref="trendRef" class="chart chart-trend" />
      </el-card>
      <el-card shadow="never">
        <div class="chart-title">分类商品分布</div>
        <div ref="pieRef" class="chart chart-pie" />
      </el-card>
    </div>

    <el-card shadow="never" class="hot-card">
      <div class="chart-title">热销商品 Top5</div>
      <div ref="barRef" class="chart chart-bar" />
    </el-card>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import {
  dashboardCategories,
  dashboardHotProducts,
  dashboardSummary,
  dashboardTrend
} from '@/api/admin'
import { money } from '@/utils/format'

const summary = ref({})
const trendRef = ref()
const pieRef = ref()
const barRef = ref()
const charts = []

const cards = computed(() => [
  { label: '用户总数', value: summary.value.userCount ?? '-', extra: '含买家与卖家' },
  { label: '在售商品', value: summary.value.onSaleCount ?? '-', extra: `待审核 ${summary.value.pendingCount ?? 0} 件` },
  { label: '累计订单', value: summary.value.orderCount ?? '-', extra: `今日 ${summary.value.todayOrders ?? 0} 单` },
  { label: '累计成交额', value: '¥' + money(summary.value.gmv), extra: `今日 ¥${money(summary.value.todayGmv)}` }
])

function initChart(el) {
  const chart = echarts.init(el)
  charts.push(chart)
  return chart
}

async function render() {
  const [sumRes, trendRes, cateRes, hotRes] = await Promise.all([
    dashboardSummary(),
    dashboardTrend(7),
    dashboardCategories(),
    dashboardHotProducts(5)
  ])
  summary.value = sumRes.data || {}

  const trend = trendRes.data || []
  const trendChart = initChart(trendRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    // 图例放底部：放顶部会与右侧"成交额"纵轴标题重叠
    legend: { data: ['订单数', '成交额'], bottom: 0 },
    grid: { left: 56, right: 64, top: 56, bottom: 48 },
    xAxis: { type: 'category', data: trend.map((t) => t.statDate) },
    yAxis: [
      {
        type: 'value',
        name: '订单数(单)',
        nameGap: 18,
        nameTextStyle: { color: '#6b7280' },
        minInterval: 1
      },
      {
        type: 'value',
        name: '成交额(元)',
        nameGap: 20,
        nameTextStyle: { color: '#6b7280' }
      }
    ],
    series: [
      { name: '订单数', type: 'bar', barWidth: 18, itemStyle: { color: '#2f6bff' }, data: trend.map((t) => t.orderCount) },
      {
        name: '成交额',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        itemStyle: { color: '#ff8a3d' },
        data: trend.map((t) => Number(t.gmv))
      }
    ]
  })

  const cate = (cateRes.data || []).filter((c) => Number(c.productCount) > 0)
  const pieChart = initChart(pieRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}：{c} 件（{d}%）' },
    // 图例竖排放右侧；扇区标签只显示百分比并放在环内，彻底避免"标签压图例""小扇区文字重叠"
    legend: { orient: 'vertical', right: 8, top: 'center', itemWidth: 10, itemHeight: 10 },
    series: [
      {
        type: 'pie',
        radius: ['36%', '62%'],
        center: ['36%', '50%'],
        minAngle: 3,
        avoidLabelOverlap: true,
        label: { position: 'inside', formatter: '{d}%', fontSize: 11, color: '#fff' },
        labelLine: { show: false },
        data: cate.map((c) => ({ name: c.name, value: c.productCount }))
      }
    ]
  })

  const hot = hotRes.data || []
  const barChart = initChart(barRef.value)
  barChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 140, right: 40, top: 20, bottom: 30 },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: { type: 'category', data: hot.map((h) => h.title) },
    series: [
      {
        type: 'bar',
        barWidth: 16,
        itemStyle: { color: '#48c9b0', borderRadius: [0, 6, 6, 0] },
        label: { show: true, position: 'right', formatter: '{c} 单' },
        data: hot.map((h) => h.soldCount)
      }
    ]
  })
}

function onResize() {
  charts.forEach((c) => c.resize())
}

onMounted(async () => {
  await render()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  charts.forEach((c) => c.dispose())
})
</script>

<style scoped>
.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.summary-card {
  background: #fff;
  border-radius: 12px;
  padding: 18px;
  border: 1px solid #eef0f4;
}

.summary-label {
  font-size: 13px;
  color: var(--text-sub);
}

.summary-value {
  font-size: 24px;
  font-weight: 700;
  margin: 6px 0 4px;
  color: var(--brand);
}

.summary-extra {
  font-size: 12px;
  color: #9aa4b2;
}

.chart-grid {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 8px;
}

.chart {
  width: 100%;
}

.chart-trend {
  height: 300px;
}

.chart-pie {
  height: 300px;
}

.chart-bar {
  height: 260px;
}

@media (max-width: 900px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>
