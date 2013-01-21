/*
 * Copyright 2012 Roman Nurik
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.spoon.ordering.wizard.ui;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import com.example.spoon.ordering.R;

public class StepPagerStrip extends View {
  private static final int[] ATTRS = new int[] {
      android.R.attr.gravity
  };
  private int mPageCount;
  private int mCurrentPage;

  private int mGravity = Gravity.LEFT | Gravity.TOP;
  private float mTabWidth;
  private float mTabHeight;
  private float mTabSpacing;

  private Paint mPrevTabPaint;
  private Paint mSelectedTabPaint;
  private Paint mSelectedLastTabPaint;
  private Paint mNextTabPaint;

  private RectF mTempRectF = new RectF();

  //private Scroller mScroller;

  private OnPageSelectedListener mOnPageSelectedListener;

  public StepPagerStrip(Context context) {
    this(context, null, 0);
  }

  public StepPagerStrip(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public StepPagerStrip(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);

    final TypedArray a = context.obtainStyledAttributes(attrs, ATTRS);
    mGravity = a.getInteger(0, mGravity);
    a.recycle();

    final Resources res = getResources();
    mTabWidth = res.getDimensionPixelSize(R.dimen.step_pager_tab_width);
    mTabHeight = res.getDimensionPixelSize(R.dimen.step_pager_tab_height);
    mTabSpacing = res.getDimensionPixelSize(R.dimen.step_pager_tab_spacing);

    mPrevTabPaint = new Paint();
    mPrevTabPaint.setColor(res.getColor(R.color.step_pager_previous_tab_color));

    mSelectedTabPaint = new Paint();
    mSelectedTabPaint.setColor(res.getColor(R.color.step_pager_selected_tab_color));

    mSelectedLastTabPaint = new Paint();
    mSelectedLastTabPaint.setColor(res.getColor(R.color.step_pager_selected_last_tab_color));

    mNextTabPaint = new Paint();
    mNextTabPaint.setColor(res.getColor(R.color.step_pager_next_tab_color));
  }

  public void setOnPageSelectedListener(OnPageSelectedListener onPageSelectedListener) {
    mOnPageSelectedListener = onPageSelectedListener;
  }

  @Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    if (mPageCount == 0) {
      return;
    }

    float totalWidth = mPageCount * (mTabWidth + mTabSpacing) - mTabSpacing;
    float totalLeft;
    boolean fillHorizontal = false;

    switch (mGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
      case Gravity.CENTER_HORIZONTAL:
        totalLeft = (getWidth() - totalWidth) / 2;
        break;
      case Gravity.RIGHT:
        totalLeft = getWidth() - getPaddingRight() - totalWidth;
        break;
      case Gravity.FILL_HORIZONTAL:
        totalLeft = getPaddingLeft();
        fillHorizontal = true;
        break;
      default:
        totalLeft = getPaddingLeft();
    }

    switch (mGravity & Gravity.VERTICAL_GRAVITY_MASK) {
      case Gravity.CENTER_VERTICAL:
        mTempRectF.top = (int) (getHeight() - mTabHeight) / 2;
        break;
      case Gravity.BOTTOM:
        mTempRectF.top = getHeight() - getPaddingBottom() - mTabHeight;
        break;
      default:
        mTempRectF.top = getPaddingTop();
    }

    mTempRectF.bottom = mTempRectF.top + mTabHeight;

    float tabWidth = mTabWidth;
    if (fillHorizontal) {
      tabWidth =
          (getWidth() - getPaddingRight() - getPaddingLeft() - (mPageCount - 1) * mTabSpacing)
              / mPageCount;
    }

    for (int i = 0; i < mPageCount; i++) {
      mTempRectF.left = totalLeft + (i * (tabWidth + mTabSpacing));
      mTempRectF.right = mTempRectF.left + tabWidth;
      canvas.drawRect(mTempRectF, i < mCurrentPage ? mPrevTabPaint
          : (i > mCurrentPage ? mNextTabPaint
              : (i == mPageCount - 1 ? mSelectedLastTabPaint : mSelectedTabPaint)));
    }
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    setMeasuredDimension(View.resolveSize(
        (int) (mPageCount * (mTabWidth + mTabSpacing) - mTabSpacing)
            + getPaddingLeft()
            + getPaddingRight(), widthMeasureSpec),
        View.resolveSize((int) mTabHeight + getPaddingTop() + getPaddingBottom(),
            heightMeasureSpec));
  }

  @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    scrollCurrentPageIntoView();
    super.onSizeChanged(w, h, oldw, oldh);
  }

  @Override public boolean onTouchEvent(MotionEvent event) {
    if (mOnPageSelectedListener != null) {
      switch (event.getActionMasked()) {
        case MotionEvent.ACTION_DOWN:
        case MotionEvent.ACTION_MOVE:
          int position = hitTest(event.getX());
          if (position >= 0) {
            mOnPageSelectedListener.onPageStripSelected(position);
          }
          return true;
        default:
          // Pass through.
      }
    }
    return super.onTouchEvent(event);
  }

  private int hitTest(float x) {
    if (mPageCount == 0) {
      return -1;
    }

    float totalWidth = mPageCount * (mTabWidth + mTabSpacing) - mTabSpacing;
    float totalLeft;
    boolean fillHorizontal = false;

    switch (mGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
      case Gravity.CENTER_HORIZONTAL:
        totalLeft = (getWidth() - totalWidth) / 2;
        break;
      case Gravity.RIGHT:
        totalLeft = getWidth() - getPaddingRight() - totalWidth;
        break;
      case Gravity.FILL_HORIZONTAL:
        totalLeft = getPaddingLeft();
        fillHorizontal = true;
        break;
      default:
        totalLeft = getPaddingLeft();
    }

    float tabWidth = mTabWidth;
    if (fillHorizontal) {
      tabWidth =
          (getWidth() - getPaddingRight() - getPaddingLeft() - (mPageCount - 1) * mTabSpacing)
              / mPageCount;
    }

    float totalRight = totalLeft + (mPageCount * (tabWidth + mTabSpacing));
    if (x >= totalLeft && x <= totalRight && totalRight > totalLeft) {
      return (int) (((x - totalLeft) / (totalRight - totalLeft)) * mPageCount);
    } else {
      return -1;
    }
  }

  public void setCurrentPage(int currentPage) {
    mCurrentPage = currentPage;
    invalidate();
    scrollCurrentPageIntoView();

    // TODO: Set content description appropriately
  }

  private void scrollCurrentPageIntoView() {
    // TODO: only works with left gravity for now
    //
    //float widthToActive = getPaddingLeft() + (mCurrentPage + 1) * (mTabWidth + mTabSpacing)
    //        - mTabSpacing;
    //int viewWidth = getWidth();
    //
    //int startScrollX = getScrollX();
    //int destScrollX = (widthToActive > viewWidth) ? (int) (widthToActive - viewWidth) : 0;
    //
    //if (mScroller == null) {
    //    mScroller = new Scroller(getContext());
    //}
    //
    //mScroller.abortAnimation();
    //mScroller.startScroll(startScrollX, 0, destScrollX - startScrollX, 0);
    //postInvalidate();
  }

  public void setPageCount(int count) {
    mPageCount = count;
    invalidate();

    // TODO: Set content description appropriately
  }

  public interface OnPageSelectedListener {
    void onPageStripSelected(int position);
  }

  //@Override public void computeScroll() {
  //    super.computeScroll();
  //    if (mScroller.computeScrollOffset()) {
  //        setScrollX(mScroller.getCurrX());
  //    }
  //}
}
