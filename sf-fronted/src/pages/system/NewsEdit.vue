<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, nextTick, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import Quill from 'quill'
import request from '@/utils/request'

interface ApiResult<T> {
  code: number
  msg?: string
  data: T
}

interface NewsApiRow {
  id: number
  title: string
  category: string
  summary: string
  source: string
  status: string
  content: string
  clickCount: number
}

interface NewsForm {
  id?: number
  title: string
  category: string
  summary: string
  source: string
  status: string
  content: string
  clickCount: number | ''
}

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const formRef = ref()
const editorRef = ref<HTMLDivElement | null>(null)
let quill: Quill | null = null

const formModel = reactive<NewsForm>({
  title: '',
  category: '',
  summary: '',
  source: '',
  status: '待审核',
  content: '',
  clickCount: 0
})

const categoryOptions = ['研学动态', '政策解读', '案例分享', '活动通知', '专家观点']
const statusOptions = ['已发布', '待审核', '已下架']

const rules = {
  title: [{ required: true, message: '请输入资讯标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择资讯分类', trigger: 'change' }],
  summary: [{ required: true, message: '请输入资讯摘要', trigger: 'blur' }],
  content: [{ required: true, message: '请输入资讯内容', trigger: 'blur' }],
  status: [{ required: true, message: '请选择资讯状态', trigger: 'change' }]
}

const newsId = computed(() => Number(route.params.id))

const initEditor = async () => {
  await nextTick()
  if (!editorRef.value || quill) return
  quill = new Quill(editorRef.value, {
    theme: 'snow',
    placeholder: '请输入资讯内容...'
  })
  quill.root.innerHTML = formModel.content || ''
  quill.on('text-change', () => {
    formModel.content = quill?.root.innerHTML || ''
  })
}

const fetchDetail = async () => {
  loading.value = true
  try {
    const { data } = await request.get<ApiResult<NewsApiRow>>('/researchNews/detail', {
      params: { id: newsId.value }
    })

    if (data.code !== 1 || !data.data) {
      ElMessage.error(data.msg || '获取资讯详情失败')
      return
    }

    Object.assign(formModel, {
      id: data.data.id,
      title: data.data.title || '',
      category: data.data.category || '',
      summary: data.data.summary || '',
      source: data.data.source || '',
      status: data.data.status || '待审核',
      content: data.data.content || '',
      clickCount: data.data.clickCount || 0
    })

    await initEditor()
  } catch {
    ElMessage.error('获取资讯详情失败，请稍后再试')
  } finally {
    loading.value = false
  }
}

const submitForm = async () => {
  const form = formRef.value
  if (!form) return

  await form.validate(async (valid: boolean) => {
    if (!valid) return
    submitting.value = true
    try {
      const payload = {
        ...formModel,
        content: quill?.root.innerHTML || formModel.content,
        clickCount: Number(formModel.clickCount || 0)
      }

      const { data } = await request.put<ApiResult<null>>('/researchNews/update', payload)
      if (data.code !== 1) {
        ElMessage.error(data.msg || '更新资讯失败')
        return
      }

      ElMessage.success('更新成功')
      router.back()
    } catch {
      ElMessage.error('更新失败，请稍后再试')
    } finally {
      submitting.value = false
    }
  })
}

onMounted(() => {
  void fetchDetail()
})

onBeforeUnmount(() => {
  quill = null
})
</script>

<template>
  <div class="news-edit-page">
    <el-card class="page-card" shadow="never" v-loading="loading">
      <div class="page-header">
        <div>
          <el-button text class="back-btn" @click="router.back()">返回</el-button>
          <div class="page-title">编辑资讯</div>
          <div class="page-subtitle">按照卡片式页面编辑资讯信息，保存后立即生效。</div>
        </div>
      </div>

      <el-form ref="formRef" :model="formModel" :rules="rules" label-width="110px" class="news-form">
        <el-row :gutter="24">
          <el-col :span="24">
            <div class="section-title">基础信息</div>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="资讯标题" prop="title">
              <el-input v-model="formModel.title" placeholder="请输入资讯标题" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="资讯分类" prop="category">
              <el-select v-model="formModel.category" class="w-100" filterable allow-create default-first-option placeholder="请选择或输入分类">
                <el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="来源">
              <el-input v-model="formModel.source" placeholder="请输入来源" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="资讯状态" prop="status">
              <el-select v-model="formModel.status" class="w-100">
                <el-option v-for="item in statusOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="点击数">
              <el-input-number v-model="formModel.clickCount" :min="0" class="w-100" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <div class="section-title section-title--mt">封面与摘要</div>
          </el-col>
          <el-col :span="24">
            <el-form-item label="资讯摘要" prop="summary" class="summary-item">
              <div class="summary-card">
                <div class="summary-card__badge">摘要</div>
                <el-input v-model="formModel.summary" type="textarea" :rows="4" placeholder="请输入资讯摘要，建议概括文章核心内容，便于列表展示与用户快速浏览。" />
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="资讯内容" prop="content">
              <div class="editor-card">
                <div class="editor-toolbar-tip">支持富文本编辑，可用于加粗、列表、引用和链接等排版。</div>
                <div ref="editorRef" class="rich-editor"></div>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div class="page-footer">
        <el-button class="sf-btn is-plain page-btn page-btn--cancel" @click="router.back()">取消</el-button>
        <el-button class="sf-btn page-btn page-btn--save" type="primary" :loading="submitting" @click="submitForm">保存修改</el-button>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.news-edit-page {
  min-height: calc(100vh - 80px);
  padding: 16px;
  background: linear-gradient(180deg, #f6f8fd 0%, #f3f5fb 100%);
}

.page-card {
  max-width: 1180px;
  margin: 0 auto;
  border-radius: 18px;
  border: none;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
}

.page-header { margin-bottom: 20px; }
.back-btn { padding-left: 0; margin-bottom: 8px; color: #5b8def; }
.page-title { font-size: 24px; font-weight: 700; color: #1f2937; margin-bottom: 6px; }
.page-subtitle { color: #8a94a6; font-size: 14px; }
.news-form :deep(.el-form-item__label) { color: #4b5563; font-weight: 500; }
.w-100 { width: 100%; }
.section-title {
  font-size: 16px;
  font-weight: 700;
  color: #24324b;
  margin: 4px 0 2px;
}
.section-title--mt { margin-top: 8px; }
.summary-card {
  position: relative;
  width: 100%;
  padding: 18px;
  border-radius: 16px;
  background: linear-gradient(180deg, #ffffff 0%, #f9fbff 100%);
  border: 1px solid #e5ebf5;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
}
.summary-card__badge {
  display: inline-flex;
  align-items: center;
  margin-bottom: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: #4f6ef7;
  background: rgba(79, 110, 247, 0.1);
}
.editor-card {
  width: 100%;
  padding: 18px;
  border-radius: 16px;
  background: linear-gradient(180deg, #ffffff 0%, #f9fbff 100%);
  border: 1px solid #e5ebf5;
}
.editor-toolbar-tip {
  margin-bottom: 12px;
  color: #8a94a6;
  font-size: 13px;
}
.rich-editor {
  min-height: 320px;
  border-radius: 12px;
  background: #fff;
}
.rich-editor :deep(.ql-toolbar) {
  border: 1px solid #dfe6f3;
  border-bottom: none;
  border-radius: 12px 12px 0 0;
}
.rich-editor :deep(.ql-container) {
  min-height: 260px;
  border: 1px solid #dfe6f3;
  border-radius: 0 0 12px 12px;
  font-size: 14px;
}
.rich-editor :deep(.ql-editor) {
  min-height: 260px;
}
.page-footer {
  display: flex;
  justify-content: flex-end;
  gap: 14px;
  margin-top: 28px;
}
.page-btn {
  min-width: 112px;
  height: 44px;
  border-radius: 12px;
}
.page-btn--cancel {
  border-color: #d7deea;
}
.page-btn--save {
  box-shadow: 0 10px 24px rgba(91, 141, 239, 0.18);
}
@media (max-width: 768px) {
  .news-edit-page { padding: 12px; }
  .page-card { border-radius: 14px; }
  .page-footer { gap: 10px; }
  .page-btn { min-width: 96px; }
}
</style>
