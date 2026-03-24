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
    BOOL _positionSticky;
    BOOL _stickyShapeAdaptive;
    CGFloat _stickyCornerRadius;
    CAShapeLayer * _maskLayer;
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
    _maskLayer = [CAShapeLayer layer];
    _view.layer.mask = _maskLayer;

    _size = 48.0;
    _bubbleOpacity = 1.0;
    _borderOpacity = 1.0;
    _autoFadeOpacity = 0.2;
    _autoFadeTimingMs = 2000;
    _autoFade = NO;
    _positionSticky = NO;
    _stickyShapeAdaptive = YES;
    _stickyCornerRadius = 12.0;

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

    if (oldViewProps.positionSticky != newViewProps.positionSticky) {
        _positionSticky = newViewProps.positionSticky;
    }

    if (oldViewProps.stickyShapeAdaptive != newViewProps.stickyShapeAdaptive) {
        _stickyShapeAdaptive = newViewProps.stickyShapeAdaptive;
        if (!_stickyShapeAdaptive) {
            [self applyCircleShape];
        }
    }

    if (oldViewProps.stickyCornerRadius != newViewProps.stickyCornerRadius) {
        _stickyCornerRadius = newViewProps.stickyCornerRadius > 0 ? newViewProps.stickyCornerRadius : 12.0;
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
  [self applyCircleShape];
}

- (void)handlePan:(UIPanGestureRecognizer *)gesture
{
  UIView *target = _view;
  CGPoint translation = [gesture translationInView:target.superview];
  if (gesture.state == UIGestureRecognizerStateBegan) {
    [self cancelAutoFade];
    _view.alpha = 1.0;
    [self animateToCircleShape];
  }

  CGPoint newCenter = CGPointMake(target.center.x + translation.x, target.center.y + translation.y);
  target.center = newCenter;
  [gesture setTranslation:CGPointZero inView:target.superview];

  if (gesture.state == UIGestureRecognizerStateEnded || gesture.state == UIGestureRecognizerStateCancelled) {
    if (_positionSticky) {
      [self snapToNearestEdge];
    }
    [self scheduleAutoFade];
  }
}

- (void)snapToNearestEdge
{
  UIView *parent = _view.superview;
  if (!parent) return;

  CGFloat maxX = parent.bounds.size.width - _view.bounds.size.width;
  CGFloat maxY = parent.bounds.size.height - _view.bounds.size.height;

  CGFloat left = _view.frame.origin.x;
  CGFloat top = _view.frame.origin.y;
  CGFloat right = maxX - _view.frame.origin.x;
  CGFloat bottom = maxY - _view.frame.origin.y;

  NSString *edge = @"LEFT";
  CGFloat minVal = left;
  if (right < minVal) { minVal = right; edge = @"RIGHT"; }
  if (top < minVal) { minVal = top; edge = @"TOP"; }
  if (bottom < minVal) { minVal = bottom; edge = @"BOTTOM"; }

  CGRect frame = _view.frame;
  if ([edge isEqualToString:@"LEFT"]) {
    frame.origin.x = 0;
  } else if ([edge isEqualToString:@"RIGHT"]) {
    frame.origin.x = maxX;
  } else if ([edge isEqualToString:@"TOP"]) {
    frame.origin.y = 0;
  } else if ([edge isEqualToString:@"BOTTOM"]) {
    frame.origin.y = maxY;
  }

  [UIView animateWithDuration:0.2 animations:^{
    self->_view.frame = frame;
  } completion:^(BOOL finished) {
    if (self->_stickyShapeAdaptive) {
      [self animateToStickyShapeForEdge:edge];
    } else {
      [self animateToCircleShape];
    }
  }];
}

- (void)applyCircleShape
{
  UIBezierPath *path = [UIBezierPath bezierPathWithRoundedRect:_view.bounds
                                             byRoundingCorners:UIRectCornerAllCorners
                                                   cornerRadii:CGSizeMake(MIN(_view.bounds.size.width, _view.bounds.size.height) / 2.0,
                                                                           MIN(_view.bounds.size.width, _view.bounds.size.height) / 2.0)];
  _maskLayer.frame = _view.bounds;
  _maskLayer.path = path.CGPath;
}

- (void)applyStickyShapeForEdge:(NSString *)edge
{
  CGFloat r = _stickyCornerRadius;
  UIRectCorner corners = 0;

  if ([edge isEqualToString:@"LEFT"]) {
    corners = UIRectCornerTopRight | UIRectCornerBottomRight;
  } else if ([edge isEqualToString:@"RIGHT"]) {
    corners = UIRectCornerTopLeft | UIRectCornerBottomLeft;
  } else if ([edge isEqualToString:@"TOP"]) {
    corners = UIRectCornerBottomLeft | UIRectCornerBottomRight;
  } else if ([edge isEqualToString:@"BOTTOM"]) {
    corners = UIRectCornerTopLeft | UIRectCornerTopRight;
  } else {
    corners = UIRectCornerAllCorners;
  }

  UIBezierPath *path = [UIBezierPath bezierPathWithRoundedRect:_view.bounds
                                             byRoundingCorners:corners
                                                   cornerRadii:CGSizeMake(r, r)];
  _maskLayer.frame = _view.bounds;
  _maskLayer.path = path.CGPath;
}

- (void)animateToCircleShape
{
  UIBezierPath *path = [UIBezierPath bezierPathWithRoundedRect:_view.bounds
                                             byRoundingCorners:UIRectCornerAllCorners
                                                   cornerRadii:CGSizeMake(MIN(_view.bounds.size.width, _view.bounds.size.height) / 2.0,
                                                                           MIN(_view.bounds.size.width, _view.bounds.size.height) / 2.0)];
  [self animateMaskToPath:path];
}

- (void)animateToStickyShapeForEdge:(NSString *)edge
{
  CGFloat r = _stickyCornerRadius;
  UIRectCorner corners = 0;

  if ([edge isEqualToString:@"LEFT"]) {
    corners = UIRectCornerTopRight | UIRectCornerBottomRight;
  } else if ([edge isEqualToString:@"RIGHT"]) {
    corners = UIRectCornerTopLeft | UIRectCornerBottomLeft;
  } else if ([edge isEqualToString:@"TOP"]) {
    corners = UIRectCornerBottomLeft | UIRectCornerBottomRight;
  } else if ([edge isEqualToString:@"BOTTOM"]) {
    corners = UIRectCornerTopLeft | UIRectCornerTopRight;
  } else {
    corners = UIRectCornerAllCorners;
  }

  UIBezierPath *path = [UIBezierPath bezierPathWithRoundedRect:_view.bounds
                                             byRoundingCorners:corners
                                                   cornerRadii:CGSizeMake(r, r)];
  [self animateMaskToPath:path];
}

- (void)animateMaskToPath:(UIBezierPath *)path
{
  if (!_maskLayer) return;
  CGPathRef fromPath = _maskLayer.path ?: path.CGPath;
  _maskLayer.frame = _view.bounds;
  _maskLayer.path = path.CGPath;

  CABasicAnimation *animation = [CABasicAnimation animationWithKeyPath:@"path"];
  animation.fromValue = (__bridge id)fromPath;
  animation.toValue = (__bridge id)path.CGPath;
  animation.duration = 0.18;
  animation.timingFunction = [CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionEaseInEaseOut];
  [_maskLayer addAnimation:animation forKey:@"path"];
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
