import React, { useState } from 'react'
import { api } from '../../../api'
import { L, levelBadge } from '../../../components/common'

/** 分层管理建议面板：系统按家庭支持/依从性/异常频率给出建议，医生确认调整。 */
export default function StratifyPanel({ record, suggestion, onChanged }) {
  const [busy, setBusy] = useState(false)
  if (!suggestion) return null
  const mismatch = suggestion.suggestedLevel !== record.manageLevel

  const apply = async () => {
    const reason = window.prompt(`调整为「${L.level[suggestion.suggestedLevel]}」的理由（可选）：`, suggestion.reason)
    if (reason === null) return
    setBusy(true)
    try {
      await api.post(`/api/records/${record.id}/stratify`, {
        manageLevel: suggestion.suggestedLevel,
        reason,
      })
      onChanged()
    } catch (err) { alert(err.message) } finally { setBusy(false) }
  }

  return (
    <div className="card" style={{ borderLeft: `4px solid ${mismatch ? 'var(--orange)' : 'var(--green)'}` }}>
      <div className="flex-between flex-wrap">
        <div>
          <div className="card-title" style={{ marginBottom: 6 }}>🧭 分层管理建议</div>
          <div className="muted" style={{ fontSize: 12 }}>
            近 30 天：上传 {suggestion.uploads30d} 次 · 依从性 {suggestion.adherence30d}% · 告警 {suggestion.alerts30d} 条
            · {suggestion.hasProxy ? '已有家属代管' : '无家属代管'} · 家庭支持：{L.support[record.familySupport]}
          </div>
          <div className="mt" style={{ fontSize: 13 }}>
            系统建议：<b>{L.level[suggestion.suggestedLevel]}</b>（{suggestion.reason}）
            ；当前层级：{levelBadge(record.manageLevel)}
          </div>
        </div>
        {mismatch && (
          <button className="btn btn-warn" disabled={busy} onClick={apply}>
            {busy ? '调整中…' : '采纳建议并调整分层'}
          </button>
        )}
      </div>
    </div>
  )
}
