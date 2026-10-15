"""
Graph problems.

Run: python python/08_graphs/graphs.py

REMEMBER:
  * Build with defaultdict(list). BFS = deque + visited (mark when ADDING). DFS = recursion/stack.
  * BFS gives the shortest path in an UNWEIGHTED graph; Dijkstra (heapq) when edges have weights.
  * Grid = graph: cells are nodes, 4 directions are edges; "sink" visited cells to avoid revisits.
  * Topological sort (Kahn): start from in-degree 0; leftover nodes mean a CYCLE.
  * Union-Find: find with path compression; union returns False -> the edge closes a cycle.
"""
import heapq
from collections import defaultdict, deque


def check(label, actual, expected):
    assert actual == expected, f"{label}: expected {expected!r}, got {actual!r}"
    print(f"{label:<28} {actual!r}")


def build_graph(edges, directed=False):
    graph = defaultdict(list)
    for a, b in edges:
        graph[a].append(b)
        graph[b] if directed else graph[b].append(a)
    return graph


def bfs(graph, start):
    visited, order, queue = {start}, [], deque([start])
    while queue:
        node = queue.popleft()
        order.append(node)
        for nxt in graph[node]:
            if nxt not in visited:
                visited.add(nxt)          # mark when ADDING, not when popping
                queue.append(nxt)
    return order


def dfs(graph, start, visited=None):
    visited = visited if visited is not None else set()
    if start in visited:
        return []
    visited.add(start)
    order = [start]
    for nxt in graph[start]:
        order += dfs(graph, nxt, visited)
    return order


def dfs_iterative(graph, start):
    visited, order, stack = set(), [], [start]
    while stack:
        node = stack.pop()
        if node in visited:
            continue
        visited.add(node)
        order.append(node)
        stack.extend(reversed(graph[node]))     # reversed keeps the recursive order
    return order


def shortest_path(graph, src, dst):
    parents = {src: None}
    queue = deque([src])
    while queue:
        node = queue.popleft()
        if node == dst:
            break
        for nxt in graph[node]:
            if nxt not in parents:
                parents[nxt] = node
                queue.append(nxt)
    if dst not in parents:
        return []
    path, node = [], dst
    while node is not None:
        path.append(node)
        node = parents[node]
    return path[::-1]


def num_islands(grid):
    grid = [list(row) for row in grid]
    rows, cols = len(grid), len(grid[0])

    def sink(r, c):
        if 0 <= r < rows and 0 <= c < cols and grid[r][c] == "1":
            grid[r][c] = "0"
            for dr, dc in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                sink(r + dr, c + dc)

    count = 0
    for r in range(rows):
        for c in range(cols):
            if grid[r][c] == "1":
                count += 1
                sink(r, c)
    return count


def oranges_rotting(grid):
    grid = [row[:] for row in grid]
    rows, cols = len(grid), len(grid[0])
    queue = deque((r, c) for r in range(rows) for c in range(cols) if grid[r][c] == 2)   # ALL sources at once
    fresh = sum(row.count(1) for row in grid)
    minutes = 0
    while queue and fresh:
        for _ in range(len(queue)):
            r, c = queue.popleft()
            for nr, nc in ((r + 1, c), (r - 1, c), (r, c + 1), (r, c - 1)):
                if 0 <= nr < rows and 0 <= nc < cols and grid[nr][nc] == 1:
                    grid[nr][nc] = 2
                    fresh -= 1
                    queue.append((nr, nc))
        minutes += 1
    return minutes if not fresh else -1


def topological_sort(n, edges):
    """edges (a, b) mean a must come before b. Returns [] if there is a cycle."""
    graph, indegree = defaultdict(list), [0] * n
    for a, b in edges:
        graph[a].append(b)
        indegree[b] += 1
    queue = deque(node for node in range(n) if indegree[node] == 0)
    order = []
    while queue:
        node = queue.popleft()
        order.append(node)
        for nxt in graph[node]:
            indegree[nxt] -= 1
            if indegree[nxt] == 0:
                queue.append(nxt)
    return order if len(order) == n else []


def can_finish_courses(n, prerequisites):
    return len(topological_sort(n, prerequisites)) == n


def dijkstra(graph, src):
    """graph: {node: [(neighbour, weight), ...]}"""
    distances = {src: 0}
    heap = [(0, src)]
    while heap:
        dist, node = heapq.heappop(heap)
        if dist > distances.get(node, float("inf")):
            continue                                  # stale entry
        for neighbour, weight in graph[node]:
            new_dist = dist + weight
            if new_dist < distances.get(neighbour, float("inf")):
                distances[neighbour] = new_dist
                heapq.heappush(heap, (new_dist, neighbour))
    return distances


class UnionFind:
    def __init__(self, n):
        self.parent = list(range(n))
        self.components = n

    def find(self, x):
        while self.parent[x] != x:
            self.parent[x] = self.parent[self.parent[x]]   # path compression
            x = self.parent[x]
        return x

    def union(self, a, b):
        ra, rb = self.find(a), self.find(b)
        if ra == rb:
            return False                                   # already connected -> cycle
        self.parent[ra] = rb
        self.components -= 1
        return True


def is_bipartite(adjacency):
    color = {}
    for start in range(len(adjacency)):
        if start in color:
            continue
        color[start] = 1
        queue = deque([start])
        while queue:
            node = queue.popleft()
            for nxt in adjacency[node]:
                if nxt not in color:
                    color[nxt] = -color[node]
                    queue.append(nxt)
                elif color[nxt] == color[node]:
                    return False
    return True


def word_ladder(begin, end, word_list):
    """Shortest transformation length changing one letter at a time (BFS)."""
    words = set(word_list)
    if end not in words:
        return 0
    queue = deque([(begin, 1)])
    while queue:
        word, steps = queue.popleft()
        if word == end:
            return steps
        for i in range(len(word)):
            for ch in "abcdefghijklmnopqrstuvwxyz":
                candidate = word[:i] + ch + word[i + 1:]
                if candidate in words:
                    words.remove(candidate)               # visited
                    queue.append((candidate, steps + 1))
    return 0


def clone_graph(node, cloned=None):
    """node: object with .val and .neighbours"""
    if node is None:
        return None
    cloned = {} if cloned is None else cloned
    if node.val in cloned:
        return cloned[node.val]
    copy = type(node)(node.val)
    cloned[node.val] = copy
    copy.neighbours = [clone_graph(n, cloned) for n in node.neighbours]
    return copy


class Node:
    def __init__(self, val, neighbours=None):
        self.val, self.neighbours = val, neighbours or []


if __name__ == "__main__":
    #  0 - 1
    #  |   |
    #  2 - 3 - 4
    graph = build_graph([(0, 1), (0, 2), (1, 3), (2, 3), (3, 4)])
    check("bfs", bfs(graph, 0), [0, 1, 2, 3, 4])
    check("dfs", dfs(graph, 0), [0, 1, 3, 2, 4])
    check("dfs_iterative", dfs_iterative(graph, 0), [0, 1, 3, 2, 4])
    check("shortest_path 0->4", shortest_path(graph, 0, 4), [0, 1, 3, 4])

    check("num_islands", num_islands(["11000", "11000", "00100", "00011"]), 3)
    check("oranges_rotting", oranges_rotting([[2, 1, 1], [1, 1, 0], [0, 1, 1]]), 4)
    check("topological_sort", topological_sort(6, [(5, 2), (5, 0), (4, 0), (4, 1), (2, 3), (3, 1)]), [4, 5, 2, 0, 3, 1])
    check("can_finish_courses", (can_finish_courses(2, [(0, 1)]), can_finish_courses(2, [(0, 1), (1, 0)])), (True, False))

    weighted = defaultdict(list, {0: [(1, 4), (2, 1)], 1: [(3, 1)], 2: [(1, 2), (3, 5)], 3: [(4, 3)], 4: []})
    check("dijkstra", dijkstra(weighted, 0), {0: 0, 2: 1, 1: 3, 3: 4, 4: 7})

    uf = UnionFind(5)
    for a, b in ((0, 1), (1, 2), (3, 4)):
        uf.union(a, b)
    check("union-find components", uf.components, 2)
    check("union-find detects cycle", uf.union(2, 0), False)

    check("is_bipartite", (is_bipartite([[1, 3], [0, 2], [1, 3], [0, 2]]), is_bipartite([[1, 2], [0, 2], [0, 1]])),
          (True, False))
    check("word_ladder", word_ladder("hit", "cog", ["hot", "dot", "dog", "lot", "log", "cog"]), 5)

    a, b, c = Node(1), Node(2), Node(3)
    a.neighbours, b.neighbours, c.neighbours = [b, c], [a], [a]
    copy = clone_graph(a)
    check("clone_graph", (copy is not a, copy.val, sorted(n.val for n in copy.neighbours)), (True, 1, [2, 3]))

    print("\nall checks passed")
