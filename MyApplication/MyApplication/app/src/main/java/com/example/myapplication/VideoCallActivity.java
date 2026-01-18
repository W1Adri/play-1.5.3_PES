package com.example.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.List;

public class VideoCallActivity extends AppCompatActivity {

    private static final int REQ_PERMISSIONS = 1001;
    private static final String[] REQUIRED_PERMISSIONS = new String[] {
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    };

    private EglBase eglBase;
    private PeerConnectionFactory factory;
    private PeerConnection peerConnection;
    private VideoCapturer videoCapturer;
    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;
    private SurfaceViewRenderer localRenderer;
    private SurfaceViewRenderer remoteRenderer;
    private String reservaId;
    private String rol;
    private volatile boolean running = true;
    private Button btnHangup;
    private Button btnToggleAudio;
    private Button btnToggleVideo;
    private boolean isAudioEnabled = true;
    private boolean isVideoEnabled = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videocall);

        reservaId = getIntent().getStringExtra("reservaId");
        rol = getIntent().getStringExtra("rol");

        localRenderer = findViewById(R.id.localRenderer);
        remoteRenderer = findViewById(R.id.remoteRenderer);
        btnHangup = findViewById(R.id.btnHangup);
        btnToggleAudio = findViewById(R.id.btnToggleAudio);
        btnToggleVideo = findViewById(R.id.btnToggleVideo);
        
        btnHangup.setOnClickListener(v -> finish());
        
        btnToggleAudio.setOnClickListener(v -> {
            isAudioEnabled = !isAudioEnabled;
            if (localAudioTrack != null) {
                localAudioTrack.setEnabled(isAudioEnabled);
            }
            btnToggleAudio.setText(isAudioEnabled ? "🔊 Audio" : "🔇 Mute");
        });
        
        btnToggleVideo.setOnClickListener(v -> {
            isVideoEnabled = !isVideoEnabled;
            if (localVideoTrack != null) {
                localVideoTrack.setEnabled(isVideoEnabled);
            }
            btnToggleVideo.setText(isVideoEnabled ? "📹 Video" : "📷 Video Off");
        });

        if (hasPermissions()) {
            initWebRtc();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQ_PERMISSIONS);
        }
    }

    @Override
    protected void onDestroy() {
        running = false;
        if (peerConnection != null) {
            peerConnection.close();
        }
        if (localRenderer != null) {
            localRenderer.release();
        }
        if (remoteRenderer != null) {
            remoteRenderer.release();
        }
        if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
            } catch (InterruptedException ignored) {
            }
            videoCapturer.dispose();
        }
        if (factory != null) {
            factory.dispose();
        }
        if (eglBase != null) {
            eglBase.release();
        }
        super.onDestroy();
    }

    private boolean hasPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSIONS) {
            if (hasPermissions()) {
                initWebRtc();
            } else {
                Toast.makeText(this, "Permisos de cámara y audio requeridos.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void initWebRtc() {
        eglBase = EglBase.create();
        localRenderer.init(eglBase.getEglBaseContext(), null);
        remoteRenderer.init(eglBase.getEglBaseContext(), null);
        localRenderer.setMirror(true);
        remoteRenderer.setMirror(false);

        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(this).createInitializationOptions()
        );
        factory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(new DefaultVideoDecoderFactory(eglBase.getEglBaseContext()))
            .setVideoEncoderFactory(new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true))
            .createPeerConnectionFactory();

        videoCapturer = createVideoCapturer();
        if (videoCapturer == null) {
            Toast.makeText(this, "No se encontró cámara.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
        VideoSource videoSource = factory.createVideoSource(false);
        videoCapturer.initialize(surfaceTextureHelper, this, videoSource.getCapturerObserver());
        videoCapturer.startCapture(640, 480, 30);
        localVideoTrack = factory.createVideoTrack("LOCAL_VIDEO", videoSource);
        localVideoTrack.addSink(localRenderer);

        AudioSource audioSource = factory.createAudioSource(new MediaConstraints());
        localAudioTrack = factory.createAudioTrack("LOCAL_AUDIO", audioSource);

        MediaStream stream = factory.createLocalMediaStream("LOCAL_STREAM");
        stream.addTrack(localVideoTrack);
        stream.addTrack(localAudioTrack);

        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());

        PeerConnection.RTCConfiguration config = new PeerConnection.RTCConfiguration(iceServers);
        peerConnection = factory.createPeerConnection(config, new PeerConnection.Observer() {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                // In a production app, you would send this to the remote peer
                // For now, we're using trickle ICE which is handled by the SDP exchange
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                runOnUiThread(() -> {
                    if (mediaStream.videoTracks.size() > 0) {
                        mediaStream.videoTracks.get(0).addSink(remoteRenderer);
                    }
                });
            }

            @Override 
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                runOnUiThread(() -> {
                    if (signalingState == PeerConnection.SignalingState.STABLE) {
                        Toast.makeText(VideoCallActivity.this, "Conexión establecida", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override 
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                runOnUiThread(() -> {
                    if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED) {
                        Toast.makeText(VideoCallActivity.this, "Video conectado", Toast.LENGTH_SHORT).show();
                    } else if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {
                        Toast.makeText(VideoCallActivity.this, "Desconectado", Toast.LENGTH_SHORT).show();
                    } else if (iceConnectionState == PeerConnection.IceConnectionState.FAILED) {
                        Toast.makeText(VideoCallActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override public void onIceConnectionReceivingChange(boolean b) { }
            @Override public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) { }
            @Override public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) { }
            @Override public void onRemoveStream(MediaStream mediaStream) { }
            @Override public void onDataChannel(org.webrtc.DataChannel dataChannel) { }
            @Override public void onRenegotiationNeeded() { }
            @Override public void onAddTrack(org.webrtc.RtpReceiver rtpReceiver, MediaStream[] mediaStreams) { }
        });

        if (peerConnection != null) {
            peerConnection.addStream(stream);
        }

        if ("ALUMNO".equalsIgnoreCase(rol)) {
            crearOffer();
        } else {
            esperarOfferYResponder();
        }
    }

    private void crearOffer() {
        MediaConstraints constraints = new MediaConstraints();
        peerConnection.createOffer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(new SimpleSdpObserver() {
                    @Override
                    public void onSetSuccess() {
                        publicarSdp("/reservas/" + reservaId + "/offer", sessionDescription);
                        esperarAnswer();
                    }
                    
                    @Override
                    public void onSetFailure(String error) {
                        runOnUiThread(() -> Toast.makeText(VideoCallActivity.this, 
                            "Error configurando oferta: " + error, Toast.LENGTH_SHORT).show());
                    }
                }, sessionDescription);
            }
            
            @Override
            public void onCreateFailure(String error) {
                runOnUiThread(() -> Toast.makeText(VideoCallActivity.this, 
                    "Error creando oferta: " + error, Toast.LENGTH_SHORT).show());
            }
        }, constraints);
    }

    private void esperarOfferYResponder() {
        runOnUiThread(() -> Toast.makeText(this, "Esperando oferta del profesor...", Toast.LENGTH_SHORT).show());
        new Thread(() -> {
            final int MAX_ATTEMPTS = 60; // 60 attempts * 1.5s = 90 seconds timeout
            final int POLL_INTERVAL_MS = 1500;
            int intentos = 0;
            while (running && intentos < MAX_ATTEMPTS) {
                try {
                    ApiClient.ApiResponse response = ApiClient.get("/reservas/" + reservaId + "/offer");
                    JSONObject json = ApiClient.parseJson(response.body);
                    if (response.code >= 200 && response.code < 300 && json != null && json.optString("sdp", "").length() > 0) {
                        String sdp = json.optString("sdp", "");
                        SessionDescription offer = new SessionDescription(SessionDescription.Type.OFFER, sdp);
                        peerConnection.setRemoteDescription(new SimpleSdpObserver() {
                            @Override
                            public void onSetSuccess() {
                                crearAnswer();
                            }
                            
                            @Override
                            public void onSetFailure(String error) {
                                runOnUiThread(() -> Toast.makeText(VideoCallActivity.this, 
                                    "Error procesando oferta: " + error, Toast.LENGTH_SHORT).show());
                            }
                        }, offer);
                        return;
                    }
                    intentos++;
                    Thread.sleep(POLL_INTERVAL_MS);
                } catch (Exception e) {
                    break;
                }
            }
            if (intentos >= MAX_ATTEMPTS) {
                runOnUiThread(() -> {
                    Toast.makeText(VideoCallActivity.this, "Timeout esperando oferta", Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    private void crearAnswer() {
        MediaConstraints constraints = new MediaConstraints();
        peerConnection.createAnswer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(new SimpleSdpObserver() {
                    @Override
                    public void onSetSuccess() {
                        publicarSdp("/reservas/" + reservaId + "/answer", sessionDescription);
                    }
                    
                    @Override
                    public void onSetFailure(String error) {
                        runOnUiThread(() -> Toast.makeText(VideoCallActivity.this, 
                            "Error configurando respuesta: " + error, Toast.LENGTH_SHORT).show());
                    }
                }, sessionDescription);
            }
            
            @Override
            public void onCreateFailure(String error) {
                runOnUiThread(() -> Toast.makeText(VideoCallActivity.this, 
                    "Error creando respuesta: " + error, Toast.LENGTH_SHORT).show());
            }
        }, constraints);
    }

    private void esperarAnswer() {
        runOnUiThread(() -> Toast.makeText(this, "Esperando respuesta del alumno...", Toast.LENGTH_SHORT).show());
        new Thread(() -> {
            final int MAX_ATTEMPTS = 60; // 60 attempts * 1.5s = 90 seconds timeout
            final int POLL_INTERVAL_MS = 1500;
            int intentos = 0;
            while (running && intentos < MAX_ATTEMPTS) {
                try {
                    ApiClient.ApiResponse response = ApiClient.get("/reservas/" + reservaId + "/answer");
                    JSONObject json = ApiClient.parseJson(response.body);
                    if (response.code >= 200 && response.code < 300 && json != null && json.optString("sdp", "").length() > 0) {
                        String sdp = json.optString("sdp", "");
                        SessionDescription answer = new SessionDescription(SessionDescription.Type.ANSWER, sdp);
                        peerConnection.setRemoteDescription(new SimpleSdpObserver() {
                            @Override
                            public void onSetSuccess() {
                                runOnUiThread(() -> Toast.makeText(VideoCallActivity.this, 
                                    "Estableciendo conexión...", Toast.LENGTH_SHORT).show());
                            }
                            
                            @Override
                            public void onSetFailure(String error) {
                                runOnUiThread(() -> Toast.makeText(VideoCallActivity.this, 
                                    "Error procesando respuesta: " + error, Toast.LENGTH_SHORT).show());
                            }
                        }, answer);
                        return;
                    }
                    intentos++;
                    Thread.sleep(POLL_INTERVAL_MS);
                } catch (Exception e) {
                    break;
                }
            }
            if (intentos >= MAX_ATTEMPTS) {
                runOnUiThread(() -> {
                    Toast.makeText(VideoCallActivity.this, "Timeout esperando respuesta", Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    private void publicarSdp(String path, SessionDescription sessionDescription) {
        new Thread(() -> {
            try {
                java.util.Map<String, String> params = new java.util.HashMap<>();
                params.put("sdp", sessionDescription.description);
                ApiClient.postForm(path, params);
            } catch (Exception ignored) {
            }
        }).start();
    }

    private VideoCapturer createVideoCapturer() {
        Camera2Enumerator enumerator = new Camera2Enumerator(this);
        for (String deviceName : enumerator.getDeviceNames()) {
            if (enumerator.isFrontFacing(deviceName)) {
                CameraVideoCapturer capturer = enumerator.createCapturer(deviceName, null);
                if (capturer != null) return capturer;
            }
        }
        for (String deviceName : enumerator.getDeviceNames()) {
            if (!enumerator.isFrontFacing(deviceName)) {
                CameraVideoCapturer capturer = enumerator.createCapturer(deviceName, null);
                if (capturer != null) return capturer;
            }
        }
        return null;
    }

    private static class SimpleSdpObserver implements org.webrtc.SdpObserver {
        @Override public void onCreateSuccess(SessionDescription sessionDescription) { }
        @Override public void onSetSuccess() { }
        @Override public void onCreateFailure(String s) { }
        @Override public void onSetFailure(String s) { }
    }
}
