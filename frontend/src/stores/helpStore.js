import { defineStore } from 'pinia'

/**
 * helpStore - 预置 HELP 提示信息，不依赖 JSBridge 时序即可展示。
 * 匹配 HelpCommandHandler.java 输出格式。
 */
export const useHelpStore = defineStore('help', {
  state: () => ({
    helpText: `## 命令列表

| 命令 | 说明 | 命令 | 说明 |
|------|------|------|------|
| **#help** | 显示此帮助 | **#clean** | 清屏 |
| **#login {昵称} [-s 编号 -h IP -p 端口 -c]** | 登录鱼塘 | **#exit** | 退出登录 |
| **#showServer [-c]** | 鱼塘列表，-c 清理缓存 | **#showStatus** | 查看可用状态值 |
| **#setStatus {状态值}** | 设置当前状态 | **#showGame** | 游戏列表 |
| **#play {编号}** | 开始游戏 | **#join** | 加入游戏，后接任意字符拒绝邀请 |
| **#over** | 结束游戏/工具 | **#showMode** | 查看模式选项 |
| **#mode {编号}** | 模式设置 | **#open [{编号}]** | 打开工具 |
| **#moyu** | 摸鱼办生成 | **#weather {地名} [-d 0\\|3\\|7]** | 天气查询 |
| **#alive {0\\|1}** | 活着，0关闭 / 1开启 | **#notify {1\\|2\\|3}** | 通知：1正常 / 2隐晦 / 3关闭 |
| **#mask [-@ -a -u 昵称 -r 省份 -i IP -c -n]** | 屏蔽管理，-c 解除所有 | **#backgroundImage** | 切换背景图片 |
| **#admin** | 管控 | | |

> *Tips*: \`{ }\` 必填参数，\`[ ]\` 可选参数，参数以空格分隔。

[开源](https://github.com/anlingyi/xechat-idea)  ·  [更多](https://xeblog.cn/?tag=xechat-idea)`
  }),

  getters: {
    helpTexts(state) {
      return state.helpText
    }
  }
})
