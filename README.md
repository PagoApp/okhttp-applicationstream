# OkHttp Application Stream

A Kotlin library that processes server-sent events or streaming HTTP responses in a line-by-line JSON format using OkHttp.

## Features

- Process streaming HTTP responses line by line
- Automatically parse JSON objects from each line
- Handle incomplete JSON objects across chunks
- Return parsed objects as a Kotlin Flow
- Built with OkHttp, Gson, and Kotlin Coroutines
- Support for both GET and POST requests
- Configurable OkHttpClient and buffer size

## Installation

Add the library to your project's dependencies:

```kotlin
dependencies {
    implementation("app.pago:okhttp-applicationstream:1.1.0")
}
```

## Usage

### Basic Usage

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

### Advanced Configuration

You can customize the service with additional configuration options:

```kotlin
// Create a custom OkHttpClient
val okHttpClient = OkHttpClient.Builder()
    .readTimeout(30, TimeUnit.MINUTES)
    .writeTimeout(30, TimeUnit.MINUTES)
    .callTimeout(30, TimeUnit.MINUTES)
    .connectTimeout(30, TimeUnit.MINUTES)
    .addInterceptor(yourCustomInterceptor)
    .build()

// Configure the service with custom options
val config = ApplicationStreamServiceConfig(
    baseUrl = "https://api.example.com",
    client = okHttpClient,
    readBufferSize = 2048 // Default is 1024
)

val service = ApplicationStreamService(config)
```

## Requirements

- Java 17+
- OkHttp 4.10.0
- Gson 2.13.0
- Kotlin Coroutines 1.8.1
