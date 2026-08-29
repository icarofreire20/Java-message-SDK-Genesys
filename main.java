import cloud.genesys.webmessaging.sdk.GenesysCloudRegionWebSocketHosts;
import cloud.genesys.webmessaging.sdk.WebMessagingClient;
import cloud.genesys.webmessaging.sdk.WebMessagingException;
import cloud.genesys.webmessaging.sdk.model.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class GenesysWebMessagingApp {

    // Your configured environment details
    private static final String DEPLOYMENT_ID = "60b4628f-38b6-46a7-a2c2-be91b3c87ef7";
    
    // Web Messaging origin domain for US West (Oregon)
    private static final String ORIGIN = "usw2.pure.cloud";

    // Synchronizes async event callbacks
    private static final CountDownLatch sessionReadyLatch = new CountDownLatch(1);

    public static void main(String[] args) {
        // 1. Instantiate the SDK client mapped to US West 2 (usw2.pure.cloud)
        // Alternatively: new WebMessagingClient("wss://webmessaging.usw2.pure.cloud/v1")
        WebMessagingClient client = new WebMessagingClient(GenesysCloudRegionWebSocketHosts.us_west_2);

        // 2. Register listener for WebSocket and Session events
        client.addSessionListener(new WebMessagingClient.SessionListener() {

            @Override
            public void webSocketConnected() {
                System.out.println("[+] WebSocket connected. Configuring session...");
                try {
                    // Send session configuration right after connection establishes
                    client.configureSession(DEPLOYMENT_ID, ORIGIN);
                } catch (WebMessagingException e) {
                    System.err.println("[-] Failed to configure session: " + e.getMessage());
                }
            }

            @Override
            public void sessionResponse(SessionResponse sessionResponse, String rawMessage) {
                System.out.println("[+] Session successfully established!");
                System.out.println("    Connected: " + sessionResponse.getConnected());
                System.out.println("    Readonly: " + sessionResponse.getReadOnly());
                
                // Signal main thread to proceed with sending the message
                sessionReadyLatch.countDown();
            }

            @Override
            public void structuredMessage(StructuredMessage structuredMessage, String rawMessage) {
                System.out.println("[+] Message event received: " + rawMessage);
            }

            @Override
            public void webSocketDisconnected(int statusCode, String reason) {
                System.out.println("[-] WebSocket disconnected: " + reason + " (Code: " + statusCode + ")");
            }

            @Override public void presignedUrlResponse(PresignedUrlResponse response, String s) {}
            @Override public void uploadSuccessEvent(UploadSuccessEvent event, String s) {}
            @Override public void uploadFailureEvent(UploadFailureEvent event, String s) {}
            @Override public void connectionClosedEvent(ConnectionClosedEvent event, String s) {}
            @Override public void sessionExpiredEvent(SessionExpiredEvent event, String s) {}
            @Override public void jwtResponse(JwtResponse response, String s) {}
            @Override public void unexpectedMessage(BaseMessage message, String s) {}
        });

        try {
            // 3. Connect to the socket
            System.out.println("[*] Connecting to Genesys Web Messaging...");
            client.connect(DEPLOYMENT_ID, ORIGIN);

            // 4. Block until session configuration completes (max 10 seconds timeout)
            boolean isConfigured = sessionReadyLatch.await(10, TimeUnit.SECONDS);

            if (isConfigured) {
                // 5. Send your message
                System.out.println("[*] Sending message...");
                client.sendMessage("Hello from Java Web Messaging SDK!");
                System.out.println("[+] Message sent.");

                // Keep process alive briefly to receive inbound acknowledgments/echoes
                Thread.sleep(5000);
            } else {
                System.err.println("[-] Timed out waiting for session authorization response.");
            }

        } catch (WebMessagingException e) {
            System.err.println("[-] Genesys WebMessaging SDK Exception: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // 6. Clean up socket connection
            client.disconnect();
            System.out.println("[*] Client disconnected.");
        }
    }
}
