import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import client from '../api/client'
import CollabEditor from '../components/CollabEditor'
import PresenceBar from '../components/PresenceBar'
import { useAuthStore } from '../hooks/useAuth'

interface Document {
  id: string
  ownerId: string
  title: string
  latestSnapshot: string | null
}

export default function Editor() {
  const { id } = useParams<{ id: string }>()
  const { user } = useAuthStore()
  const [doc, setDoc] = useState<Document | null>(null)
  const [title, setTitle] = useState('')
  const [saving, setSaving] = useState(false)
  const [presenceUsers, setPresenceUsers] = useState<string[]>([])
  const [status, setStatus] = useState<'connecting' | 'connected' | 'disconnected'>('connecting')
  const navigate = useNavigate()

  useEffect(() => {
    if (!id) return
    client.get(`/documents/${id}`).then((r) => {
      setDoc(r.data)
      setTitle(r.data.title)
      setStatus('connected')
    }).catch(() => navigate('/documents'))
  }, [id])

  const saveTitle = async () => {
    if (!id || !title.trim()) return
    setSaving(true)
    await client.patch(`/documents/${id}`, { title: title.trim() }).catch(() => {})
    setSaving(false)
  }

  const statusColor = status === 'connected' ? 'var(--green)' : status === 'connecting' ? 'var(--amber)' : 'var(--red)'
  const statusLabel = status === 'connected' ? 'ONLINE' : status === 'connecting' ? 'CONNECTING...' : 'OFFLINE'

  if (!doc) {
    return (
      <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <span className="cursor-blink" style={{ color: 'var(--green-dim)', letterSpacing: '0.2em' }}>LOADING DOCUMENT</span>
      </div>
    )
  }

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>

      {/* Header */}
      <header className="retro-header" style={{ padding: '0.6rem 1.5rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '1rem' }}>

        {/* Left: back + title */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', flex: 1, minWidth: 0 }}>
          <button
            onClick={() => navigate('/documents')}
            className="retro-btn"
            style={{ padding: '0.2rem 0.6rem', fontSize: '0.75rem', whiteSpace: 'nowrap' }}
          >
            &#x2190; DIR
          </button>

          {doc.ownerId === user?.id ? (
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              onBlur={saveTitle}
              style={{
                background: 'transparent',
                border: 'none',
                borderBottom: '1px solid var(--green-dim)',
                color: 'var(--green)',
                fontFamily: 'var(--font-mono)',
                fontSize: '0.95rem',
                outline: 'none',
                flex: 1,
                minWidth: 0,
                padding: '0.1rem 0.25rem',
              }}
            />
          ) : (
            <span style={{ color: 'var(--green)', fontSize: '0.95rem', flex: 1, minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {title}
            </span>
          )}

          {saving && (
            <span style={{ fontSize: '0.7rem', color: 'var(--amber)', letterSpacing: '0.1em', whiteSpace: 'nowrap' }}>
              SAVING...
            </span>
          )}
        </div>

        {/* Right: presence + status */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', flexShrink: 0 }}>
          <PresenceBar users={presenceUsers} />
          <span style={{ fontSize: '0.7rem', color: statusColor, letterSpacing: '0.12em', border: `1px solid ${statusColor}`, padding: '0.15rem 0.5rem' }}>
            &#x25CF; {statusLabel}
          </span>
        </div>
      </header>

      {/* Editor area */}
      <main style={{ flex: 1, maxWidth: '860px', width: '100%', margin: '0 auto', padding: '1.5rem', display: 'flex', flexDirection: 'column' }}>
        <div className="retro-panel" style={{ flex: 1 }}>
          <CollabEditor
            docId={id!}
            initialSnapshot={doc.latestSnapshot}
            onPresenceChange={setPresenceUsers}
          />
        </div>
      </main>

      {/* Footer */}
      <footer style={{ borderTop: '1px solid var(--green-dark)', padding: '0.4rem 1.5rem', display: 'flex', justifyContent: 'space-between', fontSize: '0.65rem', color: 'var(--green-dark)', letterSpacing: '0.1em' }}>
        <span>DOC ID: {id}</span>
        <span>AUTO-SAVE: ENABLED</span>
        <span className="cursor-blink">EDIT MODE</span>
      </footer>
    </div>
  )
}
