import type { ComponentType, ReactNode, SVGProps } from 'react'

export type IconProps = SVGProps<SVGSVGElement>

interface BaseProps extends IconProps {
  children: ReactNode
}

function Base({ children, ...props }: BaseProps) {
  return (
    <svg
      viewBox="0 0 24 24"
      width="1em"
      height="1em"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
      {...props}
    >
      {children}
    </svg>
  )
}

export function SearchIcon(props: IconProps) {
  return (
    <Base {...props}>
      <circle cx="11" cy="11" r="6.8" />
      <path d="M16.1 16.1 21 21" />
    </Base>
  )
}

export function HomeIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="M4 10.6 12 4.2l8 6.4V19a1.4 1.4 0 0 1-1.4 1.4h-3.9v-5.6H9.3v5.6H5.4A1.4 1.4 0 0 1 4 19z" />
    </Base>
  )
}

export function GridIcon(props: IconProps) {
  return (
    <Base {...props}>
      <rect x="4" y="4" width="6.4" height="6.4" rx="1.8" />
      <rect x="13.6" y="4" width="6.4" height="6.4" rx="1.8" />
      <rect x="4" y="13.6" width="6.4" height="6.4" rx="1.8" />
      <rect x="13.6" y="13.6" width="6.4" height="6.4" rx="1.8" />
    </Base>
  )
}

export function GearIcon(props: IconProps) {
  return (
    <Base {...props}>
      <circle cx="12" cy="12" r="5.5" />
      <circle cx="12" cy="12" r="2.4" />
      <path d="M17.5 12h1.8M12 17.5v1.8M6.5 12H4.7M12 6.5V4.7" />
      <path d="M15.9 15.9l1.3 1.3M8.1 15.9l-1.3 1.3M15.9 8.1l1.3-1.3M8.1 8.1 6.8 6.8" />
    </Base>
  )
}

export function SparklesIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="M12 3.8l1.7 4.6 4.6 1.7-4.6 1.7L12 16.4l-1.7-4.6-4.6-1.7 4.6-1.7z" />
      <path d="M18.4 15.2l.7 1.9 1.9.7-1.9.7-.7 1.9-.7-1.9-1.9-.7 1.9-.7z" />
    </Base>
  )
}

export function DropletIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="M12 3.4c0 0 6.1 6.3 6.1 10.3a6.1 6.1 0 0 1-12.2 0C5.9 9.7 12 3.4 12 3.4z" />
    </Base>
  )
}

export function BoltIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="M13.6 3.2 5.9 13.4h5.2L10.4 20.8l7.7-10.2h-5.2z" />
    </Base>
  )
}

export function RollerIcon(props: IconProps) {
  return (
    <Base {...props}>
      <rect x="3.6" y="4.4" width="12.4" height="5.4" rx="1.4" />
      <path d="M16 7.1h3.2a1.4 1.4 0 0 1 1.4 1.4v1.8a1.4 1.4 0 0 1-1.4 1.4h-6.8a1.4 1.4 0 0 0-1.4 1.4v1.8" />
      <rect x="10.2" y="14.9" width="2.8" height="5" rx="1.2" />
    </Base>
  )
}

export function TruckIcon(props: IconProps) {
  return (
    <Base {...props}>
      <rect x="2.8" y="7.4" width="10.4" height="8.4" rx="1.2" />
      <path d="M13.2 10.3h3.7l3.3 3.1v2.4h-7z" />
      <circle cx="7" cy="17.7" r="1.7" />
      <circle cx="16.6" cy="17.7" r="1.7" />
    </Base>
  )
}

export function LeafIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="M19.7 4.3c0 8.6-3.7 15-9.9 15A4.3 4.3 0 0 1 5.5 15c0-5.6 5.8-10.7 14.2-10.7z" />
      <path d="M6.3 18.9c2.1-4.3 5.2-7.3 9.3-8.9" />
    </Base>
  )
}

export function PlugIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="M9.2 3.6v4.1M14.8 3.6v4.1" />
      <path d="M6.6 7.7h10.8v3a5.4 5.4 0 0 1-10.8 0z" />
      <path d="M12 16.1v4.3" />
    </Base>
  )
}

export function SnowflakeIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="M12 3.4v17.2M4.6 7.7l14.8 8.6M19.4 7.7 4.6 16.3" />
    </Base>
  )
}

export function ShieldCheckIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="M12 3.4l7.2 2.6v5.3c0 4.5-3 8.2-7.2 9.7-4.2-1.5-7.2-5.2-7.2-9.7V6z" />
      <path d="M8.9 12.1l2.2 2.2 4-4.4" />
    </Base>
  )
}

export function StarIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="M12 3.6l2.6 5.3 5.8.85-4.2 4.09 1 5.78-5.2-2.73-5.2 2.73 1-5.78L3.6 9.75l5.8-.85z" />
    </Base>
  )
}

export function CheckIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="M5.2 12.6 9.8 17.2 18.8 7.4" />
    </Base>
  )
}

export function CheckCircleIcon(props: IconProps) {
  return (
    <Base {...props}>
      <circle cx="12" cy="12" r="8.6" />
      <path d="M8.4 12.2l2.5 2.5 4.7-5" />
    </Base>
  )
}

export function XIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="M6.4 6.4 17.6 17.6M17.6 6.4 6.4 17.6" />
    </Base>
  )
}

export function PlusIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="M12 5.2v13.6M5.2 12h13.6" />
    </Base>
  )
}

export function MenuIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="M4 7.2h16M4 12h16M4 16.8h16" />
    </Base>
  )
}

export function ChevronRightIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="M9.4 6.2 15.2 12l-5.8 5.8" />
    </Base>
  )
}

export function ChevronDownIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="M6.2 9.4 12 15.2l5.8-5.8" />
    </Base>
  )
}

export function ArrowLeftIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="M19 12H5.2" />
      <path d="M11.4 18.4 5 12l6.4-6.4" />
    </Base>
  )
}

export function ArrowRightIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="M5 12h13.8" />
      <path d="M12.6 5.6 19 12l-6.4 6.4" />
    </Base>
  )
}

export function MapPinIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="M12 21.2s-6.6-5.6-6.6-10.6a6.6 6.6 0 1 1 13.2 0c0 5-6.6 10.6-6.6 10.6z" />
      <circle cx="12" cy="10.4" r="2.4" />
    </Base>
  )
}

export function CalendarIcon(props: IconProps) {
  return (
    <Base {...props}>
      <rect x="3.6" y="5.4" width="16.8" height="15" rx="2.4" />
      <path d="M3.6 10.2h16.8M8.4 3.4v4M15.6 3.4v4" />
    </Base>
  )
}

export function ClockIcon(props: IconProps) {
  return (
    <Base {...props}>
      <circle cx="12" cy="12" r="8.6" />
      <path d="M12 7.4V12l3.2 2" />
    </Base>
  )
}

export function UserIcon(props: IconProps) {
  return (
    <Base {...props}>
      <circle cx="12" cy="8.4" r="3.7" />
      <path d="M5.2 20.2c0-3.6 3.1-5.7 6.8-5.7s6.8 2.1 6.8 5.7" />
    </Base>
  )
}

export function UsersIcon(props: IconProps) {
  return (
    <Base {...props}>
      <circle cx="9.4" cy="8.6" r="3.3" />
      <path d="M3.2 19.8c0-3.2 2.8-5.1 6.2-5.1s6.2 1.9 6.2 5.1" />
      <path d="M16.2 5.8a3.3 3.3 0 0 1 0 5.6M17.6 15c2 .6 3.3 2.3 3.3 4.8" />
    </Base>
  )
}

export function LogOutIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="M15.2 12H3.8" />
      <path d="M8.2 8.2 4 12l4.2 3.8" />
      <path d="M11.6 4.4h6.6a1.6 1.6 0 0 1 1.6 1.6v12a1.6 1.6 0 0 1-1.6 1.6h-6.6" />
    </Base>
  )
}

export function AlertTriangleIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="M12 4.6l8.1 14.1H3.9z" />
      <path d="M12 10.1v4.2" />
      <path d="M12 17.1h.01" />
    </Base>
  )
}

export function InfoIcon(props: IconProps) {
  return (
    <Base {...props}>
      <circle cx="12" cy="12" r="8.6" />
      <path d="M12 11.2v5" />
      <path d="M12 8.1h.01" />
    </Base>
  )
}

export function RefreshIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="M19.6 12a7.6 7.6 0 1 1-2.3-5.4" />
      <path d="M19.9 4.4v4.4h-4.4" />
    </Base>
  )
}

export function SlidersIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="M4 7.5h4.2M12.2 7.5H20" />
      <circle cx="10.2" cy="7.5" r="1.9" />
      <path d="M4 16.5h7.2M15.2 16.5H20" />
      <circle cx="13.2" cy="16.5" r="1.9" />
    </Base>
  )
}

export function TagIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="M4 12.4V5.6A1.6 1.6 0 0 1 5.6 4h6.8l7.6 7.6a2.1 2.1 0 0 1 0 3l-5.4 5.4a2.1 2.1 0 0 1-3 0z" />
      <circle cx="8.4" cy="8.4" r="1.5" />
    </Base>
  )
}

export function FileTextIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="M6.2 3.8h7.2l4.4 4.4v12a1.6 1.6 0 0 1-1.6 1.6H6.2a1.6 1.6 0 0 1-1.6-1.6V5.4a1.6 1.6 0 0 1 1.6-1.6z" />
      <path d="M13.4 3.8v4.4h4.4" />
      <path d="M8.4 12.6h7.2M8.4 16.2h4.8" />
    </Base>
  )
}

export function PhoneIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="M6.6 3.6h2.9l1.5 3.9-2 1.4a11.6 11.6 0 0 0 5.7 5.7l1.4-2 3.9 1.5v2.9a2 2 0 0 1-2.2 2A16.4 16.4 0 0 1 4.6 5.8a2 2 0 0 1 2-2.2z" />
    </Base>
  )
}

export function WalletIcon(props: IconProps) {
  return (
    <Base {...props}>
      <rect x="3.4" y="6.2" width="17.2" height="12.4" rx="2.4" />
      <path d="M3.4 10.4h17.2" />
      <path d="M16.4 14.4h.01" />
    </Base>
  )
}

const CATEGORY_ICONS: Array<[RegExp, ComponentType<IconProps>]> = [
  [/ve-?sinh|don dep|giat|clean|housekeep|maid|laundry|deep/i, SparklesIcon],
  [/dien-?lanh|dieu hoa|may lanh|tu lanh|cool|hvac|fridge|air ?cond|heat|vent/i, SnowflakeIcon],
  [/sua chua|bao tri|bao-?duong|repair|maintenance|handy|fix|mount/i, GearIcon],
  [/lap dat|lap-?dat|applianc|install|assembly/i, PlugIcon],
  [/dien|electr|wir|light|socket/i, BoltIcon],
  [/nuoc|plumb|pipe|leak|drain|water/i, DropletIcon],
  [/son|paint|decor|renovat|wallpaper/i, RollerIcon],
  [/chuyen nha|chuyen-?nha|mov|relocat|deliver|transport|haul/i, TruckIcon],
  [/cay|garden|lawn|landscap|plant|tree|pest/i, LeafIcon],
  [/an ninh|bao ve|secur|lock|alarm|guard|cctv/i, ShieldCheckIcon],
]

/** Picks a glyph from the category name and slug, in English or Vietnamese. */
export function categoryIcon(
  name?: string | null,
  slug?: string | null,
): ComponentType<IconProps> {
  const haystack = `${name ?? ''} ${slug ?? ''}`.trim()
  if (!haystack) {
    return GridIcon
  }
  for (const [pattern, Icon] of CATEGORY_ICONS) {
    if (pattern.test(haystack)) {
      return Icon
    }
  }
  return GridIcon
}
