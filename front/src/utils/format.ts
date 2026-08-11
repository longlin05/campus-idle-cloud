/**
 * 格式化时间字段。
 * 后端 `java.util.Date` 经 Jackson 默认序列化为 epoch 毫秒数字，
 * 也可能直接返回 `yyyy-MM-dd HH:mm:ss` 或带 `T` 的 ISO 字符串。
 * 统一转成 `yyyy-MM-dd HH:mm:ss`；解析失败原样返回。
 */
export function formatTime(v: unknown): string {
  if (v === null || v === undefined || v === '') return '-'
  let d: Date
  if (typeof v === 'number') {
    d = new Date(v)
  } else if (v instanceof Date) {
    d = v
  } else {
    d = new Date(String(v).replace('T', ' ').replace('Z', ''))
  }
  if (isNaN(d.getTime())) return String(v)
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}
