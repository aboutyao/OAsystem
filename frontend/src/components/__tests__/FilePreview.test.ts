import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import FilePreview from '../FilePreview.vue'

describe('FilePreview', () => {
  it('should render correctly', () => {
    const wrapper = mount(FilePreview, {
      props: {
        url: 'https://example.com/test.pdf',
        name: 'test.pdf',
        visible: false,
      },
    })

    expect(wrapper.exists()).toBe(true)
  })

  it('should accept props', () => {
    const wrapper = mount(FilePreview, {
      props: {
        url: 'https://example.com/test.pdf',
        name: 'test.pdf',
        visible: true,
      },
    })

    expect(wrapper.props('url')).toBe('https://example.com/test.pdf')
    expect(wrapper.props('name')).toBe('test.pdf')
    expect(wrapper.props('visible')).toBe(true)
  })
})
