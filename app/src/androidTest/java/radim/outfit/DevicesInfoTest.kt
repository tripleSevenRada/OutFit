package radim.outfit

import com.garmin.android.connectiq.IQDevice

//test

val device1 = IQDevice(1,"device1")
val status1 = IQDevice.IQDeviceStatus.CONNECTED
//onDeviceEvent(device1,status1)

val device2 = IQDevice(2,"device2")
val status2 = IQDevice.IQDeviceStatus.CONNECTED
//onDeviceEvent(device2,status2)

val device3 = IQDevice(3,"device3")
val status3 = IQDevice.IQDeviceStatus.CONNECTED
//onDeviceEvent(device3,status3)

val device4 = IQDevice(4,"device4")
val status4 = IQDevice.IQDeviceStatus.CONNECTED
//onDeviceEvent(device4,status4)

val device5 = IQDevice(5,"device5")
val status5 = IQDevice.IQDeviceStatus.CONNECTED
//onDeviceEvent(device5,status5)
//Thread.sleep(1000)
val device6 = IQDevice(5,"device5")
val status6 = IQDevice.IQDeviceStatus.NOT_CONNECTED
//onDeviceEvent(device6,status6)