// import Client.Client;
// import Server.Server;
// import org.junit.AfterClass;
// import org.junit.BeforeClass;
// import org.junit.Test;

// import java.io.IOException;
// import java.util.HashMap;
// import java.util.HashSet;
// import java.util.Map;
// import java.util.Set;

// import static org.junit.Assert.*;

// public class ClientServerTest {
// private static Server server;
// private static Thread serverThread;
// private static final String HOST = "localhost";
// private static final int PORT = 8080;

// @BeforeClass
// public static void setUp() throws InterruptedException {
// server = new Server(10);
// serverThread = new Thread(() -> {
// try {
// server.start(PORT);
// } catch (InterruptedException e) {
// e.printStackTrace();
// }
// });
// serverThread.start();
// Thread.sleep(1000); // Wait for the server to start
// }

// @AfterClass
// public static void tearDown() {
// serverThread.interrupt();
// }

// @Test
// public void testRegisterAndAuthenticate() throws IOException {
// try (Client client = new Client(HOST, PORT)) {
// boolean registered = client.register("user1", "password1");
// assertTrue(registered);

// boolean authenticated = client.authenticate("user1", "password1");
// assertTrue(authenticated);

// // logout and try to authenticate again
// client.logout();

// boolean failedAuth = client.authenticate("user1", "wrongpassword");
// assertFalse(failedAuth);
// }
// }

// @Test
// public void testPutAndGet() throws IOException {
// try (Client client = new Client(HOST, PORT)) {
// client.authenticate("user1", "password1");
// client.put("key1", "value1".getBytes());
// byte[] value = client.get("key1");
// assertNotNull(value);
// assertEquals("value1", new String(value));
// }
// }

// @Test
// public void testMultiPutAndMultiGet() throws IOException {
// try (Client client = new Client(HOST, PORT)) {
// client.authenticate("user1", "password1");
// Map<String, byte[]> pairs = new HashMap<>();
// pairs.put("key2", "value2".getBytes());
// pairs.put("key3", "value3".getBytes());
// client.multiPut(pairs);

// Set<String> keys = new HashSet<>(pairs.keySet());
// Map<String, byte[]> values = client.multiGet(keys);
// assertNotNull(values);
// assertEquals(2, values.size());
// assertEquals("value2", new String(values.get("key2")));
// assertEquals("value3", new String(values.get("key3")));
// }
// }

// @Test
// public void testGetWhen() throws IOException, InterruptedException {
// try (Client client = new Client(HOST, PORT)) {
// client.authenticate("user1", "password1");
// client.put("keyCond", "condValue".getBytes());
// byte[] value = client.getWhen("key1", "keyCond", "condValue".getBytes());
// assertNotNull(value);
// assertEquals("value1", new String(value));
// }
// }

// @Test
// public void testLogout() throws IOException {
// try (Client client = new Client(HOST, PORT)) {
// client.authenticate("user1", "password1");
// client.logout();
// assertFalse(client.authenticate("user1", "wrongpassword"));
// }
// }
// }