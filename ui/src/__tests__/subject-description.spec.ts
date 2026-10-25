// @vitest-environment jsdom
import { describe, expect, it } from 'vitest'
import { renderDescription } from '../../../packages/issue-static/src/description'

describe('subject description rendering', () => {
  it('renders Markdown and safe HTML', () => {
    expect(renderDescription('**重点**\n\n[文档](/docs)')).toContain('<strong>重点</strong>')
    expect(renderDescription('<em>说明</em>')).toContain('<em>说明</em>')
  })
  it('removes executable HTML, unsafe URLs and Alpine directives', () => {
    const result = renderDescription('<script>alert(1)</script><img src=x onerror="alert(1)"><a href="javascript:alert(1)" x-data="evil" @click="evil">x</a>')
    expect(result).not.toMatch(/script|onerror|javascript:|x-data|@click/)
  })
})
