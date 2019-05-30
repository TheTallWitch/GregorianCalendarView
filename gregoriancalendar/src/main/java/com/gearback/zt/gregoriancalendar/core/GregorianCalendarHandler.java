package com.gearback.zt.gregoriancalendar.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.DrawableRes;
import android.support.annotation.RawRes;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.TimeZone;

import com.gearback.zt.calendarcore.core.Constants;
import com.gearback.zt.calendarcore.core.exceptions.DayOutOfRangeException;
import com.gearback.zt.calendarcore.core.interfaces.OnDayClickedListener;
import com.gearback.zt.calendarcore.core.interfaces.OnDayLongClickedListener;
import com.gearback.zt.calendarcore.core.interfaces.OnEventUpdateListener;
import com.gearback.zt.calendarcore.core.interfaces.OnMonthChangedListener;
import com.gearback.zt.calendarcore.core.models.AbstractDate;
import com.gearback.zt.calendarcore.core.models.CalendarEvent;
import com.gearback.zt.calendarcore.core.models.CivilDate;
import com.gearback.zt.calendarcore.core.models.Day;
import com.gearback.zt.calendarcore.core.models.IslamicDate;
import com.gearback.zt.calendarcore.core.models.PersianDate;
import com.gearback.zt.calendarcore.helpers.ArabicShaping;
import com.gearback.zt.calendarcore.helpers.DateConverter;
import com.gearback.zt.gregoriancalendar.R;

public class GregorianCalendarHandler {
    private final String TAG = GregorianCalendarHandler.class.getName();
    private Context mContext;
    private Typeface mTypeface;
    private Typeface mHeadersTypeface;

    private int mColorBackground = Color.BLACK;
    private int mColorHoliday = Color.RED;
    private int mColorHolidaySelected = Color.RED;
    private int mColorNormalDay = Color.WHITE;
    private int mColorNormalDaySelected = Color.BLUE;
    private int mColorDayName = Color.LTGRAY;
    private int mColorEventUnderline = Color.RED;

    private List<Integer> gregorianDaysInMonth = Arrays.asList(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31);

    @DrawableRes
    private int mSelectedDayBackground = R.drawable.circle_select;
    @DrawableRes
    private int mTodayBackground = R.drawable.circle_current_day;

    private float mDaysFontSize = 25;
    private float mHeadersFontSize = 20;

    private boolean mHighlightLocalEvents = true;
    private boolean mHighlightOfficialEvents = true;
    private boolean mHighlightAfghanistanEvents = true;
    private boolean mHighlightIranEvents = true;
    private boolean mHighlightAncientEvents = true;
    private boolean mHighlightIslamicEvents = true;
    private boolean mHighlightGregorianEvents = true;
    private boolean mHighlightAdEvents = true;

    private List<CalendarEvent> mOfficialEvents;
    private List<CalendarEvent> mLocalEvents;

    private OnDayClickedListener mOnDayClickedListener;
    private OnDayLongClickedListener mOnDayLongClickedListener;
    private OnMonthChangedListener mOnMonthChangedListener;
    private OnEventUpdateListener mOnEventUpdateListener;

    private String[] mMonthNames = {
            "ژانویه",
            "فوریه",
            "مارس",
            "آوریل",
            "مه",
            "ژوئن",
            "ژوئیه",
            "اوت",
            "سپتامبر",
            "اکتبر",
            "نوامبر",
            "دسامبر"
    };
    private String[] mWeekDaysNames = {
            "شنبه",
            "یک‌شنبه",
            "دوشنبه",
            "سه‌شنبه",
            "چهارشنبه",
            "پنج‌شنبه",
            "جمعه"
    };

    private GregorianCalendarHandler(Context context) {
        this.mContext = context;
        mLocalEvents = new ArrayList<>();
    }

    private static WeakReference<GregorianCalendarHandler> myWeakInstance;

    public static GregorianCalendarHandler getInstance(Context context) {
        if (myWeakInstance == null || myWeakInstance.get() == null) {
            myWeakInstance = new WeakReference<>(new GregorianCalendarHandler(context.getApplicationContext()));
        }
        return myWeakInstance.get();
    }

    /**
     * Text shaping is a essential thing on supporting Arabic script text on older Android versions.
     * It converts normal Arabic character to their presentation forms according to their position
     * on the text.
     *
     * @param text Arabic string
     * @return Shaped text
     */
    public String shape(String text) {
        return (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN)
                ? ArabicShaping.shape(text)
                : text;
    }

    private void initTypeface() {
        if (mTypeface == null) {
            mTypeface = Typeface.createFromAsset(mContext.getAssets(), Constants.FONT_PATH);
        }
        if (mHeadersTypeface == null) {
            mHeadersTypeface = Typeface.createFromAsset(mContext.getAssets(), Constants.FONT_PATH);
        }
    }

    public void setFont(TextView textView) {
        initTypeface();
        textView.setTypeface(mTypeface);
    }

    public void setFontAndShape(TextView textView) {
        setFont(textView);
        textView.setText(shape(textView.getText().toString()));
    }

    private char[] mPreferredDigits = Constants.PERSIAN_DIGITS;
    private boolean mIranTime;

    public CivilDate getToday() {
        return new CivilDate(makeCalendarFromDate(new Date()));
    }

    public Calendar makeCalendarFromDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        if (mIranTime) {
            calendar.setTimeZone(TimeZone.getTimeZone("Asia/Tehran"));
        }
        calendar.setTime(date);
        return calendar;
    }

    public String formatNumber(int number) {
        return formatNumber(Integer.toString(number));
    }

    public String formatNumber(String number) {
        if (mPreferredDigits == Constants.ARABIC_DIGITS)
            return number;

        StringBuilder sb = new StringBuilder();
        for (char i : number.toCharArray()) {
            if (Character.isDigit(i)) {
                sb.append(mPreferredDigits[Integer.parseInt(i + "")]);
            } else {
                sb.append(i);
            }
        }
        return sb.toString();
    }

    public String dateToString(AbstractDate date) {
        return formatNumber(date.getDayOfMonth()) + ' ' + getMonthName(date) + ' ' +
                formatNumber(date.getYear());
    }

    public String dayTitleSummary(CivilDate civilDate) {
        return getWeekDayName(civilDate) + Constants.PERSIAN_COMMA + " " + dateToString(civilDate);
    }

    public GregorianCalendarHandler setHighlightLocalEvents(boolean highlightLocalEvents) {
        mHighlightLocalEvents = highlightLocalEvents;
        return this;
    }

    public GregorianCalendarHandler setHighlightAfghanistanEvents(boolean highlightAfghanistanEvents) {
        mHighlightAfghanistanEvents = highlightAfghanistanEvents;
        return this;
    }

    public GregorianCalendarHandler setHighlightIranEvents(boolean highlightIranEvents) {
        mHighlightIranEvents = highlightIranEvents;
        return this;
    }

    public GregorianCalendarHandler setHighlightIslamicEvents(boolean highlightIslamicEvents) {
        mHighlightIslamicEvents = highlightIslamicEvents;
        return this;
    }

    public GregorianCalendarHandler setHighlightAncientEvents(boolean highlightAncientEvents) {
        mHighlightAncientEvents = highlightAncientEvents;
        return this;
    }

    public GregorianCalendarHandler setHighlightGregorianEvents(boolean highlightGregorianEvents) {
        mHighlightGregorianEvents = highlightGregorianEvents;
        return this;
    }

    public GregorianCalendarHandler setHighlightAdEvents(boolean highlightAdEvents) {
        mHighlightAdEvents = highlightAdEvents;
        return this;
    }

    public boolean isHighlightingOfficialEvents() {
        return mHighlightOfficialEvents;
    }

    public GregorianCalendarHandler setHighlightOfficialEvents(boolean highlightOfficialEvents) {
        mHighlightOfficialEvents = highlightOfficialEvents;
        return this;
    }

    public String[] monthsNamesOfCalendar(AbstractDate date) {
        return mMonthNames.clone();
    }

    public String getMonthName(AbstractDate date) {
        return monthsNamesOfCalendar(date)[date.getMonth() - 1];
    }

    public String getWeekDayName(AbstractDate date) {
        if (date instanceof IslamicDate)
            date = DateConverter.islamicToCivil((IslamicDate) date);
        else if (date instanceof PersianDate)
            date = DateConverter.persianToCivil((PersianDate) date);

        return mWeekDaysNames[date.getDayOfWeek() % 7];
    }

    private String readStream(InputStream is) {
        // http://stackoverflow.com/a/5445161
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private String readRawResource(@RawRes int res) {
        return readStream(mContext.getResources().openRawResource(res));
    }

    private List<CalendarEvent> readEventsFromJSON() {
        List<CalendarEvent> result = new ArrayList<>();
        String json = readRawResource(R.raw.events);
        try {
            JSONArray gregorianCalendar = new JSONObject(json).getJSONArray("gregorianCalendar");
            for (int i = 0; i < gregorianCalendar.length(); ++i) {
                JSONObject event = gregorianCalendar.getJSONObject(i);
                int month = event.getInt("month");
                int day = event.getInt("day");
                String title = event.getString("title");
                String desc = event.getString("description");
                String type = event.getString("type");
                boolean holiday = event.getBoolean("holiday");
                boolean obit = event.getBoolean("obit");
                result.add(new CalendarEvent(null, new CivilDate(-1, month, day), null, title, desc, holiday, obit, type));
            }

            JSONArray persianCalendar = new JSONObject(json).getJSONArray("persianCalendar");
            for (int i = 0; i < persianCalendar.length(); ++i) {
                JSONObject event = persianCalendar.getJSONObject(i);
                int month = event.getInt("month");
                int day = event.getInt("day");
                String title = event.getString("title");
                String desc = event.getString("description");
                String type = event.getString("type");
                boolean holiday = event.getBoolean("holiday");
                boolean obit = event.getBoolean("obit");
                result.add(new CalendarEvent(new PersianDate(-1, month, day), null, null, title, desc, holiday, obit, type));
            }

            JSONArray lunarCalendar = new JSONObject(json).getJSONArray("lunarCalendar");
            for (int i = 0; i < lunarCalendar.length(); ++i) {
                JSONObject event = lunarCalendar.getJSONObject(i);
                int month = event.getInt("month");
                int day = event.getInt("day");
                String title = event.getString("title");
                String desc = event.getString("description");
                String type = event.getString("type");
                boolean holiday = event.getBoolean("holiday");
                boolean obit = event.getBoolean("obit");
                result.add(new CalendarEvent(null, null, new IslamicDate(-1, month, day), title, desc, holiday, obit, type));
            }

        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
        return result;
    }

    public List<CalendarEvent> getOfficialEventsForDay(CivilDate day){
        if (mOfficialEvents == null)
            mOfficialEvents = readEventsFromJSON();

        return getEventsForDay(day, mOfficialEvents);
    }

    public List<CalendarEvent> getAllEventsForDay(CivilDate day) {
        List<CalendarEvent> events = getOfficialEventsForDay(day);
        List<CalendarEvent> localEvents = getLocalEventsForDay(day);
        events.addAll(localEvents);
        return events;
    }

    public String getEventsTitle(CivilDate day, boolean holiday) {
        String titles = "";
        boolean first = true;
        List<CalendarEvent> dayEvents = getAllEventsForDay(day);

        for (CalendarEvent event : dayEvents) {
            if (event.isHoliday() == holiday) {
                if (first) {
                    first = false;

                } else {
                    titles = titles + "\n";
                }
                titles = titles + event.getTitle();
            }
        }
        return titles;
    }

    public GregorianCalendarHandler setMonthNames(String[] monthNames) {
        mMonthNames = monthNames;
        return this;
    }

    public GregorianCalendarHandler setWeekDaysNames(String[] weekDaysNames) {
        mWeekDaysNames = weekDaysNames;
        return this;
    }

    public List<Day> getDays(int offset) {
        List<Day> days = new ArrayList<>();
        CivilDate civilDate = getToday();
        int month = civilDate.getMonth() - offset;
        month -= 1;
        int year = civilDate.getYear();

        year = year + (month / 12);
        month = month % 12;
        if (month < 0) {
            year -= 1;
            month += 12;
        }
        month += 1;
        civilDate.setMonth(month);
        civilDate.setYear(year);
        civilDate.setDayOfMonth(1);
        int monthLength = getMonthLength(year, month);

        int dayOfWeek = civilDate.getDayOfWeek() % 7;
 
        try {
            CivilDate today = getToday();
            Log.e("today", today.getYear() + "/" + today.getMonth() + "/" + today.getDayOfMonth());
            for (int i = 1; i <= monthLength; i++) {
                civilDate.setDayOfMonth(i);

                Day day = new Day();
                day.setNum(formatNumber(i));
                day.setDayOfWeek(dayOfWeek);

                if (dayOfWeek == 6 || !TextUtils.isEmpty(getEventsTitle(civilDate, true))) {
                    day.setHoliday(true);
                }

                if (mHighlightLocalEvents) {
                    if (getLocalEventsForDay(civilDate).size() > 0) {
                        day.setLocalEvent(true);
                    }
                }
                if (mHighlightOfficialEvents) {
                    boolean hasEvent = false;
                    List<CalendarEvent> events = getOfficialEventsForDay(civilDate);
                    for (CalendarEvent event: events) {
                        if (event.getType().equals("Afghanistan") && mHighlightAfghanistanEvents) {
                            hasEvent = true;
                            break;
                        }
                        else if (event.getType().equals("Iran") && mHighlightIranEvents) {
                            hasEvent = true;
                            break;
                        }
                        else if (event.getType().equals("Ancient Iran") && mHighlightAncientEvents) {
                            hasEvent = true;
                            break;
                        }
                        else if (event.getType().equals("Islamic Iran") && mHighlightIslamicEvents) {
                            hasEvent = true;
                            break;
                        }
                        else if (event.getType().equals("Islamic Afghanistan") && mHighlightIslamicEvents && mHighlightAfghanistanEvents) {
                            hasEvent = true;
                            break;
                        }
                        else if (event.getType().equals("Gregorian") && mHighlightGregorianEvents) {
                            hasEvent = true;
                            break;
                        }
                        else if (event.getType().equals("Ad") && mHighlightAdEvents) {
                            hasEvent = true;
                            break;
                        }
                    }
                    day.setEvent(hasEvent);
                }

                day.setCivilDate(civilDate.clone());

                if (civilDate.equals(today)) {
                    day.setToday(true);
                }

                days.add(day);
                dayOfWeek++;
                if (dayOfWeek == 7) {
                    dayOfWeek = 0;
                }
            }
        } catch (DayOutOfRangeException e) {
        }

        return days;
    }

    public boolean isIranTime() {
        return mIranTime;
    }

    public GregorianCalendarHandler setIranTime(boolean iranTime) {
        mIranTime = iranTime;
        return this;
    }

    public Typeface getTypeface() {
        return mTypeface;
    }

    public GregorianCalendarHandler setTypeface(Typeface typeface) {
        mTypeface = typeface;
        return this;
    }

    public int getColorBackground() {
        return mColorBackground;
    }

    public GregorianCalendarHandler setColorBackground(int colorBackground) {
        mColorBackground = colorBackground;
        return this;
    }

    public int getColorDayName() {
        return mColorDayName;
    }

    public GregorianCalendarHandler setColorDayName(int colorDayName) {
        mColorDayName = colorDayName;
        return this;
    }

    public int getColorHoliday() {
        return mColorHoliday;
    }

    public GregorianCalendarHandler setColorHoliday(int colorHoliday) {
        mColorHoliday = colorHoliday;
        return this;
    }

    public int getColorHolidaySelected() {
        return mColorHolidaySelected;
    }

    public GregorianCalendarHandler setColorHolidaySelected(int colorHolidaySelected) {
        mColorHolidaySelected = colorHolidaySelected;
        return this;
    }

    public GregorianCalendarHandler setColorNormalDay(int colorNormalDay) {
        mColorNormalDay = colorNormalDay;
        return this;
    }

    public int getColorNormalDay() {
        return mColorNormalDay;
    }

    public int getColorNormalDaySelected() {
        return mColorNormalDaySelected;
    }

    public GregorianCalendarHandler setColorNormalDaySelected(int colorNormalDaySelected) {
        mColorNormalDaySelected = colorNormalDaySelected;
        return this;
    }

    public int getColorEventUnderline() {
        return mColorEventUnderline;
    }

    public GregorianCalendarHandler setColorEventUnderline(int colorEventUnderline) {
        mColorEventUnderline = colorEventUnderline;
        return this;
    }

    public float getDaysFontSize() {
        return mDaysFontSize;
    }

    public GregorianCalendarHandler setDaysFontSize(float daysFontSize) {
        mDaysFontSize = daysFontSize;
        return this;
    }

    public float getHeadersFontSize() {
        return mHeadersFontSize;
    }

    public GregorianCalendarHandler setHeadersFontSize(float headersFontSize) {
        mHeadersFontSize = headersFontSize;
        return this;
    }

    public List<CalendarEvent> getLocalEvents() {
        return mLocalEvents;
    }

    public List<CalendarEvent> getLocalEventsForDay(CivilDate day){
        return getEventsForDay(day,mLocalEvents);
    }

    private List<CalendarEvent> getEventsForDay(CivilDate day, List<CalendarEvent> events){
        List<CalendarEvent> result = new ArrayList<>();
        for (CalendarEvent calendarEvent : events) {
            if (calendarEvent.getCivilDate() != null) {
                if (calendarEvent.getCivilDate().equals(day)) {
                    result.add(calendarEvent);
                }
            }
            if (calendarEvent.getPersianDate() != null) {
                if (calendarEvent.getPersianDate().equals(DateConverter.civilToPersian(day))) {
                    result.add(calendarEvent);
                }
            }
            if (calendarEvent.getIslamicDate() != null) {
                if (calendarEvent.getIslamicDate().equals(DateConverter.civilToIslamic(day, 0))) {
                    result.add(calendarEvent);
                }
            }
        }
        return result;
    }

    public void addLocalEvent(CalendarEvent event) {
        mLocalEvents.add(event);
    }

    public GregorianCalendarHandler setOnDayClickedListener(OnDayClickedListener onDayClickedListener) {
        mOnDayClickedListener = onDayClickedListener;
        return this;
    }

    public OnDayClickedListener getOnDayClickedListener() {
        return mOnDayClickedListener;
    }

    public GregorianCalendarHandler setOnDayLongClickedListener(OnDayLongClickedListener onDayLongClickedListener) {
        mOnDayLongClickedListener = onDayLongClickedListener;
        return this;
    }

    public OnDayLongClickedListener getOnDayLongClickedListener() {
        return mOnDayLongClickedListener;
    }

    public GregorianCalendarHandler setOnMonthChangedListener(OnMonthChangedListener onMonthChangedListener) {
        mOnMonthChangedListener = onMonthChangedListener;
        return this;
    }

    public OnEventUpdateListener getOnEventUpdateListener() {
        return mOnEventUpdateListener;
    }

    public GregorianCalendarHandler setOnEventUpdateListener(OnEventUpdateListener onEventUpdateListener) {
        mOnEventUpdateListener = onEventUpdateListener;
        return this;
    }

    public OnMonthChangedListener getOnMonthChangedListener() {
        return mOnMonthChangedListener;
    }

    public int getTodayBackground() {
        return mTodayBackground;
    }

    public GregorianCalendarHandler setTodayBackground(int todayBackground) {
        mTodayBackground = todayBackground;
        return this;
    }

    public int getSelectedDayBackground() {
        return mSelectedDayBackground;
    }
    public GregorianCalendarHandler setSelectedDayBackground(int selectedDayBackground) {
        mSelectedDayBackground = selectedDayBackground;
        return this;
    }

    public Typeface getHeadersTypeface() {
        return mHeadersTypeface;
    }

    public GregorianCalendarHandler setHeadersTypeface(Typeface headersTypeface) {
        mHeadersTypeface = headersTypeface;
        return this;
    }

    public int getMonthLength(int year, int month) {
        CivilDate date = new CivilDate(year, month, 1);
        if (date.isLeapYear() && month == 2) {
            return gregorianDaysInMonth.get(month - 1) + 1;
        }
        else {
            return gregorianDaysInMonth.get(month - 1);
        }
    }
}
