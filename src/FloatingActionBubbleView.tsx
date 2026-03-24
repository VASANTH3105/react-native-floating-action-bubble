import FloatingActionBubbleViewNativeComponent, {
  type NativeProps,
} from './FloatingActionBubbleViewNativeComponent';

const DEFAULTS: Partial<NativeProps> = {
  size: 48,
  color: 'white',
  borderColor: 'white',
  borderWidth: 0,
  bubbleOpacity: 1,
  borderOpacity: 1,
  autoFade: false,
  autoFadeOpacity: 0.2,
  autoFadeTimingMs: 2000,
  onLongPressNavigate: undefined,
};

export default function FloatingActionBubbleView(props: NativeProps) {
  return <FloatingActionBubbleViewNativeComponent {...DEFAULTS} {...props} />;
}
