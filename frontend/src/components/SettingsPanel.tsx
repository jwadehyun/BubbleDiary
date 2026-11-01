import { useState } from 'react';
import { compressImage } from '../lib/image';
import type { FanVoice, Me, ModelInfo, Settings } from '../lib/types';
import { Avatar } from './Avatar';

interface Props {
  me: Me;
  models: ModelInfo[];
  modelsError: string;
  onChange: (s: Settings) => void;
  onClearPosts: () => void;
  onLogout: () => void;
  onClose: () => void;
}

export function SettingsPanel({ me, models, modelsError, onChange, onClearPosts, onLogout, onClose }: Props) {
  const settings = me.settings;
  const [freeOnly, setFreeOnly] = useState(true);
  const set = <K extends keyof Settings>(key: K, value: Settings[K]) => onChange({ ...settings, [key]: value });

  const visible = models.filter((m) => !freeOnly || m.free || m.id === settings.model);
  const current = models.find((m) => m.id === settings.model);

  return (
    <div className="screen overlay slide-up">
      <header className="topbar">
        <div className="topbar-title">
          <strong>Settings</strong>
        </div>
        <button className="done" onClick={onClose}>
          Done
        </button>
      </header>

      <div className="settings">
        <section>
          <h3>Profile</h3>
          <div className="profile-row">
            <label className="avatar-pick">
              <Avatar name={settings.displayName} src={settings.avatar} size={64} />
              <input
                type="file"
                accept="image/*"
                hidden
                onChange={async (e) => {
                  const f = e.target.files?.[0];
                  if (f) set('avatar', await compressImage(f, 256));
                }}
              />
              <span className="small">Change</span>
            </label>
            <div className="grow">
              <label className="field">
                Your name
                <input value={settings.displayName} onChange={(e) => set('displayName', e.target.value)} />
              </label>
              <label className="field">
                Fandom name
                <input value={settings.fandom} onChange={(e) => set('fandom', e.target.value)} />
              </label>
            </div>
          </div>
          {settings.avatar && (
            <button className="link" onClick={() => set('avatar', null)}>
              Remove photo
            </button>
          )}
        </section>

        <section>
          <h3>AI (OpenRouter)</h3>
          <p className="hint first">
            {me.aiEnabled
              ? "Replies are written with the server's OpenRouter key. You only choose the model."
              : 'The server has no OpenRouter key yet, so only quick reactions appear.'}
          </p>

          <label className="field">
            Model
            <select value={settings.model} onChange={(e) => set('model', e.target.value)}>
              {!current && <option value={settings.model}>{settings.model}</option>}
              {visible.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.name}
                  {m.free ? ' · free' : ''}
                  {m.vision ? ' · 📷' : ''}
                </option>
              ))}
            </select>
          </label>
          <label className="check">
            <input type="checkbox" checked={freeOnly} onChange={(e) => setFreeOnly(e.target.checked)} />
            Show free models only
          </label>
          {modelsError && <p className="error-text">{modelsError}</p>}
          <p className="hint">
            📷 = can see your photos. Other models only know a photo was attached. Free models have daily limits.
          </p>
        </section>

        <section>
          <h3>Fans</h3>
          <label className="field">
            Language & vibe
            <select value={settings.voice} onChange={(e) => set('voice', e.target.value as FanVoice)}>
              <option value="english">English + K-fan slang</option>
              <option value="korean">Mostly Korean</option>
              <option value="mixed">Global mix</option>
            </select>
          </label>
          <label className="field">
            AI-written replies per post: <b>{settings.replyCount}</b>
            <input
              type="range"
              min={5}
              max={200}
              step={5}
              value={settings.replyCount}
              onChange={(e) => set('replyCount', Number(e.target.value))}
            />
          </label>
          <label className="field">
            Quick reactions per post: <b>{settings.fillerCount}</b>
            <input
              type="range"
              min={0}
              max={1000}
              step={10}
              value={settings.fillerCount}
              onChange={(e) => set('fillerCount', Number(e.target.value))}
            />
          </label>
          <p className="hint">
            Quick reactions ("😭😭", "ILY", "eat well!!") fill out the feed without using AI. More AI replies take longer
            and use more of your free quota.
          </p>
        </section>

        <section>
          <h3>Account</h3>
          <p className="account">
            Signed in as <b>{me.email ?? me.name ?? 'you'}</b>
          </p>
          <button className="outline-btn" onClick={onLogout}>
            Sign out
          </button>
        </section>

        <section>
          <h3>Data</h3>
          <button
            className="danger-btn"
            onClick={() => confirm('Delete every diary entry and all replies?') && onClearPosts()}
          >
            Delete all entries
          </button>
        </section>
      </div>
    </div>
  );
}
