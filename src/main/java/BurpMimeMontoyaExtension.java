import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.http.message.requests.HttpRequest; // Added for request modification
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse; // For editor interactions

import javax.swing.*;
import javax.swing.JMenuItem; // Explicit import for JMenuItem
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.net.URI;
import java.awt.Desktop;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.awt.Component;
import java.awt.Frame; // Added for Dialog parent

/**
 * Burp Suite Montoya API extension for MIME type related utilities.
 * Provides context menu items to show, copy, search, and set MIME types.
 */
public class BurpMimeMontoyaExtension implements BurpExtension {

    private MontoyaApi montoyaApi;
    private Logging logging;

    private static final String EXTENSION_NAME = "MIME Type Generator";
    private static final String MENU_ITEM_TEXT_SHOW_COPY = "Show & Copy Response MIME Type";
    private static final String MENU_ITEM_TEXT_SEARCH_ONLINE = "Search this MIME Type Online";
    private static final String MENU_ITEM_TEXT_SET_MIME = "Set/Insert Content-Type";

    /**
     * Initializes the extension.
     * Sets the extension name and registers the context menu items provider.
     * 
     * @param api The Montoya API.
     */
    @Override
    public void initialize(MontoyaApi api) {
        this.montoyaApi = api;
        this.logging = api.logging();

        montoyaApi.extension().setName(EXTENSION_NAME);
        montoyaApi.userInterface().registerContextMenuItemsProvider(new MimeContextMenuItemsProvider());

        logging.logToOutput(EXTENSION_NAME + " loaded successfully.");
        logging.logToOutput("Right-click on an HTTP message to use MIME type utilities.");
    }

    /**
     * Provides context menu items for MIME type operations.
     */
    class MimeContextMenuItemsProvider implements ContextMenuItemsProvider {
        /**
         * Provides a list of menu items based on the context menu event.
         * 
         * @param creationContext The context menu event.
         * @return A list of {@link Component} menu items, or null if no items are
         *         applicable.
         */
        @Override
        public List<Component> provideMenuItems(ContextMenuEvent creationContext) {
            List<ToolType> supportedTools = Arrays.asList(
                    ToolType.PROXY, ToolType.REPEATER, ToolType.TARGET,
                    ToolType.LOGGER, ToolType.INTRUDER, ToolType.EXTENSIONS);

            if (!supportedTools.contains(creationContext.toolType())) {
                return null; // Not a supported tool
            }

            List<Component> menuList = new ArrayList<>();

            // "Show & Copy" and "Search Online" operate on selected responses from a list
            if (!creationContext.selectedRequestResponses().isEmpty()) {
                JMenuItem showCopyMimeItem = new JMenuItem(MENU_ITEM_TEXT_SHOW_COPY);
                showCopyMimeItem
                        .addActionListener(e -> handleShowCopyMimeType(creationContext.selectedRequestResponses()));
                menuList.add(showCopyMimeItem);

                JMenuItem searchOnlineMimeItem = new JMenuItem(MENU_ITEM_TEXT_SEARCH_ONLINE);
                searchOnlineMimeItem
                        .addActionListener(e -> handleSearchMimeTypeOnline(creationContext.selectedRequestResponses()));
                menuList.add(searchOnlineMimeItem);
            }

            // "Set/Insert Content-Type" operates on an editable request view
            if (creationContext.messageEditorRequestResponse().isPresent()) {
                if (!menuList.isEmpty()) {
                    menuList.add(new JSeparator()); // Add separator if other items exist
                }
                JMenuItem setMimeItem = new JMenuItem(MENU_ITEM_TEXT_SET_MIME);
                setMimeItem.addActionListener(e -> handleSetMimeType(creationContext));
                menuList.add(setMimeItem);
            }

            return menuList.isEmpty() ? null : menuList;
        }
    }

    /**
     * Handles the "Show & Copy Response MIME Type" action.
     * Displays the response MIME type and copies it to the clipboard.
     * 
     * @param selectedMessages The list of selected HTTP request/response pairs.
     */
    private void handleShowCopyMimeType(List<HttpRequestResponse> selectedMessages) {
        HttpRequestResponse selectedMessage = selectedMessages.get(0);
        HttpResponse response = selectedMessage.response();

        if (response == null) {
            JOptionPane.showMessageDialog(montoyaApi.userInterface().swingUtils().suiteFrame(),
                    "This message has no response.",
                    EXTENSION_NAME,
                    JOptionPane.WARNING_MESSAGE);
            logging.logToOutput("Show/Copy MIME: No response in selected message.");
            return;
        }

        String contentType = getEffectiveMimeType(response);

        if (contentType == null || contentType.isEmpty() || contentType.equalsIgnoreCase("Unknown")) {
            JOptionPane.showMessageDialog(montoyaApi.userInterface().swingUtils().suiteFrame(),
                    "Could not determine a valid MIME type to copy.",
                    EXTENSION_NAME,
                    JOptionPane.INFORMATION_MESSAGE);
            logging.logToOutput("Show/Copy MIME: MIME type is empty or unknown after checks.");
            return;
        }

        final String finalContentType = contentType; // Effectively final for lambda

        try {
            StringSelection stringSelection = new StringSelection(finalContentType);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);

            // Use JOptionPane for feedback, similar to other parts of the extension
            JOptionPane.showMessageDialog(montoyaApi.userInterface().swingUtils().suiteFrame(),
                    "MIME Type copied: " + finalContentType,
                    EXTENSION_NAME,
                    JOptionPane.INFORMATION_MESSAGE);
            logging.logToOutput("Copied MIME Type: " + finalContentType);

        } catch (Exception e) {
            logging.logToError("Error copying MIME type to clipboard: " + e.getMessage());
            JOptionPane.showMessageDialog(montoyaApi.userInterface().swingUtils().suiteFrame(),
                    "Error copying to clipboard: " + e.getMessage(),
                    EXTENSION_NAME,
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Handles the "Search this MIME Type Online" action.
     * Opens a web browser to search for the response MIME type.
     * 
     * @param selectedMessages The list of selected HTTP request/response pairs.
     */
    private void handleSearchMimeTypeOnline(List<HttpRequestResponse> selectedMessages) {
        HttpRequestResponse selectedMessage = selectedMessages.get(0);
        HttpResponse response = selectedMessage.response();

        if (response == null) {
            JOptionPane.showMessageDialog(montoyaApi.userInterface().swingUtils().suiteFrame(),
                    "This message has no response.",
                    EXTENSION_NAME,
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String contentType = getEffectiveMimeType(response);

        if (contentType == null || contentType.isEmpty() || contentType.equalsIgnoreCase("Unknown")) {
            JOptionPane.showMessageDialog(montoyaApi.userInterface().swingUtils().suiteFrame(),
                    "Could not determine a valid MIME type to search.",
                    EXTENSION_NAME,
                    JOptionPane.INFORMATION_MESSAGE);
            logging.logToOutput("Search MIME: MIME type is empty or unknown after checks.");
            return;
        }

        try {
            String encodedContentType = java.net.URLEncoder.encode(contentType,
                    java.nio.charset.StandardCharsets.UTF_8.toString());
            String searchUrl = "https://www.google.com/search?q=" + encodedContentType;

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(searchUrl));
                logging.logToOutput("Opening browser to search for MIME Type: " + contentType);
            } else {
                JOptionPane.showMessageDialog(montoyaApi.userInterface().swingUtils().suiteFrame(),
                        "Could not open browser automatically. Search URL:\n" + searchUrl,
                        EXTENSION_NAME,
                        JOptionPane.WARNING_MESSAGE);
                logging.logToOutput("Could not open browser automatically. URL to search: " + searchUrl);
            }
        } catch (Exception ex) {
            logging.logToError("Error trying to open browser for MIME type search: " + ex.getMessage());
            JOptionPane.showMessageDialog(montoyaApi.userInterface().swingUtils().suiteFrame(),
                    "Error trying to open browser: " + ex.getMessage(),
                    EXTENSION_NAME,
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Extracts the effective MIME type from an HTTP response.
     * It first checks the "Content-Type" header. If not present or empty,
     * it falls back to the inferred MIME type. Parameters like "; charset=..." are
     * removed.
     *
     * @param response The {@link HttpResponse} to extract the MIME type from.
     * @return The determined MIME type string (e.g., "application/json"),
     *         or null if no MIME type could be reliably determined.
     *         It may return "Unknown" if that's what the inference provides and no
     *         header is set.
     */
    private String getEffectiveMimeType(HttpResponse response) {
        if (response == null) {
            return null;
        }

        String contentType = null;
        // 1. Try Content-Type header
        for (HttpHeader header : response.headers()) {
            if (header.name().equalsIgnoreCase("Content-Type")) {
                contentType = header.value();
                break;
            }
        }

        // 2. If no header, try inferred MIME type
        if (contentType == null || contentType.isEmpty()) {
            contentType = response.inferredMimeType().toString();
        }

        // 3. Clean up and remove parameters
        if (contentType != null && !contentType.isEmpty()) {
            if (contentType.contains(";")) {
                contentType = contentType.split(";")[0].trim();
            }
            // Ensure "Unknown" is handled consistently if it's the result after stripping
            // parameters.
            // However, if the original inference was "Unknown" and it's the only source, we
            // keep it.
            // If it was something like "Unknown; charset=utf-8", it becomes "Unknown".
            // If it's an empty string after stripping, treat as undetermined.
            if (contentType.trim().isEmpty()) {
                return null; // Or "Unknown" depending on desired strictness. Null seems more accurate if it
                             // became empty.
            }
            return contentType.trim();
        }

        return null; // No MIME type could be determined
    }

    /**
     * Handles the "Set/Insert Content-Type" action.
     * Opens a dialog to select a MIME type and sets/inserts it into the request's
     * Content-Type header.
     * 
     * @param creationContext The context menu event, providing access to the
     *                        message editor.
     */
    private void handleSetMimeType(ContextMenuEvent creationContext) {
        // This check is now more of a safeguard, as provideMenuItems should ensure
        // presence.
        if (!creationContext.messageEditorRequestResponse().isPresent()) {
            logging.logToOutput("handleSetMimeType called without a message editor context.");
            JOptionPane.showMessageDialog(montoyaApi.userInterface().swingUtils().suiteFrame(),
                    "This action requires an editable message editor context.",
                    EXTENSION_NAME,
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        MessageEditorHttpRequestResponse editorRequestResponse = creationContext.messageEditorRequestResponse().get();
        HttpRequest request = editorRequestResponse.requestResponse().request(); // Get the current request from the
                                                                                 // editor's content

        if (request == null) {
            JOptionPane.showMessageDialog(montoyaApi.userInterface().swingUtils().suiteFrame(),
                    "Cannot set MIME type: No request is currently available in the editor.",
                    EXTENSION_NAME,
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        MimeTypeSelectionDialog dialog = new MimeTypeSelectionDialog(
                (Frame) montoyaApi.userInterface().swingUtils().suiteFrame());
        dialog.setVisible(true);
        String selectedMimeType = dialog.getSelectedMimeType();

        if (selectedMimeType != null && !selectedMimeType.isEmpty()) {
            HttpHeader newContentTypeHeader = HttpHeader.httpHeader("Content-Type", selectedMimeType);
            HttpRequest modifiedRequest = request.withHeader(newContentTypeHeader);

            editorRequestResponse.setRequest(modifiedRequest); // Update the editor content
            logging.logToOutput("Content-Type set to: " + selectedMimeType);
        } else {
            logging.logToOutput("Set Content-Type operation cancelled by user.");
        }
    }
}
