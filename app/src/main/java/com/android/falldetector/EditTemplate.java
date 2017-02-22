package com.android.falldetector;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by SS-WQY on 2017/2/20.
 */

public class EditTemplate extends Activity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_template);

        Intent intent = getIntent();
    }
}
