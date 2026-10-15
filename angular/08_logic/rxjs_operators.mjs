/**
 * The same four flattening operators, verified against REAL RxJS.
 * (pure_logic.mjs implements them by hand; this proves the hand-written semantics are right.)
 *
 *   cd angular && npm install && npm run rxjs
 */
import {
  concatMap,
  debounceTime,
  distinctUntilChanged,
  exhaustMap,
  firstValueFrom,
  map,
  mergeMap,
  of,
  scan,
  shareReplay,
  switchMap,
  take,
  timer,
  toArray,
} from 'rxjs';

function check(label, actual, expected) {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
  console.log(`${label.padEnd(40)} ${a}`);
}

/** Source: A at 0ms, B at 5ms, C at 10ms (take(3) completes it). */
const source = () => timer(0, 5).pipe(map((i) => ['A', 'B', 'C'][i]), take(3));

/** Inner: two values, 12ms apart - slow enough that the source overtakes it. */
const inner = (value) => timer(12, 12).pipe(map((i) => `${value}${i + 1}`), take(2));

async function main() {
  check('switchMap  cancels previous', await firstValueFrom(source().pipe(switchMap(inner), toArray())), ['C1', 'C2']);
  check('exhaustMap ignores while busy', await firstValueFrom(source().pipe(exhaustMap(inner), toArray())), ['A1', 'A2']);
  check('concatMap  queues, keeps order', await firstValueFrom(source().pipe(concatMap(inner), toArray())),
        ['A1', 'A2', 'B1', 'B2', 'C1', 'C2']);

  const merged = await firstValueFrom(source().pipe(mergeMap(inner), toArray()));
  check('mergeMap   runs all concurrently', [merged.length, [...merged].sort()],
        [6, ['A1', 'A2', 'B1', 'B2', 'C1', 'C2']]);

  // typeahead pipeline: debounce -> distinct -> switchMap
  const typed = timer(0, 30).pipe(map((i) => ['a', 'ab', 'ab', 'abc'][i]), take(4));
  const searches = await firstValueFrom(
    typed.pipe(
      debounceTime(20),
      distinctUntilChanged(),
      switchMap((term) => of(`results for ${term}`)),
      toArray(),
    ),
  );
  check('typeahead skips repeats', searches, ['results for a', 'results for ab', 'results for abc']);

  // scan = a running reducer (how a BehaviorSubject store accumulates)
  check('scan accumulates', await firstValueFrom(of(1, 2, 3).pipe(scan((total, n) => total + n, 0), toArray())), [1, 3, 6]);

  // shareReplay: one execution shared by several subscribers (cold -> hot)
  let executions = 0;
  const shared = of(1).pipe(
    map((value) => {
      executions++;
      return value;
    }),
    shareReplay({ bufferSize: 1, refCount: false }),
  );
  await firstValueFrom(shared);
  await firstValueFrom(shared);
  check('shareReplay runs the source once', executions, 1);

  console.log('\nall checks passed');
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
