package ais.hskl.tobi;

public interface Constants
{
    public static final int CAMERA_PERMISSION_CODE = 42;

    public static final String TOBI_SHARED_PREFERENCES = "tobi";
    public static final String DETECTION_SCORE = "detection_score";
    public static final String SHOW_DEBUG = "show_debug";

    public static enum SIGNS
    {
        SPEED_LIMIT_30,
        SPEED_LIMIT_50,
        SPEED_LIMIT_60,
        SPEED_LIMIT_70,
        SPEED_LIMIT_80,
        END_SPEED_LIMIT_80,
        SPEED_LIMIT_100,
        SPEED_LIMIT_120,
        NO_OVERTAKING,
        NO_OVERTAKING_TRUCK,
        RIGHT_OF_WAY,
        MAJOR_ROAD,
        GIVE_WAY,
        STOP,
        RESTRICTION_ALL,
        RESTRICTION_TRUCK,
        RESTRICTION_ENTRY,
        DANGER,
        CURVE_LEFT,
        CURVE_RIGHT,
        DOUBLE_CURVE,
        UNEVEN_ROAD,
        SLIPPERY_ROAD,
        NARROW_ROAD,
        CONSTRUCTION,
        TRAFFIC_LIGHT,
        PEDESTRIAN,
        CHILDREN,
        BIKE,
        SNOW_ICE,
        ANIMALS,
        END_RESTRICTION_ALL,
        RIGHT,
        LEFT,
        STRAIGHT,
        RIGHT_OR_STRAIGHT,
        LEFT_OR_STRAIGHT,
        PASS_RIGHT,
        PASS_LEFT,
        ROUND_ABOUT,
        END_NO_OVERTAKING,
        END_NO_OVERTAKING_TRUCKS;

        public static int mapSignToImageResource(Constants.SIGNS detectedClass)
        {
            switch (detectedClass)
            {
                case SPEED_LIMIT_30:
                    return R.drawable.speed_limit_30;
                case SPEED_LIMIT_50:
                    return R.drawable.speed_limit_50;
                case SPEED_LIMIT_60:
                    return R.drawable.speed_limit_60;
                case SPEED_LIMIT_70:
                    return R.drawable.speed_limit_70;
                case SPEED_LIMIT_80:
                    return R.drawable.speed_limit_80;
                case END_SPEED_LIMIT_80:
                    return R.drawable.end_speed_limit_80;
                case SPEED_LIMIT_100:
                    return R.drawable.speed_limit_100;
                case SPEED_LIMIT_120:
                    return R.drawable.speed_limit_120;
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
    }
}
