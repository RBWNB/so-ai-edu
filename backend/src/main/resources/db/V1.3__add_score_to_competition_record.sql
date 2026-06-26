ALTER TABLE competition_record
    ADD COLUMN score INT NOT NULL DEFAULT 0 COMMENT '本场积分（答对1题2分 + 全对额外20分）' AFTER correct_count;
