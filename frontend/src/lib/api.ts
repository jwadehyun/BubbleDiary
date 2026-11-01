import type { Me, ModelInfo, Post, Provider, Reply, Settings } from './types';

/** The session ended (or never started); show the login screen. */
export class AuthError extends Error {}

// Spring Security sets this cookie; writes must echo it back in a header.
function csrfToken() {
  const match = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/);
  return match ? decodeURIComponent(match[1]) : undefined;
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const token = csrfToken();
  if (token) headers['X-XSRF-TOKEN'] = token;
  const res = await fetch(path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
    credentials: 'same-origin',
  });
  if (res.status === 401) throw new AuthError('Please sign in again.');
  if (!res.ok) {
    const data = await res.json().catch(() => null);
    throw new Error(data?.message || `Request failed (${res.status})`);
  }
  return res.status === 204 ? (undefined as T) : res.json();
}

export const api = {
  providers: () => request<Provider[]>('GET', '/api/auth/providers'),
  me: () => request<Me>('GET', '/api/me'),
  updateSettings: (s: Partial<Settings>) => request<Me>('PUT', '/api/me/settings', s),
  models: () => request<ModelInfo[]>('GET', '/api/models'),

  posts: () => request<Post[]>('GET', '/api/messages'),
  post: (id: number) => request<Post>('GET', `/api/messages/${id}`),
  replies: (id: number) => request<Reply[]>('GET', `/api/messages/${id}/replies`),
  createPost: (text: string, image?: string) => request<Post>('POST', '/api/messages', { text, image }),
  retry: (id: number) => request<Post>('POST', `/api/messages/${id}/retry`),
  deletePost: (id: number) => request<void>('DELETE', `/api/messages/${id}`),
  deleteAll: () => request<void>('DELETE', '/api/messages'),

  logout: () => request<void>('POST', '/logout'),
};
