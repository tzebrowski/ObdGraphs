# `ObdGraphs` is an Android application intended to collect and display vehicle telemetry.

![CI](https://github.com/tzebrowski/ObdGraphs/actions/workflows/build.yml/badge.svg)

## About

The `ObdGraphs` Android application, is a feature-rich tool designed for real-time vehicle telemetry visualization using `ELM327` and `STNxxxx` OBD2 adapters.
Built atop the [ObdMetrics](https://github.com/tzebrowski/ObdMetrics "ObdMetrics") Java framework, it offers a comprehensive suite of functionalities tailored for automotive diagnostics and performance monitoring.

---

## 🚀 Key Features and Capabilities

* **Broad Adapter Compatibility:** Seamlessly connects with **ELM327** and **STNxxxx** OBD2 adapters via Bluetooth, WiFi, or USB.
* **Real-Time Data Visualization:** Transforms raw sensor data into intuitive, customizable gauges and high-frequency graphs for instant analysis.
* **Native Engine Support:** Features specialized, native support for high-performance engines, specifically optimized for:
    * **Alfa Romeo 1.75 TBI** - 4C, Giulietta
    * **Alfa Romeo 2.0 GME** -  Giulia, and Stelvio
* **Android Auto Integration:** Extends the diagnostic experience directly to your vehicle’s infotainment screen, allowing you to monitor critical engine parameters safely while on the road.
* **Alerting:** Ability to set upper and lower threshold for individual metrics
* **Adapter Compatibility:** Supports various connection types including Wi-Fi, Bluetooth, and USB for ELM327 and STN1170/STN2120 adapters.
* **Android Auto Integration:** Provides dedicated dashboards for performance metrics and trip information, enhancing the driving experience.
* **Data Logging:** Automatically records trip data, enabling users to review historical telemetry at any time
* **Configurable views** -  The application offers few different kind of gauges which can visualize telemetry in the real-time, e.g: Boost, MAF, OIL temp
* **Vehicle Profiles:** Allows creation of customizable profiles with vehicle-specific settings and PID configurations.

---

## 📊 Data Analysis & Logging

The application includes native integration with the **ObdGraphsLogViewer** project. This integration allows users to:
* **Export Telemetry Logs:** Save detailed session data from the mobile app in the Google Drive.
* **Online Log Visualization:** Utilize the specialized [ObdGraphsLogViewer](https://github.com/tzebrowski/ObdGraphsLogViewer) to analyze logs on a larger screen, enabling deep-dive performance reviews and trend analysis.


### 🔄 How to Sync Data between Mobile App and Log Viewer

To analyze your vehicle's performance on a larger screen, follow these steps to sync your data:

1.  **Enable Recording:** Within the ObdGraphs mobile app, ensure that data logging is active during your drive. The app automatically saves trip data to your local storage.
2.  **Upload to Cloud:** Navigate to the synchronization settings in the app and use the **Cloud Synchronization** feature to upload your recorded trip logs directly to your **Google Drive**.
3.  **Launch Analyzer:** Visit the [Online Log Analyzer](https://my-giulia.com/#analyzer) on your desktop.
4.  **Analyze:** Connect your Google Drive account within the web analyzer to instantly access and visualize your uploaded logs.

For a detailed video walkthrough of this process, refer to the official tutorial: [ObdGraphs - Online Log Analyzer (YouTube)](https://www.youtube.com/watch?v=SJe4QkUgMKs).

---
## 🔗 Official Resources

| Resource | Link |
| :--- | :--- |
| **Official Website** | [https://my-giulia.com/](https://my-giulia.com/) |
| **Online Log Analyzer** | [my-giulia.com/#analyzer](https://my-giulia.com/#analyzer) |
| **Google Play Store** | [Download ObdGraphs](https://play.google.com/store/apps/details?id=org.obd.graphs.my.giulia) |
| **GitHub (Log Viewer)** | [ObdGraphsLogViewer Project](https://github.com/tzebrowski/ObdGraphsLogViewer) |

> **Note:** For the best performance and highest data sampling rates, the use of high-quality **STNxxxx** based adapters is recommended.


## Template App
This application can serve as a template to support various vehicles, each with its own set of custom PIDs. 
Example for such use-case  can be found [here](https://github.com/tzebrowski/ObdGraphs/compare/master...feat/custom_pids_example "custom pids")

##  Developer-Friendly Aspects
* `Open Source:` Licensed under Apache 2.0, encouraging community contributions and modifications.
* `Modular Architecture:` The codebase is organized into distinct modules (e.g., datalogger, screen_renderer, automotive), promoting maintainability and scalability.
* `Custom PID Integration:` Supports the addition of new PIDs through external JSON configurations, enabling adaptability to various vehicle models.


### YouTube overview video (brake-boosting assistance)

[![MyGiulia AA on YouTube](https://img.youtube.com/vi/dYNOtf7SPDk/0.jpg)](https://youtube.com/shorts/dYNOtf7SPDk?feature=share "Brake-Boosting Assistance")

## Views

Application offers few configurable screens which visualize vehicle telemetry data.

### Android Auto (alerts emitted when upper threshold breached)
|                                                     |                                                     |
|-----------------------------------------------------|-----------------------------------------------------|
| ![Alt text](./res/aa_8.jpg?raw=true "Android Auto") |

### Android Auto (Performance Dashboard)
|                                                     |                                                     |
|-----------------------------------------------------|-----------------------------------------------------|
| ![Alt text](./res/aa_7.jpg?raw=true "Android Auto") |


### Android Auto (Trip Info Dashboard)
|                                                     |                                                     |
|-----------------------------------------------------|-----------------------------------------------------|
| ![Alt text](./res/aa_6.jpg?raw=true "Android Auto") | 



### Android Auto (Giulia Renderer)
|                                                     |                                                     |
|-----------------------------------------------------|-----------------------------------------------------|
| ![Alt text](./res/aa_3.jpg?raw=true "Android Auto") | ![Alt text](./res/aa_2.jpg?raw=true "Android Auto") | 


### Android Auto (Gauge Renderer)
|                                                     |                                                     |
|-----------------------------------------------------|-----------------------------------------------------|
| ![Alt text](./res/aa_4.jpg?raw=true "Android Auto") | ![Alt text](./res/aa_5.jpg?raw=true "Android Auto") | 




### Mobile

|                                                                   |                                            |
|-------------------------------------------------------------------|--------------------------------------------|
| ![Alt text](./res/Screenshot_phone_2.png?raw=true "") | ![Alt text](./res/Screenshot_phone_1.png?raw=true "")  | 
| ![Alt text](./res/Screenshot_8.png?raw=true "") | ![Alt text](./res/Screenshot_phone_4.png?raw=true "")  |

### Tablet
|                                                      |                                                      |
|------------------------------------------------------|------------------------------------------------------|
| ![Alt text](./res/Screenshot_3.png?raw=true "Gauge") | ![Alt text](./res/Screenshot_6.png?raw=true "Graph") | 


## Road map
* Performance optimization
* Support for PIDs creation through application

## Instructions

* [Adding new PIDs to query and displaying on AA virtual screen](doc/guides/query_new_pid/query_new_pid.md)
* [Change vehicle profile](doc/guides/change_vehicle_profile/change_vehicle_profile.md)