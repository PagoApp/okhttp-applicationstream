# OkHttp Application Stream

A Kotlin library that processes server-sent events or streaming HTTP responses in a line-by-line JSON format using OkHttp.

## Features

- Process streaming HTTP responses line by line
- Automatically parse JSON objects from each line
- Handle incomplete JSON objects across chunks
- Return parsed objects as a Kotlin Flow
- Built with OkHttp, Gson, and Kotlin Coroutines
- Support for both GET and POST requests

## Installation

Add the library to your project's dependencies:

```kotlin
dependencies {
    implementation("app.pago:okhttp-applicationstream:1.1.0")
}
```

## Usage

```kotlin
// Create an instance with your base URL
val service = ApplicationStreamService("https://api.example.com")

// GET request: Get a flow of typed objects from a streaming endpoint
val responseType = object : TypeToken<MyDataType>() {}
val dataFlow: Flow<MyDataType> = service.get("stream/endpoint", responseType)

// POST request: Send data and get a flow of typed objects from a streaming endpoint
val requestBody = MyRequestType(param1 = "value", param2 = 123)
val postFlow: Flow<MyResponseType> = service.post(
    "stream/endpoint", 
    requestBody, 
    object : TypeToken<MyResponseType>() {}
)

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
