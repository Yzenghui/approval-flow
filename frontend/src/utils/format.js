/**
 * 通用展示格式化工具
 */

/**
 * 格式化后端返回的时间字符串
 * 后端返回形如 2026-09-13T10:20:30 的 ISO 字符串，页面只展示到秒
 * @param {string} isoString 时间字符串
 * @returns {string} 形如 2026-09-13 10:20:30，空值返回 —
 */
export function formatDateTime(isoString) {
  if (!isoString) return '—'
  return isoString.replace('T', ' ').substring(0, 19)
}

/**
 * 把表单数据格式化成一行键值对预览
 * 后端以 JSON 字符串存储表单数据，同时兼容传入对象的情况
 * @param {string|object} formData 表单数据
 * @returns {string} 形如 金额: 100 | 事由: 出差，空值或解析失败返回 —
 */
export function formatFormData(formData) {
  if (!formData) return '—'
  try {
    const data = typeof formData === 'string' ? JSON.parse(formData) : formData
    return Object.entries(data)
      .map(([k, v]) => `${k}: ${v}`)
      .join(' | ')
  } catch {
    return '—'
  }
}
