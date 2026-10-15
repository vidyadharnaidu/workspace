# Design a Video Streaming Platform (YouTube / Netflix)

> **Remember:** *"Upload the raw file straight to object storage → async transcoding pipeline (split into segments, encode many resolutions in parallel, package HLS/DASH) → CDN serves segments → the player uses adaptive bitrate."*

## 1. Requirements

**Functional**
- Upload videos (large files, resumable)
- Watch videos smoothly on any device and network (adaptive quality)
- Video metadata: title, description, thumbnails; search; view counts

**Non-functional**
- Playback starts fast (< 2 s) with minimal buffering; **global**
- Uploads can be processed asynchronously (minutes is OK)
- Very read-heavy (views ≫ uploads); huge storage and bandwidth → cost matters
- Durable: uploaded videos must never be lost

## 2. Estimates
- 500 hours of video uploaded per minute ≈ 720k hours/day
- Raw storage: 1 hour 1080p ≈ 2.5 GB → ~1.8 PB/day raw. With all renditions (4–6 resolutions), roughly 2–3× more → **several PB/day** → object storage with tiering
- Views: 1B hours watched/day. At ~3 Mbps average that's enormous egress, so **the CDN carries ~all traffic**

## 3. API
```
POST /v1/videos                         { title, description } -> { videoId, uploadUrl(s) (pre-signed, multipart) }
PUT  <pre-signed S3 part URLs>          (client uploads chunks directly, resumable)
POST /v1/videos/{id}/upload-complete    -> 202 (processing starts)
GET  /v1/videos/{id}                    -> metadata + manifest URL (when READY)
GET  <CDN>/videos/{id}/master.m3u8      -> HLS manifest -> segment URLs per quality
```

## 4. Data model
```
videos      (video_id, owner_id, title, description, status: UPLOADING|PROCESSING|READY|FAILED,
             duration, created_at, manifest_url, thumbnail_url)          -- SQL / Cassandra
renditions  (video_id, resolution, codec, bitrate, segment_prefix)
view_counts aggregated asynchronously (Kafka -> stream counts -> cache/DB)
object storage: raw/{video_id}/original.mp4,  hls/{video_id}/{720p}/seg_00001.ts, master.m3u8
```

## 5. High-level design

```
 UPLOAD
 Creator --1. create--> [Upload Service] --> [Metadata DB] (status=UPLOADING)
    |  <-- pre-signed multipart URLs --
    |--2. PUT chunks------------------------------> [Object Storage: raw bucket]
    |--3. complete--> [Upload Service] --> [Queue: video_uploaded]
                                               |
                                               v
                       +------------------ TRANSCODING PIPELINE (DAG) ------------------+
                       |  validate/inspect -> split into ~4 s GOP-aligned chunks          |
                       |    -> [Encoder workers] x N (parallel: 240p..4K, H.264/VP9/AV1) |
                       |    -> merge -> package HLS/DASH segments + manifests            |
                       |    -> thumbnails, captions (ASR), content moderation / DRM      |
                       +-----------------------------------------------------------------+
                                               |
                          [Object Storage: processed] --> origin for --> [CDN edges worldwide]
                                               |
                                    [Metadata DB] status=READY -> notify creator
 WATCH
 Viewer -> [API: metadata + manifest URL] ; Player -> [CDN] manifest -> segments (adaptive bitrate)
        -> playback events (heartbeats) -> [Kafka] -> view counts, watch history, recommendations
```

## 6. Deep dives

### Resumable uploads
- Multipart upload: a 5 GB file in 10 MB parts. Retry only failed parts, and track part ETags. Pre-signed URLs mean app servers never touch the bytes.

### Transcoding pipeline
- **Split into segments first**, encode segments in parallel across many workers, then stitch. A 2-hour movie finishes in minutes, not hours.
- Model it as a **DAG of tasks** with a workflow engine or queue per stage. Each task is idempotent, and failed tasks retry independently.
- Store intermediate results in object storage, not on the workers (they are stateless and can be spot instances).

### Adaptive bitrate streaming (HLS/DASH)
- Each rendition is cut into 2–6 s segments. A **master manifest** lists the renditions.
- The player measures throughput/buffer and switches quality segment by segment, which avoids buffering on bad networks.

### CDN strategy
- Popular videos are cached at edges (pre-warmed for big releases). Long-tail videos are fetched from origin/regional caches on demand.
- Netflix-style: put caching appliances inside ISPs for the most-watched content.
- Signed URLs/cookies for paid content; DRM (Widevine/FairPlay) for studios.

### View counts
- Don't update the DB per view. Playback events go to Kafka, get deduped and aggregated (validity rules, e.g. ≥ 30 s watched), and counts are periodically written to the cache/DB.

## 7. Trade-offs
- Encode every resolution up front (storage cost) vs on demand for rare videos (latency on first view). Hybrid: encode popular formats first, rare ones lazily.
- Codec choice: H.264 (universal) vs VP9/AV1 (≈30–50% smaller, more CPU to encode). Use newer codecs for popular videos.
- Storage tiering: move old, unwatched renditions to cold storage.

## 8. Follow-up questions
- **Live streaming?** RTMP/SRT ingest → real-time transcoder → low-latency HLS/DASH (short segments) or WebRTC → CDN. There's no pre-processing window.
- **Resume watching on another device?** Store watch progress from playback heartbeats per (user, video).
- **Handle a viral video?** It's the CDN's job. Pre-warm edges, use origin shield to protect the origin, and cache the metadata API aggressively.
