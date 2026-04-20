<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import Quill from 'quill'
import 'quill/dist/quill.snow.css'

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
  visible: true
})

const editorRef = ref<HTMLDivElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
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
    form.value.content = quill?.root.innerHTML || ''
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

const onSelectCover = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请上传图片文件')
    return
  }

  const reader = new FileReader()
  reader.onload = () => {
    form.value.cover = String(reader.result || '')
  }
  reader.readAsDataURL(file)
}

const onPublish = () => {
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

  ElMessage.success('发布成功（演示）')
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
    </section>

    <section class="publish-card">
      <h3 class="panel-title">发布设置</h3>

      <div class="cover-block">
        <span class="item-label">封面</span>

        <div class="cover-upload" @click="onClickUpload">
          <template v-if="form.cover">
            <img :src="form.cover" alt="cover" class="cover-img" />
          </template>
          <template v-else>
            <span class="upload-plus">+</span>
            <span class="upload-text">点击此封面</span>
          </template>
        </div>

        <input
          ref="fileInputRef"
          class="hidden-input"
          type="file"
          accept="image/*"
          @change="onSelectCover"
        />

        <p class="cover-tip">建议尺寸 16:9，jpg/png 格式</p>
      </div>

      <div class="visible-row">
        <span class="item-label">可见</span>
        <el-switch v-model="form.visible" />
      </div>

      <div class="action-row">
        <el-button type="primary" @click="onPublish">发布</el-button>
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

.cover-upload {
  width: 180px;
  height: 110px;
  border: 1px dashed #dcdfe6;
  border-radius: 2px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #909399;
  overflow: hidden;
}

.cover-upload:hover {
  border-color: #409eff;
}

.upload-plus {
  font-size: 24px;
  line-height: 1;
}

.upload-text {
  margin-top: 6px;
  font-size: 13px;
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hidden-input {
  display: none;
}

.cover-tip {
  margin: 0;
  font-size: 12px;
  color: #909399;
}

.visible-row {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.action-row {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}
</style>
