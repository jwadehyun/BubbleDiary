import { apiBaseUrl } from './config';
import { saveTokens } from './tokenStore';
import type { Me, Post, Reply, Settings, Tokens } from './types';

/** The sign-in ended (or never started); show the sign-in screen. */
export class AuthError extends Error {}

let tokens: Tokens | null = null;
let onSignedOut = () => {};
let refreshing: Promise<boolean> | null = null;

/** Called by the session once at startup. */
export function initApi(saved: Tokens | null, signedOut: () => void) {
  tokens = saved;
  onSignedOut = signedOut;
}

export async function setTokens(next: Tokens | null) {
  tokens = next;
  await saveTokens(next);
}

/** Trades the refresh token for new tokens. Parallel requests that all got a 401 share one refresh. */
function refresh(): Promise<boolean> {
  refreshing ??= (async () => {
    const current = tokens;
    if (!current) return false;
    try {
      const res = await fetch(`${apiBaseUrl()}/api/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: current.refreshToken }),
      });
      if (!res.ok) return false;
      await setTokens(await res.json());
      return true;
    } catch {
      return false;
    }
  })().finally(() => {
    refreshing = null;
  });
  return refreshing;
}

async function request<T>(method: string, path: string, body?: unknown, retried = false): Promise<T> {
  const signedIn = tokens !== null;
  const headers: Record<string, string> = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (tokens) headers.Authorization = `Bearer ${tokens.accessToken}`;

  let res: Response;
  try {
    res = await fetch(`${apiBaseUrl()}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new Error(`Can't reach the server at ${apiBaseUrl()}. Is it running?`);
  }

  // The access token lasts an hour; get a new one and try once more.
  if (res.status === 401 && signedIn) {
    if (!retried && (await refresh())) return request(method, path, body, true);
    await setTokens(null);
    onSignedOut();
    throw new AuthError('Please sign in again.');
  }
  if (!res.ok) {
    const data = await res.json().catch(() => null);
    throw new Error(data?.message || `Request failed (${res.status})`);
  }
  return res.status === 204 ? (undefined as T) : res.json();
}

export const api = {
  /** provider: "google" or "apple"; idToken from native sign-in. */
  signIn: (provider: string, idToken: string, name?: string) =>
    request<Tokens>('POST', '/api/auth/token', { provider, idToken, name }),
  /** Only works when the server runs with DEV_LOGIN=true. */
  devSignIn: (name: string) => request<Tokens>('POST', '/api/auth/dev', { name }),
  logout: () =>
    tokens ? request<void>('POST', '/api/auth/logout', { refreshToken: tokens.refreshToken }) : Promise.resolve(),

  me: () => request<Me>('GET', '/api/me'),
  updateSettings: (s: Partial<Settings>) => request<Me>('PUT', '/api/me/settings', s),
  deleteAccount: () => request<void>('DELETE', '/api/me'),

  posts: () => request<Post[]>('GET', '/api/messages'),
  post: (id: number) => request<Post>('GET', `/api/messages/${id}`),
  replies: (id: number) => request<Reply[]>('GET', `/api/messages/${id}/replies`),
  createPost: (text: string, image?: string) => request<Post>('POST', '/api/messages', { text, image }),
  retry: (id: number) => request<Post>('POST', `/api/messages/${id}/retry`),
  deletePost: (id: number) => request<void>('DELETE', `/api/messages/${id}`),
  deleteAll: () => request<void>('DELETE', '/api/messages'),

  /** An image source for a post's photo. Photos are private, so the request carries the access token. */
  imageSource: (path: string) => ({
    uri: `${apiBaseUrl()}${path}`,
    headers: tokens ? { Authorization: `Bearer ${tokens.accessToken}` } : undefined,
  }),
};
