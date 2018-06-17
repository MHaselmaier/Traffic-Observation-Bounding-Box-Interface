package ais.hskl.tobi;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.flexbox.FlexboxLayout;

public class SignsView extends FlexboxLayout
{
    private static final int SIGN_VISIBILITY_DURATION_MS = 3000;

    private Context context;
    private ImageView[] signs;
    private long[] signsTiming;

    public SignsView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        this.context = context;

        createImageViewArray();
    }

    private void createImageViewArray()
    {
        int size = this.context.getResources().getDimensionPixelSize(R.dimen.sign_size);
        Constants.SIGNS[] signConstants = Constants.SIGNS.values();
        this.signs = new ImageView[signConstants.length];
        this.signsTiming = new long[signConstants.length];
        for (int i = 0; this.signs.length > i; ++i)
        {
            this.signs[i] = new ImageView(this.context);
            this.signs[i].setImageResource(mapSignToImageResource(signConstants[i]));
            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(size, size);
            this.signs[i].setLayoutParams(layoutParams);
            this.signs[i].setVisibility(View.GONE);
            this.addView(signs[i]);
            this.signsTiming[i] = SystemClock.uptimeMillis();
        }
    }

    private int mapSignToImageResource(Constants.SIGNS detectedClass)
    {
        switch (detectedClass)
        {
            case SPEED_LIMIT_30:
                //return R.drawable.SPEED_LIMIT_30;
            case SPEED_LIMIT_50:
                //return R.drawable.SPEED_LIMIT_50;
            case SPEED_LIMIT_60:
                return R.drawable.speed_limit_60;
            case SPEED_LIMIT_70:
                //return R.drawable.SPEED_LIMIT_70;
            case SPEED_LIMIT_80:
                //return R.drawable.SPEED_LIMIT_80;
            case END_SPEED_LIMIT_80:
                //return R.drawable.END_SPEED_LIMIT_80;
            case SPEED_LIMIT_100:
                //return R.drawable.SPEED_LIMIT_100;
            case SPEED_LIMIT_120:
                //return R.drawable.SPEED_LIMIT_120;
            case NO_OVERTAKING:
                return R.drawable.no_overtaking;
            case NO_OVERTAKING_TRUCK:
                return R.drawable.no_overtaking_truck;
            case RIGHT_OF_WAY:
                return R.drawable.right_of_way;
            case MAJOR_ROAD:
                return R.drawable.major_road;
            case GIVE_WAY:
                return R.drawable.give_way;
            case STOP:
                return R.drawable.stop;
            case RESTRICTION_ALL:
                return R.drawable.restriction_all;
            case RESTRICTION_TRUCK:
                return R.drawable.restriction_truck;
            case RESTRICTION_ENTRY:
                return R.drawable.restriction_entry;
            case DANGER:
                return R.drawable.danger;
            case CURVE_LEFT:
                return R.drawable.curve_left;
            case CURVE_RIGHT:
                return R.drawable.curve_right;
            case DOUBLE_CURVE:
                return R.drawable.double_curve;
            case UNEVEN_ROAD:
                return R.drawable.uneven_road;
            case SLIPPERY_ROAD:
                return R.drawable.slippery_road;
            case NARROW_ROAD:
                return R.drawable.narrow_road;
            case CONSTRUCTION:
                return R.drawable.construction;
            case TRAFFIC_LIGHT:
                return R.drawable.traffic_light;
            case PEDESTRIAN:
                return R.drawable.pedestrian;
            case CHILDREN:
                return R.drawable.children;
            case BIKE:
                return R.drawable.bike;
            case SNOW_ICE:
                return R.drawable.snow_ice;
            case ANIMALS:
                return R.drawable.animals;
            case END_RESTRICTION_ALL:
                return R.drawable.end_restriction_all;
            case RIGHT:
                return R.drawable.right;
            case LEFT:
                return R.drawable.left;
            case STRAIGHT:
                return R.drawable.straight;
            case RIGHT_OR_STRAIGHT:
                return R.drawable.right_or_straight;
            case LEFT_OR_STRAIGHT:
                return R.drawable.left_or_straight;
            case PASS_RIGHT:
                return R.drawable.pass_right;
            case PASS_LEFT:
                return R.drawable.pass_left;
            case ROUND_ABOUT:
                return R.drawable.round_about;
            case END_NO_OVERTAKING:
                return R.drawable.end_no_overtaking;
            case END_NO_OVERTAKING_TRUCKS:
                return R.drawable.end_no_overtaking_truck;
        }
        return -1;
    }

    public void updateSigns(TobiNetwork.DetectedObject[] detectedObjects)
    {
        for (TobiNetwork.DetectedObject detectedObject: detectedObjects)
        {
            this.signsTiming[detectedObject.getDetectedClass().ordinal()] = SystemClock.uptimeMillis() + SignsView.SIGN_VISIBILITY_DURATION_MS;
        }

        for (int i = 0; this.signs.length > i; ++i)
        {
            if (this.signsTiming[i] > SystemClock.uptimeMillis())
            {
                this.signs[i].setVisibility(View.VISIBLE);
            }
            else
            {
                this.signs[i].setVisibility(View.GONE);
            }
        }
    }
}
