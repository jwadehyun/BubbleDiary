import { ImageManipulator, SaveFormat } from 'expo-image-manipulator';
import * as ImagePicker from 'expo-image-picker';

/**
 * Lets the user pick a photo and shrinks it to a JPEG data URL, the format the server takes. Phone photos are often
 * 5+ MB; at 1280px they're a few hundred KB. Returns undefined if the user cancelled.
 */
export async function pickPhoto(maxSize = 1280, square = false): Promise<string | undefined> {
  const result = await ImagePicker.launchImageLibraryAsync({
    mediaTypes: ['images'],
    allowsEditing: square,
    aspect: square ? [1, 1] : undefined,
    quality: 1,
  });
  if (result.canceled || !result.assets?.length) return undefined;
  const asset = result.assets[0];
  const wide = asset.width >= asset.height;
  const resized = await ImageManipulator.manipulate(asset.uri)
    .resize(wide ? { width: Math.min(maxSize, asset.width), height: null } : { width: null, height: Math.min(maxSize, asset.height) })
    .renderAsync();
  const saved = await resized.saveAsync({ format: SaveFormat.JPEG, compress: 0.8, base64: true });
  return `data:image/jpeg;base64,${saved.base64}`;
}
