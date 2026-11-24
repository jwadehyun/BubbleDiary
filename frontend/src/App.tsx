import { useEffect, useRef, useState } from 'react';
import { ChatView } from './components/ChatView';
import { Login } from './components/Login';
import { RepliesView } from './components/RepliesView';
import { SettingsPanel } from './components/SettingsPanel';
import { api, AuthError } from './lib/api';
import type { Me, Post, Provider, Settings } from './lib/types';

// The server writes AI replies in the background; check pending posts this often.
const POLL_MS = 2000;

export default function App() {
  const [me, setMe] = useState<Me | null>(null);
  const [signedOut, setSignedOut] = useState(false);
  const [providers, setProviders] = useState<Provider[]>([]);
  const [posts, setPosts] = useState<Post[]>([]);
  const [toast, setToast] = useState('');
  const [openPostId, setOpenPostId] = useState<number | null>(null);
  const [showSettings, setShowSettings] = useState(false);
  const saveTimer = useRef<number>();

  const fail = (e: unknown) => {
    if (e instanceof AuthError) {
      setMe(null);
      setSignedOut(true);
    } else {
      setToast((e as Error).message);
    }
  };

  const signIn = (m: Me) => {
    setMe(m);
    setSignedOut(false);
    api.posts().then(setPosts).catch(fail);
  };

  useEffect(() => {
    // This first request also gives the browser its CSRF cookie.
    api.providers().then(setProviders).catch(() => {});
    api.me().then(signIn).catch(fail);
  }, []);

  useEffect(() => {
    if (!toast) return;
    const t = setTimeout(() => setToast(''), 5000);
    return () => clearTimeout(t);
  }, [toast]);

  // Poll pending posts so reply counts tick up as each AI batch lands.
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

  // Settings change on every keystroke, so update the screen now and save once typing pauses.
  const updateSettings = (s: Settings) => {
    setMe((m) => m && { ...m, settings: s });
    clearTimeout(saveTimer.current);
    saveTimer.current = window.setTimeout(() => {
      // Empty text tells the server to clear a field (null would mean "unchanged").
      api
        .updateSettings({
          ...s,
          avatar: s.avatar ?? '',
          statusText: s.statusText ?? '',
          profileMessage: s.profileMessage ?? '',
          fanPrompt: s.fanPrompt ?? '',
        })
        .catch((e) => {
          fail(e);
          api.me().then(setMe).catch(() => {});
        });
    }, 500);
  };

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

  const deletePost = async (id: number) => {
    try {
      await api.deletePost(id);
      setPosts((ps) => ps.filter((p) => p.id !== id));
      setOpenPostId(null);
    } catch (e) {
      fail(e);
    }
  };

  const clearPosts = async () => {
    try {
      await api.deleteAll();
      setPosts([]);
    } catch (e) {
      fail(e);
    }
  };

  const logout = async () => {
    await api.logout().catch(() => {});
    setShowSettings(false);
    setOpenPostId(null);
    setPosts([]);
    setMe(null);
    setSignedOut(true);
  };

  if (!me) {
    return (
      <div className="phone">
        {signedOut ? <Login providers={providers} /> : <div className="screen loading">🫧</div>}
      </div>
    );
  }

  const openPost = posts.find((p) => p.id === openPostId);

  return (
    <div className="phone">
      <ChatView
        settings={me.settings}
        aiEnabled={me.aiEnabled}
        posts={posts}
        onSend={send}
        onOpen={setOpenPostId}
        onRetry={retry}
        onSettings={() => setShowSettings(true)}
      />
      {openPost && (
        <RepliesView
          post={openPost}
          settings={me.settings}
          onBack={() => setOpenPostId(null)}
          onDelete={() => deletePost(openPost.id)}
          onRetry={() => retry(openPost.id)}
        />
      )}
      {showSettings && (
        <SettingsPanel
          me={me}
          onChange={updateSettings}
          onClearPosts={clearPosts}
          onLogout={logout}
          onClose={() => setShowSettings(false)}
        />
      )}
      {toast && (
        <div className="toast" onClick={() => setToast('')}>
          {toast}
        </div>
      )}
    </div>
  );
}
