package com.namtah.game2048.widget;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

import java.util.ArrayList;
import java.util.List;

/**
 * 2048游戏视图，使用{@link #load(int)}加载或创建游戏数据，使用{@link #save()}保存当前游戏数据，
 * 使用{@link #restartGame()}来进行重新游戏
 *
 * 作者：Namtah Kin
 */
public class GameView extends View {

    private static final String TAG = "GameView";

    private final int DURATION_MOVE = 180;         //“移动”动画持续时间
    private final int DURATION_MERGE = 160;        //“合并”动画持续时间
    private final int DURATION_NEW = 200;          //“生成”动画持续时间
    private int mTouchSlop;

    private DataHelper mDataHelper;                     //实际数据辅助类对象
    private Paint pBg, pText;                           //画笔对象
    private RectF tileRec;                              //瓷砖边界，绘制时复用
    private float mSideWidth, mGap;                     //面板边距、瓷砖间距
    private Scroller mMoveScorller;                     //“移动”动画辅助
    private Scroller mNewScroller;                      //“生成”动画辅助
    private Scroller mMergeScroller;                    //“合并”动画辅助
    private Direction mDirection;                       //移动方向，同样用于动画
    //private List<Point> mNewPoints;                     //保存每次需要新生成砖块的位置，因为第一次和重新游戏需要生成两个砖块，故用列表
    private OnStepListener mOnStepListener = null;      //每一步监听

    enum Direction {
        LEFT, RIGHT, TOP, BOTTOM
    }

    /**
     * 每走一步的监听回调
     */
    public interface OnStepListener {
        /**
         * 有效滑动后立即回调
         * @param stepScore 该一步滑动后所得的分数
         * @param stepMax 该一步滑动后的最大值
         */
        void onStepOver(int stepScore, int stepMax);

    }

    public void setOnStepListener(OnStepListener l) {
        mOnStepListener = l;
    }

    public GameView(Context context) {
        this(context, null);
    }

    public GameView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GameView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        mDataHelper = new DataHelper();

        pBg = new Paint();
        pBg.setAntiAlias(true);
        pText = new Paint();
        pText.setAntiAlias(true);
        pText.setTextAlign(Paint.Align.CENTER);

        tileRec = new RectF();

        //mNewPoints = new ArrayList<>();
        mNewScroller = new Scroller();
        mMoveScorller = new Scroller();
        mMergeScroller = new Scroller();
        mMoveScorller.setOnCompleteListener(new Scroller.OnCompleteListener() {
            @Override
            public void onComplete() {
                //移动动画完毕开启增加新砖块动画
                mNewScroller.startScroll(DURATION_NEW);
                mMergeScroller.startScroll(DURATION_MERGE);
            }
        });

    }

    /**
     * 重新开始游戏。
     */
    public void restartGame() {
        mDataHelper.clear();

//        mNewPoints.clear();
//        mNewPoints.add(mDataHelper.createNewTile());
//        mNewPoints.add(mDataHelper.createNewTile());
        mDataHelper.putTwoNewTile();
        save();
        mMoveScorller.abortAnimation();
        mNewScroller.startScroll(DURATION_NEW);
        invalidate();
    }

    /**
     * 根据阶数从文件加载数据，如果没有数据会自动调用{@link #restartGame()}重新开始游戏,
     * 你应该先调用此方法开始游戏，然后重新游戏则restartGame()方法。
     * @param base 阶数
     */
    public void load(int base) {
        int[][] real = new int[base][base];
        mDataHelper.setTiles(real);
        SharedPreferences sp = getContext().getSharedPreferences("base-" + base, Context.MODE_PRIVATE);
        boolean allZero = true;
        for (int i = 0; i < base; i++) {
            for (int j = 0; j < base; j++) {
                real[i][j] = sp.getInt(i + "-" + j, 0);
                if (real[i][j] != 0) {
                    allZero = false;
                    break;
                }
            }
        }
        if (allZero) {
            restartGame();
        } else {
            invalidate();
        }
    }

    /**
     * 保存当前数据
     */
    public void save() {
        SharedPreferences sp = getContext().getSharedPreferences("base-" + mDataHelper.number, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        for (int i = 0; i < mDataHelper.number; i++) {
            for (int j = 0; j < mDataHelper.number; j++) {
                editor.putInt(i + "-" + j, mDataHelper.get(i, j));
            }
        }
        editor.apply();
    }

    /**
     * 检测当前游戏是否走的通
     * @return 是否走的通
     */
    public boolean checkAccessibility() {
        return mDataHelper.checkAccessibility();
    }

    //分发滑动状态
    private void dispatchScroll() {
        boolean hasChanged = false;
        if (mDirection == Direction.LEFT) {
            hasChanged = mDataHelper.goLeft();
        } else if (mDirection == Direction.RIGHT) {
            hasChanged = mDataHelper.goRight();
        } else if (mDirection == Direction.TOP) {
            hasChanged = mDataHelper.goTop();
        } else if (mDirection == Direction.BOTTOM) {
            hasChanged = mDataHelper.goBottom();
        }

        if (hasChanged) {
            //增加新砖块并记录
//            mNewPoints.clear();
//            mNewPoints.add(mDataHelper.createNewTile());
            mDataHelper.putOneNewTile();
            //要先启动动画，因为下面的回调可能会取消动画
            mMoveScorller.startScroll(DURATION_MOVE);
            if (mOnStepListener != null) {
                mOnStepListener.onStepOver(mDataHelper.stepScore, mDataHelper.stepMax);
            }
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        if (width > height) {
            width = height;
        } else if (width < height) {
            height = width;
        }
        //Log.d(TAG, "onMeasure: w: " + width + ", h: " + height);
        setMeasuredDimension(width, height);
    }

    float startX, startY;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                startY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:

                break;
            case MotionEvent.ACTION_UP:
                final float offsetX = event.getX() - startX;
                final float offsetY = event.getY() - startY;
                if (Math.abs(offsetX) > mTouchSlop || Math.abs(offsetY) > mTouchSlop) {
                    if (Math.abs(offsetX) > Math.abs(offsetY)) {
                        if (offsetX > 0) {
                            mDirection = Direction.RIGHT;
                        } else {
                            mDirection = Direction.LEFT;
                        }
                    } else {
                        if (offsetY > 0) {
                            mDirection = Direction.BOTTOM;
                        } else {
                            mDirection = Direction.TOP;
                        }
                    }
                    dispatchScroll();
                }
                break;
            default: break;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mSideWidth = getWidth() / (mDataHelper.number * 8f);
        mGap = getWidth() / (mDataHelper.number * 10f);

        //计算每块瓷砖的宽和高
        final float tWidth = (getWidth() - mSideWidth * 2 - mGap * (mDataHelper.number - 1)) / mDataHelper.number;
        final float tHeight = (getHeight() - mSideWidth * 2 - mGap * (mDataHelper.number - 1)) / mDataHelper.number;

        //绘制大背景
        pBg.setColor(0xffbbada0);
        tileRec.set(0, 0, getWidth(), getHeight());
        canvas.drawRoundRect(tileRec, tWidth / 6, tHeight / 6, pBg);


        //绘制瓷砖背景
        for (int i = 0; i < mDataHelper.number; i++) {
            for (int j = 0; j < mDataHelper.number; j++) {
                tileRec.set(mSideWidth + tWidth * j + mGap * j,
                        mSideWidth + tHeight * i + mGap * i,
                        mSideWidth + tWidth * (j + 1) + mGap * j,
                        mSideWidth + tHeight * (i + 1) + mGap * i);
                pBg.setColor(0xffcdc1b4);
                canvas.drawRoundRect(tileRec, tWidth / 6, tHeight / 6, pBg);
            }
        }

        //根据必要的偏移量和缩放来计算瓷砖位置并绘制瓷砖前景和数字
        //是否正在进行某种动画
        boolean scrolling = mMoveScorller.computeScrollOffset();
        boolean creating = mNewScroller.computeScrollOffset();
        boolean merging = mMergeScroller.computeScrollOffset();
//        Log.d(TAG, "onDraw: " + mNewScroller.getTimeFraction());

        for (int i = 0; i < mDataHelper.number; i++) {
            for (int j = 0; j < mDataHelper.number; j++) {
                int value;
                //先把状态分成”移动“状态和”非移动“状态（或者说固定状态），用于计算当前瓷砖的实际偏移位置以及大小
                //”移动“状态进行”移动“动画，操作的是瓷砖临时值（”移动“前的值），因为瓷砖值是即时修改的，而”移动“动画是缓慢进行的，需要旧值和位移值来进行动画
                //”非移动“状态：正好”合并“动画、”生成“动画、以及”静止状态“操作的都是新值，所以可以归为一个状态
                // ---------- ”非移动“状态 ----------
                if (!scrolling) {
                    value = mDataHelper.get(i, j);
                    if (value == 0)
                        continue;
                    tileRec.set(mSideWidth + tWidth * j + mGap * j,
                            mSideWidth + tHeight * i + mGap * i,
                            mSideWidth + tWidth * (j + 1) + mGap * j,
                            mSideWidth + tHeight * (i + 1) + mGap * i);
                    if (merging) {                                          //是否正在进行合并动画
                        if (mDataHelper.needMerge(i, j)) {                            //当前位置是否需要合并
//                            float y;
//                            if (mMergeScroller.getTimeFraction() < 0.5) {
//                                y = -2 * mMergeScroller.getTimeFraction() + 1;
//                            } else {
//                                y = 2 * mMergeScroller.getTimeFraction() - 1;
//                            }
                            tileRec.inset(-12 * mMergeScroller.getTimeFraction(), -12 * mMergeScroller.getTimeFraction());
                        }
                    }
                    if (creating) {                                         //是否正在进行生成动画
                        for (int k = 0; k < mDataHelper.getNewTiles().size(); k++) {       //当前位置的瓷砖是否是新生成的
                            if (mDataHelper.getNewTiles().get(k).x == i && mDataHelper.getNewTiles().get(k).y == j) {
                                tileRec.inset((1 - mNewScroller.getTimeFraction()) * tWidth / 2, (1 - mNewScroller.getTimeFraction()) * tHeight / 2);
                            }
                        }
                    }
                // ---------- ”移动“状态 ----------
                } else {
                    value = mDataHelper.getTemp(i, j);
                    if (value == 0)
                        continue;
                    if (mDirection == Direction.LEFT || mDirection == Direction.RIGHT) {
                        tileRec.set(mSideWidth + tWidth * j + mGap * j + mDataHelper.getOffset(i, j) * (tWidth + mGap) * mMoveScorller.getTimeFraction(),
                                mSideWidth + tHeight * i + mGap * i,
                                mSideWidth + tWidth * (j + 1) + mGap * j + mDataHelper.getOffset(i, j) * (tWidth + mGap) * mMoveScorller.getTimeFraction(),
                                mSideWidth + tHeight * (i + 1) + mGap * i);
                    } else if (mDirection == Direction.TOP || mDirection == Direction.BOTTOM) {
                        tileRec.set(mSideWidth + tWidth * j + mGap * j,
                                mSideWidth + tHeight * i + mGap * i + mDataHelper.getOffset(i, j) * (tHeight + mGap) * mMoveScorller.getTimeFraction(),
                                mSideWidth + tWidth * (j + 1) + mGap * j,
                                mSideWidth + tHeight * (i + 1) + mGap * i + mDataHelper.getOffset(i, j) * (tHeight + mGap) * mMoveScorller.getTimeFraction());
                    }

                }

                //绘制瓷砖前景色
                pBg.setColor(calcTileColor(value));
                canvas.drawRoundRect(tileRec, tWidth / 6, tHeight / 6, pBg);

                //绘制瓷砖数字
                pText.setColor(calcTextColor(value));
                float min = tileRec.width() < tileRec.height() ? tileRec.width() : tileRec.height();
                pText.setTextSize(min * 2.1f / (getFigures(value) + 3) );    //文字按大小与位数成反比例函数缩放
                final Paint.FontMetrics fontMetrics = pText.getFontMetrics();
                final float top = fontMetrics.top;
                final float bottom = fontMetrics.bottom;
                //baseLinY = (-top + bottom) / 2 - bottom + base
                final int baseLineY = (int) (tileRec.centerY() - top / 2 - bottom / 2);

                canvas.drawText(String.valueOf(value), tileRec.centerX(), baseLineY, pText);
            }
        }

        if (scrolling || creating || merging) {
            invalidate();
        }
    }



    /**
     * class Scroller
     * 系统自带Scroller的简化版，参见{@link android.widget.Scroller}，
     * 动画辅助类，通过{@link #startScroll(int)}启动动画，然后通过{@link #computeScrollOffset()}计算并返回动画是否结束，
     * 最后可以调用{@link #getTimeFraction()}来获取当前动画进行的时刻。
     */
    public static class Scroller {

        private int mDuration;                                  //动画持续时间
        private long mStartTime;                                //动画开始时间
        private boolean mFinished = true;                       //动画是否结束
        private float mTimeFraction;                            //动画的时刻
        private OnCompleteListener mOnCompleteListener = null;  //动画完成监听
        private Interpolator mInterpolator = null;              //插值器

        interface OnCompleteListener {
            void onComplete();
        }

        Scroller() {

        }

        Scroller(Interpolator i) {
            this.mInterpolator = i;
        }

        void setInterpolator(Interpolator i) {
            this.mInterpolator = i;
        }

        /**
         * 动画开始
         * @param duration 动画持续时间
         */
        public void startScroll(int duration) {
            mFinished = false;
            mDuration = duration;
            mStartTime = AnimationUtils.currentAnimationTimeMillis();
        }

        /**
         * 计算当前动画的时刻（0~1），并返回动画是否结束
         * @return 动画是否结束
         */
        public boolean computeScrollOffset() {
            if (mFinished) {
                return false;
            }
            int timePassed = (int) (AnimationUtils.currentAnimationTimeMillis() - mStartTime);
            if (timePassed < mDuration) {
                mTimeFraction = (float)timePassed / mDuration;
                if (mInterpolator != null) {
                    mTimeFraction = mInterpolator.getInterpolation(mTimeFraction);
                }
            } else {
                mFinished = true;
                if (mOnCompleteListener != null) {      //因为动画完成时最后一个时刻不一定达到1.0f，所以需要一个监听来表示动画确实完成了
                    mOnCompleteListener.onComplete();
                }
            }
            return true;
        }

        /**
         * 获取当前动画的时刻
         * @return 当前动画所处的时刻
         */
        public float getTimeFraction() {
            return mTimeFraction;
        }

        /**
         * 取消动画
         */
        public void abortAnimation() {
            mFinished = true;
        }

        /**
         * 设置动画完成监听
         * @param l 动画完成监听
         */
        public void setOnCompleteListener(OnCompleteListener l) {
            this.mOnCompleteListener = l;
        }
    }



    /**
     * class DataHelper
     *
     * 包括每块瓷砖的数据以及一些用来动画辅助的数据，
     * 用来进行滑动及生成新瓷砖的方法。
     */
    public static class DataHelper {

        int number;                                 //阶数、底数（base）
        private int mTiles[][], mTemp[][];          //瓷砖数组（滑动后即时修改） 和 临时数组（保存旧值用于“移动”动画，因为动画不能即时变化）
        private int mOffsets[][];                   //位移数组（用于“移动”动画，要“移动”多少个单位）
        private boolean mMerged[][];                //合并数组（用于“合并”动画，该位置是否需要进行合并动画）
        private int stepScore, stepMax;             //每走一步的成绩，瓷砖最大值（用来判断是否达到了2048）
        private List<Point> mNewPoints;             //保存每次需要新生成瓷砖的位置，因为第一次游戏和重新游戏需要生成两个砖块，故用列表

        public DataHelper() {
            mNewPoints = new ArrayList<>();
            int[][] defaultTiles = {{0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}};
            setTiles(defaultTiles);
        }

        /**
         * 设置所有瓷砖数值，接受一个二维数组，且二维数组的行数与列数必须相同
         * @param tiles 瓷砖数值二维数组
         */
        public void setTiles(@NonNull int[][] tiles) {
            for (int i = 0; i < tiles.length; i++) {
                if (tiles[i].length != tiles.length) {
                    throw new IllegalArgumentException("The number of rows and of columns must be the same. You have "
                            + tiles.length + " rows but in Row[" + i + "] you set " + tiles[i].length + " columns!");
                }
            }
            this.mTiles = tiles;
            this.number = mTiles.length;
            this.mOffsets = new int[number][number];
            this.mTemp = new int[number][number];
            this.mMerged = new boolean[number][number];
        }

        /**
         * 获取当前所有瓷砖数值
         * @return 当前瓷砖数值数组
         */
        public int[][] getTiles() {
            return mTiles;
        }


        /**
         * 获取某个位置的瓷砖数值
         * @param row 行数
         * @param column 列数
         * @return 该位置的瓷砖数值
         */
        public int get(int row, int column) {
            return mTiles[row][column];
        }

        /**
         * 获取某个位置的旧瓷砖数值，应在滑动后调用
         * @param row 行数
         * @param column 列数
         * @return 该位置的旧值
         */
        public int getTemp(int row, int column) {
            return mTemp[row][column];
        }

        /**
         * 获取某个位置的瓷砖位移值，应在滑动后调用
         * @param row 行数
         * @param column 列数
         * @return 该位置的旧值
         */
        public int getOffset(int row, int column) {
            return mOffsets[row][column];
        }

        /**
         * 获取某个位置是否需要进行合并动画，应在滑动后调用
         * @param row 行数
         * @param column 列数
         * @return 该位置是否需要进行合并动画
         */
        public boolean needMerge(int row, int column) {
            return mMerged[row][column];
        }

        /**
         * 清空瓷砖数值数组
         */
        public void clear() {
            for (int i = 0; i < number; i++) {
                for (int j = 0; j < number; j++) {
                    mTiles[i][j] = 0;
                    mTemp[i][j] = 0;
                    mOffsets[i][j] = 0;
                    mMerged[i][j] = false;
                }
            }
        }

        /**
         * 检查是否可以继续走，应在每次滑动后进行检查
         * @return 是否可以继续走
         */
        public boolean checkAccessibility() {
            for (int i = 0; i < number; i++) {
                for (int j = 0; j < number; j++) {
                    if (mTiles[i][j] == 0) {
                        return true;
                    }
                    if (j != number - 1) {
                        if (mTiles[i][j] == mTiles[i][j + 1]) {
                            return true;
                        }
                    }
                    if (i != number - 1) {
                        if (mTiles[i][j] == mTiles[i + 1][j]) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        /**
         * 添加一个新的瓷砖并记录位置
         */
        public void putOneNewTile() {
            mNewPoints.clear();
            mNewPoints.add(createNewTile());
        }

        /**
         * 添加两个新的瓷砖并记录位置
         */
        public void putTwoNewTile() {
            mNewPoints.clear();
            mNewPoints.add(createNewTile());
            mNewPoints.add(createNewTile());
        }

        /**
         * 取得新添加的瓷砖
         * @return 新添加的瓷砖
         */
        public List<Point> getNewTiles() {
            return mNewPoints;
        }

        private Point createNewTile() {
            List<Point> l = new ArrayList<>();
            for (int i = 0; i < number; i++) {
                for (int j = 0; j < number; j++) {
                    if (mTiles[i][j] == 0) {
                        l.add(new Point(i, j));
                    }
                }
            }
            int randomP = (int)(Math.random() * l.size());
            Point p = l.get(randomP);
            if (Math.random() > 0.18f) {
                mTiles[p.x][p.y] = 2;
            } else {
                mTiles[p.x][p.y] = 4;
            }
            return p;
        }

        //移动之前需要进行初始化
        private void initBeforeGo() {
            for (int i = 0; i < number; i++) {
                for (int j = 0; j < number; j++) {
                    mTemp[i][j] = mTiles[i][j];
                    mOffsets[i][j] = 0;
                    mMerged[i][j] = false;
                }
            }
        }

        /**
         * 向左走，改变了数据返回true，未改变则返回false
         * @return 瓷砖数组是否发生变化
         */
        public boolean goLeft() {
            initBeforeGo();
            boolean hasChanged = false;
            stepMax = stepScore = 0;
            int temp = 0;
            for (int i = 0; i < number; i++) {
                int position = 0;    //辅助游标
                if (stepMax < mTiles[i][position]) stepMax = mTiles[i][0];     //由于第一个数没进入循环，需要专门判断一次
                for (int j = 1; j < number; j++) {
                    if (mTiles[i][j] == 0)
                        continue;
                    //else
                    if (mTiles[i][position] == 0) {                     // [p:0, j:2, 2, 4] -> [p:2, j:0, 2, 4]
                        mTiles[i][position] = mTiles[i][j];
                        mTiles[i][j] = 0;
                        mOffsets[i][j] = position - j;
                        hasChanged = true;
                        temp = mTiles[i][position];
                    } else {    // mTitle[i][position] != 0
                        if (mTiles[i][position] == mTiles[i][j]) {      // [p:2, 0, j:2, 4] -> [4, p:0, j:0, 4]
                            mTiles[i][position] += mTiles[i][j];
                            mTiles[i][j] = 0;
                            mOffsets[i][j] = position - j;
                            stepScore += mTiles[i][position];
                            hasChanged = true;
                            temp = mTiles[i][position];
                            mMerged[i][position] = true;
                            ++position;
                        } else {                                        // [p:2, j:4, 8, 16] -> [2, p:j:4, 8, 16] or
                            ++position;                                 // [p:2, 0, j:4, 16] -> [2, p:4, j:0, 16]
                            if (position < j) {
                                mTiles[i][position] = mTiles[i][j];
                                mTiles[i][j] = 0;
                                hasChanged = true;
                            }
                            mOffsets[i][j] = position - j;
                            temp = mTiles[i][position];
                        }
                    }
                    if (stepMax < temp) {
                        stepMax = temp;
                    }
                }
            }
            return hasChanged;
        }

        /**
         * 向右走，改变了数据返回true，未改变则返回false
         * @return 瓷砖数组是否发生变化
         */
        public boolean goRight() {
            initBeforeGo();
            boolean hasChanged = false;
            stepMax = stepScore = 0;
            int temp = 0;
            for (int i = 0; i < number; i++) {
                int position = number - 1;
                if (stepMax < mTiles[i][position]) stepMax = mTiles[i][position];
                for (int j = position - 1; j >= 0; j--) {
                    if (mTiles[i][j] == 0)
                        continue;
                    if (mTiles[i][position] == 0) {
                        mTiles[i][position] = mTiles[i][j];
                        mTiles[i][j] = 0;
                        mOffsets[i][j] = position - j;
                        hasChanged = true;
                        temp = mTiles[i][position];
                    } else {
                        if (mTiles[i][position] == mTiles[i][j]) {
                            mTiles[i][position] += mTiles[i][j];
                            mTiles[i][j] = 0;
                            mOffsets[i][j] = position - j;
                            stepScore += mTiles[i][position];
                            hasChanged = true;
                            temp = mTiles[i][position];
                            mMerged[i][position] = true;
                            --position;
                        } else {
                            --position;
                            if (position > j) {
                                mTiles[i][position] = mTiles[i][j];
                                mTiles[i][j] = 0;
                                hasChanged = true;
                            }
                            mOffsets[i][j] = position - j;
                            temp = mTiles[i][position];
                        }
                    }
                    if (stepMax < temp) {
                        stepMax = temp;
                    }
                }
            }
            return hasChanged;
        }

        /**
         * 向上走，改变了数据返回true，未改变则返回false
         * @return 瓷砖数组是否发生变化
         */
        public boolean goTop() {
            initBeforeGo();
            boolean hasChanged = false;
            stepMax = stepScore = 0;
            int temp = 0;
            for (int i = 0; i < number; i++) {
                int position = 0;
                if (stepMax < mTiles[position][i]) stepMax = mTiles[position][i];
                for (int j = 1; j < number; j++) {
                    if (mTiles[j][i] == 0)
                        continue;
                    if (mTiles[position][i] == 0) {
                        mTiles[position][i] = mTiles[j][i];
                        mTiles[j][i] = 0;
                        mOffsets[j][i] = position - j;
                        hasChanged = true;
                        temp = mTiles[position][i];
                    } else {
                        if (mTiles[position][i] == mTiles[j][i]) {
                            mTiles[position][i] += mTiles[j][i];
                            mTiles[j][i] = 0;
                            mOffsets[j][i] = position - j;
                            stepScore += mTiles[position][i];
                            hasChanged = true;
                            temp = mTiles[position][i];
                            mMerged[position][i] = true;
                            ++position;
                        } else {
                            ++position;
                            if (position < j) {
                                mTiles[position][i] = mTiles[j][i];
                                mTiles[j][i] = 0;
                                hasChanged = true;
                            }
                            mOffsets[j][i] = position - j;
                            temp = mTiles[position][i];
                        }
                    }
                    if (stepMax < temp) {
                        stepMax = temp;
                    }
                }
            }
            return hasChanged;
        }

        /**
         * 向下走，改变了数据返回true，未改变则返回false
         * @return 瓷砖数组是否发生变化
         */
        public boolean goBottom() {
            initBeforeGo();
            boolean hasChanged = false;
            stepMax = stepScore = 0;
            int temp = 0;
            for (int i = 0; i < number; i++) {
                int position = number - 1;
                if (stepMax < mTiles[position][i]) stepMax = mTiles[position][i];
                for (int j = position - 1; j >= 0; j--) {
                    if (mTiles[j][i] == 0)
                        continue;
                    if (mTiles[position][i] == 0) {
                        mTiles[position][i] = mTiles[j][i];
                        mTiles[j][i] = 0;
                        mOffsets[j][i] = position - j;
                        hasChanged = true;
                        temp = mTiles[i][j];
                    } else {
                        if (mTiles[position][i] == mTiles[j][i]) {
                            mTiles[position][i] += mTiles[j][i];
                            mTiles[j][i] = 0;
                            mOffsets[j][i] = position - j;
                            stepScore += mTiles[position][i];
                            hasChanged = true;
                            temp = mTiles[position][i];
                            mMerged[position][i] = true;
                            --position;
                        } else {
                            --position;
                            if (position > j) {
                                mTiles[position][i] = mTiles[j][i];
                                mTiles[j][i] = 0;
                                hasChanged = true;
                            }
                            mOffsets[j][i] = position - j;
                            temp = mTiles[position][i];
                        }
                    }
                    if (stepMax < temp) {
                        stepMax = temp;
                    }
                }
            }
            return hasChanged;
        }

    } // DataHelper


    //用于辅助生成新的瓷砖，见addNewTile()
    static class Point {
        int x, y;
        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    //用于计算某个瓷砖数值对应的前景颜色
    static int calcTileColor(int value) {
        int color = 0;
        if (value <= 2) {
            color = 0xffeee4da;
        } else if (value <= 4) {
            color = 0xffede0c8;
        } else if (value <= 8) {
            color = 0xfff2b179;
        } else if (value <= 16) {
            color = 0xfff59563;
        } else if (value <= 32) {
            color = 0xfff67c5f;
        } else if (value <= 64) {
            color = 0xfff65e3b;
        } else if (value <= 128) {
            color = 0xffedcf72;
        } else if (value <= 256) {
            color = 0xffedcc61;
        } else if (value <= 512) {
            color = 0xffedc850;
        } else if (value <= 1024) {
            color = 0xffedc53f;
        } else if (value <= 2048) {
            color = 0xffe5b804;
        } else if (value <= 4096) {
            color = 0xffa8b825;
        } else if (value <= 8192) {
            color = 0xff50b811;
        } else {
            color = 0xffc40dd4;
        }
        return color;
    }

    //用于计算某个瓷砖数值对应的字体颜色
    static int calcTextColor(int value) {
        int color = 0xfff9f6f2;
        if (value <= 4) color = 0xff776e65;
        return color;
    }

    // 获取一个数字有几位
    static int getFigures(int n) {
        int result = 0;
        do {
            n /= 10;
            ++result;
        } while (n > 0);
        return result;
    }
}
