import { useId, useState } from 'react'
import { Bar, Doughnut } from 'react-chartjs-2'
import {
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  Legend,
  LinearScale,
  ArcElement,
  Tooltip,
} from 'chart.js'
import { Card } from '../../components/Card'
import { Button } from '../../components/Button'
import type { ChartData } from './types'

ChartJS.register(BarElement, CategoryScale, LinearScale, ArcElement, Legend, Tooltip)

// Chart.js can't read CSS custom properties, so these are hardcoded copies of the
// brand tokens in index.css, matched to the Inter body font used everywhere else.
ChartJS.defaults.font.family = "'Inter', ui-sans-serif, system-ui, -apple-system, sans-serif"
ChartJS.defaults.color = '#4a564f'

const palette = [
  '#2f7d6d',
  '#7bb5aa',
  '#e0982f',
  '#c4523f',
  '#256456',
  '#93392a',
  '#a8d1c8',
  '#4f9686',
  '#163a31',
]

// Canvas-rendered charts are invisible to screen readers by default (WCAG requirement for
// this route), every chart here ships a paired, always-in-the-DOM data table that a sighted
// user can toggle to and a screen reader user can reach directly, with the same numbers.
export function ChartWithTable({
  title,
  data,
  variant = 'bar',
}: {
  title: string
  data: ChartData
  variant?: 'bar' | 'doughnut'
}) {
  const [showTable, setShowTable] = useState(false)
  const tableId = useId()

  const chartData = {
    labels: data.labels,
    datasets: [
      {
        label: title,
        data: data.data,
        backgroundColor: data.backgroundColors ?? palette,
      },
    ],
  }

  return (
    <Card className="p-6">
      <div className="flex items-center justify-between">
        <h3 className="text-lg text-ink">{title}</h3>
        <Button
          variant="ghost"
          aria-expanded={showTable}
          aria-controls={tableId}
          onClick={() => setShowTable((v) => !v)}
        >
          {showTable ? 'Show chart' : 'View as table'}
        </Button>
      </div>

      <div className="mt-4">
        {showTable ? (
          <table id={tableId} className="w-full text-left text-sm">
            <caption className="sr-only">{title}: data table</caption>
            <thead>
              <tr className="border-b border-teal-100 text-ink-soft">
                <th scope="col" className="py-2">
                  Label
                </th>
                <th scope="col" className="py-2 text-right">
                  Value
                </th>
              </tr>
            </thead>
            <tbody>
              {data.labels.map((label, i) => (
                <tr key={label} className="border-b border-teal-50">
                  <td className="py-2 text-ink">{label}</td>
                  <td className="py-2 text-right text-ink">{data.data[i]}</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <div aria-hidden="true" className="h-64">
            {variant === 'bar' ? (
              <Bar data={chartData} options={{ maintainAspectRatio: false, responsive: true }} />
            ) : (
              <Doughnut data={chartData} options={{ maintainAspectRatio: false, responsive: true }} />
            )}
          </div>
        )}
      </div>
    </Card>
  )
}
