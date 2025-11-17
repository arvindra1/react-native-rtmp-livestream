const { withInfoPlist, withAndroidManifest, createRunOncePlugin } = require('expo/config-plugins');

const pkg = require('./package.json');

function addiOSPermissions(config) {
  return withInfoPlist(config, (config) => {
    config.modResults.NSCameraUsageDescription =
      config.modResults.NSCameraUsageDescription ||
      'This app requires camera access for RTMP live streaming.';
    config.modResults.NSMicrophoneUsageDescription =
      config.modResults.NSMicrophoneUsageDescription ||
      'This app requires microphone access for RTMP audio streaming.';
    return config;
  });
}

function addAndroidPermissions(config) {
  return withAndroidManifest(config, (config) => {
    const perms = [
      'android.permission.CAMERA',
      'android.permission.RECORD_AUDIO',
      'android.permission.INTERNET'
    ];

    perms.forEach((perm) => {
      if (!config.modResults.manifest['uses-permission']) {
        config.modResults.manifest['uses-permission'] = [];
      }

      if (
        !config.modResults.manifest['uses-permission'].some(
          (p) => p.$['android:name'] === perm
        )
      ) {
        config.modResults.manifest['uses-permission'].push({
          $: { 'android:name': perm }
        });
      }
    });

    return config;
  });
}

const withRTMPLiveStream = (config) => {
  config = addiOSPermissions(config);
  config = addAndroidPermissions(config);
  return config;
};

module.exports = createRunOncePlugin(
  withRTMPLiveStream,
  pkg.name,
  pkg.version
);
