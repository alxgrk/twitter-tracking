# twitter-tracking-android
An Android app tracking clicks in Twitter - used for my Master's thesis. 

## Build Release APK

If you want to build the release APK, make sure to do the following:
 - to your `local.properties` file located at project root add:
   - `prod.api.url` - the endpoint where to publish events to
   - `signing.store.password` - the JKS store's password
   - `signing.key.password` - the key's password
 - in Android Studio
   - on the very left pane, select `Build Variants` -> `Active Build Variant` should be `release`
   - click `Build` -> `Generate Signed Bundle / APK...` to finish creation

## How to connect to the server locally

If you are running the server on your computer, you need to do the following to connect you phone to it:
 - only neccessary on first connect
   - connect your phone via USB
   - run `adb tcpip 5555`
   - run `export DEVICE_PORT=$(adb shell ifconfig | grep -A1 wlan0 | grep -oP '\d+\.\d+\.\d+\.\d+(?=\s+Bcast)')`
   - disconnect USB cable
 - run `adb connect $DEVICE_PORT`
 - check if your computer's IP matches the one in [build.gradle's build config](./build.gradle) and in [network security config](./app/src/main/res/xml/network_security_config.xml) 
    - find your own ip e.g. by running `ifconfig | grep -A1 wlp3s0 | grep -oP '\d+\.\d+\.\d+\.\d+(?=\s+netmask)'`
 - optional: open port temporarily by running `sudo iptables -A INPUT -p tcp -s $DEVICE_PORT --dport 8080 -j ACCEPT`
