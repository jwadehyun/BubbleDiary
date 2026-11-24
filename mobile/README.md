# BubbleDiary mobile app 📱

The iOS and Android app, built with [Expo](https://docs.expo.dev) (React Native + TypeScript). It talks to the same Spring Boot server as the web app.

## Run it on your phone (development)

1. **Start the server with development sign-in on.** In the repo root `.env`, add `DEV_LOGIN=true`, then run `docker compose up --build` (or `mvn spring-boot:run`).
2. **Start the app:**
   ```sh
   cd mobile
   npm install
   npx expo start
   ```
3. Install **Expo Go** from the App Store / Play Store, make sure your phone is on the same Wi-Fi as your computer, and scan the QR code. Sign in with any name in the "Development sign-in" box.

The app finds the server at your computer's address on port 8080. If your Mac asks whether Java/Docker may accept incoming connections, allow it. To use a server elsewhere, set `EXPO_PUBLIC_API_URL`, e.g. `EXPO_PUBLIC_API_URL=https://api.example.com npx expo start`.

**No phone handy?** Press `w` to open the app in a browser. Add `CORS_ORIGINS=http://localhost:8081` to the server's `.env` first, so the browser is allowed to call it.

## Real sign-in (Google and Apple)

Development sign-in is only for testing; turn `DEV_LOGIN` off anywhere real people can reach the server.

- **Sign in with Apple** works in Expo Go on an iPhone, but Apple issues those tokens for Expo Go rather than your app. For your own app it needs an Apple Developer account and a development build (below). Put the app's bundle id (`com.jwadehyun.bubblediary`, set in `app.config.ts`) in the server's `APPLE_BUNDLE_ID`.
- **Google sign-in** needs a development build; it doesn't work in Expo Go. In Google Cloud, create an **iOS** and an **Android** OAuth client next to your existing web one, then set:
  - `EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID`: your existing **web** client id (Google puts it in the token the server checks)
  - `EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID` and `GOOGLE_IOS_URL_SCHEME`: from the iOS client
  - on the server, `GOOGLE_MOBILE_CLIENT_IDS`: the iOS and Android client ids, comma-separated

  Put the `EXPO_PUBLIC_*` and `GOOGLE_IOS_URL_SCHEME` values in `mobile/.env` (git-ignored).

A **development build** is your own copy of the app with all the native code. Build it in the cloud with EAS, no Xcode or Android Studio needed:

```sh
npx eas-cli@latest build --profile development --platform ios   # or android
```

## Tests

```sh
npm test            # Jest + React Native Testing Library
npm run typecheck   # TypeScript
```

## Where things are

- `src/app/`: screens; each file is a route (Expo Router). `(app)/` is everything behind sign-in.
  - `sign-in.tsx`, `(app)/index.tsx` (the diary feed), `(app)/post/[id].tsx` (replies), `(app)/settings.tsx`
- `src/components/`: the post bubble, reply row, avatar and top bar
- `src/lib/api.ts`: server calls; gets a new access token automatically when one expires
- `src/lib/session.tsx`, `src/lib/posts.tsx`: who's signed in, and their posts (shared by all screens)
- `src/lib/nativeSignIn.ts`: Google and Apple sign-in
- `app.config.ts`: app name, bundle id, permissions
