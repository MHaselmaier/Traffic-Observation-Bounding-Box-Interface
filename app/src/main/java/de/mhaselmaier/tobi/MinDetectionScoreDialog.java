package de.mhaselmaier.tobi;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.NumberPicker;

public class MinDetectionScoreDialog extends Dialog
{
    private Context context;
    private Button minDetectionScoreButton;
    private NumberPicker picker;

    private TobiNetwork tobi;

    public MinDetectionScoreDialog(Context context, Button minDetectionScoreButton, TobiNetwork tobi)
    {
        super(context);

        this.context = context;
        this.minDetectionScoreButton = minDetectionScoreButton;
        this.tobi = tobi;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.min_detection_score_dialog);

        this.picker = findViewById(R.id.min_detection_score);
        this.picker.setMinValue(0);
        this.picker.setMaxValue(100);

        Button cancel = findViewById(R.id.cancel);
        cancel.setOnClickListener((v) -> dismiss());

        Button accept = findViewById(R.id.accept);
        accept.setOnClickListener((v) -> handleAccept());
    }

    @Override
    public void show()
    {
        super.show();
        this.picker.setValue((int)(this.tobi.getMinDetectionScore() * 100));
    }

    private void handleAccept()
    {
        this.minDetectionScoreButton.setText(this.context.getString(R.string.min_detection_score, this.picker.getValue()));
        this.tobi.setMinDetectionScore(this.picker.getValue() / 100f);
        dismiss();
    }
}
