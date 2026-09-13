import React from 'react'

/** 异常原因判断：疾病变化 / 用药依从性 / 家属照护缺口，每条结论附可核验依据。 */
export default function CausePanel({ causes }) {
  if (!causes || causes.length === 0) return null
  const icon = { DISEASE: '🩺', ADHERENCE: '💊', CARE_GAP: '👨‍👩‍👧' }
  return (
    <div className="card">
      <div className="card-title">🔍 异常原因判断（基于近 30 天家庭监测数据）</div>
      <div className="grid-3">
        {causes.map(c => (
          <div key={c.type} style={{
            border: `1px solid ${c.detected ? '#f5c6c6' : 'var(--border)'}`,
            borderLeft: `4px solid ${c.detected ? 'var(--red)' : 'var(--green)'}`,
            borderRadius: 10, padding: 14, background: c.detected ? '#fffafa' : '#fff',
          }}>
            <div className="flex-between">
              <b>{icon[c.type] || '▪️'} {c.label}</b>
              {c.detected
                ? <span className="badge badge-red">检出</span>
                : <span className="badge badge-green">未检出</span>}
            </div>
            <ul style={{ margin: '10px 0 0', paddingLeft: 18, fontSize: 12, color: 'var(--muted)', lineHeight: 1.7 }}>
              {c.evidence.map((e, i) => <li key={i}>{e}</li>)}
            </ul>
          </div>
        ))}
      </div>
    </div>
  )
}
