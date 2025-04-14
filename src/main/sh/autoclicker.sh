#!/bin/sh

# debug if leads 20 and 21 shorted
gpioget 0 20 > /dev/null # input mode
gpioset 0 21=1 # set 21 high
debug=$(gpioget 0 20) # get 20
gpioget 0 21 > /dev/null # input mode

echo $debug
if [ $debug -gt 0 ]; then
  echo debug
  exit 1 # stop if debug
fi

modprobe dwc2
modprobe libcomposite

cd /sys/kernel/config/usb_gadget/
mkdir m500
cd m500

echo 0 > bDeviceClass
echo 0 > bDeviceSubClass
echo 0 > bDeviceProtocol
echo 8 > bMaxPacketSize0
echo 0x046d > idVendor
echo 0xc069 > idProduct
echo 0x5601 > bcdDevice
echo 0x0200 > bcdUSB

mkdir -p strings/0x409
echo "Logitech" > strings/0x409/manufacturer
echo "USB Laser Mouse" > strings/0x409/product

mkdir -p configs/c.1/strings/0x409
echo 0xa0 > configs/c.1/bmAttributes
echo 98 > configs/c.1/MaxPower # 98 mA

mkdir functions/hid.usb0
echo 1 > functions/hid.usb0/subclass
echo 1 > functions/hid.usb0/protocol
echo 3 > functions/hid.usb0/report_length # 8-byte reports
/usr/bin/printf "\x05\x01\x09\x02\xa1\x01\x09\x01\xa1\x00\x05\x09\x19\x01\x29\x03\x15\x00\x25\x01\x95\x03\x75\x01\x81\x02\x95\x01\x75\x05\x81\x03\x05\x01\x09\x30\x09\x31\x15\x81\x25\x7f\x75\x08\x95\x02\x81\x06\xc0\xc0" > functions/hid.usb0/report_desc

ln -s functions/hid.usb0 configs/c.1

# enable
ls /sys/class/udc > UDC
chmod a+rw /dev/hidg0

cd /home/user
java -jar autoclicker-1.0-jar-with-dependencies.jar
