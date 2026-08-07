<template>
  <div class="markdown-body message-markdown" v-html="renderedHtml"></div>
</template>

<script setup>
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'
import container from 'markdown-it-container'

const props = defineProps({
  content: { type: String, required: true }
})

const md = new MarkdownIt({
  html: true,
  linkify: true,
  breaks: false
})

// VitePress 风格提示块：::: info / ::: warn / ::: error
function createContainer(type, label) {
  md.use(container, type, {
    validate: () => true,
    render: (tokens, idx) => {
      const token = tokens[idx]
      if (token.nesting === 1) {
        return `<div class="admonition ${type}">\n<p class="admonition-title">${label}</p>\n`
      }
      return '</div>\n'
    }
  })
}
createContainer('info', 'INFO')
createContainer('warn', 'WARN')
createContainer('error', 'ERROR')

// 为所有链接添加 target="_blank"
const defaultRender = md.renderer.rules.link_open || ((tokens, idx, options, _env, self) => self.renderToken(tokens, idx, options))
md.renderer.rules.link_open = (tokens, idx, options, env, self) => {
  tokens[idx].attrSet('target', '_blank')
  return defaultRender(tokens, idx, options, env, self)
}

function preprocessFont(text) {
  // <font color=...> 转为 <span style="color:...">
  return text.replace(/<font\s+color=(['"]?)(\w+)\1\s*>(.*?)<\/font>/g,
    (_, _q, color, content) => `<span style="color:${color}">${content}</span>`
  )
}

const renderedHtml = computed(() => {
  if (!props.content) return ''
  return md.render(preprocessFont(props.content))
})
</script>
