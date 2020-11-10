//TODO
//Text input string

package uk.lgl.modmenu;

import android.animation.ArgbEvaluator;
import android.animation.TimeAnimator;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.text.Html;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import uk.lgl.NativeToast;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.widget.RelativeLayout.ALIGN_PARENT_RIGHT;

public class FloatingModMenuService extends Service {
    //********** Here you can easly change the menu appearance **********//
    private final String TAG = "Mod_Menu"; //Tag for logcat
    private final int TEXT_COLOR = Color.parseColor("#82CAFD");
    private final int TEXT_COLOR_2 = Color.parseColor("#FFFFFF");
    private final int BTN_COLOR = Color.parseColor("#1C262D");
    private final int MENU_BG_COLOR = Color.parseColor("#DD1C2A35"); //#AARRGGBB
    private final int MENU_FEATURE_BG_COLOR = Color.parseColor("#FF171E24"); //#AARRGGBB
    private final int MENU_WIDTH = 290;
    private final int MENU_HEIGHT = 210;
    private final float MENU_CORNER = 20f;
    private final int ICON_SIZE = 50;
    private final float ICON_ALPHA = 0.7f; //Transparent
    //********************************************************************//

    //Some fields
    private MediaPlayer FXPlayer;
    private GradientDrawable gdMenuBody, gdAnimation = new GradientDrawable();
    public RelativeLayout mCollapsed, mRootContainer;
    public LinearLayout mExpanded, patches, mSettings;
    public WindowManager mWindowManager;
    public WindowManager.LayoutParams params;
    private ImageView startimage;
    private FrameLayout rootFrame;
    private AlertDialog alert;
    private EditText edittextvalue;
    private ScrollView scrollView;

    //For alert dialog
    private TextView inputFieldTextView;
    private String inputFieldFeatureName;
    private int inputFieldFeatureNum;
    private EditTextValue inputFieldTxtValue;

    boolean soundDelayed;
    public String cacheDir;

    LinearLayout.LayoutParams scrlLLExpanded, scrlLL;

    //initialize methods from the native library
    public static native void LoadSounds(String dir);

    private native String Title();

    private native String Heading();

    private native String Icon();

    private native String IconWebViewData();

    private native String[] getFeatureList();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //When this Class is called the code in this function will be executed
    @Override
    public void onCreate() {
        super.onCreate();
        Preferences.context = this;
        cacheDir = getCacheDir().getPath() + "/";
        //Log.d(TAG, "cache dir " + cacheDir);
        LoadSounds(cacheDir);
        //A little message for the user when he opens the app
        NativeToast.makeText(this, 0);

        //Create the menu
        initFloating();
        initAlertDiag();

        //Start the Gradient Animation
        startAnimation();

        //Create a handler for this Class
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            public void run() {
                Thread();
                handler.postDelayed(this, 1000);
            }
        });
    }

    //Here we write the code for our Menu
    // Reference: https://www.androidhive.info/2016/11/android-floating-widget-like-facebook-chat-head/
    private void initFloating() {
        rootFrame = new FrameLayout(this); // Global markup
        rootFrame.setOnTouchListener(onTouchListener());
        mRootContainer = new RelativeLayout(this); // Markup on which two markups of the icon and the menu itself will be placed
        mCollapsed = new RelativeLayout(this); // Markup of the icon (when the menu is minimized)
        mCollapsed.setVisibility(View.VISIBLE);
        mCollapsed.setAlpha(ICON_ALPHA);

        //********** The box of the mod menu **********
        mExpanded = new LinearLayout(this); // Menu markup (when the menu is expanded)
        mExpanded.setVisibility(View.GONE);
        mExpanded.setBackgroundColor(MENU_BG_COLOR);
        //mExpanded.setAlpha(0.95f);
        mExpanded.setGravity(Gravity.CENTER);
        mExpanded.setOrientation(LinearLayout.VERTICAL);
        mExpanded.setPadding(0, 0, 0, 0);
        mExpanded.setLayoutParams(new LinearLayout.LayoutParams(dp(MENU_WIDTH), WRAP_CONTENT));
        gdMenuBody = new GradientDrawable();
        gdMenuBody.setCornerRadius(MENU_CORNER); //Set corner
        gdMenuBody.setColor(MENU_BG_COLOR); //Set background color
        //gradientdrawable.setStroke(1, Color.parseColor("#32cb00")); //Set border
        mExpanded.setBackground(Preferences.loadPrefBoolean("Color animation", 1001) ? gdAnimation : gdMenuBody); //Apply aninmation to it

        //********** The icon to open mod menu **********
        startimage = new ImageView(this);
        startimage.setLayoutParams(new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
        int applyDimension = (int) TypedValue.applyDimension(1, ICON_SIZE, getResources().getDisplayMetrics()); //Icon size
        startimage.getLayoutParams().height = applyDimension;
        startimage.getLayoutParams().width = applyDimension;
        startimage.requestLayout();
        startimage.setScaleType(ImageView.ScaleType.FIT_XY);
        byte[] decode = Base64.decode(Icon(), 0);
        startimage.setImageBitmap(BitmapFactory.decodeByteArray(decode, 0, decode.length));
        ((ViewGroup.MarginLayoutParams) startimage.getLayoutParams()).topMargin = convertDipToPixels(10);
        //Initialize event handlers for buttons, etc.
        startimage.setOnTouchListener(onTouchListener());
        startimage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mCollapsed.setVisibility(View.GONE);
                mExpanded.setVisibility(View.VISIBLE);
            }
        });

        //********** The icon in Webview to open mod menu **********
        WebView wView = new WebView(this); //Icon size width=\"50\" height=\"50\"
        wView.loadData("<html>" +
                "<head>" +
                "<body style=\"margin: 0; padding: 0\"" +
                "<img src=\"" + IconWebViewData() + "\" width=\"" + ICON_SIZE + "\" height=\"" + ICON_SIZE + "\"" +
                "</body>" +
                "</html>", "text/html", "utf-8");
        wView.setBackgroundColor(0x00000000); //Transparent
        wView.requestLayout();
        wView.setAlpha(ICON_ALPHA);
        wView.getSettings().setAppCachePath("/data/data/" + getPackageName() + "/cache");
        wView.getSettings().setAppCacheEnabled(true);
        wView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        wView.setOnTouchListener(onTouchListener());

        //********** Settings icon **********
        TextView settings = new TextView(this); //Android 5 can't show ⚙, instead show other icon instead
        settings.setText(Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M ? "⚙" : "\uD83D\uDD27");
        settings.setTextColor(TEXT_COLOR);
        settings.setTypeface(Typeface.DEFAULT_BOLD);
        settings.setTextSize(20.0f);
        RelativeLayout.LayoutParams rlsettings = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        rlsettings.addRule(ALIGN_PARENT_RIGHT);
        settings.setLayoutParams(rlsettings);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    playSound("Select.ogg");
                    scrollView.removeView(patches);
                    scrollView.addView(mSettings);
                } catch (IllegalStateException e) {
                }
            }
        });

        //********** Settings **********
        mSettings = new LinearLayout(this);
        mSettings.setOrientation(LinearLayout.VERTICAL);
        addSwitch(1000, "Sounds");
        addSwitch(1001, "Color animation");
        addSwitch(1002, "Auto size vertically");
        addSwitch(9998, "Save feature preferences");
        addButton(9999, "Close");

        //********** Title text **********
        RelativeLayout titleText = new RelativeLayout(this);
        titleText.setPadding(10, 5, 10, 5);
        titleText.setVerticalGravity(16);

        TextView title = new TextView(this);
        title.setText(Title());
        title.setTextColor(TEXT_COLOR);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextSize(18.0f);
        title.setGravity(Gravity.CENTER);
        RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        rl.addRule(RelativeLayout.CENTER_HORIZONTAL);
        title.setLayoutParams(rl);

        //********** Heading text **********
        TextView heading = new TextView(this);
        heading.setText(Html.fromHtml(Heading()));
        heading.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        heading.setSingleLine(true);
        heading.setSelected(true);
        heading.setTextColor(TEXT_COLOR);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        heading.setTextSize(10.0f);
        heading.setGravity(Gravity.CENTER);
        heading.setPadding(0, 0, 0, 5);

        //********** Mod menu feature list **********
        scrollView = new ScrollView(this);
        //Auto size. To set size manually, change the width and height example 500, 500
        scrlLL = new LinearLayout.LayoutParams(MATCH_PARENT, dp(MENU_HEIGHT));
        scrlLLExpanded = new LinearLayout.LayoutParams(mExpanded.getLayoutParams());
        scrlLLExpanded.weight = 1.0f;
        scrollView.setLayoutParams(Preferences.loadPrefBoolean("Auto size vertically", 1002) ? scrlLLExpanded : scrlLL);
        scrollView.setBackgroundColor(MENU_FEATURE_BG_COLOR);

        patches = new LinearLayout(this);
        patches.setOrientation(LinearLayout.VERTICAL);
        patches.setBackgroundColor(MENU_FEATURE_BG_COLOR);

        //**********  Hide/Kill button **********
        RelativeLayout relativeLayout = new RelativeLayout(this);
        relativeLayout.setPadding(10, 3, 10, 3);
        relativeLayout.setVerticalGravity(Gravity.CENTER);

        Button hideBtn = new Button(this);
        hideBtn.setBackgroundColor(Color.TRANSPARENT);
        hideBtn.setText("HIDE/KILL (Hold)");
        hideBtn.setTextColor(TEXT_COLOR);
        hideBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mCollapsed.setVisibility(View.VISIBLE);
                mCollapsed.setAlpha(0);
                mExpanded.setVisibility(View.GONE);
                NativeToast.makeText(view.getContext(), 1);
                playSound("Back.ogg");
            }
        });
        hideBtn.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                NativeToast.makeText(view.getContext(), 2);
                playSound("Back.ogg");
                FloatingModMenuService.this.stopSelf();
                return false;
            }
        });

        //********** Close button **********
        Button closeBtn = new Button(this);
        closeBtn.setBackgroundColor(Color.TRANSPARENT);
        closeBtn.setText("MINIMIZE");
        closeBtn.setTextColor(TEXT_COLOR);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mCollapsed.setVisibility(View.VISIBLE);
                mCollapsed.setAlpha(ICON_ALPHA);
                mExpanded.setVisibility(View.GONE);
                playSound("Back.ogg");
            }
        });

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        layoutParams.addRule(ALIGN_PARENT_RIGHT);
        closeBtn.setLayoutParams(layoutParams);

        //********** Params **********
        //Variable to check later if the phone supports Draw over other apps permission
        int iparams = Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O ? 2038 : 2002;
        params = new WindowManager.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, iparams, 8, -3);
        params.gravity = 51;
        params.x = 0;
        params.y = 100;

        //********** Adding view components **********
        rootFrame.addView(mRootContainer);
        mRootContainer.addView(mCollapsed);
        mRootContainer.addView(mExpanded);
        if (IconWebViewData() != null) {
            mCollapsed.addView(wView);
        } else {
            mCollapsed.addView(startimage);
        }
        titleText.addView(title);
        titleText.addView(settings);
        mExpanded.addView(titleText);
        mExpanded.addView(heading);
        scrollView.addView(patches);
        mExpanded.addView(scrollView);
        relativeLayout.addView(hideBtn);
        relativeLayout.addView(closeBtn);
        mExpanded.addView(relativeLayout);
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(rootFrame, params);

        //********** Create menu list **********
        String[] listFT = getFeatureList();
        for (int i = 0; i < listFT.length; i++) {
            final int feature = i;
            String str = listFT[i];
            String[] split = str.split("_");
            if (str.contains("Toggle_")) {
                addSwitch(feature, split[1]);
            } else if (str.contains("SeekBar_")) {
                addSeekBar(feature, split[1], Integer.parseInt(split[2]), Integer.parseInt(split[3]));
            } else if (str.contains("Button_")) {
                addButton(feature, split[1]);
            } else if (str.contains("ButtonLink_")) {
                addButtonLink(split[1], split[2]);
            } else if (str.contains("ButtonOnOff_")) {
                addButtonOnOff(feature, split[1]);
            } else if (str.contains("Spinner_")) {
                addSpinner(feature, split[1], split[2]);
            } else if (str.contains("InputValue_")) {
                addTextField(feature, split[1]);
            } else if (str.contains("CheckBox_")) {
                addCheckBox(feature, split[1]);
            } else if (str.contains("Category_")) {
                addCategory(split[1]);
            } else if (str.contains("RichTextView_")) {
                addRichTextView(split[1]);
            } else if (str.contains("RichWebView_")) {
                addRichWebView(split[1]);
            }
        }
    }

    private View.OnTouchListener onTouchListener() {
        return new View.OnTouchListener() {
            final View collapsedView = mCollapsed;
            final View expandedView = mExpanded;
            private float initialTouchX, initialTouchY;
            private int initialX, initialY;

            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = motionEvent.getRawX();
                        initialTouchY = motionEvent.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        int rawX = (int) (motionEvent.getRawX() - initialTouchX);
                        int rawY = (int) (motionEvent.getRawY() - initialTouchY);

                        //The check for Xdiff <10 && YDiff< 10 because sometime elements moves a little while clicking.
                        //So that is click event.
                        if (rawX < 10 && rawY < 10 && isViewCollapsed()) {
                            //When user clicks on the image view of the collapsed layout,
                            //visibility of the collapsed layout will be changed to "View.GONE"
                            //and expanded view will become visible.
                            try {
                                collapsedView.setVisibility(View.GONE);
                                expandedView.setVisibility(View.VISIBLE);
                                playSound("OpenMenu.ogg");
                            } catch (NullPointerException e) {

                            }
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        //Calculate the X and Y coordinates of the view.
                        params.x = initialX + ((int) (motionEvent.getRawX() - initialTouchX));
                        params.y = initialY + ((int) (motionEvent.getRawY() - initialTouchY));
                        //Update the layout with new X & Y coordinate
                        mWindowManager.updateViewLayout(rootFrame, params);
                        return true;
                    default:
                        return false;
                }
            }
        };
    }

    //Dialog for changing value
    private void initAlertDiag() {
        //LinearLayout
        LinearLayout linearLayout1 = new LinearLayout(this);
        linearLayout1.setPadding(5, 5, 5, 5);
        linearLayout1.setOrientation(LinearLayout.VERTICAL);
        linearLayout1.setBackgroundColor(MENU_FEATURE_BG_COLOR);

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        //FrameLayout
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.addView(linearLayout);

        //TextView
        final TextView textView = new TextView(this);
        textView.setText(Html.fromHtml("<font face='roboto'>Tap OK to apply changes. Tap outside to cancel</font>"));
        textView.setTextColor(TEXT_COLOR_2);

        //Edit text
        edittextvalue = new EditText(this);
        edittextvalue.setMaxLines(1);
        edittextvalue.setWidth(convertDipToPixels(300));
        edittextvalue.setTextColor(TEXT_COLOR_2);
        edittextvalue.setTextSize(13.0f);
        edittextvalue.setHintTextColor(Color.parseColor("#434d52"));
        edittextvalue.setInputType(InputType.TYPE_CLASS_NUMBER);
        edittextvalue.setKeyListener(DigitsKeyListener.getInstance("0123456789-"));

        InputFilter[] FilterArray = new InputFilter[1];
        FilterArray[0] = new InputFilter.LengthFilter(10);
        edittextvalue.setFilters(FilterArray);

        //Button
        Button button = new Button(this);
        button.setBackgroundColor(BTN_COLOR);
        button.setTextColor(TEXT_COLOR_2);
        button.setText("OK");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inputFieldTxtValue.setValue(Integer.parseInt(edittextvalue.getText().toString()));
                inputFieldTextView.setText(Html.fromHtml("<font face='roboto'>" + inputFieldFeatureName + ": <font color='#41c300'>" + edittextvalue.getText().toString() + "</font></font>"));
                alert.dismiss();
                Preferences.changeFeatureInt(inputFieldFeatureName, inputFieldFeatureNum, Integer.parseInt(edittextvalue.getText().toString()));
                playSound("Select.ogg");
            }
        });

        alert = new AlertDialog.Builder(this, 2).create();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Objects.requireNonNull(alert.getWindow()).setType(Build.VERSION.SDK_INT >= 26 ? 2038 : 2002);
        }
        linearLayout1.addView(textView);
        linearLayout1.addView(edittextvalue);
        linearLayout1.addView(button);
        alert.setView(linearLayout1);
    }

    private void addSwitch(final int featureNum, final String featureName) {
        final Switch switchR = new Switch(this);
        switchR.setText(Html.fromHtml("<font face='roboto'>" + featureName + "</font>"));
        switchR.setTextColor(TEXT_COLOR_2);
        switchR.setPadding(10, 5, 0, 5);
        switchR.setChecked(Preferences.loadPrefBoolean(featureName, featureNum));
        switchR.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    playSound("On.ogg");
                } else {
                    playSound("Off.ogg");
                }
                if (featureNum == 1001)
                    mExpanded.setBackground(isChecked ? gdAnimation : gdMenuBody);
                if (featureNum == 1002)
                    scrollView.setLayoutParams(isChecked ? scrlLLExpanded : scrlLL);
                Preferences.changeFeatureBoolean(featureName, featureNum, isChecked);
            }
        });
        if (featureNum >= 1000)
            mSettings.addView(switchR);
        else
            patches.addView(switchR);
    }

    private void addSeekBar(final int featureNum, final String featureName, final int min, int max) {
        int prog2 = Preferences.loadPrefInt(featureName, min);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setPadding(10, 5, 0, 5);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setGravity(Gravity.CENTER);

        final TextView textView = new TextView(this);
        textView.setText(Html.fromHtml("<font face='roboto'>" + featureName + ": <font color='#41c300'>" + prog2 + "</font>"));
        textView.setTextColor(TEXT_COLOR_2);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setPadding(25, 10, 35, 10);
        seekBar.setMax(max);
        seekBar.setProgress(prog2);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            int l;

            public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
                if (l < i) {
                    playSound("SliderIncrease.ogg");
                } else {
                    playSound("SliderDecrease.ogg");
                }
                l = i;

                //if progress is greater than minimum, don't go below. Else, set progress
                seekBar.setProgress(i < min ? min : i);
                Preferences.changeFeatureInt(featureName, featureNum, i < min ? min : i);
                textView.setText(Html.fromHtml("<font face='roboto'>" + featureName + ": <font color='#41c300'>" + (i < min ? min : i) + "</font>"));
            }
        });
        linearLayout.addView(textView);
        linearLayout.addView(seekBar);
        patches.addView(linearLayout);
    }

    private void addButton(final int featureNum, final String featureName) {
        final Button button = new Button(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        layoutParams.setMargins(7, 5, 7, 5);
        button.setLayoutParams(layoutParams);
        button.setPadding(10, 5, 10, 5);
        button.setTextSize(13.0f);
        button.setTextColor(TEXT_COLOR_2);
        button.setGravity(Gravity.CENTER);
        button.setText(featureName.replace("Link_", ""));
        button.setBackgroundColor(BTN_COLOR);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (featureName.contains("Link_")) {
                    String[] split = featureName.split("_");
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(split[2])));
                }
                playSound("Select.ogg");
                if (featureNum == 9999) {
                    scrollView.removeView(mSettings);
                    scrollView.addView(patches);
                    return;
                } else
                    Preferences.changeFeatureInt(featureName, featureNum, 0);
            }
        });

        if (featureNum >= 1000)
            mSettings.addView(button);
        else
            patches.addView(button);
    }

    private void addButtonLink(final String featureName, final String url) {
        final Button button = new Button(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        layoutParams.setMargins(7, 5, 7, 5);
        button.setLayoutParams(layoutParams);
        button.setPadding(10, 5, 10, 5);
        button.setTextSize(13.0f);
        button.setTextColor(TEXT_COLOR_2);
        button.setGravity(Gravity.CENTER);
        button.setText(featureName);
        button.setBackgroundColor(BTN_COLOR);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            }
        });
        patches.addView(button);
    }

    private void addButtonOnOff(final int featureNum, String featureName) {
        final Button button = new Button(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        layoutParams.setMargins(7, 5, 7, 5);
        button.setLayoutParams(layoutParams);
        button.setPadding(10, 5, 10, 5);
        button.setTextSize(13.0f);
        button.setTextColor(TEXT_COLOR_2);
        button.setGravity(Gravity.CENTER);

        final String finalFeatureName = featureName.replace("OnOff_", "");
        boolean isActive = Preferences.loadPrefBoolean(featureName, featureNum);
        if (isActive) {
            button.setText(finalFeatureName + ": ON");
            button.setBackgroundColor(Color.parseColor("#003300"));
            isActive = false;
        } else {
            button.setText(finalFeatureName + ": OFF");
            button.setBackgroundColor(Color.parseColor("#7f0000"));
            isActive = true;
        }
        final boolean finalIsActive = isActive;
        button.setOnClickListener(new View.OnClickListener() {
            boolean isActive2 = finalIsActive;

            public void onClick(View v) {
                Preferences.changeFeatureBoolean(finalFeatureName, featureNum, isActive2);
                //Log.d(TAG, finalFeatureName + " " + featureNum + " " + isActive2);
                if (isActive2) {
                    playSound("On.ogg");
                    button.setText(finalFeatureName + ": ON");
                    button.setBackgroundColor(Color.parseColor("#003300"));
                    isActive2 = false;
                } else {
                    playSound("Off.ogg");
                    button.setText(finalFeatureName + ": OFF");
                    button.setBackgroundColor(Color.parseColor("#7f0000"));
                    isActive2 = true;
                }
            }
        });

        if (featureNum >= 1000)
            mSettings.addView(button);
        else
            patches.addView(button);
    }

    private void addSpinner(final int featureNum, final String featureName, final String list) {
        final List<String> lists = new LinkedList<>(Arrays.asList(list.split(",")));

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setPadding(10, 5, 10, 5);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setGravity(Gravity.CENTER);

        final TextView textView = new TextView(this);
        textView.setText(Html.fromHtml("<font face='roboto'>" + featureName + ": <font color='#41c300'></font>"));
        textView.setTextColor(TEXT_COLOR_2);

        // Create another LinearLayout as a workaround to use it as a background
        // and to keep the 'down' arrow symbol
        // If spinner had the setBackgroundColor set, there would be no arrow symbol
        LinearLayout linearLayout2 = new LinearLayout(this);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        layoutParams2.setMargins(10, 2, 10, 5);
        linearLayout2.setOrientation(LinearLayout.VERTICAL);
        linearLayout2.setBackgroundColor(BTN_COLOR);
        linearLayout2.setLayoutParams(layoutParams2);

        final Spinner spinner = new Spinner(this);
        spinner.setPadding(5, 10, 5, 8);
        spinner.setLayoutParams(layoutParams2);
        spinner.getBackground().setColorFilter(1, PorterDuff.Mode.SRC_ATOP); //trick to show white down arrow color
        //Creating the ArrayAdapter instance having the list
        ArrayAdapter aa = new ArrayAdapter(this, android.R.layout.simple_spinner_item, lists);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //Setting the ArrayAdapter data on the Spinner
        spinner.setAdapter(aa);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                Preferences.changeFeatureInt(spinner.getSelectedItem().toString(), featureNum, position);
                ((TextView) parentView.getChildAt(0)).setTextColor(TEXT_COLOR_2);
                playSound("Select.ogg");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                playSound("Select.ogg");
            }
        });
        linearLayout.addView(textView);
        linearLayout2.addView(spinner);
        patches.addView(linearLayout);
        patches.addView(linearLayout2);
    }

    private void addTextField(final int feature, final String featureName) {
        RelativeLayout relativeLayout2 = new RelativeLayout(this);
        relativeLayout2.setPadding(10, 5, 10, 5);
        relativeLayout2.setVerticalGravity(16);

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        layoutParams.topMargin = 15;

        final TextView textView = new TextView(this);
        int num = Preferences.loadPrefInt(featureName, 0);
        textView.setText(Html.fromHtml("<font face='roboto'>" + featureName + ": <font color='#41c300'>" + num + "</font></font>"));
        textView.setTextColor(TEXT_COLOR_2);
        textView.setLayoutParams(layoutParams);

        final EditTextValue edittextval = new EditTextValue();

        RelativeLayout.LayoutParams layoutParams2 = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        layoutParams2.addRule(ALIGN_PARENT_RIGHT);

        Button button2 = new Button(this);
        button2.setLayoutParams(layoutParams2);
        button2.setBackgroundColor(BTN_COLOR);
        button2.setText("SET");
        button2.setTextColor(TEXT_COLOR_2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alert.show();
                inputFieldTextView = textView;
                inputFieldFeatureNum = feature;
                inputFieldFeatureName = featureName;
                inputFieldTxtValue = edittextval;
                edittextvalue.setText(String.valueOf(edittextval.getValue()));
            }
        });

        relativeLayout2.addView(textView);
        relativeLayout2.addView(button2);
        patches.addView(relativeLayout2);
    }

    private void addCheckBox(final int featureNum, final String featureName) {
        final CheckBox checkBox = new CheckBox(this);
        checkBox.setText(featureName);
        checkBox.setTextColor(TEXT_COLOR_2);
        checkBox.setPadding(10, 5, 10, 5);
        checkBox.setChecked(Preferences.loadPrefBoolean(featureName, featureNum));
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (checkBox.isChecked()) {
                    playSound("On.ogg");
                    Preferences.changeFeatureBoolean(featureName, featureNum, isChecked);
                } else {
                    playSound("Off.ogg");
                    Preferences.changeFeatureBoolean(featureName, featureNum, isChecked);
                }
            }
        });
        patches.addView(checkBox);
    }

    private void addCategory(String text) {
        TextView textView = new TextView(this);
        textView.setBackgroundColor(Color.parseColor("#2F3D4C"));
        textView.setText(text);
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(14.0f);
        textView.setTextColor(TEXT_COLOR_2);
        textView.setTypeface(null, Typeface.BOLD);
        textView.setPadding(10, 5, 10, 5);
        patches.addView(textView);
    }

    private void addRichTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(Html.fromHtml(text));
        textView.setTextColor(TEXT_COLOR_2);
        textView.setPadding(10, 5, 10, 5);
        patches.addView(textView);
    }

    private void addRichWebView(String text) {
        WebView wView = new WebView(this);
        wView.loadData(text, "text/html", "utf-8");
        wView.setBackgroundColor(0x00000000); //Transparent
        wView.setPadding(0, 5, 0, 5);
        wView.getSettings().setAppCacheEnabled(false);
        wView.requestLayout();
        patches.addView(wView);
    }

    //Play sounds
    public void playSound(String uri) {
        if (Preferences.isSoundEnabled) {
            if (!soundDelayed) {
                soundDelayed = true;
                if (FXPlayer != null) {
                    FXPlayer.stop();
                    FXPlayer.release();
                }
                FXPlayer = MediaPlayer.create(this, Uri.fromFile(new File(cacheDir + uri)));
                if (FXPlayer != null) {
                    //Volume reduced so sounds are not too loud
                    FXPlayer.setVolume(0.4f, 0.4f);
                    FXPlayer.start();
                }

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        soundDelayed = false;
                    }
                }, 100);
            }
        }
    }

    //View animation
    public void startAnimation() {
        final int start = Color.parseColor("#dd00820d");
        final int end = Color.parseColor("#dd000282");

        final ArgbEvaluator evaluator = new ArgbEvaluator();
        gdAnimation.setCornerRadius(MENU_CORNER);
        gdAnimation.setOrientation(GradientDrawable.Orientation.TL_BR);
        final GradientDrawable gradient = gdAnimation;

        ValueAnimator animator = TimeAnimator.ofFloat(0.0f, 1.0f);
        animator.setDuration(10000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                Float fraction = valueAnimator.getAnimatedFraction();
                int newStrat = (int) evaluator.evaluate(fraction, start, end);
                int newEnd = (int) evaluator.evaluate(fraction, end, start);
                int[] newArray = {newStrat, newEnd};
                gradient.setColors(newArray);
            }
        });

        animator.start();
    }

    //Override our Start Command so the Service doesnt try to recreate itself when the App is closed
    public int onStartCommand(Intent intent, int i, int i2) {
        return Service.START_NOT_STICKY;
    }

    public boolean isViewCollapsed() {
        return rootFrame == null || mCollapsed.getVisibility() == View.VISIBLE;
    }

    //For our image a little converter
    private int convertDipToPixels(int i) {
        return (int) ((((float) i) * getResources().getDisplayMetrics().density) + 0.5f);
    }

    private int dp(int i) {
        return (int) TypedValue.applyDimension(1, (float) i, getResources().getDisplayMetrics());
    }

    //Check if we are still in the game. If now our menu and menu button will dissapear
    private boolean isNotInGame() {
        RunningAppProcessInfo runningAppProcessInfo = new RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(runningAppProcessInfo);
        return runningAppProcessInfo.importance != 100;
    }

    //Destroy our View
    public void onDestroy() {
        super.onDestroy();
        View view = rootFrame;
        if (view != null) {
            mWindowManager.removeView(view);
        }
    }

    //Same as above so it wont crash in the background and therefore use alot of Battery life
    public void onTaskRemoved(Intent intent) {
        stopSelf();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.onTaskRemoved(intent);
        //exit(0);
    }

    public void Thread() {
        if (rootFrame == null) {
            return;
        }
        if (isNotInGame()) {
            rootFrame.setVisibility(View.INVISIBLE);
        } else {
            rootFrame.setVisibility(View.VISIBLE);
        }
    }

    public class EditTextValue {
        private int val;

        public void setValue(int i) {
            val = i;
        }

        public int getValue() {
            return val;
        }
    }
}