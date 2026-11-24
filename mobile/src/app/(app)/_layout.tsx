import { Stack } from 'expo-router';
import { PostsProvider } from '@/lib/posts';
import { colors } from '@/lib/theme';

export default function AppLayout() {
  return (
    <PostsProvider>
      <Stack screenOptions={{ headerShown: false, contentStyle: { backgroundColor: colors.bg } }}>
        <Stack.Screen name="index" />
        <Stack.Screen name="post/[id]" />
        <Stack.Screen name="settings" options={{ presentation: 'modal' }} />
      </Stack>
    </PostsProvider>
  );
}
