import React, { useEffect, useState } from 'react'
import { api, getUser } from '../../../api'
import { L, Modal, alertLevelBadge, fmtTime, Empty } from '../../../components/common'

/**
 * 连续高血压预警全流程：
 * 居民/家属补充测量信息 → 护士电话确认 → 医生处置（调整随访/建议门诊/联系家属）。
 */
export default function WarningTab({ recordId, warnings, followUps, onChanged }) {
  const user = getUser()
  const [suppTarget, setSuppTarget] = useState(null)
  const [nurseTarget, setNurseTarget] = useState(null)
  const [doctorTarget, setDoctorTarget] = useState(null)

  if (warnings.length === 0) {
    return <div className="card"><Empty text="暂无高血压预警。连续多次上传超标血压时会自动生成预警。" /></div>
  }

  return (
    <div>
      {warnings.map(w => (
        <div key={w.id} className="card" style={{ borderLeft: `4px solid ${w.status === 'RESOLVED' ? 'var(--green)' : 'var(--red)'}` }}>
          <div className="flex-between flex-wrap">
            <div className="flex flex-wrap">
              {alertLevelBadge(w.level)}
              <b>连续高血压预警</b>
              <span className={`badge badge-${w.status === 'OPEN' ? 'red' : w.status === 'NURSE_CONFIRMED' ? 'orange' : 'green'}`}>
                {L.warningStatus[w.status]}
              </span>
              <span className="muted" style={{ fontSize: 12 }}>{fmtTime(w.createdAt)}</span>
            </div>
            <div className="flex">
              {(user?.role === 'RESIDENT' || user?.role === 'FAMILY') && w.status === 'OPEN' && !w.suppAt && (
                <button className="btn btn-sm" onClick={() => setSuppTarget(w)}>📝 补充测量信息</button>
              )}
              {user?.role === 'NURSE' && w.status === 'OPEN' && (
                <button className="btn btn-sm btn-warn" onClick={() => setNurseTarget(w)}>📞 电话确认</button>
              )}
              {user?.role === 'DOCTOR' && w.status === 'NURSE_CONFIRMED' && (
                <button className="btn btn-sm" onClick={() => setDoctorTarget(w)}>🩺 医生处置</button>
              )}
            </div>
          </div>
          <div className="mt">{w.message}</div>

          {/* 流程进度 */}
          <div className="tag-row mt" style={{ fontSize: 12 }}>
            <span className={`badge badge-${w.suppAt ? 'green' : 'gray'}`}>① 补充信息{w.suppAt ? `（${w.suppBy}）` : '（待补充）'}</span>
            <span className={`badge badge-${w.nurseConfirmedAt ? 'green' : 'gray'}`}>② 护士电话确认{w.nurse ? `（${w.nurse.name}）` : ''}</span>
            <span className={`badge badge-${w.doctorHandledAt ? 'green' : 'gray'}`}>③ 医生处置{w.doctor ? `（${w.doctor.name}）` : ''}</span>
          </div>

          {w.suppAt && (
            <div className="mt" style={{ fontSize: 12, background: '#f6f9fc', borderRadius: 8, padding: 10 }}>
              <b>居民/家属补充：</b>测量时间 {w.suppMeasuredAt || '未填写'} ·
              {w.tookMed ? ' 已服药' : ' 未服药'} · 症状：{w.symptoms || '无'} · {w.sawDoctor ? '已就医' : '未就医'}
            </div>
          )}
          {w.nurseConfirmedAt && (
            <div className="mt" style={{ fontSize: 12, background: '#fdf6ec', borderRadius: 8, padding: 10 }}>
              <b>护士电话确认（{fmtTime(w.nurseConfirmedAt)}）：</b>症状：{w.nurseSymptoms || '无不适'} ·
              用药情况：{w.nurseMedNote || '不详'}{w.nurseNote && ` · 备注：${w.nurseNote}`}
            </div>
          )}
          {w.doctorHandledAt && (
            <div className="mt" style={{ fontSize: 12, background: '#eef6ec', borderRadius: 8, padding: 10 }}>
              <b>医生处置（{fmtTime(w.doctorHandledAt)}）：</b>{L.doctorAction[w.doctorAction]}
              {w.doctorNote && ` · ${w.doctorNote}`}
            </div>
          )}
        </div>
      ))}

      {suppTarget && <SupplementModal warning={suppTarget} onClose={() => setSuppTarget(null)} onDone={() => { setSuppTarget(null); onChanged() }} />}
      {nurseTarget && <NurseConfirmModal warning={nurseTarget} onClose={() => setNurseTarget(null)} onDone={() => { setNurseTarget(null); onChanged() }} />}
      {doctorTarget && <DoctorHandleModal warning={doctorTarget} recordId={recordId} followUps={followUps} onClose={() => setDoctorTarget(null)} onDone={() => { setDoctorTarget(null); onChanged() }} />}
    </div>
  )
}

/** 居民/家属补充：测量时间、是否服药、症状、是否就医 */
export function SupplementModal({ warning, onClose, onDone }) {
  const [form, setForm] = useState({ suppMeasuredAt: '', tookMed: true, symptoms: '', sawDoctor: false })
  const [error, setError] = useState('')
  const submit = async (e) => {
    e.preventDefault()
    try {
      await api.post(`/api/warnings/${warning.id}/supplement`, form)
      onDone()
    } catch (err) { setError(err.message) }
  }
  return (
    <Modal title="补充测量信息（连续高血压预警）" onClose={onClose}>
      <div className="muted mb" style={{ fontSize: 12 }}>
        您连续多次上传的血压偏高（最高 {warning.maxSys}/{warning.maxDia} mmHg），请补充以下信息，便于护士和医生判断：
      </div>
      <form onSubmit={submit}>
        <div className="form-item"><label>测量时间 *</label>
          <input required value={form.suppMeasuredAt} placeholder="如：近三天早晨 7 点左右"
            onChange={e => setForm({ ...form, suppMeasuredAt: e.target.value })} /></div>
        <div className="form-grid mt">
          <div className="form-item"><label>测量前是否已服药</label>
            <select value={String(form.tookMed)} onChange={e => setForm({ ...form, tookMed: e.target.value === 'true' })}>
              <option value="true">已服药</option>
              <option value="false">未服药</option>
            </select></div>
          <div className="form-item"><label>是否已就医</label>
            <select value={String(form.sawDoctor)} onChange={e => setForm({ ...form, sawDoctor: e.target.value === 'true' })}>
              <option value="false">未就医</option>
              <option value="true">已就医</option>
            </select></div>
        </div>
        <div className="form-item mt"><label>症状（头晕、胸闷等，无则填"无"）*</label>
          <textarea required rows={2} value={form.symptoms}
            onChange={e => setForm({ ...form, symptoms: e.target.value })} /></div>
        {error && <div className="error-text mt">{error}</div>}
        <button className="btn mt" type="submit">提交补充信息</button>
      </form>
    </Modal>
  )
}

/** 护士电话确认：记录症状和用药情况 */
export function NurseConfirmModal({ warning, onClose, onDone }) {
  const [form, setForm] = useState({ symptoms: '', medNote: '', note: '' })
  const [error, setError] = useState('')
  const submit = async (e) => {
    e.preventDefault()
    try {
      await api.post(`/api/warnings/${warning.id}/nurse-confirm`, form)
      onDone()
    } catch (err) { setError(err.message) }
  }
  return (
    <Modal title="护士电话确认（记录症状和用药情况）" onClose={onClose}>
      {warning.suppAt && (
        <div className="mb" style={{ fontSize: 12, background: '#f6f9fc', borderRadius: 8, padding: 10 }}>
          <b>居民已补充：</b>测量时间 {warning.suppMeasuredAt} · {warning.tookMed ? '已服药' : '未服药'} ·
          症状：{warning.symptoms || '无'} · {warning.sawDoctor ? '已就医' : '未就医'}
        </div>
      )}
      <form onSubmit={submit}>
        <div className="form-item"><label>电话中了解到的症状 *</label>
          <textarea required rows={2} value={form.symptoms} placeholder="如：晨起轻微头晕，无胸闷"
            onChange={e => setForm({ ...form, symptoms: e.target.value })} /></div>
        <div className="form-item mt"><label>用药情况 *</label>
          <textarea required rows={2} value={form.medNote} placeholder="如：近一周自行停用缬沙坦，仅服氨氯地平"
            onChange={e => setForm({ ...form, medNote: e.target.value })} /></div>
        <div className="form-item mt"><label>备注（可选）</label>
          <input value={form.note} onChange={e => setForm({ ...form, note: e.target.value })} /></div>
        {error && <div className="error-text mt">{error}</div>}
        <button className="btn mt" type="submit">确认并转医生处置</button>
      </form>
    </Modal>
  )
}

/** 医生处置：调整随访提醒 / 建议门诊 / 联系家属；调整频次需结合趋势、依从性、家属反馈 */
function DoctorHandleModal({ warning, recordId, followUps, onClose, onDone }) {
  const [form, setForm] = useState({ action: 'ADJUST_FOLLOWUP', note: '', reminderDays: 7 })
  const [trends, setTrends] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api.get(`/api/records/${recordId}/trends?days=30`).then(setTrends).catch(() => {})
  }, [recordId])

  const latestFeedback = followUps?.find(f => f.familyFeedback)?.familyFeedback

  const submit = async (e) => {
    e.preventDefault()
    try {
      await api.post(`/api/warnings/${warning.id}/doctor-handle`, {
        ...form, reminderDays: Number(form.reminderDays),
      })
      onDone()
    } catch (err) { setError(err.message) }
  }

  return (
    <Modal title="医生处置（连续高血压预警）" onClose={onClose} width={640}>
      {warning.nurseConfirmedAt && (
        <div className="mb" style={{ fontSize: 12, background: '#fdf6ec', borderRadius: 8, padding: 10 }}>
          <b>护士电话确认：</b>症状：{warning.nurseSymptoms || '无不适'} · 用药情况：{warning.nurseMedNote || '不详'}
        </div>
      )}
      <form onSubmit={submit}>
        <div className="form-item"><label>处置方式</label>
          <select value={form.action} onChange={e => setForm({ ...form, action: e.target.value })}>
            {Object.entries(L.doctorAction).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select></div>

        {form.action === 'ADJUST_FOLLOWUP' && (
          <div className="mt" style={{ border: '1px solid var(--border)', borderRadius: 10, padding: 12 }}>
            <div className="muted mb" style={{ fontSize: 12, fontWeight: 600 }}>
              调整提醒频次前，请结合以下随访依据：
            </div>
            <div className="grid-3" style={{ fontSize: 12 }}>
              <div>
                <div className="muted">📈 血压趋势（近30天）</div>
                {trends ? (
                  <div className="mt">
                    测量 {trends.bp.length} 次，超标 {trends.bp.filter(u => u.sys > (warning.record?.targetSys ?? 140) || u.dia > (warning.record?.targetDia ?? 90)).length} 次
                    {trends.selfVsFamily.selfAvgSys != null && `，自测均值 ${trends.selfVsFamily.selfAvgSys}/${trends.selfVsFamily.selfAvgDia}`}
                  </div>
                ) : <div className="mt muted">加载中…</div>}
              </div>
              <div>
                <div className="muted">💊 用药依从性</div>
                <div className="mt">
                  {trends?.adherence != null ? `按时服药率 ${trends.adherence}%（漏服 ${trends.missedCount} 次）` : '近30天无用药打卡'}
                </div>
              </div>
              <div>
                <div className="muted">👪 家属反馈</div>
                <div className="mt">{latestFeedback || '近期随访无家属反馈记录'}</div>
              </div>
            </div>
            <div className="form-item mt"><label>新的随访提醒间隔（天）*</label>
              <input required type="number" min="1" max="90" value={form.reminderDays}
                onChange={e => setForm({ ...form, reminderDays: e.target.value })} /></div>
          </div>
        )}

        <div className="form-item mt"><label>处置说明</label>
          <textarea rows={2} value={form.note} placeholder={form.action === 'CLINIC' ? '如：建议一周内社区门诊复诊' : ''}
            onChange={e => setForm({ ...form, note: e.target.value })} /></div>
        {error && <div className="error-text mt">{error}</div>}
        <button className="btn mt" type="submit">提交处置（办结预警）</button>
      </form>
    </Modal>
  )
}
