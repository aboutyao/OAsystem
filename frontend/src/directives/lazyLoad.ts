import type { Directive, DirectiveBinding } from 'vue'

/**
 * 图片懒加载指令
 * 使用方式：v-lazy="imageUrl" 或 v-lazy="{ src: imageUrl, placeholder: '...' }"
 */

interface LazyLoadOptions {
  src: string
  placeholder?: string
  error?: string
}

const defaultPlaceholder = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgZmlsbD0iI2Y1ZjVmNSIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTQiIGZpbGw9IiNjY2MiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIj5Mb2FkaW5nLi4uPC90ZXh0Pjwvc3ZnPg=='

const defaultError = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgZmlsbD0iI2ZmZjJjYyIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTQiIGZpbGw9IiNjYzk5MDAiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIj5Mb2FkIEVycm9yPC90ZXh0Pjwvc3ZnPg=='

const observer = new IntersectionObserver(
  (entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        const img = entry.target as HTMLImageElement
        const src = img.dataset.src

        if (src) {
          img.src = src
          img.removeAttribute('data-src')

          img.onload = () => {
            img.classList.add('lazy-loaded')
          }

          img.onerror = () => {
            const errorSrc = img.dataset.error || defaultError
            img.src = errorSrc
            img.classList.add('lazy-error')
          }
        }

        observer.unobserve(img)
      }
    })
  },
  {
    rootMargin: '50px',
    threshold: 0.1,
  }
)

function getImageSrc(binding: DirectiveBinding<string | LazyLoadOptions>): {
  src: string
  placeholder: string
  error: string
} {
  if (typeof binding.value === 'string') {
    return {
      src: binding.value,
      placeholder: defaultPlaceholder,
      error: defaultError,
    }
  }

  return {
    src: binding.value.src,
    placeholder: binding.value.placeholder || defaultPlaceholder,
    error: binding.value.error || defaultError,
  }
}

export const vLazy: Directive<HTMLImageElement, string | LazyLoadOptions> = {
  mounted(el, binding) {
    const { src, placeholder, error } = getImageSrc(binding)

    el.dataset.src = src
    el.dataset.error = error
    el.src = placeholder
    el.classList.add('lazy-image')

    observer.observe(el)
  },

  updated(el, binding) {
    const { src, error } = getImageSrc(binding)

    if (el.dataset.src !== src) {
      el.dataset.src = src
      el.dataset.error = error
      el.classList.remove('lazy-loaded', 'lazy-error')

      observer.observe(el)
    }
  },

  unmounted(el) {
    observer.unobserve(el)
  },
}

/**
 * CSS to add to your global styles:
 *
 * .lazy-image {
 *   transition: opacity 0.3s ease;
 *   opacity: 0.6;
 * }
 *
 * .lazy-loaded {
 *   opacity: 1;
 * }
 *
 * .lazy-error {
 *   opacity: 0.8;
 * }
 */
