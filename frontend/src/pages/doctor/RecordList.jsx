import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../api'
import { L, diseaseBadge, levelBadge, statusBadge, Empty } from '../../components/common'

export default function RecordList() {
  const [records, setRecords] = useState([])
  const [filters, setFilters] = useState({ disease: '', level: '', status: '', q: '' })
  const [error, setError] = useState('')

  const load = () => {
    const params = new URLSearchParams()
    Object.entries(filters).forEach(([k, v]) => v && params.set(k, v))
    api.get('/api/records?' + params).then(setRecords).catch(e => setError(e.message))
  }
  useEffect(load, [filters])

  return (
    <div>
      <div className="flex-between">
        <div>
          <div className="page-title">慢病档案</div>
          <div className="page-sub">高血压 / 糖尿病 / 冠心病居民分层管理</div>
        </div>
        <Link className="btn" to="/records/new">➕ 新建档案</Link>
      </div>

      <div className="card">
        <div className="flex flex-wrap mb">
          <select value={filters.disease} onChange={e => setFilters({ ...filters, disease: e.target.value })}>
            <option value="">全部病种</option>
            {Object.entries(L.disease).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select>
          <select value={filters.level} onChange={e => setFilters({ ...filters, level: e.target.value })}>
            <option value="">全部层级</option>
            {Object.entries(L.level).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select>
          <select value={filters.status} onChange={e => setFilters({ ...filters, status: e.target.value })}>
            <option value="">全部状态</option>
            {Object.entries(L.status).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select>
          <input placeholder="搜索姓名/账号" value={filters.q}
            onChange={e => setFilters({ ...filters, q: e.target.value })} style={{ width: 180 }} />
        </div>
        {error && <div className="error-text">{error}</div>}
        {records.length === 0 ? <Empty /> : (
          <table>
            <thead>
              <tr>
                <th>居民</th><th>病种</th><th>诊断</th><th>目标血压</th><th>家庭支持</th>
                <th>分层</th><th>状态</th><th>医保签约</th><th></th>
              </tr>
            </thead>
            <tbody>
              {records.map(r => (
                <tr key={r.id}>
                  <td><b>{r.resident.name}</b><div className="muted" style={{ fontSize: 11 }}>{r.resident.phone}</div></td>
                  <td>{diseaseBadge(r.diseaseType)}</td>
                  <td style={{ maxWidth: 200 }}>{r.diagnosis}</td>
                  <td>&lt;{r.targetSys}/{r.targetDia}</td>
                  <td>{L.support[r.familySupport]}</td>
                  <td>{levelBadge(r.manageLevel)}</td>
                  <td>{statusBadge(r.status)}</td>
                  <td style={{ fontSize: 12 }}>{r.insurance || '—'}</td>
                  <td><Link className="btn btn-sm btn-outline" to={`/records/${r.id}`}>详情</Link></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
