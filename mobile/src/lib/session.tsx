import { createContext, type ReactNode, useCallback, useContext, useEffect, useState } from 'react';
import { api, AuthError, initApi, setTokens } from './api';
import { loadTokens } from './tokenStore';
import type { Me, Tokens } from './types';

interface Session {
  me: Me | null;
  /** true until the saved sign-in has been checked at startup */
  loading: boolean;
  /** set when the server couldn't be reached at startup */
  error: string;
  retry: () => void;
  /** Runs a sign-in (Google, Apple or dev) and loads the profile. */
  signIn: (getTokens: () => Promise<Tokens>) => Promise<void>;
  signOut: () => Promise<void>;
  setMe: (me: Me) => void;
}

const SessionContext = createContext<Session | null>(null);

export function useSession() {
  const session = useContext(SessionContext);
  if (!session) throw new Error('useSession must be used inside <SessionProvider>');
  return session;
}

/** Signed-in state for the whole app. */
export function SessionProvider({ children }: { children: ReactNode }) {
  const [me, setMe] = useState<Me | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let live = true;
    (async () => {
      const saved = await loadTokens();
      initApi(saved, () => setMe(null));
      if (saved) {
        try {
          const profile = await api.me();
          if (live) setMe(profile);
        } catch (e) {
          if (live && !(e instanceof AuthError)) setError((e as Error).message);
        }
      }
      if (live) setLoading(false);
    })();
    return () => {
      live = false;
    };
  }, [attempt]);

  const retry = useCallback(() => {
    setError('');
    setLoading(true);
    setAttempt((n) => n + 1);
  }, []);

  const signIn = useCallback(async (getTokens: () => Promise<Tokens>) => {
    await setTokens(await getTokens());
    setMe(await api.me());
  }, []);

  const signOut = useCallback(async () => {
    await api.logout().catch(() => {});
    await setTokens(null);
    setMe(null);
  }, []);

  return (
    <SessionContext.Provider value={{ me, loading, error, retry, signIn, signOut, setMe }}>
      {children}
    </SessionContext.Provider>
  );
}
