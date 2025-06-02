// package com.example.burp; // You can change this package if you wish

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.http.message.requests.HttpRequest; // Added for request modification
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.logging.Logging;
// import burp.api.montoya.ui.UserInterface; // Not using this directly for dialogs now
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse; // For editor interactions
// import burp.api.montoya.ui.menu.MenuItem; // Switching to javax.swing.JMenuItem

import javax.swing.*;
import javax.swing.JMenuItem; // Explicit import for JMenuItem
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.net.URI;
import java.awt.Desktop;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.awt.Component;
import java.awt.Frame; // Added for Dialog parent

public class BurpMimeMontoyaExtension implements BurpExtension {

    private MontoyaApi montoyaApi;
    private Logging logging;

    private static final String EXTENSION_NAME = "MIME Type Generator";
    private static final String MENU_ITEM_TEXT_SHOW_COPY = "Show & Copy Response MIME Type";
    private static final String MENU_ITEM_TEXT_SEARCH_ONLINE = "Search this MIME Type Online";
    private static final String MENU_ITEM_TEXT_SET_MIME = "Set/Insert Content-Type";

    @Override
    public void initialize(MontoyaApi api) {
        this.montoyaApi = api;
        this.logging = api.logging();

        montoyaApi.extension().setName(EXTENSION_NAME);
        montoyaApi.userInterface().registerContextMenuItemsProvider(new MimeContextMenuItemsProvider());

        logging.logToOutput(EXTENSION_NAME + " loaded successfully.");
        logging.logToOutput("Right-click on an HTTP message to use MIME type utilities.");
    }

    class MimeContextMenuItemsProvider implements ContextMenuItemsProvider {
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

        String contentType = null;
        for (HttpHeader header : response.headers()) {
            if (header.name().equalsIgnoreCase("Content-Type")) {
                contentType = header.value();
                if (contentType.contains(";")) {
                    contentType = contentType.split(";")[0].trim();
                }
                break;
            }
        }

        if (contentType == null || contentType.isEmpty()) {
            contentType = response.inferredMimeType().toString();
            if (contentType.equalsIgnoreCase("Unknown") || contentType.isEmpty()) {
                JOptionPane.showMessageDialog(montoyaApi.userInterface().swingUtils().suiteFrame(),
                        "Could not find or infer MIME type from response.",
                        EXTENSION_NAME,
                        JOptionPane.INFORMATION_MESSAGE);
                logging.logToOutput("Show/Copy MIME: Could not determine MIME type.");
                return;
            }
            if (contentType.contains(";")) {
                contentType = contentType.split(";")[0].trim();
            }
        }

        if (contentType.isEmpty() || contentType.equalsIgnoreCase("Unknown")) {
            JOptionPane.showMessageDialog(montoyaApi.userInterface().swingUtils().suiteFrame(),
                    "Could not determine a valid MIME type to copy.",
                    EXTENSION_NAME,
                    JOptionPane.INFORMATION_MESSAGE);
            logging.logToOutput("Show/Copy MIME: MIME type is empty or unknown after checks.");
            return;
        }

        final String finalContentType = contentType;

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

        String contentType = null;
        for (HttpHeader header : response.headers()) {
            if (header.name().equalsIgnoreCase("Content-Type")) {
                contentType = header.value();
                if (contentType.contains(";")) {
                    contentType = contentType.split(";")[0].trim();
                }
                break;
            }
        }

        if (contentType == null || contentType.isEmpty()) {
            contentType = response.inferredMimeType().toString();
            if (contentType.equalsIgnoreCase("Unknown") || contentType.isEmpty()) {
                JOptionPane.showMessageDialog(montoyaApi.userInterface().swingUtils().suiteFrame(),
                        "Could not find 'Content-Type' header and could not infer MIME type to search.",
                        EXTENSION_NAME,
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (contentType.contains(";")) {
                contentType = contentType.split(";")[0].trim();
            }
        }

        if (contentType.isEmpty()) {
            JOptionPane.showMessageDialog(montoyaApi.userInterface().swingUtils().suiteFrame(),
                    "Could not determine MIME type for search.",
                    EXTENSION_NAME,
                    JOptionPane.INFORMATION_MESSAGE);
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
