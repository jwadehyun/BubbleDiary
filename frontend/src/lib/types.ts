export type FanVoice = 'english' | 'korean' | 'mixed';

export interface Reply {
  id: number;
  fan: string;
  text: string;
  time: number;
  likes: number;
  /** true for AI-written replies, false for quick generic reactions */
  ai: boolean;
}

export interface Post {
  id: number;
  text: string;
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
  model: string;
  voice: FanVoice;
  replyCount: number;
  fillerCount: number;
}

export interface Me {
  email: string | null;
  name: string | null;
  /** false when the server has no OpenRouter API key */
  aiEnabled: boolean;
  settings: Settings;
}

export interface ModelInfo {
  id: string;
  name: string;
  free: boolean;
  vision: boolean;
}

export interface Provider {
  id: string;
  name: string;
  loginUrl: string;
}
