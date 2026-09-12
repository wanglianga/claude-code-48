import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import { L, diseaseBadge, levelBadge, statusBadge, alertLevelBadge, fmtTime, Empty } from '../components/common'
import UploadPanel from '../components/UploadPanel'

/** 家属端：代管居民、代测代报、接收告警。 */
export default function FamilyHome() {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [selected, setSelected] = useState(null)

  const load = () => api.get('/api/dashboard').then(setData).catch(e => setError(e.message))
  useEffect(() => { load() }, [])

  if (error) return <div className="error-text">{error}</div>
  if (!data) return <div className="muted">加载中…</div>

  const current = selected ?? data.records[0]?.record?.id

  return (
    <div>
      <div className="page-title">家人健康</div>
      <div className="page-sub">为家人代测血压血糖、代报用药情况，异常时您会收到提醒</div>

      {data.records.length === 0 ? (
        <div className="card"><Empty text="您还没有关联的家人档案，请联系社区医生将您登记为家属联系人。" /></div>
      ) : (
        <>
          <div className="grid-2">
            {data.records.map(item => (
              <div key={item.record.id} className="card"
                style={{ cursor: 'pointer', borderColor: current === item.record.id ? 'var(--primary)' : undefined }}
                onClick={() => setSelected(item.record.id)}>
                <div className="flex flex-wrap">
                  <b style={{ fontSize: 16 }}>{item.record.resident.name}</b>
                  <span className="muted">（{item.relation || '家人'}）</span>
                  {diseaseBadge(item.record.diseaseType)}
                  {levelBadge(item.record.manageLevel)}
                  {statusBadge(item.record.status)}
                  {item.proxy && <span className="badge badge-purple">我代管</span>}
                </div>
                <div className="muted mt" style={{ fontSize: 12 }}>
                  目标血压 &lt;{item.record.targetSys}/{item.record.targetDia} ·
                  最近上传：{item.lastUpload ? fmtTime(item.lastUpload) : '暂无'}
                </div>
                <div className="mt">
                  <Link className="btn btn-sm btn-outline" to={`/records/${item.record.id}`}>查看健康档案</Link>
                </div>
              </div>
            ))}
          </div>

          {current && (
            <div className="card">
              <div className="card-title">📤 为 {data.records.find(x => x.record.id === current)?.record.resident.name} 代传数据</div>
              <UploadPanel recordId={current} onDone={load} />
            </div>
          )}
        </>
      )}

      <div className="card">
        <div className="card-title">🔔 家人健康提醒（{data.openAlerts.length}）</div>
        {data.openAlerts.length === 0 ? <Empty text="暂无提醒，家人健康状况平稳" /> : data.openAlerts.map(a => (
          <div key={a.id} className={`alert-banner alert-${a.level.toLowerCase()}`}>
            <div>
              <div className="flex">
                {alertLevelBadge(a.level)}
                <b>{L.alertType[a.alertType]}</b>
                <span>{a.record.resident.name}</span>
              </div>
              <div style={{ marginTop: 4 }}>{a.message}</div>
              <div className="muted" style={{ fontSize: 11, marginTop: 4 }}>{fmtTime(a.createdAt)}</div>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
