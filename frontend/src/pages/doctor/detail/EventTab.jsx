import React from 'react'
import { L, fmtTime, Empty } from '../../../components/common'

/** 事件流：药物变更、家属代管、住院、失访、转诊结果等全部回流到同一档案。 */
export default function EventTab({ events }) {
  if (events.length === 0) return <div className="card"><Empty text="暂无事件" /></div>
  const color = {
    CREATED: 'var(--green)', MED_CHANGE: 'var(--primary)', PROXY_ASSIGN: 'var(--purple)',
    HOSPITALIZATION: 'var(--orange)', LOST: 'var(--red)', REFERRAL: 'var(--orange)',
    LEVEL_CHANGE: 'var(--primary)', FOLLOW_UP: 'var(--green)', ALERT: 'var(--red)',
  }
  return (
    <div className="card">
      <div className="card-title">🕘 档案事件流（所有关键变化统一回流）</div>
      <div className="timeline">
        {events.map(e => (
          <div key={e.id} className="timeline-item">
            <div className="flex">
              <span className="badge" style={{ background: color[e.eventType] || 'var(--primary)', color: '#fff' }}>
                {L.eventType[e.eventType] || e.eventType}
              </span>
              <span className="timeline-time">{fmtTime(e.createdAt)} · {e.createdBy}</span>
            </div>
            <div className="timeline-detail">{e.detail}</div>
          </div>
        ))}
      </div>
    </div>
  )
}
