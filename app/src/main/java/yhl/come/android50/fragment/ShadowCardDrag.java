package yhl.come.android50.fragment;

/**
 * Created by yuhailong on 15/11/26.
 */

import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.RoundRectShape;
import android.graphics.drawable.shapes.Shape;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import java.util.ArrayList;

import yhl.come.android50.R;

public class ShadowCardDrag extends Fragment {
    private static final float MAX_Z_DP = 10;
    private static final float MOMENTUM_SCALE = 10;
    private static final int MAX_ANGLE = 10;

    private class CardDragState {
        long lastEventTime;
        float lastX;
        float lastY;

        float momentumX;
        float momentumY;

        public void onDown(long eventTime, float x, float y) {
            lastEventTime = eventTime;
            lastX = x;
            lastY = y;

            momentumX = 0;
            momentumY = 0;
        }

        public void onMove(long eventTime, float x, float y) {
            final long deltaT = eventTime - lastEventTime;

            if (deltaT != 0) {
                float newMomentumX = (x - lastX) / (mDensity * deltaT);
                float newMomentumY = (y - lastY) / (mDensity * deltaT);

                momentumX = 0.9f * momentumX + 0.1f * (newMomentumX * MOMENTUM_SCALE);
                momentumY = 0.9f * momentumY + 0.1f * (newMomentumY * MOMENTUM_SCALE);

                momentumX = Math.max(Math.min((momentumX), MAX_ANGLE), -MAX_ANGLE);
                momentumY = Math.max(Math.min((momentumY), MAX_ANGLE), -MAX_ANGLE);

                //noinspection SuspiciousNameCombination
                mCard.setRotationX(-momentumY);
                //noinspection SuspiciousNameCombination
                mCard.setRotationY(momentumX);

                if (mShadingEnabled) {
                    float alphaDarkening = (momentumX * momentumX + momentumY * momentumY) / (90 * 90);
                    alphaDarkening /= 2;

                    int alphaByte = 0xff - ((int)(alphaDarkening * 255) & 0xff);
                    int color = Color.rgb(alphaByte, alphaByte, alphaByte);
                    mCardBackground.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
                }
            }

            lastX = x;
            lastY = y;
            lastEventTime = eventTime;
        }

        public void onUp() {
            ObjectAnimator flattenX = ObjectAnimator.ofFloat(mCard, "rotationX", 0);
            flattenX.setDuration(100);
            flattenX.setInterpolator(new AccelerateInterpolator());
            flattenX.start();

            ObjectAnimator flattenY = ObjectAnimator.ofFloat(mCard, "rotationY", 0);
            flattenY.setDuration(100);
            flattenY.setInterpolator(new AccelerateInterpolator());
            flattenY.start();
            mCardBackground.setColorFilter(null);
        }
    }

    /**
     * Simple shape example that generates a shadow casting outline.
     */
    private static class TriangleShape extends Shape {
        private final Path mPath = new Path();

        @Override
        protected void onResize(float width, float height) {
            mPath.reset();
            mPath.moveTo(0, 0);
            mPath.lineTo(width, 0);
            mPath.lineTo(width / 2, height);
            mPath.lineTo(0, 0);
            mPath.close();
        }

        @Override
        public void draw(Canvas canvas, Paint paint) {
            canvas.drawPath(mPath, paint);
        }

        @Override
        public void getOutline(Outline outline) {
            outline.setConvexPath(mPath);
        }
    }

    private final ShapeDrawable mCardBackground = new ShapeDrawable();
    private final ArrayList<Shape> mShapes = new ArrayList<>();
    private float mDensity;
    private View mCard;

    private final CardDragState mDragState = new CardDragState();
    private boolean mTiltEnabled;
    private boolean mShadingEnabled;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return View.inflate(getActivity(), R.layout.shadow_card_drag,null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mDensity = getResources().getDisplayMetrics().density;
        mShapes.add(new RectShape());
        mShapes.add(new OvalShape());
        float r = 10 * mDensity;
        float radii[] = new float[] {r, r, r, r, r, r, r, r};
        mShapes.add(new RoundRectShape(radii, null, null));
        mShapes.add(new TriangleShape());

        mCardBackground.getPaint().setColor(Color.WHITE);
        mCardBackground.setShape(mShapes.get(0));
        final View cardParent = view.findViewById(R.id.card_parent);
        mCard = view.findViewById(R.id.card);
        mCard.setBackground(mCardBackground);

        final CheckBox tiltCheck = (CheckBox) view.findViewById(R.id.tilt_check);
        tiltCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mTiltEnabled = isChecked;
                if (!mTiltEnabled) {
                    mDragState.onUp();
                }
            }
        });

        final CheckBox shadingCheck = (CheckBox) view.findViewById(R.id.shading_check);
        shadingCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mShadingEnabled = isChecked;
                if (!mShadingEnabled) {
                    mCardBackground.setColorFilter(null);
                }
            }
        });

        final Button shapeButton = (Button) view.findViewById(R.id.shape_select);
        shapeButton.setOnClickListener(new View.OnClickListener() {
            int index = 0;
            @Override
            public void onClick(View v) {
                index = (index + 1) % mShapes.size();
                mCardBackground.setShape(mShapes.get(index));
            }
        });

        /**
         * Enable any touch on the parent to drag the card. Note that this doesn't do a proper hit
         * test, so any drag (including off of the card) will work.
         *
         * This enables the user to see the effect more clearly for the purpose of this demo.
         */
        cardParent.setOnTouchListener(new View.OnTouchListener() {
            float downX;
            float downY;
            long downTime;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getX() - mCard.getTranslationX();
                        downY = event.getY() - mCard.getTranslationY();
                        downTime = event.getDownTime();
                        ObjectAnimator upAnim = ObjectAnimator.ofFloat(mCard, "translationZ",
                                MAX_Z_DP * mDensity);
                        upAnim.setDuration(100);
                        upAnim.setInterpolator(new DecelerateInterpolator());
                        upAnim.start();
                        if (mTiltEnabled) {
                            mDragState.onDown(event.getDownTime(), event.getX(), event.getY());
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        mCard.setTranslationX(event.getX() - downX);
                        mCard.setTranslationY(event.getY() - downY);
                        if (mTiltEnabled) {
                            mDragState.onMove(event.getEventTime(), event.getX(), event.getY());
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        ObjectAnimator downAnim = ObjectAnimator.ofFloat(mCard, "translationZ", 0);
                        downAnim.setDuration(100);
                        downAnim.setInterpolator(new AccelerateInterpolator());
                        downAnim.start();
                        if (mTiltEnabled) {
                            mDragState.onUp();
                        }
                        break;
                }
                return true;
            }
        });
    }
}