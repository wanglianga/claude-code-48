import React from 'react'
import { Routes, Route, Navigate, Link, useNavigate, useLocation } from 'react-router-dom'
import { getUser, clearSession, api } from './api'
import Login from './pages/Login'
import DoctorDashboard from './pages/doctor/DoctorDashboard'
import RecordList from './pages/doctor/RecordList'
import RecordDetail from './pages/doctor/RecordDetail'
import NewRecord from './pages/doctor/NewRecord'
import NurseAlerts from './pages/NurseAlerts'
import ResidentHome from './pages/ResidentHome'
import FamilyHome from './pages/FamilyHome'

function Layout({ children, nav }) {
  const user = getUser()
  const navigate = useNavigate()
  const location = useLocation()
  const logout = async () => {
    try { await api.post('/api/auth/logout') } catch { /* ignore */ }
    clearSession()
    navigate('/login')
  }
  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-icon">❤</div>
          <div>
            <div className="brand-title">社区慢病管理</div>
            <div className="brand-sub">随访 · 用药 · 家庭血压</div>
          </div>
        </div>
        <nav>
          {nav.map(n => (
            <Link key={n.to} to={n.to}
              className={location.pathname === n.to || (n.to !== '/' && location.pathname.startsWith(n.to)) ? 'nav-item active' : 'nav-item'}>
              <span className="nav-icon">{n.icon}</span>{n.label}
            </Link>
          ))}
        </nav>
        <div className="sidebar-footer">
          <div className="user-chip">
            <div className="avatar">{user?.name?.[0]}</div>
            <div>
              <div className="user-name">{user?.name}</div>
              <div className="user-role">{roleLabel(user?.role)}</div>
            </div>
          </div>
          <button className="btn btn-ghost btn-block" onClick={logout}>退出登录</button>
        </div>
      </aside>
      <main className="main">{children}</main>
    </div>
  )
}

export function roleLabel(role) {
  return { DOCTOR: '社区医生', NURSE: '社区护士', RESIDENT: '居民', FAMILY: '家属', ADMIN: '管理员' }[role] || role
}

function Home() {
  const user = getUser()
  if (!user) return <Navigate to="/login" />
  switch (user.role) {
    case 'DOCTOR': return <Navigate to="/dashboard" />
    case 'NURSE': return <Navigate to="/alerts" />
    case 'RESIDENT': return <Navigate to="/my" />
    case 'FAMILY': return <Navigate to="/family" />
    default: return <Navigate to="/alerts" />
  }
}

function DoctorRoutes() {
  const nav = [
    { to: '/dashboard', icon: '📊', label: '工作台' },
    { to: '/records', icon: '📁', label: '慢病档案' },
    { to: '/records/new', icon: '➕', label: '新建档案' },
    { to: '/alerts', icon: '🔔', label: '告警任务' },
  ]
  return (
    <Layout nav={nav}>
      <Routes>
        <Route path="/dashboard" element={<DoctorDashboard />} />
        <Route path="/records" element={<RecordList />} />
        <Route path="/records/new" element={<NewRecord />} />
        <Route path="/records/:id" element={<RecordDetail />} />
        <Route path="/alerts" element={<NurseAlerts />} />
        <Route path="*" element={<Navigate to="/dashboard" />} />
      </Routes>
    </Layout>
  )
}

function NurseRoutes() {
  const nav = [
    { to: '/alerts', icon: '🔔', label: '告警任务' },
    { to: '/records', icon: '📁', label: '慢病档案' },
  ]
  return (
    <Layout nav={nav}>
      <Routes>
        <Route path="/alerts" element={<NurseAlerts />} />
        <Route path="/records" element={<RecordList />} />
        <Route path="/records/:id" element={<RecordDetail />} />
        <Route path="*" element={<Navigate to="/alerts" />} />
      </Routes>
    </Layout>
  )
}

function ResidentRoutes() {
  const nav = [{ to: '/my', icon: '🏠', label: '我的健康' }]
  return (
    <Layout nav={nav}>
      <Routes>
        <Route path="/my" element={<ResidentHome />} />
        <Route path="*" element={<Navigate to="/my" />} />
      </Routes>
    </Layout>
  )
}

function FamilyRoutes() {
  const nav = [{ to: '/family', icon: '👨‍👩‍👧', label: '家人健康' }]
  return (
    <Layout nav={nav}>
      <Routes>
        <Route path="/family" element={<FamilyHome />} />
        <Route path="/records/:id" element={<RecordDetail />} />
        <Route path="*" element={<Navigate to="/family" />} />
      </Routes>
    </Layout>
  )
}

export default function App() {
  const user = getUser()
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/" element={<Home />} />
      <Route path="/*" element={
        !user ? <Navigate to="/login" /> :
        user.role === 'DOCTOR' ? <DoctorRoutes /> :
        user.role === 'NURSE' ? <NurseRoutes /> :
        user.role === 'RESIDENT' ? <ResidentRoutes /> :
        user.role === 'FAMILY' ? <FamilyRoutes /> : <NurseRoutes />
      } />
    </Routes>
  )
}
