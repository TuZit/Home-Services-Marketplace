interface SkeletonProps {
  className?: string
}

export default function Skeleton({ className }: SkeletonProps) {
  return (
    <div
      aria-hidden="true"
      className={[
        'animate-shimmer skeleton-sheen rounded-md bg-surface-3',
        className,
      ]
        .filter(Boolean)
        .join(' ')}
    />
  )
}
