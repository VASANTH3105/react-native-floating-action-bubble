#import "FloatingActionBubbleView.h"

#import <React/RCTConversions.h>
#import <React/RCTUtils.h>

#import <react/renderer/components/FloatingActionBubbleViewSpec/ComponentDescriptors.h>
#import <react/renderer/components/FloatingActionBubbleViewSpec/Props.h>
#import <react/renderer/components/FloatingActionBubbleViewSpec/RCTComponentViewHelpers.h>

#import "RCTFabricComponentsPlugins.h"

using namespace facebook::react;

@implementation FloatingActionBubbleView {
    UIView * _view;
    CGFloat _size;
    CGFloat _bubbleOpacity;
    CGFloat _borderOpacity;
    CGFloat _autoFadeOpacity;
    NSInteger _autoFadeTimingMs;
    BOOL _autoFade;
    NSTimer * _fadeTimer;
    NSString * _onLongPressNavigate;
}

+ (ComponentDescriptorProvider)componentDescriptorProvider
{
    return concreteComponentDescriptorProvider<FloatingActionBubbleViewComponentDescriptor>();
}

- (instancetype)initWithFrame:(CGRect)frame
{
  if (self = [super initWithFrame:frame]) {
    static const auto defaultProps = std::make_shared<const FloatingActionBubbleViewProps>();
    _props = defaultProps;

    _view = [[UIView alloc] init];
    _view.userInteractionEnabled = YES;
    _view.layer.masksToBounds = YES;

    _size = 48.0;
    _bubbleOpacity = 1.0;
    _borderOpacity = 1.0;
    _autoFadeOpacity = 0.2;
    _autoFadeTimingMs = 2000;
    _autoFade = NO;

    UIPanGestureRecognizer *pan = [[UIPanGestureRecognizer alloc] initWithTarget:self action:@selector(handlePan:)];
    [_view addGestureRecognizer:pan];

    UILongPressGestureRecognizer *longPress = [[UILongPressGestureRecognizer alloc] initWithTarget:self action:@selector(handleLongPress:)];
    longPress.minimumPressDuration = 0.5;
    longPress.cancelsTouchesInView = NO;
    [_view addGestureRecognizer:longPress];

    self.contentView = _view;
  }

  return self;
}

- (void)updateProps:(Props::Shared const &)props oldProps:(Props::Shared const &)oldProps
{
    const auto &oldViewProps = *std::static_pointer_cast<FloatingActionBubbleViewProps const>(_props);
    const auto &newViewProps = *std::static_pointer_cast<FloatingActionBubbleViewProps const>(props);

    if (oldViewProps.color != newViewProps.color) {
        [_view setBackgroundColor: RCTUIColorFromSharedColor(newViewProps.color)];
    }

    if (oldViewProps.size != newViewProps.size) {
        _size = newViewProps.size > 0 ? newViewProps.size : 48.0;
        [self invalidateIntrinsicContentSize];
        [self setNeedsLayout];
    }

    if (oldViewProps.borderColor != newViewProps.borderColor) {
        UIColor *borderColor = RCTUIColorFromSharedColor(newViewProps.borderColor);
        _view.layer.borderColor = [borderColor colorWithAlphaComponent:_borderOpacity].CGColor;
    }

    if (oldViewProps.borderWidth != newViewProps.borderWidth) {
        _view.layer.borderWidth = newViewProps.borderWidth > 0 ? newViewProps.borderWidth : 0;
    }

    if (oldViewProps.bubbleOpacity != newViewProps.bubbleOpacity) {
        _bubbleOpacity = newViewProps.bubbleOpacity;
        _view.alpha = _bubbleOpacity;
    }

    if (oldViewProps.borderOpacity != newViewProps.borderOpacity) {
        _borderOpacity = newViewProps.borderOpacity;
        UIColor *borderColor = RCTUIColorFromSharedColor(newViewProps.borderColor);
        _view.layer.borderColor = [borderColor colorWithAlphaComponent:_borderOpacity].CGColor;
    }

    if (oldViewProps.autoFade != newViewProps.autoFade) {
        _autoFade = newViewProps.autoFade;
        [self scheduleAutoFade];
    }

    if (oldViewProps.autoFadeOpacity != newViewProps.autoFadeOpacity) {
        _autoFadeOpacity = newViewProps.autoFadeOpacity;
        [self scheduleAutoFade];
    }

    if (oldViewProps.autoFadeTimingMs != newViewProps.autoFadeTimingMs) {
        _autoFadeTimingMs = newViewProps.autoFadeTimingMs;
        [self scheduleAutoFade];
    }

    if (oldViewProps.onLongPressNavigate != newViewProps.onLongPressNavigate) {
        if (!newViewProps.onLongPressNavigate.empty()) {
            _onLongPressNavigate = [NSString stringWithUTF8String:newViewProps.onLongPressNavigate.c_str()];
        } else {
            _onLongPressNavigate = nil;
        }
    }

    [super updateProps:props oldProps:oldProps];
}

- (CGSize)intrinsicContentSize
{
  return CGSizeMake(_size, _size);
}

- (void)layoutSubviews
{
  [super layoutSubviews];
  _view.frame = self.bounds;
  _view.layer.cornerRadius = MIN(self.bounds.size.width, self.bounds.size.height) / 2.0;
}

- (void)handlePan:(UIPanGestureRecognizer *)gesture
{
  UIView *target = _view;
  CGPoint translation = [gesture translationInView:target.superview];
  if (gesture.state == UIGestureRecognizerStateBegan) {
    [self cancelAutoFade];
    _view.alpha = 1.0;
  }

  CGPoint newCenter = CGPointMake(target.center.x + translation.x, target.center.y + translation.y);
  target.center = newCenter;
  [gesture setTranslation:CGPointZero inView:target.superview];

  if (gesture.state == UIGestureRecognizerStateEnded || gesture.state == UIGestureRecognizerStateCancelled) {
    [self scheduleAutoFade];
  }
}

- (void)handleLongPress:(UILongPressGestureRecognizer *)gesture
{
  if (gesture.state != UIGestureRecognizerStateBegan) {
    return;
  }

  if (_onLongPressNavigate.length == 0) {
    return;
  }

  NSURL *url = [NSURL URLWithString:_onLongPressNavigate];
  if (!url) {
    return;
  }

  RCTExecuteOnMainQueue(^{
    UIApplication *app = RCTSharedApplication();
    if (!app) return;
    [app openURL:url options:@{} completionHandler:nil];
  });
}

- (void)scheduleAutoFade
{
  [self cancelAutoFade];
  if (!_autoFade) {
    return;
  }

  __weak typeof(self) weakSelf = self;
  _fadeTimer = [NSTimer scheduledTimerWithTimeInterval:((double)_autoFadeTimingMs / 1000.0)
                                                repeats:NO
                                                  block:^(NSTimer * _Nonnull timer) {
    __strong typeof(weakSelf) strongSelf = weakSelf;
    if (!strongSelf) return;
    [UIView animateWithDuration:0.2 animations:^{
      strongSelf->_view.alpha = strongSelf->_autoFadeOpacity;
    }];
  }];
}

- (void)cancelAutoFade
{
  if (_fadeTimer) {
    [_fadeTimer invalidate];
    _fadeTimer = nil;
  }
}

@end
