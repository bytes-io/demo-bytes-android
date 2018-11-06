package amiin.bazouk.application.com.demo_bytes_android;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class AmountActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_amount);

        findViewById(R.id.ok).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent result = new Intent();
                long amount = Long.parseLong(((EditText)findViewById(R.id.amount)).getText().toString());
                result.putExtra(MainActivity.AMOUNT_INTENT, amount);
                setResult(RESULT_OK, result);
                finish();
            }
        });

        findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }
}
