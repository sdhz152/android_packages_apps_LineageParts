/*
 * Copyright (C) 2014-2015 The CyanogenMod Project
 *               2017-2020 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lineageos.lineageparts.statusbar;

import android.app.AlertDialog;
import android.content.Context;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.app.Dialog;
import android.app.DialogFragment;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.text.TextUtils;
import android.util.ArraySet;
import android.view.View;
import android.widget.EditText;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.ListPreference;

import lineageos.preference.LineageSystemSettingListPreference;
import lineageos.providers.LineageSettings;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;
import org.lineageos.lineageparts.search.BaseSearchIndexProvider;
import org.lineageos.lineageparts.search.Searchable;
import org.lineageos.lineageparts.utils.DeviceUtils;

import java.util.Set;
import java.util.Date;

public class StatusBarSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener, Searchable {

    private static final String CATEGORY_BATTERY = "status_bar_battery_key";
    private static final String CATEGORY_CLOCK = "status_bar_clock_key";

    private static final String ICON_BLACKLIST = "icon_blacklist";

    private static final String STATUS_BAR_CLOCK_STYLE = "status_bar_clock";
    private static final String STATUS_BAR_AM_PM = "status_bar_am_pm";
    private static final String STATUS_BAR_BATTERY_STYLE = "status_bar_battery_style";
    private static final String STATUS_BAR_SHOW_BATTERY_PERCENT = "status_bar_show_battery_percent";
    private static final String STATUS_BAR_QUICK_QS_PULLDOWN = "qs_quick_pulldown";
    private static final String STATUS_BAR_DATE = "status_bar_date";
    private static final String STATUS_BAR_DATE_STYLE = "status_bar_date_style";
    private static final String STATUS_BAR_DATE_FORMAT = "status_bar_date_format";

    private static final int STATUS_BAR_BATTERY_STYLE_TEXT = 2;

    private static final int PULLDOWN_DIR_NONE = 0;
    private static final int PULLDOWN_DIR_RIGHT = 1;
    private static final int PULLDOWN_DIR_LEFT = 2;
    public static final int CLOCK_DATE_STYLE_LOWERCASE = 1;
    public static final int CLOCK_DATE_STYLE_UPPERCASE = 2;
    private static final int CUSTOM_CLOCK_DATE_FORMAT_INDEX = 18;

    private static final String NETWORK_TRAFFIC_SETTINGS = "network_traffic_settings";

    private LineageSystemSettingListPreference mQuickPulldown;
    private LineageSystemSettingListPreference mStatusBarClock;
    private LineageSystemSettingListPreference mStatusBarAmPm;
    private LineageSystemSettingListPreference mStatusBarBattery;
    private LineageSystemSettingListPreference mStatusBarBatteryShowPercent;

    private PreferenceCategory mStatusBarBatteryCategory;
    private PreferenceCategory mStatusBarClockCategory;
    private PreferenceScreen mNetworkTrafficPref;

    private ListPreference mStatusBarDate;
    private ListPreference mStatusBarDateStyle;
    private ListPreference mStatusBarDateFormat;

    private boolean mHasNotch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.status_bar_settings);
        final ContentResolver resolver = getActivity().getContentResolver();

        mNetworkTrafficPref = findPreference(NETWORK_TRAFFIC_SETTINGS);

        mHasNotch = DeviceUtils.hasNotch(getActivity());
        if (mHasNotch) {
            getPreferenceScreen().removePreference(mNetworkTrafficPref);
        }

        mStatusBarAmPm = findPreference(STATUS_BAR_AM_PM);
        mStatusBarClock = findPreference(STATUS_BAR_CLOCK_STYLE);
        mStatusBarClock.setOnPreferenceChangeListener(this);

        mStatusBarClockCategory = getPreferenceScreen().findPreference(CATEGORY_CLOCK);

        mStatusBarBatteryShowPercent = findPreference(STATUS_BAR_SHOW_BATTERY_PERCENT);
        mStatusBarBattery = findPreference(STATUS_BAR_BATTERY_STYLE);
        mStatusBarBattery.setOnPreferenceChangeListener(this);
        enableStatusBarBatteryDependents(mStatusBarBattery.getIntValue(2));

        mStatusBarBatteryCategory = getPreferenceScreen().findPreference(CATEGORY_BATTERY);

        mQuickPulldown = findPreference(STATUS_BAR_QUICK_QS_PULLDOWN);
        mQuickPulldown.setOnPreferenceChangeListener(this);
        updateQuickPulldownSummary(mQuickPulldown.getIntValue(0));

        mStatusBarDate = (ListPreference) findPreference(STATUS_BAR_DATE);
	    int clockDateDisplay = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_DATE, 0);
        mStatusBarDate.setValue(String.valueOf(clockDateDisplay));
        mStatusBarDate.setSummary(mStatusBarDate.getEntry());
        mStatusBarDate.setOnPreferenceChangeListener(this);
 	    mStatusBarDateStyle = (ListPreference) findPreference(STATUS_BAR_DATE_STYLE);
        int clockDateStyle = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_DATE_STYLE, 0);
        mStatusBarDateStyle.setValue(String.valueOf(clockDateStyle));
        mStatusBarDateStyle.setSummary(mStatusBarDateStyle.getEntry());
        mStatusBarDateStyle.setOnPreferenceChangeListener(this);
         mStatusBarDateFormat = (ListPreference) findPreference(STATUS_BAR_DATE_FORMAT);
        mStatusBarDateFormat.setOnPreferenceChangeListener(this);
        if (mStatusBarDateFormat.getValue() == null) {
            mStatusBarDateFormat.setValue("EEEE");
        }
        parseClockDateFormats();
    }

    @Override
    public void onResume() {
        super.onResume();

        final String curIconBlacklist = Settings.Secure.getString(getContext().getContentResolver(),
                ICON_BLACKLIST);

        if (TextUtils.delimitedStringContains(curIconBlacklist, ',', "clock")) {
            getPreferenceScreen().removePreference(mStatusBarClockCategory);
        } else {
            getPreferenceScreen().addPreference(mStatusBarClockCategory);
        }

        if (TextUtils.delimitedStringContains(curIconBlacklist, ',', "battery")) {
            getPreferenceScreen().removePreference(mStatusBarBatteryCategory);
        } else {
            getPreferenceScreen().addPreference(mStatusBarBatteryCategory);
        }

        if (DateFormat.is24HourFormat(getActivity())) {
            mStatusBarAmPm.setEnabled(false);
            mStatusBarAmPm.setSummary(R.string.status_bar_am_pm_info);
        }

        final boolean disallowCenteredClock = mHasNotch || getNetworkTrafficStatus() != 0;

        // Adjust status bar preferences for RTL
        if (getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            if (disallowCenteredClock) {
                mStatusBarClock.setEntries(R.array.status_bar_clock_position_entries_notch_rtl);
                mStatusBarClock.setEntryValues(R.array.status_bar_clock_position_values_notch_rtl);
            } else {
                mStatusBarClock.setEntries(R.array.status_bar_clock_position_entries_rtl);
                mStatusBarClock.setEntryValues(R.array.status_bar_clock_position_values_rtl);
            }
            mQuickPulldown.setEntries(R.array.status_bar_quick_qs_pulldown_entries_rtl);
            mQuickPulldown.setEntryValues(R.array.status_bar_quick_qs_pulldown_values_rtl);
        } else if (disallowCenteredClock) {
            mStatusBarClock.setEntries(R.array.status_bar_clock_position_entries_notch);
            mStatusBarClock.setEntryValues(R.array.status_bar_clock_position_values_notch);
        } else {
            mStatusBarClock.setEntries(R.array.status_bar_clock_position_entries);
            mStatusBarClock.setEntryValues(R.array.status_bar_clock_position_values);
        }

        // Disable network traffic preferences if clock is centered in the status bar
        updateNetworkTrafficStatus(getClockPosition());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
		AlertDialog dialog;
        if (preference == mStatusBarDate) {
            int clockDateDisplay = Integer.valueOf((String) newValue);
            int index = mStatusBarDate.findIndexOfValue((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUS_BAR_DATE, clockDateDisplay);
            mStatusBarDate.setSummary(mStatusBarDate.getEntries()[index]);
            if (clockDateDisplay == 0) {
                mStatusBarDateStyle.setEnabled(false);
                mStatusBarDateFormat.setEnabled(false);
            } else {
                mStatusBarDateStyle.setEnabled(true);
                mStatusBarDateFormat.setEnabled(true);
            }
            return true;
        } else if (preference == mStatusBarDateStyle) {
            int clockDateStyle = Integer.valueOf((String) newValue);
            int index = mStatusBarDateStyle.findIndexOfValue((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUS_BAR_DATE_STYLE, clockDateStyle);
            mStatusBarDateStyle.setSummary(mStatusBarDateStyle.getEntries()[index]);
            parseClockDateFormats();
            return true;
        } else if (preference == mStatusBarDateFormat) {
            int index = mStatusBarDateFormat.findIndexOfValue((String) newValue);
             if (index == CUSTOM_CLOCK_DATE_FORMAT_INDEX) {
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                alert.setTitle(R.string.status_bar_date_string_edittext_title);
                alert.setMessage(R.string.status_bar_date_string_edittext_summary);
                 final EditText input = new EditText(getActivity());
                String oldText = Settings.System.getString(
                    getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_DATE_FORMAT);
                if (oldText != null) {
                    input.setText(oldText);
                }
                alert.setView(input);
                 alert.setPositiveButton(R.string.menu_save, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int whichButton) {
                        String value = input.getText().toString();
                        if (value.equals("")) {
                            return;
                        }
                        Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.STATUS_BAR_DATE_FORMAT, value);
                         return;
                    }
                });
                 alert.setNegativeButton(R.string.menu_cancel,
                    new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int which) {
                        return;
                    }
                });
                dialog = alert.create();
                dialog.show();
            } else {
                if ((String) newValue != null) {
                    Settings.System.putString(getActivity().getContentResolver(),
                        Settings.System.STATUS_BAR_DATE_FORMAT, (String) newValue);
                }
            }
            return true;
        }
        int value = Integer.parseInt((String) newValue);
        String key = preference.getKey();
        switch (key) {
            case STATUS_BAR_QUICK_QS_PULLDOWN:
                updateQuickPulldownSummary(value);
                break;
            case STATUS_BAR_CLOCK_STYLE:
                updateNetworkTrafficStatus(value);
                break;
            case STATUS_BAR_BATTERY_STYLE:
                enableStatusBarBatteryDependents(value);
                break;
        }
        return true;
    }

    private void enableStatusBarBatteryDependents(int batteryIconStyle) {
        mStatusBarBatteryShowPercent.setEnabled(batteryIconStyle != STATUS_BAR_BATTERY_STYLE_TEXT);
    }

    private void parseClockDateFormats() {
        // Parse and repopulate mStatusBarDateFormat's entries based on current date.
        String[] dateEntries = getResources().getStringArray(R.array.status_bar_date_format_entries_values);
        CharSequence parsedDateEntries[];
        parsedDateEntries = new String[dateEntries.length];
        Date now = new Date();
         int lastEntry = dateEntries.length - 1;
        int dateFormat = Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.STATUS_BAR_DATE_STYLE, 0);
        for (int i = 0; i < dateEntries.length; i++) {
            if (i == lastEntry) {
                parsedDateEntries[i] = dateEntries[i];
            } else {
                String newDate;
                CharSequence dateString = DateFormat.format(dateEntries[i], now);
                if (dateFormat == CLOCK_DATE_STYLE_LOWERCASE) {
                    newDate = dateString.toString().toLowerCase();
                } else if (dateFormat == CLOCK_DATE_STYLE_UPPERCASE) {
                    newDate = dateString.toString().toUpperCase();
                } else {
                    newDate = dateString.toString();
                }
                 parsedDateEntries[i] = newDate;
            }
        }
        mStatusBarDateFormat.setEntries(parsedDateEntries);
    }

    private void updateQuickPulldownSummary(int value) {
        String summary="";
        switch (value) {
            case PULLDOWN_DIR_NONE:
                summary = getResources().getString(
                    R.string.status_bar_quick_qs_pulldown_off);
                break;

            case PULLDOWN_DIR_LEFT:
            case PULLDOWN_DIR_RIGHT:
                summary = getResources().getString(
                    R.string.status_bar_quick_qs_pulldown_summary,
                    getResources().getString(value == PULLDOWN_DIR_LEFT
                        ? R.string.status_bar_quick_qs_pulldown_summary_left
                        : R.string.status_bar_quick_qs_pulldown_summary_right));
                break;
        }
        mQuickPulldown.setSummary(summary);
    }

    private void updateNetworkTrafficStatus(int clockPosition) {
        if (mHasNotch) {
            // Unconditional no network traffic for you
            return;
        }

        boolean isClockCentered = clockPosition == 1;
        mNetworkTrafficPref.setEnabled(!isClockCentered);
        mNetworkTrafficPref.setSummary(getResources().getString(isClockCentered ?
                R.string.network_traffic_disabled_clock :
                R.string.network_traffic_settings_summary
        ));
    }

    private int getNetworkTrafficStatus() {
        return LineageSettings.Secure.getInt(getActivity().getContentResolver(),
                LineageSettings.Secure.NETWORK_TRAFFIC_MODE, 0);
    }

    private int getClockPosition() {
        return LineageSettings.System.getInt(getActivity().getContentResolver(),
                STATUS_BAR_CLOCK_STYLE, 2);
    }

    public static final Searchable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

        @Override
        public Set<String> getNonIndexableKeys(Context context) {
            final Set<String> result = new ArraySet<String>();

            if (DeviceUtils.hasNotch(context)) {
                result.add(NETWORK_TRAFFIC_SETTINGS);
            }
            return result;
        }
    };
}
