import { client } from './generated/client.gen'

export * from './generated'

export type ProblemDetail = {
  type?: string
  title: string
  status: number
  detail: string
  instance: string
  errors?: { detail: string; pointer: string }[]
}

export class ApiError extends Error {
  constructor(public problem: ProblemDetail) {
    super(problem.errors?.map((error) => error.detail).join('; ') ?? problem.detail)
  }
}

// CSRF protection
client.interceptors.request.use((request) => {
  const token = readCookie('XSRF-TOKEN')
  if (token !== null) {
    request.headers.set('X-XSRF-TOKEN', token)
  }
  return request
})

function readCookie(name: string): string | null {
  for (const cookie of document.cookie.split('; ')) {
    if (cookie.startsWith(name + '=')) {
      return cookie.substring(name.length + 1)
    }
  }
  return null
}

client.interceptors.error.use((error, response) => {
  if (!response) return error
  return new ApiError(error as ProblemDetail)
})
