require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name         = "react-native-rtmp-livestream"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.description  = package["description"]
  s.homepage     = package["repository"]["url"]
  s.license      = package["license"]
  s.author       = package["author"]

  s.source       = { :git => package["repository"]["url"], :tag => "v#{s.version}" }
  s.platform     = :ios, "13.0"

  s.source_files = "ios/**/*.{h,m,mm,swift}"

  s.dependency "ExpoModulesCore"
end
