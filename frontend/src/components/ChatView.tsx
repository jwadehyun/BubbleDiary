import { Fragment, useEffect, useRef, useState } from 'react';
import { compressImage } from '../lib/image';
import { compact, formatDay, formatTime, useCountUp } from '../lib/format';
import type { Post, Settings } from '../lib/types';
import { Avatar } from './Avatar';

interface Props {
  settings: Settings;
  aiEnabled: boolean;
  posts: Post[];
  /** resolves to false if the post couldn't be saved, so the draft is kept */
  onSend: (text: string, image?: string) => Promise<boolean>;
  onOpen: (id: number) => void;
  onRetry: (id: number) => void;
  onSettings: () => void;
}

export function ChatView({ settings, aiEnabled, posts, onSend, onOpen, onRetry, onSettings }: Props) {
  const [text, setText] = useState('');
  const [image, setImage] = useState<string>();
  const [imageError, setImageError] = useState('');
  const [sending, setSending] = useState(false);
  const listRef = useRef<HTMLDivElement>(null);
  const fileRef = useRef<HTMLInputElement>(null);
  const mountedAt = useRef(Date.now());

  useEffect(() => {
    listRef.current?.scrollTo({ top: listRef.current.scrollHeight, behavior: 'smooth' });
  }, [posts.length]);

  const pickImage = async (file?: File) => {
    if (!file) return;
    try {
      setImage(await compressImage(file));
      setImageError('');
    } catch (e) {
      setImageError((e as Error).message);
    }
  };

  const submit = async () => {
    if ((!text.trim() && !image) || sending) return;
    setSending(true);
    if (await onSend(text.trim(), image)) {
      setText('');
      setImage(undefined);
    }
    setSending(false);
  };

  const totalReplies = posts.reduce((n, p) => n + p.replyCount, 0);

  return (
    <div className="screen">
      <header className="topbar">
        <Avatar name={settings.displayName} src={settings.avatar} size={38} />
        <div className="topbar-title">
          <strong>{settings.displayName}</strong>
          <span>
            {settings.fandom} · {compact(totalReplies)} replies
          </span>
        </div>
        <button className="icon-btn" onClick={onSettings} aria-label="Settings">
          ⚙︎
        </button>
      </header>

      <div className="chat" ref={listRef}>
        {!aiEnabled && (
          <div className="banner">
            AI fan replies are off because the server has no OpenRouter key. Set <code>OPENROUTER_API_KEY</code> in{' '}
            <code>.env</code> and restart. Quick reactions still work.
          </div>
        )}
        {posts.length === 0 && (
          <div className="empty">
            <div className="empty-emoji">🫧</div>
            <p>Your diary is empty.</p>
            <p className="muted">Write about your day or share a photo. Your fans are waiting!</p>
          </div>
        )}
        {posts.map((p, i) => {
          const newDay = i === 0 || new Date(posts[i - 1].time).toDateString() !== new Date(p.time).toDateString();
          return (
            <Fragment key={p.id}>
              {newDay && <div className="day-sep">{formatDay(p.time)}</div>}
              <PostBubble
                post={p}
                settings={settings}
                isNew={p.time > mountedAt.current}
                onOpen={() => onOpen(p.id)}
                onRetry={() => onRetry(p.id)}
              />
            </Fragment>
          );
        })}
      </div>

      <div className="composer">
        {image && (
          <div className="preview">
            <img src={image} alt="" />
            <button onClick={() => setImage(undefined)} aria-label="Remove photo">
              ✕
            </button>
          </div>
        )}
        {imageError && <div className="error-text">{imageError}</div>}
        <div className="composer-row">
          <button className="icon-btn attach" onClick={() => fileRef.current?.click()} aria-label="Add photo">
            ＋
          </button>
          <input
            ref={fileRef}
            type="file"
            accept="image/*"
            hidden
            onChange={(e) => {
              pickImage(e.target.files?.[0]);
              e.target.value = '';
            }}
          />
          <textarea
            value={text}
            rows={1}
            placeholder="Write to your fans…"
            onChange={(e) => setText(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
                e.preventDefault();
                submit();
              }
            }}
          />
          <button className="send" onClick={submit} disabled={sending || (!text.trim() && !image)}>
            ↑
          </button>
        </div>
      </div>
    </div>
  );
}

function PostBubble({
  post,
  settings,
  isNew,
  onOpen,
  onRetry,
}: {
  post: Post;
  settings: Settings;
  isNew: boolean;
  onOpen: () => void;
  onRetry: () => void;
}) {
  const count = useCountUp(post.replyCount, isNew);
  return (
    <div className="post">
      <Avatar name={settings.displayName} src={settings.avatar} size={36} />
      <div className="post-body">
        <div className="post-name">{settings.displayName}</div>
        <div className="bubble-row">
          <div className="bubble" onClick={onOpen}>
            {post.imageUrl && <img src={post.imageUrl} alt="" className="bubble-img" />}
            {post.text && <div className="bubble-text">{post.text}</div>}
          </div>
          <span className="post-time">{formatTime(post.time)}</span>
        </div>
        <div className="post-meta">
          <button className="reply-pill" onClick={onOpen}>
            💬 {count.toLocaleString()}
            {post.status === 'pending' && <span className="typing">fans typing…</span>}
          </button>
          {post.status === 'error' && (
            <span className="error-text">
              {post.error}{' '}
              <button className="link" onClick={onRetry}>
                Retry
              </button>
            </span>
          )}
        </div>
      </div>
    </div>
  );
}
