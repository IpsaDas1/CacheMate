# CacheMate

This project provides an efficient image caching library for Android applications, with support for both in-memory and disk-based caching. It demonstrates how to integrate this caching mechanism with Jetpack Compose for modern UI development.

## Features

- **In-Memory Caching**: Uses `LruCache` for fast and efficient memory management.
- **Disk Caching**: Persists cached images on the device's storage.
- **Image Resizing**: Optimizes memory usage by resizing images.
- **Coroutines for Async Operations**: Ensures smooth, non-blocking image loading.
- **Compose Integration**: Easy integration with Jetpack Compose using custom composables.

## Components

### 1. `ImageCache`
A singleton class that provides:
- **Memory Cache**: Uses `LruCache` for in-memory storage of images.
- **Disk Cache**: Stores images on disk for reuse across app sessions.
- **Image Downloading**: Fetches images from URLs using `HttpURLConnection`.
- **Drawable Loading**: Loads and caches drawable resources from the app.

### 2. `MainActivity`
The entry point of the app, showcasing:
- How to use the `ImageCache` library in Jetpack Compose.
- Examples for loading images from resources or URLs.

## Installation

1. Clone the repository:
   ```bash
   git clone <https://github.com/IpsaDas1/CacheMate.git>

