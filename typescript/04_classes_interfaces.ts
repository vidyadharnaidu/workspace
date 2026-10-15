/**
 * Classes, interfaces, abstract classes, structural typing, generic classes.
 *
 * Run:   node typescript/04_classes_interfaces.ts
 *
 * REMEMBER:
 *  * TypeScript is STRUCTURALLY typed: a class-shaped object satisfies an interface without `implements`.
 *    `implements` is only a compile-time check that you didn't forget a member.
 *  * public / protected / private are COMPILE-TIME only. `#field` is real runtime privacy.
 *  * abstract class = shared implementation + required members; interface = shape only, no runtime code.
 *  * Prefer composition (inject dependencies) over deep inheritance.
 *  * `this` typing: use `this` as a return type for fluent builders.
 */

function check(label: string, actual: unknown, expected: unknown): void {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
  console.log(`${label.padEnd(34)} ${a}`);
}

// ---------------------------------------------------------------- interfaces & structural typing

interface Logger {
  log(message: string): void;
}

interface Repository<T, ID> {
  save(entity: T): T;
  findById(id: ID): T | undefined;
  findAll(): readonly T[];
  delete(id: ID): boolean;
}

interface Employee {
  id: number;
  name: string;
  dept: string;
  salary: number;
}

// A plain object satisfies Logger: no `implements` needed (structural typing)
const consoleLogger: Logger = { log: (message) => lines.push(message) };
const lines: string[] = [];

// ---------------------------------------------------------------- generic class implementing an interface

class InMemoryRepository<T extends { id: ID }, ID> implements Repository<T, ID> {
  // NOTE: parameter properties (constructor(private x)) are not used here so the file
  // runs directly with Node's type stripping, which only allows erasable syntax.
  private readonly items = new Map<ID, T>();
  private readonly logger: Logger;

  constructor(logger: Logger) {
    this.logger = logger;
  }

  save(entity: T): T {
    this.items.set(entity.id, entity);
    this.logger.log(`saved ${String(entity.id)}`);
    return entity;
  }

  findById(id: ID): T | undefined {
    return this.items.get(id);
  }

  findAll(): readonly T[] {
    return [...this.items.values()];
  }

  delete(id: ID): boolean {
    return this.items.delete(id);
  }
}

// ---------------------------------------------------------------- abstract class + polymorphism

abstract class Shape {
  abstract readonly kind: string;
  abstract area(): number;

  describe(): string {                       // shared implementation
    return `${this.kind} with area ${this.area().toFixed(2)}`;
  }
}

class Circle extends Shape {
  override readonly kind = "circle";
  readonly radius: number;

  constructor(radius: number) {
    super();
    this.radius = radius;
  }

  override area(): number {
    return Math.PI * this.radius ** 2;
  }
}

class Square extends Shape {
  override readonly kind = "square";
  readonly side: number;

  constructor(side: number) {
    super();
    this.side = side;
  }

  override area(): number {
    return this.side ** 2;
  }
}

// ---------------------------------------------------------------- access modifiers, getters, statics

class BankAccount {
  static #count = 0;
  #balance: number;                         // truly private at runtime
  protected readonly currency: string;       // visible to subclasses only
  readonly owner: string;

  constructor(owner: string, balance = 0, currency = "EUR") {
    this.owner = owner;
    this.#balance = balance;
    this.currency = currency;
    BankAccount.#count++;
  }

  static get accountsCreated(): number {
    return BankAccount.#count;
  }

  static fromString(text: string): BankAccount {     // alternative constructor
    const [owner = "", amount = "0"] = text.split(":");
    return new BankAccount(owner, Number(amount));
  }

  get balance(): number {
    return this.#balance;
  }

  deposit(amount: number): this {                    // `this` return type -> chainable
    if (amount <= 0) throw new RangeError("amount must be positive");
    this.#balance += amount;
    return this;
  }

  withdraw(amount: number): boolean {
    if (amount > this.#balance) return false;
    this.#balance -= amount;
    return true;
  }

  toString(): string {
    return `${this.owner}: ${this.#balance} ${this.currency}`;
  }
}

class SavingsAccount extends BankAccount {
  readonly rate: number;

  constructor(owner: string, balance: number, rate: number) {
    super(owner, balance);
    this.rate = rate;
  }

  addInterest(): this {
    return this.deposit(this.balance * this.rate);
  }

  override toString(): string {
    return `${super.toString()} (savings at ${this.currency})`;   // protected member is visible here
  }
}

// ---------------------------------------------------------------- fluent builder with `this` types

class QueryBuilder {
  #table = "";
  #wheres: string[] = [];
  #limit?: number;

  from(table: string): this {
    this.#table = table;
    return this;
  }

  where(condition: string): this {
    this.#wheres.push(condition);
    return this;
  }

  limit(count: number): this {
    this.#limit = count;
    return this;
  }

  build(): string {
    const where = this.#wheres.length ? ` WHERE ${this.#wheres.join(" AND ")}` : "";
    const limit = this.#limit ? ` LIMIT ${this.#limit}` : "";
    return `SELECT * FROM ${this.#table}${where}${limit}`;
  }
}

// ---------------------------------------------------------------- composition over inheritance

interface Notifier {
  send(to: string, message: string): string;
}

const emailNotifier: Notifier = { send: (to, message) => `email to ${to}: ${message}` };
const smsNotifier: Notifier = { send: (to, message) => `sms to ${to}: ${message}` };

class OrderService {
  readonly #notifier: Notifier;              // injected dependency: easy to swap and to test

  constructor(notifier: Notifier) {
    this.#notifier = notifier;
  }

  placeOrder(customer: string, item: string): string {
    return this.#notifier.send(customer, `order confirmed: ${item}`);
  }
}

const repo = new InMemoryRepository<Employee, number>(consoleLogger);
repo.save({ id: 1, name: "ann", dept: "it", salary: 120 });
repo.save({ id: 2, name: "bob", dept: "hr", salary: 95 });

check("generic repository", [repo.findById(1)?.name, repo.findAll().length, repo.delete(2), repo.findAll().length],
      ["ann", 2, true, 1]);
check("structural logger got calls", lines, ["saved 1", "saved 2"]);

const shapes: Shape[] = [new Circle(1), new Square(2)];
check("abstract class polymorphism", shapes.map((s) => s.describe()),
      ["circle with area 3.14", "square with area 4.00"]);
check("instanceof narrowing", shapes.filter((s) => s instanceof Circle).length, 1);

const account = BankAccount.fromString("alice:100");
account.deposit(50).deposit(25);
check("class with #private + chaining", [account.owner, account.balance, account.withdraw(1000)], ["alice", 175, false]);
check("static members", BankAccount.accountsCreated >= 1, true);

const savings = new SavingsAccount("bob", 1000, 0.05);
savings.addInterest();
check("inheritance + protected + super", [savings.balance, savings.toString()], [1050, "bob: 1050 EUR (savings at EUR)"]);

check("fluent builder with `this`",
      new QueryBuilder().from("orders").where("status = 'paid'").where("total > 100").limit(10).build(),
      "SELECT * FROM orders WHERE status = 'paid' AND total > 100 LIMIT 10");

check("composition: swap the dependency",
      [new OrderService(emailNotifier).placeOrder("ann", "book"), new OrderService(smsNotifier).placeOrder("bob", "pen")],
      ["email to ann: order confirmed: book", "sms to bob: order confirmed: pen"]);

console.log("\nall checks passed");

export {};
