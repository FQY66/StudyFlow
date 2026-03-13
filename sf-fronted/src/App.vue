<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const activePath = computed(() => route.path)
</script>

<template>
  <div id="app">
    <router-view></router-view>
  </div>
</template>

<style scoped>
.layout-root {
  height: 100vh;
}

.layout-aside {
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
  padding: 16px 0;
}

.logo {
  padding: 0 20px 16px;
  border-bottom: 1px solid #f2f2f2;
  margin-bottom: 8px;
}

.logo-title {
  display: block;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.logo-subtitle {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.side-menu {
  border-right: none;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  background-color: #ffffff;
}

.header-title {
  font-size: 14px;
  color: #606266;
}

.layout-main {
  padding: 16px 24px 24px;
  background-color: #f5f7fa;
}
</style>