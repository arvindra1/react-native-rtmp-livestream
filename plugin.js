const { withInfoPlist, withAndroidManifest, createRunOncePlugin } = require('expo/config-plugins');
const pkg = require('./package.json');


function addiOSPermissions(config) {
return withInfoPlist(config, config => {
config.modResults.NSCameraUsageDescription = config.modResults.NSCameraUsageDescription || 'This app requires camera access for RTMP live streaming.';
config.modResults.NSMicrophoneUsageDescription = config.modResults.NSMicrophoneUsageDescription || 'This app requires microphone access for RTMP audio.';
return config;
});
}


function addAndroidPermissions(config) {
return withAndroidManifest(config, config => {
const manifest = config.modResults.manifest;
if (!manifest['uses-permission']) manifest['uses-permission'] = [];
const perms = ['android.permission.CAMERA','android.permission.RECORD_AUDIO','android.permission.INTERNET'];
perms.forEach(name => {
if (!manifest['uses-permission'].some(p => p.$['android:name'] === name)) {
manifest['uses-permission'].push({ $: { 'android:name': name } });
}
});
return config;
});
}


const withRTMP = config => {
config = addiOSPermissions(config);
config = addAndroidPermissions(config);
return config;
};


module.exports = createRunOncePlugin(withRTMP, pkg.name, pkg.version);