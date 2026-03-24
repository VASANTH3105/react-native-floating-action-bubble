import { useEffect, useState } from 'react';
import { AppState, Pressable, StyleSheet, Text, View } from 'react-native';
import {
  FloatingActionBubbleView,
  hideOverlay,
  isOverlayPermissionGranted,
  requestOverlayPermission,
  showOverlay,
} from 'react-native-floating-action-bubble';

export default function App() {
  const [hasPermission, setHasPermission] = useState(false);
  const refreshPermission = async () => {
    const granted = await isOverlayPermissionGranted();
    setHasPermission(granted);
  };

  useEffect(() => {
    refreshPermission();
    const subscription = AppState.addEventListener('change', (state) => {
      if (state === 'active') {
        refreshPermission();
      }
    });
    return () => subscription.remove();
  }, []);

  const handleStart = () => {
    showOverlay({
      size: 56,
      color: '#FFFFFF',
      borderColor: '#FFFFFF',
      borderWidth: 6,
      bubbleOpacity: 1,
      borderOpacity: 0.6,
      autoFade: true,
      autoFadeOpacity: 0.35,
      autoFadeTimingMs: 2500,
    });
  };

  const handleStop = () => {
    hideOverlay();
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Floating Bubble Overlay</Text>

      {!hasPermission ? (
        <Pressable
          style={styles.primaryButton}
          onPress={requestOverlayPermission}
        >
          <Text style={styles.primaryButtonText}>Open Settings</Text>
        </Pressable>
      ) : (
        <View style={styles.controls}>
          <Pressable style={styles.primaryButton} onPress={handleStart}>
            <Text style={styles.primaryButtonText}>Start</Text>
          </Pressable>
          <Pressable style={styles.secondaryButton} onPress={handleStop}>
            <Text style={styles.secondaryButtonText}>Stop</Text>
          </Pressable>
        </View>
      )}

      <View style={styles.previewCard}>
        <Text style={styles.previewTitle}>In-app Preview</Text>
        <FloatingActionBubbleView
          color="#FFFFFF"
          borderColor="#FFFFFF"
          borderWidth={6}
          bubbleOpacity={1}
          borderOpacity={0.6}
          autoFade
          autoFadeOpacity={0.35}
          autoFadeTimingMs={2500}
          style={styles.box}
        />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 24,
    backgroundColor: '#0E1116',
  },
  title: {
    color: '#FFFFFF',
    fontSize: 20,
    marginBottom: 20,
    fontWeight: '600',
  },
  controls: {
    width: '100%',
  },
  primaryButton: {
    width: '100%',
    backgroundColor: '#3A6FF8',
    paddingVertical: 14,
    borderRadius: 12,
    alignItems: 'center',
    marginBottom: 12,
  },
  primaryButtonText: {
    color: '#FFFFFF',
    fontWeight: '600',
  },
  secondaryButton: {
    width: '100%',
    backgroundColor: '#1E2430',
    paddingVertical: 14,
    borderRadius: 12,
    alignItems: 'center',
  },
  secondaryButtonText: {
    color: '#FFFFFF',
    fontWeight: '600',
  },
  previewCard: {
    marginTop: 24,
    width: '100%',
    alignItems: 'center',
    padding: 16,
    borderRadius: 16,
    backgroundColor: '#151A22',
  },
  previewTitle: {
    color: '#A5B0C2',
    marginBottom: 12,
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
