<template>
  <div class="markdown-body message-markdown" v-html="renderedHtml"></div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  content: { type: String, required: true }
})

const renderedHtml = computed(() => {
  if (!props.content) return ''
  return renderSimpleMarkdown(props.content)
})

function renderSimpleMarkdown(text) {
  // 1. 先提取 <font color=...> 标签，替换为占位符，避免被转义
  const fontPlaceholders = []
  let html = text.replace(/<font\s+color=(['"]?)(\w+)\1\s*>(.*?)<\/font>/g, (_, _q, color, content) => {
    const idx = fontPlaceholders.length
    fontPlaceholders.push(`<span style="color:${color}">${content}</span>`)
    return `\u0000FONT_${idx}\u0000`
  })

  // 2. HTML 转义
  html = html
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

  // code blocks
  html = html.replace(/```(\w*)\n([\s\S]*?)```/g, (_, lang, code) =>
    `<pre><code>${code.trim()}</code></pre>`
  )

  // headers
  html = html.replace(/^#### (.+)$/gm, '<h4>$1</h4>')
  html = html.replace(/^### (.+)$/gm, '<h3>$1</h3>')
  html = html.replace(/^## (.+)$/gm, '<h2>$1</h2>')
  html = html.replace(/^# (.+)$/gm, '<h1>$1</h1>')

  // bold / italic
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>')

  // inline code
  html = html.replace(/`([^`]+)`/g, '<code>$1</code>')

  // links
  html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank">$1</a>')

  // images
  html = html.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, '<img src="$2" alt="$1" style="max-width:100%">')

  // unordered list
  html = html.replace(/^- (.+)$/gm, '<li>$1</li>')

  // blockquote
  html = html.replace(/^> (.+)$/gm, '<blockquote>$1</blockquote>')

  // horizontal rule
  html = html.replace(/^---$/gm, '<hr>')

  // Markdown tables: header | separator | body...
  html = html.replace(/^\|(.+)\|\n\|[-: |]+\|\n((?:^\|.+\|(?:\n|$))+)/gm, (_, headerLine, bodyLines) => {
    const headers = headerLine.split('|').map(h => h.trim()).filter(h => h)
    const rows = bodyLines.trim().split('\n')
    var th = '', td = ''
    headers.forEach(function (h) { th += '<th>' + h + '</th>' })
    rows.forEach(function (row) {
      const cells = row.split('|').map(function (c) { return c.trim() }).filter(function (c) { return c })
      td += '<tr>'
      cells.forEach(function (c) { td += '<td>' + c + '</td>' })
      td += '</tr>'
    })
    return '<table><thead><tr>' + th + '</tr></thead><tbody>' + td + '</tbody></table>'
  })

  // line breaks
  html = html.replace(/\n\n/g, '<br><br>')

  // 3. 还原 font 占位符
  fontPlaceholders.forEach((span, idx) => {
    html = html.replace(`\u0000FONT_${idx}\u0000`, span)
  })

  return html
}
</script>
