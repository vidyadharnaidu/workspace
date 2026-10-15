"""
OOP in Python: dunder methods, dataclasses, properties, class/static methods, inheritance & MRO, ABCs, enums.

Run: python python/01_python_basics/oop_dataclasses.py

REMEMBER:
  * __repr__ is for developers (unambiguous), __str__ for users. Implement __repr__ at least.
  * If you define __eq__, define __hash__ too (or the object becomes unhashable).
  * @dataclass writes __init__/__repr__/__eq__ for you. Mutable defaults need field(default_factory=list).
  * @property = computed attribute / validation.  @classmethod = alternative constructors.  @staticmethod = plain helper.
  * super() follows the MRO (method resolution order), not just "the parent". See ClassName.__mro__.
  * Class attributes are SHARED by all instances; a mutable class attribute is a classic bug.
"""
import math
from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from enum import Enum, auto
from functools import total_ordering


def check(label, actual, expected):
    assert actual == expected, f"{label}: expected {expected!r}, got {actual!r}"
    print(f"{label:<34} {actual!r}")


# Q1. Dunder (magic) methods
class Vector:
    __slots__ = ("x", "y")          # no per-instance __dict__ -> less memory, no accidental new attributes

    def __init__(self, x, y):
        self.x, self.y = x, y

    def __repr__(self):
        return f"Vector({self.x}, {self.y})"

    def __eq__(self, other):
        return isinstance(other, Vector) and (self.x, self.y) == (other.x, other.y)

    def __hash__(self):
        return hash((self.x, self.y))

    def __add__(self, other):
        return Vector(self.x + other.x, self.y + other.y)

    def __mul__(self, scalar):
        return Vector(self.x * scalar, self.y * scalar)

    __rmul__ = __mul__                  # 3 * v

    def __abs__(self):
        return math.hypot(self.x, self.y)

    def __bool__(self):
        return bool(self.x or self.y)

    def __iter__(self):                 # enables x, y = vector
        yield self.x
        yield self.y


# Q2. Dataclasses
@dataclass
class Employee:
    name: str
    dept: str
    salary: float = 0.0
    skills: list[str] = field(default_factory=list)    # NOT skills: list = []


@dataclass(frozen=True, order=True)
class Point:                                            # immutable, hashable, sortable
    x: int
    y: int


# Q3. property, classmethod, staticmethod
class Account:
    interest_rate = 0.02                                # class attribute (shared)

    def __init__(self, owner, balance=0):
        self.owner = owner
        self.balance = balance                          # goes through the property setter

    @property
    def balance(self):
        return self._balance

    @balance.setter
    def balance(self, value):
        if value < 0:
            raise ValueError("balance cannot be negative")
        self._balance = value

    @classmethod
    def from_string(cls, text):                         # alternative constructor: "alice:100"
        owner, amount = text.split(":")
        return cls(owner, int(amount))

    @staticmethod
    def is_valid_owner(name):
        return name.isalpha()


# Q4. Inheritance, super() and MRO (diamond)
class A:
    def hello(self):
        return "A"


class B(A):
    def hello(self):
        return "B" + super().hello()


class C(A):
    def hello(self):
        return "C" + super().hello()


class D(B, C):
    def hello(self):
        return "D" + super().hello()


# Q5. Abstract base classes and polymorphism
class Shape(ABC):
    @abstractmethod
    def area(self) -> float: ...

    def describe(self):
        return f"{type(self).__name__} with area {self.area():.2f}"


class Circle(Shape):
    def __init__(self, r):
        self.r = r

    def area(self):
        return math.pi * self.r ** 2


class Square(Shape):
    def __init__(self, side):
        self.side = side

    def area(self):
        return self.side ** 2


# Q6. Enum
class Status(Enum):
    PENDING = auto()
    SHIPPED = auto()
    DELIVERED = auto()


# Q7. Singleton (simplest real answer: a module-level instance; here via __new__)
class Config:
    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance.settings = {}
        return cls._instance


# Q8. Shared mutable class attribute bug
class DogBad:
    tricks = []                      # shared by ALL dogs

    def add_trick(self, trick):
        self.tricks.append(trick)


class DogGood:
    def __init__(self):
        self.tricks = []             # per instance


# Q9. total_ordering: define __eq__ and __lt__, get the rest
@total_ordering
class Version:
    def __init__(self, text):
        self.parts = tuple(int(p) for p in text.split("."))

    def __eq__(self, other):
        return self.parts == other.parts

    def __lt__(self, other):
        return self.parts < other.parts

    def __repr__(self):
        return ".".join(map(str, self.parts))


# Q10. Make a class behave like a sequence
class Playlist:
    def __init__(self, songs):
        self._songs = list(songs)

    def __len__(self):
        return len(self._songs)

    def __getitem__(self, index):            # supports indexing, slicing, iteration, `in`
        return self._songs[index]

    def __contains__(self, song):
        return song in self._songs


if __name__ == "__main__":
    v = Vector(3, 4)
    check("Q1 __add__ / __mul__ / __rmul__", (v + Vector(1, 1), v * 2, 2 * v), (Vector(4, 5), Vector(6, 8), Vector(6, 8)))
    check("Q1 __abs__ / __bool__", (abs(v), bool(Vector(0, 0))), (5.0, False))
    x, y = v
    check("Q1 __iter__ unpacking", (x, y), (3, 4))
    check("Q1 hashable -> set dedupe", len({Vector(1, 2), Vector(1, 2)}), 1)
    try:
        v.z = 1
    except AttributeError:
        check("Q1 __slots__ blocks new attrs", "AttributeError", "AttributeError")

    e1, e2 = Employee("Ann", "IT", 100), Employee("Ann", "IT", 100)
    e1.skills.append("python")
    check("Q2 dataclass __eq__ uses fields", Employee("Ann", "IT", 100) == Employee("Ann", "IT", 100), True)
    check("Q2 default_factory not shared", (e1.skills, e2.skills), (["python"], []))
    check("Q2 dataclass __repr__", repr(e2), "Employee(name='Ann', dept='IT', salary=100, skills=[])")
    check("Q2 frozen+order sortable", sorted([Point(2, 1), Point(1, 5), Point(1, 2)]), [Point(1, 2), Point(1, 5), Point(2, 1)])
    check("Q2 frozen is hashable", Point(1, 2) in {Point(1, 2)}, True)

    acc = Account.from_string("alice:100")
    check("Q3 classmethod constructor", (acc.owner, acc.balance), ("alice", 100))
    check("Q3 staticmethod", Account.is_valid_owner("bob1"), False)
    try:
        acc.balance = -5
    except ValueError as err:
        check("Q3 property setter validates", str(err), "balance cannot be negative")

    check("Q4 super() follows MRO", D().hello(), "DBCA")
    check("Q4 D.__mro__", [k.__name__ for k in D.__mro__], ["D", "B", "C", "A", "object"])

    shapes = [Circle(1), Square(2)]
    check("Q5 polymorphism", [s.describe() for s in shapes], ["Circle with area 3.14", "Square with area 4.00"])
    try:
        Shape()
    except TypeError:
        check("Q5 can't instantiate ABC", "TypeError", "TypeError")

    check("Q6 Enum by name / value", (Status["SHIPPED"], Status(3).name), (Status.SHIPPED, "DELIVERED"))
    check("Q7 singleton same object", Config() is Config(), True)

    d1, d2 = DogBad(), DogBad()
    d1.add_trick("roll")
    check("Q8 class attr shared (bug)", d2.tricks, ["roll"])
    g1, g2 = DogGood(), DogGood()
    g1.tricks.append("roll")
    check("Q8 instance attr (fix)", g2.tricks, [])

    check("Q9 total_ordering", (Version("1.10.0") > Version("1.9.9"), max(Version("2.0"), Version("10.1"))),
          (True, Version("10.1")))

    p = Playlist(["a", "b", "c", "d"])
    check("Q10 len / slice / in / iterate", (len(p), p[1:3], "c" in p, list(p)[-1]), (4, ["b", "c"], True, "d"))

    print("\nall checks passed")
