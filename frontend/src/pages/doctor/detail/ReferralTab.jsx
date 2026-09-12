import React, { useState } from 'react'
import { api } from '../../../api'
import { L, Modal, fmtDate, fmtTime, Empty } from '../../../components/common'

/** 转诊上级医院 + 住院记录，结果回填后写入事件流。 */
export default function ReferralTab({ recordId, referrals, hospitalizations, isStaff, onChanged }) {
  const [showRef, setShowRef] = useState(false)
  const [showHosp, setShowHosp] = useState(false)
  const [refForm, setRefForm] = useState({ toHospital: '', reason: '' })
  const [hospForm, setHospForm] = useState({ hospital: '', reason: '', startDate: '', endDate: '', note: '' })
  const [error, setError] = useState('')

  const addReferral = async (e) => {
    e.preventDefault()
    try {
      await api.post(`/api/records/${recordId}/referrals`, refForm)
      setShowRef(false); setRefForm({ toHospital: '', reason: '' }); onChanged()
    } catch (err) { setError(err.message) }
  }

  const completeReferral = async (r) => {
    const result = window.prompt('填写转诊结果（上级医院反馈）：')
    if (!result) return
    try {
      await api.put(`/api/referrals/${r.id}/complete`, { result })
      onChanged()
    } catch (err) { alert(err.message) }
  }

  const addHosp = async (e) => {
    e.preventDefault()
    try {
      await api.post(`/api/records/${recordId}/hospitalizations`, hospForm)
      setShowHosp(false); setHospForm({ hospital: '', reason: '', startDate: '', endDate: '', note: '' }); onChanged()
    } catch (err) { setError(err.message) }
  }

  return (
    <div>
      <div className="card">
        <div className="flex-between mb">
          <div className="card-title" style={{ marginBottom: 0 }}>🏥 转诊记录（{referrals.length}）</div>
          {isStaff && <button className="btn btn-sm" onClick={() => setShowRef(true)}>➕ 发起转诊</button>}
        </div>
        {referrals.length === 0 ? <Empty text="暂无转诊记录" /> : (
          <table>
            <thead><tr><th>时间</th><th>转入医院</th><th>原因</th><th>状态</th><th>结果</th>{isStaff && <th></th>}</tr></thead>
            <tbody>
              {referrals.map(r => (
                <tr key={r.id}>
                  <td>{fmtTime(r.createdAt)}</td>
                  <td><b>{r.toHospital}</b></td>
                  <td>{r.reason || '—'}</td>
                  <td><span className={`badge badge-${r.status === 'OPEN' ? 'orange' : 'green'}`}>{L.referralStatus[r.status]}</span></td>
                  <td style={{ maxWidth: 220 }}>{r.result || '—'}</td>
                  {isStaff && <td>{r.status === 'OPEN' && <button className="btn btn-sm" onClick={() => completeReferral(r)}>回填结果</button>}</td>}
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="card">
        <div className="flex-between mb">
          <div className="card-title" style={{ marginBottom: 0 }}>🛏 住院记录（{hospitalizations.length}）</div>
          {isStaff && <button className="btn btn-sm" onClick={() => setShowHosp(true)}>➕ 登记住院</button>}
        </div>
        {hospitalizations.length === 0 ? <Empty text="暂无住院记录" /> : (
          <table>
            <thead><tr><th>医院</th><th>原因</th><th>住院区间</th><th>备注</th></tr></thead>
            <tbody>
              {hospitalizations.map(h => (
                <tr key={h.id}>
                  <td><b>{h.hospital}</b></td>
                  <td>{h.reason || '—'}</td>
                  <td>{fmtDate(h.startDate)} ~ {h.endDate ? fmtDate(h.endDate) : '在院'}</td>
                  <td className="muted">{h.note || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {showRef && (
        <Modal title="发起转诊" onClose={() => setShowRef(false)}>
          <form onSubmit={addReferral}>
            <div className="form-item"><label>转入医院 *</label>
              <input required value={refForm.toHospital} onChange={e => setRefForm({ ...refForm, toHospital: e.target.value })} placeholder="如 市人民医院心内科" /></div>
            <div className="form-item mt"><label>转诊原因</label>
              <textarea rows={3} value={refForm.reason} onChange={e => setRefForm({ ...refForm, reason: e.target.value })} /></div>
            {error && <div className="error-text mt">{error}</div>}
            <button className="btn mt" type="submit">保存（写入档案事件流）</button>
          </form>
        </Modal>
      )}

      {showHosp && (
        <Modal title="登记住院" onClose={() => setShowHosp(false)}>
          <form onSubmit={addHosp}>
            <div className="form-item"><label>医院 *</label>
              <input required value={hospForm.hospital} onChange={e => setHospForm({ ...hospForm, hospital: e.target.value })} /></div>
            <div className="form-item mt"><label>原因</label>
              <input value={hospForm.reason} onChange={e => setHospForm({ ...hospForm, reason: e.target.value })} /></div>
            <div className="form-grid mt">
              <div className="form-item"><label>入院日期</label>
                <input type="date" value={hospForm.startDate} onChange={e => setHospForm({ ...hospForm, startDate: e.target.value })} /></div>
              <div className="form-item"><label>出院日期（可空）</label>
                <input type="date" value={hospForm.endDate} onChange={e => setHospForm({ ...hospForm, endDate: e.target.value })} /></div>
            </div>
            <div className="form-item mt"><label>备注</label>
              <textarea rows={2} value={hospForm.note} onChange={e => setHospForm({ ...hospForm, note: e.target.value })} /></div>
            {error && <div className="error-text mt">{error}</div>}
            <button className="btn mt" type="submit">保存（写入档案事件流）</button>
          </form>
        </Modal>
      )}
    </div>
  )
}
