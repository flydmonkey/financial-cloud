-- Disable MaxKey/SSO menu entries that no longer have backend or frontend support.
-- Run against the jinbooks database after deploying the frontend cleanup.

UPDATE `jinbooks`.`resources`
SET `status` = '0', `is_visible` = 'n'
WHERE `deleted` = 'n'
  AND (
    `request_url` LIKE '/apps%'
    OR `request_url` LIKE '/sync%'
    OR `request_url` LIKE '/access%'
    OR `request_url` LIKE '/accounts%'
    OR `request_url` LIKE '/monitor%'
    OR `request_url` LIKE '/tool%'
    OR `request_url` LIKE '/system/dict%'
    OR `request_url` LIKE '/system/post%'
    OR `request_url` LIKE '/system/role%'
    OR `request_url` LIKE '/security/gradings%'
    OR `request_url` LIKE '/security/ldapcontext%'
    OR `request_url` LIKE '/security/configweakpassword%'
    OR `request_url` LIKE '/security/configpasswordencrypt%'
    OR `request_url` LIKE '/config/adapters%'
    OR `request_url` LIKE '/config/appscategory%'
    OR `request_url` LIKE '/config/expandattrs%'
    OR `permission` LIKE 'apps:%'
    OR `permission` LIKE 'monitor:%'
    OR `permission` LIKE 'tool:%'
  );

-- Point surviving resource-management menu to the relocated Vue page.
UPDATE `jinbooks`.`resources`
SET `request_url` = '/permissions/resources'
WHERE `deleted` = 'n'
  AND `request_url` IN ('/permissions/apps/resources', 'permissions/apps/resources');
