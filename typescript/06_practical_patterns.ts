/**
 * Practical TypeScript patterns that show up in real code reviews and take-homes.
 *
 * Run:   node typescript/06_practical_patterns.ts
 *
 * PATTERNS:
 *  1. Discriminated union state machine (loading / success / error) - no impossible states
 *  2. Typed reducer (Redux-style) with exhaustive action handling
 *  3. Type-safe event emitter (payload type per event name)
 *  4. Generic API client with typed endpoints
 *  5. DTO -> domain mapping
 *  6. Options objects with sensible defaults
 *  7. Type-safe object helpers (Object.keys loses types!)
 */

function check(label: string, actual: unknown, expected: unknown): void {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
  console.log(`${label.padEnd(36)} ${a}`);
}

// ---------------------------------------------------------------- 1. state machine

type RequestState<T> =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "success"; data: T }
  | { status: "error"; error: string };

function renderState<T>(state: RequestState<T>): string {
  switch (state.status) {
    case "idle":
      return "nothing yet";
    case "loading":
      return "loading...";
    case "success":
      return `data: ${JSON.stringify(state.data)}`;   // only here does `data` exist
    case "error":
      return `failed: ${state.error}`;
    default: {
      const exhaustive: never = state;
      throw new Error(`unhandled state ${JSON.stringify(exhaustive)}`);
    }
  }
}

// ---------------------------------------------------------------- 2. typed reducer

interface CartItem {
  sku: string;
  quantity: number;
  price: number;
}

interface CartState {
  items: CartItem[];
  coupon?: string;
}

type CartAction =
  | { type: "add"; item: CartItem }
  | { type: "remove"; sku: string }
  | { type: "changeQuantity"; sku: string; quantity: number }
  | { type: "applyCoupon"; code: string }
  | { type: "clear" };

function cartReducer(state: CartState, action: CartAction): CartState {
  switch (action.type) {
    case "add":
      return { ...state, items: [...state.items, action.item] };
    case "remove":
      return { ...state, items: state.items.filter((i) => i.sku !== action.sku) };
    case "changeQuantity":
      return {
        ...state,
        items: state.items.map((i) => (i.sku === action.sku ? { ...i, quantity: action.quantity } : i)),
      };
    case "applyCoupon":
      return { ...state, coupon: action.code };
    case "clear":
      return { items: [] };
    default: {
      const exhaustive: never = action;
      throw new Error(`unhandled action ${JSON.stringify(exhaustive)}`);
    }
  }
}

const cartTotal = (state: CartState): number =>
  state.items.reduce((sum, i) => sum + i.price * i.quantity, 0) * (state.coupon === "SAVE10" ? 0.9 : 1);

// ---------------------------------------------------------------- 3. type-safe event emitter

type EventMap = {
  orderPlaced: { orderId: string; total: number };
  userLoggedIn: { userId: number };
  error: Error;
};

class TypedEmitter<Events extends Record<string, unknown>> {
  #handlers: { [K in keyof Events]?: ((payload: Events[K]) => void)[] } = {};

  on<K extends keyof Events>(event: K, handler: (payload: Events[K]) => void): () => void {
    (this.#handlers[event] ??= []).push(handler);
    return () => {
      this.#handlers[event] = (this.#handlers[event] ?? []).filter((h) => h !== handler);
    };
  }

  emit<K extends keyof Events>(event: K, payload: Events[K]): number {
    const handlers = this.#handlers[event] ?? [];
    handlers.forEach((handler) => handler(payload));
    return handlers.length;
  }
}

// ---------------------------------------------------------------- 4. generic API client

interface ApiEndpoints {
  "GET /users": { request: { page: number }; response: { id: number; name: string }[] };
  "POST /orders": { request: { sku: string; qty: number }; response: { orderId: string } };
}

type ApiRequest<E extends keyof ApiEndpoints> = ApiEndpoints[E]["request"];
type ApiResponse<E extends keyof ApiEndpoints> = ApiEndpoints[E]["response"];

// A #private field cannot be referenced in a type (ApiClient["#handlers"] does not compile),
// so give the handler map its own named type.
type ApiHandlers = { [K in keyof ApiEndpoints]: (req: ApiRequest<K>) => ApiResponse<K> };

class ApiClient {
  #handlers: ApiHandlers;

  constructor(handlers: ApiHandlers) {
    this.#handlers = handlers;
  }

  async call<E extends keyof ApiEndpoints>(endpoint: E, request: ApiRequest<E>): Promise<ApiResponse<E>> {
    return this.#handlers[endpoint](request);       // request and response types are tied to the endpoint
  }
}

// ---------------------------------------------------------------- 5. DTO -> domain mapping

interface UserDto {
  user_id: number;
  full_name: string;
  created_at: string;
  is_active: 0 | 1;
}

interface DomainUser {
  id: number;
  name: string;
  createdAt: Date;
  active: boolean;
}

const toDomainUser = (dto: UserDto): DomainUser => ({
  id: dto.user_id,
  name: dto.full_name,
  createdAt: new Date(dto.created_at),
  active: dto.is_active === 1,
});

// ---------------------------------------------------------------- 6. options with defaults

interface RetryOptions {
  attempts?: number;
  baseDelayMs?: number;
  onRetry?: (attempt: number) => void;
}

function withDefaults(options: RetryOptions = {}): Required<Omit<RetryOptions, "onRetry">> & RetryOptions {
  return { attempts: 3, baseDelayMs: 100, ...options };
}

// ---------------------------------------------------------------- 7. type-safe object helpers

/** Object.keys returns string[], which loses the key union - this helper keeps it. */
const typedKeys = <T extends object>(obj: T): (keyof T)[] => Object.keys(obj) as (keyof T)[];
const typedEntries = <T extends object>(obj: T): [keyof T, T[keyof T]][] =>
  Object.entries(obj) as [keyof T, T[keyof T]][];

const settings = { theme: "dark", fontSize: 14 };

const states: RequestState<{ id: number }>[] = [
  { status: "idle" },
  { status: "loading" },
  { status: "success", data: { id: 1 } },
  { status: "error", error: "500" },
];
check("state machine rendering", states.map(renderState),
      ["nothing yet", "loading...", 'data: {"id":1}', "failed: 500"]);

let cart: CartState = { items: [] };
cart = cartReducer(cart, { type: "add", item: { sku: "A", quantity: 1, price: 100 } });
cart = cartReducer(cart, { type: "add", item: { sku: "B", quantity: 2, price: 25 } });
cart = cartReducer(cart, { type: "changeQuantity", sku: "A", quantity: 3 });
cart = cartReducer(cart, { type: "applyCoupon", code: "SAVE10" });
check("typed reducer total", [cart.items.length, cartTotal(cart)], [2, 315]);
check("reducer clear", cartReducer(cart, { type: "clear" }), { items: [] });

const emitter = new TypedEmitter<EventMap>();
const log: string[] = [];
const off = emitter.on("orderPlaced", (payload) => log.push(`order ${payload.orderId} for ${payload.total}`));
emitter.on("userLoggedIn", (payload) => log.push(`user ${payload.userId}`));
emitter.emit("orderPlaced", { orderId: "A1", total: 99 });
emitter.emit("userLoggedIn", { userId: 7 });
off();
emitter.emit("orderPlaced", { orderId: "A2", total: 10 });
check("type-safe emitter", log, ["order A1 for 99", "user 7"]);

const api = new ApiClient({
  "GET /users": (req) => [{ id: req.page, name: `page${req.page}` }],
  "POST /orders": (req) => ({ orderId: `${req.sku}-${req.qty}` }),
});
void (async () => {
  const users = await api.call("GET /users", { page: 2 });
  const order = await api.call("POST /orders", { sku: "BOOK", qty: 3 });
  check("typed api client", [users[0]?.name, order.orderId], ["page2", "BOOK-3"]);

  check("dto -> domain mapping",
        (({ id, name, active, createdAt }) => [id, name, active, createdAt.getFullYear()])(
          toDomainUser({ user_id: 5, full_name: "ann lee", created_at: "2026-02-03", is_active: 1 })),
        [5, "ann lee", true, 2026]);

  check("options with defaults", [withDefaults().attempts, withDefaults({ attempts: 5 }).baseDelayMs], [3, 100]);
  check("typed keys / entries", [typedKeys(settings), typedEntries(settings).map(([k, v]) => `${String(k)}=${String(v)}`)],
        [["theme", "fontSize"], ["theme=dark", "fontSize=14"]]);

  console.log("\nall checks passed");
})();

export {};
