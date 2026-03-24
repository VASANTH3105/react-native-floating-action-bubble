import {
  codegenNativeComponent,
  type ColorValue,
  type ViewProps,
} from 'react-native';
import type { Float, Int32 } from 'react-native/Libraries/Types/CodegenTypes';

export interface NativeProps extends ViewProps {
  color?: ColorValue;
  size?: Float;
  borderColor?: ColorValue;
  borderWidth?: Float;
  bubbleOpacity?: Float;
  borderOpacity?: Float;
  autoFade?: boolean;
  autoFadeOpacity?: Float;
  autoFadeTimingMs?: Int32;
}

export default codegenNativeComponent<NativeProps>('FloatingActionBubbleView');
