import type { ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';
import { colors } from '@/lib/theme';

/** The header strip at the top of each screen. */
export function TopBar({ children }: { children: ReactNode }) {
  return <View style={styles.bar}>{children}</View>;
}

const styles = StyleSheet.create({
  bar: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    paddingHorizontal: 14,
    paddingVertical: 10,
    backgroundColor: colors.surface,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: colors.line,
  },
});
