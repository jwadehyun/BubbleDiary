import { fireEvent, render, screen } from '@testing-library/react-native';
import { PostBubble } from '@/components/PostBubble';
import type { Post, Settings } from '@/lib/types';

jest.mock('@/lib/config', () => ({ apiBaseUrl: () => 'http://server' }));

const settings: Settings = {
  displayName: 'Jisung',
  fandom: 'STAY',
  avatar: null,
  statusText: null,
  profileMessage: null,
  voice: 'english',
  fanPrompt: null,
};

const post = (overrides: Partial<Post>): Post => ({
  id: 1,
  text: 'ate tteokbokki today',
  imageUrl: null,
  time: Date.now(),
  replyCount: 84,
  status: 'done',
  error: null,
  ...overrides,
});

describe('<PostBubble />', () => {
  test('shows the post, its reply count, and that fans are typing', async () => {
    await render(
      <PostBubble post={post({ status: 'pending' })} settings={settings} isNew={false} onOpen={jest.fn()} onRetry={jest.fn()} />,
    );
    expect(screen.getByText('ate tteokbokki today')).toBeTruthy();
    expect(screen.getByText('💬 84')).toBeTruthy();
    expect(screen.getByText('fans typing…')).toBeTruthy();
  });

  test('the reply count opens the replies', async () => {
    const onOpen = jest.fn();
    await render(<PostBubble post={post({})} settings={settings} isNew={false} onOpen={onOpen} onRetry={jest.fn()} />);
    await fireEvent.press(screen.getByLabelText('84 replies'));
    expect(onOpen).toHaveBeenCalled();
    expect(screen.queryByText('fans typing…')).toBeNull();
  });

  test('a failed post shows why and can be retried', async () => {
    const onRetry = jest.fn();
    await render(
      <PostBubble
        post={post({ status: 'error', error: 'Rate limited.' })}
        settings={settings}
        isNew={false}
        onOpen={jest.fn()}
        onRetry={onRetry}
      />,
    );
    await fireEvent.press(screen.getByText('Retry'));
    expect(onRetry).toHaveBeenCalled();
  });
});
