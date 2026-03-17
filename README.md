# AutoSnap

**AutoSnap** is an Android mobile application developed as part of a Bachelor's engineering thesis (B.Sc. Thesis). Its primary objective is to recognize car models from user-captured photos, integrating a gamification system (earning points and unlocking achievements) with spatial tracking of discovered vehicles. Plotting these locations generates density buffers, enabling comprehensive urban traffic analysis. By monitoring the distribution of vehicle types across different city districts, the collected data can inform infrastructure planning-such as identifying optimal locations for EV charging stations in areas with a high density of electric vehicles.

## Key Features

* **Vehicle Recognition (AI):** The application analyzes a captured photo and automatically identifies the car's make and model.
* **Gamification (Points and Achievements):** Users earn points for every "hunted" car. Rarer models yield higher scores based on a dynamic point list fetched from the cloud. Upon reaching consecutive point thresholds, achievements are unlocked.
* **Location Tracking (GPS):** Every successful recognition records the exact geographical coordinates (latitude and longitude) of the place where the photo was taken. The point is plotted on a map discretized into clusters. A density buffer is generated based on the number of points within a cluster.
* **Cloud Synchronization:** Data regarding points, achievements, and vehicle locations are continuously synchronized with an external cloud database.
* **Google Integration:** Seamless integration with Google services.

## Technologies and Architecture

The application was built using modern tools and libraries within the Android ecosystem:

* **Language:** Kotlin
* **UI:** Jetpack Compose (handling permissions, ActivityResultContracts)
* **Local Database:** SQLite (`SqlLiteHelper`) - storing photos (as byte arrays) for prototype case.
* **Cloud Database:** Firebase Firestore - storing the global point values for specific car models and saving vehicle location data.
* **Location Services:** Google Play Services (`FusedLocationProviderClient`) - fetching precise, current GPS locations.

## Firebase Database Structure (Firestore)

The application utilizes Firebase Firestore to manage for example the scoring logic. The structure is as follows:

* **Collection:** `AutoSnap`
  * **Document:** `Cars`
    * **Fields:** A key-value map where the key is the model name (e.g., `ToyotaCorolla`), and the value is the number of points (e.g., `15`).

Thanks to this approach, adding new car models and modifying their point values is handled server-side, without requiring an app update.

## About the Project

This project is developed as a Bachelor's engineering thesis (B.Sc. Thesis).
