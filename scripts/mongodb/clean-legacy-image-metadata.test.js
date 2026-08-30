const assert = require('assert');
const cleanup = require('./clean-legacy-image-metadata');

function database() {
    const files = new Map([['legacy-1', {}], ['legacy-2', {}], ['shared-file', {}]]);
    const chunks = new Map([['legacy-1', {}], ['legacy-2', {}], ['shared-file', {}]]);
    const records = [{gridFsFileId: 'legacy-1'}, {gridFsFileId: 'legacy-1'}, {gridFsFileId: 'legacy-2'}, {gridFsFileId: 'missing-file'}];
    return {
        getName: () => 'claude_code',
        getCollection: name => {
            if (name === 'image_metadata') return {
                find: () => ({toArray: () => records.slice()}),
                deleteMany: () => { const count = records.length; records.length = 0; return {deletedCount: count}; }
            };
            if (name === 'fs.files') return {deleteOne: query => ({deletedCount: files.delete(query._id) ? 1 : 0})};
            if (name === 'fs.chunks') return {deleteMany: query => ({deletedCount: chunks.delete(query.files_id) ? 1 : 0})};
            throw new Error('unexpected collection ' + name);
        },
        state: {files, chunks, records}
    };
}

const previewDatabase = database();
const preview = cleanup.runCleanup(previewDatabase, false, id => id);
assert.deepStrictEqual(preview, {
    preview: true, legacyMetadataCount: 4, uniqueFileCount: 3,
    successFileCount: 0, missingFileCount: 0, failedFileCount: 0, failedFileIds: [], metadataDeletedCount: 0
});
assert.strictEqual(previewDatabase.state.records.length, 4);
assert.strictEqual(previewDatabase.state.files.size, 3);

const executeDatabase = database();
const executed = cleanup.runCleanup(executeDatabase, true, id => id);
assert.deepStrictEqual(executed, {
    preview: false, legacyMetadataCount: 4, uniqueFileCount: 3,
    successFileCount: 2, missingFileCount: 1, failedFileCount: 0, failedFileIds: [], metadataDeletedCount: 4
});
assert.deepStrictEqual(Array.from(executeDatabase.state.files.keys()), ['shared-file']);
assert.deepStrictEqual(Array.from(executeDatabase.state.chunks.keys()), ['shared-file']);
assert.strictEqual(executeDatabase.state.records.length, 0);
console.log('clean-legacy-image-metadata tests passed');
