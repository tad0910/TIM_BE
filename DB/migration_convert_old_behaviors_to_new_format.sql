
INSERT INTO behavior_point_types (behavior_id, point_type_id, points, created_at)
SELECT 
    id AS behavior_id,
    1 AS point_type_id,  -- ID của "Điểm Chuyên cần"
    point_diligence AS points,
    created_at
FROM gamification_behaviors
WHERE point_diligence > 0
  AND id NOT IN (
      SELECT behavior_id 
      FROM behavior_point_types 
      WHERE point_type_id = 1
  );

-- Bước 3: Convert behaviors có point_competence > 0
INSERT INTO behavior_point_types (behavior_id, point_type_id, points, created_at)
SELECT 
    id AS behavior_id,
    2 AS point_type_id,  -- ID của "Điểm Năng lực"
    point_competence AS points,
    created_at
FROM gamification_behaviors
WHERE point_competence > 0
  AND id NOT IN (
      SELECT behavior_id 
      FROM behavior_point_types 
      WHERE point_type_id = 2
  );

-- Bước 4: Convert behaviors có point_experience > 0
INSERT INTO behavior_point_types (behavior_id, point_type_id, points, created_at)
SELECT 
    id AS behavior_id,
    3 AS point_type_id,  -- ID của "Điểm Kinh nghiệm"
    point_experience AS points,
    created_at
FROM gamification_behaviors
WHERE point_experience > 0
  AND id NOT IN (
      SELECT behavior_id 
      FROM behavior_point_types 
      WHERE point_type_id = 3
  );

