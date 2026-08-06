import { useDark, useToggle } from '@vueuse/core'

export const isDark = useDark({
  selector: 'html',
  attribute: 'class',
  valueDark: '',
  valueLight: 'light',
  initialValue: 'dark'
})

export const toggleTheme = useToggle(isDark)
