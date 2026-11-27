<template>
  <router-view />
</template>

<style>
:root {
  --el-color-primary: #0071e3;
  --el-color-primary-light-3: #3b82f6;
  --el-color-primary-dark-2: #1e3a8a;
  --el-border-radius-base: 10px;
}

html, body, #app {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'PingFang SC', 'Helvetica Neue', sans-serif;
  color: #0f172a;
  -webkit-font-smoothing: antialiased;
  /* 液态玻璃底层柔和环境光晕 */
  background:
    radial-gradient(circle at 12% 18%, rgba(191, 219, 254, 0.55) 0%, transparent 42%),
    radial-gradient(circle at 88% 15%, rgba(221, 214, 254, 0.45) 0%, transparent 40%),
    radial-gradient(circle at 75% 85%, rgba(187, 247, 208, 0.35) 0%, transparent 45%),
    #f1f5f9;
  background-attachment: fixed;
}

/* Liquid Glass 光学液态玻璃卡片 */
.el-card {
  background: rgba(255, 255, 255, 0.72) !important;
  backdrop-filter: blur(22px) saturate(180%) !important;
  -webkit-backdrop-filter: blur(22px) saturate(180%) !important;
  border-radius: 18px !important;
  border: 1px solid rgba(255, 255, 255, 0.78) !important;
  box-shadow:
    0 8px 30px rgba(15, 23, 42, 0.05),
    inset 0 1px 0 rgba(255, 255, 255, 0.92) !important;
  transition: transform 0.22s ease, box-shadow 0.22s ease !important;
}

.el-button {
  border-radius: 10px !important;
  font-weight: 500 !important;
  transition: all 0.2s ease !important;
}

.el-table {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: rgba(255, 255, 255, 0.45);
  border-radius: 12px;
  overflow: hidden;
}

.el-table th.el-table__cell {
  background-color: rgba(241, 245, 249, 0.85) !important;
  color: #1e293b !important;
  font-weight: 600 !important;
}

.el-dialog {
  background: rgba(255, 255, 255, 0.88) !important;
  backdrop-filter: blur(28px) saturate(180%) !important;
  border-radius: 20px !important;
  border: 1px solid rgba(255, 255, 255, 0.85) !important;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.14) !important;
}

/* Flowable 流程图节点高亮颜色（已完成：苹果绿，当前进行中：苹果橙） */
.highlight-done .djs-visual > :nth-child(1) {
  fill: #eafaf1 !important;
  stroke: #16a34a !important;
  stroke-width: 2.5px !important;
}
.highlight-active .djs-visual > :nth-child(1) {
  fill: #fff7ed !important;
  stroke: #f97316 !important;
  stroke-width: 3px !important;
}
</style>
