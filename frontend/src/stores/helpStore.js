import { defineStore } from 'pinia'

/**
 * helpStore - 预置 HELP 提示信息，不依赖 JSBridge 时序即可展示。
 * 匹配 HelpCommandHandler.java 输出格式。
 */
export const useHelpStore = defineStore('help', {
  state: () => ({
    helpText: `## 命令列表

| 命令 | 说明 |
|------|------|
| **#help** | 显示此帮助 |
| **#clean** | 清空消息列表 |
| **#login {昵称}** | 登录服务器 |
| **#exit** | 退出登录 |
| **#showServer** | 查询服务列表 |
| **#showStatus** | 查看连接状态 |
| **#setStatus {状态}** | 设置自定义状态 |
| **#showGame** | 显示游戏列表 |
| **#play {序号}** | 开始游戏 |
| **#join {房间号}** | 加入游戏房间 |
| **#over** | 结束当前游戏 |
| **#showMode** | 显示模式列表 |
| **#mode {序号}** | 切换模式 |
| **#open {序号}** | 打开工具 |
| **#weather {城市}** | 查询天气 |
| **#notify {内容}** | 发送系统通知 |
| **#alive** | 存活检测 |
| **#moyu** | 摸鱼 |
| **#admin {指令}** | 管理员指令 |
| **#mask {关键词}** | 屏蔽关键词消息 |
| **#unmask {关键词}** | 取消屏蔽 |
| **#backgroundImage {URL}** | 切换聊天背景 |

> *Tips*: \`{ }\` 必填参数，\`[ ]\` 可选参数，参数以空格分隔。

[开源](https://github.com/anlingyi/xechat-idea)  ·  [更多](https://xeblog.cn/?tag=xechat-idea)`
  }),

  getters: {
    helpTexts(state) {
      return state.helpText
    }
  }
})
