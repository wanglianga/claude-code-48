import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import { L, alertLevelBadge, fmtTime, Empty } from '../components/common'
import { NurseConfirmModal } from './doctor/detail/WarningTab'

/** 告警任务中心（护士/医生）：处理连续漏服、血压异常、低血糖、不良反应、长期未上传。 */
export default function NurseAlerts() {
  const [alerts, setAlerts] = useState([])
  const [warnings, setWarnings] = useState([])
  const [status, setStatus] = useState('OPEN')
  const [error, setError] = useState('')
  const [confirmTarget, setConfirmTarget] = useState(null)

  const load = () => {
    api.get(`/api/alerts?status=${status}`).then(setAlerts).catch(e => setError(e.message))
    api.get('/api/warnings?status=OPEN').then(setWarnings).catch(() => {})
  }
  useEffect(load, [status])

  const handle = async (a, action) => {
    const note = window.prompt(action === 'ack' ? '知晓备注（可选）：' : '办结说明（可选）：')
    if (note === null) return
    try {
      await api.post(`/api/alerts/${a.id}/${action}`, { note })
      load()
    } catch (err) { alert(err.message) }
  }

  return (
    <div>
      <div className="page-title">告警任务中心</div>
      <div className="page-sub">连续漏服 / 血压异常升高 / 低血糖 / 药物不良反应 / 长期未上传，推送至护士与医生处理</div>

      {warnings.length > 0 && (
        <div className="card" style={{ borderLeft: '4px solid var(--red)' }}>
          <div className="card-title">⚠️ 连续高血压预警（待电话确认 {warnings.length}）</div>
          {warnings.map(w => (
            <div key={w.id} className="flex-between flex-wrap" style={{ padding: '10px 0', borderBottom: '1px solid var(--border)' }}>
              <div style={{ flex: 1 }}>
                <div className="flex flex-wrap">
                  {alertLevelBadge(w.level)}
                  <Link to={`/records/${w.record.id}`}><b>{w.record.resident.name}</b></Link>
                  <span className="muted" style={{ fontSize: 12 }}>{fmtTime(w.createdAt)}</span>
                </div>
                <div style={{ marginTop: 4 }}>{w.message}</div>
                {w.suppAt && (
                  <div className="muted" style={{ fontSize: 12, marginTop: 4 }}>
                    居民已补充：测量时间 {w.suppMeasuredAt} · {w.tookMed ? '已服药' : '未服药'} ·
                    症状：{w.symptoms || '无'} · {w.sawDoctor ? '已就医' : '未就医'}
                  </div>
                )}
              </div>
              <div className="flex">
                <button className="btn btn-sm btn-warn" onClick={() => setConfirmTarget(w)}>📞 电话确认</button>
                <Link className="btn btn-sm btn-outline" to={`/records/${w.record.id}?tab=warning`}>查看档案</Link>
              </div>
            </div>
          ))}
        </div>
      )}

      <div className="tabs">
        {[['OPEN', '待处理'], ['ACKED', '已知晓'], ['RESOLVED', '已办结'], ['ALL', '全部']].map(([k, label]) => (
          <button key={k} className={status === k ? 'tab active' : 'tab'} onClick={() => setStatus(k)}>{label}</button>
        ))}
      </div>

      {error && <div className="error-text">{error}</div>}
      {alerts.length === 0 ? <div className="card"><Empty text="暂无告警" /></div> : alerts.map(a => (
        <div key={a.id} className={`alert-banner alert-${a.level.toLowerCase()}`}>
          <div style={{ flex: 1 }}>
            <div className="flex flex-wrap">
              {alertLevelBadge(a.level)}
              <b>{L.alertType[a.alertType]}</b>
              <Link to={`/records/${a.record.id}`}><b>{a.record.resident.name}</b></Link>
              <span className="muted">（{L.disease[a.record.diseaseType]} · 医生 {a.record.doctor.name}）</span>
            </div>
            <div style={{ marginTop: 6 }}>{a.message}</div>
            <div className="muted" style={{ fontSize: 11, marginTop: 4 }}>
              {fmtTime(a.createdAt)}
              {a.handledBy && ` · ${a.handledBy.name} 处理于 ${fmtTime(a.handledAt)}${a.handleNote ? '：' + a.handleNote : ''}`}
            </div>
          </div>
          <div className="flex">
            {a.status === 'OPEN' && <button className="btn btn-sm btn-warn" onClick={() => handle(a, 'ack')}>知晓</button>}
            {a.status !== 'RESOLVED' && <button className="btn btn-sm" onClick={() => handle(a, 'resolve')}>办结</button>}
            <Link className="btn btn-sm btn-outline" to={`/records/${a.record.id}`}>查看档案</Link>
          </div>
        </div>
      ))}

      {confirmTarget && (
        <NurseConfirmModal
          warning={confirmTarget}
          onClose={() => setConfirmTarget(null)}
          onDone={() => { setConfirmTarget(null); load() }}
        />
      )}
    </div>
  )
}
