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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Burp Suite Montoya API extension for MIME type related utilities.
 * Provides context menu items to show, copy, search, and set MIME types.
 */
public class BurpMimeMontoyaExtension implements BurpExtension {

    private MontoyaApi montoyaApi;
    private Logging logging;

    private static final String EXTENSION_NAME = "MIME Types Generator";
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
     * Extracts and cleans MIME type from HTTP response
     */
    private String getEffectiveMimeType(HttpResponse response) {
        if (response == null)
            return null;

        // 1. Get Content-Type header value
        String contentType = response.headers().stream()
                .filter(header -> "Content-Type".equalsIgnoreCase(header.name()))
                .map(HttpHeader::value)
                .findFirst()
                .orElse(null);

        // 2. Fallback to inferred type if needed
        if (contentType == null || contentType.isEmpty()) {
            contentType = response.inferredMimeType().toString();
        }

        // 3. Clean parameters from MIME type
        return cleanMimeType(contentType);
    }

    /**
     * Removes parameters from MIME type string
     */
    private String cleanMimeType(String mimeType) {
        if (mimeType == null)
            return null;
        return mimeType.split(";")[0].trim();
    }

    /**
     * Handles response-based operations with consistent error handling
     */
    private String handleResponseOperation(List<HttpRequestResponse> selectedMessages,
            String operationName) {
        if (selectedMessages.isEmpty()) {
            showErrorDialog("No message selected", operationName);
            return null;
        }

        HttpResponse response = selectedMessages.get(0).response();
        if (response == null) {
            showErrorDialog("Message has no response", operationName);
            return null;
        }

        String mimeType = getEffectiveMimeType(response);
        if (mimeType == null || mimeType.isEmpty() || "Unknown".equalsIgnoreCase(mimeType)) {
            showErrorDialog("Could not determine MIME type", operationName);
            return null;
        }

        return mimeType;
    }

    /**
     * Shows standardized error dialog
     */
    private void showErrorDialog(String message, String title) {
        JOptionPane.showMessageDialog(
                montoyaApi.userInterface().swingUtils().suiteFrame(),
                message,
                title,
                JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Handles the "Show & Copy Response MIME Type" action
     */
    private void handleShowCopyMimeType(List<HttpRequestResponse> selectedMessages) {
        String mimeType = handleResponseOperation(selectedMessages, "Show/Copy MIME");
        if (mimeType == null)
            return;

        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection(mimeType), null);

            JOptionPane.showMessageDialog(
                    montoyaApi.userInterface().swingUtils().suiteFrame(),
                    "Copied MIME Type: " + mimeType,
                    EXTENSION_NAME,
                    JOptionPane.INFORMATION_MESSAGE);
            logging.logToOutput("Copied MIME: " + mimeType);
        } catch (Exception e) {
            logging.logToError("Copy failed: " + e.getMessage());
            showErrorDialog("Copy failed: " + e.getMessage(), "Copy Error");
        }
    }

    /**
     * Handles the "Search this MIME Type Online" action
     */
    private void handleSearchMimeTypeOnline(List<HttpRequestResponse> selectedMessages) {
        String mimeType = handleResponseOperation(selectedMessages, "Search MIME");
        if (mimeType == null)
            return;

        try {
            String encoded = URLEncoder.encode(mimeType, StandardCharsets.UTF_8.toString());
            String searchUrl = "https://www.google.com/search?q=" + encoded;

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(searchUrl));
            } else {
                showErrorDialog("Browser not available. Search URL:\n" + searchUrl, "Browser Error");
            }
        } catch (Exception ex) {
            logging.logToError("Search failed: " + ex.getMessage());
            showErrorDialog("Search failed: " + ex.getMessage(), "Search Error");
        }
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
