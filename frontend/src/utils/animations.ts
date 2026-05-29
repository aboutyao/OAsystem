/**
 * 动画工具函数
 */

/**
 * 淡入动画
 */
export function fadeIn(element: HTMLElement, duration = 300) {
  element.style.opacity = '0'
  element.style.display = 'block'

  const start = performance.now()

  function animate(time: number) {
    const elapsed = time - start
    const progress = Math.min(elapsed / duration, 1)

    element.style.opacity = String(progress)

    if (progress < 1) {
      requestAnimationFrame(animate)
    }
  }

  requestAnimationFrame(animate)
}

/**
 * 淡出动画
 */
export function fadeOut(element: HTMLElement, duration = 300) {
  const start = performance.now()
  const startOpacity = parseFloat(getComputedStyle(element).opacity) || 1

  function animate(time: number) {
    const elapsed = time - start
    const progress = Math.min(elapsed / duration, 1)

    element.style.opacity = String(startOpacity * (1 - progress))

    if (progress < 1) {
      requestAnimationFrame(animate)
    } else {
      element.style.display = 'none'
    }
  }

  requestAnimationFrame(animate)
}

/**
 * 滑动展开
 */
export function slideDown(element: HTMLElement, duration = 300) {
  element.style.display = 'block'
  const height = element.scrollHeight
  element.style.height = '0px'
  element.style.overflow = 'hidden'

  const start = performance.now()

  function animate(time: number) {
    const elapsed = time - start
    const progress = Math.min(elapsed / duration, 1)

    element.style.height = `${height * progress}px`

    if (progress < 1) {
      requestAnimationFrame(animate)
    } else {
      element.style.height = ''
      element.style.overflow = ''
    }
  }

  requestAnimationFrame(animate)
}

/**
 * 滑动收起
 */
export function slideUp(element: HTMLElement, duration = 300) {
  const height = element.scrollHeight
  element.style.height = `${height}px`
  element.style.overflow = 'hidden'

  const start = performance.now()

  function animate(time: number) {
    const elapsed = time - start
    const progress = Math.min(elapsed / duration, 1)

    element.style.height = `${height * (1 - progress)}px`

    if (progress < 1) {
      requestAnimationFrame(animate)
    } else {
      element.style.display = 'none'
      element.style.height = ''
      element.style.overflow = ''
    }
  }

  requestAnimationFrame(animate)
}

/**
 * 弹跳效果
 */
export function bounce(element: HTMLElement, scale = 1.1, duration = 300) {
  const start = performance.now()
  const originalTransform = getComputedStyle(element).transform

  function animate(time: number) {
    const elapsed = time - start
    const progress = Math.min(elapsed / duration, 1)

    // Bounce curve
    const bounce = Math.sin(progress * Math.PI * 2) * (1 - progress)
    const currentScale = 1 + (scale - 1) * bounce

    element.style.transform = `${originalTransform} scale(${currentScale})`

    if (progress < 1) {
      requestAnimationFrame(animate)
    } else {
      element.style.transform = originalTransform
    }
  }

  requestAnimationFrame(animate)
}

/**
 * 闪烁效果（用于高亮新内容）
 */
export function flash(element: HTMLElement, color = 'var(--oa-primary-lighter)', duration = 1000) {
  const originalBg = element.style.backgroundColor
  element.style.backgroundColor = color
  element.style.transition = `background-color ${duration}ms ease`

  setTimeout(() => {
    element.style.backgroundColor = originalBg
    setTimeout(() => {
      element.style.transition = ''
    }, duration)
  }, 50)
}
