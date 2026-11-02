import { createContext, type ReactNode, useCallback, useContext, useEffect, useState } from 'react';
import { api, AuthError } from './api';
import type { Post } from './types';

// The server writes AI replies in the background; check pending posts this often.
const POLL_MS = 2000;

interface Posts {
  posts: Post[];
  loading: boolean;
  error: string;
  reload: () => Promise<void>;
  /** Resolves to false if the post couldn't be saved, so the draft is kept. */
  send: (text: string, image?: string) => Promise<boolean>;
  retry: (id: number) => Promise<void>;
  remove: (id: number) => Promise<void>;
  removeAll: () => Promise<void>;
  /** The last error from an action, shown as a message; cleared by the screen. */
  notice: string;
  clearNotice: () => void;
}

const PostsContext = createContext<Posts | null>(null);

export function usePosts() {
  const posts = useContext(PostsContext);
  if (!posts) throw new Error('usePosts must be used inside <PostsProvider>');
  return posts;
}

/** The signed-in user's diary, shared by the feed and the replies screen. */
export function PostsProvider({ children }: { children: ReactNode }) {
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const fail = (e: unknown) => {
    // A 401 already sent the user to the sign-in screen.
    if (!(e instanceof AuthError)) setNotice((e as Error).message);
  };

  const reload = useCallback(async () => {
    try {
      setPosts(await api.posts());
      setError('');
    } catch (e) {
      if (!(e instanceof AuthError)) setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  // Poll pending posts so reply counts tick up once the AI replies land.
  const pendingIds = posts
    .filter((p) => p.status === 'pending')
    .map((p) => p.id)
    .join(',');
  useEffect(() => {
    if (!pendingIds) return;
    const timer = setInterval(() => {
      for (const id of pendingIds.split(',').map(Number)) {
        api
          .post(id)
          .then((fresh) => setPosts((ps) => ps.map((p) => (p.id === id ? fresh : p))))
          .catch((e) => e instanceof AuthError && fail(e));
      }
    }, POLL_MS);
    return () => clearInterval(timer);
  }, [pendingIds]);

  const send = async (text: string, image?: string) => {
    try {
      const post = await api.createPost(text, image);
      setPosts((ps) => [...ps, post]);
      return true;
    } catch (e) {
      fail(e);
      return false;
    }
  };

  const retry = async (id: number) => {
    try {
      const post = await api.retry(id);
      setPosts((ps) => ps.map((p) => (p.id === id ? post : p)));
    } catch (e) {
      fail(e);
    }
  };

  const remove = async (id: number) => {
    try {
      await api.deletePost(id);
      setPosts((ps) => ps.filter((p) => p.id !== id));
    } catch (e) {
      fail(e);
    }
  };

  const removeAll = async () => {
    try {
      await api.deleteAll();
      setPosts([]);
    } catch (e) {
      fail(e);
    }
  };

  return (
    <PostsContext.Provider
      value={{ posts, loading, error, reload, send, retry, remove, removeAll, notice, clearNotice: () => setNotice('') }}
    >
      {children}
    </PostsContext.Provider>
  );
}
