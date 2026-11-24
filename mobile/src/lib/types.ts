// The server's JSON shapes. Kept in step with frontend/src/lib/types.ts (the web app).

export type FanVoice = 'english' | 'korean' | 'mixed';

export interface Reply {
  id: number;
  fan: string;
  text: string;
  time: number;
  likes: number;
  /** true for AI-written replies, false for quick generic reactions */
  ai: boolean;
  kind: 'fan' | 'anti';
}

export interface Post {
  id: number;
  text: string;
  /** server path like /api/messages/1/image, fetched with the access token */
  imageUrl: string | null;
  time: number;
  replyCount: number;
  /** progress of the AI replies, which the server writes in the background */
  status: 'pending' | 'done' | 'error';
  error: string | null;
}

export interface Settings {
  displayName: string;
  fandom: string;
  /** an image URL from the login provider, or an uploaded data URL */
  avatar: string | null;
  statusText: string | null;
  profileMessage: string | null;
  voice: FanVoice;
  /** how the user wants fans to talk; added to the AI prompt */
  fanPrompt: string | null;
}

export interface Me {
  email: string | null;
  name: string | null;
  /** false when the server has no OpenRouter API key */
  aiEnabled: boolean;
  plan: 'free' | 'premium';
  settings: Settings;
}

/** What the server returns from sign-in and refresh. */
export interface Tokens {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}
