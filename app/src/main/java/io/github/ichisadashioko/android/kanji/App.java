package io.github.ichisadashioko.android.kanji;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import java.io.IOException;
import java.util.List;

import io.github.ichisadashioko.android.kanji.tflite.OldClassifier;
import io.github.ichisadashioko.android.kanji.tflite.Recognition;
import io.github.ichisadashioko.android.kanji.views.TouchCallback;
import io.github.ichisadashioko.android.kanji.views.HandwritingCanvas;
import io.github.ichisadashioko.android.kanji.views.ResultButton;

public class App extends Activity implements TouchCallback {
    public static final String KANJI_FONT_PATH = "fonts/HGKyokashotai_Medium.ttf";

    private HandwritingCanvas canvas;
    private OldClassifier tflite;
    private LinearLayout resultContainer;
    private int resultViewWidth;
    private EditText textRenderer;
    private boolean autoEvaluate;
    private boolean autoClear;
    private Typeface kanjiTypeface;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        canvas = (HandwritingCanvas) findViewById(R.id.canvas);
        resultContainer = (LinearLayout) findViewById(R.id.result_container);
        resultViewWidth = (int) getResources().getDimension(R.dimen.result_size);
        textRenderer = (EditText) findViewById(R.id.text_renderer);

        ToggleButton autoEvaluateToggleButton = (ToggleButton) findViewById(R.id.auto_evaluate);
        autoEvaluate = autoEvaluateToggleButton.isChecked();
        autoEvaluateToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                autoEvaluate = isChecked;
            }
        });

        ToggleButton autoClearToggleButton = (ToggleButton) findViewById(R.id.auto_clear);
        autoClear = autoClearToggleButton.isChecked();
        autoClearToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                autoClear = isChecked;
            }
        });

        canvas.touchCallback = this;

        try {
            tflite = new OldClassifier(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        kanjiTypeface = Typeface.createFromAsset(getApplicationContext().getAssets(), KANJI_FONT_PATH);
        textRenderer.setTypeface(kanjiTypeface);
        ResultButton.LABEL_FONT = kanjiTypeface;
    }

    public void clearCanvas(View view) {
        canvas.clearCanvas();
        if (resultContainer.getChildCount() > 0) {
            resultContainer.removeAllViews();
        }
    }

    private View createButtonFromResult(Recognition r, Bitmap image) {
        ResultButton btn = new ResultButton(this, null, r.title, r.confidence);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(resultViewWidth, LinearLayout.LayoutParams.MATCH_PARENT);
        btn.setLayoutParams(layoutParams);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textRenderer.setText(textRenderer.getText() + btn.label);
                textRenderer.setSelection(textRenderer.getText().length());
                if (autoClear) {
                    clearCanvas(v);
                }
            }
        });
        return btn;
    }

    public void runClassifier(View view) {
        if (canvas == null || tflite == null || resultContainer == null) {
            return;
        }

        long startTime = SystemClock.elapsedRealtime();
        Bitmap image = canvas.getImage();
        List<Recognition> results = tflite.recognizeImage(image);
        long evaluateDuration = SystemClock.elapsedRealtime() - startTime;
        System.out.println(String.format("Inference took %d ms.", evaluateDuration));

        if (resultContainer.getChildCount() > 0) {
            resultContainer.removeAllViews();
        }

        for (Recognition result : results) {
            resultContainer.addView(createButtonFromResult(result, image));
        }
    }

    public void copyTextToClipboard(View view) {
        if (textRenderer.getText().length() > 0) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("text copied from handwriting input", textRenderer.getText());
            clipboard.setPrimaryClip(clipData);
        }
    }

    public void clearText(View view) {
        textRenderer.setText("");
    }

    @Override
    public void onTouchEnd() {
        if (autoEvaluate) {
            runClassifier(null);
        }
    }
}