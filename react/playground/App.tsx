import { useRef, useState, type ReactNode } from "react";

import {
  Badge,
  Button,
  Card,
  CountBadge,
  StatusView,
  TextField,
  TodoApp,
} from "../01_fundamentals/components_and_props";
import { SearchBox } from "../01_fundamentals/custom_hooks";
import {
  Cart,
  Counter,
  DocumentTitle,
  MeasuredBox,
  ProductSearch,
  StopwatchAndFocus,
  ThemeProvider,
  UserProfile,
  useTheme,
} from "../01_fundamentals/hooks_basics";
import { PatternsDemo } from "../02_patterns/component_patterns";
import { Dashboard, Panel, ProductTable, VirtualList } from "../02_patterns/performance";
import { CommentForm, MessageList, SignupForm, UncontrolledForm } from "../03_forms_data/forms_and_data";

interface Example {
  id: string;
  label: string;
  file: string;
  render: () => ReactNode;
}

const PRODUCTS = [
  { id: 1, name: "Pen", price: 2 },
  { id: 2, name: "Notebook", price: 8 },
  { id: 3, name: "Laptop", price: 900 },
];

const ROWS = Array.from({ length: 40 }, (_, i) => ({ id: i, label: `row ${i}`, price: i * 3 }));
const VIRTUAL_ROWS = Array.from({ length: 1000 }, (_, i) => ({ id: i, label: `r${i}`, price: i }));

function UserProfileDemo() {
  const [userId, setUserId] = useState(1);
  return (
    <div>
      <DocumentTitle title={`User ${userId}`} />
      <div className="row">
        <button type="button" onClick={() => setUserId(1)}>
          Ada (1)
        </button>
        <button type="button" onClick={() => setUserId(2)}>
          Grace (2)
        </button>
        <button type="button" onClick={() => setUserId(99)}>
          Missing (99)
        </button>
      </div>
      <UserProfile userId={userId} />
    </div>
  );
}

function ThemeDemo() {
  return (
    <ThemeProvider>
      <ThemedPanel />
    </ThemeProvider>
  );
}

function ThemedPanel() {
  const { mode, toggle } = useTheme();
  return (
    <div data-theme={mode}>
      <p>mode: {mode}</p>
      <button type="button" onClick={toggle}>
        Toggle theme
      </button>
    </div>
  );
}

function StatusViewDemo() {
  const [status, setStatus] = useState<"idle" | "loading" | "error" | "ready">("ready");
  return (
    <div>
      <div className="row">
        {(["idle", "loading", "error", "ready"] as const).map((value) => (
          <button key={value} type="button" onClick={() => setStatus(value)}>
            {value}
          </button>
        ))}
      </div>
      <StatusView status={status} error="boom">
        Ready content
      </StatusView>
    </div>
  );
}

function TextFieldDemo() {
  const ref = useRef<HTMLInputElement>(null);
  return (
    <div className="row">
      <TextField label="Name" ref={ref} defaultValue="Reddi" />
      <Button type="button" onClick={() => ref.current?.focus()}>
        Focus
      </Button>
      <Button type="button" variant="ghost">
        Ghost
      </Button>
    </div>
  );
}

function SearchBoxDemo() {
  const [last, setLast] = useState("(type, then wait 300ms)");
  return (
    <div>
      <SearchBox onSearch={setLast} />
      <p>last search: {last}</p>
    </div>
  );
}

function DashboardDemo() {
  const [showChart, setShowChart] = useState(false);
  return (
    <div>
      <button type="button" onClick={() => setShowChart((value) => !value)}>
        {showChart ? "Hide chart" : "Show chart"}
      </button>
      <Dashboard showChart={showChart} />
    </div>
  );
}

function MessageListDemo() {
  const [messages, setMessages] = useState([{ id: "1", text: "hi" }]);
  return (
    <MessageList
      messages={messages}
      send={async (text) => {
        await new Promise((resolve) => setTimeout(resolve, 400));
        setMessages((current) => [...current, { id: crypto.randomUUID(), text }]);
      }}
    />
  );
}

const GROUPS: { folder: string; examples: Example[] }[] = [
  {
    folder: "01_fundamentals",
    examples: [
      { id: "counter", label: "Counter (useState)", file: "hooks_basics.tsx", render: () => <Counter /> },
      { id: "profile", label: "UserProfile (useEffect)", file: "hooks_basics.tsx", render: () => <UserProfileDemo /> },
      {
        id: "measure",
        label: "MeasuredBox (useLayoutEffect)",
        file: "hooks_basics.tsx",
        render: () => <MeasuredBox>measure me</MeasuredBox>,
      },
      { id: "stopwatch", label: "StopwatchAndFocus (useRef)", file: "hooks_basics.tsx", render: () => <StopwatchAndFocus /> },
      { id: "cart", label: "Cart (useReducer)", file: "hooks_basics.tsx", render: () => <Cart /> },
      { id: "theme", label: "Theme (useContext)", file: "hooks_basics.tsx", render: () => <ThemeDemo /> },
      {
        id: "search",
        label: "ProductSearch (useMemo)",
        file: "hooks_basics.tsx",
        render: () => <ProductSearch products={PRODUCTS} onPick={(p) => console.log(p)} />,
      },
      { id: "searchbox", label: "SearchBox (custom hooks)", file: "custom_hooks.tsx", render: () => <SearchBoxDemo /> },
      { id: "todos", label: "TodoApp", file: "components_and_props.tsx", render: () => <TodoApp /> },
      {
        id: "badge",
        label: "Badge / Card / CountBadge",
        file: "components_and_props.tsx",
        render: () => (
          <div>
            <div className="row">
              <Badge label="new" tone="warning" onDismiss={() => undefined} />
              <CountBadge count={3} />
              <CountBadge count={0} />
            </div>
            <Card title="Card" footer={<span>footer</span>}>
              body
            </Card>
          </div>
        ),
      },
      { id: "status", label: "StatusView", file: "components_and_props.tsx", render: () => <StatusViewDemo /> },
      { id: "textfield", label: "TextField + Button", file: "components_and_props.tsx", render: () => <TextFieldDemo /> },
    ],
  },
  {
    folder: "02_patterns",
    examples: [
      { id: "patterns", label: "PatternsDemo", file: "component_patterns.tsx", render: () => <PatternsDemo /> },
      {
        id: "table",
        label: "ProductTable",
        file: "performance.tsx",
        render: () => <ProductTable rows={ROWS} onPick={(id) => console.log(id)} />,
      },
      {
        id: "panel",
        label: "Panel",
        file: "performance.tsx",
        render: () => <Panel title="Panel">inner — collapse me</Panel>,
      },
      { id: "dashboard", label: "Dashboard (lazy)", file: "performance.tsx", render: () => <DashboardDemo /> },
      {
        id: "virtual",
        label: "VirtualList",
        file: "performance.tsx",
        render: () => <VirtualList rows={VIRTUAL_ROWS} />,
      },
    ],
  },
  {
    folder: "03_forms_data",
    examples: [
      {
        id: "signup",
        label: "SignupForm",
        file: "forms_and_data.tsx",
        render: () => <SignupForm onSubmit={async () => undefined} />,
      },
      {
        id: "uncontrolled",
        label: "UncontrolledForm",
        file: "forms_and_data.tsx",
        render: () => <UncontrolledForm onSubmit={(data) => console.log(data)} />,
      },
      {
        id: "comment",
        label: "CommentForm (actions)",
        file: "forms_and_data.tsx",
        render: () => <CommentForm save={async (text) => text} />,
      },
      { id: "messages", label: "MessageList (optimistic)", file: "forms_and_data.tsx", render: () => <MessageListDemo /> },
    ],
  },
];

const ALL = GROUPS.flatMap((group) => group.examples);

function exampleFromHash(): string {
  const id = window.location.hash.replace(/^#/, "");
  return ALL.some((example) => example.id === id) ? id : "counter";
}

export function App() {
  const [selectedId, setSelectedId] = useState(exampleFromHash);
  const selected = ALL.find((example) => example.id === selectedId) ?? ALL[0];

  const select = (id: string) => {
    setSelectedId(id);
    window.location.hash = id;
  };

  return (
    <div className="app">
      <nav className="nav">
        <h1>React interview playground</h1>
        <p>Click an example. Edit the source files and Vite will hot-reload.</p>
        {GROUPS.map((group) => (
          <section key={group.folder}>
            <h2>{group.folder}</h2>
            {group.examples.map((example) => (
              <button
                key={example.id}
                type="button"
                aria-current={example.id === selected.id ? "page" : undefined}
                onClick={() => select(example.id)}
              >
                {example.label}
              </button>
            ))}
          </section>
        ))}
      </nav>
      <main className="stage">
        <header>
          <h2>{selected.label}</h2>
          <div className="file">{selected.file}</div>
        </header>
        <div className="demo">{selected.render()}</div>
      </main>
    </div>
  );
}
