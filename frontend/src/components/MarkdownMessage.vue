<template>
  <div class="markdown-body message-markdown" v-html="renderedHtml"></div>
</template>

<script setup>
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'
import container from 'markdown-it-container'
import { full as markdownItEmoji } from 'markdown-it-emoji'

const props = defineProps({
  content: { type: String, required: true }
})

const md = new MarkdownIt({
  html: true,
  linkify: true,
  breaks: false
})

// VitePress 风格提示块：::: info / ::: warn / ::: error
function createContainer(type, defaultLabel) {
  md.use(container, type, {
    validate: (params) => params.trim().split(' ', 1)[0] === type,
    render: (tokens, idx) => {
      const token = tokens[idx]
      if (token.nesting === 1) {
        // 从 info 中提取自定义标题：::: info XEChat → 标题为 XEChat
        const infoParts = token.info.trim().split(' ')
        const label = infoParts.length > 1 ? infoParts.slice(1).join(' ') : defaultLabel
        return `<div class="admonition ${type}">\n<p class="admonition-title">${label}</p>\n`
      }
      return '</div>\n'
    }
  })
}
createContainer('info', 'INFO')
createContainer('warn', 'WARN')
createContainer('error', 'ERROR')

// emoji shortcode 支持 :smile: → 😄
md.use(markdownItEmoji)

// 为所有链接添加 target="_blank"
const defaultRender = md.renderer.rules.link_open || ((tokens, idx, options, _env, self) => self.renderToken(tokens, idx, options))
md.renderer.rules.link_open = (tokens, idx, options, env, self) => {
  tokens[idx].attrSet('target', '_blank')
  return defaultRender(tokens, idx, options, env, self)
}

function preprocessFont(text) {
  return text.replace(/<font\s+color=(['"]?)(\w+)\1\s*>(.*?)<\/font>/g,
    (_, _q, color, content) => `<span style="color:${color}">${content}</span>`
  )
}

const renderedHtml = computed(() => {
  if (!props.content) return ''
  return md.render(preprocessFont(props.content))
})
</script>
