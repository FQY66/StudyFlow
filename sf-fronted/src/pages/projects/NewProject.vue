<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
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

const categoryOptions = [
  '古都文化',
  '红色探访',
  '科技创新',
  '社会实践',
  '乡村振兴',
  '其他'
]

const form = ref({
  title: '',
  category: '',
  summary: '',
  content: '',
  cover: '',
  capacity: 30
})

const coverFile = ref<File | null>(null)
const uploading = ref(false)

const editorRef = ref<HTMLDivElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const editorImageInputRef = ref<HTMLInputElement | null>(null)
const isDragOver = ref(false)
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

const getTipFromTarget = (target: HTMLElement): string => {
  for (const [selector, tip] of Object.entries(toolbarTips)) {
    if (target.matches(selector) || target.closest(selector)) {
      const matched = target.matches(selector)
        ? target
        : (target.closest(selector) as HTMLElement | null)

      if (!matched) return tip

      if (selector === '.ql-header' && matched.classList.contains('ql-picker-label')) {
        return '标题级别'
      }

      return tip
    }
  }

  const alignItem = target.closest('.ql-align .ql-picker-item') as HTMLElement | null
  if (alignItem) {
    const value = alignItem.getAttribute('data-value') || ''
    const labelMap: Record<string, string> = {
      '': '左对齐',
      center: '居中对齐',
      right: '右对齐',
      justify: '两端对齐'
    }
    return labelMap[value] || '文本对齐'
  }

  const headerItem = target.closest('.ql-header .ql-picker-item') as HTMLElement | null
  if (headerItem) {
    const value = headerItem.getAttribute('data-value') || 'false'
    const headerMap: Record<string, string> = {
      '1': '标题1',
      '2': '标题2',
      '3': '标题3',
      false: '正文'
    }
    return headerMap[value] || '正文/标题'
  }

  const sizeItem = target.closest('.ql-size .ql-picker-item') as HTMLElement | null
  if (sizeItem) {
    const value = sizeItem.getAttribute('data-value') || '14px'
    return value
  }

  const lineHeightItem = target.closest('.ql-lineheight .ql-picker-item') as HTMLElement | null
  if (lineHeightItem) {
    const value = lineHeightItem.getAttribute('data-value') || '1.5'
    return `行高 ${value}`
  }

  const fontItem = target.closest('.ql-font .ql-picker-item') as HTMLElement | null
  if (fontItem) {
    const value = fontItem.getAttribute('data-value') || 'songti'
    const fontMap: Record<string, string> = {
      songti: '宋体',
      yahei: '微软雅黑',
      kaiti: '楷体',
      heiti: '黑体'
    }
    return fontMap[value] || '字体'
  }

  return ''
}

const bindInstantToolbarTips = () => {
  const toolbar = editorRef.value?.parentElement?.querySelector('.ql-toolbar') as HTMLElement | null
  if (!toolbar) return

  Object.entries(toolbarTips).forEach(([selector, tip]) => {
    const elements = toolbar.querySelectorAll(selector)
    elements.forEach((element) => {
      const target = element as HTMLElement
      target.setAttribute('aria-label', tip)
    })
  })

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

    if (related && control.contains(related)) {
      return
    }

    control.removeAttribute('data-tip')
  }

  toolbar.addEventListener('mouseover', toolbarMouseOverHandler)
  toolbar.addEventListener('mouseout', toolbarMouseOutHandler)
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
      image: () => {
        editorImageInputRef.value?.click()
      },
      undo: () => {
        quill?.history.undo()
      },
      redo: () => {
        quill?.history.redo()
      }
    }
  },
  history: {
    delay: 1000,
    maxStack: 200,
    userOnly: true
  }
}

onMounted(() => {
  if (!editorRef.value) return

  quill = new Quill(editorRef.value, {
    theme: 'snow',
    placeholder: '请输入内容...',
    modules: quillModules
  })

  quill.on('text-change', () => {
    form.value.content = normalizeHtmlToRelativePaths(quill?.root.innerHTML || '')
  })

  bindInstantToolbarTips()
})

onBeforeUnmount(() => {
  const toolbar = editorRef.value?.parentElement?.querySelector('.ql-toolbar') as HTMLElement | null
  if (toolbar && toolbarMouseOverHandler) {
    toolbar.removeEventListener('mouseover', toolbarMouseOverHandler)
  }
  if (toolbar && toolbarMouseOutHandler) {
    toolbar.removeEventListener('mouseout', toolbarMouseOutHandler)
  }

  toolbarMouseOverHandler = null
  toolbarMouseOutHandler = null
  quill = null
})

const onClickUpload = () => {
  fileInputRef.value?.click()
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

const clearCover = () => {
  form.value.cover = ''
  coverFile.value = null
  isDragOver.value = false
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

const handleCoverFile = (file?: File) => {
  if (!file) return

  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请上传图片文件')
    return
  }

  coverFile.value = file
  const reader = new FileReader()
  reader.onload = () => {
    form.value.cover = String(reader.result || '')
  }
  reader.readAsDataURL(file)
}

const onSelectCover = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  handleCoverFile(file)
  target.value = ''
}

const onCoverDragOver = (event: DragEvent) => {
  event.preventDefault()
  isDragOver.value = true
}

const onCoverDragLeave = () => {
  isDragOver.value = false
}

const onCoverDrop = (event: DragEvent) => {
  event.preventDefault()
  isDragOver.value = false

  const file = event.dataTransfer?.files?.[0]
  handleCoverFile(file)
}

const uploadCover = async () => {
  if (!coverFile.value) return ''

  const formData = new FormData()
  formData.append('file', coverFile.value)

  const { data } = await request.post('/common/file/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })

  if (data?.code !== 1) {
    throw new Error(data?.msg || '封面上传失败')
  }

  return data.data?.path || data.data?.url || ''
}

const uploadImageFile = async (file: File) => {
  const formData = new FormData()
  formData.append('file', file)

  const { data } = await request.post('/common/file/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })

  if (data?.code !== 1) {
    throw new Error(data?.msg || '图片上传失败')
  }

  return data.data?.path || data.data?.url || ''
}

const resolveFileUrl = (path: string) => {
  if (!path) return ''
  if (/^https?:\/\//i.test(path) || path.startsWith('data:')) return path
  const base = import.meta.env.VITE_API_BASE_URL || ''
  return path.startsWith('/') ? `${base}${path}` : `${base}/${path}`
}

const toRelativeFilePath = (src: string) => {
  const base = import.meta.env.VITE_API_BASE_URL || ''
  if (!src) return src
  if (src.startsWith('data:')) return src
  if (base && src.startsWith(base)) {
    return src.slice(base.length).replace(/^\/+/, '/')
  }
  return src
}

const normalizeHtmlToRelativePaths = (html: string) => {
  if (!html) return html
  const parser = new DOMParser()
  const doc = parser.parseFromString(html, 'text/html')
  doc.querySelectorAll('img').forEach((img) => {
    const src = img.getAttribute('src') || ''
    img.setAttribute('src', toRelativeFilePath(src))
  })
  return doc.body.innerHTML
}

const insertEditorImage = async (file: File) => {
  if (!quill) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请上传图片文件')
    return
  }

  const path = await uploadImageFile(file)
  if (!path) {
    throw new Error('正文图片路径获取失败')
  }

  const src = resolveFileUrl(path)
  const range = quill.getSelection(true)
  const index = range ? range.index : quill.getLength()
  quill.insertEmbed(index, 'image', src, 'user')
  quill.setSelection(index + 1, 0)
}

const onPublish = async () => {
  if (!form.value.title.trim()) {
    ElMessage.warning('请输入项目主题')
    return
  }
  if (!form.value.category) {
    ElMessage.warning('请选择项目类别')
    return
  }
  if (!form.value.content || form.value.content === '<p><br></p>') {
    ElMessage.warning('请填写项目内容')
    return
  }
  if (!form.value.capacity || Number(form.value.capacity) < 1) {
    ElMessage.warning('请设置报名容量')
    return
  }

  uploading.value = true
  try {
    const coverPath = await uploadCover()
    const payload = {
      coverPath,
      theme: form.value.title.trim(),
      introduction: form.value.summary.trim(),
      content: form.value.content,
      category: form.value.category,
      capacity: Number(form.value.capacity),
      status: '审核中',
      likeCount: 0,
      clickCount: 0,
      createTime: new Date().toISOString().slice(0, 19).replace('T', ' '),
      updateTime: new Date().toISOString().slice(0, 19).replace('T', ' ')
    }

    const { data } = await request.post('/project/save', payload)
    if (data?.code !== 1) {
      ElMessage.error(data?.msg || '发布失败')
      return
    }

    ElMessage.success('发布成功')
    clearCover()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '发布失败，请稍后再试')
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <div class="new-project-page">
    <section class="editor-card">
      <div class="top-row">
        <el-input
          v-model="form.title"
          placeholder="请输入项目主题（最多100个字符）"
          maxlength="100"
          show-word-limit
          class="title-input"
        />

        <el-select
          v-model="form.category"
          placeholder="请选择项目类别"
          class="category-select"
        >
          <el-option
            v-for="item in categoryOptions"
            :key="item"
            :label="item"
            :value="item"
          />
        </el-select>
      </div>

      <div class="summary-block">
        <!-- <div class="summary-header">
          <span class="summary-label">项目简介</span>
          <span class="summary-tip">最多 300 字</span>
        </div> -->
        <el-input
          v-model="form.summary"
          type="textarea"
          :rows="4"
          maxlength="300"
          show-word-limit
          resize="none"
          placeholder="请输入项目简介"
        />
      </div>

      <div ref="editorRef" class="quill-editor" />
      <input
        ref="editorImageInputRef"
        class="hidden-input"
        type="file"
        accept="image/*"
        @change="onSelectEditorImage"
      />
    </section>

    <section class="publish-card">
      <h3 class="panel-title">发布设置</h3>

      <div class="cover-block">
        <span class="item-label">封面</span>

        <div class="cover-uploader">
          <div
            class="cover-preview"
            :class="{ 'has-image': !!form.cover, 'is-drag-over': isDragOver }"
            @click="onClickUpload"
            @dragover="onCoverDragOver"
            @dragleave="onCoverDragLeave"
            @drop="onCoverDrop"
          >
            <template v-if="form.cover">
              <img :src="form.cover" alt="cover" class="cover-img" />
            </template>
            <template v-else>
              <div class="cover-placeholder">
                <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" class="upload-icon">
                  <path
                    d="M7 10V9C7 6.23858 9.23858 4 12 4C14.7614 4 17 6.23858 17 9V10C19.2091 10 21 11.7909 21 14C21 15.4806 20.1956 16.8084 19 17.5M7 10C4.79086 10 3 11.7909 3 14C3 15.4806 3.8044 16.8084 5 17.5M7 10C7.43285 10 7.84965 10.0688 8.24006 10.1959M12 12V21M12 12L15 15M12 12L9 15"
                    stroke="currentColor"
                    stroke-width="1.5"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  />
                </svg>
                <p class="upload-hint">拖拽或点击上传图片</p>
              </div>
            </template>
          </div>

          <label class="file-footer" @click.prevent="onClickUpload">
            <svg viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg" class="file-left-icon">
              <path d="M15.331 6H8.5v20h15V14.154h-8.169z"></path>
              <path d="M18.153 6h-.009v5.342H23.5v-.002z"></path>
            </svg>
            <p class="file-name">{{ form.cover ? '已选择封面文件' : '还未选择文件' }}</p>
            <button v-if="form.cover" type="button" class="file-delete-btn" @click.stop="clearCover">
              <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" class="trash-icon">
                <path
                  d="M5.16565 10.1534C5.07629 8.99181 5.99473 8 7.15975 8H16.8402C18.0053 8 18.9237 8.9918 18.8344 10.1534L18.142 19.1534C18.0619 20.1954 17.193 21 16.1479 21H7.85206C6.80699 21 5.93811 20.1954 5.85795 19.1534L5.16565 10.1534Z"
                  stroke="currentColor"
                  stroke-width="2"
                />
                <path d="M19.5 5H4.5" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
                <path d="M10 3C10 2.44772 10.4477 2 11 2H13C13.5523 2 14 2.44772 14 3V5H10V3Z" stroke="currentColor" stroke-width="2" />
              </svg>
            </button>
          </label>

          <input
            ref="fileInputRef"
            class="hidden-input"
            type="file"
            accept="image/*"
            @change="onSelectCover"
          />
        </div>

        <p class="cover-tip">建议尺寸 16:9，jpg/png 格式</p>
      </div>

      <div class="capacity-block">
        <span class="item-label">报名容量</span>
        <el-input-number
          v-model="form.capacity"
          :min="1"
          :max="99999"
          controls-position="right"
          class="capacity-input"
        />
        <p class="capacity-tip">请设置可报名人数上限，最少为 1 人</p>
      </div>

      <div class="action-row">
        <el-button type="primary" :loading="uploading" @click="onPublish">发布</el-button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.new-project-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
  max-width: 1120px;
  margin: 0 auto;
}

.editor-card,
.publish-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 14px;
}

.top-row {
  display: flex;
  gap: 14px;
  margin-bottom: 12px;
}

.title-input {
  flex: 1;
}

.category-select {
  width: 250px;
}

.summary-block {
  margin-bottom: 12px;
}

.summary-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.summary-label {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.summary-tip {
  font-size: 12px;
  color: #909399;
}

.quill-editor {
  min-height: 520px;
}

:deep(.ql-toolbar.ql-snow) {
  border-color: #ebeef5;
  padding: 8px 10px;
}

:deep(.ql-toolbar button),
:deep(.ql-toolbar .ql-picker-label),
:deep(.ql-toolbar .ql-picker-item) {
  position: relative;
  border-radius: 4px;
  font-size: 14px;
  transition: background-color 0.12s ease, box-shadow 0.12s ease;
}

:deep(.ql-toolbar button:hover),
:deep(.ql-toolbar .ql-picker-label:hover),
:deep(.ql-toolbar .ql-picker-item:hover) {
  background-color: #f5f7fa;
  box-shadow: inset 0 0 0 1px #dcdfe6;
}

:deep(.ql-toolbar button.ql-active),
:deep(.ql-toolbar .ql-picker-label.ql-active),
:deep(.ql-toolbar .ql-picker-item.ql-selected) {
  background-color: #ecf5ff;
  box-shadow: inset 0 0 0 1px #409eff;
}

:deep(.ql-toolbar .ql-undo::before) {
  content: '↶';
  font-size: 18px;
  line-height: 1;
}

:deep(.ql-toolbar .ql-redo::before) {
  content: '↷';
  font-size: 18px;
  line-height: 1;
}

:deep(.ql-toolbar .ql-undo),
:deep(.ql-toolbar .ql-redo) {
  width: 28px;
}

:deep(.ql-toolbar button[data-tip]::after),
:deep(.ql-toolbar .ql-picker-label[data-tip]::after),
:deep(.ql-toolbar .ql-picker-item[data-tip]::after) {
  content: attr(data-tip);
  position: absolute;
  left: 50%;
  top: calc(100% + 8px);
  transform: translateX(-50%);
  white-space: pre-line;
  text-align: center;
  background: rgba(48, 49, 51, 0.96);
  color: #fff;
  border-radius: 6px;
  padding: 7px 10px;
  font-size: 12px;
  line-height: 1.2;
  min-width: 70px;
  z-index: 9999;
  pointer-events: none;
}

:deep(.ql-toolbar button[data-tip]::before),
:deep(.ql-toolbar .ql-picker-label[data-tip]::before),
:deep(.ql-toolbar .ql-picker-item[data-tip]::before) {
  content: '';
  position: absolute;
  left: 50%;
  top: calc(100% + 2px);
  transform: translateX(-50%);
  border: 6px solid transparent;
  border-bottom-color: rgba(48, 49, 51, 0.96);
  z-index: 9999;
  pointer-events: none;
}

:deep(.ql-container.ql-snow) {
  border-color: #ebeef5;
  min-height: 500px;
  font-size: 15px;
}

:deep(.ql-editor) {
  line-height: 1.75;
}

:deep(.ql-editor blockquote) {
  margin: 10px 0;
  padding: 10px 14px;
  border-left: 4px solid #dcdfe6;
  background-color: #f5f7fa;
  color: #303133;
}

:deep(.ql-snow .ql-picker.ql-header) {
  width: 94px;
}

:deep(.ql-snow .ql-picker.ql-header .ql-picker-label::before) {
  content: '正文';
}

:deep(.ql-snow .ql-picker.ql-header .ql-picker-label[data-value='1']::before) {
  content: '标题1';
}

:deep(.ql-snow .ql-picker.ql-header .ql-picker-label[data-value='2']::before) {
  content: '标题2';
}

:deep(.ql-snow .ql-picker.ql-header .ql-picker-label[data-value='3']::before) {
  content: '标题3';
}

:deep(.ql-snow .ql-picker.ql-header .ql-picker-item::before) {
  content: '正文';
}

:deep(.ql-snow .ql-picker.ql-header .ql-picker-item[data-value='1']::before) {
  content: '标题1';
}

:deep(.ql-snow .ql-picker.ql-header .ql-picker-item[data-value='2']::before) {
  content: '标题2';
}

:deep(.ql-snow .ql-picker.ql-header .ql-picker-item[data-value='3']::before) {
  content: '标题3';
}

:deep(.ql-snow .ql-picker.ql-font) {
  width: 102px;
}

:deep(.ql-snow .ql-picker.ql-font .ql-picker-label::before) {
  content: '宋体';
}

:deep(.ql-snow .ql-picker.ql-font .ql-picker-label[data-value='songti']::before) {
  content: '宋体';
}

:deep(.ql-snow .ql-picker.ql-font .ql-picker-label[data-value='yahei']::before) {
  content: '微软雅黑';
}

:deep(.ql-snow .ql-picker.ql-font .ql-picker-label[data-value='kaiti']::before) {
  content: '楷体';
}

:deep(.ql-snow .ql-picker.ql-font .ql-picker-label[data-value='heiti']::before) {
  content: '黑体';
}

:deep(.ql-snow .ql-picker.ql-font .ql-picker-item::before) {
  content: '宋体';
}

:deep(.ql-snow .ql-picker.ql-font .ql-picker-item[data-value='songti']::before) {
  content: '宋体';
}

:deep(.ql-snow .ql-picker.ql-font .ql-picker-item[data-value='yahei']::before) {
  content: '微软雅黑';
}

:deep(.ql-snow .ql-picker.ql-font .ql-picker-item[data-value='kaiti']::before) {
  content: '楷体';
}

:deep(.ql-snow .ql-picker.ql-font .ql-picker-item[data-value='heiti']::before) {
  content: '黑体';
}

:deep(.ql-font-songti) {
  font-family: 'SimSun', 'Songti SC', serif;
}

:deep(.ql-font-yahei) {
  font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif;
}

:deep(.ql-font-kaiti) {
  font-family: 'KaiTi', 'STKaiti', serif;
}

:deep(.ql-font-heiti) {
  font-family: 'SimHei', 'Heiti SC', sans-serif;
}

:deep(.ql-snow .ql-picker.ql-size) {
  width: 96px;
}

:deep(.ql-snow .ql-picker.ql-size .ql-picker-label::before) {
  content: '默认字号';
}

:deep(.ql-snow .ql-picker.ql-size .ql-picker-label[data-value='12px']::before),
:deep(.ql-snow .ql-picker.ql-size .ql-picker-item[data-value='12px']::before) {
  content: '12px';
}

:deep(.ql-snow .ql-picker.ql-size .ql-picker-label[data-value='13px']::before),
:deep(.ql-snow .ql-picker.ql-size .ql-picker-item[data-value='13px']::before) {
  content: '13px';
}

:deep(.ql-snow .ql-picker.ql-size .ql-picker-label[data-value='14px']::before),
:deep(.ql-snow .ql-picker.ql-size .ql-picker-item[data-value='14px']::before) {
  content: '14px';
}

:deep(.ql-snow .ql-picker.ql-size .ql-picker-label[data-value='15px']::before),
:deep(.ql-snow .ql-picker.ql-size .ql-picker-item[data-value='15px']::before) {
  content: '15px';
}

:deep(.ql-snow .ql-picker.ql-size .ql-picker-label[data-value='16px']::before),
:deep(.ql-snow .ql-picker.ql-size .ql-picker-item[data-value='16px']::before) {
  content: '16px';
}

:deep(.ql-snow .ql-picker.ql-size .ql-picker-label[data-value='19px']::before),
:deep(.ql-snow .ql-picker.ql-size .ql-picker-item[data-value='19px']::before) {
  content: '19px';
}

:deep(.ql-snow .ql-picker.ql-size .ql-picker-label[data-value='22px']::before),
:deep(.ql-snow .ql-picker.ql-size .ql-picker-item[data-value='22px']::before) {
  content: '22px';
}

:deep(.ql-snow .ql-picker.ql-size .ql-picker-item::before) {
  content: '默认字号';
}

:deep(.ql-snow .ql-picker.ql-lineheight) {
  width: 96px;
}

:deep(.ql-snow .ql-picker.ql-lineheight .ql-picker-label::before) {
  content: '默认行高';
}

:deep(.ql-snow .ql-picker.ql-lineheight .ql-picker-item::before) {
  content: '默认行高';
}

:deep(.ql-snow .ql-picker.ql-lineheight .ql-picker-label[data-value='1']::before),
:deep(.ql-snow .ql-picker.ql-lineheight .ql-picker-item[data-value='1']::before) {
  content: '1';
}

:deep(.ql-snow .ql-picker.ql-lineheight .ql-picker-label[data-value='1.5']::before),
:deep(.ql-snow .ql-picker.ql-lineheight .ql-picker-item[data-value='1.5']::before) {
  content: '1.5';
}

:deep(.ql-snow .ql-picker.ql-lineheight .ql-picker-label[data-value='1.75']::before),
:deep(.ql-snow .ql-picker.ql-lineheight .ql-picker-item[data-value='1.75']::before) {
  content: '1.75';
}

:deep(.ql-snow .ql-picker.ql-lineheight .ql-picker-label[data-value='2']::before),
:deep(.ql-snow .ql-picker.ql-lineheight .ql-picker-item[data-value='2']::before) {
  content: '2';
}

:deep(.ql-snow .ql-picker.ql-lineheight .ql-picker-label[data-value='2.5']::before),
:deep(.ql-snow .ql-picker.ql-lineheight .ql-picker-item[data-value='2.5']::before) {
  content: '2.5';
}

:deep(.ql-editor [style*='line-height: 1']) { line-height: 1; }
:deep(.ql-editor [style*='line-height: 1.5']) { line-height: 1.5; }
:deep(.ql-editor [style*='line-height: 1.75']) { line-height: 1.75; }
:deep(.ql-editor [style*='line-height: 2']) { line-height: 2; }
:deep(.ql-editor [style*='line-height: 2.5']) { line-height: 2.5; }

.panel-title {
  margin: 0 0 14px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.cover-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.item-label {
  font-size: 13px;
  color: #606266;
}

.cover-uploader {
  width: 100%;
  max-width: 460px;
}

.cover-preview {
  position: relative;
  height: 280px;
  width: 100%;
  border: 2px dashed #d7dce6;
  border-radius: 16px;
  background: #fafbfe;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  cursor: pointer;
  box-shadow: none;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.cover-preview:hover,
.cover-preview.is-drag-over {
  border-color: #7aa7ff;
  background: #eef5ff;
}

.cover-preview.has-image {
  border-style: solid;
  border-color: #e6ebf4;
  background: #fff;
}

.cover-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 28px;
  width: 100%;
  height: 100%;
  color: #111;
}

.upload-icon {
  height: 110px;
  width: 110px;
  color: #000;
  flex: none;
}

.upload-hint {
  text-align: center;
  color: #111;
  font-size: 18px;
  margin: 0;
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.file-footer {
  margin-top: 14px;
  width: 100%;
  min-height: 56px;
  padding: 0 16px;
  border-radius: 14px;
  background: #eef3fb;
  border: 1px solid #e2e8f3;
  display: flex;
  align-items: center;
  gap: 12px;
  box-sizing: border-box;
  cursor: pointer;
  box-shadow: none;
}

.file-left-icon {
  width: 22px;
  height: 22px;
  fill: #4c6ef5;
  flex: none;
}

.file-name {
  flex: 1;
  text-align: center;
  margin: 0;
  font-size: 14px;
  color: #111;
}

.file-delete-btn {
  border: 0;
  background: transparent;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #111;
}

.trash-icon {
  width: 22px;
  height: 22px;
}

.hidden-input {
  display: none;
}

.cover-tip {
  margin: 10px 0 0;
  font-size: 12px;
  color: #8a94a6;
}

.capacity-block {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.capacity-input {
  width: 220px;
}

.capacity-tip {
  margin: 0;
  font-size: 12px;
  color: #909399;
}

.action-row {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}
</style>
