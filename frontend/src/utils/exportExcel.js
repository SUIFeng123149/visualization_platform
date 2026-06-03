function escapeXml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;')
}

// Excel formula injection protection: prefix values starting with =, +, -, @ with a tab
function sanitizeCell(value) {
  const str = String(value ?? '')
  if (/^[=+\-@]/.test(str)) {
    return '\t' + str
  }
  return str
}

function cell(value) {
  const type = typeof value === 'number' && Number.isFinite(value) ? 'Number' : 'String'
  const safe = type === 'Number' ? escapeXml(value) : escapeXml(sanitizeCell(value))
  return `<Cell><Data ss:Type="${type}">${safe}</Data></Cell>`
}

function worksheet(sheet) {
  const header = `<Row>${sheet.columns.map((column) => cell(column.label)).join('')}</Row>`
  const rows = sheet.rows
    .map((row) => `<Row>${sheet.columns.map((column) => cell(row[column.key])).join('')}</Row>`)
    .join('')

  return `
    <Worksheet ss:Name="${escapeXml(sheet.name)}">
      <Table>${header}${rows}</Table>
    </Worksheet>
  `
}

export function exportExcelWorkbook(filename, sheets) {
  const workbook = `<?xml version="1.0" encoding="UTF-8"?>
  <Workbook
    xmlns="urn:schemas-microsoft-com:office:spreadsheet"
    xmlns:o="urn:schemas-microsoft-com:office:office"
    xmlns:x="urn:schemas-microsoft-com:office:excel"
    xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">
    ${sheets.map(worksheet).join('')}
  </Workbook>`

  const blob = new Blob([workbook], {
    type: 'application/vnd.ms-excel;charset=utf-8',
  })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename.endsWith('.xls') ? filename : `${filename}.xls`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}
