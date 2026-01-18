package com.example.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera2Enumerator;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;

public class VideoCallActivity extends AppCompatActivity {

    private static final int REQ_PERMISSIONS = 2001;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SurfaceViewRenderer remoteRenderer;
    private SurfaceViewRenderer localRenderer;
    private TextView txtStatus;
    private PeerConnectionFactory factory;
    private PeerConnection peerConnection;
    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;
    private EglBase eglBase;
    private String reservaId;
    private String rol;
    private boolean isOfferer;
    private boolean localSdpSent = false;
    private boolean remoteSdpSet = false;
    private boolean polling = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

        reservaId = getIntent().getStringExtra("reservaId");
        rol = getIntent().getStringExtra("rol");
        isOfferer = rol != null && rol.equalsIgnoreCase("ALUMNO");

        txtStatus = findViewById(R.id.txtStatus);
        remoteRenderer = findViewById(R.id.remoteView);
        localRenderer = findViewById(R.id.localView);
        Button btnStart = findViewById(R.id.btnStart);
        Button btnReset = findViewById(R.id.btnReset);
        Button btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
        btnReset.setOnClickListener(v -> resetSesion());
        btnStart.setOnClickListener(v -> iniciarLlamada());

        if (!tienePermisos()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                    REQ_PERMISSIONS);
        }
    }

    @Override
    protected void onDestroy() {
        polling = false;
        handler.removeCallbacksAndMessages(null);
        if (peerConnection != null) {
            peerConnection.close();
        }
        if (localRenderer != null) {
            localRenderer.release();
        }
        if (remoteRenderer != null) {
            remoteRenderer.release();
        }
        if (eglBase != null) {
            eglBase.release();
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSIONS) {
            boolean granted = true;
            for (int result : grantResults) {
                granted = granted && result == PackageManager.PERMISSION_GRANTED;
            }
            if (!granted) {
                Toast.makeText(this, "Permisos de cámara y audio requeridos.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean tienePermisos() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void iniciarLlamada() {
        if (reservaId == null) {
            Toast.makeText(this, "Reserva no válida", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!tienePermisos()) {
            Toast.makeText(this, "Permisos necesarios", Toast.LENGTH_SHORT).show();
            return;
        }
        txtStatus.setText("Inicializando llamada...");
        new Thread(() -> {
            try {
                ApiClient.ApiResponse response = ApiClient.get("/api/reservas/" + reservaId + "/video-config");
                JSONObject json = ApiClient.parseJson(response.body);
                if (response.code >= 200 && response.code < 300 && json != null
                        && "ok".equalsIgnoreCase(json.optString("status"))) {
                    String turnUrls = json.optString("turnUrls", "");
                    String turnUsername = json.optString("turnUsername", "");
                    String turnCredential = json.optString("turnCredential", "");
                    List<PeerConnection.IceServer> iceServers = new ArrayList<>();
                    if (!turnUrls.isEmpty()) {
                        String[] urls = turnUrls.split(",");
                        for (String url : urls) {
                            PeerConnection.IceServer.Builder builder = PeerConnection.IceServer.builder(url.trim());
                            if (!turnUsername.isEmpty() && !turnCredential.isEmpty()) {
                                builder.setUsername(turnUsername);
                                builder.setPassword(turnCredential);
                            }
                            iceServers.add(builder.createIceServer());
                        }
                    }
                    runOnUiThread(() -> iniciarWebRtc(iceServers));
                } else {
                    String msg = json != null ? json.optString("msg", response.body) : response.body;
                    runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void iniciarWebRtc(List<PeerConnection.IceServer> iceServers) {
        PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(this).createInitializationOptions()
        );
        eglBase = EglBase.create();
        localRenderer.init(eglBase.getEglBaseContext(), null);
        remoteRenderer.init(eglBase.getEglBaseContext(), null);
        localRenderer.setZOrderMediaOverlay(true);

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        DefaultVideoEncoderFactory encoderFactory = new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true);
        DefaultVideoDecoderFactory decoderFactory = new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());
        factory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();

        VideoCapturer capturer = createVideoCapturer();
        VideoSource videoSource = factory.createVideoSource(capturer.isScreencast());
        capturer.initialize(
                org.webrtc.SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext()),
                this,
                videoSource.getCapturerObserver()
        );
        capturer.startCapture(640, 480, 30);
        localVideoTrack = factory.createVideoTrack("LOCAL_VIDEO", videoSource);
        localVideoTrack.addSink(localRenderer);

        AudioSource audioSource = factory.createAudioSource(new MediaConstraints());
        localAudioTrack = factory.createAudioTrack("LOCAL_AUDIO", audioSource);

        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        peerConnection = factory.createPeerConnection(rtcConfig, new PeerConnection.Observer() {
            @Override public void onSignalingChange(PeerConnection.SignalingState signalingState) { }
            @Override public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) { }
            @Override public void onIceConnectionReceivingChange(boolean b) { }
            @Override public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                if (iceGatheringState == PeerConnection.IceGatheringState.COMPLETE && !localSdpSent) {
                    SessionDescription local = peerConnection.getLocalDescription();
                    if (local != null) {
                        enviarSdp(local);
                    }
                }
            }
            @Override public void onIceCandidate(org.webrtc.IceCandidate iceCandidate) { }
            @Override public void onIceCandidatesRemoved(org.webrtc.IceCandidate[] iceCandidates) { }
            @Override public void onAddStream(org.webrtc.MediaStream mediaStream) { }
            @Override public void onRemoveStream(org.webrtc.MediaStream mediaStream) { }
            @Override public void onDataChannel(org.webrtc.DataChannel dataChannel) { }
            @Override public void onRenegotiationNeeded() { }
            @Override public void onAddTrack(org.webrtc.RtpReceiver rtpReceiver, org.webrtc.MediaStream[] mediaStreams) {
                if (mediaStreams != null && mediaStreams.length > 0) {
                    org.webrtc.MediaStream stream = mediaStreams[0];
                    if (!stream.videoTracks.isEmpty()) {
                        stream.videoTracks.get(0).addSink(remoteRenderer);
                    }
                }
            }
        });

        peerConnection.addTrack(localVideoTrack);
        peerConnection.addTrack(localAudioTrack);

        txtStatus.setText(isOfferer ? "Creando offer..." : "Esperando offer...");
        if (isOfferer) {
            peerConnection.createOffer(new SdpAdapter("offer") {
                @Override public void onCreateSuccess(SessionDescription sessionDescription) {
                    peerConnection.setLocalDescription(new SdpAdapter("setLocalOffer"), sessionDescription);
                }
            }, constraints);
        } else {
            polling = true;
            handler.post(this::pollOffer);
        }
    }

    private void enviarSdp(SessionDescription local) {
        localSdpSent = true;
        String sdp64 = Base64.encodeToString(local.description.getBytes(), Base64.NO_WRAP);
        String endpoint = isOfferer ? "/reservas/" + reservaId + "/offer" : "/reservas/" + reservaId + "/answer";
        new Thread(() -> {
            try {
                java.util.Map<String, String> params = new java.util.HashMap<>();
                params.put("sdp64", sdp64);
                ApiClient.postForm(endpoint, params);
                runOnUiThread(() -> txtStatus.setText("SDP enviado, esperando respuesta..."));
                if (isOfferer) {
                    polling = true;
                    handler.post(this::pollAnswer);
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error SDP: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void pollOffer() {
        if (!polling || remoteSdpSet) return;
        new Thread(() -> {
            try {
                ApiClient.ApiResponse response = ApiClient.get("/reservas/" + reservaId + "/offer");
                JSONObject json = ApiClient.parseJson(response.body);
                if (json != null && json.has("sdp") && json.optString("sdp", "").length() > 0) {
                    String sdp = json.optString("sdp", "");
                    SessionDescription remote = new SessionDescription(SessionDescription.Type.OFFER, sdp);
                    peerConnection.setRemoteDescription(new SdpAdapter("setRemoteOffer"), remote);
                    remoteSdpSet = true;
                    peerConnection.createAnswer(new SdpAdapter("answer") {
                        @Override public void onCreateSuccess(SessionDescription sessionDescription) {
                            peerConnection.setLocalDescription(new SdpAdapter("setLocalAnswer"), sessionDescription);
                        }
                    }, new MediaConstraints());
                    return;
                }
            } catch (Exception ignored) {
            }
            if (polling) {
                handler.postDelayed(this::pollOffer, 2000);
            }
        }).start();
    }

    private void pollAnswer() {
        if (!polling || remoteSdpSet) return;
        new Thread(() -> {
            try {
                ApiClient.ApiResponse response = ApiClient.get("/reservas/" + reservaId + "/answer");
                JSONObject json = ApiClient.parseJson(response.body);
                if (json != null && json.has("sdp") && json.optString("sdp", "").length() > 0) {
                    String sdp = json.optString("sdp", "");
                    SessionDescription remote = new SessionDescription(SessionDescription.Type.ANSWER, sdp);
                    peerConnection.setRemoteDescription(new SdpAdapter("setRemoteAnswer"), remote);
                    remoteSdpSet = true;
                    runOnUiThread(() -> txtStatus.setText("Conectado"));
                    return;
                }
            } catch (Exception ignored) {
            }
            if (polling) {
                handler.postDelayed(this::pollAnswer, 2000);
            }
        }).start();
    }

    private void resetSesion() {
        if (reservaId == null) return;
        new Thread(() -> {
            try {
                ApiClient.postForm("/reservas/" + reservaId + "/reset", new java.util.HashMap<>());
                runOnUiThread(() -> Toast.makeText(this, "Sesión reiniciada", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private VideoCapturer createVideoCapturer() {
        Camera2Enumerator enumerator = new Camera2Enumerator(this);
        for (String deviceName : enumerator.getDeviceNames()) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer capturer = enumerator.createCapturer(deviceName, null);
                if (capturer != null) return capturer;
            }
        }
        for (String deviceName : enumerator.getDeviceNames()) {
            VideoCapturer capturer = enumerator.createCapturer(deviceName, null);
            if (capturer != null) return capturer;
        }
        throw new RuntimeException("No se encontró cámara");
    }

    private static abstract class SdpAdapter implements org.webrtc.SdpObserver {
        private final String tag;

        SdpAdapter(String tag) {
            this.tag = tag;
        }

        @Override public void onCreateSuccess(SessionDescription sessionDescription) { }
        @Override public void onSetSuccess() { }
        @Override public void onCreateFailure(String s) { }
        @Override public void onSetFailure(String s) { }
    }
}
