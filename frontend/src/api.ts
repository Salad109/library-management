import { client } from './generated/client.gen'

export * from './generated'

export type ProblemDetail = {
  type?: string
  title?: string
  status: number
  detail: string
  instance?: string
  errors?: { detail: string; pointer: string }[]
}

export class ApiError extends Error {
  constructor(public problem: ProblemDetail) {
    super(problem.detail)
  }
}

client.interceptors.error.use((error, response) => {
  if (!response) return error
  // Login and logout still answer in plain text
  return new ApiError(typeof error === 'string' ? { status: response.status, detail: error } : (error as ProblemDetail))
})
