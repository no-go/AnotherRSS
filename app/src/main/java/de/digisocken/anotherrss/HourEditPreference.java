package de.digisocken.anotherrss;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

public class HourEditPreference extends EditTextPreference {

    public HourEditPreference(Context context) {
        super(context);
    }

    public HourEditPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HourEditPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        return String.valueOf(getPersistedInt(0));
    }

    @Override
    protected boolean persistString(String value) {
        if (value.equals("")) value="0";
        int val = Integer.valueOf(value);
        if (val > 23 || val < 0) val = 0;
        return persistInt(val);
    }
}
