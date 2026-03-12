<script setup lang="ts">
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

</script>

<template>
  <div id="app">
    <router-view></router-view>
  </div>
</template>

<style scoped>
  .btn{ @apply
    bg-red-500 text-indigo-50 px-1 py-2 rounded-4xl;
  }
</style>