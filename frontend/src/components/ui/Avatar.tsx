function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean)
  if (parts.length === 0) {
    return '?'
  }
  if (parts.length === 1) {
    return parts[0].slice(0, 2).toUpperCase()
  }
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
}

/* Deterministic warm hue so the same provider always keeps the same tile. */
function hueFor(seed: string): number {
  let hash = 0
  for (let index = 0; index < seed.length; index += 1) {
    hash = (hash * 31 + seed.charCodeAt(index)) % 360
  }
  return hash
}

interface AvatarProps {
  name: string
  size?: 'sm' | 'md' | 'lg'
  className?: string
}

const SIZES = {
  sm: 'size-9 text-[0.75rem] rounded-md',
  md: 'size-11 text-sm rounded-lg',
  lg: 'size-14 text-base rounded-xl',
}

export default function Avatar({ name, size = 'md', className }: AvatarProps) {
  const hue = hueFor(name)
  return (
    <span
      aria-hidden="true"
      style={{
        backgroundColor: `oklch(0.93 0.045 ${hue})`,
        color: `oklch(0.36 0.075 ${hue})`,
      }}
      className={[
        'inline-flex shrink-0 items-center justify-center font-bold tracking-wide',
        SIZES[size],
        className,
      ]
        .filter(Boolean)
        .join(' ')}
    >
      {initials(name)}
    </span>
  )
}
