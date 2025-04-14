# autoclicker
open-source hardware autoclicker

### enable ssh
```
sudo raspi-config
3 Interface Options
11 SSH
<Yes>
<Ok>
<Finish>
```
ssh raspberrypi.local

### java
```
sudo apt update
sudo apt install openjdk-17-jdk
```

### create hid_gadget_test
manually copy hid_gadget_test.c from https://www.kernel.org/doc/html/v5.4/usb/gadget_hid.html
```bash
gcc hid_gadget_test.c -o hid_gadget_test
```

### remove symlink
```
ls -l /dev/gpiochip*
sudo rm /dev/gpiochip4
```

### enable linux hid

/boot/firmware/config.txt
```
...
dtoverlay=dwc2
```

### autostart
/etc/rc.local
```
#!/bin/sh -e

cd /home/user
sh autoclicker.sh
```

```
sudo chmod a+x /etc/rc.local
```
