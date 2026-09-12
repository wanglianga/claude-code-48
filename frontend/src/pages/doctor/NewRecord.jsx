import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../../api'
import { L } from '../../components/common'

export default function NewRecord() {
  const navigate = useNavigate()
  const [form, setForm] = useState({
    residentName: '', residentUsername: '', residentPassword: 'resident123', residentPhone: '',
    diseaseType: 'HYPERTENSION', diagnosis: '', complications: '',
    targetSys: 140, targetDia: 90, glucoseMin: 3.9, glucoseMax: 7.8,
    insurance: '居民医保（签约家庭医生）', familySupport: 'MODERATE',
    planType: 'PHONE', intervalDays: 30,
  })
  const [contact, setContact] = useState({ name: '', relation: '', phone: '', proxy: false, linkedUsername: '' })
  const [med, setMed] = useState({ name: '', dosage: '', timesPerDay: 1, timeSlots: '08:00' })
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  const set = (k, v) => setForm({ ...form, [k]: v })

  const submit = async (e) => {
    e.preventDefault()
    setSaving(true)
    setError('')
    try {
      const body = {
        ...form,
        targetSys: Number(form.targetSys), targetDia: Number(form.targetDia),
        glucoseMin: Number(form.glucoseMin), glucoseMax: Number(form.glucoseMax),
        familyContacts: contact.name ? [{ ...contact, proxy: !!contact.proxy }] : [],
        medications: med.name ? [{ ...med, timesPerDay: Number(med.timesPerDay) }] : [],
        plan: { planType: form.planType, intervalDays: Number(form.intervalDays) },
      }
      const r = await api.post('/api/records', body)
      navigate(`/records/${r.id}`)
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div>
      <div className="page-title">新建慢病档案</div>
      <div className="page-sub">为居民建立档案：诊断、目标值、药物、家属联系人、医保签约与随访计划</div>
      <form onSubmit={submit}>
        <div className="card">
          <div className="card-title">👤 居民信息</div>
          <div className="form-grid">
            <div className="form-item"><label>姓名 *</label>
              <input required value={form.residentName} onChange={e => set('residentName', e.target.value)} /></div>
            <div className="form-item"><label>登录账号（留空自动生成）</label>
              <input value={form.residentUsername} onChange={e => set('residentUsername', e.target.value)} /></div>
            <div className="form-item"><label>初始密码</label>
              <input value={form.residentPassword} onChange={e => set('residentPassword', e.target.value)} /></div>
            <div className="form-item"><label>联系电话</label>
              <input value={form.residentPhone} onChange={e => set('residentPhone', e.target.value)} /></div>
          </div>
        </div>

        <div className="card">
          <div className="card-title">🩺 诊断与目标</div>
          <div className="form-grid">
            <div className="form-item"><label>病种 *</label>
              <select value={form.diseaseType} onChange={e => set('diseaseType', e.target.value)}>
                {Object.entries(L.disease).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
              </select></div>
            <div className="form-item"><label>诊断</label>
              <input value={form.diagnosis} onChange={e => set('diagnosis', e.target.value)} placeholder="如：原发性高血压 2 级" /></div>
            <div className="form-item"><label>并发症</label>
              <input value={form.complications} onChange={e => set('complications', e.target.value)} /></div>
            <div className="form-item"><label>医保签约</label>
              <input value={form.insurance} onChange={e => set('insurance', e.target.value)} /></div>
            <div className="form-item"><label>目标收缩压 (mmHg)</label>
              <input type="number" value={form.targetSys} onChange={e => set('targetSys', e.target.value)} /></div>
            <div className="form-item"><label>目标舒张压 (mmHg)</label>
              <input type="number" value={form.targetDia} onChange={e => set('targetDia', e.target.value)} /></div>
            <div className="form-item"><label>血糖下限 (mmol/L)</label>
              <input type="number" step="0.1" value={form.glucoseMin} onChange={e => set('glucoseMin', e.target.value)} /></div>
            <div className="form-item"><label>血糖上限 (mmol/L)</label>
              <input type="number" step="0.1" value={form.glucoseMax} onChange={e => set('glucoseMax', e.target.value)} /></div>
            <div className="form-item"><label>家庭支持程度</label>
              <select value={form.familySupport} onChange={e => set('familySupport', e.target.value)}>
                <option value="STRONG">强（同住且能照护）</option>
                <option value="MODERATE">中（就近但不同住）</option>
                <option value="WEAK">弱（独居/家属在外地）</option>
              </select></div>
          </div>
        </div>

        <div className="grid-2">
          <div className="card">
            <div className="card-title">👨‍👩‍👧 家属联系人（可选）</div>
            <div className="form-grid">
              <div className="form-item"><label>姓名</label>
                <input value={contact.name} onChange={e => setContact({ ...contact, name: e.target.value })} /></div>
              <div className="form-item"><label>关系</label>
                <input value={contact.relation} onChange={e => setContact({ ...contact, relation: e.target.value })} /></div>
              <div className="form-item"><label>电话</label>
                <input value={contact.phone} onChange={e => setContact({ ...contact, phone: e.target.value })} /></div>
              <div className="form-item"><label>家属平台账号（可空）</label>
                <input value={contact.linkedUsername} onChange={e => setContact({ ...contact, linkedUsername: e.target.value })} placeholder="如 family" /></div>
            </div>
            <label className="flex mt" style={{ cursor: 'pointer' }}>
              <input type="checkbox" checked={contact.proxy}
                onChange={e => setContact({ ...contact, proxy: e.target.checked })} style={{ width: 'auto' }} />
              <span>设为家属代管（代测血压、代收提醒）</span>
            </label>
          </div>

          <div className="card">
            <div className="card-title">💊 首个药物（可选）</div>
            <div className="form-grid">
              <div className="form-item"><label>药名</label>
                <input value={med.name} onChange={e => setMed({ ...med, name: e.target.value })} /></div>
              <div className="form-item"><label>剂量</label>
                <input value={med.dosage} onChange={e => setMed({ ...med, dosage: e.target.value })} placeholder="如 5mg" /></div>
              <div className="form-item"><label>每日次数</label>
                <input type="number" min="1" value={med.timesPerDay} onChange={e => setMed({ ...med, timesPerDay: e.target.value })} /></div>
              <div className="form-item"><label>服药时间（逗号分隔）</label>
                <input value={med.timeSlots} onChange={e => setMed({ ...med, timeSlots: e.target.value })} /></div>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="card-title">📅 随访计划</div>
          <div className="form-grid">
            <div className="form-item"><label>随访方式</label>
              <select value={form.planType} onChange={e => set('planType', e.target.value)}>
                {Object.entries(L.planType).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
              </select></div>
            <div className="form-item"><label>随访间隔（天）</label>
              <input type="number" min="1" value={form.intervalDays} onChange={e => set('intervalDays', e.target.value)} /></div>
          </div>
        </div>

        {error && <div className="error-text mb">{error}</div>}
        <button className="btn" disabled={saving} type="submit">{saving ? '保存中…' : '建立档案'}</button>
      </form>
    </div>
  )
}
