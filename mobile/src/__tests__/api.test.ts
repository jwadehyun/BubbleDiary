import { api, AuthError, initApi } from '@/lib/api';
import { saveTokens } from '@/lib/tokenStore';

jest.mock('@/lib/config', () => ({ apiBaseUrl: () => 'http://server' }));
jest.mock('@/lib/tokenStore', () => ({ saveTokens: jest.fn(), loadTokens: jest.fn() }));

const oldTokens = { accessToken: 'old-access', refreshToken: 'old-refresh', expiresIn: 3600 };
const newTokens = { accessToken: 'new-access', refreshToken: 'new-refresh', expiresIn: 3600 };

function respond(status: number, body?: unknown) {
  return Promise.resolve({ ok: status < 400, status, json: () => Promise.resolve(body) } as Response);
}

describe('api', () => {
  const fetchMock = jest.fn();
  const signedOut = jest.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    signedOut.mockReset();
    (saveTokens as jest.Mock).mockReset();
    globalThis.fetch = fetchMock;
    initApi(oldTokens, signedOut);
  });

  test('gets a fresh access token after a 401 and tries again', async () => {
    fetchMock
      .mockReturnValueOnce(respond(401))
      .mockReturnValueOnce(respond(200, newTokens))
      .mockReturnValueOnce(respond(200, { name: 'Jisung' }));

    await expect(api.me()).resolves.toEqual({ name: 'Jisung' });

    expect(fetchMock.mock.calls[1][0]).toBe('http://server/api/auth/refresh');
    expect(JSON.parse(fetchMock.mock.calls[1][1].body)).toEqual({ refreshToken: 'old-refresh' });
    expect(fetchMock.mock.calls[2][1].headers.Authorization).toBe('Bearer new-access');
    expect(saveTokens).toHaveBeenCalledWith(newTokens);
    expect(signedOut).not.toHaveBeenCalled();
  });

  test('signs out when the refresh token no longer works', async () => {
    fetchMock.mockReturnValueOnce(respond(401)).mockReturnValueOnce(respond(401));

    await expect(api.me()).rejects.toBeInstanceOf(AuthError);
    expect(saveTokens).toHaveBeenLastCalledWith(null);
    expect(signedOut).toHaveBeenCalled();
  });

  test("shows the server's error message", async () => {
    fetchMock.mockReturnValueOnce(respond(429, { message: 'You can post 5 times a day.' }));

    await expect(api.createPost('hi')).rejects.toThrow('You can post 5 times a day.');
    expect(signedOut).not.toHaveBeenCalled();
  });

  test('a failed sign-in is an error, not a sign-out', async () => {
    initApi(null, signedOut);
    fetchMock.mockReturnValueOnce(respond(401, { message: 'Sign-in token is invalid or expired' }));

    await expect(api.signIn('google', 'bad')).rejects.toThrow('Sign-in token is invalid or expired');
    expect(signedOut).not.toHaveBeenCalled();
  });
});
