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
  let html = text
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

  // line breaks
  html = html.replace(/\n\n/g, '<br><br>')

  return html
}
</script>
