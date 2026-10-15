# Testing React (React Testing Library)

> **The principle:** *"The more your tests resemble the way your software is used, the more confidence they give you."* Test what the user sees and does, not internal state.

## Setup (the usual stack)

```bash
npm i -D vitest @testing-library/react @testing-library/user-event @testing-library/jest-dom jsdom msw
```

```ts
// vitest.config.ts
export default { test: { environment: "jsdom", setupFiles: ["./test/setup.ts"], globals: true } };
// test/setup.ts
import "@testing-library/jest-dom";
```

## The shape of a component test

```tsx
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { TodoApp } from "../01_fundamentals/components_and_props";

test("adds a todo and updates the remaining count", async () => {
  const user = userEvent.setup();
  render(<TodoApp />);

  await user.type(screen.getByPlaceholderText("What needs doing?"), "write tests");
  await user.click(screen.getByRole("button", { name: "Add" }));

  expect(screen.getByText("write tests")).toBeInTheDocument();
  expect(screen.getByText("1 remaining")).toBeInTheDocument();
});
```

Pattern: **arrange** (render with props) → **act** (`user-event`) → **assert** (what is on screen).

## Query priority (use the highest one that works)

| Priority | Query | Why |
|---|---|---|
| 1 | `getByRole("button", { name: "Save" })` | How assistive tech sees it; catches accessibility problems |
| 2 | `getByLabelText("Email")` | The right way to find form fields |
| 3 | `getByPlaceholderText`, `getByText`, `getByDisplayValue` | Fine when there's no label |
| 4 | `getByTestId("row-7")` | Escape hatch only |

| Variant | Returns | Use for |
|---|---|---|
| `getBy*` | element, throws if missing | it should be there now |
| `queryBy*` | element or `null` | asserting something is **absent** |
| `findBy*` | promise | it appears **after** an async update |
| `*AllBy*` | array | lists |

## Async, timers and the `act` warning

```tsx
// wait for an element that appears after a fetch
expect(await screen.findByRole("status")).toHaveTextContent("Saved");

// wait for something to disappear
await waitForElementToBeRemoved(() => screen.queryByText("Loading..."));

// fake timers with user-event
const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
vi.useFakeTimers();
```

An `act(...)` warning means state changed outside React's knowledge — usually a promise resolving after the test ended. Fix it by awaiting (`findBy*`, `waitFor`), not by wrapping everything in `act`.

## Mocking the network (MSW is preferred over stubbing fetch)

```ts
import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";

const server = setupServer(
  http.get("/api/users/:id", ({ params }) => HttpResponse.json({ id: Number(params.id), name: "Ann" })),
);
beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

test("shows an error when the request fails", async () => {
  server.use(http.get("/api/users/:id", () => new HttpResponse(null, { status: 500 })));
  render(<UserProfile userId={1} />);
  expect(await screen.findByRole("alert")).toHaveTextContent("HTTP 500");
});
```

## Testing a custom hook

```tsx
import { renderHook, act } from "@testing-library/react";
import { useToggle } from "../01_fundamentals/custom_hooks";

test("useToggle flips the value", () => {
  const { result } = renderHook(() => useToggle(false));
  expect(result.current[0]).toBe(false);
  act(() => result.current[1]());
  expect(result.current[0]).toBe(true);
});
```

For hooks needing context, pass a `wrapper`: `renderHook(() => useTheme(), { wrapper: ThemeProvider })`.

## What to test (and what not to)

✅ Rendering per state: empty, loading, error, data, permission variants.
✅ User flows: fill the form, submit, see the result.
✅ Conditional rendering and disabled states.
✅ Accessibility basics: roles, labels, `aria-invalid`, focus.
✅ Pure logic (reducers, validators, selectors) as plain unit tests — cheapest and fastest; see `04_logic/pure_logic.js`.

❌ Implementation details: internal state variables, "was useEffect called", child component internals.
❌ Snapshot tests of big trees: they break on every change and nobody reads the diff.
❌ Re-testing the library (that `useState` works).

## The testing pyramid answer

Many fast **unit tests** of pure logic → a solid layer of **component tests** with RTL → a few **end-to-end tests** (Playwright/Cypress) for critical flows such as login and checkout. Prefer more component tests than most teams have, and only the end-to-end tests that would wake you at night if they broke.

## Debug helpers

```ts
screen.debug();                     // print the DOM
screen.logTestingPlaygroundURL();   // get a suggested query
```
