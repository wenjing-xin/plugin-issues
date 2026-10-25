import type { FieldCondition } from "../../ui/src/api/generated/models/field-condition";
import type { TemplateField } from "../../ui/src/api/generated/models/template-field";

export function isFieldVisible(
  field: TemplateField,
  fields: TemplateField[],
  values: Record<string, unknown>,
  path = new Set<string>(),
): boolean {
  if (path.has(field.key)) return false;
  if (!field.showWhen) return true;
  path.add(field.key);
  const parent = fields.find(item => item.key === field.showWhen!.fieldKey);
  return !!parent && isFieldVisible(parent, fields, values, path)
    && String(values[parent.key] ?? "") === field.showWhen.value;
}

export function prepareTemplateValues(fields: TemplateField[], values: Record<string, unknown>) {
  const result = { ...values };
  for (const field of fields) {
    if (!isFieldVisible(field, fields, values)) delete result[field.key];
  }
  return result;
}

export function requiredField(field: TemplateField) {
  return field.requiredMode?.toLowerCase() === "required";
}

export function validateFieldConditions(fields: TemplateField[]): string | undefined {
  const byKey = new Map(fields.map(field => [field.key, field]));
  for (const field of fields) {
    const visited = new Set<string>();
    let current: TemplateField | undefined = field;
    while (current?.showWhen) {
      if (visited.has(current.key)) return "显示条件不能循环引用";
      visited.add(current.key);
      const condition: FieldCondition = current.showWhen;
      if (!condition.fieldKey || condition.value == null || !byKey.has(condition.fieldKey)) {
        return "请选择有效的条件字段并填写匹配值";
      }
      current = byKey.get(condition.fieldKey);
    }
  }
  return undefined;
}
