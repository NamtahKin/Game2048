package com.namtah.game2048;

//import com.namtah.game2048.widget.GameView;

import org.junit.Test;

//import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

//    @Test
//    public void test_figure() {
//        int re = GameView.getFigures(23145);
//        System.out.println(re);
//    }

//    @Test
//    public void test_grid() {
//        GameView.DataHelper dataHelper = new GameView.DataHelper();
//
//        int t[][] = {   {2, 0, 2, 4},
//                        {0, 4, 2, 4},
//                        {4, 0, 2, 8},
//                        {0, 0, 2, 0}};
//        dataHelper.setTiles(t);
//
//        long t1 = System.currentTimeMillis();
//        dataHelper.goTop();
//        long t2 = System.currentTimeMillis();
//
//        System.out.println(Arrays.toString(t[0]));
//        System.out.println(Arrays.toString(t[1]));
//        System.out.println(Arrays.toString(t[2]));
//        System.out.println(Arrays.toString(t[3]));
//
//        for (int i = 0; i < 4; i++) {
//            System.out.print("[");
//            for (int j = 0; j < 4; j++) {
//                System.out.print(dataHelper.getOffset(i, j) + " ");
//            }
//            System.out.println("]");
//        }
//        System.out.println("in " + (t2 - t1) + "ms");
//    }
}