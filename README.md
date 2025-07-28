# MIME Type Generator Burp Suite Extension

Burp Suite extension for inspecting, copying, searching, and modifying MIME types in HTTP messages.

## Features

- **Show & Copy Response MIME Type**: Right-click response → Show & Copy. Displays and copies cleaned `Content-Type` (falls back to inferred type). Available in Proxy, Repeater, Target, Logger, Intruder, Extensions.
- **Search MIME Type Online**: Right-click response → Search Online. Opens Google search for the MIME type.
- **Set/Insert Content-Type**: Right-click editable request → Set/Insert. Select from filterable MIME list and update request.

## Build

Requires JDK 21+.

- Unix: `./gradlew jar`
- Windows: `gradlew.bat jar`

Output: `build/libs/mimegen.jar`

## Load into Burp

1. Extensions > Installed > Add.
2. Select `mimegen.jar`.
3. Optional: Set output/error logs.
4. Next > Review Output/Errors > Close.

Test: Right-click HTTP messages in Proxy/Repeater.

## Reload

1. Rebuild JAR.
2. Extensions > Installed: Ctrl+deselect/reselect Loaded checkbox for extension.

## Development

- Main: `src/main/java/BurpMimeMontoyaExtension.java`
- Dialog: `src/main/java/MimeTypeSelectionDialog.java`

Uses Montoya API. See [JavaDoc](https://portswigger.github.io/burp-extensions-montoya-api/javadoc/burp/api/montoya/MontoyaApi.html), [Examples](https://github.com/PortSwigger/burp-extensions-montoya-api-examples), [Docs](https://portswigger.net/burp/documentation/desktop/extend-burp/extensions/creating).