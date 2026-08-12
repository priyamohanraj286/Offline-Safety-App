# Offline Safety App

**Offline emergency SOS communication using Bluetooth Low Energy (BLE), Android, Room Database and Jetpack Compose.**

## Overview

The Offline Safety App is an Android application designed to provide emergency SOS communication between nearby devices without requiring mobile data or an internet connection. One device broadcasts an SOS message using Bluetooth Low Energy while nearby devices continuously scan for the application's SOS packets.

A foreground Android service maintains background BLE monitoring so that SOS alerts can be detected even when the application is not visible. When a valid SOS packet is detected, the receiver provides an alert notification and audible SOS sound.

## Objectives

* Provide an emergency SOS mechanism without internet dependency.
* Enable nearby Android devices to exchange SOS alerts using BLE.
* Support background SOS scanning.
* Provide audible and notification-based alerts.
* Store user and travel information locally.
* Provide a simple, professional emergency-oriented interface.
* Distinguish application SOS packets from unrelated BLE advertisements.

## Key Features

### SOS Broadcasting

* Large SOS button for immediate activation.
* BLE advertising of a compact SOS message.
* User details are checked before an SOS can be sent.
* SOS can be stopped from the same interface.

### Automatic Background Scanning

* BLE scanning starts automatically after travel details are saved.
* No separate start/stop scanning controls are required during normal use.
* A foreground service maintains monitoring in the background.
* A detected SOS triggers an audible alert and notification.

### Local User and Travel Data

* User name and emergency contact are stored locally.
* Travel information includes train name, coach and seat.
* Room Database provides local persistence.
* Active travel information is used to control the scanning lifecycle.

### BLE Packet Identification

The application uses a dedicated Bluetooth manufacturer identifier so the scanner can distinguish its SOS packets from unrelated BLE advertisements.

## System Architecture


                 Android Application
                        |
              +---------+---------+
              |                   |
        Jetpack Compose       Room Database
              |                   |
        MainActivity       User / Travel DAOs
              |                   |
              +---------+---------+
                        |
                   SosService
                 /             \\
                /               \\
        BLE Scanner          BLE Advertiser
             |                    |
       SOS Detection          SOS Broadcast
             |
      Notification + Sound


## SOS Communication Flow

text
Sender Device
     |
     | Press SOS
     v
SosService
     |
     | BLE Advertising
     v
Nearby Devices
     |
     | BLE Scan
     v
Manufacturer ID Check
     |
     | Valid SOS packet
     v
SOS Alert
     |
     +----> Notification
     |
     +----> Alert Sound


## Technology Stack

|Technology|Purpose|
|-|-|
|Kotlin|Application programming language|
|Android|Application platform|
|Jetpack Compose|Declarative UI|
|Material 3|UI components and styling|
|Bluetooth Low Energy|Offline device-to-device communication|
|BluetoothLeScanner|Detecting BLE advertisements|
|BluetoothLeAdvertiser|Broadcasting SOS advertisements|
|Android Foreground Service|Background SOS operation|
|Room Database|Local persistence|
|SQLite|Local database engine used by Room|
|Kotlin Coroutines|Asynchronous database/background operations|
|Gradle Kotlin DSL|Build and dependency configuration|

## Major Components

**MainActivity.kt**  
Controls the main SOS interface, Bluetooth permission requests, travel handling and service activation.

**SosService.kt**  
Core communication component. Handles BLE scanning, BLE advertising, foreground execution, SOS packet processing and alert sound.

**AppDatabase.kt**  
Creates and manages the Room database instance.

**User.kt / Travel.kt**  
Room entities representing locally stored user and travel information.

**UserDao.kt / TravelDao.kt**  
Provide database operations through Room.

**SetupActivity.kt**  
Collects and stores user information.

**ViewUserActivity.kt**  
Displays saved user and travel information.

**AndroidManifest.xml**  
Declares application components, permissions and required Android features.



## Background Operation

A foreground Android service is used because normal Activity lifecycle execution cannot guarantee continuous monitoring after the UI moves to the background. The service maintains BLE scanning/advertising and presents an ongoing system notification while the operation is active.

Actual background behavior can still be affected by Android version, device manufacturer restrictions and battery optimization policies.

Recommended testing uses at least two BLE-capable Android devices.

**SOS broadcast:** Device A presses SOS → BLE advertisement → Device B detects SOS → alert sound/notification.

**Stop SOS:** Device A stops SOS → advertising stops → Device B no longer receives new SOS packets from Device A.

**Automatic scanning:** Travel details are saved → service starts → BLE scanning begins automatically.

**Background detection:** Scanning is active → application moves to background/screen is turned off → foreground service continues monitoring → nearby SOS is detected.

## Running the Project

1. Clone the repository.
2. Open the project in Android Studio.
3. Allow Gradle synchronization.
4. Connect a BLE-capable Android device.
5. Grant the required Bluetooth permissions.
6. Build and run the application.
7. For SOS communication testing, use at least two compatible Android devices.



