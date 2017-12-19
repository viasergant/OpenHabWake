#!/bin/sh
apt-get update
apt-get install vim mc wget
wget -qO - 'https://bintray.com/user/downloadSubjectPublicKey?username=openhab' | sudo apt-key add -
apt-get install apt-transport-https
echo 'deb https://dl.bintray.com/openhab/apt-repo2 stable main' | sudo tee /etc/apt/sources.list.d/openhab2.list
apt-get update
apt-get install openhab2
apt-get install openhab2-addons
usermod -G gpio,dialout openhab
raspi-gpio set 04 op dl
echo "EXTRA_JAVA_OPTS="-Dgnu.io.rxtx.SerialPorts=/dev/ttyS0"" >> /etc/default/openhab2
echo "USER_AND_GROUP=root:root" >> /etc/default/openhab2
# change openhab to root in /usr/lib/systemd/system/openhab2.service
# add permissions in /usr/lib/jvm/java-8-oracle/jre/lib/security/java.policy
#        permission jdk.dio.gpio.GPIOPinPermission ":4";
#        permission jdk.dio.gpio.GPIOPinPermission "0:4";
#        permission jdk.dio.DeviceMgmtPermission "GPIO4:4", "open";
# copy libdio.so to /usr/lib/jvm/java-8-oracle/jre/lib/arm

