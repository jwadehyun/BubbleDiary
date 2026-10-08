# BubbleDiary 🫧

A diary styled like the Korean fan app Bubble. You post diary entries or photos as if you were the celebrity, and "fans" reply: quick generic reactions right away, and AI-written replies (via OpenRouter) a few seconds later.

- **Backend**: Spring Boot 4, Spring Security OAuth login (Google), JPA + MySQL, OpenRouter calls made from the server
- **Front end**: React + Vite in `frontend/`, ported from BubbleDiaryV2

## Setup

1. **MySQL**: create a database called `bubblediary`.
   If you ran an earlier version, drop the old tables once, because their columns changed:
   `DROP TABLE IF EXISTS replies, messages, users;`
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

- **Quick reactions** (default 250): generic replies like "😭😭" and "eat well!!", created as soon as you post.
- **AI replies** (default 40): written by an OpenRouter model about your specific post, 25 at a time, in the background. The app checks every 2 seconds while a post is "fans typing…", so the count ticks up.
- The server's key pays for all users, so only free models are offered unless you set `OPENROUTER_ALLOW_PAID=true`.

## Tests

`mvn test` runs against an in-memory H2 database with a mocked login. It doesn't need MySQL, Google or OpenRouter.

## API

All `/api` endpoints except `/api/auth/providers` require login. Write requests need the `X-XSRF-TOKEN` header (copied from the `XSRF-TOKEN` cookie).

| Method | Path | |
|---|---|---|
| GET | `/api/auth/providers` | Login buttons |
| GET / PUT | `/api/me`, `/api/me/settings` | Profile and diary settings |
| GET | `/api/models` | Models you can pick |
| GET / POST / DELETE | `/api/messages` | List, create, delete all |
| GET / DELETE | `/api/messages/{id}` | One post (poll while `pending`) |
| GET | `/api/messages/{id}/replies`, `/image` | Fan replies, photo |
| POST | `/api/messages/{id}/retry` | Rewrite the AI replies |

## Shutdown
sudo lsof -i :PORT_NUMBER
kill PID
