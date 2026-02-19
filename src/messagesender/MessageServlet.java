package messagesender;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;

@WebServlet("/sendMessage")
public class MessageServlet extends HttpServlet {

    private static final String MESSAGES_DIR = "messages";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Get parameters from the form
        String from = request.getParameter("from");
        String to = request.getParameter("to");
        String subject = request.getParameter("subject");
        String message = request.getParameter("message");

        // Validate required fields
        if (from == null || from.trim().isEmpty() ||
                to == null || to.trim().isEmpty() ||
                message == null || message.trim().isEmpty()) {

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("From, To, and Message are required fields");
            return;
        }

        try {
            // Sanitize usernames (remove any path traversal attempts)
            from = sanitizeFilename(from);
            to = sanitizeFilename(to);

            // Create timestamp for filename
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

            // Create message content
            StringBuilder messageContent = new StringBuilder();
            messageContent.append("FROM: ").append(from).append("\n");
            messageContent.append("TO: ").append(to).append("\n");
            messageContent.append("SUBJECT: ").append((subject != null && !subject.trim().isEmpty()) ? subject : "(No Subject)").append("\n");
            messageContent.append("TIMESTAMP: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n");
            messageContent.append("-".repeat(50)).append("\n\n");
            messageContent.append(message);

            // Get the webapp's directory
            String webappPath = getServletContext().getRealPath("/");
            File messagesBaseDir = new File(webappPath, MESSAGES_DIR);

            // Create messages directory if it doesn't exist
            if (!messagesBaseDir.exists()) {
                messagesBaseDir.mkdir();
            }

            // Create user directory for recipient
            File userDir = new File(messagesBaseDir, to);
            if (!userDir.exists()) {
                userDir.mkdir();
            }

            // Save the message
            String filename = timestamp + "_from_" + from + ".txt";
            File messageFile = new File(userDir, filename);

            Files.write(messageFile.toPath(), messageContent.toString().getBytes("UTF-8"));

            // Also save a copy in sender's outbox (optional)
            File senderDir = new File(messagesBaseDir, from + "_sent");
            if (!senderDir.exists()) {
                senderDir.mkdir();
            }
            File sentCopy = new File(senderDir, timestamp + "_to_" + to + ".txt");
            Files.write(sentCopy.toPath(), messageContent.toString().getBytes("UTF-8"));

            // Log the message (for server console)
            System.out.println("[MESSAGE] From: " + from + " To: " + to + " - Saved to: " + messageFile.getAbsolutePath());

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("Message sent successfully!");

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Error saving message: " + e.getMessage());
        }
    }



    private String sanitizeFilename(String input) {
        // Remove any path traversal attempts and invalid characters
        return input.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }
}
