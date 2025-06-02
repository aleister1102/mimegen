# MIME Type Generator Burp Suite Extension

This Burp Suite extension provides utilities for working with MIME types in HTTP messages. It allows users to easily inspect, copy, search, and modify the `Content-Type` header of HTTP requests and responses directly within Burp's various tools.

## Features

*   **Show & Copy Response MIME Type:**
    *   Displays the `Content-Type` of a selected HTTP response.
    *   If the `Content-Type` header is present, its value (without parameters like `charset`) is shown.
    *   If the header is not present or is empty, the extension attempts to use Burp's inferred MIME type.
    *   The determined MIME type is copied to the clipboard.
    *   Accessible from Proxy, Repeater, Target, Logger, Intruder, and Extensions tools.
*   **Search this MIME Type Online:**
    *   Takes the determined MIME type (as per the "Show & Copy" logic) from a selected response.
    *   Opens a new browser tab to perform a Google search for that MIME type.
    *   Useful for quickly looking up information about a specific MIME type.
    *   Accessible from the same tools as "Show & Copy".
*   **Set/Insert Content-Type:**
    *   Allows users to set or insert a `Content-Type` header in the current HTTP request within an editable message editor (e.g., Repeater, Intruder request tabs).
    *   Presents a dialog with a filterable list of common MIME types to choose from.
    *   Updates the request in the editor with the selected `Content-Type`.
    *   Accessible from tools with editable request views.

## Building your extension

Before you begin development or building, make sure that your project's JDK is set to version "21" or higher, as specified in the `build.gradle.kts` file.

To build the JAR file, run the following command in the root directory of this project:

*   For UNIX-based systems: `./gradlew jar`
*   For Windows systems: `gradlew jar`

If successful, the JAR file is saved to `<project_root_directory>/build/libs/mimegen.jar`. (The project name `mimegen` is defined in the [settings.gradle.kts](./settings.gradle.kts) file). If the build fails, errors are shown in the console.

## Loading the JAR file into Burp

To load the JAR file into Burp:

1.  In Burp Suite, go to **Extensions > Installed**.
2.  Click **Add**.
3.  Under **Extension details**, click **Select file**.
4.  Select the `mimegen.jar` file you just built (typically located in `build/libs/`), then click **Open**.
5.  [Optional] Under **Standard output** and **Standard error**, choose where to save output and error messages from the extension.
6.  Click **Next**. The extension is loaded into Burp.
7.  Review any messages displayed in the **Output** and **Errors** tabs (e.g., "MIME Type Generator loaded successfully.").
8.  Click **Close**.

Your extension is now loaded and listed in the **Burp extensions** table. You can test its behavior by right-clicking on HTTP messages in tools like Proxy history or Repeater.

### Reloading the JAR file in Burp

If you make changes to the code, you must rebuild the JAR file and reload your extension in Burp for the changes to take effect.

To rebuild the JAR file, follow the steps for [building the JAR file](#building-the-jar-file).

To quickly reload your extension in Burp:

1.  In Burp, go to **Extensions > Installed**.
2.  Hold `Ctrl` (or `⌘` on macOS), and deselect then reselect the **Loaded** checkbox next to your "MIME Type Generator" extension.

## Development Notes

The main extension logic is in `src/main/java/BurpMimeMontoyaExtension.java`.
The dialog for selecting MIME types is implemented in `src/main/java/MimeTypeSelectionDialog.java`.

This extension uses the Burp Suite Montoya API.

*   For more information on Montoya API features, see the [JavaDoc](https://portswigger.github.io/burp-extensions-montoya-api/javadoc/burp/api/montoya/MontoyaApi.html).
*   To explore example extensions, visit the [PortSwigger GitHub repository](https://github.com/PortSwigger/burp-extensions-montoya-api-examples).
*   For more information on creating extensions, see the official [documentation](https://portswigger.net/burp/documentation/desktop/extend-burp/extensions/creating).