CREATE TABLE app_release_policy (
    id VARCHAR(32) PRIMARY KEY,
    revision BIGINT NOT NULL,
    payload TEXT NOT NULL
);
INSERT INTO app_release_policy(id, revision, payload) VALUES ('android', 1,
'{"minSupportedVersionCode":1,"deprecatedVersionCode":1,"latestVersionCode":3,"latestVersionName":"1.2.1","minSupportedMajor":1,"updateUrl":"https://github.com/TatoniMatteo/DietReminder/releases/latest"}');
