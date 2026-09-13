import React, { useEffect, useState } from 'react'
import { api } from '../api'
import { L, diseaseBadge, levelBadge, fmtTime, fmtDate, Empty } from '../components/common'
import UploadPanel from '../components/UploadPanel'
import { SupplementModal } from './doctor/detail/WarningTab'

/** 居民端：今日用药提醒、家庭数据上传、我的计划与历史记录。 */
export default function ResidentHome() {
  const [data, setData] = useState(null)
  const [warnings, setWarnings] = useState([])
  const [suppTarget, setSuppTarget] = useState(null)
  const [error, setError] = useState('')

  const load = () => {
    api.get('/api/dashboard').then(setData).catch(e => setError(e.message))
    api.get('/api/warnings?status=OPEN').then(setWarnings).catch(() => {})
  }
  useEffect(() => { load() }, [])

  if (error) return <div className="error-text">{error}</div>
  if (!data) return <div className="muted">加载中…</div>
  if (!data.record) {
    return (
      <div className="card">
        <div className="page-title">我的健康</div>
        <Empty text="您还没有慢病档案，请联系社区医生为您建档。" />
      </div>
    )
  }
  const r = data.record

  return (
    <div>
      <div className="page-title">我的健康</div>
      <div className="page-sub">每日测量、按时服药，数据会自动同步给您的家庭医生</div>

      {warnings.filter(w => !w.suppAt).map(w => (
        <div key={w.id} className="alert-banner alert-critical">
          <div>
            <b>⚠️ 连续高血压预警</b>
            <div style={{ marginTop: 4 }}>{w.message}</div>
            <div className="muted" style={{ fontSize: 12, marginTop: 4 }}>请补充测量时间、服药、症状和就医情况，社区护士将与您电话确认</div>
          </div>
          <button className="btn btn-sm" onClick={() => setSuppTarget(w)}>📝 补充信息</button>
        </div>
      ))}

      <div className="card">
        <div className="flex flex-wrap">
          <b style={{ fontSize: 16 }}>{r.resident.name}</b>
          {diseaseBadge(r.diseaseType)}
          {levelBadge(r.manageLevel)}
          <span className="muted">目标血压 &lt;{r.targetSys}/{r.targetDia} mmHg · 血糖 {r.glucoseMin}~{r.glucoseMax} mmol/L</span>
        </div>
        <div className="muted mt" style={{ fontSize: 12 }}>
          家庭医生：{r.doctor.name} {r.nurse && `· 社区护士：${r.nurse.name}`}
          {data.nextFollowUp && ` · 下次随访：${fmtDate(data.nextFollowUp)}`}
        </div>
      </div>

      <div className="card">
        <div className="card-title">💊 今日用药提醒</div>
        {data.todayMeds.length === 0 ? <Empty text="今日无用药任务" /> : (
          <div className="tag-row">
            {data.todayMeds.map(m => (
              <span key={m.id} className={`badge badge-${m.takenToday ? 'green' : 'orange'}`}
                style={{ fontSize: 13, padding: '8px 14px' }}>
                {m.takenToday ? '✓' : '⏰'} {m.name} {m.dosage}（{m.timeSlots}）
                {m.takenToday ? ' 已服' : ' 待服药'}
              </span>
            ))}
          </div>
        )}
        <div className="muted mt" style={{ fontSize: 12 }}>服药后请在下方「用药」中打卡，连续漏服会提醒医生和家人。</div>
      </div>

      <div className="card">
        <div className="card-title">📤 上传今日数据</div>
        <UploadPanel medications={data.todayMeds} onDone={load} />
      </div>

      <div className="card">
        <div className="card-title">🕘 最近上传记录</div>
        {data.recentUploads.length === 0 ? <Empty text="暂无记录" /> : (
          <table>
            <thead><tr><th>时间</th><th>类型</th><th>内容</th></tr></thead>
            <tbody>
              {data.recentUploads.map(u => (
                <tr key={u.id}>
                  <td>{fmtTime(u.measuredAt)}</td>
                  <td>{L.uploadType[u.type]}</td>
                  <td>
                    {u.type === 'BP' && `${u.sys}/${u.dia} mmHg${u.heartRate ? ` · 心率 ${u.heartRate}` : ''}`}
                    {u.type === 'GLUCOSE' && `${u.glucose} mmol/L`}
                    {u.type === 'MEDICATION' && `${u.medicationName} · ${L.medLogStatus[u.medStatus]}`}
                    {u.type === 'DIET' && u.dietNote}
                    {u.type === 'EXERCISE' && u.exerciseNote}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {suppTarget && (
        <SupplementModal
          warning={suppTarget}
          onClose={() => setSuppTarget(null)}
          onDone={() => { setSuppTarget(null); load() }}
        />
      )}
    </div>
  )
}
