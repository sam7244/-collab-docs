import { useEffect, useRef, useState } from 'react'
import { useEditor, EditorContent } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import Collaboration from '@tiptap/extension-collaboration'
import CollaborationCursor from '@tiptap/extension-collaboration-cursor'
import * as Y from 'yjs'
import { WebsocketProvider } from 'y-websocket'
import { useAuthStore } from '../hooks/useAuth'
import client from '../api/client'

const COLORS = ['#f44336','#e91e63','#9c27b0','#673ab7','#3f51b5','#2196f3','#009688','#4caf50','#ff9800']
const randomColor = () => COLORS[Math.floor(Math.random() * COLORS.length)]

// Safe base64 encoding for arbitrary binary data (avoids spread stack overflow)
const toBase64 = (bytes: Uint8Array): string => {
  let binary = ''
  for (let i = 0; i < bytes.length; i++) {
    binary += String.fromCharCode(bytes[i])
  }
  return btoa(binary)
}

interface Props {
  docId: string
  initialSnapshot?: string | null
  onPresenceChange?: (users: string[]) => void
}

export default function CollabEditor({ docId, initialSnapshot, onPresenceChange }: Props) {
  const { token, user } = useAuthStore()
  const saveTimeoutRef = useRef<number | null>(null)
  // Only save on unmount if the user actually changed content (prevents
  // React 18 StrictMode's fake-unmount from overwriting DB with empty ydoc).
  const isDirtyRef = useRef(false)
  const userColor = useRef(randomColor())

  // ydoc and provider live in state so that when StrictMode destroys and
  // recreates them, useEditor (which depends on [ydoc]) properly recreates too.
  const [ydoc, setYdoc] = useState<Y.Doc | null>(null)
  const [provider, setProvider] = useState<WebsocketProvider | null>(null)

  // Create ydoc + WebSocket provider — StrictMode-safe lifecycle
  useEffect(() => {
    const newYdoc = new Y.Doc()
    isDirtyRef.current = false
    let newProvider: WebsocketProvider | null = null

    if (token) {
      const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
      const serverUrl = `${protocol}://${window.location.host}/ws/documents`
      newProvider = new WebsocketProvider(serverUrl, docId, newYdoc, {
        connect: true,
        params: { token },
      })
      newProvider.awareness.setLocalStateField('user', {
        name: user?.displayName ?? 'Anonymous',
        color: userColor.current,
      })
    }

    setYdoc(newYdoc)
    setProvider(newProvider)

    return () => {
      newProvider?.destroy()
      newYdoc.destroy()
      setYdoc(null)
      setProvider(null)
    }
  }, [docId, token]) // eslint-disable-line react-hooks/exhaustive-deps

  // Apply persisted snapshot once both the ydoc and the snapshot are available.
  // Depends on both so it re-runs if either changes (StrictMode safe).
  useEffect(() => {
    if (!initialSnapshot || !ydoc) return
    try {
      const bytes = Uint8Array.from(atob(initialSnapshot), c => c.charCodeAt(0))
      Y.applyUpdate(ydoc, bytes)
    } catch (e) {
      console.warn('Failed to apply snapshot:', e)
    }
  }, [initialSnapshot, ydoc])

  // Wire awareness → presence bar
  useEffect(() => {
    if (!provider || !onPresenceChange) return
    const handler = () => {
      const users = Array.from(provider.awareness.getStates().values())
        .map((s: any) => s.user?.name)
        .filter(Boolean) as string[]
      onPresenceChange(users)
    }
    provider.awareness.on('change', handler)
    return () => provider.awareness.off('change', handler)
  }, [provider, onPresenceChange])

  // Flush save on unmount — only when the user has actually edited so that
  // StrictMode's fake unmount (with an empty ydoc) never overwrites the DB.
  useEffect(() => {
    if (!ydoc) return
    return () => {
      if (saveTimeoutRef.current) {
        clearTimeout(saveTimeoutRef.current)
        saveTimeoutRef.current = null
      }
      if (isDirtyRef.current) {
        const update = Y.encodeStateAsUpdate(ydoc)
        const b64 = toBase64(update)
        client.post(`/documents/${docId}/snapshot`, { snapshot: b64 })
          .catch(e => console.error('Final snapshot save failed:', e))
      }
    }
  }, [docId, ydoc])

  // Recreate the editor whenever the ydoc instance changes so the Collaboration
  // extension always references the live (non-destroyed) document.
  const editor = useEditor({
    extensions: ydoc ? [
      StarterKit.configure({ history: false }),
      Collaboration.configure({ document: ydoc }),
      ...(provider ? [CollaborationCursor.configure({
        provider,
        user: { name: user?.displayName ?? 'Anonymous', color: userColor.current },
      })] : []),
    ] : [
      StarterKit.configure({ history: false }),
    ],
    onUpdate: () => {
      isDirtyRef.current = true
      if (saveTimeoutRef.current) clearTimeout(saveTimeoutRef.current)
      saveTimeoutRef.current = window.setTimeout(() => {
        if (!ydoc) return
        const update = Y.encodeStateAsUpdate(ydoc)
        const b64 = toBase64(update)
        client.post(`/documents/${docId}/snapshot`, { snapshot: b64 })
          .catch(e => console.error('Snapshot save failed:', e))
      }, 2000)
    },
  }, [ydoc]) // re-create editor when ydoc instance changes

  return (
    <div style={{ background: '#000', border: '1px solid var(--green-dim)', overflow: 'hidden' }}>
      <EditorContent editor={editor} />
    </div>
  )
}
