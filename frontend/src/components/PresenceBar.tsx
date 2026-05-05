const COLORS = ['#00ff41', '#ffb000', '#00cfff', '#ff6ec7', '#b967ff']

interface Props {
  users: string[]
}

export default function PresenceBar({ users }: Props) {
  if (users.length === 0) return null
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
      {users.map((name, i) => (
        <div
          key={i}
          title={name}
          style={{
            width: '26px',
            height: '26px',
            border: `1px solid ${COLORS[i % COLORS.length]}`,
            color: COLORS[i % COLORS.length],
            fontSize: '0.65rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontFamily: 'var(--font-mono)',
            letterSpacing: '0.05em',
            textTransform: 'uppercase',
            boxShadow: `0 0 6px ${COLORS[i % COLORS.length]}55`,
          }}
        >
          {name.slice(0, 2)}
        </div>
      ))}
      <span style={{ fontSize: '0.65rem', color: 'var(--green-dim)', letterSpacing: '0.08em', marginLeft: '0.2rem' }}>
        {users.length === 1 ? '1 USER' : `${users.length} USERS`}
      </span>
    </div>
  )
}
