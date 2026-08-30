(function () {
    'use strict';

    function output(message) {
        if (typeof print === 'function') {
            print(message);
        } else {
            console.log(message);
        }
    }

    function referencedFileIds(records) {
        var ids = [];
        var seen = {};
        records.forEach(function (record) {
            var id = record && record.gridFsFileId;
            if (typeof id === 'string' && id.trim() !== '' && !seen[id]) {
                seen[id] = true;
                ids.push(id);
            }
        });
        return ids;
    }

    function runCleanup(database, execute, toObjectId) {
        if (!database || typeof database.getCollection !== 'function') {
            throw new Error('必须在 mongosh 中连接目标数据库后执行该脚本');
        }
        var metadata = database.getCollection('image_metadata');
        var files = database.getCollection('fs.files');
        var chunks = database.getCollection('fs.chunks');
        var records = metadata.find({}).toArray();
        var ids = referencedFileIds(records);
        var summary = {
            preview: !execute,
            legacyMetadataCount: records.length,
            uniqueFileCount: ids.length,
            successFileCount: 0,
            missingFileCount: 0,
            failedFileCount: 0,
            failedFileIds: [],
            metadataDeletedCount: 0
        };

        if (!execute) {
            output('预览模式：未写入任何数据。' + JSON.stringify(summary));
            return summary;
        }

        ids.forEach(function (id) {
            try {
                var objectId = toObjectId(id);
                var fileResult = files.deleteOne({_id: objectId});
                if (!fileResult || fileResult.deletedCount !== 1) {
                    summary.missingFileCount++;
                    return;
                }
                chunks.deleteMany({files_id: objectId});
                summary.successFileCount++;
            } catch (error) {
                summary.failedFileCount++;
                summary.failedFileIds.push(id);
                output('清理 GridFS 文件失败，fileId=' + id + '，错误=' + error.message);
            }
        });
        summary.metadataDeletedCount = metadata.deleteMany({}).deletedCount;
        output('历史足迹清理汇总：' + JSON.stringify(summary));
        return summary;
    }

    var api = {referencedFileIds: referencedFileIds, runCleanup: runCleanup};
    if (typeof module !== 'undefined' && module.exports) {
        module.exports = api;
    }
    if (typeof db !== 'undefined' && db && typeof db.getCollection === 'function') {
        var execute = typeof EXECUTE !== 'undefined' && EXECUTE === true;
        runCleanup(db, execute, function (id) { return ObjectId(id); });
    }
}());
