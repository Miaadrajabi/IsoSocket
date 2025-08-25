package com.miaad.isosocket.sample;

import static com.miaad.isosocket.util.Logger.Level.INFO;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.miaad.isosocket.ConnectError;
import com.miaad.isosocket.ConnectionMode;
import com.miaad.isosocket.TcpClient;
import com.miaad.isosocket.TcpResponse;
import com.miaad.isosocket.framing.DelimiterFramer;
import com.miaad.isosocket.framing.LengthPrefixedFramer;
import com.miaad.isosocket.framing.Framer;
import com.miaad.isosocket.state.ConnectionState;
import com.miaad.isosocket.state.StateInfo;
import com.miaad.isosocket.state.StateListener;
import com.miaad.isosocket.state.TrafficEvent;
import com.miaad.isosocket.util.Loggers;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

/**
 * Minimal interactive test screen for IsoSocket.
 * - Lets you choose framing (Delimiter or Length-Prefixed)
 * - Hex/Text payload toggle
 * - Shows state transitions, traffic, and RTT
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Basic inputs to configure the connection and payload.
     */
    private EditText hostEt, portEt, messageEt, timeoutEt;
    private Button connectBtn, sendBtn, disconnectBtn, loadIsoBtn;
    private TextView logTv;
    private ScrollView scroll;

    /**
     * Framing controls: length-prefixed options and byte order, and hex mode.
     */
    private CheckBox useLengthPrefixCb, includeLenCb, hexModeCb;
    private EditText lengthBytesEt;
    private Spinner byteOrderSp;

    /**
     * Active client instance (closed/recreated on connect/disconnect).
     */
    @Nullable private TcpClient client;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildSimpleLayout());

        // Wire buttons to actions.
        connectBtn.setOnClickListener(v -> onConnect());
        sendBtn.setOnClickListener(v -> onSend());
        disconnectBtn.setOnClickListener(v -> onDisconnect());
        loadIsoBtn.setOnClickListener(v -> loadIsoSample());
    }

    /**
     * Builds a TcpClient using current UI selections and attempts a connect.
     * Includes a complete, production-style configuration: timeouts, socket
     * options, backoff with jitter, strict retry categories, and logging.
     */
    private void onConnect() {
        String host = hostEt.getText().toString().trim();
        int port = Integer.parseInt(portEt.getText().toString().trim());
        append("Connecting to " + host + ":" + port + "...\n");
        Handler main = new Handler(Looper.getMainLooper());
        client = new TcpClient.Builder()
                .host(host)
                .port(port)
                .mode(ConnectionMode.BLOCKING)
                .framer(buildFramer())

                // Low-level socket tuning
                .tcpNoDelay(true)
                .keepAlive(true)
                .soLingerSec(0)
                .receiveBufferSize(128 * 1024)
                .sendBufferSize(128 * 1024)

                // Timeouts
                .connectTimeoutMs(5000)
                .readTimeoutMs(8000)
                .writeTimeoutMs(8000)
                .handshakeTimeoutMs(10000)
                .requestTimeoutMs(10000)

                // Concurrency/queue and pacing
                .maxInFlightRequests(1)
                .requestQueueCapacity(32)
                .minInterRequestDelayMs(50L)

                // Retry policy: exactly three attempts with jittered backoff
                .autoReconnect(true)
                .connectRetryEnabled(true)
                .connectMaxRetries(5)
                .connectInitialBackoffMs(500)
                .connectMaxBackoffMs(5000)
                .connectJitterFactor(0.2f)
                .connectRetryOn(new java.util.HashSet<>(java.util.Arrays.asList(
                        ConnectError.TIMEOUT,
                        ConnectError.CONNECTION_REFUSED,
                        ConnectError.NETWORK_UNREACHABLE,
                        ConnectError.HANDSHAKE_TIMEOUT,
                        ConnectError.DNS,
                        ConnectError.UNKNOWN
                )))

                // TLS (disabled by default here; enable for secure endpoints)
                .tlsOptions(new com.miaad.isosocket.tls.TlsOptions.Builder()
                        .enableTls(false)
                        .verifyHostname(true)
                        .tlsProtocolVersions(java.util.Arrays.asList("TLSv1.2","TLSv1.3"))
                        // .pinnedSpkiSha256(java.util.Arrays.asList("BASE64_PIN_1","BASE64_PIN_2"))
                        .build())

                // Logging and callbacks on main thread
                .logger(Loggers.androidTag("IsoSocket" , INFO))
                .stateListener(new UiStateListener())
                .mainThreadHandler(main)
                .build();
        new Thread(() -> {
            try {
                client.connect();
                runOnUiThread(() -> append("connect() returned\n"));
            } catch (Exception e) {
                runOnUiThread(() -> append("connect error: " + e + "\n"));
            }
        }).start();
    }

    /**
     * Creates the appropriate Framer from UI controls.
     * - Length-Prefixed: 2/4 bytes, endianness, include-length toggle
     * - Delimiter: single-byte terminator ('\n')
     */
    private Framer buildFramer() {
        boolean lp = useLengthPrefixCb.isChecked();
        if (lp) {
            int lenBytes = safeParseInt(lengthBytesEt.getText().toString().trim(), 2);
            if (lenBytes != 2 && lenBytes != 4) lenBytes = 2;
            String orderSel = (String) byteOrderSp.getSelectedItem();
            ByteOrder order = (orderSel != null && orderSel.toUpperCase(Locale.US).contains("LITTLE")) ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
            boolean includeLen = includeLenCb.isChecked();
            return new LengthPrefixedFramer(lenBytes, order, includeLen);
        } else {
            return new DelimiterFramer((byte) '\n');
        }
    }

    /**
     * Sends the payload (text/hex based on toggle) and prints both text and hex responses with RTT.
     */
    private void onSend() {
        TcpClient c = client;
        if (c == null) { append("Not connected\n"); return; }
        byte[] req = buildPayloadBytes();
        int timeout = Integer.parseInt(timeoutEt.getText().toString().trim());
        append("Sending " + (hexModeCb.isChecked() ? ("[hex " + req.length + " bytes]") : ("[text] '" + messageEt.getText().toString() + "'")) + "\n");
        new Thread(() -> {
            try {
                TcpResponse res = c.sendAndReceive(req, timeout);
                String bodyText = new String(res.getPayload(), StandardCharsets.UTF_8);
                String bodyHex = toHex(res.getPayload());
                runOnUiThread(() -> {
                    append("Received (" + res.getRoundTripTimeMillis() + "ms) bytes=" + res.getPayload().length + "\n");
                    append("as text: " + bodyText + "\n");
                    append("as hex : " + bodyHex + "\n");
                });
            } catch (IOException | TimeoutException e) {
                runOnUiThread(() -> append("send error: " + e + "\n"));
            }
        }).start();
    }

    /**
     * Closes the active client.
     */
    private void onDisconnect() {
        TcpClient c = client; client = null;
        if (c != null) {
            c.disconnect();
            append("disconnect() called\n");
        }
    }

    /**
     * Loads a small ISO-8583 sample (hex string) and configures LPF 2-byte BE.
     * Useful when testing against echo servers (e.g., tcpbin) to validate framing.
     */
    private void loadIsoSample() {
        String isoHex = "30 38 30 30" + // '0800'
                " 70 00 00 00 00 00 00 00" + // illustrative bitmap
                " 32 30 31 38 30 38 32 33 31 32 33 34" + // field 7 '201808231234'
                " 30 30 30 30 31" + // field 11 '00001'
                " 33 30 31"; // field 70 '301'
        hexModeCb.setChecked(true);
        messageEt.setText(isoHex);
        useLengthPrefixCb.setChecked(true);
        lengthBytesEt.setText("2");
        includeLenCb.setChecked(false);
        byteOrderSp.setSelection(0); // BIG_ENDIAN
        append("Loaded ISO-8583 sample (hex).\n");
    }

    /**
     * Converts UI payload to bytes based on hex toggle.
     */
    private byte[] buildPayloadBytes() {
        if (hexModeCb.isChecked()) {
            return parseHex(messageEt.getText().toString());
        } else {
            return messageEt.getText().toString().getBytes(StandardCharsets.UTF_8);
        }
    }

    private static int safeParseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception ignore) { return def; }
    }

    /**
     * Parses a hex string like "30 31 32" into bytes {0x30,0x31,0x32}.
     */
    private static byte[] parseHex(String s) {
        String clean = s.replaceAll("[^0-9A-Fa-f]", "");
        if (clean.length() % 2 != 0) clean = "0" + clean;
        byte[] out = new byte[clean.length() / 2];
        for (int i = 0; i < clean.length(); i += 2) {
            out[i / 2] = (byte) Integer.parseInt(clean.substring(i, i + 2), 16);
        }
        return out;
    }

    /**
     * Formats bytes as upper-case hex, space separated.
     */
    private static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) sb.append(String.format(Locale.US, "%02X ", b));
        return sb.toString().trim();
    }

    /**
     * Programmatic UI to keep the sample focused on socket logic (no XML layout).
     */
    @SuppressLint("SetTextI18n")
    private View buildSimpleLayout() {
        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (12 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        hostEt = new EditText(this); hostEt.setHint("Host (e.g. tcpbin.com)"); hostEt.setText("tcpbin.com");
        portEt = new EditText(this); portEt.setHint("Port (e.g. 4242)"); portEt.setInputType(android.text.InputType.TYPE_CLASS_NUMBER); portEt.setText("4242");
        messageEt = new EditText(this); messageEt.setHint("Message (text or hex)"); messageEt.setText("ping");
        timeoutEt = new EditText(this); timeoutEt.setHint("Timeout ms"); timeoutEt.setInputType(android.text.InputType.TYPE_CLASS_NUMBER); timeoutEt.setText("5000");

        useLengthPrefixCb = new CheckBox(this); useLengthPrefixCb.setText("Use Length-Prefixed Framer");
        includeLenCb = new CheckBox(this); includeLenCb.setText("Include length in frame");
        hexModeCb = new CheckBox(this); hexModeCb.setText("Hex payload mode");
        lengthBytesEt = new EditText(this); lengthBytesEt.setHint("Length bytes (2 or 4)"); lengthBytesEt.setInputType(android.text.InputType.TYPE_CLASS_NUMBER); lengthBytesEt.setText("2");
        byteOrderSp = new Spinner(this);
        byteOrderSp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"BIG_ENDIAN", "LITTLE_ENDIAN"}));

        connectBtn = new Button(this); connectBtn.setText("Connect");
        sendBtn = new Button(this); sendBtn.setText("Send");
        disconnectBtn = new Button(this); disconnectBtn.setText("Disconnect");
        loadIsoBtn = new Button(this); loadIsoBtn.setText("Load ISO-8583 Sample");

        logTv = new TextView(this); logTv.setTextIsSelectable(true);
        scroll = new ScrollView(this);
        scroll.addView(logTv);

        root.addView(hostEt);
        root.addView(portEt);
        root.addView(messageEt);
        root.addView(timeoutEt);
        root.addView(hexModeCb);
        root.addView(useLengthPrefixCb);
        root.addView(lengthBytesEt);
        root.addView(includeLenCb);
        root.addView(byteOrderSp);
        root.addView(connectBtn);
        root.addView(sendBtn);
        root.addView(disconnectBtn);
        root.addView(loadIsoBtn);
        root.addView(scroll, new android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return root;
    }

    /**
     * Appends a line to the on-screen log and scrolls to bottom.
     */
    private void append(String s) {
        logTv.append(s);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    /**
     * Bridges socket state/traffic/errors to the UI log.
     */
    private final class UiStateListener implements StateListener {
        @Override public void onStateChanged(ConnectionState state, StateInfo info) { runOnUiThread(() -> append("state: " + state + "\n")); }
        @Override public void onTraffic(TrafficEvent event) { runOnUiThread(() -> append("traffic: " + event.kind + " " + event.byteCount + "\n")); }
        @Override public void onError(Throwable t, ErrorStage stage) { runOnUiThread(() -> append("error(" + stage + "): " + t + "\n")); }
        @Override public void onRetryScheduled(int attempt, long backoffMs, Throwable cause) { runOnUiThread(() -> append("retry #" + attempt + " in " + backoffMs + "ms: " + cause + "\n")); }
        @Override public void onRetryExhausted(Throwable lastError) { runOnUiThread(() -> append("retry exhausted: " + lastError + "\n")); }
    }
}
