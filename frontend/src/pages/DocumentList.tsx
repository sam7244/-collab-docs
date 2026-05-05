import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import client from '../api/client'
import { useAuthStore } from '../hooks/useAuth'

interface Document {
  id: string
  ownerId: string
  title: string
  updatedAt: string
}

const PAGE_SIZE = 6

export default function DocumentList() {
  const [docs, setDocs] = useState<Document[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [showModal, setShowModal] = useState(false)
  const [newTitle, setNewTitle] = useState('')
  const [creating, setCreating] = useState(false)
  const [titleError, setTitleError] = useState(false)
  const modalInputRef = useRef<HTMLInputElement>(null)
  const { user, logout } = useAuthStore()
  const navigate = useNavigate()

  useEffect(() => {
    client.get('/documents').then((r) => {
      setDocs(r.data)
      setLoading(false)
    })
  }, [])

  useEffect(() => { setPage(0) }, [search])

  useEffect(() => {
    if (showModal) {
      setTimeout(() => modalInputRef.current?.focus(), 50)
    } else {
      setNewTitle('')
      setTitleError(false)
    }
  }, [showModal])

  const createDoc = async () => {
    if (!newTitle.trim()) { setTitleError(true); return }
    setTitleError(false)
    setCreating(true)
    try {
      const { data } = await client.post('/documents', { title: newTitle.trim() })
      navigate(`/documents/${data.id}`)
    } finally {
      setCreating(false)
    }
  }

  const filtered = docs.filter((d) =>
    d.title.toLowerCase().includes(search.toLowerCase())
  )
  const totalPages = Math.ceil(filtered.length / PAGE_SIZE)
  const pageDocs = filtered.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE)

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>

      {/* Header */}
      <header className="retro-header" style={{ padding: '0.75rem 1.5rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <span style={{ fontFamily: 'var(--font-pixel)', fontSize: '1.5rem', color: 'var(--green)', textShadow: '0 0 8px var(--green)' }}>
            COLLABDOCS
          </span>
          <span style={{ fontSize: '0.7rem', color: 'var(--green-dim)', letterSpacing: '0.1em' }}>FILE SYSTEM</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1.25rem' }}>
          <span style={{ fontSize: '0.8rem', color: 'var(--green-dim)' }}>
            USER: <span style={{ color: 'var(--green)' }}>{user?.displayName?.toUpperCase()}</span>
          </span>
          <button onClick={logout} className="retro-btn retro-btn-danger" style={{ padding: '0.25rem 0.75rem', fontSize: '0.75rem' }}>
            LOGOUT
          </button>
        </div>
      </header>

      <main style={{ maxWidth: '760px', width: '100%', margin: '0 auto', padding: '2rem 1.5rem', flex: 1 }}>

        {/* Single search + new doc bar */}
        <div style={{ display: 'flex', gap: '0.6rem', marginBottom: '1.75rem' }}>
          <input
            type="text"
            placeholder="Search documents..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="retro-input"
            style={{ flex: 1 }}
          />
          <button
            onClick={() => setShowModal(true)}
            className="retro-btn"
            title="New document"
            style={{ fontSize: '1.25rem', padding: '0 1rem', lineHeight: 1 }}
          >
            +
          </button>
        </div>

        {/* Divider */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.25rem' }}>
          <span style={{ fontSize: '0.7rem', color: 'var(--green-dim)', letterSpacing: '0.15em', whiteSpace: 'nowrap' }}>
            DIRECTORY LISTING
          </span>
          <div style={{ flex: 1, borderTop: '1px dashed var(--green-dark)' }} />
          <span style={{ fontSize: '0.7rem', color: 'var(--green-dim)' }}>
            {search ? `${filtered.length} MATCH(ES)` : `${docs.length} FILE(S)`}
          </span>
        </div>

        {/* Doc list */}
        {loading ? (
          <div style={{ color: 'var(--green-dim)', fontSize: '0.85rem', padding: '1rem 0' }}>
            <span className="cursor-blink">LOADING</span>
          </div>
        ) : filtered.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '3rem 0', color: 'var(--green-dark)', fontSize: '0.85rem', letterSpacing: '0.1em' }}>
            <div style={{ fontSize: '2rem', marginBottom: '0.5rem', fontFamily: 'var(--font-pixel)' }}>[ EMPTY ]</div>
            {search ? 'NO MATCHING FILES FOUND.' : 'NO FILES FOUND. PRESS + TO CREATE ONE.'}
          </div>
        ) : (
          <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            {pageDocs.map((doc, i) => (
              <li key={doc.id}>
                <button className="retro-doc-item" onClick={() => navigate(`/documents/${doc.id}`)}>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                      <span style={{ color: 'var(--green-dim)', fontSize: '0.75rem' }}>
                        {String(page * PAGE_SIZE + i + 1).padStart(2, '0')}.
                      </span>
                      <span style={{ color: 'var(--green)', fontSize: '0.9rem' }}>{doc.title}</span>
                    </div>
                    <span className={`retro-tag ${doc.ownerId === user?.id ? 'retro-tag-green' : 'retro-tag-dim'}`}>
                      {doc.ownerId === user?.id ? 'OWNED' : 'SHARED'}
                    </span>
                  </div>
                  <div style={{ fontSize: '0.7rem', color: 'var(--green-dark)', marginTop: '0.3rem', paddingLeft: '1.5rem' }}>
                    MODIFIED: {new Date(doc.updatedAt).toLocaleString().toUpperCase()}
                  </div>
                </button>
              </li>
            ))}
          </ul>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: '1.5rem', borderTop: '1px dashed var(--green-dark)', paddingTop: '1rem' }}>
            <button
              className="retro-btn"
              onClick={() => setPage((p) => p - 1)}
              disabled={page === 0}
              style={{ fontSize: '0.75rem', padding: '0.3rem 0.9rem', opacity: page === 0 ? 0.35 : 1 }}
            >
              &#x25C4; PREV
            </button>
            <span style={{ fontSize: '0.7rem', color: 'var(--green-dim)', letterSpacing: '0.15em' }}>
              PAGE {page + 1} / {totalPages}
            </span>
            <button
              className="retro-btn"
              onClick={() => setPage((p) => p + 1)}
              disabled={page >= totalPages - 1}
              style={{ fontSize: '0.75rem', padding: '0.3rem 0.9rem', opacity: page >= totalPages - 1 ? 0.35 : 1 }}
            >
              NEXT &#x25BA;
            </button>
          </div>
        )}
      </main>

      {/* Footer */}
      <footer style={{ borderTop: '1px solid var(--green-dark)', padding: '0.5rem 1.5rem', display: 'flex', justifyContent: 'space-between', fontSize: '0.65rem', color: 'var(--green-dark)', letterSpacing: '0.1em' }}>
        <span>COLLABDOCS SYSTEM v1.0</span>
        <span className="cursor-blink">READY</span>
      </footer>

      {/* New document modal */}
      {showModal && (
        <div
          onClick={() => setShowModal(false)}
          style={{
            position: 'fixed', inset: 0,
            background: 'rgba(0,0,0,0.75)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            zIndex: 100,
          }}
        >
          <div
            onClick={(e) => e.stopPropagation()}
            style={{
              background: '#0d1a0f',
              border: '1px solid var(--green)',
              boxShadow: '0 0 24px rgba(0,255,65,0.2)',
              padding: '2rem',
              width: '100%',
              maxWidth: '420px',
            }}
          >
            <div style={{ fontSize: '0.7rem', color: 'var(--green-dim)', letterSpacing: '0.2em', marginBottom: '1.25rem' }}>
              &#x25B6; CREATE NEW FILE
            </div>
            <input
              ref={modalInputRef}
              type="text"
              placeholder="Enter file name..."
              value={newTitle}
              onChange={(e) => { setNewTitle(e.target.value); setTitleError(false) }}
              onKeyDown={(e) => { if (e.key === 'Enter') createDoc(); if (e.key === 'Escape') setShowModal(false) }}
              className={`retro-input${titleError ? ' error' : ''}`}
              style={{ width: '100%', marginBottom: titleError ? '0.25rem' : '1rem' }}
            />
            {titleError && (
              <p style={{ color: 'var(--red)', fontSize: '0.7rem', marginBottom: '1rem' }}>
                &#x25B6; ERROR: filename required
              </p>
            )}
            <div style={{ display: 'flex', gap: '0.6rem', justifyContent: 'flex-end' }}>
              <button onClick={() => setShowModal(false)} className="retro-btn retro-btn-danger" style={{ fontSize: '0.75rem', padding: '0.3rem 0.9rem' }}>
                CANCEL
              </button>
              <button onClick={createDoc} disabled={creating} className="retro-btn" style={{ fontSize: '0.75rem', padding: '0.3rem 0.9rem' }}>
                {creating ? '...' : 'CREATE'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
