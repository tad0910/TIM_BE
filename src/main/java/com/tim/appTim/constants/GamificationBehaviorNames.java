package com.tim.appTim.constants;

public class GamificationBehaviorNames {
    public static final String ATTEND_ON_TIME = "Điểm danh đúng giờ";
    public static final String HIGH_POINT_1 = "Đạt điểm cao (>80%)";
    public static final String HIGH_POINT_2 = "Đạt điểm xuất sắc (>95%)";
    public static final String FIRST_POST = "Đăng bài viết đầu tiên";
    public static final String POSTS_LIKE = "Bài viết được yêu thích (>10 lượt thích)";
    public static final String POST_SHARE = "Chia sẻ kiến thức (Bài viết có link)";
    public static final String READ_BLOG = "Lần đầu tiên truy cập tin tức";
    public static final String GIVING_SCORES = "Giáo viên chấm điểm 10";

    private GamificationBehaviorNames() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
