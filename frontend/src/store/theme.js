import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

const THEME_KEY = 'marine_theme'

function getStoredTheme() {
  try {
    const stored = localStorage.getItem(THEME_KEY)
    if (stored === 'dark') return 'dark'
  } catch {}
  return 'light'
}

function applyTheme(mode) {
  const html = document.documentElement
  if (mode === 'dark') {
    html.setAttribute('data-theme', 'dark')
    html.classList.add('dark')
  } else {
    html.removeAttribute('data-theme')
    html.classList.remove('dark')
  }
}

export const useThemeStore = defineStore('theme', () => {
  const mode = ref(getStoredTheme())

  applyTheme(mode.value)

  watch(mode, (val) => {
    try { localStorage.setItem(THEME_KEY, val) } catch {}
    applyTheme(val)
  })

  const toggle = () => {
    mode.value = mode.value === 'dark' ? 'light' : 'dark'
  }

  const isDark = () => mode.value === 'dark'

  return { mode, toggle, isDark }
})
