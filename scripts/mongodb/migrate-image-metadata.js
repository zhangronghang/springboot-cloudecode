(function () {
    'use strict';

    var OLD_TIME_PATTERN = /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/;
    var NEW_TIME_PATTERN = /^\d{14}$/;

    function isValidFourteenDigitTime(value) {
        if (typeof value !== 'string' || !NEW_TIME_PATTERN.test(value)) {
            return false;
        }
        var year = Number(value.substring(0, 4));
        var month = Number(value.substring(4, 6));
        var day = Number(value.substring(6, 8));
        var hour = Number(value.substring(8, 10));
        var minute = Number(value.substring(10, 12));
        var second = Number(value.substring(12, 14));
        var date = new Date(year, month - 1, day, hour, minute, second);
        return date.getFullYear() === year
            && date.getMonth() === month - 1
            && date.getDate() === day
            && date.getHours() === hour
            && date.getMinutes() === minute
            && date.getSeconds() === second;
    }

    function normalizeUploadTime(value) {
        if (typeof value !== 'string') {
            return null;
        }
        var normalized = OLD_TIME_PATTERN.test(value) ? value.replace(/[- :]/g, '') : value;
        return isValidFourteenDigitTime(normalized) ? normalized : null;
    }

    function hasOwn(document, property) {
        return Object.prototype.hasOwnProperty.call(document, property);
    }

    function buildMigrationUpdate(document) {
        var normalizedUploadTime = normalizeUploadTime(document.uploadTime);
        if (normalizedUploadTime === null) {
            return {
                action: 'skip',
                reason: 'invalid_upload_time',
                value: document.uploadTime
            };
        }

        var update = {};
        if (document.uploadTime !== normalizedUploadTime) {
            update.uploadTime = normalizedUploadTime;
        }
        if (document.createTime === undefined || document.createTime === null || document.createTime === '') {
            update.createTime = normalizedUploadTime;
        }
        if (!hasOwn(document, 'provinceCode')) {
            update.provinceCode = '';
        }
        if (!hasOwn(document, 'districtCode')) {
            update.districtCode = '';
        }

        return Object.keys(update).length === 0
            ? {action: 'unchanged'}
            : {action: 'update', update: update};
    }

    function output(value) {
        if (typeof print === 'function') {
            print(value);
        } else {
            console.log(value);
        }
    }

    function runMigration(database, dryRun) {
        if (!database || typeof database.getCollection !== 'function') {
            throw new Error('必须在 mongosh 中连接目标数据库后执行该脚本');
        }

        var collection = database.getCollection('image_metadata');
        var summary = {scanned: 0, planned: 0, applied: 0, unchanged: 0, skipped: 0};
        output('目标数据库：' + database.getName() + '，collection：image_metadata，DRY_RUN=' + dryRun);

        collection.find({}).forEach(function (document) {
            summary.scanned++;
            var result = buildMigrationUpdate(document);
            if (result.action === 'skip') {
                summary.skipped++;
                output('跳过记录 _id=' + document._id + '，uploadTime=' + result.value + '，原因=' + result.reason);
                return;
            }
            if (result.action === 'unchanged') {
                summary.unchanged++;
                return;
            }

            summary.planned++;
            if (dryRun) {
                output('[DRY RUN] 将更新 _id=' + document._id + '，字段=' + JSON.stringify(result.update));
                return;
            }
            collection.updateOne({_id: document._id}, {$set: result.update});
            summary.applied++;
        });

        output('迁移汇总：' + JSON.stringify(summary));
        return summary;
    }

    var api = {
        normalizeUploadTime: normalizeUploadTime,
        buildMigrationUpdate: buildMigrationUpdate,
        runMigration: runMigration
    };

    if (typeof module !== 'undefined' && module.exports) {
        module.exports = api;
    }

    if (typeof db !== 'undefined' && db && typeof db.getCollection === 'function') {
        // 首次执行保持 true，仅核对输出；确认无误后由用户手工改为 false 再执行。
        var DRY_RUN = true;
        runMigration(db, DRY_RUN);
    }
}());
