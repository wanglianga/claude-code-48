import React, { useEffect, useState } from 'react'
import { api } from '../../../api'
import { L, fmtTime, Empty } from '../../../components/common'
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ReferenceLine, ResponsiveContainer,
} from 'recharts'

/** 趋势与依从性：血压/血糖曲线（区分居民自测 vs 家属代测）、用药依从性、生活方式记录。 */
export default function TrendTab({ recordId, record }) {
  const [days, setDays] = useState(30)
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api.get(`/api/records/${recordId}/trends?days=${days}`).then(setData).catch(e => setError(e.message))
  }, [recordId, days])

  if (error) return <div className="error-text">{error}</div>
  if (!data) return <div className="muted">加载中…</div>

  // 血压图数据：自测与代测分开两条线，便于医生对比差异
  const bpMap = {}
  data.bp.forEach(u => {
    const key = u.measuredAt.slice(0, 16).replace('T', ' ')
    if (!bpMap[key]) bpMap[key] = { time: key }
    if (u.uploaderType === 'SELF') {
      bpMap[key].selfSys = u.sys; bpMap[key].selfDia = u.dia
    } else {
      bpMap[key].famSys = u.sys; bpMap[key].famDia = u.dia
    }
  })
  const bpData = Object.values(bpMap)

  const gluData = data.glucose.map(u => ({
    time: u.measuredAt.slice(0, 16).replace('T', ' '),
    glucose: u.glucose,
    source: u.uploaderType === 'SELF' ? '自测' : '家属代测',
  }))

  const diff = data.selfVsFamily

  return (
    <div>
      <div className="flex-between mb">
        <div className="muted">家庭监测趋势（含用药依从性与家属代管差异分析）</div>
        <select value={days} onChange={e => setDays(Number(e.target.value))} style={{ width: 130 }}>
          <option value={7}>近 7 天</option>
          <option value={30}>近 30 天</option>
          <option value={90}>近 90 天</option>
        </select>
      </div>

      <div className="grid-2">
        <div className="card">
          <div className="card-title">🩸 家庭血压趋势（目标 &lt;{record.targetSys}/{record.targetDia}）</div>
          {bpData.length === 0 ? <Empty text="暂无血压记录" /> : (
            <ResponsiveContainer width="100%" height={280}>
              <LineChart data={bpData} margin={{ top: 5, right: 10, bottom: 5, left: -18 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#eef1f4" />
                <XAxis dataKey="time" tick={{ fontSize: 10 }} interval="preserveStartEnd" />
                <YAxis domain={[40, 200]} tick={{ fontSize: 10 }} />
                <Tooltip />
                <Legend wrapperStyle={{ fontSize: 11 }} />
                <ReferenceLine y={record.targetSys} stroke="#d64545" strokeDasharray="4 4" label={{ value: '目标收缩压', fontSize: 10, fill: '#d64545' }} />
                <ReferenceLine y={record.targetDia} stroke="#e8871a" strokeDasharray="4 4" />
                <Line type="monotone" dataKey="selfSys" name="自测收缩压" stroke="#1677c8" strokeWidth={2} dot={{ r: 2 }} connectNulls />
                <Line type="monotone" dataKey="selfDia" name="自测舒张压" stroke="#7ab8e8" strokeWidth={2} dot={{ r: 2 }} connectNulls />
                <Line type="monotone" dataKey="famSys" name="家属代测收缩压" stroke="#7b5ea7" strokeWidth={2} dot={{ r: 3 }} strokeDasharray="6 3" connectNulls />
                <Line type="monotone" dataKey="famDia" name="家属代测舒张压" stroke="#b39ddb" strokeWidth={2} dot={{ r: 3 }} strokeDasharray="6 3" connectNulls />
              </LineChart>
            </ResponsiveContainer>
          )}
        </div>

        <div className="card">
          <div className="card-title">🍬 家庭血糖趋势（目标 {record.glucoseMin}~{record.glucoseMax} mmol/L）</div>
          {gluData.length === 0 ? <Empty text="暂无血糖记录" /> : (
            <ResponsiveContainer width="100%" height={280}>
              <LineChart data={gluData} margin={{ top: 5, right: 10, bottom: 5, left: -18 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#eef1f4" />
                <XAxis dataKey="time" tick={{ fontSize: 10 }} interval="preserveStartEnd" />
                <YAxis domain={[0, 15]} tick={{ fontSize: 10 }} />
                <Tooltip />
                <Legend wrapperStyle={{ fontSize: 11 }} />
                <ReferenceLine y={record.glucoseMax} stroke="#d64545" strokeDasharray="4 4" label={{ value: '上限', fontSize: 10, fill: '#d64545' }} />
                <ReferenceLine y={record.glucoseMin} stroke="#e8871a" strokeDasharray="4 4" label={{ value: '低血糖线', fontSize: 10, fill: '#e8871a' }} />
                <Line type="monotone" dataKey="glucose" name="血糖 (mmol/L)" stroke="#e8871a" strokeWidth={2} dot={{ r: 3 }} connectNulls />
              </LineChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>

      <div className="grid-2">
        <div className="card">
          <div className="card-title">👨‍👩‍👧 家属代管 vs 居民自测 差异（每次测量来源均已标注）</div>
          <div className="diff-box">
            <div className="diff-col self">
              <div className="muted">居民自测（{diff.selfCount} 次）</div>
              <div className="diff-num">{diff.selfAvgSys ?? '—'}/{diff.selfAvgDia ?? '—'}</div>
              <div className="muted" style={{ fontSize: 11 }}>平均血压 mmHg</div>
            </div>
            <div className="diff-col family">
              <div className="muted">家属代测（{diff.familyCount} 次）</div>
              <div className="diff-num">{diff.familyAvgSys ?? '—'}/{diff.familyAvgDia ?? '—'}</div>
              <div className="muted" style={{ fontSize: 11 }}>平均血压 mmHg</div>
            </div>
          </div>
          {diff.selfAvgSys != null && diff.familyAvgSys != null && (
            <div className="mt muted" style={{ fontSize: 12 }}>
              收缩压均值差 {Math.abs(diff.selfAvgSys - diff.familyAvgSys)} mmHg
              {Math.abs(diff.selfAvgSys - diff.familyAvgSys) >= 10
                ? '，差异较大，需关注测量方式或照护质量'
                : '，差异在合理范围'}
            </div>
          )}
        </div>

        <div className="card">
          <div className="card-title">💊 用药依从性（近 {days} 天）</div>
          {data.adherence == null ? <Empty text="暂无用药打卡记录" /> : (
            <>
              <div className="flex" style={{ gap: 20 }}>
                <div>
                  <div className="stat-num" style={{ color: data.adherence >= 80 ? 'var(--green)' : data.adherence >= 60 ? 'var(--orange)' : 'var(--red)' }}>
                    {data.adherence}%
                  </div>
                  <div className="stat-label">按时服药率</div>
                </div>
                <div>
                  <div className="stat-num green">{data.takenCount}</div>
                  <div className="stat-label">已服次数</div>
                </div>
                <div>
                  <div className="stat-num red">{data.missedCount}</div>
                  <div className="stat-label">漏服次数</div>
                </div>
              </div>
              <div className="mt muted" style={{ fontSize: 12 }}>
                {data.adherence >= 80 ? '依从性良好，继续保持。'
                  : data.adherence >= 60 ? '依从性一般，建议加强用药提醒。'
                  : '依从性差，建议家属代管或上门随访。'}
              </div>
            </>
          )}
        </div>
      </div>

      <div className="grid-2">
        <div className="card">
          <div className="card-title">📋 最近用药打卡</div>
          {data.medLogs.length === 0 ? <Empty text="暂无用药记录" /> : (
            <table>
              <thead><tr><th>时间</th><th>药物</th><th>状态</th><th>来源</th><th>备注</th></tr></thead>
              <tbody>
                {data.medLogs.slice(0, 10).map(u => (
                  <tr key={u.id}>
                    <td>{fmtTime(u.measuredAt)}</td>
                    <td>{u.medicationName}</td>
                    <td>
                      <span className={`badge badge-${u.medStatus === 'TAKEN' ? 'green' : u.medStatus === 'MISSED' ? 'orange' : 'red'}`}>
                        {L.medLogStatus[u.medStatus]}
                      </span>
                    </td>
                    <td>{u.uploaderType === 'SELF' ? '本人' : '家属'}</td>
                    <td className="muted">{u.note || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <div className="card">
          <div className="card-title">🥗 饮食与运动记录</div>
          {data.lifestyle.length === 0 ? <Empty text="暂无饮食/运动记录" /> : (
            <table>
              <thead><tr><th>时间</th><th>类型</th><th>内容</th><th>来源</th></tr></thead>
              <tbody>
                {data.lifestyle.slice(0, 10).map(u => (
                  <tr key={u.id}>
                    <td>{fmtTime(u.measuredAt)}</td>
                    <td>{L.uploadType[u.type]}</td>
                    <td>{u.dietNote || u.exerciseNote || '—'}</td>
                    <td>{u.uploaderType === 'SELF' ? '本人' : '家属'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  )
}
