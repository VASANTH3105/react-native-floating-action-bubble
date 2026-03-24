import { NativeModules, Platform, processColor } from 'react-native';

type OverlayOptions = {
  size?: number;
  color?: string;
  borderColor?: string;
  borderWidth?: number;
  bubbleOpacity?: number;
  borderOpacity?: number;
  autoFade?: boolean;
  autoFadeOpacity?: number;
  autoFadeTimingMs?: number;
  onLongPressNavigate?: string;
};

type NotificationOptions = {
  title?: string;
  body?: string;
  delayMs?: number;
  deepLink?: string;
};

const NativeModule = NativeModules.FloatingActionBubbleModule;

export function isOverlayPermissionGranted(): Promise<boolean> {
  if (Platform.OS !== 'android' || !NativeModule?.isOverlayPermissionGranted) {
    return Promise.resolve(false);
  }
  return NativeModule.isOverlayPermissionGranted();
}

export function requestOverlayPermission(): void {
  if (Platform.OS === 'android') {
    NativeModule?.requestOverlayPermission?.();
  }
}

export function showOverlay(options: OverlayOptions = {}): void {
  if (Platform.OS === 'android') {
    const payload = {
      ...options,
      color: options.color ? processColor(options.color) : undefined,
      borderColor: options.borderColor
        ? processColor(options.borderColor)
        : undefined,
    };
    NativeModule?.showOverlay?.(payload);
  }
}

export function hideOverlay(): void {
  if (Platform.OS === 'android') {
    NativeModule?.hideOverlay?.();
  }
}

export function requestNotificationPermission(): Promise<boolean> {
  if (Platform.OS !== 'ios' || !NativeModule?.requestNotificationPermission) {
    return Promise.resolve(false);
  }
  return NativeModule.requestNotificationPermission();
}

export function scheduleNotification(options: NotificationOptions = {}): void {
  if (Platform.OS === 'ios') {
    NativeModule?.scheduleNotification?.(options);
  }
}
