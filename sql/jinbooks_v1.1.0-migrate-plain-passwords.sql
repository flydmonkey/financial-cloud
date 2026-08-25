-- Audit and verify legacy {plain} passwords before/after deploying plain-password removal.
-- The application migrates these rows to bcrypt automatically on startup via PlainPasswordMigrator.

-- Before deploy: list affected users
SELECT id, username, password
FROM `jinbooks`.`userinfo`
WHERE `password` LIKE '{plain}%'
  AND `deleted` = 'n';

-- After first startup: should return 0
SELECT COUNT(*) AS plain_password_count
FROM `jinbooks`.`userinfo`
WHERE `password` LIKE '{plain}%'
  AND `deleted` = 'n';
