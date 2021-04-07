#import "NimmstaSdkPlugin.h"
#if __has_include(<nimmsta_sdk/nimmsta_sdk-Swift.h>)
#import <nimmsta_sdk/nimmsta_sdk-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "nimmsta_sdk-Swift.h"
#endif

@implementation NimmstaSdkPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftNimmstaSdkPlugin registerWithRegistrar:registrar];
}
@end
