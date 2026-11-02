# BubbleDiary 🫧

A diary styled like the Korean fan app Bubble. You post diary entries or photos as if you were the celebrity, and "fans" reply: quick generic reactions right away, and AI-written replies (via OpenRouter) a few seconds later.

- **Backend**: Spring Boot 4, Spring Security OAuth login (Google), JPA + MySQL, OpenRouter calls made from the server
- **Front end**: React + Vite in `frontend/`, ported from BubbleDiaryV2

## Setup

1. **MySQL**: create a database called `bubblediary`.
   If you ran an earlier version, its `users` table has columns that no longer exist and would block new sign-ups. Either keep your data:
   `ALTER TABLE users DROP COLUMN model, DROP COLUMN reply_count, DROP COLUMN filler_count;`
   or start fresh: `DROP TABLE IF EXISTS replies, messages, users;`
2. **Google OAuth**: at https://console.cloud.google.com/apis/credentials create an *OAuth client ID* (Web application) with these authorized redirect URIs:
   - `http://localhost:8080/login/oauth2/code/google`
   - `http://localhost:5173/login/oauth2/code/google` (for the Vite dev server)
3. **OpenRouter**: get a key at https://openrouter.ai/keys
4. Copy `.env.example` to `.env` and fill it in.

GitHub login works too: uncomment the GitHub lines in `application.properties`, then add `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET` to `.env`.

## Run

Development (hot reload): start Spring Boot from IntelliJ (or `mvn spring-boot:run`), then:

```sh
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. Vite forwards `/api`, `/oauth2`, `/login/oauth2` and `/logout` to port 8080.

Single server: run `npm run build` in `frontend/`. That writes the app to `src/main/resources/static`, so Spring Boot serves everything at http://localhost:8080.

## How replies work

Each post gets replies according to the user's plan (`Plan.java`):

| | Free | Premium |
|---|---|---|
| Quick fan reactions ("😭😭", "eat well!!") | 80 | 200 |
| Quick anti-fan reactions ("who asked", "mid") | 4 | 0 |
| AI-written fan replies | 15 | 30 |
| AI-written anti-fan replies | 1 | 0 |
| Posts per day | 5 | unlimited |

- Quick reactions are saved the moment you post. The AI replies come from one OpenRouter call in the background, and the app checks every 2 seconds while a post shows "fans typing…".
- AI replies are capped at 150 characters. Anti-fans are snarky about the post, never about the person.
- Users can describe how their fans talk ("How should your fans talk?" in settings). That text is added to the prompt as style guidance.
- **The server chooses the model**, not the user, because the server's key pays. Set `OPENROUTER_MODEL_FREE` / `OPENROUTER_MODEL_PREMIUM` in `.env`. The default is a free model, which is fine for development. For production, `anthropic/claude-haiku-5.5` costs about $0.001 per post.
- Nobody can become premium yet; payments come later.

## Tests

`mvn test` runs against an in-memory H2 database with a mocked login. It doesn't need MySQL, Google or OpenRouter.

- `ReplyMixTests`: unit tests for the reply mix, the length cap, parsing and the prompt (no Spring, milliseconds).
- `AiRepliesTests`: the whole posting flow with a fake AI, checking a free post ends with exactly 100 replies.
- `BubbleDiaryApplicationTests`: login, CSRF, settings, the daily post limit, and that users can't see each other's posts.
- `AppAuthTests`: app sign-in, refresh tokens, account deletion, and web + app sharing one account (Google is faked).
- `IdTokenVerifierTests`: forged, expired and wrong-app ID tokens are rejected.

## Sign-in

- **Web**: Google's sign-in page redirects back to the server, which keeps you signed in with a session cookie. Write requests need the `X-XSRF-TOKEN` header (copied from the `XSRF-TOKEN` cookie).
- **Mobile app**: the app signs in natively with Google or Apple and sends the resulting ID token to `POST /api/auth/token`. The server checks it was really signed by Google/Apple for this app, then returns its own tokens:
  - `accessToken`: sent as `Authorization: Bearer …` on every request, valid for 1 hour. No CSRF header needed.
  - `refreshToken`: traded at `POST /api/auth/refresh` for a new pair when the access token expires. Each one works once and lasts 30 days.
- Signing in with the same Google account on the web and in the app gives the same diary.
- Set `APP_JWT_SECRET` in production. Without it the server makes a random key at startup, so app sign-ins end on every restart.

## API

All `/api` endpoints except `/api/auth/*` require sign-in.

| Method | Path | |
|---|---|---|
| GET | `/api/auth/providers` | Web login buttons |
| POST | `/api/auth/token`, `/refresh`, `/logout` | App sign-in: `{provider, idToken}`, then `{refreshToken}` |
| GET / PUT | `/api/me`, `/api/me/settings` | Profile and diary settings |
| DELETE | `/api/me` | Delete the account and all its posts |
| GET / POST / DELETE | `/api/messages` | List, create, delete all |
| GET / DELETE | `/api/messages/{id}` | One post (poll while `pending`) |
| GET | `/api/messages/{id}/replies`, `/image` | Fan replies, photo |
| POST | `/api/messages/{id}/retry` | Rewrite the AI replies |

## Shutdown
sudo lsof -i :PORT_NUMBER
kill PID
