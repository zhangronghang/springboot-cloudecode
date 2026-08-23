const assert = require('assert');
const migration = require('./migrate-image-metadata');

function testOldTimeMigration() {
    const result = migration.buildMigrationUpdate({
        _id: 'old-record',
        uploadTime: '2026-08-23 17:10:11'
    });

    assert.deepStrictEqual(result, {
        action: 'update',
        update: {
            uploadTime: '20260823171011',
            createTime: '20260823171011',
            provinceCode: '',
            districtCode: ''
        }
    });
}

function testNewTimeMigrationDoesNotOverwriteExistingValues() {
    const result = migration.buildMigrationUpdate({
        _id: 'new-record',
        uploadTime: '20260823171011',
        createTime: '20200101000000',
        provinceCode: '11',
        districtCode: '1101'
    });

    assert.deepStrictEqual(result, {action: 'unchanged'});
}

function testInvalidTimeIsSkippedWithoutAnyUpdate() {
    const result = migration.buildMigrationUpdate({
        _id: 'bad-record',
        uploadTime: 'invalid-time'
    });

    assert.deepStrictEqual(result, {
        action: 'skip',
        reason: 'invalid_upload_time',
        value: 'invalid-time'
    });
}

testOldTimeMigration();
testNewTimeMigrationDoesNotOverwriteExistingValues();
testInvalidTimeIsSkippedWithoutAnyUpdate();
console.log('migrate-image-metadata tests passed');
