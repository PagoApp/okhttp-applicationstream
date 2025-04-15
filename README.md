# OkHttp Application Stream

A Kotlin library that processes server-sent events or streaming HTTP responses in a line-by-line JSON format using OkHttp.

## Features

- Process streaming HTTP responses line by line
- Automatically parse JSON objects from each line
- Handle incomplete JSON objects across chunks
- Return parsed objects as a Kotlin Flow
- Built with OkHttp, Gson, and Kotlin Coroutines

## Installation

Add the library to your project's dependencies:

```kotlin
dependencies {
    implementation("app.pago:okhttp-applicationstream:1.0.0")
}
```

## Usage

```kotlin
// Create an instance with your base URL
val service = ApplicationStreamService("https://api.example.com")

// Get a flow of typed objects from a streaming endpoint
val dataFlow: Flow<MyDataType> = service.get("stream/endpoint", MyDataType::class.java)

// Process the flow in a coroutine scope
lifecycleScope.launch {
    dataFlow.collect { data ->
        // Process each data object as it arrives
    }
}
```

## Requirements

- Java 17+
- OkHttp 4.10.0
- Gson 2.13.0
- Kotlin Coroutines 1.8.1
