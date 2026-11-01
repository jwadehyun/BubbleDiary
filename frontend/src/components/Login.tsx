import type { Provider } from '../lib/types';

// Fake fan bubbles that float behind the login card.
const FLOATING = ['보고싶었어 ㅠㅠ', 'eat well!!', '💜💜💜', 'notified!!', '사랑해!!', 'ILY', '🥹🥹', 'first!!'];

export function Login({ providers }: { providers: Provider[] }) {
  const failed = new URLSearchParams(location.search).has('loginError');

  return (
    <div className="screen login">
      <div className="login-float" aria-hidden>
        {FLOATING.map((t, i) => (
          <span key={t} style={{ animationDelay: `${i * 0.9}s`, left: `${(i * 37) % 80 + 5}%` }}>
            {t}
          </span>
        ))}
      </div>

      <div className="login-card">
        <div className="login-logo">🫧</div>
        <h1>BubbleDiary</h1>
        <p className="muted">Write your day like a star.<br />Your fans are always online.</p>

        {failed && <p className="error-text">Sign-in didn't finish. Please try again.</p>}

        <div className="login-buttons">
          {providers.map((p) => (
            <a key={p.id} className={`login-btn ${p.id}`} href={p.loginUrl}>
              <span className="login-icon">{p.name[0]}</span>
              Continue with {p.name}
            </a>
          ))}
          {providers.length === 0 && (
            <p className="hint">No login providers are set up. Add OAuth credentials to the server's .env.</p>
          )}
        </div>
      </div>
    </div>
  );
}
