import * as XLSX from 'xlsx'

export function exportExcelWorkbook(filename, sheets) {
  const wb = XLSX.utils.book_new()

  sheets.forEach(sheet => {
    // Build header row
    const headerRow = sheet.columns.map(col => col.label)
    // Build data rows
    const dataRows = sheet.rows.map(row =>
      sheet.columns.map(col => {
        const value = row[col.key]
        return formatCellValue(value)
      })
    )

    // Combine header + data
    const wsData = [headerRow, ...dataRows]
    const ws = XLSX.utils.aoa_to_sheet(wsData)

    // Auto-fit column widths (approximate)
    const colWidths = sheet.columns.map((col, i) => {
      const maxLen = Math.max(
        col.label.length,
        ...dataRows.map(row => String(row[i] || '').length)
      )
      return { wch: Math.max(8, Math.min(60, maxLen + 4)) }
    })
    ws['!cols'] = colWidths

    XLSX.utils.book_append_sheet(wb, ws, sheet.name)
  })

  XLSX.writeFile(wb, filename.endsWith('.xlsx') ? filename : filename + '.xlsx')
}

function formatCellValue(value) {
  if (value === undefined || value === null) return ''
  if (typeof value === 'boolean') return value ? '是' : '否'
  if (typeof value === 'object') return JSON.stringify(value)
  return value
}
