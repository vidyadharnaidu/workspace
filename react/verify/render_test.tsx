/**
 * Renders every component in this folder with react-dom/server and checks the HTML.
 * No browser, no jsdom: a fast smoke test that the examples actually work.
 *
 *   cd react && npm install && npm run verify
 */
import type { ReactElement } from "react";
import { renderToStaticMarkup } from "react-dom/server";

import { Badge, Card, CountBadge, StatusView, TodoApp, TodoList } from "../01_fundamentals/components_and_props";
import { Cart, Counter, ProductSearch, ThemeProvider } from "../01_fundamentals/hooks_basics";
import { ErrorBoundary, OrderTable, PatternsDemo, Rating, RenderPropExample, Tabs } from "../02_patterns/component_patterns";
import { Panel, RowList, VirtualList } from "../02_patterns/performance";
import { MessageList, SignupForm, UncontrolledForm } from "../03_forms_data/forms_and_data";

let failures = 0;

function expectHtml(label: string, element: ReactElement, expected: string | RegExp): void {
  const html = renderToStaticMarkup(element);
  const ok = typeof expected === "string" ? html.includes(expected) : expected.test(html);
  if (!ok) failures++;
  console.log(`${ok ? "PASS" : "FAIL"}  ${label.padEnd(38)} ${html.slice(0, 80)}`);
}

expectHtml("Badge", <Badge label="new" tone="warning" />, "badge--warning");
expectHtml("Card with footer", <Card title="T" footer={<span>f</span>}>body</Card>, "<footer>");
expectHtml("TodoList empty state", <TodoList todos={[]} onToggle={() => {}} />, "Nothing to do");
expectHtml("TodoList item done", <TodoList todos={[{ id: "1", title: "ship", done: true }]} onToggle={() => {}} />, "line-through");
expectHtml("TodoApp derived count", <TodoApp initialTodos={[{ id: "1", title: "write", done: false }]} />, "1 remaining");
expectHtml("StatusView loading", <StatusView status="loading">x</StatusView>, 'role="status"');
expectHtml("StatusView error", <StatusView status="error" error="boom">x</StatusView>, "boom");
expectHtml("CountBadge(0) renders nothing", <CountBadge count={0} />, /^$/);
expectHtml("Counter initial state", <Counter />, "<output>0</output>");
expectHtml("Cart reducer total", <Cart />, "0.00");
expectHtml("ThemeProvider children", <ThemeProvider><span>themed</span></ThemeProvider>, "themed");
expectHtml("ProductSearch list", <ProductSearch products={[{ id: 1, name: "Pen", price: 2 }]} onPick={() => {}} />, "Pen");
expectHtml(
  "Tabs compound component",
  <Tabs defaultId="a">
    <Tabs.List>
      <Tabs.Tab id="a">A</Tabs.Tab>
    </Tabs.List>
    <Tabs.Panel id="a">panel</Tabs.Panel>
  </Tabs>,
  'role="tabpanel"',
);
expectHtml("OrderTable", <OrderTable orders={[{ id: "A1", total: 25 }]} />, "25.00");
expectHtml("Rating (uncontrolled)", <Rating defaultValue={2} />, 'role="radiogroup"');
expectHtml("Render prop", <RenderPropExample />, "OFF");
expectHtml("ErrorBoundary passes children", <ErrorBoundary fallback={() => <p>err</p>}><p>fine</p></ErrorBoundary>, "fine");
expectHtml("PatternsDemo", <PatternsDemo />, "Orders");
expectHtml("RowList (memo children)", <RowList rows={[{ id: 1, label: "row", price: 5 }]} onPick={() => {}} />, "row - 5");
expectHtml(
  "VirtualList renders a window",
  <VirtualList rows={Array.from({ length: 1000 }, (_, i) => ({ id: i, label: `r${i}`, price: i }))} />,
  "r0",
);
expectHtml("Panel children stay stable", <Panel title="p">inner</Panel>, "inner");
expectHtml("SignupForm", <SignupForm onSubmit={async () => {}} />, "Create account");
expectHtml("UncontrolledForm", <UncontrolledForm onSubmit={() => {}} />, 'name="title"');
expectHtml("MessageList (optimistic)", <MessageList messages={[{ id: "1", text: "hi" }]} send={async () => {}} />, "hi");

if (failures > 0) throw new Error(`${failures} component(s) failed to render as expected`);
console.log("\nall components rendered");
