import { describe, expect, it } from 'vitest'
import type { TemplateField } from '../api/generated'
import { TemplateFieldTypeEnum } from '../api/generated'
import { isFieldVisible, prepareTemplateValues, requiredField, validateFieldConditions } from '../../../packages/issue-shared/template-fields'

const fields: TemplateField[] = [
  { key: 'kind', title: '类型', type: TemplateFieldTypeEnum.Text, requiredMode: 'required' },
  { key: 'steps', title: '步骤', type: TemplateFieldTypeEnum.Text, showWhen: { fieldKey: 'kind', value: 'bug' }, requiredMode: 'REQUIRED' },
  { key: 'details', title: '详情', type: TemplateFieldTypeEnum.Text, showWhen: { fieldKey: 'steps', value: 'reproduce' } },
]

describe('template field contract', () => {
  it('cascades visibility and removes stale hidden values without dropping unrelated annotations', () => {
    const values = { kind: 'question', steps: 'reproduce', details: 'hidden', custom: 'keep' }
    expect(isFieldVisible(fields[2], fields, values)).toBe(false)
    expect(prepareTemplateValues(fields, values)).toEqual({ kind: 'question', custom: 'keep' })
    expect(values.details).toBe('hidden')
    expect(isFieldVisible(fields[2], fields, { ...values, kind: 'bug' })).toBe(true)
  })
  it('keeps existing unconditional templates and both historical required spellings compatible', () => {
    expect(isFieldVisible(fields[0], fields, {})).toBe(true)
    expect(requiredField(fields[0])).toBe(true)
    expect(requiredField(fields[1])).toBe(true)
    expect(requiredField(fields[2])).toBe(false)
  })
  it('rejects cycles, self-references and dangling references', () => {
    const cyclic: TemplateField[] = [
      { key: 'a', title: 'A', type: TemplateFieldTypeEnum.Text, showWhen: { fieldKey: 'b', value: '' } },
      { key: 'b', title: 'B', type: TemplateFieldTypeEnum.Text, showWhen: { fieldKey: 'a', value: '' } },
    ]
    expect(validateFieldConditions(cyclic)).toContain('循环')
    expect(isFieldVisible(cyclic[0], cyclic, {})).toBe(false)
    expect(validateFieldConditions([{ key: 'a', title: 'A', type: TemplateFieldTypeEnum.Text, showWhen: { fieldKey: 'gone', value: '' } }])).toContain('有效')
    expect(validateFieldConditions(fields)).toBeUndefined()
  })
})
