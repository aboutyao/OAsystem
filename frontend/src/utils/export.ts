/**
 * 导出工具 — 将数据导出为 CSV 文件
 */

export function exportToCsv(data: Record<string, unknown>[], filename: string, columns?: { key: string; label: string }[]) {
  if (!data.length) return

  const keys = columns ? columns.map(c => c.key) : Object.keys(data[0])
  const headers = columns ? columns.map(c => c.label) : keys

  const csvContent = [
    headers.join(','),
    ...data.map(row =>
      keys.map(k => {
        const val = row[k]
        const str = val == null ? '' : String(val)
        return str.includes(',') || str.includes('"') || str.includes('\n')
          ? `"${str.replace(/"/g, '""')}"`
          : str
      }).join(',')
    ),
  ].join('\n')

  const BOM = '﻿'
  const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${filename}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

export function exportToExcel(data: Record<string, unknown>[], filename: string, columns?: { key: string; label: string }[]) {
  // 简单的 Excel XML 格式（无需额外依赖）
  if (!data.length) return

  const keys = columns ? columns.map(c => c.key) : Object.keys(data[0])
  const headers = columns ? columns.map(c => c.label) : keys

  let xml = '<?xml version="1.0"?>\n'
  xml += '<?mso-application progid="Excel.Sheet"?>\n'
  xml += '<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"\n'
  xml += ' xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">\n'
  xml += '<Worksheet ss:Name="Sheet1">\n<Table>\n'

  // Headers
  xml += '<Row>'
  headers.forEach(h => { xml += `<Cell><Data ss:Type="String">${escapeXml(h)}</Data></Cell>` })
  xml += '</Row>\n'

  // Data
  data.forEach(row => {
    xml += '<Row>'
    keys.forEach(k => {
      const val = row[k]
      const type = typeof val === 'number' ? 'Number' : 'String'
      xml += `<Cell><Data ss:Type="${type}">${escapeXml(val == null ? '' : String(val))}</Data></Cell>`
    })
    xml += '</Row>\n'
  })

  xml += '</Table>\n</Worksheet>\n</Workbook>'

  const blob = new Blob([xml], { type: 'application/vnd.ms-excel' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${filename}.xls`
  link.click()
  URL.revokeObjectURL(url)
}

function escapeXml(str: string): string {
  return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}
