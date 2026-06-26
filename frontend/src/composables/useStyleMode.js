import { ref, watch } from 'vue'

const STORAGE_KEY = 'so-ai-edu-home-style'

const isOldStyle = ref(localStorage.getItem(STORAGE_KEY) === 'old')

watch(isOldStyle, (val) => {
  localStorage.setItem(STORAGE_KEY, val ? 'old' : 'new')
})

export function useStyleMode() {
  const toggleStyle = () => {
    isOldStyle.value = !isOldStyle.value
  }

  return { isOldStyle, toggleStyle }
}
