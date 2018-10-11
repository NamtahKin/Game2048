package com.namtah.game2048;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.namtah.game2048.widget.GameView;

public class GameActivity extends AppCompatActivity {

    private static final String TAG = "GameActivity";
    GameView viewGame;
    TextView tvBestScore, tvScore, tvRestart;
    int mBestScore, mScore;
    int mBase;
    GameOverlayDialog mDialog;
    DisplayMetrics mScreenMetrics;
    boolean mAlreadyWin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        viewGame = findViewById(R.id.view_game);
        tvBestScore = findViewById(R.id.tv_best_score);
        tvScore = findViewById(R.id.tv_score);
        tvRestart = findViewById(R.id.tv_restart);

        mScreenMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(mScreenMetrics);

        mDialog = new GameOverlayDialog(GameActivity.this);
        mDialog.setOnButtonsClickListener(new GameOverlayDialog.OnButtonsClickListener() {
            @Override
            public void onContinueButtonClicked(GameOverlayDialog dialog) {
                dialog.dismiss();
            }

            @Override
            public void onRestartButtonClicked(GameOverlayDialog dialog) {
                dialog.dismiss();
                GameActivity.this.restartGame();
            }
        });

        if (mBase == 0) {
            mBase = getIntent().getIntExtra("base", 4);
        }

        load();
        viewGame.load(mBase);

        tvScore.setText(String.valueOf(mScore));
        tvBestScore.setText(String.valueOf(mBestScore));

        viewGame.setOnStepListener(new GameView.OnStepListener() {
            @Override
            public void onStepOver(int stepScore, int stepMax) {        //一步结束
//                Log.d(TAG, "onStepOver: ------stepScore-" + stepScore + ", stepMax-" + stepMax + "------");

                if (stepMax >= 0x40000000 || mScore + stepScore >= 0x40000000) {
                    mDialog.setTitle("恭喜达到最大值")
                            .setScore(mScore)
                            .showContinueButton(false)
                            .show(mScreenMetrics.widthPixels, mScreenMetrics.heightPixels);
                    viewGame.load(mBase);
                    return;
                }

                viewGame.save();
                if (stepScore > 0) {
                    mScore += stepScore;
                    tvScore.setText(String.valueOf(mScore));
                    if (mScore > mBestScore) {
                        mBestScore = mScore;
                        tvBestScore.setText(String.valueOf(mBestScore));
                        save(mScore, mBestScore);
                    } else {
                        save(mScore);
                    }
                }
                if (!viewGame.checkAccessibility()) {   //如果走不通了
                    Log.i(TAG, "onStepOver: 走不通了");
                    mDialog.setTitle("游戏结束")
                            .setScore(mScore)
                            .showContinueButton(false)
                            .show(mScreenMetrics.widthPixels, mScreenMetrics.heightPixels);
                } else if (!mAlreadyWin && stepMax == 2048) {
                    mAlreadyWin = true;
                    save(mAlreadyWin);
                    mDialog.setTitle("游戏成功")
                            .setScore(mScore)
                            .showContinueButton(true)
                            .show(mScreenMetrics.widthPixels, mScreenMetrics.heightPixels);
                }
            }
        });

        tvRestart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {                   //重新游戏
                restartGame();
            }
        });
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getWindowManager().getDefaultDisplay().getMetrics(mScreenMetrics);
    }

    void restartGame() {
        viewGame.restartGame();
        mAlreadyWin = false;
        mScore = 0;
        tvScore.setText(String.valueOf(mScore));
        save(mScore);
        save(mAlreadyWin);
    }

    void load() {
        SharedPreferences sp = getSharedPreferences("base-" + mBase, MODE_PRIVATE);
        mScore = sp.getInt("score", 0);
        mBestScore = sp.getInt("best-score", 0);
        mAlreadyWin = sp.getBoolean("win", false);
    }

    void save(int score) {
        SharedPreferences.Editor editor = getSharedPreferences("base-" + mBase, MODE_PRIVATE).edit();
        editor.putInt("score", score).apply();
    }

    void save(int score, int bestScore) {
        SharedPreferences.Editor editor = getSharedPreferences("base-" + mBase, MODE_PRIVATE).edit();
        editor.putInt("score", score);
        editor.putInt("best-score", bestScore);
        editor.apply();
    }

    void save(boolean win) {
        SharedPreferences.Editor editor = getSharedPreferences("base-" + mBase, MODE_PRIVATE).edit();
        editor.putBoolean("win", win);
        editor.apply();
    }

//    @Override
//    public boolean dispatchKeyEvent(KeyEvent event) {
//        super.dispatchKeyEvent(event);
//        if (!mDialog.isShowing()) {
//            return viewGame.dispatchKeyEvent(event);
//        }
//        return true;
//    }

    /**
     * 弹框辅助类，弹框包括一个标题、一个分数和两个按钮
     */
    static class GameOverlayDialog implements View.OnClickListener {

        AlertDialog mAlertDialog;
        View mView;
        OnButtonsClickListener mListener;

        TextView mTitle, mScore, mContinue, mRestart;

        void setOnButtonsClickListener(OnButtonsClickListener l) {
            this.mListener = l;
        }

        GameOverlayDialog(Context context) {
            mView = LayoutInflater.from(context).inflate(R.layout.dialog_game, null);
            mAlertDialog = new AlertDialog.Builder(context,R.style.AppTheme_AlertDialog)
                    .setCancelable(true)
                    .setView(mView)
                    .create();

            mTitle = mView.findViewById(R.id.dlg_game_title);
            mScore = mView.findViewById(R.id.dlg_game_score);
            mContinue = mView.findViewById(R.id.dlg_game_continue);
            mRestart = mView.findViewById(R.id.dlg_game_restart);
            mContinue.setOnClickListener(this);
            mRestart.setOnClickListener(this);
        }

        /**
         * 按指定大小显示弹框内容
         * @param width 弹框内容宽度
         * @param height 弹框内容高度
         */
        void show(int width, int height) {
            mAlertDialog.show();
            Window window = mAlertDialog.getWindow();
            WindowManager.LayoutParams wlp = window.getAttributes();
            if (wlp != null) {
                wlp.width = width - 80;
                wlp.height = height - 200;
                window.setAttributes(wlp);
            }

            ViewGroup.LayoutParams lp = mView.getLayoutParams();
            lp.width = width - 80;
            lp.height = height - 200;
            mView.setLayoutParams(lp);
        }

        void dismiss() {
            mAlertDialog.dismiss();
        }

        GameOverlayDialog showContinueButton(boolean show) {
            if (show) {
                mContinue.setVisibility(View.VISIBLE);
            } else {
                mContinue.setVisibility(View.GONE);
            }
            return this;
        }

        GameOverlayDialog setTitle(CharSequence title) {
            mTitle.setText(title);
            return this;
        }

        GameOverlayDialog setScore(int score) {
            mScore.setText("分数：" + score);
            return this;
        }

        boolean isShowing() {
            return mAlertDialog.isShowing();
        }

        /**
         * 接口
         */
        interface OnButtonsClickListener {
            void onContinueButtonClicked(GameOverlayDialog dialog);
            void onRestartButtonClicked(GameOverlayDialog dialog);
        }

        @Override
        public void onClick(View v) {
            if (mListener != null) {
                if (v.getId() == R.id.dlg_game_continue) {
                    mListener.onContinueButtonClicked(this);
                } else if (v.getId() == R.id.dlg_game_restart) {
                    mListener.onRestartButtonClicked(this);
                }
            }
        }
    }

}
