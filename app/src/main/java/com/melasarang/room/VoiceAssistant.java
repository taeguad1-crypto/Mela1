package com.melasarang.room;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

/**
 * Activity-scoped speech loop. It intentionally does not use an accessibility service or
 * draw-over-other-apps permission: WebView pages remain inside 멜라루카 사랑방, so native controls stay
 * available without exposing untrusted web pages to an Android JavaScript bridge.
 */
public final class VoiceAssistant implements RecognitionListener, TextToSpeech.OnInitListener {

    public interface Callback {
        void onTranscript(String transcript);
        void onListeningState(boolean listening);
        void onAudioLevel(float normalizedLevel);
        void onVoiceUnavailable(String reason);
    }

    private final Activity activity;
    private final Callback callback;
    private final Handler main = new Handler(Looper.getMainLooper());
    private SpeechRecognizer recognizer;
    private TextToSpeech speaker;
    private boolean desiredListening = true;
    private boolean hostResumed;
    private boolean listening;
    private boolean speaking;
    private boolean speechMuted;
    private boolean ttsReady;
    private int retryCount;
    private String activeUtteranceId;
    private Runnable pendingAfterSpeech;

    public VoiceAssistant(Activity activity, Callback callback) {
        this.activity = activity;
        this.callback = callback;
        try {
            this.speaker = new TextToSpeech(activity.getApplicationContext(), this);
            this.speaker.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String utteranceId) { speaking = true; }
                @Override public void onError(String utteranceId) { finishSpeaking(utteranceId); }
                @Override public void onDone(String utteranceId) { finishSpeaking(utteranceId); }
            });
        } catch (RuntimeException ttsError) {
            this.speaker = null;
            this.ttsReady = false;
            callback.onVoiceUnavailable("음성 답변 서비스를 준비하지 못했습니다. 음성 명령은 계속 사용할 수 있습니다.");
        }
    }

    @Override public void onInit(int status) {
        ttsReady = status == TextToSpeech.SUCCESS;
        if (ttsReady && speaker != null) {
            try {
                speaker.setLanguage(Locale.KOREA);
                speaker.setSpeechRate(1.0f);
            } catch (RuntimeException error) {
                ttsReady = false;
            }
        }
    }

    public void onHostResume() {
        hostResumed = true;
        if (desiredListening) scheduleStart(250);
    }

    public void onHostPause() {
        hostResumed = false;
        cancelRecognition();
    }

    public boolean isEnabled() {
        return desiredListening;
    }

    public void resumeCompletely() {
        desiredListening = true;
        retryCount = 0;
        if (hostResumed) scheduleStart(100);
    }

    public void stopCompletely() {
        desiredListening = false;
        main.removeCallbacksAndMessages(null);
        cancelRecognition();
        if (speaker != null) speaker.stop();
    }

    public void setSpeechMuted(boolean muted) {
        speechMuted = muted;
        if (muted && speaker != null) speaker.stop();
    }

    public void respond(String text) {
        respond(text, null);
    }

    public void respond(String text, Runnable afterSpeech) {
        cancelRecognition();
        if (text == null || text.trim().isEmpty() || speechMuted || !ttsReady) {
            if (afterSpeech != null) afterSpeech.run();
            if (desiredListening && hostResumed) scheduleStart(250);
            return;
        }

        speaking = true;
        String id = "sarangbang-" + UUID.randomUUID();
        activeUtteranceId = id;
        pendingAfterSpeech = afterSpeech;
        Bundle params = new Bundle();
        try {
            speaker.speak(text, TextToSpeech.QUEUE_FLUSH, params, id);
        } catch (RuntimeException error) {
            speaking = false;
            activeUtteranceId = null;
            pendingAfterSpeech = null;
            if (afterSpeech != null) afterSpeech.run();
            if (desiredListening && hostResumed) scheduleStart(250);
            return;
        }

        // Some vendor TTS engines omit completion callbacks. This watchdog restores listening.
        main.postDelayed(() -> {
            if (id.equals(activeUtteranceId)) finishSpeaking(id);
        }, Math.max(2200, text.length() * 140L));
    }

    public void startListeningNow() {
        desiredListening = true;
        cancelRecognition();
        startListeningInternal();
    }

    private void finishSpeaking(String utteranceId) {
        main.post(() -> {
            if (!speaking || utteranceId == null || !utteranceId.equals(activeUtteranceId)) return;
            speaking = false;
            Runnable completion = pendingAfterSpeech;
            pendingAfterSpeech = null;
            activeUtteranceId = null;
            if (completion != null) completion.run();
            if (desiredListening && hostResumed) scheduleStart(250);
        });
    }

    private void ensureRecognizer() {
        if (recognizer != null) return;
        try {
            if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
                callback.onVoiceUnavailable("이 기기에서 음성 인식 서비스를 사용할 수 없습니다.");
                return;
            }
            recognizer = SpeechRecognizer.createSpeechRecognizer(activity);
            recognizer.setRecognitionListener(this);
        } catch (RuntimeException recognitionError) {
            recognizer = null;
            callback.onVoiceUnavailable("음성 인식 서비스를 시작하지 못했습니다. Google 음성 서비스를 확인해 주세요.");
        }
    }

    private void startListeningInternal() {
        if (!hostResumed || !desiredListening || speaking || listening) return;
        ensureRecognizer();
        if (recognizer == null) return;

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 800L);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 500L);
        try {
            recognizer.startListening(intent);
            listening = true;
            callback.onListeningState(true);
        } catch (RuntimeException error) {
            listening = false;
            callback.onListeningState(false);
            scheduleStart(800);
        }
    }

    private void scheduleStart(long delayMs) {
        main.removeCallbacks(startRunnable);
        main.postDelayed(startRunnable, delayMs);
    }

    private final Runnable startRunnable = this::startListeningInternal;

    private void cancelRecognition() {
        main.removeCallbacks(startRunnable);
        if (recognizer != null) {
            try { recognizer.cancel(); } catch (RuntimeException ignored) { }
        }
        listening = false;
        callback.onListeningState(false);
    }

    public void destroy() {
        desiredListening = false;
        main.removeCallbacksAndMessages(null);
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
        if (speaker != null) {
            try {
                speaker.stop();
                speaker.shutdown();
            } catch (RuntimeException ignored) { }
            speaker = null;
        }
    }

    @Override public void onReadyForSpeech(Bundle params) {
        retryCount = 0;
        callback.onListeningState(true);
    }
    @Override public void onBeginningOfSpeech() { }
    @Override public void onRmsChanged(float rmsdB) {
        callback.onAudioLevel(Math.max(0f, Math.min(1f, (rmsdB + 2f) / 12f)));
    }
    @Override public void onBufferReceived(byte[] buffer) { }
    @Override public void onEndOfSpeech() {
        listening = false;
        callback.onListeningState(false);
    }
    @Override public void onError(int error) {
        listening = false;
        callback.onListeningState(false);
        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
            callback.onVoiceUnavailable("마이크 권한이 필요합니다.");
            return;
        }
        retryCount = Math.min(5, retryCount + 1);
        if (desiredListening && hostResumed) scheduleStart(350L + retryCount * 180L);
    }
    @Override public void onResults(Bundle results) {
        listening = false;
        callback.onListeningState(false);
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) callback.onTranscript(matches.get(0));
        else if (desiredListening && hostResumed) scheduleStart(350);
    }
    @Override public void onPartialResults(Bundle partialResults) { }
    @Override public void onEvent(int eventType, Bundle params) { }
}
