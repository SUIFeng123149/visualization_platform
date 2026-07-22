import { computed, ref } from 'vue'
import { fetchPlatforms } from '@/api/content'

const platforms = ref([])
const selectedPlatform = ref('all')
const loadingPlatforms = ref(false)
let loadPromise = null

const activePlatform = computed(() => (
  platforms.value.find((item) => item.platformCode === selectedPlatform.value) ?? null
))
const activeCapabilities = computed(() => activePlatform.value?.capabilities ?? {})

async function loadPlatforms() {
  if (platforms.value.length) return platforms.value
  if (loadPromise) return loadPromise
  loadingPlatforms.value = true
  loadPromise = fetchPlatforms()
    .then((rows) => {
      platforms.value = rows.filter((item) => item.enabled)
      return platforms.value
    })
    .finally(() => {
      loadingPlatforms.value = false
      loadPromise = null
    })
  return loadPromise
}

export function usePlatformContext() {
  return { platforms, selectedPlatform, loadingPlatforms, activePlatform, activeCapabilities, loadPlatforms }
}
