import { Image } from 'expo-image';
import { StyleSheet, Text, View } from 'react-native';
import { fanColor } from '@/lib/fans';

export function Avatar({ name, src, size }: { name: string; src?: string | null; size: number }) {
  const round = { width: size, height: size, borderRadius: size / 2 };
  if (src) return <Image source={{ uri: src }} style={round} contentFit="cover" />;
  return (
    <View style={[styles.fallback, round, { backgroundColor: fanColor(name) }]}>
      <Text style={[styles.initial, { fontSize: size * 0.42 }]}>{[...name.trim()][0]?.toUpperCase() ?? '?'}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  fallback: { alignItems: 'center', justifyContent: 'center' },
  initial: { color: '#4b3f6b', fontWeight: '700' },
});
