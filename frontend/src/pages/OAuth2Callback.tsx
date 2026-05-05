import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../hooks/useAuth'
import client from '../api/client'

export default function OAuth2Callback() {
  const navigate = useNavigate()
  const { login } = useAuthStore()

  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    const token = params.get('token')

    if (!token) {
      navigate('/login')
      return
    }

    client.get('/auth/me', {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then(({ data }) => {
        login(token, { id: data.id, email: data.email, displayName: data.displayName })
        navigate('/documents', { replace: true })
      })
      .catch(() => navigate('/login'))
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <div style={{ textAlign: 'center' }}>
        <div style={{ fontFamily: 'var(--font-pixel)', fontSize: '2rem', color: 'var(--green)', textShadow: '0 0 10px var(--green)', marginBottom: '1rem' }}>
          COLLABDOCS
        </div>
        <div className="cursor-blink" style={{ color: 'var(--green-dim)', letterSpacing: '0.2em', fontSize: '0.85rem' }}>
          AUTHENTICATING
        </div>
      </div>
    </div>
  )
}
