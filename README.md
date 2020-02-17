## Prerequisites
1. ROS Kinetic (Robot Operating system)
2. Android Studio 
3. Astrobee robot software
4. Gazebo 7 
5. CMake (v3.5.1 is used by the original developer)

## Default path
Put these lines in .bashrc file for convinience
```bash
export AVD="AstrobeeAndroidSim"
export EMULATOR=$HOME/Android/Sdk/tools/emulator
export SOURCE_PATH=$HOME/freeflyer
export ANDROID_PATH="${SOURCE_PATH}_android"
export BUILD_PATH=$HOME/freeflyer_build/native
export INSTALL_PATH=$HOME/freeflyer_install/native
pushd $BUILD_PATH
source devel/setup.bash
popd
```

## Running Simulation 
Running Android Emulator (Terminal 1)
```bash
cd $ANDROID_PATH/scripts
./launch_emulator.sh -n
```
Running Gazebo Simulation (Terminal 2)
```bash
export ROS_IP=$(getent hosts llp | awk '{ print $1 }')
export ROS_MASTER_URI=http://${ROS_IP}:11311
roslaunch astrobee sim.launch dds:=false robot:=sim_pub sviz:=true
```
Running GSM APK and GS APK (Terminal 3) - GSM firstly need to be built
```bash
export ROS_IP=$(getent hosts llp | awk '{ print $1 }')
export ROS_MASTER_URI=http://${ROS_IP}:11311
$ANDROID_PATH/scripts/gs_manager.sh start
cd $SOURCE_PATH/tools/gds_helper/
python gds_simulator.py
```

### Another Bash commands
Building APK on Linux:
```bash
ANDROID_HOME=$HOME/Android/Sdk ./gradlew assembleDebug 
```
Installing apk
```bash
adb install -g -r app/build/outputs/apk/app-debug.apk
```


## Dependencies for building Astrobee
Dependencies listed below: glog, JSON-C, argtable2, libv4l
```bash
sudo apt-get install libgoogle-glog-dev
sudo apt-get install libjson0 libjson0-dev
sudo apt-get install libargtable2-dev
sudo apt-get install libv4l-dev
#fix gazebo 7 missing package
sudo apt-get install libignition-math2-dev
```
### Source Installation<br>
1. [Luajit](http://luajit.org/install.html)<br>
2. [Protobuf](https://github.com/protocolbuffers/protobuf/blob/v3.11.3/src/README.md) (v2.6.1 checkout required)<br>
3. Mavlink. Shortcut: [MavROS](https://dev.px4.io/v1.9.0/en/ros/mavros_installation.html)
4. [Gazebo 7](http://gazebosim.org/tutorials?tut=install_ubuntu&ver=7.0&cat=install)
## Important pages
[Kibo-RPC Download page](https://jaxa.krpc.jp/download/index.html)

