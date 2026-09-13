import React, { useEffect, useState } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { api, getUser } from '../../api'
import { L, diseaseBadge, levelBadge, statusBadge, fmtDate } from '../../components/common'
import TrendTab from './detail/TrendTab'
import MedTab from './detail/MedTab'
import AlertTab from './detail/AlertTab'
import FollowUpTab from './detail/FollowUpTab'
import EventTab from './detail/EventTab'
import FamilyTab from './detail/FamilyTab'
import ReferralTab from './detail/ReferralTab'
import StratifyPanel from './detail/StratifyPanel'
import CausePanel from './detail/CausePanel'

export default function RecordDetail() {
  const { id } = useParams()
  const [searchParams] = useSearchParams()
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [tab, setTab] = useState(searchParams.get('tab') || 'trend')
  const user = getUser()
  const isStaff = user && (user.role === 'DOCTOR' || user.role === 'NURSE' || user.role === 'ADMIN')

  const load = () => api.get(`/api/records/${id}`).then(setData).catch(e => setError(e.message))
  useEffect(() => { load() }, [id])

  if (error) return <div className="error-text">{error}</div>
  if (!data) return <div className="muted">加载中…</div>
  const r = data.record
  const openAlerts = data.alerts.filter(a => a.status === 'OPEN')

  return (
    <div>
      {/* 头部：档案概要 */}
      <div className="card">
        <div className="flex-between flex-wrap">
          <div>
            <div className="flex flex-wrap">
              <span style={{ fontSize: 20, fontWeight: 800 }}>{r.resident.name}</span>
              {diseaseBadge(r.diseaseType)}
              {levelBadge(r.manageLevel)}
              {statusBadge(r.status)}
            </div>
            <div className="muted mt" style={{ fontSize: 12 }}>
              {r.diagnosis} {r.complications && `· 并发症：${r.complications}`}
            </div>
            <div className="muted" style={{ fontSize: 12, marginTop: 4 }}>
              目标血压 &lt;{r.targetSys}/{r.targetDia} mmHg · 血糖 {r.glucoseMin}~{r.glucoseMax} mmol/L
              · {r.insurance || '未登记医保'} · 家庭医生 {r.doctor.name}
              {r.nurse && ` · 护士 ${r.nurse.name}`} · 建档 {fmtDate(r.createdAt)}
            </div>
          </div>
          {openAlerts.length > 0 && (
            <span className="badge badge-red" style={{ fontSize: 13, padding: '6px 14px' }}>
              🔔 {openAlerts.length} 条待处理告警
            </span>
          )}
        </div>
      </div>

      {/* 分层建议（仅医护可见） */}
      {isStaff && <StratifyPanel record={r} suggestion={data.suggestion} onChanged={load} />}

      {/* 异常原因判断：疾病变化 / 用药依从性 / 家属照护缺口（仅医护可见） */}
      {isStaff && <CausePanel causes={data.causes} />}

      <div className="tabs">
        {[
          ['trend', '📈 趋势与依从性'],
          ['med', '💊 用药管理'],
          ['alert', `🔔 告警 (${data.alerts.length})`],
          ['followup', `🩺 随访 (${data.followUps.length})`],
          ['family', `👨‍👩‍👧 家属 (${data.familyContacts.length})`],
          ['referral', '🏥 转诊与住院'],
          ['event', `🕘 事件流 (${data.events.length})`],
        ].map(([k, label]) => (
          <button key={k} className={tab === k ? 'tab active' : 'tab'} onClick={() => setTab(k)}>{label}</button>
        ))}
      </div>

      {tab === 'trend' && <TrendTab recordId={id} record={r} />}
      {tab === 'med' && <MedTab recordId={id} medications={data.medications} isDoctor={user?.role === 'DOCTOR'} onChanged={load} />}
      {tab === 'alert' && <AlertTab alerts={data.alerts} isStaff={isStaff} onChanged={load} />}
      {tab === 'followup' && <FollowUpTab recordId={id} followUps={data.followUps} plans={data.plans} isDoctor={user?.role === 'DOCTOR'} onChanged={load} />}
      {tab === 'family' && <FamilyTab recordId={id} contacts={data.familyContacts} isStaff={isStaff} onChanged={load} />}
      {tab === 'referral' && <ReferralTab recordId={id} referrals={data.referrals} hospitalizations={data.hospitalizations} isStaff={isStaff} onChanged={load} />}
      {tab === 'event' && <EventTab events={data.events} />}
    </div>
  )
}
