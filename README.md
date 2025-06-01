# `ObdGraphs` is an Android application intended to collect and display vehicle telemetry.

![CI](https://github.com/tzebrowski/ObdGraphs/actions/workflows/build.yml/badge.svg)

## About

The `ObdGraphs` Android application, is a feature-rich tool designed for real-time vehicle telemetry visualization using `ELM327` and `STNxxxx` OBD2 adapters.
Built atop the [ObdMetrics](https://github.com/tzebrowski/ObdMetrics "ObdMetrics") Java framework, it offers a comprehensive suite of functionalities tailored for automotive diagnostics and performance monitoring.
It has native support for the Alfa Romeo engines like `1.75 TBI` and `2.0 GME`.
Application offers integration with `Android Auto` and it is available in google play https://play.google.com/store/apps/details?id=org.obd.graphs.my.giulia

## Template App
This application can serve as a template to support various vehicles, each with its own set of custom PIDs. 
Example for such use-case  can be found [here](https://github.com/tzebrowski/ObdGraphs/compare/master...feat/custom_pids_example "custom pids")


## Key Features

* `1Vehicle Support:` Native compatibility with Alfa Romeo engines such as the 1.75 TBI and 2.0 GME, commonly found in models like the 4C, Giulietta, Giulia, and Stelvio.
* `Alerting:` Ability to set upper and lower threshold for individual metrics
* `Adapter Compatibility:` Supports various connection types including Wi-Fi, Bluetooth, and USB for ELM327 and STN1170/STN2120 adapters.
* `Android Auto Integration:` Provides dedicated dashboards for performance metrics and trip information, enhancing the driving experience.
* `Data Logging:` Automatically records trip data, enabling users to review historical telemetry at any time
* `Configurable views` -  The application offers few different kind of gauges which can visualize telemetry in the real-time, e.g: Boost, MAF, OIL temp
* `Vehicle Profiles:` Allows creation of customizable profiles with vehicle-specific settings and PID configurations.

##  Developer-Friendly Aspects
* `Open Source:` Licensed under Apache 2.0, encouraging community contributions and modifications.
* `Modular Architecture:` The codebase is organized into distinct modules (e.g., datalogger, screen_renderer, automotive), promoting maintainability and scalability.
* `Custom PID Integration:` Supports the addition of new PIDs through external JSON configurations, enabling adaptability to various vehicle models.


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