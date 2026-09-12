import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, setSession } from '../api'

export default function Login() {
  const [username, setUsername] = useState('doctor')
  const [password, setPassword] = useState('doctor123')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const submit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      const data = await api.post('/api/auth/login', { username, password })
      setSession(data.token, data.user)
      navigate('/')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-title">社区慢病随访与家庭血压管理平台</div>
        <div className="login-sub">高血压 · 糖尿病 · 冠心病 社区一体化管理</div>
        <form onSubmit={submit}>
          <div className="form-item mb">
            <label>用户名</label>
            <input value={username} onChange={e => setUsername(e.target.value)} autoFocus />
          </div>
          <div className="form-item mb">
            <label>密码</label>
            <input type="password" value={password} onChange={e => setPassword(e.target.value)} />
          </div>
          {error && <div className="error-text mb">{error}</div>}
          <button className="btn btn-block" disabled={loading} type="submit">
            {loading ? '登录中…' : '登 录'}
          </button>
        </form>
        <div className="login-accounts">
          <div><b>演示账号</b>（密码同角色名+123）：</div>
          <div>医生 <code>doctor/doctor123</code> · 护士 <code>nurse/nurse123</code></div>
          <div>居民 <code>resident/resident123</code> · 家属 <code>family/family123</code></div>
        </div>
      </div>
    </div>
  )
}
