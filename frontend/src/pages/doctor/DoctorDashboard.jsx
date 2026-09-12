import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../api'
import { L, StatCard, alertLevelBadge, levelBadge, fmtTime, fmtDate, Empty } from '../../components/common'

export default function DoctorDashboard() {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api.get('/api/dashboard').then(setData).catch(e => setError(e.message))
  }, [])

  if (error) return <div className="error-text">{error}</div>
  if (!data) return <div className="muted">加载中…</div>

  return (
    <div>
      <div className="page-title">医生工作台</div>
      <div className="page-sub">待办告警、随访计划与重点管理居民一览</div>

      <div className="stat-grid">
        <StatCard label="在管居民" value={data.activeRecords} color="blue" />
        <StatCard label="待处理告警" value={data.openAlertCount} color={data.openAlertCount > 0 ? 'red' : 'green'} />
        <StatCard label="到期随访" value={data.duePlanCount} color={data.duePlanCount > 0 ? 'orange' : 'green'} />
        <StatCard label="重点名单" value={data.keyFocusCount} color={data.keyFocusCount > 0 ? 'red' : ''} />
        <StatCard label="失访居民" value={data.lostCount} color={data.lostCount > 0 ? 'red' : ''} />
      </div>

      <div className="grid-2">
        <div className="card">
          <div className="card-title">🔔 最新待处理告警</div>
          {data.recentAlerts.length === 0 && <Empty text="暂无待处理告警" />}
          {data.recentAlerts.map(a => (
            <div key={a.id} className={`alert-banner alert-${a.level.toLowerCase()}`}>
              <div>
                <div className="flex">
                  {alertLevelBadge(a.level)}
                  <b>{L.alertType[a.alertType]}</b>
                  <Link to={`/records/${a.record.id}`}>{a.record.resident.name}</Link>
                </div>
                <div className="muted" style={{ marginTop: 4 }}>{a.message}</div>
                <div className="muted" style={{ fontSize: 11 }}>{fmtTime(a.createdAt)}</div>
              </div>
              <Link className="btn btn-sm" to={`/records/${a.record.id}`}>去处理</Link>
            </div>
          ))}
        </div>

        <div className="card">
          <div className="card-title">📅 到期随访计划</div>
          {data.duePlans.length === 0 && <Empty text="暂无到期随访" />}
          {data.duePlans.map(p => (
            <div key={p.id} className="flex-between" style={{ padding: '10px 0', borderBottom: '1px solid var(--border)' }}>
              <div>
                <Link to={`/records/${p.record.id}`}><b>{p.record.resident.name}</b></Link>
                <span className="muted"> · {L.planType[p.planType]} · 应访日期 {fmtDate(p.nextDueDate)}</span>
                <div style={{ marginTop: 4 }}>{levelBadge(p.record.manageLevel)}</div>
              </div>
              <Link className="btn btn-sm" to={`/records/${p.record.id}?tab=followup`}>去随访</Link>
            </div>
          ))}
        </div>
      </div>

      <div className="card">
        <div className="card-title">📊 分层管理分布</div>
        <div className="tag-row">
          {Object.entries(L.level).map(([k, label]) => (
            <span key={k} className="badge badge-blue" style={{ fontSize: 13, padding: '6px 14px' }}>
              {label}：{data.levelDist[k] || 0} 人
            </span>
          ))}
        </div>
      </div>
    </div>
  )
}
