import { renderToStaticMarkup } from "react-dom/server";
import {
  Badge,
  Card,
  TodoList,
  TodoApp,
  StatusView,
  CountBadge,
  TextField,
  Button,
} from "../01_fundamentals/components_and_props";

console.log("================================================================================");
console.log("             RUNNING: 01_fundamentals/components_and_props.tsx                ");
console.log("================================================================================\n");

console.log("1. Badge Component (Typing props, literal union, optional callback):");
console.log("   - info:   ", renderToStaticMarkup(<Badge label="new" tone="info" />));
console.log("   - warning:", renderToStaticMarkup(<Badge label="alert" tone="warning" onDismiss={() => {}} />));
console.log("   - danger: ", renderToStaticMarkup(<Badge label="error" tone="danger" />));

console.log("\n2. Card Component (Composition / 'Slots' with children & footer):");
console.log(
  "  ",
  renderToStaticMarkup(
    <Card title="Project Summary" footer={<small>Updated 2m ago</small>}>
      <p>Card body passed via children prop.</p>
    </Card>
  )
);

console.log("\n3. TodoList Component (Lists, Keys, and Empty State):");
console.log("   - Empty state: ", renderToStaticMarkup(<TodoList todos={[]} onToggle={() => {}} />));
console.log(
  "   - With items:  ",
  renderToStaticMarkup(
    <TodoList
      todos={[
        { id: "task-1", title: "Review React 19 fundamentals", done: true },
        { id: "task-2", title: "Demonstrate props and components", done: false },
      ]}
      onToggle={() => {}}
    />
  )
);

console.log("\n4. TodoApp Component (State, Controlled Input, Immutable Updates, Derived State):");
console.log(
  "  ",
  renderToStaticMarkup(
    <TodoApp
      initialTodos={[
        { id: "1", title: "Initial Todo", done: false },
        { id: "2", title: "Completed Todo", done: true },
      ]}
    />
  )
);

console.log("\n5. StatusView Component (Conditional Rendering / Early Returns):");
console.log("   - status='idle':   ", JSON.stringify(renderToStaticMarkup(<StatusView status="idle">Loaded Content</StatusView>)));
console.log("   - status='loading':", renderToStaticMarkup(<StatusView status="loading">Loaded Content</StatusView>));
console.log("   - status='error':  ", renderToStaticMarkup(<StatusView status="error" error="Network error 500">Loaded Content</StatusView>));
console.log("   - status='ready':  ", renderToStaticMarkup(<StatusView status="ready">Loaded Content</StatusView>));

console.log("\n6. CountBadge Component (Falsy 0 trap prevention: count && <Badge/> renders '0'):");
console.log("   - count=0: ", JSON.stringify(renderToStaticMarkup(<CountBadge count={0} />)), " (cleanly renders nothing)");
console.log("   - count=3: ", renderToStaticMarkup(<CountBadge count={3} />));

console.log("\n7. TextField Component (React 19 ref as normal prop & uncontrolled input):");
console.log("  ", renderToStaticMarkup(<TextField label="Username" defaultValue="developer" />));

console.log("\n8. Button Component (Props spreading & ComponentPropsWithoutRef):");
console.log("   - primary: ", renderToStaticMarkup(<Button variant="primary" type="submit">Submit</Button>));
console.log("   - ghost:   ", renderToStaticMarkup(<Button variant="ghost" disabled>Cancel</Button>));

console.log("\n================================================================================");
console.log("All components in 01_fundamentals/components_and_props.tsx rendered successfully!");
console.log("================================================================================");
