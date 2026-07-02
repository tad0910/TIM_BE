UPDATE posts p
SET tong_comments = (
    SELECT COUNT(*) FROM comments c WHERE c.bai_viet_id = p.id
) + (
    SELECT COUNT(*) FROM reply_comments rc
    JOIN comments c ON rc.comments_id = c.id
    WHERE c.bai_viet_id = p.id
);

UPDATE posts p
SET tong_reactions = (
    SELECT COUNT(*) FROM reactions r 
    WHERE r.bai_viet_id = p.id 
      AND r.comment_id IS NULL 
      AND r.reply_comment_id IS NULL
);
