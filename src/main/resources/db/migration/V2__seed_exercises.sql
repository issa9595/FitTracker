-- =====================================================================
-- FitTracker V2 - Referentiel d'exercices (running, MMA, lutte,
-- musculation, mobilite). UUID deterministes : memes valeurs que
-- l'ancien ExerciseSeed.java pour stabilite des tests existants.
-- =====================================================================

INSERT INTO exercises (id, name, category, muscle_group, unit) VALUES
('00000000-0000-0000-0000-0000000000a1', 'Course a pied',         'RUNNING',   'cardio',    'DISTANCE'),
('00000000-0000-0000-0000-0000000000a2', 'Course fractionnee',    'RUNNING',   'cardio',    'TIME'),
('00000000-0000-0000-0000-0000000000b1', 'Developpe couche',      'STRENGTH',  'pectoraux', 'REPS'),
('00000000-0000-0000-0000-0000000000b2', 'Squat',                 'STRENGTH',  'jambes',    'REPS'),
('00000000-0000-0000-0000-0000000000b3', 'Souleve de terre',      'STRENGTH',  'dos',       'REPS'),
('00000000-0000-0000-0000-0000000000b4', 'Tractions',             'STRENGTH',  'dos',       'REPS'),
('00000000-0000-0000-0000-0000000000c1', 'Jab cross',             'MMA',       'full body', 'REPS'),
('00000000-0000-0000-0000-0000000000c2', 'Low kick',              'MMA',       'jambes',    'REPS'),
('00000000-0000-0000-0000-0000000000c3', 'Sac de frappe',         'MMA',       'full body', 'TIME'),
('00000000-0000-0000-0000-0000000000d1', 'Single leg takedown',   'WRESTLING', 'fullbody',  'REPS'),
('00000000-0000-0000-0000-0000000000d2', 'Sprawl',                'WRESTLING', 'full body', 'REPS'),
('00000000-0000-0000-0000-0000000000e1', 'Etirement',             'OTHER',     'mobilite',  'TIME');
