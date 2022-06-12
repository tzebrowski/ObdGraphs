# `ObdGraphs` is an Android application which is intended to collect and display OBD related metrics.

![CI](https://github.com/tzebrowski/ObdGraphs/actions/workflows/build.yml/badge.svg)

## About

`ObdGraphs` is an Android application with graphical interface intended to collect and display OBD
related metrics. It is build on top of `ObdMetrics` library and it visualize OBD metrics that are
generated while vehicle is driving.

## Features

* Trips - the application associate all metrics collected during the session to the `trip`.
  `Trip` is stored on the external storage. At any time user can open already saved `trip` and check
  telemetry collected while vehicle was driving in the past
* Real-time metrics visualization - the application uses multiple performance techniques to collect
  and display vehicle telemetry data as fast as possible
* Profiles - the application allows binding all configurable preferences to the distinct profile.
  User can select different profile at any time and load specified preferences like: displayed
  views, collected metrics, protocol settings (can speed, headers)
  At this moment five fully configurable profiles are allowed.

## Views

Application offers multiple configurable screens which visualize vehicle telemetry data.

### Graph View

|      |      |
| ---- | ---- |
|   ![Alt text](./res/Screenshot_2.png?raw=true "Graph view")   | ![Alt text](./res/Screenshot_6.png?raw=true "Graph view") |

### Gauge View

|      |      |
| ---- | ---- |
|    ![Alt text](./res/Screenshot_3.png?raw=true "Gauge view")  | ![Alt text](./res/Screenshot_5.png?raw=true "Gauge view") |

### Tiles View

|      |      |
| ---- | ---- |
|     ![Alt text](./res/Screenshot_1.png?raw=true "Dashboard view") |![Alt text](./res/Screenshot_7.png?raw=true "Tiles view") |

### Raw Data View

|      |      |
| ---- | ---- |
|    ![Alt text](./res/Screenshot_4.png?raw=true "Gauge view")  | |

## Road map

* Performance optimization
* Features
    * Profiles - there should be possible to assign vehicle specified settings (protocol, can,
      headers, PIDs) to the configurable profile
    * Loading external PID definition file - there should be possible to load PID from the external
      place
    * Publishing trips to the cloud - there should be possible to publish collected trip to the
      cloud backend service
* Release to google play