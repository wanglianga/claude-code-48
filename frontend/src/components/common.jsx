import React from 'react'

// ---------- 枚举中文映射 ----------
export const L = {
  disease: { HYPERTENSION: '高血压', DIABETES: '糖尿病', CHD: '冠心病' },
  level: { NORMAL: '常规管理', FAMILY_PROXY: '家属代管', HOME_VISIT: '上门随访', KEY_FOCUS: '重点名单' },
  status: { ACTIVE: '在管', LOST: '失访', CLOSED: '结案' },
  support: { STRONG: '强', MODERATE: '中', WEAK: '弱' },
  alertType: {
    MISSED_MED: '连续漏服', BP_HIGH: '血压异常', GLUCOSE_LOW: '低血糖',
    ADVERSE_REACTION: '药物不良反应', NO_UPLOAD: '长期未上传',
  },
  alertLevel: { INFO: '提示', WARN: '警告', CRITICAL: '紧急' },
  alertStatus: { OPEN: '待处理', ACKED: '已知晓', RESOLVED: '已办结' },
  planType: { PHONE: '电话随访', CLINIC: '门诊随访', HOME: '上门随访' },
  decision: { NONE: '常规继续', ADJUST_REMINDER: '调整提醒', REVIEW: '建议复诊', REFER: '转诊上级医院' },
  adherence: { GOOD: '良好', FAIR: '一般', POOR: '差' },
  uploadType: { BP: '血压', GLUCOSE: '血糖', MEDICATION: '用药', DIET: '饮食', EXERCISE: '运动' },
  medLogStatus: { TAKEN: '已服药', MISSED: '漏服', ADVERSE: '不良反应' },
  eventType: {
    CREATED: '建档', MED_CHANGE: '药物变更', PROXY_ASSIGN: '家属代管', HOSPITALIZATION: '住院',
    LOST: '失访', REFERRAL: '转诊', LEVEL_CHANGE: '分层调整', FOLLOW_UP: '随访', ALERT: '告警',
  },
  referralStatus: { OPEN: '转诊中', COMPLETED: '已回填结果' },
  warningStatus: { OPEN: '待电话确认', NURSE_CONFIRMED: '待医生处置', RESOLVED: '已办结' },
  doctorAction: { ADJUST_FOLLOWUP: '调整随访提醒', CLINIC: '建议门诊', CONTACT_FAMILY: '联系家属' },
}

export function levelBadge(level) {
  const color = { NORMAL: 'green', FAMILY_PROXY: 'blue', HOME_VISIT: 'orange', KEY_FOCUS: 'red' }[level] || 'gray'
  return <span className={`badge badge-${color}`}>{L.level[level] || level}</span>
}

export function statusBadge(status) {
  const color = { ACTIVE: 'green', LOST: 'red', CLOSED: 'gray' }[status] || 'gray'
  return <span className={`badge badge-${color}`}>{L.status[status] || status}</span>
}

export function alertLevelBadge(level) {
  const color = { INFO: 'blue', WARN: 'orange', CRITICAL: 'red' }[level] || 'gray'
  return <span className={`badge badge-${color}`}>{L.alertLevel[level] || level}</span>
}

export function diseaseBadge(d) {
  const color = { HYPERTENSION: 'red', DIABETES: 'orange', CHD: 'purple' }[d] || 'gray'
  return <span className={`badge badge-${color}`}>{L.disease[d] || d}</span>
}

export function fmtTime(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 16)
}

export function fmtDate(t) {
  if (!t) return '—'
  return String(t).slice(0, 10)
}

export function Modal({ title, onClose, children, width }) {
  return (
    <div className="modal-mask" onClick={onClose}>
      <div className="modal" style={width ? { width } : {}} onClick={e => e.stopPropagation()}>
        <div className="flex-between mb">
          <div className="modal-title" style={{ marginBottom: 0 }}>{title}</div>
          <button className="btn btn-outline btn-sm" onClick={onClose}>关闭</button>
        </div>
        {children}
      </div>
    </div>
  )
}

export function StatCard({ label, value, color }) {
  return (
    <div className="stat-card">
      <div className="stat-label">{label}</div>
      <div className={`stat-num ${color || ''}`}>{value}</div>
    </div>
  )
}

export function Empty({ text }) {
  return <div className="muted" style={{ padding: '18px 0', textAlign: 'center' }}>{text || '暂无数据'}</div>
}
