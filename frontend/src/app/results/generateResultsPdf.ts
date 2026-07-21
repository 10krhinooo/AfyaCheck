import type { RiskAssessmentDto } from '../../lib/results-types'

// Matches src/index.css's @theme palette so the PDF reads as the same brand as the app.
const TEAL_700: [number, number, number] = [28, 76, 65]
const TEAL_600: [number, number, number] = [37, 100, 86]
const TEAL_50: [number, number, number] = [238, 246, 244]
const INK: [number, number, number] = [28, 37, 33]
const INK_SOFT: [number, number, number] = [74, 86, 79]

const RISK_TONE: Record<RiskAssessmentDto['riskLevel'], [number, number, number]> = {
  Low: TEAL_600,
  Medium: [181, 112, 26], // amber-600
  High: [147, 57, 42], // coral-700
}

const riskLabel: Record<RiskAssessmentDto['riskLevel'], string> = {
  Low: 'Low risk',
  Medium: 'Medium risk',
  High: 'High risk',
}

// jsPDF (and its font/canvas machinery) is only worth the download when someone actually
// wants a PDF, so it's dynamically imported here rather than bundled into ResultsPage's
// chunk (see budget.json's 15KB ResultsPage budget).
export async function downloadResultsPdf(assessment: RiskAssessmentDto, sessionId: string) {
  const { jsPDF } = await import('jspdf')
  const doc = new jsPDF({ unit: 'mm', format: 'a4' })
  const pageWidth = doc.internal.pageSize.getWidth()
  const marginX = 20
  const contentWidth = pageWidth - marginX * 2

  // Header band with the AfyaCheck wordmark — there's no logo image asset in the app (the
  // brand mark is the styled "AfyaCheck" text in Fraunces, see NavBar.tsx), so the PDF
  // reproduces that same wordmark treatment instead of inventing an image that doesn't exist.
  doc.setFillColor(...TEAL_700)
  doc.rect(0, 0, pageWidth, 32, 'F')
  doc.setFont('times', 'bold')
  doc.setFontSize(22)
  doc.setTextColor(255, 255, 255)
  doc.text('AfyaCheck', marginX, 20)
  doc.setFont('helvetica', 'normal')
  doc.setFontSize(9)
  doc.setTextColor(210, 230, 225)
  doc.text('Confidential STI/HIV risk assessment', marginX, 26)

  let y = 48

  doc.setFont('helvetica', 'normal')
  doc.setFontSize(10)
  doc.setTextColor(...INK_SOFT)
  const createdAt = new Date(assessment.createdAt)
  doc.text(
    `Generated ${createdAt.toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' })}`,
    marginX,
    y,
  )
  y += 12

  // Risk result card
  const tone = RISK_TONE[assessment.riskLevel]
  doc.setFillColor(...TEAL_50)
  doc.roundedRect(marginX, y, contentWidth, 34, 3, 3, 'F')
  doc.setFont('helvetica', 'bold')
  doc.setFontSize(12)
  doc.setTextColor(...tone)
  doc.text(riskLabel[assessment.riskLevel], marginX + 8, y + 14)
  doc.setFont('times', 'bold')
  doc.setFontSize(24)
  doc.setTextColor(...INK)
  doc.text(`${assessment.riskScore} / 100`, marginX + 8, y + 27)
  y += 44

  // Recommendations
  doc.setFont('helvetica', 'bold')
  doc.setFontSize(13)
  doc.setTextColor(...INK)
  doc.text('What this means for you', marginX, y)
  y += 8

  doc.setFont('helvetica', 'normal')
  doc.setFontSize(11)
  doc.setTextColor(...INK_SOFT)
  for (const rec of assessment.recommendations) {
    const lines = doc.splitTextToSize(rec, contentWidth - 8)
    if (y + lines.length * 6 > 270) {
      doc.addPage()
      y = 20
    }
    doc.setFillColor(...tone)
    doc.circle(marginX + 1.5, y - 1.5, 1, 'F')
    doc.text(lines, marginX + 6, y)
    y += lines.length * 6 + 3
  }

  // Footer disclaimer
  const pageCount = doc.getNumberOfPages()
  for (let i = 1; i <= pageCount; i++) {
    doc.setPage(i)
    const pageHeight = doc.internal.pageSize.getHeight()
    doc.setDrawColor(...TEAL_50)
    doc.line(marginX, pageHeight - 22, pageWidth - marginX, pageHeight - 22)
    doc.setFont('helvetica', 'normal')
    doc.setFontSize(8)
    doc.setTextColor(...INK_SOFT)
    doc.text(
      'This is a screening tool, not a medical diagnosis. Speak with a healthcare provider about your results.',
      marginX,
      pageHeight - 16,
      { maxWidth: contentWidth },
    )
    doc.text(`Reference: ${sessionId.slice(0, 12)}`, marginX, pageHeight - 10)
  }

  const dateStamp = createdAt.toISOString().slice(0, 10)
  doc.save(`afyacheck-results-${dateStamp}.pdf`)
}
