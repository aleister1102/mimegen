import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * A dialog for selecting a MIME type from a predefined list.
 * Allows filtering the list via a search field.
 */
public class MimeTypeSelectionDialog extends JDialog {
    private JList<String> mimeList;
    private DefaultListModel<String> listModel;
    private JTextField searchField;
    private JButton okButton;
    private JButton cancelButton;
    private String selectedMimeType;

    // Expanded list of common MIME types
    private static final List<String> PREDEFINED_MIME_TYPES = List.of(
            // Application types
            "application/atom+xml",
            "application/EDI-X12",
            "application/EDIFACT",
            "application/graphql",
            "application/javascript",
            "application/json",
            "application/ld+json",
            "application/msword", // .doc
            "application/octet-stream",
            "application/ogg",
            "application/pdf",
            "application/pkcs8",
            "application/postscript",
            "application/rdf+xml",
            "application/rss+xml",
            "application/rtf",
            "application/soap+xml",
            "application/vnd.api+json",
            "application/vnd.ms-excel", // .xls
            "application/vnd.ms-powerpoint", // .ppt
            "application/vnd.oasis.opendocument.presentation", // .odp
            "application/vnd.oasis.opendocument.spreadsheet", // .ods
            "application/vnd.oasis.opendocument.text", // .odt
            "application/vnd.openxmlformats-officedocument.presentationml.presentation", // .pptx
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xlsx
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
            "application/x-7z-compressed",
            "application/x-bzip",
            "application/x-bzip2",
            "application/x-csh",
            "application/x-www-form-urlencoded",
            "application/xhtml+xml",
            "application/xml",
            "application/zip",
            // Audio types
            "audio/aac",
            "audio/midi",
            "audio/mpeg",
            "audio/ogg",
            "audio/opus",
            "audio/wav",
            "audio/webm",
            // Font types
            "font/otf",
            "font/ttf",
            "font/woff",
            "font/woff2",
            // Image types
            "image/apng",
            "image/bmp",
            "image/gif",
            "image/jpeg", // .jpg, .jpeg
            "image/png",
            "image/svg+xml",
            "image/tiff",
            "image/webp",
            "image/x-icon",
            // Multipart types
            "multipart/form-data",
            "multipart/mixed",
            "multipart/alternative",
            "multipart/related",
            // Text types
            "text/calendar",
            "text/css",
            "text/csv",
            "text/html",
            "text/javascript", // (Obsolete, use application/javascript)
            "text/plain",
            "text/markdown",
            "text/rtf",
            "text/sgml",
            "text/xml",
            "text/yaml",
            // Video types
            "video/mpeg",
            "video/mp4",
            "video/ogg",
            "video/quicktime",
            "video/webm",
            "video/x-msvideo" // .avi
    );

    /**
     * Constructs a MimeTypeSelectionDialog.
     * 
     * @param owner The Frame from which the dialog is displayed.
     */
    public MimeTypeSelectionDialog(Frame owner) {
        super(owner, "Select MIME Type", true);
        initComponents();
        layoutComponents();
        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * Initializes the components of the dialog.
     */
    private void initComponents() {
        listModel = new DefaultListModel<>();
        PREDEFINED_MIME_TYPES.forEach(listModel::addElement);
        mimeList = new JList<>(listModel);
        mimeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mimeList.setVisibleRowCount(10);

        searchField = new JTextField(20);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterList();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterList();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterList();
            }
        });

        okButton = new JButton("OK");
        okButton.addActionListener((ActionEvent e) -> {
            selectedMimeType = mimeList.getSelectedValue();
            if (selectedMimeType != null) {
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Please select a MIME type.", "Selection Required",
                        JOptionPane.WARNING_MESSAGE);
            }
        });

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener((ActionEvent e) -> {
            selectedMimeType = null;
            dispose();
        });
    }

    /**
     * Lays out the components within the dialog.
     */
    private void layoutComponents() {
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout(5, 5));
        contentPane.add(searchPanel, BorderLayout.NORTH);
        contentPane.add(new JScrollPane(mimeList), BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        ((JComponent) contentPane).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    /**
     * Filters the list of MIME types based on the search term entered by the user.
     */
    private void filterList() {
        String searchTerm = searchField.getText().toLowerCase();
        listModel.clear();
        PREDEFINED_MIME_TYPES.stream()
                .filter(mime -> mime.toLowerCase().contains(searchTerm))
                .forEach(listModel::addElement);
    }

    /**
     * Gets the MIME type selected by the user.
     * 
     * @return The selected MIME type string, or null if the dialog was cancelled or
     *         no selection was made.
     */
    public String getSelectedMimeType() {
        return selectedMimeType;
    }
}