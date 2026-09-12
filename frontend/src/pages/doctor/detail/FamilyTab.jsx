import React, { useState } from 'react'
import { api } from '../../../api'
import { Modal, Empty } from '../../../components/common'

/** 家属联系人与代管设置。 */
export default function FamilyTab({ recordId, contacts, isStaff, onChanged }) {
  const [show, setShow] = useState(false)
  const [form, setForm] = useState({ name: '', relation: '', phone: '', proxy: false, linkedUsername: '' })
  const [error, setError] = useState('')

  const add = async (e) => {
    e.preventDefault()
    try {
      await api.post(`/api/records/${recordId}/family-contacts`, form)
      setShow(false)
      setForm({ name: '', relation: '', phone: '', proxy: false, linkedUsername: '' })
      onChanged()
    } catch (err) { setError(err.message) }
  }

  const toggleProxy = async (c) => {
    try {
      await api.put(`/api/family-contacts/${c.id}/proxy`, { proxy: !c.proxy })
      onChanged()
    } catch (err) { alert(err.message) }
  }

  return (
    <div className="card">
      <div className="flex-between mb">
        <div className="card-title" style={{ marginBottom: 0 }}>👨‍👩‍👧 家属联系人（{contacts.length}）</div>
        {isStaff && <button className="btn btn-sm" onClick={() => setShow(true)}>➕ 添加联系人</button>}
      </div>
      {contacts.length === 0 ? <Empty text="暂无家属联系人" /> : (
        <table>
          <thead><tr><th>姓名</th><th>关系</th><th>电话</th><th>平台账号</th><th>代管</th>{isStaff && <th>操作</th>}</tr></thead>
          <tbody>
            {contacts.map(c => (
              <tr key={c.id}>
                <td><b>{c.name}</b></td>
                <td>{c.relation || '—'}</td>
                <td>{c.phone || '—'}</td>
                <td>{c.linkedUser ? c.linkedUser.username : <span className="muted">未关联</span>}</td>
                <td>{c.proxy ? <span className="badge badge-purple">代管中</span> : <span className="badge badge-gray">否</span>}</td>
                {isStaff && (
                  <td>
                    <button className="btn btn-sm btn-outline" onClick={() => toggleProxy(c)}>
                      {c.proxy ? '取消代管' : '设为代管'}
                    </button>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      )}
      <div className="muted mt" style={{ fontSize: 12 }}>
        代管家属可代测血压/血糖、代报用药与不良反应，平台会区分「居民自测」与「家属代测」并展示差异。
      </div>

      {show && (
        <Modal title="添加家属联系人" onClose={() => setShow(false)}>
          <form onSubmit={add}>
            <div className="form-grid">
              <div className="form-item"><label>姓名 *</label>
                <input required value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} /></div>
              <div className="form-item"><label>关系</label>
                <input value={form.relation} onChange={e => setForm({ ...form, relation: e.target.value })} placeholder="如 儿子/配偶" /></div>
              <div className="form-item"><label>电话</label>
                <input value={form.phone} onChange={e => setForm({ ...form, phone: e.target.value })} /></div>
              <div className="form-item"><label>关联平台账号（家属用户名，可空）</label>
                <input value={form.linkedUsername} onChange={e => setForm({ ...form, linkedUsername: e.target.value })} placeholder="如 family" /></div>
            </div>
            <label className="flex mt" style={{ cursor: 'pointer' }}>
              <input type="checkbox" checked={form.proxy} style={{ width: 'auto' }}
                onChange={e => setForm({ ...form, proxy: e.target.checked })} />
              <span>设为代管家属（代测、代收提醒）</span>
            </label>
            {error && <div className="error-text mt">{error}</div>}
            <button className="btn mt" type="submit">保存</button>
          </form>
        </Modal>
      )}
    </div>
  )
}
