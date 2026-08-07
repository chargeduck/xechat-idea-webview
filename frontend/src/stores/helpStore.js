import { defineStore } from 'pinia'

/**
 * helpStore - 预置 HELP 提示信息，不依赖 JSBridge 时序即可展示。
 * 匹配 HelpCommandHandler.java 输出格式。
 */
export const useHelpStore = defineStore('help', {
  state: () => ({
    // 首次加载时 ChatPanel 检测消息为空则自动注入
    // 这里只存数据，注入逻辑由 ChatPanel.vue 的 onMounted 控制
    messages: [
      '命令列表 & 触发命令前缀 #',
      '',
      '· help：帮助',
      '· mask {关键词}：屏蔽指定关键词消息',
      '· unmask {关键词}：取消屏蔽',
      '· backgroundImage {图片URL}：切换聊天背景图片',
      '· clear：清空消息列表',
      '· status：查看当前连接状态',
      '· showServer：查询服务列表',
      '· login {昵称}：登录服务器',
      '· exit：退出登录',
      '· chat {内容}：发送公开消息',
      '· msg {用户} {内容}：发送私聊消息',
      '',
      ' > Tips: "{ }"表示输入参数占位符，"[ ]"内的参数为可选参数，所有参数均以空格分隔。',
      '',
      '--------------',
      '[开源](https://github.com/anlingyi/xechat-idea)  [更多](https://xeblog.cn/?tag=xechat-idea)',
      '--------------'
    ]
  }),

  getters: {
    helpTexts(state) {
      return state.messages
    }
  }
})
