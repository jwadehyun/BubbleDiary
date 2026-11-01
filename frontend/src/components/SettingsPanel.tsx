import { compressImage } from '../lib/image';
import type { FanVoice, Me, Settings } from '../lib/types';
import { Avatar } from './Avatar';

interface Props {
  me: Me;
  onChange: (s: Settings) => void;
  onClearPosts: () => void;
  onLogout: () => void;
  onClose: () => void;
}

export function SettingsPanel({ me, onChange, onClearPosts, onLogout, onClose }: Props) {
  const settings = me.settings;
  const set = <K extends keyof Settings>(key: K, value: Settings[K]) => onChange({ ...settings, [key]: value });

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
          <label className="field">
            Status
            <input
              value={settings.statusText ?? ''}
              maxLength={60}
              placeholder="in Tokyo 🗼"
              onChange={(e) => set('statusText', e.target.value)}
            />
          </label>
          <label className="field">
            Profile message
            <input
              value={settings.profileMessage ?? ''}
              maxLength={150}
              placeholder="thank you for always being here 💜"
              onChange={(e) => set('profileMessage', e.target.value)}
            />
          </label>
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
            How should your fans talk?
            <textarea
              rows={3}
              maxLength={300}
              value={settings.fanPrompt ?? ''}
              placeholder="e.g. they call me bread, tease me about my dance practice, and are very dramatic"
              onChange={(e) => set('fanPrompt', e.target.value)}
            />
          </label>
          <p className="hint">
            {me.plan === 'premium'
              ? 'Premium: 230 replies per post (30 written by AI) and no anti-fans.'
              : 'Free: 100 replies per post. 15 fans and 1 anti-fan write about your post with AI; the rest are quick reactions.'}
            {!me.aiEnabled && ' The server has no OpenRouter key yet, so only quick reactions appear.'}
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
