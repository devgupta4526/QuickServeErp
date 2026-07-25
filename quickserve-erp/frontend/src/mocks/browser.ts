import { setupWorker } from 'msw/browser'
import { handlers } from './handlers'

/**
 * MSW Service Worker instance.
 * Started conditionally in main.tsx when import.meta.env.VITE_MOCK === 'true'.
 */
export const worker = setupWorker(...handlers)
