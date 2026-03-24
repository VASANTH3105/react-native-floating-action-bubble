#import <React/RCTBridgeModule.h>
#import <UserNotifications/UserNotifications.h>

@interface FloatingActionBubbleModule : NSObject <RCTBridgeModule>
@end

@implementation FloatingActionBubbleModule

RCT_EXPORT_MODULE(FloatingActionBubbleModule)

RCT_EXPORT_METHOD(requestNotificationPermission:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
  [center requestAuthorizationWithOptions:(UNAuthorizationOptionAlert | UNAuthorizationOptionSound | UNAuthorizationOptionBadge)
                        completionHandler:^(BOOL granted, NSError * _Nullable error) {
    if (error) {
      reject(@"notification_error", @"Failed to request notification permission", error);
      return;
    }
    resolve(@(granted));
  }];
}

RCT_EXPORT_METHOD(scheduleNotification:(NSDictionary *)options)
{
  NSString *title = options[@"title"] ?: @"Floating Bubble";
  NSString *body = options[@"body"] ?: @"Tap to open the app";
  NSNumber *delayMs = options[@"delayMs"] ?: @(0);
  NSString *deepLink = options[@"deepLink"];

  UNMutableNotificationContent *content = [[UNMutableNotificationContent alloc] init];
  content.title = title;
  content.body = body;
  if (deepLink) {
    content.userInfo = @{@"deepLink": deepLink};
  }

  NSTimeInterval delay = MAX(0, delayMs.doubleValue / 1000.0);
  UNTimeIntervalNotificationTrigger *trigger = [UNTimeIntervalNotificationTrigger triggerWithTimeInterval:MAX(delay, 1) repeats:NO];

  UNNotificationRequest *request = [UNNotificationRequest requestWithIdentifier:[[NSUUID UUID] UUIDString]
                                                                        content:content
                                                                        trigger:trigger];

  [[UNUserNotificationCenter currentNotificationCenter] addNotificationRequest:request withCompletionHandler:nil];
}

@end
