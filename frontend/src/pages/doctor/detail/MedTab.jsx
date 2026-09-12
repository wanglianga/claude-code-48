import React, { useState } from 'react'
import { api } from '../../../api'
import { Modal, fmtDate, Empty } from '../../../components/common'

/** 用药管理：当前方案 + 历史变更（停药保留记录）。 */
export default function MedTab({ recordId, medications, isDoctor, onChanged }) {
  const [showAdd, setShowAdd] = useState(false)
  const [form, setForm] = useState({ name: '', dosage: '', timesPerDay: 1, timeSlots: '08:00', note: '' })
  const [error, setError] = useState('')

  const active = medications.filter(m => m.status === 'ACTIVE')
  const stopped = medications.filter(m => m.status === 'STOPPED')

  const add = async (e) => {
    e.preventDefault()
    try {
      await api.post(`/api/records/${recordId}/medications`, { ...form, timesPerDay: Number(form.timesPerDay) })
      setShowAdd(false)
      setForm({ name: '', dosage: '', timesPerDay: 1, timeSlots: '08:00', note: '' })
      onChanged()
    } catch (err) { setError(err.message) }
  }

  const stop = async (m) => {
    const reason = window.prompt(`停用「${m.name}」的原因（可选）：`)
    if (reason === null) return
    try {
      await api.put(`/api/medications/${m.id}/stop`, { reason })
      onChanged()
    } catch (err) { alert(err.message) }
  }

  return (
    <div>
      <div className="card">
        <div className="flex-between mb">
          <div className="card-title" style={{ marginBottom: 0 }}>💊 当前用药方案（{active.length}）</div>
          {isDoctor && <button className="btn btn-sm" onClick={() => setShowAdd(true)}>➕ 新增药物</button>}
        </div>
        {active.length === 0 ? <Empty text="暂无在用药物" /> : (
          <table>
            <thead><tr><th>药物</th><th>剂量</th><th>频次</th><th>服药时间</th><th>开始日期</th><th>备注</th>{isDoctor && <th></th>}</tr></thead>
            <tbody>
              {active.map(m => (
                <tr key={m.id}>
                  <td><b>{m.name}</b></td>
                  <td>{m.dosage}</td>
                  <td>每日 {m.timesPerDay} 次</td>
                  <td>{m.timeSlots}</td>
                  <td>{fmtDate(m.startDate)}</td>
                  <td className="muted">{m.note || '—'}</td>
                  {isDoctor && <td><button className="btn btn-sm btn-danger" onClick={() => stop(m)}>停药</button></td>}
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="card">
        <div className="card-title">📜 药物变更历史（已停用 {stopped.length}）</div>
        {stopped.length === 0 ? <Empty text="暂无变更记录" /> : (
          <table>
            <thead><tr><th>药物</th><th>剂量</th><th>用药区间</th><th>备注</th></tr></thead>
            <tbody>
              {stopped.map(m => (
                <tr key={m.id}>
                  <td>{m.name}</td>
                  <td>{m.dosage}</td>
                  <td>{fmtDate(m.startDate)} ~ {fmtDate(m.endDate)}</td>
                  <td className="muted">{m.note || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {showAdd && (
        <Modal title="新增药物" onClose={() => setShowAdd(false)}>
          <form onSubmit={add}>
            <div className="form-grid">
              <div className="form-item"><label>药物名称 *</label>
                <input required value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} /></div>
              <div className="form-item"><label>剂量</label>
                <input value={form.dosage} onChange={e => setForm({ ...form, dosage: e.target.value })} placeholder="如 5mg" /></div>
              <div className="form-item"><label>每日次数</label>
                <input type="number" min="1" value={form.timesPerDay} onChange={e => setForm({ ...form, timesPerDay: e.target.value })} /></div>
              <div className="form-item"><label>服药时间（逗号分隔）</label>
                <input value={form.timeSlots} onChange={e => setForm({ ...form, timeSlots: e.target.value })} placeholder="08:00,18:00" /></div>
            </div>
            <div className="form-item mt"><label>备注</label>
              <textarea rows={2} value={form.note} onChange={e => setForm({ ...form, note: e.target.value })} /></div>
            {error && <div className="error-text mt">{error}</div>}
            <button className="btn mt" type="submit">保存（变更将写入档案事件流）</button>
          </form>
        </Modal>
      )}
    </div>
  )
}
