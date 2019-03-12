package com.barak.ball;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final long TIME_MILI = 10000;
    private static final String HIGH_SCORE = "HIGH_SCORE";
    private List<Ball> balls = null;
    private int START_BALLS = 10;
    private int START_BALLS_ = 10;
    private int countBall = START_BALLS;

    private TextView textTimer, textScore, textLevel;
    private int screenH, screenW;
    private MyTimer timer = null;
    private SoundPool soundPool = null;
    private int streamSoundTuck = 0;
    private int speed = 10;
    private int streamSoundLose = 0;
    private int mLevel = 0;
    private int mScroe;
    private int caliber;
    private FrameLayout frame;
    private AdView mAdView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT < 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            decorView.setSystemUiVisibility(uiOptions);
        }

        setContentView(R.layout.activity_main);
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        frame = (FrameLayout)findViewById(R.id.frame);
        ViewTreeObserver vto = frame.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                   frame.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    frame.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                screenW  = frame.getMeasuredWidth();
                screenH = frame.getMeasuredHeight();
                createBalls();

            }
        });
        textTimer = findViewById(R.id.text_timer);
        textScore = findViewById(R.id.text_score);
        textLevel = findViewById(R.id.text_level);
        textScore.setText( getString(R.string.points) + mScroe);
        textLevel.setText( getString(R.string.screen)+ ++mLevel);
        WindowManager windowManager =
                (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        final Display display = windowManager.getDefaultDisplay();
        Point outPoint = new Point();
        if (Build.VERSION.SDK_INT >= 19) {
            display.getRealSize(outPoint);
        } else {
            display.getSize(outPoint);
        }
        if (outPoint.y > outPoint.x) {
            screenH = outPoint.y;
            screenW = outPoint.x;
        } else {
            screenH = outPoint.x;
            screenW = outPoint.y;
        }

        createSoundPool();

        balls = new ArrayList<>();
        timer = new MyTimer(TIME_MILI, 50);
        timer.start();
    }

    private void createSoundPool() {
        soundPool = new SoundPool(1, AudioAttributes.CONTENT_TYPE_MUSIC, 1);
        streamSoundTuck = soundPool.load(this, R.raw.tuck, 1);
        streamSoundLose = soundPool.load(this, R.raw.lose, 3);
    }

    private void createBalls() {

        for (int i = 0; i < countBall; i++) {
            int rad_x = (int) (Math.random() * screenW * .85)+100;
            int rad_y = (int) (Math.random() * screenH * .85)+100;
            balls.add(new Ball(MainActivity.this, screenH, screenW, 200,
                    speed, speed * 3 ,rad_x,rad_y,frame));
        }
        caliber = balls.get(0).size;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x, y;
        x = event.getX();
        y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                boolean flag = Ball.touch(balls, x, y, soundPool, streamSoundTuck);
                if (flag) {
                    --countBall;
                    textScore.setText( getString(R.string.points)+ ++mScroe);
                    if (countBall == 0) {
                        START_BALLS++;
                        countBall = START_BALLS;
                        mScroe += countBall;
                        mLevel++;
                        speed++;
                        textLevel.setText( getString(R.string.screen) + mLevel);
                        textScore.setText( getString(R.string.points) + mScroe);
                        if (timer != null) timer.cancel();
                        timer = new MyTimer(TIME_MILI, 100);
                        timer.start();
                        createBalls();
                    }
                }
                break;
        }
        return true;
    }

    public class MyTimer extends CountDownTimer {

        public MyTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long l) {
            textTimer.setText( getString(R.string.left)+l / 1000 );
            moveBalls();
        }

        @Override
        public void onFinish() {
            timer.cancel();
            timer = null;
            showPopUpFinish();
        }
    }
    private void showPopUpFinish() {

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String dialogTitle = getString(R.string.app_name);
        String dialogText;


        dialogText = String.format(getString(R.string.score_text), mScroe);
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        int highScore = sp.getInt(HIGH_SCORE, 2);
        if (mScroe > highScore) {
            sp.edit().putInt(HIGH_SCORE, mScroe).apply();
            dialogTitle = dialogTitle + " - " + getString(R.string.start_title_);
            ImageView image = new ImageView(this);
            image.setImageResource(R.drawable.right);
            alert.setView(image);
        } else if (mScroe == 0) {
            dialogText = "";
        }
        alert.setTitle(dialogTitle).setMessage(dialogText)
                .setOnDismissListener(dialogInterface -> {
                    initTimer();
                }).setNeutralButton(getString(R.string.submit), (dialogInterface, d) -> {
            initTimer();
        });
        alert.show();
    }

    private void initTimer() {

        if (timer != null) return;
        for (int i = balls.size() - 1; i > -1; i--) {
            ((ViewGroup) balls.get(i).image.getParent()).removeView(balls.get(i).image);
            balls.remove(i);
        }
        START_BALLS = START_BALLS_;
        countBall = START_BALLS;
        mScroe = 0;
        mLevel = 1;
        speed = 10;
        textLevel.setText( getString(R.string.screen) + mLevel);
        textScore.setText( getString(R.string.points) + mScroe);
        if (timer != null) timer.cancel();
        timer = new MyTimer(TIME_MILI, 100);
        timer.start();
        createBalls();
    }

    private void moveBalls() {
        for (Ball ball : balls) {
            ball.move();
        }
        handleCollisions();
    }

    public void handleCollisions() {
        double xDist, yDist;
        for (int i = 0; i < balls.size(); i++) {
            Ball A = balls.get(i);
            for (int j = i + 1; j < balls.size(); j++) {
                Ball B = balls.get(j);
                xDist = A.xx - B.xx;
                yDist = A.yy - B.yy;
                double distSquared = xDist * xDist + yDist * yDist;
                //Check the squared distances instead of the the distances, same result, but avoids a square root.
                if (distSquared <= (caliber) * (caliber)) {
                    double xVelocity = B.speedX - A.speedX;
                    double yVelocity = B.speedY - A.speedY;
                    double dotProduct = xDist * xVelocity + yDist * yVelocity;
                    //Neat vector maths, used for checking if the objects moves towards one another.
                    if (dotProduct > 0) {
                        double collisionScale = dotProduct / distSquared;
                        double xCollision = xDist * collisionScale;
                        double yCollision = yDist * collisionScale;
                        //The Collision vector is the speed difference projected on the Dist vector,
                        //thus it is the component of the speed difference needed for the collision.
                        A.speedX += xCollision;
                        A.speedY += yCollision;
                        B.speedX -= xCollision;
                        B.speedY -= yCollision;
                    }
                }
            }
        }
    }

    public static double distance(float x1, float y1,
                                  float x2, float y2) {
        return Math.hypot(x1 - x2, y1 - y2);
    }
}
