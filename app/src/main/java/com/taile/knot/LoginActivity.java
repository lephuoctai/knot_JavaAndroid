package com.taile.knot;

import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.taile.knot.data.PrototypeStore;
import com.taile.knot.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {
    public static final String EXTRA_CHOOSE_JOURNEY = "com.taile.knot.CHOOSE_JOURNEY";
    private ActivityLoginBinding binding;
    private PrototypeStore store;
    private boolean journeyStep;
    private final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
    private boolean busy;
    private final ActivityResultLauncher<Intent> accountPicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String email = result.getData().getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (email != null) {
                        binding.email.setText(email);
                        continueWithEmail();
                    }
                }
            });
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        store = new PrototypeStore(this);
        journeyStep = store.hasEmail() && (state == null || state.getBoolean("journeyStep", true));
        binding.signIn.setOnClickListener(v -> {
            if (journeyStep) connect(); else continueWithEmail();
        });
        binding.secondaryAction.setOnClickListener(v -> {
            if (journeyStep) {
                execute(() -> {
                    if (!store.createJourney()) throw new IllegalStateException(getString(R.string.save_failed));
                }, this::openTimeline);
            } else {
                try {
                    accountPicker.launch(AccountManager.newChooseAccountIntent(null, null,
                            new String[]{"com.google"}, null, null, null, null));
                } catch (android.content.ActivityNotFoundException error) {
                    showError(R.string.google_no_account_picker);
                }
            }
        });
        renderStep();
    }
    private void continueWithEmail() {
        String email = binding.email.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.inputLayout.setError(getString(R.string.login_invalid_email)); return;
        }
        execute(() -> {
            if (!store.setEmail(email)) throw new IllegalStateException(getString(R.string.save_failed));
        }, () -> { journeyStep = true; binding.email.setText(""); renderStep(); });
    }
    private void renderStep() {
        binding.inputLayout.setError(null);
        binding.status.setVisibility(View.GONE);
        binding.inputLayout.setHint(getString(journeyStep ? R.string.journey_label : R.string.login_email));
        binding.email.setHint(journeyStep ? R.string.journey_hint : R.string.login_email_hint);
        binding.email.setInputType(journeyStep ? android.text.InputType.TYPE_CLASS_TEXT
                : android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        binding.signIn.setText(journeyStep ? R.string.journey_connect : R.string.login_action);
        binding.secondaryAction.setText(journeyStep ? R.string.journey_create : R.string.login_google);
    }
    private void connect() {
        final String code = binding.email.getText().toString().trim();
        execute(() -> {
            if (!store.joinJourney(code)) throw new IllegalStateException(getString(R.string.journey_not_found));
        }, this::openTimeline);
    }
    private void execute(Runnable action, Runnable success) {
        if (busy) return;
        busy = true;
        binding.signIn.setEnabled(false); binding.secondaryAction.setEnabled(false);
        executor.execute(() -> {
            String error = null;
            try { action.run(); } catch (RuntimeException failure) { error = failure.getMessage(); }
            final String message = error;
            runOnUiThread(() -> {
                if (isDestroyed() || isFinishing()) return;
                busy = false;
                binding.signIn.setEnabled(true); binding.secondaryAction.setEnabled(true);
                if (message == null) success.run();
                else { binding.status.setVisibility(View.VISIBLE); binding.status.setText(message); }
            });
        });
    }
    private void showError(int message) {
        binding.status.setVisibility(View.VISIBLE); binding.status.setText(message);
    }
    private void openTimeline() {
        startActivity(new Intent(this, MainActivity.class)); finish();
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putBoolean("journeyStep", journeyStep); super.onSaveInstanceState(out);
    }
    @Override protected void onDestroy() { executor.shutdownNow(); super.onDestroy(); }
}
