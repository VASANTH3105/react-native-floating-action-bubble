import { View, StyleSheet } from 'react-native';
import { FloatingActionBubbleView } from 'react-native-floating-action-bubble';

export default function App() {
  return (
    <View style={styles.container}>
      <FloatingActionBubbleView color="#32a852" style={styles.box} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
