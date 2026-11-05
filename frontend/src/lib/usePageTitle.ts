import { useEffect } from 'react'

const SUFFIX = 'HomeServices'

/** Keeps the document title in step with the visible page. */
export function usePageTitle(title?: string): void {
  useEffect(() => {
    document.title = title ? `${title} · ${SUFFIX}` : SUFFIX
  }, [title])
}
