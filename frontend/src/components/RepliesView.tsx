import { useEffect, useMemo, useState } from 'react';
import { api } from '../lib/api';
import { formatDay, formatTime, timeAgo } from '../lib/format';
import type { Post, Reply, Settings } from '../lib/types';
import { Avatar } from './Avatar';

type Sort = 'latest' | 'top';
const PAGE = 60;

interface Props {
  post: Post;
  settings: Settings;
  onBack: () => void;
  onDelete: () => void;
  onRetry: () => void;
}

export function RepliesView({ post, settings, onBack, onDelete, onRetry }: Props) {
  const [sort, setSort] = useState<Sort>('top');
  const [shown, setShown] = useState(PAGE);
  const [liked, setLiked] = useState<Set<number>>(new Set());
  const [loaded, setLoaded] = useState<Reply[] | null>(null);
  const [loadError, setLoadError] = useState('');

  // Refetch as the count grows, so AI replies show up while they're being written.
  useEffect(() => {
    let live = true;
    api
      .replies(post.id)
      .then((r) => {
        if (!live) return;
        setLoaded(r);
        setLoadError('');
      })
      .catch((e) => live && setLoadError(e.message));
    return () => {
      live = false;
    };
  }, [post.id, post.replyCount]);

  const replies = useMemo(() => {
    const list = [...(loaded ?? [])];
    if (sort === 'latest') list.sort((a, b) => b.time - a.time);
    // "Top" favors the written replies so the interesting ones surface first.
    else list.sort((a, b) => b.likes + (b.ai ? 1000 : 0) - (a.likes + (a.ai ? 1000 : 0)));
    return list;
  }, [loaded, sort]);

  const toggleLike = (id: number) =>
    setLiked((s) => {
      const next = new Set(s);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });

  return (
    <div className="screen overlay slide-in">
      <header className="topbar">
        <button className="icon-btn" onClick={onBack} aria-label="Back">
          ‹
        </button>
        <div className="topbar-title">
          <strong>Fan replies</strong>
          <span>{post.replyCount.toLocaleString()} replies</span>
        </div>
        <button
          className="icon-btn danger"
          aria-label="Delete post"
          onClick={() => confirm('Delete this diary entry and its replies?') && onDelete()}
        >
          🗑
        </button>
      </header>

      <div className="replies" onScroll={(e) => {
        const el = e.currentTarget;
        if (el.scrollTop + el.clientHeight > el.scrollHeight - 400) setShown((n) => n + PAGE);
      }}>
        <div className="original">
          <div className="original-head">
            <Avatar name={settings.displayName} src={settings.avatar} size={40} />
            <div>
              <strong>{settings.displayName}</strong>
              <div className="muted small">
                {formatDay(post.time)} · {formatTime(post.time)}
              </div>
            </div>
          </div>
          {post.imageUrl && <img src={post.imageUrl} alt="" className="original-img" />}
          {post.text && <p className="original-text">{post.text}</p>}
        </div>

        <div className="sort-tabs">
          {(['top', 'latest'] as Sort[]).map((s) => (
            <button key={s} className={sort === s ? 'active' : ''} onClick={() => setSort(s)}>
              {s === 'top' ? 'Top' : 'Latest'}
            </button>
          ))}
        </div>

        {post.status === 'pending' && <div className="status-row">✨ Fans are typing…</div>}
        {!loaded && !loadError && <div className="status-row muted">Loading replies…</div>}
        {loadError && <div className="status-row error-text">{loadError}</div>}
        {post.status === 'error' && (
          <div className="status-row error-text">
            {post.error}{' '}
            <button className="link" onClick={onRetry}>
              Retry
            </button>
          </div>
        )}

        {replies.slice(0, shown).map((r) => {
          const isLiked = liked.has(r.id);
          return (
            <div className="reply" key={r.id}>
              <Avatar name={r.fan} size={34} />
              <div className="reply-body">
                <div className="reply-head">
                  <strong>{r.fan}</strong>
                  <span className="muted small">{timeAgo(r.time)}</span>
                </div>
                <div className="reply-text">{r.text}</div>
              </div>
              <button className={`like ${isLiked ? 'on' : ''}`} onClick={() => toggleLike(r.id)}>
                {isLiked ? '♥' : '♡'}
                <span>{r.likes + (isLiked ? 1 : 0) || ''}</span>
              </button>
            </div>
          );
        })}
      </div>
    </div>
  );
}
