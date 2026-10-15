# Design Cloud File Storage & Sync (Dropbox / Google Drive)

> **Remember:** *"Split files into content-hashed chunks, upload only chunks the server doesn't have (dedupe + delta sync) directly to object storage, keep a strongly consistent metadata DB of file → versions → chunk lists, and notify other devices to pull changes."*

## 1. Requirements

**Functional**
- Upload/download files of any size; folders
- Automatic sync across a user's devices; offline edits sync later
- File versions / restore; share files & folders with permissions

**Non-functional**
- **Never lose or corrupt files**; strong consistency of metadata
- Efficient bandwidth (large files, small edits); resumable transfers
- Scale: hundreds of millions of users, exabytes of data

## 2. Estimates
- 500M users, 100M DAU; average user stores 10 GB → **5 EB** logical (dedupe and compression reduce physical usage)
- Uploads: 100M DAU × 2 file changes/day = 200M/day → ~2.3k changes/s

## 3. API
```
POST /v1/files/upload-session   { path, size, chunkHashes[] }
     -> { sessionId, missingChunks: [{hash, uploadUrl (pre-signed)}] }     (dedupe: only missing chunks)
PUT  <pre-signed URL>            (chunk bytes, directly to object storage)
POST /v1/files/commit            { sessionId, baseVersion }  -> { fileId, version }  | 409 conflict
GET  /v1/changes?cursor=<c>      -> { changes[], nextCursor }  (what changed since my last sync)
GET  /v1/files/{id}/versions/{v} -> chunk list with download URLs
POST /v1/shares                  { fileId|folderId, userId, role }
Notifications: long-poll / WebSocket "something changed, call /changes"
```

## 4. Data model
```
files          (file_id, owner_id, parent_folder_id, name, is_folder, latest_version, deleted)   -- SQL, sharded by owner/namespace
file_versions  (file_id, version, size, chunk_hashes[] (ordered), modified_by, created_at)
chunks         (chunk_hash PK (SHA-256), size, storage_location, ref_count)
changes_journal(namespace_id, seq (monotonic), file_id, version, op)    -- drives /changes cursor
shares         (resource_id, grantee_id, role)
object storage: chunks/{sha256}   (immutable, content-addressed)
```

## 5. High-level design

```
 Device (sync client: watcher + chunker + local DB of synced state)
   |  1. file changed -> split into ~4 MB chunks -> SHA-256 each
   |  2. upload-session (hashes) ------------------> [Metadata Service] -> which chunks are missing? -> [Chunks table]
   |  3. PUT missing chunks ------------------------> [Object Storage]  (pre-signed, parallel, resumable)
   |  4. commit(baseVersion) -----------------------> [Metadata Service] -> transaction: new version + journal entry
   |                                                        |
   |                                                        +--> [Notification Service] (long-poll/WebSocket)
   |                                                                     |
 Other devices <----------- "changes available" ---------------------------+
   |  5. GET /changes?cursor -> new version chunk list -> download only chunks not present locally -> rebuild file
```

## 6. Deep dives

### Chunking, dedupe and delta sync
- Fixed 4 MB chunks are simple. **Content-defined chunking** (rolling hash, e.g. Rabin fingerprints) keeps chunk boundaries stable when bytes are inserted, so a small edit changes only 1–2 chunks.
- Content addressing (`sha256 → chunk`) gives **dedupe**: the same chunk across versions/users is stored once, and the upload skips known chunks. (For privacy, cross-user dedupe is sometimes limited.)
- Resumable: chunks are independent. Retry only the failed ones.

### Consistency & conflicts
- The metadata commit is a SQL transaction with **optimistic concurrency**: `commit` includes `baseVersion`. If the server's latest version ≠ baseVersion, someone else changed the file, so return 409.
- **Conflict resolution:** keep both, saving the loser as `report (conflicted copy from Laptop).docx`. Silent merging is unsafe for arbitrary files. (Google Docs-style real-time co-editing uses OT/CRDTs, which is a different problem.)
- Chunks are uploaded **before** the commit, so metadata never points to missing data. Garbage-collect unreferenced chunks later (ref counts + grace period).

### Sync protocol
- A per-namespace **change journal** with monotonic sequence numbers. Each device stores its cursor, then asks "changes since seq X". It's efficient, works after being offline for days, and easy to reason about.
- Notification is only a hint ("something changed"). The client always pulls via `/changes`, so missed notifications are harmless.

### Storage durability & cost
- Object storage with erasure coding / cross-zone replication (11 nines).
- Tiering: rarely accessed versions/chunks move to cold storage.
- Encryption at rest per chunk; keys in KMS.

## 7. Trade-offs
- Smaller chunks: better dedupe/delta but more metadata and requests. ~4 MB is a common middle.
- Strong consistency for metadata (SQL, sharded by namespace) vs eventual for notifications.
- Client complexity (local state DB, watcher, chunker) buys large bandwidth savings.

## 8. Follow-up questions
- **Sharing a folder with 1,000 users: how do their devices learn about changes?** Shared folders are their own namespace with a journal, and each member's client tracks a cursor per namespace it can access.
- **Large file download on mobile?** Stream chunks in parallel via CDN/pre-signed URLs, and allow partial/on-demand download ("files on demand" placeholders).
- **Restore a deleted folder?** Deletes are soft (a journal entry + deleted flag). Restoring re-links previous versions within the retention window.
