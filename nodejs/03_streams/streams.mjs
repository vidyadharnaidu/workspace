/**
 * Streams and backpressure - what separates a junior from a senior Node answer.
 *
 * Run: node nodejs/03_streams/streams.mjs
 *
 * WHY STREAMS:
 *  Reading a 2 GB file with fs.readFile puts 2 GB in RAM (and V8's Buffer limit may reject it).
 *  A stream moves it in chunks (64 KB by default), so memory stays flat no matter the file size.
 *
 * THE FOUR TYPES:
 *  Readable  - source (fs.createReadStream, http request, S3 object)
 *  Writable  - sink (fs.createWriteStream, http response, socket)
 *  Duplex    - both (TCP socket)
 *  Transform - duplex that transforms (zlib.createGzip, a CSV parser, a cipher)
 *
 * BACKPRESSURE (the key concept):
 *  writable.write() returns FALSE when its internal buffer is full. You must stop writing and wait
 *  for the 'drain' event. pipe()/pipeline() do this for you - which is why you should always use them
 *  instead of manual read/write loops. Ignoring backpressure = unbounded memory growth.
 *
 *  ALWAYS use stream.pipeline (not .pipe) so errors propagate and every stream is destroyed properly.
 */
import { Readable, Transform, Writable } from 'node:stream';
import { pipeline } from 'node:stream/promises';
import { createGunzip, createGzip } from 'node:zlib';
import { createHash, randomBytes } from 'node:crypto';
import { performance } from 'node:perf_hooks';

function check(label, actual, expected) {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
  console.log(`${label.padEnd(42)} ${a}`);
}

// ---------------------------------------------------------------- 1. creating streams

/** A readable from any iterable - the easiest way to start a pipeline. */
const numbers = (count) => Readable.from(Array.from({ length: count }, (_, i) => i + 1));

/** Transform: object mode so chunks can be objects instead of Buffers. */
const toOrder = () =>
  new Transform({
    objectMode: true,
    transform(id, _encoding, callback) {
      callback(null, { id, total: id * 10 }); // callback(error, value) - never throw synchronously
    },
  });

/** Writable sink that collects what it received (a test double for a database writer). */
function collector(sink) {
  return new Writable({
    objectMode: true,
    write(chunk, _encoding, callback) {
      sink.push(chunk);
      callback();
    },
  });
}

// ---------------------------------------------------------------- 2. NDJSON / CSV style line parsing

/** Splits a byte stream into lines - the classic "parse a huge log file" question. */
function splitLines() {
  let remainder = '';
  return new Transform({
    readableObjectMode: true,
    transform(chunk, _encoding, callback) {
      const lines = (remainder + chunk.toString('utf8')).split('\n');
      remainder = lines.pop() ?? ''; // the last piece may be an incomplete line
      for (const line of lines) if (line.trim()) this.push(line);
      callback();
    },
    flush(callback) {
      if (remainder.trim()) this.push(remainder); // don't lose the final line
      callback();
    },
  });
}

const parseJson = () =>
  new Transform({
    objectMode: true,
    transform(line, _encoding, callback) {
      try {
        callback(null, JSON.parse(line));
      } catch (error) {
        callback(new Error(`invalid JSON: ${line}`, { cause: error })); // errors travel down the pipeline
      }
    },
  });

// ---------------------------------------------------------------- 3. backpressure, observed

/** A slow writable: it only accepts a chunk every few ms, so a fast producer MUST slow down. */
function slowWritable(delayMs, sink) {
  return new Writable({
    objectMode: true,
    highWaterMark: 2, // tiny buffer so backpressure kicks in immediately
    write(chunk, _encoding, callback) {
      sink.push(chunk);
      setTimeout(callback, delayMs); // signalling "ready for more" only after the delay
    },
  });
}

async function backpressureDemo() {
  const written = [];
  const produced = [];
  const source = new Readable({
    objectMode: true,
    highWaterMark: 2,
    read() {
      if (produced.length >= 10) {
        this.push(null); // end of stream
        return;
      }
      const value = produced.length;
      produced.push(value);
      this.push(value);
    },
  });

  await pipeline(source, slowWritable(5, written));
  return { produced: produced.length, written: written.length };
}

// ---------------------------------------------------------------- 4. memory: stream vs buffer everything

async function memoryComparison(totalBytes) {
  const chunk = randomBytes(64 * 1024);
  const chunkCount = Math.ceil(totalBytes / chunk.length);

  // Streaming: constant memory, we only ever hold one chunk plus the hash state
  const before = process.memoryUsage().heapUsed;
  const hash = createHash('sha256');
  await pipeline(
    Readable.from(
      (function* () {
        for (let i = 0; i < chunkCount; i++) yield chunk;
      })(),
    ),
    new Writable({
      write(piece, _encoding, callback) {
        hash.update(piece);
        callback();
      },
    }),
  );
  const streamedHeapGrowth = process.memoryUsage().heapUsed - before;

  return { digestLength: hash.digest('hex').length, streamedHeapGrowthMB: streamedHeapGrowth / 1024 / 1024 };
}

// ---------------------------------------------------------------- 5. compression pipeline + error handling

async function gzipRoundTrip(text) {
  const compressed = [];
  await pipeline(Readable.from([text]), createGzip(), collectorBuffer(compressed));
  const decompressed = [];
  await pipeline(Readable.from(compressed), createGunzip(), collectorBuffer(decompressed));
  return {
    originalBytes: Buffer.byteLength(text),
    compressedBytes: Buffer.concat(compressed).length,
    roundTripped: Buffer.concat(decompressed).toString('utf8') === text,
  };
}

function collectorBuffer(sink) {
  return new Writable({
    write(chunk, _encoding, callback) {
      sink.push(chunk);
      callback();
    },
  });
}

// ---------------------------------------------------------------- run

const orders = [];
await pipeline(numbers(5), toOrder(), collector(orders));
check('transform pipeline', orders.map((o) => o.total), [10, 20, 30, 40, 50]);

const parsed = [];
const ndjson = '{"id":1}\n{"id":2}\n{"id":3}';
await pipeline(Readable.from([Buffer.from(ndjson.slice(0, 12)), Buffer.from(ndjson.slice(12))]), splitLines(), parseJson(), collector(parsed));
check('line splitting across chunks', parsed.map((row) => row.id), [1, 2, 3]);

const pipelineError = await pipeline(Readable.from(['not json']), splitLines(), parseJson(), collector([]))
  .then(() => null)
  .catch((error) => error.message);
check('pipeline propagates errors', pipelineError, 'invalid JSON: not json');

const { produced, written } = await backpressureDemo();
check('backpressure: nothing lost', { produced, written }, { produced: 10, written: 10 });

const start = performance.now();
const memory = await memoryComparison(8 * 1024 * 1024); // 8 MB through a constant-memory pipeline
check('streamed 8MB, sha256 produced', memory.digestLength, 64);
console.log(`${'heap growth while streaming 8MB'.padEnd(42)} ${memory.streamedHeapGrowthMB.toFixed(2)}MB in ${(performance.now() - start).toFixed(0)}ms`);
check('memory stayed flat', memory.streamedHeapGrowthMB < 8, true);

const gzip = await gzipRoundTrip('log line: user logged in\n'.repeat(500));
check('gzip round trip', gzip.roundTripped, true);
check('compression shrank the data', gzip.compressedBytes < gzip.originalBytes / 5, true);
console.log(`${'gzip'.padEnd(42)} ${gzip.originalBytes} -> ${gzip.compressedBytes} bytes`);

console.log('\nall checks passed');

/**
 * INTERVIEW FOLLOW-UPS
 *  "How would you process a 10 GB CSV?"      -> createReadStream + split into lines + transform + write,
 *                                               with pipeline() so memory stays flat and errors propagate.
 *  "How do you upload a file without RAM?"   -> pipe the request stream straight to storage (or a pre-signed URL).
 *  "What is objectMode?"                     -> chunks are JS objects instead of Buffers/strings.
 *  "pipe vs pipeline?"                       -> pipe leaks on error (no cleanup, no error forwarding); pipeline
 *                                               destroys every stream and gives you one error.
 *  "How do you know backpressure works?"     -> write() returns false and you wait for 'drain' - or you use pipeline.
 */
