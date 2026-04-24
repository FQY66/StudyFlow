<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import Quill from 'quill'
import 'quill/dist/quill.snow.css'
import request from '@/utils/request'

try {
  const SizeStyle = Quill.import('attributors/style/size') as any
  SizeStyle.whitelist = ['12px', '13px', '14px', '15px', '16px', '19px', '22px']
  Quill.register(SizeStyle, true)
} catch {
  // ignore
}

try {
  const FontClass = Quill.import('attributors/class/font') as any
  FontClass.whitelist = ['songti', 'yahei', 'kaiti', 'heiti']
  Quill.register(FontClass, true)
} catch {
  // ignore
}

try {
  const Parchment = Quill.import('parchment') as any
  const LineHeightStyle = new Parchment.Attributor.Style('lineheight', 'line-height', {
    scope: Parchment.Scope.BLOCK,
    whitelist: ['1', '1.5', '1.75', '2', '2.5']
  })
  Quill.register(LineHeightStyle, true)
} catch {
  // ignore
}

const router = useRouter()
const categoryOptions = ['研学动态', '政策解读', '案例分享', '活动通知', '专家观点']
const statusOptions = ['已发布', '待审核', '已下架']

const form = ref({
  title: '',
  category: '',
  summary: '',
  content: '',
  source: '研学资讯',
  status: '已发布'
})

const uploading = ref(false)
const editorRef = ref<HTMLDivElement | null>(null)
const editorImageInputRef = ref<HTMLInputElement | null>(null)
let quill: Quill | null = null
let toolbarMouseOverHandler: ((event: Event) => void) | null = null
let toolbarMouseOutHandler: ((event: Event) => void) | null = null

const toolbarTips: Record<string, string> = {
  '.ql-bold': '加粗',
  '.ql-italic': '斜体',
  '.ql-underline': '下划线',
  '.ql-strike': '删除线',
  '.ql-color': '字体颜色',
  '.ql-background': '背景颜色',
  '.ql-list[value="ordered"]': '有序列表',
  '.ql-list[value="bullet"]': '无序列表',
  '.ql-indent[value="-1"]': '减少缩进',
  '.ql-indent[value="+1"]': '增加缩进',
  '.ql-align': '文本对齐',
  '.ql-blockquote': '引用',
  '.ql-code-block': '代码块',
  '.ql-link': '插入链接',
  '.ql-image': '插入图片',
  '.ql-undo': '撤销',
  '.ql-redo': '重做',
  '.ql-clean': '清除格式',
  '.ql-header': '正文/标题',
  '.ql-size': '字体大小',
  '.ql-lineheight': '行高',
  '.ql-font': '字体'
}

const toolbarShortcuts: Record<string, string> = {
  '.ql-bold': 'ctrl+b',
  '.ql-italic': 'ctrl+i',
  '.ql-underline': 'ctrl+u',
  '.ql-undo': 'ctrl+z',
  '.ql-redo': 'ctrl+y'
}

const quillModules = {
  toolbar: {
    container: [
      [{ header: [false, 1, 2, 3] }],
      ['bold', 'italic', 'underline', 'strike'],
      [{ color: [] }, { background: [] }],
      [{ font: ['songti', 'yahei', 'kaiti', 'heiti'] }],
      [{ size: ['12px', '13px', '14px', '15px', '16px', '19px', '22px'] }],
      [{ lineheight: ['1', '1.5', '1.75', '2', '2.5'] }],
      [{ list: 'ordered' }, { list: 'bullet' }],
      [{ indent: '-1' }, { indent: '+1' }],
      [{ align: [] }],
      ['blockquote', 'code-block'],
      ['link', 'image'],
      ['undo', 'redo'],
      ['clean']
    ],
    handlers: {
      image: () => editorImageInputRef.value?.click(),
      undo: () => quill?.history.undo(),
      redo: () => quill?.history.redo()
    }
  },
  history: { delay: 1000, maxStack: 200, userOnly: true }
}

const getTipFromTarget = (target: HTMLElement): string => {
  for (const [selector, tip] of Object.entries(toolbarTips)) {
    if (target.matches(selector) || target.closest(selector)) return tip
  }
  return ''
}

const bindInstantToolbarTips = () => {
  const toolbar = editorRef.value?.parentElement?.querySelector('.ql-toolbar') as HTMLElement | null
  if (!toolbar) return

  toolbarMouseOverHandler = (event: Event) => {
    const target = event.target as HTMLElement | null
    if (!target) return
    const control = target.closest('button, .ql-picker-label') as HTMLElement | null
    if (!control) return
    const tip = getTipFromTarget(control)
    if (!tip) return
    const shortcut = Object.entries(toolbarShortcuts).find(([selector]) =>
      control.matches(selector) || Boolean(control.closest(selector))
    )?.[1]
    control.setAttribute('data-tip', shortcut ? `${tip}\n${shortcut}` : tip)
  }

  toolbarMouseOutHandler = (event: Event) => {
    const target = event.target as HTMLElement | null
    const related = (event as MouseEvent).relatedTarget as HTMLElement | null
    if (!target) return
    const control = target.closest('button, .ql-picker-label') as HTMLElement | null
    if (!control) return
    if (related && control.contains(related)) return
    control.removeAttribute('data-tip')
  }

  toolbar.addEventListener('mouseover', toolbarMouseOverHandler)
  toolbar.addEventListener('mouseout', toolbarMouseOutHandler)
}

const normalizeHtmlToRelativePaths = (html: string) => {
  if (!html) return html
  const parser = new DOMParser()
  const doc = parser.parseFromString(html, 'text/html')
  doc.querySelectorAll('img').forEach((img) => {
    const src = img.getAttribute('src') || ''
    img.setAttribute('src', src)
  })
  return doc.body.innerHTML
}

const uploadImageFile = async (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await request.post('/common/file/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
  if (data?.code !== 1) throw new Error(data?.msg || '图片上传失败')
  return data.data?.path || data.data?.url || ''
}

const resolveFileUrl = (path: string) => {
  if (!path) return ''
  if (/^https?:\/\//i.test(path) || path.startsWith('data:')) return path
  const base = import.meta.env.VITE_API_BASE_URL || ''
  return path.startsWith('/') ? `${base}${path}` : `${base}/${path}`
}

const insertEditorImage = async (file: File) => {
  if (!quill) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请上传图片文件')
    return
  }
  const path = await uploadImageFile(file)
  if (!path) throw new Error('正文图片路径获取失败')
  const src = resolveFileUrl(path)
  const range = quill.getSelection(true)
  const index = range ? range.index : quill.getLength()
  quill.insertEmbed(index, 'image', src, 'user')
  quill.setSelection(index + 1, 0)
}

const onSelectEditorImage = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  try {
    await insertEditorImage(file)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '正文图片上传失败')
  } finally {
    target.value = ''
  }
}

const onPublish = async () => {
  if (!form.value.title.trim()) return ElMessage.warning('请输入资讯标题')
  if (!form.value.category) return ElMessage.warning('请选择资讯分类')
  if (!form.value.summary.trim()) return ElMessage.warning('请输入资讯摘要')
  if (!form.value.content || form.value.content === '<p><br></p>') return ElMessage.warning('请填写资讯内容')

  uploading.value = true
  try {
    const payload = {
      title: form.value.title.trim(),
      category: form.value.category,
      summary: form.value.summary.trim(),
      source: form.value.source.trim() || '研学资讯',
      status: form.value.status,
      content: form.value.content,
      clickCount: 0,
      createTime: new Date().toISOString().slice(0, 19).replace('T', ' '),
      updateTime: new Date().toISOString().slice(0, 19).replace('T', ' ')
    }
    const { data } = await request.post('/researchNews/save', payload)
    if (data?.code !== 1) {
      ElMessage.error(data?.msg || '发布失败')
      return
    }
    ElMessage.success('发布成功')
    router.push('/system/news')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '发布失败，请稍后再试')
  } finally {
    uploading.value = false
  }
}

onMounted(() => {
  if (!editorRef.value) return
  quill = new Quill(editorRef.value, { theme: 'snow', placeholder: '请输入内容...', modules: quillModules })
  quill.on('text-change', () => {
    form.value.content = normalizeHtmlToRelativePaths(quill?.root.innerHTML || '')
  })
  bindInstantToolbarTips()
})

onBeforeUnmount(() => {
  const toolbar = editorRef.value?.parentElement?.querySelector('.ql-toolbar') as HTMLElement | null
  if (toolbar && toolbarMouseOverHandler) toolbar.removeEventListener('mouseover', toolbarMouseOverHandler)
  if (toolbar && toolbarMouseOutHandler) toolbar.removeEventListener('mouseout', toolbarMouseOutHandler)
})
</script>

<template>
  <div class="new-project-page">
    <section class="editor-card">
      <div class="top-row">
        <el-input v-model="form.title" placeholder="请输入资讯标题" maxlength="50" show-word-limit class="title-input" />
        <el-select v-model="form.category" placeholder="请选择资讯分类" class="category-select">
          <el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
        </el-select>
      </div>

      <div class="summary-block">
        <el-input v-model="form.summary" type="textarea" :rows="4" maxlength="300" show-word-limit resize="none" placeholder="请输入资讯摘要" />
      </div>

      <div ref="editorRef" class="quill-editor" />
      <input ref="editorImageInputRef" class="hidden-input" type="file" accept="image/*" @change="onSelectEditorImage" />
    </section>

    <section class="publish-card">
      <h3 class="panel-title">发布设置</h3>
      <div class="row-field">
        <span class="item-label">来源</span>
        <el-input v-model="form.source" placeholder="请输入来源" />
      </div>
      <div class="row-field">
        <span class="item-label">状态</span>
        <el-select v-model="form.status" class="status-select">
          <el-option v-for="item in statusOptions" :key="item" :label="item" :value="item" />
        </el-select>
      </div>
      <div class="action-row">
        <el-button type="primary" class="sf-btn" :loading="uploading" @click="onPublish">发布</el-button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.new-project-page { display: flex; flex-direction: column; gap: 18px; max-width: 1120px; margin: 0 auto; }
.editor-card, .publish-card { background: #fff; border: 1px solid #ebeef5; border-radius: 8px; padding: 14px; }
.top-row { display: flex; gap: 14px; margin-bottom: 12px; }
.title-input { flex: 1; }
.category-select { width: 250px; }
.summary-block { margin-bottom: 12px; }
.quill-editor { min-height: 520px; }
:deep(.ql-toolbar.ql-snow) { border-color: #ebeef5; padding: 8px 10px; }
:deep(.ql-container.ql-snow) { border-color: #ebeef5; min-height: 500px; font-size: 15px; }
:deep(.ql-toolbar button[data-tip]::after), :deep(.ql-toolbar .ql-picker-label[data-tip]::after) { content: attr(data-tip); position: absolute; left: 50%; top: calc(100% + 8px); transform: translateX(-50%); white-space: pre-line; background: rgba(48,49,51,.96); color: #fff; border-radius: 6px; padding: 7px 10px; font-size: 12px; z-index: 9999; pointer-events: none; }
:deep(.ql-toolbar button[data-tip]::before), :deep(.ql-toolbar .ql-picker-label[data-tip]::before) { content: ''; position: absolute; left: 50%; top: calc(100% + 2px); transform: translateX(-50%); border: 6px solid transparent; border-bottom-color: rgba(48,49,51,.96); z-index: 9999; pointer-events: none; }
.panel-title { margin: 0 0 14px; font-size: 18px; font-weight: 600; color: #303133; }
.row-field { display: flex; flex-direction: column; gap: 8px; margin-bottom: 12px; }
.item-label { font-size: 13px; color: #606266; }
.status-select { width: 220px; }
.action-row { margin-top: 18px; display: flex; justify-content: flex-end; }
.hidden-input { display: none; }
</style>
