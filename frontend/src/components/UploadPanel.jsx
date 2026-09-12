import React, { useState } from 'react'
import { api } from '../api'

/** 家庭数据上传面板（居民自测 / 家属代测共用）。 */
export default function UploadPanel({ recordId, medications, onDone }) {
  const [type, setType] = useState('BP')
  const [form, setForm] = useState({
    sys: '', dia: '', heartRate: '', glucose: '',
    medicationName: '', medStatus: 'TAKEN', dietNote: '', exerciseNote: '', note: '',
  })
  const [msg, setMsg] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const set = (k, v) => { setForm({ ...form, [k]: v }); setMsg(''); setError('') }

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true); setError(''); setMsg('')
    try {
      const body = { type, recordId }
      if (type === 'BP') {
        body.sys = Number(form.sys); body.dia = Number(form.dia)
        if (form.heartRate) body.heartRate = Number(form.heartRate)
      } else if (type === 'GLUCOSE') {
        body.glucose = Number(form.glucose)
      } else if (type === 'MEDICATION') {
        body.medicationName = form.medicationName
        body.medStatus = form.medStatus
        if (form.note) body.note = form.note
      } else if (type === 'DIET') {
        body.dietNote = form.dietNote
      } else if (type === 'EXERCISE') {
        body.exerciseNote = form.exerciseNote
      }
      await api.post('/api/uploads', body)
      setMsg('上传成功，平台已自动评估是否达标 ✓')
      setForm({ ...form, sys: '', dia: '', heartRate: '', glucose: '', dietNote: '', exerciseNote: '', note: '' })
      onDone && onDone()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  const types = [
    ['BP', '🩸', '血压'], ['GLUCOSE', '🍬', '血糖'], ['MEDICATION', '💊', '用药'],
    ['DIET', '🥗', '饮食'], ['EXERCISE', '🏃', '运动'],
  ]

  return (
    <div>
      <div className="upload-quick mb">
        {types.map(([k, icon, label]) => (
          <button key={k} type="button" onClick={() => setType(k)}
            style={type === k ? { borderColor: 'var(--primary)', color: 'var(--primary)', background: '#eef6fd' } : {}}>
            <span className="icon">{icon}</span>{label}
          </button>
        ))}
      </div>

      <form onSubmit={submit}>
        {type === 'BP' && (
          <div className="form-grid">
            <div className="form-item"><label>收缩压（高压）mmHg *</label>
              <input required type="number" min="50" max="260" value={form.sys} onChange={e => set('sys', e.target.value)} placeholder="如 128" /></div>
            <div className="form-item"><label>舒张压（低压）mmHg *</label>
              <input required type="number" min="30" max="180" value={form.dia} onChange={e => set('dia', e.target.value)} placeholder="如 78" /></div>
            <div className="form-item"><label>心率（次/分，可选）</label>
              <input type="number" min="30" max="220" value={form.heartRate} onChange={e => set('heartRate', e.target.value)} /></div>
          </div>
        )}
        {type === 'GLUCOSE' && (
          <div className="form-item"><label>血糖（mmol/L）*</label>
            <input required type="number" step="0.1" min="1" max="35" value={form.glucose} onChange={e => set('glucose', e.target.value)} placeholder="如 6.1" /></div>
        )}
        {type === 'MEDICATION' && (
          <>
            <div className="form-grid">
              <div className="form-item"><label>药物 *</label>
                {medications && medications.length > 0 ? (
                  <select required value={form.medicationName} onChange={e => set('medicationName', e.target.value)}>
                    <option value="">请选择</option>
                    {medications.map(m => <option key={m.id ?? m} value={m.name ?? m}>{m.name ?? m}</option>)}
                  </select>
                ) : (
                  <input required value={form.medicationName} onChange={e => set('medicationName', e.target.value)} />
                )}
              </div>
              <div className="form-item"><label>服药情况 *</label>
                <select value={form.medStatus} onChange={e => set('medStatus', e.target.value)}>
                  <option value="TAKEN">已按时服药</option>
                  <option value="MISSED">漏服</option>
                  <option value="ADVERSE">出现不良反应</option>
                </select></div>
            </div>
            <div className="form-item mt"><label>备注（不良反应描述等）</label>
              <input value={form.note} onChange={e => set('note', e.target.value)} /></div>
          </>
        )}
        {type === 'DIET' && (
          <div className="form-item"><label>饮食记录 *</label>
            <textarea required rows={2} value={form.dietNote} onChange={e => set('dietNote', e.target.value)} placeholder="如：今日低盐饮食，晚餐清蒸鱼+杂粮饭" /></div>
        )}
        {type === 'EXERCISE' && (
          <div className="form-item"><label>运动记录 *</label>
            <textarea required rows={2} value={form.exerciseNote} onChange={e => set('exerciseNote', e.target.value)} placeholder="如：晚饭后快走 40 分钟" /></div>
        )}
        {msg && <div className="mt" style={{ color: 'var(--green)', fontSize: 13 }}>{msg}</div>}
        {error && <div className="error-text mt">{error}</div>}
        <button className="btn mt" disabled={busy} type="submit">{busy ? '上传中…' : '提交上传'}</button>
      </form>
    </div>
  )
}
