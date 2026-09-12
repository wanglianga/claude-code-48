import React, { useState } from 'react'
import { api } from '../../../api'
import { L, Modal, fmtDate, Empty } from '../../../components/common'

/** 随访：计划 + 历史记录 + 新增随访（医生）。 */
export default function FollowUpTab({ recordId, followUps, plans, isDoctor, onChanged }) {
  const [show, setShow] = useState(false)
  const [form, setForm] = useState({
    mode: 'PHONE', summary: '', adherence: 'GOOD', familyFeedback: '',
    recentMedical: '', decision: 'NONE', decisionDetail: '', nextIntervalDays: 30,
  })
  const [error, setError] = useState('')

  const submit = async (e) => {
    e.preventDefault()
    try {
      await api.post(`/api/records/${recordId}/follow-ups`, {
        ...form, nextIntervalDays: Number(form.nextIntervalDays),
      })
      setShow(false)
      onChanged()
    } catch (err) { setError(err.message) }
  }

  return (
    <div>
      <div className="card">
        <div className="flex-between mb">
          <div className="card-title" style={{ marginBottom: 0 }}>📅 随访计划</div>
          {isDoctor && <button className="btn btn-sm" onClick={() => setShow(true)}>✍️ 录入随访</button>}
        </div>
        {plans.length === 0 ? <Empty text="暂无随访计划" /> : (
          <table>
            <thead><tr><th>方式</th><th>间隔</th><th>下次应访日期</th><th>状态</th></tr></thead>
            <tbody>
              {plans.map(p => {
                const overdue = p.active && p.nextDueDate && p.nextDueDate < new Date().toISOString().slice(0, 10)
                return (
                  <tr key={p.id}>
                    <td>{L.planType[p.planType]}</td>
                    <td>每 {p.intervalDays} 天</td>
                    <td>{fmtDate(p.nextDueDate)}</td>
                    <td>{!p.active ? <span className="badge badge-gray">停用</span>
                      : overdue ? <span className="badge badge-red">已逾期</span>
                      : <span className="badge badge-green">进行中</span>}</td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        )}
      </div>

      <div className="card">
        <div className="card-title">🩺 随访记录（{followUps.length}）</div>
        {followUps.length === 0 ? <Empty text="暂无随访记录" /> : followUps.map(f => (
          <div key={f.id} style={{ borderBottom: '1px solid var(--border)', padding: '12px 0' }}>
            <div className="flex flex-wrap">
              <b>{fmtDate(f.visitDate)}</b>
              <span className="badge badge-blue">{L.planType[f.mode]}</span>
              <span className="muted">随访医生：{f.doctor.name}</span>
              <span className={`badge badge-${f.decision === 'REFER' ? 'red' : f.decision === 'REVIEW' ? 'orange' : 'green'}`}>
                {L.decision[f.decision]}
              </span>
              {f.adherence && <span className="badge badge-gray">依从性：{L.adherence[f.adherence]}</span>}
            </div>
            <div className="mt">{f.summary}</div>
            {f.familyFeedback && <div className="muted mt" style={{ fontSize: 12 }}>👪 家属反馈：{f.familyFeedback}</div>}
            {f.recentMedical && <div className="muted" style={{ fontSize: 12 }}>🏥 近期就医：{f.recentMedical}</div>}
            {f.decisionDetail && <div className="muted" style={{ fontSize: 12 }}>📌 处置说明：{f.decisionDetail}</div>}
          </div>
        ))}
      </div>

      {show && (
        <Modal title="录入随访" onClose={() => setShow(false)} width={640}>
          <form onSubmit={submit}>
            <div className="form-grid">
              <div className="form-item"><label>随访方式</label>
                <select value={form.mode} onChange={e => setForm({ ...form, mode: e.target.value })}>
                  {Object.entries(L.planType).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                </select></div>
              <div className="form-item"><label>用药依从性评估</label>
                <select value={form.adherence} onChange={e => setForm({ ...form, adherence: e.target.value })}>
                  {Object.entries(L.adherence).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                </select></div>
            </div>
            <div className="form-item mt"><label>随访摘要 *（症状、血压/血糖控制情况）</label>
              <textarea required rows={3} value={form.summary} onChange={e => setForm({ ...form, summary: e.target.value })} /></div>
            <div className="form-item mt"><label>家属反馈</label>
              <textarea rows={2} value={form.familyFeedback} onChange={e => setForm({ ...form, familyFeedback: e.target.value })} /></div>
            <div className="form-item mt"><label>近期就医情况</label>
              <input value={form.recentMedical} onChange={e => setForm({ ...form, recentMedical: e.target.value })} /></div>
            <div className="form-grid mt">
              <div className="form-item"><label>处置决定</label>
                <select value={form.decision} onChange={e => setForm({ ...form, decision: e.target.value })}>
                  {Object.entries(L.decision).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                </select></div>
              <div className="form-item"><label>下次随访间隔（天）</label>
                <input type="number" min="1" value={form.nextIntervalDays}
                  onChange={e => setForm({ ...form, nextIntervalDays: e.target.value })} /></div>
            </div>
            <div className="form-item mt"><label>处置说明（调整提醒内容 / 复诊建议 / 转诊原因）</label>
              <textarea rows={2} value={form.decisionDetail} onChange={e => setForm({ ...form, decisionDetail: e.target.value })} /></div>
            {error && <div className="error-text mt">{error}</div>}
            <button className="btn mt" type="submit">保存随访（自动更新随访计划并写入事件流）</button>
          </form>
        </Modal>
      )}
    </div>
  )
}
