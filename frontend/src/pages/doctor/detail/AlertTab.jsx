import React from 'react'
import { api } from '../../../api'
import { L, alertLevelBadge, fmtTime, Empty } from '../../../components/common'

/** 告警列表（档案维度），医护可处理。 */
export default function AlertTab({ alerts, isStaff, onChanged }) {
  const handle = async (a, action) => {
    const note = window.prompt(action === 'ack' ? '知晓备注（可选）：' : '办结说明（可选）：')
    if (note === null) return
    try {
      await api.post(`/api/alerts/${a.id}/${action}`, { note })
      onChanged()
    } catch (err) { alert(err.message) }
  }

  if (alerts.length === 0) return <div className="card"><Empty text="暂无告警" /></div>

  return (
    <div className="card">
      <table>
        <thead><tr><th>时间</th><th>级别</th><th>类型</th><th>内容</th><th>状态</th><th>处理人</th>{isStaff && <th>操作</th>}</tr></thead>
        <tbody>
          {alerts.map(a => (
            <tr key={a.id}>
              <td style={{ whiteSpace: 'nowrap' }}>{fmtTime(a.createdAt)}</td>
              <td>{alertLevelBadge(a.level)}</td>
              <td><b>{L.alertType[a.alertType]}</b></td>
              <td>{a.message}</td>
              <td>
                <span className={`badge badge-${a.status === 'OPEN' ? 'red' : a.status === 'ACKED' ? 'orange' : 'green'}`}>
                  {L.alertStatus[a.status]}
                </span>
              </td>
              <td className="muted">{a.handledBy ? `${a.handledBy.name} ${fmtTime(a.handledAt)}` : '—'}</td>
              {isStaff && (
                <td style={{ whiteSpace: 'nowrap' }}>
                  {a.status === 'OPEN' && <button className="btn btn-sm btn-warn" onClick={() => handle(a, 'ack')}>知晓</button>}
                  {a.status !== 'RESOLVED' && <button className="btn btn-sm" style={{ marginLeft: 6 }} onClick={() => handle(a, 'resolve')}>办结</button>}
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
