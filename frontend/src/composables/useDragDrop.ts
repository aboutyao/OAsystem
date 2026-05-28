import { ref } from 'vue'

/**
 * Reusable composable for drag-and-drop file handling.
 *
 * Usage:
 *   const { isDragging, dragHandlers } = useDragDrop((files) => {
 *     // handle dropped files
 *   })
 *
 *   <div v-on="dragHandlers" :class="{ 'drop-active': isDragging }">
 *     Drop files here
 *   </div>
 */
export function useDragDrop(
  onDrop: (files: File[]) => void,
  options?: { accept?: string },
) {
  const isDragging = ref(false)
  let dragCounter = 0

  function onDragOver(e: DragEvent) {
    e.preventDefault()
    e.stopPropagation()
    if (e.dataTransfer) {
      e.dataTransfer.dropEffect = 'copy'
    }
  }

  function onDragEnter(e: DragEvent) {
    e.preventDefault()
    e.stopPropagation()
    dragCounter++
    isDragging.value = true
  }

  function onDragLeave(e: DragEvent) {
    e.preventDefault()
    e.stopPropagation()
    dragCounter--
    if (dragCounter <= 0) {
      dragCounter = 0
      isDragging.value = false
    }
  }

  function onDropHandler(e: DragEvent) {
    e.preventDefault()
    e.stopPropagation()
    dragCounter = 0
    isDragging.value = false

    const files = Array.from(e.dataTransfer?.files ?? [])
    if (files.length === 0) return

    // Filter by accept type if specified
    const accepted = options?.accept
      ? files.filter((f) => matchesAccept(f, options.accept!))
      : files

    if (accepted.length > 0) {
      onDrop(accepted)
    }
  }

  function matchesAccept(file: File, accept: string): boolean {
    const types = accept.split(',').map((t) => t.trim().toLowerCase())
    return types.some((pattern) => {
      if (pattern.startsWith('.')) {
        // Extension check
        return file.name.toLowerCase().endsWith(pattern)
      }
      if (pattern.endsWith('/*')) {
        // MIME type wildcard e.g. image/*
        const prefix = pattern.replace('/*', '')
        return file.type.startsWith(prefix)
      }
      // Full MIME type
      return file.type === pattern
    })
  }

  const dragHandlers = {
    onDragOver,
    onDragEnter,
    onDragLeave,
    onDrop: onDropHandler,
  }

  return { isDragging, dragHandlers }
}
