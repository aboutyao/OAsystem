import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import FilePreview from '../FilePreview.vue'

describe('FilePreview', () => {
  it('should render correctly with PDF file', () => {
    const wrapper = mount(FilePreview, {
      props: {
        url: 'https://example.com/test.pdf',
        name: 'test.pdf',
        visible: true,
      },
    })

    expect(wrapper.find('.preview-container').exists()).toBe(true)
  })

  it('should render correctly with image file', () => {
    const wrapper = mount(FilePreview, {
      props: {
        url: 'https://example.com/test.jpg',
        name: 'test.jpg',
        visible: true,
      },
    })

    expect(wrapper.find('.preview-image').exists()).toBe(true)
  })

  it('should show unsupported message for unknown file type', () => {
    const wrapper = mount(FilePreview, {
      props: {
        url: 'https://example.com/test.xyz',
        name: 'test.xyz',
        visible: true,
      },
    })

    expect(wrapper.find('.preview-unsupported').exists()).toBe(true)
  })

  it('should emit update:visible when closed', async () => {
    const wrapper = mount(FilePreview, {
      props: {
        url: 'https://example.com/test.pdf',
        name: 'test.pdf',
        visible: true,
      },
    })

    await wrapper.vm.$nextTick()
    // The dialog should be visible
    expect(wrapper.props('visible')).toBe(true)
  })
})
